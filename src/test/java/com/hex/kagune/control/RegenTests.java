package com.hex.kagune.control;

import com.hex.kagune.control.wound.Regeneration;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 4.2.2 (the owner: "regeneration is consuming way too much energy, and no longer is it automatic. We're gonna bind a new key for all Kagunes
 * that is called regeneration, except for the Bikaku ... You need to hold the key down, and it no longer consumes as much energy"; "just
 * slow down the regeneration in general. Even when holding the key down, make sure it's slower than what it currently is"; "Every single
 * form has this key there."): regeneration only while the key is held, slower, a fifth of the price, the key on every form's panel.
 */
public final class RegenTests {
    private static int passed;
    private static final List<String> FAILURES = new ArrayList<>();

    private static void check(boolean condition, String description) {
        if (condition) {
            passed++;
        } else {
            FAILURES.add(description);
        }
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }

    public static void main(String[] args) throws Exception {
        rate();
        price();
        held();
        panels();
        if (!FAILURES.isEmpty()) {
            FAILURES.forEach(failure -> System.out.println("FAILED: " + failure));
            System.exit(1);
        }
        System.out.println("Regen tests: " + passed + " checks passed");
    }

    /** Slower than before: for every mastery, any wound past a heart and a quarter comes back slower than the old burst did. */
    private static void rate() {
        for (int m = 0; m <= 1000; m += 25) {
            float held = Regeneration.heldPerTick(m, false, 1.0);
            for (float wound = 2.5F; wound <= 40.0F; wound += 0.5F) {
                float old = wound / Regeneration.healTicks(m);
                check(held <= old + 1e-6F, "mastery " + m + ", a wound of " + wound + ": " + held + " a tick held, no faster than the old " + old);
            }
            // A full bar takes longer held than it took with the old wait and burst.
            float fullHeld = 20.0F / held;
            check(fullHeld >= Regeneration.waitTicks(m, false) + Regeneration.healTicks(m), "a full bar slower than before at mastery " + m);
        }
        check(Math.abs(Regeneration.heldPerTick(0, false, 1.0) * 20.0F - 0.5F) < 1e-6 && Math.abs(Regeneration.heldPerTick(1000, false, 1.0) * 20.0F - 1.25F) < 1e-6,
            "half a point a second at mastery 0, a point and a quarter at 1000");
        check(Regeneration.heldPerTick(500, true, 1.0) > Regeneration.heldPerTick(500, false, 1.0), "the Regenerative Reservoir regenerates faster");
        check(Regeneration.heldPerTick(500, false, 2.0) > Regeneration.heldPerTick(500, false, 1.0), "Graft DNA's faster healing, kagune out");
        check(Regeneration.heldPerTick(500, false, 0.0) == 0.0F, "no speed, no healing");
    }

    /** A fifth of the old price: 20 health back costs 2 of the reserve's 20 (it cost 10). */
    private static void price() {
        check(Math.abs(GhoulReserve.REGEN_PER_HEALTH - GhoulReserve.PER_HEALTH * 0.2F) < 1e-6 && GhoulReserve.REGEN_PER_HEALTH <= 0.1F + 1e-6,
            "a fifth of the old price a point");
        check(Math.abs(GhoulReserve.afterHeal(20.0F, 20.0F, GhoulReserve.REGEN_PER_HEALTH) - 18.0F) < 1e-4, "a full bar of health back costs 2 of 20");
        check(Math.abs(GhoulReserve.healable(1.0F, 20.0F, GhoulReserve.REGEN_PER_HEALTH) - 10.0F) < 1e-4, "1 left of the reserve pays for 10 health");
        check(GhoulReserve.healable(0.0F, 5.0F, GhoulReserve.REGEN_PER_HEALTH) == 0.0F, "an empty reserve pays for none");
    }

    /** Only while held; never a Bikaku's (its Monstrous Regeneration is its own); the key told as it goes and while it stays down. */
    private static void held() throws Exception {
        String regen = read("src/main/java/com/hex/kagune/control/server/GhoulRegen.java");
        check(regen.contains("if (health >= max || !holding(player) || BikakuServer.ownRegeneration(player)) {\n            return;"),
            "no regeneration unless the key is held; never a Bikaku's this way once Monstrous Regeneration is his");
        check(read("src/main/java/com/hex/kagune/control/server/BikakuServer.java").contains("return isBikaku(player) && com.hex.kagune.control.bikaku.BikakuRules.ownRegeneration(KaguneProfile.mastery(player),\n            QuestServer.open(player, com.hex.kagune.control.quest.Gate.BIKAKU_WORMS));"),
            "Monstrous Regeneration his by his mastery and Noro's lesson (asked quietly)");
        check(regen.contains("boolean diet = !BikakuServer.isBikaku(player) && GhoulReserveServer.applies(player);"), "a Bikaku's held key costs nothing");
        check(regen.contains("float amount = Math.min(max - health, Regeneration.heldPerTick(mastery, reservoir, speed));")
            && !regen.contains("Regeneration.waitTicks(") && !regen.contains("missing[0] / heal"), "the held rate, no wait, no burst");
        check(regen.contains("amount = GhoulReserve.healable(reserve, amount, GhoulReserve.REGEN_PER_HEALTH);")
            && regen.contains("GhoulReserve.afterHeal(reserve, amount, GhoulReserve.REGEN_PER_HEALTH)")
            && regen.contains("* com.hex.kagune.control.graft.GraftRules.REGEN_COST * Regeneration.PRICE_SHARE);"), "paid at a fifth");
        check(regen.contains("if (down) {\n            HELD.put(player.m_20148_(), player.m_9236_().m_46467_());\n        } else {\n            HELD.remove(player.m_20148_());"),
            "let go, it stops at once");
        check(regen.contains("return at != null && now >= at && now - at <= HELD_FOR;") && regen.contains("HELD_FOR = 30L;"),
            "held only while told within a second and a half (a client gone quiet stops)");
        check(regen.contains("if (now < held) {\n                return; // 1.3.25: a quinque's cut"), "a quinque's cut still holds it back");
        // War's and Peace's own mend (1 health a second, free) was the one other heal on a timer: now it too only with the key held.
        check(read("src/main/java/com/hex/kagune/control/server/KingServer.java").contains(
            "if (now % MEND_TICKS == 0L && player.m_21223_() < player.m_21233_() && player.m_6084_() && GhoulRegen.holding(player)) {"),
            "War's and Peace's mend only while the key is held");
        check(regen.contains("private static final Map<UUID, Long> HELD = WorldTimes.map();"), "the held key forgotten with its world");
        // The other heals on a timer never land by themselves on a ghoul: a heal of 1.5 or less the mod didn't make through GhoulRegen is
        // taken for food's and cancelled. The original's Regen Reservoir heal (1, 1.5 in the full kakuja) and War's and Peace's mend (1).
        check(com.hex.kagune.control.wound.Regeneration.natural(1.0F, false, false) && com.hex.kagune.control.wound.Regeneration.natural(1.5F, false, false)
            && !com.hex.kagune.control.wound.Regeneration.natural(1.0F, false, true) && !com.hex.kagune.control.wound.Regeneration.natural(1.0F, true, false),
            "the original's Reservoir heal and War's mend are cancelled; ours and a regeneration potion's are not");
        check(regen.contains("&& Regeneration.natural(event.getAmount(), player.m_21023_(MobEffects.f_19605_), healing)) {\n            event.setCanceled(true);"),
            "and cancelled on every ghoul");
        check(read("src/main/java/com/hex/kagune/control/server/KingServer.java").contains("private static final float MEND = 1.0F;"),
            "War's mend small enough to be cancelled");
        String bikaku = read("src/main/java/com/hex/kagune/control/server/BikakuServer.java");
        check(bikaku.contains("GhoulRegen.healOwn(player, BikakuRules.REGEN_HEAL);"), "a Bikaku's Monstrous Regeneration heals by itself");
        String client = read("src/main/java/com/hex/kagune/control/client/RegenClient.java");
        check(client.contains("if (down != was || down && ticks % REPEAT == 0) {\n            ControlNetwork.CHANNEL.sendToServer(new C2SRegen(down));")
            && client.contains("REPEAT = 10;"), "told as it goes down or up, and every half second held");
        check(client.contains("minecraft.f_91080_ == null && ControlKeys.REGENERATE.m_90857_()"), "never held behind a screen");
        String keys = read("src/main/java/com/hex/kagune/control/client/ControlKeys.java");
        check(keys.contains("KEY_X = 88;") && keys.contains("new KeyMapping(\"key.hexkagune.regenerate\", InputConstants.Type.KEYSYM, KEY_X,")
            && keys.contains("event.register(REGENERATE);"), "the key, X by default, in Controls");
        check(read("src/main/resources/assets/hexkagune/lang/en_us.json").contains("\"key.hexkagune.regenerate\": \"Regenerate (hold)\""), "named in Controls");
        String network = read("src/main/java/com/hex/kagune/control/network/ControlNetwork.java");
        check(network.contains("registerMessage(59, C2SRegen.class") && network.contains("PROTOCOL = \"129\""), "message 59, protocol 129");
        check(read("src/main/java/com/hex/kagune/control/network/C2SRegen.java").contains("GhoulRegen.held(sender, message.down)"), "the server told");
    }

    /** The key on the panel in every state and form but Bikaku's: eyes shut, the kakugan, the kagune out, every kakuja, War, Peace. */
    private static void panels() {
        Map<AbilityGuide.Key, String> keys = new EnumMap<>(AbilityGuide.Key.class);
        for (AbilityGuide.Key key : AbilityGuide.Key.values()) {
            keys.put(key, key.name());
        }
        keys.put(AbilityGuide.Key.REGEN, "X");
        int panels = 0;
        for (KagunePath path : new KagunePath[] {KagunePath.RINKAKU, KagunePath.UKAKU, KagunePath.PALE, KagunePath.GRAFT, KagunePath.BIKAKU}) {
            for (AbilityGuide.State state : AbilityGuide.State.values()) {
                for (com.hex.kagune.gameplay.KaguneForm form : com.hex.kagune.gameplay.KaguneForm.values()) {
                    for (com.hex.kagune.control.ukaku.WingForm wings : com.hex.kagune.control.ukaku.WingForm.values()) {
                        for (boolean graftKakuja : new boolean[] {false, true}) {
                            AbilityGuide.Panel panel = AbilityGuide.panel(new AbilityGuide.Input(path, state, 1000, form, wings, false, keys, false,
                                com.hex.kagune.control.graft.GraftStage.KAKUJA, graftKakuja));
                            boolean shown = has(panel) && has(AbilityGuide.brief(panel));
                            panels++;
                            if (path == KagunePath.BIKAKU) {
                                // At 1000 with every lesson taught Monstrous Regeneration is his: that, not the key. Before it is, the key.
                                check(!has(panel), "Bikaku's panel keeps its own (Monstrous Regeneration): " + state);
                                AbilityGuide.Panel young = AbilityGuide.panel(new AbilityGuide.Input(path, state, com.hex.kagune.control.bikaku.BikakuRules.REGENERATION - 1,
                                    form, wings, false, keys, false, com.hex.kagune.control.graft.GraftStage.KAKUJA, graftKakuja));
                                AbilityGuide.Panel untaught = AbilityGuide.panel(new AbilityGuide.Input(path, state, 1000, form, wings, false, keys, false,
                                    com.hex.kagune.control.graft.GraftStage.KAKUJA, graftKakuja, java.util.EnumSet.of(com.hex.kagune.control.quest.Gate.BIKAKU_WORMS)));
                                check(has(young) && has(AbilityGuide.brief(young)) && has(untaught), "Bikaku before Monstrous Regeneration is his: the key: " + state);
                                // 4.2.2: and no passive regeneration offered (the worm-out "Fast regeneration" went with a ghoul's own).
                                check(panel.rows().stream().noneMatch(row -> row.name().toLowerCase(java.util.Locale.ROOT).contains("fast regen"))
                                    && AbilityGuide.brief(panel).rows().stream().noneMatch(row -> row.name().toLowerCase(java.util.Locale.ROOT).contains("fast regen")),
                                    "Bikaku's panel offers no passive regeneration: " + state);
                            } else {
                                check(shown, path + ", " + state + ", " + form + ", " + wings + (graftKakuja ? ", kakuja" : "") + ": the regeneration key shown");
                            }
                        }
                    }
                }
            }
        }
        check(panels > 100, "every path, state and form looked at (" + panels + ")");
        for (boolean peace : new boolean[] {false, true}) {
            check(has(AbilityGuide.king(keys, peace)), (peace ? "Peace" : "War") + ": the regeneration key shown");
        }
    }

    private static boolean has(AbilityGuide.Panel panel) {
        for (AbilityGuide.Row row : panel.rows()) {
            if (row.keys().equals("X hold") && row.name().equals("Regenerate")) {
                return true;
            }
        }
        return false;
    }
}
