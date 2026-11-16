package com.hex.kagune.control.ukaku;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * The final kakuja's mask (pure, tested; 1.3.1): the owner's own model ("Black Half Plague Doctor": a curved beak with a broad sculpted
 * ridge, a compact cheek housing round the lower face, a square clip on each cheek), baked by {@code tools/kakuja_mask.py} from their
 * OBJ into {@link #RESOURCE} and drawn textured ({@link #TEXTURE}) on the head in place of the old half mask. Head pixels throughout (x to
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

    /** How opaque it is {@code grow} (0..1) of the way grown in: it darkens in quickly as it grows. */
    public static float alpha(double grow) {
        return (float)smooth(grow / 0.6);
    }

    static double smooth(double t) {
        double x = t < 0.0 ? 0.0 : t > 1.0 ? 1.0 : t;
        return x * x * (3.0 - 2.0 * x);
    }
}
