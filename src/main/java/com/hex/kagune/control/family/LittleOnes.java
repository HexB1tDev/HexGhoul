package com.hex.kagune.control.family;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 1.3.59: a brother or sister with the baby of the family (pure, tested; the world's side is {@code SiblingLife}), from the last handoff's
 * "babies in sibling moments (an older one entertaining a baby)". An older one (a toddler too, in its own way) goes over to a baby sibling
 * and plays with it: peekaboo (its own face hidden, then "BOO!"), silly faces, baby talk, or, in the evening, a hum to settle it. The baby
 * answers as a baby does (shrieks of laughter, a fistful of hair, a tiny yawn: actions, in its thought cloud), with its arms up or a
 * giggle. What the older one says is its own temper's and age's.
 */
public final class LittleOnes {
    /** What they do (never sent: order free). */
    public enum Kind {
        PEEKABOO, FACES, BABBLE, LULLABY
    }

    /** One line: whether the baby says (does) it, what, and what it does with its body. */
    public record Line(boolean baby, String text, ChildCue cue) {
    }

    /** How often one older child plays with a baby sibling, at most (ticks), and the chance of it when it could. */
    public static final long EVERY = 1800L;
    public static final double CHANCE = 0.5;

    private LittleOnes() {
    }

    /** What the older one ({@code temper}) does with the baby: in the {@code evening}, a hum to settle it; else by its temper and {@code roll}. */
    public static Kind pick(ChildTemper temper, boolean evening, double roll) {
        if (evening) {
            return Kind.LULLABY;
        }
        double[] w = {3.0, 2.0, 2.0, 0.6};
        if (temper == ChildTemper.MISCHIEF || temper == ChildTemper.SUNNY) {
            w[1] += 1.5;
        }
        if (temper == ChildTemper.GENTLE || temper == ChildTemper.SHY) {
            w[3] += 1.5;
            w[2] += 0.5;
        }
        if (temper == ChildTemper.SERIOUS || temper == ChildTemper.CURIOUS || temper == ChildTemper.BOLD) {
            w[2] += 1.5;
        }
        double total = 0.0;
        for (double x : w) {
            total += x;
        }
        double at = Math.max(0.0, Math.min(0.999999, roll)) * total;
        Kind[] kinds = Kind.values();
        for (int i = 0; i < kinds.length; i++) {
            at -= w[i];
            if (at < 0.0) {
                return kinds[i];
            }
        }
        return Kind.BABBLE;
    }

    /** What they say and do, in turn: the older one ({@code temper} at {@code stage}) and the baby ({@code name}). */
    public static List<Line> talk(Kind kind, ChildTemper temper, ChildStage stage, String name, int seed) {
        List<Line> out = new ArrayList<>();
        String k = kind.name().toLowerCase(Locale.ROOT);
        switch (kind) {
            case PEEKABOO -> {
                out.add(new Line(false, line(k, "open", temper, stage, name, seed), ChildCue.COUNT));
                out.add(new Line(false, line(k, "boo", temper, stage, name, seed + 1), ChildCue.CHEER));
                out.add(new Line(true, line(k, "baby", null, ChildStage.BABY, name, seed + 2), ChildCue.CHEER));
                out.add(new Line(false, line(k, "close", temper, stage, name, seed + 3), ChildCue.GIGGLE));
            }
            case FACES -> {
                out.add(new Line(false, line(k, "open", temper, stage, name, seed), ChildCue.BLEH));
                out.add(new Line(true, line(k, "baby", null, ChildStage.BABY, name, seed + 2), ChildCue.GIGGLE));
                out.add(new Line(false, line(k, "close", temper, stage, name, seed + 3), temper == ChildTemper.SUNNY ? ChildCue.CHEER : ChildCue.GIGGLE));
            }
            case BABBLE -> {
                out.add(new Line(false, line(k, "open", temper, stage, name, seed), ChildCue.WAVE));
                out.add(new Line(true, line(k, "baby", null, ChildStage.BABY, name, seed + 2), ChildCue.CHEER));
                out.add(new Line(false, line(k, "close", temper, stage, name, seed + 3), temper == ChildTemper.CURIOUS ? ChildCue.THINK : ChildCue.NONE));
            }
            case LULLABY -> {
                out.add(new Line(false, line(k, "open", temper, stage, name, seed), ChildCue.PAT));
                out.add(new Line(true, line(k, "baby", null, ChildStage.BABY, name, seed + 2), ChildCue.NONE));
                out.add(new Line(false, line(k, "close", temper, stage, name, seed + 3), ChildCue.NONE));
            }
        }
        // What a line says it does comes first (a raspberry blown back, a silly face pulled).
        for (int i = 0; i < out.size(); i++) {
            ChildCue own = TalkCues.said(out.get(i).text());
            if (own != ChildCue.NONE && own != ChildCue.PAT) {
                out.set(i, new Line(out.get(i).baby(), out.get(i).text(), own));
            }
        }
        return out;
    }

    /** One line: {@code part} of {@code kind}, its temper's at its age if it has one (now and then the age's anyway), else its age's. */
    static String line(String kind, String part, ChildTemper temper, ChildStage stage, String name, int seed) {
        String group = stage == ChildStage.BABY ? "baby" : stage == ChildStage.TODDLER ? "little" : stage.older() ? "teen" : "big";
        List<String> any = LINES.get(kind + ":" + part + ":any:" + group);
        if (any == null && group.equals("teen")) {
            any = LINES.get(kind + ":" + part + ":any:big");
        }
        List<String> own = temper == null ? null : LINES.get(kind + ":" + part + ":" + temper.word() + ":" + group);
        if (own == null && temper != null && group.equals("teen")) {
            own = LINES.get(kind + ":" + part + ":" + temper.word() + ":big");
        }
        List<String> from = own != null && (any == null || Math.floorMod(seed, 3) != 0) ? own : any;
        String text = from.get(Math.floorMod(seed * 7 + 3, from.size()));
        return text.replace("{name}", name == null || name.isBlank() ? "baby" : name);
    }

    /** Every key's lines ("kind:part:temper|any:baby|little|big|teen"), for the tests. */
    public static Map<String, List<String>> lines() {
        return LINES;
    }

    private static final Map<String, List<String>> LINES = Map.ofEntries(
        // Peekaboo: its own face hidden, then BOO.
        Map.entry("peekaboo:open:any:little", List.of("Where's me? Where'd I go?", "Baby! Where am I?", "Peek... peek...")),
        Map.entry("peekaboo:open:any:big", List.of("Where'd I go? Where'd I go~?", "Peekaboo... where am I~?", "Hmm, where did I go, {name}?")),
        Map.entry("peekaboo:open:any:teen", List.of("Okay, okay. Where'd I go?", "Peek... a...")),
        Map.entry("peekaboo:open:serious:big", List.of("Observe. I am now invisible.")),
        Map.entry("peekaboo:open:bold:big", List.of("You can't see me! I'm a ghoul in the dark!")),
        Map.entry("peekaboo:open:shy:big", List.of("...where'd I go?")),
        Map.entry("peekaboo:boo:any:little", List.of("Boo!", "BOO! Hehe!")),
        Map.entry("peekaboo:boo:any:big", List.of("BOO!", "Peekaboo!", "...Boo!")),
        Map.entry("peekaboo:boo:any:teen", List.of("Boo. ...Okay, that was cute.", "Peekaboo!")),
        Map.entry("peekaboo:baby:any:baby", List.of("*shrieks with laughter*", "*squeals and kicks its little feet*", "*giggles, then stares, waiting for more*",
            "*claps its hands*")),
        Map.entry("peekaboo:close:any:little", List.of("Again! Again!", "Baby laughed!")),
        Map.entry("peekaboo:close:any:big", List.of("Ha! Again? Okay, again.", "You like that? Yeah you do.")),
        Map.entry("peekaboo:close:any:teen", List.of("...Fine. One more time.", "Don't tell anyone I did that.")),
        Map.entry("peekaboo:close:serious:big", List.of("Results: positive.")),
        Map.entry("peekaboo:close:mischief:big", List.of("Hehe. Easiest audience ever.")),
        Map.entry("peekaboo:close:gentle:big", List.of("There's that smile.")),
        // Silly faces.
        Map.entry("faces:open:any:little", List.of("Bleh! Bleh!", "Look! Funny face!")),
        Map.entry("faces:open:any:big", List.of("Look at this face. Look at it.", "*pulls the silliest face*")),
        Map.entry("faces:open:any:teen", List.of("*crosses eyes and puffs cheeks*", "You think that's funny? How about THIS?")),
        Map.entry("faces:open:bold:big", List.of("Behold! My scariest face! RAWR!")),
        Map.entry("faces:open:shy:big", List.of("*makes a tiny silly face, checking nobody's watching*")),
        Map.entry("faces:open:mischief:big", List.of("Okay, {name}. Watch closely. *nyeh*")),
        Map.entry("faces:baby:any:baby", List.of("*stares... then giggles*", "*blows a raspberry right back*", "*grabs at a nose*",
            "*laughs so hard it hiccups*")),
        Map.entry("faces:close:any:little", List.of("Baby funny!", "Hehe! Baby!")),
        Map.entry("faces:close:any:big", List.of("Ha! It worked!", "You're making faces back! No fair!")),
        Map.entry("faces:close:any:teen", List.of("...I'm keeping that one.", "Okay, you win this round.")),
        Map.entry("faces:close:mischief:big", List.of("Next lesson: pranks.")),
        Map.entry("faces:close:serious:big", List.of("Your technique needs work.")),
        Map.entry("faces:close:gentle:big", List.of("Aww. You're so silly.")),
        Map.entry("faces:close:sunny:big", List.of("Best audience EVER!")),
        // Baby talk.
        Map.entry("babble:open:any:little", List.of("Baby! Hi baby! Hi!", "You're small. I'm BIG.")),
        Map.entry("babble:open:any:big", List.of("Who's the littlest monster? You are! Yes you are!", "Hi, {name}. It's me. Your favourite.")),
        Map.entry("babble:open:any:teen", List.of("Hey, kiddo. What's the news?", "So, {name}. How's life down there?")),
        Map.entry("babble:open:serious:big", List.of("Hello. I am your older sibling. ...You're drooling.")),
        Map.entry("babble:open:bold:big", List.of("When you're bigger I'll teach you to fight. Deal?")),
        Map.entry("babble:open:curious:big", List.of("What are you thinking about? Can you even think yet?")),
        Map.entry("babble:open:gentle:big", List.of("Hi, little one. Are you being good?")),
        Map.entry("babble:open:mischief:big", List.of("When you can walk, we're raiding the pantry together.")),
        Map.entry("babble:baby:any:baby", List.of("*babbles something very important*", "*grabs a fistful of hair*", "*gurgles happily*",
            "*reaches up with both hands*", "*sneezes a tiny sneeze*")),
        Map.entry("babble:close:any:little", List.of("Baby said hi!", "Baby likes me!")),
        Map.entry("babble:close:any:big", List.of("I know, right? Totally agree.", "Ow! Okay, okay, you win.")),
        Map.entry("babble:close:any:teen", List.of("Fair point.", "...Yeah. Me too.")),
        Map.entry("babble:close:bold:big", List.of("OW. ...Okay. You're tough. Deal.")),
        Map.entry("babble:close:curious:big", List.of("Fascinating.")),
        Map.entry("babble:close:serious:big", List.of("Noted.")),
        Map.entry("babble:close:gentle:big", List.of("I love you too.")),
        Map.entry("babble:close:mischief:big", List.of("Partners in crime. Shake on it. ...Ew, sticky.")),
        // A hum to settle it, in the evening.
        Map.entry("lullaby:open:any:little", List.of("Shh... baby sleep...", "*pats the baby, very gently*")),
        Map.entry("lullaby:open:any:big", List.of("Shh, shh... it's okay, {name}.", "*hums a little tune*")),
        Map.entry("lullaby:open:any:teen", List.of("*hums an old lullaby, softly*", "Hey. Shh. I've got you.")),
        Map.entry("lullaby:open:shy:big", List.of("*hums very, very softly*")),
        Map.entry("lullaby:baby:any:baby", List.of("*yawns a tiny yawn*", "*grips a finger and won't let go*", "*blinks slowly*", "*fusses, then settles*")),
        Map.entry("lullaby:close:any:little", List.of("Baby sleepy.", "Shhh.")),
        Map.entry("lullaby:close:any:big", List.of("...there. Sleep, little one.", "Night night, {name}.")),
        Map.entry("lullaby:close:any:teen", List.of("Out like a light. ...I'm good at this.", "Sleep tight, kiddo.")));
}
