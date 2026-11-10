package com.hex.kagune.control.npc;

import java.util.List;

/**
 * 4.0.0 (The Great Exploration): Noro's words (pure, tested), what few there are. The owner: "extremely quiet, unsettling, and difficult
 * to read. Avoid giving him an excessively talkative personality ... limited speech, deliberate movements, and an intimidating presence";
 * then: "very brief, barely talks". Aogiri Tree's, masked; he teaches the Bikaku, all of it, to those who feed it. No romance: a friend at
 * most, and a teacher (he is never asked out, and never asks). Most of what he "says" is what he does, told in asterisks.
 */
final class NoroScript extends CompanionScript {
    @Override
    protected String key() {
        return "noro";
    }

    @Override
    protected String her() {
        return "him";
    }

    @Override
    protected String spouse() {
        return "husband";
    }

    /** Never. */
    @Override
    protected boolean romantic(EtoTalk.Context c) {
        return false;
    }

    @Override
    protected String errandLabel() {
        return "I brought you meat. (the Ghoul Meat in your hand)";
    }

    @Override
    protected String flowerLabel() {
        return "I brought you a bone. (the one in your hand)";
    }

    @Override
    protected String lessonLabel(EtoTalk.Context c) {
        return c.teaching().refusal() == Teaching.Refusal.HUMAN ? "Teach me." : "Teach me. (the worms)";
    }

    @Override
    public String title(EtoTalk.Context c) {
        return EtoVoice.of(c) == EtoVoice.WARY ? "Aogiri Tree" : "Noro";
    }

    @Override
    public String greeting(EtoTalk.Context c) {
        int seed = c.seed();
        return switch (EtoVoice.of(c)) {
            case WARY -> one(seed, "*He turns his head. Only his head.*", "...", "*The mask tilts. He waits.*");
            case FRIENDLY -> one(seed, "...", "*A nod.*", "...You.");
            default -> one(seed, "...You.", "*He stops what he is doing, and looks at you.*", "...Sit.");
        };
    }

    @Override
    public EtoTalk.Reply answer(EtoTalk.Context c, NpcTalk.Choice choice) {
        int seed = c.seed();
        return switch (choice) {
            case WHO -> new EtoTalk.Reply("...Noro.", c.has(NpcBonds.ASKED) ? 0 : EtoRules.WHO, EtoTalk.Effect.NONE);
            case CHAT -> new EtoTalk.Reply(one(seed, "*He listens. He says nothing.*", "...", "*He sits. You talk. He doesn't.*"),
                c.chattedToday() ? 0 : EtoRules.CHAT, EtoTalk.Effect.TALK);
            case BOOK -> errandItem(c, "...None.", "...Later.", "*He takes it. He eats it, mask and all, somehow.* ...Good.",
                "*He takes it without a word.*", EtoGesture.NONE);
            case FLOWER -> flowerGiven(c, "...", "...Enough.", "*He turns the bone over in his fingers.* ...", "*He keeps it.*", EtoGesture.TILT);
            case GIFT -> c.meat() ? new EtoTalk.Reply("*He takes it.* ...More.", c.giftedToday() ? 0 : EtoRules.GIFT, EtoTalk.Effect.MEAT,
                EtoGesture.NONE) : r("...", 0);
            case PUPPY -> c.puppy() == null ? r("...", 0) : new EtoTalk.Reply(this.puppy(c, c.puppy(), false), 4, EtoTalk.Effect.PUPPY, EtoGesture.TILT);
            case LETTER -> c.letter() ? new EtoTalk.Reply(this.letter(c, false), c.giftedToday() ? 0 : 2, EtoTalk.Effect.LETTER, EtoGesture.TILT)
                : r("...", 0);
            case LESSON -> lesson(c, LESSONS);
            case FAVOUR -> r("...No.", 0);
            case ASK_OUT -> r("...No.", 0);
            case KISS, HUG, SWEET, NIGHT -> r("*He doesn't move. He doesn't answer.*", -1);
            case PROPOSE -> r("...No.", 0);
            case HOME -> r("...", 0);
            case FOLLOW -> new EtoTalk.Reply(one(seed, "*He follows.*", "..."), 0, EtoTalk.Effect.FOLLOW);
            case STAY -> new EtoTalk.Reply(one(seed, "*He stays.*", "..."), 0, EtoTalk.Effect.STAY);
            case DECLINE -> r("...", 0);
            case TEASE -> new EtoTalk.Reply("*He looks at you for a long time.*", 0, EtoTalk.Effect.NONE, EtoGesture.TILT);
            case INSULT -> NpcBonds.clamp(c.regard() + EtoRules.INSULT) <= NpcBonds.ENEMY
                ? new EtoTalk.Reply("*Something moves under his coat.*", EtoRules.INSULT, EtoTalk.Effect.ANGER, EtoGesture.TILT)
                : new EtoTalk.Reply("*He turns away.*", EtoRules.INSULT, EtoTalk.Effect.CLOSE, EtoGesture.TILT);
            case BACK -> new EtoTalk.Reply("...", 0, EtoTalk.Effect.MAIN);
            case LEAVE -> new EtoTalk.Reply("", 0, EtoTalk.Effect.CLOSE);
            default -> r("...", 0);
        };
    }

    /** How he teaches: by showing. A word or two; the worm does the rest. */
    static final LessonWords LESSONS = new LessonWords() {
        @Override
        public String teach(Teaching.Lesson lesson, EtoTalk.Context c) {
            return switch (lesson.gate()) {
                case BIKAKU_WORMS -> "*He opens his coat. Something moves under it. Then two of them. Then three.* ...Like this. (The Worms: Twin "
                    + "and Three Worms, Ravenous Pursuit, Carrion Eruption, Monstrous Regeneration.)";
                case BIKAKU_WALKER -> "...Stand. On them.";
                case BIKAKU_KAKUJA -> "...Let it eat you.";
                case BIKAKU_DEVOURING -> "*His worm uncoils, slowly, and swallows a crow whole.* ...Whole. (Grand Devouring: a body at a heart or "
                    + "less, within 10 blocks, swallowed whole.)";
                case BIKAKU_POTENTIAL -> "...Come apart. Come back.";
                default -> "... (" + lesson.name() + ")";
            };
        }

        @Override
        public String refuse(Teaching.Refusal refusal, Teaching.View view, EtoTalk.Context c) {
            return switch (refusal) {
                case HUMAN -> "...Food.";
                case WRONG_PATH -> "...Not a worm.";
                case WEAK -> "...Not yet. (" + view.next().name() + ": mastery " + view.need() + ")";
                case BEFORE -> "... (" + (view.before() == null ? "what comes before" : view.before().displayName()) + " first)";
                case ALL_TAUGHT -> "...Nothing left.";
            };
        }
    };

    @Override
    public String trialWords(Teaching.Lesson lesson, EtoTalk.Context c) {
        return switch (lesson.gate()) {
            case BIKAKU_WALKER -> "Noro is standing over you when you get up. On legs that are not yours.\n\"...Good.\"";
            case BIKAKU_POTENTIAL -> "Every piece of you is back where it belongs. Noro has not moved.\n\"...Again. Whenever.\"";
            default -> "Noro is crouched beside you, mask tilted, when it lets you go.\n\"...Good.\"";
        };
    }

    @Override
    protected String[][] sweetSays() {
        return new String[][] {{"...", "quiet"}};
    }

    @Override
    public EtoTalk.Reply sweetly(EtoTalk.Context c, String what) {
        return r("...", 0);
    }

    @Override
    public String giftsAsked(int seed) {
        return "...";
    }

    @Override
    public String topicsAgain(int seed) {
        return "...";
    }

    @Override
    public String busy() {
        return "Noro is eating.";
    }

    @Override
    public String approach(EtoTalk.Context c) {
        return one(c.seed(), "...", "*He is standing behind you. You didn't hear him come.*", "...You.");
    }

    @Override
    public String welcome(EtoTalk.Context c) {
        return "...";
    }

    @Override
    public String kissed(int seed, boolean married) {
        return "...";
    }

    @Override
    public String held(int seed) {
        return "...";
    }

    @Override
    public String pulled(int seed) {
        return "...";
    }

    @Override
    public String hanging(int seed) {
        return "...";
    }

    @Override
    public String hugged(int seed) {
        return "*He doesn't move.*";
    }

    @Override
    public String handTaken(int seed) {
        return "*He looks at your hand. Then away.*";
    }

    @Override
    public String handLet(int seed) {
        return "...";
    }

    @Override
    public String partnerHit(int seed, boolean married) {
        return "*His head turns, slowly.*";
    }

    @Override
    public String serious(int seed) {
        return one(seed, "...", "*The worms come out. All three.*");
    }

    @Override
    public String maniac(int seed) {
        return one(seed, "...", "*It keeps eating.*");
    }

    @Override
    public String protect(EtoTalk.Context c) {
        return "...Mine.";
    }

    @Override
    public String won(int seed) {
        return one(seed, "...", "*He eats.*");
    }

    @Override
    public String puppy(EtoTalk.Context c, String kind, boolean sorry) {
        return "*He looks at it for a long time. It does not move either.*";
    }

    @Override
    public String letter(EtoTalk.Context c, boolean sorry) {
        return "*He takes it. He doesn't open it. He keeps it.*";
    }

    @Override
    public String snub(int seed, boolean enemy) {
        return enemy ? "Noro's worms stir under his coat." : "Noro doesn't answer.";
    }

    @Override
    public String ring(EtoTalk.Context c, Romance.Refusal refusal) {
        return "...No.";
    }

    @Override
    public String morning(EtoTalk.Context c) {
        return "Noro is where he was.\n\"...\"";
    }

    @Override
    public String conceived(EtoTalk.Context c) {
        return "...";
    }

    @Override
    public String pregnant(com.hex.kagune.control.family.Pregnancy.Stage stage, int seed) {
        return "...";
    }

    @Override
    public String saw(Display shown, EtoVoice voice, boolean home, int seed) {
        return switch (shown) {
            case NONE -> "";
            case EYES, KAGUNE -> "...";
            case KAKUJA -> "...Hungry.";
            case DREAD -> "*He steps back. Once.*";
        };
    }

    @Override
    public String dread(FinalSight sight, EtoVoice voice, boolean home, int seed) {
        return sight == FinalSight.NONE ? "" : one(seed, "...", "*He watches. He does not run.*");
    }

    @Override
    public EtoGesture dreadGesture(FinalSight sight, boolean ours) {
        return sight == FinalSight.NONE ? EtoGesture.NONE : EtoGesture.TILT;
    }

    @Override
    public String initiative(EtoInitiative.Act act, int seed) {
        return "";
    }

    @Override
    public String moment(EtoInitiative.Moment moment, boolean together, int seed) {
        return switch (moment) {
            case KILL -> "...Eat.";
            case HURT -> "...";
            default -> "";
        };
    }

    static final List<EtoTopics.Topic> TOPICS = List.of(
        new EtoTopics.Topic("mask", c -> true, c -> "Why the mask?",
            c -> "...",
            List.of(new EtoTopics.Follow("Take it off?", c -> r("*He doesn't.*", 0, EtoGesture.TILT)))),
        new EtoTopics.Topic("hungry", c -> true, c -> "Are you hungry?",
            c -> "...Always.",
            List.of(new EtoTopics.Follow("For what?", c -> r("*He looks at you. A long time.*", 0, EtoGesture.TILT)))),
        new EtoTopics.Topic("worms", c -> c.ghoul(), c -> "What are those, under your coat?",
            c -> "...Bikaku.",
            List.of(new EtoTopics.Follow("Teach me.", c -> r("...Ask.", 0, EtoGesture.NONE)))));

    @Override
    protected List<EtoTopics.Topic> topics() {
        return TOPICS;
    }
}
