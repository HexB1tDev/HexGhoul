package com.hex.kagune.control;

import com.hex.kagune.control.ukaku.CentipedeTail;
import com.hex.kagune.control.ukaku.KakujaModel;
import com.hex.kagune.control.ukaku.UkakuModel;
import com.hex.kagune.control.client.PathCards;
import com.hex.kagune.control.graft.GraftArmor;
import com.hex.kagune.control.graft.GraftModel;
import com.hex.kagune.control.graft.GraftMotion;
import com.hex.kagune.control.graft.GraftMove;
import com.hex.kagune.control.graft.GraftRig;
import com.hex.kagune.control.graft.GraftRules;
import com.hex.kagune.control.graft.GraftSkin;
import com.hex.kagune.control.graft.GraftStage;
import com.hex.kagune.gameplay.KaguneForm;
import com.hex.kagune.control.ukaku.WingForm;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 7.0.0: Graft DNA, dependency-free: its stages and their limbs, every move's beats, the rig keeping every limb out of his body
 * through every move, the limbs growing out of his back, the meshes, the kakuja's hide and head, the rules, his panel and his card.
 */
public final class GraftTests {
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
        stages();
        moves();
        keys();
        rig();
        motion();
        strikes();
        hunting();
        meshes();
        hide();
        head();
        rules();
        mutations();
        body();
        tails();
        centipedes();
        thorns();
        eyes();
        kakuja();
        pvp();
        panel();
        card();
        if (!FAILURES.isEmpty()) {
            FAILURES.stream().limit(40).forEach(failure -> System.err.println("FAILED: " + failure));
            System.err.println(FAILURES.size() + " of " + (passed + FAILURES.size()) + " checks failed");
            System.exit(1);
        }
        System.out.println("Graft tests: " + passed + " checks passed");
    }

    private static void stages() {
        Set<String> ids = new HashSet<>();
        int last = -1;
        for (GraftStage stage : GraftStage.values()) {
            check(ids.add(stage.id()) && stage.id().matches("[a-z_]+"), "stage ids are unique plain words: " + stage.id());
            check(GraftStage.fromId(stage.id()) == stage && GraftStage.byOrdinal(stage.ordinal()) == stage, "stage round-trips: " + stage);
            check(stage.mastery() > last, "stages open in order: " + stage);
            last = stage.mastery();
            check(stage.length(0) > 0.0 && stage.length(1000) >= stage.length(stage.mastery()) && stage.width(1000) > 0.0,
                "a stage grows with mastery: " + stage);
        }
        check(GraftStage.byOrdinal(-1) == GraftStage.TRANSPLANT && GraftStage.byOrdinal(99) == GraftStage.TRANSPLANT
            && GraftStage.fromId("owl") == null, "an unknown stage reads as none (the first on the wire)");
        check(GraftStage.TRANSPLANT.limbs(0) == 1 && GraftStage.TRANSPLANT.limbs(GraftStage.TWIN_MASTERY) == 2,
            "Rize's kakuhou grows a second tail with mastery");
        check(GraftStage.CENTIPEDE.limbs(1000) == 3 && GraftStage.THORNS.limbs(1000) == 4 && GraftStage.DRAGON.limbs(1000) == 5
            && GraftStage.KAKUJA.limbs(1000) == 2, "three centipedes, four thorns, five crescents, two colossi");
        check(GraftStage.highest(0) == GraftStage.TRANSPLANT && GraftStage.highest(1000) == GraftStage.DRAGON,
            "the highest base stage is never the kakuja");
        check(GraftStage.usable(GraftStage.THORNS, 100) == GraftStage.TRANSPLANT && GraftStage.usable(GraftStage.KAKUJA, 1000) == GraftStage.DRAGON
            && GraftStage.usable(GraftStage.CENTIPEDE, 1000) == GraftStage.CENTIPEDE, "a stage asked for is held to the mastery, never the kakuja");
        check(GraftStage.next(GraftStage.DRAGON, 1000) == GraftStage.TRANSPLANT && GraftStage.next(GraftStage.TRANSPLANT, 0) == GraftStage.TRANSPLANT
            && GraftStage.next(GraftStage.TRANSPLANT, 300) == GraftStage.CENTIPEDE, "K held steps round the unlocked base stages");
        check(GraftStage.KAKUJA.mastery() == GraftRules.KAKUJA_MASTERY, "the kakuja opens where its rules say");
        check(GraftStage.KAKUJA.damage() > GraftStage.DRAGON.damage() && GraftStage.DRAGON.damage() > GraftStage.TRANSPLANT.damage(),
            "each stage hits harder than the last");
    }

    private static void moves() {
        for (GraftMove move : GraftMove.values()) {
            if (move == GraftMove.NONE) {
                continue;
            }
            int limbs = move.stage().limbs(1000);
            List<GraftMove.Beat> beats = move.beats(limbs);
            check(!beats.isEmpty() || move.travels(), "a move moves its kagune: " + move);
            check(move.ticks() > 0 && move.cost() >= 0.0F && move.cooldown() >= 0, "a move has a length, a cost and a cooldown: " + move);
            check(move.mastery() >= move.stage().mastery() || move.stage() == GraftStage.TRANSPLANT, "a move opens with its stage: " + move);
            for (GraftMove.Beat beat : beats) {
                check(beat.start() >= 0 && beat.start() <= beat.land() && beat.land() <= beat.end() && beat.end() <= move.ticks() + 40,
                    "a beat winds up, lands, lets go in order: " + move + " " + beat);
                check(beat.land() <= move.ticks(), "a beat lands within its move: " + move + " " + beat);
                check(beat.limb() == GraftMove.ALL || beat.limb() == GraftMove.ARM || beat.limb() >= 0 && beat.limb() < limbs,
                    "a beat names a limb the stage has: " + move + " " + beat);
                check(beat.reach() > 0.0 && beat.power() >= 0.0F && beat.power() <= 3.0F, "a beat has a reach and a sane power: " + move);
            }
            for (int n = 1; n <= 5; n++) {
                for (GraftMove.Beat beat : move.beats(n)) {
                    check(beat.limb() < Math.max(1, n), "beats fit fewer limbs too: " + move + " at " + n);
                }
            }
        }
        for (GraftStage stage : GraftStage.values()) {
            GraftMove first = GraftMove.firstStrike(stage);
            check(first.slot() == GraftMove.Slot.M1 && first.combo(), "every stage has its own M1 string: " + stage);
            GraftMove at = first;
            Set<GraftMove> seen = new HashSet<>();
            while (at != null && at != GraftMove.NONE && seen.add(at)) {
                at = GraftMove.nextStrike(at, stage);
            }
            check(seen.size() >= 3, "a string runs at least three blows: " + stage);
        }
        List<GraftMove> track = GraftMove.track();
        for (int i = 1; i < track.size(); i++) {
            check(track.get(i).mastery() >= track.get(i - 1).mastery(), "the unlock track is in mastery order");
        }
        check(track.size() >= 20, "there are many moves to learn (" + track.size() + ")");
        for (GraftStage stage : GraftStage.values()) {
            int string = 0;
            for (GraftMove at = GraftMove.firstStrike(stage); at.combo() && at.stage() == stage && string < 10;
                 at = GraftMove.values()[at.ordinal() + 1]) {
                string++;
            }
            check(string >= (stage == GraftStage.KAKUJA ? 6 : 5), "each stage's M1 string runs five blows (the kakuja's six): " + stage);
        }
        check(GraftMove.CHECKMATE.beats(2).stream().filter(b -> b.limb() == GraftMove.ARM).count() >= 4,
            "the kakuja's checkmate is claws and jaws as well as its colossi");
    }

    private static void keys() {
        for (GraftStage stage : GraftStage.values()) {
            int moves = 0;
            for (GraftMove.Slot slot : GraftMove.Slot.values()) {
                GraftMove move = GraftMove.of(slot, stage);
                check(move != null, "every key answers on every stage: " + slot + " " + stage);
                if (move == null || move == GraftMove.NONE) {
                    continue;
                }
                moves++;
                check(move.stage().ordinal() <= stage.ordinal(), "a key never reaches a later stage's move: " + slot + " " + stage);
                check(move.slot() == slot, "a move is on its own key: " + move);
                if (stage != GraftStage.KAKUJA) {
                    check(move.stage() != GraftStage.KAKUJA, "the kakuja's moves are only in the kakuja: " + move);
                }
            }
            check(moves >= 6, "each stage has several moves (" + stage + ": " + moves + ")");
        }
        // The user's ability list: the base kagune's six on abilities 1-6, the evolved kagune's four (it keeps 5 and 6), the kakuja's five.
        // 7.0.2, the user's rework: the two tails' own four on Rize's Kakuhou (the centipedes and the thorns keep the six until theirs).
        GraftMove[] thorns = {GraftMove.CRUCIFIXION, GraftMove.BREACH, GraftMove.DRAGNET, GraftMove.CATAPULT, GraftMove.NONE, GraftMove.NONE};
        GraftMove[] tails = {GraftMove.MOCKINGBIRD, GraftMove.OWLS_EXECUTION, GraftMove.SCORPION_VAULT, GraftMove.HUMAN_PUPPET, GraftMove.NONE,
            GraftMove.NONE};
        GraftMove[] centipedes = {GraftMove.HUNGRY_MAW, GraftMove.LOCOMOTION, GraftMove.AMBUSH, GraftMove.CARAPACE, GraftMove.NONE, GraftMove.NONE};
        GraftMove[] evolved = {GraftMove.STORM, GraftMove.BLOOM, GraftMove.CURTAIN, GraftMove.KINGS_GAME, GraftMove.NONE, GraftMove.NONE};
        GraftMove[] kakuja = {GraftMove.CHECKMATE, GraftMove.HUNT, GraftMove.RECOVERY, GraftMove.PERFORMANCE, GraftMove.NONE, GraftMove.NONE};
        GraftMove.Slot[] abilities = {GraftMove.Slot.Z, GraftMove.Slot.J, GraftMove.Slot.V, GraftMove.Slot.B, GraftMove.Slot.N, GraftMove.Slot.H};
        for (int i = 0; i < abilities.length; i++) {
            check(GraftMove.of(abilities[i], GraftStage.TRANSPLANT) == tails[i], "two tails ability " + (i + 1) + " is " + tails[i]);
            check(GraftMove.of(abilities[i], GraftStage.CENTIPEDE) == centipedes[i], "centipede ability " + (i + 1) + " is " + centipedes[i]);
            check(GraftMove.of(abilities[i], GraftStage.THORNS) == thorns[i], "four thorns ability " + (i + 1) + " is " + thorns[i]);
            check(GraftMove.of(abilities[i], GraftStage.DRAGON) == evolved[i], "evolved ability " + (i + 1) + " is " + evolved[i]);
            check(GraftMove.of(abilities[i], GraftStage.KAKUJA) == kakuja[i], "kakuja ability " + (i + 1) + " is " + kakuja[i]);
        }
    }

    private static final double[] AIM = {0.3, 1.3, 3.2};

    private static void rig() {
        for (GraftStage stage : GraftStage.values()) {
            int n = stage.limbs(1000);
            double length = stage.length(1000);
            double[][][] rest = GraftRig.restTips(stage, 1000);
            check(rest.length == n, "every limb has a rest pose: " + stage);
            for (int limb = 0; limb < n; limb++) {
                double[] root = rest[limb][0];
                double[] tip = rest[limb][1];
                check(root[2] < 0.0 && root[1] > 0.6 && root[1] < 2.0, "a limb leaves his back: " + stage + " " + limb);
                check(Math.hypot(tip[0], tip[2]) > GraftRig.BODY_RADIUS || tip[1] > GraftRig.BODY_TOP, "at rest a point is clear of him: " + stage);
            }
            List<GraftMove> moves = new ArrayList<>();
            moves.add(GraftMove.NONE);
            for (GraftMove move : GraftMove.values()) {
                if (move != GraftMove.NONE && GraftMove.of(move.slot(), stage) == move || move.combo() && move.stage() == stage
                    || move.followUp() && followed(move, stage)) {
                    moves.add(move);
                }
            }
            for (GraftMove move : moves) {
                for (boolean crouching : new boolean[] {false, true}) {
                    GraftRig.Body body = new GraftRig.Body(0.2, crouching, 0.0, true, 0.0, 0.0, 37.0);
                    for (int age = 0; age <= Math.max(1, move.ticks()); age++) {
                        for (int limb = 0; limb < n; limb++) {
                            double[] held = move.holds() && age > 8 ? AIM : null;
                            GraftRig.Pose pose = GraftRig.pose(new GraftRig.Input(stage, n, limb, length, body, move, age, AIM, null, held, 0.35));
                            double[][] p = pose.points();
                            check(p.length >= 4 && finite(p), "a pose is a finite line: " + stage + " " + move + " " + age);
                            check(pose.jaw() >= 0.0 && pose.jaw() <= 1.0 && pose.length() > 0.5 && pose.response() > 0.0,
                                "a pose's mouth, length and speed are sane: " + stage + " " + move);
                            double top = GraftRig.BODY_TOP - (crouching ? 0.3 : 0.0);
                            for (int i = 2; i < p.length; i++) {
                                boolean inside = p[i][1] > -0.2 && p[i][1] < top && Math.hypot(p[i][0], p[i][2]) < GraftRig.BODY_RADIUS * 0.75 - 1.0E-6;
                                check(!inside, "a limb never runs through him: " + stage + " " + move + " limb " + limb + " at " + age);
                            }
                            double reach = 0.0;
                            for (double[] point : p) {
                                reach = Math.max(reach, Math.hypot(point[0], point[2]));
                            }
                            check(reach < length * 2.4 + 4.0, "a limb stays within its reach: " + stage + " " + move);
                        }
                    }
                }
            }
        }
        double[][] cleared = GraftRig.clear(new double[][] {{0.0, 1.0, -0.2}, {0.0, 1.0, -0.3}, {0.0, 1.0, 0.1}, {0.05, 1.0, 0.0}}, 1, false);
        check(Math.hypot(cleared[2][0], cleared[2][2]) >= GraftRig.BODY_RADIUS - 1.0E-9 && cleared[3][0] > 0.0,
            "a point inside him goes straight out, its limb's way when dead centre");
        check(cleared[0][2] == -0.2 && cleared[1][2] == -0.3, "the root and the point leaving his back stay where they are");
    }

    private static void motion() {
        GraftStage stage = GraftStage.CENTIPEDE;
        GraftRig.Pose pose = GraftRig.pose(new GraftRig.Input(stage, 3, 0, stage.length(1000), GraftRig.Body.STILL, GraftMove.NONE, 0.0, null,
            null, null, 0.3));
        GraftMotion.Limb limb = new GraftMotion.Limb();
        limb.follow(pose, 100.0);
        double[][] first = limb.points();
        double[] root = pose.points()[0];
        double[] tip = pose.points()[pose.points().length - 1];
        check(distance(first[first.length - 1], root) < distance(tip, root) * 0.3, "a limb starts coiled in its root (it grows out)");
        for (int t = 1; t <= 120; t++) {
            limb.follow(pose, 100.0 + t * 0.5);
        }
        double[][] settled = limb.points();
        check(finite(settled) && distance(settled[settled.length - 1], tip) < 0.6, "it settles on its pose");
        check(Math.abs(limb.jaw() - pose.jaw()) < 0.05 && Math.abs(limb.length() - pose.length()) < 0.2, "its mouth and length follow too");
        limb.follow(pose, 90.0);
        check(distance(limb.points()[limb.points().length - 1], root) < distance(tip, root) * 0.3, "set back in time, it grows out again");
    }

    /** The masteries the strike checks take each stage at (every limb it has out). */
    private static final int[] STRIKE_MASTERY = {200, 350, 600, 800, 1000};

    /**
     * What is seen is what hits: on the tick each blow lands (the tick the server hurts on), the limb as the game draws it (its
     * springs followed from rest through the move) reaches where the server strikes: a thrust's, bite's, rip's or seizure's point, a
     * slam's ground. Aimed straight ahead, off to a side, and out of reach (held to the beat's reach).
     */
    private static void strikes() {
        double[][] aims = {{0.0, 1.0, 4.5}, {1.8, 1.4, 5.0}, {-1.5, 0.8, 3.6}, {0.0, 1.2, 30.0}};
        for (GraftStage stage : GraftStage.values()) {
            int mastery = STRIKE_MASTERY[stage.ordinal()];
            int n = stage.limbs(mastery);
            double length = stage.length(mastery);
            for (GraftMove move : GraftMove.values()) {
                if (move == GraftMove.NONE || !usable(move, stage)) {
                    continue;
                }
                for (double[] aim : aims) {
                    double[][] targets = null;
                    if (move.beats(n).stream().anyMatch(b -> b.has(GraftMove.MULTI))) {
                        targets = new double[n][];
                        for (int i = 0; i < n; i++) {
                            targets[i] = new double[] {aim[0] + (i - (n - 1) * 0.5) * 1.6, aim[1], aim[2] - 0.4 * (i % 2)};
                        }
                    }
                    GraftMotion.Limb[] legs = new GraftMotion.Limb[n];
                    for (int i = 0; i < n; i++) {
                        legs[i] = new GraftMotion.Limb();
                    }
                    double step = 0.25;
                    int lastLand = 0;
                    for (GraftMove.Beat beat : move.beats(n)) {
                        lastLand = Math.max(lastLand, beat.land());
                    }
                    for (double t = -24.0; t <= lastLand + 1.0E-9; t += step) {
                        for (int limb = 0; limb < n; limb++) {
                            legs[limb].follow(GraftRig.pose(strikeInput(stage, n, limb, length, move, t, aim, targets)), 100.0 + t);
                        }
                        for (GraftMove.Beat beat : move.beats(n)) {
                            if (Math.abs(t - beat.land()) > 1.0E-9 || !reaches(beat.kind()) || beat.has(GraftMove.CARRY)) {
                                continue; // a carry lands no blow: it goes on holding what was caught
                            }
                            for (int limb = 0; limb < n; limb++) {
                                if (!beat.involves(limb)) {
                                    continue;
                                }
                                double[] hit = GraftRig.hitPoint(strikeInput(stage, n, limb, length, move, t, aim, targets), beat);
                                double miss = nearEnd(legs[limb].points(), hit);
                                check(miss <= 0.9, stage + " " + move + " limb " + limb + " " + beat.kind() + " at tick " + beat.land() + " aimed at "
                                    + java.util.Arrays.toString(aim) + ": the drawn limb lands where the server hits (" + String.format("%.2f", miss)
                                    + " blocks off)");
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * The mouths hunt: with prey near, the head follows it (closer to it than it ever rests), gapes wide, snaps shut and lunges at
     * it; with none, it jerks round to new bearings and freezes on each.
     */
    private static void hunting() {
        Object[][] mouths = {{GraftStage.CENTIPEDE, 0}, {GraftStage.KAKUJA, 0}, {GraftStage.KAKUJA, 1}};
        for (Object[] m : mouths) {
            GraftStage stage = (GraftStage)m[0];
            int limb = (Integer)m[1];
            int n = stage.limbs(1000);
            check(GraftRig.maw(stage, limb), stage + " limb " + limb + " is a mouth");
            double[] prey = {limb == 0 ? 1.5 : -1.5, 1.2, 5.0};
            double near = 0.0;
            double aimed = 0.0;
            double idleAim = 0.0;
            double widest = 0.0;
            double shut = 1.0;
            double closest = Double.MAX_VALUE;
            int samples = 0;
            for (double t = 0.0; t < 240.0; t += 0.5) {
                GraftRig.Pose hunting = GraftRig.pose(new GraftRig.Input(stage, n, limb, stage.length(1000),
                    new GraftRig.Body(0.0, false, 0.0, true, 0.0, 0.0, t, prey), GraftMove.NONE, 0.0, null, null, null, 0.3));
                GraftRig.Pose idle = GraftRig.pose(new GraftRig.Input(stage, n, limb, stage.length(1000),
                    new GraftRig.Body(0.0, false, 0.0, true, 0.0, 0.0, t), GraftMove.NONE, 0.0, null, null, null, 0.3));
                double[] tip = hunting.points()[hunting.points().length - 1];
                near += distance(tip, prey);
                aimed += headAngle(hunting.points(), prey);
                idleAim += headAngle(idle.points(), prey);
                closest = Math.min(closest, distance(tip, prey));
                widest = Math.max(widest, hunting.jaw());
                shut = Math.min(shut, hunting.jaw());
                samples++;
                check(finite(hunting.points()) && hunting.response() <= 1.0, stage + " limb " + limb + ": a hunting mouth follows fast and stays finite");
            }
            check(aimed / samples < 30.0 && aimed / samples < idleAim / samples - 15.0, stage + " limb " + limb + ": its head turns on its prey ("
                + String.format("%.0f", aimed / samples) + " degrees off it on average, " + String.format("%.0f", idleAim / samples) + " without prey)");
            check(widest > 0.95 && shut < 0.05, stage + " limb " + limb + ": it gapes wide and snaps shut at its prey");
            check(closest < near / samples - 0.8, stage + " limb " + limb + ": it lunges at its prey");
            // With none: it holds each bearing still (frozen) and turns to new ones (a jerk between).
            double moved = 0.0;
            double held = 0.0;
            double[] last = null;
            for (double t = 0.0; t < 240.0; t += 1.0) {
                GraftRig.Pose idle = GraftRig.pose(new GraftRig.Input(stage, n, limb, stage.length(1000),
                    new GraftRig.Body(0.0, false, 0.0, true, 0.0, 0.0, t), GraftMove.NONE, 0.0, null, null, null, 0.3));
                double[] tip = idle.points()[idle.points().length - 1];
                if (last != null) {
                    double step = distance(tip, last);
                    moved = Math.max(moved, step);
                    held += step < 0.12 ? 1 : 0;
                }
                last = tip;
            }
            check(moved > 0.4 && held > 100, stage + " limb " + limb + ": without prey it jerks to new bearings (fastest " + String.format("%.2f", moved)
                + " a tick) and freezes on them (" + (int)held + " still ticks of 239)");
        }
    }

    /** Degrees between where a limb's head points (from its neck to its point) and {@code at}. */
    private static double headAngle(double[][] points, double[] at) {
        double[] neck = points[(int)Math.round((points.length - 1) * 0.7)];
        double[] tip = points[points.length - 1];
        double[] a = {tip[0] - neck[0], tip[1] - neck[1], tip[2] - neck[2]};
        double[] b = {at[0] - neck[0], at[1] - neck[1], at[2] - neck[2]};
        double cos = (a[0] * b[0] + a[1] * b[1] + a[2] * b[2]) / Math.max(1.0E-9, Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2])
            * Math.sqrt(b[0] * b[0] + b[1] * b[1] + b[2] * b[2]));
        return Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, cos))));
    }

    /** True if {@code move} can be made in {@code stage} (its own string, the dash, the abilities its keys start). */
    private static boolean usable(GraftMove move, GraftStage stage) {
        if (move == GraftMove.STEP) {
            return true;
        }
        if (move.followUp()) {
            return followed(move, stage);
        }
        if (move.combo() || move == GraftMove.ACCORD) {
            return move.stage() == stage || move == GraftMove.ACCORD && stage == GraftStage.TRANSPLANT;
        }
        for (GraftMove.Slot slot : GraftMove.Slot.values()) {
            if (slot != GraftMove.Slot.M1 && slot != GraftMove.Slot.M2 && GraftMove.of(slot, stage) == move) {
                return true;
            }
        }
        return false;
    }

    /** 7.0.2: true if {@code move} is a follow-up of a move a key starts in {@code stage} (the counter, the dive, the puppet's). */
    private static boolean followed(GraftMove move, GraftStage stage) {
        GraftMove from = switch (move) {
            case MOCKINGBIRD_COUNTER -> GraftMove.MOCKINGBIRD;
            case VAULT_DIVE -> GraftMove.SCORPION_VAULT;
            case PUPPET_THROW, PUPPET_STRIKE, PUPPET_PULL -> GraftMove.HUMAN_PUPPET;
            case MAW_DRAG -> GraftMove.HUNGRY_MAW;
            case CRAWL_BITE -> GraftMove.LOCOMOTION;
            case CARAPACE_SPRING, CARAPACE_BITE -> GraftMove.CARAPACE;
            case CATAPULT_FLIGHT, CATAPULT_LAND -> GraftMove.CATAPULT;
            case KINGS_STRIKE -> GraftMove.KINGS_GAME;
            case HUNT_DRAG -> GraftMove.HUNT;
            case PERF_CLAW, PERF_FINISH -> GraftMove.PERFORMANCE;
            default -> GraftMove.NONE;
        };
        return from != GraftMove.NONE && GraftMove.of(from.slot(), stage) == from;
    }

    /** The blows whose limb is driven to one point (the rest strike an area: sweeps, spins, bursts, his own claws). */
    private static boolean reaches(GraftMove.Kind kind) {
        return switch (kind) {
            case THRUST, BITE, UPPER, SLAM, SEIZE, HOOK, FLANK -> true;
            default -> false;
        };
    }

    private static GraftRig.Input strikeInput(GraftStage stage, int n, int limb, double length, GraftMove move, double t, double[] aim,
                                               double[][] targets) {
        boolean moving = t >= 0.0;
        return new GraftRig.Input(stage, n, limb, length, new GraftRig.Body(0.0, false, 0.0, true, 0.0, 0.0, 100.0 + t),
            moving ? move : GraftMove.NONE, Math.max(0.0, t), moving ? aim : null, moving && targets != null ? targets[limb] : null, null, 0.35);
    }

    /** How far {@code at} is from the last quarter of the limb {@code points} (its striking end). */
    private static double nearEnd(double[][] points, double[] at) {
        double best = Double.MAX_VALUE;
        int from = (int)Math.floor((points.length - 1) * 0.75);
        for (int i = from; i + 1 < points.length; i++) {
            double[] a = points[i];
            double[] b = points[i + 1];
            double[] ab = {b[0] - a[0], b[1] - a[1], b[2] - a[2]};
            double len = ab[0] * ab[0] + ab[1] * ab[1] + ab[2] * ab[2];
            double u = len < 1.0E-12 ? 0.0 : Math.max(0.0, Math.min(1.0, ((at[0] - a[0]) * ab[0] + (at[1] - a[1]) * ab[1] + (at[2] - a[2]) * ab[2]) / len));
            best = Math.min(best, distance(new double[] {a[0] + ab[0] * u, a[1] + ab[1] * u, a[2] + ab[2] * u}, at));
        }
        return best;
    }

    private static void meshes() {
        for (GraftStage stage : GraftStage.values()) {
            int n = stage.limbs(1000);
            for (int limb = 0; limb < n; limb++) {
                for (GraftMove move : new GraftMove[] {GraftMove.NONE, GraftMove.firstStrike(stage), GraftMove.of(GraftMove.Slot.Z, stage)}) {
                    GraftRig.Pose pose = GraftRig.pose(new GraftRig.Input(stage, n, limb, stage.length(1000), GraftRig.Body.STILL, move, 4.0,
                        AIM, null, null, 0.3));
                    KakujaModel.Spine spine = CentipedeTail.through(pose.points(), 96, GraftRig.side(stage, n, limb), pose.dorsal());
                    for (boolean fine : new boolean[] {true, false}) {
                        GraftModel.Look look = new GraftModel.Look(stage.width(1000), fine, 1.0, limb * 7 + 3, pose.jaw(), pose.flare(), pose.stare(),
                            GraftRig.maw(stage, limb), 40.0, GraftModel.gazeToward(pose.points()[pose.points().length - 1], AIM));
                        Count count = new Count();
                        GraftModel.limb(count, stage, spine, look);
                        check(count.quads > 60 && count.finite, "a limb is a solid finite mesh: " + stage + " " + limb + " fine " + fine);
                        check(count.quads < 60_000, "a limb stays cheap to draw: " + stage + " " + count.quads);
                        if (GraftRig.maw(stage, limb) && fine) {
                            check(count.lit > 0, "the centipede's mouth glows: " + stage);
                        }
                    }
                }
            }
        }
        check(GraftRig.maw(GraftStage.CENTIPEDE, 0) && !GraftRig.maw(GraftStage.CENTIPEDE, 1), "one centipede has the mouth");
        // The many-eyed crescents stay off his back (thin where they leave it, no mass there).
        check(GraftModel.crescentWidth(0.0) < GraftModel.crescentWidth(0.4) * 0.45 && GraftModel.rinkakuWidth(0.0) < GraftModel.rinkakuWidth(0.3) * 0.45,
            "the limbs leave his back thin and swell further out");
        double[] gaze = GraftModel.gazeToward(new double[] {0.0, 1.0, 0.0}, new double[] {0.0, 1.0, 5.0});
        check(gaze != null && gaze.length >= 2, "eyes look toward what they are after");
    }

    private static void hide() {
        for (GraftSkin.Box box : GraftSkin.Box.values()) {
            for (GraftSkin.Face face : GraftSkin.Face.values()) {
                double[] r = GraftSkin.rect(box, face);
                check(r[0] >= 0.0 && r[1] >= 0.0 && r[0] + r[2] <= GraftSkin.WIDTH + 1.0E-9 && r[1] + r[3] <= GraftSkin.HEIGHT + 1.0E-9,
                    "a face's texture is on the sheet: " + box + " " + face);
            }
            int[] quads = new int[1];
            boolean[] sane = {true};
            GraftSkin.build((corners, uv, normal, alpha) -> {
                quads[0]++;
                for (int k = 0; k < 4; k++) {
                    sane[0] &= Double.isFinite(corners[k][0]) && Double.isFinite(corners[k][1]) && Double.isFinite(corners[k][2]);
                    sane[0] &= uv[k][0] >= -1.0E-9 && uv[k][0] <= 1.0 + 1.0E-9 && uv[k][1] >= -1.0E-9 && uv[k][1] <= 1.0 + 1.0E-9;
                    sane[0] &= alpha[k] >= 0.0 && alpha[k] <= 1.0;
                }
            }, box, 1.0, 2.0);
            check(quads[0] > 10 && sane[0], "the whole hide is on " + box);
            int[] none = new int[1];
            GraftSkin.build((corners, uv, normal, alpha) -> none[0]++, box, 0.0, 2.0);
            check(none[0] == 0, "no hide before the kakuja: " + box);
        }
        // The spread: from the sockets in the small of the back, the head last.
        check(GraftArmor.reveal(1.0, 1.0) > 0.99 && GraftArmor.reveal(0.0, 0.0) < 0.01 && GraftArmor.reveal(0.3, 0.0) > GraftArmor.reveal(0.3, 0.9),
            "the hide creeps from the back to the head");
        check(GraftSkin.order(GraftSkin.Box.TORSO, new double[] {0.0, 10.0, 2.0}) < GraftSkin.order(GraftSkin.Box.HEAD, new double[] {0.0, -8.0, 0.0}),
            "it reaches the lower back before the crown");
        int black = 0;
        int glow = 0;
        for (int i = 0; i < 400; i++) {
            double[] p = {-4.0 + (i % 20) * 0.4, (i / 20) * 0.6, -2.0};
            int colour = GraftSkin.paint(GraftSkin.Box.TORSO, p, new double[] {0.0, 0.0, -1.0}, false);
            int lum = ((colour >> 16) & 0xFF) + ((colour >> 8) & 0xFF) + (colour & 0xFF);
            black += lum < 120 ? 1 : 0;
            int lit = GraftSkin.paint(GraftSkin.Box.TORSO, p, new double[] {0.0, 0.0, -1.0}, true);
            glow += (lit & 0xFFFFFF) != 0 ? 1 : 0;
        }
        check(black > 200, "the hide is black (" + black + " of 400)");
        check(glow > 4 && glow < 200, "the slashes burn on his belly, not all over it (" + glow + ")");
    }

    private static void head() {
        Count none = new Count();
        GraftArmor.head(none, 0.0, 0.0, -2.0, -3.0, 0.0, 0.0, true);
        check(none.quads == 0, "no head before the kakuja");
        for (double jaw : new double[] {0.0, 0.5, 1.0}) {
            Count head = new Count();
            GraftArmor.head(head, 1.0, jaw, -2.0, -3.0, jaw, 12.0, true);
            check(head.quads > 80 && head.finite && head.lit > 0, "the head: crown, a burning star, a grin (jaw " + jaw + ")");
        }
        for (GraftArmor.Part part : GraftArmor.Part.values()) {
            Count count = new Count();
            GraftArmor.build(count, part, false, 1.0, 10.0, true);
            check(count.finite, "what stands out of the hide is finite: " + part);
        }
        Count torso = new Count();
        GraftArmor.build(torso, GraftArmor.Part.TORSO, false, 1.0, 10.0, true);
        check(torso.quads > 0, "the sockets stand out of his back");
        Count claws = new Count();
        GraftArmor.build(claws, GraftArmor.Part.RIGHT_ARM, true, 1.0, 10.0, true);
        check(claws.quads > 0, "his hands end in claws (slim arms too)");
    }

    private static void rules() {
        for (GraftStage stage : GraftStage.values()) {
            check(GraftRules.damage(1000, stage) > GraftRules.damage(0, stage) && GraftRules.damage(0, stage) > 0.0F,
                "damage grows with mastery: " + stage);
        }
        check(GraftRules.waitShare(1000, false) < GraftRules.waitShare(0, false) && GraftRules.waitShare(1000, true) < GraftRules.waitShare(1000, false)
            && GraftRules.waitShare(0, false) < 1.0, "wounds start closing sooner with mastery, far sooner in the kakuja");
        check(GraftRules.healShare(1000, false) < GraftRules.healShare(0, false) && GraftRules.healShare(1000, true) < GraftRules.healShare(1000, false)
            && GraftRules.healShare(1000, true) > 0.0, "they close faster with mastery, far faster in the kakuja, never at once");
        check(GraftRules.REGEN_COST > 0.0F && GraftRules.RECOVER_COST > 0.0F && GraftRules.SURVIVE_COST > 0.0F
            && GraftRules.SURVIVE_COOLDOWN >= 1200, "his regeneration is paid for in RC and is not unlimited");
        check(GraftRules.formRegen(1000, false) > GraftRules.formRegen(0, false) && GraftRules.formRegen(1000, true) == 0.0F,
            "the kakuhou feeds RC while out (not in the kakuja)");
        check(GraftRules.hide(true, 0.0) < 0.01 && GraftRules.hide(true, 200.0) > 0.99 && GraftRules.hide(false, 200.0) < 0.01,
            "the hide spreads in and draws back");
        check(GraftRules.colossi(true, 0.0) < 0.01 && GraftRules.colossi(true, 200.0) > 0.99 && GraftRules.colossi(false, 200.0) < 0.01,
            "the colossi unfold and fold away");
        check(GraftRules.baseRetract(true, 200.0) < 0.01 && GraftRules.baseRetract(false, 200.0) > 0.99 && GraftRules.baseRetract(true, 0.0) > 0.99,
            "the base stage sinks in for the kakuja and comes back after");
        check(GraftRules.colossi(true, GraftRules.KAKUJA_READY) > 0.5, "the colossi are mostly out by the time the kakuja can fight");
        check(GraftRules.KAKUJA_TAKEN < 1.0F && GraftRules.KAKUJA_DRAIN > 0.0F, "the kakuja shrugs off blows and burns RC");
    }

    /** His mutations: points from the milestones, each taken once, Owl DNA's revive among them (and slower than it was). */
    private static void mutations() {
        Set<String> ids = new HashSet<>();
        for (com.hex.kagune.control.graft.GraftMutation mutation : com.hex.kagune.control.graft.GraftMutation.values()) {
            check(ids.add(mutation.id()) && com.hex.kagune.control.graft.GraftMutation.fromId(mutation.id()) == mutation, "mutation ids are unique");
            check(!mutation.description().isEmpty() && mutation.description().length() < 110, "a mutation says what it does in a line: " + mutation);
        }
        check(com.hex.kagune.control.graft.GraftMutation.NOT_THAT_EASILY.displayName().contains("Get Rid of Me That Easily"),
            "You Thought You'd Get Rid of Me That Easily? is his");
        check(com.hex.kagune.control.graft.GraftMutation.earned(0) == 0 && com.hex.kagune.control.graft.GraftMutation.earned(1000)
            == com.hex.kagune.control.graft.GraftMutation.values().length, "every mutation can be had by mastery 1000");
        int mask = com.hex.kagune.control.graft.GraftMutation.RIZES_APPETITE.bit();
        check(!com.hex.kagune.control.graft.GraftMutation.canUnlock(499, 0, com.hex.kagune.control.graft.GraftMutation.NOT_THAT_EASILY)
            && com.hex.kagune.control.graft.GraftMutation.canUnlock(500, 0, com.hex.kagune.control.graft.GraftMutation.NOT_THAT_EASILY)
            && !com.hex.kagune.control.graft.GraftMutation.canUnlock(500, mask, com.hex.kagune.control.graft.GraftMutation.NOT_THAT_EASILY)
            && !com.hex.kagune.control.graft.GraftMutation.canUnlock(1000, mask, com.hex.kagune.control.graft.GraftMutation.RIZES_APPETITE),
            "a point a milestone, each mutation once");
        check(com.hex.kagune.control.pale.ReviveRules.WHOLE >= 250, "the revive takes far longer to knit him back than Owl DNA's did (128 ticks)");
    }

    /** His body through every move and in the kakuja's stance: finite, never wrenched past what a body does. */
    private static void body() {
        for (GraftStage stage : GraftStage.values()) {
            int n = stage.limbs(1000);
            for (GraftMove move : GraftMove.values()) {
                if (move != GraftMove.NONE && move.stage() != stage && !(stage == GraftStage.KAKUJA ? move.stage() == stage : GraftMove.of(move.slot(), stage) == move)
                    && !followed(move, stage)) {
                    continue;
                }
                for (double age = 0.0; age <= move.ticks() + 6; age += 0.5) {
                    com.hex.kagune.control.client.animation.RigPose p = new com.hex.kagune.control.client.animation.RigPose();
                    com.hex.kagune.control.client.animation.GraftPose.body(p, stage, n, move, age, stage == GraftStage.KAKUJA ? 1.0 : 0.0, age * 3.7);
                    float[] channels = {p.lean, p.twist, p.drop, p.lunge, p.headX, p.headY, p.headZ, p.rightArmX, p.rightArmY, p.rightArmZ, p.rightElbow,
                        p.leftArmX, p.leftArmY, p.leftArmZ, p.leftElbow};
                    boolean ok = true;
                    for (float c : channels) {
                        ok &= Float.isFinite(c) && Math.abs(c) < 8.0F;
                    }
                    check(ok && p.lean < 1.3F && p.lean > -0.8F && p.rightArmX > -4.2F && p.leftArmX > -4.2F, "a sane body: " + stage + " " + move + " at " + age);
                }
            }
        }
        com.hex.kagune.control.client.animation.RigPose rake = new com.hex.kagune.control.client.animation.RigPose();
        com.hex.kagune.control.client.animation.GraftPose.body(rake, GraftStage.KAKUJA, 2, GraftMove.CLAW_1, 1.0, 1.0, 0.0);
        check(rake.rightArmX < -1.0F && rake.armHold > 0.5F, "the kakuja's rake winds his right claw up over his shoulder");
        com.hex.kagune.control.client.animation.RigPose still = new com.hex.kagune.control.client.animation.RigPose();
        com.hex.kagune.control.client.animation.GraftPose.body(still, GraftStage.KAKUJA, 2, GraftMove.NONE, 0.0, 1.0, 20.0);
        check(still.lean > 0.15F && still.drop > 0.5F, "in the kakuja he stands hunched, a predator");
        // 7.0.0: the hide folds at the elbow and knee with the limb under it (drawn straight, the bare forearm showed through).
        check(Math.abs(still.rightElbow) > 0.2F && Math.abs(still.rightKnee) > 0.1F, "the stance bends his elbows and knees");
        Object[][] limbs = {{GraftSkin.Box.RIGHT_ARM, "rightArm"}, {GraftSkin.Box.LEFT_ARM, "leftArm"}, {GraftSkin.Box.RIGHT_LEG, "rightLeg"},
            {GraftSkin.Box.LEFT_LEG, "leftLeg"}};
        for (Object[] limb : limbs) {
            GraftSkin.Box box = (GraftSkin.Box)limb[0];
            String name = (String)limb[1];
            double[] worst = new double[2];
            GraftSkin.build((corners, uv, normal, alpha) -> {
                for (double[] c : corners) {
                    double[] under = {Math.max(box.x0, Math.min(box.x0 + box.w, c[0])), Math.max(box.y0, Math.min(box.y0 + box.h, c[1])),
                        Math.max(box.z0, Math.min(box.z0 + box.d, c[2]))};
                    double[] limbAt = com.hex.kagune.control.client.animation.BodyFrame.bent(still, name, under);
                    worst[0] = Math.max(worst[0], distance(com.hex.kagune.control.client.animation.BodyFrame.bent(still, name, c), limbAt));
                    worst[1] = Math.max(worst[1], distance(c, limbAt));
                }
            }, box, 1.0, 2.0);
            check(worst[0] < 1.6, "the hide stays on the bent " + name + " (" + worst[0] + " px off it)");
            check(worst[1] > worst[0] + 0.5, "the bend is real: drawn straight, the hide would leave the " + name + " (" + worst[1] + ")");
        }
    }

    /**
     * 7.0.2, the user's rework: the two tails' four. Bleeding refreshes and never stacks; the dive always comes down; the vault
     * throws him up and he falls again; the execution circles its prey cut after cut, each tail back and forth with no gap; the
     * counter hooks before it strikes from behind; the puppet's three follow-ups hold on to it; the stance poises the tails over
     * his shoulders; the hook wraps round its prey's back.
     */
    private static void tails() {
        // Bleeding: refreshed, not stacked.
        int hit = GraftRules.bleedStacks(0.34F);
        long[] bleed = GraftRules.refreshBleed(0, 0L, false, 100L, hit);
        long fresh = com.hex.kagune.control.ukaku.UkakuRules.bleedDuration(hit);
        check(bleed[0] == hit && bleed[1] == 100L + fresh, "a first cut bleeds its own strength for its own length");
        for (long now = 102L; now < 140L; now += 2L) {
            bleed = GraftRules.refreshBleed((int)bleed[0], bleed[1], true, now, hit);
            check(bleed[0] == hit && bleed[1] == now + fresh, "cut after cut refreshes the bleeding, never stacking it (" + now + ")");
        }
        long[] strong = GraftRules.refreshBleed(5, 500L, true, 200L, 2);
        check(strong[0] == 5 && strong[1] == 500L, "a light cut neither weakens nor shortens a deeper wound");
        long[] stale = GraftRules.refreshBleed(5, 150L, false, 200L, 2);
        check(stale[0] == 2, "a wound that has stopped bleeding starts again from the new cut");
        check(GraftRules.bleedStacks(0.2F) <= GraftRules.bleedStacks(1.0F) && GraftRules.bleedStacks(1.0F) <= GraftRules.bleedStacks(2.2F)
            && GraftRules.bleedStacks(0.0F) >= 1 && GraftRules.bleedStacks(3.0F) <= com.hex.kagune.control.ukaku.UkakuRules.MAX_BLEED_STACKS,
            "heavier blows bleed harder, within the bleeding's own scale");
        // The dive.
        double[][] looks = {{0.0, 0.0, 1.0}, {0.6, 0.5, 0.6}, {0.0, -1.0, 0.0}, {0.0, 1.0, 0.0}, {-0.3, -0.2, 0.9}, {0.0, -0.8, 0.6}};
        for (double[] look : looks) {
            double[] d = GraftRules.diveDirection(look[0], look[1], look[2]);
            check(Math.abs(Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]) - 1.0) < 1.0E-9 && d[1] <= -GraftRules.DIVE_DOWN + 1.0E-9,
                "the dive always comes down: " + java.util.Arrays.toString(look));
            double flat = Math.hypot(look[0], look[2]);
            if (flat > 1.0E-6 && Math.hypot(d[0], d[2]) > 1.0E-6) {
                check((d[0] * look[0] + d[2] * look[2]) / (flat * Math.hypot(d[0], d[2])) > 0.999, "the dive keeps his heading");
            }
        }
        check(GraftRules.diveDirection(0.0, -0.8, 0.6)[1] < -0.79, "looking steeply down, he dives steeply");
        // The vault.
        double peak = 0.0;
        for (double t = 0.0; t <= 40.0; t += 0.5) {
            peak = Math.max(peak, GraftRules.vaultRise(t));
        }
        check(peak > 4.0 && peak < 9.0, "the vault throws him high (" + String.format("%.1f", peak) + " blocks)");
        check(GraftRules.vaultRise(3.0) > GraftRules.vaultRise(1.0) && GraftRules.vaultRise(30.0) < peak, "he goes up, then comes down");
        // The execution.
        check(GraftRules.executionTurn(0.0) == 0.0 && GraftRules.executionTurn(GraftRules.EXECUTION_RUSH) == 0.0
            && Math.abs(GraftRules.executionTurn(GraftRules.EXECUTION_CIRCLE_END) - GraftRules.EXECUTION_TURN) < 1.0E-9, "the circle starts after the rush");
        for (double a = 1.0; a < 30.0; a += 1.0) {
            check(GraftRules.executionTurn(a) >= GraftRules.executionTurn(a - 1.0), "the circle only goes on round");
        }
        for (int n = 1; n <= 2; n++) {
            List<GraftMove.Beat> beats = GraftMove.OWLS_EXECUTION.beats(n);
            int cuts = 0;
            for (int limb = 0; limb < n; limb++) {
                GraftMove.Beat last = null;
                for (GraftMove.Beat beat : beats) {
                    if (beat.limb() != limb) {
                        continue;
                    }
                    if (last != null) {
                        check(beat.start() == last.end(), "the execution's next blow takes over as the last ends (tail " + limb + " of " + n + ")");
                        if (beat.kind() == GraftMove.Kind.SWEEP) {
                            check(Math.signum(beat.to() - beat.from()) != Math.signum(last.to() - last.from()) && Math.abs(beat.height() - last.height()) > 0.3,
                                "each tail cuts back the other way, at a new height");
                            check(Math.abs(beat.from() - last.to()) < 20.0, "a cut starts where the last left the tail");
                        }
                    }
                    if (beat.kind() == GraftMove.Kind.SWEEP) {
                        cuts++;
                        check(beat.has(GraftMove.BLEED), "the execution's cuts bleed");
                    } else {
                        check(beat.kind() == GraftMove.Kind.THRUST && beat.land() == 27 && beat.has(GraftMove.BREAK) && beat.has(GraftMove.BLEED),
                            "it ends with the tails driven through it together, breaking through");
                    }
                    last = beat;
                }
            }
            check(cuts == (n == 1 ? 4 : 8), "eight cuts with both tails, every other one with one (" + cuts + ")");
        }
        // The counter.
        List<GraftMove.Beat> counter = GraftMove.MOCKINGBIRD_COUNTER.beats(2);
        GraftMove.Beat hook = counter.stream().filter(b -> b.kind() == GraftMove.Kind.HOOK).findFirst().orElse(null);
        GraftMove.Beat flank = counter.stream().filter(b -> b.kind() == GraftMove.Kind.FLANK).findFirst().orElse(null);
        check(hook != null && flank != null && hook.limb() != flank.limb() && hook.land() < flank.land() && flank.land() <= hook.end()
            && flank.power() > hook.power() && hook.has(GraftMove.STUN) && flank.has(GraftMove.STUN),
            "the counter hooks it with one tail and, while it is held, runs it through with the other");
        check(GraftMove.MOCKINGBIRD.beats(2).get(0).kind() == GraftMove.Kind.LURK && GraftMove.MOCKINGBIRD.beats(2).get(0).end() >= GraftMove.MOCKINGBIRD.ticks()
            && GraftRules.STANCE_WHIFF_COOLDOWN < GraftMove.MOCKINGBIRD.cooldown(), "the stance holds its whole length, and costs less wait if nothing came");
        check(GraftMove.nextStrike(GraftMove.LASH_3, GraftStage.TRANSPLANT) == GraftMove.LASH_4, "the counter flows on into the string's fourth blow");
        // The puppet.
        for (GraftMove follow : new GraftMove[] {GraftMove.PUPPET_THROW, GraftMove.PUPPET_STRIKE, GraftMove.PUPPET_PULL, GraftMove.MOCKINGBIRD_COUNTER,
            GraftMove.VAULT_DIVE}) {
            check(follow.followUp() && follow.slot() == GraftMove.Slot.NONE && !GraftMove.track().contains(follow), "a follow-up has no key of its own: " + follow);
        }
        for (GraftMove follow : new GraftMove[] {GraftMove.PUPPET_STRIKE, GraftMove.PUPPET_PULL}) {
            List<GraftMove.Beat> beats = follow.beats(2);
            check(beats.get(0).limb() == 0 && beats.get(0).has(GraftMove.CARRY) && beats.stream().anyMatch(b -> b.limb() == 1 && b.power() >= 1.0F),
                "the puppet stays caught on the first tail while the second goes through it: " + follow);
        }
        double[] puppet = GraftRules.heldPoint(GraftMove.HUMAN_PUPPET, GraftMove.Kind.SEIZE, 10.0);
        check(puppet[2] > 2.5 && puppet[2] < 4.5 && Math.abs(puppet[0]) < 1.0, "the puppet is dragged in and held out at the tail's length before him");
        check(puppet[2] - 0.3 > GraftRules.PULL_CLOSE + 1.0, "held out there, hauling himself in to it is worth doing");
        check(GraftMove.PUPPET_STRIKE.beats(2).stream().anyMatch(b -> b.limb() == 1 && b.reach() >= Math.hypot(puppet[2], puppet[1] - GraftRig.CHEST)),
            "the second tail reaches the puppet where it is held");
        check(GraftRules.heldPoint(GraftMove.PUPPET_THROW, GraftMove.Kind.WHIP, 4.0)[1] > puppet[1] + 1.5, "the throw hoists it high first");
        check(GraftMove.OWLS_EXECUTION.follows() && GraftMove.MOCKINGBIRD_COUNTER.follows() && !GraftMove.VAULT_DIVE.follows(),
            "the execution and the counter follow their prey; the dive goes where he looks");
        // The stance and the hook on the rig.
        GraftStage stage = GraftStage.TRANSPLANT;
        double length = stage.length(200);
        for (int limb = 0; limb < 2; limb++) {
            GraftRig.Pose stance = GraftRig.pose(new GraftRig.Input(stage, 2, limb, length, new GraftRig.Body(0.0, false, 0.0, true, 0.0, 0.0, 50.0),
                GraftMove.MOCKINGBIRD, 15.0, null, null, null, 0.35));
            double[][] p = stance.points();
            double lowest = Double.MAX_VALUE;
            for (int i = 1; i < p.length; i++) {
                lowest = Math.min(lowest, p[i][1]);
            }
            double[] sting = p[p.length - 1];
            check(lowest < 0.5 && sting[1] > 0.7 && sting[1] < 1.6 && sting[2] > 0.0 && sting[0] * (limb == 0 ? 1 : -1) > 0.6,
                "in the stance tail " + limb + " runs low behind him and its sting is poised at his side, at his waist, pointing ahead");
            GraftRig.Pose rest = GraftRig.pose(new GraftRig.Input(stage, 2, limb, length, new GraftRig.Body(0.0, false, 0.0, true, 0.0, 0.0, 50.0),
                GraftMove.NONE, 0.0, null, null, null, 0.35));
            check(distance(rest.points()[rest.points().length - 1], sting) > 1.0, "the stance is not the rest pose: tail " + limb);
        }
        double[] prey = {0.4, 1.0, 3.6};
        GraftRig.Pose hooked = GraftRig.pose(new GraftRig.Input(stage, 2, 0, length, GraftRig.Body.STILL, GraftMove.MOCKINGBIRD_COUNTER, 8.0, prey,
            null, prey, 0.4));
        double behind = -Double.MAX_VALUE;
        double near = Double.MAX_VALUE;
        for (double[] point : hooked.points()) {
            behind = Math.max(behind, point[2] - prey[2]);
            near = Math.min(near, distance(point, prey));
        }
        check(behind > 0.25 && near < 0.6, "the hook wraps round the back of what it holds");
    }

    /**
     * 7.0.2, the user's rework: the centipedes' four. The crawl and the drag are jobs (they run alongside); the crawl's strides
     * alternate sides and each hauls him a couple of blocks; the mouth goes out further every tick, round its bend, and reaches its
     * prey; dragging, the mouth stays on what it holds whatever the string does, while the string still has the centipedes; the
     * ambush feints with the centipedes and breaks through with the mouth; the carapace guards, springs, and the mouth bites.
     */
    private static void centipedes() {
        GraftStage stage = GraftStage.CENTIPEDE;
        int n = stage.limbs(400);
        double length = stage.length(400);
        check(GraftMove.LOCOMOTION.job() && GraftMove.MAW_DRAG.job() && !GraftMove.HUNGRY_MAW.job() && !GraftMove.AMBUSH.job(),
            "the crawl and the drag run alongside; the rest are moves");
        List<GraftMove.Beat> strides = GraftMove.LOCOMOTION.beats(n);
        for (int i = 1; i < strides.size(); i++) {
            check(strides.get(i).limb() != strides.get(i - 1).limb() && strides.get(i).start() - strides.get(i - 1).start() == GraftRules.CRAWL_STRIDE
                && strides.get(i).limb() != 0, "the side centipedes stride in turn, the mouth free");
        }
        double stride = GraftRules.crawled(GraftRules.CRAWL_STRIDE);
        check(stride > 1.5 && stride < 3.0 && GraftRules.crawled(4) < stride, "each stride hauls him on (" + String.format("%.2f", stride) + " blocks)");
        check(GraftRules.mawReach(30, length) - 2.6 * length / 5.0 > GraftRules.MAW_RANGE * 1.3 && GraftRules.mawReach(10, length) > GraftRules.mawReach(5, length),
            "the mouth goes out further every tick, far enough to bend round to anything in its range");
        // The chase on the rig: out along its route, its head ever nearer its prey, at it once it has gone far enough.
        double[] prey = {1.5, 1.0, 9.0};
        double[] bend = {0.75, 5.0, 4.5};
        double last = Double.MAX_VALUE;
        boolean reached = false;
        for (int age = 2; age <= 30; age += 2) {
            GraftRig.Pose pose = GraftRig.pose(new GraftRig.Input(stage, n, 0, length, GraftRig.Body.STILL, GraftMove.HUNGRY_MAW, age, bend, prey,
                null, 0.35));
            double[] head = pose.points()[pose.points().length - 1];
            double gap = distance(head, prey);
            check(gap <= last + 1.0E-6, "the mouth's head only closes on its prey (" + age + ")");
            last = gap;
            reached |= gap < 1.0;
        }
        check(reached, "the mouth reaches its prey round the bend");
        // Dragging: the mouth stays on what it holds through the string's bite; a centipede still strikes for the string.
        double[] held = {1.7, 0.45, 0.5};
        GraftRig.Input dragging = new GraftRig.Input(stage, n, 0, length, GraftRig.Body.STILL, GraftMove.RIP_3, 5.0, new double[] {0.0, 1.2, 5.0},
            null, null, 0.35, GraftMove.MAW_DRAG, 30.0, held);
        GraftRig.Pose mouth = GraftRig.pose(dragging);
        check(distance(mouth.points()[mouth.points().length - 1], held) < 0.8 && mouth.jaw() < 0.3, "dragging, the mouth stays clamped on its catch");
        GraftRig.Input crawling = new GraftRig.Input(stage, n, 1, length, GraftRig.Body.STILL, GraftMove.RIP_1, 4.0, new double[] {0.0, 1.2, 5.0},
            null, null, 0.35, GraftMove.LOCOMOTION, 3.0, null);
        GraftRig.Pose sweeping = GraftRig.pose(crawling);
        GraftRig.Pose striding = GraftRig.pose(new GraftRig.Input(stage, n, 1, length, GraftRig.Body.STILL, GraftMove.NONE, 0.0, null, null, null,
            0.35, GraftMove.LOCOMOTION, 3.0, null));
        check(distance(sweeping.points()[sweeping.points().length - 1], striding.points()[striding.points().length - 1]) > 0.5,
            "crawling, a centipede still answers the string (its strike takes it from the stride)");
        double[] planted = striding.points()[striding.points().length - 1];
        GraftRig.Pose later = GraftRig.pose(new GraftRig.Input(stage, n, 1, length, GraftRig.Body.STILL, GraftMove.NONE, 0.0, null, null, null,
            0.35, GraftMove.LOCOMOTION, 10.0, null));
        double[] slid = later.points()[later.points().length - 1];
        check(planted[1] < 0.3 && slid[1] < 0.3 && slid[2] < planted[2] - 1.0, "a stride's point stays in the ground as he is hauled past it");
        // The ambush and the carapace.
        List<GraftMove.Beat> ambush = GraftMove.AMBUSH.beats(n);
        GraftMove.Beat snap = ambush.stream().filter(b -> b.limb() == 0).findFirst().orElse(null);
        check(snap != null && snap.kind() == GraftMove.Kind.UPPER && snap.has(GraftMove.BREAK) && snap.has(GraftMove.STUN)
            && ambush.stream().filter(b -> b.limb() != 0).allMatch(b -> b.land() < snap.land() && b.power() < snap.power()),
            "the centipedes' feint lands first and light; the mouth snaps up through the guard after");
        check(GraftMove.CARAPACE.guards() && GraftMove.CARAPACE.beats(n).stream().noneMatch(b -> b.limb() == 0),
            "the carapace guards with the centipedes, the mouth left free");
        check(GraftMove.CARAPACE_SPRING.beats(n).stream().allMatch(b -> b.kind() == GraftMove.Kind.BURST && b.has(GraftMove.HEAVY))
            && GraftMove.CARAPACE_BITE.beats(n).get(0).limb() == 0, "struck, the coils spring out stunning hard; then the mouth bites");
    }

    /**
     * 7.0.2, the user's rework: the four thorns' four. Crucifixion: the upper pair catch and hold it up from either side while the
     * lower pair run it through in turn from new angles (bleeding), each thorn's thrusts one after another without a gap, then it
     * is hurled. The breach: each thrust smashes or runs through, the four in turn. The dragnet: the lower pair far out and swept
     * in, dragging, the upper pair coming down together once they have. The catapult: flung far higher than the vault, a flight
     * that splays the four, four slams round him on landing in turn.
     */
    private static void thorns() {
        int n = GraftStage.THORNS.limbs(600);
        List<GraftMove.Beat> cross = GraftMove.CRUCIFIXION.beats(n);
        check(cross.stream().filter(b -> b.kind() == GraftMove.Kind.SEIZE).map(GraftMove.Beat::limb).distinct().count() == 2
            && cross.stream().filter(b -> b.kind() == GraftMove.Kind.SEIZE).allMatch(b -> b.limb() < 2), "the upper pair catch it from either side");
        int stabs = 0;
        int throwAt = cross.stream().filter(b -> b.kind() == GraftMove.Kind.WHIP).mapToInt(GraftMove.Beat::land).min().orElse(-1);
        for (int limb = 2; limb < 4; limb++) {
            GraftMove.Beat last = null;
            for (GraftMove.Beat beat : cross) {
                if (beat.limb() != limb) {
                    continue;
                }
                stabs++;
                check(beat.kind() == GraftMove.Kind.THRUST && beat.has(GraftMove.BLEED) && beat.has(GraftMove.PIERCE) && beat.land() < throwAt,
                    "the lower pair run it through, bleeding, before it is thrown");
                if (last != null) {
                    check(beat.start() == last.end() && beat.from() != last.from(), "each lower thorn's thrusts follow on, each from a new angle");
                }
                last = beat;
            }
        }
        check(stabs >= 6 && throwAt == GraftRules.CRUCIFY_THROW, "a rapid run of thrusts (" + stabs + "), then the throw at its tick");
        double[] up = GraftRules.heldPoint(GraftMove.CRUCIFIXION, GraftMove.Kind.SEIZE, 20.0);
        check(up[1] - 1.0 > 1.0, "held up: its feet off the ground");
        List<GraftMove.Beat> breach = GraftMove.BREACH.beats(n);
        check(GraftMove.BREACH.travels() && breach.stream().allMatch(b -> b.has(GraftMove.CRATER) && b.has(GraftMove.BLEED) && b.has(GraftMove.STUN))
            && breach.stream().map(GraftMove.Beat::limb).distinct().count() == 4, "the breach: all four in turn, smashing or running through");
        List<GraftMove.Beat> net = GraftMove.DRAGNET.beats(n);
        List<GraftMove.Beat> sweeps = net.stream().filter(b -> b.has(GraftMove.GATHER)).toList();
        List<GraftMove.Beat> slams = net.stream().filter(b -> b.kind() == GraftMove.Kind.SLAM).toList();
        check(sweeps.size() == 2 && sweeps.stream().allMatch(b -> b.limb() >= 2 && b.reach() >= 8.0 && Math.abs(b.from()) > 90.0
            && Math.abs(b.to()) < 20.0), "the lower pair go far out to either side and sweep in");
        check(slams.size() == 2 && slams.stream().allMatch(b -> b.limb() < 2 && b.land() == slams.get(0).land()
            && b.land() > sweeps.get(0).end() && b.has(GraftMove.HEAVY)), "the upper pair come down together once the net has closed");
        check(GraftRules.DRAGNET_HOLD >= slams.get(0).land(), "what it caught is held in the middle until the slam");
        double peak = 0.0;
        for (double t = 0.0; t < 40.0; t += 0.5) {
            peak = Math.max(peak, GraftRules.catapultRise(t));
        }
        check(peak > 12.0 && peak > 2.0 * GraftRules.vaultRise(11.0), "the catapult flings him enormously high (" + String.format("%.1f", peak) + ")");
        check(GraftMove.CATAPULT_FLIGHT.job(), "the flight runs alongside (he can still strike in the air)");
        List<GraftMove.Beat> landing = GraftMove.CATAPULT_LAND.beats(n);
        for (int i = 1; i < landing.size(); i++) {
            check(landing.get(i).land() > landing.get(i - 1).land() && landing.get(i).limb() != landing.get(i - 1).limb(),
                "on landing the four go into the ground in turn");
        }
        check(landing.stream().mapToDouble(GraftMove.Beat::from).max().orElse(0.0) > 90.0
            && landing.stream().mapToDouble(GraftMove.Beat::from).min().orElse(0.0) < -90.0, "all round him");
        for (int limb = 0; limb < n; limb++) {
            GraftRig.Pose falling = GraftRig.pose(new GraftRig.Input(GraftStage.THORNS, n, limb, GraftStage.THORNS.length(600),
                new GraftRig.Body(0.0, false, -0.8, false, 0.0, 0.0, 30.0), GraftMove.NONE, 0.0, null, null, null, 0.35, GraftMove.CATAPULT_FLIGHT, 30.0,
                null));
            double[] tip = falling.points()[falling.points().length - 1];
            check(tip[1] < 0.0 && Math.hypot(tip[0], tip[2]) > 2.0, "falling, thorn " + limb + " splays out and reaches down for the ground");
        }
    }

    /**
     * 7.0.2, the user's rework: the mass of eyes' four. The storm: every crescent on its own path (the odd ones the other way round),
     * wandering in and out and up and down, drawn where the server cuts; the bloom's spikes burst in waves and settle; the curtain
     * stands as a wall across his front, then lies flat on the ground ahead, its ends curled round; the King's Game runs alongside,
     * at its prey from every side (straight in, round behind, from under, from above), always out of his back; his sword's blow bleeds.
     */
    private static void eyes() {
        GraftStage stage = GraftStage.DRAGON;
        int n = stage.limbs(800);
        double length = stage.length(800);
        for (int limb = 0; limb < n; limb++) {
            double turned = GraftRules.stormAngle(limb, 40.0) - GraftRules.stormAngle(limb, 20.0);
            check(Math.signum(turned) == (limb % 2 == 0 ? 1.0 : -1.0), "storm crescent " + limb + " wheels its own way");
            double low = Double.MAX_VALUE;
            double high = 0.0;
            for (double age = 8.0; age < 56.0; age += 1.0) {
                double r = GraftRules.stormRadius(limb, age, length);
                low = Math.min(low, r);
                high = Math.max(high, r);
                double h = GraftRules.stormHeight(limb, age);
                check(h > 0.1 && h < 3.0, "a crescent keeps off the ground and within reach of a body");
            }
            check(high - low > 1.0 && low > 1.5, "each crescent wanders in and out (" + String.format("%.1f..%.1f", low, high) + ")");
            GraftRig.Pose storm = GraftRig.pose(new GraftRig.Input(stage, n, limb, length, GraftRig.Body.STILL, GraftMove.STORM, 30.0,
                new double[] {0.0, 1.3, 6.0}, null, null, 0.35));
            double[] tip = storm.points()[storm.points().length - 1];
            double bearing = Math.atan2(tip[0], tip[2]);
            double want = GraftRules.stormAngle(limb, 30.0);
            double off = Math.abs(Math.atan2(Math.sin(bearing - want), Math.cos(bearing - want)));
            check(off < 0.35, "storm crescent " + limb + " is drawn where the server cuts with it (" + String.format("%.2f", off) + " rad off)");
        }
        for (int wave : GraftRules.BLOOM_WAVES) {
            check(GraftRules.bloomFlare(wave) > 3.0 && GraftRules.bloomFlare(wave) > GraftRules.bloomFlare(wave + 4) + 0.8,
                "the bloom's spikes burst at wave " + wave + " and ease after");
        }
        check(Math.abs(GraftRules.bloomFlare(44.0) - 1.0) < 1.0E-9 && Math.abs(GraftRules.bloomFlare(2.0) - 1.0) < 1.0E-9,
            "the spikes are gone once it draws in again");
        GraftMove.Beat curtain = GraftMove.CURTAIN.beats(n).get(0);
        double[] aim = {0.0, 1.3, 6.0};
        GraftRig.Pose standing = GraftRig.pose(new GraftRig.Input(stage, n, 0, length, GraftRig.Body.STILL, GraftMove.CURTAIN, curtain.land() - 5.0, aim,
            null, null, 0.35));
        GraftRig.Pose lying = GraftRig.pose(new GraftRig.Input(stage, n, 0, length, GraftRig.Body.STILL, GraftMove.CURTAIN, curtain.land() + 8.0, aim,
            null, null, 0.35));
        double top = 0.0;
        double highest = 0.0;
        double widest = 0.0;
        double furthest = 0.0;
        for (double[] p : standing.points()) {
            top = Math.max(top, p[1]);
        }
        double[][] laid = lying.points();
        for (int i = laid.length / 3; i < laid.length; i++) {
            highest = Math.max(highest, laid[i][1]);
            widest = Math.max(widest, Math.abs(laid[i][0]));
            furthest = Math.max(furthest, laid[i][2]);
        }
        check(top > 4.5, "the curtain stands as a wall higher than him (" + String.format("%.1f", top) + ")");
        check(highest < 1.5 && widest > 3.0 && furthest > 5.0, "fallen, it lies on the ground across his front and far ahead");
        check(GraftMove.KINGS_GAME.job(), "the King's Game runs alongside: he goes where he likes");
        java.util.Set<GraftMove.Kind> ways = new java.util.HashSet<>();
        for (GraftMove.Beat beat : GraftMove.KINGS_GAME.beats(n)) {
            ways.add(beat.kind());
            check(beat.land() <= GraftMove.KINGS_GAME.ticks() && beat.reach() >= 12.0, "every crescent's blow reaches its prey in time");
        }
        check(ways.containsAll(java.util.List.of(GraftMove.Kind.THRUST, GraftMove.Kind.FLANK, GraftMove.Kind.UPPER, GraftMove.Kind.SLAM)),
            "the King's Game comes at its prey from every side");
        double[] prey = {3.0, 1.0, 10.0};
        for (int limb = 0; limb < n; limb++) {
            GraftRig.Pose playing = GraftRig.pose(new GraftRig.Input(stage, n, limb, length, GraftRig.Body.STILL, GraftMove.NONE, 0.0, null, null, null,
                0.35, GraftMove.KINGS_GAME, 34.0, prey));
            double[] root = GraftRig.root(stage, n, limb, false);
            check(distance(playing.points()[0], root) < 1.0E-6, "the mass stays joined to him: crescent " + limb);
        }
        check(GraftMove.KINGS_STRIKE.beats(n).get(0).has(GraftMove.BLEED), "the blow with his sword bleeds");
    }

    /**
     * 7.0.2, the user's rework: the final kakuja's four. Checkmate: the frontal blow first, the hook from behind after, his charge,
     * then claws, jaws, a colossus and his last blow while it is still hooked. The hunt: a bite every few ticks while the other
     * colossus strides; caught, dragged (the colossus holding it), thrown. The recovery: a job of coils round him, costly, halving
     * blows. The performance: a job, the colossi taking turns from every side, every blow bleeding; its end: caught, hauled in,
     * run through, his last blow.
     */
    private static void kakuja() {
        int n = GraftStage.KAKUJA.limbs(1000);
        List<GraftMove.Beat> mate = GraftMove.CHECKMATE.beats(n);
        GraftMove.Beat front = mate.get(0);
        GraftMove.Beat hook = mate.stream().filter(b -> b.kind() == GraftMove.Kind.HOOK).findFirst().orElse(null);
        check(front.limb() == 0 && hook != null && hook.limb() == 1 && front.land() < hook.land() && hook.has(GraftMove.BREAK),
            "checkmate: one colossus at its front first, the other round behind it after");
        List<GraftMove.Beat> combo = mate.stream().filter(b -> b.start() > hook.land()).toList();
        check(combo.size() >= 4 && combo.stream().allMatch(b -> b.land() < hook.end()) && combo.get(0).start() >= GraftRules.CHECKMATE_CHARGE_TO - 1
            && combo.stream().anyMatch(b -> b.limb() == GraftMove.ARM && b.kind() == GraftMove.Kind.BITE)
            && combo.stream().anyMatch(b -> b.limb() == 0 && b.has(GraftMove.HEAVY)),
            "he charges in, then claws, jaws, a colossus and a last blow while it is still hooked");
        List<GraftMove.Beat> hunt = GraftMove.HUNT.beats(n);
        check(GraftMove.HUNT.travels() && hunt.stream().filter(b -> b.limb() == 0).allMatch(b -> b.kind() == GraftMove.Kind.BITE && b.has(GraftMove.BLEED))
            && hunt.stream().filter(b -> b.limb() == 1).allMatch(b -> b.kind() == GraftMove.Kind.CRAWL), "the hunt: one bites as he closes, the other strides him on");
        check(GraftMove.HUNT_DRAG.beats(n).get(0).has(GraftMove.CARRY) && GraftRules.HUNT_THROW < GraftMove.HUNT_DRAG.ticks(),
            "caught, it is held dragging and thrown before the drag ends");
        check(GraftMove.RECOVERY.job() && GraftMove.RECOVERY.beats(n).get(0).kind() == GraftMove.Kind.CURL
            && GraftMove.RECOVERY.beats(n).get(0).end() >= GraftMove.RECOVERY.ticks() && GraftRules.RECOVERY_COST >= 1.0F
            && GraftRules.RECOVERY_TAKEN < 1.0F && GraftRules.RECOVERY_TAKEN > 0.0F, "the recovery: coils round him all through, costly, taking half of a blow");
        check(GraftMove.PERFORMANCE.job(), "the performance runs alongside: he moves and claws as he likes");
        java.util.Set<GraftMove.Kind> ways = new java.util.HashSet<>();
        GraftMove.Beat last = null;
        for (GraftMove.Beat beat : GraftMove.PERFORMANCE.beats(n)) {
            ways.add(beat.kind());
            check(beat.has(GraftMove.BLEED) && beat.land() <= GraftMove.PERFORMANCE.ticks(), "every colossus blow bleeds, within its time");
            if (last != null) {
                check(beat.limb() != last.limb(), "the colossi take turns");
            }
            last = beat;
        }
        check(ways.size() >= 4, "the colossi come from every side (" + ways + ")");
        List<GraftMove.Beat> finish = GraftMove.PERF_FINISH.beats(n);
        GraftMove.Beat catchIt = finish.get(0);
        check(catchIt.kind() == GraftMove.Kind.SEIZE && catchIt.limb() == 0 && finish.stream().anyMatch(b -> b.limb() == 1 && b.power() >= 2.0F
            && b.land() > catchIt.land()) && finish.stream().anyMatch(b -> b.limb() == GraftMove.ARM && b.has(GraftMove.KNOCK_UP)),
            "its end: caught, the other colossus through it, his last blow throwing it up");
        double[] in = GraftRules.heldPoint(GraftMove.PERF_FINISH, GraftMove.Kind.SEIZE, 6.0);
        check(Math.hypot(in[0], in[2]) < 2.5, "the caught prey is hauled right in to him");
    }

    /**
     * 7.0.2 (PvP): against another player, at the top mastery of its stage and before his armor, no ability (with what follows on
     * from it) does more than about two health bars at once, and no job more than a few hearts a second; a player can't be kept
     * stunned past a couple of seconds without a break, and then can't be stunned again for as long.
     */
    private static void pvp() {
        check(GraftRules.PVP_SHARE > 0.2F && GraftRules.PVP_SHARE < 1.0F, "a player takes a share of a blow, not all of it");
        int[] top = {250, 450, 700, 900, 1000};
        for (GraftStage stage : GraftStage.values()) {
            int mastery = top[stage.ordinal()];
            float blow = GraftRules.damage(mastery, stage) * GraftRules.PVP_SHARE;
            int n = stage.limbs(mastery);
            for (GraftMove move : GraftMove.values()) {
                if (move == GraftMove.NONE || move.stage() != stage || move.combo() || move == GraftMove.STEP || move == GraftMove.ACCORD) {
                    continue;
                }
                double total = 0.0;
                for (GraftMove.Beat beat : move.beats(n)) {
                    total += beat.power();
                }
                if (move.job()) {
                    double perSecond = total * blow * 20.0 / move.ticks();
                    check(perSecond <= 8.0, move + " running alongside does " + String.format("%.1f", perSecond) + " a second to a player (at most 8)");
                } else {
                    check(total * blow <= 36.0, move + " does " + String.format("%.1f", total * blow) + " to a player at once (at most 36, before armor)");
                }
            }
        }
        check(com.hex.kagune.control.pale.StunRules.PLAYER_CHAIN <= 60 && com.hex.kagune.control.pale.StunRules.PLAYER_CHAIN >= GraftRules.HEAVY_STUN
            && com.hex.kagune.control.pale.StunRules.PLAYER_IMMUNE >= 20, "a player breaks a long stun chain, then can't be stunned again for a moment");
        check(!com.hex.kagune.control.pale.StunRules.breaksFree(100, 100 + com.hex.kagune.control.pale.StunRules.PLAYER_CHAIN - 1)
            && com.hex.kagune.control.pale.StunRules.breaksFree(100, 100 + com.hex.kagune.control.pale.StunRules.PLAYER_CHAIN), "he breaks free on its tick");
    }

    private static void panel() {
        Map<AbilityGuide.Key, String> keys = new EnumMap<>(AbilityGuide.Key.class);
        for (AbilityGuide.Key key : AbilityGuide.Key.values()) {
            keys.put(key, key.name().substring(0, 1));
        }
        for (GraftStage stage : GraftStage.values()) {
            boolean kakuja = stage == GraftStage.KAKUJA;
            AbilityGuide.Panel low = AbilityGuide.panel(new AbilityGuide.Input(KagunePath.GRAFT, AbilityGuide.State.OUT, 0, KaguneForm.RINKAKU_SEED,
                WingForm.TWIN, false, keys, false, kakuja ? GraftStage.DRAGON : stage, kakuja));
            AbilityGuide.Panel high = AbilityGuide.panel(new AbilityGuide.Input(KagunePath.GRAFT, AbilityGuide.State.OUT, 1000, KaguneForm.RINKAKU_SEED,
                WingForm.TWIN, false, keys, false, kakuja ? GraftStage.DRAGON : stage, kakuja));
            check(high.rows().size() >= 8, "his panel lists the stage's moves: " + stage);
            check(high.rows().stream().noneMatch(AbilityGuide.Row::locked), "at full mastery nothing is locked: " + stage);
            check(stage == GraftStage.TRANSPLANT || low.rows().stream().anyMatch(AbilityGuide.Row::locked), "a fresh ghoul sees what is ahead: " + stage);
            check(high.subtitle().contains("Graft"), "it is named for his path");
        }
        AbilityGuide.Panel closed = AbilityGuide.panel(new AbilityGuide.Input(KagunePath.GRAFT, AbilityGuide.State.CLOSED, 0, KaguneForm.RINKAKU_SEED,
            WingForm.TWIN, false, keys));
        check(closed.title().equals("GRAFT DNA"), "sheathed, the panel names Graft DNA");
    }

    private static void card() {
        PathCards.Card card = PathCards.of(KagunePath.GRAFT);
        check(card.path() == KagunePath.GRAFT && card.name().contains("GRAFT"), "Graft DNA has its own card");
        check(PathCards.of(KagunePath.PALE) == card, "Owl DNA's old players see Graft DNA's card");
        check(card.stageNames().size() == GraftStage.values().length && card.stageMastery().size() == card.stageNames().size(),
            "its card lists every stage with its mastery");
        boolean owl = false;
        for (PathCards.Card each : PathCards.choosable()) {
            owl |= each.path() == KagunePath.PALE || each.name().contains("OWL");
        }
        check(!owl, "no card offers Owl DNA");
    }

    /** Counts the quads put to it, the glowing ones (straight-up normal), and whether every corner is finite. */
    private static final class Count implements UkakuModel.Sink {
        int quads;
        int lit;
        boolean finite = true;

        @Override
        public void quad(double[] a, double[] b, double[] c, double[] d, double[] normal, int argbA, int argbB, int argbC, int argbD) {
            this.quads++;
            if (normal[0] == 0.0 && normal[1] == 1.0 && normal[2] == 0.0) {
                this.lit++;
            }
            for (double[] p : new double[][] {a, b, c, d, normal}) {
                this.finite &= Double.isFinite(p[0]) && Double.isFinite(p[1]) && Double.isFinite(p[2]);
            }
        }
    }

    private static boolean finite(double[][] points) {
        for (double[] p : points) {
            for (double v : p) {
                if (!Double.isFinite(v)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static double distance(double[] a, double[] b) {
        return Math.sqrt((a[0] - b[0]) * (a[0] - b[0]) + (a[1] - b[1]) * (a[1] - b[1]) + (a[2] - b[2]) * (a[2] - b[2]));
    }
}
