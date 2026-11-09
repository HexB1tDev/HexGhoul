package com.hex.kagune.control;

import com.hex.kagune.control.bikaku.BikakuRules;
import com.hex.kagune.control.bikaku.BikakuRules.Bite;
import com.hex.kagune.control.client.PathCards;
import java.util.EnumMap;
import java.util.Map;

/**
 * 3.4.9: the Bikaku path in the game (pure parts): its place among the paths, its M1 string of bites, what a bite does, Predatory
 * Feeding, its card, its move guide and its plan's gates. The worm itself is BikakuWormTests'.
 */
public final class BikakuTests {
    private static int passed;

    private static void check(boolean good, String message) {
        if (!good) {
            throw new AssertionError(message);
        }
        passed++;
    }

    public static void main(String[] args) {
        path();
        string();
        bites();
        card();
        guide();
        moves();
        regeneration();
        kakujaMoves();
        held();
        mutations();
        cooldownsOff();
        System.out.println("Bikaku tests: " + passed + " checks passed");
    }

    /**
     * 3.6.4 (the owner, on /kagune cooldowns off not ending the Bikaku cooldowns already running: "it should."): every path's server that
     * keeps cooldowns of its own ({@code static void clearCooldowns(ServerPlayer)}) is cleared by {@code Cooldowns.clear}; the Bikaku
     * path's clears its moves', the kakuja's and Grand Devouring's.
     */
    private static void cooldownsOff() {
        try {
            java.nio.file.Path server = java.nio.file.Paths.get("src/main/java/com/hex/kagune/control/server");
            String cooldowns = new String(java.nio.file.Files.readAllBytes(server.resolve("Cooldowns.java")), java.nio.charset.StandardCharsets.UTF_8);
            String clear = cooldowns.substring(cooldowns.indexOf("static void clear(ServerPlayer player)"));
            clear = clear.substring(0, clear.indexOf("\n    }\n"));
            int paths = 0;
            try (java.util.stream.Stream<java.nio.file.Path> files = java.nio.file.Files.list(server)) {
                for (java.nio.file.Path file : (Iterable<java.nio.file.Path>)files.filter(f -> f.toString().endsWith(".java"))::iterator) {
                    String source = new String(java.nio.file.Files.readAllBytes(file), java.nio.charset.StandardCharsets.UTF_8);
                    if (source.contains("static void clearCooldowns(ServerPlayer player)")) {
                        String name = file.getFileName().toString().replace(".java", "");
                        check(clear.contains(name + ".clearCooldowns(player)"), "/kagune cooldowns off ends " + name + "'s running cooldowns");
                        paths++;
                    }
                }
            }
            check(paths >= 4, "every path's cooldowns are found (" + paths + ")");
            String bikaku = new String(java.nio.file.Files.readAllBytes(server.resolve("BikakuServer.java")), java.nio.charset.StandardCharsets.UTF_8);
            String reset = bikaku.substring(bikaku.indexOf("static void clearCooldowns(ServerPlayer player)"));
            reset = reset.substring(0, reset.indexOf("\n    }\n"));
            check(reset.contains("readyAt.clear()") && reset.contains("kakujaReadyAt = Long.MIN_VALUE")
                && reset.contains("devourReadyAt = Long.MIN_VALUE"), "the Bikaku path's moves', kakuja's and Grand Devouring's cooldowns all end");
        } catch (java.io.IOException failure) {
            throw new AssertionError("the sources can't be read: " + failure);
        }
    }

    /**
     * 3.6.3: the Bikaku mutations, unlocked by a quest (the owner: "these mutations are NOT unlocked through mastery"): read back by their
     * ids, a bit each; Grand Devouring built, Potential Unlock still to come; its cooldown a minute (the owner: "Grand Devour should have a
     * cooldown of 1 minute"); M1's bite passed over the feeding worm to the next one, or none in One Worm.
     */
    private static void mutations() {
        com.hex.kagune.control.bikaku.BikakuMutation devour = com.hex.kagune.control.bikaku.BikakuMutation.GRAND_DEVOURING;
        com.hex.kagune.control.bikaku.BikakuMutation unlock = com.hex.kagune.control.bikaku.BikakuMutation.POTENTIAL_UNLOCK;
        check(com.hex.kagune.control.bikaku.BikakuMutation.byId("grand_devouring") == devour
            && com.hex.kagune.control.bikaku.BikakuMutation.byId("potential_unlock") == unlock
            && com.hex.kagune.control.bikaku.BikakuMutation.byId("nope") == null, "each mutation reads back by its id");
        check(devour.bit() != unlock.bit() && devour.in(devour.bit()) && !unlock.in(devour.bit())
            && com.hex.kagune.control.bikaku.BikakuMutation.all() == (devour.bit() | unlock.bit()), "a bit each");
        check(devour.built() && unlock.built(), "both are built (3.6.7: Potential Unlock's first part)");
        // 3.6.7, Potential Unlock (the owner: "The maximum energy capacity is also permanently multiplied by 3"; "Should be Purple, the bar,
        // my bad"): three times the RC with it, once without; Grand Devouring alone changes nothing.
        check(com.hex.kagune.control.bikaku.BikakuRules.capacity(0) == 1 && com.hex.kagune.control.bikaku.BikakuRules.capacity(devour.bit()) == 1
            && com.hex.kagune.control.bikaku.BikakuRules.capacity(unlock.bit()) == 3
            && com.hex.kagune.control.bikaku.BikakuRules.capacity(devour.bit() | unlock.bit()) == 3, "Potential Unlock: three times the RC");
        int purple = com.hex.kagune.control.bikaku.BikakuRules.POTENTIAL_PURPLE;
        int pr = purple >> 16 & 255;
        int pg = purple >> 8 & 255;
        int pb = purple & 255;
        check(pb > pg + 100 && pr > pg + 60, "its bar's colour is purple (red and blue, little green)");
        // The bar drawn through the tint comes out purple: each of the original bar's reds turned to purple's hue.
        for (int red : new int[] {0xFFE64045, 0xFFE05B4C, 0xFFB8173E, 0xD92A0712, 0xFFC8204E}) {
            int c = com.hex.kagune.control.KaguneTint.recolour(red, purple);
            int r = c >> 16 & 255;
            int g = c >> 8 & 255;
            int b = c & 255;
            check(b > r * 0.7 && b > g && (c >>> 24) == (red >>> 24), "a red of the bar turns purple, its alpha kept (" + Integer.toHexString(c) + ")");
        }
        check(com.hex.kagune.control.bikaku.BikakuMutation.byOrdinal(0) == devour && com.hex.kagune.control.bikaku.BikakuMutation.byOrdinal(9) == null,
            "read back by ordinal");
        for (com.hex.kagune.control.bikaku.BikakuMutation mutation : com.hex.kagune.control.bikaku.BikakuMutation.values()) {
            String shown = (mutation.displayName() + " " + mutation.description()).toLowerCase(java.util.Locale.ROOT);
            check(!shown.contains("noro") && !shown.contains("mastery"), "no character's name, and no mastery, in what it shows: " + mutation);
        }
        check(com.hex.kagune.control.bikaku.BikakuRules.DEVOUR_COOLDOWN == 1200, "Grand Devouring's cooldown is a minute");
        com.hex.kagune.control.bikaku.BikakuRules.Bite[] string = {com.hex.kagune.control.bikaku.BikakuRules.Bite.SNAP,
            com.hex.kagune.control.bikaku.BikakuRules.Bite.LUNGE, com.hex.kagune.control.bikaku.BikakuRules.Bite.MAUL};
        for (int worms = 1; worms <= 3; worms++) {
            for (com.hex.kagune.control.bikaku.BikakuRules.Bite bite : string) {
                check(com.hex.kagune.control.bikaku.BikakuForm.biter(bite, worms, -1) == com.hex.kagune.control.bikaku.BikakuForm.biter(bite, worms),
                    "with no worm feeding the bites take turns as before");
                for (int busy = 0; busy < worms; busy++) {
                    int biter = com.hex.kagune.control.bikaku.BikakuForm.biter(bite, worms, busy);
                    check(biter != busy, "the feeding worm never bites (" + worms + " worms, " + busy + " feeding)");
                    check(worms == 1 ? biter == -1 : biter >= 0 && biter < worms, "another worm bites in its place, none in One Worm");
                }
            }
        }
    }

    /** A path to choose on the starting screen, saved by its own id, named by its kagune type (no character's name on screen). */
    private static void path() {
        check(KagunePath.choosable().contains(KagunePath.BIKAKU) && !KagunePath.BIKAKU.dormant() && KagunePath.BIKAKU.chosen(),
            "Bikaku can be chosen");
        check(KagunePath.fromId("bikaku") == KagunePath.BIKAKU && KagunePath.fromId(" BIKAKU ") == KagunePath.BIKAKU, "its saved id reads back");
        check(KagunePath.byOrdinal(KagunePath.BIKAKU.ordinal()) == KagunePath.BIKAKU, "its network ordinal reads back");
        check(KagunePath.BIKAKU.ordinal() == KagunePath.values().length - 1, "appended last: the paths before it keep their ordinals");
        check(KagunePath.BIKAKU.displayName().equals("Bikaku") && !KagunePath.BIKAKU.displayName().toLowerCase(java.util.Locale.ROOT).contains("noro"),
            "named by its kagune type");
    }

    /** M1: Snap, Lunge, Maul, then Snap again; each lands partway through; the Maul heavier and stunning. */
    private static void string() {
        check(Bite.NONE.next() == Bite.SNAP && Bite.SNAP.next() == Bite.LUNGE && Bite.LUNGE.next() == Bite.MAUL && Bite.MAUL.next() == Bite.SNAP,
            "the string goes Snap, Lunge, Maul and round again");
        for (Bite bite : Bite.values()) {
            check(Bite.byOrdinal(bite.ordinal()) == bite, "each bite reads back by its ordinal");
            if (bite == Bite.NONE) {
                continue;
            }
            check(bite.land() > 0 && bite.land() < bite.ticks() && bite.cost() > 0.0F && bite.power() > 0.0F, bite + " has a landing, a price and a weight");
            check(bite.ticks() <= 20, bite + " is quick enough for M1 (" + bite.ticks() + " ticks)");
        }
        check(Bite.byOrdinal(-1) == Bite.NONE && Bite.byOrdinal(99) == Bite.NONE, "anything unknown is no bite");
        check(Bite.MAUL.heavy() && !Bite.SNAP.heavy() && !Bite.LUNGE.heavy() && Bite.MAUL.power() > Bite.SNAP.power(), "the Maul is the heavy one");
        check(Bite.SNAP.style() != Bite.LUNGE.style() && Bite.LUNGE.style() != Bite.MAUL.style(), "each bite comes its own way");
        check(BikakuRules.CHAIN_WINDOW > 0 && BikakuRules.BUFFER > 0, "a click soon after a bite goes on with the string");
    }

    /** Damage by mastery (a player takes a share), bleeding, a stun, healing on a bite and more on a kill, a long reach. */
    private static void bites() {
        float last = 0.0F;
        boolean rises = true;
        for (int m = 0; m <= 1000; m += 50) {
            float damage = BikakuRules.damage(m);
            rises &= damage > last;
            last = damage;
        }
        check(rises && BikakuRules.damage(0) > 3.0F, "a bite hurts, more as mastery grows");
        check(BikakuRules.PVP_SHARE > 0.0F && BikakuRules.PVP_SHARE < 1.0F, "another player takes a share");
        check(BikakuRules.bleedStacks(Bite.MAUL) > BikakuRules.bleedStacks(Bite.SNAP) && BikakuRules.bleedStacks(Bite.SNAP) > 0, "bites bleed, the Maul more");
        check(BikakuRules.STUN > 0, "the Maul stuns");
        check(BikakuRules.BITE_HEAL > 0.0F && BikakuRules.KILL_HEAL > BikakuRules.BITE_HEAL && BikakuRules.BITE_HEAL <= 2.0F,
            "Predatory Feeding: a bite heals a little, a kill more");
        // 3.5.2: a ghoul's heals of 1.5 or less are taken for food's natural regeneration and cancelled unless the mod marks them its own
        // (GhoulRegen.healOwn); a bite's half a heart is one of them.
        check(com.hex.kagune.control.wound.Regeneration.natural(BikakuRules.BITE_HEAL, false, false)
            && !com.hex.kagune.control.wound.Regeneration.natural(BikakuRules.BITE_HEAL, false, true), "a bite's heal must go through as the mod's own");
        check(BikakuRules.REACH >= 5.0 && BikakuRules.MOUTH_RADIUS > 0.5, "the worm reaches far");
        double[] near = {0.3, 1.0, 2.0};
        check(BikakuRules.reach(near) == near, "something in reach is bitten where it is");
        int a = BikakuRules.seed(123L, 456L);
        check(a == BikakuRules.seed(123L, 456L) && a >= 0 && a < 0x8000, "each player's worm is its own, the same on every client");
        check(BikakuRules.MAW == 0 && BikakuRules.MAW < BikakuRules.DRAG && BikakuRules.DRAG < BikakuRules.PURSUIT
            && BikakuRules.PURSUIT < BikakuRules.ERUPTION && BikakuRules.ERUPTION < BikakuRules.REGENERATION
            && BikakuRules.REGENERATION < BikakuRules.KAKUJA && BikakuRules.KAKUJA == 1000, "the plan's moves unlock in order, the kakuja at 1000");
    }

    /** Its starting-screen card: its own, its stages with their mastery, no character's name. */
    private static void card() {
        PathCards.Card card = PathCards.of(KagunePath.BIKAKU);
        check(card.path() == KagunePath.BIKAKU && card.name().equals("BIKAKU"), "Bikaku has its own card");
        check(card.stageNames().size() == card.stageMastery().size() && card.stageMastery().get(0) == 0, "its stage strip starts with the worm");
        boolean ascending = true;
        for (int i = 1; i < card.stageMastery().size(); i++) {
            ascending &= card.stageMastery().get(i) > card.stageMastery().get(i - 1);
        }
        check(ascending, "its stages come in mastery order");
        StringBuilder text = new StringBuilder(card.name() + card.type() + card.tags() + card.awaken());
        card.bullets().forEach(text::append);
        card.stageNames().forEach(text::append);
        check(!text.toString().matches("(?is).*(noro|takatsuki|aogiri|yoshimura|kaneki|ayato|furuta).*"), "its card names no one");
    }

    /** The move guide: the worm's bites on M1 while it is out; the path's name while it is in. */
    private static void guide() {
        Map<AbilityGuide.Key, String> keys = new EnumMap<>(AbilityGuide.Key.class);
        for (AbilityGuide.Key key : AbilityGuide.Key.values()) {
            keys.put(key, key == AbilityGuide.Key.ATTACK ? "LMB" : key.name());
        }
        AbilityGuide.Panel out = AbilityGuide.panel(new AbilityGuide.Input(KagunePath.BIKAKU, AbilityGuide.State.OUT, 0,
            com.hex.kagune.gameplay.KaguneForm.RINKAKU_SEED, com.hex.kagune.control.ukaku.WingForm.TWIN, false, keys));
        check(out.title().equals("BIKAKU") && out.rows().get(0).keys().equals("LMB") && out.rows().get(0).name().startsWith("Bites"),
            "kagune out: the bites are on M1");
        check(out.rows().stream().noneMatch(row -> row.name().equals("Grab") || row.name().startsWith("Thy Death") || row.name().startsWith("Rubble")),
            "no Rinkaku moves on the board");
        AbilityGuide.Panel brief = AbilityGuide.brief(out);
        check(brief.rows().size() == out.rows().size() && brief.rows().get(0).name().equals("Bites"), "in brief: Bites");
        AbilityGuide.Panel in = AbilityGuide.panel(new AbilityGuide.Input(KagunePath.BIKAKU, AbilityGuide.State.CLOSED, 0,
            com.hex.kagune.gameplay.KaguneForm.RINKAKU_SEED, com.hex.kagune.control.ukaku.WingForm.TWIN, false, keys));
        check(in.title().equals("BIKAKU"), "kagune in: the board is Bikaku's");
    }

    /** 3.5.0: the moves on their keys, opened by mastery alone, each landing partway through, cooling down longer than it lasts. */
    private static void moves() {
        java.util.Set<com.hex.kagune.control.bikaku.BikakuMove.Slot> slots = java.util.EnumSet.noneOf(com.hex.kagune.control.bikaku.BikakuMove.Slot.class);
        java.util.Set<com.hex.kagune.control.bikaku.BikakuMove.Slot> kakujaSlots = java.util.EnumSet.noneOf(com.hex.kagune.control.bikaku.BikakuMove.Slot.class);
        for (com.hex.kagune.control.bikaku.BikakuMove move : com.hex.kagune.control.bikaku.BikakuMove.values()) {
            check(com.hex.kagune.control.bikaku.BikakuMove.byOrdinal(move.ordinal()) == move, "each move reads back");
            if (move == com.hex.kagune.control.bikaku.BikakuMove.NONE) {
                continue;
            }
            // 3.5.7: the worm's moves each on its own key; the kakuja's on the same keys, each its own among them.
            check((move.kakuja() ? kakujaSlots : slots).add(move.slot()) && move.slot() != com.hex.kagune.control.bikaku.BikakuMove.Slot.NONE,
                move + " has its own key");
            check(com.hex.kagune.control.bikaku.BikakuMove.of(move.slot(), move.kakuja()) == move, move + " is what its key does");
            check(move.kakuja() || com.hex.kagune.control.bikaku.BikakuMove.of(move.slot()) == move, move + " is what its key does out of the kakuja");
            check(move.land() > 0 && move.land() < move.missedTicks() && move.missedTicks() <= move.ticks(), move + " bites partway through");
            check(move.cooldown() > move.ticks() && move.cost() > 0.0F, move + " cools down longer than it lasts, and costs RC");
            check(!move.unlocked(move.mastery() - 1) && move.unlocked(move.mastery()) && move.unlocked(1000), move + " opens at its mastery");
        }
        check(com.hex.kagune.control.bikaku.BikakuMove.of(com.hex.kagune.control.bikaku.BikakuMove.Slot.G) == com.hex.kagune.control.bikaku.BikakuMove.MAW
            && com.hex.kagune.control.bikaku.BikakuMove.of(com.hex.kagune.control.bikaku.BikakuMove.Slot.J) == com.hex.kagune.control.bikaku.BikakuMove.DRAG,
            "G is Devouring Maw, J Predatory Drag");
        check(com.hex.kagune.control.bikaku.BikakuMove.MAW.mastery() == BikakuRules.MAW && com.hex.kagune.control.bikaku.BikakuMove.DRAG.mastery() == BikakuRules.DRAG,
            "they open at the plan's mastery");
        // 3.5.1
        check(com.hex.kagune.control.bikaku.BikakuMove.of(com.hex.kagune.control.bikaku.BikakuMove.Slot.Z) == com.hex.kagune.control.bikaku.BikakuMove.PURSUIT
            && com.hex.kagune.control.bikaku.BikakuMove.of(com.hex.kagune.control.bikaku.BikakuMove.Slot.B) == com.hex.kagune.control.bikaku.BikakuMove.ERUPTION
            && com.hex.kagune.control.bikaku.BikakuMove.PURSUIT.mastery() == BikakuRules.PURSUIT
            && com.hex.kagune.control.bikaku.BikakuMove.ERUPTION.mastery() == BikakuRules.ERUPTION, "Z is Ravenous Pursuit (300), B Carrion Eruption (450)");
        check(!com.hex.kagune.control.bikaku.BikakuMove.PURSUIT.grabs() && com.hex.kagune.control.bikaku.BikakuMove.PURSUIT.missedTicks()
            == com.hex.kagune.control.bikaku.BikakuMove.PURSUIT.ticks() && com.hex.kagune.control.bikaku.BikakuMove.MAW.grabs(), "only the grabs draw back early");
        check(com.hex.kagune.control.bikaku.BikakuMove.pursuitLands(2) + com.hex.kagune.control.bikaku.BikakuMove.PURSUIT_AFTER
            <= com.hex.kagune.control.bikaku.BikakuMove.PURSUIT_MERGE + 1 && com.hex.kagune.control.bikaku.BikakuMove.pursuitLands(0)
            - com.hex.kagune.control.bikaku.BikakuMove.PURSUIT_LEAD >= com.hex.kagune.control.bikaku.BikakuMove.PURSUIT_SPLIT - 1,
            "Ravenous Pursuit's worms bite between its split and its merge");
        check(com.hex.kagune.control.bikaku.BikakuMove.ERUPT_DIVE < com.hex.kagune.control.bikaku.BikakuMove.ERUPT_RISE
            && com.hex.kagune.control.bikaku.BikakuMove.ERUPT_RISE < com.hex.kagune.control.bikaku.BikakuMove.ERUPTION.land()
            && com.hex.kagune.control.bikaku.BikakuMove.ERUPTION.land() < com.hex.kagune.control.bikaku.BikakuMove.ERUPT_HOLD
            && com.hex.kagune.control.bikaku.BikakuMove.ERUPT_HOLD < com.hex.kagune.control.bikaku.BikakuMove.ERUPT_SUNK
            && com.hex.kagune.control.bikaku.BikakuMove.ERUPT_SUNK < com.hex.kagune.control.bikaku.BikakuMove.ERUPT_BACK
            && com.hex.kagune.control.bikaku.BikakuMove.ERUPT_BACK < com.hex.kagune.control.bikaku.BikakuMove.ERUPTION.ticks(), "Carrion Eruption's beats come in order");
        check(com.hex.kagune.control.bikaku.BikakuMove.MAW_WINDUP < com.hex.kagune.control.bikaku.BikakuMove.MAW.land()
            && com.hex.kagune.control.bikaku.BikakuMove.MAW.land() < com.hex.kagune.control.bikaku.BikakuMove.MAW_PULLED
            && com.hex.kagune.control.bikaku.BikakuMove.MAW_PULLED < com.hex.kagune.control.bikaku.BikakuMove.MAW_SHAKE_END
            && com.hex.kagune.control.bikaku.BikakuMove.MAW_SHAKE_END < com.hex.kagune.control.bikaku.BikakuMove.MAW_RELEASE
            && com.hex.kagune.control.bikaku.BikakuMove.MAW_RELEASE < com.hex.kagune.control.bikaku.BikakuMove.MAW.ticks(), "Devouring Maw's beats come in order");
        check(com.hex.kagune.control.bikaku.BikakuMove.DRAG_DOWN < com.hex.kagune.control.bikaku.BikakuMove.DRAG.land()
            && com.hex.kagune.control.bikaku.BikakuMove.DRAG.land() < com.hex.kagune.control.bikaku.BikakuMove.DRAG_HOME
            && com.hex.kagune.control.bikaku.BikakuMove.DRAG_HOME < com.hex.kagune.control.bikaku.BikakuMove.DRAG_LIFTED
            && com.hex.kagune.control.bikaku.BikakuMove.DRAG_LIFTED < com.hex.kagune.control.bikaku.BikakuMove.DRAG_SLAM
            && com.hex.kagune.control.bikaku.BikakuMove.DRAG_SLAM < com.hex.kagune.control.bikaku.BikakuMove.DRAG.ticks(), "Predatory Drag's beats come in order");
        Map<AbilityGuide.Key, String> keys = new EnumMap<>(AbilityGuide.Key.class);
        for (AbilityGuide.Key key : AbilityGuide.Key.values()) {
            keys.put(key, key.name());
        }
        for (int mastery : new int[] {0, 149, 150, 1000}) {
            AbilityGuide.Panel out = AbilityGuide.panel(new AbilityGuide.Input(KagunePath.BIKAKU, AbilityGuide.State.OUT, mastery,
                com.hex.kagune.gameplay.KaguneForm.RINKAKU_SEED, com.hex.kagune.control.ukaku.WingForm.TWIN, false, keys));
            AbilityGuide.Row maw = out.rows().stream().filter(r -> r.name().equals("Devouring Maw")).findFirst().orElse(null);
            AbilityGuide.Row drag = out.rows().stream().filter(r -> r.name().equals("Predatory Drag")).findFirst().orElse(null);
            check(maw != null && maw.keys().equals("GRAB") && !maw.locked(), "the board shows Devouring Maw on G, open");
            check(drag != null && drag.keys().equals("SLINGSHOT") && drag.locked() == mastery < BikakuRules.DRAG, "the board shows Predatory Drag on J, dim until 150");
        }
    }

    /** 3.5.3: Monstrous Regeneration (C) and the passives; its strands on his body. */
    private static void regeneration() {
        com.hex.kagune.control.bikaku.BikakuMove regen = com.hex.kagune.control.bikaku.BikakuMove.REGENERATION;
        check(com.hex.kagune.control.bikaku.BikakuMove.of(com.hex.kagune.control.bikaku.BikakuMove.Slot.C) == regen && regen.mastery() == BikakuRules.REGENERATION,
            "C is Monstrous Regeneration, at mastery 600");
        check(!regen.occupies() && com.hex.kagune.control.bikaku.BikakuMove.MAW.occupies(), "it runs alongside the worm (the worm keeps biting)");
        float total = BikakuRules.REGEN_HEAL * regen.ticks() / BikakuRules.REGEN_EVERY;
        check(total >= 20.0F && total <= 40.0F && regen.cooldown() >= regen.ticks() * 4, "strong for a while (" + total + " health), then a long cooldown");
        check(BikakuRules.REGEN_WAIT_SHARE > 0.0 && BikakuRules.REGEN_WAIT_SHARE < 1.0 && BikakuRules.REGEN_HEAL_SHARE > 0.0 && BikakuRules.REGEN_HEAL_SHARE < 1.0,
            "Abnormal Regeneration: a ghoul's regeneration sooner and faster");
        check(BikakuRules.KNOCKBACK_RESIST > 0.0 && BikakuRules.KNOCKBACK_RESIST <= 1.0 && BikakuRules.STUN_SHARE > 0.0 && BikakuRules.STUN_SHARE < 1.0,
            "Pain Suppression: harder to throw, stuns shorter (never none)");
        check(com.hex.kagune.control.bikaku.MawTissue.shown(-1) == 0.0 && com.hex.kagune.control.bikaku.MawTissue.shown(regen.ticks()) == 0.0
            && com.hex.kagune.control.bikaku.MawTissue.shown(80) > 0.99, "its strands show while it lasts");
        boolean finite = true;
        boolean onSkin = true;
        int most = 0;
        for (double age = 0; age < regen.ticks(); age += 3) {
            java.util.List<com.hex.kagune.control.bikaku.MawTissue.Strand> strands = com.hex.kagune.control.bikaku.MawTissue.strands(40 + age, age, 7);
            most = Math.max(most, strands.size());
            for (com.hex.kagune.control.bikaku.MawTissue.Strand strand : strands) {
                check(strand.local().length >= 2 && strand.radius() > 0.0 && strand.radius() < 0.08, "a strand is a thin line of points");
                for (double[] p : strand.local()) {
                    finite &= Double.isFinite(p[0]) && Double.isFinite(p[1]) && Double.isFinite(p[2]);
                    // Off the skin: outside its part's box (in x or z) by most of the gap it keeps.
                    double[][] box = switch (strand.part()) {
                        case "torso" -> new double[][] {{-4, 0, -2}, {4, 12, 2}};
                        case "head" -> new double[][] {{-4, -8, -4}, {4, 0, 4}};
                        case "rightArm" -> new double[][] {{-3, -2, -2}, {1, 10, 2}};
                        case "leftArm" -> new double[][] {{-1, -2, -2}, {3, 10, 2}};
                        default -> new double[][] {{-2, 0, -2}, {2, 12, 2}};
                    };
                    boolean outside = p[0] < box[0][0] - 0.1 || p[0] > box[1][0] + 0.1 || p[2] < box[0][2] - 0.1 || p[2] > box[1][2] + 0.1;
                    boolean height = p[1] >= box[0][1] && p[1] <= box[1][1];
                    boolean nearJoint = !(strand.part().endsWith("Arm") || strand.part().endsWith("Leg")) || p[1] <= box[0][1] + 0.6 * (box[1][1] - box[0][1]);
                    onSkin &= outside && height && nearJoint;
                }
            }
        }
        check(finite && onSkin, "every strand lies just off its part's skin (on an arm or leg, near its joint)");
        check(most >= 12, "many strands crawl over him (" + most + ")");
        check(com.hex.kagune.control.bikaku.MawTissue.strands(40, regen.ticks() + 5, 7).isEmpty(), "none once it is over");
    }

    /**
     * 3.5.7, the kakuja's own moves (the owner's spec; mastery alone opens them, with the kakuja): Thousand Jaws, Endless Hunger, Flesh
     * Cataclysm and Undying Abomination on the worm's Z, G, B and C in it; nothing else in it; Undying Abomination alongside the rest;
     * the board shows them in the kakuja only, with its tentacles biting on their own; its numbers hang together.
     */
    private static void kakujaMoves() {
        com.hex.kagune.control.bikaku.BikakuMove[] own = {com.hex.kagune.control.bikaku.BikakuMove.JAWS, com.hex.kagune.control.bikaku.BikakuMove.HUNGER,
            com.hex.kagune.control.bikaku.BikakuMove.CATACLYSM, com.hex.kagune.control.bikaku.BikakuMove.UNDYING};
        com.hex.kagune.control.bikaku.BikakuMove.Slot[] on = {com.hex.kagune.control.bikaku.BikakuMove.Slot.Z, com.hex.kagune.control.bikaku.BikakuMove.Slot.G,
            com.hex.kagune.control.bikaku.BikakuMove.Slot.B, com.hex.kagune.control.bikaku.BikakuMove.Slot.C};
        int kakujaMoves = 0;
        for (com.hex.kagune.control.bikaku.BikakuMove move : com.hex.kagune.control.bikaku.BikakuMove.values()) {
            kakujaMoves += move.kakuja() ? 1 : 0;
        }
        check(kakujaMoves == 4, "the kakuja has four moves of its own");
        for (int i = 0; i < own.length; i++) {
            check(own[i].kakuja() && own[i].slot() == on[i] && com.hex.kagune.control.bikaku.BikakuMove.of(on[i], true) == own[i], own[i] + " on its key in the kakuja");
            check(com.hex.kagune.control.bikaku.BikakuMove.of(on[i]) != own[i], own[i] + " not out of it");
            check(own[i].mastery() == BikakuRules.KAKUJA && !own[i].unlocked(BikakuRules.KAKUJA - 1) && own[i].unlocked(BikakuRules.KAKUJA),
                own[i] + " opens by mastery alone, with the kakuja");
        }
        check(com.hex.kagune.control.bikaku.BikakuMove.of(com.hex.kagune.control.bikaku.BikakuMove.Slot.J, true) == com.hex.kagune.control.bikaku.BikakuMove.NONE,
            "nothing on J in the kakuja");
        check(!com.hex.kagune.control.bikaku.BikakuMove.UNDYING.occupies() && com.hex.kagune.control.bikaku.BikakuMove.JAWS.occupies()
            && com.hex.kagune.control.bikaku.BikakuMove.HUNGER.grabs() && com.hex.kagune.control.bikaku.BikakuMove.CATACLYSM.occupies(),
            "Undying Abomination runs alongside; the others take its tentacles; Endless Hunger holds what it catches");
        // The board.
        Map<AbilityGuide.Key, String> keys = new EnumMap<>(AbilityGuide.Key.class);
        for (AbilityGuide.Key key : AbilityGuide.Key.values()) {
            keys.put(key, key == AbilityGuide.Key.ATTACK ? "LMB" : key.name());
        }
        AbilityGuide.Panel in = AbilityGuide.panel(new AbilityGuide.Input(KagunePath.BIKAKU, AbilityGuide.State.OUT, 1000,
            com.hex.kagune.gameplay.KaguneForm.RINKAKU_SEED, com.hex.kagune.control.ukaku.WingForm.TWIN, false, keys, false, null, true, null, false));
        AbilityGuide.Panel out = AbilityGuide.panel(new AbilityGuide.Input(KagunePath.BIKAKU, AbilityGuide.State.OUT, 1000,
            com.hex.kagune.gameplay.KaguneForm.RINKAKU_SEED, com.hex.kagune.control.ukaku.WingForm.TWIN, false, keys, false, null, false, null, false));
        String[] boardKeys = {"RAPID", "GRAB", "SLAM", "GUARD"};
        for (int i = 0; i < own.length; i++) {
            String name = own[i].displayName();
            String key = boardKeys[i];
            check(in.title().equals("KAKUJA") && in.rows().stream().anyMatch(r -> r.name().equals(name) && r.keys().equals(key) && !r.locked()),
                "in the kakuja the board shows " + name + " on " + key);
            check(out.rows().stream().noneMatch(r -> r.name().equals(name)), "the worm's board doesn't show " + name);
        }
        check(in.rows().stream().anyMatch(r -> r.name().equals("Its tentacles bite on their own")), "the kakuja's board shows its tentacles biting on their own");
        check(AbilityGuide.brief("Its tentacles bite on their own", "auto").equals("Bites on its own") && AbilityGuide.brief("Undying Abomination", "GUARD")
            .equals("Undying Abomination"), "in brief, a word or two");
        // Its numbers.
        check(BikakuRules.UNDYING_TAKEN < 1.0F && BikakuRules.KAKUJA_TAKEN * BikakuRules.UNDYING_TAKEN < 0.3F, "Undying Abomination takes still less");
        check(BikakuRules.UNDYING_EVERY < BikakuRules.KAKUJA_REGEN_EVERY && BikakuRules.UNDYING_SLOW_EVERY > BikakuRules.UNDYING_EVERY
            && BikakuRules.UNDYING_SLOWED < com.hex.kagune.control.bikaku.BikakuMove.UNDYING.ticks(), "it heals far faster; a heavy blow only slows it, a while");
        check(BikakuRules.AUTO_EVERY > com.hex.kagune.control.bikaku.KakujaActs.autoTicks() && BikakuRules.AUTO_GAP > 0 && BikakuRules.AUTO_REACH <= BikakuRules.KAKUJA_REACH,
            "its bites of its own come at sensible intervals, within its reach");
        check(BikakuRules.CATA_POWER > BikakuRules.CATA_NEAR_POWER && BikakuRules.HUNGER_CRUSH_POWER > BikakuRules.HUNGER_POWER
            && BikakuRules.JAWS_POWER < BikakuRules.HUNGER_POWER, "the sweep and the crush hit hardest; Thousand Jaws' many bites each lightly");
        for (com.hex.kagune.control.bikaku.BikakuMove move : own) {
            check(move.cooldown() >= 300 && move.cost() >= 10.0F, move + " cools down long and costs RC");
        }
    }

    /**
     * 3.5.8 (the owner: "it should be a held move that the longer you hold it for, the longer it goes for ... covering up to like 30
     * blocks. Of course, attacks are not able to be performed until the kagune is pulled fully back"): a tap reaches what it always did, a
     * full hold 30 blocks, the longer the further; a longer throw takes longer; the worm's thrown moves are held, the rest aren't.
     */
    private static void held() {
        for (double base : new double[] {BikakuRules.REACH, BikakuRules.DRAG_REACH, BikakuRules.PURSUIT_REACH, BikakuRules.ERUPTION_REACH}) {
            check(Math.abs(BikakuRules.heldReach(base, 0) - base) < 1e-9, "a tap reaches what it always did");
            check(Math.abs(BikakuRules.heldReach(base, BikakuRules.CHARGE_FULL) - 30.0) < 1e-9
                && Math.abs(BikakuRules.heldReach(base, BikakuRules.CHARGE_FULL * 3) - 30.0) < 1e-9, "a full hold reaches 30 blocks, no more");
            double last = -1.0;
            for (int t = 0; t <= BikakuRules.CHARGE_FULL; t++) {
                double r = BikakuRules.heldReach(base, t);
                check(r > last, "the longer it is held, the further it goes");
                last = r;
            }
            check(Math.abs(BikakuRules.slower(base, base) - 1.0) < 1e-9 && BikakuRules.slower(30.0, base) > 1.5
                && BikakuRules.slower(20.0, base) < BikakuRules.slower(30.0, base), "a tap takes as long as ever; further takes longer");
        }
        check(BikakuRules.HELD_REACH >= 30.0 && BikakuRules.CHARGE_FULL >= 20 && BikakuRules.CHARGE_GIVE_UP > BikakuRules.CHARGE_FULL,
            "up to 30 blocks, a second or more to get there, let go by itself only long after");
        for (com.hex.kagune.control.bikaku.BikakuMove move : com.hex.kagune.control.bikaku.BikakuMove.values()) {
            boolean thrown = move == com.hex.kagune.control.bikaku.BikakuMove.MAW || move == com.hex.kagune.control.bikaku.BikakuMove.DRAG
                || move == com.hex.kagune.control.bikaku.BikakuMove.PURSUIT || move == com.hex.kagune.control.bikaku.BikakuMove.ERUPTION;
            check(move.held() == thrown, move + (thrown ? " is held to throw it further" : " isn't held"));
        }
    }
}
