package com.hex.kagune.control.eto;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * 6.2.4: the One-Eyed Owl's mesh, as tools/owl_smooth.py bakes it smooth out of the user's Blockbench model (art/one_eyed_owl.bbmodel):
 * its bones (each a name, its parent, its pivot in the model's own pixels), and its quads (each a bone's, four corners in the model's
 * pixels, the way the skin faces at each, each corner's place in its texture, whether any of it burns). Read once, off the mod's own
 * jar (or, for the tests and the previews, off the sources).
 *
 * <p>The model's own space throughout: its pixels, x to its left (its front is -z), y up from the ground under it. {@link Pose} turns
 * its bones about their pivots, each carrying its children with it.
 */
public final class OwlMesh {
    /** Where it is among the mod's assets, and (the tests and the previews) in the sources. */
    public static final String RESOURCE = "/assets/hexkagune/models/one_eyed_owl.bin";

    public final String[] names;
    public final int[] parent;
    public final double[][] pivot;
    /**
     * Each quad's bone, corners (twelve floats each: four corners), the way its skin faces at each corner (twelve), texture places
     * (eight: u, v at each corner), burning.
     */
    public final int[] bone;
    public final float[] corners;
    public final float[] normals;
    public final float[] uvs;
    public final boolean[] burns;
    private final float[][] uvAt;
    private final Map<String, Integer> index = new HashMap<>();

    private OwlMesh(String[] names, int[] parent, double[][] pivot, int[] bone, float[] corners, float[] normals, float[] uvs, boolean[] burns) {
        this.names = names;
        this.parent = parent;
        this.pivot = pivot;
        this.bone = bone;
        this.corners = corners;
        this.normals = normals;
        this.uvs = uvs;
        this.burns = burns;
        this.uvAt = new float[bone.length][];
        for (int i = 0; i < bone.length; i++) {
            this.uvAt[i] = java.util.Arrays.copyOfRange(uvs, i * 8, i * 8 + 8);
        }
        for (int i = 0; i < names.length; i++) {
            this.index.putIfAbsent(names[i], i);
        }
    }

    /** How many quads it has. */
    public int quads() {
        return this.bone.length;
    }

    /** Quad {@code i}'s texture places (u, v at each corner; one array kept for it, never to be changed). */
    public float[] uv(int i) {
        return this.uvAt[i];
    }

    /** A bone by its name (-1: no such bone). */
    public int bone(String name) {
        return this.index.getOrDefault(name, -1);
    }

    private static OwlMesh loaded;

    /** The mesh (read the first time it is asked for; if it cannot be read, an empty one, so nothing is drawn rather than anything failing). */
    public static synchronized OwlMesh get() {
        if (loaded == null) {
            loaded = read();
        }
        return loaded;
    }

    static OwlMesh read() {
        try (InputStream in = open()) {
            if (in != null) {
                return read(new DataInputStream(new GZIPInputStream(in)));
            }
        } catch (IOException | RuntimeException e) {
            // (Unreadable: nothing drawn.)
        }
        return new OwlMesh(new String[] {"model"}, new int[] {-1}, new double[][] {{0.0, 0.0, 0.0}}, new int[0], new float[0], new float[0], new float[0],
            new boolean[0]);
    }

    static InputStream open() throws IOException {
        return open(RESOURCE);
    }

    static InputStream open(String resource) throws IOException {
        InputStream in = OwlMesh.class.getResourceAsStream(resource);
        if (in != null) {
            return in;
        }
        File file = new File("src/main/resources" + resource);
        return file.isFile() ? new FileInputStream(file) : null;
    }

    static OwlMesh read(DataInputStream in) throws IOException {
        if (in.readInt() != 0x4F574C34) {
            throw new IOException("not an OWL4 mesh");
        }
        int bones = in.readInt();
        String[] names = new String[bones];
        int[] parent = new int[bones];
        double[][] pivot = new double[bones][3];
        for (int i = 0; i < bones; i++) {
            names[i] = in.readUTF();
            parent[i] = in.readInt();
            for (int k = 0; k < 3; k++) {
                pivot[i][k] = in.readFloat();
            }
            for (int k = 0; k < 3; k++) {
                in.readFloat();
            }
        }
        int attached = in.readInt();
        for (int i = 0; i < attached; i++) {
            in.readUTF();
            for (int k = 0; k < 3; k++) {
                in.readFloat();
            }
        }
        int n = in.readInt();
        int[] bone = new int[n];
        float[] corners = new float[n * 12];
        float[] normals = new float[n * 12];
        float[] uvs = new float[n * 8];
        boolean[] burns = new boolean[n];
        for (int i = 0; i < n; i++) {
            bone[i] = in.readShort();
            for (int k = 0; k < 12; k++) {
                corners[i * 12 + k] = in.readFloat();
            }
            for (int k = 0; k < 12; k++) {
                normals[i * 12 + k] = in.readFloat();
            }
            for (int k = 0; k < 8; k++) {
                uvs[i * 8 + k] = in.readFloat();
            }
            burns[i] = in.readBoolean();
        }
        return new OwlMesh(names, parent, pivot, bone, corners, normals, uvs, burns);
    }

    // ------------------------------------------------------------------ posed

    /**
     * Its bones posed: each turned (and scaled) about its own pivot in its parent's posed frame, carrying its children; or set to a turn
     * of its own in the model ({@link #world}: an arm reaching for where its hand is put). {@link #solve} works out where each is.
     */
    public static final class Pose {
        final OwlMesh mesh;
        final double[][][] local;
        final double[][][] world;
        final double[] scale;
        final double[][] shift;
        final double[][][] rot;
        final double[][] at;
        final double[] k;

        public Pose(OwlMesh mesh) {
            this.mesh = mesh;
            int n = mesh.names.length;
            this.local = new double[n][][];
            this.world = new double[n][][];
            this.scale = new double[n];
            this.shift = new double[n][];
            this.rot = new double[n][][];
            this.at = new double[n][];
            this.k = new double[n];
            java.util.Arrays.fill(this.scale, 1.0);
        }

        /** Turns bone {@code b} by {@code r} about its pivot (after what it is turned already). */
        public void turn(int b, double[][] r) {
            if (b >= 0) {
                this.local[b] = this.local[b] == null ? r : times(r, this.local[b]);
            }
        }

        /** Scales bone {@code b} (and all of it) {@code s} about its pivot. */
        public void scale(int b, double s) {
            if (b >= 0) {
                this.scale[b] *= s;
            }
        }

        /** Moves bone {@code b}'s pivot by {@code d} (the model's pixels, in its parent's posed frame). */
        public void shift(int b, double[] d) {
            if (b >= 0) {
                this.shift[b] = this.shift[b] == null ? d.clone() : OwlModel.add(this.shift[b], d);
            }
        }

        /** Sets bone {@code b}'s turn in the model outright (its parent's turn no longer carried into it). */
        public void world(int b, double[][] r) {
            if (b >= 0) {
                this.world[b] = r;
            }
        }

        /** Works out each bone's place and turn from its parent's (bones come after their parents). */
        public void solve() {
            for (int b = 0; b < this.mesh.names.length; b++) {
                solve(b);
            }
        }

        /** Works out bone {@code b} alone (its parent already worked out): an arm's bones are reached for one after the other. */
        public void solve(int b) {
            int p = this.mesh.parent[b];
            double[] pv = this.mesh.pivot[b];
            double[][] pr = p < 0 ? IDENTITY : this.rot[p];
            double pk = p < 0 ? 1.0 : this.k[p];
            double[] place = p < 0 ? pv.clone() : apply(p, pv);
            if (this.shift[b] != null) {
                place = OwlModel.add(place, OwlModel.mul(OwlMesh.apply(pr, this.shift[b]), pk));
            }
            this.at[b] = place;
            this.rot[b] = this.world[b] != null ? this.world[b] : this.local[b] == null ? pr : times(pr, this.local[b]);
            this.k[b] = pk * this.scale[b];
        }

        /** Where the rest point {@code q} (the model's pixels) of bone {@code b} is, posed. */
        public double[] apply(int b, double[] q) {
            double[] d = {q[0] - this.mesh.pivot[b][0], q[1] - this.mesh.pivot[b][1], q[2] - this.mesh.pivot[b][2]};
            return OwlModel.add(this.at[b], OwlModel.mul(OwlMesh.apply(this.rot[b], d), this.k[b]));
        }

        /** Which way the rest direction {@code d} of bone {@code b} points, posed. */
        public double[] turned(int b, double[] d) {
            return OwlMesh.apply(this.rot[b], d);
        }

        /** Bone {@code b}'s turn in the model, posed. */
        public double[][] rotation(int b) {
            return this.rot[b];
        }

        /** How much bigger bone {@code b} is drawn than at rest. */
        public double size(int b) {
            return this.k[b];
        }
    }

    static final double[][] IDENTITY = {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};

    /** A turn of {@code angle} radians about the unit {@code axis}, right-handed. */
    public static double[][] about(double[] axis, double angle) {
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        double t = 1.0 - c;
        double x = axis[0];
        double y = axis[1];
        double z = axis[2];
        return new double[][] {{t * x * x + c, t * x * y - s * z, t * x * z + s * y}, {t * x * y + s * z, t * y * y + c, t * y * z - s * x},
            {t * x * z - s * y, t * y * z + s * x, t * z * z + c}};
    }

    /** The turns about x, y and z (radians), one after the other in that order (as Blockbench turns a bone). */
    public static double[][] euler(double x, double y, double z) {
        return times(about(new double[] {0, 0, 1}, z), times(about(new double[] {0, 1, 0}, y), about(new double[] {1, 0, 0}, x)));
    }

    /** The least turn taking the direction {@code from} to {@code to}. */
    public static double[][] arc(double[] from, double[] to) {
        double[] a = OwlModel.normalize(from);
        double[] b = OwlModel.normalize(to);
        double[] axis = OwlModel.cross(a, b);
        double sin = Math.sqrt(OwlModel.dot(axis, axis));
        double cos = OwlModel.dot(a, b);
        if (sin < 1.0E-9) {
            if (cos > 0.0) {
                return IDENTITY;
            }
            return about(OwlModel.normalize(OwlModel.square(a)), Math.PI);
        }
        return about(OwlModel.mul(axis, 1.0 / sin), Math.atan2(sin, cos));
    }

    public static double[][] times(double[][] a, double[][] b) {
        double[][] r = new double[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                r[i][j] = a[i][0] * b[0][j] + a[i][1] * b[1][j] + a[i][2] * b[2][j];
            }
        }
        return r;
    }

    public static double[] apply(double[][] m, double[] v) {
        return new double[] {m[0][0] * v[0] + m[0][1] * v[1] + m[0][2] * v[2], m[1][0] * v[0] + m[1][1] * v[1] + m[1][2] * v[2],
            m[2][0] * v[0] + m[2][1] * v[1] + m[2][2] * v[2]};
    }
}
