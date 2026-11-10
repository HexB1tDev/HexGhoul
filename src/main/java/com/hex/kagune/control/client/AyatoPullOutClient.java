package com.hex.kagune.control.client;

import com.hex.kagune.control.client.animation.RigPose;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;

/**
 * 4.1.0 (the owner: "the player lifts both hands god like and then they're unleashed"): a player Ayato taught brings the kagune out his
 * way ({@code AyatoPullOut} on the server). Here, the lift drawn on the body: they rise a little onto their toes, both arms lifted out
 * wide from the sides, a little below the shoulders, the elbows soft, the palms turned open to the sky, the chin up, calm; held as the
 * wings burst out behind them ({@code AyatoPullOut.OUT_AT}), then the arms come down.
 */
public final class AyatoPullOutClient {
    private AyatoPullOutClient() {
    }

    /** How long the lift is drawn (ticks): the arms out by 14, the wings at 20, held, then down by 40. */
    public static final double LASTS = 40.0;

    private static final Map<Integer, Double> STARTS = new HashMap<>();

    /** The lift begins on player {@code id}. */
    public static void start(int id) {
        STARTS.put(id, ControlClient.worldTime(Minecraft.m_91087_().m_91296_()));
        if (STARTS.size() > 64) {
            STARTS.clear();
        }
    }

    /** How far into the lift player {@code id} is (ticks), or -1 when they aren't lifting. */
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

    /** {@code p} posed for the lift, if player {@code id} is lifting now; true if so. */
    public static boolean apply(RigPose p, int id, double time) {
        double age = age(id, time);
        if (age < 0.0) {
            return false;
        }
        lift(p, (float)age);
        return true;
    }

    /** The god-like lift at {@code t} ticks (pure: drawn the same wherever it is asked for). */
    public static void lift(RigPose p, float t) {
        float up = t < 14.0F ? ease(t / 14.0F) : t > 30.0F ? 1.0F - ease(Math.min(1.0F, (t - 30.0F) / 10.0F)) : 1.0F;
        // A slow breath in as the arms rise: the chest opens, the chin lifts, up onto the toes.
        p.rightArmZ = 1.15F * up;
        p.leftArmZ = -1.15F * up;
        p.rightArmX = -0.18F * up;
        p.leftArmX = -0.18F * up;
        // The palms turned up to the sky.
        p.rightArmY = -1.25F * up;
        p.leftArmY = 1.25F * up;
        p.rightElbow = 0.18F * up;
        p.leftElbow = 0.18F * up;
        p.headX = -0.28F * up;
        p.lean = -0.06F * up;
        p.drop = -0.8F * up;
        p.armHold = Math.max(p.armHold, up);
    }

    private static float ease(float x) {
        float t = Math.max(0.0F, Math.min(1.0F, x));
        return t * t * (3.0F - 2.0F * t);
    }
}
