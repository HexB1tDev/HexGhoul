package com.hex.kagune.control.client.fx;

import com.hex.kagune.control.ayato.RcShard;
import com.hex.kagune.control.client.AyatoClient;
import com.hex.kagune.control.client.WoundClient;
import com.hex.kagune.control.network.S2CAyatoFx;
import com.hex.kagune.control.wound.WoundBook;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Everything of Ayato's fire and crystal that the server announces ({@link S2CAyatoFx}), on this client: the shards and
 * spikes in flight (flown here from their start and velocity, drawn as {@link RcShard} and {@code CrystalSpike} by the wing
 * renderer, shedding Photon fire or glints each tick), and the bursts: impacts, the Rising Flame pillar, the hop's
 * downbeat, the dash's trail, embers left burning, spikes planted in blocks, stuck spikes bursting, the Crystal Bloom, the
 * blood they throw ({@link AyatoBlood}). Purely visual; the server owns every hit.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class AyatoFx {
    /**
     * A shard of fire or a crystal spike ({@link #crystal}) in flight; a {@code burning} one is a final kakuja needle wrapped in
     * flame.
     */
    public record Bolt(int id, int kind, int shooter, Vec3 start, Vec3 velocity, long born, int life, float size, int seed, boolean crystal,
                       boolean burning) {
        /** Where the head is at {@code time} (client game ticks, interpolated). */
        public Vec3 at(double time) {
            double age = Math.max(0.0, Math.min(this.life, time - this.born));
            return this.start.m_82549_(this.velocity.m_82490_(age));
        }
    }

    /** The kind a kakuja needle carries in flight, stuck and planted (after the crystal spikes' five). */
    public static final int NEEDLE_KIND = 5;

    /** A burst that keeps going for a few ticks. */
    private record Lasting(int kind, int shooter, Vec3 at, Vec3 direction, float size, long until) {
    }

    /** A Wing Vortex: flying from where it was thrown, then unwinding and fading where it ended. */
    public static final class Vortex {
        private final int id;
        private final int shooter;
        private final Vec3 start;
        private final Vec3 velocity;
        private final long born;
        private final int life;
        private final float size;
        private final int seed;
        private Vec3 end;
        /** Where it was attached when it ended (it detaches there and stays as it fades). */
        private Vec3 endRoot;
        private long endedAt = Long.MIN_VALUE;
        private int fade = 6;

        Vortex(int id, int shooter, Vec3 start, Vec3 velocity, long born, int life, float size, int seed) {
            this.id = id;
            this.shooter = shooter;
            this.start = start;
            this.velocity = velocity;
            this.born = born;
            this.life = life;
            this.size = size;
            this.seed = seed;
        }

        /** Where its point is at {@code time} (client game ticks, interpolated). */
        public Vec3 at(double time) {
            if (this.end != null && time >= this.endedAt) {
                return this.end;
            }
            double age = Math.max(0.0, Math.min(this.life, time - this.born));
            return this.start.m_82549_(this.velocity.m_82490_(age));
        }

        /**
         * Where it grows from at {@code time}: the shooter's shoulder blade while it is attached, then where it detached, drifting
         * a little off it as it unwinds.
         */
        public Vec3 root(double time, float partial) {
            Vec3 head = this.at(time);
            if (this.endRoot != null) {
                Vec3 along = head.m_82546_(this.endRoot);
                double length = along.m_82553_();
                double drift = Math.min(length * 0.5, 1.2 * this.unwind(time));
                return length < 1.0E-6 ? this.endRoot : this.endRoot.m_82549_(along.m_82490_(drift / length));
            }
            Vec3 shoulder = shoulderOf(this.shooter, partial);
            return shoulder != null ? shoulder : this.start;
        }

        /** 0 while it flies .. 1 once it has unwound and faded. */
        public float unwind(double time) {
            if (this.endedAt == Long.MIN_VALUE) {
                double over = time - (this.born + this.life);
                return over <= 0.0 ? 0.0F : (float)Math.min(1.0, over / this.fade);
            }
            return (float)Math.max(0.0, Math.min(1.0, (time - this.endedAt) / Math.max(1, this.fade)));
        }

        boolean done(long now) {
            return this.endedAt != Long.MIN_VALUE ? now > this.endedAt + this.fade + 1 : now > this.born + this.life + this.fade + 20;
        }

        public Vec3 velocity() {
            return this.velocity;
        }

        public long born() {
            return this.born;
        }

        public float size() {
            return this.size;
        }

        public int seed() {
            return this.seed;
        }

        public int shooter() {
            return this.shooter;
        }
    }

    private static final Map<Integer, Bolt> BOLTS = new HashMap<>();
    private static final Map<Integer, Vortex> VORTICES = new HashMap<>();
    private static final List<Lasting> LASTING = new ArrayList<>();
    private static final Random RANDOM = new Random();
    /** Never more than this many shards drawn at once (a crowd of Ayatos firing). */
    private static final int MAX_BOLTS = 96;
    static final double VISIBLE = 72.0;

    private AyatoFx() {
    }

    /** Shards to draw this frame. */
    public static Iterable<Bolt> bolts() {
        return BOLTS.values();
    }

    /** Wing Vortices to draw this frame. */
    public static Iterable<Vortex> vortices() {
        return VORTICES.values();
    }

    public static void accept(S2CAyatoFx message) {
        ClientLevel level = Minecraft.m_91087_().f_91073_;
        if (level == null) {
            return;
        }
        long now = level.m_46467_();
        Vec3 at = new Vec3(message.x(), message.y(), message.z());
        Vec3 velocity = new Vec3(message.vx(), message.vy(), message.vz());
        int kind = message.kind();
        if (kind >= S2CAyatoFx.BOLT && kind < S2CAyatoFx.BOLT + 4) {
            if (BOLTS.size() < MAX_BOLTS) {
                BOLTS.put(message.id(), new Bolt(message.id(), kind - S2CAyatoFx.BOLT, message.shooter(), at, velocity, now, Math.max(1, message.extra()),
                    message.size(), RANDOM.nextInt(1000), false, false));
            }
            if (RcFx.near(at, VISIBLE)) {
                muzzle(at, velocity, message.size());
            }
            return;
        }
        if (kind == S2CAyatoFx.NEEDLE || kind == S2CAyatoFx.FLAME_NEEDLE) {
            boolean burning = kind == S2CAyatoFx.FLAME_NEEDLE;
            if (BOLTS.size() < MAX_BOLTS) {
                BOLTS.put(message.id(), new Bolt(message.id(), NEEDLE_KIND, message.shooter(), at, velocity, now, Math.max(1, message.extra()), message.size(),
                    RANDOM.nextInt(1000), true, burning));
            }
            if (RcFx.near(at, VISIBLE)) {
                crystalMuzzle(at, velocity, 0.45F * message.size());
                if (burning) {
                    muzzle(at, velocity, 0.45F * message.size());
                }
            }
            return;
        }
        if (kind >= S2CAyatoFx.SPIKE && kind < S2CAyatoFx.SPIKE + 5) {
            if (BOLTS.size() < MAX_BOLTS) {
                BOLTS.put(message.id(), new Bolt(message.id(), kind - S2CAyatoFx.SPIKE, message.shooter(), at, velocity, now, Math.max(1, message.extra()),
                    message.size(), RANDOM.nextInt(1000), true, false));
            }
            if (RcFx.near(at, VISIBLE)) {
                crystalMuzzle(at, velocity, message.size());
            }
            return;
        }
        switch (kind) {
            case S2CAyatoFx.IMPACT -> {
                Bolt bolt = BOLTS.remove(message.id());
                if (RcFx.near(at, VISIBLE)) {
                    impact(at, velocity, bolt == null ? 1.0F : bolt.size(), message.extra() == 1, message.size() > 0.5F);
                }
            }
            case S2CAyatoFx.FIZZLE -> {
                BOLTS.remove(message.id());
                if (RcFx.near(at, VISIBLE)) {
                    fizzle(at);
                }
            }
            case S2CAyatoFx.PILLAR -> {
                LASTING.add(new Lasting(kind, message.shooter(), at, Vec3.f_82478_, message.size(), now + 14));
                if (RcFx.near(at, VISIBLE)) {
                    pillarBurst(at, message.size());
                }
            }
            case S2CAyatoFx.HOP -> {
                if (RcFx.near(at, VISIBLE)) {
                    Entity shooter = level.m_6815_(message.shooter());
                    if (shooter instanceof net.minecraft.world.entity.player.Player player && AyatoClient.crystalMode(player)) {
                        crystalHop(at, velocity);
                    } else {
                        hop(at, velocity);
                    }
                }
            }
            case S2CAyatoFx.DASH -> LASTING.add(new Lasting(kind, message.shooter(), at, velocity, 1.0F, now + Math.max(2, message.extra())));
            case S2CAyatoFx.EMBERS -> LASTING.add(new Lasting(kind, message.shooter(), at, Vec3.f_82478_, message.size(), now + Math.max(2, message.extra())));
            case S2CAyatoFx.SPIKE_IMPACT -> {
                Bolt bolt = BOLTS.remove(message.id());
                boolean stuck = message.size() > 0.5F;
                float size = bolt == null ? 1.0F : bolt.size();
                if (!stuck) {
                    // It hit a block: it stays planted in it a moment (5.0.3: buried to nearly half its length), then crumbles.
                    Vec3 dir = velocity.m_82556_() < 1.0E-6 ? new Vec3(0.0, -1.0, 0.0) : velocity.m_82541_();
                    double length = message.extra() == NEEDLE_KIND ? com.hex.kagune.control.ayato.KakujaNeedle.LENGTH
                        : com.hex.kagune.control.ayato.CrystalSpike.LENGTH;
                    AyatoSpikes.plant(at.m_82549_(dir.m_82490_(AyatoSpikes.PLANTED_DEPTH * length * size)), dir, size, message.extra(), now,
                        RANDOM.nextInt(1000));
                }
                if (RcFx.near(at, VISIBLE)) {
                    crystalImpact(at, velocity, size, stuck);
                    if (bolt != null && bolt.burning()) {
                        impact(at, velocity, 0.5F * size, false, stuck);
                    }
                }
            }
            case S2CAyatoFx.SPIKE_FIZZLE -> {
                Bolt bolt = BOLTS.remove(message.id());
                if (RcFx.near(at, VISIBLE)) {
                    crumble(at, velocity, bolt == null ? 1.0F : bolt.size());
                }
            }
            case S2CAyatoFx.SPIKE_BURST -> {
                if (RcFx.near(at, VISIBLE)) {
                    spikeBurst(at, velocity, message.size(), message.extra() == 1);
                }
            }
            case S2CAyatoFx.BLOOD -> {
                if (RcFx.near(at, VISIBLE)) {
                    AyatoBlood.spurt(level, at, velocity, message.size(), level.m_6815_(message.id()));
                }
            }
            case S2CAyatoFx.KANEKI_HIT -> kanekiHit(level, message, at, velocity);
            case S2CAyatoFx.HIT -> hit(level, message, at, velocity);
            case S2CAyatoFx.BLADE_HIT -> BladeBloodFx.hit(level, message, at, velocity, RcFx.near(at, VISIBLE));
            case S2CAyatoFx.VANISH, S2CAyatoFx.REVEAL, S2CAyatoFx.GUST -> {
                if (RcFx.near(at, VISIBLE)) {
                    VanishFx.accept(message, at, velocity);
                }
            }
            case S2CAyatoFx.THORNS, S2CAyatoFx.THORNS_PIERCE, S2CAyatoFx.THORNS_END -> AyatoThorns.accept(level, message, at, now);
            case S2CAyatoFx.BLOOM -> {
                if (RcFx.near(at, VISIBLE)) {
                    bloom(at, message.extra());
                }
            }
            case S2CAyatoFx.BURST_BLAST -> {
                if (RcFx.near(at, VISIBLE)) {
                    if (message.extra() == 1) {
                        // A fire shard bursting (Bursting Shards): a ball of fire, not crystal.
                        impact(at, Vec3.f_82478_, message.size(), true, true);
                    } else {
                        blast(at, message.size());
                    }
                }
            }
            case S2CAyatoFx.VORTEX -> {
                if (VORTICES.size() < 32) {
                    VORTICES.put(message.id(), new Vortex(message.id(), message.shooter(), at, velocity, now, Math.max(1, message.extra()), message.size(),
                        RANDOM.nextInt(1000)));
                }
                if (RcFx.near(at, VISIBLE)) {
                    vortexLaunch(at, velocity, message.size());
                }
            }
            case S2CAyatoFx.VORTEX_HIT -> {
                if (RcFx.near(at, VISIBLE)) {
                    vortexHit(at, velocity, message.size());
                }
            }
            case S2CAyatoFx.VORTEX_END -> {
                Vortex vortex = VORTICES.get(message.id());
                if (vortex != null && vortex.endedAt == Long.MIN_VALUE) {
                    // The wing detaches from his back where it was.
                    vortex.endRoot = vortex.root(now, 1.0F);
                    vortex.end = at;
                    vortex.endedAt = now;
                    vortex.fade = Math.max(1, message.extra());
                }
                if (RcFx.near(at, VISIBLE)) {
                    vortexEnd(at, velocity, message.size());
                }
            }
            case S2CAyatoFx.CRYSTALLIZE -> {
                Entity shooter = level.m_6815_(message.shooter());
                if (shooter != null && RcFx.near(shooter.m_20182_(), VISIBLE)) {
                    crystallize(shooter, message.extra() == 1);
                }
            }
            default -> {
            }
        }
    }

    /**
     * One of Kaneki's tails hit a body (3.0.3): blood bursts out where it struck, and a player's wounds take the blow's shape (a
     * slash's gash where it hit and along the swing, a stab's hole, a crush's or a slam's big tears).
     */
    private static void kanekiHit(ClientLevel level, S2CAyatoFx message, Vec3 at, Vec3 into) {
        Entity body = level.m_6815_(message.id());
        if (RcFx.near(at, VISIBLE)) {
            AyatoBlood.spurt(level, at, into.m_82490_(-1.0), message.size(), body);
        }
        wound(body, message, at, into);
    }

    /**
     * 5.0.17: any other hit on a player: blood where it went in if it draws blood (not a burn, a bruise or poison), and the wounds
     * it opens take its shape there.
     */
    private static void hit(ClientLevel level, S2CAyatoFx message, Vec3 at, Vec3 into) {
        Entity body = level.m_6815_(message.id());
        WoundBook.Blow blow = WoundBook.Blow.byOrdinal(message.extra() & 15);
        if (blow.bleeds() && into.m_82556_() > 1.0E-6 && RcFx.near(at, VISIBLE)) {
            AyatoBlood.spurt(level, at, into.m_82490_(-1.0), message.size() * 0.8F, body);
        }
        wound(body, message, at, into);
    }

    /** The wounds a hit opens on a player take its shape where it landed ({@code into} zero: anywhere fitting). */
    private static void wound(Entity body, S2CAyatoFx message, Vec3 at, Vec3 into) {
        if (!(body instanceof AbstractClientPlayer player) || !player.m_6084_()) {
            return;
        }
        WoundBook.Blow blow = WoundBook.Blow.byOrdinal(message.extra() & 15);
        double angle = Math.toRadians((message.extra() >> 4) - 180.0);
        if (into.m_82556_() < 1.0E-6) {
            WoundClient.blow(player, blow, null, angle);
            return;
        }
        // The hit in the body's own frame (x to its right, y up from the feet, z forward), as the spikes are placed.
        double yaw = Math.toRadians(player.f_20883_);
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double rightX = -Math.cos(yaw);
        double rightZ = -Math.sin(yaw);
        double dx = at.f_82479_ - player.m_20185_();
        double dy = at.f_82480_ - player.m_20186_();
        double dz = at.f_82481_ - player.m_20189_();
        WoundBook.Spot spot = WoundBook.spot(dx * rightX + dz * rightZ, dy, dx * forwardX + dz * forwardZ,
            into.f_82479_ * rightX + into.f_82481_ * rightZ, into.f_82480_, into.f_82479_ * forwardX + into.f_82481_ * forwardZ,
            "slim".equals(player.m_108564_()));
        WoundClient.blow(player, blow, spot, angle);
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        ClientLevel level = minecraft.f_91073_;
        if (level == null || minecraft.m_91104_()) {
            return;
        }
        long now = level.m_46467_();
        for (Iterator<Bolt> iterator = BOLTS.values().iterator(); iterator.hasNext(); ) {
            Bolt bolt = iterator.next();
            if (now - bolt.born() > bolt.life() + 2) {
                iterator.remove();
                continue;
            }
            Vec3 head = bolt.at(now);
            if (RcFx.near(head, VISIBLE)) {
                if (bolt.crystal()) {
                    crystalTrail(bolt, head);
                    if (bolt.burning()) {
                        flameWrap(bolt, head);
                    }
                } else {
                    trail(bolt, head);
                }
            }
        }
        for (Iterator<Vortex> iterator = VORTICES.values().iterator(); iterator.hasNext(); ) {
            Vortex vortex = iterator.next();
            if (vortex.done(now)) {
                iterator.remove();
                continue;
            }
            Vec3 head = vortex.at(now);
            if (RcFx.near(head, VISIBLE)) {
                if (vortex.endedAt == Long.MIN_VALUE) {
                    vortexTrail(vortex, head, now);
                } else {
                    vortexUnwinding(vortex, head);
                }
            }
        }
        for (Iterator<Lasting> iterator = LASTING.iterator(); iterator.hasNext(); ) {
            Lasting lasting = iterator.next();
            if (now >= lasting.until()) {
                iterator.remove();
                continue;
            }
            switch (lasting.kind()) {
                case S2CAyatoFx.PILLAR -> {
                    if (RcFx.near(lasting.at(), VISIBLE)) {
                        pillar(lasting.at(), lasting.size(), (lasting.until() - now) / 14.0F);
                    }
                }
                case S2CAyatoFx.DASH -> {
                    Entity shooter = level.m_6815_(lasting.shooter());
                    if (shooter != null && RcFx.near(shooter.m_20182_(), VISIBLE)) {
                        dash(shooter.m_20182_(), lasting.direction());
                    }
                }
                case S2CAyatoFx.EMBERS -> {
                    if (now % 3L == 0L && RcFx.near(lasting.at(), VISIBLE)) {
                        embers(lasting.at(), lasting.size());
                    }
                }
                default -> {
                }
            }
        }
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        BOLTS.clear();
        VORTICES.clear();
        LASTING.clear();
        RcFx.reset();
    }

    // ------------------------------------------------------------------ the fire

    static Vec3 jitter(double spread) {
        return new Vec3((RANDOM.nextDouble() - 0.5) * spread, (RANDOM.nextDouble() - 0.5) * spread, (RANDOM.nextDouble() - 0.5) * spread);
    }

    /** Unit vector in a random direction. */
    static Vec3 sphere() {
        double z = RANDOM.nextDouble() * 2.0 - 1.0;
        double a = RANDOM.nextDouble() * Math.PI * 2.0;
        double s = Math.sqrt(1.0 - z * z);
        return new Vec3(s * Math.cos(a), z, s * Math.sin(a));
    }

    /** RC flame: molten into red into nothing, rising a little. */
    private static void flame(Vec3 at, Vec3 velocity, float size, int life) {
        RcFx.at(RcFx.FLAME, true, life, at).velocity(velocity).drag(0.88F).fall(-0.003F).wander(0.01F).bloom(1)
            .colors(RANDOM.nextFloat() < 0.35F ? RcFx.MOLTEN : RcFx.FIRE, RcFx.clear(RcFx.CRIMSON)).size(size, size * 0.4F).spawn();
    }

    private static void ember(Vec3 at, Vec3 velocity, float size, int life) {
        RcFx.at(RcFx.EMBER, true, life, at).velocity(velocity).drag(0.93F).fall(0.004F).wander(0.008F).bloom(0)
            .colors(RANDOM.nextFloat() < 0.5F ? RcFx.CORE : RcFx.MOLTEN, RcFx.clear(RcFx.RED)).size(size, size * 0.4F).spawn();
    }

    /** The pink glow that hangs around RC fire. */
    private static void aura(Vec3 at, Vec3 velocity, float size, int life) {
        RcFx.at(RcFx.MOTE, true, life, at).velocity(velocity).drag(0.9F).fall(-0.001F).bloom(2)
            .colors(0x80F2A2F0, RcFx.clear(RcFx.PINK)).size(size, size * 1.4F).spawn();
    }

    private static void smoke(Vec3 at, Vec3 velocity, float size, int life) {
        RcFx.at(RcFx.PUFF, false, life, at).velocity(velocity).drag(0.92F).fall(-0.002F).wander(0.004F).bloom(4)
            .colors(RcFx.SMOKE, 0x00100406).size(size, size * 2.2F).spawn();
    }

    /** A shard leaving the wing: a flash of fire at the muzzle. */
    private static void muzzle(Vec3 at, Vec3 velocity, float size) {
        Vec3 dir = velocity.m_82556_() < 1.0E-6 ? Vec3.f_82478_ : velocity.m_82541_();
        for (int i = RcFx.count(5 * size); i > 0; i--) {
            flame(at.m_82549_(jitter(0.15)), dir.m_82490_(0.08).m_82549_(jitter(0.08)), 0.28F * size, 6 + RANDOM.nextInt(4));
        }
        for (int i = RcFx.count(3); i > 0; i--) {
            ember(at, dir.m_82490_(0.12).m_82549_(jitter(0.15)), 0.06F, 10 + RANDOM.nextInt(8));
        }
        aura(at, Vec3.f_82478_, 0.5F * size, 5);
    }

    /** What a shard sheds each tick of flight: fire peeling off behind it, embers, a little of the pink glow. */
    private static void trail(Bolt bolt, Vec3 head) {
        Vec3 dir = bolt.velocity().m_82556_() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : bolt.velocity().m_82541_();
        float size = bolt.size();
        double step = bolt.velocity().m_82553_();
        // Spread the fire along this tick's stretch of path so a fast shard leaves an unbroken streak.
        int pieces = Math.max(2, (int)Math.ceil(step / 0.45));
        for (int i = 0; i < pieces; i++) {
            Vec3 at = head.m_82546_(dir.m_82490_(step * i / pieces + 0.15 * size));
            if (RANDOM.nextFloat() < RcFx.density()) {
                flame(at.m_82549_(jitter(0.1 * size)), dir.m_82490_(-0.03).m_82549_(jitter(0.03)), (0.3F + 0.12F * RANDOM.nextFloat()) * size,
                    7 + RANDOM.nextInt(6));
            }
        }
        if (RANDOM.nextFloat() < 0.6F * RcFx.density()) {
            ember(head.m_82549_(jitter(0.2 * size)), jitter(0.06), 0.05F + 0.03F * size, 14 + RANDOM.nextInt(12));
        }
        if (bolt.kind() == 1 || RANDOM.nextFloat() < 0.25F) {
            aura(head, Vec3.f_82478_, 0.45F * size, 4);
        }
        if (bolt.kind() == 1 && RANDOM.nextFloat() < 0.5F) {
            smoke(head.m_82546_(dir.m_82490_(0.6 * size)), jitter(0.02), 0.4F * size, 20 + RANDOM.nextInt(10));
        }
    }

    /** A shard bursting where it ended: a flower of fire, embers flung out, the pink flash, a ring and red veins on a hit. */
    private static void impact(Vec3 at, Vec3 velocity, float size, boolean flare, boolean hit) {
        float scale = flare ? 2.2F : Math.max(0.8F, size);
        for (int i = RcFx.count(12 * scale); i > 0; i--) {
            flame(at, sphere().m_82490_(0.08 + 0.12 * RANDOM.nextDouble() * scale), (0.3F + 0.15F * RANDOM.nextFloat()) * scale, 8 + RANDOM.nextInt(8));
        }
        for (int i = RcFx.count(10 * scale); i > 0; i--) {
            ember(at, sphere().m_82490_(0.12 + 0.18 * RANDOM.nextDouble()).m_82520_(0.0, 0.08, 0.0), 0.05F + 0.03F * RANDOM.nextFloat(),
                16 + RANDOM.nextInt(14));
        }
        aura(at, Vec3.f_82478_, 0.9F * scale, 6);
        RcFx.at(RcFx.RING, true, 8, at).bloom(0).colors(0xFFFF6A3A, RcFx.clear(RcFx.CRIMSON)).size(0.3F * scale, 2.4F * scale).spawn();
        if (hit || flare) {
            RcFx.at(RcFx.VEINS, true, 7, at).bloom(0).colors(0xFFFF2A3A, RcFx.clear(RcFx.CRIMSON)).size(0.9F * scale, 1.6F * scale).spawn();
        }
        for (int i = RcFx.count(flare ? 4 : 1); i > 0; i--) {
            smoke(at.m_82549_(jitter(0.3 * scale)), new Vec3(0.0, 0.02, 0.0).m_82549_(jitter(0.02)), 0.5F * scale, 28 + RANDOM.nextInt(16));
        }
    }

    /** A shard burning out at the end of its range. */
    private static void fizzle(Vec3 at) {
        for (int i = RcFx.count(4); i > 0; i--) {
            flame(at, jitter(0.06), 0.22F, 6 + RANDOM.nextInt(4));
        }
        smoke(at, new Vec3(0.0, 0.02, 0.0), 0.3F, 20);
    }

    /** The Rising Flame's first instant: a ring and veins of fire on the ground. */
    private static void pillarBurst(Vec3 base, float radius) {
        Vec3 ground = base.m_82520_(0.0, 0.1, 0.0);
        RcFx.at(RcFx.RING, true, 10, ground).bloom(0).colors(0xFFFF6A3A, RcFx.clear(RcFx.CRIMSON)).size(0.5F, radius * 3.0F).spawn();
        RcFx.at(RcFx.VEINS, true, 12, ground).bloom(0).colors(0xFFFF2A3A, RcFx.clear(RcFx.CRIMSON)).size(radius * 1.6F, radius * 2.2F).spawn();
        for (int i = RcFx.count(16); i > 0; i--) {
            Vec3 out = sphere();
            ember(ground, new Vec3(out.f_82479_ * 0.25, 0.25 + 0.3 * RANDOM.nextDouble(), out.f_82481_ * 0.25), 0.06F, 18 + RANDOM.nextInt(12));
        }
    }

    /** The pillar going up: fire pouring upward through the column, thinning as it ends ({@code left} 1 .. 0). */
    private static void pillar(Vec3 base, float radius, float left) {
        for (int i = RcFx.count(9 * left + 2); i > 0; i--) {
            double a = RANDOM.nextDouble() * Math.PI * 2.0;
            double r = Math.sqrt(RANDOM.nextDouble()) * radius * 0.8;
            Vec3 at = base.m_82520_(Math.cos(a) * r, 0.1 + RANDOM.nextDouble() * 0.6, Math.sin(a) * r);
            RcFx.at(RcFx.FLAME, true, 10 + RANDOM.nextInt(6), at).velocity(new Vec3(-Math.cos(a) * r * 0.04, 0.35 + 0.3 * RANDOM.nextDouble(),
                    -Math.sin(a) * r * 0.04)).drag(0.9F).fall(-0.01F).wander(0.01F).bloom(1)
                .colors(RANDOM.nextFloat() < 0.4F ? RcFx.MOLTEN : RcFx.FIRE, RcFx.clear(RcFx.CRIMSON)).size(0.55F, 0.25F).spawn();
        }
        if (RANDOM.nextFloat() < 0.6F * left) {
            aura(base.m_82520_(0.0, 1.0 + RANDOM.nextDouble(), 0.0), new Vec3(0.0, 0.15, 0.0), radius * 0.8F, 6);
        }
    }

    /** The hop's downbeat: fire pushed down and forward off the wings, embers kicked up. */
    private static void hop(Vec3 feet, Vec3 back) {
        Vec3 forward = back.m_82490_(-1.0);
        for (int i = RcFx.count(10); i > 0; i--) {
            flame(feet.m_82520_(0.0, 0.8 + RANDOM.nextDouble() * 0.8, 0.0).m_82549_(jitter(0.6)), forward.m_82490_(0.12).m_82520_(0.0, -0.08, 0.0)
                .m_82549_(jitter(0.06)), 0.35F, 8 + RANDOM.nextInt(5));
        }
        for (int i = RcFx.count(8); i > 0; i--) {
            ember(feet.m_82520_(0.0, 0.1, 0.0), jitter(0.3).m_82520_(0.0, 0.15, 0.0), 0.05F, 16 + RANDOM.nextInt(10));
        }
        smoke(feet.m_82520_(0.0, 0.2, 0.0), new Vec3(0.0, 0.03, 0.0), 0.6F, 24);
    }

    /** The dash: the wings stream fire behind him as he goes. */
    private static void dash(Vec3 position, Vec3 ahead) {
        Vec3 back = ahead.m_82556_() < 1.0E-6 ? Vec3.f_82478_ : ahead.m_82541_().m_82490_(-1.0);
        for (int i = RcFx.count(8); i > 0; i--) {
            Vec3 at = position.m_82520_(0.0, 0.4 + RANDOM.nextDouble() * 1.4, 0.0).m_82549_(back.m_82490_(RANDOM.nextDouble() * 1.2)).m_82549_(jitter(0.6));
            flame(at, back.m_82490_(0.05).m_82549_(jitter(0.04)), 0.4F + 0.2F * RANDOM.nextFloat(), 9 + RANDOM.nextInt(6));
        }
        for (int i = RcFx.count(3); i > 0; i--) {
            ember(position.m_82520_(0.0, 0.5 + RANDOM.nextDouble(), 0.0), jitter(0.1), 0.06F, 18 + RANDOM.nextInt(10));
        }
        aura(position.m_82520_(0.0, 1.0, 0.0), Vec3.f_82478_, 1.2F, 4);
    }

    /** Embers still burning on the ground. */
    private static void embers(Vec3 at, float radius) {
        for (int i = RcFx.count(2); i > 0; i--) {
            double a = RANDOM.nextDouble() * Math.PI * 2.0;
            double r = RANDOM.nextDouble() * radius;
            Vec3 spot = at.m_82520_(Math.cos(a) * r, 0.05, Math.sin(a) * r);
            RcFx.at(RcFx.FLAME, true, 10 + RANDOM.nextInt(6), spot).velocity(new Vec3(0.0, 0.05, 0.0)).drag(0.9F).fall(-0.004F).wander(0.006F).bloom(1)
                .colors(RcFx.FIRE, RcFx.clear(RcFx.CRIMSON)).size(0.22F, 0.1F).spawn();
        }
    }
    // ------------------------------------------------------------------ the crystal

    /** RC crystal's colours (the wing's crystal): ember at the root, red, crimson, violet, the pale pink of a point. */
    private static final int GEM_RED = 0xFFFF1020;
    static final int GEM_CRIMSON = 0xFFC80A26;
    private static final int GEM_VIOLET = 0xFF420A50;
    static final int GEM_POINT = 0xFFFFB4BC;
    private static final int GEM_GLINT = 0xFFFFF2FA;

    /** A glint of crystal: a white-pink spark that flashes and is gone. */
    static void glint(Vec3 at, Vec3 velocity, float size, int life) {
        RcFx.at(RcFx.MOTE, true, life, at).velocity(velocity).drag(0.9F).bloom(0)
            .colors(RANDOM.nextFloat() < 0.5F ? GEM_GLINT : GEM_POINT, RcFx.clear(GEM_RED)).size(size, size * 0.2F).spawn();
    }

    /** A chip of crystal: a red shard flung off that falls and fades. */
    static void chip(Vec3 at, Vec3 velocity, float size, int life) {
        RcFx.at(RcFx.STREAK, true, life, at).velocity(velocity).drag(0.94F).fall(0.025F).bloom(0)
            .colors(RANDOM.nextFloat() < 0.4F ? GEM_POINT : RANDOM.nextFloat() < 0.6F ? GEM_RED : GEM_CRIMSON, RcFx.clear(GEM_VIOLET))
            .size(size, size * 0.5F).spawn();
    }

    /** The faint red mist RC crystal leaves hanging in the air. */
    static void haze(Vec3 at, Vec3 velocity, float size, int life) {
        RcFx.at(RcFx.MOTE, true, life, at).velocity(velocity).drag(0.9F).fall(-0.001F).bloom(2)
            .colors(0x60FF2C58, RcFx.clear(GEM_CRIMSON)).size(size, size * 1.5F).spawn();
    }

    /** A spike leaving the wing: a ring of glints and a puff of red at the wing tip. */
    private static void crystalMuzzle(Vec3 at, Vec3 velocity, float size) {
        Vec3 dir = velocity.m_82556_() < 1.0E-6 ? Vec3.f_82478_ : velocity.m_82541_();
        for (int i = RcFx.count(5 * size); i > 0; i--) {
            glint(at.m_82549_(jitter(0.12)), dir.m_82490_(0.05).m_82549_(jitter(0.08)), 0.12F * size, 4 + RANDOM.nextInt(4));
        }
        haze(at, Vec3.f_82478_, 0.35F * size, 5);
    }

    /** What a spike sheds in flight: a thin trail of glints and red haze (the crystal doesn't burn like the fire). */
    private static void crystalTrail(Bolt bolt, Vec3 head) {
        Vec3 dir = bolt.velocity().m_82556_() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : bolt.velocity().m_82541_();
        double step = bolt.velocity().m_82553_();
        int pieces = Math.max(2, (int)Math.ceil(step / 0.6));
        for (int i = 0; i < pieces; i++) {
            Vec3 at = head.m_82546_(dir.m_82490_(step * i / pieces + 0.4 * bolt.size()));
            if (RANDOM.nextFloat() < 0.6F * RcFx.density()) {
                haze(at.m_82549_(jitter(0.05)), dir.m_82490_(-0.01), (0.16F + 0.06F * RANDOM.nextFloat()) * bolt.size(), 6 + RANDOM.nextInt(4));
            }
        }
        if (RANDOM.nextFloat() < 0.5F * RcFx.density()) {
            glint(head.m_82549_(jitter(0.15)), jitter(0.02), 0.09F, 5 + RANDOM.nextInt(5));
        }
    }

    /**
     * A final kakuja needle wrapped in flame: fire licking round its shaft and peeling off behind it (bigger for a bigger needle,
     * the Pyre Lance a torch), a few embers.
     */
    private static void flameWrap(Bolt bolt, Vec3 head) {
        Vec3 dir = bolt.velocity().m_82556_() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : bolt.velocity().m_82541_();
        float size = Math.max(0.6F, bolt.size());
        double step = bolt.velocity().m_82553_();
        int pieces = Math.max(2, (int)Math.ceil(step / 0.5));
        for (int i = 0; i < pieces; i++) {
            Vec3 at = head.m_82546_(dir.m_82490_(step * i / pieces + 0.25 * size));
            if (RANDOM.nextFloat() < 0.8F * RcFx.density()) {
                flame(at.m_82549_(jitter(0.08 * size)), dir.m_82490_(-0.02).m_82549_(jitter(0.025)), (0.16F + 0.08F * RANDOM.nextFloat()) * size,
                    5 + RANDOM.nextInt(4));
            }
        }
        if (RANDOM.nextFloat() < 0.35F * RcFx.density()) {
            ember(head.m_82549_(jitter(0.15 * size)), jitter(0.05), 0.04F + 0.02F * size, 10 + RANDOM.nextInt(8));
        }
    }

    /** A spike stopping: chips and glints off the point, a ring of red if it went into a body. */
    private static void crystalImpact(Vec3 at, Vec3 velocity, float size, boolean body) {
        Vec3 back = velocity.m_82556_() < 1.0E-6 ? new Vec3(0.0, 1.0, 0.0) : velocity.m_82541_().m_82490_(-1.0);
        for (int i = RcFx.count(6 * size); i > 0; i--) {
            chip(at, back.m_82490_(0.08 + 0.1 * RANDOM.nextDouble()).m_82549_(jitter(0.16)).m_82520_(0.0, 0.05, 0.0), 0.1F * size, 10 + RANDOM.nextInt(8));
        }
        for (int i = RcFx.count(4); i > 0; i--) {
            glint(at.m_82549_(jitter(0.2)), jitter(0.05), 0.12F, 4 + RANDOM.nextInt(4));
        }
        if (body) {
            RcFx.at(RcFx.RING, true, 6, at).bloom(0).colors(0xC0FF2C58, RcFx.clear(GEM_CRIMSON)).size(0.2F * size, 1.1F * size).spawn();
        }
    }

    /** A planted or spent spike crumbling into chips and glints. */
    static void crumble(Vec3 at, Vec3 direction, float size) {
        Vec3 dir = direction.m_82556_() < 1.0E-6 ? new Vec3(0.0, 1.0, 0.0) : direction.m_82541_();
        for (int i = RcFx.count(6 * size); i > 0; i--) {
            Vec3 along = at.m_82546_(dir.m_82490_(RANDOM.nextDouble() * 0.8 * size));
            chip(along, jitter(0.08).m_82520_(0.0, 0.04, 0.0), 0.08F * size, 12 + RANDOM.nextInt(8));
        }
        for (int i = RcFx.count(3); i > 0; i--) {
            glint(at.m_82549_(jitter(0.3)), jitter(0.02), 0.1F, 5 + RANDOM.nextInt(5));
        }
        haze(at, new Vec3(0.0, 0.01, 0.0), 0.4F * size, 10);
    }

    /** A stuck spike bursting out of a body along {@code out}: a star of crystal chips, a white flash, a red ring. */
    private static void spikeBurst(Vec3 at, Vec3 out, float strength, boolean lance) {
        float scale = Math.max(1.0F, strength) * (lance ? 1.4F : 1.0F);
        Vec3 dir = out.m_82556_() < 1.0E-6 ? new Vec3(0.0, 1.0, 0.0) : out.m_82541_();
        for (int i = RcFx.count(14 * scale); i > 0; i--) {
            Vec3 way = dir.m_82490_(0.6).m_82549_(sphere()).m_82541_();
            chip(at, way.m_82490_(0.12 + 0.2 * RANDOM.nextDouble() * scale).m_82520_(0.0, 0.05, 0.0), (0.1F + 0.06F * RANDOM.nextFloat()) * scale,
                12 + RANDOM.nextInt(10));
        }
        for (int i = RcFx.count(8 * scale); i > 0; i--) {
            glint(at.m_82549_(jitter(0.25)), sphere().m_82490_(0.06), 0.16F * scale, 4 + RANDOM.nextInt(4));
        }
        RcFx.at(RcFx.MOTE, true, 4, at).bloom(0).colors(0xFFFFFFFF, RcFx.clear(GEM_POINT)).size(0.9F * scale, 1.5F * scale).spawn();
        RcFx.at(RcFx.RING, true, 7, at).bloom(0).colors(0xE0FF2C58, RcFx.clear(GEM_CRIMSON)).size(0.25F * scale, 1.8F * scale).spawn();
        haze(at, Vec3.f_82478_, 0.7F * scale, 8);
    }

    /** The Crystal Bloom: a flower of crystal light around him (bigger the more spikes it burst). */
    private static void bloom(Vec3 chest, int burst) {
        float scale = 1.0F + Math.min(1.5F, burst * 0.12F);
        RcFx.at(RcFx.RING, true, 10, chest).bloom(0).colors(0xFFFF6A8A, RcFx.clear(GEM_CRIMSON)).size(0.4F, 5.0F * scale).spawn();
        RcFx.at(RcFx.VEINS, true, 9, chest).bloom(0).colors(0xFFFF2A3A, RcFx.clear(GEM_CRIMSON)).size(1.2F * scale, 2.6F * scale).spawn();
        for (int i = RcFx.count(24 * scale); i > 0; i--) {
            Vec3 way = sphere();
            glint(chest.m_82549_(way.m_82490_(0.4)), way.m_82490_(0.15 + 0.15 * RANDOM.nextDouble()), 0.16F, 6 + RANDOM.nextInt(6));
        }
        haze(chest, Vec3.f_82478_, 1.4F * scale, 10);
    }

    /** A bursting spike exploding (Bursting Shards): a blast of crystal and red light {@code radius} wide. */
    private static void blast(Vec3 at, float radius) {
        RcFx.at(RcFx.RING, true, 9, at).bloom(0).colors(0xFFFFE0E8, RcFx.clear(GEM_CRIMSON)).size(0.4F, radius * 2.4F).spawn();
        for (int i = RcFx.count(20); i > 0; i--) {
            chip(at, sphere().m_82490_(0.2 + 0.3 * RANDOM.nextDouble()), 0.14F, 12 + RANDOM.nextInt(10));
        }
        for (int i = RcFx.count(3); i > 0; i--) {
            smoke(at.m_82549_(jitter(0.5)), new Vec3(0.0, 0.03, 0.0), 0.7F, 24 + RANDOM.nextInt(12));
        }
    }

    /** The wings freezing (glints racing up from the shoulder blades) or thawing (the crystal breaking back into embers). */
    private static void crystallize(Entity shooter, boolean freezing) {
        Vec3 back = shooter.m_20182_().m_82520_(0.0, 1.3, 0.0);
        for (int i = RcFx.count(16); i > 0; i--) {
            Vec3 at = back.m_82549_(jitter(0.9)).m_82520_(0.0, 0.3, 0.0);
            if (freezing) {
                glint(at, new Vec3(0.0, 0.05 + 0.05 * RANDOM.nextDouble(), 0.0).m_82549_(jitter(0.04)), 0.14F, 6 + RANDOM.nextInt(8));
            } else {
                chip(at, jitter(0.1).m_82520_(0.0, 0.06, 0.0), 0.1F, 10 + RANDOM.nextInt(8));
                ember(at, jitter(0.08).m_82520_(0.0, 0.05, 0.0), 0.05F, 14 + RANDOM.nextInt(10));
            }
        }
    }

    /** The hop's downbeat in the crystal mode: chips of crystal and red haze instead of fire. */
    private static void crystalHop(Vec3 feet, Vec3 back) {
        Vec3 forward = back.m_82490_(-1.0);
        for (int i = RcFx.count(10); i > 0; i--) {
            chip(feet.m_82520_(0.0, 0.9 + RANDOM.nextDouble() * 0.8, 0.0).m_82549_(jitter(0.6)), forward.m_82490_(0.1).m_82520_(0.0, -0.05, 0.0)
                .m_82549_(jitter(0.06)), 0.1F, 10 + RANDOM.nextInt(6));
        }
        haze(feet.m_82520_(0.0, 1.2, 0.0), Vec3.f_82478_, 1.0F, 8);
        smoke(feet.m_82520_(0.0, 0.2, 0.0), new Vec3(0.0, 0.03, 0.0), 0.5F, 20);
    }
    // ------------------------------------------------------------------ the Wing Vortex

    /** The wing tearing off the shoulder blade: a gout of fire and the pink flash where it leaves. */
    private static void vortexLaunch(Vec3 at, Vec3 velocity, float size) {
        Vec3 dir = velocity.m_82556_() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : velocity.m_82541_();
        for (int i = RcFx.count(18 * size); i > 0; i--) {
            flame(at.m_82549_(jitter(0.4)), dir.m_82490_(0.12 + 0.12 * RANDOM.nextDouble()).m_82549_(jitter(0.12)), (0.4F + 0.2F * RANDOM.nextFloat()) * size,
                8 + RANDOM.nextInt(6));
        }
        for (int i = RcFx.count(8); i > 0; i--) {
            ember(at, sphere().m_82490_(0.15).m_82549_(dir.m_82490_(0.1)), 0.06F, 14 + RANDOM.nextInt(10));
        }
        aura(at, Vec3.f_82478_, 1.4F * size, 6);
        RcFx.at(RcFx.RING, true, 8, at).bloom(0).colors(0xFFFF6A3A, RcFx.clear(RcFx.CRIMSON)).size(0.4F * size, 2.6F * size).spawn();
    }

    /** What the vortex sheds as it spins: fire flung off its whole spinning length, embers, the pink glow, smoke at the point. */
    private static void vortexTrail(Vortex vortex, Vec3 head, long now) {
        Vec3 root = vortex.root(now, 1.0F);
        Vec3 axis = head.m_82546_(root);
        double length = axis.m_82553_();
        Vec3 dir = length < 1.0E-6 ? (vortex.velocity.m_82556_() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : vortex.velocity.m_82541_()) : axis.m_82490_(1.0 / length);
        Vec3 helper = Math.abs(dir.f_82480_) < 0.95 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
        Vec3 u = dir.m_82537_(helper).m_82541_();
        Vec3 v = dir.m_82537_(u);
        float size = vortex.size;
        double spin = (now - vortex.born) / 20.0 * Math.PI * 2.0 * 3.2; // the vortex's own spin (WingVortex.SPIN turns a second)
        for (int i = RcFx.count((4 + (float)Math.min(10.0, length * 0.5)) * size); i > 0; i--) {
            double s = RANDOM.nextDouble();
            double angle = spin + RANDOM.nextDouble() * Math.PI * 2.0;
            double r = 0.9 * size * (s < 0.1 ? 0.4 : 1.0);
            Vec3 out = u.m_82490_(Math.cos(angle)).m_82549_(v.m_82490_(Math.sin(angle)));
            Vec3 at = root.m_82549_(axis.m_82490_(s)).m_82549_(out.m_82490_(r));
            // Flung off the spin: outward and along it.
            Vec3 tangent = dir.m_82537_(out).m_82490_(0.12);
            flame(at, out.m_82490_(0.06).m_82549_(tangent).m_82549_(dir.m_82490_(-0.04)), (0.35F + 0.2F * RANDOM.nextFloat()) * size, 7 + RANDOM.nextInt(5));
        }
        if (RANDOM.nextFloat() < 0.7F * RcFx.density()) {
            ember(head.m_82549_(jitter(0.8 * size)), jitter(0.1), 0.06F, 14 + RANDOM.nextInt(10));
        }
        if (RANDOM.nextFloat() < 0.5F) {
            aura(head.m_82546_(dir.m_82490_(1.2 * size)), Vec3.f_82478_, 1.1F * size, 4);
        }
        if (RANDOM.nextFloat() < 0.35F) {
            smoke(root.m_82549_(axis.m_82490_(RANDOM.nextDouble())), jitter(0.02), 0.6F * size, 22 + RANDOM.nextInt(10));
        }
    }

    /** The shoulder blade the vortex of player {@code shooter} grows from, or null if that player isn't here. */
    static Vec3 shoulderOf(int shooter, float partial) {
        ClientLevel level = Minecraft.m_91087_().f_91073_;
        if (level != null && level.m_6815_(shooter) instanceof net.minecraft.client.player.AbstractClientPlayer player) {
            int side = AyatoClient.vortexSide(player);
            return AyatoClient.shoulder(player, side == 0 ? 1 : side, partial);
        }
        return null;
    }

    /** The vortex catching a body: a burst of fire thrown along its flight, embers, a ring and red veins. */
    private static void vortexHit(Vec3 at, Vec3 heading, float size) {
        Vec3 dir = heading.m_82556_() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : heading.m_82541_();
        float scale = Math.max(1.0F, size * 1.3F);
        for (int i = RcFx.count(16 * scale); i > 0; i--) {
            flame(at, dir.m_82490_(0.15 + 0.2 * RANDOM.nextDouble()).m_82549_(sphere().m_82490_(0.12)), (0.35F + 0.15F * RANDOM.nextFloat()) * scale,
                8 + RANDOM.nextInt(8));
        }
        for (int i = RcFx.count(10); i > 0; i--) {
            ember(at, sphere().m_82490_(0.2).m_82549_(dir.m_82490_(0.15)), 0.06F, 16 + RANDOM.nextInt(12));
        }
        RcFx.at(RcFx.RING, true, 8, at).bloom(0).colors(0xFFFF6A3A, RcFx.clear(RcFx.CRIMSON)).size(0.3F * scale, 2.2F * scale).spawn();
        RcFx.at(RcFx.VEINS, true, 7, at).bloom(0).colors(0xFFFF2A3A, RcFx.clear(RcFx.CRIMSON)).size(0.8F * scale, 1.5F * scale).spawn();
        aura(at, Vec3.f_82478_, 0.9F * scale, 5);
    }

    /** Where the vortex ends: a last swirl of fire as it unwinds. */
    private static void vortexEnd(Vec3 at, Vec3 velocity, float size) {
        for (int i = RcFx.count(14 * size); i > 0; i--) {
            Vec3 way = sphere();
            flame(at.m_82549_(way.m_82490_(0.5 * size)), way.m_82490_(0.08), (0.4F + 0.2F * RANDOM.nextFloat()) * size, 9 + RANDOM.nextInt(6));
        }
        aura(at, Vec3.f_82478_, 1.6F * size, 7);
    }

    /** The detached, unwinding vortex shedding embers and smoke all along it as it fades. */
    private static void vortexUnwinding(Vortex vortex, Vec3 head) {
        float size = vortex.size;
        Vec3 root = vortex.endRoot != null ? vortex.endRoot : head;
        Vec3 axis = head.m_82546_(root);
        int count = RcFx.count((3 + (float)Math.min(8.0, axis.m_82553_() * 0.4)) * size);
        for (int i = count; i > 0; i--) {
            Vec3 at = root.m_82549_(axis.m_82490_(RANDOM.nextDouble()));
            ember(at.m_82549_(jitter(1.4 * size)), jitter(0.08).m_82520_(0.0, 0.04, 0.0), 0.06F, 14 + RANDOM.nextInt(10));
        }
        if (RANDOM.nextFloat() < 0.6F) {
            smoke(root.m_82549_(axis.m_82490_(RANDOM.nextDouble())).m_82549_(jitter(1.0 * size)), new Vec3(0.0, 0.03, 0.0), 0.7F * size,
                24 + RANDOM.nextInt(12));
        }
    }
}
