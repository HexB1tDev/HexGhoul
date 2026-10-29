package com.hex.kagune.control.client;

import com.hex.kagune.client.KaguneClientState;
import com.hex.kagune.client.KaguneManifestationEffects;
import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.ukaku.UkakuRules;
import com.hex.kagune.control.ukaku.KakujaModel;
import com.hex.kagune.control.ukaku.KakujaMotion;
import com.hex.kagune.control.ukaku.KakujaRig;
import com.hex.kagune.control.ukaku.WingForm;
import com.hex.kagune.control.ukaku.WingRig;
import com.hex.kagune.control.client.animation.KaguneBodyAnimation;
import com.hex.kagune.control.client.fx.KakujaFx;
import com.hex.kagune.control.network.S2CUkakuState;
import com.hex.kagune.gameplay.CentipedeTechnique;
import com.hex.kagune.gameplay.KaguneForm;
import com.hex.kagune.gameplay.TendrilMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client store of every Ukaku player's kagune state (fed only by the server) and the bridge from synced state to the
 * {@link WingRig}. Also the redirect target that keeps the original tendril renderer and writhing sounds away from
 * Ukaku players (their kagune is wings, drawn by {@code UkakuWingRenderer}).
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class UkakuClient {
    /** What the server says about one Ukaku player. */
    public record UkakuView(WingForm form, int vortexSide, long vortexAt, long vortexRegrowAt, boolean crystalMode, long crystalChangedAt,
                            boolean gatling, long gatlingChangedAt, int mutations, boolean blade, long bladeChangedAt) {
        public static final UkakuView DEFAULT = new UkakuView(WingForm.TWIN, 0, Long.MIN_VALUE, Long.MIN_VALUE, false, Long.MIN_VALUE, false,
            Long.MIN_VALUE, 0, false, Long.MIN_VALUE);
    }

    /** Per-player smoothing so crystallization eases in and out instead of snapping. */
    private static final class Smooth {
        float crystalRight;
        float crystalLeft;
        /** The Gatling's shudder: its phase (radians), running with the stream. */
        float barrel;
        double lastTime = Double.NaN;
    }

    private static final Map<UUID, UkakuView> VIEWS = new HashMap<>();
    private static final Map<UUID, Smooth> SMOOTH = new HashMap<>();
    /**
     * Each player's last change of kagune form: {game time, the form before}. The wings pull in and re-form over
     * {@link #RESHAPE_TICKS}; the kakuja transformation (K) grows the tentacles out over {@link #KAKUJA_EMERGE_TICKS}, and
     * turning back regrows the wings over {@link #WINGS_REGROW_TICKS}.
     */
    private static final Map<UUID, long[]> RESHAPED = new HashMap<>();
    private static final double RESHAPE_TICKS = 8.0;
    public static final double KAKUJA_EMERGE_TICKS = 40.0;
    static final double WINGS_REGROW_TICKS = 14.0;
    /** Each kakuja's spring chains (the life in its tentacles), per player. */
    private static final Map<UUID, KakujaMotion> KAKUJA_MOTION = new HashMap<>();
    private static KaguneClientState.View inactive;

    private UkakuClient() {
    }

    public static void accept(S2CUkakuState message) {
        UkakuView previous = VIEWS.put(message.playerId(), new UkakuView(message.form(), message.vortexSide(), message.vortexAt(),
            message.vortexRegrowAt(), message.crystalMode(), message.crystalChangedAt(), message.gatling(), message.gatlingChangedAt(), message.mutations(),
            message.blade(), message.bladeChangedAt()));
        ClientLevel level = Minecraft.m_91087_().f_91073_;
        if (previous != null && previous.form() != message.form() && level != null) {
            RESHAPED.put(message.playerId(), new long[] {level.m_46467_(), previous.form().ordinal()});
        }
    }

    public static UkakuView get(Player player) {
        return player == null ? UkakuView.DEFAULT : VIEWS.getOrDefault(player.m_36316_().getId(), UkakuView.DEFAULT);
    }

    public static boolean isUkaku(Player player) {
        return player instanceof AbstractClientPlayer client && PathClient.get(client) == KagunePath.UKAKU;
    }

    /**
     * Trampoline target for {@code KaguneManifestationEffects.spawnOriginMist}: the red mist (vanilla poof clouds when
     * Photon is missing) the original puffs at the kagune's origin every few ticks while it is out. Ukaku's wings never had it
     * (they erupt in their own fire), and since 3.1.0 Rinkaku's back is clean too: nothing is drawn. The blood as each limb
     * tears out ({@link #spawnManifestBlood}) stays.
     */
    public static void spawnOriginMist(AbstractClientPlayer player) {
    }

    /**
     * Trampoline target for {@code KaguneManifestationEffects.spawnBlood}, called once per limb as it comes out: Rinkaku's
     * tendrils tear out in blood, Ukaku's wings don't. Each limb also plays its path's manifestation sound.
     */
    public static void spawnManifestBlood(AbstractClientPlayer player, boolean opening) {
        if (PaleClient.isPale(player)) {
            // 6.1.0: nothing tears out of Pale: her one eye turns, the steam rises.
            if (opening) {
                ManifestSounds.pale(player);
            }
            return;
        }
        boolean ukaku = isUkaku(player);
        ManifestSounds.limb(player, opening, ukaku);
        if (!ukaku) {
            KaguneManifestationEffects.spawnBloodLegacy(player, opening);
        }
    }

    /**
     * Redirect target for the original {@code KaguneClientState.get} calls in the tendril renderer and the ambient
     * writhing sounds (bytecode hook): Ukaku players read as having no tendrils out.
     */
    public static KaguneClientState.View tendrilView(Player player) {
        // Rinkaku's kakuja bursting out and breaking is played from here: the original asks for every player every frame.
        KakujaFx.observe(player);
        KaguneClientState.View view = KaguneClientState.get(player);
        // 6.1.0: Pale's base form keeps her human shape: no tendrils, no writhing (her flesh is drawn by PaleFleshRenderer).
        // 7.0.0: Graft DNA's limbs are its own (GraftRenderer): the original's tendrils and their writhing are not drawn.
        return view.active() && (isUkaku(player) || PaleClient.isPale(player) || GraftClient.isGraft(player)) ? inactiveView() : view;
    }

    /** The original's own "nothing manifested" state (same values as {@code KaguneClientState}'s empty view). */
    private static KaguneClientState.View inactiveView() {
        if (inactive == null) {
            inactive = new KaguneClientState.View(false, false, 0, 100.0F, 10817335, 1573643, KaguneForm.RINKAKU_SEED, 1, 0L, 1.0F, 1.0F,
                1, 1, 0, TendrilMode.IDLE, 0L, 0, 0, 0, 1, 1, Vec3.f_82478_, 0L, 0, -1, 0, Vec3.f_82478_, CentipedeTechnique.STRIKE, 0L, 0,
                -1, Vec3.f_82478_, new int[6], 8.0F, 0.18F, 0.46F, 6.0F, 10.0F, 8.5F, 5.0F, false, false, 0, 0, 0);
        }
        return inactive;
    }

    /** 0..1 deployment of the kagune from the original manifest/dismiss timing. */
    static float deploy(KaguneClientState.View view, double time) {
        if (!view.active()) {
            return 0.0F;
        }
        float progress = view.modeDuration() <= 0 ? 1.0F
            : (float)Math.max(0.0, Math.min(1.0, (time - view.modeStarted()) / view.modeDuration()));
        if (view.mode() == TendrilMode.MANIFEST) {
            return progress;
        }
        return view.mode() == TendrilMode.DISMISS ? 1.0F - progress : 1.0F;
    }

    /** Everything the rig needs for this frame. {@code time} is the interpolated world time in ticks. */
    public static WingRig.Input rigInput(AbstractClientPlayer player, KaguneClientState.View view, ControlView control, double time,
                                         boolean firstPerson, float detail) {
        UkakuView ukaku = get(player);
        int mastery = view.mastery();
        WingForm form = ukaku.form().shown(mastery);
        UkakuMotions.Strike strike = UkakuMotions.strike(view, control, time);

        // The crystal mode keeps both wings crystal; a strike crystallizes the wing it uses.
        boolean mode = ukaku.crystalMode();
        float targetRight = mode || strike.crystalRight() ? 1.0F : 0.0F;
        float targetLeft = mode || strike.crystalLeft() ? 1.0F : 0.0F;

        Smooth smooth = SMOOTH.computeIfAbsent(player.m_36316_().getId(), ignored -> new Smooth());
        double elapsed = Double.isNaN(smooth.lastTime) ? 100.0 : Math.max(0.0, Math.min(100.0, time - smooth.lastTime));
        smooth.lastTime = time;
        double sinceToggle = ukaku.crystalChangedAt() == Long.MIN_VALUE ? Double.MAX_VALUE : time - ukaku.crystalChangedAt();
        if (mode || sinceToggle < UkakuRules.THAW_TICKS + 4.0) {
            // The V toggle: the crystal climbs the wing at a steady pace from the shoulder blade to the very tip (the wing
            // model draws crystal up to this front, fire beyond it), and thaws back down the same way.
            float step = (float)(elapsed / (mode ? UkakuRules.CRYSTALLIZE_TICKS : UkakuRules.THAW_TICKS));
            smooth.crystalRight = toward(smooth.crystalRight, targetRight, step);
            smooth.crystalLeft = toward(smooth.crystalLeft, targetLeft, step);
        } else {
            float rate = (float)(1.0 - Math.exp(-elapsed * 0.38));
            smooth.crystalRight += (targetRight - smooth.crystalRight) * rate;
            smooth.crystalLeft += (targetLeft - smooth.crystalLeft) * rate;
        }
        // The Gatling: both wings curled forward, shuddering about their centre lines with the stream (a full barrel spin would
        // squash the curled wing as it turned; a quick rock keeps its size).
        float gun = UkakuRules.gatlingAim(time, ukaku.gatling(), ukaku.gatlingChangedAt());
        smooth.barrel = (float)((smooth.barrel + elapsed * GATLING_SHUDDER_RATE * gun) % (Math.PI * 2.0));

        // In first person the kagune is drawn faint so the player can still see.
        float alpha = firstPerson ? (form.kakuja() ? 0.3F : 0.45F) : 1.0F;
        float deploy = deploy(view, time) * transformation(player, form, time);
        // The wings aim where the player looks, in the rig's own space (x right, y up, z forward along the body).
        double[] aim = aimDirection(player, time - Math.floor(time), strike.aimYaw());
        // The Wing Vortex: the thrown wing twirls up, is gone while it flies, and a new one grows back in its place.
        float[] thrown = UkakuRules.vortexWing(time, ukaku.vortexAt(), ukaku.vortexRegrowAt());
        boolean rightThrown = ukaku.vortexSide() > 0;
        boolean leftThrown = ukaku.vortexSide() < 0;
        float[] gunKick = gatlingKick(ukaku, time, gun);
        return new WingRig.Input(form, mastery, deploy, time / 20.0, smooth.crystalRight, smooth.crystalLeft, strike.motion(),
            strike.progress(), strike.impact(), 0.0F, 0.0F, player.m_6047_(), detail, alpha,
            view.mode() == TendrilMode.DISMISS, Math.max(strike.aimRight(), gun), Math.max(strike.aimLeft(), gun), aim,
            Math.max(strike.kickRight(), gunKick[0]), Math.max(strike.kickLeft(), gunKick[1]),
            rightThrown ? thrown[0] : 0.0F, leftThrown ? thrown[0] : 0.0F, rightThrown ? thrown[1] : 1.0F, leftThrown ? thrown[1] : 1.0F,
            gun > 0.0F ? GATLING_SHUDDER * gun * (float)Math.sin(smooth.barrel) : 0.0F);
    }

    /**
     * How far the kagune is through its last change of form (multiplies the deploy): a kakuja's tentacles grow out from nothing
     * (they come out in turn, the upper pair first), the wings regrow when he turns back, and any other change (the wings
     * reshaping, one kakuja turning into the other) pulls the kagune in a little and re-forms it.
     */
    static float transformation(AbstractClientPlayer player, WingForm form, double time) {
        long[] reshaped = RESHAPED.get(player.m_36316_().getId());
        if (reshaped == null) {
            return 1.0F;
        }
        double since = time - reshaped[0];
        boolean fromKakuja = WingForm.byOrdinal((int)reshaped[1]).kakuja();
        boolean emerging = form.kakuja() && !fromKakuja;
        boolean regrowing = fromKakuja && !form.kakuja();
        double span = emerging ? KAKUJA_EMERGE_TICKS : regrowing ? WINGS_REGROW_TICKS : RESHAPE_TICKS;
        if (since < 0.0 || since >= span) {
            return 1.0F;
        }
        float t = (float)(since / span);
        float eased = t * t * (3.0F - 2.0F * t);
        return emerging || regrowing ? eased : 0.55F + 0.45F * eased;
    }

    /** The Gatling's shudder: how far the wings rock about their centre lines (radians), and how fast (radians a tick). */
    static final float GATLING_SHUDDER = 0.28F;
    static final float GATLING_SHUDDER_RATE = 1.6F;

    /** The Gatling's shots leaving the wings, the wings taking turns at the fire rate: {right kick, left kick}. */
    static float[] gatlingKick(UkakuView ukaku, double time, float gun) {
        if (!ukaku.gatling() || gun <= 0.0F || ukaku.gatlingChangedAt() == Long.MIN_VALUE) {
            return new float[] {0.0F, 0.0F};
        }
        double held = time - ukaku.gatlingChangedAt();
        double rate = UkakuRules.gatlingRate((long)held);
        if (rate <= 0.0) {
            return new float[] {0.0F, 0.0F};
        }
        double shots = (held - UkakuRules.GATLING_SPINUP) * rate;
        double since = (shots - Math.floor(shots)) / rate;
        float pulse = gun * (float)Math.exp(-since * 0.55);
        boolean right = ((long)Math.floor(shots) & 1L) == 0L;
        return new float[] {right ? pulse : 0.0F, right ? 0.0F : pulse};
    }

    /** How far this player's Gatling is up (0..1), for the body. */
    public static float gatlingAmount(AbstractClientPlayer player, double time) {
        if (!isUkaku(player)) {
            return 0.0F;
        }
        UkakuView ukaku = get(player);
        return UkakuRules.gatlingAim(time, ukaku.gatling(), ukaku.gatlingChangedAt());
    }

    /** The body's backflip right now (radians, 0 when not flipping): the wings turn with the body. */
    public static float flip(AbstractClientPlayer player, KaguneClientState.View view, double time) {
        return UkakuMotions.strike(view, ControlClient.get(player), time).flip();
    }

    /** Where the player looks, in rig space (x right, y up, z forward along the body), turned {@code yawOffset} degrees. */
    static double[] aimDirection(AbstractClientPlayer player, double partial, float yawOffset) {
        float bodyYaw = player.f_20884_ + wrap(player.f_20883_ - player.f_20884_) * (float)partial;
        float headYaw = player.f_20886_ + wrap(player.f_20885_ - player.f_20886_) * (float)partial;
        double relative = Math.toRadians(wrap(headYaw - bodyYaw) + yawOffset);
        double pitch = Math.toRadians(player.m_146909_());
        // Minecraft yaw grows turning right (south to west), and rig x is the player's right.
        return new double[] {Math.sin(relative) * Math.cos(pitch), -Math.sin(pitch), Math.cos(relative) * Math.cos(pitch)};
    }

    /** {@code value} moved toward {@code target} by at most {@code step}. */
    static float toward(float value, float target, float step) {
        return value < target ? Math.min(target, value + step) : Math.max(target, value - step);
    }

    /** True while this Ukaku player is in the crystal mode (as far as this client knows). */
    public static boolean crystalMode(Player player) {
        return isUkaku(player) && get(player).crystalMode();
    }

    private static float wrap(float degrees) {
        float wrapped = degrees % 360.0F;
        if (wrapped >= 180.0F) {
            wrapped -= 360.0F;
        } else if (wrapped < -180.0F) {
            wrapped += 360.0F;
        }
        return wrapped;
    }

    /** Ticks the body's throw lasts from the start of the wind-up (the coil, the throw, the follow-through). */
    public static final int VORTEX_POSE_TICKS = UkakuRules.VORTEX_WINDUP + 12;

    /**
     * The body throwing a Wing Vortex at {@code time}: {progress 0..1 through {@link #VORTEX_POSE_TICKS}, the release at
     * {@link #vortexRelease}, side +1 right or -1 left}, or null when no throw is under way.
     */
    public static float[] vortexPose(AbstractClientPlayer player, double time) {
        if (!isUkaku(player)) {
            return null;
        }
        UkakuView ukaku = get(player);
        if (ukaku.vortexAt() == Long.MIN_VALUE || ukaku.vortexSide() == 0) {
            return null;
        }
        double age = time - (ukaku.vortexAt() - UkakuRules.VORTEX_WINDUP);
        if (age < 0.0 || age > VORTEX_POSE_TICKS) {
            return null;
        }
        return new float[] {(float)(age / VORTEX_POSE_TICKS), ukaku.vortexSide()};
    }

    /**
     * The shoulder blade wing {@code side} (+1 right, -1 left) grows from, in the world, for this player as drawn this frame
     * ({@code partial} the frame's partial tick): where a Wing Vortex stays attached.
     */
    public static Vec3 shoulder(AbstractClientPlayer player, int side, double partial) {
        float delta = ((player.f_20883_ - player.f_20884_) % 360.0F + 540.0F) % 360.0F - 180.0F;
        double yaw = Math.toRadians(player.f_20884_ + delta * partial);
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double rightX = -Math.cos(yaw);
        double rightZ = -Math.sin(yaw);
        double x = WingRig.ROOT_X * side;
        double z = WingRig.ROOT_Z;
        double y = WingRig.ROOT_Y - (player.m_6047_() ? WingRig.CROUCH_DROP : 0.0);
        double px = player.f_19790_ + (player.m_20185_() - player.f_19790_) * partial;
        double py = player.f_19791_ + (player.m_20186_() - player.f_19791_) * partial;
        double pz = player.f_19792_ + (player.m_20189_() - player.f_19792_) * partial;
        return new Vec3(px + rightX * x + forwardX * z, py + y, pz + rightZ * x + forwardZ * z);
    }

    /** The wing a player's last Wing Vortex is (+1 right, -1 left, 0 none). */
    public static int vortexSide(Player player) {
        return isUkaku(player) ? get(player).vortexSide() : 0;
    }

    /** Where in the throw pose the wing leaves (0..1). */
    public static float vortexRelease() {
        return UkakuRules.VORTEX_WINDUP / (float)VORTEX_POSE_TICKS;
    }

    /** 5.0.18: how closed into the Steel Blade this player's steel wings are at {@code time} (0 open .. 1 the blade). */
    public static double blade(Player player, double time) {
        UkakuView view = get(player);
        if (!isUkaku(player) || !view.blade() && view.bladeChangedAt() == Long.MIN_VALUE) {
            return 0.0;
        }
        return view.bladeChangedAt() == Long.MIN_VALUE ? 1.0 : com.hex.kagune.control.ukaku.SteelBlade.formed(view.blade(), time - view.bladeChangedAt());
    }

    /** True while this player's kagune is a kakuja, the first or the final (as far as this client knows). */
    public static boolean kakuja(AbstractClientPlayer player) {
        return isUkaku(player) && get(player).form().shown(KaguneClientState.get(player).mastery()).kakuja();
    }

    /** True while this player's kagune is the final kakuja (as far as this client knows). */
    public static boolean finalKakuja(AbstractClientPlayer player) {
        return isUkaku(player) && get(player).form().shown(KaguneClientState.get(player).mastery()).finalKakuja();
    }

    /** How far the kakuja's armor is grown over this player's body (0..1): it comes and goes with the tentacles. */
    public static float kakujaArmor(AbstractClientPlayer player, float partial) {
        if (!kakuja(player)) {
            return 0.0F;
        }
        KaguneClientState.View view = KaguneClientState.get(player);
        if (!view.active() || !player.m_6084_()) {
            return 0.0F;
        }
        double time = ControlClient.worldTime(partial);
        WingForm form = get(player).form().shown(view.mastery());
        return Math.min(1.0F, deploy(view, time) * transformation(player, form, time) * 1.4F);
    }

    /**
     * What the body is doing, for the kakuja's tentacles: how fast he runs, his stride, rising or falling, and the upper body's
     * lean, sideways bend, chest turn and drop as the body layer posed it (so they ride the torso through runs, jumps, landings
     * and strikes).
     */
    static KakujaRig.Body kakujaBody(AbstractClientPlayer player, float partial) {
        double dx = player.m_20185_() - player.f_19790_;
        double dz = player.m_20189_() - player.f_19792_;
        float run = (float)Math.max(0.0, Math.min(1.0, Math.sqrt(dx * dx + dz * dz) / 0.32));
        float stride = player.f_267362_.m_267590_(partial) * 0.6662F;
        float vertical = (float)(player.m_20186_() - player.f_19791_);
        float[] torso = KaguneBodyAnimation.torso(player, ControlClient.worldTime(partial));
        // The body layer's drop is in model pixels; the rig's in blocks (the model is drawn at 15/16).
        float drop = torso[3] / 16.0F * 0.9375F;
        return new KakujaRig.Body(torso[0], torso[1], torso[2], drop, run, stride, vertical, !player.m_20096_());
    }

    /**
     * The kakuja's quads for this frame, alive: the designed pose, then this player's spring chains run in the world (the
     * body's position {@code px, py, pz} and yaw in radians, as the renderer places it) and their lag handed back.
     */
    public static List<WingRig.Quad> kakujaQuads(AbstractClientPlayer player, WingRig.Input in, double[] eye, float partial, double time,
                                                 double px, double py, double pz, double yaw) {
        KakujaRig.Body body = kakujaBody(player, partial);
        KaguneClientState.View view = KaguneClientState.get(player);
        KakujaRig.Action action = KakujaMotions.action(view, ControlClient.get(player), time, aimDirection(player, time - Math.floor(time), 0.0F),
            get(player));
        KakujaRig.Limb[] limbs = KakujaRig.pose(in, body, action);
        double fold = com.hex.kagune.control.ukaku.SteelBlade.fold(blade(player, time));
        if (fold > 0.0) {
            // The Steel Blade: the steel tentacles draw back into his back as they close round his right arm (SteelBladeLayer).
            for (int i = 0; i < limbs.length; i++) {
                if (limbs[i] != null && limbs[i].side() > 0) {
                    limbs[i] = KakujaRig.folded(limbs[i], fold);
                }
            }
        }
        KakujaModel.Spine[] spines = KakujaRig.spines(in, body, limbs);
        double[][][] nodes = KakujaRig.nodes(spines);
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double rightX = -Math.cos(yaw);
        double rightZ = -Math.sin(yaw);
        double[][][] world = new double[nodes.length][][];
        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i] == null) {
                continue;
            }
            world[i] = new double[nodes[i].length][];
            for (int k = 0; k < nodes[i].length; k++) {
                double[] r = nodes[i][k];
                world[i][k] = new double[] {px + rightX * r[0] + forwardX * r[2], py + r[1], pz + rightZ * r[0] + forwardZ * r[2]};
            }
        }
        KakujaMotion motion = KAKUJA_MOTION.computeIfAbsent(player.m_36316_().getId(), ignored -> new KakujaMotion());
        double[][][] lag = motion.step(world, time);
        double[][][] offsets = new double[lag.length][][];
        for (int i = 0; i < lag.length; i++) {
            if (lag[i] == null) {
                continue;
            }
            offsets[i] = new double[lag[i].length][];
            for (int k = 0; k < lag[i].length; k++) {
                double[] w = lag[i][k];
                offsets[i][k] = new double[] {w[0] * rightX + w[2] * rightZ, w[1], w[0] * forwardX + w[2] * forwardZ};
            }
        }
        List<WingRig.Quad> out = new ArrayList<>();
        KakujaRig.draw(out, in, spines, limbs, offsets, !body.airborne(), eye, in.form().finalKakuja());
        return out;
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        VIEWS.clear();
        SMOOTH.clear();
        RESHAPED.clear();
        KAKUJA_MOTION.clear();
    }
}
