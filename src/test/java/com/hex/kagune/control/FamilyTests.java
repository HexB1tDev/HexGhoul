package com.hex.kagune.control;

import com.hex.kagune.control.client.GenderView;
import com.hex.kagune.control.client.PathChoiceLayout.Box;
import com.hex.kagune.control.npc.EtoRules;
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
}
