package com.hex.kagune.control.npc;

/**
 * 1.3.14: everything a one-of-a-kind NPC says and offers (pure): its talk's pages and answers on the shared talk screen, its topics,
 * and every line its mind says unasked (greetings, the kiss, the hand, the fight, what it saw). One per {@link Persona}: Eto's is
 * {@link EtoTalk}, {@link EtoLines}, {@link EtoTopics} and {@link EtoInitiative} as they always were ({@link EtoScript}); every
 * companion after her has its own. The server and the mind never pick words themselves: they ask the script.
 */
public interface Script {
    // ---------------------------------------------------------------- the talk's pages

    /** The first page: {@code line} its words, and what can be said, by who they are to it. */
    EtoTalk.Page main(EtoTalk.Context c, String line);

    /** Its first words. */
    String greeting(EtoTalk.Context c);

    /** The topics page. */
    EtoTalk.Page talk(EtoTalk.Context c, String line);

    /** A topic brought up: its answer and what can be said back. */
    EtoTalk.Page topic(EtoTalk.Context c, EtoTopics.Topic topic);

    /** What was said back on {@code topic}. */
    EtoTalk.Reply follow(EtoTalk.Context c, EtoTopics.Topic topic, int index);

    /** The topic {@code id}, or null. */
    EtoTopics.Topic topicOf(String id);

    /** Where {@code topic} stands among its topics (its bits in the day's said-already mask). */
    int topicIndex(EtoTopics.Topic topic);

    /** Its partner's sweet page. */
    EtoTalk.Page sweet(EtoTalk.Context c, String line);

    /** Its answer to a sweet thing ({@code what}: the option key's word). */
    EtoTalk.Reply sweetly(EtoTalk.Context c, String what);

    /** The gifts page. */
    EtoTalk.Page gifts(EtoTalk.Context c, String line);

    /** Its answer to {@code choice}. */
    EtoTalk.Reply answer(EtoTalk.Context c, NpcTalk.Choice choice);

    /** The subtitle on the talk screen (who they are to it). */
    String title(EtoTalk.Context c);

    /** Asked what is on the gifts page. */
    String giftsAsked(int seed);

    /** Back from a topic, to the others. */
    String topicsAgain(int seed);

    /** Too busy fighting to talk. */
    String busy();

    // ---------------------------------------------------------------- what it says unasked

    String approach(EtoTalk.Context c);

    String welcome(EtoTalk.Context c);

    String kissed(int seed, boolean married);

    String held(int seed);

    String pulled(int seed);

    String hanging(int seed);

    String hugged(int seed);

    String handTaken(int seed);

    String handLet(int seed);

    String partnerHit(int seed, boolean married);

    String serious(int seed);

    String maniac(int seed);

    String protect(EtoTalk.Context c);

    String won(int seed);

    String puppy(EtoTalk.Context c, String kind, boolean sorry);

    String letter(EtoTalk.Context c, boolean sorry);

    String snub(int seed, boolean enemy);

    String ring(EtoTalk.Context c, Romance.Refusal refusal);

    String morning(EtoTalk.Context c);

    String conceived(EtoTalk.Context c);

    String pregnant(com.hex.kagune.control.family.Pregnancy.Stage stage, int seed);

    /** What it says on seeing {@code shown} ({@code home}: in its own place). */
    String saw(Display shown, EtoVoice voice, boolean home, int seed);

    /** What it says as it starts {@code act} unasked. */
    String initiative(EtoInitiative.Act act, int seed);

    /** What it says of a moment ({@link EtoInitiative.Moment}). */
    String moment(EtoInitiative.Moment moment, boolean together, int seed);

    /** 1.3.14: after a quick kiss it gave itself. */
    default String peck(int seed) {
        return this.kissed(seed, false);
    }

    /** 1.3.14: the line on screen while it waits on a kiss ({@code name}: what it is called). */
    default String kissWaiting(String name) {
        return name + " leans in, eyes closed...";
    }

    /** 1.3.14: as it walks up wanting a kiss (a hint; "" says nothing). */
    default String kissHint(int seed) {
        return "";
    }

    /** 1.3.14: night, and they asked for a baby: it comes to them. */
    default String babyNight(EtoTalk.Context c) {
        return "It's dark. You asked me for something. ...Come here.";
    }

    /** 1.3.14: what it says as a lesson lived through ends (the last words of the trial's cutscene). */
    default String trialWords(Teaching.Lesson lesson, EtoTalk.Context c) {
        return "";
    }
}
