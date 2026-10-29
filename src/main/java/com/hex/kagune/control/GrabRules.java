package com.hex.kagune.control;

/**
 * 1.0.42: Rinkaku's grab (G), slower and breakable (pure). The tendril takes {@link #SLOWER} times the original's time to reach,
 * and reels its catch in only over the last {@link #REEL} ticks of it. 1.0.43: a parry, not a button to mash: only the first swing
 * at him the one it reaches for makes after it sets out is judged, and it breaks the grab only in the last {@link #WINDOW} ticks before it
 * holds ({@link #inWindow}). Too early and that was the one chance. Broken, the tendril goes back and he waits {@link #STAGGER} ticks.
 */
public final class GrabRules {
    /** How many times the original's reach (9 ticks) the tendril now takes. */
    public static final double SLOWER = 2.0;
    /** Ticks at the end of the reach that the catch is reeled in (the original reeled it from its 3rd tick). */
    public static final int REEL = 6;
    /** The original's first tick of reeling in, the earliest it ever does. */
    public static final int ORIGINAL_REEL_FROM = 3;
    /** 1.0.43: the last ticks before it holds that a swing breaks it in (a cue, sound and glint, marks the first). */
    public static final int WINDOW = 8;
    /** Ticks before he can act again once a grab is broken. */
    public static final int STAGGER = 12;
    /** A player's bare swing still counts this many ticks after it began (the server sees it as it goes). */
    public static final int FRESH_SWING = 2;
    /** How hard the one who broke it is thrown back from him (blocks a tick across, and up). */
    public static final double PUSH = 0.45;
    public static final double LIFT = 0.2;

    private GrabRules() {
    }

    /** The reach, in ticks, the original's {@code original} becomes. */
    public static int reach(int original) {
        return (int)Math.round(Math.max(1, original) * SLOWER);
    }

    /** The tick of the reach {@code duration} long that reeling in starts on (never before the original's). */
    public static long reelFrom(int duration) {
        return Math.max(ORIGINAL_REEL_FROM, duration - REEL);
    }

    /** 1.0.43: the tick of the reach {@code duration} long that the window to break it opens on. */
    public static long windowOpens(int duration) {
        return Math.max(0, duration - WINDOW);
    }

    /**
     * A swing begun at {@code swungAt} is one the grab that set out at {@code began} judges {@code now}: begun since (a swing a
     * moment before it set out counts as its first, so one mashing is caught out), and not yet to come.
     */
    public static boolean counts(long began, long swungAt, long now) {
        return swungAt >= began - FRESH_SWING && swungAt <= now;
    }

    /** 1.0.43: a swing begun at {@code swungAt} comes in the window of a grab that set out at {@code began}, {@code duration} long. */
    public static boolean inWindow(long began, int duration, long swungAt) {
        return swungAt >= began + windowOpens(duration);
    }
}
