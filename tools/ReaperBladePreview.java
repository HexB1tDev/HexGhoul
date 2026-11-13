import com.hex.kagune.control.client.animation.ReaperBladePose;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.npc.AgentRules;
import com.hex.kagune.control.quinque.ArmMesh;
import com.hex.kagune.control.quinque.ReaperBladeMesh;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * 4.1.37: the White Reaper's second phase off-game, as {@code client.render.ReaperBladeLayer} draws it: his bare look (wide arms, stiff:
 * a mob has no elbows), his chest, the blade in his right fist turned by the pose's wrist, the golden case while he draws. Sheets:
 * {@code guard} (standing and running, from every side), {@code draw} (the case to the guard, frame by frame), {@code string} (his blows).
 *
 * <pre>
 * javac -nowarn -cp build/quick/classes:build/quick/base.jar -d build/reaper-preview tools/PoseRender.java tools/ReaperPreview.java tools/ReaperBladePreview.java
 * java -Djava.awt.headless=true -cp build/reaper-preview:build/quick/classes:build/quick/base.jar:src/main/resources ReaperBladePreview &lt;bare skin.png&gt; &lt;out.png&gt; guard|draw|string|shed|case|dress|end
 * java ... ReaperBladePreview &lt;bare skin.png&gt; &lt;frames dir&gt; shedgif|endgif   (then: python3 tools/frames_gif.py &lt;frames dir&gt; &lt;out.gif&gt;)
 * </pre>
 */
public final class ReaperBladePreview {
    static final int W = 300;
    static final int H = 340;

    public static void main(String[] args) throws Exception {
        PoseRender.skin = ImageIO.read(new File(args[0]));
        PoseRender.slim = false;
        PoseRender.stiff = true;
        PoseRender.armOut = com.hex.kagune.control.npc.ReaperBuild.ARM_OUT;
        File out = new File(args[1]);
        String mode = args.length > 2 ? args[2] : "guard";
        List<BufferedImage> frames = new ArrayList<>();
        int cols = 4;
        switch (mode) {
            case "guard" -> {
                double[] yaws = {0, 40, 90, -90, -40, 160};
                for (double yaw : yaws) {
                    frames.add(frame(ReaperBladePose.guard(0.0F), Double.NaN, yaw, 8, "guard, yaw " + (int)yaw));
                }
                frames.add(frame(ReaperBladePose.guard(1.0F), Double.NaN, 40, 8, "on his run, three-quarter"));
                frames.add(frame(ReaperBladePose.guard(1.0F), Double.NaN, -90, 4, "on his run, his right"));
            }
            case "draw" -> {
                cols = 6;
                for (double t : new double[] {0, 6, 11, 16, 22, 26, 30, 33, 36, 39, 42, 45, 48, 51, 54, 58}) {
                    frames.add(frame(ReaperBladePose.draw(t), t, 35, 8, "draw, tick " + (int)t));
                }
                for (double t : new double[] {30, 39, 45, 58}) {
                    frames.add(frame(ReaperBladePose.draw(t), t, -80, 4, "his right, tick " + (int)t));
                }
            }
            case "string" -> {
                cols = 6;
                for (int step = 0; step < 4; step++) {
                    for (double t : new double[] {2, 5, 8, 10, 13, 16}) {
                        frames.add(frame(ReaperBladePose.strike(step, t), Double.NaN, 30, 8, "blow " + (step + 1) + ", tick " + (int)t));
                    }
                }
            }
            case "shed" -> {
                cols = 6;
                bare = PoseRender.skin;
                coatSkin = ImageIO.read(new File(new File(args[0]).getParentFile(), "ccg_reaper.png"));
                coatTexture = ImageIO.read(new File(new File(args[0]).getParentFile(), "reaper_coat.png"));
                // 4.1.74: labelled in the world's seconds (the keys played AgentRules.REAPER_CHANGE_SLOW times slower).
                for (double t : new double[] {0, 4, 8, 12, 15, 18, 21, 24, 26, 28, 30, 33, 37, 46}) {
                    frames.add(shed(t, 30, 8, "jacket, " + seconds(t)));
                }
                for (double t : new double[] {6, 11, 16, 22, 28, 34, 38, 42, 46, 50, 54, 58}) {
                    frames.add(shed(AgentRules.REAPER_SHED_TICKS + t, 30, 8, "then the case, " + seconds(AgentRules.REAPER_SHED_TICKS + t)));
                }
                frames.add(shed(AgentRules.REAPER_CHANGE_TICKS - 2, 30, 8, "ready, " + seconds(AgentRules.REAPER_CHANGE_TICKS - 2)));
                for (double t : new double[] {15, 21, 26, 30, 33, 46}) {
                    frames.add(shed(t, 150, 12, "behind, " + seconds(t)));
                }
                for (double t : new double[] {18, 24, 28, 37}) {
                    frames.add(shed(t, 90, 6, "his left, " + seconds(t)));
                }
            }
            case "end" -> {
                // 4.1.74: his whole end (ReaperBladePose.end; the blade cut at ReaperBladeRules.level, its shards from their own breaksAt).
                cols = 6;
                for (double t : new double[] {0, 6, 12, 19, 21, 26, 32, 38, 44, 50, 56, 62, 68, 74, 79, 82, 86, 92, 98, 104, 110, 130, 150, 169}) {
                    frames.add(end(t, 30, 8, endLabel(t)));
                }
                for (double t : new double[] {19, 40, 64, 82, 98, 169}) {
                    frames.add(end(t, -80, 6, "his right, " + endLabel(t)));
                }
            }
            case "shedgif", "endgif" -> {
                // 4.1.74: one frame per world tick (20 a second: real speed), front three-quarter beside his right side, written to the
                // directory args[1] as frame-NNN.png (tools/frames_gif.py makes the GIF).
                bare = PoseRender.skin;
                coatSkin = ImageIO.read(new File(new File(args[0]).getParentFile(), "ccg_reaper.png"));
                coatTexture = ImageIO.read(new File(new File(args[0]).getParentFile(), "reaper_coat.png"));
                boolean change = mode.equals("shedgif");
                int last = change ? AgentRules.REAPER_CHANGE_REAL + 12 : AgentRules.REAPER_END_TICKS + 6;
                out.mkdirs();
                for (int real = -6; real <= last; real++) {
                    BufferedImage a;
                    BufferedImage b;
                    if (change) {
                        double age = Math.max(0.0, Math.min(AgentRules.changeAge(real), AgentRules.REAPER_CHANGE_TICKS - 0.01));
                        String text = String.format("%.2f s", real / 20.0);
                        a = shed(age, 30, 8, text);
                        b = shed(age, -80, 6, text);
                    } else {
                        double age = Math.max(0.0, Math.min(real, AgentRules.REAPER_END_TICKS));
                        String text = String.format("%.2f s", real / 20.0) + (real >= AgentRules.REAPER_END_TICKS ? " (dies)" : "");
                        a = end(age, 30, 8, text);
                        b = end(age, -80, 6, text);
                    }
                    BufferedImage pair = new BufferedImage(2 * W, H, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = pair.createGraphics();
                    g.drawImage(a, 0, 0, null);
                    g.drawImage(b, W, 0, null);
                    g.dispose();
                    ImageIO.write(pair, "png", new File(out, String.format("frame-%03d.png", real + 6)));
                }
                System.out.println("wrote " + out);
                return;
            }
            case "case" -> {
                // Every tick of the case in his hand, two sides: to judge its turn frame by frame.
                cols = 8;
                double yaw = args.length > 3 ? Double.parseDouble(args[3]) : 35.0;
                for (double t = AgentRules.REAPER_CASE_FROM; t <= AgentRules.REAPER_CASE_DROPS; t += 1.0) {
                    frames.add(frame(ReaperBladePose.draw(t), t, yaw, 8, "case, tick " + (int)t));
                }
            }
            case "dress" -> {
                cols = 6;
                bare = PoseRender.skin;
                coatSkin = ImageIO.read(new File(new File(args[0]).getParentFile(), "ccg_reaper.png"));
                coatTexture = ImageIO.read(new File(new File(args[0]).getParentFile(), "reaper_coat.png"));
                for (double t : new double[] {0, 3, 6, 9, 12, 14, 16, 18, 19, 21, 23, 26, 28, 30, 33, 36, 39, 42, 44, 47, 50, 54, 58, 62, 66, 70}) {
                    frames.add(dress(t, 30, 8, "dress, tick " + (int)t));
                }
                for (double t : new double[] {9, 16, 23, 47, 54, 62}) {
                    frames.add(dress(t, 150, 10, "behind, tick " + (int)t));
                }
            }
            default -> throw new IllegalArgumentException(mode);
        }
        sheet(out, frames, cols);
    }

    static java.awt.image.BufferedImage bare;
    static java.awt.image.BufferedImage coatSkin;
    static java.awt.image.BufferedImage coatTexture;

    /** 4.1.74: key tick {@code t} of his change in the world's seconds (and the key's tick). */
    static String seconds(double t) {
        return String.format("%.2f s (key %d)", t * AgentRules.REAPER_CHANGE_SLOW / 20.0, (int)t);
    }

    /** 4.1.74: tick {@code t} of his end, in seconds, with what is happening. */
    static String endLabel(double t) {
        String what = t < AgentRules.REAPER_END_SHATTER ? "lowering" : t < AgentRules.REAPER_END_SHATTERED ? "breaking"
            : t < AgentRules.REAPER_END_KNEELS ? "slack" : t < AgentRules.REAPER_END_KNEELS + AgentRules.REAPER_END_KNEELING ? "kneeling" : "still";
        return String.format("%.2f s, %s", t / 20.0, what);
    }

    /**
     * 4.1.74: {@code age} ticks into his end, as ReaperBladeLayer.ended draws it: his pose (ReaperBladePose.end), the blade cut at what is
     * left of it (ReaperBladeRules.level), each shard from the tick it broke off, flying out and falling to lie flat on the ground.
     */
    static BufferedImage end(double age, double yaw, double pitch, String text) {
        RigPose pose = ReaperBladePose.end(age);
        List<PoseRender.Tri> tris = new ArrayList<>(PoseRender.model(pose));
        ReaperPreview.chest(tris, pose, false);
        endShards(tris, pose, age);
        List<PoseRender.Tri> lit = new ArrayList<>();
        List<Boolean> glows = new ArrayList<>();
        double[] fist = {-1.0, 9.0, 0.0};
        double[][] inArm = ReaperBladePose.inArm(pose);
        add(lit, glows, ReaperBladeMesh.brokenAt(com.hex.kagune.control.npc.ReaperBladeRules.level(age)),
            v -> PoseRender.point(pose, "rightArm", plus(fist, apply(inArm, v))));
        PoseRender.Camera cam = PoseRender.orbit(yaw, pitch, 6.4, 1.0);
        PoseRender.Canvas canvas = new PoseRender.Canvas(W, H, 2, cam, 0x3A4450);
        canvas.floor(6, 0x58626C, 0x4E5862);
        for (PoseRender.Tri t : tris) {
            canvas.tri(t, true);
        }
        QuinqueMeshPreview.draw(canvas, lit, glows, cam.eye());
        BufferedImage img = canvas.image();
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        g.drawString(text, 6, 16);
        g.dispose();
        return img;
    }

    /** 4.1.74: the shards as ReaperBladeLayer.shards places them (the model's frame, moved with his body), both faces drawn. */
    static void endShards(List<PoseRender.Tri> tris, RigPose pose, double age) {
        double ground = 24.0 - pose.drop - 0.05;
        for (int i = 0; i < com.hex.kagune.control.npc.ReaperBladeRules.SHARDS; i++) {
            double dt = age - com.hex.kagune.control.npc.ReaperBladeRules.breaksAt(i);
            if (dt < 0.0) {
                continue;
            }
            double[][] s = ReaperBladePose.shard(i, dt);
            double fall = 0.5 * com.hex.kagune.control.npc.ReaperBladeRules.SHARD_FALL * dt * dt;
            double[] c = s[0];
            double[] l = s[1];
            double[] w = s[2];
            double low = c[1] + fall + Math.abs(l[1]) * 0.5 + Math.abs(w[1]) * 0.5;
            double y = c[1] + fall;
            if (low > ground) {
                y = ground - 0.1;
                l = new double[] {l[0], 0.0, l[2]};
                w = new double[] {w[0], 0.0, w[2]};
                if (Math.hypot(l[0], l[2]) < 1.0E-3) {
                    l = new double[] {0.6, 0.0, 0.0};
                }
                if (Math.hypot(w[0], w[2]) < 1.0E-3) {
                    w = new double[] {0.0, 0.0, 0.3};
                }
            }
            double[][] q = {{c[0] - l[0] * 0.5, y - l[1] * 0.5, c[2] - l[2] * 0.5}, {c[0] + w[0], y + w[1], c[2] + w[2]},
                {c[0] + l[0] * 0.5, y + l[1] * 0.5, c[2] + l[2] * 0.5}, {c[0] - w[0], y - w[1], c[2] - w[2]}};
            double[][] at = new double[4][];
            for (int k = 0; k < 4; k++) {
                at[k] = PoseRender.world(pose, q[k]);
            }
            int silver = i % 4 == 0 ? 0x9A0C16 : i % 3 == 0 ? 0xE8E9EB : 0xC9CBCE;
            double[] n = PoseRender.norm(PoseRender.cross(PoseRender.sub(at[1], at[0]), PoseRender.sub(at[3], at[0])));
            tris.add(new PoseRender.Tri(new double[][] {at[0], at[1], at[2]}, silver, n));
            tris.add(new PoseRender.Tri(new double[][] {at[0], at[2], at[3]}, silver, n));
            double[] back = PoseRender.scale(n, -1);
            tris.add(new PoseRender.Tri(new double[][] {at[2], at[1], at[0]}, silver, back));
            tris.add(new PoseRender.Tri(new double[][] {at[3], at[2], at[0]}, silver, back));
        }
    }

    /**
     * 4.1.37: {@code age} ticks into his change (the shed, then the draw): his look (the coat's, slim, before the swap; bare and wide after),
     * the coat itself as ReaperCoatLayer draws it ({@code ReaperCoatPose}), the blade once he draws it.
     */
    static BufferedImage shed(double age, double yaw, double pitch, String text) {
        boolean coatLook = age < AgentRules.REAPER_SHED_SWAP;
        PoseRender.skin = coatLook ? coatSkin : bare;
        PoseRender.slim = coatLook;
        RigPose pose = age < AgentRules.REAPER_SHED_TICKS ? com.hex.kagune.control.client.animation.AgentPose.shed(age)
            : ReaperBladePose.draw(age - AgentRules.REAPER_SHED_TICKS);
        List<PoseRender.Tri> tris = new ArrayList<>(PoseRender.model(pose));
        ReaperPreview.chest(tris, pose, coatLook);
        double ground = 24.0 - pose.drop;
        if (com.hex.kagune.control.client.animation.ReaperCoatPose.drawn(age)) {
            coat(tris, pose, age, coatLook, ground);
        }
        PoseRender.Camera cam = PoseRender.orbit(yaw, pitch, 6.4, 1.0);
        PoseRender.Canvas canvas = new PoseRender.Canvas(W, H, 2, cam, 0x3A4450);
        canvas.floor(6, 0x58626C, 0x4E5862);
        for (PoseRender.Tri t : tris) {
            canvas.tri(t, true);
        }
        if (age >= AgentRules.REAPER_SHED_TICKS) {
            // His draw after the jacket: the case from behind his back, opened, the blade out of it, the case dropped (as ReaperBladeLayer).
            List<PoseRender.Tri> lit = new ArrayList<>();
            List<Boolean> glows = new ArrayList<>();
            bladeAndCase(pose, age - AgentRules.REAPER_SHED_TICKS, lit, glows);
            QuinqueMeshPreview.draw(canvas, lit, glows, cam.eye());
        }
        BufferedImage img = canvas.image();
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        g.drawString(text, 6, 16);
        g.dispose();
        PoseRender.skin = bare;
        PoseRender.slim = false;
        return img;
    }

    /** 4.1.37: the coat's pieces ({@code age}: the tick of his taking it off they are drawn at), as ReaperCoatLayer draws them. */
    static void coat(List<PoseRender.Tri> tris, RigPose pose, double age, boolean coatLook, double ground) {
        com.hex.kagune.control.outfit.OutfitModels.Model model = com.hex.kagune.control.outfit.OutfitModels.get("formal");
            for (int piece = 0; piece < com.hex.kagune.control.client.animation.ReaperCoatPose.PIECES; piece++) {
                double[][][] frame = com.hex.kagune.control.client.animation.ReaperCoatPose.frame(piece, age, ground);
                int bone = com.hex.kagune.control.client.animation.ReaperCoatPose.BONE[piece];
                if (coatLook && (bone == 2 || bone == 3)) {
                    bone += 4; // the slim sleeves on his first look's arms
                }
                for (com.hex.kagune.control.outfit.OutfitModels.Cube cube : model.on(bone, com.hex.kagune.control.client.animation.ReaperCoatPose.SLOT[piece])) {
                    for (com.hex.kagune.control.outfit.OutfitModels.Face face : cube.faces()) {
                        double[][] w = new double[4][];
                        for (int k = 0; k < 4; k++) {
                            double[] q = com.hex.kagune.control.client.animation.ReaperCoatPose.place(frame,
                                new double[] {face.corners()[k * 3], face.corners()[k * 3 + 1], face.corners()[k * 3 + 2]});
                            q[1] = Math.min(q[1], ground - 0.02);
                            w[k] = PoseRender.world(pose, q);
                        }
                        float[] uv = face.uvs();
                        double u = (uv[0] + uv[2] + uv[4] + uv[6]) / 4.0;
                        double v = (uv[1] + uv[3] + uv[5] + uv[7]) / 4.0;
                        int argb = coatTexture.getRGB((int)Math.min(coatTexture.getWidth() - 1, u * coatTexture.getWidth()),
                            (int)Math.min(coatTexture.getHeight() - 1, v * coatTexture.getHeight()));
                        if ((argb >>> 24) < 16) {
                            continue;
                        }
                        double[] n = PoseRender.norm(PoseRender.cross(PoseRender.sub(w[1], w[0]), PoseRender.sub(w[3], w[0])));
                        double[] mid = PoseRender.scale(PoseRender.add(w[0], w[2]), 0.5);
                        double[] probe = PoseRender.world(pose, com.hex.kagune.control.client.animation.ReaperCoatPose.place(frame,
                            new double[] {face.corners()[0] + face.normal()[0], face.corners()[1] + face.normal()[1], face.corners()[2] + face.normal()[2]}));
                        if (PoseRender.dot(n, PoseRender.sub(probe, w[0])) < 0) {
                            n = PoseRender.scale(n, -1);
                        }
                        tris.add(new PoseRender.Tri(new double[][] {w[0], w[1], w[2]}, argb & 0xFFFFFF, n));
                        tris.add(new PoseRender.Tri(new double[][] {w[0], w[2], w[3]}, argb & 0xFFFFFF, n));
                    }
                }
            }
    }

    /**
     * 4.1.38: {@code t} ticks into putting his jacket back on: the blade back into the golden case and the case put away, then the coat
     * back on (his look his coat's again under it at the swap).
     */
    static BufferedImage dress(double t, double yaw, double pitch, String text) {
        boolean coatLook = t >= AgentRules.REAPER_DRESS_SWAP;
        PoseRender.skin = coatLook ? coatSkin : bare;
        PoseRender.slim = coatLook;
        RigPose pose = com.hex.kagune.control.client.animation.ReaperDressPose.pose(t);
        List<PoseRender.Tri> tris = new ArrayList<>(PoseRender.model(pose));
        ReaperPreview.chest(tris, pose, coatLook);
        double ground = 24.0 - pose.drop;
        double coatAge = com.hex.kagune.control.client.animation.ReaperDressPose.coatAge(t);
        if (coatAge >= 0.0) {
            coat(tris, pose, coatAge, coatLook, ground);
        }
        List<PoseRender.Tri> lit = new ArrayList<>();
        List<Boolean> glows = new ArrayList<>();
        double[] fist = {-1.0, 9.0, 0.0};
        com.hex.kagune.control.client.animation.ReaperDressPose.Held held = com.hex.kagune.control.client.animation.ReaperDressPose.held(pose, t);
        if (held != null) {
            for (int side : new int[] {1, -1}) {
                add(lit, glows, ReaperBladeMesh.goldHalf(side), v -> PoseRender.point(pose, "rightArm", plus(held.origin(), apply(held.turn(),
                    ReaperBladeMesh.opened(v, side, held.open())))));
            }
            if (held.handle()) {
                add(lit, glows, ReaperBladeMesh.goldHandle(), v -> PoseRender.point(pose, "rightArm", plus(held.origin(), apply(held.turn(), v))));
            }
        }
        float k = com.hex.kagune.control.client.animation.ReaperDressPose.bladeOut(t);
        if (k >= 0.0F) {
            double[][] inArm = ReaperBladePose.inArm(pose);
            add(lit, glows, ReaperBladeMesh.of(), v -> PoseRender.point(pose, "rightArm", plus(fist, apply(inArm,
                new double[] {k >= 1.0F ? v[0] : ReaperBladeMesh.slid(v[0], k), v[1], v[2]}))));
        }
        PoseRender.Camera cam = PoseRender.orbit(yaw, pitch, 6.4, 1.0);
        PoseRender.Canvas canvas = new PoseRender.Canvas(W, H, 2, cam, 0x3A4450);
        canvas.floor(6, 0x58626C, 0x4E5862);
        for (PoseRender.Tri tri : tris) {
            canvas.tri(tri, true);
        }
        QuinqueMeshPreview.draw(canvas, lit, glows, cam.eye());
        BufferedImage img = canvas.image();
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        g.drawString(text, 6, 16);
        g.dispose();
        PoseRender.skin = bare;
        PoseRender.slim = false;
        return img;
    }

    /** {@code pose} seen from {@code yaw} round him, {@code after} ticks into the draw (NaN: not drawing). */
    static BufferedImage frame(RigPose pose, double after, double yaw, double pitch, String text) {
        List<PoseRender.Tri> tris = new ArrayList<>(PoseRender.model(pose));
        ReaperPreview.chest(tris, pose, false);
        List<PoseRender.Tri> lit = new ArrayList<>();
        List<Boolean> glows = new ArrayList<>();
        bladeAndCase(pose, after, lit, glows);
        PoseRender.Camera cam = PoseRender.orbit(yaw, pitch, 6.4, 1.0);
        PoseRender.Canvas canvas = new PoseRender.Canvas(W, H, 2, cam, 0x3A4450);
        canvas.floor(6, 0x58626C, 0x4E5862);
        for (PoseRender.Tri t : tris) {
            canvas.tri(t, true);
        }
        QuinqueMeshPreview.draw(canvas, lit, glows, cam.eye());
        BufferedImage img = canvas.image();
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        g.drawString(text, 6, 16);
        g.dispose();
        return img;
    }

    /** The blade in his fist and, while he draws it ({@code after} ticks into the draw; NaN: not drawing), the golden case, held or dropped. */
    static void bladeAndCase(RigPose pose, double after, List<PoseRender.Tri> lit, List<Boolean> glows) {
        boolean drawing = !Double.isNaN(after);
        double[] fist = {-1.0, 9.0, 0.0};
        if (drawing && ReaperBladePose.caseHeld(after) && after >= AgentRules.REAPER_CASE_FROM) {
            double[][] turn = ReaperBladePose.caseTurn(pose, after);
            double angle = ReaperBladePose.caseOpen(after);
            for (int side : new int[] {1, -1}) {
                ArmMesh.Mesh half = ReaperBladeMesh.goldHalf(side);
                add(lit, glows, half, v -> PoseRender.point(pose, "rightArm", plus(fist, apply(turn, ReaperBladeMesh.opened(v, side, angle)))));
            }
            if (after < AgentRules.REAPER_CASE_OPENS) {
                add(lit, glows, ReaperBladeMesh.goldHandle(), v -> PoseRender.point(pose, "rightArm", plus(fist, apply(turn, v))));
            }
        }
        if (drawing && after >= AgentRules.REAPER_CASE_DROPS) {
            double dt = after - AgentRules.REAPER_CASE_DROPS;
            for (int side : new int[] {1, -1}) {
                ArmMesh.Mesh half = ReaperBladeMesh.goldHalf(side);
                double fall = 0.5 * ReaperBladePose.CASE_FALL * dt * dt;
                add(lit, glows, half, v -> {
                    double[] q = ReaperBladePose.dropped(v, side, dt, false);
                    return PoseRender.world(new RigPose(), new double[] {q[0], Math.min(24.0, q[1] + fall), q[2]});
                });
            }
        }
        if (!drawing || after >= AgentRules.REAPER_CASE_OPENS) {
            double[][] inArm = ReaperBladePose.inArm(pose);
            float k = drawing ? ReaperBladePose.extended(after) : 1.0F;
            add(lit, glows, ReaperBladeMesh.of(), v -> {
                double[] p = {k >= 1.0F ? v[0] : ReaperBladeMesh.slid(v[0], k), v[1], v[2]};
                return PoseRender.point(pose, "rightArm", plus(fist, apply(inArm, p)));
            });
        }
    }

    static void add(List<PoseRender.Tri> tris, List<Boolean> glows, ArmMesh.Mesh mesh, java.util.function.Function<double[], double[]> place) {
        QuinqueMeshPreview.add(tris, glows, mesh, v -> place.apply(new double[] {v[0], v[1], v[2]}));
    }

    static double[] apply(double[][] m, double[] v) {
        return new double[] {m[0][0] * v[0] + m[0][1] * v[1] + m[0][2] * v[2], m[1][0] * v[0] + m[1][1] * v[1] + m[1][2] * v[2],
            m[2][0] * v[0] + m[2][1] * v[1] + m[2][2] * v[2]};
    }

    static double[] plus(double[] a, double[] b) {
        return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    static void sheet(File out, List<BufferedImage> frames, int cols) throws Exception {
        int rows = (frames.size() + cols - 1) / cols;
        BufferedImage s = new BufferedImage(cols * W, rows * H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = s.createGraphics();
        for (int i = 0; i < frames.size(); i++) {
            g.drawImage(frames.get(i), i % cols * W, i / cols * H, null);
        }
        g.dispose();
        out.getAbsoluteFile().getParentFile().mkdirs();
        ImageIO.write(s, "png", out);
        System.out.println("wrote " + out);
    }
}
