package com.hex.kagune.control.family;

import java.util.HashMap;
import java.util.Map;

/**
 * 3.4.0 (pure, tested; the owner's list after 3.2.1: "siblings at home reacting to the one brought back"): a child brought home
 * ({@link Homecoming}) and its brothers and sisters at home, who were not out with it. One that is older and finds it went off on its own
 * covers for it, or wants to know where it WAS; a younger one tells on it, or was worried; back from an outing without them, an older one
 * minds being left behind and a younger one wants to go next time. The one brought back answers, by its own temper. A baby says nothing
 * yet, and nobody has words for a baby carried in from a walk. The world's side is {@code HomecomingServer}.
 */
public final class SiblingWelcome {
    private SiblingWelcome() {
    }

    /** What the one at home does about it (never sent: order free). */
    public enum Act {
        WHERE, COVER, TATTLE, WITHOUT, TAKEME
    }

    /** How near a brother or sister must be to the one brought in to react (blocks); how many of them do, at most. */
    public static final double NEAR = 16.0;
    public static final int MOST = 2;
    /** When the first of them speaks (ticks after it came in: after its call-out to the parent at the door), and the gap between lines. */
    public static final long FIRST = 50L;
    public static final long GAP = 45L;

    /**
     * What a brother or sister of temper {@code temper} at home does about the one brought back ({@code found}: it went off on its own;
     * {@code older}: the one at home is the older of the two). Null if either is a baby ({@code speaker}, {@code brought}).
     */
    public static Act act(boolean found, boolean older, ChildTemper temper, ChildStage speaker, ChildStage brought) {
        if (speaker == ChildStage.BABY || brought == ChildStage.BABY) {
            return null;
        }
        ChildTemper t = temper == null ? ChildTemper.SUNNY : temper;
        if (!found) {
            return older ? Act.WITHOUT : Act.TAKEME;
        }
        if (older) {
            return t == ChildTemper.GENTLE || t == ChildTemper.MISCHIEF || t == ChildTemper.BOLD ? Act.COVER : Act.WHERE;
        }
        return t == ChildTemper.GENTLE || t == ChildTemper.SHY || t == ChildTemper.CURIOUS ? Act.WHERE : Act.TATTLE;
    }

    /** What the one at home says ({@code name}: the one brought back), by its temper and age; one by {@code seed}. */
    public static String open(Act act, ChildTemper temper, ChildStage stage, String name, int seed) {
        return line("open", act, temper, stage, name, seed);
    }

    /** What the one brought back says to it ({@code name}: the one at home), by its own temper and age. */
    public static String reply(Act act, ChildTemper temper, ChildStage stage, String name, int seed) {
        return line("reply", act, temper, stage, name, seed);
    }

    private static String line(String part, Act act, ChildTemper temper, ChildStage stage, String name, int seed) {
        String who = stage == ChildStage.TODDLER ? "little" : (temper == null ? ChildTemper.SUNNY : temper).word();
        String[] lines = LINES.get(key(part, act, who));
        String text = lines == null || lines.length == 0 ? "..." : lines[Math.floorMod(seed, lines.length)];
        return text.replace("{name}", name == null || name.isEmpty() ? "you" : name);
    }

    static String key(String part, Act act, String who) {
        return part + ":" + act.name().toLowerCase(java.util.Locale.ROOT) + ":" + who;
    }

    /** Every key ("open|reply:act:temper|little"), for the tests. */
    static java.util.Set<String> keys() {
        return java.util.Collections.unmodifiableSet(LINES.keySet());
    }

    private static final Map<String, String[]> LINES = new HashMap<>();

    private static void put(String part, Act act, String who, String... lines) {
        LINES.put(key(part, act, who), lines);
    }

    static {
        // ---- WHERE: "where WERE you?" (an older one who wants to know; a younger one who was worried)
        put("open", Act.WHERE, "sunny", "{name}! Where WERE you? I looked behind every tree!", "There you are! I was about to make a search party. Of me.");
        put("open", Act.WHERE, "shy", "*runs up and grabs {name}'s sleeve* ...Where were you? I was scared.", "*very quietly* {name}... you were gone so long.");
        put("open", Act.WHERE, "bold", "Where WERE you? You can't just go off without telling me!", "{name}! You're dead. You're so dead. Where were you?!");
        put("open", Act.WHERE, "curious", "Where did you go? How far? What did you see? Was it scary?", "{name}! Where were you? Did you find anything?");
        put("open", Act.WHERE, "mischief", "Where WERE you? ...And why didn't you take me?", "{name}! You went on an adventure without me? Rude.");
        put("open", Act.WHERE, "gentle", "*hugs {name} tight* Where were you? Are you hurt? I was so worried.", "{name}, you're okay! Where did you go? I kept watching for you.");
        put("open", Act.WHERE, "serious", "Where were you? Nobody knew. That's the rule. Somebody always knows.", "{name}. Where were you? Explain. From the beginning.");
        put("open", Act.WHERE, "little", "{name}! Where you GO?", "*runs at {name}* You gone! Where?", "{name} back! Where was you?");
        // ---- COVER: the big one covering for it
        put("open", Act.COVER, "gentle", "It wasn't {name}'s fault. I should've been watching. Don't be mad at them.", "Please don't be cross with {name}. They didn't mean to go so far.");
        put("open", Act.COVER, "mischief", "Okay, so, technically I said {name} could go. So technically it's my fault. Technically.", "{name} was on a secret mission. For me. Very important. Don't ask.");
        put("open", Act.COVER, "bold", "Leave {name} alone. They were with me. Sort of. Near me. Close enough.", "If anyone's in trouble, it's me. I'm older. I should've stopped them.");
        put("open", Act.COVER, "little", "*stands in front of {name}* No! Not {name} fault!", "{name} GOOD! {name} good!");
        // ---- TATTLE: the little one telling on it
        put("open", Act.TATTLE, "sunny", "{name} went out ALL BY THEMSELVES! I saw! I'm telling! ...I told!", "Guess who wandered off? {name}! I knew it! I knew it!");
        put("open", Act.TATTLE, "bold", "{name} went past the fence! I SAW! That's not allowed!", "{name}'s in trouble, {name}'s in trouble!");
        put("open", Act.TATTLE, "mischief", "Ooooh. Somebody's in trouuuuble. *points at {name}*", "I'm not saying {name} went out alone. I'm just saying I saw it. With my eyes.");
        put("open", Act.TATTLE, "serious", "{name} went out without asking. I'm only saying because it's true.", "For the record, I told {name} not to go. For the record.");
        put("open", Act.TATTLE, "little", "{name} went OUT! By SELF! Bad!", "*points at {name}* They goed away! I saw!", "Uh-oh. {name} in TROUBLE.");
        // ---- WITHOUT: back from an outing without them (an older one)
        put("open", Act.WITHOUT, "sunny", "You two went out without me? Where'd you go? Was it fun? Next time I'm coming!", "{name}! You're back! Tell me everything, every bit!");
        put("open", Act.WITHOUT, "shy", "*peeks out* ...You went somewhere? I'd have come. If you'd asked.", "*small voice* Was it nice, {name}? Out there?");
        put("open", Act.WITHOUT, "bold", "Hey! You went out and didn't take me? I'm older! I should've been in charge!", "Oh, so {name} gets an adventure and I get chores. Great.");
        put("open", Act.WITHOUT, "curious", "Where'd you go? Did you see the old tower? Did you find any bones?", "{name}, what did you find? Show me. Show me right now.");
        put("open", Act.WITHOUT, "mischief", "So I see we're leaving people behind now. I'll remember this. I'll remember this forever.", "You went out without me? Cool. Cool cool cool. Your bed's full of frogs now.");
        put("open", Act.WITHOUT, "gentle", "Welcome back, {name}! Did you have a nice time? I kept your dinner warm.", "{name}! I missed you. Was it fun? I want to hear about it.");
        put("open", Act.WITHOUT, "serious", "You were gone four hours. I counted. Was the outing a success, at least?", "Welcome back. You're late. I'm not upset. I'm noting it.");
        put("open", Act.WITHOUT, "little", "You goed without me!", "*stamps* Not FAIR! Me too!");
        // ---- TAKEME: a younger one, left behind
        put("open", Act.TAKEME, "sunny", "{name}! You're back! Next time take me! PLEASE! I'll be so good!", "Where'd you go? Can I come next time? Can I can I can I?");
        put("open", Act.TAKEME, "shy", "*tugs {name}'s hand* ...Next time... can I come too?", "*whispers* I waited for you, {name}.");
        put("open", Act.TAKEME, "bold", "No fair! I wanted to go! Next time I'm going FIRST!", "{name} got to go and I didn't! That's not fair!");
        put("open", Act.TAKEME, "curious", "What was out there? Was it big? Bigger than here? Take me next time!", "{name}, did you see a fox? I want to see a fox!");
        put("open", Act.TAKEME, "mischief", "You went without me? Okay. Okay. I'm hiding your shoes. Both of them.", "Next time you sneak out without me, I'm telling. ...Unless you take me.");
        put("open", Act.TAKEME, "gentle", "{name}! I'm glad you're back. I made you a picture while you were gone.", "Was it fun, {name}? I'm happy you had fun.");
        put("open", Act.TAKEME, "serious", "I would like to go next time. I'll pack my own bag. I'll be ready.", "Next time, I'd like to be considered for the outing. Formally.");
        put("open", Act.TAKEME, "little", "Me too! Me too next time!", "*grabs {name}* You goed without me!", "{name} back! I come next time!");

        // ---- the one brought back, answering
        put("reply", Act.WHERE, "sunny", "I'm fine! I'm totally fine! I found a whole field of flowers, {name}!", "Sorry! I didn't mean to be gone so long. But it was so pretty out there!");
        put("reply", Act.WHERE, "shy", "*hides behind the parent* ...Sorry, {name}.", "*very small* ...I got lost. I'm sorry.");
        put("reply", Act.WHERE, "bold", "I wasn't lost! I knew exactly where I was. Mostly.", "Relax, {name}. I had it handled.");
        put("reply", Act.WHERE, "curious", "I followed a beetle, and then a bird, and then I didn't know where I was. But {name}, the BIRD.", "There's a cave past the hill, {name}! A real one! ...I'm grounded, though.");
        put("reply", Act.WHERE, "mischief", "Where was I? Where were YOU, {name}? Ever think about that?", "*grins* Exploring. You'd have loved it. You're not invited next time.");
        put("reply", Act.WHERE, "gentle", "I'm sorry I scared you, {name}. I won't do it again.", "*hugs {name} back* I'm okay. I'm sorry. I'm here.");
        put("reply", Act.WHERE, "serious", "I made a mistake, {name}. I already apologised. I'll explain later.", "I lost track of time. It won't happen again. I've made a plan.");
        put("reply", Act.WHERE, "little", "Was LOST! Now found!", "*sniff* ...Sorry, {name}.", "Went far! Was big out there!");
        put("reply", Act.COVER, "sunny", "Thanks, {name}! You're the best!", "*whispers* I owe you one, {name}. A big one.");
        put("reply", Act.COVER, "shy", "*looks at {name}, eyes big* ...Thank you.", "*hides behind {name} instead*");
        put("reply", Act.COVER, "bold", "I don't need you covering for me, {name}. ...But thanks.", "See? {name} gets it.");
        put("reply", Act.COVER, "curious", "Wait, did you say you told me to go? You didn't. ...Oh. OH. Yes. They told me to.", "*mouths 'thank you' at {name}*");
        put("reply", Act.COVER, "mischief", "*nods very seriously* Yes. It was all {name}'s idea. Every bit of it.", "*winks at {name}* Exactly. Secret mission. Very hush-hush.");
        put("reply", Act.COVER, "gentle", "No, it's not {name}'s fault. It's mine. Don't be mad at them.", "{name}, you don't have to take the blame for me. ...Thank you, though.");
        put("reply", Act.COVER, "serious", "That's not true, {name}. I went on my own. I'll take the consequences.", "I appreciate it, {name}, but lying makes it worse. It was me.");
        put("reply", Act.COVER, "little", "Yeah! {name} said!", "*hugs {name}'s leg*", "*nods and nods*");
        put("reply", Act.TATTLE, "sunny", "{name}! You weren't supposed to tell! ...Okay, yeah, I did.", "Tattle-tale! ...But yeah. I did. Sorry.");
        put("reply", Act.TATTLE, "shy", "*goes red and stares at the floor*", "*whispers* ...{name}, that's mean.");
        put("reply", Act.TATTLE, "bold", "Snitch! ...Yeah, I went. So what? I came back.", "{name}, you're SO dead later.");
        put("reply", Act.TATTLE, "curious", "Technically I didn't go past the fence. I went round it. It's different.", "How did you even see me, {name}? Were you following me?");
        put("reply", Act.TATTLE, "mischief", "Lies. Slander. {name} is a known liar. Don't listen.", "*sticks out tongue at {name}*");
        put("reply", Act.TATTLE, "gentle", "It's true. I did. Sorry, everyone. Sorry, {name}.", "{name}'s right. I shouldn't have gone.");
        put("reply", Act.TATTLE, "serious", "Yes. That's accurate. Thank you, {name}. I'll take it from here.", "{name} is correct. I'm prepared to discuss it.");
        put("reply", Act.TATTLE, "little", "Was NOT! ...Was a little.", "{name} MEAN!", "*cries a bit*");
        put("reply", Act.WITHOUT, "sunny", "It was AMAZING, {name}! We saw a deer! Next time you're coming!", "{name}, you have to come next time! I'll show you everything!");
        put("reply", Act.WITHOUT, "shy", "*holds out a little stone* ...I brought you this, {name}.", "*quietly* It was nice. You can come next time.");
        put("reply", Act.WITHOUT, "bold", "I led the whole way, {name}. You'd have slowed us down.", "Jealous? You should be. It was great.");
        put("reply", Act.WITHOUT, "curious", "No tower. But we found a skull! A real one! Probably a cow.", "{name}! I've got so much to tell you. Sit down. This'll take a while.");
        put("reply", Act.WITHOUT, "mischief", "If there's frogs in my bed, {name}, there'll be frogs in YOURS.", "*grins* I'd have taken you. But you'd have told.");
        put("reply", Act.WITHOUT, "gentle", "Thank you for keeping my dinner, {name}! I brought you a flower.", "I missed you too, {name}. Next time, come with us.");
        put("reply", Act.WITHOUT, "serious", "It was a success. I'll write you a report, {name}.", "We were late because of the river. I'll explain the river.");
        put("reply", Act.WITHOUT, "little", "Was FUN! You come next time!", "*shows {name} a muddy stick* Look!", "Saw a BIRD, {name}!");
        put("reply", Act.TAKEME, "sunny", "Next time, {name}! Promise! You'll love it!", "I'll take you next time, {name}! We'll go even further!");
        put("reply", Act.TAKEME, "shy", "*nods* ...Next time. Together.", "*takes {name}'s hand* ...Okay.");
        put("reply", Act.TAKEME, "bold", "When you're bigger, {name}. Maybe.", "You'd get tired. ...Fine. Next time.");
        put("reply", Act.TAKEME, "curious", "There was a fox, {name}! I'll show you where it lives!", "Bigger! Way bigger! I'll draw you a map.");
        put("reply", Act.TAKEME, "mischief", "Hide my shoes and you're never coming. ...Okay, you can come.", "Shh. Next time we'll sneak out together.");
        put("reply", Act.TAKEME, "gentle", "You made me a picture? {name}, it's beautiful. You're coming next time.", "*hugs {name}* I wish you'd been there.");
        put("reply", Act.TAKEME, "serious", "Your application is accepted, {name}. Pack light.", "Next time. I'll make sure you're on the list.");
        put("reply", Act.TAKEME, "little", "You come! Next time!", "*hugs {name}*", "Okay! Together!");
    }
}
