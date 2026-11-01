package com.hex.kagune.control;

import com.hex.kagune.control.quinque.Quinque;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 1.3.25: the CCG's quinques: a case opening into its weapon and shutting again over its frames, never overflowing; the two kinds'
 * strengths; and every file they need in place (a missing model is a purple cube in game): each item's model, its five frames, their
 * texture, the property they are chosen by, its name. 1.3.27: and no recipe: a case is never made, only taken from a fallen CCG
 * investigator, and rarely.
 */
public final class QuinqueTests {
    static int passed;
    static final List<String> FAILURES = new ArrayList<>();
    static final Path RESOURCES = Path.of("src/main/resources");

    private static void check(boolean condition, String description) {
        if (condition) {
            passed++;
        } else {
            FAILURES.add(description);
        }
    }

    public static void main(String[] args) throws IOException {
        turning();
        kinds();
        files();
        holds();
        if (!FAILURES.isEmpty()) {
            FAILURES.stream().limit(40).forEach(failure -> System.err.println("FAILED: " + failure));
            System.err.println(FAILURES.size() + " of " + (passed + FAILURES.size()) + " checks failed");
            System.exit(1);
        }
        System.out.println("Quinque tests: " + passed + " checks passed");
    }

    static void turning() {
        long now = 1_000_000L;
        check(Quinque.progress(now, Long.MIN_VALUE) == 1.0F && !Quinque.moving(now, Long.MIN_VALUE), "never turned: still (no overflow)");
        check(Quinque.progress(now, now) == 0.0F && Quinque.moving(now, now), "just turned: it starts moving");
        check(Quinque.progress(now, now - Quinque.UNFOLD / 2) > 0.4F && Quinque.progress(now, now - Quinque.UNFOLD / 2) < 0.6F, "half way, half out");
        check(Quinque.progress(now, now - Quinque.UNFOLD) == 1.0F && !Quinque.moving(now, now - Quinque.UNFOLD), "done after its unfold");
        check(Quinque.progress(now, now + 40L) == 0.0F, "a time ahead of the clock (a lagging client) is the start, never negative");
        check(Quinque.progress(Long.MAX_VALUE, Long.MIN_VALUE + 1) == 1.0F && Quinque.progress(Long.MIN_VALUE + 5, Long.MAX_VALUE) == 0.0F,
            "the far ends of time don't overflow");
        float last = -1.0F;
        boolean rising = true;
        for (int t = 0; t <= Quinque.UNFOLD; t++) {
            float p = Quinque.shown(true, now + t, now);
            rising &= p >= last && p >= 0.0F && p <= 1.0F;
            last = p;
            check(Math.abs(Quinque.shown(false, now + t, now) - (1.0F - p)) < 1.0E-6F, "a case shutting runs its frames backwards (tick " + t + ")");
        }
        check(rising && last == 1.0F, "a weapon opening runs its frames forwards to all the way out");
        check(Quinque.shown(true, now, Long.MIN_VALUE) == 1.0F && Quinque.shown(false, now, Long.MIN_VALUE) == 0.0F,
            "still: a weapon out, a case shut");
    }

    static void kinds() {
        check(Math.abs(Quinque.BLADE.attackDamage() - 8.0F) < 1.0E-4F && Math.abs(Quinque.BLADE.attacksPerSecond() - 1.6F) < 1.0E-4F,
            "the Bikaku blade: 8 damage, 1.6 swings a second (a netherite sword's)");
        check(Math.abs(Quinque.SPEAR.attackDamage() - 8.0F) < 1.0E-4F && Quinque.SPEAR.attacksPerSecond() > Quinque.BLADE.attacksPerSecond(),
            "1.3.26: the Koukaku spear is agile: 8 damage, the quickest swing");
        check(Quinque.SPEAR.reach >= 2.0 && Quinque.BLADE.reach == 0.0 && Quinque.SPEAR.reach > Quinque.KATANA.reach, "the spear reaches furthest");
        check(Quinque.MACE.attackDamage() > Quinque.KATANA.attackDamage() && Quinque.MACE.attacksPerSecond() < 1.0F,
            "the mace is the heaviest and slowest");
        check(Quinque.SPEAR.twoHanded && Quinque.KATANA.twoHanded && Quinque.MACE.twoHanded && !Quinque.BLADE.twoHanded,
            "the spear, katana and mace are held in both hands");
        check(Quinque.LAUNCHER.ranged && !Quinque.KATANA.ranged, "only the launcher fires");
        // 1.3.26: a string of three M1s, the third the finisher; a pause starts it again.
        check(Quinque.next(-1, 0L) == 0 && Quinque.next(0, 10L) == 1 && Quinque.next(1, 10L) == 2 && Quinque.next(2, 10L) == 0
            && Quinque.next(1, Quinque.COMBO + 1) == 0 && Quinque.next(0, Long.MAX_VALUE) == 0, "three M1s to a string, a pause starts again");
        for (Quinque q : Quinque.values()) {
            check(q.weight(0) == 1.0F && q.weight(1) == 1.0F && (q.ranged || q.weight(Quinque.FINISHER) > 1.0F), q + ": only its third blow is heavier");
        }
        check(Quinque.KATANA.sweep(Quinque.FINISHER) > 0.0 && Quinque.MACE.sweep(Quinque.FINISHER) > 0.0 && Quinque.SPEAR.sweep(Quinque.FINISHER) > 0.0
            && Quinque.BLADE.sweep(Quinque.FINISHER) == 0.0 && Quinque.KATANA.sweep(0) == 0.0, "the katana's, mace's and spear's finishers reach others");
        int heat = 0;
        int shots = 0;
        while (heat < Quinque.HEAT_MOST && shots < 1000) {
            heat = Quinque.heated(heat, Quinque.SHOT_EVERY);
            shots++;
        }
        double seconds = shots * Quinque.SHOT_EVERY / 20.0;
        check(seconds >= 4.5 && seconds <= 8.0, "held down (1.3.33: a shard every two ticks) it overheats after about six seconds of fire: " + seconds);
        check(Quinque.SHOT_EVERY == 2 && 20.0 / Quinque.SHOT_EVERY * Quinque.SHARD_DAMAGE >= 17.5, "ten a second, no weaker than five of the old");
        heat = 0;
        for (int burst = 0; burst < 20; burst++) {
            for (int shot = 0; shot < 8; shot++) {
                heat = Quinque.heated(heat, Quinque.SHOT_EVERY);
            }
            heat = Quinque.heated(heat, 60L) - 1; // a breath between bursts
        }
        check(heat < Quinque.HEAT_MOST, "fired in bursts it never overheats");
        check(Quinque.heated(29, Long.MAX_VALUE) == 1, "long left it is cold (no overflow)");
        for (Quinque q : Quinque.values()) {
            check(q.ghoul > 1.0F && q.suppress > 0, q + " cuts a ghoul deeper and holds its regeneration back");
            check(Quinque.byId(q.caseId) == q && Quinque.byId("hexkagune:" + q.weaponId) == q, q + " is found by its case's and weapon's ids");
        }
        check(Quinque.byId("hexkagune:ghoul_meat") == null && Quinque.byId(null) == null, "nothing else is a quinque");
    }

    static String read(String path) throws IOException {
        Path p = RESOURCES.resolve(path);
        return Files.exists(p) ? Files.readString(p) : null;
    }

    static void files() throws IOException {
        String lang = read("assets/hexkagune/lang/en_us.json");
        Pattern override = Pattern.compile("\"hexkagune:" + Quinque.PROPERTY + "\"\\s*:\\s*([0-9.]+)\\s*}\\s*,\\s*\"model\"\\s*:\\s*\"hexkagune:([a-z_/]+)\"");
        for (Quinque q : Quinque.values()) {
            for (String item : new String[] {q.caseId, q.weaponId}) {
                String json = read("assets/hexkagune/models/item/" + item + ".json");
                check(json != null, item + " has an item model");
                if (json == null) {
                    continue;
                }
                Matcher m = override.matcher(json);
                double previous = -1.0;
                int frames = 0;
                while (m.find()) {
                    double at = Double.parseDouble(m.group(1));
                    check(at > previous, item + ": its frames are listed in order (" + at + ")");
                    previous = at;
                    String frame = read("assets/hexkagune/models/" + m.group(2) + ".json");
                    check(frame != null, item + ": its frame " + m.group(2) + " exists");
                    if (frame != null) {
                        Matcher texture = Pattern.compile("\"q\"\\s*:\\s*\"hexkagune:([a-z_/]+)\"").matcher(frame);
                        check(texture.find() && Files.exists(RESOURCES.resolve("assets/hexkagune/textures/" + texture.group(1) + ".png")),
                            item + ": its frame " + m.group(2) + "'s texture exists");
                    }
                    frames++;
                }
                // 1.3.28: a case also has an investigator's two carried frames (ahead of the four opening ones); a weapon never does.
                int expected = item.equals(q.caseId) ? 6 : 4;
                check(frames == expected && previous > 0.9 && previous <= 1.0, item + ": " + expected + " frames past its shut case, the last all the way out");
                check(json.contains("case_carried") == item.equals(q.caseId), item + ": the carried frames on the case alone");
                Matcher parent = Pattern.compile("\"parent\"\\s*:\\s*\"hexkagune:([a-z_/]+)\"").matcher(json);
                check(parent.find() && read("assets/hexkagune/models/" + parent.group(1) + ".json") != null, item + ": its shut case model exists");
                check(lang != null && lang.contains("\"item.hexkagune." + item + "\""), item + " has a name");
            }
            check(lang != null && lang.contains("item.hexkagune.quinque." + q.model + ".lore"), q + " has its lore line");
        }
        check(lang != null && lang.contains("item.hexkagune.quinque.open") && lang.contains("item.hexkagune.quinque.close")
            && lang.contains("item.hexkagune.quinque.ghoul"), "the quinques' use lines are named");
        // 1.3.28: the case's own sounds (tools/case_sfx.py), two takes each, played as it opens and shuts, with subtitles.
        String sounds = read("assets/hexkagune/sounds.json");
        String server = Files.readString(Path.of("src/main/java/com/hex/kagune/control/server/QuinqueServer.java"));
        for (String event : new String[] {"case_open", "case_close"}) {
            check(sounds != null && sounds.contains("\"sfx." + event + "\"") && sounds.contains("hexkagune:sfx/quinque/" + event + "_1")
                && sounds.contains("hexkagune:sfx/quinque/" + event + "_2"), "the case's " + event + " sound has its two takes");
            check(server.contains("\"hexkagune:sfx." + event + "\""), "the case plays its " + event + " sound");
            check(lang != null && lang.contains("\"subtitles.hexkagune.sfx." + event + "\""), "the case's " + event + " sound has a subtitle");
        }
        check(!server.contains("centipede_click"), "the case's latch is its own now, not a centipede's click");
        // 1.3.27: the owner: "a suitcase you can't [craft], definitely": no recipe makes any of them.
        Path recipes = RESOURCES.resolve("data/hexkagune/recipes");
        if (Files.isDirectory(recipes)) {
            // 1.3.40: every recipe, the kitchen's folder of them included.
            try (java.util.stream.Stream<Path> all = Files.walk(recipes).filter(Files::isRegularFile)) {
                for (Path recipe : (Iterable<Path>)all::iterator) {
                    String text = Files.readString(recipe);
                    for (Quinque q : Quinque.values()) {
                        check(!text.contains("hexkagune:" + q.caseId + "\"") && !text.contains("hexkagune:" + q.weaponId + "\""),
                            recipe.getFileName() + " never makes a quinque (" + q + ")");
                    }
                }
            }
        }
        check(lang != null && lang.contains("entity.hexkagune.ccg_investigator"), "the investigator has a name");
    }

    // ---------------------------------------------------------------- 1.3.33: how each is held and swung

    /** The model file's third person transform (rotation, translation, scale) for {@code frame}. */
    static double[] third(String frame) throws IOException {
        String json = read("assets/hexkagune/models/item/quinque/" + frame + ".json");
        Matcher m = Pattern.compile("\"thirdperson_righthand\"\\s*:\\s*\\{\\s*\"rotation\"\\s*:\\s*\\[([^\\]]*)\\]\\s*,"
            + "\\s*\"translation\"\\s*:\\s*\\[([^\\]]*)\\]\\s*,\\s*\"scale\"\\s*:\\s*\\[([^\\]]*)\\]").matcher(json == null ? "" : json);
        if (!m.find()) {
            return null;
        }
        double[] out = new double[7];
        int i = 0;
        for (int g = 1; g <= 3; g++) {
            String[] parts = m.group(g).split(",");
            for (int k = 0; k < (g == 3 ? 1 : 3); k++) {
                out[i++] = Double.parseDouble(parts[k].trim());
            }
        }
        return out;
    }

    /** How far {@code p} is from the segment {@code a}..{@code b}. */
    static double toSegment(double[] p, double[] a, double[] b) {
        double[] ab = {b[0] - a[0], b[1] - a[1], b[2] - a[2]};
        double len2 = ab[0] * ab[0] + ab[1] * ab[1] + ab[2] * ab[2];
        double t = len2 < 1.0E-9 ? 0.0 : Math.max(0.0, Math.min(1.0, ((p[0] - a[0]) * ab[0] + (p[1] - a[1]) * ab[1] + (p[2] - a[2]) * ab[2]) / len2));
        return distance(p, new double[] {a[0] + ab[0] * t, a[1] + ab[1] * t, a[2] + ab[2] * t});
    }

    static double distance(double[] a, double[] b) {
        return Math.sqrt((a[0] - b[0]) * (a[0] - b[0]) + (a[1] - b[1]) * (a[1] - b[1]) + (a[2] - b[2]) * (a[2] - b[2]));
    }

    /**
     * Each quinque's hold (QuinqueHolds, solved by tools/quinque_models.py) is the one its model is drawn with; its right grip sits in the
     * fist; a two-handed one's left hand is on its grip in its stance and stays near it through every swing (QuinqueGrip.reach); the
     * launcher's barrel lies along the look; the blade comes out of the fist, never along the arm; melee quinques fold at a tap.
     */
    static void holds() throws IOException {
        for (Quinque q : Quinque.values()) {
            com.hex.kagune.control.client.animation.QuinqueHolds.Hold h = com.hex.kagune.control.client.animation.QuinqueHolds.of(q);
            check(h != null, q + " has a hold");
            if (h == null) {
                continue;
            }
            for (String frame : new String[] {"full", "nearly", "half"}) {
                double[] d = third(q.model + "_" + frame);
                boolean same = d != null;
                for (int i = 0; same && i < 3; i++) {
                    same = Math.abs(d[i] - h.rotation()[i]) < 1.0E-3 && Math.abs(d[3 + i] - h.translation()[i]) < 1.0E-3;
                }
                check(same && Math.abs(d[6] - h.scale()) < 1.0E-3, q + ": its " + frame + " frame is drawn with the hold's transform");
            }
            com.hex.kagune.control.client.animation.RigPose stance = com.hex.kagune.control.client.animation.QuinquePose.hold(q);
            for (boolean slim : new boolean[] {false, true}) {
                double[] fist = com.hex.kagune.control.client.animation.QuinqueGrip.fist(stance, true, slim);
                double[] grip = com.hex.kagune.control.client.animation.QuinqueGrip.held(stance, h, slim, h.rightGrip());
                check(distance(fist, grip) < 0.5, q + ": the right grip in the middle of the fist (" + distance(fist, grip) + " px, slim " + slim + ")");
            }
            if (q.twoHanded) {
                com.hex.kagune.control.client.animation.RigPose p = com.hex.kagune.control.client.animation.QuinquePose.hold(q);
                float miss = com.hex.kagune.control.client.animation.QuinqueGrip.reach(p, q, false);
                double[] left = com.hex.kagune.control.client.animation.QuinqueGrip.fist(p, false, false);
                double[] a = com.hex.kagune.control.client.animation.QuinqueGrip.held(p, h, false, h.slideFrom());
                double[] b = com.hex.kagune.control.client.animation.QuinqueGrip.held(p, h, false, h.slideTo());
                double off = toSegment(left, a, b);
                check(Math.abs(miss) < 0.6F && off < 0.8, q + ": in its stance the left hand is on its grip (" + off + " px)");
                float worst = 0.0F;
                for (int step = 0; step < 3; step++) {
                    for (double t = 0.0; t <= 1.0; t += 0.05) {
                        com.hex.kagune.control.client.animation.RigPose swing = com.hex.kagune.control.client.animation.QuinquePose.swing(q, step, t);
                        worst = Math.max(worst, Math.abs(com.hex.kagune.control.client.animation.QuinqueGrip.reach(swing, q, false)));
                    }
                }
                check(worst < 3.0F, q + ": through every swing the left hand stays within a fist's half of its grip (" + worst + " px)");
            }
            // Every swing leaves the stance and comes back to it.
            for (int step = 0; step < 3; step++) {
                com.hex.kagune.control.client.animation.RigPose a = com.hex.kagune.control.client.animation.QuinquePose.swing(q, step, 0.0);
                com.hex.kagune.control.client.animation.RigPose m = com.hex.kagune.control.client.animation.QuinquePose.swing(q, step, 0.5);
                com.hex.kagune.control.client.animation.RigPose b = com.hex.kagune.control.client.animation.QuinquePose.swing(q, step, 1.0);
                double moved = distance(com.hex.kagune.control.client.animation.QuinqueGrip.fist(m, true, false),
                    com.hex.kagune.control.client.animation.QuinqueGrip.fist(stance, true, false)) + Math.abs(m.twist - stance.twist) * 8.0 + Math.abs(m.lunge);
                check(moved > 2.0, q + ": swing " + (step + 1) + " moves");
                check(distance(com.hex.kagune.control.client.animation.QuinqueGrip.fist(a, true, false), com.hex.kagune.control.client.animation.QuinqueGrip.fist(stance, true, false)) < 0.05
                    && distance(com.hex.kagune.control.client.animation.QuinqueGrip.fist(b, true, false), com.hex.kagune.control.client.animation.QuinqueGrip.fist(stance, true, false)) < 0.05,
                    q + ": swing " + (step + 1) + " starts and ends in the stance");
            }
        }
        // The launcher: a rifle. Its barrel along the look (the chest's turn taken out), level, the muzzle well out in front.
        com.hex.kagune.control.client.animation.QuinqueHolds.Hold gun = com.hex.kagune.control.client.animation.QuinqueHolds.of(Quinque.LAUNCHER);
        com.hex.kagune.control.client.animation.RigPose aim = com.hex.kagune.control.client.animation.QuinquePose.hold(Quinque.LAUNCHER);
        aim.spin = 0.0F; // (the whole body's share of the turn: undone here so the look is the model's -z)
        aim.twist = gun.twist();
        aim.headY = 0.0F;
        double[] breech = com.hex.kagune.control.client.animation.QuinqueGrip.held(aim, gun, false, new float[] {1.5F, 9.8F, 8.0F});
        double[] muzzle = com.hex.kagune.control.client.animation.QuinqueGrip.held(aim, gun, false, new float[] {31.0F, 9.8F, 8.0F});
        double length = distance(breech, muzzle);
        check(length > 15.0 && Math.abs(muzzle[0] - breech[0]) < 0.12 * length && Math.abs(muzzle[1] - breech[1]) < 0.12 * length
            && muzzle[2] < breech[2] - 0.9 * length, "the launcher's barrel lies along the look, level, at the target");
        double[] stock = com.hex.kagune.control.client.animation.QuinqueGrip.held(aim, gun, false, new float[] {-6.0F, 9.0F, 8.0F});
        check(stock[1] > -2.0 && stock[1] < 6.0, "its stock at the height of the shoulder (" + stock[1] + ")");
        // The blade: its point out of the fist, never back along the arm (the owner: "who grabs a sword by the tip of it").
        com.hex.kagune.control.client.animation.QuinqueHolds.Hold blade = com.hex.kagune.control.client.animation.QuinqueHolds.of(Quinque.BLADE);
        for (int step = 0; step < 3; step++) {
            for (double t = 0.0; t <= 1.0; t += 0.1) {
                com.hex.kagune.control.client.animation.RigPose p = com.hex.kagune.control.client.animation.QuinquePose.swing(Quinque.BLADE, step, t);
                double[] fist = com.hex.kagune.control.client.animation.QuinqueGrip.fist(p, true, false);
                double[] tip = com.hex.kagune.control.client.animation.QuinqueGrip.held(p, blade, false, new float[] {(float)(8 + 23 * Math.sqrt(0.5)), (float)(8 + 23 * Math.sqrt(0.5)), 8F});
                float[] turn = p.transform("rightArm", com.hex.kagune.control.client.animation.RigPose.ROTATION, 0, 0, 0);
                double cx = Math.cos(turn[0]), sx = Math.sin(turn[0]), cy = Math.cos(turn[1]), sy = Math.sin(turn[1]), cz = Math.cos(turn[2]), sz = Math.sin(turn[2]);
                // The arm's own length (Rz Ry Rx of +y).
                double[] along = {cz * sy * sx - sz * cx, sz * sy * sx + cz * cx, cy * sx};
                double[] out = {tip[0] - fist[0], tip[1] - fist[1], tip[2] - fist[2]};
                double n = Math.sqrt(out[0] * out[0] + out[1] * out[1] + out[2] * out[2]);
                double cos = (out[0] * along[0] + out[1] * along[1] + out[2] * along[2]) / n;
                check(cos > Math.cos(Math.toRadians(75.0)) && cos < Math.cos(Math.toRadians(25.0)),
                    "the blade leaves the fist 25..75 degrees off the forearm's line (swing " + (step + 1) + " at " + t + ")");
            }
        }
        // A tap folds a melee quinque; the launcher fires on it and folds on Shift.
        String server = Files.readString(Path.of("src/main/java/com/hex/kagune/control/server/QuinqueServer.java"));
        check(server.contains("boolean folds = open != null && (!open.ranged || player.m_6047_())") && server.contains("tapped("),
            "a tap of right click folds a melee quinque (held down, it never flips back)");
    }
}
