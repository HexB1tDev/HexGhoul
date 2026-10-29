package com.hex.kagune.control.client.fx;

import com.hex.kagune.KaguneMod;
import com.lowdragmc.photon.client.gameobject.emitter.data.material.TextureMaterial;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.NumberFunction;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.NumberFunction3;
import com.lowdragmc.photon.client.gameobject.emitter.particle.ParticleConfig;
import com.lowdragmc.photon.client.gameobject.emitter.particle.ParticleEmitter;
import com.lowdragmc.photon.client.gameobject.particle.TileParticle;
import com.mojang.blaze3d.platform.GlStateManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * RC particles, on Photon (the mod's own particle engine dependency), ported from Hex God of Death's {@code DeathFx}: no
 * vanilla particle types. A particle is a {@link Mote}: a sprite (the white shapes in {@code textures/particle}, taken from
 * Hex God of Death) tinted per particle, with drag and weight, wandering a little, fading between two colours as it grows
 * or shrinks. Emitters are pooled and capped and batched per tick and sprite, so a hundred motes in one tick share one
 * emitter. Everything here is client-only and purely visual.
 */
public final class RcFx {
    // The sprites (white, tinted by each mote).
    public static final ResourceLocation FLAME = KaguneMod.id("textures/particle/flame.png");
    public static final ResourceLocation EMBER = KaguneMod.id("textures/particle/ember.png");
    public static final ResourceLocation MOTE = KaguneMod.id("textures/particle/mote.png");
    public static final ResourceLocation PUFF = KaguneMod.id("textures/particle/puff.png");
    public static final ResourceLocation STREAK = KaguneMod.id("textures/particle/streak.png");
    public static final ResourceLocation RING = KaguneMod.id("textures/particle/ring.png");
    public static final ResourceLocation VEINS = KaguneMod.id("textures/particle/veins.png");
    /** 1.0.19: a white feather, a flake of ash, a slash's crescent, a four-pointed glint. */
    public static final ResourceLocation FEATHER = KaguneMod.id("textures/particle/feather.png");
    public static final ResourceLocation ASH = KaguneMod.id("textures/particle/ash.png");
    public static final ResourceLocation SLASH = KaguneMod.id("textures/particle/slash.png");
    public static final ResourceLocation SPARK = KaguneMod.id("textures/particle/spark.png");

    // Ayato's RC flame, the same palette the wings are drawn in (UkakuFlame): white-hot core, molten orange, red, crimson,
    // the pink aura at the rim, and the dark red-black of its smoke.
    public static final int CORE = 0xFFFFF0D8, MOLTEN = 0xFFFFB02A, FIRE = 0xFFFF3C12, RED = 0xFFE8121C, CRIMSON = 0xFF8E0A1E,
        AURA = 0xFFF2A2F0, PINK = 0xFFFF86C4, SMOKE = 0xB0240812, VEIN = 0xFFFF2236;

    private static final List<ParticleEmitter> ACTIVE = new ArrayList<>();
    /** Where each emitter stands (Photon keeps it to itself). */
    private static final Map<ParticleEmitter, Vec3> ORIGINS = new IdentityHashMap<>();
    private static final int MAX_EMITTERS = 256;
    private static final Map<String, ParticleEmitter> BATCH = new HashMap<>();
    private static long batchTick = Long.MIN_VALUE;

    private RcFx() {
    }

    public static int clear(int argb) {
        return argb & 0x00FFFFFF;
    }

    /** 1 with all particles, 0.5 decreased, 0.15 minimal (the client's own particle setting). */
    public static float density() {
        Minecraft minecraft = Minecraft.m_91087_();
        if (minecraft.f_91066_ == null) {
            return 1.0F;
        }
        Object status = minecraft.f_91066_.m_231929_().m_231551_();
        int level = status instanceof Enum<?> mode ? mode.ordinal() : 0; // ALL, DECREASED, MINIMAL
        return level == 0 ? 1.0F : level == 1 ? 0.5F : 0.15F;
    }

    /** How many of {@code wanted} to actually spawn, randomly rounded so low densities still show something. */
    public static int count(float wanted) {
        float scaled = wanted * density();
        int whole = (int)scaled;
        return whole + (Math.random() < scaled - whole ? 1 : 0);
    }

    /** True if {@code at} is within {@code range} blocks of the camera (callers cull far effects). */
    public static boolean near(Vec3 at, double range) {
        Minecraft minecraft = Minecraft.m_91087_();
        return minecraft.f_91063_ != null && minecraft.f_91063_.m_109153_().m_90583_().m_82557_(at) < range * range;
    }

    /** A new emitter at {@code at} whose particles live about {@code lifetime} ticks, glowing (additive) or blended. */
    private static ParticleEmitter emitter(Vec3 at, int lifetime, ResourceLocation texture, boolean glow) {
        ParticleEmitter emitter = new ParticleEmitter();
        ParticleConfig config = emitter.config;
        config.setDuration(1);
        config.setLooping(false);
        config.setMaxParticles(2048);
        config.setParallelUpdate(false);
        config.setParallelRendering(false);
        config.emission.setEmissionRate(NumberFunction.constant(0));
        config.setStartLifetime(NumberFunction.constant(lifetime));
        config.setStartSpeed(NumberFunction.constant(0));
        config.setStartSize(new NumberFunction3(NumberFunction.constant(1), NumberFunction.constant(1), NumberFunction.constant(1)));
        config.physics.setEnable(false);
        config.material.setMaterial(new TextureMaterial(texture));
        config.material.setCull(false);
        config.material.setDepthTest(true);
        config.material.setDepthMask(false);
        if (glow) {
            config.material.getBlendMode().setDstColorFactor(GlStateManager.DestFactor.ONE);
        }
        config.renderer.setBloomEffect(glow);
        config.renderer.getCull().setEnable(false);
        register(emitter, at);
        return emitter;
    }

    /** This tick's shared emitter for one sprite: positions given to {@link Mote#world} are made relative to it. */
    public static ParticleEmitter batch(Vec3 near, int lifetime, ResourceLocation texture, boolean glow) {
        Minecraft minecraft = Minecraft.m_91087_();
        long now = minecraft.f_91073_ == null ? 0L : minecraft.f_91073_.m_46467_();
        if (now != batchTick) {
            BATCH.clear();
            batchTick = now;
        }
        String key = texture + (glow ? "+" : "-");
        ParticleEmitter emitter = BATCH.get(key);
        if (emitter == null || !emitter.m_107276_()) {
            emitter = emitter(near, lifetime, texture, glow);
            BATCH.put(key, emitter);
        }
        return emitter;
    }

    private static void register(ParticleEmitter emitter, Vec3 at) {
        Minecraft minecraft = Minecraft.m_91087_();
        if (minecraft.f_91073_ == null) {
            return;
        }
        ACTIVE.removeIf(each -> {
            boolean dead = !each.m_107276_();
            if (dead) {
                ORIGINS.remove(each);
            }
            return dead;
        });
        while (ACTIVE.size() >= MAX_EMITTERS) {
            ParticleEmitter oldest = ACTIVE.remove(0);
            ORIGINS.remove(oldest);
            oldest.remove(true);
        }
        emitter.setLevel(minecraft.f_91073_);
        emitter.m_107264_(at.f_82479_, at.f_82480_, at.f_82481_);
        ORIGINS.put(emitter, at);
        ACTIVE.add(emitter);
        minecraft.f_91061_.m_107344_(emitter);
    }

    /** Drops every emitter (leaving a world). */
    public static void reset() {
        for (ParticleEmitter emitter : ACTIVE) {
            emitter.remove(true);
        }
        ACTIVE.clear();
        ORIGINS.clear();
        BATCH.clear();
    }

    /** 6.1.0: where a pooled emitter stands (the origin its particles' local positions are measured from). */
    public static Vec3 origin(ParticleEmitter emitter) {
        return ORIGINS.getOrDefault(emitter, Vec3.f_82478_);
    }

    public static Mote mote(ParticleEmitter emitter) {
        return new Mote(emitter);
    }

    /** One particle of {@code texture} at {@code at} in this tick's batch (glowing = additive). */
    public static Mote at(ResourceLocation texture, boolean glow, int lifetime, Vec3 at) {
        return mote(batch(at, lifetime, texture, glow)).world(at).life(lifetime);
    }

    /**
     * A particle as a small body: slowed by drag, pulled down (or up) by its weight, wandering sideways, fading over its life
     * from one colour to another while it grows or shrinks, opening over its first ticks.
     */
    public static final class Mote extends TileParticle {
        private final ParticleEmitter owner;
        private float drag = 0.96F;
        private float gravity;
        private float jitter;
        private int from = 0xFFFFFFFF;
        private int to = 0x00FFFFFF;
        private float size0 = 0.2F;
        private float size1 = 0.1F;
        private float bloom = 2.0F;

        Mote(ParticleEmitter emitter) {
            super(emitter, emitter.config, emitter.getRandomSource());
            this.owner = emitter;
        }

        /** Position relative to the emitter. */
        public Mote local(Vec3 local) {
            this.setLocalPos(new Vector3f((float)local.f_82479_, (float)local.f_82480_, (float)local.f_82481_), true);
            return this;
        }

        /** Position in the world (the batch emitter is shared by many places). */
        public Mote world(Vec3 world) {
            Vec3 origin = ORIGINS.getOrDefault(this.owner, Vec3.f_82478_);
            return this.local(world.m_82546_(origin));
        }

        /** Velocity in blocks per tick. */
        public Mote velocity(Vec3 velocity) {
            this.setInternalVelocity(new Vector3f((float)velocity.f_82479_, (float)velocity.f_82480_, (float)velocity.f_82481_));
            return this;
        }

        public Mote colors(int from, int to) {
            this.from = from;
            this.to = to;
            this.setARGBColor(from);
            return this;
        }

        public Mote size(float from, float to) {
            this.size0 = from;
            this.size1 = to;
            this.setSize(new Vector3f(from, from, from));
            return this;
        }

        public Mote life(int ticks) {
            this.setLifetime(Math.max(1, ticks));
            return this;
        }

        public Mote drag(float drag) {
            this.drag = drag;
            return this;
        }

        /** Weight in blocks per tick per tick, positive down (negative rises, as flame does). */
        public Mote fall(float gravity) {
            this.gravity = gravity;
            return this;
        }

        /** Wanders sideways a little each tick (flame licking, embers drifting). */
        public Mote wander(float jitter) {
            this.jitter = jitter;
            return this;
        }

        /** Ticks to open in (0: simply there, like a spark). */
        public Mote bloom(float ticks) {
            this.bloom = ticks;
            return this;
        }

        public void spawn() {
            this.owner.emitParticle(this);
        }

        @Override
        protected void update() {
            this.velocityY -= this.gravity;
            if (this.jitter != 0.0F) {
                this.velocityX += (this.randomSource.m_188501_() - 0.5F) * this.jitter;
                this.velocityY += (this.randomSource.m_188501_() - 0.5F) * this.jitter * 0.5F;
                this.velocityZ += (this.randomSource.m_188501_() - 0.5F) * this.jitter;
            }
            this.velocityX *= this.drag;
            this.velocityY *= this.drag;
            this.velocityZ *= this.drag;
            float t = this.lifetime > 0 ? Math.min(1.0F, this.age / (float)this.lifetime) : 0.0F;
            float a0 = ((this.from >>> 24) & 255) / 255.0F;
            float a1 = ((this.to >>> 24) & 255) / 255.0F;
            float open = this.bloom <= 0.0F ? 1.0F : Math.min(1.0F, this.age / this.bloom);
            this.setColor(new Vector4f(
                lerp(((this.from >> 16) & 255) / 255.0F, ((this.to >> 16) & 255) / 255.0F, t),
                lerp(((this.from >> 8) & 255) / 255.0F, ((this.to >> 8) & 255) / 255.0F, t),
                lerp((this.from & 255) / 255.0F, (this.to & 255) / 255.0F, t),
                lerp(a0, a1, t) * open * open * (3.0F - 2.0F * open)));
            float size = lerp(this.size0, this.size1, t);
            this.setSize(new Vector3f(size, size, size));
            super.update();
        }

        private static float lerp(float a, float b, float t) {
            return a + (b - a) * t;
        }
    }
}
