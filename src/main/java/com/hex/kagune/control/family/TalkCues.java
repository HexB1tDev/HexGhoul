package com.hex.kagune.control.family;

import com.hex.kagune.control.npc.EtoGesture;
import java.util.Locale;

/**
 * 1.3.57: what each one in a talk does with its body as it says its line (pure, tested), the owner: "focusing mainly on the beauty of
 * interactions". A child's {@link ChildCue}, a companion's {@link EtoGesture}: first from what the line says ("*crosses her arms*",
 * "Hehe~", "Pleeease?"), else from the talk ({@link Scene}), its place in it and the speaker's own temper or way (a squabble's pointing and
 * folded arms, a brag's hands on hips, a scheme's whispers, a scolding's hands on hips or a hand to the chin, a sorry with its head down,
 * a tongue stuck out when nobody's looking).
 */
public final class TalkCues {
    /** What kind of talk a line is in (never sent: order free). */
    public enum Scene {
        CHAT, TEASE, BRAG, COMFORT, SCHEME, SQUABBLE, MIND,
        /** A parent's telling-off for wandering or being out at night (the parent first). */
        SCOLDED,
        /** A parent stopping a rampage. */
        RAMPAGE,
        /** The word at the door. */
        HOME,
        /** A parent breaking up a squabble, and each child's sorry. */
        MAKE_UP,
        /** A talk cut short (someone walked off). */
        CUT
    }

    private TalkCues() {
    }

    /** {@code text}'s own cue, if it says what it does (an action in asterisks, a giggle, a plea, a cheer); else NONE. */
    public static ChildCue said(String text) {
        String t = text == null ? "" : text.toLowerCase(Locale.ROOT);
        if (t.contains("cross") && t.contains("arm")) {
            return ChildCue.CROSS;
        }
        if (t.contains("stamp") || t.contains("stomp")) {
            return ChildCue.STAMP;
        }
        if (t.contains("*point")) {
            return ChildCue.POINT;
        }
        if (t.contains("hands on") && t.contains("hip")) {
            return ChildCue.HIPS;
        }
        if (t.contains("shrug")) {
            return ChildCue.SHRUG;
        }
        if (t.contains("tongue") || t.contains("raspberry") || t.contains("bleh") || t.contains("nyeh")) {
            return ChildCue.BLEH;
        }
        if (t.contains("whisper") || t.startsWith("psst")) {
            return ChildCue.WHISPER;
        }
        if (t.contains("giggl") || t.contains("snicker") || t.contains("hehe") || t.contains("hihi")) {
            return ChildCue.GIGGLE;
        }
        if (t.contains("pout") || t.contains("hmph") || t.contains("sulk")) {
            return ChildCue.SULK;
        }
        if (t.contains("sorry") || t.contains("sniff") || t.contains("wobbl")) {
            return ChildCue.SORRY;
        }
        if (t.contains("please") || t.contains("pleeease") || t.contains("pretty please")) {
            return ChildCue.PLEAD;
        }
        if (t.contains("*wave")) {
            return ChildCue.WAVE;
        }
        if (t.contains("*pat") || t.contains("hug")) {
            return ChildCue.PAT;
        }
        if (t.startsWith("yay") || t.contains("woo") || t.startsWith("yes!")) {
            return ChildCue.CHEER;
        }
        if (t.startsWith("hmm") || t.startsWith("hm.")) {
            return ChildCue.THINK;
        }
        return ChildCue.NONE;
    }

    /**
     * A child's cue for its line {@code index} in the talk (0 the first) in {@code scene}, {@code starter} if it began the talk, by its
     * temper; what the line itself says first. Now and then nothing at all (it just talks): {@code seed} decides.
     */
    public static ChildCue child(String text, Scene scene, ChildTemper temper, boolean starter, int index, int seed) {
        ChildCue own = said(text);
        if (own != ChildCue.NONE) {
            return own;
        }
        String t = text == null ? "" : text;
        boolean shout = t.contains("!");
        boolean asks = t.contains("?");
        ChildTemper me = temper == null ? ChildTemper.SUNNY : temper;
        int pick = Math.floorMod(seed, 4);
        return switch (scene) {
            case SQUABBLE -> starter ? pickOf(pick, switch (me) {
                case BOLD -> new ChildCue[] {ChildCue.POINT, ChildCue.STAMP};
                case MISCHIEF -> new ChildCue[] {ChildCue.BLEH, ChildCue.POINT};
                case SHY, GENTLE -> new ChildCue[] {ChildCue.SULK};
                case SERIOUS -> new ChildCue[] {ChildCue.CROSS, ChildCue.POINT};
                default -> new ChildCue[] {ChildCue.POINT, ChildCue.STAMP, ChildCue.CROSS};
            }) : pickOf(pick, switch (me) {
                case BOLD -> new ChildCue[] {ChildCue.STAMP, ChildCue.CROSS};
                case MISCHIEF -> new ChildCue[] {ChildCue.BLEH, ChildCue.GIGGLE};
                case SHY, GENTLE -> new ChildCue[] {ChildCue.SULK, ChildCue.SORRY};
                case SERIOUS -> new ChildCue[] {ChildCue.CROSS};
                default -> new ChildCue[] {ChildCue.CROSS, ChildCue.STAMP};
            });
            case TEASE -> starter ? pickOf(pick, new ChildCue[] {ChildCue.GIGGLE, ChildCue.POINT, ChildCue.BLEH})
                : pickOf(pick, switch (me) {
                    case SHY, GENTLE -> new ChildCue[] {ChildCue.SULK};
                    case BOLD -> new ChildCue[] {ChildCue.CROSS, ChildCue.STAMP};
                    case MISCHIEF -> new ChildCue[] {ChildCue.BLEH};
                    default -> new ChildCue[] {ChildCue.CROSS, ChildCue.SHRUG};
                });
            case BRAG -> starter ? ChildCue.HIPS : pickOf(pick, switch (me) {
                case SUNNY -> new ChildCue[] {ChildCue.CHEER};
                case CURIOUS -> new ChildCue[] {ChildCue.THINK};
                case BOLD -> new ChildCue[] {ChildCue.CROSS, ChildCue.HIPS};
                default -> new ChildCue[] {ChildCue.SHRUG, ChildCue.NONE};
            });
            case COMFORT -> starter ? ChildCue.PAT : ChildCue.SORRY;
            case SCHEME -> index >= 2 ? ChildCue.GIGGLE : ChildCue.WHISPER;
            case MIND -> starter ? pickOf(pick, switch (me) {
                case GENTLE, SUNNY -> new ChildCue[] {ChildCue.PAT};
                case BOLD, SERIOUS -> new ChildCue[] {ChildCue.HIPS, ChildCue.POINT};
                default -> new ChildCue[] {ChildCue.POINT, ChildCue.HIPS};
            }) : pickOf(pick, switch (me) {
                case SHY, GENTLE -> new ChildCue[] {ChildCue.SORRY};
                case MISCHIEF -> new ChildCue[] {ChildCue.BLEH, ChildCue.PLEAD};
                case BOLD -> new ChildCue[] {ChildCue.SULK, ChildCue.STAMP};
                default -> new ChildCue[] {ChildCue.PLEAD, ChildCue.SULK};
            });
            case SCOLDED, RAMPAGE -> pickOf(pick, switch (me) {
                case SHY, GENTLE -> new ChildCue[] {ChildCue.SORRY};
                case BOLD -> new ChildCue[] {ChildCue.CROSS, ChildCue.STAMP, ChildCue.SULK};
                case MISCHIEF -> new ChildCue[] {ChildCue.PLEAD, ChildCue.GIGGLE, ChildCue.SORRY};
                case SUNNY -> new ChildCue[] {ChildCue.PLEAD, ChildCue.SORRY};
                case CURIOUS -> new ChildCue[] {ChildCue.SHRUG, ChildCue.THINK, ChildCue.SORRY};
                case SERIOUS -> new ChildCue[] {ChildCue.SORRY, ChildCue.CROSS};
            });
            // 3.2.0: its answer at the door (line 1) and, now and then, its last word (line 3), each in its temper's way.
            case HOME -> index >= 3 ? pickOf(pick, switch (me) {
                case SUNNY -> new ChildCue[] {ChildCue.CHEER, ChildCue.WAVE};
                case BOLD -> new ChildCue[] {ChildCue.SHRUG, ChildCue.CROSS};
                case MISCHIEF -> new ChildCue[] {ChildCue.GIGGLE, ChildCue.BLEH};
                case CURIOUS -> new ChildCue[] {ChildCue.THINK};
                case GENTLE -> new ChildCue[] {ChildCue.PAT, ChildCue.NONE};
                case SHY, SERIOUS -> new ChildCue[] {ChildCue.NONE};
            }) : pickOf(pick, switch (me) {
                case SUNNY -> new ChildCue[] {ChildCue.WAVE, ChildCue.CHEER};
                case BOLD -> new ChildCue[] {ChildCue.SHRUG, ChildCue.CROSS};
                case MISCHIEF -> new ChildCue[] {ChildCue.GIGGLE, ChildCue.PLEAD};
                case CURIOUS -> new ChildCue[] {ChildCue.THINK, ChildCue.SHRUG};
                case SHY, GENTLE -> new ChildCue[] {ChildCue.SORRY, ChildCue.NONE};
                case SERIOUS -> new ChildCue[] {ChildCue.NONE, ChildCue.SORRY};
            });
            // The sorry after a squabble: most mean it; a mischievous one sticks its tongue out at the other when nobody's looking.
            case MAKE_UP -> me == ChildTemper.MISCHIEF ? (pick % 2 == 0 ? ChildCue.BLEH : ChildCue.SORRY) : me == ChildTemper.BOLD
                ? (pick % 2 == 0 ? ChildCue.SHRUG : ChildCue.CROSS) : ChildCue.SORRY;
            case CUT -> me == ChildTemper.BOLD ? ChildCue.STAMP : me == ChildTemper.SUNNY || me == ChildTemper.GENTLE ? ChildCue.WAVE
                : me == ChildTemper.SHY ? ChildCue.SULK : ChildCue.SHRUG;
            case CHAT -> asks && (me == ChildTemper.CURIOUS || pick == 0) ? ChildCue.THINK : shout && (me == ChildTemper.SUNNY || pick == 1)
                ? ChildCue.CHEER : index == 0 && pick == 2 ? ChildCue.WAVE : pick == 3 ? ChildCue.SHRUG : ChildCue.NONE;
        };
    }

    /** {@code text}'s own gesture for a companion (a sigh, folded arms, a laugh, a hug, a tapped foot), else NONE. */
    public static EtoGesture saidBy(String text) {
        String t = text == null ? "" : text.toLowerCase(Locale.ROOT);
        if (t.contains("cross") && t.contains("arm")) {
            return EtoGesture.CROSS;
        }
        if (t.contains("sigh") || t.contains("huff")) {
            return EtoGesture.HUFF;
        }
        if (t.contains("laugh") || t.contains("fufu") || t.contains("ahaha") || t.contains("haha")) {
            return EtoGesture.LAUGH;
        }
        if (t.contains("hug")) {
            return EtoGesture.HUG;
        }
        if (t.contains("tap")) {
            return EtoGesture.TAP;
        }
        if (t.contains("*bow")) {
            return EtoGesture.BOW;
        }
        return EtoGesture.NONE;
    }

    /**
     * A companion parent's gesture ({@code persona} its key) for its line {@code index} in the talk in {@code scene}: Touka's hands on hips
     * and tapping foot, Ken's hand to his chin and a gentle telling-off, Eto's amused chin-in-hand and laugh; what the line says first.
     */
    public static EtoGesture parent(String text, Scene scene, String persona, int index, int seed) {
        EtoGesture own = saidBy(text);
        if (own != EtoGesture.NONE) {
            return own;
        }
        String who = persona == null ? "" : persona.toLowerCase(Locale.ROOT);
        int pick = Math.floorMod(seed, 3);
        boolean last = index >= 4;
        return switch (scene) {
            case SCOLDED -> switch (who) {
                case "touka" -> last ? EtoGesture.HUFF : index == 0 ? (pick == 0 ? EtoGesture.TAP : EtoGesture.SCOLD) : EtoGesture.CROSS;
                case "ken" -> last ? EtoGesture.NONE : index == 0 ? EtoGesture.SCOLD : EtoGesture.CHIN;
                case "eto" -> last ? EtoGesture.LAUGH : index == 0 ? EtoGesture.CHIN : (pick == 0 ? EtoGesture.CROSS : EtoGesture.CHIN);
                default -> index == 0 ? EtoGesture.SCOLD : EtoGesture.NONE;
            };
            case RAMPAGE -> switch (who) {
                case "touka" -> index == 0 ? EtoGesture.ANGRY : last ? EtoGesture.HUFF : EtoGesture.SCOLD;
                case "eto" -> index == 0 ? EtoGesture.CROSS : last ? EtoGesture.LAUGH : EtoGesture.CHIN;
                default -> last ? EtoGesture.NONE : EtoGesture.SCOLD;
            };
            case MAKE_UP -> switch (who) {
                case "touka" -> pick == 0 ? EtoGesture.TAP : EtoGesture.SCOLD;
                case "eto" -> pick == 0 ? EtoGesture.LAUGH : EtoGesture.CROSS;
                default -> EtoGesture.SCOLD;
            };
            // 3.2.0: at the door (line 0), then its answer to the child (line 2): Touka's folded arms and a sigh, Ken's hand to his chin,
            // Eto's chin in her hand and her laugh.
            case HOME -> switch (who) {
                case "touka" -> index == 0 ? EtoGesture.CROSS : pick == 0 ? EtoGesture.HUFF : EtoGesture.NONE;
                case "ken" -> index == 0 ? EtoGesture.NONE : pick == 0 ? EtoGesture.CHIN : EtoGesture.NONE;
                case "eto" -> index == 0 ? (pick == 0 ? EtoGesture.CHIN : EtoGesture.NONE) : pick == 1 ? EtoGesture.LAUGH : EtoGesture.NONE;
                default -> EtoGesture.NONE;
            };
            case CUT -> "touka".equals(who) ? EtoGesture.TAP : "eto".equals(who) ? EtoGesture.LAUGH : EtoGesture.HUFF;
            default -> EtoGesture.NONE;
        };
    }

    private static ChildCue pickOf(int pick, ChildCue[] from) {
        return from[Math.floorMod(pick, from.length)];
    }
}
