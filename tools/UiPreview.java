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
                // 7.0.2: a real skin's face (u 8..16, v 8..16).
                for (int v = 0; v < 8; v++) {
                    for (int u = 0; u < 8; u++) {
                        this.fill(x + u * cell, y + v * cell, x + (u + 1) * cell, y + (v + 1) * cell, 0xFF000000 | SKIN.getRGB(8 + u, 8 + v));
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
                for (int v = 0; v < 8; v++) {
                    for (int u = 0; u < 8; u++) {
                        int argb = SKIN.getRGB(40 + u, 8 + v);
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
        int w = 960;
        int h = 540;
        BufferedImage image = new BufferedImage(w * 2, h * 2, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = graphics(image, 2);
        ImageCanvas c = new ImageCanvas(g, false);
        c.gradient(0, 0, w, h, 0xE8070A08, 0xF0160A0B);
        float scale = Math.min(1.0F, Math.min((w - 12.0F) / UkakuMenuView.UI_WIDTH, (h - 12.0F) / UkakuMenuView.UI_HEIGHT));
        float ox = (w - UkakuMenuView.UI_WIDTH * scale) * 0.5F;
        float oy = (h - UkakuMenuView.UI_HEIGHT * scale) * 0.5F;
        c.push();
        c.translate(ox, oy);
        c.scale(scale);
        UkakuMenuView.draw(c, new UkakuMenuView.Model(mastery, true, form, KakuganEyes.DEFAULT, MUTATIONS), mouseX, mouseY, 1.3);
        c.pop();
        ImageIO.write(image, "png", out);
        System.out.println("wrote " + out);
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
        for (int i = 0; i < 37; i++) {
            ghoul.apply(com.hex.kagune.control.quest.QuestEvent.of(com.hex.kagune.control.quest.Signal.STRIKE, 1), KagunePath.GRAFT, 400);
        }
        for (int i = 0; i < 15; i++) {
            ghoul.apply(com.hex.kagune.control.quest.QuestEvent.of(com.hex.kagune.control.quest.Signal.FLIP, 1), KagunePath.GRAFT, 400);
        }
        journal(new File(dir, "quests-ghoul.png"), KagunePath.GRAFT, 400, ghoul, com.hex.kagune.control.client.QuestJournalView.Tab.GHOUL, "light_feet",
            0L);
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
        int w = 960;
        int h = 540;
        BufferedImage image = new BufferedImage(w * 2, h * 2, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = graphics(image, 2);
        ImageCanvas c = new ImageCanvas(g, false);
        c.gradient(0, 0, w, h, 0xE8080506, 0xF0180608);
        c.push();
        c.translate(0, 10);
        com.hex.kagune.control.client.QuestJournalView.draw(c, new com.hex.kagune.control.client.QuestJournalView.Model(path, mastery, log, time, tab, picked),
            0, 0, 1.0);
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

    public static void main(String[] args) throws Exception {
        File dir = new File(args.length > 0 ? args[0] : ".");
        dir.mkdirs();
        if (args.length > 1 && args[1].equals("quests")) {
            quests(dir);
            return;
        }
        if (args.length > 1 && args[1].equals("talk")) {
            talk(dir);
            return;
        }
        if (args.length > 1 && args[1].equals("eto-talk")) {
            etoTalk(dir);
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
        if (args.length > 1 && args[1].equals("graft")) {
            graftMenu(new File(dir, "graft-menu-300.png"), 300, 0, 0, 0);
            graftMenu(new File(dir, "graft-menu-900.png"), 900, com.hex.kagune.control.graft.GraftMutation.NOT_THAT_EASILY.bit(), 400, 300);
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
            com.hex.kagune.control.client.AbilityHudView.draw(c, layout, 2, chatTop - 4, 0.75F, king ? (double[])tiles[i][2] : null,
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
        Object[][] cards = {{"king-cards.png", none, 0.0, 0.0, 0.0, false}, {"king-cards-war.png", war, 0.55, 300.0, 250.0, false},
            {"king-cards-peace.png", peace, 0.0, 660.0, 250.0, true}};
        for (Object[] shot : cards) {
            int w = com.hex.kagune.control.client.KingChoiceView.UI_WIDTH;
            int h = com.hex.kagune.control.client.KingChoiceView.UI_HEIGHT;
            BufferedImage image = new BufferedImage(w * 2, h * 2, BufferedImage.TYPE_INT_RGB);
            ImageCanvas c = new ImageCanvas(graphics(image, 2), false);
            c.fill(0, 0, w, h, 0xFF0A0607);
            com.hex.kagune.control.client.KingChoiceView.draw(c, new com.hex.kagune.control.client.KingChoiceView.Model(
                (com.hex.kagune.control.king.KingForm)shot[1], (Double)shot[2], (Boolean)shot[5]), (Double)shot[3], (Double)shot[4], 1.7);
            File out = new File(dir, (String)shot[0]);
            ImageIO.write(image, "png", out);
            System.out.println("wrote " + out);
        }
        Object[][] menus = {{"king-menu-rinkaku.png", none, none}, {"king-menu-war.png", war, war}, {"king-menu-peace.png", peace, none}};
        for (Object[] shot : menus) {
            int w = com.hex.kagune.control.client.RinkakuMenuView.UI_WIDTH;
            int h = com.hex.kagune.control.client.RinkakuMenuView.UI_HEIGHT;
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
