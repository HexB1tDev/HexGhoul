package com.hex.kagune.control.client.render;

import com.hex.kagune.control.ControlEntities;
import com.hex.kagune.control.client.ChildClient;
import com.hex.kagune.control.family.ChildRules;
import com.hex.kagune.control.family.ChildSkin;
import com.hex.kagune.control.family.ChildStage;
import com.hex.kagune.control.npc.ChildNpc;
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
 */
public final class ChildRenderer extends HumanoidMobRenderer<ChildNpc, PlayerModel<ChildNpc>> {
    private static final ResourceLocation FALLBACK = Objects.requireNonNull(ResourceLocation.m_214293_("minecraft", "textures/entity/player/wide/steve.png"));
    /** Each skin made (by the child's looks, stage and kakugan), and its texture's name. */
    private static final Map<String, ResourceLocation> MADE = new HashMap<>();

    public ChildRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(context.m_174023_(ModelLayers.f_171162_)), 0.25F);
    }

    /** The model, sitting where its stage or its parent's arms have it sit. */
    static final class Model extends PlayerModel<ChildNpc> {
        Model(ModelPart root) {
            super(root, false);
        }

        @Override
        public void m_6973_(ChildNpc child, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
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
                this.f_103377_.m_104315_(this.f_102813_);
                this.f_103376_.m_104315_(this.f_102814_);
                this.f_103375_.m_104315_(this.f_102811_);
                this.f_103374_.m_104315_(this.f_102812_);
            }
        }
    }

    @Override
    public void m_7392_(ChildNpc child, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        ChildClient.View view = ChildClient.get(child.m_19879_());
        ChildStage stage = view == null ? child.stage : view.stage();
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

    /** A carried child, drawn at its parent's chest as they are drawn this frame (whatever lag its own position has). */
    private static void glue(ChildNpc child, ChildClient.View view, float partial, PoseStack pose) {
        if (view == null || view.carrier() == 0 || !(child.m_9236_().m_6815_(view.carrier()) instanceof net.minecraft.world.entity.LivingEntity carrier)) {
            return;
        }
        float turn = ((carrier.f_20883_ - carrier.f_20884_) % 360.0F + 540.0F) % 360.0F - 180.0F;
        float bodyYaw = carrier.f_20884_ + turn * partial;
        double scale = com.hex.kagune.control.Genders.female(carrier) ? com.hex.kagune.control.FemaleBody.SIZE : 1.0;
        double yaw = Math.toRadians(bodyYaw);
        double ahead = ChildRules.CARRY_AHEAD * scale;
        double cx = carrier.f_19790_ + (carrier.m_20185_() - carrier.f_19790_) * partial - Math.sin(yaw) * ahead;
        double cy = carrier.f_19791_ + (carrier.m_20186_() - carrier.f_19791_) * partial + ChildRules.CARRY_UP * scale - child.m_20206_() * 0.35;
        double cz = carrier.f_19792_ + (carrier.m_20189_() - carrier.f_19792_) * partial + Math.cos(yaw) * ahead;
        double ex = child.f_19790_ + (child.m_20185_() - child.f_19790_) * partial;
        double ey = child.f_19791_ + (child.m_20186_() - child.f_19791_) * partial;
        double ez = child.f_19792_ + (child.m_20189_() - child.f_19792_) * partial;
        pose.m_85837_(cx - ex, cy - ey, cz - ez);
        child.f_20883_ = bodyYaw;
        child.f_20884_ = bodyYaw;
    }

    @Override
    public ResourceLocation m_5478_(ChildNpc child) {
        ChildClient.View view = ChildClient.get(child.m_19879_());
        if (view == null) {
            return FALLBACK;
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
