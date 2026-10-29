package com.hex.kagune.control.server;

import com.hex.kagune.control.ComboStage;
import com.hex.kagune.control.StrikePattern;
import com.hex.kagune.control.ukaku.UkakuRules;
import com.hex.kagune.control.ukaku.KakujaRig;
import com.hex.kagune.control.ukaku.WingForm;
import com.hex.kagune.control.ukaku.WingRig;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CUkakuFx;
import com.hex.kagune.control.network.S2CUkakuSpikes;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.effect.KaguneEffects;
import com.hex.kagune.gameplay.KaguneRuntime;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.network.KaguneNetwork;
import com.hex.kagune.sound.HexKaguneSounds;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Ukaku's crystal mode on the server (V crystallizes the wings): the crystal spikes of the M1 and the crystal enders,
 * resolved at their impact tick.
 *
 * <p>A spike leaves the tip of the aiming wing ({@link UkakuFire#tip}) and is simulated here like a fire bolt, only
 * faster and much further. Whatever living thing it hits takes the hit, starts to bleed (the original's own
 * {@code hexkagune:bleeding}, one stack per spike: Hex God of Time draws its blood and stains for it when installed), and
 * the spike stays stuck in it: kept in the target's own body frame, so it turns and moves with the body, and every client
 * is told ({@link S2CUkakuSpikes}). Fifteen seconds later ({@link UkakuRules#STUCK_TICKS}) the spike bursts: a smaller hit, another stack, crystal shards and a
 * spurt of blood. With the Bursting Shards mutation ({@link #burstingSpikes}) every burst also explodes.
 *
 * <p>The enders: Crystal Lance (one long spike through everything in its line), Spike Fan (a tight fan), Spike Rain (thrown
 * high, coming down on the target and around it), Spike Barrage (both wings machine-gunning where he looks) and the Crystal
 * Bloom (every spike he has stuck in anyone bursts at once, harder, and a ring of spikes flies out around him).
 */
final class UkakuCrystalCombat {
    /** What a spike is, for its look (sent to clients as {@link S2CUkakuFx#SPIKE} + ordinal) and what it does. */
    enum Kind {
        SHOT, LANCE, FAN, RAIN, BARRAGE, NEEDLE
    }

    /**
     * A kakuja needle waiting its turn: when it leaves, from where, toward what ({@code direction} when it flies a set way
     * instead), how hard, and how it flies (scatter, range, shove, how many it runs through, bleeding stacks, size).
     */
    static final class Pending {
        long at;
        Vec3 from;
        Vec3 aim;
        Vec3 direction;
        float damage;
        float spread = UkakuRules.NEEDLE_SPREAD;
        double range = UkakuRules.NEEDLE_RANGE;
        float knockback = 0.08F;
        int pierce;
        int stacks = 1;
        float size = 1.0F;
        float speed = 1.0F;
        /** False for the Needle Gatling's stream (it trains the kagune once a second, not per needle). */
        boolean trains = true;
        /** The final kakuja: seconds a needle wrapped in flame sets what it hits alight (0 for a plain needle). */
        int burnSeconds;
        /** The Pyre Lance: the ground burns where it ends. */
        boolean embers;
        double spreadA;
        double spreadB;
    }

    private static final class Spike {
        final int id = nextSpikeId++;
        /** The world it flies in (5.0.14: Rinkaku's guards only see the shots in theirs). */
        ServerLevel level;
        Vec3 position;
        Vec3 velocity;
        int life;
        float radius;
        float damage;
        int pierce;
        int stacks;
        Kind kind;
        /** How hard a hit shoves (the original's knockback strength). */
        float knockback = 0.12F;
        boolean landed;
        /** False for the Gatling's stream: its spikes don't train the kagune one by one (UkakuGatling does, once a second). */
        boolean trains = true;
        /** Seconds a burning needle sets what it hits alight (0: it doesn't burn), and whether the ground burns where it ends. */
        int burnSeconds;
        boolean embers;
        final Set<Integer> hit = new HashSet<>();
    }

    /**
     * A spike stuck in a body: where (from the body's feet, in its own frame: x to its right, y up, z forward) and which way it
     * went in, when it bursts and how hard. 5.0.14: {@code limb} is the Rinkaku tentacle it is stuck in when a guard stopped it
     * ({@link TendrilShieldServer}; its place is then where the tentacle was struck), -1 for the body itself.
     */
    private record Stuck(int id, UUID owner, int targetId, double ox, double oy, double oz, double dx, double dy, double dz, int kind, long stuckAt,
                         long burstAt, float burstDamage, int limb) {
    }

    /** One body's bleeding from the spikes: its stacks, until when it bleeds, and its last hit (stacks reset after a pause). */
    private static final class Bleed {
        int stacks;
        long until;
        long lastHit;
    }

    /** An ender still firing after its impact tick: the rain coming down, the barrage. */
    private static final class Art {
        StrikePattern pattern;
        long started;
        int duration;
        int targetId;
        Vec3 aimPoint;
        float damage;
        int fired;
        int spikes;
    }

    private static final Map<UUID, List<Spike>> SPIKES = new HashMap<>();
    /** Every stuck spike, by the id of the body it is in. */
    private static final Map<Integer, List<Stuck>> STUCK = new HashMap<>();
    private static final Map<UUID, Bleed> BLEEDS = new HashMap<>();
    private static final Map<UUID, Art> ARTS = new HashMap<>();
    /** Kakuja needles queued to leave over the next few ticks (a volley's needles go one after another). */
    private static final Map<UUID, List<Pending>> PENDING = new HashMap<>();
    /** Shooters whose Gatling stream has hit something since UkakuGatling last asked. */
    private static final Set<UUID> GATLING_LANDED = new HashSet<>();
    /**
     * Bodies whose spikes or bleeding changed, per world (their clients are told at the end of the tick of a shooter in that
     * world; a body is only ever looked up in its own world).
     */
    private static final Map<Object, Set<Integer>> DIRTY = new IdentityHashMap<>();
    /** Spike ids, apart from the fire's bolt ids (the clients fly both in one table). */
    private static int nextSpikeId = 0x40000000;
    /** Stacks reset when a body hasn't been hit for this long and its bleeding has stopped. */
    private static final long BLEED_RESET_TICKS = 200L;
    /** Stuck spikes and bleeding are told again this often, for clients that have come near since. */
    private static final long RESEND_TICKS = 20L;

    private UkakuCrystalCombat() {
    }

    /**
     * The impact tick of a crystal stage. {@code started} and {@code duration} are the stage's own timing (the enders that
     * keep firing read them).
     */
    static void resolve(ServerPlayer player, KaguneRuntime.State state, ComboStage stage, int limbMask, Vec3 direction, long started, int duration) {
        ServerLevel level = player.m_284548_();
        int mastery = KaguneProfile.mastery(player);
        WingForm form = UkakuServer.form(player);
        StrikePattern pattern = stage.pattern();
        double range = UkakuRules.crystalRange(mastery) + (pattern == StrikePattern.CRYSTAL_LANCE ? UkakuRules.LANCE_EXTRA_RANGE : 0.0);
        Vec3 aim = UkakuFire.aimPoint(player, state, direction, range);
        float damage = UkakuRules.meleeDamage(mastery) * stage.damageMul();
        float radius = UkakuRules.crystalRadius(mastery);
        float speed = UkakuRules.crystalSpeed(mastery);
        boolean right = (limbMask & 1) != 0 && form.rightWing();
        if (form.rightWing() && UkakuServer.wingGone(player, right ? 1 : -1, level.m_46467_())) {
            right = !right; // that wing was thrown as a Wing Vortex and isn't back yet: the other one fires
        }
        switch (pattern) {
            case CRYSTAL_SHOT -> {
                Vec3 tip = UkakuFire.tip(player, right ? 1 : -1, aim);
                launch(player, tip, aim.m_82546_(tip), speed, radius, damage, 0, 1, Kind.SHOT, range);
                UkakuFire.sound(level, tip, "block.amethyst_block.hit", 0.8F, KaguneServerController.vary(player, 1.6F, 0.1F));
            }
            case CRYSTAL_LANCE -> {
                Vec3 tip = form.rightWing() ? UkakuFire.tip(player, 1, aim).m_82549_(UkakuFire.tip(player, -1, aim)).m_82490_(0.5)
                    : UkakuFire.tip(player, -1, aim);
                launch(player, tip, aim.m_82546_(tip), speed * 1.25F, radius * 1.4F, damage, UkakuRules.lancePierce(mastery), 2, Kind.LANCE, range);
                UkakuFire.sound(level, tip, "block.amethyst_cluster.break", 1.0F, KaguneServerController.vary(player, 0.8F, 0.05F));
                UkakuFire.sound(level, tip, "block.amethyst_block.chime", 1.0F, KaguneServerController.vary(player, 0.7F, 0.05F));
            }
            case SPIKE_FAN -> {
                int spikes = UkakuRules.spikeFanCount(mastery);
                for (int index = 0; index < spikes; index++) {
                    double yaw = -UkakuRules.SPIKE_FAN_HALF_ANGLE + 2.0 * UkakuRules.SPIKE_FAN_HALF_ANGLE * index / (spikes - 1.0);
                    int side = form.rightWing() && index % 2 == 0 ? 1 : -1;
                    Vec3 tip = UkakuFire.tip(player, side, aim);
                    launch(player, tip, KaguneCombat.rotateYaw(aim.m_82546_(tip), (float)yaw), speed, radius, damage, 0, 1, Kind.FAN, range);
                }
                UkakuFire.sound(level, player.m_146892_(), "block.amethyst_cluster.break", 1.0F, KaguneServerController.vary(player, 1.2F, 0.05F));
            }
            case SPIKE_RAIN -> {
                Art art = art(player, pattern, started, duration, damage, aim, state.actionTargetId);
                art.spikes = UkakuRules.rainSpikes(mastery);
                // Thrown high out of both wings first: the clients see them go up, the rain comes down a few ticks later.
                for (int side = -1; side <= 1; side += 2) {
                    if (side > 0 && !form.rightWing()) {
                        continue;
                    }
                    Vec3 tip = UkakuFire.tip(player, side, player.m_146892_().m_82520_(0.0, 8.0, 0.0));
                    UkakuFire.fx(player, S2CUkakuFx.SPIKE + Kind.RAIN.ordinal(), nextSpikeId++, tip, new Vec3(side * 0.15, 1.6, 0.0), 0.8F, 5);
                }
                UkakuFire.sound(level, player.m_146892_(), "block.amethyst_cluster.place", 1.0F, KaguneServerController.vary(player, 0.9F, 0.05F));
            }
            case SPIKE_BARRAGE -> {
                Art art = art(player, pattern, started, duration, damage, aim, state.actionTargetId);
                art.spikes = UkakuRules.barrageSpikes(mastery);
            }
            case CRYSTAL_BLOOM -> bloom(player, damage, mastery, level.m_46467_(), false);
            case NEEDLE_VOLLEY -> volley(player, limbMask, aim, damage, level.m_46467_());
            case HARROW -> harrow(player, aim, damage, level.m_46467_());
            case PIERCING_LINE -> piercingLine(player, aim, damage, level.m_46467_());
            case THORN_FAN -> thornFan(player, aim, damage, level.m_46467_());
            case HAIL_OF_THORNS -> {
                Art art = art(player, pattern, started, duration, damage, aim, state.actionTargetId);
                art.spikes = UkakuRules.HAIL_NEEDLES;
                // Thrown high out of all six first: the clients see them go up, the hail comes down a few ticks later.
                for (int index = 0; index < KakujaRig.TENTACLES; index++) {
                    Vec3 from = muzzles(player, index, player.m_146892_().m_82520_(0.0, 10.0, 0.0), 1)[0];
                    UkakuFire.fx(player, S2CUkakuFx.NEEDLE, nextSpikeId++, from, new Vec3(KakujaRig.sideOf(index) * 0.2, 2.2, 0.0), 1.0F, 5);
                }
                UkakuFire.sound(level, player.m_146892_(), "block.amethyst_cluster.place", 1.0F, KaguneServerController.vary(player, 1.3F, 0.05F));
            }
            case SIX_TAIL_BARRAGE -> {
                Art art = art(player, pattern, started, duration, damage, aim, state.actionTargetId);
                art.spikes = UkakuRules.SIX_TAIL_NEEDLES;
            }
            case CRIMSON_BLOOM -> bloom(player, damage, mastery, level.m_46467_(), true);
            case RUSHING_DEATH -> {
                // At the top of the flip: all six snap forward and throw their needles at once.
                for (int index = 0; index < KakujaRig.TENTACLES; index++) {
                    for (Vec3 from : muzzles(player, index, aim, UkakuRules.RUSH_NEEDLES_PER_TENTACLE)) {
                        Pending pending = queue(player, level.m_46467_(), from, aim, damage);
                        pending.spread = 2.5F;
                        pending.knockback = 0.3F;
                    }
                }
                UkakuFire.sound(level, player.m_146892_(), "block.amethyst_cluster.break", 1.2F, KaguneServerController.vary(player, 0.9F, 0.05F));
                UkakuFire.sound(level, player.m_146892_(), "entity.player.attack.sweep", 0.8F, KaguneServerController.vary(player, 0.8F, 0.05F));
            }
            default -> {
            }
        }
    }

    /**
     * A kakuja volley: each tentacle in {@code limbMask} (bits 20..25) sends {@link UkakuRules#VOLLEY_NEEDLES} needles from
     * the points of the spikes along its belly, one after another, at {@code aim} (the clients see each one leave its spike).
     */
    private static void volley(ServerPlayer player, int limbMask, Vec3 aim, float damage, long now) {
        int fired = 0;
        for (int index = 0; index < KakujaRig.TENTACLES; index++) {
            if ((limbMask & 1 << (20 + index)) == 0) {
                continue;
            }
            Vec3[] muzzles = muzzles(player, index, aim, UkakuRules.VOLLEY_NEEDLES);
            for (int k = 0; k < muzzles.length; k++) {
                queue(player, now + (long)k * UkakuRules.NEEDLE_STAGGER, muzzles[k], aim, damage);
                fired++;
            }
        }
        if (fired > 0) {
            UkakuFire.sound(player.m_284548_(), player.m_146892_(), "block.amethyst_block.hit", 0.5F,
                KaguneServerController.vary(player, 1.8F, 0.1F));
        }
    }

    /** A needle to leave at {@code at} (now or a few ticks on) toward {@code aim}; returns it to set how it flies. */
    static Pending queue(ServerPlayer player, long at, Vec3 from, Vec3 aim, float damage) {
        Pending pending = new Pending();
        pending.at = at;
        pending.from = from;
        pending.aim = aim;
        pending.damage = damage;
        pending.spreadA = player.m_217043_().m_188500_();
        pending.spreadB = player.m_217043_().m_188500_();
        PENDING.computeIfAbsent(player.m_20148_(), ignored -> new ArrayList<>()).add(pending);
        return pending;
    }

    /**
     * Where needles leave tentacle {@code index} of this player's kakuja, turned to fire at {@code aim}: {@code count} spike
     * points along its belly (KakujaRig), in the world, kept out of walls.
     */
    static Vec3[] muzzles(ServerPlayer player, int index, Vec3 aim, int count) {
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
        double[][] local = KakujaRig.muzzles(index, dir, player.m_6047_(), count);
        Vec3[] out = new Vec3[local.length];
        for (int i = 0; i < local.length; i++) {
            Vec3 point = new Vec3(player.m_20185_() + rightX * local[i][0] + forwardX * local[i][2], player.m_20186_() + local[i][1],
                player.m_20189_() + rightZ * local[i][0] + forwardZ * local[i][2]);
            out[i] = UkakuVortex.clearOfBlocks(player, player.m_146892_(), point);
        }
        return out;
    }

    /** Harrow: all six blast a cone of needles point-blank, every one shoving hard. */
    private static void harrow(ServerPlayer player, Vec3 aim, float damage, long now) {
        Vec3 eye = player.m_146892_();
        Vec3 ahead = aim.m_82546_(eye).m_82541_();
        for (int index = 0; index < KakujaRig.TENTACLES; index++) {
            for (Vec3 from : muzzles(player, index, aim, UkakuRules.VOLLEY_NEEDLES)) {
                Pending pending = queue(player, now, from, aim, damage);
                pending.direction = ahead;
                pending.spread = UkakuRules.HARROW_CONE;
                pending.range = UkakuRules.HARROW_RANGE;
                pending.knockback = UkakuRules.HARROW_KNOCKBACK;
            }
        }
        UkakuFire.sound(player.m_284548_(), eye, "block.amethyst_cluster.break", 1.1F, KaguneServerController.vary(player, 1.1F, 0.05F));
        UkakuFire.sound(player.m_284548_(), eye, "entity.player.attack.sweep", 0.7F, KaguneServerController.vary(player, 1.3F, 0.05F));
    }

    /** Piercing Line: the six converge in front of his chest and fire one thick needle through everything in its line. */
    private static void piercingLine(ServerPlayer player, Vec3 aim, float damage, long now) {
        Vec3 eye = player.m_146892_();
        Vec3 ahead = aim.m_82546_(eye).m_82541_();
        Vec3 from = UkakuVortex.clearOfBlocks(player, eye, eye.m_82520_(0.0, -0.3, 0.0).m_82549_(ahead.m_82490_(1.2)));
        Pending pending = queue(player, now, from, aim, damage);
        pending.spread = 0.0F;
        pending.size = UkakuRules.LINE_SIZE;
        pending.pierce = UkakuRules.LINE_PIERCE;
        pending.stacks = 2;
        pending.knockback = 0.5F;
        pending.speed = 1.15F;
        pending.range = UkakuRules.NEEDLE_RANGE + UkakuRules.LINE_EXTRA_RANGE;
        UkakuFire.sound(player.m_284548_(), from, "block.amethyst_cluster.break", 1.2F, KaguneServerController.vary(player, 0.7F, 0.05F));
        UkakuFire.sound(player.m_284548_(), from, "block.amethyst_block.chime", 1.2F, KaguneServerController.vary(player, 0.6F, 0.05F));
    }

    /** Thorn Fan: needles swept across the front, the right tentacles covering the right half and the left ones the left. */
    private static void thornFan(ServerPlayer player, Vec3 aim, float damage, long now) {
        Vec3 eye = player.m_146892_();
        Vec3 ahead = aim.m_82546_(eye).m_82541_();
        int count = UkakuRules.FAN_NEEDLES;
        for (int k = 0; k < count; k++) {
            // Swept from his left across to his right over a few ticks.
            double t = k / (count - 1.0);
            float yaw = (float)(-UkakuRules.THORN_FAN_HALF_ANGLE + 2.0 * UkakuRules.THORN_FAN_HALF_ANGLE * t);
            int side = yaw >= 0.0F ? 1 : -1;
            int index = KakujaRig.index(side, k % 3);
            Vec3 from = muzzles(player, index, aim, 3)[k % 3];
            Pending pending = queue(player, now + (long)Math.floor(t * UkakuRules.FAN_SWEEP_TICKS), from, aim, damage);
            pending.direction = KaguneCombat.rotateYaw(ahead, yaw);
            pending.spread = 1.0F;
        }
        UkakuFire.sound(player.m_284548_(), eye, "block.amethyst_cluster.break", 1.0F, KaguneServerController.vary(player, 1.3F, 0.05F));
    }

    /** Sends the needles whose turn has come. */
    private static void tickPending(ServerPlayer player, long now) {
        List<Pending> queue = PENDING.get(player.m_20148_());
        if (queue == null || queue.isEmpty()) {
            return;
        }
        int mastery = KaguneProfile.mastery(player);
        for (Iterator<Pending> iterator = queue.iterator(); iterator.hasNext(); ) {
            Pending pending = iterator.next();
            if (now < pending.at) {
                continue;
            }
            iterator.remove();
            Vec3 line = pending.direction != null ? pending.direction : pending.aim.m_82546_(pending.from);
            double[] spread = UkakuRules.spreadDirection(line.f_82479_, line.f_82480_, line.f_82481_, pending.spread, pending.spreadA, pending.spreadB);
            Spike spike = launch(player, pending.from, new Vec3(spread[0], spread[1], spread[2]), UkakuRules.needleSpeed(mastery) * pending.speed,
                UkakuRules.NEEDLE_RADIUS * (pending.size > 1.5F ? 1.8F : 1.0F), pending.damage, pending.pierce, pending.stacks, Kind.NEEDLE, pending.range,
                pending.size, pending.burnSeconds > 0);
            spike.knockback = pending.knockback;
            spike.trains = pending.trains;
            spike.burnSeconds = pending.burnSeconds;
            spike.embers = pending.embers;
        }
    }

    /** Moves the spikes, runs the enders that keep firing, bursts the stuck spikes that are due. Every server tick for Ukaku players. */
    static void tick(ServerPlayer player, long now) {
        UUID id = player.m_20148_();
        tickPending(player, now);
        List<Spike> spikes = SPIKES.get(id);
        if (spikes != null) {
            for (Iterator<Spike> iterator = spikes.iterator(); iterator.hasNext(); ) {
                if (!step(player, iterator.next(), now)) {
                    iterator.remove();
                }
            }
            if (spikes.isEmpty()) {
                SPIKES.remove(id);
            }
        }
        Art art = ARTS.get(id);
        if (art != null && !runArt(player, art, now)) {
            ARTS.remove(id);
        }
        tickStuck(player, now);
        if (now % 100L == 0L) {
            BLEEDS.values().removeIf(bleed -> now - bleed.lastHit > BLEED_RESET_TICKS && now >= bleed.until);
        }
        flush(player, now);
    }

    /** The shooter left: his spikes in flight vanish, his stuck spikes fall out. */
    static void forget(ServerPlayer player) {
        UUID id = player.m_20148_();
        SPIKES.remove(id);
        ARTS.remove(id);
        PENDING.remove(id);
        GATLING_LANDED.remove(id);
        for (Map.Entry<Integer, List<Stuck>> entry : STUCK.entrySet()) {
            if (entry.getValue().removeIf(stuck -> stuck.owner().equals(id))) {
                dirty(player.m_284548_(), entry.getKey());
            }
        }
        STUCK.values().removeIf(List::isEmpty);
        flush(player, player.m_9236_().m_46467_());
    }

    /** The crystal mode went on or off: the wings freeze (a ringing chime) or thaw back into fire. */
    static void modeChanged(ServerPlayer player, boolean on) {
        ServerLevel level = player.m_284548_();
        Vec3 back = player.m_20182_().m_82520_(0.0, 1.3, 0.0);
        if (on) {
            UkakuFire.sound(level, back, "block.amethyst_block.chime", 1.2F, KaguneServerController.vary(player, 0.6F, 0.04F));
            UkakuFire.sound(level, back, "block.amethyst_cluster.place", 1.0F, KaguneServerController.vary(player, 0.75F, 0.04F));
        } else {
            UkakuFire.sound(level, back, "block.amethyst_cluster.break", 0.8F, KaguneServerController.vary(player, 0.7F, 0.04F));
            UkakuFire.sound(level, back, "item.firecharge.use", 0.6F, KaguneServerController.vary(player, 1.2F, 0.05F));
        }
        UkakuFire.fx(player, S2CUkakuFx.CRYSTALLIZE, 0, back, Vec3.f_82478_, 1.0F, on ? 1 : 0);
    }

    /** The Bursting Shards mutation: every stuck spike or needle that bursts also explodes for extra damage around it. */
    static boolean burstingSpikes(ServerPlayer player) {
        return UkakuServer.hasMutation(player, com.hex.kagune.control.ukaku.UkakuMutation.BURSTING_SHARDS);
    }

    // ------------------------------------------------------------------ spikes in flight

    /**
     * One spike of the Gatling's stream: a light barrage spike that sticks and bleeds like any other but doesn't train the
     * kagune by itself. Returns false if too many of his spikes are already in flight.
     */
    static boolean gatlingSpike(ServerPlayer player, Vec3 from, Vec3 direction, float damage, int mostInFlight) {
        List<Spike> flying = SPIKES.get(player.m_20148_());
        if (flying != null && flying.size() >= mostInFlight) {
            return false;
        }
        int mastery = KaguneProfile.mastery(player);
        Spike spike = launch(player, from, direction, UkakuRules.crystalSpeed(mastery) * 1.1F, UkakuRules.crystalRadius(mastery), damage, 0, 1, Kind.BARRAGE,
            UkakuRules.crystalRange(mastery));
        spike.trains = false;
        return true;
    }

    /**
     * One salvo of the kakuja's Needle Gatling: tentacle {@code index} fires its needles at once at {@code aim} (from its
     * spike points). False if too many of this shooter's needles are already in flight.
     */
    static boolean needleSalvo(ServerPlayer player, int index, Vec3 aim, float damage, int needles, int mostInFlight) {
        List<Spike> flying = SPIKES.get(player.m_20148_());
        if (flying != null && flying.size() >= mostInFlight) {
            return false;
        }
        long now = player.m_9236_().m_46467_();
        for (Vec3 from : muzzles(player, index, aim, needles)) {
            Pending pending = queue(player, now, from, aim, damage);
            pending.spread = UkakuRules.GATLING_SPREAD;
            pending.trains = false;
        }
        return true;
    }

    /** True (once) if a Gatling spike of this shooter has landed since the last ask. */
    static boolean gatlingLanded(ServerPlayer player) {
        return GATLING_LANDED.remove(player.m_20148_());
    }

    private static Spike launch(ServerPlayer player, Vec3 from, Vec3 direction, float speed, float radius, float damage, int pierce, int stacks,
                               Kind kind, double range) {
        return launch(player, from, direction, speed, radius, damage, pierce, stacks, kind, range, 0.0F);
    }

    /** {@link #launch}, drawn at {@code drawnSize} (0 for its kind's own). */
    private static Spike launch(ServerPlayer player, Vec3 from, Vec3 direction, float speed, float radius, float damage, int pierce, int stacks,
                               Kind kind, double range, float drawnSize) {
        return launch(player, from, direction, speed, radius, damage, pierce, stacks, kind, range, drawnSize, false);
    }

    /** {@link #launch}; a needle that is {@code burning} is drawn wrapped in flame (the caller sets how long it burns). */
    private static Spike launch(ServerPlayer player, Vec3 from, Vec3 direction, float speed, float radius, float damage, int pierce, int stacks,
                               Kind kind, double range, float drawnSize, boolean burning) {
        Vec3 heading = direction.m_82556_() < 1.0E-6 ? player.m_20154_() : direction.m_82541_();
        Spike spike = new Spike();
        spike.level = player.m_284548_();
        spike.position = from;
        spike.velocity = heading.m_82490_(speed);
        spike.life = (int)Math.ceil(range / speed);
        spike.radius = radius;
        spike.damage = damage;
        spike.pierce = pierce;
        spike.stacks = stacks;
        spike.kind = kind;
        SPIKES.computeIfAbsent(player.m_20148_(), ignored -> new ArrayList<>()).add(spike);
        float size = switch (kind) {
            case LANCE -> 2.2F;
            case BARRAGE -> 0.85F;
            case NEEDLE -> 1.0F;
            default -> 1.0F;
        } * (kind != Kind.NEEDLE && KaguneProfile.mastery(player) >= 500 ? 1.15F : 1.0F);
        if (drawnSize > 0.0F) {
            size = drawnSize;
        }
        spike.knockback = kind == Kind.LANCE ? 0.45F : kind == Kind.NEEDLE ? 0.08F : 0.12F;
        int fx = kind != Kind.NEEDLE ? S2CUkakuFx.SPIKE + kind.ordinal() : burning ? S2CUkakuFx.FLAME_NEEDLE : S2CUkakuFx.NEEDLE;
        UkakuFire.fx(player, fx, spike.id, from, spike.velocity, size, spike.life);
        return spike;
    }

    /** One tick of flight. Returns false once the spike is spent. */
    private static boolean step(ServerPlayer player, Spike spike, long now) {
        ServerLevel level = player.m_284548_();
        Vec3 from = spike.position;
        Vec3 to = from.m_82549_(spike.velocity);
        BlockHitResult block = level.m_45547_(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        boolean wall = block.m_6662_() == HitResult.Type.BLOCK;
        Vec3 end = wall ? block.m_82450_() : to;
        // 1.0.42: a kakuja needle (RC, not crystal) is met by another's kagune M1 on its way, as the fire's shards are (KaguneClash):
        // it breaks there. The crystal spikes can't be.
        Vec3 met = spike.kind == Kind.NEEDLE ? KaguneClash.swat(player, level, from, end, now) : null;
        if (met != null) {
            end = met;
            wall = false;
        }

        Vec3 middle = from.m_82549_(end).m_82490_(0.5);
        Vec3 shift = middle.m_82546_(player.m_20191_().m_82399_());
        AABB search = player.m_20191_().m_82386_(shift.f_82479_, shift.f_82480_, shift.f_82481_).m_82400_(from.m_82554_(end) * 0.5 + spike.radius + 1.0);
        List<LivingEntity> candidates = level.m_6443_(LivingEntity.class, search,
            entity -> entity != player && entity.m_6084_() && !spike.hit.contains(entity.m_19879_())
                && KaguneServerController.validSwipeTarget(player, entity));
        int samples = Math.max(1, (int)Math.ceil(from.m_82554_(end) / 0.25));
        for (int sample = 0; sample <= samples; sample++) {
            Vec3 point = from.m_82549_(end.m_82546_(from).m_82490_(sample / (double)samples));
            for (LivingEntity target : candidates) {
                if (spike.hit.contains(target.m_19879_()) || !target.m_20191_().m_82400_(spike.radius).m_82390_(point)) {
                    continue;
                }
                spike.hit.add(target.m_19879_());
                // 5.0.14: a Rinkaku tentacle guarding him takes the spike; it shatters on the tentacle (no wound, nothing stuck), or
                // what the guard couldn't hold goes on into him.
                TendrilShieldServer.Guarded guarded = TendrilShieldServer.guard(target, from, spike.velocity, point, spike.damage, now);
                if (guarded != null && guarded.through() <= 0.0F) {
                    // It goes into the tentacle and stays there, like in a body: it bursts there in time (Bursting Shards' blast
                    // catches him beside it), or falls out when the tentacle goes back in.
                    stickInTentacle(player, spike, target, guarded, now);
                    return false;
                }
                if (guarded != null) {
                    float full = spike.damage;
                    spike.damage = guarded.through();
                    strike(player, spike, target, point, now);
                    spike.damage = full;
                } else {
                    strike(player, spike, target, point, now);
                }
                if (spike.pierce-- <= 0) {
                    UkakuFire.fx(player, S2CUkakuFx.SPIKE_IMPACT, spike.id, point, spike.velocity, 1.0F, spike.kind.ordinal());
                    ended(player, spike, point, now);
                    return false;
                }
            }
        }
        if (met != null) {
            UkakuFire.fx(player, S2CUkakuFx.SPIKE_IMPACT, spike.id, met, spike.velocity, 0.0F, spike.kind.ordinal());
            return false;
        }
        if (wall) {
            UkakuFire.fx(player, S2CUkakuFx.SPIKE_IMPACT, spike.id, end, spike.velocity, 0.0F, spike.kind.ordinal());
            UkakuFire.sound(level, end, "block.amethyst_block.step", 0.6F, KaguneServerController.vary(player, 1.4F, 0.1F));
            ended(player, spike, end, now);
            return false;
        }
        spike.position = to;
        if (--spike.life <= 0) {
            UkakuFire.fx(player, S2CUkakuFx.SPIKE_FIZZLE, spike.id, to, spike.velocity, 1.0F, spike.kind.ordinal());
            ended(player, spike, to, now);
            return false;
        }
        return true;
    }

    /** 5.0.14: every spike and needle in flight, for Rinkaku's guards to see coming ({@link TendrilShieldServer}). */
    static void shots(TendrilShieldServer.Shots visitor) {
        for (Map.Entry<UUID, List<Spike>> entry : SPIKES.entrySet()) {
            for (Spike spike : entry.getValue()) {
                visitor.shot(entry.getKey(), spike.level, spike.position, spike.velocity, spike.radius);
            }
        }
    }

    /** Where a spike stops (a body, a wall, or out of range): the Pyre Lance leaves the ground burning there. */
    private static void ended(ServerPlayer player, Spike spike, Vec3 at, long now) {
        if (spike.embers) {
            UkakuFire.embers(player, at, now, UkakuRules.EMBER_TICKS);
        }
    }

    /** A spike hits {@code target} at {@code point}: the hit, the bleeding, and the spike stays in. */
    private static void strike(ServerPlayer player, Spike spike, LivingEntity target, Vec3 point, long now) {
        target.f_19802_ = 0;
        Vec3 into = spike.velocity.m_82556_() > 1.0E-6 ? spike.velocity.m_82541_() : KaguneServerController.center(target).m_82546_(point);
        WoundHits.expect(target, com.hex.kagune.control.wound.WoundBook.Blow.STAB, point, into, 0.0);
        if (!target.m_6469_(player.m_269291_().m_269075_(player), spike.damage)) {
            return;
        }
        Vec3 push = spike.velocity;
        if (push.m_165925_() > 1.0E-6 && spike.knockback > 0.0F) {
            target.m_147240_(spike.knockback, -push.f_82479_, -push.f_82481_);
        }
        if (spike.burnSeconds > 0) {
            // Wrapped in flame: alight first, so the wound it opens is already a scorched one.
            target.m_20254_(spike.burnSeconds);
        }
        bleed(target, spike.stacks, player, now);
        float power = spike.kind == Kind.LANCE ? 1.6F : spike.kind == Kind.BARRAGE ? 0.6F : 1.0F;
        UkakuFire.fx(player, S2CUkakuFx.BLOOD, target.m_19879_(), point, push.m_82541_(), power, 0);
        KaguneServerController.playAt(player, target, HexKaguneSounds.FLESH_IMPACT, 0.7F, KaguneServerController.vary(player, 1.15F, 0.08F));
        UkakuFire.sound(player.m_284548_(), point, "block.amethyst_cluster.break", 0.55F, KaguneServerController.vary(player, 1.5F, 0.1F));
        if (target.m_6084_()) {
            embed(player, spike, target, point, now);
        }
        ControlServer.onKaguneHit(player, 1);
        if (!spike.trains) {
            GATLING_LANDED.add(player.m_20148_());
        } else if (!spike.landed) {
            spike.landed = true;
            KaguneProfile.addMastery(player, "Swipe", 1);
            KaguneNetwork.broadcast(player);
        }
    }

    /** The spike stays in the body: kept in the body's own frame so it turns and moves with it. */
    private static void embed(ServerPlayer player, Spike spike, LivingEntity target, Vec3 point, long now) {
        UUID owner = player.m_20148_();
        List<Stuck> inTarget = STUCK.computeIfAbsent(target.m_19879_(), ignored -> new ArrayList<>());
        int mine = 0;
        for (Stuck stuck : inTarget) {
            mine += stuck.owner().equals(owner) ? 1 : 0;
        }
        if (mine >= UkakuRules.MAX_STUCK_PER_TARGET || countStuck(owner) >= UkakuRules.MAX_STUCK) {
            if (inTarget.isEmpty()) {
                STUCK.remove(target.m_19879_());
            }
            return;
        }
        // The entry wound: the hit point pulled just inside its box, a touch toward its middle (the box is wider than the
        // model), so most of the spike stands out of the body where it went in.
        AABB box = target.m_20191_();
        double x = clamp(point.f_82479_, box.f_82288_ + 0.04, box.f_82291_ - 0.04);
        double y = clamp(point.f_82480_, box.f_82289_ + 0.1, box.f_82292_ - 0.05);
        double z = clamp(point.f_82481_, box.f_82290_ + 0.04, box.f_82293_ - 0.04);
        Vec3 centre = box.m_82399_();
        x += (centre.f_82479_ - x) * 0.12;
        z += (centre.f_82481_ - z) * 0.12;
        Vec3 feet = target.m_20182_();
        double[] local = toBody(target, x - feet.f_82479_, y - feet.f_82480_, z - feet.f_82481_);
        Vec3 heading = spike.velocity.m_82541_();
        double[] dir = toBody(target, heading.f_82479_, heading.f_82480_, heading.f_82481_);
        int mastery = KaguneProfile.mastery(player);
        inTarget.add(new Stuck(spike.id, owner, target.m_19879_(), local[0], local[1], local[2], dir[0], dir[1], dir[2], spike.kind.ordinal(), now,
            now + UkakuRules.burstDelay(mastery), UkakuRules.burstDamage(spike.damage), -1));
        dirty(player.m_284548_(), target.m_19879_());
    }

    /**
     * 5.0.14: a spike a Rinkaku guard stopped stays stuck in the tentacle (where it struck it, kept in his body's frame for the
     * server; every client puts it on the tentacle itself). It counts toward the same limits as spikes in bodies.
     */
    private static void stickInTentacle(ServerPlayer player, Spike spike, LivingEntity target, TendrilShieldServer.Guarded guarded, long now) {
        UUID owner = player.m_20148_();
        List<Stuck> inTarget = STUCK.computeIfAbsent(target.m_19879_(), ignored -> new ArrayList<>());
        int mine = 0;
        for (Stuck stuck : inTarget) {
            mine += stuck.owner().equals(owner) ? 1 : 0;
        }
        if (mine >= UkakuRules.MAX_STUCK_PER_TARGET || countStuck(owner) >= UkakuRules.MAX_STUCK) {
            if (inTarget.isEmpty()) {
                STUCK.remove(target.m_19879_());
            }
            UkakuFire.fx(player, S2CUkakuFx.SPIKE_IMPACT, spike.id, guarded.at(), spike.velocity, 0.0F, spike.kind.ordinal());
            return;
        }
        Vec3 feet = target.m_20182_();
        Vec3 at = guarded.at();
        double[] local = toBody(target, at.f_82479_ - feet.f_82479_, at.f_82480_ - feet.f_82480_, at.f_82481_ - feet.f_82481_);
        Vec3 heading = spike.velocity.m_82541_();
        double[] dir = toBody(target, heading.f_82479_, heading.f_82480_, heading.f_82481_);
        int mastery = KaguneProfile.mastery(player);
        inTarget.add(new Stuck(spike.id, owner, target.m_19879_(), local[0], local[1], local[2], dir[0], dir[1], dir[2], spike.kind.ordinal(), now,
            now + UkakuRules.burstDelay(mastery), UkakuRules.burstDamage(spike.damage), guarded.limb()));
        UkakuFire.sound(player.m_284548_(), at, "block.amethyst_cluster.place", 0.55F, KaguneServerController.vary(player, 1.5F, 0.1F));
        dirty(player.m_284548_(), target.m_19879_());
    }

    private static int countStuck(UUID owner) {
        int count = 0;
        for (List<Stuck> list : STUCK.values()) {
            for (Stuck stuck : list) {
                count += stuck.owner().equals(owner) ? 1 : 0;
            }
        }
        return count;
    }

    // ------------------------------------------------------------------ stuck spikes

    /** The shooter's stuck spikes: those in bodies that are gone fall out, those that are due burst. */
    private static void tickStuck(ServerPlayer player, long now) {
        if (STUCK.isEmpty()) {
            return;
        }
        UUID owner = player.m_20148_();
        ServerLevel level = player.m_284548_();
        for (Iterator<Map.Entry<Integer, List<Stuck>>> entries = STUCK.entrySet().iterator(); entries.hasNext(); ) {
            Map.Entry<Integer, List<Stuck>> entry = entries.next();
            Entity found = level.m_6815_(entry.getKey());
            LivingEntity target = found instanceof LivingEntity living && living.m_6084_() ? living : null;
            List<Stuck> list = entry.getValue();
            List<Stuck> due = new ArrayList<>();
            for (Iterator<Stuck> iterator = list.iterator(); iterator.hasNext(); ) {
                Stuck stuck = iterator.next();
                if (!stuck.owner().equals(owner)) {
                    continue;
                }
                if (target == null || stuck.limb() >= 0 && !TendrilShieldServer.holds(target, stuck.limb())) {
                    // The body is gone, or the tentacle it was stuck in went back in: it falls out.
                    iterator.remove();
                    dirty(level, entry.getKey());
                } else if (now >= stuck.burstAt()) {
                    iterator.remove();
                    due.add(stuck);
                }
            }
            if (list.isEmpty()) {
                entries.remove();
            }
            for (Stuck stuck : due) {
                burst(player, stuck, target, 1.0F, false, now);
            }
        }
    }

    /**
     * A stuck spike bursts out of {@code target}: a smaller hit, another stack, shards and blood (and with the mutation, a blast).
     * {@code fire} (the Crimson Pyre): it bursts into flame, the body goes up and the fire blasts round the wound.
     */
    private static void burst(ServerPlayer player, Stuck stuck, LivingEntity target, float multiplier, boolean fire, long now) {
        ServerLevel level = player.m_284548_();
        dirty(level, stuck.targetId());
        double[] offset = fromBody(target, stuck.ox(), stuck.oy(), stuck.oz());
        double[] dir = fromBody(target, stuck.dx(), stuck.dy(), stuck.dz());
        Vec3 at = target.m_20182_().m_82520_(offset[0], offset[1], offset[2]);
        Vec3 out = new Vec3(-dir[0], -dir[1], -dir[2]);
        float damage = stuck.burstDamage() * multiplier;
        if (stuck.limb() >= 0) {
            // 5.0.14: stuck in a Rinkaku tentacle: it bursts out of the tentacle (its guard takes it, not his body), and with
            // Bursting Shards the blast right beside him catches him in full, as it does anyone near.
            TendrilShieldServer.wound(target, stuck.limb(), damage, at, now);
            if (fire) {
                UkakuFire.fireBlast(player, null, at, damage * UkakuRules.PYRE_BLAST_SHARE, UkakuRules.PYRE_BLAST_RADIUS);
            }
            UkakuFire.fx(player, S2CUkakuFx.SPIKE_BURST, stuck.id(), at, out, multiplier, stuck.kind());
            UkakuFire.sound(level, at, "block.amethyst_block.break", 0.9F, KaguneServerController.vary(player, 1.3F, 0.1F));
            if (burstingSpikes(player)) {
                blast(player, null, at, damage * UkakuRules.BURST_EXPLOSION_SHARE);
            }
            return;
        }
        target.f_19802_ = 0;
        // Bursting out where it sat: the flesh round it torn out.
        WoundHits.expect(target, com.hex.kagune.control.wound.WoundBook.Blow.BURST, at, new Vec3(dir[0], dir[1], dir[2]), 0.0);
        if (target.m_6469_(player.m_269291_().m_269075_(player), damage)) {
            if (fire) {
                target.m_20254_(UkakuRules.PYRE_BURN_SECONDS);
            }
            bleed(target, 1, player, now);
            ControlServer.onKaguneHit(player, 1);
        }
        if (fire) {
            UkakuFire.fireBlast(player, target, at, damage * UkakuRules.PYRE_BLAST_SHARE, UkakuRules.PYRE_BLAST_RADIUS);
        }
        UkakuFire.fx(player, S2CUkakuFx.SPIKE_BURST, stuck.id(), at, out, multiplier, stuck.kind());
        UkakuFire.fx(player, S2CUkakuFx.BLOOD, target.m_19879_(), at, out, 1.3F * multiplier, 1);
        UkakuFire.sound(level, at, "block.amethyst_block.break", 0.9F, KaguneServerController.vary(player, 1.3F, 0.1F));
        UkakuFire.sound(level, at, "block.glass.break", 0.45F, KaguneServerController.vary(player, 1.5F, 0.1F));
        if (burstingSpikes(player)) {
            blast(player, target, at, damage * UkakuRules.BURST_EXPLOSION_SHARE);
        }
    }

    /**
     * The Bursting Shards mutation's blast: everyone else near the burst is caught in it too ({@code source}, the body it burst
     * out of, at half; null when it burst out of a Rinkaku tentacle, so he takes it in full).
     */
    private static void blast(ServerPlayer player, LivingEntity source, Vec3 at, float damage) {
        double radius = UkakuRules.BURST_EXPLOSION_RADIUS;
        Vec3 shift = at.m_82546_(player.m_20191_().m_82399_());
        AABB area = player.m_20191_().m_82386_(shift.f_82479_, shift.f_82480_, shift.f_82481_).m_82400_(radius);
        for (LivingEntity target : player.m_284548_().m_6443_(LivingEntity.class, area,
            entity -> entity != player && entity.m_6084_() && KaguneServerController.validSwipeTarget(player, entity))) {
            if (KaguneServerController.center(target).m_82557_(at) <= (radius + 0.6) * (radius + 0.6)) {
                target.f_19802_ = 0;
                Vec3 centre = KaguneServerController.center(target);
                Vec3 into = centre.m_82557_(at) < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : centre.m_82546_(at).m_82541_();
                WoundHits.expect(target, com.hex.kagune.control.wound.WoundBook.Blow.BLAST, centre.m_82546_(into.m_82490_(0.3)), into, 0.0);
                target.m_6469_(player.m_269291_().m_269075_(player), target == source ? damage * 0.5F : damage);
            }
        }
        UkakuFire.fx(player, S2CUkakuFx.BURST_BLAST, 0, at, Vec3.f_82478_, (float)radius, 0);
        UkakuFire.sound(player.m_284548_(), at, "entity.generic.explode", 0.6F, KaguneServerController.vary(player, 1.5F, 0.1F));
    }

    // ------------------------------------------------------------------ bleeding

    /**
     * {@code stacks} more stacks of bleeding on {@code target}: the original's {@code hexkagune:bleeding}, heavier every
     * second stack and renewed by each spike (credited to the shooter).
     */
    private static void bleed(LivingEntity target, int stacks, ServerPlayer player, long now) {
        // Scorched wounds (the final kakuja): a body on fire bleeds twice as hard.
        stacks = UkakuRules.scorchedStacks(stacks, target.m_6060_(), UkakuServer.form(player).finalKakuja());
        MobEffect effect = KaguneEffects.BLEEDING.get();
        Bleed bleed = BLEEDS.computeIfAbsent(target.m_20148_(), ignored -> new Bleed());
        if (!target.m_21023_(effect) && now - bleed.lastHit > BLEED_RESET_TICKS || now >= bleed.until + BLEED_RESET_TICKS) {
            bleed.stacks = 0;
        }
        bleed.stacks = Math.min(UkakuRules.MAX_BLEED_STACKS, bleed.stacks + stacks);
        bleed.lastHit = now;
        int duration = UkakuRules.bleedDuration(bleed.stacks);
        bleed.until = Math.max(bleed.until, now + duration);
        target.m_147207_(new MobEffectInstance(effect, duration, UkakuRules.bleedAmplifier(bleed.stacks), false, false, true), player);
        dirty(target.m_9236_(), target.m_19879_());
    }

    /**
     * 3.1.0: a hit from Rinkaku's centipede legs bleeds {@code target} exactly as a spike does (one stack), and the clients near it
     * are told at once (a Rinkaku has no Ukaku tick to flush it; {@link RinkakuBleed}).
     */
    static void rinkakuBleed(LivingEntity target, ServerPlayer player, long now) {
        bleed(target, 1, player, now);
        if (now % 100L == 0L) {
            BLEEDS.values().removeIf(bleed -> now - bleed.lastHit > BLEED_RESET_TICKS && now >= bleed.until);
        }
        flush(player, now);
    }

    /**
     * 7.0.2: a cut of Graft DNA's, {@code stacks} by its weight ({@code GraftRules.bleedStacks}): the same bleeding, refreshed
     * and never stacked (the user's rework): as hard as the harder of what it had and this cut, for at least this cut's length from
     * now, never longer than either alone ({@code GraftRules.refreshBleed}).
     */
    static void graftBleed(LivingEntity target, ServerPlayer player, int stacks, long now) {
        if (target == null || !target.m_6084_()) {
            return;
        }
        MobEffect effect = KaguneEffects.BLEEDING.get();
        Bleed bleed = BLEEDS.computeIfAbsent(target.m_20148_(), ignored -> new Bleed());
        boolean running = target.m_21023_(effect) && now < bleed.until;
        long[] after = com.hex.kagune.control.graft.GraftRules.refreshBleed(bleed.stacks, bleed.until, running, now, stacks);
        bleed.stacks = (int)Math.min(UkakuRules.MAX_BLEED_STACKS, after[0]);
        bleed.until = after[1];
        bleed.lastHit = now;
        target.m_147207_(new MobEffectInstance(effect, (int)Math.max(1L, bleed.until - now), UkakuRules.bleedAmplifier(bleed.stacks), false, false, true),
            player);
        dirty(target.m_9236_(), target.m_19879_());
        if (now % 100L == 0L) {
            BLEEDS.values().removeIf(old -> now - old.lastHit > BLEED_RESET_TICKS && now >= old.until);
        }
        flush(player, now);
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
        if (!player.m_6084_() || !KaguneProfile.isActive(player) || art.fired >= art.spikes) {
            return false;
        }
        int mastery = KaguneProfile.mastery(player);
        ServerLevel level = player.m_284548_();
        long age = now - art.started;
        Vec3 target = art.aimPoint;
        if (art.targetId > 0 && level.m_6815_(art.targetId) instanceof LivingEntity living && living.m_6084_()) {
            target = KaguneServerController.center(living);
        }
        if (art.pattern == StrikePattern.HAIL_OF_THORNS) {
            // The impact tick threw them up; from a few ticks later they come down, three a tick, over the target.
            if (age < 5) {
                return true;
            }
            for (int each = 0; each < 3 && art.fired < art.spikes; each++, art.fired++) {
                double angle = player.m_217043_().m_188500_() * Math.PI * 2.0;
                double r = Math.sqrt(player.m_217043_().m_188500_()) * UkakuRules.HAIL_RADIUS * (art.fired == 0 ? 0.0 : 1.0);
                Vec3 ground = target.m_82520_(Math.cos(angle) * r, -0.6, Math.sin(angle) * r);
                Vec3 sky = ground.m_82520_((player.m_217043_().m_188500_() - 0.5) * 1.5, UkakuRules.RAIN_HEIGHT, (player.m_217043_().m_188500_() - 0.5) * 1.5);
                launch(player, sky, ground.m_82546_(sky), UkakuRules.needleSpeed(mastery) * 0.7F, UkakuRules.NEEDLE_RADIUS * 1.4F, art.damage, 0, 1,
                    Kind.NEEDLE, UkakuRules.RAIN_HEIGHT + 6.0);
            }
            if (art.fired % 6 == 0) {
                UkakuFire.sound(level, target, "block.amethyst_block.hit", 0.8F, KaguneServerController.vary(player, 1.6F, 0.15F));
            }
            return true;
        }
        if (art.pattern == StrikePattern.SIX_TAIL_BARRAGE) {
            double t = age / (double)Math.max(1, art.duration);
            if (t > 0.9) {
                return false;
            }
            // All six stream needles where he is looking now (still on the target if it is roughly there), two a tick, the
            // tentacles taking turns round the six, never all at once.
            Vec3 aim = barrageAim(player, target, UkakuRules.NEEDLE_RANGE);
            for (int each = 0; each < UkakuRules.SIX_TAIL_PER_TICK && art.fired < art.spikes; each++, art.fired++) {
                int index = UkakuRules.sixTailTentacle(art.fired);
                Vec3 from = muzzles(player, index, aim, 3)[(art.fired / UkakuRules.SIX_TAIL_ORDER.length) % 3];
                Vec3 line = aim.m_82546_(from);
                double[] spread = UkakuRules.spreadDirection(line.f_82479_, line.f_82480_, line.f_82481_, 2.5F, player.m_217043_().m_188500_(),
                    player.m_217043_().m_188500_());
                launch(player, from, new Vec3(spread[0], spread[1], spread[2]), UkakuRules.needleSpeed(mastery), UkakuRules.NEEDLE_RADIUS, art.damage, 0, 1,
                    Kind.NEEDLE, UkakuRules.NEEDLE_RANGE);
            }
            if (art.fired % 4 < 2) {
                UkakuFire.sound(level, player.m_146892_(), "block.amethyst_block.hit", 0.45F, KaguneServerController.vary(player, 1.9F, 0.12F));
            }
            return true;
        }
        if (art.pattern == StrikePattern.SPIKE_RAIN) {
            // The impact tick (age 0) threw them up; from a few ticks later they come down, two a tick, over the target.
            if (age < 5) {
                return true;
            }
            for (int each = 0; each < 2 && art.fired < art.spikes; each++, art.fired++) {
                double angle = player.m_217043_().m_188500_() * Math.PI * 2.0;
                double r = Math.sqrt(player.m_217043_().m_188500_()) * UkakuRules.RAIN_RADIUS * (art.fired == 0 ? 0.0 : 1.0);
                Vec3 ground = target.m_82520_(Math.cos(angle) * r, -0.6, Math.sin(angle) * r);
                Vec3 sky = ground.m_82520_((player.m_217043_().m_188500_() - 0.5) * 1.5, UkakuRules.RAIN_HEIGHT, (player.m_217043_().m_188500_() - 0.5) * 1.5);
                launch(player, sky, ground.m_82546_(sky), UkakuRules.crystalSpeed(mastery) * 0.8F, UkakuRules.crystalRadius(mastery) * 1.2F, art.damage, 0,
                    1, Kind.RAIN, UkakuRules.RAIN_HEIGHT + 6.0);
            }
            if (art.fired % 4 == 0) {
                UkakuFire.sound(level, target, "block.amethyst_block.hit", 0.8F, KaguneServerController.vary(player, 1.2F, 0.15F));
            }
            return true;
        }
        if (art.pattern == StrikePattern.SPIKE_BARRAGE) {
            double t = age / (double)Math.max(1, art.duration);
            if (t > 0.95) {
                return false;
            }
            // Both wings machine-gun where he is looking now; still on the target if it is roughly there.
            Vec3 eye = player.m_146892_();
            double range = UkakuRules.crystalRange(mastery);
            Vec3 look = player.m_20154_();
            Vec3 aim = eye.m_82549_(look.m_82490_(range));
            Vec3 toTarget = target.m_82546_(eye);
            if (toTarget.m_82556_() > 1.0E-4 && toTarget.m_82541_().m_82526_(look) > 0.94 && toTarget.m_82556_() < range * range) {
                aim = target;
            } else {
                BlockHitResult block = level.m_45547_(new ClipContext(eye, aim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                if (block.m_6662_() == HitResult.Type.BLOCK) {
                    aim = block.m_82450_();
                }
            }
            WingForm form = UkakuServer.form(player);
            int side = form.rightWing() && art.fired % 2 == 0 ? 1 : -1;
            Vec3 tip = UkakuFire.tip(player, side, aim);
            double[] spread = UkakuRules.spreadDirection(aim.f_82479_ - tip.f_82479_, aim.f_82480_ - tip.f_82480_, aim.f_82481_ - tip.f_82481_,
                UkakuRules.BARRAGE_SPREAD, player.m_217043_().m_188500_(), player.m_217043_().m_188500_());
            launch(player, tip, new Vec3(spread[0], spread[1], spread[2]), UkakuRules.crystalSpeed(mastery) * 1.1F, UkakuRules.crystalRadius(mastery),
                art.damage, 0, 1, Kind.BARRAGE, range);
            art.fired++;
            if (art.fired % 2 == 1) {
                UkakuFire.sound(level, tip, "block.amethyst_block.hit", 0.55F, KaguneServerController.vary(player, 1.8F, 0.12F));
            }
            return true;
        }
        return false;
    }

    /**
     * The Crystal Bloom: every spike he has stuck in anyone within reach bursts now, harder, and a ring of spikes flies out
     * around him (so it still does something with nothing stuck).
     */
    /** Where a barrage aims this tick: where he is looking, or the target if it is roughly there, or the wall in the way. */
    private static Vec3 barrageAim(ServerPlayer player, Vec3 target, double range) {
        Vec3 eye = player.m_146892_();
        Vec3 look = player.m_20154_();
        Vec3 aim = eye.m_82549_(look.m_82490_(range));
        Vec3 toTarget = target.m_82546_(eye);
        if (toTarget.m_82556_() > 1.0E-4 && toTarget.m_82541_().m_82526_(look) > 0.94 && toTarget.m_82556_() < range * range) {
            return target;
        }
        BlockHitResult block = player.m_284548_().m_45547_(new ClipContext(eye, aim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return block.m_6662_() == HitResult.Type.BLOCK ? block.m_82450_() : aim;
    }

    private static void bloom(ServerPlayer player, float damage, int mastery, long now, boolean needles) {
        int burst = burstAll(player, false, now);
        // The ring: short spikes out of both wings all around him, level with his chest (the kakuja: needles out of all six).
        int ring = needles ? UkakuRules.BLOOM_RING_NEEDLES : 10;
        Vec3 chest = player.m_20182_().m_82520_(0.0, 1.2, 0.0);
        for (int index = 0; index < ring; index++) {
            double angle = Math.PI * 2.0 * index / ring + player.m_217043_().m_188500_() * 0.3;
            Vec3 heading = new Vec3(Math.cos(angle), needles ? 0.05 + 0.25 * (index % 3 - 1) : 0.05, Math.sin(angle));
            if (needles) {
                launch(player, chest.m_82549_(heading.m_82490_(1.0)), heading, UkakuRules.needleSpeed(mastery), UkakuRules.NEEDLE_RADIUS, damage * 0.3F, 0, 1,
                    Kind.NEEDLE, UkakuRules.NEEDLE_RANGE * 0.5);
            } else {
                launch(player, chest.m_82549_(heading.m_82490_(0.6)), heading, UkakuRules.crystalSpeed(mastery), UkakuRules.crystalRadius(mastery),
                    damage * 0.6F, 0, 1, Kind.FAN, UkakuRules.crystalRange(mastery) * 0.4);
            }
        }
        UkakuFire.fx(player, S2CUkakuFx.BLOOM, 0, chest, Vec3.f_82478_, 1.0F, burst);
        UkakuFire.sound(level(player), chest, "block.amethyst_block.break", 1.2F, KaguneServerController.vary(player, 0.6F, 0.05F));
        UkakuFire.sound(level(player), chest, "block.amethyst_block.chime", 1.2F, KaguneServerController.vary(player, 1.4F, 0.05F));
    }

    private static ServerLevel level(ServerPlayer player) {
        return player.m_284548_();
    }

    /**
     * The Crimson Pyre's burst: every spike and needle he has stuck in anyone within reach bursts at once, harder, into flame
     * (the bodies go up, fire blasts round each wound). Returns how many burst.
     */
    static int pyreBurst(ServerPlayer player, long now) {
        return burstAll(player, true, now);
    }

    /** Every spike he has stuck in anyone within {@link UkakuRules#BLOOM_RANGE} bursts now, {@link UkakuRules#BLOOM_MULTIPLIER} harder. */
    private static int burstAll(ServerPlayer player, boolean fire, long now) {
        UUID owner = player.m_20148_();
        ServerLevel level = player.m_284548_();
        double reach = UkakuRules.BLOOM_RANGE * UkakuRules.BLOOM_RANGE;
        int burst = 0;
        for (Iterator<Map.Entry<Integer, List<Stuck>>> entries = STUCK.entrySet().iterator(); entries.hasNext(); ) {
            Map.Entry<Integer, List<Stuck>> entry = entries.next();
            if (!(level.m_6815_(entry.getKey()) instanceof LivingEntity target) || !target.m_6084_() || target.m_20280_(player) > reach) {
                continue;
            }
            List<Stuck> mine = new ArrayList<>();
            entry.getValue().removeIf(stuck -> stuck.owner().equals(owner) && mine.add(stuck));
            if (entry.getValue().isEmpty()) {
                entries.remove();
            }
            for (Stuck stuck : mine) {
                if (!target.m_6084_()) {
                    dirty(level, stuck.targetId());
                    break;
                }
                burst(player, stuck, target, UkakuRules.BLOOM_MULTIPLIER, fire, now);
                burst++;
            }
        }
        return burst;
    }

    /** A needle thrown high coming down from {@code sky} onto {@code ground} (the final kakuja's Hellrain). */
    static void fallingNeedle(ServerPlayer player, Vec3 sky, Vec3 ground, float damage) {
        int mastery = KaguneProfile.mastery(player);
        launch(player, sky, ground.m_82546_(sky), UkakuRules.needleSpeed(mastery) * 0.7F, UkakuRules.NEEDLE_RADIUS * 1.4F, damage, 0, 1, Kind.NEEDLE,
            UkakuRules.RAIN_HEIGHT + 6.0);
    }

    /** A plain needle flying out from {@code from} along {@code heading} (the Crimson Pyre's ring). */
    static void ringNeedle(ServerPlayer player, Vec3 from, Vec3 heading, float damage, double range) {
        launch(player, from, heading, UkakuRules.needleSpeed(KaguneProfile.mastery(player)), UkakuRules.NEEDLE_RADIUS, damage, 0, 1, Kind.NEEDLE, range);
    }

    /** A fresh id for something drawn but not simulated (needles seen going up before they rain). */
    static int nextId() {
        return nextSpikeId++;
    }

    // ------------------------------------------------------------------ clients

    private static void dirty(Object level, int id) {
        DIRTY.computeIfAbsent(level, ignored -> new HashSet<>()).add(id);
    }

    /**
     * Tells every client near each body in this shooter's world whose spikes or bleeding changed (and, now and then, each
     * body carrying his spikes).
     */
    private static void flush(ServerPlayer player, long now) {
        ServerLevel level = player.m_284548_();
        if (now % RESEND_TICKS == 0L) {
            UUID owner = player.m_20148_();
            for (Map.Entry<Integer, List<Stuck>> entry : STUCK.entrySet()) {
                if (entry.getValue().stream().anyMatch(stuck -> stuck.owner().equals(owner))) {
                    dirty(level, entry.getKey());
                }
            }
        }
        Set<Integer> changed = DIRTY.remove(level);
        if (changed == null) {
            return;
        }
        for (Integer id : changed) {
            Entity entity = level.m_6815_(id);
            if (entity == null) {
                continue;
            }
            Bleed bleed = BLEEDS.get(entity.m_20148_());
            boolean bleeding = bleed != null && now < bleed.until;
            List<Stuck> list = STUCK.getOrDefault(id, List.of());
            List<S2CUkakuSpikes.Spike> spikes = new ArrayList<>();
            for (Stuck stuck : list) {
                spikes.add(new S2CUkakuSpikes.Spike(stuck.id(), (float)stuck.ox(), (float)stuck.oy(), (float)stuck.oz(), (float)stuck.dx(),
                    (float)stuck.dy(), (float)stuck.dz(), stuck.kind(), stuck.stuckAt(), stuck.burstAt(), stuck.limb()));
            }
            // The body's yaw as the server has it: clients turn the spikes from their own (vanilla never syncs a body's yaw).
            float yaw = entity instanceof LivingEntity living ? living.f_20883_ : entity.m_146908_();
            ControlNetwork.broadcastAround(entity, new S2CUkakuSpikes(id, bleeding ? bleed.stacks : 0, bleeding ? bleed.until : 0L, yaw, spikes));
        }
    }

    // ------------------------------------------------------------------ helpers

    /** A world offset turned into the body's own frame (x to its right, y up, z forward), from its body yaw. */
    private static double[] toBody(LivingEntity body, double x, double y, double z) {
        double yaw = Math.toRadians(body.f_20883_);
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double rightX = -Math.cos(yaw);
        double rightZ = -Math.sin(yaw);
        return new double[] {x * rightX + z * rightZ, y, x * forwardX + z * forwardZ};
    }

    /** The inverse of {@link #toBody}. */
    private static double[] fromBody(LivingEntity body, double x, double y, double z) {
        double yaw = Math.toRadians(body.f_20883_);
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double rightX = -Math.cos(yaw);
        double rightZ = -Math.sin(yaw);
        return new double[] {rightX * x + forwardX * z, y, rightZ * x + forwardZ * z};
    }

    private static double clamp(double value, double low, double high) {
        return high < low ? (low + high) * 0.5 : Math.max(low, Math.min(high, value));
    }
}
