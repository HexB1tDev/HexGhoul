package com.hex.kagune.control.awakening.geo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A Bedrock/Blockbench {@code .geo.json} model baked exactly as GeckoLib 4 bakes it, so a model
 * edited in Blockbench looks the same here: bone and cube pivots and positions have x negated,
 * rotations become {@code (-x, -y, z)} radians, a cube's origin becomes {@code -(origin.x + size.x)},
 * and faces take GeckoLib's vertex order and box/per-face UV layout.
 *
 * <p>Pure Java: the server reads the same model to know where the blades are.
 */
public final class GeoModel {
    public final String identifier;
    public final int textureWidth;
    public final int textureHeight;
    /** Parents always come before their children. */
    public final Bone[] bones;
    private final Map<String, Integer> index;

    public static final class Bone {
        public final String name;
        public final int parent;
        /** Pivot in model pixels, GeckoLib space (x negated). */
        public final double pivotX;
        public final double pivotY;
        public final double pivotZ;
        /** Rest rotation in radians, GeckoLib space. */
        public final double rotX;
        public final double rotY;
        public final double rotZ;
        public final Cube[] cubes;

        Bone(String name, int parent, double[] pivot, double[] rotation, Cube[] cubes) {
            this.name = name;
            this.parent = parent;
            this.pivotX = -pivot[0];
            this.pivotY = pivot[1];
            this.pivotZ = pivot[2];
            this.rotX = Math.toRadians(-rotation[0]);
            this.rotY = Math.toRadians(-rotation[1]);
            this.rotZ = Math.toRadians(rotation[2]);
            this.cubes = cubes;
        }
    }

    public static final class Cube {
        /** Pivot in model pixels and rotation in radians, GeckoLib space. */
        public final double pivotX;
        public final double pivotY;
        public final double pivotZ;
        public final double rotX;
        public final double rotY;
        public final double rotZ;
        /** Up to six faces; a face with no UV in a per-face model is absent. */
        public final Quad[] quads;
        /** A cube with a zero dimension; its normals are flipped as GeckoLib does. */
        public final boolean flatX;
        public final boolean flatYZ;

        Cube(double[] pivot, double[] rotation, Quad[] quads, double[] size) {
            this.pivotX = -pivot[0];
            this.pivotY = pivot[1];
            this.pivotZ = pivot[2];
            this.rotX = Math.toRadians(-rotation[0]);
            this.rotY = Math.toRadians(-rotation[1]);
            this.rotZ = Math.toRadians(rotation[2]);
            this.quads = quads;
            this.flatX = size[0] == 0.0;
            this.flatYZ = size[1] == 0.0 || size[2] == 0.0;
        }
    }

    /** Four vertices in blocks (GeckoLib space) with their texture coordinates and a face normal. */
    public static final class Quad {
        public final float[] positions = new float[12];
        public final float[] uvs = new float[8];
        public final float normalX;
        public final float normalY;
        public final float normalZ;

        Quad(double[][] vertices, float[] uv, float nx, float ny, float nz) {
            for (int i = 0; i < 4; i++) {
                positions[i * 3] = (float) vertices[i][0];
                positions[i * 3 + 1] = (float) vertices[i][1];
                positions[i * 3 + 2] = (float) vertices[i][2];
            }
            System.arraycopy(uv, 0, uvs, 0, 8);
            normalX = nx;
            normalY = ny;
            normalZ = nz;
        }
    }

    private GeoModel(String identifier, int textureWidth, int textureHeight, Bone[] bones) {
        this.identifier = identifier;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.bones = bones;
        this.index = new HashMap<>();
        for (int i = 0; i < bones.length; i++) index.put(bones[i].name, i);
    }

    /** Index of a bone, or -1. */
    public int bone(String name) {
        Integer i = index.get(name);
        return i == null ? -1 : i;
    }

    public int quadCount() {
        int count = 0;
        for (Bone bone : bones) for (Cube cube : bone.cubes) for (Quad quad : cube.quads) if (quad != null) count++;
        return count;
    }

    /** Reads the first geometry of a {@code .geo.json} file. */
    public static GeoModel read(String json) {
        Map<String, Object> root = Json.map(Json.parse(json), "model");
        List<Object> geometries = Json.list(root.get("minecraft:geometry"), "minecraft:geometry");
        if (geometries.isEmpty()) throw new IllegalArgumentException("model has no geometry");
        Map<String, Object> geometry = Json.map(geometries.get(0), "geometry");
        Map<String, Object> description = Json.map(geometry.get("description"), "description");
        String identifier = String.valueOf(description.get("identifier"));
        int width = (int) Json.number(description.get("texture_width"), "texture_width");
        int height = (int) Json.number(description.get("texture_height"), "texture_height");
        List<Object> rawBones = geometry.get("bones") == null ? List.of() : Json.list(geometry.get("bones"), "bones");

        // Order bones so that every parent is baked before its children.
        Map<String, Map<String, Object>> byName = new LinkedHashMap<>();
        for (Object raw : rawBones) {
            Map<String, Object> bone = Json.map(raw, "bone");
            String name = String.valueOf(bone.get("name"));
            if (byName.put(name, bone) != null) throw new IllegalArgumentException("duplicate bone " + name);
        }
        List<String> order = new ArrayList<>();
        for (String name : byName.keySet()) place(name, byName, order, new ArrayList<>());
        Map<String, Integer> positions = new HashMap<>();
        Bone[] bones = new Bone[order.size()];
        for (int i = 0; i < order.size(); i++) {
            Map<String, Object> raw = byName.get(order.get(i));
            String parentName = raw.get("parent") == null ? null : String.valueOf(raw.get("parent"));
            int parent = parentName == null ? -1 : positions.get(parentName);
            double[] pivot = Json.vector(raw.get("pivot"), 3, new double[3], "bone pivot");
            double[] rotation = Json.vector(raw.get("rotation"), 3, new double[3], "bone rotation");
            boolean mirror = Boolean.TRUE.equals(raw.get("mirror"));
            Double inflate = raw.get("inflate") instanceof Double d ? d : null;
            List<Cube> cubes = new ArrayList<>();
            if (raw.get("cubes") != null) {
                for (Object cube : Json.list(raw.get("cubes"), "cubes")) cubes.add(cube(Json.map(cube, "cube"), width, height, mirror, inflate));
            }
            bones[i] = new Bone(order.get(i), parent, pivot, rotation, cubes.toArray(new Cube[0]));
            positions.put(order.get(i), i);
        }
        return new GeoModel(identifier, width, height, bones);
    }

    private static void place(String name, Map<String, Map<String, Object>> byName, List<String> order, List<String> path) {
        if (order.contains(name)) return;
        if (path.contains(name)) throw new IllegalArgumentException("bone parent cycle at " + name);
        Map<String, Object> bone = byName.get(name);
        if (bone == null) throw new IllegalArgumentException("unknown parent bone " + name);
        path.add(name);
        if (bone.get("parent") != null) place(String.valueOf(bone.get("parent")), byName, order, path);
        path.remove(path.size() - 1);
        order.add(name);
    }

    private enum Face { WEST, EAST, NORTH, SOUTH, UP, DOWN }

    private static final float[][] NORMALS = {{-1, 0, 0}, {1, 0, 0}, {0, 0, -1}, {0, 0, 1}, {0, 1, 0}, {0, -1, 0}};

    private static Cube cube(Map<String, Object> raw, int texWidth, int texHeight, boolean boneMirror, Double boneInflate) {
        double[] size = Json.vector(raw.get("size"), 3, null, "cube size");
        double[] origin = Json.vector(raw.get("origin"), 3, null, "cube origin");
        if (size == null || origin == null) throw new IllegalArgumentException("cube needs origin and size");
        double[] rotation = Json.vector(raw.get("rotation"), 3, new double[3], "cube rotation");
        double[] pivot = Json.vector(raw.get("pivot"), 3, new double[3], "cube pivot");
        boolean mirror = raw.get("mirror") instanceof Boolean b ? b : boneMirror;
        double inflate = (raw.get("inflate") instanceof Double d ? d : boneInflate == null ? 0.0 : boneInflate) / 16.0;
        double ox = -(origin[0] + size[0]) / 16.0;
        double oy = origin[1] / 16.0;
        double oz = origin[2] / 16.0;
        double sx = size[0] / 16.0;
        double sy = size[1] / 16.0;
        double sz = size[2] / 16.0;
        // GeckoLib's VertexSet: "back/front" is x, "left/right" is z.
        double[] bottomLeftBack = {ox - inflate, oy - inflate, oz - inflate};
        double[] bottomRightBack = {ox - inflate, oy - inflate, oz + sz + inflate};
        double[] topLeftBack = {ox - inflate, oy + sy + inflate, oz - inflate};
        double[] topRightBack = {ox - inflate, oy + sy + inflate, oz + sz + inflate};
        double[] topLeftFront = {ox + sx + inflate, oy + sy + inflate, oz - inflate};
        double[] topRightFront = {ox + sx + inflate, oy + sy + inflate, oz + sz + inflate};
        double[] bottomLeftFront = {ox + sx + inflate, oy - inflate, oz - inflate};
        double[] bottomRightFront = {ox + sx + inflate, oy - inflate, oz + sz + inflate};
        double[][] west = {topRightBack, topLeftBack, bottomLeftBack, bottomRightBack};
        double[][] east = {topLeftFront, topRightFront, bottomRightFront, bottomLeftFront};
        double[][] north = {topLeftBack, topLeftFront, bottomLeftFront, bottomLeftBack};
        double[][] south = {topRightFront, topRightBack, bottomRightBack, bottomRightFront};
        double[][] up = {topRightBack, topRightFront, topLeftFront, topLeftBack};
        double[][] down = {bottomLeftBack, bottomLeftFront, bottomRightFront, bottomRightBack};

        Object uv = raw.get("uv");
        boolean box = uv == null || uv instanceof List;
        double[] boxUv = box ? Json.vector(uv, 2, new double[2], "cube uv") : null;
        Map<String, Object> faces = box ? null : Json.map(uv, "cube uv");
        Quad[] quads = new Quad[6];
        for (Face face : Face.values()) {
            double[][] vertices = switch (face) {
                case WEST -> mirror ? east : west;
                case EAST -> mirror ? west : east;
                case NORTH -> north;
                case SOUTH -> south;
                case UP -> mirror && !box ? down : up;
                case DOWN -> mirror && !box ? up : down;
            };
            double u;
            double v;
            double uSize;
            double vSize;
            int turn = 0;
            if (box) {
                double fx = Math.floor(size[0]);
                double fy = Math.floor(size[1]);
                double fz = Math.floor(size[2]);
                double[] at = switch (face) {
                    case WEST -> new double[] {boxUv[0] + fz + fx, boxUv[1] + fz, fz, fy};
                    case EAST -> new double[] {boxUv[0], boxUv[1] + fz, fz, fy};
                    case NORTH -> new double[] {boxUv[0] + fz, boxUv[1] + fz, fx, fy};
                    case SOUTH -> new double[] {boxUv[0] + fz + fx + fz, boxUv[1] + fz, fx, fy};
                    case UP -> new double[] {boxUv[0] + fz, boxUv[1], fx, fz};
                    case DOWN -> new double[] {boxUv[0] + fz + fx, boxUv[1] + fz, fx, -fz};
                };
                u = at[0];
                v = at[1];
                uSize = at[2];
                vSize = at[3];
            } else {
                Object entry = faces.get(face.name().toLowerCase(java.util.Locale.ROOT));
                if (entry == null) continue;
                Map<String, Object> faceUv = Json.map(entry, "face uv");
                double[] at = Json.vector(faceUv.get("uv"), 2, new double[2], "face uv");
                double[] span = Json.vector(faceUv.get("uv_size"), 2, new double[2], "face uv_size");
                u = at[0];
                v = at[1];
                uSize = span[0];
                vSize = span[1];
                turn = faceUv.get("uv_rotation") instanceof Double r ? Math.floorMod((int) Math.round(r), 360) / 90 : 0;
            }
            quads[face.ordinal()] = quad(vertices, u, v, uSize, vSize, turn, texWidth, texHeight, mirror, NORMALS[face.ordinal()]);
        }
        return new Cube(pivot, rotation, quads, size);
    }

    /** GeckoLib's GeoQuad.build: u runs right to left unless mirrored, then optional UV rotation. */
    private static Quad quad(double[][] vertices, double u, double v, double uSize, double vSize, int turn, int texWidth, int texHeight,
                             boolean mirror, float[] normal) {
        float uWidth = (float) ((u + uSize) / texWidth);
        float vHeight = (float) ((v + vSize) / texHeight);
        float u0 = (float) (u / texWidth);
        float v0 = (float) (v / texHeight);
        float nx = normal[0];
        if (!mirror) {
            float swap = uWidth;
            uWidth = u0;
            u0 = swap;
        } else {
            nx = -nx;
        }
        float[] uv = switch (turn) {
            case 1 -> new float[] {uWidth, v0, uWidth, vHeight, u0, vHeight, u0, v0};
            case 2 -> new float[] {uWidth, vHeight, u0, vHeight, u0, v0, uWidth, v0};
            case 3 -> new float[] {u0, vHeight, u0, v0, uWidth, v0, uWidth, vHeight};
            default -> new float[] {u0, v0, uWidth, v0, uWidth, vHeight, u0, vHeight};
        };
        return new Quad(vertices, uv, nx, normal[1], normal[2]);
    }
}
