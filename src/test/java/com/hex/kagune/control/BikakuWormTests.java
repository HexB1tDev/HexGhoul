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
        roundHim();
        kakujaMoves();
        heldFar();
        routes();
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

    // ---------------------------------------------------------------- 3.5.5: drawn round him, never through him

    /** Minecraft's player in the body's frame: head, torso, right arm, left arm, legs ({minX, minY, minZ, maxX, maxY, maxZ}). */
    private static final double[][] BODY = {
        {-0.25, 1.5, -0.25, 0.25, 2.0, 0.25}, {-0.25, 0.75, -0.125, 0.25, 1.5, 0.125}, {0.25, 0.75, -0.125, 0.5, 1.5, 0.125},
        {-0.5, 0.75, -0.125, -0.25, 1.5, 0.125}, {-0.25, 0.0, -0.125, 0.25, 0.75, 0.125}};

    /**
     * How deep the worm drawn along {@code path} ({@code girth}, mouth {@code open}) goes into his body at its deepest: its line as the mesh
     * draws it, its thickness there, past the first 0.45 blocks out of his back.
     */
    private static double depthInHim(double[][] path, double girth, double open) {
        KakujaModel.Spine spine = MawWorm.spine(path, new double[] {0, 1, 0}, 160);
        double length = spine.length();
        double deepest = 0.0;
        for (int i = 0; i < spine.p().length; i++) {
            double u = i / (double)(spine.p().length - 1);
            if (u * length < 0.45) {
                continue;
            }
            double r = MawWorm.radius(u, length, open) * girth;
            for (double[] b : BODY) {
                double[] q = spine.p()[i];
                double dx = Math.max(Math.max(b[0] - q[0], 0.0), q[0] - b[3]);
                double dy = Math.max(Math.max(b[1] - q[1], 0.0), q[1] - b[4]);
                double dz = Math.max(Math.max(b[2] - q[2], 0.0), q[2] - b[5]);
                deepest = Math.max(deepest, r - Math.sqrt(dx * dx + dy * dy + dz * dz));
            }
        }
        return deepest;
    }

    /** How far the worm drawn along {@code a} and {@code b} moves at its most (each cut into 80 even lengths). */
    private static double moved(double[][] a, double[][] b) {
        KakujaModel.Spine sa = MawWorm.spine(a, new double[] {0, 1, 0}, 80);
        KakujaModel.Spine sb = MawWorm.spine(b, new double[] {0, 1, 0}, 80);
        double most = 0.0;
        for (int i = 0; i < sa.p().length; i++) {
            most = Math.max(most, distance(sa.p()[i], sb.p()[i]));
        }
        return most;
    }

    /**
     * 3.5.5 (the owner: "make sure that the tentacles don't go through a player's body ... the predatory drag looks a little strange when
     * you send it and it goes through the player's body instead of going around it"): every move, drawn as the game draws it
     * (MawMotion.around), keeps the worm out of his head, torso, arms and legs (2 cm of leeway: Predatory Drag had gone through him in about
     * half its frames, by the worm's whole thickness); it is drawn round him without jumping (a quarter tick apart, no more than 0.3 further
     * than its points move, for bodies before him); its root stays where it is, its mouth on what it bites (only nudged off his face in
     * flight); Predatory Drag drags its catch in to its own side.
     */
    private static void roundHim() {
        double leeway = 0.03;
        // Before him (as M1 and the moves aim: where he looks), near and far, high and low; and beside and behind him for the depth.
        double[][] before = {{0.2, 0.99, 4.0}, {0.0, 0.99, 6.5}, {3.0, 0.99, 3.0}, {-3.0, 0.99, 3.0}, {0.0, 3.5, 3.0}, {1.0, 0.3, 1.5},
            {-1.0, 0.3, 1.5}, {0.0, 0.99, 1.4}, {0.0, 0.99, 10.0}, {-6.0, 0.99, 6.0}, {0.5, -2.0, 4.0}, {2.0, 0.99, 1.2}};
        double[][] around = {{4.0, 0.99, 0.0}, {-4.0, 0.99, 0.0}, {-3.0, 0.99, -3.0}, {3.0, 0.99, -3.0}, {0.0, 0.99, -4.0}, {-1.5, 0.99, 0.5}};
        for (double[][] set : new double[][][] {before, around}) {
            boolean ahead = set == before;
            for (double[] target : set) {
                for (com.hex.kagune.control.bikaku.BikakuRules.Bite bite : new com.hex.kagune.control.bikaku.BikakuRules.Bite[] {
                    com.hex.kagune.control.bikaku.BikakuRules.Bite.SNAP, com.hex.kagune.control.bikaku.BikakuRules.Bite.LUNGE,
                    com.hex.kagune.control.bikaku.BikakuRules.Bite.MAUL}) {
                    MawMotion.Shape last = null;
                    for (double age = 0.0; age < bite.ticks(); age += 0.25) {
                        MawMotion.Shape s = MawMotion.of(40 + age, 7, bite, age, target);
                        double[][] drawn = MawMotion.around(s.path(), 1.0, s.aperture());
                        check(depthInHim(drawn, 1.0, s.aperture()) < leeway, bite + " is drawn round him, not through him (at " + age + ")");
                        double mouthMoved = distance(drawn[drawn.length - 1], s.path()[s.path().length - 1]);
                        check(distance(drawn[0], s.path()[0]) < 1.0E-12, bite + ": its root stays where it is");
                        check(age == bite.land() ? mouthMoved < 1.0E-12 : mouthMoved < 0.25, bite + ": its mouth stays on what it bites, and is only "
                            + "nudged off his face in flight (" + mouthMoved + " at " + age + ")");
                        if (ahead && last != null) {
                            double raw = moved(last.path(), s.path());
                            double drawnMove = moved(MawMotion.around(last.path(), 1.0, last.aperture()), drawn);
                            check(drawnMove < raw + 0.3, bite + " drawn round him doesn't jump (" + drawnMove + " against " + raw + " at " + age + ")");
                        }
                        last = s;
                    }
                }
                for (com.hex.kagune.control.bikaku.BikakuMove move : new com.hex.kagune.control.bikaku.BikakuMove[] {
                    com.hex.kagune.control.bikaku.BikakuMove.MAW, com.hex.kagune.control.bikaku.BikakuMove.DRAG}) {
                    boolean maw = move == com.hex.kagune.control.bikaku.BikakuMove.MAW;
                    double[] t = maw ? target : new double[] {target[0], Math.min(target[1], 0.99), target[2]};
                    double[] aim = maw ? com.hex.kagune.control.bikaku.BikakuRules.reach(t) : MawMotion.dragReach(t);
                    for (boolean caught : new boolean[] {true, false}) {
                        int end = caught ? move.ticks() : move.missedTicks();
                        MawMotion.Shape last = null;
                        for (double age = 0.0; age < end; age += 0.25) {
                            double[] held = age < move.land() || !caught ? null
                                : maw ? MawMotion.mawHeld(aim, age) : MawMotion.dragHeld(aim, Math.min(age, com.hex.kagune.control.bikaku.BikakuMove.DRAG_SLAM));
                            MawMotion.Shape s = MawMotion.move(40 + age, 7, move, age, t, held, caught);
                            double[][] drawn = MawMotion.around(s.path(), 1.0, s.aperture());
                            check(depthInHim(drawn, 1.0, s.aperture()) < leeway, move + " is drawn round him, not through him (at " + age + ", caught "
                                + caught + ", " + t[0] + ", " + t[2] + ")");
                            if (ahead && last != null) {
                                double raw = moved(last.path(), s.path());
                                double drawnMove = moved(MawMotion.around(last.path(), 1.0, last.aperture()), drawn);
                                check(drawnMove < raw + 0.3, move + " drawn round him doesn't jump (" + drawnMove + " against " + raw + " at " + age
                                    + ", caught " + caught + ", " + t[0] + ", " + t[2] + ")");
                            }
                            last = s;
                        }
                    }
                }
            }
        }
        // Ravenous Pursuit's three and Carrion Eruption (what shows above the ground) are drawn round him too.
        double[][][] sets = {{{2.6, 0.99, 3.2}, {-2.3, 0.99, 3.0}, {0.2, 0.99, 4.3}}, {{0.2, 0.99, 4.0}, {0.2, 0.99, 4.0}, {0.2, 0.99, 4.0}}};
        for (double[][] targets : sets) {
            for (double age = 0.0; age < com.hex.kagune.control.bikaku.BikakuMove.PURSUIT.ticks(); age += 0.5) {
                for (MawMotion.Worm worm : MawMotion.pursuit(40 + age, 7, targets, age)) {
                    double[][] drawn = MawMotion.grown(MawMotion.around(worm.shape().path(), worm.girth(), worm.shape().aperture()), worm.grown());
                    if (worm.grown() > 0.002 && drawn.length >= 2) {
                        check(depthInHim(drawn, worm.girth(), worm.shape().aperture()) < leeway, "Ravenous Pursuit's worms are drawn round him (at " + age + ")");
                    }
                }
            }
        }
        for (double age = 0.0; age < com.hex.kagune.control.bikaku.BikakuMove.ERUPTION.ticks(); age += 0.5) {
            MawMotion.Shape s = MawMotion.move(40 + age, 7, com.hex.kagune.control.bikaku.BikakuMove.ERUPTION, age, new double[] {0.2, 0.99, 4.0}, null, true);
            java.util.List<double[]> shown = new java.util.ArrayList<>();
            for (double[] q : MawMotion.around(s.path(), 1.0, s.aperture())) {
                if (q[1] < -0.2) {
                    break;
                }
                shown.add(q);
            }
            if (shown.size() >= 2) {
                check(depthInHim(shown.toArray(new double[0][]), 1.0, s.aperture()) < leeway, "Carrion Eruption is drawn round him (at " + age + ")");
            }
        }
        // At rest it never comes near him, so the pass leaves its line as it was.
        for (double time : new double[] {0, 50, 333}) {
            MawMotion.Shape rest = MawMotion.idle(time, 7);
            check(moved(rest.path(), MawMotion.around(rest.path(), 1.0, rest.aperture())) < 0.02, "at rest it is drawn where it always was");
        }
        // Predatory Drag drags its catch in to the side it came from: straight ahead as before; from his left, to his left, never through him.
        int land = com.hex.kagune.control.bikaku.BikakuMove.DRAG.land();
        for (double[] at : new double[][] {{0, 1, 8}, {-6, 1, 2}, {6, 1, 2}, {-4, 1, -4}}) {
            for (int age = land; age <= com.hex.kagune.control.bikaku.BikakuMove.DRAG_SLAM; age++) {
                double[] held = MawMotion.dragHeld(at, age);
                check(Math.hypot(held[0], held[2]) > 1.2, "Predatory Drag never drags its catch through him (" + at[0] + ", " + at[2] + " at " + age + ")");
                if (age > land + 4) {
                    check(held[0] * at[0] + held[2] * at[2] > 0.0, "Predatory Drag drags its catch in to its own side");
                }
            }
        }
        double[] home = MawMotion.dragHeld(new double[] {0, 1, 8}, com.hex.kagune.control.bikaku.BikakuMove.DRAG_HOME);
        check(Math.abs(home[0] - 0.55) < 1.0E-9 && Math.abs(home[2] - 1.9) < 1.0E-9, "a catch straight ahead is dragged where it always was");
        // 3.5.6 (the owner, of 3.5.5's Drag: "why not make a bigger angle? it's going too close to the body"): past its first 1.6 blocks
        // out of his back, Predatory Drag's worm keeps well clear of him: never under 0.2, and 0.45 or more in at least half its frames
        // (3.5.5: under 0.1 in half its frames, touching in some).
        for (double[] target : new double[][] {{0.2, 0.99, 6.0}, {0.0, 0.99, 10.0}, {-3.0, 0.99, 3.0}, {3.0, 0.99, 3.0}, {-6.0, 0.99, 6.0},
            {0.0, 0.99, 3.0}}) {
            java.util.List<Double> gaps = new java.util.ArrayList<>();
            com.hex.kagune.control.bikaku.BikakuMove drag = com.hex.kagune.control.bikaku.BikakuMove.DRAG;
            for (double age = 0.0; age < drag.ticks(); age += 0.5) {
                double[] held = age < drag.land() ? null : MawMotion.dragHeld(MawMotion.dragReach(target), Math.min(age, com.hex.kagune.control.bikaku.BikakuMove.DRAG_SLAM));
                MawMotion.Shape s = MawMotion.move(40 + age, 7, drag, age, target, held, true);
                gaps.add(clearOf(MawMotion.around(s.path(), 1.0, s.aperture()), s.aperture(), 1.6));
            }
            java.util.Collections.sort(gaps);
            check(gaps.get(0) > 0.2, "Predatory Drag swings out wide of him (closest " + gaps.get(0) + ", " + target[0] + ", " + target[2] + ")");
            check(gaps.get(gaps.size() / 2) > 0.45, "Predatory Drag keeps well clear of him most of the time (" + gaps.get(gaps.size() / 2) + ")");
        }
    }

    /** How far the worm drawn along {@code path} keeps its skin from his body at its closest, past the first {@code from} blocks out of his back. */
    private static double clearOf(double[][] path, double open, double from) {
        KakujaModel.Spine spine = MawWorm.spine(path, new double[] {0, 1, 0}, 160);
        double length = spine.length();
        double least = Double.MAX_VALUE;
        for (int i = 0; i < spine.p().length; i++) {
            double u = i / (double)(spine.p().length - 1);
            if (u * length < from) {
                continue;
            }
            double r = MawWorm.radius(u, length, open);
            for (double[] b : BODY) {
                double[] q = spine.p()[i];
                double dx = Math.max(Math.max(b[0] - q[0], 0.0), q[0] - b[3]);
                double dy = Math.max(Math.max(b[1] - q[1], 0.0), q[1] - b[4]);
                double dz = Math.max(Math.max(b[2] - q[2], 0.0), q[2] - b[5]);
                least = Math.min(least, Math.sqrt(dx * dx + dy * dy + dz * dz) - r);
            }
        }
        return least;
    }

    // ---------------------------------------------------------------- 3.5.7: the kakuja's own moves

    /**
     * 3.5.7 (the owner's spec): Thousand Jaws' tentacles each bite their own body again and again, shut on it as they bite; Endless Hunger's
     * three mouths bite its catch from round it, a tentacle coiled round it, the catch pulled into the great maw, which shuts on it;
     * Flesh Cataclysm draws in, bursts out with its tentacles thrown far and level, sweeps them round and comes back, every mouth roaring;
     * Undying Abomination's mending shows and goes; a bite of its own lands on its body. Every frame finite, built, and never jumping.
     */
    private static void kakujaMoves() {
        com.hex.kagune.control.bikaku.MawKakuja.Growth full = com.hex.kagune.control.bikaku.MawKakuja.growth(true, 500);
        int tentacles = com.hex.kagune.control.bikaku.MawKakuja.tentacles();
        // Thousand Jaws: each mouthed tentacle after its own body round it.
        double[][] bodies = new double[tentacles][];
        double[][] round = {{7.0, 1.0, 6.0}, {-7.5, 1.0, 4.0}, {2.0, 1.0, -8.0}, null, {1.0, 1.0, 3.2}, {-5.0, 1.0, -6.5}};
        System.arraycopy(round, 0, bodies, 0, tentacles);
        com.hex.kagune.control.bikaku.BikakuMove jaws = com.hex.kagune.control.bikaku.BikakuMove.JAWS;
        for (int i = 0; i < tentacles; i++) {
            java.util.List<Integer> starts = com.hex.kagune.control.bikaku.BikakuMove.jawsStarts(i);
            if (!com.hex.kagune.control.bikaku.MawKakuja.mouthed(i)) {
                check(starts.isEmpty(), "Thousand Jaws: the mouthless tentacle doesn't bite");
                continue;
            }
            check(starts.size() >= 4, "Thousand Jaws: every mouthed tentacle bites again and again (" + starts.size() + ")");
            for (int start : starts) {
                int bite = start + com.hex.kagune.control.bikaku.BikakuMove.JAWS_BITE;
                check(bite < jaws.ticks() && com.hex.kagune.control.bikaku.BikakuMove.jawsBites(i, bite), "each bite inside the move");
                com.hex.kagune.control.bikaku.MawKakuja.Act act = com.hex.kagune.control.bikaku.KakujaActs.act(jaws, bite, 300 + bite, bodies, null, null, true);
                double[][] line = com.hex.kagune.control.bikaku.MawKakuja.tentacle(i, 300 + bite, act.lunge()[i], act.reach()[i]);
                if (bite >= 4 && bite <= jaws.ticks() - 8) {
                    check(distance(line[line.length - 1], bodies[i]) < 0.05, "Thousand Jaws: tentacle " + i + "'s mouth is on its body as it bites (at " + bite + ")");
                }
                check(act.bite()[i] < 0.1, "Thousand Jaws: shut as it bites");
            }
        }
        com.hex.kagune.control.bikaku.MawKakuja.Act snap = com.hex.kagune.control.bikaku.KakujaActs.act(jaws, com.hex.kagune.control.bikaku.BikakuMove.JAWS_SNAPS[0] - 1,
            300, bodies, null, null, true);
        check(snap.grins() > 0.7, "Thousand Jaws: its grins open wide before they snap");
        // Endless Hunger.
        com.hex.kagune.control.bikaku.BikakuMove hunger = com.hex.kagune.control.bikaku.BikakuMove.HUNGER;
        double[] caught = {2.5, 1.0, 7.0};
        int[] biters = com.hex.kagune.control.bikaku.KakujaActs.hungerBiters(caught);
        check(biters.length == 3 && biters[0] != biters[1] && biters[1] != biters[2] && biters[0] >= 0, "Endless Hunger: three mouths go after it");
        for (int b : biters) {
            check(com.hex.kagune.control.bikaku.MawMotion.class != null && com.hex.kagune.control.bikaku.MawKakuja.mouthed(b) && b != com.hex.kagune.control.bikaku.KakujaActs.WRAPPER,
                "each of them a mouth, not the coiling tentacle");
        }
        check(hunger.land() < com.hex.kagune.control.bikaku.BikakuMove.HUNGER_COILED
            && com.hex.kagune.control.bikaku.BikakuMove.HUNGER_BITES[com.hex.kagune.control.bikaku.BikakuMove.HUNGER_BITES.length - 1] < com.hex.kagune.control.bikaku.BikakuMove.HUNGER_PULL
            && com.hex.kagune.control.bikaku.BikakuMove.HUNGER_PULL < com.hex.kagune.control.bikaku.BikakuMove.HUNGER_CRUSH
            && com.hex.kagune.control.bikaku.BikakuMove.HUNGER_CRUSH < com.hex.kagune.control.bikaku.BikakuMove.HUNGER_RELEASE
            && com.hex.kagune.control.bikaku.BikakuMove.HUNGER_RELEASE < hunger.ticks(), "Endless Hunger's beats come in order");
        for (int n = 0; n < com.hex.kagune.control.bikaku.BikakuMove.HUNGER_BITES.length; n++) {
            int at = com.hex.kagune.control.bikaku.BikakuMove.HUNGER_BITES[n];
            int k = com.hex.kagune.control.bikaku.KakujaActs.hungerBiter(n);
            double[] held = com.hex.kagune.control.bikaku.KakujaActs.hungerHeld(caught, at);
            com.hex.kagune.control.bikaku.MawKakuja.Act act = com.hex.kagune.control.bikaku.KakujaActs.act(hunger, at, 400 + at, null, held, caught, true);
            double[][] line = com.hex.kagune.control.bikaku.MawKakuja.tentacle(biters[k], 400 + at, act.lunge()[biters[k]], act.reach()[biters[k]]);
            check(distance(line[line.length - 1], held) < 0.6 && act.bite()[biters[k]] < 0.1, "Endless Hunger: each bite on the catch, shut ("
                + distance(line[line.length - 1], held) + ")");
            check(distance(held, caught) < 1e-9, "the catch is held where it was caught while it is bitten");
        }
        double[] coiledAt = com.hex.kagune.control.bikaku.KakujaActs.hungerHeld(caught, 20);
        com.hex.kagune.control.bikaku.MawKakuja.Act coiling = com.hex.kagune.control.bikaku.KakujaActs.act(hunger, 20, 420, null, coiledAt, caught, true);
        check(coiling.wrapper() == com.hex.kagune.control.bikaku.KakujaActs.WRAPPER && coiling.wrap() > 0.99, "Endless Hunger: a tentacle is coiled round its catch");
        double[][] coil = com.hex.kagune.control.bikaku.MawKakuja.wrapped(coiling.wrapper(), 420, coiling.wrapAt(), coiling.wrap());
        int round0 = 0;
        for (int k = coil.length - 10; k < coil.length; k++) {
            double flat = Math.hypot(coil[k][0] - caught[0], coil[k][2] - caught[2]);
            round0 += flat > 0.35 && flat < 0.85 && Math.abs(coil[k][1] - caught[1]) < 1.0 ? 1 : 0;
        }
        check(round0 >= 9, "its coil winds round the catch, close about it (" + round0 + ")");
        double[] inMaw = com.hex.kagune.control.bikaku.KakujaActs.hungerHeld(caught, com.hex.kagune.control.bikaku.BikakuMove.HUNGER_CRUSH);
        com.hex.kagune.control.bikaku.MawKakuja.Act crush = com.hex.kagune.control.bikaku.KakujaActs.act(hunger, com.hex.kagune.control.bikaku.BikakuMove.HUNGER_CRUSH, 446,
            null, inMaw, caught, true);
        com.hex.kagune.control.bikaku.MawKakuja.Act gaping = com.hex.kagune.control.bikaku.KakujaActs.act(hunger, com.hex.kagune.control.bikaku.BikakuMove.HUNGER_CRUSH - 2, 444,
            null, inMaw, caught, true);
        check(distance(inMaw, com.hex.kagune.control.bikaku.KakujaActs.MAW_GRIP) < 1e-9 && crush.maw() < 0.1 && gaping.maw() > 0.9,
            "Endless Hunger: pulled into the great maw, which gapes and shuts on it");
        com.hex.kagune.control.bikaku.MawKakuja.Act missed = com.hex.kagune.control.bikaku.KakujaActs.act(hunger, hunger.missedTicks() + 1, 500, null, null, caught, false);
        check(missed.lunge()[biters[0]] == null, "Endless Hunger missed draws back and ends");
        // Flesh Cataclysm.
        com.hex.kagune.control.bikaku.BikakuMove cata = com.hex.kagune.control.bikaku.BikakuMove.CATACLYSM;
        check(com.hex.kagune.control.bikaku.KakujaActs.squeeze(com.hex.kagune.control.bikaku.BikakuMove.CATA_DRAWN) < 0.85
            && com.hex.kagune.control.bikaku.KakujaActs.squeeze(cata.land() + 1) > 1.08, "Flesh Cataclysm draws in, then bursts out");
        check(com.hex.kagune.control.bikaku.KakujaActs.roar(cata.land()) > 0.99, "every mouth on it roars as it bursts");
        check(Math.abs(com.hex.kagune.control.bikaku.KakujaActs.turn(com.hex.kagune.control.bikaku.BikakuMove.CATA_SWEPT) - com.hex.kagune.control.bikaku.KakujaActs.SWEEP) < 1e-9
            && com.hex.kagune.control.bikaku.KakujaActs.turn(cata.ticks()) < 1e-9 && com.hex.kagune.control.bikaku.KakujaActs.thrown(cata.ticks()) < 1e-9
            && Math.abs(com.hex.kagune.control.bikaku.KakujaActs.squeeze(cata.ticks()) - 1.0) < 1e-6, "it sweeps round, then is back as it was");
        for (int i = 0; i < tentacles; i++) {
            double[] head = com.hex.kagune.control.bikaku.KakujaActs.cataclysmHead(i, cata.land() + 2, 500);
            check(Math.hypot(head[0], head[2]) > 9.0 && head[1] > -0.5 && head[1] < 4.0, "Flesh Cataclysm: tentacle " + i + " thrown far out and level ("
                + Math.hypot(head[0], head[2]) + ", " + head[1] + ")");
        }
        // Undying Abomination.
        com.hex.kagune.control.bikaku.BikakuMove undying = com.hex.kagune.control.bikaku.BikakuMove.UNDYING;
        check(com.hex.kagune.control.bikaku.KakujaActs.mend(0) < 1e-9 && com.hex.kagune.control.bikaku.KakujaActs.mend(100) > 0.99
            && com.hex.kagune.control.bikaku.KakujaActs.mend(undying.ticks()) < 1e-6, "Undying Abomination's mending shows, then goes");
        // A bite of its own lands on its body.
        double[] near = {5.5, 1.0, 5.0};
        int land = com.hex.kagune.control.bikaku.BikakuRules.Bite.TENTACLE.land();
        com.hex.kagune.control.bikaku.MawKakuja.Act own = com.hex.kagune.control.bikaku.KakujaActs.withAuto(com.hex.kagune.control.bikaku.MawKakuja.Act.still(), 4, near, land);
        double[][] ownLine = com.hex.kagune.control.bikaku.MawKakuja.tentacle(4, 700, own.lunge()[4], own.reach()[4]);
        check(distance(ownLine[ownLine.length - 1], near) < 0.01 && own.bite()[4] < 0.1, "a bite of its own lands on its body, shut");
        check(com.hex.kagune.control.bikaku.KakujaActs.withAuto(own, 4, new double[] {-5, 1, -5}, 3).lunge()[4] == near,
            "a tentacle already busy isn't taken for another bite");
        // Every frame of every move: finite, built, its tentacles never jumping.
        for (com.hex.kagune.control.bikaku.BikakuMove move : new com.hex.kagune.control.bikaku.BikakuMove[] {jaws, hunger, cata, undying}) {
            double[][][] last = null;
            for (double age = 0.0; age < move.ticks(); age += 0.25) {
                double[] held = move == hunger ? com.hex.kagune.control.bikaku.KakujaActs.hungerHeld(caught, age) : null;
                com.hex.kagune.control.bikaku.MawKakuja.Act act = com.hex.kagune.control.bikaku.KakujaActs.act(move, age, 300 + age, bodies, held, caught, true);
                double[][][] lines = new double[tentacles][][];
                for (int i = 0; i < tentacles; i++) {
                    lines[i] = i == act.wrapper() && act.wrap() > 0.0 ? com.hex.kagune.control.bikaku.MawKakuja.resampleForTests(
                        com.hex.kagune.control.bikaku.MawKakuja.wrapped(i, 300 + age, act.wrapAt(), act.wrap()), 9)
                        : com.hex.kagune.control.bikaku.MawKakuja.tentacle(i, 300 + age, act.lunge()[i], act.reach()[i], act.squeeze(), act.straight(), act.turn());
                }
                if (last != null) {
                    double most = 0.0;
                    for (int i = 0; i < tentacles; i++) {
                        for (int k = 0; k < lines[i].length; k++) {
                            most = Math.max(most, distance(lines[i][k], last[i][k]));
                        }
                    }
                    boolean burst = move == cata && age > com.hex.kagune.control.bikaku.BikakuMove.CATA_DRAWN && age <= cata.land() + 1;
                    check(most < (burst ? 4.0 : 1.6), move + " never jumps (" + most + " at " + age + ")");
                }
                last = lines;
                if (age == Math.floor(age) && ((int)age) % 8 == 0) {
                    boolean[] finite = {true};
                    int[] quads = {0};
                    com.hex.kagune.control.bikaku.MawKakuja.build((a, b, c, d, n, ca, cb, cc, cd) -> {
                        quads[0]++;
                        for (double[] q : new double[][] {a, b, c, d}) {
                            finite[0] &= Double.isFinite(q[0]) && Double.isFinite(q[1]) && Double.isFinite(q[2]);
                        }
                    }, full, 300 + age, 7, 0xA50F37, false, act);
                    check(finite[0] && quads[0] > 1500, move + " is built of finite quads (" + quads[0] + " at " + age + ")");
                }
            }
        }
    }

    /**
     * 3.5.8: held, each throw goes as far as it was held for: M1 bites, Devouring Maw and Predatory Drag land on bodies up to 30 blocks
     * out (on their own beats, slower), Ravenous Pursuit's worms at 20; the worm thrown far stays whole, snakes along its whole length, never
     * jumps, keeps out of his body and ends at rest; wound up, it draws back as it is held.
     */
    private static void heldFar() {
        double[] far = {1.0, 0.99, 28.0};
        for (com.hex.kagune.control.bikaku.BikakuRules.Bite bite : new com.hex.kagune.control.bikaku.BikakuRules.Bite[] {
            com.hex.kagune.control.bikaku.BikakuRules.Bite.SNAP, com.hex.kagune.control.bikaku.BikakuRules.Bite.LUNGE,
            com.hex.kagune.control.bikaku.BikakuRules.Bite.MAUL}) {
            double k = com.hex.kagune.control.bikaku.BikakuRules.slower(30.0, com.hex.kagune.control.bikaku.BikakuRules.REACH);
            MawMotion.Shape shut = MawMotion.of(40 + bite.land() * k, 7, bite, bite.land() * k, far, 30.0);
            double[] head = shut.path()[shut.path().length - 1];
            check(distance(head, far) < 0.25 && shut.aperture() < 0.1, bite + " held full lands on a body 28 blocks out, shut (" + distance(head, far) + ")");
            check(length(shut) > 25.0, bite + " thrown 28 blocks is that long (" + length(shut) + ")");
            check(MawMotion.of(40, 7, bite, bite.ticks() * k + 0.01, far, 30.0).path().length == MawMotion.idle(40, 7).path().length
                && distance(MawMotion.of(40, 7, bite, bite.ticks() * k + 0.01, far, 30.0).path()[0], MawMotion.idle(40, 7).path()[0]) < 1e-9,
                bite + " held full is back at rest only after its longer time");
            MawMotion.Shape last = null;
            for (double age = 0.0; age < bite.ticks() * k; age += 0.25) {
                MawMotion.Shape s = MawMotion.of(40 + age, 7, bite, age, far, 30.0);
                double[][] drawn = MawMotion.around(s.path(), 1.0, s.aperture());
                check(depthInHim(drawn, 1.0, s.aperture()) < 0.03, bite + " thrown far is drawn round him (" + age + ")");
                if (last != null) {
                    // (it covers 30 blocks in about 7 ticks: over a block a quarter tick is its speed; a flip through him would be several)
                    check(moved(last.path(), s.path()) < 2.6, bite + " thrown far never jumps (" + moved(last.path(), s.path()) + " at " + age + ")");
                }
                last = s;
            }
            // It snakes along its whole length: well off the straight line in its first half too.
            KakujaModel.Spine spine = MawWorm.spine(shut.path(), shut.dorsal(), 120);
            double off = 0.0;
            for (int i = 10; i < 60; i++) {
                off = Math.max(off, Math.abs(spine.p()[i][0] - (MawMotion.shoulder()[0] + (far[0] - MawMotion.shoulder()[0]) * (spine.p()[i][2] - MawMotion.shoulder()[2])
                    / (far[2] - MawMotion.shoulder()[2]))));
            }
            check(off > 0.4, bite + " thrown far snakes along its first half too (" + off + ")");
        }
        double[] ground = {1.0, 0.99, 28.0};
        for (com.hex.kagune.control.bikaku.BikakuMove move : new com.hex.kagune.control.bikaku.BikakuMove[] {com.hex.kagune.control.bikaku.BikakuMove.MAW,
            com.hex.kagune.control.bikaku.BikakuMove.DRAG}) {
            double k = com.hex.kagune.control.bikaku.BikakuRules.slower(30.0, move.baseReach());
            MawMotion.Shape shut = MawMotion.move(40, 7, move, move.land() * k, ground, null, true, 30.0);
            double[] head = shut.path()[shut.path().length - 1];
            double[] aim = move == com.hex.kagune.control.bikaku.BikakuMove.MAW ? com.hex.kagune.control.bikaku.BikakuRules.reach(ground, 30.0)
                : MawMotion.dragReach(ground, 30.0);
            check(distance(head, aim) < 0.9 && shut.aperture() < 0.1, move + " held full bites 28 blocks out (" + distance(head, aim) + ")");
            double[] end = MawMotion.move(40, 7, move, move.ticks() * k + 0.01, ground, null, true, 30.0).path()[0];
            check(distance(end, MawMotion.idle(40, 7).path()[0]) < 1e-9, move + " ends at rest after its longer time");
        }
        double[][] three = {{6.0, 0.99, 17.0}, {-7.0, 0.99, 16.0}, {0.5, 0.99, 19.0}};
        double pk = com.hex.kagune.control.bikaku.BikakuRules.slower(20.0, com.hex.kagune.control.bikaku.BikakuRules.PURSUIT_REACH);
        for (int i = 0; i < 3; i++) {
            double lands = com.hex.kagune.control.bikaku.BikakuMove.pursuitLands(i) * pk;
            MawMotion.Worm[] worms = MawMotion.pursuit(40 + lands, 7, three, lands, 20.0);
            double[] head = worms[i].shape().path()[worms[i].shape().path().length - 1];
            check(distance(head, three[i]) < 0.6, "Ravenous Pursuit held to 20: worm " + i + " bites its body (" + distance(head, three[i]) + ")");
        }
        // Wound up: drawn back further the longer it is held; finite.
        double before = MawMotion.charging(40, 7, 0.0).path()[MawMotion.charging(40, 7, 0.0).path().length - 1][2];
        double after = MawMotion.charging(40, 7, 1.0).path()[MawMotion.charging(40, 7, 1.0).path().length - 1][2];
        check(after < before - 0.8, "wound up, its head draws back behind him (" + before + " to " + after + ")");
        for (double[] q : MawMotion.charging(77, 7, 0.6).path()) {
            check(Double.isFinite(q[0]) && Double.isFinite(q[1]) && Double.isFinite(q[2]), "its wind-up is finite");
        }
    }

    /**
     * 3.5.9 (the owner: "go around blocks like a worm, sort of, and actually go around them"): the way round blocks: straight when nothing is
     * in the way; round a wall when one is, every leg of it clear, no longer than its reach, a few points; none out of a sealed box, none
     * past its reach, none through a gap only a corner wide; the same wherever in the world (negative places too).
     */
    private static void routes() {
        com.hex.kagune.control.bikaku.WormRoute.Open air = (x, y, z) -> y >= 0;
        double[][] straight = com.hex.kagune.control.bikaku.WormRoute.find(new double[] {0.5, 1.5, 0.5}, new double[] {0.5, 1.5, 20.5}, 30.0, air);
        check(straight != null && straight.length == 2, "nothing in the way: a straight line");
        for (int[] at : new int[][] {{0, 0, 0}, {-3000, 0, -77}, {12345, 64, -9876}}) {
            int ox = at[0];
            int oy = at[1];
            int oz = at[2];
            // A wall 7 wide and 5 high across the way, 5 out.
            com.hex.kagune.control.bikaku.WormRoute.Open walled = (x, y, z) -> y >= oy && !(z == oz + 5 && x >= ox - 3 && x <= ox + 3 && y <= oy + 4);
            double[] from = {ox + 0.5, oy + 1.5, oz + 0.5};
            double[] to = {ox + 0.5, oy + 1.5, oz + 10.5};
            double[][] round = com.hex.kagune.control.bikaku.WormRoute.find(from, to, 30.0, walled);
            check(round != null && round.length >= 3 && round.length <= com.hex.kagune.control.bikaku.WormRoute.MOST_POINTS, "a wall in the way: round it ("
                + (round == null ? "none" : round.length + " points") + " at " + ox + ")");
            if (round != null) {
                for (int i = 1; i < round.length; i++) {
                    check(com.hex.kagune.control.bikaku.WormRoute.clear(round[i - 1], round[i], walled), "every leg of the way round is clear");
                }
                double length = com.hex.kagune.control.bikaku.WormRoute.length(round);
                check(length > 10.0 && length <= 30.0, "the way round is longer than straight, within its reach (" + length + ")");
                check(distance(round[0], from) < 1e-9 && distance(round[round.length - 1], to) < 1e-9, "it starts and ends where it was asked");
                double[] half = com.hex.kagune.control.bikaku.WormRoute.at(round, length / 2.0);
                check(Double.isFinite(half[0]) && distance(com.hex.kagune.control.bikaku.WormRoute.at(round, length + 5.0), to) < 1e-9
                    && com.hex.kagune.control.bikaku.WormRoute.upTo(round, length * 0.5).length >= 2, "points along it");
            }
            check(com.hex.kagune.control.bikaku.WormRoute.find(from, to, 10.5, walled) == null, "no way round within too short a reach");
        }
        // Sealed in a box: no way out.
        com.hex.kagune.control.bikaku.WormRoute.Open box = (x, y, z) -> !(Math.abs(x) == 3 || Math.abs(y - 2) == 3 || Math.abs(z) == 3) || Math.abs(x) > 3
            || Math.abs(y - 2) > 3 || Math.abs(z) > 3;
        check(com.hex.kagune.control.bikaku.WormRoute.find(new double[] {0.5, 2.5, 0.5}, new double[] {0.5, 2.5, 10.5}, 30.0, box) == null, "sealed in: no way out");
        // A gap only a corner wide (two solid columns touching at a corner, a wall either side): never through it.
        com.hex.kagune.control.bikaku.WormRoute.Open corner = (x, y, z) -> {
            if (y < 0 || y > 6) {
                return y > 6;
            }
            if (z == 5) {
                return x == 0; // a hole one wide at x 0 in the wall at z 5 ...
            }
            if (z == 6) {
                return x == 1; // ... but the next wall's hole is one over: only their corners meet
            }
            return true;
        };
        double[][] squeezed = com.hex.kagune.control.bikaku.WormRoute.find(new double[] {0.5, 3.5, 0.5}, new double[] {0.5, 3.5, 12.5}, 30.0, corner);
        if (squeezed != null) {
            for (int i = 1; i < squeezed.length; i++) {
                check(com.hex.kagune.control.bikaku.WormRoute.clear(squeezed[i - 1], squeezed[i], corner), "never through a corner-wide gap");
            }
        }
        check(true, "corner gap handled");
        // Laid along a way round: unchanged for a straight way; the start and the end on the way's; halfway along the line halfway along it.
        double[] origin = MawMotion.shoulder();
        double[] end = {0.5, 0.99, 10.0};
        double[][] wayStraight = {origin, end};
        double[] mid = {(origin[0] + end[0]) / 2, (origin[1] + end[1]) / 2, (origin[2] + end[2]) / 2};
        check(distance(MawMotion.along(mid, origin, end, wayStraight), mid) < 1e-12, "a straight way changes nothing");
        com.hex.kagune.control.bikaku.WormRoute.Open wall = (x, y, z) -> y >= 0 && !(z == 5 && x >= -4 && x <= 2 && y <= 3);
        double[][] way = com.hex.kagune.control.bikaku.WormRoute.find(origin, end, 30.0, wall);
        check(way != null && way.length >= 3, "a way round the wall");
        check(distance(MawMotion.along(end, origin, end, way), end) < 1e-9 && distance(MawMotion.along(origin, origin, end, way), origin) < 1e-9,
            "its start and its end stay where they are");
        double[] laidMid = MawMotion.along(mid, origin, end, way);
        check(distance(laidMid, com.hex.kagune.control.bikaku.WormRoute.at(way, com.hex.kagune.control.bikaku.WormRoute.length(way) / 2.0)) < 1e-6,
            "halfway along the line is halfway along the way");
        double[] behind = {0.5, 1.0, -1.5};
        check(distance(MawMotion.along(behind, origin, end, way), behind) < 1e-12, "the coils behind him stay where they are");
        // A bite held to 16 round the wall: its drawn line never in the wall, its mouth where the way ends as it bites.
        com.hex.kagune.control.bikaku.BikakuRules.Bite snap = com.hex.kagune.control.bikaku.BikakuRules.Bite.SNAP;
        double k = com.hex.kagune.control.bikaku.BikakuRules.slower(16.0, com.hex.kagune.control.bikaku.BikakuRules.REACH);
        int inWall = 0;
        int points = 0;
        for (double age = 0.0; age < snap.ticks() * k; age += 0.5) {
            MawMotion.Shape bitten = MawMotion.of(60 + age, 7, snap, age, end, 16.0);
            double[][] laid = MawMotion.along(MawMotion.dense(bitten.path()), origin, end, way);
            for (double[] q : laid) {
                points++;
                inWall += wall.open((int)Math.floor(q[0]), (int)Math.floor(q[1]), (int)Math.floor(q[2])) ? 0 : 1;
            }
            if (Math.abs(age - snap.land() * k) < 0.26) {
                check(distance(laid[laid.length - 1], end) < 0.3, "round the wall, it bites where its way ends (" + distance(laid[laid.length - 1], end) + ")");
            }
        }
        check(inWall == 0, "round the wall, none of it goes into the wall (" + inWall + " of " + points + ")");
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
