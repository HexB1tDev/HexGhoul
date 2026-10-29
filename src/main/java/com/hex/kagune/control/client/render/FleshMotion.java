package com.hex.kagune.control.client.render;

import com.hex.kagune.control.client.animation.BodyFrame;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.eto.EtoFleshModel;
import com.hex.kagune.control.eto.FleshPhysics;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 6.2.0: every length of Eto's flesh in the world carrying its own weight ({@link FleshPhysics}): for each Eto and each of her
 * lengths of flesh (the one out of her back, the club, each tentacle, the arm), a chain that follows the path the move gives it,
 * frame by frame, in the world: so when she moves, it trails after her, and when the move swings it, the heavy end lags, whips
 * through and swings back. A new move starts it afresh.
 */
final class FleshMotion {
    /** A length of flesh unseen this long (ticks) is forgotten. */
    static final double FORGET = 100.0;
    /** How far (blocks) the trunk keeps off her body once it is out of her back (it may lag into her otherwise). */
    static final double CLEAR = 0.2;
    /** Points this near the root are still coming out of her back: they are not pushed off it. */
    static final int EMERGING = 3;

    private static final Map<Long, State> CHAINS = new HashMap<>();

    private FleshMotion() {
    }

    private static final class State {
        final FleshPhysics.Chain chain = new FleshPhysics.Chain();
        double lastTime = Double.NaN;
        long startedAt = Long.MIN_VALUE;
        int move = -1;
    }

    /** Turns a point of her rig space into the world (blocks), and back. */
    interface Space {
        double[] world(double[] rig);

        double[] rig(double[] world);
    }

    /**
     * {@code pose}'s flesh as it carries itself: entity {@code entity}'s length of flesh number {@code slot}, the move begun at
     * {@code startedAt} ({@code move} its kind), at {@code time} (EtoClient's clock); {@code body} her body as drawn (kept clear of
     * it). Null when none is out (and then the chain is let go).
     */
    static EtoFleshModel.Pose carried(int entity, int slot, long startedAt, int move, EtoFleshModel.Pose pose, Space space, double time,
                                      FleshPhysics.Feel feel, RigPose body) {
        return carried(entity, slot, startedAt, move, pose, space, time, feel, body, true);
    }

    /** As above; {@code rightArm} false: her own right arm is not drawn (the Mutated Arm is in its place), so nothing is kept off it. */
    static EtoFleshModel.Pose carried(int entity, int slot, long startedAt, int move, EtoFleshModel.Pose pose, Space space, double time,
                                      FleshPhysics.Feel feel, RigPose body, boolean rightArm) {
        long key = (long)entity * 16L + slot;
        State state = CHAINS.computeIfAbsent(key, ignored -> new State());
        if (state.startedAt != startedAt || state.move != move) {
            state.chain.reset();
            state.startedAt = startedAt;
            state.move = move;
            state.lastTime = Double.NaN;
        }
        double dt = Double.isNaN(state.lastTime) ? 0.0 : time - state.lastTime;
        state.lastTime = time;
        if (pose == null) {
            state.chain.reset();
            return null;
        }
        double[][] path = pose.path();
        double[][] world = new double[path.length][];
        for (int i = 0; i < path.length; i++) {
            world[i] = space.world(path[i]);
        }
        double[][] carried = state.chain.step(world, Math.max(0.0, dt), feel);
        if (carried == null) {
            return pose;
        }
        BodyFrame.Solid solid = new BodyFrame.Solid(body, rightArm);
        double[][] rig = new double[carried.length][];
        for (int i = 0; i < carried.length; i++) {
            rig[i] = space.rig(carried[i]);
            if (i >= EMERGING) {
                rig[i] = solid.pushOut(rig[i], CLEAR);
            }
        }
        return new EtoFleshModel.Pose(rig, pose.up(), pose.axis(), pose.grown(), pose.shape(), pose.grip(), pose.time(), pose.seed(), pose.keep());
    }

    /** Forgets the chains not drawn for a while (bodies gone out of sight, moves long over). */
    static void forget(double time) {
        for (Iterator<State> it = CHAINS.values().iterator(); it.hasNext(); ) {
            State state = it.next();
            if (Double.isNaN(state.lastTime) || time - state.lastTime > FORGET || time < state.lastTime - FORGET) {
                it.remove();
            }
        }
    }

    static void clear() {
        CHAINS.clear();
    }
}
