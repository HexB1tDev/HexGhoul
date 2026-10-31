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
}
