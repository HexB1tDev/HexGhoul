package com.hex.kagune.control.outfit;

import com.hex.kagune.control.FemaleBody;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 4.1.89 (the owner: "when women are wearing our armor, our CCG armor or the ghoul armor, their breasts literally stick out from the
 * armor, which is very awkward and very awfully putting. So you got to fix that so the armor adapts to their chest."): a coat on a woman
 * takes her bust's shape (pure, tested). Her bust is a wedge on her torso's front ({@link FemaleBody}: from {@code BUST_TOP} down to
 * {@code BUST_BOTTOM}, out to its tip); drawn under a coat, it came out through the coat's front. Now the coat's body cubes are cut into
 * strips across that band ({@link #cutAll}) and everything on the coat's front is pushed out by the bust's own depth at its height
 * ({@link #fitted}): all that lies on or in front of the coat's face ({@link #SURFACE}: its panels' faces, the lapels, the tie, the
 * buttons, the buckles) the whole depth, so it rides the curve; the panels' inner sides less and less toward the torso ({@link #TORSO}:
 * not at all), so each panel thickens over the bust rather than coming away from the body; its back and sides behind the torso's front
 * never. The bust's knots and the strips' edges are the same for every face, so what lies on the coat moves with it, never into it.
 */
public final class OutfitFit {
    /** The torso's front (sixteenths, z back): nothing here or behind it moves. */
    public static final float TORSO = FemaleBody.BUST_FRONT;
    /** On or in front of this (a coat's face is at about -2.45 to -2.5), a point moves the bust's whole depth. */
    public static final float SURFACE = -2.3F;
    /** The strips' height (sixteenths): every face is cut at the same heights from the bust's top, its tip (at rest) on one of them. */
    public static final float STEP = 0.5F;
    /** The fitted coats kept (by model, figure and bounce), at most this many before they are made again. */
    static final int KEPT = 256;

    private static final Map<String, List<OutfitModels.Cube>> CUT = new HashMap<>();
    private static final Map<String, List<OutfitModels.Cube>> FITTED = new HashMap<>();

    private OutfitFit() {
    }

    /**
     * How far (sixteenths, out from the torso) her bust stands at {@code y} (down from the neck): none above its top or below its bottom,
     * the most at its tip ({@code bounce} pixels up off its rest), {@code curves} of the full figure's, straight between (as the wedge).
     */
    public static float depth(float y, float curves, float bounce) {
        float top = FemaleBody.BUST_TOP;
        float tip = FemaleBody.BUST_TIP - bounce;
        float bottom = FemaleBody.BUST_BOTTOM;
        if (!(y > top) || !(y < bottom) || !(curves > 0.0F)) {
            return 0.0F;
        }
        float full = (FemaleBody.BUST_FRONT - FemaleBody.BUST_OUT) * curves;
        return full * (y <= tip ? (y - top) / (tip - top) : (bottom - y) / (bottom - tip));
    }

    /** How much of that depth a point {@code z} (sixteenths, back) takes: all of it on or in front of the coat's face, none on the torso or behind. */
    public static float weight(float z) {
        if (z <= SURFACE) {
            return 1.0F;
        }
        if (z >= TORSO) {
            return 0.0F;
        }
        return (TORSO - z) / (TORSO - SURFACE);
    }

    /** Where a point of the coat at ({@code y}, {@code z}) goes on her: pushed out ({@code z} smaller) by the bust under it. */
    public static float fitZ(float y, float z, float curves, float bounce) {
        return z - depth(y, curves, bounce) * weight(z);
    }

    /**
     * {@code cubes} (a coat's body cubes, {@code key} naming them) on a woman of {@code curves}, her bust's tip {@code bounce} up: cut
     * ({@link #cutAll}) and fitted ({@link #fit}). Kept by her figure and the bounce to a fiftieth of a pixel (far finer than shows), so a
     * woman standing still has her coat made once.
     */
    public static synchronized List<OutfitModels.Cube> fitted(String key, List<OutfitModels.Cube> cubes, float curves, float bounce) {
        int step = Math.round(bounce * 50.0F);
        String id = key + '|' + Math.round(curves * 1000.0F) + '|' + step;
        List<OutfitModels.Cube> kept = FITTED.get(id);
        if (kept == null) {
            if (FITTED.size() >= KEPT) {
                FITTED.clear();
                CUT.clear();
            }
            float quantized = step / 50.0F;
            List<OutfitModels.Cube> cut = CUT.computeIfAbsent(key + '|' + step, k -> cutAll(cubes, quantized));
            kept = fit(cut, curves, quantized);
            FITTED.put(id, kept);
        }
        return kept;
    }

    /**
     * Every face of {@code cubes} that lies in front of the torso and across the bust's band cut at each strip's height ({@link #STEP} from
     * the bust's top to its bottom), at its tip ({@code bounce} up off its rest: the wedge's edge), and where the push starts and ends in
     * depth ({@link #SURFACE}, {@link #TORSO}): each piece flat, so it bends only at its edges, exactly where the bust does; their texture
     * coordinates carried along (a face's texture lies straight across it). Others are left whole.
     */
    public static List<OutfitModels.Cube> cutAll(List<OutfitModels.Cube> cubes, float bounce) {
        List<OutfitModels.Cube> out = new ArrayList<>(cubes.size());
        for (OutfitModels.Cube cube : cubes) {
            List<OutfitModels.Face> faces = new ArrayList<>();
            for (OutfitModels.Face face : cube.faces()) {
                cutFace(face, faces, FemaleBody.BUST_TIP - bounce);
            }
            out.add(new OutfitModels.Cube(cube.bone(), cube.slot(), faces.toArray(new OutfitModels.Face[0])));
        }
        return out;
    }

    /** {@code cut} (from {@link #cutAll} at the same bounce) fitted, not kept: each corner pushed out by the bust under it, each face's normal made again from where its corners went. */
    public static List<OutfitModels.Cube> fit(List<OutfitModels.Cube> cut, float curves, float bounce) {
        List<OutfitModels.Cube> out = new ArrayList<>(cut.size());
        for (OutfitModels.Cube cube : cut) {
            OutfitModels.Face[] faces = new OutfitModels.Face[cube.faces().length];
            for (int i = 0; i < faces.length; i++) {
                OutfitModels.Face face = cube.faces()[i];
                float[] c = face.corners().clone();
                boolean moved = false;
                for (int k = 0; k < 4; k++) {
                    float z = fitZ(c[k * 3 + 1], c[k * 3 + 2], curves, bounce);
                    moved |= z != c[k * 3 + 2];
                    c[k * 3 + 2] = z;
                }
                faces[i] = moved ? new OutfitModels.Face(c, face.uvs(), normal(c, face.normal())) : face;
            }
            out.add(new OutfitModels.Cube(cube.bone(), cube.slot(), faces));
        }
        return out;
    }

    /** A face's outward normal from its corners (its diagonals' cross), on the side {@code was} was; {@code was} if it has no area. */
    static float[] normal(float[] c, float[] was) {
        double ax = c[6] - c[0];
        double ay = c[7] - c[1];
        double az = c[8] - c[2];
        double bx = c[9] - c[3];
        double by = c[10] - c[4];
        double bz = c[11] - c[5];
        double nx = ay * bz - az * by;
        double ny = az * bx - ax * bz;
        double nz = ax * by - ay * bx;
        double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (length < 1.0E-6) {
            return was;
        }
        double sign = nx * was[0] + ny * was[1] + nz * was[2] < 0.0 ? -1.0 : 1.0;
        return new float[] {(float)(sign * nx / length), (float)(sign * ny / length), (float)(sign * nz / length)};
    }

    /** One face cut into flat pieces across the bust's band (if it crosses it in front of the torso), each piece a quad, into {@code out}. */
    static void cutFace(OutfitModels.Face face, List<OutfitModels.Face> out, float tip) {
        float[] c = face.corners();
        float[] uv = face.uvs();
        List<float[]> polygon = new ArrayList<>(4);
        for (int k = 0; k < 4; k++) {
            float[] v = {c[k * 3], c[k * 3 + 1], c[k * 3 + 2], uv[k * 2], uv[k * 2 + 1]};
            // A repeated corner (a triangle drawn as a quad) is one corner.
            if (polygon.isEmpty() || !same(polygon.get(polygon.size() - 1), v) && (k < 3 || !same(polygon.get(0), v))) {
                polygon.add(v);
            }
        }
        if (!crosses(polygon)) {
            out.add(face);
            return;
        }
        List<List<float[]>> pieces = new ArrayList<>();
        pieces.add(polygon);
        for (float y = FemaleBody.BUST_TOP; y <= FemaleBody.BUST_BOTTOM + 1.0E-4F; y += STEP) {
            pieces = split(pieces, 1, y);
        }
        pieces = split(pieces, 1, tip);
        pieces = split(pieces, 2, SURFACE);
        pieces = split(pieces, 2, TORSO);
        if (pieces.size() == 1) {
            out.add(face);
            return;
        }
        for (List<float[]> piece : pieces) {
            // A flat convex piece, as quads fanned from its first corner (a last odd triangle with its third corner repeated).
            for (int i = 1; i + 1 < piece.size(); i += 2) {
                float[] a = piece.get(0);
                float[] b = piece.get(i);
                float[] d = piece.get(i + 1);
                float[] e = i + 2 < piece.size() ? piece.get(i + 2) : d;
                out.add(quad(new float[][] {a, b, d, e}, face.normal()));
            }
        }
    }

    /** True if {@code polygon} has a corner in front of the torso and reaches into the bust's band (between its top and its bottom). */
    static boolean crosses(List<float[]> polygon) {
        float low = Float.MAX_VALUE;
        float high = -Float.MAX_VALUE;
        boolean front = false;
        for (float[] v : polygon) {
            low = Math.min(low, v[1]);
            high = Math.max(high, v[1]);
            front |= v[2] < TORSO - 1.0E-4F;
        }
        return front && high > FemaleBody.BUST_TOP && low < FemaleBody.BUST_BOTTOM;
    }

    /** Each polygon of {@code pieces} split by the plane where coordinate {@code axis} (1: y, 2: z) is {@code at} (one that lies on one side kept whole). */
    static List<List<float[]>> split(List<List<float[]>> pieces, int axis, float at) {
        List<List<float[]>> out = new ArrayList<>(pieces.size() + 2);
        for (List<float[]> polygon : pieces) {
            boolean below = false;
            boolean above = false;
            for (float[] v : polygon) {
                below |= v[axis] < at - 1.0E-4F;
                above |= v[axis] > at + 1.0E-4F;
            }
            if (!below || !above) {
                out.add(polygon);
                continue;
            }
            List<float[]> under = new ArrayList<>();
            List<float[]> over = new ArrayList<>();
            for (int i = 0; i < polygon.size(); i++) {
                float[] p = polygon.get(i);
                float[] q = polygon.get((i + 1) % polygon.size());
                float dp = p[axis] - at;
                float dq = q[axis] - at;
                if (dp <= 1.0E-4F) {
                    under.add(p);
                }
                if (dp >= -1.0E-4F) {
                    over.add(p);
                }
                if (dp < -1.0E-4F && dq > 1.0E-4F || dp > 1.0E-4F && dq < -1.0E-4F) {
                    float t = dp / (dp - dq);
                    float[] r = new float[5];
                    for (int k = 0; k < 5; k++) {
                        r[k] = p[k] + (q[k] - p[k]) * t;
                    }
                    r[axis] = at;
                    under.add(r);
                    over.add(r);
                }
            }
            if (under.size() >= 3) {
                out.add(under);
            }
            if (over.size() >= 3) {
                out.add(over);
            }
        }
        return out;
    }

    private static OutfitModels.Face quad(float[][] v, float[] normal) {
        float[] corners = new float[12];
        float[] uvs = new float[8];
        for (int k = 0; k < 4; k++) {
            corners[k * 3] = v[k][0];
            corners[k * 3 + 1] = v[k][1];
            corners[k * 3 + 2] = v[k][2];
            uvs[k * 2] = v[k][3];
            uvs[k * 2 + 1] = v[k][4];
        }
        return new OutfitModels.Face(corners, uvs, normal);
    }

    private static boolean same(float[] a, float[] b) {
        return Math.abs(a[0] - b[0]) < 1.0E-5F && Math.abs(a[1] - b[1]) < 1.0E-5F && Math.abs(a[2] - b[2]) < 1.0E-5F;
    }
}
