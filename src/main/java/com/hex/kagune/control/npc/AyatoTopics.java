package com.hex.kagune.control.npc;

import static com.hex.kagune.control.npc.CompanionScript.one;
import static com.hex.kagune.control.npc.CompanionScript.r;
import static com.hex.kagune.control.npc.CompanionScript.together;

import java.util.List;

/**
 * 4.2.6 (the owner: "So develop every single NPC even further, add more dialogue, more options, all of them"): Ayato's further topics (pure,
 * tested), after his first ones ({@link AyatoScript}). The owner wanted his romance to change him slowly and never into someone else: a
 * stranger gets contempt, a friend insults with something under them, his partner the same insults and the something said out loud, once.
 */
final class AyatoTopics {
    private AyatoTopics() {
    }

    static final List<EtoTopics.Topic> MORE = List.of(
        new EtoTopics.Topic("rabbit", c -> true, c -> "Why do they call you the Black Rabbit?",
            c -> one(c.seed(), "The mask. And because the last guy who laughed at it couldn't run fast enough. Rabbits are quick. So am I.",
                "Doves gave me the name. They meant it as a joke. They stopped joking."),
            List.of(
                new EtoTopics.Follow("It's terrifying.", c -> r("Good. That's the point. ...You're not scared, though. Weird.", 2, EtoGesture.CROSS)),
                new EtoTopics.Follow("Can I call you that?", c -> r(together(c) ? "...Only when nobody's around. And not in that voice." : "No.",
                    together(c) ? 2 : -1, EtoGesture.HUFF)),
                new EtoTopics.Follow("It's a cute name.", c -> r(together(c) ? "It's not cute. *ears red* ...Shut up." : "Say that again and find out how "
                    + "cute it is.", together(c) ? 1 : -2, EtoGesture.HUFF)))),

        new EtoTopics.Topic("train", c -> c.regard() >= NpcBonds.FRIENDLY, c -> "Train with me.",
            c -> one(c.seed(), "You? You'd last ten seconds. Nine if I'm in a mood. I'm always in a mood.",
                "Fine. Stand there. Don't blink. ...You blinked. Again."),
            List.of(
                new EtoTopics.Follow("Then teach me to last eleven.", c -> r("...Huh. Okay. That's the first smart thing you've said. Tomorrow. Early. "
                    + "Don't be late.", 3, EtoGesture.CROSS)),
                new EtoTopics.Follow("I'd beat you.", c -> r("Big words. *grins* Prove it. ...I like you a little better now. A little.", 1, EtoGesture.HUFF)),
                new EtoTopics.Follow("Forget it, you're too rough.", c -> r("Then you're too weak. Figures.", -2, EtoGesture.SHRUG)))),

        new EtoTopics.Topic("anteiku", c -> true, c -> "Have you ever been to Anteiku?",
            c -> one(c.seed(), "That cafe? Where my sister pretends to be human and pours coffee for people she could eat? No.",
                "Once. I stood across the street. She was laughing at something. I left."),
            List.of(
                EtoTopics.Follow.once("You should visit her.", c -> r("...Maybe. Don't push it. *looks away* ...Maybe.", 2, EtoGesture.LOOK)),
                new EtoTopics.Follow("Touka makes great coffee.", c -> r("Of course she does. She's good at everything she's not supposed to be.", -1,
                    EtoGesture.HUFF)),
                new EtoTopics.Follow("Humans aren't so bad.", c -> r("Tell that to my father. ...Oh, right. You can't.", -3, EtoGesture.ANGRY)))),

        new EtoTopics.Topic("weakness", c -> c.regard() >= NpcBonds.FRIEND, c -> "What's your weakness?",
            c -> one(c.seed(), "I don't have one.", "Weakness is for people who stop. I don't stop."),
            List.of(
                new EtoTopics.Follow("Everyone has one.", c -> r("...Fine. My sister. Happy? Tell anyone and you're dead.", 1, EtoGesture.HUFF)),
                EtoTopics.Follow.once("It's me, isn't it?", c -> together(c), c -> r("...Shut up. *a long breath* ...Yeah. It is. Don't make me regret "
                    + "saying it.", 4, EtoGesture.SHY)),
                new EtoTopics.Follow("Your temper.", c -> r("My temper's fine. *it is clearly not fine*", -2, EtoGesture.ANGRY)))),

        new EtoTopics.Topic("scar", c -> c.regard() >= NpcBonds.FRIEND, c -> "Where did that scar come from?",
            c -> "*He touches it without meaning to.* ...I was fourteen. Two doves cornered Touka behind the station. She told me to run. I didn't. "
                + "One of them had a quinque like a saw. ...She doesn't know I remember that she cried.",
            List.of(
                EtoTopics.Follow.once("You were brave.", c -> r("I was stupid. ...Same thing, at fourteen.", 3, EtoGesture.LOOK)),
                EtoTopics.Follow.once("Let me see it.", c -> together(c), c -> r("*he tilts his head, letting you* ...Happy? Don't make a thing of it. "
                    + "...Your hand's warm.", 3, EtoGesture.SHY)),
                new EtoTopics.Follow("You were reckless.", c -> r("Reckless worked. She's alive. Got a better plan, genius?", -1, EtoGesture.CROSS))),
            true),

        new EtoTopics.Topic("doves", c -> true, c -> "What about the CCG?",
            c -> one(c.seed(), "Doves. They killed my father. Every one I put down is one fewer.", "Rats with briefcases. They hunt us with weapons "
                + "made of us. Think about that."),
            List.of(
                new EtoTopics.Follow("I'll fight them with you.", c -> r("...Yeah? Don't slow me down. ...Stay behind me. I mean it.", 2, EtoGesture.CROSS)),
                new EtoTopics.Follow("Revenge won't bring him back.", c -> r("I know that. Don't lecture me. Nobody asked you.", -2, EtoGesture.HUFF)),
                new EtoTopics.Follow("Not all of them are bad.", c -> r("*his eyes go red* Say that again. Go on.", -4, EtoGesture.ANGRY)))),

        new EtoTopics.Topic("patrol", EtoTalk.Context::night, c -> "You're up late.",
            c -> one(c.seed(), "Patrol. Somebody has to keep the idiots alive. Mostly I'm keeping the idiots from doing something stupid.",
                "Can't sleep. Don't ask why. ...I'm not telling you why."),
            List.of(
                new EtoTopics.Follow("Keep me company.", c -> together(c), c -> r("Tch. ...Fine. Five minutes. *it's an hour*", 3, EtoGesture.SIT)),
                new EtoTopics.Follow("Get some sleep.", c -> r("You're not my sister. ...But fine. After this round.", 1, EtoGesture.YAWN)),
                new EtoTopics.Follow("You look exhausted.", c -> r("And you look like you want to get hit. We both have problems.", -1, EtoGesture.HUFF)))),

        new EtoTopics.Topic("eto", c -> c.regard() >= NpcBonds.FRIENDLY, c -> "What do you think of Eto?",
            c -> one(c.seed(), "She's insane. She's also the only reason Aogiri works. Both of those are terrifying.", "The owl. Smiles like she's "
                + "already read how you die. ...She probably has."),
            List.of(
                new EtoTopics.Follow("She scares me too.", c -> r("Good. She should. Stay away from her when she laughs like that.", 1, EtoGesture.LOOK)),
                new EtoTopics.Follow("You admire her.", c -> r("...I respect strength. That's different. Shut up.", 0, EtoGesture.HUFF)),
                new EtoTopics.Follow("You're her errand boy.", c -> r("Say that again and I'll show you what an errand boy does to people.", -4,
                    EtoGesture.ANGRY)))),

        new EtoTopics.Topic("touka_young", c -> c.regard() >= NpcBonds.FRIEND, c -> "What was Touka like as a kid?",
            c -> "Bossy. Loud. Gave me her share when there wasn't enough and said she wasn't hungry. She was always hungry. ...Don't tell her I "
                + "remember.",
            List.of(
                EtoTopics.Follow.once("You love her.", c -> r("...Yeah. Obviously. Idiot. *quietly* Don't tell her.", 3, EtoGesture.LOOK)),
                new EtoTopics.Follow("She's still bossy.", c -> r("Ha! See? You get it. She'll boss the doves to death one day.", 2, EtoGesture.LAUGH)),
                new EtoTopics.Follow("You two are hopeless.", c -> r("Hopeless? Says the one hanging around Aogiri. Get lost.", -1, EtoGesture.HUFF)))),

        new EtoTopics.Topic("promise", c -> together(c), c -> "Promise me something.",
            c -> one(c.seed(), "Ugh. Fine. What.", "*crosses his arms* Make it quick. ...I'm listening. Properly."),
            List.of(
                EtoTopics.Follow.once("Don't go and get yourself killed.", c -> r("...Same to you. Idiot. *quieter* Deal.", 4, EtoGesture.SHY)),
                EtoTopics.Follow.once("Let me in, sometimes.", c -> r("...I'm trying. It's not easy. Don't give up on me.", 4, EtoGesture.LOOK)),
                EtoTopics.Follow.once("Stop calling me weak.", c -> r("...You're not weak. There. I'm only saying it once.", 3, EtoGesture.HUFF)))),

        new EtoTopics.Topic("first", c -> together(c), c -> "When did you start liking me?",
            c -> one(c.seed(), "I don't like you. ...When you didn't run. Everyone runs. You stayed and argued.", "Who says I do? ...The day you "
                + "patched me up and didn't say anything stupid about it."),
            List.of(
                new EtoTopics.Follow("Liar. You like me a lot.", c -> r("*goes red* ...Shut up. Okay. A lot. Happy?", 2, EtoGesture.BLUSH)),
                new EtoTopics.Follow("Say it properly.", c -> r("...I like you. Don't make me say it twice. *says it again anyway, quieter*", 3, EtoGesture.SHY)),
                new EtoTopics.Follow("I'm still not sure I like you.", c -> r("...Fine. Whatever. *he doesn't look at you for a while*", -3,
                    EtoGesture.FLINCH)))),

        new EtoTopics.Topic("bunny", c -> together(c), c -> "Can I give you a pet name?",
            c -> "Try it and die.",
            List.of(
                EtoTopics.Follow.once("Bunny.", c -> r("...*the longest silence of your life* Once. You get once. Never in front of anyone. Ever.", 2,
                    EtoGesture.BLUSH)),
                new EtoTopics.Follow("Fine. Ayato it is.", c -> r("Good. ...Say it again, though.", 1, EtoGesture.CROSS)),
                new EtoTopics.Follow("Shorty.", c -> r("*very slowly* Do you want to die today, or tomorrow?", -3, EtoGesture.ANGRY)))));
}
