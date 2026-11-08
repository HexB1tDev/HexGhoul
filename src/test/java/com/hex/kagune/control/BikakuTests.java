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
        check(out.rows().stream().noneMatch(row -> row.keys().contains(keys.get(AbilityGuide.Key.GRAB))), "no Rinkaku grab on the board");
        AbilityGuide.Panel brief = AbilityGuide.brief(out);
        check(brief.rows().size() == out.rows().size() && brief.rows().get(0).name().equals("Bites"), "in brief: Bites");
        AbilityGuide.Panel in = AbilityGuide.panel(new AbilityGuide.Input(KagunePath.BIKAKU, AbilityGuide.State.CLOSED, 0,
            com.hex.kagune.gameplay.KaguneForm.RINKAKU_SEED, com.hex.kagune.control.ukaku.WingForm.TWIN, false, keys));
        check(in.title().equals("BIKAKU"), "kagune in: the board is Bikaku's");
    }
}
