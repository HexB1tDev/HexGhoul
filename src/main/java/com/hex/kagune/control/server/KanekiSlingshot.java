package com.hex.kagune.control.server;

import com.hex.kagune.control.SlingshotPath;
import com.hex.kagune.gameplay.KaguneRuntime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Kaneki's grapple (J) since 3.0.0: the second press no longer flings him past the block his tendril grabbed, it hauls him
 * up and over and sets him down on top of it ({@link SlingshotPath}). The launch is the trampoline target of the original's
 * {@code launchSlingshot} (where it set his velocity); the server then steers him tick by tick, his client is told each
 * tick's motion, and the landing puts him exactly on the spot. If the block's top is covered (a trunk, a wall), he lands on
 * the first place above it he fits; if there is none within reach, the original leap runs instead.
 */
public final class KanekiSlingshot {
    /** How far up the grabbed block's column a place to stand is looked for (blocks). */
    static final int SEARCH = 24;
    /** The launch target the original hands over is the grabbed block's centre this far up. */
    static final double TARGET_LIFT = 8.0;
    private static final Map<UUID, Flight> FLIGHTS = new HashMap<>();

    /** A flight under way: its path, how far along it is, and where he lands. */
    private static final class Flight {
        final SlingshotPath path;
        final Vec3 landing;
        final ServerLevel level;
        int tick;

        Flight(SlingshotPath path, Vec3 landing, ServerLevel level) {
            this.path = path;
            this.landing = landing;
            this.level = level;
        }
    }

    private KanekiSlingshot() {
    }

    /**
     * The launch: {@code velocity} is the original's leap, {@code target} the grabbed block's centre {@link #TARGET_LIFT} up.
     * Called where the original set his velocity; it still syncs it to his client right after.
     */
    public static void launch(ServerPlayer player, Vec3 velocity, KaguneRuntime.State state, Vec3 target) {
        Vec3 landing = landing(player, target.f_82479_, target.f_82480_ - TARGET_LIFT - 0.5, target.f_82481_);
        if (landing == null) {
            FLIGHTS.remove(player.m_20148_());
            player.m_20256_(velocity);
            return;
        }
        Vec3 from = player.m_20182_();
        SlingshotPath path = SlingshotPath.plan(new double[] {from.f_82479_, from.f_82480_, from.f_82481_},
            new double[] {landing.f_82479_, landing.f_82480_, landing.f_82481_});
        FLIGHTS.put(player.m_20148_(), new Flight(path, landing, player.m_284548_()));
        player.m_20256_(motion(path, 0));
    }

    /** A new grapple ends the flight under way. */
    public static void cancel(ServerPlayer player) {
        FLIGHTS.remove(player.m_20148_());
    }

    /** Once a tick per player, after his own tick: the next stretch of the flight, or the landing. */
    static void tick(ServerPlayer player) {
        Flight flight = FLIGHTS.get(player.m_20148_());
        if (flight == null) {
            return;
        }
        if (!player.m_6084_() || player.m_5833_() || player.m_20159_() || player.m_284548_() != flight.level) {
            FLIGHTS.remove(player.m_20148_());
            return;
        }
        flight.tick++;
        player.f_19789_ = 0.0F;
        if (flight.tick < flight.path.ticks()) {
            push(player, motion(flight.path, flight.tick));
            return;
        }
        FLIGHTS.remove(player.m_20148_());
        Vec3 at = player.m_20182_();
        Vec3 landing = fits(player, flight.landing.f_82479_, flight.landing.f_82480_, flight.landing.f_82481_) ? flight.landing
            : landing(player, flight.landing.f_82479_, flight.landing.f_82480_ - 1.0, flight.landing.f_82481_);
        if (landing != null && at.m_82557_(landing) > 1.0E-4) {
            player.m_6021_(landing.f_82479_, landing.f_82480_, landing.f_82481_);
        }
        push(player, Vec3.f_82478_);
    }

    /** True while a flight is under way. */
    public static boolean flying(ServerPlayer player) {
        return FLIGHTS.containsKey(player.m_20148_());
    }

    private static Vec3 motion(SlingshotPath path, int tick) {
        double[] v = path.velocity(tick);
        return new Vec3(v[0], v[1], v[2]);
    }

    private static void push(ServerPlayer player, Vec3 motion) {
        player.m_20256_(motion);
        player.f_19812_ = true;
        player.f_19864_ = true;
    }

    /**
     * Where he stands on top of the block whose bottom centre is ({@code x}, {@code bottom}, {@code z}): the first place up its
     * column where he fits, settled down onto whatever it stands on (a slab's top, not a block above it). Null if there is none.
     */
    static Vec3 landing(ServerPlayer player, double x, double bottom, double z) {
        double base = Math.floor(bottom + 0.5);
        for (int up = 1; up <= SEARCH; up++) {
            double y = base + up;
            if (!fits(player, x, y, z)) {
                continue;
            }
            // Settle onto the surface below in sixteenths, at most one block down.
            for (int step = 0; step < 16 && fits(player, x, y - 1.0 / 16.0, z); step++) {
                y -= 1.0 / 16.0;
            }
            return new Vec3(x, y, z);
        }
        return null;
    }

    /** True if his body fits with his feet at ({@code x}, {@code y}, {@code z}). */
    private static boolean fits(ServerPlayer player, double x, double y, double z) {
        ServerLevel level = player.m_284548_();
        Vec3 at = player.m_20182_();
        AABB box = player.m_20191_().m_82386_(x - at.f_82479_, y - at.f_82480_, z - at.f_82481_);
        return level.m_45756_(player, box);
    }
}
