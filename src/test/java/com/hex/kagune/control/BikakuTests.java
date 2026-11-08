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
        System.out.println("Bikaku tests: " + passed + " checks passed");
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
        for (com.hex.kagune.control.bikaku.BikakuMove move : com.hex.kagune.control.bikaku.BikakuMove.values()) {
            check(com.hex.kagune.control.bikaku.BikakuMove.byOrdinal(move.ordinal()) == move, "each move reads back");
            if (move == com.hex.kagune.control.bikaku.BikakuMove.NONE) {
                continue;
            }
            check(slots.add(move.slot()) && move.slot() != com.hex.kagune.control.bikaku.BikakuMove.Slot.NONE, move + " has its own key");
            check(com.hex.kagune.control.bikaku.BikakuMove.of(move.slot()) == move, move + " is what its key does");
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
}
