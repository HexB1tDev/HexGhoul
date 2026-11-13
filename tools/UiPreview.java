import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.ukaku.WingForm;
import com.hex.kagune.control.client.UkakuMenuView;
import com.hex.kagune.control.client.KakuganScreenLayout;
import com.hex.kagune.control.client.KakuganView;
import com.hex.kagune.control.client.ui.UiCanvas;
import com.hex.kagune.control.kakugan.KakuganEyes;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;
import javax.imageio.ImageIO;

/**
 * Off-game renders of the new screens (the Ukaku menu and the kakugan screen) from the exact drawing code the game
 * runs (their views draw on a UiCanvas; here the canvas is an image). Only the font is a stand-in.
 * Usage after {@code python3 build.py}: {@code java -cp build/classes:build/base.jar tools/UiPreview.java <out-dir>}
 */
public class UiPreview {
    /** A stand-in skin face (inner layer) and a glasses outer layer, as in the 3D previews. */
    static final int[][] FACE = {
        {0x4A3A30, 0x6E5444, 0x4A3A30, 0x6E5444, 0x6E5444, 0x4A3A30, 0x6E5444, 0x4A3A30},
        {0x56463A, 0x87695A, 0x56463A, 0x87695A, 0x87695A, 0x56463A, 0x87695A, 0x56463A},
        {0x56463A, 0x9A7A64, 0x56463A, 0x93745F, 0x93745F, 0x56463A, 0x9A7A64, 0x56463A},
        {0x56463A, 0x28221A, 0x2E2820, 0xDCBC8C, 0xDCBC8C, 0x2E2820, 0x28221A, 0x56463A},
        {0x2A241C, 0xFAD8FA, 0xA050BE, 0xE6C49A, 0xEEC898, 0xA050BE, 0xFAD8FA, 0x2A241C},
        {0x2E2820, 0xFFFFFF, 0xE070FA, 0xE8C69C, 0xF0CC9C, 0xE070FA, 0xFFFFFF, 0x2E2820},
        {0x4E3E32, 0xE8BC90, 0xEAC496, 0xE6C49A, 0xE6C49A, 0xEAC496, 0xE8BC90, 0x4E3E32},
        {0x4E3E32, 0xE2B48A, 0xE4BC90, 0xE2BC92, 0xE2BC92, 0xE4BC90, 0xE2B48A, 0x4E3E32}};

    static final class ImageCanvas implements UiCanvas {
        final Graphics2D g;
        final boolean glasses;
        final Deque<AffineTransform> stack = new ArrayDeque<>();

        ImageCanvas(Graphics2D g, boolean glasses) {
            this.g = g;
            this.glasses = glasses;
        }

        static Color colour(int argb) {
            return new Color((argb >> 16) & 255, (argb >> 8) & 255, argb & 255, (argb >>> 24) & 255);
        }

        // 4.1.66: items, textures and words from the mod's own assets (the game's own are not here: a plain square, the key).
        static final java.util.Map<String, BufferedImage> IMAGES = new java.util.HashMap<>();
        static java.util.Map<String, String> lang;

        static BufferedImage image(String ns, String path) {
            return IMAGES.computeIfAbsent(ns + ":" + path, k -> {
                File file = new File("src/main/resources/assets/" + ns + "/" + path);
                try {
                    return file.isFile() ? ImageIO.read(file) : null;
                } catch (java.io.IOException failure) {
                    return null;
                }
            });
        }

        public void item(String id, int count, int x, int y, int size) {
            String ns = id.indexOf(':') >= 0 ? id.substring(0, id.indexOf(':')) : "hexkagune";
            String name = id.indexOf(':') >= 0 ? id.substring(id.indexOf(':') + 1) : id;
            BufferedImage icon = image(ns, "textures/item/" + name + ".png");
            if (icon == null) {
                this.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0xFF6A5A50);
                this.fill(x + 2, y + 2, x + size - 2, y + size - 2, 0xFF8A7A6A);
                this.text(name.substring(0, 1).toUpperCase(), x + size / 2 - 3, y + size / 2 - 4, 0xFF2A2018, false);
            } else {
                this.g.drawImage(icon, x, y, x + size, y + size, 0, 0, icon.getWidth(), Math.min(icon.getHeight(), icon.getWidth()), null);
            }
            if (count > 1) {
                this.text(String.valueOf(count), x + size - this.width(String.valueOf(count)), y + size - 8, 0xFFFFFFFF, true);
            }
        }

        public void texture(String id, int x, int y, int w, int h, int u, int v, int uw, int vh, int texW, int texH) {
            int colon = id.indexOf(':');
            BufferedImage sheet = colon < 0 ? null : image(id.substring(0, colon), id.substring(colon + 1));
            if (sheet == null) {
                this.fill(x, y, x + w, y + h, 0xFF5A4A44);
                return;
            }
            float sx = sheet.getWidth() / (float)texW;
            float sy = sheet.getHeight() / (float)texH;
            this.g.drawImage(sheet, x, y, x + w, y + h, Math.round(u * sx), Math.round(v * sy), Math.round((u + uw) * sx), Math.round((v + vh) * sy), null);
        }

        public String tr(String key, Object... args) {
            if (lang == null) {
                lang = new java.util.HashMap<>();
                try {
                    String json = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/assets/hexkagune/lang/en_us.json"));
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(json);
                    while (m.find()) {
                        lang.put(m.group(1), m.group(2).replace("\\\"", "\"").replace("\\n", "\n").replace("\\u00a7", "\u00a7"));
                    }
                } catch (java.io.IOException failure) {
                    // no words: the keys
                }
            }
            String text = lang.get(key);
            if (text == null) {
                String last = key.substring(key.lastIndexOf('.') + 1).replace('_', ' ');
                return last.isEmpty() ? key : Character.toUpperCase(last.charAt(0)) + last.substring(1);
            }
            try {
                return args.length == 0 ? text : String.format(text, args);
            } catch (java.util.IllegalFormatException wrong) {
                return text;
            }
        }

        public void fill(int x0, int y0, int x1, int y1, int argb) {
            this.g.setColor(colour(argb));
            this.g.fillRect(Math.min(x0, x1), Math.min(y0, y1), Math.abs(x1 - x0), Math.abs(y1 - y0));
        }

        public void gradient(int x0, int y0, int x1, int y1, int top, int bottom) {
            for (int y = y0; y < y1; y++) {
                float t = (y - y0) / (float)Math.max(1, y1 - y0 - 1);
                int a = (int)(((top >>> 24) & 255) * (1 - t) + ((bottom >>> 24) & 255) * t);
                int r = (int)(((top >> 16) & 255) * (1 - t) + ((bottom >> 16) & 255) * t);
                int gr = (int)(((top >> 8) & 255) * (1 - t) + ((bottom >> 8) & 255) * t);
                int b = (int)((top & 255) * (1 - t) + (bottom & 255) * t);
                this.fill(x0, y, x1, y + 1, a << 24 | r << 16 | gr << 8 | b);
            }
        }

        public void text(String text, int x, int y, int argb, boolean shadow) {
            if (shadow) {
                this.g.setColor(colour(argb & 0xFF000000 | (argb & 0xFCFCFC) >> 2));
                this.g.drawString(text, x + 1, y + 8);
            }
            this.g.setColor(colour(argb));
            this.g.drawString(text, x, y + 7);
        }

        public void centered(String text, int centerX, int y, int argb) {
            this.text(text, centerX - this.width(text) / 2, y, argb, true);
        }

        public int width(String text) {
            return this.g.getFontMetrics().stringWidth(text);
        }

        public void face(int x, int y, int size) {
            int cell = size / 8;
            if (SKIN != null) {
                // 7.0.2: a real skin's face (u 8..16, v 8..16). 4.1.55: of a skin of any size (a villager's are 512 across), as the game
                // draws it by its share of the texture.
                int k = Math.max(1, SKIN.getWidth() / 64);
                for (int v = 0; v < 8; v++) {
                    for (int u = 0; u < 8; u++) {
                        this.fill(x + u * cell, y + v * cell, x + (u + 1) * cell, y + (v + 1) * cell, 0xFF000000 | SKIN.getRGB((8 + u) * k + k / 2,
                            (8 + v) * k + k / 2));
                    }
                }
                return;
            }
            for (int v = 0; v < 8; v++) {
                for (int u = 0; u < 8; u++) {
                    this.fill(x + u * cell, y + v * cell, x + (u + 1) * cell, y + (v + 1) * cell, 0xFF000000 | FACE[v][u]);
                }
            }
        }

        public void hat(int x, int y, int size, int[] hole) {
            if (SKIN != null) {
                int cell = size / 8;
                int k = Math.max(1, SKIN.getWidth() / 64); // 4.1.55: a skin of any size
                for (int v = 0; v < 8; v++) {
                    for (int u = 0; u < 8; u++) {
                        int argb = SKIN.getRGB((40 + u) * k + k / 2, (8 + v) * k + k / 2);
                        if ((argb >>> 24) >= 128) {
                            this.fill(x + u * cell, y + v * cell, x + (u + 1) * cell, y + (v + 1) * cell, 0xFF000000 | argb);
                        }
                    }
                }
                return;
            }
            if (!this.glasses) {
                return;
            }
            int cell = size / 8;
            for (int v = 3; v <= 5; v++) {
                for (int u = 0; u < 8; u++) {
                    boolean frame = v == 3 || u == 0 || u == 7 || (v == 4 && (u == 3 || u == 4));
                    boolean lens = v >= 4 && (u == 1 || u == 2 || u == 5 || u == 6);
                    if (!frame && !lens || hole != null && u >= hole[0] && u < hole[2] && v >= hole[1] && v < hole[3]) {
                        continue;
                    }
                    this.fill(x + u * cell, y + v * cell, x + (u + 1) * cell, y + (v + 1) * cell, frame ? 0xFF101010 : 0xFFB4B8BC);
                }
            }
        }

        public void push() {
            this.stack.push(this.g.getTransform());
        }

        public void translate(float x, float y) {
            this.g.translate(x, y);
        }

        public void scale(float factor) {
            this.g.scale(factor, factor);
        }

        public void pop() {
            this.g.setTransform(this.stack.pop());
        }

        public void batch(Runnable draw) {
            draw.run();
        }
    }

    static Graphics2D graphics(BufferedImage image, int scale) {
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.scale(scale, scale);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 8));
        return g;
    }

    /** The Ukaku menu as it fits a 1080p window at GUI scale 2 (960 x 540), at a given mastery. */
    static void menu(File out, int mastery, WingForm form, double mouseX, double mouseY) throws Exception {
        menu(out, mastery, form, mouseX, mouseY, UkakuMenuView.Tab.ORGAN, null, true, com.hex.kagune.control.client.QuestLocks.ALL);
    }

    /**
     * 4.1.56: the Ukaku menu as the game draws it at 1080p, GUI scale 2 (960 x 540, at 2x): the quest menu's box over a stand-in daylit
     * scene, at {@code mastery}, {@code form} out (or in), the tab and pick given, the mouse at ({@code mouseX}, {@code mouseY}) in the box.
     */
    static void menu(File out, int mastery, WingForm form, double mouseX, double mouseY, UkakuMenuView.Tab tab, String picked, boolean active,
                     com.hex.kagune.control.client.QuestLocks quests) throws Exception {
        int w = 960;
        int h = 540;
        BufferedImage image = new BufferedImage(w * 2, h * 2, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = graphics(image, 2);
        ImageCanvas c = new ImageCanvas(g, false);
        c.gradient(0, 0, w, h * 6 / 10, 0xFF79A6FF, 0xFFBCD6FF);
        c.fill(0, h * 6 / 10, w, h, 0xFF5C9A3C);
        for (int i = 0; i < w; i += 40) {
            c.fill(i, h * 6 / 10 - 20 - (i * 37 % 60), i + 40, h * 6 / 10, 0xFF4E8834);
        }
        float scale = Math.min(1.0F, Math.min((w - 24.0F) / UkakuMenuView.BOX_WIDTH, (h - 24.0F) / UkakuMenuView.BOX_HEIGHT));
        float ox = (w - UkakuMenuView.BOX_WIDTH * scale) * 0.5F;
        float oy = (h - UkakuMenuView.BOX_HEIGHT * scale) * 0.5F;
        UkakuMenuView.Model model = new UkakuMenuView.Model(mastery, active, form, KakuganEyes.DEFAULT, MUTATIONS, quests, tab, picked);
        double time = Double.parseDouble(System.getProperty("ui.time", "1.3"));
        if (mouseY < 0) {
            // The mouse on the first area whose name starts with "mutation:" (the UNLOCK button): found by drawing once off screen.
            BufferedImage scratch = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            for (java.util.Map.Entry<String, UkakuMenuView.Rect> hit : UkakuMenuView.draw(new ImageCanvas(graphics(scratch, 1), false), model, -1, -1,
                time).entrySet()) {
                if (hit.getKey().startsWith("mutation:")) {
                    mouseX = hit.getValue().x() + hit.getValue().width() * 0.6;
                    mouseY = hit.getValue().y() + hit.getValue().height() * 0.5;
                }
            }
        }
        c.push();
        c.translate(ox, oy);
        c.scale(scale);
        UkakuMenuView.draw(c, model, mouseX, mouseY, time);
        c.pop();
        ImageIO.write(image, "png", out);
        System.out.println("wrote " + out);
    }

    /**
     * 4.1.58: the Rinkaku menu as the game draws it at 1080p, GUI scale 2: the quest menu's box over a stand-in daylit scene; the mouse on
     * the first area whose name starts with {@code over} (null: at {@code mx, my}).
     */
    static void rinkakuMenu(File out, com.hex.kagune.control.client.RinkakuMenuView.Model model, String over, double mx, double my) throws Exception {
        int w = 960;
        int h = 540;
        BufferedImage image = new BufferedImage(w * 2, h * 2, BufferedImage.TYPE_INT_RGB);
        ImageCanvas c = new ImageCanvas(graphics(image, 2), false);
        c.gradient(0, 0, w, h * 6 / 10, 0xFF79A6FF, 0xFFBCD6FF);
        c.fill(0, h * 6 / 10, w, h, 0xFF5C9A3C);
        for (int i = 0; i < w; i += 40) {
            c.fill(i, h * 6 / 10 - 20 - (i * 37 % 60), i + 40, h * 6 / 10, 0xFF4E8834);
        }
        int bw = com.hex.kagune.control.client.RinkakuMenuView.BOX_WIDTH;
        int bh = com.hex.kagune.control.client.RinkakuMenuView.BOX_HEIGHT;
        double time = 1.3;
        if (over != null) {
            BufferedImage scratch = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            for (java.util.Map.Entry<String, UkakuMenuView.Rect> hit : com.hex.kagune.control.client.RinkakuMenuView.draw(new ImageCanvas(graphics(scratch, 1),
                false), model, -1, -1, time).entrySet()) {
                if (hit.getKey().startsWith(over)) {
                    mx = hit.getValue().x() + hit.getValue().width() * 0.6;
                    my = hit.getValue().y() + hit.getValue().height() * 0.5;
                    break;
                }
            }
        }
        c.push();
        c.translate((w - bw) / 2.0F, (h - bh) / 2.0F);
        com.hex.kagune.control.client.RinkakuMenuView.draw(c, model, mx, my, time);
        c.pop();
        ImageIO.write(image, "png", out);
        System.out.println("wrote " + out);
    }

    /** 4.1.60: the Graft menu as the game draws it: the quest menu's box over a stand-in scene; the mouse on the first area named {@code over}. */
    static void graftBox(File out, com.hex.kagune.control.client.GraftMenuView.Model model, String over) throws Exception {
        int w = 960;
        int h = 540;
        BufferedImage image = new BufferedImage(w * 2, h * 2, BufferedImage.TYPE_INT_RGB);
        ImageCanvas c = new ImageCanvas(graphics(image, 2), false);
        c.gradient(0, 0, w, h * 6 / 10, 0xFF79A6FF, 0xFFBCD6FF);
        c.fill(0, h * 6 / 10, w, h, 0xFF5C9A3C);
        for (int i = 0; i < w; i += 40) {
            c.fill(i, h * 6 / 10 - 20 - (i * 37 % 60), i + 40, h * 6 / 10, 0xFF4E8834);
        }
        int bw = com.hex.kagune.control.client.GraftMenuView.BOX_WIDTH;
        int bh = com.hex.kagune.control.client.GraftMenuView.BOX_HEIGHT;
        double mx = -1;
        double my = -1;
        if (over != null) {
            BufferedImage scratch = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            for (java.util.Map.Entry<String, UkakuMenuView.Rect> hit : com.hex.kagune.control.client.GraftMenuView.draw(new ImageCanvas(graphics(scratch, 1),
                false), model, -1, -1, 1.3).entrySet()) {
                if (hit.getKey().startsWith(over)) {
                    mx = hit.getValue().x() + hit.getValue().width() * 0.6;
                    my = hit.getValue().y() + hit.getValue().height() * 0.5;
                    break;
                }
            }
        }
        c.push();
        c.translate((w - bw) / 2.0F, (h - bh) / 2.0F);
        com.hex.kagune.control.client.GraftMenuView.draw(c, model, mx, my, 1.3);
        c.pop();
        ImageIO.write(image, "png", out);
        System.out.println("wrote " + out);
    }

    /** 4.1.60: the Graft menu: a stage to grow into under the mouse, the kakuja, a stage waiting for its quest, the moves, the mutations. */
    static void graftQuest(File dir) throws Exception {
        com.hex.kagune.control.client.QuestLocks all = com.hex.kagune.control.client.QuestLocks.ALL;
        java.util.Set<com.hex.kagune.control.quest.Gate> open = java.util.EnumSet.of(com.hex.kagune.control.quest.Gate.GRAFT_CENTIPEDE);
        com.hex.kagune.control.client.QuestLocks some = new com.hex.kagune.control.client.QuestLocks(open::contains, 1);
        com.hex.kagune.control.graft.GraftStage centipede = com.hex.kagune.control.graft.GraftStage.CENTIPEDE;
        com.hex.kagune.control.graft.GraftStage dragon = com.hex.kagune.control.graft.GraftStage.DRAGON;
        com.hex.kagune.control.client.GraftMenuView.Tab stages = com.hex.kagune.control.client.GraftMenuView.Tab.STAGES;
        com.hex.kagune.control.client.GraftMenuView.Tab moves = com.hex.kagune.control.client.GraftMenuView.Tab.MOVES;
        com.hex.kagune.control.client.GraftMenuView.Tab mutations = com.hex.kagune.control.client.GraftMenuView.Tab.MUTATIONS;
        int revive = com.hex.kagune.control.graft.GraftMutation.NOT_THAT_EASILY.bit();
        graftBox(new File(dir, "graft-stages-grow.png"), new com.hex.kagune.control.client.GraftMenuView.Model(760, true, centipede, false, 64.0F, 100.0F, 0,
            all, stages, "pick:s:dragon"), "stage:");
        graftBox(new File(dir, "graft-stages-kakuja.png"), new com.hex.kagune.control.client.GraftMenuView.Model(950, true, dragon, true, 70.0F, 100.0F, 0,
            all, stages, null), "step:980");
        graftBox(new File(dir, "graft-stages-quest.png"), new com.hex.kagune.control.client.GraftMenuView.Model(760, true, centipede, false, 64.0F, 100.0F, 0,
            some, stages, "pick:s:thorns"), null);
        graftBox(new File(dir, "graft-moves.png"), new com.hex.kagune.control.client.GraftMenuView.Model(600, true, centipede, false, 64.0F, 100.0F, 0, all,
            moves, "pick:m:HUNGRY_MAW"), "pick:s:thorns");
        graftBox(new File(dir, "graft-moves-string.png"), new com.hex.kagune.control.client.GraftMenuView.Model(760, true, dragon, false, 64.0F, 100.0F, 0, some,
            moves, "pick:m:CRESCENT_1"), null);
        graftBox(new File(dir, "graft-moves-shut.png"), new com.hex.kagune.control.client.GraftMenuView.Model(760, true, centipede, false, 64.0F, 100.0F, 0, some,
            moves, "pick:m:CRUCIFIXION"), null);
        graftBox(new File(dir, "graft-mutations.png"), new com.hex.kagune.control.client.GraftMenuView.Model(900, true, dragon, false, 64.0F, 100.0F, revive,
            some, mutations, null), "mutation:");
        // Every stage's preview, in the menu and alone, larger, at the tile's own size in GUI pixels.
        for (com.hex.kagune.control.graft.GraftStage stage : com.hex.kagune.control.graft.GraftStage.values()) {
            graftBox(new File(dir, "graft-stage-" + stage.id() + ".png"), new com.hex.kagune.control.client.GraftMenuView.Model(1000, true,
                stage.kakuja() ? dragon : stage, stage.kakuja(), 80.0F, 100.0F, 0, all, stages, "pick:s:" + stage.id()), null);
        }
        int tw = 190 - 16; // the menu's tile less its frame and its caption
        int th = 150 - 30;
        com.hex.kagune.control.graft.GraftStage[] all5 = com.hex.kagune.control.graft.GraftStage.values();
        String yaws = System.getProperty("graft.yaws", "");
        for (String yawText : yaws.isEmpty() ? new String[0] : yaws.split(",")) {
            double yaw = Double.parseDouble(yawText);
            BufferedImage strip = new BufferedImage((tw + 8) * all5.length * 4, (th + 8) * 4, BufferedImage.TYPE_INT_RGB);
            ImageCanvas c = new ImageCanvas(graphics(strip, 4), false);
            c.fill(0, 0, (tw + 8) * all5.length, th + 8, 0xFF0C080E);
            for (int i = 0; i < all5.length; i++) {
                com.hex.kagune.control.client.PathChoiceLayout.Box box = new com.hex.kagune.control.client.PathChoiceLayout.Box(i * (tw + 8) + 4, 4, tw, th);
                c.fill(box.x() - 1, box.y() - 1, box.x() + tw + 1, box.y() + th + 1, 0xFF3A1A20);
                c.fill(box.x(), box.y(), box.x() + tw, box.y() + th, 0xFF0C080E);
                for (com.hex.kagune.control.client.PathSilhouette.Mark mark : com.hex.kagune.control.client.PathSilhouette.graftForm(box, 0.0, 1.0F,
                    all5[i], 1000, 0x0C080E, yaw)) {
                    UkakuMenuView.draw(c, mark);
                }
            }
            File out = new File(dir, "graft-yaw-" + yawText + ".png");
            ImageIO.write(strip, "png", out);
            System.out.println("wrote " + out);
        }
        for (double time : new double[] {0.0, 2.7}) {
            BufferedImage strip = new BufferedImage((tw + 8) * all5.length * 4, (th + 8) * 2 * 4, BufferedImage.TYPE_INT_RGB);
            ImageCanvas c = new ImageCanvas(graphics(strip, 4), false);
            c.fill(0, 0, (tw + 8) * all5.length, (th + 8) * 2, 0xFF0C080E);
            for (int i = 0; i < all5.length; i++) {
                for (int row = 0; row < 2; row++) {
                    int mastery = row == 0 ? all5[i].mastery() : 1000;
                    com.hex.kagune.control.client.PathChoiceLayout.Box box = new com.hex.kagune.control.client.PathChoiceLayout.Box(i * (tw + 8) + 4, row * (th + 8) + 4, tw, th);
                    c.fill(box.x() - 1, box.y() - 1, box.x() + tw + 1, box.y() + th + 1, 0xFF3A1A20);
                    c.fill(box.x(), box.y(), box.x() + tw, box.y() + th, 0xFF0C080E);
                    for (com.hex.kagune.control.client.PathSilhouette.Mark mark : com.hex.kagune.control.client.PathSilhouette.graftForm(box, time, 1.0F, all5[i], mastery, 0x0C080E)) {
                        UkakuMenuView.draw(c, mark);
                    }
                }
            }
            File out = new File(dir, "graft-forms-" + (time == 0.0 ? "a" : "b") + ".png");
            ImageIO.write(strip, "png", out);
            System.out.println("wrote " + out);
        }
    }

    /** 4.1.61: the Pale menu as the game draws it: the quest menu's box over a stand-in scene; the mouse on the first area named {@code over}. */
    static void paleBox(File out, com.hex.kagune.control.client.PaleMenuView.Model model, String over) throws Exception {
        int w = 960;
        int h = 540;
        BufferedImage image = new BufferedImage(w * 2, h * 2, BufferedImage.TYPE_INT_RGB);
        ImageCanvas c = new ImageCanvas(graphics(image, 2), false);
        c.gradient(0, 0, w, h * 6 / 10, 0xFF79A6FF, 0xFFBCD6FF);
        c.fill(0, h * 6 / 10, w, h, 0xFF5C9A3C);
        for (int i = 0; i < w; i += 40) {
            c.fill(i, h * 6 / 10 - 20 - (i * 37 % 60), i + 40, h * 6 / 10, 0xFF4E8834);
        }
        int bw = com.hex.kagune.control.client.PaleMenuView.BOX_WIDTH;
        int bh = com.hex.kagune.control.client.PaleMenuView.BOX_HEIGHT;
        double mx = -1;
        double my = -1;
        if (over != null) {
            BufferedImage scratch = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            for (java.util.Map.Entry<String, UkakuMenuView.Rect> hit : com.hex.kagune.control.client.PaleMenuView.draw(new ImageCanvas(graphics(scratch, 1),
                false), model, -1, -1, 1.3).entrySet()) {
                if (hit.getKey().startsWith(over)) {
                    mx = hit.getValue().x() + hit.getValue().width() * 0.6;
                    my = hit.getValue().y() + hit.getValue().height() * 0.5;
                    break;
                }
            }
        }
        c.push();
        c.translate((w - bw) / 2.0F, (h - bh) / 2.0F);
        com.hex.kagune.control.client.PaleMenuView.draw(c, model, mx, my, 1.3);
        c.pop();
        ImageIO.write(image, "png", out);
        System.out.println("wrote " + out);
    }

    /** 4.1.61: the Pale menu: the base form, her moves, the Owl, the mutations (one ready to unlock, Witness This's switch). */
    static void paleQuest(File dir) throws Exception {
        KakuganEyes right = KakuganEyes.DEFAULT.withGhoul(KakuganEyes.RIGHT);
        com.hex.kagune.control.client.PaleMenuView.Tab form = com.hex.kagune.control.client.PaleMenuView.Tab.FORM;
        com.hex.kagune.control.client.PaleMenuView.Tab moves = com.hex.kagune.control.client.PaleMenuView.Tab.MOVES;
        com.hex.kagune.control.client.PaleMenuView.Tab owl = com.hex.kagune.control.client.PaleMenuView.Tab.OWL;
        com.hex.kagune.control.client.PaleMenuView.Tab mutations = com.hex.kagune.control.client.PaleMenuView.Tab.MUTATIONS;
        int revive = com.hex.kagune.control.pale.PaleMutation.NOT_THAT_EASILY.bit();
        int witness = com.hex.kagune.control.pale.PaleMutation.WITNESS_THIS.bit() | revive;
        paleBox(new File(dir, "pale-form.png"), new com.hex.kagune.control.client.PaleMenuView.Model(320, true, right, 0, 0L, form, null), "step:350");
        paleBox(new File(dir, "pale-form-ahead.png"), new com.hex.kagune.control.client.PaleMenuView.Model(320, false, right, 0, 0L, form, "pick:s:2"),
            null);
        paleBox(new File(dir, "pale-moves.png"), new com.hex.kagune.control.client.PaleMenuView.Model(420, true, right, 0, 0L, moves, "pick:m:BLITZ"),
            "pick:m:GRAB");
        paleBox(new File(dir, "pale-moves-hammer.png"), new com.hex.kagune.control.client.PaleMenuView.Model(720, true, right, 0, 0L, moves,
            "pick:m:HAMMER"), null);
        paleBox(new File(dir, "pale-owl.png"), new com.hex.kagune.control.client.PaleMenuView.Model(820, true, right, revive, 0L, owl, null), null);
        paleBox(new File(dir, "pale-owl-move.png"), new com.hex.kagune.control.client.PaleMenuView.Model(700, true, right, 0, 0L, owl, "pick:o:WHIRL"),
            null);
        paleBox(new File(dir, "pale-mutations.png"), new com.hex.kagune.control.client.PaleMenuView.Model(910, true, right, revive, 1200L, mutations,
            null), "mutation:");
        paleBox(new File(dir, "pale-mutations-witness.png"), new com.hex.kagune.control.client.PaleMenuView.Model(1000, true, right, witness, 0L,
            mutations, "pick:u:witness_this"), null);
        // The Owl and the base form alone, larger, at the tile's own size in GUI pixels.
        int tw = 190 - 16;
        int th = 150 - 30;
        BufferedImage strip = new BufferedImage((tw + 8) * 3 * 4, (th + 8) * 4, BufferedImage.TYPE_INT_RGB);
        ImageCanvas c = new ImageCanvas(graphics(strip, 4), false);
        c.fill(0, 0, (tw + 8) * 3, th + 8, 0xFF0B090B);
        for (int i = 0; i < 3; i++) {
            com.hex.kagune.control.client.PathChoiceLayout.Box box = new com.hex.kagune.control.client.PathChoiceLayout.Box(i * (tw + 8) + 4, 4, tw, th);
            c.fill(box.x() - 1, box.y() - 1, box.x() + tw + 1, box.y() + th + 1, 0xFF3A1A20);
            c.fill(box.x(), box.y(), box.x() + tw, box.y() + th, 0xFF0B090B);
            java.util.List<com.hex.kagune.control.client.PathSilhouette.Mark> marks = i == 0
                ? com.hex.kagune.control.client.PathSilhouette.pale(box, 1.3, 1.0F, true, 0x0B090B)
                : com.hex.kagune.control.client.PathSilhouette.owl(box, i == 1 ? 0.0 : 2.4, 1.0F, 0x0B090B);
            for (com.hex.kagune.control.client.PathSilhouette.Mark mark : marks) {
                UkakuMenuView.draw(c, mark);
            }
        }
        File out = new File(dir, "pale-owl-tiles.png");
        ImageIO.write(strip, "png", out);
        System.out.println("wrote " + out);
    }

    /** 4.1.59: the Bikaku menu as the game draws it: the quest menu's box over a stand-in scene; the mouse on the first area named {@code over}. */
    static void bikakuBox(File out, com.hex.kagune.control.client.BikakuMenuView.Model model, String over) throws Exception {
        int w = 960;
        int h = 540;
        BufferedImage image = new BufferedImage(w * 2, h * 2, BufferedImage.TYPE_INT_RGB);
        ImageCanvas c = new ImageCanvas(graphics(image, 2), false);
        c.gradient(0, 0, w, h * 6 / 10, 0xFF79A6FF, 0xFFBCD6FF);
        c.fill(0, h * 6 / 10, w, h, 0xFF5C9A3C);
        for (int i = 0; i < w; i += 40) {
            c.fill(i, h * 6 / 10 - 20 - (i * 37 % 60), i + 40, h * 6 / 10, 0xFF4E8834);
        }
        int bw = com.hex.kagune.control.client.BikakuMenuView.BOX_WIDTH;
        int bh = com.hex.kagune.control.client.BikakuMenuView.BOX_HEIGHT;
        double mx = -1;
        double my = -1;
        if (over != null) {
            BufferedImage scratch = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            for (java.util.Map.Entry<String, UkakuMenuView.Rect> hit : com.hex.kagune.control.client.BikakuMenuView.draw(new ImageCanvas(graphics(scratch, 1),
                false), model, -1, -1, 1.3).entrySet()) {
                if (hit.getKey().startsWith(over)) {
                    mx = hit.getValue().x() + hit.getValue().width() * 0.6;
                    my = hit.getValue().y() + hit.getValue().height() * 0.5;
                    break;
                }
            }
        }
        c.push();
        c.translate((w - bw) / 2.0F, (h - bh) / 2.0F);
        com.hex.kagune.control.client.BikakuMenuView.draw(c, model, mx, my, 1.3);
        c.pop();
        ImageIO.write(image, "png", out);
        System.out.println("wrote " + out);
    }

    /** 4.1.59: the Bikaku menu: its forms (one to take under the mouse, The Walker, one waiting for its quest), its moves, its kakuja, its mutations. */
    static void bikakuQuest(File dir) throws Exception {
        com.hex.kagune.control.client.QuestLocks all = com.hex.kagune.control.client.QuestLocks.ALL;
        java.util.Set<com.hex.kagune.control.quest.Gate> open = java.util.EnumSet.of(com.hex.kagune.control.quest.Gate.BIKAKU_WORMS);
        com.hex.kagune.control.client.QuestLocks some = new com.hex.kagune.control.client.QuestLocks(open::contains, 0);
        int potential = com.hex.kagune.control.bikaku.BikakuMutation.POTENTIAL_UNLOCK.bit();
        int devour = com.hex.kagune.control.bikaku.BikakuMutation.GRAND_DEVOURING.bit();
        com.hex.kagune.control.client.BikakuMenuView.Tab forms = com.hex.kagune.control.client.BikakuMenuView.Tab.FORMS;
        com.hex.kagune.control.client.BikakuMenuView.Tab moves = com.hex.kagune.control.client.BikakuMenuView.Tab.MOVES;
        com.hex.kagune.control.client.BikakuMenuView.Tab kakuja = com.hex.kagune.control.client.BikakuMenuView.Tab.KAKUJA;
        com.hex.kagune.control.client.BikakuMenuView.Tab mutations = com.hex.kagune.control.client.BikakuMenuView.Tab.MUTATIONS;
        bikakuBox(new File(dir, "bikaku-forms-take.png"), new com.hex.kagune.control.client.BikakuMenuView.Model(600, true, 64.0F, 100.0F, 0, 0, 0L, all,
            forms, "pick:f:THREE"), "form");
        bikakuBox(new File(dir, "bikaku-forms-walker.png"), new com.hex.kagune.control.client.BikakuMenuView.Model(800, true, 210.0F, 300.0F, 3, potential,
            0L, all, forms, null), "step:1000");
        bikakuBox(new File(dir, "bikaku-forms-quest.png"), new com.hex.kagune.control.client.BikakuMenuView.Model(800, false, 80.0F, 100.0F, 2, 0, 0L, some,
            forms, "pick:f:WALKER"), null);
        bikakuBox(new File(dir, "bikaku-moves.png"), new com.hex.kagune.control.client.BikakuMenuView.Model(500, true, 64.0F, 100.0F, 1, 0, 0L, all, moves,
            "pick:m:ERUPTION"), "step:600");
        bikakuBox(new File(dir, "bikaku-kakuja.png"), new com.hex.kagune.control.client.BikakuMenuView.Model(1000, true, 64.0F, 100.0F, 2, 0, 0L, all,
            kakuja, null), null);
        bikakuBox(new File(dir, "bikaku-mutations.png"), new com.hex.kagune.control.client.BikakuMenuView.Model(700, true, 64.0F, 100.0F, 1, devour, 380L,
            all, mutations, null), null);
        bikakuBox(new File(dir, "bikaku-mutations-quest.png"), new com.hex.kagune.control.client.BikakuMenuView.Model(700, true, 64.0F, 100.0F, 1, devour,
            0L, all, mutations, "pick:u:POTENTIAL_UNLOCK"), null);
    }

    /** 4.1.58: the Rinkaku menu: its forms (one to wear under the mouse, a road step, one ahead), its passive, the final kakuja's states. */
    static void rinkaku(File dir) throws Exception {
        com.hex.kagune.gameplay.KaguneForm quad = com.hex.kagune.gameplay.KaguneForm.RINKAKU_QUAD;
        com.hex.kagune.gameplay.KaguneForm centipede = com.hex.kagune.gameplay.KaguneForm.CENTIPEDE;
        com.hex.kagune.gameplay.KaguneForm full = com.hex.kagune.gameplay.KaguneForm.FULL_KAKUJA;
        com.hex.kagune.control.client.QuestLocks all = com.hex.kagune.control.client.QuestLocks.ALL;
        java.util.Set<com.hex.kagune.control.quest.Gate> open = java.util.EnumSet.noneOf(com.hex.kagune.control.quest.Gate.class);
        com.hex.kagune.control.client.QuestLocks none = new com.hex.kagune.control.client.QuestLocks(open::contains, 0);
        com.hex.kagune.control.king.KingForm no = com.hex.kagune.control.king.KingForm.NONE;
        com.hex.kagune.control.king.KingForm war = com.hex.kagune.control.king.KingForm.WAR;
        com.hex.kagune.control.king.KingForm peace = com.hex.kagune.control.king.KingForm.PEACE;
        com.hex.kagune.control.client.RinkakuMenuView.Tab forms = com.hex.kagune.control.client.RinkakuMenuView.Tab.FORMS;
        com.hex.kagune.control.client.RinkakuMenuView.Tab king = com.hex.kagune.control.client.RinkakuMenuView.Tab.KING;
        com.hex.kagune.control.client.RinkakuMenuView.King asleep = com.hex.kagune.control.client.RinkakuMenuView.King.ASLEEP;
        com.hex.kagune.control.client.RinkakuMenuView.King awake = new com.hex.kagune.control.client.RinkakuMenuView.King(true, no, no, no);
        rinkakuMenu(new File(dir, "rinkaku-forms-wear.png"), new com.hex.kagune.control.client.RinkakuMenuView.Model(600, true, quad, 64.0F, 100.0F, 0L,
            null, all, asleep, forms, "pick:dual"), "form:", 0, 0);
        rinkakuMenu(new File(dir, "rinkaku-forms-worn.png"), new com.hex.kagune.control.client.RinkakuMenuView.Model(800, true, centipede, 82.0F, 100.0F,
            0L, null, all, asleep, forms, null), "step:1000", 0, 0);
        rinkakuMenu(new File(dir, "rinkaku-forms-ahead.png"), new com.hex.kagune.control.client.RinkakuMenuView.Model(300, false,
            com.hex.kagune.gameplay.KaguneForm.RINKAKU_DUAL, 100.0F, 100.0F, 0L, null, none, asleep, forms, "pick:full"), "step:350", 0, 0);
        rinkakuMenu(new File(dir, "rinkaku-passive.png"), new com.hex.kagune.control.client.RinkakuMenuView.Model(600, true, quad, 64.0F, 100.0F, 0L,
            null, none, asleep, forms, "pick:passive"), null, 0, 0);
        rinkakuMenu(new File(dir, "rinkaku-king-choose.png"), new com.hex.kagune.control.client.RinkakuMenuView.Model(1000, true, full, 70.0F, 100.0F,
            0L, null, all, awake, king, null), "king:", 0, 0);
        rinkakuMenu(new File(dir, "rinkaku-king-dragon.png"), new com.hex.kagune.control.client.RinkakuMenuView.Model(1000, true, full, 70.0F, 100.0F,
            0L, null, all, asleep, king, "pick:peace"), null, 0, 0);
        rinkakuMenu(new File(dir, "rinkaku-king-war.png"), new com.hex.kagune.control.client.RinkakuMenuView.Model(1000, true, full, 70.0F, 100.0F, 0L,
            null, all, new com.hex.kagune.control.client.RinkakuMenuView.King(true, war, war, no), king, null), null, 0, 0);
        rinkakuMenu(new File(dir, "rinkaku-king-peace.png"), new com.hex.kagune.control.client.RinkakuMenuView.Model(1000, true, full, 70.0F, 100.0F, 0L,
            null, all, new com.hex.kagune.control.client.RinkakuMenuView.King(true, peace, no, no), forms, null), null, 0, 0);
    }

    /**
     * 4.1.56: the Ukaku menu in the quest menu's style: the organ's tab at a few masteries (a stage picked, a step of the road under the
     * mouse), a stage ahead picked, the mutations' tab (one ready under the mouse, one waiting for its quest, one had).
     */
    static void ukaku(File dir) throws Exception {
        UkakuMenuView.Tab organ = UkakuMenuView.Tab.ORGAN;
        UkakuMenuView.Tab mutations = UkakuMenuView.Tab.MUTATIONS;
        com.hex.kagune.control.client.QuestLocks all = com.hex.kagune.control.client.QuestLocks.ALL;
        MUTATIONS = 0;
        menu(new File(dir, "ukaku-organ-0.png"), 0, WingForm.RABBIT, 0, 0, organ, null, false, all);
        menu(new File(dir, "ukaku-organ-300.png"), 300, WingForm.TWIN, 107, 116, organ, null, true, all);
        menu(new File(dir, "ukaku-organ-760.png"), 760, WingForm.KAKUJA, 244 + 10 + 33 * 10, 405, organ, null, true, all);
        menu(new File(dir, "ukaku-organ-ahead.png"), 300, WingForm.TWIN, 0, 0, organ, "stage:KAKUJA_I", true, all);
        menu(new File(dir, "ukaku-organ-1000.png"), 1000, WingForm.FINAL_KAKUJA, 0, 0, organ, "stage:KAKUJA_II", true, all);
        MUTATIONS = com.hex.kagune.control.ukaku.UkakuMutation.FIRE_DASH.bit();
        java.util.Set<com.hex.kagune.control.quest.Gate> open = java.util.EnumSet.noneOf(com.hex.kagune.control.quest.Gate.class);
        com.hex.kagune.control.client.QuestLocks quests = new com.hex.kagune.control.client.QuestLocks(open::contains, 1);
        menu(new File(dir, "ukaku-mutations-ready.png"), 760, WingForm.TWIN, 330, -1, mutations, null, true, quests);
        menu(new File(dir, "ukaku-mutations-quest.png"), 760, WingForm.TWIN, 100, 230, mutations, "pick:4", true, quests);
        menu(new File(dir, "ukaku-mutations-owned.png"), 760, WingForm.TWIN, 0, 0, mutations, "pick:1", true, quests);
        MUTATIONS = com.hex.kagune.control.ukaku.UkakuMutation.all();
        menu(new File(dir, "ukaku-mutations-all.png"), 1000, WingForm.TWIN, 0, 0, mutations, null, false, all);
        MUTATIONS = 0;
    }

    /** 6.1.0: the Pale menu (960 x 540 at 2x), at {@code mastery}, her base form out or in, the mouse at ({@code mouseX}, {@code mouseY}). */
    static void paleMenu(File out, int mastery, boolean active, double mouseX, double mouseY) throws Exception {
        int w = 960;
        int h = 540;
        BufferedImage image = new BufferedImage(w * 2, h * 2, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = graphics(image, 2);
        ImageCanvas c = new ImageCanvas(g, false);
        c.gradient(0, 0, w, h, 0xE8080709, 0xF0120E10);
        float scale = Math.min(1.0F, Math.min((w - 12.0F) / com.hex.kagune.control.client.PaleMenuView.UI_WIDTH,
            (h - 12.0F) / com.hex.kagune.control.client.PaleMenuView.UI_HEIGHT));
        float ox = (w - com.hex.kagune.control.client.PaleMenuView.UI_WIDTH * scale) * 0.5F;
        float oy = (h - com.hex.kagune.control.client.PaleMenuView.UI_HEIGHT * scale) * 0.5F;
        c.push();
        c.translate(ox, oy);
        c.scale(scale);
        com.hex.kagune.control.client.PaleMenuView.draw(c, new com.hex.kagune.control.client.PaleMenuView.Model(mastery, active,
            KakuganEyes.DEFAULT.withGhoul(KakuganEyes.RIGHT), PALE_MUTATIONS, PALE_REVIVE_IN), mouseX, mouseY, 1.3);
        c.pop();
        ImageIO.write(image, "png", out);
        System.out.println("wrote " + out);
    }

    /** 3.4.9: the Bikaku menu (960 x 540 at 2x), at {@code mastery}, the mouse at ({@code mouseX}, {@code mouseY}). */
    static void bikakuMenu(File out, int mastery, double mouseX, double mouseY) throws Exception {
        bikakuMenu(out, mastery, 0, 0L, mouseX, mouseY);
    }

    /** 3.6.3: as above, with {@code mutations} (BikakuMutation bits) and Grand Devouring's wait ({@code devourWait} ticks). */
    static void bikakuMenu(File out, int mastery, int mutations, long devourWait, double mouseX, double mouseY) throws Exception {
        int w = 960;
        int h = 540;
        BufferedImage image = new BufferedImage(w * 2, h * 2, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = graphics(image, 2);
        ImageCanvas c = new ImageCanvas(g, false);
        c.gradient(0, 0, w, h, 0xE8080306, 0xF0180610);
        float scale = Math.min(1.0F, Math.min((w - 12.0F) / com.hex.kagune.control.client.BikakuMenuView.UI_WIDTH,
            (h - 12.0F) / com.hex.kagune.control.client.BikakuMenuView.UI_HEIGHT));
        float ox = (w - com.hex.kagune.control.client.BikakuMenuView.UI_WIDTH * scale) * 0.5F;
        float oy = (h - com.hex.kagune.control.client.BikakuMenuView.UI_HEIGHT * scale) * 0.5F;
        c.push();
        c.translate(ox, oy);
        c.scale(scale);
        int capacity = com.hex.kagune.control.bikaku.BikakuRules.capacity(mutations); // 3.6.7: Potential Unlock reads three times the energy
        com.hex.kagune.control.client.BikakuMenuView.draw(c, new com.hex.kagune.control.client.BikakuMenuView.Model(mastery, true, 64.0F * capacity,
            100.0F * capacity, mastery >= 750 ? 3 : mastery >= 500 ? 2 : 0, mutations, devourWait), mouseX, mouseY, 1.3);
        c.pop();
        ImageIO.write(image, "png", out);
        System.out.println("wrote " + out);
    }

    /** 7.0.0: the Graft DNA menu (960 x 540 at 2x), at {@code mastery} with {@code mutations} taken, the mouse at ({@code mouseX}, {@code mouseY}). */
    static void graftMenu(File out, int mastery, int mutations, double mouseX, double mouseY) throws Exception {
        int w = 960;
        int h = 540;
        BufferedImage image = new BufferedImage(w * 2, h * 2, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = graphics(image, 2);
        ImageCanvas c = new ImageCanvas(g, false);
        c.gradient(0, 0, w, h, 0xE8080506, 0xF0180608);
        float scale = Math.min(1.0F, Math.min((w - 12.0F) / com.hex.kagune.control.client.GraftMenuView.UI_WIDTH,
            (h - 12.0F) / com.hex.kagune.control.client.GraftMenuView.UI_HEIGHT));
        float ox = (w - com.hex.kagune.control.client.GraftMenuView.UI_WIDTH * scale) * 0.5F;
        float oy = (h - com.hex.kagune.control.client.GraftMenuView.UI_HEIGHT * scale) * 0.5F;
        c.push();
        c.translate(ox, oy);
        c.scale(scale);
        com.hex.kagune.control.client.GraftMenuView.draw(c, new com.hex.kagune.control.client.GraftMenuView.Model(mastery, true,
            com.hex.kagune.control.graft.GraftStage.highest(mastery), false, 64.0F, 100.0F, mutations), mouseX, mouseY, 1.3);
        c.pop();
        ImageIO.write(image, "png", out);
        System.out.println("wrote " + out);
    }

    /**
     * 7.0.1: the quest journal in its states, from the exact view the game draws: a new Rinkaku ghoul's path (one quest under way, the
     * rest waiting for mastery, what is still locked), the rival ways once one is taken, a hunt on tonight, the ghoul's way, and the
     * Graft menu with stages waiting for their quests.
     */
    static void quests(File dir) throws Exception {
        com.hex.kagune.control.quest.QuestLog fresh = new com.hex.kagune.control.quest.QuestLog();
        fresh.migrate(gate -> false);
        for (int i = 0; i < 9; i++) {
            fresh.apply(com.hex.kagune.control.quest.QuestEvent.of(com.hex.kagune.control.quest.Signal.KILL, 1, com.hex.kagune.control.quest.Tags.KAGUNE,
                com.hex.kagune.control.quest.Tags.HOSTILE), KagunePath.RINKAKU, 300);
        }
        for (int i = 0; i < 2; i++) {
            fresh.apply(com.hex.kagune.control.quest.QuestEvent.of(com.hex.kagune.control.quest.Signal.RESTRAIN, 1), KagunePath.RINKAKU, 300);
        }
        journal(new File(dir, "quests-rinkaku-path.png"), KagunePath.RINKAKU, 300, fresh, com.hex.kagune.control.client.QuestJournalView.Tab.PATH,
            "rizes_gift", 0L);
        com.hex.kagune.control.quest.QuestLog chose = com.hex.kagune.control.quest.QuestLog.load(fresh.save());
        chose.complete(com.hex.kagune.control.quest.QuestBook.get("rizes_gift"));
        chose.complete(com.hex.kagune.control.quest.QuestBook.get("pack"));
        journal(new File(dir, "quests-rinkaku-done.png"), KagunePath.RINKAKU, 520, chose, com.hex.kagune.control.client.QuestJournalView.Tab.DONE,
            "anteiku", 0L);
        com.hex.kagune.control.quest.QuestLog hunting = com.hex.kagune.control.quest.QuestLog.load(chose.save());
        com.hex.kagune.control.quest.Hunts.Spec spec = new com.hex.kagune.control.quest.Hunts.Spec(0, 4, 3, com.hex.kagune.control.quest.Hunts.PAY_MEAT, 1);
        hunting.startHunt(spec, 9000L);
        hunting.apply(com.hex.kagune.control.quest.QuestEvent.of(com.hex.kagune.control.quest.Signal.KILL, 1,
            com.hex.kagune.control.quest.Tags.mob("minecraft:witch")), KagunePath.RINKAKU, 520);
        journal(new File(dir, "quests-hunt.png"), KagunePath.RINKAKU, 520, hunting, com.hex.kagune.control.client.QuestJournalView.Tab.HUNTS, null, 1800L);
        com.hex.kagune.control.quest.QuestLog ghoul = new com.hex.kagune.control.quest.QuestLog();
        // 1.3.29: the ghoul's way holds only what was given: Eto's first lesson (asked of her) and the ghouls' first (one talked to).
        ghoul.bond(com.hex.kagune.control.npc.EtoRules.KEY, com.hex.kagune.control.quest.QuestLog.Bond.NONE.with(com.hex.kagune.control.npc.EtoRules.TAUGHT_KICK, true));
        ghoul.bond("a-ghoul", com.hex.kagune.control.quest.QuestLog.Bond.NONE.with(com.hex.kagune.control.npc.NpcBonds.MET, true));
        for (int i = 0; i < 37; i++) {
            ghoul.apply(com.hex.kagune.control.quest.QuestEvent.of(com.hex.kagune.control.quest.Signal.STRIKE, 1), KagunePath.GRAFT, 400);
        }
        for (int i = 0; i < 15; i++) {
            ghoul.apply(com.hex.kagune.control.quest.QuestEvent.of(com.hex.kagune.control.quest.Signal.FLIP, 1), KagunePath.GRAFT, 400);
        }
        journal(new File(dir, "quests-ghoul.png"), KagunePath.GRAFT, 400, ghoul, com.hex.kagune.control.client.QuestJournalView.Tab.GHOUL, "eto_kick",
            0L);
        // 1.3.29: the Romance tab: married to Eto (home set, a child), Touka courting (2.1.2: chapter two, five of eight), Ken not met.
        java.util.List<com.hex.kagune.control.npc.RomanceCard> cards = java.util.List.of(
            new com.hex.kagune.control.npc.RomanceCard("eto", true, com.hex.kagune.control.npc.Romance.Stage.MARRIED, 96, true, null,
                com.hex.kagune.control.npc.EtoRules.Step.DATING, 0, 12L, 1, true, false),
            new com.hex.kagune.control.npc.RomanceCard("touka", true, com.hex.kagune.control.npc.Romance.Stage.NONE, 74, true, null,
                com.hex.kagune.control.npc.EtoRules.Step.COURTING, 5, 0L, 0, false, false, 1),
            new com.hex.kagune.control.npc.RomanceCard("ken", false, com.hex.kagune.control.npc.Romance.Stage.NONE, 0, true, null,
                com.hex.kagune.control.npc.EtoRules.Step.NONE, 0, 0L, 0, false, false));
        journal(new File(dir, "quests-romance.png"), KagunePath.RINKAKU, 520, chose, com.hex.kagune.control.client.QuestJournalView.Tab.ROMANCE, "romance:eto",
            0L, cards);
        journal(new File(dir, "quests-romance-touka.png"), KagunePath.RINKAKU, 520, chose, com.hex.kagune.control.client.QuestJournalView.Tab.ROMANCE,
            "romance:touka", 0L, cards);
        // The Graft menu at 760 mastery with only the transplant's and the centipede's quests done: thorns and the many-eyed wait.
        java.util.Set<com.hex.kagune.control.quest.Gate> open = java.util.EnumSet.of(com.hex.kagune.control.quest.Gate.GRAFT_CENTIPEDE);
        File out = new File(dir, "quests-graft-menu.png");
        int w = 960;
        int h = 540;
        BufferedImage image = new BufferedImage(w * 2, h * 2, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = graphics(image, 2);
        ImageCanvas c = new ImageCanvas(g, false);
        c.gradient(0, 0, w, h, 0xE8080506, 0xF0180608);
        c.push();
        c.translate(0, 10);
        com.hex.kagune.control.client.GraftMenuView.draw(c, new com.hex.kagune.control.client.GraftMenuView.Model(760, true,
            com.hex.kagune.control.graft.GraftStage.CENTIPEDE, false, 64.0F, 100.0F, 0, new com.hex.kagune.control.client.QuestLocks(open::contains, 1)), 0, 0,
            1.3);
        c.pop();
        ImageIO.write(image, "png", out);
        System.out.println("wrote " + out);
    }

    static void journal(File out, KagunePath path, int mastery, com.hex.kagune.control.quest.QuestLog log, com.hex.kagune.control.client.QuestJournalView.Tab tab,
                        String picked, long time) throws Exception {
        journal(out, path, mastery, log, tab, picked, time, java.util.List.of());
    }

    static void journal(File out, KagunePath path, int mastery, com.hex.kagune.control.quest.QuestLog log, com.hex.kagune.control.client.QuestJournalView.Tab tab,
                        String picked, long time, java.util.List<com.hex.kagune.control.npc.RomanceCard> cards) throws Exception {
        int w = 960;
        int h = 540;
        BufferedImage image = new BufferedImage(w * 2, h * 2, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = graphics(image, 2);
        ImageCanvas c = new ImageCanvas(g, false);
        // 4.1.34: the journal is a box over the world: a stand-in daylit scene behind it (sky, hills, grass).
        c.gradient(0, 0, w, h * 6 / 10, 0xFF79A6FF, 0xFFBCD6FF);
        c.fill(0, h * 6 / 10, w, h, 0xFF5C9A3C);
        for (int i = 0; i < w; i += 40) {
            c.fill(i, h * 6 / 10 - 20 - (i * 37 % 60), i + 40, h * 6 / 10, 0xFF4E8834);
        }
        c.push();
        c.translate((w - com.hex.kagune.control.client.QuestJournalView.UI_WIDTH) / 2.0F, (h - com.hex.kagune.control.client.QuestJournalView.UI_HEIGHT) / 2.0F);
        com.hex.kagune.control.client.QuestJournalView.draw(c, new com.hex.kagune.control.client.QuestJournalView.Model(path, mastery, log, time, tab, picked,
            cards), Double.parseDouble(System.getProperty("mx", "0")), Double.parseDouble(System.getProperty("my", "0")), 1.0);
        c.pop();
        ImageIO.write(image, "png", out);
        System.out.println("wrote " + out);
    }

    /** 7.0.2: the skin whose face the next canvas draws (a ghoul NPC's), or null for the stand-in face. */
    static BufferedImage SKIN;

    /**
     * 7.0.2: talking with ghoul NPCs, as the talk screen draws it over the world at 1080p, GUI scale 2 (960 x 540): over a scene of
     * the ghoul if one is given ({@code -Dtalk.scene}, a 960 x 540 render: tools/graft-preview.sh npc-scene), and its face from its
     * skin ({@code -Dtalk.skins}, vanilla's wide skins: build/skins).
     */
    static void talk(File dir) throws Exception {
        java.util.List<com.hex.kagune.control.npc.NpcTalk.Choice> all = java.util.List.of(com.hex.kagune.control.npc.NpcTalk.Choice.WHO,
            com.hex.kagune.control.npc.NpcTalk.Choice.CHAT, com.hex.kagune.control.npc.NpcTalk.Choice.GIFT, com.hex.kagune.control.npc.NpcTalk.Choice.FAVOUR,
            com.hex.kagune.control.npc.NpcTalk.Choice.INVITE, com.hex.kagune.control.npc.NpcTalk.Choice.INSULT, com.hex.kagune.control.npc.NpcTalk.Choice.LEAVE);
        com.hex.kagune.control.npc.NpcIdentity ren = new com.hex.kagune.control.npc.NpcIdentity(com.hex.kagune.control.npc.NpcIdentity.Kind.RINKAKU,
            com.hex.kagune.control.npc.NpcTemper.DRIFTER, 1, 4, 36, 1);
        talkPage(new File(dir, "npc-talk-greeting.png"), "kai", new com.hex.kagune.control.client.NpcTalkView.Model(ren.displayName(), "Drifter", 30, false,
            "You again. The night's quieter with company.", all), 300, 470);
        com.hex.kagune.control.npc.NpcIdentity pack = new com.hex.kagune.control.npc.NpcIdentity(com.hex.kagune.control.npc.NpcIdentity.Kind.UKAKU,
            com.hex.kagune.control.npc.NpcTemper.PACK, 2, 6, 20, 77);
        com.hex.kagune.control.quest.Favours.Spec spec = com.hex.kagune.control.quest.Favours.offer(pack, 0);
        talkPage(new File(dir, "npc-talk-favour.png"), "noor", new com.hex.kagune.control.client.NpcTalkView.Model(pack.displayName(), "Pack", 0, false,
            "Prove you're worth something. " + com.hex.kagune.control.quest.Favours.asking(spec, pack), com.hex.kagune.control.npc.NpcTalk.offer()), 0, 0);
        com.hex.kagune.control.npc.NpcIdentity mio = new com.hex.kagune.control.npc.NpcIdentity(com.hex.kagune.control.npc.NpcIdentity.Kind.UKAKU,
            com.hex.kagune.control.npc.NpcTemper.ANTEIKU, 1, 1, 30, 5);
        talkPage(new File(dir, "npc-talk-ally.png"), "alex", new com.hex.kagune.control.client.NpcTalkView.Model(mio.displayName(), "Anteiku", 85, true,
            "Still here. Where are we going?", java.util.List.of(com.hex.kagune.control.npc.NpcTalk.Choice.WHO, com.hex.kagune.control.npc.NpcTalk.Choice.CHAT,
                com.hex.kagune.control.npc.NpcTalk.Choice.FAVOUR, com.hex.kagune.control.npc.NpcTalk.Choice.STAY, com.hex.kagune.control.npc.NpcTalk.Choice.PART,
                com.hex.kagune.control.npc.NpcTalk.Choice.LEAVE)), 0, 0);
    }

    /**
     * 1.3.3: Eto's talk screen as it moves: her words half typed (the meter's heart gliding, what changed floating up), then all said
     * with the answers coming in, then all in place. Her face from her own skin (128 pixels, read at half size).
     */
    static void etoTalk(File dir) throws Exception {
        BufferedImage big = ImageIO.read(new File("src/main/resources/assets/hexkagune/textures/entity/eto/eto.png"));
        BufferedImage half = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        half.getGraphics().drawImage(big, 0, 0, 64, 64, null);
        java.util.List<com.hex.kagune.control.npc.NpcTalk.Choice> choices = java.util.List.of(com.hex.kagune.control.npc.NpcTalk.Choice.CHAT,
            com.hex.kagune.control.npc.NpcTalk.Choice.SWEET, com.hex.kagune.control.npc.NpcTalk.Choice.BOOK, com.hex.kagune.control.npc.NpcTalk.Choice.LESSON,
            com.hex.kagune.control.npc.NpcTalk.Choice.STAY, com.hex.kagune.control.npc.NpcTalk.Choice.TEASE, com.hex.kagune.control.npc.NpcTalk.Choice.LEAVE);
        java.util.List<String> labels = java.util.List.of("Talk to me.", "Be sweet to her.", "I brought you a book. (give the book in your hand)",
            "Teach me something.", "Wait here.", "Tease her.", "Leave.");
        String line = "Stop looking at me like that. ...No, keep doing it. You know what, come here. Closer. I have a rule about you being more than an "
            + "arm away.";
        com.hex.kagune.control.client.NpcTalkView.Model model = new com.hex.kagune.control.client.NpcTalkView.Model("Eto", "Your date - giddy", 86, false,
            line, choices, labels);
        double done = com.hex.kagune.control.client.NpcTalkView.duration(line);
        etoFrame(new File(dir, "eto-talk-1-typing.png"), half, model, new com.hex.kagune.control.client.NpcTalkView.Motion(2.0, done * 0.45, 78.0F, 3, 0.35));
        etoFrame(new File(dir, "eto-talk-2-answers.png"), half, model, new com.hex.kagune.control.client.NpcTalkView.Motion(4.0, done + 0.16, 86.0F, 3, 1.1));
        etoFrame(new File(dir, "eto-talk-3-still.png"), half, model, com.hex.kagune.control.client.NpcTalkView.Motion.still(86));
        com.hex.kagune.control.client.NpcTalkView.Model cold = new com.hex.kagune.control.client.NpcTalkView.Model("Eto", "Taken - cold", 12, false,
            "Mm? Oh. You. If it's lessons, fine. If it's anything else, I'm Steve's. Don't get funny ideas.",
            java.util.List.of(com.hex.kagune.control.npc.NpcTalk.Choice.CHAT, com.hex.kagune.control.npc.NpcTalk.Choice.LESSON,
                com.hex.kagune.control.npc.NpcTalk.Choice.INSULT, com.hex.kagune.control.npc.NpcTalk.Choice.LEAVE));
        etoFrame(new File(dir, "eto-talk-4-claimed.png"), half, cold, new com.hex.kagune.control.client.NpcTalkView.Motion(3.0, 99.0, 12.0F, -8, 0.5));
    }

    /**
     * 4.1.67: the gangs' leaders on leaving: Roma's member page with the way out, her asking to be sure, Banjou's goodbye, and Ayato as a
     * spouse whose wife or husband runs with the Clowns (his greeting is his demand).
     */
    static void gangTalk(File dir) throws Exception {
        int clowns = com.hex.kagune.control.npc.GangPlayer.CLOWNS;
        int gas = com.hex.kagune.control.npc.GangPlayer.GAS_MASKS;
        com.hex.kagune.control.npc.NpcTalk.Choice[] says = com.hex.kagune.control.npc.NpcTalk.Choice.SAYS;
        com.hex.kagune.control.npc.NpcTalk.Choice back = com.hex.kagune.control.npc.NpcTalk.Choice.BACK;
        com.hex.kagune.control.npc.NpcTalk.Choice leave = com.hex.kagune.control.npc.NpcTalk.Choice.LEAVE;
        BufferedImage roma = ImageIO.read(new File("src/main/resources/assets/hexkagune/textures/entity/roma/roma.png"));
        BufferedImage banjou = ImageIO.read(new File("src/main/resources/assets/hexkagune/textures/entity/banjou/banjou.png"));
        BufferedImage ayato = ImageIO.read(new File("src/main/resources/assets/hexkagune/textures/entity/ayato/ayato.png"));
        etoFrame(new File(dir, "gang-roma-member.png"), roma, new com.hex.kagune.control.client.NpcTalkView.Model("Roma", "Your ringmaster", 70, false,
            "My favourite Clown! Go scare someone.", java.util.List.of(says[0], back), java.util.List.of(com.hex.kagune.control.npc.GangWords.wantOut(clowns), "")),
            com.hex.kagune.control.client.NpcTalkView.Motion.still(70));
        etoFrame(new File(dir, "gang-roma-sure.png"), roma, new com.hex.kagune.control.client.NpcTalkView.Model("Roma", "Your ringmaster", 70, false,
            com.hex.kagune.control.npc.GangWords.asked(clowns), java.util.List.of(says[0], says[1], back), java.util.List.of("I'm out. I quit.", "No. I'm staying.",
                "")), com.hex.kagune.control.client.NpcTalkView.Motion.still(70));
        etoFrame(new File(dir, "gang-banjou-gone.png"), banjou, new com.hex.kagune.control.client.NpcTalkView.Model("Banjou", "Gas Mask leader", 60, false,
            com.hex.kagune.control.npc.GangWords.gone(gas), java.util.List.of(says[2], leave), java.util.List.of("I want to join the Gas Masks.", "")),
            com.hex.kagune.control.client.NpcTalkView.Motion.still(60));
        etoFrame(new File(dir, "gang-ayato-spouse.png"), ayato, new com.hex.kagune.control.client.NpcTalkView.Model("Ayato", "Your husband", 92, false,
            com.hex.kagune.control.npc.GangWords.demand(com.hex.kagune.control.npc.GangPlayer.AOGIRI, clowns, 0), java.util.List.of(
                com.hex.kagune.control.npc.NpcTalk.Choice.CHAT, com.hex.kagune.control.npc.NpcTalk.Choice.SWEET, says[2], leave),
            java.util.List.of("Talk to me.", "Be sweet to him.", "I want to join Aogiri.", "")), com.hex.kagune.control.client.NpcTalkView.Motion.still(92));
    }

    /**
     * 4.1.71 (the owner: "CCG Faction: / Join through the general (quest) and now daily quests as well."): the General's pages: asked to join
     * (why, three answers; a ghoul told of the division), sworn in, a member's page with the reputation meter in the CCG's cyan blue and
     * today's orders, and the orders given.
     */
    static void ccgTalk(File dir) throws Exception {
        int ccg = com.hex.kagune.control.npc.GangPlayer.CCG;
        com.hex.kagune.control.npc.NpcTalk.Choice[] says = com.hex.kagune.control.npc.NpcTalk.Choice.SAYS;
        com.hex.kagune.control.npc.NpcTalk.Choice back = com.hex.kagune.control.npc.NpcTalk.Choice.BACK;
        com.hex.kagune.control.npc.NpcTalk.Choice leave = com.hex.kagune.control.npc.NpcTalk.Choice.LEAVE;
        com.hex.kagune.control.npc.NpcTalk.Choice chat = com.hex.kagune.control.npc.NpcTalk.Choice.CHAT;
        BufferedImage general = ImageIO.read(new File("src/main/resources/assets/hexkagune/textures/entity/general/general.png"));
        String[] why = com.hex.kagune.control.npc.GangWords.GENERAL_WHY;
        etoFrame(new File(dir, "ccg-general-why.png"), general, new com.hex.kagune.control.client.NpcTalkView.Model("General Kuroda", "The General", 30,
            false, "You're a ghoul. *Flat, not unkind.* We have a division for that. The oath's the same. You want to join the CCG. *He sets the pen "
                + "down.* Why? One answer. Make it the true one.", java.util.List.of(says[0], says[1], says[2], back), java.util.List.of(why[2], why[0],
                why[1], "")), com.hex.kagune.control.client.NpcTalkView.Motion.still(30));
        etoFrame(new File(dir, "ccg-general-sworn.png"), general, new com.hex.kagune.control.client.NpcTalkView.Model("General Kuroda", "The General", 48,
            false, "*He stands, which he doesn't do for many.* Investigator. Welcome to the Commission of Counter Ghoul. Your quinque's in the case. "
                + "Look after it, and it'll look after you.", java.util.List.of(chat, says[2], leave), java.util.List.of("", "How's the bureau, sir?", "")),
            com.hex.kagune.control.client.NpcTalkView.Motion.still(48));
        etoFrame(new File(dir, "ccg-general-member.png"), general, new com.hex.kagune.control.client.NpcTalkView.Model("General Kuroda", "The General", 55,
            false, "You're one of mine now. That means you come back alive. That's an order.", java.util.List.of(says[0], back), java.util.List.of(
                com.hex.kagune.control.npc.GangWords.wantOut(ccg), ""), 72 | ccg << 8), com.hex.kagune.control.client.NpcTalkView.Motion.still(55));
        com.hex.kagune.control.npc.GangDaily orders = com.hex.kagune.control.npc.GangDaily.give(ccg, 9L, 3L);
        etoFrame(new File(dir, "ccg-general-orders.png"), general, new com.hex.kagune.control.client.NpcTalkView.Model("General Kuroda", "The General", 55,
            false, "Today's orders: " + orders.job().asked(orders.goal()) + ". Report back when it's done.", java.util.List.of(chat, says[2], says[5], leave),
            java.util.List.of("", "How's the bureau, sir?", "Today's orders: how far?", ""), 72 | ccg << 8), com.hex.kagune.control.client.NpcTalkView.Motion.still(55));
    }

    /**
     * 4.1.70 (the owner: "Anteikou - Joinable through Ken (quest)"): Ken's pages for Anteiku: asked to join (why, three answers), the beans
     * in hand, taken in, and a member's page with the reputation meter in Anteiku's light brown.
     */
    static void anteikuTalk(File dir) throws Exception {
        int anteiku = com.hex.kagune.control.npc.GangPlayer.ANTEIKU;
        com.hex.kagune.control.npc.NpcTalk.Choice[] says = com.hex.kagune.control.npc.NpcTalk.Choice.SAYS;
        com.hex.kagune.control.npc.NpcTalk.Choice back = com.hex.kagune.control.npc.NpcTalk.Choice.BACK;
        com.hex.kagune.control.npc.NpcTalk.Choice leave = com.hex.kagune.control.npc.NpcTalk.Choice.LEAVE;
        com.hex.kagune.control.npc.NpcTalk.Choice chat = com.hex.kagune.control.npc.NpcTalk.Choice.CHAT;
        BufferedImage ken = ImageIO.read(new File("src/main/resources/assets/hexkagune/textures/entity/ken/ken.png"));
        String[] answers = com.hex.kagune.control.npc.GangWords.KEN_WHY;
        etoFrame(new File(dir, "anteiku-ken-why.png"), ken, new com.hex.kagune.control.client.NpcTalkView.Model("Ken", "The one-eyed king", 45, false,
            "Anteiku? *He closes his book on one finger.* It's just a café. ...Mostly. People who join us look after each other. Why do you want in? "
                + "Honestly.", java.util.List.of(says[0], says[1], says[2], back), java.util.List.of(answers[1], answers[2], answers[0], "")),
            com.hex.kagune.control.client.NpcTalkView.Motion.still(45));
        etoFrame(new File(dir, "anteiku-ken-beans.png"), ken, new com.hex.kagune.control.client.NpcTalkView.Model("Ken", "The one-eyed king", 52, false,
            "*He's quiet for a long moment. Then he smiles, a little sadly.* ...Yeah. That's the right reason. It was mine too. Okay. Small things "
                + "first: we're out of beans, and Touka's been unbearable. Eight, if you can find them.", java.util.List.of(chat, says[2],
                com.hex.kagune.control.npc.NpcTalk.Choice.INSULT, leave), java.util.List.of("", "Here. Coffee beans. (the cocoa beans in your hand)", "", "")),
            com.hex.kagune.control.client.NpcTalkView.Motion.still(52));
        etoFrame(new File(dir, "anteiku-ken-welcome.png"), ken, new com.hex.kagune.control.client.NpcTalkView.Model("Ken", "Ken", 70, false,
            "*He hangs a second apron on the hook by the door.* ...Welcome to Anteiku. Nobody here will ask what you did before. Just... be kind, okay?",
            java.util.List.of(chat, says[2], leave), java.util.List.of("", "How's Anteiku?", "")), com.hex.kagune.control.client.NpcTalkView.Motion.still(70));
        etoFrame(new File(dir, "anteiku-ken-member.png"), ken, new com.hex.kagune.control.client.NpcTalkView.Model("Ken", "Ken", 74, false,
            "Have you eaten? Properly, I mean. Sit down, I'll make you a cup.", java.util.List.of(says[0], back), java.util.List.of(
                com.hex.kagune.control.npc.GangWords.wantOut(anteiku), ""), 64 | anteiku << 8), com.hex.kagune.control.client.NpcTalkView.Motion.still(74));
    }

    /**
     * 4.1.70 (the owner: "Enegy bar increases size and increases maximum capacity."; then "naw man just make the energy bar smaller but longer
     * to fit the bar, dont make it past it wtf bruh"; of two ways, "B: shrunk into its spot"): the HUD as it is and for one of Anteiku (the
     * finer, longer bar on half pixels in the same spot), each at 1.65x; then the bar full, hungry and low, as it is and for Anteiku, at 3.6x.
     */
    static void anteikuBar(File out) throws Exception {
        int w = 900;
        int h = 520;
        BufferedImage image = new BufferedImage(w * 2, h * 2, BufferedImage.TYPE_INT_RGB);
        ImageCanvas c = new ImageCanvas(graphics(image, 2), false);
        c.fill(0, 0, w, h, 0xFF141016);
        c.text("ANTEIKU: the one bar (energy and food) as it is (left) and for one of Anteiku (right): finer and longer in the same spot, holding "
            + "one bar more", 8, 6, 0xFFC89B6D, false);
        for (int side = 0; side < 2; side++) {
            c.push();
            c.translate(10 + side * 445, 24);
            c.scale(1.65F);
            hudWide(c, 0.62F, side == 1);
            c.pop();
        }
        float[][] reserve = {{1.0F, 0.0F, 40.0F}, {0.4F, 0.55F, 120.0F}, {0.15F, 0.85F, 31.5F}};
        String[] names = {"full", "hungry (40%)", "low (15%): it beats"};
        for (int k = 0; k < 3; k++) {
            for (int side = 0; side < 2; side++) {
                int x = 10 + side * 445;
                int y = 160 + k * 120;
                c.text((side == 0 ? "As it is, " : "One of Anteiku, ") + names[k], x, y, 0xFFE6D2DA, false);
                c.push();
                c.translate(x, y + 12);
                c.scale(3.6F);
                com.hex.kagune.control.client.ReserveBarView.Frame frame = new com.hex.kagune.control.client.ReserveBarView.Frame(reserve[k][0],
                    reserve[k][0], 0.0F, 0.0F, reserve[k][1], reserve[k][2]);
                if (side == 1) {
                    com.hex.kagune.control.client.ReserveBarView.drawFine(c, 0, 0, frame);
                } else {
                    com.hex.kagune.control.client.ReserveBarView.draw(c, 0, 0, frame);
                }
                c.pop();
            }
        }
        ImageIO.write(image, "png", out);
        System.out.println("wrote " + out);
    }

    /** 4.1.70: a 260 x 60 slice of the survival HUD round the hotbar (its middle at x 100); {@code fine}: one of Anteiku's bar. */
    static void hudWide(ImageCanvas c, float fill, boolean fine) {
        int cx = 100;
        int H = 60;
        c.fill(0, 0, 260, H, 0xFF5E8A48);
        c.fill(0, 0, 260, 20, 0xFF7FA6D8);
        c.fill(cx - 91, H - 22, cx + 91, H, 0xFF2A2A2A);
        for (int s = 0; s < 9; s++) {
            c.fill(cx - 90 + s * 20, H - 21, cx - 90 + s * 20 + 20, H - 1, 0xFF8B8B8B);
            c.fill(cx - 89 + s * 20, H - 20, cx - 89 + s * 20 + 18, H - 2, 0xFF3F3F3F);
        }
        c.fill(cx - 91, H - 29, cx + 91, H - 24, 0xFF1A1A1A);
        c.fill(cx - 90, H - 28, cx + 30, H - 25, 0xFF80FF20);
        for (int i = 0; i < 10; i++) {
            int hx = cx - 91 + i * 8;
            int hy = H - 39;
            for (int r = 0; r < 9; r++) {
                for (int q = 0; q < 9; q++) {
                    char ch = HEART[r].charAt(q);
                    if (ch != '.') {
                        int col = ch == '#' ? 0xFF1A0000 : ch == 'w' ? 0xFFFFC8C8 : i < 7 ? 0xFFE01818 : 0xFF3A0E0E;
                        c.fill(hx + q, hy + r, hx + q + 1, hy + r + 1, col);
                    }
                }
            }
        }
        com.hex.kagune.control.client.ReserveBarView.Frame frame = new com.hex.kagune.control.client.ReserveBarView.Frame(fill, fill, 0.0F, 0.0F, 0.2F, 50.0);
        if (fine) {
            com.hex.kagune.control.client.ReserveBarView.drawFine(c, cx + 10, H - 39, frame);
        } else {
            com.hex.kagune.control.client.ReserveBarView.draw(c, cx + 10, H - 39, frame);
        }
        // The level number as the game draws it, after the food row: centred, 35 up from the bottom, outlined in black.
        String level = "12";
        int lx = cx - c.width(level) / 2;
        for (int[] o : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            c.text(level, lx + o[0], H - 35 + o[1], 0xFF000000, false);
        }
        c.text(level, lx, H - 35, 0xFF80FF20, false);
    }

    /** 2.1.3: a village human's talk: the first page, the "Talk a while." page with one of its secrets told, and a gift it loved. */
    static void villageTalk(File dir) throws Exception {
        BufferedImage face = ImageIO.read(new File("src/main/resources/assets/hexkagune/textures/entity/human/human_03.png"));
        com.hex.kagune.control.village.HumanPersona.Temper shy = com.hex.kagune.control.village.HumanPersona.Temper.SHY;
        com.hex.kagune.control.village.HumanLife.Life life = com.hex.kagune.control.village.HumanLife.of(424242L,
            com.hex.kagune.control.village.HumanLife.Job.BEEKEEPER);
        com.hex.kagune.control.npc.NpcTalk.Choice[] says = com.hex.kagune.control.npc.NpcTalk.Choice.SAYS;
        java.util.List<com.hex.kagune.control.npc.NpcTalk.Choice> main = java.util.List.of(says[0], com.hex.kagune.control.npc.NpcTalk.Choice.CHAT, says[3],
            com.hex.kagune.control.npc.NpcTalk.Choice.GIFT, com.hex.kagune.control.npc.NpcTalk.Choice.FAVOUR, com.hex.kagune.control.npc.NpcTalk.Choice.LEAVE);
        java.util.List<String> mainLabels = java.util.List.of("How are you?", "Talk a while.", "You look lovely today.",
            "This is for you. (the " + life.lovesName() + " in your hand)", "Let's trade.", "");
        String hello = com.hex.kagune.control.village.HumanVoice.caught(life, 0) + com.hex.kagune.control.village.HumanPersona.hello(shy, 64, false, 1);
        etoFrame(new File(dir, "village-talk-main.png"), face, new com.hex.kagune.control.client.NpcTalkView.Model("Hana", "Shy beekeeper", 64, false,
            hello, main, mainLabels), com.hex.kagune.control.client.NpcTalkView.Motion.still(64));
        java.util.List<com.hex.kagune.control.village.HumanVoice.Topic> topics = com.hex.kagune.control.village.HumanVoice.topics(86, false,
            com.hex.kagune.control.village.HumanVoice.Topic.WORK.flag() | com.hex.kagune.control.village.HumanVoice.Topic.FAMILY.flag(), 2);
        java.util.List<com.hex.kagune.control.npc.NpcTalk.Choice> choices = new java.util.ArrayList<>();
        java.util.List<String> labels = new java.util.ArrayList<>();
        for (int i = 0; i < topics.size(); i++) {
            choices.add(says[i]);
            labels.add(topics.get(i).label);
        }
        choices.add(com.hex.kagune.control.npc.NpcTalk.Choice.BACK);
        labels.add("Let's talk about something else.");
        choices.add(com.hex.kagune.control.npc.NpcTalk.Choice.LEAVE);
        labels.add("");
        String secret = com.hex.kagune.control.village.HumanVoice.answer(shy, life, com.hex.kagune.control.village.HumanVoice.Topic.SECRET, 0, null);
        etoFrame(new File(dir, "village-talk-secret.png"), face, new com.hex.kagune.control.client.NpcTalkView.Model("Hana", "Shy beekeeper", 88, false,
            secret, choices, labels), com.hex.kagune.control.client.NpcTalkView.Motion.still(88));
        String loved = com.hex.kagune.control.village.HumanVoice.gift(shy, com.hex.kagune.control.village.HumanLife.Gift.LOVED, life.lovesName(), false, 1);
        etoFrame(new File(dir, "village-talk-gift.png"), face, new com.hex.kagune.control.client.NpcTalkView.Model("Hana", "Shy beekeeper", 72, false,
            loved, main, mainLabels), com.hex.kagune.control.client.NpcTalkView.Motion.still(72));
    }

    /**
     * 4.1.52: the talk screen with the reputation meter: one of Aogiri talking to a feared member, one of the Clowns to a member at half,
     * one to a member whose bar is empty (UNWANTED beating), and Ayato to a member of his (the leaders show it too).
     */
    static void reputationTalk(File dir) throws Exception {
        BufferedImage ghoul = ImageIO.read(new File("src/main/resources/assets/hexkagune/textures/entity/ghoul/boy_03.png"));
        BufferedImage ayato = ImageIO.read(new File("src/main/resources/assets/hexkagune/textures/entity/ayato/ayato.png"));
        java.util.List<com.hex.kagune.control.npc.NpcTalk.Choice> all = java.util.List.of(com.hex.kagune.control.npc.NpcTalk.Choice.WHO,
            com.hex.kagune.control.npc.NpcTalk.Choice.CHAT, com.hex.kagune.control.npc.NpcTalk.Choice.FAVOUR, com.hex.kagune.control.npc.NpcTalk.Choice.LEAVE);
        int aogiri = com.hex.kagune.control.npc.GangPlayer.AOGIRI << 8;
        int clowns = com.hex.kagune.control.npc.GangPlayer.CLOWNS << 8;
        etoFrame(new File(dir, "reputation-feared.png"), ghoul, new com.hex.kagune.control.client.NpcTalkView.Model("Masked ghoul", "Aogiri Tree", 40, false,
            com.hex.kagune.control.npc.GangTalk.greet(88, 0), all, all.stream().map(com.hex.kagune.control.npc.NpcTalk.Choice::text).toList(), 88 | aogiri),
            com.hex.kagune.control.client.NpcTalkView.Motion.still(40));
        etoFrame(new File(dir, "reputation-known.png"), ghoul, new com.hex.kagune.control.client.NpcTalkView.Model("Grinning clown", "The Clowns", 10, false,
            com.hex.kagune.control.npc.GangTalk.greet(50, 0), all, all.stream().map(com.hex.kagune.control.npc.NpcTalk.Choice::text).toList(), 50 | clowns),
            com.hex.kagune.control.client.NpcTalkView.Motion.still(10));
        etoFrame(new File(dir, "reputation-unwanted.png"), ghoul, new com.hex.kagune.control.client.NpcTalkView.Model("Masked ghoul", "Aogiri Tree", -40, false,
            com.hex.kagune.control.npc.GangTalk.greet(0, 0), all, all.stream().map(com.hex.kagune.control.npc.NpcTalk.Choice::text).toList(), aogiri),
            com.hex.kagune.control.client.NpcTalkView.Motion.still(-40));
        BufferedImage half = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        half.getGraphics().drawImage(ayato, 0, 0, 64, 64, null);
        etoFrame(new File(dir, "reputation-leader.png"), ayato.getWidth() > 64 ? half : ayato, new com.hex.kagune.control.client.NpcTalkView.Model("Ayato",
            "Aogiri's - bored", 30, false, "Tch. You're still breathing. Good. Aogiri doesn't need dead weight.", all,
            java.util.List.of("Who are you?", "Talk to me.", "How's Aogiri?", "Leave."), 67 | aogiri), com.hex.kagune.control.client.NpcTalkView.Motion.still(30));
    }

    /**
     * 4.1.78: the daily board as each giver shows it (Noro, Roma, Banjou, the General, Ken): their words, today's three jobs to choose from
     * (GangDaily.offers, every job a player can do), the way back; then one taken, in their words (DailyVoice).
     */
    static void boardTalk(File dir) throws Exception {
        java.util.List<com.hex.kagune.control.npc.NpcTalk.Choice> board = java.util.List.of(com.hex.kagune.control.npc.NpcTalk.Choice.SAY_1,
            com.hex.kagune.control.npc.NpcTalk.Choice.SAY_2, com.hex.kagune.control.npc.NpcTalk.Choice.SAY_3, com.hex.kagune.control.npc.NpcTalk.Choice.BACK);
        String[][] givers = {{"Noro", "Aogiri's - quiet", "noro/noro.png"}, {"Roma", "Your ringmaster - giddy", "roma/roma.png"},
            {"Banjou", "The Gas Masks' leader - happy", "banjou/banjou.png"}, {"The General", "The CCG - stern", "general/general.png"},
            {"Ken", "Anteiku - warm", "ken/ken.png"}};
        int[] gangs = {com.hex.kagune.control.npc.GangPlayer.AOGIRI, com.hex.kagune.control.npc.GangPlayer.CLOWNS,
            com.hex.kagune.control.npc.GangPlayer.GAS_MASKS, com.hex.kagune.control.npc.GangPlayer.CCG, com.hex.kagune.control.npc.GangPlayer.ANTEIKU};
        for (int i = 0; i < gangs.length; i++) {
            int gang = gangs[i];
            long day = 5L + i;
            java.util.List<com.hex.kagune.control.npc.GangDaily.Job> offered = com.hex.kagune.control.npc.GangDaily.offers(gang, day, 11L, job -> true);
            java.util.List<String> labels = new java.util.ArrayList<>();
            for (com.hex.kagune.control.npc.GangDaily.Job job : offered) {
                labels.add(job.text(com.hex.kagune.control.npc.GangDaily.take(job, day, 11L).goal()));
            }
            labels.add("Back.");
            String key = givers[i][0].toLowerCase().replace("the ", "");
            etoFrame(new File(dir, "board-" + key + ".png"), face(givers[i][2]), new com.hex.kagune.control.client.NpcTalkView.Model(givers[i][0],
                givers[i][1], 40, false, com.hex.kagune.control.npc.DailyVoice.voice(gang, "board", i, "", null), board, labels, 60 | gang << 8),
                com.hex.kagune.control.client.NpcTalkView.Motion.still(40));
            com.hex.kagune.control.npc.GangDaily taken = com.hex.kagune.control.npc.GangDaily.take(offered.get(1), day, 11L);
            java.util.List<com.hex.kagune.control.npc.NpcTalk.Choice> page = java.util.List.of(com.hex.kagune.control.npc.NpcTalk.Choice.CHAT,
                com.hex.kagune.control.npc.NpcTalk.Choice.SAY_1, com.hex.kagune.control.npc.NpcTalk.Choice.LEAVE);
            etoFrame(new File(dir, "board-" + key + "-taken.png"), face(givers[i][2]), new com.hex.kagune.control.client.NpcTalkView.Model(givers[i][0],
                givers[i][1], 40, false, com.hex.kagune.control.npc.DailyVoice.voice(gang, "took", i, taken.job().asked(taken.goal()), taken), page,
                java.util.List.of("Talk to me.", "Today's job: how far?", "Leave."), 60 | gang << 8), com.hex.kagune.control.client.NpcTalkView.Motion.still(40));
        }
    }

    /**
     * 4.1.79: the gangs' ways in, as each leader shows them (JoinVoice's words, JoinTrial's questions): every trial put to them with the one
     * button that begins it; the General's academy test (a question, its three answers turned round); Roma's job in her face; each leader's
     * scene as they take them in (the member's page, the reputation bar now theirs).
     */
    static void joiningTalk(File dir) throws Exception {
        com.hex.kagune.control.npc.NpcTalk.Choice[] says = com.hex.kagune.control.npc.NpcTalk.Choice.SAYS;
        com.hex.kagune.control.npc.NpcTalk.Choice back = com.hex.kagune.control.npc.NpcTalk.Choice.BACK;
        com.hex.kagune.control.npc.NpcTalk.Choice chat = com.hex.kagune.control.npc.NpcTalk.Choice.CHAT;
        com.hex.kagune.control.npc.NpcTalk.Choice leave = com.hex.kagune.control.npc.NpcTalk.Choice.LEAVE;
        String[][] leaders = {{"Ayato", "The black rabbit - bored", "ayato/ayato.png"}, {"Roma", "The Clowns' ringmaster - giddy", "roma/roma.png"},
            {"Banjou", "The Gas Masks' leader - wary", "banjou/banjou.png"}, {"Ken", "The one-eyed king - happy", "ken/ken.png"},
            {"The General", "The General - wary", "general/general.png"}};
        for (com.hex.kagune.control.npc.JoinTrial.Kind kind : com.hex.kagune.control.npc.JoinTrial.Kind.values()) {
            int gang = com.hex.kagune.control.npc.JoinTrial.gang(kind);
            String[] who = leaders[gang - 1];
            etoFrame(new File(dir, "join-" + kind.name().toLowerCase() + "-offer.png"), face(who[2]), new com.hex.kagune.control.client.NpcTalkView.Model(who[0],
                who[1], 45, false, com.hex.kagune.control.npc.JoinVoice.say(kind, "offer", 0), java.util.List.of(says[0], back),
                java.util.List.of(com.hex.kagune.control.npc.JoinVoice.start(kind), ""), -1), com.hex.kagune.control.client.NpcTalkView.Motion.still(45));
            etoFrame(new File(dir, "join-" + kind.name().toLowerCase() + "-start.png"), face(who[2]), new com.hex.kagune.control.client.NpcTalkView.Model(who[0],
                who[1], 45, false, com.hex.kagune.control.npc.JoinVoice.say(kind, "start", 0), java.util.List.of(chat, says[2], leave),
                java.util.List.of("Talk to me.", com.hex.kagune.control.npc.JoinVoice.ask(kind), ""), -1), com.hex.kagune.control.client.NpcTalkView.Motion.still(45));
        }
        // The right first answer taken, the next step put to them in the same breath (the longest lines on these pages).
        com.hex.kagune.control.npc.JoinTrial.Kind[] next = {null, com.hex.kagune.control.npc.JoinTrial.Kind.RING, com.hex.kagune.control.npc.JoinTrial.Kind.GAS,
            com.hex.kagune.control.npc.JoinTrial.Kind.SHIFT, null};
        for (int gang = 2; gang <= 5; gang++) {
            String[] who = leaders[gang - 1];
            boolean exam = gang == com.hex.kagune.control.npc.GangPlayer.CCG;
            String line = com.hex.kagune.control.npc.JoinVoice.answered(gang) + (exam ? com.hex.kagune.control.npc.JoinVoice.general("exam", 0)
                : com.hex.kagune.control.npc.JoinVoice.say(next[gang - 1], "offer", 0));
            etoFrame(new File(dir, "join-answered-" + who[0].toLowerCase().replace("the ", "") + ".png"), face(who[2]),
                new com.hex.kagune.control.client.NpcTalkView.Model(who[0], who[1], 45, false, line, java.util.List.of(says[0], back),
                    java.util.List.of(exam ? "I'm ready for the test." : com.hex.kagune.control.npc.JoinVoice.start(next[gang - 1]), ""), -1),
                com.hex.kagune.control.client.NpcTalkView.Motion.still(45));
        }
        int[] drawn = com.hex.kagune.control.npc.JoinTrial.draw(12L, 5L);
        for (int i = 0; i < drawn.length; i++) {
            com.hex.kagune.control.npc.JoinTrial.Question q = com.hex.kagune.control.npc.JoinTrial.QUESTIONS[drawn[i]];
            java.util.List<String> answers = new java.util.ArrayList<>(java.util.List.of(q.answer(0), q.answer(1), q.answer(2)));
            java.util.Collections.rotate(answers, i + 1);
            answers.add("");
            String said = (i == 0 ? com.hex.kagune.control.npc.JoinVoice.general("exam", 0) : com.hex.kagune.control.npc.JoinVoice.general("next", i))
                + " Question " + (i + 1) + " of " + com.hex.kagune.control.npc.JoinTrial.EXAM_ASKED + ": " + q.asked();
            etoFrame(new File(dir, "join-exam-" + (i + 1) + ".png"), face("general/general.png"), new com.hex.kagune.control.client.NpcTalkView.Model(
                "The General", "The General - wary", 30, false, said, java.util.List.of(says[0], says[1], says[2], back), answers, -1),
                com.hex.kagune.control.client.NpcTalkView.Motion.still(30));
        }
        etoFrame(new File(dir, "join-roma-job.png"), face("roma/roma.png"), new com.hex.kagune.control.client.NpcTalkView.Model("Roma",
            "The Clowns' ringmaster - giddy", 55, false, com.hex.kagune.control.npc.JoinVoice.roma("job", 0), java.util.List.of(chat, says[2], leave),
            java.util.List.of("Talk to me.", "About the job...", ""), -1), com.hex.kagune.control.client.NpcTalkView.Motion.still(55));
        String[] asks = {"How's Aogiri?", "How are the Clowns?", "How are the boys?", "How's Anteiku?", "How's the bureau, sir?"};
        for (int gang = 1; gang <= 5; gang++) {
            String[] who = leaders[gang - 1];
            etoFrame(new File(dir, "join-scene-" + who[0].toLowerCase().replace("the ", "") + ".png"), face(who[2]),
                new com.hex.kagune.control.client.NpcTalkView.Model(who[0], who[1], 60, false, com.hex.kagune.control.npc.JoinVoice.scene(gang, 0),
                    java.util.List.of(chat, says[2], leave), java.util.List.of("Talk to me.", asks[gang - 1], ""), 50 | gang << 8),
                com.hex.kagune.control.client.NpcTalkView.Motion.still(60));
        }
    }

    /**
     * 4.1.80: the courtship's new chapters, as each companion asks them (the button that begins the spar or the homework; how far a count
     * has gone), Touka's homework (a question, its three answers turned round), each changed moment's first page with its answers, and the
     * journal's Romance tab on a chapter of each kind.
     */
    static void romanceTalk(File dir) throws Exception {
        com.hex.kagune.control.npc.NpcTalk.Choice[] says = com.hex.kagune.control.npc.NpcTalk.Choice.SAYS;
        com.hex.kagune.control.npc.NpcTalk.Choice back = com.hex.kagune.control.npc.NpcTalk.Choice.BACK;
        com.hex.kagune.control.npc.NpcTalk.Choice chat = com.hex.kagune.control.npc.NpcTalk.Choice.CHAT;
        com.hex.kagune.control.npc.NpcTalk.Choice favour = com.hex.kagune.control.npc.NpcTalk.Choice.FAVOUR;
        com.hex.kagune.control.npc.NpcTalk.Choice leave = com.hex.kagune.control.npc.NpcTalk.Choice.LEAVE;
        String[][] who = {{"touka", "Touka", "Anteiku's waitress - wary", "touka/touka.png"}, {"ken", "Ken", "The one-eyed king - happy", "ken/ken.png"},
            {"ayato", "Ayato", "The black rabbit - bored", "ayato/ayato.png"}, {"general", "The General", "The General - wary", "general/general.png"},
            {"roma", "Roma", "The Clowns' ringmaster - giddy", "roma/roma.png"}, {"banjou", "Banjou", "The Gas Masks' leader - happy", "banjou/banjou.png"}};
        java.util.Map<String, String[]> by = new java.util.HashMap<>();
        for (String[] w : who) {
            by.put(w[0], w);
        }
        String[] changed = {"touka_patrol", "ken_doves", "ayato_ghouls", "ayato_roof", "general_vouch", "general_pack", "general_high", "roma_show",
            "banjou_supper", "banjou_doves"};
        for (String id : changed) {
            String key = id.substring(0, id.indexOf('_'));
            String[] w = by.get(key);
            com.hex.kagune.control.npc.Courtship.Chapter chapter = null;
            for (com.hex.kagune.control.npc.Courtship.Chapter one : com.hex.kagune.control.npc.Courtship.of(key)) {
                if (one.id().equals(id)) {
                    chapter = one;
                }
            }
            boolean begun = chapter.task().begun();
            int done = chapter.task().once() ? 0 : Math.max(1, chapter.need() / 3);
            String line = com.hex.kagune.control.npc.Courtship.said(chapter, begun ? 0 : done);
            etoFrame(new File(dir, "court-" + id + ".png"), face(w[3]), new com.hex.kagune.control.client.NpcTalkView.Model(w[1], w[2], 62, false, line,
                begun ? java.util.List.of(says[0], back) : java.util.List.of(chat, favour, leave),
                begun ? java.util.List.of(com.hex.kagune.control.npc.Courtship.begin(chapter.task()), "") : java.util.List.of("Talk to me.",
                    "About what you asked...", ""), -1), com.hex.kagune.control.client.NpcTalkView.Motion.still(62));
        }
        // Touka's homework: the three questions of a day, answers turned round as the server turns them.
        int[] drawn = com.hex.kagune.control.npc.Courtship.homework(9L, 3L);
        for (int i = 0; i < drawn.length; i++) {
            com.hex.kagune.control.npc.JoinTrial.Question q = com.hex.kagune.control.npc.Courtship.HOMEWORK[drawn[i]];
            java.util.List<String> answers = new java.util.ArrayList<>(java.util.List.of(q.answer(0), q.answer(1), q.answer(2)));
            java.util.Collections.rotate(answers, i + 1);
            answers.add("");
            String said = (i == 0 ? com.hex.kagune.control.npc.CourtVoice.homework("begin", 0) : com.hex.kagune.control.npc.CourtVoice.homework("next", i))
                + " Question " + (i + 1) + " of " + com.hex.kagune.control.npc.Courtship.ASKED + ": " + q.asked();
            etoFrame(new File(dir, "court-homework-" + (i + 1) + ".png"), face("touka/touka.png"), new com.hex.kagune.control.client.NpcTalkView.Model(
                "Touka", "Anteiku's waitress - wary", 62, false, said, java.util.List.of(says[0], says[1], says[2], back), answers, -1),
                com.hex.kagune.control.client.NpcTalkView.Motion.still(62));
        }
        etoFrame(new File(dir, "court-homework-wrong.png"), face("touka/touka.png"), new com.hex.kagune.control.client.NpcTalkView.Model("Touka",
            "Anteiku's waitress - sulking", 62, false, com.hex.kagune.control.npc.CourtVoice.homework("wrong", 0), java.util.List.of(chat, favour, leave),
            java.util.List.of("Talk to me.", "About what you asked...", ""), -1), com.hex.kagune.control.client.NpcTalkView.Motion.still(62));
        // Each changed moment's first page, as it plays when its task is done.
        for (String id : changed) {
            String key = id.substring(0, id.indexOf('_'));
            String[] w = by.get(key);
            String text = new String(java.nio.file.Files.readAllBytes(new File("src/main/resources/data/hexkagune/talk/" + key + ".txt").toPath()),
                java.nio.charset.StandardCharsets.UTF_8);
            com.hex.kagune.control.npc.scene.Scene scene = null;
            for (com.hex.kagune.control.npc.scene.Scene one : com.hex.kagune.control.npc.scene.SceneBook.parse(key, text).scenes()) {
                if (one.id().equals(id)) {
                    scene = one;
                }
            }
            com.hex.kagune.control.npc.scene.Scene.Page first = scene.first();
            java.util.List<com.hex.kagune.control.npc.NpcTalk.Choice> choices = new java.util.ArrayList<>();
            java.util.List<String> labels = new java.util.ArrayList<>();
            for (int i = 0; i < first.options().size(); i++) {
                choices.add(says[i]);
                labels.add(first.options().get(i).label());
            }
            etoFrame(new File(dir, "moment-" + id + ".png"), face(w[3]), new com.hex.kagune.control.client.NpcTalkView.Model(w[1], w[2], 70, false,
                first.lines().get(0), choices, labels, -1), com.hex.kagune.control.client.NpcTalkView.Motion.still(70));
        }
        // The journal's Romance tab: one courting at each new kind (Roma's show, the General's walk, Banjou's garden, Ken's reading).
        java.util.List<com.hex.kagune.control.npc.RomanceCard> cards = java.util.List.of(
            new com.hex.kagune.control.npc.RomanceCard("roma", true, com.hex.kagune.control.npc.Romance.Stage.NONE, 66, true, null,
                com.hex.kagune.control.npc.EtoRules.Step.COURTING, 3, 0L, 0, false, false, 0),
            new com.hex.kagune.control.npc.RomanceCard("general", true, com.hex.kagune.control.npc.Romance.Stage.NONE, 72, true, null,
                com.hex.kagune.control.npc.EtoRules.Step.COURTING, 5, 0L, 0, false, false, 3),
            new com.hex.kagune.control.npc.RomanceCard("banjou", true, com.hex.kagune.control.npc.Romance.Stage.NONE, 70, true, null,
                com.hex.kagune.control.npc.EtoRules.Step.COURTING, 2, 0L, 0, false, false, 1),
            new com.hex.kagune.control.npc.RomanceCard("ken", true, com.hex.kagune.control.npc.Romance.Stage.NONE, 80, true, null,
                com.hex.kagune.control.npc.EtoRules.Step.COURTING, 4, 0L, 0, false, false, 3),
            new com.hex.kagune.control.npc.RomanceCard("touka", true, com.hex.kagune.control.npc.Romance.Stage.NONE, 64, true, null,
                com.hex.kagune.control.npc.EtoRules.Step.COURTING, 0, 0L, 0, false, false, 1));
        com.hex.kagune.control.quest.QuestLog log = new com.hex.kagune.control.quest.QuestLog();
        for (String key : new String[] {"roma", "general", "banjou", "ken", "touka"}) {
            journal(new File(dir, "journal-" + key + ".png"), KagunePath.RINKAKU, 520, log, com.hex.kagune.control.client.QuestJournalView.Tab.ROMANCE,
                "romance:" + key, 0L, cards);
        }
    }

    /**
     * 4.1.81: dates at real places: each companion's dates page (four of their dates, as the server offers them) and their yes to one,
     * where to take them (CourtVoice.lead, DateSpot's words).
     */
    static void datesTalk(File dir) throws Exception {
        com.hex.kagune.control.npc.NpcTalk.Choice[] says = com.hex.kagune.control.npc.NpcTalk.Choice.SAYS;
        com.hex.kagune.control.npc.NpcTalk.Choice back = com.hex.kagune.control.npc.NpcTalk.Choice.BACK;
        String[][] who = {{"eto", "Eto", "Your girlfriend - happy", "eto/eto.png"}, {"touka", "Touka", "Your girlfriend - happy", "touka/touka.png"},
            {"ken", "Ken", "Your boyfriend - happy", "ken/ken.png"}, {"ayato", "Ayato", "Your boyfriend - bored", "ayato/ayato.png"},
            {"general", "The General", "Your boyfriend - happy", "general/general.png"}, {"roma", "Roma", "Your girlfriend - giddy", "roma/roma.png"},
            {"banjou", "Banjou", "Your boyfriend - happy", "banjou/banjou.png"}};
        for (String[] w : who) {
            String text = new String(java.nio.file.Files.readAllBytes(new File("src/main/resources/data/hexkagune/talk/" + w[0] + ".txt").toPath()),
                java.nio.charset.StandardCharsets.UTF_8);
            java.util.List<com.hex.kagune.control.npc.scene.Scene> dates = new java.util.ArrayList<>();
            for (com.hex.kagune.control.npc.scene.Scene one : com.hex.kagune.control.npc.scene.SceneBook.parse(w[0], text).scenes()) {
                if (one.kind() == com.hex.kagune.control.npc.scene.Scene.Kind.DATE) {
                    dates.add(one);
                }
            }
            java.util.List<com.hex.kagune.control.npc.NpcTalk.Choice> choices = new java.util.ArrayList<>();
            java.util.List<String> labels = new java.util.ArrayList<>();
            for (int i = 0; i < dates.size() && i < 4; i++) {
                choices.add(says[i]);
                labels.add(dates.get(i).prompt());
            }
            choices.add(back);
            labels.add("");
            etoFrame(new File(dir, "dates-" + w[0] + ".png"), face(w[3]), new com.hex.kagune.control.client.NpcTalkView.Model(w[1], w[2], 92, false,
                com.hex.kagune.control.npc.Courtship.datesAsked(w[0], 0), choices, labels, -1), com.hex.kagune.control.client.NpcTalkView.Motion.still(92));
            com.hex.kagune.control.npc.scene.Scene picked = dates.get(Math.min(1, dates.size() - 1));
            etoFrame(new File(dir, "dates-" + w[0] + "-lead.png"), face(w[3]), new com.hex.kagune.control.client.NpcTalkView.Model(w[1], w[2], 92, false,
                com.hex.kagune.control.npc.CourtVoice.lead(w[0], com.hex.kagune.control.npc.DateSpot.of(picked.id()), 0),
                java.util.List.of(com.hex.kagune.control.npc.NpcTalk.Choice.KISS, com.hex.kagune.control.npc.NpcTalk.Choice.HUG, back),
                java.util.List.of("*Kiss.*", "*Hug.*", ""), -1), com.hex.kagune.control.client.NpcTalkView.Motion.still(92));
        }
    }

    /**
     * 4.1.84: each companion's gifts page with a present in hand (the option the server adds: npc.Tastes.offer), then what they say to one
     * they love and one they hate (npc.Tastes.said), as the talk screen draws them.
     */
    static void giftsTalk(File dir) throws Exception {
        com.hex.kagune.control.npc.NpcTalk.Choice back = com.hex.kagune.control.npc.NpcTalk.Choice.BACK;
        com.hex.kagune.control.npc.NpcTalk.Choice leave = com.hex.kagune.control.npc.NpcTalk.Choice.LEAVE;
        String[][] who = {{"eto", "Eto", "Your girlfriend - happy", "eto/eto.png", "minecraft:ink_sac", "hexkagune:quinque_katana"},
            {"touka", "Touka", "Your girlfriend - happy", "touka/touka.png", "minecraft:rabbit_hide", "minecraft:bread"},
            {"ken", "Ken", "Your boyfriend - happy", "ken/ken.png", "hexkagune:anteiku_blend", "minecraft:spider_eye"},
            {"ayato", "Ayato", "Your boyfriend - bored", "ayato/ayato.png", "minecraft:netherite_sword", "minecraft:poppy"},
            {"noro", "Noro", "Aogiri's - quiet", "noro/noro.png", "hexkagune:kakuhou_tartare", "minecraft:cake"},
            {"general", "The General", "Your boyfriend - happy", "general/general.png", "minecraft:clock", "hexkagune:mask_eto"},
            {"roma", "Roma", "Your girlfriend - giddy", "roma/roma.png", "minecraft:firework_star", "minecraft:writable_book"},
            {"banjou", "Banjou", "Your boyfriend - happy", "banjou/banjou.png", "minecraft:oak_sapling", "minecraft:wither_rose"}};
        for (String[] w : who) {
            int regard = w[0].equals("noro") ? 20 : 92;
            etoFrame(new File(dir, "gifts-" + w[0] + "-page.png"), face(w[3]), new com.hex.kagune.control.client.NpcTalkView.Model(w[1], w[2], regard, false,
                com.hex.kagune.control.npc.Persona.of(w[0]).script().giftsAsked(0), java.util.List.of(com.hex.kagune.control.npc.NpcTalk.Choice.SAY_1, back),
                java.util.List.of(com.hex.kagune.control.npc.Tastes.offer(w[4]), ""), -1), com.hex.kagune.control.client.NpcTalkView.Motion.still(regard));
            String[] items = {w[4], w[5]};
            String[] names = {"loved", "hated"};
            for (int k = 0; k < 2; k++) {
                com.hex.kagune.control.npc.Tastes.Taste taste = com.hex.kagune.control.npc.Tastes.of(w[0], items[k]);
                int after = Math.max(0, Math.min(100, regard + com.hex.kagune.control.npc.Tastes.regard(taste)));
                etoFrame(new File(dir, "gifts-" + w[0] + "-" + names[k] + ".png"), face(w[3]), new com.hex.kagune.control.client.NpcTalkView.Model(w[1], w[2],
                    after, false, com.hex.kagune.control.npc.Tastes.said(w[0], taste, items[k], 0, !w[0].equals("noro")),
                    java.util.List.of(com.hex.kagune.control.npc.NpcTalk.Choice.CHAT, leave), java.util.List.of("Talk to me.", ""), -1),
                    com.hex.kagune.control.client.NpcTalkView.Motion.still(after));
            }
        }
    }

    /** 4.1.85: each companion asked what they want today (their wish of a day), then the wish brought. */
    static void wishTalk(File dir) throws Exception {
        com.hex.kagune.control.npc.NpcTalk.Choice leave = com.hex.kagune.control.npc.NpcTalk.Choice.LEAVE;
        String[][] who = {{"eto", "Eto", "Your girlfriend - happy", "eto/eto.png"}, {"touka", "Touka", "Your girlfriend - happy", "touka/touka.png"},
            {"ken", "Ken", "Your boyfriend - happy", "ken/ken.png"}, {"ayato", "Ayato", "Your boyfriend - bored", "ayato/ayato.png"},
            {"general", "The General", "Your husband - happy", "general/general.png"},
            {"roma", "Roma", "Your girlfriend - giddy", "roma/roma.png"}, {"banjou", "Banjou", "Your boyfriend - happy", "banjou/banjou.png"}};
        for (String[] w : who) {
            String wish = com.hex.kagune.control.npc.Tastes.wish(w[0], 7L);
            etoFrame(new File(dir, "wish-" + w[0] + "-asked.png"), face(w[3]), new com.hex.kagune.control.client.NpcTalkView.Model(w[1], w[2], 80, false,
                com.hex.kagune.control.npc.Tastes.wishSaid(w[0], wish, 1), java.util.List.of(com.hex.kagune.control.npc.NpcTalk.Choice.CHAT,
                com.hex.kagune.control.npc.NpcTalk.Choice.SAY_2, leave), java.util.List.of("Talk to me.", com.hex.kagune.control.npc.Tastes.WISH_ASK, ""), -1),
                com.hex.kagune.control.client.NpcTalkView.Motion.still(80));
            etoFrame(new File(dir, "wish-" + w[0] + "-granted.png"), face(w[3]), new com.hex.kagune.control.client.NpcTalkView.Model(w[1], w[2], 90, false,
                com.hex.kagune.control.npc.Tastes.granted(w[0], wish, 0), java.util.List.of(com.hex.kagune.control.npc.NpcTalk.Choice.CHAT, leave),
                java.util.List.of("Talk to me.", ""), -1), com.hex.kagune.control.client.NpcTalkView.Motion.still(90));
        }
    }

    /** 4.1.53: Noro and Roma giving today's job to one of their gang (the option on their first page, the job given, the reputation bar). */
    static void dailiesTalk(File dir) throws Exception {
        java.util.List<com.hex.kagune.control.npc.NpcTalk.Choice> page = java.util.List.of(com.hex.kagune.control.npc.NpcTalk.Choice.CHAT,
            com.hex.kagune.control.npc.NpcTalk.Choice.SAY_1, com.hex.kagune.control.npc.NpcTalk.Choice.SAY_2, com.hex.kagune.control.npc.NpcTalk.Choice.LEAVE);
        com.hex.kagune.control.npc.GangDaily noroJob = com.hex.kagune.control.npc.GangDaily.give(com.hex.kagune.control.npc.GangPlayer.AOGIRI, 3L, 11L);
        com.hex.kagune.control.npc.GangDaily romaJob = com.hex.kagune.control.npc.GangDaily.give(com.hex.kagune.control.npc.GangPlayer.CLOWNS, 3L, 4L);
        etoFrame(new File(dir, "daily-noro.png"), face("noro/noro.png"), new com.hex.kagune.control.client.NpcTalkView.Model("Noro", "Aogiri's - quiet", 20, false,
            "*tilts his head* ...Work. " + noroJob.job().text(noroJob.goal()) + ". Today.", page,
            java.util.List.of("Talk to me.", "How's Aogiri?", "Any work for me today?", "Leave."), 61 | com.hex.kagune.control.npc.GangPlayer.AOGIRI << 8),
            com.hex.kagune.control.client.NpcTalkView.Motion.still(20));
        etoFrame(new File(dir, "daily-roma.png"), face("roma/roma.png"), new com.hex.kagune.control.client.NpcTalkView.Model("Roma", "Your ringmaster - giddy", 45,
            false, "A job! For you! " + romaJob.job().text(romaJob.goal()) + ". Make it funny.", page,
            java.util.List.of("Talk to me.", "How are the Clowns?", "Got a job for me, boss?", "Leave."), 54 | com.hex.kagune.control.npc.GangPlayer.CLOWNS << 8),
            com.hex.kagune.control.client.NpcTalkView.Motion.still(45));
    }

    /**
     * 4.1.54: Banjou: asked to join (his question and the three answers, one of which takes him seriously), and giving one of the Gas Masks
     * today's job (the option on his first page, the job given, the reputation bar in the gang's green).
     */
    static void banjouTalk(File dir) throws Exception {
        java.util.List<com.hex.kagune.control.npc.NpcTalk.Choice> answers = java.util.List.of(com.hex.kagune.control.npc.NpcTalk.Choice.SAY_1,
            com.hex.kagune.control.npc.NpcTalk.Choice.SAY_2, com.hex.kagune.control.npc.NpcTalk.Choice.SAY_3, com.hex.kagune.control.npc.NpcTalk.Choice.BACK);
        etoFrame(new File(dir, "banjou-join.png"), face("banjou/banjou.png"), new com.hex.kagune.control.client.NpcTalkView.Model("Banjou",
            "The Gas Masks' leader - wary", 0, false, "Join us? *He straightens up, trying to look like a gang boss.* Why? Tell me why. And don't lie to "
            + "me. Everyone lies to me.", answers, java.util.List.of("You? The boss? Of a gang? Seriously?",
            "Because you're the strongest ghoul in the ward. Everyone says so.", "Because they follow you. Not for your strength. Because you stay.",
            "Back."), -1), com.hex.kagune.control.client.NpcTalkView.Motion.still(0));
        java.util.List<com.hex.kagune.control.npc.NpcTalk.Choice> page = java.util.List.of(com.hex.kagune.control.npc.NpcTalk.Choice.CHAT,
            com.hex.kagune.control.npc.NpcTalk.Choice.SAY_3, com.hex.kagune.control.npc.NpcTalk.Choice.SAY_1, com.hex.kagune.control.npc.NpcTalk.Choice.LEAVE);
        com.hex.kagune.control.npc.GangDaily job = com.hex.kagune.control.npc.GangDaily.give(com.hex.kagune.control.npc.GangPlayer.GAS_MASKS, 3L, 7L);
        etoFrame(new File(dir, "daily-banjou.png"), face("banjou/banjou.png"), new com.hex.kagune.control.client.NpcTalkView.Model("Banjou",
            "The Gas Masks' leader - happy", 35, false, "I hate asking. Could you " + job.job().asked(job.goal())
            + "? I'd do it myself, but you'd do it better.", page,
            java.util.List.of("Talk to me.", "How are the boys?", "Anything the boys need today?", "Leave."), 58 | com.hex.kagune.control.npc.GangPlayer.GAS_MASKS << 8),
            com.hex.kagune.control.client.NpcTalkView.Motion.still(35));
    }

    /** A companion's face from their skin (128 pixels read at 64). */
    static BufferedImage face(String path) throws Exception {
        BufferedImage skin = ImageIO.read(new File("src/main/resources/assets/hexkagune/textures/entity/" + path));
        if (skin.getWidth() == 64) {
            return skin;
        }
        BufferedImage half = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        half.getGraphics().drawImage(skin, 0, 0, 64, 64, null);
        return half;
    }

    static void etoFrame(File out, BufferedImage face, com.hex.kagune.control.client.NpcTalkView.Model model,
                         com.hex.kagune.control.client.NpcTalkView.Motion motion) throws Exception {
        int w = 960;
        int h = 540;
        BufferedImage image = new BufferedImage(w * 2, h * 2, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = graphics(image, 2);
        new ImageCanvas(g, false).gradient(0, 0, w, h, 0xFF2A3A26, 0xFF101810);
        SKIN = face;
        ImageCanvas c = new ImageCanvas(g, false);
        c.push();
        c.translate(0, h - com.hex.kagune.control.client.NpcTalkView.UI_HEIGHT);
        com.hex.kagune.control.client.NpcTalkView.draw(c, model, 300, 470, 1.3, motion);
        c.pop();
        SKIN = null;
        ImageIO.write(image, "png", out);
        System.out.println("wrote " + out);
    }

    static void talkPage(File out, String skin, com.hex.kagune.control.client.NpcTalkView.Model model, double mouseX, double mouseY) throws Exception {
        int w = 960;
        int h = 540;
        BufferedImage image = new BufferedImage(w * 2, h * 2, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = graphics(image, 2);
        String scene = System.getProperty("talk.scene");
        if (scene != null && new File(scene).isFile()) {
            g.drawImage(ImageIO.read(new File(scene)), 0, 0, w, h, null);
        } else {
            new ImageCanvas(g, false).gradient(0, 0, w, h, 0xFF1A2230, 0xFF0A0C10);
        }
        SKIN = ImageIO.read(new File(System.getProperty("talk.skins", "build/skins"), "wide-" + skin + ".png"));
        ImageCanvas c = new ImageCanvas(g, false);
        c.push();
        c.translate(0, h - com.hex.kagune.control.client.NpcTalkView.UI_HEIGHT);
        com.hex.kagune.control.client.NpcTalkView.draw(c, model, mouseX, mouseY, 1.0);
        c.pop();
        SKIN = null;
        ImageIO.write(image, "png", out);
        System.out.println("wrote " + out);
    }

    /** Mutations shown unlocked in the next menu drawn. */
    static int MUTATIONS;
    /** 6.2.0: Pale's mutations shown unlocked in the next Pale menu drawn, and how long until her revive is ready (ticks). */
    static int PALE_MUTATIONS;
    static long PALE_REVIVE_IN;

    /** The kakugan screen at 1080p, GUI scale 4 (480 x 270). */
    static void kakugan(File out, KakuganEyes eyes, boolean glasses) throws Exception {
        kakugan(out, eyes, glasses, null, false);
    }

    /** The same, with {@code hovered} under the mouse and the One eyed box open if {@code choosing} (3.1.0). */
    static void kakugan(File out, KakuganEyes eyes, boolean glasses, String hovered, boolean choosing) throws Exception {
        int w = 480;
        int h = 270;
        BufferedImage image = new BufferedImage(w * 3, h * 3, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = graphics(image, 3);
        ImageCanvas c = new ImageCanvas(g, glasses);
        KakuganScreenLayout layout = KakuganScreenLayout.of(w, h);
        long covered = 0L;
        for (int v = 3; glasses && v <= 5; v++) {
            for (int u = 0; u < 8; u++) {
                boolean frame = v == 3 || u == 0 || u == 7 || (v == 4 && (u == 3 || u == 4));
                boolean lens = v >= 4 && (u == 1 || u == 2 || u == 5 || u == 6);
                if (frame || lens) {
                    covered |= com.hex.kagune.control.kakugan.KakuganSkin.bit(u, v);
                }
            }
        }
        KakuganView.draw(c, layout, new KakuganView.State(eyes, 0, eyes.mirrored(), true, hovered, false, 1.0, covered, 0L, true, choosing));
        ImageIO.write(image, "png", out);
        System.out.println("wrote " + out);
    }

    /** 4.1.66: the cookbook in its box over a scene; the mouse on the first area named {@code over} ("slot": the recipe's first slot). */
    static void cookbook(File out, String tab, String picked, String over) throws Exception {
        int w = 960;
        int h = 540;
        BufferedImage image = new BufferedImage(w * 2, h * 2, BufferedImage.TYPE_INT_RGB);
        ImageCanvas c = new ImageCanvas(graphics(image, 2), false);
        c.gradient(0, 0, w, h * 6 / 10, 0xFF79A6FF, 0xFFBCD6FF);
        c.fill(0, h * 6 / 10, w, h, 0xFF5C9A3C);
        int bw = com.hex.kagune.control.client.CookbookView.BOX_WIDTH;
        int bh = com.hex.kagune.control.client.CookbookView.BOX_HEIGHT;
        com.hex.kagune.control.client.CookbookView.Model model = new com.hex.kagune.control.client.CookbookView.Model(tab, picked);
        double mx = -1;
        double my = -1;
        if ("slot".equals(over)) {
            mx = 244 + 190 + 12 + 9;
            my = 78 + 16 + 13 + 40;
        } else if (over != null) {
            BufferedImage scratch = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            for (java.util.Map.Entry<String, UkakuMenuView.Rect> hit : com.hex.kagune.control.client.CookbookView.draw(new ImageCanvas(graphics(scratch, 1),
                false), model, -1, -1, 1.3).entrySet()) {
                if (hit.getKey().startsWith(over)) {
                    mx = hit.getValue().x() + hit.getValue().width() * 0.6;
                    my = hit.getValue().y() + hit.getValue().height() * 0.5;
                    break;
                }
            }
        }
        c.push();
        c.translate((w - bw) / 2.0F, (h - bh) / 2.0F);
        com.hex.kagune.control.client.CookbookView.draw(c, model, mx, my, 1.3);
        c.pop();
        ImageIO.write(image, "png", out);
        System.out.println("wrote " + out);
    }

    /** 4.1.64: the path choice at a GUI size; the mouse on card {@code over} (-2: on Awaken, -1: nowhere), {@code hold} how far it is held. */
    static void pathChoice(File out, int w, int h, KagunePath selected, int over, int unused, float hold, int zoom) throws Exception {
        BufferedImage image = new BufferedImage(w * zoom, h * zoom, BufferedImage.TYPE_INT_RGB);
        ImageCanvas c = new ImageCanvas(graphics(image, zoom), false);
        c.gradient(0, 0, w, h * 6 / 10, 0xFF79A6FF, 0xFFBCD6FF);
        c.fill(0, h * 6 / 10, w, h, 0xFF5C9A3C);
        com.hex.kagune.control.client.PathChoiceLayout layout = com.hex.kagune.control.client.PathChoiceLayout.of(w, h);
        double mx = -10;
        double my = -10;
        if (over == -2) {
            mx = layout.confirm.centerX();
            my = layout.confirm.y() + 5;
        } else if (over >= 0) {
            mx = layout.cards.get(over).centerX();
            my = layout.cards.get(over).y() + 10;
        }
        com.hex.kagune.control.client.PathChoiceView.draw(c, layout, com.hex.kagune.control.client.PathCards.choosable(),
            new com.hex.kagune.control.client.PathChoiceView.State(selected, mx, my, 1.3, hold));
        ImageIO.write(image, "png", out);
        System.out.println("wrote " + out);
    }

    /** 4.1.62: the kakugan screen over a stand-in scene at a GUI size ({@code w} x {@code h}, drawn {@code zoom} times). */
    static void kakuganBox(File out, int w, int h, KakuganEyes eyes, String hovered, boolean choosing, int zoom) throws Exception {
        BufferedImage image = new BufferedImage(w * zoom, h * zoom, BufferedImage.TYPE_INT_RGB);
        ImageCanvas c = new ImageCanvas(graphics(image, zoom), false);
        c.gradient(0, 0, w, h * 6 / 10, 0xFF79A6FF, 0xFFBCD6FF);
        c.fill(0, h * 6 / 10, w, h, 0xFF5C9A3C);
        for (int i = 0; i < w; i += 40) {
            c.fill(i, h * 6 / 10 - 20 - (i * 37 % 60), i + 40, h * 6 / 10, 0xFF4E8834);
        }
        KakuganScreenLayout layout = KakuganScreenLayout.of(w, h);
        KakuganView.draw(c, layout, new KakuganView.State(eyes, 0, eyes.mirrored(), true, hovered, false, 1.0, 0L, 0L, true, choosing));
        ImageIO.write(image, "png", out);
        System.out.println("wrote " + out);
    }

    /** 1.3.9: the gender screen at a GUI size, with a pick and the mouse over something. */
    static void gender(File out, int w, int h, com.hex.kagune.control.Gender picked, String hovered) throws Exception {
        BufferedImage image = new BufferedImage(w * 2, h * 2, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = graphics(image, 2);
        ImageCanvas c = new ImageCanvas(g, false);
        // 4.1.63: the world round the box (a stand-in scene).
        c.gradient(0, 0, w, h * 6 / 10, 0xFF79A6FF, 0xFFBCD6FF);
        c.fill(0, h * 6 / 10, w, h, 0xFF5C9A3C);
        for (int i = 0; i < w; i += 40) {
            c.fill(i, h * 6 / 10 - 20 - (i * 37 % 60), i + 40, h * 6 / 10, 0xFF4E8834);
        }
        com.hex.kagune.control.client.GenderView.draw(c, new com.hex.kagune.control.client.GenderView.Layout(w, h), picked, hovered, 0.4);
        ImageIO.write(image, "png", out);
        System.out.println("wrote " + out);
    }

    public static void main(String[] args) throws Exception {
        File dir = new File(args.length > 0 ? args[0] : ".");
        dir.mkdirs();
        if (args.length > 1 && args[1].equals("gender")) {
            gender(new File(dir, "gender-unpicked.png"), 480, 270, com.hex.kagune.control.Gender.UNSET, "male");
            gender(new File(dir, "gender-female.png"), 480, 270, com.hex.kagune.control.Gender.FEMALE, "done");
            gender(new File(dir, "gender-small.png"), 320, 200, com.hex.kagune.control.Gender.MALE, null);
            gender(new File(dir, "gender-960.png"), 960, 540, com.hex.kagune.control.Gender.FEMALE, "male");
            return;
        }
        if (args.length > 1 && args[1].equals("quests")) {
            quests(dir);
            return;
        }
        if (args.length > 1 && args[1].equals("talk")) {
            talk(dir);
            return;
        }
        if (args.length > 1 && args[1].equals("village-talk")) {
            villageTalk(dir);
            return;
        }
        if (args.length > 1 && args[1].equals("eto-talk")) {
            etoTalk(dir);
            return;
        }
        if (args.length > 1 && args[1].equals("reputation")) {
            reputationTalk(dir);
            return;
        }
        if (args.length > 1 && args[1].equals("joining")) {
            joiningTalk(dir);
            return;
        }
        if (args.length > 1 && args[1].equals("board")) {
            boardTalk(dir);
            return;
        }
        if (args.length > 1 && args[1].equals("romance")) {
            romanceTalk(dir);
            return;
        }
        if (args.length > 1 && args[1].equals("dates")) {
            datesTalk(dir);
            return;
        }
        if (args.length > 1 && args[1].equals("gifts")) {
            giftsTalk(dir);
            return;
        }
        if (args.length > 1 && args[1].equals("wish")) {
            wishTalk(dir);
            return;
        }
        if (args.length > 1 && args[1].equals("dailies")) {
            dailiesTalk(dir);
            return;
        }
        if (args.length > 1 && args[1].equals("banjou")) {
            banjouTalk(dir);
            return;
        }
        if (args.length > 1 && args[1].equals("king")) {
            king(dir);
            return;
        }
        if (args.length > 1 && args[1].equals("hud")) {
            abilities(new File(dir, "abilities-hud.png"));
            return;
        }
        if (args.length > 1 && args[1].equals("bikaku")) {
            bikakuMenu(new File(dir, "bikaku-menu-120.png"), 120, 0, 0);
            bikakuMenu(new File(dir, "bikaku-menu-500.png"), 500, 380, 345);
            // 3.6.3: its mutations (unlocked by a quest): none yet, and Grand Devouring on, cooling down.
            bikakuMenu(new File(dir, "bikaku-menu-600.png"), 600, 0, 0L, 0, 0);
            bikakuMenu(new File(dir, "bikaku-menu-1000.png"), 1000, 1, 840L, 0, 0);
            // 3.6.7: Potential Unlock too: three times the energy, the bar purple.
            bikakuMenu(new File(dir, "bikaku-menu-potential.png"), 1000, 3, 0L, 0, 0);
            potential(new File(dir, "potential-bars.png"));
            return;
        }
        if (args.length > 1 && args[1].equals("graft")) {
            graftMenu(new File(dir, "graft-menu-300.png"), 300, 0, 0, 0);
            graftMenu(new File(dir, "graft-menu-900.png"), 900, com.hex.kagune.control.graft.GraftMutation.NOT_THAT_EASILY.bit(), 400, 300);
            return;
        }
        if (args.length > 1 && args[1].equals("ukaku")) {
            ukaku(dir);
            return;
        }
        if (args.length > 1 && args[1].equals("rinkaku")) {
            rinkaku(dir);
            return;
        }
        if (args.length > 1 && args[1].equals("bikaku-menu")) {
            bikakuQuest(dir);
            return;
        }
        if (args.length > 1 && args[1].equals("graft-menu")) {
            graftQuest(dir);
            return;
        }
        if (args.length > 1 && args[1].equals("pale-menu")) {
            paleQuest(dir);
            return;
        }
        if (args.length > 1 && args[1].equals("gang-talk")) {
            gangTalk(dir);
            return;
        }
        if (args.length > 1 && args[1].equals("ccg")) {
            ccgTalk(dir);
            return;
        }
        if (args.length > 1 && args[1].equals("anteiku")) {
            anteikuTalk(dir);
            anteikuBar(new File(dir, "anteiku-bar.png"));
            return;
        }
        if (args.length > 1 && args[1].equals("cookbook")) {
            // 4.1.66: the cookbook as the quest menu: a chapter and a dish, a dish's recipe with the pointer on an ingredient, the effects.
            cookbook(new File(dir, "cookbook-basics.png"), "basics", null, null);
            cookbook(new File(dir, "cookbook-bowls.png"), "bowls", "pick:d:" + com.hex.kagune.control.kitchen.Dishes.chapter("bowls").get(3).id(), "slot");
            cookbook(new File(dir, "cookbook-plates.png"), "plates", "pick:d:" + com.hex.kagune.control.kitchen.Dishes.chapter("plates").get(7).id(),
                "pick:d:" + com.hex.kagune.control.kitchen.Dishes.chapter("plates").get(12).id());
            cookbook(new File(dir, "cookbook-effects.png"), com.hex.kagune.control.client.CookbookView.EFFECTS, "pick:e:replenished", "goto:");
            return;
        }
        if (args.length > 1 && args[1].equals("path-choice")) {
            // 4.1.64: the path choice in the quest menu's look: nothing picked, a path picked with the pointer on Awaken, held half way,
            // at GUI scale 2, 3 and 4 of 1080p and the smallest screen.
            KagunePath first = com.hex.kagune.control.client.PathCards.choosable().get(0).path();
            KagunePath second = com.hex.kagune.control.client.PathCards.choosable().get(1).path();
            pathChoice(new File(dir, "path-960.png"), 960, 540, KagunePath.NONE, -1, -1, -1.0F, 2);
            pathChoice(new File(dir, "path-640.png"), 640, 360, second, -2, 0, -1.0F, 3);
            pathChoice(new File(dir, "path-480.png"), 480, 270, first, -2, 0, 0.5F, 4);
            pathChoice(new File(dir, "path-320.png"), 320, 240, second, -1, -1, -1.0F, 4);
            return;
        }
        if (args.length > 1 && args[1].equals("kakugan-menu")) {
            // 4.1.62: the kakugan screen in the quest menu's box, at GUI scale 2 and 4 of 1080p, the which-eye box open, a narrow screen.
            kakuganBox(new File(dir, "kakugan-960.png"), 960, 540, KakuganEyes.DEFAULT, "mirror", false, 2);
            kakuganBox(new File(dir, "kakugan-480.png"), 480, 270, KakuganEyes.DEFAULT, "right", false, 4);
            kakuganBox(new File(dir, "kakugan-choice.png"), 960, 540, KakuganEyes.DEFAULT, "pick-left", true, 2);
            kakuganBox(new File(dir, "kakugan-narrow.png"), 340, 300, KakuganEyes.DEFAULT.withGhoul(KakuganEyes.LEFT), "done", false, 3);
            return;
        }
        menu(new File(dir, "ukaku-menu-0.png"), 0, WingForm.TWIN, 0, 0);
        menu(new File(dir, "ukaku-menu-300.png"), 300, WingForm.TWIN, 330, 248);
        menu(new File(dir, "ukaku-menu-600.png"), 600, WingForm.TWIN, 410, 248);
        MUTATIONS = com.hex.kagune.control.ukaku.UkakuMutation.FIRE_DASH.bit();
        menu(new File(dir, "ukaku-menu-mutations.png"), 760, WingForm.TWIN, 700, 410);
        MUTATIONS = 0;
        paleMenu(new File(dir, "pale-menu-0.png"), 0, false, 0, 0);
        paleMenu(new File(dir, "pale-menu-480.png"), 480, true, 700, 420);
        paleMenu(new File(dir, "pale-menu-520.png"), 520, true, 700, 460);
        PALE_MUTATIONS = com.hex.kagune.control.pale.PaleMutation.NOT_THAT_EASILY.bit();
        PALE_REVIVE_IN = 3745L;
        paleMenu(new File(dir, "pale-menu-mutation.png"), 800, true, 700, 118);
        PALE_MUTATIONS = com.hex.kagune.control.pale.PaleMutation.NOT_THAT_EASILY.bit() | com.hex.kagune.control.pale.PaleMutation.UP_WE_GO.bit();
        paleMenu(new File(dir, "pale-menu-mutations.png"), 900, true, 700, 176);
        PALE_MUTATIONS = 0;
        PALE_REVIVE_IN = 0L;
        kakugan(new File(dir, "kakugan-screen.png"), KakuganEyes.DEFAULT, false);
        kakugan(new File(dir, "kakugan-screen-glasses.png"), KakuganEyes.DEFAULT, true);
        kakugan(new File(dir, "kakugan-one-eyed-pick.png"), KakuganEyes.DEFAULT, false, "pick-left", true);
        kakugan(new File(dir, "kakugan-one-eyed.png"), KakuganEyes.DEFAULT.withGhoul(KakuganEyes.LEFT), false, "one", false);
        rinkakuForms(new File(dir, "rinkaku-forms.png"));
        rinkakuMutations(new File(dir, "rinkaku-mutations.png"));
        rinkakuMutationsAfter(new File(dir, "rinkaku-mutations-after.png"));
        reserveBar(new File(dir, "reserve-bar.png"));
        abilities(new File(dir, "abilities-hud.png"));
    }

    /**
     * The abilities panel above the chat (3.1.0, {@code AbilityHudView}), in place on a screen of the game's GUI size (480 x 300 GUI
     * pixels, drawn at 1.5x), for each path and form, and once with rebound keys.
     */
    static void abilities(File out) throws Exception {
        int tw = 480;
        int th = 300;
        int cols = 3;
        com.hex.kagune.gameplay.KaguneForm seed = com.hex.kagune.gameplay.KaguneForm.RINKAKU_SEED;
        WingForm twin = WingForm.TWIN;
        java.util.Map<com.hex.kagune.control.AbilityGuide.Key, String> keys = new java.util.EnumMap<>(com.hex.kagune.control.AbilityGuide.Key.class);
        String[][] names = {{"ATTACK", "LMB"}, {"USE", "RMB"}, {"TOGGLE", "R"}, {"MENU", "P"}, {"GRAB", "G"}, {"THROW", "N"}, {"RAPID", "Z"},
            {"SLINGSHOT", "J"}, {"CYCLE", "K"}, {"CRUSH", "H"}, {"LIFT", "V"}, {"SLAM", "B"}, {"GUARD", "C"}, {"UNLEASH", "LCtrl"}, {"QUESTS", "U"}};
        for (String[] name : names) {
            keys.put(com.hex.kagune.control.AbilityGuide.Key.valueOf(name[0]), name[1]);
        }
        java.util.Map<com.hex.kagune.control.AbilityGuide.Key, String> rebound = new java.util.EnumMap<>(keys);
        rebound.put(com.hex.kagune.control.AbilityGuide.Key.RAPID, com.hex.kagune.control.AbilityGuide.keyName("key.keyboard.f", "CONTROL"));
        rebound.put(com.hex.kagune.control.AbilityGuide.Key.SLINGSHOT, com.hex.kagune.control.AbilityGuide.keyName("key.mouse.4", "NONE"));
        rebound.put(com.hex.kagune.control.AbilityGuide.Key.GUARD, com.hex.kagune.control.AbilityGuide.keyName("key.keyboard.left.alt", "NONE"));
        com.hex.kagune.control.AbilityGuide.State unleashed = com.hex.kagune.control.AbilityGuide.State.OUT;
        Object[][] tiles = {
            {"Rinkaku, kagune in", new com.hex.kagune.control.AbilityGuide.Input(KagunePath.RINKAKU, com.hex.kagune.control.AbilityGuide.State.CLOSED, 300, seed, twin, false, keys)},
            {"Rinkaku, ghoul eyes (R)", new com.hex.kagune.control.AbilityGuide.Input(KagunePath.RINKAKU, com.hex.kagune.control.AbilityGuide.State.EYES, 300, seed, twin, false, keys)},
            {"Rinkaku, Rinkaku Seed (300)", new com.hex.kagune.control.AbilityGuide.Input(KagunePath.RINKAKU, unleashed, 300, seed, twin, false, keys)},
            {"Rinkaku, Four-Tail (600)", new com.hex.kagune.control.AbilityGuide.Input(KagunePath.RINKAKU, unleashed, 600, com.hex.kagune.gameplay.KaguneForm.RINKAKU_QUAD, twin, false, keys)},
            {"Rinkaku, first kakuja (800)", new com.hex.kagune.control.AbilityGuide.Input(KagunePath.RINKAKU, unleashed, 800, com.hex.kagune.gameplay.KaguneForm.CENTIPEDE, twin, false, keys)},
            {"Rinkaku, final kakuja (1000)", new com.hex.kagune.control.AbilityGuide.Input(KagunePath.RINKAKU, unleashed, 1000, com.hex.kagune.gameplay.KaguneForm.FULL_KAKUJA, twin, false, keys)},
            {"Ukaku, fire wings (120)", new com.hex.kagune.control.AbilityGuide.Input(KagunePath.UKAKU, unleashed, 120, seed, twin, false, keys)},
            {"Ukaku, crystal mode (450)", new com.hex.kagune.control.AbilityGuide.Input(KagunePath.UKAKU, unleashed, 450, seed, twin, true, keys)},
            {"Ukaku, first kakuja (820)", new com.hex.kagune.control.AbilityGuide.Input(KagunePath.UKAKU, unleashed, 820, seed, WingForm.KAKUJA, false, keys)},
            {"Ukaku, final kakuja (1000)", new com.hex.kagune.control.AbilityGuide.Input(KagunePath.UKAKU, unleashed, 1000, seed, WingForm.FINAL_KAKUJA, false, keys)},
            {"Graft, two tails (200)", new com.hex.kagune.control.AbilityGuide.Input(KagunePath.GRAFT, unleashed, 200, seed, twin, false, keys, false,
                com.hex.kagune.control.graft.GraftStage.TRANSPLANT, false)},
            {"Bikaku, the worm out (120)", new com.hex.kagune.control.AbilityGuide.Input(KagunePath.BIKAKU, unleashed, 120, seed, twin, false, keys)},
            {"Bikaku, kagune in", new com.hex.kagune.control.AbilityGuide.Input(KagunePath.BIKAKU, com.hex.kagune.control.AbilityGuide.State.CLOSED, 120, seed, twin, false, keys)},
            {"Rebound keys follow Controls (Ctrl+F, M4, LAlt)", new com.hex.kagune.control.AbilityGuide.Input(KagunePath.RINKAKU, unleashed, 300, seed, twin, false, rebound)},
            {"Undecided path", new com.hex.kagune.control.AbilityGuide.Input(KagunePath.NONE, com.hex.kagune.control.AbilityGuide.State.CLOSED, 0, seed, twin, false, keys)},
            // 1.0.22: War and Peace on the same card, two abilities recharging.
            {"War (J, V recharging)", com.hex.kagune.control.AbilityGuide.king(keys, false),
                new double[] {1, 1, 1, 1, 0.35, 1, 1, 1, 0.8, 1, 1}, new long[] {0, 0, 0, 0, 195, 0, 0, 0, 360, 0, 0}},
            {"Peace (Z, G recharging)", com.hex.kagune.control.AbilityGuide.king(keys, true),
                new double[] {1, 1, 1, 0.5, 1, 0.1, 1, 1, 1, 1, 1}, new long[] {0, 0, 0, 100, 0, 270, 0, 0, 0, 0, 0}}};
        int rows = (tiles.length + cols - 1) / cols;
        float s = 1.5F;
        BufferedImage image = new BufferedImage((int)(cols * tw * s), (int)(rows * (th + 14) * s), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.scale(s, s);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 8));
        ImageCanvas c = new ImageCanvas(g, false);
        for (int i = 0; i < tiles.length; i++) {
            int x0 = (i % cols) * tw;
            int y0 = (i / cols) * (th + 14);
            c.push();
            c.translate(x0, y0);
            c.fill(0, 0, tw, 14, 0xFF101014);
            c.text((String)tiles[i][0], 6, 3, 0xFFEDE3D6, false);
            c.translate(0, 14);
            // A daylit world, the hotbar, two lines of chat.
            c.gradient(0, 0, tw, th * 2 / 3, 0xFF7EA7D6, 0xFFB9D3EA);
            c.gradient(0, th * 2 / 3, tw, th, 0xFF5E8A48, 0xFF3E6230);
            c.fill(tw / 2 - 91, th - 22, tw / 2 + 91, th, 0xA0000000);
            for (int slot = 0; slot < 9; slot++) {
                c.fill(tw / 2 - 90 + slot * 20 + 2, th - 20, tw / 2 - 90 + slot * 20 + 18, th - 2, 0x60FFFFFF);
            }
            String[] chat = {"<Touka> where did the ghoul go", "<Hide> behind you"};
            for (int line = 0; line < chat.length; line++) {
                int y = th - 40 - (chat.length - line) * 9;
                c.fill(2, y, 2 + 320, y + 9, 0x80000000);
                c.text(chat[line], 4, y + 1, 0xFFFFFFFF, false);
            }
            boolean king = tiles[i][1] instanceof com.hex.kagune.control.AbilityGuide.Panel;
            com.hex.kagune.control.client.AbilityHudView.Layout layout = com.hex.kagune.control.client.AbilityHudView.layout(c,
                king ? (com.hex.kagune.control.AbilityGuide.Panel)tiles[i][1]
                    : com.hex.kagune.control.AbilityGuide.panel((com.hex.kagune.control.AbilityGuide.Input)tiles[i][1]), king);
            int chatTop = th - 40 - 90;
            com.hex.kagune.control.client.AbilityHudView.draw(c, layout, 2, chatTop - 4, com.hex.kagune.control.client.AbilityHud.SCALE, king ? (double[])tiles[i][2] : null,
                king ? (long[])tiles[i][3] : null);
            c.pop();
        }
        ImageIO.write(image, "png", out);
        System.out.println("wrote " + out);
    }

    /**
     * The flesh reserve that replaces a ghoul's food bar ({@code ReserveBarView}): each state at 8x, then in place over the hotbar
     * at 4x (hearts, the RC bar and the control line above it as the game lays them out).
     */
    static void reserveBar(File out) throws Exception {
        int w = 700;
        int h = 520;
        BufferedImage image = new BufferedImage(w * 2, h * 2, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = graphics(image, 2);
        ImageCanvas c = new ImageCanvas(g, false);
        c.fill(0, 0, w, h, 0xFF141016);
        c.text("FLESH RESERVE (replaces a ghoul's food bar)", 8, 6, 0xFFF0784A, false);
        Object[][] states = {
            {"Fed, full: calm tail, slit pupil", new com.hex.kagune.control.client.ReserveBarView.Frame(1.0F, 1.0F, 0.0F, 0.0F, 0.0F, 40.0)},
            {"Just ate raw flesh: flash", new com.hex.kagune.control.client.ReserveBarView.Frame(0.8F, 0.8F, 0.0F, 0.7F, 0.0F, 61.0)},
            {"Healing: the pale ghost is what it cost", new com.hex.kagune.control.client.ReserveBarView.Frame(0.55F, 0.8F, 0.8F, 0.0F, 0.1F, 88.0)},
            {"Hungry (40%): writhing, wider pupil, veins", new com.hex.kagune.control.client.ReserveBarView.Frame(0.4F, 0.4F, 0.0F, 0.0F, 0.55F, 120.0)},
            {"Low (15%): the kakuhou beats", new com.hex.kagune.control.client.ReserveBarView.Frame(0.15F, 0.15F, 0.0F, 0.0F, 0.85F, 31.5)},
            {"Empty: dry husk, no regeneration", new com.hex.kagune.control.client.ReserveBarView.Frame(0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 150.0)},
            {"Blink", new com.hex.kagune.control.client.ReserveBarView.Frame(0.9F, 0.9F, 0.0F, 0.0F, 0.0F, 172.0)}};
        for (int k = 0; k < states.length; k++) {
            int x = 10 + (k % 2) * 345;
            int y = 24 + (k / 2) * 92;
            c.text((String)states[k][0], x, y, 0xFFE6D2DA, false);
            c.push();
            c.translate(x, y + 12);
            c.scale(4.0F);
            c.fill(-2, -2, com.hex.kagune.control.client.ReserveBarView.WIDTH + 2, com.hex.kagune.control.client.ReserveBarView.HEIGHT + 2, 0xFF3C5A2E);
            com.hex.kagune.control.client.ReserveBarView.draw(c, 0, 0, (com.hex.kagune.control.client.ReserveBarView.Frame)states[k][1]);
            c.pop();
        }
        // In place, at 4x: a 200 px wide slice of the HUD.
        int sx = 345 + 10;
        int sy = 24 + 3 * 92;
        c.text("In place: hearts | reserve, with the RC bar and control line above", sx, sy, 0xFFE6D2DA, false);
        c.push();
        c.translate(sx - 4, sy + 10);
        c.scale(1.6F);
        hud(c, 0.62F);
        c.pop();
        ImageIO.write(image, "png", out);
        System.out.println("wrote " + out);
    }

    /**
     * 3.6.7, Potential Unlock's purple bar (the owner: "Should be Purple, the bar, my bad"): each bar as it is and with it, at 4x: the flesh
     * reserve, the one bar (energy and food), full, hungry and low. There is no RC bar (the owner: "RC bar IS NOT A THING").
     */
    static void potential(File out) throws Exception {
        int w = 760;
        int h = 250;
        BufferedImage image = new BufferedImage(w * 2, h * 2, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = graphics(image, 2);
        ImageCanvas c = new ImageCanvas(g, false);
        c.fill(0, 0, w, h, 0xFF141016);
        c.text("POTENTIAL UNLOCK: the one bar (energy and food) purple (left as it is, right with Potential Unlock), at 4x", 8, 6, 0xFFC890F0, false);
        float[][] reserve = {{1.0F, 0.0F, 40.0F}, {0.4F, 0.55F, 120.0F}, {0.15F, 0.85F, 31.5F}};
        String[] names = {"full", "hungry (40%)", "low (15%): it beats"};
        for (int k = 0; k < 3; k++) {
            for (int side = 0; side < 2; side++) {
                int x = 10 + side * 380;
                int y = 26 + k * 70;
                c.text((side == 0 ? "Flesh reserve, " : "With Potential Unlock, ") + names[k], x, y, 0xFFE6D2DA, false);
                c.push();
                c.translate(x, y + 12);
                c.scale(4.0F);
                com.hex.kagune.control.client.ui.UiCanvas onto = side == 0 ? c
                    : new com.hex.kagune.control.client.ui.TintedCanvas(c, com.hex.kagune.control.bikaku.BikakuRules.POTENTIAL_PURPLE);
                com.hex.kagune.control.client.ReserveBarView.draw(onto, 0, 0, new com.hex.kagune.control.client.ReserveBarView.Frame(reserve[k][0],
                    reserve[k][0], 0.0F, 0.0F, reserve[k][1], reserve[k][2]));
                c.pop();
            }
        }
        ImageIO.write(image, "png", out);
        System.out.println("wrote " + out);
    }

    static final String[] HEART = {
        ".##...##.",
        "#rr#.#rr#",
        "#rwrrrrr#",
        "#rrrrrrr#",
        ".#rrrrr#.",
        "..#rrr#..",
        "...#r#...",
        "....#....",
        "........."};

    /** A 200 x 60 slice of the survival HUD around the hotbar (centre at x 100), the reserve at {@code fill}. */
    static void hud(ImageCanvas c, float fill) {
        int cx = 100;
        int H = 60;
        c.fill(0, 0, 200, H, 0xFF5E8A48);
        c.fill(0, 0, 200, 20, 0xFF7FA6D8);
        // Hotbar and XP bar.
        c.fill(cx - 91, H - 22, cx + 91, H, 0xFF2A2A2A);
        for (int s = 0; s < 9; s++) {
            c.fill(cx - 90 + s * 20, H - 21, cx - 90 + s * 20 + 20, H - 1, 0xFF8B8B8B);
            c.fill(cx - 89 + s * 20, H - 20, cx - 89 + s * 20 + 18, H - 2, 0xFF3F3F3F);
        }
        c.fill(cx - 91, H - 29, cx + 91, H - 24, 0xFF1A1A1A);
        c.fill(cx - 90, H - 28, cx + 30, H - 25, 0xFF80FF20);
        c.text("12", cx - 5, H - 37, 0xFF80FF20, false);
        // Hearts.
        for (int i = 0; i < 10; i++) {
            int hx = cx - 91 + i * 8;
            int hy = H - 39;
            for (int r = 0; r < 9; r++) {
                for (int q = 0; q < 9; q++) {
                    char ch = HEART[r].charAt(q);
                    if (ch != '.') {
                        int col = ch == '#' ? 0xFF1A0000 : ch == 'w' ? 0xFFFFC8C8 : i < 7 ? 0xFFE01818 : 0xFF3A0E0E;
                        c.fill(hx + q, hy + r, hx + q + 1, hy + r + 1, col);
                    }
                }
            }
        }
        // The original RC bar and the control line above the food row (kagune out).
        int bx = cx + 10;
        c.fill(bx, H - 52, bx + 81, H - 46, 0xFF1A0A0E);
        c.fill(bx + 1, H - 51, bx + 60, H - 47, 0xFFB82038);
        c.fill(bx - 1, H - 44, bx + 82, H - 41, 0xC8000000);
        c.fill(bx + 1, H - 43, bx + 50, H - 42, 0xFFC08A86);
        com.hex.kagune.control.client.ReserveBarView.draw(c, bx, H - 39,
            new com.hex.kagune.control.client.ReserveBarView.Frame(fill, fill, 0.0F, 0.0F, 0.2F, 50.0));
    }

    /**
     * The form buttons of the original Rinkaku menu (KaguneScreen draws them itself, so this copies its geometry and colours: 32 x 15
     * boxes 35 px apart, the id's first three letters) with the build's colour filters ({@code RinkakuForms}) applied.
     */
    /** 1.0.10: the final kakuja's two cards after the Dragon, and the Rinkaku menu in each theme (Rinkaku, War, Peace). */
    static void king(File dir) throws Exception {
        com.hex.kagune.control.king.KingForm none = com.hex.kagune.control.king.KingForm.NONE;
        com.hex.kagune.control.king.KingForm war = com.hex.kagune.control.king.KingForm.WAR;
        com.hex.kagune.control.king.KingForm peace = com.hex.kagune.control.king.KingForm.PEACE;
        // 4.1.65: the cards in the quest menu's box over a stand-in scene (mouse in box coordinates; on the button: 370, 386).
        Object[][] cards = {{"king-cards.png", none, 0.0, -1.0, -1.0, false}, {"king-cards-war.png", war, 0.55, 370.0, 386.0, false},
            {"king-cards-peace.png", peace, 0.0, 550.0, 200.0, true}, {"king-cards-war-hover.png", war, 0.0, 190.0, 200.0, false}};
        for (Object[] shot : cards) {
            int w = 960;
            int h = 540;
            int bw = com.hex.kagune.control.client.KingChoiceView.BOX_WIDTH;
            int bh = com.hex.kagune.control.client.KingChoiceView.BOX_HEIGHT;
            BufferedImage image = new BufferedImage(w * 2, h * 2, BufferedImage.TYPE_INT_RGB);
            ImageCanvas c = new ImageCanvas(graphics(image, 2), false);
            c.gradient(0, 0, w, h * 6 / 10, 0xFF79A6FF, 0xFFBCD6FF);
            c.fill(0, h * 6 / 10, w, h, 0xFF5C9A3C);
            c.push();
            c.translate((w - bw) / 2.0F, (h - bh) / 2.0F);
            com.hex.kagune.control.client.KingChoiceView.draw(c, new com.hex.kagune.control.client.KingChoiceView.Model(
                (com.hex.kagune.control.king.KingForm)shot[1], (Double)shot[2], (Boolean)shot[5]), (Double)shot[3], (Double)shot[4], 1.7);
            c.pop();
            File out = new File(dir, (String)shot[0]);
            ImageIO.write(image, "png", out);
            System.out.println("wrote " + out);
        }
        Object[][] menus = {{"king-menu-rinkaku.png", none, none}, {"king-menu-war.png", war, war}, {"king-menu-peace.png", peace, none}};
        for (Object[] shot : menus) {
            int w = com.hex.kagune.control.client.RinkakuMenuView.BOX_WIDTH;
            int h = com.hex.kagune.control.client.RinkakuMenuView.BOX_HEIGHT;
            BufferedImage image = new BufferedImage(w * 2, h * 2, BufferedImage.TYPE_INT_RGB);
            ImageCanvas c = new ImageCanvas(graphics(image, 2), false);
            com.hex.kagune.control.king.KingForm chosen = (com.hex.kagune.control.king.KingForm)shot[1];
            c.fill(0, 0, w, h, chosen == peace ? 0xFFD8DCE2 : 0xFF10080A);
            com.hex.kagune.control.client.RinkakuMenuView.draw(c, new com.hex.kagune.control.client.RinkakuMenuView.Model(1000, true,
                com.hex.kagune.gameplay.KaguneForm.FULL_KAKUJA, 70.0F, 100.0F, 0L, null, com.hex.kagune.control.client.QuestLocks.ALL,
                new com.hex.kagune.control.client.RinkakuMenuView.King(true, chosen, (com.hex.kagune.control.king.KingForm)shot[2], none)), 0.0, 0.0,
                1.7);
            File out = new File(dir, (String)shot[0]);
            ImageIO.write(image, "png", out);
            System.out.println("wrote " + out);
        }
    }

    static void rinkakuForms(File out) throws Exception {
        int w = 228;
        int h = 178;
        BufferedImage image = new BufferedImage(w * 4, h * 4, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = graphics(image, 4);
        ImageCanvas c = new ImageCanvas(g, false);
        c.fill(0, 0, w, h, 0xFF120814);
        c.text("RINKAKU FORMS (menu buttons, in order)", 8, 6, 0xFFF0784A, false);
        Object[][] rows = {{250, com.hex.kagune.gameplay.KaguneForm.RINKAKU_DUAL}, {500, com.hex.kagune.gameplay.KaguneForm.RINKAKU_QUAD},
            {800, com.hex.kagune.gameplay.KaguneForm.CENTIPEDE}, {1000, com.hex.kagune.gameplay.KaguneForm.FULL_KAKUJA}};
        for (int r = 0; r < rows.length; r++) {
            int mastery = (Integer)rows[r][0];
            com.hex.kagune.gameplay.KaguneForm current = (com.hex.kagune.gameplay.KaguneForm)rows[r][1];
            int y = 26 + r * 34;
            c.text("mastery " + mastery + ", in " + current.displayName(), 8, y, 0xFF9E6A5C, false);
            int i = 0;
            for (com.hex.kagune.gameplay.KaguneForm form : com.hex.kagune.control.RinkakuForms.forms()) {
                int x = 8 + i++ * 35;
                boolean unlocked = form.isUnlocked(mastery);
                boolean selected = form == current;
                int fill = com.hex.kagune.control.RinkakuForms.buttonColor(selected ? 0xFFB51E46 : unlocked ? 0xFF5D1830 : 0xFF2E2228, form, selected, unlocked);
                c.fill(x, y + 12, x + 32, y + 27, fill);
                int label = com.hex.kagune.control.RinkakuForms.labelColor(unlocked ? 0xFFF6DDE4 : 0xFF705860, form, unlocked);
                c.centered(form.id().substring(0, Math.min(3, form.id().length())).toUpperCase(), x + 16, y + 15, label);
            }
        }
        StringBuilder ladder = new StringBuilder();
        for (com.hex.kagune.gameplay.KaguneForm form : com.hex.kagune.control.RinkakuForms.forms()) {
            ladder.append(ladder.length() == 0 ? "" : "  ").append(form.id().substring(0, 3).toUpperCase()).append(' ').append(form.requiredMastery());
        }
        c.text(ladder.toString(), 8, h - 14, 0xFFB8A0C8, false);
        ImageIO.write(image, "png", out);
        System.out.println("wrote " + out);
    }

    /** What each of Rinkaku's mutations really does in the code (the board's own descriptions are vaguer). */
    static final java.util.Map<String, String> MUTATION_EFFECTS = java.util.Map.ofEntries(
        java.util.Map.entry("MASS", "+20% swipe & slam, +16% crush, thicker, heavier knockback"),
        java.util.Map.entry("REACH", "+2 grab range, +1.5 reach, longer tails, -10% RC cost, calmer"),
        java.util.Map.entry("AGILITY", "attacks recover 25% faster, faster regeneration"),
        java.util.Map.entry("ARM_WRAP", "+28% damage on swipes within 4 blocks (arm wraps)"),
        java.util.Map.entry("AUTO_PROJECTILE", "always on: a tail slaps projectiles away, costs RC"),
        java.util.Map.entry("GUARD_MEMBRANE", "unlocks the guard on C (no guard without it)"),
        java.util.Map.entry("MULTI_CONTROL", "other tails follow up hits within 3 blocks, calmer"),
        java.util.Map.entry("CENTIPEDE_BODY", "legs forms: -6% damage taken, +12% slam, stiffer"),
        java.util.Map.entry("REGEN_RESERVOIR", "regeneration starts sooner after a hit"),
        java.util.Map.entry("PARTIAL_ARMOR", "in a kakuja: -10% damage taken, thicker"),
        java.util.Map.entry("RANGED_ORGAN", "better Auto Projectile: cheaper, +35% deflect damage"),
        java.util.Map.entry("GRAPPLER_FRAME", "+1 grab range, +28% slam, +18% crush"),
        java.util.Map.entry("FULL_MASS", "+25% swipe & slam, -10% damage taken, thicker"),
        java.util.Map.entry("FULL_HUNTER", "Threat Auto Attack toggle, +10% swipe, +0.75 reach, -18% cooldowns"),
        java.util.Map.entry("FULL_GRAPPLER", "+2 grab range, +8% swipe, +35% slam, +25% crush, -4% damage taken"));

    /**
     * Rinkaku's mutation board as the original menu draws it (KaguneScreen.drawMutationTree: three 106 x 38 nodes a tier, 113 px
     * apart, 68 px between tiers, its own colours; one pick per tier locks the other two, each tier needs a pick from the one
     * before), for a sample player at mastery 600 who took Mass & Impact, Guard Membrane and the Reservoir; beside it, what each
     * one really does.
     */
    static void rinkakuMutations(File out) throws Exception {
        int w = 760;
        int h = 430;
        BufferedImage image = new BufferedImage(w * 2, h * 2, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = graphics(image, 2);
        ImageCanvas c = new ImageCanvas(g, false);
        c.fill(0, 0, w, h, 0xFF120814);
        int left = 12;
        int top = 10;
        int mastery = 600;
        java.util.Set<String> owned = java.util.Set.of("MASS", "GUARD_MEMBRANE", "REGEN_RESERVOIR");
        c.text("MUTATION PATHS", left, top, -1676152, false);
        c.text("Permanent \u00b7 click twice to commit", left + 104, top, -6592908, false);
        com.hex.kagune.gameplay.MutationNode[] nodes = com.hex.kagune.gameplay.MutationNode.values();
        for (int tier = 1; tier <= 5; tier++) {
            int y = top + 29 + (tier - 1) * 68;
            int need = 0;
            for (com.hex.kagune.gameplay.MutationNode node : nodes) {
                if (node.tier() == tier) {
                    need = node.requiredMastery();
                }
            }
            c.text("Tier " + tier + " \u00b7 " + need + " mastery", left, y - 10, -7515294, false);
            boolean tierTaken = false;
            for (com.hex.kagune.gameplay.MutationNode node : nodes) {
                tierTaken |= node.tier() == tier && owned.contains(node.name());
            }
            boolean previousTaken = tier == 1;
            for (com.hex.kagune.gameplay.MutationNode node : nodes) {
                previousTaken |= node.tier() == tier - 1 && owned.contains(node.name());
            }
            int index = 0;
            for (com.hex.kagune.gameplay.MutationNode node : nodes) {
                if (node.tier() != tier) {
                    continue;
                }
                int x = left + index++ * 113;
                boolean has = owned.contains(node.name());
                boolean available = !has && !tierTaken && previousTaken && mastery >= node.requiredMastery();
                int fill = has ? -4113576 : available ? -11593686 : -13819353;
                c.fill(x, y, x + 106, y + 38, -8707008);
                c.fill(x + 2, y + 2, x + 104, y + 36, fill);
                String name = node.displayName().length() > 16 ? node.displayName().substring(0, 16) : node.displayName();
                c.centered(name, x + 53, y + 7, has || available ? -860958 : -9348511);
                c.centered(has ? "COMMITTED" : node.id(), x + 53, y + 22, has ? -31586 : -6461322);
            }
        }
        c.text("Tier choice locks its siblings. /kagune reset starts over.", left, top + 371, -7907227, false);
        c.text("Sample: mastery 600 with Mass & Impact, Guard Membrane and the Reservoir.", left, top + 386, 0xFF9E8A94, false);
        c.text("A Rinkaku ends with 5 of the 15; the other 10 stay locked for good.", left, top + 398, 0xFF9E8A94, false);
        // What each one really does.
        int x = left + 3 * 113 + 14;
        c.text("WHAT EACH ONE REALLY DOES (from the code)", x, top, 0xFFF0784A, false);
        int y = top + 18;
        int lastTier = 0;
        for (com.hex.kagune.gameplay.MutationNode node : nodes) {
            if (node.tier() != lastTier) {
                lastTier = node.tier();
                c.text("Tier " + node.tier() + " (" + node.requiredMastery() + ")", x, y, 0xFFB45A78, false);
                y += 11;
            }
            c.text(node.displayName(), x + 6, y, 0xFFF6DDE4, false);
            y += 9;
            c.text(MUTATION_EFFECTS.getOrDefault(node.name(), node.description()), x + 12, y, 0xFFA898A4, false);
            y += 13;
        }
        ImageIO.write(image, "png", out);
        System.out.println("wrote " + out);
    }

    /**
     * Rinkaku's board after the cut (3.0.3), as the patched menu draws it: only the five kept (RinkakuMutations.nodes), one a
     * tier; nothing locks anything out, so each is available once its mastery is reached. A sample player at mastery 800.
     */
    static void rinkakuMutationsAfter(File out) throws Exception {
        int w = 640;
        int h = 430;
        BufferedImage image = new BufferedImage(w * 2, h * 2, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = graphics(image, 2);
        ImageCanvas c = new ImageCanvas(g, false);
        c.fill(0, 0, w, h, 0xFF120814);
        int left = 12;
        int top = 10;
        int mastery = 800;
        java.util.Set<String> owned = java.util.Set.of("REACH", "CENTIPEDE_BODY");
        c.text("MUTATION PATHS", left, top, -1676152, false);
        c.text("Permanent \u00b7 click twice to commit", left + 104, top, -6592908, false);
        com.hex.kagune.gameplay.MutationNode[] nodes = com.hex.kagune.control.RinkakuMutations.nodes();
        for (int tier = 1; tier <= 5; tier++) {
            int y = top + 29 + (tier - 1) * 68;
            int index = 0;
            for (com.hex.kagune.gameplay.MutationNode node : nodes) {
                if (node.tier() != tier) {
                    continue;
                }
                c.text("Tier " + tier + " \u00b7 " + node.requiredMastery() + " mastery", left, y - 10, -7515294, false);
                int x = left + index++ * 113;
                boolean has = owned.contains(node.name());
                boolean available = !has && mastery >= node.requiredMastery();
                int fill = has ? -4113576 : available ? -11593686 : -13819353;
                c.fill(x, y, x + 106, y + 38, -8707008);
                c.fill(x + 2, y + 2, x + 104, y + 36, fill);
                String name = node.displayName().length() > 16 ? node.displayName().substring(0, 16) : node.displayName();
                c.centered(name, x + 53, y + 7, has || available ? -860958 : -9348511);
                c.centered(has ? "COMMITTED" : node.id(), x + 53, y + 22, has ? -31586 : -6461322);
                c.text(MUTATION_EFFECTS.getOrDefault(node.name(), node.description()), x + 116, y + 14, 0xFFA898A4, false);
            }
        }
        c.text("Every mutation can be taken. /kagune reset starts over.", left, top + 371, -7907227, false);
        c.text("Sample: mastery 800 with Reach & Control and Centipede Body: Auto Projectile and the Reflex Organ are", left, top + 386,
            0xFF9E8A94, false);
        c.text("open (any order), Hunter at 1000. Cut: the other ten. The guard (C) no longer needs a mutation.", left, top + 398,
            0xFF9E8A94, false);
        ImageIO.write(image, "png", out);
        System.out.println("wrote " + out);
    }
}
