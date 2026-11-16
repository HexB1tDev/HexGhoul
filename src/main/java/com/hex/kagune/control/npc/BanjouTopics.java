package com.hex.kagune.control.npc;

import static com.hex.kagune.control.npc.CompanionScript.one;
import static com.hex.kagune.control.npc.CompanionScript.r;
import static com.hex.kagune.control.npc.CompanionScript.together;

import java.util.List;

/**
 * 4.2.5 (the owner: "Let's add more dialogue to the gas mask leader, banjo ... more opportunities to gain relationship ... some that can increase
 * relationship, um, some that reduce it ... one-time dialogue options"): Banjou's further topics (pure, tested), after his first ones
 * ({@link BanjouScript}). Each has replies that warm him and replies that hurt him; the ones about who he was, the things he only tells once,
 * and the promises are said once ({@link EtoTopics.Follow#once}, {@link EtoTopics.Topic#once}).
 */
final class BanjouTopics {
    private BanjouTopics() {
    }

    static final List<EtoTopics.Topic> MORE = List.of(
        new EtoTopics.Topic("boys", c -> c.regard() >= NpcBonds.FRIENDLY, c -> "Do you know all your boys' names?",
            c -> one(c.seed(), "Every one. Kazuo hums when he's scared. Rin can't read, so I read to him. Tetsu counts the filters twice, then once "
                + "more for luck. ...You learn those things when you're the one who stays up.", "All of them. Their names, their birthdays, which "
                + "ones snore. Somebody should know. Nobody knew mine for a long time."),
            List.of(
                new EtoTopics.Follow("That's what a good leader does.", c -> r("...You think? I just thought it was what you do. Huh.", 3, EtoGesture.SHY)),
                new EtoTopics.Follow("Read to me sometime.", c -> together(c), c -> r("Read to- ...yes. Tonight. I'll do the voices. I'm very bad at the "
                    + "voices.", 3, EtoGesture.READ)),
                new EtoTopics.Follow("What's my birthday, then?", c -> r("I- ...you haven't told me. Tell me. I'll write it on the wall.", 1, EtoGesture.LOOK)),
                new EtoTopics.Follow("Sounds like a babysitter, not a boss.", c -> r("...Yeah. Maybe. Somebody has to babysit. Thanks for pointing it out.",
                    -3, EtoGesture.HUFF)))),

        new EtoTopics.Topic("tea", c -> true, c -> "What tea are you making?",
            c -> one(c.seed(), "Whatever's in the tin. I make tea when I'm nervous. I'm nervous a lot. There's a lot of tea.", "Barley. We can't taste "
                + "it, not really. But the boys like holding something warm. So do I."),
            List.of(
                new EtoTopics.Follow("I'll have a cup.", c -> r("Really? Okay. Sit. It's hot. Careful. ...I'm fussing. Sorry.", 2, EtoGesture.SIP)),
                new EtoTopics.Follow("Why are you nervous?", c -> r(together(c) ? "You're here. It's the good kind of nervous. The kind where I keep "
                    + "checking my hair." : "You're here, and I don't know what you want yet. ...That came out rude. It wasn't meant rude.", 1, EtoGesture.LOOK)),
                new EtoTopics.Follow("Your tea's terrible.", c -> r("It's- ...it is, isn't it. I boil it too long. I get distracted. *pours it away*", -2,
                    EtoGesture.POUT)))),

        new EtoTopics.Topic("before", c -> c.regard() >= NpcBonds.FRIEND, c -> "What were you like, before the Gas Masks?",
            c -> "*He's quiet a while.* ...Alone. Eleventh ward. Nobody fed me, so I learned how long you can go without. Long. Longer than you'd "
                + "think. Then I found Kazuo starving behind a noodle shop, and I couldn't leave him. Then there were two of us. Then twelve.",
            List.of(
                EtoTopics.Follow.once("That must have been hard.", c -> r("It was. ...It's easier to say now. Thank you for asking. Nobody asks.", 4,
                    EtoGesture.SHY)),
                EtoTopics.Follow.once("Is that why you look after them?", c -> r("...Yeah. Nobody looked after me. I'm not going to be the reason anyone "
                    + "else goes without.", 3, EtoGesture.LOOK)),
                new EtoTopics.Follow("Everyone's got a sad story.", c -> r("I know. I wasn't asking for pity. You asked. *turns back to the pots*", -3,
                    EtoGesture.HUFF))),
            true),

        new EtoTopics.Topic("pushover", c -> true, c -> "People say you're a pushover.",
            c -> one(c.seed(), "People say a lot. ...They're not wrong. I'd rather be pushed over than push someone. I can get back up.",
                "The Clowns say it. Loudly. With a song. There's a song. ...It's catchy, I'll give them that."),
            List.of(
                new EtoTopics.Follow("They're wrong.", c -> r("You don't have to say that. ...But thanks. It's nice to hear from somebody.", 3, EtoGesture.SHY)),
                new EtoTopics.Follow("Getting back up is the hard part.", c -> r("...Huh. Yeah. It is. I never thought of it like that.", 3, EtoGesture.LOOK)),
                new EtoTopics.Follow("Sing me the song.", c -> r("Absolutely not. ...*hums two bars* No. That's all you get.", 1, EtoGesture.HUM)),
                new EtoTopics.Follow("They're right.", c -> r("...I know. You didn't have to say it out loud. *looks at his boots*", -4, EtoGesture.FLINCH)))),

        new EtoTopics.Topic("peace", c -> c.regard() >= NpcBonds.FRIENDLY, c -> "Will you ever make peace with the Clowns?",
            c -> one(c.seed(), "I'd like to. I don't start anything. They start it, we finish it, and then everybody's bleeding. I hate it.",
                "Roma laughs every time we fight. Every time. I don't think she even hates us. I think she's just bored. That's worse."),
            List.of(
                new EtoTopics.Follow("Peace is worth trying.", c -> r("It is. ...Would you- if you ever talk to her, tell her we'd meet. Unarmed. Well. "
                    + "Mostly unarmed.", 3, EtoGesture.LOOK)),
                new EtoTopics.Follow("I'll talk to Roma for you.", c -> r("You'd do that? Be careful. She'll make a joke of you. ...She makes a joke of "
                    + "everybody. Thank you.", 2, EtoGesture.BOW)),
                new EtoTopics.Follow("Just wipe them out.", c -> r("No. That's not who we are. We don't start things. ...I thought you knew that about us.",
                    -3, EtoGesture.SCOLD)))),

        new EtoTopics.Topic("watch", EtoTalk.Context::night, c -> "Can't sleep?",
            c -> one(c.seed(), "I keep watch. Somebody has to listen for the door. The boys sleep better when they know I'm up.",
                "Never could. Too many things that might happen. If I'm awake, at least I'm awake for them."),
            List.of(
                new EtoTopics.Follow("I'll keep watch with you.", c -> r("...Yeah? Okay. Sit. You take the left, I take the right. *it's very quiet* ...This "
                    + "is nice.", 3, EtoGesture.SIT)),
                new EtoTopics.Follow("Go to bed. I've got it.", c -> together(c), c -> r("You'd- ...okay. Okay. Wake me if anything moves. Wake me if nothing "
                    + "moves. Goodnight, love.", 3, EtoGesture.YAWN)),
                new EtoTopics.Follow("You look terrible.", c -> r("Thanks. I've been awake since Tuesday. ...What day is it?", -1, EtoGesture.YAWN)))),

        new EtoTopics.Topic("wing", c -> c.regard() >= NpcBonds.FRIEND, c -> "Show me your wing.",
            c -> "*He looks around first, to be sure nobody's watching. Then a small blue wing unfolds from his shoulder, about the size of a dinner "
                + "plate. It trembles.* ...That's it. That's all of it.",
            List.of(
                EtoTopics.Follow.once("It's beautiful.", c -> r("...*the wing folds itself away very fast* Don't- you can't just say that. ...Thank you. "
                    + "Nobody's ever said that.", 4, EtoGesture.BLUSH)),
                new EtoTopics.Follow("Does it hurt?", c -> r("A little, when it comes out. Like a cramp. It's okay. I'm used to it.", 2, EtoGesture.LOOK)),
                EtoTopics.Follow.once("That's tiny!", c -> r("...I know. Everyone says that. I just thought you might not. *it's gone*", -4,
                    EtoGesture.POUT))),
            true),

        new EtoTopics.Topic("stronger", c -> true, c -> "Do you want to get stronger?",
            c -> one(c.seed(), "More than anything. ...But the quick way's eating ghouls, and I won't. Not my boys. Not anybody's boys.",
                "Every day. I do my push-ups. I do all of them. I'm still me after. ...It's okay. I'm useful in other ways."),
            List.of(
                new EtoTopics.Follow("I'll help you train.", c -> r("You'd train with me? I'll warn you, I'm hopeless. ...But I'll show up. Every time.", 2,
                    EtoGesture.HOP)),
                new EtoTopics.Follow("You're strong enough as you are.", c -> r("...You mean that? Okay. I'll try to believe it. One day at a time.", 3,
                    EtoGesture.SHY)),
                new EtoTopics.Follow("Then eat ghouls.", c -> r("*his face goes hard, for once* No. Never. Don't ask me that again.", -6, EtoGesture.ANGRY)))),

        new EtoTopics.Topic("dream", c -> c.regard() >= NpcBonds.FRIEND, c -> "What do you really want?",
            c -> one(c.seed(), "A place where nobody needs a mask. Where the boys can sit in the sun with their faces out. ...Stupid, right?",
                "A garden. A big one. Nobody hiding in it. ...I know. I know how it sounds."),
            List.of(
                new EtoTopics.Follow("It's not stupid.", c -> r("...No? Okay. Then it's not. I'll keep it. Thanks.", 3, EtoGesture.SHY)),
                new EtoTopics.Follow("I'll help you build it.", c -> together(c), c -> r("You- ...we'd start with the garden. Blue orchids along the "
                    + "wall. And a bench for two. *he's already planning it*", 4, EtoGesture.HUG)),
                new EtoTopics.Follow("That'll never happen.", c -> r("...Probably not. You didn't have to be the one to say it.", -3, EtoGesture.FLINCH)))),

        new EtoTopics.Topic("eating", c -> c.regard() >= NpcBonds.FRIENDLY, c -> "How do the Gas Masks eat?",
            c -> c.ghoul() ? one(c.seed(), "The ones nobody will miss, who were already gone. The bridge. The cliffs. We don't hunt. I can't. I tried, once.",
                "We wait. Somebody always jumps, somewhere. It's not pretty. It's better than the other way.")
                : "...You really want to know? We don't hunt. We wait for people who've already decided to go. It's not pretty. I'm sorry.",
            List.of(
                new EtoTopics.Follow("That's decent of you.", c -> r("It's the least bad thing we could think of. ...Thanks for not looking at me like that.",
                    2, EtoGesture.LOOK)),
                new EtoTopics.Follow("I'll bring you something.", c -> r("You don't have to. ...But if you did, the boys eat first. Always.", 1, EtoGesture.BOW)),
                new EtoTopics.Follow("That's cowardly.", c -> r("Maybe. I'd rather be a coward than a killer. You can think what you like.", -4,
                    EtoGesture.HUFF)))),

        new EtoTopics.Topic("charcoal", c -> true, c -> "What do the filters need?",
            c -> one(c.seed(), "Charcoal. Lots of it. Packed tight in the canister, changed every few days or the smell gets through.",
                "Charcoal and cloth. Rin sews, I pack. We're very good at it. It's the one thing we're very good at."),
            List.of(
                new EtoTopics.Follow("I'll bring you charcoal.", c -> r("Would you? It's the thing we always run out of. ...Bring it by. I'll make sure "
                    + "the boys know who it came from.", 2, EtoGesture.BOW)),
                new EtoTopics.Follow("Show me how you pack one.", c -> r("*he takes one apart, very carefully, and walks you through it* ...And that's it. "
                    + "You're a natural. Don't tell Rin.", 2, EtoGesture.TIDY)),
                new EtoTopics.Follow("Seems like a waste of time.", c -> r("It's not, if it's your nose. ...Never mind.", -2, EtoGesture.SHRUG)))),

        new EtoTopics.Topic("promise", c -> together(c), c -> "Promise me something.",
            c -> one(c.seed(), "Anything. ...Within reason. I'm not very good at reason, so maybe anything.", "*straightens up, very serious* Go on. "
                + "I'm listening. With my whole face."),
            List.of(
                EtoTopics.Follow.once("Don't ever get hurt for me.", c -> r("...I can't promise that. I'd do it without thinking. ...Fine. I'll try. "
                    + "I'll try very hard.", 4, EtoGesture.HUG)),
                EtoTopics.Follow.once("Stay exactly as you are.", c -> r("As I am? Weak, nervous, too much tea? ...Okay. Okay. For you.", 4, EtoGesture.SHY)),
                EtoTopics.Follow.once("Teach our kids to garden.", c -> c.married(), c -> r("*his eyes go wet behind the mask* ...Every one of them. "
                    + "Blue orchids first.", 4, EtoGesture.HUG)))),

        new EtoTopics.Topic("first", c -> together(c), c -> "When did you know you liked me?",
            c -> one(c.seed(), "The first time you didn't laugh at me. ...Okay, you laughed a little. But kindly. Nobody laughs kindly.",
                "When you came back. Everybody comes once. You came twice. I made the boys stand up straight. They didn't."),
            List.of(
                new EtoTopics.Follow("I knew the moment I saw you.", c -> r("You did not. ...Did you? *goes very red* Stop it. Don't stop it.", 3,
                    EtoGesture.BLUSH)),
                new EtoTopics.Follow("I'm still deciding.", c -> r("Oh. ...Right. Take your time. I'll be here. Doing my best.", -2, EtoGesture.FLINCH)),
                new EtoTopics.Follow("Say it again.", c -> r("*clears his throat* ...You came back. That's when. That's all it took.", 2, EtoGesture.SHY)))),

        new EtoTopics.Topic("scared", c -> c.regard() >= NpcBonds.FRIEND, c -> "What scares you most?",
            c -> one(c.seed(), "That one day somebody hurts one of the boys and I'm not strong enough to stop it. ...It's not if. It's when.",
                "Being found out. That I'm not tough. That I'm just a big man in a mask, guessing. ...Oh. I just told you."),
            List.of(
                new EtoTopics.Follow("You won't be alone when it happens.", c -> r("...You'd come? *nods slowly* Okay. That helps. More than you know.", 3,
                    EtoGesture.LOOK)),
                new EtoTopics.Follow("Everyone's guessing.", c -> r("Really? Even you? ...Huh. That's the most comforting thing anyone's said all year.", 2,
                    EtoGesture.LAUGH)),
                EtoTopics.Follow.once("Then I'll be strong for both of us.", c -> together(c), c -> r("*he laughs, and it cracks halfway* ...Okay. Okay. "
                    + "Deal. I'll hold you to it. I'll hold you, too.", 4, EtoGesture.HUG)),
                new EtoTopics.Follow("You should be scared.", c -> r("I am. You didn't need to add to it.", -3, EtoGesture.HUFF)))));
}
