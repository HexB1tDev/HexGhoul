package com.hex.kagune.control.client;

import com.hex.kagune.control.client.animation.EtoPose;
import com.hex.kagune.control.client.animation.RigPose;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;

/**
 * 4.0.2: a player Noro taught pulls the kagune out his way: they stand, their head tilts slowly far over ({@link EtoPose#tiltOver}), and
 * the kagune comes out ({@code NoroPullOut} on the server). Here, the tilt drawn on the body.
 */
public final class NoroPullOutClient {
    private NoroPullOutClient() {
    }

    /** How long the tilt is drawn (ticks): the kagune comes at {@code NoroPullOut.OUT_AT}, the head comes back after. */
    public static final double LASTS = 46.0;

    private static final Map<Integer, Double> STARTS = new HashMap<>();

    /** The tilt begins on player {@code id}. */
    public static void start(int id) {
        STARTS.put(id, ControlClient.worldTime(Minecraft.m_91087_().m_91296_()));
        if (STARTS.size() > 64) {
            STARTS.clear();
        }
    }

    /** How far into the tilt player {@code id} is (ticks), or -1 when they aren't tilting. */
    public static double age(int id, double time) {
        Double start = STARTS.get(id);
        if (start == null) {
            return -1.0;
        }
        double age = time - start;
        if (age < 0.0 || age > LASTS) {
            if (age > LASTS) {
                STARTS.remove(id);
            }
            return -1.0;
        }
        return age;
    }

    /** {@code p} posed for the tilt, if player {@code id} is tilting now; true if so. */
    public static boolean apply(RigPose p, int id, double time) {
        double age = age(id, time);
        if (age < 0.0) {
            return false;
        }
        EtoPose.tiltOver(p, (float)age, (float)LASTS);
        return true;
    }
}
