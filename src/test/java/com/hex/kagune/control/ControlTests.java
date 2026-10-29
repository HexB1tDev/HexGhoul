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
                check(combo == null && !anyMove && find(panel, "Rapid Strike") == null && find(panel, "Grab") == null
                    && find(panel, "Guard") == null && find(panel, "Grapple") == null, "the first kakuja lists no move he can make");
                check(find(panel, "Centipede legs fight on their own") != null && find(panel, "You can only walk") != null
                    && find(panel, "Turn back") != null && find(panel, "Put it all away") != null,
                    "the first kakuja: the legs fight on their own, he walks, K and R still take him out of it");
            } else {
                check(combo != null && combo.keys().equals("LMB"), forms[i] + ": the M1 is the " + combos[i] + " combo on the attack key");
                check(find(panel, "Rapid Strike") != null && find(panel, "Rapid Strike").keys().equals("Z"), forms[i] + ": Z is Rapid Strike");
                check(find(panel, "Grab") != null && find(panel, "Grab").keys().equals("G·N"), forms[i] + ": grab and throw on G and N");
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
        check(find(blade, "Steel Blade:") != null && find(blade, "Dash") != null && find(blade, "Vanish") != null && find(blade, "Fireballs") != null,
            "the Steel Blade's board: swings, dash, vanish, fireballs");
        // The player's own keys.
        java.util.Map<AbilityGuide.Key, String> rebound = defaultKeys();
        rebound.put(AbilityGuide.Key.RAPID, AbilityGuide.keyName("key.keyboard.f", "CONTROL"));
        rebound.put(AbilityGuide.Key.TOGGLE, AbilityGuide.keyName("key.mouse.4", "NONE"));
        AbilityGuide.Panel moved = AbilityGuide.panel(new AbilityGuide.Input(KagunePath.RINKAKU, AbilityGuide.State.OUT, 100,
            com.hex.kagune.gameplay.KaguneForm.RINKAKU_SEED, twin, false, rebound));
        check(find(moved, "Rapid Strike").keys().equals("Ctrl+F"), "a rebound key shows as bound: " + find(moved, "Rapid Strike").keys());
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

    public static void main(String[] args) {
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
