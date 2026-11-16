import com.hex.kagune.control.client.animation.ChildTalkPose;
import com.hex.kagune.control.client.animation.EtoPose;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.family.ChildCue;
import com.hex.kagune.control.npc.EtoGesture;
import com.hex.kagune.control.village.HumanGestures;
import com.hex.kagune.control.village.HumanPersona;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * 2.0: every move a villager makes ({@link HumanGestures}: Eto's gestures in each temper's style, the children's talk cues on a grown body),
 * posed off-game exactly as {@code HumanRenderer.posed} poses it ({@link PoseRender}: the game's own transforms), at a few moments of each,
 * on a villager's skin. Run {@code python3 build.py} first, then:
 * <pre>
 * javac -nowarn -cp build/classes:build/base.jar -d build/villager-preview tools/PoseRender.java tools/VillagerPreview.java
 * java -Djava.awt.headless=true -cp build/villager-preview:build/classes:build/base.jar VillagerPreview &lt;out-dir&gt; &lt;skin&gt; &lt;slim&gt;
 * </pre>
 */
public final class VillagerPreview {
    record Shot(String label, RigPose pose, double yaw) {
    }

    public static void main(String[] args) throws Exception {
        File out = new File(args[0]);
        out.mkdirs();
        PoseRender.skin = small(ImageIO.read(new File(args[1])));
        PoseRender.slim = Boolean.parseBoolean(args[2]);
        PoseRender.stiff = true;
        String name = args.length > 3 ? args[3] : "villager";
        // Every move villagers make, with the styles it is made in.
        Map<Integer, java.util.Set<Integer>> moves = new LinkedHashMap<>();
        for (HumanPersona.Temper temper : HumanPersona.Temper.values()) {
            int style = HumanGestures.style(temper);
            for (int seed = 0; seed < 12; seed++) {
                for (int code : new int[] {HumanGestures.hello(temper, false, seed), HumanGestures.hello(temper, true, seed), HumanGestures.chat(temper, seed),
                    HumanGestures.about(temper, seed), HumanGestures.news(seed), HumanGestures.compliment(temper, seed), HumanGestures.yes(temper),
                    HumanGestures.love(temper, seed), HumanGestures.bye(temper, seed), HumanGestures.greet(seed), HumanGestures.dusk(seed),
                    HumanGestures.idle(temper, seed)}) {
                    moves.computeIfAbsent(code, k -> new java.util.TreeSet<>()).add(style);
                }
            }
        }
        for (HumanPersona.Answer a : HumanPersona.Answer.values()) {
            moves.computeIfAbsent(HumanGestures.confessed(a), k -> new java.util.TreeSet<>()).add(0);
        }
        for (EtoGesture g : new EtoGesture[] {EtoGesture.KISS, EtoGesture.KISSED, EtoGesture.HUG, EtoGesture.HAND}) {
            moves.computeIfAbsent(HumanGestures.of(g), k -> new java.util.TreeSet<>()).addAll(List.of(0, 1, 2));
        }
        List<Shot> shots = new ArrayList<>();
        for (Map.Entry<Integer, java.util.Set<Integer>> e : moves.entrySet()) {
            int code = e.getKey();
            int ticks = Math.max(8, HumanGestures.ticks(code));
            for (int style : e.getValue()) {
                for (double f : new double[] {0.2, 0.5}) {
                    double t = ticks * f;
                    RigPose p;
                    String label;
                    if (HumanGestures.cue(code)) {
                        ChildCue cue = HumanGestures.childCue(code);
                        RigPose raw = ChildTalkPose.pose(cue, t, null, false);
                        p = raw == null ? new RigPose() : raw.scale(cue.weight(t));
                        label = "cue " + cue.name().toLowerCase();
                    } else {
                        EtoGesture g = HumanGestures.gesture(code);
                        p = EtoPose.gesture(g, t, style, 0);
                        label = g.name().toLowerCase() + " " + new String[] {"eto", "ken", "touka"}[style];
                    }
                    shots.add(new Shot(label + " t" + (int)t, p, 25.0));
                }
                if (HumanGestures.cue(code)) {
                    break; // a cue is the same in every style
                }
            }
        }
        sheet(out, name, shots);
    }

    /** An HD skin brought down to vanilla's 64 pixels (PoseRender reads 64), each texel the middle of its block. */
    static BufferedImage small(BufferedImage hd) {
        int k = Math.max(1, hd.getWidth() / 64);
        BufferedImage out = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                out.setRGB(x, y, hd.getRGB(Math.min(hd.getWidth() - 1, x * k + k / 2), Math.min(hd.getHeight() - 1, y * k + k / 2)));
            }
        }
        return out;
    }

    static void sheet(File out, String name, List<Shot> shots) throws Exception {
        int w = 200;
        int h = 260;
        int columns = 8;
        int per = columns * 5;
        for (int s = 0; s * per < shots.size(); s++) {
            List<Shot> part = shots.subList(s * per, Math.min(shots.size(), (s + 1) * per));
            int rows = (part.size() + columns - 1) / columns;
            BufferedImage sheet = new BufferedImage(columns * w, rows * (h + 20), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = sheet.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(new Color(34, 34, 40));
            g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
            for (int i = 0; i < part.size(); i++) {
                Shot shot = part.get(i);
                BufferedImage image = PoseRender.render(shot.pose(), PoseRender.orbit(shot.yaw(), 8.0, 4.2, 0.95), w, h, null);
                int x = i % columns * w;
                int y = i / columns * (h + 20);
                g.drawImage(image, x, y, null);
                g.setColor(Color.WHITE);
                g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
                g.drawString(shot.label(), x + 6, y + h + 14);
            }
            g.dispose();
            ImageIO.write(sheet, "png", new File(out, name + "-moves-" + s + ".png"));
        }
        System.out.println(shots.size() + " shots");
    }
}
