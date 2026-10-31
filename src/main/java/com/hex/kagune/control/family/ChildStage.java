package com.hex.kagune.control.family;

/**
 * 1.3.10: how grown a child is (pure), by its age in Minecraft days: a baby that sits and is carried, a toddler that toddles after you and
 * can still be picked up, a child that can be taught to fight, a teen. Each its own size: its box ({@link #box}, of a player's) and how big
 * it is drawn ({@link #draw}; babies and toddlers on a little one's proportions, a big head, {@link #young}). Never the adult female body.
 */
public enum ChildStage {
    // 1.3.15: a baby and a toddler bigger (the owner lost one: too small to find), on their own little-one proportions.
    BABY(0.0, 0.38F, 0.6F, true, 6.0F),
    TODDLER(2.0, 0.5F, 0.8F, true, 10.0F),
    CHILD(5.0, 0.7F, 0.7F, false, 16.0F),
    TEEN(9.0, 0.88F, 0.88F, false, 24.0F);

    /** From this many Minecraft days old. */
    public final double fromDays;
    /** Its box, as a share of a player's (both ways). */
    public final float box;
    /** How big it is drawn (on a little one's proportions if {@link #young}). */
    public final float draw;
    /** Drawn with a little one's big head and short body (the model's own young proportions). */
    public final boolean young;
    public final float health;

    ChildStage(double fromDays, float box, float draw, boolean young, float health) {
        this.fromDays = fromDays;
        this.box = box;
        this.draw = draw;
        this.young = young;
        this.health = health;
    }

    /** One Minecraft day (game ticks). */
    public static final long DAY = 24000L;

    public static double days(long born, long now) {
        return Math.max(0.0, (now - born) / (double)DAY);
    }

    public static ChildStage of(double days) {
        ChildStage out = BABY;
        for (ChildStage stage : values()) {
            if (days >= stage.fromDays) {
                out = stage;
            }
        }
        return out;
    }

    public static ChildStage byOrdinal(int ordinal) {
        ChildStage[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : BABY;
    }

    /** It sits where it is put (it cannot walk yet). */
    public boolean sits() {
        return this == BABY;
    }

    /** It can be picked up and carried. */
    public boolean carried() {
        return this == BABY || this == TODDLER;
    }

    /** It can be taught to fight, and fights once it has been. */
    public boolean learns() {
        return this == CHILD || this == TEEN;
    }

    /** It can follow you about, or wait where it is told. */
    public boolean walks() {
        return this != BABY;
    }

    /** The next stage, or this if it is the last. */
    public ChildStage next() {
        return this == TEEN ? TEEN : values()[this.ordinal() + 1];
    }
}
