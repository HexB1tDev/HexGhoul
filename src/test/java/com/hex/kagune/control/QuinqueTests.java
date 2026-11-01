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
            heat = Quinque.heated(heat, 4L);
            shots++;
        }
        check(shots >= 25 && shots <= 40, "held down (a shard every four ticks) it overheats after a few seconds of fire: " + shots + " shards");
        heat = 0;
        for (int burst = 0; burst < 20; burst++) {
            for (int shot = 0; shot < 5; shot++) {
                heat = Quinque.heated(heat, 4L);
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
                check(frames == 4 && previous > 0.9 && previous <= 1.0, item + ": four frames past its shut case, the last all the way out");
                Matcher parent = Pattern.compile("\"parent\"\\s*:\\s*\"hexkagune:([a-z_/]+)\"").matcher(json);
                check(parent.find() && read("assets/hexkagune/models/" + parent.group(1) + ".json") != null, item + ": its shut case model exists");
                check(lang != null && lang.contains("\"item.hexkagune." + item + "\""), item + " has a name");
            }
            check(lang != null && lang.contains("item.hexkagune.quinque." + q.model + ".lore"), q + " has its lore line");
        }
        check(lang != null && lang.contains("item.hexkagune.quinque.open") && lang.contains("item.hexkagune.quinque.close")
            && lang.contains("item.hexkagune.quinque.ghoul"), "the quinques' use lines are named");
        // 1.3.27: the owner: "a suitcase you can't [craft], definitely": no recipe makes any of them.
        Path recipes = RESOURCES.resolve("data/hexkagune/recipes");
        if (Files.isDirectory(recipes)) {
            try (java.util.stream.Stream<Path> all = Files.list(recipes)) {
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
}
