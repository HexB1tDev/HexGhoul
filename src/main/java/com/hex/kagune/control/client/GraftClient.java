package com.hex.kagune.control.client;

import com.hex.kagune.client.KaguneClientState;
import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.graft.GraftMove;
import com.hex.kagune.control.graft.GraftRules;
import com.hex.kagune.control.graft.GraftStage;
import com.hex.kagune.control.network.S2CGraftState;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 7.0.0: the client's store of every Graft DNA player's kagune (fed only by the server, {@code S2CGraftState}): its stage, the
 * kakuja and when it began or ended, the move and when it began, its point and the bodies its limbs go after. The renderer, the hide
 * and the head all read it and animate from when each began, on the client's own clock ({@link PaleClient#clock}: never set back, so
 * a lagging server never throws a move back a few ticks).
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class GraftClient {
    /** A move or a transformation this few ticks old when it gets here is played from its first frame (it was sent as it began). */
    static final long JUST_BEGUN = 4L;

    /** One player's kagune as the server last said, with when (on the client clock) its move and its kakuja began here. */
    public record View(GraftStage stage, boolean kakuja, long kakujaAt, double kakujaBegan, GraftMove move, long startedAt, double began, float yaw,
                       double x, double y, double z, int[] targets, int heldId, int mutations, GraftMove job, long jobAt,
                       double jobBegan, int jobHeld) {
        public static final View NONE = new View(GraftStage.TRANSPLANT, false, Long.MIN_VALUE, -1.0E6, GraftMove.NONE, Long.MIN_VALUE, 0.0, 0.0F,
            0.0, 0.0, 0.0, new int[S2CGraftState.TARGETS], 0, 0, GraftMove.NONE, Long.MIN_VALUE, 0.0, 0);

        /** A view with no job running (7.0.2: the ghoul NPCs' lashes). */
        public View(GraftStage stage, boolean kakuja, long kakujaAt, double kakujaBegan, GraftMove move, long startedAt, double began, float yaw,
                    double x, double y, double z, int[] targets, int heldId, int mutations) {
            this(stage, kakuja, kakujaAt, kakujaBegan, move, startedAt, began, yaw, x, y, z, targets, heldId, mutations, GraftMove.NONE,
                Long.MIN_VALUE, 0.0, 0);
        }

        /** 7.0.2: ticks since the job began ({@code clock}: {@link PaleClient#clock}); MAX_VALUE for none. */
        public double jobAge(double clock) {
            return this.job == GraftMove.NONE || this.jobAt == Long.MIN_VALUE ? Double.MAX_VALUE : Math.max(0.0, clock - this.jobBegan);
        }

        /** Ticks since the move began ({@code clock}: {@link PaleClient#clock}). */
        public double age(double clock) {
            return this.move == GraftMove.NONE || this.startedAt == Long.MIN_VALUE ? Double.MAX_VALUE : Math.max(0.0, clock - this.began);
        }

        /** Ticks since the kakuja last went on or off. */
        public double kakujaAge(double clock) {
            return Math.max(0.0, clock - this.kakujaBegan);
        }
    }

    private static final Map<UUID, View> VIEWS = new HashMap<>();

    private GraftClient() {
    }

    public static void accept(S2CGraftState message) {
        Minecraft minecraft = Minecraft.m_91087_();
        long world = minecraft.f_91073_ == null ? 0L : minecraft.f_91073_.m_46467_();
        double clock = PaleClient.clock(0.0F);
        View before = VIEWS.get(message.playerId());
        long behind = message.startedAt() == Long.MIN_VALUE ? 0L : world - message.startedAt();
        double began = clock - (behind > JUST_BEGUN ? behind : 0L);
        if (before != null && before.startedAt() == message.startedAt() && before.move().ordinal() == message.move()) {
            began = before.began(); // the same move, told again (it caught something, it threw it): its clock runs on
        }
        long jobBehind = message.jobAt() == Long.MIN_VALUE ? 0L : world - message.jobAt();
        double jobBegan = clock - (jobBehind > JUST_BEGUN ? jobBehind : 0L);
        if (before != null && before.jobAt() == message.jobAt() && before.job().ordinal() == message.job()) {
            jobBegan = before.jobBegan();
        }
        long kakujaBehind = world - message.kakujaAt();
        double kakujaBegan = clock - (kakujaBehind > JUST_BEGUN ? Math.min(kakujaBehind, 10_000L) : 0L);
        if (before != null && before.kakujaAt() == message.kakujaAt()) {
            kakujaBegan = before.kakujaBegan();
        }
        VIEWS.put(message.playerId(), new View(GraftStage.byOrdinal(message.stage()), message.kakuja(), message.kakujaAt(), kakujaBegan,
            GraftMove.byOrdinal(message.move()), message.startedAt(), began, message.yaw(), message.x(), message.y(), message.z(), message.targets(),
            message.heldId(), message.mutations(), GraftMove.byOrdinal(message.job()), message.jobAt(), jobBegan, message.jobHeld()));
    }

    public static View get(Player player) {
        return player == null ? View.NONE : VIEWS.getOrDefault(player.m_36316_().getId(), View.NONE);
    }

    public static boolean isGraft(Player player) {
        return player instanceof AbstractClientPlayer client && PathClient.get(client) == KagunePath.GRAFT;
    }

    /** 0..1: how far the kagune is out (the original's manifest and dismiss timing; 0 put away). */
    public static float deploy(Player player, float partial) {
        return UkakuClient.deploy(KaguneClientState.get(player), ControlClient.worldTime(partial));
    }

    /** How far the base stage's limbs are out through a kakuja transformation (1 outside one). */
    public static double baseShown(View view, double clock) {
        return GraftRules.baseRetract(view.kakuja(), view.kakujaAge(clock));
    }

    /** How far the hide has spread over his body (0 none .. 1 whole). */
    public static double hide(View view, double clock) {
        return GraftRules.hide(view.kakuja(), view.kakujaAge(clock));
    }

    /** How far the colossi are out (0 inside him .. 1 unfolded). */
    public static double colossi(View view, double clock) {
        return GraftRules.colossi(view.kakuja(), view.kakujaAge(clock));
    }

    /** True while either the kakuja or its unmaking still shows anything of it. */
    public static boolean kakujaShows(View view, double clock) {
        return hide(view, clock) > 0.002 || colossi(view, clock) > 0.002;
    }

    /**
     * How wide the kakuja's mouth is: shut at rest (a grin), thrown open through a move's wind-up and blows, and wide through the
     * transformation's last stretch.
     */
    public static double jaw(View view, double clock) {
        double age = view.age(clock);
        double open = 0.0;
        if (view.move() != GraftMove.NONE && age < view.move().ticks()) {
            double into = age / Math.max(1.0, view.move().ticks());
            open = Math.sin(Math.PI * Math.min(1.0, into * 1.4)) * (view.move().slot() == GraftMove.Slot.M1 ? 0.75 : 1.0);
            // His own jaws' bites: thrown wide through the lunge, snapped shut on the bite, slack after.
            for (GraftMove.Beat beat : view.move().beats(2)) {
                if (beat.limb() == GraftMove.ARM && beat.kind() == GraftMove.Kind.BITE && age >= beat.start() && age <= beat.end() + 2) {
                    open = age < beat.land() ? Math.min(1.0, (age - beat.start() + 1.0) / 2.0) : 0.08 + 0.25 * Math.min(1.0, (age - beat.land()) / 4.0);
                }
            }
        }
        if (view.kakuja()) {
            double k = view.kakujaAge(clock);
            open = Math.max(open, k > 30.0 && k < 56.0 ? Math.sin(Math.PI * (k - 30.0) / 26.0) : 0.0);
        }
        return Math.max(0.0, Math.min(1.0, open));
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        VIEWS.clear();
    }
}
