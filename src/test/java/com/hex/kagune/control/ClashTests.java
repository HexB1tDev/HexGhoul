package com.hex.kagune.control;

import java.util.ArrayList;
import java.util.List;

/** 1.0.40: two players' M1s meeting ({@link ClashRules}): when they meet, and when they don't. */
public final class ClashTests {
    private static int passed;
    private static final List<String> FAILURES = new ArrayList<>();

    private static void check(boolean condition, String description) {
        if (condition) {
            passed++;
        } else {
            FAILURES.add(description);
        }
    }

    /**
     * 3.2.4 (the owner: a maxed ghoul thrown back by low-rank investigators): both sides on one scale; a fresh ghoul even with a Rank 0, a
     * maxed one the Reaper's equal, and everything below the dangerous ranks the one sent flying by a maxed ghoul.
     */
    static void power() {
        double max = com.hex.kagune.control.ClashPower.progress(1000, 1.0, 1000);
        double fresh = com.hex.kagune.control.ClashPower.progress(0, 0.0, 0);
        check(Math.abs(max - 1.0) < 1.0E-9 && fresh == 0.0, "progress runs 0 (fresh) to 1 (mastery, every gate, the ghoul meat)");
        check(com.hex.kagune.control.ClashPower.progress(1000, 0.0, 0) < 0.6 && com.hex.kagune.control.ClashPower.progress(1000, 1.0, 0) < 0.9,
            "mastery alone isn't all of it: the quests and the ghoul meat count too");
        check(com.hex.kagune.control.ClashPower.power(1.0) == com.hex.kagune.control.npc.AgentRules.Rank.RANK_10.power
            && com.hex.kagune.control.ClashPower.power(0.0) == com.hex.kagune.control.npc.AgentRules.Rank.RANK_0.power, "from a Rank 0's power to the Reaper's");
        com.hex.kagune.control.quinque.Quinque dawn = com.hex.kagune.control.quinque.Quinque.DAWN;
        double reaper = com.hex.kagune.control.ClashPower.agent(com.hex.kagune.control.npc.AgentRules.power(com.hex.kagune.control.npc.AgentRules.Rank.RANK_10, false),
            com.hex.kagune.control.npc.AgentRules.damage(dawn, 0));
        double maxed = com.hex.kagune.control.ClashPower.player(max, 1.0, 1.0);
        double ratio = ClashRules.ratio(maxed, reaper);
        check(ratio > 0.95 && ratio < 1.05, "a maxed ghoul's ordinary blow meets the Reaper's as an equal: " + ratio);
        boolean outhits = true;
        boolean flung = true;
        for (com.hex.kagune.control.npc.AgentRules.Rank rank : com.hex.kagune.control.npc.AgentRules.Rank.values()) {
            for (com.hex.kagune.control.quinque.Quinque q : com.hex.kagune.control.quinque.Quinque.values()) {
                for (int step = 0; step < 3; step++) {
                    double agent = com.hex.kagune.control.ClashPower.agent(com.hex.kagune.control.npc.AgentRules.power(rank, false),
                        com.hex.kagune.control.npc.AgentRules.damage(q, step));
                    if (rank.ordinal() <= com.hex.kagune.control.npc.AgentRules.Rank.RANK_7.ordinal()) {
                        // Pushed less than the agent, whatever it swings.
                        outhits &= ClashRules.push(1.5F, ClashRules.ratio(maxed, agent)) < ClashRules.push(1.5F, ClashRules.ratio(agent, maxed));
                    }
                    if (rank.ordinal() <= com.hex.kagune.control.npc.AgentRules.Rank.RANK_1.ordinal() && step == 0) {
                        flung &= ClashRules.overwhelmed(ClashRules.ratio(agent, maxed));
                    }
                    flung &= !ClashRules.overwhelmed(ClashRules.ratio(maxed, agent)) || rank == com.hex.kagune.control.npc.AgentRules.Rank.RANK_10;
                }
            }
        }
        check(outhits, "a maxed ghoul is thrown back less than any investigator up to Rank 7, whatever weapon, whatever blow");
        check(flung, "a Rank 0 or 1 is sent flying by a maxed ghoul's blow; no investigator but the Reaper ever sends a maxed ghoul flying");
        double freshBlow = com.hex.kagune.control.ClashPower.player(fresh, 1.0, 1.0);
        double rookie = com.hex.kagune.control.ClashPower.agent(com.hex.kagune.control.npc.AgentRules.power(com.hex.kagune.control.npc.AgentRules.Rank.RANK_0, false),
            com.hex.kagune.control.npc.AgentRules.damage(com.hex.kagune.control.quinque.Quinque.BLADE, 0));
        double freshRatio = ClashRules.ratio(freshBlow, rookie);
        check(freshRatio > 0.8 && freshRatio < 1.25, "a fresh ghoul and a Rank 0 meet about even: " + freshRatio);
        check(com.hex.kagune.control.ClashPower.player(0.5, 1.0, 1.0) > freshBlow && com.hex.kagune.control.ClashPower.player(0.5, 1.0, 1.0) < maxed,
            "halfway, in between");
        check(com.hex.kagune.control.ClashPower.build(0.5) == 1.0 && com.hex.kagune.control.ClashPower.build(1.69) > 1.29
            && com.hex.kagune.control.ClashPower.build(10.0) == com.hex.kagune.control.ClashPower.BUILD_MOST, "form and mutations add, up to a ceiling");
        check(com.hex.kagune.control.ClashPower.player(1.0, 1.0, 2.0) > maxed && com.hex.kagune.control.ClashPower.player(1.0, 1.0, 2.0) < maxed * 2.0,
            "a heavier blow counts more, by its root");
    }

    public static void main(String[] args) {
        power();
        // The other blow on its way: from the tick it set out to the tick it lands, and no later.
        check(ClashRules.onItsWay(100, 106, 100) && ClashRules.onItsWay(100, 106, 103) && ClashRules.onItsWay(100, 106, 106),
            "a blow is on its way from its start to its landing");
        check(!ClashRules.onItsWay(100, 106, 99) && !ClashRules.onItsWay(100, 106, 107), "not before it set out, nor after it landed");
        // Facing: straight at him, a little off, side on, away.
        check(ClashRules.faces(0.0, 1.0, 0.0, 3.0), "looking straight at him");
        check(ClashRules.faces(Math.sin(Math.toRadians(50.0)), Math.cos(Math.toRadians(50.0)), 0.0, 3.0), "50 degrees off still meets");
        check(!ClashRules.faces(1.0, 0.0, 0.0, 3.0), "side on does not");
        check(!ClashRules.faces(0.0, -1.0, 0.0, 3.0), "looking away does not");
        check(ClashRules.faces(0.0, 1.0, 0.0, 0.0), "on top of each other always meets");
        check(!ClashRules.faces(0.0, 0.0, 0.0, 3.0), "looking straight up or down does not");
        // Reach: within the other blow's reach and a little past it.
        check(ClashRules.reaches(4.0, 3.0) && !ClashRules.reaches(4.6, 3.0), "within its reach and the slack past it, no further");
        // All of it.
        check(ClashRules.meets(100, 106, 104, 0.0, 1.0, 0.0, 3.0, 3.0, 3.5), "a blow on its way, aimed at him, reaching him: they meet");
        check(!ClashRules.meets(100, 106, 108, 0.0, 1.0, 0.0, 3.0, 3.0, 3.5), "the other already landed: it does not");
        check(!ClashRules.meets(100, 106, 104, 1.0, 0.0, 0.0, 3.0, 3.0, 3.5), "the other aimed elsewhere: it does not");
        check(!ClashRules.meets(100, 106, 104, 0.0, 1.0, 0.0, 9.0, 9.0, 3.5), "the other out of its reach: it does not");
        check(ClashRules.PUSH > 0.0 && ClashRules.LIFT > 0.0 && ClashRules.STAGGER > 0, "both are thrown apart and kept a moment from striking again");
        shards();
        grabs();
        impacts();
        sameMoves();
        if (!FAILURES.isEmpty()) {
            FAILURES.forEach(failure -> System.err.println("FAILED: " + failure));
            System.err.println(FAILURES.size() + " of " + (passed + FAILURES.size()) + " checks failed");
            System.exit(1);
        }
        System.out.println("Clash tests: " + passed + " checks passed");
    }

    /**
     * 1.3.62: a clash as big as both blows were hard: two ordinary blows as it always looked, two jabs a small one, two heavy blows a big
     * one that shakes the ground (harder and longer the bigger, felt further), thrown apart harder, a deeper, louder sound; never past its
     * bounds.
     */
    static void impacts() {
        check(Math.abs(ClashRules.impact(ClashRules.REFERENCE / 2.0, ClashRules.REFERENCE / 2.0) - ClashRules.ORDINARY) < 1.0E-4,
            "two ordinary blows: the clash as it always was");
        check(ClashRules.impact(1.4, 1.4) < ClashRules.ORDINARY && ClashRules.impact(12.0, 15.0) > ClashRules.ORDINARY, "two jabs smaller, two heavy blows bigger");
        boolean rising = true;
        float last = 0.0F;
        for (double h = 0.0; h < 200.0; h += 0.5) {
            float p = ClashRules.impact(h, h);
            rising &= p >= last && p >= ClashRules.LEAST && p <= ClashRules.MOST;
            last = p;
        }
        check(rising && ClashRules.impact(500.0, 500.0) == ClashRules.MOST && ClashRules.impact(0.0, 0.0) == ClashRules.LEAST
            && ClashRules.impact(-5.0, 1.0) == ClashRules.LEAST, "the harder, the bigger, within its bounds");
        check(!ClashRules.quakes(ClashRules.ORDINARY) && ClashRules.quakes(ClashRules.impact(12.0, 12.0)) && ClashRules.rumble(ClashRules.ORDINARY) == 0.0F
            && ClashRules.rumble(ClashRules.MOST) == 1.0F && ClashRules.rumble(2.0F) > ClashRules.rumble(1.7F), "only a hard clash shakes the ground, harder the bigger");
        check(ClashRules.rumbleTicks(ClashRules.MOST) > ClashRules.rumbleTicks(ClashRules.QUAKES) && ClashRules.felt(ClashRules.MOST) > ClashRules.felt(ClashRules.QUAKES),
            "longer, and felt further, the bigger");
        check(Math.abs(ClashRules.push(ClashRules.ORDINARY) - ClashRules.PUSH) < 1.0E-6 && ClashRules.push(ClashRules.MOST) > ClashRules.PUSH,
            "thrown apart as before by an ordinary clash, harder by a big one");
        check(Math.abs(ClashRules.pitch(ClashRules.ORDINARY) - 1.0F) < 1.0E-6 && ClashRules.pitch(ClashRules.MOST) < 1.0F
            && ClashRules.volume(ClashRules.MOST) > ClashRules.volume(ClashRules.ORDINARY), "a big clash sounds deeper and carries further");
        // 2.0.11: louder and bigger, the weaker thrown further, a big gap overwhelming (the Speed Kick's flight).
        check(ClashRules.volume(ClashRules.ORDINARY) > 0.95F + 0.3F * ClashRules.ORDINARY && ClashRules.layers(ClashRules.ORDINARY) == 1
            && ClashRules.layers(1.6F) == 2 && ClashRules.layers(ClashRules.MOST) == 3 && ClashRules.shown(ClashRules.MOST) > ClashRules.MOST
            && ClashRules.shown(2.0F) > ClashRules.shown(1.0F), "the clash louder (laid over itself when hard) and drawn bigger, the harder the bigger");
        check(Math.abs(ClashRules.ratio(7.0, 7.0) - 1.0) < 1.0E-9 && ClashRules.ratio(4.0, 12.0) > 1.0 && ClashRules.ratio(12.0, 4.0) < 1.0,
            "the gap between two blows");
        check(Math.abs(ClashRules.push(ClashRules.ORDINARY, 1.0) - ClashRules.PUSH) < 1.0E-6
            && ClashRules.push(1.5F, ClashRules.ratio(4.0, 12.0)) > ClashRules.push(1.5F, 1.0)
            && ClashRules.push(1.5F, ClashRules.ratio(12.0, 4.0)) < ClashRules.push(1.5F, 1.0), "the weaker thrown further, the stronger less");
        check(!ClashRules.overwhelmed(ClashRules.ratio(7.0, 12.0)) && ClashRules.overwhelmed(ClashRules.ratio(4.0, 10.0))
            && !ClashRules.overwhelmed(ClashRules.ratio(10.0, 4.0)), "only a big enough gap overwhelms, and only the weaker");
        // 2.0.12: every clash shakes the view, harder, longer and further the harder it is (an ordinary one about 6 degrees).
        float ordinary = ClashRules.shown(ClashRules.ORDINARY);
        check(ClashRules.shake(ordinary) >= 1.5F && ClashRules.shake(ClashRules.shown(ClashRules.MOST)) == ClashRules.SHAKE_MOST
            && ClashRules.shake(ClashRules.shown(ClashRules.LEAST)) > 0.5F && ClashRules.shakeTicks(ClashRules.shown(ClashRules.MOST)) > ClashRules.shakeTicks(ordinary)
            && ClashRules.shakeReach(ordinary) >= 20.0, "every clash shakes the view: an ordinary one hard, a huge one hardest, longest and furthest");
    }

    /**
     * 1.3.63: the same big move meeting itself: a Big Punch meets a Big Punch (the Owl's too, or one still winding up), a Speed Kick a
     * Speed Kick, a club slam a slam; never two different moves; let-go moves are on their way until they'd land; the impact is always
     * big, bigger for harder blows, within bounds.
     */
    static void sameMoves() {
        com.hex.kagune.control.pale.MoveClashRules.Family punch = com.hex.kagune.control.pale.MoveClashRules.Family.PUNCH;
        check(com.hex.kagune.control.pale.MoveClashRules.family(com.hex.kagune.control.pale.PaleMove.PUNCH) == punch
            && com.hex.kagune.control.pale.MoveClashRules.family(com.hex.kagune.control.pale.PaleMove.OWL_PUNCH) == punch
            && com.hex.kagune.control.pale.MoveClashRules.family(com.hex.kagune.control.pale.PaleMove.PUNCH_CHARGE) == punch
            && com.hex.kagune.control.pale.MoveClashRules.family(com.hex.kagune.control.pale.PaleMove.OWL_PUNCH_CHARGE) == punch,
            "a Big Punch is a Big Punch: the Owl's, or one winding up");
        check(com.hex.kagune.control.pale.MoveClashRules.family(com.hex.kagune.control.pale.PaleMove.KICK)
            == com.hex.kagune.control.pale.MoveClashRules.family(com.hex.kagune.control.pale.PaleMove.OWL_KICK)
            && com.hex.kagune.control.pale.MoveClashRules.family(com.hex.kagune.control.pale.PaleMove.KICK) != punch
            && com.hex.kagune.control.pale.MoveClashRules.family(com.hex.kagune.control.pale.PaleMove.HAMMER)
            == com.hex.kagune.control.pale.MoveClashRules.family(com.hex.kagune.control.pale.PaleMove.OWL_FISSURE)
            && com.hex.kagune.control.pale.MoveClashRules.family(com.hex.kagune.control.pale.PaleMove.HAMMER)
            != com.hex.kagune.control.pale.MoveClashRules.family(com.hex.kagune.control.pale.PaleMove.KICK),
            "a kick meets a kick, a slam a slam; never different moves");
        check(com.hex.kagune.control.pale.MoveClashRules.family(com.hex.kagune.control.pale.PaleMove.STRIKE) == null
            && com.hex.kagune.control.pale.MoveClashRules.family(com.hex.kagune.control.pale.PaleMove.NONE) == null
            && com.hex.kagune.control.pale.MoveClashRules.family(null) == null, "an M1 or nothing isn't a big move (M1s have their own clash)");
        boolean windows = true;
        for (com.hex.kagune.control.pale.PaleMove move : com.hex.kagune.control.pale.PaleMove.values()) {
            boolean released = com.hex.kagune.control.pale.MoveClashRules.released(move);
            int window = com.hex.kagune.control.pale.MoveClashRules.window(move, 5);
            windows &= released ? window > 0 && window < 20 && com.hex.kagune.control.pale.MoveClashRules.family(move) != null : window == 0;
        }
        check(windows && !com.hex.kagune.control.pale.MoveClashRules.released(com.hex.kagune.control.pale.PaleMove.PUNCH_CHARGE),
            "a let-go big move is on its way until it lands (a moment); one winding up isn't let go yet");
        check(com.hex.kagune.control.pale.MoveClashRules.window(com.hex.kagune.control.pale.PaleMove.KICK, 7)
            > com.hex.kagune.control.pale.MoveClashRules.window(com.hex.kagune.control.pale.PaleMove.KICK, 2), "a longer dash is on its way longer");
        check(com.hex.kagune.control.pale.MoveClashRules.power(1.0, 1.0) == com.hex.kagune.control.pale.MoveClashRules.BIG
            && com.hex.kagune.control.pale.MoveClashRules.power(80.0, 80.0) == ClashRules.MOST && ClashRules.quakes(com.hex.kagune.control.pale.MoveClashRules.BIG)
            && com.hex.kagune.control.pale.MoveClashRules.STAGGER > ClashRules.STAGGER, "always a big impact that shakes the ground, both staggered longer");
        for (com.hex.kagune.control.pale.MoveClashRules.Family family : com.hex.kagune.control.pale.MoveClashRules.Family.values()) {
            check(com.hex.kagune.control.pale.MoveClashRules.reach(family) >= 3.5, family + " meets within reach");
        }
    }

    /** 1.0.42: an M1 on its way meets a shard flying at the one swinging it, in front of him and within reach; nothing else. */
    static void shards() {
        double[] chest = {0.0, 1.1, 0.0};
        // He looks along +z; a shard comes in down -z from 6 blocks off, crossing 2 blocks in front of him this tick.
        double[] from = {0.0, 1.2, 6.0};
        double[] to = {0.0, 1.2, 2.0};
        double t = ClashRules.swats(100, 106, 103, 0.0, 1.0, chest, from, to, 3.0);
        check(t >= 0.0 && t <= 1.0, "a shard coming straight at him through his blow's reach is met: " + t);
        double z = from[2] + (to[2] - from[2]) * t;
        check(Math.abs(Math.sqrt(z * z + 0.01) - (3.0 + ClashRules.SHARD_SLACK)) < 1.0E-6, "met as it comes into his reach: " + z);
        check(ClashRules.swats(100, 106, 107, 0.0, 1.0, chest, from, to, 3.0) < 0.0, "not once his blow has landed");
        check(ClashRules.swats(100, 106, 99, 0.0, 1.0, chest, from, to, 3.0) < 0.0, "not before it set out");
        check(ClashRules.swats(100, 106, 103, 0.0, -1.0, chest, from, to, 3.0) < 0.0, "not with his back to it");
        check(ClashRules.swats(100, 106, 103, 0.0, 1.0, chest, new double[] {0.0, 1.2, 14.0}, new double[] {0.0, 1.2, 10.0}, 3.0) < 0.0,
            "not while it is still out of reach");
        check(ClashRules.swats(100, 106, 103, 0.0, 1.0, chest, new double[] {0.0, 1.2, 2.0}, new double[] {0.0, 1.2, 6.0}, 3.0) < 0.0,
            "not one flying away from him");
        check(ClashRules.swats(100, 106, 103, 0.0, 1.0, chest, new double[] {6.0, 1.2, 3.0}, new double[] {-6.0, 1.2, 3.0}, 3.0) >= 0.0,
            "one crossing in front of him, in reach, is met");
        double through = ClashRules.swats(100, 106, 103, 0.0, 1.0, chest, new double[] {0.0, 1.2, 4.0}, new double[] {0.0, 1.2, -4.0}, 3.0);
        check(through >= 0.0 && through < 0.5, "a fast one flying right through him is met before it reaches him: " + through);
        check(ClashRules.SHARDS_PER_BLOW > 0 && ClashRules.SHARD_SLACK >= 0.0, "a blow meets a few shards");
        // 1.0.43: a blow that met shards is spent on them: from when it set out to just after it lands.
        check(ClashRules.spent(100, 106, 100) && ClashRules.spent(100, 106, 107), "a blow that swatted lands on nothing");
        check(!ClashRules.spent(100, 106, 99) && !ClashRules.spent(100, 106, 109), "but only that blow");
    }

    /** 1.0.42: Rinkaku's grab, slower and reeling in only at its end, broken by a swing since it set out. */
    static void grabs() {
        check(GrabRules.reach(9) == 18, "the grab takes twice the original's 9 ticks: " + GrabRules.reach(9));
        check(GrabRules.reelFrom(18) == 12, "it reels in over its last 6 ticks: " + GrabRules.reelFrom(18));
        check(GrabRules.reelFrom(9) == 3 && GrabRules.reelFrom(2) == 3, "a reach left as the original's reels in as the original's did");
        for (int original = 2; original <= 20; original++) { // (the original's cooldown() never goes under 2)
            int reach = GrabRules.reach(original);
            check(reach > original && GrabRules.reelFrom(reach) < reach, "slower, and caught before it ends: " + original);
        }
        check(GrabRules.counts(100, 104, 110), "a swing after it set out is judged");
        check(GrabRules.counts(100, 99, 110), "a swing a moment before it set out is judged too (mashing is caught out)");
        check(!GrabRules.counts(100, 90, 110), "an old one is not");
        check(!GrabRules.counts(100, 111, 110), "one yet to come is not");
        // 1.0.43: the window is the last 8 ticks before it holds.
        check(GrabRules.windowOpens(18) == 10 && GrabRules.windowOpens(4) == 0, "the window opens 8 ticks before it holds: " + GrabRules.windowOpens(18));
        check(!GrabRules.inWindow(100, 18, 100) && !GrabRules.inWindow(100, 18, 109), "a swing as it sets out, or too early, is not in it");
        check(GrabRules.inWindow(100, 18, 110) && GrabRules.inWindow(100, 18, 117), "one in its last 8 ticks is");
        check(GrabRules.windowOpens(18) <= GrabRules.reelFrom(18), "the window is open before it starts reeling in");
        check(GrabRules.WINDOW >= 6, "the window is long enough to react in (" + GrabRules.WINDOW + " ticks)");
    }
}
