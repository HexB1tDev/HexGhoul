package com.hex.kagune.control.server;

import com.hex.kagune.control.ComboStage;
import com.hex.kagune.control.StrikePattern;
import com.hex.kagune.control.ayato.AyatoRules;
import com.hex.kagune.control.ayato.KakujaRig;
import com.hex.kagune.control.ayato.WingRig;
import com.hex.kagune.control.network.S2CAyatoFx;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.KaguneRuntime;
import com.hex.kagune.gameplay.KaguneServerController;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Ayato's final kakuja on the server (K held, mastery 1000): needles from the three right tentacles (they bleed, through
 * {@link AyatoCrystal}) and fire from the three left fire tendrils (it burns, through {@link AyatoFire}), resolved at each
 * stage's impact tick. A burning body bleeds twice as hard (Scorched wounds, in {@code AyatoCrystal.bleed}).
 *
 * <p>The M1 chain takes the sides in turn: Needle Pair (two right tentacles), Fire Lash (one tendril whips three shards across),
 * Needle Fan (all three right), Fireball (from between two tendrils), Crossfire (burning needles from the right and fire from the
 * left, crossing at the target). The enders, by how many shots came before: Ember Hop (the tendrils beat down and throw him
 * back out of a burst of fire), Searing Harrow (a point-blank cone of burning needles and fire), Twin Serpents (a fan of
 * needles sweeping right to left while a fan of fire sweeps left to right), Pyre Lance (one thick burning needle through
 * everything, the ground burning where it stops), Hellrain (needles and fireballs thrown up and coming down together) and the
 * Crimson Pyre (every needle stuck in anyone bursts into flame, and a ring of needles and fire flies out round him).
 */
final class AyatoPyre {
    /** The Hellrain still coming down: where, how hard, and what is left to fall. */
    private static final class Rain {
        long started;
        int targetId;
        Vec3 aimPoint;
        float damage;
        int needles;
        int fireballs;
    }

    private static final Map<UUID, Rain> RAINS = new HashMap<>();

    private AyatoPyre() {
    }

    /** The impact tick of a final kakuja stage ({@code started} and {@code duration} are the stage's own timing). */
    static void resolve(ServerPlayer player, KaguneRuntime.State state, ComboStage stage, int limbMask, Vec3 direction, long started, int duration) {
        int mastery = KaguneProfile.mastery(player);
        long now = player.m_9236_().m_46467_();
        Vec3 aim = AyatoFire.aimPoint(player, state, direction, AyatoRules.NEEDLE_RANGE);
        float damage = AyatoRules.meleeDamage(mastery) * stage.damageMul();
        switch (stage.pattern()) {
            case NEEDLE_PAIR -> needlePair(player, limbMask, aim, damage, now);
            case FIRE_LASH -> fireLash(player, limbMask, aim, damage);
            case NEEDLE_FAN -> needleFan(player, aim, damage, now);
            case FIREBALL -> fireball(player, aim, damage);
            case CROSSFIRE -> crossfire(player, aim, damage, now);
            case EMBER_HOP -> emberHop(player, damage, mastery, now);
            case SEARING_HARROW -> searingHarrow(player, aim, damage, now);
            case TWIN_SERPENTS -> twinSerpents(player, aim, damage, now);
            case PYRE_LANCE -> pyreLance(player, aim, damage, now);
            case HELLRAIN -> hellrain(player, state, aim, damage);
            case CRIMSON_PYRE -> crimsonPyre(player, damage, mastery, now);
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------ the M1

    /** Needle Pair: each right tentacle in the mask sends its volley of needles from the spikes along its belly, one after another. */
    private static void needlePair(ServerPlayer player, int limbMask, Vec3 aim, float damage, long now) {
        int fired = 0;
        for (int index = 0; index < 3; index++) {
            if ((limbMask & 1 << (20 + index)) == 0) {
                continue;
            }
            Vec3[] muzzles = AyatoCrystal.muzzles(player, index, aim, AyatoRules.VOLLEY_NEEDLES);
            for (int k = 0; k < muzzles.length; k++) {
                AyatoCrystal.queue(player, now + (long)k * AyatoRules.NEEDLE_STAGGER, muzzles[k], aim, damage);
                fired++;
            }
        }
        if (fired > 0) {
            AyatoFire.sound(player.m_284548_(), player.m_146892_(), "block.amethyst_block.hit", 0.5F, KaguneServerController.vary(player, 1.8F, 0.1F));
        }
    }

    /** Fire Lash: the tendril in the mask whips its shards across the target, left to right, one a tick. */
    private static void fireLash(ServerPlayer player, int limbMask, Vec3 aim, float damage) {
        int index = 3;
        for (int candidate = 3; candidate < KakujaRig.TENTACLES; candidate++) {
            if ((limbMask & 1 << (20 + candidate)) != 0) {
                index = candidate;
                break;
            }
        }
        Vec3 from = mouth(player, index, aim);
        int shards = AyatoRules.LASH_SHARDS;
        for (int k = 0; k < shards; k++) {
            double yaw = -AyatoRules.LASH_HALF_ANGLE + 2.0 * AyatoRules.LASH_HALF_ANGLE * k / Math.max(1.0, shards - 1.0);
            AyatoFire.shard(player, from, aim, AyatoFire.Kind.SHOT, damage, yaw, 0.0F, k);
        }
        AyatoFire.sound(player.m_284548_(), from, "entity.blaze.shoot", 0.7F, KaguneServerController.vary(player, 1.25F, 0.08F));
    }

    /** Needle Fan: needles out of all three right tentacles, fanned across the target, three a tick. */
    private static void needleFan(ServerPlayer player, Vec3 aim, float damage, long now) {
        Vec3 ahead = ahead(player, aim);
        int count = AyatoRules.NEEDLE_FAN_NEEDLES;
        for (int k = 0; k < count; k++) {
            double t = k / (count - 1.0);
            float yaw = (float)(-AyatoRules.NEEDLE_FAN_HALF_ANGLE + 2.0 * AyatoRules.NEEDLE_FAN_HALF_ANGLE * t);
            int index = k % 3;
            Vec3 from = AyatoCrystal.muzzles(player, index, aim, 3)[(k / 3) % 3];
            AyatoCrystal.Pending pending = AyatoCrystal.queue(player, now + k / 3, from, aim, damage);
            pending.direction = KaguneCombat.rotateYaw(ahead, yaw);
            pending.spread = 0.8F;
        }
        AyatoFire.sound(player.m_284548_(), player.m_146892_(), "block.amethyst_cluster.break", 0.7F, KaguneServerController.vary(player, 1.5F, 0.06F));
    }

    /** Fireball: one big ball of fire from between the upper two tendrils; it bursts where it lands and leaves the ground burning. */
    private static void fireball(ServerPlayer player, Vec3 aim, float damage) {
        Vec3 from = mouth(player, 3, aim).m_82549_(mouth(player, 4, aim)).m_82490_(0.5);
        AyatoFire.shard(player, from, aim, AyatoFire.Kind.FLARE, damage, 0.0, AyatoRules.FIREBALL_SIZE, 0L);
        AyatoFire.sound(player.m_284548_(), from, "item.firecharge.use", 1.0F, KaguneServerController.vary(player, 0.75F, 0.05F));
        AyatoFire.sound(player.m_284548_(), from, "entity.blaze.shoot", 0.6F, KaguneServerController.vary(player, 0.8F, 0.05F));
    }

    /** Crossfire: burning needles from the right, a shard from each tendril on the left, all at the target so they cross on it. */
    private static void crossfire(ServerPlayer player, Vec3 aim, float damage, long now) {
        for (int index = 0; index < 3; index++) {
            Vec3[] muzzles = AyatoCrystal.muzzles(player, index, aim, AyatoRules.CROSSFIRE_NEEDLES);
            for (int k = 0; k < muzzles.length; k++) {
                AyatoCrystal.Pending pending = AyatoCrystal.queue(player, now + index + k, muzzles[k], aim, damage);
                pending.burnSeconds = AyatoRules.BURNING_NEEDLE_SECONDS;
            }
        }
        for (int index = 3; index < KakujaRig.TENTACLES; index++) {
            AyatoFire.shard(player, mouth(player, index, aim), aim, AyatoFire.Kind.SHOT, damage * 2.0F, 0.0, 0.0F, index - 3);
        }
        AyatoFire.sound(player.m_284548_(), player.m_146892_(), "block.amethyst_cluster.break", 0.8F, KaguneServerController.vary(player, 1.3F, 0.06F));
        AyatoFire.sound(player.m_284548_(), player.m_146892_(), "entity.blaze.shoot", 0.8F, KaguneServerController.vary(player, 1.0F, 0.06F));
    }

    // ------------------------------------------------------------------ the enders

    /** Ember Hop: the fire tendrils beat down and throw him back; where he stood bursts into fire and keeps burning. */
    private static void emberHop(ServerPlayer player, float damage, int mastery, long now) {
        ServerLevel level = player.m_284548_();
        Vec3 feet = player.m_20182_();
        Vec3 back = AyatoFire.horizontal(player.m_20154_()).m_82490_(-AyatoRules.EMBER_HOP_BACK);
        double radius = AyatoRules.EMBER_HOP_RADIUS;
        AABB area = player.m_20191_().m_82400_(radius);
        for (LivingEntity target : level.m_6443_(LivingEntity.class, area,
            entity -> entity != player && entity.m_6084_() && KaguneServerController.validSwipeTarget(player, entity))) {
            Vec3 away = target.m_20182_().m_82546_(feet);
            if (away.m_82556_() > (radius + 0.6) * (radius + 0.6)) {
                continue;
            }
            if (AyatoFire.scorch(player, target, damage, AyatoRules.flameBurnSeconds(mastery) + 1) && away.m_165925_() > 1.0E-4) {
                target.m_147240_(0.8, -away.f_82479_, -away.f_82481_);
            }
        }
        AyatoFire.push(player, new Vec3(back.f_82479_, AyatoRules.EMBER_HOP_UP, back.f_82481_));
        AyatoFire.embers(player, feet, now, AyatoRules.EMBER_TICKS);
        AyatoFire.fx(player, S2CAyatoFx.HOP, 0, feet, back, 1.3F, 0);
        AyatoFire.fx(player, S2CAyatoFx.BURST_BLAST, 0, feet.m_82520_(0.0, 0.3, 0.0), Vec3.f_82478_, (float)(radius / 2.2), 1);
        AyatoFire.sound(level, feet, "item.firecharge.use", 1.0F, KaguneServerController.vary(player, 0.9F, 0.06F));
        AyatoFire.sound(level, feet, "entity.ender_dragon.flap", 0.5F, KaguneServerController.vary(player, 1.6F, 0.08F));
    }

    /** Searing Harrow: a point-blank cone of burning needles from the right three, and fire shards from each tendril into it. */
    private static void searingHarrow(ServerPlayer player, Vec3 aim, float damage, long now) {
        Vec3 ahead = ahead(player, aim);
        for (int index = 0; index < 3; index++) {
            for (Vec3 from : AyatoCrystal.muzzles(player, index, aim, AyatoRules.SEARING_NEEDLES)) {
                AyatoCrystal.Pending pending = AyatoCrystal.queue(player, now, from, aim, damage);
                pending.direction = ahead;
                pending.spread = AyatoRules.HARROW_CONE;
                pending.range = AyatoRules.HARROW_RANGE;
                pending.knockback = AyatoRules.HARROW_KNOCKBACK;
                pending.burnSeconds = AyatoRules.BURNING_NEEDLE_SECONDS;
            }
        }
        Vec3 eye = player.m_146892_();
        Vec3 reach = eye.m_82549_(ahead.m_82490_(AyatoRules.SEARING_FIRE_RANGE));
        for (int index = 3; index < KakujaRig.TENTACLES; index++) {
            Vec3 from = mouth(player, index, aim);
            for (int k = 0; k < AyatoRules.SEARING_SHARDS; k++) {
                double yaw = (player.m_217043_().m_188500_() * 2.0 - 1.0) * AyatoRules.HARROW_CONE;
                AyatoFire.shard(player, from, reach, AyatoFire.Kind.FAN, damage * 1.5F, yaw, 0.0F, k, AyatoRules.SEARING_FIRE_RANGE);
            }
        }
        AyatoFire.sound(player.m_284548_(), eye, "block.amethyst_cluster.break", 1.1F, KaguneServerController.vary(player, 1.0F, 0.05F));
        AyatoFire.sound(player.m_284548_(), eye, "item.firecharge.use", 1.0F, KaguneServerController.vary(player, 1.1F, 0.05F));
    }

    /**
     * Twin Serpents: a fan of needles from the right tentacles sweeps across from his right to his left while a fan of fire from
     * the tendrils sweeps the other way, over the same few ticks.
     */
    private static void twinSerpents(ServerPlayer player, Vec3 aim, float damage, long now) {
        Vec3 ahead = ahead(player, aim);
        int sweep = AyatoRules.SERPENT_SWEEP_TICKS;
        int needles = AyatoRules.SERPENT_NEEDLES;
        for (int k = 0; k < needles; k++) {
            double t = k / (needles - 1.0);
            float yaw = (float)(AyatoRules.SERPENT_NEEDLE_HALF_ANGLE - 2.0 * AyatoRules.SERPENT_NEEDLE_HALF_ANGLE * t);
            int index = k % 3;
            Vec3 from = AyatoCrystal.muzzles(player, index, aim, 3)[(k / 3) % 3];
            AyatoCrystal.Pending pending = AyatoCrystal.queue(player, now + (long)Math.floor(t * sweep), from, aim, damage);
            pending.direction = KaguneCombat.rotateYaw(ahead, yaw);
            pending.spread = 1.0F;
        }
        int shards = AyatoRules.SERPENT_SHARDS;
        Vec3 eye = player.m_146892_();
        Vec3 front = eye.m_82549_(ahead.m_82490_(AyatoRules.NEEDLE_RANGE));
        for (int k = 0; k < shards; k++) {
            double t = k / (shards - 1.0);
            double yaw = -AyatoRules.SERPENT_FIRE_HALF_ANGLE + 2.0 * AyatoRules.SERPENT_FIRE_HALF_ANGLE * t;
            AyatoFire.shard(player, mouth(player, 3 + k % 3, aim), front, AyatoFire.Kind.FAN, damage * 2.0F, yaw, 0.0F, (long)Math.floor(t * sweep));
        }
        AyatoFire.sound(player.m_284548_(), eye, "block.amethyst_cluster.break", 1.0F, KaguneServerController.vary(player, 1.3F, 0.05F));
        AyatoFire.sound(player.m_284548_(), eye, "item.firecharge.use", 0.9F, KaguneServerController.vary(player, 1.2F, 0.05F));
    }

    /** Pyre Lance: all six converge in front of his chest and drive one thick burning needle through everything in its line. */
    private static void pyreLance(ServerPlayer player, Vec3 aim, float damage, long now) {
        Vec3 eye = player.m_146892_();
        Vec3 ahead = ahead(player, aim);
        Vec3 from = AyatoVortex.clearOfBlocks(player, eye, eye.m_82520_(0.0, -0.3, 0.0).m_82549_(ahead.m_82490_(1.2)));
        AyatoCrystal.Pending pending = AyatoCrystal.queue(player, now, from, aim, damage);
        pending.spread = 0.0F;
        pending.size = AyatoRules.PYRE_LANCE_SIZE;
        pending.pierce = AyatoRules.LINE_PIERCE;
        pending.stacks = 2;
        pending.knockback = 0.6F;
        pending.speed = 1.15F;
        pending.range = AyatoRules.NEEDLE_RANGE + AyatoRules.LINE_EXTRA_RANGE;
        pending.burnSeconds = AyatoRules.PYRE_LANCE_SECONDS;
        pending.embers = true;
        AyatoFire.sound(player.m_284548_(), from, "block.amethyst_cluster.break", 1.2F, KaguneServerController.vary(player, 0.7F, 0.05F));
        AyatoFire.sound(player.m_284548_(), from, "item.firecharge.use", 1.2F, KaguneServerController.vary(player, 0.6F, 0.05F));
    }

    /** Hellrain: needles and fireballs thrown high out of all six; they come down together a few ticks later ({@link #tick}). */
    private static void hellrain(ServerPlayer player, KaguneRuntime.State state, Vec3 aim, float damage) {
        Rain rain = new Rain();
        rain.started = player.m_9236_().m_46467_();
        rain.targetId = state.actionTargetId;
        rain.aimPoint = aim;
        rain.damage = damage;
        rain.needles = AyatoRules.HELLRAIN_NEEDLES;
        rain.fireballs = AyatoRules.HELLRAIN_FIREBALLS;
        RAINS.put(player.m_20148_(), rain);
        Vec3 up = player.m_146892_().m_82520_(0.0, 10.0, 0.0);
        for (int index = 0; index < KakujaRig.TENTACLES; index++) {
            int side = KakujaRig.sideOf(index);
            if (side > 0) {
                Vec3 from = AyatoCrystal.muzzles(player, index, up, 1)[0];
                AyatoFire.fx(player, S2CAyatoFx.NEEDLE, AyatoCrystal.nextId(), from, new Vec3(side * 0.2, 2.2, 0.0), 1.0F, 5);
            } else {
                Vec3 from = mouth(player, index, up);
                AyatoFire.fx(player, S2CAyatoFx.BOLT + AyatoFire.Kind.FLARE.ordinal(), AyatoCrystal.nextId(), from, new Vec3(side * 0.2, 1.9, 0.0),
                    1.4F, 5);
            }
        }
        AyatoFire.sound(player.m_284548_(), player.m_146892_(), "block.amethyst_cluster.place", 1.0F, KaguneServerController.vary(player, 1.3F, 0.05F));
        AyatoFire.sound(player.m_284548_(), player.m_146892_(), "entity.blaze.shoot", 1.0F, KaguneServerController.vary(player, 0.7F, 0.05F));
    }

    /** Crimson Pyre: every needle he has stuck in anyone bursts into flame, and a ring of needles and fire flies out round him. */
    private static void crimsonPyre(ServerPlayer player, float damage, int mastery, long now) {
        int burst = AyatoCrystal.pyreBurst(player, now);
        Vec3 chest = player.m_20182_().m_82520_(0.0, 1.2, 0.0);
        int needles = AyatoRules.PYRE_RING_NEEDLES;
        for (int k = 0; k < needles; k++) {
            double angle = Math.PI * 2.0 * k / needles + player.m_217043_().m_188500_() * 0.3;
            Vec3 heading = new Vec3(Math.cos(angle), 0.05 + 0.25 * (k % 3 - 1), Math.sin(angle));
            AyatoCrystal.ringNeedle(player, chest.m_82549_(heading), heading, damage * 0.3F, AyatoRules.NEEDLE_RANGE * 0.5);
        }
        int shards = AyatoRules.PYRE_RING_SHARDS;
        for (int k = 0; k < shards; k++) {
            // Between the needles, level with his chest.
            double angle = Math.PI * 2.0 * (k + 0.5) / shards;
            Vec3 heading = new Vec3(Math.cos(angle), 0.05, Math.sin(angle));
            Vec3 from = AyatoVortex.clearOfBlocks(player, chest, chest.m_82549_(heading.m_82490_(0.8)));
            AyatoFire.shard(player, from, from.m_82549_(heading.m_82490_(8.0)), AyatoFire.Kind.FAN, damage * 0.5F, 0.0, 0.0F, 0L,
                AyatoRules.flameRange(mastery) * 0.6);
        }
        AyatoFire.fx(player, S2CAyatoFx.BLOOM, 0, chest, Vec3.f_82478_, 1.0F, burst);
        AyatoFire.sound(player.m_284548_(), chest, "block.amethyst_block.break", 1.2F, KaguneServerController.vary(player, 0.6F, 0.05F));
        AyatoFire.sound(player.m_284548_(), chest, "entity.generic.explode", 0.7F, KaguneServerController.vary(player, 1.2F, 0.05F));
        AyatoFire.sound(player.m_284548_(), chest, "item.firecharge.use", 1.2F, KaguneServerController.vary(player, 0.7F, 0.05F));
    }

    // ------------------------------------------------------------------ the rain that keeps falling

    /** Brings the Hellrain down. Every server tick for Ayato players. */
    static void tick(ServerPlayer player, long now) {
        Rain rain = RAINS.get(player.m_20148_());
        if (rain != null && !rain(player, rain, now)) {
            RAINS.remove(player.m_20148_());
        }
    }

    /** One tick of the Hellrain; false once it is over. */
    private static boolean rain(ServerPlayer player, Rain rain, long now) {
        if (!player.m_6084_() || !KaguneProfile.isActive(player) || rain.needles <= 0 && rain.fireballs <= 0) {
            return false;
        }
        long age = now - rain.started;
        if (age < 5) {
            return true; // still going up
        }
        ServerLevel level = player.m_284548_();
        Vec3 target = rain.aimPoint;
        if (rain.targetId > 0 && level.m_6815_(rain.targetId) instanceof LivingEntity living && living.m_6084_()) {
            target = KaguneServerController.center(living);
        }
        for (int each = 0; each < 2 && rain.needles > 0; each++) {
            boolean first = rain.needles == AyatoRules.HELLRAIN_NEEDLES;
            rain.needles--;
            Vec3[] fall = fall(player, target, first);
            AyatoCrystal.fallingNeedle(player, fall[0], fall[1], rain.damage);
        }
        if ((age - 5) % 3 == 0 && rain.fireballs > 0) {
            boolean first = rain.fireballs == AyatoRules.HELLRAIN_FIREBALLS;
            rain.fireballs--;
            Vec3[] fall = fall(player, target, first);
            AyatoFire.shard(player, fall[0], fall[1], AyatoFire.Kind.FLARE, rain.damage * 3.0F, 0.0, 1.2F, 0L, AyatoRules.RAIN_HEIGHT + 6.0);
            AyatoFire.sound(level, target, "entity.blaze.shoot", 0.6F, KaguneServerController.vary(player, 0.8F, 0.1F));
        }
        if (age % 4 == 0) {
            AyatoFire.sound(level, target, "block.amethyst_block.hit", 0.8F, KaguneServerController.vary(player, 1.5F, 0.15F));
        }
        return true;
    }

    /** Where one thing of the Hellrain starts (high over the target) and lands (round it; the first one right on it). */
    private static Vec3[] fall(ServerPlayer player, Vec3 target, boolean first) {
        double angle = player.m_217043_().m_188500_() * Math.PI * 2.0;
        double r = first ? 0.0 : Math.sqrt(player.m_217043_().m_188500_()) * AyatoRules.HELLRAIN_RADIUS;
        Vec3 ground = target.m_82520_(Math.cos(angle) * r, -0.6, Math.sin(angle) * r);
        Vec3 sky = ground.m_82520_((player.m_217043_().m_188500_() - 0.5) * 1.5, AyatoRules.RAIN_HEIGHT, (player.m_217043_().m_188500_() - 0.5) * 1.5);
        return new Vec3[] {sky, ground};
    }

    /** The shooter left: the rain stops. */
    static void forget(ServerPlayer player) {
        RAINS.remove(player.m_20148_());
    }

    // ------------------------------------------------------------------ helpers

    /**
     * Where fire leaves fire tendril {@code index} (3..5) of this player's final kakuja turned to fire at {@code aim}: in the
     * world, kept out of walls ({@link KakujaRig#mouth} turned exactly as {@link AyatoCrystal#muzzles} turns its points).
     */
    static Vec3 mouth(ServerPlayer player, int index, Vec3 aim) {
        double yaw = Math.toRadians(player.f_20883_);
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double rightX = -Math.cos(yaw);
        double rightZ = -Math.sin(yaw);
        Vec3 shoulders = player.m_20182_().m_82520_(0.0, WingRig.ROOT_Y, 0.0);
        Vec3 toward = aim.m_82546_(shoulders);
        double[] rig = {toward.f_82479_ * rightX + toward.f_82481_ * rightZ, toward.f_82480_, toward.f_82479_ * forwardX + toward.f_82481_ * forwardZ};
        double length = Math.sqrt(rig[0] * rig[0] + rig[1] * rig[1] + rig[2] * rig[2]);
        double[] dir = length < 1.0E-6 ? WingRig.FORWARD : new double[] {rig[0] / length, rig[1] / length, rig[2] / length};
        double[] local = KakujaRig.mouth(index, dir, player.m_6047_());
        Vec3 point = new Vec3(player.m_20185_() + rightX * local[0] + forwardX * local[2], player.m_20186_() + local[1],
            player.m_20189_() + rightZ * local[0] + forwardZ * local[2]);
        return AyatoVortex.clearOfBlocks(player, player.m_146892_(), point);
    }

    /** From his eyes toward {@code aim}, unit (his look if the aim is on top of him). */
    private static Vec3 ahead(ServerPlayer player, Vec3 aim) {
        Vec3 line = aim.m_82546_(player.m_146892_());
        return line.m_82556_() < 1.0E-6 ? player.m_20154_() : line.m_82541_();
    }
}
