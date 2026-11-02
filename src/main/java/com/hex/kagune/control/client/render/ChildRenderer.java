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
    /** 1.3.43: the children drawn this frame (their kagune is drawn after entities, as a wild ghoul's; cleared with theirs). */
    private static final java.util.Set<ChildNpc> DRAWN = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    /** 1.3.43: the children drawn so far this frame. */
    static java.util.List<ChildNpc> drawn() {
        return new java.util.ArrayList<>(DRAWN);
    }

    /** 1.3.43: the frame's list cleared (after both kagune renderers). */
    static void cleared() {
        DRAWN.clear();
    }
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

    /** 2.0 (lag): each texture's location made once, not several times a frame for every child drawn. */
    private static final java.util.Map<String, ResourceLocation> TEXTURES = new java.util.HashMap<>();

    private static ResourceLocation texture(String name) {
        return TEXTURES.computeIfAbsent(name, n -> Objects.requireNonNull(ResourceLocation.m_214293_("hexkagune", "textures/entity/child/skins/" + n + ".png")));
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
            boolean asleep = child.m_5803_();
            boolean walking = view != null && view.ride() == ChildRules.RIDE_HAND;
            if (view != null && !asleep && !walking && (view.stage().sits() || view.carrier() != 0)) {
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
            // 1.3.20: its moment with a parent, or its ride on their shoulders (arms, legs, head; the body's hop and tip are the renderer's).
            com.hex.kagune.control.client.animation.RigPose posed = asleep ? null : posed(child, view, ageInTicks);
            if (posed != null) {
                turn(this.f_102808_, posed.headX, posed.headY, posed.headZ, posed.headHold);
                turn(this.f_102811_, posed.rightArmX, posed.rightArmY, posed.rightArmZ, Math.max(posed.armHold, posed.rightArmHold));
                turn(this.f_102812_, posed.leftArmX, posed.leftArmY, posed.leftArmZ, Math.max(posed.armHold, posed.leftArmHold));
                turn(this.f_102813_, posed.rightLegX, posed.rightLegY, posed.rightLegZ, posed.legHold);
                turn(this.f_102814_, posed.leftLegX, posed.leftLegY, posed.leftLegZ, posed.legHold);
            }
            little(young);
            // 1.3.56: Eto's child in its fight (her flesh's moves, her Mutated Arm): each part posed exactly as hers is (ChildFight).
            com.hex.kagune.control.client.animation.RigPose fight = young || asleep ? null : FIGHT.get(child.m_19879_());
            if (fight != null) {
                apply("head", this.f_102808_, fight);
                apply("torso", this.f_102810_, fight);
                apply("rightArm", this.f_102811_, fight);
                apply("leftArm", this.f_102812_, fight);
                apply("rightLeg", this.f_102813_, fight);
                apply("leftLeg", this.f_102814_, fight);
            }
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

        /** 1.3.56: {@code part} (PlayerAnimator's name {@code name}) moved and turned as the pose has it, from where it was (as EtoModel's). */
        private static void apply(String name, ModelPart part, com.hex.kagune.control.client.animation.RigPose p) {
            float[] at = p.transform(name, com.hex.kagune.control.client.animation.RigPose.POSITION, part.f_104200_, part.f_104201_, part.f_104202_);
            float[] turn = p.transform(name, com.hex.kagune.control.client.animation.RigPose.ROTATION, part.f_104203_, part.f_104204_, part.f_104205_);
            part.f_104200_ = at[0];
            part.f_104201_ = at[1];
            part.f_104202_ = at[2];
            part.f_104203_ = turn[0];
            part.f_104204_ = turn[1];
            part.f_104205_ = turn[2];
        }

        /** A part's turn: held at {@code (x, y, z)} by {@code hold} (taken from what it had), the rest added. */
        private static void turn(ModelPart part, float x, float y, float z, float hold) {
            float own = 1.0F - Math.max(0.0F, Math.min(1.0F, hold));
            part.f_104203_ = part.f_104203_ * own + x;
            part.f_104204_ = part.f_104204_ * own + y;
            part.f_104205_ = part.f_104205_ * own + z;
        }

        private static void scale(ModelPart part, float k) {
            part.f_233553_ = k;
            part.f_233554_ = k;
            part.f_233555_ = k;
        }
    }

    /** 1.3.56: each fighting Eto's child's body as its fight poses it this frame (by entity id). */
    private static final java.util.Map<Integer, com.hex.kagune.control.client.animation.RigPose> FIGHT = new java.util.HashMap<>();

    /** 1.3.56: {@code child}'s body as its fight posed it when it was last drawn (for her flesh on it), or null (the move alone poses it). */
    public static com.hex.kagune.control.client.animation.RigPose fightDrawn(ChildNpc child) {
        return FIGHT.get(child.m_19879_());
    }

    /** 1.3.20: how its moment (or its ride) poses it now ({@code ageInTicks}: its own clock, for looking about), or null. */
    static com.hex.kagune.control.client.animation.RigPose posed(ChildNpc child, ChildClient.View view, float ageInTicks) {
        if (view == null) {
            return null;
        }
        double clock = com.hex.kagune.control.client.PaleClient.clock(0.0F);
        if (view.inMoment(clock)) {
            double age = view.momentAge(clock);
            com.hex.kagune.control.client.animation.RigPose p = com.hex.kagune.control.client.animation.ChildMomentPose.child(view.moment(), age,
                view.stage());
            if (p != null) {
                float w = (float)com.hex.kagune.control.family.ChildMoment.weight(view.moment(), age);
                com.hex.kagune.control.client.animation.RigPose out = new com.hex.kagune.control.client.animation.RigPose();
                com.hex.kagune.control.client.animation.ChildMomentPose.blend(out, p, w);
                out.armHold = p.armHold * w;
                out.rightArmHold = p.rightArmHold * w;
                out.leftArmHold = p.leftArmHold * w;
                out.legHold = p.legHold * w;
                return out;
            }
        }
        // 1.3.57: what it does with its body as it says a line in a talk (arms folded, a stamp, a point, a shrug...).
        double cueAge = view.cueAge(clock);
        if (view.cue().running(cueAge)) {
            com.hex.kagune.control.client.animation.RigPose cue = com.hex.kagune.control.client.animation.ChildTalkPose.pose(view.cue(), cueAge,
                view.stage(), view.carrier() != 0 || view.stage().sits()); // 1.3.59: a baby sitting keeps its legs as they sit, no hops
            if (cue != null) {
                float w = view.cue().weight(cueAge);
                com.hex.kagune.control.client.animation.RigPose out = new com.hex.kagune.control.client.animation.RigPose();
                com.hex.kagune.control.client.animation.ChildMomentPose.blend(out, cue, w);
                out.armHold = cue.armHold * w;
                out.rightArmHold = cue.rightArmHold * w;
                out.leftArmHold = cue.leftArmHold * w;
                out.legHold = cue.legHold * w;
                out.headHold = cue.headHold * w;
                return out;
            }
        }
        if (view.carrier() == 0) {
            // 1.3.48: a flip in a fight, a ghoul's, as the wild ghouls' are posed (its whole body turned over about its middle).
            com.hex.kagune.control.client.GhoulNpcClient.View fighting = com.hex.kagune.control.client.GhoulNpcClient.get(child.m_19879_());
            return fighting == null ? null : com.hex.kagune.control.client.GhoulNpcClient.flip(fighting, clock, child.m_20096_());
        }
        // 1.3.21: hand in hand, its arm to the hand of whoever holds it, by their size.
        double size = child.m_9236_().m_6815_(view.carrier()) instanceof net.minecraft.world.entity.LivingEntity carrier ? ChildNpc.carrySize(carrier) : 1.0;
        ChildSkins.Skin skin = skin(view);
        return com.hex.kagune.control.client.animation.ChildMomentPose.ride(view.ride(), view.stage(), ageInTicks, size, skin != null && skin.slim());
    }

    @Override
    public void m_7392_(ChildNpc child, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        ChildClient.View view = ChildClient.get(child.m_19879_());
        ChildStage stage = view == null ? child.stage : view.stage();
        child.carriedBy = view == null ? 0 : view.carrier();
        if (view != null && com.hex.kagune.control.family.KaguneGenes.any(view.genome().kagune())) {
            DRAWN.add(child); // 1.3.43: its kagune drawn after entities
        }
        ChildSkins.Skin skin = skin(view);
        this.f_115290_ = skin != null && skin.slim() ? this.slim : this.wide;
        // 1.3.56: Eto's child fighting with her flesh: its body posed for it this frame (its model's parts; her flesh drawn on it after).
        FIGHT.remove(child.m_19879_());
        if (view != null && com.hex.kagune.control.family.KaguneGenes.owl(view.genome().kagune()) && !child.m_5803_() && !stage.young) {
            com.hex.kagune.control.client.animation.RigPose fight = com.hex.kagune.control.client.animation.ChildFight.pose(child,
                com.hex.kagune.control.client.PaleClient.clock(partial), partial);
            if (fight != null) {
                FIGHT.put(child.m_19879_(), fight);
            }
        }
        pose.m_85836_();
        try {
            boolean asleep = child.m_5803_();
            if (!asleep) {
                glue(child, view, partial, pose);
            }
            if (view != null && !asleep && (stage.sits() || view.carrier() != 0) && view.ride() != com.hex.kagune.control.family.ChildRules.RIDE_SPIN
                && view.ride() != ChildRules.RIDE_HAND) {
                // Sat down: its legs (a little one's are half as long) out in front, not under it.
                pose.m_85837_(0.0, -0.75 * stage.draw * (stage.young ? 0.5 : 1.0), 0.0);
            }
            pose.m_85841_(stage.draw, stage.draw, stage.draw);
            // 1.3.20: its moment's hop, sway and tip, about its middle.
            com.hex.kagune.control.client.animation.RigPose body = asleep ? null : posed(child, view, child.f_19797_ + partial);
            if (body != null && (body.drop != 0.0F || body.roll != 0.0F || body.flip != 0.0F)) {
                float delta = ((child.f_20883_ - child.f_20884_) % 360.0F + 540.0F) % 360.0F - 180.0F;
                double turn = Math.toRadians(child.f_20884_ + delta * partial);
                float rightX = (float)-Math.cos(turn);
                float rightZ = (float)-Math.sin(turn);
                double middle = stage.young ? 0.62 : 0.9; // its middle, in its own (unscaled) blocks
                pose.m_85837_(0.0, -body.drop / 16.0, 0.0);
                pose.m_85837_(0.0, middle, 0.0);
                pose.m_252781_(new org.joml.Quaternionf().rotateAxis(body.flip, rightX, 0.0F, rightZ));
                pose.m_252781_(new org.joml.Quaternionf().rotateAxis(-body.roll, rightZ, 0.0F, -rightX));
                pose.m_85837_(0.0, -middle, 0.0);
            }
            // 1.3.50: Eto's child's Mutated Arm out: its own right arm is the flesh now (drawn by PaleFleshRenderer), not drawn here.
            boolean mutated = com.hex.kagune.control.client.ArmClient.out(child);
            if (mutated) {
                this.f_115290_.f_102811_.f_104207_ = false;
                this.f_115290_.f_103375_.f_104207_ = false;
            }
            try {
                super.m_7392_(child, yaw, partial, pose, buffers, light);
            } finally {
                if (mutated) {
                    this.f_115290_.f_102811_.f_104207_ = true;
                    this.f_115290_.f_103375_.f_104207_ = true;
                }
            }
        } finally {
            pose.m_85849_();
        }
        // 1.3.30: what it says, in a bubble over its head (never the chat). 1.3.38: a real bubble (SpeechBubbles), a thought cloud for
        // what it does; asleep with nothing said, now and then a cloud of z's.
        java.util.List<String> said = com.hex.kagune.control.client.NpcSpeech.lines(child);
        if (!child.m_20145_()) {
            if (said.isEmpty() && child.m_5803_()) {
                SpeechBubbles.snore(child, com.hex.kagune.control.client.PaleClient.clock(partial), pose, buffers);
            } else {
                SpeechBubbles.draw(child, said, 0.3, com.hex.kagune.control.client.NpcSpeech.age(child.m_19879_(), partial), pose, buffers);
            }
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
        // 1.3.20: up on their shoulders, facing their way; held out at arm's length and carried round as their body spins; thrown up.
        int ride = view.ride();
        double clock = com.hex.kagune.control.client.PaleClient.clock(partial);
        double age = view.momentAge(clock);
        if (ride == ChildRules.RIDE_SPIN && view.moment() == com.hex.kagune.control.family.ChildMoment.SPIN && view.inMoment(clock)) {
            bodyYaw += (float)Math.toDegrees(com.hex.kagune.control.family.ChildMoment.spinTurn(age)
                * com.hex.kagune.control.family.ChildMoment.weight(com.hex.kagune.control.family.ChildMoment.SPIN, age));
        }
        double yaw = Math.toRadians(bodyYaw);
        double ahead = (ride == ChildRules.RIDE_SHOULDERS ? -ChildRules.SHOULDERS_BACK : ride == ChildRules.RIDE_SPIN ? ChildRules.SPIN_AHEAD
            : ChildRules.CARRY_AHEAD) * scale;
        double up = ride == ChildRules.RIDE_SHOULDERS ? ChildRules.SHOULDERS_UP : ride == ChildRules.RIDE_SPIN ? ChildRules.SPIN_UP
            : ride == ChildRules.RIDE_HAND ? 0.0 : ChildRules.CARRY_UP;
        if (ride == ChildRules.RIDE_HAND) {
            ahead = 0.0; // hand in hand: at their left, on the ground (below)
        }
        if (view.moment() == com.hex.kagune.control.family.ChildMoment.TOSS && view.inMoment(clock)) {
            up += com.hex.kagune.control.family.ChildMoment.tossLift(age);
        }
        ChildSkins.Skin skin = skin(view);
        double left = ride == ChildRules.RIDE_HAND ? ChildRules.hand(view.stage(), scale, skin != null && skin.slim()).side() : 0.0;
        double cx = carrier.f_19790_ + (carrier.m_20185_() - carrier.f_19790_) * partial - Math.sin(yaw) * ahead + Math.cos(yaw) * left;
        double cy = carrier.f_19791_ + (carrier.m_20186_() - carrier.f_19791_) * partial + up * scale;
        double cz = carrier.f_19792_ + (carrier.m_20189_() - carrier.f_19792_) * partial + Math.cos(yaw) * ahead + Math.sin(yaw) * left;
        if (ride == ChildRules.RIDE_SHOULDERS || ride == ChildRules.RIDE_HAND) {
            bodyYaw -= 180.0F; // facing their way, not them
        }
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
            // 1.3.79: its skin in the look nearest the hair and eyes its genes gave it (ChildLooks: other hair and eye colours).
            return texture(com.hex.kagune.control.family.ChildLooks.pick(skin.key(), view.genome().hair(), view.genome().eyes()).texture());
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
     * the skin), their irises drawn full bright. 1.3.76: its blinks (and its eyes shut asleep), over the kakugan.
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
                if (skin == null) {
                    return;
                }
                PlayerModel<ChildNpc> model = this.m_117386_();
                // 1.3.76: its eyes shut now and then (a blink, as Eto's and the companions'), or asleep: the shut eyes over any kakugan.
                if (com.hex.kagune.control.family.ChildBlinks.has(skin.key())
                    && (child.m_5803_() || com.hex.kagune.control.npc.EtoRules.blink(child.m_19879_() * 31 + 7, ageInTicks))) {
                    model.m_7695_(pose, buffers.m_6299_(RenderType.m_110473_(texture(skin.key() + "_closed"))), light, OverlayTexture.f_118083_,
                        1.0F, 1.0F, 1.0F, 1.0F);
                    return;
                }
                if (!view.turned()) {
                    return;
                }
                int eyes = view.genome().kakugan();
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
