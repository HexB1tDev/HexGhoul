package com.hex.kagune.control.server;

import com.hex.kagune.control.eto.EtoMove;
import com.hex.kagune.control.eto.EtoRules;
import com.hex.kagune.control.eto.WatchRules;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CEtoFx;
import com.hex.kagune.control.network.S2CEtoWatch;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.network.KaguneNetwork;
import com.hex.kagune.sound.HexKaguneSounds;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 6.2.0, Watch This (B; {@link WatchRules}): her arms flung out in a cross, two tentacles burst out of her back and for exactly ten
 * seconds hunt on their own. Each picks something near her that is hostile to her (a mob after her, or anything that hurt her
 * lately), favouring its own side, rears back and lunges; the beak goes through its chest (the hurt, a bleed, blood, a hole in
 * it); it is lifted on the beak in a great sweep up over her and out to that tentacle's side ({@link WatchRules#carry}) and thrown
 * on at the speed of the sweep, through weak blocks: the left tentacle's to her left, the right's to her right. Then they draw back
 * into her. Every client draws the tentacles from {@link S2CEtoWatch}; what they carry is held where the sweep has it each tick.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class EtoWatch {
    /** One tentacle: which (1 her right, -1 her left), what it is doing since when, at what, and where a carry began. */
    static final class Tentacle {
        final int side;
        WatchRules.Phase phase = WatchRules.Phase.NONE;
        long startedAt;
        int targetId;
        int lunge;
        /** A carry: where the body's chest went on the beak, in her frame, and the way she faced then (degrees). */
        double[] start = new double[3];
        float yaw;
        /** Where the beak was when a miss or a follow-through began (the world). */
        Vec3 point = Vec3.f_82478_;

        Tentacle(int side) {
            this.side = side;
        }
    }

    /** Her tentacles: when they burst out, and the bodies just thrown (by entity id: free again from that tick). */
    static final class Watch {
        final long out;
        final Tentacle right = new Tentacle(1);
        final Tentacle left = new Tentacle(-1);
        final Map<Integer, Long> thrown = new HashMap<>();

        Watch(long out) {
            this.out = out;
        }

        Tentacle[] both() {
            return new Tentacle[] {this.right, this.left};
        }
    }

    private static final Map<UUID, Watch> WATCHES = new HashMap<>();
    /** Who hurt each Eto player, and when (by entity id): hostile to her for {@link WatchRules#GRUDGE} ticks. */
    private static final Map<UUID, Map<Integer, Long>> GRUDGES = new HashMap<>();

    private EtoWatch() {
    }

    /** B: the cross and the tentacles, if she can (her mastery, her RC, not already out, nothing else going on). */
    static void start(ServerPlayer player, EtoServer.EtoState state, long now) {
        if (WATCHES.containsKey(player.m_20148_()) || !EtoCancel.allowed(state, now) || now < state.watchReadyAt
            || EtoServer.locked(player, state, "Watch This", EtoRules.WATCH_MASTERY)) {
            return;
        }
        if (!KaguneServerController.useEnergy(player, WatchRules.COST, true)) {
            return;
        }
        EtoCancel.cut(player, state, now);
        state.queuedAt = Long.MIN_VALUE;
        EtoServer.begin(player, state, EtoMove.WATCH, 0, 0, 0.0F, null, now);
        Watch watch = new Watch(now + WatchRules.ERUPT_AT);
        WATCHES.put(player.m_20148_(), watch);
        for (Tentacle tentacle : watch.both()) {
            set(player, tentacle, WatchRules.Phase.OUT, watch.out);
        }
        state.watchReadyAt = watch.out + WatchRules.ACTIVE + WatchRules.RETRACT + Cooldowns.of(player, WatchRules.COOLDOWN);
        KaguneServerController.playSelf(player, HexKaguneSounds.MANIFEST_RIP, 1.0F, KaguneServerController.vary(player, 0.7F, 0.05F));
    }

    /** Whether her tentacles are out (or coming out, or going back in). */
    static boolean out(ServerPlayer player) {
        return WATCHES.containsKey(player.m_20148_());
    }

    /** Each tick of hers: the tentacles hunt, strike, carry and throw; after ten seconds they go back in. */
    static void tick(ServerPlayer player, long now) {
        Watch watch = WATCHES.get(player.m_20148_());
        if (watch == null) {
            return;
        }
        long end = watch.out + WatchRules.ACTIVE;
        if (now >= end) {
            for (Tentacle tentacle : watch.both()) {
                if (tentacle.phase == WatchRules.Phase.CARRY) {
                    fling(player, watch, tentacle, now);
                }
                if (tentacle.phase != WatchRules.Phase.IN) {
                    set(player, tentacle, WatchRules.Phase.IN, end);
                }
            }
            if (now >= end + WatchRules.RETRACT) {
                for (Tentacle tentacle : watch.both()) {
                    set(player, tentacle, WatchRules.Phase.NONE, now);
                }
                WATCHES.remove(player.m_20148_());
            }
            return;
        }
        watch.thrown.values().removeIf(free -> now >= free);
        for (Tentacle tentacle : watch.both()) {
            step(player, watch, tentacle, now);
        }
    }

    private static void step(ServerPlayer player, Watch watch, Tentacle tentacle, long now) {
        long age = now - tentacle.startedAt;
        LivingEntity target = tentacle.targetId == 0 ? null
            : player.m_9236_().m_6815_(tentacle.targetId) instanceof LivingEntity living && living.m_6084_() ? living : null;
        switch (tentacle.phase) {
            case OUT -> {
                if (age >= WatchRules.ERUPT) {
                    set(player, tentacle, WatchRules.Phase.IDLE, now);
                }
            }
            case IDLE -> {
                LivingEntity prey = hunt(player, watch, tentacle, now);
                if (prey != null) {
                    tentacle.targetId = prey.m_19879_();
                    tentacle.lunge = WatchRules.lungeTicks(chest(prey).m_82554_(world(player, player.m_146908_(), WatchRules.pivot(tentacle.side))));
                    set(player, tentacle, WatchRules.Phase.STRIKE, now);
                }
            }
            case STRIKE -> {
                if (age < WatchRules.strikeTicks(tentacle.lunge)) {
                    return;
                }
                Vec3 pivot = world(player, player.m_146908_(), WatchRules.pivot(tentacle.side));
                if (target == null || !EtoBlitz.valid(player, target) || chest(target).m_82554_(pivot) > WatchRules.REACH) {
                    // Gone, or got away: the beak stabs the air where it was going.
                    Vec3 aim = target == null ? pivot.m_82549_(player.m_20154_().m_82490_(3.0)) : chest(target);
                    Vec3 way = aim.m_82546_(pivot);
                    tentacle.point = pivot.m_82549_(way.m_82490_(Math.min(1.0, WatchRules.REACH / Math.max(1.0E-6, way.m_82553_()))));
                    set(player, tentacle, WatchRules.Phase.MISS, now);
                    return;
                }
                impale(player, tentacle, target, now);
            }
            case CARRY -> {
                if (target == null) {
                    tentacle.point = world(player, tentacle.yaw, WatchRules.carry(1.0, tentacle.start, tentacle.side));
                    set(player, tentacle, WatchRules.Phase.RECOVER, now);
                    return;
                }
                long lifted = age - WatchRules.IMPALE_HOLD;
                double u = lifted <= 0 ? 0.0 : WatchRules.sweep(lifted);
                hold(player, target, world(player, tentacle.yaw, WatchRules.carry(u, tentacle.start, tentacle.side)));
                if (lifted >= WatchRules.LIFT) {
                    fling(player, watch, tentacle, now);
                }
            }
            case RECOVER -> {
                if (age >= WatchRules.RECOVER) {
                    set(player, tentacle, WatchRules.Phase.IDLE, now);
                }
            }
            case MISS -> {
                if (age >= WatchRules.MISS_RECOVER) {
                    set(player, tentacle, WatchRules.Phase.IDLE, now);
                }
            }
            default -> {
            }
        }
    }

    /**
     * What this tentacle goes for: something near her (chest to chest, {@link WatchRules#RANGE}) hostile to her, in her sight, not
     * the other tentacle's, not just thrown; the nearest, favouring its own side of her.
     */
    static LivingEntity hunt(ServerPlayer player, Watch watch, Tentacle tentacle, long now) {
        ServerLevel level = player.m_284548_();
        Vec3 from = chest(player);
        Vec3 eye = player.m_146892_();
        Tentacle other = tentacle == watch.right ? watch.left : watch.right;
        int taken = other.phase == WatchRules.Phase.STRIKE || other.phase == WatchRules.Phase.CARRY ? other.targetId : 0;
        Map<Integer, Long> grudges = GRUDGES.get(player.m_20148_());
        double yaw = Math.toRadians(player.m_146908_());
        Vec3 right = new Vec3(-Math.cos(yaw), 0.0, -Math.sin(yaw));
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        AABB area = player.m_20191_().m_82400_(WatchRules.RANGE + 1.0);
        for (LivingEntity candidate : level.m_6443_(LivingEntity.class, area, entity -> EtoBlitz.valid(player, entity))) {
            int id = candidate.m_19879_();
            if (id == taken || watch.thrown.containsKey(id) || !hostile(player, candidate, grudges, now)) {
                continue;
            }
            Vec3 to = chest(candidate).m_82546_(from);
            double distance = to.m_82553_();
            if (distance > WatchRules.RANGE || !EtoMoves.seen(level, player, eye, candidate)) {
                continue;
            }
            boolean ownSide = to.m_82526_(right) * tentacle.side >= 0.0;
            double score = distance + (ownSide ? 0.0 : 2.5);
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    /** Who hurt her lately (by entity id: when), or null; 6.2.0: the Owl's lances hunt them too ({@link OwlWatch}). */
    static Map<Integer, Long> grudges(ServerPlayer player) {
        return GRUDGES.get(player.m_20148_());
    }

    /** Hostile to her: a mob after her, or anything that hurt her lately. */
    static boolean hostile(ServerPlayer player, LivingEntity body, Map<Integer, Long> grudges, long now) {
        if (body instanceof Mob mob && mob.m_5448_() == player) {
            return true;
        }
        Long hurtAt = grudges == null ? null : grudges.get(body.m_19879_());
        return hurtAt != null && now - hurtAt <= WatchRules.GRUDGE;
    }

    /**
     * The beak goes through its chest: the hurt, a bleed, blood out of its back, a hole in it (a stab wound in a player); it is
     * caught on the beak where it is (her frame now, the way she faces now).
     */
    private static void impale(ServerPlayer player, Tentacle tentacle, LivingEntity target, long now) {
        ServerLevel level = player.m_284548_();
        Vec3 chest = chest(target);
        Vec3 pivot = world(player, player.m_146908_(), WatchRules.pivot(tentacle.side));
        Vec3 through = chest.m_82546_(pivot).m_82541_();
        target.f_19802_ = 0;
        WoundHits.expect(target, com.hex.kagune.control.wound.WoundBook.Blow.STAB, chest.m_82546_(through.m_82490_(target.m_20205_() * 0.5)), through, 0.0);
        float damage = EtoRules.fist(KaguneProfile.mastery(player)) * WatchRules.DAMAGE;
        if (!target.m_6469_(player.m_269291_().m_269075_(player), damage)) {
            tentacle.point = chest;
            set(player, tentacle, WatchRules.Phase.MISS, now);
            return;
        }
        AyatoCrystal.kanekiBleed(target, player, now);
        EtoBlitz.fx(player, S2CEtoFx.IMPALED, target.m_19879_(), chest, through, target.m_20206_(), tentacle.side);
        AyatoFire.sound(level, chest, "entity.player.attack.strong", 1.0F, KaguneServerController.vary(player, 0.6F, 0.05F));
        AyatoFire.sound(level, chest, "entity.slime.squish", 1.0F, KaguneServerController.vary(player, 0.7F, 0.05F));
        KaguneServerController.playAt(player, target, HexKaguneSounds.BONE_CRACK, 0.9F, KaguneServerController.vary(player, 0.85F, 0.05F));
        KaguneProfile.addMastery(player, "Swipe", 2);
        KaguneNetwork.broadcast(player);
        EtoServer.landed(player, 1);
        tentacle.yaw = player.m_146908_();
        tentacle.start = frame(player, tentacle.yaw, chest);
        set(player, tentacle, WatchRules.Phase.CARRY, now);
        hold(player, target, chest);
    }

    /** Let go at the end of the sweep: thrown on at the sweep's own speed, out to that tentacle's side, through weak blocks. */
    private static void fling(ServerPlayer player, Watch watch, Tentacle tentacle, long now) {
        if (player.m_9236_().m_6815_(tentacle.targetId) instanceof LivingEntity body && body.m_6084_()) {
            double[] v = WatchRules.throwVelocity(tentacle.start, tentacle.side);
            Vec3 launch = turn(tentacle.yaw, v);
            BlockSmash.launch(player, body, launch, 40);
            watch.thrown.put(body.m_19879_(), now + WatchRules.THROWN_FREE);
            AyatoFire.sound(player.m_284548_(), chest(body), "entity.player.attack.sweep", 1.2F, KaguneServerController.vary(player, 0.5F, 0.05F));
        }
        tentacle.point = world(player, tentacle.yaw, WatchRules.carry(1.0, tentacle.start, tentacle.side));
        set(player, tentacle, WatchRules.Phase.RECOVER, now);
    }

    /** Held on the beak: its chest where the sweep has it (its feet below that), nothing else moving it. */
    private static void hold(ServerPlayer player, LivingEntity body, Vec3 chest) {
        CentipedeHunt.place(player, body, chest.m_82520_(0.0, -body.m_20206_() * WatchRules.CHEST, 0.0));
    }

    /** The tentacle starts something new now (or at {@code at}) and every client is told. */
    private static void set(ServerPlayer player, Tentacle tentacle, WatchRules.Phase phase, long at) {
        tentacle.phase = phase;
        tentacle.startedAt = at;
        if (phase == WatchRules.Phase.IDLE || phase == WatchRules.Phase.OUT || phase == WatchRules.Phase.IN || phase == WatchRules.Phase.NONE) {
            tentacle.targetId = 0;
        }
        double[] p = phase == WatchRules.Phase.CARRY ? tentacle.start
            : new double[] {tentacle.point.f_82479_, tentacle.point.f_82480_, tentacle.point.f_82481_};
        ControlNetwork.broadcast(player, new S2CEtoWatch(player.m_20148_(), tentacle.side, phase.ordinal(), at, tentacle.targetId, tentacle.lunge,
            tentacle.yaw, p[0], p[1], p[2]));
    }

    /** Her tentacles go at once (her form put away, death, another world, gone): what they hold is let go where it is. */
    static void forget(ServerPlayer player) {
        Watch watch = WATCHES.remove(player.m_20148_());
        if (watch == null) {
            return;
        }
        long now = player.m_9236_().m_46467_();
        for (Tentacle tentacle : watch.both()) {
            set(player, tentacle, WatchRules.Phase.NONE, now);
        }
    }

    /** Whoever hurts an Eto player is hostile to her a while. */
    @SubscribeEvent
    public static void hurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getSource().m_7639_() instanceof LivingEntity attacker && attacker != player
            && EtoServer.isEto(player)) {
            long now = player.m_9236_().m_46467_();
            Map<Integer, Long> grudges = GRUDGES.computeIfAbsent(player.m_20148_(), ignored -> new HashMap<>());
            grudges.values().removeIf(at -> now - at > WatchRules.GRUDGE || now < at);
            grudges.put(attacker.m_19879_(), now);
        }
    }

    /** {@code body}'s chest (the world): {@link WatchRules#CHEST} of its height up from its feet. */
    static Vec3 chest(LivingEntity body) {
        return body.m_20182_().m_82520_(0.0, body.m_20206_() * WatchRules.CHEST, 0.0);
    }

    /** A point of her frame ({@code yaw} her facing, degrees; her feet where they are now) in the world. */
    static Vec3 world(ServerPlayer player, float yaw, double[] p) {
        return player.m_20182_().m_82549_(turn(yaw, p));
    }

    /** A vector of her frame ({@code yaw}, degrees) in the world's axes. */
    static Vec3 turn(float yaw, double[] p) {
        double r = Math.toRadians(yaw);
        double fx = -Math.sin(r);
        double fz = Math.cos(r);
        return new Vec3(-fz * p[0] + fx * p[2], p[1], fx * p[0] + fz * p[2]);
    }

    /** A world point in her frame ({@code yaw}, degrees; her feet where they are now). */
    static double[] frame(ServerPlayer player, float yaw, Vec3 at) {
        Vec3 d = at.m_82546_(player.m_20182_());
        double r = Math.toRadians(yaw);
        double fx = -Math.sin(r);
        double fz = Math.cos(r);
        return new double[] {-fz * d.f_82479_ + fx * d.f_82481_, d.f_82480_, fx * d.f_82479_ + fz * d.f_82481_};
    }
}
