package com.hex.kagune.control.family;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 3.1.9 (pure, tested; the owner: "make the relationships even more complex ... jealousy is actually ... a really prominent thing ... if
 * you have two wives ... the kids and all, they react to each other, like meeting each other, if they got like siblings from another
 * mother, they interact and talk about it, like about other mothers and like other fathers ... as immersive as possible ... You gotta
 * make up your own shit"): what a blended family says.
 * <ul>
 * <li>{@link #witnessed}: a partner who sees the player being sweet on another, said to their face.</li>
 * <li>{@link #coWives}: two of the player's partners meeting (the first time; one of them jealous; both with children by the player; or
 * settled into it), each pair in their own voices.</li>
 * <li>{@link #halfSiblings}: two of the player's children by different mothers or fathers, talking about it (little ones, big ones, or a big
 * one and a little one).</li>
 * </ul>
 * Names in a line: {you} the player, {other} the partner the player was sweet on, {akid}/{bkid} the first and second partner's child by the
 * player, {a}/{b} the two children, {amom}/{bmom} each child's other parent (not the one they share), {par} what they call the one they
 * share.
 */
public final class Blended {
    private Blended() {
    }

    /** A line of a talk: who says it (0 the first, 1 the second), what, and the gesture with it (a word, or "" for none). */
    public record Said(int who, String text, String gesture) {
    }

    /** How two partners stand when they meet. */
    public enum Mood {
        /** The first time they meet as the same person's. */
        FIRST,
        /** One of them is jealous now. */
        RIVAL,
        /** Both have children by the player. */
        KIDS,
        /** Settled into it. */
        EASY
    }

    /** How old the two children are: both little (a toddler or a child), both big (teens and up), or one of each (the big one first). */
    public enum Ages {
        LITTLE, BIG, MIXED
    }

    /** The key of a pair, either way round: the two persona keys in order ("eto+touka"). */
    public static String pair(String a, String b) {
        return a.compareTo(b) <= 0 ? a + "+" + b : b + "+" + a;
    }

    /** True if {@code a} speaks first in its pair with {@code b} (the earlier key). */
    public static boolean first(String a, String b) {
        return a.compareTo(b) <= 0;
    }

    // ---------------------------------------------------------------- seen being sweet on another

    private static final Map<String, String[][]> WITNESSED = Map.of(
        "eto", new String[][] {
            {"Oh, don't stop on my account. I'm taking notes. For a tragedy.", "laugh"},
            {"{other}. Again. You do have a type, don't you, {you}? Lucky us.", "lean"},
            {"Mm. I'll remember that face you made. I remember everything, you know.", "look"},
            {"How sweet. How very, very sweet. I think I'll go and sharpen something.", "twirl"},
            {"You kiss {other} like that, and me like a chore. Interesting.", "cross"},
            {"Careful, {you}. I write the endings around here.", "giggle"}},
        "touka", new String[][] {
            {"Right in front of me. Seriously?", "huff"},
            {"Don't look at me like that. Go on. Kiss {other} some more. See if I care.", "cross"},
            {"Tch. I'm going to kick something. It might be you.", "angry"},
            {"Unbelievable. I made you coffee this morning, {you}.", "huff"},
            {"You two done? Good. I'm leaving.", "cross"},
            {"...It's fine. It's FINE. Don't talk to me for a while.", "pout"}},
        "ken", new String[][] {
            {"Oh. I, um. I'll just... I'll be over there.", "look"},
            {"No, it's okay. It's okay. I knew. I just didn't have to see it.", "shy"},
            {"Sorry, I didn't mean to stare. ...Yes I did.", "look"},
            {"{other}'s lucky. That's all. I'm fine, {you}.", "shy"},
            {"I read somewhere that jealousy is love with nowhere to go. I think I get it now.", "read"},
            {"Could you... not, when I'm right here? Please?", "cross"}});

    /** What {@code persona} says, seeing the player sweet on another ({@code seed}: which). */
    public static Said witnessed(String persona, int seed, Map<String, String> names) {
        String[][] lines = WITNESSED.getOrDefault(persona, WITNESSED.get("touka"));
        String[] line = lines[Math.floorMod(seed, lines.length)];
        return new Said(0, fill(line[0], names), line[1]);
    }

    /** How many witnessed lines {@code persona} has. */
    public static int witnessedCount(String persona) {
        String[][] lines = WITNESSED.get(persona);
        return lines == null ? 0 : lines.length;
    }

    // ---------------------------------------------------------------- two partners meeting

    /** Each pair's talks by mood: lines of {who, text, gesture}, who 0 the pair's first key ("eto" in "eto+touka"). */
    private static final Map<String, Map<Mood, String[][][]>> PAIRS = Map.of(
        "eto+touka", Map.of(
            Mood.FIRST, new String[][][] {{
                {"1", "So you're the other one.", "cross"},
                {"0", "And you're the angry one. {you} described you perfectly.", "giggle"},
                {"1", "What else did {you} say?", "huff"},
                {"0", "That you make the best coffee in the ward. I'm told I'm the interesting one.", "twirl"},
                {"1", "Hah. Interesting is a word for it.", "cross"},
                {"0", "Truce, little rabbit? I'd hate to ruin the furniture.", "lean"},
                {"1", "...Truce. For now. Touch my cups and it's over.", "huff"}}},
            Mood.RIVAL, new String[][][] {{
                {"1", "You've been smug all day.", "cross"},
                {"0", "I've been happy all day. You only see the difference when it isn't you.", "giggle"},
                {"1", "Keep talking. I dare you.", "angry"},
                {"0", "Oh, I will. I'm a writer. Words are what I'm for.", "twirl"},
                {"1", "Then write this down: back off.", "huff"}}, {
                {"0", "Do you think {you} likes me more, or just differently?", "lean"},
                {"1", "I think {you} likes you at a safe distance.", "cross"},
                {"0", "Ooh. That one stung. You've been practising.", "laugh"},
                {"1", "I've had a lot of nights to practise.", "huff"}}},
            Mood.KIDS, new String[][][] {{
                {"0", "{akid} told me your {bkid} can climb anything. Anything.", "giggle"},
                {"1", "Don't. I found {bkid} on the roof last week. The ROOF.", "huff"},
                {"0", "Mine reads under the covers with a torch. I pretend not to know.", "read"},
                {"1", "...They should do something together. Supervised.", "cross"},
                {"0", "Heavily supervised. By you. I'll bring the snacks.", "giggle"},
                {"1", "You'll bring the trouble, is what you'll bring.", "cross"}}},
            Mood.EASY, new String[][][] {{
                {"1", "Coffee?", "sip"},
                {"0", "From you? Is it poisoned?", "giggle"},
                {"1", "Only on Tuesdays.", "sip"},
                {"0", "Then I'll take two. We both love the same fool, you know.", "lean"},
                {"1", "Don't make it sound sweet. ...Here.", "blush"}}, {
                {"0", "Funny. A year ago one of us would have killed the other for this.", "lean"},
                {"1", "A year ago I didn't know you snored.", "cross"},
                {"0", "I do NOT snore. I breathe with conviction.", "huff"},
                {"1", "Tell {you} that. They have to sleep between us.", "laugh"}}}),
        "eto+ken", Map.of(
            Mood.FIRST, new String[][][] {{
                {"0", "Well, well. The quiet one. {you} has such varied taste.", "giggle"},
                {"1", "Um. Hello. You're Eto. I've, um, read some of your books.", "shy"},
                {"0", "Did you like them? Be honest. I'll know if you're lying.", "lean"},
                {"1", "...They frightened me. In a good way. Mostly.", "read"},
                {"0", "Oh, I like you. That's going to make this so much harder.", "laugh"}}},
            Mood.RIVAL, new String[][][] {{
                {"1", "You were with {you} again last night.", "look"},
                {"0", "Were you counting, Ken? How diligent.", "giggle"},
                {"1", "I wasn't counting. I was waiting. It's different.", "shy"},
                {"0", "It isn't, sweetheart. It never is.", "lean"},
                {"1", "...I'm sorry. I don't know how to be good at this.", "look"}}},
            Mood.KIDS, new String[][][] {{
                {"1", "{bkid} asked me why {akid} has a different mother.", "read"},
                {"0", "And what did you say? Something gentle and wise, I bet.", "giggle"},
                {"1", "I said families are made of whoever stays. I think I got that from a book.", "shy"},
                {"0", "It's a good line. I might steal it.", "write"},
                {"1", "Please don't put our kids in a novel.", "cross"},
                {"0", "No promises.", "laugh"}}},
            Mood.EASY, new String[][][] {{
                {"1", "I made too much tea. Do you, um, want some?", "sip"},
                {"0", "You made too much on purpose. You always do.", "giggle"},
                {"1", "...Maybe. It's nicer when someone's there.", "shy"},
                {"0", "It is. Don't tell anyone I said that.", "sip"}}}),
        "ken+touka", Map.of(
            Mood.FIRST, new String[][][] {{
                {"1", "Ken. Of course it's you.", "cross"},
                {"0", "Touka. I... didn't know you and {you} were... I'm sorry.", "shy"},
                {"1", "Why are YOU sorry? You're always sorry.", "huff"},
                {"0", "Habit. I'm working on it. Sorry. ...Ah.", "look"},
                {"1", "Idiot. ...Sit. I'll make two coffees.", "sip"}}},
            Mood.RIVAL, new String[][][] {{
                {"1", "Stop following {you} around like a lost puppy.", "cross"},
                {"0", "I'm not following. We happen to be walking the same way.", "shy"},
                {"1", "For three hours?", "huff"},
                {"0", "...It's a long way.", "look"},
                {"1", "Tch. You're impossible.", "angry"}}},
            Mood.KIDS, new String[][][] {{
                {"0", "{bkid} taught {akid} how to kick. I saw it from the window.", "look"},
                {"1", "Good. Someone in this family should be able to.", "cross"},
                {"0", "{akid} tried it on me. It worked.", "shy"},
                {"1", "...Hah. Okay. That's actually great.", "laugh"},
                {"0", "You're proud. You're smiling.", "giggle"},
                {"1", "Shut up, Ken.", "blush"}}},
            Mood.EASY, new String[][][] {{
                {"0", "The coffee's better than at the cafe.", "sip"},
                {"1", "Don't say that where the manager can hear.", "sip"},
                {"0", "Your secret's safe. ...This is nice. All of us. Strange, but nice.", "shy"},
                {"1", "Strange is the only kind of nice we get.", "lean"}}}));

    /** The talk two partners ({@code a} and {@code b}, persona keys) have when they meet, in {@code mood}; who 0 is {@code a}. */
    public static List<Said> coWives(String a, String b, Mood mood, int seed, Map<String, String> names) {
        Map<Mood, String[][][]> pair = PAIRS.get(pair(a, b));
        if (pair == null || a.equals(b)) {
            return List.of();
        }
        String[][][] talks = pair.getOrDefault(mood, pair.get(Mood.EASY));
        String[][] talk = talks[Math.floorMod(seed, talks.length)];
        boolean swap = !first(a, b);
        List<Said> out = new ArrayList<>();
        for (String[] line : talk) {
            int who = Integer.parseInt(line[0]);
            out.add(new Said(swap ? 1 - who : who, fill(line[1], names), line[2]));
        }
        return out;
    }

    /** Every pair that has talks. */
    public static List<String> pairs() {
        return List.copyOf(PAIRS.keySet());
    }

    // ---------------------------------------------------------------- children by different mothers or fathers

    private static final Map<Ages, String[][][]> HALF = Map.of(
        Ages.LITTLE, new String[][][] {{
            {"0", "My mama is {amom}. Who's your mama?", ""},
            {"1", "{bmom}! ...But we have the same {par}.", ""},
            {"0", "So we're half brother-sister-ish?", ""},
            {"1", "We're whole friends. That's more.", ""}}, {
            {"1", "Does {amom} make you eat vegetables too?", ""},
            {"0", "Yes. Does {bmom}?", ""},
            {"1", "Yes. Mamas are all the same.", ""},
            {"0", "But {par} gives us cookies.", ""},
            {"1", "{par} is the best one.", ""}}, {
            {"0", "Why do you have a different mama?", ""},
            {"1", "Because {par} loved two people. That's allowed.", ""},
            {"0", "Then I can love two people too. You and cake.", ""},
            {"1", "Cake isn't a people.", ""}}},
        Ages.BIG, new String[][][] {{
            {"0", "Does it ever get weird? Two mothers in one house. Well, one each.", ""},
            {"1", "All the time. {bmom} and {amom} had a whole stare-off at breakfast.", ""},
            {"0", "Who won?", ""},
            {"1", "{par} did. Walked in, kissed both of them, left with the toast.", ""},
            {"0", "Legend.", ""}}, {
            {"1", "I used to hate that you existed. No offence.", ""},
            {"0", "None taken. I hated your face for a year.", ""},
            {"1", "And now?", ""},
            {"0", "Now it's just a face. A stupid one. I'd still fight anyone for it.", ""}}, {
            {"0", "Do you think {par} loves {bmom} more than {amom}?", ""},
            {"1", "I think {par} loves them differently. Like how you love your kagune and your bed.", ""},
            {"0", "...That's the weirdest thing anyone's ever said to me. And it makes sense.", ""}}},
        Ages.MIXED, new String[][][] {{
            {"1", "Why do you look like {par} but not like my mama?", ""},
            {"0", "Because my mother is {amom}. Yours is {bmom}. We share {par}.", ""},
            {"1", "Can I have half your dessert, since you're half mine?", ""},
            {"0", "...That's not how it works. Fine. Half.", ""}}, {
            {"0", "Listen, kid. If anyone at the market says something about our family, you come get me.", ""},
            {"1", "What would they say?", ""},
            {"0", "Nothing, if they know what's good for them. {bmom} would agree with me on that one.", ""},
            {"1", "You're scary. I like it.", ""}}});

    /** The talk two of the player's children by different mothers or fathers have ({@code ages}; who 0 the older). */
    public static List<Said> halfSiblings(Ages ages, int seed, Map<String, String> names) {
        String[][][] talks = HALF.get(ages);
        String[][] talk = talks[Math.floorMod(seed, talks.length)];
        List<Said> out = new ArrayList<>();
        for (String[] line : talk) {
            out.add(new Said(Integer.parseInt(line[0]), fill(line[1], names), line[2]));
        }
        return out;
    }

    /** How many talks two children of {@code ages} have. */
    public static int halfCount(Ages ages) {
        return HALF.get(ages).length;
    }

    /** How many talks a pair has in {@code mood}. */
    public static int coWifeCount(String a, String b, Mood mood) {
        Map<Mood, String[][][]> pair = PAIRS.get(pair(a, b));
        return pair == null || !pair.containsKey(mood) ? 0 : pair.get(mood).length;
    }

    /** {@code line} with its {names} filled ({@code names}' own; any left over as "them"). */
    public static String fill(String line, Map<String, String> names) {
        String out = line;
        for (Map.Entry<String, String> name : names.entrySet()) {
            out = out.replace("{" + name.getKey() + "}", name.getValue() == null || name.getValue().isEmpty() ? "them" : name.getValue());
        }
        return out.replaceAll("\\{[a-z]+}", "them");
    }
}
