package com.hex.kagune.control.village;

import com.hex.kagune.control.family.ChildCue;
import com.hex.kagune.control.npc.EtoGesture;

/**
 * 1.3.77: what a villager does with its body as it talks and lives (pure). The owner: "take animations from eto, kids, touka and kaneki for
 * base, mix it together and adapt it to NPCS". A move is one of Eto's gestures (drawn in Eto's, Ken's or Touka's way by the villager's
 * temper, {@link #style}) or one of the children's talk cues ({@link #CUE} on), coded into one int for its entity data with its style and a
 * count, so the same move twice plays twice ({@link #pack}). None of them holds a prop: a villager's hands stay its own.
 */
public final class HumanGestures {
    private HumanGestures() {
    }

    /** A child's cue is coded as its ordinal plus this; an Eto gesture as its ordinal (0: nothing). */
    public static final int CUE = 128;
    /** How long a gesture Eto holds until she is told otherwise (a kiss, a blush) plays for a villager. */
    public static final int HELD = 40;

    public static int of(EtoGesture gesture) {
        return gesture == null ? 0 : gesture.ordinal();
    }

    public static int of(ChildCue cue) {
        return cue == null || cue == ChildCue.NONE ? 0 : CUE + cue.ordinal();
    }

    public static boolean cue(int code) {
        return code >= CUE;
    }

    public static EtoGesture gesture(int code) {
        return code > 0 && code < CUE ? EtoGesture.byOrdinal(code) : EtoGesture.NONE;
    }

    public static ChildCue childCue(int code) {
        return code >= CUE ? ChildCue.byOrdinal(code - CUE) : ChildCue.NONE;
    }

    /** How long move {@code code} plays (ticks; 0 for nothing). */
    public static int ticks(int code) {
        if (code <= 0) {
            return 0;
        }
        if (cue(code)) {
            return childCue(code).ticks;
        }
        EtoGesture g = gesture(code);
        return g.held() ? HELD : g.ticks;
    }

    /** True if the move shuts its eyes (a kiss, a giggle, a hug). */
    public static boolean eyesClosed(int code) {
        return !cue(code) && gesture(code).eyesClosed;
    }

    /** True if the move blushes its cheeks (Eto's blushing gestures; the children's giggle and plea). */
    public static boolean blush(int code) {
        return cue(code) ? childCue(code) == ChildCue.GIGGLE || childCue(code) == ChildCue.PLEAD : gesture(code).blush;
    }

    /** The move, its style and its count as one int (code in the low 8 bits, style the next 2, the count above). */
    public static int pack(int code, int style, int count) {
        return code & 255 | (style & 3) << 8 | (count & 0x3FFFFF) << 10;
    }

    public static int code(int packed) {
        return packed & 255;
    }

    public static int style(int packed) {
        return packed >>> 8 & 3;
    }

    public static int count(int packed) {
        return packed >>> 10;
    }

    /** Whose way it moves (EtoPose's style): the bright and the dreamy as Eto (0), the steady as Ken (1), the reserved as Touka (2). */
    public static int style(HumanPersona.Temper temper) {
        return switch (temper) {
            case CHEERFUL, FLIRTY, DREAMY -> 0;
            case KIND, BOLD -> 1;
            case SHY, GRUMPY, NERVOUS -> 2;
            // 2.0.36: the new tempers: the playful and the romantic bright, the stoic steady, the sarcastic reserved.
            case PLAYFUL, ROMANTIC -> 0;
            case STOIC -> 1;
            case SARCASTIC -> 2;
        };
    }

    private static int pick(int seed, int... codes) {
        return codes[Math.floorMod(seed, codes.length)];
    }

    private static int g(EtoGesture gesture) {
        return of(gesture);
    }

    private static int c(ChildCue cue) {
        return of(cue);
    }

    /** Saying hello to a player (a partner: warmer). */
    public static int hello(HumanPersona.Temper temper, boolean partner, int seed) {
        if (partner) {
            return switch (temper) {
                case SHY, NERVOUS -> g(EtoGesture.SHY);
                case GRUMPY -> g(EtoGesture.HUFF);
                case FLIRTY -> pick(seed, g(EtoGesture.PECK), g(EtoGesture.TWIRL));
                default -> pick(seed, g(EtoGesture.WAVE), g(EtoGesture.HOP));
            };
        }
        return switch (temper) {
            case CHEERFUL -> pick(seed, g(EtoGesture.WAVE), g(EtoGesture.PEACE), g(EtoGesture.HOP));
            case SHY -> pick(seed, g(EtoGesture.SHY), c(ChildCue.SORRY));
            case BOLD -> pick(seed, g(EtoGesture.WAVE), c(ChildCue.HIPS));
            case KIND -> pick(seed, g(EtoGesture.WAVE), g(EtoGesture.BOW));
            case GRUMPY -> pick(seed, g(EtoGesture.CROSS), c(ChildCue.CROSS));
            case FLIRTY -> pick(seed, g(EtoGesture.HAIR), g(EtoGesture.WAVE));
            case DREAMY -> pick(seed, g(EtoGesture.WAVE), g(EtoGesture.LOOK));
            case NERVOUS -> pick(seed, g(EtoGesture.TAP), g(EtoGesture.BOW));
            case PLAYFUL -> pick(seed, g(EtoGesture.WAVE), g(EtoGesture.HOP), g(EtoGesture.PEACE));
            case STOIC -> pick(seed, g(EtoGesture.BOW), g(EtoGesture.WAVE));
            case SARCASTIC -> pick(seed, c(ChildCue.SHRUG), g(EtoGesture.CROSS));
            case ROMANTIC -> pick(seed, g(EtoGesture.TWIRL), g(EtoGesture.WAVE));
        };
    }

    /** Talking a while. */
    public static int chat(HumanPersona.Temper temper, int seed) {
        return switch (temper) {
            case CHEERFUL -> pick(seed, g(EtoGesture.LAUGH), g(EtoGesture.GIGGLE), c(ChildCue.CHEER));
            case SHY -> pick(seed, c(ChildCue.GIGGLE), g(EtoGesture.SHY));
            case BOLD -> pick(seed, g(EtoGesture.LAUGH), c(ChildCue.HIPS), c(ChildCue.POINT));
            case KIND -> pick(seed, g(EtoGesture.LAUGH), c(ChildCue.PAT));
            case GRUMPY -> pick(seed, c(ChildCue.SHRUG), g(EtoGesture.HUFF));
            case FLIRTY -> pick(seed, g(EtoGesture.GIGGLE), g(EtoGesture.HAIR), c(ChildCue.WHISPER));
            case DREAMY -> pick(seed, g(EtoGesture.CHIN), c(ChildCue.THINK), g(EtoGesture.HUM));
            case NERVOUS -> pick(seed, g(EtoGesture.TAP), c(ChildCue.GIGGLE));
            case PLAYFUL -> pick(seed, g(EtoGesture.GIGGLE), c(ChildCue.CHEER), c(ChildCue.POINT));
            case STOIC -> pick(seed, c(ChildCue.THINK), g(EtoGesture.CHIN));
            case SARCASTIC -> pick(seed, c(ChildCue.SHRUG), g(EtoGesture.LAUGH));
            case ROMANTIC -> pick(seed, g(EtoGesture.HUM), g(EtoGesture.TWIRL), g(EtoGesture.SHY));
        };
    }

    /** Talking about themselves. */
    public static int about(HumanPersona.Temper temper, int seed) {
        return switch (temper) {
            case CHEERFUL, BOLD -> pick(seed, c(ChildCue.HIPS), c(ChildCue.CHEER));
            case SHY, NERVOUS -> pick(seed, g(EtoGesture.SHY), c(ChildCue.SORRY));
            case GRUMPY -> c(ChildCue.SHRUG);
            case DREAMY -> pick(seed, g(EtoGesture.LOOK), c(ChildCue.THINK));
            default -> pick(seed, g(EtoGesture.CHIN), c(ChildCue.THINK));
        };
    }

    /** Passing on the village's news. */
    public static int news(int seed) {
        return pick(seed, c(ChildCue.WHISPER), c(ChildCue.THINK), c(ChildCue.SHRUG), g(EtoGesture.CHIN));
    }

    /** Hearing a compliment. */
    public static int compliment(HumanPersona.Temper temper, int seed) {
        return switch (temper) {
            case CHEERFUL -> pick(seed, g(EtoGesture.GIGGLE), g(EtoGesture.TWIRL));
            case SHY, NERVOUS -> g(EtoGesture.SHY);
            case BOLD -> c(ChildCue.HIPS);
            case KIND -> pick(seed, g(EtoGesture.GIGGLE), g(EtoGesture.BOW));
            case GRUMPY -> pick(seed, g(EtoGesture.HUFF), g(EtoGesture.POUT));
            case FLIRTY -> pick(seed, g(EtoGesture.HAIR), g(EtoGesture.TWIRL));
            case DREAMY -> pick(seed, g(EtoGesture.SHY), g(EtoGesture.HUM));
            case PLAYFUL -> pick(seed, g(EtoGesture.GIGGLE), g(EtoGesture.TWIRL));
            case STOIC -> g(EtoGesture.BOW);
            case SARCASTIC -> pick(seed, c(ChildCue.SHRUG), g(EtoGesture.POUT));
            case ROMANTIC -> pick(seed, g(EtoGesture.SHY), g(EtoGesture.TWIRL));
        };
    }

    /** Hearing a ghoul's confession, as it took it. */
    public static int confessed(HumanPersona.Answer answer) {
        return switch (answer) {
            case YES -> g(EtoGesture.AWE);
            case DOUBT -> c(ChildCue.CROSS);
            case NO -> g(EtoGesture.FLINCH);
        };
    }

    /** Saying yes to going out. */
    public static int yes(HumanPersona.Temper temper) {
        return switch (temper) {
            case CHEERFUL -> c(ChildCue.CHEER);
            case SHY, NERVOUS -> g(EtoGesture.SHY);
            case GRUMPY -> g(EtoGesture.POUT);
            case FLIRTY -> g(EtoGesture.KISSED);
            default -> g(EtoGesture.HUG);
        };
    }

    /** Hearing "I love you." */
    public static int love(HumanPersona.Temper temper, int seed) {
        return switch (temper) {
            case SHY, NERVOUS -> g(EtoGesture.SHY);
            case GRUMPY -> pick(seed, g(EtoGesture.POUT), g(EtoGesture.SHY));
            case FLIRTY -> pick(seed, g(EtoGesture.PECK), g(EtoGesture.KISSED));
            default -> pick(seed, g(EtoGesture.KISSED), g(EtoGesture.HUG));
        };
    }

    /** Saying goodbye. */
    public static int bye(HumanPersona.Temper temper, int seed) {
        return switch (temper) {
            case KIND, NERVOUS -> pick(seed, g(EtoGesture.BOW), g(EtoGesture.WAVE));
            case GRUMPY -> c(ChildCue.SHRUG);
            default -> g(EtoGesture.WAVE);
        };
    }

    /** A greeting to another of the village in passing. */
    public static int greet(int seed) {
        return pick(seed, g(EtoGesture.WAVE), g(EtoGesture.WAVE), g(EtoGesture.BOW), c(ChildCue.WAVE));
    }

    /** A word at dusk. */
    public static int dusk(int seed) {
        return pick(seed, g(EtoGesture.YAWN), g(EtoGesture.STRETCH));
    }

    /** Something to do by itself now and then while it stands in the street, by temper. */
    public static int idle(HumanPersona.Temper temper, int seed) {
        return switch (temper) {
            case CHEERFUL -> pick(seed, g(EtoGesture.DANCE), g(EtoGesture.HUM), g(EtoGesture.STRETCH));
            case SHY -> pick(seed, g(EtoGesture.HAIR), g(EtoGesture.LOOK));
            case BOLD -> pick(seed, g(EtoGesture.STRETCH), c(ChildCue.HIPS));
            case KIND -> pick(seed, g(EtoGesture.HUM), g(EtoGesture.LOOK));
            case GRUMPY -> pick(seed, g(EtoGesture.CROSS), g(EtoGesture.TAP));
            case FLIRTY -> pick(seed, g(EtoGesture.HAIR), g(EtoGesture.TWIRL));
            case DREAMY -> pick(seed, g(EtoGesture.LOOK), g(EtoGesture.HUM), g(EtoGesture.CHIN));
            case NERVOUS -> pick(seed, g(EtoGesture.TAP), g(EtoGesture.LOOK));
            case PLAYFUL -> pick(seed, g(EtoGesture.DANCE), g(EtoGesture.STRETCH), g(EtoGesture.HOP));
            case STOIC -> pick(seed, g(EtoGesture.CROSS), g(EtoGesture.LOOK));
            case SARCASTIC -> pick(seed, g(EtoGesture.TAP), g(EtoGesture.LOOK), g(EtoGesture.CROSS));
            case ROMANTIC -> pick(seed, g(EtoGesture.HUM), g(EtoGesture.HAIR), g(EtoGesture.LOOK));
        };
    }

    /** True if it stands still for the move (Eto's still gestures, any child's cue but a wave or a cheer). */
    public static boolean still(int code) {
        if (cue(code)) {
            ChildCue cue = childCue(code);
            return cue != ChildCue.WAVE && cue != ChildCue.CHEER;
        }
        EtoGesture g = gesture(code);
        return g.still() || g.held() || g == EtoGesture.HUG || g == EtoGesture.KISSED || g == EtoGesture.SHY;
    }
}
