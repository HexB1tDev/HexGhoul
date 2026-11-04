package com.hex.kagune.control.client.render;

import com.hex.kagune.control.ControlEntities;
import com.hex.kagune.control.client.AgentClient;
import com.hex.kagune.control.client.animation.BodyFrame;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.npc.AgentRules;
import com.hex.kagune.control.npc.CcgAgent;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Objects;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * 1.3.27: a CCG investigator as vanilla draws a player, in the owner's skin it was given ({@link AgentRules#LOOKS}), on the arms that
 * skin is painted for: a man a player's size, a woman Eto's body a little taller and skinnier ({@link AgentRules#FEMALE_SIZE},
 * {@link AgentRules#FEMALE_BUILD}: her figure's legs, her bust drawn here on her torso). Posed as it moves ({@link AgentClient}): its run,
 * its quinque in its hand (the item itself drawn by vanilla's hand layer, opening and shutting by its model's frames), its flips (the
 * whole body turned over about its middle), its dashes.
 */
public final class AgentRenderer extends HumanoidMobRenderer<CcgAgent, PlayerModel<CcgAgent>> {
    private static final ResourceLocation[] SKINS = new ResourceLocation[AgentRules.LOOKS.length];

    static {
        for (int i = 0; i < SKINS.length; i++) {
            SKINS[i] = Objects.requireNonNull(ResourceLocation.m_214293_("hexkagune", AgentRules.LOOKS[i].path()));
        }
    }

    /** A man on the wide arms, a man on the slim, a woman (slim, her figure). */
    private final AgentModel wide;
    private final AgentModel slim;
    private final AgentModel female;

    public AgentRenderer(EntityRendererProvider.Context context) {
        this(context, new AgentModel(context.m_174023_(ModelLayers.f_171162_), false, false),
            new AgentModel(context.m_174023_(ModelLayers.f_171166_), true, false), new AgentModel(context.m_174023_(ModelLayers.f_171166_), true, true));
    }

    private AgentRenderer(EntityRendererProvider.Context context, AgentModel wide, AgentModel slim, AgentModel female) {
        super(context, wide, 0.5F);
        this.wide = wide;
        this.slim = slim;
        this.female = female;
        this.m_115326_(new Figure(this));
        this.m_115326_(new Build(this));
    }

    @Override
    public void m_7392_(CcgAgent agent, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        AgentRules.Look look = agent.look();
        this.f_115290_ = look.female() ? this.female : look.slim() ? this.slim : this.wide;
        float delta = ((agent.f_20883_ - agent.f_20884_) % 360.0F + 540.0F) % 360.0F - 180.0F;
        float limbSwing = agent.f_267362_.m_267590_(partial);
        RigPose posed;
        try {
            posed = AgentClient.pose(agent, limbSwing, partial);
        } catch (RuntimeException failure) {
            PlayerLayers.failed("an investigator's pose", failure);
            posed = null;
        }
        AgentModel.pose = posed;
        AgentModel.brawny = agent.lookIndex() == AgentRules.REAPER_LOOK; // 2.0.45: his arms set out round his broader chest
        try {
            AgentClient.afterimages(agent, posed, partial);
        } catch (RuntimeException failure) {
            PlayerLayers.failed("an investigator's afterimage", failure);
        }
        boolean turned = posed != null && (Math.abs(posed.flip) > 1.0E-3F || Math.abs(posed.roll) > 1.0E-3F || Math.abs(posed.spin) > 1.0E-3F
            || Math.abs(posed.drop) > 1.0E-3F || Math.abs(posed.lunge) > 1.0E-3F);
        float size = AgentRules.size(look);
        float wide = size * AgentRules.build(look);
        pose.m_85836_();
        pose.m_85841_(wide, size, wide);
        try {
            if (turned) {
                double body = Math.toRadians(agent.f_20884_ + delta * partial);
                float rightX = (float)-Math.cos(body);
                float rightZ = (float)-Math.sin(body);
                pose.m_85836_();
                pose.m_85837_(-Math.sin(body) * posed.lunge / 16.0, 0.0, Math.cos(body) * posed.lunge / 16.0);
                pose.m_85837_(0.0, -posed.drop / 16.0 + BodyFrame.TURN_PIVOT, 0.0);
                // 1.3.32: a pose's spin turns the body as PlayerAnimator turns a player's (+ to their left, seen from above anticlockwise): it was
                // the other way here, so an NPC parent's airplane spun against the child it carries round (ChildMomentPose.spinning).
                pose.m_252781_(new Quaternionf().rotateAxis(posed.spin, 0.0F, 1.0F, 0.0F));
                pose.m_252781_(new Quaternionf().rotateAxis(posed.flip, rightX, 0.0F, rightZ));
                pose.m_252781_(new Quaternionf().rotateAxis(-posed.roll, rightZ, 0.0F, -rightX));
                pose.m_85837_(0.0, -BodyFrame.TURN_PIVOT, 0.0);
            }
            super.m_7392_(agent, yaw, partial, pose, buffers, light);
        } finally {
            if (turned) {
                pose.m_85849_();
            }
            pose.m_85849_();
            AgentModel.pose = null;
            AgentModel.brawny = false;
            if (this.f_115290_ instanceof AgentModel model) {
                model.settle();
            }
        }
        // 1.3.65: what it says (faltering at War, its nerve back), in a bubble over its head.
        if (!agent.m_20145_()) {
            SpeechBubbles.draw(agent, com.hex.kagune.control.client.NpcSpeech.lines(agent), 0.3,
                com.hex.kagune.control.client.NpcSpeech.age(agent.m_19879_(), partial), pose, buffers);
        }
    }

    @Override
    public ResourceLocation m_5478_(CcgAgent agent) {
        return SKINS[agent.lookIndex()];
    }

    /** A woman's bust on her torso (the shared female figure's, a little less of it), over her own skin. */
    static final class Figure extends RenderLayer<CcgAgent, PlayerModel<CcgAgent>> {
        Figure(AgentRenderer parent) {
            super(parent);
        }

        @Override
        public void m_6494_(PoseStack pose, MultiBufferSource buffers, int light, CcgAgent agent, float limbSwing, float limbSwingAmount, float partialTick,
                            float ageInTicks, float netHeadYaw, float headPitch) {
            if (agent.m_20145_() || !agent.look().female()) {
                return;
            }
            try {
                FemaleFigure.bust(this.m_117386_().f_102810_, pose, buffers.m_6299_(RenderType.m_110473_(SKINS[agent.lookIndex()])), light, 0.0F, false,
                    AgentRules.FEMALE_CURVES);
            } catch (RuntimeException failure) {
                PlayerLayers.failed("an investigator's figure", failure);
            }
        }
    }

    /**
     * 2.0.45: the White Reaper's build ({@link com.hex.kagune.control.npc.ReaperBuild}): his torso's upper part drawn again over it, broader
     * and standing out in front of his belly, in his own skin and its jacket layer, carried by his torso as it is posed.
     */
    static final class Build extends RenderLayer<CcgAgent, PlayerModel<CcgAgent>> {
        Build(AgentRenderer parent) {
            super(parent);
        }

        @Override
        public void m_6494_(PoseStack pose, MultiBufferSource buffers, int light, CcgAgent agent, float limbSwing, float limbSwingAmount, float partialTick,
                            float ageInTicks, float netHeadYaw, float headPitch) {
            if (agent.m_20145_() || agent.lookIndex() != AgentRules.REAPER_LOOK) {
                return;
            }
            try {
                pose.m_85836_();
                this.m_117386_().f_102810_.m_104299_(pose);
                Matrix4f m = pose.m_85850_().m_252922_();
                Matrix3f n = pose.m_85850_().m_252943_();
                VertexConsumer out = buffers.m_6299_(RenderType.m_110473_(SKINS[agent.lookIndex()]));
                com.hex.kagune.control.npc.ReaperBuild.Sink sink = (corners, uv, normal) -> {
                    for (int k = 0; k < 4; k++) {
                        double[] c = corners[k];
                        out.m_252986_(m, (float)(c[0] / 16.0), (float)(c[1] / 16.0), (float)(c[2] / 16.0)).m_6122_(255, 255, 255, 255)
                            .m_7421_((float)(uv[2 * k] / 64.0), (float)(uv[2 * k + 1] / 64.0)).m_86008_(OverlayTexture.f_118083_).m_85969_(light)
                            .m_252939_(n, (float)normal[0], (float)normal[1], (float)normal[2]).m_5752_();
                    }
                };
                com.hex.kagune.control.npc.ReaperBuild.chest(false, sink);
                com.hex.kagune.control.npc.ReaperBuild.chest(true, sink);
                pose.m_85849_();
            } catch (RuntimeException failure) {
                PlayerLayers.failed("the White Reaper's build", failure);
            }
        }
    }

    @Mod.EventBusSubscriber(modid = "hexkagune", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ControlEntities.AGENT.get(), AgentRenderer::new);
        }
    }
}
