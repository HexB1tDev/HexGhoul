package com.hex.kagune.control.npc;

/**
 * 4.1.29: the White Reaper's blade string's timing (pure; shared by his brain and how he is drawn, client.animation.ReaperBladePose): each
 * blow's length and the tick it lands.
 */
public final class ReaperBladeRules {
    private ReaperBladeRules() {
    }

    /** A blow: how long (ticks), when it lands. */
    public record Blow(int total, int impact) {
    }

    /** A cleave from over his head; a rising cut from low on his left; a sweep across; a thrust, all his weight behind it. */
    public static final Blow[] STRING = {new Blow(16, 8), new Blow(15, 7), new Blow(17, 8), new Blow(18, 8)};
    public static final int STEPS = STRING.length;

    /** Blow {@code step} (wrapped onto the string). */
    public static Blow blow(int step) {
        return STRING[Math.floorMod(step, STEPS)];
    }
}
