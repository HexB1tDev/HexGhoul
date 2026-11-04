package com.hex.kagune.control.village;

import com.hex.kagune.control.village.HumanPersona.Temper;
import java.util.Set;

/**
 * 2.2.6 (pure, tested; the owner: "make it feel like every NPC is unique", and the handoff's next step since 2.1.4: villagers coming up to
 * the player with something to say, by temper and how close they are, partners too): a human who knows a player walks over to them now and
 * then, by itself, for a reason of its own ({@link Why}): to say hello, to pass on news or a neighbour's gossip, to give them something of
 * its trade, worried when they're hurt; a partner also because they missed them, for a kiss, a hug, or to take their hand for a walk. How
 * often ({@link #every}) and why ({@link #pick}) by its temper and how warm it is to them; its words ({@link #line}) its temper's own.
 */
public final class HumanApproach {
    private HumanApproach() {
    }

    /** Why it comes over. */
    public enum Why {
        /** Just to say hello. */
        HELLO,
        /** News it heard (HumanVoice's), its temper's way in. */
        NEWS,
        /** A word about a neighbour (HumanVoice's gossip). */
        GOSSIP,
        /** Something of its trade, a gift (once a day at most). */
        GIFT,
        /** They're hurt (under half their health). */
        WORRIED,
        /** A partner, who hasn't seen them for a while. */
        MISSED,
        /** A partner, for a kiss (it walks up and kisses them). */
        KISS,
        /** A partner, for a hug. */
        HUG,
        /** A partner, who takes their hand for a walk. */
        WALK;

        /** True for a partner's reasons only. */
        public boolean partners() {
            return this == MISSED || this == KISS || this == HUG || this == WALK;
        }
    }

    /** How often (ticks) a partner comes over, before its temper. */
    public static final long PARTNER_EVERY = 3000L;
    /** How often one close to them does ({@link HumanPersona#CLOSE}). */
    public static final long CLOSE_EVERY = 6000L;
    /** How often one warm to them does ({@link HumanPersona#WARM}). */
    public static final long WARM_EVERY = 12000L;
    /** How long (ticks) after one comes over to a player before another of them may (so a street never swarms them). */
    public static final long CROWD = 900L;
    /** How far (blocks) off it notices them to come over, and how close it comes. */
    public static final double NOTICE = 12.0;
    public static final double ARRIVE = 2.2;
    /** How long (ticks) it tries to reach them before it gives up. */
    public static final long TRIES = 240L;

    /** How often (ticks) {@code temper} comes over to someone it is {@code regard} warm to ({@code partner}: theirs), or 0: never. */
    public static long every(Temper temper, int regard, boolean partner) {
        long base = partner ? PARTNER_EVERY : regard >= HumanPersona.CLOSE ? CLOSE_EVERY : regard >= HumanPersona.WARM ? WARM_EVERY : 0L;
        return Math.round(base * keen(temper));
    }

    /** How readily {@code temper} comes over (a factor on the wait: under 1, sooner). */
    static double keen(Temper temper) {
        return switch (temper) {
            case FLIRTY -> 0.6;
            case CHEERFUL, BOLD, PLAYFUL, ROMANTIC -> 0.7;
            case KIND -> 0.85;
            case DREAMY, SARCASTIC -> 1.1;
            case NERVOUS -> 1.4;
            case STOIC -> 1.6;
            case SHY, GRUMPY -> 1.8;
        };
    }

    /**
     * Why {@code temper} comes over now: worried if they're {@code hurt}; a partner also because it {@code missed} them (not seen for days),
     * for a kiss, a hug or a walk; a gift only if it hasn't given one today ({@code gifted}). {@code roll}: any number.
     */
    public static Why pick(Temper temper, int regard, boolean partner, boolean hurt, boolean missed, boolean gifted, int roll) {
        if (hurt) {
            return Why.WORRIED;
        }
        int[] weights = new int[Why.values().length];
        weights[Why.HELLO.ordinal()] = partner ? 1 : 3;
        weights[Why.NEWS.ordinal()] = 2;
        weights[Why.GOSSIP.ordinal()] = partner ? 1 : regard >= HumanPersona.CLOSE ? 2 : 1;
        weights[Why.GIFT.ordinal()] = gifted || regard < HumanPersona.CLOSE && !partner ? 0 : giving(temper);
        if (partner) {
            weights[Why.MISSED.ordinal()] = missed ? 8 : 1;
            weights[Why.KISS.ordinal()] = kissing(temper);
            weights[Why.HUG.ordinal()] = hugging(temper);
            weights[Why.WALK.ordinal()] = 2;
        }
        int total = 0;
        for (int w : weights) {
            total += w;
        }
        int at = Math.floorMod(roll, total);
        for (Why why : Why.values()) {
            at -= weights[why.ordinal()];
            if (at < 0) {
                return why;
            }
        }
        return Why.HELLO;
    }

    static int giving(Temper temper) {
        return switch (temper) {
            case KIND, CHEERFUL, ROMANTIC -> 3;
            case GRUMPY, STOIC, SARCASTIC -> 1;
            default -> 2;
        };
    }

    static int kissing(Temper temper) {
        return switch (temper) {
            case FLIRTY, ROMANTIC -> 4;
            case BOLD, PLAYFUL, CHEERFUL -> 3;
            case KIND, DREAMY, SARCASTIC -> 2;
            case GRUMPY, STOIC, NERVOUS, SHY -> 1;
        };
    }

    static int hugging(Temper temper) {
        return switch (temper) {
            case SHY, NERVOUS, KIND -> 3;
            case GRUMPY -> 1;
            default -> 2;
        };
    }

    // ---------------------------------------------------------------- its words

    /**
     * What {@code temper} says, come over for {@code why} ({@code thing}: the gift, as {@link #thing} puts it; {@code said}: the news or
     * gossip it passes on). For a kiss or a hug, what it calls out on its way over; for a walk, nothing (its hand-taking says it).
     */
    public static String line(Temper temper, Why why, String thing, String said, int roll) {
        int t = temper.ordinal();
        return switch (why) {
            case HELLO -> pick(roll, HELLO[t]);
            case NEWS -> pick(roll, NEWS[t]) + said;
            case GOSSIP -> pick(roll, GOSSIP[t]) + said;
            case GIFT -> capital(pick(roll, GIFT[t]).replace("{thing}", thing));
            case WORRIED -> pick(roll, WORRIED[t]);
            case MISSED -> pick(roll, MISSED[t]);
            case KISS -> pick(roll, KISS[t]);
            case HUG -> pick(roll, HUG[t]);
            case WALK -> "";
        };
    }

    /** Names of wares said without "a" (stuff, not things). */
    static final Set<String> STUFF = Set.of("bread", "wheat", "sugar", "clay", "coal", "cobblestone", "flint", "honeycomb", "kelp",
        "dried kelp", "leather", "paper", "string", "white wool", "sugar cane", "bone meal", "mushroom stew", "beetroot soup");

    /** How many of a ware it gives (a third of what it sells at once, at least one). */
    public static int gives(int count) {
        return Math.max(1, count / 3);
    }

    /** A ware named {@code name}, {@code count} of it, as said: "cookies", "bread", "a lantern", "an iron ingot". */
    public static String thing(String name, int count) {
        if (name.endsWith("s") || count > 1 || STUFF.contains(name)) {
            return name;
        }
        return ("aeiou".indexOf(name.charAt(0)) >= 0 ? "an " : "a ") + name;
    }

    static String capital(String line) {
        return line.isEmpty() ? line : Character.toUpperCase(line.charAt(0)) + line.substring(1);
    }

    static String pick(int seed, String[] lines) {
        return lines[Math.floorMod(seed, lines.length)];
    }

    // Each by temper, in Temper's order: cheerful, shy, bold, kind, grumpy, flirty, dreamy, nervous, playful, stoic, sarcastic, romantic.

    static final String[][] HELLO = {
        {"There you are! I saw you from across the street and had to come say hi!", "Hi, hi, hi! Sorry. I'm just happy to see you."},
        {"...Um. Hi. I saw you, and I thought... I'd say hello. That's all.", "*fidgets* H-hello. I practised that. Sort of."},
        {"Hey! Thought I'd catch you before you vanished again.", "You. Stop right there. ...I just wanted to say hello. Ha!"},
        {"Oh, it's you. I was hoping I'd see you today. Are you eating properly?", "Hello, dear. Just came to see how you're doing."},
        {"Hmph. You again. ...Fine. Hello. Don't make it a thing.", "I came over to say hello. Don't ask me why. I don't know either."},
        {"Well, look who's wandering my streets looking like that.", "I saw you and my feet just walked me over here. Funny, that."},
        {"Oh! I was just thinking about you. Or about clouds. Both, maybe. Hello.", "Hello. The light's doing something lovely on you today."},
        {"H-hi! Sorry! I didn't mean to sneak up. Did I sneak up? Hi.", "Oh good, it's you. I mean, hello. It feels safer with you around."},
        {"Tag! ...You're it. Kidding. Hi!", "Boo! Ha! Got you. Hello, by the way."},
        {"Hello.", "...I saw you. I came to say hello. That's all."},
        {"Oh, look, a celebrity. Do I need to book a meeting?", "Fancy seeing you here. In the village. Where I live. Shocking."},
        {"I knew it was you before I even turned around. Hello.", "The whole street got brighter. Then I saw why. Hello, you."}};

    static final String[][] NEWS = {
        {"Ooh, have you heard? ", "Wait till you hear this! "},
        {"Um... did you hear? ", "*quietly* I heard something... "},
        {"Listen up. ", "Here's something you'll want to know. "},
        {"You should hear this, just so you're careful. ", "Did anyone tell you? "},
        {"Nobody tells me anything, but I heard this anyway. ", "Ugh. More news. "},
        {"Come closer, I've got a secret. ", "Want to hear something juicy? "},
        {"I heard something strange, like a dream... ", "The wind's been saying things. "},
        {"D-did you hear? I can't stop thinking about it. ", "Something's going on, I'm sure of it. "},
        {"Guess what! No, really, guess. Too slow! ", "Newsflash! "},
        {"News. ", "You should know. "},
        {"Breaking news from our thrilling little village: ", "Hold onto your hat. "},
        {"There's a story going round. Listen... ", "Oh, you have to hear this. "}};

    static final String[][] GOSSIP = {
        {"Can I tell you something? ", "Okay, don't tell anyone, but... "},
        {"*whispers* ...", "I shouldn't say, but... "},
        {"I'll say it to anyone. ", "Real talk? "},
        {"I'm a little worried about someone. ", "I only say this because I care. "},
        {"Somebody has to say it. ", "Don't repeat this. "},
        {"Psst. Gossip, for your ears only. ", "Lean in, gorgeous. "},
        {"I was thinking about the people here... ", "Do you ever watch people? I do. "},
        {"I-I don't want to spread things, but... ", "Is it bad to tell you this? "},
        {"Secret mission briefing! ", "Gossip time! "},
        {"A word. ", "Something I noticed. "},
        {"In today's episode of 'people are weird': ", "You'll love this. "},
        {"I see everything in this village, you know. ", "Little lives, little stories. Listen. "}};

    static final String[][] GIFT = {
        {"Surprise! I saved you {thing}! Don't argue!", "Look what I've got for you: {thing}! Made my whole morning."},
        {"*holds out {thing}* ...For you. If you want. You don't have to.", "I-I put aside {thing} for you. ...Okay. Bye."},
        {"Hands out. {thing}. Yours now, no arguing.", "Caught you! {thing}, from me. Don't thank me."},
        {"I had {thing} going spare, dear. For you. Look after yourself out there.", "Here, {thing}. You work too hard, I can tell."},
        {"Take {thing} off my hands. Cluttering up the place. That's the only reason.", "Here. {thing}. Don't make a face about it."},
        {"A little something for my favourite face: {thing}.", "{thing}, from me to you. Think of me later."},
        {"I found {thing} and thought of you. I don't know why. I never know why.", "I had a feeling about you today. So: {thing}."},
        {"I-I brought you {thing}. Is that weird? Sorry if that's weird.", "{thing}! For you! For keeping us safe. Or just because. Both."},
        {"Close your eyes! ...Okay, open! {thing}! Ta-da!", "Special delivery for one lucky person: {thing}!"},
        {"{thing}. For you.", "Spare from the work. {thing}. Yours."},
        {"Congratulations, you've won {thing}. No refunds.", "I definitely didn't save you {thing}. Coincidence. Pure coincidence."},
        {"I wrapped {thing} in my whole heart. Metaphorically. Here.", "{thing}, because you deserve something good today."}};

    static final String[][] WORRIED = {
        {"Oh no, you're hurt! Sit, sit! Who did this? I'll go yell at them!", "You're bleeding! Come on, let me find you a bandage."},
        {"Y-you're hurt... are you okay? I don't know what to do. Please be okay.", "*stares at your wounds* ...Does it hurt? Sorry. Stupid question."},
        {"Who did that to you? Point. I'll handle it.", "You look terrible. Sit down before you fall down."},
        {"Oh, sweetheart, look at you. Let me clean that up. Hold still.", "You're hurt. Come inside, I'll make you something warm."},
        {"You look awful. ...Go rest. That's an order, not concern.", "Bleeding all over my street. Go get that looked at. Now."},
        {"Who scratched up that pretty face? I want names.", "Hurt? Come here, I'll kiss it better. ...I'm only half joking."},
        {"You've got blood on you... like a painting. A sad one. Are you alright?", "You're hurt. I dreamed something like this. I didn't like it."},
        {"Y-you're bleeding! Was it a ghoul? Is it still here? Are YOU okay?!", "Oh no, oh no, you're hurt. Sit down, please sit down."},
        {"Whoa, what happened? Lose a fight with a bear? ...Seriously, are you okay?", "Ow, that looks bad. No more playing tag for you."},
        {"You're wounded. Rest.", "Sit. Breathe. You'll heal."},
        {"Great look. Very 'almost died'. Go lie down, hero.", "You're bleeding on my shoes. ...Are you alright, though? Really?"},
        {"My heart stopped when I saw you like this. Please, be careful.", "Don't you dare get hurt again. I can't bear it."}};

    static final String[][] MISSED = {
        {"THERE you are! I missed you so much! Where have you BEEN?", "You're back! I counted the days. Okay, I lost count. But I missed you!"},
        {"...You're back. I missed you. I didn't say it to anyone. Just to you.", "*runs over, then stops, embarrassed* ...Hi. I missed you."},
        {"Finally! You don't get to disappear that long again. Got it?", "Where've you been? I was about to go looking for you myself."},
        {"There you are, love. I kept worrying. I'm so glad you're safe.", "I missed you. I kept a seat for you at supper every night."},
        {"Oh, NOW you show up. ...I missed you. Shut up.", "Took you long enough. ...Don't go away like that again."},
        {"I was starting to think you'd found someone prettier. Impossible, but still.", "Missed me? I missed you. Badly. Come here."},
        {"I dreamed about you every night you were gone. Now you're real again.", "You're back. The days felt grey without you."},
        {"You're okay! I was so scared something happened to you!", "Please don't vanish like that. I kept imagining the worst."},
        {"Missing person found! Reward: me. Hi!", "You were gone SO long. I was bored out of my mind. Hi!"},
        {"...You're back. Good. I missed you.", "I noticed you were gone. Every day."},
        {"Oh, you remembered where I live. Touching. ...I missed you, idiot.", "Look who finally crawled back. ...I'm glad you're okay."},
        {"Every day without you was a page torn out. You're back. My story's whole again.", "I missed you like the moon misses the sun."}};

    static final String[][] KISS = {
        {"Wait, wait! I forgot something! *runs over*", "Hey, you! Hold still!"},
        {"*walks over, red to the ears* ...Can I...?", "Don't look. Just... close your eyes."},
        {"C'mere. I'm kissing you. No objections.", "You. Stay right there."},
        {"Come here, love. Just a little one.", "You looked like you needed this."},
        {"Ugh. Come here. Don't make it weird.", "Stand still. This is happening."},
        {"I've been thinking about your lips all day. Come here.", "Pucker up, gorgeous."},
        {"I saw you and forgot everything else. Come here.", "Close your eyes. Let's pretend it's sunset."},
        {"I-I'm going to kiss you now, is that okay? Okay. Okay.", "Don't laugh. I'm coming over. To kiss you."},
        {"Kiss ambush incoming!", "Ready or not, here I come!"},
        {"Come here.", "...Hold still a moment."},
        {"Brace yourself. Affection incoming.", "Don't get used to this. Come here."},
        {"My heart walked me over here. Kiss me.", "I've waited all day for this."}};

    static final String[][] HUG = {
        {"HUG! Incoming! Arms out!", "I need a hug and you're the best one!"},
        {"...Can I have a hug? Just a small one.", "*opens their arms, shyly*"},
        {"Come here. Hug. Now.", "Bring it in."},
        {"You look like you need a hug. Come here.", "Come here, let me hold you a moment."},
        {"I'm hugging you. Don't comment.", "One hug. Then I go back to being annoyed."},
        {"Hug me. And take your time.", "Come here, I'm cold. ...I'm not cold."},
        {"Let's hug like the world's ending softly.", "Hold me. Just for a breath."},
        {"C-can I hug you? Today was a lot.", "I just need to hold onto something. Someone. You."},
        {"Hug tackle!", "Bear hug! Rawr!"},
        {"...Come here.", "A moment. Hold still."},
        {"Fine, I'll be the needy one today. Hug.", "This is a hostage situation. The hostage is you. In a hug."},
        {"Come here. I want to hold you until the stars come out.", "Let me hold you like the end of a love story."}};
}
