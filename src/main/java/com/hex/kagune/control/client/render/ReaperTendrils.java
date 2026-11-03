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
 * 2.0.33: the White Reaper's black tendril ({@link ReaperTendril}) drawn where it burst up out of the ground: the original's own Rinkaku
 * tube (its taper, its surface, its veins and its point, {@code KaguneWorldRenderer.emitTubeLegacy}, as the ghoul children's tentacles are
 * drawn) along {@link ReaperTendril#line}, black with dark red veins, as thick as a man where it leaves the ground. Told by the server
 * ({@code S2CUkakuFx.REAPER_TENDRIL}); nothing it does is decided here.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class ReaperTendrils {
    private static final RenderType TYPE = RenderType.m_110473_(Objects.requireNonNull(KaguneMod.id("textures/misc/ukaku_white.png")));
    private static final double MAX_DISTANCE_SQR = 128.0 * 128.0;
    private static final int POINTS = 14;
    private static final int PATH = 28;

    /** One rising out of the ground: where, its seed, when it burst up (the client's clock), its chain (the tube's own state). */
    private record Rising(Vec3 at, int seed, double began, com.hex.kagune.client.render.TendrilChain chain) {
    }

    private static final List<Rising> RISING = new ArrayList<>();

    private ReaperTendrils() {
    }

    /** A tendril bursts up at {@code at} now ({@code seed}: how it leans and whips). */
    public static void rise(Vec3 at, int seed) {
        if (RISING.size() >= 8) {
            RISING.remove(0);
        }
        RISING.add(new Rising(at, seed, PaleClient.clock(0.0F), new com.hex.kagune.client.render.TendrilChain(0)));
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

    private static void draw(VertexConsumer out, Matrix4f matrix, Matrix3f normals, ClientLevel level, Rising rising, double age, Vec3 camera,
                             double clock) {
        double[][] line = ReaperTendril.line(age, rising.seed(), POINTS);
        Vec3[] path = new Vec3[PATH];
        for (int i = 0; i < PATH; i++) {
            double[] p = com.hex.kagune.control.ukaku.CentipedeTail.catmullRom(line, i / (double)(PATH - 1));
            path[i] = new Vec3(rising.at().f_82479_ + p[0] - camera.f_82479_, rising.at().f_82480_ + p[1] - camera.f_82480_,
                rising.at().f_82481_ + p[2] - camera.f_82481_);
        }
        float length = (float)(ReaperTendril.HEIGHT * Math.max(0.03, ReaperTendril.out(age)));
        com.hex.kagune.client.KaguneClientState.View look = new com.hex.kagune.client.KaguneClientState.View(true, true, 0, 100.0F,
            ReaperTendril.COLOUR, ReaperTendril.VEINS, com.hex.kagune.gameplay.KaguneForm.RINKAKU_SEED, 1, 0L, 1.0F, 1.0F, 1, 1, 0,
            com.hex.kagune.gameplay.TendrilMode.IDLE, 0L, 0, 0, 0, 1, 1, Vec3.f_82478_, 0L, 0, -1, 0, Vec3.f_82478_,
            com.hex.kagune.gameplay.CentipedeTechnique.STRIKE, 0L, 0, -1, Vec3.f_82478_, new int[6], length, ReaperTendril.THICKNESS, 0.46F, 6.0F,
            10.0F, 8.5F, 5.0F, false, false, 0, 0, 0);
        int light = LevelRenderer.m_109541_(level, BlockPos.m_274561_(rising.at().f_82479_, rising.at().f_82480_ + 1.0, rising.at().f_82481_));
        com.hex.kagune.client.render.KaguneWorldRenderer.emitTubeLegacy(out, matrix, normals, rising.chain(), path, look, 0, light, clock, rising.seed());
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        RISING.clear();
    }
}
