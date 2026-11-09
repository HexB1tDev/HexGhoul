package com.hex.kagune.control.client.render;

import com.hex.kagune.control.ControlEntities;
import com.hex.kagune.control.client.PaleClient;
import com.hex.kagune.control.client.GhoulNpcClient;
import com.hex.kagune.control.client.animation.BodyFrame;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.kakugan.KakuganModel;
import com.hex.kagune.control.npc.GhoulNpc;
import com.hex.kagune.control.npc.NpcLook;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;

/**
 * 7.0.2: a ghoul NPC as vanilla draws a player (the player model) in its skin ({@link NpcLook}), with the kakugan opening on its face
 * as its kagune comes out. Its kagune is drawn after entities with the players' ({@link GraftRenderer} for the tails,
 * {@link UkakuWingRenderer} for the wings): this renderer notes every NPC it draws in a frame ({@link #drawn}), so only the ones in
 * view are drawn. 1.0.41: in its own skin, a girl on the slim arms ({@link GhoulModel} either way), the kakugan on that skin's eyes,
 * and its flips: the whole body turned over about its middle, the limbs posed as a player's are.
 */
public final class GhoulNpcRenderer extends HumanoidMobRenderer<GhoulNpc, PlayerModel<GhoulNpc>> {
    private static final ResourceLocation[] SKINS = new ResourceLocation[NpcLook.SKINS.length];
    /** The NPCs drawn this frame (their kagune is drawn after entities, then the list is cleared). */
    private static final Set<GhoulNpc> DRAWN = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final double DETAIL_DISTANCE_SQR = 24.0 * 24.0;

    static {
        for (int i = 0; i < SKINS.length; i++) {
            SKINS[i] = Objects.requireNonNull(ResourceLocation.m_214293_("hexkagune", NpcLook.texture(i)));
        }
    }

    /** 1.0.41: the body on the wide arms (a boy's) and on the slim (a girl's). */
    private final GhoulModel wide;
    private final GhoulModel slim;

    public GhoulNpcRenderer(EntityRendererProvider.Context context) {
        this(context, new GhoulModel(context.m_174023_(ModelLayers.f_171162_), false), new GhoulModel(context.m_174023_(ModelLayers.f_171166_), true));
    }

    private GhoulNpcRenderer(EntityRendererProvider.Context context, GhoulModel wide, GhoulModel slim) {
        super(context, wide, 0.5F);
        this.wide = wide;
        this.slim = slim;
        this.m_115326_(new Eyes(this));
        this.m_115326_(new BladeArmLayer(this));
    }

    /**
     * 1.0.41: drawn on the arms its skin is painted for, posed as it is (a flip: the limbs as a player's, and the whole body turned over
     * about its middle, about the line through its shoulders, the way it faces).
     */
    @Override
    public void m_7392_(GhoulNpc npc, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        // 2.0.27: noted as drawn before anything else (not only from its eyes' layer: a layer skipped or failing left its kagune undrawn).
        note(npc);
        GhoulNpcClient.View view = GhoulNpcClient.get(npc.m_19879_());
        this.f_115290_ = view != null && view.identity().girl() ? this.slim : this.wide;
        double clock = PaleClient.clock(partial);
        RigPose posed = GhoulNpcClient.flip(view, clock, npc.m_20096_());
        if (posed == null) {
            // 1.3.36: an arm blade's guard, cuts, wrap and sprint.
            try {
                double speed = Math.hypot(npc.m_20185_() - npc.f_19790_, npc.m_20189_() - npc.f_19792_);
                posed = GhoulNpcClient.blade(view, clock, speed, npc.f_267362_.m_267590_(partial) * 0.6662);
            } catch (RuntimeException failure) {
                PlayerLayers.failed("a ghoul's arm blade pose", failure);
                posed = null;
            }
        }
        GhoulModel.pose = posed;
        boolean shaking = npc.afraid(); // 2.1.9: trembling before Peace or War
        if (shaking) {
            float shiver = com.hex.kagune.control.npc.FinalSight.tremble(npc.m_19879_(), npc.f_19797_ + partial);
            pose.m_85836_();
            pose.m_252781_(new Quaternionf().rotateAxis(shiver, 0.0F, 1.0F, 0.0F));
            pose.m_252781_(new Quaternionf().rotateAxis(shiver * 0.5F, 0.0F, 0.0F, 1.0F));
        }
        boolean turned = posed != null && (Math.abs(posed.flip) > 1.0E-3F || Math.abs(posed.roll) > 1.0E-3F || Math.abs(posed.spin) > 1.0E-3F
            || Math.abs(posed.drop) > 1.0E-3F || Math.abs(posed.lunge) > 1.0E-3F);
        try {
            if (turned) {
                float delta = ((npc.f_20883_ - npc.f_20884_) % 360.0F + 540.0F) % 360.0F - 180.0F;
                double body = Math.toRadians(npc.f_20884_ + delta * partial);
                float rightX = (float)-Math.cos(body);
                float rightZ = (float)-Math.sin(body);
                pose.m_85836_();
                // 1.3.36: a step in and a drop carry the whole body, and a spin turns it (as the investigators' do).
                pose.m_85837_(-Math.sin(body) * posed.lunge / 16.0, 0.0, Math.cos(body) * posed.lunge / 16.0);
                pose.m_85837_(0.0, -posed.drop / 16.0 + BodyFrame.TURN_PIVOT, 0.0);
                pose.m_252781_(new Quaternionf().rotateAxis(posed.spin, 0.0F, 1.0F, 0.0F));
                // A backflip (+) takes the head back over: about its right; a roll (+) lifts its right side: about its back-to-front line.
                pose.m_252781_(new Quaternionf().rotateAxis(posed.flip, rightX, 0.0F, rightZ));
                pose.m_252781_(new Quaternionf().rotateAxis(-posed.roll, rightZ, 0.0F, -rightX));
                pose.m_85837_(0.0, -BodyFrame.TURN_PIVOT, 0.0);
            }
            super.m_7392_(npc, yaw, partial, pose, buffers, light);
        } finally {
            if (turned) {
                pose.m_85849_();
            }
            if (shaking) {
                pose.m_85849_();
            }
            GhoulModel.pose = null;
        }
        // 1.3.30: what it says, in a bubble over its head (never the chat). 1.3.38: a real bubble (SpeechBubbles).
        java.util.List<String> said = com.hex.kagune.control.client.NpcSpeech.lines(npc);
        if (!npc.m_20145_()) {
            SpeechBubbles.draw(npc, said, 0.3, com.hex.kagune.control.client.NpcSpeech.age(npc.m_19879_(), partial), pose, buffers);
        }
    }

    @Override
    public ResourceLocation m_5478_(GhoulNpc npc) {
        GhoulNpcClient.View view = GhoulNpcClient.get(npc.m_19879_());
        return SKINS[view == null ? 0 : view.identity().skin()];
    }

    /** 2.0.27: {@code npc} noted as drawn this frame (its kagune is drawn after entities). */
    private static void note(GhoulNpc npc) {
        // 3.6.3: not while it is being devoured (Grand Devouring): its kagune off at once.
        if (DRAWN.size() < 256 && com.hex.kagune.control.client.BikakuClient.devoured(npc.m_19879_(), PaleClient.clock(0.0F)) < 0.0) {
            DRAWN.add(npc);
        }
    }

    /** How near a living ghoul with its kagune out has its kagune drawn even if its body was not noted this frame (2.0.27). */
    private static final double KAGUNE_ALWAYS = 48.0;

    /**
     * The NPCs drawn so far this frame. 2.0.27 (the owner: wild ghouls fight "but they're not manifesting the kagune ... It's invisible"):
     * and every living ghoul within {@link #KAGUNE_ALWAYS} of the player with its kagune coming out or out, even if its body's renderer
     * never noted it (a renderer or layer skipped by another mod or a failure): the kagune no longer hangs on the body's draw.
     */
    /** 2.5.6: this frame's list, once worked out (null: not yet). */
    private static List<GhoulNpc> FRAME;

    static List<GhoulNpc> drawn() {
        // 2.5.6 (lag): worked out once a frame (the tails' and the wings' passes both read it), forgotten with the frame's list.
        if (FRAME != null) {
            return FRAME;
        }
        List<GhoulNpc> out = new ArrayList<>(DRAWN);
        FRAME = out;
        try {
            Minecraft minecraft = Minecraft.m_91087_();
            if (minecraft.f_91073_ != null && minecraft.f_91074_ != null) {
                double clock = PaleClient.clock(0.0F);
                Set<GhoulNpc> seen = Collections.newSetFromMap(new IdentityHashMap<>());
                seen.addAll(out);
                for (GhoulNpc npc : minecraft.f_91073_.m_6443_(GhoulNpc.class, minecraft.f_91074_.m_20191_().m_82400_(KAGUNE_ALWAYS),
                    living -> living.m_6084_() && !living.m_20145_())) {
                    if (out.size() >= 256) {
                        break;
                    }
                    if (!seen.contains(npc) && GhoulNpcClient.deploy(GhoulNpcClient.get(npc.m_19879_()), clock) > 0.005F) {
                        out.add(npc);
                    }
                }
            }
        } catch (RuntimeException failure) {
            PlayerLayers.failed("the wild ghouls near you", failure);
        }
        return out;
    }

    /** After both kagune renderers: the frame's list cleared, and now and then the NPCs gone from the world forgotten. */
    @Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
    public static final class Frame {
        private Frame() {
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void after(RenderLevelStageEvent event) {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
                return;
            }
            DRAWN.clear();
            FRAME = null;
            ChildRenderer.cleared(); // 1.3.43: the children's too
            Minecraft minecraft = Minecraft.m_91087_();
            if (minecraft.f_91073_ != null && minecraft.f_91073_.m_46467_() % 100L == 0L) {
                GhoulNpcClient.prune(minecraft.f_91073_);
            }
        }
    }

    /**
     * 1.3.36: an arm blade on its right arm ({@code BladeArmModel}, in its own colour: KaguneTint.wild), wound out as far as it is, riding
     * the arm as drawn; its glowing seams full bright.
     */
    static final class BladeArmLayer extends RenderLayer<GhoulNpc, PlayerModel<GhoulNpc>> {
        private static final RenderType TYPE = RenderType.m_110473_(Objects.requireNonNull(ResourceLocation.m_214293_("minecraft",
            "textures/block/white_concrete.png")));
        private static final int FULL_BRIGHT = 0xF000F0;

        BladeArmLayer(RenderLayerParent<GhoulNpc, PlayerModel<GhoulNpc>> parent) {
            super(parent);
        }

        @Override
        public void m_6494_(PoseStack pose, MultiBufferSource buffers, int packedLight, GhoulNpc npc, float limbSwing, float limbSwingAmount,
                            float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (npc.m_20145_() || com.hex.kagune.control.client.BikakuClient.devoured(npc.m_19879_(), PaleClient.clock(partialTick)) >= 0.0) {
                return; // 3.6.3: devoured (Grand Devouring): its kagune off at once
            }
            try {
                GhoulNpcClient.View view = GhoulNpcClient.get(npc.m_19879_());
                float wrap = GhoulNpcClient.wrap(view, PaleClient.clock(partialTick));
                if (wrap <= 0.0F) {
                    return;
                }
                int colour = com.hex.kagune.control.KaguneTint.wild(view.identity().seed());
                pose.m_85836_();
                this.m_117386_().f_102811_.m_104299_(pose);
                pose.m_85841_(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
                org.joml.Matrix4f matrix = pose.m_85850_().m_252922_();
                org.joml.Matrix3f normals = pose.m_85850_().m_252943_();
                com.mojang.blaze3d.vertex.VertexConsumer out = buffers.m_6299_(TYPE);
                com.hex.kagune.control.npc.BladeArmModel.build(wrap, view.identity().girl(), colour, (a, b, c, d, n, ca, cb, cc, cd) -> {
                    boolean glow = n == com.hex.kagune.control.npc.BladeArmModel.GLOW;
                    int lit = glow ? FULL_BRIGHT : packedLight;
                    double[][] corners = {a, b, c, d};
                    int[] colours = {ca, cb, cc, cd};
                    for (int k = 0; k < 4; k++) {
                        int argb = colours[k];
                        out.m_252986_(matrix, (float)corners[k][0], (float)corners[k][1], (float)corners[k][2])
                            .m_6122_((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, argb >>> 24)
                            .m_7421_(0.5F, 0.5F)
                            .m_86008_(OverlayTexture.f_118083_)
                            .m_85969_(lit)
                            .m_252939_(normals, (float)n[0], (float)n[1], (float)n[2])
                            .m_5752_();
                    }
                });
                pose.m_85849_();
            } catch (RuntimeException failure) {
                PlayerLayers.failed("a ghoul's arm blade", failure);
            }
        }
    }

    /** The kakugan on its face as its kagune is out (as {@link KakuganLayer} draws a player's), and it noted as drawn. */
    static final class Eyes extends RenderLayer<GhoulNpc, PlayerModel<GhoulNpc>> {
        Eyes(RenderLayerParent<GhoulNpc, PlayerModel<GhoulNpc>> parent) {
            super(parent);
        }

        @Override
        public void m_6494_(PoseStack pose, MultiBufferSource buffers, int packedLight, GhoulNpc npc, float limbSwing, float limbSwingAmount,
                            float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            try {
                note(npc);
                GhoulNpcClient.View view = GhoulNpcClient.get(npc.m_19879_());
                float intensity = GhoulNpcClient.deploy(view, PaleClient.clock(partialTick));
                if (view != null && view.eyes()) {
                    intensity = 1.0F; // 1.3.68: a wild ghoul prowling at night: its kakugan on, its kagune not yet
                }
                if (intensity <= 0.003F || npc.m_20145_()) {
                    return;
                }
                Vec3 camera = Minecraft.m_91087_().f_91063_.m_109153_().m_90583_();
                double dx = npc.m_20185_() - camera.f_82479_;
                double dy = npc.m_20186_() + 1.6 - camera.f_82480_;
                double dz = npc.m_20189_() - camera.f_82481_;
                double distanceSqr = dx * dx + dy * dy + dz * dz;
                List<KakuganModel.Quad> quads = KakuganModel.build(NpcLook.eyes(view.identity().skin()), npc.m_19879_() * 31 + 7, ageInTicks / 20.0, intensity,
                    distanceSqr < DETAIL_DISTANCE_SQR, KakuganModel.layerFor(distanceSqr), 0L);
                if (quads.isEmpty()) {
                    return;
                }
                pose.m_85836_();
                this.m_117386_().f_102808_.m_104299_(pose);
                KakuganLayer.emit(buffers.m_6299_(KakuganLayer.SOLID), quads, false, packedLight, pose.m_85850_().m_252922_(), pose.m_85850_().m_252943_());
                KakuganLayer.emit(buffers.m_6299_(KakuganLayer.GLOW), quads, true, 0xF000F0, pose.m_85850_().m_252922_(), pose.m_85850_().m_252943_());
                pose.m_85849_();
            } catch (RuntimeException failure) {
                PlayerLayers.failed("ghoul eyes", failure);
            }
        }
    }

    @Mod.EventBusSubscriber(modid = "hexkagune", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ControlEntities.GHOUL.get(), GhoulNpcRenderer::new);
            // 1.2.1: Peace's flesh creatures' too, as FleshThrallRenderer does (in some setups that one was never made): twice can't hurt.
            event.registerEntityRenderer(ControlEntities.THRALL.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
            // 1.3.1: Eto's too, as EtoRenderer does (the owner's crash: she had none); ThrallRenderGuard mends it if still none.
            event.registerEntityRenderer(ControlEntities.ETO.get(), EtoRenderer::new);
        }
    }
}
