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
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Home Living (1.3.40): The Human as vanilla draws a player, in one of vanilla's own default skins ({@link HumanRules#LOOKS}, wide or
 * slim arms as the skin is painted for); frightened, it runs with its arms thrown up over its head, flailing. 1.3.65: its screams in a
 * bubble over its head.
 */
public final class HumanRenderer extends HumanoidMobRenderer<HumanNpc, PlayerModel<HumanNpc>> {
    private static final ResourceLocation[] SKINS = new ResourceLocation[HumanRules.LOOKS.length * 2];
    private static final java.util.Map<Integer, ResourceLocation> CUTE = new java.util.HashMap<>();
    /** 1.3.76: each look's shut eyes (an empty Optional: none). */
    private static final java.util.Map<Integer, java.util.Optional<ResourceLocation>> SHUT = new java.util.HashMap<>();

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
        this.m_115326_(new Blink(this));
    }

    /** 1.3.76: its eyes shut now and then, a short blink (now and then twice), as Eto's, the companions' and the children's do. */
    static final class Blink extends RenderLayer<HumanNpc, PlayerModel<HumanNpc>> {
        Blink(HumanRenderer parent) {
            super(parent);
        }

        @Override
        public void m_6494_(PoseStack pose, MultiBufferSource buffers, int light, HumanNpc human, float limbSwing, float limbSwingAmount,
                            float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (human.m_20145_() || !(human.m_5803_() || com.hex.kagune.control.npc.EtoRules.blink(human.m_19879_() * 31 + 7, ageInTicks))) {
                return;
            }
            try {
                java.util.Optional<ResourceLocation> shut = SHUT.computeIfAbsent(human.look(), l -> {
                    String path = HumanRules.closed(l);
                    return path == null ? java.util.Optional.empty() : java.util.Optional.of(Objects.requireNonNull(ResourceLocation.m_214293_("hexkagune", path)));
                });
                if (shut.isPresent()) {
                    this.m_117386_().m_7695_(pose, buffers.m_6299_(RenderType.m_110473_(shut.get())), light, OverlayTexture.f_118083_, 1.0F, 1.0F, 1.0F, 1.0F);
                }
            } catch (RuntimeException failure) {
                com.hex.kagune.control.Safety.caught(failure, "a villager's blink");
            }
        }
    }

    @Override
    public void m_7392_(HumanNpc human, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        this.f_115290_ = HumanRules.slim(human.look()) ? this.slim : this.wide;
        super.m_7392_(human, yaw, partial, pose, buffers, light);
        // 1.3.65: its screams, in a bubble over its head.
        if (!human.m_20145_()) {
            SpeechBubbles.draw(human, com.hex.kagune.control.client.NpcSpeech.lines(human), 0.3,
                com.hex.kagune.control.client.NpcSpeech.age(human.m_19879_(), partial), pose, buffers);
        }
    }

    @Override
    public ResourceLocation m_5478_(HumanNpc human) {
        int look = human.look();
        if (look >= HumanRules.CUTE) {
            // 1.3.75: a villager's own skin (HumanSkins).
            return CUTE.computeIfAbsent(look, l -> Objects.requireNonNull(ResourceLocation.m_214293_(HumanRules.namespace(l), HumanRules.skin(l))));
        }
        return SKINS[Math.floorMod(look, SKINS.length)];
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
