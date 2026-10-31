package com.hex.kagune.control.client.animation;

import com.hex.kagune.client.KaguneClientState;
import com.hex.kagune.control.ComboStage;
import com.hex.kagune.control.ControlConfig;
import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.StrikePattern;
import com.hex.kagune.control.UrgeType;
import com.hex.kagune.control.ukaku.UkakuBody;
import com.hex.kagune.control.ukaku.BladeCarry;
import com.hex.kagune.control.ukaku.KakujaBody;
import com.hex.kagune.control.ukaku.SteelBlade;
import com.hex.kagune.control.ukaku.SteelSwing;
import com.hex.kagune.control.client.UkakuClient;
import com.hex.kagune.control.client.ControlClient;
import com.hex.kagune.control.client.ControlView;
import com.hex.kagune.control.client.PathClient;
import com.hex.kagune.gameplay.KaguneForm;
import com.hex.kagune.gameplay.TendrilMode;
import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.core.util.Vec3f;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Continuous, additive kagune body layer (PlayerAnimator priority 60), and the only writer of the upper-body bend.
 * On the Rinkaku path everything scales smoothly with the server's control/hunger/frenzy: an untrained ghoul pants,
 * trembles, spasms, clutches the wound the kagune tears out of and stumbles; a master stands in a calm predator stance.
 * Ukaku is never feral, whatever his mastery: none of that plays on his path. Both paths settle into their ambient
 * stance when standing still ({@link IdleStance}), and combo strikes are driven by the torso, shoulders, arms and legs in
 * sync with the server's impact tick. The pose is computed once per frame in {@link #setupAnim} into a {@link RigPose},
 * which owns the conversion to PlayerAnimator's (easy to get backwards) conventions.
 */
public final class KaguneBodyAnimation implements IAnimation {
    /**
     * Each player's upper body as last posed ({lean, side, twist, drop in model pixels, world time}): the kakuja's tentacles
     * (drawn in the world) and its chest armor (drawn on the unbent body part) ride it.
     */
    private static final Map<AbstractClientPlayer, double[]> TORSO = new WeakHashMap<>();
    /** A posed torso older than this (ticks: the model wasn't drawn, as in first person) counts as upright. */
    private static final double TORSO_STALE = 2.0;

    private final AbstractClientPlayer player;
    private final IdleStance idle;
    /** 6.1.0: Pale's playful stance and gestures, and how much she is walking with nothing else going on (eased). */
    private final PaleIdle paleIdle;
    /** Ticks she has stood still (not moving from where she is), and how many before her idle play may start. */
    private int paleStillTicks;
    static final int PALE_STILL_TICKS = 20;
    /** 6.1.0: Pale's strikes and flips on her body. */
    private final PaleAnimator paleMoves = new PaleAnimator();
    /** 6.2.0: when (PaleClient's clock) her move was last put on the body; NaN when the last set-up put none on. */
    private double palePosedAt = Double.NaN;
    /** 1.0.15: when the body was last posed (world ticks with the frame's share), whatever path. */
    private double posedAt = Double.NaN;
    private float paleStrolling;
    private float lastPaleStrolling;
    private final RigPose p = new RigPose();
    private long seed;
    private int age;
    private float fade;
    private boolean wanted;

    private int spasmKind = -1;
    private int spasmStart;
    private float spasmAmplitude;
    private int clutchStart = -1000;
    private int clutchLength;
    private long lastManifestStart = Long.MIN_VALUE;
    /** The kakuja's landings: was he in the air last tick, how fast was he falling, and when he last touched down. */
    private boolean wasAirborne;
    private float lastVertical;
    /**
     * 1.0.26: a final kakuja's run (KingRun): its stride (radians, the tick before and now), how hard he runs, how much he sprints, how
     * free its upper body is of blows, and how much of a blow's legs are its own (in the air, or a move that moves him), each the tick
     * before and now so a frame between sits between.
     */
    private double runPhase;
    private double runPhaseWas;
    private float run;
    private float runWas;
    private float sprint;
    private float sprintWas;
    private float free = 1.0F;
    private float freeWas = 1.0F;
    private float blowLegs;
    private float blowLegsWas;
    private int landedAt = -1000;
    private float landSpeed;
    /** 5.0.20: the Steel Blade arm this frame: a swing's turn (null: the hold), the last frame's, and where the swing began. */
    private double[] steelArm;
    private double[] lastArm;
    private double[] swingFrom;
    private double swingStartedAt = Double.NaN;
    private boolean wasSwinging;
    private double[] settleFrom;
    private double settleAt;
    /**
     * 6.0.4: how much the blade is drawn in to rest off the ground ({@link RigPose#bladeFit}): this frame's in a swing, the last
     * frame's, what it was as the swing began, and as it settled.
     */
    private double steelFit = 1.0;
    private double lastFit = 1.0;
    private double fitFrom = 1.0;
    private double settleFit = 1.0;
    /** How long a cut-short swing takes to ease back into the hold (ticks). */
    private static final double SETTLE_TICKS = 5.0;

    /**
     * 6.0.0: each player's body layer, so the first-person blade can take its arm from it ({@link #bladePose}). 6.2.0: found by the
     * player object itself ({@link #layer}), never through a hashed map keyed by the player: an entity's equals and hashCode are its
     * entity id, and the layer is made in the player's constructor, before the game gives it its network id, so a WeakHashMap never
     * found it again (her own arms in first person, the flesh on her drawn body, the first-person blade: all fell back or vanished).
     * Weak: a layer goes with its player.
     */
    private static final List<WeakReference<KaguneBodyAnimation>> LAYERS = new ArrayList<>();

    public KaguneBodyAnimation(AbstractClientPlayer player) {
        this.player = player;
        LAYERS.removeIf(ref -> ref.get() == null);
        LAYERS.add(new WeakReference<>(this));
        this.idle = new IdleStance(player);
        this.paleIdle = new PaleIdle(player.m_19879_());
        this.seed = player.m_19879_() * 0x9E3779B97F4A7C15L + 0x2545F4914F6CDD1DL;
    }

    private float random() {
        this.seed = this.seed * 6364136223846793005L + 1442695040888963407L;
        return (this.seed >>> 40) / (float)(1L << 24);
    }

    /** True on the Rinkaku path (and before a path is known): the kagune's hold on the body shows. Ukaku is always in control. */
    private boolean feral() {
        // 6.1.0: nor is Pale: her base form is all play and control. 7.0.0: nor Graft, whose kakuhou was put in him to be used.
        KagunePath path = PathClient.get(this.player);
        return path != KagunePath.UKAKU && path != KagunePath.PALE && path != KagunePath.GRAFT;
    }

    /**
     * The player's upper body as the body layer last posed it: {lean, side, twist} (radians) and the drop (model pixels), or
     * zeros when it isn't posed (another path, the layer off, or the model not drawn lately).
     */
    public static float[] torso(AbstractClientPlayer player, double time) {
        double[] last = TORSO.get(player);
        if (last == null || Math.abs(time - last[4]) > TORSO_STALE) {
            return new float[4];
        }
        return new float[] {(float)last[0], (float)last[1], (float)last[2], (float)last[3]};
    }

    @Override
    public void tick() {
        this.age++;
        this.idle.tick();
        this.tickPale();
        boolean airborne = !this.player.m_20096_();
        float vertical = (float)(this.player.m_20186_() - this.player.f_19791_);
        if (this.wasAirborne && !airborne) {
            this.landedAt = this.age;
            this.landSpeed = Math.max(0.0F, -this.lastVertical);
        }
        this.wasAirborne = airborne;
        this.lastVertical = vertical;
        this.tickRun(airborne);
        KaguneClientState.View view = KaguneClientState.get(this.player);
        this.wanted = this.wants();
        this.fade += ((this.wanted ? 1.0F : 0.0F) - this.fade) * (this.wanted ? 0.2F : 0.12F);
        if (!this.wanted && this.fade < 0.002F) {
            this.fade = 0.0F;
            return;
        }
        ControlView control = ControlClient.get(this.player);
        float struggle = this.feral() ? control.struggle() : 0.0F;
        boolean manifesting = view.mode() == TendrilMode.MANIFEST;
        if (manifesting && view.modeStarted() != this.lastManifestStart) {
            this.lastManifestStart = view.modeStarted();
            if (struggle > 0.3F) {
                this.clutchStart = this.age + view.modeDuration() / 2;
                this.clutchLength = 30 + Math.round(40.0F * struggle);
            }
        }
        float spasmRate = struggle <= 0.15F ? 0.0F : struggle * struggle * 0.012F * (1.0F + control.frenzy()) * (manifesting ? 3.0F : 1.0F);
        if (this.spasmKind < 0 || this.age - this.spasmStart > 12) {
            this.spasmKind = -1;
            if (this.random() < spasmRate) {
                this.spasmKind = (int)(this.random() * 4.0F);
                this.spasmStart = this.age;
                this.spasmAmplitude = (0.25F + 0.35F * this.random()) * struggle * (this.random() < 0.5F ? -1.0F : 1.0F);
            }
        }
        boolean starving = control.hunger() > 0.5F || control.frenzy() > 0.6F;
        if (struggle > 0.0F && starving && this.age > this.clutchStart + this.clutchLength + 100 && this.random() < 0.002F * struggle) {
            this.clutchStart = this.age;
            this.clutchLength = 25 + Math.round(30.0F * struggle);
        }
    }

    /** 1.0.26: a final kakuja's run on a tick: its stride on as far as he went, how hard he runs and sprints, how free of blows. */
    private void tickRun(boolean airborne) {
        this.runPhaseWas = this.runPhase;
        this.runWas = this.run;
        this.sprintWas = this.sprint;
        this.freeWas = this.free;
        this.blowLegsWas = this.blowLegs;
        com.hex.kagune.control.client.KingClient.View worn = com.hex.kagune.control.client.KingClient.get(this.player);
        if (worn == null || !worn.on()) {
            this.run = this.sprint = this.blowLegs = 0.0F;
            this.free = 1.0F;
            return;
        }
        double speed = Math.hypot(this.player.m_20185_() - this.player.f_19790_, this.player.m_20189_() - this.player.f_19792_);
        // In the air the legs keep turning over, slower; on the ground as fast as he goes.
        this.runPhase += KingRun.phase(speed) * (airborne ? 0.35 : 1.0);
        if (this.runPhase > 2000.0 * Math.PI) {
            this.runPhase -= 2000.0 * Math.PI;
            this.runPhaseWas -= 2000.0 * Math.PI;
        }
        float wantRun = this.player.m_6047_() ? 0.0F : airborne ? this.run * 0.85F : KingRun.amount(speed); // crouched: vanilla's sneak
        this.run += (wantRun - this.run) * 0.35F;
        this.sprint += ((this.player.m_20142_() ? 1.0F : 0.0F) - this.sprint) * 0.25F;
        double now = this.player.m_9236_().m_46467_();
        com.hex.kagune.control.king.KingMove move = worn.moveAt(now);
        boolean striking = move != com.hex.kagune.control.king.KingMove.NONE;
        this.free += ((striking ? 0.0F : 1.0F) - this.free) * (striking ? 0.6F : 0.3F);
        boolean ownLegs = airborne || move == com.hex.kagune.control.king.KingMove.DASH || move == com.hex.kagune.control.king.KingMove.ASCENT
            || move == com.hex.kagune.control.king.KingMove.DIVE || move == com.hex.kagune.control.king.KingMove.LANDING
            || move == com.hex.kagune.control.king.KingMove.BLITZ || move == com.hex.kagune.control.king.KingMove.SUNDER // 1.0.27: in the air
            || move == com.hex.kagune.control.king.KingMove.DEFY; // 1.0.28: rooted in the cocoon, crouched
        this.blowLegs += ((ownLegs ? 1.0F : 0.0F) - this.blowLegs) * 0.4F;
        this.afterimages(worn, speed, airborne, now);
    }

    /** 1.0.26: how fast a final kakuja must go (blocks a tick) for its sprint to leave afterimages, and every how many ticks one. */
    static final double AFTERIMAGE_SPEED = 0.26;
    static final int AFTERIMAGE_EVERY = 2;
    private long lastStep;

    /**
     * 1.0.26: sprinting flat out, a final kakuja leaves afterimages ({@code KingAfterimages}), and its footfalls show: War's stamp soot
     * out of the ground (every other one cracking it), Peace's leave a glint. 1.0.33: his own in first person too (posed here, as his
     * body is never drawn there; {@code KingAfterimages} leaves out the ones still round his eyes).
     */
    private void afterimages(com.hex.kagune.control.client.KingClient.View worn, double speed, boolean airborne, double now) {
        long step = (long)Math.floor(this.runPhase / Math.PI);
        boolean stepped = step != this.lastStep;
        this.lastStep = step;
        if (!this.player.m_20142_() || speed < AFTERIMAGE_SPEED) {
            return;
        }
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.m_91087_();
        boolean own = this.player == minecraft.f_91074_ && minecraft.f_91066_.m_92176_().m_90612_();
        boolean war = worn.form() == com.hex.kagune.control.king.KingForm.WAR;
        // (1.0.35: the afterimages themselves are the dash's now, left by WarPathFx's sprint; here only the footfalls.)
        if (stepped && !airborne && this.player.m_9236_() instanceof net.minecraft.client.multiplayer.ClientLevel level) {
            double yaw = Math.toRadians(this.player.f_20883_);
            Vec3 back = new Vec3(Math.sin(yaw), 0.15, -Math.cos(yaw));
            Vec3 foot = this.player.m_20182_().m_82520_(0.0, 0.05, 0.0);
            if (war) {
                com.hex.kagune.control.client.fx.KingMarks.play(level, com.hex.kagune.control.king.KingMarkFx.SOOT, foot, back, 0.45F, null);
                if (step % 2L == 0L) {
                    com.hex.kagune.control.client.fx.KingMarks.strike(level, com.hex.kagune.control.king.KingMarkFx.QUAKE, foot, new Vec3(0.0, 1.0, 0.0),
                        0.3F, null);
                }
            } else {
                com.hex.kagune.control.client.fx.KingMarks.play(level, com.hex.kagune.control.king.KingMarkFx.GLINT, foot.m_82520_(0.0, 0.3, 0.0), back,
                    0.4F, null);
            }
        }
    }

    /** {@code was} to {@code now}, {@code k} of the way. */
    private static float between(float was, float now, float k) {
        return was + (now - was) * k;
    }

    /** Degrees in [-180, 180). */
    private static float wrapDegrees(float degrees) {
        return degrees - 360.0F * (float)Math.floor((degrees + 180.0F) / 360.0F);
    }

    /** 6.1.0: Pale's idle play: a gesture now and then once she has stood still a second, none on the move. */
    private void tickPale() {
        this.lastPaleStrolling = this.paleStrolling;
        if (PathClient.get(this.player) != KagunePath.PALE) {
            this.paleStrolling = 0.0F;
            return;
        }
        boolean free = com.hex.kagune.control.client.PaleClient.get(this.player).move() == com.hex.kagune.control.pale.PaleMove.NONE
            && !this.paleMoves.busy(com.hex.kagune.control.client.PaleClient.clock(0.0F)) && this.player.m_20096_() && !this.player.m_20159_() && !this.player.m_6069_() && !this.player.m_5803_();
        boolean strolling = free && this.player.f_267362_.m_267711_(1.0F) > 0.05F;
        this.paleStrolling += ((strolling ? 1.0F : 0.0F) - this.paleStrolling) * 0.15F;
        // Her idle play only once she has stood still a second: not moving from where she is (looking round is fine), on the
        // ground, nothing else going on. Moving at all ends it.
        double dx = this.player.m_20185_() - this.player.f_19790_;
        double dy = this.player.m_20186_() - this.player.f_19791_;
        double dz = this.player.m_20189_() - this.player.f_19792_;
        boolean moved = dx * dx + dy * dy + dz * dz > 1.0E-6 || strolling || !free;
        this.paleStillTicks = moved ? 0 : this.paleStillTicks + 1;
        boolean still = this.paleStillTicks >= PALE_STILL_TICKS && this.idle.style() == IdlePose.Style.PALE_PLAYFUL && this.idle.weight(1.0F) > 0.6F;
        this.paleIdle.tick(still, false);
    }

    @Override
    public boolean isActive() {
        // PlayerAnimator only ticks layers that already report active, so this must not depend on tick() having run.
        return this.fade > 0.002F || this.wants() || this.idle.showing();
    }

    private boolean wants() {
        // 1.3.3: holding Eto's hand, the arm reaches out to her (whatever else the body is doing).
        if (com.hex.kagune.control.client.EtoClient.handHeld(this.player.m_19879_()) > 0.0F) {
            return this.player.m_6084_() && ControlConfig.bodyAnimations();
        }
        if (com.hex.kagune.control.client.PaleClient.eyes(this.player)) {
            return this.player.m_6084_() && ControlConfig.bodyAnimations(); // 7.0.1: the eyes alone fight with the body
        }
        if (this.king() > 0.0F || this.turning() >= 0.0) {
            return this.player.m_6084_() && ControlConfig.bodyAnimations(); // 1.0.5: a final kakuja has the body (1.0.11: or the Dragon, coming)
        }
        return KaguneClientState.get(this.player).active() && this.player.m_6084_() && (ControlConfig.bodyAnimations() || this.bladeOut());
    }

    /** 1.0.11: how far this player has turned into the Dragon (-1: not turning). */
    private double turning() {
        return com.hex.kagune.control.client.TurningClient.progress(this.player, ControlClient.worldTime(0.0F));
    }

    /** 1.0.5: how far a final kakuja has grown over this player (0: none worn); 1.0.17: all of it while it comes over him. */
    private float king() {
        com.hex.kagune.control.client.KingClient.View view = com.hex.kagune.control.client.KingClient.get(this.player);
        if (view == null || view.form() == com.hex.kagune.control.king.KingForm.NONE) {
            return 0.0F;
        }
        double time = ControlClient.worldTime(0.0F);
        if (com.hex.kagune.control.king.KingTransform.coming(view.form(), view.on(), time - view.since())) {
            return 1.0F; // the body is the transformation's from its first moment (War's kakuja is not out of him until its burst)
        }
        return (float)view.grow(time);
    }

    /** 5.0.19: the Steel Blade is out (or forming): its carry runs even with the body animations turned off. */
    private boolean bladeOut() {
        return UkakuClient.blade(this.player, ControlClient.worldTime(0.0F)) > 0.0;
    }

    /** {@code player}'s body layer (by identity), or null when it has none. */
    private static KaguneBodyAnimation layer(AbstractClientPlayer player) {
        if (player == null) {
            return null;
        }
        for (int i = LAYERS.size() - 1; i >= 0; i--) {
            KaguneBodyAnimation layer = LAYERS.get(i).get();
            if (layer != null && layer.player == player) {
                return layer;
            }
        }
        return null;
    }

    /**
     * 6.0.0: the body's pose for {@code player} this frame, as the third person gets it (the Steel Blade's hold or swing, the
     * guard included): for drawing the blade in first person, where the player model is never set up.
     */
    public static RigPose bladePose(AbstractClientPlayer player, float tickDelta) {
        KaguneBodyAnimation layer = layer(player);
        if (layer == null || layer.fade <= 0.0F) {
            RigPose p = new RigPose();
            float[] turn = SteelSwing.euler(SteelSwing.hold(ControlClient.worldTime(tickDelta) / 20.0, 0.0, 0.0, 0.0));
            p.rightArmHold = 1.0F;
            p.rightArmX = turn[0];
            p.rightArmY = turn[1];
            p.rightArmZ = turn[2];
            p.blade = 1.0F;
            p.bladeFit = 1.0F;
            return p;
        }
        layer.setupAnim(tickDelta);
        return layer.p;
    }

    /** 6.1.0: the body pose {@code player} was last drawn in (a copy), or null when the layer isn't posing them. */
    public static RigPose drawn(AbstractClientPlayer player) {
        KaguneBodyAnimation layer = layer(player);
        return layer == null || layer.fade <= 0.0F ? null : new RigPose().add(layer.p, 1.0F);
    }

    /**
     * 7.0.0: the pose {@code player}'s elbows and knees were last bent in (a copy), or null when nothing bends them: no bendy-lib, 3D
     * Skin Layers keeping the limbs straight, or the layer not posing them (neither a kagune's pose nor the idle stance).
     */
    public static RigPose limbs(AbstractClientPlayer player) {
        if (!BodyAnimations.limbsBend()) {
            return null;
        }
        KaguneBodyAnimation layer = layer(player);
        return layer == null || layer.fade <= 0.0F && !layer.idle.showing() ? null : new RigPose().add(layer.p, 1.0F);
    }

    /**
     * 7.0.0: the pose {@code player}'s body was drawn in lately (a copy; {@code time} the world clock), or null when nothing poses it
     * or the model hasn't been drawn lately (as in first person): what grows out of the body rides it.
     */
    public static RigPose body(AbstractClientPlayer player, double time) {
        KaguneBodyAnimation layer = layer(player);
        double[] last = TORSO.get(player);
        if (layer == null || last == null || Math.abs(time - last[4]) > TORSO_STALE || layer.fade <= 0.0F && !layer.idle.showing()) {
            return null;
        }
        return new RigPose().add(layer.p, 1.0F);
    }

    /** 6.2.0: the body pose {@code player} is drawn in this very frame (a copy), whatever she is doing; null when it isn't. */
    public static RigPose drawnNow(AbstractClientPlayer player, double time) {
        KaguneBodyAnimation layer = layer(player);
        if (layer == null || layer.fade <= 0.0F || !(Math.abs(time - layer.palePosedAt) < 0.01)) {
            return null;
        }
        return new RigPose().add(layer.p, 1.0F);
    }

    /**
     * 6.2.0: the body pose {@code player} is drawn in this very frame ({@code time}, PaleClient's clock) with her {@code move} on it (a
     * copy); null when it isn't: the model wasn't set up this frame (in first person it never is, and the pose last drawn is stale),
     * or the layer isn't putting that move on her (the body animations off).
     */
    public static RigPose drawn(AbstractClientPlayer player, com.hex.kagune.control.pale.PaleMove move, double time) {
        KaguneBodyAnimation layer = layer(player);
        if (layer == null || layer.fade <= 0.0F || !(Math.abs(time - layer.palePosedAt) < 0.01) || layer.paleMoves.posing() != move) {
            return null;
        }
        return new RigPose().add(layer.p, 1.0F);
    }

    /**
     * 6.2.4: the body pose {@code player} is in this frame (a copy), run here as the model would run it if her model has not been set
     * up this frame (in the Owl her body is not drawn: the creature is, in its place, carried over from this); null when there is none.
     */
    public static RigPose posed(AbstractClientPlayer player, float tickDelta, double time) {
        KaguneBodyAnimation layer = layer(player);
        if (layer == null || layer.fade <= 0.0F) {
            return null;
        }
        if (!(Math.abs(time - layer.palePosedAt) < 0.01)) {
            layer.setupAnim(tickDelta);
        }
        if (!(Math.abs(time - layer.palePosedAt) < 0.01)) {
            return null;
        }
        return new RigPose().add(layer.p, 1.0F);
    }

    /**
     * 6.2.0: in first person, where the model is never set up, her body this frame exactly as third person poses it (the layer run
     * as the model would run it, once a frame: her strikes chain out of the pose before as they do outside), when her arms are a
     * strike's or the Big Punch's or still up in the guard after one; else null. What her own arms are drawn on in first person.
     * Called every frame she is seen from her own eyes, so a strike begins from what she was really doing.
     */
    public static RigPose ownArms(AbstractClientPlayer player, float tickDelta, double time) {
        KaguneBodyAnimation layer = layer(player);
        if (layer == null || layer.fade <= 0.0F) {
            return null;
        }
        if (!(Math.abs(time - layer.palePosedAt) < 0.01)) {
            layer.setupAnim(tickDelta);
        }
        if (!(Math.abs(time - layer.palePosedAt) < 0.01) || !layer.paleMoves.arms(time)) {
            return null;
        }
        return new RigPose().add(layer.p, 1.0F);
    }

    /**
     * 6.0.4: how much of the Steel Blade is out for {@code player} as their arm was last posed (1 all of it; less while it rests
     * drawn in off the ground, {@link RigPose#bladeReach}): the third-person blade is drawn with it.
     */
    public static double bladeReach(AbstractClientPlayer player) {
        KaguneBodyAnimation layer = layer(player);
        return layer == null || layer.fade <= 0.0F ? 1.0 : layer.p.bladeReach;
    }

    @Override
    public void setupAnim(float tickDelta) {
        RigPose p = this.p.clear();
        this.palePosedAt = Double.NaN;
        this.pose(tickDelta);
        // 1.3.3: hand in hand with Eto (she walks at their right): the right arm out to her side, a little forward. 1.3.5: held still,
        // none of the walk's swing or the idle bob on it (her hand in it doesn't swing either, EtoPose), stretched out further.
        float hand = com.hex.kagune.control.client.EtoClient.handHeld(this.player.m_19879_()) * this.fade;
        if (hand > 0.0F) {
            p.rightArmZ += com.hex.kagune.control.npc.EtoHand.ARM * hand;
            p.rightArmX -= com.hex.kagune.control.npc.EtoHand.FORWARD * hand;
            p.rightArmHold = Math.max(p.rightArmHold, hand);
        }
        // 6.2.0: just whole again after her death that did not take, she stands as the pieces went back together and eases into it.
        float risen = com.hex.kagune.control.client.ReviveClient.risen(this.player, com.hex.kagune.control.client.PaleClient.clock(tickDelta));
        if (risen < 1.0F) {
            RigPose eased = p.scale(risen);
            p.clear().add(eased, 1.0F);
        }
        if (PathClient.get(this.player) == KagunePath.PALE || com.hex.kagune.control.client.PaleClient.eyes(this.player)) {
            this.paleMoves.shown(p); // a strike or a flip starts from exactly this
        }
        TORSO.put(this.player, new double[] {p.lean, p.side, p.twist, p.drop, ControlClient.worldTime(tickDelta)});
        this.posedAt = ControlClient.worldTime(tickDelta);
    }

    /**
     * 1.0.15: {@code player}'s body as it is posed this frame (posed now if nothing has yet: in first person the model is never drawn),
     * or null when nothing poses it.
     */
    public static RigPose current(AbstractClientPlayer player, float tickDelta) {
        KaguneBodyAnimation layer = layer(player);
        if (layer == null || layer.fade <= 0.0F) {
            return null;
        }
        if (!(Math.abs(ControlClient.worldTime(tickDelta) - layer.posedAt) < 1.0E-3)) {
            layer.setupAnim(tickDelta);
        }
        return new RigPose().add(layer.p, 1.0F);
    }

    private void pose(float tickDelta) {
        RigPose p = this.p;
        if (this.fade <= 0.0F) {
            return;
        }
        KaguneClientState.View view = KaguneClientState.get(this.player);
        ControlView control = ControlClient.get(this.player);
        double time = ControlClient.worldTime(tickDelta);
        float now = this.age + tickDelta;
        boolean feral = this.feral();
        float struggle = feral ? control.struggle() : 0.0F;
        float calm = feral ? control.control() : 1.0F;
        float low = 1.0F - calm;
        float frenzy = feral ? control.frenzy() : 0.0F;
        float hunger = feral ? control.hunger() : 0.0F;
        float stance = this.idle.weight(tickDelta);
        // 5.0.19: the Steel Blade arm holds its own pose ({@link #carry}), whatever else the body does; with the body animations
        // turned off, that is all there is (without it the blade would hang into the ground).
        float steel = (float)UkakuClient.blade(this.player, time);
        this.steelArm = null;
        if (!ControlConfig.bodyAnimations()) {
            this.finish(steel, time, tickDelta);
            return;
        }
        // 1.3.3: only holding Eto's hand (no kagune, no eyes, nothing turning): nothing more on the body than the arm (setupAnim's).
        if (!view.active() && !com.hex.kagune.control.client.PaleClient.eyes(this.player) && this.king() <= 0.0F
            && com.hex.kagune.control.client.TurningClient.progress(this.player, time) < 0.0
            && com.hex.kagune.control.client.EtoClient.handHeld(this.player.m_19879_()) > 0.0F) {
            this.finish(steel, time, tickDelta);
            return;
        }
        // 1.0.7: War's body is his own: none of Rinkaku's animal stance, spasms or urges under it. 1.0.15: ahead of the eyes' own moves,
        // or a final kakuja with the eyes open lost its every pose.
        float war = this.king();
        if (war > 0.0F) {
            this.war(p, time, tickDelta, war);
            this.finish(steel, time, tickDelta);
            return;
        }
        if (com.hex.kagune.control.client.PaleClient.eyes(this.player)) {
            // 7.0.1: the eyes alone (the shared eye-mode fight): her body's moves on it, nothing of any kagune's.
            this.eyeMoves(tickDelta, now);
            this.finish(steel, time, tickDelta);
            return;
        }

        // 1.0.11: turning into the Dragon, the body is the turning's alone.
        double turning = com.hex.kagune.control.client.TurningClient.progress(this.player, time);
        if (turning >= 0.0) {
            WarPose.turning(p, turning, time, com.hex.kagune.control.king.TurningModel.scream(turning), com.hex.kagune.control.king.TurningModel.burst(turning));
            this.finish(steel, time, tickDelta);
            return;
        }
        // The ambient stance (Ukaku relaxed, Rinkaku animalistic) when standing still.
        p.add(this.idle.pose(tickDelta), 1.0F);
        if (PathClient.get(this.player) == KagunePath.PALE) {
            // 6.1.0: Pale at play: hip-shot, gestures now and then, and a loose saunter when she walks (6.2.0: none of it in the Owl).
            double owlClock = com.hex.kagune.control.client.PaleClient.clock(tickDelta);
            com.hex.kagune.control.client.OwlClient.Owl owl = com.hex.kagune.control.client.OwlClient.get(this.player, owlClock);
            float play = owl == null ? 1.0F : (float)(1.0 - OwlPose.weight(owl.on(), owl.age(owlClock)));
            float strolling = this.lastPaleStrolling + (this.paleStrolling - this.lastPaleStrolling) * tickDelta;
            p.add(this.paleIdle.pose(tickDelta, stance, strolling, BodyAnimations.limbsBend()), play);
            if (strolling > 0.0F) {
                float stride = this.player.f_267362_.m_267590_(tickDelta) * 0.6662F;
                float walk = Math.min(1.0F, this.player.f_267362_.m_267711_(tickDelta) * 1.4F) * strolling;
                p.twist += 0.08F * (float)Math.sin(stride) * walk;
                p.rightArmZ += 0.06F * walk;
                p.leftArmZ -= 0.06F * walk;
            }
            // Her strikes and flips take the body over, from this very pose, and give it back.
            com.hex.kagune.control.client.PaleClient.PaleView move = com.hex.kagune.control.client.PaleClient.get(this.player);
            float bodyYaw = this.player.f_20884_ + wrapDegrees(this.player.f_20883_ - this.player.f_20884_) * tickDelta;
            float turn = (float)Math.toRadians(wrapDegrees(move.yaw() - bodyYaw));
            double clock = com.hex.kagune.control.client.PaleClient.clock(tickDelta);
            // 6.2.0: the Owl growing on her, in it, or going: under its own moves (they take its stance over and give it back), over
            // anything else (a strike's guard as she grows it is taken over by it).
            boolean owlMove = move.move().owl() || this.paleMoves.owlPosing(clock);
            if (owl != null && owlMove) {
                OwlPose.body(p, owl.on(), owl.age(clock), clock);
            }
            this.paleMoves.apply(p, move, clock, now, this.landedAt, turn);
            if (owl != null && !owlMove) {
                OwlPose.body(p, owl.on(), owl.age(clock), clock);
            }
            // 6.2.0: her Mutated Arm's weight on her, whatever she is doing.
            com.hex.kagune.control.client.ArmClient.Arm arm = com.hex.kagune.control.client.ArmClient.get(this.player);
            if (arm != null) {
                double[] look = arm.aim(tickDelta);
                double yaw = Math.toRadians(bodyYaw);
                double[] aim = {-look[0] * Math.cos(yaw) - look[2] * Math.sin(yaw), look[1], -look[0] * Math.sin(yaw) + look[2] * Math.cos(yaw)};
                ArmPose.body(p, arm.phase, arm.age(clock), arm.reach(clock), aim, arm.afterSphere(), clock);
            }
            if (owl != null) {
                // 6.2.0: her heavy head turns and nods only so far in the Owl.
                float headYaw = this.player.f_20886_ + wrapDegrees(this.player.f_20885_ - this.player.f_20886_) * tickDelta;
                OwlPose.neck(p, Math.toRadians(wrapDegrees(headYaw - bodyYaw)), Math.toRadians(this.player.m_146909_()),
                    OwlPose.weight(owl.on(), owl.age(clock)));
            }
            this.palePosedAt = clock;
        }
        if (PathClient.get(this.player) == KagunePath.GRAFT) {
            // 7.0.0: Graft DNA: the kakuja's predator stance, and his body behind every blow (GraftPose).
            double clock = com.hex.kagune.control.client.PaleClient.clock(tickDelta);
            com.hex.kagune.control.client.GraftClient.View graft = com.hex.kagune.control.client.GraftClient.get(this.player);
            boolean kakujaMove = graft.move().stage() == com.hex.kagune.control.graft.GraftStage.KAKUJA;
            com.hex.kagune.control.graft.GraftStage stage = kakujaMove ? com.hex.kagune.control.graft.GraftStage.KAKUJA : graft.stage();
            // 7.0.2: and the job running alongside, if it is this kagune's (the kakuja's jobs while he is in it).
            com.hex.kagune.control.graft.GraftMove job = graft.job();
            boolean ours = job.stage() == stage || graft.kakuja() && job.stage() == com.hex.kagune.control.graft.GraftStage.KAKUJA;
            GraftPose.body(p, stage, stage.limbs(view.mastery()), graft.move(), graft.age(clock), ours ? job : com.hex.kagune.control.graft.GraftMove.NONE,
                graft.jobAge(clock), com.hex.kagune.control.client.GraftClient.hide(graft, clock), clock);
        }
        if (UkakuClient.kakuja(this.player)) {
            // In a kakuja he moves like a fighter: knees driving, arms pumping, tucking in the air and soaking up landings.
            this.kakujaLocomotion(tickDelta, view.mode() == TendrilMode.SWIPE ? 0.3F : 1.0F, steel);
        }

        if (feral) {
            // Breathing and predator posture (the animalistic stance already carries its own hunch).
            float posture = 1.0F - 0.7F * stance;
            float rate = 0.1F + 0.2F * struggle * (0.6F + 0.4F * frenzy);
            float breath = (float)Math.sin(now * rate);
            p.lean += posture * (0.08F + 0.3F * struggle + 0.08F * hunger * low) + breath * (0.012F + 0.035F * struggle);
            p.headX += posture * (0.04F + 0.1F * struggle) - 0.08F * struggle * posture - breath * (0.01F + 0.025F * struggle);
            float shoulders = posture * (0.05F + 0.06F * struggle);
            p.rightArmZ += shoulders - breath * 0.02F * struggle;
            p.leftArmZ -= shoulders - breath * 0.02F * struggle;
            p.rightArmX += breath * 0.03F * struggle;
            p.leftArmX += breath * 0.03F * struggle;

            // Tremor: fine, smooth, never per-frame random.
            float tremor = struggle <= 0.05F ? 0.0F : (0.012F + 0.045F * struggle * (0.6F + 0.4F * frenzy));
            p.rightArmX += tremor * noise(now, 2.9F, 0.0F);
            p.leftArmX += tremor * noise(now, 3.3F, 1.7F);
            p.rightArmZ += tremor * 0.6F * noise(now, 3.7F, 3.1F);
            p.leftArmZ += tremor * 0.6F * noise(now, 4.1F, 4.4F);
            p.headY += tremor * 0.8F * noise(now, 2.3F, 5.2F);
            p.headX += tremor * 0.5F * noise(now, 2.7F, 6.6F);

            // Spasms: fast attack, slower decay.
            if (this.spasmKind >= 0) {
                float spasmAge = now - this.spasmStart;
                float envelope = spasmAge < 1.5F ? spasmAge / 1.5F : (float)Math.exp(-(spasmAge - 1.5F) * 0.45F);
                float jerk = this.spasmAmplitude * envelope;
                switch (this.spasmKind) {
                    case 0 -> { p.headY += jerk * 1.4F; p.headZ += jerk * 0.6F; }
                    case 1 -> { p.rightArmZ += Math.abs(jerk); p.rightArmX -= jerk * 0.8F; p.side += jerk * 0.3F; }
                    case 2 -> { p.lean += Math.abs(jerk) * 0.7F; p.twist += jerk * 0.4F; p.headX -= jerk * 0.5F; }
                    default -> { p.leftArmZ -= Math.abs(jerk); p.leftArmX += jerk * 0.9F; p.headZ -= jerk * 0.4F; }
                }
            }

            // Clutching the small of the back, where the kagune tears out.
            float clutchAge = now - this.clutchStart;
            if (clutchAge >= 0.0F && clutchAge <= this.clutchLength) {
                float clutch = window(clutchAge, this.clutchLength, 6.0F) * Math.max(0.35F, struggle);
                p.rightArmX += 0.75F * clutch;
                p.leftArmX += 0.75F * clutch;
                p.rightArmZ += 0.3F * clutch;
                p.leftArmZ -= 0.3F * clutch;
                p.rightElbow += 0.9F * clutch;
                p.leftElbow += 0.9F * clutch;
                p.lean += 0.22F * clutch;
                p.headX += 0.1F * clutch;
            }

            // Stumbling while walking untrained.
            float walk = this.player.f_267362_.m_267711_(tickDelta);
            float stride = this.player.f_267362_.m_267590_(tickDelta);
            float stumble = struggle * Math.min(1.0F, walk * 1.5F);
            p.side += (float)Math.sin(stride * 0.6662F) * 0.07F * stumble;
            p.headZ -= (float)Math.sin(stride * 0.6662F) * 0.05F * stumble;
        }

        switch (view.mode()) {
            case MANIFEST -> this.manifest(view, time, low, calm, feral);
            case DISMISS -> {
                float slump = (float)Math.sin(Math.PI * progress(view, time)) * low;
                p.lean += 0.25F * slump;
                p.headX += 0.1F * slump;
                p.rightArmX += 0.15F * slump;
                p.leftArmX += 0.15F * slump;
            }
            case SWIPE -> {
                if (control.describesSwipe(view.modeStarted())) {
                    this.strike(view, control, time, low, tickDelta);
                } else {
                    this.jab(progress(view, time), 1.0F, low);
                }
            }
            case RAPID_STRIKE -> {
                double age = time - view.modeStarted();
                float local = (float)(age / 4.0 - Math.floor(age / 4.0));
                this.jab(local, ((int)Math.floor(age / 4.0) & 1) == 0 ? 1.0F : -1.0F, low);
            }
            case REACH_ENTITY, REACH_ITEM, REACH_BLOCK -> this.reach(view, -1.25F * window01(progress(view, time)));
            case HOLD, LIFT -> {
                p.rightArmX -= 1.05F;
                p.leftArmX -= 1.05F;
                p.lean += 0.12F;
                p.rightArmX += 0.04F * noise(now, 3.1F, 0.4F);
                p.leftArmX += 0.04F * noise(now, 3.4F, 2.2F);
            }
            case CRUSH -> {
                float squeeze = (float)Math.sin(Math.PI * progress(view, time));
                p.rightArmX -= 1.2F;
                p.leftArmX -= 1.2F;
                p.rightArmY -= 0.35F * squeeze;
                p.leftArmY += 0.35F * squeeze;
                p.rightElbow += 0.6F * squeeze;
                p.leftElbow += 0.6F * squeeze;
                p.lean += 0.2F * squeeze;
            }
            case SLAM -> {
                float progress = progress(view, time);
                float lift = progress < 0.35F ? smooth(progress / 0.35F) : 1.0F - smooth((progress - 0.35F) / 0.3F);
                float drive = progress < 0.35F ? 0.0F : window01((progress - 0.35F) / 0.65F);
                p.rightArmX -= 2.3F * lift + 0.6F * drive;
                p.leftArmX -= 2.3F * lift + 0.6F * drive;
                p.lean += 0.8F * drive - 0.12F * lift;
            }
            case SLINGSHOT_REACH, SLINGSHOT_HOLD -> {
                p.rightArmX -= 2.2F;
                p.leftArmX -= 0.5F;
                p.lean -= 0.08F;
            }
            default -> {
            }
        }
        if (view.guarding()) {
            p.rightArmX -= 1.0F;
            p.leftArmX -= 1.0F;
            p.rightArmY -= 0.5F;
            p.leftArmY += 0.5F;
            p.lean += 0.1F;
        }
        // Ukaku's Wing Vortex: the body coils, then the arm on the thrown wing's side whips it forward in a spiral.
        float[] vortex = UkakuClient.vortexPose(this.player, time);
        if (vortex != null) {
            UkakuBody.Pose pose = UkakuBody.vortex(vortex[1] > 0.0F, vortex[0], UkakuClient.vortexRelease());
            p.twist += pose.twist;
            this.addUkaku(pose, 1.0F, 0.0F);
        }
        // Ukaku's Gatling: both palms out at the target, braced against the stream.
        float gun = UkakuClient.gatlingAmount(this.player, time);
        if (gun > 0.0F) {
            UkakuBody.Pose pose = UkakuBody.gatling(gun, time);
            p.twist += pose.twist;
            this.addUkaku(pose, 1.0F, 0.0F);
        }
        if (PathClient.get(this.player) != KagunePath.PALE && this.paleMoves.busy(com.hex.kagune.control.client.PaleClient.clock(tickDelta))) {
            // 7.0.1: a move of the eyes alone cut off by the kagune coming out: blended out over whatever the kagune does.
            this.eyeMoves(tickDelta, now);
        }
        this.urge(control, time, feral);
        this.finish(steel, time, tickDelta);
    }

    /** 1.0.5: War's body: his stance and his twitching head (WarPose), and (1.0.6) his moves, the twitching half given way to them. */
    private void war(RigPose p, double time, float tickDelta, float war) {
        com.hex.kagune.control.client.KingClient.View worn = com.hex.kagune.control.client.KingClient.get(this.player);
        double since = worn == null ? 1.0E9 : time - worn.since();
        if (worn != null && com.hex.kagune.control.king.KingTransform.coming(worn.form(), worn.on(), since)) {
            // 1.0.17: coming over him: the transformation's body, his stance coming in under it over its last ten ticks.
            boolean isWar = worn.form() == com.hex.kagune.control.king.KingForm.WAR;
            double ticks = com.hex.kagune.control.king.KingTransform.ticks(worn.form());
            float stance = (float)Math.max(0.0, Math.min(1.0, (since - (ticks - 10.0)) / 10.0));
            if (isWar) {
                WarPose.apply(p, time, this.player.m_20148_().hashCode(), stance, 0.0F);
            } else {
                WarPose.peace(p, time, stance);
            }
            RigPose coming = new RigPose();
            WarPose.coming(coming, isWar, since, time);
            p.add(coming, 1.0F - stance * stance);
            return;
        }
        if (worn != null && com.hex.kagune.control.king.KingTransform.going(worn.form(), worn.on(), since)) {
            // 1.0.19: leaving him: his stance let go at once, the leaving's body (which ends as his own).
            boolean isWar = worn.form() == com.hex.kagune.control.king.KingForm.WAR;
            float stance = (float)Math.max(0.0, 1.0 - since / 4.0);
            if (isWar) {
                WarPose.apply(p, time, this.player.m_20148_().hashCode(), stance, 0.0F);
            } else {
                WarPose.peace(p, time, stance);
            }
            WarPose.going(p, isWar, since, time);
            return;
        }
        // 1.0.26: its run: the stride, how hard and how fast, how free of blows its upper body is; a blow's legs only in the air.
        float run = between(this.runWas, this.run, tickDelta);
        float sprinting = between(this.sprintWas, this.sprint, tickDelta);
        float free = between(this.freeWas, this.free, tickDelta);
        float blowLegs = between(this.blowLegsWas, this.blowLegs, tickDelta);
        double phase = this.runPhaseWas + (this.runPhase - this.runPhaseWas) * tickDelta;
        if (worn != null && worn.form() == com.hex.kagune.control.king.KingForm.PEACE) {
            WarPose.peace(p, time, war * (1.0F - 0.7F * run)); // 1.0.8 (1.0.26: giving way to its run)
            KingRun.peace(p, phase, war * run, sprinting, free, time);
            com.hex.kagune.control.king.KingMove move = worn.moveAt(time);
            if (move != com.hex.kagune.control.king.KingMove.NONE) {
                // 1.0.16: its moves (PeacePose), and through them square to where he looks: the blades on his back strike where the
                // server has them strike (along his look), so his body faces that way while they do. 1.0.26: on the ground, from the
                // hips up only (the legs run on), the turn to his look a turn of the chest.
                RigPose blow = new RigPose();
                PeacePose.act(blow, move, time - worn.moveAt(), worn.level(), war);
                float bodyYaw = this.player.f_20884_ + wrapDegrees(this.player.f_20883_ - this.player.f_20884_) * tickDelta;
                float headYaw = this.player.f_20886_ + wrapDegrees(this.player.f_20885_ - this.player.f_20886_) * tickDelta;
                blow.spin -= (float)(war * Math.toRadians(wrapDegrees(headYaw - bodyYaw)));
                p.add(blow.legs(blowLegs, BLOW_TWIST), 1.0F);
            }
            return;
        }
        float moving = Math.min(1.0F, this.player.f_267362_.m_267711_(tickDelta) * 1.4F);
        com.hex.kagune.control.client.KingClient.View king = com.hex.kagune.control.client.KingClient.get(this.player);
        com.hex.kagune.control.king.KingMove move = king == null ? com.hex.kagune.control.king.KingMove.NONE : king.moveAt(time);
        boolean acting = move != com.hex.kagune.control.king.KingMove.NONE;
        int seed = this.player.m_20148_().hashCode();
        WarPose.apply(p, time, seed, war * (acting ? 0.5F : 1.0F) * (1.0F - 0.6F * run), moving);
        KingRun.war(p, phase, war * run, sprinting, free, time, seed); // 1.0.26: the warpath
        // 1.0.26: his blows from the hips up on the ground (the legs run on under them); in the air, or a move that moves him, all of him.
        RigPose blow = new RigPose();
        if (acting && move == com.hex.kagune.control.king.KingMove.STRIKE) {
            // 1.0.14: his M1, each blow taking over from the one it cut off.
            int wasBlow = king.was() == com.hex.kagune.control.king.KingMove.STRIKE ? king.wasLevel() : -1;
            WarPose.strikes(blow, king.level(), time - king.moveAt(), wasBlow, king.moveAt() - king.wasAt(), war);
        } else if (acting && move == com.hex.kagune.control.king.KingMove.REAPER) {
            // 1.0.28: each phase out of the one before it.
            int from = king.was() == com.hex.kagune.control.king.KingMove.REAPER ? king.wasLevel() : -1;
            WarPose.reaper(blow, king.level(), time - king.moveAt(), from, war);
        } else if (acting && move == com.hex.kagune.control.king.KingMove.SPIRE_SEND) {
            // 1.0.28: held out until the mass has come back (its timeline is the effect's).
            double[] ends = com.hex.kagune.control.client.fx.WarHoldFx.spireEnds(this.player);
            WarPose.spireSend(blow, time - king.moveAt(), ends[0], ends[1], war);
        } else if (acting) {
            WarPose.act(blow, move, time - king.moveAt(), king.level(), war);
        }
        if (acting) {
            p.add(blow.legs(blowLegs, BLOW_TWIST), 1.0F);
        }
    }

    /** 1.0.26: how far a blow's turn of the whole body may turn the chest instead, on the ground (radians). */
    static final float BLOW_TWIST = 0.9F;

    /** 7.0.1: the shared eye-mode fight's move on the body, out of the pose so far and back to it, as Pale's are put on hers. */
    private void eyeMoves(float tickDelta, float now) {
        com.hex.kagune.control.client.PaleClient.PaleView move = com.hex.kagune.control.client.PaleClient.get(this.player);
        float bodyYaw = this.player.f_20884_ + wrapDegrees(this.player.f_20883_ - this.player.f_20884_) * tickDelta;
        float turn = (float)Math.toRadians(wrapDegrees(move.yaw() - bodyYaw));
        double clock = com.hex.kagune.control.client.PaleClient.clock(tickDelta);
        this.paleMoves.apply(this.p, move, clock, now, this.landedAt, turn);
        this.palePosedAt = clock;
    }

    /**
     * Settles the pose (none while lying down or riding, half while using an item) and puts the Steel Blade's carry on: always
     * while it is out, riding or using an item too, except lying down (asleep, swimming, gliding).
     */
    private void finish(float steel, double time, float tickDelta) {
        boolean lying = this.player.m_5803_() || this.player.m_21255_() || this.player.m_6069_();
        float damp = this.fade;
        if (lying || this.player.m_20159_()) {
            damp = 0.0F;
        } else if (this.player.m_6117_()) {
            damp *= 0.5F;
        }
        this.p.settle(damp);
        if (steel > 0.0F && !lying) {
            this.carry(steel * Math.min(1.0F, this.fade), steel, time, tickDelta);
        }
    }

    /**
     * The Steel Blade's arm, always on while it is out: the right arm drops vanilla's own swing, crouch and bob and holds the
     * blade in its relaxed hold ({@link SteelSwing#hold}: hanging down at his right side, drawn in to rest just off the ground,
     * breathing, swaying with his stride, swinging back as he runs, now and then turned over in his hand); a steel swing ({@link SteelSwing#swing})
     * takes the arm from wherever it is and gives it back to the hold. The arm is sealed in steel, so it doesn't bend. And
     * whatever the pose, the guard keeps every part of the blade off the ground ({@link BladeCarry#guard}, applied in
     * {@link RigPose}, where the arm's final turn is known).
     */
    private void carry(float weight, float steel, double time, float tickDelta) {
        RigPose p = this.p;
        double[] arm = this.steelArm;
        double fit;
        if (arm == null && this.wasSwinging && this.lastArm != null) {
            // The swing just ended (or was cut short, the combo broken off): eased back into the hold from where it was, from this
            // very frame (6.0.4: until now the first frame snapped to the hold).
            this.settleFrom = this.lastArm;
            this.settleFit = this.lastFit;
            this.settleAt = time;
        }
        if (arm == null) {
            arm = this.hold(time, tickDelta);
            fit = 1.0;
            if (this.settleFrom != null) {
                double since = time - this.settleAt;
                if (since < SETTLE_TICKS) {
                    double t = Math.max(0.0, since) / SETTLE_TICKS;
                    double eased = t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
                    arm = SteelSwing.slerp(this.settleFrom, arm, eased);
                    fit = this.settleFit + (1.0 - this.settleFit) * eased;
                }
            }
        } else {
            fit = this.steelFit;
        }
        this.wasSwinging = this.steelArm != null;
        this.lastArm = arm;
        this.lastFit = fit;
        // 6.0.4: resting, the blade draws in to hang off the ground; it drives out as a swing winds up.
        p.bladeFit = (float)fit;
        float[] turn = SteelSwing.euler(arm);
        p.rightArmHold = weight;
        p.rightArmX += weight * turn[0];
        p.rightArmY += weight * turn[1];
        p.rightArmZ += weight * turn[2];
        p.rightElbow *= 1.0F - weight;
        boolean crouching = this.player.m_6047_();
        p.blade = (float)SteelBlade.extend(steel);
        p.bladeSlim = "slim".equals(this.player.m_108564_());
        p.ground = crouching ? BladeCarry.CROUCH_GROUND : BladeCarry.GROUND;
        p.shoulderY = crouching ? BladeCarry.CROUCH_SHOULDER : BladeCarry.SHOULDER;
    }

    /** The Steel Blade's relaxed hold this frame ({@link SteelSwing#hold}), with his stride and how fast he runs. */
    private double[] hold(double time, float tickDelta) {
        AbstractClientPlayer player = this.player;
        double dx = player.m_20185_() - player.f_19790_;
        double dz = player.m_20189_() - player.f_19792_;
        double run = Math.max(0.0, Math.min(1.0, (Math.sqrt(dx * dx + dz * dz) - 0.2) / 0.2));
        double stride = player.f_267362_.m_267590_(tickDelta) * 0.6662;
        double walk = Math.min(1.0, player.f_267362_.m_267711_(tickDelta));
        // Crouched it hangs higher, on the same frame vanilla's crouch drops the model.
        return SteelSwing.hold(time / 20.0, walk, stride, run, player.m_6047_() ? 1.0 : 0.0);
    }

    /**
     * Ukaku's kakuja locomotion ({@link KakujaBody}) on top of vanilla's walk; the arms at {@code arms} while a strike has them,
     * the right one left alone as far as the Steel Blade is out ({@code steel}: it holds the blade).
     */
    private void kakujaLocomotion(float tickDelta, float arms, float steel) {
        AbstractClientPlayer player = this.player;
        double dx = player.m_20185_() - player.f_19790_;
        double dz = player.m_20189_() - player.f_19792_;
        float speed = (float)Math.sqrt(dx * dx + dz * dz);
        float run = Math.max(0.0F, Math.min(1.0F, (speed - 0.17F) / 0.11F));
        float stride = player.f_267362_.m_267590_(tickDelta) * 0.6662F;
        float swing = Math.min(1.0F, player.f_267362_.m_267711_(tickDelta));
        float vertical = (float)(player.m_20186_() - player.f_19791_);
        float sinceLand = this.age + tickDelta - this.landedAt;
        KakujaBody.Pose k = KakujaBody.pose(new KakujaBody.Motion(stride, swing, run, !player.m_20096_(), vertical, sinceLand, this.landSpeed));
        RigPose p = this.p;
        p.lean += k.lean;
        p.twist += k.twist;
        p.drop += k.drop;
        p.headX += k.headX;
        float right = arms * (1.0F - steel);
        p.rightArmX += k.rightArmX * right;
        p.rightArmZ += k.rightArmZ * right;
        p.rightElbow += k.rightElbow * right;
        p.leftArmX += k.leftArmX * arms;
        p.leftArmZ += k.leftArmZ * arms;
        p.leftElbow += k.leftElbow * arms;
        p.rightLegX += k.rightLegX;
        p.rightLegZ += k.rightLegZ;
        p.rightKnee += k.rightKnee;
        p.leftLegX += k.leftLegX;
        p.leftLegZ += k.leftLegZ;
        p.leftKnee += k.leftKnee;
    }

    private void manifest(KaguneClientState.View view, double time, float low, float calm, boolean feral) {
        RigPose p = this.p;
        float envelope = window01(progress(view, time));
        if (feral) {
            double age = time - view.modeStarted();
            int limbs = Math.max(1, view.transitionToLimbCount());
            float jolt = 0.0F;
            if (age >= 0.0 && age < limbs * 7.0) {
                jolt = (float)Math.exp(-(age % 7.0) * 0.6);
            }
            float shake = low * envelope;
            float now = (float)time;
            p.lean -= 0.25F * jolt * low;
            p.headX -= 0.3F * jolt * low;
            p.side += 0.07F * shake * noise(now, 5.3F, 0.9F);
            p.headY += 0.1F * shake * noise(now, 6.1F, 2.3F);
            p.rightArmX += 0.12F * shake * noise(now, 5.7F, 4.1F);
            p.leftArmX += 0.12F * shake * noise(now, 6.4F, 5.5F);
            p.rightLegZ += 0.05F * shake;
            p.leftLegZ -= 0.05F * shake;
        }
        // A master summons the kagune deliberately: chest out, shoulders back, arms slightly spread.
        float deliberate = calm * calm * envelope;
        p.lean -= 0.1F * deliberate;
        p.headX += 0.05F * deliberate;
        p.rightArmZ += 0.28F * deliberate;
        p.leftArmZ -= 0.28F * deliberate;
        p.rightArmX += 0.15F * deliberate;
        p.leftArmX += 0.15F * deliberate;
    }

    /** Combo strike: windup coils toward the striking side, the strike whips through, recovery settles (or staggers). */
    private void strike(KaguneClientState.View view, ControlView control, double time, float low, float tickDelta) {
        RigPose p = this.p;
        ComboStage stage = control.stage();
        float progress = progress(view, time);
        int duration = Math.max(1, view.modeDuration());
        float impact = stage.impactTick(duration) / (float)duration;
        if (SteelSwing.swings(stage.pattern())) {
            this.steelStrike(stage, view.modeStarted(), progress, impact, time, tickDelta);
            return;
        }
        if (UkakuBody.poses(stage.pattern())) {
            this.ukakuStrike(stage, control, progress, impact, time, low);
            return;
        }
        float side = strikeSide(view, control);
        float weight = stage.weight();
        float amp = (0.8F + 0.5F * weight) * (1.0F + 0.3F * low);
        float coil;
        float release;
        if (progress < impact) {
            coil = smooth(progress / Math.max(0.05F, impact * 0.85F));
            release = 0.0F;
        } else {
            coil = 0.0F;
            float after = (progress - impact) / Math.max(0.05F, 1.0F - impact);
            release = after < 0.12F ? smooth(after / 0.12F) : 1.0F - smooth((after - 0.12F) / 0.88F);
        }
        float strikeArmX = 0.0F, strikeArmZ = 0.0F, offArmX = 0.0F, twist = 0.0F, lean = 0.0F, bend = 0.0F, head = 0.0F, lunge = 0.0F;
        float bothX = 0.0F, bothZ = 0.0F, bothY = 0.0F;
        StrikePattern pattern = stage.pattern();
        switch (pattern) {
            case LASH -> { twist = 0.45F * coil - 0.4F * release; strikeArmX = 0.45F * coil - 1.3F * release; strikeArmZ = 0.4F * release;
                lean = -0.05F * coil + 0.22F * release; offArmX = -0.3F * coil + 0.3F * release; }
            case BACKHAND -> { twist = -0.4F * coil + 0.45F * release; strikeArmX = -0.4F * coil - 1.2F * release; strikeArmZ = -0.45F * release;
                lean = 0.2F * release; offArmX = 0.25F * release; }
            case OVERHEAD -> { bothX = -2.5F * coil - 0.7F * release; lean = -0.15F * coil + 0.5F * release; bend = 0.45F * release;
                head = -0.25F * coil + 0.3F * release; lunge = 0.25F * release; }
            case THRUST -> { twist = 0.3F * coil - 0.3F * release; strikeArmX = 0.6F * coil - 1.5F * release; lean = -0.05F * coil + 0.3F * release;
                offArmX = 0.3F * release; lunge = 0.35F * release; }
            case RISING -> { lean = 0.35F * coil - 0.15F * release; strikeArmX = 0.3F * coil - 2.4F * release; bend = 0.3F * coil;
                twist = 0.15F * coil - 0.2F * release; }
            case SPIN -> { twist = 0.6F * coil - 0.8F * release; bothZ = 0.8F * (coil + release); lean = 0.15F * (coil + release); }
            case SCISSOR -> { bothZ = 0.9F * coil - 0.2F * release; bothX = -0.6F * coil - 1.3F * release; bothY = 0.6F * release;
                lean = 0.3F * release; }
            case IMPALE -> { bothX = 0.5F * coil - 1.5F * release; bothZ = 0.6F * coil; lean = -0.1F * coil + 0.4F * release;
                bend = 0.2F * release; lunge = 0.4F * release; }
            default -> {
            }
        }
        // Untrained hosts overshoot and stagger after the impact; involuntary strikes yank the body around.
        float after = progress < impact ? 0.0F : (progress - impact) / Math.max(0.05F, 1.0F - impact);
        float stagger = low * (after > 0.2F && after < 0.7F ? (float)Math.sin((after - 0.2F) / 0.5F * Math.PI) : 0.0F);
        if (control.comboInvoluntary()) {
            head += 0.25F * window01(progress) * noise((float)time, 4.0F, 1.0F);
            twist *= 1.3F;
        }
        boolean right = side >= 0.0F;
        p.twist += twist * side * amp;
        p.lean += (lean + bend) * amp - 0.18F * stagger;
        // The head rides the lean: keep the eyes on the target.
        p.headX += head - 0.6F * (lean + bend) * amp - 0.12F * stagger;
        p.headZ += 0.1F * stagger * side;
        float strikeX = (strikeArmX + bothX) * amp;
        float otherX = (offArmX + bothX) * amp;
        if (right) {
            p.rightArmX += strikeX;
            p.rightArmZ += (strikeArmZ + bothZ) * amp;
            p.rightArmY -= bothY * amp;
            p.leftArmX += otherX;
            p.leftArmZ -= bothZ * amp;
            p.leftArmY += bothY * amp;
        } else {
            p.leftArmX += strikeX;
            p.leftArmZ -= (strikeArmZ + bothZ) * amp;
            p.leftArmY += bothY * amp;
            p.rightArmX += otherX;
            p.rightArmZ += bothZ * amp;
            p.rightArmY -= bothY * amp;
        }
        // Weight transfer into the strike: lead leg forward, trailing leg back.
        float step = lunge * amp + 0.15F * weight * release;
        if (right) {
            p.leftLegX -= step;
            p.rightLegX += step * 0.6F;
        } else {
            p.rightLegX -= step;
            p.leftLegX += step * 0.6F;
        }
    }

    /** Ukaku's kicks and wing strikes (poses in {@link UkakuBody}). */
    private void ukakuStrike(ComboStage stage, ControlView control, float progress, float impact, double time, float low) {
        RigPose p = this.p;
        // Flame bursts follow the swiping hand (10 right, 11 left), not the wing that fires.
        boolean flame = stage.pattern().flame();
        boolean right = flame ? control.comboUsesLimb(10) : control.comboUsesLimb(0) || control.comboUsesLimb(8);
        boolean left = flame ? control.comboUsesLimb(11) : control.comboUsesLimb(1) || control.comboUsesLimb(9);
        UkakuBody.Pose pose = UkakuBody.pose(stage.pattern(), right || !left, right && left, progress, impact);
        float amp = 0.92F + 0.16F * stage.weight();
        float twist = pose.twist * amp;
        float head = 0.0F;
        if (control.comboInvoluntary()) {
            head = 0.25F * window01(progress) * noise((float)time, 4.0F, 1.0F);
            twist *= 1.3F;
        }
        p.twist += twist;
        this.addUkaku(pose, amp, head);
    }

    /**
     * 5.0.20: a Steel Blade swing ({@link SteelSwing#swing}): the body coils, cuts and steps into it, and the blade arm's whole
     * turn goes to {@link #carry} (it starts from wherever the blade was as the swing began, so one swing flows into the next,
     * and ends in the hold).
     */
    private void steelStrike(ComboStage stage, double startedAt, float progress, float impact, double time, float tickDelta) {
        RigPose p = this.p;
        double[] hold = this.hold(time, tickDelta);
        if (startedAt != this.swingStartedAt || this.swingFrom == null) {
            this.swingStartedAt = startedAt;
            this.swingFrom = this.lastArm != null ? this.lastArm : hold;
            this.fitFrom = this.lastFit;
        }
        SteelSwing.Pose pose = SteelSwing.swing(stage.pattern(), progress, impact, this.swingFrom, hold);
        this.steelFit = SteelSwing.fit(stage.pattern(), progress, impact, this.fitFrom);
        p.twist += pose.twist;
        p.lean += pose.torsoX;
        p.headX += pose.headX;
        p.headY += pose.headY;
        p.leftArmX += pose.leftArmX;
        p.leftArmY += pose.leftArmY;
        p.leftArmZ += pose.leftArmZ;
        p.leftElbow += pose.leftArmBend;
        p.rightLegX += pose.rightLegX;
        p.rightLegZ += pose.rightLegZ;
        p.leftLegX += pose.leftLegX;
        p.leftLegZ += pose.leftLegZ;
        p.rightKnee += pose.rightKnee;
        p.leftKnee += pose.leftKnee;
        this.steelArm = pose.arm;
    }

    /** Adds an Ukaku pose (everything but its twist) at strength {@code amp}, with {@code head} extra on the head's pitch. */
    private void addUkaku(UkakuBody.Pose pose, float amp, float head) {
        RigPose p = this.p;
        p.headX += pose.headX * amp + head - 0.6F * (pose.torsoX + pose.torsoBend) * amp;
        p.headY += pose.headY * amp;
        p.lean += (pose.torsoX + pose.torsoBend) * amp;
        p.rightArmX += pose.rightArmX * amp;
        p.rightArmY += pose.rightArmY * amp;
        p.rightArmZ += pose.rightArmZ * amp;
        p.rightElbow += pose.rightArmBend * amp;
        p.leftArmX += pose.leftArmX * amp;
        p.leftArmY += pose.leftArmY * amp;
        p.leftArmZ += pose.leftArmZ * amp;
        p.leftElbow += pose.leftArmBend * amp;
        p.rightLegX += pose.rightLegX * amp;
        p.rightLegZ += pose.rightLegZ * amp;
        p.leftLegX += pose.leftLegX * amp;
        p.leftLegZ += pose.leftLegZ * amp;
        p.rightKnee += pose.rightKnee * amp;
        p.leftKnee += pose.leftKnee * amp;
        p.flip += pose.flip;
    }

    private void jab(float progress, float side, float low) {
        RigPose p = this.p;
        float hit = (float)Math.sin(Math.PI * Math.min(1.0F, progress * 1.4F));
        if (side >= 0.0F) {
            p.rightArmX -= 1.3F * hit;
        } else {
            p.leftArmX -= 1.3F * hit;
        }
        p.twist += -0.25F * hit * side;
        p.lean += 0.15F * hit + 0.05F * low * hit;
    }

    private void reach(KaguneClientState.View view, float amount) {
        float side = view.form().limb(view.actionLimbIndex()).fanSide() < 0.0 ? 1.0F : -1.0F;
        if (side > 0.0F) {
            this.p.rightArmX += amount;
        } else {
            this.p.leftArmX += amount;
        }
        this.p.lean += -0.08F * amount;
    }

    /** Head and torso snap toward the prey; a STRIKE tell coils and trembles; a restrained urge folds the body away. */
    private void urge(ControlView control, double time, boolean feral) {
        RigPose p = this.p;
        if (control.urge() == UrgeType.NONE) {
            return;
        }
        float age = control.urgeAge(time);
        if (age < 0.0F) {
            return;
        }
        float length = Math.max(1, control.urgeDuration());
        float envelope = window(age, length + 4.0F, 3.0F) * control.urgeStrength();
        if (control.urgeRestrained()) {
            if (feral) {
                p.rightArmX += 0.6F * envelope;
                p.leftArmX += 0.6F * envelope;
                p.rightElbow += 0.7F * envelope;
                p.leftElbow += 0.7F * envelope;
                p.lean += 0.3F * envelope;
                p.headX += 0.1F * envelope;
            }
            return;
        }
        float relative = 0.0F;
        Entity target = this.player.m_9236_().m_6815_(control.urgeTargetId());
        if (target != null) {
            Vec3 from = this.player.m_20182_();
            Vec3 to = target.m_20182_();
            double yaw = Math.toDegrees(Math.atan2(-(to.f_82479_ - from.f_82479_), to.f_82481_ - from.f_82481_));
            double body = this.player.f_20884_ + (this.player.f_20883_ - this.player.f_20884_) * 0.5;
            relative = (float)Math.toRadians(Math.max(-70.0, Math.min(70.0, wrapDegrees(yaw - body))));
        }
        p.headY += relative * 0.7F * envelope;
        if (!feral) {
            return; // Ukaku only looks.
        }
        p.headX -= 0.15F * envelope;
        p.twist += relative * 0.3F * envelope;
        p.lean += 0.15F * envelope;
        if (relative >= 0.0F) {
            p.rightArmX -= 0.6F * envelope;
        } else {
            p.leftArmX -= 0.6F * envelope;
        }
        if (control.urge() == UrgeType.STRIKE) {
            float tremble = 0.06F * envelope * noise((float)time, 7.0F, 2.0F);
            p.lean += 0.3F * envelope;
            p.rightArmX += 0.5F * envelope + tremble;
            p.leftArmX += 0.5F * envelope - tremble;
        }
    }

    /** +1 when the striking limbs sit on the player's right (fanSide < 0), -1 on the left. */
    private static float strikeSide(KaguneClientState.View view, ControlView control) {
        KaguneForm form = view.form();
        double sum = 0.0;
        for (int limb = 0; limb < form.limbCount(); limb++) {
            if (control.comboUsesLimb(limb)) {
                double fan = form.limb(limb).fanSide();
                sum += Math.abs(fan) < 0.01 ? (limb % 2 == 0 ? 1.0 : -1.0) : -Math.signum(fan);
            }
        }
        return sum >= 0.0 ? 1.0F : -1.0F;
    }

    @Override
    public Vec3f get3DTransform(String modelName, TransformType type, float tickDelta, Vec3f value0) {
        if (this.fade <= 0.0F && !this.idle.showing()) {
            return value0;
        }
        int kind = type == TransformType.POSITION ? RigPose.POSITION : type == TransformType.ROTATION ? RigPose.ROTATION : RigPose.BEND;
        if (kind == RigPose.BEND && com.hex.kagune.control.client.render.PlayerLayers.skinLayers3d()) {
            // 3D Skin Layers draws the sleeves and trousers as straight voxel boxes on the parts: a bent elbow or knee would
            // leave them behind, so with it the limbs stay straight.
            return value0;
        }
        float x = value0.getX();
        float y = value0.getY();
        float z = value0.getZ();
        float[] out = this.p.transform(modelName, kind, x, y, z);
        if (out[0] == x && out[1] == y && out[2] == z) {
            return value0;
        }
        return new Vec3f(out[0], out[1], out[2]);
    }

    private static float progress(KaguneClientState.View view, double time) {
        return view.modeDuration() <= 0 ? 1.0F : (float)Math.max(0.0, Math.min(1.0, (time - view.modeStarted()) / view.modeDuration()));
    }

    /** Smooth, bounded pseudo-noise in [-1, 1]. */
    private static float noise(float time, float frequency, float phase) {
        return (float)(Math.sin(time * frequency + phase) * 0.6 + Math.sin(time * frequency * 1.73 + phase * 2.1) * 0.4);
    }

    private static float smooth(float value) {
        float t = Math.max(0.0F, Math.min(1.0F, value));
        return t * t * (3.0F - 2.0F * t);
    }

    /** 0 -> 1 -> 0 over [0, 1] with eased edges. */
    private static float window01(float p) {
        return smooth(p / 0.2F) * (1.0F - smooth((p - 0.75F) / 0.25F));
    }

    /** 0 -> 1 -> 0 over [0, length] with {@code edge}-tick eased edges. */
    private static float window(float age, float length, float edge) {
        return smooth(age / edge) * (1.0F - smooth((age - (length - edge)) / edge));
    }

    private static double wrapDegrees(double degrees) {
        double wrapped = degrees % 360.0;
        if (wrapped >= 180.0) {
            wrapped -= 360.0;
        } else if (wrapped < -180.0) {
            wrapped += 360.0;
        }
        return wrapped;
    }
}
