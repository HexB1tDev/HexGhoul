package com.hex.kagune.control.king;

/**
 * 1.0.6: what War's body is doing (pure), as the server says and every client draws it ({@code WarPose}, {@code WarModel}): the
 * moves of its six abilities, step by step, each with how long its body takes ({@code ticks}; 0: held until the next). 1.0.13: the
 * moves of the six abilities as they are now ({@link WarRules}). 1.0.14: and his M1's blows ({@link #STRIKE}).
 */
public enum KingMove {
    NONE(0),
    /** King's Execution: the free hand thrown forward, the centipedes pouring out of the arm to seize, coil and crush. */
    GRASP(WarRules.GRASP_TICKS),
    /** Black Reaper: the dash, low, the bladed arm trailing. */
    DASH(WarRules.DASH_TICKS),
    /** Black Reaper: the cleave at the end of it, the bladed arm raised high and brought down with all his weight. */
    CLEAVE(WarRules.CLEAVE_TICKS),
    /** Centipede Dominion: he is the small Dragon (held until it is over; his body is not drawn). */
    WYRM(0),
    /** Spire Wave: the bladed arm raised and driven into the ground. */
    SPIRE(WarRules.SPIRE_TICKS),
    /** King's Defiance: the bladed arm raised across him, waiting for the blow (held). */
    DEFIANCE(0),
    /** King's Defiance: the counter from behind his attacker. */
    COUNTER(WarRules.COUNTER_TICKS),
    /** King's Defiance: wrapped in his rinkaku, storing what strikes him. */
    COCOON(WarRules.COCOON_TICKS),
    /** King's Defiance: the cocoon bursting outward. */
    BURST(WarRules.BURST_TICKS),
    /** Black Sun: the eye opened, the arms spread, the head thrown back under the black sun. */
    SUN(WarRules.SUN_TICKS),
    /** 1.0.14: one of his M1's blows: which ({@link WarStrike}) is the move's level, and it lasts as long as that blow does. */
    STRIKE(0),
    // 1.0.15: Peace's.
    /** Peace's M1: one blow of its blades ({@link PeaceStrike}, the level), as long as that blow. */
    BLADES(0),
    /** Crossblade Waltz: its three strikes. */
    WALTZ(PeaceRules.WALTZ_TICKS),
    /** (Aegis of :Re before 1.0.34: the wall. Now its shields are their own, and the blades are free.) */
    AEGIS(0),
    /** (Aegis of :Re before 1.0.34: the wall opening.) */
    AEGIS_OPEN(10),
    /** White Wing Ascent: lifted on its two tendrils, the blades spread as wings (held until he is down). */
    ASCENT(0),
    /** White Wing Ascent: wrenched down by the tendrils (held until he lands). */
    DIVE(0),
    /** White Wing Ascent: landed, the blades struck into the ground all round, the flesh tearing up out of it. */
    LANDING(PeaceRules.LAND_TICKS),
    /** Piercing Salvation: level 0 the lance gathering (held); level 1 out (held until it is back). */
    PIERCE(0),
    /** Rebirth Instinct: level 0 the blades in the ground (held); level 1 drawn back out of it. */
    REBIRTH(0),
    /** The Final Horizon: the uppercut, the blades into it in the sky, the blow down, the drive through the ground. */
    HORIZON(PeaceRules.HORIZON_TICKS),
    /** 1.0.17: Peace coming over him, its kagune unfolding as wings (only ever drawn: its client makes it from the transformation). */
    EMERGE(KingTransform.PEACE_TICKS),
    /** 1.0.19: Peace leaving him, its blades folding in and sinking into his back (only ever drawn, as EMERGE). */
    RECEDE(KingTransform.PEACE_LEAVE),
    /** 1.0.20: the heavy held, charging (held until let go): War crouched, the blade dragged back; Peace's blades drawn back behind him. */
    CHARGE(0),
    // 1.0.27: the warpath (War's rework; appended, so the others keep their numbers).
    /** War's RMB: the hand swiped out where he looks, and the spikes coming up in a cone. */
    CONE(WarRules.CONE_TICKS),
    /** King's Execution at nothing: at the end of the dash, both hands raised, the ground's spikes up round him. */
    RISE(WarRules.RISE_TICKS),
    /** King's Execution at a body: the blitz (the punch, the cut, the kick, the slowed blow to the gut), by its own clock. */
    BLITZ(WarRules.BLITZ_TICKS),
    /** Black Sun (the ultimate): the dash, the uppercut, the leap after it, the cocked punch, the ring of air. */
    SUNDER(WarRules.SUNDER_LAUNCH + 30),
    // 1.0.28: the held moves.
    /** Black Reaper (held): the centipedes out of his back at it, dragging it in, the blade through it, going back (the level). */
    REAPER(0),
    /** King's Defiance (held): the arm over his face, the rinkaku closed round him (level 0), opening (level 1). */
    DEFY(0),
    /** Spire Wave, held: the free arm out at where he looks, the centipedes winding round it, more and bigger the longer. */
    SPIRE_AIM(0),
    /** Spire Wave, let go: the arm thrust out after them as they go, held out through the crush, eased back as they come back. */
    SPIRE_SEND(0);

    private final int ticks;

    KingMove(int ticks) {
        this.ticks = ticks;
    }

    /** How long the body takes over it (0: until the next move). */
    public int ticks() {
        return this.ticks;
    }

    public static KingMove byOrdinal(int ordinal) {
        KingMove[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : NONE;
    }

    /** 1.0.14: how long the body takes over it at {@code level} (a blow: that blow's length). */
    public int ticks(int level) {
        return this == STRIKE ? WarStrike.byOrdinal(level).duration : this == BLADES ? PeaceStrike.byOrdinal(level).duration : this.ticks;
    }

    /** How far through it is {@code age} ticks in: 0..1 (a held move stays at 0). */
    public double progress(double age) {
        return this.progress(age, 0);
    }

    /** 1.0.14: how far through it is {@code age} ticks in at {@code level}: 0..1 (a held move stays at 0). */
    public double progress(double age, int level) {
        int ticks = this.ticks(level);
        return ticks <= 0 ? 0.0 : Math.max(0.0, Math.min(1.0, age / ticks));
    }

    /**
     * 1.0.36: a move that carries his body along its own path (the blitz, Black Sun, the dash, the Final Horizon, the Ascent): his own
     * keys move nothing through it (they only fought the path: the shaking, and worse).
     */
    public boolean carries() {
        return this == BLITZ || this == SUNDER || this == DASH || this == HORIZON || this == ASCENT || this == DIVE || this == LANDING;
    }
}
