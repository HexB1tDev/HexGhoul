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
 * texture, the property they are chosen by, its name and its recipe.
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
        check(Math.abs(Quinque.SPEAR.attackDamage() - 10.0F) < 1.0E-4F && Math.abs(Quinque.SPEAR.attacksPerSecond() - 1.1F) < 1.0E-4F,
            "the Koukaku spear: 10 damage, 1.1 swings a second (heavy)");
        check(Quinque.SPEAR.reach > 1.0 && Quinque.BLADE.reach == 0.0, "only the spear reaches further");
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
            String recipe = read("data/hexkagune/recipes/" + q.caseId + ".json");
            check(recipe != null && recipe.contains("\"hexkagune:" + q.caseId + "\""), q + "'s case can be crafted");
            check(lang != null && lang.contains("item.hexkagune.quinque." + q.type + ".lore"), q + " has its lore line");
        }
        check(lang != null && lang.contains("item.hexkagune.quinque.open") && lang.contains("item.hexkagune.quinque.close")
            && lang.contains("item.hexkagune.quinque.ghoul"), "the quinques' use lines are named");
    }
}
