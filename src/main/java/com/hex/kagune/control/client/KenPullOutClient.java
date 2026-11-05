package com.hex.kagune.control.client;

import com.hex.kagune.control.client.animation.EtoPose;
import com.hex.kagune.control.client.animation.RigPose;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;

/**
 * 2.4.8: a player Ken taught a kakuja pulls the kagune out his way: a finger cracked first (the right arm alone out in front:
 * {@link EtoPose#crack}), then it bursts out of their back ({@code KenPullOut} on the server). Here, the crack drawn on the body.
 */
public final class KenPullOutClient {
    private KenPullOutClient() {
    }

    /** How long the crack is drawn (ticks): the burst follows at {@code KenPullOut.OUT_AT}. */
    public static final double LASTS = EtoPose.CRACK_END; // 2.5.1: the whole crack, eased up and back down

    private static final Map<Integer, Double> STARTS = new HashMap<>();

    /** The crack begins on player {@code id}. */
    public static void start(int id) {
        STARTS.put(id, ControlClient.worldTime(Minecraft.m_91087_().m_91296_()));
        if (STARTS.size() > 64) {
            STARTS.clear();
        }
    }

    /** How far into the crack player {@code id} is (ticks), or -1 when they aren't cracking. */
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

    /** {@code p} posed for the crack, if player {@code id} is cracking now; true if so. */
    public static boolean apply(RigPose p, int id, double time) {
        double age = age(id, time);
        if (age < 0.0) {
            return false;
        }
        EtoPose.crackOver(p, (float)age); // 2.5.1: over what the body was doing, never a jump to it
        return true;
    }
}
