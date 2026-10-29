package com.hex.kagune.control.ayato;

import com.hex.kagune.control.AyatoStage;
import com.hex.kagune.control.ComboStage;
import com.hex.kagune.control.ComboStyle;
import com.hex.kagune.control.StrikePattern;

/**
 * Ayato path numbers (pure, tested). Canon fixes what each ability does; these are the tuning values
 * (docs/AYATO_PATH.md, "The canon-only rule"). Damage sits on the same curve as the Kaneki path's swipe
 * ({@code 6 + 0.006 * mastery}), scaled by stage instead of forms and mutations.
 */
public final class AyatoRules {
    /** Mastery from which hardened wing blades can strike while crystallized (Wing Blades stage). */
    public static final int WING_BLADES_MASTERY = 250;

    private AyatoRules() {
    }

    public static AyatoStage stage(int mastery) {
        return AyatoStage.forMastery(mastery);
    }

    /**
     * Balance tier by mastery (0..5 at 0, 100, 250, 450, 750, 1000): the moves keep their tuning while the wing stages
     * change; the moves themselves are being redesigned.
     */
    static int tier(int mastery) {
        int[] thresholds = {100, 250, 450, 750, 1000};
        int tier = 0;
        for (int threshold : thresholds) {
            if (mastery >= threshold) {
                tier++;
            }
        }
        return tier;
    }

    /** Damage multiplier per tier on the shared swipe curve. */
    public static float stageMultiplier(int mastery) {
        return new float[] {1.0F, 1.05F, 1.2F, 1.35F, 1.6F, 1.8F}[tier(Math.max(0, mastery))];
    }

    /**
     * Base damage (before the combo stage's multiplier); every Ayato move derives from it. Brought down with Kaneki's in 3.0.3
     * ({@link com.hex.kagune.control.DamageBalance}).
     */
    public static float meleeDamage(int mastery) {
        int clamped = Math.max(0, Math.min(1000, mastery));
        return (6.0F + clamped * 0.006F) * stageMultiplier(clamped) * com.hex.kagune.control.DamageBalance.scale(clamped);
    }

    /** One small wing at first. */
    public static final float ONE_WING_LENGTH = 1.5F;
    /** The twin wings start small and grow steadily to full size at {@link AyatoStage#FULL_SIZE_MASTERY}. */
    public static final float SMALL_WINGS_LENGTH = 1.6F;
    public static final float FULL_WINGS_LENGTH = 4.3F;

    /** Length of one wing in blocks: one small wing, then two small wings growing until full size at mastery 500. */
    public static float wingLength(int mastery, WingForm form) {
        if (form == WingForm.ONE_WING) {
            return mastery >= 1000 ? 3.7F : 3.4F;
        }
        if (mastery < AyatoStage.TWIN_WINGS.requiredMastery()) {
            return ONE_WING_LENGTH;
        }
        return SMALL_WINGS_LENGTH + (FULL_WINGS_LENGTH - SMALL_WINGS_LENGTH) * AyatoStage.growth(mastery);
    }

    /** Reach of wing strikes in blocks (kicks use a fraction of it through their combo stage). */
    public static double reach(int mastery, WingForm form) {
        if (form == WingForm.ONE_WING) {
            return mastery >= 1000 ? 6.5 : 6.0;
        }
        return 1.9 + wingLength(mastery, form) * 0.8;
    }

    /** Shortest close-range reach: a kick (legs don't grow with the kagune), and never less than a normal melee hit. */
    public static final double KICK_REACH = 3.2;

    /** Reach of one close-range combo stage: kicks stay at leg length, wing strikes and bursts scale with the wings. */
    public static double strikeRange(int mastery, WingForm form, ComboStage stage) {
        if (stage.pattern() == StrikePattern.KICK) {
            return KICK_REACH;
        }
        if (stage.pattern().kakuja()) {
            return NEEDLE_RANGE;
        }
        if (stage.pattern().crystal()) {
            return crystalRange(mastery) + (stage.pattern() == StrikePattern.CRYSTAL_LANCE ? LANCE_EXTRA_RANGE : 0.0);
        }
        if (stage.pattern().flame()) {
            return flameRange(mastery);
        }
        return Math.max(KICK_REACH, Math.min(15.0, reach(mastery, form.usable(mastery)) * stage.rangeMul()));
    }

    /**
     * Ayato's low RC supply (canon): his RC costs start 60% above the Kaneki path's and fall to parity at mastery 1000
     * (:re Ayato fights long battles).
     */
    public static float rcCostMultiplier(int mastery) {
        int clamped = Math.max(0, Math.min(1000, mastery));
        return 1.6F - 0.6F * clamped / 1000.0F;
    }

    /**
     * Ayato's M1 for the current wing form: the fire shots ({@link ComboStyle#AYATO_FIRE}), or the crystal spikes while the
     * crystal mode is on ({@link ComboStyle#AYATO_CRYSTAL}); the :re One-Winged Kagune stays a blade; the first kakuja fires
     * needles ({@link ComboStyle#AYATO_KAKUJA}), the final kakuja needles and fire in turn ({@link ComboStyle#AYATO_FINAL_KAKUJA}).
     */
    public static ComboStyle style(int mastery, WingForm form, boolean crystallized, boolean crystalMode) {
        if (form.usable(mastery) == WingForm.ONE_WING) {
            return ComboStyle.AYATO_ONE_WING;
        }
        if (form.usable(mastery).finalKakuja()) {
            return ComboStyle.AYATO_FINAL_KAKUJA;
        }
        if (form.usable(mastery).kakuja()) {
            return ComboStyle.AYATO_KAKUJA;
        }
        return crystalMode && crystalModeAllowed(mastery, form) ? ComboStyle.AYATO_CRYSTAL : ComboStyle.AYATO_FIRE;
    }

    /** {@link #style(int, WingForm, boolean, boolean)} with the crystal mode off. */
    public static ComboStyle style(int mastery, WingForm form, boolean crystallized) {
        return style(mastery, form, crystallized, false);
    }

    // ------------------------------------------------------------------ Crystal mode (V)

    /** Mastery from which V crystallizes the wings into the crystal mode. */
    public static final int CRYSTAL_MASTERY = 200;
    /** Mastery from which Z throws a Wing Vortex. */
    public static final int VORTEX_MASTERY = 150;
    /** Mastery from which J fires the Gatling (in the crystal mode). */
    public static final int GATLING_MASTERY = 400;
    /** Mastery from which Z in the first kakuja is Rushing Death. */
    public static final int RUSH_MASTERY = 800;
    /** Mastery from which J in the first kakuja is the Needle Gatling. */
    public static final int NEEDLE_GATLING_MASTERY = 850;
    /** Ticks between two V presses that count (the wings need a moment to freeze or thaw). */
    public static final int CRYSTAL_TOGGLE_TICKS = 16;
    /** Ticks the crystal takes to climb from the shoulder blades to the very tips (and to thaw back). */
    public static final int CRYSTALLIZE_TICKS = 24;
    public static final int THAW_TICKS = 16;

    /**
     * True if V may crystallize these wings: from {@link #CRYSTAL_MASTERY}, twin wings or Rabbit Guise (the One-Wing is crystal
     * already, the kakuja is armored tentacles).
     */
    public static boolean crystalModeAllowed(int mastery, WingForm form) {
        return mastery >= CRYSTAL_MASTERY && form.usable(mastery).wings();
    }

    /** Ticks between two spikes: a beat slower than the fire (it trades the fire's pace for reach, bleeding and the burst). */
    public static int crystalCadence(int mastery) {
        return flameCadence(mastery) + 1;
    }

    /** Ticks between two M1 shots of either mode. */
    public static int shotCadence(StrikePattern pattern, int mastery) {
        if (pattern.finalKakuja()) {
            return finalCadence(pattern);
        }
        if (pattern.kakuja()) {
            return kakujaCadence(mastery);
        }
        return pattern.crystal() ? crystalCadence(mastery) : flameCadence(mastery);
    }

    // ------------------------------------------------------------------ The first kakuja's needles (K)

    /** Needles each firing tentacle sends in one M1 volley, and the ticks between them (they leave one after another). */
    public static final int VOLLEY_NEEDLES = 2;
    public static final int NEEDLE_STAGGER = 1;
    /** Scatter of a needle about its line to the target (degrees). */
    public static final float NEEDLE_SPREAD = 1.4F;

    /** Ticks between two kakuja volleys: much faster than the wings fire. */
    public static int kakujaCadence(int mastery) {
        return mastery >= 1000 ? 3 : 4;
    }

    /** Needle speed in blocks per tick: faster than any spike. */
    public static float needleSpeed(int mastery) {
        return mastery >= 1000 ? 4.6F : 4.2F;
    }

    /** How far a needle flies (blocks). */
    public static final double NEEDLE_RANGE = 32.0;
    /** A needle's hit radius (blocks): thin, but fast enough to still find what it was aimed at. */
    public static final float NEEDLE_RADIUS = 0.22F;
    /** Harrow: the cone (half angle, degrees), how far it carries and how hard each needle shoves. */
    public static final float HARROW_CONE = 18.0F;
    public static final double HARROW_RANGE = 12.0;
    public static final float HARROW_KNOCKBACK = 0.45F;
    /** Piercing Line: the needle's size, how many it runs through, how hard it shoves and how much further it flies. */
    public static final float LINE_SIZE = 2.6F;
    public static final int LINE_PIERCE = 5;
    public static final double LINE_EXTRA_RANGE = 8.0;
    /** Thorn Fan: needles, the fan's half angle (degrees) and the ticks it is swept over. */
    public static final int FAN_NEEDLES = 24;
    public static final float THORN_FAN_HALF_ANGLE = 60.0F;
    public static final int FAN_SWEEP_TICKS = 4;
    /** Hail of Thorns: needles, and the radius they rain over. */
    public static final int HAIL_NEEDLES = 30;
    public static final double HAIL_RADIUS = 3.5;
    /** Six-Tail Barrage: needles a tick, and in all. */
    public static final int SIX_TAIL_PER_TICK = 2;
    public static final int SIX_TAIL_NEEDLES = 56;
    /** The order the six tentacles take turns in a barrage: right and left alternating, top to bottom and back up. */
    public static final int[] SIX_TAIL_ORDER = {0, 3, 1, 4, 2, 5, 1, 4};

    /** The tentacle (KakujaRig index) the {@code shot}th needle of a barrage leaves. */
    public static int sixTailTentacle(int shot) {
        return SIX_TAIL_ORDER[Math.floorMod(shot, SIX_TAIL_ORDER.length)];
    }

    /**
     * Rushing Death (Z): the backflip's launch (blocks a tick back and up: about six blocks up and seven back), needles each
     * tentacle throws at the top, and the cooldown (ticks, from its start).
     */
    public static final double RUSH_BACK = 0.75;
    public static final double RUSH_UP = 0.95;
    public static final int RUSH_NEEDLES_PER_TENTACLE = 2;
    public static final int RUSH_COOLDOWN = 120;

    /** Crimson Bloom: needles in its ring. */
    public static final int BLOOM_RING_NEEDLES = 24;

    // ------------------------------------------------------------------ The final kakuja (K held, mastery 1000)

    /**
     * Ticks between two of the final kakuja's M1 shots: the needles keep the first kakuja's pace, the fire the wings' (a lash
     * and a fireball take a breath longer), the crossfire longest.
     */
    public static int finalCadence(StrikePattern pattern) {
        return switch (pattern) {
            case NEEDLE_PAIR -> 3;
            case FIRE_LASH, NEEDLE_FAN -> 4;
            default -> 5;
        };
    }

    /**
     * The ticks after the impact at which tentacle {@code index} (KakujaRig: right 0..2, fire tendrils 3..5) fires in one of the
     * final kakuja's M1 shots, as {@code server/AyatoPyre} sends them ({@code limbMask}: the stage's limbs, bits 20..25), so
     * every client kicks each tentacle as its needle or fire leaves. Empty when it doesn't fire.
     */
    public static int[] finalFireTicks(StrikePattern pattern, int index, int limbMask) {
        boolean right = index >= 0 && index < 3;
        boolean inMask = (limbMask & 1 << (20 + index)) != 0;
        switch (pattern) {
            case NEEDLE_PAIR -> {
                if (!right || !inMask) {
                    return new int[0];
                }
                int[] ticks = new int[VOLLEY_NEEDLES];
                for (int k = 0; k < ticks.length; k++) {
                    ticks[k] = k * NEEDLE_STAGGER;
                }
                return ticks;
            }
            case FIRE_LASH -> {
                if (right || !inMask) {
                    return new int[0];
                }
                int[] ticks = new int[LASH_SHARDS];
                for (int k = 0; k < ticks.length; k++) {
                    ticks[k] = k;
                }
                return ticks;
            }
            case NEEDLE_FAN -> {
                if (!right) {
                    return new int[0];
                }
                int[] ticks = new int[(NEEDLE_FAN_NEEDLES - index + 2) / 3];
                for (int k = index, n = 0; k < NEEDLE_FAN_NEEDLES; k += 3) {
                    ticks[n++] = k / 3;
                }
                return ticks;
            }
            case FIREBALL -> {
                return index == 3 || index == 4 ? new int[] {0} : new int[0];
            }
            case CROSSFIRE -> {
                if (!right) {
                    return index < 6 ? new int[] {index - 3} : new int[0];
                }
                int[] ticks = new int[CROSSFIRE_NEEDLES];
                for (int k = 0; k < ticks.length; k++) {
                    ticks[k] = index + k;
                }
                return ticks;
            }
            default -> {
                return new int[0];
            }
        }
    }

    /** Fire Lash: the shards one tendril whips across the target (one a tick) and how wide the sweep is (degrees each side). */
    public static final int LASH_SHARDS = 3;
    public static final float LASH_HALF_ANGLE = 9.0F;
    /** Needle Fan: needles from the three right tentacles, and the fan's half angle (degrees). */
    public static final int NEEDLE_FAN_NEEDLES = 9;
    public static final float NEEDLE_FAN_HALF_ANGLE = 18.0F;
    /** Fireball: its size against the Flare's, and the radius of its burst (blocks). */
    public static final float FIREBALL_SIZE = 1.6F;
    public static final double FIREBALL_SPLASH = 2.2;
    /** Crossfire: burning needles from each right tentacle; each fire tendril adds one shard. */
    public static final int CROSSFIRE_NEEDLES = 2;
    /** Seconds a burning needle (Crossfire, Searing Harrow) sets its target alight, and the Pyre Lance's. */
    public static final int BURNING_NEEDLE_SECONDS = 4;
    public static final int PYRE_LANCE_SECONDS = 6;
    /** The Pyre Lance's size against a needle (a little bigger than the Piercing Line's). */
    public static final float PYRE_LANCE_SIZE = 2.9F;
    /** Searing Harrow: burning needles from each right tentacle, fire shards from each tendril, the fire's reach (blocks). */
    public static final int SEARING_NEEDLES = 3;
    public static final int SEARING_SHARDS = 2;
    public static final double SEARING_FIRE_RANGE = 9.0;
    /** Twin Serpents: needles and fire shards in each fan, their half angles (degrees), the ticks both are swept over. */
    public static final int SERPENT_NEEDLES = 20;
    public static final int SERPENT_SHARDS = 9;
    public static final float SERPENT_NEEDLE_HALF_ANGLE = 60.0F;
    public static final float SERPENT_FIRE_HALF_ANGLE = 50.0F;
    public static final int SERPENT_SWEEP_TICKS = 6;
    /** Hellrain: needles and fireballs that come down (needles two a tick, a fireball every third tick) and the radius. */
    public static final int HELLRAIN_NEEDLES = 24;
    public static final int HELLRAIN_FIREBALLS = 8;
    public static final double HELLRAIN_RADIUS = 3.8;
    /** Ember Hop: how far the fire tendrils' downbeat throws him (blocks a tick back and up) and the burst where he stood. */
    public static final double EMBER_HOP_BACK = 1.05;
    public static final double EMBER_HOP_UP = 0.55;
    public static final double EMBER_HOP_RADIUS = 2.4;
    /** Ticks the ground stays burning after an Ember Hop or where a Pyre Lance ends. */
    public static final int EMBER_TICKS = 60;
    /** Crimson Pyre: the fire blast round each needle that bursts (blocks, and its share of the burst), and seconds alight. */
    public static final double PYRE_BLAST_RADIUS = 1.8;
    public static final float PYRE_BLAST_SHARE = 0.35F;
    public static final int PYRE_BURN_SECONDS = 6;
    /** Crimson Pyre: needles and fire shards in its ring. */
    public static final int PYRE_RING_NEEDLES = 12;
    public static final int PYRE_RING_SHARDS = 12;

    /**
     * Scorched wounds (the final kakuja's passive): a needle or a burst opening a body that is on fire bleeds it twice as hard
     * (twice the stacks), so a burning target's bleeding climbs to its heaviest twice as fast.
     */
    public static int scorchedStacks(int stacks, boolean burning, boolean finalKakuja) {
        return burning && finalKakuja ? stacks * 2 : stacks;
    }

    /** The final kakuja's J: needle salvos and fire shards take turns (even shots the right tentacles, odd the fire tendrils). */
    public static boolean pyreGatlingFire(int shot) {
        return Math.floorMod(shot, 2) == 1;
    }

    /** The tentacle (KakujaRig index) the {@code shot}th shot of the final kakuja's J leaves: right 0, 1, 2 and left 3, 4, 5 in turn. */
    public static int pyreGatlingTentacle(int shot) {
        int round = Math.floorMod(shot / 2, 3);
        return pyreGatlingFire(shot) ? 3 + round : round;
    }

    /** Damage of one fire shard of the final kakuja's J (one for every needle salvo, heavier than a needle). */
    public static float pyreGatlingFireDamage(int mastery) {
        return meleeDamage(mastery) * 0.14F;
    }

    /** Spike speed in blocks per tick: faster than the fire. */
    public static float crystalSpeed(int mastery) {
        return 2.6F + 0.7F * Math.max(0, Math.min(1000, mastery)) / 1000.0F;
    }

    /** How far a spike flies: much further than the fire. */
    public static double crystalRange(int mastery) {
        return mastery >= 500 ? 30.0 : 26.0;
    }

    /** The Crystal Lance flies this much further still. */
    public static final double LANCE_EXTRA_RANGE = 6.0;

    /** Spike hit radius in blocks (a thin crystal: a little tighter than the fire). */
    public static float crystalRadius(int mastery) {
        return mastery >= 500 ? 0.36F : 0.28F;
    }

    /** Targets a Crystal Lance runs through (it sticks in every one). */
    public static int lancePierce(int mastery) {
        return mastery >= 750 ? 4 : 3;
    }

    /** Ticks a spike stays stuck in a body before it bursts: fifteen seconds, at every mastery (the Bloom bursts them sooner). */
    public static final int STUCK_TICKS = 300;

    /** Ticks a spike stays stuck before it bursts ({@link #STUCK_TICKS}). */
    public static int burstDelay(int mastery) {
        return STUCK_TICKS;
    }

    /** Damage of a stuck spike bursting, from the damage of the hit that put it there. */
    public static float burstDamage(float hitDamage) {
        return 1.0F + 0.45F * Math.max(0.0F, hitDamage);
    }

    /** The Crystal Bloom bursts every stuck spike at once, this much harder. */
    public static final float BLOOM_MULTIPLIER = 1.6F;
    /** How far from him the Crystal Bloom reaches the spikes he has stuck (blocks). */
    public static final double BLOOM_RANGE = 40.0;

    /** Spikes one target can carry from one Ayato at a time (more still hit and bleed, they just don't stick). */
    public static final int MAX_STUCK_PER_TARGET = 10;
    /** Spikes one Ayato can have stuck in anything at once. */
    public static final int MAX_STUCK = 48;

    /** Bleeding stacks one target can build up (each spike that hits adds one). */
    public static final int MAX_BLEED_STACKS = 9;

    /** The original's bleeding amplifier for {@code stacks} stacks: heavier every second stack, at most III (amplifier 3). */
    public static int bleedAmplifier(int stacks) {
        return Math.max(0, Math.min(3, (Math.max(1, stacks) - 1) / 2));
    }

    /** How long the bleeding runs for {@code stacks} stacks (ticks), renewed by each new spike. */
    public static int bleedDuration(int stacks) {
        return Math.min(220, 60 + 20 * Math.max(1, stacks));
    }

    /** Spikes in a Spike Fan, and its half-angle in degrees (tighter than the fire's: it is aimed). */
    public static int spikeFanCount(int mastery) {
        return mastery >= 450 ? 9 : 7;
    }

    public static final float SPIKE_FAN_HALF_ANGLE = 24.0F;

    /** Spikes in a Spike Rain and the radius they come down over (blocks). */
    public static int rainSpikes(int mastery) {
        return mastery >= 750 ? 12 : mastery >= 450 ? 10 : 8;
    }

    public static final double RAIN_RADIUS = 2.6;
    /** How high above the target the rain is thrown from (blocks). */
    public static final double RAIN_HEIGHT = 9.0;

    /** Spikes in a Spike Barrage (one a tick, the wings alternating). */
    public static int barrageSpikes(int mastery) {
        return mastery >= 750 ? 16 : 12;
    }

    /** Half-angle (degrees) of the barrage's scatter around the aim. */
    public static final float BARRAGE_SPREAD = 4.0F;

    /** Radius of a bursting spike's explosion with the Bursting Shards mutation (blocks), and its damage share. */
    public static final double BURST_EXPLOSION_RADIUS = 2.2;
    public static final float BURST_EXPLOSION_SHARE = 0.8F;

    // ------------------------------------------------------------------ Right-click enders

    /** Shots in one M1 chain before it starts over. */
    public static final int CHAIN_SHOTS = 5;
    /** Ticks after a shot ends in which a right click still counts that chain (and in which the next shot continues it). */
    public static final int ENDER_WINDOW_TICKS = 24;
    /** Mastery each ender needs, by the shots before it: hop (0), Flare (1), Rising Flame (2), Fire Fan (3), Rabbit Dash (4), Black Rabbit (5). */
    private static final int[] ENDER_MASTERY = {0, 0, 100, 250, 350, 500};
    /**
     * Mastery each of the first kakuja's enders needs, by the volleys before it: Harrow (0), Piercing Line (1), Thorn Fan (2),
     * Hail of Thorns (3), Six-Tail Barrage (4), Crimson Bloom (5). The kakuja comes at 750 with the first two; the rest spread
     * over the way to 1000.
     */
    private static final int[] KAKUJA_ENDER_MASTERY = {750, 750, 800, 850, 900, 950};
    /** Cooldown (ticks, from its start) of each ender; the ones that need a chain are gated by the chain itself. */
    private static final int[] ENDER_COOLDOWN = {16, 0, 0, 0, 30, 60};

    /**
     * The ender a right click fires after {@code shots} M1 shots (0..5) at {@code mastery}: the one for that count if it is
     * unlocked, else the strongest unlocked one below it (a chain always ends in at least the Flare). No shots: the hop.
     */
    public static int ender(int shots, int mastery) {
        return ender(shots, mastery, false);
    }

    /** {@link #ender(int, int)}, from the first kakuja's set when {@code kakuja} (its own mastery table). */
    public static int ender(int shots, int mastery, boolean kakuja) {
        int[] needs = kakuja ? KAKUJA_ENDER_MASTERY : ENDER_MASTERY;
        int count = Math.max(0, Math.min(CHAIN_SHOTS, shots));
        if (count == 0) {
            return 0;
        }
        for (int ender = count; ender > 1; ender--) {
            if (mastery >= needs[ender]) {
                return ender;
            }
        }
        return 1;
    }

    public static int enderMastery(int ender) {
        return enderMastery(ender, false);
    }

    public static int enderMastery(int ender, boolean kakuja) {
        int[] needs = kakuja ? KAKUJA_ENDER_MASTERY : ENDER_MASTERY;
        return needs[Math.max(0, Math.min(needs.length - 1, ender))];
    }

    public static int enderCooldown(int ender) {
        return ENDER_COOLDOWN[Math.max(0, Math.min(ENDER_COOLDOWN.length - 1, ender))];
    }

    /** Fire shards in a Fire Fan, and its half-angle in degrees. */
    public static int fanShards(int mastery) {
        return mastery >= 450 ? 9 : 7;
    }

    public static final float FAN_HALF_ANGLE = 38.0F;

    /** Fire shards in the Black Rabbit's barrage (one a tick). */
    public static int barrageShards(int mastery) {
        return mastery >= 750 ? 14 : 10;
    }

    /** Radius of the Rising Flame pillar (blocks) and how hard it throws what it catches upward (blocks per tick). */
    public static final double PILLAR_RADIUS = 1.8;
    public static final double PILLAR_LAUNCH = 1.05;

    // ------------------------------------------------------------------ Fire shots

    /** Ticks between two bursts: fast from the start, faster with mastery. */
    public static int flameCadence(int mastery) {
        return mastery >= 450 ? 5 : mastery >= 100 ? 6 : 7;
    }

    /** Bolt speed in blocks per tick. */
    public static float flameSpeed(int mastery) {
        int m = Math.max(0, Math.min(1000, mastery));
        return 1.5F + 0.4F * Math.min(m, 500) / 500.0F + 0.2F * Math.max(0, m - 500) / 500.0F;
    }

    /** How far a bolt flies before it burns out. */
    public static double flameRange(int mastery) {
        return mastery >= 500 ? 18.0 : 14.0;
    }

    /** Bolt hit radius in blocks (bigger bolts with the full wings). */
    public static float flameRadius(int mastery) {
        return mastery >= 500 ? 0.5F : mastery >= 450 ? 0.38F : 0.3F;
    }

    /** Targets a burst passes through before it bursts (the first one doesn't count). */
    public static int flamePierce(int mastery) {
        return mastery >= 250 ? 1 : 0;
    }

    public static int flameBurnSeconds(int mastery) {
        return mastery >= 450 ? 4 : mastery >= 250 ? 3 : 2;
    }

    /** From 450 a burst's hit leaves burning embers on the ground for a moment. */
    public static boolean flameEmbers(int mastery) {
        return mastery >= 450;
    }

    // ------------------------------------------------------------------ Gatling (J, crystal mode only)

    /** Ticks both wings take to curl forward and spin up before the first spike. */
    public static final int GATLING_SPINUP = 6;
    /** Ticks they take to ease back after the trigger is let go. */
    public static final int GATLING_SPINDOWN = 6;
    /** The trigger counts as held this long after the client last said so (it says so every few ticks while J is down). */
    public static final int GATLING_KEEPALIVE = 12;
    /** Spikes a tick: from the first spike, and at full spin (reached over {@link #GATLING_RAMP} ticks). */
    public static final double GATLING_START_RATE = 0.2;
    public static final double GATLING_FULL_RATE = 0.6;
    public static final int GATLING_RAMP = 40;
    /** Half-angle (degrees) of the stream's scatter. */
    public static final float GATLING_SPREAD = 3.0F;
    /** Spikes of one Gatling in flight at once. */
    public static final int GATLING_MOST_IN_FLIGHT = 96;

    /** Spikes a tick after the trigger has been held {@code held} ticks: none while spinning up, then faster and faster. */
    public static double gatlingRate(long held) {
        if (held < GATLING_SPINUP) {
            return 0.0;
        }
        double t = Math.min(1.0, (held - GATLING_SPINUP) / (double)GATLING_RAMP);
        return GATLING_START_RATE + (GATLING_FULL_RATE - GATLING_START_RATE) * t * t * (3.0 - 2.0 * t);
    }

    /** RC drained a tick after {@code held} ticks of fire: it costs more the longer it runs (low-RC multiplier included). */
    public static float gatlingDrain(long held, int mastery) {
        return (0.22F + 0.005F * Math.max(0L, held)) * rcCostMultiplier(mastery);
    }

    /** Damage of one Gatling spike: light, it is the stream (and the bleeding and the bursts) that kill. */
    public static float gatlingDamage(int mastery) {
        // 3.0.3: a spike a tick or more at full spin; 0.2 of a melee hit each made it the strongest thing in the game.
        return meleeDamage(mastery) * 0.09F;
    }

    /** The kakuja's Needle Gatling: salvos a tick (each one tentacle firing two needles) from the first, and at full spin. */
    public static final double NEEDLE_GATLING_START_RATE = 0.5;
    public static final double NEEDLE_GATLING_FULL_RATE = 1.4;
    /** 6.0.0: it reaches full spin in a second (the wings' Gatling takes two): the barrage out-fires the M1 volleys by far. */
    public static final int NEEDLE_GATLING_RAMP = 20;
    /** Needles each tentacle fires in one salvo (both at once). */
    public static final int NEEDLE_GATLING_PER_SALVO = 2;

    /** Salvos a tick of the kakuja's Needle Gatling after {@code held} ticks (none while it spins up), ramping like the wings'. */
    public static double needleGatlingRate(long held) {
        if (held < GATLING_SPINUP) {
            return 0.0;
        }
        double t = Math.min(1.0, (held - GATLING_SPINUP) / (double)NEEDLE_GATLING_RAMP);
        return NEEDLE_GATLING_START_RATE + (NEEDLE_GATLING_FULL_RATE - NEEDLE_GATLING_START_RATE) * t * t * (3.0 - 2.0 * t);
    }

    /** RC drained a tick by the Needle Gatling: a fifth more than the wings' and, like theirs, more the longer it runs. */
    public static float needleGatlingDrain(long held, int mastery) {
        return gatlingDrain(held, mastery) * 1.2F;
    }

    /** Damage of one Needle Gatling needle: lighter still than a wing Gatling spike, there are far more of them. */
    public static float needleGatlingDamage(int mastery) {
        return meleeDamage(mastery) * 0.06F;
    }

    /** How far forward both wings are curled (0..1) at {@code time} for a Gatling that went on or off at {@code changed}. */
    public static float gatlingAim(double time, boolean on, long changed) {
        if (changed == Long.MIN_VALUE) {
            return 0.0F;
        }
        double since = time - changed;
        float t = (float)Math.max(0.0, Math.min(1.0, since / (on ? GATLING_SPINUP : GATLING_SPINDOWN)));
        float eased = t * t * (3.0F - 2.0F * t);
        return on ? eased : 1.0F - eased;
    }

    // ------------------------------------------------------------------ Wing Vortex (Z)

    /** How far the Wing Vortex flies (blocks). */
    public static final double VORTEX_RANGE = 25.0;
    /** Ticks the body coils and the wing twirls up before it is thrown. */
    public static final int VORTEX_WINDUP = 5;
    /** Ticks a new wing takes to grow back out of the shoulder blade once the thrown one has faded. */
    public static final int REGROW_TICKS = 16;
    /** Ticks the thrown wing takes to unwind and fade at the end of its flight. */
    public static final int VORTEX_FADE_TICKS = 6;
    /** How hard the vortex throws what it hits: along its flight (blocks per tick) and up. */
    public static final double VORTEX_KNOCKBACK = 2.1;
    public static final double VORTEX_LIFT = 0.55;

    /** Vortex speed in blocks per tick. */
    public static float vortexSpeed(int mastery) {
        return 1.5F + 0.3F * Math.max(0, Math.min(1000, mastery)) / 1000.0F;
    }

    /** Ticks the vortex takes to fly its whole range. */
    public static int vortexFlight(int mastery) {
        return (int)Math.ceil(VORTEX_RANGE / vortexSpeed(mastery));
    }

    /** Radius of the vortex (blocks): everything it passes this close to is caught. Wider with the full wings. */
    public static float vortexRadius(int mastery) {
        return mastery >= 500 ? 1.55F : 1.2F;
    }

    /** Damage to each body it catches. */
    public static float vortexDamage(int mastery) {
        return meleeDamage(mastery) * 1.5F;
    }

    public static int vortexBurnSeconds(int mastery) {
        return mastery >= 450 ? 6 : 5;
    }

    /** RC cost of one vortex (already includes the low-RC multiplier). */
    public static float vortexCost(int mastery) {
        return 10.0F * rcCostMultiplier(mastery);
    }

    /**
     * Ticks before the next vortex, counted from the key press: always longer than the thrown wing takes to grow back, so
     * he never throws a wing that isn't there.
     */
    public static int vortexCooldown(int mastery) {
        int cooldown = Math.round(100.0F - 30.0F * Math.max(0, Math.min(1000, mastery)) / 1000.0F);
        return Math.max(cooldown, vortexBusy(mastery));
    }

    /** 6.2.0: how long a throw takes, from the windup until the wing has grown back (the least wait, cooldowns off or on). */
    public static int vortexBusy(int mastery) {
        return VORTEX_WINDUP + vortexFlight(mastery) + VORTEX_FADE_TICKS + REGROW_TICKS + 4;
    }

    /**
     * Which wing is thrown (+1 right, -1 left): one wing alone (one small wing, Rabbit Guise) throws the left; twin wings a
     * random one ({@code random} in [0, 1)), never one that is still growing back.
     */
    public static int vortexSide(boolean twin, boolean rightGone, boolean leftGone, float random) {
        if (!twin) {
            return -1;
        }
        if (rightGone != leftGone) {
            return rightGone ? -1 : 1;
        }
        return random < 0.5F ? 1 : -1;
    }

    /**
     * What a thrown wing's side of the back looks like at {@code time} (ticks), for a vortex thrown at {@code launch} whose new
     * wing starts growing at {@code regrow}: {coil 0..1 (the wing twirling up before the throw), grow 0..1 (how much of a wing is
     * there: 1 before the throw, 0 once it has gone, growing back after {@code regrow})}.
     */
    public static float[] vortexWing(double time, long launch, long regrow) {
        if (launch == Long.MIN_VALUE) {
            return new float[] {0.0F, 1.0F};
        }
        double age = time - launch;
        if (age < -VORTEX_WINDUP) {
            return new float[] {0.0F, 1.0F};
        }
        if (age < 0.0) {
            float t = (float)((age + VORTEX_WINDUP) / VORTEX_WINDUP);
            return new float[] {t * t * (3.0F - 2.0F * t), 1.0F};
        }
        double growing = time - regrow;
        if (growing < 0.0) {
            return new float[] {0.0F, 0.0F};
        }
        float t = (float)Math.min(1.0, growing / REGROW_TICKS);
        return new float[] {0.0F, t * t * (3.0F - 2.0F * t)};
    }

    // ------------------------------------------------------------------ Scatter (the Spike Barrage)

    /**
     * A unit direction inside a cone of half-angle {@code spreadDegrees} around the aim ({@code ax, ay, az}),
     * uniform over the cone's cap for {@code r1, r2} uniform in [0, 1).
     */
    public static double[] spreadDirection(double ax, double ay, double az, float spreadDegrees, double r1, double r2) {
        double length = Math.sqrt(ax * ax + ay * ay + az * az);
        if (!(length > 1.0E-9) || !Double.isFinite(length)) {
            ax = 0.0;
            ay = 0.0;
            az = 1.0;
            length = 1.0;
        }
        ax /= length;
        ay /= length;
        az /= length;
        // Basis around the aim: u = aim x helper, v = aim x u.
        double hx = Math.abs(ay) < 0.95 ? 0.0 : 1.0;
        double hy = Math.abs(ay) < 0.95 ? 1.0 : 0.0;
        double ux = ay * 0.0 - az * hy;
        double uy = az * hx - ax * 0.0;
        double uz = ax * hy - ay * hx;
        double ul = Math.sqrt(ux * ux + uy * uy + uz * uz);
        ux /= ul;
        uy /= ul;
        uz /= ul;
        double vx = ay * uz - az * uy;
        double vy = az * ux - ax * uz;
        double vz = ax * uy - ay * ux;
        double spread = Math.toRadians(Math.max(0.0F, Math.min(80.0F, spreadDegrees)));
        double cos = 1.0 - Math.max(0.0, Math.min(1.0, r1)) * (1.0 - Math.cos(spread));
        double sin = Math.sqrt(Math.max(0.0, 1.0 - cos * cos));
        double phi = 2.0 * Math.PI * r2;
        double cu = Math.cos(phi) * sin;
        double cv = Math.sin(phi) * sin;
        return new double[] {ax * cos + ux * cu + vx * cv, ay * cos + uy * cu + vy * cv, az * cos + uz * cu + vz * cv};
    }

    // ------------------------------------------------------------------ Ukaku speed

    /** Speed amplifier while the wings are out (0 = Speed I). */
    public static int speedAmplifier(int mastery) {
        return mastery >= 450 ? 1 : 0;
    }

    /** The Rabbit Dash's place among the fire's enders (the one Fire Dash's forward dash uses). */
    public static final int RABBIT_DASH_ENDER = 4;

    /**
     * Agility Boost (mutation): Speed and Jump Boost levels added while the kagune is out. 3.0.3: Speed four levels up (V, VI from
     * mastery 450: a sprint more than twice as fast), at the user's word that it should be faster.
     */
    public static final int AGILITY_SPEED = 4;
    public static final int AGILITY_JUMP = 2;
    /** Fire Dash (mutation): the hop's push back and lift, against Rabbit Hop's own 0.95 and 0.5. */
    public static final double FIRE_DASH_BACK = 1.5;
    public static final double FIRE_DASH_LIFT = 0.62;
    /** Fire Dance (mutation): the Wing Vortex's hit radius, drawn size and damage, multiplied. */
    public static final double FIRE_DANCE_RADIUS = 1.7;
    public static final float FIRE_DANCE_SIZE = 1.6F;
    public static final float FIRE_DANCE_DAMAGE = 1.5F;
    /** Bursting Shards (mutation): a fire shard's explosion where it hits, its radius and share of the shard's damage. */
    public static final double SHARD_EXPLOSION_RADIUS = 1.8;
    public static final float SHARD_EXPLOSION_SHARE = 0.4F;

    /** Jump Boost amplifier while the wings are out (0 = Jump Boost I). */
    public static int jumpAmplifier(int mastery) {
        return mastery >= 750 ? 1 : 0;
    }
}
