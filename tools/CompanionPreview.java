import com.hex.kagune.control.client.animation.EtoPose;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.npc.EtoGesture;
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
 * 1.3.14: the companions posed off-game ({@link PoseRender}, the game's own transforms; a mob's stiff limbs, no bust drawn): Touka's
 * strict, pouty idles, Ken's quiet ones, and Ken's way of holding someone (the kiss, making out, the forehead kiss, the hug, an arm round
 * them). Run {@code python3 build.py} first, then:
 * <pre>
 * javac -nowarn -cp build/classes:build/base.jar -d build/companion-preview tools/PoseRender.java tools/CompanionPreview.java
 * java -Djava.awt.headless=true -cp build/companion-preview:build/classes:build/base.jar CompanionPreview &lt;out-dir&gt;
 * </pre>
 */
public final class CompanionPreview {
    record Shot(String label, RigPose pose, double yaw) {
    }

    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "build/preview");
        out.mkdirs();
        if (args.length > 1 && args[1].equals("sit")) {
            // 4.1.69 (the owner: "Ken's sitting animation is broken, one of his legs is just in the air."): Ken sitting, held, all round.
            List<Shot> sit = new ArrayList<>();
            for (double yaw : new double[] {0.0, 90.0, 180.0, 270.0, 35.0, 325.0}) {
                sit.add(new Shot("sit " + (int)yaw, EtoPose.gesture(EtoGesture.SIT, 200.0, true), yaw));
            }
            sheet(new File(out, "ken-sit.png"), "Ken", "src/main/resources/assets/hexkagune/textures/entity/ken/ken.png", true, sit);
            // Sitting down, from his right side (the propping hand) and from the front three quarters: tick by tick.
            List<Shot> down = new ArrayList<>();
            for (double yaw : new double[] {270.0, 30.0}) {
                for (double tick : new double[] {0.0, 5.0, 8.0, 11.0, 14.0}) {
                    down.add(new Shot("tick " + (int)tick + ", " + (int)yaw, EtoPose.gesture(EtoGesture.SIT, tick, true), yaw));
                }
            }
            sheet(new File(out, "ken-sit-down.png"), "Ken sitting down", "src/main/resources/assets/hexkagune/textures/entity/ken/ken.png", true, down);
            // Getting up (called away: into no gesture), through the game's own fade, from his right side.
            List<Shot> up = new ArrayList<>();
            com.hex.kagune.control.client.animation.PoseFade fade = new com.hex.kagune.control.client.animation.PoseFade();
            fade.through(EtoPose.gesture(EtoGesture.SIT, 400.0, true), 1L, 500.0);
            for (double tick : new double[] {0.0, 1.0, 2.0, 3.0, 4.0}) {
                RigPose now = fade.through(null, 2L, 500.0 + tick);
                up.add(new Shot("up, tick " + (int)tick, now == null ? new RigPose() : now, 270.0));
            }
            sheet(new File(out, "ken-sit-up.png"), "Ken getting up", "src/main/resources/assets/hexkagune/textures/entity/ken/ken.png", true, up);
            return;
        }
        if (args.length > 1 && args[1].equals("frames")) {
            frames(out);
            return;
        }
        if (args.length > 1 && args[1].equals("kisses")) {
            kisses(out);
            return;
        }
        if (args.length > 1 && args[1].equals("new-kisses")) {
            newKisses(out);
            return;
        }
        if (args.length > 1 && args[1].equals("jealous")) {
            jealous(out);
            return;
        }
        if (args.length > 1 && args[1].equals("crack")) {
            crack(out);
            return;
        }
        if (args.length > 1 && args[1].equals("cooking")) {
            cooking(out);
            return;
        }
        String textures = "src/main/resources/assets/hexkagune/textures/entity/";
        List<Shot> touka = new ArrayList<>();
        for (Object[] g : new Object[][] {{EtoGesture.CROSS, 40.0}, {EtoGesture.TAP, 20.0}, {EtoGesture.HUFF, 14.0}, {EtoGesture.SIP, 50.0},
            {EtoGesture.WIPE, 30.0}, {EtoGesture.KICKS, 10.0}, {EtoGesture.SCOLD, 20.0}, {EtoGesture.SHY, 40.0}, {EtoGesture.PECK, 15.0}}) {
            EtoGesture gesture = (EtoGesture)g[0];
            touka.add(new Shot(gesture.name().toLowerCase(), EtoPose.gesture(gesture, (Double)g[1], false), 25.0));
        }
        sheet(new File(out, "touka-poses.png"), "Touka", textures + "touka/touka.png", true, touka);
        List<Shot> ken = new ArrayList<>();
        for (Object[] g : new Object[][] {{EtoGesture.CHIN, 40.0}, {EtoGesture.CRACK, 24.0}, {EtoGesture.BOW, 18.0}, {EtoGesture.READ, 40.0},
            {EtoGesture.SIP, 50.0}}) {
            EtoGesture gesture = (EtoGesture)g[0];
            ken.add(new Shot(gesture.name().toLowerCase(), EtoPose.gesture(gesture, (Double)g[1], true), 12.0));
        }
        for (Object[] g : new Object[][] {{EtoGesture.KISS, 30.0, "kiss (waiting)"}, {EtoGesture.PULL, 30.0, "making out"},
            {EtoGesture.PECK, 15.0, "forehead kiss"}, {EtoGesture.HUG, 25.0, "hug"}, {EtoGesture.CLING, 40.0, "arm round them"},
            {EtoGesture.KISSED, 15.0, "after a kiss"}}) {
            ken.add(new Shot((String)g[2], EtoPose.gesture((EtoGesture)g[0], (Double)g[1], true), 12.0));
            ken.add(new Shot((String)g[2] + ", side", EtoPose.gesture((EtoGesture)g[0], (Double)g[1], true), 80.0));
        }
        sheet(new File(out, "ken-poses.png"), "Ken", textures + "ken/ken.png", true, ken);
        // 1.3.65: at the worst sights: a flinch from War (Touka, Ken), awe at Peace (all three), front and side, as it starts and held.
        for (Object[] who : new Object[][] {{"Eto", "eto/eto.png", true, 0}, {"Touka", "touka/touka.png", true, 2}, {"Ken", "ken/ken.png", false, 1}}) {
            List<Shot> fear = new ArrayList<>();
            for (EtoGesture gesture : new EtoGesture[] {EtoGesture.FLINCH, EtoGesture.AWE}) {
                for (double age : new double[] {4.0, 30.0}) {
                    for (double yaw : new double[] {20.0, 80.0}) {
                        fear.add(new Shot(gesture.name().toLowerCase() + " t" + (int)age + (yaw > 50 ? " side" : ""),
                            EtoPose.gesture(gesture, age, (Integer)who[3], 0), yaw));
                    }
                }
            }
            sheet(new File(out, ((String)who[0]).toLowerCase() + "-fear-poses.png"), who[0] + " (1.3.65 flinch, awe)", textures + who[1],
                (Boolean)who[2], fear);
        }
    }

    /**
     * Animation frames (every other tick, ten a second): each gesture looping as it plays, one sheet per frame per companion; a GIF is made
     * of them by {@code tools/companion-gifs.py}.
     */
    static void frames(File out) throws Exception {
        String textures = "src/main/resources/assets/hexkagune/textures/entity/";
        Object[][] touka = {{EtoGesture.CROSS, "arms folded"}, {EtoGesture.TAP, "foot tapping"}, {EtoGesture.HUFF, "huff"},
            {EtoGesture.SIP, "sipping coffee"}, {EtoGesture.WIPE, "drying a cup"}, {EtoGesture.KICKS, "practice kick"}, {EtoGesture.SCOLD, "telling you off"},
            {EtoGesture.SHY, "shy, turned away"}, {EtoGesture.PECK, "a quick kiss"}, {EtoGesture.KISS, "waiting for a kiss"}};
        Object[][] ken = {{EtoGesture.CHIN, "hand to chin"}, {EtoGesture.CRACK, "cracking a finger"}, {EtoGesture.BOW, "a bow"},
            {EtoGesture.READ, "reading"}, {EtoGesture.SIP, "coffee"}, {EtoGesture.KISS, "kiss (waiting)"}, {EtoGesture.PULL, "making out"},
            {EtoGesture.PECK, "forehead kiss"}, {EtoGesture.HUG, "hug"}, {EtoGesture.KISSED, "after a kiss"}};
        frames(new File(out, "touka-frames"), textures + "touka/touka.png", false, touka);
        frames(new File(out, "ken-frames"), textures + "ken/ken.png", true, ken);
    }

    static void frames(File folder, String skin, boolean leads, Object[][] gestures) throws Exception {
        folder.mkdirs();
        PoseRender.skin = ImageIO.read(new File(skin));
        PoseRender.slim = true;
        PoseRender.stiff = true;
        int w = 200;
        int h = 260;
        int columns = 5;
        int rows = (gestures.length + columns - 1) / columns;
        for (int frame = 0; frame < 60; frame++) {
            double tick = frame * 2.0;
            BufferedImage sheet = new BufferedImage(columns * w, rows * (h + 20), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = sheet.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(new Color(34, 34, 40));
            g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
            for (int i = 0; i < gestures.length; i++) {
                EtoGesture gesture = (EtoGesture)gestures[i][0];
                double length = gesture.held() ? 120.0 : gesture.ticks + 10.0;
                RigPose pose = EtoPose.gesture(gesture, tick % length, leads);
                BufferedImage image = PoseRender.render(pose == null ? new RigPose() : pose, PoseRender.orbit(22.0, 8.0, 4.4, 0.95), w, h, null);
                int x = i % columns * w;
                int y = i / columns * (h + 20);
                g.drawImage(image, x, y, null);
                g.setColor(Color.WHITE);
                g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
                g.drawString((String)gestures[i][1], x + 8, y + h + 15);
            }
            g.dispose();
            ImageIO.write(sheet, "png", new File(folder, String.format("frame_%03d.png", frame)));
        }
    }

    /**
     * 2.2.4: the kiss as each mood has it ({@code MoodKissPose}): every kind, waiting (three-quarter and side), blushing (held a while) and
     * pulling them in (held longer), on Eto, Touka and Ken; and frames of each waiting, for a GIF ({@code kisses-frames}).
     */
    static void kisses(File out) throws Exception {
        String textures = "src/main/resources/assets/hexkagune/textures/entity/";
        for (Object[] who : new Object[][] {{"Eto", "eto/eto.png", true, 0}, {"Touka", "touka/touka.png", true, 2}, {"Ken", "ken/ken.png", false, 1}}) {
            List<Shot> shots = new ArrayList<>();
            for (com.hex.kagune.control.npc.MoodKiss.Style style : com.hex.kagune.control.npc.MoodKiss.Style.values()) {
                String name = style.name().toLowerCase();
                shots.add(new Shot(name + ": waiting", kiss(style, EtoGesture.KISS, 30.0, (Integer)who[3]), 28.0));
                shots.add(new Shot(name + ": waiting, side", kiss(style, EtoGesture.KISS, 30.0, (Integer)who[3]), 85.0));
                shots.add(new Shot(name + ": held (blush)", kiss(style, EtoGesture.BLUSH, 30.0, (Integer)who[3]), 28.0));
                shots.add(new Shot(name + ": pulled in", kiss(style, EtoGesture.PULL, 30.0, (Integer)who[3]), 28.0));
                shots.add(new Shot(name + ": pulled in, side", kiss(style, EtoGesture.PULL, 30.0, (Integer)who[3]), 85.0));
            }
            sheet(new File(out, ((String)who[0]).toLowerCase() + "-mood-kisses.png"), who[0] + ": a kiss for every mood (2.2.4)", textures + who[1],
                (Boolean)who[2], shots);
        }
        for (Object[] who : new Object[][] {{"eto", "eto/eto.png", true, 0}, {"touka", "touka/touka.png", true, 2}, {"ken", "ken/ken.png", false, 1}}) {
            kissFrames(new File(out, who[0] + "-kisses-frames"), textures + who[1], (Boolean)who[2], (Integer)who[3]);
        }
    }

    /** 2.4.9: the spouse at the stove (the fire fed, seasoning, a taste) and holding the food out, on Eto, Touka and Ken. */
    static void cooking(File out) throws Exception {
        String textures = "src/main/resources/assets/hexkagune/textures/entity/";
        for (Object[] who : new Object[][] {{"Eto", "eto/eto.png", true, 0}, {"Touka", "touka/touka.png", true, 2}, {"Ken", "ken/ken.png", false, 1}}) {
            List<Shot> shots = new ArrayList<>();
            for (Object[] g : new Object[][] {{EtoGesture.STOKE, 8.0}, {EtoGesture.STOKE, 5.0}, {EtoGesture.SEASON, 20.0}, {EtoGesture.SEASON, 21.0},
                {EtoGesture.TASTE, 10.0}, {EtoGesture.TASTE, 30.0}, {EtoGesture.TASTE, 42.0}, {EtoGesture.COOK, 40.0}, {EtoGesture.SERVE, 25.0},
                {EtoGesture.SERVE, 25.0}}) {
                EtoGesture gesture = (EtoGesture)g[0];
                double t = (Double)g[1];
                boolean side = shots.size() % 2 == 1;
                shots.add(new Shot(gesture.name().toLowerCase() + " t" + (int)t + (side ? ", side" : ""), EtoPose.gesture(gesture, t, (Integer)who[3], 0),
                    side ? 85.0 : 30.0));
            }
            sheet(new File(out, ((String)who[0]).toLowerCase() + "-cooking.png"), who[0] + ": at the stove, and handing the food over (2.4.9)",
                textures + who[1], (Boolean)who[2], shots);
        }
    }

    /** 2.4.8: Ken's finger crack, one arm out, through the gesture (front, three-quarter, side), as he does it (persona 1). */
    /** 3.3.6: a jealous companion's act (EtoGesture.JEALOUS), on Touka, seen from where the player stands and from the side. */
    static void jealous(File out) throws Exception {
        String textures = "src/main/resources/assets/hexkagune/textures/entity/";
        List<Shot> shots = new ArrayList<>();
        for (double t : new double[] {4.0, 12.0, 30.0, 50.0, 72.0, 95.0, 120.0, 138.0}) {
            RigPose pose = EtoPose.gesture(EtoGesture.JEALOUS, t);
            shots.add(new Shot("t" + (int)t + ": from you", pose, 0.0));
            shots.add(new Shot("t" + (int)t + ": side", pose, 80.0));
        }
        sheet(new File(out, "jealous-3.3.6.png"), "3.3.6: jealous: back turned, a glare over the shoulder, a huff, the snub", textures + "touka/touka.png", true, shots);
    }

    static void crack(File out) throws Exception {
        String textures = "src/main/resources/assets/hexkagune/textures/entity/";
        List<Shot> shots = new ArrayList<>();
        for (double t : new double[] {4.0, 9.0, 13.0, 17.0, 22.0, 34.0, 44.0}) {
            RigPose pose = EtoPose.gesture(EtoGesture.CRACK, t, 1, 0);
            shots.add(new Shot("t" + (int)t + ": front", pose, 0.0));
            shots.add(new Shot("t" + (int)t + ": three-quarter", pose, 35.0));
            shots.add(new Shot("t" + (int)t + ": side", pose, 85.0));
        }
        sheet(new File(out, "ken-crack.png"), "Ken: the finger crack, stretched out, then bent (2.5.3)", textures + "ken/ken.png", false, shots);
        List<Shot> player = new ArrayList<>();
        for (double t : new double[] {4.0, 9.0, 13.0, 17.0, 22.0, 34.0, 44.0}) {
            RigPose pose = new RigPose();
            EtoPose.crackOver(pose, (float)t);
            player.add(new Shot("t" + (int)t + ": front", pose, 0.0));
            player.add(new Shot("t" + (int)t + ": three-quarter", pose, 35.0));
            player.add(new Shot("t" + (int)t + ": side", pose, 85.0));
        }
        bendy = true;
        sheet(new File(out, "player-crack.png"), "A player Ken taught: the same crack over their stance, stretched then bent (2.5.3)", textures + "ken/ken.png", false, player);
        bendy = false;
    }

    /** Frames of every kind of kiss waiting, side by side, moving, on one companion ({@code persona}: Persona.style). */
    static void kissFrames(File folder, String skin, boolean slim, int persona) throws Exception {
        folder.mkdirs();
        PoseRender.skin = ImageIO.read(new File(skin));
        PoseRender.slim = slim;
        PoseRender.stiff = true;
        com.hex.kagune.control.npc.MoodKiss.Style[] styles = com.hex.kagune.control.npc.MoodKiss.Style.values();
        int w = 200;
        int h = 260;
        int columns = 5;
        int rows = (styles.length + columns - 1) / columns;
        for (int frame = 0; frame < 80; frame++) {
            double tick = frame * 2.0;
            BufferedImage sheet = new BufferedImage(columns * w, rows * (h + 20), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = sheet.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(new Color(34, 34, 40));
            g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
            for (int i = 0; i < styles.length; i++) {
                RigPose pose = kiss(styles[i], EtoGesture.KISS, tick, persona);
                BufferedImage image = PoseRender.render(pose == null ? new RigPose() : pose, PoseRender.orbit(40.0, 8.0, 4.4, 0.95), w, h, null);
                int x = i % columns * w;
                int y = i / columns * (h + 20);
                g.drawImage(image, x, y, null);
                g.setColor(Color.WHITE);
                g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
                g.drawString(styles[i].name().toLowerCase(), x + 8, y + h + 15);
            }
            g.dispose();
            ImageIO.write(sheet, "png", new File(folder, String.format("frame_%03d.png", frame)));
        }
    }

    /**
     * 4.1.15 (the owner: "add more kiss types"): the new kinds only (shy, the morning after, in the rain, a film's, nose to nose, a bow over
     * their hand), waiting (three-quarter and side), held, pulled in, on Eto, Touka and Ken.
     */
    static void newKisses(File out) throws Exception {
        String textures = "src/main/resources/assets/hexkagune/textures/entity/";
        com.hex.kagune.control.npc.MoodKiss.Style[] styles = com.hex.kagune.control.npc.MoodKiss.Style.values();
        for (Object[] who : new Object[][] {{"Eto", "eto/eto.png", true, 0}, {"Touka", "touka/touka.png", true, 2}, {"Ken", "ken/ken.png", false, 1}}) {
            List<Shot> shots = new ArrayList<>();
            for (int i = com.hex.kagune.control.npc.MoodKiss.Style.SHY.ordinal(); i < styles.length; i++) {
                String name = styles[i].name().toLowerCase();
                shots.add(new Shot(name + ": waiting", kiss(styles[i], EtoGesture.KISS, 12.0, (Integer)who[3]), 28.0));
                shots.add(new Shot(name + ": waiting, side", kiss(styles[i], EtoGesture.KISS, 12.0, (Integer)who[3]), 85.0));
                shots.add(new Shot(name + ": held", kiss(styles[i], EtoGesture.BLUSH, 30.0, (Integer)who[3]), 28.0));
                shots.add(new Shot(name + ": pulled in, side", kiss(styles[i], EtoGesture.PULL, 30.0, (Integer)who[3]), 85.0));
            }
            sheet(new File(out, ((String)who[0]).toLowerCase() + "-new-kisses.png"), who[0] + ": the new kisses (4.1.15)", textures + who[1],
                (Boolean)who[2], shots);
        }
    }

    /** The kiss pose as the game draws it: the persona's usual, with the mood's over it. */
    static RigPose kiss(com.hex.kagune.control.npc.MoodKiss.Style style, EtoGesture gesture, double age, int persona) {
        RigPose usual = EtoPose.gesture(gesture, age, persona, 0);
        return com.hex.kagune.control.client.animation.MoodKissPose.apply(usual, style, gesture, age, persona);
    }

    /** 2.4.8: drawn as a player is (elbows and knees bend), not as a companion's stiff limbs. */
    static boolean bendy = false;

    static void sheet(File file, String title, String skin, boolean slim, List<Shot> shots) throws Exception {
        PoseRender.skin = ImageIO.read(new File(skin));
        PoseRender.slim = slim;
        PoseRender.stiff = !bendy;
        int w = 260;
        int h = 330;
        int columns = 5;
        int rows = (shots.size() + columns - 1) / columns;
        BufferedImage sheet = new BufferedImage(columns * w, rows * (h + 22) + 36, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(34, 34, 40));
        g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        g.drawString(title, 10, 24);
        for (int i = 0; i < shots.size(); i++) {
            Shot shot = shots.get(i);
            BufferedImage image = PoseRender.render(shot.pose() == null ? new RigPose() : shot.pose(), PoseRender.orbit(shot.yaw(), 8.0, 4.2, 0.95), w, h, null);
            int x = i % columns * w;
            int y = 36 + i / columns * (h + 22);
            g.drawImage(image, x, y, null);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
            g.drawString(shot.label(), x + 10, y + h + 16);
        }
        g.dispose();
        ImageIO.write(sheet, "png", file);
    }
}
