import com.hex.kagune.control.client.animation.ChildMomentPose;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.family.ChildMoment;
import com.hex.kagune.control.family.ChildRules;
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
 * 1.3.20: the parent-child moments posed off-game ({@link PoseRender}, the game's own transforms): the parent (a stand-in player) and the
 * child (one of the owner's skins) at a moment's telling frame, both on the poses the game gives them ({@link ChildMomentPose}). The child
 * is drawn as a scaled-down player (PoseRender has no little one's big head), at its stage's height. Run {@code python3 build.py} first:
 * <pre>
 * javac -nowarn -cp build/classes:build/base.jar -d build/moment-preview tools/PoseRender.java tools/MomentPreview.java
 * java -Djava.awt.headless=true -cp build/moment-preview:build/classes:build/base.jar MomentPreview &lt;out-dir&gt; &lt;kid skin png&gt;
 * </pre>
 */
public final class MomentPreview {
    record Shot(String label, ChildMoment moment, ChildStage stage, double age, int ride, double camYaw) {
    }

    /** The child's height against a player's (it is drawn as a scaled player). */
    static double height(ChildStage stage) {
        return stage.young ? 20.0 * stage.draw / 32.0 : stage.draw;
    }

    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "build/preview");
        out.mkdirs();
        BufferedImage kidSkin = small(ImageIO.read(new File(args[1])));
        List<Shot> shots = List.of(
            new Shot("hug (toddler)", ChildMoment.HUG, ChildStage.TODDLER, 30, -1, 65),
            new Shot("hug (teen)", ChildMoment.HUG, ChildStage.TEEN, 30, -1, 70),
            new Shot("head pat (child)", ChildMoment.PAT, ChildStage.CHILD, 24, -1, 65),
            new Shot("high five (child)", ChildMoment.HIGH_FIVE, ChildStage.CHILD, 12, -1, 70),
            new Shot("tickle (toddler)", ChildMoment.TICKLE, ChildStage.TODDLER, 30, -1, 55),
            new Shot("peekaboo: hidden", ChildMoment.PEEKABOO, ChildStage.BABY, 10, -1, 60),
            new Shot("peekaboo: boo!", ChildMoment.PEEKABOO, ChildStage.BABY, 28, -1, 60),
            new Shot("toss: thrown up", ChildMoment.TOSS, ChildStage.TODDLER, 11, ChildRules.RIDE_ARMS, 70),
            new Shot("toss: caught", ChildMoment.TOSS, ChildStage.TODDLER, 21, ChildRules.RIDE_ARMS, 70),
            new Shot("airplane spin: a quarter in", ChildMoment.SPIN, ChildStage.TODDLER, 20, ChildRules.RIDE_SPIN, 60),
            new Shot("airplane spin: nearly done", ChildMoment.SPIN, ChildStage.TODDLER, 50, ChildRules.RIDE_SPIN, 60),
            new Shot("on the shoulders", ChildMoment.NONE, ChildStage.CHILD, 0, ChildRules.RIDE_SHOULDERS, 25),
            new Shot("arms up: pick me up!", ChildMoment.ARMS_UP, ChildStage.TODDLER, 20, -1, 65),
            new Shot("wave (child)", ChildMoment.WAVE, ChildStage.CHILD, 14, -1, 60),
            new Shot("clap (baby)", ChildMoment.CLAP, ChildStage.BABY, 9, -1, 60),
            new Shot("a flower for you", ChildMoment.FLOWER, ChildStage.CHILD, 20, -1, 65),
            new Shot("hand in hand (toddler)", ChildMoment.NONE, ChildStage.TODDLER, 0, ChildRules.RIDE_HAND, 20),
            new Shot("hand in hand (child)", ChildMoment.NONE, ChildStage.CHILD, 0, ChildRules.RIDE_HAND, 20),
            new Shot("hand in hand (teen)", ChildMoment.NONE, ChildStage.TEEN, 0, ChildRules.RIDE_HAND, 20),
            new Shot("tucked in", ChildMoment.TUCK, ChildStage.CHILD, 24, -2, 70),
            new Shot("feeding (toddler)", ChildMoment.FEED, ChildStage.TODDLER, 12, -1, 60),
            new Shot("feeding (teen, eating)", ChildMoment.FEED, ChildStage.TEEN, 34, -1, 60));
        int w = 300;
        int h = 340;
        int columns = 5;
        int rows = (shots.size() + columns - 1) / columns;
        BufferedImage sheet = new BufferedImage(columns * w, rows * (h + 22) + 36, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(34, 34, 40));
        g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        g.drawString("Parent and child moments (the parent a stand-in player; the child on its in-game proportions)", 10, 24);
        for (int i = 0; i < shots.size(); i++) {
            Shot shot = shots.get(i);
            BufferedImage image = render(shot, kidSkin, w, h);
            int x = i % columns * w;
            int y = 36 + i / columns * (h + 22);
            g.drawImage(image, x, y, null);
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
            g.drawString(shot.label(), x + 10, y + h + 16);
        }
        g.dispose();
        ImageIO.write(sheet, "png", new File(out, "moments.png"));
    }

    /** Vanilla's player model drawn this much smaller than a block per pixel (PlayerRenderer.scale): the parent's size here. */
    static final double PLAYER = 0.9375;

    /**
     * The child exactly as ChildRenderer draws it: vanilla's player model on its stage's layout (a baby and a toddler on a little one's:
     * a full-size head on a neck at 12, the body, arms and legs half size, ChildRenderer.Model.little), its pose put straight onto the
     * parts as ChildRenderer.Model.turn does (held parts take the pose; the rest add it to vanilla's idle), at its stage's size, against
     * a parent drawn here at a player's (so divided by {@link #PLAYER}). Blocks; its feet at 0, x to its left, z ahead; sitting not
     * applied (the caller drops it).
     */
    static List<PoseRender.Tri> kid(RigPose p, ChildStage stage, boolean slim, boolean sitting) {
        boolean little = stage.young;
        double part = little ? 0.5 : 1.0;
        double px = stage.draw / PLAYER / 16.0;
        // name, pivot (model pixels), scale, rotation, box, skin u/v, second layer u/v, its grow.
        double rightArmX = p.rightArmX + 0.0 * (1.0 - hold(p.armHold, p.rightArmHold)) + (sitting ? -0.5 * (1.0 - hold(p.armHold, p.rightArmHold)) : 0.0);
        double leftArmX = p.leftArmX + (sitting ? -0.5 * (1.0 - hold(p.armHold, p.leftArmHold)) : 0.0);
        double rightArmZ = p.rightArmZ + 0.05 * (1.0 - hold(p.armHold, p.rightArmHold));
        double leftArmZ = p.leftArmZ - 0.05 * (1.0 - hold(p.armHold, p.leftArmHold));
        double legOwn = 1.0 - Math.max(0.0, Math.min(1.0, p.legHold));
        double rightLegX = p.rightLegX + (sitting ? -1.4137167 * legOwn : 0.0);
        double leftLegX = p.leftLegX + (sitting ? -1.4137167 * legOwn : 0.0);
        double rightLegY = p.rightLegY + (sitting ? 0.31415927 * legOwn : 0.0);
        double leftLegY = p.leftLegY + (sitting ? -0.31415927 * legOwn : 0.0);
        double rightLegZ = p.rightLegZ + (sitting ? 0.07853982 * legOwn : 0.0);
        double leftLegZ = p.leftLegZ + (sitting ? -0.07853982 * legOwn : 0.0);
        Object[][] parts = {
            {new double[] {0, little ? 12 : 0, 0}, 1.0, new double[] {p.headX, p.headY, p.headZ}, new double[] {-4, -8, -4}, new double[] {4, 0, 4}, 0, 0, 32, 0, 0.5},
            {new double[] {0, little ? 12 : 0, 0}, part, new double[] {0, 0, 0}, new double[] {-4, 0, -2}, new double[] {4, 12, 2}, 16, 16, 16, 32, 0.25},
            {new double[] {little ? -2.5 : -5, little ? 13 : slim ? 2.5 : 2, 0}, part, new double[] {rightArmX, p.rightArmY, rightArmZ},
                new double[] {slim ? -2 : -3, -2, -2}, new double[] {1, 10, 2}, 40, 16, 40, 32, 0.25},
            {new double[] {little ? 2.5 : 5, little ? 13 : slim ? 2.5 : 2, 0}, part, new double[] {leftArmX, p.leftArmY, leftArmZ},
                new double[] {-1, -2, -2}, new double[] {slim ? 2 : 3, 10, 2}, 32, 48, 48, 48, 0.25},
            {new double[] {little ? -0.95 : -1.9, little ? 18 : 12, little ? 0.05 : 0}, part, new double[] {rightLegX, rightLegY, rightLegZ},
                new double[] {-2, 0, -2}, new double[] {2, 12, 2}, 0, 16, 0, 32, 0.25},
            {new double[] {little ? 0.95 : 1.9, little ? 18 : 12, little ? 0.05 : 0}, part, new double[] {leftLegX, leftLegY, leftLegZ},
                new double[] {-2, 0, -2}, new double[] {2, 12, 2}, 16, 48, 0, 48, 0.25}};
        List<PoseRender.Tri> tris = new ArrayList<>();
        for (Object[] q : parts) {
            double[] pivot = (double[])q[0];
            double scale = (Double)q[1];
            double[] r = (double[])q[2];
            double[] mn = (double[])q[3];
            double[] mx = (double[])q[4];
            PoseRender.Part at = new PoseRender.Part();
            at.rx = r[0];
            at.ry = r[1];
            at.rz = r[2];
            int w = (int)Math.round(mx[0] - mn[0]);
            int h = (int)Math.round(mx[1] - mn[1]);
            int d = (int)Math.round(mx[2] - mn[2]);
            // ModelPart: its pivot, turned, then scaled (uniformly: the same as scaled about the pivot after the turn).
            java.util.function.UnaryOperator<double[]> world = v -> new double[] {(pivot[0] + scale * v[0]) * px, (24.0 - pivot[1] - scale * v[1]) * px,
                -(pivot[2] + scale * v[2]) * px};
            PoseRender.cube(tris, new PoseRender.Cube(mn, mx, 0, PoseRender.textured((Integer)q[5], (Integer)q[6], w, h, d, false)), at, v -> v, world);
            PoseRender.cube(tris, new PoseRender.Cube(mn, mx, 0, PoseRender.textured((Integer)q[7], (Integer)q[8], w, h, d, true)), at, v -> v, world,
                1.0, 1.0, (Double)q[9]);
        }
        return tris;
    }

    static double hold(float all, float one) {
        return Math.max(0.0, Math.min(1.0, Math.max(all, one)));
    }

    static BufferedImage small(BufferedImage hd) {
        BufferedImage out = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        int s = hd.getWidth() / 64;
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                out.setRGB(x, y, hd.getRGB(x * s + s / 2, y * s + s / 2));
            }
        }
        return out;
    }

    static BufferedImage render(Shot shot, BufferedImage kidSkin, int w, int h) {
        // The parent's pose: its side of the moment (a player's: knees bent), or the ride's.
        RigPose parent = new RigPose();
        if (shot.ride() == ChildRules.RIDE_SHOULDERS) {
            ChildMomentPose.blend(parent, ChildMomentPose.shouldersParent(), 1.0F);
        } else if (shot.ride() == ChildRules.RIDE_HAND) {
            ChildMomentPose.blend(parent, ChildMomentPose.handParent(shot.stage(), 1.0), 1.0F);
        } else if (shot.ride() == ChildRules.RIDE_ARMS) {
            parent.rightArmX = -0.95F;
            parent.leftArmX = -0.95F;
            parent.rightArmY = -0.45F;
            parent.leftArmY = 0.45F;
            parent.armHold = 1.0F;
        }
        if (shot.moment() != ChildMoment.NONE && !shot.moment().solo) {
            ChildMomentPose.blend(parent, ChildMomentPose.parent(shot.moment(), shot.age(), shot.stage(), true), 1.0F);
        }
        // The child's: sitting when held in the arms, its moment's or its ride's over that.
        RigPose child = new RigPose();
        if (shot.ride() == ChildRules.RIDE_ARMS || shot.stage().sits()) {
            child.rightLegX = -1.41F;
            child.leftLegX = -1.41F;
            child.rightLegY = 0.31F;
            child.leftLegY = -0.31F;
            child.legHold = 1.0F;
        }
        RigPose moment = shot.moment() == ChildMoment.NONE ? ChildMomentPose.ride(shot.ride(), shot.stage(), 40.0, 1.0, false)
            : ChildMomentPose.child(shot.moment(), shot.age(), shot.stage());
        if (moment != null) {
            ChildMomentPose.blend(child, moment, 1.0F);
        }
        boolean sitting = (shot.ride() >= 0 && shot.ride() != ChildRules.RIDE_HAND) || shot.stage().sits() && shot.ride() != -2;
        java.awt.image.BufferedImage parentSkin = PoseRender.skin;
        PoseRender.skin = kidSkin;
        List<PoseRender.Tri> kid = kid(child, shot.stage(), false, sitting);
        PoseRender.skin = parentSkin;
        // Where the child is (blocks in game, below turned to this drawing's player-sized parent): facing the parent its stand away, or
        // held (in the arms, on the shoulders, out spinning, thrown up), or at their side holding their hand.
        double k = height(shot.stage());
        double sat = sitting ? 0.75 * shot.stage().draw * (shot.stage().young ? 0.5 : 1.0) : 0.0;
        boolean lying = shot.ride() == -2;
        double[] at;
        double turn;
        if (shot.ride() == ChildRules.RIDE_SHOULDERS) {
            at = new double[] {0.0, ChildRules.SHOULDERS_UP - sat, -ChildRules.SHOULDERS_BACK};
            turn = 0.0;
        } else if (shot.ride() == ChildRules.RIDE_SPIN) {
            double a = ChildMoment.spinTurn(shot.age());
            at = new double[] {-Math.sin(a) * ChildRules.SPIN_AHEAD, ChildRules.SPIN_UP - 0.45 * k, Math.cos(a) * ChildRules.SPIN_AHEAD};
            turn = Math.PI - a;
        } else if (shot.ride() == ChildRules.RIDE_ARMS) {
            at = new double[] {0.0, ChildRules.CARRY_UP + ChildMoment.tossLift(shot.age()) - sat, ChildRules.CARRY_AHEAD};
            turn = Math.PI;
        } else if (shot.ride() == ChildRules.RIDE_HAND) {
            at = new double[] {ChildRules.handSide(shot.stage()), 0.0, 0.0};
            turn = 0.0;
        } else if (lying) {
            at = new double[] {-k * PLAYER, 0.58, 0.95};
            turn = 0.0;
        } else {
            double stand = shot.moment().stand > 0.0 ? shot.moment().stand : 1.1;
            at = new double[] {0.0, -sat, stand};
            turn = Math.PI;
        }
        for (int i = 0; i < 3; i++) {
            at[i] /= PLAYER;
        }
        List<PoseRender.Tri> placed = new ArrayList<>();
        double cos = Math.cos(turn);
        double sin = Math.sin(turn);
        for (PoseRender.Tri t : kid) {
            double[][] v = new double[t.v().length][];
            for (int i = 0; i < v.length; i++) {
                double[] q = t.v()[i];
                double x = q[0];
                double y = q[1];
                double z = q[2];
                if (lying) {
                    // On its back in bed across in front of them (they stand at its bedside): head to their left, face up.
                    double lx = y;
                    y = z;
                    z = x;
                    x = lx;
                }
                v[i] = new double[] {x * cos + z * sin + at[0], y + at[1], -x * sin + z * cos + at[2]};
            }
            double[] n = t.n();
            double[] m = n == null ? null : lying ? new double[] {n[1], n[2], n[0]} : n;
            double[] nn = m == null ? null : new double[] {m[0] * cos + m[2] * sin, m[1], -m[0] * sin + m[2] * cos};
            placed.add(new PoseRender.Tri(v, t.rgb(), nn));
        }
        return PoseRender.render(parent, PoseRender.orbit(shot.camYaw(), 12.0, 4.6, 0.95), w, h, placed);
    }
}
