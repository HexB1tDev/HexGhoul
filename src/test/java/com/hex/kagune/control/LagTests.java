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
                boolean client = file.toString().contains("/client/");
                asked += source.split("NpcIndex\\.of\\(", -1).length - 1;
                if (!client && !file.getFileName().toString().equals("NpcIndex.java") && search.matcher(source).find()) {
                    left.add(file.getFileName().toString());
                }
            }
        }
        check(left.isEmpty(), "no server code searches the world box for the mod's NPCs: " + left);
        check(asked >= 55, "the mod's NPC searches go through the list (" + asked + ")");
        String index = read("src/main/java/com/hex/kagune/control/server/NpcIndex.java");
        for (String kind : new String[] {"GhoulNpc", "CcgAgent", "HumanNpc", "EtoNpc", "ChildNpc", "FleshThrall"}) {
            check(index.contains("entity instanceof com.hex.kagune.control.npc." + kind), "the list keeps every " + kind);
        }
        check(index.contains("if (event.getLevel() instanceof ServerLevel level && ours(event.getEntity())) {"), "kept as each joins a server world");
        check(index.contains("if (body.m_213877_() || body.m_9236_() != level) {\n                    it.remove();"), "the gone dropped as walked");
        check(index.contains("if (type.isInstance(body) && body.m_20191_().m_82381_(box)) {"), "the game's own answer: its kind, its box meeting the asked");
        check(index.contains("found.removeIf(body -> !filter.test(body));"), "and the asker's filter");
        check(index.contains("Collections.newSetFromMap(new WeakHashMap<>())") && index.contains("new WeakHashMap<>();"),
            "held weakly (nothing kept alive by the list)");
        check(index.contains("if (!(level instanceof ServerLevel)) {\n            return level.m_6443_(type, box, filter);"), "a client asks the game");
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

    /** The build's guard and the game's side, as written. */
    private static void wiring() throws Exception {
        String guard = read("patch/KaguneGuard.java");
        for (String npc : new String[] {"CcgAgent", "HumanNpc", "EtoNpc", "ChildNpc", "FleshThrall"}) {
            check(guard.contains("\"com/hex/kagune/control/npc/" + npc + "\", Set.of(\"m_8024_\")"), npc + "'s brain is timed");
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
