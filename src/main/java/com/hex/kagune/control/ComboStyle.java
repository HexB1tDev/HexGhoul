package com.hex.kagune.control;

import com.hex.kagune.gameplay.KaguneForm;

/**
 * Combat personality of each kagune form. Limb indices refer to {@link KaguneForm} layouts:
 * <ul>
 *   <li>Seed: one flexible tail - fast whip lashes ending in a long-range crack.</li>
 *   <li>Twin: two tails - alternating lashes ending in a scissor.</li>
 *   <li>Four-tail: upper tails jab, lower tails sweep low, then all four impale.</li>
 *   <li>(Blade: the Partial Kakuja's, kept for its enum slot; the form is gone since 3.0.2.)</li>
 *   <li>Centipede Form (750, the first kakuja): two heavy segmented legs - slow crushing overheads, scythes and a drill.</li>
 *   <li>Full Kakuja (1000, the final kakuja): blades cleave, tails flurry and crash, then the whole kagune devours forward.</li>
 * </ul>
 * Each style follows its form's limbs, not its place in the order ({@link KanekiForms}).
 */
public enum ComboStyle {
    WHIP(
        new ComboStage("Lash", StrikePattern.LASH, HitShape.SWEEP, 80.0F, 11, 0.36F, 1.0F, 1.0F, 1.0F, 3, 1.0F, new int[0], 0.15F),
        new ComboStage("Backlash", StrikePattern.BACKHAND, HitShape.SWEEP, 80.0F, 11, 0.36F, 1.0F, 1.0F, 1.0F, 3, 1.0F, new int[0], 0.15F),
        new ComboStage("Whip Crack", StrikePattern.OVERHEAD, HitShape.PIERCE, 0.9F, 14, 0.42F, 1.3F, 1.35F, 1.1F, 2, 1.25F, new int[0], 0.45F)
    ),
    SCISSOR(
        new ComboStage("Right Lash", StrikePattern.LASH, HitShape.SWEEP, 90.0F, 11, 0.36F, 1.0F, 1.0F, 1.0F, 3, 1.0F, new int[] {0}, 0.2F),
        new ComboStage("Left Lash", StrikePattern.LASH, HitShape.SWEEP, 90.0F, 11, 0.36F, 1.0F, 1.0F, 1.0F, 3, 1.0F, new int[] {1}, 0.2F),
        new ComboStage("Scissor", StrikePattern.SCISSOR, HitShape.SWEEP, 130.0F, 14, 0.42F, 1.3F, 1.25F, 1.0F, 4, 1.4F, new int[] {0, 1}, 0.5F)
    ),
    FLURRY(
        new ComboStage("Jab", StrikePattern.THRUST, HitShape.PIERCE, 1.0F, 10, 0.34F, 0.95F, 0.8F, 1.05F, 2, 1.0F, new int[] {0}, 0.15F),
        new ComboStage("Cross Jab", StrikePattern.THRUST, HitShape.PIERCE, 1.0F, 10, 0.34F, 0.95F, 0.8F, 1.05F, 2, 1.0F, new int[] {1}, 0.15F),
        new ComboStage("Low Sweep", StrikePattern.LASH, HitShape.SWEEP, 120.0F, 12, 0.38F, 1.05F, 1.1F, 0.95F, 4, 1.2F, new int[] {2, 3}, 0.35F),
        new ComboStage("Quad Impale", StrikePattern.IMPALE, HitShape.PIERCE, 1.25F, 15, 0.44F, 1.4F, 1.4F, 1.1F, 3, 1.6F, new int[] {0, 1, 2, 3}, 0.6F)
    ),
    HEAVY(
        new ComboStage("Crash", StrikePattern.OVERHEAD, HitShape.SLAM, 2.2F, 15, 0.45F, 1.2F, 1.3F, 0.95F, 5, 1.2F, new int[] {0}, 0.85F),
        new ComboStage("Scythe", StrikePattern.LASH, HitShape.SWEEP, 150.0F, 14, 0.42F, 1.05F, 1.2F, 1.0F, 5, 1.2F, new int[] {1}, 0.75F),
        new ComboStage("Drill", StrikePattern.IMPALE, HitShape.PIERCE, 1.2F, 17, 0.47F, 1.45F, 1.6F, 1.08F, 3, 1.5F, new int[] {0, 1}, 1.0F)
    ),
    BLADE(
        new ComboStage("Rising Slash", StrikePattern.RISING, HitShape.SWEEP, 100.0F, 11, 0.36F, 1.0F, 1.0F, 1.0F, 3, 1.0F, new int[] {0}, 0.35F),
        new ComboStage("Cross Slash", StrikePattern.SCISSOR, HitShape.SWEEP, 130.0F, 12, 0.38F, 1.15F, 1.1F, 1.0F, 4, 1.3F, new int[] {0, 1}, 0.5F),
        new ComboStage("Tail Pierce", StrikePattern.THRUST, HitShape.PIERCE, 1.0F, 11, 0.36F, 1.05F, 1.0F, 1.1F, 2, 1.0F, new int[] {2}, 0.3F),
        new ComboStage("Whirlwind", StrikePattern.SPIN, HitShape.SPIN, 0.5F, 16, 0.46F, 1.3F, 1.3F, 1.0F, 6, 1.6F, new int[] {0, 1, 2, 3}, 0.7F)
    ),
    PREDATOR(
        new ComboStage("Cleave", StrikePattern.SCISSOR, HitShape.SWEEP, 140.0F, 12, 0.38F, 1.1F, 1.15F, 1.0F, 4, 1.3F, new int[] {0, 1}, 0.55F),
        new ComboStage("Flurry", StrikePattern.THRUST, HitShape.PIERCE, 1.1F, 11, 0.36F, 1.05F, 0.9F, 1.05F, 3, 1.2F, new int[] {4, 5}, 0.3F),
        new ComboStage("Crash", StrikePattern.OVERHEAD, HitShape.SLAM, 2.6F, 14, 0.44F, 1.25F, 1.4F, 1.0F, 6, 1.4F, new int[] {2, 3}, 0.8F),
        new ComboStage("Devour", StrikePattern.IMPALE, HitShape.PIERCE, 1.4F, 16, 0.46F, 1.5F, 1.6F, 1.1F, 4, 1.8F, new int[] {2, 3, 4, 5}, 0.9F)
    ),
    // ---- Ayato path (docs/AYATO_PATH.md, "Close-range combo"). Limbs: 0 right wing, 1 left wing, 8 right leg, 9 left leg.
    //      Range multipliers apply to Ayato's wing reach (AyatoRules.reach), not the tendril attack range.
    /**
     * Twin wings below Wing Blades: kicks and a point-blank shard burst. Untrained, the wings don't strike up close: close
     * combat with them is what Ayato later trains for (canon C4, C5, C16).
     */
    AYATO_KICKS(
        new ComboStage("Kick", StrikePattern.KICK, HitShape.SWEEP, 70.0F, 10, 0.36F, 0.95F, 1.25F, 0.6F, 2, 0.8F, new int[] {8}, 0.3F),
        new ComboStage("Kick", StrikePattern.KICK, HitShape.SWEEP, 70.0F, 10, 0.36F, 0.95F, 1.25F, 0.6F, 2, 0.8F, new int[] {9}, 0.3F),
        new ComboStage("Point-blank Shard Burst", StrikePattern.SHARD_BURST, HitShape.PIERCE, 1.3F, 14, 0.42F, 1.35F, 1.0F, 0.85F, 4, 1.3F,
            new int[] {0, 1}, 0.6F)
    ),
    /** Right after a Shard Volley, below Wing Blades: the crystallized wings can't fight up close (canon C4), so only kicks. */
    AYATO_CRYSTAL_KICKS(
        new ComboStage("Kick", StrikePattern.KICK, HitShape.SWEEP, 70.0F, 10, 0.36F, 0.95F, 1.25F, 0.6F, 2, 0.8F, new int[] {8}, 0.3F),
        new ComboStage("Kick", StrikePattern.KICK, HitShape.SWEEP, 70.0F, 10, 0.36F, 0.95F, 1.25F, 0.6F, 2, 0.8F, new int[] {9}, 0.3F)
    ),
    /** Wing Blades and later: hardened wings strike like blades between kicks (canon C5, C17). */
    AYATO_BLADES(
        new ComboStage("Right Wing Blade", StrikePattern.WING_SLASH, HitShape.SWEEP, 120.0F, 11, 0.38F, 1.05F, 1.0F, 1.0F, 3, 1.0F,
            new int[] {0}, 0.4F),
        new ComboStage("Left Wing Blade", StrikePattern.WING_SLASH, HitShape.SWEEP, 120.0F, 11, 0.38F, 1.05F, 1.0F, 1.0F, 3, 1.0F,
            new int[] {1}, 0.4F),
        new ComboStage("Kick", StrikePattern.KICK, HitShape.SWEEP, 70.0F, 10, 0.36F, 0.95F, 1.25F, 0.6F, 2, 0.8F, new int[] {8}, 0.3F),
        new ComboStage("Cross Cut", StrikePattern.WING_CROSS, HitShape.SWEEP, 150.0F, 14, 0.42F, 1.45F, 1.3F, 1.0F, 4, 1.4F, new int[] {0, 1},
            0.55F)
    ),
    /** Rabbit Guise below Wing Blades: one (left) wing like Touka's, so the burst comes from one side (canon C9, C4, C16). */
    AYATO_RABBIT(
        new ComboStage("Kick", StrikePattern.KICK, HitShape.SWEEP, 70.0F, 10, 0.36F, 0.95F, 1.25F, 0.6F, 2, 0.8F, new int[] {8}, 0.3F),
        new ComboStage("Kick", StrikePattern.KICK, HitShape.SWEEP, 70.0F, 10, 0.36F, 0.95F, 1.25F, 0.6F, 2, 0.8F, new int[] {9}, 0.3F),
        new ComboStage("Point-blank Shard Burst", StrikePattern.SHARD_BURST, HitShape.PIERCE, 1.1F, 13, 0.42F, 1.3F, 1.0F, 0.85F, 3, 1.2F,
            new int[] {1}, 0.55F)
    ),
    /** :re One-Winged Kagune: wide sweeps ending in the beheading cut (canon C10, C15). */
    AYATO_ONE_WING(
        new ComboStage("One-Wing Slash", StrikePattern.ONE_WING_SLASH, HitShape.SWEEP, 160.0F, 13, 0.42F, 1.1F, 1.3F, 1.0F, 5, 1.2F,
            new int[] {1}, 0.6F),
        new ComboStage("Return Slash", StrikePattern.ONE_WING_RETURN, HitShape.SWEEP, 160.0F, 12, 0.4F, 1.0F, 1.2F, 1.0F, 5, 1.1F, new int[] {1},
            0.55F),
        new ComboStage("Beheading Cut", StrikePattern.BEHEADING, HitShape.SWEEP, 200.0F, 16, 0.46F, 1.4F, 1.5F, 1.05F, 6, 1.6F, new int[] {1},
            0.85F)
    ),
    /** Rabbit Guise with Wing Blades: the one hardened wing cuts between kicks, the way Touka's does (canon C9, C5, C16). */
    AYATO_RABBIT_BLADES(
        new ComboStage("Kick", StrikePattern.KICK, HitShape.SWEEP, 70.0F, 10, 0.36F, 0.95F, 1.25F, 0.6F, 2, 0.8F, new int[] {8}, 0.3F),
        new ComboStage("One-Wing Slash", StrikePattern.WING_SLASH, HitShape.SWEEP, 110.0F, 11, 0.38F, 1.05F, 1.0F, 1.0F, 3, 1.0F, new int[] {1},
            0.4F),
        new ComboStage("Point-blank Shard Burst", StrikePattern.SHARD_BURST, HitShape.PIERCE, 1.1F, 13, 0.42F, 1.3F, 1.0F, 0.85F, 3, 1.2F,
            new int[] {1}, 0.55F)
    ),
    /**
     * Ayato's M1 (left click): up to five fire shots, right wing then left wing (one small wing or Rabbit Guise answers both
     * hands). The shot count so far picks the right-click ender ({@link #AYATO_ENDERS}).
     */
    AYATO_FIRE(
        shot(0), shot(1), shot(2), shot(3), shot(4)
    ),
    /** The right-click enders, indexed by how many shots came before (0 to 5); see {@code AyatoRules.ender}. */
    AYATO_ENDERS(
        new ComboStage("Rabbit Hop", StrikePattern.RABBIT_HOP, HitShape.SINGLE, 0.0F, 12, 0.2F, 0.0F, 0.0F, 1.0F, 1, 0.5F, new int[] {0, 1},
            0.3F),
        new ComboStage("Flare", StrikePattern.FLARE, HitShape.SINGLE, 0.0F, 14, 0.45F, 1.8F, 0.9F, 1.0F, 1, 1.2F, new int[] {0, 1, 10, 11},
            0.6F),
        new ComboStage("Rising Flame", StrikePattern.RISING_FLAME, HitShape.SINGLE, 0.0F, 16, 0.45F, 1.5F, 0.0F, 1.0F, 1, 1.4F,
            new int[] {0, 1, 10, 11}, 0.7F),
        new ComboStage("Fire Fan", StrikePattern.FIRE_FAN, HitShape.SINGLE, 0.0F, 16, 0.42F, 0.8F, 0.5F, 1.0F, 1, 1.5F, new int[] {0, 1, 10},
            0.6F),
        new ComboStage("Rabbit Dash", StrikePattern.RABBIT_DASH, HitShape.SINGLE, 0.0F, 14, 0.15F, 1.6F, 1.2F, 1.0F, 1, 1.6F, new int[] {0, 1},
            0.8F),
        new ComboStage("Black Rabbit", StrikePattern.BLACK_RABBIT, HitShape.SINGLE, 0.0F, 34, 0.1F, 0.7F, 0.3F, 1.0F, 1, 2.4F,
            new int[] {0, 1, 10, 11}, 0.9F)
    ),
    /**
     * Crystal mode's M1 (V crystallizes the wings): up to five crystal spikes, right wing then left, the same hands as the fire.
     * A spike flies further than fire, sticks in what it hits, makes it bleed and bursts a few seconds later. The shot count
     * picks the right-click ender ({@link #AYATO_CRYSTAL_ENDERS}).
     */
    AYATO_CRYSTAL(
        spike(0), spike(1), spike(2), spike(3), spike(4)
    ),
    /**
     * The first kakuja's M1 (K transforms): five volleys of needles, each from a pair of tentacles (the upper pair, the middle
     * pair, the lower pair, then the upper and middle again), the hands thrusting in turn. Fast and from many spots at once:
     * each needle is light, the volume does the work.
     */
    AYATO_KAKUJA(
        volley(0), volley(1), volley(2), volley(3), volley(4)
    ),
    /** The first kakuja's right-click enders, indexed by how many volleys came before (0 to 5); see {@code AyatoRules.ender}. */
    AYATO_KAKUJA_ENDERS(
        new ComboStage("Harrow", StrikePattern.HARROW, HitShape.SINGLE, 0.0F, 12, 0.35F, 0.12F, 1.0F, 1.0F, 1, 0.6F, kakujaAll(), 0.5F),
        new ComboStage("Piercing Line", StrikePattern.PIERCING_LINE, HitShape.SINGLE, 0.0F, 14, 0.45F, 1.4F, 0.8F, 1.0F, 1, 1.2F, kakujaAll(), 0.6F),
        new ComboStage("Thorn Fan", StrikePattern.THORN_FAN, HitShape.SINGLE, 0.0F, 16, 0.4F, 0.1F, 0.3F, 1.0F, 1, 1.4F, kakujaAll(), 0.6F),
        new ComboStage("Hail of Thorns", StrikePattern.HAIL_OF_THORNS, HitShape.SINGLE, 0.0F, 18, 0.4F, 0.14F, 0.0F, 1.0F, 1, 1.5F, kakujaAll(), 0.7F),
        new ComboStage("Six-Tail Barrage", StrikePattern.SIX_TAIL_BARRAGE, HitShape.SINGLE, 0.0F, 34, 0.15F, 0.07F, 0.1F, 1.0F, 1, 1.8F, kakujaAll(),
            0.8F),
        new ComboStage("Crimson Bloom", StrikePattern.CRIMSON_BLOOM, HitShape.SINGLE, 0.0F, 16, 0.5F, 1.0F, 0.0F, 1.0F, 1, 2.2F, kakujaAll(), 0.9F)
    ),
    /** The first kakuja's Z: Rushing Death, one move of its own (not part of a chain). */
    AYATO_RUSHING_DEATH(
        new ComboStage("Rushing Death", StrikePattern.RUSHING_DEATH, HitShape.SINGLE, 0.0F, 28, 0.42F, 0.35F, 0.3F, 1.0F, 1, 2.5F, kakujaAll(), 0.9F)
    ),
    /** Crystal mode's right-click enders, indexed by how many spikes came before (0 to 5); see {@code AyatoRules.ender}. */
    AYATO_CRYSTAL_ENDERS(
        new ComboStage("Rabbit Hop", StrikePattern.RABBIT_HOP, HitShape.SINGLE, 0.0F, 12, 0.2F, 0.0F, 0.0F, 1.0F, 1, 0.5F, new int[] {0, 1},
            0.3F),
        new ComboStage("Crystal Lance", StrikePattern.CRYSTAL_LANCE, HitShape.SINGLE, 0.0F, 14, 0.45F, 1.6F, 0.6F, 1.0F, 1, 1.2F,
            new int[] {0, 1, 10, 11}, 0.6F),
        new ComboStage("Spike Fan", StrikePattern.SPIKE_FAN, HitShape.SINGLE, 0.0F, 16, 0.42F, 0.7F, 0.3F, 1.0F, 1, 1.4F, new int[] {0, 1, 10},
            0.6F),
        new ComboStage("Spike Rain", StrikePattern.SPIKE_RAIN, HitShape.SINGLE, 0.0F, 18, 0.45F, 0.65F, 0.0F, 1.0F, 1, 1.5F,
            new int[] {0, 1, 10, 11}, 0.7F),
        new ComboStage("Spike Barrage", StrikePattern.SPIKE_BARRAGE, HitShape.SINGLE, 0.0F, 30, 0.2F, 0.45F, 0.1F, 1.0F, 1, 1.7F,
            new int[] {0, 1, 10, 11}, 0.8F),
        new ComboStage("Crystal Bloom", StrikePattern.CRYSTAL_BLOOM, HitShape.SINGLE, 0.0F, 16, 0.5F, 1.0F, 0.0F, 1.0F, 1, 2.2F,
            new int[] {0, 1, 10, 11}, 0.9F)
    ),
    /**
     * The final kakuja's M1 (K held): the sides take turns, needles from the three armored tentacles on his right (they bleed),
     * fire from the three fire tendrils on his left (it burns), then both at once. The count picks the ender
     * ({@link #AYATO_FINAL_ENDERS}).
     */
    AYATO_FINAL_KAKUJA(
        new ComboStage("Needle Pair", StrikePattern.NEEDLE_PAIR, HitShape.SINGLE, 0.0F, 6, 0.3F, 0.045F, 0.1F, 1.0F, 1, 0.35F, new int[] {20, 21, 10},
            0.2F),
        new ComboStage("Fire Lash", StrikePattern.FIRE_LASH, HitShape.SINGLE, 0.0F, 8, 0.35F, 0.11F, 0.3F, 1.0F, 1, 0.4F, new int[] {23, 11}, 0.3F),
        new ComboStage("Needle Fan", StrikePattern.NEEDLE_FAN, HitShape.SINGLE, 0.0F, 7, 0.32F, 0.045F, 0.15F, 1.0F, 1, 0.45F,
            new int[] {20, 21, 22, 10}, 0.3F),
        new ComboStage("Fireball", StrikePattern.FIREBALL, HitShape.SINGLE, 0.0F, 9, 0.4F, 0.36F, 0.6F, 1.0F, 1, 0.55F, new int[] {23, 24, 11}, 0.4F),
        new ComboStage("Crossfire", StrikePattern.CROSSFIRE, HitShape.SINGLE, 0.0F, 10, 0.4F, 0.055F, 0.3F, 1.0F, 1, 0.7F, kakujaAll(), 0.5F)
    ),
    /** The final kakuja's right-click enders, indexed by how many shots came before (0 to 5); see {@code AyatoRules.ender}. */
    AYATO_FINAL_ENDERS(
        new ComboStage("Ember Hop", StrikePattern.EMBER_HOP, HitShape.SINGLE, 0.0F, 12, 0.2F, 0.4F, 0.0F, 1.0F, 1, 0.5F, new int[] {23, 24, 25},
            0.3F),
        new ComboStage("Searing Harrow", StrikePattern.SEARING_HARROW, HitShape.SINGLE, 0.0F, 12, 0.35F, 0.12F, 1.0F, 1.0F, 1, 0.7F, kakujaAll(),
            0.5F),
        new ComboStage("Twin Serpents", StrikePattern.TWIN_SERPENTS, HitShape.SINGLE, 0.0F, 18, 0.35F, 0.1F, 0.3F, 1.0F, 1, 1.4F, kakujaAll(), 0.6F),
        new ComboStage("Pyre Lance", StrikePattern.PYRE_LANCE, HitShape.SINGLE, 0.0F, 14, 0.45F, 1.5F, 0.8F, 1.0F, 1, 1.3F, kakujaAll(), 0.7F),
        new ComboStage("Hellrain", StrikePattern.HELLRAIN, HitShape.SINGLE, 0.0F, 22, 0.4F, 0.16F, 0.0F, 1.0F, 1, 1.6F, kakujaAll(), 0.75F),
        new ComboStage("Crimson Pyre", StrikePattern.CRIMSON_PYRE, HitShape.SINGLE, 0.0F, 16, 0.5F, 1.0F, 0.0F, 1.0F, 1, 2.3F, kakujaAll(), 0.9F)
    ),
    /**
     * 5.0.18: the final kakuja's Steel Blade (C): the steel wings closed round his right arm into one massive blade, swung wide
     * and fast (its reach is the blade's: {@code SteelBlade.REACH}); its own sword swings since 5.0.19.
     */
    AYATO_STEEL_BLADE(
        new ComboStage("Steel Sweep", StrikePattern.STEEL_SWEEP, HitShape.SWEEP, 210.0F, 9, 0.4F, 1.05F, 1.3F, 1.0F, 6, 1.0F, new int[] {0}, 0.6F),
        new ComboStage("Steel Return", StrikePattern.STEEL_RETURN, HitShape.SWEEP, 210.0F, 8, 0.4F, 1.0F, 1.2F, 1.0F, 6, 1.0F, new int[] {0}, 0.55F),
        new ComboStage("Steel Execution", StrikePattern.STEEL_EXECUTION, HitShape.SWEEP, 240.0F, 12, 0.45F, 1.45F, 1.6F, 1.0F, 8, 1.4F, new int[] {0}, 0.9F)
    );

    private final ComboStage[] stages;

    ComboStyle(ComboStage... stages) {
        this.stages = stages;
    }

    /** M1 shot {@code index}: even shots from the right wing and hand, odd ones from the left. */
    private static ComboStage shot(int index) {
        int[] limbs = index % 2 == 0 ? new int[] {0, 10} : new int[] {1, 11};
        return new ComboStage("Flame Shot", StrikePattern.FLAME_SHOT, HitShape.SINGLE, 0.0F, 8, 0.38F, 0.38F, 0.3F, 1.0F, 1, 0.45F, limbs, 0.25F);
    }

    /** Crystal M1 spike {@code index}: even spikes from the right wing and hand, odd ones from the left. */
    private static ComboStage spike(int index) {
        int[] limbs = index % 2 == 0 ? new int[] {0, 10} : new int[] {1, 11};
        return new ComboStage("Crystal Spike", StrikePattern.CRYSTAL_SHOT, HitShape.SINGLE, 0.0F, 8, 0.38F, 0.38F, 0.2F, 1.0F, 1, 0.5F, limbs, 0.25F);
    }

    /** Kakuja M1 volley {@code index}: a pair of tentacles (upper, middle, lower, upper, middle) and a hand in turn. */
    private static ComboStage volley(int index) {
        int tier = index % 3;
        int[] limbs = {20 + tier, 23 + tier, index % 2 == 0 ? 10 : 11};
        return new ComboStage("Needle Volley", StrikePattern.NEEDLE_VOLLEY, HitShape.SINGLE, 0.0F, 6, 0.3F, 0.045F, 0.1F, 1.0F, 1, 0.35F, limbs, 0.2F);
    }

    /** Every tentacle of the kakuja (limbs 20..25) and both hands. */
    private static int[] kakujaAll() {
        return new int[] {20, 21, 22, 23, 24, 25, 10, 11};
    }

    /** True for Ayato's M1 chains of shots (fire, crystal spikes, the kakuja's needles): a right click after them fires their ender. */
    public boolean ayatoShots() {
        return this == AYATO_FIRE || this == AYATO_CRYSTAL || this == AYATO_KAKUJA || this == AYATO_FINAL_KAKUJA;
    }

    /** True for either kakuja's M1 (their enders have their own mastery table, and no forward dash). */
    public boolean kakujaShots() {
        return this == AYATO_KAKUJA || this == AYATO_FINAL_KAKUJA;
    }

    /** The right-click enders that end this chain of shots (the fire's for anything else). */
    public ComboStyle ayatoEnders() {
        if (this == AYATO_KAKUJA || this == AYATO_KAKUJA_ENDERS) {
            return AYATO_KAKUJA_ENDERS;
        }
        if (this == AYATO_FINAL_KAKUJA || this == AYATO_FINAL_ENDERS) {
            return AYATO_FINAL_ENDERS;
        }
        return this == AYATO_CRYSTAL || this == AYATO_CRYSTAL_ENDERS ? AYATO_CRYSTAL_ENDERS : AYATO_ENDERS;
    }

    /** True for any set of right-click enders. */
    public boolean ayatoEnderSet() {
        return this == AYATO_ENDERS || this == AYATO_CRYSTAL_ENDERS || this == AYATO_KAKUJA_ENDERS || this == AYATO_FINAL_ENDERS;
    }

    public int stageCount() {
        return this.stages.length;
    }

    public ComboStage stage(int index) {
        return this.stages[Math.floorMod(index, this.stages.length)];
    }

    public static ComboStyle forForm(KaguneForm form) {
        if (form == null) {
            return WHIP;
        }
        return switch (form) {
            case RINKAKU_SEED -> WHIP;
            case RINKAKU_DUAL -> SCISSOR;
            case RINKAKU_QUAD -> FLURRY;
            case PARTIAL_KAKUJA -> BLADE;
            case CENTIPEDE -> HEAVY;
            case FULL_KAKUJA -> PREDATOR;
        };
    }

    /** True for the Ayato path's styles (wings and kicks instead of tendrils). */
    public boolean ayato() {
        return this.compareTo(AYATO_KICKS) >= 0;
    }

    public static ComboStyle byOrdinal(int ordinal) {
        ComboStyle[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : WHIP;
    }

    /**
     * Limbs that strike for a stage given how many tails are manifested (tails can be cycled down).
     * Falls back to {@code fallbackLimb} when none of the stage's limbs are out, or returns 0 if it is negative.
     */
    public static int limbMask(ComboStage stage, int manifested, int fallbackLimb) {
        int mask = 0;
        for (int limb : stage.limbs()) {
            if (limb >= 0 && limb < manifested && limb < 31) {
                mask |= 1 << limb;
            }
        }
        if (mask == 0 && fallbackLimb >= 0) {
            mask = 1 << Math.min(30, fallbackLimb);
        }
        return mask;
    }

    /**
     * Ticks after a strike starts before the next stage may begin. Chained impacts therefore always land at least
     * 10 ticks apart, outside vanilla hurt immunity, so fast chains never waste (or stack) damage on one target.
     */
    public static int chainOpenOffset(int impactTick, int nextImpactTick) {
        return impactTick + Math.max(2, 10 - nextImpactTick);
    }

    public static int firstLimb(int mask) {
        return mask == 0 ? 0 : Integer.numberOfTrailingZeros(mask);
    }
}
