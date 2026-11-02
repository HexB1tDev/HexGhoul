import com.hex.kagune.control.client.animation.ChildMomentPose;
import com.hex.kagune.control.client.animation.ChildTalkPose;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.family.ChildCue;
import com.hex.kagune.control.family.ChildStage;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * 1.3.57: a child's talk cues posed off-game ({@link ChildTalkPose}, put on the child exactly as {@code ChildRenderer} does, through
 * {@link MomentPreview#kid}): each cue at its telling frame on a child and on a toddler, and two siblings squabbling face to face. The body's
 * hop ({@code drop}) and tip ({@code flip}) as the renderer applies them. Run {@code python3 build.py} first:
 * <pre>
 * javac -nowarn -cp build/classes:build/base.jar -d build/talk-preview tools/PoseRender.java tools/MomentPreview.java tools/TalkPreview.java
 * java -Djava.awt.headless=true -cp build/talk-preview:build/classes:build/base.jar TalkPreview &lt;out-dir&gt; &lt;kid skin png&gt; &lt;another&gt;
 * </pre>
 */
public final class TalkPreview {
    record Shot(String label, ChildCue cue, double age, ChildStage stage, double camYaw) {
    }

    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "build/preview");
        out.mkdirs();
        BufferedImage boy = MomentPreview.small(ImageIO.read(new File(args[1])));
        BufferedImage girl = MomentPreview.small(ImageIO.read(new File(args.length > 2 ? args[2] : args[1])));
        double[] telling = {0, 30, 8.5, 16, 30, 11, 30, 10, 20, 30, 7, 22, 45, 30, 14, 20, 300, 40};
        List<Shot> shots = new ArrayList<>();
        for (ChildCue cue : ChildCue.values()) {
            if (cue != ChildCue.NONE) {
                shots.add(new Shot(cue.name().toLowerCase() + " (child)", cue, telling[cue.ordinal()], ChildStage.CHILD, 30));
            }
        }
        for (ChildCue cue : new ChildCue[] {ChildCue.CROSS, ChildCue.STAMP, ChildCue.SORRY, ChildCue.PLEAD, ChildCue.SULK, ChildCue.HIDE, ChildCue.COUNT}) {
            shots.add(new Shot(cue.name().toLowerCase() + " (toddler)", cue, telling[cue.ordinal()], ChildStage.TODDLER, 30));
        }
        int w = 260;
        int h = 300;
        int columns = 5;
        int rows = (shots.size() + columns - 1) / columns;
        BufferedImage sheet = new BufferedImage(columns * w, rows * (h + 22) + 36 + 360 + 22, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(34, 34, 40));
        g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        g.drawString("A child's body as it talks (1.3.57 ChildTalkPose, on its in-game proportions)", 10, 24);
        for (int i = 0; i < shots.size(); i++) {
            Shot shot = shots.get(i);
            List<PoseRender.Tri> kid = place(posed(shot.cue(), shot.age(), shot.stage()), shot.stage(), shot.stage() == ChildStage.TODDLER ? girl : boy,
                0.0, 0.0);
            double height = MomentPreview.height(shot.stage()) * 1.8;
            BufferedImage image = alone(kid, PoseRender.orbit(shot.camYaw(), 8.0, 2.4 * height + 0.6, height * 0.55), w, h);
            int x = i % columns * w;
            int y = 36 + i / columns * (h + 22);
            g.drawImage(image, x, y, null);
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
            g.drawString(shot.label(), x + 10, y + h + 16);
        }
        // Two siblings squabbling, face to face: one points, the other folds its arms and looks away.
        List<PoseRender.Tri> both = new ArrayList<>(place(posed(ChildCue.POINT, 16, ChildStage.CHILD), ChildStage.CHILD, boy, 0.0, -0.8));
        both.addAll(place(posed(ChildCue.CROSS, 30, ChildStage.CHILD), ChildStage.CHILD, girl, Math.PI, 0.8));
        BufferedImage pair = alone(both, PoseRender.orbit(90, 10.0, 4.2, 0.65), columns * w, 360);
        int y = 36 + rows * (h + 22);
        g.drawImage(pair, 0, y, null);
        g.setColor(Color.WHITE);
        g.drawString("a squabble: one points, the other folds its arms and looks away (seen from the side)", 10, y + 360 + 16);
        g.dispose();
        ImageIO.write(sheet, "png", new File(out, "talk-cues.png"));
        System.out.println("wrote " + new File(out, "talk-cues.png"));
    }

    /** The cue's pose as ChildRenderer.posed blends it (at full weight). */
    static RigPose posed(ChildCue cue, double age, ChildStage stage) {
        RigPose p = ChildTalkPose.pose(cue, age, stage, false);
        RigPose out = new RigPose();
        ChildMomentPose.blend(out, p, 1.0F);
        out.armHold = p.armHold;
        out.rightArmHold = p.rightArmHold;
        out.leftArmHold = p.leftArmHold;
        out.legHold = p.legHold;
        return out;
    }

    /**
     * The child posed and placed: its hop down ({@code drop}, model pixels at its size) and its tip forward about its middle
     * ({@code flip}) as the renderer does, then turned by {@code turn} and moved {@code z} along the line between the two.
     */
    static List<PoseRender.Tri> place(RigPose p, ChildStage stage, BufferedImage skin, double turn, double z) {
        BufferedImage was = PoseRender.skin;
        PoseRender.skin = skin;
        List<PoseRender.Tri> kid = MomentPreview.kid(p, stage, false, false);
        PoseRender.skin = was;
        double size = stage.draw / MomentPreview.PLAYER;
        double middle = (stage.young ? 0.62 : 0.9) * size;
        double drop = -p.drop / 16.0 * size;
        double cf = Math.cos(p.flip);
        double sf = Math.sin(p.flip);
        double ct = Math.cos(turn);
        double st = Math.sin(turn);
        List<PoseRender.Tri> out = new ArrayList<>();
        for (PoseRender.Tri t : kid) {
            double[][] v = new double[t.v().length][];
            for (int i = 0; i < v.length; i++) {
                double[] q = t.v()[i];
                double y = q[1] - middle;
                double zz = q[2];
                double ty = y * cf - zz * sf + middle + drop;
                double tz = y * sf + zz * cf;
                v[i] = new double[] {q[0] * ct + tz * st, ty, -q[0] * st + tz * ct + z};
            }
            double[] n = t.n();
            double[] m = n == null ? null : new double[] {n[0], n[1] * cf - n[2] * sf, n[1] * sf + n[2] * cf};
            double[] nn = m == null ? null : new double[] {m[0] * ct + m[2] * st, m[1], -m[0] * st + m[2] * ct};
            out.add(new PoseRender.Tri(v, t.rgb(), nn));
        }
        return out;
    }

    /** Only these triangles on the floor (no player), seen by {@code cam}. */
    static BufferedImage alone(List<PoseRender.Tri> tris, PoseRender.Camera cam, int w, int h) {
        PoseRender.Canvas canvas = new PoseRender.Canvas(w, h, 2, cam, 0x3A4450);
        canvas.floor(6, 0x58626C, 0x4E5862);
        for (PoseRender.Tri t : tris) {
            canvas.tri(t, true);
        }
        return canvas.image();
    }
}
