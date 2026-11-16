package com.hex.kagune.control.server;

import com.hex.kagune.control.bikaku.WormRoute;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 3.5.9 (the owner: "go around blocks like a worm, sort of, and actually go around them"): the Bikaku worm's way round the world's blocks
 * ({@link WormRoute} over this level: a block lets it through if nothing in it has a collision box; an unloaded one doesn't). The server
 * finds it as a throw begins, sends it to every client, and lays what the worm holds along it.
 */
final class BikakuRoutes {
    private BikakuRoutes() {
    }

    /** The blocks of {@code level} as the worm sees them. */
    static WormRoute.Open open(ServerLevel level) {
        return (x, y, z) -> {
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.m_46805_(pos)) {
                return false;
            }
            BlockState state = level.m_8055_(pos);
            return state.m_60812_(level, pos).m_83281_();
        };
    }

    /** Its way from {@code from} to {@code to} in {@code player}'s level, no longer than {@code reach} (null: none). */
    static double[][] route(ServerPlayer player, Vec3 from, Vec3 to, double reach) {
        return WormRoute.find(new double[] {from.f_82479_, from.f_82480_, from.f_82481_}, new double[] {to.f_82479_, to.f_82480_, to.f_82481_}, reach,
            open(player.m_284548_()));
    }

    /** As far toward {@code to} from {@code from} as the straight line runs before a block (a little short of it). */
    static Vec3 clipped(ServerPlayer player, Vec3 from, Vec3 to) {
        WormRoute.Open open = open(player.m_284548_());
        double[] a = {from.f_82479_, from.f_82480_, from.f_82481_};
        Vec3 d = to.m_82546_(from);
        double length = d.m_82553_();
        if (length < 1.0E-6) {
            return to;
        }
        double last = 0.0;
        for (double along = 0.25; along <= length; along += 0.25) {
            Vec3 p = from.m_82549_(d.m_82490_(along / length));
            if (!WormRoute.clear(a, new double[] {p.f_82479_, p.f_82480_, p.f_82481_}, open)) {
                break;
            }
            last = along;
        }
        return last >= length - 1.0E-6 ? to : from.m_82549_(d.m_82490_(Math.max(0.0, last - 0.3) / length));
    }

    /** True if {@code body} is within reach of the end of {@code route} (world points), the last leg clear to it. */
    static boolean atEnd(ServerPlayer player, double[][] route, Vec3 centre, double slack) {
        if (route == null || route.length < 2) {
            return false;
        }
        double[] end = route[route.length - 1];
        double[] before = route[route.length - 2];
        double[] at = {centre.f_82479_, centre.f_82480_, centre.f_82481_};
        double dx = at[0] - end[0];
        double dy = at[1] - end[1];
        double dz = at[2] - end[2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz) <= slack && WormRoute.clear(before, at, open(player.m_284548_()));
    }

    /** {@code route} (world) in {@code player}'s body frame now. */
    static double[][] rig(ServerPlayer player, double[][] route) {
        double[][] out = new double[route.length][];
        for (int i = 0; i < route.length; i++) {
            out[i] = BikakuServer.rig(player, new Vec3(route[i][0], route[i][1], route[i][2]));
        }
        return out;
    }

    /** A way worth sending (it bends round something): three points or more. */
    static boolean bends(double[][] route) {
        return route != null && route.length >= 3;
    }
}
