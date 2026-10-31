package com.hex.kagune.control.server;

import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CUkakuFx;
import com.hex.kagune.control.npc.EtoNpc;
import com.hex.kagune.control.npc.EtoRules;
import com.hex.kagune.control.npc.NpcRules;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1.3.2: Eto's kagune in a fight, once she takes it seriously ({@link EtoRules.Phase}): her Ukaku's shards, loosed from her shoulders in
 * volleys at whatever she fights from range, and, maniac, a storm of them (a fan from both shoulders at once). Each flies a tick at a
 * time, hits the first body in its way that is fair game (what she fights, or something hostile; never a player she isn't fighting,
 * never her partner), sets it alight, and every client draws it as an Ukaku's shot ({@code S2CUkakuFx}).
 */
final class EtoKagune {
    /** Shot ids clear of the players' and the ghoul NPCs'. */
    private static int nextShard = 3 << 28;

    /** A shard in flight. */
    static final class Shard {
        final int id = nextShard++;
        Vec3 position;
        Vec3 velocity;
        int life;
        float damage;
        final Set<Integer> hit = new HashSet<>();
    }

    /** One Eto's shards and when her next volley and storm are ready. */
    static final class Wings {
        final List<Shard> shards = new ArrayList<>();
        long volleyAt = Long.MIN_VALUE;
        long stormAt = Long.MIN_VALUE;
        int left;
        long nextShot;
        int side = 1;
    }

    private EtoKagune() {
    }

    /** A tick of her fight at range: a volley when it is ready (serious and on), a storm when maniac; true if she loosed anything. */
    static boolean fire(EtoNpc npc, Wings wings, LivingEntity target, ServerLevel level, long now, EtoRules.Phase phase, double distance, boolean sees) {
        if (phase == EtoRules.Phase.CALM || !sees || distance < EtoRules.SHARD_NEAREST || distance > EtoRules.SHARD_RANGE) {
            wings.left = 0;
            return false;
        }
        if (phase == EtoRules.Phase.MANIAC && now >= wings.stormAt) {
            for (int i = 0; i < EtoRules.STORM_SHOTS; i++) {
                double spread = EtoRules.STORM_SPREAD * (2.0 * i / (EtoRules.STORM_SHOTS - 1) - 1.0);
                shoot(npc, wings, target, level, i % 2 == 0 ? 1 : -1, spread, EtoRules.SHARD_SPEED * 1.1, EtoRules.SHARD_DAMAGE * 0.8F * phase.power);
            }
            wings.stormAt = now + EtoRules.STORM_EVERY;
            UkakuFire.sound(level, npc.m_20182_().m_82520_(0.0, 1.3, 0.0), "entity.ender_dragon.flap", 0.9F, 1.7F);
            UkakuFire.sound(level, npc.m_20182_().m_82520_(0.0, 1.3, 0.0), "entity.blaze.shoot", 1.0F, 0.75F);
            return true;
        }
        if (wings.left <= 0) {
            if (now < wings.volleyAt) {
                return false;
            }
            wings.left = phase == EtoRules.Phase.MANIAC ? EtoRules.SHARDS + 2 : EtoRules.SHARDS;
            wings.nextShot = now + 4L;
            wings.volleyAt = now + Math.round(EtoRules.SHARD_EVERY * phase.cooldowns);
            return false;
        }
        if (now < wings.nextShot) {
            return false;
        }
        shoot(npc, wings, target, level, wings.side, 0.0, EtoRules.SHARD_SPEED, EtoRules.SHARD_DAMAGE * phase.power);
        wings.side = -wings.side;
        wings.left--;
        wings.nextShot = now + (phase == EtoRules.Phase.MANIAC ? 2L : 4L);
        return true;
    }

    /** A shard from her shoulder on {@code side} (1 right, -1 left) at {@code target}'s middle, turned {@code spread} degrees off it. */
    private static void shoot(EtoNpc npc, Wings wings, LivingEntity target, ServerLevel level, int side, double spread, double speed, float damage) {
        double yaw = Math.toRadians(npc.f_20883_);
        double rightX = -Math.cos(yaw);
        double rightZ = -Math.sin(yaw);
        double backX = Math.sin(yaw);
        double backZ = -Math.cos(yaw);
        double size = EtoRules.SIZE;
        Vec3 from = new Vec3(npc.m_20185_() + rightX * 0.55 * side * size + backX * 0.35 * size, npc.m_20186_() + 1.45 * size,
            npc.m_20189_() + rightZ * 0.55 * side * size + backZ * 0.35 * size);
        Vec3 aim = GhoulNpcBrain.centre(target);
        Vec3 way = aim.m_82546_(from);
        way = way.m_82556_() < 1.0E-6 ? npc.m_20154_() : way.m_82541_();
        if (spread != 0.0) {
            double turn = Math.toRadians(spread);
            way = new Vec3(way.f_82479_ * Math.cos(turn) - way.f_82481_ * Math.sin(turn), way.f_82480_, way.f_82479_ * Math.sin(turn) + way.f_82481_ * Math.cos(turn));
        }
        Shard shard = new Shard();
        shard.position = from;
        shard.velocity = way.m_82490_(speed);
        shard.life = (int)Math.ceil(EtoRules.SHARD_RANGE / speed);
        shard.damage = damage;
        wings.shards.add(shard);
        fx(npc, S2CUkakuFx.BOLT, shard.id, from, shard.velocity, 1.0F, shard.life);
        UkakuFire.sound(level, from, "entity.blaze.shoot", 0.5F, 1.4F + 0.1F * (float)npc.m_217043_().m_188500_());
    }

    /** Every shard of hers a tick on. {@code target}: what she fights (fair game, with anything hostile); {@code spared}: never hit. */
    static void fly(EtoNpc npc, Wings wings, ServerLevel level, long now, int target, java.util.UUID spared) {
        for (Iterator<Shard> it = wings.shards.iterator(); it.hasNext(); ) {
            if (!step(npc, it.next(), level, now, target, spared)) {
                it.remove();
            }
        }
    }

    private static boolean step(EtoNpc npc, Shard shard, ServerLevel level, long now, int target, java.util.UUID spared) {
        Vec3 from = shard.position;
        Vec3 to = from.m_82549_(shard.velocity);
        BlockHitResult block = level.m_45547_(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, npc));
        boolean wall = block.m_6662_() == HitResult.Type.BLOCK;
        Vec3 end = wall ? block.m_82450_() : to;
        AABB search = new AABB(from.f_82479_, from.f_82480_, from.f_82481_, end.f_82479_, end.f_82480_, end.f_82481_).m_82400_(NpcRules.SHOT_RADIUS + 1.0);
        List<LivingEntity> near = level.m_6443_(LivingEntity.class, search, living -> living != npc && living.m_6084_() && !shard.hit.contains(living.m_19879_())
            && (living.m_19879_() == target || living instanceof Enemy) && !living.m_20148_().equals(spared));
        int samples = Math.max(1, (int)Math.ceil(from.m_82554_(end) / 0.25));
        for (int sample = 0; sample <= samples; sample++) {
            Vec3 point = from.m_82549_(end.m_82546_(from).m_82490_(sample / (double)samples));
            for (LivingEntity body : near) {
                if (!body.m_20191_().m_82400_(NpcRules.SHOT_RADIUS).m_82390_(point)) {
                    continue;
                }
                shard.hit.add(body.m_19879_());
                burn(npc, body, shard, level);
                fx(npc, S2CUkakuFx.IMPACT, shard.id, point, shard.velocity, 1.0F, 0);
                return false;
            }
        }
        if (wall) {
            fx(npc, S2CUkakuFx.IMPACT, shard.id, end, shard.velocity, 0.0F, 0);
            return false;
        }
        shard.position = to;
        if (--shard.life <= 0) {
            fx(npc, S2CUkakuFx.FIZZLE, shard.id, to, shard.velocity, 1.0F, 0);
            return false;
        }
        return true;
    }

    private static void burn(EtoNpc npc, LivingEntity body, Shard shard, ServerLevel level) {
        body.f_19802_ = 0;
        if (!body.m_6469_(npc.m_269291_().m_269333_(npc), shard.damage)) {
            return;
        }
        body.m_20254_(NpcRules.SHOT_BURN);
        if (shard.velocity.m_165925_() > 1.0E-6) {
            body.m_147240_(0.25, -shard.velocity.f_82479_, -shard.velocity.f_82481_);
        }
        UkakuFire.sound(level, GhoulNpcBrain.centre(body), "entity.generic.burn", 0.5F, 1.2F);
    }

    /** Her shards in flight put out (she died, or the fight is over): every client stops drawing them. */
    static void fizzleAll(EtoNpc npc, Wings wings) {
        for (Shard shard : wings.shards) {
            fx(npc, S2CUkakuFx.FIZZLE, shard.id, shard.position, shard.velocity, 1.0F, 0);
        }
        wings.shards.clear();
    }

    private static void fx(EtoNpc npc, int kind, int id, Vec3 at, Vec3 velocity, float size, int extra) {
        ControlNetwork.broadcastAround(npc, new S2CUkakuFx(kind, npc.m_19879_(), id, at.f_82479_, at.f_82480_, at.f_82481_, (float)velocity.f_82479_,
            (float)velocity.f_82480_, (float)velocity.f_82481_, size, extra));
    }
}
