package com.hex.kagune.control.client;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.control.client.animation.LaughPose;
import com.hex.kagune.control.client.animation.RigPose;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * 4.3.0: a mastered Ukaku's laughing pull-out on every client ({@code LaughPullOut} on the server): the body posed by {@link LaughPose}, laid
 * over what it was doing, and the laugh written over the head (the owner: "add like "HAHAHAHAHA" text over the head (similar the angry symbol
 * above the head on npcs)", "white and with a cool letter type"): one "HA" after another in white manga lettering ({@code laugh_ha.png}), each
 * tilted its own way, popping in, facing the camera, fading as the hand comes down.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class LaughPullOutClient {
    private static final RenderType HA = RenderType.m_110473_(Objects.requireNonNull(KaguneMod.id("textures/misc/laugh_ha.png")));
    /** One "HA"'s width over the head (blocks; its height half that) and how far over the head the row sits. */
    static final float HA_WIDTH = 0.52F;
    static final float OVER = 0.42F;

    private static final Map<Integer, Double> STARTS = new HashMap<>();

    private LaughPullOutClient() {
    }

    /** The laugh begins on player {@code id}. */
    public static void start(int id) {
        if (STARTS.size() > 64) {
            STARTS.clear();
        }
        STARTS.put(id, ControlClient.worldTime(Minecraft.m_91087_().m_91296_()));
    }

    /** How far into the laugh player {@code id} is (ticks), or -1 when they aren't laughing. */
    public static double age(int id, double time) {
        Double start = STARTS.get(id);
        if (start == null) {
            return -1.0;
        }
        double age = time - start;
        if (age < 0.0 || age > LaughPose.END) {
            if (age > LaughPose.END) {
                STARTS.remove(id);
            }
            return -1.0;
        }
        return age;
    }

    /** {@code p} posed for the laugh, if player {@code id} is laughing now; true if so. */
    public static boolean apply(RigPose p, int id, double time) {
        double age = age(id, time);
        if (age < 0.0) {
            return false;
        }
        LaughPose.over(p, (float)age);
        return true;
    }

    /** The "HA"s over a laughing player's head. */
    @SubscribeEvent
    public static void drawn(RenderPlayerEvent.Post event) {
        if (STARTS.isEmpty()) {
            return;
        }
        Player player = event.getEntity();
        double age = age(player.m_19879_(), ControlClient.worldTime(event.getPartialTick()));
        if (age < 0.0) {
            return;
        }
        double[][] has = LaughPose.has((float)age);
        if (has.length == 0) {
            return;
        }
        Vec3 camera = Minecraft.m_91087_().f_91063_.m_109153_().m_90583_();
        double x = player.f_19790_ + (player.m_20185_() - player.f_19790_) * event.getPartialTick();
        double z = player.f_19792_ + (player.m_20189_() - player.f_19792_) * event.getPartialTick();
        float toward = (float)Math.atan2(camera.f_82479_ - x, camera.f_82481_ - z);
        PoseStack pose = event.getPoseStack();
        pose.m_85836_();
        pose.m_85837_(0.0, player.m_20206_() + OVER, 0.0);
        pose.m_252781_(new Quaternionf().rotateAxis(toward, 0.0F, 1.0F, 0.0F));
        VertexConsumer out = event.getMultiBufferSource().m_6299_(HA);
        float height = HA_WIDTH * 0.5F;
        for (double[] one : has) {
            pose.m_85836_();
            // Across the row (this frame's x runs left to right as the camera sees it, as the anger mark's does), up and down it, tilted
            // (LaughPose's tilt is clockwise as seen: about z it turns the other way), sized.
            pose.m_85837_(one[0] * HA_WIDTH, one[1] * height, 0.0);
            pose.m_252781_(new Quaternionf().rotateAxis((float)-one[2], 0.0F, 0.0F, 1.0F));
            pose.m_85841_((float)one[3], (float)one[3], 1.0F);
            Matrix4f matrix = pose.m_85850_().m_252922_();
            Matrix3f normals = pose.m_85850_().m_252943_();
            int alpha = (int)Math.round(255.0 * Math.max(0.0, Math.min(1.0, one[4])));
            float w = HA_WIDTH * 0.5F;
            float h = height * 0.5F;
            float[][] corners = {{-w, -h, 0.0F, 1.0F}, {w, -h, 1.0F, 1.0F}, {w, h, 1.0F, 0.0F}, {-w, h, 0.0F, 0.0F}};
            for (float[] c : corners) {
                out.m_252986_(matrix, c[0], c[1], 0.0F).m_6122_(255, 255, 255, alpha).m_7421_(c[2], c[3]).m_86008_(OverlayTexture.f_118083_)
                    .m_85969_(0xF000F0).m_252939_(normals, 0.0F, 0.0F, 1.0F).m_5752_();
            }
            pose.m_85849_();
        }
        pose.m_85849_();
    }
}
