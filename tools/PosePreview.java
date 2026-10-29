import com.hex.kagune.control.client.animation.IdlePose;
import com.hex.kagune.control.client.animation.RigPose;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javax.imageio.ImageIO;

/**
 * Pose sheets from {@link PoseRender} (the game's exact PlayerAnimator/bendy-lib transforms on a stand-in body).
 * Usage: {@code java -cp <pure classes> tools/PosePreview.java <out-dir> [sheet ...]}; sheets: idle.
 * Reference pictures (git-ignored) are read from {@code -Dref=<dir>} when present: bakugo.png, jotaro.png.
 */
public final class PosePreview {
    record Panel(String label, Supplier<BufferedImage> image) {
    }

    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "build/preview");
        out.mkdirs();
        java.util.Set<String> wanted = new java.util.HashSet<>(java.util.Arrays.asList(args).subList(Math.min(1, args.length), args.length));
        if (wanted.isEmpty() || wanted.contains("idle")) {
            idle(new File(out, "idle-poses.png"));
        }
        if (wanted.contains("closeup")) {
            RigPose ayato = IdlePose.pose(IdlePose.Style.AYATO_RELAXED, 1.0F, 0.0);
            RigPose kaneki = IdlePose.pose(IdlePose.Style.KANEKI_FERAL, 1.0F, 0.0);
            List<Panel> panels = new ArrayList<>();
            panels.add(reference("Jotaro", "jotaro.png"));
            panels.add(close("Ayato, front", ayato, 0, 6));
            panels.add(close("Ayato, three-quarter", ayato, -30, 8));
            panels.add(reference("Bakugo", "bakugo.png"));
            panels.add(close("Kaneki, front", kaneki, 0, 6));
            panels.add(close("Kaneki, three-quarter", kaneki, 30, 8));
            sheet(new File(out, "idle-closeup.png"), "Close-ups beside the references", panels, 3, 420, 520);
        }
    }

    /** Reference panels only when the (git-ignored) reference pictures are there; committed sheets leave them out. */
    static boolean references() {
        return new File(System.getProperty("ref", "docs/reference"), "jotaro.png").exists() && !Boolean.getBoolean("noref");
    }

    static void idle(File file) throws Exception {
        RigPose ayato = IdlePose.pose(IdlePose.Style.AYATO_RELAXED, 1.0F, 0.0);
        RigPose kaneki = IdlePose.pose(IdlePose.Style.KANEKI_FERAL, 1.0F, 0.0);
        report("Ayato", ayato);
        report("Kaneki", kaneki);
        List<Panel> panels = new ArrayList<>();
        if (references()) panels.add(reference("Reference: Jotaro (Ayato's stance)", "jotaro.png"));
        panels.add(view("Ayato relaxed, front", ayato, 0, 8));
        panels.add(view("Ayato relaxed, three-quarter", ayato, 35, 10));
        panels.add(view("Ayato relaxed, side (his left)", ayato, 90, 6));
        panels.add(view("Ayato relaxed, other side", ayato, -70, 8));
        panels.add(view("Ayato relaxed, from behind (F5)", ayato, 180, 22));
        if (references()) panels.add(reference("Reference: Bakugo (Kaneki's stance)", "bakugo.png"));
        panels.add(view("Kaneki animalistic, front", kaneki, 0, 8));
        panels.add(view("Kaneki animalistic, three-quarter", kaneki, 35, 10));
        panels.add(view("Kaneki animalistic, side", kaneki, 90, 6));
        panels.add(view("Kaneki animalistic, other side", kaneki, -60, 12));
        panels.add(view("Kaneki animalistic, from behind (F5)", kaneki, 180, 22));
        if (references()) {
            panels.add(view("Standing (no pose), front", new RigPose(), 0, 8));
            panels.add(view("Ayato, easing in halfway", IdlePose.pose(IdlePose.Style.AYATO_RELAXED, 0.5F, 0.0), 30, 10));
            panels.add(view("Kaneki, easing in halfway", IdlePose.pose(IdlePose.Style.KANEKI_FERAL, 0.5F, 0.0), 30, 10));
        }
        sheet(file, "Ambient idle stances (exact PlayerAnimator + bendy-lib transforms)", panels, 3, 360, 440);
    }

    /** Where each foot ends up: the lowest point of each leg should sit on the ground (y 0). */
    static void report(String name, RigPose pose) {
        double right = Double.POSITIVE_INFINITY, left = Double.POSITIVE_INFINITY;
        List<PoseRender.Tri> tris = PoseRender.model(pose);
        // Legs are the last two cubes; their lowest vertices are the soles.
        int legTris = 2 * 2 * (4 * 12 + 4 * 12 + 4 * 12 + 4 * 12 + 4 * 4 + 4 * 4);
        for (int i = tris.size() - 2 * legTris; i < tris.size(); i++) {
            for (double[] v : tris.get(i).v()) {
                if (i < tris.size() - legTris) right = Math.min(right, v[1]);
                else left = Math.min(left, v[1]);
            }
        }
        System.out.printf("%s: right sole at %.3f blocks, left sole at %.3f blocks, drop %.2f px%n", name, right, left, pose.drop);
    }

    static Panel view(String label, RigPose pose, double yaw, double pitch) {
        return new Panel(label, () -> PoseRender.render(pose, PoseRender.orbit(yaw, pitch, 4.4, 0.95), 360, 410, null));
    }

    static Panel close(String label, RigPose pose, double yaw, double pitch) {
        return new Panel(label, () -> PoseRender.render(pose, PoseRender.orbit(yaw, pitch, 4.0, 1.0), 420, 490, null));
    }

    static Panel reference(String label, String name) {
        return new Panel(label, () -> {
            File file = new File(System.getProperty("ref", "docs/reference"), name);
            BufferedImage canvas = new BufferedImage(360, 410, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = canvas.createGraphics();
            g.setColor(new Color(0xF2F0EC));
            g.fillRect(0, 0, 360, 410);
            try {
                BufferedImage ref = ImageIO.read(file);
                double k = Math.min(350.0 / ref.getWidth(), 400.0 / ref.getHeight());
                int w = (int)(ref.getWidth() * k), h = (int)(ref.getHeight() * k);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(ref, (360 - w) / 2, (410 - h) / 2, w, h, null);
            } catch (Exception missing) {
                g.setColor(Color.DARK_GRAY);
                g.drawString("(reference " + name + " not found)", 20, 200);
            }
            g.dispose();
            return canvas;
        });
    }

    static void sheet(File file, String title, List<Panel> panels, int columns, int pw, int ph) throws Exception {
        int rows = (panels.size() + columns - 1) / columns;
        BufferedImage sheet = new BufferedImage(columns * pw, rows * ph + 36, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setColor(new Color(0x101014));
        g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        g.setColor(new Color(0xEDE3D6));
        g.drawString(title, 12, 25);
        for (int i = 0; i < panels.size(); i++) {
            int x = (i % columns) * pw, y = (i / columns) * ph + 36;
            g.drawImage(panels.get(i).image().get(), x, y, null);
            g.setColor(new Color(0x101014));
            g.fillRect(x, y + ph - 30, pw, 30);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            g.setColor(new Color(0xEDE3D6));
            g.drawString(panels.get(i).label(), x + 8, y + ph - 10);
        }
        g.dispose();
        ImageIO.write(sheet, "png", file);
        System.out.println("wrote " + file);
    }
}
