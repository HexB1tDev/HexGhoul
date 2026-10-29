package com.hex.kagune.control.server;

import com.hex.kagune.control.ComboStage;
import com.hex.kagune.control.StrikePattern;
import com.hex.kagune.control.ayato.AyatoMutation;
import com.hex.kagune.control.ayato.AyatoRules;
import com.hex.kagune.control.ayato.WingForm;
import com.hex.kagune.control.ayato.WingRig;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CAyatoFx;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.KaguneRuntime;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.network.KaguneNetwork;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Ayato's fire on the server: the M1 shots and the right-click enders, resolved at their impact tick.
 *
 * <p>A shot is a shard of the wing's own RC fire. It leaves from the tip of the wing that is curled forward aiming
 * ({@link WingRig#aimTip}) and is simulated here, not as an entity. It flies straight, stops at walls, hits each
 * living thing once (ignoring hurt immunity, so the fast cadence counts) and sets it on fire. Every client is told when it
 * leaves and where it ends ({@link S2CAyatoFx}) and draws it as a piece of the kagune with Photon fire.
 *
 * <p>The enders: Rabbit Hop (thrown back by one wingbeat), Flare (a big fireball that bursts), Rising Flame (a pillar under
 * the target that throws it up), Fire Fan (a fan of shards), Rabbit Dash (straight through, burning everything passed) and
 * the Black Rabbit (a backflip into the air, then a hovering barrage from both wings).
 */
final class AyatoFire {
    private static final Map<UUID, List<Bolt>> BOLTS = new HashMap<>();
    private static final Map<UUID, List<Embers>> EMBERS = new HashMap<>();
    private static final Map<UUID, Art> ARTS = new HashMap<>();
    /** Shards waiting their turn (a sweep of fire leaves one a tick). */
    private static final Map<UUID, List<Later>> LATER = new HashMap<>();
    private static int nextBoltId = 1;
    /** Rabbit Dash speed (blocks per tick) while the rush lasts: about six blocks. */
    private static final double DASH_SPEED = 1.05;

    /** What a bolt is, for its look and what it does where it ends. */
    enum Kind {
        SHOT, FLARE, FAN, BARRAGE
    }

    private static final class Bolt {
        final int id = nextBoltId++;
        /** The world it flies in (5.0.14: Kaneki's guards only see the shots in theirs). */
        ServerLevel level;
        Vec3 position;
        Vec3 velocity;
        int life;
        float radius;
        float damage;
        int pierce;
        Kind kind;
        int burnSeconds;
        boolean embers;
        boolean landed;
        final Set<Integer> hit = new HashSet<>();
    }

    private record Embers(Vec3 at, long until, float damage) {
    }

    /**
     * A shard to leave at {@code at}: from where, toward what, turned how far, how hard, how big it is drawn (times its kind's
     * size, 0: its own) and how far it flies (0: the fire's own range).
     */
    private record Later(long at, Vec3 from, Vec3 aim, Kind kind, float damage, double yaw, float scale, double range) {
    }

    /** An ender still running after its impact tick: the dash's path, the flip's barrage. */
    private static final class Art {
        StrikePattern pattern;
        /** The dash's heading (horizontal, unit). */
        Vec3 heading = Vec3.f_82478_;
        long started;
        int duration;
        int targetId;
        Vec3 aimPoint;
        float damage;
        int fired;
        int shards;
        final Set<Integer> hit = new HashSet<>();
    }

    private AyatoFire() {
    }

    /**
     * The impact tick of a fire stage (instead of a close-range hit). {@code started} and {@code duration} are the stage's
     * own timing (the enders that keep going read them).
     */
    static void resolve(ServerPlayer player, KaguneRuntime.State state, ComboStage stage, int limbMask, Vec3 direction, long started, int duration) {
        ServerLevel level = player.m_284548_();
        int mastery = KaguneProfile.mastery(player);
        WingForm form = AyatoServer.form(player);
        double range = AyatoRules.flameRange(mastery);
        Vec3 aim = aimPoint(player, state, direction, range);
        float damage = AyatoRules.meleeDamage(mastery) * stage.damageMul();
        float radius = AyatoRules.flameRadius(mastery);
        float speed = AyatoRules.flameSpeed(mastery);
        boolean right = (limbMask & 1) != 0 && form.rightWing();
        if (form.rightWing() && AyatoServer.wingGone(player, right ? 1 : -1, player.m_9236_().m_46467_())) {
            right = !right; // that wing was thrown as a Wing Vortex and isn't back yet: the other one fires
        }
        StrikePattern pattern = stage.pattern();
        switch (pattern) {
            case FLAME_SHOT -> {
                Vec3 tip = tip(player, right ? 1 : -1, aim);
                launch(player, tip, aim, speed, radius, damage, AyatoRules.flamePierce(mastery), Kind.SHOT, mastery, 0.0);
                sound(level, tip, "entity.blaze.shoot", 0.5F, KaguneServerController.vary(player, 1.4F, 0.08F));
            }
            case FLARE -> {
                Vec3 tip = form.rightWing() ? tip(player, 1, aim).m_82549_(tip(player, -1, aim)).m_82490_(0.5) : tip(player, -1, aim);
                launch(player, tip, aim, speed * 0.85F, radius * 2.2F, damage, 0, Kind.FLARE, mastery, 0.0);
                sound(level, tip, "item.firecharge.use", 1.0F, KaguneServerController.vary(player, 0.85F, 0.05F));
            }
            case RISING_FLAME -> pillar(player, state, aim, damage, mastery);
            case FIRE_FAN -> {
                int shards = AyatoRules.fanShards(mastery);
                for (int index = 0; index < shards; index++) {
                    double yaw = -AyatoRules.FAN_HALF_ANGLE + 2.0 * AyatoRules.FAN_HALF_ANGLE * index / (shards - 1.0);
                    int side = form.rightWing() && index % 2 == 0 ? 1 : -1;
                    launch(player, tip(player, side, aim), aim, speed, radius, damage, AyatoRules.flamePierce(mastery) + 1, Kind.FAN, mastery, yaw);
                }
                sound(level, player.m_146892_(), "item.firecharge.use", 0.9F, KaguneServerController.vary(player, 1.1F, 0.05F));
            }
            case RABBIT_HOP -> {
                // Fire Dash (mutation): the downbeat throws him much further.
                boolean dash = AyatoServer.hasMutation(player, AyatoMutation.FIRE_DASH);
                Vec3 back = horizontal(player.m_20154_()).m_82490_(-(dash ? AyatoRules.FIRE_DASH_BACK : 0.95));
                push(player, new Vec3(back.f_82479_, dash ? AyatoRules.FIRE_DASH_LIFT : 0.5, back.f_82481_));
                fx(player, S2CAyatoFx.HOP, 0, player.m_20182_(), back, dash ? 1.4F : 1.0F, 0);
                sound(level, player.m_20182_(), "entity.ender_dragon.flap", 0.55F, KaguneServerController.vary(player, 1.5F, 0.08F));
            }
            case RABBIT_DASH -> {
                Vec3 heading = horizontal(direction.m_82556_() > 1.0E-6 ? direction : player.m_20154_());
                Art art = art(player, pattern, started, duration, damage, aim, state.actionTargetId);
                art.heading = heading;
                push(player, new Vec3(heading.f_82479_ * DASH_SPEED, player.m_20096_() ? 0.12 : 0.04, heading.f_82481_ * DASH_SPEED));
                fx(player, S2CAyatoFx.DASH, 0, player.m_20182_(), heading.m_82490_(DASH_SPEED), 1.0F, 8);
                sound(level, player.m_20182_(), "entity.blaze.shoot", 0.9F, KaguneServerController.vary(player, 0.7F, 0.05F));
            }
            case BLACK_RABBIT -> {
                // About three and a half blocks up and a little back: the top of the jump comes as the barrage starts.
                Vec3 back = horizontal(player.m_20154_()).m_82490_(-0.45);
                push(player, new Vec3(back.f_82479_, 0.82, back.f_82481_));
                Art art = art(player, pattern, started, duration, damage, aim, state.actionTargetId);
                art.shards = AyatoRules.barrageShards(mastery);
                sound(level, player.m_20182_(), "entity.ender_dragon.flap", 0.8F, KaguneServerController.vary(player, 1.1F, 0.05F));
            }
            default -> {
            }
        }
    }

    /** Moves the bolts, burns the embers and runs the dash and the flip. Called every server tick for Ayato players. */
    static void tick(ServerPlayer player, long now) {
        UUID id = player.m_20148_();
        List<Bolt> bolts = BOLTS.get(id);
        if (bolts != null) {
            for (Iterator<Bolt> iterator = bolts.iterator(); iterator.hasNext(); ) {
                if (!step(player, iterator.next(), now)) {
                    iterator.remove();
                }
            }
            if (bolts.isEmpty()) {
                BOLTS.remove(id);
            }
        }
        List<Later> later = LATER.get(id);
        if (later != null) {
            int mastery = KaguneProfile.mastery(player);
            List<Later> due = new ArrayList<>();
            later.removeIf(shard -> now >= shard.at() && due.add(shard));
            for (Later shard : due) {
                fire(player, shard.from(), shard.aim(), shard.kind(), shard.damage(), shard.yaw(), shard.scale(), shard.range(), mastery);
            }
            if (later.isEmpty()) {
                LATER.remove(id);
            }
        }
        List<Embers> embers = EMBERS.get(id);
        if (embers != null) {
            embers.removeIf(patch -> now >= patch.until());
            for (Embers patch : embers) {
                burnEmbers(player, patch, now);
            }
            if (embers.isEmpty()) {
                EMBERS.remove(id);
            }
        }
        Art art = ARTS.get(id);
        if (art != null && !runArt(player, art, now)) {
            ARTS.remove(id);
        }
    }

    static void forget(ServerPlayer player) {
        LATER.remove(player.m_20148_());
        BOLTS.remove(player.m_20148_());
        EMBERS.remove(player.m_20148_());
        ARTS.remove(player.m_20148_());
    }

    // ------------------------------------------------------------------ the enders that keep going

    private static Art art(ServerPlayer player, StrikePattern pattern, long started, int duration, float damage, Vec3 aim, int targetId) {
        Art art = new Art();
        art.pattern = pattern;
        art.started = started;
        art.duration = duration;
        art.damage = damage;
        art.aimPoint = aim;
        art.targetId = targetId;
        ARTS.put(player.m_20148_(), art);
        return art;
    }

    /** One tick of a running ender; false once it is over. */
    private static boolean runArt(ServerPlayer player, Art art, long now) {
        if (!player.m_6084_() || !KaguneProfile.isActive(player)) {
            return false;
        }
        double t = (now - art.started) / (double)Math.max(1, art.duration);
        if (art.pattern == StrikePattern.RABBIT_DASH) {
            if (t > 0.75) {
                return false;
            }
            // A steady rush, the same on the ground or in the air (air friction alone would carry him far too far).
            if (t < 0.55) {
                Vec3 motion = player.m_20184_();
                player.m_20256_(new Vec3(art.heading.f_82479_ * DASH_SPEED, Math.min(motion.f_82480_, 0.2), art.heading.f_82481_ * DASH_SPEED));
                player.f_19864_ = true;
            }
            // Everything he passes through burns: each one once.
            AABB reach = player.m_20191_().m_82400_(1.1);
            for (LivingEntity target : player.m_284548_().m_6443_(LivingEntity.class, reach,
                entity -> entity != player && entity.m_6084_() && KaguneServerController.validSwipeTarget(player, entity))) {
                if (art.hit.add(target.m_19879_())) {
                    burn(player, target, art.damage, 3, null);
                    Vec3 away = KaguneServerController.center(target).m_82546_(player.m_20182_());
                    if (away.m_165925_() > 1.0E-4) {
                        target.m_147240_(0.9, -away.f_82479_, -away.f_82481_);
                    }
                }
            }
            return true;
        }
        if (art.pattern == StrikePattern.BLACK_RABBIT) {
            if (t < 0.38) {
                return true; // still flipping up
            }
            if (t > 0.78 || art.fired >= art.shards) {
                return false;
            }
            // Hanging at the top of the jump on the wings, raining fire.
            Vec3 motion = player.m_20184_();
            player.m_20256_(new Vec3(motion.f_82479_ * 0.5, 0.02, motion.f_82481_ * 0.5));
            player.f_19864_ = true;
            Vec3 target = art.aimPoint;
            if (art.targetId > 0 && player.m_284548_().m_6815_(art.targetId) instanceof LivingEntity living && living.m_6084_()) {
                target = KaguneServerController.center(living);
            }
            int mastery = KaguneProfile.mastery(player);
            WingForm form = AyatoServer.form(player);
            int side = form.rightWing() && art.fired % 2 == 0 ? 1 : -1;
            float spread = (player.m_217043_().m_188501_() - 0.5F) * 10.0F;
            launch(player, tip(player, side, target), target, AyatoRules.flameSpeed(mastery) * 1.2F, AyatoRules.flameRadius(mastery), art.damage, 0,
                Kind.BARRAGE, mastery, spread);
            art.fired++;
            if (art.fired % 2 == 1) {
                sound(player.m_284548_(), player.m_146892_(), "entity.blaze.shoot", 0.4F, KaguneServerController.vary(player, 1.6F, 0.1F));
            }
            return true;
        }
        return false;
    }

    /** Rising Flame: a column of fire bursts up under the target and throws whatever it catches into the air. */
    private static void pillar(ServerPlayer player, KaguneRuntime.State state, Vec3 aim, float damage, int mastery) {
        ServerLevel level = player.m_284548_();
        Vec3 base = aim;
        if (state.actionTargetId > 0 && level.m_6815_(state.actionTargetId) instanceof LivingEntity living && living.m_6084_()) {
            base = living.m_20182_();
        } else {
            // Find the ground under the aim point (up to six blocks down).
            BlockHitResult ground = level.m_45547_(new ClipContext(aim.m_82520_(0.0, 0.5, 0.0), aim.m_82520_(0.0, -6.0, 0.0), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
            if (ground.m_6662_() == HitResult.Type.BLOCK) {
                base = ground.m_82450_();
            }
        }
        double radius = AyatoRules.PILLAR_RADIUS;
        AABB column = new AABB(base.f_82479_ - radius, base.f_82480_ - 0.5, base.f_82481_ - radius, base.f_82479_ + radius, base.f_82480_ + 3.5,
            base.f_82481_ + radius);
        int landed = 0;
        for (LivingEntity target : level.m_6443_(LivingEntity.class, column,
            entity -> entity != player && entity.m_6084_() && KaguneServerController.validSwipeTarget(player, entity))) {
            if (burn(player, target, damage, AyatoRules.flameBurnSeconds(mastery) + 1, null)) {
                landed++;
                Vec3 motion = target.m_20184_();
                target.m_20256_(new Vec3(motion.f_82479_ * 0.2, AyatoRules.PILLAR_LAUNCH, motion.f_82481_ * 0.2));
                target.f_19864_ = true;
            }
        }
        fx(player, S2CAyatoFx.PILLAR, 0, base, Vec3.f_82478_, (float)radius, landed);
        sound(level, base, "entity.blaze.shoot", 1.0F, KaguneServerController.vary(player, 0.6F, 0.05F));
        sound(level, base, "item.firecharge.use", 0.8F, KaguneServerController.vary(player, 0.8F, 0.05F));
    }

    // ------------------------------------------------------------------ bolts

    /** Where a shot leaves the wing on {@code side} aiming at {@code aim} (world). */
    static Vec3 tip(ServerPlayer player, int side, Vec3 aim) {
        double yaw = Math.toRadians(player.f_20883_);
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double rightX = -Math.cos(yaw);
        double rightZ = -Math.sin(yaw);
        // Aim from the shoulder blades, in the rig's own space (x right, y up, z forward).
        Vec3 shoulders = player.m_20182_().m_82520_(0.0, WingRig.ROOT_Y, 0.0);
        Vec3 toward = aim.m_82546_(shoulders);
        double[] rig = {toward.f_82479_ * rightX + toward.f_82481_ * rightZ, toward.f_82480_, toward.f_82479_ * forwardX + toward.f_82481_ * forwardZ};
        double length = Math.sqrt(rig[0] * rig[0] + rig[1] * rig[1] + rig[2] * rig[2]);
        double[] dir = length < 1.0E-6 ? WingRig.FORWARD : new double[] {rig[0] / length, rig[1] / length, rig[2] / length};
        double[] local = WingRig.aimTip(KaguneProfile.mastery(player), AyatoServer.form(player), side, dir, player.m_6047_());
        Vec3 launch = new Vec3(player.m_20185_() + rightX * local[0] + forwardX * local[2], player.m_20186_() + local[1],
            player.m_20189_() + rightZ * local[0] + forwardZ * local[2]);
        return AyatoVortex.clearOfBlocks(player, player.m_146892_(), launch);
    }

    /** The strike's target if it is still there, else where the strike direction meets a wall or runs out. */
    static Vec3 aimPoint(ServerPlayer player, KaguneRuntime.State state, Vec3 direction, double range) {
        if (state.actionTargetId > 0 && player.m_284548_().m_6815_(state.actionTargetId) instanceof LivingEntity living && living.m_6084_()
            && living.m_20280_(player) <= (range + 4.0) * (range + 4.0)) {
            return KaguneServerController.center(living);
        }
        Vec3 eye = player.m_146892_();
        Vec3 end = eye.m_82549_(direction.m_82490_(range));
        BlockHitResult block = player.m_9236_().m_45547_(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return block.m_6662_() == HitResult.Type.BLOCK ? block.m_82450_() : end;
    }

    // ------------------------------------------------------------------ the final kakuja's fire tendrils

    /**
     * A shard of fire ({@code kind} SHOT, FAN or BARRAGE) or a fireball (FLARE) leaving {@code from} (a fire tendril's mouth) toward
     * {@code aim}, turned {@code yaw} degrees, now or {@code delay} ticks on; drawn {@code scale} times its kind's size (0: its own).
     */
    static void shard(ServerPlayer player, Vec3 from, Vec3 aim, Kind kind, float damage, double yaw, float scale, long delay) {
        shard(player, from, aim, kind, damage, yaw, scale, delay, 0.0);
    }

    /** {@link #shard(ServerPlayer, Vec3, Vec3, Kind, float, double, float, long)} flying {@code range} blocks (0: the fire's own range). */
    static void shard(ServerPlayer player, Vec3 from, Vec3 aim, Kind kind, float damage, double yaw, float scale, long delay, double range) {
        if (delay <= 0L) {
            fire(player, from, aim, kind, damage, yaw, scale, range, KaguneProfile.mastery(player));
            return;
        }
        long at = player.m_9236_().m_46467_() + delay;
        LATER.computeIfAbsent(player.m_20148_(), ignored -> new ArrayList<>()).add(new Later(at, from, aim, kind, damage, yaw, scale, range));
    }

    private static void fire(ServerPlayer player, Vec3 from, Vec3 aim, Kind kind, float damage, double yaw, float scale, double range, int mastery) {
        boolean ball = kind == Kind.FLARE;
        float speed = AyatoRules.flameSpeed(mastery) * (ball ? 0.85F : 1.0F);
        // A bigger fireball has a bigger body to hit with.
        float radius = AyatoRules.flameRadius(mastery) * (ball ? 1.8F * Math.max(1.0F, scale) : 1.0F);
        int pierce = ball ? 0 : AyatoRules.flamePierce(mastery) + (kind == Kind.FAN ? 1 : 0);
        launch(player, from, aim, speed, radius, damage, pierce, kind, mastery, yaw, scale > 0.0F ? scale * size(kind) : 0.0F, range);
    }

    /** The ground burns at {@code at} for {@code ticks}: whoever stands in it catches fire (drawn by the clients). */
    static void embers(ServerPlayer player, Vec3 at, long now, int ticks) {
        EMBERS.computeIfAbsent(player.m_20148_(), ignored -> new ArrayList<>()).add(new Embers(at, now + ticks, 1.0F));
        fx(player, S2CAyatoFx.EMBERS, 0, at, Vec3.f_82478_, 1.4F, ticks);
    }

    /** Fire blasting out round {@code at} (a needle bursting into flame): everyone near burns, {@code source} at half. */
    static void fireBlast(ServerPlayer player, LivingEntity source, Vec3 at, float damage, double radius) {
        Vec3 shift = at.m_82546_(player.m_20191_().m_82399_());
        AABB area = player.m_20191_().m_82386_(shift.f_82479_, shift.f_82480_, shift.f_82481_).m_82400_(radius);
        for (LivingEntity target : player.m_284548_().m_6443_(LivingEntity.class, area,
            entity -> entity != player && entity.m_6084_() && KaguneServerController.validSwipeTarget(player, entity))) {
            if (KaguneServerController.center(target).m_82557_(at) <= (radius + 0.6) * (radius + 0.6)) {
                burn(player, target, target == source ? damage * 0.5F : damage, AyatoRules.PYRE_BURN_SECONDS, Vec3.f_82478_);
            }
        }
        fx(player, S2CAyatoFx.BURST_BLAST, 0, at, Vec3.f_82478_, (float)(radius / 2.2), 1);
        sound(player.m_284548_(), at, "item.firecharge.use", 0.6F, KaguneServerController.vary(player, 1.3F, 0.1F));
    }

    /** Damages and sets fire to {@code target} for the final kakuja's own moves (the Ember Hop's burst). */
    static boolean scorch(ServerPlayer player, LivingEntity target, float damage, int burnSeconds) {
        return burn(player, target, damage, burnSeconds, null);
    }

    private static void launch(ServerPlayer player, Vec3 from, Vec3 aim, float speed, float radius, float damage, int pierce, Kind kind,
                               int mastery, double yawOffsetDegrees) {
        launch(player, from, aim, speed, radius, damage, pierce, kind, mastery, yawOffsetDegrees, 0.0F);
    }

    private static void launch(ServerPlayer player, Vec3 from, Vec3 aim, float speed, float radius, float damage, int pierce, Kind kind,
                               int mastery, double yawOffsetDegrees, float scale) {
        launch(player, from, aim, speed, radius, damage, pierce, kind, mastery, yawOffsetDegrees, scale, 0.0);
    }

    /** A bolt's own drawn size by kind (before the mastery 500 growth). */
    private static float size(Kind kind) {
        return switch (kind) {
            case FLARE -> 2.2F;
            case BARRAGE -> 0.8F;
            case FAN -> 0.9F;
            default -> 1.0F;
        };
    }

    private static void launch(ServerPlayer player, Vec3 from, Vec3 aim, float speed, float radius, float damage, int pierce, Kind kind,
                               int mastery, double yawOffsetDegrees, float scale, double range) {
        Vec3 direction = aim.m_82546_(from);
        if (direction.m_82556_() < 1.0E-6) {
            direction = player.m_20154_();
        }
        direction = KaguneCombat.rotateYaw(direction.m_82541_(), (float)yawOffsetDegrees);
        Bolt bolt = new Bolt();
        bolt.level = player.m_284548_();
        bolt.position = from;
        bolt.velocity = direction.m_82490_(speed);
        bolt.life = (int)Math.ceil((range > 0.0 ? range : AyatoRules.flameRange(mastery)) / speed) + (kind == Kind.FLARE ? 3 : 0);
        bolt.radius = radius;
        bolt.damage = damage;
        bolt.pierce = pierce;
        bolt.kind = kind;
        bolt.burnSeconds = AyatoRules.flameBurnSeconds(mastery) + (kind == Kind.FLARE ? 2 : 0);
        bolt.embers = AyatoRules.flameEmbers(mastery) || kind == Kind.FLARE;
        BOLTS.computeIfAbsent(player.m_20148_(), ignored -> new ArrayList<>()).add(bolt);
        float size = size(kind) * (mastery >= 500 ? 1.2F : 1.0F);
        if (scale > 0.0F) {
            size = scale * (mastery >= 500 ? 1.2F : 1.0F);
        }
        fx(player, S2CAyatoFx.BOLT + kind.ordinal(), bolt.id, from, bolt.velocity, size, bolt.life);
    }

    /** One tick of flight. Returns false once the bolt is spent. */
    private static boolean step(ServerPlayer player, Bolt bolt, long now) {
        ServerLevel level = player.m_284548_();
        Vec3 from = bolt.position;
        Vec3 to = from.m_82549_(bolt.velocity);
        BlockHitResult block = level.m_45547_(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        boolean wall = block.m_6662_() == HitResult.Type.BLOCK;
        Vec3 end = wall ? block.m_82450_() : to;
        // 1.0.42: another's kagune M1 on its way meets a shard coming at him first (KaguneClash): it bursts there and goes no
        // further. (Not the Flare: a fireball, not a shard.)
        Vec3 met = bolt.kind == Kind.FLARE ? null : KaguneClash.swat(player, level, from, end, now);
        if (met != null) {
            end = met;
            wall = false;
        }

        // Everything the bolt's path passes within reach of, in order along the path.
        Vec3 middle = from.m_82549_(end).m_82490_(0.5);
        Vec3 shift = middle.m_82546_(player.m_20191_().m_82399_());
        AABB search = player.m_20191_().m_82386_(shift.f_82479_, shift.f_82480_, shift.f_82481_).m_82400_(from.m_82554_(end) * 0.5 + bolt.radius + 1.0);
        List<LivingEntity> candidates = level.m_6443_(LivingEntity.class, search,
            entity -> entity != player && entity.m_6084_() && !bolt.hit.contains(entity.m_19879_())
                && KaguneServerController.validSwipeTarget(player, entity));
        int samples = Math.max(1, (int)Math.ceil(from.m_82554_(end) / 0.25));
        for (int sample = 0; sample <= samples; sample++) {
            Vec3 point = from.m_82549_(end.m_82546_(from).m_82490_(sample / (double)samples));
            for (LivingEntity target : candidates) {
                if (bolt.hit.contains(target.m_19879_()) || !target.m_20191_().m_82400_(bolt.radius).m_82390_(point)) {
                    continue;
                }
                bolt.hit.add(target.m_19879_());
                // 5.0.14: a Kaneki tentacle guarding him takes the shot; it bursts on the tentacle, or what the guard couldn't hold
                // goes on into him.
                TendrilShieldServer.Guarded guarded = TendrilShieldServer.guard(target, from, bolt.velocity, point, bolt.damage, now);
                if (guarded != null && guarded.through() <= 0.0F) {
                    fx(player, S2CAyatoFx.IMPACT, bolt.id, guarded.at(), bolt.velocity, 0.0F, bolt.kind.ordinal());
                    return false;
                }
                strike(player, bolt, target, guarded != null ? guarded.through() : bolt.damage);
                if (bolt.pierce-- <= 0) {
                    impact(player, bolt, point, now, true);
                    return false;
                }
            }
        }
        if (met != null) {
            fx(player, S2CAyatoFx.IMPACT, bolt.id, met, bolt.velocity, 0.0F, bolt.kind.ordinal());
            return false;
        }
        if (wall) {
            impact(player, bolt, end, now, false);
            return false;
        }
        bolt.position = to;
        if (--bolt.life <= 0) {
            fx(player, S2CAyatoFx.FIZZLE, bolt.id, to, bolt.velocity, 1.0F, bolt.kind.ordinal());
            return false;
        }
        return true;
    }

    /** 5.0.14: every fire shot in flight, for Kaneki's guards to see coming ({@link TendrilShieldServer}). */
    static void shots(TendrilShieldServer.Shots visitor) {
        for (Map.Entry<UUID, List<Bolt>> entry : BOLTS.entrySet()) {
            for (Bolt bolt : entry.getValue()) {
                visitor.shot(entry.getKey(), bolt.level, bolt.position, bolt.velocity, bolt.radius);
            }
        }
    }

    private static void strike(ServerPlayer player, Bolt bolt, LivingEntity target, float damage) {
        Vec3 push = bolt.velocity;
        if (!burn(player, target, damage, bolt.burnSeconds, push)) {
            return;
        }
        if (push.m_165925_() > 1.0E-6) {
            target.m_147240_(bolt.kind == Kind.FLARE ? 0.6 : 0.2, -push.f_82479_, -push.f_82481_);
        }
        if (!bolt.landed) {
            bolt.landed = true;
            KaguneProfile.addMastery(player, "Swipe", 1);
            KaguneNetwork.broadcast(player);
        }
    }

    /**
     * Damages and sets fire to {@code target}; true if the hit landed. Fire comes faster than hurt immunity: each hit counts.
     * {@code push} is the way the fire drives in; null or zero, from him toward the target (6.0.10: three callers pass null, and the
     * Rising Flame threw on every target it caught, kicking the player off the server).
     */
    private static boolean burn(ServerPlayer player, LivingEntity target, float damage, int burnSeconds, Vec3 push) {
        target.f_19802_ = 0;
        Vec3 centre = KaguneServerController.center(target);
        Vec3 into = push != null && push.m_82556_() > 1.0E-6 ? push.m_82541_() : centre.m_82546_(player.m_146892_()).m_82541_();
        WoundHits.expect(target, com.hex.kagune.control.wound.WoundBook.Blow.BURN, centre.m_82546_(into.m_82490_(0.3)), into, 0.0);
        if (!target.m_6469_(player.m_269291_().m_269075_(player), damage)) {
            return false;
        }
        target.m_20254_(burnSeconds);
        sound(player.m_284548_(), KaguneServerController.center(target), "entity.generic.burn", 0.5F, KaguneServerController.vary(player, 1.2F, 0.1F));
        ControlServer.onKaguneHit(player, 1);
        return true;
    }

    /** The bolt bursts at {@code at}: everyone sees the fire, and the Flare splashes and leaves embers. */
    private static void impact(ServerPlayer player, Bolt bolt, Vec3 at, long now, boolean hit) {
        ServerLevel level = player.m_284548_();
        fx(player, S2CAyatoFx.IMPACT, bolt.id, at, bolt.velocity, hit ? 1.0F : 0.0F, bolt.kind.ordinal());
        if (AyatoServer.hasMutation(player, AyatoMutation.BURSTING_SHARDS)) {
            shardBlast(player, bolt, at);
        }
        if (bolt.kind == Kind.FLARE) {
            splash(player, bolt, at, 2.6, bolt.damage * 0.6F);
            sound(level, at, "entity.generic.explode", 0.5F, KaguneServerController.vary(player, 1.45F, 0.1F));
        }
        if (bolt.embers) {
            EMBERS.computeIfAbsent(player.m_20148_(), ignored -> new ArrayList<>()).add(new Embers(at, now + 40L, 1.0F));
            fx(player, S2CAyatoFx.EMBERS, 0, at, Vec3.f_82478_, 1.2F, 40);
        }
    }

    /**
     * Bursting Shards (mutation): the shard explodes where it hit, catching everyone near (the one it hit too) for a share of
     * its damage, burning them.
     */
    private static void shardBlast(ServerPlayer player, Bolt bolt, Vec3 at) {
        double radius = AyatoRules.SHARD_EXPLOSION_RADIUS;
        Vec3 shift = at.m_82546_(player.m_20191_().m_82399_());
        AABB area = player.m_20191_().m_82386_(shift.f_82479_, shift.f_82480_, shift.f_82481_).m_82400_(radius);
        float damage = bolt.damage * AyatoRules.SHARD_EXPLOSION_SHARE;
        for (LivingEntity target : player.m_284548_().m_6443_(LivingEntity.class, area,
            entity -> entity != player && entity.m_6084_() && KaguneServerController.validSwipeTarget(player, entity))) {
            if (KaguneServerController.center(target).m_82557_(at) <= (radius + 0.6) * (radius + 0.6)) {
                burn(player, target, damage, bolt.burnSeconds, Vec3.f_82478_);
            }
        }
        fx(player, S2CAyatoFx.BURST_BLAST, 0, at, Vec3.f_82478_, (float)(radius / 2.2), 1);
        sound(player.m_284548_(), at, "entity.generic.explode", 0.35F, KaguneServerController.vary(player, 1.6F, 0.1F));
    }

    /** Area damage around {@code at} for everyone the bolt hasn't already hit. */
    private static void splash(ServerPlayer player, Bolt bolt, Vec3 at, double radius, float damage) {
        Vec3 shift = at.m_82546_(player.m_20191_().m_82399_());
        AABB area = player.m_20191_().m_82386_(shift.f_82479_, shift.f_82480_, shift.f_82481_).m_82400_(radius);
        for (LivingEntity target : player.m_284548_().m_6443_(LivingEntity.class, area,
            entity -> entity != player && entity.m_6084_() && KaguneServerController.validSwipeTarget(player, entity))) {
            if (bolt.hit.add(target.m_19879_()) && KaguneServerController.center(target).m_82557_(at) <= (radius + 0.6) * (radius + 0.6)) {
                strike(player, bolt, target, damage);
            }
        }
    }

    /** Embers on the ground: a little fire every half second for whoever stands in them (drawn by the clients). */
    private static void burnEmbers(ServerPlayer player, Embers patch, long now) {
        if (now % 10L != 0L) {
            return;
        }
        ServerLevel level = player.m_284548_();
        Vec3 shift = patch.at().m_82546_(player.m_20191_().m_82399_());
        AABB area = player.m_20191_().m_82386_(shift.f_82479_, shift.f_82480_, shift.f_82481_).m_82400_(1.2);
        for (LivingEntity target : level.m_6443_(LivingEntity.class, area,
            entity -> entity != player && entity.m_6084_() && KaguneServerController.validSwipeTarget(player, entity))) {
            target.f_19802_ = 0;
            // Standing in his embers: the legs burn.
            Vec3 into = horizontal(KaguneServerController.center(target).m_82546_(patch.at()));
            WoundHits.expect(target, com.hex.kagune.control.wound.WoundBook.Blow.BURN, target.m_20182_().m_82520_(0.0, 0.35, 0.0).m_82546_(
                into.m_82490_(0.3)), into, 0.0);
            if (target.m_6469_(player.m_269291_().m_269075_(player), patch.damage())) {
                target.m_20254_(2);
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    /** Throws the player: the new motion, and the client is told at once. */
    static void push(ServerPlayer player, Vec3 motion) {
        player.m_20256_(motion);
        player.f_19864_ = true;
    }

    static Vec3 horizontal(Vec3 direction) {
        double length = Math.sqrt(direction.f_82479_ * direction.f_82479_ + direction.f_82481_ * direction.f_82481_);
        return length < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : new Vec3(direction.f_82479_ / length, 0.0, direction.f_82481_ / length);
    }

    static void fx(ServerPlayer player, int kind, int id, Vec3 at, Vec3 velocity, float size, int extra) {
        ControlNetwork.broadcast(player, new S2CAyatoFx(kind, player.m_19879_(), id, at.f_82479_, at.f_82480_, at.f_82481_, (float)velocity.f_82479_,
            (float)velocity.f_82480_, (float)velocity.f_82481_, size, extra));
    }

    /** A vanilla sound by name (heard by everyone near). */
    static void sound(ServerLevel level, Vec3 at, String id, float volume, float pitch) {
        ResourceLocation location = ResourceLocation.m_214293_("minecraft", id);
        if (location != null) {
            level.m_6263_(null, at.f_82479_, at.f_82480_, at.f_82481_, SoundEvent.m_262824_(location), SoundSource.PLAYERS, volume, pitch);
        }
    }
}
