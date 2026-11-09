package com.hex.kagune.control.outfit;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 4.0.0 (The Great Exploration): the owner's outfits and masks as the game draws them (pure, tested), baked by
 * {@code tools/outfit_models.py} from their canonical geometry ({@code art/outfits/}) into {@link #RESOURCE}. Each model is its cubes, each
 * on one of the player model's bones (head, body, arms, legs) and in one armour slot, at its place in the player model's frame (sixteenths
 * of a block, y down), turned about its own pivot, its faces' texture where the game's own box UV puts them ({@code ModelPart.Cube}: the
 * nets from the cube's offset, fractional sizes as they are). Here each cube is made once into its six faces, each four corners relative
 * to its bone's pivot (the bone's model part moves them) with their texture coordinates and the face's normal.
 */
public final class OutfitModels {
    public static final String RESOURCE = "/assets/hexkagune/models/outfits.bin";
    /** The bones, in the baked order. */
    public static final int HEAD = 0;
    public static final int BODY = 1;
    public static final int RIGHT_ARM = 2;
    public static final int LEFT_ARM = 3;
    public static final int RIGHT_LEG = 4;
    public static final int LEFT_LEG = 5;
    /** Each bone's pivot in the player model (sixteenths, y down). */
    public static final float[][] PIVOTS = {{0, 0, 0}, {0, 0, 0}, {-5, 2, 0}, {5, 2, 0}, {-1.9F, 12, 0}, {1.9F, 12, 0}};
    /** The armour slots, in the baked order: helmet, chestplate, leggings, boots. */
    public static final int HELMET = 0;
    public static final int CHESTPLATE = 1;
    public static final int LEGGINGS = 2;
    public static final int BOOTS = 3;

    /**
     * One face: its four corners ({@code x, y, z} each, sixteenths, relative to its bone's pivot, in the order the game winds them), their
     * texture coordinates ({@code u, v} each, 0..1) and its outward normal.
     */
    public record Face(float[] corners, float[] uvs, float[] normal) {
    }

    /** One cube: its bone, its slot, its six faces. */
    public record Cube(int bone, int slot, Face[] faces) {
    }

    /** One model: its id (its texture's name), its texture's size, its cubes. */
    public record Model(String id, int textureWidth, int textureHeight, List<Cube> cubes) {
        /** Its cubes on {@code bone} in {@code slot} (any slot: -1). */
        public List<Cube> on(int bone, int slot) {
            List<Cube> out = new ArrayList<>();
            for (Cube cube : this.cubes) {
                if (cube.bone() == bone && (slot < 0 || cube.slot() == slot)) {
                    out.add(cube);
                }
            }
            return out;
        }
    }

    private static Map<String, Model> loaded;

    private OutfitModels() {
    }

    /** The model {@code id} (an outfit's style, "formal" or "crimson", or a mask's item id), or null if there is none. */
    public static synchronized Model get(String id) {
        if (loaded == null) {
            Map<String, Model> all = new HashMap<>();
            try (InputStream in = OutfitModels.class.getResourceAsStream(RESOURCE)) {
                if (in != null) {
                    all = read(in);
                }
            } catch (IOException | RuntimeException failure) {
                com.hex.kagune.control.Safety.caught(failure, "OutfitModels");
            }
            loaded = all;
        }
        return loaded.get(id);
    }

    /** Every model in a baked stream. */
    public static Map<String, Model> read(InputStream raw) throws IOException {
        DataInputStream in = new DataInputStream(raw);
        byte[] magic = new byte[4];
        in.readFully(magic);
        if (magic[0] != 'H' || magic[1] != 'X' || magic[2] != 'O' || magic[3] != 'F') {
            throw new IOException("not an outfits file");
        }
        int version = in.readUnsignedShort();
        if (version != 1) {
            throw new IOException("outfits file version " + version);
        }
        int count = in.readUnsignedShort();
        Map<String, Model> out = new HashMap<>();
        for (int m = 0; m < count; m++) {
            byte[] name = new byte[in.readUnsignedShort()];
            in.readFully(name);
            String id = new String(name, java.nio.charset.StandardCharsets.UTF_8);
            int tw = in.readUnsignedShort();
            int th = in.readUnsignedShort();
            int cubes = in.readUnsignedShort();
            List<Cube> list = new ArrayList<>(cubes);
            for (int c = 0; c < cubes; c++) {
                int bone = in.readUnsignedByte();
                int slot = in.readUnsignedByte();
                float[] v = new float[14];
                for (int k = 0; k < 14; k++) {
                    v[k] = in.readFloat();
                }
                list.add(cube(bone, slot, v, tw, th));
            }
            out.put(id, new Model(id, tw, th, List.copyOf(list)));
        }
        return out;
    }

    /**
     * A cube's six faces as the game's {@code ModelPart.Cube} makes them, from {@code v}: its least corner (3), size (3), pivot (3), rotation
     * in degrees (3: x, y, z, turned as a model part is: x first, then y, then z) and box-UV offset (2), all in the model's frame.
     */
    static Cube cube(int bone, int slot, float[] v, int tw, int th) {
        float x0 = v[0];
        float y0 = v[1];
        float z0 = v[2];
        float x1 = x0 + v[3];
        float y1 = y0 + v[4];
        float z1 = z0 + v[5];
        float w = v[3];
        float h = v[4];
        float d = v[5];
        float u = v[12];
        float vv = v[13];
        float f4 = u;
        float f5 = u + d;
        float f6 = u + d + w;
        float f7 = u + d + w + w;
        float f8 = u + d + w + d;
        float f9 = u + d + w + d + w;
        float f10 = vv;
        float f11 = vv + d;
        float f12 = vv + d + h;
        float[] p0 = {x0, y0, z0};
        float[] p1 = {x1, y0, z0};
        float[] p2 = {x1, y1, z0};
        float[] p3 = {x0, y1, z0};
        float[] p4 = {x0, y0, z1};
        float[] p5 = {x1, y0, z1};
        float[] p6 = {x1, y1, z1};
        float[] p7 = {x0, y1, z1};
        Face[] faces = {
            face(new float[][] {p5, p1, p2, p6}, f6, f11, f8, f12, tw, th, 1, 0, 0),   // east
            face(new float[][] {p0, p4, p7, p3}, f4, f11, f5, f12, tw, th, -1, 0, 0),  // west
            face(new float[][] {p5, p4, p0, p1}, f5, f10, f6, f11, tw, th, 0, -1, 0),  // down (toward the model's -y: up in the world)
            face(new float[][] {p2, p3, p7, p6}, f6, f11, f7, f10, tw, th, 0, 1, 0),   // up (the model's +y)
            face(new float[][] {p1, p0, p3, p2}, f5, f11, f6, f12, tw, th, 0, 0, -1),  // north (the front)
            face(new float[][] {p4, p5, p6, p7}, f8, f11, f9, f12, tw, th, 0, 0, 1)};  // south
        float rx = (float)Math.toRadians(v[9]);
        float ry = (float)Math.toRadians(v[10]);
        float rz = (float)Math.toRadians(v[11]);
        float[] pivot = PIVOTS[Math.max(0, Math.min(PIVOTS.length - 1, bone))];
        for (int f = 0; f < faces.length; f++) {
            Face face = faces[f];
            float[] corners = face.corners();
            float[] normal = face.normal();
            if (rx != 0.0F || ry != 0.0F || rz != 0.0F) {
                for (int k = 0; k < 4; k++) {
                    float[] p = turn(corners[k * 3] - v[6], corners[k * 3 + 1] - v[7], corners[k * 3 + 2] - v[8], rx, ry, rz);
                    corners[k * 3] = p[0] + v[6];
                    corners[k * 3 + 1] = p[1] + v[7];
                    corners[k * 3 + 2] = p[2] + v[8];
                }
                float[] n = turn(normal[0], normal[1], normal[2], rx, ry, rz);
                System.arraycopy(n, 0, normal, 0, 3);
            }
            for (int k = 0; k < 4; k++) {
                corners[k * 3] -= pivot[0];
                corners[k * 3 + 1] -= pivot[1];
                corners[k * 3 + 2] -= pivot[2];
            }
        }
        return new Cube(bone, slot, faces);
    }

    /** The game's polygon: its corners and the four texture coordinates it gives them ({@code u2,v1}, {@code u1,v1}, {@code u1,v2}, {@code u2,v2}). */
    private static Face face(float[][] points, float u1, float v1, float u2, float v2, int tw, int th, float nx, float ny, float nz) {
        float[] corners = new float[12];
        for (int k = 0; k < 4; k++) {
            System.arraycopy(points[k], 0, corners, k * 3, 3);
        }
        float[] uvs = {u2 / tw, v1 / th, u1 / tw, v1 / th, u1 / tw, v2 / th, u2 / tw, v2 / th};
        return new Face(corners, uvs, new float[] {nx, ny, nz});
    }

    /** ({@code x, y, z}) turned as a model part turns: about x, then y, then z (the game's {@code rotationZYX(z, y, x)}). */
    static float[] turn(float x, float y, float z, float rx, float ry, float rz) {
        double cy = y * Math.cos(rx) - z * Math.sin(rx);
        double cz = y * Math.sin(rx) + z * Math.cos(rx);
        double bx = x * Math.cos(ry) + cz * Math.sin(ry);
        double bz = -x * Math.sin(ry) + cz * Math.cos(ry);
        double ax = bx * Math.cos(rz) - cy * Math.sin(rz);
        double ay = bx * Math.sin(rz) + cy * Math.cos(rz);
        return new float[] {(float)ax, (float)ay, (float)bz};
    }
}
