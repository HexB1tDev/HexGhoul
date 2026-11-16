package com.hex.kagune.control.ukaku;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * The final kakuja's mask (pure, tested; 1.3.1): the owner's own model ("Black Half Plague Doctor": a curved beak with a broad sculpted
 * ridge, a compact cheek housing round the lower face, a square clip on each cheek), baked by {@code tools/kakuja_mask.py} from their
 * OBJ into {@link #RESOURCE} and drawn on the head in place of the old half mask, in the armor's own colours since 4.2.9 ({@link #colours};
 * {@link #TEXTURE}, the owner's texture, is kept but no longer drawn). Head pixels throughout (x to
 * the player's left, y down from the neck's pivot, z backward; the face at z -4), as the owner's model sits on a player's head.
 */
public final class KakujaMask {
    public static final String RESOURCE = "/assets/hexkagune/models/kakuja_mask.bin";
    /** Its texture (the owner's), under the mod's assets. */
    public static final String TEXTURE = "textures/entity/kakuja_mask.png";
    static final int MAGIC = 0x484B4D31;
    /** Where it grows in from as the kakuja comes on: the middle of the face's lower half, in front of it. */
    static final double[] ANCHOR = {0.0, -1.0, -5.0};

    /** The mesh: per vertex x, y, z (pixels), u, v (0..1), and a unit normal; three vertex indices per triangle. */
    public record Mesh(float[] positions, float[] uvs, float[] normals, int[] triangles) {
        public int vertices() {
            return this.positions.length / 3;
        }

        public int triangleCount() {
            return this.triangles.length / 3;
        }
    }

    private static Mesh mesh;
    private static boolean tried;

    private KakujaMask() {
    }

    /** The mask's mesh from the mod's own jar (read once), or null if it could not be read (the old half mask is drawn then). */
    public static synchronized Mesh mesh() {
        if (!tried) {
            tried = true;
            try (InputStream in = KakujaMask.class.getResourceAsStream(RESOURCE)) {
                mesh = in == null ? null : read(in);
            } catch (IOException | RuntimeException broken) {
                mesh = null;
            }
        }
        return mesh;
    }

    /** Reads a mesh baked by {@code tools/kakuja_mask.py}. */
    public static Mesh read(InputStream stream) throws IOException {
        DataInputStream in = new DataInputStream(new java.io.BufferedInputStream(stream));
        if (in.readInt() != MAGIC) {
            throw new IOException("not a kakuja mask mesh");
        }
        int vertices = in.readInt();
        int triangles = in.readInt();
        if (vertices <= 0 || triangles <= 0 || vertices > 1_000_000 || triangles > 1_000_000) {
            throw new IOException("a kakuja mask mesh of " + vertices + " vertices and " + triangles + " triangles");
        }
        float[] positions = new float[vertices * 3];
        float[] uvs = new float[vertices * 2];
        float[] normals = new float[vertices * 3];
        for (int i = 0; i < vertices; i++) {
            positions[i * 3] = in.readShort() / 1024.0F;
            positions[i * 3 + 1] = in.readShort() / 1024.0F;
            positions[i * 3 + 2] = in.readShort() / 1024.0F;
            uvs[i * 2] = in.readUnsignedShort() / 65535.0F;
            uvs[i * 2 + 1] = in.readUnsignedShort() / 65535.0F;
            float nx = in.readByte();
            float ny = in.readByte();
            float nz = in.readByte();
            float length = (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (length < 1.0E-6F) {
                nx = 0.0F;
                ny = -1.0F;
                nz = 0.0F;
                length = 1.0F;
            }
            normals[i * 3] = nx / length;
            normals[i * 3 + 1] = ny / length;
            normals[i * 3 + 2] = nz / length;
        }
        boolean wide = vertices >= 65536;
        int[] tris = new int[triangles * 3];
        for (int i = 0; i < tris.length; i++) {
            tris[i] = wide ? in.readInt() : in.readUnsignedShort();
            if (tris[i] < 0 || tris[i] >= vertices) {
                throw new IOException("a kakuja mask triangle points past its vertices");
            }
        }
        return new Mesh(positions, uvs, normals, tris);
    }

    /**
     * Where vertex {@code i} of {@code mesh} is drawn, {@code grow} (0..1) of the way grown in and fitted over {@code shell} (head pixels out
     * from the head's middle to what is under its lower face, as {@link KakujaModel#halfMaskShell}): the owner's model sits on the face itself;
     * over a skin's second layer (or 3D Skin Layers' voxels) it is pushed out across and forward by as much, never into it.
     */
    public static void place(Mesh mesh, int i, double grow, double shell, float[] out) {
        double g = smooth(grow);
        double scale = 0.35 + 0.65 * g;
        double push = Math.max(0.0, shell - KakujaModel.FACE);
        double x = mesh.positions[i * 3];
        double y = mesh.positions[i * 3 + 1];
        double z = mesh.positions[i * 3 + 2];
        // Out over the second layer: across its sides and forward of its face, as far as that layer stands off the head.
        if (push > 0.0) {
            x *= (KakujaModel.FACE + push) / KakujaModel.FACE;
            z -= push;
        }
        out[0] = (float)(ANCHOR[0] + (x - ANCHOR[0]) * scale);
        out[1] = (float)(ANCHOR[1] + (y - ANCHOR[1]) * scale);
        out[2] = (float)(ANCHOR[2] + (z - ANCHOR[2]) * scale);
    }

    /**
     * How sharply the surface must bend at a vertex (its normal's turn per pixel along its edges, convex positive) before it takes the
     * armor's bright edge, and how sharply for its full brightness; the same for a crease's darkness.
     */
    static final double RIDGE_FROM = 2.5;
    static final double RIDGE_FULL = 14.0;
    static final double CREASE_FROM = -1.0;
    static final double CREASE_FULL = -8.0;

    private static int[] colours;
    private static Mesh coloured;

    /**
     * 4.2.9 (the owner: "the mask is meant to be the same color as the armor"): one opaque colour per vertex in the kakuja armor's own palette
     * ({@link KakujaModel#chest}'s plates): {@code PLATE_DARK} where the mask faces down or back, {@code PLATE} sideways, the plates' lit blue
     * where it faces forward or up; its convex crests (the beak's ridge, the housing's edges) the plates' bright bevel edge, its creases
     * dark. Drawn on white and tinted like the chest ({@code KaguneTint.recolour}), so it is the armor's colour in every kagune colour.
     */
    public static synchronized int[] colours(Mesh mesh) {
        if (coloured == mesh && colours != null) {
            return colours;
        }
        int n = mesh.vertices();
        float[] p = mesh.positions;
        float[] ns = mesh.normals;
        double[] bend = new double[n];
        int[] edges = new int[n];
        int[] tris = mesh.triangles;
        for (int t = 0; t < tris.length; t += 3) {
            for (int k = 0; k < 3; k++) {
                int i = tris[t + k];
                int j = tris[t + (k + 1) % 3];
                double dx = p[i * 3] - p[j * 3];
                double dy = p[i * 3 + 1] - p[j * 3 + 1];
                double dz = p[i * 3 + 2] - p[j * 3 + 2];
                double length = dx * dx + dy * dy + dz * dz;
                if (length < 1.0E-8) {
                    continue;
                }
                // The normal's turn along the edge over its length: 1/r on a sphere of radius r, positive where the surface is convex.
                double k2 = ((ns[i * 3] - ns[j * 3]) * dx + (ns[i * 3 + 1] - ns[j * 3 + 1]) * dy + (ns[i * 3 + 2] - ns[j * 3 + 2]) * dz) / length;
                bend[i] += k2;
                bend[j] += k2;
                edges[i]++;
                edges[j]++;
            }
        }
        int lit = KakujaModel.mix(KakujaModel.PLATE, KakujaModel.PLATE_HI, 0.35);
        int edgeHi = KakujaModel.mix(KakujaModel.PLATE_HI, KakujaModel.RIM, 0.35);
        int[] out = new int[n];
        for (int i = 0; i < n; i++) {
            // Head pixels: y down, z backward, so forward is -z and up is -y.
            double facing = clamp(0.5 - 0.4 * ns[i * 3 + 2] - 0.45 * ns[i * 3 + 1]);
            int colour = facing < 0.5 ? KakujaModel.mix(KakujaModel.PLATE_DARK, KakujaModel.PLATE, facing * 2.0)
                : KakujaModel.mix(KakujaModel.PLATE, lit, (facing - 0.5) * 2.0);
            double k = edges[i] == 0 ? 0.0 : bend[i] / edges[i];
            if (k > RIDGE_FROM) {
                colour = KakujaModel.mix(colour, edgeHi, smooth((k - RIDGE_FROM) / (RIDGE_FULL - RIDGE_FROM)) * (0.45 + 0.55 * facing));
            } else if (k < CREASE_FROM) {
                colour = KakujaModel.mix(colour, KakujaModel.PLATE_DARK, smooth((k - CREASE_FROM) / (CREASE_FULL - CREASE_FROM)));
            }
            out[i] = 0xFF000000 | colour & 0xFFFFFF;
        }
        colours = out;
        coloured = mesh;
        return out;
    }

    static double clamp(double x) {
        return x < 0.0 ? 0.0 : x > 1.0 ? 1.0 : x;
    }

    /** How opaque it is {@code grow} (0..1) of the way grown in: it darkens in quickly as it grows. */
    public static float alpha(double grow) {
        return (float)smooth(grow / 0.6);
    }

    static double smooth(double t) {
        double x = t < 0.0 ? 0.0 : t > 1.0 ? 1.0 : t;
        return x * x * (3.0 - 2.0 * x);
    }
}
