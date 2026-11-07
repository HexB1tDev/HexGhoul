package com.hex.kagune.control.family;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 3.4.1 (pure, tested; the owner's list after 3.2.1: "the companion and the child talking to each other at the door after the player's
 * scene"): once the player's door scene with its companion parent is over ({@link Homecoming}), the two of them have a word of their own:
 * the parent starts it (Eto, Touka, Ken, in their voices; found, or back from an outing; to a toddler or a bigger child), the child answers
 * by its temper, the parent answers that, and the child has the last word. To a baby the parent talks alone. The world's side is
 * {@code HomecomingServer} (a {@code Conversation}).
 */
public final class DoorTalk {
    private DoorTalk() {
    }

    /** How long after the homecoming the talk may start at the soonest (ticks), and how long it waits for them to be free before it is let go. */
    public static final long AFTER = 100L;
    public static final long WAITS = 1200L;

    /** The child's temper as the parent hears it: soft (shy, gentle), wild (bold, mischief), bright (sunny, curious), steady (serious). */
    static String group(ChildTemper temper) {
        return switch (temper == null ? ChildTemper.SUNNY : temper) {
            case SHY, GENTLE -> "soft";
            case BOLD, MISCHIEF -> "wild";
            case SUNNY, CURIOUS -> "bright";
            case SERIOUS -> "steady";
        };
    }

    /**
     * The talk: lines of who says it (1 the parent {@code persona}, 2 the child) and what ({@code kid}: the child's name; {@code call}: what
     * it calls the parent), one of each by {@code seed}.
     */
    public static List<Siblings.Line> talk(String persona, boolean found, ChildTemper temper, ChildStage stage, String kid, String call, int seed) {
        String p = LINES.containsKey(key("open", persona, found, "big")) ? persona : "touka";
        String f = found ? "found" : "outing";
        boolean little = stage == ChildStage.TODDLER;
        String own = little ? "little" : (temper == null ? ChildTemper.SUNNY : temper).word();
        List<Siblings.Line> out = new ArrayList<>();
        if (stage == ChildStage.BABY) {
            out.add(line(1, "baby:" + p + ":1", kid, call, seed));
            out.add(line(1, "baby:" + p + ":2", kid, call, seed + 1));
            return out;
        }
        out.add(line(1, key("open", p, found, little ? "little" : "big"), kid, call, seed));
        out.add(line(2, "answer:" + f + ":" + own, kid, call, seed + 1));
        out.add(line(1, key("back", p, found, little ? "little" : group(temper)), kid, call, seed + 2));
        out.add(line(2, "last:" + f + ":" + own, kid, call, seed + 3));
        return out;
    }

    static String key(String part, String persona, boolean found, String who) {
        return part + ":" + persona + ":" + (found ? "found" : "outing") + ":" + who;
    }

    private static Siblings.Line line(int who, String key, String kid, String call, int seed) {
        String[] lines = LINES.get(key);
        String text = lines == null || lines.length == 0 ? "..." : lines[Math.floorMod(seed, lines.length)];
        return new Siblings.Line(who, text.replace("{kid}", kid == null || kid.isEmpty() ? "little one" : kid).replace("{call}",
            call == null || call.isEmpty() ? "Mama" : call));
    }

    /** Every key, for the tests. */
    static java.util.Set<String> keys() {
        return java.util.Collections.unmodifiableSet(LINES.keySet());
    }

    private static final Map<String, String[]> LINES = new HashMap<>();

    private static void put(String key, String... lines) {
        LINES.put(key, lines);
    }

    static {
        // ================================================================ Eto
        put("open:eto:found:big", "So, {kid}. A little adventure. Tell me the plot. Don't skip the part where you got lost.",
            "Come here, {kid}. Sit. I'd like to hear your side of the story before I write my own.");
        put("open:eto:found:little", "{kid}. *crouches* You went on a walk all by yourself. Was it a good walk?", "Little wanderer. *taps {kid}'s nose* Where did your feet take you?");
        put("open:eto:outing:big", "Well? Was it a good day, {kid}? I want every detail. Especially the ones you'd rather leave out.",
            "Tell me about it, {kid}. Leave out the boring parts. Actually, no. Tell me those too.");
        put("open:eto:outing:little", "*scoops up {kid}* Did you see the world, little one? Is it still there?", "{kid}! Tell me everything. Use your words. All six of them.");
        put("back:eto:found:soft", "Oh, darling. It's alright to be frightened. I was too. Next time, you come and find me first, and we'll be frightened together.",
            "Shh. You're home. The scary part is over. I'll keep the rest of it far away from you.");
        put("back:eto:found:wild", "Mm. A rebel. I approve, in principle. In practice, if you vanish again I'll put a bell on you. A loud one.",
            "Brave and foolish. My favourite combination. Just keep the foolish to the garden, hm?");
        put("back:eto:found:bright", "You do find the most wonderful things. Next time, find them with me. I want to see your face when you do.",
            "Curiosity's a good thing, {kid}. It's what makes a reader. Just... read the map first.");
        put("back:eto:found:steady", "That was a very good account. Clear, honest. You'd make a fine witness. A better one if you'd stayed home.",
            "Thank you for telling me straight. I'll remember that. And you'll remember to tell me before you go.");
        put("back:eto:found:little", "*hugs {kid} tight* Next time, hold my hand. Okay? Big hand, little hand.", "You're home now. Home is where I am. Remember that.");
        put("back:eto:outing:soft", "*smiles* I'm glad you had someone to hold onto out there. You can hold onto me now.",
            "Quiet ones see the most, you know. You'll have to tell me the rest later, when it's just us.");
        put("back:eto:outing:wild", "A goose. You fought a goose. I'm so proud I could weep.", "Next time take me. I'll bring a stick. For the goose.");
        put("back:eto:outing:bright", "Write it down before you forget. All of it. Stories leak out of your head if you leave them.",
            "That's a whole chapter, {kid}. Maybe two. I'm keeping it.");
        put("back:eto:outing:steady", "Very thorough. I'll file it with the others. Did you have fun, though? That goes in the report too.",
            "Thank you. Now, off the record: what was the best part?");
        put("back:eto:outing:little", "*kisses {kid}'s muddy forehead* Bath. Then you tell me again.", "Did you? Clever thing. *spins {kid} round*");
        put("baby:eto:1", "*holds {kid} close* Did you see the sky? Was it big? It's always too big. That's why we have walls.",
            "*bounces {kid}* There you are. You smell like wind. I'll have to write that down.");
        put("baby:eto:2", "*hums something old and quiet until {kid}'s eyes droop*", "Mama missed you. Don't tell anyone. *kisses {kid}'s head*");
        // ================================================================ Touka
        put("open:touka:found:big", "Okay. You and me. Talk. What were you thinking, {kid}?", "*arms crossed* Start talking. And don't tell me you weren't lost.");
        put("open:touka:found:little", "*kneels down* {kid}. Look at me. You can't go out by yourself. You hear me?", "Where did you go, huh? *wipes {kid}'s face with her sleeve*");
        put("open:touka:outing:big", "So? How was it? And don't say \"fine\". I can tell when you're lying. I invented lying.",
            "*hands {kid} a cup* Drink. Then tell me about it.");
        put("open:touka:outing:little", "*picks {kid} up* Look at you. Mud everywhere. Did you have fun?", "Hey, you. *nose to nose* Was it fun out there?");
        put("back:touka:found:soft", "*hugs {kid}, rough* Hey. I'm not mad. I was scared. That's different. ...Okay, a little mad.",
            "It's okay. You're okay. Next time, come get me first. I'll go with you. Anywhere.");
        put("back:touka:found:wild", "Yeah, yeah. You're tough. I know. I was tough too. I still got scared every time. Next time, take me.",
            "Next time you want to run off, you run off with ME. Deal? Shake on it.");
        put("back:touka:found:bright", "A cave. Of course. *sighs* Okay. We go see it together. With rope. And I go first.",
            "You can't follow every shiny thing, {kid}. ...Okay, you can. With me there.");
        put("back:touka:found:steady", "...Okay. That was a good apology. Better than mine ever were. You're still grounded.",
            "I hear you. And I believe you. Now go wash up. We'll talk rules tomorrow.");
        put("back:touka:found:little", "*holds {kid} tight* No more going out alone. Promise me.", "You scared me. Don't do that. *kisses the top of {kid}'s head*");
        put("back:touka:outing:soft", "*small smile* You went the whole way? Good. That's brave.", "You can tell me the rest later. When it's quiet.");
        put("back:touka:outing:wild", "You climbed WHAT? *rubs her face* ...Okay. Show me tomorrow. I want to see.",
            "Heh. Good. Next time, race me back.");
        put("back:touka:outing:bright", "Okay, okay, breathe. I'll hear all of it. Every single bird.", "You've got the whole world in your pockets. Empty them before dinner.");
        put("back:touka:outing:steady", "Back on time. Good. *ruffles {kid}'s hair* You're allowed to have fun too, you know.",
            "Thanks for the report, captain. Dismissed. Go eat.");
        put("back:touka:outing:little", "*laughs* Bath. Right now. You smell like a pond.", "Good. Next time I come too.");
        put("baby:touka:1", "*takes {kid} and holds them close* Hey, you. Did you see anything good out there?",
            "*sniffs {kid}'s hair* Grass. And sun. ...Good. That's a good smell.");
        put("baby:touka:2", "*sways a little, without thinking about it* ...Missed you. Shut up. You can't even talk.", "*hums, very quietly, so nobody else hears*");
        // ================================================================ Ken
        put("open:ken:found:big", "Hey. Can we talk for a minute, {kid}? I'm not angry. I just want to understand.",
            "Come sit with me, {kid}. Tell me what happened. Take your time.");
        put("open:ken:found:little", "*kneels down* Hey, little one. Where did you go? Were you scared?", "*checks {kid}'s hands, one finger at a time* Are you hurt anywhere?");
        put("open:ken:outing:big", "How was it, {kid}? Did you see anything new? Tell me everything. I've got time.",
            "*puts his book down* I want to hear all about it. Start wherever you like.");
        put("open:ken:outing:little", "*lifts {kid} up* There you are. Did you have a good time?", "Hi, you. *smiles* What did you see?");
        put("back:ken:found:soft", "It's okay to be scared. I'm scared a lot. You just have to come back to people who love you. And you did.",
            "*holds {kid}* You don't have to say sorry anymore. You're here. That's all.");
        put("back:ken:found:wild", "I know you can handle a lot. I just don't want you to have to handle it alone. Take someone next time. Take me.",
            "You're brave. Braver than I was. But brave and alone is a lonely way to be brave.");
        put("back:ken:found:bright", "That sounds incredible. Really. Next time, let's find it together. I'll bring a notebook.",
            "I love that you wonder about things. Just wonder about them with someone nearby, okay?");
        put("back:ken:found:steady", "Thank you for being honest with me. That matters more than anything. We'll make a plan for next time.",
            "You thought about it carefully. I'm proud of that part. The rest we'll fix together.");
        put("back:ken:found:little", "*holds {kid} close* Next time, hold someone's hand. Okay? Always.", "You're safe. I've got you.");
        put("back:ken:outing:soft", "I'm really glad you went. That's a big thing, going somewhere new. I'm proud of you.",
            "*smiles* You can tell me the rest whenever you want. I'll be here.");
        put("back:ken:outing:wild", "The tallest one? *a weak laugh* Okay. Next time I'm coming, and I'm standing underneath.",
            "That sounds like an adventure. I'd have been terrified. You're amazing.");
        put("back:ken:outing:bright", "Let's write it down. All of it. Then you'll have it forever.", "You saw all that? We'll look it up together. I've got a book.");
        put("back:ken:outing:steady", "That was a very good report. And you came back on time. Thank you. Did you enjoy it?",
            "I'll put it in the notebook. Your days are my favourite thing to write down.");
        put("back:ken:outing:little", "*laughs softly* Mud everywhere. Let's get you clean.", "Good. *kisses {kid}'s forehead* Next time, me too.");
        put("baby:ken:1", "*holds {kid} so carefully* Hello. Did you see the clouds? I'll tell you their names.",
            "*rocks {kid} gently* You're back. Good. It was too quiet without you.");
        put("baby:ken:2", "*very softly* I missed you. Isn't that silly? It was only a little while.", "*hums a lullaby, a little off-key, and doesn't mind*");

        // ================================================================ the child, answering (to whichever parent)
        put("answer:found:sunny", "I found a field, {call}! A whole field of flowers! I just wanted to see all of it!", "I'm sorry! I'm fine! It was so pretty out there, {call}!");
        put("answer:found:shy", "*stares at the floor* ...I got lost, {call}. I didn't mean to.", "*very small* ...I was scared.");
        put("answer:found:bold", "I wasn't lost, {call}. I knew where I was. I just didn't know where YOU were.", "I'm not a baby. I can go places.");
        put("answer:found:curious", "I followed a beetle, and then a bird, and then there was a cave, {call}. A real one.", "I just wanted to know what was past the hill. Now I know.");
        put("answer:found:mischief", "Was I lost? Or was everyone ELSE lost? Think about it, {call}.", "*grins* I was playing hide and seek. With the whole forest.");
        put("answer:found:gentle", "I'm so sorry, {call}. I didn't mean to scare you. I brought you this.", "*hugs {call}* I'm sorry. I'm sorry I made you sad.");
        put("answer:found:serious", "I went too far, {call}. I made a mistake. I've already thought about why.", "I lost track of the time. It won't happen again. I'm sorry.");
        put("answer:found:little", "Went far, {call}! Was BIG out there!", "*sniff* ...Lost. Sorry, {call}.");
        put("answer:outing:sunny", "It was the BEST, {call}! We saw a deer and a river and a really big rock!", "We had so much fun! Can we go again tomorrow?");
        put("answer:outing:shy", "*quietly* It was nice, {call}. I held hands the whole time.", "*small smile* ...We saw a bird. It was blue.");
        put("answer:outing:bold", "I led the way home, {call}. Didn't need any help.", "I climbed the biggest tree! Higher than anyone!");
        put("answer:outing:curious", "{call}, why do rivers bend? And why are some rocks warm? And-", "We found bones! Real ones! Probably a sheep. Maybe a dragon.");
        put("answer:outing:mischief", "Nothing happened. Nothing at all, {call}. Don't ask about the river.", "*innocent* We were very good. Mostly.");
        put("answer:outing:gentle", "I brought you something, {call}! *holds out a flower*", "Were you lonely, {call}? I thought about you.");
        put("answer:outing:serious", "Outing complete, {call}. No injuries. Back on time.", "It was educational. Also, I liked it. That's allowed.");
        put("answer:outing:little", "Was FUN, {call}!", "*holds up a muddy stick* Look!");
        put("last:found:sunny", "Okay! Next time we go together!", "*hugs {call}* Okay. Together.");
        put("last:found:shy", "*nods, and holds on* ...Okay.", "*whispers* Okay, {call}.");
        put("last:found:bold", "...Fine. Deal. But I'm still brave.", "*shakes on it, very seriously*");
        put("last:found:curious", "Okay. But we're bringing a lantern. And a jar.", "Can I draw you a map of where I went?");
        put("last:found:mischief", "Deal. *crosses fingers behind back* ...Okay, real deal.", "*sighs dramatically* Fine. No more forest hide and seek.");
        put("last:found:gentle", "I love you, {call}.", "*doesn't let go for a long time*");
        put("last:found:serious", "Understood. I'll leave a note every time.", "Thank you, {call}. I'll do better.");
        put("last:found:little", "*hugs {call}* Okay.", "*nods and nods*");
        put("last:outing:sunny", "Next time, you come too, {call}!", "*runs off to tell someone else*");
        put("last:outing:shy", "*leans on {call}* ...Can I tell you the rest at bedtime?", "*small smile*");
        put("last:outing:bold", "Next time I'm going even further.", "*grins* You'll see.");
        put("last:outing:curious", "Okay! Can I have the book now?", "I'm going to write it ALL down.");
        put("last:outing:mischief", "*whispers* The frog's in my pocket.", "Okay. Bath. ...Do I have to?");
        put("last:outing:gentle", "I'm glad you're here, {call}.", "*hugs {call}*");
        put("last:outing:serious", "Yes, {call}. I enjoyed it. Officially.", "*almost smiles*");
        put("last:outing:little", "*yawns* ...Again tomorrow.", "*giggles*");
    }
}
