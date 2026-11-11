package com.hex.kagune.control.client;

import com.hex.kagune.control.ControlItems;
import com.hex.kagune.control.quinque.ArmMesh;
import com.hex.kagune.control.quinque.Quinque;
import com.hex.kagune.control.quinque.QuinqueMesh;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Objects;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * 4.1.20 (the owner: "every weapon in the game should look like it's made from a kagune ... I don't want squares"): a quinque grown as a
 * smooth mesh ({@link QuinqueMesh}), drawn by code wherever its item is drawn (its model's parent is builtin/entity, so the game hands the
 * drawing to this): in a hand in third person, in first person ({@code ArmItemLayer.throughBody} draws it so too), in an investigator's
 * hand, on the ground, in a frame, in the inventory. The item's own display transforms still place it, so every hold solved for its old
 * model holds. Coming out of its case it grows out of the fist, the small case round the grip shrinking into it ({@link QuinqueMesh#drawn},
 * {@link QuinqueMesh#caseBox}), as smoothly as the clock runs.
 */
public final class QuinqueItemRenderer extends net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer {
    private static final int FULL_BRIGHT = 0xF000F0;

    /**
     * Its render type, made the first time it draws: this class is loaded as the items are registered (offer), long before anything is
     * drawn, and must not touch the render types then.
     */
    private static final class Solid {
        static final RenderType TYPE = RenderType.m_110473_(Objects.requireNonNull(ResourceLocation.m_214293_("hexkagune", "textures/misc/white.png")));
    }

    private static QuinqueItemRenderer instance;

    private QuinqueItemRenderer() {
        super(net.minecraft.client.Minecraft.m_91087_().m_167982_(), net.minecraft.client.Minecraft.m_91087_().m_167973_());
    }

    /** The one renderer, made the first time an item asks for it (the game is up by then). */
    static synchronized QuinqueItemRenderer get() {
        if (instance == null) {
            instance = new QuinqueItemRenderer();
        }
        return instance;
    }

    /** Given to a mesh quinque's item by its {@code initializeClient} (on a client only: through DistExecutor). */
    public static void offer(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return get();
            }
        });
    }

    @Override
    public void m_108829_(ItemStack stack, net.minecraft.world.item.ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light,
                          int overlay) {
        try {
            Quinque q = ControlItems.quinqueWeapon(stack);
            ArmMesh.Mesh mesh = q == null ? null : QuinqueMesh.of(q);
            if (mesh == null) {
                return;
            }
            float out = QuinqueMesh.out(QuinqueClient.shown(stack, true));
            pose.m_85836_();
            try {
                // 4.1.21: the wrists of whoever holds it (QuinqueWrists): the weapon turned in the fist about the right grip, as the body's
                // swing bends them (QuinqueGrip.wristInItem: the arm's turn in the item's own frame).
                float[] wrists = QuinqueWrists.of(stack, context);
                if (wrists != null && (wrists[0] != 0.0F || wrists[1] != 0.0F || wrists[2] != 0.0F)) {
                    com.hex.kagune.control.client.animation.QuinqueHolds.Hold h = com.hex.kagune.control.client.animation.QuinqueHolds.of(q);
                    if (h != null) {
                        float[] turn = com.hex.kagune.control.client.animation.QuinqueGrip.wristInItem(h, wrists[0], wrists[1], wrists[2]);
                        float[] g = h.rightGrip();
                        pose.m_85837_(g[0] / 16.0, g[1] / 16.0, g[2] / 16.0);
                        pose.m_252781_(new org.joml.Quaternionf().rotateAxis(turn[2], 0.0F, 0.0F, 1.0F).rotateAxis(turn[1], 0.0F, 1.0F, 0.0F)
                            .rotateAxis(turn[0], 1.0F, 0.0F, 0.0F));
                        pose.m_85837_(-g[0] / 16.0, -g[1] / 16.0, -g[2] / 16.0);
                    }
                }
                // Turned onto the handheld diagonal about the model's middle, as the baker turns its boxes (QuinqueMesh.turned), then the
                // model's own pixels (ItemRenderer has already moved the model's corner to the origin).
                pose.m_85837_(0.5, 0.5, 0.5);
                pose.m_252781_(new org.joml.Quaternionf().rotateAxis((float)Math.toRadians(QuinqueMesh.DIAGONAL), 0.0F, 0.0F, 1.0F));
                pose.m_85837_(-0.5, -0.5, -0.5);
                pose.m_85841_(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
                // An enchanted one shimmers as any enchanted item (its glint runs over the mesh by the uv its points are given).
                VertexConsumer to = net.minecraft.client.renderer.entity.ItemRenderer.m_115222_(buffers, Solid.TYPE, false, stack.m_41790_());
                org.joml.Matrix4f m = pose.m_85850_().m_252922_();
                org.joml.Matrix3f n = pose.m_85850_().m_252943_();
                for (int i = 0; i < mesh.quads; i++) {
                    int lit = mesh.glow[i] ? FULL_BRIGHT : light;
                    for (int c = 0; c < 4; c++) {
                        int v = i * 4 + c;
                        int argb = mesh.argb[v];
                        to.m_252986_(m, mesh.pos[v * 3], QuinqueMesh.drawn(mesh.pos[v * 3 + 1], out), mesh.pos[v * 3 + 2])
                            .m_6122_(argb >> 16 & 255, argb >> 8 & 255, argb & 255, 255).m_7421_((mesh.pos[v * 3] + mesh.pos[v * 3 + 2]) / 32.0F, mesh.pos[v * 3 + 1] / 32.0F)
                            .m_86008_(overlay).m_85969_(lit).m_252939_(n, mesh.normal[v * 3], mesh.normal[v * 3 + 1], mesh.normal[v * 3 + 2]).m_5752_();
                    }
                }
                float[] box = QuinqueMesh.caseBox(out);
                if (box != null) {
                    ArmMesh.Mesh shell = QuinqueMesh.caseMesh();
                    for (int i = 0; i < shell.quads; i++) {
                        int lit = shell.glow[i] ? FULL_BRIGHT : light;
                        for (int c = 0; c < 4; c++) {
                            int v = i * 4 + c;
                            int argb = shell.argb[v];
                            float x = box[0] + (shell.pos[v * 3] + 1.0F) * 0.5F * (box[3] - box[0]);
                            float y = box[1] + (shell.pos[v * 3 + 1] + 1.0F) * 0.5F * (box[4] - box[1]);
                            float z = box[2] + (shell.pos[v * 3 + 2] + 1.0F) * 0.5F * (box[5] - box[2]);
                            to.m_252986_(m, x, y, z).m_6122_(argb >> 16 & 255, argb >> 8 & 255, argb & 255, 255).m_7421_((x + z) / 32.0F, y / 32.0F).m_86008_(overlay)
                                .m_85969_(lit).m_252939_(n, shell.normal[v * 3], shell.normal[v * 3 + 1], shell.normal[v * 3 + 2]).m_5752_();
                        }
                    }
                }
            } finally {
                pose.m_85849_();
            }
        } catch (RuntimeException failure) {
            com.hex.kagune.control.Safety.caught(failure, "drawing a quinque");
        }
    }
}
