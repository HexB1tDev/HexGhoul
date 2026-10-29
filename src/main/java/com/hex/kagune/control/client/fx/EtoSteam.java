package com.hex.kagune.control.client.fx;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.client.KaguneClientState;
import com.hex.kagune.control.client.EtoClient;
import com.hex.kagune.control.client.animation.KaguneBodyAnimation;
import com.hex.kagune.control.eto.EtoSteamRules;
import com.lowdragmc.photon.client.gameobject.emitter.particle.ParticleEmitter;
import com.lowdragmc.photon.client.gameobject.particle.TileParticle;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

/**
 * Eto's base form (6.1.0): light steam rising off her body, on Photon. Each wisp is a small volume, not a sprite turned to the
 * camera: three soft planes crossed at right angles, tumbling slowly as the wisp rises, each as opaque as it squarely faces the
 * eye (the squares of the three facings always add up to one, so the wisp is equally faint from every angle and never shows a
 * card edge-on). Pale whitish grey and barely there, they leave her shoulders, upper back, arms, chest and now and then her legs
 * ({@link EtoSteamRules}), curl and spread as they climb and fade away. They ride her body (its lean and twist) and drift with
 * her as she moves; the emission eases in as the form wakes and out when it ends, the wisps already out living out their time.
 *
 * <p>Everything is computed on each client from what it already knows (her path and that the kagune is out): no packets. A few
 * dozen quads per Eto in view, culled beyond {@link EtoSteamRules#RANGE}, scaled by the client's particle setting.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class EtoSteam {
    public static final ResourceLocation STEAM = KaguneMod.id("textures/particle/steam.png");
    private static final Map<UUID, Source> SOURCES = new HashMap<>();
    private static final java.util.Random RANDOM = new java.util.Random();

    /**
     * How much harder she steams now: charging Big Punch the steam pours off her, more the fuller the charge (up to
     * {@link #SURGE} times as much).
     */
    static final float SURGE = 4.0F;

    private static float surge(net.minecraft.world.entity.player.Player player, double time) {
        EtoClient.EtoView view = EtoClient.get(player);
        if (view.move() != com.hex.kagune.control.eto.EtoMove.PUNCH_CHARGE) {
            return 1.0F;
        }
        double charge = Math.min(1.0, view.age(EtoClient.clock(0.0F)) / com.hex.kagune.control.eto.EtoRules.PUNCH_CHARGE_TICKS);
        return 1.0F + (SURGE - 1.0F) * (float)charge;
    }

    /** One Eto's emission: how awake it is (eases in and out) and the part of a wisp owed from earlier ticks. */
    private static final class Source {
        float strength;
        float owed;
        long seen;
    }

    private EtoSteam() {
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        ClientLevel level = minecraft.f_91073_;
        if (level == null || minecraft.m_91104_() || minecraft.f_91063_ == null) {
            SOURCES.clear();
            return;
        }
        Vec3 camera = minecraft.f_91063_.m_109153_().m_90583_();
        boolean firstPersonView = minecraft.f_91066_.m_92176_().m_90612_();
        double time = level.m_46467_();
        long now = level.m_46467_();
        for (AbstractClientPlayer player : level.m_6907_()) {
            UUID id = player.m_20148_();
            boolean on = EtoClient.isEto(player) && KaguneClientState.get(player).active() && player.m_6084_() && !player.m_20145_()
                && !player.m_5833_();
            Source source = SOURCES.get(id);
            if (source == null) {
                if (!on) {
                    continue;
                }
                source = new Source();
                SOURCES.put(id, source);
            }
            source.seen = now;
            source.strength = EtoSteamRules.ease(source.strength, on);
            if (source.strength <= 0.0F) {
                SOURCES.remove(id);
                continue;
            }
            if (player.m_20182_().m_82557_(camera) > EtoSteamRules.RANGE * EtoSteamRules.RANGE) {
                continue;
            }
            boolean firstPerson = player == minecraft.f_91074_ && firstPersonView;
            double speed = Math.hypot(player.m_20185_() - player.f_19790_, player.m_20189_() - player.f_19792_);
            source.owed += EtoSteamRules.rate(source.strength, speed, firstPerson) * RcFx.density() * surge(player, time);
            while (source.owed >= 1.0F) {
                source.owed -= 1.0F;
                emit(player, firstPerson, time);
            }
        }
        if (!SOURCES.isEmpty()) {
            // Someone who left the world (or this client's view of it) stops steaming.
            SOURCES.values().removeIf(source -> source.seen != now);
        }
    }

    /** One wisp off a point of her body. */
    private static void emit(AbstractClientPlayer player, boolean firstPerson, double time) {
        java.util.Random random = RANDOM;
        EtoSteamRules.Point point = EtoSteamRules.pick(random.nextDouble(), firstPerson);
        float[] torso = KaguneBodyAnimation.torso(player, time);
        double bodyYaw = Math.toRadians(player.f_20883_);
        double[] rig = EtoSteamRules.place(point, torso[0] + (player.m_6047_() ? 0.45F : 0.0F), torso[1], torso[2],
            player.m_6047_() ? 0.82 : 1.0, random.nextGaussian() * 0.05, random.nextGaussian() * 0.05, random.nextGaussian() * 0.05);
        // Rig space (x her right, y up, z forward) to the world.
        double forwardX = -Math.sin(bodyYaw);
        double forwardZ = Math.cos(bodyYaw);
        double rightX = -forwardZ;
        double rightZ = forwardX;
        Vec3 at = new Vec3(player.m_20185_() + rightX * rig[0] + forwardX * rig[2], player.m_20186_() + rig[1],
            player.m_20189_() + rightZ * rig[0] + forwardZ * rig[2]);
        if (firstPerson && at.m_82557_(player.m_146892_()) < EtoSteamRules.FIRST_PERSON_CLEARANCE * EtoSteamRules.FIRST_PERSON_CLEARANCE) {
            return; // never in front of her own eyes
        }
        Vec3 motion = new Vec3(player.m_20185_() - player.f_19790_, player.m_20186_() - player.f_19791_, player.m_20189_() - player.f_19792_);
        double outX = rightX * point.x() + forwardX * point.z();
        double outZ = rightZ * point.x() + forwardZ * point.z();
        double out = Math.max(1.0E-4, Math.hypot(outX, outZ));
        Vec3 velocity = new Vec3(motion.f_82479_ * EtoSteamRules.INHERIT + outX / out * EtoSteamRules.OUTWARD,
            EtoSteamRules.RISE_MIN + random.nextDouble() * (EtoSteamRules.RISE_MAX - EtoSteamRules.RISE_MIN) + Math.max(0.0, motion.f_82480_) * 0.4,
            motion.f_82481_ * EtoSteamRules.INHERIT + outZ / out * EtoSteamRules.OUTWARD);
        int life = EtoSteamRules.LIFE_MIN + random.nextInt(EtoSteamRules.LIFE_MAX - EtoSteamRules.LIFE_MIN + 1);
        ParticleEmitter emitter = RcFx.batch(at, life, STEAM, false);
        new Wisp(emitter, at, velocity, life, random).spawn();
    }

    /**
     * A wisp: three soft planes crossed at right angles round its centre, tumbling, growing and fading as it rises and curls.
     * Its own alpha, size and turn are kept here (previous and current tick) and drawn interpolated.
     */
    static final class Wisp extends TileParticle {
        private final ParticleEmitter owner;
        private final float spin0, spin1, spin2;
        private final float curl;
        private final int uvTurn;
        private final float peak;
        private final float scale;
        private float angle0, angle1, angle2;
        private float lastAngle0, lastAngle1, lastAngle2;
        private float size;
        private float lastSize;
        private float alpha;
        private float lastAlpha;
        private float phase;

        Wisp(ParticleEmitter emitter, Vec3 at, Vec3 velocity, int life, java.util.Random random) {
            this(emitter, at, velocity, life, random, 1.0F, EtoSteamRules.PEAK_ALPHA);
        }

        /** 6.2.0: a wisp {@code scale} times the size, as thick as {@code peak} at most (the vapour she turns to in the Speed Kick). */
        Wisp(ParticleEmitter emitter, Vec3 at, Vec3 velocity, int life, java.util.Random random, float scale, float peak) {
            super(emitter, emitter.config, emitter.getRandomSource());
            this.owner = emitter;
            Vec3 origin = RcFx.origin(emitter);
            this.setLocalPos(new Vector3f((float)(at.f_82479_ - origin.f_82479_), (float)(at.f_82480_ - origin.f_82480_),
                (float)(at.f_82481_ - origin.f_82481_)), true);
            this.setInternalVelocity(new Vector3f((float)velocity.f_82479_, (float)velocity.f_82480_, (float)velocity.f_82481_));
            this.setLifetime(life);
            this.setARGBColor(EtoSteamRules.COLOUR);
            this.angle0 = this.lastAngle0 = random.nextFloat() * 6.2832F;
            this.angle1 = this.lastAngle1 = random.nextFloat() * 6.2832F;
            this.angle2 = this.lastAngle2 = random.nextFloat() * 6.2832F;
            this.spin0 = (random.nextFloat() - 0.5F) * EtoSteamRules.TUMBLE;
            this.spin1 = (random.nextFloat() - 0.5F) * EtoSteamRules.TUMBLE;
            this.spin2 = (random.nextFloat() - 0.5F) * EtoSteamRules.TUMBLE;
            this.curl = (random.nextBoolean() ? 1.0F : -1.0F) * (0.6F + 0.8F * random.nextFloat());
            this.phase = random.nextFloat() * 6.2832F;
            this.uvTurn = random.nextInt(8);
            this.peak = peak * (0.75F + 0.5F * random.nextFloat());
            this.scale = scale;
            this.size = this.lastSize = EtoSteamRules.size(0.0F) * scale;
        }

        void spawn() {
            this.owner.emitParticle(this);
        }

        @Override
        protected void update() {
            float t = this.lifetime > 0 ? Math.min(1.0F, this.age / (float)this.lifetime) : 1.0F;
            this.lastAngle0 = this.angle0;
            this.lastAngle1 = this.angle1;
            this.lastAngle2 = this.angle2;
            this.lastSize = this.size;
            this.lastAlpha = this.alpha;
            this.angle0 += this.spin0;
            this.angle1 += this.spin1;
            this.angle2 += this.spin2;
            this.size = EtoSteamRules.size(t) * this.scale;
            this.alpha = this.peak * EtoSteamRules.opacity(t, this.age);
            // Vapour: slowed by the air, buoyant, curling round as it climbs, wandering a little.
            this.phase += EtoSteamRules.CURL_RATE;
            this.velocityX = this.velocityX * EtoSteamRules.DRAG + (float)Math.cos(this.phase) * EtoSteamRules.CURL * this.curl;
            this.velocityZ = this.velocityZ * EtoSteamRules.DRAG + (float)Math.sin(this.phase) * EtoSteamRules.CURL * this.curl;
            this.velocityY = this.velocityY * EtoSteamRules.DRAG + EtoSteamRules.BUOYANCY;
            this.velocityX += (this.randomSource.m_188501_() - 0.5F) * EtoSteamRules.WANDER;
            this.velocityZ += (this.randomSource.m_188501_() - 0.5F) * EtoSteamRules.WANDER;
            super.update();
        }

        @Override
        public void renderInternal(VertexConsumer buffer, Camera camera, float partialTicks) {
            float alpha = this.lastAlpha + (this.alpha - this.lastAlpha) * partialTicks;
            if (alpha <= 0.002F) {
                return;
            }
            Vec3 eye = camera.m_90583_();
            Vector3f world = this.getWorldPos(partialTicks);
            float x = (float)(world.x - eye.f_82479_);
            float y = (float)(world.y - eye.f_82480_);
            float z = (float)(world.z - eye.f_82481_);
            float size = this.lastSize + (this.size - this.lastSize) * partialTicks;
            float[] axes = EtoSteamRules.axes(this.lastAngle0 + (this.angle0 - this.lastAngle0) * partialTicks,
                this.lastAngle1 + (this.angle1 - this.lastAngle1) * partialTicks, this.lastAngle2 + (this.angle2 - this.lastAngle2) * partialTicks);
            float distance = (float)Math.sqrt(x * x + y * y + z * z);
            float vx = distance < 1.0E-4F ? 0.0F : x / distance;
            float vy = distance < 1.0E-4F ? 1.0F : y / distance;
            float vz = distance < 1.0E-4F ? 0.0F : z / distance;
            // Up close (inside the wisp) it fades out instead of filling the view.
            float near = Math.min(1.0F, Math.max(0.0F, (distance - size * 0.6F) / (size * 1.4F + 0.2F)));
            int light = this.getRealLight(partialTicks);
            int red = (EtoSteamRules.COLOUR >> 16) & 0xFF;
            int green = (EtoSteamRules.COLOUR >> 8) & 0xFF;
            int blue = EtoSteamRules.COLOUR & 0xFF;
            for (int plane = 0; plane < 3; plane++) {
                // The plane's normal is axis `plane`; it spans the other two.
                int n = plane * 3;
                float facing = axes[n] * vx + axes[n + 1] * vy + axes[n + 2] * vz;
                float share = facing * facing;
                int a = Math.round(255.0F * alpha * share * near);
                if (a <= 0) {
                    continue;
                }
                int u = ((plane + 1) % 3) * 3;
                int w = ((plane + 2) % 3) * 3;
                float ux = axes[u] * size;
                float uy = axes[u + 1] * size;
                float uz = axes[u + 2] * size;
                float wx = axes[w] * size;
                float wy = axes[w + 1] * size;
                float wz = axes[w + 2] * size;
                int turn = (this.uvTurn + plane * 3) & 7;
                corner(buffer, x - ux - wx, y - uy - wy, z - uz - wz, 0, turn, red, green, blue, a, light);
                corner(buffer, x - ux + wx, y - uy + wy, z - uz + wz, 1, turn, red, green, blue, a, light);
                corner(buffer, x + ux + wx, y + uy + wy, z + uz + wz, 2, turn, red, green, blue, a, light);
                corner(buffer, x + ux - wx, y + uy - wy, z + uz - wz, 3, turn, red, green, blue, a, light);
            }
        }

        /** One corner of a plane; {@code turn} (0..7) turns and flips the texture so the three planes never repeat. */
        private static void corner(VertexConsumer buffer, float x, float y, float z, int corner, int turn, int red, int green, int blue,
                                   int alpha, int light) {
            int c = (corner + (turn & 3)) & 3;
            float u = c == 0 || c == 1 ? 0.0F : 1.0F;
            float v = c == 0 || c == 3 ? 1.0F : 0.0F;
            if ((turn & 4) != 0) {
                u = 1.0F - u;
            }
            buffer.m_5483_(x, y, z).m_7421_(u, v).m_6122_(red, green, blue, alpha).m_85969_(light).m_5752_();
        }
    }
}
