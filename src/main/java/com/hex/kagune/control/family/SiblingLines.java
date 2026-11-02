package com.hex.kagune.control.family;

import java.util.HashMap;
import java.util.Map;

/**
 * 1.3.51: what brothers and sisters say to each other ({@link Siblings#talk}), and what a parent says breaking up their squabble: by the
 * speaker's temper ({@link ChildTemper}) or "any", the kind of moment ({@link Siblings.Kind}), and its age ("little" a toddler, "big"
 * older; "any" for a parent's). Parts: "open" (the one who starts), "reply", "back", for a squabble "hit" and "hitback", for tag "tagged",
 * a parent's "break" and the children's "sulk". {@code {sib}} is the other one's name, {@code {call}} what the speaker calls the parent,
 * {@code {sibrole}} "brother" or "sister".
 */
final class SiblingLines {
    private static final Map<String, String[]> LINES = new HashMap<>();

    private SiblingLines() {
    }

    private static void put(String key, String... lines) {
        LINES.put(key, lines);
    }

    /** The lines for the key, falling back from the speaker's temper to "any", and between "little" and "big". */
    static String[] get(String part, String who, String kind, String group) {
        String[] lines = LINES.get(part + ":" + who + ":" + kind + ":" + group);
        if (lines == null) {
            lines = LINES.get(part + ":any:" + kind + ":" + group);
        }
        if (lines == null && ("little".equals(group) || "big".equals(group))) {
            // A toddler's line missing: an older one's; an older one's missing: a toddler's.
            String otherGroup = "little".equals(group) ? "big" : "little";
            lines = LINES.get(part + ":" + who + ":" + kind + ":" + otherGroup);
            if (lines == null) {
                lines = LINES.get(part + ":any:" + kind + ":" + otherGroup);
            }
        }
        return lines;
    }

    static int count() {
        int n = 0;
        for (String[] lines : LINES.values()) {
            n += lines.length;
        }
        return n;
    }

    static {
        put("open:sunny:chat:big", "{sib}! Guess what! I saw a fox today. A real one!", "{sib}, wanna know a secret? I like you. Okay that's it.");
        put("open:shy:chat:big", "...{sib}? Can I sit with you?", "{sib}... do you ever get scared at night?");
        put("open:bold:chat:big", "{sib}. When I'm grown I'm gonna be the strongest ghoul ever.", "Hey {sib}, bet I can climb the roof. Watch later.");
        put("open:curious:chat:big", "{sib}, why do you think our eyes go red?", "{sib}, did you know crows remember faces? Do you think they remember us?");
        put("open:mischief:chat:big", "Psst. {sib}. I hid something in your boots. Don't check.", "{sib}, wanna hear the funniest thing ever? ...I forgot it. Wait.");
        put("open:gentle:chat:big", "{sib}, did you eat today? You should eat.", "{sib}, I made you something. It's a flower crown. It's a little broken.");
        put("open:serious:chat:big", "{sib}. We should go over the escape routes again.", "{sib}, I've been keeping track. You've been really good this week.");
        put("reply:sunny:chat:big", "Whoa, really?! Tell me everything!", "Hehe, I like you too, {sib}!");
        put("reply:shy:chat:big", "...yeah. Okay.", "*nods quietly and scoots closer*");
        put("reply:bold:chat:big", "Pfft. I could beat that.", "Yeah? I'm gonna be stronger than you.");
        put("reply:curious:chat:big", "Ooh, good question. Let's look it up.", "Wait, wait, how do you know that?");
        put("reply:mischief:chat:big", "Hehe. Same. ...I did something too.", "You're weird. I like it.");
        put("reply:gentle:chat:big", "Aw, thank you, {sib}.", "That's really sweet of you.");
        put("reply:serious:chat:big", "Noted.", "Hm. Okay. That's fair.");
        put("back:any:chat:big", "*grins* Best sibling ever.", "Okay. Let's tell {call} later.", "*bumps shoulders* Yeah.", "Cool. You're cool.");
        put("open:any:chat:little", "*babbles at {sib}* Ba! Ba!", "{sib}! {sib}! Look!", "*holds up a stick proudly*", "*pokes {sib}* Hi!");
        put("reply:any:chat:little", "*giggles*", "*babbles back*", "Mine!", "*claps*");
        put("open:sunny:tease:big", "{sib}, you snore like a bear! A cute bear!", "Ha! {sib} tripped over nothing again!");
        put("open:shy:tease:big", "...your hair looks funny. Sorry.", "*whispers* you have sauce on your face.");
        put("open:bold:tease:big", "{sib}, you hit like a baby.", "You're slow, {sib}. Slow, slow, slow.");
        put("open:curious:tease:big", "{sib}, did you know you talk in your sleep? About cake?", "Fun fact: you're the only one who's scared of geese.");
        put("open:mischief:tease:big", "{sib} likes someone~ {sib} likes someone~", "Hehe, {sib}'s face when the frog jumped. Classic.");
        put("open:gentle:tease:big", "{sib}, your shirt's on backwards. ...I wasn't going to say anything.", "You missed a spot. *wipes {sib}'s cheek*");
        put("open:serious:tease:big", "Your form was sloppy today, {sib}.", "{sib}, you left your boots in the doorway. Again.");
        put("reply:sunny:tease:big", "Hey! ...Okay, that's fair. Haha!", "At least I'm a cute bear!");
        put("reply:shy:tease:big", "*goes red* ...stop.", "...that's mean. *hides face*");
        put("reply:bold:tease:big", "Say that again. I dare you.", "Oh yeah? Fight me.");
        put("reply:curious:tease:big", "That's not scientifically accurate.", "Do I really? About which cake?");
        put("reply:mischief:tease:big", "Oh, you're gonna regret that.", "Hehe. I'll get you back. You won't see it coming.");
        put("reply:gentle:tease:big", "...okay, thank you for telling me.", "Hehe, I guess I'm a mess.");
        put("reply:serious:tease:big", "Noted. I'll correct it.", "...You're not wrong. Irritating, but not wrong.");
        put("back:any:tease:big", "*sticks out tongue*", "Hehe. Love you, loser.", "*runs off laughing*");
        put("open:any:tease:little", "*pulls {sib}'s hair* Hehe!", "*steals {sib}'s toy* Mine now!", "*blows a raspberry at {sib}*");
        put("reply:any:tease:little", "Nooo! Mine!", "*wails* {call}!", "*blows a raspberry back*");
        put("open:sunny:brag:big", "{sib}! I learned a new move! Wanna see? Wanna see?!", "I helped {call} cook today! I'm basically a chef!");
        put("open:shy:brag:big", "...I fixed the fence. By myself.", "...{call} said I did good today.");
        put("open:bold:brag:big", "I took down a zombie today. Alone. No big deal.", "My kagune's way faster than yours. Just saying.");
        put("open:curious:brag:big", "I read the whole book. The big one. In one night.", "I figured out how the furnace works. Ask me anything.");
        put("open:mischief:brag:big", "I snuck past {call} three times today. Three!", "I'm the best hider in this house. Nobody's ever found me.");
        put("open:gentle:brag:big", "The bird I found can fly again! I helped!", "I grew a flower all by myself. Look.");
        put("open:serious:brag:big", "I trained an extra hour. My strikes are cleaner now.", "I haven't missed a single chore this week.");
        put("reply:sunny:brag:big", "Whoa! Show me! Show me!", "That's so cool! I'm so proud of you!");
        put("reply:shy:brag:big", "...that's really cool.", "*small clap*");
        put("reply:bold:brag:big", "Pfft. I could do that blindfolded.", "Cool. I did it first though.");
        put("reply:curious:brag:big", "How? Explain it step by step.", "Really? Prove it.");
        put("reply:mischief:brag:big", "Cool. Wanna see me do it better?", "Sure you did. Sure.");
        put("reply:gentle:brag:big", "That's amazing! You worked so hard.", "I'm really happy for you.");
        put("reply:serious:brag:big", "Acceptable.", "Good. Keep it up.");
        put("back:any:brag:big", "*puffs up proudly*", "Told you. I'm the best.", "*does a little victory dance*");
        put("open:any:brag:little", "Look! Big! *stands on tiptoes*", "I walked! Far!", "*shows off a muddy rock* Treasure!");
        put("reply:any:brag:little", "Ooooh!", "Me too! Me too!", "*claps*");
        put("open:sunny:scheme:big", "{sib}, if we BOTH ask {call} for cookies, {call} can't say no!", "Let's make {call} breakfast tomorrow! As a surprise!");
        put("open:shy:scheme:big", "...{sib}, will you ask {call} if we can stay up? You're braver.", "...can you help me ask {call} something?");
        put("open:bold:scheme:big", "{sib}. Tonight. We sneak out and hunt. Don't tell {call}.", "If we both train extra, {call} has to let us fight for real.");
        put("open:curious:scheme:big", "{sib}, if we time it right we can get into {call}'s study.", "I wonder what {call} keeps in the locked chest. Want to find out?");
        put("open:mischief:scheme:big", "Okay, {sib}, here's the plan. Frog. {call}'s boot. Tomorrow.", "You distract {call}. I'll get the cake.");
        put("open:gentle:scheme:big", "{sib}, {call} looks tired. Let's do all the chores before {call} wakes up.", "Let's pick flowers for {call}. Shh, it's a surprise.");
        put("open:serious:scheme:big", "{sib}. We need a plan to get a later bedtime. I've drafted a proposal.", "If we present a united front, {call} will consider it.");
        put("reply:sunny:scheme:big", "YES! Best plan ever!", "Ooh, I'm in!");
        put("reply:shy:scheme:big", "...okay. But you go first.", "...I'll come. If you hold my hand.");
        put("reply:bold:scheme:big", "Obviously I'm in.", "Let's do it. No backing out.");
        put("reply:curious:scheme:big", "Hmm. What's our exit strategy?", "Interesting. Go on.");
        put("reply:mischief:scheme:big", "Hehehe. I've been waiting for this.", "I'm so in. This is gonna be legendary.");
        put("reply:gentle:scheme:big", "Okay, but nobody gets hurt.", "Aw, {call}'s gonna love it.");
        put("reply:serious:scheme:big", "I'll need to see the details first.", "Agreed. We move at dawn.");
        put("back:any:scheme:big", "*fist bump*", "Shh! Act normal.", "Okay. Pinky promise.");
        put("open:any:scheme:little", "*whispers loudly* {sib}! Cookie!", "Shhh! *giggles*", "*tugs {sib} toward the kitchen*");
        put("reply:any:scheme:little", "Cookie!", "*giggles* Shhh!", "*nods very seriously*");
        put("open:sunny:comfort:big", "Hey, {sib}! You'll be okay! You're the toughest!", "{sib}, want a hug? I give the best hugs.");
        put("open:shy:comfort:big", "...{sib}? Does it hurt? *sits close*", "...I'm here. *holds {sib}'s hand*");
        put("open:bold:comfort:big", "Who did that to you? I'll get them.", "Get up, {sib}. ...Here, lean on me.");
        put("open:curious:comfort:big", "Let me see. It's healing already. Ghoul cells are amazing.", "{sib}, does it feel better if you don't look at it?");
        put("open:gentle:comfort:big", "Oh, {sib}... come here. It's okay.", "I'll get you something to eat. You'll feel better.");
        put("open:mischief:comfort:big", "Want me to make a funny face? It always works. *makes a face*", "Hey. I saved you the last cookie. Don't tell anyone.");
        put("open:serious:comfort:big", "Sit down. Breathe. I'll keep watch.", "You fought well, {sib}. Rest now.");
        put("reply:sunny:comfort:big", "*sniffles, then smiles* ...thanks, {sib}.", "Haha... okay. Hug.");
        put("reply:shy:comfort:big", "*leans into {sib}* ...thanks.", "...don't go.");
        put("reply:bold:comfort:big", "I'm fine. ...Okay, maybe not. Thanks.", "Pfft. It's a scratch. ...Stay anyway.");
        put("reply:curious:comfort:big", "...it does feel better. Weird.", "Thanks. Tell me more about the cells?");
        put("reply:mischief:comfort:big", "*laughs despite itself* Stop, it hurts to laugh.", "...you're the worst. Thanks.");
        put("reply:gentle:comfort:big", "Thank you, {sib}. You're so kind.", "*hugs back* I'm okay now.");
        put("reply:serious:comfort:big", "...Thank you. I'll recover.", "Acknowledged. ...And thank you.");
        put("back:any:comfort:big", "*stays close*", "Anytime. That's what siblings are for.", "*pats {sib}'s head*");
        put("open:any:comfort:little", "*pats {sib}* Ow? Kiss better.", "*hugs {sib} tight*", "*offers {sib} a slightly chewed biscuit*");
        put("reply:any:comfort:little", "*sniffles* ...better.", "*hugs back*", "*stops crying and giggles*");
        put("open:sunny:squabble:big", "{sib}, that was MY spot!", "Hey! You ate my snack!");
        put("open:shy:squabble:big", "...{sib}, you broke my thing.", "...that's mine. Give it back.");
        put("open:bold:squabble:big", "Move, {sib}. Now.", "You want to fight about it? Let's go.");
        put("open:curious:squabble:big", "You took my book and now there's a page missing!", "{sib}, you moved my rock collection. In what order?!");
        put("open:mischief:squabble:big", "You told {call} it was me! Snitch!", "That was MY prank! You stole my prank!");
        put("open:gentle:squabble:big", "{sib}, that wasn't nice. You hurt my feelings.", "Why would you say that? That's mean.");
        put("open:serious:squabble:big", "You broke the rules, {sib}. Again.", "That's the third time this week. I'm keeping count.");
        put("reply:sunny:squabble:big", "Nuh-uh! I was here first!", "It didn't have your name on it!");
        put("reply:shy:squabble:big", "...I didn't mean to.", "*eyes fill with tears* ...it wasn't me.");
        put("reply:bold:squabble:big", "Make me.", "Bring it, {sib}.");
        put("reply:curious:squabble:big", "Technically it's everyone's book.", "Your system made no sense anyway.");
        put("reply:mischief:squabble:big", "Prove it.", "Hehe. Maybe.");
        put("reply:gentle:squabble:big", "I'm sorry! I didn't mean it like that!", "...I'm sorry. Please don't be mad.");
        put("reply:serious:squabble:big", "You don't get to keep score on me.", "That's not the rule and you know it.");
        put("hit:any:squabble:big", "You ALWAYS do this!", "I'm telling {call}!", "You're so annoying!", "Ugh! I can't even look at you!");
        put("hitback:any:squabble:big", "No, YOU always do this!", "Go ahead! Tell! See if I care!", "You're MORE annoying!", "Fine! Don't look at me!");
        put("open:any:squabble:little", "Mine! MINE!", "*shoves {sib}* No!", "*grabs the toy back* MINE!");
        put("reply:any:squabble:little", "*shoves back* NO! Mine!", "*bursts into tears*", "*bites {sib}*");
        put("open:sunny:tag:big", "Tag! You're it, {sib}!", "Bet you can't catch me! Tag!");
        put("open:shy:tag:big", "...tag. *runs away very fast*", "...you're it. *giggles quietly*");
        put("open:bold:tag:big", "Tag. You'll never catch me.", "You're it, {sib}! Too slow!");
        put("open:curious:tag:big", "Tag! Let's see who has better stamina!", "You're it! I'm testing your reaction time!");
        put("open:mischief:tag:big", "*smacks {sib}'s back* TAG! Hehehe!", "Tag! And I licked my hand first!");
        put("open:gentle:tag:big", "Tag! ...was that too hard? Sorry! You're it!", "Tag, {sib}! Come get me!");
        put("open:serious:tag:big", "Tag. You're it. Ten second head start.", "Tag. Begin.");
        put("reply:any:tag:big", "Hey! Get back here!", "Oh, you're SO dead!", "Not fair! I wasn't ready!", "Come back here!");
        put("tagged:any:tag:big", "Got you! You're it!", "HA! Tag!", "Gotcha! Your turn!", "Tag! Revenge!");
        put("open:any:tag:little", "*giggles and toddles away* Tag!", "*shrieks happily* Chase!", "*runs in a circle* Can't catch!");
        put("reply:any:tag:little", "*squeals and chases*", "Get you! Get you!", "*toddles after, giggling*");
        put("open:sunny:mind:big", "{sib}! Where are you going? Come on, back home!", "Hey, little explorer! Too far! Let's go back!");
        put("open:shy:mind:big", "...{sib}, come back. Please. We'll get in trouble.", "...hold my hand. We're going home.");
        put("open:bold:mind:big", "{sib}! Get back here. Now. ...Please.", "Hey! You don't wander off. I'm in charge. Come on.");
        put("open:curious:mind:big", "{sib}, did you know there are wolves out here? Let's go home.", "Where were you going, {sib}? ...Okay, tell me on the way home.");
        put("open:mischief:mind:big", "{sib}! If {call} finds out we're both dead. Come ON.", "Okay, escape artist. Back to base.");
        put("open:gentle:mind:big", "{sib}, you scared me. Come here. Let's go home.", "It's okay, I've got you. Let's go back to {call}.");
        put("open:serious:mind:big", "{sib}. You're outside the boundary. Come with me.", "This is too far. We're going home. Now.");
        put("reply:any:mind:little", "*giggles and runs the other way*", "Nooo! Bug!", "*holds up arms* Up?", "*pouts* ...okay.");
        put("back:any:mind:big", "Got you. Come on. {call}'s gonna kill us.", "Okay, hold my hand. Don't let go.", "You're lucky I found you first.", "There we go. Home. And not a word to {call}.");
        put("open:sunny:react:big", "Whoa, {sib}! Your kagune's so cool!", "Go, {sib}! You got this!");
        put("open:shy:react:big", "...{sib}'s kagune... it's pretty.", "...be careful, {sib}.");
        put("open:bold:react:big", "Mine's bigger. Just saying.", "Not bad, {sib}. Not as good as mine.");
        put("open:curious:react:big", "Huh, {sib}'s moves differently from mine. Interesting.", "{sib}, why's yours that colour?");
        put("open:mischief:react:big", "No fair, yours is cooler!", "Hehe, {sib}'s gonna squash it!");
        put("open:gentle:react:big", "Please be careful, {sib}!", "Don't get hurt, {sib}!");
        put("open:serious:react:big", "Good deployment, {sib}. Watch your left.", "Focus, {sib}. Don't overextend.");
        put("open:any:react:little", "Ooooh! Pretty!", "*claps* {sib}!", "*hides behind a chair* ...whoa.");
        put("open:sunny:team:big", "I got your back, {sib}!", "Together! Let's go!");
        put("open:shy:team:big", "...I'm with you, {sib}.", "...stay close to me.");
        put("open:bold:team:big", "Nobody touches my {sibrole}!", "Back off! That's my {sibrole}!");
        put("open:curious:team:big", "You go left, I go right. Flank it!", "Two on one. The odds are ours.");
        put("open:mischief:team:big", "Hehe, it doesn't know there's two of us!", "Double trouble! Get it!");
        put("open:gentle:team:big", "Don't worry, {sib}, I'm here!", "Leave {sib} alone!");
        put("open:serious:team:big", "Cover me, {sib}. On three.", "Formation. Now.");
        put("break:touka:squabble:any", "HEY. Both of you. Enough. I don't care who started it.", "You two. Separate corners. Now. Don't make me count.", "If I hear one more word, nobody gets dinner. ...I'm serious.", "*grabs both by the collar* Do I need to make you hug? Because I will.");
        put("break:ken:squabble:any", "Hey, hey. Stop. Let's talk about this calmly.", "You're family. You don't fight like this. Say sorry, both of you.", "I'm not angry. I'm disappointed. ...Okay, a little angry. Apologise.", "Sit down, both of you. We'll fix this together.");
        put("break:eto:squabble:any", "Ooh, drama! Delicious. Now stop it before I write you both into a tragedy.", "Children. Children. If anyone's going to cause chaos here, it's me.", "Apologise to each other or I'm reading my new manuscript aloud. All six hundred pages.", "*pulls them apart by the ears* Ah ah ah. Sibling love. Remember it.");
        put("sulk:sunny:squabble:any", "...sorry, {sib}. Hug?", "Okay, okay. Sorry.");
        put("sulk:shy:squabble:any", "*mumbles* ...sorry.", "...I didn't mean it.");
        put("sulk:bold:squabble:any", "Fine. Sorry. ...They started it.", "*crosses arms* ...sorry.");
        put("sulk:curious:squabble:any", "Technically I apologise.", "...Sorry. I was wrong. Partially.");
        put("sulk:mischief:squabble:any", "Sorryyy. *sticks out tongue when nobody's looking*", "Okay. Truce. For now.");
        put("sulk:gentle:squabble:any", "I'm sorry, {sib}. I love you.", "*hugs {sib}* Sorry.");
        put("sulk:serious:squabble:any", "I apologise. It won't happen again.", "...Sorry. That was childish of me.");
    }
}
