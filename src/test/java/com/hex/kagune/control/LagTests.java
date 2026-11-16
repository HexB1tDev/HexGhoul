package com.hex.kagune.control;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 4.1.99: the mod's profiler ({@link LagMeter}, {@code /kagune lag}): a call's own time without the timed calls inside it, nothing
 * counted while no window is open or from an older window, frames left by a call that never exited dropped, each thread's calls in its
 * own window (the client's never among the server's workers), the report's lines; and the build's guard times every listener, every NPC
 * brain, renderer and layer, structure search and packet.
 */
public final class LagTests {
    private static int passed;
    private static final List<String> FAILURES = new ArrayList<>();

    private static void check(boolean condition, String description) {
        if (condition) {
            passed++;
        } else {
            FAILURES.add(description);
        }
    }

    private static void spin(double ms) {
        long until = System.nanoTime() + (long)(ms * 1e6);
        while (System.nanoTime() < until) {
            Thread.onSpinWait();
        }
    }

    private static LagMeter.Entry entry(LagMeter.Report report, String site) {
        for (LagMeter.Entry entry : report.entries()) {
            if (entry.site().equals(site)) {
                return entry;
            }
        }
        return null;
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }

    public static void main(String[] args) throws Exception {
        nesting();
        windows();
        threads();
        lines();
        wiring();
        registry();
        pace();
        worldgen();
        if (!FAILURES.isEmpty()) {
            FAILURES.forEach(failure -> System.out.println("FAILED: " + failure));
            System.exit(1);
        }
        System.out.println("Lag tests: " + passed + " checks passed");
    }

    /** A call's own time is its time less the timed calls inside it; its worst is its whole time. */
    private static void nesting() {
        LagMeter.enter("before.open");
        LagMeter.exit("before.open");
        check(!LagMeter.measuring(), "nothing is measured before a window opens");
        // A first window warms the meter (its classes loaded, its maps made: a cold JVM's milliseconds land in the parent call).
        LagMeter.open(LagMeter.Side.SERVER, Thread.currentThread());
        LagMeter.enter("Warm.up");
        LagMeter.enter("Warm.inner");
        LagMeter.exit("Warm.inner");
        LagMeter.exit("Warm.up");
        LagMeter.lines(LagMeter.close(LagMeter.Side.SERVER));
        LagMeter.open(LagMeter.Side.SERVER, Thread.currentThread());
        check(LagMeter.measuring() && LagMeter.open(LagMeter.Side.SERVER) && !LagMeter.open(LagMeter.Side.CLIENT), "the server's window open");
        LagMeter.enter("Outer.tick");
        spin(2.0);
        LagMeter.enter("Inner.tick");
        spin(12.0); // well over the outer one's own 2.5 ms, so a pause on a busy machine can't reorder them
        LagMeter.exit("Inner.tick");
        spin(0.5);
        LagMeter.exit("Outer.tick");
        for (int i = 0; i < 4; i++) {
            LagMeter.enter("Packet.encode");
            LagMeter.exit("Packet.encode");
        }
        LagMeter.tick(LagMeter.Side.SERVER, 10_000_000L);
        LagMeter.tick(LagMeter.Side.SERVER, 30_000_000L);
        LagMeter.Report report = LagMeter.close(LagMeter.Side.SERVER);
        check(!LagMeter.measuring() && !LagMeter.open(LagMeter.Side.SERVER), "closed after its report");
        check(report != null && report.side() == LagMeter.Side.SERVER, "the server's report");
        if (report == null) {
            return;
        }
        double s = report.seconds();
        LagMeter.Entry outer = entry(report, "Outer.tick");
        LagMeter.Entry inner = entry(report, "Inner.tick");
        check(outer != null && inner != null, "both places counted");
        check(entry(report, "before.open") == null, "a call before the window opened is not counted");
        if (outer == null || inner == null) {
            return;
        }
        double outerSelf = outer.msPerSecond() * s;
        double innerSelf = inner.msPerSecond() * s;
        check(innerSelf >= 11.9, "the inner call's own time is its whole time: " + innerSelf + " ms");
        check(outerSelf >= 2.4 && outerSelf < outer.worstMs() - 11.9, "the outer call's own time leaves out the inner one's: " + outerSelf
            + " ms of " + outer.worstMs());
        check(Math.abs(outerSelf + innerSelf - outer.worstMs()) < 0.01, "own times add up to the outer call's whole time (nothing counted twice)");
        check(Math.abs(inner.worstMs() - innerSelf) < 0.01, "a call with nothing inside: its worst is its own time");
        check(Math.abs(outer.callsPerSecond() * s - 1.0) < 1e-6 && Math.abs(inner.callsPerSecond() * s - 1.0) < 1e-6, "one call each");
        check(report.entries().get(0).site().equals("Inner.tick"), "the costliest own time first: " + report.entries().get(0));
        check(Math.abs(report.packetsPerSecond() * s - 4.0) < 1e-6, "four packets counted by their encoders");
        check(report.ticks() == 2 && Math.abs(report.tickMs() - 40.0) < 1e-9 && Math.abs(report.worstTickMs() - 30.0) < 1e-9,
            "the ticks: two, 40 ms in all, the worst 30");
        check(Math.abs(report.ourMsPerSecond() * s - (outerSelf + innerSelf + entry(report, "Packet.encode").msPerSecond() * s)) < 1e-6,
            "the mod's share is the sum of the own times");
        check(LagMeter.close(LagMeter.Side.SERVER) == null, "a closed window has no second report");
        // Three deep: the middle call's whole time (with what it called) comes off the outer one's own.
        LagMeter.open(LagMeter.Side.SERVER, Thread.currentThread());
        LagMeter.enter("A.tick");
        spin(1.0);
        LagMeter.enter("B.tick");
        spin(1.0);
        LagMeter.enter("C.tick");
        spin(2.0);
        LagMeter.exit("C.tick");
        LagMeter.exit("B.tick");
        LagMeter.exit("A.tick");
        report = LagMeter.close(LagMeter.Side.SERVER);
        s = report.seconds();
        LagMeter.Entry a = entry(report, "A.tick");
        LagMeter.Entry b = entry(report, "B.tick");
        LagMeter.Entry c = entry(report, "C.tick");
        check(a != null && b != null && c != null && Math.abs((a.msPerSecond() + b.msPerSecond() + c.msPerSecond()) * s - a.worstMs()) < 0.01,
            "three deep, the own times add up to the outermost call's whole time");
        // Exact by construction (no timing bound: a busy machine may pause the thread anywhere): each one's own is its whole less the
        // whole of the one it called.
        check(a != null && b != null && c != null && Math.abs(a.msPerSecond() * s - (a.worstMs() - b.worstMs())) < 0.01
            && Math.abs(b.msPerSecond() * s - (b.worstMs() - c.worstMs())) < 0.01 && b.worstMs() >= 2.9 && c.worstMs() >= 1.9,
            "each one's own: its whole less the whole of what it called: " + a + " " + b + " " + c);
    }

    /** Frames from an older window, or left by a call that never exited, are dropped. */
    private static void windows() {
        LagMeter.open(LagMeter.Side.SERVER, Thread.currentThread());
        LagMeter.enter("Old.call");
        LagMeter.close(LagMeter.Side.SERVER);
        LagMeter.open(LagMeter.Side.SERVER, Thread.currentThread());
        LagMeter.exit("Old.call");
        LagMeter.enter("Lost.call"); // never exits (a throwable let through)
        LagMeter.enter("Kept.call");
        spin(1.0);
        LagMeter.exit("Kept.call");
        LagMeter.exit("Never.entered");
        LagMeter.enter("After.call");
        LagMeter.exit("After.call");
        LagMeter.Report report = LagMeter.close(LagMeter.Side.SERVER);
        check(entry(report, "Old.call") == null, "a call begun in an older window is not counted in the next");
        check(entry(report, "Never.entered") == null, "an exit with no enter counts nothing");
        check(entry(report, "Kept.call") != null && entry(report, "After.call") != null, "calls after a lost frame are still counted");
        check(entry(report, "Lost.call") == null, "a call that never exited is not counted");
        LagMeter.open(LagMeter.Side.SERVER, Thread.currentThread());
        LagMeter.enter("Outer.lost");
        LagMeter.enter("Inner.lost"); // its exit never comes
        LagMeter.exit("Outer.lost");
        report = LagMeter.close(LagMeter.Side.SERVER);
        check(entry(report, "Outer.lost") != null && entry(report, "Inner.lost") == null, "an outer call is counted though one inside it never exited");
        // Deeper than DEEP: not timed, and what is outside them still is.
        LagMeter.open(LagMeter.Side.SERVER, Thread.currentThread());
        for (int i = 0; i < LagMeter.DEEP + 5; i++) {
            LagMeter.enter("Deep." + (i < LagMeter.DEEP ? "in" : "over"));
        }
        for (int i = LagMeter.DEEP + 4; i >= 0; i--) {
            LagMeter.exit("Deep." + (i < LagMeter.DEEP ? "in" : "over"));
        }
        report = LagMeter.close(LagMeter.Side.SERVER);
        LagMeter.Entry deep = entry(report, "Deep.in");
        check(deep != null && Math.abs(deep.callsPerSecond() * report.seconds() - LagMeter.DEEP) < 1e-6, "the first DEEP levels counted");
        check(entry(report, "Deep.over") == null, "deeper ones are not");
    }

    /** Each thread's calls in its own window; the client's never among the server's workers. */
    private static void threads() throws Exception {
        Thread clientThread = new Thread(() -> {
            LagMeter.knowClient(Thread.currentThread());
        });
        clientThread.start();
        clientThread.join();
        LagMeter.open(LagMeter.Side.SERVER, Thread.currentThread());
        Runnable call = () -> {
            LagMeter.enter("Worker.call");
            LagMeter.exit("Worker.call");
        };
        Thread worker = new Thread(call);
        worker.start();
        worker.join();
        LagMeter.enter("Server.call");
        LagMeter.exit("Server.call");
        LagMeter.Report report = LagMeter.close(LagMeter.Side.SERVER);
        check(entry(report, "Server.call") != null && entry(report, "Worker.call") == null, "the server thread's calls in the server's report");
        check(report.workers() != null && entry(report.workers(), "Worker.call") != null, "a worker's calls among the server's workers");
        // A client window: the client thread's calls only.
        LagMeter.Report[] client = new LagMeter.Report[1];
        Thread game = new Thread(() -> {
            LagMeter.open(LagMeter.Side.CLIENT, Thread.currentThread());
            LagMeter.enter("Render.frame");
            LagMeter.exit("Render.frame");
            LagMeter.tick(LagMeter.Side.CLIENT, 7_000_000L);
            client[0] = LagMeter.close(LagMeter.Side.CLIENT);
        });
        game.start();
        game.join();
        check(client[0] != null && client[0].side() == LagMeter.Side.CLIENT && entry(client[0], "Render.frame") != null, "the client's report");
        // The client thread known, only the server measuring: its calls go nowhere (not among the workers).
        Thread[] known = new Thread[1];
        Thread render = new Thread(() -> {
            LagMeter.knowClient(Thread.currentThread());
            known[0] = Thread.currentThread();
        });
        render.start();
        render.join();
        LagMeter.open(LagMeter.Side.SERVER, Thread.currentThread());
        Thread again = new Thread(() -> {
            LagMeter.knowClient(Thread.currentThread());
            LagMeter.enter("Client.while.server");
            LagMeter.exit("Client.while.server");
        });
        again.start();
        again.join();
        report = LagMeter.close(LagMeter.Side.SERVER);
        check(entry(report.workers(), "Client.while.server") == null && entry(report, "Client.while.server") == null,
            "the client's calls never land in the server's report while only the server measures");
        check(known[0] != null, "the client thread told");
    }

    /** The report as read in the chat. */
    private static void lines() {
        List<LagMeter.Entry> entries = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            entries.add(new LagMeter.Entry("Place" + i + ".tick", 14 - i, 20.0, 0.5));
        }
        entries.add(new LagMeter.Entry("S2CGhoulNpc.encode", 0.2, 200.0, 0.01));
        entries.add(new LagMeter.Entry("S2CEto.encode", 0.1, 80.0, 0.01));
        LagMeter.Report workers = new LagMeter.Report(LagMeter.Side.WORKERS, 30.0, 0, 0.0, 0.0,
            List.of(new LagMeter.Entry("ControlStructures$Site.findGenerationPoint", 120.0, 3.0, 400.0)), null);
        LagMeter.Report report = new LagMeter.Report(LagMeter.Side.SERVER, 30.0, 600, 600 * 10.0, 61.0, entries, workers);
        List<String> lines = LagMeter.lines(report);
        String all = String.join("\n", lines);
        check(lines.get(0).contains("the server, 30 s") && lines.get(0).contains("a tick takes 10.0 ms on average")
            && lines.get(0).contains("the worst 61.0 ms") && lines.get(0).contains("a tick has 50 ms"), "the head: " + LagMeter.plain(lines.get(0)));
        double ours = report.ourMsPerSecond() / 20.0;
        check(lines.get(0).contains("the mod's own share " + LagMeter.ms(ours) + " ms a tick"), "the mod's share a tick: " + lines.get(0));
        check(all.contains(" 1. §fPlace0.tick§7: 14.0 ms/s, 20 calls/s, worst 0.50 ms"), "the costliest first, with calls and worst");
        check(all.contains(" 10. §fPlace9.tick") && !all.contains(" 11. "), "ten places named, no more");
        check(all.contains("Packets sent: §f280/s§7: S2CGhoulNpc 200, S2CEto 80"), "the packets sent, the most first");
        check(all.contains("Other threads (world generation): §f120 ms/s§7: ControlStructures$Site.findGenerationPoint 120 ms/s (worst 400 ms)"),
            "the workers' line");
        check(!LagMeter.plain(all).contains("§"), "the log's lines have no colours");
        LagMeter.Report paused = new LagMeter.Report(LagMeter.Side.CLIENT, 30.0, 0, 0.0, 0.0, List.of(), null);
        List<String> none = LagMeter.lines(paused);
        check(none.get(0).contains("your game") && none.get(0).contains("no frame measured") && none.get(1).contains("Nothing of the mod's ran"),
            "a window with no frames and nothing run says so");
        LagMeter.Report frames = new LagMeter.Report(LagMeter.Side.CLIENT, 10.0, 600, 600 * 5.0, 20.0, entries.subList(0, 2), null);
        check(LagMeter.lines(frames).get(0).contains("a frame takes 5.00 ms on average") && LagMeter.lines(frames).get(0).contains("(60 fps)"),
            "the client's frames and fps");
        check(LagMeter.ms(3.14159).equals("3.14") && LagMeter.ms(31.4159).equals("31.4") && LagMeter.ms(314.159).equals("314"), "ms as read");
    }

    /**
     * 4.1.100: no server code searches the world for the mod's own NPCs: each asks its world's list (NpcIndex), which keeps every one of
     * them as it joins (server worlds only), answers what the game's search would (box met, filter passed) and drops the gone.
     */
    private static void registry() throws Exception {
        java.util.regex.Pattern search = java.util.regex.Pattern.compile(
            "m_6443_\\((?:com\\.hex\\.kagune\\.control\\.npc\\.)?(GhoulNpc|EtoNpc|ChildNpc|HumanNpc|CcgAgent|FleshThrall)\\.class");
        List<String> left = new ArrayList<>();
        int asked = 0;
        try (var files = Files.walk(Path.of("src/main/java/com/hex/kagune/control"))) {
            for (Path file : (Iterable<Path>)files.filter(f -> f.toString().endsWith(".java"))::iterator) {
                String source = Files.readString(file);
                asked += source.split("NpcIndex\\.of\\(", -1).length - 1;
                // 4.1.102: the client's too (its renderers' "the ghouls near you", every frame).
                if (!file.getFileName().toString().equals("NpcIndex.java") && search.matcher(source).find()) {
                    left.add(file.getFileName().toString());
                }
            }
        }
        check(left.isEmpty(), "no code searches the world box for the mod's NPCs (4.1.102: the client's neither): " + left);
        check(asked >= 59, "the mod's NPC searches go through the list (" + asked + ")");
        String index = read("src/main/java/com/hex/kagune/control/server/NpcIndex.java");
        for (String kind : new String[] {"GhoulNpc", "CcgAgent", "HumanNpc", "EtoNpc", "ChildNpc", "FleshThrall"}) {
            check(index.contains("entity instanceof com.hex.kagune.control.npc." + kind), "the list keeps every " + kind);
        }
        check(index.contains("if (event.getLevel() != null && ours(event.getEntity())) {\n            add(event.getLevel(), (LivingEntity)event.getEntity());"),
            "kept as each joins its world (4.1.102: a client's too)");
        check(index.contains("if (body.m_213877_() || body.m_9236_() != level) {\n                    it.remove();"), "the gone dropped as walked");
        check(index.contains("if (type.isInstance(body) && body.m_20191_().m_82381_(box) && level.m_6815_(body.m_19879_()) == body) {"),
            "the game's own answer: its kind, its box meeting the asked, really in its world (4.1.103)");
        check(index.contains("@SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)\n    public static void join("),
            "kept last of the listeners: a cancelled join never kept (4.1.103)");
        check(index.contains("found.removeIf(body -> !filter.test(body));"), "and the asker's filter");
        check(index.contains("Collections.newSetFromMap(new WeakHashMap<>())") && index.contains("new WeakHashMap<>();"),
            "held weakly (nothing kept alive by the list)");
        check(!index.contains("m_6443_") && !index.contains("ServerLevel"), "4.1.102: every world walks its own list, a client's too (no world search left)");
        for (String renderer : new String[] {"client/render/GhoulNpcRenderer.java", "client/render/ChildRenderer.java", "client/render/GraftRenderer.java",
            "client/fx/PeaceFx.java"}) {
            check(read("src/main/java/com/hex/kagune/control/" + renderer).contains("com.hex.kagune.control.server.NpcIndex.of("), renderer + " walks the list");
        }
        String outfit = read("src/main/java/com/hex/kagune/control/client/render/OutfitLayer.java");
        check(outfit.contains("return TEXTURES.computeIfAbsent(model, id -> Objects.requireNonNull(KaguneMod.id(\"textures/outfit/\" + id + \".png\")));"),
            "a sleeve's texture made once a model, not every frame");
    }

    /** 4.1.100: an NPC looks round for a foe every half second near a player, every two seconds when none is within 64 blocks. */
    private static void pace() throws Exception {
        String pace = read("src/main/java/com/hex/kagune/control/server/NpcPace.java");
        check(pace.contains("FAR = 64.0;") && pace.contains("NEAR_LOOK = 10L;") && pace.contains("FAR_LOOK = 40L;"), "64 blocks, 10 and 40 ticks");
        check(pace.contains("return ago >= FAR_LOOK || ago >= NEAR_LOOK && !far(body, level);"), "near: every 10 ticks; far: every 40");
        check(pace.contains("if (player.m_20280_(body) <= FAR * FAR) {"), "any player within 64 blocks (spectators see too)");
        check(com.hex.kagune.control.server.NpcPace.due(10L, false) && !com.hex.kagune.control.server.NpcPace.due(9L, false)
            && !com.hex.kagune.control.server.NpcPace.due(39L, true) && com.hex.kagune.control.server.NpcPace.due(40L, true),
            "due: 10 ticks near, 40 far");
        String ghoul = read("src/main/java/com/hex/kagune/control/server/GhoulNpcBrain.java");
        String agent = read("src/main/java/com/hex/kagune/control/server/AgentBrain.java");
        check(ghoul.contains("mind.preySought == Long.MIN_VALUE || NpcPace.looks(npc, level, now - mind.preySought)"), "a ghoul's look paced");
        check(agent.contains("current == null && NpcPace.looks(agent, level, ago(now, mind.preySought))"), "an investigator's look paced");
    }

    /**
     * 4.1.101: the world's hotel plan worked out once (HotelPlan.Memo: the same answers as worked out afresh, copies handed out, kept
     * bounded) and a search's ground kept as told (DryLand.Memo: the same answers, each column and biome cell worked out once), timed.
     */
    private static void worldgen() throws Exception {
        int[] asked = new int[1];
        long[] seeds = {1234L, -99L, 8675309L, 42L};
        int nearSame = 0;
        int nearAll = 0;
        boolean spotsSame = true;
        long plainCalls = 0L;
        long memoCalls = 0L;
        for (long seed : seeds) {
            com.hex.kagune.control.village.HotelPlan.Offers villages = (cx, cz) -> {
                asked[0]++;
                return com.hex.kagune.control.village.DryLand.spreadChunk(seed, Math.floorDiv(cx, 34), Math.floorDiv(cz, 34), 34, 8, 10387312);
            };
            com.hex.kagune.control.village.HotelPlan.Memo plan = new com.hex.kagune.control.village.HotelPlan.Memo(seed, villages);
            for (int x = -60; x <= 60; x += 3) {
                for (int z = -60; z <= 60; z += 3) {
                    asked[0] = 0;
                    boolean plain = com.hex.kagune.control.village.HotelPlan.near(seed, villages, x, z);
                    plainCalls += asked[0];
                    asked[0] = 0;
                    boolean kept = plan.near(x, z);
                    boolean again = plan.near(x, z);
                    memoCalls += asked[0];
                    nearAll++;
                    if (plain == kept && kept == again) {
                        nearSame++;
                    }
                    int[] offer = villages.at(x, z);
                    int[][] spots = com.hex.kagune.control.village.HotelPlan.spots(seed, villages, offer[0], offer[1]);
                    int[][] memoSpots = plan.spots(offer[0], offer[1]);
                    spotsSame &= java.util.Arrays.deepEquals(spots, memoSpots);
                    if (memoSpots.length > 0) {
                        memoSpots[0][0] += 1000; // a caller changing what it was handed changes nothing kept
                        spotsSame &= java.util.Arrays.deepEquals(spots, plan.spots(offer[0], offer[1]));
                    }
                    int[] at = plan.at(x, z);
                    at[0] += 1000;
                    spotsSame &= java.util.Arrays.equals(plan.at(x, z), villages.at(x, z));
                }
            }
            check(plan.size() <= 3 * com.hex.kagune.control.village.HotelPlan.Memo.CAP, "a plan keeps a bounded number of answers");
        }
        com.hex.kagune.control.village.HotelPlan.Memo small = new com.hex.kagune.control.village.HotelPlan.Memo(7L,
            (cx, cz) -> com.hex.kagune.control.village.DryLand.spreadChunk(7L, Math.floorDiv(cx, 34), Math.floorDiv(cz, 34), 34, 8, 10387312), 64);
        boolean bounded = true;
        for (int x = -40; x <= 40; x += 2) {
            for (int z = -40; z <= 40; z += 2) {
                bounded &= small.near(x, z) == com.hex.kagune.control.village.HotelPlan.near(7L, small, x, z) && small.size() <= 3 * 64;
            }
        }
        check(bounded, "a plan full begins again (never more than its cap of each kind kept), its answers still right");
        check(nearSame == nearAll, "the kept plan says near or not as worked out afresh, every time (" + nearSame + " of " + nearAll + ")");
        check(spotsSame, "the kept plan's spots and offers are the ones worked out afresh, and never changed by an asker");
        System.out.println("  4.1.101 the hotel plan, " + nearAll + " chunks asked twice: " + plainCalls + " village offers worked out afresh, "
            + memoCalls + " kept");
        check(memoCalls * 20 < plainCalls, "the kept plan asks the villages' offers a twentieth as often or less (" + memoCalls + " against "
            + plainCalls + ")");
        // A search's ground: every column and biome cell worked out once, the answers the ground's own.
        int[] cost = new int[1];
        com.hex.kagune.control.village.DryLand.Ground world = new com.hex.kagune.control.village.DryLand.Ground() {
            @Override
            public int surface(int x, int z) {
                cost[0]++;
                spin(0.02); // about what the terrain noise costs a column (its base height: the noise down the column)
                return 64 + Math.floorMod(x * 7 + z * 3, 5);
            }

            @Override
            public int floor(int x, int z) {
                cost[0]++;
                spin(0.02);
                return 64 + Math.floorMod(x * 7 + z * 3, 5) - (Math.floorMod(x + z, 97) == 0 ? 1 : 0);
            }

            @Override
            public boolean wet(int x, int y, int z) {
                cost[0]++;
                spin(0.002); // a biome's climate sampled
                return Math.floorMod((x >> 2) * 31 + (z >> 2) * 17, 211) == 0;
            }

            @Override
            public boolean sea(int x, int y, int z) {
                cost[0]++;
                return Math.floorMod((x >> 2) + (z >> 2), 401) == 0;
            }

            @Override
            public boolean steep(int x, int y, int z) {
                cost[0]++;
                return (x >> 2) % 300 == 7;
            }
        };
        com.hex.kagune.control.village.DryLand.Memo memo = new com.hex.kagune.control.village.DryLand.Memo(world, true);
        boolean same = true;
        for (int x = -20; x < 20; x++) {
            for (int z = -20; z < 20; z++) {
                int y = 60 + (x & 7);
                same &= memo.surface(x, z) == world.surface(x, z) && memo.floor(x, z) == world.floor(x, z)
                    && memo.wet(x, y, z) == world.wet(x, y, z) && memo.sea(x, y, z) == world.sea(x, y, z) && memo.steep(x, y, z) == world.steep(x, y, z);
            }
        }
        check(same, "the kept ground answers as the ground itself");
        memo = new com.hex.kagune.control.village.DryLand.Memo(world, true);
        for (int i = 0; i < 3; i++) {
            memo.surface(5, 9);
            memo.floor(5, 9);
        }
        for (int dx = 0; dx < 4; dx++) {
            memo.wet(8 + dx, 64 + dx, 12 + dx); // one 4 x 4 x 4 cell
            memo.sea(8 + dx, 64, 12);
        }
        check(memo.worked == 4 && memo.asked == 14, "a column's height and a cell's biome worked out once each (" + memo.worked + " of " + memo.asked + ")");
        com.hex.kagune.control.village.DryLand.Memo exact = new com.hex.kagune.control.village.DryLand.Memo(world, false);
        exact.wet(8, 64, 12);
        exact.wet(9, 64, 12);
        exact.wet(8, 64, 12);
        check(exact.worked == 2, "by block where not told of cells");
        // Timed: a place's search, 25 spots a chunk apart, its rough ground and water looked at under each (ControlStructures.DryPlace).
        int size = 64;
        search(world, size); // both warmed first
        search(new com.hex.kagune.control.village.DryLand.Memo(world, true), size);
        long t0 = System.nanoTime();
        cost[0] = 0;
        search(world, size);
        long plainNanos = System.nanoTime() - t0;
        int plainCost = cost[0];
        memo = new com.hex.kagune.control.village.DryLand.Memo(world, true);
        cost[0] = 0;
        t0 = System.nanoTime();
        search(memo, size);
        long memoNanos = System.nanoTime() - t0;
        int memoCost = cost[0];
        System.out.println("  4.1.101 a place's search over 25 spots: " + plainCost + " ground lookups, " + String.format(java.util.Locale.ROOT,
            "%.1f", plainNanos / 1e6) + " ms; kept: " + memoCost + " lookups, " + String.format(java.util.Locale.ROOT, "%.1f", memoNanos / 1e6) + " ms");
        check(memoCost * 2 < plainCost, "a place's search works out under half the ground it did (" + memoCost + " of " + plainCost + ")");
        String structures = read("src/main/java/com/hex/kagune/control/ControlStructures.java");
        check(structures.contains("return new DryLand.Memo(new DryLand.Ground() {") && structures.contains("        }, true);\n    }")
            && structures.contains("return biomes.m_203407_(x >> 2, y >> 2, z >> 2, sampler).m_203656_(WET);"),
            "the world's ground kept as told for each search, its biomes by the game's own cells");
        check(structures.contains("return villageOffers(spread, seed).near(x, z);") && structures.contains("int[][] spots = offers.spots(at[0], at[1]);")
            && structures.contains("if (plan == null || plan.seed != seed) {"), "each world's hotel plan kept (by its villages' placement and seed)");
    }

    /** A place's search as ControlStructures.DryPlace makes it: at each of 25 spots, its roughness, then its water. */
    private static void search(com.hex.kagune.control.village.DryLand.Ground ground, int size) {
        int[][] shifts = com.hex.kagune.control.village.DryLand.shifts(2);
        for (int[] shift : shifts) {
            int minX = shift[0] * 16 - size / 2 + 3;
            int minZ = shift[1] * 16 - size / 2 + 5;
            com.hex.kagune.control.village.DryLand.roughness(ground, minX, minZ, size, size, 6, 10);
            com.hex.kagune.control.village.DryLand.dry(ground, 64, minX, minZ, size, size);
        }
    }

    /** The build's guard and the game's side, as written. */
    private static void wiring() throws Exception {
        String guard = read("patch/KaguneGuard.java");
        // 4.1.103: the brains are guarded now (so timed with their guard, NoFailTests); before, timed only.
        for (String npc : new String[] {"CcgAgent", "HumanNpc", "EtoNpc", "ChildNpc", "FleshThrall"}) {
            check(guard.contains("\"com/hex/kagune/control/npc/" + npc + "\", Set.of(\"m_8024_\""), npc + "'s brain is guarded, so timed");
        }
        check(guard.contains("GHOUL, Set.of(\"m_8024_\")"), "GhoulNpc's brain guarded (so timed with its guard)");
        for (String part : new String[] {"DryJigsaw", "Site", "DryPlace"}) {
            check(guard.contains("STRUCTURES + \"" + part + "\", Set.of(\"m_214086_\", \"m_262864_\", \"m_214110_\")"), part + "'s searches timed");
        }
        check(guard.contains("TIMED_NAMES = Set.of(\"m_7392_\", \"m_6494_\")"), "renderers and layers timed");
        check(guard.contains("method.name.equals(\"encode\")") && guard.contains("method.name.startsWith(\"lambda$handle$\")"),
            "packets' encoders and handlers' work timed");
        int guardAt = guard.indexOf("static void guard(");
        int timeAt = guard.indexOf("static void time(");
        check(guardAt > 0 && guard.indexOf("exits(method, meterSite);", guardAt) > guardAt
            && guard.indexOf("meter(\"enter\", meterSite)", guardAt) > guardAt && guard.indexOf("meter(\"exit\", meterSite)", guardAt) > guardAt,
            "every guarded method timed: enter, every return, its catch");
        check(timeAt > 0 && guard.indexOf("exits(method, site);", timeAt) > timeAt && guard.indexOf("Opcodes.ATHROW", timeAt) > timeAt,
            "a timed-only method's catch tells the meter and throws on");
        check(guard.contains("is not timed at its start and every way out") && guard.contains("is an untimed listener or timed place"),
            "the guard proves its timing on every method it changed and every listener in the jar");
        check(guard.contains("TIMED_FLOOR = 100"), "a floor on the timed places");
        String meter = read("src/main/java/com/hex/kagune/control/LagMeter.java");
        int enter = meter.indexOf("public static void enter(String site) {");
        check(enter > 0 && meter.indexOf("if (on) {", enter) - enter < 60 && meter.indexOf("if (on) {", enter) > enter,
            "closed, a timed call reads one field");
        int exitAt = meter.indexOf("public static void exit(String site) {");
        check(enter > 0 && exitAt > enter && meter.substring(enter, exitAt).contains("} catch (RuntimeException ignored) {")
            && meter.substring(exitAt, meter.indexOf("public static boolean measuring()")).contains("} catch (RuntimeException ignored) {"),
            "the meter itself never throws: enter and exit each catch whatever goes wrong in them");
        LagMeter.open(LagMeter.Side.SERVER, Thread.currentThread());
        boolean threw = false;
        try {
            LagMeter.enter(null);
            LagMeter.exit(null); // null.equals: a NullPointerException inside, caught
        } catch (RuntimeException failure) {
            threw = true;
        }
        LagMeter.close(LagMeter.Side.SERVER);
        check(!threw, "a broken call to the meter is swallowed, never thrown into the guard's catch");
        String commands = read("src/main/java/com/hex/kagune/control/server/ControlCommands.java");
        int lag = commands.indexOf("Commands.m_82127_(\"lag\")");
        check(lag > 0 && !commands.substring(lag, commands.indexOf(";", lag)).contains("requires"), "anyone may run /kagune lag");
        check(commands.contains("Commands.m_82127_(\"log\").executes(context -> lag(context, true))"), "/kagune lag log");
        String server = read("src/main/java/com/hex/kagune/control/server/LagServer.java");
        check(server.contains("SECONDS = 30;") && server.contains("LOG_SECONDS = 60;"), "30 s a measurement, a minute a log window");
        check(server.contains("@SubscribeEvent(priority = EventPriority.HIGHEST)") && server.contains("@SubscribeEvent(priority = EventPriority.LOWEST)"),
            "the tick timed from as early to as late as the mod sees it");
        check(server.contains("ControlNetwork.sendTo(asker, new S2CLag(SECONDS, false))"), "the asker's game measures its side too");
        check(server.contains("LagMeter.close(LagMeter.Side.SERVER);\n        ASKERS.clear();"), "a stopped world takes its measurement with it");
        String client = read("src/main/java/com/hex/kagune/control/client/LagClient.java");
        check(client.contains("LagMeter.knowClient(Thread.currentThread());"), "the client tells the meter its thread");
        check(client.contains("KaguneMod.LOGGER.info(\"HexKagune lag: \"") && server.contains("KaguneMod.LOGGER.info(\"HexKagune lag: \""),
            "both sides write their report to their log");
    }
}
