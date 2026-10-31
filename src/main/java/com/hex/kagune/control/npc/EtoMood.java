package com.hex.kagune.control.npc;

/**
 * 1.3.2: how Eto feels about the player in front of her right now (pure, tested): what colours her greeting, her answers, the idle
 * things she does with her hands, and how often she comes looking for them. Worked out fresh each time from what has happened
 * ({@link #of}); nothing of it is kept but what it is made from.
 */
public enum EtoMood {
    /** Fine. Pleased to see you, in her own crooked way. */
    HAPPY("happy"),
    /** Just kissed, hugged, held: can't stop smiling, and won't admit it. */
    GIDDY("giddy"),
    /** Left hanging, hit, insulted: arms crossed, chin up. */
    SULKY("sulky"),
    /** You were sweet on someone else. */
    JEALOUS("jealous"),
    /** Her partner stayed away for days. */
    LONELY("lonely"),
    /** Her partner is here and she does not intend to let go of them. */
    CLINGY("clingy"),
    /** Dawn: the night's over and so is she. */
    SLEEPY("sleepy"),
    /** Deep in the night, hungry, her eye wide open. */
    BLOODTHIRSTY("hungry"),
    /** Night, her partner close: she wants them, and not for conversation. */
    AFFECTIONATE("affectionate"),
    /** Nobody's been by in ages. */
    BORED("bored"),
    /** Someone else's partner talking to her: polite at best. */
    CLAIMED("taken"),
    /** A stranger, or someone she doesn't like much. */
    WARY("wary");

    private final String word;

    EtoMood(String word) {
        this.word = word;
    }

    /** One word for it (the talk screen's subtitle). */
    public String word() {
        return this.word;
    }

    /** The night begins: her clock (the day's ticks). */
    static final long DUSK = 12542L;
    static final long DEEP = 17000L;
    static final long DAWN = 22500L;
    static final long MORNING = 1500L;

    /**
     * How she feels about them: {@code stage} theirs with her, {@code claimed} she is someone else's, {@code regard}, {@code dayTime} the
     * day's ticks, {@code daysAway} days since they were last with her, and what happened lately ({@code sulking} cross with them,
     * {@code jealous}, {@code giddy} just kissed or hugged, {@code near} they have been close a while), {@code roll} in [0, 1) for her whims.
     */
    public static EtoMood of(Romance.Stage stage, boolean claimed, int regard, long dayTime, long daysAway, boolean sulking, boolean jealous,
                             boolean giddy, boolean near, double roll) {
        long time = Math.floorMod(dayTime, 24000L);
        boolean night = time >= DUSK && time <= DAWN;
        boolean dawn = time > DAWN || time < MORNING;
        if (sulking) {
            return SULKY;
        }
        if (claimed) {
            return CLAIMED;
        }
        if (stage.together()) {
            if (jealous) {
                return JEALOUS;
            }
            if (giddy) {
                return GIDDY;
            }
            if (daysAway >= 2L) {
                return LONELY;
            }
            if (night && time >= DEEP && roll < 0.55) {
                return AFFECTIONATE;
            }
            if (dawn && roll < 0.5) {
                return SLEEPY;
            }
            if (near && roll < 0.6) {
                return CLINGY;
            }
            return night && roll > 0.85 ? BLOODTHIRSTY : HAPPY;
        }
        if (regard < NpcBonds.FRIENDLY) {
            return regard < 0 ? WARY : night && roll < 0.3 ? BLOODTHIRSTY : WARY;
        }
        if (daysAway >= 4L) {
            return BORED;
        }
        if (dawn && roll < 0.4) {
            return SLEEPY;
        }
        return night && roll < 0.25 ? BLOODTHIRSTY : HAPPY;
    }

    /** The idle things she does in this mood (her hands find something to do), most likely first. */
    public EtoGesture[] idles() {
        return switch (this) {
            case GIDDY -> new EtoGesture[] {EtoGesture.HUM, EtoGesture.TWIRL, EtoGesture.HOP, EtoGesture.DANCE, EtoGesture.HAIR, EtoGesture.GIGGLE};
            case SULKY, JEALOUS -> new EtoGesture[] {EtoGesture.POUT, EtoGesture.POUT, EtoGesture.READ, EtoGesture.LOOK};
            case LONELY, BORED -> new EtoGesture[] {EtoGesture.READ, EtoGesture.WRITE, EtoGesture.LOOK, EtoGesture.HAIR, EtoGesture.STRETCH};
            case CLINGY, AFFECTIONATE -> new EtoGesture[] {EtoGesture.HAIR, EtoGesture.HUM, EtoGesture.PEACE, EtoGesture.LEAN, EtoGesture.WAVE};
            case SLEEPY -> new EtoGesture[] {EtoGesture.YAWN, EtoGesture.STRETCH, EtoGesture.READ, EtoGesture.LEAN};
            case BLOODTHIRSTY -> new EtoGesture[] {EtoGesture.LICK, EtoGesture.LOOK, EtoGesture.HAIR, EtoGesture.STRETCH};
            case CLAIMED, WARY -> new EtoGesture[] {EtoGesture.READ, EtoGesture.WRITE, EtoGesture.SWEEP, EtoGesture.LOOK, EtoGesture.STRETCH};
            default -> new EtoGesture[] {EtoGesture.READ, EtoGesture.WRITE, EtoGesture.SWEEP, EtoGesture.HUM, EtoGesture.WAVE, EtoGesture.STRETCH,
                EtoGesture.TWIRL, EtoGesture.PEACE, EtoGesture.LEAN, EtoGesture.HAIR, EtoGesture.GIGGLE, EtoGesture.HOP};
        };
    }

    /** How often (game ticks, about) she goes looking for them in this mood: 0 never. */
    public long seeks() {
        return switch (this) {
            case CLINGY, AFFECTIONATE, LONELY -> 2400L;
            case GIDDY, HAPPY -> 4800L;
            case JEALOUS -> 3600L;
            case BLOODTHIRSTY, SLEEPY -> 7200L;
            default -> 0L;
        };
    }
}
