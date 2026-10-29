package com.hex.kagune.control;

/** Visual shape a kagune limb traces during one combo stage. Server hit shapes are chosen to match. */
public enum StrikePattern {
    /** Horizontal lash from the limb's own side across the target. */
    LASH,
    /** Mirrored lash: from the far side back toward the limb's side. */
    BACKHAND,
    /** Rises high above the shoulders and crashes down onto the target. */
    OVERHEAD,
    /** Coils back and stabs straight through the target. */
    THRUST,
    /** Sweeps upward from near the ground through the target. */
    RISING,
    /** Circles the user at target height. */
    SPIN,
    /** Two limbs lash inward from opposite sides and cross at the target. */
    SCISSOR,
    /** Every participating limb spreads wide, then converges into one impaling thrust. */
    IMPALE,
    /** Ukaku: a fast kick (limb 8 = right leg, 9 = left leg). Canon: like Touka, he kicks. */
    KICK,
    /** Ukaku: one wing swings across the front as a blade (limb 0 = right wing, 1 = left wing). */
    WING_SLASH,
    /** Ukaku: both wing blades cut across each other. */
    WING_CROSS,
    /** Ukaku: both wings crystallize and fire a point-blank burst of RC shards. */
    SHARD_BURST,
    /** Ukaku (:re): the One-Winged Kagune sweeps across from his left to his right. */
    ONE_WING_SLASH,
    /** Ukaku (:re): the One-Winged Kagune sweeps back from right to left. */
    ONE_WING_RETURN,
    /** Ukaku (:re): the One-Winged Kagune rises high and cuts through at neck height (canon: it beheads). */
    BEHEADING,
    // ---- 5.0.19: the final kakuja's Steel Blade (C), on the right arm.
    /** The blade drawn back to his right, then swept wide across the front to his left at chest height. */
    STEEL_SWEEP,
    /** Back the other way: a backhand from his left out past his right. */
    STEEL_RETURN,
    /** Raised high over his head, then driven down across him on a slant with a lunge. */
    STEEL_EXECUTION,
    // ---- Ukaku's fire (limbs: 0 right wing, 1 left wing, 10 right hand, 11 left hand).
    /** M1: the hand thrusts, the wing on that side curls forward, aims and spits a shard of its own fire. */
    FLAME_SHOT,
    /** Ender after no shots (right click alone): one downbeat of the wings throws him back out of reach. */
    RABBIT_HOP,
    /** Ender after one shot: both palms thrust and the wings hurl one big fireball that bursts. */
    FLARE,
    /** Ender after two shots: a pillar of RC fire erupts under the target and throws it into the air. */
    RISING_FLAME,
    /** Ender after three shots: both wings sweep across and spray a fan of fire shards. */
    FIRE_FAN,
    /** Ender after four shots: he dashes straight through, wings streaming fire behind, burning everything he passes. */
    RABBIT_DASH,
    /** Ender after five shots: a backflip high into the air, then a hovering barrage from both wings (the Black Rabbit). */
    BLACK_RABBIT,
    // ---- Ukaku's crystal mode (V): the same hands and wings, firing crystal spikes that stick, bleed and burst.
    /** Crystal M1: the hand thrusts, the crystallized wing on that side curls forward and fires one spike. */
    CRYSTAL_SHOT,
    /** Crystal ender after one shot: both wings fire one long lance that runs through everything in its line. */
    CRYSTAL_LANCE,
    /** Crystal ender after two shots: a tight fan of spikes sprayed across the front. */
    SPIKE_FAN,
    /** Crystal ender after three shots: spikes thrown high that rain down on the target and around it. */
    SPIKE_RAIN,
    /** Crystal ender after four shots: both wings machine-gun spikes at where he is looking. */
    SPIKE_BARRAGE,
    /** Crystal ender after five shots: every spike he has stuck in anyone bursts at once, harder. */
    CRYSTAL_BLOOM,
    // ---- Ukaku's first kakuja (K): six armored tentacles firing needles from their spike rows (limbs 20..25: right upper,
    //      middle, lower, then the left's; 10 and 11 the hands).
    /** Kakuja M1: a pair of tentacles turns its spike rows on the target and fires a volley of needles from many spots. */
    NEEDLE_VOLLEY,
    /** Kakuja ender after no volleys: all six slam forward and blast a cone of needles point-blank, shoving hard. */
    HARROW,
    /** Kakuja ender after one volley: the six converge and fire one thick needle that runs through everything in its line. */
    PIERCING_LINE,
    /** Kakuja ender after two volleys: a fan of needles swept across the front, each side's tentacles covering their side. */
    THORN_FAN,
    /** Kakuja ender after three volleys: needles thrown high that rain down on the target and around it. */
    HAIL_OF_THORNS,
    /** Kakuja ender after four volleys: all six stream needles at where he is looking, two a tick, round the tentacles. */
    SIX_TAIL_BARRAGE,
    /** Kakuja ender after five volleys: every needle and spike he has stuck in anyone bursts at once; a ring of needles flies out. */
    CRIMSON_BLOOM,
    /** Kakuja Z: a backflip high into the air and well back; at the top all six snap forward and throw twelve needles at once. */
    RUSHING_DEATH,
    // ---- Ukaku's final kakuja (K held): the three armored tentacles on his right (limbs 20..22) fire needles that bleed, the
    //      three fire tendrils on his left (23..25) breathe RC fire that burns; 10 and 11 the hands. Its M1 alternates the sides.
    /** Final M1 1: two of the right tentacles fire a volley of needles each. */
    NEEDLE_PAIR,
    /** Final M1 2: a fire tendril whips across the target and lashes a sweep of fire. */
    FIRE_LASH,
    /** Final M1 3: all three right tentacles fan their needles across the target. */
    NEEDLE_FAN,
    /** Final M1 4: two fire tendrils knot together and hurl a fireball that bursts. */
    FIREBALL,
    /** Final M1 5: all six at once, needles wrapped in flame from the right and fire from the left, crossing on the target. */
    CROSSFIRE,
    /** Final ender after no shots: the fire tendrils beat the ground and throw him back, leaving it burning. */
    EMBER_HOP,
    /** Final ender after one shot: a point-blank cone of burning needles, the fire tendrils breathing into it. */
    SEARING_HARROW,
    /** Final ender after two: a needle fan and a fire fan sweeping across the front in opposite directions. */
    TWIN_SERPENTS,
    /** Final ender after three: one huge needle through everything in its line, trailing fire and leaving the ground burning. */
    PYRE_LANCE,
    /** Final ender after four: needles and fireballs thrown high, raining down together on the target and around it. */
    HELLRAIN,
    /** Final ender after five: every needle he has stuck in anyone bursts into flame, and a ring of needles and fire flies out. */
    CRIMSON_PYRE;

    /**
     * True for Ukaku's ranged stages, fire or crystal (the M1 shots and the enders): resolved by what they fire, not by a
     * close-range hit volume.
     */
    public boolean flame() {
        return this.compareTo(FLAME_SHOT) >= 0;
    }

    /** True for the crystal mode's stages (its shots and its enders). */
    public boolean crystal() {
        return this.compareTo(CRYSTAL_SHOT) >= 0;
    }

    /** True for the M1 shots of either mode (and the kakuja's volleys, and the final kakuja's five). */
    public boolean shot() {
        return this == FLAME_SHOT || this == CRYSTAL_SHOT || this == NEEDLE_VOLLEY || this.compareTo(NEEDLE_PAIR) >= 0 && this.compareTo(CROSSFIRE) <= 0;
    }

    /** True for the final kakuja's stages (its M1 and its enders). */
    public boolean finalKakuja() {
        return this.compareTo(NEEDLE_PAIR) >= 0;
    }

    /** True for either kakuja's stages (their M1 and their enders, Rushing Death). */
    public boolean kakuja() {
        return this.compareTo(NEEDLE_VOLLEY) >= 0;
    }

    /** True for the right-click enders. */
    public boolean ender() {
        return this.compareTo(RABBIT_HOP) >= 0 && !this.shot();
    }
}
