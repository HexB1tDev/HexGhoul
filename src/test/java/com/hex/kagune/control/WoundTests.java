package com.hex.kagune.control;

import com.hex.kagune.control.wound.Regeneration;
import com.hex.kagune.control.wound.Wound;
import com.hex.kagune.control.wound.WoundBook;
import com.hex.kagune.control.wound.WoundShape;
import java.util.ArrayList;
import java.util.List;

/** 3.0.0: wounds that follow a player's health and are carved into their body, and a ghoul's regeneration. */
public final class WoundTests {
    private static int passed;
    private static final List<String> FAILURES = new ArrayList<>();

    private static void check(boolean condition, String description) {
        if (condition) {
            passed++;
        } else {
            FAILURES.add(description);
        }
    }

    public static void main(String[] args) {
        regeneration();
        book();
        shapes();
        spots();
        blows();
        if (!FAILURES.isEmpty()) {
            FAILURES.stream().limit(40).forEach(failure -> System.err.println("FAIL: " + failure));
            System.err.println(FAILURES.size() + " of " + (passed + FAILURES.size()) + " checks failed");
            System.exit(1);
        }
        System.out.println("Wound tests: " + passed + " checks passed");
    }

    /** 3.0.3: Rinkaku's tails shape the wounds their hits open. */
    private static void blows() {
        WoundBook.Spot chest = new WoundBook.Spot(Wound.Part.BODY, Wound.Face.FRONT, 4.0, 5.0);
        WoundBook slash = new WoundBook();
        slash.health(20.0, 20.0, 1, 100L);
        slash.blow(WoundBook.Blow.SLASH, chest, Math.PI * 0.5, 100L);
        slash.health(14.0, 20.0, 2, 102L);
        Wound gash = slash.wounds().get(0);
        check(gash.part() == Wound.Part.BODY && gash.face() == Wound.Face.FRONT && gash.kind() == Wound.Kind.GASH
            && Math.abs(gash.angle() - Math.PI * 0.5) < 1.0E-9 && gash.length() > 4.0 && Math.abs(gash.v() - 5.0) < 1.0E-9,
            "a slash leaves a long gash where it struck, along the swing: " + gash);
        WoundBook stab = new WoundBook();
        stab.health(20.0, 20.0, 1, 100L);
        stab.blow(WoundBook.Blow.STAB, chest, 0.0, 100L);
        stab.health(15.0, 20.0, 2, 101L);
        check(stab.wounds().get(0).kind() == Wound.Kind.HOLE && stab.wounds().get(0).part() == Wound.Part.BODY, "a thrust leaves a hole");
        WoundBook arrow = new WoundBook();
        arrow.health(20.0, 20.0, 1, 100L);
        arrow.blow(WoundBook.Blow.STAB, new WoundBook.Spot(Wound.Part.RIGHT_ARM, Wound.Face.FRONT, 2.0, 5.0), 0.0, 100L);
        arrow.health(18.0, 20.0, 2, 101L);
        Wound hole = arrow.wounds().get(0);
        check(hole.kind() == Wound.Kind.HOLE && hole.part() == Wound.Part.RIGHT_ARM && hole.width() >= 2.5,
            "6.0.11: even a light arrow leaves a hole plain to see where it went in: " + hole);
        WoundBook bite = new WoundBook();
        bite.health(20.0, 20.0, 1, 100L);
        bite.blow(WoundBook.Blow.MAUL, new WoundBook.Spot(Wound.Part.BODY, Wound.Face.FRONT, 4.0, 6.0), 0.3, 100L);
        bite.health(17.0, 20.0, 2, 101L);
        long teeth = bite.wounds().stream().filter(w -> w.kind() == Wound.Kind.HOLE && w.part() == Wound.Part.BODY
            && Math.hypot(w.u() - 4.0, w.v() - 6.0) < 5.0).count();
        Wound torn = bite.wounds().get(0);
        check(torn.kind() == Wound.Kind.TEAR && torn.length() >= 4.5 && torn.width() >= 2.4 && teeth >= 3
            && bite.wounds().stream().anyMatch(w -> w.kind() == Wound.Kind.CUT),
            "6.0.11: a zombie's bite tears the flesh open, the teeth's punctures round it and a claw's rakes: " + bite.wounds().size());
        WoundBook bitten = new WoundBook();
        bitten.health(20.0, 20.0, 1, 100L);
        bitten.blow(WoundBook.Blow.MAUL, new WoundBook.Spot(Wound.Part.LEFT_LEG, Wound.Face.FRONT, 2.0, 6.0), 0.0, 100L);
        bitten.health(19.0, 20.0, 2, 101L);
        boolean inside = !bitten.wounds().isEmpty();
        for (Wound w : bitten.wounds()) {
            double[] side = w.face().size(w.part().box(false));
            inside &= w.u() > 0.0 && w.u() < side[0] && w.v() > 0.0 && w.v() < side[1];
        }
        check(inside, "a bite on a narrow leg stays on the leg");
        WoundBook crush = new WoundBook();
        crush.health(20.0, 20.0, 1, 100L);
        crush.blow(WoundBook.Blow.CRUSH, null, 0.0, 100L);
        crush.health(18.0, 20.0, 2, 103L);
        boolean round = crush.wounds().size() >= 3;
        for (Wound w : crush.wounds()) {
            round &= (w.kind() == Wound.Kind.TEAR || w.kind() == Wound.Kind.BRUISE) && (w.part() == Wound.Part.BODY || w.part() == Wound.Part.RIGHT_ARM
                || w.part() == Wound.Part.LEFT_ARM);
        }
        check(round, "a crush tears the torso and arms, even a light one: " + crush.wounds().size());
        WoundBook slam = new WoundBook();
        slam.health(20.0, 20.0, 1, 100L);
        slam.blow(WoundBook.Blow.SLAM, null, 0.0, 100L);
        slam.health(17.0, 20.0, 2, 104L);
        boolean back = slam.wounds().size() >= 4;
        WoundBook plain = new WoundBook();
        plain.health(20.0, 20.0, 1, 100L);
        plain.health(17.0, 20.0, 2, 104L);
        double biggest = 0.0;
        for (Wound w : slam.wounds()) {
            back &= w.face() == Wound.Face.BACK;
            biggest = Math.max(biggest, w.length() * w.width());
        }
        double plainBiggest = 0.0;
        for (Wound w : plain.wounds()) {
            plainBiggest = Math.max(plainBiggest, w.length() * w.width());
        }
        check(back && biggest > plainBiggest, "a slam tears the back, bigger than a plain hit of the same cost");
        WoundBook late = new WoundBook();
        late.health(20.0, 20.0, 1, 100L);
        late.blow(WoundBook.Blow.SLAM, null, 0.0, 100L);
        late.health(17.0, 20.0, 2, 100L + WoundBook.BLOW_TICKS + 5L);
        check(late.wounds().size() < 4 || late.wounds().stream().anyMatch(w -> w.face() != Wound.Face.BACK), "a blow that never landed expires");
        WoundBook once = new WoundBook();
        once.health(20.0, 20.0, 1, 100L);
        once.blow(WoundBook.Blow.CRUSH, null, 0.0, 100L);
        once.health(18.0, 20.0, 2, 101L);
        int after = once.wounds().size();
        once.health(17.5, 20.0, 3, 102L);
        check(once.wounds().size() - after < 3, "a blow shapes one hit only");
    }

    private static void regeneration() {
        check(Regeneration.waitTicks(0, false) == 300, "a new ghoul's wounds start closing after 15 seconds");
        check(Regeneration.waitTicks(0, false) + Regeneration.healTicks(0) == 400, "and are healed 5 seconds later");
        check(Regeneration.waitTicks(1000, false) + Regeneration.healTicks(1000) == 140, "at mastery 1000 full health within 7 seconds of the hit");
        check(Regeneration.waitTicks(500, true) < Regeneration.waitTicks(500, false), "the Regenerative Reservoir shortens the wait");
        int previous = Integer.MAX_VALUE;
        for (int m = 0; m <= 1000; m += 50) {
            int total = Regeneration.waitTicks(m, false) + Regeneration.healTicks(m);
            check(total <= previous, "mastery only ever speeds it up (" + m + ")");
            previous = total;
        }
        check(Regeneration.healThisTick(Regeneration.waitTicks(300, false) - 1, 300, false, 10.0F) == 0.0F, "nothing heals before the wait is over");
        for (int m : new int[] {0, 400, 1000}) {
            float missing = 13.0F;
            float healed = 0.0F;
            int wait = Regeneration.waitTicks(m, false);
            for (int t = 0; t < wait + Regeneration.healTicks(m); t++) {
                healed += Regeneration.healThisTick(t, m, false, missing);
            }
            check(Math.abs(healed - missing) < 1.0E-3, "everything missing comes back by the end (mastery " + m + "): " + healed);
        }
        check(Regeneration.natural(1.0F, false, false), "food's natural regeneration (a point) no longer heals a ghoul");
        check(Regeneration.natural(0.5F, false, false), "nor its saturation trickle");
        check(!Regeneration.natural(4.0F, false, false), "a potion of healing still does");
        check(!Regeneration.natural(1.0F, true, false), "a regeneration effect still does");
        check(!Regeneration.natural(1.0F, false, true), "and this regeneration does");
    }

    private static void book() {
        WoundBook book = new WoundBook();
        book.health(20.0, 20.0, 1);
        check(book.wounds().isEmpty(), "a healthy player has no wounds");
        book.health(18.0, 20.0, 2);
        int light = book.wounds().size();
        check(light >= 1, "a light hit opens a wound");
        book.health(8.0, 20.0, 3);
        int heavy = book.wounds().size() - light;
        check(heavy > light, "a heavy hit opens more (" + heavy + " against " + light + ")");
        check(book.wounds().stream().allMatch(w -> book.openness(w) > 0.99), "fresh wounds are open");
        List<Wound> before = new ArrayList<>(book.wounds());
        book.health(13.0, 20.0, 4);
        Wound first = before.get(0);
        Wound last = before.get(before.size() - 1);
        check(book.openness(last) < book.openness(first) || !book.wounds().contains(last), "regenerating closes the newest wounds first");
        check(book.wounds().contains(first) && book.openness(first) > 0.99, "the first wounds stay open until their health is back");
        book.health(19.0, 20.0, 5);
        check(book.wounds().size() < before.size(), "healed wounds are gone");
        book.health(20.0, 20.0, 6);
        check(book.wounds().isEmpty(), "full health, no wounds");
        for (int i = 0; i < 40; i++) {
            book.health(20.0 - (i % 19) - 1, 20.0, 100 + i);
            book.health(20.0 - (i % 19) - 0.5, 20.0, 200 + i);
        }
        check(book.wounds().size() <= WoundBook.MOST, "a body holds at most " + WoundBook.MOST + " wounds");
        book.health(0.0, 20.0, 7);
        check(book.wounds().isEmpty(), "death clears the body");
        book.health(10.0, 20.0, 8);
        int cuts = book.wounds().size();
        book.puncture(Wound.Part.BODY, Wound.Face.FRONT, 4.0, 5.0, 9);
        check(book.wounds().size() == cuts + 1 && book.wounds().get(cuts).kind() == Wound.Kind.HOLE, "a spike leaves a puncture");
        book.health(20.0, 20.0, 10);
        check(book.wounds().isEmpty(), "punctures close with the rest");
    }

    private static void shapes() {
        for (boolean slim : new boolean[] {false, true}) {
            WoundBook book = new WoundBook();
            book.slim(slim);
            for (int seed = 0; seed < 400; seed++) {
                book.health(20.0, 20.0, seed);
                book.health(20.0 - 2.0 - seed % 15, 20.0, seed * 13 + 5);
                if (seed % 5 == 0) {
                    book.puncture(Wound.Part.values()[seed % 6], Wound.Face.values()[seed % 4], 1.5 + seed % 2, 3.0 + seed % 5, seed);
                }
                for (Wound w : book.wounds()) {
                    double[] box = w.part().box(slim);
                    for (double open : new double[] {1.0, 0.5, 0.1}) {
                        String label = w.kind() + " on " + w.part() + " " + w.face() + " seed " + seed + " open " + open;
                        check(w.part() != Wound.Part.HEAD || w.face() != Wound.Face.FRONT || w.kind() == Wound.Kind.HOLE, "never a wound over the face: " + label);
                        double thickness = w.face().thickness(box);
                        double[] normal = w.face().normal();
                        double[] origin = w.face().point(box, 0, 0, 0);
                        double[] count = new double[WoundShape.Role.values().length];
                        WoundShape.build(w, open, box, (role, points, argb) -> {
                            count[role.ordinal()]++;
                            for (double[] p : points) {
                                // Height above the side's surface, along its outward normal.
                                double out = (p[0] - origin[0]) * normal[0] + (p[1] - origin[1]) * normal[1] + (p[2] - origin[2]) * normal[2];
                                // A broken bone stands out of the skin.
                                double out2 = role == WoundShape.Role.BONE ? 1.0 + WoundShape.SHARD_OUT : 1.0;
                                boolean inside = p[0] >= box[0] - out2 && p[0] <= box[3] + out2 && p[1] >= box[1] - 1.0E-6 && p[1] <= box[4] + 1.0E-6
                                    && p[2] >= box[2] - out2 && p[2] <= box[5] + out2;
                                check(inside, "stays on its part: " + label);
                                switch (role) {
                                    case OPENING -> check(Math.abs(out - w.part().outer()) < 1.0E-6, "the opening lies on the outer layer: " + label);
                                    case FLOOR -> check(out < 0.0 && out > -thickness * 0.5, "the floor lies inside, never through: " + label);
                                    case WALL -> check(out <= w.part().outer() + 1.0E-6 && out > -thickness * 0.5, "the walls run down inside: " + label);
                                    case BONE -> check(w.kind() == Wound.Kind.FRACTURE || out < 0.0, "bones and guts lie inside: " + label);
                                    default -> check(out > w.part().outer(), "rims and blood lie over the skin: " + label);
                                }
                            }
                        });
                        check(!WoundShape.opens(w.kind()) || count[WoundShape.Role.OPENING.ordinal()] == 1 && count[WoundShape.Role.FLOOR.ordinal()] >= 1
                            && count[WoundShape.Role.WALL.ordinal()] >= 8, "a wound has an opening, a floor and walls: " + label);
                        for (double[] uv : WoundShape.outline(w, open, box)) {
                            double[] side = w.face().size(box);
                            check(uv[0] >= 0.0 && uv[0] <= side[0] && uv[1] >= 0.0 && uv[1] <= side[1], "the opening stays on its side: " + label);
                        }
                    }
                }
            }
        }
        // Closing narrows the opening.
        Wound tear = new Wound(Wound.Part.BODY, Wound.Face.FRONT, Wound.Kind.TEAR, 4.0, 6.0, 0.3, 3.0, 1.4, 5, 0.5, 0.3);
        double[] box = Wound.Part.BODY.box(false);
        check(area(WoundShape.outline(tear, 0.3, box)) < area(WoundShape.outline(tear, 1.0, box)) * 0.5, "a closing wound narrows");
        double[] none = new double[1];
        WoundShape.build(tear, 0.0, box, (role, points, argb) -> none[0]++);
        check(none[0] == 0, "a closed wound draws nothing");
    }

    /** Where a spike stuck in a body goes in, from its spot in the body's frame. */
    private static void spots() {
        // Chest height, a little right of centre, from the front, pointing back into the body.
        WoundBook.Spot chest = WoundBook.spot(0.1, 1.15, -0.05, 0.0, 0.0, -1.0, false);
        check(chest.part() == Wound.Part.BODY && chest.face() == Wound.Face.FRONT, "a spike in the chest from the front: " + chest);
        WoundBook.Spot back = WoundBook.spot(-0.05, 1.0, 0.05, 0.0, -0.2, 1.0, false);
        check(back.part() == Wound.Part.BODY && back.face() == Wound.Face.BACK, "a spike in the back from behind: " + back);
        WoundBook.Spot arm = WoundBook.spot(0.36, 1.1, 0.0, -1.0, 0.0, 0.0, false);
        check(arm.part() == Wound.Part.RIGHT_ARM && arm.face() == Wound.Face.RIGHT, "a spike in the right arm from the right: " + arm);
        WoundBook.Spot leg = WoundBook.spot(-0.1, 0.4, -0.05, 0.0, 0.0, -1.0, false);
        check(leg.part() == Wound.Part.LEFT_LEG && leg.face() == Wound.Face.FRONT, "a spike in the left shin from the front: " + leg);
        WoundBook.Spot head = WoundBook.spot(0.0, 1.65, -0.1, 0.1, 0.0, -1.0, false);
        check(head.part() == Wound.Part.HEAD && head.face() != Wound.Face.FRONT, "never over the face: " + head);
        for (WoundBook.Spot spot : new WoundBook.Spot[] {chest, back, arm, leg, head}) {
            double[] size = spot.face().size(spot.part().box(false));
            check(spot.u() >= 0.0 && spot.u() <= size[0] && spot.v() >= 0.0 && spot.v() <= size[1], "the spot is on its side: " + spot);
        }
    }

    private static double area(double[][] polygon) {
        double sum = 0.0;
        for (int i = 0; i < polygon.length; i++) {
            double[] a = polygon[i];
            double[] b = polygon[(i + 1) % polygon.length];
            sum += a[0] * b[1] - b[0] * a[1];
        }
        return Math.abs(sum) * 0.5;
    }
}
