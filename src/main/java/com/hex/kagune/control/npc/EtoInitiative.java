package com.hex.kagune.control.npc;

/**
 * 1.3.12: what Eto does with her partner unasked (pure), by her mood: takes their hand because she wants it, winds round their arm, hugs
 * them out of nowhere, leans on them, hums at them, reads to them, dances; and what she says as she does. Never more than once every
 * {@link #EVERY} ticks, only close by, never while she is already holding on. Built for every partner to come: a mood in, a deed out.
 */
public final class EtoInitiative {
    /** What she does. */
    public enum Act {
        NONE(EtoGesture.NONE),
        HAND(EtoGesture.HAND),
        CLING(EtoGesture.CLING),
        HUG(EtoGesture.HUG),
        LEAN(EtoGesture.LEAN),
        HUM(EtoGesture.HUM),
        READ(EtoGesture.READ),
        DANCE(EtoGesture.DANCE);

        /** The gesture she walks up to them for. */
        public final EtoGesture gesture;

        Act(EtoGesture gesture) {
            this.gesture = gesture;
        }
    }

    /** At least this long between two things she starts herself (game ticks: a minute and a half), and only this close (blocks). */
    public static final long EVERY = 1800L;
    public static final double NEAR = 10.0;

    private EtoInitiative() {
    }

    /**
     * Her pick: {@code mood} hers now, {@code distance} to her partner, {@code holding} whether she already holds on, {@code since} ticks
     * since the last thing she started, {@code roll} in [0, 1) (a chance each time she is asked: about every second while near), {@code
     * roll2} in [0, 1) which of her mood's deeds.
     */
    public static Act pick(EtoMood mood, double distance, boolean holding, long since, double roll, double roll2) {
        if (holding || distance > NEAR || since < EVERY) {
            return Act.NONE;
        }
        double chance = switch (mood) {
            case CLINGY, LONELY -> 0.02;
            case AFFECTIONATE, GIDDY -> 0.015;
            case HAPPY -> 0.008;
            case BORED -> 0.006;
            case SLEEPY -> 0.004;
            default -> 0.0;
        };
        if (roll >= chance) {
            return Act.NONE;
        }
        Act[] deeds = switch (mood) {
            case CLINGY, LONELY -> new Act[] {Act.HAND, Act.HAND, Act.CLING, Act.HUG};
            case AFFECTIONATE -> new Act[] {Act.HAND, Act.LEAN, Act.HUG, Act.HUM};
            case GIDDY -> new Act[] {Act.DANCE, Act.HAND, Act.HUM};
            case HAPPY -> new Act[] {Act.HUM, Act.READ, Act.HAND};
            case BORED -> new Act[] {Act.READ, Act.DANCE, Act.HAND};
            case SLEEPY -> new Act[] {Act.LEAN, Act.CLING};
            default -> new Act[] {Act.NONE};
        };
        return deeds[(int)Math.min(deeds.length - 1, Math.floor(roll2 * deeds.length))];
    }

    /** What she says as she does it. */
    public static String line(Act act, int seed) {
        return switch (act) {
            case HAND -> pick(seed, "Your hand. Give it. ...I don't need a reason.", "Hold still. I'm taking this.",
                "Don't look at me like that. I just wanted your hand.", "Mine. ...Your hand, I mean. Also you.");
            case CLING -> pick(seed, "Don't move. I'm comfortable.", "You're warm. That's all. Don't read into it.", "Stay. Just like this.");
            case HUG -> pick(seed, "Come here. No reason. Shut up.", "I missed you. Just now. In the last five minutes. It's embarrassing.",
                "Hold still. This is a hug. You're being hugged.");
            case LEAN -> pick(seed, "I'm leaning on you. You're a very good wall.", "Let me rest here a moment.", "Mm. Don't go anywhere.");
            case HUM -> pick(seed, "*hums something old and sad, smiling at you*", "*hums at you until you notice*", "Do you know this one? ...You will.");
            case READ -> pick(seed, "Listen to this line. No, really listen.", "Sit. I'm reading to you. You don't get a say.",
                "This page made me think of you. Don't ask why. It's a murder scene.");
            case DANCE -> pick(seed, "Dance with me. No one's watching. ...The books don't count.", "Come on. One song. I'll lead.",
                "Spin me. Gently. ...Okay, not gently.");
            default -> "";
        };
    }

    /** 1.3.12: what she says, unasked, at a moment (a kill near her, their wound, the rain, the sunset, the first star). */
    public enum Moment {
        KILL,
        HURT,
        RAIN,
        DUSK,
        DAWN
    }

    public static String moment(Moment moment, boolean together, int seed) {
        return switch (moment) {
            case KILL -> together ? pick(seed, "Messy. ...I liked watching you.", "You fight like you're angry at the world. Me too.",
                "Did it taste of anything? ...Just asking.") : pick(seed, "Charming.", "Do that somewhere else.");
            case HURT -> together ? pick(seed, "You're bleeding. Come here. Now.", "Who did that? Point. I'll do the rest.",
                "Don't you dare die before I finish my book.") : pick(seed, "You're hurt. Careful.", "Bleed somewhere else, please.");
            case RAIN -> together ? pick(seed, "Rain. Good. Stay inside with me.", "Listen to it on the windows. Best sound in the world.",
                "Rain means nobody's coming. Just us.") : pick(seed, "Rain. The Library likes it. So do I.", "Hm. Rain.");
            case DUSK -> together ? pick(seed, "The sun's going. My favourite part. ...Of the day. You're my favourite part of the rest.",
                "Night soon. My eye will open. Don't be scared.") : pick(seed, "Night's coming. Run along, little human.", "Dusk. My hour.");
            case DAWN -> together ? pick(seed, "Morning already? ...Stay a bit longer.", "Look. The light. Pretty, isn't it? Like you. Don't tell anyone.")
                : pick(seed, "Morning. How dull.", "The sun again. Ugh.");
        };
    }

    static String pick(int seed, String... lines) {
        return lines[Math.floorMod(seed, lines.length)];
    }
}
