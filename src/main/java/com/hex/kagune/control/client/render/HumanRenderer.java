package com.hex.kagune.control.client.render;

import com.hex.kagune.control.ControlEntities;
import com.hex.kagune.control.client.animation.BodyFrame;
import com.hex.kagune.control.client.animation.ChildTalkPose;
import com.hex.kagune.control.client.animation.EtoPose;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.family.ChildCue;
import com.hex.kagune.control.village.HumanGestures;
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
import org.joml.Quaternionf;

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
    /** 1.3.77: each look's blush. */
    private static final java.util.Map<Integer, java.util.Optional<ResourceLocation>> BLUSH = new java.util.HashMap<>();

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

    /** 1.3.77: its gesture now ({@code age}: its age in ticks), or null: each new one played from when it was first seen. */
    static RigPose posed(HumanNpc human, float age) {
        int packed = human.gesture();
        if (packed != human.shownGesture) {
            human.shownGesture = packed;
            human.shownAt = age;
        }
        int code = HumanGestures.code(packed);
        if (code == 0) {
            return null;
        }
        double t = Math.max(0.0, age - human.shownAt);
        if (HumanGestures.cue(code)) {
            ChildCue cue = HumanGestures.childCue(code);
            RigPose p = ChildTalkPose.pose(cue, t, null, false);
            return p == null ? null : p.scale(cue.weight(t));
        }
        return EtoPose.gesture(HumanGestures.gesture(code), t, HumanGestures.style(packed), HumanGestures.count(packed) % 3);
    }

    /** 1.3.77: how long its current gesture has played (ticks), for its blush to come up. */
    static float gestureAge(HumanNpc human, float age) {
        return Math.max(0.0F, age - human.shownAt);
    }

    /** 1.3.76: its eyes shut now and then, a short blink (now and then twice), as Eto's, the companions' and the children's do. */
    static final class Blink extends RenderLayer<HumanNpc, PlayerModel<HumanNpc>> {
        Blink(HumanRenderer parent) {
            super(parent);
        }

        @Override
        public void m_6494_(PoseStack pose, MultiBufferSource buffers, int light, HumanNpc human, float limbSwing, float limbSwingAmount,
                            float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (human.m_20145_()) {
                return;
            }
            try {
                int code = HumanGestures.code(human.gesture());
                // 1.3.77: blushing (a kiss, a hug, shy): pink coming up over its cheeks.
                if (!human.scared() && HumanGestures.blush(code)) {
                    java.util.Optional<ResourceLocation> pink = BLUSH.computeIfAbsent(human.look(), l -> {
                        String path = HumanRules.blush(l);
                        return path == null ? java.util.Optional.empty() : java.util.Optional.of(Objects.requireNonNull(ResourceLocation.m_214293_("hexkagune", path)));
                    });
                    if (pink.isPresent()) {
                        float rise = Math.min(1.0F, gestureAge(human, ageInTicks) / 8.0F);
                        this.m_117386_().m_7695_(pose, buffers.m_6299_(RenderType.m_110473_(pink.get())), light, OverlayTexture.f_118083_, 1.0F, 1.0F, 1.0F, rise);
                    }
                }
                boolean shutNow = human.m_5803_() || !human.scared() && HumanGestures.eyesClosed(code)
                    || com.hex.kagune.control.npc.EtoRules.blink(human.m_19879_() * 31 + 7, ageInTicks);
                if (!shutNow) {
                    return;
                }
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
        // 1.3.77: its gesture (Eto's, in its temper's way, or a child's cue), posed as Eto's model poses hers, its hop and tip too.
        RigPose posed = human.scared() ? null : posed(human, human.f_19797_ + partial);
        Model.pose = posed;
        boolean turned = posed != null && (Math.abs(posed.flip) > 1.0E-3F || Math.abs(posed.roll) > 1.0E-3F || Math.abs(posed.spin) > 1.0E-3F
            || Math.abs(posed.drop) > 1.0E-3F);
        try {
            if (turned) {
                float delta = ((human.f_20883_ - human.f_20884_) % 360.0F + 540.0F) % 360.0F - 180.0F;
                double body = Math.toRadians(human.f_20884_ + delta * partial);
                float rightX = (float)-Math.cos(body);
                float rightZ = (float)-Math.sin(body);
                pose.m_85836_();
                pose.m_85837_(0.0, -posed.drop / 16.0 + BodyFrame.TURN_PIVOT, 0.0);
                pose.m_252781_(new Quaternionf().rotateAxis(posed.spin, 0.0F, 1.0F, 0.0F));
                pose.m_252781_(new Quaternionf().rotateAxis(posed.flip, rightX, 0.0F, rightZ));
                pose.m_252781_(new Quaternionf().rotateAxis(-posed.roll, rightZ, 0.0F, -rightX));
                pose.m_85837_(0.0, -BodyFrame.TURN_PIVOT, 0.0);
            }
            super.m_7392_(human, yaw, partial, pose, buffers, light);
        } finally {
            if (turned) {
                pose.m_85849_();
            }
            Model.pose = null;
        }
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

        /** 1.3.77: the pose of the villager being drawn now (the render thread's alone), or null. */
        static RigPose pose;

        @Override
        public void m_6973_(HumanNpc human, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            super.m_6973_(human, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            RigPose p = pose;
            if (p != null) {
                // 1.3.77: each part posed as a player's is under the same pose (EtoModel's way: RigPose.transform).
                apply("head", this.f_102808_, p);
                apply("torso", this.f_102810_, p);
                apply("rightArm", this.f_102811_, p);
                apply("leftArm", this.f_102812_, p);
                apply("rightLeg", this.f_102813_, p);
                apply("leftLeg", this.f_102814_, p);
                this.f_102809_.m_104315_(this.f_102808_);
                this.f_103375_.m_104315_(this.f_102811_);
                this.f_103374_.m_104315_(this.f_102812_);
                this.f_103377_.m_104315_(this.f_102813_);
                this.f_103376_.m_104315_(this.f_102814_);
                this.f_103378_.m_104315_(this.f_102810_);
            }
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

    /** {@code part} (PlayerAnimator's name {@code name}) moved and turned as the pose has it, from where vanilla put it. */
    private static void apply(String name, ModelPart part, RigPose p) {
        float[] at = p.transform(name, RigPose.POSITION, part.f_104200_, part.f_104201_, part.f_104202_);
        float[] turn = p.transform(name, RigPose.ROTATION, part.f_104203_, part.f_104204_, part.f_104205_);
        part.f_104200_ = at[0];
        part.f_104201_ = at[1];
        part.f_104202_ = at[2];
        part.f_104203_ = turn[0];
        part.f_104204_ = turn[1];
        part.f_104205_ = turn[2];
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
