package com.hex.kagune.control.family;

import com.hex.kagune.control.npc.NpcTalk;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 2.0.5: the choices on a child's talk page (pure, tested): its parents' pages (the main one, play, asleep) as before 2.0.5 (moved here
 * from {@code ChildTalk}), and a stranger's page: someone who is not its parent gets gentle choices only (talk, wave, a high five,
 * offered food, "Where are your parents?"), never what is a parent's (carry, a hand, follow, teach, bed, home, feed...). Nothing on any
 * page is ever romance, for anyone, at any age (the owner: "NOT DATEABLE"): {@link #romantic} and the tests guard it.
 */
public final class ChildPage {
    /** One choice: its key (what {@code ChildTalk.heard} does) and the words shown. */
    public record Choice(String key, String label) {
    }

    /** A stranger's keys. */
    public static final String CHAT = "x:chat";
    public static final String WAVE = "x:wave";
    public static final String FIVE = "x:five";
    public static final String TREAT = "x:treat";
    public static final String PARENTS = "x:parents";
    public static final Set<String> STRANGER = Set.of(CHAT, WAVE, FIVE, TREAT, PARENTS);

    /** What only its parents may do (and the scene and moment keys: "s:", "t:", "m:"). "back" is anyone's (a sleeping child's page). */
    public static final Set<String> PARENT_ONLY = Set.of("up", "down", "play", "shoulders", "teach", "talk", "topics", "follow", "stay", "hand", "feed",
        "bed", "home", "tuck", "stroke");

    /** Every key a parent's page may have (besides "s:", "t:", "m:"). */
    public static final Set<String> FAMILY = Set.of("up", "down", "play", "shoulders", "teach", "talk", "topics", "follow", "stay", "hand", "feed",
        "bed", "home", "tuck", "stroke", "back",
        "more"); // 4.1.15: the play page's second page of games

    /** Romance, as a key or a scene effect: never on a child's page. */
    public static final Set<String> ROMANCE_KEYS = Set.of("kiss", "date", "confess", "marry", "propose", "ask_out", "flirt", "night", "sweet", "tease",
        "letter", "invite", "romance", "court", "love");

    /** The talk's own romance choices (companions' and villagers'): never sent for a child. */
    public static final Set<NpcTalk.Choice> ROMANCE_CHOICES = Set.of(NpcTalk.Choice.ASK_OUT, NpcTalk.Choice.KISS, NpcTalk.Choice.HUG,
        NpcTalk.Choice.SWEET, NpcTalk.Choice.PROPOSE, NpcTalk.Choice.NIGHT, NpcTalk.Choice.TEASE, NpcTalk.Choice.LETTER, NpcTalk.Choice.INVITE,
        NpcTalk.Choice.FLOWER, NpcTalk.Choice.BOOK, NpcTalk.Choice.SURGERY, NpcTalk.Choice.HOME);

    /** Words that would make a line romance (a parent's "Kiss it better" on a scraped knee is not: these are). */
    static final String[] ROMANCE_WORDS = {"go out with me", "go out with you", "ask you out", "marry me", "marry you", "date me", "a date", "on a date",
        "be mine", "girlfriend", "boyfriend", "kiss me", "kiss you", "make out", "flirt", "my feelings for you", "in love with you", "fall for you",
        "romance", "romantic", "gorgeous", "sexy", "hot stuff", "handsome stranger", "cutie", "wink"};

    private ChildPage() {
    }

    /** True if {@code text} reads as romance. */
    public static boolean romantic(String text) {
        String t = text == null ? "" : text.toLowerCase(Locale.ROOT);
        for (String word : ROMANCE_WORDS) {
            if (t.contains(word)) {
                return true;
            }
        }
        return false;
    }

    /** True if {@code key} is only ever its parents'. */
    public static boolean parentOnly(String key) {
        return key == null || PARENT_ONLY.contains(key) || key.startsWith("m:") || key.startsWith("s:") || key.startsWith("t:");
    }

    /** What a parent's main page holds: {@code carrying} it in their arms (how: {@code ride}); its state; food in hand; bedtime at home. */
    public record Parent(boolean carrying, int ride, boolean following, boolean food, boolean bedtime, boolean away) {
    }

    /** A parent's main page. */
    public static List<Choice> parent(ChildStage stage, Parent p) {
        return parent(stage, p, true);
    }

    /** 4.1.15: a parent's main page for a {@code girl} or a boy: no hand to hold for a son from his teens on ({@link ChildMoment#touched}). */
    public static List<Choice> parent(ChildStage stage, Parent p, boolean girl) {
        List<Choice> out = new ArrayList<>();
        if (stage.carried() || p.carrying()) {
            add(out, p.carrying() ? "down" : "up", p.carrying() ? "Put you down." : "Pick up.");
        }
        add(out, "play", stage.older() ? "Want to hang out?" : "Let's play!");
        if (stage.learns()) {
            add(out, "teach", "Let me teach you to fight.");
        }
        // 1.3.29: talking, at every age (a baby is talked to; it answers its own way).
        add(out, "talk", switch (stage) {
            case BABY -> "Talk to the baby.";
            case TODDLER -> "Tell me something!";
            case CHILD -> "How are you?";
            case TEEN, ADULT -> "Talk to me.";
        });
        if (stage.walks()) {
            add(out, p.following() ? "stay" : "follow", p.following() ? "Wait here." : "Follow me.");
        }
        // 1.3.21: hand in hand; fed; to bed; home.
        boolean handed = p.carrying() && p.ride() == ChildRules.RIDE_HAND;
        if (ChildRules.hand(stage) && (!p.carrying() && ChildMoment.touched(stage, girl) || handed)) {
            add(out, handed ? "down" : "hand", handed ? "Let go." : "Hold my hand.");
        }
        if (p.food()) {
            add(out, "feed", "Here, eat this. (the food in your hand)");
        }
        if (p.bedtime()) {
            add(out, "bed", "Bedtime.");
        }
        // 2.3.8 (the owner: "there's no button for the kid to send them home"): always, once it walks (before, only when it was out away
        // from home: never while it followed them about near it).
        if (p.away() || stage.walks()) {
            add(out, "home", "Go home.");
        }
        return out;
    }

    /** A parent's play page: the moments its stage can have, its shoulders, back. */
    public static List<Choice> play(ChildStage stage, boolean carrying, int ride) {
        return play(stage, carrying, ride, 1);
    }

    /**
     * 4.1.15 (the owner: "add more kid interactions"): the play page {@code page} (1 or 2): when its stage has more games than one page holds
     * with its shoulders and back, the first page's last game slot leads on to the rest ({@code more}), and the second page back to the first
     * ({@code play}).
     */
    public static List<Choice> play(ChildStage stage, boolean carrying, int ride, int page) {
        return play(stage, true, carrying, ride, page);
    }

    /** 4.1.15: the play page for a {@code girl} or a boy (a son from his teens on: no hugs, pats, nuzzles or dances, {@link ChildMoment#fits}). */
    public static List<Choice> play(ChildStage stage, boolean girl, boolean carrying, int ride, int page) {
        List<ChildMoment> games = new ArrayList<>();
        for (ChildMoment moment : ChildMoment.PLAY) {
            if (moment.fits(stage, girl)) {
                games.add(moment);
            }
        }
        boolean shoulders = ChildRules.shoulders(stage);
        int room = MOST - 1 - (shoulders ? 1 : 0);
        boolean split = games.size() > room;
        List<Choice> out = new ArrayList<>();
        if (split && page >= 2) {
            for (ChildMoment moment : games.subList(room - 1, games.size())) {
                add(out, "m:" + moment.name(), moment.label());
            }
            add(out, "play", "...The other games.");
            add(out, "back", "...");
            return out;
        }
        for (ChildMoment moment : split ? games.subList(0, room - 1) : games) {
            add(out, "m:" + moment.name(), moment.label());
        }
        if (split) {
            add(out, "more", "More games...");
        }
        if (shoulders) {
            boolean riding = carrying && ride == ChildRules.RIDE_SHOULDERS;
            add(out, riding ? "down" : "shoulders", riding ? "Down you get." : "Up on my shoulders!");
        }
        add(out, "back", "...");
        return out;
    }

    /** A sleeping child's page: its parents may tuck it in ({@code free}: nothing else under way) and stroke its hair; anyone may hush it. */
    public static List<Choice> asleep(boolean parent, boolean free) {
        return asleep(parent, free, true);
    }

    /** 4.1.15: the same, {@code touched} as {@link ChildMoment#touched} has it: a son from his teens on doesn't have his hair stroked. */
    public static List<Choice> asleep(boolean parent, boolean free, boolean touched) {
        List<Choice> out = new ArrayList<>();
        if (parent && free) {
            add(out, "tuck", "*tuck them in*");
        }
        if (parent && touched) {
            add(out, "stroke", "*stroke their hair*");
        }
        add(out, "back", "Go back to sleep.");
        return out;
    }

    /** Someone else's child: gentle choices only, by its age ({@code food}: they hold food out). */
    public static List<Choice> stranger(ChildStage stage, boolean food) {
        List<Choice> out = new ArrayList<>();
        add(out, CHAT, switch (stage) {
            case BABY -> "Hello, little one.";
            case TODDLER -> "Hi there, little one!";
            case CHILD -> "Hi! What's your name?";
            case TEEN -> "Hey. How's it going?";
            case ADULT -> "Hello. Nice to meet you.";
        });
        add(out, WAVE, stage == ChildStage.BABY ? "*wiggle your fingers at it*" : "*wave*");
        if (ChildMoment.HIGH_FIVE.fits(stage)) {
            add(out, FIVE, "High five?");
        }
        if (food && stage != ChildStage.BABY) {
            add(out, TREAT, stage.older() ? "Want some? (the food in your hand)" : "Want a treat? (the food in your hand)");
        }
        add(out, PARENTS, stage.older() ? "Are your parents around?" : stage == ChildStage.BABY ? "Where's your mommy?" : "Where are your parents?");
        return out;
    }

    /** The choices sent for a page of {@code n} answers: its reply slots, then goodbye. Never a romance choice. */
    public static int[] choices(int n) {
        int[] out = new int[n + 1];
        for (int i = 0; i < n; i++) {
            out[i] = NpcTalk.Choice.SAYS[i].ordinal();
        }
        out[n] = NpcTalk.Choice.LEAVE.ordinal();
        return out;
    }

    /** Most answers on a page: the reply slots (goodbye is its own). */
    public static final int MOST = NpcTalk.Choice.SAYS.length;

    private static void add(List<Choice> out, String key, String label) {
        if (out.size() < MOST) {
            out.add(new Choice(key, label));
        }
    }
}
