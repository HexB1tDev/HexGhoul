package com.hex.kagune.control.client.render;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.control.client.PaleClient;
import com.hex.kagune.control.npc.ReaperTendril;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 2.0.33: what the White Reaper's ground strike ({@link ReaperTendril}) raises where it burst up out of the ground (4.1.27: Dawn's own
 * blades growing up out of it, no longer a tendril). Told by the server ({@code S2CUkakuFx.REAPER_TENDRIL}); nothing it does is decided here.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class ReaperTendrils {
    private static final RenderType TYPE = RenderType.m_110473_(Objects.requireNonNull(KaguneMod.id("textures/misc/ukaku_white.png")));
    private static final double MAX_DISTANCE_SQR = 128.0 * 128.0;
    private static final int POINTS = 14;
    private static final int PATH = 28;

    /** One rising out of the ground: where, its seed, when it burst up (the client's clock). */
    private record Rising(Vec3 at, int seed, double began) {
    }

    private static final List<Rising> RISING = new ArrayList<>();

    private ReaperTendrils() {
    }

    /** A tendril bursts up at {@code at} now ({@code seed}: how it leans and whips). */
    public static void rise(Vec3 at, int seed) {
        if (RISING.size() >= 8) {
            RISING.remove(0);
        }
        RISING.add(new Rising(at, seed, PaleClient.clock(0.0F)));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || RISING.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        ClientLevel level = minecraft.f_91073_;
        if (level == null) {
            return;
        }
        float partial = event.getPartialTick();
        double clock = PaleClient.clock(partial);
        Vec3 camera = minecraft.f_91063_.m_109153_().m_90583_();
        PoseStack stack = event.getPoseStack();
        Matrix4f matrix = stack.m_85850_().m_252922_();
        Matrix3f normals = stack.m_85850_().m_252943_();
        MultiBufferSource.BufferSource buffers = minecraft.m_91269_().m_110104_();
        VertexConsumer out = null;
        for (Iterator<Rising> it = RISING.iterator(); it.hasNext(); ) {
            Rising rising = it.next();
            double age = clock - rising.began();
            if (age >= ReaperTendril.LIFE) {
                it.remove();
                continue;
            }
            if (rising.at().m_82557_(camera) > MAX_DISTANCE_SQR || ReaperTendril.out(age) <= 0.0) {
                continue;
            }
            try {
                if (out == null) {
                    out = buffers.m_6299_(TYPE);
                }
                draw(out, matrix, normals, level, rising, age, camera, clock);
            } catch (RuntimeException failure) {
                it.remove();
                PlayerLayers.failed("the White Reaper's tendril", failure);
            }
        }
        if (out != null) {
            buffers.m_109912_(TYPE);
        }
    }

    /**
     * 4.1.27 (the owner: "it shouldn't be a tentacle. It should be like a smooth part of the blade, as if the blade grew out of the
     * ground"): Dawn's own blades growing up out of the mark ({@link ReaperTendril#blades}, each {@link com.hex.kagune.control.quinque
     * .QuinqueDawnMesh#risingBlade} scaled to its height as far out as it is, leaning its own way), smooth, black, a gold line down each
     * face; lit where they stand.
     */
    private static void draw(VertexConsumer out, Matrix4f matrix, Matrix3f normals, ClientLevel level, Rising rising, double age, Vec3 camera,
                             double clock) {
        com.hex.kagune.control.quinque.ArmMesh.Mesh mesh = com.hex.kagune.control.quinque.QuinqueDawnMesh.risingBlade();
        int light = LevelRenderer.m_109541_(level, BlockPos.m_274561_(rising.at().f_82479_, rising.at().f_82480_ + 1.0, rising.at().f_82481_));
        for (double[] blade : ReaperTendril.blades(rising.seed())) {
            double shown = ReaperTendril.out(age - blade[6]);
            if (shown <= 0.0) {
                continue;
            }
            double[] place = new double[3];
            double[] way = {Math.cos(blade[2]), 0.0, Math.sin(blade[2])};
            double cos = Math.cos(blade[3]);
            double sin = Math.sin(blade[3]);
            double tall = blade[4] * shown;
            double across = blade[5];
            // Its foot a little under the ground (it breaks out of it, never stands on it).
            double ox = rising.at().f_82479_ + blade[0] - camera.f_82479_;
            double oy = rising.at().f_82480_ - 0.25 - camera.f_82480_;
            double oz = rising.at().f_82481_ + blade[1] - camera.f_82481_;
            for (int q = 0; q < mesh.quads; q++) {
                for (int c = 0; c < 4; c++) {
                    int v = q * 4 + c;
                    lean(mesh.pos[v * 3] * across, mesh.pos[v * 3 + 1] * tall, mesh.pos[v * 3 + 2] * across, way, cos, sin, place);
                    float x = (float)(ox + place[0]);
                    float y = (float)(oy + place[1]);
                    float z = (float)(oz + place[2]);
                    lean(mesh.normal[v * 3], mesh.normal[v * 3 + 1], mesh.normal[v * 3 + 2], way, cos, sin, place);
                    int argb = mesh.argb[v];
                    out.m_252986_(matrix, x, y, z).m_6122_(argb >> 16 & 255, argb >> 8 & 255, argb & 255, 255).m_7421_(0.5F, 0.5F)
                        .m_86008_(net.minecraft.client.renderer.texture.OverlayTexture.f_118083_).m_85969_(light)
                        .m_252939_(normals, (float)place[0], (float)place[1], (float)place[2]).m_5752_();
                }
            }
        }
    }

    /** {@code x, y, z} (up the blade's own y) leaned toward {@code way} by the angle whose cosine and sine are given: into {@code to}. */
    static void lean(double x, double y, double z, double[] way, double cos, double sin, double[] to) {
        // About the level line square to the lean (k = up x way): up goes to up cos + way sin.
        double kx = way[2];
        double kz = -way[0];
        double dot = kx * x + kz * z;
        double cx = -kz * y;
        double cy = kz * x - kx * z;
        double cz = kx * y;
        to[0] = x * cos + cx * sin + kx * dot * (1.0 - cos);
        to[1] = y * cos + cy * sin;
        to[2] = z * cos + cz * sin + kz * dot * (1.0 - cos);
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        RISING.clear();
    }
}
