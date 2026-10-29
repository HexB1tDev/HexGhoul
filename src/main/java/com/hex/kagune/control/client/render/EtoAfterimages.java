package com.hex.kagune.control.client.render;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.control.client.EtoClient;
import com.hex.kagune.control.client.animation.BodyFrame;
import com.hex.kagune.control.client.animation.RigPose;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 6.1.0: Eto's afterimages: where she blinks from, her body stays a moment as a pale, see-through shape, frozen in the pose it
 * was in, fading out. Drawn as her posed body's own boxes ({@link BodyFrame#surface}), so the shape is exactly hers.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class EtoAfterimages {
    private static final RenderType TYPE = RenderType.m_110473_(Objects.requireNonNull(KaguneMod.id("textures/misc/ukaku_white.png")));
    /** How long one lasts (ticks), how see-through it starts, its colour, and at most how many at once. */
    static final double LIFE = 9.0;
    static final float ALPHA = 0.42F;
    static final int COLOUR = 0xE8ECEF;
    static final int MOST = 24;
    private static final int FULL_BRIGHT = 0xF000F0;

    /** One afterimage: the pose, her feet and body yaw (degrees) where she was, when (EtoClient's clock). */
    record Ghost(RigPose pose, double x, double y, double z, float yaw, double at) {
    }

    private static final List<Ghost> GHOSTS = new ArrayList<>();

    private EtoAfterimages() {
    }

    public static void add(RigPose pose, Vec3 feet, float bodyYaw, double time) {
        if (GHOSTS.size() >= MOST) {
            GHOSTS.remove(0);
        }
        GHOSTS.add(new Ghost(pose, feet.f_82479_, feet.f_82480_, feet.f_82481_, bodyYaw, time));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || GHOSTS.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        ClientLevel level = minecraft.f_91073_;
        if (level == null) {
            GHOSTS.clear();
            return;
        }
        double time = EtoClient.clock(event.getPartialTick());
        Vec3 camera = minecraft.f_91063_.m_109153_().m_90583_();
        PoseStack stack = event.getPoseStack();
        Matrix4f matrix = stack.m_85850_().m_252922_();
        Matrix3f normals = stack.m_85850_().m_252943_();
        MultiBufferSource.BufferSource buffers = minecraft.m_91269_().m_110104_();
        VertexConsumer consumer = null;
        for (Iterator<Ghost> it = GHOSTS.iterator(); it.hasNext(); ) {
            Ghost ghost = it.next();
            double age = time - ghost.at();
            if (age > LIFE || age < -20.0) {
                it.remove();
                continue;
            }
            if (age < 0.0) {
                continue;
            }
            double fade = 1.0 - age / LIFE;
            int alpha = (int)Math.round(255.0 * ALPHA * fade * fade);
            if (alpha <= 0) {
                continue;
            }
            int colour = alpha << 24 | COLOUR;
            double yaw = Math.toRadians(ghost.yaw());
            // Rig space (x her right, z forward) into the world at her feet, then relative to the camera.
            double fx = -Math.sin(yaw);
            double fz = Math.cos(yaw);
            double rx = -Math.cos(yaw);
            double rz = -Math.sin(yaw);
            double ox = ghost.x() - camera.f_82479_;
            double oy = ghost.y() - camera.f_82480_;
            double oz = ghost.z() - camera.f_82481_;
            if (consumer == null) {
                consumer = buffers.m_6299_(TYPE);
            }
            VertexConsumer out = consumer;
            BodyFrame.surface(ghost.pose(), (a, b, c, d, n) -> {
                double[] normal = {rx * n[0] + fx * n[2], n[1], rz * n[0] + fz * n[2]};
                for (double[] p : new double[][] {a, b, c, d}) {
                    out.m_252986_(matrix, (float)(ox + rx * p[0] + fx * p[2]), (float)(oy + p[1]), (float)(oz + rz * p[0] + fz * p[2]))
                        .m_6122_((colour >> 16) & 0xFF, (colour >> 8) & 0xFF, colour & 0xFF, colour >>> 24)
                        .m_7421_(0.5F, 0.5F)
                        .m_86008_(OverlayTexture.f_118083_)
                        .m_85969_(FULL_BRIGHT)
                        .m_252939_(normals, (float)normal[0], (float)normal[1], (float)normal[2])
                        .m_5752_();
                }
            });
        }
        if (consumer != null) {
            buffers.m_109912_(TYPE);
        }
    }
}
