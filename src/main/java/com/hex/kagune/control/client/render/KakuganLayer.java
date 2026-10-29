package com.hex.kagune.control.client.render;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.control.client.KakuganClient;
import com.hex.kagune.control.kakugan.KakuganModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Draws each player's kakugan on their head ({@link KakuganModel}), as a layer of the player renderer so it follows
 * the head exactly (turning, crouching, every animation). It lies on the skin's inner face layer, replacing its eye
 * pixels (lifted only the hair the depth buffer needs, sized by camera distance), under the outer (hat) layer.
 *
 * <p>3.0.0, see-through second layers (glasses): Minecraft draws the whole skin, lenses included, before any layer, so a lens
 * stood in front of the kakugan in the depth buffer and the glass showed the skin's own eyes. While the kakugan is out over a
 * skin whose second layer has glass over the eyes ({@code SkinReader}), the second layer is held back for that frame
 * ({@link OuterLayer}) and drawn here after the kakugan, exactly as vanilla draws it, so the glass lies over the ghoul eyes.
 * Only fully opaque second-layer pixels over an eye (eyes painted on that layer) get those pixels of the kakugan drawn on them.
 * Solid parts use the same translucent white-texel type as the wings (lit by the world); the glow uses vanilla's eye type
 * (added light at full brightness, like spider eyes), so it shines in the dark.
 */
public final class KakuganLayer extends RenderLayer<AbstractClientPlayer, EntityModel<AbstractClientPlayer>> {
    static final RenderType SOLID = RenderType.m_110473_(Objects.requireNonNull(KaguneMod.id("textures/misc/ukaku_white.png")));
    static final RenderType GLOW = RenderType.m_110488_(Objects.requireNonNull(KaguneMod.id("textures/misc/ukaku_white.png")));
    private static final int FULL_BRIGHT = 0xF000F0;
    /** Beyond this distance (squared, blocks) the finest detail (the little veins across the whites) is skipped. */
    private static final double DETAIL_DISTANCE_SQR = 24.0 * 24.0;

    public KakuganLayer(RenderLayerParent<AbstractClientPlayer, EntityModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void m_6494_(PoseStack pose, MultiBufferSource buffers, int packedLight, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                        float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        PlayerLayers.ran(player);
        try {
            this.draw(pose, buffers, packedLight, player, limbSwing, limbSwingAmount, partialTick, ageInTicks, netHeadYaw, headPitch);
        } catch (RuntimeException failure) {
            PlayerLayers.failed("kakugan", failure);
        }
    }

    private void draw(PoseStack pose, MultiBufferSource buffers, int packedLight, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                        float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.m_20145_() || !(this.m_117386_() instanceof HumanoidModel<?> model)) {
            return;
        }
        float intensity = KakuganClient.intensity(player, partialTick);
        if (intensity <= 0.003F) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        Vec3 camera = minecraft.f_91063_.m_109153_().m_90583_();
        double dx = player.m_20185_() - camera.f_82479_;
        double dy = player.m_20186_() + 1.6 - camera.f_82480_;
        double dz = player.m_20189_() - camera.f_82481_;
        double distanceSqr = dx * dx + dy * dy + dz * dz;
        List<KakuganModel.Quad> quads = KakuganModel.build(KakuganClient.eyes(player), player.m_36316_().getId().hashCode(), ageInTicks / 20.0, intensity,
            distanceSqr < DETAIL_DISTANCE_SQR, KakuganModel.layerFor(distanceSqr), KakuganClient.covered(player) & ~KakuganClient.glass(player));
        boolean outerHeld = HELD.contains(player.m_20148_());
        if (quads.isEmpty() && !outerHeld) {
            return;
        }
        pose.m_85836_();
        PlayerLayers.upperBody(pose, player, partialTick);
        model.f_102808_.m_104299_(pose);
        Matrix4f matrix = pose.m_85850_().m_252922_();
        Matrix3f normals = pose.m_85850_().m_252943_();
        // Solid parts first, then the glow (its own batch, drawn over them), then the held-back second layer over both: each
        // batch is drawn as the next one starts, so the glass blends over the ghoul eyes.
        emit(buffers.m_6299_(SOLID), quads, false, packedLight, matrix, normals);
        emit(buffers.m_6299_(GLOW), quads, true, FULL_BRIGHT, matrix, normals);
        if (outerHeld) {
            hat(buffers.m_6299_(RenderType.m_110473_(player.m_108560_())), matrix, normals, packedLight);
        }
        pose.m_85849_();
    }

    /** Players whose second layer is held back this frame by {@link OuterLayer}: the layer draws it after the kakugan. */
    static final Set<UUID> HELD = new HashSet<>();

    /** The skin's outer (hat) layer of the head as vanilla draws it: the same box, half a pixel out, the same texture areas. */
    private static void hat(VertexConsumer c, Matrix4f m, Matrix3f n, int light) {
        float x0 = -4.5F;
        float x1 = 4.5F;
        float y0 = -8.5F;
        float y1 = 0.5F;
        float z0 = -4.5F;
        float z1 = 4.5F;
        hatFace(c, m, n, light, new float[][] {{x0, y0, z1, 40, 0}, {x1, y0, z1, 48, 0}, {x1, y0, z0, 48, 8}, {x0, y0, z0, 40, 8}}, 0, -1, 0);
        hatFace(c, m, n, light, new float[][] {{x0, y1, z0, 48, 8}, {x1, y1, z0, 56, 8}, {x1, y1, z1, 56, 0}, {x0, y1, z1, 48, 0}}, 0, 1, 0);
        hatFace(c, m, n, light, new float[][] {{x0, y0, z1, 32, 8}, {x0, y0, z0, 40, 8}, {x0, y1, z0, 40, 16}, {x0, y1, z1, 32, 16}}, -1, 0, 0);
        hatFace(c, m, n, light, new float[][] {{x1, y0, z0, 48, 8}, {x1, y0, z1, 56, 8}, {x1, y1, z1, 56, 16}, {x1, y1, z0, 48, 16}}, 1, 0, 0);
        hatFace(c, m, n, light, new float[][] {{x1, y0, z1, 56, 8}, {x0, y0, z1, 64, 8}, {x0, y1, z1, 64, 16}, {x1, y1, z1, 56, 16}}, 0, 0, 1);
        hatFace(c, m, n, light, new float[][] {{x0, y0, z0, 40, 8}, {x1, y0, z0, 48, 8}, {x1, y1, z0, 48, 16}, {x0, y1, z0, 40, 16}}, 0, 0, -1);
    }

    /** One face of the hat: corners {x, y, z (head pixels), u, v (skin pixels)}, and its normal. */
    private static void hatFace(VertexConsumer c, Matrix4f m, Matrix3f n, int light, float[][] corners, float nx, float ny, float nz) {
        for (float[] p : corners) {
            c.m_252986_(m, p[0] / 16.0F, p[1] / 16.0F, p[2] / 16.0F)
                .m_6122_(255, 255, 255, 255)
                .m_7421_(p[3] / 64.0F, p[4] / 64.0F)
                .m_86008_(OverlayTexture.f_118083_)
                .m_85969_(light)
                .m_252939_(n, nx, ny, nz)
                .m_5752_();
        }
    }

    static void emit(VertexConsumer consumer, List<KakuganModel.Quad> quads, boolean glow, int light, Matrix4f matrix, Matrix3f normals) {
        for (KakuganModel.Quad quad : quads) {
            if (quad.glow() != glow) {
                continue;
            }
            // The glow type culls back faces: send it both ways round so it shows whatever the winding.
            for (int pass = 0; pass < (glow ? 2 : 1); pass++) {
                for (int i = 0; i < 4; i++) {
                    int corner = pass == 0 ? i : 3 - i;
                    float[] xyz = quad.xyz();
                    int colour = quad.argb()[corner];
                    consumer.m_252986_(matrix, xyz[corner * 3] / 16.0F, xyz[corner * 3 + 1] / 16.0F, xyz[corner * 3 + 2] / 16.0F)
                        .m_6122_((colour >> 16) & 0xFF, (colour >> 8) & 0xFF, colour & 0xFF, colour >>> 24)
                        .m_7421_(0.5F, 0.5F)
                        .m_86008_(OverlayTexture.f_118083_)
                        .m_85969_(light)
                        .m_252939_(normals, quad.nx(), quad.ny(), quad.nz())
                        .m_5752_();
                }
            }
        }
    }

    /**
     * Holds a player's second layer back for this frame while their kakugan is out over glass on their skin's second layer
     * (lenses over the eyes), so the kakugan layer can draw it after the kakugan. Vanilla sets the hat's visibility just before
     * this event every frame, so it never sticks.
     */
    @Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
    public static final class OuterLayer {
        private OuterLayer() {
        }

        @SubscribeEvent
        public static void pre(RenderPlayerEvent.Pre event) {
            if (!(event.getEntity() instanceof AbstractClientPlayer player) || player.m_20145_()
                || KakuganClient.intensity(player, event.getPartialTick()) <= 0.003F || !KakuganClient.glassOverEyes(player)) {
                return;
            }
            if (event.getRenderer().m_7200_() instanceof HumanoidModel<?> model && model.f_102809_.f_104207_) {
                model.f_102809_.f_104207_ = false;
                HELD.add(player.m_20148_());
            }
        }

        @SubscribeEvent
        public static void post(RenderPlayerEvent.Post event) {
            if (HELD.remove(event.getEntity().m_20148_()) && event.getRenderer().m_7200_() instanceof HumanoidModel<?> model) {
                model.f_102809_.f_104207_ = true;
            }
        }
    }
}
