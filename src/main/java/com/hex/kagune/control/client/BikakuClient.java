package com.hex.kagune.control.client;

import com.hex.kagune.client.KaguneClientState;
import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.bikaku.BikakuMove;
import com.hex.kagune.control.bikaku.BikakuRules;
import com.hex.kagune.control.bikaku.BikakuRules.Bite;
import com.hex.kagune.control.network.S2CBikakuState;
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
 * 3.4.9: the client's store of every Bikaku player's worm (fed only by the server, {@code S2CBikakuState}): the bite it is making and when
 * it began, where it is thrown and at whom. The renderer animates from when each bite began on the client's own clock
 * ({@link PaleClient#clock}: never set back, so a lagging server never throws a bite back a few ticks).
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class BikakuClient {
    /** A bite this few ticks old when it gets here is played from its first frame (it was sent as it began). */
    static final long JUST_BEGUN = 4L;

    /**
     * One player's worm as the server last said, with when (on the client clock) its bite began here; 3.5.0: and its move, when that
     * began here, its point, the bodies it goes after and the one it holds.
     */
    public record View(Bite bite, long startedAt, double began, double x, double y, double z, int targetId, BikakuMove move, long moveAt,
                       double moveBegan, double mx, double my, double mz, int[] targets, int heldId, boolean caught, long regenAt, double regenBegan,
                       boolean kakuja, long kakujaAt, double kakujaBegan, long undyingAt, double undyingBegan, long[] autoAt, double[] autoBegan,
                       int[] autoTarget, double reach, double moveReach, int charge, long chargeAt, double chargeBegan, double[][][] routes,
                       com.hex.kagune.control.bikaku.BikakuForm form, int moveWorms, int[] held, int caughtBits, double[][] points, int mutations,
                       int devourVictim, int devourWorm, long devourAt, double devourBegan, double[] devourSpot, double devourWidth, double devourHeight,
                       long devourReadyAt) {
        public static final View NONE = new View(Bite.NONE, Long.MIN_VALUE, 0.0, 0.0, 0.0, 0.0, 0, BikakuMove.NONE, Long.MIN_VALUE, 0.0, 0.0, 0.0,
            0.0, new int[S2CBikakuState.TARGETS], 0, false, -1_000_000L, -1.0E6, false, -1_000_000L, -1.0E6, -1_000_000L, -1.0E6,
            new long[S2CBikakuState.AUTOS], filled(-1.0E6), new int[S2CBikakuState.AUTOS], BikakuRules.REACH, 0.0, -1, Long.MIN_VALUE, 0.0,
            new double[S2CBikakuState.ROUTES][][], com.hex.kagune.control.bikaku.BikakuForm.ONE, 1, new int[3], 0, new double[3][], 0, 0, -1,
            Long.MIN_VALUE, 0.0, null, 0.6, 1.8, Long.MIN_VALUE);

        /** 3.6.3: ticks into Grand Devouring (-1 none). */
        public double devourAge(double clock) {
            return this.devourVictim == 0 || this.devourAt == Long.MIN_VALUE ? -1.0 : Math.max(0.0, clock - this.devourBegan);
        }

        /** 3.6.3: the worm feeding (-1 none). */
        public int busyWorm(double clock) {
            double age = this.devourAge(clock);
            return age >= 0.0 && com.hex.kagune.control.bikaku.GrandDevour.busy(age) ? this.devourWorm : -1;
        }

        /** 3.5.9: way {@code r} round blocks (0 the bite's, 1 .. 3 the move's), in the world; null for a straight line. */
        public double[][] route(int r) {
            return this.routes == null || r < 0 || r >= this.routes.length ? null : this.routes[r];
        }

        /** 3.5.8: how far toward its farthest the wind-up is (0 .. 1; 0 none). */
        public double charged(double clock) {
            return this.charge < 0 ? 0.0 : Math.max(0.0, Math.min(1.0, (clock - this.chargeBegan) / BikakuRules.CHARGE_FULL));
        }

        /** 3.5.7: ticks since Undying Abomination began. */
        public double undyingAge(double clock) {
            return Math.max(0.0, clock - this.undyingBegan);
        }

        /** 3.5.7: ticks since tentacle {@code i}'s last bite of its own began. */
        public double autoAge(int i, double clock) {
            return i < 0 || i >= this.autoBegan.length ? Double.MAX_VALUE : Math.max(0.0, clock - this.autoBegan[i]);
        }

        private static double[] filled(double value) {
            double[] out = new double[S2CBikakuState.AUTOS];
            java.util.Arrays.fill(out, value);
            return out;
        }

        /** Ticks since the bite began ({@code clock}: {@link PaleClient#clock}); MAX_VALUE for none. */
        public double age(double clock) {
            return this.bite == Bite.NONE || this.startedAt == Long.MIN_VALUE ? Double.MAX_VALUE : Math.max(0.0, clock - this.began);
        }

        /** 3.5.4: ticks since it last went into the kakuja or came out of it. */
        public double kakujaAge(double clock) {
            return Math.max(0.0, clock - this.kakujaBegan);
        }

        /** 3.5.3: ticks since Monstrous Regeneration began. */
        public double regenAge(double clock) {
            return Math.max(0.0, clock - this.regenBegan);
        }

        /** Ticks since the move began; MAX_VALUE for none. */
        public double moveAge(double clock) {
            return this.move == BikakuMove.NONE || this.moveAt == Long.MIN_VALUE ? Double.MAX_VALUE : Math.max(0.0, clock - this.moveBegan);
        }
    }

    private static final Map<UUID, View> VIEWS = new HashMap<>();

    private BikakuClient() {
    }

    public static void accept(S2CBikakuState message) {
        Minecraft minecraft = Minecraft.m_91087_();
        long world = minecraft.f_91073_ == null ? 0L : minecraft.f_91073_.m_46467_();
        double clock = PaleClient.clock(0.0F);
        View before = VIEWS.get(message.playerId());
        long behind = message.startedAt() == Long.MIN_VALUE ? 0L : world - message.startedAt();
        double began = clock - (behind > JUST_BEGUN ? behind : 0L);
        if (before != null && before.startedAt() == message.startedAt() && before.bite().ordinal() == message.bite()) {
            began = before.began(); // the same bite, told again: its clock runs on
        }
        long moveBehind = message.moveAt() == Long.MIN_VALUE ? 0L : world - message.moveAt();
        double moveBegan = clock - (moveBehind > JUST_BEGUN ? moveBehind : 0L);
        if (before != null && before.moveAt() == message.moveAt() && before.move().ordinal() == message.move()) {
            moveBegan = before.moveBegan(); // the same move, told again (it caught something, it let go): its clock runs on
        }
        long regenBehind = world - message.regenAt();
        double regenBegan = clock - (regenBehind > JUST_BEGUN ? Math.min(regenBehind, 100_000L) : 0L);
        if (before != null && before.regenAt() == message.regenAt()) {
            regenBegan = before.regenBegan();
        }
        VIEWS.put(message.playerId(), new View(Bite.byOrdinal(message.bite()), message.startedAt(), began, message.x(), message.y(), message.z(),
            message.targetId(), BikakuMove.byOrdinal(message.move()), message.moveAt(), moveBegan, message.mx(), message.my(), message.mz(),
            message.targets(), message.heldId(), message.caught(), message.regenAt(), regenBegan, message.kakuja(), message.kakujaAt(),
            kakujaBegan(before, message, world, clock), message.undyingAt(), began(before == null ? null : before.undyingAt(), before == null ? 0.0
                : before.undyingBegan(), message.undyingAt(), world, clock), message.autoAt(), autoBegan(before, message, world, clock), message.autoTarget(),
            message.reach(), message.moveReach(), message.charge(), message.chargeAt(), began(before == null ? null : before.chargeAt(), before == null ? 0.0
                : before.chargeBegan(), message.chargeAt(), world, clock), message.routes(), com.hex.kagune.control.bikaku.BikakuForm.byOrdinal(message.form()),
            message.moveWorms(), message.held(), message.caughtBits(), message.points(), message.mutations(), message.devourVictim(),
            message.devourWorm(), message.devourAt(), began(before == null || before.devourVictim() != message.devourVictim() ? null : before.devourAt(),
                before == null ? 0.0 : before.devourBegan(), message.devourAt(), world, clock), message.devourSpot(), message.devourWidth(),
            message.devourHeight(), message.devourReadyAt()));
    }

    /**
     * 3.6.3: how far into being devoured {@code entity} is (ticks, by whoever is feeding on it; -1 if nobody is), on the clock {@code clock}
     * ({@code PaleClient.clock}).
     */
    public static double devoured(int entity, double clock) {
        if (entity == 0) {
            return -1.0;
        }
        for (View view : VIEWS.values()) {
            if (view.devourVictim() == entity) {
                double age = view.devourAge(clock);
                if (age >= 0.0 && age < com.hex.kagune.control.bikaku.GrandDevour.KILL + 40) {
                    return age;
                }
            }
        }
        return -1.0;
    }

    /** 3.5.7: when (the client clock) something that began at {@code at} began here: its clock running on if it is told again. */
    private static double began(Long beforeAt, double beforeBegan, long at, long world, double clock) {
        if (beforeAt != null && beforeAt == at) {
            return beforeBegan;
        }
        long behind = at == Long.MIN_VALUE ? 100_000L : world - at;
        return clock - (behind > JUST_BEGUN ? Math.min(behind, 100_000L) : 0L);
    }

    /** 3.5.7: when each tentacle's own bite began here. */
    private static double[] autoBegan(View before, S2CBikakuState message, long world, double clock) {
        double[] out = new double[S2CBikakuState.AUTOS];
        for (int i = 0; i < out.length; i++) {
            long at = message.autoAt() != null && i < message.autoAt().length ? message.autoAt()[i] : Long.MIN_VALUE;
            boolean same = before != null && before.autoAt() != null && i < before.autoAt().length && before.autoAt()[i] == at;
            out[i] = began(same ? at : null, same ? before.autoBegan()[i] : 0.0, at, world, clock);
        }
        return out;
    }

    /** When (the client clock) the kakuja last went in or out here: from its first frame if just begun, its clock running on if told again. */
    private static double kakujaBegan(View before, S2CBikakuState message, long world, double clock) {
        if (before != null && before.kakujaAt() == message.kakujaAt()) {
            return before.kakujaBegan();
        }
        long behind = world - message.kakujaAt();
        return clock - (behind > JUST_BEGUN ? Math.min(behind, 100_000L) : 0L);
    }

    public static View get(Player player) {
        return player == null ? View.NONE : VIEWS.getOrDefault(player.m_36316_().getId(), View.NONE);
    }

    public static boolean isBikaku(Player player) {
        return player instanceof AbstractClientPlayer client && PathClient.get(client) == KagunePath.BIKAKU;
    }

    /** 0..1: how far the kagune is out (the original's manifest and dismiss timing; 0 put away). */
    public static float deploy(Player player, float partial) {
        return UkakuClient.deploy(KaguneClientState.get(player), ControlClient.worldTime(partial));
    }

    /**
     * 3.6.2: true while {@code player} is in The Walker with the kagune out (not in the kakuja): its walking tentacles carry him, his own
     * legs hang still, and (his own client) he climbs walls.
     */
    public static boolean walking(Player player, float partial) {
        if (!isBikaku(player)) {
            return false;
        }
        View view = get(player);
        return view.form().walks() && !view.kakuja() && deploy(player, partial) > 0.5F;
    }

    /** The worm's seed for {@code player}: the same on every client. */
    public static int seed(Player player) {
        UUID id = player.m_36316_().getId();
        return BikakuRules.seed(id.getMostSignificantBits(), id.getLeastSignificantBits());
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        VIEWS.clear();
    }
}
