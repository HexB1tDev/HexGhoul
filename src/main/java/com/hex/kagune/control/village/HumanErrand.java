package com.hex.kagune.control.village;

import com.hex.kagune.control.village.HumanPersona.Temper;

/**
 * 2.4.7 (pure, tested; the owner: "Add even more lines, mini random quests to every human NPC. (Randomly triggered through the alert boxes
 * AND rare, boost relationship + rewards"): a villager's favour, now and then, asked of a player near it (a banner tells them): fetch it
 * something ({@link Kind#FETCH}: held out to it), deal with what prowls by its house at night ({@link Kind#HUNT}: monsters, any of them),
 * or look in on a neighbour for it ({@link Kind#VISIT}: talk to them). Done, it pays in emeralds and something of its trade, and thinks
 * better of them ({@link #REGARD}). Rare: a player is asked at most once every {@link #OFFER_EVERY} ticks, one favour at a time, and it
 * lapses after {@link #LASTS}.
 */
public final class HumanErrand {
    private HumanErrand() {
    }

    public enum Kind {
        FETCH, HUNT, VISIT
    }

    /** A favour: what, the item's id and name and how many (FETCH), how many to deal with (HUNT), the neighbour (VISIT), its pay. */
    public record Errand(Kind kind, String item, String name, int count, String other, int emeralds) {
        /** What the banner says it wants. */
        public String summary(String giver) {
            return switch (this.kind) {
                case FETCH -> "needs a favour: bring " + this.count + " " + this.name + ". (" + this.emeralds + " emeralds)";
                case HUNT -> "needs a favour: deal with " + this.count + " monsters near their home at night. (" + this.emeralds + " emeralds)";
                case VISIT -> "needs a favour: go and see " + this.other + " for them. (" + this.emeralds + " emeralds)";
            };
        }
    }

    /** How often a player may be asked at most (ticks: ten minutes), how long a favour stays open (an hour), its regard. */
    public static final long OFFER_EVERY = 12000L;
    public static final long LASTS = 72000L;
    public static final int REGARD = 10;
    /** The chance a villager near asks, each time it thinks of it (once every ten seconds, near a player it sees). */
    public static final double CHANCE = 0.02;

    /** What villagers ask to be fetched: {id, name, how many}. Everyday things, a little of each. */
    static final String[][] FETCH = {{"minecraft:wheat", "wheat", "8"}, {"minecraft:egg", "eggs", "4"}, {"minecraft:sugar_cane", "sugar cane", "6"},
        {"minecraft:coal", "coal", "6"}, {"minecraft:iron_ingot", "iron ingots", "2"}, {"minecraft:oak_log", "oak logs", "8"},
        {"minecraft:white_wool", "white wool", "3"}, {"minecraft:string", "string", "4"}, {"minecraft:apple", "apples", "4"},
        {"minecraft:carrot", "carrots", "6"}, {"minecraft:potato", "potatoes", "6"}, {"minecraft:leather", "leather", "3"},
        {"minecraft:paper", "paper", "6"}, {"minecraft:bone_meal", "bone meal", "6"}, {"minecraft:poppy", "poppies", "3"},
        {"minecraft:cobblestone", "cobblestone", "16"}, {"minecraft:glass_bottle", "glass bottles", "3"}, {"minecraft:feather", "feathers", "4"},
        {"minecraft:flint", "flint", "3"}, {"minecraft:torch", "torches", "8"}, {"minecraft:bread", "bread", "3"}, {"minecraft:milk_bucket", "a bucket of milk", "1"}};

    /** The favour {@code roll} gives ({@code neighbour}: one near to look in on, or null). */
    public static Errand roll(long roll, String neighbour) {
        int pick = (int)Math.floorMod(roll, 100L);
        long r = roll >>> 7;
        if (pick < 20 && neighbour != null) {
            return new Errand(Kind.VISIT, "", "", 1, neighbour, 2);
        }
        if (pick < 50) {
            int count = 3 + (int)Math.floorMod(r, 3L);
            return new Errand(Kind.HUNT, "", "", count, "", 3 + count / 2 + 1);
        }
        String[] want = FETCH[(int)Math.floorMod(r, (long)FETCH.length)];
        return new Errand(Kind.FETCH, want[0], want[1], Integer.parseInt(want[2]), "", 2 + (int)Math.floorMod(r >>> 5, 3L));
    }

    // ---------------------------------------------------------------- saved with the player: one line

    /** {@code errand} for {@code giver} (its UUID and name) until {@code until}, {@code done} of it, as one line. */
    public static String save(String giver, String giverName, Errand errand, int done, long until) {
        return String.join("|", giver, giverName.replace("|", " "), errand.kind().name(), errand.item(), errand.name().replace("|", " "),
            Integer.toString(errand.count()), errand.other().replace("|", " "), Integer.toString(errand.emeralds()), Integer.toString(done),
            Long.toString(until));
    }

    /** A saved favour: its giver, the giver's name, the favour, how much is done, until when; null for none or a bad line. */
    public record Open(String giver, String giverName, Errand errand, int done, long until) {
        public boolean finished() {
            return this.errand.kind() != Kind.FETCH && this.done >= this.errand.count();
        }
    }

    public static Open load(String line) {
        if (line == null || line.isEmpty()) {
            return null;
        }
        String[] p = line.split("\\|", -1);
        if (p.length < 10) {
            return null;
        }
        try {
            Errand errand = new Errand(Kind.valueOf(p[2]), p[3], p[4], Integer.parseInt(p[5]), p[6], Integer.parseInt(p[7]));
            return new Open(p[0], p[1], errand, Integer.parseInt(p[8]), Long.parseLong(p[9]));
        } catch (RuntimeException bad) {
            return null;
        }
    }

    // ---------------------------------------------------------------- their words, by temper

    /** What {@code temper} says asking it. */
    public static String ask(Temper temper, Errand errand, int seed) {
        String what = switch (errand.kind()) {
            case FETCH -> errand.count() + " " + errand.name();
            case HUNT -> "the monsters prowling by my house at night (" + errand.count() + " of them)";
            case VISIT -> errand.other();
        };
        String[] lines = switch (errand.kind()) {
            case FETCH -> FETCH_ASK[temper.ordinal()];
            case HUNT -> HUNT_ASK[temper.ordinal()];
            case VISIT -> VISIT_ASK[temper.ordinal()];
        };
        return lines[Math.floorMod(seed, lines.length)].replace("{what}", what);
    }

    /** What {@code temper} says when it's done (and paid). */
    public static String thanks(Temper temper, int seed) {
        String[] lines = THANKS[temper.ordinal()];
        return lines[Math.floorMod(seed, lines.length)];
    }

    /** What a neighbour says, looked in on for {@code sender}. */
    public static String visited(String sender, int seed) {
        String[] lines = {"{sender} sent you? ...That's sweet of them. Tell them I'm fine. Better now.",
            "Oh! {sender} worries too much. ...But I'm glad someone came. Thank you.",
            "Tell {sender} I've still got their pie dish. And that I miss our talks.", "{sender}? Ha. Tell them the roof's fixed and I'm alive. Mostly.",
            "You came all this way for {sender}? Sit a minute. ...Alright, go on, tell them I said hello."};
        return lines[Math.floorMod(seed, lines.length)].replace("{sender}", sender);
    }

    /** What {@code temper} says when they give it up. */
    public static String dropped(Temper temper, int seed) {
        String[] lines = DROPPED[temper.ordinal()];
        return lines[Math.floorMod(seed, lines.length)];
    }

    // Each by temper, in Temper's order: cheerful, shy, bold, kind, grumpy, flirty, dreamy, nervous, playful, stoic, sarcastic, romantic.

    static final String[][] FETCH_ASK = {
        {"Oh! Perfect timing! Could you bring me {what}? I'd pay you, promise!", "Hiii, favour time! {what}, please? You're the best!"},
        {"Um... sorry to ask... could you maybe bring me {what}? Only if it's no trouble.", "I-I need {what}... would you...? Sorry."},
        {"You. I need {what}. I'll make it worth your while.", "Bring me {what} and I'll pay well. Deal?"},
        {"Would you be a dear and fetch me {what}? I'll see you right for it.", "I hate to ask, but I'm short of {what}. Could you help an old friend?"},
        {"Don't just stand there. Make yourself useful: {what}. I'll pay. Grudgingly.", "I need {what}. And no, I won't say please. ...Please."},
        {"Could you get me {what}? I'll make it up to you. Any way you like.", "A little favour for a pretty face: {what}? I'll reward you."},
        {"I dreamed you'd bring me {what}. Silly, isn't it? ...Would you?", "I keep forgetting to get {what}. Could you? The stars would thank you."},
        {"P-please, I need {what} and I can't go out there alone. Could you?", "I'm sorry, I'm sorry, but could you bring me {what}? I'll pay!"},
        {"Quest time! Your mission: bring me {what}. Reward: glory! And emeralds.", "Bet you can't bring me {what} before sundown. ...Prove me wrong!"},
        {"{what}. If you can. I'll pay.", "I need {what}. You're capable. I'll see you paid."},
        {"Oh good, a hero. Fetch me {what}, would you? Very heroic.", "I'd get {what} myself, but then what would you do all day?"},
        {"Bring me {what}, and I'll remember it like a love letter.", "Would you fetch me {what}? Every gift from you means something."}};

    static final String[][] HUNT_ASK = {
        {"There's something by my house at night. Scary! Could you chase off {what}?", "Monsters again! Could you deal with {what}? I'll bake you something!"},
        {"Th-there are things outside my window at night... {what}... could you...?", "I can't sleep. There's {what}. ...Could you make them go?"},
        {"Something's sniffing round my door at night. {what}. Kill them. I'll pay.", "I'd do it myself if I had your arms. {what}. Go."},
        {"Dear, would you see off {what}? I worry for the little ones.", "There's {what} at night now. Would you? I'll feel safer."},
        {"Those things by my house at night are driving me mad. {what}. Deal with it.", "{what}. Every night. Make it stop and I'll stop complaining. Mostly."},
        {"Protect me from {what}, and I'll be very... grateful.", "A hero's job: {what}. Do it and come back to me."},
        {"Shadows by my house at night... {what}. Would you chase the shadows away?", "Something sings at my window at night, and it isn't a bird. {what}."},
        {"P-please! {what}! Every night! I haven't slept in days!", "They scratch at the door. {what}. Please help me."},
        {"Monster hunt! Get {what}, and I'll owe you one!", "Race you! ...No, you go. {what}. I'll cheer."},
        {"{what} by my house. Deal with them. You'll be paid.", "There's {what}. Handle it."},
        {"There's {what} outside. Perfect job for someone who loves fighting things.", "Free monsters, delivered nightly. {what}. Help yourself."},
        {"Be my knight? {what} stalk my house at night.", "Save me from {what}, like a story."}};

    static final String[][] VISIT_ASK = {
        {"Could you check on {what} for me? I haven't seen them in ages!", "Go say hi to {what} for me! They'll love it!"},
        {"Could you... look in on {what}? I'm too shy to go myself.", "Um, is {what} alright? Would you see? Sorry."},
        {"Go see {what}. Tell them I said hello. And come back.", "{what}'s been quiet. Check on them for me."},
        {"Would you look in on {what}, dear? I worry about them.", "Go see how {what} is doing, would you? It would mean a lot."},
        {"Go see {what}. They owe me a chat and I'm not walking over there.", "{what}. Go see if they're alive. Not that I care."},
        {"Pay {what} a visit for me? Tell them I said something charming.", "Visit {what} and give them my love. Not all of it."},
        {"I dreamed {what} was sad. Would you go see them?", "Go to {what}. Tell them the sky's nice today."},
        {"Is {what} okay? I keep worrying. Could you go and see?", "Could you check on {what}? I can't stop thinking something's wrong."},
        {"Secret mission: go see {what} and report back!", "Go poke {what} for me. Gently. Then come back."},
        {"Look in on {what}. Tell them I asked.", "{what}. Check on them."},
        {"Go see {what}. Someone has to, and it won't be me.", "Visit {what}. Bring back gossip. That's the real payment."},
        {"Take my regards to {what}. Wrap them up nicely.", "Visit {what} for me. Hearts are lighter shared."}};

    static final String[][] THANKS = {
        {"You did it! Here, take this, and this, and a hug if you want one!", "Thank you thank you thank you! You're my favourite person!"},
        {"Th-thank you. Really. Here... for you.", "...You actually did it. Thank you. I mean it."},
        {"Good work. Here's your pay. You've earned it.", "Done already? I like you. Take this."},
        {"Bless you, dear. Here, take this, and come by for supper sometime.", "You're a treasure. Here, for your trouble."},
        {"...Fine. You did good. Take it before I change my mind.", "Huh. Done. ...Thanks. Don't let it go to your head."},
        {"My hero. Here's your reward. ...Some of it.", "Mm, you came back for me. Here, take this. And a wink."},
        {"You came back. Like a star that keeps its promise. Here.", "Thank you. I'll write it down somewhere so I don't forget."},
        {"Oh thank goodness! Thank you! Here, please, take it!", "I can breathe again. Thank you so much. Here."},
        {"Mission complete! Here's your loot!", "You did it! Level up! Here's your prize!"},
        {"Done. Thank you. Here.", "Good. Take this. You've earned it."},
        {"Look at that, a real hero. Here's your shiny reward.", "Wow, you actually did it. I'm almost impressed. Take this."},
        {"You did it for me. I'll keep this moment. And you keep these.", "Thank you, truly. Here, with all my heart."}};

    static final String[][] DROPPED = {
        {"Aw, that's okay! Maybe next time!", "No worries! I'll figure it out!"}, {"Oh... okay. It's fine. Really.", "...That's alright. Sorry for asking."},
        {"Fine. I'll find someone else.", "Your loss."}, {"That's alright, dear. Don't worry about it.", "Never mind it. Look after yourself."},
        {"Figures.", "Hmph. Should've known."}, {"Breaking my heart already?", "Oh well. I'll ask someone less cute."},
        {"The moment's passed, like a cloud.", "Never mind. It was a silly wish."}, {"O-okay. I'll manage. Somehow.", "It's fine. It's fine. It's fine."},
        {"Quest abandoned! Boo!", "Aww. Game over, then."}, {"Understood.", "Alright."},
        {"Shocking. Truly.", "Wow, okay. I'll just suffer, then."}, {"Some stories end before they begin.", "It's alright. I'll wait for another day."}};
}
