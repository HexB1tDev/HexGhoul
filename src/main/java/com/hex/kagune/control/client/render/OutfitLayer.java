package com.hex.kagune.control.client.render;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.control.outfit.CoatDrape;
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
 * on its face ({@link #EXTRA}: an Aogiri member's faction mask, Noro's, Ayato's in a fight). Vanilla's armour layer draws these pieces
 * clear ({@link OutfitItem#getArmorTexture}).
 */
public final class OutfitLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final Map<String, RenderType> TYPES = new HashMap<>();
    /** 4.0.0: a mask drawn over whatever is on the head (an item id), by whoever knows of one for the wearer; null for none. */
    public static Function<LivingEntity, String> EXTRA = OutfitLayer::faction;

    /**
     * 4.0.0: the mask a wearer has on its face that is no item it holds: an Aogiri member's faction mask (its identity, as every client is
     * told it; 4.0.3: the Crimson Outfit has no hood, its coat a high collar), a companion's own ({@link #COMPANION}).
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
            CoatDrape[] drape = null;
            for (EquipmentSlot slot : SLOTS) {
                ItemStack stack = wearer.m_6844_(slot);
                if (stack.m_41720_() instanceof OutfitItem item) {
                    Outfits.Piece piece = item.piece();
                    if (drape == null && slot == EquipmentSlot.LEGS) {
                        drape = drapes(wearer, model.f_102813_, model.f_102814_, player, partialTick, ageInTicks);
                    }
                    draw(pose, buffers, light, bones, player, partialTick, piece.model(), piece.mask() ? -1 : piece.slot(), slim, drape);
                }
            }
            String extra = EXTRA.apply(wearer);
            if (extra != null) {
                draw(pose, buffers, light, bones, player, partialTick, extra, -1, slim, null);
            }
        } catch (RuntimeException failure) {
            PlayerLayers.failed("an outfit", failure);
        }
    }

    /**
     * {@code model}'s cubes in {@code slot} (any: -1) on {@code bones}, as they are posed (the sleeves for slim arms on a slim wearer), a
     * coat's tails hung as {@code drape} has them ({@link CoatDrape}: the right half's, the left's; unturned if null).
     */
    static void draw(PoseStack pose, MultiBufferSource buffers, int light, ModelPart[] bones, AbstractClientPlayer player, float partial, String modelId,
                     int slot, boolean slim, CoatDrape[] drape) {
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
            // 4.0.6 (the owner: "when limbs are removed ... coats and armors go with the limbs as well. (same with masks)"): a part not drawn
            // (torn off by Potential Unlock, a body cut in two) takes what is worn on it: its sleeve, its trouser leg and boot, the mask and
            // cap with the head (drawn on the part itself where it lies: PotentialRenderer).
            if (!cubes.isEmpty() && bones[bone].f_104207_) {
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
            // 4.0.2 (the owner: "the jacket also breaks when walking"): a coat's tails hang from the hip; 4.0.3: draped over both legs
            // ({@link #drape}), never parting down the middle, never with a leg through them.
            if ((bone == OutfitModels.RIGHT_LEG || bone == OutfitModels.LEFT_LEG) && bones[OutfitModels.BODY] != null && bones[OutfitModels.BODY].f_104207_) {
                // The tails are the coat's: they stay with the body (hung from the hip, a leg off or not).
                List<OutfitModels.Cube> tails = model.on(bone == OutfitModels.RIGHT_LEG ? OutfitModels.RIGHT_LEG_COAT : OutfitModels.LEFT_LEG_COAT, slot);
                if (!tails.isEmpty()) {
                    pose.m_85836_();
                    tails(pose, bones[bone]);
                    emit(out, pose, tails, light, null, drape == null ? null : drape[bone == OutfitModels.RIGHT_LEG ? 0 : 1]);
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

    /**
     * 4.0.2: the pose a coat tail hangs in from the leg {@code leg}'s hip: its pivot, turned out and round with the leg (z, then y, as the
     * part turns); 4.0.3: its swing forward and back is the drape's ({@link #drape}), each corner its own.
     */
    static void tails(PoseStack pose, ModelPart leg) {
        pose.m_252880_(leg.f_104200_ / 16.0F, leg.f_104201_ / 16.0F, leg.f_104202_ / 16.0F);
        pose.m_252781_(new org.joml.Quaternionf().rotateAxis(leg.f_104205_, 0.0F, 0.0F, 1.0F).rotateAxis(leg.f_104204_, 0.0F, 1.0F, 0.0F));
    }

    /** 4.0.3: how fast a coat's tails fall back as the leg that pushed them swings away (radians a tick): a leg pushes them at once. */
    static final float TAIL_FALL = 0.12F;
    /** 4.0.3: each wearer's tails as they hung last; 4.1.12: each half's ({right front, right back, left front, left back, when}). */
    private static final Map<LivingEntity, float[]> HUNG = new java.util.WeakHashMap<>();

    /**
     * 4.1.12 (the owner, of the Crimson Outfit in a jump and a kick: "I truly don't know how in this world you expected the clothes to open like
     * that and look good"): each half of a coat's tails hangs from its own leg. Its front goes with its own thigh only when that thigh comes
     * forward, its back only when it goes back, just clear of that leg (its trousers, its bent knee, its shin, its boot); otherwise it hangs
     * straight down. So a kick or a tucked leg lifts that leg's flap, the other hangs, and the coat parts down the middle as a long coat does.
     * (4.0.3 hung both fronts from the leg furthest forward and both backs from the one furthest back, never parted: any kick or tuck threw
     * the whole skirt out flat, a disc.) A leg pushes its half at once; the half falls back slowly as it swings away ({@link #TAIL_FALL}).
     * {right half, left half}, or null.
     */
    static CoatDrape[] drapes(LivingEntity wearer, ModelPart right, ModelPart left, AbstractClientPlayer player, float partial, float age) {
        if (right == null || left == null) {
            return null;
        }
        float[] thigh = {right.f_104203_, left.f_104203_};
        float[] knee = {knee(player, "rightLeg", partial), knee(player, "leftLeg", partial)};
        float[] hung = {Math.min(0.0F, thigh[0]), Math.max(0.0F, thigh[0]), Math.min(0.0F, thigh[1]), Math.max(0.0F, thigh[1]), age};
        if (wearer != null) {
            float[] last = HUNG.get(wearer);
            if (last != null && last.length == 5 && Math.abs(age - last[4]) < 20.0F) {
                float since = Math.max(0.0F, age - last[4]);
                for (int leg = 0; leg < 2; leg++) {
                    hung[leg * 2] = Math.min(hung[leg * 2], Math.min(0.0F, last[leg * 2] + TAIL_FALL * since));
                    hung[leg * 2 + 1] = Math.max(hung[leg * 2 + 1], Math.max(0.0F, last[leg * 2 + 1] - TAIL_FALL * since));
                }
            }
            HUNG.put(wearer, hung);
        }
        CoatDrape[] out = new CoatDrape[2];
        for (int leg = 0; leg < 2; leg++) {
            out[leg] = CoatDrape.over(hung[leg * 2], hung[leg * 2 + 1], new float[] {thigh[leg], thigh[leg]}, new float[] {knee[leg], knee[leg]});
        }
        return out;
    }

    /** 4.0.3: how far a player's knee is bent this frame (radians about x: the shin back), by bendy-lib; 0 for none. */
    static float knee(AbstractClientPlayer player, String part, float partial) {
        if (player == null) {
            return 0.0F;
        }
        float[] turn = com.hex.kagune.control.client.animation.BodyAnimations.bend(player, part, partial);
        return turn == null ? 0.0F : turn[0] * turn[1];
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

    /** As {@link #emit(VertexConsumer, PoseStack, List, int, Bend, CoatDrape)}, unbent. */
    static void emit(VertexConsumer out, PoseStack pose, List<OutfitModels.Cube> cubes, int light) {
        emit(out, pose, cubes, light, null, null);
    }

    /** As {@link #emit(VertexConsumer, PoseStack, List, int, Bend, CoatDrape)}, undraped. */
    static void emit(VertexConsumer out, PoseStack pose, List<OutfitModels.Cube> cubes, int light, Bend bend) {
        emit(out, pose, cubes, light, bend, null);
    }

    /**
     * The cubes' faces, from where their bone's part stands (sixteenths: the pose is in blocks), bent as the part is ({@code bend}, or
     * null), a coat's tails hung as {@code drape} has them (or null).
     */
    static void emit(VertexConsumer out, PoseStack pose, List<OutfitModels.Cube> cubes, int light, Bend bend, CoatDrape drape) {
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
                if (drape != null) {
                    float faceTurn = 0.0F;
                    for (int k = 0; k < 4; k++) {
                        faceTurn += drape.turn(c[k * 3 + 1], CoatDrape.backward(c[k * 3 + 2])) / 4.0F;
                    }
                    float ny = n[1];
                    n[1] = (float)(ny * Math.cos(faceTurn) - n[2] * Math.sin(faceTurn));
                    n[2] = (float)(ny * Math.sin(faceTurn) + n[2] * Math.cos(faceTurn));
                }
                for (int k = 0; k < 4; k++) {
                    p[0] = c[k * 3];
                    p[1] = c[k * 3 + 1];
                    p[2] = c[k * 3 + 2];
                    if (bend != null) {
                        bend.turn(p, bend.share(p[1]), true);
                    }
                    if (drape != null) {
                        drape.place(p);
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
