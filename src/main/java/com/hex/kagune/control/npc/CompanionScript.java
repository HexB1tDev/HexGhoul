package com.hex.kagune.control.npc;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.3.14: the shape of a companion's talk after Eto (pure, tested): the same pages as hers ({@link EtoTalk}: who they are to it decides
 * the first page, "talk a while" opens the topics, its partner gets the sweet page, gifts get their own page when there are many), the
 * same road to dating (2.1.2: its own courtship's chapters, {@link Courtship}), and where hers has her pact and her surgery, a teacher's
 * lessons ({@link Teaching}). What it says is its own: a companion is this shape plus its words.
 */
abstract class CompanionScript implements Script {
    /** Every topic it can talk about. */
    protected abstract List<EtoTopics.Topic> topics();

    /** The sweet things its partner can say ({label, key}), three offered a time. */
    protected abstract String[][] sweetSays();

    /** What handing over its first errand's item is called, and its last one's. */
    protected abstract String errandLabel();

    protected abstract String flowerLabel();

    /** What asking for a lesson is called. */
    protected abstract String lessonLabel(EtoTalk.Context c);

    /** 2.1.2: its key (its courtship's chapters, {@link Courtship}). */
    protected abstract String key();

    /** Him or her (the kiss, the hug, being sweet). */
    protected abstract String her();

    /** Wife or husband. */
    protected abstract String spouse();

    /** 1.3.14: whether it would date them at all (Ken: women only); a friend and a teacher otherwise. */
    protected boolean romantic(EtoTalk.Context c) {
        return true;
    }

    // ---------------------------------------------------------------- the first page

    @Override
    public EtoTalk.Page main(EtoTalk.Context c, String line) {
        List<EtoTalk.Option> out = new ArrayList<>();
        EtoVoice voice = EtoVoice.of(c);
        if (!c.has(NpcBonds.ASKED)) {
            out.add(EtoTalk.Option.of(NpcTalk.Choice.WHO));
        }
        out.add(new EtoTalk.Option(NpcTalk.Choice.CHAT, voice.together() ? "Talk to me." : "", ""));
        if (voice == EtoVoice.CLAIMED) {
            // Someone else's: lessons, a word, and the door.
            this.lesson(c, out);
            out.add(EtoTalk.Option.of(NpcTalk.Choice.INSULT));
            out.add(EtoTalk.Option.of(NpcTalk.Choice.LEAVE));
            return new EtoTalk.Page(line, out);
        }
        if (voice.together()) {
            out.add(new EtoTalk.Option(NpcTalk.Choice.SWEET, c.married() ? "Be sweet to your " + this.spouse() + "." : "Be sweet to " + this.her() + ".", ""));
        }
        List<EtoTalk.Option> gifts = this.giftOptions(c);
        if (gifts.size() > 2) {
            out.add(new EtoTalk.Option(NpcTalk.Choice.SAY_8, "I brought you something...", "gifts"));
        } else {
            out.addAll(gifts);
        }
        this.lesson(c, out);
        Courtship.options(this.key(), c, this.romantic(c), out); // 2.1.2: its own courtship
        if (voice.together() || c.regard() >= NpcBonds.FRIENDLY) {
            out.add(EtoTalk.Option.of(c.following() ? NpcTalk.Choice.STAY : NpcTalk.Choice.FOLLOW));
        }
        out.add(voice.together() ? new EtoTalk.Option(NpcTalk.Choice.TEASE, "Tease " + this.her() + ".", "") : EtoTalk.Option.of(NpcTalk.Choice.INSULT));
        out.add(EtoTalk.Option.of(NpcTalk.Choice.LEAVE));
        return new EtoTalk.Page(line, out);
    }

    /**
     * A lesson. 1.3.29: always offered (everything taught, or known some other way, they say so: "You already know..."), so asking for
     * what they know already is answered, never hidden.
     */
    private void lesson(EtoTalk.Context c, List<EtoTalk.Option> out) {
        out.add(new EtoTalk.Option(NpcTalk.Choice.LESSON, this.lessonLabel(c), ""));
    }

    private List<EtoTalk.Option> giftOptions(EtoTalk.Context c) {
        List<EtoTalk.Option> out = new ArrayList<>();
        if (c.puppy() != null) {
            out.add(EtoTalk.Option.of(NpcTalk.Choice.PUPPY));
        }
        if (c.book()) {
            out.add(new EtoTalk.Option(NpcTalk.Choice.BOOK, this.errandLabel(), ""));
        }
        if (c.flower()) {
            out.add(new EtoTalk.Option(NpcTalk.Choice.FLOWER, this.flowerLabel(), ""));
        }
        if (c.meat()) {
            out.add(new EtoTalk.Option(NpcTalk.Choice.GIFT, "I brought you dinner. (the Ghoul Meat in your hand)", ""));
        }
        if (c.letter()) {
            out.add(EtoTalk.Option.of(NpcTalk.Choice.LETTER));
        }
        return out;
    }

    @Override
    public EtoTalk.Page gifts(EtoTalk.Context c, String line) {
        List<EtoTalk.Option> out = new ArrayList<>(this.giftOptions(c));
        out.add(EtoTalk.Option.of(NpcTalk.Choice.BACK));
        return new EtoTalk.Page(line, out);
    }

    // ---------------------------------------------------------------- talking a while

    @Override
    public EtoTalk.Page talk(EtoTalk.Context c, String line) {
        List<EtoTopics.Topic> open = new ArrayList<>();
        List<EtoTopics.Topic> pinned = new ArrayList<>();
        for (EtoTopics.Topic topic : this.topics()) {
            if (topic.when().test(c)) {
                (EtoTopics.pinned(topic) ? pinned : open).add(topic);
            }
        }
        List<EtoTalk.Option> out = new ArrayList<>();
        int room = NpcTalk.Choice.SAYS.length;
        int said = 0;
        for (EtoTopics.Topic topic : pinned) {
            if (said < room) {
                out.add(new EtoTalk.Option(NpcTalk.Choice.SAYS[said++], topic.prompt().apply(c), "topic:" + topic.id()));
            }
        }
        int start = open.isEmpty() ? 0 : Math.floorMod(c.seed(), open.size());
        for (int i = 0; i < open.size() && said < room; i++) {
            EtoTopics.Topic topic = open.get((start + i) % open.size());
            out.add(new EtoTalk.Option(NpcTalk.Choice.SAYS[said++], topic.prompt().apply(c), "topic:" + topic.id()));
        }
        out.add(EtoTalk.Option.of(NpcTalk.Choice.BACK));
        return new EtoTalk.Page(line, out);
    }

    @Override
    public EtoTalk.Page topic(EtoTalk.Context c, EtoTopics.Topic topic) {
        List<EtoTalk.Option> out = new ArrayList<>();
        List<EtoTopics.Follow> follows = topic.open(c);
        for (int i = 0; i < Math.min(NpcTalk.Choice.SAYS.length, follows.size()); i++) {
            out.add(new EtoTalk.Option(NpcTalk.Choice.SAYS[i], follows.get(i).prompt(), "follow:" + topic.id() + ":" + i));
        }
        out.add(EtoTalk.Option.of(NpcTalk.Choice.BACK));
        return new EtoTalk.Page(EtoLines.fill(c, topic.answer().apply(c)), out);
    }

    @Override
    public EtoTalk.Reply follow(EtoTalk.Context c, EtoTopics.Topic topic, int index) {
        List<EtoTopics.Follow> follows = topic.open(c);
        if (index < 0 || index >= follows.size()) {
            return new EtoTalk.Reply("What?", 0, EtoTalk.Effect.NONE);
        }
        EtoTalk.Reply reply = follows.get(index).reply().apply(c);
        return new EtoTalk.Reply(EtoLines.fill(c, reply.line()), reply.regard(), reply.effect(), reply.gesture());
    }

    @Override
    public EtoTopics.Topic topicOf(String id) {
        for (EtoTopics.Topic topic : this.topics()) {
            if (topic.id().equals(id)) {
                return topic;
            }
        }
        return null;
    }

    @Override
    public int topicIndex(EtoTopics.Topic topic) {
        return this.topics().indexOf(topic);
    }

    // ---------------------------------------------------------------- its partner's

    @Override
    public EtoTalk.Page sweet(EtoTalk.Context c, String line) {
        List<EtoTalk.Option> out = new ArrayList<>();
        out.add(new EtoTalk.Option(NpcTalk.Choice.KISS, "Kiss " + this.her() + ".", ""));
        out.add(new EtoTalk.Option(NpcTalk.Choice.HUG, "Hug " + this.her() + ".", ""));
        out.add(new EtoTalk.Option(NpcTalk.Choice.SAY_1, "Hold me.", "sweet:cling"));
        out.add(new EtoTalk.Option(NpcTalk.Choice.SAY_2, "Hold my hand.", "sweet:hand"));
        out.add(new EtoTalk.Option(NpcTalk.Choice.SAY_6, Courtship.DATES, "dates")); // 2.1.2
        String[][] says = this.sweetSays();
        int start = Math.floorMod(c.seed(), says.length);
        for (int i = 0; i < Math.min(3, says.length); i++) {
            String[] say = says[(start + i) % says.length];
            out.add(new EtoTalk.Option(NpcTalk.Choice.SAYS[i + 2], say[0], "sweet:" + say[1]));
        }
        EtoTalk.Option baby = EtoTalk.babyOption(c); // 2.5.7: here, not among the topics
        if (baby != null) {
            out.add(baby);
        }
        if (c.night()) {
            out.add(EtoTalk.Option.of(NpcTalk.Choice.NIGHT));
        }
        if (c.ring() && !c.married()) {
            out.add(EtoTalk.Option.of(NpcTalk.Choice.PROPOSE));
        }
        if (c.stage() == Romance.Stage.MARRIED) {
            out.add(EtoTalk.Option.of(NpcTalk.Choice.HOME)); // 1.3.21
        }
        out.add(EtoTalk.Option.of(NpcTalk.Choice.BACK));
        return new EtoTalk.Page(line, out);
    }

    // ---------------------------------------------------------------- shared helpers for its words

    static EtoTalk.Reply r(String line, int regard) {
        return new EtoTalk.Reply(line, regard, EtoTalk.Effect.NONE, EtoGesture.NONE);
    }

    static EtoTalk.Reply r(String line, int regard, EtoGesture gesture) {
        return new EtoTalk.Reply(line, regard, EtoTalk.Effect.NONE, gesture);
    }

    static EtoTalk.Reply r(String line, int regard, EtoTalk.Effect effect, EtoGesture gesture) {
        return new EtoTalk.Reply(line, regard, effect, gesture);
    }

    static String one(int seed, String... lines) {
        return lines[Math.floorMod(seed, lines.length)];
    }

    static String[] s(String... lines) {
        return lines;
    }

    static boolean together(EtoTalk.Context c) {
        return EtoVoice.of(c).together();
    }

    /** 2.1.2: what is said about its courtship ({@link Courtship#favour}): its chapters' words are its own, the rest given here. */
    static EtoTalk.Reply errand(EtoTalk.Context c, String key, String notYet, String asking, String dating, String taken, String married) {
        return Courtship.favour(key, c, notYet, asking, dating, taken, married);
    }

    /** Its first gift's item handed over (books, beans...): a gift, once a day. */
    static EtoTalk.Reply errandItem(EtoTalk.Context c, String none, String spoiled, String warm, String cool, EtoGesture gesture) {
        if (!c.book()) {
            return new EtoTalk.Reply(none, 0, EtoTalk.Effect.NONE);
        }
        if (c.giftedToday()) {
            return new EtoTalk.Reply(spoiled, 0, EtoTalk.Effect.NONE);
        }
        return new EtoTalk.Reply(together(c) ? warm : cool, EtoRules.GIFT, EtoTalk.Effect.BOOK, gesture);
    }

    /** Its second gift handed over (a flower, a rabbit's foot...): a gift, once a day. */
    static EtoTalk.Reply flowerGiven(EtoTalk.Context c, String none, String spoiled, String warm, String cool, EtoGesture gesture) {
        if (!c.flower()) {
            return new EtoTalk.Reply(none, 0, EtoTalk.Effect.NONE);
        }
        if (c.giftedToday()) {
            return new EtoTalk.Reply(spoiled, 0, EtoTalk.Effect.NONE);
        }
        return new EtoTalk.Reply(together(c) ? warm : cool, EtoRules.GIFT, EtoTalk.Effect.FLOWER, gesture);
    }

    /**
     * A lesson asked for: taught ({@link EtoTalk.Effect#LEARN}), lived through first ({@link EtoTalk.Effect#TORTURE}), or why not, each in
     * the teacher's words ({@code words}).
     */
    static EtoTalk.Reply lesson(EtoTalk.Context c, LessonWords words) {
        Teaching.View view = c.teaching();
        if (view.ready()) {
            Teaching.Lesson lesson = view.next();
            return new EtoTalk.Reply(words.teach(lesson, c), 0, lesson.kind() == Teaching.Kind.TRIAL ? EtoTalk.Effect.TORTURE : EtoTalk.Effect.LEARN,
                EtoGesture.NONE);
        }
        Teaching.Refusal refusal = view.refusal() == null ? Teaching.Refusal.ALL_TAUGHT : view.refusal();
        return new EtoTalk.Reply(words.refuse(refusal, view, c), 0, EtoTalk.Effect.NONE, EtoGesture.NONE);
    }

    /** How a teacher says it. */
    interface LessonWords {
        String teach(Teaching.Lesson lesson, EtoTalk.Context c);

        String refuse(Teaching.Refusal refusal, Teaching.View view, EtoTalk.Context c);
    }
}
