package com.hex.kagune.control.client;

import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.pale.PaleMove;
import com.hex.kagune.control.pale.PaleRules;
import com.hex.kagune.control.network.S2CPaleState;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 6.1.0: the client's store of every Pale player's move (fed only by the server, {@code S2CPaleState}). Her body animation, the
 * pale flesh and her effects all read the move from here and animate it from the tick it began, so every client is in step.
 *
 * <p>They are timed on a clock of the client's own ({@link #clock}): ticks counted here, never set back. The world's time is set
 * to the server's every second, and on a server running behind (below 20 ticks a second, as any busy server now and then) each
 * of those resets would throw every move back a few ticks: a twitch through the body, now and then, worse the more the server
 * lags.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class PaleClient {
    /** A move this few ticks old when it gets here is played from its first frame (it was sent as it began). */
    static final long JUST_BEGUN = 4L;

    /**
     * One Pale player's move as the server last said, and when (on {@link #clock}) it began here.
     */
    public record PaleView(PaleMove move, int detail, long startedAt, int targetId, float charge, float yaw, double x, double y, double z,
                          double began) {
        public static final PaleView NONE = new PaleView(PaleMove.NONE, 0, Long.MIN_VALUE, 0, 0.0F, 0.0F, 0.0, 0.0, 0.0, 0.0);

        /** Ticks since the move began, at {@code clock} ({@link PaleClient#clock}), 0 before it. */
        public double age(double clock) {
            return this.startedAt == Long.MIN_VALUE ? Double.MAX_VALUE : Math.max(0.0, clock - this.began);
        }
    }

    private static final Map<UUID, PaleView> VIEWS = new HashMap<>();
    /** Client ticks counted while a world is open and not paused. */
    private static long ticks;

    private PaleClient() {
    }

    /** The client's own clock (ticks), {@code partial} of the way through the current tick: it only ever goes forward. */
    public static double clock(float partial) {
        return ticks + (double)partial;
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        Minecraft minecraft = Minecraft.m_91087_();
        if (event.phase == TickEvent.Phase.START) {
            if (minecraft.f_91073_ != null && !minecraft.m_91104_() && minecraft.f_91074_ != null) {
                drive(minecraft.f_91074_);
            }
            return;
        }
        if (minecraft.f_91073_ != null && !minecraft.m_91104_()) {
            ticks++;
        }
    }

    /** 6.2.0: where the Speed Kick began for the player at the keys (her feet then), and which kick that was. */
    private static Vec3 kickFrom;
    private static long kickStarted = Long.MIN_VALUE;

    /**
     * 6.2.0: the player at the keys in her Speed Kick is moved by it, not by her keys: each tick exactly onto where the kick has her
     * at the end of it ({@link PaleRules#kickPlace}: 6.2.0, the dash in unseen, risen to the blow, the rebound's hop, and down where
     * she kicked from). Moved here, by her own client, she goes there smoothly and the server hears it as any movement of hers.
     */
    static void drive(LocalPlayer self) {
        PaleView view = get(self);
        if (frozenIn(self) != null) {
            // 6.2.0: frozen in someone's Witness This: carried along its path (only the view is theirs).
            witnessed(self, frozenIn(self));
            kickFrom = null;
            return;
        }
        if (view.move() == PaleMove.WITNESS) {
            double[] next = com.hex.kagune.control.pale.WitnessRules.her(view.age(clock(0.0F)) + 1.0, view.charge());
            if (next != null) {
                carry(self, view, next);
            }
            kickFrom = null;
            return;
        }
        if (view.move() != PaleMove.KICK && view.move() != PaleMove.OWL_KICK) {
            kickFrom = null;
            return;
        }
        if (kickFrom == null || kickStarted != view.startedAt()) {
            kickFrom = self.m_20182_();
            kickStarted = view.startedAt();
        }
        double[] to = {view.x() - kickFrom.f_82479_, view.y() - kickFrom.f_82480_, view.z() - kickFrom.f_82481_};
        // 6.2.0: the Owl's kick dashes all but unseen and plants her there through all of it (OwlRules.kickPlace).
        double[] next = view.move() == PaleMove.OWL_KICK
            ? com.hex.kagune.control.pale.OwlRules.kickPlace(to, view.detail(), Math.toRadians(view.yaw()), view.age(clock(0.0F)) + 1.0)
            : PaleRules.kickPlace(to, view.detail(), view.charge(), view.age(clock(0.0F)) + 1.0);
        if (next == null) {
            return;
        }
        Vec3 here = self.m_20182_();
        self.m_20256_(new Vec3(kickFrom.f_82479_ + next[0] - here.f_82479_, kickFrom.f_82480_ + next[1] - here.f_82480_,
            kickFrom.f_82481_ + next[2] - here.f_82481_));
    }

    /**
     * 6.2.0, Witness This: the Owl DNA player whose combo {@code self} is frozen in (their view of it), or null. Every client knows
     * every Pale player's move, so a frozen player's own client finds it here.
     */
    static PaleView frozenIn(LocalPlayer self) {
        for (PaleView view : VIEWS.values()) {
            if (view.move() == PaleMove.WITNESS && view.targetId() == self.m_19879_()
                && com.hex.kagune.control.pale.WitnessRules.frozen(view.age(clock(0.0F)))) {
                return view;
            }
        }
        return null;
    }

    /**
     * 6.2.2: the Witness This combo the body {@code entityId} is caught in this frame, as {age (ticks), the move's yaw (degrees)};
     * null when none (every client draws how it is thrown about from this).
     */
    public static double[] witnessOn(int entityId, float partial) {
        for (PaleView view : VIEWS.values()) {
            if (view.move() == PaleMove.WITNESS && view.targetId() == entityId) {
                double age = view.age(clock(partial));
                if (com.hex.kagune.control.pale.WitnessRules.frozen(age)) {
                    return new double[] {age, view.yaw()};
                }
            }
        }
        return null;
    }

    /** The player at the keys, frozen in {@code view}'s combo: moved each tick onto where it has them next (WitnessRules.target). */
    private static void witnessed(LocalPlayer self, PaleView view) {
        carry(self, view, com.hex.kagune.control.pale.WitnessRules.target(view.age(clock(0.0F)) + 1.0, view.charge()));
        self.f_19789_ = 0.0F;
    }

    /** Moves {@code self} this tick onto {@code at}: a point in {@code view}'s frame (x her right, y up, z the way the punch went, from its point). */
    private static void carry(LocalPlayer self, PaleView view, double[] at) {
        double yaw = Math.toRadians(view.yaw());
        double fx = -Math.sin(yaw);
        double fz = Math.cos(yaw);
        double tx = view.x() + -fz * at[0] + fx * at[2];
        double ty = view.y() + at[1];
        double tz = view.z() + fx * at[0] + fz * at[2];
        Vec3 here = self.m_20182_();
        self.m_20256_(new Vec3(tx - here.f_82479_, ty - here.f_82480_, tz - here.f_82481_));
    }

    /** ...and her keys don't move her meanwhile. */
    @SubscribeEvent
    public static void movementInput(MovementInputUpdateEvent event) {
        if (event.getEntity() instanceof LocalPlayer self && kicking(self)) {
            Input input = event.getInput();
            input.f_108567_ = 0.0F;
            input.f_108566_ = 0.0F;
        }
    }

    /** Whether the player at the keys is still being carried by her Speed Kick (until she is down on her feet again). */
    static boolean kicking(LocalPlayer self) {
        if (self == null) {
            return false;
        }
        PaleView view = get(self);
        if (view.move() == PaleMove.OWL_KICK) {
            return view.age(clock(0.0F)) < view.detail() + com.hex.kagune.control.pale.OwlRules.KICK_TICKS;
        }
        if (view.move() == PaleMove.WITNESS) {
            return view.age(clock(0.0F)) < com.hex.kagune.control.pale.WitnessRules.FREE;
        }
        if (frozenIn(self) != null) {
            return true; // 6.2.0: frozen in someone's Witness This
        }
        return view.move() == PaleMove.KICK && view.age(clock(0.0F)) < PaleRules.kickArrive(view.detail()) + PaleRules.KICK_LAND;
    }

    /**
     * 6.2.0: whether {@code player} is gone in the Speed Kick's dash this frame ({@code partial} of the way through the tick): nothing
     * of her is drawn (her body, what she holds and wears, her name; her own hands in first person). The server makes her invisible
     * for the dash too, so not even her shadow shows.
     */
    public static boolean kickGone(Player player, float partial) {
        PaleView view = get(player);
        return view.move() == PaleMove.KICK && PaleRules.kickGone(view.detail(), view.age(clock(partial)));
    }

    @SubscribeEvent
    public static void hideBody(net.minecraftforge.client.event.RenderPlayerEvent.Pre event) {
        if (kickGone(event.getEntity(), event.getPartialTick())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void hideHands(net.minecraftforge.client.event.RenderHandEvent event) {
        Minecraft minecraft = Minecraft.m_91087_();
        if (minecraft.f_91074_ != null && kickGone(minecraft.f_91074_, minecraft.m_91296_())) {
            event.setCanceled(true);
        }
    }

    public static void accept(S2CPaleState message) {
        // Sent as it began (the usual case): played here from its first frame, from now on. (6.2.0: from now, not the coming tick:
        // what the server does on a move's tick arrives as late as the move itself did, so a blow is seen landing as it lands,
        // never after it.) Begun a while before (she came into sight mid-move): that far in.
        Minecraft minecraft = Minecraft.m_91087_();
        long behind = minecraft.f_91073_ == null || message.startedAt() == Long.MIN_VALUE ? 0L : minecraft.f_91073_.m_46467_() - message.startedAt();
        double began = ticks - (behind > JUST_BEGUN ? behind : 0L);
        PaleView before = VIEWS.get(message.playerId());
        if (before != null && before.startedAt() == message.startedAt() && before.move().ordinal() == message.move() && before.detail() == message.detail()) {
            began = before.began(); // the same move, told again (its facing or target changed): its clock runs on
        }
        VIEWS.put(message.playerId(), new PaleView(PaleMove.byOrdinal(message.move()), message.detail(), message.startedAt(), message.targetId(),
            message.charge(), message.yaw(), message.x(), message.y(), message.z(), began));
    }

    public static PaleView get(Player player) {
        return player == null ? PaleView.NONE : VIEWS.getOrDefault(player.m_36316_().getId(), PaleView.NONE);
    }

    public static boolean isPale(Player player) {
        return player instanceof AbstractClientPlayer client && PathClient.get(client) == KagunePath.PALE;
    }

    /**
     * 7.0.1: the shared eye-mode fight ({@link com.hex.kagune.control.pale.EyeRules}): {@code player} is another path's ghoul with
     * only the kakugan open (the kagune put away), so fights with her body.
     */
    public static boolean eyes(Player player) {
        return player instanceof AbstractClientPlayer client && PathClient.get(client) != KagunePath.PALE && KakuganClient.ghoul(client)
            && !com.hex.kagune.client.KaguneClientState.get(client).active();
    }

    /** 7.0.1: whether {@code player}'s body is hers now: Owl DNA with the base form out, or the eyes alone ({@link #eyes}). */
    public static boolean fighter(Player player) {
        return isPale(player) && com.hex.kagune.client.KaguneClientState.get(player).active() || eyes(player);
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        VIEWS.clear();
    }
}
