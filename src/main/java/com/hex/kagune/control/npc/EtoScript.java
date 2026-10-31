package com.hex.kagune.control.npc;

/**
 * 1.3.14: Eto's {@link Script}: her talk and lines exactly as they were ({@link EtoTalk}, {@link EtoLines}, {@link EtoTopics},
 * {@link EtoInitiative}), only reached through the one door every companion shares.
 */
final class EtoScript implements Script {
    @Override
    public EtoTalk.Page main(EtoTalk.Context c, String line) {
        return EtoTalk.main(c, line);
    }

    @Override
    public String greeting(EtoTalk.Context c) {
        return EtoTalk.greeting(c);
    }

    @Override
    public EtoTalk.Page talk(EtoTalk.Context c, String line) {
        return EtoTalk.talk(c, line);
    }

    @Override
    public EtoTalk.Page topic(EtoTalk.Context c, EtoTopics.Topic topic) {
        return EtoTalk.topic(c, topic);
    }

    @Override
    public EtoTalk.Reply follow(EtoTalk.Context c, EtoTopics.Topic topic, int index) {
        return EtoTalk.follow(c, topic, index);
    }

    @Override
    public EtoTopics.Topic topicOf(String id) {
        return EtoTopics.get(id);
    }

    @Override
    public int topicIndex(EtoTopics.Topic topic) {
        return EtoTopics.index(topic);
    }

    @Override
    public EtoTalk.Page sweet(EtoTalk.Context c, String line) {
        return EtoTalk.sweet(c, line);
    }

    @Override
    public EtoTalk.Reply sweetly(EtoTalk.Context c, String what) {
        return EtoTalk.sweetly(c, what);
    }

    @Override
    public EtoTalk.Page gifts(EtoTalk.Context c, String line) {
        return EtoTalk.gifts(c, line);
    }

    @Override
    public EtoTalk.Reply answer(EtoTalk.Context c, NpcTalk.Choice choice) {
        return EtoTalk.answer(c, choice);
    }

    @Override
    public String title(EtoTalk.Context c) {
        return EtoLines.title(c);
    }

    @Override
    public String giftsAsked(int seed) {
        return EtoLines.one(seed, "Oh? For me? What is it?", "A present? Show me.");
    }

    @Override
    public String topicsAgain(int seed) {
        return EtoLines.one(seed, "What else?", "Mm. Something else, then.", "Go on.");
    }

    @Override
    public String busy() {
        return EtoRules.NAME + " is busy. Very busy. With teeth.";
    }

    @Override
    public String approach(EtoTalk.Context c) {
        return EtoLines.approach(c);
    }

    @Override
    public String welcome(EtoTalk.Context c) {
        return EtoLines.welcome(c);
    }

    @Override
    public String kissed(int seed, boolean married) {
        return EtoLines.kissed(seed, married);
    }

    @Override
    public String held(int seed) {
        return EtoLines.held(seed);
    }

    @Override
    public String pulled(int seed) {
        return EtoLines.pulled(seed);
    }

    @Override
    public String hanging(int seed) {
        return EtoLines.hanging(seed);
    }

    @Override
    public String hugged(int seed) {
        return EtoLines.hugged(seed);
    }

    @Override
    public String handTaken(int seed) {
        return EtoLines.handTaken(seed);
    }

    @Override
    public String handLet(int seed) {
        return EtoLines.handLet(seed);
    }

    @Override
    public String partnerHit(int seed, boolean married) {
        return EtoLines.partnerHit(seed, married);
    }

    @Override
    public String serious(int seed) {
        return EtoLines.serious(seed);
    }

    @Override
    public String maniac(int seed) {
        return EtoLines.maniac(seed);
    }

    @Override
    public String protect(EtoTalk.Context c) {
        return EtoLines.protect(c);
    }

    @Override
    public String won(int seed) {
        return EtoLines.won(seed);
    }

    @Override
    public String puppy(EtoTalk.Context c, String kind, boolean sorry) {
        return EtoLines.puppy(c, kind, sorry);
    }

    @Override
    public String letter(EtoTalk.Context c, boolean sorry) {
        return EtoLines.letter(c, sorry);
    }

    @Override
    public String snub(int seed, boolean enemy) {
        return EtoLines.snub(seed, enemy);
    }

    @Override
    public String ring(EtoTalk.Context c, Romance.Refusal refusal) {
        return EtoLines.ring(c, refusal);
    }

    @Override
    public String babyNight(EtoTalk.Context c) {
        return EtoLines.nightAsk(c);
    }

    @Override
    public String morning(EtoTalk.Context c) {
        return EtoLines.morning(c);
    }

    @Override
    public String conceived(EtoTalk.Context c) {
        return EtoLines.conceived(c);
    }

    @Override
    public String pregnant(com.hex.kagune.control.family.Pregnancy.Stage stage, int seed) {
        return EtoLines.pregnant(stage, seed);
    }

    @Override
    public String saw(Display shown, EtoVoice voice, boolean home, int seed) {
        return EtoLines.saw(shown, voice, home, seed);
    }

    @Override
    public String initiative(EtoInitiative.Act act, int seed) {
        return EtoInitiative.line(act, seed);
    }

    @Override
    public String moment(EtoInitiative.Moment moment, boolean together, int seed) {
        return EtoInitiative.moment(moment, together, seed);
    }
}
