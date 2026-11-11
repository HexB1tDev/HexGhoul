package com.hex.kagune.control.client.render;

import com.hex.kagune.control.ControlEntities;
import com.hex.kagune.control.client.EtoClient;
import com.hex.kagune.control.client.PaleClient;
import com.hex.kagune.control.client.animation.BodyFrame;
import com.hex.kagune.control.client.animation.EtoFight;
import com.hex.kagune.control.client.animation.EtoPose;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.npc.EtoGesture;
import com.hex.kagune.control.npc.EtoHand;
import com.hex.kagune.control.npc.EtoNpc;
import com.hex.kagune.control.npc.EtoRules;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * 1.3.0: Eto as vanilla draws a player, on the slim arms in her own skin ({@code tools/eto_textures.py}), her figure ({@link EtoModel}:
 * wider hips, and a bust drawn on her torso here), posed as she is ({@link EtoPose}: her gestures, her moves, her flips). Over the skin:
 * her kakugan (the right eye turned, veins and all) fading in at night and in a fight with its iris glowing, her eyes shut "- -" when she
 * blinks or leans in for a kiss, her cheeks pink when she blushes, and the anger mark over her head when she was left hanging.
 */
public final class EtoRenderer extends HumanoidMobRenderer<EtoNpc, PlayerModel<EtoNpc>> {
    /** 1.3.14: the anger mark, the same for every companion. */
    static final ResourceLocation ANGRY = texture("eto", "eto_angry");

    /** 1.3.14: whose body this renderer draws, and its textures ({@code textures/entity/<key>/<key>...}). */
    final com.hex.kagune.control.npc.Persona persona;
    final ResourceLocation skin;
    final ResourceLocation kakugan;
    final ResourceLocation glow;
    final ResourceLocation closed;
    final ResourceLocation blush;

    private static ResourceLocation texture(String folder, String name) {
        return Objects.requireNonNull(ResourceLocation.m_214293_("hexkagune", "textures/entity/" + folder + "/" + name + ".png"));
    }

    public EtoRenderer(EntityRendererProvider.Context context) {
        this(context, com.hex.kagune.control.npc.Persona.ETO);
    }

    /** 1.3.14: any companion: a woman's figure on the slim arms, or a man's on the wide ones. */
    public EtoRenderer(EntityRendererProvider.Context context, com.hex.kagune.control.npc.Persona persona) {
        super(context, new EtoModel(context.m_174023_(persona.slim() ? ModelLayers.f_171166_ : ModelLayers.f_171162_), persona.slim(), persona.female(),
            persona.legs(), persona.muscle()),
            0.5F * persona.size());
        this.persona = persona;
        if (persona.slim()) {
            OutfitLayer.slim(this.f_115290_); // 4.0.0: anything worn, its sleeves for slim arms
        }
        String key = persona.key();
        this.skin = texture(key, key);
        this.kakugan = texture(key, key + "_kakugan");
        this.glow = texture(key, key + "_kakugan_glow");
        this.closed = texture(key, key + "_closed");
        this.blush = texture(key, key + "_blush");
        this.m_115326_(new Face(this));
        this.m_115326_(new Kakuja(this));
        this.m_115326_(new OutfitLayer<>(this)); // 4.0.0: a companion's mask (Noro's always; Ayato's in a fight) and anything worn
        if (persona.muscle() > 0.0) {
            this.m_115326_(new Build(this, this.skin, com.hex.kagune.control.npc.ReaperBuild.of(persona.muscle())));
        }
    }

    /**
     * 4.0.8 (the owner: "make him a little muscular but way less than the male player"): a man's build (Ayato's), as a male player's and the
     * White Reaper's are drawn ({@link com.hex.kagune.control.npc.ReaperBuild}): his torso's upper part again over it, broader across the
     * shoulders and chest, in his own skin and its jacket layer, carried by his torso as it is posed.
     */
    static final class Build extends RenderLayer<EtoNpc, PlayerModel<EtoNpc>> {
        private final ResourceLocation skin;
        private final double[][] profile;

        Build(EtoRenderer parent, ResourceLocation skin, double[][] profile) {
            super(parent);
            this.skin = skin;
            this.profile = profile;
        }

        @Override
        public void m_6494_(PoseStack pose, MultiBufferSource buffers, int light, EtoNpc npc, float limbSwing, float limbSwingAmount, float partialTick,
                            float ageInTicks, float netHeadYaw, float headPitch) {
            if (npc.m_20145_() || !this.m_117386_().f_102810_.f_104207_) {
                return;
            }
            try {
                pose.m_85836_();
                this.m_117386_().f_102810_.m_104299_(pose);
                Matrix4f m = pose.m_85850_().m_252922_();
                Matrix3f n = pose.m_85850_().m_252943_();
                VertexConsumer out = buffers.m_6299_(RenderType.m_110473_(this.skin));
                com.hex.kagune.control.npc.ReaperBuild.Sink sink = (corners, uv, normal) -> {
                    for (int k = 0; k < 4; k++) {
                        double[] c = corners[k];
                        out.m_252986_(m, (float)(c[0] / 16.0), (float)(c[1] / 16.0), (float)(c[2] / 16.0)).m_6122_(255, 255, 255, 255)
                            .m_7421_((float)(uv[2 * k] / 64.0), (float)(uv[2 * k + 1] / 64.0)).m_86008_(OverlayTexture.f_118083_).m_85969_(light)
                            .m_252939_(n, (float)normal[0], (float)normal[1], (float)normal[2]).m_5752_();
                    }
                };
                com.hex.kagune.control.npc.ReaperBuild.chest(this.profile, false, sink);
                com.hex.kagune.control.npc.ReaperBuild.chest(this.profile, true, sink);
                pose.m_85849_();
            } catch (RuntimeException failure) {
                PlayerLayers.failed("a companion's build", failure);
            }
        }
    }

    /** 1.3.19: a final kakuja worn (Ken's Peace) on the body's posed parts, as a player's is drawn. */
    static final class Kakuja extends RenderLayer<EtoNpc, PlayerModel<EtoNpc>> {
        Kakuja(EtoRenderer parent) {
            super(parent);
        }

        @Override
        public void m_6494_(PoseStack pose, MultiBufferSource buffers, int light, EtoNpc npc, float limbSwing, float limbSwingAmount, float partialTick,
                            float ageInTicks, float netHeadYaw, float headPitch) {
            if (com.hex.kagune.control.client.KingClient.get(npc) == null) {
                return;
            }
            try {
                KingLayer.drawPeaceBody(pose, buffers, light, npc, this.m_117386_(), partialTick);
            } catch (RuntimeException failure) {
                com.hex.kagune.control.Safety.caught(failure, "a companion's final kakuja");
            }
        }
    }

    /** 4.1.16: each companion's blush (by entity id). */
    static final java.util.Map<Integer, com.hex.kagune.control.client.animation.KissBlush> BLUSHES = new java.util.HashMap<>();

    /** 4.1.15: each companion's pose carried from one gesture into the next (by entity id). */
    static final java.util.Map<Integer, com.hex.kagune.control.client.animation.PoseFade> FADES = new java.util.HashMap<>();

    /** Her pose this frame: her gesture, with her fight's move (1.3.4: a Pale player's, by its own animator) over it (null: none). */
    static RigPose pose(EtoNpc npc, EtoClient.View view, double clock, float partial) {
        if (view == null) {
            return null;
        }
        // 1.3.29: each her own way (Persona.style), and a kiss not the same kiss every time (by when it began).
        RigPose own = EtoPose.gesture(EtoPose.shown(view.gesture(), view.with()), view.gestureAge(clock), npc.persona().style(),
            (int)Math.floorMod(view.gestureAt(), 3L)); // 3.2.3: a blush with nobody to kiss is a shy one
        // 2.2.4: the kiss as her mood gives it (MoodKiss): angry, possessive, giddy, tender, desperate, sleepy, hungry, teasing.
        own = com.hex.kagune.control.client.animation.MoodKissPose.apply(own, view.kiss(), view.gesture(), view.gestureAge(clock),
            npc.persona().style()); // 2.3.5: each companion's own way of it
        // 1.3.30: a child in its arms: cradled the whole time it is held (never faded out with its gesture's time), easing in as it is picked up.
        if (com.hex.kagune.control.client.ChildClient.carrying(npc.m_19879_())) {
            double age = view.gesture() == com.hex.kagune.control.npc.EtoGesture.PET ? view.gestureAge(clock) : Double.MAX_VALUE;
            own = EtoPose.cradle(age, view.gestureAge(clock));
        }
        // 4.1.15 (the owner: "better the animation"): from one gesture into the next without dropping back upright in between.
        if (FADES.size() > 256) {
            FADES.clear();
        }
        own = FADES.computeIfAbsent(npc.m_19879_(), id -> new com.hex.kagune.control.client.animation.PoseFade()).through(own, view.gestureAt(), clock);
        return EtoFight.pose(npc, view.fight(), moment(npc, alive(npc, view, own, partial), clock), clock, partial);
    }

    /** 1.3.20: a moment with one of its children (a hug, a toss, a spin...) over its gesture: its parent's side of it. */
    static RigPose moment(EtoNpc npc, RigPose under, double clock) {
        com.hex.kagune.control.client.ChildClient.View child = com.hex.kagune.control.client.ChildClient.partnered(npc.m_19879_(), clock);
        if (child == null) {
            return under;
        }
        RigPose p = under == null ? new RigPose() : under;
        double age = child.momentAge(clock);
        com.hex.kagune.control.client.animation.ChildMomentPose.blend(p, com.hex.kagune.control.client.animation.ChildMomentPose.parent(child.moment(),
            age, child.stage(), false), (float)com.hex.kagune.control.family.ChildMoment.weight(child.moment(), age));
        return p;
    }

    /**
     * 1.3.13: hand in hand, her held arm alive ({@link com.hex.kagune.control.npc.HandSway}): it reaches forward to their hand as she
     * trails, out as she swings wide, and sways with theirs on its own slow clock; pulled along, she leans into it a little.
     */
    static RigPose alive(EtoNpc npc, EtoClient.View view, RigPose gesture, float partial) {
        com.hex.kagune.control.npc.HandSway sway = view.gesture() == EtoGesture.HAND ? com.hex.kagune.control.client.HandClient.of(npc.m_19879_()) : null;
        if (gesture == null || sway == null) {
            return gesture;
        }
        double trail = sway.trail(partial);
        float[] arm = com.hex.kagune.control.npc.HandSway.arm(trail, sway.out(partial), com.hex.kagune.control.client.HandClient.clock(sway, partial), false,
            npc.persona().size());
        gesture.leftArmX += arm[0];
        gesture.leftArmZ -= arm[1];
        gesture.lean += (float)com.hex.kagune.control.npc.HandSway.pulled(trail);
        return gesture;
    }

    @Override
    public void m_7392_(EtoNpc npc, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        EtoClient.View view = EtoClient.get(npc.m_19879_());
        double now = PaleClient.clock(partial);
        if (com.hex.kagune.control.client.OwlClient.creature(npc, now)) {
            // 1.3.8: the Owl's creature is drawn in her place (PaleFleshRenderer), carried over from her body as posed this frame: posed,
            // never drawn.
            RigPose posed = pose(npc, view, now, partial);
            DRAWN.put(npc.m_19879_(), posed == null ? new RigPose() : new RigPose().add(posed, 1.0F));
            DRAWN_AT.put(npc.m_19879_(), now);
            return;
        }
        boolean glued = !npc.m_5803_() && glue(npc, view, partial, pose); // 1.3.21: asleep in her bed, where the bed lays her
        try {
            this.draw(npc, yaw, partial, pose, buffers, light, view);
        } finally {
            if (glued) {
                pose.m_85849_();
            }
        }
    }

    /**
     * 1.3.4: hand in hand, she is drawn glued at the side of the one whose hand she holds (where EtoHand puts her, as they are drawn
     * this frame), facing the way they face: they walk as one, whatever lag her own position has. True if she was moved (a pose pushed).
     */
    static boolean glue(EtoNpc npc, EtoClient.View view, float partial, PoseStack pose) {
        if (view == null || view.gesture() != EtoGesture.HAND || view.with() == 0
            || !(npc.m_9236_().m_6815_(view.with()) instanceof net.minecraft.world.entity.LivingEntity holder)) {
            return false;
        }
        float turn = ((holder.f_20883_ - holder.f_20884_) % 360.0F + 540.0F) % 360.0F - 180.0F;
        float bodyYaw = holder.f_20884_ + turn * partial;
        double[] off = EtoHand.offset(bodyYaw, com.hex.kagune.control.Genders.female(holder));
        // 1.3.13: on her spring from that spot: a little behind as they pull her along, out and in as they turn.
        com.hex.kagune.control.npc.HandSway sway = com.hex.kagune.control.client.HandClient.of(npc.m_19879_());
        if (sway != null) {
            double yaw = Math.toRadians(bodyYaw);
            double trail = sway.trail(partial);
            double out = sway.out(partial);
            off[0] += Math.sin(yaw) * trail - Math.cos(yaw) * out;
            off[1] += -Math.cos(yaw) * trail - Math.sin(yaw) * out;
        }
        double hx = holder.f_19790_ + (holder.m_20185_() - holder.f_19790_) * partial;
        double hy = holder.f_19791_ + (holder.m_20186_() - holder.f_19791_) * partial;
        double hz = holder.f_19792_ + (holder.m_20189_() - holder.f_19792_) * partial;
        double ex = npc.f_19790_ + (npc.m_20185_() - npc.f_19790_) * partial;
        double ey = npc.f_19791_ + (npc.m_20186_() - npc.f_19791_) * partial;
        double ez = npc.f_19792_ + (npc.m_20189_() - npc.f_19792_) * partial;
        pose.m_85836_();
        pose.m_85837_(hx + off[0] - ex, hy - ey, hz + off[1] - ez);
        npc.f_20883_ = bodyYaw;
        npc.f_20884_ = bodyYaw;
        return true;
    }

    /**
     * 1.3.6: what draws the Owl DNA kit on a body ({@link PaleFleshRenderer}'s flesh, tentacles...): who, her move, her body as it was
     * drawn this frame (null: not drawn, the move alone poses it), and her size against a player's.
     */
    interface Kit {
        boolean draw(net.minecraft.world.entity.LivingEntity who, PaleClient.PaleView view, RigPose drawn, double scale);
    }

    /** 1.3.6: each Eto's body as it was last drawn, and on what clock. */
    private static final java.util.Map<Integer, RigPose> DRAWN = new java.util.HashMap<>();
    private static final java.util.Map<Integer, Double> DRAWN_AT = new java.util.HashMap<>();

    /** 1.3.19: the body {@code id} as it was last drawn (whenever that was), or null. */
    public static RigPose lastPose(int id) {
        return DRAWN.get(id);
    }

    /** 1.3.6: her body as drawn on {@code clock} (this frame), or null if it wasn't. */
    static RigPose drawn(int id, double clock) {
        Double at = DRAWN_AT.get(id);
        return at != null && Math.abs(at - clock) < 1.0E-3 ? DRAWN.get(id) : null;
    }

    /** 1.3.6: {@code kit} run for every Eto in sight within {@code range} of this client's player; true if anything was drawn. */
    static boolean kit(Kit kit, net.minecraft.client.multiplayer.ClientLevel level, net.minecraft.client.Minecraft minecraft, double clock, double range) {
        DRAWN.keySet().removeIf(id -> level.m_6815_(id) == null);
        DRAWN_AT.keySet().removeIf(id -> level.m_6815_(id) == null);
        boolean drew = false;
        for (int id : EtoClient.ids()) {
            EtoClient.View view = EtoClient.get(id);
            if (view == null || !(level.m_6815_(id) instanceof EtoNpc npc) || !npc.m_6084_() || npc.m_20145_()
                || npc.m_20280_(minecraft.f_91074_) > range * range) {
                continue;
            }
            drew |= kit.draw(npc, view.fight(), drawn(id, clock), npc.persona().size());
        }
        return drew;
    }

    private void draw(EtoNpc npc, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light, EtoClient.View view) {
        double clock = PaleClient.clock(partial);
        RigPose posed = npc.m_5803_() ? null : pose(npc, view, clock, partial); // 1.3.21: asleep: lying still
        DRAWN.put(npc.m_19879_(), posed == null ? null : new RigPose().add(posed, 1.0F));
        DRAWN_AT.put(npc.m_19879_(), clock);
        com.hex.kagune.control.client.KingClient.seen(npc); // 1.3.19: Peace's effects find him
        EtoModel.pose = posed;
        boolean turned = posed != null && (Math.abs(posed.flip) > 1.0E-3F || Math.abs(posed.roll) > 1.0E-3F || Math.abs(posed.spin) > 1.0E-3F
            || Math.abs(posed.drop) > 1.0E-3F || Math.abs(posed.lunge) > 1.0E-3F);
        // 1.3.1: a little smaller than a player (her box is too, EtoRules.SIZE).
        pose.m_85836_();
        float size = this.persona.size();
        float wide = size * this.persona.build();
        pose.m_85841_(wide, size, wide);
        try {
            if (turned) {
                float delta = ((npc.f_20883_ - npc.f_20884_) % 360.0F + 540.0F) % 360.0F - 180.0F;
                double body = Math.toRadians(npc.f_20884_ + delta * partial);
                float rightX = (float)-Math.cos(body);
                float rightZ = (float)-Math.sin(body);
                pose.m_85836_();
                // 1.3.4: a lunge carries the whole of her forward (model pixels), as a Pale player's.
                pose.m_85837_(-Math.sin(body) * posed.lunge / 16.0, 0.0, Math.cos(body) * posed.lunge / 16.0);
                pose.m_85837_(0.0, -posed.drop / 16.0 + BodyFrame.TURN_PIVOT, 0.0);
                // 1.3.32: a pose's spin turns the body as PlayerAnimator turns a player's (+ to their left, seen from above anticlockwise): it was
                // the other way here, so an NPC parent's airplane spun against the child it carries round (ChildMomentPose.spinning).
                pose.m_252781_(new Quaternionf().rotateAxis(posed.spin, 0.0F, 1.0F, 0.0F));
                pose.m_252781_(new Quaternionf().rotateAxis(posed.flip, rightX, 0.0F, rightZ));
                pose.m_252781_(new Quaternionf().rotateAxis(-posed.roll, rightZ, 0.0F, -rightX));
                pose.m_85837_(0.0, -BodyFrame.TURN_PIVOT, 0.0);
            }
            super.m_7392_(npc, yaw, partial, pose, buffers, light);
        } finally {
            if (turned) {
                pose.m_85849_();
            }
            pose.m_85849_();
            EtoModel.pose = null;
        }
        if (view != null && view.gesture() == EtoGesture.ANGRY && !npc.m_20145_()) {
            angry(npc, view.gestureAge(clock), pose, buffers);
        }
        // 1.3.2: taken, she says so over her head: her name, and under it whose she is.
        boolean tagged = view != null && !view.tag().isEmpty() && !npc.m_20145_();
        if (tagged) {
            this.m_7649_(npc, Component.m_237113_(this.persona.colour() + "\u2764 \u00a7f" + view.tag()), pose, buffers, light);
            pose.m_85836_();
            pose.m_85837_(0.0, 0.26, 0.0);
            this.m_7649_(npc, Component.m_237113_(this.persona.colour() + npc.displayName()), pose, buffers, light);
            pose.m_85849_();
        }
        // 1.3.30: what she says, in a bubble over all of that (never the chat).
        if (!npc.m_20145_()) {
            bubble(npc, com.hex.kagune.control.client.NpcSpeech.lines(npc), tagged ? 0.56 : 0.3, pose, buffers, light);
        }
    }

    @Override
    public ResourceLocation m_5478_(EtoNpc npc) {
        return this.skin;
    }

    /** 1.3.30: {@code lines} (top first) in a bubble {@code base} blocks over its name tag's place, a line each, the last at the bottom. */
    /**
     * 1.3.38: a real bubble over her (SpeechBubbles: white, a tail to her; a thought cloud for what she does), not a name tag's line;
     * asleep with nothing said, now and then a cloud of z's.
     */
    private void bubble(EtoNpc npc, java.util.List<String> lines, double base, PoseStack pose, MultiBufferSource buffers, int light) {
        float partial = Minecraft.m_91087_().m_91296_();
        if (lines.isEmpty() && npc.m_5803_()) {
            SpeechBubbles.snore(npc, PaleClient.clock(partial), pose, buffers);
            return;
        }
        SpeechBubbles.draw(npc, lines, base, com.hex.kagune.control.client.NpcSpeech.age(npc.m_19879_(), partial), pose, buffers);
    }

    /** The anger mark over her head, to her right, popping in and throbbing, turned to the camera. */
    private static void angry(EtoNpc npc, double age, PoseStack pose, MultiBufferSource buffers) {
        int ticks = EtoGesture.ANGRY.ticks;
        if (age >= ticks) {
            return;
        }
        float pop = age < 4.0 ? (float)(age / 4.0) * 1.25F : age < 7.0 ? 1.25F - 0.25F * (float)((age - 4.0) / 3.0) : 1.0F;
        float fade = age > ticks - 8 ? (float)((ticks - age) / 8.0) : 1.0F;
        float throb = 1.0F + 0.08F * (float)Math.sin(age * 0.9);
        float size = 0.36F * pop * throb;
        Vec3 camera = Minecraft.m_91087_().f_91063_.m_109153_().m_90583_();
        double dx = camera.f_82479_ - npc.m_20185_();
        double dz = camera.f_82481_ - npc.m_20189_();
        float toward = (float)Math.atan2(dx, dz);
        pose.m_85836_();
        pose.m_85837_(0.0, npc.m_20206_() + 0.32, 0.0);
        pose.m_252781_(new Quaternionf().rotateAxis(toward, 0.0F, 1.0F, 0.0F));
        pose.m_85837_(-0.28, 0.0, 0.0);
        Matrix4f matrix = pose.m_85850_().m_252922_();
        Matrix3f normals = pose.m_85850_().m_252943_();
        VertexConsumer consumer = buffers.m_6299_(RenderType.m_110473_(ANGRY));
        int alpha = Math.round(255 * Math.max(0.0F, Math.min(1.0F, fade)));
        float[][] corners = {{-size, -size, 0.0F, 1.0F}, {size, -size, 1.0F, 1.0F}, {size, size, 1.0F, 0.0F}, {-size, size, 0.0F, 0.0F}};
        for (float[] c : corners) {
            consumer.m_252986_(matrix, c[0], c[1], 0.0F).m_6122_(255, 255, 255, alpha).m_7421_(c[2], c[3]).m_86008_(OverlayTexture.f_118083_)
                .m_85969_(0xF000F0).m_252939_(normals, 0.0F, 0.0F, 1.0F).m_5752_();
        }
        pose.m_85849_();
    }

    /**
     * Her face and figure over the skin: the bust on her torso, the kakugan (the eye open by its fade; its iris glowing), her eyes shut
     * (a blink, or a gesture that shuts them), her blush.
     */
    static final class Face extends RenderLayer<EtoNpc, PlayerModel<EtoNpc>> {
        private final EtoRenderer renderer;

        Face(EtoRenderer parent) {
            super(parent);
            this.renderer = parent;
        }

        @Override
        public void m_6494_(PoseStack pose, MultiBufferSource buffers, int light, EtoNpc npc, float limbSwing, float limbSwingAmount, float partialTick,
                            float ageInTicks, float netHeadYaw, float headPitch) {
            if (npc.m_20145_()) {
                return;
            }
            try {
                PlayerModel<EtoNpc> model = this.m_117386_();
                // 1.3.1: her bust bounces as she jumps and lands (her height as drawn: a hop's lift too).
                RigPose posed = EtoModel.pose;
                double y = npc.f_19791_ + (npc.m_20186_() - npc.f_19791_) * partialTick - (posed == null ? 0.0 : posed.drop / 16.0);
                float bounce = EtoClient.jiggle(npc.m_19879_(), y, PaleClient.clock(partialTick));
                EtoRenderer r = this.renderer;
                if (r.persona.female()) {
                    FemaleFigure.bust(model.f_102810_, pose, buffers.m_6299_(RenderType.m_110473_(r.skin)), light, bounce, false, r.persona.curves());
                    // 1.3.10: carrying a child, her belly (the shared female body's).
                    FemaleFigure.belly(model.f_102810_, pose, buffers.m_6299_(RenderType.m_110473_(r.skin)), light,
                        com.hex.kagune.control.client.PregnancyClient.belly(npc, partialTick), false);
                }
                EtoClient.View view = EtoClient.get(npc.m_19879_());
                double clock = PaleClient.clock(partialTick);
                EtoGesture gesture = view == null ? EtoGesture.NONE : view.gesture();
                boolean shut = gesture.eyesClosed || npc.m_5803_() || EtoRules.blink(npc.m_19879_() * 31 + 7, ageInTicks);
                float eye = EtoClient.eye(npc.m_19879_(), view, npc.m_9236_().m_46468_(), clock);
                if (!shut && eye > 0.003F) {
                    model.m_7695_(pose, buffers.m_6299_(RenderType.m_110473_(r.kakugan)), light, OverlayTexture.f_118083_, 1.0F, 1.0F, 1.0F, eye);
                    model.m_7695_(pose, buffers.m_6299_(RenderType.m_110488_(r.glow)), 0xF000F0, OverlayTexture.f_118083_, eye, eye, eye, 1.0F);
                }
                if (shut) {
                    model.m_7695_(pose, buffers.m_6299_(RenderType.m_110473_(r.closed)), light, OverlayTexture.f_118083_, 1.0F, 1.0F, 1.0F, 1.0F);
                }
                // 4.1.16: every woman blushes through a kiss and a while after, as deep as the kiss's mood has it (KissBlush).
                if (BLUSHES.size() > 256) {
                    BLUSHES.clear();
                }
                float blush = BLUSHES.computeIfAbsent(npc.m_19879_(), id -> new com.hex.kagune.control.client.animation.KissBlush()).at(r.persona.female(),
                    r.persona.blushes(gesture), gesture, view == null ? null : view.kiss(), clock);
                if (blush > 0.003F) {
                    model.m_7695_(pose, buffers.m_6299_(RenderType.m_110473_(r.blush)), light, OverlayTexture.f_118083_, 1.0F, 1.0F, 1.0F, blush);
                }
            } catch (RuntimeException failure) {
                PlayerLayers.failed(npc.persona().name() + "'s face", failure);
            }
        }
    }

    /** Her bust (1.3.9: the shared female body's, {@link FemaleFigure#bust}), on her torso, its tip {@code bounce} pixels up off its rest. */
    static void bust(PlayerModel<EtoNpc> model, PoseStack pose, VertexConsumer consumer, int light, float bounce) {
        FemaleFigure.bust(model.f_102810_, pose, consumer, light, bounce, false);
    }

    @Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
    public static final class Frame {
        private Frame() {
        }

        @SubscribeEvent
        public static void after(RenderLevelStageEvent event) {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
                return;
            }
            Minecraft minecraft = Minecraft.m_91087_();
            if (minecraft.f_91073_ != null && minecraft.f_91073_.m_46467_() % 100L == 50L) {
                EtoClient.prune(minecraft.f_91073_);
                com.hex.kagune.control.client.ChildClient.prune(minecraft.f_91073_);
                EtoFight.prune(id -> minecraft.f_91073_.m_6815_(id) == null);
            }
            EtoClient.pull();
        }
    }

    @Mod.EventBusSubscriber(modid = "hexkagune", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ControlEntities.ETO.get(), EtoRenderer::new);
            // 1.3.14: Touka and Ken, each in their own skin and body.
            event.registerEntityRenderer(ControlEntities.TOUKA.get(), context -> new EtoRenderer(context, com.hex.kagune.control.npc.Persona.TOUKA));
            event.registerEntityRenderer(ControlEntities.KEN.get(), context -> new EtoRenderer(context, com.hex.kagune.control.npc.Persona.KEN));
            event.registerEntityRenderer(ControlEntities.AYATO.get(), context -> new EtoRenderer(context, com.hex.kagune.control.npc.Persona.AYATO));
            event.registerEntityRenderer(ControlEntities.NORO.get(), context -> new EtoRenderer(context, com.hex.kagune.control.npc.Persona.NORO));
            event.registerEntityRenderer(ControlEntities.GENERAL.get(), context -> new EtoRenderer(context, com.hex.kagune.control.npc.Persona.GENERAL));
        }
    }
}
