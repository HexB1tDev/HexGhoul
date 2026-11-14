package com.hex.kagune.control;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 4.1.99 (the owner: "make handoff mentioning a massive long, lag patch, making sure absolutely nothing in the mod fails and nothing ever
 * lags."): the mod's own profiler, read by {@code /kagune lag}. The build ({@code patch/KaguneGuard}) puts {@link #enter} at the start of
 * every way the game calls into the mod (every event listener, the original's and ours; our NPCs' brains; our renderers and their
 * layers; the structures' searches and placing; our packets' encoders and the work they hand the game's thread) and {@link #exit} before
 * each of its returns and in its catch.
 *
 * <p>Nothing is counted unless a window is open: then {@link #enter} and {@link #exit} read the clock, and each call's time goes to its
 * place, less the time of the timed calls inside it (its own time, so nothing is counted twice), on the thread it ran on: the server's,
 * the client's (the render thread) or, while the server measures, any other (the world generation workers). Closed, all either costs is
 * one read of {@link #on}.
 */
public final class LagMeter {
    /** Whose time a window holds. */
    public enum Side {
        SERVER, CLIENT, WORKERS
    }

    /** Places nested deeper than this (never seen: a listener in a listener in a listener...) are not timed. */
    static final int DEEP = 64;
    /** How many places a report names. */
    public static final int TOP = 10;

    /** Any window open: all a timed call reads when nothing is measured. */
    private static volatile boolean on;
    /** Bumped as a window opens: frames left from an older one are dropped. */
    private static volatile int generation;
    private static volatile Window server;
    private static volatile Window client;
    private static volatile Window workers;
    /** The client's render thread, once known (its calls never land among the workers' while only the server measures). */
    private static volatile Thread clientThread;
    private static final ThreadLocal<Frames> FRAMES = ThreadLocal.withInitial(Frames::new);

    private LagMeter() {
    }

    /** Bytecode hook: a timed place begins. */
    public static void enter(String site) {
        if (on) {
            try {
                push(site, System.nanoTime());
            } catch (RuntimeException ignored) {
                // The meter may never be what fails (it runs inside every guard's catch).
            }
        }
    }

    /** Bytecode hook: a timed place ends (a return, or a throwable on its way out). */
    public static void exit(String site) {
        if (on) {
            try {
                pop(site, System.nanoTime());
            } catch (RuntimeException ignored) {
                // As above.
            }
        }
    }

    /** True while any window is open. */
    public static boolean measuring() {
        return on;
    }

    /** The client's render thread (told by the client every tick: one write). */
    public static void knowClient(Thread thread) {
        if (clientThread != thread) {
            clientThread = thread;
        }
    }

    /** Opens {@code side}'s window on {@code thread} (the server's: its workers' too), forgetting what an earlier one held. */
    public static synchronized void open(Side side, Thread thread) {
        long now = System.nanoTime();
        generation++;
        if (side == Side.CLIENT) {
            clientThread = thread;
            client = new Window(side, thread, now);
        } else {
            server = new Window(Side.SERVER, thread, now);
            workers = new Window(Side.WORKERS, null, now);
        }
        on = true;
    }

    /** Closes {@code side}'s window (the server's: its workers' too) and returns what it held; null if none was open. */
    public static synchronized Report close(Side side) {
        long now = System.nanoTime();
        Report report;
        if (side == Side.CLIENT) {
            report = client == null ? null : client.report(now, null);
            client = null;
        } else {
            report = server == null ? null : server.report(now, workers == null ? null : workers.report(now, null));
            server = null;
            workers = null;
        }
        on = server != null || client != null;
        return report;
    }

    /** True while {@code side}'s window is open. */
    public static boolean open(Side side) {
        return (side == Side.CLIENT ? client : server) != null;
    }

    /** Seconds {@code side}'s window has been open (0 if closed). */
    public static double seconds(Side side) {
        Window window = side == Side.CLIENT ? client : server;
        return window == null ? 0.0 : (System.nanoTime() - window.began) / 1e9;
    }

    /** One server tick or client frame of {@code nanos}, for the report's first line. */
    public static void tick(Side side, long nanos) {
        Window window = side == Side.CLIENT ? client : server;
        if (window != null && nanos > 0) {
            window.tick(nanos);
        }
    }

    static void push(String site, long now) {
        Frames frames = FRAMES.get();
        frames.fresh(generation);
        if (frames.depth >= DEEP) {
            frames.over++;
            return;
        }
        int at = frames.depth++;
        frames.sites[at] = site;
        frames.starts[at] = now;
        frames.inner[at] = 0L;
    }

    static void pop(String site, long now) {
        Frames frames = FRAMES.get();
        if (!frames.fresh(generation)) {
            return; // began before this window: not counted
        }
        if (frames.over > 0) {
            frames.over--;
            return;
        }
        // A frame whose exit never came (a throwable let through: out of memory) is dropped with what it held.
        int at = frames.depth - 1;
        while (at >= 0 && !site.equals(frames.sites[at])) {
            at--;
        }
        if (at < 0) {
            return; // its enter came before the window opened
        }
        long total = now - frames.starts[at];
        long self = total - frames.inner[at];
        frames.depth = at;
        frames.sites[at] = null;
        if (at > 0) {
            frames.inner[at - 1] += total;
        }
        Window window = windowFor(Thread.currentThread());
        if (window != null) {
            window.add(site, Math.max(0L, self), Math.max(0L, total));
        }
    }

    private static Window windowFor(Thread thread) {
        Window window = server;
        if (window != null && window.thread == thread) {
            return window;
        }
        window = client;
        if (window != null && window.thread == thread) {
            return window;
        }
        if (thread == clientThread) {
            return null; // the client's own calls, while only the server measures
        }
        return workers;
    }

    /** One thread's timed calls in progress. */
    static final class Frames {
        final String[] sites = new String[DEEP];
        final long[] starts = new long[DEEP];
        /** The time of the timed calls inside each, taken off its own. */
        final long[] inner = new long[DEEP];
        int depth;
        int over;
        int generation = -1;

        /** False (and emptied) if these frames belong to an older window. */
        boolean fresh(int now) {
            if (generation == now) {
                return true;
            }
            generation = now;
            java.util.Arrays.fill(sites, null);
            depth = 0;
            over = 0;
            return false;
        }
    }

    /** One place's sums in a window (added to by its threads, read as it closes). */
    static final class Site {
        final String name;
        long self;
        long calls;
        long worst;

        Site(String name) {
            this.name = name;
        }

        synchronized void add(long self, long total) {
            this.self += self;
            this.calls++;
            if (total > this.worst) {
                this.worst = total;
            }
        }

        synchronized Entry entry(double seconds) {
            return new Entry(name, self / 1e6 / seconds, calls / seconds, worst / 1e6);
        }
    }

    static final class Window {
        final Side side;
        final Thread thread;
        final long began;
        final ConcurrentHashMap<String, Site> sites = new ConcurrentHashMap<>();
        long ticks;
        long tickTime;
        long worstTick;

        Window(Side side, Thread thread, long began) {
            this.side = side;
            this.thread = thread;
            this.began = began;
        }

        void add(String site, long self, long total) {
            sites.computeIfAbsent(site, Site::new).add(self, total);
        }

        synchronized void tick(long nanos) {
            ticks++;
            tickTime += nanos;
            worstTick = Math.max(worstTick, nanos);
        }

        synchronized Report report(long now, Report workers) {
            double seconds = Math.max(1e-3, (now - began) / 1e9);
            List<Entry> entries = new ArrayList<>();
            for (Site site : sites.values()) {
                entries.add(site.entry(seconds));
            }
            entries.sort(Comparator.comparingDouble(Entry::msPerSecond).reversed().thenComparing(Entry::site));
            return new Report(side, seconds, ticks, tickTime / 1e6, worstTick / 1e6, List.copyOf(entries), workers);
        }
    }

    /** One place in a report: its own time (ms a second), its calls a second and its slowest call (ms, with what it called). */
    public record Entry(String site, double msPerSecond, double callsPerSecond, double worstMs) {
    }

    /**
     * What a window held: its side, how long it was open, its ticks (the server's) or frames (the client's) and their time, every place
     * by its own time (costliest first) and, for the server, its workers' report.
     */
    public record Report(Side side, double seconds, long ticks, double tickMs, double worstTickMs, List<Entry> entries, Report workers) {
        /** The mod's own time on this side, ms a second. */
        public double ourMsPerSecond() {
            double sum = 0.0;
            for (Entry entry : entries) {
                sum += entry.msPerSecond;
            }
            return sum;
        }

        /** The packets this side sent (our encoders' calls), a second. */
        public double packetsPerSecond() {
            double sum = 0.0;
            for (Entry entry : entries) {
                if (entry.site.endsWith(".encode")) {
                    sum += entry.callsPerSecond;
                }
            }
            return sum;
        }
    }

    /** The report as chat lines: the tick or frame, the ten costliest places, the packets sent, and the server's workers. */
    public static List<String> lines(Report report) {
        List<String> lines = new ArrayList<>();
        boolean server = report.side != Side.CLIENT;
        String where = server ? "the server" : "your game";
        String tick = server ? "tick" : "frame";
        StringBuilder head = new StringBuilder("§6HexKagune lag, " + where + ", " + whole(report.seconds) + " s: §f");
        double ours = report.ourMsPerSecond();
        if (report.ticks > 0) {
            double each = report.tickMs / report.ticks;
            double perSecond = report.ticks / report.seconds;
            double oursEach = ours / perSecond;
            head.append("a ").append(tick).append(" takes ").append(ms(each)).append(" ms on average, the worst ")
                .append(ms(report.worstTickMs)).append(" ms");
            head.append(server ? " (a tick has 50 ms)" : " (" + whole(perSecond) + " fps)");
            head.append("; the mod's own share ").append(ms(oursEach)).append(" ms a ").append(tick);
            if (each > 0.0) {
                head.append(" (").append(whole(Math.min(100.0, 100.0 * oursEach / each))).append("%)");
            }
        } else {
            head.append("no ").append(tick).append(" measured (was the game paused?); the mod's own time ").append(ms(ours)).append(" ms a second");
        }
        lines.add(head.toString());
        int n = 0;
        for (Entry entry : report.entries) {
            if (n == TOP) {
                break;
            }
            n++;
            lines.add("§7 " + n + ". §f" + entry.site + "§7: " + ms(entry.msPerSecond) + " ms/s, " + whole(entry.callsPerSecond)
                + " calls/s, worst " + ms(entry.worstMs) + " ms");
        }
        if (n == 0) {
            lines.add("§7 Nothing of the mod's ran.");
        }
        double packets = report.packetsPerSecond();
        if (packets > 0.0) {
            List<Entry> sent = new ArrayList<>();
            for (Entry entry : report.entries) {
                if (entry.site.endsWith(".encode")) {
                    sent.add(entry);
                }
            }
            sent.sort(Comparator.comparingDouble(Entry::callsPerSecond).reversed().thenComparing(Entry::site));
            StringBuilder line = new StringBuilder("§7 Packets sent: §f" + whole(packets) + "/s§7");
            for (int i = 0; i < Math.min(3, sent.size()); i++) {
                line.append(i == 0 ? ": " : ", ").append(sent.get(i).site.substring(0, sent.get(i).site.length() - ".encode".length()))
                    .append(' ').append(whole(sent.get(i).callsPerSecond));
            }
            lines.add(line.toString());
        }
        Report workers = report.workers;
        if (workers != null && !workers.entries.isEmpty()) {
            StringBuilder line = new StringBuilder("§7 Other threads (world generation): §f" + ms(workers.ourMsPerSecond()) + " ms/s§7");
            for (int i = 0; i < Math.min(3, workers.entries.size()); i++) {
                Entry entry = workers.entries.get(i);
                line.append(i == 0 ? ": " : ", ").append(entry.site).append(' ').append(ms(entry.msPerSecond)).append(" ms/s (worst ")
                    .append(ms(entry.worstMs)).append(" ms)");
            }
            lines.add(line.toString());
        }
        return lines;
    }

    /** Milliseconds as read: two places under ten, one under a hundred, none above. */
    static String ms(double value) {
        if (value < 10.0) {
            return String.format(Locale.ROOT, "%.2f", value);
        }
        return value < 100.0 ? String.format(Locale.ROOT, "%.1f", value) : String.format(Locale.ROOT, "%.0f", value);
    }

    static String whole(double value) {
        return String.format(Locale.ROOT, "%.0f", value);
    }

    /** The lines without their colours (for the log). */
    public static String plain(String line) {
        return line.replaceAll("§.", "");
    }
}
