package com.hex.kagune.control.client.fx;

import com.hex.kagune.control.client.PaleClient;
import com.hex.kagune.control.pale.KickTumble;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;

/**
 * 6.1.0: a body Pale hits reels (every client): tipped over its feet the way the blow drove it, in a tick, then back past upright
 * and settling, like a spring; the harder the blow, the further. Purely how it is drawn: where it goes is the server's knockback.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class PaleReactions {
    /** A blow: which way it drove (horizontal, unit), how hard (a strike's damage multiplier), when (PaleClient's clock). */
    record Reel(double x, double z, float strength, double at) {
    }

    /** How long a reel lasts (ticks), and how far a blow of strength 1 tips the body (radians). */
    static final double LENGTH = 16.0;
    static final double TIP = 0.3;

    private static final Map<Integer, Reel> REELS = new HashMap<>();

    /**
     * 6.2.0: a body the Speed Kick launched ({@link KickTumble}): which way it flies (level, unit), when the foot landed and when
     * it came down (PaleClient's clock; NaN: still in the air).
     */
    static final class Kicked {
        final double x;
        final double z;
        final double at;
        double landed = Double.NaN;
        /** Which way round it goes (+1: to its right), fixed on the first frame it is drawn (its AI may turn it in the air). */
        float side;
        /** 6.2.2: caught in Witness This: it turns over slower ({@link KickTumble#WITNESS_SLOW}). */
        boolean witness;

        Kicked(double x, double z, double at) {
            this.x = x;
            this.z = z;
            this.at = at;
        }
    }

    private static final Map<Integer, Kicked> KICKED = new HashMap<>();
    /** 6.2.0: how long (ticks) a kicked body trails torn air and dust behind it while it flies, and how much a tick at first. */
    static final double TRAIL_TICKS = 16.0;
    static final float TRAIL = 6.0F;
    private static final java.util.Random RANDOM = new java.util.Random();
    /** The heads turned this frame: each body's own head turn and pitch (now and last tick) before, put back after it is drawn. */
    private static final Map<LivingEntity, float[]> HEADS = new IdentityHashMap<>();
    /** Bodies turned this frame, untilted again after they are drawn. */
    private static final Set<LivingEntity> TURNED = Collections.newSetFromMap(new IdentityHashMap<>());

    private PaleReactions() {
    }

    /** The body {@code entityId} was hit, driven along {@code push}. */
    public static void hit(int entityId, Vec3 push, float strength, double time) {
        double length = Math.sqrt(push.f_82479_ * push.f_82479_ + push.f_82481_ * push.f_82481_);
        if (entityId == 0 || length < 1.0E-4) {
            return;
        }
        REELS.put(entityId, new Reel(push.f_82479_ / length, push.f_82481_ / length, strength, time));
    }

    /** 6.2.0: the body {@code entityId} was kicked off its feet, launched along {@code launch}. */
    public static void kicked(int entityId, Vec3 launch, double time) {
        double length = Math.sqrt(launch.f_82479_ * launch.f_82479_ + launch.f_82481_ * launch.f_82481_);
        if (entityId == 0 || length < 1.0E-4) {
            return;
        }
        REELS.remove(entityId);
        KICKED.put(entityId, new Kicked(launch.f_82479_ / length, launch.f_82481_ / length, time));
    }

    /** How far (radians, + the way it was hit) a body is tipped {@code t} ticks after a blow of {@code strength}. */
    static double tilt(double t, float strength) {
        if (t < 0.0 || t > LENGTH) {
            return 0.0;
        }
        double peak = TIP * Math.min(1.7, Math.max(0.4, strength));
        if (t < 1.0) {
            double u = t;
            return peak * u * u * (3.0 - 2.0 * u);
        }
        // A spring: back past upright a little, then still.
        double u = t - 1.0;
        return peak * Math.exp(-u * 0.32) * Math.cos(u * 0.62) * (1.0 - u / (LENGTH - 1.0));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void before(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity entity = event.getEntity();
        Kicked kicked = KICKED.get(entity.m_19879_());
        if (kicked != null) {
            tumble(event, entity, kicked);
            return;
        }
        Reel reel = REELS.get(entity.m_19879_());
        if (reel == null) {
            return;
        }
        double since = PaleClient.clock(event.getPartialTick()) - reel.at();
        if (since > LENGTH || since < -2.0) {
            REELS.remove(entity.m_19879_());
            return;
        }
        double angle = tilt(since, reel.strength());
        if (Math.abs(angle) < 1.0E-3) {
            return;
        }
        PoseStack stack = event.getPoseStack();
        stack.m_85836_();
        // About the level line square to the blow, through the feet: the top goes the way it was driven.
        stack.m_252781_(new Quaternionf().rotateAxis((float)angle, (float)reel.z(), 0.0F, (float)-reel.x()));
        TURNED.add(entity);
    }

    /**
     * A kicked body as {@link KickTumble} has it this frame: its head snapped round the way it was driven (its own head turn, so
     * every model's head goes), the whole body twisted about its own length and tipped over the way it flies, about its middle in
     * the air and, lying, so that it lies on the ground.
     */
    private static void tumble(RenderLivingEvent.Pre<?, ?> event, LivingEntity entity, Kicked kicked) {
        double time = PaleClient.clock(event.getPartialTick());
        double landed = Double.isNaN(kicked.landed) ? Double.NaN : kicked.landed - kicked.at;
        double[] witness = PaleClient.witnessOn(entity.m_19879_(), event.getPartialTick());
        if (witness != null) {
            kicked.witness = true;
        }
        double t = time - kicked.at;
        if (kicked.witness) {
            // 6.2.2: slower over in the air, so every blow of the storm reads; down and up again at the usual pace.
            double s = KickTumble.WITNESS_SLOW;
            t = Double.isNaN(landed) || t < landed ? t * s : landed * s + (t - landed);
            landed = Double.isNaN(landed) ? Double.NaN : landed * s;
        }
        KickTumble.Frame frame = KickTumble.at(t, landed, lie(kicked));
        // Which way round: toward where it flies, from where it faces.
        float bodyYaw = entity.f_20884_ + wrap(entity.f_20883_ - entity.f_20884_) * event.getPartialTick();
        if (kicked.side == 0.0F) {
            float flying = (float)Math.toDegrees(Math.atan2(-kicked.x, kicked.z));
            kicked.side = wrap(flying - bodyYaw) >= 0.0F ? 1.0F : -1.0F;
        }
        float side = kicked.side;
        float headYaw = side * (float)Math.toDegrees(frame.head());
        float headPitch = -(float)Math.toDegrees(frame.pitch());
        // 6.2.2: each blow of Witness This jolts it: the body snapped over the way it was struck, the head whipped round.
        double[] jolt = witness == null ? null : jolt(witness);
        if (jolt != null) {
            headYaw += (float)jolt[3];
            headPitch += (float)jolt[4];
        }
        if (!HEADS.containsKey(entity)) {
            HEADS.put(entity, new float[] {entity.f_20885_, entity.f_20886_, entity.m_146909_(), entity.f_19860_});
        }
        // 6.2.3: a rag doll's head is its body's: square on it (never its own look at whoever it is watching, which jittered as its
        // AI or its player turned it), turned and thrown only by the blows, this frame and last alike so it never smears.
        float yaw = bodyYaw + headYaw;
        float pitch = Math.max(-HEAD_PITCH_MAX, Math.min(HEAD_PITCH_MAX, headPitch));
        entity.f_20885_ = yaw;
        entity.f_20886_ = yaw;
        entity.f_19860_ = pitch;
        entity.m_146926_(pitch);
        double pivot = entity.m_20206_() * 0.5 * (1.0 - frame.flat()) + entity.m_20205_() * 0.5 * frame.flat();
        PoseStack stack = event.getPoseStack();
        stack.m_85836_();
        stack.m_85837_(0.0, pivot, 0.0);
        if (jolt != null && Math.abs(jolt[0]) > 1.0E-4) {
            stack.m_252781_(new Quaternionf().rotateAxis((float)jolt[0], (float)jolt[1], 0.0F, (float)jolt[2]));
        }
        if (jolt != null && Math.abs(jolt[5]) > 1.0E-3) {
            // 6.2.3: crushed along the line of the blow, then flung out long: turned so the blow is up, scaled, turned back.
            double tip = Math.acos(Math.max(-1.0, Math.min(1.0, jolt[7])));
            double ax = jolt[8];
            double az = -jolt[6];
            double len = Math.sqrt(ax * ax + az * az);
            float squash = (float)jolt[5];
            float bulge = 1.0F + squash * 0.5F;
            if (len > 1.0E-4) {
                stack.m_252781_(new Quaternionf().rotateAxis((float)tip, (float)(ax / len), 0.0F, (float)(az / len)));
                stack.m_85841_(bulge, 1.0F - squash, bulge);
                stack.m_252781_(new Quaternionf().rotateAxis((float)-tip, (float)(ax / len), 0.0F, (float)(az / len)));
            } else {
                stack.m_85841_(bulge, 1.0F - squash, bulge);
            }
        }
        stack.m_252781_(new Quaternionf().rotateAxis((float)frame.tumble(), (float)kicked.z, 0.0F, (float)-kicked.x));
        // (The model's own turn to its facing comes after this: a turn here turns it the opposite way to its yaw.)
        stack.m_252781_(new Quaternionf().rotateAxis((float)(-side * frame.twist()), 0.0F, 1.0F, 0.0F));
        stack.m_85837_(0.0, -pivot, 0.0);
        TURNED.add(entity);
    }

    /** 6.2.3: how long a kicked body lies there once down: in Witness This's crater, stunned, far longer. */
    private static double lie(Kicked kicked) {
        return kicked.witness ? com.hex.kagune.control.pale.WitnessRules.DOWN_TICKS : com.hex.kagune.control.pale.PaleRules.KICK_DOWN_TICKS;
    }

    /** How far a Witness This blow tips the body over (radians, at its height) and whips its head round and back (degrees). */
    static final double JOLT_TIP = 0.85;
    static final double JOLT_HEAD = 45.0;
    static final double JOLT_CHIN = 30.0;
    /** 6.2.3: a rag doll's head is never pitched further than this (degrees) up or down. */
    static final float HEAD_PITCH_MAX = 70.0F;

    /**
     * 6.2.2: the jolt of the last Witness This blow on a body ({@code witness}: the combo's age and yaw): {tip (radians), the tip's
     * axis x, z (world, level), the head's extra turn and pitch (degrees)}; null between blows.
     */
    private static double[] jolt(double[] witness) {
        double age = witness[0];
        int hit = -1;
        for (int i = 0; i < com.hex.kagune.control.pale.WitnessRules.HITS; i++) {
            if (age >= com.hex.kagune.control.pale.WitnessRules.hitAt(i)) {
                hit = i;
            }
        }
        double[] k;
        double since;
        double stop = 0.8;
        double power = 1.0;
        if (age >= com.hex.kagune.control.pale.WitnessRules.DROP) {
            k = new double[] {0.0, -1.0, 0.0};
            since = age - com.hex.kagune.control.pale.WitnessRules.DROP;
            power = 1.4;
        } else if (hit >= 0) {
            k = com.hex.kagune.control.pale.WitnessRules.knock(hit);
            since = age - com.hex.kagune.control.pale.WitnessRules.hitAt(hit);
            stop = com.hex.kagune.control.pale.WitnessRules.hitStop(hit);
            power = Math.min(1.5, com.hex.kagune.control.pale.WitnessRules.power(hit));
        } else if (age >= com.hex.kagune.control.pale.WitnessRules.UPPER) {
            k = new double[] {0.0, 1.0, 0.0};
            since = age - com.hex.kagune.control.pale.WitnessRules.UPPER;
        } else {
            return null;
        }
        double e = KickTumble.jolt(since, stop) * Math.min(1.3, 0.6 + 0.4 * power);
        double squash = KickTumble.squash(since, stop) * Math.min(1.3, 0.7 + 0.3 * power);
        if (e < 1.0E-3 && Math.abs(squash) < 1.0E-3) {
            return null;
        }
        double yaw = Math.toRadians(witness[1]);
        double fx = -Math.sin(yaw);
        double fz = Math.cos(yaw);
        // The knock's level part in the world (x her right, z the way the punch went).
        double wx = -fz * k[0] + fx * k[2];
        double wz = fx * k[0] + fz * k[2];
        double level = Math.sqrt(wx * wx + wz * wz);
        double whip = (hit % 2 == 0 ? 1.0 : -1.0) * JOLT_HEAD * e;
        double chin = -k[1] * JOLT_CHIN * e;
        // {tip, its axis x, z, head turn, head pitch, squash, the blow's line in the world x, y, z}.
        double wy = k[1];
        double norm = Math.sqrt(wx * wx + wy * wy + wz * wz);
        double lx = norm < 1.0E-6 ? 0.0 : wx / norm;
        double ly = norm < 1.0E-6 ? 1.0 : wy / norm;
        double lz = norm < 1.0E-6 ? 0.0 : wz / norm;
        if (level < 0.2) {
            // Straight up or down: folded over the way it faces, the head thrown.
            return new double[] {JOLT_TIP * 0.6 * e * Math.signum(k[1] == 0.0 ? 1.0 : -k[1]), 1.0, 0.0, whip, chin, squash, lx, ly, lz};
        }
        return new double[] {JOLT_TIP * e * level, wz / level, -wx / level, whip, chin, squash, lx, ly, lz};
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void after(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        restore(entity, HEADS.remove(entity));
        if (TURNED.remove(entity)) {
            event.getPoseStack().m_85849_();
        }
    }

    /**
     * 6.2.0: a body the Speed Kick sent flying trails what it tears through: pale streaks of air left behind it the way it came and a
     * puff of dust off it, thinning out as it slows.
     */
    private static void trail(LivingEntity body, Kicked kicked, double since) {
        float fade = (float)(1.0 - since / TRAIL_TICKS);
        Vec3 middle = body.m_20182_().m_82520_(0.0, body.m_20206_() * 0.5, 0.0);
        Vec3 back = new Vec3(-kicked.x, 0.0, -kicked.z);
        for (int i = RcFx.count(TRAIL * fade); i > 0; i--) {
            Vec3 at = middle.m_82520_(RANDOM.nextGaussian() * body.m_20205_() * 0.5, RANDOM.nextGaussian() * body.m_20206_() * 0.3,
                RANDOM.nextGaussian() * body.m_20205_() * 0.5);
            RcFx.at(RcFx.STREAK, false, 5 + RANDOM.nextInt(3), at).velocity(back.m_82490_(0.3 + 0.3 * RANDOM.nextDouble())).drag(0.75F)
                .colors(0x99FFFFFF, RcFx.clear(0xFFE8E4DE)).size(0.09F, 0.02F).bloom(0.0F).spawn();
        }
        if (RANDOM.nextFloat() < fade) {
            RcFx.at(RcFx.PUFF, false, 14 + RANDOM.nextInt(8), middle).velocity(back.m_82490_(0.05)).drag(0.85F).fall(-0.002F)
                .colors(0x88E4DED4, RcFx.clear(0xFFC8C0B4)).size(0.4F, 1.3F).spawn();
        }
    }

    /** A body's own head turn and pitch put back as they were before it was drawn turned. */
    private static void restore(LivingEntity entity, float[] head) {
        if (head != null) {
            entity.f_20885_ = head[0];
            entity.f_20886_ = head[1];
            entity.m_146926_(head[2]);
            entity.f_19860_ = head[3];
        }
    }

    private static float wrap(float degrees) {
        return degrees - 360.0F * (float)Math.floor((degrees + 180.0F) / 360.0F);
    }

    @SubscribeEvent
    public static void frame(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            TURNED.clear();
            // (A body whose drawing was cut short last frame gets its own head back.)
            HEADS.forEach(PaleReactions::restore);
            HEADS.clear();
        }
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        double now = PaleClient.clock(0.0F);
        if (!REELS.isEmpty()) {
            REELS.values().removeIf(reel -> now - reel.at() > LENGTH || now < reel.at() - 40.0);
        }
        if (KICKED.isEmpty()) {
            return;
        }
        // A kicked body is down from the tick it touches the ground (as the server finds it), and done once it is up again.
        ClientLevel level = Minecraft.m_91087_().f_91073_;
        KICKED.entrySet().removeIf(entry -> {
            Kicked kicked = entry.getValue();
            Entity body = level == null ? null : level.m_6815_(entry.getKey());
            if (!(body instanceof LivingEntity living) || !living.m_6084_() || now < kicked.at - 40.0) {
                return true;
            }
            double since = now - kicked.at;
            if (Double.isNaN(kicked.landed) && ((since >= 3.0 && living.m_20096_()) || since >= KickTumble.AIRBORNE_MAX)) {
                kicked.landed = now;
            }
            if (Double.isNaN(kicked.landed) && since < TRAIL_TICKS) {
                trail(living, kicked, since);
            }
            return KickTumble.over(since, Double.isNaN(kicked.landed) ? Double.NaN : kicked.landed - kicked.at, lie(kicked));
        });
    }
}
