package com.hex.kagune.control.awakening;

/**
 * When the Dragon wakes (pure). The quest itself is the expansion's {@code the_dragon} ("Kakuja V2: The Dragon", Kaneki DNA,
 * mastery 1000: kills in the Full Kakuja and brinks lived through in a kakuja, {@code control/quest/QuestBook}); done, it opens
 * {@code Gate.KANEKI_DRAGON}. From then on the next death of a Kaneki ghoul is the Dragon's, once.
 */
public final class AwakeningQuest {
    public static final String QUEST_ID = "the_dragon";
    public static final String TITLE = "Kakuja V2: The Dragon";

    private AwakeningQuest() {
    }

    /** The stage once the gate is known: a Kaneki ghoul whose Dragon gate is open is armed; never moves backwards. */
    public static AwakeningStage advance(AwakeningStage stage, boolean kaneki, boolean gateOpen) {
        if (!kaneki || !gateOpen) return stage;
        return stage == AwakeningStage.DORMANT || stage == AwakeningStage.QUEST ? AwakeningStage.ARMED : stage;
    }

    /** True if dying now should wake the Dragon instead: Kaneki DNA, the gate open, not yet woken, not a fall into the void. */
    public static boolean awakensOnDeath(AwakeningStage stage, boolean kaneki, boolean gateOpen, boolean inVoid) {
        if (!kaneki || !gateOpen || inVoid) return false;
        return stage == AwakeningStage.DORMANT || stage == AwakeningStage.QUEST || stage == AwakeningStage.ARMED;
    }
}
