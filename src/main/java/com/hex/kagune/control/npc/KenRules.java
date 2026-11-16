package com.hex.kagune.control.npc;

import com.hex.kagune.control.king.KingCombo;
import com.hex.kagune.control.king.PeaceRules;

/**
 * 1.3.19: Ken's fight (pure): the strongest of them all. The moment a fight finds him he wears Peace, his final kakuja, and throws every
 * one of its moves as a player would (its blades' chain, the launcher and the blows in the air, the spike, the heavy, all six abilities),
 * and with them Eto's own grammar of the body: the Speed Blitz's fists, the Big Punch, the Speed Kick, flips in and out, and a dash in
 * (or back out) far faster than anyone runs.
 */
public final class KenRules {
    private KenRules() {
    }

    /** What he does next, out of what is ready. */
    public enum Act {
        /** Nothing ready that fits: he closes in at a run. */
        CHASE,
        /** A dash in, low and fast (gone, there). */
        DASH_IN,
        /** A dash back out, to make room for what reaches far. */
        DASH_OUT,
        /** Peace's M1: the blades' chain (the launcher after two, the blows in the air, the spike). */
        STRIKE,
        /** Peace's heavy: Cross Cleave, or held long, the Seraph Thrust. */
        HEAVY,
        /** Eto's Speed Blitz: his fists. */
        BLITZ,
        /** Eto's Big Punch (Witness This at the full charge). */
        PUNCH,
        /** Eto's Speed Kick. */
        KICK,
        /** A front flip in over the gap. */
        FLIP_IN,
        /** A back flip clear. */
        FLIP_BACK,
        /** Z: Crossblade Waltz. */
        WALTZ,
        /** J: Aegis of :Re. */
        AEGIS,
        /** G: White Wing Ascent (and, up there over it, the slam). */
        ASCENT,
        /** B: Piercing Salvation (held, then driven out). */
        PIERCE,
        /** C: Rebirth Instinct (held, then his flesh climbs up). */
        REBIRTH,
        /** V: The Final Horizon. */
        HORIZON
    }

    /** How far his blades' M1 reaches (blocks from his front to its near side). */
    public static final double REACH = 3.3;
    /** His fists' reach (Eto's strikes). */
    public static final double FISTS = 2.4;
    /** His abilities' waits against a player's: he throws them far more often. */
    public static final double COOLDOWNS = 0.55;
    /** The dash: how fast (blocks a tick), how long, how often. */
    public static final double DASH_SPEED = 1.25;
    public static final int DASH_TICKS = 6;
    public static final int DASH_EVERY = 36;
    /** How long he holds the heavy for the Seraph Thrust (the full charge), the lance, the burial. */
    public static final int HEAVY_HOLD = KingCombo.CHARGE_MAX;
    public static final int HEAVY_TAP = KingCombo.CHARGE_TICKS + 2;
    public static final int HEAVY_EVERY = 90;
    public static final int PIERCE_HOLD = PeaceRules.PIERCE_CHARGE;
    public static final int REBIRTH_HOLD = 80;
    /** How high up on the Ascent before he wrenches himself down onto it, and how near (level) it must be under him. */
    public static final double SLAM_HEIGHT = 4.0;
    public static final double SLAM_OVER = 6.5;
    /** After how many chained blows he may throw the launcher instead (and how often he does). */
    public static final double LAUNCH_CHANCE = 0.45;
    /** How long out of a fight before Peace goes back into him. */
    public static final long OFF_AFTER = 240L;

    /** When each of his moves is ready again (game ticks): Peace's six by slot, then the dash, the heavy, the fists, the kick, the flips. */
    public record Ready(long waltz, long aegis, long ascent, long pierce, long rebirth, long horizon, long dash, long heavy, long punch, long kick,
                        long flip) {
        static boolean at(long ready, long now) {
            return ready <= now;
        }
    }

    /** The ability on each key slot, as Peace has them (Z, J, G, B, C, V). */
    public static Act ability(int slot) {
        return switch (slot) {
            case 0 -> Act.WALTZ;
            case 1 -> Act.AEGIS;
            case 2 -> Act.ASCENT;
            case 3 -> Act.PIERCE;
            case 4 -> Act.REBIRTH;
            case 5 -> Act.HORIZON;
            default -> null;
        };
    }

    /** The key slot of {@code act} if it is one of Peace's six, else -1. */
    public static int slot(Act act) {
        return switch (act) {
            case WALTZ -> 0;
            case AEGIS -> 1;
            case ASCENT -> 2;
            case PIERCE -> 3;
            case REBIRTH -> 4;
            case HORIZON -> 5;
            default -> -1;
        };
    }

    /**
     * What he does now: {@code gap} blocks off what he fights (from his front to its near side), whether he {@code sees} it, whether he is
     * on the ground, his {@code health} share, whether he was {@code hurt} lately, how many {@code foes} are round him; {@code roll} in
     * [0, 1).
     */
    public static Act choose(double gap, boolean sees, boolean grounded, float health, boolean hurt, int foes, long now, Ready r, double roll) {
        // Hurt and hard-pressed: the shields first.
        if (Ready.at(r.aegis(), now) && hurt && (health < 0.7F || foes >= 3)) {
            return Act.AEGIS;
        }
        // The Final Horizon: whenever it is ready and it is in reach and in sight.
        if (Ready.at(r.horizon(), now) && sees && gap <= PeaceRules.HORIZON_RANGE - 0.5 && roll < 0.55) {
            return Act.HORIZON;
        }
        // Outnumbered, or the fight turning: his flesh to fight beside him.
        if (Ready.at(r.rebirth(), now) && grounded && (foes >= 2 || health < 0.55F) && roll < 0.6) {
            return Act.REBIRTH;
        }
        if (gap > REACH) {
            // At range: the Waltz into it, the lance through it, up onto the wings over it; else in at it, fast.
            if (Ready.at(r.waltz(), now) && sees && gap <= PeaceRules.WALTZ_RANGE - 0.5) {
                return Act.WALTZ;
            }
            if (Ready.at(r.pierce(), now) && sees && grounded && gap >= 5.0 && gap <= 20.0) {
                return Act.PIERCE;
            }
            if (Ready.at(r.ascent(), now) && grounded && gap <= 9.0 && roll < 0.35) {
                return Act.ASCENT;
            }
            if (Ready.at(r.flip(), now) && grounded && gap <= 6.5 && roll < 0.45) {
                return Act.FLIP_IN;
            }
            if (Ready.at(r.kick(), now) && grounded && sees && gap <= 7.0 && roll < 0.7) {
                return Act.KICK;
            }
            if (Ready.at(r.dash(), now) && grounded && gap > 4.5) {
                return Act.DASH_IN;
            }
            return Act.CHASE;
        }
        // Close: the blades, the fists, the heavy; now and then a flip back and out, to throw what reaches far.
        if (Ready.at(r.flip(), now) && grounded && roll > 0.93 && (Ready.at(r.pierce(), now) || Ready.at(r.waltz(), now))) {
            return Act.FLIP_BACK;
        }
        if (Ready.at(r.dash(), now) && grounded && roll > 0.88 && Ready.at(r.pierce(), now)) {
            return Act.DASH_OUT;
        }
        if (Ready.at(r.punch(), now) && grounded && gap <= FISTS && roll < 0.18) {
            return Act.PUNCH;
        }
        if (Ready.at(r.heavy(), now) && grounded && roll >= 0.18 && roll < 0.32) {
            return Act.HEAVY;
        }
        if (gap <= FISTS && roll >= 0.32 && roll < 0.5) {
            return Act.BLITZ;
        }
        if (Ready.at(r.ascent(), now) && grounded && roll >= 0.5 && roll < 0.56) {
            return Act.ASCENT;
        }
        return Act.STRIKE;
    }

    /** His wait for an ability with a player's wait of {@code ticks}. */
    public static long cooldown(int ticks) {
        return Math.max(20L, Math.round(ticks * COOLDOWNS));
    }
}
