package com.hex.kagune.control;

import com.hex.kagune.control.client.GenderView;
import com.hex.kagune.control.client.PathChoiceLayout.Box;
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

    public static void main(String[] args) {
        genders();
        body();
        screen();
        pregnancy();
        genome();
        talk();
        children();
        looks();
        ownerSkins();
        reactions();
        initiative();
        if (!FAILURES.isEmpty()) {
            FAILURES.stream().limit(40).forEach(failure -> System.err.println("FAILED: " + failure));
            System.err.println(FAILURES.size() + " of " + (passed + FAILURES.size()) + " checks failed");
            System.exit(1);
        }
        System.out.println("Family tests: " + passed + " checks passed");
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
            == com.hex.kagune.control.family.ChildStage.CHILD && com.hex.kagune.control.family.ChildStage.of(50.0) == teen,
            "a baby, a toddler, a child, a teen, by its age in days");
        float last = 0.0F;
        boolean growing = true;
        for (com.hex.kagune.control.family.ChildStage stage : com.hex.kagune.control.family.ChildStage.values()) {
            growing &= stage.box > last && stage.box < 1.0F && stage.health > 0.0F;
            last = stage.box;
        }
        check(growing, "each stage bigger than the last, all smaller than a grown player");
        check(baby.sits() && baby.carried() && !baby.walks() && !baby.learns() && com.hex.kagune.control.family.ChildStage.TODDLER.carried()
            && com.hex.kagune.control.family.ChildStage.TODDLER.walks() && !com.hex.kagune.control.family.ChildStage.CHILD.carried()
            && com.hex.kagune.control.family.ChildStage.CHILD.learns() && teen.learns() && teen.next() == teen,
            "a baby sits and is carried; a toddler walks and can still be carried; a child and a teen can be taught");
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
}
