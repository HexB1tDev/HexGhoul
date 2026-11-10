package com.hex.kagune.control.npc;

/**
 * 2.2.0 (pure, tested; the owner: "Check if there's an apology system ... I'm tired of waiting 5 minutes every time I hit the spouse once by
 * accident. And add tolerance, make them not be pissed every single time you hit them or miss a kiss"): how much a partner lets go.
 *
 * <p>What a companion holds against their partner is counted ({@link #offences}): it fades after {@link #FORGETS} ticks without another.
 * A first blow is an accident (more so in the middle of a fight: {@link #hit}); a missed kiss the first time is shrugged off ({@link #missed}).
 * Only a second or a third, close together, puts them out, and never for long. And "I'm sorry." is always there to say ({@link #apology}):
 * it ends it at once, unless they have been hurt again and again, when they want a moment first.
 */
public final class Tolerance {
    private Tolerance() {
    }

    /** What they hold is forgotten this long after the last offence (ticks: three minutes). */
    public static final long FORGETS = 3600L;

    /** How they take it. */
    public enum Reaction {
        /** Brushed off: a word, nothing held. */
        SHRUG(0, 0L),
        /** Put out: a little regard, and a short sulk (they won't kiss or come up to them; talking is fine, sorry ends it). */
        PUT_OUT(-2, 400L),
        /** Cross: more regard, a longer sulk. */
        CROSS(-6, 1200L);

        public final int regard;
        public final long sulk;

        Reaction(int regard, long sulk) {
            this.regard = regard;
            this.sulk = sulk;
        }
    }

    /** How many offences still count at {@code now}: {@code count} so far, the last at {@code last} (none: Long.MIN_VALUE). */
    public static int offences(int count, long last, long now) {
        return last == Long.MIN_VALUE || now - last >= FORGETS ? 0 : count;
    }

    /** A blow from their partner, the {@code before}-th (0: the first) still counted; {@code fighting}: in the middle of a fight. */
    public static Reaction hit(int before, boolean fighting) {
        int weight = fighting ? before - 1 : before;
        return weight <= 0 ? Reaction.SHRUG : weight == 1 ? Reaction.PUT_OUT : Reaction.CROSS;
    }

    /** A kiss left hanging, the {@code before}-th still counted. */
    public static Reaction missed(int before) {
        return before <= 0 ? Reaction.SHRUG : before == 1 ? Reaction.PUT_OUT : Reaction.CROSS;
    }

    /** What "I'm sorry." does: {@code offences} still counted. */
    public enum Apology {
        /** Forgiven, all of it. */
        FORGIVEN,
        /** Forgiven, but they want a moment ({@link #MOMENT}) before they're themselves again. */
        A_MOMENT
    }

    public static final long MOMENT = 300L;

    public static Apology apology(int offences) {
        return offences >= 4 ? Apology.A_MOMENT : Apology.FORGIVEN;
    }

    // ---------------------------------------------------------------- their words

    /** What {@code key} says to a blow brushed off. */
    public static String shrug(String key, int seed) {
        return pick(seed, switch (key) {
            case "eto" -> new String[] {"Ow. ...Careful where you swing that, love.", "Hey. That was me. ...I'll let it go. Once.",
                "Mm, rough today. I'll pretend that was a hug."};
            case "touka" -> new String[] {"Ow! Watch it, idiot.", "Did you just— ...Fine. Accidents happen. Don't make it a habit.",
                "Watch your aim. I'm not a training dummy."};
            case "ken" -> new String[] {"Ah— it's okay. I'm okay. Accidents happen.", "That was me, you know. ...It's fine. Really.",
                "Careful. I heal fast, but still."};
            case "general" -> new String[] {"Steady! ...Once. I'll overlook once.", "Watch your arm. I've court-martialled for less.", "Hm. Careless."};
            case "ayato" -> new String[] {"Hey! Watch it. ...Once. You get one.", "Do that again and I hit back.", "Tch. Sloppy. Aim better."};
            case "noro" -> new String[] {"*He doesn't react.*", "*His head turns. Slowly.*"};
            default -> new String[] {"Ow."};
        });
    }

    /** What {@code key} says to a kiss missed and brushed off. */
    public static String shrugKiss(String key, int seed) {
        return pick(seed, switch (key) {
            case "eto" -> new String[] {"...Really? Rude. I'll collect it later. With interest.", "Hm. Your loss. I'll ask again when you deserve it."};
            case "touka" -> new String[] {"...Fine. Whatever. Not like I wanted one.", "Seriously? ...You owe me one. Later."};
            case "ken" -> new String[] {"Oh. ...That's okay. Another time.", "Ah, you're busy. It's alright. I'll wait."};
            case "general" -> new String[] {"...As you were.", "Hm. Another time, then."};
            case "ayato" -> new String[] {"...Fine. Whatever. Your loss.", "Tch. Didn't want one anyway."};
            case "noro" -> new String[] {"*He doesn't move.*"};
            default -> new String[] {"Hm."};
        });
    }

    /** What {@code key} says forgiving an apology ({@code moment}: but they want a moment first). */
    public static String forgiven(String key, boolean moment, int seed) {
        if (moment) {
            return pick(seed, switch (key) {
                case "eto" -> new String[] {"Sorry. Sorry, sorry, sorry. You say it a lot lately. ...Give me a minute. Then come back and say it better."};
                case "touka" -> new String[] {"...I know you are. Just. Give me a minute, okay? I'm not leaving. I'm just... a minute."};
                case "ken" -> new String[] {"I believe you. I just need a moment. That's all. Come back in a little while?"};
                case "general" -> new String[] {"...I heard you. Give me an hour. I'm not going anywhere. I only need to sign something very hard."};
                case "ayato" -> new String[] {"...I heard you. Go away for a bit. I'm not leaving. I just need to hit something first."};
                case "noro" -> new String[] {"*He turns away. Later.*"};
                default -> new String[] {"Give me a minute."};
            });
        }
        return pick(seed, switch (key) {
            case "eto" -> new String[] {"Hmph. Fine. You're forgiven. You're lucky you're cute.", "Sorry? ...Say it like you mean it. ...Okay. That was good. Come here.",
                "Apology accepted. I'm writing it down, though. In ink."};
            case "touka" -> new String[] {"...Fine. You're forgiven. Don't look so pleased.", "Ugh. I can't stay mad at you. It's annoying. Come here.",
                "You're sorry? ...Yeah. Me too. Let's forget it."};
            case "ken" -> new String[] {"It's okay. Really. I was never really angry.", "Thank you for saying it. That means more than you think.",
                "Already forgotten. Come here."};
            case "general" -> new String[] {"...Forgiven. Don't make a habit of it.", "I can't stay angry at you. It's most irregular.", "It's forgotten. Come here."};
            case "ayato" -> new String[] {"...Fine. Forgiven. Don't make it a habit.", "Tch. I can't stay mad at you. It's annoying.",
                "Sorry? ...Yeah. Okay. Come here, idiot."};
            case "noro" -> new String[] {"*A nod.*", "*He looks at you. Then nods.*"};
            default -> new String[] {"Okay."};
        });
    }

    /** What {@code key} says, still put out, when they come to talk (not cross enough to turn them away). */
    public static String putOut(String key, int seed) {
        return pick(seed, switch (key) {
            case "eto" -> new String[] {"Oh, it's you. The one who hits me. ...What?", "I'm not talking to you. ...I'm talking to you, but I'm not happy about it."};
            case "touka" -> new String[] {"What. I'm still mad. A little.", "Hmph. Say what you came to say."};
            case "ken" -> new String[] {"...Hi. I'm alright. Just a little hurt. Not the body kind.", "You came. ...Okay. I'm listening."};
            case "general" -> new String[] {"Yes? I'm still angry.", "Oh. You. ...Say what you came to say."};
            case "ayato" -> new String[] {"What. I'm still mad.", "Oh. It's you. ...Say what you came to say."};
            case "noro" -> new String[] {"*Silence. Even for him.*"};
            default -> new String[] {"Hm."};
        });
    }

    static String pick(int seed, String[] lines) {
        return lines[Math.floorMod(seed, lines.length)];
    }
}
