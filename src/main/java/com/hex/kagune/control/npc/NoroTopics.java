package com.hex.kagune.control.npc;

import static com.hex.kagune.control.npc.CompanionScript.r;

import java.util.List;

/**
 * 4.2.6 (the owner: "So develop every single NPC even further, add more dialogue, more options, all of them"): Noro's further topics (pure,
 * tested), after his first three ({@link NoroScript}). Still the owner's Noro: "very brief, barely talks". What he answers is a word or two, or
 * what he does; what he gives is small, and what he takes badly he shows by how far his head tilts.
 */
final class NoroTopics {
    private NoroTopics() {
    }

    static final List<EtoTopics.Topic> MORE = List.of(
        new EtoTopics.Topic("talk", c -> true, c -> "Do you ever talk?", c -> "*...*",
            List.of(
                new EtoTopics.Follow("I like the quiet too.", c -> r("*He nods. Once.*", 2, EtoGesture.TILT)),
                new EtoTopics.Follow("Say something. Anything.", c -> r("*He doesn't.*", -1, EtoGesture.TILT)))),

        new EtoTopics.Topic("eto", c -> true, c -> "Do you serve Eto?", c -> "*A slow nod.*",
            List.of(
                new EtoTopics.Follow("Is she good to you?", c -> r("...Yes.", 1, EtoGesture.NONE)),
                new EtoTopics.Follow("She's using you.", c -> r("*His head tilts. Further than a head should.*", -3, EtoGesture.TILT)))),

        new EtoTopics.Topic("students", c -> c.regard() >= NpcBonds.FRIENDLY, c -> "Have you taught others?", c -> "...Few.",
            List.of(
                new EtoTopics.Follow("What happened to them?", c -> r("*He looks away.* ...Eaten.", 0, EtoGesture.NONE)),
                new EtoTopics.Follow("I'll be your best.", c -> r("...We'll see.", 2, EtoGesture.TILT)),
                new EtoTopics.Follow("Were they weak?", c -> r("*He looks at you instead.*", -1, EtoGesture.TILT)))),

        new EtoTopics.Topic("coat", c -> true, c -> "Isn't that coat hot?", c -> "*Something under it moves.*",
            List.of(
                new EtoTopics.Follow("...Never mind.", c -> r("*He seems to approve.*", 1, EtoGesture.NONE)),
                new EtoTopics.Follow("Can I touch it?", c -> r("*Something under the coat snaps, a finger's width from your hand.*", -2, EtoGesture.TILT)))),

        new EtoTopics.Topic("kakuja", c -> c.ghoul() && c.regard() >= NpcBonds.FRIEND, c -> "Are you a kakuja?", c -> "...Yes.",
            List.of(
                EtoTopics.Follow.once("What did it cost you?", c -> r("...Everything. *He touches the mask, once.*", 3, EtoGesture.NONE)),
                new EtoTopics.Follow("Show me.", c -> r("No.", -1, EtoGesture.NONE)))),

        new EtoTopics.Topic("sleep", EtoTalk.Context::night, c -> "Do you sleep?", c -> "...No.",
            List.of(
                new EtoTopics.Follow("Neither do I.", c -> r("*He sits down beside you. That's all. It's enough.*", 2, EtoGesture.SIT)),
                new EtoTopics.Follow("That explains a lot.", c -> r("*Silence. A colder one.*", -1, EtoGesture.TILT)))),

        new EtoTopics.Topic("name", c -> c.regard() >= NpcBonds.FRIEND, c -> "Is Noro your real name?", c -> "*A long silence.* ...No.",
            List.of(
                EtoTopics.Follow.once("Will you tell me?", c -> r("*He writes something in the dirt with one finger, then wipes it away before you can "
                    + "read it.* ...Close.", 3, EtoGesture.NONE)),
                EtoTopics.Follow.once("Keep it, then.", c -> r("*A nod. Something in it is grateful.*", 2, EtoGesture.TILT))),
            true),

        new EtoTopics.Topic("afraid", c -> c.regard() >= NpcBonds.FRIEND, c -> "Are you afraid of anything?", c -> "...Being full.",
            List.of(
                new EtoTopics.Follow("Why?", c -> r("...Then it stops.", 1, EtoGesture.NONE)),
                new EtoTopics.Follow("That's creepy.", c -> r("*He turns away.*", -2, EtoGesture.NONE)))));
}
