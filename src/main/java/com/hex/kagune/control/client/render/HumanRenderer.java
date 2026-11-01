package com.hex.kagune.control.client.render;

import com.hex.kagune.control.ControlEntities;
import com.hex.kagune.control.npc.HumanNpc;
import com.hex.kagune.control.npc.HumanRules;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Objects;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Home Living (1.3.40): The Human as vanilla draws a player, in one of vanilla's own default skins ({@link HumanRules#LOOKS}, wide or
 * slim arms as the skin is painted for); frightened, it runs with its arms thrown up over its head, flailing.
 */
public final class HumanRenderer extends HumanoidMobRenderer<HumanNpc, PlayerModel<HumanNpc>> {
    private static final ResourceLocation[] SKINS = new ResourceLocation[HumanRules.LOOKS.length * 2];

    static {
        for (int i = 0; i < SKINS.length; i++) {
            SKINS[i] = Objects.requireNonNull(ResourceLocation.m_214293_("minecraft", HumanRules.skin(i)));
        }
    }

    private final Model wide;
    private final Model slim;

    public HumanRenderer(EntityRendererProvider.Context context) {
        this(context, new Model(context.m_174023_(ModelLayers.f_171162_), false), new Model(context.m_174023_(ModelLayers.f_171166_), true));
    }

    private HumanRenderer(EntityRendererProvider.Context context, Model wide, Model slim) {
        super(context, wide, 0.5F);
        this.wide = wide;
        this.slim = slim;
    }

    @Override
    public void m_7392_(HumanNpc human, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        this.f_115290_ = HumanRules.slim(human.look()) ? this.slim : this.wide;
        super.m_7392_(human, yaw, partial, pose, buffers, light);
    }

    @Override
    public ResourceLocation m_5478_(HumanNpc human) {
        return SKINS[Math.floorMod(human.look(), SKINS.length)];
    }

    /** A player's body; frightened, its arms up over its head, waving. */
    static final class Model extends PlayerModel<HumanNpc> {
        Model(ModelPart root, boolean slim) {
            super(root, slim);
        }

        @Override
        public void m_6973_(HumanNpc human, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            super.m_6973_(human, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            if (!human.scared()) {
                return;
            }
            float wave = (float)Math.sin(ageInTicks * 0.9F) * 0.35F;
            this.f_102811_.f_104203_ = -2.75F + wave;
            this.f_102811_.f_104205_ = 0.25F + wave * 0.4F;
            this.f_102812_.f_104203_ = -2.75F - wave;
            this.f_102812_.f_104205_ = -0.25F + wave * 0.4F;
            this.f_103375_.m_104315_(this.f_102811_);
            this.f_103374_.m_104315_(this.f_102812_);
        }
    }

    @Mod.EventBusSubscriber(modid = "hexkagune", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ControlEntities.HUMAN.get(), HumanRenderer::new);
        }
    }
}
