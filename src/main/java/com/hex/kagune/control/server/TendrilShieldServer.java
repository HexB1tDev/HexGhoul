package com.hex.kagune.control.server;

import com.hex.kagune.control.ControlConfig;
import com.hex.kagune.control.TendrilShield;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CTendrilShield;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.KaguneForm;
import com.hex.kagune.gameplay.KaguneRuntime;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.gameplay.MutationNode;
import com.hex.kagune.sound.HexKaguneSounds;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Auto Projectile (and the Projectile Reflex Organ) on the server, 5.0.14: Kaneki's tentacles guard him against incoming shots
 * ({@link TendrilShield} has the rules). Replaces the original's deflection through two bytecode trampolines:
 * {@code KaguneServerController.tickAutoProjectile} (every tick: see what is coming and raise a free tentacle toward it) and
 * {@code deflectProjectileImpact} (a projectile entity about to hit him: it bounces off a guard that is in place). Ayato's shots
 * are not entities; his fire ({@link AyatoFire}) and crystal ({@link AyatoCrystal}) ask {@link #guard} at the moment they would
 * hit, and end on the tentacle (or go on, weaker, through a guard they break).
 *
 * <p>6.0.9: arrows are seen far enough out to be stopped. They are followed along their real flight (arc and all) from
 * {@link TendrilShield#FLIGHT_SCAN} away, and the guard goes up against a shooter before he looses (a ranged mob after him, a
 * player drawing at him or holding a loaded crossbow on him), so a shot from close up meets a tentacle already in place.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class TendrilShieldServer {
    /** A shot in flight: who fired it, in which world, where it is, how far it moves a tick and its hit radius. */
    interface Shots {
        void shot(UUID owner, ServerLevel level, Vec3 position, Vec3 velocity, float radius);
    }

    /** What a guard did to one of Ayato's shots: where it struck which tentacle, and the damage that still goes into the body. */
    record Guarded(Vec3 at, float through, int limb) {
    }

    /** One tentacle held up: which, the direction it faces (toward the shots), when it went up and until when it stays. */
    private static final class Guard {
        final int limb;
        final double[] dir;
        final long raisedAt;
        long until;

        Guard(int limb, double[] dir, long raisedAt, long until) {
            this.limb = limb;
            this.dir = dir;
            this.raisedAt = raisedAt;
            this.until = until;
        }
    }

    /** One player's guards, and every tentacle's guard: the damage it can still take and until when a broken one is aside. */
    private static final class Wall {
        final List<Guard> guards = new ArrayList<>();
        final float[] health = new float[LIMBS];
        final long[] brokenUntil = new long[LIMBS];
        long regenAt;
        long soundAt = Long.MIN_VALUE;

        Wall(float full, long now) {
            java.util.Arrays.fill(this.health, full);
            this.regenAt = now;
        }
    }

    private static final int LIMBS = 16;
    private static final Map<UUID, Wall> WALLS = new HashMap<>();
    /** Projectile entities that bounced off a guard: {whose (UUID hash), tick}, so the same one isn't stopped twice. */
    private static final Map<Entity, long[]> BOUNCED = new WeakHashMap<>();

    private TendrilShieldServer() {
    }

    /** True while this player's tentacles may guard: the kagune out and at rest, the mutation, and the guard switched on. */
    static boolean ready(ServerPlayer player, KaguneRuntime.State state) {
        if (state == null || !KaguneProfile.isActive(player) || !player.m_6084_() || AyatoServer.isAyato(player)
            || ControlConfig.tendrilGuardStrength() <= 0.0) {
            return false;
        }
        if (!KaguneProfile.hasMutation(player, MutationNode.AUTO_PROJECTILE) && !KaguneProfile.hasMutation(player, MutationNode.RANGED_ORGAN)) {
            return false;
        }
        // Attacking, grabbing, crushing, grappling, coming out or going in: the tentacles are needed (committed is every mode
        // but at rest and returning to rest).
        return !KaguneServerController.committed(state.mode);
    }

    private static boolean organ(ServerPlayer player) {
        return KaguneProfile.hasMutation(player, MutationNode.RANGED_ORGAN);
    }

    private static float full(ServerPlayer player) {
        return TendrilShield.guardHealth(KaguneProfile.mastery(player), organ(player), ControlConfig.tendrilGuardStrength());
    }

    private static double[] vec(Vec3 v) {
        return new double[] {v.f_82479_, v.f_82480_, v.f_82481_};
    }

    private static Vec3 vec(double[] v) {
        return new Vec3(v[0], v[1], v[2]);
    }

    private static Wall wall(ServerPlayer player, long now) {
        return WALLS.computeIfAbsent(player.m_20148_(), ignored -> new Wall(full(player), now));
    }

    /** Guards heal while not broken; a broken one is back, whole, once its time aside is over. */
    private static void regen(Wall wall, float full, long now) {
        long ticks = Math.max(0L, now - wall.regenAt);
        wall.regenAt = now;
        for (int limb = 0; limb < LIMBS; limb++) {
            if (now >= wall.brokenUntil[limb]) {
                wall.health[limb] = Math.min(full, wall.health[limb] + TendrilShield.REGEN_PER_TICK * ticks);
            }
        }
    }

    /** True if {@code limb} may guard: an ordinary tentacle (not a centipede leg) that is out, holds nothing and isn't aside. */
    private static boolean usable(ServerPlayer player, KaguneRuntime.State state, Wall wall, int limb, long now) {
        KaguneForm form = KaguneProfile.form(player);
        int count = Math.min(Math.min(KaguneProfile.manifestedLimbs(player), form.limbCount()), LIMBS);
        return limb >= 0 && limb < count && !form.limb(limb).segmented() && state.heldEntityId(limb) == 0 && now >= wall.brokenUntil[limb]
            && wall.health[limb] > 0.0F;
    }

    /** The free tentacle to raise toward {@code dir}: one on that side of him first, then the one with the most guard left. */
    private static int pick(ServerPlayer player, KaguneRuntime.State state, Wall wall, double[] dir, long now) {
        KaguneForm form = KaguneProfile.form(player);
        double yaw = Math.toRadians(player.f_20883_);
        boolean fromRight = dir[0] * -Math.cos(yaw) + dir[2] * -Math.sin(yaw) >= 0.0;
        int best = -1;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int limb = 0; limb < LIMBS; limb++) {
            if (!usable(player, state, wall, limb, now) || guarding(wall, limb)) {
                continue;
            }
            double score = ((form.limb(limb).fanSide() >= 0.0) == fromRight ? 1.0 : 0.0) + 0.01 * wall.health[limb];
            if (score > bestScore) {
                best = limb;
                bestScore = score;
            }
        }
        return best;
    }

    private static boolean guarding(Wall wall, int limb) {
        for (Guard guard : wall.guards) {
            if (guard.limb == limb) {
                return true;
            }
        }
        return false;
    }

    /** The guard that takes shots from {@code dir}: one up already, or a free tentacle raised now ({@code raised}); null if none. */
    private static Guard cover(ServerPlayer player, KaguneRuntime.State state, Wall wall, double[] dir, long now, boolean[] raised) {
        for (Guard guard : wall.guards) {
            if (TendrilShield.covers(guard.dir, dir)) {
                guard.until = Math.max(guard.until, now + TendrilShield.HOLD_TICKS);
                return guard;
            }
        }
        if (wall.guards.size() >= ControlConfig.tendrilGuardMax()) {
            return null;
        }
        int limb = pick(player, state, wall, dir, now);
        if (limb < 0) {
            return null;
        }
        Guard guard = new Guard(limb, dir.clone(), now, now + TendrilShield.HOLD_TICKS);
        wall.guards.add(guard);
        raised[0] = true;
        return guard;
    }

    /** Trampoline target for the original {@code tickAutoProjectile}: see what is coming and put tentacles in its way. */
    public static void tickAutoProjectile(ServerPlayer player, KaguneRuntime.State state, long now) {
        UUID id = player.m_20148_();
        Wall existing = WALLS.get(id);
        if (!ready(player, state)) {
            if (existing != null && !existing.guards.isEmpty()) {
                existing.guards.clear();
                sync(player, existing);
            }
            return;
        }
        Wall wall = existing != null ? existing : wall(player, now);
        regen(wall, full(player), now);
        boolean changed = wall.guards.removeIf(guard -> now > guard.until || !usable(player, state, wall, guard.limb, now));
        boolean[] raised = {false};
        Vec3 middle = KaguneServerController.center(player);
        double[] centre = vec(middle);
        Vec3 motion = player.m_20184_();
        // Standing, the motion's steady fall (gravity pressing him into the ground) isn't his: he isn't sinking.
        Vec3 drift = player.m_20096_() ? new Vec3(motion.f_82479_, 0.0, motion.f_82481_) : motion;
        ServerLevel level = player.m_284548_();
        // Projectile entities (arrows, tridents, thrown things, fireballs), followed along their own flight (6.0.9).
        for (Projectile projectile : level.m_6443_(Projectile.class, player.m_20191_().m_82400_(TendrilShield.FLIGHT_SCAN),
            projectile -> threat(player, projectile, now))) {
            double[] p = vec(projectile.m_20182_());
            boolean straight = projectile instanceof AbstractHurtingProjectile;
            double gravity = straight ? 0.0 : projectile instanceof AbstractArrow ? TendrilShield.ARROW_GRAVITY : TendrilShield.THROWN_GRAVITY;
            if (TendrilShield.incoming(p, vec(projectile.m_20184_()), gravity, straight ? 1.0 : TendrilShield.AIR_DRAG, centre, vec(drift),
                TendrilShield.FLIGHT_LOOKAHEAD, TendrilShield.INTERCEPT) && player.m_142582_(projectile)) {
                cover(player, state, wall, TendrilShield.from(p, vec(projectile.m_20184_().m_82546_(drift)), centre), now, raised);
            }
        }
        // Shooters about to loose at him (6.0.9): the guard goes up toward them before the shot, which from close up would land
        // before a tentacle could swing in.
        for (LivingEntity shooter : level.m_6443_(LivingEntity.class, player.m_20191_().m_82400_(TendrilShield.AIM_RANGE),
            other -> other != player && other.m_6084_() && aiming(player, other, middle))) {
            if (player.m_142582_(shooter)) {
                cover(player, state, wall, TendrilShield.from(vec(shooter.m_146892_()), new double[3], centre), now, raised);
            }
        }
        // Ayato's fire and crystal (simulated, not entities).
        Shots seen = (owner, world, position, velocity, radius) -> {
            if (owner.equals(id) || world != level || position.m_82557_(middle) > TendrilShield.SHOT_SCAN * TendrilShield.SHOT_SCAN) {
                return;
            }
            double[] p = vec(position);
            double[] v = vec(velocity.m_82546_(drift));
            if (TendrilShield.incoming(p, v, centre, TendrilShield.SHOT_LOOKAHEAD, TendrilShield.INTERCEPT + radius)) {
                cover(player, state, wall, TendrilShield.from(p, v, centre), now, raised);
            }
        };
        AyatoFire.shots(seen);
        AyatoCrystal.shots(seen);
        if (changed || raised[0]) {
            sync(player, wall);
        }
    }

    /**
     * 6.0.9: true when {@code shooter} is about to loose a shot at {@code player}: a ranged mob (skeletons, pillagers, drowned,
     * witches) after him within {@link TendrilShield#MOB_RANGE}, or another player drawing something or holding a loaded crossbow
     * aimed at him ({@link TendrilShield#aimedAt}).
     */
    private static boolean aiming(ServerPlayer player, LivingEntity shooter, Vec3 middle) {
        if (shooter instanceof Mob mob && shooter instanceof RangedAttackMob) {
            return mob.m_5448_() == player && shooter.m_20182_().m_82557_(middle) <= TendrilShield.MOB_RANGE * TendrilShield.MOB_RANGE;
        }
        if (shooter instanceof Player other && !other.m_5833_()) {
            boolean armed = other.m_6117_() || loaded(other.m_21205_()) || loaded(other.m_21206_());
            return armed && TendrilShield.aimedAt(vec(other.m_146892_()), vec(other.m_20154_()), vec(middle));
        }
        return false;
    }

    private static boolean loaded(ItemStack stack) {
        return stack.m_41720_() instanceof CrossbowItem && CrossbowItem.m_40932_(stack);
    }

    /** As the original: alive, not his own, not already bounced off him. */
    private static boolean threat(ServerPlayer player, Projectile projectile, long now) {
        return projectile.m_6084_() && !projectile.m_213877_() && projectile.m_19749_() != player && !bounced(projectile, player, now);
    }

    private static boolean bounced(Entity projectile, ServerPlayer player, long now) {
        long[] mark = BOUNCED.get(projectile);
        return mark != null && mark[0] == player.m_20148_().hashCode() && now - mark[1] <= TendrilShield.REBLOCK_TICKS;
    }

    /**
     * One of Ayato's shots, fired from {@code from} at {@code velocity} a tick, is about to hit {@code target} at {@code hit} for
     * {@code damage}. If a Kaneki guard in place covers it, the guard takes what it can: the shot strikes the tentacle at the
     * returned point, and only {@link Guarded#through} (0 when it held) goes on into the body. Null: no guard, it hits as ever.
     */
    static Guarded guard(LivingEntity target, Vec3 from, Vec3 velocity, Vec3 hit, float damage, long now) {
        if (!(target instanceof ServerPlayer player)) {
            return null;
        }
        KaguneRuntime.State state = KaguneRuntime.peek(player.m_20148_());
        if (!ready(player, state)) {
            return null;
        }
        Wall wall = wall(player, now);
        double[] centre = vec(KaguneServerController.center(player));
        double[] dir = TendrilShield.from(vec(from), vec(velocity), centre);
        boolean[] raised = {false};
        Guard guard = cover(player, state, wall, dir, now, raised);
        if (raised[0]) {
            sync(player, wall);
        }
        if (guard == null || !TendrilShield.inPlace(guard.raisedAt, now)) {
            return null; // no tentacle free, or it is still swinging in: the shot is first
        }
        float[] split = TendrilShield.absorb(damage, wall.health[guard.limb]);
        if (split[0] <= 0.0F || !KaguneServerController.useEnergy(player, TendrilShield.cost(split[0], false, organ(player)), false)) {
            return null;
        }
        wall.health[guard.limb] -= split[0];
        guard.until = Math.max(guard.until, now + TendrilShield.HOLD_TICKS);
        Vec3 at = vec(TendrilShield.stopPoint(vec(from), vec(velocity), vec(hit), centre, guard.dir));
        struck(player, wall, guard, at, now);
        return new Guarded(at, split[1], guard.limb);
    }

    /** True while {@code target}'s tentacle {@code limb} is out (a spike stuck in it stays; otherwise it falls out). */
    static boolean holds(LivingEntity target, int limb) {
        if (!(target instanceof ServerPlayer player) || !KaguneProfile.isActive(player) || !player.m_6084_()) {
            return false;
        }
        KaguneForm form = KaguneProfile.form(player);
        int count = Math.min(Math.min(KaguneProfile.manifestedLimbs(player), form.limbCount()), LIMBS);
        return limb >= 0 && limb < count && !form.limb(limb).segmented();
    }

    /** A spike stuck in {@code target}'s tentacle {@code limb} burst for {@code damage} at {@code at}: that tentacle's guard takes it. */
    static void wound(LivingEntity target, int limb, float damage, Vec3 at, long now) {
        if (!(target instanceof ServerPlayer player) || limb < 0 || limb >= LIMBS) {
            return;
        }
        Wall wall = wall(player, now);
        if (now < wall.brokenUntil[limb]) {
            return; // already knocked aside
        }
        wall.health[limb] -= Math.max(0.0F, damage);
        if (wall.health[limb] <= 1.0E-4F) {
            knockAside(player, wall, limb, at, now);
        }
    }

    /**
     * Trampoline target for the original {@code deflectProjectileImpact} (its {@code ProjectileImpactEvent} listener skips the hit
     * when this is true): a projectile entity about to hit him bounces off a guard in place covering it. With the Projectile
     * Reflex Organ it flies back at whoever shot it; otherwise it drops off the tentacle.
     */
    public static boolean deflectProjectileImpact(ServerPlayer player, Projectile projectile) {
        if (projectile.m_19749_() != player) {
            // 1.0.34: a final kakuja's own guard first (Peace's Aegis of :Re sends shots back at who shot them).
            long at = player.m_9236_().m_46467_();
            if (bounced(projectile, player, at)) {
                return true;
            }
            double[] met = KingServer.deflect(player, projectile);
            if (met != null) {
                sendBack(player, projectile, new Vec3(met[0], met[1], met[2]), at);
                return true;
            }
        }
        KaguneRuntime.State state = KaguneRuntime.get(player.m_20148_());
        if (!ready(player, state) || projectile.m_19749_() == player) {
            return false;
        }
        long now = player.m_9236_().m_46467_();
        if (bounced(projectile, player, now)) {
            return true; // it already bounced off him: let it fall past
        }
        Wall wall = wall(player, now);
        Vec3 middle = KaguneServerController.center(player);
        double[] centre = vec(middle);
        Vec3 velocity = projectile.m_20184_();
        double[] dir = TendrilShield.from(vec(projectile.m_20182_()), vec(velocity), centre);
        boolean[] raised = {false};
        Guard guard = cover(player, state, wall, dir, now, raised);
        if (raised[0]) {
            sync(player, wall);
        }
        boolean organ = organ(player);
        if (guard == null || !TendrilShield.inPlace(guard.raisedAt, now)
            || !KaguneServerController.useEnergy(player, TendrilShield.cost(0.0F, true, organ), false)) {
            return false;
        }
        wall.health[guard.limb] -= TendrilShield.absorb(TendrilShield.ENTITY_WEIGHT, wall.health[guard.limb])[0];
        guard.until = Math.max(guard.until, now + TendrilShield.HOLD_TICKS);
        BOUNCED.put(projectile, new long[] {player.m_20148_().hashCode(), now});
        Vec3 at = middle.m_82549_(vec(guard.dir).m_82490_(TendrilShield.DISTANCE + 0.15));
        Entity owner = projectile.m_19749_();
        Vec3 away;
        if (organ && owner != null && owner.m_6084_()) {
            away = KaguneServerController.center(owner).m_82546_(at).m_82541_().m_82490_(Math.max(1.2, velocity.m_82553_()));
        } else {
            away = velocity.m_82490_(-0.25).m_82520_(0.0, 0.1, 0.0);
        }
        projectile.m_146884_(at);
        projectile.m_20256_(away);
        if (projectile instanceof AbstractHurtingProjectile hurting) {
            Vec3 push = away.m_82541_().m_82490_(0.1);
            hurting.f_36813_ = push.f_82479_;
            hurting.f_36814_ = push.f_82480_;
            hurting.f_36815_ = push.f_82481_;
        }
        projectile.f_19812_ = true;
        player.m_284548_().m_8767_(ParticleTypes.f_123797_, at.f_82479_, at.f_82480_, at.f_82481_, 6, 0.18, 0.18, 0.18, 0.12);
        struck(player, wall, guard, at, now);
        return true;
    }

    /** 1.0.34: {@code projectile} sent back from {@code at} at whoever shot it (else back the way it came), as the organ does. */
    private static void sendBack(ServerPlayer player, Projectile projectile, Vec3 at, long now) {
        BOUNCED.put(projectile, new long[] {player.m_20148_().hashCode(), now});
        Vec3 velocity = projectile.m_20184_();
        Entity owner = projectile.m_19749_();
        Vec3 away = owner != null && owner.m_6084_()
            ? KaguneServerController.center(owner).m_82546_(at).m_82541_().m_82490_(Math.max(1.2, velocity.m_82553_()))
            : velocity.m_82490_(-0.8);
        projectile.m_146884_(at);
        projectile.m_20256_(away);
        if (projectile instanceof AbstractHurtingProjectile hurting) {
            Vec3 push = away.m_82541_().m_82490_(0.1);
            hurting.f_36813_ = push.f_82479_;
            hurting.f_36814_ = push.f_82480_;
            hurting.f_36815_ = push.f_82481_;
        }
        projectile.f_19812_ = true;
        KaguneServerController.playAt(player, at, HexKaguneSounds.KAGUNE_GUARD, 0.8F, KaguneServerController.vary(player, 1.3F, 0.1F));
    }

    /** A guard was struck at {@code at}: it rings (not more than every few ticks), or breaks and is knocked aside. */
    private static void struck(ServerPlayer player, Wall wall, Guard guard, Vec3 at, long now) {
        if (wall.health[guard.limb] <= 1.0E-4F) {
            knockAside(player, wall, guard.limb, at, now);
            return;
        }
        if (now - wall.soundAt >= 3L) {
            wall.soundAt = now;
            KaguneServerController.playAt(player, at, HexKaguneSounds.KAGUNE_GUARD, 0.6F, KaguneServerController.vary(player, 1.25F, 0.1F));
        }
    }

    /** Tentacle {@code limb}'s guard broke: it is knocked aside for {@link TendrilShield#BREAK_TICKS}, whole again after. */
    private static void knockAside(ServerPlayer player, Wall wall, int limb, Vec3 at, long now) {
        boolean guarding = wall.guards.removeIf(guard -> guard.limb == limb);
        wall.brokenUntil[limb] = now + TendrilShield.BREAK_TICKS;
        wall.health[limb] = full(player);
        KaguneServerController.playAt(player, at, HexKaguneSounds.BONE_CRACK, 0.9F, KaguneServerController.vary(player, 1.1F, 0.08F));
        player.m_284548_().m_8767_(ParticleTypes.f_123797_, at.f_82479_, at.f_82480_, at.f_82481_, 14, 0.3, 0.3, 0.3, 0.2);
        if (guarding) {
            sync(player, wall);
        }
    }

    /** Every client tracking him (and his own) hears which tentacles are up and toward where. */
    private static void sync(ServerPlayer player, Wall wall) {
        int n = wall.guards.size();
        int[] limbs = new int[n];
        float[] yaws = new float[n];
        float[] pitches = new float[n];
        long[] raisedAt = new long[n];
        for (int i = 0; i < n; i++) {
            Guard guard = wall.guards.get(i);
            limbs[i] = guard.limb;
            yaws[i] = TendrilShield.yaw(guard.dir);
            pitches[i] = TendrilShield.pitch(guard.dir);
            raisedAt[i] = guard.raisedAt;
        }
        ControlNetwork.broadcast(player, new S2CTendrilShield(player.m_20148_(), limbs, yaws, pitches, raisedAt));
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        WALLS.remove(event.getEntity().m_20148_());
    }
}
