import com.hex.kagune.control.village.DryLand;
import com.hex.kagune.control.village.SitePrep;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import javax.imageio.ImageIO;

/**
 * 4.1.57: a place built on a hillside with trees, the old way (4.1.17 to 4.1.56: lifted onto its footprint's highest ground, the world's
 * blocks left where the template has none, a foundation copied up under its floor) and the new (SitePrep: on its footprint's own floor,
 * its site and a margin round it cleared over that floor before it is placed, trees and all, its apron filled up to the floor), from the
 * mod's own template (its .nbt) and SitePrep's own numbers. Each: a view from above at an angle, and a cut through its middle seen from the
 * side. The game's own terrain shaping round a structure (its beard) is left out: it only ever helps.
 *
 * <p>Usage after a build: {@code java -cp build/quick/classes tools/SitePrepPreview.java <out.png> [template name]}
 */
public class SitePrepPreview {
    static int W = 96;
    static int D = 80;
    static final int H = 120;
    static final int BASE = 40;

    // Block kinds in the voxel world.
    static final int AIR = 0;
    static final int STONE = 1;
    static final int DIRT = 2;
    static final int GRASS = 3;
    static final int LOG = 4;
    static final int LEAVES = 5;
    /** Template blocks: 100 + their palette index. */
    static final int PLACE = 100;

    static int[][][] world;
    static List<String> palette;

    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "site-prep.png");
        String name = args.length > 1 ? args[1] : "gas_hideout";
        Map<String, Object> template = readNbt(new File("src/main/resources/data/hexkagune/structures/" + name + ".nbt"));
        int[] size = ints(template.get("size"));
        palette = new ArrayList<>();
        for (Object entry : (List<?>)template.get("palette")) {
            palette.add((String)((Map<?, ?>)entry).get("Name"));
        }
        List<int[]> blocks = new ArrayList<>();
        for (Object b : (List<?>)template.get("blocks")) {
            Map<?, ?> m = (Map<?, ?>)b;
            int[] pos = ints(m.get("pos"));
            int state = (Integer)m.get("state");
            if (!palette.get(state).endsWith(":air") && !palette.get(state).contains("jigsaw") && !palette.get(state).contains("structure_void")) {
                blocks.add(new int[] {pos[0], pos[1], pos[2], state});
            }
        }
        W = Math.max(96, size[0] + 44);
        D = Math.max(80, size[2] + 40);
        int minX = (W - size[0]) / 2;
        int minZ = (D - size[2]) / 2;
        int maxX = minX + size[0] - 1;
        int maxZ = minZ + size[2] - 1;
        DryLand.Ground ground = new DryLand.Ground() {
            public int surface(int x, int z) {
                return height(x, z);
            }

            public int floor(int x, int z) {
                return height(x, z);
            }

            public boolean wet(int x, int y, int z) {
                return false;
            }
        };
        int startX = minX + size[0] / 2;
        int startZ = minZ + size[2] / 2;

        // Before: lifted onto the highest ground (DryLand.lift), nothing cleared, a foundation copied up under its floor.
        terrain();
        int lift = DryLand.lift(ground, startX, startZ, minX, minZ, maxX, maxZ);
        int oldFloor = height(startX, startZ) - 1 + lift; // the template's ground layer (its anchor, start height -1)
        place(blocks, minX, oldFloor, minZ);
        foundation(blocks, minX, oldFloor, minZ, false);
        int[][][] before = copy();
        String beforeText = "4.1.56: lifted " + lift + " onto its highest ground; the world's blocks left inside it";

        // After: SitePrep's floor, the clearing's tiles laid first, then the place, its foundation and its apron.
        terrain();
        int floor = SitePrep.floor(ground, minX, minZ, maxX, maxZ) - 1;
        int highest = SitePrep.highest(ground, minX - SitePrep.MARGIN, minZ - SitePrep.MARGIN, maxX + SitePrep.MARGIN, maxZ + SitePrep.MARGIN);
        int top = SitePrep.top(highest, floor + size[1], H - 1);
        int tiles = 0;
        for (SitePrep.Tile t : SitePrep.clearing(minX, minZ, maxX, maxZ, 0, 0, floor, top)) {
            tiles++;
            for (int x = t.x(); x < t.x() + SitePrep.CLEAR; x++) {
                for (int y = t.y(); y < t.y() + SitePrep.CLEAR; y++) {
                    for (int z = t.z(); z < t.z() + SitePrep.CLEAR; z++) {
                        set(x, y, z, AIR);
                    }
                }
            }
        }
        place(blocks, minX, floor, minZ);
        foundation(blocks, minX, floor, minZ, true);
        apron(minX, minZ, maxX, maxZ, floor);
        int[][][] after = copy();
        String afterText = "4.1.57: on its own floor; its site cleared first (" + tiles + " tiles of air), trees and all; graded round it";

        int panelW = 900;
        int panelH = 470;
        BufferedImage image = new BufferedImage(panelW * 2, panelH * 2 + 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0x9CC4F0));
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        g.setColor(new Color(0x10202A));
        g.drawString(name + ".nbt on a hillside with trees: before (4.1.56) and after (4.1.57), from the mod's own template and numbers", 20, 34);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        g.drawString("Left out here: the game's own shaping of the ground round a place (its beard), which carves and grasses the ground under"
            + " it first; it only helps. The clearing, the floor, the bank and the fill are the mod's own code (SitePrep).", 20, 60);
        drawPanel(g, before, 0, 80, panelW, panelH, minX, minZ, maxX, maxZ, oldFloor, beforeText, new Color(0xB03030));
        drawPanel(g, after, 0, 80 + panelH, panelW, panelH, minX, minZ, maxX, maxZ, floor, afterText, new Color(0x207040));
        ImageIO.write(image, "png", out);
        System.out.println("wrote " + out + "  old floor " + oldFloor + " (lift " + lift + "), new floor " + floor + ", tiles " + tiles + ", clear to " + top);
        report(before, "before", minX, minZ, maxX, maxZ, oldFloor);
        report(after, "after", minX, minZ, maxX, maxZ, floor);
    }

    /** What is left of the world over the floor in the site, and the floor's columns standing on nothing. */
    static void report(int[][][] w, String label, int minX, int minZ, int maxX, int maxZ, int floor) {
        int natural = 0;
        int trees = 0;
        int hanging = 0;
        int m = SitePrep.MARGIN;
        for (int x = minX - m; x <= maxX + m; x++) {
            for (int z = minZ - m; z <= maxZ + m; z++) {
                int d = SitePrep.distance(x, z, minX, minZ, maxX, maxZ);
                for (int y = d == 0 ? floor + 1 : SitePrep.fill(floor, d) + 1; y < H; y++) {
                    int k = w[x][y][z];
                    if (k != LOG && k != LEAVES && d > 0 && y < SitePrep.cut(floor, d)) {
                        continue; // the bank's own ground
                    }
                    if (k == LOG || k == LEAVES) {
                        trees++;
                    } else if (k != AIR && k < PLACE) {
                        natural++;
                    }
                }
            }
        }
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                int k = w[x][floor][z];
                if (k != AIR && w[x][floor - 1][z] == AIR) {
                    hanging++;
                }
            }
        }
        System.out.println(label + ": world blocks over the floor in the site " + natural + ", tree blocks " + trees + ", floor blocks over air " + hanging);
    }

    // ------------------------------------------------------------------ the world

    /** The hillside: rising 14 blocks across x, a bump, then trees every few blocks. */
    static int height(int x, int z) {
        double h = BASE + 14.0 * x / W + 3.0 * Math.sin(z * 0.11) + 2.0 * Math.cos((x + z) * 0.07);
        return (int)Math.round(h);
    }

    static void terrain() {
        world = new int[W][H][D];
        for (int x = 0; x < W; x++) {
            for (int z = 0; z < D; z++) {
                int h = height(x, z);
                for (int y = 0; y < h; y++) {
                    world[x][y][z] = y < h - 4 ? STONE : y < h - 1 ? DIRT : GRASS;
                }
            }
        }
        java.util.Random random = new java.util.Random(57);
        for (int tx = 4; tx < W - 4; tx += 7) {
            for (int tz = 4; tz < D - 4; tz += 7) {
                int x = tx + random.nextInt(3) - 1;
                int z = tz + random.nextInt(3) - 1;
                if (random.nextFloat() > 0.55F) {
                    continue;
                }
                int h = height(x, z);
                int tall = 5 + random.nextInt(3);
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        for (int dy = tall - 2; dy <= tall + 1; dy++) {
                            if (Math.abs(dx) + Math.abs(dz) + Math.max(0, dy - tall) <= 3 && get(x + dx, h + dy, z + dz) == AIR) {
                                set(x + dx, h + dy, z + dz, LEAVES);
                            }
                        }
                    }
                }
                for (int y = h; y < h + tall; y++) {
                    set(x, y, z, LOG);
                }
            }
        }
    }

    static int get(int x, int y, int z) {
        return x < 0 || z < 0 || y < 0 || x >= W || z >= D || y >= H ? AIR : world[x][y][z];
    }

    static void set(int x, int y, int z, int k) {
        if (x >= 0 && z >= 0 && y >= 0 && x < W && z < D && y < H) {
            world[x][y][z] = k;
        }
    }

    static int[][][] copy() {
        int[][][] c = new int[W][H][D];
        for (int x = 0; x < W; x++) {
            for (int y = 0; y < H; y++) {
                c[x][y] = world[x][y].clone();
            }
        }
        return c;
    }

    /** The template's blocks (its air not saved: the world's left where it has none), its layer 0 at {@code floor}. */
    static void place(List<int[]> blocks, int minX, int floor, int minZ) {
        for (int[] b : blocks) {
            set(minX + b[0], floor + b[1], minZ + b[2], PLACE + b[3]);
        }
    }

    /** ControlStructures.foundation: under each column's floor (its lowest block in its first layers) the ground copied up. */
    static void foundation(List<int[]> blocks, int minX, int floor, int minZ, boolean treesAreGaps) {
        Map<Long, Integer> lowest = new HashMap<>();
        for (int[] b : blocks) {
            if (b[1] <= 1) {
                long key = (long)(minX + b[0]) << 32 | (minZ + b[2]) & 0xFFFFFFFFL;
                lowest.merge(key, floor + b[1], Math::min);
            }
        }
        for (Map.Entry<Long, Integer> column : lowest.entrySet()) {
            int x = (int)(column.getKey() >> 32);
            int z = (int)(long)column.getKey();
            int bottom = column.getValue();
            for (int y = bottom - 1; y >= bottom - 64 && y >= 0; y--) {
                int k = get(x, y, z);
                boolean gap = k == AIR || treesAreGaps && (k == LOG || k == LEAVES);
                if (!gap) {
                    for (int f = y + 1; f < bottom; f++) {
                        set(x, f, z, k);
                    }
                    break;
                }
            }
        }
    }

    /**
     * ControlStructures.apron: the site graded (after the place): round it flat for SitePrep.FLAT, then a bank a block up or down a block
     * (cut into the hill, built up over the dip), no tree left on it; in its footprint a column with nothing at its floor filled up to it.
     */
    static void apron(int minX, int minZ, int maxX, int maxZ, int floor) {
        int m = SitePrep.MARGIN;
        int bankTop = floor + SitePrep.bank();
        for (int x = minX - m; x <= maxX + m; x++) {
            for (int z = minZ - m; z <= maxZ + m; z++) {
                int d = SitePrep.distance(x, z, minX, minZ, maxX, maxZ);
                if (d == 0) {
                    continue; // under the place itself only its foundation (and the game's beard) fills
                } else {
                    int cut = SitePrep.cut(floor, d);
                    int skin = AIR;
                    for (int y = bankTop; y > SitePrep.fill(floor, d); y--) {
                        int k = get(x, y, z);
                        if (k == AIR) {
                            continue;
                        }
                        if (skin == AIR && k != LOG && k != LEAVES) {
                            skin = k;
                        }
                        if (y >= cut || k == LOG || k == LEAVES) {
                            set(x, y, z, AIR);
                        }
                    }
                    int under = get(x, cut - 1, z);
                    if (skin != AIR && under != AIR && under != LOG && under != LEAVES) {
                        set(x, cut - 1, z, skin);
                    }
                }
                int ground = Integer.MIN_VALUE;
                int kind = AIR;
                for (int y = SitePrep.fill(floor, d); y >= floor - 25; y--) {
                    int k = get(x, y, z);
                    if (k != AIR && k != LOG && k != LEAVES && k < PLACE) {
                        ground = y;
                        kind = k;
                        break;
                    }
                }
                int[] fill = SitePrep.apron(floor, d, ground, 24);
                if (fill != null) {
                    for (int y = fill[0]; y <= fill[1]; y++) {
                        set(x, y, z, kind);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ drawing

    static Color colour(int k, int face) {
        Color c;
        if (k >= PLACE) {
            c = blockColour(palette.get(k - PLACE));
        } else {
            c = switch (k) {
                case STONE -> new Color(0x7A7A7A);
                case DIRT -> new Color(0x86603E);
                case GRASS -> face == 0 ? new Color(0x6AAA3E) : new Color(0x86603E);
                case LOG -> new Color(0x5A3E22);
                case LEAVES -> new Color(0x2F6A2A);
                default -> Color.MAGENTA;
            };
        }
        float shade = face == 0 ? 1.0F : face == 1 ? 0.78F : 0.62F;
        return new Color(Math.round(c.getRed() * shade), Math.round(c.getGreen() * shade), Math.round(c.getBlue() * shade));
    }

    static final Map<String, Color> NAMED = new LinkedHashMap<>();

    static {
        String[][] keys = {{"glass", "B8D8E8"}, {"leaves", "2F6A2A"}, {"log", "5A3E22"}, {"wood", "5A3E22"}, {"planks", "A8844E"},
            {"stone_brick", "8A8A8A"}, {"cobble", "6E6E6E"}, {"deepslate", "4A4A50"}, {"stone", "7A7A7A"}, {"andesite", "8A8C8C"},
            {"brick", "9A4A3A"}, {"quartz", "E8E2D8"}, {"white", "E8E8E8"}, {"black", "222226"}, {"gray", "4E4E52"}, {"red", "9A2A2A"},
            {"green", "4A6A2A"}, {"blue", "2A4A9A"}, {"yellow", "D8B83A"}, {"orange", "D86A2A"}, {"brown", "6A4A2A"}, {"iron", "C8C8C8"},
            {"grass_block", "6AAA3E"}, {"dirt", "86603E"}, {"path", "9A8450"}, {"sand", "D8C890"}, {"wool", "D8D8D8"}, {"lantern", "E8B85A"},
            {"terracotta", "9A5A42"}, {"concrete", "8A8A8A"}};
        for (String[] k : keys) {
            NAMED.put(k[0], new Color(Integer.parseInt(k[1], 16)));
        }
    }

    static Color blockColour(String name) {
        for (Map.Entry<String, Color> e : NAMED.entrySet()) {
            if (name.contains(e.getKey())) {
                return e.getValue();
            }
        }
        int h = name.hashCode();
        return new Color(0x70 + (h & 0x3F), 0x60 + (h >> 6 & 0x3F), 0x58 + (h >> 12 & 0x3F));
    }

    /** One scenario: the view from above at an angle (left) and the cut through its middle seen from the side (right). */
    static void drawPanel(Graphics2D g, int[][][] w, int ox, int oy, int pw, int ph, int minX, int minZ, int maxX, int maxZ, int floor, String text,
                          Color ink) {
        g.setColor(new Color(0xEAF2FA));
        g.fillRect(ox + 10, oy + 6, pw * 2 - 20, ph - 12);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        g.setColor(ink);
        g.drawString(text, ox + 24, oy + 32);
        iso(g, w, ox + 20, oy + 40, pw - 40, ph - 60);
        cut(g, w, ox + pw + 10, oy + 40, pw - 40, ph - 60, (minZ + maxZ) / 2, minX, maxX, floor);
    }

    /** Isometric cubes seen from the south-east, back to front (only faces open to the air drawn). */
    static void iso(Graphics2D g, int[][][] w, int ox, int oy, int pw, int ph) {
        int s = Math.max(2, Math.min(5, (pw - 20) / (W + D)));
        int cx = ox + pw / 2;
        int cy = oy + ph - 30;
        int yFrom = BASE - 12;
        for (int x = 0; x < W; x++) {
            for (int z = 0; z < D; z++) {
                for (int y = yFrom; y < H; y++) {
                    int k = w[x][y][z];
                    if (k == AIR) {
                        continue;
                    }
                    boolean up = get(w, x, y + 1, z) == AIR || get(w, x, y + 1, z) == LEAVES && k != LEAVES;
                    boolean south = get(w, x, y, z + 1) == AIR;
                    boolean west = get(w, x - 1, y, z) == AIR;
                    if (!up && !south && !west && z < D - 1 && x > 0) {
                        continue;
                    }
                    // screen: x to the right-down, z to the left-down
                    int sx = cx + (x - z) * s - (W - D) * s / 2;
                    int sy = cy + (x + z) * s / 2 - (y - yFrom) * s - (W + D) * s / 4;
                    int[] topX = {sx, sx + s, sx, sx - s};
                    int[] topY = {sy - s / 2, sy, sy + s / 2, sy};
                    if (up) {
                        g.setColor(colour(k, 0));
                        g.fillPolygon(topX, topY, 4);
                    }
                    if (south || z == D - 1) {
                        g.setColor(colour(k, 1));
                        g.fillPolygon(new int[] {sx - s, sx, sx, sx - s}, new int[] {sy, sy + s / 2, sy + s / 2 + s, sy + s}, 4);
                    }
                    if (west || x == 0 || true) {
                        g.setColor(colour(k, 2));
                        g.fillPolygon(new int[] {sx, sx + s, sx + s, sx}, new int[] {sy + s / 2, sy, sy + s, sy + s / 2 + s}, 4);
                    }
                }
            }
        }
    }

    static int get(int[][][] w, int x, int y, int z) {
        return x < 0 || z < 0 || y < 0 || x >= W || z >= D || y >= H ? AIR : w[x][y][z];
    }

    /** The slice at {@code z} seen from the south, blocks nearer behind it faded: the ground's line, the place's floor, any gap under it. */
    static void cut(Graphics2D g, int[][][] w, int ox, int oy, int pw, int ph, int z, int minX, int maxX, int floor) {
        int s = Math.min(pw / W, (ph - 20) / 50);
        int yTop = floor + 34;
        int yBottom = floor - 14;
        for (int x = 0; x < W; x++) {
            for (int y = yBottom; y <= yTop; y++) {
                int k = w[x][y][z];
                int depth = 0;
                if (k == AIR) {
                    for (int d = 1; d <= 6 && k == AIR; d++) {
                        k = get(w, x, y, z - d);
                        depth = d;
                    }
                }
                int px = ox + x * s;
                int py = oy + (yTop - y) * s;
                if (k == AIR) {
                    g.setColor(new Color(0xBFDDF7));
                } else {
                    Color c = colour(k, 1);
                    float fade = depth == 0 ? 1.0F : 0.55F - depth * 0.05F;
                    g.setColor(new Color(Math.round(c.getRed() * fade + 0xBF * (1 - fade)), Math.round(c.getGreen() * fade + 0xDD * (1 - fade)),
                        Math.round(c.getBlue() * fade + 0xF7 * (1 - fade))));
                }
                g.fillRect(px, py, s, s);
            }
        }
        // The place's floor: a line along its ground layer, its footprint's edges.
        g.setColor(new Color(0xD02020));
        int fy = oy + (yTop - floor) * s + s;
        g.drawLine(ox + minX * s, fy, ox + (maxX + 1) * s, fy);
        g.drawLine(ox + minX * s, fy - 4, ox + minX * s, fy + 4);
        g.drawLine(ox + (maxX + 1) * s, fy - 4, ox + (maxX + 1) * s, fy + 4);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        g.setColor(new Color(0x10202A));
        g.drawString("cut through its middle, seen from the side (red: its floor)", ox, oy + ph + 14);
    }

    // ------------------------------------------------------------------ nbt

    static int[] ints(Object list) {
        List<?> l = (List<?>)list;
        int[] out = new int[l.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = (Integer)l.get(i);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> readNbt(File file) throws Exception {
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(new FileInputStream(file)))) {
            int type = in.readByte();
            in.readUTF();
            return (Map<String, Object>)tag(in, type);
        }
    }

    static Object tag(DataInputStream in, int type) throws Exception {
        switch (type) {
            case 1: return (int)in.readByte();
            case 2: return (int)in.readShort();
            case 3: return in.readInt();
            case 4: return in.readLong();
            case 5: return in.readFloat();
            case 6: return in.readDouble();
            case 7: {
                byte[] b = new byte[in.readInt()];
                in.readFully(b);
                return b;
            }
            case 8: return in.readUTF();
            case 9: {
                int inner = in.readByte();
                int n = in.readInt();
                List<Object> out = new ArrayList<>(n);
                for (int i = 0; i < n; i++) {
                    out.add(tag(in, inner));
                }
                return out;
            }
            case 10: {
                Map<String, Object> out = new LinkedHashMap<>();
                while (true) {
                    int t = in.readByte();
                    if (t == 0) {
                        return out;
                    }
                    String name = in.readUTF();
                    out.put(name, tag(in, t));
                }
            }
            case 11: {
                int[] a = new int[in.readInt()];
                for (int i = 0; i < a.length; i++) {
                    a[i] = in.readInt();
                }
                return a;
            }
            case 12: {
                long[] a = new long[in.readInt()];
                for (int i = 0; i < a.length; i++) {
                    a[i] = in.readLong();
                }
                return a;
            }
            default:
                throw new IllegalStateException("tag " + type);
        }
    }
}
