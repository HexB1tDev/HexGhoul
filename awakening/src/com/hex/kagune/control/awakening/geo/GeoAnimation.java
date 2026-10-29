package com.hex.kagune.control.awakening.geo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The clips of a Bedrock/Blockbench {@code .animation.json} file. Channel values are in the file's
 * own units (degrees, pixels, scale factors); {@link GeoPose} converts them the way GeckoLib does.
 *
 * <p>Keyframes may be plain vectors, {@code {"vector": [...]}}, or {@code {"pre": [...], "post": [...]}}
 * for a jump, and each component a number or a {@link Molang} string. A segment eases with
 * Catmull-Rom when either end says {@code "lerp_mode": "catmullrom"} (or GeckoLib's
 * {@code "easing": "catmullrom"}), as Blockbench previews it; otherwise linearly.
 */
public final class GeoAnimation {
    public enum Loop { ONCE, LOOP, HOLD }

    public final Map<String, Clip> clips;

    private GeoAnimation(Map<String, Clip> clips) {
        this.clips = clips;
    }

    public Clip clip(String name) {
        return clips.get(name);
    }

    public static final class Clip {
        public final String name;
        public final Loop loop;
        /** Length in seconds. */
        public final double length;
        public final Map<String, BoneChannels> bones;

        Clip(String name, Loop loop, double length, Map<String, BoneChannels> bones) {
            this.name = name;
            this.loop = loop;
            this.length = length;
            this.bones = bones;
        }

        /** Time inside the clip for a time since it started, after looping or holding. */
        public double localTime(double seconds) {
            // A clip with no length (only Molang, or a single pose) runs on its own clock.
            if (length <= 0.0) return Math.max(0.0, seconds);
            if (loop == Loop.LOOP) return ((seconds % length) + length) % length;
            return Math.max(0.0, Math.min(length, seconds));
        }

        public boolean finished(double seconds) {
            return loop == Loop.ONCE && seconds >= length;
        }
    }

    public static final class BoneChannels {
        public final Channel rotation;
        public final Channel position;
        public final Channel scale;

        BoneChannels(Channel rotation, Channel position, Channel scale) {
            this.rotation = rotation;
            this.position = position;
            this.scale = scale;
        }
    }

    /** One animated vector: keyframe times with the value arriving at (pre) and leaving (post) each. */
    public static final class Channel {
        final double[] times;
        final Molang.Expr[][] pre;
        final Molang.Expr[][] post;
        final boolean[] smooth;

        Channel(double[] times, Molang.Expr[][] pre, Molang.Expr[][] post, boolean[] smooth) {
            this.times = times;
            this.pre = pre;
            this.post = post;
            this.smooth = smooth;
        }

        public double lastTime() {
            return times.length == 0 ? 0.0 : times[times.length - 1];
        }

        /** Writes the value at {@code time} (seconds) into {@code out}. */
        public void sample(double time, Molang.Context context, double[] out) {
            int n = times.length;
            if (time <= times[0] || n == 1) {
                eval(time <= times[0] ? pre[0] : post[0], context, out);
                return;
            }
            if (time >= times[n - 1]) {
                eval(post[n - 1], context, out);
                return;
            }
            int i = 0;
            while (i + 1 < n && times[i + 1] <= time) i++;
            double span = times[i + 1] - times[i];
            double f = span <= 0.0 ? 1.0 : (time - times[i]) / span;
            double[] a = new double[3];
            double[] b = new double[3];
            eval(post[i], context, a);
            eval(pre[i + 1], context, b);
            if (!(smooth[i] || smooth[i + 1])) {
                for (int k = 0; k < 3; k++) out[k] = a[k] + (b[k] - a[k]) * f;
                return;
            }
            double[] p0 = new double[3];
            double[] p3 = new double[3];
            if (i > 0) eval(post[i - 1], context, p0); else System.arraycopy(a, 0, p0, 0, 3);
            if (i + 2 < n) eval(pre[i + 2], context, p3); else System.arraycopy(b, 0, p3, 0, 3);
            for (int k = 0; k < 3; k++) out[k] = catmullRom(f, p0[k], a[k], b[k], p3[k]);
        }

        private static void eval(Molang.Expr[] value, Molang.Context context, double[] out) {
            for (int k = 0; k < 3; k++) out[k] = value[k].eval(context);
        }
    }

    static double catmullRom(double t, double p0, double p1, double p2, double p3) {
        return 0.5 * (2.0 * p1 + (p2 - p0) * t + (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3) * t * t + (3.0 * p1 - p0 - 3.0 * p2 + p3) * t * t * t);
    }

    public static GeoAnimation read(String json) {
        Map<String, Object> root = Json.map(Json.parse(json), "animation file");
        Map<String, Object> animations = Json.map(root.get("animations"), "animations");
        Map<String, Clip> clips = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : animations.entrySet()) {
            String name = entry.getKey();
            Map<String, Object> raw = Json.map(entry.getValue(), name);
            Object loopValue = raw.get("loop");
            Loop loop = Boolean.TRUE.equals(loopValue) ? Loop.LOOP : "hold_on_last_frame".equals(loopValue) ? Loop.HOLD : Loop.ONCE;
            Map<String, BoneChannels> bones = new LinkedHashMap<>();
            double last = 0.0;
            if (raw.get("bones") != null) {
                for (Map.Entry<String, Object> bone : Json.map(raw.get("bones"), name + " bones").entrySet()) {
                    Map<String, Object> channels = Json.map(bone.getValue(), name + "/" + bone.getKey());
                    String where = name + "/" + bone.getKey();
                    Channel rotation = channel(channels.get("rotation"), where + "/rotation", 0.0);
                    Channel position = channel(channels.get("position"), where + "/position", 0.0);
                    Channel scale = channel(channels.get("scale"), where + "/scale", 1.0);
                    for (Channel c : new Channel[] {rotation, position, scale}) if (c != null) last = Math.max(last, c.lastTime());
                    bones.put(bone.getKey(), new BoneChannels(rotation, position, scale));
                }
            }
            double length = raw.get("animation_length") instanceof Double d ? d : last;
            clips.put(name, new Clip(name, loop, length, bones));
        }
        return new GeoAnimation(clips);
    }

    private static Channel channel(Object raw, String where, double fill) {
        if (raw == null) return null;
        if (!(raw instanceof Map)) {
            Molang.Expr[] value = vector(raw, where, fill);
            return new Channel(new double[] {0.0}, new Molang.Expr[][] {value}, new Molang.Expr[][] {value}, new boolean[] {false});
        }
        Map<String, Object> frames = Json.map(raw, where);
        List<double[]> order = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (Map.Entry<String, Object> frame : frames.entrySet()) {
            double time;
            try {
                time = Double.parseDouble(frame.getKey());
            } catch (NumberFormatException bad) {
                throw new IllegalArgumentException(where + ": keyframe time '" + frame.getKey() + "' is not a number");
            }
            order.add(new double[] {time, values.size()});
            values.add(frame.getValue());
        }
        order.sort((a, b) -> Double.compare(a[0], b[0]));
        int n = order.size();
        if (n == 0) return null;
        double[] times = new double[n];
        Molang.Expr[][] pre = new Molang.Expr[n][];
        Molang.Expr[][] post = new Molang.Expr[n][];
        boolean[] smooth = new boolean[n];
        for (int i = 0; i < n; i++) {
            times[i] = order.get(i)[0];
            Object value = values.get((int) order.get(i)[1]);
            String at = where + "@" + times[i];
            if (value instanceof Map) {
                Map<String, Object> frame = Json.map(value, at);
                Object vector = frame.get("vector");
                Object before = frame.containsKey("pre") ? frame.get("pre") : vector != null ? vector : frame.get("post");
                Object after = frame.containsKey("post") ? frame.get("post") : vector != null ? vector : frame.get("pre");
                if (before == null || after == null) throw new IllegalArgumentException(at + ": keyframe has no value");
                pre[i] = vector(before, at, fill);
                post[i] = vector(after, at, fill);
                Object mode = frame.containsKey("lerp_mode") ? frame.get("lerp_mode") : frame.get("easing");
                smooth[i] = "catmullrom".equals(mode);
            } else {
                pre[i] = vector(value, at, fill);
                post[i] = pre[i];
            }
        }
        return new Channel(times, pre, post, smooth);
    }

    /** A vector of three, or a single value used for all three axes. */
    private static Molang.Expr[] vector(Object raw, String where, double fill) {
        if (raw instanceof List) {
            List<Object> list = Json.list(raw, where);
            if (list.size() != 3) throw new IllegalArgumentException(where + " must have 3 components");
            Molang.Expr[] out = new Molang.Expr[3];
            for (int k = 0; k < 3; k++) out[k] = Molang.of(list.get(k), where);
            return out;
        }
        if (raw == null) return new Molang.Expr[] {Molang.constant(fill), Molang.constant(fill), Molang.constant(fill)};
        Molang.Expr single = Molang.of(raw, where);
        return new Molang.Expr[] {single, single, single};
    }
}
