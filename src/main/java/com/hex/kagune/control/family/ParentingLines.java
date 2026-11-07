package com.hex.kagune.control.family;

import java.util.HashMap;
import java.util.Map;

/**
 * 1.3.49: what a companion parent and its child say to each other ({@link Parenting#talk}): the parent's lines by who it is (Touka, Ken,
 * Eto), why ({@link Parenting.Why}) and the child's age (a toddler's, a child's, a teen's); the child's by its temper ({@link ChildTemper})
 * and the same. Parts: "open" (the parent first), "reply" (the child), "follow" (why), "close" (the child again), "final" (the parent's
 * last word). {@code {name}} is the child's name, {@code {call}} what it calls that parent.
 */
final class ParentingLines {
    private static final Map<String, String[]> LINES = new HashMap<>();

    private ParentingLines() {
    }

    private static void put(String key, String... lines) {
        LINES.put(key, lines);
    }

    /** The lines for {@code part} by {@code who} (a parent's key or a temper's word), {@code why}, {@code group}; null if none. */
    static String[] get(String part, String who, String why, String group) {
        String[] lines = LINES.get(part + ":" + who + ":" + why + ":" + group);
        if (lines == null && "little".equals(group)) {
            lines = LINES.get(part + ":" + who + ":" + why + ":child");
        }
        return lines;
    }

    /** How many lines there are in all. */
    static int count() {
        int n = 0;
        for (String[] lines : LINES.values()) {
            n += lines.length;
        }
        return n;
    }

    /** Every key ("part:who:why:group"). */
    static java.util.Set<String> keys() {
        return java.util.Collections.unmodifiableSet(LINES.keySet());
    }

    static {
        put("open:touka:wandered:little", "*grabs {name} under the arms* Got you. Where do you think you're going, huh?", "{name}! Stop right there. ...You're fast for someone with legs that short.", "*catches up, breathing hard* You little escape artist. Home. Now.", "Hey! {name}! That's way too far. You don't walk off on your own.");
        put("open:touka:wandered:child", "{name}. Do you have any idea how far from the house you are?", "Found you. You know you're not supposed to go this far. Don't play dumb.", "*grabs {name}'s collar* And where was this little adventure going to end?", "Hey. HEY. Stop. You think I wouldn't notice you were gone?");
        put("open:touka:wandered:teen", "So this is where you've been. Didn't think to tell anyone?", "{name}. You're miles from home. Explain. Short version.", "You want to go out, you say so first. You don't just vanish.", "*crosses her arms* Nice walk? Good. It's over. We're going back.");
        put("open:touka:night:little", "*scoops {name} up* It's the middle of the night! Babies are supposed to be asleep!", "{name}?! What are you doing outside in the dark? ...Come here. You're freezing.", "*picks {name} up, holding tight* Never. Ever. Go out at night. Got it?");
        put("open:touka:night:child", "{name}. It's pitch black. What are you doing outside?", "You snuck out. At night. Do you know what walks around out here after dark?", "*stops in front of {name}* Bed. Was. An hour ago. Why are you out here?", "I went to check on you and your bed was empty. Do you know what that did to me?");
        put("open:touka:night:teen", "Out at night without a word. Real mature, {name}.", "It's dark. You're out. I'm out here looking for you. See the problem?", "Night's when the CCG walks. And other things. You know that. So why are you out here?");
        put("open:touka:rampage:child", "{name}! Enough! Pull it back. NOW.", "*grabs {name}'s arm* Stop. It's done. You hear me? It's done.", "Hey! Look at me, not at it. Breathe. Put it away.", "*steps between {name} and the fight* That's enough. I mean it.");
        put("open:touka:rampage:teen", "{name}! Stop chasing it! You're past the point of protecting anyone.", "*catches {name}'s wrist* You've been at it way too long. Stand down.", "Look at yourself. You're bleeding and you're still going. Stop. Now.", "That's not a fight anymore, {name}. That's a hunt. Put it away.");
        put("open:touka:transform:child", "Your kagune... {name}, careful. Keep your eyes on it, not on the thing you're hitting.", "Already? ...Fine. Stay where I can see you.", "Hey! Watch your footing. It pulls you around at first.", "There it is. ...Don't show off. Just protect yourself.");
        put("open:touka:transform:teen", "Nice control. Don't get cocky.", "Kagune out, huh? Keep it tight. Short strikes.", "Good. Remember what I taught you. Hit, move, don't stand still.", "...You look like me, doing that. Weird. Be careful.");
        put("open:touka:home:little", "There. Home. *sets {name} down gently* Stay inside where it's warm.", "Home sweet home. And you're not leaving it again today, little one.", "*kisses the top of {name}'s head* There. You're safe. Don't scare me like that.");
        put("open:touka:home:child", "Inside. And you're helping me with the dishes tonight. That's the deal.", "We're home. Go wash your hands. ...And your face. You look like a wild animal.", "There. Home. Was it worth it? Didn't think so.");
        put("open:touka:home:teen", "Home. Next time, a word before you go. That's all I'm asking.", "We're back. I'm not mad. ...I'm a little mad. Go eat something.", "Inside. We'll talk about it tomorrow. Get some sleep.");
        put("follow:touka:wandered:little", "It's big out there and you're tiny. Things out there eat tiny.", "You don't go past the garden. Mama needs to see you. Always.", "If you want to explore, you come get me first. We go together.");
        put("follow:touka:wandered:child", "I'm not saying never go out. I'm saying not alone, not this far. Not yet.", "There are people out here who'd kill a ghoul kid without blinking. Don't make it easy for them.", "When I can't see you, I can't protect you. It's that simple.");
        put("follow:touka:wandered:teen", "I know you're strong. Strong's not the same as smart. Tell someone where you're going.", "You think I'm being overprotective. Maybe. I've buried people who weren't careful.", "One word. 'I'm going out.' That's all it takes. Is that so hard?");
        put("follow:touka:night:little", "At night the dark has teeth. That's why you stay in bed.", "If you can't sleep you come wake Mama. You don't go outside.", "The monsters come out at night. ...The other monsters. Not us.");
        put("follow:touka:night:child", "At night you can't see what's coming. Neither can I. That's why we stay in.", "Investigators patrol after dark. If they see a kakugan, they don't ask questions.", "You can't sleep? Then you come knock on my door. You don't climb out the window.");
        put("follow:touka:night:teen", "I get it. Night feels like freedom. It's also when people like us get hunted.", "If you're meeting someone, I want to know who. If you're hunting, I want to be there.", "You're not a kid anymore, but you're still mine. That doesn't stop at sunset.");
        put("follow:touka:rampage:child", "You protect first. Then you stop. You don't chase things into the dark.", "When it runs, you let it run. Chasing is how you get ambushed.", "Your kagune wants to keep going. You're the one who decides. Not it.");
        put("follow:touka:rampage:teen", "Losing yourself in it feels good. That's exactly why it's dangerous.", "You were bleeding and you didn't even notice. That's the hunger talking, not you.", "The point is to come home. Every time. Not to win every fight.");
        put("final:touka:wandered:little", "*carries {name} on her hip* Come on. I'll make you something warm.", "*holds {name} a little tighter* ...You scared me, you know.");
        put("final:touka:wandered:child", "Come on. Home. ...And you're not in trouble. This time.", "*ruffles {name}'s hair, too hard* Idiot. Let's go.", "Next time you want to go somewhere, ask me. I might even say yes.");
        put("final:touka:wandered:teen", "Let's go home. I'll put the coffee on.", "*bumps {name}'s shoulder with hers* Come on. Walk with me.", "...I'm glad you're okay. Don't tell anyone I said that.");
        put("final:touka:night:little", "*hums softly, walking home* Shh. Sleep. Mama's got you.", "There. Close your eyes. We'll be home before you know it.");
        put("final:touka:night:child", "Come on. Bed. And I'm sitting with you till you're asleep.", "Let's go. ...I'll leave the light on tonight. Just this once.", "Home. Bed. Tomorrow you can tell me what was so important.");
        put("final:touka:night:teen", "Let's go home before I start crying, okay? I'm not crying. Shut up.", "Come on. I'm not going back to sleep anyway. We'll talk.", "Home. And tomorrow night, if you want to go out, we go together.");
        put("final:touka:rampage:child", "*wipes the blood off {name}'s cheek* There. You're okay. Let's go home.", "You did good, protecting yourself. The chasing part, we'll work on.", "Come here. *a quick, fierce hug* Don't do that to me again.");
        put("final:touka:rampage:teen", "Walk it off. We're going home. I'll patch you up.", "You're strong, {name}. Now learn to be strong enough to stop.", "Come on. I'm proud of you. And furious. Mostly proud.");
        put("final:touka:transform:child", "Just be careful. That's all.", "...You'll be fine. You're mine, after all.");
        put("final:touka:transform:teen", "Go. I've got your back.", "Don't make me come in there.");
        put("open:ken:wandered:little", "*kneels down* There you are. You walked a long way, didn't you?", "{name}... you scared me. Come here. *picks {name} up carefully*", "Found you. You're all muddy. Were you following a butterfly again?");
        put("open:ken:wandered:child", "{name}. I looked everywhere for you. Are you hurt?", "There you are. You're a long way from home. Want to tell me why?", "I'm not angry. I just... I need to know where you are. Always.", "{name}, stop for a second. Look how far the house is.");
        put("open:ken:wandered:teen", "I figured you'd be out here. You should have told me.", "{name}. Can we talk? You've been gone a long time.", "I'm not going to yell. But I need you to understand why I came looking.");
        put("open:ken:night:little", "*gathers {name} up in his coat* It's so cold... what are you doing out here at night?", "{name}, it's dark. Little ones should be in bed. Let's get you warm.", "*holds {name} close* Shh, it's okay. Papa's here. But we don't go out at night.");
        put("open:ken:night:child", "{name}, it's the middle of the night. I woke up and your bed was empty.", "You can't be out here after dark. Not yet. Please.", "Hey. It's late. What's out here that couldn't wait till morning?");
        put("open:ken:night:teen", "It's late, {name}. Out at night, alone... you know how that ends for people like us.", "I'm not trying to control you. I'm trying to keep you alive.", "I couldn't sleep either. Then I saw your window open.");
        put("open:ken:rampage:child", "{name}! Stop. Please. Look at me.", "*catches {name} gently by the shoulders* It's over. Let it go. Breathe with me.", "That's enough. You did enough. Pull it back in, slowly.", "{name}, listen to my voice. Not the hunger. Me.");
        put("open:ken:rampage:teen", "{name}! You don't have to finish it. You don't have to finish everything.", "*steps in front of {name}* Stop. I know that look. I've worn it.", "That's not you anymore, that's the kagune. Come back to me.", "You're hurt and you're still going. Please stop.");
        put("open:ken:transform:child", "{name}, your kagune... slowly. Let it come out slowly.", "It's alright. It's a part of you. Just stay calm.", "Careful. Remember: you're in charge of it. Not the other way round.", "...It's beautiful. And dangerous. Stay close to me.");
        put("open:ken:transform:teen", "Good form. Keep breathing.", "That's it. Control first. Strength second.", "I'm right behind you. Don't overextend.", "You've gotten stronger. ...Don't let it change you.");
        put("open:ken:home:little", "*sets {name} down by the fire* There. Warm and safe.", "Home. Let's get you cleaned up and into bed.", "*tucks a blanket around {name}* You're home. That's what matters.");
        put("open:ken:home:child", "We're home. I'll make cocoa. Then we talk.", "Here we are. Go change out of those clothes. I'll warm something up.", "Home. ...Thank you for walking back with me.");
        put("open:ken:home:teen", "Home. I'm making coffee. You want some?", "We're back. Get some rest. We'll talk when you're ready.", "Thanks for coming home. I mean that.");
        put("follow:ken:wandered:little", "The world is big and it isn't always kind. That's why you stay where I can see you.", "If you want to see the river, I'll take you. Just don't go alone.", "You're very brave. But brave and alone isn't a good mix.");
        put("follow:ken:wandered:child", "There are people who'd hurt you just for being what you are. I can't let that happen.", "I'm not saying stay inside forever. I'm saying tell me first. Let me come.", "When you disappear, my head goes to the worst places. I can't help it.");
        put("follow:ken:wandered:teen", "You don't need my permission for everything. Just... let me know you're alive.", "I lost a lot of people because nobody knew where they were. I won't lose you that way.", "Next time leave a note. Even a stupid one. I'll keep them all.");
        put("follow:ken:night:little", "At night it's dark and you can get lost. Papa wouldn't find you.", "When you can't sleep, come and wake me. I'll read to you.", "The night is for sleeping. The day is for exploring. Okay?");
        put("follow:ken:night:child", "Investigators hunt at night. If they saw your eyes... I can't even think about it.", "If something's keeping you awake, you can tell me. Anything.", "Nights are when I'm most afraid. For you. Please stay in.");
        put("follow:ken:night:teen", "I used to walk at night too. Looking for something I couldn't name. It found me first.", "If you need air, wake me. We'll walk together. I won't ask questions.", "You're old enough to choose. Choose to come home. Every time.");
        put("follow:ken:rampage:child", "You protected yourself. That's good. But when it runs, you let it go.", "The hunger always wants one more. You don't have to give it one more.", "Fighting isn't about winning. It's about coming home.");
        put("follow:ken:rampage:teen", "I know what it feels like when it takes over. I know how good it feels. That's why I'm scared.", "You kept going even hurt. Someday you won't be able to stop. Learn now.", "Strength that can't stop isn't strength. It's a cage.");
        put("final:ken:wandered:little", "*carries {name} home, humming* Let's go home. I'll make you a snack.", "*holds {name} close* You're safe now. That's all that matters.");
        put("final:ken:wandered:child", "Come on. Let's go home together. ...Want to hold my hand?", "It's okay. You're not in trouble. Let's just go home.", "Home. And next time, I'll come with you. Deal?");
        put("final:ken:wandered:teen", "Walk with me? I've missed talking to you.", "Let's head back. I'm proud of how grown you are. Really.", "Come home. We'll figure the rest out.");
        put("final:ken:night:little", "*rocks {name} gently* Sleep. Papa's got you. We'll be home soon.", "There. Close your eyes. I'll tell you a story on the way.");
        put("final:ken:night:child", "Let's go home. I'll stay with you until you fall asleep.", "Come on. I'll make tea. Then bed.", "It's alright. I'm not angry. I just love you. A lot.");
        put("final:ken:night:teen", "Let's go home. The coffee's still warm.", "Come on. We both need sleep. ...I'm glad it was just a walk.", "Home. If you want to talk, I'll be up.");
        put("final:ken:rampage:child", "*kneels and checks {name}'s wounds* You'll heal. Let's go home.", "You stopped. That's the hardest part. I'm proud of you.", "*hugs {name} tightly* It's over. You're okay.");
        put("final:ken:rampage:teen", "Come home. I'll help you clean up.", "You came back to yourself. Remember that feeling.", "You're stronger than the hunger. You just proved it.");
        put("final:ken:transform:child", "I'm right here if you need me.", "Stay calm. You're doing well.");
        put("final:ken:transform:teen", "Go. But come back to me.", "Don't let it decide for you.");
        put("open:eto:wandered:little", "*swoops {name} up* Caught you, my little runaway! Plot twist: Mama is faster.", "And where is my tiny monster wandering off to? Hm? Hm?", "*dangles {name} upside down* Escaped prisoner recaptured! Back to the dungeon!");
        put("open:eto:wandered:child", "Well, well, well. Look who's playing explorer without telling the author.", "{name}, darling. You're very far from home. Is this a dramatic exit? Because I love those. But no.", "Running away? How literary. Disappointing ending, though: I found you.", "*appears right behind {name}* Boo. You're grounded. Hello.");
        put("open:eto:wandered:teen", "Ah, the rebellious teenage chapter. I've read this one. It ends badly. Come home.", "{name}. You know I can find anyone, anywhere. Why do you even try?", "Wandering off without a note? Even villains leave notes, sweetheart.");
        put("open:eto:night:little", "*wraps {name} in her coat* Out in the dark? Oh, my little nightmare. Not yet.", "Who let the baby out at midnight? ...Was it you? It was you.", "*picks {name} up, nose to nose* The night is Mama's time. Not yours. Not yet.");
        put("open:eto:night:child", "Out at night? Oh, {name}. The night has teeth. Mine are just the friendliest.", "Sneaking out at midnight. I'd be proud if I weren't so annoyed.", "Your bed was empty and I aged ten years. I'm already old enough, thank you.");
        put("open:eto:night:teen", "A midnight stroll. How very gothic of you. Also: absolutely not.", "The night's dangerous for people like us. I should know. I'm what makes it dangerous.", "Out after dark without telling me? I'm offended. Professionally.");
        put("open:eto:rampage:child", "{name}! That's enough, sweetheart. Put the scary thing away.", "*catches {name} mid-swing* Ah ah ah. Story's over. The monster lost. You won. Stop.", "Look at Mama. Look. At. Me. Breathe. Good. Now put it away.", "*holds {name}'s face in both hands* Hey. You're in there. Come out.");
        put("open:eto:rampage:teen", "{name}! You're not chasing it, the hunger is. Stop.", "*grabs {name} by the collar* That's quite enough carnage for one chapter.", "Bleeding and still swinging? That's MY thing. Don't steal my thing. Stop.", "Look at you, all teeth and no brakes. Pull it back.");
        put("open:eto:transform:child", "Ooh, look at that! My little monster, all grown teeth!", "Kagune out! Mama's so proud. Don't poke your eye out.", "There it is! Isn't it pretty? Be careful, it bites. Everything bites.", "Careful, darling. It's heavier than it looks.");
        put("open:eto:transform:teen", "Now THAT is a kagune. You get that from me. Obviously.", "Gorgeous. Terrifying. Keep it tight.", "Show them, {name}. But don't show off.", "Ah, the transformation scene. My favourite part.");
        put("open:eto:home:little", "*plops {name} on the couch* Home! The dungeon of cuddles. No escape.", "There. Safe and sound and in so much trouble. Kidding. Mostly.", "*tucks {name} into a blanket burrito* Home. You're a burrito now. Burritos can't leave.");
        put("open:eto:home:child", "Home! Go wash. You smell like adventure and dirt.", "And we're back. I'll make tea. You'll tell me everything. Every detail.", "Home sweet home. The prodigal child returns. I'll write about this.");
        put("open:eto:home:teen", "Home. I'll put the kettle on. Then you're telling me the whole story.", "We're back. Go rest. I'll be in my study, worrying dramatically.", "Welcome home, my favourite plot hole.");
        put("follow:eto:wandered:little", "Out there are people who'd love to put you in a jar. Mama doesn't share.", "If you want to explore, you take Mama. Mama knows all the best secret places.", "You're tiny and delicious-looking. To bad people, I mean. Stay close.");
        put("follow:eto:wandered:child", "The CCG has a whole department for kids like you. Don't give them a reason.", "I love that you're curious. I hate that you're curious alone.", "I've seen a lot of stories end with 'and then they wandered off'. Let's not be one.");
        put("follow:eto:wandered:teen", "Freedom's lovely. So is a note on the fridge. Both, please.", "I spent my childhood alone in the dark. You don't have to. That's the whole point of me.", "You can go anywhere you want. Just tell me where. I worry. Silently. Theatrically.");
        put("follow:eto:night:little", "At night the dark eats little ones. Except you. Because Mama eats the dark first.", "Can't sleep? Come and get Mama. I'll tell you a scary story. A nice scary one.", "Babies sleep at night. Mamas prowl. That's the rule.");
        put("follow:eto:night:child", "At night the investigators come out with their shiny briefcases. Let's not meet them.", "If you can't sleep, wake me. We'll read something horrible together.", "The night belongs to monsters. You're a little one. The big ones would eat you.");
        put("follow:eto:night:teen", "I know the night calls. It called me too. I answered. Look how I turned out. ...Actually, don't.", "If you want to prowl, prowl with me. I know where the good rooftops are.", "You're becoming a creature of the night. Wonderful. Do it with a chaperone.");
        put("follow:eto:rampage:child", "When the fight's won, you stop. Chasing is for people who want to lose.", "The hunger wants more. It always wants more. You don't have to listen to it.", "You're allowed to be scary. You're not allowed to be lost.");
        put("follow:eto:rampage:teen", "Losing yourself is easy. I've done it. Finding yourself again is the hard part.", "You didn't even notice you were bleeding. That's how it starts. Trust me.", "A good monster knows when the scene is over.");
        put("final:eto:wandered:little", "*carries {name} home, singing something slightly unsettling* La la la, home we go~", "*nuzzles {name}* Mine. All mine. Let's go home.");
        put("final:eto:wandered:child", "Come along, little explorer. I'll show you the river tomorrow. Together.", "Home! And if you're good, I'll let you read one of my manuscripts. The tame one.", "*takes {name}'s hand* Let's go home, my tiny catastrophe.");
        put("final:eto:wandered:teen", "Walk me home, would you? It's dangerous out here. For them.", "Come on. You can be mysterious at home. I'll pretend not to notice.", "Home. And I'm writing you into my next book as the annoying one.");
        put("final:eto:night:little", "*wraps {name} up tight* Shh. Sleep, my little nightmare. Mama has you.", "There we go. Close your eyes. We'll be home before the monsters notice.");
        put("final:eto:night:child", "Bed. Now. And I'm reading you the scariest story I have. As punishment.", "Come on. Home. I'll sit with you until you fall asleep. And then a bit longer.", "Let's go. ...I'm glad you're alright. Truly.");
        put("final:eto:night:teen", "Home. Then tea. Then you tell me who you were meeting. Kidding. Unless...", "Let's go home, darling. Even monsters need sleep.", "Come on. Next full moon, we go out together. Promise.");
        put("final:eto:rampage:child", "*licks her thumb and wipes {name}'s cheek* There. Like nothing happened. Let's go.", "You were magnificent. And reckless. Mostly reckless. Home.", "*squeezes {name} too tight* Don't scare me like that. I'm fragile. Emotionally.");
        put("final:eto:rampage:teen", "Home. I'll stitch you up. You'll complain. It'll be lovely.", "That was a very impressive tantrum. Now let's go home.", "You came back. That's the part that counts.");
        put("final:eto:transform:child", "Go get them, tiny terror!", "Don't let it eat you. Eat it first.");
        put("final:eto:transform:teen", "Make it a good scene.", "I'll be watching. Taking notes.");
        put("reply:sunny:wandered:little", "{call}! {call}! I saw a froggy!", "Hi {call}! *giggles* Go again?");
        put("reply:sunny:wandered:child", "But {call}, there was a really cool cave and I just had to see it!", "Hi! I was gonna come back! Eventually! Probably!");
        put("reply:sunny:wandered:teen", "Relax, I was just exploring. It's really pretty out here, you know?", "I lost track of time! The view from the hill is amazing, you should see it.");
        put("reply:sunny:night:little", "{call}! Look, stars! So many!", "*yawns, then beams* I wanted to see the moon!");
        put("reply:sunny:night:child", "But the fireflies only come out at night! I wanted to catch one for you!", "I couldn't sleep and the moon looked so nice...");
        put("reply:sunny:night:teen", "I just wanted to see the sky. It's really clear tonight.", "I was stargazing! I swear that's all!");
        put("reply:sunny:rampage:child", "But I was winning! Did you see? Did you see me?", "*panting, grinning* I got it SO good, {call}!");
        put("reply:sunny:rampage:teen", "I'm fine! I'm totally fine! ...Okay, maybe a little hurt.", "It kept getting back up! I wanted to make sure it stayed down!");
        put("reply:sunny:transform:child", "Look, {call}! Look at it! It's SO cool!", "Wheee! It's out! Watch me!");
        put("reply:sunny:transform:teen", "Check it out! I've been practicing!", "Watch this, {call}!");
        put("reply:sunny:home:little", "*claps* Home! Yay!", "Home! Cookie?");
        put("reply:sunny:home:child", "Okay! Can we have cocoa? Pleeease?", "Home! I'm starving! What's for dinner?");
        put("reply:sunny:home:teen", "Fine, fine. Thanks for coming to get me, {call}.", "Okay. ...It was kind of nice, being walked home.");
        put("close:sunny:wandered:little", "Okay {call}! *hugs your leg*", "Sowwy! *big smile*");
        put("close:sunny:wandered:child", "Okay, okay! Next time I'll bring you! It'll be an adventure!", "Sorry, {call}! I'll ask first next time. Promise!");
        put("close:sunny:wandered:teen", "Okay. I'll text you next time. Well. Leave a note.", "Yeah, you're right. Sorry for worrying you.");
        put("close:sunny:night:little", "*sleepy* Night night, {call}...", "Okay... *yawns* carry me?");
        put("close:sunny:night:child", "Okay... can we catch fireflies together sometime?", "Sorry, {call}. I'll stay in bed. I'll try!");
        put("close:sunny:night:teen", "Okay. I'll ask next time. Maybe we can go together?", "Yeah. Sorry. I didn't think you'd be scared.");
        put("close:sunny:rampage:child", "Okay... I'm sorry, {call}. It just felt so good to win.", "*sniffles* I'll stop when you say. Promise.");
        put("close:sunny:rampage:teen", "Yeah... okay. I'll learn to stop. Teach me?", "You're right. I didn't even notice I was hurt. That's scary.");
        put("reply:shy:wandered:little", "*hides face* ...", "*whimpers* {call}... lost...");
        put("reply:shy:wandered:child", "...I'm sorry. I followed a cat and then I didn't know where I was.", "*quietly* I got lost. I was scared to come back alone.");
        put("reply:shy:wandered:teen", "...I just needed some quiet. Sorry.", "I didn't want to bother anyone. I just went for a walk.");
        put("reply:shy:night:little", "*clings* ...dark...", "*small voice* {call}... bad dream...");
        put("reply:shy:night:child", "I had a nightmare and... I didn't want to wake you.", "...I heard something and went to look. I'm sorry.");
        put("reply:shy:night:teen", "I couldn't sleep. I didn't want anyone to see me like that.", "...It's easier to think at night. When no one's looking.");
        put("reply:shy:rampage:child", "*shaking* It wouldn't stop... I couldn't stop...", "I'm sorry... I didn't mean to... it just kept going...");
        put("reply:shy:rampage:teen", "...I didn't know how to stop. It was like watching someone else.", "*stares at their hands* Was that... me?");
        put("reply:shy:transform:child", "...is it okay? It feels weird.", "*whispers* {call}... it came out by itself...");
        put("reply:shy:transform:teen", "...I'll be careful.", "I'm okay. I think.");
        put("reply:shy:home:little", "*clings to your neck* ...", "*nods into your shoulder*");
        put("reply:shy:home:child", "...Can I sleep in your room tonight?", "*quietly* Thank you for finding me.");
        put("reply:shy:home:teen", "...Thanks. For coming.", "*small nod* I'm glad you came.");
        put("close:shy:wandered:little", "*nods, sniffling*", "...'kay.");
        put("close:shy:wandered:child", "...Okay. I'll stay close. I promise.", "*holds your hand very tight* Okay.");
        put("close:shy:wandered:teen", "...Okay. I'll tell you next time.", "Sorry for making you worry.");
        put("close:shy:night:little", "*already asleep on your shoulder*", "*mumbles* ...stay...");
        put("close:shy:night:child", "...Will you stay till I fall asleep?", "Okay. Can I leave the light on?");
        put("close:shy:night:teen", "...Okay. Thanks for not yelling.", "I'll wake you next time. If that's okay.");
        put("close:shy:rampage:child", "*cries quietly* I'm sorry, {call}...", "...Okay. Hold my hand?");
        put("close:shy:rampage:teen", "...I'm scared of it sometimes.", "Okay. I'll try to listen to you instead of it.");
        put("reply:bold:wandered:little", "No! Go THAT way! *points at the forest*", "*struggles* Wanna go!");
        put("reply:bold:wandered:child", "I'm not scared! I was gonna fight anything that came!", "I can take care of myself! I'm strong!");
        put("reply:bold:wandered:teen", "I'm not a kid anymore. I can handle myself.", "Nothing out here can take me. You know that.");
        put("reply:bold:night:little", "*growls at the dark* Rawr!", "No sleep! Hunt!");
        put("reply:bold:night:child", "I was hunting monsters! Real ones!", "I'm not afraid of the dark! The dark's afraid of me!");
        put("reply:bold:night:teen", "I was patrolling. Someone has to keep an eye on things.", "I'm stronger at night. You know that. I can feel it.");
        put("reply:bold:rampage:child", "It started it! I was gonna finish it!", "No! It's still moving! Let me get it!");
        put("reply:bold:rampage:teen", "I had it! Another minute and it was done!", "Let go! I'm not finished!");
        put("reply:bold:transform:child", "Finally! Now I'm gonna smash it!", "Yeah! Let's GO!");
        put("reply:bold:transform:teen", "Just watch, {call}. This one's mine.", "Stand back. I've got this.");
        put("reply:bold:home:little", "*pouts* Wanna go out...", "*kicks* No home!");
        put("reply:bold:home:child", "Fine. But tomorrow I'm going further.", "Ugh. Fine. Can I at least practice in the yard?");
        put("reply:bold:home:teen", "Yeah, yeah. I'm home. Happy?", "Fine. But next time I'm not coming back early.");
        put("close:bold:wandered:little", "*crosses tiny arms* Hmph!", "...okay. *grumbles*");
        put("close:bold:wandered:child", "Fine... but you have to teach me to fight better.", "Okay! But next time you come with me and we fight stuff together!");
        put("close:bold:wandered:teen", "Fine. I'll say where I'm going. Doesn't mean I'll stay close.", "...Okay. You've got a point. Sort of.");
        put("close:bold:night:little", "*yawns mid-growl* ...sleep...", "Okay... tomorrow hunt?");
        put("close:bold:night:child", "Fine. But I'm sleeping with my kagune out in case.", "Okay... but if something comes, I'm fighting it.");
        put("close:bold:night:teen", "Fine. I'll patrol in the yard.", "...Okay. You can come next time.");
        put("close:bold:rampage:child", "*huffs* ...Fine. But I would've won.", "Okay... it was kind of a lot.");
        put("close:bold:rampage:teen", "...Okay. I didn't notice I was bleeding. That's bad, huh.", "Fine. Teach me how you stop. Because I can't.");
        put("reply:curious:wandered:little", "{call}, what's that? And that? And THAT?", "*holds up a rock* Look! Shiny!");
        put("reply:curious:wandered:child", "Did you know the river goes all the way to another river? I wanted to see where!", "I found three kinds of mushrooms! Are any of them poisonous?");
        put("reply:curious:wandered:teen", "I wanted to see what's past the ridge. There's a ruin over there, did you know?", "I was mapping the area. Someone should.");
        put("reply:curious:night:little", "Moon! Why moon? Why dark?", "*points* What's that sound?");
        put("reply:curious:night:child", "Why do owls only come out at night? I wanted to find out!", "I wanted to see if the flowers close at night. They do! Did you know?");
        put("reply:curious:night:teen", "Things are different at night. I wanted to understand why we're stronger in the dark.", "I was reading by moonlight. It's actually bright enough.");
        put("reply:curious:rampage:child", "I wanted to see what would happen if I kept going!", "Why does it want more? Why can't it stop?");
        put("reply:curious:rampage:teen", "I wanted to know how far I could push it.", "How do you stop? Like, physically? What does it feel like?");
        put("reply:curious:transform:child", "How does it know where to go? Does it think?", "Why is it this colour? Is it yours or {call}'s?");
        put("reply:curious:transform:teen", "I think I'm starting to understand how it moves.", "Interesting. It reacts faster than I think.");
        put("reply:curious:home:little", "Why home? Why?", "*yawns* Why sleepy?");
        put("reply:curious:home:child", "Can I look up the mushrooms in your books?", "Okay. But can you tell me more about the CCG?");
        put("reply:curious:home:teen", "Fine. Can I borrow that book about kagune?", "Okay. I'm writing all this down, by the way.");
        put("close:curious:wandered:little", "*nods* ...why?", "Okay! What's for snack?");
        put("close:curious:wandered:child", "Okay! But can you show me the river tomorrow?", "Fine. Can I keep the rocks though?");
        put("close:curious:wandered:teen", "Fair. Want to see my map?", "Okay. I'll take you to the ruin sometime. It's fascinating.");
        put("close:curious:night:little", "*sleepy* why moon...", "Okay... story?");
        put("close:curious:night:child", "Okay... but tell me why the owls do it. On the way home?", "Can I read in bed instead?");
        put("close:curious:night:teen", "Okay. I'll research from my window.", "...Fair point. I'll study the dark from inside.");
        put("close:curious:rampage:child", "Okay... can you teach me how to stop it?", "...I'll think about it. That was scary.");
        put("close:curious:rampage:teen", "That makes sense. I'll practise stopping.", "Huh. So the hunger lies. Noted.");
        put("reply:mischief:wandered:little", "*giggles and wriggles* Hehehe! Catch me!", "*blows a raspberry* Pbbbt!");
        put("reply:mischief:wandered:child", "I wasn't running away! I was... hiding! For a game! You lost!", "What? I've been here the whole time. You're the one who's lost.");
        put("reply:mischief:wandered:teen", "I was totally going to be back before you noticed. You noticed fast.", "Define 'far'.");
        put("reply:mischief:night:little", "*giggles* Sneaky!", "Shhh! Secret!");
        put("reply:mischief:night:child", "I was sleepwalking! Definitely sleepwalking!", "I was... um... looking for my sock! Outside!");
        put("reply:mischief:night:teen", "I was asleep. This is a dream. You're dreaming.", "Night walk. Very healthy. Doctors recommend it.");
        put("reply:mischief:rampage:child", "Hehe, did you see it run? It was so funny!", "It was gonna be a prank! A big one!");
        put("reply:mischief:rampage:teen", "Okay, okay, it got a little out of hand.", "In my defence, it looked at me funny.");
        put("reply:mischief:transform:child", "Hehehe, it's gonna get SO squashed.", "Bet I can poke it from here!");
        put("reply:mischief:transform:teen", "Time for some chaos.", "Watch this. It's gonna be hilarious.");
        put("reply:mischief:home:little", "*tries to sneak off again* Hehe...", "Nooo~ *giggles*");
        put("reply:mischief:home:child", "Okay! *immediately starts sneaking to the window*", "Fine. ...Did anyone eat the cookies? Not me.");
        put("reply:mischief:home:teen", "Home. Boring. Love it. Goodnight.", "Fine. But you can't prove anything.");
        put("close:mischief:wandered:little", "*pretends to be asleep* ...zzz...", "*giggles* 'kay!");
        put("close:mischief:wandered:child", "Okay, okay. I'll only hide inside the house.", "Fine. But you have to admit it was a good hiding spot.");
        put("close:mischief:wandered:teen", "Alright, alright. Next time I'll leave a fake note.", "Okay. I'll tell you. Mostly.");
        put("close:mischief:night:little", "*fake snore* ...", "Shhh... *falls actually asleep*");
        put("close:mischief:night:child", "Okay. ...Can I still look for my sock tomorrow?", "Fine. But I'm putting a decoy in my bed next time. Kidding!");
        put("close:mischief:night:teen", "Fine. My window's staying closed. Probably.", "Okay. You win this round.");
        put("close:mischief:rampage:child", "Okay... *small voice* it wasn't really funny, was it.", "...Sorry. I'll save the pranks for people.");
        put("close:mischief:rampage:teen", "Yeah. Okay. That wasn't a joke anymore.", "Fine. Lesson learned. Mostly.");
        put("reply:gentle:wandered:little", "*holds out a flower* For {call}!", "*hugs* Missed {call}...");
        put("reply:gentle:wandered:child", "I'm sorry! I was picking flowers for you and I went too far...", "There was a hurt bird. I wanted to help it.");
        put("reply:gentle:wandered:teen", "There was a stray dog. I was trying to find its owner.", "I'm sorry. I didn't mean to make you worry.");
        put("reply:gentle:night:little", "*rubs eyes* {call} sad?", "*holds out arms* Up...");
        put("reply:gentle:night:child", "I heard a kitten crying. I wanted to help it.", "I'm sorry... I thought you were asleep and I didn't want to wake you.");
        put("reply:gentle:night:teen", "I went to check on the neighbours' place. I heard something.", "I'm sorry. I couldn't sleep and I thought a walk would help.");
        put("reply:gentle:rampage:child", "*crying* I didn't want to hurt it that much...", "I'm sorry, I'm sorry, I couldn't stop...");
        put("reply:gentle:rampage:teen", "I just wanted it to stop hurting people. Then I couldn't stop.", "...I hurt it so badly. Is that okay? Is it okay?");
        put("reply:gentle:transform:child", "I'll be careful, {call}. Promise.", "I don't want to hurt anyone... just protect.");
        put("reply:gentle:transform:teen", "I'll only use it to protect.", "Stay back, {call}. I'll keep you safe.");
        put("reply:gentle:home:little", "*kisses your cheek*", "Love {call}...");
        put("reply:gentle:home:child", "Thank you for coming to get me. I love you.", "Can we feed the bird tomorrow?");
        put("reply:gentle:home:teen", "Thanks, {call}. Sorry again.", "I'll make you tea. You look tired.");
        put("close:gentle:wandered:little", "*nods and hugs*", "Sowwy {call}...");
        put("close:gentle:wandered:child", "Okay. I'm sorry I made you worry, {call}.", "Okay... can we go help the bird together?");
        put("close:gentle:wandered:teen", "You're right. I'll tell you next time.", "I'm sorry. I really am.");
        put("close:gentle:night:little", "*snuggles* ...love...", "Okay... night night...");
        put("close:gentle:night:child", "Okay. Can we look for the kitten in the morning?", "I'm sorry. Are you okay? You look scared.");
        put("close:gentle:night:teen", "Okay. I'll wake you next time.", "Sorry. Go back to sleep, okay? I'm fine.");
        put("close:gentle:rampage:child", "*hugs tightly* I don't want to be like that...", "Okay... I'll stop next time. I promise.");
        put("close:gentle:rampage:teen", "...Okay. I'll listen to you. Not it.", "Thank you for stopping me.");
        put("reply:serious:wandered:little", "*frowns* No. Busy.", "*very serious* Exploring.");
        put("reply:serious:wandered:child", "I was surveying the perimeter. Someone has to.", "I had a plan. I was going to be back by sunset. It's not sunset yet.");
        put("reply:serious:wandered:teen", "I calculated the risk. It was acceptable.", "I was scouting. It's important to know the terrain.");
        put("reply:serious:night:little", "*stern* Guarding.", "No sleep. Watch.");
        put("reply:serious:night:child", "I was on watch. Somebody has to watch at night.", "I heard something. I went to investigate. That's the protocol.");
        put("reply:serious:night:teen", "I was keeping watch. You and {call} need sleep.", "Investigators patrol at night. I wanted to know their route.");
        put("reply:serious:rampage:child", "It was a threat. I was neutralising it.", "I followed the procedure. Protect, then strike.");
        put("reply:serious:rampage:teen", "It was still a danger. I was finishing the job.", "I had the situation under control.");
        put("reply:serious:transform:child", "I'm in control. Don't worry.", "Kagune deployed. Assessing.");
        put("reply:serious:transform:teen", "Understood. I'll keep it tight.", "Focus. Breathe. Strike.");
        put("reply:serious:home:little", "*nods solemnly*", "Home. Good.");
        put("reply:serious:home:child", "Understood. I'll write a report about what I found.", "Fine. I'll go to bed. At a reasonable time.");
        put("reply:serious:home:teen", "Understood. I'll follow the rules.", "Fine. I'll give you my route next time.");
        put("close:serious:wandered:little", "*nods* ...'kay.", "*salutes clumsily*");
        put("close:serious:wandered:child", "...Fine. That's a reasonable rule.", "Understood. I'll stay within the boundary.");
        put("close:serious:wandered:teen", "Fair. I'll check in from now on.", "Point taken. I'll leave a note with my route.");
        put("close:serious:night:little", "*nods, eyes closing*", "Watch... tomorrow...");
        put("close:serious:night:child", "Understood. I'll watch from my window instead.", "Okay. You're right. It was a risk.");
        put("close:serious:night:teen", "Understood. You're right, that wasn't smart.", "Fine. I'll keep watch from the roof. Inside the boundary.");
        put("close:serious:rampage:child", "...I lost count of how long I fought. That's bad.", "Understood. Stop when it runs. I'll remember.");
        put("close:serious:rampage:teen", "...You're right. I wasn't in control. I'll train it.", "Understood. Next time I'll disengage.");
    }

    /*
     * 3.2.0: the word at the door, longer and by who is talking. "door:<parent>:<why>:<age>" is what the parent says at the door knowing
     * why it went after the child (it strayed, it was out at night, it went on a rampage; heard alongside the old "open:<parent>:home"
     * lines); "doorreply:<temper>:<why>:<age>" the child's answer to that (alongside the old "reply:<temper>:home"); "answer:<parent>:
     * <temper>:<age>" the parent to that kind of child, whatever it said; "last:<temper>:home:<age>" the child's last word, now and then.
     */
    static {
        // ---- Touka at the door: blunt, counting the scrapes, soft the moment nobody's looking.
        put("door:touka:wandered:little", "*sets {name} down inside and points at the floor* This is home. Your feet stay on this floor. Got it?",
            "There. Inside. *checks {name}'s hands and knees* No scrapes. You got lucky, shorty.",
            "Home. Next time you see a butterfly, you come get me. You don't chase it to the end of the world.");
        put("door:touka:wandered:child", "In. Shoes off. You walked all that way, you can walk to the sink and wash up.",
            "Home. You know how many places I checked before I found you? Too many. Way too many.",
            "*holds the door open* After you, explorer. And wipe that look off your face.");
        put("door:touka:wandered:teen", "Home. I'm not lecturing you at the door. I'm lecturing you over dinner.",
            "*drops onto the step* I almost called in everyone I know. Over a walk. Sit down a second.",
            "There. Back in one piece. That's the part I care about. The rest we fight about later.");
        put("door:touka:night:little", "*carries {name} in and kicks the door shut behind them* There. The dark stays out there. You stay in here.",
            "Home. Shh. Feel that? Warm. That's what inside is for at night.",
            "*rubs {name}'s cold hands between hers* Ice cubes. You've got ice cubes for fingers. Bed.");
        put("door:touka:night:child", "Inside. There's a lock going on that window tomorrow. Don't give me that look.",
            "Home. It's the middle of the night, {name}. The moon isn't going anywhere. Neither are you.",
            "*shuts the door, leans on it and lets out a long breath* ...Okay. You're in. Go get warm.");
        put("door:touka:night:teen", "Home. Bed. If you're not asleep in ten minutes, I'm sitting on you.",
            "There. Mud on your shoes and a look on your face. We're talking about both tomorrow.",
            "*locks the door behind them* Next time you can't sleep, you knock on my door. Even if I bite your head off.");
        put("door:touka:rampage:child", "Sit. *grabs a cloth* Hold still. You're getting cleaned up whether you like it or not.",
            "Home. Let me see your hands. ...Shaking. That's normal. It stops. Breathe with me.",
            "Inside. Nobody's chasing you in here, and you're not chasing anybody. It's done.");
        put("door:touka:rampage:teen", "Sit down before you fall down. I'm getting the first-aid box. Don't argue.",
            "Home. Your kakugan's still lit. Close your eyes. Count to twenty. I'll wait.",
            "*tosses {name} a towel* Blood off your face first. Then food. Then we talk about what you almost did.");
        // ---- Ken at the door: gentle, kneeling to their height, more frightened than he lets show.
        put("door:ken:wandered:little", "*kneels and unbuttons {name}'s muddy coat* There we go. Home. Your little boots are full of forest.",
            "Home. *sets {name} down by the fire* Warm your hands, okay? I'll sit right here.",
            "There. Safe. *lets out a breath he'd been holding* I'm not angry. I was scared. Those are different.");
        put("door:ken:wandered:child", "Home. Sit with me a minute? I'm not going to shout. I just want to hear about it.",
            "*hangs up {name}'s coat* We're home. I made a list in my head of everywhere you could be. You weren't on it.",
            "There. Thank you for walking back with me and not running off again. That meant a lot.");
        put("door:ken:wandered:teen", "We're home. I'll make coffee, and you can tell me as much or as little as you want.",
            "Home. I know you're not little anymore. I just... worry like you are. I'm working on it.",
            "*holds the door* After you. ...You walked further than I ever did at your age. That isn't a compliment. Mostly.");
        put("door:ken:night:little", "*wraps {name} in the blanket off the couch* There. All warm. The night can't get in here.",
            "Home. Shh, shh. You're yawning. Your body knows what time it is, even if you don't.",
            "*sits down with {name} in his lap* We're home. Let's stay right here a moment, you and me, until you're warm.");
        put("door:ken:night:child", "Home. I'll warm some milk. Then bed, and I'll leave the hall light on.",
            "*draws the curtains* There. In here the dark is just the dark. Out there it's... more. Do you understand?",
            "We're back. I'm going to sit by your bed tonight. Not because you're in trouble. Because I want to.");
        put("door:ken:night:teen", "Home. I won't sleep for a while anyway. Tea? We can just sit.",
            "We're in. ...When I couldn't find you, I remembered every bad night I've ever had. Please don't do that to me.",
            "Home. Go and rest. I'll be in the kitchen if you want to tell me what was out there.");
        put("door:ken:rampage:child", "Sit here. *kneels with a damp cloth* Let me see. ...It's healing already. You're alright. You're alright.",
            "Home. Your hands are still shaking. So are mine. Let's breathe together for a minute.",
            "*wipes the blood from {name}'s chin* There. Nobody is going to hurt you here. And you don't have to hurt anyone.");
        put("door:ken:rampage:teen", "Sit. I'll get bandages. ...And then I'll tell you about the first time I couldn't stop. You should know.",
            "We're home. You stopped. Remember that more than anything you did before it.",
            "Here. Eat something. The hunger lies to you after a fight. Food first, then we talk.");
        // ---- Eto at the door: theatrical, possessive, a writer to the bone, and underneath it all, frightened for them.
        put("door:eto:wandered:little", "*plops {name} on the couch like a sack of treasure* Home! Captured, returned and catalogued.",
            "Home. Little legs, enormous ambitions. You get that from me, I'm afraid. Condolences.",
            "There. The tiny explorer is back on the shelf where she keeps her favourite things. *pats {name}'s head*");
        put("door:eto:wandered:child", "Home. Sit. I want the whole story from the very first step. Leave nothing out. I'm taking notes.",
            "*closes the door with a flourish* And so the runaway returned, muddy and unrepentant. Chapter closed. Go wash.",
            "Home. Next time you want an adventure, leave me a note. A good one. With foreshadowing.");
        put("door:eto:wandered:teen", "Home. The mysterious disappearance only works if nobody can find you. I always can. Remember that.",
            "*hangs up her coat* Welcome back. I've already written three versions of where you went. Tell me which is true.",
            "Home. I'm not angry. I'm curious. That's so much worse, darling.");
        put("door:eto:night:little", "*tucks {name} inside her coat* Shh. Home now. The night was lovely, I know. The night is also hungry.",
            "Home. Look at you, all big eyes. The dark tried to keep you, didn't it? It can't. You're spoken for.",
            "There. Inside. I'm going to read you the boring story. The one that always works.");
        put("door:eto:night:child", "Home. You went out to meet the night alone? Rude. I'd have come. I adore the night.",
            "*draws the curtains one by one* There. The dark is outside. The monsters are outside. Except me. Bed.",
            "Home. It's very late, little moth. You flew toward something. Tomorrow you tell me what.");
        put("door:eto:night:teen", "Home. A midnight walk. How gothic. I'm almost proud. Almost. Bed.",
            "*leans in the doorway* There. Next time you sneak out, take me. I know better places. Safer, mostly.",
            "Home. I didn't sleep either, if that helps. It shouldn't. Go to bed.");
        put("door:eto:rampage:child", "Sit. *wipes {name}'s face with her sleeve* Look at you. Red to the elbows. What an entrance.",
            "Home. Your kagune's tired. You're tired. I'm tired of being frightened. Let's all lie down.",
            "*kneels, eye to eye with {name}* There. You came back. That's the only rule I really care about. Always come back.");
        put("door:eto:rampage:teen", "Home. Sit and bleed quietly while I find the thread. Yes, I'm stitching. No, it won't be pretty.",
            "There. You went somewhere just now, in your head. I know that place. I lived there. Don't move in.",
            "*hands {name} a cup* Drink. Sleep. Tomorrow you tell me how it felt, and I'll tell you why that frightens me.");

        // ---- The child's answer at the door, by its temper and what it did.
        put("doorreply:sunny:wandered:little", "*bounces* I saw a rabbit! A BIG one!", "Home! *runs in a circle* Home home home!");
        put("doorreply:sunny:wandered:child", "I found the best hill, {call}! Can we go together tomorrow? Please?",
            "I wasn't lost! I was just really, really far. That's different!");
        put("doorreply:sunny:wandered:teen", "Okay, okay. But the view from the ridge was amazing. You'd have loved it.",
            "Sorry, {call}. I lost track of time. The sun was too nice.");
        put("doorreply:sunny:night:little", "*yawns hugely* Stars...", "The moon followed me! *giggles*");
        put("doorreply:sunny:night:child", "But the fireflies, {call}! There were SO many!", "I just wanted to see the moon up close. It's bigger outside.");
        put("doorreply:sunny:night:teen", "I know, I know. The sky was just really clear tonight.", "Sorry. Couldn't sleep. The night was too pretty to waste.");
        put("doorreply:sunny:rampage:child", "*sniffles, then smiles* I'm okay. See? I'm okay!", "Did you see me? I was so fast! ...Too fast, I guess.");
        put("doorreply:sunny:rampage:teen", "I'm fine. Really. Mostly. It stings a bit.", "Sorry. I got carried away. I'll eat and I'll be good as new.");
        put("doorreply:shy:wandered:little", "*hides face in {call}'s coat*", "*holds a fistful of {call}'s sleeve and won't let go*");
        put("doorreply:shy:wandered:child", "...I didn't mean to go so far. Everything started to look the same.", "*very quietly* I thought you'd be mad.");
        put("doorreply:shy:wandered:teen", "...Sorry. I needed somewhere quiet.", "*doesn't look up* I didn't think anyone would notice I was gone.");
        put("doorreply:shy:night:little", "*whimpers and burrows in closer*", "*tiny voice* ...dark.");
        put("doorreply:shy:night:child", "...There was a noise. I wanted to see. Then I couldn't find the way back.", "*clings* Can I sleep with the light on?");
        put("doorreply:shy:night:teen", "...I just like it when everyone's asleep. It's easier.", "*mumbles* Sorry. I'll go to bed.");
        put("doorreply:shy:rampage:child", "*trembling* I d-didn't want to... it just came out.", "...Are you scared of me now?");
        put("doorreply:shy:rampage:teen", "...I didn't like how good it felt.", "*stares at the floor* Sorry. Sorry.");
        put("doorreply:bold:wandered:little", "*squirms* Down! Go again!", "*stamps a tiny foot* Not done!");
        put("doorreply:bold:wandered:child", "I wasn't lost. I knew exactly where I was. Mostly.", "I got all the way to the river on my own! Nobody's ever done that!");
        put("doorreply:bold:wandered:teen", "I can look after myself, {call}. You know I can.", "It was a walk. People walk. It's allowed.");
        put("doorreply:bold:night:little", "*growls at the dark window*", "Not sleepy! NOT!");
        put("doorreply:bold:night:child", "I'm not scared of the dark. The dark should be scared of me.", "I was guarding the house. From outside. It's a strategy.");
        put("doorreply:bold:night:teen", "There's nothing out there scarier than me.", "I'm not a kid. I can be out at night.");
        put("doorreply:bold:rampage:child", "I was winning! I had it!", "It started it! I just finished it!");
        put("doorreply:bold:rampage:teen", "I had it handled. You didn't have to drag me back.", "It wasn't a rampage. It was a hunt. There's a difference.");
        put("doorreply:curious:wandered:little", "*holds up a snail* Look!", "Why house? Why not outside?");
        put("doorreply:curious:wandered:child", "Did you know there's a cave past the hill? I counted the bats. Forty-two!",
            "I followed a beetle. It went really far. Beetles are fast, {call}.");
        put("doorreply:curious:wandered:teen", "I wanted to see where the old road goes. It goes somewhere weird.", "I was mapping the forest. I've got most of the north side now.");
        put("doorreply:curious:night:little", "Why dark? Where sun go?", "*points at the window* Owl! Saw owl!");
        put("doorreply:curious:night:child", "Did you know some flowers only open at night? I had to check.", "I wanted to see if the stars move. They do! A little!");
        put("doorreply:curious:night:teen", "The night birds sound completely different. I was keeping track of which ones.",
            "I wanted to see what the forest does when nobody's watching it.");
        put("doorreply:curious:rampage:child", "Why did it keep wanting to go? My kagune. It wanted more.", "Is it always that loud in your head when it comes out?");
        put("doorreply:curious:rampage:teen", "I couldn't stop. I need to know why I couldn't stop.", "I want to understand what happened. Will you explain it? Properly?");
        put("doorreply:mischief:wandered:little", "*giggles and points the other way* Go there!", "Hehe. Hided!");
        put("doorreply:mischief:wandered:child", "I wasn't running away, I was running TOWARD something. Totally different.",
            "*innocent face* What? I was in the garden. A very big garden.");
        put("doorreply:mischief:wandered:teen", "Technically I never left. I was just very far into the yard.", "You found me faster than last time. You're getting better.");
        put("doorreply:mischief:night:little", "*giggles* Sneaky!", "Shhh! *puts a finger on {call}'s lips*");
        put("doorreply:mischief:night:child", "I was being a ninja. Ninjas go out at night. It's the rules.", "*whispers* We don't have to tell anyone about this, right? Right?");
        put("doorreply:mischief:night:teen", "I was getting snacks. From very, very far away.", "In my defence, the window was already open.");
        put("doorreply:mischief:rampage:child", "*grins with red teeth* ...Was it cool though?", "Okay, but did you SEE it run?");
        put("doorreply:mischief:rampage:teen", "Okay, that got a little out of hand. A little.", "I was going to stop. Like, any second.");
        put("doorreply:gentle:wandered:little", "*pats {call}'s cheek* Sowwy.", "*holds out a squashed flower*");
        put("doorreply:gentle:wandered:child", "I'm sorry I worried you. Were you scared? You look scared.",
            "I picked these for you. That's why I went so far. *holds out wilted flowers*");
        put("doorreply:gentle:wandered:teen", "I'm sorry, {call}. I should've told you. I hate making you worry.",
            "I was helping a lost dog find its way home. Then I was the lost dog.");
        put("doorreply:gentle:night:little", "*rubs eyes* {call} sad?", "*yawns and hugs {call}'s neck*");
        put("doorreply:gentle:night:child", "I heard a bird crying. I wanted to help it. I'm sorry.", "Are you cold, {call}? You came out without your coat. That's my fault.");
        put("doorreply:gentle:night:teen", "I'm sorry you had to come out in the cold. I'll make you something warm.", "I just needed air. I'm sorry I scared you.");
        put("doorreply:gentle:rampage:child", "*crying* Is it hurt? The thing I hit? Is it okay?", "I'm sorry, I'm sorry. I didn't want to be like that.");
        put("doorreply:gentle:rampage:teen", "I hurt it more than I had to. I know I did.", "Did I scare you? Tell me honestly.");
        put("doorreply:serious:wandered:little", "*points firmly at the door* Home.", "*nods* Back.");
        put("doorreply:serious:wandered:child", "I was making a survey of the area. I'll have it written up by tomorrow.",
            "I worked out I'd be back before dark. I was wrong by an hour.");
        put("doorreply:serious:wandered:teen", "I had a plan and a route. I should have shared them. That's on me.",
            "Understood. Next time I'll leave a note with when I'll be back.");
        put("doorreply:serious:night:little", "*very seriously* Monsters. Checked.", "*solemn* Dark. Big.");
        put("doorreply:serious:night:child", "I was keeping watch. Someone has to keep watch at night.", "I wanted to test whether I can see in the dark. I can. Mostly.");
        put("doorreply:serious:night:teen", "I was patrolling. I know how that sounds.", "It was a calculated risk. The calculation was bad.");
        put("doorreply:serious:rampage:child", "I lost control. I need to practise more.", "That wasn't acceptable. I know.");
        put("doorreply:serious:rampage:teen", "I went past what was necessary. It won't happen again.", "I need you to train me harder. So that doesn't happen again.");

        // ---- The parent's answer to that kind of child (whatever it just said).
        put("answer:touka:sunny:little", "*ruffles {name}'s hair* Yeah, yeah. You're very happy. Happy and grounded.",
            "*can't keep a straight face* Stop smiling at me like that. I'm trying to be mad.");
        put("answer:touka:sunny:child", "Don't grin at me. I'm still deciding how much trouble you're in.", "You're impossible to stay mad at. It's really annoying.");
        put("answer:touka:sunny:teen", "You and that smile. Fine. Next time I come with you, and you show me.", "Don't charm your way out of this. ...It's working a little. Go eat.");
        put("answer:touka:shy:little", "*holds {name} tighter* Hey. Hey. I've got you. Nobody's mad.", "*softly* It's okay, little one. You're home. That's all.");
        put("answer:touka:shy:child", "I'm not mad at you. I was scared. Those look the same on me, I know.",
            "*kneels* Look at me. You can always come back. Always. No matter what.");
        put("answer:touka:shy:teen", "I always notice. You're kind of the main thing I notice.", "You want quiet, I'll find you quiet. Just not out there alone.");
        put("answer:touka:bold:little", "*catches {name} mid-wriggle* Nope. Feet down. You're not going anywhere, tiny.", "Ha. You got my temper. Poor you.");
        put("answer:touka:bold:child", "Tough, huh? Even tough kids tell their mother where they're going.", "Brave's good. Brave and stupid is how people get eaten. Learn the difference.");
        put("answer:touka:bold:teen", "I know you can. So could I, at your age. I still nearly didn't come home a few times.",
            "You want me to trust you out there? Then earn it. Start by telling me where 'out there' is.");
        put("answer:touka:curious:little", "Questions, questions. You get one more and then it's bed. Choose wisely.", "Yes, very interesting. Interesting things can wait till morning.");
        put("answer:touka:curious:child", "Write it down. All of it. And next time we go and look together.", "You can be curious from inside the house. Books exist. Read one. In bed.");
        put("answer:touka:curious:teen", "Fine. Show me your notes tomorrow. If they're good, I'll show you the places that aren't on any map.",
            "Curiosity's good. Getting killed for it isn't. Find the middle.");
        put("answer:touka:mischief:little", "*narrows her eyes* I see you. I always see you.", "Giggle all you want. You're still going in the bath.");
        put("answer:touka:mischief:child", "Uh-huh. You get that from somewhere and I refuse to believe it's me.", "Nice try. Dishes. All week.");
        put("answer:touka:mischief:teen", "Cute. Grounded is also cute. You'll find out.", "Keep it up and I start locking the windows. All of them.");
        put("answer:touka:gentle:little", "*catches {name}'s little hand* ...Thanks. Idiot. *kisses {name}'s forehead*", "*sighs and melts completely* How am I supposed to scold that face.");
        put("answer:touka:gentle:child", "Don't worry about me. Worrying about you is my job. Get your own.", "I'm fine. Really. Now that you're here, I'm fine.");
        put("answer:touka:gentle:teen", "You don't have to look after me. ...But thanks. Really.", "Next time worry about you first. I'm tougher than I look.");
        put("answer:touka:serious:little", "*nods back, just as solemn* Good. Home. Report accepted.", "*fighting a smile* Yes, sir. Bedtime, sir.");
        put("answer:touka:serious:child", "Look at you, taking it like a soldier. Fine. At ease. Go wash up.", "Good. Then you know what you did wrong and I don't have to yell. Nice.");
        put("answer:touka:serious:teen", "Good. Plans are good. Plans I know about are better.", "Okay. I'll hold you to that. Not because I don't trust you. Because I've lost people.");
        put("answer:ken:sunny:little", "*laughs softly* Yes, yes. You can tell me everything in the bath.", "You're so happy. I'm glad. ...Please don't do that again.");
        put("answer:ken:sunny:child", "I love that you find wonderful things everywhere. I'd just like to be there when you do.", "You make it very hard to be stern, you know. I am trying.");
        put("answer:ken:sunny:teen", "You always find the bright side. Keep that. Just bring it home before dark.", "I'm glad it was beautiful. I'm gladder you're home.");
        put("answer:ken:shy:little", "*rubs {name}'s back in slow circles* Shh. I'm not going anywhere.", "It's alright. You can hold on as long as you like.");
        put("answer:ken:shy:child", "I'm not mad. I promise. I was just... very worried. Come here.",
            "You can always come home. Even when you think I'll be upset. Especially then.");
        put("answer:ken:shy:teen", "I'd always notice. Always. You matter more than you think you do.",
            "If you need quiet, I'll make some for you. You don't have to leave to find it.");
        put("answer:ken:bold:little", "*sets {name} down, one hand still on {name}'s back* Not tonight, little lion.", "*smiles* So brave. And so very tired, I think.");
        put("answer:ken:bold:child", "I know you're brave. Brave people still come home. That's the bravest part.", "You remind me of me. That's what worries me.");
        put("answer:ken:bold:teen", "I believe you. I just need you to believe I'll worry anyway. Both things are true.",
            "You can look after yourself. Let me look after you a little too. It's good for me.");
        put("answer:ken:curious:little", "*listens very seriously* Wow. Tell me all about it in the morning. Every bit.", "So many whys. Those are tomorrow's whys. Tonight is for sleeping.");
        put("answer:ken:curious:child", "That's fascinating. Truly. Write it down, and tomorrow I'll find you a book about it.",
            "We can explore together next time. I'm a slow walker, but I'm a good listener.");
        put("answer:ken:curious:teen", "I'd love to see your notes. And next time, I'd love to be asked along.", "Questions are good. Questions you ask from somewhere safe are better.");
        put("answer:ken:mischief:little", "*scoops {name} back before the next escape begins* Oh no you don't.", "*smiles* I see that grin. Nobody's that pleased with themselves for nothing. Bath, little rascal.");
        put("answer:ken:mischief:child", "That's... a very creative explanation. I almost believe it.", "That face is very convincing. I'm choosing not to be convinced.");
        put("answer:ken:mischief:teen", "*sighs* You know I can't stay angry. Please don't use that against me.",
            "I'll pretend I believe you, and you pretend to be sorry. Deal?");
        put("answer:ken:gentle:little", "*holds {name}'s hand like something precious* Thank you. I'm alright now.", "You always think of everyone else first. Think of you, too. Okay?");
        put("answer:ken:gentle:child", "I'm alright. You don't have to carry my worry. That one's mine.", "Thank you. Now let me look after you for a while.");
        put("answer:ken:gentle:teen", "You didn't scare me. ...You did. But I'm alright now. Are you?", "Sit with me. You don't owe me anything. I just like having you near.");
        put("answer:ken:serious:little", "*nods gravely* Thank you for your report, little captain.", "Very good. Home. Bath. Bed. In that order.");
        put("answer:ken:serious:child", "Thank you for being honest. That's all I ever want from you.", "You're very hard on yourself. I know where that comes from. Go easy, okay?");
        put("answer:ken:serious:teen", "Thank you for owning it. That's more than most grown-ups manage.", "You take responsibility like a grown-up. I'm proud of that. Still worried. But proud.");
        put("answer:eto:sunny:little", "*squishes {name}'s cheeks* How are you this delighted after giving me a heart attack? Monstrous. I love it.",
            "Yes, yes, yay. Home. The best place in any story.");
        put("answer:eto:sunny:child", "Tomorrow, little sunbeam, I'll take you somewhere so wonderful you'll forget all about today.",
            "You're impossible to scold. It's a superpower. Use it for evil sparingly.");
        put("answer:eto:sunny:teen", "Hm. Next time take me. I'll bring snacks and bad opinions.", "Don't you smile at me like that. I wrote that smile. I know how it works.");
        put("answer:eto:shy:little", "*hums against {name}'s hair* Shh. Mama's here. Nothing gets past her.", "*holds on just as tight* Mine. Safe. Shh.");
        put("answer:eto:shy:child", "Mad? Darling, I'm never mad at you. I'm mad at the world for being so big.",
            "*brushes {name}'s hair back* You can always come back to me. That isn't a rule. It's a promise.");
        put("answer:eto:shy:teen", "Oh, sweetheart. I notice everything. You most of all.", "Quiet is precious. I'll make you a quiet room. With a lock. Only you and I get keys.");
        put("answer:eto:bold:little", "*laughs* Look at that face. A villain already. My heart.", "No more conquering tonight, little tyrant. Tomorrow you may have the garden.");
        put("answer:eto:bold:child", "Brave, reckless and very small. The first two you get from me. The third will pass.",
            "Fearless heroes die in chapter three. Clever ones make it to the sequel. Be clever.");
        put("answer:eto:bold:teen", "Oh, I know you can. That's exactly what frightens me.", "You're dangerous. Good. Dangerous people tell their mothers where they're going.");
        put("answer:eto:curious:little", "Look at you, wondering at everything. A little scholar. A tiny, muddy scholar.", "Why? Because the world is enormous and you're a crumb. A wonderful crumb.");
        put("answer:eto:curious:child", "Bring me your notes. We'll make them into a story. A true one. Those are the best.",
            "Every time I think you can't surprise me, you find something new to wonder at. ...You're absolutely my child.");
        put("answer:eto:curious:teen", "Show me your notes. I'll add the places I'm not supposed to know about.",
            "Curiosity is my favourite sin. Just don't let it kill you. That's a terrible ending.");
        put("answer:eto:mischief:little", "*gasps theatrically* A criminal! In my house!", "Fufu. That giggle is mine. You stole it. Give it back.");
        put("answer:eto:mischief:child", "Oh, that's good. Structure, delivery, a hint of menace... seven out of ten. Now go to bed.", "You can't fool the woman who invented that face, darling.");
        put("answer:eto:mischief:teen", "Brilliant. Utterly brilliant. Still grounded.", "I'd be proud if I weren't the victim. ...I'm still a little proud.");
        put("answer:eto:gentle:little", "*kisses {name}'s little hand like a crown jewel* For me? That's going in my memoirs. The nice chapter.", "*melts* How did I make something this soft? I'm a monster. Allegedly.");
        put("answer:eto:gentle:child", "Worried about me? Sweetheart, I'm the scariest thing in this forest. Worry about the forest.",
            "Come here. A hug is a perfectly good apology. The only kind I take, actually.");
        put("answer:eto:gentle:teen", "You didn't scare me. You terrified me. Which is very different and far more flattering.",
            "Don't look after me. Let me look after you. I'm out of practice. Let me practise.");
        put("answer:eto:serious:little", "*salutes gravely* Thank you, Captain. Dismissed to bed.", "Such a solemn little face. Like a tiny judge. I adore you.");
        put("answer:eto:serious:child", "So grave. So dignified. So muddy. Go and wash, Your Honour.", "You're so serious. Where did I get you? ...Never mind. I know where.");
        put("answer:eto:serious:teen", "Good. I like a child who takes responsibility. It makes it so much easier to blame you.",
            "Rules, plans, notes. Yes. And also: come here. I was frightened.");

        // ---- The child's last word, now and then.
        put("last:sunny:home:little", "*yawns and giggles at the same time*", "Love you! *sloppy kiss*");
        put("last:sunny:home:child", "Okay! Love you, {call}! Night!", "Yay! *hugs {call} around the middle*");
        put("last:sunny:home:teen", "Love you. Don't tell anyone I said that.", "Okay. Thanks for coming to get me. Really.");
        put("last:shy:home:little", "*falls asleep still holding on*", "*tiny* ...love.");
        put("last:shy:home:child", "*whispers* ...Can you stay till I fall asleep?", "*nods, and hugs {call} quick and tight*");
        put("last:shy:home:teen", "...Okay. Thanks.", "*small smile* ...I'm glad it was you who came.");
        put("last:bold:home:little", "*yawns* ...not tired...", "*grumbles into {call}'s shoulder*");
        put("last:bold:home:child", "Fine. But next time I'm going further. With you.", "*crosses arms* ...Okay. Fine. Sorry.");
        put("last:bold:home:teen", "Yeah. Fine. ...Sorry.", "Okay, okay. I'll tell you next time.");
        put("last:curious:home:little", "Why sleep?", "*looks out the window one last time, just to be sure*");
        put("last:curious:home:child", "Can I at least write it down before bed?", "Okay. But tomorrow you have to answer three questions.");
        put("last:curious:home:teen", "Fine. But you owe me an explanation. A real one.", "Okay. I'm still finishing those notes, though.");
        put("last:mischief:home:little", "*giggles and wriggles* Again!", "Hehe. Night night.");
        put("last:mischief:home:child", "Okay. ...Don't look under my bed. No reason.", "*salutes, already plotting*");
        put("last:mischief:home:teen", "Grounded. Got it. *already looking at the window*", "You love me, though.");
        put("last:gentle:home:little", "*pats {call}'s face* Night night.", "*kisses {call}'s cheek*");
        put("last:gentle:home:child", "I love you. I'm sorry. I love you.", "*hugs {call}* You're warm.");
        put("last:gentle:home:teen", "I'll make you tea tomorrow. To say sorry.", "Thanks for coming for me. I mean it.");
        put("last:serious:home:little", "*nods once, satisfied*", "*solemn yawn*");
        put("last:serious:home:child", "Understood. Goodnight.", "I'll do better. You'll see.");
        put("last:serious:home:teen", "Noted. Goodnight, {call}.", "Thank you. I mean it. I'll do better.");
    }
}
