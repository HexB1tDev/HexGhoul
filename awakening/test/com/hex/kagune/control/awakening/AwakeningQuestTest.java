package com.hex.kagune.control.awakening;

/** Arming and the death trigger, without a server: Kaneki DNA, the quest's gate open, once, never into the void. */
public final class AwakeningQuestTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        check(AwakeningQuest.QUEST_ID.equals("the_dragon"), "the quest is the expansion's the_dragon");
        check(AwakeningQuest.advance(AwakeningStage.DORMANT, true, false) == AwakeningStage.DORMANT, "not before the quest is done");
        check(AwakeningQuest.advance(AwakeningStage.DORMANT, false, true) == AwakeningStage.DORMANT, "only Kaneki DNA arms the Dragon");
        check(AwakeningQuest.advance(AwakeningStage.DORMANT, true, true) == AwakeningStage.ARMED, "the gate arms it");
        check(AwakeningQuest.advance(AwakeningStage.QUEST, true, true) == AwakeningStage.ARMED, "an old in-progress stage arms too");
        check(AwakeningQuest.advance(AwakeningStage.ARMED, true, false) == AwakeningStage.ARMED, "stages never move backwards");
        check(AwakeningQuest.advance(AwakeningStage.FINAL, true, true) == AwakeningStage.FINAL, "final is permanent");

        check(AwakeningQuest.awakensOnDeath(AwakeningStage.ARMED, true, true, false), "an armed death awakens");
        check(AwakeningQuest.awakensOnDeath(AwakeningStage.DORMANT, true, true, false), "a death the same second the gate opened awakens");
        check(!AwakeningQuest.awakensOnDeath(AwakeningStage.ARMED, true, false, false), "the quest must be done");
        check(!AwakeningQuest.awakensOnDeath(AwakeningStage.ARMED, false, true, false), "Kaneki DNA only");
        check(!AwakeningQuest.awakensOnDeath(AwakeningStage.ARMED, true, true, true), "falling into the void is an ordinary death");
        check(!AwakeningQuest.awakensOnDeath(AwakeningStage.FINAL, true, true, false), "the Dragon happens once");
        check(!AwakeningQuest.awakensOnDeath(AwakeningStage.DRAGON, true, true, false), "never twice at once");
        check(AwakeningStage.of(99) == AwakeningStage.DORMANT && AwakeningStage.of(-1) == AwakeningStage.DORMANT, "corrupt stage data falls back");
        System.out.println("Verified: the Dragon arms on Kaneki DNA when its quest's gate opens, and wakes once, never into the void.");
    }
}
