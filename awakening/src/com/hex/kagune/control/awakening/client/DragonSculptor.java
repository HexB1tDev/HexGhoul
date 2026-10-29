package com.hex.kagune.control.awakening.client;

import com.hex.kagune.control.awakening.DragonBody;
import com.hex.kagune.control.awakening.DragonRig;
import com.hex.kagune.control.awakening.DragonTimeline;
import com.hex.kagune.control.awakening.V3;

/**
 * Procedural mesh of Kaneki's Dragon. 1.0.31: as the source has it (Tokyo Ghoul:re), a full centipede: its body Kaneki's own centipede
 * kagune at the Dragon's size ({@code CentipedeTail}: plum armour plates, red flesh glowing between them, a pair of sickle claws off
 * every plate walking), its great round eyes (thick dark rims, pink irises that glow) in a row down its back and here and there on its
 * flanks; the front head a rounded mass of folded flesh over a mouth of teeth, big round eyes in its sides and one small white eye at
 * its front, the lesser heads the same, and its tentacles. Built from the server frame
 * every render frame; only the look is this file's (the rig moves it, the heads and tentacles strike from where the server has them).
 */
final class DragonSculptor {
    /** 1.0.29: rings per segment (six: each plate a smooth dome). */
    private static final int SUBDIVISIONS = 6;

    private DragonSculptor() {
    }

    /** Everything relative to the pose anchor; {@code lights} holds the packed light per body segment. */
    static void sculpt(DragonMesh mesh, DragonPose pose, double time, int sides, int[] lights) {
        double instability = DragonTimeline.instability((int) pose.tick);
        if (pose.tick < DragonTimeline.RAMPAGE_START + 30) eruption(mesh, pose, time, lights);
        body(mesh, pose, time, instability, sides, lights);
        legs(mesh, pose, time, lights, sides >= 20);
        eyes(mesh, pose, time, instability, lights, sides >= 20);
        for (int h = 0; h < DragonRig.HEADS; h++) head(mesh, pose, h, time, lights);
        for (int t = 0; t < DragonRig.TENTACLES; t++) tentacle(mesh, pose, t, time, lights);
        if (instability > 0.0) cracks(mesh, pose, instability, time);
    }

    private static int lastBorn(DragonPose pose) {
        int last = -1;
        for (int i = 0; i < DragonRig.SEGMENTS; i++) if (pose.born(i)) last = i;
        return last;
    }

    private static V3 catmull(V3 p0, V3 p1, V3 p2, V3 p3, double t) {
        double t2 = t * t;
        double t3 = t2 * t;
        return p1.scale(2.0).add(p2.sub(p0).scale(t)).add(p0.scale(2.0).sub(p1.scale(5.0)).add(p2.scale(4.0)).sub(p3).scale(t2))
                .add(p1.scale(3.0).sub(p0).sub(p2.scale(3.0)).add(p3).scale(t3)).scale(0.5);
    }

    // ---- Body ---------------------------------------------------------------------------------

    /**
     * 1.0.31: its body is Kaneki's centipede kagune ({@code CentipedeTail}, the legs of his centipede form) at the Dragon's size: plate
     * after plate of the same dusky plum armour with their pale V-notched rims, the glowing red flesh between them, a pair of its black
     * sickle claws off every plate rippling in the walking wave; as thick along it as the Dragon's body is ({@code profile}), and with
     * no fang at its front (its head is the Dragon's own).
     */
    private static void body(DragonMesh mesh, DragonPose pose, double time, double instability, int sides, int[] segmentLights) {
        int last = lastBorn(pose);
        if (last < 2) return;
        double widest = 0.0;
        for (int i = 0; i <= last; i++) widest = Math.max(widest, pose.radii[i]);
        if (widest < 0.05) return;
        // Tail to head (the kagune runs from its root to its head).
        double[][] controls = new double[last + 1][];
        double[] radii = new double[last + 1];
        for (int i = last; i >= 0; i--) {
            V3 at = pose.segments[i];
            controls[last - i] = new double[] {at.x(), at.y(), at.z()};
            radii[last - i] = pose.radii[i];
        }
        boolean fine = sides >= 20;
        com.hex.kagune.control.ayato.KakujaModel.Spine spine = com.hex.kagune.control.ayato.CentipedeTail.through(controls, fine ? last * 5 : last * 3, 1,
                new double[] {0.0, 1.0, 0.0});
        double top = widest;
        java.util.function.DoubleUnaryOperator profile = u -> {
            double x = Math.max(0.0, Math.min(last, u * last));
            int k = Math.min(last - 1, (int) Math.floor(x));
            double f = x - k;
            return (radii[k] + (radii[k + 1] - radii[k]) * f) / top;
        };
        com.hex.kagune.control.ayato.CentipedeTail.Look look = new com.hex.kagune.control.ayato.CentipedeTail.Look(widest, plates(last), fine, 1.0, 7, 0.0, 1.0,
                0.45, time * 0.35, profile, true, fine);
        com.hex.kagune.control.ayato.CentipedeTail.tail(redden(DragonMesh.sink(mesh, segmentLights[last / 2])), spine, look);
    }

    /** 1.0.31: its plates: one every {@link #RINGS_PER_PLATE} of its rings (great plates, not a ribbon of thin ones). */
    private static final int RINGS_PER_PLATE = 3;

    private static int plates(int last) {
        return Math.max(8, last / RINGS_PER_PLATE);
    }

    /**
     * 1.0.31: the kagune's colours in the Dragon's: its armour (plum, light to dark) dark red, light to dark as it was; the flesh between
     * its plates (red) light red and hot.
     */
    private static com.hex.kagune.control.ayato.UkakuModel.Sink redden(com.hex.kagune.control.ayato.UkakuModel.Sink sink) {
        return (a, b, c, d, n, ca, cb, cc, cd) -> sink.quad(a, b, c, d, n, redden(ca), redden(cb), redden(cc), redden(cd));
    }

    private static int redden(int argb) {
        int r = argb >> 16 & 0xFF;
        int g = argb >> 8 & 0xFF;
        int b = argb & 0xFF;
        double l = 0.3 * r + 0.59 * g + 0.11 * b;
        int out;
        if (r > 90 && r > g * 1.8 && r > b * 1.5) {
            out = DragonPalette.mix(DragonPalette.INNER_DEEP, DragonPalette.INNER_HOT, Math.min(1.0, Math.max(0.0, (l - 40.0) / 70.0)));
        } else {
            out = DragonPalette.mix(DragonPalette.PLATE_SHADOW, DragonPalette.PLATE_LIGHT, Math.min(1.0, l / 170.0));
        }
        return argb & 0xFF000000 | out;
    }

    /** 1.0.29: how far along its plate ring {@code ring} is (0 at the seam before it, toward 1 at its lip). */
    private static double plateAt(int ring) {
        return (ring % SUBDIVISIONS) / (double) SUBDIVISIONS;
    }

    private static V3 surfaceUp(DragonPose pose, int i) {
        return pose.upAt(i);
    }

    /** 1.0.32: how fast it must crawl (blocks a tick, per block of its girth) for its feet to lift clear in full strides. */
    private static final double WALK_PACE = 0.05;

    /**
     * 1.0.31: its legs: real legs it walks on, a pair under each plate: the thigh out of its flank and up to a knee over its side, the
     * shank straight down from the knee to a clawed foot on the ground, stepping in a wave from head to tail (each foot lifted and
     * swung forward, then planted and drawn back), the two sides in opposite phase. 1.0.32: they step by how far it has really
     * crawled (standing, feet down, when it stops), each foot finds the real ground (up a slope, down into a dip), and where the
     * ground is past a leg's reach (the front reared up, or over a drop) the legs let go and hang under it, paddling.
     */
    private static void legs(DragonMesh mesh, DragonPose pose, double time, int[] lights, boolean fine) {
        int last = lastBorn(pose);
        if (last < 6) return;
        int sides = fine ? 6 : 4;
        double widest = 0.5;
        for (int i = 0; i <= last; i++) widest = Math.max(widest, pose.radii[i]);
        double walking = Math.max(0.0, Math.min(1.0, pose.pace / (WALK_PACE * widest)));
        for (int i = 2; i < last - 1; i += RINGS_PER_PLATE) {
            double r = pose.radii[i];
            if (r < 0.4) continue;
            V3 c = pose.segments[i];
            V3 forward = pose.forwardAt(i);
            V3 side = pose.sideAt(i);
            V3 up = surfaceUp(pose, i);
            int light = lights[i];
            double rest = c.y() - DragonBody.RIDE * r;
            for (int sign = -1; sign <= 1; sign += 2) {
                double phase = Math.PI * 2.0 * pose.steps - i * 0.55 + (sign > 0 ? Math.PI : 0.0);
                double swing = Math.sin(phase);
                double lift = Math.max(0.0, Math.cos(phase)) * walking;
                V3 out = side.scale(sign);
                // Out of the flank under the plate's edge, up and out to a knee held above the back's shoulder, then
                // straight down to a clawed foot planted on the ground: an arch on each side, the centipede's.
                V3 hip = c.add(out.scale(r * 0.92)).add(up.scale(-r * 0.3));
                V3 reach = c.add(out.scale(r * 1.9)).add(forward.scale(swing * r * 0.6));
                double found = pose.floor.at(reach.x(), c.y(), reach.z());
                double ground = Double.isNaN(found) ? rest : found;
                double hang = DragonTimeline.smooth((rest - ground - r * 0.5) / (r * 0.9));
                V3 foot = reach.withY(Math.max(rest - r * 1.4, Math.min(rest + r * 0.9, ground)) + lift * r * 0.5);
                V3 knee = c.add(out.scale(r * 1.55)).add(up.scale(r * (0.62 + 0.3 * lift))).add(forward.scale(swing * r * 0.25));
                knee = knee.withY(Math.max(knee.y(), foot.y() + r * 0.5));
                if (hang > 0.0) {
                    double paddle = Math.sin(time * 0.3 + i * 0.7 + sign);
                    knee = knee.lerp(c.add(out.scale(r * 1.45)).add(up.scale(r * 0.15)).add(forward.scale(paddle * r * 0.2)), hang);
                    foot = foot.lerp(c.add(out.scale(r * 1.6)).add(up.scale(-r * 1.05)).add(forward.scale(paddle * r * 0.5)), hang);
                }
                V3 ankle = knee.lerp(foot, 0.8);
                V3 down = foot.sub(knee).normalize();
                mesh.prism(hip, knee, r * 0.22, r * 0.17, sides, DragonPalette.LEG, DragonPalette.LEG_SHANK, light);
                mesh.prism(knee.sub(out.scale(r * 0.08)), knee.add(out.scale(r * 0.1)), r * 0.2, r * 0.17, sides, DragonPalette.JOINT, DragonPalette.LEG, light);
                mesh.prism(knee, ankle, r * 0.16, r * 0.11, sides, DragonPalette.LEG_SHANK, DragonPalette.LEG, light);
                mesh.prism(ankle.sub(down.scale(r * 0.03)), ankle.add(down.scale(r * 0.04)), r * 0.13, r * 0.11, sides, DragonPalette.JOINT, DragonPalette.LEG_SHANK, light);
                mesh.prism(ankle, foot, r * 0.11, 0.0, sides, DragonPalette.LEG, DragonPalette.LEG_CLAW, light);
            }
        }
    }

    // ---- Eyes ---------------------------------------------------------------------------------

    /** 1.0.31: rings between two eyes down its back, and between two on its flanks. */
    private static final int EYE_EVERY = 4;
    private static final int FLANK_EYE_EVERY = 7;
    /** The rims round them (the many-eyed kagune's, in the Dragon's red). */
    private static final int EYE_RIM = 0x1E0306;
    private static final int EYE_RIM_INNER = 0x7A0A14;

    /**
     * 1.0.31: its eyes: a row of great eyes down its back (a little this side or that of its spine) and here and there on its flanks,
     * realistic ones, the many-eyed kagune's of Furuta's ({@code FurutaMesh.almond}: a white ball with a pupil ringed in red that rolls
     * about, a thick rim of flesh round it, lids that blink).
     */
    private static void eyes(DragonMesh mesh, DragonPose pose, double time, double instability, int[] lights, boolean fine) {
        for (int i = 3; i < DragonRig.SEGMENTS - 4; i++) {
            boolean back = i % EYE_EVERY == 0;
            boolean flank = (i + 2) % FLANK_EYE_EVERY == 0;
            if (!back && !flank) continue;
            double r = Math.min(pose.radii[i], pose.radii[i + 1]);
            if (r < 0.6) continue;
            double angle = back ? Math.toRadians((DragonPalette.hash(i, 51) - 0.5) * 40.0)
                    : ((i / FLANK_EYE_EVERY) % 2 == 0 ? 1 : -1) * Math.toRadians(62.0 + 18.0 * DragonPalette.hash(i, 50));
            double size = r * (back ? 0.26 + 0.08 * DragonPalette.hash(i, 60) : 0.18 + 0.06 * DragonPalette.hash(i, 61));
            V3 center = pose.segments[i].lerp(pose.segments[i + 1], 0.5);
            V3 forward = pose.forwardAt(i);
            V3 side = pose.sideAt(i);
            V3 up = side.cross(forward).normalize();
            V3 normal = up.scale(Math.cos(angle)).add(side.scale(Math.sin(angle))).normalize();
            int seed = i * 31 + (back ? 1 : 2);
            double period = 70.0 + 90.0 * DragonPalette.hash(seed, 3);
            double phase = (time + period * DragonPalette.hash(seed, 4)) % period;
            double blink = phase < 5.0 ? Math.sin(Math.PI * phase / 5.0) : 0.0;
            double wander = time * 0.04 + seed;
            V3 look = forward.scale(Math.sin(wander) * 0.8).add(side.scale(Math.cos(wander * 0.7) * 0.6)).add(normal.scale(0.4));
            V3 at = center.add(normal.scale(r * 0.98));
            com.hex.kagune.control.furuta.FurutaMesh.almond(DragonMesh.sink(mesh, lights[i]), new double[] {at.x(), at.y(), at.z()},
                    new double[] {normal.x(), normal.y(), normal.z()}, new double[] {forward.x(), forward.y(), forward.z()}, size * 3.0, size * 1.6,
                    new double[] {look.x(), look.y(), look.z()}, 1.0 - blink, 0.3 + 0.5 * instability, EYE_RIM, EYE_RIM_INNER, fine, 1.0);
        }
    }

    // ---- Heads ----------------------------------------------------------------------------------

    private static V3 quadratic(V3 a, V3 c, V3 b, double t) {
        return a.scale((1 - t) * (1 - t)).add(c.scale(2 * (1 - t) * t)).add(b.scale(t * t));
    }

    private static void head(DragonMesh mesh, DragonPose pose, int h, double time, int[] lights) {
        int rootSegment = DragonRig.HEAD_ROOTS[h];
        double rootRadius = pose.radii[rootSegment];
        if (rootRadius < 0.3) return;
        V3 root = pose.headRoot(h);
        V3 tip = pose.headTips[h];
        double reach = tip.distance(root);
        // The main neck leaves the front of the body forwards; the side necks rise off the back
        // before they bend towards their prey.
        V3 control = h == 0
                ? root.add(pose.forwardAt(0).scale(reach * 0.45))
                : root.add(pose.upAt(rootSegment).scale(reach * 0.6));
        // 1.0.31: Furuta's kakuja's colossus head ({@code FurutaModel.colossusHead}: the swell, the toothed maw and its mandibles,
        // its two eyes) in the Dragon's plum, no bigger than the body it leads (the lesser heads half that); the neck up to it.
        boolean main = h == 0;
        double size = main ? rootRadius * MAIN_HEAD : Math.max(0.5, rootRadius * 0.55);
        int rings = 13;
        V3[] neck = new V3[rings];
        double[] radii = new double[rings];
        int[] light = new int[rings];
        double neckEnd = 0.62;
        for (int k = 0; k < rings; k++) {
            double t = neckEnd * k / (rings - 1);
            neck[k] = quadratic(root, control, tip, t);
            double start = main ? rootRadius * 0.95 : rootRadius * 0.48;
            radii[k] = start + (size * 0.9 - start) * (t / neckEnd);
            light[k] = lights[rootSegment];
        }
        mesh.tube(neck, radii, 12, ribbed(0.06), light);
        V3 direction = tip.sub(quadratic(root, control, tip, 0.88)).normalize();
        if (direction.length() < 1.0E-6) direction = pose.forwardAt(rootSegment);
        int headLight = lights[rootSegment];
        double[][] path = new double[5][];
        for (int k = 0; k < 4; k++) {
            V3 q = quadratic(root, control, tip, neckEnd + (1.0 - neckEnd) * k / 3.0);
            path[k] = new double[] {q.x(), q.y(), q.z()};
        }
        V3 past = tip.add(direction.scale(size * 0.8));
        path[4] = new double[] {past.x(), past.y(), past.z()};
        com.hex.kagune.control.ayato.KakujaModel.Spine spine = com.hex.kagune.control.ayato.CentipedeTail.through(path, 24, 1, new double[] {0.0, 1.0, 0.0});
        com.hex.kagune.control.furuta.FurutaModel.colossusHead(plum(DragonMesh.sink(mesh, headLight)), spine,
                new com.hex.kagune.control.furuta.FurutaModel.Look(size, true, 1.0, 11 + h * 7, pose.jaws[h], 1.0, 0.5, true, time + h * 17.0, null));
        if (main) {
            // Its one small white eye, at the very front of it, over the mouth.
            double[][] f = spine.at(0.3);
            V3 up = new V3(f[2][0], f[2][1], f[2][2]);
            V3 ahead = new V3(f[1][0], f[1][1], f[1][2]);
            V3 at = new V3(f[0][0], f[0][1], f[0][2]).add(up.scale(size * 1.15)).add(ahead.scale(size * 0.25));
            roundEye(mesh, at, up.add(ahead.scale(0.6)).normalize(), ahead, size * 0.16, true, time, 0.0, headLight);
        }
    }

    /**
     * 1.0.31: Furuta's colours in the Dragon's: his black shell the kagune's plum (light to dark as it was), what is red (its flesh, its
     * gums, its eyes) and what is pale (its teeth) left as they are.
     */
    private static com.hex.kagune.control.ayato.UkakuModel.Sink plum(com.hex.kagune.control.ayato.UkakuModel.Sink sink) {
        return (a, b, c, d, n, ca, cb, cc, cd) -> sink.quad(a, b, c, d, n, plum(ca), plum(cb), plum(cc), plum(cd));
    }

    private static int plum(int argb) {
        int r = argb >> 16 & 0xFF;
        int g = argb >> 8 & 0xFF;
        int b = argb & 0xFF;
        double l = 0.3 * r + 0.59 * g + 0.11 * b;
        if (r > 70 && r > g * 1.8 && r > b * 1.5 || l > 150.0) return argb;
        int shade = DragonPalette.mix(DragonPalette.MEAT_BELLY, DragonPalette.MEAT_HI, Math.min(1.0, l / 70.0));
        return argb & 0xFF000000 | shade;
    }

    private static DragonMesh.Skin ribbed(double depth) {
        return new DragonMesh.Skin() {
            @Override
            public double radius(int ring, double around, V3 direction) {
                return 1.0 + depth * Math.sin(ring * 2.4) + 0.03 * Math.sin(around * 5.0);
            }

            @Override
            public int color(int ring, double around, V3 direction) {
                // 1.0.31: its flesh (the necks', the first tendrils').
                return ring % 3 == 0 ? DragonPalette.SEAM : (direction.y() < -0.4 ? DragonPalette.MEAT_BELLY : DragonPalette.mix(DragonPalette.MEAT,
                        DragonPalette.MEAT_HI, Math.max(0.0, direction.y()) * 0.6));
            }
        };
    }

    /** 1.0.31: the front head against the front of the body it leads. */
    private static final double MAIN_HEAD = 1.0;

    /**
     * 1.0.31: one of its eyes, as the source's: a thick rim of dark flesh standing out round it, the iris glowing pink inside a darker
     * ring, the dark pupil, a glint; now and then it narrows (a blink of the flesh round it), and it twitches as the Dragon breaks.
     * {@code white}: the front head's small white eye, all white with a dark speck.
     */
    private static void roundEye(DragonMesh mesh, V3 at, V3 normal, V3 forward, double size, boolean white, double time, double instability, int light) {
        V3 u = forward.sub(normal.scale(forward.dot(normal))).normalize();
        if (u.length() < 1.0E-6) u = DragonMesh.perpendicular(normal);
        V3 v = normal.cross(u);
        V3 face = at.add(normal.scale(size * 0.08));
        mesh.prism(at.sub(normal.scale(size * 0.1)), face.add(normal.scale(size * 0.12)), size * 1.38, size * 1.18, 12, DragonPalette.MEAT_DEEP, DragonPalette.EYE_RIM,
                light);
        if (white) {
            mesh.disc(face.add(normal.scale(size * 0.13)), u, v, size * 1.05, size * 1.05, 12, DragonPalette.PALE_EYE, 255, normal, DragonMesh.FULL_BRIGHT);
            mesh.disc(face.add(normal.scale(size * 0.15)), u, v, size * 0.22, size * 0.22, 8, DragonPalette.SHELL_EDGE, 255, normal, DragonMesh.FULL_BRIGHT);
            return;
        }
        double period = 90.0 + 70.0 * DragonPalette.hash((int) (size * 100.0), 7);
        double phase = (time + period * 0.5) % period;
        double open = phase < 4.0 ? 0.25 + 0.75 * Math.abs(Math.cos(Math.PI * phase / 4.0)) : 1.0;
        V3 look = u.scale(0.12 * size * Math.sin(time * 0.05 + size) + instability * 0.2 * size * Math.sin(time * 1.9)).add(v.scale(0.08 * size * Math.cos(time * 0.04)));
        mesh.disc(face.add(normal.scale(size * 0.13)), u, v, size * 1.1, size * 1.1 * open, 14, DragonPalette.EYE_RING, 255, normal, DragonMesh.FULL_BRIGHT);
        mesh.disc(face.add(normal.scale(size * 0.15)).add(look.scale(0.5)), u, v, size * 0.86, size * 0.86 * open, 14, DragonPalette.EYE_IRIS, 255, normal,
                DragonMesh.FULL_BRIGHT);
        mesh.disc(face.add(normal.scale(size * 0.17)).add(look), u, v, size * 0.34, size * 0.34 * open, 10, DragonPalette.EYE_PUPIL, 255, normal, DragonMesh.FULL_BRIGHT);
        mesh.disc(face.add(normal.scale(size * 0.19)).add(look).add(u.scale(size * 0.1)).add(v.scale(size * 0.1)), u, v, size * 0.11, size * 0.11 * open, 6,
                DragonPalette.EYE_GLINT, 255, normal, DragonMesh.FULL_BRIGHT);
    }

    // ---- Tentacles ------------------------------------------------------------------------------

    private static void tentacle(DragonMesh mesh, DragonPose pose, int t, double time, int[] lights) {
        int rootSegment = DragonRig.TENTACLE_ROOTS[t];
        double rootRadius = pose.radii[rootSegment];
        if (rootRadius < 0.3) return;
        V3 root = pose.tentacleRoot(t);
        V3 tip = pose.tentacleTips[t];
        double side = DragonBody.tentacleSide(t);
        double length = Math.max(2.0, tip.distance(root));
        // Tentacles leave the upper flank arching up and out.
        V3 out = pose.sideAt(rootSegment).scale(side * 0.6).add(pose.upAt(rootSegment).scale(0.65)).normalize();
        V3 c1 = root.add(out.scale(length * 0.35));
        double w = time * 0.13 + t * 2.1;
        V3 writhe = new V3(Math.sin(w) * 2.2, Math.sin(w * 1.4) * 1.6, Math.cos(w * 0.9) * 2.2).scale(Math.min(1.0, length / 14.0));
        V3 c2 = tip.add(root.sub(tip).scale(0.3)).add(writhe);
        int rings = 16;
        V3[] centers = new V3[rings];
        double[] radii = new double[rings];
        int[] light = new int[rings];
        double start = rootRadius * 0.28;
        for (int k = 0; k < rings; k++) {
            double u = k / (double) (rings - 1);
            double a = 1 - u;
            centers[k] = root.scale(a * a * a).add(c1.scale(3 * a * a * u)).add(c2.scale(3 * a * u * u)).add(tip.scale(u * u * u));
            radii[k] = start * (1.0 - 0.88 * u);
            light[k] = lights[rootSegment];
        }
        mesh.tube(centers, radii, 8, new DragonMesh.Skin() {
            @Override
            public double radius(int ring, double around, V3 direction) {
                return 1.0 + 0.08 * Math.sin(ring * 1.9 + around * 2.0);
            }

            @Override
            public int color(int ring, double around, V3 direction) {
                double spiral = Math.sin(around * 2.0 + ring * 0.9);
                // 1.0.31: its flesh, darkening to the point.
                int base = ring > rings - 4 ? DragonPalette.MEAT_DEEP : DragonPalette.mix(DragonPalette.MEAT, DragonPalette.MEAT_HI, Math.max(0.0, direction.y()) * 0.5);
                return spiral > 0.9 ? DragonPalette.mix(base, DragonPalette.MEAT_DEEP, 0.7) : base;
            }
        }, light);
        V3 direction = tip.sub(centers[rings - 2]).normalize();
        V3 across = DragonMesh.perpendicular(direction);
        for (int claw = 0; claw < 3; claw++) {
            double a = Math.PI * 2.0 * claw / 3.0;
            V3 spread = across.scale(Math.cos(a)).add(direction.cross(across).scale(Math.sin(a)));
            mesh.prism(tip, tip.add(direction.scale(1.1)).add(spread.scale(0.6)), 0.12, 0.0, 3, DragonPalette.MEAT_DEEP, DragonPalette.MEAT_BELLY, lights[rootSegment]);
        }
    }

    // ---- Eruption, cracks, shatter ----------------------------------------------------------------

    /** The first seconds: a rapidly expanding knot of black and crimson tendrils tears out of Kaneki. */
    private static void eruption(DragonMesh mesh, DragonPose pose, double time, int[] lights) {
        double t = pose.tick;
        double grow = DragonTimeline.smooth(t / 26.0) * (1.0 - DragonTimeline.smooth((t - DragonTimeline.RAMPAGE_START - 10) / 20.0));
        if (grow <= 0.01) return;
        int light = lights[DragonRig.SEGMENTS - 1];
        for (int k = 0; k < 16; k++) {
            double yaw = DragonPalette.hash(k, 1) * Math.PI * 2.0;
            double pitch = Math.toRadians(25.0 + 60.0 * DragonPalette.hash(k, 2));
            V3 direction = new V3(Math.cos(yaw) * Math.cos(pitch), Math.sin(pitch), Math.sin(yaw) * Math.cos(pitch));
            double length = (6.0 + 14.0 * DragonPalette.hash(k, 3)) * grow;
            int rings = 10;
            V3[] centers = new V3[rings];
            double[] radii = new double[rings];
            int[] lit = new int[rings];
            for (int i = 0; i < rings; i++) {
                double u = i / (double) (rings - 1);
                double wave = Math.sin(time * 0.4 + k + u * 6.0) * u * 1.8;
                V3 lateral = DragonMesh.perpendicular(direction).scale(wave);
                centers[i] = direction.scale(length * u).add(lateral).add(0.0, -0.5, 0.0);
                radii[i] = (1.1 - u) * 0.9 * grow + 0.05;
                lit[i] = light;
            }
            mesh.tube(centers, radii, 6, ribbed(0.1), lit);
        }
    }

    /** Glowing fissures that spread across the body while it destabilizes. */
    private static void cracks(DragonMesh mesh, DragonPose pose, double instability, double time) {
        double width = 0.08 + 0.32 * instability;
        for (int i = 0; i < DragonRig.SEGMENTS - 1; i++) {
            double r = pose.radii[i];
            if (r < 0.5) continue;
            V3 forward = pose.forwardAt(i);
            V3 side = pose.sideAt(i);
            V3 up = side.cross(forward).normalize();
            int count = 1 + (int) (instability * 3.0);
            for (int c = 0; c < count; c++) {
                double angle = DragonPalette.hash(i, 200 + c) * Math.PI * 2.0;
                V3 previous = null;
                for (int k = 0; k <= 4; k++) {
                    double along = (k / 4.0 - 0.5) * instability;
                    double zig = (k % 2 == 0 ? 1.0 : -1.0) * 0.35 * instability;
                    double a = angle + zig;
                    V3 normal = up.scale(Math.cos(a)).add(side.scale(Math.sin(a)));
                    V3 point = pose.segments[i].lerp(pose.segments[i + 1], 0.5 + along).add(normal.scale(r * 1.04));
                    if (previous != null) {
                        V3 across = forward.cross(normal).normalize().scale(width);
                        mesh.quad(previous.sub(across), previous.add(across), point.add(across), point.sub(across), DragonPalette.CRACK, 255, normal, DragonMesh.FULL_BRIGHT);
                    }
                    previous = point;
                }
            }
        }
    }
}
