package com.hex.kagune.control.client.render;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.control.bikaku.GrandDevour;
import com.hex.kagune.control.client.BikakuClient;
import com.hex.kagune.control.client.PaleClient;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.Color;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 3.6.5, Grand Devouring from inside (the owner: "If the victim is another player, they must experience a special first-person perspective
 * from inside the kagune's mouth"; "the inside of the enormous fleshy cavity, including the thick organic walls, multiple rows of teeth,
 * moving tissue, and the throat extending deeper into darkness"; "visibly witness the jaws slowly closing around them, gradually blocking
 * the outside world"; "The fleshy walls should pulsate, contract, and move around the victim while the mouth slowly pulls them deeper
 * inside"; "The victim's first-person experience must match the actual execution animation happening in the world"; on its first draft:
 * "I like the inside of the mouth, proceed wit hthe mod").
 *
 * <p>A GeckoLib model ({@code geo/devour_cavity.geo.json}, made by {@code tools/devour_cavity.py}): rings of flesh round the cavity with
 * rings of fangs, the throat on up into a dark cap, a flap of lip per eighth of the rim. Its own keyframed animation
 * ({@code animations/devour_cavity.animation.json}) breathes the walls, a ring at a time, and works the throat; code turns the jaws' hinges
 * by the worm's own aperture ({@link GrandDevour}: open, they hang out past the rim; shut, they lie across the hole) and squeezes the rings
 * one after another up the throat at the gulp. It is drawn, in his own first person only, where the feeding worm's head is in the world
 * this frame ({@link BikakuRenderer#feedingMouth}: the very curve the worm is drawn along), along its head, scaled to its cavity a little
 * inside it, its flesh in the devourer's kagune colour, its teeth bone. Client only.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class DevourInside {
    static final ResourceLocation MODEL = KaguneMod.id("geo/devour_cavity.geo.json");
    static final ResourceLocation TEXTURE = KaguneMod.id("textures/geo/devour_cavity.png");
    static final ResourceLocation ANIMATION = KaguneMod.id("animations/devour_cavity.animation.json");
    /** The model's canonical cavity: its radius and its walls' height (blocks, 16 pixels to a block). */
    static final double MODEL_RADIUS = 1.0;
    static final double MODEL_DEPTH = 2.25;
    /** A little inside the worm's own cavity, so its walls never fight the world worm's. */
    static final double INSIDE = 0.95;
    /** The hinges' turn when the jaws are wide (radians; GeckoLib's sense, negative: the flap's inner end swings down): out and down past the rim. */
    static final float WIDE = (float)Math.toRadians(-100.0);
    /** How many rings of wall and of throat the gulp squeezes. */
    static final int RINGS = 6;
    static final int THROAT = 3;

    private static Renderer renderer;
    private static final Cavity CAVITY = new Cavity();

    private DevourInside() {
    }

    /** The one cavity this client draws (only he is ever inside one here): what the model reads this frame. */
    static final class Cavity implements GeoAnimatable {
        static final RawAnimation PULSE = RawAnimation.begin().thenLoop("animation.hexkagune.devour_cavity.pulse");
        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
        /** Its jaws (1 wide, 0 shut), the gulp (0 none .. 1 through), the devourer's kagune colour, the clock. */
        double open = 1.0;
        double gulp;
        int tint = 0xFFFFFF;
        double clock;

        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
            controllers.add(new AnimationController<>(this, "pulse", 0, state -> state.setAndContinue(PULSE)));
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return this.cache;
        }

        @Override
        public double getTick(Object object) {
            return this.clock;
        }
    }

    /** Its resources, and its jaws and gulp driven from code. */
    static final class Model extends GeoModel<Cavity> {
        @Override
        public ResourceLocation getModelResource(Cavity cavity) {
            return MODEL;
        }

        @Override
        public ResourceLocation getTextureResource(Cavity cavity) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(Cavity cavity) {
            return ANIMATION;
        }

        @Override
        public void setCustomAnimations(Cavity cavity, long instanceId, AnimationState<Cavity> state) {
            float hinge = WIDE * (float)Math.max(0.0, Math.min(1.0, cavity.open));
            for (int j = 0; j < 8; j++) {
                this.getBone("hinge" + j).ifPresent(bone -> bone.setRotX(hinge)); // its rest turn is none
            }
            if (cavity.gulp > 0.0) {
                // The gulp: each ring squeezed in turn, from the mouth on up the throat.
                for (int k = 0; k < RINGS + THROAT; k++) {
                    double at = cavity.gulp * (RINGS + THROAT + 2) - k;
                    float squeeze = (float)(1.0 - 0.3 * Math.max(0.0, Math.sin(Math.PI * Math.max(0.0, Math.min(1.0, at / 2.0)))));
                    this.getBone(k < RINGS ? "wall" + k : "throat" + (k - RINGS)).ifPresent(bone -> {
                        bone.setScaleX(bone.getScaleX() * squeeze);
                        bone.setScaleZ(bone.getScaleZ() * squeeze);
                    });
                }
            }
        }
    }

    /** Its flesh tinted the devourer's kagune colour; its teeth bone. */
    static final class Renderer extends GeoObjectRenderer<Cavity> {
        Renderer() {
            super(new Model());
        }

        @Override
        public Color getRenderColor(Cavity cavity, float partialTick, int packedLight) {
            return Color.ofRGB(cavity.tint >> 16 & 0xFF, cavity.tint >> 8 & 0xFF, cavity.tint & 0xFF);
        }

        @Override
        public void renderCubesOfBone(PoseStack poseStack, GeoBone bone, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green,
                                      float blue, float alpha) {
            String name = bone.getName();
            boolean teeth = name.startsWith("tooth") || name.startsWith("fang");
            super.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, teeth ? 1.0F : red, teeth ? 1.0F : green, teeth ? 1.0F : blue,
                alpha);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        ClientLevel level = minecraft.f_91073_;
        LocalPlayer me = minecraft.f_91074_;
        if (level == null || me == null || !minecraft.f_91066_.m_92176_().m_90612_()) {
            return; // first person only: in third person his camera is outside, and he sees the world worm
        }
        float partial = event.getPartialTick();
        double clock = PaleClient.clock(partial);
        AbstractClientPlayer devourer = BikakuClient.devourer(me.m_19879_(), clock);
        if (devourer == null) {
            return;
        }
        try {
            draw(event.getPoseStack(), minecraft, level, devourer, partial, clock);
        } catch (RuntimeException failure) {
            PlayerLayers.failed("the inside of a Bikaku worm's mouth", failure);
        }
    }

    private static void draw(PoseStack stack, Minecraft minecraft, ClientLevel level, AbstractClientPlayer devourer, float partial, double clock) {
        BikakuRenderer.Mouth mouth = BikakuRenderer.feedingMouth(devourer, partial, clock);
        if (mouth == null || mouth.age() < GrandDevour.CAUGHT - GrandDevour.ENGULF * 0.35 || mouth.age() >= GrandDevour.KILL) {
            return; // from as its mouth comes down round his eyes until he is swallowed
        }
        if (renderer == null) {
            renderer = new Renderer();
        }
        CAVITY.open = mouth.aperture();
        CAVITY.gulp = mouth.age() < GrandDevour.SHUT ? 0.0 : Math.min(1.0, (mouth.age() - GrandDevour.SHUT) / GrandDevour.SWALLOW);
        CAVITY.tint = mouth.tint();
        CAVITY.clock = clock;
        Vec3 camera = minecraft.f_91063_.m_109153_().m_90583_();
        double[] at = mouth.mouth();
        double[] back = mouth.back();
        double ax = back[0] - at[0];
        double ay = back[1] - at[1];
        double az = back[2] - at[2];
        double length = Math.sqrt(ax * ax + ay * ay + az * az);
        if (length < 1.0E-4) {
            return;
        }
        ax /= length;
        ay /= length;
        az /= length;
        int light = LevelRenderer.m_109541_(level, BlockPos.m_274561_(at[0], at[1] + mouth.depth() * 0.5, at[2]));
        stack.m_85836_();
        stack.m_85837_(at[0] - camera.f_82479_, at[1] - camera.f_82480_, at[2] - camera.f_82481_);
        // The model's up (into the throat) along the worm's head, from its mouth to the back of its head: turned about up x the axis.
        double turnX = az;
        double turnZ = -ax;
        double sideways = Math.sqrt(turnX * turnX + turnZ * turnZ);
        float angle = (float)Math.atan2(sideways, ay);
        if (sideways > 1.0E-6) {
            stack.m_252781_(new Quaternionf().rotateAxis(angle, (float)(turnX / sideways), 0.0F, (float)(turnZ / sideways)));
        } else if (ay < 0.0) {
            stack.m_252781_(new Quaternionf().rotateAxis((float)Math.PI, 1.0F, 0.0F, 0.0F));
        }
        float across = (float)(mouth.hole() / MODEL_RADIUS * INSIDE);
        stack.m_85841_(across, (float)(mouth.depth() / MODEL_DEPTH), across);
        // GeoObjectRenderer.preRender moves the model to a block's middle (0.5, 0.51, 0.5): taken back.
        stack.m_252880_(-0.5F, -0.51F, -0.5F);
        MultiBufferSource.BufferSource buffers = minecraft.m_91269_().m_110104_();
        RenderType type = RenderType.m_110473_(TEXTURE);
        renderer.render(stack, CAVITY, buffers, type, buffers.m_6299_(type), light);
        buffers.m_109912_(type);
        stack.m_85849_();
    }
}
