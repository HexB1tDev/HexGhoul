package com.hex.kagune.control.eto;

/**
 * What Eto's body is doing (6.1.0): one move at a time, sent to every client with the tick it began so they all animate it in
 * step with the server. Sent by ordinal: new moves go at the end.
 */
public enum EtoMove {
    /** Nothing: her stance, her idle play, walking. */
    NONE,
    /** A Speed Blitz strike (detail: {@link EtoStrike} ordinal). */
    STRIKE,
    /** A flip (detail: {@link EtoRules.Flip} ordinal). */
    FLIP,
    /** Z held: Big Punch winding up (charge from {@code startedAt}). */
    PUNCH_CHARGE,
    /** Big Punch let go: the blitz, the gut punch, the launch (charge as released). */
    PUNCH,
    /** J tapped: the flesh erupts and sweeps across in front of her. */
    SWIPE,
    /** J held: the flesh reaches for what she looks at (target: the entity, point: where it reaches when there is none). */
    GRAB_REACH,
    /** The flesh hand holds its catch while J stays down. */
    GRAB_HOLD,
    /** J let go: pulled back, whipped round and thrown. */
    GRAB_THROW,
    /** The reach closed on nothing: the hand clutches air and goes back in. */
    GRAB_MISS,
    /** V tapped: the speed kick. */
    KICK,
    /** V held: the leg rises and the flesh club grows round it (charge from {@code startedAt}). */
    HAMMER_CHARGE,
    /** V let go: the axe kick slams the club into the ground. */
    HAMMER,
    /** B: Watch This, her arms thrown out in a cross as the tentacles burst out of her back (they are {@code EtoWatch}'s after). */
    WATCH,
    /**
     * 6.2.0, the Owl (her first kakuja): Z held, its Big Punch winding up: she sinks under it, the right giant arm reared up behind her,
     * the left one's blade braced on the ground (charge from {@code startedAt}).
     */
    OWL_PUNCH_CHARGE,
    /**
     * The Owl's Big Punch let go: her fist and the right giant arm's elbow driven into it together, the blade whipping over after;
     * it flies dead level along the blow, through what is in its way ({@code OwlRules}; charge as released).
     */
    OWL_PUNCH,
    /**
     * The Owl's Speed Kick (V tapped): the dash, all but unseen, the right giant arm's blade through it and holding it, the left
     * one's across its neck, the right drawn out, the spinning heel sending it flying (detail: the dash's ticks; point: where she
     * plants; target: what she kicks; {@code OwlRules}).
     */
    OWL_KICK,
    /**
     * The Owl's bone spikes (J held): hunched over, they burst out of her back all round her and are held out while J is; let go,
     * drawn back in (detail: their seed, {@code OwlSpikes.of}; charge: the tick she let go, 0 while held).
     */
    OWL_SPIKES,
    /** The Owl's M1, its Rend: one of its four cuts of the giant blades (detail: which, 0..3; {@code OwlRules}). */
    OWL_STRIKE,
    /** The Owl's J tapped, its Reaping Whirl: a whole turn with both giant arms out, reaping all round her ({@code OwlRules}). */
    OWL_WHIRL,
    /**
     * The Owl's M2, its Pounce: the bound and, as she lands, both blades driven into the ground ahead (detail: the way and whether it
     * soars, {@code OwlRules.leapDetail}; charge: the tick she landed, 0 in the air).
     */
    OWL_LEAP,
    /** The Owl's V held, its Fissure charging: reared up, the giant arms raised high (charge from {@code startedAt}). */
    OWL_FISSURE_CHARGE,
    /** Let go: both blades brought down on one place and the ground split from there on (charge as released; {@code OwlRules}). */
    OWL_FISSURE,
    /** 6.2.0, the Owl's B, its Watch This: hunched, then reared up as its lances burst out of her back ({@code OwlLances}). */
    OWL_WATCH,
    /**
     * 6.2.0, the Owl's N, its blade volley ({@code OwlShards}): the giant arms throwing turn about (detail: 1 held, a storm; charge:
     * the tick it was let go, 0 while held).
     */
    OWL_VOLLEY,
    /**
     * 6.2.0, Witness This (a mutation): a fully charged Big Punch that landed goes on: behind it, the uppercut, the midair blitz, the
     * kick down into a crater ({@code WitnessRules}; target: what she hit; point: where it stood; yaw: the way the punch went; charge:
     * the ground under it, against where it stood).
     */
    WITNESS,
    /**
     * 6.2.4, the Owl's N, its great hand: reaching for what she looks at (target: it; charge: the ticks till it is there; point: where
     * it reaches when there is nothing, {@code OwlRules}).
     */
    OWL_GRAB_REACH,
    /** Its great hand holds what it seized up before its jaws while N stays down, worrying at it (target: what it holds). */
    OWL_GRAB_HOLD,
    /** N let go: raised high and slammed down on the ground ahead (target: what it held). */
    OWL_GRAB_SLAM,
    /** The reach closed on nothing (point: where it clutched). */
    OWL_GRAB_MISS;

    public static EtoMove byOrdinal(int ordinal) {
        EtoMove[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : NONE;
    }

    /** 6.2.0: true for the Owl's own moves (her first kakuja's: its giant arms and her body are theirs). */
    public boolean owl() {
        return this.ordinal() >= OWL_PUNCH_CHARGE.ordinal() && this.ordinal() <= OWL_VOLLEY.ordinal() || this.grab();
    }

    /** 6.2.4: true for the Owl's great hand's moves (N in it). */
    public boolean grab() {
        return this == OWL_GRAB_REACH || this == OWL_GRAB_HOLD || this == OWL_GRAB_SLAM || this == OWL_GRAB_MISS;
    }

    /** True for the moves the pale flesh is out for (it erupts from the upper back, or grows round the leg). */
    public boolean flesh() {
        return this == SWIPE || this == GRAB_REACH || this == GRAB_HOLD || this == GRAB_THROW || this == GRAB_MISS || this == HAMMER_CHARGE
            || this == HAMMER;
    }
}
