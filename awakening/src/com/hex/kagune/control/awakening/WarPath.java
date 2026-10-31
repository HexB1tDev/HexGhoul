package com.hex.kagune.control.awakening;

import com.hex.kagune.control.king.KingMarkFx;
import com.hex.kagune.control.king.KingMove;
import com.hex.kagune.control.king.WarRules;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CKingFx;
import com.hex.kagune.control.server.KingEffects;
import com.hex.kagune.control.server.KingServer;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.sound.HexKaguneSounds;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1.0.27: the warpath, War's reworked moves on the server ({@link WarRules}), handed his keys, ticks and blows by {@link WarKit}:
 * <ul>
 *   <li>RMB, the spike cone: his hand swiped out where he looks, and spikes break the ground in a widening cone, row by row, each on
 *       the ground where it stands (up a slope, down into a dip), cutting what stands over them.</li>
 *   <li>Z, King's Execution: at a body within {@link WarRules#BLITZ_RANGE} of his aim, the blitz: on it in a blink (afterimages, out of
 *       sight and back), the punch, after it into the air, the cut, the kick, the last blow to the gut drawn back slow, and it is
 *       driven into the ground: the crater, the cracks, the ground shaking for everyone near. At nothing: the dash, both hands raised,
 *       and the ground's spikes up all round him (slowing, bleeding).</li>
 *   <li>V, Black Sun, the ultimate: at a body within {@link WarRules#SUNDER_RANGE}, on it, the uppercut sending it high, after it, the
 *       punch drawn all the way back and thrown: a ring of air out across everything; the body flung down and away, through whatever
 *       stands in its way (never down into the ground), skidding on along it to a stop; what it broke grows back after.</li>
 * </ul>
 * Each is a small state machine on the wearer; the body's moves go to every client through {@link KingServer#move}, what they do in
 * the world through {@link S2CKingFx}.
 */
final class WarPath {
    static final WarPath INSTANCE = new WarPath();

    /** The cone in flight: its spikes (x, y, z, row), what they have cut. */
    private static final class Cone {
        long at;
        V3 ahead;
        V3 right;
        V3 from;
        final List<double[]> spikes = new ArrayList<>();
        final Set<Integer> hit = new HashSet<>();
    }

    /** The Execution's dash at nothing, then the ground's spikes. */
    private static final class Rise {
        long at;
        V3 from;
        V3 to;
        long risenAt = -1L;
    }

    /** The blitz, or Black Sun (the same shape): at whom, from where, which way; the fall at the end. */
    private static final class Blitz {
        boolean sunder;
        long at;
        int target;
        V3 origin;
        V3 away;
        V3 right;
        V3 from;
        /** Where it was the tick before (the server drives it). */
        V3 body;
        /** Driven down (the blitz) or flung (Black Sun): which way, how fast; skidding along the ground now. */
        V3 fall;
        double speed;
        boolean skidding;
        /** 1.0.28: it has touched the ground once (the crash's blow and crater are its first touch's alone). */
        boolean touched;
        /** 1.0.28: ticks it has been driven on into solid ground in a row (a wall, a roof, a tree: through; more: it has hit the ground). */
        int drilled;
        /** 1.0.28: how much of the move's height it is thrown (0..1): what room there is over it (a ceiling, a cave's roof). */
        double lift = 1.0;
        boolean landed;
        long landedAt = -1L;
        /** 1.0.33: Black Sun: where the punch found it (it hangs there on his fist, feeling it, till it flies). */
        V3 punched;
        /** 1.0.33: Black Sun: what its blows would have killed it with on the way (paid where it stops, so it always flies). */
        float owed;
        /** 1.0.36: whether it fell of itself before (its gravity given back when it is let go; held, it does not fall between holds). */
        boolean fell = true;
    }

    private static final class State {
        long coneReady = Long.MIN_VALUE;
        Cone cone;
        Rise rise;
        Blitz blitz;
        long safeUntil = Long.MIN_VALUE;
        /** 1.0.33: after Black Sun he hangs in the air till this, then falls on his own ({@code falling}: no fall hurts him till he is down). */
        long hoverUntil = Long.MIN_VALUE;
        boolean falling;
    }

    private final Map<UUID, State> states = new HashMap<>();

    private WarPath() {
    }

    private State state(ServerPlayer player) {
        return states.computeIfAbsent(player.getUUID(), id -> new State());
    }

    /** True while one of the warpath's moves has his body (no other ability, no blow). */
    boolean busy(ServerPlayer player, long now) {
        State state = states.get(player.getUUID());
        return state != null && (state.blitz != null || state.rise != null && (state.rise.risenAt < 0L || now - state.rise.risenAt < WarRules.RISE_TICKS));
    }

    /** True while no blow touches him: through the blitz and Black Sun. */
    boolean untouchable(ServerPlayer player, long now) {
        State state = states.get(player.getUUID());
        return state != null && state.blitz != null;
    }

    /** Whether his falls (no one's blow) do him nothing just now: through and a while after the moves that leave him in the air. */
    boolean fallSafe(ServerPlayer player, long now) {
        State state = states.get(player.getUUID());
        return state != null && now < state.safeUntil;
    }

    /** 1.0.36: what the move held let go: its gravity its own again. */
    private static void release(ServerPlayer player, Blitz b) {
        if (b == null || !b.fell) return;
        b.fell = false;
        LivingEntity prey = WarKit.byId(player, b.target, 128.0);
        if (prey != null) prey.setNoGravity(false);
    }

    void off(ServerPlayer player) {
        State gone = states.get(player.getUUID());
        if (gone != null) release(player, gone.blitz);
        gone = states.remove(player.getUUID());
        if (gone != null && gone.hoverUntil != Long.MIN_VALUE) player.setNoGravity(false);
    }

    // ------------------------------------------------------------------ RMB: the spike cone

    /** The swipe: the cone of spikes laid out on the ground where he looks, rising a row a tick from his hand. */
    void cone(ServerPlayer player, long now) {
        State state = state(player);
        if (now < state.coneReady || state.cone != null && now - state.cone.at < WarRules.CONE_SWIPE + WarRules.CONE_ROWS) return;
        state.coneReady = now + (com.hex.kagune.control.server.Cooldowns.off(player) ? 0L : WarRules.CONE_COOLDOWN);
        Cone cone = new Cone();
        cone.at = now;
        cone.from = new V3(player.getX(), player.getY(), player.getZ());
        cone.ahead = WarKit.facing(player);
        cone.right = new V3(-cone.ahead.z(), 0.0, cone.ahead.x());
        ServerLevel level = player.serverLevel();
        double rowY = cone.from.y();
        long seed = now * 31L + player.getId();
        List<float[]> drawn = new ArrayList<>();
        for (int row = 0; row < WarRules.CONE_ROWS; row++) {
            int n = WarRules.coneSpikes(row);
            double found = Double.NaN;
            for (int k = 0; k < n; k++) {
                double j1 = noise(seed, row * 17 + k) * 2.0 - 1.0;
                double j2 = noise(seed, row * 17 + k + 7) * 2.0 - 1.0;
                double across = WarRules.coneAcross(row, k, j1);
                double along = WarRules.coneAlong(row) + 0.35 * j2;
                V3 flat = cone.from.add(cone.ahead.scale(along)).add(cone.right.scale(across));
                double y = ground(level, flat.x(), flat.z(), rowY);
                if (Double.isNaN(y)) continue; // a drop or a wall there: no spike
                found = Double.isNaN(found) ? y : Math.max(found, y);
                double side = across == 0.0 ? 0.0 : Math.signum(across);
                V3 lean = cone.ahead.scale(0.35).add(cone.right.scale(0.25 * side));
                double height = 1.1 + 0.11 * row + 0.4 * noise(seed, row * 17 + k + 3);
                double width = 0.24 + 0.012 * row;
                cone.spikes.add(new double[] {flat.x(), y, flat.z(), row});
                drawn.add(new float[] {(float) flat.x(), (float) y, (float) flat.z(), (float) lean.x(), (float) lean.z(), (float) height, (float) width,
                        WarRules.CONE_SWIPE + row});
            }
            if (!Double.isNaN(found)) rowY = found; // the next row follows the ground on from this one
        }
        state.cone = cone;
        spikes(player, drawn);
        KingServer.move(player, KingMove.CONE, 0, 0);
        WarKit.play(player, WarKit.center(player), HexKaguneSounds.KAGUNE_WHIP, 1.5F, 0.6F);
        WarKit.vanilla(player, WarKit.center(player), "entity.player.attack.sweep", 1.0F, 0.6F);
    }

    private void coning(ServerPlayer player, State state, long now) {
        Cone cone = state.cone;
        if (cone == null) return;
        int row = (int) (now - cone.at) - WarRules.CONE_SWIPE;
        if (row >= WarRules.CONE_ROWS) {
            state.cone = null;
            return;
        }
        if (row < 0) return;
        V3 rowAt = null;
        for (double[] spike : cone.spikes) {
            if ((int) spike[3] != row) continue;
            V3 at = new V3(spike[0], spike[1], spike[2]);
            if (rowAt == null) rowAt = at;
            for (LivingEntity body : WarKit.near(player, at, 1.6)) {
                if (cone.hit.contains(body.getId()) || !KaguneServerController.validSwipeTarget(player, body)) continue;
                double dx = body.getX() - at.x();
                double dz = body.getZ() - at.z();
                if (dx * dx + dz * dz > Math.pow(0.7 + body.getBbWidth() * 0.5, 2.0) || body.getY() < at.y() - 1.0 || body.getY() > at.y() + 2.2) continue;
                cone.hit.add(body.getId());
                if (WarKit.hurt(player, body, WarRules.CONE_DAMAGE, at.sub(cone.ahead), 0.15, WarRules.CONE_LIFT) && body.isAlive()) {
                    KingEffects.bleed(body, player, WarRules.CONE_BLEED, 0);
                    KingEffects.slow(body, player, WarRules.CONE_SLOW, 0);
                    V3 c = WarKit.center(body);
                    WarKit.mark(player, KingMarkFx.BLOOD, c, new V3(0.0, 1.0, 0.0).add(cone.ahead.scale(0.4)), 0.9, body);
                }
            }
        }
        if (rowAt != null) {
            WarKit.play(player, rowAt, HexKaguneSounds.BONE_CRACK, 0.9F, 0.65F + 0.03F * row);
            if (row % 3 == 0) WarKit.mark(player, KingMarkFx.SOOT, rowAt.add(0.0, 0.3, 0.0), new V3(0.0, 1.0, 0.0), 0.5 + 0.05 * row, null);
        }
    }

    // ------------------------------------------------------------------ Z: King's Execution

    void execution(ServerPlayer player, int clicked, long now) {
        State state = state(player);
        LivingEntity prey = aimed(player, clicked, WarRules.BLITZ_RANGE);
        int slot = WarRules.Ability.EXECUTION.ordinal();
        if (prey != null) {
            KingServer.cooldown(player, slot, now + WarRules.Ability.EXECUTION.cooldown());
            begin(player, state, prey, now, false);
            return;
        }
        // At nothing: the dash where he looks, then both hands raised and the ground's spikes up round him.
        KingServer.cooldown(player, slot, now + WarRules.Ability.EXECUTION.cooldown() * 3 / 5);
        V3 from = new V3(player.getX(), player.getY(), player.getZ());
        V3 look = WarKit.look(player);
        V3 eye = WarKit.eye(player);
        V3 end = eye.add(look.scale(WarRules.DASH_RANGE));
        BlockHitResult hit = player.serverLevel().clip(new ClipContext(new Vec3(eye.x(), eye.y(), eye.z()), new Vec3(end.x(), end.y(), end.z()),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double distance = WarRules.DASH_RANGE;
        if (hit.getType() == HitResult.Type.BLOCK) {
            Vec3 at = hit.getLocation();
            distance = Math.max(0.0, new V3(at.x, at.y, at.z).distance(eye) - 0.8);
        }
        Rise rise = new Rise();
        rise.at = now;
        rise.from = from;
        rise.to = from.add(look.scale(distance)).withY(Math.max(from.y() - 2.0, from.y() + look.y() * distance));
        state.rise = rise;
        state.safeUntil = now + WarRules.DASH_TICKS + WarRules.RISE_TICKS + 20;
        KingServer.move(player, KingMove.DASH, 0, 0);
        WarKit.fx(player, S2CKingFx.BLITZ, new int[0], from.add(0.0, 1.0, 0.0), rise.to.add(0.0, 1.0, 0.0));
        WarKit.play(player, WarKit.center(player), HexKaguneSounds.KAGUNE_WHIP, 1.6F, 1.3F);
    }

    private void rising(ServerPlayer player, State state, long now) {
        Rise rise = state.rise;
        if (rise == null) return;
        long age = now - rise.at;
        if (age < WarRules.DASH_TICKS) {
            V3 step = rise.to.sub(rise.from).scale(1.0 / WarRules.DASH_TICKS);
            KingCombos.motion(player, step.x(), step.y(), step.z());
            player.fallDistance = 0.0F;
            return;
        }
        if (rise.risenAt < 0L) {
            KingCombos.motion(player, 0.0, 0.0, 0.0); // 1.0.36: (never his last speed again: the server's is stale)
            rise.risenAt = now;
            KingServer.move(player, KingMove.RISE, 0, 0);
            WarKit.play(player, WarKit.center(player), HexKaguneSounds.KAGUNE_STRAIN, 1.6F, 0.55F);
            return;
        }
        long risen = now - rise.risenAt;
        if (risen == WarRules.RISE_SPIKES) groundSpikes(player);
        if (risen >= WarRules.RISE_TICKS) state.rise = null;
    }

    /** Both hands raised: two rings of spikes up out of the ground round him, slowing and bleeding what they catch. */
    private void groundSpikes(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        V3 at = new V3(player.getX(), player.getY(), player.getZ());
        long seed = level.getGameTime() * 13L + player.getId();
        List<float[]> drawn = new ArrayList<>();
        double[][] rings = {{1.6, 6}, {3.0, 10}, {4.4, 14}};
        for (int ring = 0; ring < rings.length; ring++) {
            int n = (int) rings[ring][1];
            for (int k = 0; k < n; k++) {
                double a = (k + 0.5 * noise(seed, ring * 40 + k)) / n * Math.PI * 2.0 + ring * 0.4;
                double r = rings[ring][0] + 0.4 * (noise(seed, ring * 40 + k + 20) - 0.5);
                V3 out = new V3(Math.cos(a), 0.0, Math.sin(a));
                V3 flat = at.add(out.scale(r));
                double y = ground(level, flat.x(), flat.z(), at.y());
                if (Double.isNaN(y)) continue;
                drawn.add(new float[] {(float) flat.x(), (float) y, (float) flat.z(), (float) (out.x() * 0.45), (float) (out.z() * 0.45),
                        (float) (1.5 + 0.45 * ring + 0.5 * noise(seed, ring * 40 + k + 9)), (float) (0.26 + 0.04 * ring), ring * 2});
            }
        }
        spikes(player, drawn);
        for (LivingEntity body : WarKit.near(player, WarRules.RISE_RADIUS + 1.0)) {
            if (!KaguneServerController.validSwipeTarget(player, body)) continue;
            if (new V3(body.getX() - at.x(), 0.0, body.getZ() - at.z()).length() > WarRules.RISE_RADIUS + body.getBbWidth() * 0.5) continue;
            if (Math.abs(body.getY() - at.y()) > 3.0) continue;
            if (WarKit.hurt(player, body, WarRules.RISE_DAMAGE, at, 0.2, 0.45) && body.isAlive()) {
                KingEffects.slow(body, player, WarRules.RISE_SLOW, 1);
                KingEffects.bleed(body, player, WarRules.RISE_BLEED, 1);
                WarKit.mark(player, KingMarkFx.BLOOD, WarKit.center(body), new V3(0.0, 1.0, 0.0), 1.0, body);
            }
        }
        WarKit.mark(player, KingMarkFx.QUAKE, at, V3.ZERO, 1.4, null);
        WarKit.mark(player, KingMarkFx.SOOT, at.add(0.0, 0.3, 0.0), new V3(0.0, 1.0, 0.0), 1.4, null);
        WarKit.play(player, at, HexKaguneSounds.KAGUNE_SLAM, 2.2F, 0.5F);
        WarKit.play(player, at, HexKaguneSounds.BONE_CRACK, 1.8F, 0.6F);
    }

    // ------------------------------------------------------------------ the blitz, and Black Sun

    void sunder(ServerPlayer player, int clicked, long now) {
        LivingEntity prey = aimed(player, clicked, WarRules.SUNDER_RANGE);
        if (prey == null) {
            player.displayClientMessage(Component.literal("§cBlack Sun§7: nothing within " + (int) WarRules.SUNDER_RANGE + " blocks of your aim"), true);
            return;
        }
        KingServer.cooldown(player, WarRules.Ability.SUN.ordinal(), now + WarRules.Ability.SUN.cooldown());
        begin(player, state(player), prey, now, true);
    }

    /** On {@code prey} in a blink: the blitz, or ({@code sunder}) Black Sun. */
    private void begin(ServerPlayer player, State state, LivingEntity prey, long now, boolean sunder) {
        Blitz b = new Blitz();
        b.sunder = sunder;
        b.at = now;
        b.target = prey.getId();
        b.origin = new V3(prey.getX(), prey.getY(), prey.getZ());
        b.from = new V3(player.getX(), player.getY(), player.getZ());
        V3 away = new V3(b.origin.x() - b.from.x(), 0.0, b.origin.z() - b.from.z());
        b.away = away.length() < 1.0E-3 ? WarKit.facing(player) : away.normalize();
        b.right = new V3(-b.away.z(), 0.0, b.away.x());
        b.body = b.origin;
        // 1.0.28: under a ceiling (a room, a cave) it is thrown only as high as there is room, never up into the blocks over it. (1.0.33:
        // Black Sun's uppercut smashes up through whatever is over it, to grow back after: only the top of the world stops it.)
        double full = sunder ? WarRules.SUNDER_UP : WarRules.BLITZ_PEAK;
        double room = sunder ? player.serverLevel().getMaxBuildHeight() - 3.0 - b.origin.y() - prey.getBbHeight()
                : headroom(player.serverLevel(), b.origin, full + prey.getBbHeight() + 1.0) - prey.getBbHeight() - 0.3;
        b.lift = Math.max(0.0, Math.min(1.0, room / full));
        b.fell = !prey.isNoGravity();
        prey.setNoGravity(true); // 1.0.36: held by him, not falling between his holds (what made it shake)
        state.blitz = b;
        state.cone = null;
        state.rise = null;
        KingEffects.stun(prey, (sunder ? WarRules.SUNDER_LAUNCH : WarRules.BLITZ_GUT) + 30);
        KingServer.move(player, sunder ? KingMove.SUNDER : KingMove.BLITZ, 0, prey.getId());
        V3 front = b.origin.sub(b.away.scale(1.2 + prey.getBbWidth() * 0.5));
        WarKit.fx(player, S2CKingFx.BLITZ, new int[] {prey.getId()}, b.from.add(0.0, 1.0, 0.0), front.add(0.0, 1.0, 0.0));
        WarKit.play(player, WarKit.center(player), HexKaguneSounds.KAGUNE_WHIP, 1.8F, 1.4F);
        WarKit.vanilla(player, WarKit.center(player), "entity.player.attack.sweep", 0.8F, 1.6F);
    }

    private void blitzing(ServerPlayer player, State state, long now) {
        Blitz b = state.blitz;
        if (b == null) return;
        long age = now - b.at;
        LivingEntity prey = WarKit.byId(player, b.target, 96.0);
        if (prey == null || !prey.isAlive() || age > (b.sunder ? WarRules.SUNDER_TICKS : WarRules.BLITZ_TICKS)) {
            finish(player, state, now);
            return;
        }
        if (b.sunder) sundering(player, state, b, prey, age, now);
        else blitz(player, b, prey, age, now);
        if (b.landed && now - b.landedAt > 12) finish(player, state, now);
    }

    /** The blitz, tick by tick ({@link WarRules#blitzBody}, {@link WarRules#blitzBeside}). */
    private void blitz(ServerPlayer player, Blitz b, LivingEntity prey, long age, long now) {
        V3 front = b.origin.sub(b.away.scale(1.2 + prey.getBbWidth() * 0.5));
        if (age < WarRules.BLITZ_GUT) {
            double[] local = WarRules.blitzBody(age);
            local[1] *= b.lift;
            b.body = place(b, local);
            hold(prey, b.body);
        }
        // Him: on it, then up beside it from the blink on, then let go after the last blow. 1.0.36: carried along where the blitz has
        // him (glide), never chasing where the server last saw him (that threw him back and forth, and off across the world).
        double w = prey.getBbWidth() * 0.5;
        if (age < WarRules.BLITZ_GUT) {
            V3 at = blitzPlan(b, w, age);
            if ((age == WarRules.BLITZ_BLINK || age == WarRules.BLITZ_KICK + 1 || WarRules.flurryBlink(age)) && open(player.serverLevel(), at)) {
                V3 was = new V3(player.getX(), player.getY(), player.getZ());
                blink(player, at);
                WarKit.fx(player, S2CKingFx.BLITZ, new int[] {prey.getId()}, was.add(0.0, 1.0, 0.0), at.add(0.0, 1.0, 0.0));
            } else {
                glide(player, at, blitzPlan(b, w, age + 1.0));
            }
        } else if (age == WarRules.BLITZ_GUT) {
            // Let go: still where he is, then down on his own, slowly (nothing of the fall hurts him).
            KingCombos.motion(player, 0.0, 0.0, 0.0);
            KingEffects.glide(player, 40);
        }
        V3 c = WarKit.center(prey);
        V3 into = b.away.add(0.0, 0.2, 0.0).normalize();
        int jab = WarRules.jab(age);
        int hit = WarRules.flurryHit(age);
        if (jab >= 0) {
            // 1.2.8: the jabs on the ground first, left and right, before the punch that sends it up.
            blow(player, prey, WarRules.BLITZ_JAB_DAMAGE, c, into, 0.55, false);
            WarKit.vanilla(player, c, jab % 2 == 0 ? "entity.player.attack.strong" : "entity.player.attack.knockback", 0.9F, 1.2F);
        } else if (hit >= 0) {
            // 1.2.8: the flurry: a blow from each side he blinks to, four seconds of them, small; the last one is the one that counts.
            V3 from = c.sub(WarKit.center(player));
            V3 way = from.length() < 1.0E-3 ? into : from.normalize();
            blow(player, prey, WarRules.BLITZ_FLURRY_DAMAGE, c, way, 0.6, false);
            WarKit.vanilla(player, c, hit % 3 == 2 ? "entity.player.attack.knockback" : "entity.player.attack.strong", 0.8F, 1.0F + 0.1F * (hit % 3));
        } else if (age == WarRules.BLITZ_PUNCH) {
            blow(player, prey, WarRules.BLITZ_PUNCH_DAMAGE, c, into, 1.1, false);
            WarKit.vanilla(player, c, "entity.player.attack.strong", 1.4F, 0.7F);
        } else if (age == WarRules.BLITZ_CUT) {
            if (blow(player, prey, WarRules.BLITZ_CUT_DAMAGE, c, b.right, 1.0, true)) KingEffects.bleed(prey, player, 100, 1);
            WarKit.play(player, c, HexKaguneSounds.KAGUNE_WHIP, 1.6F, 0.9F);
            WarKit.vanilla(player, c, "entity.player.attack.sweep", 1.2F, 0.8F);
        } else if (age == WarRules.BLITZ_KICK) {
            blow(player, prey, WarRules.BLITZ_KICK_DAMAGE, c, into.add(0.0, 0.5, 0.0).normalize(), 1.0, false);
            WarKit.vanilla(player, c, "entity.player.attack.knockback", 1.4F, 0.6F);
        } else if (age == WarRules.BLITZ_WIND) {
            // The last one drawn back, slow: everything waits on it.
            WarKit.play(player, c, HexKaguneSounds.KAGUNE_STRAIN, 2.0F, 0.4F);
            WarKit.vanilla(player, c, "entity.warden.heartbeat", 2.0F, 0.6F);
        } else if (age == WarRules.BLITZ_GUT) {
            blow(player, prey, WarRules.BLITZ_GUT_DAMAGE, c, b.away, 2.2, false);
            WarKit.play(player, c, HexKaguneSounds.BONE_CRUSH, 2.4F, 0.5F);
            WarKit.vanilla(player, c, "entity.generic.explode", 1.2F, 1.3F);
            b.fall = new V3(0.0, -1.0, 0.0).add(b.away.scale(0.28)).normalize();
            b.speed = WarRules.BLITZ_CRASH_SPEED;
        }
        if (age > WarRules.BLITZ_GUT && !b.landed) crash(player, b, prey, now, false);
    }

    /**
     * Black Sun, tick by tick. 1.0.33: the uppercut smashes it up through whatever is over it (and him after it); the punch is his bare
     * left fist, drawn back long and slow, from over and behind it; where it lands the body hangs on his fist a moment, driven in and
     * shuddering, feeling it (the ring of air standing up across the sky round the blow), before it is sent down steep into the
     * ground and on along it through everything in its way, tumbling limp; he hangs in the air a while after, then falls on his own.
     * Its blows on the way never kill it: what would have is paid where it stops.
     */
    private void sundering(ServerPlayer player, State state, Blitz b, LivingEntity prey, long age, long now) {
        TerrainScars scars = TerrainScars.of(player.serverLevel());
        V3 front = b.origin.sub(b.away.scale(1.2 + prey.getBbWidth() * 0.5));
        if (age < WarRules.SUNDER_PUNCH) {
            double up = WarRules.sunderRise(age) * b.lift;
            b.body = b.origin.add(0.0, up, 0.0).add(b.away.scale(0.4 * up / WarRules.SUNDER_UP));
            if (age >= WarRules.SUNDER_UPPER && age <= WarRules.SUNDER_APEX + 1) {
                // Up through the ceiling: what is over it and round it broken as it goes (never the ground it stood on).
                double floor = b.origin.y() + 0.6;
                scars.carveSphere(player, b.body.add(0.0, prey.getBbHeight() + 0.9, 0.0), 1.7, floor);
                scars.carveSphere(player, b.body.add(0.0, prey.getBbHeight() * 0.5, 0.0), 1.3, floor);
            }
            hold(prey, b.body);
        } else if (age < WarRules.SUNDER_LAUNCH) {
            if (b.punched == null) b.punched = b.body;
            double k = (age - WarRules.SUNDER_PUNCH) / (double) WarRules.SUNDER_FELT;
            double sink = WarRules.SUNDER_SINK * (1.0 - (1.0 - k) * (1.0 - k));
            double shudder = WarRules.SUNDER_SHUDDER * (1.0 - 0.6 * k);
            V3 jolt = b.right.scale(shudder * Math.sin(age * 2.9)).add(0.0, shudder * Math.sin(age * 3.7 + 1.0), 0.0);
            V3 into = b.away.scale(Math.cos(WarRules.SUNDER_DIVE)).add(0.0, -Math.sin(WarRules.SUNDER_DIVE), 0.0);
            b.body = b.punched.add(into.scale(sink)).add(jolt);
            hold(prey, b.body);
        }
        // Over it and behind it, his left shoulder in line with it, to drive it down into the ground.
        V3 over = b.origin.add(0.0, WarRules.SUNDER_UP * b.lift + 0.6, 0.0).sub(b.away.scale(0.95 + prey.getBbWidth() * 0.5)).add(b.right.scale(0.35));
        if (age < WarRules.SUNDER_LAUNCH) {
            // 1.0.36: along his own path (glide), as the blitz.
            glide(player, sunderPlan(b, front, over, age), sunderPlan(b, front, over, age + 1.0)); // his fist left in it till it flies
        }
        if (age >= WarRules.SUNDER_LEAP && age < WarRules.SUNDER_LAUNCH) {
            if (age <= WarRules.SUNDER_BESIDE + 2) {
                scars.carveSphere(player, new V3(player.getX(), player.getY() + 1.0, player.getZ()), 1.4, b.origin.y() + 0.6);
            }
        }
        V3 c = WarKit.center(prey);
        if (age == WarRules.SUNDER_UPPER) {
            b.owed += bruise(player, prey, WarRules.SUNDER_UPPER_DAMAGE, c.sub(new V3(0.0, 1.0, 0.0)));
            WarKit.mark(player, KingMarkFx.FLARE, c, new V3(0.0, 1.0, 0.0), 1.4, prey);
            WarKit.play(player, c, HexKaguneSounds.FLESH_IMPACT, 1.6F, 0.7F);
            WarKit.vanilla(player, c, "entity.player.attack.strong", 1.6F, 0.5F);
            WarKit.vanilla(player, c, "entity.generic.explode", 0.8F, 1.6F);
        } else if (age == WarRules.SUNDER_BESIDE) {
            // 1.2.8: the punch drawn back: the user's energy swelling all through the draw (it was a swing's whoosh and a blood drop).
            WarKit.vanilla(player, c, "hexkagune:sfx.transform.energy_metamorphosis", 2.4F, 1.0F);
            WarKit.vanilla(player, c, "entity.warden.heartbeat", 2.4F, 0.5F);
        } else if (age > WarRules.SUNDER_BESIDE && age < WarRules.SUNDER_PUNCH && (WarRules.SUNDER_PUNCH - age) % 12 == 0) {
            // The draw goes on: a heartbeat at each turn of the screw.
            WarKit.vanilla(player, c, "entity.warden.heartbeat", 2.6F, 0.45F);
        } else if (age == WarRules.SUNDER_PUNCH) {
            b.owed += bruise(player, prey, WarRules.SUNDER_PUNCH_DAMAGE, c.sub(b.away));
            WarKit.mark(player, KingMarkFx.FLARE, c, b.away, 3.0, prey);
            WarKit.play(player, c, HexKaguneSounds.FLESH_IMPACT, 1.6F, 0.7F);
            V3 fist = c.sub(b.away.scale(0.6));
            // The ring: where, {how far, how long, how far the ground shakes}, {the punch's way x, z, the ground's height}.
            WarKit.fx(player, S2CKingFx.AIR_RING, new int[0], fist, new V3(WarRules.RING_RADIUS, WarRules.RING_TICKS, WarRules.SUNDER_SHAKE),
                    new V3(b.away.x(), b.away.z(), b.origin.y()));
            WarKit.vanilla(player, fist, "entity.warden.sonic_boom", 3.0F, 0.6F);
            WarKit.vanilla(player, fist, "entity.generic.explode", 3.0F, 0.5F);
            WarKit.play(player, fist, HexKaguneSounds.BONE_CRUSH, 3.0F, 0.4F);
        } else if (age == WarRules.SUNDER_PUNCH + WarRules.SUNDER_FELT / 2) {
            WarKit.play(player, c, HexKaguneSounds.BONE_CRACK, 2.2F, 0.5F); // it gives, inside
        } else if (age == WarRules.SUNDER_LAUNCH) {
            WarKit.vanilla(player, c, "entity.generic.explode", 2.6F, 0.7F);
            WarKit.vanilla(player, c, "entity.player.attack.knockback", 2.0F, 0.5F);
            WarKit.mark(player, KingMarkFx.FLARE, c, V3.ZERO, 2.0, null);
            double dive = WarRules.SUNDER_DIVE;
            b.fall = b.away.scale(Math.cos(dive)).add(0.0, -Math.sin(dive), 0.0).normalize();
            b.speed = WarRules.SUNDER_FLY;
            // Limp from here on, tumbling the way it is driven, lying where it stops.
            KingEffects.tumble(player, prey, new Vec3(b.fall.x() * b.speed, b.fall.y() * b.speed, b.fall.z() * b.speed));
            // And him: left hanging in the air where the blow left him (his view his own), till he falls.
            state.hoverUntil = now + WarRules.SUNDER_HANG;
            state.falling = false;
            KingCombos.motion(player, 0.0, 0.0, 0.0);
            player.setNoGravity(true);
        }
        if (age > WarRules.SUNDER_LAUNCH && !b.landed) crash(player, b, prey, now, true);
    }

    /**
     * 1.0.33: one of Black Sun's blows on its way: as much of {@code amount} as leaves it alive (one health at least), the rest owed
     * (paid where it stops, {@link #land}): so it is always flung, its body always seen to fly.
     */
    private static float bruise(ServerPlayer player, LivingEntity prey, float amount, V3 from) {
        float now = Math.min(amount, Math.max(0.0F, prey.getHealth() - 1.0F));
        if (now > 0.0F) WarKit.hurt(player, prey, now, from, 0.0, 0.0);
        return amount - now;
    }

    /**
     * The body driven along {@link Blitz#fall}: through what stands in its way (broken, to grow back), never down into the ground; where
     * it meets the ground, the crater (the blitz) or the skid along it to a stop (Black Sun).
     */
    private void crash(ServerPlayer player, Blitz b, LivingEntity prey, long now, boolean skid) {
        ServerLevel level = player.serverLevel();
        TerrainScars scars = TerrainScars.of(level);
        if (b.skidding) {
            V3 flat = b.fall.withY(0.0).normalize();
            V3 next = b.body.add(flat.scale(b.speed));
            double floor = ground(level, next.x(), next.z(), b.body.y());
            if (Double.isNaN(floor)) {
                if (solid(level, next.x(), b.body.y() + 1.0, next.z()) || solid(level, next.x(), b.body.y() + 0.3, next.z())) {
                    // 1.0.28: a cliff too tall to ride up: it slams into it and stops there (a dent knocked in it, never a tunnel).
                    scars.carveSphere(player, next.add(0.0, 1.0, 0.0), 1.5, b.body.y());
                    hold(prey, b.body);
                    land(player, b, prey, now, true);
                    return;
                }
                // 1.0.28: off an edge: over it and falling on, still flung, to come down again further on.
                b.skidding = false;
                b.fall = flat.scale(0.7).add(0.0, -0.7, 0.0).normalize();
                b.speed = Math.max(b.speed, 0.9);
                b.body = next;
                hold(prey, b.body);
                return;
            }
            // What stands in its way at its own height, broken (never below it: it rides along the ground).
            scars.carveSphere(player, next.add(0.0, 1.0, 0.0), 1.25, floor);
            b.body = next.withY(floor);
            hold(prey, b.body);
            if ((now & 1L) == 0L) WarKit.mark(player, KingMarkFx.QUAKE, b.body, V3.ZERO, 0.5, null);
            if (now % 3L == 0L) WarKit.mark(player, KingMarkFx.SOOT, b.body.add(0.0, 0.3, 0.0), b.fall.scale(-1.0).withY(0.4), 0.8, null);
            if (now % 4L == 0L) WarKit.play(player, b.body, HexKaguneSounds.BONE_CRACK, 1.0F, 0.5F);
            b.speed *= WarRules.SUNDER_DRAG;
            if (b.speed < 0.08) land(player, b, prey, now, true);
            return;
        }
        V3 next = b.body.add(b.fall.scale(b.speed));
        double floor = ground(level, next.x(), next.z(), Math.max(next.y(), b.body.y()) + 1.0);
        if (Double.isNaN(floor) && solid(level, next.x(), next.y() + 0.5, next.z())) {
            // 1.0.28: into solid ground: through it if it is thin (a wall, a roof, a tree), but driven on into it a while more and it
            // has hit the ground: down where it last was in the open (a slope, a hill's side), never on down into it.
            if (++b.drilled > DRILL) {
                next = b.body;
                floor = b.body.y();
            }
        } else {
            b.drilled = 0;
        }
        if (!Double.isNaN(floor) && next.y() <= floor + 0.05) {
            // The ground: on it, not in it.
            b.body = next.withY(floor);
            hold(prey, b.body);
            if (skid) {
                b.skidding = true;
                b.speed = Math.max(b.speed * 0.6, WarRules.SUNDER_SKID);
                if (!b.touched) {
                    b.touched = true;
                    b.owed += bruise(player, prey, WarRules.SUNDER_CRASH_DAMAGE, b.body.add(0.0, 2.0, 0.0));
                    WarKit.fx(player, S2CKingFx.CRATER, new int[] {prey.getId()}, b.body, new V3(2.6, WarRules.SHAKE_RADIUS, 0.0));
                    // 1.0.36: it slams onto the ground and goes on along it: what stands there is broken, never the ground under it.
                    scars.carveSphere(player, b.body.add(0.0, 1.0, 0.0), 1.6, floor);
                } else {
                    b.speed = Math.min(b.speed, WarRules.SUNDER_SKID * 0.7);
                }
            } else {
                land(player, b, prey, now, false);
            }
            return;
        }
        // Through whatever is in the air on its way (the ground under it left alone).
        double under = Double.isNaN(floor) ? next.y() - 2.0 : floor;
        scars.carveSphere(player, next.add(0.0, 0.8, 0.0), 1.35, Math.max(under, next.y() - 0.2));
        b.body = next;
        hold(prey, b.body);
        if (now % 2L == 0L) WarKit.mark(player, KingMarkFx.SOOT, WarKit.center(prey), b.fall.scale(-1.0), 0.7, null);
    }

    /** 1.0.28: ticks a falling body may be driven on into solid blocks (smashing through) before it counts as having hit the ground. */
    static final int DRILL = 2;

    /** It came down: the crater (the blitz), or it skidded to a stop (Black Sun); either way it lies there, stunned. */
    private void land(ServerPlayer player, Blitz b, LivingEntity prey, long now, boolean skidded) {
        b.landed = true;
        release(player, b); // down: it lies where it stopped, its own weight again
        b.landedAt = now;
        ServerLevel level = player.serverLevel();
        V3 at = b.body;
        if (!skidded) {
            TerrainScars.of(level).carveSphere(player, at.add(0.0, -1.2, 0.0), WarRules.CRATER_RADIUS, at.y() - 2.6);
            double floor = ground(level, at.x(), at.z(), at.y());
            if (!Double.isNaN(floor)) at = at.withY(floor);
            b.body = at;
            hold(prey, at);
            WarKit.hurt(player, prey, WarRules.BLITZ_CRASH_DAMAGE, at.add(0.0, 2.0, 0.0), 0.0, 0.0);
            for (LivingEntity near : WarKit.near(player, at, WarRules.CRATER_RADIUS + 1.0)) {
                if (near == prey || !KaguneServerController.validSwipeTarget(player, near)) continue;
                if (WarKit.center(near).distance(at) <= WarRules.CRATER_RADIUS + near.getBbWidth() * 0.5) {
                    WarKit.hurt(player, near, WarRules.CRATER_DAMAGE, at, 1.1, 0.6);
                }
            }
            WarKit.fx(player, S2CKingFx.CRATER, new int[] {prey.getId()}, at, new V3(WarRules.CRATER_RADIUS, WarRules.SHAKE_RADIUS, 0.0));
            WarKit.play(player, at, HexKaguneSounds.KAGUNE_SLAM, 3.0F, 0.4F);
            WarKit.vanilla(player, at, "entity.generic.explode", 2.2F, 0.6F);
        } else {
            // Black Sun: when it is all over, what it broke grows back (sooner than the usual wait). 1.0.33: and what its blows owed it,
            // paid here.
            TerrainScars.of(level).hastenAll(100L);
            if (b.owed > 0.0F) WarKit.hurt(player, prey, b.owed, at.add(0.0, 2.0, 0.0), 0.0, 0.0);
            b.owed = 0.0F;
        }
        if (prey.isAlive()) {
            KingEffects.stun(prey, 50);
            KingEffects.bleed(prey, player, 100, 1);
            if (!skidded) KingEffects.tumble(player, prey, new Vec3(b.away.x() * 0.6, 0.2, b.away.z() * 0.6)); // (Black Sun's tumbles from the launch)
            KingCombos.motion(prey, 0.0, 0.0, 0.0);
        }
        WarKit.mark(player, KingMarkFx.GORE, WarKit.center(prey), new V3(0.0, 1.0, 0.0), 1.6, prey);
        WarKit.mark(player, KingMarkFx.QUAKE, at, V3.ZERO, skidded ? 1.0 : 2.4, null);
    }

    /** It is over (or what it was at is gone): his body his own again, safe from the fall a while. */
    private void finish(ServerPlayer player, State state, long now) {
        Blitz b = state.blitz;
        state.blitz = null;
        release(player, b);
        state.safeUntil = Math.max(state.safeUntil, now + 50);
        if (b != null && !b.sunder) KingEffects.glide(player, 40); // 1.0.36: (was a float that kept re-sending his last speed: across the world)
        if (b != null && b.sunder) TerrainScars.of(player.serverLevel()).hastenAll(140L);
        KingServer.move(player, KingMove.NONE, 0, 0);
    }

    /** A blow of the blitz or Black Sun: {@code damage}, its mark ({@code cut}: a cut's blood, else a hot flash), the view kicked. */
    private static boolean blow(ServerPlayer player, LivingEntity prey, float damage, V3 at, V3 way, double size, boolean cut) {
        boolean landed = WarKit.hurt(player, prey, damage, at.sub(way), 0.0, 0.0);
        WarKit.mark(player, cut ? KingMarkFx.BLOOD : KingMarkFx.FLARE, at, way, size, prey);
        if (cut) WarKit.mark(player, KingMarkFx.GLINT, at, way, 0.8, null);
        WarKit.play(player, at, HexKaguneSounds.FLESH_IMPACT, 1.6F, 0.7F);
        return landed;
    }

    // ------------------------------------------------------------------ the clock

    void tick(ServerPlayer player, long now) {
        State state = states.get(player.getUUID());
        if (state == null) return;
        coning(player, state, now);
        rising(player, state, now);
        blitzing(player, state, now);
        if (now < state.safeUntil) player.fallDistance = 0.0F;
        hang(player, state, now);
    }

    /**
     * 1.0.33: after Black Sun: hanging in the air where the blow left him (looking where he likes), then let go to fall on his own,
     * nothing of the fall hurting him till he is down.
     */
    private static void hang(ServerPlayer player, State state, long now) {
        if (state.hoverUntil == Long.MIN_VALUE) return;
        if (now < state.hoverUntil) {
            player.setNoGravity(true);
            player.fallDistance = 0.0F;
            return;
        }
        if (!state.falling) {
            state.falling = true;
            player.setNoGravity(false);
        }
        player.fallDistance = 0.0F;
        if (solid(player.serverLevel(), player.getX(), player.getY() - 0.05, player.getZ()) || now - state.hoverUntil > 400L) {
            state.hoverUntil = Long.MIN_VALUE;
            state.falling = false;
        }
    }

    // ------------------------------------------------------------------ helpers

    /** Spikes breaking the ground, every client drawing them ({@link S2CKingFx#SPIKES}), sent in packets the network takes. */
    private static void spikes(ServerPlayer player, List<float[]> spikes) {
        int per = 200;
        for (int from = 0; from < spikes.size(); from += per) {
            int n = Math.min(per, spikes.size() - from);
            float[] data = new float[n * 8];
            for (int i = 0; i < n; i++) System.arraycopy(spikes.get(from + i), 0, data, i * 8, 8);
            ControlNetwork.broadcast(player, new S2CKingFx(player.getUUID(), S2CKingFx.SPIKES, new int[0], data));
        }
    }

    /** Where {@code local} (his frame: x his right, y up, z away from him) is from where the blitz found its body. */
    private static V3 place(Blitz b, double[] local) {
        return b.origin.add(b.right.scale(local[0])).add(0.0, local[1], 0.0).add(b.away.scale(local[2]));
    }

    /** The body held where the move has it (its own motion and its fall forgotten). */
    private static void hold(LivingEntity body, V3 at) {
        body.teleportTo(at.x(), at.y(), at.z());
        body.setDeltaMovement(new Vec3(0.0, 0.0, 0.0));
        body.fallDistance = 0.0F;
        body.hurtMarked = true;
    }

    /** 1.0.36: where the blitz has him {@code t} ticks in: in at it, then (from the blink) up beside it as it is held. */
    private static V3 blitzPlan(Blitz b, double w, double t) {
        V3 front = b.origin.sub(b.away.scale(1.2 + w));
        if (t <= WarRules.BLITZ_ARRIVE) {
            return b.from.lerp(front, Math.min(1.0, t / WarRules.BLITZ_ARRIVE));
        }
        if (t < WarRules.BLITZ_BLINK) {
            return front;
        }
        double[] local = WarRules.blitzBody(Math.min(t, WarRules.BLITZ_GUT - 0.01));
        local[1] *= b.lift;
        V3 body = place(b, local);
        double[] beside = WarRules.blitzBeside(t);
        return body.add(b.right.scale(beside[0])).add(0.0, beside[1], 0.0).add(b.away.scale(beside[2] - w));
    }

    /** 1.0.36: where Black Sun has him {@code t} ticks in: in at it, waiting under it, then up after it, over and behind it. */
    private static V3 sunderPlan(Blitz b, V3 front, V3 over, double t) {
        if (t <= WarRules.SUNDER_ARRIVE) {
            return b.from.lerp(front, Math.min(1.0, t / WarRules.SUNDER_ARRIVE));
        }
        return t < WarRules.SUNDER_LEAP ? front : front.lerp(over, WarRules.sunderLeap(t));
    }

    /**
     * 1.0.36: him carried from {@code was} to {@code to} over this tick: that step, and a third of how far he is off {@code was} (a
     * planned path, never "wherever the server last saw him to there", which overshoots a tick late, every tick: the shaking).
     */
    static void glide(ServerPlayer player, V3 was, V3 to) {
        V3 step = to.sub(was).add(was.sub(new V3(player.getX(), player.getY(), player.getZ())).scale(0.33));
        KingCombos.motion(player, step.x(), step.y() + (player.isNoGravity() ? 0.0 : 0.08), step.z());
        player.fallDistance = 0.0F;
    }


    /** Him there now (a blink). */
    private static void blink(ServerPlayer player, V3 at) {
        Entity body = player;
        if (player.connection != null) player.connection.teleport(at.x(), at.y(), at.z(), body.getYRot(), body.getXRot());
        else player.teleportTo(at.x(), at.y(), at.z());
        KingCombos.motion(player, 0.0, 0.08, 0.0);
    }


    /** 1.0.28: whether the block at (x, y, z) stops a body (has a collision shape). */
    static boolean solid(ServerLevel level, double x, double y, double z) {
        BlockPos pos = new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
        return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    /** 1.0.28: whether a body two blocks tall stands free with its feet at {@code at} (somewhere a blink may put him). */
    static boolean open(ServerLevel level, V3 at) {
        return !solid(level, at.x(), at.y() + 0.1, at.z()) && !solid(level, at.x(), at.y() + 1.1, at.z()) && !solid(level, at.x(), at.y() + 1.8, at.z());
    }

    /** 1.0.28: how far it is open straight up from {@code at} (blocks), looked for up to {@code most}. */
    static double headroom(ServerLevel level, V3 at, double most) {
        int bx = (int) Math.floor(at.x());
        int bz = (int) Math.floor(at.z());
        int from = (int) Math.floor(at.y());
        for (int dy = 0; dy <= (int) Math.ceil(most); dy++) {
            BlockPos pos = new BlockPos(bx, from + dy, bz);
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) return Math.max(0.0, from + dy - at.y());
        }
        return most;
    }

    /**
     * The ground at (x, z) near height {@code y}: the top of the first solid block (with room above it) from a little over {@code y}
     * down a few blocks; NaN when there is none that near (a drop, or solid all the way up: a wall).
     */
    static double ground(ServerLevel level, double x, double z, double y) {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        int top = (int) Math.floor(y) + 3;
        for (int by = top; by >= top - 11; by--) {
            BlockPos pos = new BlockPos(bx, by, bz);
            BlockState state = level.getBlockState(pos);
            if (state.getCollisionShape(level, pos).isEmpty()) continue;
            BlockPos above = new BlockPos(bx, by + 1, bz);
            if (!level.getBlockState(above).getCollisionShape(level, above).isEmpty()) {
                if (by == top) return Double.NaN; // solid right up to where we looked from: a wall
                continue;
            }
            return by + 1.0;
        }
        return Double.NaN;
    }

    /**
     * What he aims at within {@code range} blocks: what he clicked, else the nearest body his line of sight passes through (its box,
     * a little grown), else the nearest within a few degrees of it; null for none.
     */
    static LivingEntity aimed(LivingEntity player, int clicked, double range) {
        V3 eye = WarKit.eye(player);
        V3 look = WarKit.look(player);
        LivingEntity best = null;
        double bestAlong = Double.MAX_VALUE;
        LivingEntity near = null;
        double nearScore = Double.MAX_VALUE;
        for (LivingEntity body : WarKit.near(player, range + 2.0)) {
            if (!com.hex.kagune.control.server.KingEffects.valid(player, body)) continue;
            V3 c = WarKit.center(body);
            V3 to = c.sub(eye);
            double d = to.length();
            if (d > range + body.getBbWidth() * 0.5 || d < 1.0E-4) continue;
            if (body.getId() == clicked) return body;
            double along = to.dot(look);
            if (along <= 0.0) continue;
            // The line of sight against its box (grown a little): through it?
            double half = body.getBbWidth() * 0.5 + 0.35;
            V3 closest = eye.add(look.scale(along));
            double off = new V3(closest.x() - c.x(), 0.0, closest.z() - c.z()).length();
            double up = Math.abs(closest.y() - c.y());
            if (off <= half && up <= body.getBbHeight() * 0.5 + 0.35 && along < bestAlong) {
                bestAlong = along;
                best = body;
            }
            double facing = along / d;
            if (facing > 0.96 && d < nearScore) {
                nearScore = d;
                near = body;
            }
        }
        return best != null ? best : near;
    }

    /** A number in [0, 1) from {@code seed} and {@code k}, the same every time. */
    static double noise(long seed, int k) {
        long h = seed * 0x9E3779B97F4A7C15L + k * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        return (h >>> 11) * 0x1.0p-53;
    }
}
