package com.hex.kagune.control.npc;

import static com.hex.kagune.control.npc.CompanionScript.one;
import static com.hex.kagune.control.npc.CompanionScript.r;
import static com.hex.kagune.control.npc.CompanionScript.together;

import java.util.List;

/**
 * 4.2.5 (the owner: "Let's add more dialogue to ... the circus grill, more dialogue options ... some that can increase relationship, um, some
 * that reduce it ... one-time dialogue options"): Roma's further topics (pure, tested), after her first ones ({@link RomaScript}). Each has
 * replies that delight her and replies that sting; what she only tells once, and the promises, are said once.
 */
final class RomaTopics {
    private RomaTopics() {
    }

    static final List<EtoTopics.Topic> MORE = List.of(
        new EtoTopics.Topic("joke", c -> true, c -> "Tell me a joke.",
            c -> one(c.seed(), "Why don't ghouls eat clowns? Because they taste funny! ...I've eaten three. They don't. It's a lie. The joke's the lie.",
                "A dove walks into a bar. Then he walks into me. Then he doesn't walk anymore! Ha! ...Too soon? It's always too soon. That's the best time.",
                "What's the difference between a ghoul and a dentist? The dentist bills you after. HA!",
                "Knock knock. Who's there? Not your family! ...Okay, that one's mine, and it's not for everyone."),
            List.of(
                new EtoTopics.Follow("*laugh*", c -> r("HA! See? SEE? Everyone laughs eventually. Some of them just take a while to get there.", 2, EtoGesture.LAUGH)),
                new EtoTopics.Follow("Tell me another.", c -> r("Greedy! I like greedy. ...Okay: what do you call a ghoul with no kagune? Lunch.", 1,
                    EtoGesture.GIGGLE)),
                new EtoTopics.Follow("That's not funny.", c -> r("Everyone's a critic. Everyone's a DEAD critic, eventually. ...Fine. Tough crowd.", -2,
                    EtoGesture.POUT)),
                new EtoTopics.Follow("That's sick.", c -> r("Sick! Yes! Thank you! ...Wait. Were you not complimenting me? *squints*", -1, EtoGesture.TILT)))),

        new EtoTopics.Topic("tent", c -> true, c -> "What's the circus like?",
            c -> one(c.seed(), "The best tent in the world! Patched forty times. Every patch has a story. Most of the stories end with somebody screaming.",
                "At night we put on shows. Fire, knives, tendrils. Nobody pays. Nobody leaves either. Ahaha. ...They can leave. Mostly."),
            List.of(
                new EtoTopics.Follow("Can I see a show?", c -> r("Can you- YES. Front row. Bring a coat. Not for the cold. For the splash zone.", 2,
                    EtoGesture.HOP)),
                new EtoTopics.Follow("Who built it?", c -> r("Me! And everyone. Mostly everyone. I supervised. Supervising is a lot of pointing.", 1,
                    EtoGesture.POSE)),
                new EtoTopics.Follow("It looks like a junkyard.", c -> r("A junkyard with a ringmaster, thank you. ...Wow. Rude. I'm writing that "
                    + "down. For a sad clown routine.", -3, EtoGesture.HUFF)))),

        new EtoTopics.Topic("first_laugh", c -> c.regard() >= NpcBonds.FRIEND, c -> "When did you start laughing at everything?",
            c -> "*For once she doesn't answer right away.* ...The night the doves came for my mother. I hid under the sink. They were so serious. So "
                + "serious, with their cases and their forms. And I started laughing, under there, and I couldn't stop. They never found me. ...I never "
                + "stopped.",
            List.of(
                EtoTopics.Follow.once("I'm so sorry.", c -> r("...Don't. Don't be sorry. It was a long time ago. *quietly* Thank you.", 3, EtoGesture.LOOK)),
                EtoTopics.Follow.once("You don't have to laugh with me.", c -> r("*the grin slips, just a little* ...Okay. Okay. I'll remember that.", 4,
                    EtoGesture.SHY)),
                new EtoTopics.Follow("Everyone's got a sad story.", c -> r("Sure. And mine's the funniest one. ...Wow. You really didn't know what to do "
                    + "with that, huh.", -3, EtoGesture.SHRUG))),
            true),

        new EtoTopics.Topic("kicks", c -> c.regard() >= NpcBonds.FRIENDLY, c -> "Who taught you to kick like that?",
            c -> one(c.seed(), "Nobody! I taught me. Then the General's people tried to arrest me and I taught them too. Lesson one: duck.",
                "Tendrils are slow. Feet are rude. Everybody watches the pretty purple things, and the boot comes from nowhere. Comedy is timing!"),
            List.of(
                new EtoTopics.Follow("Show me a move.", c -> r("*one leg goes up past her own head and stops a hair from your nose* ...Like that. Don't "
                    + "flinch next time. Flinching's funnier, though.", 2, EtoGesture.KICKS)),
                new EtoTopics.Follow("You're incredible.", c -> r("I KNOW. ...Say it again, slower.", 3, EtoGesture.POSE)),
                new EtoTopics.Follow("Kicking's for kids.", c -> r("Says the person who's never been kicked by me. Want to try? No? Then hush.", -2,
                    EtoGesture.HUFF)))),

        new EtoTopics.Topic("paint", c -> true, c -> "Is that face paint?",
            c -> one(c.seed(), "Greasepaint! Waterproof, bloodproof, tearproof. ...Mostly tearproof. Don't test that.",
                "Every morning. It takes twenty minutes. The smile's painted wider than mine, so it's always smiling even when I forget."),
            List.of(
                new EtoTopics.Follow("Paint my face.", c -> together(c), c -> r("Sit! Still! STILL! ...*she's very gentle about it* There. Now we match. "
                    + "Don't you dare wipe it off.", 3, EtoGesture.HAND)),
                new EtoTopics.Follow("Teach me how.", c -> r("First: white, all over. Second: the eyes, big, sad, like a dog. Third: the smile, too wide. "
                    + "Fourth: never let them see the first three.", 1, EtoGesture.TILT)),
                new EtoTopics.Follow("It's smudged.", c -> r("WHERE. ...*spins round to find a mirror* You're lying. ...You're lying, right?", -1,
                    EtoGesture.FLINCH)))),

        new EtoTopics.Topic("prank", c -> c.regard() >= NpcBonds.FRIENDLY, c -> "What's the best prank you've ever pulled?",
            c -> one(c.seed(), "I filled a CCG van with whoopee cushions. They searched it for three hours. Not ONE of them laughed. Worst audience I've "
                + "ever had.", "I sent the Gas Masks a cake. A real one! With a candle! They thought it was a bomb and evacuated the cottage. They "
                + "didn't come back for an hour. I ate the cake."),
            List.of(
                new EtoTopics.Follow("That's genius.", c -> r("THANK you. Finally. Someone with taste. ...You can help with the next one.", 3,
                    EtoGesture.LAUGH)),
                new EtoTopics.Follow("Let's do one together.", c -> r("Ooh. OOH. I have a list. The list is long. The list has diagrams.", 2,
                    EtoGesture.HOP)),
                new EtoTopics.Follow("That's childish.", c -> r("Childish? It's ART. ...Okay, it's childish art. Still art.", -2, EtoGesture.POUT)))),

        new EtoTopics.Topic("silence", c -> c.regard() >= NpcBonds.FRIEND && c.night(), c -> "What scares you?",
            c -> one(c.seed(), "...Silence. A tent with nobody laughing in it. I check on them at night. All of them. Just to hear somebody snore.",
                "Being the last one laughing. Everyone else gone, and me still doing the act to nobody."),
            List.of(
                EtoTopics.Follow.once("I'll always laugh at your jokes.", c -> r("...Even the bad ones? *quietly* Okay. I'm holding you to that. Forever.",
                    4, EtoGesture.SHY)),
                new EtoTopics.Follow("Then let's be loud together.", c -> r("*she whoops so loud something falls over in the tent* ...Better. Much better.",
                    2, EtoGesture.LAUGH)),
                new EtoTopics.Follow("Silence is nice.", c -> r("...For you, maybe. *the grin comes back a little too fast* Never mind!", -2,
                    EtoGesture.SHRUG)))),

        new EtoTopics.Topic("doves", c -> true, c -> "What about the CCG?",
            c -> one(c.seed(), "Doves! Terrible audience. Always shooting, never clapping. I've done my best material for them. Nothing.",
                "They hate us. Fair! We're very hateable. But they hate the Gas Masks too, and the Gas Masks water FLOWERS."),
            List.of(
                new EtoTopics.Follow("We'll keep you safe.", c -> r("*blinks* ...We? Oh, I like 'we'. Keep saying 'we'.", 2, EtoGesture.TILT)),
                new EtoTopics.Follow("Have you ever met the General?", c -> r("Once! He didn't laugh either. I think his face is broken. I offered to fix it. "
                    + "With my foot.", 1, EtoGesture.KICKS)),
                new EtoTopics.Follow("They're just doing their job.", c -> r("So am I! Mine's funnier and nobody dies. ...Fewer people die. Hm.", -2,
                    EtoGesture.HUFF)))),

        new EtoTopics.Topic("juggling", c -> together(c), c -> "Tell me a secret.",
            c -> "*She leans in close, very serious, then whispers:* ...I can't juggle. Thirty Clowns, and the ringmaster can't juggle. I fake it with "
                + "the tendrils. Nobody's ever noticed. Nobody's ever looked.",
            List.of(
                EtoTopics.Follow.once("Your secret's safe with me.", c -> r("...I know. That's why I told you. *kisses your nose* There. Sealed.", 4,
                    EtoTalk.Effect.NONE, EtoGesture.PECK)),
                EtoTopics.Follow.once("I'll teach you.", c -> r("You can juggle?! ...Marry me. Wait. Teach me first. THEN marry me.", 3, EtoGesture.HOP)),
                EtoTopics.Follow.once("I'm telling everyone.", c -> r("You WOULDN'T. ...You would. *narrows her eyes* I know where you sleep. Next to me. "
                    + "Think about that.", -2, EtoGesture.SCOLD))),
            true),

        new EtoTopics.Topic("banjou", c -> c.regard() >= NpcBonds.FRIENDLY, c -> "What do you think of Banjou?",
            c -> one(c.seed(), "The big sad lamp? He waters flowers. FLOWERS. In a war. ...He's not bad. Don't tell him I said that.",
                "He breathes very loudly through that mask. Like an old dog. A big, loyal, sad old dog. ...I'd never hurt him. Badly."),
            List.of(
                new EtoTopics.Follow("He's a pushover.", c -> r("Ha! Exactly! A big soft pushover with a gas mask. ...Somebody has to protect people like "
                    + "that. Don't repeat that either.", 1, EtoGesture.LAUGH)),
                new EtoTopics.Follow("You like him.", c -> r("I do NOT. ...I like annoying him. That's different. That's very different.", 0, EtoGesture.HUFF)),
                new EtoTopics.Follow("He's sweet.", c -> together(c), c -> r("...Sweet. Is he. *the smile stays exactly where it is* How nice for him.", -2,
                    EtoGesture.JEALOUS)))),

        new EtoTopics.Topic("promise", c -> together(c), c -> "Promise me something.",
            c -> one(c.seed(), "*hand on heart, very grand* Anything! Within reason! Reason not included!", "...Okay. *drops the voice* I'm listening. "
                + "For real."),
            List.of(
                EtoTopics.Follow.once("Never stop laughing.", c -> r("...That's an easy one. You make it easy. *she isn't joking*", 4, EtoGesture.SHY)),
                EtoTopics.Follow.once("Take the mask off for me someday.", c -> r("*very quiet* ...Someday. Not today. But you'll be the first. You'll be "
                    + "the only.", 4, EtoGesture.LOOK)),
                EtoTopics.Follow.once("Don't die doing something stupid.", c -> r("No promises! ...*softer* I'll try. I'll try very, very hard.", 3,
                    EtoGesture.HUG)))),

        new EtoTopics.Topic("first", c -> together(c), c -> "When did you fall for me?",
            c -> one(c.seed(), "When you didn't laugh at the joke. You just looked at me, like you were waiting for the real me to show up. Nobody waits.",
                "The second time you came to the tent. Everyone comes once for the show. You came back for the clown."),
            List.of(
                new EtoTopics.Follow("I've been waiting since.", c -> r("...Stop. STOP. I'm a professional. *she is not being professional*", 3, EtoGesture.BLUSH)),
                new EtoTopics.Follow("I only came for the show.", c -> r("...Oh. Ha. Ha! Good one! *it isn't*", -3, EtoGesture.FLINCH)),
                new EtoTopics.Follow("Again. Say it again.", c -> r("You came back for the clown. *a real smile, under the painted one* ...Happy?", 2,
                    EtoGesture.GIGGLE)))),

        new EtoTopics.Topic("purple", c -> true, c -> "Why purple?",
            c -> one(c.seed(), "It's the colour of a bruise! Healing! Something that hurt and is getting better! ...Also it looks good on me.",
                "Everyone else is red. Red, red, red. Boring. If I'm going to be a monster I'm going to be a fashionable one."),
            List.of(
                new EtoTopics.Follow("It suits you.", c -> r("Doesn't it? It does. *twirls* Say more things like that.", 2, EtoGesture.TWIRL)),
                new EtoTopics.Follow("Red's scarier.", c -> r("Scary's easy. Memorable's hard. ...Okay, red's a LITTLE scarier.", -1, EtoGesture.SHRUG)),
                new EtoTopics.Follow("A bruise getting better. That's lovely.", c -> r("...Huh. You actually listened. People don't usually listen past "
                    + "the joke.", 3, EtoGesture.LOOK)))));
}
