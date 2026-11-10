package com.hex.kagune.control;

import com.hex.kagune.control.outfit.OutfitModels;
import com.hex.kagune.control.outfit.Outfits;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 4.0.0 (The Great Exploration): the owner's outfits and masks: every item has its model and texture, every outfit piece has cubes in its
 * own slot, the masks sit on the head, the box UV is the game's, and a wild ghoul never draws a character's or a faction's mask.
 */
public final class OutfitTests {
    static int passed;
    static final List<String> FAILURES = new ArrayList<>();

    private static void check(boolean condition, String description) {
        if (condition) {
            passed++;
        } else {
            FAILURES.add(description);
        }
    }

    public static void main(String[] args) throws Exception {
        Map<String, OutfitModels.Model> models;
        try (InputStream in = Files.newInputStream(Path.of("src/main/resources" + OutfitModels.RESOURCE))) {
            models = OutfitModels.read(in);
        }
        catalogue(models);
        geometry(models);
        pools();
        if (!FAILURES.isEmpty()) {
            FAILURES.stream().limit(40).forEach(failure -> System.err.println("FAILED: " + failure));
            System.err.println(FAILURES.size() + " of " + (passed + FAILURES.size()) + " checks failed");
            System.exit(1);
        }
        System.out.println("Outfit tests: " + passed + " checks passed");
    }

    static void catalogue(Map<String, OutfitModels.Model> models) {
        check(Outfits.ALL.size() == 8 + 24 + 9, "eight outfit pieces, 24 owner's masks, nine generic: " + Outfits.ALL.size());
        Set<String> ids = new HashSet<>();
        for (Outfits.Piece piece : Outfits.ALL) {
            check(ids.add(piece.id()), piece.id() + ": one item each");
            OutfitModels.Model model = models.get(piece.model());
            check(model != null, piece.id() + ": its model is baked");
            check(Files.exists(Path.of("src/main/resources/assets/hexkagune/" + piece.texture())), piece.id() + ": its texture");
            check(Files.exists(Path.of("src/main/resources/assets/hexkagune/textures/item/" + piece.id() + ".png")), piece.id() + ": its icon");
            check(Files.exists(Path.of("src/main/resources/assets/hexkagune/models/item/" + piece.id() + ".json")), piece.id() + ": its item model");
            if (model == null) {
                continue;
            }
            int own = 0;
            for (OutfitModels.Cube cube : model.cubes()) {
                if (cube.slot() == piece.slot()) {
                    own++;
                }
            }
            check(own > 0, piece.id() + ": cubes in its own slot");
            if (piece.mask()) {
                check(model.cubes().stream().allMatch(c -> c.bone() == OutfitModels.HEAD && c.slot() == OutfitModels.HELMET),
                    piece.id() + ": a mask is all on the head, a helmet");
            }
        }
        check(Outfits.of("formal_chestplate") != null && Outfits.of("formal_chestplate").slot() == OutfitModels.CHESTPLATE, "the overcoat is a chestplate");
        check(Outfits.of("crimson_helmet") != null && !Outfits.of("crimson_helmet").mask(), "the hood is no mask");
        // The owner's counts (their build-verification.txt): the outfits' slots and three masks. 4.0.1: every one of theirs, and the
        // details added (the owner: "add more details to it as well to make it as polished").
        check(count(models.get("formal"), OutfitModels.CHESTPLATE) > 73 && count(models.get("formal"), OutfitModels.HELMET) > 4,
            "the Formal Outfit: the owner's 73 coat cubes and 4 cap cubes, and its details");
        check(count(models.get("crimson"), OutfitModels.HELMET) > 19 && count(models.get("crimson"), OutfitModels.CHESTPLATE) > 45,
            "the Crimson Outfit: the owner's 19 hood cubes and 45 coat cubes, and its details");
        check(models.get("mask_ayato").cubes().size() == 128 && models.get("mask_noro").cubes().size() == 48
            && models.get("mask_eyepatch").cubes().size() == 183, "Ayato's 128 cubes, Noro's 48, Eyepatch's 183");
    }

    /** The cubes in {@code slot} as the owner made them (the slim sleeves, made from theirs, left out). */
    private static int count(OutfitModels.Model model, int slot) {
        return (int)model.cubes().stream().filter(c -> c.slot() == slot && c.bone() < OutfitModels.RIGHT_ARM_SLIM).count();
    }

    static void geometry(Map<String, OutfitModels.Model> models) {
        for (OutfitModels.Model model : models.values()) {
            for (OutfitModels.Cube cube : model.cubes()) {
                check(cube.faces().length == 6, model.id() + ": six faces a cube");
                for (OutfitModels.Face face : cube.faces()) {
                    for (float v : face.corners()) {
                        check(Float.isFinite(v) && Math.abs(v) < 40.0F, model.id() + ": a corner near its bone: " + v);
                    }
                    for (float v : face.uvs()) {
                        check(v >= -1.0E-4F && v <= 1.0001F, model.id() + ": texture coordinates inside the texture: " + v);
                    }
                    float[] n = face.normal();
                    double length = Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
                    check(Math.abs(length - 1.0) < 1.0E-3, model.id() + ": a unit normal");
                }
            }
        }
        // The game's box UV, checked on a cube of its own: 2 x 3 x 4 at (10, 20): north face from u 14 to 16, v 24 to 27.
        float[] v = {0, 0, 0, 2, 3, 4, 0, 0, 0, 0, 0, 0, 10, 20};
        OutfitModels.Cube cube = cubeOf(v);
        float[] north = cube.faces()[4].uvs();
        check(near(north[0], 16 / 64.0F) && near(north[1], 24 / 64.0F) && near(north[2], 14 / 64.0F) && near(north[5], 27 / 64.0F),
            "the north face's net is where the game puts it");
        float[] east = cube.faces()[0].uvs();
        check(near(east[2], 16 / 64.0F) && near(east[0], 20 / 64.0F), "the east face's net follows the north's");
        // A quarter turn about y (a strap round the side) takes the cube's +x corner toward -z about its pivot.
        float[] turned = {0, 0, 0, 2, 2, 2, 0, 0, 0, 0, 90, 0, 0, 0};
        float[] corner = cubeOf(turned).faces()[0].corners(); // east face, its first corner was (2, 0, 2)
        check(near(corner[0], 2.0F) && near(corner[2], -2.0F), "a quarter turn about y as a model part turns: " + corner[0] + ", " + corner[2]);
        fit(models);
        // The right arm's cubes are near its shoulder (relative to its pivot), as the arm part carries them.
        for (OutfitModels.Cube arm : models.get("formal").on(OutfitModels.RIGHT_ARM, -1)) {
            for (float c : arm.faces()[4].corners()) {
                check(Math.abs(c) < 14.0F, "the overcoat's right sleeve sits on the right arm: " + c);
            }
        }
    }

    /**
     * The owner's "Armor is too bulky, if the player is alex model it should adapt to it ... half masks are covering most of the face
     * instead of being lower, way lower ... the eye position of the masks are very off": each sleeve again for slim arms, as many and
     * narrower; the half masks below the eyes; the coats close to the body; the masks clear of the skin's hat layer.
     */
    static void fit(Map<String, OutfitModels.Model> models) {
        for (String style : List.of("formal", "crimson")) {
            OutfitModels.Model model = models.get(style);
            for (int[] arm : new int[][] {{OutfitModels.RIGHT_ARM, OutfitModels.RIGHT_ARM_SLIM}, {OutfitModels.LEFT_ARM, OutfitModels.LEFT_ARM_SLIM}}) {
                List<OutfitModels.Cube> wide = model.on(arm[0], -1);
                List<OutfitModels.Cube> slim = model.on(arm[1], -1);
                check(!wide.isEmpty() && wide.size() == slim.size(), style + ": a slim sleeve for every wide one's cube: " + wide.size() + ", " + slim.size());
                check(span(slim, 0) < span(wide, 0) - 0.5F, style + ": the slim sleeve narrower: " + span(slim, 0) + " < " + span(wide, 0));
                check(Math.abs(span(slim, 1) - span(wide, 1)) < 0.1F, style + ": as long (a turned piece's height a hair less, narrower)");
            }
            // The coat's sleeve: 4.0.1 (the owner: "They seem a little too thick"): under a pixel off the arm, front and back (the owner's
            // stood nearly one out, its bands further).
            for (OutfitModels.Cube cube : model.on(OutfitModels.RIGHT_ARM, -1)) {
                for (OutfitModels.Face face : cube.faces()) {
                    for (int k = 0; k < 4; k++) {
                        float z = face.corners()[k * 3 + 2];
                        check(Math.abs(z) < 2.0F + 0.8F, style + ": the sleeve snug to the arm: " + z);
                    }
                }
            }
            // And the coat's front, with its lapels, belt and buttons: within a pixel of the chest.
            for (OutfitModels.Cube cube : model.on(OutfitModels.BODY, OutfitModels.CHESTPLATE)) {
                for (OutfitModels.Face face : cube.faces()) {
                    for (int k = 0; k < 4; k++) {
                        check(face.corners()[k * 3 + 2] > -2.0F - 1.0F, style + ": the coat close to the chest: " + face.corners()[k * 3 + 2]);
                    }
                }
            }
        }
        // 4.0.2 (the owner: "the armor does not allow for animation, make it compatible with animations"; "the jacket also breaks when
        // walking"): the coats' tails on bones of their own (hung from the hip, swinging less than the leg), the trousers on the leg; no
        // straight piece across an elbow (4 below the arm's pivot), a knee (6 below the leg's) or the torso's middle (6 down): each half
        // bends as the skin's does.
        for (String style : List.of("formal", "crimson")) {
            OutfitModels.Model model = models.get(style);
            check(!model.on(OutfitModels.RIGHT_LEG_COAT, OutfitModels.LEGGINGS).isEmpty() && !model.on(OutfitModels.LEFT_LEG_COAT, OutfitModels.LEGGINGS).isEmpty()
                && !model.on(OutfitModels.RIGHT_LEG, OutfitModels.LEGGINGS).isEmpty(), style + ": the coat's tails hung apart from the trousers");
            int[][] joints = {{OutfitModels.RIGHT_ARM, 4}, {OutfitModels.LEFT_ARM, 4}, {OutfitModels.RIGHT_ARM_SLIM, 4}, {OutfitModels.LEFT_ARM_SLIM, 4},
                {OutfitModels.RIGHT_LEG, 6}, {OutfitModels.LEFT_LEG, 6}, {OutfitModels.BODY, 6}};
            for (int[] joint : joints) {
                for (OutfitModels.Cube cube : model.on(joint[0], -1)) {
                    float lo = Float.MAX_VALUE;
                    float hi = -Float.MAX_VALUE;
                    boolean straight = true;
                    for (OutfitModels.Face face : cube.faces()) {
                        for (int k = 0; k < 4; k++) {
                            lo = Math.min(lo, face.corners()[k * 3 + 1]);
                            hi = Math.max(hi, face.corners()[k * 3 + 1]);
                        }
                        straight &= Math.abs(Math.abs(face.normal()[0]) + Math.abs(face.normal()[1]) + Math.abs(face.normal()[2]) - 1.0) < 1.0E-4;
                    }
                    check(!straight || !(lo < joint[1] - 0.06F && hi > joint[1] + 0.06F), style + ": a piece across a joint is cut there (" + joint[0] + ")");
                }
            }
        }
        try {
            String layer = new String(java.nio.file.Files.readAllBytes(Path.of("src/main/java/com/hex/kagune/control/client/render/OutfitLayer.java")));
            check(layer.contains("BodyAnimations.bend(player, part, partial)") && layer.contains("tails(pose, bones[bone])"),
                "the outfit bends with PlayerAnimator's bends; the tails hang from the hip");
        } catch (java.io.IOException failure) {
            check(false, "read the layer: " + failure);
        }
        // A half mask starts under the skin's eyes (the face's fifth row, y -4 to -3), a full one's front clears the hat layer.
        for (String half : List.of("mask_eyepatch", "mask_ayato", "mask_nishiki", "mask_shachi", "mask_tatara", "mask_yomo", "mask_rize", "mask_dotted")) {
            check(top(models.get(half)) >= -3.0F, half + ": over the mouth, below the eyes: " + top(models.get(half)));
        }
        for (String mask : Outfits.GENERIC_MASKS) {
            OutfitModels.Model model = models.get(mask);
            float front = 0;
            for (OutfitModels.Cube cube : model.cubes()) {
                front = Math.min(front, cube.faces()[4].corners()[2]);
            }
            check(front < -4.5F, mask + ": in front of the hat layer: " + front);
        }
    }

    /** How far {@code cubes} reach along {@code axis}. */
    private static float span(List<OutfitModels.Cube> cubes, int axis) {
        float lo = Float.MAX_VALUE;
        float hi = -Float.MAX_VALUE;
        for (OutfitModels.Cube cube : cubes) {
            for (OutfitModels.Face face : cube.faces()) {
                for (int k = 0; k < 4; k++) {
                    lo = Math.min(lo, face.corners()[k * 3 + axis]);
                    hi = Math.max(hi, face.corners()[k * 3 + axis]);
                }
            }
        }
        return hi - lo;
    }

    /** The highest point of {@code model}'s front (y down; its straps left out: they are at its top edge's height anyway). */
    private static float top(OutfitModels.Model model) {
        float top = Float.MAX_VALUE;
        for (OutfitModels.Cube cube : model.cubes()) {
            for (OutfitModels.Face face : cube.faces()) {
                for (int k = 0; k < 4; k++) {
                    if (face.corners()[k * 3 + 2] < -4.0F) {
                        top = Math.min(top, face.corners()[k * 3 + 1]);
                    }
                }
            }
        }
        return top;
    }

    private static OutfitModels.Cube cubeOf(float[] v) {
        try {
            java.lang.reflect.Method cube = OutfitModels.class.getDeclaredMethod("cube", int.class, int.class, float[].class, int.class, int.class);
            cube.setAccessible(true);
            return (OutfitModels.Cube)cube.invoke(null, OutfitModels.HEAD, OutfitModels.HELMET, v, 64, 64);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static boolean near(float a, float b) {
        return Math.abs(a - b) < 1.0E-4F;
    }

    static void pools() {
        for (String mask : Outfits.GENERIC_MASKS) {
            check(!Outfits.CHARACTER_MASKS.contains(mask), mask + ": a generic mask is no character's");
        }
        Set<String> seen = new HashSet<>();
        int masked = 0;
        for (int i = 0; i < 10000; i++) {
            int index = Outfits.wildMask(i / 10000.0);
            String mask = Outfits.generic(index);
            if (index == 0) {
                check(mask == null, "no mask is none");
                continue;
            }
            masked++;
            check(mask != null && Outfits.GENERIC_MASKS.contains(mask), "a wild ghoul's mask is generic: " + mask);
            check(!Outfits.AOGIRI_MASKS.contains(mask) && !Outfits.CHARACTER_MASKS.contains(mask), "never a faction's or a character's: " + mask);
            seen.add(mask);
        }
        check(seen.size() == Outfits.GENERIC_MASKS.size(), "every generic mask turns up: " + seen);
        check(Math.abs(masked / 10000.0 - Outfits.WILD_MASK_CHANCE) < 0.01, "about the chance of a mask: " + masked);
        for (String mask : Outfits.AOGIRI_MASKS) {
            check(Outfits.CHARACTER_MASKS.contains(mask), mask + ": Aogiri's own (from the owner's sheet)");
            check(!mask.equals("mask_ayato") && !mask.equals("mask_noro") && !mask.equals("mask_eto"), mask + ": no executive's");
        }
        check(Outfits.DROP_CHANCE > 0.0F && Outfits.DROP_CHANCE <= 0.05F, "each piece's drop is a small chance");
        double full = Math.pow(Outfits.DROP_CHANCE, 4);
        check(full < 1.0E-4, "a whole outfit at once almost never: " + full);
    }
}
