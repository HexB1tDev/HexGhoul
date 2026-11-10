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
        if (wearer instanceof com.hex.kagune.control.npc.EtoNpc npc && npc.persona().mask() != null) {
            // 4.0.0: Noro never shows his face; Ayato's respirator goes on when his kagune comes out.
            if (!npc.persona().maskInFight()) {
                return npc.persona().mask();
            }
            com.hex.kagune.control.client.NpcWingsClient.View wings = com.hex.kagune.control.client.NpcWingsClient.get(npc.m_19879_());
            return wings != null && wings.out() ? npc.persona().mask() : null;
        }
        return COMPANION.apply(wearer);
    }

    /** 4.0.0: a companion's own mask (Noro's always; Ayato's in a fight), told by the companions' client. */
    public static Function<LivingEntity, String> COMPANION = wearer -> null;

    /** 4.0.0: the models drawn on slim (three-pixel) arms, as their renderers built them (a player's is told by its skin). */
    private static final java.util.Set<Object> SLIM = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

    /** 4.0.0: {@code model} has slim arms (a renderer says so of its own as it makes it); returns it. */
    public static <X> X slim(X model) {
        if (model != null) {
            SLIM.add(model);
        }
        return model;
    }

    /** Whether {@code wearer}, drawn on {@code model}, has slim arms: a player by its skin, anyone else by the model it is drawn on. */
    static boolean slim(LivingEntity wearer, Object model) {
        if (wearer instanceof AbstractClientPlayer player) {
            return "slim".equals(player.m_108564_());
        }
        return SLIM.contains(model);
    }

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
            boolean slim = slim(wearer, model);
            for (EquipmentSlot slot : SLOTS) {
                ItemStack stack = wearer.m_6844_(slot);
                if (stack.m_41720_() instanceof OutfitItem item) {
                    Outfits.Piece piece = item.piece();
                    draw(pose, buffers, light, bones, player, partialTick, piece.model(), piece.mask() ? -1 : piece.slot(), slim);
                }
            }
            String extra = EXTRA.apply(wearer);
            if (extra != null) {
                draw(pose, buffers, light, bones, player, partialTick, extra, -1, slim);
            }
        } catch (RuntimeException failure) {
            PlayerLayers.failed("an outfit", failure);
        }
    }

    /** {@code model}'s cubes in {@code slot} (any: -1) on {@code bones}, as they are posed (the sleeves for slim arms on a slim wearer). */
    static void draw(PoseStack pose, MultiBufferSource buffers, int light, ModelPart[] bones, AbstractClientPlayer player, float partial, String modelId,
                     int slot, boolean slim) {
        OutfitModels.Model model = OutfitModels.get(modelId);
        if (model == null) {
            return;
        }
        VertexConsumer out = buffers.m_6299_(type(modelId));
        for (int bone = 0; bone < bones.length; bone++) {
            if (bones[bone] == null) {
                continue;
            }
            List<OutfitModels.Cube> cubes = model.on(arm(bone, slim), slot);
            if (!cubes.isEmpty()) {
                pose.m_85836_();
                // PlayerAnimator bends a player's head and arms with the upper body: they ride it as the parts do (the torso itself bends
                // at its middle, below).
                if (player != null && bone != OutfitModels.BODY && bone <= OutfitModels.LEFT_ARM) {
                    PlayerLayers.upperBody(pose, player, partial);
                }
                bones[bone].m_104299_(pose);
                emit(out, pose, cubes, light, bend(player, bone, partial));
                pose.m_85849_();
            }
            // 4.0.2 (the owner: "the jacket also breaks when walking"): a coat's tails hang from the hip and go only part of the way with
            // the leg, as cloth does (never a knee's bend).
            if (bone == OutfitModels.RIGHT_LEG || bone == OutfitModels.LEFT_LEG) {
                List<OutfitModels.Cube> tails = model.on(bone == OutfitModels.RIGHT_LEG ? OutfitModels.RIGHT_LEG_COAT : OutfitModels.LEFT_LEG_COAT, slot);
                if (!tails.isEmpty()) {
                    pose.m_85836_();
                    tails(pose, bones[bone]);
                    emit(out, pose, tails, light, null);
                    pose.m_85849_();
                }
            }
        }
    }

    /** The bone whose cubes go on {@code bone}'s part: a slim wearer's arms take the slim sleeves. */
    static int arm(int bone, boolean slim) {
        if (!slim) {
            return bone;
        }
        return bone == OutfitModels.RIGHT_ARM ? OutfitModels.RIGHT_ARM_SLIM : bone == OutfitModels.LEFT_ARM ? OutfitModels.LEFT_ARM_SLIM : bone;
    }

    /** 4.0.2: how much of a leg's swing its coat tail follows (forward and back; out to the side). */
    static final float TAIL_SWING = 0.45F;
    static final float TAIL_SPREAD = 0.6F;

    /** 4.0.2: the pose a coat tail hangs in from the leg {@code leg}'s hip: its pivot, its swing damped ({@link #TAIL_SWING}). */
    static void tails(PoseStack pose, ModelPart leg) {
        pose.m_252880_(leg.f_104200_ / 16.0F, leg.f_104201_ / 16.0F, leg.f_104202_ / 16.0F);
        // As the part turns (z, then y, then x), each damped.
        pose.m_252781_(new org.joml.Quaternionf().rotateAxis(leg.f_104205_ * TAIL_SPREAD, 0.0F, 0.0F, 1.0F).rotateAxis(leg.f_104204_, 0.0F, 1.0F, 0.0F)
            .rotateAxis(leg.f_104203_ * TAIL_SWING, 1.0F, 0.0F, 0.0F));
    }

    /**
     * 4.0.2: how a part's cubes bend with the bend bendy-lib puts in it this frame ({@code BodyAnimations.bend}), or null: an arm's lower
     * half from its elbow (the middle of its 12 pixels: 4 below its pivot), a leg's from its knee (6 below), the torso's upper half from
     * its middle (6 below the neck).
     */
    static Bend bend(AbstractClientPlayer player, int bone, float partial) {
        if (player == null) {
            return null;
        }
        String part = switch (bone) {
            case OutfitModels.BODY -> "body";
            case OutfitModels.RIGHT_ARM -> "rightArm";
            case OutfitModels.LEFT_ARM -> "leftArm";
            case OutfitModels.RIGHT_LEG -> "rightLeg";
            case OutfitModels.LEFT_LEG -> "leftLeg";
            default -> null;
        };
        if (part == null) {
            return null;
        }
        float[] turn = com.hex.kagune.control.client.animation.BodyAnimations.bend(player, part, partial);
        if (turn == null) {
            return null;
        }
        float middle = bone == OutfitModels.BODY ? 6.0F : bone == OutfitModels.RIGHT_ARM || bone == OutfitModels.LEFT_ARM ? 4.0F : 6.0F;
        return new Bend(turn[0], turn[1], turn[3], middle, bone != OutfitModels.BODY);
    }

    /**
     * 4.0.2: a bend through a part: {@code angle} about the axis ({@code ax}, 0, {@code az}) through (0, {@code middle}, 0) of the part (its
     * pixels), turning what lies below the middle ({@code below}; else above it) the whole way and what lies on it half way, as bendy-lib
     * turns the skin (the joint closed).
     */
    record Bend(float angle, float ax, float az, float middle, boolean below) {
        /** How much of the bend a point {@code y} pixels down the part takes: 0, a half on the joint, or all of it. */
        float share(float y) {
            float past = this.below ? y - this.middle : this.middle - y;
            return past > 0.01F ? 1.0F : past < -0.01F ? 0.0F : 0.5F;
        }

        /** ({@code v}: x, y, z) turned {@code share} of the bend about its axis (Rodrigues), in place; {@code about}: through its middle. */
        void turn(float[] v, float share, boolean about) {
            double a = this.angle * share;
            double length = Math.sqrt(this.ax * this.ax + this.az * this.az);
            if (length < 1.0E-6 || a == 0.0) {
                return;
            }
            double kx = this.ax / length;
            double kz = this.az / length;
            double x = v[0];
            double y = v[1] - (about ? this.middle : 0.0F);
            double z = v[2];
            double c = Math.cos(a);
            double s = Math.sin(a);
            double dot = kx * x + kz * z;
            // v cos + (k x v) sin + k (k . v)(1 - cos), k = (kx, 0, kz).
            double cx = -kz * y;
            double cy = kz * x - kx * z;
            double cz = kx * y;
            v[0] = (float)(x * c + cx * s + kx * dot * (1.0 - c));
            v[1] = (float)(y * c + cy * s) + (about ? this.middle : 0.0F);
            v[2] = (float)(z * c + cz * s + kz * dot * (1.0 - c));
        }
    }

    /** As {@link #emit(VertexConsumer, PoseStack, List, int, Bend)}, unbent. */
    static void emit(VertexConsumer out, PoseStack pose, List<OutfitModels.Cube> cubes, int light) {
        emit(out, pose, cubes, light, null);
    }

    /** The cubes' faces, from where their bone's part stands (sixteenths: the pose is in blocks), bent as the part is ({@code bend}, or null). */
    static void emit(VertexConsumer out, PoseStack pose, List<OutfitModels.Cube> cubes, int light, Bend bend) {
        Matrix4f matrix = pose.m_85850_().m_252922_();
        Matrix3f normals = pose.m_85850_().m_252943_();
        float[] p = new float[3];
        float[] n = new float[3];
        for (OutfitModels.Cube cube : cubes) {
            for (OutfitModels.Face face : cube.faces()) {
                float[] c = face.corners();
                float[] uv = face.uvs();
                float[] fn = face.normal();
                n[0] = fn[0];
                n[1] = fn[1];
                n[2] = fn[2];
                if (bend != null) {
                    float faceShare = 0.0F;
                    for (int k = 0; k < 4; k++) {
                        faceShare += bend.share(c[k * 3 + 1]) / 4.0F;
                    }
                    bend.turn(n, faceShare, false);
                }
                for (int k = 0; k < 4; k++) {
                    p[0] = c[k * 3];
                    p[1] = c[k * 3 + 1];
                    p[2] = c[k * 3 + 2];
                    if (bend != null) {
                        bend.turn(p, bend.share(p[1]), true);
                    }
                    out.m_252986_(matrix, p[0] / 16.0F, p[1] / 16.0F, p[2] / 16.0F)
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
