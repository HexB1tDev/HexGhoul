package com.hex.kagune.control.awakening;

import com.hex.kagune.control.king.KingMarkFx;
import com.hex.kagune.control.king.KingMove;
import com.hex.kagune.control.king.WarRules;
import com.hex.kagune.control.network.S2CKingFx;
import com.hex.kagune.control.server.KingEffects;
import com.hex.kagune.control.server.KingServer;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.sound.HexKaguneSounds;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1.0.28: War's held moves on the server ({@link WarRules}), handed their keys (pressed and let go), ticks and blows by {@link WarKit}:
 * <ul>
 *   <li>J, Black Reaper: black centipedes out of his back at what he aims at; they bite and drag it in for as long as the key is held;
 *       brought close, the bladed arm goes through it (bleeding, slowed, stunned, thrown off its feet). Let go, and they go back into
 *       his back.</li>
 *   <li>C, King's Defiance: the arm over his face and the rinkaku closed round him in a cocoon: nothing hurts him while the key is held
 *       (ten seconds at most); whatever strikes it, a spike grows out of it where it was struck, at the striker, and goes back in.</li>
 *   <li>B, Spire Wave: the free arm held out where he looks, the centipedes winding round it, more and bigger the longer it is held;
 *       let go, and they go out along it (up to 25 blocks), slow and heavy, carrying everything before them, and crush it at the
 *       end.</li>
 * </ul>
 * Their bodies go to every client through {@link KingServer#move} (each phase a level of the move), the Spire Wave's mass and the
 * cocoon's spikes through {@link S2CKingFx}.
 */
final class WarHold {
    static final WarHold INSTANCE = new WarHold();

    private static final class Reaper {
        int phase;
        long phaseAt;
        int target;
        boolean held = true;
        /** They bit into it (else, let go or at nothing, only a whiff's wait after). */
        boolean caught;
    }

    private static final class Defy {
        long at;
        long openAt = -1L;
        boolean held = true;
        final Map<Integer, Long> answered = new HashMap<>();
    }

    private static final class Spire {
        long at;
        boolean held = true;
        long sentAt = -1L;
        double charge;
        V3 from;
        V3 dir;
        V3 right;
        V3 up;
        double reach;
        double size;
        double speed;
        boolean crushed;
        /** What the mass carries: its id, and where in the mass it was caught (across it: right, up, blocks). */
        final Map<Integer, double[]> carried = new LinkedHashMap<>();
    }

    private static final class State {
        Reaper reaper;
        Defy defy;
        Spire spire;
    }

    private final Map<UUID, State> states = new HashMap<>();

    private WarHold() {
    }

    private State state(ServerPlayer player) {
        return states.computeIfAbsent(player.getUUID(), id -> new State());
    }

    /** True while one of the held moves has his body (no other ability, no blow). */
    boolean busy(ServerPlayer player) {
        State state = states.get(player.getUUID());
        return state != null && (state.reaper != null || state.defy != null || state.spire != null);
    }

    /** True while the cocoon is closed round him. */
    boolean cocooned(ServerPlayer player) {
        State state = states.get(player.getUUID());
        return state != null && state.defy != null && state.defy.openAt < 0L;
    }

    void off(ServerPlayer player) {
        states.remove(player.getUUID());
    }

    /** Its key pressed ({@code ability} ready: {@link WarKit} has looked), aimed at {@code clicked}. */
    void press(ServerPlayer player, WarRules.Ability ability, int clicked, long now) {
        State state = state(player);
        int slot = ability.ordinal();
        switch (ability) {
            case REAPER -> {
                Reaper r = new Reaper();
                LivingEntity prey = WarPath.aimed(player, clicked, WarRules.REAPER_RANGE);
                r.target = prey == null ? 0 : prey.getId();
                state.reaper = r;
                // The wait counts from when it ends; till then, not again.
                KingServer.cooldown(player, slot, now + WarRules.REAPER_HOLD + 60 + ability.cooldown());
                phase(player, r, WarRules.REAPER_OUT, now);
                WarKit.play(player, WarKit.center(player), HexKaguneSounds.KAGUNE_WHIP, 1.6F, 0.7F);
                WarKit.play(player, WarKit.center(player), HexKaguneSounds.KAGUNE_RESHAPE, 1.2F, 0.8F);
            }
            case DEFIANCE -> {
                Defy d = new Defy();
                d.at = now;
                state.defy = d;
                KingServer.cooldown(player, slot, now + WarRules.DEFY_MOST + WarRules.DEFY_OPEN + ability.cooldown());
                KingServer.move(player, KingMove.DEFY, WarRules.DEFY_HELD, 0);
                WarKit.play(player, WarKit.center(player), HexKaguneSounds.KAGUNE_RESHAPE, 1.6F, 0.6F);
                WarKit.play(player, WarKit.center(player), HexKaguneSounds.KAGUNE_GUARD, 1.4F, 0.5F);
            }
            case SPIRE -> {
                Spire s = new Spire();
                s.at = now;
                state.spire = s;
                KingServer.cooldown(player, slot, now + WarRules.SPIRE_HOLD_MOST + 80 + ability.cooldown());
                KingServer.move(player, KingMove.SPIRE_AIM, 0, 0);
                WarKit.play(player, WarKit.center(player), HexKaguneSounds.KAGUNE_STRAIN, 1.2F, 0.6F);
            }
            default -> {
            }
        }
    }

    /** Its key let go. */
    void release(ServerPlayer player, WarRules.Ability ability, long now) {
        State state = states.get(player.getUUID());
        if (state == null) return;
        switch (ability) {
            case REAPER -> {
                if (state.reaper != null) state.reaper.held = false;
            }
            case DEFIANCE -> {
                if (state.defy != null) state.defy.held = false;
            }
            case SPIRE -> {
                if (state.spire != null) state.spire.held = false;
            }
            default -> {
            }
        }
    }

    /** A blow on him: what is left of it, or -1 if none of these has anything to say to it. */
    float hurt(ServerPlayer player, Entity attacker, Entity direct, float amount, long now) {
        State state = states.get(player.getUUID());
        if (state == null || state.defy == null || state.defy.openAt >= 0L) return -1.0F;
        // Inside the cocoon: nothing reaches him; what struck it is answered.
        Entity at = direct != null ? direct : attacker;
        if (at == null) return 0.0F;
        Defy d = state.defy;
        Long next = d.answered.get(at.getId());
        if (next != null && now < next) return 0.0F;
        d.answered.put(at.getId(), now + WarRules.DEFY_EVERY);
        V3 centre = new V3(player.getX(), player.getY() + 1.05, player.getZ());
        V3 to = WarKit.center(at).sub(centre);
        V3 dir = to.length() < 1.0E-3 ? WarKit.facing(player) : to.normalize();
        V3 surface = centre.add(dir.scale(WarRules.DEFY_RADIUS));
        V3 tip = surface.add(dir.scale(WarRules.DEFY_SPIKE));
        WarKit.fx(player, S2CKingFx.DEFY_SPIKE, new int[] {at.getId()}, surface, tip);
        WarKit.play(player, surface, HexKaguneSounds.KAGUNE_WHIP, 1.4F, 1.1F);
        WarKit.play(player, surface, HexKaguneSounds.KAGUNE_GUARD, 1.2F, 1.3F);
        LivingEntity striker = attacker instanceof LivingEntity living && living != player ? living : null;
        if (striker != null && striker.isAlive()) {
            double reach = WarRules.DEFY_RADIUS + WarRules.DEFY_SPIKE + striker.getBbWidth() * 0.5 + 0.4;
            if (WarKit.center(striker).distance(centre) <= reach && WarKit.hurt(player, striker, WarRules.DEFY_SPIKE_DAMAGE, centre, 0.7, 0.2)) {
                if (striker.isAlive()) KingEffects.bleed(striker, player, WarRules.DEFY_BLEED, 1);
                WarKit.mark(player, KingMarkFx.BLOOD, WarKit.center(striker), dir, 0.9, striker);
                WarKit.play(player, WarKit.center(striker), HexKaguneSounds.FLESH_IMPACT, 1.4F, 0.8F);
            }
        }
        return 0.0F;
    }

    // ------------------------------------------------------------------ the clock

    void tick(ServerPlayer player, long now) {
        State state = states.get(player.getUUID());
        if (state == null) return;
        if (state.reaper != null) reaper(player, state, now);
        if (state.defy != null) defy(player, state, now);
        if (state.spire != null) spire(player, state, now);
    }

    // ------------------------------------------------------------------ J: Black Reaper

    private static void phase(ServerPlayer player, Reaper r, int phase, long now) {
        r.phase = phase;
        r.phaseAt = now;
        KingServer.move(player, KingMove.REAPER, phase, r.target);
    }

    private void reaper(ServerPlayer player, State state, long now) {
        Reaper r = state.reaper;
        long age = now - r.phaseAt;
        LivingEntity prey = r.target == 0 ? null : WarKit.byId(player, r.target, WarRules.REAPER_RANGE + 10.0);
        boolean gone = prey == null || !prey.isAlive();
        switch (r.phase) {
            case WarRules.REAPER_OUT -> {
                if (age < WarRules.REAPER_REACH) return;
                if (gone || !r.held || WarKit.center(prey).distance(WarKit.center(player)) > WarRules.REAPER_RANGE + 4.0) {
                    if (gone) r.target = 0;
                    phase(player, r, WarRules.REAPER_BACK, now);
                    return;
                }
                // They bite into it.
                V3 c = WarKit.center(prey);
                WarKit.hurt(player, prey, WarRules.REAPER_BITE, c, 0.0, 0.0);
                KingEffects.stun(prey, 12);
                WarKit.mark(player, KingMarkFx.BLOOD, c, c.sub(WarKit.center(player)).normalize().scale(-1.0), 0.8, prey);
                WarKit.play(player, c, HexKaguneSounds.FLESH_IMPACT, 1.3F, 0.9F);
                r.caught = true;
                phase(player, r, WarRules.REAPER_DRAG, now);
            }
            case WarRules.REAPER_DRAG -> {
                if (gone || !r.held || age > WarRules.REAPER_HOLD || WarKit.center(prey).distance(WarKit.center(player)) > WarRules.REAPER_RANGE + 6.0) {
                    if (gone) r.target = 0;
                    phase(player, r, WarRules.REAPER_BACK, now);
                    return;
                }
                V3 face = WarKit.facing(player);
                V3 stop = new V3(player.getX(), player.getY(), player.getZ()).add(face.scale(1.3 + prey.getBbWidth() * 0.5));
                V3 feet = new V3(prey.getX(), prey.getY(), prey.getZ());
                V3 flat = stop.sub(feet).withY(0.0);
                double d = flat.length();
                if (new V3(prey.getX() - player.getX(), 0.0, prey.getZ() - player.getZ()).length() <= WarRules.REAPER_CLOSE + prey.getBbWidth() * 0.5) {
                    impale(player, r, prey, now);
                    return;
                }
                double step = Math.min(WarRules.REAPER_PULL, Math.max(0.15, d * WarRules.REAPER_PULL_SHARE));
                V3 way = flat.scale(step / Math.max(1.0E-4, d));
                double rise = (stop.y() - feet.y()) * 0.3 + (KingEffects.grounded(prey) ? 0.16 : 0.08);
                KingCombos.motion(prey, way.x(), Math.max(-0.4, Math.min(0.5, rise)), way.z());
                prey.fallDistance = 0.0F;
                if (age % 10L == 0L) KingEffects.stun(prey, 12);
                if (age % 20L == 0L) KingEffects.slow(prey, player, 30, 2);
                if (age % 7L == 0L) WarKit.play(player, WarKit.center(prey), HexKaguneSounds.KAGUNE_STRAIN, 1.0F, 0.7F + 0.02F * (age % 5L));
            }
            case WarRules.REAPER_THROUGH -> {
                if (!gone) {
                    // Held up on the blade before him.
                    V3 face = WarKit.facing(player);
                    V3 on = new V3(player.getX(), player.getY() + 0.35, player.getZ()).add(face.scale(1.25 + prey.getBbWidth() * 0.5));
                    if (WarPath.open(player.serverLevel(), on)) {
                        prey.teleportTo(on.x(), on.y(), on.z());
                        prey.setDeltaMovement(new Vec3(0.0, 0.0, 0.0));
                        prey.hurtMarked = true;
                    }
                    prey.fallDistance = 0.0F;
                }
                if (age < WarRules.REAPER_IMPALE) return;
                if (!gone) {
                    // Thrown off the blade, off its feet.
                    V3 face = WarKit.facing(player);
                    Vec3 launch = new Vec3(face.x() * 0.8, 0.35, face.z() * 0.8);
                    KingEffects.tumble(player, prey, launch);
                    KingCombos.motion(prey, launch.x, launch.y, launch.z);
                    WarKit.play(player, WarKit.center(prey), HexKaguneSounds.KAGUNE_WHIP, 1.4F, 0.6F);
                }
                phase(player, r, WarRules.REAPER_BACK, now);
            }
            default -> {
                if (age < WarRules.REAPER_RETRACT) return;
                state.reaper = null;
                KingServer.move(player, KingMove.NONE, 0, 0);
                int slot = WarRules.Ability.REAPER.ordinal();
                KingServer.cooldown(player, slot, now + (r.caught ? WarRules.Ability.REAPER.cooldown() : WarRules.REAPER_WHIFF_COOLDOWN));
            }
        }
    }

    /** Close: the bladed arm through it. */
    private static void impale(ServerPlayer player, Reaper r, LivingEntity prey, long now) {
        phase(player, r, WarRules.REAPER_THROUGH, now);
        V3 c = WarKit.center(prey);
        V3 way = c.sub(WarKit.center(player)).withY(0.0).normalize();
        if (WarKit.hurt(player, prey, WarRules.REAPER_IMPALE_DAMAGE, c.sub(way), 0.0, 0.0) && prey.isAlive()) {
            KingEffects.bleed(prey, player, WarRules.REAPER_BLEED, 2);
            KingEffects.slow(prey, player, WarRules.REAPER_SLOW, 2);
            KingEffects.stun(prey, WarRules.REAPER_STUN);
        }
        WarKit.mark(player, KingMarkFx.GORE, c, way, 1.4, prey);
        WarKit.mark(player, KingMarkFx.BLOOD, c.add(way.scale(0.5)), way, 1.2, prey);
        WarKit.play(player, c, HexKaguneSounds.FLESH_IMPACT, 2.0F, 0.6F);
        WarKit.play(player, c, HexKaguneSounds.BONE_CRUSH, 1.6F, 0.8F);
        WarKit.vanilla(player, c, "entity.player.attack.crit", 1.4F, 0.6F);
    }

    // ------------------------------------------------------------------ C: King's Defiance

    private void defy(ServerPlayer player, State state, long now) {
        Defy d = state.defy;
        if (d.openAt < 0L) {
            // (Let go at once, it still closes all the way round him before it opens.)
            if (!d.held && now - d.at >= WarRules.DEFY_CLOSE + 6 || now - d.at >= WarRules.DEFY_MOST) {
                d.openAt = now;
                KingServer.move(player, KingMove.DEFY, WarRules.DEFY_OPENING, 0);
                int slot = WarRules.Ability.DEFIANCE.ordinal();
                KingServer.cooldown(player, slot, now + WarRules.Ability.DEFIANCE.cooldown());
                WarKit.play(player, WarKit.center(player), HexKaguneSounds.KAGUNE_RESHAPE, 1.4F, 0.8F);
                return;
            }
            // Rooted where he stands (still falling, if he was in the air).
            KingCombos.motion(player, 0.0, KingEffects.grounded(player) ? 0.0 : -0.2, 0.0); // 1.0.36: (the server's own speed for him is stale)
            if ((now - d.at) % 30L == 0L) WarKit.play(player, WarKit.center(player), HexKaguneSounds.KAGUNE_STRAIN, 0.8F, 0.5F);
            return;
        }
        if (now - d.openAt >= WarRules.DEFY_OPEN) {
            state.defy = null;
            KingServer.move(player, KingMove.NONE, 0, 0);
        }
    }

    // ------------------------------------------------------------------ B: Spire Wave

    private void spire(ServerPlayer player, State state, long now) {
        Spire s = state.spire;
        if (s.sentAt < 0L) {
            long held = now - s.at;
            if (s.held && held < WarRules.SPIRE_HOLD_MOST) {
                if (held % 10L == 5L && held <= WarRules.SPIRE_CHARGE) {
                    WarKit.play(player, WarKit.center(player), HexKaguneSounds.KAGUNE_RESHAPE, 0.9F, 0.6F + 0.5F * (float) WarRules.spireCharge(held));
                }
                return;
            }
            send(player, s, now);
            return;
        }
        long age = now - s.sentAt;
        int arrive = WarRules.spireArrive(s.reach, s.speed);
        double front = WarRules.spireFront(age, s.reach, s.speed);
        V3 head = s.from.add(s.dir.scale(front));
        if (age <= arrive) {
            // What the head of the mass reaches, it takes along.
            for (LivingEntity body : WarKit.near(player, head, s.size + 2.5)) {
                if (s.carried.containsKey(body.getId()) || !KaguneServerController.validSwipeTarget(player, body)) continue;
                V3 c = WarKit.center(body);
                V3 rel = c.sub(s.from);
                double along = rel.dot(s.dir);
                V3 across = rel.sub(s.dir.scale(along));
                if (along < front - s.size * 1.5 - 1.0 || along > front + 0.8 || across.length() > s.size + body.getBbWidth() * 0.5) continue;
                s.carried.put(body.getId(), new double[] {across.dot(s.right) * 0.55, across.dot(s.up) * 0.55});
                if (WarKit.hurt(player, body, WarRules.SPIRE_CARRY_DAMAGE, c.sub(s.dir), 0.0, 0.0) && body.isAlive()) {
                    KingEffects.stun(body, arrive - (int) age + WarRules.SPIRE_CRUSH + 20);
                    WarKit.mark(player, KingMarkFx.BLOOD, c, s.dir, 0.7, body);
                }
                WarKit.play(player, c, HexKaguneSounds.FLESH_IMPACT, 1.2F, 0.8F);
            }
        }
        // What it carries, carried along at its head (squeezed in toward its middle as it goes).
        if (age <= arrive + WarRules.SPIRE_CRUSH) {
            double squeeze = age >= arrive ? 0.25 : 1.0 - 0.5 * Math.min(1.0, age / (double) arrive);
            for (Map.Entry<Integer, double[]> e : s.carried.entrySet()) {
                LivingEntity body = WarKit.byId(player, e.getKey(), s.reach + 12.0);
                if (body == null || !body.isAlive()) continue;
                double[] off = e.getValue();
                V3 want = head.add(s.dir.scale(0.4)).add(s.right.scale(off[0] * squeeze)).add(s.up.scale(off[1] * squeeze));
                V3 move = want.sub(WarKit.center(body));
                double most = s.speed * 1.8 + 0.4;
                if (move.length() > most) move = move.normalize().scale(most);
                KingCombos.motion(body, move.x(), move.y() + 0.08, move.z());
                body.fallDistance = 0.0F;
            }
        }
        if (age % 4L == 0L && age <= arrive) {
            WarKit.play(player, head, HexKaguneSounds.KAGUNE_WHIP, 1.0F, 0.5F + 0.3F * (float) s.charge);
            WarKit.mark(player, KingMarkFx.SOOT, head, s.dir.scale(-1.0), 0.5 + 0.4 * s.charge, null);
        }
        if (!s.crushed && age >= arrive) {
            s.crushed = true;
            crush(player, s, head);
        }
        if (age == arrive + WarRules.SPIRE_CRUSH) {
            // Let go: what was crushed is slammed down out of it.
            for (Integer id : s.carried.keySet()) {
                LivingEntity body = WarKit.byId(player, id, s.reach + 12.0);
                if (body != null && body.isAlive()) KingCombos.motion(body, s.dir.x() * 0.2, -0.9, s.dir.z() * 0.2);
            }
        }
        if (age >= WarRules.spireTicks(s.reach, s.speed)) {
            state.spire = null;
            KingServer.move(player, KingMove.NONE, 0, 0);
            KingServer.cooldown(player, WarRules.Ability.SPIRE.ordinal(), now + WarRules.Ability.SPIRE.cooldown());
        }
    }

    /** Let go: the mass sent out along his look, as far as it goes before it meets a wall. */
    private static void send(ServerPlayer player, Spire s, long now) {
        s.sentAt = now;
        s.charge = WarRules.spireCharge(now - s.at);
        V3 look = WarKit.look(player);
        // Level-ish: a little up or down with the look, never straight into the ground or the sky.
        double y = Math.max(-0.45, Math.min(0.35, look.y()));
        V3 flat = look.withY(0.0);
        flat = flat.length() < 1.0E-4 ? WarKit.facing(player) : flat.normalize();
        s.dir = flat.scale(Math.sqrt(1.0 - y * y)).add(0.0, y, 0.0).normalize();
        s.right = new V3(-flat.z(), 0.0, flat.x());
        s.up = s.right.cross(s.dir).normalize();
        if (s.up.y() < 0.0) s.up = s.up.scale(-1.0);
        V3 eye = WarKit.eye(player);
        // From his outstretched left hand.
        s.from = eye.add(0.0, -0.45, 0.0).sub(s.right.scale(0.3)).add(s.dir.scale(0.7));
        double reach = WarRules.spireReach(s.charge);
        V3 end = s.from.add(s.dir.scale(reach));
        BlockHitResult hit = player.serverLevel().clip(new ClipContext(new Vec3(s.from.x(), s.from.y(), s.from.z()), new Vec3(end.x(), end.y(), end.z()),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.BLOCK) {
            Vec3 at = hit.getLocation();
            reach = Math.max(2.0, new V3(at.x, at.y, at.z).distance(s.from) - 0.4);
        }
        s.reach = reach;
        s.size = WarRules.spireSize(s.charge);
        s.speed = WarRules.spireSpeed(s.charge);
        int arms = WarRules.spireArms(s.charge);
        float[] data = {(float) s.from.x(), (float) s.from.y(), (float) s.from.z(), (float) s.dir.x(), (float) s.dir.y(), (float) s.dir.z(), (float) s.reach,
                (float) s.size, (float) s.speed, (float) s.charge, arms};
        com.hex.kagune.control.network.ControlNetwork.broadcast(player, new S2CKingFx(player.getUUID(), S2CKingFx.SPIRE_WAVE,
                new int[] {(int) (now & 0x7FFFFFFF)}, data));
        KingServer.move(player, KingMove.SPIRE_SEND, 0, 0);
        WarKit.play(player, s.from, HexKaguneSounds.KAGUNE_WHIP, 2.0F, 0.45F);
        WarKit.play(player, s.from, HexKaguneSounds.MANIFEST_RIP, 1.6F, 0.6F);
        WarKit.vanilla(player, s.from, "entity.ravager.roar", 0.8F, 1.6F);
    }

    /** At the end: the centipedes close on all they carry and crush it. */
    private static void crush(ServerPlayer player, Spire s, V3 head) {
        float damage = (float) (WarRules.SPIRE_CRUSH_LOW + (WarRules.SPIRE_CRUSH_HIGH - WarRules.SPIRE_CRUSH_LOW) * s.charge);
        java.util.Set<Integer> done = new java.util.HashSet<>();
        for (Map.Entry<Integer, double[]> e : s.carried.entrySet()) {
            LivingEntity body = WarKit.byId(player, e.getKey(), s.reach + 12.0);
            if (body != null && crushOne(player, body, damage, head)) done.add(body.getId());
        }
        for (LivingEntity body : WarKit.near(player, head, s.size + 1.5)) {
            if (done.contains(body.getId()) || !KaguneServerController.validSwipeTarget(player, body)) continue;
            if (WarKit.center(body).distance(head) <= s.size + 1.2 + body.getBbWidth() * 0.5) crushOne(player, body, damage * 0.7F, head);
        }
        WarKit.mark(player, KingMarkFx.GORE, head, new V3(0.0, 1.0, 0.0), 1.4 + s.charge, null);
        WarKit.mark(player, KingMarkFx.QUAKE, head.add(0.0, -1.0, 0.0), V3.ZERO, 1.0 + s.charge, null);
        WarKit.play(player, head, HexKaguneSounds.BONE_CRUSH, 2.4F, 0.5F);
        WarKit.play(player, head, HexKaguneSounds.KAGUNE_SLAM, 2.0F, 0.6F);
        WarKit.vanilla(player, head, "entity.generic.explode", 1.0F + (float) s.charge, 1.1F);
    }

    private static boolean crushOne(ServerPlayer player, LivingEntity body, float damage, V3 head) {
        if (!body.isAlive()) return false;
        boolean landed = WarKit.hurt(player, body, damage, head.add(0.0, 1.0, 0.0), 0.0, 0.0);
        if (landed && body.isAlive()) {
            KingEffects.bleed(body, player, 100, 1);
            KingEffects.slow(body, player, 60, 1);
            KingEffects.stun(body, 30);
        }
        WarKit.mark(player, KingMarkFx.BLOOD, WarKit.center(body), new V3(0.0, 1.0, 0.0), 1.1, body);
        return landed;
    }
}
