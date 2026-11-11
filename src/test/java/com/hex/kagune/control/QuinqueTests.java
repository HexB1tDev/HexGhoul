package com.hex.kagune.control;

import com.hex.kagune.control.quinque.Quinque;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 1.3.25: the CCG's quinques: a case opening into its weapon and shutting again over its frames, never overflowing; the two kinds'
 * strengths; and every file they need in place (a missing model is a purple cube in game): each item's model, its five frames, their
 * texture, the property they are chosen by, its name. 1.3.27: and no recipe: a case is never made, only taken from a fallen CCG
 * investigator, and rarely.
 */
public final class QuinqueTests {
    static int passed;
    static final List<String> FAILURES = new ArrayList<>();
    static final Path RESOURCES = Path.of("src/main/resources");

    private static void check(boolean condition, String description) {
        if (condition) {
            passed++;
        } else {
            FAILURES.add(description);
        }
    }

    /** 2.0.48: the White Reaper's volley: five from one point, opening into an even ring, closing on the aimed-at point, apart past it. */
    static void dawnVolley() {
        double[] from = {10.0, 64.0, -3.0};
        double[][] ways = {{0.0, 0.0, 1.0}, {0.6, 0.0, -0.8}, {0.0, 1.0, 0.0}, {0.48, 0.6, 0.64}};
        for (double[] way : ways) {
            double aimed = 14.0;
            double[][] basis = com.hex.kagune.control.quinque.DawnVolley.basis(way);
            double dot = basis[0][0] * way[0] + basis[0][1] * way[1] + basis[0][2] * way[2];
            double dot2 = basis[1][0] * way[0] + basis[1][1] * way[1] + basis[1][2] * way[2];
            check(Math.abs(dot) < 1.0E-9 && Math.abs(dot2) < 1.0E-9 && basis[1][1] >= 0.0, "the ring is square to its way, its up up");
            int n = com.hex.kagune.control.quinque.DawnVolley.COUNT;
            double[][] start = new double[n][];
            double[][] open = new double[n][];
            double[][] met = new double[n][];
            for (int i = 0; i < n; i++) {
                start[i] = com.hex.kagune.control.quinque.DawnVolley.at(from, way, i, aimed, 0.0);
                open[i] = com.hex.kagune.control.quinque.DawnVolley.at(from, way, i, aimed, aimed * com.hex.kagune.control.quinque.DawnVolley.OPEN);
                met[i] = com.hex.kagune.control.quinque.DawnVolley.at(from, way, i, aimed, aimed);
            }
            double[] centre = {from[0] + way[0] * aimed * com.hex.kagune.control.quinque.DawnVolley.OPEN,
                from[1] + way[1] * aimed * com.hex.kagune.control.quinque.DawnVolley.OPEN, from[2] + way[2] * aimed * com.hex.kagune.control.quinque.DawnVolley.OPEN};
            double[] aim = {from[0] + way[0] * aimed, from[1] + way[1] * aimed, from[2] + way[2] * aimed};
            boolean together = true;
            boolean ring = true;
            boolean closed = true;
            double nearest = 1.0E9;
            for (int i = 0; i < n; i++) {
                together &= distance(start[i], from) < 1.0E-9;
                double[] off = {open[i][0] - centre[0], open[i][1] - centre[1], open[i][2] - centre[2]};
                double across = off[0] * basis[0][0] + off[1] * basis[0][1] + off[2] * basis[0][2];
                double upward = off[0] * basis[1][0] + off[1] * basis[1][1] + off[2] * basis[1][2];
                double a = com.hex.kagune.control.quinque.DawnVolley.angle(i);
                ring &= Math.abs(across - Math.cos(a) * com.hex.kagune.control.quinque.DawnVolley.RADIUS) < 1.0E-9
                    && Math.abs(upward - Math.sin(a) * com.hex.kagune.control.quinque.DawnVolley.RADIUS_UP) < 1.0E-9;
                closed &= distance(met[i], aim) < 1.0E-9;
                nearest = Math.min(nearest, distance(open[i], open[(i + 1) % n]));
            }
            check(together, "all five leave the one point");
            check(ring, "they open into a ring, evenly round it (" + com.hex.kagune.control.quinque.DawnVolley.RADIUS + " across, "
                + com.hex.kagune.control.quinque.DawnVolley.RADIUS_UP + " up and down)");
            check(nearest > 1.0, "never two together while it is open");
            check(closed, "and close on the point aimed at");
            double[] past0 = com.hex.kagune.control.quinque.DawnVolley.at(from, way, 0, aimed, aimed * 1.5);
            double[] past2 = com.hex.kagune.control.quinque.DawnVolley.at(from, way, 2, aimed, aimed * 1.5);
            check(distance(past0, past2) > 1.0, "past it they spread again");
        }
        double[] top = com.hex.kagune.control.quinque.DawnVolley.at(from, new double[] {0.0, 0.0, 1.0}, 0, 14.0, 14.0 * 0.35);
        check(top[1] > from[1] + 0.95, "the first ball is the top of the ring");
        double lowest = 0.0;
        for (int i = 0; i < com.hex.kagune.control.quinque.DawnVolley.COUNT; i++) {
            for (double t = 0.0; t <= 14.0; t += 0.25) {
                lowest = Math.min(lowest, com.hex.kagune.control.quinque.DawnVolley.at(from, new double[] {0.0, 0.0, 1.0}, i, 14.0, t)[1] - from[1]);
            }
        }
        check(lowest > -1.0, "from chest height none of it dips to the ground (" + lowest + ")");
        check(com.hex.kagune.control.quinque.DawnVolley.spread(0.0) == 0.0 && com.hex.kagune.control.quinque.DawnVolley.spread(Double.NaN) == 0.0
            && com.hex.kagune.control.quinque.DawnVolley.spread(com.hex.kagune.control.quinque.DawnVolley.OPEN) == 1.0
            && com.hex.kagune.control.quinque.DawnVolley.spread(1.0) == 0.0, "its spread: shut, open, shut");
        // A near quarry: the ring still opens (taken at the nearest it allows), and it doesn't go wild.
        double[] near = com.hex.kagune.control.quinque.DawnVolley.at(from, new double[] {0.0, 0.0, 1.0}, 3, 1.0, 2.0);
        check(distance(near, from) < 4.0, "a near shot stays near its line");
        check(com.hex.kagune.control.quinque.DawnVolley.SHARE * com.hex.kagune.control.quinque.DawnVolley.COUNT <= 2.0F + 1.0E-6F,
            "all five on one body: at most twice a single shot");
        check(com.hex.kagune.control.npc.AgentRules.volley(10.0, 0.1) && !com.hex.kagune.control.npc.AgentRules.volley(10.0, 0.9)
            && !com.hex.kagune.control.npc.AgentRules.volley(4.0, 0.0), "the volley from far enough off, half the time");
    }

    /**
     * 4.1.17 (the owner: "We need to improve every single M1 of every single weapon ... the animations always have to be top tier"): a swing
     * written by intent is drawn as it says: the baked keys (QuinqueKeys) are its aims solved as they stand now; at each aimed key the blade
     * points within a few degrees of its aim; and between keys the arm never turns over (no wrist flip).
     */
    static void aimed() {
        String baked = com.hex.kagune.control.client.animation.QuinqueKeysBaker.source();
        try {
            String file = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/client/animation/QuinqueKeys.java"));
            check(file.equals(baked), "QuinqueKeys.java is its aims solved as they stand (run tools/QuinqueAimBake.java after changing an aim)");
        } catch (java.io.IOException e) {
            check(false, "QuinqueKeys.java can be read");
        }
        for (Quinque q : Quinque.values()) {
            for (int step = 0; step < q.steps(); step++) {
                com.hex.kagune.control.client.animation.QuinqueAim.Aim[] aims = com.hex.kagune.control.client.animation.QuinqueKeysBaker.aims(q, step);
                if (aims == null) {
                    continue;
                }
                double lastPitch = Double.NaN;
                double lastYaw = 0.0;
                double lastRoll = 0.0;
                for (double t = 0.0; t <= 1.0001; t += 0.02) {
                    com.hex.kagune.control.client.animation.RigPose p = com.hex.kagune.control.client.animation.QuinquePose.swing(q, step, t);
                    float[] arm = {p.rightArmX, p.rightArmY, p.rightArmZ};
                    if (!Double.isNaN(lastPitch)) {
                        double jump = Math.max(Math.abs(arm[0] - lastPitch), Math.max(Math.abs(arm[1] - lastYaw), Math.abs(arm[2] - lastRoll)));
                        check(jump < 0.9, q + " " + step + " at " + String.format("%.2f", t) + ": the arm moves, never turns over (" + jump + ")");
                    }
                    lastPitch = arm[0];
                    lastYaw = arm[1];
                    lastRoll = arm[2];
                }
                for (com.hex.kagune.control.client.animation.QuinqueAim.Aim aim : aims) {
                    if (aim.blade() == null) {
                        continue;
                    }
                    com.hex.kagune.control.client.animation.RigPose p = new com.hex.kagune.control.client.animation.RigPose();
                    double[] at = com.hex.kagune.control.client.animation.QuinqueAim.solve(q, aim.blade(), aim.hand(), new double[] {-1.15, -0.75, 0.49});
                    com.hex.kagune.control.client.animation.QuinqueGrip.aimRight(p, at[0], at[1], at[2]);
                    double[] dir = com.hex.kagune.control.client.animation.QuinqueAim.blade(p, q);
                    double[] want = com.hex.kagune.control.client.animation.QuinqueAim.unit(new double[] {-aim.blade()[0], -aim.blade()[1], -aim.blade()[2]});
                    double off = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, dir[0] * want[0] + dir[1] * want[1] + dir[2] * want[2]))));
                    check(off < 30.0, q + " " + step + " at " + aim.t() + ": the blade points where it is aimed (" + Math.round(off) + " degrees off)");
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        turning();
        aimed();
        firstPerson();
        meshes();
        kinds();
        files();
        holds();
        dawn();
        dawnIdle();
        dawnFire();
        dawnVolley();
        if (!FAILURES.isEmpty()) {
            FAILURES.stream().limit(40).forEach(failure -> System.err.println("FAILED: " + failure));
            System.err.println(FAILURES.size() + " of " + (passed + FAILURES.size()) + " checks failed");
            System.exit(1);
        }
        System.out.println("Quinque tests: " + passed + " checks passed");
    }

    /**
     * 4.1.19 (the owner: "don't forget the the first person animation, like first person view to match the animation that they got, because
     * they currently don't have that"): a melee quinque in first person is the third person chain seen from FirstPersonQuinque's view, and
     * ArmItemLayer draws it in that order. The camera never spins with the body; the body's step carries the weapon away. At rest every
     * one is in view, right of and below the crosshair; through every swing some of it is always in view, its impact (0.4 to 0.65) always
     * is, and no part of it comes nearer the camera than {@link #FIRST_PERSON_NEAR}. The draw out of the case is in view all the way.
     */
    static final double FIRST_PERSON_NEAR = 1.0;

    static void firstPerson() throws IOException {
        // ArmItemLayer.throughBody: FirstPersonQuinque.view's steps, in its order, then the third person chain.
        String layer = Files.readString(Path.of("src/main/java/com/hex/kagune/control/client/render/ArmItemLayer.java"));
        String[] order = {"FirstPersonQuinque.still(p)", "pose.m_85841_(zoom, zoom, 1.0F)", "FirstPersonQuinque.PITCH, 1, 0, 0",
            "pose.m_85837_(0.0, -p.drop / 16.0, -p.lunge / 16.0)", "pose.m_85841_(-model, -model, model)",
            "pose.m_85837_(-eye[0] / 16.0, -eye[1] / 16.0, -eye[2] / 16.0)", "still.transform(\"rightArm\"", "pose.m_85837_(1.0 / 16.0, 0.125, -0.625)"};
        int at = layer.indexOf("static void throughBody(");
        for (String step : order) {
            int next = at < 0 ? -1 : layer.indexOf(step, at);
            check(next > at, "first person: ArmItemLayer.throughBody does " + step + " in FirstPersonQuinque.view's order");
            at = next;
        }
        check(layer.contains("throughBody(pose, body, \"slim\".equals(player.m_108564_()))")
            && layer.contains("ItemDisplayContext.THIRD_PERSON_RIGHT_HAND"), "first person: the held quinque drawn through the body with its third person transform");
        for (Quinque q : Quinque.values()) {
            com.hex.kagune.control.client.animation.QuinqueHolds.Hold h = com.hex.kagune.control.client.animation.QuinqueHolds.of(q);
            if (q.ranged || h == null) {
                continue;
            }
            List<float[]> corners = corners(q);
            check(corners.size() >= 8, q + ": its full model read for first person");
            com.hex.kagune.control.client.animation.RigPose stance = com.hex.kagune.control.client.animation.QuinquePose.hold(q);
            if (q.twoHanded) {
                com.hex.kagune.control.client.animation.QuinqueGrip.reach(stance, q, false);
            }
            double[] grip = com.hex.kagune.control.client.animation.FirstPersonQuinque.held(stance, h, false, h.rightGrip());
            check(com.hex.kagune.control.client.animation.FirstPersonQuinque.seen(grip) && grip[0] > 0.0 && grip[1] < 0.0,
                q + ": first person at rest: the grip in view, right of and below the crosshair");
            // The camera never spins: the finisher's whole turn changes nothing in first person.
            com.hex.kagune.control.client.animation.RigPose spun = new com.hex.kagune.control.client.animation.RigPose().add(stance, 1.0F);
            spun.rightArmHold = stance.rightArmHold;
            spun.spin += 2.1F;
            double[] spunGrip = com.hex.kagune.control.client.animation.FirstPersonQuinque.held(spun, h, false, h.rightGrip());
            check(Math.abs(spunGrip[0] - grip[0]) + Math.abs(spunGrip[1] - grip[1]) + Math.abs(spunGrip[2] - grip[2]) < 1.0E-6,
                q + ": first person leaves the body's whole turn out");
            // The step: a lunge carries the weapon away along the look (tipped down by the view), as far as the body goes.
            com.hex.kagune.control.client.animation.RigPose lunged = new com.hex.kagune.control.client.animation.RigPose().add(stance, 1.0F);
            lunged.rightArmHold = stance.rightArmHold;
            lunged.lunge += 8.0F;
            double[] away = com.hex.kagune.control.client.animation.FirstPersonQuinque.held(lunged, h, false, h.rightGrip());
            check(Math.abs((grip[2] - away[2]) - 0.5 * Math.cos(com.hex.kagune.control.client.animation.FirstPersonQuinque.PITCH)) < 1.0E-4,
                q + ": a lunge of 8 pixels carries the weapon half a block away in first person");
            for (int i = 1; i <= 6; i++) {
                com.hex.kagune.control.client.animation.RigPose out = com.hex.kagune.control.client.animation.QuinquePose.unfolding(q, i / 7.0F);
                check(com.hex.kagune.control.client.animation.FirstPersonQuinque.seen(
                    com.hex.kagune.control.client.animation.FirstPersonQuinque.held(out, h, false, h.rightGrip())), q + ": first person: the draw in view at " + i + "/7");
            }
            for (int step = 0; step < q.steps(); step++) {
                // 4.1.19: the mace's second swing (still written as arm angles) winds up behind the head: its head passes half a block over
                // the camera. Its string is rewritten next (the quinque overhaul), under the same rule as the rest.
                double near = q == Quinque.MACE && step == 1 ? 0.5 : FIRST_PERSON_NEAR;
                double nearest = Double.MAX_VALUE;
                boolean always = true;
                boolean impact = false;
                for (int k = 0; k <= 50; k++) {
                    double t = k / 50.0;
                    com.hex.kagune.control.client.animation.RigPose p = com.hex.kagune.control.client.animation.QuinquePose.swing(q, step, t);
                    if (q.twoHanded) {
                        com.hex.kagune.control.client.animation.QuinqueGrip.reach(p, q, false);
                    }
                    boolean seen = false;
                    for (float[] c : corners) {
                        double[] v = com.hex.kagune.control.client.animation.FirstPersonQuinque.held(p, h, false, c);
                        nearest = Math.min(nearest, Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]));
                        seen |= com.hex.kagune.control.client.animation.FirstPersonQuinque.seen(v);
                    }
                    always &= seen;
                    impact |= seen && t >= 0.4 && t <= 0.65;
                }
                check(always, q + " swing " + (step + 1) + ": always some of it in view in first person");
                check(impact, q + " swing " + (step + 1) + ": its impact in view in first person");
                check(nearest >= near, q + " swing " + (step + 1) + ": never nearer the camera than " + near + " (" + nearest + ")");
            }
        }
    }

    /**
     * 4.1.20 (the owner: "The katana should be like every weapon in the game should look like it's made from a kagune ... I don't want
     * squares"): a quinque grown as a mesh is smooth (thousands of quads, no boxes), as long as its old model, turned onto the handheld
     * diagonal exactly as the baker turned its boxes, so both its grips lie on its hilt's axis; it grows out of the fist as it comes out of
     * its case; its weapon frames are drawn by code (builtin/entity, no elements) and its case's are still boxes; and its renderer and item
     * override the game's methods by the names the game calls (renderByItem m_108829_, Forge's initializeClient), read in the compiled
     * classes (4.1.9's lesson: a stub's Mojang name compiles and never runs).
     */
    static void meshes() throws IOException {
        for (Quinque q : Quinque.values()) {
            String full = read("assets/hexkagune/models/item/quinque/" + kind(q) + "_full.json");
            boolean byCode = full != null && full.contains("\"minecraft:builtin/entity\"");
            check(byCode == com.hex.kagune.control.quinque.QuinqueMesh.has(q), q + ": its weapon drawn by code exactly when it is a mesh");
            if (!com.hex.kagune.control.quinque.QuinqueMesh.has(q)) {
                check(com.hex.kagune.control.quinque.QuinqueMesh.of(q) == null, q + ": no mesh");
                continue;
            }
            for (String frame : new String[] {"full", "out_1", "out_2", "out_3", "out_4", "out_5", "out_6"}) {
                String json = read("assets/hexkagune/models/item/quinque/" + kind(q) + "_" + frame + ".json");
                check(json != null && json.contains("\"minecraft:builtin/entity\"") && !json.contains("\"elements\"")
                    && json.contains("\"thirdperson_righthand\""), q + ": its " + frame + " frame drawn by code, placed by its display");
            }
            for (String frame : new String[] {"case_closed", "case_open", "case_carried"}) {
                String json = read("assets/hexkagune/models/item/quinque/" + kind(q) + "_" + frame + ".json");
                check(json != null && json.contains("\"elements\""), q + ": its " + frame + " frame still a case of boxes");
            }
            com.hex.kagune.control.quinque.ArmMesh.Mesh mesh = com.hex.kagune.control.quinque.QuinqueMesh.of(q);
            check(mesh != null && mesh.quads >= 2000, q + ": a smooth mesh (" + (mesh == null ? 0 : mesh.quads) + " quads)");
            if (mesh == null) {
                continue;
            }
            boolean finite = true;
            float low = Float.MAX_VALUE;
            float high = -Float.MAX_VALUE;
            float wide = 0.0F;
            for (int i = 0; i < mesh.pos.length; i += 3) {
                finite &= Float.isFinite(mesh.pos[i]) && Float.isFinite(mesh.pos[i + 1]) && Float.isFinite(mesh.pos[i + 2])
                    && Float.isFinite(mesh.normal[i]) && Float.isFinite(mesh.normal[i + 1]) && Float.isFinite(mesh.normal[i + 2]);
                low = Math.min(low, mesh.pos[i + 1]);
                high = Math.max(high, mesh.pos[i + 1]);
                wide = Math.max(wide, Math.max(Math.abs(mesh.pos[i] - 8.0F), Math.abs(mesh.pos[i + 2] - 8.0F)));
            }
            check(finite, q + ": every point and normal of its mesh a number");
            check(low > -7.0F && low < -5.0F && high > 31.0F && high < 32.5F && wide < 4.0F,
                q + ": its mesh as long as its old model (" + low + " .. " + high + ", " + wide + " across)");
            // Its grips: turned back off the diagonal, on the hilt's axis (x 8, z 8), between its ends.
            com.hex.kagune.control.client.animation.QuinqueHolds.Hold h = com.hex.kagune.control.client.animation.QuinqueHolds.of(q);
            for (float[] grip : new float[][] {h.rightGrip(), h.leftGrip()}) {
                float[] back = unturned(grip);
                float[] again = com.hex.kagune.control.quinque.QuinqueMesh.turned(back);
                check(Math.abs(again[0] - grip[0]) + Math.abs(again[1] - grip[1]) < 1.0E-4F, q + ": turned undoes unturned");
                check(Math.abs(back[0] - 8.0F) < 0.05F && Math.abs(back[2] - 8.0F) < 0.05F && back[1] > low + 0.5F && back[1] < 6.0F,
                    q + ": its grip " + java.util.Arrays.toString(grip) + " on its hilt's axis (standing " + java.util.Arrays.toString(back) + ")");
            }
            // The baker's turn: an element turned -45 about z through 8 8 8 (tools/quinque_models.py box, FaceBakery's Rz).
            float[] t = com.hex.kagune.control.quinque.QuinqueMesh.turned(new float[] {8.0F, 0.0F, 8.0F});
            check(Math.abs(t[0] - (8.0F - 8.0F / (float)Math.sqrt(2.0))) < 1.0E-4F && Math.abs(t[1] - t[0]) < 1.0E-4F,
                q + ": standing down the hilt goes down the diagonal, as the baker's boxes");
            // The draw: grown out of the fist; the case round the grip shrinking to nothing.
            check(com.hex.kagune.control.quinque.QuinqueMesh.drawn(31.0F, 1.0F) == 31.0F && com.hex.kagune.control.quinque.QuinqueMesh.drawn(31.0F, 0.0F)
                == (float)com.hex.kagune.control.quinque.QuinqueMesh.HAND, q + ": all out when out, all in the fist when not");
            float last = -Float.MAX_VALUE;
            float lastCase = Float.MAX_VALUE;
            boolean growing = true;
            for (int k = 0; k <= 20; k++) {
                float out = k / 20.0F;
                float tip = com.hex.kagune.control.quinque.QuinqueMesh.drawn(high, out);
                growing &= tip > last;
                last = tip;
                float[] box = com.hex.kagune.control.quinque.QuinqueMesh.caseBox(out);
                if (box != null) {
                    float size = (box[3] - box[0]) * (box[4] - box[1]);
                    growing &= size < lastCase && box[1] > (float)com.hex.kagune.control.quinque.QuinqueMesh.HAND;
                    lastCase = size;
                } else {
                    growing &= k == 20;
                }
            }
            check(growing, q + ": coming out it grows out of the fist and its case shrinks into it, gone only once out");
            check(Math.abs(com.hex.kagune.control.quinque.QuinqueMesh.out(0.95F) - 1.0F) < 1.0E-6F
                && com.hex.kagune.control.quinque.QuinqueMesh.out(0.2F) == 0.0F
                && Math.abs(com.hex.kagune.control.quinque.QuinqueMesh.out(0.3071F) - 1.0F / 7.0F) < 0.01F,
                q + ": the renderer reads its frames' deploy as the baker placed them (case open 0.2, out_1 a seventh, full 0.95)");
        }
        String renderer = compiled("com/hex/kagune/control/client/QuinqueItemRenderer.class");
        check(renderer.contains("m_108829_") && renderer.contains("(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V"),
            "the quinques' renderer overrides renderByItem by its run-time name m_108829_ (compiled)");
        String item = compiled("com/hex/kagune/control/ControlItems$MeshQuinque.class");
        check(item.contains("initializeClient") && item.contains("(Ljava/util/function/Consumer;)V") && item.contains("net/minecraftforge/fml/DistExecutor"),
            "a mesh quinque gives Forge its renderer in initializeClient, through DistExecutor (compiled)");
        String source = Files.readString(Path.of("src/main/java/com/hex/kagune/control/client/QuinqueItemRenderer.java"));
        check(source.contains("QuinqueMesh.DIAGONAL), 0.0F, 0.0F, 1.0F)") && source.indexOf("pose.m_85837_(0.5, 0.5, 0.5)") < source.indexOf("QuinqueMesh.DIAGONAL")
            && source.indexOf("QuinqueMesh.DIAGONAL") < source.indexOf("pose.m_85837_(-0.5, -0.5, -0.5)"),
            "the renderer turns the mesh onto the diagonal about the model's middle, as QuinqueMesh.turned does");
        // Loaded as the items are registered (its offer), long before the first frame: no render type made by loading it.
        check(source.contains("private static final class Solid") && !source.replaceAll("(?s)private static final class Solid \\{.*?\\}", "")
            .contains("RenderType.m_"), "the renderer makes its render type the first time it draws, not as the items are registered");
    }

    /** The baker's kind of {@code q}'s models (its file names). */
    static String kind(Quinque q) {
        return switch (q) {
            case SPEAR -> "koukaku";
            case KATANA -> "katana";
            case MACE -> "mace";
            case LAUNCHER -> "launcher";
            case BLADE -> "bikaku";
            case DAWN -> "dawn";
        };
    }

    /** A point turned onto the diagonal (item pixels) back to standing: QuinqueMesh.turned undone. */
    static float[] unturned(float[] p) {
        double a = -Math.toRadians(com.hex.kagune.control.quinque.QuinqueMesh.DIAGONAL);
        double x = p[0] - 8.0;
        double y = p[1] - 8.0;
        return new float[] {(float)(8.0 + x * Math.cos(a) - y * Math.sin(a)), (float)(8.0 + x * Math.sin(a) + y * Math.cos(a)), p[2]};
    }

    /** A compiled class's bytes as text (its constant pool readable). */
    static String compiled(String path) throws IOException {
        try (java.io.InputStream in = QuinqueTests.class.getClassLoader().getResourceAsStream(path)) {
            return in == null ? "" : new String(in.readAllBytes(), java.nio.charset.StandardCharsets.ISO_8859_1);
        }
    }

    /** {@code q}'s weapon model ("full" frame): each element's eight corners (its pixels, its own turn applied). */
    static List<float[]> corners(Quinque q) throws IOException {
        List<float[]> out = new ArrayList<>();
        if (com.hex.kagune.control.quinque.QuinqueMesh.has(q)) {
            // 4.1.20: a mesh's points (every fifth), turned onto the diagonal as the item draws it.
            com.hex.kagune.control.quinque.ArmMesh.Mesh mesh = com.hex.kagune.control.quinque.QuinqueMesh.of(q);
            for (int v = 0; v < mesh.pos.length / 3; v += 5) {
                out.add(com.hex.kagune.control.quinque.QuinqueMesh.turned(new float[] {mesh.pos[v * 3], mesh.pos[v * 3 + 1], mesh.pos[v * 3 + 2]}));
            }
            return out;
        }
        String item = read("assets/hexkagune/models/item/" + q.weaponId + ".json");
        Matcher full = item == null ? null : Pattern.compile("\"hexkagune:item/(quinque/[a-z_]+_full)\"").matcher(item);
        String model = full != null && full.find() ? read("assets/hexkagune/models/item/" + full.group(1) + ".json") : null;
        if (model == null) {
            return out;
        }
        String num = "\\s*(-?[0-9.]+)\\s*";
        Pattern box = Pattern.compile("\"from\":\\s*\\[" + num + "," + num + "," + num + "\\],\\s*\"to\":\\s*\\[" + num + "," + num + "," + num + "\\]");
        Pattern turn = Pattern.compile("\"rotation\":\\s*\\{\\s*\"angle\":" + num + ",\\s*\"axis\":\\s*\"([xyz])\",\\s*\"origin\":\\s*\\[" + num + "," + num + "," + num + "\\]");
        Matcher m = box.matcher(model);
        List<Integer> starts = new ArrayList<>();
        List<double[]> boxes = new ArrayList<>();
        while (m.find()) {
            starts.add(m.start());
            double[] b = new double[6];
            for (int i = 0; i < 6; i++) {
                b[i] = Double.parseDouble(m.group(i + 1));
            }
            boxes.add(b);
        }
        for (int e = 0; e < boxes.size(); e++) {
            String rest = model.substring(starts.get(e), e + 1 < starts.size() ? starts.get(e + 1) : model.length());
            Matcher r = turn.matcher(rest);
            boolean turned = r.find();
            double[] b = boxes.get(e);
            for (int c = 0; c < 8; c++) {
                double[] p = {(c & 1) == 0 ? b[0] : b[3], (c & 2) == 0 ? b[1] : b[4], (c & 4) == 0 ? b[2] : b[5]};
                if (turned) {
                    double a = Math.toRadians(Double.parseDouble(r.group(1)));
                    double[] o = {Double.parseDouble(r.group(3)), Double.parseDouble(r.group(4)), Double.parseDouble(r.group(5))};
                    double x = p[0] - o[0];
                    double y = p[1] - o[1];
                    double z = p[2] - o[2];
                    double cos = Math.cos(a);
                    double sin = Math.sin(a);
                    switch (r.group(2)) {
                        case "x" -> p = new double[] {x, y * cos - z * sin, y * sin + z * cos};
                        case "y" -> p = new double[] {x * cos + z * sin, y, -x * sin + z * cos};
                        default -> p = new double[] {x * cos - y * sin, x * sin + y * cos, z};
                    }
                    p = new double[] {p[0] + o[0], p[1] + o[1], p[2] + o[2]};
                }
                out.add(new float[] {(float)p[0], (float)p[1], (float)p[2]});
            }
        }
        return out;
    }

    /** 2.0.31: Dawn's own string: its sweep takes the others in front, its thrust the others in its line; its ribbons follow each swing. */
    /**
     * 3.3.2 (the owner: Dawn needs an idle): at rest it starts from the stance and loops: the turn in the fingers, the raise aside and
     * back; its three blocks never go down through the ground or through his head, the twirl never sweeps it round; the other quinques
     * keep their stance.
     */
    static void dawnIdle() {
        com.hex.kagune.control.client.animation.QuinqueHolds.Hold h = com.hex.kagune.control.client.animation.QuinqueHolds.of(Quinque.DAWN);
        com.hex.kagune.control.client.animation.RigPose stance = com.hex.kagune.control.client.animation.QuinquePose.hold(Quinque.DAWN);
        com.hex.kagune.control.client.animation.RigPose start = com.hex.kagune.control.client.animation.QuinquePose.idle(Quinque.DAWN, 0.0);
        check(Math.abs(start.rightArmX - stance.rightArmX) < 1.0E-6 && Math.abs(start.rightArmY - stance.rightArmY) < 1.0E-6
            && Math.abs(start.rightArmZ - stance.rightArmZ) < 1.0E-6, "Dawn's idle starts in its stance");
        float[] grip = h.rightGrip();
        float[] tip = {grip[0] + 44.0F, grip[1], grip[2]};
        boolean aboveGround = true;
        boolean offHead = true;
        double highest = 0.0;
        double turn = 0.0;
        double[] restDir = dir(stance, h, grip, tip);
        for (double t = 0.0; t < com.hex.kagune.control.client.animation.QuinquePose.DAWN_SETTLE + com.hex.kagune.control.client.animation.QuinquePose.DAWN_LOOP; t += 0.5) {
            com.hex.kagune.control.client.animation.RigPose p = com.hex.kagune.control.client.animation.QuinquePose.idle(Quinque.DAWN, t);
            for (int k = 0; k <= 8; k++) {
                float[] at = {grip[0] + 44.0F * k / 8.0F, grip[1], grip[2]};
                double[] w = com.hex.kagune.control.client.animation.QuinqueGrip.held(p, h, false, at);
                aboveGround &= w[1] < 23.0; // model pixels, y down: the feet at 24
                offHead &= !(Math.abs(w[0]) < 4.5 && w[1] > -8.5 && w[1] < 0.5 && Math.abs(w[2]) < 4.5);
            }
            highest = Math.max(highest, com.hex.kagune.control.client.animation.QuinquePose.dawnShoulder(t));
            if (t > com.hex.kagune.control.client.animation.QuinquePose.DAWN_SETTLE + 55 && t < com.hex.kagune.control.client.animation.QuinquePose.DAWN_SETTLE + 97) {
                double[] d = dir(p, h, grip, tip);
                turn = Math.max(turn, Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, d[0] * restDir[0] + d[1] * restDir[1] + d[2] * restDir[2])))));
            }
        }
        check(aboveGround, "Dawn's idle: its whole length stays above the ground");
        check(offHead, "Dawn's idle: never through his head");
        check(highest == 1.0 && com.hex.kagune.control.client.animation.QuinquePose.dawnShoulder(com.hex.kagune.control.client.animation.QuinquePose.DAWN_SETTLE
            + com.hex.kagune.control.client.animation.QuinquePose.DAWN_LOOP - 1) == 0.0, "Dawn's idle: raised aside, then back in the stance before the loop ends");
        check(turn > 5.0 && turn < 45.0, "Dawn's idle: turned in the fingers, its point circling a little, never swept round (" + turn + " degrees)");
        for (Quinque q : Quinque.values()) {
            if (q == Quinque.DAWN || com.hex.kagune.control.client.animation.QuinquePose.hold(q) == null) {
                continue;
            }
            com.hex.kagune.control.client.animation.RigPose a = com.hex.kagune.control.client.animation.QuinquePose.idle(q, 200.0);
            com.hex.kagune.control.client.animation.RigPose b = com.hex.kagune.control.client.animation.QuinquePose.hold(q);
            check(a.rightArmX == b.rightArmX && a.rightArmY == b.rightArmY && a.lean == b.lean, q + ": no idle of Dawn's, its own stance");
        }
    }

    static double[] dir(com.hex.kagune.control.client.animation.RigPose p, com.hex.kagune.control.client.animation.QuinqueHolds.Hold h, float[] from, float[] to) {
        double[] a = com.hex.kagune.control.client.animation.QuinqueGrip.held(p, h, false, from);
        double[] b = com.hex.kagune.control.client.animation.QuinqueGrip.held(p, h, false, to);
        double[] d = {b[0] - a[0], b[1] - a[1], b[2] - a[2]};
        double l = Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]);
        return new double[] {d[0] / l, d[1] / l, d[2] / l};
    }

    static void dawn() {
        check(Quinque.DAWN.arc(0) == 3.0 && Quinque.DAWN.arc(1) == 0.0 && Quinque.DAWN.arc(Quinque.FINISHER) == 0.0,
            "Dawn's first blow, the wide sweep, takes the others in front within its length");
        for (Quinque q : Quinque.values()) {
            if (q != Quinque.DAWN) {
                for (int step = 0; step < 3; step++) {
                    check(q.arc(step) == 0.0, q + " has no arc of its own on blow " + step);
                }
            }
        }
        check(Quinque.DAWN.sweep(Quinque.FINISHER) == 4.5 && Quinque.DAWN.sweep(0) == 0.0, "Dawn's thrust reaches its whole length");
        check(Quinque.ARC_SHARE > 0.0F && Quinque.ARC_SHARE < 1.0F, "the arc deals a share of the blow");
        check(com.hex.kagune.control.quinque.DawnArc.along(0.0) == 0.0 && com.hex.kagune.control.quinque.DawnArc.along(1.0) == 1.0
            && Math.abs(com.hex.kagune.control.quinque.DawnArc.along(0.49) - 0.5) < 0.01, "the ribbon is laid from the windup to the follow through");
        double[] a = com.hex.kagune.control.quinque.DawnArc.point(0, 0.0);
        double[] b = com.hex.kagune.control.quinque.DawnArc.point(0, 1.0);
        double[] mid = com.hex.kagune.control.quinque.DawnArc.point(0, 0.5);
        check(a[0] > 1.5 && b[0] < -1.5 && Math.abs(mid[0]) < 0.01 && mid[2] > 2.5 && a[1] == b[1] && a[1] > 1.0 && a[1] < 1.6,
            "the sweep: level at chest height, from out on the right round the front to the left");
        for (double u = 0.0; u <= 1.0; u += 0.1) {
            double[] p = com.hex.kagune.control.quinque.DawnArc.point(0, u);
            check(Math.hypot(p[0], p[2]) <= Quinque.DAWN.arc(0), "the sweep's ribbon stays within the arc it hits: " + u);
        }
        double[] low = com.hex.kagune.control.quinque.DawnArc.point(1, 0.0);
        double[] high = com.hex.kagune.control.quinque.DawnArc.point(1, 1.0);
        check(low[0] < 0.0 && high[0] > 0.0 && high[1] > low[1] + 1.5, "the second: rising from low on the left to high on the right");
        double[] in = com.hex.kagune.control.quinque.DawnArc.point(2, 0.0);
        double[] out = com.hex.kagune.control.quinque.DawnArc.point(2, 1.0);
        check(in[0] == out[0] && in[1] == out[1] && out[2] > in[2] + 3.0 && out[2] <= Quinque.DAWN.sweep(Quinque.FINISHER),
            "the thrust: straight out along its line, no further than it reaches");
    }

    /** 2.0.32: Dawn's lightning in a player's hand: a right click fires it, its point open while the ball gathers and just after. */
    static void dawnFire() throws IOException {
        check(Quinque.DAWN.firesOnUse() && Quinque.LAUNCHER.firesOnUse() && !Quinque.KATANA.firesOnUse() && !Quinque.SPEAR.firesOnUse(),
            "Dawn and the launcher fire on right click (Shift folds them); the rest fold on it");
        long at = 1000L;
        long fires = at + Quinque.DAWN_CHARGE;
        check(!Quinque.tipOpen(at - 1, at, fires) && Quinque.tipOpen(at, at, fires) && Quinque.tipOpen(fires, at, fires)
            && Quinque.tipOpen(fires + Quinque.DAWN_OPEN_AFTER, at, fires) && !Quinque.tipOpen(fires + Quinque.DAWN_OPEN_AFTER + 1, at, fires),
            "its point opens as the ball gathers and closes a moment after it leaves");
        check(!Quinque.tipOpen(at, Long.MIN_VALUE, fires) && !Quinque.tipOpen(at, at, Long.MIN_VALUE) && !Quinque.tipOpen(at + 2, at + 5, at),
            "never open without both times, or with them the wrong way round");
        check(Quinque.dawnReady(-1L) && !Quinque.dawnReady(Quinque.DAWN_COOLDOWN - 1) && Quinque.dawnReady(Quinque.DAWN_COOLDOWN),
            "another ball only after it rests");
        check(Quinque.DAWN_PLAYER_SHOT > 0.0F && Quinque.DAWN_PLAYER_SHOT < com.hex.kagune.control.npc.AgentRules.DAWN_SHOT_DAMAGE,
            "a player's ball lighter than the White Reaper's");
        check(Quinque.DAWN_FIRE_FRAME > 0.95F && Quinque.DAWN_FIRE_FRAME <= 1.0F, "its fire frame above the full frame, inside the clamped property");
        String weapon = read("assets/hexkagune/models/item/quinque_dawn.json");
        String shut = read("assets/hexkagune/models/item/quinque_case_dawn.json");
        check(weapon != null && weapon.replaceAll("\\s", "").endsWith("{\"predicate\":{\"hexkagune:deploy\":" + Quinque.DAWN_FIRE_FRAME
            + "},\"model\":\"hexkagune:item/quinque/dawn_fire\"}]}"), "the weapon's last override is its fire frame");
        check(shut != null && !shut.contains("dawn_fire"), "the case never shows the fire frame (a case starting to shut reads 1.0)");
        check(read("assets/hexkagune/models/item/quinque/dawn_fire.json") != null, "the fire frame's model is in place");
    }

    static void turning() {
        long now = 1_000_000L;
        check(Quinque.progress(now, Long.MIN_VALUE) == 1.0F && !Quinque.moving(now, Long.MIN_VALUE), "never turned: still (no overflow)");
        check(Quinque.progress(now, now) == 0.0F && Quinque.moving(now, now), "just turned: it starts moving");
        check(Quinque.progress(now, now - Quinque.UNFOLD / 2) > 0.4F && Quinque.progress(now, now - Quinque.UNFOLD / 2) < 0.6F, "half way, half out");
        check(Quinque.progress(now, now - Quinque.UNFOLD) == 1.0F && !Quinque.moving(now, now - Quinque.UNFOLD), "done after its unfold");
        check(Quinque.progress(now, now + 40L) == 0.0F, "a time ahead of the clock (a lagging client) is the start, never negative");
        check(Quinque.progress(Long.MAX_VALUE, Long.MIN_VALUE + 1) == 1.0F && Quinque.progress(Long.MIN_VALUE + 5, Long.MAX_VALUE) == 0.0F,
            "the far ends of time don't overflow");
        float last = -1.0F;
        boolean rising = true;
        for (int t = 0; t <= Quinque.UNFOLD; t++) {
            float p = Quinque.shown(true, now + t, now);
            rising &= p >= last && p >= 0.0F && p <= 1.0F;
            last = p;
            check(Math.abs(Quinque.shown(false, now + t, now) - (1.0F - p)) < 1.0E-6F, "a case shutting runs its frames backwards (tick " + t + ")");
        }
        check(rising && last == 1.0F, "a weapon opening runs its frames forwards to all the way out");
        check(Quinque.shown(true, now, Long.MIN_VALUE) == 1.0F && Quinque.shown(false, now, Long.MIN_VALUE) == 0.0F,
            "still: a weapon out, a case shut");
    }

    static void kinds() {
        check(Math.abs(Quinque.BLADE.attackDamage() - 8.0F) < 1.0E-4F && Math.abs(Quinque.BLADE.attacksPerSecond() - 1.6F) < 1.0E-4F,
            "the Bikaku blade: 8 damage, 1.6 swings a second (a netherite sword's)");
        check(Math.abs(Quinque.SPEAR.attackDamage() - 8.0F) < 1.0E-4F && Quinque.SPEAR.attacksPerSecond() > Quinque.BLADE.attacksPerSecond(),
            "1.3.26: the Koukaku spear is agile: 8 damage, the quickest swing");
        check(Quinque.SPEAR.reach >= 2.0 && Quinque.BLADE.reach == 0.0 && Quinque.SPEAR.reach > Quinque.KATANA.reach, "the spear reaches furthest");
        check(Quinque.MACE.attackDamage() > Quinque.KATANA.attackDamage() && Quinque.MACE.attacksPerSecond() < 1.0F,
            "the mace is the heaviest and slowest");
        check(Quinque.SPEAR.twoHanded && Quinque.KATANA.twoHanded && Quinque.MACE.twoHanded && !Quinque.BLADE.twoHanded,
            "the spear, katana and mace are held in both hands");
        check(Quinque.LAUNCHER.ranged && !Quinque.KATANA.ranged, "only the launcher fires");
        // 1.3.26: a string of three M1s, the third the finisher; a pause starts it again.
        check(Quinque.next(-1, 0L) == 0 && Quinque.next(0, 10L) == 1 && Quinque.next(1, 10L) == 2 && Quinque.next(2, 10L) == 0
            && Quinque.next(1, Quinque.COMBO + 1) == 0 && Quinque.next(0, Long.MAX_VALUE) == 0, "three M1s to a string, a pause starts again");
        // 4.1.17 (the owner: "We need to improve every single M1 of every single weapon"): each its own string, the last its finisher.
        check(Quinque.KATANA.steps() == 5 && Quinque.KATANA.finisher() == 4, "the katana: five to its string, the fifth the finisher");
        for (Quinque q : Quinque.values()) {
            int step = -1;
            for (int i = 0; i < q.steps() * 2; i++) {
                int next = Quinque.next(q, step, 10L);
                check(next == (i % q.steps()), q + ": its string goes round in order (" + i + ")");
                step = next;
            }
            check(Quinque.next(q, q.steps() - 2, Quinque.COMBO + 1) == 0, q + ": a pause starts the string again");
            for (int s = 0; s < q.steps(); s++) {
                check(s == q.finisher() ? q.ranged || q.weight(s) > 1.0F : q.weight(s) == 1.0F, q + ": only its finisher is heavier (" + s + ")");
                check(q.swing(s) >= 4 && q.swing(s) <= 20, q + ": a sane swing length (" + s + ")");
            }
        }
        check(Quinque.KATANA.sweep(Quinque.KATANA.finisher()) > 0.0 && Quinque.MACE.sweep(Quinque.MACE.finisher()) > 0.0
            && Quinque.SPEAR.sweep(Quinque.SPEAR.finisher()) > 0.0 && Quinque.BLADE.sweep(Quinque.BLADE.finisher()) == 0.0
            && Quinque.KATANA.sweep(0) == 0.0 && Quinque.KATANA.sweep(2) == 0.0, "the katana's, mace's and spear's finishers reach others");
        int heat = 0;
        int shots = 0;
        while (heat < Quinque.HEAT_MOST && shots < 1000) {
            heat = Quinque.heated(heat, Quinque.SHOT_EVERY);
            shots++;
        }
        double seconds = shots * Quinque.SHOT_EVERY / 20.0;
        check(seconds >= 4.5 && seconds <= 8.0, "held down (1.3.33: a shard every two ticks) it overheats after about six seconds of fire: " + seconds);
        check(Quinque.SHOT_EVERY == 2 && 20.0 / Quinque.SHOT_EVERY * Quinque.SHARD_DAMAGE >= 17.5, "ten a second, no weaker than five of the old");
        heat = 0;
        for (int burst = 0; burst < 20; burst++) {
            for (int shot = 0; shot < 8; shot++) {
                heat = Quinque.heated(heat, Quinque.SHOT_EVERY);
            }
            heat = Quinque.heated(heat, 60L) - 1; // a breath between bursts
        }
        check(heat < Quinque.HEAT_MOST, "fired in bursts it never overheats");
        check(Quinque.heated(29, Long.MAX_VALUE) == 1, "long left it is cold (no overflow)");
        for (Quinque q : Quinque.values()) {
            check(q.ghoul > 1.0F && q.suppress > 0, q + " cuts a ghoul deeper and holds its regeneration back");
            check(Quinque.byId(q.caseId) == q && Quinque.byId("hexkagune:" + q.weaponId) == q, q + " is found by its case's and weapon's ids");
        }
        check(Quinque.byId("hexkagune:ghoul_meat") == null && Quinque.byId(null) == null, "nothing else is a quinque");
    }

    static String read(String path) throws IOException {
        Path p = RESOURCES.resolve(path);
        return Files.exists(p) ? Files.readString(p) : null;
    }

    static void files() throws IOException {
        String lang = read("assets/hexkagune/lang/en_us.json");
        Pattern override = Pattern.compile("\"hexkagune:" + Quinque.PROPERTY + "\"\\s*:\\s*([0-9.]+)\\s*}\\s*,\\s*\"model\"\\s*:\\s*\"hexkagune:([a-z0-9_/]+)\"");
        for (Quinque q : Quinque.values()) {
            for (String item : new String[] {q.caseId, q.weaponId}) {
                String json = read("assets/hexkagune/models/item/" + item + ".json");
                check(json != null, item + " has an item model");
                if (json == null) {
                    continue;
                }
                Matcher m = override.matcher(json);
                double previous = -1.0;
                int frames = 0;
                while (m.find()) {
                    double at = Double.parseDouble(m.group(1));
                    check(at > previous, item + ": its frames are listed in order (" + at + ")");
                    previous = at;
                    String frame = read("assets/hexkagune/models/" + m.group(2) + ".json");
                    check(frame != null, item + ": its frame " + m.group(2) + " exists");
                    if (frame != null) {
                        Matcher texture = Pattern.compile("\"q\"\\s*:\\s*\"hexkagune:([a-z_/]+)\"").matcher(frame);
                        check(texture.find() && Files.exists(RESOURCES.resolve("assets/hexkagune/textures/" + texture.group(1) + ".png")),
                            item + ": its frame " + m.group(2) + "'s texture exists");
                    }
                    frames++;
                }
                // 1.3.28: a case also has an investigator's two carried frames (ahead of the opening ones); a weapon never does.
                // 2.0.32: Dawn's weapon has its fire frame last too. 4.1.17: eight opening frames (the case open, six on the way out,
                // all the way out), so the weapon slides out of its case rather than jumping a third at a time.
                int expected = item.equals(q.caseId) ? 10 : q == Quinque.DAWN ? 9 : 8;
                check(frames == expected && previous > 0.9 && previous <= 1.0, item + ": " + expected + " frames past its shut case, the last all the way out");
                check(json.contains("case_carried") == item.equals(q.caseId), item + ": the carried frames on the case alone");
                Matcher parent = Pattern.compile("\"parent\"\\s*:\\s*\"hexkagune:([a-z_/]+)\"").matcher(json);
                check(parent.find() && read("assets/hexkagune/models/" + parent.group(1) + ".json") != null, item + ": its shut case model exists");
                check(lang != null && lang.contains("\"item.hexkagune." + item + "\""), item + " has a name");
            }
            check(lang != null && lang.contains("item.hexkagune.quinque." + q.model + ".lore"), q + " has its lore line");
        }
        check(lang != null && lang.contains("item.hexkagune.quinque.open") && lang.contains("item.hexkagune.quinque.close")
            && lang.contains("item.hexkagune.quinque.ghoul"), "the quinques' use lines are named");
        // 1.3.28: the case's own sounds (tools/case_sfx.py), two takes each, played as it opens and shuts, with subtitles.
        String sounds = read("assets/hexkagune/sounds.json");
        String server = Files.readString(Path.of("src/main/java/com/hex/kagune/control/server/QuinqueServer.java"));
        for (String event : new String[] {"case_open", "case_close"}) {
            check(sounds != null && sounds.contains("\"sfx." + event + "\"") && sounds.contains("hexkagune:sfx/quinque/" + event + "_1")
                && sounds.contains("hexkagune:sfx/quinque/" + event + "_2"), "the case's " + event + " sound has its two takes");
            check(server.contains("\"hexkagune:sfx." + event + "\""), "the case plays its " + event + " sound");
            check(lang != null && lang.contains("\"subtitles.hexkagune.sfx." + event + "\""), "the case's " + event + " sound has a subtitle");
        }
        check(!server.contains("centipede_click"), "the case's latch is its own now, not a centipede's click");
        // 1.3.27: the owner: "a suitcase you can't [craft], definitely": no recipe makes any of them.
        Path recipes = RESOURCES.resolve("data/hexkagune/recipes");
        if (Files.isDirectory(recipes)) {
            // 1.3.40: every recipe, the kitchen's folder of them included.
            try (java.util.stream.Stream<Path> all = Files.walk(recipes).filter(Files::isRegularFile)) {
                for (Path recipe : (Iterable<Path>)all::iterator) {
                    String text = Files.readString(recipe);
                    for (Quinque q : Quinque.values()) {
                        check(!text.contains("hexkagune:" + q.caseId + "\"") && !text.contains("hexkagune:" + q.weaponId + "\""),
                            recipe.getFileName() + " never makes a quinque (" + q + ")");
                    }
                }
            }
        }
        check(lang != null && lang.contains("entity.hexkagune.ccg_investigator"), "the investigator has a name");
    }

    // ---------------------------------------------------------------- 1.3.33: how each is held and swung

    /** The model file's third person transform (rotation, translation, scale) for {@code frame}. */
    static double[] third(String frame) throws IOException {
        String json = read("assets/hexkagune/models/item/quinque/" + frame + ".json");
        Matcher m = Pattern.compile("\"thirdperson_righthand\"\\s*:\\s*\\{\\s*\"rotation\"\\s*:\\s*\\[([^\\]]*)\\]\\s*,"
            + "\\s*\"translation\"\\s*:\\s*\\[([^\\]]*)\\]\\s*,\\s*\"scale\"\\s*:\\s*\\[([^\\]]*)\\]").matcher(json == null ? "" : json);
        if (!m.find()) {
            return null;
        }
        double[] out = new double[7];
        int i = 0;
        for (int g = 1; g <= 3; g++) {
            String[] parts = m.group(g).split(",");
            for (int k = 0; k < (g == 3 ? 1 : 3); k++) {
                out[i++] = Double.parseDouble(parts[k].trim());
            }
        }
        return out;
    }

    /** How far {@code p} is from the segment {@code a}..{@code b}. */
    static double toSegment(double[] p, double[] a, double[] b) {
        double[] ab = {b[0] - a[0], b[1] - a[1], b[2] - a[2]};
        double len2 = ab[0] * ab[0] + ab[1] * ab[1] + ab[2] * ab[2];
        double t = len2 < 1.0E-9 ? 0.0 : Math.max(0.0, Math.min(1.0, ((p[0] - a[0]) * ab[0] + (p[1] - a[1]) * ab[1] + (p[2] - a[2]) * ab[2]) / len2));
        return distance(p, new double[] {a[0] + ab[0] * t, a[1] + ab[1] * t, a[2] + ab[2] * t});
    }

    static double distance(double[] a, double[] b) {
        return Math.sqrt((a[0] - b[0]) * (a[0] - b[0]) + (a[1] - b[1]) * (a[1] - b[1]) + (a[2] - b[2]) * (a[2] - b[2]));
    }

    /**
     * Each quinque's hold (QuinqueHolds, solved by tools/quinque_models.py) is the one its model is drawn with; its right grip sits in the
     * fist; a two-handed one's left hand is on its grip in its stance and stays near it through every swing (QuinqueGrip.reach); the
     * launcher's barrel lies along the look; the blade comes out of the fist, never along the arm; melee quinques fold at a tap.
     */
    static void holds() throws IOException {
        for (Quinque q : Quinque.values()) {
            com.hex.kagune.control.client.animation.QuinqueHolds.Hold h = com.hex.kagune.control.client.animation.QuinqueHolds.of(q);
            check(h != null, q + " has a hold");
            if (h == null) {
                continue;
            }
            for (String frame : new String[] {"full", "out_1", "out_2", "out_3", "out_4", "out_5", "out_6"}) { // 4.1.17: seven frames out
                double[] d = third(q.model + "_" + frame);
                boolean same = d != null;
                for (int i = 0; same && i < 3; i++) {
                    same = Math.abs(d[i] - h.rotation()[i]) < 1.0E-3 && Math.abs(d[3 + i] - h.translation()[i]) < 1.0E-3;
                }
                check(same && Math.abs(d[6] - h.scale()) < 1.0E-3, q + ": its " + frame + " frame is drawn with the hold's transform");
            }
            com.hex.kagune.control.client.animation.RigPose stance = com.hex.kagune.control.client.animation.QuinquePose.hold(q);
            for (boolean slim : new boolean[] {false, true}) {
                double[] fist = com.hex.kagune.control.client.animation.QuinqueGrip.fist(stance, true, slim);
                double[] grip = com.hex.kagune.control.client.animation.QuinqueGrip.held(stance, h, slim, h.rightGrip());
                check(distance(fist, grip) < 0.5, q + ": the right grip in the middle of the fist (" + distance(fist, grip) + " px, slim " + slim + ")");
            }
            if (q.twoHanded) {
                com.hex.kagune.control.client.animation.RigPose p = com.hex.kagune.control.client.animation.QuinquePose.hold(q);
                float miss = com.hex.kagune.control.client.animation.QuinqueGrip.reach(p, q, false);
                double[] left = com.hex.kagune.control.client.animation.QuinqueGrip.fist(p, false, false);
                double[] a = com.hex.kagune.control.client.animation.QuinqueGrip.held(p, h, false, h.slideFrom());
                double[] b = com.hex.kagune.control.client.animation.QuinqueGrip.held(p, h, false, h.slideTo());
                double off = toSegment(left, a, b);
                check(Math.abs(miss) < 0.6F && off < 0.8, q + ": in its stance the left hand is on its grip (" + off + " px)");
                float worst = 0.0F;
                for (int step = 0; step < q.steps(); step++) {
                    for (double t = 0.0; t <= 1.0; t += 0.05) {
                        com.hex.kagune.control.client.animation.RigPose swing = com.hex.kagune.control.client.animation.QuinquePose.swing(q, step, t);
                        worst = Math.max(worst, Math.abs(com.hex.kagune.control.client.animation.QuinqueGrip.reach(swing, q, false)));
                    }
                }
                check(worst < 3.0F, q + ": through every swing the left hand stays within a fist's half of its grip (" + worst + " px)");
            }
            // Every swing leaves the stance and comes back to it.
            for (int step = 0; step < 3; step++) {
                com.hex.kagune.control.client.animation.RigPose a = com.hex.kagune.control.client.animation.QuinquePose.swing(q, step, 0.0);
                com.hex.kagune.control.client.animation.RigPose m = com.hex.kagune.control.client.animation.QuinquePose.swing(q, step, 0.5);
                com.hex.kagune.control.client.animation.RigPose b = com.hex.kagune.control.client.animation.QuinquePose.swing(q, step, 1.0);
                double moved = distance(com.hex.kagune.control.client.animation.QuinqueGrip.fist(m, true, false),
                    com.hex.kagune.control.client.animation.QuinqueGrip.fist(stance, true, false)) + Math.abs(m.twist - stance.twist) * 8.0 + Math.abs(m.lunge);
                check(moved > 2.0, q + ": swing " + (step + 1) + " moves");
                check(distance(com.hex.kagune.control.client.animation.QuinqueGrip.fist(a, true, false), com.hex.kagune.control.client.animation.QuinqueGrip.fist(stance, true, false)) < 0.05
                    && distance(com.hex.kagune.control.client.animation.QuinqueGrip.fist(b, true, false), com.hex.kagune.control.client.animation.QuinqueGrip.fist(stance, true, false)) < 0.05,
                    q + ": swing " + (step + 1) + " starts and ends in the stance");
            }
        }
        // The launcher: a rifle. Its barrel along the look (the chest's turn taken out), level, the muzzle well out in front.
        com.hex.kagune.control.client.animation.QuinqueHolds.Hold gun = com.hex.kagune.control.client.animation.QuinqueHolds.of(Quinque.LAUNCHER);
        com.hex.kagune.control.client.animation.RigPose aim = com.hex.kagune.control.client.animation.QuinquePose.hold(Quinque.LAUNCHER);
        aim.spin = 0.0F; // (the whole body's share of the turn: undone here so the look is the model's -z)
        aim.twist = gun.twist();
        aim.headY = 0.0F;
        double[] breech = com.hex.kagune.control.client.animation.QuinqueGrip.held(aim, gun, false, new float[] {1.5F, 9.8F, 8.0F});
        double[] muzzle = com.hex.kagune.control.client.animation.QuinqueGrip.held(aim, gun, false, new float[] {31.0F, 9.8F, 8.0F});
        double length = distance(breech, muzzle);
        check(length > 15.0 && Math.abs(muzzle[0] - breech[0]) < 0.12 * length && Math.abs(muzzle[1] - breech[1]) < 0.12 * length
            && muzzle[2] < breech[2] - 0.9 * length, "the launcher's barrel lies along the look, level, at the target");
        double[] stock = com.hex.kagune.control.client.animation.QuinqueGrip.held(aim, gun, false, new float[] {-6.0F, 9.0F, 8.0F});
        check(stock[1] > -2.0 && stock[1] < 6.0, "its stock at the height of the shoulder (" + stock[1] + ")");
        // The blade: its point out of the fist, never back along the arm (the owner: "who grabs a sword by the tip of it").
        com.hex.kagune.control.client.animation.QuinqueHolds.Hold blade = com.hex.kagune.control.client.animation.QuinqueHolds.of(Quinque.BLADE);
        for (int step = 0; step < 3; step++) {
            for (double t = 0.0; t <= 1.0; t += 0.1) {
                com.hex.kagune.control.client.animation.RigPose p = com.hex.kagune.control.client.animation.QuinquePose.swing(Quinque.BLADE, step, t);
                double[] fist = com.hex.kagune.control.client.animation.QuinqueGrip.fist(p, true, false);
                double[] tip = com.hex.kagune.control.client.animation.QuinqueGrip.held(p, blade, false, new float[] {(float)(8 + 23 * Math.sqrt(0.5)), (float)(8 + 23 * Math.sqrt(0.5)), 8F});
                float[] turn = p.transform("rightArm", com.hex.kagune.control.client.animation.RigPose.ROTATION, 0, 0, 0);
                double cx = Math.cos(turn[0]), sx = Math.sin(turn[0]), cy = Math.cos(turn[1]), sy = Math.sin(turn[1]), cz = Math.cos(turn[2]), sz = Math.sin(turn[2]);
                // The arm's own length (Rz Ry Rx of +y).
                double[] along = {cz * sy * sx - sz * cx, sz * sy * sx + cz * cx, cy * sx};
                double[] out = {tip[0] - fist[0], tip[1] - fist[1], tip[2] - fist[2]};
                double n = Math.sqrt(out[0] * out[0] + out[1] * out[1] + out[2] * out[2]);
                double cos = (out[0] * along[0] + out[1] * along[1] + out[2] * along[2]) / n;
                check(cos > Math.cos(Math.toRadians(75.0)) && cos < Math.cos(Math.toRadians(25.0)),
                    "the blade leaves the fist 25..75 degrees off the forearm's line (swing " + (step + 1) + " at " + t + ")");
            }
        }
        // A tap folds a melee quinque; the launcher (2.0.32: and Dawn) fires on it and folds on Shift.
        String server = Files.readString(Path.of("src/main/java/com/hex/kagune/control/server/QuinqueServer.java"));
        check(server.contains("boolean folds = open != null && (!open.firesOnUse() || player.m_6047_())") && server.contains("tapped("),
            "a tap of right click folds a melee quinque (held down, it never flips back)");
    }
}
