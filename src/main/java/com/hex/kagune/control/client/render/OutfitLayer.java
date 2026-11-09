package com.hex.kagune.control.client.render;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.control.outfit.OutfitItem;
import com.hex.kagune.control.outfit.OutfitModels;
import com.hex.kagune.control.outfit.Outfits;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 4.0.0 (The Great Exploration): the owner's outfits and masks on whoever wears them (a player, an investigator, a ghoul, Aogiri's own,
 * a companion): each piece in its slot drawn from the owner's model ({@link OutfitModels}), every cube on its bone's model part as it is
 * posed this frame (walking, running, a blow, a flip: the parts carry them), in the owner's texture. A wearer may also carry a mask drawn
 * over a hood ({@link #EXTRA}: an Aogiri member's faction mask, Noro's, Ayato's in a fight). Vanilla's armour layer draws these pieces
 * clear ({@link OutfitItem#getArmorTexture}).
 */
public final class OutfitLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final Map<String, RenderType> TYPES = new HashMap<>();
    /** 4.0.0: a mask drawn over whatever is on the head (an item id), by whoever knows of one for the wearer; null for none. */
    public static Function<LivingEntity, String> EXTRA = OutfitLayer::faction;

    /**
     * 4.0.0: the mask a wearer has over its hood or its face that is no item it holds: an Aogiri member's faction mask under the Crimson
     * Hood (its identity, as every client is told it), a companion's own ({@link #COMPANION}).
     */
    static String faction(LivingEntity wearer) {
        if (wearer instanceof com.hex.kagune.control.npc.GhoulNpc) {
            com.hex.kagune.control.client.GhoulNpcClient.View view = com.hex.kagune.control.client.GhoulNpcClient.get(wearer.m_19879_());
            return view != null && view.identity().aogiri() ? view.identity().maskItem() : null;
        }
        return COMPANION.apply(wearer);
    }

    /** 4.0.0: a companion's own mask (Noro's always; Ayato's in a fight), told by the companions' client. */
    public static Function<LivingEntity, String> COMPANION = wearer -> null;

    public OutfitLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void m_6494_(PoseStack pose, MultiBufferSource buffers, int light, T wearer, float limbSwing, float limbSwingAmount, float partialTick,
                        float ageInTicks, float netHeadYaw, float headPitch) {
        try {
            if (wearer.m_20145_() || !(this.m_117386_() instanceof HumanoidModel<?> model)) {
                return;
            }
            ModelPart[] bones = {model.f_102808_, model.f_102810_, model.f_102811_, model.f_102812_, model.f_102813_, model.f_102814_};
            AbstractClientPlayer player = wearer instanceof AbstractClientPlayer p ? p : null;
            for (EquipmentSlot slot : SLOTS) {
                ItemStack stack = wearer.m_6844_(slot);
                if (stack.m_41720_() instanceof OutfitItem item) {
                    Outfits.Piece piece = item.piece();
                    draw(pose, buffers, light, bones, player, partialTick, piece.model(), piece.mask() ? -1 : piece.slot());
                }
            }
            String extra = EXTRA.apply(wearer);
            if (extra != null) {
                draw(pose, buffers, light, bones, player, partialTick, extra, -1);
            }
        } catch (RuntimeException failure) {
            PlayerLayers.failed("an outfit", failure);
        }
    }

    /** {@code model}'s cubes in {@code slot} (any: -1) on {@code bones}, as they are posed. */
    static void draw(PoseStack pose, MultiBufferSource buffers, int light, ModelPart[] bones, AbstractClientPlayer player, float partial, String modelId,
                     int slot) {
        OutfitModels.Model model = OutfitModels.get(modelId);
        if (model == null) {
            return;
        }
        VertexConsumer out = buffers.m_6299_(type(modelId));
        for (int bone = 0; bone < bones.length; bone++) {
            List<OutfitModels.Cube> cubes = model.on(bone, slot);
            if (cubes.isEmpty() || bones[bone] == null) {
                continue;
            }
            pose.m_85836_();
            // PlayerAnimator bends a player's head, body and arms with the upper body: they ride it as the parts do.
            if (player != null && bone <= OutfitModels.LEFT_ARM) {
                PlayerLayers.upperBody(pose, player, partial);
            }
            bones[bone].m_104299_(pose);
            emit(out, pose, cubes, light);
            pose.m_85849_();
        }
    }

    /** The cubes' faces, from where their bone's part stands (sixteenths: the pose is in blocks). */
    static void emit(VertexConsumer out, PoseStack pose, List<OutfitModels.Cube> cubes, int light) {
        Matrix4f matrix = pose.m_85850_().m_252922_();
        Matrix3f normals = pose.m_85850_().m_252943_();
        for (OutfitModels.Cube cube : cubes) {
            for (OutfitModels.Face face : cube.faces()) {
                float[] c = face.corners();
                float[] uv = face.uvs();
                float[] n = face.normal();
                for (int k = 0; k < 4; k++) {
                    out.m_252986_(matrix, c[k * 3] / 16.0F, c[k * 3 + 1] / 16.0F, c[k * 3 + 2] / 16.0F)
                        .m_6122_(255, 255, 255, 255)
                        .m_7421_(uv[k * 2], uv[k * 2 + 1])
                        .m_86008_(OverlayTexture.f_118083_)
                        .m_85969_(light)
                        .m_252939_(normals, n[0], n[1], n[2])
                        .m_5752_();
                }
            }
        }
    }

    /** {@code model}'s texture as armour is drawn (cut out, both sides). */
    static RenderType type(String model) {
        return TYPES.computeIfAbsent(model, id -> RenderType.m_110431_(Objects.requireNonNull(KaguneMod.id("textures/outfit/" + id + ".png"))));
    }

    /** {@code model}'s texture's location (the first-person sleeves use it). */
    static ResourceLocation texture(String model) {
        return Objects.requireNonNull(KaguneMod.id("textures/outfit/" + model + ".png"));
    }
}
