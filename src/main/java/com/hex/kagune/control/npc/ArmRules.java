package com.hex.kagune.control.npc;

import com.hex.kagune.control.quinque.Arm;

/**
 * 3.0.0: who carries the higher ranks' quinques ({@link Arm}) and how they fight with them (pure). The better the rank, the likelier one of
 * its own kagune-made weapons: none below Rank 4, then more and more often, always from Rank 8 (their weapons a masterwork: grown bigger,
 * cutting harder). The White Reaper keeps Dawn.
 *
 * <p>A string of blows goes on while each comes soon after the last ({@link #next}); its moves of its own are taken now and then, more
 * often the better the rank ({@link #ability}), each when its foe is at a distance it is used from and its cooldown is over.
 */
public final class ArmRules {
    private ArmRules() {
    }

    /** From this rank up an investigator may carry one. */
    public static final int FROM = 4;
    /** From this rank up its weapon is a masterwork. */
    public static final int MASTERWORK = 8;
    /** A masterwork's edge on its blows. */
    public static final float MASTERWORK_DAMAGE = 1.2F;
    /** A blow this long after the last (ticks) starts its string over. */
    public static final long CHAIN_GAP = 30L;
    /** How long it takes for a weapon to tear out of its case or go back into it (ticks). */
    public static final int UNFOLD = 14;
    /** How often (ticks) it thinks about one of its own moves. */
    public static final int THINK_EVERY = 10;

    /** The chance an investigator of {@code rank} carries one of these. */
    public static double chance(AgentRules.Rank rank) {
        if (rank == AgentRules.Rank.RANK_10) {
            return 0.0; // the White Reaper: Dawn
        }
        return switch (rank.level()) {
            case 4 -> 0.3;
            case 5 -> 0.45;
            case 6 -> 0.6;
            case 7 -> 0.75;
            case 8, 9 -> 1.0;
            default -> 0.0;
        };
    }

    /**
     * The weapon an investigator of {@code rank} carries, or null (a regular quinque): {@code r} decides whether, {@code which} which. Rank 4 and
     * 5 a whip or a spear; from Rank 6 any of them, the heavy ones as often as the light.
     */
    public static Arm roll(AgentRules.Rank rank, double r, double which) {
        if (r >= chance(rank)) {
            return null;
        }
        double w = Math.max(0.0, Math.min(0.999999, which));
        if (rank.level() < 6) {
            return w < 0.5 ? Arm.WHIP : Arm.CRYSTAL_SPEAR;
        }
        Arm[] all = Arm.values();
        return all[(int)(w * all.length)];
    }

    /** True if an investigator of {@code rank}'s weapon is a masterwork. */
    public static boolean masterwork(AgentRules.Rank rank) {
        return rank.level() >= MASTERWORK && rank != AgentRules.Rank.RANK_10;
    }

    /** What its {@code step}th blow deals, before its rank's power. */
    public static float damage(Arm arm, int step, boolean masterwork) {
        return arm.damage * arm.strike(step).damage() * (masterwork ? MASTERWORK_DAMAGE : 1.0F);
    }

    /** What one of {@code ability}'s blows deals, before its rank's power. */
    public static float damage(Arm arm, Arm.Ability ability, boolean masterwork) {
        return arm.damage * ability.damage * (masterwork ? MASTERWORK_DAMAGE : 1.0F);
    }

    /** How far its {@code step}th blow reaches (blocks); a masterwork a little further. */
    public static double reach(Arm arm, int step, boolean masterwork) {
        return arm.reach * arm.strike(step).reach() * (masterwork ? 1.12 : 1.0);
    }

    /** The blow after {@code step}, {@code gap} ticks after it: the next of its string, or the first again (a pause, or the string done). */
    public static int next(Arm arm, int step, long gap) {
        if (step < 0 || gap > CHAIN_GAP || step >= arm.length() - 1) {
            return 0;
        }
        return step + 1;
    }

    /** How long a blow keeps it from the next (ticks), at its rank's pace; the last of a string a breath longer. */
    public static long recover(Arm arm, int step, double pace) {
        Arm.Strike strike = arm.strike(step);
        long rest = Math.round((strike.total() - strike.land()) * pace);
        return Math.max(2L, rest + (step == arm.length() - 1 ? 8L : 0L));
    }

    /** A move's cooldown at its rank's pace (ticks). */
    public static long cooldown(Arm.Ability ability, double pace) {
        return Math.max(20L, Math.round(ability.cooldown * pace));
    }

    /** How readily one of its rank uses a move of its own when it could (each time it thinks about it). */
    public static double eagerness(AgentRules.Rank rank) {
        return Math.min(0.65, 0.2 + 0.05 * (rank.level() - FROM));
    }

    /**
     * The move it takes now, or null: of its two ({@code ready}: whose cooldown is over), one its foe is at a distance it is used from
     * ({@code distance}), if {@code roll} falls under its eagerness; {@code pick} chooses between two that would both do.
     */
    public static Arm.Ability ability(Arm arm, AgentRules.Rank rank, double distance, boolean[] ready, double roll, double pick) {
        if (roll >= eagerness(rank)) {
            return null;
        }
        Arm.Ability[] own = arm.abilities();
        Arm.Ability first = ready.length > 0 && ready[0] && own[0].fits(distance) ? own[0] : null;
        Arm.Ability second = ready.length > 1 && ready[1] && own[1].fits(distance) ? own[1] : null;
        if (first != null && second != null) {
            return pick < 0.5 ? first : second;
        }
        return first != null ? first : second;
    }

    /**
     * Its swing as its clients are told it ({@code CcgAgent.swing}): a blow of its string ({@link #STEP} up) or one of its moves
     * ({@link #ABILITY} up): above the regular quinques' swings (0 to 6).
     */
    public static final int STEP = 16;
    public static final int ABILITY = 32;

    public static int stepCode(int step) {
        return STEP + Math.max(0, Math.min(15, step));
    }

    public static int abilityCode(Arm.Ability ability) {
        return ABILITY + ability.ordinal();
    }

    /** The blow of its string a swing is ({@code code}), or -1. */
    public static int step(int code) {
        return code >= STEP && code < ABILITY ? code - STEP : -1;
    }

    /** The move a swing is ({@code code}), or null. */
    public static Arm.Ability ability(int code) {
        return code >= ABILITY ? Arm.Ability.byOrdinal(code - ABILITY) : null;
    }

    /** Its weapon as its clients are told it: its arm (plus one), a masterwork, out of its case (bits). 0: none. */
    public static int pack(Arm arm, boolean masterwork, boolean out) {
        return arm == null ? 0 : (arm.ordinal() + 1) | (masterwork ? 1 << 4 : 0) | (out ? 1 << 5 : 0);
    }

    public static Arm arm(int packed) {
        return packed == 0 ? null : Arm.byOrdinal((packed & 0xF) - 1);
    }

    public static boolean masterwork(int packed) {
        return (packed & 1 << 4) != 0;
    }

    public static boolean out(int packed) {
        return (packed & 1 << 5) != 0;
    }

    /** How far out of its case it is ({@code age} ticks since it last came out or went in; {@code out} which): 0 in, 1 out, eased. */
    public static float shown(boolean out, double age) {
        double t = Math.max(0.0, Math.min(1.0, age / UNFOLD));
        double eased = t * t * (3.0 - 2.0 * t);
        return (float)(out ? eased : 1.0 - eased);
    }
}
