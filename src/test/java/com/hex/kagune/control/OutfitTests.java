package com.hex.kagune.control;

import com.hex.kagune.control.outfit.CoatDrape;
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
        draped(models);
        pools();
        if (!FAILURES.isEmpty()) {
            FAILURES.stream().limit(40).forEach(failure -> System.err.println("FAILED: " + failure));
            System.err.println(FAILURES.size() + " of " + (passed + FAILURES.size()) + " checks failed");
            System.exit(1);
        }
        System.out.println("Outfit tests: " + passed + " checks passed");
    }

    static void catalogue(Map<String, OutfitModels.Model> models) {
        check(Outfits.ALL.size() == 7 + 24 + 9, "seven outfit pieces, 24 owner's masks, nine generic: " + Outfits.ALL.size());
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
        // 4.0.7 (the owner: "Full masks (only full masks not half masks) prevent CCG agents and humans to tell you're a ghoul when your eyes
        // are in Kakugan."): the owner's sixteen full masks and eight of the generic ones hide the eyes; their eight half masks and the cloth
        // wrap do not; nothing that is not a mask does.
        int full = 0;
        for (String mask : Outfits.CHARACTER_MASKS) {
            full += Outfits.fullMask(mask) ? 1 : 0;
        }
        int fullGeneric = 0;
        for (String mask : Outfits.GENERIC_MASKS) {
            fullGeneric += Outfits.fullMask(mask) ? 1 : 0;
        }
        check(full == 16 && fullGeneric == 8 && !Outfits.fullMask("mask_eyepatch") && !Outfits.fullMask("mask_cloth") && Outfits.fullMask("mask_noro")
            && Outfits.fullMask("mask_touka") && !Outfits.fullMask("formal_helmet") && !Outfits.fullMask("nothing"), "full masks hide the eyes, half masks don't: " + full + ", " + fullGeneric);
        try {
            String sightings = new String(Files.readAllBytes(Path.of("src/main/java/com/hex/kagune/control/server/Sightings.java")));
            String agents = new String(Files.readAllBytes(Path.of("src/main/java/com/hex/kagune/control/server/AgentBrain.java")));
            String humans = new String(Files.readAllBytes(Path.of("src/main/java/com/hex/kagune/control/server/HumanBrain.java")));
            check(sightings.contains("PaleServer.eyes(player) && !masked(player)") && agents.contains("GhoulServer.ghoul(player) && !Sightings.masked(player)")
                && humans.contains("GhoulServer.ghoul(player) && !Sightings.masked(player)"), "investigators and townsfolk don't see kakugan behind a full mask");
        } catch (java.io.IOException failure) {
            check(false, "read the sightings: " + failure);
        }
        // 4.0.3 (the owner: "On the Ougiri outfit, no more hood ... give it a high collar on the sides of the head pointing outwards"): no
        // Crimson Hood; the coat's collar on the body, beside the head (clear of the skin's hat layer), flaring out as it rises.
        check(Outfits.of("crimson_helmet") == null && Outfits.CRIMSON_SET.size() == 3 && Outfits.FORMAL_SET.size() == 4, "the Crimson Outfit has no hood");
        check(models.get("crimson").on(OutfitModels.HEAD, -1).isEmpty() && count(models.get("crimson"), OutfitModels.HELMET) == 0,
            "nothing of the hood is drawn");
        // The owner's counts (their build-verification.txt): the outfits' slots and three masks. 4.0.1: every one of theirs, and the
        // details added (the owner: "add more details to it as well to make it as polished").
        check(count(models.get("formal"), OutfitModels.CHESTPLATE) > 73 && count(models.get("formal"), OutfitModels.HELMET) > 4,
            "the Formal Outfit: the owner's 73 coat cubes and 4 cap cubes, and its details");
        check(count(models.get("crimson"), OutfitModels.CHESTPLATE) > 45, "the Crimson Outfit: the owner's 45 coat cubes, and its details");
        // 4.0.4 (the owner: "New masks, replace ALL the masks with these models."): their new pack's masks, every cube of theirs (its
        // manifest's counts: Ayato's 75, Noro's 372, Eyepatch's 97, all 24 together).
        check(models.get("mask_ayato").cubes().size() == 75 && models.get("mask_noro").cubes().size() == 372
            && models.get("mask_eyepatch").cubes().size() == 97, "Ayato's 75 cubes, Noro's 372, Eyepatch's 97");
        try {
            String manifest = new String(Files.readAllBytes(Path.of("art/outfits/masks/models_manifest.json")));
            java.util.regex.Matcher counts = java.util.regex.Pattern.compile("\"cubes\": (\\d+)").matcher(manifest);
            int theirs = 0;
            int entries = 0;
            while (counts.find()) {
                theirs += Integer.parseInt(counts.group(1));
                entries++;
            }
            int ours = 0;
            for (String mask : Outfits.CHARACTER_MASKS) {
                ours += models.get(mask).cubes().size();
            }
            check(entries == 24 && ours == theirs, "all 24 of the owner's masks, every cube: " + ours + " of " + theirs);
        } catch (java.io.IOException failure) {
            check(false, "read the owner's mask manifest: " + failure);
        }
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
        // 4.0.2 (the owner: "our own category in the creative menu with like the armors and quinques and whatever, everything separated and
        // well organized"): the mod's own tabs, named, each kind of thing in its own.
        try {
            String items = new String(java.nio.file.Files.readAllBytes(Path.of("src/main/java/com/hex/kagune/control/ControlItems.java")));
            String lang = new String(java.nio.file.Files.readAllBytes(Path.of("src/main/resources/assets/hexkagune/lang/en_us.json")));
            for (String tab : List.of("kagune", "quinques", "outfits", "masks")) {
                check(items.contains("TABS.register(\"" + tab + "\"") && lang.contains("\"itemGroup.hexkagune." + tab + "\""), "the " + tab + " tab, named");
            }
            check(!items.contains("CreativeModeTabs."), "nothing of ours left in Minecraft's own tabs");
            // 4.1.9 (the owner: "Opening the inventory crashes the game instantly."): the tabs' lambdas must implement the game's own
            // DisplayItemsGenerator.accept, which is m_257865_ at run time (joined.tsrg cef$b); compiled as "accept", every tab crashed the
            // creative inventory with an AbstractMethodError. Read in the compiled class, not in the source.
            try (java.io.InputStream in = OutfitTests.class.getClassLoader().getResourceAsStream("com/hex/kagune/control/ControlItems.class")) {
                byte[] bytes = in == null ? new byte[0] : in.readAllBytes();
                String constants = new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1);
                check(constants.contains("m_257865_"), "the creative tabs' item lists implement DisplayItemsGenerator by its run-time name m_257865_");
            }
        } catch (java.io.IOException failure) {
            check(false, "read the items: " + failure);
        }
        try {
            String layer = new String(java.nio.file.Files.readAllBytes(Path.of("src/main/java/com/hex/kagune/control/client/render/OutfitLayer.java")));
            check(layer.contains("BodyAnimations.bend(player, part, partial)") && layer.contains("tails(pose, bones[bone])"),
                "the outfit bends with PlayerAnimator's bends; the tails hang from the hip");
        } catch (java.io.IOException failure) {
            check(false, "read the layer: " + failure);
        }
        // 4.0.4: the owner's masks as their pack makes them: a half mask leaves the eyes clear (their pack's zones: either side, 1 to 3.2 out
        // from the middle, 1 to 3.8 above the chin; only a nose bridge between), and no mask comes into the head or its hat layer.
        for (String half : List.of("mask_eyepatch", "mask_ayato", "mask_nishiki", "mask_shachi", "mask_tatara", "mask_yomo", "mask_rize", "mask_dotted")) {
            int over = 0;
            for (OutfitModels.Cube cube : models.get(half).cubes()) {
                float[] b = bounds(cube);
                for (int side = -1; side <= 1; side += 2) {
                    float x0 = Math.min(side * 1.0F, side * 3.2F);
                    float x1 = Math.max(side * 1.0F, side * 3.2F);
                    over += b[0] < x1 - 1.0E-4F && b[3] > x0 + 1.0E-4F && b[1] < -1.0F - 1.0E-4F && b[4] > -3.8F + 1.0E-4F && b[2] < -4.0F ? 1 : 0;
                }
            }
            check(over == 0, half + ": over the mouth, the eyes left clear (" + over + " in the way)");
        }
        for (String mask : Outfits.CHARACTER_MASKS) {
            int inside = 0;
            for (OutfitModels.Cube cube : models.get(mask).cubes()) {
                float[] b = bounds(cube);
                inside += b[0] < 4.5F - 1.0E-4F && b[3] > -4.5F + 1.0E-4F && b[1] < 0.5F - 1.0E-4F && b[4] > -8.5F + 1.0E-4F
                    && b[2] < 4.5F - 1.0E-4F && b[5] > -4.5F + 1.0E-4F ? 1 : 0;
            }
            check(inside == 0, mask + ": clear of the head and its hat layer (" + inside + " cubes in it)");
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

    /**
     * 4.0.3 (the owner, of the renders: "Do I need to say anything about both outfit's legs and arms?"): a piece cut at a joint carries its
     * own part of its texture in each half (not all of it squeezed into both); every sleeve closes over the arm's inner side (the skin
     * showed there as the arms swung); and a coat's tails never have a leg through them, walking, running, crouching, sitting, kicking or
     * tucked in a flip, nor part down the middle.
     */
    static void draped(Map<String, OutfitModels.Model> models) {
        for (String style : List.of("formal", "crimson")) {
            OutfitModels.Model model = models.get(style);
            int halves = 0;
            for (int bone = 0; bone < OutfitModels.PIVOTS.length; bone++) {
                List<OutfitModels.Cube> cubes = model.on(bone, -1);
                for (OutfitModels.Cube upper : cubes) {
                    for (OutfitModels.Cube lower : cubes) {
                        float[] a = bounds(upper);
                        float[] b = bounds(lower);
                        float[] fa = upper.faces()[4].uvs();
                        float[] fb = lower.faces()[4].uvs();
                        boolean pair = upper != lower && upper.slot() == lower.slot() && straight(upper) && straight(lower) && Math.abs(a[4] - b[1]) < 1.0E-3F
                            && Math.abs(a[0] - b[0]) < 1.0E-3F && Math.abs(a[2] - b[2]) < 1.0E-3F && Math.abs(a[3] - b[3]) < 1.0E-3F
                            && Math.abs(a[5] - b[5]) < 1.0E-3F && Math.abs(fa[0] - fb[0]) < 1.0E-5F && Math.abs(fa[2] - fb[2]) < 1.0E-5F;
                        if (pair) {
                            halves++;
                            check(Math.abs(fa[5] - fb[1]) < 1.0E-4F && fa[1] < fa[5] && fb[1] < fb[5],
                                style + ": a cut piece's halves each carry their own part of its texture, one after the other (" + bone + ")");
                        }
                    }
                }
            }
            check(halves >= 12, style + ": its sleeves, trousers, coat and tails cut at their joints: " + halves);
            // Each sleeve (wide and slim) closes over the arm's inner side, from the shoulder to the cuff, front to back.
            int[][] arms = {{OutfitModels.RIGHT_ARM, 1}, {OutfitModels.LEFT_ARM, -1}, {OutfitModels.RIGHT_ARM_SLIM, 1}, {OutfitModels.LEFT_ARM_SLIM, -1}};
            for (int[] arm : arms) {
                for (float y : new float[] {-1.5F, 0.0F, 2.0F, 3.9F, 4.1F, 6.0F, 8.0F, 9.0F}) {
                    for (float z : new float[] {-1.9F, 0.0F, 1.9F}) {
                        boolean covered = false;
                        for (OutfitModels.Cube cube : model.on(arm[0], OutfitModels.CHESTPLATE)) {
                            float[] c = bounds(cube);
                            float x = arm[1] * 1.01F;
                            covered |= straight(cube) && c[0] <= x && x <= c[3] && c[1] <= y && y <= c[4] && c[2] <= z && z <= c[5];
                        }
                        check(covered, style + ": the sleeve closes over the arm's inner side (" + arm[0] + " at " + y + ", " + z + ")");
                    }
                }
            }
        }
        // 4.1.12 (the owner, of the Crimson Outfit in a jump and a kick: "I truly don't know how in this world you expected the clothes to open
        // like that and look good"): each half of the tails hangs from its own leg (OutfitLayer.drapes). No point of that leg (its trousers cut
        // at the knee, bent as bendy-lib bends them, its boot) comes through its half, in any of these (right thigh, left thigh, right knee,
        // left knee); a leg that hangs straight leaves its half hanging straight, however the other moves (no more skirt thrown out flat).
        float[][] poses = {{0, 0, 0, 0}, {-0.3F, 0.3F, 0, 0}, {-0.6F, 0.6F, 0, 0}, {-1.0F, 1.0F, 0, 0}, {-1.4F, 1.4F, 0, 0}, {0.6F, -0.6F, 0, 0},
            {-0.55F, 0.55F, 0.25F, 0.6F}, {-0.8F, 0.4F, 0.1F, 1.0F}, {0.5F, -0.9F, 1.3F, 0.2F}, {-1.15F, -1.15F, 1.9F, 1.9F}, {-1.4F, -1.4F, 2.3F, 2.3F},
            {-1.0F, -0.6F, 1.6F, 1.2F}, {-1.41F, -1.41F, 0, 0}, {-1.4F, -1.4F, 1.4F, 1.4F}, {-1.2F, 0.3F, 0, 0.8F}, {-1.5F, -1.5F, 2.4F, 2.4F},
            {0.8F, 0.8F, 1.2F, 1.2F}, {0.2F, 0.2F, 1.8F, 1.8F}, {0.55F, -0.2F, 1.2F, 0.3F}, {-1.5F, 0.0F, 0.2F, 0.0F}, {0.0F, -1.5F, 0.0F, 0.3F}};
        float hem = 9.42F;
        for (float[] pose : poses) {
            String at = java.util.Arrays.toString(pose);
            for (int leg = 0; leg < 2; leg++) {
                float thigh = pose[leg];
                float knee = pose[leg + 2];
                CoatDrape drape = CoatDrape.over(Math.min(0.0F, thigh), Math.max(0.0F, thigh), new float[] {thigh, thigh}, new float[] {knee, knee});
                float[][] front = {hung(drape, -0.3F, 0.0F, -2.46F), hung(drape, -0.3F, CoatDrape.KNEE, -2.46F), hung(drape, -0.3F, hem, -2.46F)};
                float[][] back = {hung(drape, -0.3F, 0.0F, 2.43F), hung(drape, -0.3F, CoatDrape.KNEE, 2.43F), hung(drape, -0.3F, hem, 2.43F)};
                int through = 0;
                for (int side = -1; side <= 1; side += 2) {
                    for (float[][] line : CoatDrape.outline(thigh, knee, side, 0.0F)) {
                        for (int k = 0; k <= 20; k++) {
                            float t = k / 20.0F;
                            float[] q = {line[0][0] + (line[1][0] - line[0][0]) * t, line[0][1] + (line[1][1] - line[0][1]) * t};
                            through += through(front, back, q) ? 1 : 0;
                        }
                    }
                }
                check(through == 0, "no leg through its own half of the coat: " + at + " leg " + leg + " (" + through + " points)");
                if (thigh == 0.0F && knee == 0.0F) {
                    // Standing's own drape (just clear of its boot's toe below the knee), whatever the other leg does.
                    CoatDrape standing = CoatDrape.over(0.0F, 0.0F, new float[] {0.0F, 0.0F}, new float[] {0.0F, 0.0F});
                    check(drape.equals(standing) && Math.abs(drape.front()) < 0.2F && Math.abs(drape.back()) < 0.2F,
                        "a leg hanging straight leaves its half hanging as it does standing, whatever the other does: " + at + " " + drape);
                }
            }
        }
        try {
            String layer = new String(java.nio.file.Files.readAllBytes(Path.of("src/main/java/com/hex/kagune/control/client/render/OutfitLayer.java")));
            check(layer.contains("CoatDrape.over(hung[leg * 2], hung[leg * 2 + 1], new float[] {thigh[leg], thigh[leg]}, new float[] {knee[leg], knee[leg]})")
                && layer.contains("last[leg * 2] + TAIL_FALL * since") && layer.contains("drape[bone == OutfitModels.RIGHT_LEG ? 0 : 1]")
                && layer.contains("drape.place(p)"), "each half of the tails hangs from its own leg, falling back slowly as cloth does");
        } catch (java.io.IOException failure) {
            check(false, "read the layer: " + failure);
        }
    }

    /** ({@code y}, {@code z}) where {@code drape} hangs a tail's corner ({@code x}, {@code y}, {@code z}). */
    private static float[] hung(CoatDrape drape, float x, float y, float z) {
        float[] v = {x, y, z};
        drape.place(v);
        return new float[] {v[1], v[2]};
    }

    /**
     * Whether {@code q} comes through the coat: outside its outline (the fronts' line, the hem, the backs' line, the top at the hip) and
     * nearest to its front or its back, part way along (not below its hem, nor above the hip), by more than a tenth of a texel.
     */
    private static boolean through(float[][] front, float[][] back, float[] q) {
        float[][] outline = {front[0], front[1], front[2], back[2], back[1], back[0]};
        boolean inside = false;
        for (int i = 0; i < outline.length; i++) {
            float[] a = outline[i];
            float[] b = outline[(i + 1) % outline.length];
            if ((a[1] > q[1]) != (b[1] > q[1]) && q[0] < a[0] + (q[1] - a[1]) * (b[0] - a[0]) / (b[1] - a[1])) {
                inside = !inside;
            }
        }
        if (inside) {
            return false;
        }
        float nearest = Float.MAX_VALUE;
        boolean side = false;
        for (int i = 0; i < outline.length; i++) {
            float[] a = outline[i];
            float[] b = outline[(i + 1) % outline.length];
            float dy = b[0] - a[0];
            float dz = b[1] - a[1];
            float length = dy * dy + dz * dz;
            float t = length < 1.0E-12F ? 0.0F : Math.max(0.0F, Math.min(1.0F, ((q[0] - a[0]) * dy + (q[1] - a[1]) * dz) / length));
            float d = (float)Math.hypot(q[0] - (a[0] + dy * t), q[1] - (a[1] + dz * t));
            if (d < nearest) {
                nearest = d;
                // The fronts' two lines (0, 1) and the backs' two (3, 4), part way along; the hem (2) and the top (5) are not through.
                side = (i == 0 || i == 1 || i == 3 || i == 4) && t > 0.0F && t < 1.0F;
            }
        }
        return side && nearest > 0.08F;
    }

    /** {@code cube}'s least and greatest corner (x, y, z, then x, y, z). */
    private static float[] bounds(OutfitModels.Cube cube) {
        float[] out = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        for (OutfitModels.Face face : cube.faces()) {
            for (int k = 0; k < 4; k++) {
                for (int axis = 0; axis < 3; axis++) {
                    out[axis] = Math.min(out[axis], face.corners()[k * 3 + axis]);
                    out[axis + 3] = Math.max(out[axis + 3], face.corners()[k * 3 + axis]);
                }
            }
        }
        return out;
    }

    /** Whether {@code cube} is unturned (its faces square to the axes). */
    private static boolean straight(OutfitModels.Cube cube) {
        for (OutfitModels.Face face : cube.faces()) {
            float[] n = face.normal();
            if (Math.abs(Math.abs(n[0]) + Math.abs(n[1]) + Math.abs(n[2]) - 1.0F) > 1.0E-4F) {
                return false;
            }
        }
        return true;
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
