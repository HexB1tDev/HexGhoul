package com.hex.kagune.control.awakening;

import com.hex.kagune.control.king.KingMove;
import com.hex.kagune.control.king.WarRules;
import com.hex.kagune.control.king.WarStrike;
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
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;

/**
 * 1.0.6: War's six abilities on the server ({@link WarRules}), as {@link KingServer}'s kit: every key, tick and blow taken reaches it
 * while War is worn. 1.0.13: the six as they are now, each a small state machine on the wearer:
 * <ul>
 *   <li>King's Execution (1.0.27, {@link WarPath}): the blitz at a body within reach of his aim, or the dash and the ground's spikes.</li>
 *   <li>Black Reaper (1.0.28, {@link WarHold}): held: centipedes out of his back dragging what he aims at in, the blade through it.</li>
 *   <li>Centipede Dominion: the small Dragon ({@link WarWyrm}).</li>
 *   <li>Spire Wave (1.0.28, {@link WarHold}): held, charging round the arm; let go, the centipedes out along it, carrying, crushing.</li>
 *   <li>King's Defiance (1.0.28, {@link WarHold}): held: the cocoon; nothing hurts him in it, and its spikes answer every blow.</li>
 *   <li>Black Sun (1.0.27, {@link WarPath}): the ultimate: the uppercut, the leap, the punch that throws out a ring of air.</li>
 * </ul>
 * And 1.0.7's instinct: the six rinkaku hunt on their own all the while; 1.0.14: and his M1 ({@link WarStrike}), blow by blow. The body's moves go to every client through
 * {@link KingServer#move}, what they do in the world through {@link S2CKingFx}, the waits through {@link KingServer#cooldown}.
 */
final class WarKit implements KingServer.Kit {
    static final WarKit INSTANCE = new WarKit();


    /** 1.0.7: a spike of RC one of the rinkaku fired at its prey on its own. */
    private static final class Spike {
        V3 at;
        V3 dir;
        int life;
        final int target;

        Spike(V3 at, V3 dir, int target) {
            this.at = at;
            this.dir = dir;
            this.target = target;
        }
    }

    /** One wearer's abilities in flight. */
    private static final class State {
        /** 1.0.7: when each rinkaku may strike again on its own, the bodies that struck him (id to until when), its spikes. */
        final long[] tendrilReady = new long[6];
        final Map<Integer, Long> prey = new HashMap<>();
        final List<Spike> spikes = new ArrayList<>();
        /** 1.0.35: the last lash of any of the six, the last spike. */
        long lashAt = Long.MIN_VALUE / 2;
        long spikeAt = Long.MIN_VALUE / 2;
        long safeUntil = Long.MIN_VALUE;
        // Centipede Dominion (1.0.28: Black Reaper, Spire Wave and King's Defiance are WarHold's)
        WarWyrm wyrm;
        // 1.0.14: the M1: the blow in flight (null: none), since when, at whom; the last one and when it ended; a click kept for the
        // next; the Verdict's recovery; what this blow has hit.
        WarStrike strike;
        long strikeAt;
        int strikeTarget;
        WarStrike lastStrike;
        long strikeEnded = Long.MIN_VALUE / 2;
        long queuedAt = Long.MIN_VALUE;
        int queuedTarget;
        long recoverUntil = Long.MIN_VALUE;
        final Set<Integer> struck = new HashSet<>();
        // 1.0.20: the fighting grammar (KingCombo): blows chained on the ground and in the air, the flags the kept click came with,
        // whether this blow is thrown in the air, the launch's chase, the heavy charging (since when, at whom), what the ender is worth.
        int chained;
        int airHits;
        int queuedFlags;
        boolean strikeAir;
        long chaseUntil = Long.MIN_VALUE;
        long chargeAt = -1L;
        int chargeTarget;
        double worth = 1.0;
    }

    private final Map<UUID, State> states = new HashMap<>();

    private WarKit() {
    }

    private State state(ServerPlayer player) {
        return states.computeIfAbsent(player.getUUID(), id -> new State());
    }

    // ------------------------------------------------------------------ keys

    @Override
    public void key(ServerPlayer player, int slot, int action, int target) {
        WarRules.Ability ability = WarRules.Ability.bySlot(slot);
        if (ability == null) return;
        State state = state(player);
        long now = player.serverLevel().getGameTime();
        if (action == WarRules.RELEASE) {
            WarHold.INSTANCE.release(player, ability, now); // 1.0.28: the held moves end when their key is let go
            return;
        }
        // The small Dragon has the body to itself (1.0.27: so do the warpath's moves; 1.0.28: and the held ones).
        if (state.wyrm != null || WarPath.INSTANCE.busy(player, now) || WarHold.INSTANCE.busy(player)) return;
        if (!ready(player, ability, now)) return;
        state.strike = null; // an ability cuts a blow off
        state.queuedAt = Long.MIN_VALUE;
        state.chargeAt = -1L; // and a heavy charging
        switch (ability) {
            case EXECUTION -> WarPath.INSTANCE.execution(player, target, now); // 1.0.27: the blitz, or the dash and the ground's spikes
            case REAPER, SPIRE, DEFIANCE -> WarHold.INSTANCE.press(player, ability, target, now); // 1.0.28: held
            case DOMINION -> {
                state.wyrm = new WarWyrm(player);
                KingServer.cooldown(player, slot, now + ability.cooldown());
                KingServer.move(player, KingMove.WYRM, 0, 0);
            }
            case SUN -> WarPath.INSTANCE.sunder(player, target, now); // 1.0.27: the ultimate
        }
    }

    /** True if {@code ability} is ready; if not, says how long it has left. */
    private static boolean ready(ServerPlayer player, WarRules.Ability ability, long now) {
        long at = KingServer.ready(player, ability.ordinal());
        if (at <= now) return true;
        player.displayClientMessage(Component.literal("§c" + ability.displayName() + "§7: " + String.format(java.util.Locale.ROOT, "%.1f", (at - now) / 20.0) + " s"), true);
        return false;
    }

    /** 1.0.29: Centipede Dominion driven: the keys he holds (1.0.39: and where he looks). */
    @Override
    public void drive(ServerPlayer player, int keys, int aim) {
        State state = states.get(player.getUUID());
        if (state != null && state.wyrm != null) state.wyrm.input(keys, aim);
    }

    // ------------------------------------------------------------------ blows taken

    @Override
    public float hurt(ServerPlayer player, Entity attacker, float amount) {
        return this.hurt(player, attacker, null, amount);
    }

    @Override
    public float hurt(ServerPlayer player, Entity attacker, Entity direct, float amount) {
        State state = states.get(player.getUUID());
        if (state == null) return amount;
        long now = player.serverLevel().getGameTime();
        if (attacker instanceof LivingEntity striker && striker != player) state.prey.put(striker.getId(), now + WarRules.PREY_MEMORY);
        if (state.wyrm != null) return 0.0F; // he is the small Dragon: his body is not there to hurt
        if (WarPath.INSTANCE.untouchable(player, now)) return 0.0F; // 1.0.27: through the blitz and Black Sun
        if (attacker == null && WarPath.INSTANCE.fallSafe(player, now)) return 0.0F; // and their falls
        if (attacker == null && now < state.safeUntil) return 0.0F; // the small Dragon's own fall
        // 1.0.28: King's Defiance: inside the cocoon nothing reaches him, and what struck it is answered with a spike.
        float held = WarHold.INSTANCE.hurt(player, attacker, direct, amount, now);
        return held >= 0.0F ? held : amount;
    }

    // ------------------------------------------------------------------ 1.0.14: the M1

    @Override
    public void strike(ServerPlayer player, int target) {
        this.strike(player, target, 0);
    }

    /** 1.0.20: an M1 with its flags (KingCombo): the chain's next blow, the launcher, an air blow or the spike. */
    @Override
    public void strike(ServerPlayer player, int target, int flags) {
        State state = state(player);
        long now = player.serverLevel().getGameTime();
        if (state.wyrm != null) {
            state.wyrm.lunge(); // 1.0.29: as the small Dragon, M1 is its bite
            return;
        }
        // An ability has the body, or the heavy is charging (1.0.28: or one of the warpath's moves: no blow breaks into the blitz).
        if (abilityBusy(state, now) || state.chargeAt >= 0L || WarPath.INSTANCE.busy(player, now) || WarHold.INSTANCE.busy(player)) return;
        if (state.strike != null) {
            // Kept, and thrown the moment this blow reaches its chain tick.
            state.queuedAt = now;
            state.queuedTarget = target;
            state.queuedFlags = flags;
            return;
        }
        if (now < state.recoverUntil) return;
        begin(player, state, target, now, flags);
    }

    /**
     * 1.0.27: War's RMB: the spike cone ({@link WarPath#cone}), on the press (1.0.20's heavy, its charge, ender and spike, gone for
     * War: the M1's air chain still ends in its own spike).
     */
    @Override
    public void heavy(ServerPlayer player, int flags, int target) {
        State state = state(player);
        long now = player.serverLevel().getGameTime();
        if (state.wyrm != null) {
            if ((flags & com.hex.kagune.control.king.KingCombo.PRESS) != 0) state.wyrm.slam(); // 1.0.29: as the small Dragon, RMB slams
            return;
        }
        if (abilityBusy(state, now) || WarPath.INSTANCE.busy(player, now) || WarHold.INSTANCE.busy(player)
                || (flags & com.hex.kagune.control.king.KingCombo.PRESS) == 0) return;
        state.strike = null;
        state.queuedAt = Long.MIN_VALUE;
        WarPath.INSTANCE.cone(player, now);
    }

    /** True while an ability has his body: no blow then. */
    static boolean abilityBusy(State state, long now) {
        // (1.0.27: the warpath's moves are WarPath's own, 1.0.28: the held ones WarHold's: asked of them where this is.)
        return state.wyrm != null;
    }

    /**
     * The next blow (1.0.20: by the fighting grammar, KingCombo): the chain's (or the jab, the chain gone cold), the launcher, an air
     * blow, the spike; at what was clicked or the nearest body before him.
     */
    private void begin(ServerPlayer player, State state, int target, long now, int flags) {
        boolean air = !KingEffects.grounded(player) || now < state.chaseUntil;
        if (now - state.strikeEnded > WarStrike.CHAIN_WINDOW) state.chained = 0;
        if (!air) state.airHits = 0;
        com.hex.kagune.control.king.KingCombo.Blow kind = com.hex.kagune.control.king.KingCombo.blow(state.chained, state.airHits, flags, air);
        WarStrike blow = switch (kind) {
            case LAUNCH -> WarStrike.ASCENSION;
            case AIR, SPIKE -> WarStrike.air(state.airHits);
            default -> WarStrike.after(state.lastStrike, now - state.strikeEnded);
        };
        switch (kind) {
            case LAUNCH -> state.chained = 0;
            case AIR, SPIKE -> state.airHits++;
            default -> state.chained = blow == WarStrike.JAB ? 1 : state.chained + 1;
        }
        start(player, state, blow, target, now, kind == com.hex.kagune.control.king.KingCombo.Blow.AIR || kind == com.hex.kagune.control.king.KingCombo.Blow.SPIKE);
    }

    /** Blow {@code blow} begun now ({@code air}: thrown in the air), at what was clicked or the nearest body before him. */
    private void start(ServerPlayer player, State state, WarStrike blow, int target, long now, boolean air) {
        LivingEntity prey = lockOn(player, target);
        state.strike = blow;
        state.strikeAt = now;
        state.strikeAir = air;
        state.strikeTarget = prey == null ? 0 : prey.getId();
        state.lastStrike = blow;
        state.queuedAt = Long.MIN_VALUE;
        state.struck.clear();
        // 1.0.40: on its way: another's M1 landing on him before it lands meets it (KaguneClash), and both stop.
        com.hex.kagune.control.server.KaguneClash.swing(player, now, blow.impact, blow.reach, () -> state.strike == blow && state.strikeAt == now,
                () -> clashed(player, state));
        KingServer.move(player, KingMove.STRIKE, blow.ordinal(), state.strikeTarget);
        fx(player, S2CKingFx.SWING, new int[] {blow.ordinal(), state.strikeTarget}, facing(player));
        if (blow == WarStrike.VERDICT || blow == WarStrike.CALAMITY) {
            play(player, center(player), HexKaguneSounds.KAGUNE_STRAIN, 1.2F, 0.55F);
        }
        if (air) {
            // Held up in the air for the blow: his fall forgotten.
            player.fallDistance = 0.0F;
        }
    }

    /** 1.0.40: his blow met another's coming back (KaguneClash): cut off, nothing queued after it, a moment before he can strike again. */
    private static void clashed(ServerPlayer player, State state) {
        long now = player.serverLevel().getGameTime();
        state.strike = null;
        state.queuedAt = Long.MIN_VALUE;
        state.strikeEnded = now;
        state.recoverUntil = now + com.hex.kagune.control.ClashRules.STAGGER;
        state.chained = 0;
        state.airHits = 0;
        KingServer.move(player, KingMove.NONE, 0, 0);
    }

    /** What he strikes at: what he clicked, else the nearest body within about 25 degrees of his aim he could step in to. */
    private static LivingEntity lockOn(ServerPlayer player, int target) {
        V3 eye = eye(player);
        V3 look = look(player);
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (LivingEntity body : near(player, WarStrike.STEP_IN_RANGE + 1.0)) {
            if (!KaguneServerController.validSwipeTarget(player, body)) continue;
            V3 to = center(body).sub(eye);
            double d = to.length();
            if (d > WarStrike.STEP_IN_RANGE || d < 1.0E-4) continue;
            if (body.getId() == target) return body;
            double facing = to.scale(1.0 / d).dot(look);
            if (facing < 0.9) continue;
            double score = d * (2.0 - facing);
            if (score < bestScore) {
                bestScore = score;
                best = body;
            }
        }
        return best;
    }

    private void striking(ServerPlayer player, State state, long now) {
        WarStrike blow = state.strike;
        if (blow == null) return;
        long age = now - state.strikeAt;
        LivingEntity prey = byId(player, state.strikeTarget, WarStrike.STEP_IN_RANGE + 4.0);
        if (prey != null && age < blow.impact) stepIn(player, blow, prey, age);
        if (age == blow.impact - 1) {
            // The air cut.
            play(player, center(player), HexKaguneSounds.KAGUNE_WHIP, 0.5F, blow.kind == WarStrike.Kind.BLADE ? 0.9F : 1.6F);
        }
        if (age == blow.impact) land(player, state, blow, prey, now);
        if (state.strike != blow) return; // 1.0.40: it met another's (KaguneClash): cut off
        if (age >= blow.chain && state.queuedAt >= state.strikeAt && !blow.finisher()) {
            // The next blow out of this one's hold.
            state.strikeEnded = now;
            state.strike = null;
            begin(player, state, state.queuedTarget, now, state.queuedFlags);
            return;
        }
        if (age >= blow.duration) {
            state.strike = null;
            state.strikeEnded = now;
            if (blow.finisher()) {
                state.recoverUntil = now + (blow.special() ? 6 : WarStrike.RECOVERY);
                state.chained = 0;
                state.airHits = 0;
            }
            KingServer.move(player, KingMove.NONE, 0, 0);
        }
    }

    /** 1.0.20: the heavy charging: held to the end it goes off by itself; whatever he spiked that lands breaks the ground there. */
    private void combos(ServerPlayer player, State state, long now) {
        if (state.chargeAt >= 0L && now - state.chargeAt >= com.hex.kagune.control.king.KingCombo.CHARGE_MAX) state.chargeAt = -1L;
        if (state.chargeAt >= 0L && (now - state.chargeAt) % 5 == 0) {
            mark(player, com.hex.kagune.control.king.KingMarkFx.SOOT, center(player).add(0.0, -0.4, 0.0), new V3(0.0, 1.0, 0.0), 0.6, null);
        }
        for (KingCombos.Landing landing : KingCombos.landed(player, now)) {
            V3 at = landing.at();
            mark(player, com.hex.kagune.control.king.KingMarkFx.QUAKE, at, V3.ZERO, 1.6 * landing.power(), null);
            mark(player, com.hex.kagune.control.king.KingMarkFx.GORE, at.add(0.0, 0.4, 0.0), new V3(0.0, 1.0, 0.0), landing.power(), landing.body());
            play(player, at, HexKaguneSounds.KAGUNE_SLAM, 1.8F, 0.6F);
            play(player, at, HexKaguneSounds.BONE_CRUSH, 1.2F, 0.7F);
            for (LivingEntity body : near(player, at, com.hex.kagune.control.king.KingCombo.CRATER + 1.0)) {
                if (!KaguneServerController.validSwipeTarget(player, body)) continue;
                if (new V3(body.getX() - at.x(), 0.0, body.getZ() - at.z()).length() > com.hex.kagune.control.king.KingCombo.CRATER) continue;
                if (hurt(player, body, com.hex.kagune.control.king.KingCombo.CRATER_DAMAGE * (float) landing.power(), at, 0.3, 0.45)) {
                    KingEffects.stun(body, 20);
                }
            }
        }
    }

    /** Closes the gap over the wind-up, so the blow lands on the impact tick: never faster than the step-in speed. */
    private static void stepIn(ServerPlayer player, WarStrike blow, LivingEntity prey, long age) {
        V3 to = new V3(prey.getX() - player.getX(), 0.0, prey.getZ() - player.getZ());
        double distance = to.length();
        double stop = blow.stand(prey.getBbWidth());
        if (distance <= stop + 0.05) return;
        double left = Math.max(1.0, blow.impact - age);
        double speed = Math.min(WarStrike.STEP_IN_SPEED, (distance - stop) / left);
        V3 heading = to.scale(1.0 / distance);
        player.setDeltaMovement(heading.x() * speed, 0.0, heading.z() * speed); // 1.0.36: (never the server's stale fall/launch speed)
        player.hurtMarked = true;
        if (player.connection != null) player.connection.send(new ClientboundSetEntityMotionPacket(player));
    }

    /** The impact tick: everything the blow catches within its reach is hit once; every client sees where. */
    private void land(ServerPlayer player, State state, WarStrike blow, LivingEntity prey, long now) {
        V3 eye = eye(player);
        V3 chest = center(player);
        V3 facing = prey != null ? new V3(prey.getX() - player.getX(), 0.0, prey.getZ() - player.getZ()) : facing(player);
        facing = facing.length() < 1.0E-4 ? facing(player) : facing.normalize();
        V3 right = new V3(-facing.z(), 0.0, facing.x());
        V3 knock = facing.scale(blow.back).add(right.scale(blow.sideways)).add(0.0, blow.up, 0.0);
        // The Verdict's rinkaku come up through what the blade comes down on, and catch what is round it.
        V3 verdict = prey != null ? new V3(prey.getX(), prey.getY(), prey.getZ())
                : new V3(player.getX(), player.getY(), player.getZ()).add(facing.scale(3.0));
        List<LivingEntity> hit = new ArrayList<>();
        List<V3> contacts = new ArrayList<>();
        double reach = blow.reach;
        for (LivingEntity body : near(player, reach + 3.0)) {
            if (state.struck.contains(body.getId()) || !KaguneServerController.validSwipeTarget(player, body)) continue;
            V3 c = center(body);
            V3 to = c.sub(eye);
            boolean caught = false;
            if (to.length() - body.getBbWidth() * 0.5 <= reach) {
                V3 flat = to.withY(0.0);
                caught = body == prey || flat.length() < 1.0E-4 || blow.catches(flat.normalize().dot(facing));
            }
            if (!caught && (blow == WarStrike.VERDICT || blow == WarStrike.CALAMITY)) {
                double radius = blow == WarStrike.CALAMITY ? 3.6 : WarStrike.VERDICT_RADIUS;
                caught = new V3(body.getX() - verdict.x(), 0.0, body.getZ() - verdict.z()).length() <= radius && Math.abs(body.getY() - verdict.y()) < 2.5;
            }
            if (!caught) continue;
            // 1.0.40: a player whose own M1 is on its way back at him: the two meet, and neither lands.
            if (com.hex.kagune.control.server.KaguneClash.meets(player, body, now)) return;
            state.struck.add(body.getId());
            // Where the blow met it: the side facing him, at the height the blow goes in at.
            double height = switch (blow) {
                case SIDE_KICK -> 0.5;
                case RISING, ASCENSION -> 0.8;
                case HEEL -> 0.85;
                case VERDICT, CLEAVER, DOWNFALL -> 0.7;
                case CALAMITY -> 0.35;
                default -> 0.72;
            };
            V3 contact = new V3(c.x() - facing.x() * body.getBbWidth() * 0.5, body.getY() + body.getBbHeight() * height,
                    c.z() - facing.z() * body.getBbWidth() * 0.5);
            if (blowOn(player, state, body, blow, knock, facing, now)) {
                hit.add(body);
                contacts.add(contact);
            }
        }
        com.hex.kagune.control.server.KaguneClash.landed(player);
        int[] ids = new int[1 + hit.size()];
        ids[0] = blow.ordinal();
        for (int i = 0; i < hit.size(); i++) ids[i + 1] = hit.get(i).getId();
        V3[] points = new V3[2 + contacts.size()];
        points[0] = chest;
        points[1] = blow == WarStrike.VERDICT ? new V3(facing.x(), verdict.y(), facing.z()) : new V3(facing.x(), 0.0, facing.z());
        for (int i = 0; i < contacts.size(); i++) points[2 + i] = contacts.get(i);
        if (blow == WarStrike.VERDICT) {
            // Where the rinkaku come up, for every client: on the last point.
            V3[] more = java.util.Arrays.copyOf(points, points.length + 1);
            more[more.length - 1] = verdict;
            points = more;
        }
        fx(player, S2CKingFx.STRIKE, ids, points);
        sounds(player, blow, hit.isEmpty() ? chest.add(facing.scale(2.0)) : contacts.get(0), !hit.isEmpty());
        // 1.0.20: what the grammar's blows leave; the spike thrown at nothing still brings him down hard.
        if (blow == WarStrike.DOWNFALL && hit.isEmpty()) KingCombos.motion(player, 0.0, -1.0, 0.0);
        V3 ground = new V3(verdict.x(), verdict.y(), verdict.z());
        switch (blow) {
            case CALAMITY -> {
                mark(player, com.hex.kagune.control.king.KingMarkFx.QUAKE, ground, V3.ZERO, 2.6, null);
                mark(player, com.hex.kagune.control.king.KingMarkFx.FLARE, ground.add(0.0, 0.8, 0.0), new V3(0.0, 1.0, 0.0), 1.8, null);
                mark(player, com.hex.kagune.control.king.KingMarkFx.SOOT, ground.add(0.0, 0.5, 0.0), new V3(0.0, 1.0, 0.0), 1.6, null);
            }
            case CLEAVER -> mark(player, com.hex.kagune.control.king.KingMarkFx.QUAKE, new V3(player.getX(), player.getY(), player.getZ()).add(facing.scale(2.2)),
                    V3.ZERO, 0.8, null);
            case ASCENSION -> {
                for (V3 c : contacts) mark(player, com.hex.kagune.control.king.KingMarkFx.FLARE, c, new V3(0.0, 1.0, 0.0), 1.2, null);
            }
            case REND -> {
                for (int i = 0; i < contacts.size(); i++) mark(player, com.hex.kagune.control.king.KingMarkFx.GORE, contacts.get(i), facing, 1.3, hit.get(i));
            }
            default -> {
            }
        }
    }

    /**
     * One body hit by a blow: the damage, the throw along the blow, the stun, the blade's bleeding. 1.0.20: the grammar's: the
     * launcher throws it up (and him after it), the spike drives it down, an air blow keeps it floating before him; the heavy's break a
     * guard; the ender is worth more for every blow before it.
     */
    private static boolean blowOn(ServerPlayer player, State state, LivingEntity body, WarStrike blow, V3 knock, V3 facing, long now) {
        float damage = blow.hit() * (blow == WarStrike.REND ? (float) state.worth : 1.0F);
        if (!hurt(player, body, damage, center(player), 0.0, 0.0)) return false;
        if (blow.breaksGuard()) KingEffects.breakGuard(body);
        if (!body.isAlive()) return true;
        if (blow == WarStrike.ASCENSION) {
            KingCombos.launch(player, body, facing);
            state.chaseUntil = now + com.hex.kagune.control.king.KingCombo.CHASE_TICKS;
        } else if (blow == WarStrike.DOWNFALL) {
            KingCombos.spike(player, body, facing, 1.0, now);
        } else if (state.strikeAir) {
            KingCombos.air(player, body, facing);
        } else {
            V3 push = knock.scale(1.0 - 0.75 * KingEffects.steady(body));
            Vec3 was = body.getDeltaMovement();
            KingCombos.motion(body, was.x * 0.15 + push.x(), was.y * 0.15 + push.y(), was.z * 0.15 + push.z());
            KingEffects.stun(body, blow.heavy() ? WarStrike.HEAVY_STUN : WarStrike.STUN);
        }
        if (blow.kind == WarStrike.Kind.BLADE) KingEffects.bleed(body, player, WarStrike.BLEED, 0);
        if (blow == WarStrike.VERDICT) KingEffects.bleed(body, player, WarStrike.VERDICT_BLEED, 1);
        return true;
    }

    /** Each blow sounds its own: the snap of the jab, the blade's hiss and bite, the thud of a foot, the Verdict's crash. */
    private static void sounds(ServerPlayer player, WarStrike blow, V3 at, boolean landed) {
        if (!landed) {
            vanilla(player, at, "entity.player.attack.nodamage", 0.7F, 0.9F);
            if (blow == WarStrike.VERDICT) play(player, at, HexKaguneSounds.KAGUNE_SLAM, 1.6F, 0.6F);
            return;
        }
        switch (blow) {
            case JAB -> {
                vanilla(player, at, "entity.player.attack.strong", 0.9F, 1.3F);
                play(player, at, HexKaguneSounds.FLESH_IMPACT, 0.6F, 1.4F);
            }
            case REAP, RISING -> {
                vanilla(player, at, "entity.player.attack.sweep", 1.0F, blow == WarStrike.REAP ? 0.75F : 0.95F);
                play(player, at, HexKaguneSounds.FLESH_IMPACT, 0.9F, 0.8F);
            }
            case SIDE_KICK, HEEL -> {
                vanilla(player, at, "entity.player.attack.knockback", 1.1F, 0.7F);
                play(player, at, HexKaguneSounds.BONE_CRACK, 0.7F, 1.0F);
            }
            case VERDICT -> {
                vanilla(player, at, "entity.generic.explode", 0.6F, 1.4F);
                play(player, at, HexKaguneSounds.KAGUNE_SLAM, 1.8F, 0.55F);
                play(player, at, HexKaguneSounds.BONE_CRUSH, 1.4F, 0.7F);
            }
            case ASCENSION -> {
                vanilla(player, at, "entity.player.attack.sweep", 1.2F, 0.6F);
                play(player, at, HexKaguneSounds.FLESH_IMPACT, 1.2F, 0.7F);
                vanilla(player, at, "entity.ender_dragon.flap", 0.8F, 1.3F);
            }
            case DOWNFALL, CLEAVER -> {
                vanilla(player, at, "entity.player.attack.crit", 1.2F, 0.6F);
                play(player, at, HexKaguneSounds.BONE_CRACK, 1.3F, 0.6F);
                play(player, at, HexKaguneSounds.FLESH_IMPACT, 1.3F, 0.6F);
            }
            case CALAMITY -> {
                vanilla(player, at, "entity.generic.explode", 1.0F, 0.8F);
                play(player, at, HexKaguneSounds.KAGUNE_SLAM, 2.4F, 0.5F);
                play(player, at, HexKaguneSounds.BONE_CRUSH, 1.8F, 0.6F);
            }
            case REND -> {
                play(player, at, HexKaguneSounds.MANIFEST_RIP, 1.4F, 0.8F);
                play(player, at, HexKaguneSounds.FLESH_IMPACT, 1.4F, 0.6F);
                vanilla(player, at, "entity.player.attack.knockback", 1.2F, 0.6F);
            }
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------ 1.0.7: the rinkaku's instinct

    /**
     * Each of the six rinkaku hunts on its own: every hostile creature near him, and whatever struck him lately, shared out among
     * them nearest first; one near enough is lashed, one farther off is shot with a spike of RC. 1.0.35: one lash at a time between
     * them ({@link WarRules#LASH_GAP}), and one spike every {@link WarRules#SPIKE_COOLDOWN}: a hand, not a turret.
     */
    private void instinct(ServerPlayer player, State state, long now) {
        if (state.wyrm != null || WarHold.INSTANCE.cocooned(player)) return; // they have other work (1.0.28: the cocoon is them)
        state.prey.values().removeIf(until -> until < now);
        V3 from = center(player);
        List<LivingEntity> hunted = new ArrayList<>();
        for (LivingEntity body : near(player, WarRules.INSTINCT_RANGE)) {
            if (!(body instanceof net.minecraft.world.entity.monster.Enemy) && !state.prey.containsKey(body.getId())) continue;
            if (center(body).distance(from) <= WarRules.INSTINCT_RANGE) hunted.add(body);
        }
        if (hunted.isEmpty()) return;
        hunted.sort((a, b) -> Double.compare(center(a).distance(from), center(b).distance(from)));
        int next = 0;
        ServerLevel level = player.serverLevel();
        for (int i = 0; i < state.tendrilReady.length; i++) {
            if (now < state.tendrilReady[i]) continue;
            LivingEntity prey = hunted.get(next++ % hunted.size());
            V3 at = center(prey);
            Boolean lash = WarRules.lashes(at.distance(from));
            if (lash == null) continue;
            if (lash ? now - state.lashAt < WarRules.LASH_GAP : now - state.spikeAt < WarRules.SPIKE_COOLDOWN) continue;
            if (lash) {
                state.lashAt = now;
                state.tendrilReady[i] = now + WarRules.LASH_EVERY + 3L * i;
                KingServer.lash(player, i, prey.getId(), false);
                if (hurt(player, prey, WarRules.LASH_DAMAGE, from, 0.5, 0.15)) KingEffects.bleed(prey, player, 60, 0);
                mark(player, com.hex.kagune.control.king.KingMarkFx.BLOOD, at, at.sub(from).normalize(), 0.7, prey);
                play(player, at, HexKaguneSounds.KAGUNE_WHIP, 0.6F, 1.1F + 0.05F * i);
            } else {
                state.spikeAt = now;
                state.tendrilReady[i] = now + WarRules.LASH_EVERY + 3L * i;
                KingServer.lash(player, i, prey.getId(), true);
                V3 tip = tip(player, i);
                state.spikes.add(new Spike(tip, at.sub(tip).normalize(), prey.getId()));
                play(player, tip, HexKaguneSounds.SHARD_LAUNCH, 0.7F, 1.3F);
            }
        }
    }

    /** The spikes fly, bending toward their prey, and burst in the first hostile body they meet. */
    private void spikes(ServerPlayer player, State state) {
        if (state.spikes.isEmpty()) return;
        ServerLevel level = player.serverLevel();
        state.spikes.removeIf(spike -> {
            LivingEntity prey = null;
            for (LivingEntity body : near(player, spike.at, 4.0)) if (body.getId() == spike.target) prey = body;
            if (prey != null) spike.dir = spike.dir.lerp(center(prey).sub(spike.at).normalize(), 0.35).normalize();
            V3 last = spike.at;
            spike.at = spike.at.add(spike.dir.scale(WarRules.SPIKE_SPEED));
            spike.life++;
            for (LivingEntity body : near(player, spike.at, 2.5)) {
                if (body.getId() != spike.target && !(body instanceof net.minecraft.world.entity.monster.Enemy)) continue;
                if (center(body).distanceToSegment(last, spike.at) > 0.6 + body.getBbWidth() * 0.5) continue;
                if (hurt(player, body, WarRules.SPIKE_DAMAGE, last, 0.3, 0.1)) KingEffects.bleed(body, player, 80, 0);
                mark(player, com.hex.kagune.control.king.KingMarkFx.BLOOD, spike.at, spike.dir, 0.9, body);
                return true;
            }
            return spike.life >= WarRules.SPIKE_LIFE;
        });
    }

    /** Where rinkaku {@code index}'s point is in the world, at rest ({@code WarModel.tendrilTip}, turned with him). */
    private static V3 tip(ServerPlayer player, int index) {
        double[] body = com.hex.kagune.control.king.WarModel.tendrilTip(index);
        double yaw = Math.toRadians(player.getYRot());
        double k = 0.9375 / 16.0;
        V3 left = new V3(Math.cos(yaw), 0.0, Math.sin(yaw));
        V3 forward = new V3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        return new V3(player.getX(), player.getY(), player.getZ()).add(left.scale(body[0] * k)).add(0.0, body[1] * k, 0.0).add(forward.scale(-body[2] * k));
    }

    // ------------------------------------------------------------------ the kit's clock

    @Override
    public void tick(ServerPlayer player, long now) {
        State state = states.get(player.getUUID());
        if (state == null) return;
        if (state.wyrm != null) {
            state.wyrm.tick();
            if (state.wyrm.done()) {
                state.wyrm.end();
                state.wyrm = null;
                state.safeUntil = now + 60;
                KingServer.move(player, KingMove.NONE, 0, 0);
            }
            return;
        }
        WarPath.INSTANCE.tick(player, now); // 1.0.27
        WarHold.INSTANCE.tick(player, now); // 1.0.28
        striking(player, state, now);
        combos(player, state, now);
        instinct(player, state, now);
        spikes(player, state);
    }

    @Override
    public void off(ServerPlayer player) {
        WarPath.INSTANCE.off(player);
        WarHold.INSTANCE.off(player);
        State state = states.remove(player.getUUID());
        KingCombos.forget(player);
        if (state != null && state.wyrm != null) state.wyrm.end(); // taken off as the small Dragon: he steps out of it
    }

    // ------------------------------------------------------------------ helpers

    /** To every client near him: what an ability does out in the world ({@code points} flattened into its data). */
    /** 1.0.19: a mark a blow leaves (KingMarkFx: blood, gore, soot, quake, flare), {@code power} strong, on {@code body} if any. */
    static void mark(ServerPlayer player, int style, V3 at, V3 way, double power, Entity body) {
        fx(player, S2CKingFx.MARK, new int[] {style, body == null ? 0 : body.getId()}, at, way, new V3(power, 0.0, 0.0));
    }

    static void fx(ServerPlayer player, int kind, int[] ids, V3... points) {
        float[] data = new float[points.length * 3];
        for (int i = 0; i < points.length; i++) {
            data[i * 3] = (float) points[i].x();
            data[i * 3 + 1] = (float) points[i].y();
            data[i * 3 + 2] = (float) points[i].z();
        }
        ControlNetwork.broadcast(player, new S2CKingFx(player.getUUID(), kind, ids, data));
    }

    static LivingEntity byId(ServerPlayer player, int id, double radius) {
        if (id == 0) return null;
        for (LivingEntity body : near(player, radius)) if (body.getId() == id) return body;
        return null;
    }

    /** Living bodies within {@code radius} of the wearer that War's blows may hit. */
    static List<LivingEntity> near(LivingEntity player, double radius) {
        return near(player, center(player), radius);
    }

    static List<LivingEntity> near(LivingEntity player, V3 at, double radius) {
        return ((net.minecraft.server.level.ServerLevel) player.level()).getEntitiesOfClass(LivingEntity.class,
                new AABB(at.x() - radius, at.y() - radius, at.z() - radius, at.x() + radius, at.y() + radius, at.z() + radius),
                body -> body != player && body.isAlive() && !body.isSpectator() && !(body instanceof ArmorStand));
    }

    /** One of War's blows: true if it landed. */
    static boolean hurt(ServerPlayer player, LivingEntity target, float amount, V3 from, double push, double lift) {
        if (!target.isAlive()) return false;
        target.invulnerableTime = 0;
        if (!target.hurt(player.damageSources().playerAttack(player), amount)) return false;
        KingEffects.heal(player, (float) (amount * WarRules.LIFESTEAL)); // 1.0.7: War drinks from what it cuts
        if (!target.isAlive()) {
            V3 c = center(target);
            mark(player, com.hex.kagune.control.king.KingMarkFx.GORE, c, c.sub(center(player)).withY(0.0).normalize().add(0.0, 0.5, 0.0), 1.3, null);
            play(player, c, HexKaguneSounds.CRUSH_KILL, 1.4F, 0.7F);
            return true;
        }
        if (push != 0.0 || lift != 0.0) {
            V3 away = center(target).sub(from).withY(0.0).normalize();
            target.setDeltaMovement(away.x() * push, lift, away.z() * push);
            target.hurtMarked = true;
            if (target instanceof ServerPlayer hit && hit.connection != null) hit.connection.send(new ClientboundSetEntityMotionPacket(hit));
        }
        return true;
    }

    static V3 center(Entity entity) {
        return new V3(entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ());
    }

    static V3 eye(LivingEntity player) {
        Entity body = player; // (named on Entity, as the API list has it)
        Vec3 eye = body.getEyePosition(1.0F);
        return new V3(eye.x, eye.y, eye.z);
    }

    static V3 look(LivingEntity player) {
        Entity body = player;
        Vec3 view = body.getViewVector(1.0F);
        return new V3(view.x, view.y, view.z).normalize();
    }

    /** Where he faces, level with the ground. */
    static V3 facing(ServerPlayer player) {
        V3 look = look(player);
        V3 flat = look.withY(0.0);
        return flat.length() < 1.0E-4 ? new V3(-Math.sin(Math.toRadians(player.getYRot())), 0.0, Math.cos(Math.toRadians(player.getYRot()))) : flat.normalize();
    }

    /** 1.0.14: one of vanilla's sounds, by name, at {@code at}. */
    static void vanilla(ServerPlayer player, V3 at, String name, float volume, float pitch) {
        // 1.2.2: drawn from the user's own pack (Sfx): the name's pool, its pitch drawn back toward the sound's own.
        com.hex.kagune.control.Sfx.Sound picked = com.hex.kagune.control.Sfx.play(name, volume, pitch);
        SoundEvent sound = SoundEvent.createVariableRangeEvent(new ResourceLocation(picked.namespace(), picked.path()));
        player.serverLevel().playSound(null, at.x(), at.y(), at.z(), sound, SoundSource.PLAYERS, picked.volume(), picked.pitch());
    }

    static void play(ServerPlayer player, V3 at, RegistryObject<?> sound, float volume, float pitch) {
        player.serverLevel().playSound(null, at.x(), at.y(), at.z(), (SoundEvent) sound.get(), SoundSource.PLAYERS, volume, pitch);
    }
}
