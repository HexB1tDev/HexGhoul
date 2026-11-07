package com.hex.kagune.control;

import com.hex.kagune.gameplay.KaguneForm;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Dependency-free tests for the pure kagune-control logic (no Minecraft needed): progression curves,
 * instinct thresholds, food classification, combo tables and combo pacing/balance.
 */
public final class ControlTests {
    private static int passed;
    private static final List<String> FAILURES = new ArrayList<>();

    private static void check(boolean condition, String description) {
        if (condition) {
            passed++;
        } else {
            FAILURES.add(description);
        }
    }

    static java.util.Map<AbilityGuide.Key, String> defaultKeys() {
        java.util.Map<AbilityGuide.Key, String> keys = new java.util.EnumMap<>(AbilityGuide.Key.class);
        String[][] names = {{"ATTACK", "LMB"}, {"USE", "RMB"}, {"TOGGLE", "R"}, {"MENU", "P"}, {"GRAB", "G"}, {"THROW", "N"}, {"RAPID", "Z"},
            {"SLINGSHOT", "J"}, {"CYCLE", "K"}, {"CRUSH", "H"}, {"LIFT", "V"}, {"SLAM", "B"}, {"GUARD", "C"}, {"UNLEASH", "LCtrl"}};
        for (String[] name : names) {
            keys.put(AbilityGuide.Key.valueOf(name[0]), name[1]);
        }
        return keys;
    }

    static AbilityGuide.Row find(AbilityGuide.Panel panel, String name) {
        return panel.rows().stream().filter(row -> row.name().startsWith(name)).findFirst().orElse(null);
    }

    /** 3.1.0: the abilities panel: the moves of the path and form, in its colours, with the player's own keys. */
    static void abilityGuide() {
        java.util.Map<AbilityGuide.Key, String> keys = defaultKeys();
        com.hex.kagune.control.ukaku.WingForm twin = com.hex.kagune.control.ukaku.WingForm.TWIN;
        AbilityGuide.Panel closed = AbilityGuide.panel(new AbilityGuide.Input(KagunePath.RINKAKU, AbilityGuide.State.CLOSED, 0,
            com.hex.kagune.gameplay.KaguneForm.RINKAKU_SEED, twin, false, keys));
        check(find(closed, "Ghoul eyes") != null && find(closed, "Ghoul eyes").keys().equals("R"), "kagune in: R opens the eyes");
        check(find(closed, "Unleash") != null && find(closed, "Unleash").keys().equals("LCtrl+R"), "kagune in: Ctrl + R unleashes it");
        java.util.Map<AbilityGuide.Key, String> unleashMoved = defaultKeys();
        unleashMoved.put(AbilityGuide.Key.UNLEASH, AbilityGuide.keyName("key.keyboard.g", "NONE"));
        AbilityGuide.Panel unleashBoard = AbilityGuide.panel(new AbilityGuide.Input(KagunePath.RINKAKU, AbilityGuide.State.CLOSED, 0,
            com.hex.kagune.gameplay.KaguneForm.RINKAKU_SEED, com.hex.kagune.control.ukaku.WingForm.TWIN, false, unleashMoved));
        check(find(unleashBoard, "Unleash").keys().equals("G+R"), "the unleash key rebound in Controls shows on the board");
        AbilityGuide.Panel eyes = AbilityGuide.panel(new AbilityGuide.Input(KagunePath.UKAKU, AbilityGuide.State.EYES, 0,
            com.hex.kagune.gameplay.KaguneForm.RINKAKU_SEED, twin, false, keys));
        check(find(eyes, "Close the kakugan") != null && eyes.subtitle().contains("eyes"), "eyes open: R closes them");
        check(find(eyes, "Resistance") != null, "6.1.0: the eyes' own buffs and their RC cost are on the board");
        // Rinkaku: the combo is named for the form, crimson in the rinkaku forms, plum in the kakuja.
        String[] combos = {"Whip", "Scissor", "Flurry", "Heavy", "Predator"};
        com.hex.kagune.gameplay.KaguneForm[] forms = {com.hex.kagune.gameplay.KaguneForm.RINKAKU_SEED, com.hex.kagune.gameplay.KaguneForm.RINKAKU_DUAL,
            com.hex.kagune.gameplay.KaguneForm.RINKAKU_QUAD, com.hex.kagune.gameplay.KaguneForm.CENTIPEDE, com.hex.kagune.gameplay.KaguneForm.FULL_KAKUJA};
        java.util.Set<Integer> accents = new java.util.HashSet<>();
        for (int i = 0; i < forms.length; i++) {
            AbilityGuide.Panel panel = AbilityGuide.panel(new AbilityGuide.Input(KagunePath.RINKAKU, AbilityGuide.State.OUT, 800, forms[i], twin, false, keys));
            AbilityGuide.Row combo = find(panel, combos[i]);
            if (forms[i] == com.hex.kagune.gameplay.KaguneForm.CENTIPEDE) {
                // 5.0.10: the first kakuja's legs fight on their own and he can only walk: no move key on the board but K and R.
                java.util.Set<String> moveKeys = java.util.Set.of("LMB", "Z", "J", "G", "N", "H", "V", "B", "C", "G·N", "H·V·B");
                boolean anyMove = false;
                for (AbilityGuide.Row row : panel.rows()) {
                    anyMove |= moveKeys.contains(row.keys());
                }
                check(combo == null && !anyMove && find(panel, "Thy Death") == null && find(panel, "Grab") == null
                    && find(panel, "Guard") == null && find(panel, "Grapple") == null, "the first kakuja lists no move he can make");
                check(find(panel, "Centipede legs fight on their own") != null && find(panel, "You can only walk") != null
                    && find(panel, "Turn back") != null && find(panel, "Put it all away") != null,
                    "the first kakuja: the legs fight on their own, he walks, K and R still take him out of it");
            } else {
                check(combo != null && combo.keys().equals("LMB"), forms[i] + ": the M1 is the " + combos[i] + " combo on the attack key");
                check(find(panel, "Thy Death") != null && find(panel, "Thy Death").keys().equals("Z"), forms[i] + ": Z is Thy Death");
                // 3.3.0: what G, N, H, V and B do, as it is now: with nothing held, and with a tail holding someone.
                check(find(panel, "Grab") != null && find(panel, "Grab").keys().equals("G") && find(panel, "Rubble Hurl").keys().equals("N hold")
                    && find(panel, "Impale").keys().equals("H hold") && find(panel, "Lift · Slam").keys().equals("V·B")
                    && find(panel, "Lift · Slam").colour() == AbilityGuide.DIM && find(panel, "Throw them") == null && find(panel, "Crush them") == null,
                    forms[i] + ": nothing held: grab on G, the Rubble Hurl on N held, the Impale on H held, lift and slam dim");
                AbilityGuide.Panel holding = AbilityGuide.panel(new AbilityGuide.Input(KagunePath.RINKAKU, AbilityGuide.State.OUT, 800, forms[i], twin, false,
                    keys, false, com.hex.kagune.control.graft.GraftStage.TRANSPLANT, false, java.util.Set.of(), true));
                // 3.3.1: Ken teaches the Hurl and the Impale: until he has, past their mastery the board says so, dim.
                AbilityGuide.Panel untaught = AbilityGuide.panel(new AbilityGuide.Input(KagunePath.RINKAKU, AbilityGuide.State.OUT, 800, forms[i], twin, false,
                    keys, false, com.hex.kagune.control.graft.GraftStage.TRANSPLANT, false,
                    java.util.Set.of(com.hex.kagune.control.quest.Gate.RINKAKU_HURL, com.hex.kagune.control.quest.Gate.RINKAKU_IMPALE), false));
                check(find(untaught, "Rubble Hurl (Ken teaches it)") != null && find(untaught, "Rubble Hurl (Ken teaches it)").colour() == AbilityGuide.DIM
                    && find(untaught, "Impale (Ken teaches it)") != null && find(panel, "Rubble Hurl (Ken") == null,
                    forms[i] + ": the Hurl and the Impale say Ken teaches them until he has");
                check(find(holding, "Throw them").keys().equals("N") && find(holding, "Crush them").keys().equals("H") && find(holding, "Lift them").keys().equals("V")
                    && find(holding, "Slam them").keys().equals("B") && find(holding, "Grab another").keys().equals("G") && find(holding, "Rubble Hurl") == null
                    && find(holding, "Impale") == null && holding.subtitle().contains("holds someone"),
                    forms[i] + ": a tail holding someone: throw, crush, lift, slam them on N, H, V, B, grab another on G, no Hurl or Impale");
            }
            check((find(panel, "Centipede legs") != null) == forms[i].hasCentipedeLegs(), forms[i] + ": the legs' hunt only with legs");
            accents.add(panel.accent());
        }
        check(accents.size() == 2, "Rinkaku: one colour for the rinkaku forms, another for the kakuja");
        AbilityGuide.Panel centipede = AbilityGuide.panel(new AbilityGuide.Input(KagunePath.RINKAKU, AbilityGuide.State.OUT, 800,
            com.hex.kagune.gameplay.KaguneForm.CENTIPEDE, twin, false, keys));
        check(centipede.title().equals("CENTIPEDE KAKUJA"), "the first kakuja's title");
        check(find(centipede, "Turn back") != null && find(centipede, "Final kakuja").needs() == 1000, "in the first kakuja: K back, hold K at 1000");
        AbilityGuide.Panel full = AbilityGuide.panel(new AbilityGuide.Input(KagunePath.RINKAKU, AbilityGuide.State.OUT, 1000,
            com.hex.kagune.gameplay.KaguneForm.FULL_KAKUJA, twin, false, keys));
        check(find(full, "Final kakuja") == null && find(full, "Turn back") != null, "in the final kakuja: only the way back");
        AbilityGuide.Panel seed = AbilityGuide.panel(new AbilityGuide.Input(KagunePath.RINKAKU, AbilityGuide.State.OUT, 300,
            com.hex.kagune.gameplay.KaguneForm.RINKAKU_SEED, twin, false, keys));
        check(find(seed, "First kakuja").needs() == 750 && find(seed, "Final kakuja").needs() == 1000, "kakuja locked below 750 and 1000");
        // Ukaku: the moves and their colour change with the form and the crystal mode; locked moves say when they come.
        AbilityGuide.Panel fire = AbilityGuide.panel(new AbilityGuide.Input(KagunePath.UKAKU, AbilityGuide.State.OUT, 120,
            com.hex.kagune.gameplay.KaguneForm.RINKAKU_SEED, twin, false, keys));
        check(find(fire, "Fire shots") != null && find(fire, "Fire enders").keys().equals("RMB"), "Ukaku's wings: fire on both buttons");
        check(find(fire, "Wing Vortex").needs() == 150 && find(fire, "Crystal mode").needs() == 200, "Ukaku at 120: Vortex at 150, crystal at 200");
        AbilityGuide.Panel crystal = AbilityGuide.panel(new AbilityGuide.Input(KagunePath.UKAKU, AbilityGuide.State.OUT, 300,
            com.hex.kagune.gameplay.KaguneForm.RINKAKU_SEED, twin, true, keys));
        check(find(crystal, "Crystal spikes") != null && find(crystal, "Gatling").needs() == 400 && find(crystal, "Gatling").keys().equals("J hold"),
            "crystal mode: spikes, the Gatling held on J from 400");
        check(find(crystal, "Wing Vortex") == null && find(fire, "Wing Vortex") != null, "the Wing Vortex is the fire wings' alone, not the crystal's");
        AbilityGuide.Panel first = AbilityGuide.panel(new AbilityGuide.Input(KagunePath.UKAKU, AbilityGuide.State.OUT, 760,
            com.hex.kagune.gameplay.KaguneForm.RINKAKU_SEED, com.hex.kagune.control.ukaku.WingForm.KAKUJA, false, keys));
        check(find(first, "Needle Storm") != null && find(first, "Rushing Death").needs() == 800, "first kakuja: needles, Rushing Death at 800");
        AbilityGuide.Panel last = AbilityGuide.panel(new AbilityGuide.Input(KagunePath.UKAKU, AbilityGuide.State.OUT, 1000,
            com.hex.kagune.gameplay.KaguneForm.RINKAKU_SEED, com.hex.kagune.control.ukaku.WingForm.FINAL_KAKUJA, false, keys));
        check(find(last, "Needles + fire") != null && find(last, "Fire cyclone") != null, "final kakuja: needles and fire, the cyclone");
        check(java.util.stream.Stream.of(fire, crystal, first, last).map(AbilityGuide.Panel::accent).distinct().count() == 4,
            "Ukaku: each form its own colour");
        // 5.0.13: the Wing Block is gone: no Ukaku form shows a move on C (the Guard key), but for the final kakuja's Steel Blade
        // (5.0.17) on it.
        check(java.util.stream.Stream.of(fire, crystal, first).flatMap(panel -> panel.rows().stream()).noneMatch(row -> row.keys().equals("C"))
            && last.rows().stream().filter(row -> row.keys().equals("C")).allMatch(row -> row.name().contains("Steel Blade")),
            "no Ukaku move on the Guard key but the final kakuja's Steel Blade");
        // 6.0.0: with the blade out, its swings, the dash, the vanish and the fireballs.
        AbilityGuide.Panel blade = AbilityGuide.panel(new AbilityGuide.Input(KagunePath.UKAKU, AbilityGuide.State.OUT, 1000,
            com.hex.kagune.gameplay.KaguneForm.RINKAKU_SEED, com.hex.kagune.control.ukaku.WingForm.FINAL_KAKUJA, false, keys, true));
        check(find(blade, "Steel Blade:") != null && find(blade, "Dash") != null && find(blade, "Vanish") == null && find(blade, "Reaper's dash") != null && find(blade, "Fireballs") != null,
            "the Steel Blade's board: swings, dash, the Reaper's dash (2.1.7: no vanish), fireballs");
        // The player's own keys.
        java.util.Map<AbilityGuide.Key, String> rebound = defaultKeys();
        rebound.put(AbilityGuide.Key.RAPID, AbilityGuide.keyName("key.keyboard.f", "CONTROL"));
        rebound.put(AbilityGuide.Key.TOGGLE, AbilityGuide.keyName("key.mouse.4", "NONE"));
        AbilityGuide.Panel moved = AbilityGuide.panel(new AbilityGuide.Input(KagunePath.RINKAKU, AbilityGuide.State.OUT, 100,
            com.hex.kagune.gameplay.KaguneForm.RINKAKU_SEED, twin, false, rebound));
        check(find(moved, "Thy Death").keys().equals("Ctrl+F"), "a rebound key shows as bound: " + find(moved, "Thy Death").keys());
        check(find(moved, "Kakugan mode").keys().equals("M4") && find(moved, "Put it all away").keys().equals("LCtrl+M4"),
            "a mouse button shows as bound: kakugan mode on it, everything away on Ctrl with it");
        check(AbilityGuide.keyName("key.mouse.left", "NONE").equals("LMB") && AbilityGuide.keyName("key.mouse.right", "NONE").equals("RMB"),
            "mouse buttons");
        check(AbilityGuide.keyName("key.keyboard.r", "ALT").equals("Alt+R") && AbilityGuide.keyName("key.keyboard.unknown", "SHIFT").equals("-"),
            "a modifier, and an unbound key");
        check(AbilityGuide.keyName("key.keyboard.left.shift", "NONE").equals("LShift") && AbilityGuide.keyName("key.keyboard.keypad.5", "NONE")
            .equals("Num5") && AbilityGuide.keyName("key.keyboard.f10", "NONE").equals("F10"), "named keys");
        AbilityGuide.Panel none = AbilityGuide.panel(new AbilityGuide.Input(KagunePath.NONE, AbilityGuide.State.CLOSED, 0,
            com.hex.kagune.gameplay.KaguneForm.RINKAKU_SEED, twin, false, keys));
        check(find(none, "Choose") != null && find(none, "Choose").keys().equals("P"), "undecided: the menu picks a path");
    }

    /** 3.1.0: Rinkaku's tentacles drawn through critically damped springs: exact at any frame rate, root fixed, calm, no overshoot. */
    static void pathSmoother() {
        // At rest the original's slow sway is cut to about a third round the calm shape.
        PathSmoother resting = new PathSmoother();
        double swing = 0.0;
        for (double t = 0.0; t < 900.0; t += 0.5) {
            double[][] swaying = new double[8][];
            for (int i = 0; i < 8; i++) {
                swaying[i] = new double[] {i * 0.5 + (i / 7.0) * Math.sin(t * 2.0 * Math.PI / 60.0), 1.0, -0.2 * i};
            }
            double[][] eased = resting.follow(swaying, t, 5.0, 0.25);
            if (t > 600.0) {
                swing = Math.max(swing, Math.abs(eased[7][0] - 3.5));
            }
        }
        check(swing > 0.05 && swing < 0.45, "the idle sway is calmed, not frozen: " + swing);
        double[][] from = new double[20][];
        double[][] to = new double[20][];
        for (int i = 0; i < 20; i++) {
            from[i] = new double[] {0.1 * i, 0.05 * i, -0.2 * i};
            to[i] = new double[] {0.3 * i, 0.15 * i, 0.25 * i};
        }
        PathSmoother coarse = new PathSmoother();
        PathSmoother fine = new PathSmoother();
        coarse.follow(from, 0.0, 2.0);
        fine.follow(from, 0.0, 2.0);
        double[][] a = null;
        double[][] b = null;
        for (int tick = 1; tick <= 5; tick++) {
            a = coarse.follow(to, tick, 2.0);
        }
        for (int frame = 1; frame <= 40; frame++) {
            b = fine.follow(to, frame * 0.125, 2.0);
        }
        double drift = 0.0;
        for (int i = 0; i < 20; i++) {
            drift = Math.max(drift, Math.abs(a[i][0] - b[i][0]) + Math.abs(a[i][1] - b[i][1]) + Math.abs(a[i][2] - b[i][2]));
        }
        check(drift < 1.0E-9, "tentacle smoothing is the same at 20 and 160 frames a second: " + drift);
        PathSmoother calm = new PathSmoother();
        calm.follow(from, 0.0, 2.0);
        double last = Double.MAX_VALUE;
        boolean monotone = true;
        double[][] now = null;
        for (double time = 0.25; time <= 30.0; time += 0.25) {
            now = calm.follow(to, time, 2.0);
            double gap = Math.abs(now[19][2] - to[19][2]);
            monotone &= gap <= last + 1.0E-12;
            last = gap;
            check(now[0][0] == to[0][0] && now[0][1] == to[0][1] && now[0][2] == to[0][2], "the root stays on his back");
        }
        check(monotone && last < 1.0E-4, "a tentacle settles on its path without swinging past it: " + last);
        double[][] eased = new PathSmoother().follow(to, 3.0, 2.0);
        check(eased[19][2] == to[19][2], "a new tentacle starts on its path");
        PathSmoother gap = new PathSmoother();
        gap.follow(from, 0.0, 2.0);
        check(gap.follow(to, 40.0, 2.0)[19][2] == to[19][2], "after a long gap it starts afresh instead of crawling over");
        check(gap.follow(new double[][] {{0, 0, 0}, {1, 1, 1}}, 40.5, 2.0)[1][0] == 1.0, "a new shape starts afresh");
    }

    /** 2.0.39: a Rinkaku tail as the owner's reference has it, in the tail's own colour. */
    static void rinkakuRibbon() {
        int[] crimson = RinkakuRibbon.palette(KaguneColours.DEFAULT);
        check(crimson[0] == RinkakuRibbon.HEART && crimson[1] == RinkakuRibbon.DEEP && crimson[2] == RinkakuRibbon.LIGHT,
            "the original crimson is exactly the picture's palette");
        int[] blue = RinkakuRibbon.palette(0x1E50D8);
        double[] heart = KaguneColours.toHsv(blue[0] & 0xFFFFFF);
        check(heart[0] > 0.55 && heart[0] < 0.72 && KaguneColours.toHsv(blue[2] & 0xFFFFFF)[2] > heart[2], "a blue kagune: a blue ribbon, its marks lighter");
        int[] black = RinkakuRibbon.palette(0x141414);
        check(KaguneColours.toHsv(black[0] & 0xFFFFFF)[2] < 0.3 && KaguneColours.toHsv(black[2] & 0xFFFFFF)[2] > KaguneColours.toHsv(black[0] & 0xFFFFFF)[2] + 0.15,
            "a black kagune: a black ribbon (never blood red), its marks grey enough to show");
        for (int colour : new int[] {KaguneColours.DEFAULT, 0x1E50D8, 0x7A1FB8, 0x1F9E4A, 0x141414, 0xE8E8E8}) {
            int[] p = RinkakuRibbon.palette(colour);
            check(KaguneColours.toHsv(p[1] & 0xFFFFFF)[2] < KaguneColours.toHsv(p[0] & 0xFFFFFF)[2], "its edges deeper than its heart: " + Integer.toHexString(colour));
        }
        // 2.0.50 (the owner's drawing): thin where it leaves the body, broadest near its far end, a short rounded end.
        check(RinkakuRibbon.profile(1.0) == 0.0, "it ends");
        check(RinkakuRibbon.profile(0.0) < RinkakuRibbon.profile(RinkakuRibbon.BROADEST) * 0.3, "thin where it leaves the body");
        boolean widening = true;
        for (double t = 0.02; t <= RinkakuRibbon.BROADEST; t += 0.02) {
            widening &= RinkakuRibbon.profile(t) > RinkakuRibbon.profile(t - 0.02);
        }
        check(widening, "widening all the way out to its broadest");
        double broadest = 0.0;
        double where = 0.0;
        for (double t = 0.0; t <= 1.0; t += 0.005) {
            if (RinkakuRibbon.profile(t) > broadest) {
                broadest = RinkakuRibbon.profile(t);
                where = t;
            }
        }
        check(where > 0.7 && where < 0.85, "broadest near its far end (" + where + ")");
        check(RinkakuRibbon.profile(0.97) > RinkakuRibbon.profile(0.5) * 0.4, "its end rounded, not drawn out to a needle");
        check(RinkakuRibbon.WIDE > RinkakuRibbon.THIN * 1.5, "a broad, flat ribbon");
        check(RinkakuRibbon.shade(0.0, false, 0.0, 0) < RinkakuRibbon.shade(1.0, false, 0.0, 0), "darker at its edges");
        double[][] path = new double[30][];
        for (int i = 0; i < 30; i++) {
            path[i] = new double[] {0.1 * i, 0.9 + Math.sin(i * 0.2), -0.2 - 0.05 * i};
        }
        RinkakuRibbon.Mesh mesh = RinkakuRibbon.build(path, 0.22, 0x1E50D8, 7);
        check(mesh != null && mesh.at().length >= 4 && mesh.at().length <= RinkakuRibbon.MOST_RINGS && mesh.at()[0].length == RinkakuRibbon.SIDES + 1,
            "a mesh of rings round it");
        boolean finite = true;
        for (double[][] ring : mesh.at()) {
            for (double[] p : ring) {
                finite &= Double.isFinite(p[0]) && Double.isFinite(p[1]) && Double.isFinite(p[2]);
            }
        }
        check(finite, "no NaN in the mesh");
        check(java.util.Arrays.equals(mesh.at()[2][0], mesh.at()[2][RinkakuRibbon.SIDES]), "each ring closes on itself");
        check(mesh.along()[0] == 0.0 && mesh.along()[mesh.along().length - 1] > mesh.along()[1] && RinkakuRibbon.v(1.0) > RinkakuRibbon.v(0.5),
            "its markings run from the root toward the point");
        double[][][] over = RinkakuRibbon.over(mesh);
        double[] c = mesh.centre()[3];
        check(RinkakuRoot.distance(over[3][4], c) > RinkakuRoot.distance(mesh.at()[3][4], c), "its markings a hair outside it");
        check(RinkakuRibbon.build(new double[][] {{0, 0, 0}}, 0.2, 0, 0) == null && RinkakuRibbon.build(path, 0.0, 0, 0) == null, "nothing to draw: nothing");
        // 2.0.41: its material, as Ukaku's: by the camera, glowing veins.
        int[] pal = RinkakuRibbon.palette(KaguneColours.DEFAULT);
        int face = RinkakuRibbon.surface(pal[0], pal, 0.5, 1.0, 0.0);
        int edge = RinkakuRibbon.surface(pal[0], pal, 0.5, 0.0, 0.0);
        check(face >>> 24 == 255 && edge >>> 24 < 255 && (edge >>> 24) >= (int)(RinkakuRibbon.CLEAREST * 255) - 1,
            "solid face on, a little clear seen edge-on");
        check(KaguneColours.toHsv(edge & 0xFFFFFF)[2] > KaguneColours.toHsv(face & 0xFFFFFF)[2], "a lighter, glossy rim toward its silhouette");
        check(KaguneColours.toHsv(RinkakuRibbon.surface(pal[0], pal, 0.5, 1.0, 1.0) & 0xFFFFFF)[1] < KaguneColours.toHsv(face & 0xFFFFFF)[1],
            "a pale glint where the light catches it");
        check(RinkakuRibbon.surface(pal[0], pal, 0.5, -1.0, 0.0) >>> 24 == 255, "not knowing the camera: solid");
        check(RinkakuRibbon.markAlpha(0.0) == 0 && RinkakuRibbon.markAlpha(1.0) == 255 && RinkakuRibbon.markAlpha(-1.0) == 255,
            "its markings fade seen edge-on");
        RinkakuRibbon.Mesh seen = RinkakuRibbon.build(path, 0.22, KaguneColours.DEFAULT, 7, new double[] {0.0, 1.0, -30.0});
        java.util.List<RinkakuRibbon.Glow> veins = RinkakuRibbon.veins(seen, 7, 40.0);
        check(veins.size() > RinkakuRibbon.VEINS * 4, "veins run along it, forked");
        check(RinkakuRibbon.VEINS >= 6 && RinkakuRibbon.VEIN_AURA > RinkakuRibbon.VEIN_SOFT && RinkakuRibbon.VEIN_SOFT > RinkakuRibbon.VEIN_CORE * 2.0,
            "2.0.50: more veins, a glow and an aura round them");
        check(RinkakuRibbon.VEIN_CORE < 0.006 && RinkakuRibbon.VEIN_SOFT < 0.02, "2.0.51: fine, as veins are");
        int bright = 0;
        for (RinkakuRibbon.Glow g : veins) {
            bright = Math.max(bright, g.argb()[0] >>> 24);
        }
        check(bright > 220, "their cores burn bright (" + bright + ")");
        boolean glowing = true;
        for (RinkakuRibbon.Glow g : veins) {
            for (int argb : g.argb()) {
                glowing &= (argb >>> 24) <= 255 && KaguneColours.toHsv(argb & 0xFFFFFF)[0] > 0.9 || (argb >>> 24) == 0;
            }
            for (double[] q : g.corners()) {
                glowing &= Double.isFinite(q[0]) && Double.isFinite(q[1]) && Double.isFinite(q[2]);
            }
        }
        check(glowing, "the crimson's veins glow red, no NaN");
        RinkakuRibbon.Mesh blueMesh = RinkakuRibbon.build(path, 0.22, 0x1E50D8, 7, new double[] {0.0, 1.0, -30.0});
        int vein = RinkakuRibbon.veins(blueMesh, 7, 40.0).get(1).argb()[1];
        double hue = KaguneColours.toHsv(vein & 0xFFFFFF)[0];
        check(hue > 0.5 && hue < 0.75, "a blue kagune's veins glow blue");
    }

    /** 2.0.38: a Rinkaku tail grows out of the lower back: its root inside the body, its base leaving the back, its far part its own. */
    static void rinkakuRoot() {
        double height = 1.8;
        for (int limb = 0; limb < 6; limb++) {
            double side = RinkakuRoot.side(limb, 0.0);
            double[] root = RinkakuRoot.root(limb, side, height);
            check(root[2] > RinkakuRoot.BACK && root[2] < 0.0 && Math.abs(root[0]) < 0.25 && root[1] > 0.75 && root[1] < 1.0,
                "tail " + limb + ": rooted inside the lower back (" + java.util.Arrays.toString(root) + ")");
            double[] way = RinkakuRoot.direction(limb, side);
            check(way[2] < -0.6 && Math.signum(way[0]) == side && Math.abs(Math.hypot(Math.hypot(way[0], way[1]), way[2]) - 1.0) < 1.0E-9,
                "tail " + limb + ": leaves the back, round to its own side");
            int n = 30;
            double[][] path = new double[n][];
            for (int i = 0; i < n; i++) {
                double t = i / (double)(n - 1);
                path[i] = new double[] {side * (0.15 + 0.9 * t), 0.774 + 1.5 * Math.sin(t * 1.4), -0.18 - 1.3 * t + 0.4 * t * t};
            }
            double[][] grown = RinkakuRoot.attach(path, limb, 0.0, height);
            check(grown != path && grown.length == n && java.util.Arrays.equals(grown[0], root), "tail " + limb + ": its first point is the root");
            boolean leaving = true;
            for (int i = 1; i < n && RinkakuRoot.distance(grown[i], grown[0]) < RinkakuRoot.BASE * 0.5; i++) {
                double[] d = {grown[i][0] - root[0], grown[i][1] - root[1], grown[i][2] - root[2]};
                double len = Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]);
                leaving &= (d[0] * way[0] + d[1] * way[1] + d[2] * way[2]) / len > 0.999;
            }
            check(leaving, "tail " + limb + ": its base is straight out of the back");
            check(java.util.Arrays.equals(grown[n - 1], path[n - 1]) || RinkakuRoot.distance(grown[n - 1], path[n - 1]) < 1.0E-9,
                "tail " + limb + ": its tip stays where its motion put it");
            boolean finite = true;
            for (double[] p : grown) {
                finite &= Double.isFinite(p[0]) && Double.isFinite(p[1]) && Double.isFinite(p[2]);
            }
            check(finite, "tail " + limb + ": no NaN");
        }
        check(RinkakuRoot.attach(new double[][] {{0, 1, 0}}, 0, 0.0, 1.8).length == 1, "a path too short is left alone");
        check(RinkakuRoot.root(0, -1.0, 0.9)[1] < RinkakuRoot.root(0, -1.0, 1.8)[1], "a smaller body's root is lower");
    }

    /** 2.0.42: Rinkaku's block: the perfect window, the weight that breaks it, what it covers, and the tails' guard in front of him. */
    static void rinkakuBlock() {
        // The perfect window: only a fresh press, only its first three ticks.
        check(RinkakuBlock.perfect(100, true, 100) && RinkakuBlock.perfect(100, true, 102), "a blow in the first ticks of a fresh press is perfect");
        check(!RinkakuBlock.perfect(100, true, 103), "three ticks after the press is too late");
        check(!RinkakuBlock.perfect(100, false, 100), "a block raised without a fresh press has no perfect window");
        check(!RinkakuBlock.perfect(100, true, 99), "a blow before the press is not perfect");
        check(RinkakuBlock.PERFECT_TICKS <= 3, "the window is extremely tight (0.15 s at most)");
        check(!RinkakuBlock.opensWindow(100, 110) && RinkakuBlock.opensWindow(100, 100 + RinkakuBlock.FRESH_AFTER),
            "mashing the button opens no window; a press after a pause does");
        check(RinkakuBlock.opensWindow(Long.MIN_VALUE / 2, 0), "the first press opens one");
        // The weight: light blows one each, heavy ones more, capped; a flurry counts once every few ticks.
        check(RinkakuBlock.weight(2.0F) == 1.0F && RinkakuBlock.weight(RinkakuBlock.LIGHT_BLOW) == 1.0F, "a light blow weighs one");
        check(RinkakuBlock.weight(12.0F) == 2.0F && RinkakuBlock.weight(1000.0F) == RinkakuBlock.MOST_WEIGHT, "a heavy blow weighs more, capped");
        check(RinkakuBlock.weight(Float.NaN) == 1.0F, "an odd amount weighs one");
        int light = 0;
        float weight = 0.0F;
        while (!RinkakuBlock.breaks(weight)) {
            weight += RinkakuBlock.weight(4.0F);
            light++;
        }
        check(light == 6, "six light blows break the guard (" + light + ")");
        check(RinkakuBlock.breaks(RinkakuBlock.weight(30.0F) * 2), "two crushing blows break it");
        check(!RinkakuBlock.counts(100, 100 + RinkakuBlock.HIT_GAP - 1) && RinkakuBlock.counts(100, 100 + RinkakuBlock.HIT_GAP),
            "blows in the same flurry weigh once");
        check(RinkakuBlock.settle(3.0F, RinkakuBlock.RECOVER_DELAY - 1) == 3.0F, "the weight stays a while after a blow");
        float drained = 3.0F;
        for (int t = 0; t < 200; t++) {
            drained = RinkakuBlock.settle(drained, RinkakuBlock.RECOVER_DELAY + t);
        }
        check(drained == 0.0F, "then drains away");
        check(RinkakuBlock.BREAK_COOLDOWN >= 60, "a broken guard cools down for seconds");
        // What it covers: in front and to the sides, not behind; straight above or below counts.
        double[] facing = {0.0, 0.0, 1.0};
        check(RinkakuBlock.covers(facing, new double[] {0.0, 0.3, 3.0}), "a blow from in front is blocked");
        check(RinkakuBlock.covers(facing, new double[] {3.0, 0.0, 0.0}), "and from the side");
        check(!RinkakuBlock.covers(facing, new double[] {0.0, 0.0, -3.0}), "not from behind");
        check(!RinkakuBlock.covers(facing, new double[] {2.0, 0.0, -3.0}), "nor from behind and to a side");
        check(RinkakuBlock.covers(facing, new double[] {0.05, -1.0, 0.0}), "one from right under him counts as in front");
        // The guard's look: up in three ticks, down in six.
        check(RinkakuBlock.amount(0.0, Double.NaN) == 0.0F && RinkakuBlock.amount(RinkakuBlock.RAISE_TICKS, Double.NaN) == 1.0F,
            "the tails swing into the guard in " + RinkakuBlock.RAISE_TICKS + " ticks");
        check(RinkakuBlock.amount(20.0, RinkakuBlock.LOWER_TICKS) == 0.0F && RinkakuBlock.amount(20.0, 2.0) > 0.0F, "and fold back out");
        check(RinkakuBlock.recoil(1.0, false) < -0.1 && RinkakuBlock.recoil(1.0, true) > 0.2, "a blow presses the guard back, a perfect block snaps it out");
        check(RinkakuBlock.recoil(RinkakuBlock.SNAP_TICKS + 1.0, true) == 0.0 && RinkakuBlock.recoil(RinkakuBlock.RECOIL_TICKS, false) == 0.0
            && RinkakuBlock.recoil(Double.NaN, false) == 0.0, "and both settle");
        check(RinkakuBlock.thrown(RinkakuBlock.THROW_TICKS)[0] == 1.0 && RinkakuBlock.thrown(40.0)[1] == 0.0, "a broken guard's tails are thrown back, then let go");
        // The tails: from the root round the side, crossing in front of him, each lane lower and further out.
        double height = 1.8;
        for (int tails = 1; tails <= 6; tails++) {
            check(RinkakuBlock.lanes(tails) == Math.min(3, (tails + 1) / 2), tails + " tails make " + RinkakuBlock.lanes(tails) + " lanes");
            for (int limb = 0; limb < tails; limb++) {
                double side = RinkakuRoot.side(limb, 0.0);
                double[][] pose = RinkakuBlock.pose(limb, tails, 0.0, 24, height);
                String name = tails + " tails, tail " + limb;
                check(pose.length == 24 && java.util.Arrays.equals(pose[0], RinkakuRoot.root(limb, side, height)), name + ": from its root");
                double[] middle = pose[15];
                check(middle[2] > 0.5 && Math.abs(middle[0]) < 0.45 && middle[1] > 0.7 && middle[1] < 1.85,
                    name + ": crosses in front of him (" + java.util.Arrays.toString(middle) + ")");
                boolean outside = true;
                boolean finite = true;
                double step = 0.0;
                double most = 0.0;
                double least = 1.0E9;
                for (int i = 0; i < pose.length; i++) {
                    double[] p = pose[i];
                    finite &= Double.isFinite(p[0]) && Double.isFinite(p[1]) && Double.isFinite(p[2]);
                    // Past its base, never through the torso (x within 0.3, z within 0.15, from the hips to the shoulders).
                    if (i > 3) {
                        outside &= !(Math.abs(p[0]) < 0.3 && Math.abs(p[2]) < 0.2 && p[1] > 0.7 && p[1] < 1.5);
                    }
                    if (i > 0) {
                        step = RinkakuRoot.distance(p, pose[i - 1]);
                        most = Math.max(most, step);
                        least = Math.min(least, step);
                    }
                }
                check(finite, name + ": no NaN");
                check(outside, name + ": never through his body");
                check(most - least < 0.02, name + ": evenly spaced");
                check(Math.signum(pose[8][0]) == side, name + ": comes round its own side");
                check(Math.signum(pose[23][0]) == -side || tails == 1 && Math.abs(pose[23][0]) > 0.3, name + ": ends on the far side");
            }
        }
        double[][] top = RinkakuBlock.pose(0, 4, 0.0, 24, height);
        double[][] low = RinkakuBlock.pose(2, 4, 0.0, 24, height);
        check(top[15][1] > low[15][1] + 0.25 && low[15][2] > top[15][2], "the second pair crosses lower, a little further out");
        double[][] a = RinkakuBlock.pose(0, 2, 0.0, 24, height);
        double[][] b = RinkakuBlock.pose(1, 2, 0.0, 24, height);
        check(RinkakuRoot.distance(a[15], b[15]) > 0.08, "a pair's two tails cross apart, one before the other");
        double[][] flung = RinkakuBlock.flung(0, 0.0, 24, height);
        check(flung[23][2] < -1.2 && flung[23][1] > 2.0, "a broken guard throws the tails back up over him");
        double[][] pushed = RinkakuBlock.push(top, new double[] {0.0, 0.0, 1.0}, 0.2);
        check(pushed[0][2] == top[0][2] && Math.abs(pushed[23][2] - top[23][2] - 0.2) < 1.0E-9, "a press moves the guard's front, not its root");
        double[][] half = RinkakuBlock.blend(top, flung, 0.5);
        check(Math.abs(half[10][1] - (top[10][1] + flung[10][1]) * 0.5) < 1.0E-9, "the blend goes halfway");
        double[][] small = RinkakuBlock.pose(0, 2, 0.0, 24, 0.9);
        check(small[15][1] < top[15][1] * 0.6, "a smaller body's guard is lower");
        // Its row in the guide: on the right click, but not in the final kakuja (whose right click is its dash).
        java.util.Map<AbilityGuide.Key, String> keys = defaultKeys();
        AbilityGuide.Panel quad = AbilityGuide.panel(new AbilityGuide.Input(KagunePath.RINKAKU, AbilityGuide.State.OUT, 1000,
            com.hex.kagune.gameplay.KaguneForm.RINKAKU_QUAD, com.hex.kagune.control.ukaku.WingForm.TWIN, false, keys));
        check(find(quad, "Block (hold) · on time: stagger") != null, "the guide shows the block");
        AbilityGuide.Panel full = AbilityGuide.panel(new AbilityGuide.Input(KagunePath.RINKAKU, AbilityGuide.State.OUT, 1000,
            com.hex.kagune.gameplay.KaguneForm.FULL_KAKUJA, com.hex.kagune.control.ukaku.WingForm.TWIN, false, keys));
        check(find(full, "Block (hold) · on time: stagger") == null, "not in the final kakuja");
    }

    /** 2.0.51: the tails at rest as drawn (RinkakuIdle), alive (RinkakuRibbon.stretch, swell), one skin (lifted). */
    /** 2.1.7: the White Reaper's dash (ReaperDash): three charges, one back every five seconds, the double tap, the way it goes. */
    static void reaperDash() {
        ReaperDash.Charges charges = new ReaperDash.Charges();
        check(charges.ready(0L) == 3 && charges.spend(0L) && charges.ready(1L) == 2 && ReaperDash.bar(2).equals("Dash 2/3"), "three, one spent: 2/3");
        check(charges.spend(10L) && charges.spend(20L) && !charges.spend(30L) && charges.ready(30L) == 0, "three in a row, then none");
        check(charges.ready(99L) == 0 && charges.ready(100L) == 1 && charges.ready(199L) == 1 && charges.ready(200L) == 2 && charges.ready(300L) == 3
            && charges.ready(5000L) == 3, "one back every five seconds, from the first spent, up to three");
        ReaperDash.Charges again = new ReaperDash.Charges();
        again.spend(0L);
        again.ready(100L);
        check(again.spend(150L) && again.ready(249L) == 2 && again.ready(250L) == 3, "a refill starts again from the spend after it's full");
        ReaperDash.Taps taps = new ReaperDash.Taps();
        check(!taps.tick(true, 0L) && !taps.tick(true, 1L) && !taps.tick(false, 2L) && taps.tick(true, 5L), "two presses within seven ticks");
        check(!taps.tick(false, 6L) && !taps.tick(true, 30L) && !taps.tick(false, 31L) && !taps.tick(true, 45L), "too slow: no dash");
        double[] ahead = ReaperDash.direction(0.0, 0.0, 0.0);
        double[] back = ReaperDash.direction(0.0, -1.0, 0.0);
        double[] left = ReaperDash.direction(0.0, 0.0, 1.0);
        check(Math.abs(ahead[1] - 1.0) < 1e-9 && Math.abs(back[1] + 1.0) < 1e-9 && Math.abs(left[0] - 1.0) < 1e-9,
            "facing south: no keys south, S north, A east (his left)");
        for (int deg = 0; deg < 360; deg += 15) {
            double[] way = ReaperDash.direction(deg, 1.0, 0.3);
            double[] back2 = ReaperDash.decode(ReaperDash.encode(way));
            check(Math.abs(way[0] - back2[0]) < 0.003 && Math.abs(way[1] - back2[1]) < 0.003, "the way survives the packet at " + deg);
        }
    }

    /** 2.4.5: Thy Death: each tail's lunge, out and back, its point through the foe at the hit, its root never moving. */
    static void thyDeath() {
        check(com.hex.kagune.control.ThyDeath.reach(0) == 0.0 && com.hex.kagune.control.ThyDeath.reach(com.hex.kagune.control.ThyDeath.HIT_AT) == 1.0
            && com.hex.kagune.control.ThyDeath.reach(com.hex.kagune.control.ThyDeath.STRIKE) == 0.0
            && com.hex.kagune.control.ThyDeath.reach(3) > 0.0 && com.hex.kagune.control.ThyDeath.reach(9) > 0.0, "a lunge: out, through, back");
        double[][] rest = new double[12][];
        for (int i = 0; i < rest.length; i++) {
            rest[i] = new double[] {0.3, 1.0 + i * 0.1, -0.3 - i * 0.05};
        }
        double[] foe = {0.0, 1.2, 4.0};
        double[][] at = com.hex.kagune.control.ThyDeath.strike(rest, foe, com.hex.kagune.control.ThyDeath.HIT_AT, 1.0, -1.0, 0.0, 0.9);
        double[] tip = at[at.length - 1];
        check(java.util.Arrays.equals(at[0], rest[0]) && Math.abs(tip[0] - foe[0]) < 1e-9 && Math.abs(tip[1] - foe[1]) < 1e-9 && Math.abs(tip[2] - foe[2]) < 1e-9,
            "at the hit its point is through the foe, its root where it was");
        double[][] none = com.hex.kagune.control.ThyDeath.strike(rest, foe, 0.0, 1.0, -1.0, 0.0, 0.9);
        check(java.util.Arrays.deepEquals(none, rest), "before it lunges, the tail as it was");
        check(com.hex.kagune.control.ThyDeath.range(1000) > com.hex.kagune.control.ThyDeath.range(0) && com.hex.kagune.control.ThyDeath.damage(1000) == 10.0F
            && com.hex.kagune.control.ThyDeath.first(100L, 3) > com.hex.kagune.control.ThyDeath.first(100L, 0), "reach and blows grow; tails staggered");
    }

    /** 3.1.5: the Impale: its reach and pace, a tap and a hold, its point, what it carries, its tail, its sharpened end, its key. */
    static void impale() {
        check(RinkakuImpale.reach(0) == 16.0 && RinkakuImpale.reach(1000) == 30.0 && RinkakuImpale.reach(500) > RinkakuImpale.reach(0)
            && RinkakuImpale.reach(5000) == 30.0, "it reaches 16 at mastery 0, 30 at 1000");
        check(RinkakuImpale.reach(0) > 8.5 + 0.0065 * 1000 && RinkakuImpale.reach(0) > 15.0, "even at mastery 0 it reaches past any grab or strike");
        check(RinkakuUnlocks.IMPALE == RinkakuUnlocks.CRUSH, "the Impale comes with the crush's key");
        // A tap: held one tick, then let go: it still runs out a tap's length, then stays.
        double tap = RinkakuImpale.step(0.0, true, 30.0);
        for (int i = 0; i < 20; i++) {
            tap = RinkakuImpale.step(tap, false, 30.0);
        }
        check(tap >= RinkakuImpale.TAP && tap < RinkakuImpale.TAP + RinkakuImpale.SPEED, "a tap still runs it out about " + RinkakuImpale.TAP + ": " + tap);
        check(RinkakuImpale.spent(tap, false, 30.0) && !RinkakuImpale.spent(tap, true, 30.0), "let go past a tap it is spent; held, it is not");
        // Held: the longer, the further, never past its reach.
        double held = 0.0;
        boolean growing = true;
        for (int t = 1; t <= 40; t++) {
            double next = RinkakuImpale.step(held, true, 24.0);
            growing &= next >= held && next <= 24.0;
            held = next;
            if (t == 5) {
                check(Math.abs(held - 5 * RinkakuImpale.SPEED) < 1e-9, "held five ticks: five steps out");
            }
        }
        check(growing && held == 24.0 && RinkakuImpale.spent(held, true, 24.0), "held long enough it reaches its reach and stops there");
        check(24.0 / RinkakuImpale.SPEED < 15.0, "fast: its whole reach in under 15 ticks");
        check(RinkakuImpale.back(0) == 1.0 && RinkakuImpale.back(RinkakuImpale.RETRACT) == 0.0 && RinkakuImpale.back(2) > RinkakuImpale.back(4),
            "it comes back smoothly, all the way");
        // What it carries: the newest nearest its point, the first further up its shaft, never past its point or into him.
        for (int count = 1; count <= RinkakuImpale.MOST_CARRIED; count++) {
            double last = -1.0;
            boolean order = true;
            for (int k = 0; k < count; k++) {
                double at = RinkakuImpale.slot(k, count, 12.0);
                order &= at > last && at <= 12.0 && at >= RinkakuImpale.NEAREST;
                last = at;
            }
            check(order && Math.abs(RinkakuImpale.slot(count - 1, count, 12.0) - (12.0 - RinkakuImpale.TIP_BACK)) < 1e-9,
                count + " carried: in order up the shaft, the newest by its point");
        }
        check(RinkakuImpale.slot(0, 5, 2.0) <= 2.0 && RinkakuImpale.slot(0, 5, 2.0) >= 0.0, "a short spear never carries past its own point");
        // Which tail: only a free one (never a centipede leg), each free one in its turn.
        boolean[] free = {false, true, false, true};
        java.util.Set<Integer> picked = new java.util.HashSet<>();
        for (int i = 0; i < 100; i++) {
            int limb = RinkakuImpale.pick(free, i / 100.0);
            check(limb == 1 || limb == 3, "only a free tail goes: " + limb);
            picked.add(limb);
        }
        check(picked.size() == 2 && RinkakuImpale.pick(new boolean[] {false, false}, 0.5) == -1 && RinkakuImpale.pick(new boolean[0], 0.3) == -1
            && RinkakuImpale.pick(free, 0.99999) == 3, "every free tail can go; none free, none goes");
        // It follows his look like a heavy spear.
        double[] turned = RinkakuImpale.turn(new double[] {0, 0, 1}, new double[] {1, 0, 0}, RinkakuImpale.TURN);
        double angle = Math.toDegrees(Math.acos(turned[2]));
        check(Math.abs(angle - RinkakuImpale.TURN) < 1e-6 && Math.abs(Math.sqrt(turned[0] * turned[0] + turned[1] * turned[1] + turned[2] * turned[2]) - 1.0) < 1e-9,
            "it turns at most " + RinkakuImpale.TURN + " degrees a tick");
        double[] near = RinkakuImpale.turn(new double[] {0, 0, 1}, new double[] {0.05, 0, 1}, RinkakuImpale.TURN);
        check(Math.abs(near[0] - 0.05 / Math.sqrt(1.0025)) < 1e-9, "a small turn it takes at once");
        // Its point runs through what is in its way, nothing beside it, under it or over it.
        double[] a = {0.0, 1.5, 0.0};
        double[] b = {0.0, 1.5, 6.0};
        check(RinkakuImpale.through(a, b, 0.0, 0.5, 2.3, 3.0, 0.3), "a body in its line is run through");
        check(RinkakuImpale.through(a, b, 0.5, 0.5, 2.3, 3.0, 0.3), "a body half a block off its line too (its width)");
        check(!RinkakuImpale.through(a, b, 2.0, 0.5, 2.3, 3.0, 0.3) && !RinkakuImpale.through(a, b, 0.0, 2.5, 4.0, 3.0, 0.3)
            && !RinkakuImpale.through(a, b, 0.0, 0.5, 2.3, 9.0, 0.3), "nothing to the side, overhead or past its point");
        // The tail as drawn: its root where it was, up over its own side's shoulder, then dead straight along its line to its point.
        double[] feet = {10.0, 64.0, -3.0};
        for (double side : new double[] {-1.0, 1.0}) {
            double[] launch = RinkakuImpale.launch(feet, 1.8, side, -1.0, 0.0, 0.0, 1.0, false);
            check(Math.signum(-(launch[0] - feet[0])) == Math.signum(side) && launch[1] > feet[1] + 1.5, "it leaves over its own side's shoulder");
            double[] own = RinkakuImpale.launch(feet, 1.8, side, -1.0, 0.0, 0.0, 1.0, true);
            check(own[1] < launch[1], "in his own first person it leaves lower, out of his eyes");
            double[] root = {10.0 + 0.2 * side, 64.9, -3.25};
            double[] way = {0.0, 0.0, 1.0};
            double[][] spear = RinkakuImpale.spear(root, launch, way, 14.0, 24, side, -1.0, 0.0, 0.0, 1.0);
            double[] tip = spear[spear.length - 1];
            check(spear.length == 24 && java.util.Arrays.equals(spear[0], root), "as many points as the tail, its root kept");
            check(Math.abs(tip[0] - launch[0]) < 1e-9 && Math.abs(tip[1] - launch[1]) < 1e-9 && Math.abs(tip[2] - (launch[2] + 14.0)) < 1e-9,
                "its point exactly where the server's is: the launch, 14 along its line");
            boolean straight = true;
            boolean finite = true;
            for (double[] q : spear) {
                finite &= Double.isFinite(q[0]) && Double.isFinite(q[1]) && Double.isFinite(q[2]);
                if (q[2] > launch[2] + 0.01) {
                    straight &= Math.abs(q[0] - launch[0]) < 1e-9 && Math.abs(q[1] - launch[1]) < 1e-9;
                }
            }
            check(straight && finite, "past the shoulder it is one straight line");
            double[][] none = RinkakuImpale.blend(spear, spear, 0.0);
            double[][] rest = new double[24][];
            for (int i = 0; i < 24; i++) {
                rest[i] = new double[] {root[0], root[1] + i * 0.05, root[2] - i * 0.04};
            }
            check(java.util.Arrays.deepEquals(RinkakuImpale.blend(rest, spear, 0.0), rest) && java.util.Arrays.deepEquals(none, spear)
                && Math.abs(RinkakuImpale.blend(rest, spear, 1.0)[23][2] - tip[2]) < 1e-9, "blended in and out of its own motion");
        }
        double[] home = RinkakuImpale.spear(new double[] {0, 1, 0}, new double[] {0.5, 1.7, 0.2}, new double[] {0, 0, 1}, 0.0, 12, 1.0, -1, 0, 0, 1)[11];
        check(Math.abs(home[0] - 0.5) < 1e-9 && Math.abs(home[2] - 0.2) < 1e-9, "come home, its end at the shoulder");
        // Its sharpened end: drawn to a point; the leaf unchanged when it isn't impaling.
        boolean same = true;
        for (double t = 0.0; t <= 1.0; t += 0.05) {
            same &= RinkakuRibbon.width(t, 0.0, 9.0) == RinkakuRibbon.profile(t);
        }
        check(same, "not impaling, the tail is the leaf it always was");
        check(RinkakuImpale.spearWidth(1.0, 20.0, RinkakuRibbon.PEAK, RinkakuRibbon.ROOT_WIDTH) == 0.0
            && RinkakuImpale.spearWidth(0.0, 20.0, RinkakuRibbon.PEAK, RinkakuRibbon.ROOT_WIDTH) < RinkakuRibbon.PEAK * 0.3, "thin at its root, a point at its end");
        double tipStart = 1.0 - RinkakuImpale.POINT_LENGTH / 20.0;
        boolean narrowing = true;
        for (double t = tipStart + 0.005; t <= 1.0; t += 0.005) {
            narrowing &= RinkakuImpale.spearWidth(t, 20.0, RinkakuRibbon.PEAK, RinkakuRibbon.ROOT_WIDTH)
                < RinkakuImpale.spearWidth(t - 0.005, 20.0, RinkakuRibbon.PEAK, RinkakuRibbon.ROOT_WIDTH);
        }
        check(narrowing, "over its last " + RinkakuImpale.POINT_LENGTH + " blocks it narrows all the way to its point");
        check(RinkakuImpale.spearWidth(0.5, 20.0, RinkakuRibbon.PEAK, RinkakuRibbon.ROOT_WIDTH) == RinkakuRibbon.PEAK,
            "stretched out, its shaft is its full width");
        double[][] line = new double[16][];
        for (int i = 0; i < 16; i++) {
            line[i] = new double[] {0.0, 1.5, i * 1.2};
        }
        RinkakuRibbon.Mesh leaf = RinkakuRibbon.build(line, 0.2, KaguneColours.DEFAULT, 7, new double[] {3, 3, 3}, 40.0);
        RinkakuRibbon.Mesh leaf0 = RinkakuRibbon.build(line, 0.2, KaguneColours.DEFAULT, 7, new double[] {3, 3, 3}, 40.0, 0.0);
        RinkakuRibbon.Mesh sharp = RinkakuRibbon.build(line, 0.2, KaguneColours.DEFAULT, 7, new double[] {3, 3, 3}, 40.0, 1.0);
        check(java.util.Arrays.equals(leaf.half(), leaf0.half()) && java.util.Arrays.equals(leaf.along(), leaf0.along()), "sharp 0 builds the very same tail");
        double drawn = sharp.along()[sharp.along().length - 1];
        double[] end = sharp.centre()[sharp.centre().length - 1];
        check(Math.abs(drawn - 18.0) < 1e-6 && Math.abs(end[2] - 18.0) < 1e-6, "impaling, it is drawn exactly as long as it goes (its point where it strikes)");
        check(sharp.half().length > leaf.half().length && sharp.half()[sharp.half().length - 1] < 1e-9
            && sharp.half()[sharp.half().length / 2] > leaf.half()[0], "more rings for its length, its end a point, its shaft broad");
        // Its key: H, every form whose tails are his; the first kakuja's legs fight on their own.
        java.util.Map<AbilityGuide.Key, String> keys = defaultKeys();
        for (com.hex.kagune.gameplay.KaguneForm form : new com.hex.kagune.gameplay.KaguneForm[] {com.hex.kagune.gameplay.KaguneForm.RINKAKU_SEED,
            com.hex.kagune.gameplay.KaguneForm.RINKAKU_DUAL, com.hex.kagune.gameplay.KaguneForm.RINKAKU_QUAD, com.hex.kagune.gameplay.KaguneForm.FULL_KAKUJA}) {
            AbilityGuide.Panel panel = AbilityGuide.panel(new AbilityGuide.Input(KagunePath.RINKAKU, AbilityGuide.State.OUT, 800, form,
                com.hex.kagune.control.ukaku.WingForm.TWIN, false, keys));
            // 3.3.0: its own row (H held) with nothing held; the crush's row with someone held.
            AbilityGuide.Panel holding = AbilityGuide.panel(new AbilityGuide.Input(KagunePath.RINKAKU, AbilityGuide.State.OUT, 800, form,
                com.hex.kagune.control.ukaku.WingForm.TWIN, false, keys, false, com.hex.kagune.control.graft.GraftStage.TRANSPLANT, false, java.util.Set.of(), true));
            check(find(panel, "Impale") != null && find(panel, "Impale").keys().equals("H hold") && find(panel, "Impale").needs() == 0
                && find(holding, "Crush them").keys().equals("H"), form + ": H held is the Impale, and the crush of what a tail holds");
        }
        AbilityGuide.Panel low = AbilityGuide.panel(new AbilityGuide.Input(KagunePath.RINKAKU, AbilityGuide.State.OUT, 300,
            com.hex.kagune.gameplay.KaguneForm.RINKAKU_DUAL, com.hex.kagune.control.ukaku.WingForm.TWIN, false, keys));
        check(find(low, "Impale").needs() == RinkakuUnlocks.IMPALE, "below its mastery the Impale shows when it comes");
        AbilityGuide.Panel first = AbilityGuide.panel(new AbilityGuide.Input(KagunePath.RINKAKU, AbilityGuide.State.OUT, 800,
            com.hex.kagune.gameplay.KaguneForm.CENTIPEDE, com.hex.kagune.control.ukaku.WingForm.TWIN, false, keys));
        check(find(first, "Impale") == null, "the first kakuja lists no Impale (its legs fight on their own)");
    }

    /** 3.1.6: the Rubble Hurl: its charge and size, its throw landing where he aims, its burst, its rubble, its key. */
    static void hurl() {
        check(RinkakuHurl.charge(0) == 0.0 && RinkakuHurl.charge(RinkakuHurl.CHARGE) == 1.0 && RinkakuHurl.charge(1000) == 1.0
            && RinkakuHurl.charge(RinkakuHurl.CHARGE / 2.0) == 0.5, "held, it grows to its biggest over " + RinkakuHurl.CHARGE + " ticks");
        check(RinkakuHurl.size(0.0, 1) == 0.85 && Math.abs(RinkakuHurl.size(1.0, 1) - 2.0) < 1e-9 && RinkakuHurl.size(1.0, 3) < RinkakuHurl.size(1.0, 1),
            "from a block's worth to two across; in pieces each smaller");
        check(RinkakuHurl.speed(1.0) < RinkakuHurl.speed(0.0) && RinkakuHurl.speed(1.0) > 1.0, "a big one a little slower, still fast");
        check(RinkakuHurl.radius(2.0) > RinkakuHurl.radius(0.85) && RinkakuHurl.power(1.0) == 2.0 && RinkakuHurl.power(0.0) == 1.0
            && RinkakuHurl.knock(1.0) > RinkakuHurl.knock(0.0) && RinkakuHurl.lift(1.0) > RinkakuHurl.lift(0.0)
            && RinkakuHurl.stun(1.0) > RinkakuHurl.stun(0.0) && RinkakuHurl.stun(0.0) > 0, "charged: a wider, harder burst, a longer stun");
        check(RinkakuHurl.falloff(0.0, 3.0) == 1.0 && Math.abs(RinkakuHurl.falloff(3.0, 3.0) - 0.4) < 1e-9 && RinkakuHurl.falloff(3.01, 3.0) == 0.0
            && RinkakuHurl.falloff(1.0, 3.0) > RinkakuHurl.falloff(2.0, 3.0) && RinkakuHurl.falloff(Double.NaN, 3.0) == 0.0,
            "all of it at its middle, 40% at its edge, nothing past");
        check(RinkakuUnlocks.HURL == RinkakuUnlocks.GRAB, "the Rubble Hurl comes with the throw's key");
        // Where it hangs: before and above his eyes, higher and further out the bigger it is.
        double[] eye = {5.0, 70.62, -2.0};
        double[] small = RinkakuHurl.hover(eye, 0.0, 1.0, 0.85);
        double[] big = RinkakuHurl.hover(eye, 0.0, 1.0, 2.0);
        check(small[1] > eye[1] && small[2] > eye[2] && Math.abs(small[0] - eye[0]) < 1e-9 && big[1] > small[1] && big[2] > small[2],
            "it hangs before and above his eyes, a bigger one higher and further");
        double[] ground = {7.5, 69.5, 1.5};
        double[] up = RinkakuHurl.rising(ground, big, RinkakuHurl.RISE);
        double[] start = RinkakuHurl.rising(ground, big, 0.0);
        double[] mid = RinkakuHurl.rising(ground, big, RinkakuHurl.RISE / 2.0);
        check(java.util.Arrays.equals(start, ground) && Math.abs(up[0] - big[0]) < 1e-9 && Math.abs(up[1] - big[1]) < 1e-9
            && Math.abs(up[2] - big[2]) < 1e-9 && mid[1] > (ground[1] + big[1]) / 2.0, "it rises out of the ground into their grip, arcing up");
        // Thrown, it lands where he aims: near, far, up, down.
        double[][] targets = {{5.0, 70.0, 10.0}, {5.0, 64.0, 30.0}, {-12.0, 75.0, 20.0}, {5.0, 60.0, 4.0}, {30.0, 70.0, 30.0}};
        for (double[] target : targets) {
            for (double charge : new double[] {0.0, 1.0}) {
                double[] pos = big.clone();
                double[] vel = RinkakuHurl.aim(pos, target, RinkakuHurl.speed(charge));
                double closest = Double.MAX_VALUE;
                for (int t = 0; t < RinkakuHurl.FLIGHT; t++) {
                    RinkakuHurl.step(pos, vel);
                    double dx = pos[0] - target[0];
                    double dy = pos[1] - target[1];
                    double dz = pos[2] - target[2];
                    closest = Math.min(closest, Math.sqrt(dx * dx + dy * dy + dz * dz));
                }
                check(closest < 1e-6, "thrown at " + java.util.Arrays.toString(target) + " (charge " + charge + ") it lands on the mark: " + closest);
            }
        }
        double[] level = RinkakuHurl.aim(new double[] {0, 0, 0}, new double[] {0, 0, 20}, 1.9);
        check(Math.abs(level[2] - 1.9) < 0.1 && level[1] > 0.0, "thrown level, about as fast as asked, lobbed a little to make up for its fall");
        double[] turned = RinkakuHurl.spread(new double[] {0.0, 0.3, 1.5}, RinkakuHurl.SPREAD);
        check(Math.abs(Math.hypot(turned[0], turned[2]) - 1.5) < 1e-9 && turned[1] == 0.3 && turned[0] < 0.0
            && Math.abs(Math.toDegrees(Math.atan2(-turned[0], turned[2])) - RinkakuHurl.SPREAD) < 1e-6, "the kakuja's pieces fan out, as fast");
        // What it meets on the way.
        check(RinkakuHurl.meets(new double[] {0, 1, 0}, new double[] {0, 1, 2}, 0.0, 0.0, 1.8, 1.5, 0.3, 0.6), "it meets a body in its way");
        check(RinkakuHurl.meets(new double[] {0, 1, 0}, new double[] {0, 1, 2}, 1.0, 0.0, 1.8, 1.5, 0.3, 0.9), "a big one meets a body beside its line");
        check(!RinkakuHurl.meets(new double[] {0, 1, 0}, new double[] {0, 1, 2}, 2.0, 0.0, 1.8, 1.5, 0.3, 0.4)
            && !RinkakuHurl.meets(new double[] {0, 4, 0}, new double[] {0, 4, 2}, 0.0, 0.0, 1.8, 1.5, 0.3, 0.4), "nothing beside or under it");
        // The rubble as drawn: the same every frame, its core biggest, its lumps stuck close round it.
        double[][] lumps = RinkakuHurl.pieces(77, 6);
        check(java.util.Arrays.deepEquals(lumps, RinkakuHurl.pieces(77, 6)) && lumps.length == 6 && lumps[0][3] > 0.6, "the same rubble every frame, a big core");
        boolean close = true;
        for (int i = 1; i < lumps.length; i++) {
            double r = Math.sqrt(lumps[i][0] * lumps[i][0] + lumps[i][1] * lumps[i][1] + lumps[i][2] * lumps[i][2]);
            close &= r <= 0.35 && lumps[i][3] < lumps[0][3] && lumps[i][3] > 0.3 && Double.isFinite(lumps[i][4]);
        }
        check(close && RinkakuHurl.lumps(2.0) > RinkakuHurl.lumps(0.85) && RinkakuHurl.lumps(0.85) == 4, "smaller lumps stuck round it, more the bigger");
        // Its key: N, every form whose tails are his; the first kakuja's legs fight on their own.
        java.util.Map<AbilityGuide.Key, String> keys = defaultKeys();
        for (com.hex.kagune.gameplay.KaguneForm form : new com.hex.kagune.gameplay.KaguneForm[] {com.hex.kagune.gameplay.KaguneForm.RINKAKU_SEED,
            com.hex.kagune.gameplay.KaguneForm.RINKAKU_QUAD, com.hex.kagune.gameplay.KaguneForm.FULL_KAKUJA}) {
            AbilityGuide.Panel panel = AbilityGuide.panel(new AbilityGuide.Input(KagunePath.RINKAKU, AbilityGuide.State.OUT, 800, form,
                com.hex.kagune.control.ukaku.WingForm.TWIN, false, keys));
            // 3.3.0: its own row (N held) with nothing held; the throw's row with someone held.
            AbilityGuide.Panel held = AbilityGuide.panel(new AbilityGuide.Input(KagunePath.RINKAKU, AbilityGuide.State.OUT, 800, form,
                com.hex.kagune.control.ukaku.WingForm.TWIN, false, keys, false, com.hex.kagune.control.graft.GraftStage.TRANSPLANT, false, java.util.Set.of(), true));
            AbilityGuide.Row row = find(panel, "Rubble Hurl");
            check(row != null && row.keys().equals("N hold") && find(held, "Throw them").keys().equals("N"),
                form + ": N is the throw, or with nothing held the Rubble Hurl");
        }
    }

    /**
     * 3.1.7 (the owner, with the 3.1.6 preview: "the tails are going through the body, bro"): every tail gripping the Rubble Hurl's
     * chunk, as it comes up out of the ground (before him, to either side, further, lower), held at every charge, and whipping after it
     * thrown, for every tail, seen by others and in his own first person: the ribbon, at its own breadth along it (with room for its swell),
     * never meets his body (BodyClearance's torso, arms and head) once it has left his back; its end exactly on its grip.
     */
    static void hurlGrip() {
        double rx = -1.0, rz = 0.0, fx = 0.0, fz = 1.0; // facing +z: his right is -x
        double[] feet = {0.0, 0.0, 0.0};
        double[] eye = {0.0, 1.62, 0.0};
        double worst = Double.MAX_VALUE;
        String where = "";
        int paths = 0;
        for (int limb = 0; limb < 4; limb++) {
            for (boolean own : new boolean[] {false, true}) {
                double side = RinkakuRoot.side(limb, 0.0);
                double[] r = RinkakuRoot.root(limb, side, 1.8);
                double[] root = {-r[0], r[1], r[2]};
                double s = side >= 0.0 ? 1.0 : -1.0;
                for (double charge = 0.0; charge <= 1.0001; charge += 0.25) {
                    double size = RinkakuHurl.size(charge, 1);
                    double[] hover = RinkakuHurl.hover(eye, 0.0, 1.0, size);
                    java.util.List<double[]> targets = new java.util.ArrayList<>();
                    for (double[] source : new double[][] {{0, -0.5, 1.8}, {0, -0.5, 1.0}, {0, -0.5, 2.6}, {2, -0.5, 1.8}, {-2, -0.5, 1.8},
                        {0, 1.0, 6.5}, {0, -1.5, 3}, {1, 2.5, 4}}) {
                        for (double age = 0.0; age <= RinkakuHurl.RISE; age += 0.5) {
                            targets.add(RinkakuHurl.rising(source, hover, age));
                        }
                    }
                    double[] pos = hover.clone();
                    double[] vel = RinkakuHurl.aim(pos, new double[] {0, 1, 20}, RinkakuHurl.speed(charge));
                    for (int t = 0; t < 6; t++) {
                        RinkakuHurl.step(pos, vel);
                        double dx = pos[0] - root[0], dy = pos[1] - root[1], dz = pos[2] - root[2];
                        double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
                        double k = d > 4.5 ? 4.5 / d : 1.0;
                        targets.add(new double[] {root[0] + dx * k, root[1] + dy * k, root[2] + dz * k});
                    }
                    for (double[] t : targets) {
                        double[] grip = {t[0] + rx * s * size * 0.3, t[1], t[2]};
                        double[][] path = RinkakuHurl.grip(root, feet, 1.8, side, rx, rz, fx, fz, grip, 46, own);
                        paths++;
                        double[] end = path[path.length - 1];
                        check(java.util.Arrays.equals(path[0], root) && Math.abs(end[0] - grip[0]) + Math.abs(end[1] - grip[1]) + Math.abs(end[2] - grip[2]) < 1e-9,
                            "a gripping tail leaves his back and ends on its grip");
                        double[] arcs = new double[path.length];
                        for (int i = 1; i < path.length; i++) {
                            arcs[i] = arcs[i - 1] + Math.sqrt(Math.pow(path[i][0] - path[i - 1][0], 2) + Math.pow(path[i][1] - path[i - 1][1], 2)
                                + Math.pow(path[i][2] - path[i - 1][2], 2));
                        }
                        for (int i = 1; i < path.length; i++) {
                            if (arcs[i] < BodyClearance.ROOT_EASE) {
                                continue; // leaving his back
                            }
                            double half = 0.125 * RinkakuRibbon.profile(arcs[i] / arcs[path.length - 1]) * RinkakuRibbon.WIDE * 1.3;
                            double gap = BodyClearance.distance(path[i][0], path[i][1], path[i][2], 0.6, 1.8) - half;
                            if (gap < worst) {
                                worst = gap;
                                where = "tail " + limb + (own ? " (first person)" : "") + " charge " + charge + " grip " + java.util.Arrays.toString(grip);
                            }
                        }
                    }
                }
            }
        }
        check(worst > 0.0, "over " + paths + " grips the ribbon never meets his body (closest " + worst + ": " + where + ")");
    }

    static void rinkakuAlive() {
        // 2.1.6: each tail's side as the original lays it out (TendrilChain: its fanSide along its owner's LEFT; with none, an even tail on
        // his right), so its rest, root and guard are on the side its own motion carries it (else each blend swings it across his back).
        check(RinkakuRoot.side(0, 1.0) < 0.0 && RinkakuRoot.side(1, -0.6) > 0.0 && RinkakuRoot.side(0, 0.0) > 0.0 && RinkakuRoot.side(1, 0.0) < 0.0,
            "a tail's side as the original has it: fanSide > 0 on his left");
        for (int limb = 0; limb < 4; limb++) {
            double fan = limb % 2 == 0 ? -0.8 : 0.8;
            double[][] rest = RinkakuIdle.pose(limb, 4, fan, 12, 1.8, 3.4, Double.NaN, 7, 0.0, 0.0, 0.0);
            double originalX = -fan; // the original's left (x right in the owner's frame) times its fanSide
            check(Math.signum(rest[rest.length - 1][0]) == Math.signum(originalX) && Math.signum(RinkakuRoot.root(limb, RinkakuRoot.side(limb, fan), 1.8)[0])
                == Math.signum(originalX), "tail " + limb + " rests on the side the original lays it out on");
        }

        // 2.3.2: a strike passing his body goes round the tail's own side (BodyClearance's +1 is his left: (forward.z, 0, -forward.x)).
        // Facing +z his left is +x: a tail laid out on his left (fanSide > 0) striking straight through him is carried round at +x.
        for (int limb = 0; limb < 4; limb++) {
            for (double fan : new double[] {0.8, -0.8, 0.0}) {
                double[][] strike = new double[12][];
                for (int i = 0; i < strike.length; i++) {
                    strike[i] = new double[] {0.0, 1.1, -0.45 + i * 0.18};
                }
                boolean moved = com.hex.kagune.control.BodyClearance.clear(strike, new double[] {0.0, 0.0, 0.0}, 0.0, 1.0, 0.6, 1.8, 0.12,
                    RinkakuRoot.clearance(limb, fan));
                double across = 0.0;
                for (double[] p : strike) {
                    across += p[0];
                }
                double ownSide = -RinkakuRoot.side(limb, fan); // his right is -x facing +z
                check(moved && Math.signum(across) == Math.signum(ownSide), "tail " + limb + " (fan " + fan + ") strikes round its own side, not across him");
            }
        }

        double height = 1.8;
        for (int tails = 1; tails <= 6; tails++) {
            for (int limb = 0; limb < tails; limb++) {
                double side = RinkakuRoot.side(limb, 0.0);
                double[][] rest = RinkakuIdle.pose(limb, tails, 0.0, 30, height, 4.6, 100.0, 77 + limb, 0.0, 0.0, 0.0);
                String name = tails + " tails, tail " + limb;
                check(rest.length == 30 && java.util.Arrays.equals(rest[0], RinkakuRoot.root(limb, side, height)), name + ": from its root");
                boolean finite = true;
                double most = 0.0;
                double least = 1.0E9;
                double arc = 0.0;
                for (int i = 1; i < rest.length; i++) {
                    finite &= Double.isFinite(rest[i][0]) && Double.isFinite(rest[i][1]) && Double.isFinite(rest[i][2]);
                    double step = RinkakuRoot.distance(rest[i], rest[i - 1]);
                    arc += step;
                    most = Math.max(most, step);
                    least = Math.min(least, step);
                }
                check(finite && most - least < 0.03, name + ": smooth and evenly spaced");
                check(Math.signum(rest[29][0]) == side && Math.abs(rest[29][0]) > 0.6, name + ": out to its own side");
                check(rest[29][2] < 0.6, name + ": never out in front of him at rest");
                check(arc > 3.6, name + ": its whole length, not coiled up short (" + arc + ")");
            }
        }
        double[][] upper = RinkakuIdle.pose(0, 4, 0.0, 30, height, 4.6, 0.0, 1, 0.0, 0.0, 0.0);
        double[][] lower = RinkakuIdle.pose(2, 4, 0.0, 30, height, 4.6, 0.0, 1, 0.0, 0.0, 0.0);
        check(upper[29][1] > lower[29][1] + 0.4 && Math.abs(lower[29][0]) > Math.abs(upper[29][0]) + 0.6, "the first pair arcs up high, the second fans out wide");
        double[][] a = RinkakuIdle.pose(0, 2, 0.0, 30, height, 4.6, 0.0, 5, 0.0, 0.0, 0.0);
        double[][] b = RinkakuIdle.pose(0, 2, 0.0, 30, height, 4.6, 21.0, 5, 0.0, 0.0, 0.0);
        double swayed = RinkakuRoot.distance(a[29], b[29]);
        check(swayed > 0.05 && swayed < 0.8 && RinkakuRoot.distance(a[1], b[1]) < 0.05, "it sways, its end more than its root (" + swayed + ")");
        double[][] still = RinkakuIdle.pose(0, 2, 0.0, 30, height, 4.6, Double.NaN, 5, 0.0, 0.0, 0.0);
        double[][] running = RinkakuIdle.pose(0, 2, 0.0, 30, height, 4.6, Double.NaN, 5, RinkakuIdle.FULL_RUN, 0.0, 0.0);
        check(running[29][2] < still[29][2] - 0.8 && running[29][1] < still[29][1], "running, it trails back and down");
        double[][] turning = RinkakuIdle.pose(1, 2, 0.0, 30, height, 4.6, Double.NaN, 5, 0.0, 0.0, 0.2);
        double[][] straight = RinkakuIdle.pose(1, 2, 0.0, 30, height, 4.6, Double.NaN, 5, 0.0, 0.0, 0.0);
        check(turning[29][0] > straight[29][0] + 0.3, "turning to his left, it lags out to his right");
        double[][] longer = RinkakuIdle.pose(0, 2, 0.0, 30, height, 7.0, Double.NaN, 5, 0.0, 0.0, 0.0);
        double rootY = RinkakuRoot.root(0, RinkakuRoot.side(0, 0.0), height)[1];
        check(Math.abs(longer[29][0]) > Math.abs(still[29][0]) + 0.3 && (longer[29][1] - rootY) / (still[29][1] - rootY) < 7.0 / 4.6,
            "a longer tail reaches further, rising less than it lengthens");
        double[][] huge = RinkakuIdle.pose(0, 2, 0.0, 30, height, 12.0, Double.NaN, 5, 0.0, 0.0, 0.0);
        check(huge[29][2] < 0.4, "even a very long one doesn't come forward over him (" + huge[29][2] + ")");
        check(RinkakuIdle.settle(0.0, true, 1.0) > 0.0 && RinkakuIdle.settle(0.0, true, 1.0) < 0.3, "it settles into its rest over a few ticks");
        check(RinkakuIdle.settle(1.0, false, 3.0) == 0.0, "and leaves it at once for a move");
        check(RinkakuIdle.settle(0.5, true, Double.NaN) == 0.5 && RinkakuIdle.settle(Double.NaN, true, 1.0) > 0.0, "odd numbers change nothing wild");
        // Alive: its length and its shape.
        double shortest = 9.0;
        double longest = 0.0;
        for (double t = 0.0; t < 400.0; t += 0.5) {
            double s = RinkakuRibbon.stretch(t, 9);
            shortest = Math.min(shortest, s);
            longest = Math.max(longest, s);
        }
        check(shortest < 0.92 && longest > 1.08 && shortest > 0.85 && longest < 1.15, "it stretches and draws in by about a tenth (" + shortest + ".." + longest + ")");
        check(RinkakuRibbon.stretch(Double.NaN, 9) == 1.0 && RinkakuRibbon.swell(0.5, Double.NaN, 9) == 1.0, "still when the time isn't known");
        check(RinkakuRibbon.stretch(10.0, 1) != RinkakuRibbon.stretch(10.0, 2), "each tail out of step with the others");
        double[] peakAt = new double[2];
        for (int j = 0; j < 2; j++) {
            double best = 0.0;
            for (double u = 0.0; u <= 1.0; u += 0.01) {
                double w = RinkakuRibbon.swell(u, 30.0 + j * 8.0, 0) / RinkakuRibbon.swell(u, Double.NaN, 0);
                double breath = 1.0 + RinkakuRibbon.BREATH * Math.sin(2.0 * Math.PI * (30.0 + j * 8.0) / RinkakuRibbon.BREATH_PERIOD) * Math.sin(Math.PI * u);
                if (w / breath > best) {
                    best = w / breath;
                    peakAt[j] = u;
                }
            }
        }
        check(peakAt[1] > peakAt[0], "a swell runs out along it from the root (" + peakAt[0] + " then " + peakAt[1] + ")");
        double fattest = 0.0;
        for (double t = 0.0; t < 120.0; t += 1.0) {
            fattest = Math.max(fattest, RinkakuRibbon.swell(0.5, t, 3));
        }
        check(fattest > 1.2 && fattest < 1.6, "its middle swells thicker now and then (" + fattest + ")");
        double[][] line = new double[20][];
        for (int i = 0; i < 20; i++) {
            line[i] = new double[] {0.15 * i, 1.0, 0.0};
        }
        RinkakuRibbon.Mesh grown = null;
        for (double t = 0.0; t < 80.0 && grown == null; t += 1.0) {
            if (RinkakuRibbon.stretch(t, 4) > 1.05) {
                grown = RinkakuRibbon.build(line, 0.15, KaguneColours.DEFAULT, 4, new double[] {0.0, 5.0, 5.0}, t);
            }
        }
        check(grown != null && grown.centre()[grown.centre().length - 1][0] > 2.85 + 0.05, "stretched past its last point, it goes on along its way");
        // One skin: the scales baked in, its colour brightened back.
        check(RinkakuRibbon.lifted(0xFF000000 | RinkakuRibbon.SKIN_BASE << 16) == 0xFFFF0000 && RinkakuRibbon.lifted(0x80FFFFFF) == 0x80FFFFFF
            && RinkakuRibbon.lifted(0xFF000000) == 0xFF000000, "the skin's base grey gives back the colour itself");
    }

    /** 2.1.0: a kakuja's centipede legs and mask in the kagune's colours: grey plates, its colour in the flesh between them. */
    static void kakujaPalette() {
        for (int colour : new int[] {KaguneColours.DEFAULT, 0x1E50D8, 0x7A1FB8, 0x1F9E4A, 0x141414, 0xE8E8E8, 0xFFD400, 0xFF6A00}) {
            KakujaPalette.Palette p = KakujaPalette.of(colour);
            String name = Integer.toHexString(colour);
            boolean grey = true;
            for (int plate : new int[] {p.shadow(), p.dark(), p.plate(), p.hi(), p.streak(), p.rim(), p.rimShade(), p.gloss(), p.claw()}) {
                grey &= KakujaPalette.chroma(plate) < 0.08;
            }
            check(grey, name + ": the plates are greys, black and white, never its colour");
            double[] kagune = KaguneColours.toHsv(colour);
            double[] flesh = KaguneColours.toHsv(p.flesh());
            if (kagune[1] > 0.4 && kagune[2] > 0.35) {
                double dh = Math.abs(flesh[0] - kagune[0]);
                check(Math.min(dh, 1.0 - dh) < 0.06 && flesh[1] > 0.4, name + ": the flesh between them is its colour");
            }
            double plateValue = KaguneColours.toHsv(p.plate())[2];
            check(Math.abs(plateValue - kagune[2]) > 0.25 || kagune[1] > 0.4, name + ": the plates stand apart from the kagune");
            check(KaguneColours.toHsv(p.rim())[2] > KaguneColours.toHsv(p.plate())[2] + 0.2, name + ": each plate's rim lit pale");
        }
        check(KakujaPalette.bone(0x141414) && !KakujaPalette.bone(KaguneColours.DEFAULT) && !KakujaPalette.bone(0xE8E8E8),
            "a black kagune gets bone-white plates; the others dark ones");
        check(KaguneColours.toHsv(KakujaPalette.of(0x141414).plate())[2] > 0.6, "bone-white plates are light");
        KakujaPalette.Palette blue = KakujaPalette.of(0x1E50D8);
        KakujaPalette.Palette red = KakujaPalette.of(KaguneColours.DEFAULT);
        check(blue.plate() != red.plate() && KakujaPalette.chroma(blue.plate()) < 0.08, "the greys lean, just, to the kagune's hue");
        com.hex.kagune.control.kakugan.CentipedeMask.colours(blue);
        com.hex.kagune.control.ukaku.CentipedeTail.colours(blue);
        check(true, "the legs and the mask take a palette");
        com.hex.kagune.control.kakugan.CentipedeMask.colours(red);
        com.hex.kagune.control.ukaku.CentipedeTail.colours(red);
    }

    /**
     * 2.5.0: a world's moments are its own: every map of game times on the server is made by WorldTimes (forgotten when a world stops, so
     * a single player's next world never inherits cooldowns or "until"s from the last); forgetting empties them all.
     */
    private static void worldTimes() {
        java.util.Map<String, Long> times = com.hex.kagune.control.server.WorldTimes.map();
        times.put("a", 123456789L);
        com.hex.kagune.control.server.WorldTimes.forget();
        check(times.isEmpty(), "a stopped world's moments are forgotten");
        boolean all = true;
        StringBuilder stray = new StringBuilder();
        try (java.util.stream.Stream<java.nio.file.Path> files = java.nio.file.Files.list(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/server"))) {
            for (java.nio.file.Path file : (Iterable<java.nio.file.Path>)files::iterator) {
                String text = java.nio.file.Files.readString(file);
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("static final (?:java\\.util\\.)?Map<[A-Za-z.]+, Long> ([A-Z_]+) = ([^;]+);").matcher(text);
                while (m.find()) {
                    if (!m.group(2).trim().equals("WorldTimes.map()")) {
                        all = false;
                        stray.append(' ').append(file.getFileName()).append(':').append(m.group(1));
                    }
                }
            }
        } catch (java.io.IOException failure) {
            all = false;
            stray.append(' ').append(failure);
        }
        check(all, "every map of game times on the server is WorldTimes' (forgotten with its world)" + stray);
    }

    /** 3.0.0: the higher ranks' quinques: who carries them, their strings, their moves, their meshes and the whip's paths. */
    /**
     * 3.1.8 (the owner: "improve the move sets of the briefcase ... I meant the heavy blade, hammer and whip"): the whip, the hammer and
     * the greatsword each have a third move of their own on Sneak + M1 (the crystal spear none); an investigator still chooses only its two.
     */
    static void armSignatures() {
        com.hex.kagune.control.quinque.Arm whip = com.hex.kagune.control.quinque.Arm.WHIP;
        com.hex.kagune.control.quinque.Arm hammer = com.hex.kagune.control.quinque.Arm.FLESH_HAMMER;
        com.hex.kagune.control.quinque.Arm sword = com.hex.kagune.control.quinque.Arm.BONE_SWORD;
        check(whip.signature() == com.hex.kagune.control.quinque.Arm.Ability.SWING && hammer.signature() == com.hex.kagune.control.quinque.Arm.Ability.FAULT
            && sword.signature() == com.hex.kagune.control.quinque.Arm.Ability.GUARD && com.hex.kagune.control.quinque.Arm.CRYSTAL_SPEAR.signature() == null,
            "the whip swings, the hammer breaks the ground open, the greatsword walls; the spear has none");
        java.util.Set<Integer> codes = new java.util.HashSet<>();
        for (com.hex.kagune.control.quinque.Arm.Ability a : com.hex.kagune.control.quinque.Arm.Ability.values()) {
            int code = com.hex.kagune.control.npc.ArmRules.abilityCode(a);
            check(codes.add(code) && code < 128 && com.hex.kagune.control.npc.ArmRules.ability(code) == a && com.hex.kagune.control.npc.ArmRules.step(code) < 0,
                a + ": its own code, a byte, read back");
            check(a.hits.length > 0 && a.hits[a.hits.length - 1] < a.total && a.cooldown > a.total, a + ": lands within it, rests longer than it runs");
        }
        for (com.hex.kagune.control.quinque.Arm arm : com.hex.kagune.control.quinque.Arm.values()) {
            check(arm.abilities().length == 2 && (arm.signature() == null || arm.slot(arm.signature()) == 2)
                && java.util.Arrays.stream(arm.abilities()).noneMatch(a -> a == arm.signature()), arm + ": two moves an investigator uses, its signature third");
            if (arm.signature() == null) {
                continue;
            }
            double[] start = com.hex.kagune.control.client.animation.QuinqueOwn.arm(arm, -1, arm.signature(), 0.0);
            double[] end = com.hex.kagune.control.client.animation.QuinqueOwn.arm(arm, -1, arm.signature(), arm.signature().total);
            double[] mid = com.hex.kagune.control.client.animation.QuinqueOwn.arm(arm, -1, arm.signature(), arm.signature().total * 0.5);
            double moving = 0.0;
            for (double v : mid) {
                moving += Math.abs(v);
            }
            check(java.util.Arrays.stream(start).allMatch(v -> v == 0.0) && java.util.Arrays.stream(end).allMatch(v -> v == 0.0) && moving > 0.0
                && java.util.Arrays.stream(mid).allMatch(Double::isFinite), arm + "'s signature in his own view: still at its ends, moving through it");
        }
        com.hex.kagune.control.quinque.Arm.Ability fault = com.hex.kagune.control.quinque.Arm.Ability.FAULT;
        double last = 1.6 + 1.6 * (fault.hits.length - 1);
        check(Math.abs(last - fault.farthest + 0.9) < 1.0 && fault.hits.length == 6, "the fault runs on 1.6 blocks a blow, about as far as it is used from: " + last);
        com.hex.kagune.control.quinque.Arm.Ability guard = com.hex.kagune.control.quinque.Arm.Ability.GUARD;
        check(com.hex.kagune.control.quinque.Arm.GUARD_PERFECT >= 3 && com.hex.kagune.control.quinque.Arm.GUARD_PERFECT < guard.total / 2
            && com.hex.kagune.control.quinque.Arm.GUARD_TAKES > 0.0F && com.hex.kagune.control.quinque.Arm.GUARD_TAKES < 0.5F,
            "the wall: a short perfect window, then most of a blow stopped");
        check(com.hex.kagune.control.npc.ArmRules.next(sword, sword.length() - 2, 0L) == sword.length() - 1,
            "after a parry his next blow is the string's last, its overhead cleave");
        check(com.hex.kagune.control.quinque.Arm.SWING_REACH > com.hex.kagune.control.quinque.Arm.Ability.BIND.farthest,
            "the swing lashes further than the bind catches");
        // Where it bit, told to every client: within a quarter of a block anywhere it can reach.
        boolean packs = true;
        for (double dx = -15.5; dx <= 15.5; dx += 3.1) {
            for (double dy = -15.5; dy <= 15.5; dy += 3.1) {
                for (double dz = -15.5; dz <= 15.5; dz += 3.1) {
                    double[] back = com.hex.kagune.control.quinque.WhipPath.anchor(com.hex.kagune.control.quinque.WhipPath.packAnchor(dx, dy, dz));
                    packs &= back != null && Math.abs(back[0] - dx) <= 0.126 && Math.abs(back[1] - dy) <= 0.126 && Math.abs(back[2] - dz) <= 0.126;
                }
            }
        }
        check(packs && com.hex.kagune.control.quinque.WhipPath.anchor(0) == null && com.hex.kagune.control.quinque.WhipPath.anchor(1234) == null,
            "a swing's latch survives the packet; a bind's target id is never read as one");
        double[] bit = {11.0, 0.5, 3.0};
        double[][] latched = com.hex.kagune.control.quinque.WhipPath.swing(10.0, bit, 6.0, 0.0);
        double[][] thrown = com.hex.kagune.control.quinque.WhipPath.swing(2.0, bit, 6.0, 0.0);
        double[][] back = com.hex.kagune.control.quinque.WhipPath.swing(com.hex.kagune.control.quinque.Arm.Ability.SWING.total - 0.01, bit, 6.0, 0.0);
        double[] tip = latched[latched.length - 1];
        check(Math.abs(tip[0] - bit[0]) < 1e-9 && Math.abs(tip[1] - bit[1]) < 1e-9 && Math.abs(tip[2] - bit[2]) < 1e-9 && latched[0][0] == 0.0,
            "latched, the tendril runs from the hand to where it bit (stretched past its own length)");
        double reached = Math.sqrt(thrown[thrown.length - 1][0] * thrown[thrown.length - 1][0] + thrown[thrown.length - 1][1] * thrown[thrown.length - 1][1]);
        double home = Math.sqrt(back[back.length - 1][0] * back[back.length - 1][0] + back[back.length - 1][1] * back[back.length - 1][1]);
        check(reached > 0.5 && reached < 11.0 && home < 0.5, "thrown, it is on its way out; let go, back in the hand");
    }

    private static void arms() {
        com.hex.kagune.control.npc.AgentRules.Rank[] ranks = com.hex.kagune.control.npc.AgentRules.Rank.values();
        boolean gated = true;
        for (com.hex.kagune.control.npc.AgentRules.Rank rank : ranks) {
            double chance = com.hex.kagune.control.npc.ArmRules.chance(rank);
            gated &= rank.level() < 4 || rank == com.hex.kagune.control.npc.AgentRules.Rank.RANK_10 ? chance == 0.0 : chance > 0.0;
            gated &= rank.level() >= 8 && rank.level() <= 9 ? chance == 1.0 && com.hex.kagune.control.npc.ArmRules.masterwork(rank)
                : !com.hex.kagune.control.npc.ArmRules.masterwork(rank);
        }
        boolean rising = true;
        for (int i = 5; i <= 9; i++) {
            rising &= com.hex.kagune.control.npc.ArmRules.chance(ranks[i]) >= com.hex.kagune.control.npc.ArmRules.chance(ranks[i - 1]);
        }
        check(gated && rising && com.hex.kagune.control.npc.ArmRules.roll(ranks[3], 0.0, 0.5) == null
            && com.hex.kagune.control.npc.ArmRules.roll(ranks[10], 0.0, 0.5) == null
            && com.hex.kagune.control.npc.ArmRules.roll(ranks[9], 0.99, 0.0) != null,
            "3.0.0: none below Rank 4, ever likelier up the ranks, always at 8 and 9 (a masterwork), never the White Reaper (Dawn)");
        java.util.Set<com.hex.kagune.control.quinque.Arm> low = new java.util.HashSet<>();
        java.util.Set<com.hex.kagune.control.quinque.Arm> high = new java.util.HashSet<>();
        for (int i = 0; i < 100; i++) {
            low.add(com.hex.kagune.control.npc.ArmRules.roll(ranks[4], 0.0, i / 100.0));
            high.add(com.hex.kagune.control.npc.ArmRules.roll(ranks[7], 0.0, i / 100.0));
        }
        check(low.equals(java.util.Set.of(com.hex.kagune.control.quinque.Arm.WHIP, com.hex.kagune.control.quinque.Arm.CRYSTAL_SPEAR))
            && high.size() == com.hex.kagune.control.quinque.Arm.values().length,
            "3.0.0: Rank 4 and 5 carry a whip or a crystal spear; from Rank 6 the hammer and the greatsword too");
        boolean strings = true;
        boolean moves = true;
        for (com.hex.kagune.control.quinque.Arm arm : com.hex.kagune.control.quinque.Arm.values()) {
            strings &= arm.length() >= 4;
            int step = 0;
            for (int i = 0; i < arm.length() - 1; i++) {
                int next = com.hex.kagune.control.npc.ArmRules.next(arm, step, 5L);
                strings &= next == step + 1;
                step = next;
            }
            strings &= com.hex.kagune.control.npc.ArmRules.next(arm, step, 5L) == 0 && com.hex.kagune.control.npc.ArmRules.next(arm, 1, 999L) == 0;
            for (int i = 0; i < arm.length(); i++) {
                com.hex.kagune.control.quinque.Arm.Strike strike = arm.strike(i);
                strings &= strike.land() > 0 && strike.land() < strike.total() && strike.damage() > 0.0F && strike.arc() > 0.0
                    && com.hex.kagune.control.npc.ArmRules.damage(arm, i, true) > com.hex.kagune.control.npc.ArmRules.damage(arm, i, false);
                strings &= com.hex.kagune.control.npc.ArmRules.step(com.hex.kagune.control.npc.ArmRules.stepCode(i)) == i;
            }
            float last = arm.strike(arm.length() - 1).damage();
            for (int i = 0; i < arm.length() - 1; i++) {
                strings &= arm.strike(i).damage() < last;
            }
            com.hex.kagune.control.quinque.Arm.Ability[] own = arm.abilities();
            moves &= own.length == 2 && own[0] != own[1] && arm.slot(own[0]) == 0 && arm.slot(own[1]) == 1;
            for (com.hex.kagune.control.quinque.Arm.Ability a : own) {
                moves &= a.hits.length > 0 && a.hits[a.hits.length - 1] < a.total && a.cooldown > a.total && a.farthest > a.nearest
                    && com.hex.kagune.control.npc.ArmRules.ability(com.hex.kagune.control.npc.ArmRules.abilityCode(a)) == a;
                double inside = (a.nearest + Math.min(a.farthest, a.nearest + 3.0)) / 2.0;
                moves &= com.hex.kagune.control.npc.ArmRules.ability(arm, ranks[9], inside, new boolean[] {true, true}, 0.0, arm.slot(a) == 0 ? 0.1 : 0.9) == a
                    || !own[1 - arm.slot(a)].fits(inside);
                moves &= com.hex.kagune.control.npc.ArmRules.ability(arm, ranks[9], inside, new boolean[] {false, false}, 0.0, 0.5) == null;
            }
            moves &= com.hex.kagune.control.npc.ArmRules.ability(arm, ranks[4], 3.0, new boolean[] {true, true}, 0.99, 0.5) == null;
            int packed = com.hex.kagune.control.npc.ArmRules.pack(arm, true, true);
            moves &= com.hex.kagune.control.npc.ArmRules.arm(packed) == arm && com.hex.kagune.control.npc.ArmRules.masterwork(packed)
                && com.hex.kagune.control.npc.ArmRules.out(packed) && !com.hex.kagune.control.npc.ArmRules.out(com.hex.kagune.control.npc.ArmRules.pack(arm, false, false));
        }
        check(strings, "3.0.0: every weapon's string is four to six blows, each landing inside its swing, run in order and started over after its last "
            + "or a pause; its last blow its heaviest; a masterwork's harder");
        check(moves && com.hex.kagune.control.npc.ArmRules.pack(null, true, true) == 0 && com.hex.kagune.control.npc.ArmRules.arm(0) == null
            && com.hex.kagune.control.npc.ArmRules.eagerness(ranks[9]) > com.hex.kagune.control.npc.ArmRules.eagerness(ranks[4]),
            "3.0.0: each weapon has two moves of its own, used at the distances they're for, only off cooldown, more readily the better the rank");
        check(com.hex.kagune.control.npc.ArmRules.shown(true, 0.0) == 0.0F && com.hex.kagune.control.npc.ArmRules.shown(true, 99.0) == 1.0F
            && com.hex.kagune.control.npc.ArmRules.shown(false, 0.0) == 1.0F && com.hex.kagune.control.npc.ArmRules.shown(false, 99.0) == 0.0F,
            "3.0.0: a weapon tears out of its case and goes back in over its unfold, eased");
        boolean meshes = true;
        StringBuilder sizes = new StringBuilder();
        for (com.hex.kagune.control.quinque.Arm arm : com.hex.kagune.control.quinque.Arm.values()) {
            for (boolean master : new boolean[] {false, true}) {
                com.hex.kagune.control.quinque.ArmMesh.Mesh m = com.hex.kagune.control.quinque.ArmMesh.of(arm, master);
                meshes &= m.quads > 100 && m.quads < 6000 && m.pos.length == m.quads * 12 && m.argb.length == m.quads * 4;
                for (float v : m.pos) {
                    meshes &= Float.isFinite(v) && Math.abs(v) < 4.0F;
                }
                for (int i = 0; i < m.normal.length; i += 3) {
                    double len = Math.sqrt(m.normal[i] * m.normal[i] + m.normal[i + 1] * m.normal[i + 1] + m.normal[i + 2] * m.normal[i + 2]);
                    meshes &= Math.abs(len - 1.0) < 1.0E-3;
                }
                boolean glows = false;
                for (boolean g : m.glow) {
                    glows |= g;
                }
                meshes &= glows && com.hex.kagune.control.quinque.ArmMesh.of(arm, master) == m;
                float[] span = m.span();
                sizes.append(' ').append(arm).append(master ? "*" : "").append('=').append(String.format("%.2f", span[1] - span[0]));
            }
            if (arm != com.hex.kagune.control.quinque.Arm.WHIP) {
                float[] reg = com.hex.kagune.control.quinque.ArmMesh.of(arm, false).span();
                float[] mw = com.hex.kagune.control.quinque.ArmMesh.of(arm, true).span();
                meshes &= reg[1] - reg[0] > 2.0F && mw[1] - mw[0] > reg[1] - reg[0];
            }
        }
        check(meshes, "3.0.0: every weapon's mesh builds clean (finite, unit normals, its glow), once, over two blocks long, a masterwork longer:" + sizes);
        boolean whips = true;
        double length = com.hex.kagune.control.quinque.WhipPath.length(false);
        for (int step = 0; step < 6; step++) {
            com.hex.kagune.control.quinque.Arm.Strike strike = com.hex.kagune.control.quinque.Arm.WHIP.strike(step);
            double land = strike.land() / (double)strike.total();
            double[][] at = com.hex.kagune.control.quinque.WhipPath.lash(step, land, land, length, 7.0);
            double[] tip = at[at.length - 1];
            double far = Math.sqrt(tip[0] * tip[0] + tip[1] * tip[1] + tip[2] * tip[2]);
            whips &= at.length == com.hex.kagune.control.quinque.WhipPath.POINTS && far > length * 0.75 && far <= length + 0.01;
            for (double t = 0.0; t <= 1.0; t += 0.05) {
                for (double[] pnt : com.hex.kagune.control.quinque.WhipPath.lash(step, t, land, length, 7.0)) {
                    whips &= Double.isFinite(pnt[0]) && Double.isFinite(pnt[1]) && Double.isFinite(pnt[2]);
                }
            }
        }
        double[][] bound = com.hex.kagune.control.quinque.WhipPath.bind(16.0, new double[] {4.0, 0.0, 0.0}, 0.35, length, 3.0);
        double[] end = bound[bound.length - 1];
        whips &= Math.abs(end[0] - 4.0) < 0.6 && Math.abs(end[1]) < 0.6;
        for (double age = 0.0; age <= com.hex.kagune.control.quinque.Arm.Ability.FLAY.total; age += 1.0) {
            for (double[] pnt : com.hex.kagune.control.quinque.WhipPath.flay(age, length, 3.0)) {
                whips &= Double.isFinite(pnt[0]) && Math.sqrt(pnt[0] * pnt[0] + pnt[1] * pnt[1] + pnt[2] * pnt[2]) <= length + 0.5;
            }
        }
        check(whips, "3.0.0: the whip's tendril reaches out near its whole length as each lash lands, never further; the bind coils round its foe");
        weaponPoses();
    }

    /** 3.0.0: how an investigator holds and swings its kagune-made weapon (WeaponPose, the client's, pure). */
    private static void weaponPoses() {
        java.util.Random random = new java.util.Random(30);
        boolean exact = true;
        boolean cone = true;
        for (com.hex.kagune.control.quinque.Arm arm : com.hex.kagune.control.quinque.Arm.values()) {
            double[] line = com.hex.kagune.control.client.animation.WeaponPose.held(com.hex.kagune.control.client.animation.WeaponPose.stance(arm), arm);
            double[] armWay = com.hex.kagune.control.client.animation.WeaponPose.armWay(com.hex.kagune.control.client.animation.WeaponPose.stance(arm));
            double held = line[0] * armWay[0] + line[1] * armWay[1] + line[2] * armWay[2];
            for (int i = 0; i < 300; i++) {
                double[] a = com.hex.kagune.control.client.animation.WeaponPose.way(random.nextDouble() * 360.0 - 180.0, random.nextDouble() * 180.0 - 90.0);
                double[] w = com.hex.kagune.control.client.animation.WeaponPose.way(random.nextDouble() * 360.0 - 180.0, random.nextDouble() * 180.0 - 90.0);
                com.hex.kagune.control.client.animation.RigPose p = new com.hex.kagune.control.client.animation.RigPose();
                com.hex.kagune.control.client.animation.WeaponPose.point(p, arm, a, w);
                double[] got = com.hex.kagune.control.client.animation.WeaponPose.held(p, arm);
                double[] armGot = com.hex.kagune.control.client.animation.WeaponPose.armWay(p);
                exact &= Math.abs(got[0] - w[0]) < 1.0E-4 && Math.abs(got[1] - w[1]) < 1.0E-4 && Math.abs(got[2] - w[2]) < 1.0E-4;
                cone &= Math.abs(got[0] * armGot[0] + got[1] * armGot[1] + got[2] * armGot[2] - held) < 1.0E-4;
            }
        }
        check(exact && cone, "3.0.0: the weapon points exactly where it is put, the arm holding it at its own set angle to it");
        boolean finite = true;
        boolean home = true;
        for (com.hex.kagune.control.quinque.Arm arm : com.hex.kagune.control.quinque.Arm.values()) {
            com.hex.kagune.control.client.animation.RigPose stance = com.hex.kagune.control.client.animation.WeaponPose.stance(arm);
            java.util.List<com.hex.kagune.control.client.animation.RigPose> ends = new java.util.ArrayList<>();
            for (int step = 0; step < arm.length(); step++) {
                com.hex.kagune.control.quinque.Arm.Strike strike = arm.strike(step);
                for (double age = 0.0; age <= strike.total(); age += 0.5) {
                    finite &= finite(com.hex.kagune.control.client.animation.WeaponPose.strike(arm, step, age));
                }
                ends.add(com.hex.kagune.control.client.animation.WeaponPose.strike(arm, step, strike.total()));
            }
            java.util.List<com.hex.kagune.control.quinque.Arm.Ability> moves = new java.util.ArrayList<>(java.util.List.of(arm.abilities()));
            if (arm.signature() != null) {
                moves.add(arm.signature()); // 3.1.8: its signature too
            }
            for (com.hex.kagune.control.quinque.Arm.Ability ability : moves) {
                for (double age = 0.0; age <= ability.total; age += 0.5) {
                    finite &= finite(com.hex.kagune.control.client.animation.WeaponPose.ability(arm, ability, age));
                }
                ends.add(com.hex.kagune.control.client.animation.WeaponPose.ability(arm, ability, ability.total));
            }
            for (com.hex.kagune.control.client.animation.RigPose end : ends) {
                double turn = Math.IEEEremainder(end.spin - stance.spin, Math.PI * 2.0);
                home &= Math.abs(end.rightArmX - stance.rightArmX) < 1.0E-3 && Math.abs(end.rightArmY - stance.rightArmY) < 1.0E-3
                    && Math.abs(end.rightArmZ - stance.rightArmZ) < 1.0E-3 && Math.abs(end.twist - stance.twist) < 1.0E-3 && Math.abs(turn) < 1.0E-3
                    && Math.abs(end.lean - stance.lean) < 1.0E-3 && Math.abs(end.lunge - stance.lunge) < 1.0E-3;
            }
        }
        check(finite, "3.0.0: every blow of every string and every move is a real pose all the way through");
        check(home, "3.0.0: every blow and every move ends back in its stance (a spin a whole turn round): nothing jumps when it ends");
        com.hex.kagune.control.quinque.Arm.Ability javelin = com.hex.kagune.control.quinque.Arm.Ability.JAVELIN;
        check(com.hex.kagune.control.client.animation.WeaponPose.grown(javelin, 13.0) == 1.0F
            && com.hex.kagune.control.client.animation.WeaponPose.grown(javelin, javelin.hits[0] + 1.0) == 0.0F
            && com.hex.kagune.control.client.animation.WeaponPose.grown(javelin, javelin.hits[0] + 14.0) == 1.0F
            && com.hex.kagune.control.client.animation.WeaponPose.grown(com.hex.kagune.control.quinque.Arm.Ability.QUAKE, 20.0) == 1.0F
            && com.hex.kagune.control.client.animation.WeaponPose.oneHanded(javelin, 14.0)
            && !com.hex.kagune.control.client.animation.WeaponPose.oneHanded(com.hex.kagune.control.quinque.Arm.Ability.PIERCE, 14.0),
            "3.0.0: the javelin leaves the hand as it is thrown (thrown one-handed) and another grows back");
        boolean fists = true;
        for (com.hex.kagune.control.quinque.Arm arm : com.hex.kagune.control.quinque.Arm.values()) {
            double[] fist = com.hex.kagune.control.client.animation.WeaponPose.meshToArm(arm)[3];
            fists &= Math.abs(fist[0] * 16.0 + 1.0) < 0.3 && Math.abs(fist[1] * 16.0 - 9.0) < 0.3 && Math.abs(fist[2] * 16.0) < 0.3;
        }
        check(fists, "3.0.0: each weapon is held in the fist (where the quinque it takes after is gripped)");
        double[] feet = com.hex.kagune.control.client.animation.WeaponPose.toModel(new double[] {0.0, 0.0, 0.0}, 37.0, 1.0, 1.0);
        double[] ahead = com.hex.kagune.control.client.animation.WeaponPose.toModel(new double[] {0.0, 1.501, 1.0}, 0.0, 1.0, 1.0);
        double[] left = com.hex.kagune.control.client.animation.WeaponPose.toModel(new double[] {1.0, 1.501, 0.0}, 0.0, 1.0, 1.0);
        double[] from = {0.2, 0.9, -0.1};
        double[] back = com.hex.kagune.control.client.animation.WeaponPose.toBody(from,
            com.hex.kagune.control.client.animation.WeaponPose.fromBody(from, new double[] {1.5, -0.4, 0.7}));
        check(Math.abs(feet[0]) < 1.0E-9 && Math.abs(feet[1] - 1.501) < 1.0E-9 && Math.abs(feet[2]) < 1.0E-9
            && Math.abs(ahead[2] + 1.0) < 1.0E-9 && Math.abs(ahead[1]) < 1.0E-9 && Math.abs(left[0] - 1.0) < 1.0E-9
            && Math.abs(back[0] - 1.5) < 1.0E-9 && Math.abs(back[1] + 0.4) < 1.0E-9 && Math.abs(back[2] - 0.7) < 1.0E-9,
            "3.0.0: the world into the investigator's model frame (facing south, ahead is its front, east its left) and its body's frame both ways");
    }

    private static boolean finite(com.hex.kagune.control.client.animation.RigPose p) {
        return Float.isFinite(p.rightArmX) && Float.isFinite(p.rightArmY) && Float.isFinite(p.rightArmZ) && Float.isFinite(p.twist)
            && Float.isFinite(p.lean) && Float.isFinite(p.lunge) && Float.isFinite(p.drop) && Float.isFinite(p.spin) && Float.isFinite(p.leftArmX);
    }

    public static void main(String[] args) {
        worldTimes();
        arms();
        armSignatures();
        kakujaPalette();
        rinkakuAlive();
        thyDeath();
        impale();
        hurl();
        hurlGrip();
        reaperDash();
        rinkakuBlock();
        rinkakuRoot();
        rinkakuRibbon();
        progression();
        hungerAndFrenzy();
        urges();
        food();
        comboTables();
        comboPacing();
        rinkakuForms();
        pathSmoother();
        abilityGuide();
        rinkakuMutations();
        damageBalance();
        ghoulBuffs();
        if (!FAILURES.isEmpty()) {
            FAILURES.forEach(failure -> System.err.println("FAILED: " + failure));
            System.err.println(FAILURES.size() + " of " + (passed + FAILURES.size()) + " checks failed");
            System.exit(1);
        }
        System.out.println("Control logic tests: " + passed + " checks passed");
    }

    private static void progression() {
        float previous = -1.0F;
        for (int mastery = 0; mastery <= 1000; mastery += 25) {
            float control = ControlMath.control(mastery, 0, false, false);
            check(control >= previous, "control must never drop as mastery rises (" + mastery + ")");
            previous = control;
        }
        check(ControlMath.control(0, 0, false, false) == 0.0F, "a new ghoul starts with no control");
        check(ControlMath.control(1000, 0, false, false) == 1.0F, "max mastery alone is full control (old saves keep it)");
        check(ControlMath.control(450, 0, false, false) > 0.45F && ControlMath.control(450, 0, false, false) < 0.6F,
            "mid progression (450) is roughly half control");
        check(ControlMath.control(100, ControlMath.RESTRAINT_FOR_FULL_BONUS, false, false)
            >= ControlMath.control(100, 0, false, false) + 0.17F, "restraint practice adds up to +0.18 control");
        check(ControlMath.control(100, 0, true, true) > ControlMath.control(100, 0, false, false), "calming mutations help");
        check(ControlMath.control(5000, 99999, true, true) == 1.0F, "control is clamped");

        check(ControlMath.struggle(1.0F, 1.0F, 1.0F) == 0.0F, "a master shows no struggle even starving and frenzied");
        check(ControlMath.struggle(0.0F, 0.0F, 0.0F) > 0.6F, "a new ghoul visibly struggles");
        check(ControlMath.manifestPainScale(0.0F, 0.0F) >= 1.0F, "first manifestations hurt at least as much as before");
        check(ControlMath.manifestPainScale(1.0F, 1.0F) <= 0.2F, "a master barely flinches");
        check(ControlMath.strikeDurationScale(0.0F) > 1.0F && ControlMath.strikeDurationScale(1.0F) < 1.0F, "strikes speed up with control");
        check(ControlMath.comboWindowTicks(1.0F) > ControlMath.comboWindowTicks(0.0F), "masters chain combos more easily");
        check(ControlMath.strikeDeviationDegrees(1.0F, true) == 0.0F, "mastered strikes never wander");
        check(ControlMath.strikeDeviationDegrees(0.0F, false) <= 20.0F, "untrained strikes wander only a little");
    }

    private static void hungerAndFrenzy() {
        check(GhoulReserve.eat(0.0F, 3) == 4.5F && GhoulReserve.eat(19.0F, 8) == GhoulReserve.MAX, "raw flesh fills the reserve, capped");
        check(GhoulReserve.healable(2.0F, 10.0F) == 4.0F && GhoulReserve.healable(0.0F, 1.0F) == 0.0F, "the reserve pays for what it can");
        check(GhoulReserve.afterHeal(10.0F, 4.0F) == 8.0F && GhoulReserve.reject(1.0F) == 0.0F, "healing and rejected food drain it");
        check(GhoulReserve.hunger(GhoulReserve.MAX) == 0.0F && GhoulReserve.hunger(0.0F) == 1.0F, "an empty reserve is full starvation");
        check(GhoulReserve.FOOD_HELD > 6 && GhoulReserve.FOOD_HELD < 18, "held hunger can sprint and never heals");
        check(ControlMath.starvation(20) == 0.0F && ControlMath.starvation(14) == 0.0F, "fed players are not starving");
        check(ControlMath.starvation(0) == 1.0F, "an empty food bar is full starvation");
        check(ControlMath.starvation(6) > ControlMath.starvation(10), "starvation grows as food drops");
        for (float control : new float[] {0.0F, 0.3F, 0.6F}) {
            check(ControlMath.instinct(control, 1.0F, 0.0F) > ControlMath.instinct(control, 0.0F, 0.0F),
                "hunger strengthens instinct at control " + control);
            check(ControlMath.instinct(control, 0.0F, 1.0F) > ControlMath.instinct(control, 0.0F, 0.0F),
                "frenzy strengthens instinct at control " + control);
        }
        check(ControlMath.bloodlust(0.0F, 0.0F, ControlMath.manifestFrenzy(0.0F)) > 0.4F, "first activations flood the edges with red");
        check(ControlMath.bloodlust(1.0F, 0.0F, ControlMath.manifestFrenzy(1.0F)) < 0.15F, "mastered activations stay subtle");
        check(ControlMath.frenzyDecay(1.0F, true) > ControlMath.frenzyDecay(0.0F, true), "mastered ghouls calm faster");
        check(ControlMath.frenzyFloor(1.0F, 1.0F) == 0.0F, "masters are not held on edge by hunger");
        check(ControlMath.manifestExhaustion(6, 0.0F) > ControlMath.manifestExhaustion(1, 0.0F), "more tails cost more food");
        // Starving at mastery 0: one food point (4 exhaustion) every ~40-60 seconds from manifestation alone.
        float perSecond = ControlMath.manifestExhaustion(1, 0.0F) * 20.0F;
        check(4.0F / perSecond > 35.0F && 4.0F / perSecond < 80.0F, "manifest hunger drain is meaningful but slow: " + 4.0F / perSecond + "s");
    }

    private static void urges() {
        check(ControlMath.instinct(1.0F, 1.0F, 1.0F) == 0.0F, "full control never feels urges");
        float mastered = ControlMath.control(760, 0, false, false);
        check(ControlMath.instinct(mastered, 1.0F, 1.0F) <= ControlMath.URGE_THRESHOLD,
            "late progression (mastery 760+) has no involuntary actions even starving and frenzied");
        float mid = ControlMath.control(450, 0, false, false);
        check(ControlMath.instinct(mid, 0.0F, 0.0F) <= ControlMath.URGE_THRESHOLD, "a fed, calm mid-progression ghoul is in control");
        check(ControlMath.instinct(mid, 0.0F, 1.0F) > ControlMath.URGE_THRESHOLD, "but frenzy in combat can still break through mid-game");
        check(ControlMath.instinct(0.0F, 0.0F, 0.0F) > ControlMath.URGE_THRESHOLD, "a new ghoul feels instinct even when fed");
        check(ControlMath.urgeChance(ControlMath.URGE_THRESHOLD, 1.0F) == 0.0F, "no urges at the threshold");
        check(ControlMath.urgeChance(1.0F, 1.0F) > ControlMath.urgeChance(0.6F, 1.0F), "stronger instinct, more urges");
        check(ControlMath.urgeChance(1.0F, 1.0F) <= 0.25F, "even the worst case is at most one urge chance per half-second check");
        check(ControlMath.urgeCooldownTicks(0.0F, 0.0F) >= 100, "urges are at least five seconds apart");
        check(ControlMath.restrainChance(1.0F, 0.0F, 0.0F) == 1.0F, "control makes restraint reliable");
        check(ControlMath.restrainChance(0.0F, 1.0F, 1.0F) >= 0.35F && ControlMath.restrainChance(0.0F, 1.0F, 1.0F) < 0.5F,
            "a starving, frenzied new ghoul often fails to restrain");

        Random random = new Random(7);
        for (int i = 0; i < 5000; i++) {
            float instinct = random.nextFloat();
            UrgeType scent = UrgeRules.choose(instinct, UrgeRules.PreyKind.SCENT, true, 2.0 + random.nextDouble() * 8.0, true, random.nextFloat());
            check(scent == UrgeType.GLANCE || scent == UrgeType.STEP, "players and villagers are never lunged at or struck");
            UrgeType noStrike = UrgeRules.choose(instinct, UrgeRules.PreyKind.THREAT, random.nextBoolean(), random.nextDouble() * 10.0, false,
                random.nextFloat());
            check(noStrike != UrgeType.STRIKE, "no forced strike unless the server allows it");
            UrgeType airborne = UrgeRules.choose(instinct, UrgeRules.PreyKind.THREAT, false, 5.0, false, random.nextFloat());
            check(airborne != UrgeType.LUNGE && airborne != UrgeType.STEP, "no pushes while airborne");
        }
        for (UrgeType type : UrgeType.values()) {
            check(UrgeRules.durationTicks(type, 1.0F) <= 20, "urges stay short: " + type);
        }
        check(UrgeRules.contextWeight(UrgeRules.PreyKind.HUNT, 0.0F, 0.0F) == 0.0F, "a fed, calm ghoul ignores animals");
        check(UrgeRules.contextWeight(UrgeRules.PreyKind.SCENT, 0.0F, 0.0F) == 0.0F, "a fed, calm ghoul ignores people");
        check(UrgeRules.pushSpeed(UrgeType.LUNGE, 1.0F) < 0.7, "lunges are short shoves, not dashes");
    }

    private static void food() {
        expect("beef", true, FoodClassifier.Diet.GHOUL_FOOD);
        expect("porkchop", true, FoodClassifier.Diet.GHOUL_FOOD);
        expect("chicken", true, FoodClassifier.Diet.GHOUL_FOOD);
        expect("rotten_flesh", true, FoodClassifier.Diet.GHOUL_FOOD);
        expect("cod", false, FoodClassifier.Diet.GHOUL_FOOD);
        expect("salmon", false, FoodClassifier.Diet.GHOUL_FOOD);
        expect("tropical_fish", false, FoodClassifier.Diet.GHOUL_FOOD);
        expect("pufferfish", false, FoodClassifier.Diet.GHOUL_FOOD);
        expect("spider_eye", false, FoodClassifier.Diet.GHOUL_FOOD);
        expect("raw_venison", false, FoodClassifier.Diet.GHOUL_FOOD);
        expect("raw_bacon", true, FoodClassifier.Diet.GHOUL_FOOD);
        expect("minced_beef", true, FoodClassifier.Diet.GHOUL_FOOD);
        expect("raw_tuna_slice", false, FoodClassifier.Diet.GHOUL_FOOD);
        expect("cooked_beef", true, FoodClassifier.Diet.HUMAN_FOOD);
        expect("cooked_cod", false, FoodClassifier.Diet.HUMAN_FOOD);
        expect("cooked_bacon", true, FoodClassifier.Diet.HUMAN_FOOD);
        expect("beef_stew", true, FoodClassifier.Diet.HUMAN_FOOD);
        expect("rabbit_stew", false, FoodClassifier.Diet.HUMAN_FOOD);
        expect("chicken_sandwich", true, FoodClassifier.Diet.HUMAN_FOOD);
        expect("venison_steak", true, FoodClassifier.Diet.HUMAN_FOOD);
        expect("bread", false, FoodClassifier.Diet.HUMAN_FOOD);
        expect("golden_apple", false, FoodClassifier.Diet.HUMAN_FOOD);
        expect("honey_bottle", false, FoodClassifier.Diet.HUMAN_FOOD);
        expect("sweet_berries", false, FoodClassifier.Diet.HUMAN_FOOD);
        expect("raw_potato", false, FoodClassifier.Diet.HUMAN_FOOD);
        expect("suspicious_stew", false, FoodClassifier.Diet.HUMAN_FOOD);
        expect("coffee", false, FoodClassifier.Diet.TOLERATED);
        expect("iced_coffee_cup", false, FoodClassifier.Diet.TOLERATED);
        expect("", true, FoodClassifier.Diet.GHOUL_FOOD);
        expect(null, false, FoodClassifier.Diet.HUMAN_FOOD);
    }

    private static void expect(String path, boolean meat, FoodClassifier.Diet expected) {
        FoodClassifier.Diet actual = FoodClassifier.classify(path, meat);
        check(actual == expected, "food '" + path + "' (meat=" + meat + ") should be " + expected + " but was " + actual);
    }

    /** 3.0.1: the build swaps the Centipede Form and the Partial Kakuja so the last two forms are Rinkaku's two kakuja. */
    /** 3.0.3: with only the eyes open, the kagune's movement buffs at half their level, never below I. */
    private static void ghoulBuffs() {
        check(GhoulBuffs.eyes(-1) == -1, "no kagune buff, nothing in ghoul mode");
        int[] expected = {0, 0, 0, 1, 1, 2, 2};
        for (int amp = 0; amp < expected.length; amp++) {
            check(GhoulBuffs.eyes(amp) == expected[amp] && GhoulBuffs.eyes(amp) <= amp, "level " + (amp + 1) + " gives level " + (GhoulBuffs.eyes(amp) + 1));
        }
        check(GhoulBuffs.eyes(4) < 4, "Agility Boost's Speed V is toned down");
    }

    /** 3.0.3: every kagune hit is brought down, most where the multipliers had stacked up (mastery 1000). */
    private static void damageBalance() {
        check(DamageBalance.scale(0) == DamageBalance.NEW && Math.abs(DamageBalance.scale(1000) - DamageBalance.MASTERED) < 1.0E-6F
            && DamageBalance.scale(5000) == DamageBalance.scale(1000) && DamageBalance.scale(-5) == DamageBalance.scale(0), "the scale's ends");
        float previous = 2.0F;
        for (int m = 0; m <= 1000; m += 50) {
            check(DamageBalance.scale(m) <= previous && DamageBalance.scale(m) < 1.0F, "the cut deepens with mastery (" + m + ")");
            previous = DamageBalance.scale(m);
        }
        // A Full Kakuja's average swing at 1000 (the original's 12 x 1.62 form x 1.22 stage) per its 13.3 ticks: near a sword's 18/s.
        double full = 12.0 * KaguneForm.FULL_KAKUJA.damageMultiplier() * 1.22 * DamageBalance.scale(1000) * 20.0 / 13.3;
        check(full > 14.0 && full < 24.0, "a mastered Full Kakuja's sustained swings: " + full + " a second");
        // RC goes further with mastery (1.0.33): everything drawn from the bar scaled from its full price to a fifth at 1000.
        check(EnergyBalance.costScale(0) == 1.0F && EnergyBalance.costScale(-50) == 1.0F, "a new ghoul pays what the original asks");
        check(Math.abs(EnergyBalance.capacity(1000) - EnergyBalance.MASTERED_CAPACITY) < 1.0E-6F
            && EnergyBalance.costScale(4000) == EnergyBalance.costScale(1000), "at mastery 1000 a full bar goes five times as far");
        float last = 2.0F;
        for (int m = 0; m <= 1000; m += 50) {
            float original = 1.0F - (1.0F - EnergyBalance.ORIGINAL_MASTERED) * m / 1000.0F;
            float before = 1.0F - 0.5F * m / 1000.0F; // 6.1.0's cut on a move
            check(EnergyBalance.costScale(m) <= last, "cheaper the more mastery (" + m + ")");
            check(EnergyBalance.costScale(m) <= before + 1.0E-6F, "never dearer than before (" + m + ")");
            check(Math.abs(EnergyBalance.undoOriginal(m) * original - 1.0F) < 1.0E-5F, "the original's own cut undone (" + m + ")");
            last = EnergyBalance.costScale(m);
        }
    }

    /** 1.0.41: the Rinkaku path has no mutations at all: none shown, none counts (an old save's least of all); the guard stays everyone's. */
    private static void rinkakuMutations() {
        check(RinkakuMutations.nodes().length == 0, "none kept");
        int cut = 0;
        for (com.hex.kagune.gameplay.MutationNode node : com.hex.kagune.gameplay.MutationNode.values()) {
            check(RinkakuMutations.removed(node), node + " is cut");
            cut++;
            boolean counts = RinkakuMutations.has(-1L, node);
            check(counts == (node == com.hex.kagune.gameplay.MutationNode.GUARD_MEMBRANE), node + (counts ? " counts" : " never counts, even from an old save"));
        }
        check(cut == com.hex.kagune.gameplay.MutationNode.values().length, "every one cut");
        check(RinkakuMutations.has(0L, com.hex.kagune.gameplay.MutationNode.GUARD_MEMBRANE), "the guard is everyone's");
        long old = com.hex.kagune.gameplay.MutationNode.MASS.bit() | com.hex.kagune.gameplay.MutationNode.REACH.bit()
            | com.hex.kagune.gameplay.MutationNode.FULL_HUNTER.bit();
        check(RinkakuMutations.strip(old) == 0L, "an old save keeps none of them");
    }

    private static void rinkakuForms() {
        KaguneForm[] all = KaguneForm.values();
        check(java.util.Arrays.equals(all, new KaguneForm[] {KaguneForm.RINKAKU_SEED, KaguneForm.RINKAKU_DUAL, KaguneForm.RINKAKU_QUAD,
            KaguneForm.PARTIAL_KAKUJA, KaguneForm.CENTIPEDE, KaguneForm.FULL_KAKUJA}), "enum slots stay (ordinals travel): " + java.util.Arrays.toString(all));
        for (int i = 0; i < all.length; i++) {
            check(all[i].ordinal() == i, all[i] + " ordinal matches its slot");
        }
        // 3.0.2: the Partial Kakuja is gone from everything a player sees, and the forms left come every 250 mastery.
        KaguneForm[] forms = RinkakuForms.forms();
        check(java.util.Arrays.equals(forms, new KaguneForm[] {KaguneForm.RINKAKU_SEED, KaguneForm.RINKAKU_DUAL, KaguneForm.RINKAKU_QUAD,
            KaguneForm.CENTIPEDE, KaguneForm.FULL_KAKUJA}), "Rinkaku's forms: " + java.util.Arrays.toString(forms));
        int[] mastery = {0, 250, 500, 750, 1000};
        float previousDamage = 0.0F;
        for (int i = 0; i < forms.length; i++) {
            check(forms[i].requiredMastery() == mastery[i], forms[i] + " unlocks at " + mastery[i]);
            check(RinkakuForms.kakuja(forms[i]) == (i >= forms.length - 2), forms[i] + " is a kakuja only in the last two slots");
            check(forms[i].isKakuja() == RinkakuForms.kakuja(forms[i]), forms[i] + " the original's kakuja check (armour 2+) agrees");
            check(forms[i].damageMultiplier() > previousDamage, forms[i] + " hits harder than the form before it");
            previousDamage = forms[i].damageMultiplier();
            check(KaguneForm.fromId(forms[i].id()) == forms[i] && KaguneForm.find(forms[i].name()) == forms[i], forms[i] + " stored id still loads");
        }
        KaguneForm removed = KaguneForm.PARTIAL_KAKUJA;
        check(RinkakuForms.REMOVED == removed && !RinkakuForms.kakuja(removed), "the removed form is the Partial Kakuja");
        for (int m = 0; m <= 1000; m += 10) {
            check(!removed.isUnlocked(m), "the Partial Kakuja never unlocks (" + m + ")");
        }
        check(KaguneForm.find("partial") == null && KaguneForm.find("PARTIAL_KAKUJA") == null && KaguneForm.fromId("partial") == KaguneForm.RINKAKU_SEED,
            "a stored Partial Kakuja loads as the Seed and /kagune form can't find it");
        check(!java.util.Arrays.asList(KaguneForm.ids()).contains("partial") && KaguneForm.ids().length == 5, "the form ids list skips it");
        check(KaguneForm.fromId("centipede") == KaguneForm.CENTIPEDE && KaguneForm.CENTIPEDE.displayName().equals("Centipede Form"),
            "ids and names stay with their forms (old saves)");
        check(RinkakuForms.autonomous(KaguneForm.CENTIPEDE) && !RinkakuForms.autonomous(KaguneForm.FULL_KAKUJA)
            && !RinkakuForms.autonomous(KaguneForm.RINKAKU_QUAD) && !RinkakuForms.autonomous(null),
            "only the first kakuja fights on its own (he can only walk in it)");
        check(RinkakuForms.speed(KaguneForm.CENTIPEDE) > RinkakuForms.speed(KaguneForm.FULL_KAKUJA)
            && RinkakuForms.speed(KaguneForm.FULL_KAKUJA) == 2 && RinkakuForms.speed(KaguneForm.RINKAKU_SEED) == -1,
            "5.0.9: the first kakuja runs faster than the final one's Speed III; the rinkaku forms get none");
        check(RinkakuForms.firstKakuja() == KaguneForm.CENTIPEDE && KaguneForm.CENTIPEDE.limbCount() == 2
            && KaguneForm.CENTIPEDE.isCentipedeLimb(0) && KaguneForm.CENTIPEDE.isCentipedeLimb(1), "the first kakuja: the two centipede legs");
        boolean legsAndTails = false;
        for (int limb = 0; limb < KaguneForm.FULL_KAKUJA.limbCount(); limb++) {
            legsAndTails |= !KaguneForm.FULL_KAKUJA.isCentipedeLimb(limb);
        }
        check(RinkakuForms.finalKakuja() == KaguneForm.FULL_KAKUJA && KaguneForm.FULL_KAKUJA.hasCentipedeLegs() && legsAndTails,
            "the final kakuja: centipede legs and tentacles together");
        check(RinkakuForms.firstKakuja().requiredMastery() == com.hex.kagune.control.server.UkakuForms.KAKUJA_MASTERY,
            "K unlocks with the first kakuja");
        check(ComboStyle.forForm(KaguneForm.CENTIPEDE) == ComboStyle.HEAVY && ComboStyle.forForm(KaguneForm.FULL_KAKUJA) == ComboStyle.PREDATOR,
            "combos follow each form's limbs");
        for (int m = 0; m <= 1000; m += 50) {
            KaguneForm base = RinkakuForms.strongestBase(m);
            check(!RinkakuForms.kakuja(base) && base != removed && base.isUnlocked(m), "the fallback at " + m + " is an unlocked non-kakuja form");
        }
        check(RinkakuForms.strongestBase(1000) == KaguneForm.RINKAKU_QUAD && RinkakuForms.strongestBase(300) == KaguneForm.RINKAKU_DUAL
            && RinkakuForms.strongestBase(249) == KaguneForm.RINKAKU_SEED, "the fallback is the strongest form below the kakuja");
        // K: a tap brings out the first kakuja or goes back; a hold (from 1000) the final one or back; never lands on a kakuja going back.
        KaguneForm quad = KaguneForm.RINKAKU_QUAD;
        KaguneForm first = KaguneForm.CENTIPEDE;
        KaguneForm last = KaguneForm.FULL_KAKUJA;
        check(RinkakuForms.afterKey(quad, null, 749, false) == null && RinkakuForms.afterKey(quad, null, 749, true) == null, "K is locked below 750");
        check(RinkakuForms.afterKey(quad, null, 750, false) == first, "tap at 750: the first kakuja");
        check(RinkakuForms.afterKey(quad, null, 900, true) == first, "below 1000 a hold is a tap");
        check(RinkakuForms.afterKey(first, quad, 800, false) == quad && RinkakuForms.afterKey(first, quad, 800, true) == quad,
            "tap (or a hold below 1000) again goes back to where it came from");
        check(RinkakuForms.afterKey(quad, null, 1000, false) == first && RinkakuForms.afterKey(quad, null, 1000, true) == last,
            "at 1000: tap for the first kakuja, hold for the final one");
        check(RinkakuForms.afterKey(first, quad, 1000, true) == last, "hold from the first kakuja: the final one");
        check(RinkakuForms.afterKey(last, quad, 1000, true) == quad && RinkakuForms.afterKey(last, quad, 1000, false) == quad,
            "tap or hold in the final kakuja goes back");
        check(RinkakuForms.afterKey(last, first, 1000, false) == quad
            && RinkakuForms.afterKey(first, null, 1000, false) == quad, "going back never lands on a kakuja");
        check(RinkakuForms.afterKey(first, KaguneForm.FULL_KAKUJA, 760, false) == quad
            && RinkakuForms.afterKey(first, removed, 1000, false) == quad,
            "a stored base that is a kakuja, locked or removed falls back to the strongest base");
        // The menu: purple only for the two kakuja.
        int crimson = 0xFFB51E46;
        check(RinkakuForms.buttonColor(crimson, quad, true, true) == crimson && RinkakuForms.labelColor(0xFFF6DDE4, quad, true) == 0xFFF6DDE4,
            "base forms keep the original buttons");
        check(RinkakuForms.buttonColor(crimson, first, true, true) == RinkakuForms.KAKUJA_SELECTED
            && RinkakuForms.buttonColor(0xFF5D1830, last, false, true) == RinkakuForms.KAKUJA_UNLOCKED
            && RinkakuForms.buttonColor(0xFF2E2228, last, false, false) == RinkakuForms.KAKUJA_LOCKED
            && RinkakuForms.labelColor(0xFF705860, last, false) == RinkakuForms.KAKUJA_LABEL_LOCKED, "kakuja buttons are purple");
    }

    private static void comboTables() {
        for (KaguneForm form : KaguneForm.values()) {
            ComboStyle style = ComboStyle.forForm(form);
            check(style.stageCount() >= 3, form + " has a real combo");
            int multiLimbStages = 0;
            for (int index = 0; index < style.stageCount(); index++) {
                ComboStage stage = style.stage(index);
                String label = form + " stage " + stage.name();
                check(stage.impactFraction() > 0.25F && stage.impactFraction() < 0.55F, label + " connects in its first half");
                check(stage.damageMul() >= 0.9F && stage.damageMul() <= 1.6F, label + " damage stays near the original swipe");
                check(stage.rangeMul() >= 0.9F && stage.rangeMul() <= 1.15F, label + " keeps the original reach");
                check(stage.maxTargets() >= 1 && stage.maxTargets() <= 6, label + " target cap");
                check(stage.baseDuration() >= 10 && stage.baseDuration() <= 17, label + " is not a long animation lock");
                for (int limb : stage.limbs()) {
                    check(limb >= 0 && limb < form.limbCount(), label + " limb " + limb + " exists on " + form);
                }
                if (stage.limbs().length > 1) {
                    multiLimbStages++;
                }
                boolean multiPattern = stage.pattern() == StrikePattern.SCISSOR || stage.pattern() == StrikePattern.IMPALE
                    || stage.pattern() == StrikePattern.SPIN;
                check(!multiPattern || stage.limbs().length >= 2, label + " multi-limb pattern has several limbs");
                if (style == ComboStyle.HEAVY) {
                    for (int limb : stage.limbs()) {
                        check(form.isCentipedeLimb(limb), label + " uses the heavy segmented legs");
                    }
                }
            }
            check(form.limbCount() == 1 || multiLimbStages > 0, form + " has a multi-limb finisher");
            for (int index = 0; index < style.stageCount(); index++) {
                int mask = ComboStyle.limbMask(style.stage(index), 1, 0);
                check(mask != 0 && (mask & ~1) == 0, form + " stage " + index + " still strikes with only one tail out");
            }
        }
        check(ComboStyle.forForm(KaguneForm.RINKAKU_SEED) != ComboStyle.forForm(KaguneForm.CENTIPEDE), "forms fight differently");
        check(ComboStyle.limbMask(ComboStyle.FLURRY.stage(3), 2, -1) == 0b11, "cycled-down tails drop out of a finisher");
        check(ComboStyle.limbMask(ComboStyle.FLURRY.stage(2), 2, -1) == 0, "a stage whose tails are retracted has no limbs");
        check(ComboStyle.limbMask(ComboStyle.FLURRY.stage(2), 2, 1) == 0b10, "and then falls back to the rotating limb");
        check(ComboStyle.byOrdinal(-1) == ComboStyle.WHIP && ComboStyle.byOrdinal(99) == ComboStyle.WHIP, "bad ordinals are safe");
        check(UrgeType.byOrdinal(42) == UrgeType.NONE, "bad urge ordinals are safe");
    }

    /** Fire shots at their cadence (KaguneCombat): ranged, so a little under the original melee pace, never far off it. */
    private static void flamePacing(ComboStyle style) {
        if (style != ComboStyle.UKAKU_FIRE) {
            return; // the enders end a chain; they are not a chain of their own
        }
        for (int mastery = 0; mastery <= 1000; mastery += 50) {
            double damage = 0.0;
            long time = 0L;
            for (int index = 0; index < style.stageCount() * 4; index++) {
                ComboStage stage = style.stage(index);
                int impact = stage.impactTick(Math.max(8, stage.baseDuration()));
                damage += stage.damageMul();
                time += Math.max(impact + 1, com.hex.kagune.control.ukaku.UkakuRules.flameCadence(mastery));
            }
            double ratio = damage / (time + 1.0) / (1.0 / 12.0);
            check(ratio > 0.6 && ratio < 1.4, style + " fire pace vs the original swipe at mastery " + mastery + ": " + ratio);
        }
    }

    /** Mirrors KaguneCombat timing (KaguneProfile.cooldown multiplier, 8-tick minimum) for every style and control level. */
    private static void comboPacing() {
        float[] mutationMultipliers = {1.0F, 0.75F, 0.75F * 0.82F};
        for (ComboStyle style : ComboStyle.values()) {
            if (style.stage(0).pattern().flame()) {
                flamePacing(style);
                continue; // bolts keep their own cadence and ignore hurt immunity (UkakuFire)
            }
            for (float control : new float[] {0.0F, 0.5F, 1.0F}) {
                for (float multiplier : mutationMultipliers) {
                    double damage = 0.0;
                    long time = 0L;
                    long previousImpact = Long.MIN_VALUE;
                    for (int index = 0; index < style.stageCount() * 4; index++) {
                        ComboStage stage = style.stage(index);
                        int duration = duration(stage, control, multiplier);
                        int impact = stage.impactTick(duration);
                        check(impact > 0 && impact < duration, style + " impact inside the strike");
                        long impactTime = time + impact;
                        if (previousImpact != Long.MIN_VALUE) {
                            check(impactTime - previousImpact >= 10, style + " chained impacts respect hurt immunity at control " + control);
                        }
                        previousImpact = impactTime;
                        damage += stage.damageMul();
                        ComboStage next = style.stage(index + 1);
                        time += ComboStyle.chainOpenOffset(impact, next.impactTick(duration(next, control, multiplier)));
                    }
                    // The original swipe: 1.0x damage every max(8, cooldown(12)) ticks.
                    double original = 1.0 / Math.max(8, Math.max(2, Math.round(12 * multiplier)));
                    double ratio = damage / (time + 1.0) / original;
                    check(ratio > 0.7 && ratio < 1.5, style + " single-target pace vs original at control " + control + " x" + multiplier + ": " + ratio);
                }
            }
        }
    }

    private static int duration(ComboStage stage, float control, float multiplier) {
        int base = Math.round(stage.baseDuration() * ControlMath.strikeDurationScale(control));
        return Math.max(8, Math.max(2, Math.round(base * multiplier)));
    }
}
