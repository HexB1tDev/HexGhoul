package com.hex.kagune.control;

import com.hex.kagune.control.outfit.OutfitFit;
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
        fitted489(models);
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
            check(layer.contains("BodyAnimations.bend(player, part, partial)"), "the outfit bends with PlayerAnimator's bends");
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
        // 4.1.14 (the owner: "Change the armors so they dont open like skirts, simply attach to the leg properly."): each half of a coat's
        // tails is drawn on its own leg as the trousers are (the leg's own turn and its knee's bend), so it goes wherever the leg goes and never
        // opens away from it. Around each leg from the hip to below the knee: in front of it, behind it and at its outer side.
        for (String style : List.of("formal", "crimson")) {
            OutfitModels.Model model = models.get(style);
            for (int bone : new int[] {OutfitModels.RIGHT_LEG_COAT, OutfitModels.LEFT_LEG_COAT}) {
                float[] lo = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
                float[] hi = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
                for (OutfitModels.Cube cube : model.on(bone, -1)) {
                    for (OutfitModels.Face face : cube.faces()) {
                        for (int k = 0; k < 4; k++) {
                            for (int d = 0; d < 3; d++) {
                                lo[d] = Math.min(lo[d], face.corners()[k * 3 + d]);
                                hi[d] = Math.max(hi[d], face.corners()[k * 3 + d]);
                            }
                        }
                    }
                }
                float outer = bone == OutfitModels.RIGHT_LEG_COAT ? -lo[0] : hi[0];
                check(lo[1] <= 0.0F && hi[1] >= 9.0F && lo[2] <= -2.3F && hi[2] >= 2.1F && outer >= 3.0F,
                    style + ": its tails round the leg (front, back, outer side), hip to below the knee (" + java.util.Arrays.toString(lo) + " "
                        + java.util.Arrays.toString(hi) + ")");
            }
        }
        try {
            String layer = new String(java.nio.file.Files.readAllBytes(Path.of("src/main/java/com/hex/kagune/control/client/render/OutfitLayer.java")));
            check(layer.contains("List<OutfitModels.Cube> tails = model.on(bone == OutfitModels.RIGHT_LEG ? OutfitModels.RIGHT_LEG_COAT : OutfitModels.LEFT_LEG_COAT, slot);")
                && layer.contains("bones[bone].m_104299_(pose);\n                    emit(out, pose, tails, light, bend(player, bone, partial));")
                && !layer.contains("CoatDrape") && !layer.contains("drape"), "each half of the tails is drawn on its own leg, as the trousers are");
            check(!java.nio.file.Files.exists(Path.of("src/main/java/com/hex/kagune/control/outfit/CoatDrape.java")), "no drape hangs them apart from the legs");
        } catch (java.io.IOException failure) {
            check(false, "read the layer: " + failure);
        }
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

    /**
     * 4.1.89 (the owner: "when women are wearing our armor, our CCG armor or the ghoul armor, their breasts literally stick out from the
     * armor, which is very awkward and very awfully putting. So you got to fix that so the armor adapts to their chest."): on a woman, a
     * coat's body takes her bust's shape (OutfitFit), and her skin's bust is not drawn under anything worn on her chest.
     */
    static void fitted489(Map<String, OutfitModels.Model> models) {
        float span = FemaleBody.BUST_FRONT - FemaleBody.BUST_OUT;
        check(OutfitFit.depth(FemaleBody.BUST_TOP, 1.0F, 0.0F) == 0.0F && OutfitFit.depth(FemaleBody.BUST_BOTTOM, 1.0F, 0.0F) == 0.0F
            && Math.abs(OutfitFit.depth(FemaleBody.BUST_TIP, 1.0F, 0.0F) - span) < 1.0E-5F
            && Math.abs(OutfitFit.depth(FemaleBody.BUST_TIP, 0.85F, 0.0F) - 0.85F * span) < 1.0E-5F
            && Math.abs(OutfitFit.depth(FemaleBody.BUST_TIP - 1.0F, 1.0F, 1.0F) - span) < 1.0E-5F
            && OutfitFit.depth(0.5F, 1.0F, 0.0F) == 0.0F && OutfitFit.depth(9.0F, 1.0F, 0.0F) == 0.0F && OutfitFit.depth(FemaleBody.BUST_TIP, 0.0F, 0.0F) == 0.0F,
            "the push is her bust's own wedge: none above its top or below its bottom, its full depth at its tip (bounced with it), by her curves");
        check(OutfitFit.weight(-2.5F) == 1.0F && OutfitFit.weight(OutfitFit.SURFACE) == 1.0F && OutfitFit.weight(OutfitFit.TORSO) == 0.0F
            && OutfitFit.weight(1.0F) == 0.0F && Math.abs(OutfitFit.weight((OutfitFit.SURFACE + OutfitFit.TORSO) / 2.0F) - 0.5F) < 1.0E-5F,
            "the coat's face and all on it move the whole way, nothing on the torso or behind it, between them by how far in front");
        for (String id : new String[] {"formal", "crimson"}) {
            OutfitModels.Model model = models.get(id);
            List<OutfitModels.Cube> body = model.on(OutfitModels.BODY, OutfitModels.CHESTPLATE);
            List<OutfitModels.Cube> cut = OutfitFit.cutAll(body, 0.0F);
            check(faces(cut) > faces(body) + 100 && Math.abs(area(cut) - area(body)) < 1.0E-3 * area(body),
                id + ": cut into strips across the bust, nothing lost or added: " + faces(body) + " faces, " + faces(cut) + " pieces");
            boolean textured = true;
            for (int c = 0; c < body.size(); c++) {
                float[] box = uvBox(body.get(c).faces());
                for (OutfitModels.Face piece : cut.get(c).faces()) {
                    for (int k = 0; k < 4; k++) {
                        float u = piece.uvs()[k * 2];
                        float v = piece.uvs()[k * 2 + 1];
                        textured &= u >= box[0] - 1.0E-5F && u <= box[2] + 1.0E-5F && v >= box[1] - 1.0E-5F && v <= box[3] + 1.0E-5F;
                    }
                }
            }
            check(textured, id + ": each piece keeps its face's own texture");
            float most = FemaleBody.jigglePixels(FemaleBody.JIGGLE_MOST);
            for (float curves : new float[] {1.0F, com.hex.kagune.control.npc.AgentRules.FEMALE_CURVES}) {
                for (float bounce : new float[] {0.0F, most, -most, 0.37F}) {
                    List<OutfitModels.Cube> cutThere = OutfitFit.cutAll(body, bounce);
                    List<OutfitModels.Cube> on = OutfitFit.fit(cutThere, curves, bounce);
                    // Over the whole of her bust (but the Crimson coat's own open seam down its middle), the coat stands in front of it.
                    double worst = Double.MAX_VALUE;
                    String where = "";
                    for (double x = -3.9; x <= 3.9; x += 0.1) {
                        if (Math.abs(x) < 0.15) {
                            continue;
                        }
                        for (double y = FemaleBody.BUST_TOP + 0.1; y < FemaleBody.BUST_BOTTOM - 0.05; y += 0.1) {
                            double gap = (FemaleBody.BUST_FRONT - OutfitFit.depth((float)y, curves, bounce)) - frontAt(on, x, y);
                            if (gap < worst) {
                                worst = gap;
                                where = String.format(java.util.Locale.ROOT, " at (%.1f, %.1f)", x, y);
                            }
                        }
                    }
                    check(worst > 0.3, id + String.format(java.util.Locale.ROOT, ", curves %.2f, bounce %.1f", curves, bounce)
                        + ": her bust under the coat everywhere, never through it: " + worst + " px" + where);
                    // 4.1.89: and the coat's front takes exactly her bust's shape: pushed out by its depth there, nowhere else (above its
                    // top, below its bottom, a corner between cut lines would bulge it where she has none).
                    double off = 0.0;
                    String offAt = "";
                    for (double x = -3.9; x <= 3.9; x += 0.1) {
                        if (Math.abs(x) < 0.15) {
                            continue;
                        }
                        for (double y = 0.05; y < 8.0; y += 0.1) {
                            double made = frontAt(body, x, y);
                            if (made > 50.0) {
                                continue;
                            }
                            double miss = Math.abs(frontAt(on, x, y) - (made - OutfitFit.depth((float)y, curves, bounce)));
                            if (miss > off) {
                                off = miss;
                                offAt = String.format(java.util.Locale.ROOT, " at (%.1f, %.1f)", x, y);
                            }
                        }
                    }
                    check(off < 0.02, id + String.format(java.util.Locale.ROOT, ", curves %.2f, bounce %.1f", curves, bounce)
                        + ": the coat's front out by her bust's depth there and nowhere else: off by " + off + " px" + offAt);
                    // Cut every STEP across her bust: no piece in front of the torso spans more of its band than a strip.
                    double tallest = 0.0;
                    for (OutfitModels.Cube cube : cutThere) {
                        for (OutfitModels.Face piece : cube.faces()) {
                            float low = Float.MAX_VALUE;
                            float high = -Float.MAX_VALUE;
                            boolean front = false;
                            for (int k = 0; k < 4; k++) {
                                low = Math.min(low, piece.corners()[k * 3 + 1]);
                                high = Math.max(high, piece.corners()[k * 3 + 1]);
                                front |= piece.corners()[k * 3 + 2] < OutfitFit.TORSO - 1.0E-3F;
                            }
                            if (front) {
                                tallest = Math.max(tallest, Math.min(high, FemaleBody.BUST_BOTTOM) - Math.max(low, FemaleBody.BUST_TOP));
                            }
                        }
                    }
                    check(tallest <= OutfitFit.STEP + 1.0E-3 && OutfitFit.STEP == 0.5F,
                        id + ": cut in half-pixel strips across her bust, so it bends where she does: tallest " + tallest);
                    // Each corner: only pushed out, by the bust under it as far as it is in front of the torso; its place across and its texture kept.
                    boolean kept = true;
                    boolean normals = true;
                    for (int c = 0; c < cutThere.size(); c++) {
                        OutfitModels.Face[] was = cutThere.get(c).faces();
                        OutfitModels.Face[] now = on.get(c).faces();
                        kept &= was.length == now.length;
                        for (int f = 0; f < Math.min(was.length, now.length); f++) {
                            for (int k = 0; k < 4; k++) {
                                float y = was[f].corners()[k * 3 + 1];
                                float z = was[f].corners()[k * 3 + 2];
                                float want = z - OutfitFit.depth(y, curves, bounce) * OutfitFit.weight(z);
                                kept &= now[f].corners()[k * 3] == was[f].corners()[k * 3] && now[f].corners()[k * 3 + 1] == y
                                    && Math.abs(now[f].corners()[k * 3 + 2] - want) < 1.0E-5F && now[f].corners()[k * 3 + 2] <= z
                                    && (z < OutfitFit.TORSO || now[f].corners()[k * 3 + 2] == z)
                                    && now[f].uvs()[k * 2] == was[f].uvs()[k * 2] && now[f].uvs()[k * 2 + 1] == was[f].uvs()[k * 2 + 1];
                            }
                            float[] n = now[f].normal();
                            float[] o = was[f].normal();
                            normals &= Math.abs(n[0] * n[0] + n[1] * n[1] + n[2] * n[2] - 1.0F) < 1.0E-3F && n[0] * o[0] + n[1] * o[1] + n[2] * o[2] > 0.0F;
                            // and square to the face as it now lies (not the flat face's it was cut from)
                            double[] g = across(now[f].corners());
                            double gl = Math.sqrt(g[0] * g[0] + g[1] * g[1] + g[2] * g[2]);
                            if (gl > 1.0E-4) {
                                normals &= Math.abs(n[0] * g[0] + n[1] * g[1] + n[2] * g[2]) / gl > 0.999;
                            }
                        }
                    }
                    check(kept, id + ": every corner only pushed out by the bust under it (its back and the torso never), across and textured as it was");
                    check(normals, id + ": each piece lit as it now faces (its normal remade, still outward)");
                }
            }
            check(OutfitFit.fit(cut, 0.0F, 0.0F).equals(cut) || same(OutfitFit.fit(cut, 0.0F, 0.0F), cut), id + ": on no bust, the coat as made");
        }
        // A face tilted from the coat's face back toward her torso (no cube of these two coats has one, a future model might): cut where the
        // push starts to ease (SURFACE) and ends (TORSO), so each of its points goes where fitZ says (the cut lines off the strips' on purpose).
        OutfitModels.Face tilted = new OutfitModels.Face(new float[] {-1.0F, 3.1F, -2.7F, 1.0F, 3.1F, -2.7F, 1.0F, 4.1F, -1.5F, -1.0F, 4.1F, -1.5F},
            new float[] {0.0F, 0.0F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F}, new float[] {0.0F, 0.768F, -0.640F});
        List<OutfitModels.Cube> slope = List.of(new OutfitModels.Cube(OutfitModels.BODY, OutfitModels.CHESTPLATE, new OutfitModels.Face[] {tilted}));
        List<OutfitModels.Cube> slopeOn = OutfitFit.fit(OutfitFit.cutAll(slope, 0.0F), 1.0F, 0.0F);
        double slopeOff = 0.0;
        for (double y = 3.15; y < 4.05; y += 0.01) {
            double z = -2.7 + 1.2 * (y - 3.1);
            slopeOff = Math.max(slopeOff, Math.abs(frontAt(slopeOn, 0.0, y) - OutfitFit.fitZ((float)y, (float)z, 1.0F, 0.0F)));
        }
        check(slopeOff < 0.05, "a face tilted toward her torso goes where fitZ says all along it (cut where the push eases and ends): off by " + slopeOff);
        // Its corners listed the other way round (a model's face may wind either way): its pieces still lit from the side it faced.
        float[] rc = tilted.corners();
        float[] ru = tilted.uvs();
        OutfitModels.Face reversed = new OutfitModels.Face(new float[] {rc[9], rc[10], rc[11], rc[6], rc[7], rc[8], rc[3], rc[4], rc[5], rc[0], rc[1], rc[2]},
            new float[] {ru[6], ru[7], ru[4], ru[5], ru[2], ru[3], ru[0], ru[1]}, tilted.normal());
        boolean outward = true;
        for (OutfitModels.Cube cube : OutfitFit.fit(OutfitFit.cutAll(List.of(new OutfitModels.Cube(OutfitModels.BODY, OutfitModels.CHESTPLATE,
            new OutfitModels.Face[] {reversed})), 0.0F), 1.0F, 0.0F)) {
            for (OutfitModels.Face piece : cube.faces()) {
                float[] n = piece.normal();
                outward &= n[0] * tilted.normal()[0] + n[1] * tilted.normal()[1] + n[2] * tilted.normal()[2] > 0.5F;
            }
        }
        check(outward, "a face wound the other way round: its fitted pieces still face the way it did");
        // Kept by her figure and its bounce (a fiftieth of a pixel): a woman standing still has her coat made once.
        List<OutfitModels.Cube> body = models.get("formal").on(OutfitModels.BODY, OutfitModels.CHESTPLATE);
        check(OutfitFit.fitted("test/formal", body, 1.0F, 0.0F) == OutfitFit.fitted("test/formal", body, 1.0F, 0.001F)
            && OutfitFit.fitted("test/formal", body, 1.0F, 0.0F) != OutfitFit.fitted("test/formal", body, 1.0F, 0.5F)
            && OutfitFit.fitted("test/formal", body, 1.0F, 0.0F) != OutfitFit.fitted("test/formal", body, 0.85F, 0.0F)
            && same(OutfitFit.fitted("test/formal", body, 1.0F, 0.5F), OutfitFit.fit(OutfitFit.cutAll(body, 0.5F), 1.0F, 0.5F))
            // a bounce whose tip is off every strip line: cut there, not at rest
            && same(OutfitFit.fitted("test/formal", body, 1.0F, 0.38F), OutfitFit.fit(OutfitFit.cutAll(body, 0.38F), 1.0F, 0.38F)),
            "fitted kept by her figure and bounce, as cut and fitted there");
        // Where it is wired.
        String layer = read("src/main/java/com/hex/kagune/control/client/render/OutfitLayer.java");
        check(layer.contains("float[] fit = FemaleFigure.fit(wearer, partialTick);")
            && layer.contains("piece.slot() == OutfitModels.CHESTPLATE && !piece.mask() ? fit : null")
            && layer.contains("if (fit != null && bone == OutfitModels.BODY && !cubes.isEmpty()) {")
            && layer.contains("cubes = OutfitFit.fitted(key, cubes, fit[0], fit[1]);"),
            "OutfitLayer draws a woman's coat body fitted to her bust");
        String figure = read("src/main/java/com/hex/kagune/control/client/render/FemaleFigure.java");
        check(figure.contains("return !wearer.m_6844_(net.minecraft.world.entity.EquipmentSlot.CHEST).m_41619_();")
            && figure.contains("curves = com.hex.kagune.control.Genders.female(player) ? 1.0F : 0.0F;")
            && figure.contains("curves = com.hex.kagune.control.npc.HumanRules.female(human.look()) ? com.hex.kagune.control.npc.HumanRules.WOMAN_CURVES : 0.0F;")
            && figure.contains("curves = agent.look().female() ? com.hex.kagune.control.npc.AgentRules.FEMALE_CURVES : 0.0F;")
            && figure.contains("curves = npc.persona().female() ? npc.persona().curves() : 0.0F;")
            && figure.contains("ChildRenderer.woman(com.hex.kagune.control.client.ChildClient.get(child.m_19879_())) ? com.hex.kagune.control.npc.Persona.TOUKA.curves()")
            && figure.contains("if (!(wearer instanceof com.hex.kagune.control.npc.CcgAgent)) {")
            && figure.contains("bounce = jiggle(wearer.m_19879_(), y, com.hex.kagune.control.client.PaleClient.clock(partial));")
            && figure.contains("return new float[] {curves, bounce};"),
            "every woman's figure as her own bust layer draws it: a player, a villager, an investigator, a companion, a grown daughter");
        check(read("src/main/java/com/hex/kagune/control/client/render/HumanRenderer.java").contains("|| FemaleFigure.covered(human)) {")
            && read("src/main/java/com/hex/kagune/control/client/render/AgentRenderer.java").contains("|| FemaleFigure.covered(agent)) {")
            && read("src/main/java/com/hex/kagune/control/client/render/EtoRenderer.java").contains("if (!FemaleFigure.covered(npc)) {")
            && read("src/main/java/com/hex/kagune/control/client/render/ChildRenderer.java").contains("|| FemaleFigure.covered(child)) {")
            && read("src/main/java/com/hex/kagune/control/client/render/FigureLayer.java").contains("|| !player.m_6844_(net.minecraft.world.entity.EquipmentSlot.CHEST).m_41619_()) {"),
            "no woman's skin bust drawn under anything worn on her chest (a player's never was)");
    }

    /** A face's diagonals' cross (its corners' own facing, unnormalised). */
    static double[] across(float[] c) {
        double ax = c[6] - c[0];
        double ay = c[7] - c[1];
        double az = c[8] - c[2];
        double bx = c[9] - c[3];
        double by = c[10] - c[4];
        double bz = c[11] - c[5];
        return new double[] {ay * bz - az * by, az * bx - ax * bz, ax * by - ay * bx};
    }

    static int faces(List<OutfitModels.Cube> cubes) {
        int n = 0;
        for (OutfitModels.Cube c : cubes) {
            n += c.faces().length;
        }
        return n;
    }

    static double area(List<OutfitModels.Cube> cubes) {
        double a = 0.0;
        for (OutfitModels.Cube cube : cubes) {
            for (OutfitModels.Face f : cube.faces()) {
                float[] c = f.corners();
                double ax = c[6] - c[0];
                double ay = c[7] - c[1];
                double az = c[8] - c[2];
                double bx = c[9] - c[3];
                double by = c[10] - c[4];
                double bz = c[11] - c[5];
                double nx = ay * bz - az * by;
                double ny = az * bx - ax * bz;
                double nz = ax * by - ay * bx;
                a += 0.5 * Math.sqrt(nx * nx + ny * ny + nz * nz);
            }
        }
        return a;
    }

    /** A cube's faces' texture box (u0, v0, u1, v1). */
    static float[] uvBox(OutfitModels.Face[] faces) {
        float[] box = {Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        for (OutfitModels.Face f : faces) {
            for (int k = 0; k < 4; k++) {
                box[0] = Math.min(box[0], f.uvs()[k * 2]);
                box[1] = Math.min(box[1], f.uvs()[k * 2 + 1]);
                box[2] = Math.max(box[2], f.uvs()[k * 2]);
                box[3] = Math.max(box[3], f.uvs()[k * 2 + 1]);
            }
        }
        return box;
    }

    /** The frontmost (least z) of the faces facing forward over ({@code x}, {@code y}); a large number if none. */
    static double frontAt(List<OutfitModels.Cube> cubes, double x, double y) {
        double best = 99.0;
        for (OutfitModels.Cube cube : cubes) {
            for (OutfitModels.Face f : cube.faces()) {
                if (f.normal()[2] > -0.02F) {
                    continue; // facing forward at all (4.1.89 WIP: was -0.2, which missed the steep underside of a bust bounced down)
                }
                float[] c = f.corners();
                for (int[] t : new int[][] {{0, 1, 2}, {0, 2, 3}}) {
                    double x0 = c[t[0] * 3];
                    double y0 = c[t[0] * 3 + 1];
                    double x1 = c[t[1] * 3];
                    double y1 = c[t[1] * 3 + 1];
                    double x2 = c[t[2] * 3];
                    double y2 = c[t[2] * 3 + 1];
                    double d = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2);
                    if (Math.abs(d) < 1.0E-9) {
                        continue;
                    }
                    double l0 = ((y1 - y2) * (x - x2) + (x2 - x1) * (y - y2)) / d;
                    double l1 = ((y2 - y0) * (x - x2) + (x0 - x2) * (y - y2)) / d;
                    double l2 = 1.0 - l0 - l1;
                    if (l0 < -1.0E-6 || l1 < -1.0E-6 || l2 < -1.0E-6) {
                        continue;
                    }
                    best = Math.min(best, l0 * c[t[0] * 3 + 2] + l1 * c[t[1] * 3 + 2] + l2 * c[t[2] * 3 + 2]);
                }
            }
        }
        return best;
    }

    /** The same corners everywhere. */
    static boolean same(List<OutfitModels.Cube> a, List<OutfitModels.Cube> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int c = 0; c < a.size(); c++) {
            OutfitModels.Face[] fa = a.get(c).faces();
            OutfitModels.Face[] fb = b.get(c).faces();
            if (fa.length != fb.length) {
                return false;
            }
            for (int f = 0; f < fa.length; f++) {
                if (!java.util.Arrays.equals(fa[f].corners(), fb[f].corners())) {
                    return false;
                }
            }
        }
        return true;
    }

    static String read(String path) {
        try {
            return Files.readString(Path.of(path));
        } catch (java.io.IOException failure) {
            throw new IllegalStateException(failure);
        }
    }
}
