package com.hex.kagune.control.quest;

import java.util.List;

/**
 * 7.0.1: what a quest pays (pure): mastery (raw, as the commands add it), mutation points (spent in the path's menu like the
 * milestones'; Rinkaku's mutations come with mastery tiers, so his are paid as {@link #RINKAKU_POINT_MASTERY} mastery each), pieces
 * of Ghoul Meat, the gates it opens, and the side it puts the ghoul on ({@code anteiku} or {@code pack}; ghoul NPCs remember).
 */
public record Reward(int mastery, int points, int ghoulMeat, List<Gate> gates, String side) {
    public static final Reward NONE = new Reward(0, 0, 0, List.of(), null);
    /** What a mutation point is worth to a Rinkaku ghoul, in mastery. */
    public static final int RINKAKU_POINT_MASTERY = 60;

    public Reward {
        gates = gates == null ? List.of() : List.copyOf(gates);
    }

    public static Reward mastery(int mastery) {
        return new Reward(mastery, 0, 0, List.of(), null);
    }

    public Reward points(int points) {
        return new Reward(this.mastery, points, this.ghoulMeat, this.gates, this.side);
    }

    public Reward meat(int pieces) {
        return new Reward(this.mastery, this.points, pieces, this.gates, this.side);
    }

    public Reward opens(Gate... gates) {
        return new Reward(this.mastery, this.points, this.ghoulMeat, List.of(gates), this.side);
    }

    public Reward side(String side) {
        return new Reward(this.mastery, this.points, this.ghoulMeat, this.gates, side);
    }
}
