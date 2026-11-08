package com.hex.kagune.control;

import com.hex.kagune.control.bikaku.MawMotion;
import com.hex.kagune.control.bikaku.MawWorm;
import com.hex.kagune.control.ukaku.KakujaModel;
import com.hex.kagune.control.ukaku.UkakuModel;

/**
 * 3.4.9: the Bikaku path's worm (MawWorm, MawMotion; in the game since 3.4.9). Pure, off-game checks of its shape, its mouth, its colours
 * and how it moves: at rest, through each bite of the M1 string, growing out, and in its owner's own first person.
 */
public final class BikakuWormTests {
    private static int passed;

    private static void check(boolean good, String message) {
        if (!good) {
            throw new AssertionError(message);
        }
        passed++;
    }

    public static void main(String[] args) {
        shape();
        mouth();
        colours();
        motion();
        string();
        growing();
        firstPerson();
        grabs();
        pursuit();
        eruption();
        kakuja();
        System.out.println("Bikaku worm tests: " + passed + " checks passed");
    }

    /** At rest it is long (the owner: "longer than all the others ... zigzags"), rooted in the lower back, every quad finite. */
    private static void shape() {
        for (double time : new double[] {0, 37, 120, 333}) {
            MawMotion.Shape s = MawMotion.idle(time, 7);
            KakujaModel.Spine spine = MawWorm.spine(s.path(), s.dorsal(), 60);
            check(spine.length() > 7.0, "the worm is about eight blocks long at rest (" + spine.length() + ")");
            double[] root = s.path()[0];
            check(Math.abs(root[0] - MawMotion.ROOT[0]) < 1.0E-9 && Math.abs(root[1] - MawMotion.ROOT[1]) < 1.0E-9
                && Math.abs(root[2] - MawMotion.ROOT[2]) < 1.0E-9, "it always leaves the body at the lower back");
            // It zigzags: its coils swing from side to side (x crosses back and forth).
            int crossings = 0;
            for (int i = 3; i < s.path().length - 3; i++) {
                if (Math.signum(s.path()[i][0]) != Math.signum(s.path()[i + 1][0])) {
                    crossings++;
                }
            }
            check(crossings >= 3, "its coils zigzag from side to side behind him");
            int[] quads = {0};
            boolean[] finite = {true};
            UkakuModel.Sink sink = (a, b, c, d, n, ca, cb, cc, cd) -> {
                quads[0]++;
                for (double[] p : new double[][] {a, b, c, d, n}) {
                    finite[0] &= Double.isFinite(p[0]) && Double.isFinite(p[1]) && Double.isFinite(p[2]);
                }
            };
            MawWorm.build(sink, new MawWorm.Pose(s.path(), s.dorsal(), 1.0, s.aperture(), 1.0, 7, time), 0xA50F37, true);
            check(finite[0] && quads[0] > 1500 && quads[0] < 12000, "the worm is built of finite quads, a sane number (" + quads[0] + ")");
        }
        int[] none = {0};
        MawMotion.Shape s = MawMotion.idle(0, 7);
        MawWorm.build((a, b, c, d, n, ca, cb, cc, cd) -> none[0]++, new MawWorm.Pose(s.path(), s.dorsal(), 0.0, 0.0, 1.0, 7, 0), 0xA50F37, true);
        check(none[0] == 0, "nothing is drawn of a worm not grown out at all");
    }

    /** The mouth: its hole grows as it opens, the head flares, a shut one is still a mouth (a small hole). */
    private static void mouth() {
        double last = -1.0;
        boolean grows = true;
        for (double open = 0.0; open <= 1.0001; open += 0.05) {
            double hole = MawWorm.holeRadius(open);
            grows &= hole > last;
            last = hole;
        }
        check(grows, "the mouth's hole grows steadily as it opens");
        check(MawWorm.holeRadius(0.0) > 0.15 * MawWorm.lipRadius(0.0), "shut, the lip still puckers round a hole (it reads as a mouth)");
        check(MawWorm.lipRadius(1.0) > MawWorm.lipRadius(0.0) * 1.3, "thrown wide, the head flares");
        boolean gapes = false;
        boolean mostlyShut = true;
        for (int t = 0; t < 600; t++) {
            double m = MawMotion.idleMouth(t, 7);
            gapes |= m > 0.7;
            mostlyShut &= m >= 0.0 && m <= 1.0;
        }
        check(gapes && mostlyShut, "at rest it now and then gapes, showing its teeth");
    }

    /** The flesh takes the owner's kagune colour; the teeth, spurs and throat keep theirs. */
    private static void colours() {
        MawMotion.Shape s = MawMotion.idle(14, 7);
        java.util.Set<Integer> crimson = colours(s, 0xA50F37);
        java.util.Set<Integer> blue = colours(s, 0x2350D8);
        java.util.Set<Integer> shared = new java.util.HashSet<>(crimson);
        shared.retainAll(blue);
        check(!shared.isEmpty() && shared.size() < crimson.size(), "recoloured, its flesh changes and its bone and throat stay as they are");
        boolean bluish = false;
        for (int c : blue) {
            int r = c >> 16 & 255;
            int b = c & 255;
            bluish |= b > r + 30;
        }
        check(bluish, "a blue kagune's worm is drawn blue");
    }

    private static java.util.Set<Integer> colours(MawMotion.Shape s, int tint) {
        java.util.Set<Integer> out = new java.util.HashSet<>();
        MawWorm.build((a, b, c, d, n, ca, cb, cc, cd) -> {
            out.add(ca & 0xFFFFFF);
            out.add(cc & 0xFFFFFF);
        }, new MawWorm.Pose(s.path(), s.dorsal(), 1.0, 0.8, 1.0, 7, 14), tint, false);
        return out;
    }

    /** A bite reaches its target as it lands, its mouth thrown wide just before and snapped shut on it, and comes back to rest. */
    private static void motion() {
        double[] target = {0.1, 1.2, 3.5};
        MawMotion.Shape before = MawMotion.bite(40, 7, target, 4.5, 12, 6);
        MawMotion.Shape landed = MawMotion.bite(41.5, 7, target, 6.0, 12, 6);
        double[] head = landed.path()[landed.path().length - 1];
        double miss = Math.sqrt((head[0] - target[0]) * (head[0] - target[0]) + (head[1] - target[1]) * (head[1] - target[1])
            + (head[2] - target[2]) * (head[2] - target[2]));
        check(miss < 0.2, "a bite's mouth is on its target as it lands (" + miss + " blocks off)");
        check(before.aperture() > 0.85 && landed.aperture() < 0.1, "the mouth gapes on the way in and is snapped shut as it lands");
        MawMotion.Shape end = MawMotion.bite(52, 7, target, 12.0, 12, 6);
        MawMotion.Shape rest = MawMotion.idle(52, 7);
        double[] a = end.path()[end.path().length - 1];
        double[] b = rest.path()[rest.path().length - 1];
        check(Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]) + Math.abs(a[2] - b[2]) < 1.0E-6, "a bite ends back at rest");
    }

    private static double distance(double[] a, double[] b) {
        return Math.sqrt((a[0] - b[0]) * (a[0] - b[0]) + (a[1] - b[1]) * (a[1] - b[1]) + (a[2] - b[2]) * (a[2] - b[2]));
    }

    /** The worm's length along its spine. */
    private static double length(MawMotion.Shape s) {
        return MawWorm.spine(s.path(), s.dorsal(), 60).length();
    }

    /**
     * 3.4.9: each bite of the M1 string (as the renderer poses it, MawMotion.of) lands on its target shut, gapes on the way, never jumps,
     * ends at rest; thrown out it stays long (the owner: "make sure it's long"), the coils it doesn't need still zigzagging behind him; none
     * of its points ever goes into his head or torso; a target out of reach is bitten as far as it reaches.
     */
    private static void string() {
        double[][] targets = {{0.1, 0.99, 3.6}, {-1.2, 0.6, 2.2}, {0.8, 1.8, 5.5}, {0.0, 0.3, 1.6}};
        for (com.hex.kagune.control.bikaku.BikakuRules.Bite bite : new com.hex.kagune.control.bikaku.BikakuRules.Bite[] {
            com.hex.kagune.control.bikaku.BikakuRules.Bite.SNAP, com.hex.kagune.control.bikaku.BikakuRules.Bite.LUNGE,
            com.hex.kagune.control.bikaku.BikakuRules.Bite.MAUL}) {
            check(bite.land() > 2 && bite.land() < bite.ticks() - 3, bite + " shuts partway through, with time to shake and draw back");
            for (double[] target : targets) {
                double[] at = com.hex.kagune.control.bikaku.BikakuRules.reach(target);
                MawMotion.Shape shut = MawMotion.of(40 + bite.land(), 7, bite, bite.land(), target);
                double[] head = shut.path()[shut.path().length - 1];
                check(distance(head, at) < 0.2, bite + " lands on its target (" + distance(head, at) + " off)");
                check(shut.aperture() < 0.1, bite + " is shut as it lands");
                check(length(shut) > 6.5, bite + " thrown out is still long (" + length(shut) + " blocks)");
                // The coils it didn't need stay behind him, swinging from side to side.
                double[] low = shut.path()[2];
                double[] next = shut.path()[3];
                check(low[2] < -0.4 && next[2] < -0.3 && Math.signum(low[0]) != Math.signum(next[0]), bite + "'s first coils stay zigzagging behind him");
                double widest = 0.0;
                double[] last = null;
                for (double age = 0.0; age <= bite.ticks(); age += 0.25) {
                    MawMotion.Shape s = MawMotion.of(40 + age, 7, bite, age, target);
                    widest = Math.max(widest, s.aperture());
                    double[] tip = s.path()[s.path().length - 1];
                    if (last != null) {
                        check(distance(tip, last) < 0.75, bite + " never jumps (" + distance(tip, last) + " in a quarter tick at " + age + ")");
                    }
                    last = tip;
                    for (int i = 2; i < s.path().length; i++) {
                        double[] p = s.path()[i];
                        check(distance(p, new double[] {0.0, 1.72, 0.0}) > 0.6 - 1.0E-9, bite + ": nothing of it through his head (" + i + " at " + age + ")");
                        check(!(p[1] > 0.6 && p[1] < 1.55 && Math.abs(p[0]) < 0.72 && p[2] > -0.4 + 1.0E-9 && p[2] < 0.4 - 1.0E-9),
                            bite + ": nothing of it through his torso (" + i + " at " + age + ")");
                    }
                }
                check(widest > 0.85, bite + " gapes on its way in");
                MawMotion.Shape end = MawMotion.of(40 + bite.ticks(), 7, bite, bite.ticks(), target);
                MawMotion.Shape rest = MawMotion.idle(40 + bite.ticks(), 7);
                check(distance(end.path()[end.path().length - 1], rest.path()[rest.path().length - 1]) < 1.0E-9, bite + " ends at rest");
            }
        }
        double[] far = com.hex.kagune.control.bikaku.BikakuRules.reach(new double[] {0.0, 1.0, 30.0});
        check(Math.abs(distance(far, MawMotion.shoulder()) - com.hex.kagune.control.bikaku.BikakuRules.REACH) < 1.0E-9,
            "something out of reach is bitten as far as the worm reaches");
        check(MawMotion.of(10, 7, com.hex.kagune.control.bikaku.BikakuRules.Bite.SNAP, 99, new double[] {0, 1, 3}).aperture()
            == MawMotion.idle(10, 7).aperture(), "a bite that is over is the worm at rest");
    }

    /** Coming out of his back: cut along its own line, longer the further out, exactly the whole worm when out. */
    private static void growing() {
        MawMotion.Shape s = MawMotion.idle(14, 7);
        check(MawMotion.grown(s.path(), 1.0) == s.path(), "fully out it is the whole worm");
        double last = 0.0;
        boolean longer = true;
        for (double g = 0.05; g < 1.0; g += 0.05) {
            double[][] cut = MawMotion.grown(s.path(), g);
            longer &= cut.length >= 2 && distance(cut[0], MawMotion.ROOT) < 1.0E-9;
            double len = 0.0;
            for (int i = 1; i < cut.length; i++) {
                len += distance(cut[i], cut[i - 1]);
            }
            longer &= len > last;
            last = len;
        }
        check(longer, "it grows out of his lower back, longer the further out");
    }

    /** His own first person: nothing within reach of his eyes, the near part aside to the right, the bite still on its target. */
    private static void firstPerson() {
        double[] eye = {0.0, 1.62, 0.0};
        double[] target = {0.1, 0.99, 3.6};
        for (double age = 0.0; age <= 12.0; age += 0.5) {
            MawMotion.Shape s = MawMotion.of(30 + age, 7, com.hex.kagune.control.bikaku.BikakuRules.Bite.SNAP, age, target);
            double[][] seen = MawMotion.firstPerson(s.path(), eye);
            for (double[] p : seen) {
                check(distance(p, eye) >= MawMotion.FIRST_PERSON_CLEAR - 1.0E-9, "in first person nothing of the worm comes near the eyes");
            }
            if (age == 6.0) {
                double[] head = seen[seen.length - 1];
                check(distance(head, target) < 0.35, "in first person the bite still lands on what it bites (" + distance(head, target) + ")");
                double[] mid = seen[seen.length - 3];
                check(mid[0] > 0.25, "in first person the worm comes in from his right, not over the middle of his view");
            }
        }
    }

    /**
     * 3.5.0: Devouring Maw and Predatory Drag, posed as the renderer poses them with the catch where the server holds it: the mouth gapes
     * on the way, shuts on its target, stays on its catch, never jumps, never passes through him, ends at rest; missed, it draws back.
     */
    private static void grabs() {
        double[][] targets = {{0.1, 0.99, 3.6}, {-1.5, 0.9, 2.5}, {0.6, 1.2, 6.0}, {0.0, 0.9, 9.5}};
        for (com.hex.kagune.control.bikaku.BikakuMove move : new com.hex.kagune.control.bikaku.BikakuMove[] {
            com.hex.kagune.control.bikaku.BikakuMove.MAW, com.hex.kagune.control.bikaku.BikakuMove.DRAG}) {
            boolean maw = move == com.hex.kagune.control.bikaku.BikakuMove.MAW;
            for (double[] target : targets) {
                double[] aim = maw ? com.hex.kagune.control.bikaku.BikakuRules.reach(target) : MawMotion.dragReach(target);
                for (boolean caught : new boolean[] {true, false}) {
                    double[] last = null;
                    double widest = 0.0;
                    int end = caught ? move.ticks() : move.missedTicks();
                    for (double age = 0.0; age < end; age += 0.25) {
                        double[] held = age < move.land() || !caught ? null
                            : maw ? MawMotion.mawHeld(aim, age) : MawMotion.dragHeld(aim, Math.min(age, com.hex.kagune.control.bikaku.BikakuMove.DRAG_SLAM));
                        MawMotion.Shape s = MawMotion.move(40 + age, 7, move, age, target, held, caught);
                        double[] tip = s.path()[s.path().length - 1];
                        widest = Math.max(widest, s.aperture());
                        if (last != null) {
                            check(distance(tip, last) < 0.9, move + " never jumps (" + distance(tip, last) + " at " + age + ", caught " + caught + ")");
                        }
                        last = tip;
                        boolean gripping = caught && age >= move.land() && age <= (maw ? com.hex.kagune.control.bikaku.BikakuMove.MAW_RELEASE
                            : com.hex.kagune.control.bikaku.BikakuMove.DRAG_SLAM);
                        if (gripping && held != null) {
                            check(distance(tip, held) < 0.75, move + " keeps its mouth on its catch (" + distance(tip, held) + " at " + age + ")");
                            check(s.aperture() < 0.1, move + " keeps its jaws shut on its catch");
                        }
                        for (int i = 2; i < s.path().length - 2; i++) {
                            double[] p = s.path()[i];
                            check(distance(p, new double[] {0.0, 1.72, 0.0}) > 0.6 - 1.0E-9, move + ": nothing through his head (" + i + " at " + age + ")");
                            check(!(p[1] > 0.6 && p[1] < 1.55 && Math.abs(p[0]) < 0.72 && p[2] > -0.4 + 1.0E-9 && p[2] < 0.4 - 1.0E-9),
                                move + ": nothing through his torso (" + i + " at " + age + ")");
                            check(!(p[1] >= -0.5 && p[1] <= 0.6 && Math.abs(p[0]) < 0.5 - 1.0E-9 && Math.abs(p[2]) < 0.37), move + ": nothing through his legs ("
                                + i + " at " + age + ": " + p[0] + ", " + p[1] + ", " + p[2] + ")");
                        }
                    }
                    check(widest > 0.95, move + " opens its mouth wide on the way");
                    MawMotion.Shape shut = MawMotion.move(40 + move.land(), 7, move, move.land(), target, null, caught);
                    check(shut.aperture() < 0.1, move + " is shut as it bites");
                    double[] bitHead = shut.path()[shut.path().length - 1];
                    check(distance(bitHead, aim) < (maw ? 0.6 : 0.8), move + "'s mouth is on its target as it bites (" + distance(bitHead, aim) + ")");
                    MawMotion.Shape after = MawMotion.move(40 + end, 7, move, end, target, null, caught);
                    MawMotion.Shape rest = MawMotion.idle(40 + end, 7);
                    check(distance(after.path()[after.path().length - 1], rest.path()[rest.path().length - 1]) < 1.0E-9, move + " ends at rest");
                    MawMotion.Shape near = MawMotion.move(40 + end - 0.25, 7, move, end - 0.25, target, null, caught);
                    check(distance(near.path()[near.path().length - 1], rest.path()[rest.path().length - 1]) < 0.3, move + " comes back to rest smoothly");
                }
            }
        }
        // Where the catch is held does not depend on where it was bitten once pulled in (the clients and the server agree).
        double[] a = MawMotion.mawHeld(new double[] {0, 1, 3}, com.hex.kagune.control.bikaku.BikakuMove.MAW_RELEASE);
        double[] b = MawMotion.mawHeld(new double[] {-2, 0.5, 6}, com.hex.kagune.control.bikaku.BikakuMove.MAW_RELEASE);
        check(distance(a, b) < 1.0E-9 && a[0] > MawMotion.MAW_HOLD[0] + 1.0, "Devouring Maw flings its catch out to his right");
        double[] slammed = MawMotion.dragHeld(new double[] {0, 1, 8}, com.hex.kagune.control.bikaku.BikakuMove.DRAG_SLAM);
        double[] lifted = MawMotion.dragHeld(new double[] {0, 1, 8}, com.hex.kagune.control.bikaku.BikakuMove.DRAG_LIFTED);
        check(distance(slammed, MawMotion.DRAG_SLAMMED) < 1.0E-9 && lifted[1] > 3.0 && slammed[1] < 1.0, "Predatory Drag lifts its catch high, then slams it down");
        double[] dragged = MawMotion.dragHeld(new double[] {0, 1, 8}, com.hex.kagune.control.bikaku.BikakuMove.DRAG_HOME);
        check(Math.abs(dragged[1] - 1.0) < 1.0E-9 && dragged[2] < 2.5, "Predatory Drag drags its catch along its own ground to him");
        check(Math.hypot(MawMotion.dragReach(new double[] {0, 1, 30})[0], MawMotion.dragReach(new double[] {0, 1, 30})[2])
            <= com.hex.kagune.control.bikaku.BikakuRules.DRAG_REACH + 1.0E-9, "Predatory Drag reaches as far as it reaches");
    }

    private static boolean inside(double[] p) {
        return distance(p, new double[] {0.0, 1.72, 0.0}) < 0.6 - 1.0E-9
            || p[1] > 0.6 && p[1] < 1.55 && Math.abs(p[0]) < 0.72 && p[2] > -0.4 + 1.0E-9 && p[2] < 0.4 - 1.0E-9
            || p[1] >= -0.5 && p[1] <= 0.6 && Math.abs(p[0]) < 0.5 - 1.0E-9 && Math.abs(p[2]) < 0.37;
    }

    /**
     * 3.5.1: Ravenous Pursuit: three worms, each on its own body as it bites (in turn), shut; the worm thins as two more grow out and is
     * whole again at the end; none jumps or passes through him.
     */
    private static void pursuit() {
        double[][][] cases = {{{2.6, 0.99, 3.2}, {-2.3, 0.99, 3.0}, {0.2, 0.99, 4.3}}, {{0.1, 0.99, 3.6}, {0.1, 0.99, 3.6}, {0.1, 0.99, 3.6}},
            {{4.0, 0.5, 4.0}, {-5.0, 2.0, 2.0}, {0.0, 0.3, 1.8}}};
        for (double[][] targets : cases) {
            for (int i = 0; i < com.hex.kagune.control.bikaku.BikakuMove.PURSUIT_WORMS; i++) {
                int land = com.hex.kagune.control.bikaku.BikakuMove.pursuitLands(i);
                MawMotion.Worm[] w = MawMotion.pursuit(30 + land, 7, targets, land);
                double[] head = w[i].shape().path()[w[i].shape().path().length - 1];
                double[] at = com.hex.kagune.control.bikaku.BikakuRules.reach(MawMotion.pursuitReach(targets[i]));
                check(distance(head, at) < 0.3, "Ravenous Pursuit's worm " + i + " is on its own body as it bites (" + distance(head, at) + ")");
                check(w[i].shape().aperture() < 0.1, "Ravenous Pursuit's worm " + i + " is shut as it bites");
            }
            double[][] last = new double[3][];
            for (double age = 0.0; age < com.hex.kagune.control.bikaku.BikakuMove.PURSUIT.ticks(); age += 0.25) {
                MawMotion.Worm[] w = MawMotion.pursuit(30 + age, 7, targets, age);
                check(w.length == 3, "it is three worms");
                for (int i = 0; i < 3; i++) {
                    double[][] path = w[i].shape().path();
                    double[] tip = path[path.length - 1];
                    if (last[i] != null) {
                        check(distance(tip, last[i]) < 0.9, "Ravenous Pursuit's worm " + i + " never jumps (" + distance(tip, last[i]) + " at " + age + ")");
                    }
                    last[i] = tip;
                    for (int k = 2; k < path.length - 2; k++) {
                        check(!inside(path[k]), "Ravenous Pursuit's worm " + i + ": nothing through him (" + k + " at " + age + ")");
                    }
                }
            }
            // The owner: "they're too close up to each other ... forming a kind of mass in the back ... separate them more ... just so it
            // doesn't create that mass in the back ever": past their shared root, no two of them ever come within 0.85 of each other behind
            // him (each as far as it has grown out), at any moment.
            for (double age = 0.0; age <= com.hex.kagune.control.bikaku.BikakuMove.PURSUIT.ticks(); age += 0.5) {
                MawMotion.Worm[] w = MawMotion.pursuit(30 + age, 7, targets, age);
                java.util.List<java.util.List<double[]>> drawn = new java.util.ArrayList<>();
                for (MawMotion.Worm worm : w) {
                    java.util.List<double[]> points = new java.util.ArrayList<>();
                    if (worm.grown() > 0.05) {
                        KakujaModel.Spine spine = MawWorm.spine(MawMotion.grown(worm.shape().path(), worm.grown()), worm.shape().dorsal(), 80);
                        for (int k = 0; k <= 80; k++) {
                            double u = k / 80.0;
                            double[] at = spine.at(u)[0];
                            if (u * spine.length() >= 1.2 && at[2] < -0.3) {
                                points.add(at);
                            }
                        }
                    }
                    drawn.add(points);
                }
                double closest = Double.MAX_VALUE;
                for (int i = 0; i < drawn.size(); i++) {
                    for (int j = i + 1; j < drawn.size(); j++) {
                        for (double[] a : drawn.get(i)) {
                            for (double[] b : drawn.get(j)) {
                                closest = Math.min(closest, distance(a, b));
                            }
                        }
                    }
                }
                check(closest >= 0.85, "Ravenous Pursuit's worms never bunch up behind him (" + closest + " apart at " + age + ")");
            }
            MawMotion.Worm[] mid = MawMotion.pursuit(50, 7, targets, 15);
            check(mid[0].girth() < 0.7 && mid[1].girth() < 0.7 && mid[1].grown() > 0.99 && mid[2].grown() > 0.99, "split, it is three thin worms");
            MawMotion.Worm[] start = MawMotion.pursuit(30, 7, targets, 0);
            MawMotion.Worm[] end = MawMotion.pursuit(74, 7, targets, com.hex.kagune.control.bikaku.BikakuMove.PURSUIT.ticks());
            check(start[0].girth() == 1.0 && start[1].grown() == 0.0 && end[0].girth() > 0.999 && end[1].grown() < 0.001 && end[2].grown() < 0.001,
                "it starts and ends as the one worm");
        }
    }

    /**
     * 3.5.1: Carrion Eruption: into the ground before him, under its mark (the ground heaving over it), up over its mark shut as it bites,
     * back the same way to rest; it never jumps; what shows above the ground never passes through him.
     */
    private static void eruption() {
        double[][] marks = {{0.4, 0.99, 5.5}, {-3.0, 0.99, 8.0}, {0.0, 0.99, 30.0}, {2.0, 1.99, 2.5}};
        int land = com.hex.kagune.control.bikaku.BikakuMove.ERUPTION.land();
        for (double[] mark : marks) {
            double[] spot = MawMotion.eruptSpot(mark);
            check(Math.hypot(spot[0], spot[2]) <= com.hex.kagune.control.bikaku.BikakuRules.ERUPTION_REACH + 1.0E-9, "it tunnels as far as it reaches");
            MawMotion.Shape shut = MawMotion.eruption(30 + land, 7, mark, land);
            double[] head = shut.path()[shut.path().length - 1];
            check(Math.hypot(head[0] - spot[0], head[2] - spot[2]) < 0.1 && head[1] > spot[1] + 2.0 && shut.aperture() < 0.1,
                "it bursts up over its mark and is shut as it bites");
            MawMotion.Shape rising = MawMotion.eruption(30 + land - 1.5, 7, mark, land - 1.5);
            check(rising.aperture() > 0.9, "its jaws are thrown wide as it bursts up");
            for (double age = com.hex.kagune.control.bikaku.BikakuMove.ERUPT_DIVE + 1; age < com.hex.kagune.control.bikaku.BikakuMove.ERUPT_RISE; age += 1.0) {
                MawMotion.Shape s = MawMotion.eruption(30 + age, 7, mark, age);
                double[] tip = s.path()[s.path().length - 1];
                check(tip[1] < Math.min(0.0, spot[1]) - 0.5, "tunnelling, its head is under the ground (" + tip[1] + " at " + age + ")");
                check(MawMotion.eruptBulge(mark, age) != null, "the ground heaves over it as it tunnels");
            }
            check(MawMotion.eruptBulge(mark, 2) == null && MawMotion.eruptBulge(mark, land) == null, "the ground heaves only while it tunnels");
            double[] last = null;
            for (double age = 0.0; age <= com.hex.kagune.control.bikaku.BikakuMove.ERUPTION.ticks(); age += 0.25) {
                MawMotion.Shape s = MawMotion.eruption(30 + age, 7, mark, age);
                double[] tip = s.path()[s.path().length - 1];
                if (last != null) {
                    check(distance(tip, last) < 1.2, "Carrion Eruption never jumps (" + distance(tip, last) + " at " + age + ")");
                }
                last = tip;
                for (int k = 2; k < s.path().length - 2; k++) {
                    check(!inside(s.path()[k]), "Carrion Eruption: nothing through him (" + k + " at " + age + ")");
                }
            }
            MawMotion.Shape end = MawMotion.eruption(84, 7, mark, com.hex.kagune.control.bikaku.BikakuMove.ERUPTION.ticks());
            MawMotion.Shape rest = MawMotion.idle(84, 7);
            check(distance(end.path()[end.path().length - 1], rest.path()[rest.path().length - 1]) < 1.0E-6, "Carrion Eruption ends at rest");
        }
    }

    /**
     * 3.5.4: the kakuja: 4 to 5 blocks tall round him, lopsided, its tentacles reaching far, most with mouths; growing into it and out of
     * it never jumps, ends whole either way; a tentacle's bite lands on its target; finite, sane meshes.
     */
    private static void kakuja() {
        com.hex.kagune.control.bikaku.MawKakuja.Growth full = com.hex.kagune.control.bikaku.MawKakuja.growth(true, 500);
        double[] lo = {1e9, 1e9, 1e9};
        double[] hi = {-1e9, -1e9, -1e9};
        int[] quads = {0};
        boolean[] finite = {true};
        UkakuModel.Sink sink = (a, b, c, d, n, ca, cb, cc, cd) -> {
            quads[0]++;
            for (double[] q : new double[][] {a, b, c, d}) {
                finite[0] &= Double.isFinite(q[0]) && Double.isFinite(q[1]) && Double.isFinite(q[2]);
            }
        };
        UkakuModel.Sink massOnly = (a, b, c, d, n, ca, cb, cc, cd) -> {
            for (double[] q : new double[][] {a, b, c, d}) {
                for (int k = 0; k < 3; k++) {
                    lo[k] = Math.min(lo[k], q[k]);
                    hi[k] = Math.max(hi[k], q[k]);
                }
            }
        };
        com.hex.kagune.control.bikaku.MawKakuja.Growth bodyOnly = new com.hex.kagune.control.bikaku.MawKakuja.Growth(0, 0, 0, full.lumps(), 0, 0);
        com.hex.kagune.control.bikaku.MawKakuja.build(massOnly, bodyOnly, 40, 7, 0xA50F37, false, -1, null, 0, 0);
        check(hi[1] >= 4.0 && hi[1] <= 5.4 && lo[1] < 0.2, "its body is 4 to 5 blocks tall, on the ground (" + lo[1] + " to " + hi[1] + ")");
        check(hi[0] - lo[0] >= 3.5 && hi[2] - lo[2] >= 3.0, "its body is wide round him");
        check(Math.abs(hi[0] + lo[0]) > 0.3 || Math.abs(hi[2] + lo[2]) > 0.3, "lopsided, not symmetrical");
        com.hex.kagune.control.bikaku.MawKakuja.build(sink, full, 40, 7, 0xA50F37, true, -1, null, 0, 0);
        check(finite[0] && quads[0] > 4000 && quads[0] < 80000, "the kakuja is finite quads, a sane number (" + quads[0] + ")");
        int mouthed = 0;
        double farthest = 0.0;
        double[] centre = {0.0, 2.4, 0.0};
        for (int i = 0; i < com.hex.kagune.control.bikaku.MawKakuja.tentacles(); i++) {
            mouthed += com.hex.kagune.control.bikaku.MawKakuja.mouthed(i) ? 1 : 0;
            double[][] line = com.hex.kagune.control.bikaku.MawKakuja.tentacle(i, 40, null, 0);
            farthest = Math.max(farthest, distance(line[line.length - 1], centre));
        }
        check(mouthed >= 4 && mouthed < com.hex.kagune.control.bikaku.MawKakuja.tentacles(), "most tentacles end in mouths, not all");
        check(farthest >= 7.0, "its tentacles reach far beyond its body (" + farthest + ")");
        for (boolean in : new boolean[] {true, false}) {
            com.hex.kagune.control.bikaku.MawKakuja.Growth last = null;
            int end = in ? com.hex.kagune.control.bikaku.MawKakuja.IN : com.hex.kagune.control.bikaku.MawKakuja.OUT;
            for (double age = 0; age <= end + 2; age += 0.5) {
                com.hex.kagune.control.bikaku.MawKakuja.Growth g = com.hex.kagune.control.bikaku.MawKakuja.growth(in, age);
                if (last != null) {
                    double jump = Math.max(Math.abs(g.worm() - last.worm()), Math.max(Math.abs(g.mouths() - last.mouths()), Math.abs(g.tentacles() - last.tentacles())));
                    for (int i = 0; i < g.lumps().length; i++) {
                        jump = Math.max(jump, Math.abs(g.lumps()[i] - last.lumps()[i]));
                    }
                    check(jump < 0.2, "growing " + (in ? "into" : "out of") + " it never jumps (" + jump + " at " + age + ")");
                }
                last = g;
            }
            check(in ? last.worm() < 1e-9 && last.mouths() > 0.999 && Math.abs(last.tentacles() - 1.0) < 1e-6 && !(last.wrap() > 1e-9)
                : last.worm() > 0.999 && !last.shows(), "it ends " + (in ? "grown, the worm swallowed" : "gone, the worm back"));
        }
        com.hex.kagune.control.bikaku.MawKakuja.Growth start = com.hex.kagune.control.bikaku.MawKakuja.growth(true, 0);
        check(start.worm() > 0.999 && !start.shows(), "it starts as the worm");
        com.hex.kagune.control.bikaku.MawKakuja.Growth wrapping = com.hex.kagune.control.bikaku.MawKakuja.growth(true, 22);
        check(wrapping.wrap() > 0.5 && wrapping.swell() > 0.9, "growing into it, the worm swells and tendrils wrap round him");
        double[][] targets = {{8.0, 1.0, 6.0}, {-9.0, 3.0, 2.0}, {0.0, 1.0, 12.0}, {2.0, 6.0, -8.0}};
        int land = com.hex.kagune.control.bikaku.BikakuRules.Bite.TENTACLE.land();
        for (double[] target : targets) {
            int biter = com.hex.kagune.control.bikaku.MawKakuja.biter(target, 40);
            check(com.hex.kagune.control.bikaku.MawKakuja.mouthed(biter), "a tentacle with a mouth bites");
            double[][] line = com.hex.kagune.control.bikaku.MawKakuja.tentacle(biter, 40, target, com.hex.kagune.control.bikaku.MawKakuja.lunge(land));
            check(distance(line[line.length - 1], target) < 0.01, "its mouth is on its target as it bites");
            check(com.hex.kagune.control.bikaku.MawKakuja.lungeMouth(land) < 0.1 && com.hex.kagune.control.bikaku.MawKakuja.lungeMouth(land - 2) > 0.9,
                "thrown wide, snapped shut as it lands");
            check(com.hex.kagune.control.bikaku.MawKakuja.lunge(com.hex.kagune.control.bikaku.BikakuRules.Bite.TENTACLE.ticks()) < 1e-9
                && com.hex.kagune.control.bikaku.MawKakuja.lunge(0) == 0.0, "it starts and ends where it was");
        }
        int[] taper = {0};
        MawMotion.Shape s = MawMotion.idle(14, 7);
        MawWorm.build((a, b, c, d, n, ca, cb, cc, cd) -> taper[0]++, new MawWorm.Pose(s.path(), s.dorsal(), 1.0, 0.0, 1.5, 7, 14), 0xA50F37, false, false);
        int[] mouth = {0};
        MawWorm.build((a, b, c, d, n, ca, cb, cc, cd) -> mouth[0]++, new MawWorm.Pose(s.path(), s.dorsal(), 1.0, 0.0, 1.5, 7, 14), 0xA50F37, false, true);
        check(taper[0] > 100 && taper[0] < mouth[0], "a mouthless tentacle is drawn without its mouth");
    }
}
