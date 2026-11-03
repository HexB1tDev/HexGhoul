package com.hex.kagune.control;

import com.hex.kagune.control.client.GenderView;
import com.hex.kagune.control.client.PathChoiceLayout.Box;
import com.hex.kagune.control.family.Chores;
import com.hex.kagune.control.npc.EtoLines;
import com.hex.kagune.control.npc.EtoMood;
import com.hex.kagune.control.npc.EtoRules;
import com.hex.kagune.control.npc.EtoTalk;
import com.hex.kagune.control.npc.NpcBonds;
import com.hex.kagune.control.npc.Romance;
import java.util.ArrayList;
import java.util.List;

/**
 * 1.3.9: genders and the one female body (Eto's, shared by every woman); 1.3.10 on: pregnancy, children (pure rules, no game).
 */
public final class FamilyTests {
    static int passed;
    static final List<String> FAILURES = new ArrayList<>();

    private static void check(boolean condition, String description) {
        if (condition) {
            passed++;
        } else {
            FAILURES.add(description);
        }
    }

    public static void main(String[] args) throws java.io.IOException {
        genders();
        strangers();
        body();
        screen();
        pregnancy();
        genome();
        talk();
        children();
        looks();
        ownerSkins();
        moments();
        home();
        chores();
        speech();
        kitchen();
        airplane();
        might();
        hands();
        spin();
        fights();
        reactions();
        initiative();
        drowsy();
        held();
        kagune();
        vitality();
        kit();
        parenting();
        etoChild();
        siblings();
        talks();
        cues();
        hideAndSeek();
        littleOnes();
        fear();
        if (!FAILURES.isEmpty()) {
            FAILURES.stream().limit(40).forEach(failure -> System.err.println("FAILED: " + failure));
            System.err.println(FAILURES.size() + " of " + (passed + FAILURES.size()) + " checks failed");
            System.exit(1);
        }
        System.out.println("Family tests: " + passed + " checks passed");
    }

    /**
     * 2.0.5: someone else's children. Strangers get gentle choices only, never a parent's; and NOT DATEABLE: no page, for a parent or a
     * stranger, at any stage including grown, ever offers romance (keys, words, the talk's own romance choices), and no child scene carries
     * a romance effect or words. Babies and toddlers say nothing coherent to strangers (the owner): two words at most outside the *actions*.
     */
    static void strangers() throws java.io.IOException {
        com.hex.kagune.control.family.ChildStage[] stages = com.hex.kagune.control.family.ChildStage.values();
        java.util.List<java.util.List<com.hex.kagune.control.family.ChildPage.Choice>> pages = new java.util.ArrayList<>();
        java.util.List<java.util.List<com.hex.kagune.control.family.ChildPage.Choice>> strangerPages = new java.util.ArrayList<>();
        for (com.hex.kagune.control.family.ChildStage stage : stages) {
            for (int bits = 0; bits < 32; bits++) {
                for (int ride = 0; ride <= 3; ride++) {
                    pages.add(com.hex.kagune.control.family.ChildPage.parent(stage, new com.hex.kagune.control.family.ChildPage.Parent((bits & 1) != 0, ride,
                        (bits & 2) != 0, (bits & 4) != 0, (bits & 8) != 0, (bits & 16) != 0)));
                    pages.add(com.hex.kagune.control.family.ChildPage.play(stage, (bits & 1) != 0, ride));
                }
            }
            for (boolean food : new boolean[] {false, true}) {
                strangerPages.add(com.hex.kagune.control.family.ChildPage.stranger(stage, food));
            }
        }
        for (boolean parent : new boolean[] {false, true}) {
            for (boolean free : new boolean[] {false, true}) {
                pages.add(com.hex.kagune.control.family.ChildPage.asleep(parent, free));
            }
        }
        pages.add(com.hex.kagune.control.family.ChildPage.asleep(false, true));
        boolean familyKnown = true;
        boolean noRomance = true;
        for (java.util.List<com.hex.kagune.control.family.ChildPage.Choice> page : pages) {
            for (com.hex.kagune.control.family.ChildPage.Choice choice : page) {
                String key = choice.key();
                familyKnown &= com.hex.kagune.control.family.ChildPage.FAMILY.contains(key) || key.startsWith("m:");
                noRomance &= !romanceKey(key) && !com.hex.kagune.control.family.ChildPage.romantic(choice.label());
            }
            check(page.size() <= com.hex.kagune.control.family.ChildPage.MOST, "a page fits the talk's reply slots");
        }
        check(familyKnown, "a parent's page holds only family keys (a new key must be added on purpose)");
        boolean gentle = true;
        boolean strangersAsk = true;
        for (java.util.List<com.hex.kagune.control.family.ChildPage.Choice> page : strangerPages) {
            for (com.hex.kagune.control.family.ChildPage.Choice choice : page) {
                gentle &= com.hex.kagune.control.family.ChildPage.STRANGER.contains(choice.key()) && !com.hex.kagune.control.family.ChildPage.parentOnly(choice.key());
                noRomance &= !romanceKey(choice.key()) && !com.hex.kagune.control.family.ChildPage.romantic(choice.label());
            }
            strangersAsk &= page.stream().anyMatch(c -> c.key().equals(com.hex.kagune.control.family.ChildPage.PARENTS))
                && page.stream().anyMatch(c -> c.key().equals(com.hex.kagune.control.family.ChildPage.CHAT));
        }
        check(gentle, "a stranger's page: gentle choices only, never a parent's (carry, hand, follow, teach, bed, home, feed, play)");
        check(strangersAsk, "a stranger can always talk and ask where its parents are");
        check(noRomance, "NOT DATEABLE: no child's page (parent or stranger, any stage, grown too) has a romance key or words");
        check(com.hex.kagune.control.family.ChildPage.stranger(com.hex.kagune.control.family.ChildStage.TODDLER, true).stream()
            .noneMatch(c -> c.key().equals(com.hex.kagune.control.family.ChildPage.FIVE))
            && com.hex.kagune.control.family.ChildPage.stranger(com.hex.kagune.control.family.ChildStage.BABY, true).stream()
            .noneMatch(c -> c.key().equals(com.hex.kagune.control.family.ChildPage.TREAT))
            && com.hex.kagune.control.family.ChildPage.stranger(com.hex.kagune.control.family.ChildStage.ADULT, true).stream()
            .anyMatch(c -> c.key().equals(com.hex.kagune.control.family.ChildPage.FIVE)), "a high five from a child up; no food offered to a baby");
        boolean onlySays = true;
        for (int n = 0; n <= com.hex.kagune.control.family.ChildPage.MOST; n++) {
            for (int ordinal : com.hex.kagune.control.family.ChildPage.choices(n)) {
                com.hex.kagune.control.npc.NpcTalk.Choice choice = com.hex.kagune.control.npc.NpcTalk.Choice.byOrdinal(ordinal);
                onlySays &= (choice.say() >= 0 || choice == com.hex.kagune.control.npc.NpcTalk.Choice.LEAVE)
                    && !com.hex.kagune.control.family.ChildPage.ROMANCE_CHOICES.contains(choice);
            }
        }
        check(onlySays, "a child's talk sends only reply slots and goodbye: never the talk's romance choices (kiss, ask out, propose...)");
        // Every child scene (what its parents talk about with it): no romance effect, no romance words.
        java.util.Set<String> effects = new java.util.HashSet<>(com.hex.kagune.control.npc.scene.SceneWords.CHILD_EFFECTS);
        effects.retainAll(com.hex.kagune.control.family.ChildPage.ROMANCE_KEYS);
        check(effects.isEmpty(), "no child scene effect is romance");
        com.hex.kagune.control.npc.scene.SceneBook.Parsed book = com.hex.kagune.control.npc.scene.SceneBook.parse("child",
            java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/data/hexkagune/talk/child.txt")));
        boolean scenesClean = !book.scenes().isEmpty();
        for (com.hex.kagune.control.npc.scene.Scene scene : book.scenes()) {
            scenesClean &= !com.hex.kagune.control.family.ChildPage.romantic(scene.prompt());
            for (com.hex.kagune.control.npc.scene.Scene.Page page : scene.pages().values()) {
                for (String line : page.lines()) {
                    scenesClean &= !com.hex.kagune.control.family.ChildPage.romantic(line);
                }
                for (com.hex.kagune.control.npc.scene.Scene.Option option : page.options()) {
                    scenesClean &= !com.hex.kagune.control.family.ChildPage.romantic(option.label());
                    for (String effect : option.effects()) {
                        scenesClean &= !romanceKey(effect);
                    }
                    for (String reply : option.replies()) {
                        scenesClean &= !com.hex.kagune.control.family.ChildPage.romantic(reply);
                    }
                }
            }
        }
        check(scenesClean, "NOT DATEABLE: no child scene has a romance effect or romance words");
        check(com.hex.kagune.control.family.ChildPage.romantic("Will you go out with me?") && com.hex.kagune.control.family.ChildPage.romantic("Marry me.")
            && !com.hex.kagune.control.family.ChildPage.romantic("Kiss it better.") && !com.hex.kagune.control.family.ChildPage.romantic("Not now, sweetheart."),
            "what reads as romance (a parent's kiss on a scraped knee does not)");
        check(com.hex.kagune.control.family.ChildPage.parentOnly("up") && com.hex.kagune.control.family.ChildPage.parentOnly("m:HUG")
            && com.hex.kagune.control.family.ChildPage.parentOnly("s:x:y:0") && com.hex.kagune.control.family.ChildPage.parentOnly("t:topic")
            && !com.hex.kagune.control.family.ChildPage.parentOnly("back") && !com.hex.kagune.control.family.ChildPage.parentOnly(com.hex.kagune.control.family.ChildPage.CHAT),
            "what only parents may do");
        // The lines: filled in, never romance, and little ones never coherent.
        boolean filled = true;
        boolean friendly = true;
        boolean little = true;
        com.hex.kagune.control.npc.Display[] shown = com.hex.kagune.control.npc.Display.values();
        for (com.hex.kagune.control.family.ChildStage stage : stages) {
            for (com.hex.kagune.control.family.ChildTemper temper : com.hex.kagune.control.family.ChildTemper.values()) {
                for (int seed = 0; seed < 12; seed++) {
                    java.util.List<String> lines = new java.util.ArrayList<>();
                    lines.add(com.hex.kagune.control.family.StrangerLines.hello(stage, temper, seed));
                    lines.add(com.hex.kagune.control.family.StrangerLines.chat(stage, temper, "Hana", "Touka", "Riku", seed));
                    lines.add(com.hex.kagune.control.family.StrangerLines.chat(stage, temper, "Hana", "", "", seed));
                    lines.add(com.hex.kagune.control.family.StrangerLines.wave(stage, temper, seed));
                    lines.add(com.hex.kagune.control.family.StrangerLines.five(stage, temper, seed));
                    for (boolean eats : new boolean[] {false, true}) {
                        lines.add(com.hex.kagune.control.family.StrangerLines.treat(stage, temper, eats, seed));
                    }
                    for (boolean near : new boolean[] {false, true}) {
                        for (boolean mother : new boolean[] {false, true}) {
                            lines.add(com.hex.kagune.control.family.StrangerLines.parents(stage, temper, near, mother, mother ? "Touka" : "Riku", seed));
                            lines.add(com.hex.kagune.control.family.StrangerLines.parents(stage, temper, near, mother, "", seed));
                        }
                    }
                    for (com.hex.kagune.control.npc.Display display : shown) {
                        if (display != com.hex.kagune.control.npc.Display.NONE) {
                            lines.add(com.hex.kagune.control.family.StrangerLines.scared(stage, temper, display, seed));
                        }
                    }
                    lines.add(com.hex.kagune.control.family.StrangerLines.hiding(stage, "Mama", seed));
                    lines.add(com.hex.kagune.control.family.StrangerLines.hiding(stage, "", seed));
                    for (com.hex.kagune.control.family.ChildMoment moment : new com.hex.kagune.control.family.ChildMoment[] {com.hex.kagune.control.family.ChildMoment.WAVE,
                        com.hex.kagune.control.family.ChildMoment.CLAP, com.hex.kagune.control.family.ChildMoment.HIGH_FIVE}) {
                        if (moment.fits(stage)) {
                            lines.add(com.hex.kagune.control.family.StrangerLines.moment(moment, stage, seed));
                        }
                    }
                    for (String line : lines) {
                        filled &= line != null && !line.isBlank() && !line.contains("null") && !line.contains("{") && !line.contains("  ");
                        friendly &= !com.hex.kagune.control.family.ChildPage.romantic(line);
                        if (stage == com.hex.kagune.control.family.ChildStage.BABY || stage == com.hex.kagune.control.family.ChildStage.TODDLER) {
                            little &= spoken(line) <= 2;
                        }
                    }
                }
            }
        }
        check(filled, "a stranger's lines are filled in (names, no gaps)");
        check(friendly, "NOT DATEABLE: a child's words to a stranger are never flirty, grown ones included");
        check(little, "a baby and a toddler say two words at most to a stranger (the rest is what they do)");
        check(spoken("*babbles at you* Ba! Ba-ba!") == 2 && spoken("I'm Hana! My mom is Touka.") == 6, "spoken words counted outside the *actions*");
        check(com.hex.kagune.control.family.StrangerLines.compass(0, -5).equals("north") && com.hex.kagune.control.family.StrangerLines.compass(5, 0).equals("east")
            && com.hex.kagune.control.family.StrangerLines.compass(0, 5).equals("south") && com.hex.kagune.control.family.StrangerLines.compass(-3, -3).equals("north-west"),
            "which way its parent is");
        check(com.hex.kagune.control.family.StrangerLines.shyOfHands(com.hex.kagune.control.family.ChildStage.CHILD, com.hex.kagune.control.family.ChildTemper.SHY)
            && !com.hex.kagune.control.family.StrangerLines.shyOfHands(com.hex.kagune.control.family.ChildStage.TEEN, com.hex.kagune.control.family.ChildTemper.SHY),
            "a shy child won't high-five a stranger");
    }

    /** A key that is romance: one of the words, or one of them inside it ("ask_out", "m:KISS"). */
    private static boolean romanceKey(String key) {
        String k = key.toLowerCase(java.util.Locale.ROOT);
        for (String word : com.hex.kagune.control.family.ChildPage.ROMANCE_KEYS) {
            if (k.equals(word) || k.endsWith(":" + word) || k.contains(word + "_") || k.contains("_" + word)) {
                return true;
            }
        }
        return false;
    }

    /** How many words are spoken, outside the *actions*. */
    private static int spoken(String line) {
        String said = line.replaceAll("\\*[^*]*\\*", " ").replaceAll("[^A-Za-z' -]", " ").trim();
        return said.isEmpty() ? 0 : said.split("\\s+").length;
    }

    static void genders() {
        check(Gender.UNSET.ordinal() == 0 && Gender.MALE.ordinal() == 1 && Gender.FEMALE.ordinal() == 2, "genders are sent and kept by ordinal: never reordered");
        check(Gender.byOrdinal(-1) == Gender.UNSET && Gender.byOrdinal(3) == Gender.UNSET && Gender.byOrdinal(2) == Gender.FEMALE, "a bad ordinal is unpicked");
        check(!Gender.UNSET.choice() && Gender.MALE.choice() && Gender.FEMALE.choice() && Gender.FEMALE.female() && !Gender.MALE.female(),
            "male and female are the picks; only female is a woman");
        java.util.UUID id = java.util.UUID.randomUUID();
        Genders.put(id, Gender.FEMALE);
        boolean known = Genders.of(id) == Gender.FEMALE;
        Genders.put(id, Gender.UNSET);
        check(known && Genders.of(id) == Gender.UNSET && Genders.of(null) == Gender.UNSET, "each side remembers who is who, and forgets");
    }

    static void body() {
        check(EtoRules.SIZE == FemaleBody.SIZE, "Eto is the female body's size: one body for every woman");
        check(Math.abs(FemaleBody.LEG_X - 2.0F * FemaleBody.HIPS - (FemaleBody.VANILLA_LEG_X - 2.0F)) < 1.0E-6F,
            "her wider legs meet at the middle as a player's do (0.1 pixel over), never overlapping more");
        check(FemaleBody.legShift() > 0.0F && FemaleBody.ARMS == 0.75F && FemaleBody.HIPS > 1.0F && FemaleBody.THIGHS > 1.0F,
            "wider hips, slim arms (a wide arm drawn three pixels wide)");
        check(FemaleBody.BUST_TOP < FemaleBody.BUST_TIP && FemaleBody.BUST_TIP < FemaleBody.BUST_BOTTOM && FemaleBody.BUST_BOTTOM <= 12.0F
            && FemaleBody.BUST_OUT < FemaleBody.BUST_FRONT && FemaleBody.BUST_FRONT == -2.0F, "the bust sits on the torso's front, within it top to bottom");
        double[] state = {0.0, 0.0};
        FemaleBody.jiggle(state, 0.4, 1.0);
        double kicked = Math.abs(state[0]);
        for (int t = 0; t < 200; t++) {
            FemaleBody.jiggle(state, 0.0, 1.0);
        }
        check(kicked > 0.0 && kicked <= FemaleBody.JIGGLE_MOST + 1.0E-9 && Math.abs(state[0]) < 1.0E-3, "her bounce kicks, never past its most, and settles");
        double hers = (0.34 + 0.75 * Math.sin(com.hex.kagune.control.npc.EtoHand.ARM)) * EtoRules.SIZE;
        check(Math.abs(com.hex.kagune.control.npc.EtoHand.out(true) - 2.0 * hers) < 1.0E-9 && com.hex.kagune.control.npc.EtoHand.out(true)
            < com.hex.kagune.control.npc.EtoHand.out(false), "a woman holding Eto's hand: both hang a woman's slim arm out, so they stand closer");
        FemaleBody.jiggle(state, 30.0, 1.0);
        check(state[0] == 0.0 && state[1] == 0.0, "a teleport starts it still");
    }

    static void screen() {
        for (int[] size : new int[][] {{320, 200}, {427, 240}, {480, 270}, {640, 360}, {960, 540}, {1280, 720}}) {
            GenderView.Layout layout = new GenderView.Layout(size[0], size[1]);
            String at = " at " + size[0] + "x" + size[1];
            Box[] boxes = {layout.male, layout.female, layout.done, layout.back};
            boolean inside = true;
            for (Box box : boxes) {
                inside &= box.x() >= 0 && box.y() >= 0 && box.right() <= size[0] && box.bottom() <= size[1] && box.width() > 0 && box.height() > 0;
            }
            check(inside, "everything on the gender screen fits" + at);
            check(layout.male.right() < layout.female.x() && layout.male.bottom() < layout.done.y() && layout.done.right() < layout.back.x(),
                "the cards side by side above the buttons, nothing overlapping" + at);
            check("male".equals(layout.at(layout.male.centerX(), layout.male.y() + 5)) && "female".equals(layout.at(layout.female.centerX(), layout.female.y() + 5))
                && "done".equals(layout.at(layout.done.centerX(), layout.done.y() + 5)) && "back".equals(layout.at(layout.back.centerX(), layout.back.y() + 5))
                && layout.at(1, 1) == null, "a click lands on what is under it" + at);
            check(layout.hintY < 0 || layout.hintY >= layout.done.bottom(), "the hint sits under the buttons" + at);
        }
    }

    static void pregnancy() {
        com.hex.kagune.control.family.Pregnancy.Stage early = com.hex.kagune.control.family.Pregnancy.stage(0.1);
        long term = com.hex.kagune.control.family.Pregnancy.TERM;
        check(term == 48000L, "two Minecraft days from the night to the birth");
        check(com.hex.kagune.control.family.Pregnancy.progress(1000L, 1000L) == 0.0 && Math.abs(com.hex.kagune.control.family.Pregnancy.progress(1000L,
            1000L + term / 2) - 0.5) < 1.0E-9 && com.hex.kagune.control.family.Pregnancy.progress(1000L, 1000L + term * 3) == 1.0
            && com.hex.kagune.control.family.Pregnancy.progress(Long.MIN_VALUE, 5L) == 0.0, "how far along, from the night she conceived");
        check(!com.hex.kagune.control.family.Pregnancy.due(0L, term - 1) && com.hex.kagune.control.family.Pregnancy.due(0L, term)
            && !com.hex.kagune.control.family.Pregnancy.due(Long.MIN_VALUE, Long.MAX_VALUE), "due at two days, not a tick before");
        boolean growing = true;
        float last = -1.0F;
        for (double p = 0.0; p <= 1.0; p += 0.01) {
            float belly = com.hex.kagune.control.family.Pregnancy.belly(p);
            growing &= belly >= last && belly >= 0.0F && belly <= 1.0F;
            last = belly;
        }
        check(growing && com.hex.kagune.control.family.Pregnancy.belly(0.15) == 0.0F && com.hex.kagune.control.family.Pregnancy.belly(1.0) == 1.0F
            && com.hex.kagune.control.family.Pregnancy.belly(0.6) > 0.3F, "her belly: nothing at first, then a gentle swell, fullest at the end");
        check(com.hex.kagune.control.family.Pregnancy.BELLY_OUT > 0.5F && com.hex.kagune.control.family.Pregnancy.BELLY_OUT < 2.5F,
            "a visible swell, a little, as asked");
        check(early == com.hex.kagune.control.family.Pregnancy.Stage.EARLY && com.hex.kagune.control.family.Pregnancy.stage(0.5)
            == com.hex.kagune.control.family.Pregnancy.Stage.ALONG && com.hex.kagune.control.family.Pregnancy.stage(0.9)
            == com.hex.kagune.control.family.Pregnancy.Stage.LATE && com.hex.kagune.control.family.Pregnancy.stage(1.0)
            == com.hex.kagune.control.family.Pregnancy.Stage.DUE, "early, along, late, due");
        int all = 0;
        for (double p = 0.0; p < 1.0; p += 0.013) {
            int bits = com.hex.kagune.control.family.Pregnancy.crossed(p, p + 0.013);
            check((bits & all) == 0, "each alert crossed once");
            all |= bits;
        }
        check(all == (1 << com.hex.kagune.control.family.Pregnancy.ALERTS.length) - 1, "every alert is crossed on the way");
        for (int i = 0; i < com.hex.kagune.control.family.Pregnancy.ALERTS.length; i++) {
            check(!com.hex.kagune.control.family.FamilyLines.alert(i, 3).isEmpty(), "a word to her partner at each alert");
        }
        String born = com.hex.kagune.control.family.FamilyLines.born("Eto", true, "Hana");
        check(born.contains("A girl") && born.contains("Hana") && born.contains("Eto"), "the birth's words: who, and to whom");
    }

    /**
     * 1.3.43: a ghoul child inherits its parents' kagune: the kinds (the same from both, whole; two kinds, both, the wings down to one;
     * a human parent, the other's), its colour from one parent who has one; saved with its genome (one from before reads as not known and
     * keeps everything else); its fight's rules (tails close, the wing further off, one wing's shots from its side); the wing drawn alone.
     */
    static void kagune() {
        int tails = com.hex.kagune.control.family.KaguneGenes.TAILS;
        int wings = com.hex.kagune.control.family.KaguneGenes.WINGS;
        int wing = com.hex.kagune.control.family.KaguneGenes.WING;
        int owl = com.hex.kagune.control.family.KaguneGenes.OWL;
        int none = com.hex.kagune.control.family.KaguneGenes.NONE;
        java.util.function.IntBinaryOperator inherit = com.hex.kagune.control.family.KaguneGenes::inherit;
        check(inherit.applyAsInt(wings, tails) == (tails | wing) && inherit.applyAsInt(tails, wings) == (tails | wing),
            "a Rinkaku father and an Ukaku mother: the tails and one wing (either way round)");
        check(inherit.applyAsInt(wings, wings) == wings && inherit.applyAsInt(tails, tails) == tails && inherit.applyAsInt(owl, owl) == owl,
            "two of the same kind: that kind whole (the pair of wings)");
        check(inherit.applyAsInt(none, wings) == wings && inherit.applyAsInt(tails, none) == tails && inherit.applyAsInt(none, none) == none
            && inherit.applyAsInt(com.hex.kagune.control.family.KaguneGenes.UNKNOWN, owl) == owl, "a parent with none: the other's whole");
        check(inherit.applyAsInt(owl, tails) == (owl | tails) && inherit.applyAsInt(owl, wings) == (owl | wing), "Eto's child: her kagune and its father's (a wing alone)");
        check(com.hex.kagune.control.family.KaguneGenes.wings(tails | wing) == 1 && com.hex.kagune.control.family.KaguneGenes.wings(wings) == 2
            && com.hex.kagune.control.family.KaguneGenes.wings(tails) == 0 && com.hex.kagune.control.family.KaguneGenes.tails(tails | wing)
            && !com.hex.kagune.control.family.KaguneGenes.tails(com.hex.kagune.control.family.KaguneGenes.UNKNOWN)
            && com.hex.kagune.control.family.KaguneGenes.clean(com.hex.kagune.control.family.KaguneGenes.PAIR) == wings, "how many wings, tails or not");
        check(com.hex.kagune.control.family.KaguneGenes.ofPath("ukaku", true) == wings && com.hex.kagune.control.family.KaguneGenes.ofPath("rinkaku", true) == tails
            && com.hex.kagune.control.family.KaguneGenes.ofPath("none", true) == tails && com.hex.kagune.control.family.KaguneGenes.ofPath("ukaku", false) == none,
            "a player passes on their path's kagune; a human none");
        check(com.hex.kagune.control.npc.Persona.TOUKA.kagune() == wings && com.hex.kagune.control.npc.Persona.KEN.kagune() == tails
            && com.hex.kagune.control.npc.Persona.ETO.kagune() == owl, "Touka passes on her wings, Ken his tails, Eto her Owl kagune");
        check(com.hex.kagune.control.family.KaguneGenes.colour(wings, 0x112233, tails, 0x445566, 0.2) == 0x112233
            && com.hex.kagune.control.family.KaguneGenes.colour(wings, 0x112233, tails, 0x445566, 0.8) == 0x445566
            && com.hex.kagune.control.family.KaguneGenes.colour(none, 0x112233, tails, 0x445566, 0.2) == 0x445566
            && com.hex.kagune.control.family.KaguneGenes.colour(none, 0x112233, none, 0x445566, 0.2) == -1, "its colour from one parent, only one who has a kagune");
        check(com.hex.kagune.control.family.KaguneGenes.describe(tails | wing).equals("a Rinkaku tentacle and an Ukaku wing")
            && com.hex.kagune.control.family.KaguneGenes.describe(com.hex.kagune.control.family.KaguneGenes.UNKNOWN).equals("not known yet"), "in words");
        // Conceived: everything decided before stays as it was for the same draws; the kagune and its colour from the parents.
        com.hex.kagune.control.family.Genome.Parent touka = new com.hex.kagune.control.family.Genome.Parent(0x30304A, 0x404060, 0xF0D8C8, true, false, wings, 0xA50F37);
        com.hex.kagune.control.family.Genome.Parent dad = new com.hex.kagune.control.family.Genome.Parent(0x201010, 0x403020, 0xC08060, true, false, tails, 0x2050E0);
        com.hex.kagune.control.family.Genome.Parent toukaOld = new com.hex.kagune.control.family.Genome.Parent(0x30304A, 0x404060, 0xF0D8C8, true, false);
        com.hex.kagune.control.family.Genome.Parent dadOld = new com.hex.kagune.control.family.Genome.Parent(0x201010, 0x403020, 0xC08060, true, false);
        int fromMother = 0;
        int onlyMother = 0;
        int onlyFather = 0;
        int both = 0;
        boolean same = true;
        boolean mixed = true;
        boolean trips = true;
        for (int i = 0; i < 2000; i++) {
            com.hex.kagune.control.family.Genome now = com.hex.kagune.control.family.Genome.of(touka, dad, new java.util.Random(i));
            com.hex.kagune.control.family.Genome before = com.hex.kagune.control.family.Genome.of(toukaOld, dadOld, new java.util.Random(i));
            same &= now.gender() == before.gender() && now.kakugan() == before.kakugan() && now.hair() == before.hair() && now.eyes() == before.eyes()
                && now.skin() == before.skin() && now.seed() == before.seed();
            // 2.0.6: her wings alone in her colour, his tail alone in his, or both (the tail and one wing) in either one's.
            if (now.kagune() == wings) {
                onlyMother++;
                mixed &= now.colour() == 0xA50F37;
            } else if (now.kagune() == tails) {
                onlyFather++;
                mixed &= now.colour() == 0x2050E0;
            } else {
                mixed &= now.kagune() == (tails | wing) && (now.colour() == 0xA50F37 || now.colour() == 0x2050E0);
                fromMother += now.colour() == 0xA50F37 ? 1 : 0;
                both++;
            }
            trips &= now.equals(com.hex.kagune.control.family.Genome.load(now.save()));
        }
        check(same, "deciding the kagune changes nothing else a child of the same night would have been");
        check(mixed, "Touka and a Rinkaku father: her wings, his tail, or the tail and one wing; its colour from whose kind it took");
        check(Math.abs(onlyMother / 2000.0 - 0.4) < 0.04 && Math.abs(onlyFather / 2000.0 - 0.4) < 0.04 && Math.abs(both / 2000.0 - 0.2) < 0.03,
            "2.0.6: two kinds, 40% the mother's, 40% the father's, 20% both: " + onlyMother + "/" + onlyFather + "/" + both);
        check(both > 0 && Math.abs(fromMother / (double)both - 0.5) < 0.12, "with both, either parent's colour, evenly: " + fromMother + " of " + both);
        // 2.0.6: each pairing (a roll low, middle, high): the same kind or a parent with none never changes; two kinds by the roll.
        java.util.function.IntBinaryOperator low = (m, f) -> com.hex.kagune.control.family.KaguneGenes.inherit(m, f, 0.1);
        java.util.function.IntBinaryOperator middle = (m, f) -> com.hex.kagune.control.family.KaguneGenes.inherit(m, f, 0.5);
        java.util.function.IntBinaryOperator high = (m, f) -> com.hex.kagune.control.family.KaguneGenes.inherit(m, f, 0.9);
        check(low.applyAsInt(wings, tails) == wings && middle.applyAsInt(wings, tails) == tails && high.applyAsInt(wings, tails) == (tails | wing),
            "Touka x a Rinkaku player: her wings, his tail, or both");
        check(low.applyAsInt(wings, wings) == wings && middle.applyAsInt(wings, wings) == wings && high.applyAsInt(wings, wings) == wings,
            "Touka x an Ukaku player: always wings (the owner's Ukaku-only children)");
        check(low.applyAsInt(wings, none) == wings && high.applyAsInt(none, wings) == wings, "Touka x a human player: always her wings");
        check(low.applyAsInt(tails, wings) == tails && middle.applyAsInt(tails, wings) == wings && high.applyAsInt(tails, wings) == (tails | wing),
            "Ken x an Ukaku player: his tail, the wings, or both");
        check(low.applyAsInt(owl, tails) == owl && middle.applyAsInt(owl, tails) == tails && high.applyAsInt(owl, tails) == (owl | tails)
            && high.applyAsInt(owl, wings) == (owl | wing), "Eto x a Rinkaku or Ukaku player: hers, theirs, or both");
        check(com.hex.kagune.control.family.KaguneGenes.colourOf(wings, wings, 0x111111, tails, 0x222222, 0x222222) == 0x111111
            && com.hex.kagune.control.family.KaguneGenes.colourOf(tails, wings, 0x111111, tails, 0x222222, 0x111111) == 0x222222
            && com.hex.kagune.control.family.KaguneGenes.colourOf(tails | wing, wings, 0x111111, tails, 0x222222, 0x222222) == 0x222222
            && com.hex.kagune.control.family.KaguneGenes.colourOf(wings, wings, 0x111111, wings, 0x222222, 0x222222) == 0x222222,
            "one kind alone: that parent's colour; else the one rolled");
        check(trips, "the kagune saved and read back with the rest");
        com.hex.kagune.control.family.Genome old = com.hex.kagune.control.family.Genome.load("2,1,3,5a3a22,4a3020,d9a07e,12345");
        check(old != null && !old.kaguneKnown() && old.seed() == 12345L && old.hair() == 0x5A3A22 && old.colour() == -1
            && old.withKagune(tails, 0x123456).kaguneKnown() && old.withKagune(tails, 0x123456).seed() == 12345L,
            "a genome saved before reads as before, its kagune not known yet (worked out later), nothing else lost");
        check(com.hex.kagune.control.family.Genome.load("2,1,3,5a3a22,4a3020,d9a07e,12345,3") == null, "a broken one isn't read");
        com.hex.kagune.control.family.Genome humanChild = com.hex.kagune.control.family.Genome.of(new com.hex.kagune.control.family.Genome.Parent(0, 0, 0, false, false),
            new com.hex.kagune.control.family.Genome.Parent(0, 0, 0, false, false), new java.util.Random(3));
        check(!humanChild.ghoul() && humanChild.kagune() == none && humanChild.colour() == -1, "a human child: no kagune");
        // Its fight.
        double reach = 3.0;
        check(com.hex.kagune.control.family.ChildKaguneRules.use(tails | wing, 2.0, true, reach) == com.hex.kagune.control.family.ChildKaguneRules.Use.TAILS
            && com.hex.kagune.control.family.ChildKaguneRules.use(tails | wing, 8.0, true, reach) == com.hex.kagune.control.family.ChildKaguneRules.Use.WINGS
            && com.hex.kagune.control.family.ChildKaguneRules.use(tails | wing, 3.5, true, reach) == com.hex.kagune.control.family.ChildKaguneRules.Use.CLOSE_IN
            && com.hex.kagune.control.family.ChildKaguneRules.use(tails, 8.0, true, reach) == com.hex.kagune.control.family.ChildKaguneRules.Use.CLOSE_IN
            && com.hex.kagune.control.family.ChildKaguneRules.use(wings, 1.0, true, reach) == com.hex.kagune.control.family.ChildKaguneRules.Use.WINGS
            && com.hex.kagune.control.family.ChildKaguneRules.use(wings, 1.0, false, reach) == com.hex.kagune.control.family.ChildKaguneRules.Use.CLOSE_IN
            && com.hex.kagune.control.family.ChildKaguneRules.use(owl, 1.0, true, reach) == com.hex.kagune.control.family.ChildKaguneRules.Use.NONE,
            "tails and a wing: the tails up close, the wing from further off, a step in between; tails alone close in; wings alone shoot");
        boolean oneSide = true;
        int[] layouts = new int[3];
        for (long seed = 0; seed < 4000; seed++) {
            long sd = seed * 977L + 13L;
            com.hex.kagune.control.family.ChildKaguneRules.Layout layout = com.hex.kagune.control.family.ChildKaguneRules.layout(tails | wing, sd);
            layouts[layout.ordinal()]++;
            if (layout.side != 0) {
                oneSide &= com.hex.kagune.control.family.ChildKaguneRules.wingCount(tails | wing, sd) == 1
                    && com.hex.kagune.control.family.ChildKaguneRules.side(tails | wing, sd, 1) == -layout.side
                    && com.hex.kagune.control.family.ChildKaguneRules.side(tails | wing, sd, -1) == -layout.side
                    && com.hex.kagune.control.family.ChildKaguneRules.wingSide(tails | wing, sd) == -layout.side;
            } else {
                oneSide &= com.hex.kagune.control.family.ChildKaguneRules.wingCount(tails | wing, sd) == 2
                    && com.hex.kagune.control.family.ChildKaguneRules.side(tails | wing, sd, 1) == -1
                    && com.hex.kagune.control.family.ChildKaguneRules.side(tails | wing, sd, -1) == 1;
            }
            int side = com.hex.kagune.control.family.KaguneGenes.side(sd);
            oneSide &= com.hex.kagune.control.family.ChildKaguneRules.side(owl | wing, sd, 1) == side
                && com.hex.kagune.control.family.ChildKaguneRules.wingCount(owl | wing, sd) == 1
                && com.hex.kagune.control.family.ChildKaguneRules.wingCount(wings, sd) == 2
                && com.hex.kagune.control.family.ChildKaguneRules.wingCount(tails, sd) == 0
                && com.hex.kagune.control.family.ChildKaguneRules.layout(tails | wing, sd) == layout;
        }
        check(oneSide, "1.3.46: over a shoulder, one wing on the other side, every shot from there; as a tail, both wings, taking turns; "
            + "no tentacle, its seed's side; the same every time");
        check(Math.abs(layouts[0] / 4000.0 - com.hex.kagune.control.family.ChildKaguneRules.RIGHT_SHARE) < 0.04
            && Math.abs(layouts[2] / 4000.0 - com.hex.kagune.control.family.ChildKaguneRules.TAIL_SHARE) < 0.04 && layouts[1] > 700,
            "1.3.46: born over the right shoulder, the left, or with a tail, in their shares: " + java.util.Arrays.toString(layouts));
        boolean tailRests = true;
        for (double time = 0.0; time < 400.0; time += 17.0) {
            double[][] rest = com.hex.kagune.control.family.ChildKaguneRules.tentacleRest(com.hex.kagune.control.graft.GraftStage.TRANSPLANT.length(0), time, 5, 12,
                com.hex.kagune.control.family.ChildKaguneRules.Layout.TAIL);
            double[] tip = rest[rest.length - 1];
            for (double[] p : rest) {
                tailRests &= p[1] < 1.9 && p[2] < -0.1 && Math.abs(p[0]) < 0.6; // never over a shoulder: behind it, under its shoulders, about the middle
            }
            tailRests &= Math.abs(rest[0][0]) < 1.0E-9 && tip[2] < rest[0][2] - 1.5 && tip[1] > rest[0][1] - 0.4;
        }
        check(tailRests, "1.3.46: as a tail it stays in the back: out behind, low, from the middle, swaying side to side");
        double[][] left = com.hex.kagune.control.family.ChildKaguneRules.tentacleRest(3.9, 40.0, 5, 12, com.hex.kagune.control.family.ChildKaguneRules.Layout.SHOULDER_LEFT);
        double[][] right = com.hex.kagune.control.family.ChildKaguneRules.tentacleRest(3.9, 40.0, 5, 12, com.hex.kagune.control.family.ChildKaguneRules.Layout.SHOULDER_RIGHT);
        check(left[0][0] < -0.05 && left[11][0] < -0.4 && right[0][0] > 0.05 && right[11][0] > 0.4, "1.3.46: over the left shoulder, the mirror of the right");
        check(com.hex.kagune.control.family.ChildKaguneRules.describe(tails | wing, 0L).startsWith("a Rinkaku tentacle ")
            && com.hex.kagune.control.family.ChildKaguneRules.describe(wings, 0L).equals("Ukaku wings"), "1.3.46: what it carries, in words");
        check(com.hex.kagune.control.family.ChildKaguneRules.side(wings, 5L, 1) == -1 && com.hex.kagune.control.family.ChildKaguneRules.side(wings, 5L, -1) == 1,
            "the pair: the wings take turns");
check(com.hex.kagune.control.graft.GraftStage.TRANSPLANT.limbs(com.hex.kagune.control.family.ChildKaguneRules.tailMastery(com.hex.kagune.control.family.ChildStage.CHILD)) == 1
            && com.hex.kagune.control.graft.GraftStage.TRANSPLANT.limbs(com.hex.kagune.control.family.ChildKaguneRules.tailMastery(com.hex.kagune.control.family.ChildStage.TEEN)) == 1
            && com.hex.kagune.control.family.ChildKaguneRules.TENTACLES == 1, "1.3.44 (the owner): one tentacle, not two, a child's and a teen's");
        // 1.3.44: at rest, a Rinkaku player's first tentacle's curl: from the pelvis, up behind the back, over the head; it sways, a little.
        double length = com.hex.kagune.control.graft.GraftStage.TRANSPLANT.length(0);
        boolean rests = true;
        for (double time = 0.0; time < 400.0; time += 13.0) {
            double[][] rest = com.hex.kagune.control.family.ChildKaguneRules.tentacleRest(length, time, 77, 12);
            double[] root = rest[0];
            double[] tip = rest[rest.length - 1];
            double lowestBack = 0.0;
            for (double[] p : rest) {
                lowestBack = Math.min(lowestBack, p[2]);
            }
            double run = 0.0;
            for (int i = 1; i < rest.length; i++) {
                run += Math.sqrt(Math.pow(rest[i][0] - rest[i - 1][0], 2) + Math.pow(rest[i][1] - rest[i - 1][1], 2) + Math.pow(rest[i][2] - rest[i - 1][2], 2));
            }
            rests &= rest.length == 12 && Math.abs(root[0] - com.hex.kagune.control.family.ChildKaguneRules.TENTACLE_ROOT[0]) < 1.0E-9
                && Math.abs(root[1] - com.hex.kagune.control.family.ChildKaguneRules.TENTACLE_ROOT[1]) < 1.0E-9 && tip[1] > 1.8 && lowestBack < -0.4
                && tip[0] > 0.5 && run > length * 0.6 && run < length * 1.3 && root[0] > 0.05;
            for (double[] p : rest) {
                rests &= p[0] > -0.05; // 1.3.45: never across to its left (the wing's side)
            }
        }
        check(rests, "at rest: from its pelvis (1.3.45: on its right), back and up over its right shoulder, about as long as it is, swaying a little");
        check(com.hex.kagune.control.KaguneColours.veinOf(com.hex.kagune.control.KaguneColours.DEFAULT) == com.hex.kagune.control.KaguneColours.DEFAULT_VEIN
            && com.hex.kagune.control.KaguneColours.veinOf(com.hex.kagune.control.KaguneColours.BONE) == com.hex.kagune.control.KaguneColours.BONE_VEIN
            && Math.abs(com.hex.kagune.control.KaguneColours.toHsv(com.hex.kagune.control.KaguneColours.veinOf(0x2E6BD8))[0]
                - com.hex.kagune.control.KaguneColours.toHsv(0x2E6BD8)[0]) < 0.02
            && com.hex.kagune.control.KaguneColours.toHsv(com.hex.kagune.control.KaguneColours.veinOf(0x2E6BD8))[2] < 0.2,
            "its tentacle's veins: the crimson's own, the bone's ash, else its colour's hue, much darker");
        check(com.hex.kagune.control.family.ChildKaguneRules.scale(com.hex.kagune.control.family.ChildStage.CHILD) == com.hex.kagune.control.family.ChildStage.CHILD.draw
            && com.hex.kagune.control.family.ChildKaguneRules.damage(com.hex.kagune.control.family.ChildStage.TEEN, 3, 0.5F)
                < com.hex.kagune.control.family.ChildRules.damage(com.hex.kagune.control.family.ChildStage.TEEN, 3, true, 0.5F),
            "its kagune at its size, a lash lighter than a grown ghoul's against its punch");
        com.hex.kagune.control.ukaku.WingRig.Input one = com.hex.kagune.control.npc.WingRules.child(110, 1, -1, 1.0F, 0.0, false, 0.0F,
            com.hex.kagune.control.ukaku.WingRig.FORWARD, 0.0F, 0.0F, 1.0F);
        com.hex.kagune.control.ukaku.WingRig.Input two = com.hex.kagune.control.npc.WingRules.child(110, 2, -1, 1.0F, 0.0, false, 0.0F,
            com.hex.kagune.control.ukaku.WingRig.FORWARD, 0.0F, 0.0F, 1.0F);
        check(one.growLeft() == 1.0F && one.growRight() == 0.0F && two.growLeft() == 1.0F && two.growRight() == 1.0F
            && !com.hex.kagune.control.ukaku.WingRig.build(one, new double[] {0.0, 1.5, 4.0}).isEmpty(), "one wing drawn on its side alone; the pair both");
    }

    /** 1.3.47: a child's health from its parents': the stronger's, more for the other's strength, by stage; it heals on its own. */
    static void vitality() {
        float grown = com.hex.kagune.control.family.ChildRules.grownHealth(360.0F, 0.5F, 20.0F, com.hex.kagune.control.family.ChildRules.playerMight(true, 1000, false));
        check(Math.abs(grown - 400.0F) < 2.0F, "the owner's own sum: a mother of 360 and a father at full mastery: about 400 (" + grown + ")");
        check(com.hex.kagune.control.family.ChildRules.grownHealth(20.0F, 0.0F, 340.0F, 1.0F) == 340.0F
            && com.hex.kagune.control.family.ChildRules.grownHealth(260.0F, 0.0F, 20.0F, 0.15F) > 260.0F
            && com.hex.kagune.control.family.ChildRules.grownHealth(260.0F, 0.0F, 20.0F, 1.0F) <= 260.0F * 1.1401F,
            "the stronger parent's health, never less; the other's strength adds at most 14%");
        float last = 0.0F;
        boolean rising = true;
        for (com.hex.kagune.control.family.ChildStage stage : com.hex.kagune.control.family.ChildStage.values()) {
            float h = com.hex.kagune.control.family.ChildRules.health(stage, 0.5F, 400.0F);
            // 2.0.3: a grown-up keeps a teen's (all of it).
            rising &= (stage == com.hex.kagune.control.family.ChildStage.ADULT ? h == last : h > last)
                && h >= com.hex.kagune.control.family.ChildRules.health(stage, 0.5F);
            last = h;
        }
        check(rising && com.hex.kagune.control.family.ChildRules.health(com.hex.kagune.control.family.ChildStage.TEEN, 0.5F, 400.0F) == 400.0F
            && com.hex.kagune.control.family.ChildRules.health(com.hex.kagune.control.family.ChildStage.BABY, 0.5F, 0.0F)
                == com.hex.kagune.control.family.ChildRules.health(com.hex.kagune.control.family.ChildStage.BABY, 0.5F),
            "it grows into it, all of it as a teen; not known yet, as before");
        check(com.hex.kagune.control.family.ChildRules.healing(240.0F, true) == 2.0F
            && com.hex.kagune.control.family.ChildRules.healing(240.0F, false) < 2.0F, "a ghoul child heals itself in two minutes, a human slower");
    }

    /** 1.3.48: the rest of its parents' kit: flips, a grab, a fan of crystal, as before the lashes and shots; never a kakuja. */
    static void kit() {
        int tails = com.hex.kagune.control.family.KaguneGenes.TAILS;
        int wing = com.hex.kagune.control.family.KaguneGenes.WING;
        int wings = com.hex.kagune.control.family.KaguneGenes.WINGS;
        java.util.function.Function<Object[], com.hex.kagune.control.family.ChildKaguneRules.Act> act = a -> com.hex.kagune.control.family.ChildKaguneRules.choose(
            (Integer)a[0], (Double)a[1], true, 3.0, (Boolean)a[2], (Boolean)a[3], (Boolean)a[4], 1000L, (Long)a[5], (Long)a[6], (Long)a[7], (Double)a[8]);
        long ready = 0L;
        long later = 5000L;
        check(act.apply(new Object[] {tails | wing, 1.0, false, true, true, ready, ready, ready, 0.3}) == com.hex.kagune.control.family.ChildKaguneRules.Act.BACK_FLIP,
            "right on top of it: a back flip out");
        check(act.apply(new Object[] {tails | wing, 1.0, false, true, false, ready, later, ready, 0.3}) != com.hex.kagune.control.family.ChildKaguneRules.Act.BACK_FLIP,
            "never a flip from the air");
        check(act.apply(new Object[] {tails, 8.0, false, true, true, ready, ready, ready, 0.2}) == com.hex.kagune.control.family.ChildKaguneRules.Act.FRONT_FLIP,
            "far off with a tentacle: a front flip in");
        check(act.apply(new Object[] {tails, 2.0, false, true, true, ready, ready, ready, 0.3}) == com.hex.kagune.control.family.ChildKaguneRules.Act.GRAB
            && act.apply(new Object[] {tails, 2.0, false, false, true, ready, ready, ready, 0.3}) == com.hex.kagune.control.family.ChildKaguneRules.Act.LASH
            && act.apply(new Object[] {tails, 2.0, false, true, true, ready, later, ready, 0.3}) == com.hex.kagune.control.family.ChildKaguneRules.Act.LASH,
            "in reach: a grab if it's small enough and the grab is back, else a lash");
        check(act.apply(new Object[] {wings, 6.0, true, false, true, later, ready, ready, 0.3}) == com.hex.kagune.control.family.ChildKaguneRules.Act.FAN
            && act.apply(new Object[] {wings, 6.0, false, false, true, later, ready, ready, 0.3}) == com.hex.kagune.control.family.ChildKaguneRules.Act.SHOOT
            && act.apply(new Object[] {wings, 6.0, true, false, true, later, ready, later, 0.3}) == com.hex.kagune.control.family.ChildKaguneRules.Act.SHOOT,
            "crystal wings: a fan when it's back; before they turn, or between fans, shots");
        check(act.apply(new Object[] {0, 2.0, true, true, true, ready, ready, ready, 0.3}) == com.hex.kagune.control.family.ChildKaguneRules.Act.NONE,
            "no kagune: nothing of it");
        boolean noKakuja = true;
        for (com.hex.kagune.control.family.ChildKaguneRules.Act a : com.hex.kagune.control.family.ChildKaguneRules.Act.values()) {
            noKakuja &= !a.name().contains("KAKUJA") && !a.name().contains("OWL");
        }
        check(noKakuja, "the owner: a child never has a kakuja");
        check(com.hex.kagune.control.family.ChildKaguneRules.fan(0) == -com.hex.kagune.control.family.ChildKaguneRules.FAN_SPREAD
            && com.hex.kagune.control.family.ChildKaguneRules.fan(com.hex.kagune.control.family.ChildKaguneRules.FAN_SPIKES - 1)
                == com.hex.kagune.control.family.ChildKaguneRules.FAN_SPREAD
            && com.hex.kagune.control.family.ChildKaguneRules.fan(com.hex.kagune.control.family.ChildKaguneRules.FAN_SPIKES / 2) == 0.0, "a fan, even round its aim");
        check(com.hex.kagune.control.family.ChildKaguneRules.grabbable(0.6, 1.95) && !com.hex.kagune.control.family.ChildKaguneRules.grabbable(1.4, 0.9)
            && !com.hex.kagune.control.family.ChildKaguneRules.grabbable(1.4, 2.7), "it grabs a zombie, not a spider or a golem");
        check(com.hex.kagune.control.family.ChildKaguneRules.throwsIt(0.1) && !com.hex.kagune.control.family.ChildKaguneRules.throwsIt(0.9),
            "a grab ends thrown, or crushed");
        com.hex.kagune.control.ukaku.WingRig.Input glass = com.hex.kagune.control.npc.WingRules.child(110, 2, 1, 1.0F, 0.0, false, 0.0F,
            com.hex.kagune.control.ukaku.WingRig.FORWARD, 0.0F, 0.0F, 1.0F, 1.0F);
        check(glass.crystalRight() == 1.0F && glass.crystalLeft() == 1.0F, "its wings drawn in crystal");
    }

    /** 1.3.49: a companion parent after its children: when it is too much, and what they say, back and forth, in hundreds of lines. */
    static void parenting() {
        com.hex.kagune.control.family.ChildStage baby = com.hex.kagune.control.family.ChildStage.BABY;
        com.hex.kagune.control.family.ChildStage toddler = com.hex.kagune.control.family.ChildStage.TODDLER;
        com.hex.kagune.control.family.ChildStage kid = com.hex.kagune.control.family.ChildStage.CHILD;
        com.hex.kagune.control.family.ChildStage teen = com.hex.kagune.control.family.ChildStage.TEEN;
        com.hex.kagune.control.family.Parenting.Why wandered = com.hex.kagune.control.family.Parenting.Why.WANDERED;
        com.hex.kagune.control.family.Parenting.Why night = com.hex.kagune.control.family.Parenting.Why.NIGHT;
        check(com.hex.kagune.control.family.Parenting.strayed(toddler, 6.0, false, false, false) == wandered
            && com.hex.kagune.control.family.Parenting.strayed(toddler, 2.0, false, false, false) == null
            && com.hex.kagune.control.family.Parenting.strayed(toddler, 0.5, true, false, false) == night
            && com.hex.kagune.control.family.Parenting.strayed(kid, 10.0, false, false, false) == null
            && com.hex.kagune.control.family.Parenting.strayed(kid, 20.0, false, false, false) == wandered
            && com.hex.kagune.control.family.Parenting.strayed(kid, 5.0, true, false, false) == night
            && com.hex.kagune.control.family.Parenting.strayed(teen, 30.0, false, false, false) == null
            && com.hex.kagune.control.family.Parenting.strayed(teen, 30.0, true, false, false) == night
            && com.hex.kagune.control.family.Parenting.strayed(teen, -3.0, true, false, false) == null,
            "a toddler may hardly leave home, a teen may roam; at night all of them less; at home, never");
        check(com.hex.kagune.control.family.Parenting.strayed(kid, 50.0, true, true, false) == null
            && com.hex.kagune.control.family.Parenting.strayed(toddler, 50.0, true, false, true) == null
            && com.hex.kagune.control.family.Parenting.strayed(baby, 50.0, true, false, false) == null,
            "never one that follows a player, is carried, or is a baby");
        check(!com.hex.kagune.control.family.Parenting.rampage(kid, 300L, 5.0, 0.8F) && com.hex.kagune.control.family.Parenting.rampage(kid, 700L, 5.0, 0.8F)
            && com.hex.kagune.control.family.Parenting.rampage(kid, 100L, 25.0, 0.8F) && com.hex.kagune.control.family.Parenting.rampage(kid, 100L, 0.0, 0.2F)
            && !com.hex.kagune.control.family.Parenting.rampage(teen, 900L, 25.0, 0.5F) && com.hex.kagune.control.family.Parenting.rampage(teen, 1300L, 0.0, 0.9F)
            && !com.hex.kagune.control.family.Parenting.rampage(toddler, 99999L, 99.0, 0.01F),
            "too long, too far, too hurt: a rampage (a teen has more rope); a toddler never fights");
        check(com.hex.kagune.control.family.Parenting.notices(20.0, true, 0L) && !com.hex.kagune.control.family.Parenting.notices(20.0, false, 100L)
            && com.hex.kagune.control.family.Parenting.notices(90.0, false, com.hex.kagune.control.family.Parenting.MISSED_AFTER)
            && !com.hex.kagune.control.family.Parenting.notices(60.0, true, 0L), "a parent sees it near, or misses it after a while");
        check(com.hex.kagune.control.family.Parenting.pause("") >= 40 && com.hex.kagune.control.family.Parenting.pause("x".repeat(500)) <= 140
            && com.hex.kagune.control.family.Parenting.pause("x".repeat(40)) > com.hex.kagune.control.family.Parenting.pause("x"), "long enough to read");
        // The words: every parent, every temper, every why, every age has its lines; none names anyone it mustn't.
        String[] parents = {"touka", "ken", "eto"};
        int n = 0;
        java.util.Set<String> seen = new java.util.HashSet<>();
        boolean shapes = true;
        boolean clean = true;
        for (String who : parents) {
            for (com.hex.kagune.control.family.ChildTemper temper : com.hex.kagune.control.family.ChildTemper.values()) {
                for (com.hex.kagune.control.family.Parenting.Why why : com.hex.kagune.control.family.Parenting.Why.values()) {
                    for (com.hex.kagune.control.family.ChildStage stage : new com.hex.kagune.control.family.ChildStage[] {toddler, kid, teen}) {
                        boolean fights = stage.learns();
                        if ((why == com.hex.kagune.control.family.Parenting.Why.RAMPAGE || why == com.hex.kagune.control.family.Parenting.Why.TRANSFORM) && !fights) {
                            continue;
                        }
                        for (int seed = 0; seed < 6; seed++) {
                            java.util.List<com.hex.kagune.control.family.Parenting.Line> talk = com.hex.kagune.control.family.Parenting.talk(why, who, stage, temper,
                                "Hana", "Mama", seed * 31 + 7);
                            int want = why == com.hex.kagune.control.family.Parenting.Why.TRANSFORM ? 2 : why == com.hex.kagune.control.family.Parenting.Why.HOME ? 2 : 5;
                            shapes &= talk.size() >= want && talk.size() <= want + 1 && talk.get(0).parent() && !talk.get(1).parent();
                            if (talk.size() >= 5) {
                                shapes &= talk.get(2).parent() && !talk.get(3).parent() && talk.get(4).parent();
                            }
                            for (com.hex.kagune.control.family.Parenting.Line line : talk) {
                                String low = line.text().toLowerCase(java.util.Locale.ROOT);
                                clean &= !line.text().contains("{") && !line.text().isBlank();
                                for (String banned : new String[] {"kaneki", "ayato", "furuta", "takatsuki", "aogiri", "yoshimura"}) {
                                    clean &= !low.contains(banned);
                                }
                                seen.add(line.text());
                                n++;
                            }
                        }
                    }
                }
            }
        }
        check(shapes, "back and forth: the parent first, then the child, the parent, the child, the parent's last word");
        check(clean, "every line filled in (the name, what it calls them), and no names it mustn't");
        check(seen.size() > 450, "hundreds of different lines heard: " + seen.size() + " in " + n + " talks' lines");
        java.util.List<com.hex.kagune.control.family.Parenting.Line> one = com.hex.kagune.control.family.Parenting.talk(wandered, "touka", kid,
            com.hex.kagune.control.family.ChildTemper.BOLD, "Hana", "Mom", 3);
        check(one.equals(com.hex.kagune.control.family.Parenting.talk(wandered, "touka", kid, com.hex.kagune.control.family.ChildTemper.BOLD, "Hana", "Mom", 3))
            && !one.equals(com.hex.kagune.control.family.Parenting.talk(wandered, "touka", kid, com.hex.kagune.control.family.ChildTemper.BOLD, "Hana", "Mom", 4)),
            "the same seed, the same talk; another, another");
    }

    /** 1.3.50: Eto's child: her flesh's rake, grab and club, and her Mutated Arm when it is badly hurt; never the Owl. */
    static void etoChild() {
        com.hex.kagune.control.family.ChildKaguneRules.Act arm = com.hex.kagune.control.family.ChildKaguneRules.Act.ARM;
        java.util.function.Function<Object[], com.hex.kagune.control.family.ChildKaguneRules.Act> act = a -> com.hex.kagune.control.family.ChildKaguneRules.chooseFlesh(
            (Double)a[0], true, 0.7, (Boolean)a[1], (Float)a[2], 1000L, (Long)a[3], (Long)a[4], (Long)a[5], (Long)a[6], (Double)a[7]);
        long ready = 0L;
        long later = 9000L;
        check(act.apply(new Object[] {3.0, true, 0.3F, ready, ready, ready, ready, 0.9}) == arm
            && act.apply(new Object[] {3.0, true, 0.3F, ready, ready, ready, later, 0.9}) != arm
            && act.apply(new Object[] {3.0, true, 0.8F, ready, ready, ready, ready, 0.9}) != arm,
            "badly hurt, its arm turns (its transformation), once in a while; not while it is well");
        check(act.apply(new Object[] {2.0, true, 0.9F, ready, ready, ready, ready, 0.9}) == com.hex.kagune.control.family.ChildKaguneRules.Act.SWIPE
            && act.apply(new Object[] {2.0, true, 0.9F, ready, ready, ready, ready, 0.1}) == com.hex.kagune.control.family.ChildKaguneRules.Act.HAMMER
            && act.apply(new Object[] {2.0, true, 0.9F, later, later, later, ready, 0.1}) == com.hex.kagune.control.family.ChildKaguneRules.Act.CLOSE_IN,
            "close: the rake, now and then the club; neither ready, closer");
        check(act.apply(new Object[] {4.5, true, 0.9F, ready, ready, ready, ready, 0.3}) == com.hex.kagune.control.family.ChildKaguneRules.Act.FLESH_GRAB
            && act.apply(new Object[] {4.5, false, 0.9F, ready, ready, ready, ready, 0.3}) == com.hex.kagune.control.family.ChildKaguneRules.Act.CLOSE_IN,
            "further: the flesh's grab, if it's small enough");
        check(com.hex.kagune.control.family.ChildKaguneRules.chooseFlesh(2.0, false, 0.7, true, 0.1F, 1000L, 0L, 0L, 0L, 0L, 0.1)
            == com.hex.kagune.control.family.ChildKaguneRules.Act.CLOSE_IN, "nothing it can't see");
        int owl = com.hex.kagune.control.family.KaguneGenes.OWL;
        check(com.hex.kagune.control.family.KaguneGenes.any(owl) && com.hex.kagune.control.family.ChildKaguneRules.wingCount(owl, 3L) == 0
            && com.hex.kagune.control.family.ChildKaguneRules.describe(owl | com.hex.kagune.control.family.KaguneGenes.TAILS, 3L).endsWith("Eto's flesh"),
            "Eto's child fights with her flesh, with its father's kagune beside it");
    }

    /** 1.3.51: brothers and sisters: which moment they have, and what they say, in turn, by temper and age; a parent breaks up a squabble. */
    static void siblings() {
        com.hex.kagune.control.family.ChildStage baby = com.hex.kagune.control.family.ChildStage.BABY;
        com.hex.kagune.control.family.ChildStage toddler = com.hex.kagune.control.family.ChildStage.TODDLER;
        com.hex.kagune.control.family.ChildStage kid = com.hex.kagune.control.family.ChildStage.CHILD;
        com.hex.kagune.control.family.ChildStage teen = com.hex.kagune.control.family.ChildStage.TEEN;
        com.hex.kagune.control.family.ChildTemper sunny = com.hex.kagune.control.family.ChildTemper.SUNNY;
        com.hex.kagune.control.family.ChildTemper bold = com.hex.kagune.control.family.ChildTemper.BOLD;
        check(com.hex.kagune.control.family.Siblings.pick(baby, sunny, kid, bold, false, false, 0.5) == null
            && com.hex.kagune.control.family.Siblings.pick(kid, sunny, baby, bold, true, true, 0.5) == null, "a baby has no moments of its own");
        check(com.hex.kagune.control.family.Siblings.pick(teen, sunny, toddler, bold, false, true, 0.5) == com.hex.kagune.control.family.Siblings.Kind.MIND
            && com.hex.kagune.control.family.Siblings.pick(toddler, sunny, toddler, bold, false, true, 0.5) != com.hex.kagune.control.family.Siblings.Kind.MIND,
            "an older one goes after a toddler that wandered; a toddler doesn't");
        check(com.hex.kagune.control.family.Siblings.pick(kid, sunny, kid, bold, true, false, 0.9) == com.hex.kagune.control.family.Siblings.Kind.COMFORT,
            "one hurt is comforted");
        java.util.Set<com.hex.kagune.control.family.Siblings.Kind> kinds = java.util.EnumSet.noneOf(com.hex.kagune.control.family.Siblings.Kind.class);
        for (double r = 0.0; r < 1.0; r += 0.01) {
            kinds.add(com.hex.kagune.control.family.Siblings.pick(kid, com.hex.kagune.control.family.ChildTemper.MISCHIEF, teen, bold, false, false, r));
        }
        check(kinds.size() >= 6, "they chat, tease, brag, scheme, squabble and play: " + kinds);
        java.util.Set<String> seen = new java.util.HashSet<>();
        boolean turns = true;
        boolean clean = true;
        for (com.hex.kagune.control.family.Siblings.Kind kind : com.hex.kagune.control.family.Siblings.Kind.values()) {
            for (com.hex.kagune.control.family.ChildTemper ta : com.hex.kagune.control.family.ChildTemper.values()) {
                for (com.hex.kagune.control.family.ChildTemper tb : com.hex.kagune.control.family.ChildTemper.values()) {
                    for (com.hex.kagune.control.family.ChildStage sa : new com.hex.kagune.control.family.ChildStage[] {toddler, kid, teen}) {
                        for (int seed = 0; seed < 3; seed++) {
                            java.util.List<com.hex.kagune.control.family.Siblings.Line> talk = com.hex.kagune.control.family.Siblings.talk(kind, ta, sa, "Hana",
                                "Mama", true, tb, kid, "Ren", "Mama", false, seed * 13 + ta.ordinal() * 7 + tb.ordinal());
                            int[] want = switch (kind) {
                                case REACT, TEAM -> new int[] {1};
                                case TAG -> new int[] {1, 2};
                                case SQUABBLE -> new int[] {1, 2, 1, 2};
                                default -> new int[] {1, 2, 1};
                            };
                            turns &= talk.size() == want.length;
                            for (int i = 0; i < talk.size() && i < want.length; i++) {
                                turns &= talk.get(i).who() == want[i];
                                String text = talk.get(i).text();
                                String low = text.toLowerCase(java.util.Locale.ROOT);
                                clean &= !text.contains("{") && !text.isBlank();
                                for (String banned : new String[] {"kaneki", "ayato", "furuta", "takatsuki", "aogiri", "yoshimura"}) {
                                    clean &= !low.contains(banned);
                                }
                                seen.add(text);
                            }
                        }
                    }
                }
            }
        }
        check(turns, "in turn: the one, the other, the one (a squabble back and forth twice, tag a shout and an answer)");
        check(clean, "every line filled in, no names it mustn't");
        check(seen.size() > 250, "hundreds of different lines: " + seen.size());
        java.util.List<com.hex.kagune.control.family.Siblings.Line> broken = com.hex.kagune.control.family.Siblings.breakUp("touka", sunny, "Hana", bold, "Ren", 5);
        check(broken.size() == 3 && broken.get(0).who() == 0 && broken.get(1).who() == 1 && broken.get(2).who() == 2
            && !broken.get(1).text().contains("{"), "a parent breaks it up, and each says sorry");
        check(com.hex.kagune.control.family.Siblings.tagged(bold, kid, "Ren", 2).text().length() > 2, "a tag's shout");
        // 1.3.54: tag stays in the yard: wherever the runner and the chaser are, it heads somewhere inside, away from the chaser if it can.
        java.util.Random random = new java.util.Random(54);
        boolean inside = true;
        boolean away = true;
        int roomy = 0;
        for (int i = 0; i < 4000; i++) {
            double radius = 6 + random.nextInt(14);
            double limit = com.hex.kagune.control.family.Siblings.yard(radius);
            double angle = random.nextDouble() * Math.PI * 2;
            double r = random.nextDouble() * limit;
            double x = 100 + Math.cos(angle) * r;
            double z = -40 + Math.sin(angle) * r;
            double cx = x + (random.nextDouble() - 0.5) * 6;
            double cz = z + (random.nextDouble() - 0.5) * 6;
            double[] to = com.hex.kagune.control.family.Siblings.runTo(x, z, cx, cz, (random.nextDouble() - 0.5) * 1.4, 100, -40, radius);
            inside &= Math.hypot(to[0] - 100, to[1] + 40) <= limit + 1.0E-6;
            if (r < limit - com.hex.kagune.control.family.Siblings.RUN - 1) {
                roomy++;
                away &= Math.hypot(to[0] - cx, to[1] - cz) > Math.hypot(x - cx, z - cz);
            }
        }
        check(inside, "a runner in tag never heads out of its yard");
        check(away && roomy > 100, "with room, it runs away from whoever is it");
        double[] corner = com.hex.kagune.control.family.Siblings.runTo(100 + 9.4, -40, 100 + 8, -40, 0.0, 100, -40, 12);
        // 1.3.55: a big brother or sister stands down when a parent is already going: every temper, both ages, filled in and clean.
        boolean down = true;
        java.util.Set<String> downSeen = new java.util.HashSet<>();
        for (com.hex.kagune.control.family.ChildTemper t : com.hex.kagune.control.family.ChildTemper.values()) {
            for (com.hex.kagune.control.family.ChildStage st : new com.hex.kagune.control.family.ChildStage[] {kid, teen}) {
                check(com.hex.kagune.control.family.Siblings.standDownLines().containsKey(t.word() + ":" + (st == teen ? "teen" : "child")),
                    "stand-down lines of its own for " + t.word());
                for (int seed = 0; seed < 6; seed++) {
                    String line = com.hex.kagune.control.family.Siblings.standDown(t, st, "Hana", "Mama", seed);
                    String low = line.toLowerCase(java.util.Locale.ROOT);
                    down &= !line.contains("{") && !line.isBlank();
                    for (String banned : new String[] {"kaneki", "ayato", "furuta", "takatsuki", "aogiri", "yoshimura"}) {
                        down &= !low.contains(banned);
                    }
                    downSeen.add(line);
                }
            }
        }
        check(down && downSeen.size() >= 30, "standing down for a parent: every line filled in, no names it mustn't (" + downSeen.size() + ")");
        check(Math.hypot(corner[0] - 100, corner[1] + 40) <= com.hex.kagune.control.family.Siblings.yard(12) + 1.0E-6
            && Math.hypot(corner[0] - 108, corner[1] + 40) > 1.0, "chased to the edge, it turns along it instead of running off");
        java.util.List<com.hex.kagune.control.family.Siblings.Line> team = com.hex.kagune.control.family.Siblings.talk(com.hex.kagune.control.family.Siblings.Kind.TEAM,
            bold, kid, "Hana", "Mama", true, sunny, kid, "Ren", "Mama", false, 1);
        check(team.size() == 1 && !team.get(0).text().contains("{sibrole}"), "fighting side by side: a word for a brother or a sister");
    }

    /**
     * 1.3.53: a talk that holds together: everyone turns to whoever speaks (the speaker to whom it answers or who answers it, never itself),
     * a body turns a little a tick the short way round, only walking off gets a word, and every cut-short line is filled in and clean.
     */
    static void talks() {
        int a = 11;
        int b = 22;
        int p = 33;
        check(com.hex.kagune.control.family.Talks.faces(a, b, 0, a, b, b) == b && com.hex.kagune.control.family.Talks.faces(p, b, a, a, b, a) == b,
            "listening, it faces whoever speaks");
        check(com.hex.kagune.control.family.Talks.faces(a, a, b, b, b, b) == b && com.hex.kagune.control.family.Talks.faces(a, a, p, 0, b, b) == p,
            "speaking, it faces whoever answers it, or whom it answered");
        check(com.hex.kagune.control.family.Talks.faces(b, 0, 0, a, b, a) == a && com.hex.kagune.control.family.Talks.faces(a, 0, 0, a, b, b) == b,
            "before a word: the one who waits faces the one coming to talk, who faces it");
        boolean never = true;
        for (int speaker : new int[] {0, a, b, p}) {
            for (int next : new int[] {0, a, b, p}) {
                for (int previous : new int[] {0, a, b}) {
                    never &= com.hex.kagune.control.family.Talks.faces(a, speaker, previous, next, a, b) != a;
                }
            }
        }
        check(never, "never itself");
        check(com.hex.kagune.control.family.Talks.turn(0.0F, 90.0F, 24.0F) == 24.0F && com.hex.kagune.control.family.Talks.turn(170.0F, -170.0F, 24.0F) == 190.0F
            && com.hex.kagune.control.family.Talks.turn(10.0F, 15.0F, 24.0F) == 15.0F && com.hex.kagune.control.family.Talks.turn(-10.0F, -100.0F, 24.0F) == -34.0F,
            "it turns a little a tick, the short way round");
        check(com.hex.kagune.control.family.Talks.remarked(com.hex.kagune.control.family.Talks.End.LEFT)
            && !com.hex.kagune.control.family.Talks.remarked(com.hex.kagune.control.family.Talks.End.GONE)
            && !com.hex.kagune.control.family.Talks.remarked(com.hex.kagune.control.family.Talks.End.HURT)
            && !com.hex.kagune.control.family.Talks.remarked(com.hex.kagune.control.family.Talks.End.FIGHT)
            && !com.hex.kagune.control.family.Talks.remarked(com.hex.kagune.control.family.Talks.End.INTERRUPTED)
            && !com.hex.kagune.control.family.Talks.remarked(com.hex.kagune.control.family.Talks.End.DONE),
            "only walking off gets a word (never a death, a hurt, a fight, a player)");
        check(com.hex.kagune.control.family.Talks.SPEAKS > com.hex.kagune.control.family.Talks.GATHER && com.hex.kagune.control.family.Talks.LEAVES
            > com.hex.kagune.control.family.Talks.SPEAKS && com.hex.kagune.control.family.Talks.GATHERS >= com.hex.kagune.control.family.Talks.WAITS,
            "they gather closer than a line waits for, and a line waits closer than walking off");
        boolean clean = true;
        int said = 0;
        java.util.Set<String> seen = new java.util.HashSet<>();
        String[] who = {"touka", "ken", "eto", "sunny", "shy", "bold", "curious", "mischief", "gentle", "serious", "unknown"};
        for (String w : who) {
            for (com.hex.kagune.control.family.ChildStage stage : new com.hex.kagune.control.family.ChildStage[] {com.hex.kagune.control.family.ChildStage.TODDLER,
                com.hex.kagune.control.family.ChildStage.CHILD, com.hex.kagune.control.family.ChildStage.TEEN}) {
                for (int seed = 0; seed < 12; seed++) {
                    String line = com.hex.kagune.control.family.Talks.cutShort(w, stage, "Ren", "Mama", seed);
                    if (line == null) {
                        continue;
                    }
                    said++;
                    seen.add(line);
                    String low = line.toLowerCase(java.util.Locale.ROOT);
                    clean &= !line.contains("{") && !line.isBlank();
                    for (String banned : new String[] {"kaneki", "ayato", "furuta", "takatsuki", "aogiri", "yoshimura"}) {
                        clean &= !low.contains(banned);
                    }
                }
            }
        }
        check(clean && said > 300 && seen.size() > 50, "walked off on: every line filled in, no names it mustn't (" + seen.size() + " lines)");
        check(com.hex.kagune.control.family.Talks.cutShort("bold", com.hex.kagune.control.family.ChildStage.TEEN, "Ren", "Mama", 1) != null
            && com.hex.kagune.control.family.Talks.cutShort("ken", com.hex.kagune.control.family.ChildStage.TODDLER, "Ren", "Mama", 0).contains("later"),
            "a teen in its own temper; a parent's line whatever the child's age");
    }

    /**
     * 1.3.57: what each one does as it says its line: what the line says first (folded arms, a giggle, a plea, a sigh, a laugh), else the
     * talk and the speaker's own way; every child's line in every talk and temper gets something fitting, a parent's scolding its gesture.
     */
    static void cues() {
        com.hex.kagune.control.family.ChildTemper bold = com.hex.kagune.control.family.ChildTemper.BOLD;
        com.hex.kagune.control.family.ChildTemper shy = com.hex.kagune.control.family.ChildTemper.SHY;
        com.hex.kagune.control.family.ChildTemper mischief = com.hex.kagune.control.family.ChildTemper.MISCHIEF;
        check(com.hex.kagune.control.family.TalkCues.said("*crosses her arms*") == com.hex.kagune.control.family.ChildCue.CROSS
            && com.hex.kagune.control.family.TalkCues.said("Hehe~ Gotcha.") == com.hex.kagune.control.family.ChildCue.GIGGLE
            && com.hex.kagune.control.family.TalkCues.said("Pleeease, Mama?") == com.hex.kagune.control.family.ChildCue.PLEAD
            && com.hex.kagune.control.family.TalkCues.said("*sticks out his tongue*") == com.hex.kagune.control.family.ChildCue.BLEH
            && com.hex.kagune.control.family.TalkCues.said("I'm sorry...") == com.hex.kagune.control.family.ChildCue.SORRY
            && com.hex.kagune.control.family.TalkCues.said("Psst. Come here.") == com.hex.kagune.control.family.ChildCue.WHISPER
            && com.hex.kagune.control.family.TalkCues.said("I found a frog.") == com.hex.kagune.control.family.ChildCue.NONE,
            "a line that says what it does is done that way");
        com.hex.kagune.control.family.TalkCues.Scene squabble = com.hex.kagune.control.family.TalkCues.Scene.SQUABBLE;
        check(com.hex.kagune.control.family.TalkCues.child("You always do this!", squabble, bold, true, 0, 0) == com.hex.kagune.control.family.ChildCue.POINT
            && com.hex.kagune.control.family.TalkCues.child("No, YOU always do this!", squabble, shy, false, 1, 0) == com.hex.kagune.control.family.ChildCue.SULK,
            "a squabble: a bold one points, a shy one sulks");
        check(com.hex.kagune.control.family.TalkCues.child("I took down a zombie today.", com.hex.kagune.control.family.TalkCues.Scene.BRAG, bold, true, 0, 3)
            == com.hex.kagune.control.family.ChildCue.HIPS && com.hex.kagune.control.family.TalkCues.child("You distract them. I get the cake.",
            com.hex.kagune.control.family.TalkCues.Scene.SCHEME, bold, true, 0, 1) == com.hex.kagune.control.family.ChildCue.WHISPER,
            "a brag on its hips, a scheme whispered");
        boolean bleh = false;
        for (int seed = 0; seed < 8; seed++) {
            bleh |= com.hex.kagune.control.family.TalkCues.child("Sorry, Ren.", com.hex.kagune.control.family.TalkCues.Scene.MAKE_UP, mischief, true, 1, seed)
                == com.hex.kagune.control.family.ChildCue.SORRY && com.hex.kagune.control.family.TalkCues.child("Fine.",
                com.hex.kagune.control.family.TalkCues.Scene.MAKE_UP, mischief, true, 1, seed) == com.hex.kagune.control.family.ChildCue.BLEH;
        }
        check(bleh, "a mischievous one's sorry: sometimes a tongue stuck out instead (a sorry it says is still a sorry)");
        boolean every = true;
        for (com.hex.kagune.control.family.TalkCues.Scene scene : com.hex.kagune.control.family.TalkCues.Scene.values()) {
            for (com.hex.kagune.control.family.ChildTemper t : com.hex.kagune.control.family.ChildTemper.values()) {
                for (int index = 0; index < 5; index++) {
                    for (int seed = 0; seed < 4; seed++) {
                        com.hex.kagune.control.family.ChildCue cue = com.hex.kagune.control.family.TalkCues.child("Well.", scene, t, index % 2 == 0, index, seed);
                        every &= cue != null && (cue != com.hex.kagune.control.family.ChildCue.NONE || scene == com.hex.kagune.control.family.TalkCues.Scene.CHAT
                            || scene == com.hex.kagune.control.family.TalkCues.Scene.HOME || scene == com.hex.kagune.control.family.TalkCues.Scene.BRAG);
                    }
                }
            }
        }
        check(every, "every talk but small talk gives a child something to do with itself");
        check(com.hex.kagune.control.family.TalkCues.parent("Where do you think you're going?", com.hex.kagune.control.family.TalkCues.Scene.SCOLDED,
            "touka", 0, 1) == com.hex.kagune.control.npc.EtoGesture.SCOLD && com.hex.kagune.control.family.TalkCues.parent("*sighs* Come here.",
            com.hex.kagune.control.family.TalkCues.Scene.SCOLDED, "ken", 2, 0) == com.hex.kagune.control.npc.EtoGesture.HUFF
            && com.hex.kagune.control.family.TalkCues.parent("Fufu. Again?", com.hex.kagune.control.family.TalkCues.Scene.MAKE_UP, "eto", 0, 1)
            == com.hex.kagune.control.npc.EtoGesture.LAUGH, "a parent's gesture: Touka's hands on hips, a sigh, Eto's laugh");
        for (com.hex.kagune.control.family.ChildCue cue : com.hex.kagune.control.family.ChildCue.values()) {
            check(cue == com.hex.kagune.control.family.ChildCue.NONE || cue.ticks >= 30 && cue.weight(cue.ticks / 2.0) > 0.99F && cue.weight(0.0) == 0.0F
                && cue.weight(cue.ticks) == 0.0F, cue + ": eases in, holds, eases out");
        }
        check(com.hex.kagune.control.family.ChildCue.NONE.ordinal() == 0 && com.hex.kagune.control.family.ChildCue.byOrdinal(99)
            == com.hex.kagune.control.family.ChildCue.NONE, "sent by ordinal, nothing unknown");
    }

    /**
     * 1.3.58: hide and seek: every part of the game has words for every temper at both ages, filled in and clean; the hider picks a spot out
     * of sight first, then a good distance; the spots it weighs are all in the yard; the seeker goes to a giggle it just heard.
     */
    static void hideAndSeek() {
        boolean clean = true;
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (com.hex.kagune.control.family.HideAndSeek.Part part : com.hex.kagune.control.family.HideAndSeek.Part.values()) {
            for (com.hex.kagune.control.family.ChildTemper t : com.hex.kagune.control.family.ChildTemper.values()) {
                for (com.hex.kagune.control.family.ChildStage st : new com.hex.kagune.control.family.ChildStage[] {com.hex.kagune.control.family.ChildStage.TODDLER,
                    com.hex.kagune.control.family.ChildStage.CHILD, com.hex.kagune.control.family.ChildStage.TEEN}) {
                    for (int seed = 0; seed < 12; seed++) {
                        String line = com.hex.kagune.control.family.HideAndSeek.line(part, t, st, seed);
                        String low = line == null ? "" : line.toLowerCase(java.util.Locale.ROOT);
                        clean &= line != null && !line.isBlank() && !line.contains("{");
                        for (String banned : new String[] {"kaneki", "ayato", "furuta", "takatsuki", "aogiri", "yoshimura"}) {
                            clean &= !low.contains(banned);
                        }
                        seen.add(line);
                    }
                }
            }
        }
        check(clean && seen.size() > 70, "hide and seek: every part, temper and age has its words (" + seen.size() + " lines)");
        check(com.hex.kagune.control.family.HideAndSeek.line(com.hex.kagune.control.family.HideAndSeek.Part.HINT,
            com.hex.kagune.control.family.ChildTemper.SUNNY, com.hex.kagune.control.family.ChildStage.CHILD, 1).startsWith("*"),
            "a hider's giggle is a thought (in its cloud)");
        check(com.hex.kagune.control.family.HideAndSeek.bestSpot(new double[] {9.0, 5.0, 7.0}, new boolean[] {false, true, false}) == 1
            && com.hex.kagune.control.family.HideAndSeek.bestSpot(new double[] {9.0, 5.0, 7.0}, new boolean[] {false, false, false}) == 0
            && com.hex.kagune.control.family.HideAndSeek.bestSpot(new double[] {2.0, 6.0}, new boolean[] {true, true}) == 1
            && com.hex.kagune.control.family.HideAndSeek.bestSpot(new double[] {Double.NaN, 3.0}, new boolean[] {true, false}) == 1
            && com.hex.kagune.control.family.HideAndSeek.bestSpot(new double[] {Double.NaN}, new boolean[] {true}) == -1,
            "out of sight first, then a good way off; never a spot with no ground");
        boolean inside = true;
        for (int seed = 0; seed < 40; seed++) {
            for (int i = 0; i < com.hex.kagune.control.family.HideAndSeek.SPOTS; i++) {
                double[] at = com.hex.kagune.control.family.HideAndSeek.spot(i, 10.0, -5.0, 14.0, seed * 37);
                inside &= Math.hypot(at[0] - 10.0, at[1] + 5.0) <= com.hex.kagune.control.family.Siblings.yard(14.0) + 1.0E-9;
            }
        }
        check(inside, "every spot it weighs is in the yard");
        check(com.hex.kagune.control.family.HideAndSeek.heard(10L) && !com.hex.kagune.control.family.HideAndSeek.heard(500L)
            && !com.hex.kagune.control.family.HideAndSeek.heard(-5L), "a giggle heard just now leads the seeker to it");
        check(com.hex.kagune.control.family.HideAndSeek.LONGEST > com.hex.kagune.control.family.HideAndSeek.COUNT + com.hex.kagune.control.family.HideAndSeek.SEEKS
            && com.hex.kagune.control.family.ChildCue.HIDE.ticks > com.hex.kagune.control.family.HideAndSeek.LONGEST - 200,
            "its crouch lasts the game");
    }

    /**
     * 1.3.59: an older one with the baby: peekaboo, faces, baby talk, a hum in the evening; every kind, temper and age has its words, the
     * baby's are actions (its thought cloud), peekaboo hides the face then says boo; filled in and clean.
     */
    static void littleOnes() {
        boolean clean = true;
        boolean shapes = true;
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (com.hex.kagune.control.family.LittleOnes.Kind kind : com.hex.kagune.control.family.LittleOnes.Kind.values()) {
            for (com.hex.kagune.control.family.ChildTemper t : com.hex.kagune.control.family.ChildTemper.values()) {
                for (com.hex.kagune.control.family.ChildStage st : new com.hex.kagune.control.family.ChildStage[] {com.hex.kagune.control.family.ChildStage.TODDLER,
                    com.hex.kagune.control.family.ChildStage.CHILD, com.hex.kagune.control.family.ChildStage.TEEN}) {
                    for (int seed = 0; seed < 9; seed++) {
                        java.util.List<com.hex.kagune.control.family.LittleOnes.Line> talk = com.hex.kagune.control.family.LittleOnes.talk(kind, t, st, "Mei", seed);
                        shapes &= talk.size() == (kind == com.hex.kagune.control.family.LittleOnes.Kind.PEEKABOO ? 4 : 3) && !talk.get(0).baby()
                            && !talk.get(talk.size() - 1).baby() && talk.stream().filter(com.hex.kagune.control.family.LittleOnes.Line::baby).count() == 1;
                        for (com.hex.kagune.control.family.LittleOnes.Line line : talk) {
                            String low = line.text().toLowerCase(java.util.Locale.ROOT);
                            clean &= !line.text().isBlank() && !line.text().contains("{") && line.cue() != null;
                            clean &= !line.baby() || line.text().startsWith("*") && line.text().endsWith("*");
                            for (String banned : new String[] {"kaneki", "ayato", "furuta", "takatsuki", "aogiri", "yoshimura"}) {
                                clean &= !low.contains(banned);
                            }
                            seen.add(line.text());
                        }
                    }
                }
            }
        }
        check(shapes, "the older one starts and ends it, the baby answers once between");
        check(clean && seen.size() > 70, "with the baby: every line filled in, the baby's all actions, no names it mustn't (" + seen.size() + ")");
        java.util.List<com.hex.kagune.control.family.LittleOnes.Line> peek = com.hex.kagune.control.family.LittleOnes.talk(
            com.hex.kagune.control.family.LittleOnes.Kind.PEEKABOO, com.hex.kagune.control.family.ChildTemper.GENTLE, com.hex.kagune.control.family.ChildStage.CHILD,
            "Mei", 4);
        check(peek.get(0).cue() == com.hex.kagune.control.family.ChildCue.COUNT && peek.get(1).text().toLowerCase(java.util.Locale.ROOT).contains("boo"),
            "peekaboo: its own face hidden, then boo");
        check(com.hex.kagune.control.family.LittleOnes.pick(com.hex.kagune.control.family.ChildTemper.BOLD, true, 0.1)
            == com.hex.kagune.control.family.LittleOnes.Kind.LULLABY, "in the evening, a hum to settle it");
        java.util.Set<com.hex.kagune.control.family.LittleOnes.Kind> kinds = java.util.EnumSet.noneOf(com.hex.kagune.control.family.LittleOnes.Kind.class);
        for (double r = 0.0; r < 1.0; r += 0.02) {
            kinds.add(com.hex.kagune.control.family.LittleOnes.pick(com.hex.kagune.control.family.ChildTemper.GENTLE, false, r));
        }
        check(kinds.size() == 4, "by day, all of them: " + kinds);
    }

    static void genome() {
        com.hex.kagune.control.family.Genome.Parent eto = new com.hex.kagune.control.family.Genome.Parent(EtoRules.HAIR, EtoRules.EYES, EtoRules.SKIN,
            true, true);
        com.hex.kagune.control.family.Genome.Parent human = new com.hex.kagune.control.family.Genome.Parent(0x201010, 0x403020, 0xC08060, false, false);
        com.hex.kagune.control.family.Genome.Parent ghoul = new com.hex.kagune.control.family.Genome.Parent(0x101010, 0x403020, 0xC08060, true, false);
        java.util.Random random = new java.util.Random(7);
        int one = 0;
        int girls = 0;
        int n = 4000;
        boolean between = true;
        boolean ghouls = true;
        boolean roundTrip = true;
        for (int i = 0; i < n; i++) {
            com.hex.kagune.control.family.Genome child = com.hex.kagune.control.family.Genome.of(eto, human, random);
            ghouls &= child.ghoul() && child.kakugan() != com.hex.kagune.control.family.Genome.NONE;
            one += child.oneEyed() ? 1 : 0;
            girls += child.gender() == Gender.FEMALE ? 1 : 0;
            int g = (child.hair() >> 8) & 255;
            between &= g >= 0x10 - 12 && g <= 0xC0 + 12;
            roundTrip &= child.equals(com.hex.kagune.control.family.Genome.load(child.save()));
        }
        check(ghouls, "a ghoul parent: a ghoul child, with its kakugan");
        check(Math.abs(one / (double)n - com.hex.kagune.control.family.Genome.ONE_EYED_CHANCE) < 0.04, "a one-eyed parent: a good chance of one kakugan ("
            + one + " of " + n + ")");
        check(Math.abs(girls / (double)n - 0.5) < 0.04, "a boy or a girl, as likely");
        check(between, "its hair between both parents' hair");
        check(roundTrip && com.hex.kagune.control.family.Genome.load("nonsense") == null && com.hex.kagune.control.family.Genome.load(null) == null,
            "kept and read back exactly; nonsense is nothing");
        boolean neverOne = true;
        boolean humans = true;
        for (int i = 0; i < 500; i++) {
            neverOne &= !com.hex.kagune.control.family.Genome.of(ghoul, human, random).oneEyed();
            com.hex.kagune.control.family.Genome both = com.hex.kagune.control.family.Genome.of(human, human, random);
            humans &= !both.ghoul() && both.kakugan() == com.hex.kagune.control.family.Genome.NONE;
        }
        check(neverOne, "no one-eyed parent: never one kakugan, both or none");
        check(humans, "two humans: a human child, no kakugan");
    }

    static void talk() {
        EtoTalk.Context wed = EtoTests.full(100, NpcBonds.MET, true, Romance.Stage.MARRIED, null, 6000L, EtoMood.HAPPY, true, EtoRules.Pact.NONE, false,
            false, EtoRules.Lesson.KICK, 3);
        com.hex.kagune.control.npc.EtoTopics.Topic us = com.hex.kagune.control.npc.EtoTopics.get("us");
        check(us != null && us.open(wed).stream().anyMatch(f -> f.prompt().equals("Let's have a baby.")), "married: \"Let's have a baby.\" can be asked");
        check(us.open(wed).stream().filter(f -> f.prompt().equals("Let's have a baby.")).findFirst().map(f -> f.reply().apply(wed).effect())
            .orElse(null) == EtoTalk.Effect.BABY, "and it asks for one (the next night it comes)");
        EtoTalk.Context dating = EtoTests.full(100, NpcBonds.MET, true, Romance.Stage.DATING, null, 6000L, EtoMood.HAPPY, true, EtoRules.Pact.NONE, false,
            false, EtoRules.Lesson.KICK, 3);
        check(us.open(dating).stream().noneMatch(f -> f.prompt().equals("Let's have a baby.")), "only married couples");
        EtoTalk.Context asked = with(wed, new com.hex.kagune.control.family.FamilyView(-1.0, true, 0));
        check(us.open(asked).stream().noneMatch(f -> f.prompt().equals("Let's have a baby.")) && us.open(asked).stream().anyMatch(f -> f.prompt()
            .equals("About tonight...")), "asked already: only tonight to talk of");
        EtoTalk.Context carrying = with(wed, new com.hex.kagune.control.family.FamilyView(0.5, false, 0));
        check(carrying.pregnant() && us.open(carrying).stream().noneMatch(f -> f.prompt().equals("Let's have a baby.")), "carrying: not asked again");
        EtoTalk.Page page = EtoTalk.talk(carrying, "");
        check(!page.options().isEmpty() && page.options().get(0).key().equals("topic:baby"), "carrying: the baby is the first thing to talk about");
        check(EtoTalk.talk(wed, "").options().stream().noneMatch(o -> o.key().equals("topic:baby")), "and only then");
        com.hex.kagune.control.npc.EtoTopics.Topic baby = com.hex.kagune.control.npc.EtoTopics.get("baby");
        boolean lines = true;
        for (double along : new double[] {0.05, 0.4, 0.85, 1.0}) {
            EtoTalk.Context c = with(wed, new com.hex.kagune.control.family.FamilyView(along, false, 0));
            lines &= !baby.answer().apply(c).isEmpty() && !baby.open(c).isEmpty();
            lines &= !EtoLines.pregnant(c.family().stage(), 4).isEmpty();
        }
        check(lines, "something to say at every stage");
        check(!EtoLines.conceived(wed).isEmpty(), "the morning after: she knows");
    }

    /** {@code c} with {@code family}. */
    static EtoTalk.Context with(EtoTalk.Context c, com.hex.kagune.control.family.FamilyView family) {
        return new EtoTalk.Context(c.regard(), c.flags(), c.count(), c.ghoul(), c.chattedToday(), c.giftedToday(), c.book(), c.flower(), c.next(),
            c.beforeDone(), c.granted(), c.seed(), c.stage(), c.claimedBy(), c.following(), c.dayTime(), c.mood(), c.daysTogether(), c.daysAway(),
            c.puppy(), c.ring(), c.meat(), c.letter(), c.pact(), c.pactCount(), c.trialDone(), c.nightUsed(), c.name(), c.spouseElsewhere(), family);
    }

    static void children() {
        com.hex.kagune.control.family.ChildStage baby = com.hex.kagune.control.family.ChildStage.BABY;
        com.hex.kagune.control.family.ChildStage teen = com.hex.kagune.control.family.ChildStage.TEEN;
        check(com.hex.kagune.control.family.ChildStage.of(0.0) == baby && com.hex.kagune.control.family.ChildStage.of(2.5)
            == com.hex.kagune.control.family.ChildStage.TODDLER && com.hex.kagune.control.family.ChildStage.of(6.0)
            == com.hex.kagune.control.family.ChildStage.CHILD && com.hex.kagune.control.family.ChildStage.of(12.0) == teen
            && com.hex.kagune.control.family.ChildStage.of(50.0) == com.hex.kagune.control.family.ChildStage.ADULT,
            "a baby, a toddler, a child, a teen, a grown-up (2.0.3), by its age in days");
        float last = 0.0F;
        boolean growing = true;
        for (com.hex.kagune.control.family.ChildStage stage : com.hex.kagune.control.family.ChildStage.values()) {
            growing &= stage.box > last && (stage == com.hex.kagune.control.family.ChildStage.ADULT ? stage.box == 1.0F : stage.box < 1.0F)
                && stage.health > 0.0F;
            last = stage.box;
        }
        check(growing, "each stage bigger than the last, all smaller than a grown player until it is one (2.0.3)");
        check(baby.sits() && baby.carried() && !baby.walks() && !baby.learns() && com.hex.kagune.control.family.ChildStage.TODDLER.carried()
            && com.hex.kagune.control.family.ChildStage.TODDLER.walks() && !com.hex.kagune.control.family.ChildStage.CHILD.carried()
            && com.hex.kagune.control.family.ChildStage.CHILD.learns() && teen.learns() && com.hex.kagune.control.family.ChildStage.ADULT.learns()
            && teen.next() == com.hex.kagune.control.family.ChildStage.ADULT
            && com.hex.kagune.control.family.ChildStage.ADULT.next() == com.hex.kagune.control.family.ChildStage.ADULT
            && teen.older() && com.hex.kagune.control.family.ChildStage.ADULT.older() && !com.hex.kagune.control.family.ChildStage.CHILD.older(),
            "a baby sits and is carried; a toddler walks and can still be carried; a child, a teen and a grown-up can be taught; it grows up");
        check(com.hex.kagune.control.family.ChildStage.days(0L, 48000L) == 2.0, "days from the birth");
        check(!com.hex.kagune.control.family.ChildRules.teachable(baby, 0, Long.MIN_VALUE, 0L)
            && com.hex.kagune.control.family.ChildRules.teachable(teen, 0, Long.MIN_VALUE, 0L)
            && !com.hex.kagune.control.family.ChildRules.teachable(teen, 1, 100L, 200L)
            && com.hex.kagune.control.family.ChildRules.teachable(teen, 1, 100L, 100L + com.hex.kagune.control.family.ChildStage.DAY)
            && !com.hex.kagune.control.family.ChildRules.teachable(teen, com.hex.kagune.control.family.ChildRules.SKILL_MOST, Long.MIN_VALUE, 0L),
            "taught a lesson a day, child and teen only, up to the most");
        check(!com.hex.kagune.control.family.ChildRules.fights(teen, 0) && com.hex.kagune.control.family.ChildRules.fights(teen, 1)
            && com.hex.kagune.control.family.ChildRules.damage(baby, 5, true) == 0.0F
            && com.hex.kagune.control.family.ChildRules.damage(teen, 3, false) > com.hex.kagune.control.family.ChildRules.damage(
                com.hex.kagune.control.family.ChildStage.CHILD, 3, false)
            && com.hex.kagune.control.family.ChildRules.damage(teen, 3, true) > com.hex.kagune.control.family.ChildRules.damage(teen, 3, false),
            "it fights only once taught; a teen hits harder than a child, a ghoul harder still; a baby never");
        boolean said = true;
        for (com.hex.kagune.control.family.ChildStage stage : com.hex.kagune.control.family.ChildStage.values()) {
            for (int seed = 0; seed < 6; seed++) {
                String call = com.hex.kagune.control.family.ChildLines.call(stage, seed % 2 == 0);
                for (String line : new String[] {com.hex.kagune.control.family.ChildLines.idle(stage, call, seed),
                    com.hex.kagune.control.family.ChildLines.greet(stage, call, seed), com.hex.kagune.control.family.ChildLines.up(stage, seed),
                    com.hex.kagune.control.family.ChildLines.down(stage, seed), com.hex.kagune.control.family.ChildLines.play(stage, seed),
                    com.hex.kagune.control.family.ChildLines.lesson(stage, seed % 6, seed), com.hex.kagune.control.family.ChildLines.follow(stage, seed),
                    com.hex.kagune.control.family.ChildLines.stay(stage, seed), com.hex.kagune.control.family.ChildLines.fight(stage, seed),
                    com.hex.kagune.control.family.ChildLines.scared(stage, call, seed), com.hex.kagune.control.family.ChildLines.grown(stage, "Hana")}) {
                    said &= line != null && !line.isEmpty();
                }
            }
        }
        check(said, "something to say in every stage, every time");
        check(com.hex.kagune.control.family.ChildLines.call(teen, true).equals("Mom") && com.hex.kagune.control.family.ChildLines.call(baby, false)
            .equals("Papa"), "Mama and Papa, Mom and Dad");
    }

    static int[] part(String name) {
        try (java.io.InputStream in = new java.io.FileInputStream("src/main/resources/assets/hexkagune/textures/entity/child/" + name + ".png")) {
            return com.hex.kagune.control.family.Png.read(in).pixels();
        } catch (java.io.IOException missing) {
            return null;
        }
    }

    /** 1.3.18: the owner's skins for sons and daughters: all there, each sex its own, picked near the child's tone, the same all its life. */
    static void ownerSkins() {
        java.util.List<com.hex.kagune.control.family.ChildSkins.Skin> sons = com.hex.kagune.control.family.ChildSkins.of(false);
        java.util.List<com.hex.kagune.control.family.ChildSkins.Skin> daughters = com.hex.kagune.control.family.ChildSkins.of(true);
        check(sons.size() >= 10 && daughters.size() >= 10, "the owner's skins: sons' and daughters' both there");
        boolean files = true;
        for (com.hex.kagune.control.family.ChildSkins.Skin skin : com.hex.kagune.control.family.ChildSkins.ALL) {
            for (String suffix : new String[] {"", "_kr", "_kl", "_gr", "_gl"}) {
                files &= new java.io.File("src/main/resources/assets/hexkagune/textures/entity/child/skins/" + skin.key() + suffix + ".png").isFile();
            }
            files &= skin.key().matches("[a-z0-9_]+");
        }
        check(files, "every skin has its texture and its four eye overlays, under a name a texture can have");
        com.hex.kagune.control.family.Genome.Parent light = new com.hex.kagune.control.family.Genome.Parent(0x6B4423, 0x3A6EA5, 0xF0D0B8, false, false);
        com.hex.kagune.control.family.Genome.Parent lightGhoul = new com.hex.kagune.control.family.Genome.Parent(0x202020, 0x3A6EA5, 0xF6DCC8, true, true);
        com.hex.kagune.control.family.Genome.Parent dark = new com.hex.kagune.control.family.Genome.Parent(0x101010, 0x40281A, 0x6E4A34, false, false);
        java.util.Random random = new java.util.Random(11);
        java.util.Set<String> lightBoys = new java.util.HashSet<>();
        java.util.Set<String> lightGirls = new java.util.HashSet<>();
        boolean sexes = true;
        boolean steady = true;
        boolean neverDark = true;
        int darkest = 0xFFFFFF;
        for (com.hex.kagune.control.family.ChildSkins.Skin skin : com.hex.kagune.control.family.ChildSkins.ALL) {
            if (lum(skin.tone()) < lum(darkest)) {
                darkest = skin.tone();
            }
        }
        for (int i = 0; i < 400; i++) {
            com.hex.kagune.control.family.Genome child = com.hex.kagune.control.family.Genome.of(lightGhoul, light, random);
            com.hex.kagune.control.family.ChildSkins.Skin skin = com.hex.kagune.control.family.ChildSkins.pick(child);
            sexes &= skin != null && skin.girl() == child.gender().female();
            steady &= skin == com.hex.kagune.control.family.ChildSkins.pick(com.hex.kagune.control.family.Genome.load(child.save()));
            neverDark &= skin != null && lum(skin.tone()) > lum(darkest) + 20;
            if (skin != null) {
                (skin.girl() ? lightGirls : lightBoys).add(skin.key());
            }
        }
        check(sexes, "a son wears a son's skin, a daughter a daughter's");
        check(steady, "a child's skin is the same every time it is drawn (from its genome, kept in its save)");
        check(neverDark, "light-skinned parents' children are never given the darkest skins");
        check(lightBoys.size() >= 4 && lightGirls.size() >= 4, "a light-skinned couple's children still come in many looks (" + lightBoys.size() + " boys', "
            + lightGirls.size() + " girls')");
        boolean darkNear = true;
        for (int i = 0; i < 100; i++) {
            com.hex.kagune.control.family.Genome child = com.hex.kagune.control.family.Genome.of(dark, dark, random);
            com.hex.kagune.control.family.ChildSkins.Skin skin = com.hex.kagune.control.family.ChildSkins.pick(child);
            double mine = com.hex.kagune.control.family.ChildSkins.distance(skin.tone(), child.skin());
            for (com.hex.kagune.control.family.ChildSkins.Skin other : com.hex.kagune.control.family.ChildSkins.of(skin.girl())) {
                darkNear &= mine <= com.hex.kagune.control.family.ChildSkins.distance(other.tone(), child.skin()) + com.hex.kagune.control.family.ChildSkins.SPREAD;
            }
        }
        check(darkNear, "a darker couple's children wear the darkest skins there are (none further than the nearest by more than the spread)");
    }

    /** 1.3.20: a child's moments with its parents: who can have which, their shapes over time, both bodies' poses. */
    static void moments() {
        com.hex.kagune.control.family.ChildMoment[] all = com.hex.kagune.control.family.ChildMoment.values();
        check(all[0] == com.hex.kagune.control.family.ChildMoment.NONE && com.hex.kagune.control.family.ChildMoment.byOrdinal(99)
            == com.hex.kagune.control.family.ChildMoment.NONE, "moments go by ordinal (NONE first; a bad one is none)");
        for (com.hex.kagune.control.family.ChildStage stage : com.hex.kagune.control.family.ChildStage.values()) {
            int play = 0;
            for (com.hex.kagune.control.family.ChildMoment m : com.hex.kagune.control.family.ChildMoment.PLAY) {
                if (m.fits(stage)) {
                    play++;
                    check(!m.label().isEmpty(), "every play choice has its words (" + m + ")");
                }
            }
            int page = play + (com.hex.kagune.control.family.ChildRules.shoulders(stage) ? 1 : 0) + 1;
            check(play >= 3 && page <= com.hex.kagune.control.npc.NpcTalk.Choice.SAYS.length, "the play page fits the talk screen at " + stage + " (" + page + ")");
            boolean own = false;
            for (com.hex.kagune.control.family.ChildMoment m : com.hex.kagune.control.family.ChildMoment.OWN) {
                own |= m.fits(stage);
            }
            check(own, "a " + stage + " has a moment of its own it starts");
        }
        check(com.hex.kagune.control.family.ChildMoment.TOSS.held && com.hex.kagune.control.family.ChildMoment.SPIN.held
            && !com.hex.kagune.control.family.ChildMoment.TOSS.fits(com.hex.kagune.control.family.ChildStage.TEEN)
            && !com.hex.kagune.control.family.ChildMoment.HUG.fits(com.hex.kagune.control.family.ChildStage.BABY),
            "a teen is never tossed; a baby isn't hugged standing (it sits): only what each can do");
        check(com.hex.kagune.control.family.ChildRules.shoulders(com.hex.kagune.control.family.ChildStage.TODDLER)
            && !com.hex.kagune.control.family.ChildRules.shoulders(com.hex.kagune.control.family.ChildStage.BABY)
            && !com.hex.kagune.control.family.ChildRules.shoulders(com.hex.kagune.control.family.ChildStage.TEEN), "toddlers and children ride on shoulders");
        double high = 0.0;
        boolean grounded = true;
        boolean smoothToss = true;
        double was = 0.0;
        for (double a = 0.0; a <= com.hex.kagune.control.family.ChildMoment.TOSS.ticks; a += 0.25) {
            double lift = com.hex.kagune.control.family.ChildMoment.tossLift(a);
            high = Math.max(high, lift);
            smoothToss &= Math.abs(lift - was) < 0.12;
            was = lift;
        }
        grounded &= Math.abs(com.hex.kagune.control.family.ChildMoment.tossLift(0.0)) < 0.07
            && Math.abs(com.hex.kagune.control.family.ChildMoment.tossLift(com.hex.kagune.control.family.ChildMoment.TOSS.ticks)) < 0.07;
        check(high > 0.8 && high < 1.2 && grounded && smoothToss, "a toss goes up about a block and comes back down into the arms, smoothly");
        double turned = com.hex.kagune.control.family.ChildMoment.spinAngle(com.hex.kagune.control.family.ChildMoment.SPIN.ticks);
        boolean onward = true;
        double angle = 0.0;
        for (double a = 0.0; a <= com.hex.kagune.control.family.ChildMoment.SPIN.ticks; a += 1.0) {
            double next = com.hex.kagune.control.family.ChildMoment.spinAngle(a);
            onward &= next >= angle - 1.0E-9;
            angle = next;
        }
        check(Math.abs(turned - 4.0 * Math.PI) < 1.0E-6 && onward, "a spin goes round twice, never back");
        boolean finite = true;
        boolean easedOut = true;
        for (com.hex.kagune.control.family.ChildMoment m : all) {
            if (m == com.hex.kagune.control.family.ChildMoment.NONE) {
                continue;
            }
            easedOut &= com.hex.kagune.control.family.ChildMoment.weight(m, 0.0) == 0.0 && com.hex.kagune.control.family.ChildMoment.weight(m, m.ticks) == 0.0;
            for (com.hex.kagune.control.family.ChildStage stage : com.hex.kagune.control.family.ChildStage.values()) {
                if (!m.fits(stage)) {
                    continue;
                }
                for (double a = 0.0; a < m.ticks; a += 1.5) {
                    com.hex.kagune.control.client.animation.RigPose parent = com.hex.kagune.control.client.animation.ChildMomentPose.parent(m, a, stage, true);
                    com.hex.kagune.control.client.animation.RigPose npc = com.hex.kagune.control.client.animation.ChildMomentPose.parent(m, a, stage, false);
                    com.hex.kagune.control.client.animation.RigPose child = com.hex.kagune.control.client.animation.ChildMomentPose.child(m, a, stage);
                    com.hex.kagune.control.client.animation.RigPose into = new com.hex.kagune.control.client.animation.RigPose();
                    com.hex.kagune.control.client.animation.ChildMomentPose.blend(into, parent, 0.7F);
                    com.hex.kagune.control.client.animation.ChildMomentPose.blend(into, child, 0.4F);
                    boolean asleep = m == com.hex.kagune.control.family.ChildMoment.TUCK; // tucked in, it lies as the game lays it (no pose)
                    finite &= (child != null || asleep) && ok(parent) && ok(npc) && (asleep || ok(child)) && ok(into);
                    // An NPC parent's model bends no knees: it never sinks into the ground crouching.
                    finite &= npc.drop <= 1.0F;
                }
            }
        }
        check(finite, "every moment poses both bodies, every tick, at every stage it fits (an NPC parent leans, never sinks)");
        check(easedOut, "every moment eases in from nothing and back out to nothing");
        com.hex.kagune.control.client.animation.RigPose shoulders = com.hex.kagune.control.client.animation.ChildMomentPose.shouldersChild(10.0);
        check(shoulders.legHold > 0.0F && shoulders.rightLegX < -0.5F && ok(com.hex.kagune.control.client.animation.ChildMomentPose.shouldersParent()),
            "on the shoulders, its legs down the front, their hands up holding them");
    }

    /** 1.3.21: a family's home: when each goes to bed, and whose bed is whose (the double bed the spouse's, single beds the children's). */
    /**
     * 1.3.32: the airplane: the child is carried round at the parent's body yaw plus the spin's turn (ChildRenderer.glue); an NPC parent's
     * body is turned by its pose's spin about +Y (EtoRenderer: rotateAxis(spin)); the two must go the same way round (they went opposite).
     */
    static void airplane() {
        for (double age = 4.0; age < com.hex.kagune.control.family.ChildMoment.SPIN.ticks - 4.0; age += 3.0) {
            double turn = com.hex.kagune.control.family.ChildMoment.spinTurn(age);
            if (Math.abs(turn) < 0.05 || Math.abs(Math.abs(turn) - Math.PI) < 0.05) {
                continue;
            }
            com.hex.kagune.control.client.animation.RigPose parent = com.hex.kagune.control.client.animation.ChildMomentPose.parent(
                com.hex.kagune.control.family.ChildMoment.SPIN, age, com.hex.kagune.control.family.ChildStage.CHILD, false);
            // Facing south (+z): turned about +Y by the pose's spin, as the renderer turns it.
            double a = parent.spin;
            double px = Math.sin(a);
            double pz = Math.cos(a);
            // The child, carried at yaw + turn: Minecraft faces (-sin yaw, cos yaw).
            double cx = -Math.sin(turn);
            double cz = Math.cos(turn);
            check(Math.signum(px) == Math.signum(cx) && Math.abs(px - cx) < 1.0E-6 && Math.abs(pz - cz) < 1.0E-6,
                "the airplane: the parent turns the same way round as the child it carries (" + age + ")");
        }
    }

    /** 1.3.31: the spouse's real help: cooking, a ghoul's raw meal, smelting with fuel, ripe crops, the meal kept for its spouse. */
    static void kitchen() {
        java.util.function.Function<String, String> block = id -> "Block{minecraft:" + id + "}";
        check(com.hex.kagune.control.family.Kitchen.cooked("minecraft:beef").equals("minecraft:cooked_beef")
            && com.hex.kagune.control.family.Kitchen.cooked("minecraft:potato").equals("minecraft:baked_potato")
            && com.hex.kagune.control.family.Kitchen.cooked("minecraft:cooked_beef") == null && com.hex.kagune.control.family.Kitchen.cooked("minecraft:stone") == null
            && com.hex.kagune.control.family.Kitchen.cooked(null) == null, "raw food cooks; cooked food and stone don't");
        for (String raw : new String[] {"beef", "porkchop", "chicken", "mutton", "rabbit", "cod", "salmon"}) {
            String cooked = com.hex.kagune.control.family.Kitchen.cooked("minecraft:" + raw);
            check(cooked != null && FoodClassifier.classify(cooked.substring(10), true) == FoodClassifier.Diet.HUMAN_FOOD,
                raw + " cooks into human food (a ghoul can't eat it: its meal is kept raw)");
            check(com.hex.kagune.control.family.Kitchen.flesh("minecraft:" + raw) && !com.hex.kagune.control.family.Kitchen.flesh(cooked), raw + " is a ghoul's raw cut, cooked it isn't");
        }
        check(com.hex.kagune.control.family.Kitchen.smelted("minecraft:raw_iron").equals("minecraft:iron_ingot")
            && com.hex.kagune.control.family.Kitchen.smelted("minecraft:sand").equals("minecraft:glass")
            && com.hex.kagune.control.family.Kitchen.smelted("minecraft:oak_log").equals("minecraft:charcoal")
            && com.hex.kagune.control.family.Kitchen.smelted("minecraft:stripped_oak_log") == null
            && com.hex.kagune.control.family.Kitchen.smelted("minecraft:crimson_stem") == null
            && com.hex.kagune.control.family.Kitchen.smelted("minecraft:iron_ingot") == null, "ores, sand, logs smelt; ingots don't");
        check(com.hex.kagune.control.family.Kitchen.fuel("minecraft:coal") == 16 && com.hex.kagune.control.family.Kitchen.fuel("minecraft:charcoal") == 16
            && com.hex.kagune.control.family.Kitchen.fuel("minecraft:oak_planks") == 3 && com.hex.kagune.control.family.Kitchen.fuel("minecraft:stick") == 1
            && com.hex.kagune.control.family.Kitchen.fuel("minecraft:stone") == 0 && com.hex.kagune.control.family.Kitchen.fuel("minecraft:coal") / com.hex.kagune.control.family.Kitchen.PER_ITEM == 8,
            "a coal cooks eight, as a furnace's does; stone doesn't burn");
        check(Chores.of(block.apply("furnace")) == Chores.Kind.COOK && Chores.of(block.apply("smoker")) == Chores.Kind.COOK
            && Chores.of(block.apply("campfire")) == Chores.Kind.COOK && Chores.of(block.apply("blast_furnace")) == Chores.Kind.COOK, "the stoves are found");
        check(com.hex.kagune.control.family.Kitchen.station(block.apply("furnace"), false) && com.hex.kagune.control.family.Kitchen.station(block.apply("furnace"), true)
            && com.hex.kagune.control.family.Kitchen.station(block.apply("smoker"), false) && !com.hex.kagune.control.family.Kitchen.station(block.apply("smoker"), true)
            && !com.hex.kagune.control.family.Kitchen.station(block.apply("blast_furnace"), false) && com.hex.kagune.control.family.Kitchen.station(block.apply("blast_furnace"), true)
            && com.hex.kagune.control.family.Kitchen.free(block.apply("campfire")) && !com.hex.kagune.control.family.Kitchen.free(block.apply("furnace")),
            "a furnace cooks and smelts, a smoker cooks, a blast furnace smelts; a campfire needs no fuel");
        check(com.hex.kagune.control.family.Kitchen.ripe("Block{minecraft:wheat}[age=7]") && !com.hex.kagune.control.family.Kitchen.ripe("Block{minecraft:wheat}[age=6]")
            && com.hex.kagune.control.family.Kitchen.ripe("Block{minecraft:beetroots}[age=3]") && !com.hex.kagune.control.family.Kitchen.ripe("Block{minecraft:beetroots}[age=2]")
            && com.hex.kagune.control.family.Kitchen.ripe("Block{minecraft:potatoes}[age=7]") && !com.hex.kagune.control.family.Kitchen.ripe("Block{minecraft:oak_sapling}[stage=1]")
            && !com.hex.kagune.control.family.Kitchen.ripe("Block{minecraft:poppy}") && !com.hex.kagune.control.family.Kitchen.ripe(null), "only ripe crops are picked");
        for (String crop : new String[] {"wheat", "carrots", "potatoes", "beetroots"}) {
            for (double roll = 0.0; roll < 1.0; roll += 0.1) {
                List<Chores.Pile> gives = com.hex.kagune.control.family.Kitchen.harvest(block.apply(crop), roll, 0.5);
                int total = gives.stream().mapToInt(Chores.Pile::count).sum();
                check(!gives.isEmpty() && total >= 1 && total <= 4, crop + " gives a little (" + total + ")");
            }
        }
        check(com.hex.kagune.control.family.Kitchen.harvest(block.apply("poppy"), 0.5, 0.5).isEmpty(), "a flower is never picked");
        List<Chores.Pile> meal = new ArrayList<>();
        int[] took = new int[1];
        meal = com.hex.kagune.control.family.Kitchen.add(meal, "minecraft:cooked_beef", 4, took);
        check(took[0] == 4 && com.hex.kagune.control.family.Kitchen.portions(meal) == 4, "a meal kept");
        meal = com.hex.kagune.control.family.Kitchen.add(meal, "minecraft:baked_potato", 9, took);
        check(took[0] == com.hex.kagune.control.family.Kitchen.MEAL - 4 && com.hex.kagune.control.family.Kitchen.portions(meal) == com.hex.kagune.control.family.Kitchen.MEAL,
            "never more than a meal's worth kept (the rest is put away)");
        meal = com.hex.kagune.control.family.Kitchen.add(meal, "minecraft:cooked_beef", 3, took);
        check(took[0] == 0, "a full meal takes no more");
        List<Chores.Pile> back = com.hex.kagune.control.family.Kitchen.load(com.hex.kagune.control.family.Kitchen.save(meal));
        check(back.equals(meal), "the meal survives its saved data");
        check(com.hex.kagune.control.family.Kitchen.load("bad;minecraft:x*;*3;minecraft:cooked_cod*2").size() == 1
            && com.hex.kagune.control.family.Kitchen.load(null).isEmpty() && com.hex.kagune.control.family.Kitchen.load("").isEmpty(), "a bad part is left out");
        check(com.hex.kagune.control.family.Kitchen.name("minecraft:cooked_beef").equals("cooked beef"), "named as it is said");
        String[] banned = {"kaneki", "ayato", "furuta", "takatsuki", "aogiri", "yoshimura"};
        for (String key : new String[] {"eto", "touka", "ken"}) {
            for (int seed = 0; seed < 4; seed++) {
                for (String line : new String[] {com.hex.kagune.control.family.Kitchen.served(key, true, "raw beef", seed),
                    com.hex.kagune.control.family.Kitchen.served(key, false, "cooked beef", seed), com.hex.kagune.control.family.Kitchen.noFuel(key, seed)}) {
                    check(line != null && !line.isBlank(), key + " has words for the kitchen");
                    for (String name : banned) {
                        check(!line.toLowerCase(java.util.Locale.ROOT).contains(name), key + "'s kitchen words never name " + name);
                    }
                }
                check(com.hex.kagune.control.family.Kitchen.served(key, true, "raw beef", seed).toLowerCase(java.util.Locale.ROOT).contains("raw"),
                    key + " knows a ghoul's meal is raw");
            }
        }
        int[] counts = new int[Chores.Kind.values().length];
        counts[Chores.Kind.HARVEST.ordinal()] = 3;
        counts[Chores.Kind.TIDY.ordinal()] = 3;
        int harvests = 0;
        for (int i = 0; i < 100; i++) {
            if (Chores.pick(counts, null, false, i / 100.0) == Chores.Kind.HARVEST) {
                harvests++;
            }
        }
        check(harvests > 50, "ripe crops come first more often than not (" + harvests + ")");
    }

    /** 1.3.30: what NPCs say goes into a bubble over their heads (never the chat): wrapped, timed, actions told apart. */
    static void speech() {
        String[] samples = {"...That didn't happen. Don't look at me.", "*picks Mei up and holds them close, rocking them*",
            "*her hand on your cheek, a kiss before you can blink* ...Don't. Say. Anything. I mean it.", "Boo.", "",
            "A-very-long-word-with-no-spaces-at-all-that-goes-on-and-on-and-on-and-on", Chores.start("eto", Chores.Kind.TIDY, 0)};
        for (String text : samples) {
            List<String> lines = com.hex.kagune.control.npc.Speech.wrap(text);
            check(lines.size() <= com.hex.kagune.control.npc.Speech.LINES, "a bubble is a few lines at most: " + text);
            for (String line : lines) {
                check(line.length() <= com.hex.kagune.control.npc.Speech.WIDTH && !line.isEmpty(), "each line short enough to read over a head: " + line);
            }
            if (!text.isBlank() && text.length() <= com.hex.kagune.control.npc.Speech.WIDTH * 2) {
                check(String.join(" ", lines).replace(" ", "").equals(text.replace(" ", "")), "nothing of a short line is lost: " + text);
            }
            int ticks = com.hex.kagune.control.npc.Speech.ticks(text);
            check(ticks >= com.hex.kagune.control.npc.Speech.LEAST && ticks <= com.hex.kagune.control.npc.Speech.MOST, "up long enough to read, not forever");
        }
        check(com.hex.kagune.control.npc.Speech.wrap(null).isEmpty() && com.hex.kagune.control.npc.Speech.wrap("   ").isEmpty(), "nothing said, no bubble");
        check(com.hex.kagune.control.npc.Speech.ticks("Boo.") < com.hex.kagune.control.npc.Speech.ticks(samples[2]), "more words stay up longer");
        check(com.hex.kagune.control.npc.Speech.action("*bounces and claps*") && !com.hex.kagune.control.npc.Speech.action("Shut up.")
            && !com.hex.kagune.control.npc.Speech.action(samples[2]), "an action alone is shown as one (grey, slanted)");
        check(com.hex.kagune.control.npc.Speech.styled("\u00a7dhi", false).equals("\u00a7fhi") && com.hex.kagune.control.npc.Speech.styled("waves", true).startsWith("\u00a77\u00a7o"),
            "a bubble's own look, whatever colour codes came with the words");
    }

    /** 1.3.29: a spouse's chores at home (HomeChores' rules): what each block is for, which chore next, a chest put in order, its words. */
    static void chores() {
        // 2.0.9: the spouse sorts the home's chests by kind.
        {
            java.util.function.BiFunction<String, boolean[], com.hex.kagune.control.family.ChestSort.Kind> k =
                (id, fb) -> com.hex.kagune.control.family.ChestSort.kind(id, fb[0], fb[1]);
            boolean[] item = {false, false};
            boolean[] food = {true, false};
            boolean[] block = {false, true};
            check(k.apply("minecraft:cooked_beef", food) == com.hex.kagune.control.family.ChestSort.Kind.FOOD
                && k.apply("minecraft:stone", block) == com.hex.kagune.control.family.ChestSort.Kind.BLOCKS
                && k.apply("minecraft:oak_planks", block) == com.hex.kagune.control.family.ChestSort.Kind.BLOCKS
                && k.apply("minecraft:iron_pickaxe", item) == com.hex.kagune.control.family.ChestSort.Kind.TOOLS
                && k.apply("minecraft:bow", item) == com.hex.kagune.control.family.ChestSort.Kind.TOOLS
                && k.apply("minecraft:bowl", item) == com.hex.kagune.control.family.ChestSort.Kind.WHATEVER
                && k.apply("minecraft:diamond_chestplate", item) == com.hex.kagune.control.family.ChestSort.Kind.ARMOR
                && k.apply("minecraft:iron_ore", block) == com.hex.kagune.control.family.ChestSort.Kind.ORES
                && k.apply("minecraft:raw_iron", item) == com.hex.kagune.control.family.ChestSort.Kind.ORES
                && k.apply("minecraft:iron_ingot", item) == com.hex.kagune.control.family.ChestSort.Kind.ORES
                && k.apply("minecraft:diamond", item) == com.hex.kagune.control.family.ChestSort.Kind.ORES
                && k.apply("minecraft:coal_block", block) == com.hex.kagune.control.family.ChestSort.Kind.BLOCKS
                && k.apply("minecraft:wheat_seeds", item) == com.hex.kagune.control.family.ChestSort.Kind.PLANTS
                && k.apply("minecraft:oak_sapling", block) == com.hex.kagune.control.family.ChestSort.Kind.PLANTS
                && k.apply("minecraft:poppy", block) == com.hex.kagune.control.family.ChestSort.Kind.PLANTS
                && k.apply("minecraft:grass_block", block) == com.hex.kagune.control.family.ChestSort.Kind.BLOCKS
                && k.apply("minecraft:bone", item) == com.hex.kagune.control.family.ChestSort.Kind.DROPS
                && k.apply("minecraft:string", item) == com.hex.kagune.control.family.ChestSort.Kind.DROPS
                && k.apply("minecraft:rotten_flesh", food) == com.hex.kagune.control.family.ChestSort.Kind.FOOD
                && k.apply("minecraft:enchanted_book", item) == com.hex.kagune.control.family.ChestSort.Kind.WHATEVER
                && k.apply("somemod:weird_gadget", item) == com.hex.kagune.control.family.ChestSort.Kind.WHATEVER,
                "each thing's kind: food, blocks, tools, armour, ores, plants, drops, else Whatever");
            check(com.hex.kagune.control.family.ChestSort.named("Food") == com.hex.kagune.control.family.ChestSort.Kind.FOOD
                && com.hex.kagune.control.family.ChestSort.named("armour") == com.hex.kagune.control.family.ChestSort.Kind.ARMOR
                && com.hex.kagune.control.family.ChestSort.named(" whatever ") == com.hex.kagune.control.family.ChestSort.Kind.WHATEVER
                && com.hex.kagune.control.family.ChestSort.named("Tools & Weapons") == com.hex.kagune.control.family.ChestSort.Kind.TOOLS
                && com.hex.kagune.control.family.ChestSort.named("My diamonds") == null && com.hex.kagune.control.family.ChestSort.named("") == null,
                "a chest named for a kind (any case, its word); any other name is not");
            check(com.hex.kagune.control.family.ChestSort.box("My diamonds").theirs() && !com.hex.kagune.control.family.ChestSort.box("").theirs()
                && com.hex.kagune.control.family.ChestSort.box("").free() && !com.hex.kagune.control.family.ChestSort.box("Food").free(),
                "the player's own named chest is theirs; an unnamed one is free");
            java.util.List<com.hex.kagune.control.family.ChestSort.Box> boxes = java.util.List.of(com.hex.kagune.control.family.ChestSort.box("Food"),
                com.hex.kagune.control.family.ChestSort.box(""), com.hex.kagune.control.family.ChestSort.box("Secret stash"),
                com.hex.kagune.control.family.ChestSort.box(""), com.hex.kagune.control.family.ChestSort.box(""));
            java.util.Map<com.hex.kagune.control.family.ChestSort.Kind, Integer> counts = new java.util.EnumMap<>(com.hex.kagune.control.family.ChestSort.Kind.class);
            counts.put(com.hex.kagune.control.family.ChestSort.Kind.FOOD, 20);
            counts.put(com.hex.kagune.control.family.ChestSort.Kind.BLOCKS, 300);
            counts.put(com.hex.kagune.control.family.ChestSort.Kind.TOOLS, 3);
            counts.put(com.hex.kagune.control.family.ChestSort.Kind.ORES, 40);
            java.util.List<com.hex.kagune.control.family.ChestSort.Kind> kinds = com.hex.kagune.control.family.ChestSort.assign(boxes, counts);
            check(kinds.get(0) == com.hex.kagune.control.family.ChestSort.Kind.FOOD && kinds.get(1) == com.hex.kagune.control.family.ChestSort.Kind.BLOCKS
                && kinds.get(2) == null && kinds.get(3) == com.hex.kagune.control.family.ChestSort.Kind.ORES
                && kinds.get(4) == com.hex.kagune.control.family.ChestSort.Kind.WHATEVER,
                "named chests keep their kind, the player's own is left alone, the most things first, the last free one Whatever: " + kinds);
            check(com.hex.kagune.control.family.ChestSort.target(kinds, com.hex.kagune.control.family.ChestSort.Kind.TOOLS) == 4
                && com.hex.kagune.control.family.ChestSort.target(kinds, com.hex.kagune.control.family.ChestSort.Kind.BLOCKS) == 1
                && com.hex.kagune.control.family.ChestSort.target(java.util.Arrays.asList(com.hex.kagune.control.family.ChestSort.Kind.FOOD, null),
                    com.hex.kagune.control.family.ChestSort.Kind.TOOLS) == -1, "a kind without a chest goes to Whatever; with none, it stays");
            java.util.List<com.hex.kagune.control.family.ChestSort.Kind> two = com.hex.kagune.control.family.ChestSort.assign(java.util.List.of(
                com.hex.kagune.control.family.ChestSort.box(""), com.hex.kagune.control.family.ChestSort.box("")), counts);
            check(two.get(0) == com.hex.kagune.control.family.ChestSort.Kind.BLOCKS && two.get(1) == com.hex.kagune.control.family.ChestSort.Kind.WHATEVER,
                "few chests: the biggest kind gets one, the rest goes to Whatever");
            check(com.hex.kagune.control.family.ChestSort.nameOf("{\"text\":\"Food\"}").equals("Food")
                && com.hex.kagune.control.family.ChestSort.nameOf(com.hex.kagune.control.family.ChestSort.saved("Tools & Weapons")).equals("Tools & Weapons")
                && com.hex.kagune.control.family.ChestSort.nameOf("").isEmpty()
                && com.hex.kagune.control.family.ChestSort.half("Block{minecraft:chest}[facing=north,type=left,waterlogged=false]").equals("left")
                && com.hex.kagune.control.family.ChestSort.half("Block{minecraft:barrel}[facing=up,open=false]").isEmpty(),
                "names read and written as the game saves them; a double chest's halves");
        }
        // 2.0.8: the stove's stirring three rounds over, and its crackle (none at a table).
        check(com.hex.kagune.control.family.Chores.stepTicks(com.hex.kagune.control.npc.EtoGesture.COOK)
            == 3L * com.hex.kagune.control.npc.EtoGesture.COOK.ticks - 4L
            && com.hex.kagune.control.family.Chores.stepTicks(com.hex.kagune.control.npc.EtoGesture.TIDY) == com.hex.kagune.control.npc.EtoGesture.TIDY.ticks - 4L,
            "cooking at the stove lasts three rounds of stirring; other steps one");
        check("block.furnace.fire_crackle".equals(com.hex.kagune.control.family.Chores.crackle("Block{minecraft:furnace}"))
            && "block.smoker.smoke".equals(com.hex.kagune.control.family.Chores.crackle("Block{minecraft:smoker}"))
            && "block.campfire.crackle".equals(com.hex.kagune.control.family.Chores.crackle("Block{minecraft:soul_campfire}"))
            && com.hex.kagune.control.family.Chores.crackle("Block{minecraft:oak_planks}") == null, "each stove's crackle; a table has none");
        java.util.function.Function<String, String> block = id -> "Block{minecraft:" + id + "}";
        check(Chores.id("Block{minecraft:oak_planks}").equals("minecraft:oak_planks") && Chores.id("plain").equals("plain") && Chores.id(null).isEmpty(),
            "a block's registry name is read from its text");
        check(Chores.of(block.apply("chest")) == Chores.Kind.TIDY && Chores.of(block.apply("barrel")) == Chores.Kind.TIDY
            && Chores.of(block.apply("trapped_chest")) == Chores.Kind.TIDY && Chores.of("Block{othermod:oak_chest}") == Chores.Kind.TIDY,
            "chests and barrels are tidied");
        check(Chores.of(block.apply("ender_chest")) == null, "an ender chest is never touched (its things are each player's own)");
        check(Chores.sortable(block.apply("chest")) && Chores.sortable(block.apply("barrel")) && !Chores.sortable(block.apply("trapped_chest"))
            && !Chores.sortable(block.apply("ender_chest")) && !Chores.sortable(block.apply("crafting_table")),
            "only a plain chest or a barrel has its things really sorted (a trapped chest only looks tidied)");
        check(Chores.lid(block.apply("chest")) && Chores.lid(block.apply("trapped_chest")) && !Chores.lid(block.apply("barrel"))
            && !Chores.lid("Block{othermod:oak_chest}"), "a chest's lid goes up (vanilla's: a mod's block event may mean something else), a barrel's doesn't");
        check(Chores.of(block.apply("crafting_table")) == Chores.Kind.WIPE && Chores.of(block.apply("cartography_table")) == Chores.Kind.WIPE
            && Chores.of("Block{furnimod:oak_table}") == Chores.Kind.WIPE && Chores.of(block.apply("enchanting_table")) == null, "tables are wiped");
        check(Chores.of(block.apply("bookshelf")) == Chores.Kind.READ && Chores.of(block.apply("lectern")) == Chores.Kind.READ
            && Chores.of(block.apply("chiseled_bookshelf")) == Chores.Kind.READ, "a book from the shelf, or the lectern");
        check(Chores.of(block.apply("glass_pane")) == Chores.Kind.LOOK && Chores.of(block.apply("white_stained_glass_pane")) == Chores.Kind.LOOK
            && Chores.of(block.apply("glass")) == null, "a window is looked out of");
        for (String plant : new String[] {"poppy", "dandelion", "red_tulip", "cornflower", "rose_bush", "potted_poppy", "wheat", "carrots", "potatoes",
            "beetroots", "oak_sapling", "cherry_sapling", "sweet_berry_bush", "melon_stem", "azalea", "flowering_azalea", "pink_petals"}) {
            check(Chores.of(block.apply(plant)) == Chores.Kind.WATER, plant + " is watered");
        }
        for (String not : new String[] {"azalea_leaves", "flowering_azalea_leaves", "grass", "tall_grass", "stone", "oak_planks", "flower_pot", "air",
            "water", "oak_door", "white_bed"}) {
            check(Chores.of(block.apply(not)) == null, not + " is no one's chore");
        }
        check(Chores.grows(block.apply("wheat")) && Chores.grows(block.apply("oak_sapling")) && Chores.grows(block.apply("sweet_berry_bush"))
            && Chores.grows(block.apply("pumpkin_stem")) && !Chores.grows(block.apply("poppy")) && !Chores.grows(block.apply("potted_oak_sapling"))
            && !Chores.grows(block.apply("attached_pumpkin_stem")), "watered, crops and saplings may grow a step; flowers and pots don't");
        for (Chores.Kind kind : Chores.Kind.values()) {
            check(kind.gesture.still() && kind.gesture.ticks > 20, kind + "'s gesture keeps it standing at the chore a while");
            check(kind.reach >= 1.0 && kind.reach <= 3.0, kind + " is done from close by");
        }
        check(Chores.Kind.TIDY.gesture == com.hex.kagune.control.npc.EtoGesture.TIDY && Chores.Kind.WATER.gesture == com.hex.kagune.control.npc.EtoGesture.WATER
            && Chores.Kind.SWEEP.gesture == com.hex.kagune.control.npc.EtoGesture.SWEEP && Chores.Kind.WIPE.gesture == com.hex.kagune.control.npc.EtoGesture.WIPE,
            "the chores use the gestures made for them (TIDY, WATER, the brush's SWEEP, the cloth's WIPE)");
        for (double roll = 0.0; roll <= 1.0; roll += 0.1) {
            long alone = Chores.gap(false, roll);
            long with = Chores.gap(true, roll);
            check(alone >= 300L && alone <= 700L && with >= 400L && with <= 900L && alone < with,
                "busy about the house: a chore every half minute or so, a little less often with its spouse about");
        }
        int[] none = new int[Chores.Kind.values().length];
        check(Chores.pick(none, null, false, 0.5) == null && Chores.pick(null, null, false, 0.5) == null, "nothing in the home: no chore");
        check(Chores.pick(none, null, true, 0.3) == Chores.Kind.SWEEP, "things lying on the floor: there is always the sweeping");
        int[] chests = none.clone();
        chests[Chores.Kind.TIDY.ordinal()] = 3;
        for (double roll = 0.0; roll < 1.0; roll += 0.05) {
            check(Chores.pick(chests, Chores.Kind.TIDY, false, roll) == Chores.Kind.TIDY, "only chests: the chests, even after the last tidying");
        }
        int[] all = new int[Chores.Kind.values().length];
        java.util.Arrays.fill(all, 4);
        int[] seen = new int[Chores.Kind.values().length];
        int[] afterWater = new int[Chores.Kind.values().length];
        for (int i = 0; i < 1000; i++) {
            Chores.Kind kind = Chores.pick(all, null, false, i / 1000.0);
            check(kind != null, "a full home always has a chore");
            if (kind != null) {
                seen[kind.ordinal()]++;
            }
            Chores.Kind again = Chores.pick(all, Chores.Kind.WATER, false, i / 1000.0);
            if (again != null) {
                afterWater[again.ordinal()]++;
            }
        }
        for (Chores.Kind kind : Chores.Kind.values()) {
            check(seen[kind.ordinal()] > 20, kind + " comes up in a full home");
        }
        check(afterWater[Chores.Kind.WATER.ordinal()] < seen[Chores.Kind.WATER.ordinal()] / 2, "the chore just done is less likely again");
        int[] items = all.clone();
        int swept = 0;
        for (int i = 0; i < 100; i++) {
            if (Chores.pick(items, Chores.Kind.SWEEP, true, i / 100.0) == Chores.Kind.SWEEP) {
                swept++;
            }
        }
        check(swept >= 25, "things on the floor make the sweeping likelier (even just after one)");
        // A chest put in order: nothing lost or made, stacks full, sorted, the rest empty; already in order, nothing to do.
        java.util.Random random = new java.util.Random(29);
        String[] keys = {"minecraft:bread", "minecraft:apple", "minecraft:iron_sword|{Damage:3}", "minecraft:iron_sword|{Damage:9}", "minecraft:ender_pearl",
            "minecraft:dirt"};
        int[] maxes = {64, 64, 1, 1, 16, 64};
        for (int round = 0; round < 300; round++) {
            int size = 27;
            List<Chores.Pile> piles = new ArrayList<>();
            java.util.Map<String, Integer> totals = new java.util.HashMap<>();
            for (int i = 0; i < size; i++) {
                if (random.nextInt(3) == 0) {
                    piles.add(null);
                    continue;
                }
                int k = random.nextInt(keys.length);
                int count = 1 + random.nextInt(maxes[k]);
                piles.add(new Chores.Pile(keys[k], count, maxes[k]));
                totals.merge(keys[k], count, Integer::sum);
            }
            List<Chores.Pile> sorted = Chores.sort(piles, size);
            if (totals.isEmpty()) {
                check(sorted == null, "an empty chest: nothing to sort");
                continue;
            }
            check(sorted != null && sorted.size() <= size, "a chest's things always fit back in it, stacked");
            if (sorted == null) {
                continue;
            }
            java.util.Map<String, Integer> after = new java.util.HashMap<>();
            String last = "";
            boolean order = true;
            boolean full = true;
            for (int i = 0; i < sorted.size(); i++) {
                Chores.Pile pile = sorted.get(i);
                after.merge(pile.key(), pile.count(), Integer::sum);
                order &= pile.key().compareTo(last) >= 0;
                last = pile.key();
                full &= pile.count() >= 1 && pile.count() <= pile.max();
                if (i + 1 < sorted.size() && sorted.get(i + 1).key().equals(pile.key())) {
                    full &= pile.count() == pile.max();
                }
            }
            check(after.equals(totals), "sorting a chest never loses or makes a thing");
            check(order, "a sorted chest is in name order");
            check(full, "a sorted chest's stacks are full before the next of the same is begun, none past its limit");
            List<Chores.Pile> again = new ArrayList<>(sorted);
            while (again.size() < size) {
                again.add(null);
            }
            check(Chores.sorted(again, size), "a chest already in order is left as it is");
            check(Chores.sorted(piles, size) == piles.equals(again), "a chest is in order only if sorting would leave it as it is");
            check(Chores.sort(again, size).equals(sorted), "sorting twice is sorting once");
        }
        List<Chores.Pile> over = new ArrayList<>();
        over.add(new Chores.Pile("minecraft:dirt", 200, 64));
        check(Chores.sort(over, 1) == null, "a stack past its own limit that would no longer fit: the chest is left alone");
        List<Chores.Pile> messy = new ArrayList<>(java.util.Arrays.asList(new Chores.Pile("minecraft:dirt", 3, 64), null, new Chores.Pile("minecraft:dirt", 4, 64)));
        check(!Chores.sorted(messy, 3) && Chores.sort(messy, 3).size() == 1 && Chores.sort(messy, 3).get(0).count() == 7, "two half stacks of dirt become one");
        // Its words: every companion, every chore, every way.
        String[] banned = {"kaneki", "ayato", "furuta", "takatsuki", "aogiri", "yoshimura"};
        for (String key : new String[] {"eto", "touka", "ken"}) {
            for (Chores.Kind kind : Chores.Kind.values()) {
                for (int seed = 0; seed < 6; seed++) {
                    for (String line : new String[] {Chores.start(key, kind, seed), Chores.done(key, kind, true, seed), Chores.done(key, kind, false, seed)}) {
                        check(line != null && !line.isBlank(), key + " has words for " + kind);
                        for (String name : banned) {
                            check(line == null || !line.toLowerCase(java.util.Locale.ROOT).contains(name), key + "'s chore words never name " + name);
                        }
                    }
                }
            }
            for (boolean married : new boolean[] {true, false}) {
                for (int seed = -3; seed < 6; seed++) {
                    String line = Chores.goingHome(key, married, "the Library", seed);
                    check(line != null && !line.isBlank(), key + " answers being sent home");
                }
            }
        }
        check(!Chores.start("touka", Chores.Kind.WATER, 0).equals(Chores.start("eto", Chores.Kind.WATER, 0))
            && !Chores.start("ken", Chores.Kind.WATER, 0).equals(Chores.start("eto", Chores.Kind.WATER, 0)), "each companion keeps house in its own words");
    }

    static void home() {
        // 2.0.7: out past home by day (by age), home by night; put home only when stuck.
        check(com.hex.kagune.control.family.HomeRules.roam(null) == com.hex.kagune.control.family.HomeRules.ROAM
            && com.hex.kagune.control.family.HomeRules.roam(com.hex.kagune.control.family.ChildStage.ADULT) == com.hex.kagune.control.family.HomeRules.ROAM
            && com.hex.kagune.control.family.HomeRules.roam(com.hex.kagune.control.family.ChildStage.TEEN) == com.hex.kagune.control.family.HomeRules.ROAM
            && com.hex.kagune.control.family.HomeRules.roam(com.hex.kagune.control.family.ChildStage.CHILD) == com.hex.kagune.control.family.HomeRules.ROAM / 2
            && com.hex.kagune.control.family.HomeRules.roam(com.hex.kagune.control.family.ChildStage.TODDLER) == 0
            && com.hex.kagune.control.family.HomeRules.roam(com.hex.kagune.control.family.ChildStage.BABY) == 0, "how far each may roam past home by day");
        check(com.hex.kagune.control.family.HomeRules.leash(12, false, 24) == 36 && com.hex.kagune.control.family.HomeRules.leash(12, true, 24) == 12,
            "by day the leash reaches out, at night only home");
        boolean roamsWithinStray = true;
        for (com.hex.kagune.control.family.ChildStage stage : com.hex.kagune.control.family.ChildStage.values()) {
            roamsWithinStray &= com.hex.kagune.control.family.HomeRules.roam(stage) <= com.hex.kagune.control.family.Parenting.strays(stage, false);
        }
        check(roamsWithinStray, "a child's own roaming never takes it past where its parent would go after it by day");
        check(!com.hex.kagune.control.family.HomeRules.noHeadway(40.0, 39.0, 0L, 100L)
            && com.hex.kagune.control.family.HomeRules.noHeadway(40.0, 39.0, 0L, com.hex.kagune.control.family.HomeRules.NO_HEADWAY)
            && !com.hex.kagune.control.family.HomeRules.noHeadway(40.0, 37.0, 0L, com.hex.kagune.control.family.HomeRules.NO_HEADWAY)
            && !com.hex.kagune.control.family.HomeRules.noHeadway(40.0, 39.0, Long.MIN_VALUE, 99999L), "put home only after minutes getting no nearer");
        check(!com.hex.kagune.control.family.HomeRules.night(6000L, true) && com.hex.kagune.control.family.HomeRules.night(13000L, true)
            && !com.hex.kagune.control.family.HomeRules.night(13000L, false) && com.hex.kagune.control.family.HomeRules.night(14000L, false)
            && !com.hex.kagune.control.family.HomeRules.night(23500L, true) && com.hex.kagune.control.family.HomeRules.night(24000L * 3 + 18000L, false),
            "children go to bed first, their parent after; everyone is up by morning, whatever the day");
        check(com.hex.kagune.control.family.HomeRules.bedBlock("Block{minecraft:red_bed}") && !com.hex.kagune.control.family.HomeRules.bedBlock("Block{minecraft:bedrock}")
            && com.hex.kagune.control.family.HomeRules.facing("Block{minecraft:red_bed}[facing=east,occupied=false,part=head]") == 1
            && com.hex.kagune.control.family.HomeRules.head("Block{minecraft:red_bed}[facing=east,occupied=false,part=head]")
            && !com.hex.kagune.control.family.HomeRules.head("Block{minecraft:red_bed}[facing=east,occupied=false,part=foot]")
            && com.hex.kagune.control.family.HomeRules.occupied("Block{minecraft:red_bed}[facing=east,occupied=true,part=head]"),
            "a bed, which way it faces, its head and whether someone lies in it, read off its state");
        java.util.List<com.hex.kagune.control.family.HomeRules.Head> heads = new java.util.ArrayList<>();
        // A double bed facing north (two heads side by side along x), a single facing east, and three in a row facing south.
        heads.add(new com.hex.kagune.control.family.HomeRules.Head(0, 64, 0, 0));
        heads.add(new com.hex.kagune.control.family.HomeRules.Head(1, 64, 0, 0));
        heads.add(new com.hex.kagune.control.family.HomeRules.Head(5, 64, 5, 1));
        heads.add(new com.hex.kagune.control.family.HomeRules.Head(10, 64, 0, 2));
        heads.add(new com.hex.kagune.control.family.HomeRules.Head(11, 64, 0, 2));
        heads.add(new com.hex.kagune.control.family.HomeRules.Head(12, 64, 0, 2));
        // Side by side but facing different ways: two singles, never a double.
        heads.add(new com.hex.kagune.control.family.HomeRules.Head(0, 64, 8, 0));
        heads.add(new com.hex.kagune.control.family.HomeRules.Head(1, 64, 8, 2));
        com.hex.kagune.control.family.HomeRules.Plan plan = com.hex.kagune.control.family.HomeRules.pair(heads, 0, 65, 0);
        check(plan.doubles().size() == 2 && plan.singles().size() == 4, "two heads side by side facing the same way are a double bed; the rest single ("
            + plan.doubles().size() + " doubles, " + plan.singles().size() + " singles)");
        boolean noDoubleForKids = true;
        java.util.Set<String> given = new java.util.HashSet<>();
        for (int i = 0; i < 6; i++) {
            com.hex.kagune.control.family.HomeRules.Head bed = com.hex.kagune.control.family.HomeRules.childBed(plan, i);
            if (bed == null) {
                continue;
            }
            for (com.hex.kagune.control.family.HomeRules.Bed d : plan.doubles()) {
                noDoubleForKids &= !bed.equals(d.head()) && !bed.equals(d.pair());
            }
            noDoubleForKids &= given.add(bed.x() + "," + bed.y() + "," + bed.z());
        }
        check(noDoubleForKids && com.hex.kagune.control.family.HomeRules.childBed(plan, 4) == null,
            "each child its own single bed (never the double bed), and none for a child past the last bed");
        check(plan.doubles().get(0).head().x() <= 1, "the double bed nearest home first");
    }

    /** 1.3.21: the stronger its parents, the stronger the child: more health, harder blows. */
    static void might() {
        float weak = com.hex.kagune.control.family.ChildRules.might(0.15F, 0.15F);
        float strong = com.hex.kagune.control.family.ChildRules.might(1.0F, 0.95F);
        float mixed = com.hex.kagune.control.family.ChildRules.might(1.0F, 0.15F);
        check(weak < mixed && mixed < strong && strong <= 1.0F && weak >= 0.0F, "the stronger the parents, the stronger the child");
        check(com.hex.kagune.control.family.ChildRules.might(0.2F, 0.9F) == com.hex.kagune.control.family.ChildRules.might(0.9F, 0.2F),
            "a mother's strength counts as a father's");
        for (com.hex.kagune.control.family.ChildStage stage : com.hex.kagune.control.family.ChildStage.values()) {
            check(com.hex.kagune.control.family.ChildRules.health(stage, strong) > com.hex.kagune.control.family.ChildRules.health(stage, weak)
                && com.hex.kagune.control.family.ChildRules.health(stage, 0.0F) == stage.health, "a strong couple's " + stage + " has more health");
        }
        check(com.hex.kagune.control.family.ChildRules.damage(com.hex.kagune.control.family.ChildStage.TEEN, 3, true, strong)
            > com.hex.kagune.control.family.ChildRules.damage(com.hex.kagune.control.family.ChildStage.TEEN, 3, true, weak)
            && com.hex.kagune.control.family.ChildRules.damage(com.hex.kagune.control.family.ChildStage.BABY, 0, true, 1.0F) == 0.0F,
            "a strong couple's child hits harder (a baby still doesn't hit)");
        check(com.hex.kagune.control.family.ChildRules.playerMight(false, 1000, true) < com.hex.kagune.control.family.ChildRules.playerMight(true, 100, false)
            && com.hex.kagune.control.family.ChildRules.playerMight(true, 1000, false) < com.hex.kagune.control.family.ChildRules.playerMight(true, 1000, true)
            && com.hex.kagune.control.family.ChildRules.playerMight(true, 1000, true) <= 1.0F, "a player's might: a ghoul by mastery, more with a final kakuja");
        check(com.hex.kagune.control.npc.Persona.KEN.might() > com.hex.kagune.control.npc.Persona.ETO.might()
            && com.hex.kagune.control.npc.Persona.ETO.might() > com.hex.kagune.control.npc.Persona.TOUKA.might(), "Ken's children strongest, then Eto's, then Touka's");
    }

    /**
     * 1.3.24: the airplane spin (the owner: it spun "in the opposite direction, and after it's done it spins all the way back"): the
     * parent's body turns the way the child is carried (right: a pose's spin + is left, so negative), smoothly, never past the pose's
     * one-turn limit, and it ends facing the way it began with nothing left to unwind.
     */
    static void spin() {
        com.hex.kagune.control.family.ChildMoment spin = com.hex.kagune.control.family.ChildMoment.SPIN;
        double biggestStep = 0.0;
        boolean sameWay = true;
        boolean withinTurn = true;
        for (double age = 0.0; age <= spin.ticks; age += 0.5) {
            double turn = com.hex.kagune.control.family.ChildMoment.spinTurn(age);
            double next = com.hex.kagune.control.family.ChildMoment.spinTurn(age + 0.5);
            double step = Math.atan2(Math.sin(next - turn), Math.cos(next - turn));
            biggestStep = Math.max(biggestStep, Math.abs(step));
            com.hex.kagune.control.client.animation.RigPose parent = com.hex.kagune.control.client.animation.ChildMomentPose.parent(spin, age,
                com.hex.kagune.control.family.ChildStage.TODDLER, true);
            if (parent != null) {
                withinTurn &= Math.abs(parent.spin) <= Math.PI + 1.0E-4;
                if (Math.abs(turn) > 0.05 && Math.abs(turn) < Math.PI - 0.05) {
                    sameWay &= Math.signum(parent.spin) == -Math.signum(turn);
                }
            }
        }
        check(sameWay, "the airplane: the parent turns the way the child is carried round (to their right)");
        check(withinTurn, "the airplane: the parent's spin never reaches the pose's one-turn limit");
        check(biggestStep < 0.5, "the airplane: no jump in the turn (" + biggestStep + " in half a tick)");
        check(Math.abs(com.hex.kagune.control.family.ChildMoment.spinTurn(0.0)) < 1.0E-9
            && Math.abs(com.hex.kagune.control.family.ChildMoment.spinTurn(spin.ticks)) < 1.0E-6
            && Math.abs(com.hex.kagune.control.family.ChildMoment.spinTurn(spin.ticks - 6.0)) < 0.5,
            "the airplane: it ends facing the way it began, nearly there as it eases out (nothing to unwind)");
    }

    /**
     * 1.3.24: the owner: "NPCs will now attack hostile mobs in a 5 block radius, once starting to attack a hostile mob, they will not stop
     * until it's dead, or gone far away enough". Taken on within five blocks; kept while it is near, however long the fight, while it is
     * being got at; let go far off, chased too far from home, or out of reach a long while; a child's chase shorter.
     */
    static void fights() {
        check(com.hex.kagune.control.npc.FightRules.notices(5.0) && !com.hex.kagune.control.npc.FightRules.notices(5.2)
            && com.hex.kagune.control.npc.FightRules.notices(3.5), "a hostile mob within five blocks is taken on, not one further");
        check(com.hex.kagune.control.npc.FightRules.keeps(12.0, 20.0, 0L, false) && com.hex.kagune.control.npc.FightRules.keeps(20.0, 30.0, 500L, false),
            "a mob backing off out of the old leash (twelve, twenty blocks) is still fought");
        check(!com.hex.kagune.control.npc.FightRules.keeps(25.0, 10.0, 0L, false), "a mob gone far off is let go");
        check(!com.hex.kagune.control.npc.FightRules.keeps(10.0, 33.0, 0L, false), "a companion chases no further than its chase from home");
        check(!com.hex.kagune.control.npc.FightRules.keeps(6.0, 6.0, com.hex.kagune.control.npc.FightRules.STUCK + 1L, false)
            && com.hex.kagune.control.npc.FightRules.keeps(6.0, 6.0, com.hex.kagune.control.npc.FightRules.STUCK, false),
            "a mob it can't get at (up a cliff, behind a wall) is let go after a long while, not for ever stared at");
        check(com.hex.kagune.control.npc.FightRules.keeps(15.0, -1.0, 0L, true) && !com.hex.kagune.control.npc.FightRules.keeps(17.0, -1.0, 0L, true)
            && com.hex.kagune.control.npc.FightRules.keeps(17.0, -1.0, 0L, false), "a child chases a shorter way than a companion; no home asked of it");
        long now = 100000L;
        check(com.hex.kagune.control.npc.FightRules.since(now, Long.MIN_VALUE) == Long.MAX_VALUE
            && !com.hex.kagune.control.npc.FightRules.keeps(1.0, 1.0, com.hex.kagune.control.npc.FightRules.since(now, Long.MIN_VALUE), false)
            && com.hex.kagune.control.npc.FightRules.since(now, now + 50L) == 0L && com.hex.kagune.control.npc.FightRules.since(now, now - 30L) == 30L,
            "the time since it got at it never overflows (never: no fight kept on it)");
    }

    /** A model point turned as a ModelPart turns it (x, then z), pixels, y down: worked out here apart from ChildRules. */
    private static double[] turned(double[] q, double x, double z) {
        double y1 = q[1] * Math.cos(x) - q[2] * Math.sin(x);
        double z1 = q[1] * Math.sin(x) + q[2] * Math.cos(x);
        return new double[] {q[0] * Math.cos(z) - y1 * Math.sin(z), q[0] * Math.sin(z) + y1 * Math.cos(z), z1};
    }

    /**
     * 1.3.21: hand in hand (the owner: "clearly not right"): on the real models (a player's arm at a man's, a woman's and Ken's size; the
     * child's at its stage's size and layout, wide and slim arms) the two hands meet, its body clears their arm, a toddler's big head
     * clears their hand, and every turn is the short way round.
     */
    static void hands() {
        for (com.hex.kagune.control.family.ChildStage stage : new com.hex.kagune.control.family.ChildStage[] {
            com.hex.kagune.control.family.ChildStage.TODDLER, com.hex.kagune.control.family.ChildStage.CHILD, com.hex.kagune.control.family.ChildStage.TEEN}) {
            for (double size : new double[] {1.0, 0.86, 0.94}) {
                for (boolean slim : new boolean[] {false, true}) {
                    String what = stage + " at a " + size + " parent's side" + (slim ? " (slim)" : "");
                    com.hex.kagune.control.family.ChildRules.Hand hand = com.hex.kagune.control.family.ChildRules.hand(stage, size, slim);
                    float parentZ = com.hex.kagune.control.family.ChildRules.handParentZ(stage, size);
                    double sp = 0.9375 / 16.0 * size;
                    double[] p = turned(new double[] {1.0, 10.0, 0.0}, com.hex.kagune.control.family.ChildRules.handParentX(stage), parentZ);
                    double[] theirs = {(5.0 + p[0]) * sp, (22.0 - p[1]) * sp, -p[2] * sp};
                    boolean little = stage.young;
                    double part = little ? 0.5 : 1.0;
                    double k = stage.draw / 16.0;
                    double pivotX = little ? -2.5 : -5.0;
                    double pivotY = little ? 13.0 : slim ? 2.5 : 2.0;
                    double[] q = turned(new double[] {(slim ? -0.5 : -1.0) * part, 10.0 * part, 0.0}, hand.armX(), hand.armZ());
                    double[] its = {hand.side() + (pivotX + q[0]) * k, (24.0 - pivotY - q[1]) * k, -q[2] * k};
                    double miss = Math.sqrt(Math.pow(theirs[0] - its[0], 2) + Math.pow(theirs[1] - its[1], 2) + Math.pow(theirs[2] - its[2], 2));
                    check(miss < 0.01, what + ": the hands meet (" + miss + " apart)");
                    double body = (little ? 2.0 : 4.0) * k;
                    check(hand.side() - body - (theirs[0] + 2.0 * sp) > 0.04, what + ": its body clears their arm");
                    check(Math.abs(hand.armZ()) <= Math.PI && hand.side() > 0.6 && hand.side() < 1.3 && parentZ < 0.0F && parentZ > -0.6F,
                        what + ": a sane arm and place (" + hand.armZ() + ", " + hand.side() + ", their arm " + parentZ + ")");
                    if (little) {
                        // Its head (half a block at a toddler's size, its neck 0.6 up), tipped away 0.4 (handChild), its hair a little out.
                        double above = Math.max(0.0, theirs[1] - 0.03 - stage.draw * 12.0 / 16.0);
                        double near = hand.side() - 4.0 * k * Math.cos(0.4) + above * Math.sin(0.4) - 0.5 * k;
                        check(near > theirs[0] + 2.0 * sp, what + ": its head clears their hand");
                        check(hand.armZ() > 1.5F, what + ": a toddler reaches up to their hand");
                    }
                }
            }
        }
        com.hex.kagune.control.client.animation.RigPose toddler = com.hex.kagune.control.client.animation.ChildMomentPose.handChild(
            com.hex.kagune.control.family.ChildStage.TODDLER, 1.0, false);
        com.hex.kagune.control.client.animation.RigPose teen = com.hex.kagune.control.client.animation.ChildMomentPose.handChild(
            com.hex.kagune.control.family.ChildStage.TEEN, 1.0, false);
        check(toddler.headZ > 0.3F && Math.abs(toddler.headY) < 0.01F && teen.headY > 0.0F && ok(toddler) && ok(teen),
            "hand in hand: a toddler's head tipped away from them, a teen's turned to them (its right)");
    }

    private static boolean ok(com.hex.kagune.control.client.animation.RigPose p) {
        float[] all = {p.headX, p.headY, p.headZ, p.lean, p.drop, p.roll, p.flip, p.spin, p.rightArmX, p.rightArmY, p.rightArmZ, p.leftArmX, p.leftArmY,
            p.leftArmZ, p.rightLegX, p.leftLegX, p.rightKnee, p.leftKnee};
        for (float f : all) {
            if (!Float.isFinite(f) || Math.abs(f) > 13.0F) {
                return false;
            }
        }
        return true;
    }

    private static double lum(int rgb) {
        return 0.299 * ((rgb >> 16) & 255) + 0.587 * ((rgb >> 8) & 255) + 0.114 * (rgb & 255);
    }

    static void looks() {
        int[] base = part("base_1");
        check(base != null && base.length == 64 * 32, "a skin part reads (64 x 32, by the mod's own PNG reader)");
        java.io.File dir = new java.io.File("src/main/resources/assets/hexkagune/textures/entity/child");
        String credits = "";
        try {
            credits = java.nio.file.Files.readString(new java.io.File(dir, "CREDITS.txt").toPath());
        } catch (java.io.IOException missing) {
            credits = "";
        }
        boolean credited = true;
        for (String name : dir.list()) {
            if (name.endsWith(".png") && !name.endsWith("_mask.png")) {
                credited &= credits.contains("\n" + name + "\n") || credits.startsWith(name + "\n");
            }
        }
        check(credited && credits.contains("CC BY-SA"), "every skin part is credited, with its licence");
        com.hex.kagune.control.family.Genome.Parent light = new com.hex.kagune.control.family.Genome.Parent(0x6B4423, 0x3A6EA5, 0xF0D0B8, false, false);
        com.hex.kagune.control.family.Genome.Parent eto = new com.hex.kagune.control.family.Genome.Parent(EtoRules.HAIR, EtoRules.EYES, EtoRules.SKIN, true,
            true);
        java.util.Random random = new java.util.Random(5);
        boolean lightKids = true;
        boolean drawn = true;
        boolean noBeard = true;
        boolean kakuganShows = true;
        for (int i = 0; i < 60; i++) {
            com.hex.kagune.control.family.Genome child = com.hex.kagune.control.family.Genome.of(eto, light, random);
            int skin = child.skin();
            lightKids &= ((skin >> 16) & 255) >= 0xEB - 1 - 40 && ((skin >> 8) & 255) >= 0xD0 - 40 && (skin & 255) >= 0xB8 - 40;
            com.hex.kagune.control.family.ChildStage stage = com.hex.kagune.control.family.ChildStage.values()[i % 4];
            int[] px = com.hex.kagune.control.family.ChildSkin.paint(child, stage, false, FamilyTests::part);
            drawn &= px.length == 64 * 64 && (px[2 * 64 + 11] >>> 24) == 255 && (px[60 * 64 + 22] >>> 24) == 255;
            for (int v = 12; v < 16; v++) {
                for (int u = 9; u < 15; u++) {
                    noBeard &= !near(px[v * 64 + u], child.hair(), 18) || near(px[v * 64 + u], child.skin(), 30);
                }
            }
            if (child.kakugan() != com.hex.kagune.control.family.Genome.NONE) {
                int[] turned = com.hex.kagune.control.family.ChildSkin.paint(child, stage, true, FamilyTests::part);
                boolean red = false;
                for (int v = 8; v < 16; v++) {
                    for (int u = 8; u < 16; u++) {
                        red |= turned[v * 64 + u] == 0xFFE0302A;
                    }
                }
                kakuganShows &= red;
            }
        }
        check(lightKids, "two light-skinned parents: light-skinned children, never darker than the darker parent");
        check(drawn, "a whole skin, the top of the head and the legs painted, at every stage");
        check(noBeard, "no child has a beard: nothing of the hair on the middle of the lower face");
        check(kakuganShows, "its kakugan shows red when it fights");
        int[] skinPx = new int[64 * 64];
        java.util.Arrays.fill(skinPx, 0);
        for (int v = 0; v < 10; v++) {
            for (int u = 8; u < 16; u++) {
                skinPx[v * 64 + u] = 0xFF5A3A22;
            }
        }
        for (int v = 10; v < 16; v++) {
            for (int u = 8; u < 16; u++) {
                skinPx[v * 64 + u] = 0xFFF0D0B8;
            }
        }
        check(near(0xFF000000 | com.hex.kagune.control.family.HairColour.of(skinPx, 64), 0x5A3A22, 20), "the hair read off the top of the head");
        check(near(0xFF000000 | com.hex.kagune.control.family.SkinTone.of(skinPx, 64), 0xF0D0B8, 24), "the skin tone read off the lower face");
        check(com.hex.kagune.control.family.HairColour.of(new int[64 * 64], 64) == -1 && com.hex.kagune.control.family.SkinTone.of(new int[64 * 64], 64) == -1,
            "a blank skin tells nothing (the usual colours are kept)");
    }

    static boolean near(int argb, int rgb, int by) {
        return Math.abs(((argb >> 16) & 255) - ((rgb >> 16) & 255)) <= by && Math.abs(((argb >> 8) & 255) - ((rgb >> 8) & 255)) <= by
            && Math.abs((argb & 255) - (rgb & 255)) <= by;
    }

    static void reactions() {
        com.hex.kagune.control.npc.Display none = com.hex.kagune.control.npc.Display.NONE;
        com.hex.kagune.control.npc.Display eyes = com.hex.kagune.control.npc.Display.EYES;
        com.hex.kagune.control.npc.Display kagune = com.hex.kagune.control.npc.Display.KAGUNE;
        com.hex.kagune.control.npc.Display kakuja = com.hex.kagune.control.npc.Display.KAKUJA;
        com.hex.kagune.control.npc.Display dread = com.hex.kagune.control.npc.Display.DREAD;
        check(kagune.rose(eyes) && kakuja.rose(none) && !eyes.rose(kagune) && !kagune.rose(kagune) && eyes.rose(null) && !none.rose(null),
            "noticed only as it rises: more than before");
        check(!eyes.frightening() && !kagune.frightening() && kakuja.frightening() && dread.frightening(), "a kakuja and worse frighten a stranger");
        // 1.3.64: dread is what they wear now: the Dragon, War or Peace on, a final kakuja (Rinkaku's or Ukaku's); else the rest in order.
        check(com.hex.kagune.control.npc.Display.of(false, false, false, false, false, false) == none
            && com.hex.kagune.control.npc.Display.of(false, false, false, false, false, true) == eyes
            && com.hex.kagune.control.npc.Display.of(false, false, false, false, true, true) == kagune
            && com.hex.kagune.control.npc.Display.of(false, false, false, true, true, true) == kakuja,
            "nothing worn: their kakuja, kagune, eyes or nothing (a Dragon once spent in them is not shown)");
        check(com.hex.kagune.control.npc.Display.of(true, false, false, false, false, false) == dread
            && com.hex.kagune.control.npc.Display.of(false, true, false, false, false, false) == dread
            && com.hex.kagune.control.npc.Display.of(false, false, true, true, true, true) == dread,
            "the Dragon, War or Peace worn, or a final kakuja: dread");
        check(dread.rose(com.hex.kagune.control.npc.Display.of(false, false, false, false, false, false))
            && dread.rose(com.hex.kagune.control.npc.Display.of(false, false, false, true, true, false)),
            "putting War on again after taking it off, or going from the first kakuja to the final one, is noticed again");
        boolean said = true;
        for (com.hex.kagune.control.npc.Display shown : new com.hex.kagune.control.npc.Display[] {eyes, kagune, kakuja, dread}) {
            for (com.hex.kagune.control.npc.EtoVoice voice : com.hex.kagune.control.npc.EtoVoice.values()) {
                for (boolean library : new boolean[] {true, false}) {
                    said &= !EtoLines.saw(shown, voice, library, 7).isEmpty();
                }
            }
            said &= !com.hex.kagune.control.npc.ReactionLines.stranger(shown, 3).isEmpty() && !com.hex.kagune.control.npc.ReactionLines.ally(shown, 3).isEmpty();
            for (com.hex.kagune.control.family.ChildStage stage : com.hex.kagune.control.family.ChildStage.values()) {
                said &= !com.hex.kagune.control.npc.ReactionLines.child(stage, "Papa", shown, 2).isEmpty();
            }
        }
        check(said, "Eto, ghouls and children each have something to say at every sight, whoever they are to them");
        check(EtoLines.saw(none, com.hex.kagune.control.npc.EtoVoice.FRIENDLY, false, 1).isEmpty(), "nothing shown, nothing said");
    }

    /** 1.3.65: the worst sights told apart, and everyone's own reaction to each (War feared most). */
    static void fear() {
        com.hex.kagune.control.npc.FinalSight none = com.hex.kagune.control.npc.FinalSight.NONE;
        com.hex.kagune.control.npc.FinalSight war = com.hex.kagune.control.npc.FinalSight.WAR;
        com.hex.kagune.control.npc.FinalSight peace = com.hex.kagune.control.npc.FinalSight.PEACE;
        com.hex.kagune.control.npc.FinalSight dragon = com.hex.kagune.control.npc.FinalSight.DRAGON;
        com.hex.kagune.control.npc.FinalSight rinkaku = com.hex.kagune.control.npc.FinalSight.RINKAKU;
        com.hex.kagune.control.npc.FinalSight ukaku = com.hex.kagune.control.npc.FinalSight.UKAKU;
        com.hex.kagune.control.king.KingForm noForm = com.hex.kagune.control.king.KingForm.NONE;
        check(com.hex.kagune.control.npc.FinalSight.of(false, noForm, false, false) == none
            && com.hex.kagune.control.npc.FinalSight.of(false, com.hex.kagune.control.king.KingForm.WAR, false, false) == war
            && com.hex.kagune.control.npc.FinalSight.of(false, com.hex.kagune.control.king.KingForm.PEACE, false, false) == peace
            && com.hex.kagune.control.npc.FinalSight.of(true, com.hex.kagune.control.king.KingForm.WAR, true, true) == dragon
            && com.hex.kagune.control.npc.FinalSight.of(false, noForm, true, true) == rinkaku
            && com.hex.kagune.control.npc.FinalSight.of(false, noForm, false, true) == ukaku, "the worst sights told apart, the Dragon first");
        check(war.terror() && dragon.terror() && !peace.terror() && !rinkaku.terror() && !ukaku.terror() && !none.terror() && !none.any()
            && peace.any(), "War and the Dragon are a terror; Peace and the final kakujas frighten, but less");
        check(war.scares() > peace.scares() && peace.scares() == rinkaku.scares() && peace.scares() > 160L && none.scares() == 0L,
            "a wild ghoul runs longest from War (and longer from any final sight than from a kakuja)");
        double[] behind = com.hex.kagune.control.npc.FinalSight.behind(0.0, 0.0, 10.0, 0.0, 1.0);
        check(Math.abs(behind[0] + 1.0) < 1.0E-9 && Math.abs(behind[1]) < 1.0E-9, "a child hides on the far side of the other parent from War");
        double[] onTop = com.hex.kagune.control.npc.FinalSight.behind(3.0, 4.0, 3.0, 4.0, 1.0);
        check(Math.abs(Math.hypot(onTop[0] - 3.0, onTop[1] - 4.0) - 1.0) < 1.0E-9, "right on top of them: still a step behind");
        // Humans see a terror from much further, and run harder.
        check(com.hex.kagune.control.npc.HumanRules.notice(war, true) > com.hex.kagune.control.npc.HumanRules.notice(peace, true)
            && com.hex.kagune.control.npc.HumanRules.notice(peace, false) > com.hex.kagune.control.npc.HumanRules.notice(none, true)
            && com.hex.kagune.control.npc.HumanRules.notice(none, true) == com.hex.kagune.control.npc.HumanRules.NOTICE
            && com.hex.kagune.control.npc.HumanRules.notice(none, false) == 0.0
            && com.hex.kagune.control.npc.HumanRules.TERROR_SAFE > com.hex.kagune.control.npc.HumanRules.TERROR_NOTICE,
            "a human sees War from furthest, a final kakuja further than a kagune or eyes, a ghoul hiding it all not at all; it runs till well out of it");
        check(com.hex.kagune.control.npc.NpcRules.prowls(true, 18000L) && !com.hex.kagune.control.npc.NpcRules.prowls(true, 6000L)
            && !com.hex.kagune.control.npc.NpcRules.prowls(false, 18000L) && com.hex.kagune.control.npc.NpcRules.hides(true, 6000L)
            && !com.hex.kagune.control.npc.NpcRules.hides(true, 18000L) && !com.hex.kagune.control.npc.NpcRules.hides(false, 6000L),
            "1.3.68: a wild ghoul prowls by night (eyes on, hunting humans) and hides by day; one walking with a player does neither");
        check(com.hex.kagune.control.npc.HumanRules.pace(war) > com.hex.kagune.control.npc.HumanRules.pace(peace)
            && com.hex.kagune.control.npc.HumanRules.pace(peace) == com.hex.kagune.control.npc.HumanRules.RUN, "it sprints from a terror");
        // The CCG falters: longest at War, a junior longer than its senior; never at nothing.
        check(com.hex.kagune.control.npc.AgentRules.falter(war, false) > com.hex.kagune.control.npc.AgentRules.falter(war, true)
            && com.hex.kagune.control.npc.AgentRules.falter(war, true) > com.hex.kagune.control.npc.AgentRules.falter(peace, true)
            && com.hex.kagune.control.npc.AgentRules.falter(peace, true) > 0 && com.hex.kagune.control.npc.AgentRules.falter(none, false) == 0,
            "an investigator stands frozen longest at War, a junior longer than its senior");
        check(com.hex.kagune.control.family.ChildCue.COWER.ordinal() == com.hex.kagune.control.family.ChildCue.COUNT.ordinal() + 1
            && com.hex.kagune.control.family.ChildCue.COWER.ordinal() == com.hex.kagune.control.family.ChildCue.values().length - 1
            && com.hex.kagune.control.family.ChildCue.COWER.running(600), "the cowering cue: appended (sent by ordinal), held while it hides");
        boolean posed = true;
        for (com.hex.kagune.control.family.ChildStage stage : com.hex.kagune.control.family.ChildStage.values()) {
            for (boolean carried : new boolean[] {false, true}) {
                posed &= com.hex.kagune.control.client.animation.ChildTalkPose.pose(com.hex.kagune.control.family.ChildCue.COWER, 30.0, stage, carried) != null;
            }
        }
        check(posed, "a child cowers at every age, on its feet or sat");
        // Every line, every sight: something to say, in character, no names that must stay out, every blank filled.
        java.util.regex.Pattern banned = java.util.regex.Pattern.compile("Kaneki|Ayato|Furuta|Takatsuki|Aogiri|Yoshimura|\\{|\\}|null",
            java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.Set<String> lines = new java.util.HashSet<>();
        int said = 0;
        java.util.List<String> bad = new java.util.ArrayList<>();
        for (com.hex.kagune.control.npc.FinalSight sight : com.hex.kagune.control.npc.FinalSight.values()) {
            for (int seed = 0; seed < 12; seed++) {
                java.util.List<String> these = new java.util.ArrayList<>();
                these.add(com.hex.kagune.control.npc.FearLines.scream(sight, seed));
                if (sight.any()) {
                    these.add(com.hex.kagune.control.npc.FearLines.stranger(sight, seed));
                    these.add(com.hex.kagune.control.npc.FearLines.ally(sight, seed));
                    for (boolean senior : new boolean[] {true, false}) {
                        these.add(com.hex.kagune.control.npc.FearLines.falter(sight, senior, seed));
                    }
                    for (com.hex.kagune.control.family.ChildStage stage : com.hex.kagune.control.family.ChildStage.values()) {
                        String child = com.hex.kagune.control.npc.FearLines.child(stage, "Papa", sight, seed);
                        these.add(child);
                        if (child.contains("{") || stage != com.hex.kagune.control.family.ChildStage.BABY && sight == war && seed == 1
                            && stage == com.hex.kagune.control.family.ChildStage.CHILD && !child.contains("Papa")) {
                            bad.add("unfilled: " + child);
                        }
                    }
                    for (com.hex.kagune.control.npc.Persona persona : com.hex.kagune.control.npc.Persona.ALL) {
                        for (com.hex.kagune.control.npc.EtoVoice voice : com.hex.kagune.control.npc.EtoVoice.values()) {
                            for (boolean home : new boolean[] {true, false}) {
                                these.add(persona.script().dread(sight, voice, home, seed));
                            }
                        }
                        for (boolean ours : new boolean[] {true, false}) {
                            if (persona.script().dreadGesture(sight, ours) == com.hex.kagune.control.npc.EtoGesture.NONE) {
                                bad.add(persona.key() + " does nothing at " + sight);
                            }
                        }
                    }
                } else {
                    check(com.hex.kagune.control.npc.FearLines.stranger(sight, seed).isEmpty() && com.hex.kagune.control.npc.FearLines.ally(sight, seed).isEmpty()
                        && com.hex.kagune.control.npc.FearLines.falter(sight, true, seed).isEmpty()
                        && com.hex.kagune.control.npc.Persona.ETO.script().dread(sight, com.hex.kagune.control.npc.EtoVoice.CLOSE, false, seed).isEmpty(),
                        "nothing worst shown, nothing worst said");
                }
                if (sight.terror()) {
                    these.add(com.hex.kagune.control.npc.FearLines.fleeing(sight, seed));
                } else {
                    check(com.hex.kagune.control.npc.FearLines.fleeing(sight, seed).isEmpty(), "only a terror is shouted about on the run");
                }
                for (String line : these) {
                    said++;
                    lines.add(line);
                    if (line == null || line.isBlank() || banned.matcher(line).find()) {
                        bad.add(sight + ": [" + line + "]");
                    }
                }
            }
        }
        for (com.hex.kagune.control.family.ChildStage stage : com.hex.kagune.control.family.ChildStage.values()) {
            for (String other : new String[] {"Mama", ""}) {
                for (int seed = 0; seed < 6; seed++) {
                    String line = com.hex.kagune.control.npc.FearLines.hiding(stage, "Papa", other, seed);
                    lines.add(line);
                    said++;
                    if (line.isBlank() || banned.matcher(line).find() || other.isEmpty() && line.contains("Mama")) {
                        bad.add("hiding: [" + line + "]");
                    }
                }
            }
        }
        for (boolean senior : new boolean[] {true, false}) {
            for (int seed = 0; seed < 6; seed++) {
                String line = com.hex.kagune.control.npc.FearLines.rally(senior, seed);
                lines.add(line);
                said++;
                if (line.isBlank() || banned.matcher(line).find()) {
                    bad.add("rally: [" + line + "]");
                }
            }
        }
        check(bad.isEmpty(), "every fear line set says something, in character, nothing that must stay out: " + bad);
        check(lines.size() >= 190, "a good many different words at the worst sights (" + lines.size() + " different of " + said + ")");
        check(!com.hex.kagune.control.npc.Persona.ETO.script().dread(war, com.hex.kagune.control.npc.EtoVoice.CLOSE, false, 0)
            .equals(com.hex.kagune.control.npc.Persona.TOUKA.script().dread(war, com.hex.kagune.control.npc.EtoVoice.CLOSE, false, 0))
            && com.hex.kagune.control.npc.Persona.ETO.script().dreadGesture(war, false) != com.hex.kagune.control.npc.Persona.TOUKA.script().dreadGesture(war, false)
            && com.hex.kagune.control.npc.Persona.TOUKA.script().dreadGesture(war, false) == com.hex.kagune.control.npc.EtoGesture.FLINCH
            && com.hex.kagune.control.npc.Persona.KEN.script().dreadGesture(peace, false) == com.hex.kagune.control.npc.EtoGesture.AWE,
            "each companion their own way: Eto studies War, Touka flinches from it, Ken stands in awe of Peace");
    }

    static void initiative() {
        long every = com.hex.kagune.control.npc.EtoInitiative.EVERY;
        com.hex.kagune.control.npc.EtoInitiative.Act none = com.hex.kagune.control.npc.EtoInitiative.Act.NONE;
        check(com.hex.kagune.control.npc.EtoInitiative.pick(EtoMood.CLINGY, 3.0, true, every * 2, 0.0, 0.0) == none
            && com.hex.kagune.control.npc.EtoInitiative.pick(EtoMood.CLINGY, 30.0, false, every * 2, 0.0, 0.0) == none
            && com.hex.kagune.control.npc.EtoInitiative.pick(EtoMood.CLINGY, 3.0, false, every - 1, 0.0, 0.0) == none,
            "nothing started while she already holds on, from afar, or too soon after the last");
        check(com.hex.kagune.control.npc.EtoInitiative.pick(EtoMood.CLINGY, 3.0, false, every, 0.0, 0.0)
            == com.hex.kagune.control.npc.EtoInitiative.Act.HAND, "clingy, close, unasked: she takes your hand");
        check(com.hex.kagune.control.npc.EtoInitiative.pick(EtoMood.SULKY, 3.0, false, every, 0.0, 0.0) == none
            && com.hex.kagune.control.npc.EtoInitiative.pick(EtoMood.CLAIMED, 3.0, false, every, 0.0, 0.0) == none,
            "sulking (or someone else's), she starts nothing");
        check(com.hex.kagune.control.npc.EtoInitiative.pick(EtoMood.HAPPY, 3.0, false, every, 0.5, 0.0) == none,
            "most moments, nothing: it stays a surprise");
        boolean lines = true;
        for (com.hex.kagune.control.npc.EtoInitiative.Act act : com.hex.kagune.control.npc.EtoInitiative.Act.values()) {
            lines &= act == none || !com.hex.kagune.control.npc.EtoInitiative.line(act, 5).isEmpty();
        }
        for (com.hex.kagune.control.npc.EtoInitiative.Moment moment : com.hex.kagune.control.npc.EtoInitiative.Moment.values()) {
            lines &= !com.hex.kagune.control.npc.EtoInitiative.moment(moment, true, 3).isEmpty()
                && !com.hex.kagune.control.npc.EtoInitiative.moment(moment, false, 3).isEmpty();
        }
        check(lines && !EtoLines.owl(2).isEmpty(), "a word for everything she starts and every moment she remarks on");
    }

    /**
     * 1.3.37: a sleeper woken in the night only half wakes: every spouse and every child has drowsy words (a baby only sleeps on); the
     * page's answers each have their reply; a hand on the shoulder doesn't get a spouse up unless asked (HomeLife reads drowsyUntil),
     * and a CCG investigator by the house is a threat a parent gets up for (EtoBrain.guard).
     */
    static void drowsy() throws java.io.IOException {
        for (String persona : new String[] {"eto", "touka", "ken"}) {
            for (int seed = 0; seed < 12; seed++) {
                for (String line : new String[] {com.hex.kagune.control.family.Drowsy.spouse(persona, seed),
                    com.hex.kagune.control.family.Drowsy.settle(persona, seed), com.hex.kagune.control.family.Drowsy.kissed(persona, seed),
                    com.hex.kagune.control.family.Drowsy.roused(persona, seed)}) {
                    check(line != null && !line.isBlank() && !line.contains("Kaneki") && !line.contains("Takatsuki"), persona + " says something, drowsy: " + line);
                }
            }
        }
        for (com.hex.kagune.control.family.ChildStage stage : com.hex.kagune.control.family.ChildStage.values()) {
            for (int seed = 0; seed < 8; seed++) {
                String line = com.hex.kagune.control.family.Drowsy.child(stage, seed);
                check(line != null && !line.isBlank(), stage + " stirs");
                if (stage == com.hex.kagune.control.family.ChildStage.BABY) {
                    check(line.startsWith("*") && line.endsWith("*"), "a baby sleeps on (no words): " + line);
                }
                check(!com.hex.kagune.control.family.Drowsy.childSettles(stage, seed % 2 == 0, seed).isBlank(), stage + " settles back");
            }
        }
        String life = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/server/HomeLife.java"));
        check(life.contains("mind.talking != null && now >= mind.drowsyUntil"), "a hand on her shoulder only half wakes her");
        String brain = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/server/EtoBrain.java"));
        check(brain.contains("body instanceof com.hex.kagune.control.npc.CcgAgent"), "an investigator by the house is a threat a parent gets up for");
        String server = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/server/EtoServer.java"));
        check(server.contains("\"sleep:back\"") && server.contains("\"sleep:kiss\"") && server.contains("\"sleep:up\""), "the drowsy page's three answers");
    }

    /**
     * 1.3.39, the owner's bugs: anyone held (hand in hand, in arms, on shoulders) passes through blocks and is never choked in one, and is
     * set down clear of a wall; a talk holds a companion, a child and a wild ghoul as long as it is open on the player's screen; a wild
     * ghoul hit by a player in creative fights back (its kagune out).
     */
    static void held() throws java.io.IOException {
        String eto = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/server/EtoBrain.java"));
        String child = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/server/ChildBrain.java"));
        String ghoul = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/server/GhoulNpcBrain.java"));
        int hands = eto.indexOf("private static void holdHands");
        check(hands > 0 && eto.indexOf("npc.f_19794_ = true;", hands) > hands && eto.indexOf("npc.f_19794_ = true;", hands) < eto.indexOf("private static void", hands + 30),
            "hand in hand, a companion passes through blocks");
        check(eto.contains("static void solid(") && eto.contains("solid(npc, player);"), "let go, she is solid again, set down clear of a wall");
        check(child.contains("npc.f_19794_ = true;") && child.contains("EtoBrain.solid(npc, held);"), "a held child passes through blocks; set down, it is solid");
        check(eto.contains("EtoServer.talkingWith(open, npc)") && eto.contains("npc.mind.talking != null"), "a companion stays for a talk as long as it is open");
        check(child.contains("ChildTalk.talker(npc, level) != null"), "a child stands still while talked to");
        check(ghoul.contains("GhoulNpcTalk.talkingWith(player, npc)"), "a wild ghoul stays for a talk as long as it is open");
        int grudge = ghoul.indexOf("Long until = mind.grudges.get(body.m_20148_());");
        int creative = ghoul.indexOf("if (body instanceof Player player && player.m_7500_()) {");
        check(grudge > 0 && creative > grudge, "a ghoul hit by a player in creative fights back (the grudge first)");
    }
}
