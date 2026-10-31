package com.hex.kagune.control.client.render;

import com.hex.kagune.control.ControlEntities;
import com.hex.kagune.control.client.ChildClient;
import com.hex.kagune.control.family.ChildRules;
import com.hex.kagune.control.family.ChildSkin;
import com.hex.kagune.control.family.ChildStage;
import com.hex.kagune.control.npc.ChildNpc;
import com.hex.kagune.control.family.ChildSkins;
import com.hex.kagune.control.family.Genome;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 1.3.10: a child as vanilla draws a player, on its own skin made from what it inherited ({@link ChildSkin}, its kakugan's version while it
 * fights), as big as its stage ({@link ChildStage#draw}; a baby and a toddler on the model's little-one proportions); a baby sitting, and a
 * carried child sitting on its parent's arm, glued in front of their chest as they are drawn this frame. Never the adult female body.
 * 1.3.18: on one of the owner's own skins for sons and daughters ({@link ChildSkins}: its sex's, near its tone), on the slim arms or the wide
 * ones as that skin is painted for, its kakugan laid over the eye or eyes it has as it fights (the iris glowing); the old made skin only if
 * there were none.
 */
public final class ChildRenderer extends HumanoidMobRenderer<ChildNpc, PlayerModel<ChildNpc>> {
    private static final ResourceLocation FALLBACK = Objects.requireNonNull(ResourceLocation.m_214293_("minecraft", "textures/entity/player/wide/steve.png"));
    /** Each skin made (by the child's looks, stage and kakugan), and its texture's name. */
    private static final Map<String, ResourceLocation> MADE = new HashMap<>();

    /** 1.3.18: the model on wide arms and on slim ones, worn as its skin is painted. */
    private final Model wide;
    private final Model slim;

    public ChildRenderer(EntityRendererProvider.Context context) {
        this(context, new Model(context.m_174023_(ModelLayers.f_171162_), false), new Model(context.m_174023_(ModelLayers.f_171166_), true));
    }

    private ChildRenderer(EntityRendererProvider.Context context, Model wide, Model slim) {
        super(context, wide, 0.25F);
        this.wide = wide;
        this.slim = slim;
        this.m_115326_(new Kakugan(this));
    }

    /** 1.3.18: the owner's skin a child wears (null: none for its sex, or no view yet). */
    static ChildSkins.Skin skin(ChildClient.View view) {
        return view == null ? null : ChildSkins.pick(view.genome());
    }

    private static ResourceLocation texture(String name) {
        return Objects.requireNonNull(ResourceLocation.m_214293_("hexkagune", "textures/entity/child/skins/" + name + ".png"));
    }

    /**
     * The model, sitting where its stage or its parent's arms have it sit. 1.3.15: a baby and a toddler on a little one's proportions of its
     * own (the owner: vanilla's young model read as a shrunken adult): a full-size head on a body, arms and legs half as big, stacked so its
     * feet stay on the ground ({@link #little}); anyone older on a player's.
     */
    static final class Model extends PlayerModel<ChildNpc> {
        /** A little one's head against its body (both against a player's). */
        static final float HEAD = 1.0F;
        static final float BODY = 0.5F;

        Model(ModelPart root, boolean slim) {
            super(root, slim);
        }

        @Override
        public void m_6973_(ChildNpc child, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            boolean young = this.f_102610_;
            // Its own proportions below, never vanilla's young ones on top of them.
            this.f_102610_ = false;
            super.m_6973_(child, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            ChildClient.View view = ChildClient.get(child.m_19879_());
            if (view != null && (view.stage().sits() || view.carrier() != 0)) {
                // Sitting: legs out in front, a little apart (vanilla's riding pose), hands in its lap.
                this.f_102813_.f_104203_ = -1.4137167F;
                this.f_102813_.f_104204_ = 0.31415927F;
                this.f_102813_.f_104205_ = 0.07853982F;
                this.f_102814_.f_104203_ = -1.4137167F;
                this.f_102814_.f_104204_ = -0.31415927F;
                this.f_102814_.f_104205_ = -0.07853982F;
                this.f_102811_.f_104203_ += -0.5F;
                this.f_102812_.f_104203_ += -0.5F;
            }
            little(young);
            this.f_102809_.m_104315_(this.f_102808_);
            this.f_103378_.m_104315_(this.f_102810_);
            this.f_103377_.m_104315_(this.f_102813_);
            this.f_103376_.m_104315_(this.f_102814_);
            this.f_103375_.m_104315_(this.f_102811_);
            this.f_103374_.m_104315_(this.f_102812_);
            for (ModelPart part : new ModelPart[] {this.f_102809_, this.f_103378_, this.f_103377_, this.f_103376_, this.f_103375_, this.f_103374_}) {
                ModelPart from = part == this.f_102809_ ? this.f_102808_ : part == this.f_103378_ ? this.f_102810_ : part == this.f_103377_ ? this.f_102813_
                    : part == this.f_103376_ ? this.f_102814_ : part == this.f_103375_ ? this.f_102811_ : this.f_102812_;
                part.f_233553_ = from.f_233553_;
                part.f_233554_ = from.f_233554_;
                part.f_233555_ = from.f_233555_;
            }
        }

        /**
         * A little one's layout (model pixels, y down, the feet at 24): the head full size on a neck at 12, the body, arms and legs half
         * size below it (the arms at the body's sides, the legs from 18); or a player's own, for an older child.
         */
        private void little(boolean on) {
            float body = on ? BODY : 1.0F;
            float head = on ? HEAD : 1.0F;
            scale(this.f_102808_, head);
            scale(this.f_102810_, body);
            scale(this.f_102811_, body);
            scale(this.f_102812_, body);
            scale(this.f_102813_, body);
            scale(this.f_102814_, body);
            if (on) {
                this.f_102808_.f_104201_ = 12.0F;
                this.f_102810_.f_104201_ = 12.0F;
                this.f_102811_.f_104200_ = -2.5F;
                this.f_102812_.f_104200_ = 2.5F;
                this.f_102811_.f_104201_ = 13.0F;
                this.f_102812_.f_104201_ = 13.0F;
                this.f_102813_.f_104200_ = -0.95F;
                this.f_102814_.f_104200_ = 0.95F;
                this.f_102813_.f_104201_ = 18.0F;
                this.f_102814_.f_104201_ = 18.0F;
                this.f_102813_.f_104202_ = 0.05F;
                this.f_102814_.f_104202_ = 0.05F;
            } else {
                this.f_102811_.f_104200_ = -5.0F;
                this.f_102812_.f_104200_ = 5.0F;
                this.f_102813_.f_104200_ = -1.9F;
                this.f_102814_.f_104200_ = 1.9F;
            }
        }

        private static void scale(ModelPart part, float k) {
            part.f_233553_ = k;
            part.f_233554_ = k;
            part.f_233555_ = k;
        }
    }

    @Override
    public void m_7392_(ChildNpc child, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        ChildClient.View view = ChildClient.get(child.m_19879_());
        ChildStage stage = view == null ? child.stage : view.stage();
        child.carriedBy = view == null ? 0 : view.carrier();
        ChildSkins.Skin skin = skin(view);
        this.f_115290_ = skin != null && skin.slim() ? this.slim : this.wide;
        pose.m_85836_();
        try {
            glue(child, view, partial, pose);
            if (view != null && (stage.sits() || view.carrier() != 0)) {
                // Sat down: its legs (a little one's are half as long) out in front, not under it.
                pose.m_85837_(0.0, -0.75 * stage.draw * (stage.young ? 0.5 : 1.0), 0.0);
            }
            pose.m_85841_(stage.draw, stage.draw, stage.draw);
            super.m_7392_(child, yaw, partial, pose, buffers, light);
        } finally {
            pose.m_85849_();
        }
    }

    /**
     * A carried child, drawn in its parent's arms as they are drawn this frame (whatever lag its own position has): sitting on their
     * forearms at their chest, facing them (1.3.15: it was at their hip, facing away, drawn by their feet in first person).
     */
    private static void glue(ChildNpc child, ChildClient.View view, float partial, PoseStack pose) {
        if (view == null || view.carrier() == 0 || !(child.m_9236_().m_6815_(view.carrier()) instanceof net.minecraft.world.entity.LivingEntity carrier)) {
            return;
        }
        float turn = ((carrier.f_20883_ - carrier.f_20884_) % 360.0F + 540.0F) % 360.0F - 180.0F;
        float bodyYaw = carrier.f_20884_ + turn * partial;
        double scale = ChildNpc.carrySize(carrier);
        double yaw = Math.toRadians(bodyYaw);
        double ahead = ChildRules.CARRY_AHEAD * scale;
        double cx = carrier.f_19790_ + (carrier.m_20185_() - carrier.f_19790_) * partial - Math.sin(yaw) * ahead;
        double cy = carrier.f_19791_ + (carrier.m_20186_() - carrier.f_19791_) * partial + ChildRules.CARRY_UP * scale;
        double cz = carrier.f_19792_ + (carrier.m_20189_() - carrier.f_19792_) * partial + Math.cos(yaw) * ahead;
        double ex = child.f_19790_ + (child.m_20185_() - child.f_19790_) * partial;
        double ey = child.f_19791_ + (child.m_20186_() - child.f_19791_) * partial;
        double ez = child.f_19792_ + (child.m_20189_() - child.f_19792_) * partial;
        pose.m_85837_(cx - ex, cy - ey, cz - ez);
        child.f_20883_ = bodyYaw + 180.0F;
        child.f_20884_ = bodyYaw + 180.0F;
        child.f_20885_ = bodyYaw + 180.0F;
        child.f_20886_ = bodyYaw + 180.0F;
    }

    @Override
    public ResourceLocation m_5478_(ChildNpc child) {
        ChildClient.View view = ChildClient.get(child.m_19879_());
        if (view == null) {
            return FALLBACK;
        }
        ChildSkins.Skin skin = skin(view);
        if (skin != null) {
            return texture(skin.key());
        }
        boolean kakugan = view.turned() && view.genome().kakugan() != com.hex.kagune.control.family.Genome.NONE;
        String key = view.genome().save() + "|" + view.stage().ordinal() + "|" + kakugan;
        ResourceLocation made = MADE.get(key);
        if (made == null) {
            made = make(view, kakugan, MADE.size());
            MADE.put(key, made);
        }
        return made;
    }

    /** Its skin made and uploaded as a texture of its own. */
    private static ResourceLocation make(ChildClient.View view, boolean kakugan, int n) {
        try {
            if (ChildSkin.part("base_1") == null) {
                return FALLBACK; // the parts can't be read here: a plain skin rather than an invisible child
            }
            int[] px = ChildSkin.paint(view.genome(), view.stage(), kakugan, ChildSkin::part);
            NativeImage image = new NativeImage(ChildSkin.SIZE, ChildSkin.SIZE, true);
            for (int y = 0; y < ChildSkin.SIZE; y++) {
                for (int x = 0; x < ChildSkin.SIZE; x++) {
                    int argb = px[y * ChildSkin.SIZE + x];
                    // NativeImage takes ABGR.
                    image.m_84988_(x, y, argb & 0xFF00FF00 | (argb & 0xFF) << 16 | (argb >> 16) & 0xFF);
                }
            }
            ResourceLocation location = Objects.requireNonNull(ResourceLocation.m_214293_("hexkagune", "child_skin/" + n));
            Minecraft.m_91087_().m_91097_().m_118495_(location, new DynamicTexture(image));
            return location;
        } catch (RuntimeException failure) {
            com.hex.kagune.control.Safety.caught(failure, "a child's skin");
            return FALLBACK;
        }
    }

    /**
     * 1.3.18: a ghoul child's kakugan as it fights, on the owner's skin it wears: the eye or eyes it has turned (only those pixels, laid over
     * the skin), their irises drawn full bright.
     */
    static final class Kakugan extends RenderLayer<ChildNpc, PlayerModel<ChildNpc>> {
        Kakugan(ChildRenderer parent) {
            super(parent);
        }

        @Override
        public void m_6494_(PoseStack pose, MultiBufferSource buffers, int light, ChildNpc child, float limbSwing, float limbSwingAmount,
                            float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (child.m_20145_()) {
                return;
            }
            try {
                ChildClient.View view = ChildClient.get(child.m_19879_());
                ChildSkins.Skin skin = skin(view);
                if (skin == null || !view.turned()) {
                    return;
                }
                int eyes = view.genome().kakugan();
                PlayerModel<ChildNpc> model = this.m_117386_();
                for (String side : new String[] {"r", "l"}) {
                    boolean has = eyes == Genome.BOTH || eyes == (side.equals("r") ? Genome.RIGHT : Genome.LEFT);
                    if (!has) {
                        continue;
                    }
                    model.m_7695_(pose, buffers.m_6299_(RenderType.m_110473_(texture(skin.key() + "_k" + side))), light, OverlayTexture.f_118083_,
                        1.0F, 1.0F, 1.0F, 1.0F);
                    model.m_7695_(pose, buffers.m_6299_(RenderType.m_110488_(texture(skin.key() + "_g" + side))), 0xF000F0, OverlayTexture.f_118083_,
                        0.8F, 0.8F, 0.8F, 1.0F);
                }
            } catch (RuntimeException failure) {
                com.hex.kagune.control.Safety.caught(failure, "a child's kakugan");
            }
        }
    }

    /** Leaving the world: the skins made are forgotten (made again next time). */
    public static void forget() {
        MADE.clear();
    }

    @Mod.EventBusSubscriber(modid = "hexkagune", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ControlEntities.CHILD.get(), ChildRenderer::new);
        }
    }
}
