package com.hex.kagune.control.king;

/**
 * 1.0.15: Peace's numbers (pure): its six abilities, on the same six keys as War's, and what each does. 1.0.34: all six rebuilt (the
 * user's design):
 * <ul>
 *   <li>Z Crossblade Waltz: at a body within {@link #WALTZ_RANGE}, the blades one at a time, each driven through it and left there;
 *       all in, they tear out of it together (blood thrown everywhere), and go home.</li>
 *   <li>J Aegis of :Re: three shield tendrils out of his back for {@link #AEGIS_TICKS} (one over his upper body, one each side) that
 *       take every harm for him, swinging to meet it; shots are sent back at who shot them; what crowds him is shoved off (unhurt);
 *       they push him off lava, haul him out of it, and catch him before a fall can hurt him.</li>
 *   <li>G White Wing Ascent: two tendrils out of his back down to the ground lift him {@link #ASCENT_HEIGHT} up; G again and they
 *       wrench him down into a landing that tears red flesh tendrils up out of the ground all round ({@link #ERUPT_RADIUS}), which
 *       lash at what is hostile for a while; left alone, they let him down gently.</li>
 *   <li>B Piercing Salvation (held): the blades together before him into one great lance, longer the longer it is held; let go it is
 *       driven out, through the first body it meets, carrying it on up to {@link #PIERCE_DRAG}, pinning it to whatever wall it meets
 *       there for {@link #PIERCE_PIN}; B again calls it back.</li>
 *   <li>C Rebirth Instinct (held): the blades driven down into the ground; let go, two to four creatures of his flesh climb up out of
 *       it before him (four at {@link #REBIRTH_CHARGE}), follow him, cut down what is hostile, and sink back into the ground after
 *       {@link #THRALL_LIFE}.</li>
 *   <li>V The Final Horizon: on a body in a blink, a blade's uppercut throwing it into the sky, after it; the blades into it one at a
 *       time from all round, left in it; the last strikes it down; on the ground the blades in it drive it on down through the ground,
 *       layer after layer, before they let it go.</li>
 * </ul>
 */
public final class PeaceRules {
    private PeaceRules() {
    }

    /** The six, in the order of the keys (Z, J, G, B, C, V), with their cooldowns (ticks). */
    public enum Ability {
        WALTZ("Crossblade Waltz", "Z", 220),
        AEGIS("Aegis of :Re", "J", 700),
        ASCENT("White Wing Ascent", "G", 440),
        PIERCE("Piercing Salvation", "B", 300),
        REBIRTH("Rebirth Instinct", "C", 900),
        HORIZON("The Final Horizon", "V", 1800);

        private final String displayName;
        private final String key;
        private final int cooldown;

        Ability(String displayName, String key, int cooldown) {
            this.displayName = displayName;
            this.key = key;
            this.cooldown = cooldown;
        }

        public String displayName() {
            return this.displayName;
        }

        public String key() {
            return this.key;
        }

        public int cooldown() {
            return this.cooldown;
        }

        public static Ability bySlot(int slot) {
            Ability[] all = values();
            return slot >= 0 && slot < all.length ? all[slot] : null;
        }
    }

    // ------------------------------------------------------------------ Crossblade Waltz

    /** How far off its body may be (blocks); when the first blade is sent, every how many ticks the next, how long each takes to it. */
    public static final double WALTZ_RANGE = 10.0;
    public static final int WALTZ_FIRST = 4;
    public static final int WALTZ_EVERY = 5;
    public static final int WALTZ_REACH = 3;
    /** The order the blades go in: from one side, then the other, the great one last. */
    public static final int[] WALTZ_ORDER = {PeaceMotion.UR, PeaceMotion.LL, PeaceMotion.UL, PeaceMotion.LR, PeaceMotion.ML, PeaceMotion.MR};
    /** All in by this; a beat, then they tear out together; home by the end. */
    public static final int WALTZ_IN = WALTZ_FIRST + (PeaceMotion.BLADES - 1) * WALTZ_EVERY + WALTZ_REACH;
    public static final int WALTZ_RIP = WALTZ_IN + 6;
    public static final int WALTZ_TICKS = WALTZ_RIP + 14;
    /** Each blade's thrust, the tearing out, how long it bleeds after. */
    public static final float WALTZ_STAB = 5.0F;
    public static final float WALTZ_TEAR = 20.0F;
    public static final int WALTZ_BLEED = 140;

    /** When blade {@code blade} is sent in the Waltz (ticks in), or -1. */
    public static int waltzSent(int blade) {
        for (int i = 0; i < WALTZ_ORDER.length; i++) {
            if (WALTZ_ORDER[i] == blade) {
                return WALTZ_FIRST + i * WALTZ_EVERY;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------ Aegis of :Re

    /** How long the three shields stay (ticks); how far round him what crowds him is shoved off, how often, how hard. */
    public static final int AEGIS_TICKS = 300;
    public static final double AEGIS_SHOVE_RADIUS = 3.0;
    public static final int AEGIS_SHOVE_EVERY = 4;
    public static final double AEGIS_SHOVE = 0.7;
    /** Lava this near (blocks) is pushed off from; a fall this long (blocks) and this fast (blocks a tick) is caught, this near the ground. */
    public static final double AEGIS_LAVA_NEAR = 1.6;
    public static final double AEGIS_FALL = 4.0;
    public static final double AEGIS_FALL_SPEED = 0.55;
    public static final double AEGIS_CATCH = 5.0;
    /** How long a shield takes to swing to meet a blow, and stays there (ticks). */
    public static final int SHIELD_MEET = 3;
    public static final int SHIELD_HOLD = 10;
    /** The three: over his upper body, his left, his right. */
    public static final int SHIELD_TOP = 0;
    public static final int SHIELD_LEFT = 1;
    public static final int SHIELD_RIGHT = 2;
    public static final int SHIELDS = 3;
    /** What a shield is doing (the FX's action). */
    public static final int SHIELD_BLOCK = 0;
    public static final int SHIELD_GRAB = 1;
    public static final int SHIELD_CATCH = 2;

    /** The shield best placed to meet harm coming from {@code x, z} (his frame: x his right, z ahead; {@code y} up), as {@link #SHIELDS}. */
    public static int shieldFor(double x, double y, double z) {
        double flat = Math.sqrt(x * x + z * z);
        if (y > flat * 0.8) {
            return SHIELD_TOP;
        }
        return x > 0.0 ? SHIELD_RIGHT : SHIELD_LEFT;
    }

    // ------------------------------------------------------------------ White Wing Ascent

    /** How high he is lifted (blocks), over how long; held up there how long; let down how slowly (ticks). */
    public static final double ASCENT_HEIGHT = 17.0;
    public static final int ASCENT_RISE = 30;
    public static final int ASCENT_HOLD = 120;
    public static final int ASCENT_LOWER = 70;
    /** The two tendrils' feet: this far out to each side of where he stood (blocks). */
    public static final double ASCENT_SPREAD = 1.8;
    /** G again: how fast he is wrenched down (blocks a tick); the landing's length. */
    public static final double SLAM_SPEED = 2.6;
    public static final int LAND_TICKS = 30;
    /** The eruption: how far round, how many tendrils, how fast it spreads (blocks a tick), what a tendril coming up does. */
    public static final double ERUPT_RADIUS = 30.0;
    public static final int ERUPT_COUNT = 46;
    public static final double ERUPT_SPREAD = 3.0;
    public static final float ERUPT_DAMAGE = 16.0F;
    /** Each comes up over this (ticks), stays this long all told, sinks back over the last of it. */
    public static final int ERUPT_GROW = 8;
    public static final int ERUPT_TICKS = 120;
    public static final int ERUPT_SINK = 14;
    /** While they stand: one lashes at what is hostile within its reach every this often, for this. */
    public static final int LASH_EVERY = 12;
    public static final double LASH_REACH = 5.5;
    public static final float LASH_DAMAGE = 8.0F;
    public static final int LASH_TICKS = 8;

    /** Where he is over where he stood {@code t} ticks into the lift (blocks): up, slowing, held. */
    public static double lifted(double t, double height) {
        double u = Math.max(0.0, Math.min(1.0, t / ASCENT_RISE));
        return height * (1.0 - Math.pow(1.0 - u, 3.0));
    }

    /** As above, being let down {@code t} ticks into it from {@code from} blocks up. */
    public static double lowered(double t, double from) {
        double u = Math.max(0.0, Math.min(1.0, t / ASCENT_LOWER));
        return from * (1.0 - u * u * (3.0 - 2.0 * u));
    }

    // ------------------------------------------------------------------ Piercing Salvation

    /** Held this long at most (ticks): the lance from its shortest to its longest (blocks), what it does through a body. */
    public static final int PIERCE_CHARGE = 60;
    public static final double LANCE_SHORT = 3.0;
    public static final double LANCE_LONG = 7.5;
    public static final float PIERCE_LOW = 14.0F;
    public static final float PIERCE_HIGH = 32.0F;
    /** Driven out this fast (blocks a tick), this far at most before it comes back with nothing; carrying a body this fast, this far. */
    public static final double PIERCE_SPEED = 2.6;
    public static final double PIERCE_RANGE = 26.0;
    public static final double PIERCE_CARRY = 1.7;
    public static final double PIERCE_DRAG = 30.0;
    /** Pinned to a wall this long (ticks); coming back over this. */
    public static final int PIERCE_PIN = 80;
    public static final int PIERCE_BACK = 24; // 1.0.36: (was 10) the cone opens slowly, each blade back to its place

    /** How far charged ({@code held} ticks): 0..1. */
    public static double charge(double held) {
        return Math.max(0.0, Math.min(1.0, held / PIERCE_CHARGE));
    }

    /** The lance's length charged {@code k} (blocks). */
    public static double lance(double k) {
        return LANCE_SHORT + (LANCE_LONG - LANCE_SHORT) * k;
    }

    /** What it does through a body, charged {@code k}. */
    public static float pierceDamage(double k) {
        return (float)(PIERCE_LOW + (PIERCE_HIGH - PIERCE_LOW) * k);
    }

    // ------------------------------------------------------------------ Rebirth Instinct

    /** Held this long (ticks) for all four; three from half of it; the blades sink into the ground over the first of it. */
    public static final int REBIRTH_CHARGE = 140;
    public static final int REBIRTH_SINK = 10;
    /** Drawn back out of the ground after it (ticks). */
    public static final int REBIRTH_BACK = 12;
    /** The creatures: how long each lives, climbs up out of the ground, sinks back at the end (ticks); where they come up (blocks). */
    public static final int THRALL_LIFE = 300;
    public static final int THRALL_RISE = 20;
    public static final int THRALL_MELT = 30;
    public static final double THRALL_NEAR = 2.5;
    public static final double THRALL_FAR = 4.5;
    public static final double THRALL_APART = 1.7;
    /** Following: how far off him they keep, how far they will go after what is hostile, their cut and how often. */
    public static final double THRALL_FOLLOW = 3.2;
    public static final double THRALL_HUNT = 14.0;
    public static final float THRALL_DAMAGE = 7.0F;
    public static final int THRALL_SWING = 16;
    public static final double THRALL_HEALTH = 30.0;

    /** How many come of a hold {@code held} ticks long: two, three from half the full hold, four at it. */
    public static int thralls(double held) {
        if (held >= REBIRTH_CHARGE) {
            return 4;
        }
        return held >= REBIRTH_CHARGE / 2.0 ? 3 : 2;
    }

    // ------------------------------------------------------------------ The Final Horizon

    /** How far off its body may be (blocks). */
    public static final double HORIZON_RANGE = 8.0;
    /** Its moments (ticks): there; the uppercut; at its height; he is up under it; the blades in (the first, every how often, how long
     *  each takes); the last strikes it down; the most it waits to hit the ground; the drive through it; the release; over. */
    public static final int HORIZON_ARRIVE = 2;
    public static final int HORIZON_UPPER = 3;
    public static final int HORIZON_APEX = 13;
    public static final int HORIZON_UNDER = 16;
    public static final int HORIZON_FIRST = 19;
    public static final int HORIZON_EVERY = 6;
    public static final int HORIZON_REACH = 3;
    public static final int HORIZON_STRIKE = HORIZON_FIRST + 5 * HORIZON_EVERY + 6;
    public static final int HORIZON_FALL_MOST = 34;
    public static final int HORIZON_DRIVE = 26;
    public static final int HORIZON_LET_GO = 10;
    public static final int HORIZON_TICKS = HORIZON_STRIKE + HORIZON_FALL_MOST + HORIZON_DRIVE + HORIZON_LET_GO + 20;
    /** How high it is thrown (blocks); how fast the last blade sends it down (blocks a tick). */
    public static final double HORIZON_UP = 12.0;
    public static final double HORIZON_DOWN = 2.4;
    /** 1.0.36: the line it is sent down along (radians below level, away from him), as War's Black Sun: never straight down under him. */
    public static final double HORIZON_DIVE = Math.toRadians(24.0);
    /** 1.0.36: on the ground the blades drive it on along a shallower line down through the ground (radians), this fast, this far. */
    public static final double HORIZON_TUNNEL = Math.toRadians(10.0);
    public static final double HORIZON_DRIVE_SPEED = 0.9;
    public static final double HORIZON_DEPTH = 18.0;
    /** What each does: the uppercut, each blade in, the last blade's blow, hitting the ground, the drive (each tick of it), the release. */
    public static final float HORIZON_UPPER_DAMAGE = 10.0F;
    public static final float HORIZON_STAB = 5.0F;
    public static final float HORIZON_STRIKE_DAMAGE = 14.0F;
    public static final float HORIZON_GROUND = 10.0F;
    public static final float HORIZON_GRIND = 1.0F;
    public static final float HORIZON_TEAR = 16.0F;
    /** He hangs in the air after the blow down this long (ticks), then falls on his own. */
    public static final int HORIZON_HANG = 40;
    /** The five that go in first (the great one strikes last). */
    public static final int[] HORIZON_ORDER = {PeaceMotion.UR, PeaceMotion.LL, PeaceMotion.ML, PeaceMotion.UL, PeaceMotion.LR};

    /** Where the Horizon has the body {@code t} ticks in (blocks above where the uppercut found it): sent up, slowing, held. */
    public static double horizonRise(double t) {
        if (t < HORIZON_UPPER) {
            return 0.0;
        }
        double u = Math.min(1.0, (t - HORIZON_UPPER) / (HORIZON_APEX - HORIZON_UPPER));
        return HORIZON_UP * (1.0 - Math.pow(1.0 - u, 3.0));
    }

    /** When blade {@code blade} goes into it in the Horizon (ticks in), the great one's blow included; -1 if not. */
    public static int horizonSent(int blade) {
        for (int i = 0; i < HORIZON_ORDER.length; i++) {
            if (HORIZON_ORDER[i] == blade) {
                return HORIZON_FIRST + i * HORIZON_EVERY;
            }
        }
        return blade == PeaceMotion.GREAT ? HORIZON_STRIKE - HORIZON_REACH : -1;
    }
}
