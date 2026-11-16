package com.hex.kagune.control.server;

import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CUkakuFx;
import com.hex.kagune.control.npc.AgentRules;
import com.hex.kagune.control.wound.WoundBook;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 1.3.26: the Ukaku quinque's crystal shards in flight (the launcher's: a player's, an investigator's): each flies straight a tick at a
 * time, strikes the first body it may hurt ({@code foe}), stops at a wall, crumbles at the end of its range. Drawn as Touka's crystal
 * spikes are ({@link S2CUkakuFx}'s spike kinds), smaller; a blow on its way meets one flying at its swinger ({@link KaguneClash#swat}).
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class CrystalShots {
    private static int nextId = (1 << 30) + (1 << 29);
    private static final float RADIUS = 0.3F;
    /** True while a shard's own hit is being dealt (QuinqueServer: not an M1, no string of blows from it). */
    static boolean striking;

    static final class Shot {
        final int id = nextId++;
        LivingEntity shooter;
        ServerLevel level;
        Vec3 position;
        Vec3 velocity;
        int life;
        double remaining;
        float damage;
        boolean lightning;
        Predicate<LivingEntity> foe;
        final Set<Integer> hit = new HashSet<>();
        /** 2.0.48: one ball of the White Reaper's volley (its place in the ring, -1: none): where it left and along what, how far it aimed, how far it has gone. */
        int ring = -1;
        double[] origin;
        double[] way;
        double aimed;
        double travelled;
    }

    private static final List<Shot> SHOTS = new ArrayList<>();

    private CrystalShots() {
    }

    /** {@code shooter} fires a shard from {@code from} along {@code way} (unit), {@code speed} a tick, {@code range} far, at {@code foe}. */
    public static void fire(LivingEntity shooter, ServerLevel level, Vec3 from, Vec3 way, double speed, double range, float damage,
                            Predicate<LivingEntity> foe) {
        launch(shooter, level, from, way, speed, range, damage, foe, false);
    }

    public static void lightning(LivingEntity shooter, ServerLevel level, Vec3 from, Vec3 way, Predicate<LivingEntity> foe) {
        lightning(shooter, level, from, way, foe, AgentRules.DAWN_SHOT_DAMAGE);
    }

    /** 2.0.32: a lightning ball of {@code damage} at its heart (a player's Dawn: Quinque.DAWN_PLAYER_SHOT), its blast in proportion. */
    public static void lightning(LivingEntity shooter, ServerLevel level, Vec3 from, Vec3 way, Predicate<LivingEntity> foe, float damage) {
        launch(shooter, level, from, way, AgentRules.DAWN_SHOT_SPEED, AgentRules.DAWN_SHOT_RANGE, damage, foe, true);
    }

    /**
     * 2.0.48: the White Reaper's volley ({@link com.hex.kagune.control.quinque.DawnVolley}): its balls from {@code from} along {@code way}
     * (unit), opening into a ring and closing again on the point {@code aimed} blocks along, each of {@code damage}.
     */
    public static void volley(LivingEntity shooter, ServerLevel level, Vec3 from, Vec3 way, double aimed, Predicate<LivingEntity> foe, float damage) {
        if (SHOTS.size() > 400) {
            return;
        }
        double speed = AgentRules.DAWN_SHOT_SPEED;
        double range = AgentRules.DAWN_SHOT_RANGE;
        for (int index = 0; index < com.hex.kagune.control.quinque.DawnVolley.COUNT; index++) {
            Shot shot = new Shot();
            shot.shooter = shooter;
            shot.level = level;
            shot.position = from;
            shot.velocity = way.m_82490_(speed);
            shot.life = (int)Math.ceil(range / speed);
            shot.remaining = range;
            shot.damage = damage;
            shot.foe = foe;
            shot.lightning = true;
            shot.ring = index;
            shot.origin = new double[] {from.f_82479_, from.f_82480_, from.f_82481_};
            shot.way = new double[] {way.f_82479_, way.f_82480_, way.f_82481_};
            shot.aimed = aimed;
            SHOTS.add(shot);
            fx(shot, S2CUkakuFx.DAWN_RING, from, shot.velocity, (float)aimed, index * 100 + Math.min(99, shot.life));
        }
    }

    private static void launch(LivingEntity shooter, ServerLevel level, Vec3 from, Vec3 way, double speed, double range, float damage,
                               Predicate<LivingEntity> foe, boolean lightning) {
        if (SHOTS.size() > 400) {
            return; // never a flood
        }
        Shot shot = new Shot();
        shot.shooter = shooter;
        shot.level = level;
        shot.position = from;
        shot.velocity = way.m_82490_(speed);
        shot.life = (int)Math.ceil(range / speed);
        shot.remaining = range;
        shot.damage = damage;
        shot.foe = foe;
        shot.lightning = lightning;
        SHOTS.add(shot);
        fx(shot, lightning ? S2CUkakuFx.DAWN_BALL : S2CUkakuFx.SPIKE, from, shot.velocity, 0.55F, shot.life);
    }

    @SubscribeEvent
    public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || SHOTS.isEmpty() || !(event.level instanceof ServerLevel level)) {
            return;
        }
        for (Iterator<Shot> it = SHOTS.iterator(); it.hasNext(); ) {
            Shot shot = it.next();
            if (shot.level != level) {
                continue; // each world flies its own
            }
            boolean flying;
            try {
                flying = !shot.shooter.m_213877_() && step(shot);
            } catch (RuntimeException failure) {
                com.hex.kagune.control.Safety.caught(failure, "a crystal shard");
                flying = false;
            }
            if (!flying) {
                it.remove();
            }
        }
    }

    /** A shard a tick on: the first body it may hurt is struck, a wall stops it, and it crumbles at the end of its range. */
    private static boolean step(Shot shot) {
        ServerLevel level = shot.level;
        Vec3 from = shot.position;
        double speed = shot.ring >= 0 ? AgentRules.DAWN_SHOT_SPEED : shot.velocity.m_82553_();
        Vec3 to;
        if (shot.ring >= 0) {
            // 2.0.48: a volley's ball along its ring (DawnVolley), its velocity this tick's step.
            shot.travelled += Math.min(speed, Math.max(0.0, shot.remaining));
            double[] next = com.hex.kagune.control.quinque.DawnVolley.at(shot.origin, shot.way, shot.ring, shot.aimed, shot.travelled);
            to = new Vec3(next[0], next[1], next[2]);
            Vec3 moved = to.m_82546_(from);
            if (moved.m_82556_() > 1.0E-8) {
                shot.velocity = moved;
            }
        } else {
            Vec3 step = shot.lightning && shot.remaining < speed ? shot.velocity.m_82490_(shot.remaining / speed) : shot.velocity;
            to = from.m_82549_(step);
        }
        shot.remaining -= speed;
        Vec3 met = KaguneClash.swat(shot.shooter, level, from, to, level.m_46467_());
        if (met != null) {
            fx(shot, shot.lightning ? S2CUkakuFx.DAWN_BURST : S2CUkakuFx.SPIKE_IMPACT, met, shot.velocity, 0.0F, 0);
            return false; // swatted out of the air by a blow on its way
        }
        BlockHitResult block = level.m_45547_(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shot.shooter));
        boolean wall = block.m_6662_() == HitResult.Type.BLOCK;
        Vec3 end = wall ? block.m_82450_() : to;
        AABB search = new AABB(from.f_82479_, from.f_82480_, from.f_82481_, end.f_82479_, end.f_82480_, end.f_82481_).m_82400_(RADIUS + 1.0);
        List<LivingEntity> near = level.m_6443_(LivingEntity.class, search, living -> living != shot.shooter && living.m_6084_()
            && !shot.hit.contains(living.m_19879_()) && shot.foe.test(living));
        int samples = Math.max(1, (int)Math.ceil(from.m_82554_(end) / 0.25));
        for (int sample = 0; sample <= samples; sample++) {
            Vec3 point = from.m_82549_(end.m_82546_(from).m_82490_(sample / (double)samples));
            for (LivingEntity body : near) {
                if (!body.m_20191_().m_82400_(RADIUS).m_82390_(point)) {
                    continue;
                }
                if (shot.lightning) {
                    burst(shot, point, body);
                    return false;
                }
                shot.hit.add(body.m_19879_());
                pierce(shot, body);
                fx(shot, S2CUkakuFx.SPIKE_IMPACT, point, shot.velocity, 1.0F, 0);
                return false;
            }
        }
        if (wall) {
            if (shot.lightning) {
                burst(shot, end.m_82546_(shot.velocity.m_82541_().m_82490_(0.04)), null);
            } else {
                fx(shot, S2CUkakuFx.SPIKE_IMPACT, end, shot.velocity, 0.0F, 0);
            }
            return false;
        }
        shot.position = to;
        if (--shot.life <= 0) {
            fx(shot, shot.lightning ? S2CUkakuFx.DAWN_BURST : S2CUkakuFx.SPIKE_FIZZLE, to, shot.velocity, shot.lightning ? 0.0F : 1.0F, 0);
            return false;
        }
        return true;
    }

    /** An electrical burst damages exposed targets. It never ignites entities or breaks blocks. */
    private static void burst(Shot shot, Vec3 at, LivingEntity direct) {
        double radius = AgentRules.DAWN_BLAST_RADIUS;
        AABB area = new AABB(at.f_82479_ - radius, at.f_82480_ - radius, at.f_82481_ - radius,
            at.f_82479_ + radius, at.f_82480_ + radius, at.f_82481_ + radius);
        for (LivingEntity body : shot.level.m_6443_(LivingEntity.class, area,
            body -> body != shot.shooter && body.m_6084_() && shot.foe.test(body))) {
            Vec3 centre = GhoulNpcBrain.centre(body);
            double distance = at.m_82554_(centre);
            if (body != direct && (distance > radius || shot.level.m_45547_(new ClipContext(at, centre, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, shot.shooter)).m_6662_() == HitResult.Type.BLOCK)) {
                continue;
            }
            // 2.0.32: in proportion to the ball's own heart (a player's is lighter than the Reaper's).
            float damage = AgentRules.dawnBlastDamage(body == direct ? 0.0 : distance) * shot.damage / AgentRules.DAWN_SHOT_DAMAGE;
            Vec3 away = centre.m_82546_(at).m_82541_();
            WoundHits.expect(body, WoundBook.Blow.BLUNT, centre, away, 0.0);
            body.f_19802_ = 0;
            boolean hurt;
            striking = true;
            try {
                hurt = body.m_6469_(shot.shooter.m_269291_().m_269333_(shot.shooter), damage);
            } finally {
                striking = false;
            }
            if (hurt) {
                body.m_147240_(0.9 * damage / Math.max(1.0F, shot.damage), -away.f_82479_, -away.f_82481_);
            }
        }
        fx(shot, S2CUkakuFx.DAWN_BURST, at, shot.velocity, (float)radius, 0);
        UkakuFire.sound(shot.level, at, com.hex.kagune.control.ukaku.UkakuSounds.CRACK, 1.1F, 1.05F);
        UkakuFire.sound(shot.level, at, com.hex.kagune.control.ukaku.UkakuSounds.BIG_BURST, 0.9F, 1.1F);
    }

    private static void pierce(Shot shot, LivingEntity body) {
        Vec3 centre = GhoulNpcBrain.centre(body);
        Vec3 into = shot.velocity.m_82541_();
        WoundHits.expect(body, WoundBook.Blow.STAB, centre.m_82546_(into.m_82490_(0.3)), into, 0.0);
        body.f_19802_ = 0;
        striking = true;
        boolean hurt;
        try {
            hurt = body.m_6469_(shot.shooter.m_269291_().m_269333_(shot.shooter), shot.damage);
        } finally {
            striking = false;
        }
        if (hurt) {
            if (shot.velocity.m_165925_() > 1.0E-6) {
                body.m_147240_(0.08, -shot.velocity.f_82479_, -shot.velocity.f_82481_);
            }
            UkakuFire.sound(shot.level, centre, com.hex.kagune.control.ukaku.UkakuSounds.CRYSTAL_SHATTER, 0.5F, 1.5F);
        }
    }

    private static void fx(Shot shot, int kind, Vec3 at, Vec3 velocity, float size, int extra) {
        S2CUkakuFx message = new S2CUkakuFx(kind, shot.shooter.m_19879_(), shot.id, at.f_82479_, at.f_82480_, at.f_82481_,
            (float)velocity.f_82479_, (float)velocity.f_82480_, (float)velocity.f_82481_, size, extra);
        if (shot.lightning) {
            ControlNetwork.sendNear(shot.level, at, 80.0, message);
        } else {
            ControlNetwork.broadcastAround(shot.shooter, message);
        }
    }
}
