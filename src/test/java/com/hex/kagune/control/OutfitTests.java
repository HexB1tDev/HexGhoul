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
        check(Outfits.ALL.size() == 8 + 24 + 6, "eight outfit pieces, 24 owner's masks, six generic: " + Outfits.ALL.size());
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
        // The owner's counts (their build-verification.txt): the outfits' slots and three masks.
        check(count(models.get("formal"), OutfitModels.CHESTPLATE) == 73 && count(models.get("formal"), OutfitModels.HELMET) == 4,
            "the Formal Outfit as the owner's: 73 coat cubes, 4 cap cubes");
        check(count(models.get("crimson"), OutfitModels.HELMET) == 19 && count(models.get("crimson"), OutfitModels.CHESTPLATE) == 45,
            "the Crimson Outfit as the owner's: 19 hood cubes, 45 coat cubes");
        check(models.get("mask_ayato").cubes().size() == 128 && models.get("mask_noro").cubes().size() == 48
            && models.get("mask_eyepatch").cubes().size() == 183, "Ayato's 128 cubes, Noro's 48, Eyepatch's 183");
    }

    private static int count(OutfitModels.Model model, int slot) {
        return (int)model.cubes().stream().filter(c -> c.slot() == slot).count();
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
        // The right arm's cubes are near its shoulder (relative to its pivot), as the arm part carries them.
        for (OutfitModels.Cube arm : models.get("formal").on(OutfitModels.RIGHT_ARM, -1)) {
            for (float c : arm.faces()[4].corners()) {
                check(Math.abs(c) < 14.0F, "the overcoat's right sleeve sits on the right arm: " + c);
            }
        }
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
