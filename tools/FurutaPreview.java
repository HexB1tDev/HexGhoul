import com.hex.kagune.control.ayato.CentipedeMotion;
import com.hex.kagune.control.ayato.CentipedeTail;
import com.hex.kagune.control.ayato.KakujaModel;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.furuta.FurutaArmor;
import com.hex.kagune.control.furuta.FurutaModel;
import com.hex.kagune.control.furuta.FurutaMove;
import com.hex.kagune.control.furuta.FurutaRig;
import com.hex.kagune.control.furuta.FurutaStage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Furuta DNA off-game (7.0.0): the game's own rig ({@link FurutaRig}) and meshes ({@link FurutaModel}, {@link FurutaArmor}) on the
 * posed body, in {@link ArtPreview}'s rasterizer. Usage: {@code FurutaPreview <out-dir> [sheet ...]}; sheets: stages, details,
 * kakuja, moves, deploy.
 */
public final class FurutaPreview {
    private FurutaPreview() {
    }

    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "build/preview");
        out.mkdirs();
        java.util.Set<String> wanted = new java.util.HashSet<>(java.util.Arrays.asList(args).subList(Math.min(1, args.length), args.length));
        if (wanted.isEmpty() || wanted.contains("stages")) {
            stages(new File(out, "furuta-stages.png"));
        }
        if (wanted.isEmpty() || wanted.contains("details")) {
            details(new File(out, "furuta-details.png"));
        }
        if (wanted.isEmpty() || wanted.contains("kakuja")) {
            kakuja(new File(out, "furuta-kakuja.png"));
        }
        if (wanted.isEmpty() || wanted.contains("moves")) {
            moves(new File(out, "furuta-moves.png"));
        }
        if (wanted.isEmpty() || wanted.contains("deploy")) {
            deploy(new File(out, "furuta-deploy.png"));
        }
        if (wanted.isEmpty() || wanted.contains("dragon")) {
            dragon(new File(out, "furuta-dragon.png"));
        }
        if (wanted.contains("hunt")) {
            hunt(new File(out, "furuta-hunt.png"));
        }
        if (wanted.contains("fortress")) {
            fortress(new File(out, "furuta-fortress.png"));
        }
        for (FurutaStage stage : FurutaStage.values()) {
            if (wanted.contains("frames") || wanted.contains("frames-" + stage.id())) {
                frames(out, stage);
            }
        }
        if (wanted.contains("npc")) {
            npc(new File(out, "ghoul-npc.png"));
        }
        if (wanted.contains("npc-scene")) {
            // The world behind the talk screen (tools/UiPreview.java talk): a ghoul NPC facing the player, close.
            RigPose still = new RigPose();
            // (Talking, a ghoul is calm: its eyes are its own; the kakugan only opens with the kagune.)
            ArtPreview.Shot shot = ghoul("", still, ArtPreview.NO_WINGS, new double[] {0.35, 1.65, 3.1}, new double[] {0.0, 1.3, 0.0}, new ArrayList<>(), 2);
            javax.imageio.ImageIO.write(ArtPreview.render(shot, 960, 540), "png", new File(out, "npc-scene.png"));
            System.out.println("wrote " + new File(out, "npc-scene.png"));
        }
        if (wanted.contains("npc-fight")) {
            npcFight(new File(out, "ghoul-npc-fight.png"));
        }
        if (wanted.contains("npc-skins")) {
            npcSkins(new File(out, "ghoul-npc-skins.png"), false);
            npcSkins(new File(out, "ghoul-npc-faces.png"), true);
        }
        if (wanted.contains("npc-flips")) {
            npcFlips(new File(out, "ghoul-npc-flips.png"));
        }
        if (wanted.contains("head")) {
            head(new File(out, "furuta-head.png"));
        }
        if (wanted.contains("mouth")) {
            mouth(new File(out, "furuta-mouth.png"));
        }
        if (wanted.contains("maws")) {
            maws(new File(out, "furuta-maws.png"));
        }
        if (wanted.contains("tails")) {
            tails(new File(out, "furuta-two-tails.png"));
        }
        if (wanted.contains("kakuja-four")) {
            kakujaFour(new File(out, "furuta-final-kakuja.png"));
        }
        if (wanted.contains("eyes")) {
            eyes(new File(out, "furuta-mass-of-eyes.png"));
        }
        if (wanted.contains("thorns")) {
            thorns(new File(out, "furuta-thorns.png"));
        }
        if (wanted.contains("centipedes")) {
            centipedes(new File(out, "furuta-centipedes.png"));
        }
        if (wanted.contains("tails-counter")) {
            TAILS_COUNTER = true;
            tails(new File(out, "furuta-mockingbird-counter.png"));
            TAILS_COUNTER = false;
        }
    }

    // ---------------------------------------------------------------- the ghoul NPCs (7.0.2)

    /** 1.0.41: the ghoul NPCs' own skins ({@code NpcLook}, tools/ghoul_skins.py), read from the mod's resources. */
    static java.awt.image.BufferedImage ghoulSkin(int index) throws Exception {
        return javax.imageio.ImageIO.read(new File(ROOT + "/src/main/resources/assets/hexkagune/" + com.hex.kagune.control.npc.NpcLook.texture(index)));
    }

    /** A shot of a ghoul NPC in skin {@code index} (a girl's on the slim arms), the kakugan on that skin's eyes as the game places it. */
    static ArtPreview.Shot ghoul(String label, RigPose pose, com.hex.kagune.control.ayato.WingRig.Input wings, double[] eye, double[] at,
                                 List<WingPreview.Tri> extra, int index) throws Exception {
        ArtPreview.Shot shot = worn(label, pose, wings, eye, at, extra, ghoulSkin(index));
        ArtPreview.STIFF.add(shot);
        if (com.hex.kagune.control.npc.NpcLook.girl(index)) {
            ArtPreview.SLIM.add(shot);
        }
        return shot;
    }

    /** The kakugan on a posed head on ghoul skin {@code index}'s eyes ({@code NpcLook.eyes}), under its second layer. */
    static List<WingPreview.Tri> kakugan(RigPose pose, int index) throws Exception {
        return kakugan(pose, ghoulSkin(index), com.hex.kagune.control.npc.NpcLook.eyes(index));
    }

    /** A skin's pixels, ARGB, as the game's skin reader hands them over. */
    static int[] pixels(java.awt.image.BufferedImage skin) {
        int[] out = new int[64 * 64];
        for (int v = 0; v < 64; v++) {
            for (int u = 0; u < 64; u++) {
                out[v * 64 + u] = skin.getRGB(u, v);
            }
        }
        return out;
    }

    /** The kakugan on a posed head, on the eyes found on {@code skin} (the game's own reader), under its second layer. */
    static List<WingPreview.Tri> kakugan(RigPose pose, java.awt.image.BufferedImage skin) {
        com.hex.kagune.control.kakugan.KakuganEyes found = com.hex.kagune.control.kakugan.KakuganSkin.detect(pixels(skin), 64);
        return kakugan(pose, skin, found == null ? com.hex.kagune.control.kakugan.KakuganEyes.DEFAULT : found);
    }

    /** The same on the eyes given. */
    static List<WingPreview.Tri> kakugan(RigPose pose, java.awt.image.BufferedImage skin, com.hex.kagune.control.kakugan.KakuganEyes eyes) {
        int[] px = pixels(skin);
        long covered = com.hex.kagune.control.kakugan.KakuganSkin.covered(px, 64) & ~com.hex.kagune.control.kakugan.KakuganSkin.glass(px, 64);
        List<WingPreview.Tri> out = new ArrayList<>();
        for (com.hex.kagune.control.kakugan.KakuganModel.Quad q : com.hex.kagune.control.kakugan.KakuganModel.build(eyes, 7, 1.0, 1.0F, true,
            com.hex.kagune.control.kakugan.KakuganModel.DEFAULT_LAYER, covered)) {
            float[] x = q.xyz();
            double[][] c = new double[4][];
            for (int i = 0; i < 4; i++) {
                c[i] = ArtPreview.onPart(pose, "head", new double[] {x[i * 3], x[i * 3 + 1], x[i * 3 + 2]});
            }
            double[] n = q.glow() ? new double[] {0, 1, 0} : PoseRender.norm(PoseRender.cross(PoseRender.sub(c[1], c[0]), PoseRender.sub(c[2], c[0])));
            int[] a = q.argb();
            out.add(new WingPreview.Tri(new double[][] {c[0], c[1], c[2]}, new int[] {a[0], a[1], a[2]}, n, false, q.glow()));
            out.add(new WingPreview.Tri(new double[][] {c[0], c[2], c[3]}, new int[] {a[0], a[2], a[3]}, n, false, q.glow()));
        }
        return out;
    }

    /** An ukaku ghoul's twin wings, out and at rest (Ayato's own rig at {@code mastery}). */
    static com.hex.kagune.control.ayato.WingRig.Input npcWings(int mastery) {
        return new com.hex.kagune.control.ayato.WingRig.Input(com.hex.kagune.control.ayato.WingForm.TWIN, mastery, 1.0F, 40.0, 0.0F, 0.0F,
            com.hex.kagune.control.ayato.WingRig.Motion.NONE, 0.0F, 0.4F, 0.0F, 0.0F, false, 1.0F, 1.0F, false);
    }

    static ArtPreview.Shot worn(String label, RigPose pose, com.hex.kagune.control.ayato.WingRig.Input wings, double[] eye, double[] at,
                                List<WingPreview.Tri> extra, java.awt.image.BufferedImage skin) {
        ArtPreview.Shot shot = new ArtPreview.Shot(label, pose, wings, eye, at, extra);
        ArtPreview.SKINS.put(shot, skin);
        return shot;
    }

    /**
     * The ghoul NPCs' look: a rinkaku ghoul with the transplant's tails out (one tail, then two), an ukaku ghoul with Ayato's twin
     * wings out, from round each, in their own skins, the kakugan open on each skin's eyes.
     */
    static void npc(File file) throws Exception {
        RigPose still = new RigPose();
        List<ArtPreview.Shot> shots = new ArrayList<>();
        int rinkaku = 2;
        int ukaku = 14;
        for (int mastery : new int[] {0, 200}) {
            double size = FurutaStage.TRANSPLANT.length(mastery);
            String tails = mastery == 0 ? "one tail" : "two tails";
            double[] look = {0.0, 1.5 + size * 0.15, -0.4};
            double[][] eyes = {{size * 0.75, 1.9 + size * 0.25, size * 1.35}, {-size * 0.9, 2.4 + size * 0.35, -size * 1.25}, {size * 1.45, 1.8, -0.2}};
            String[] views = {"front", "behind", "side"};
            for (int v = 0; v < eyes.length; v++) {
                List<WingPreview.Tri> tris = with(kakugan(still, rinkaku), whole(still, FurutaStage.TRANSPLANT, mastery, FurutaMove.NONE, 0.0, null, 1.0,
                    eyes[v]));
                shots.add(ghoul("Rinkaku ghoul, " + tails + ": " + views[v], still, ArtPreview.NO_WINGS, eyes[v], look, tris, rinkaku));
            }
        }
        int wingMastery = 150;
        double[][] wingEyes = {{2.4, 2.1, 4.2}, {-2.8, 2.7, -3.8}, {4.6, 1.9, -0.3}};
        String[] wingViews = {"front", "behind", "side"};
        for (int v = 0; v < wingEyes.length; v++) {
            shots.add(ghoul("Ukaku ghoul, twin wings: " + wingViews[v], still, npcWings(wingMastery), wingEyes[v], new double[] {0.0, 1.45, -0.3},
                kakugan(still, ukaku), ukaku));
        }
        ArtPreview.sheet(file, "Ghoul NPCs: their own skins, the kakugan, the transplant's tails and the twin wings", shots, 3, 480, 400);
    }

    /**
     * 1.0.41: all of the ghoul NPCs' skins, the twelve boys (wide arms) then the twelve girls (slim), kakugan open: whole, from in
     * front and to the side ({@code faces}: close on the face, the kakugan on its eyes).
     */
    static void npcSkins(File file, boolean faces) throws Exception {
        RigPose still = new RigPose();
        List<ArtPreview.Shot> shots = new ArrayList<>();
        for (int i = 0; i < com.hex.kagune.control.npc.NpcLook.SKINS.length; i++) {
            String name = com.hex.kagune.control.npc.NpcLook.SKINS[i];
            double[] eye = faces ? new double[] {0.55, 1.82, 1.55} : new double[] {1.3, 1.45, 3.2};
            double[] at = faces ? new double[] {0.0, 1.72, 0.0} : new double[] {0.0, 0.95, 0.0};
            shots.add(ghoul(name + (com.hex.kagune.control.npc.NpcLook.girl(i) ? " (slim)" : " (wide)"), still, ArtPreview.NO_WINGS, eye, at,
                kakugan(still, i), i));
        }
        ArtPreview.sheet(file, faces ? "Ghoul NPC faces: the kakugan on each skin's eyes" : "Ghoul NPC skins: twelve boys (wide arms), twelve girls (slim)",
            shots, 6, faces ? 260 : 280, faces ? 240 : 400);
    }

    /**
     * 1.0.41: a ghoul NPC's flips as the game draws them (a player's flip pose, {@code EtoMotions.flip}, the whole body turned over
     * about its middle): a back flip away and a front flip in, frame by frame, from the side.
     */
    static void npcFlips(File file) throws Exception {
        List<ArtPreview.Shot> shots = new ArrayList<>();
        double[] ages = {0.0, 3.0, 6.0, 9.0, 12.0, 15.0, 18.0, 22.0};
        int[] who = {6, 18};
        com.hex.kagune.control.eto.EtoRules.Flip[] flips = {com.hex.kagune.control.eto.EtoRules.Flip.BACK, com.hex.kagune.control.eto.EtoRules.Flip.FORWARD};
        for (int f = 0; f < flips.length; f++) {
            for (double age : ages) {
                double landed = age >= 19.0 ? age - 19.0 : -1.0;
                RigPose pose = com.hex.kagune.control.client.animation.EtoMotions.flip(flips[f], age, landed);
                // (The body is drawn where it stands: the hop's rise is left out, the camera on its middle.)
                double[] eye = {5.2, 1.6, 0.0};
                double[] at = {0.0, 0.9, 0.0};
                shots.add(ghoul(flips[f] + " flip, tick " + (int)age, pose, ArtPreview.NO_WINGS, eye, at, new ArrayList<>(), who[f]));
            }
        }
        ArtPreview.sheet(file, "Ghoul NPC flips: a back flip clear and a front flip in (side view, ticks since the push)", shots, 8, 240, 300);
    }

    /**
     * The ghoul NPCs fighting, as the game draws them: a rinkaku ghoul (two tails) through the transplant's string, each lash as it
     * winds up, lands and follows through, at a dummy 3.5 blocks off (its body stays upright: a mob, not posed as a player); an ukaku
     * ghoul with both wings curled to aim at a dummy 8 blocks off, the right one kicking as its shot leaves.
     */
    static void npcFight(File file) throws Exception {
        RigPose still = new RigPose();
        List<ArtPreview.Shot> shots = new ArrayList<>();
        int rinkaku = 0;
        int ukaku = 21;
        int mastery = 150;
        double[] aim = {0.0, 1.3, 3.5};
        double[] eye = {9.0, 3.9, 1.7};
        double[] look = {0.0, 1.8, 1.7};
        FurutaMove move = FurutaMove.firstStrike(FurutaStage.TRANSPLANT);
        for (int i = 0; i < 5; i++) {
            int land = 0;
            int end = 0;
            boolean sweep = false;
            for (FurutaMove.Beat beat : move.beats(2)) {
                land = Math.max(land, beat.land());
                end = Math.max(end, beat.end());
                sweep |= beat.kind() == FurutaMove.Kind.SWEEP;
            }
            // A sweep hurts as it crosses the dummy (half way through its arc); a strike on its landing tick.
            double[] ages = sweep ? new double[] {Math.max(1.0, land - 1.0), land + (end - land) / 2.0, end} : new double[] {Math.max(1.0, land - 2.0), land,
                land + 3.0};
            String[] when = sweep ? new String[] {"winding up", "crossing it", "follow-through"} : new String[] {"winding up", "landing", "after"};
            for (int f = 0; f < 3; f++) {
                List<WingPreview.Tri> tris = with(with(kakugan(still, rinkaku), ArtPreview.target(3.5)), whole(still, FurutaStage.TRANSPLANT, mastery, move,
                    ages[f], aim, 1.0, eye));
                shots.add(ghoul(move.displayName() + ": " + when[f] + " (tick " + (int)ages[f] + ")", still, ArtPreview.NO_WINGS, eye, look, tris, rinkaku));
            }
            move = FurutaMove.nextStrike(move, FurutaStage.TRANSPLANT);
        }
        int wings = 200;
        double[] target = {0.0, 1.3, 8.0};
        double[] dir = {target[0], target[1] - com.hex.kagune.control.ayato.WingRig.ROOT_Y, target[2]};
        double length = Math.sqrt(dir[0] * dir[0] + dir[1] * dir[1] + dir[2] * dir[2]);
        dir = new double[] {dir[0] / length, dir[1] / length, dir[2] / length};
        com.hex.kagune.control.ayato.WingRig.Input aiming = new com.hex.kagune.control.ayato.WingRig.Input(com.hex.kagune.control.ayato.WingForm.TWIN,
            wings, 1.0F, 40.0, 0.0F, 0.0F, com.hex.kagune.control.ayato.WingRig.Motion.NONE, 0.0F, 0.4F, 0.0F, 0.0F, false, 1.0F, 1.0F, false, 1.0F, 1.0F,
            dir, 1.0F, 0.15F);
        double[][] wingEyes = {{4.8, 2.9, 7.8}, {9.0, 2.6, 3.0}, {-3.6, 3.3, -5.4}};
        String[] views = {"front", "side", "behind"};
        for (int v = 0; v < wingEyes.length; v++) {
            List<WingPreview.Tri> tris = with(with(kakugan(still, ukaku), ArtPreview.target(8.0)), ArtPreview.shards(wings, 1, dir, 1.0, 1.2, 4.5));
            shots.add(ghoul("Ukaku: aiming, a shot leaving the right wing (" + views[v] + ")", still, aiming, wingEyes[v], new double[] {0.0, 1.7, 2.6},
                tris, ukaku));
        }
        ArtPreview.sheet(file, "Ghoul NPCs fighting: the transplant's string (two tails) and the twin wings' shots", shots, 3, 480, 400);
    }

    /** The centipede's mouth from round it, at rest and gaping: no hole anywhere into or through it. */
    static void mouth(File file) throws Exception {
        RigPose still = new RigPose();
        List<ArtPreview.Shot> shots = new ArrayList<>();
        double[] at = {0.1, 3.4, 1.2};
        double[][] eyes = {{2.0, 4.3, 3.0}, {-2.2, 3.6, 1.6}, {0.1, 3.4, 4.2}, {2.4, 2.4, 1.0}, {0.2, 5.6, 0.4}};
        for (double[] eye : eyes) {
            shots.add(new ArtPreview.Shot("Rest", still, ArtPreview.NO_WINGS, eye, at, whole(still, FurutaStage.CENTIPEDE, 350, FurutaMove.NONE, 0.0, null,
                1.0, eye)));
        }
        double[] gape = {0.2, 2.8, 1.5};
        double[][] gapeEyes = {{2.6, 3.6, 5.0}, {-2.4, 3.0, 2.0}, {0.2, 2.8, 4.6}, {3.0, 2.8, 1.4}, {0.2, 5.0, 1.0}};
        for (double[] eye : gapeEyes) {
            shots.add(new ArtPreview.Shot("Gaping", still, ArtPreview.NO_WINGS, eye, gape, whole(still, FurutaStage.CENTIPEDE, 350, FurutaMove.RIP_3, 4.0,
                new double[] {0.0, 1.3, 6.0}, 1.0, eye)));
        }
        ArtPreview.sheet(file, "Furuta DNA: the mouth from round it", shots, 5, 400, 340);
    }

    /** Every mouth on its own, close: the centipede's, a colossus's, the devouring tail's; shut, half open, gaping. */
    static void maws(File file) throws Exception {
        List<ArtPreview.Shot> shots = new ArrayList<>();
        double[][] line = {{0.0, 1.0, -4.0}, {0.0, 1.2, -2.0}, {0.0, 1.5, 0.0}, {0.0, 1.6, 2.0}, {0.0, 1.5, 4.0}};
        Object[][] kinds = {{"Centipede", FurutaStage.CENTIPEDE, 0.33}, {"Colossus", FurutaStage.KAKUJA, 0.62}, {"Flesh Devourer", FurutaStage.TRANSPLANT, 0.37}};
        for (Object[] kind : kinds) {
            FurutaStage stage = (FurutaStage)kind[1];
            double width = (Double)kind[2];
            KakujaModel.Spine spine = CentipedeTail.through(line, 96, 1, new double[] {0.0, 1.0, 0.0});
            for (double jaw : new double[] {0.15, 0.6, 1.0}) {
                FurutaModel.Look look = new FurutaModel.Look(width, true, 1.0, 11, jaw, 1.0, jaw, true, 30.0, null);
                List<WingPreview.Tri> tris = ArtPreview.placed(sink -> FurutaModel.limb(sink, stage, spine, look), q -> q);
                double[] eye = jaw < 0.9 ? new double[] {1.9, 2.1, 5.6} : new double[] {0.5, 1.75, 5.9};
                shots.add(new ArtPreview.Shot(kind[0] + ", jaw " + jaw, null, ArtPreview.NO_WINGS, eye, new double[] {0.0, 1.5, 3.7}, tris));
            }
        }
        ArtPreview.sheet(file, "Furuta DNA: the mouths", shots, 3, 460, 380);
    }

    /** The kakuja head alone and on the body, shut and open. */
    static void head(File file) throws Exception {
        RigPose still = new RigPose();
        List<ArtPreview.Shot> shots = new ArrayList<>();
        for (double jaw : new double[] {0.0, 0.5, 1.0}) {
            List<WingPreview.Tri> alone = ArtPreview.placed(sink -> FurutaArmor.head(sink, 1.0, jaw, -2.0, -3.6, jaw, 0.0, true),
                q -> ArtPreview.onPart(still, "head", q));
            shots.add(new ArtPreview.Shot("Head alone, jaw " + jaw, null, ArtPreview.NO_WINGS, new double[] {0.7, 1.75, 1.6}, new double[] {0.0, 1.6, 0.0},
                alone));
            shots.add(new ArtPreview.Shot("On the body, jaw " + jaw, still, ArtPreview.NO_WINGS, new double[] {0.7, 1.75, 1.6}, new double[] {0.0, 1.6, 0.0},
                kakujaBody(still, 1.0, jaw, 0.0)));
            shots.add(new ArtPreview.Shot("Side, jaw " + jaw, still, ArtPreview.NO_WINGS, new double[] {1.9, 1.7, 0.2}, new double[] {0.0, 1.55, 0.0},
                kakujaBody(still, 1.0, jaw, 0.0)));
        }
        ArtPreview.sheet(file, "Furuta DNA: the kakuja head", shots, 3, 420, 380);
    }

    /** Every stage at rest, from behind and in front. */
    static void stages(File file) throws Exception {
        RigPose still = new RigPose();
        List<ArtPreview.Shot> shots = new ArrayList<>();
        Object[][] cases = {
            {"Rize's Kakuhou: one tail (mastery 0)", FurutaStage.TRANSPLANT, 0},
            {"Rize's Kakuhou: two tails (100+)", FurutaStage.TRANSPLANT, 200},
            {"Centipede Rinkaku: three, one with a mouth (250)", FurutaStage.CENTIPEDE, 350},
            {"Four Thorns (450)", FurutaStage.THORNS, 600},
            {"Many-Eyed Kagune (700)", FurutaStage.DRAGON, 800},
            {"Kakuja (900, K)", FurutaStage.KAKUJA, 1000},
        };
        for (Object[] c : cases) {
            FurutaStage stage = (FurutaStage)c[1];
            int mastery = (Integer)c[2];
            double size = stage.length(mastery);
            double[] eye = {-size * 0.9, 2.4 + size * 0.35, -size * 1.25};
            double[] look = {0.0, 1.6 + size * 0.2, 0.0};
            shots.add(new ArtPreview.Shot(c[0] + ": behind", still, ArtPreview.NO_WINGS, eye, look, whole(still, stage, mastery, FurutaMove.NONE, 0.0, null,
                1.0, eye)));
            double[] front = {size * 0.75, 1.9 + size * 0.25, size * 1.35};
            shots.add(new ArtPreview.Shot(c[0] + ": front", still, ArtPreview.NO_WINGS, front, look, whole(still, stage, mastery, FurutaMove.NONE, 0.0, null,
                1.0, front)));
        }
        ArtPreview.sheet(file, "Furuta DNA: the kagune at rest, stage by stage", shots, 4, 520, 420);
    }

    /** The many-eyed kagune from all round him: the towering stack of crescents on his right, its lowest layer on the ground. */
    static void dragon(File file) throws Exception {
        RigPose still = new RigPose();
        List<ArtPreview.Shot> shots = new ArrayList<>();
        double[] look = {1.8, 3.2, 0.3};
        Object[][] views = {{"As the panel: in front of him, low", new double[] {-2.2, 1.5, 11.5}}, {"Front, from his left", new double[] {-7.5, 3.4, 10.0}},
            {"Front, from his right", new double[] {11.0, 3.8, 8.0}}, {"His right side", new double[] {13.0, 3.6, 0.5}},
            {"Behind", new double[] {1.0, 4.0, -12.0}}, {"Behind, from his left", new double[] {-8.0, 4.5, -9.0}}};
        for (Object[] view : views) {
            double[] eye = (double[])view[1];
            shots.add(new ArtPreview.Shot((String)view[0], still, ArtPreview.NO_WINGS, eye, look, whole(still, FurutaStage.DRAGON, 800, FurutaMove.NONE, 0.0, null,
                1.0, eye)));
        }
        double[] close = {4.5, 3.0, 6.0};
        shots.add(new ArtPreview.Shot("The almond eyes up close", still, ArtPreview.NO_WINGS, close, new double[] {2.6, 2.6, 0.0}, whole(still, FurutaStage.DRAGON,
            800, FurutaMove.NONE, 0.0, null, 1.0, close)));
        double[] full = {1.2, 3.6, 13.0};
        shots.add(new ArtPreview.Shot("At mastery 1000 (longest)", still, ArtPreview.NO_WINGS, full, look, whole(still, FurutaStage.DRAGON, 1000, FurutaMove.NONE,
            0.0, null, 1.0, full)));
        double[] back = {-3.2, 2.9, -4.6};
        shots.add(new ArtPreview.Shot("His back: thin where they leave him", still, ArtPreview.NO_WINGS, back, new double[] {0.6, 1.8, -0.5}, whole(still,
            FurutaStage.DRAGON, 800, FurutaMove.NONE, 0.0, null, 1.0, back)));
        ArtPreview.sheet(file, "Furuta DNA: the many-eyed kagune (\"let's play house\")", shots, 3, 600, 480);
    }

    /** Close-ups: surfaces, the mouth shut and open, the eyes, the kakuja head. */
    static void details(File file) throws Exception {
        RigPose still = new RigPose();
        List<ArtPreview.Shot> shots = new ArrayList<>();
        // The rinkaku tail up close.
        shots.add(new ArtPreview.Shot("Rinkaku tail: muscle bands, crimson veins, spear point", still, ArtPreview.NO_WINGS, new double[] {4.6, 3.6, -3.4},
            new double[] {1.6, 2.2, -0.4}, whole(still, FurutaStage.TRANSPLANT, 200, FurutaMove.NONE, 0.0, null, 1.0, new double[] {4.6, 3.6, -3.4})));
        // The centipede mouth: shut, then open (the bite's wind-up).
        double[] mawEye = {2.0, 4.3, 3.0};
        double[] mawAt = {0.1, 3.4, 1.2};
        shots.add(new ArtPreview.Shot("Centipede mouth at rest (half open, breathing)", still, ArtPreview.NO_WINGS, mawEye, mawAt, whole(still, FurutaStage.CENTIPEDE, 350, FurutaMove.NONE,
            0.0, null, 1.0, mawEye)));
        shots.add(new ArtPreview.Shot("Centipede mouth, gaping (Snapping Lunge wind-up)", still, ArtPreview.NO_WINGS, new double[] {2.6, 3.6, 5.0},
            new double[] {0.2, 2.8, 1.5}, whole(still, FurutaStage.CENTIPEDE, 350, FurutaMove.RIP_3, 4.0, new double[] {0.0, 1.3, 6.0}, 1.0,
                new double[] {2.6, 3.6, 5.0})));
        shots.add(new ArtPreview.Shot("Side centipede: plates, flank spikes, keel hooks", still, ArtPreview.NO_WINGS, new double[] {-2.0, 2.6, 3.5},
            new double[] {-2.2, 1.6, 0.0}, whole(still, FurutaStage.CENTIPEDE, 350, FurutaMove.NONE, 0.0, null, 1.0, new double[] {-2.0, 2.6, 3.5})));
        shots.add(new ArtPreview.Shot("Thorns: armored ridges, thorn rings, serrated point", still, ArtPreview.NO_WINGS, new double[] {-3.5, 4.5, 4.5},
            new double[] {-3.5, 2.0, 0.0}, whole(still, FurutaStage.THORNS, 600, FurutaMove.NONE, 0.0, null, 1.0, new double[] {-3.5, 4.5, 4.5})));
        shots.add(new ArtPreview.Shot("The many-eyed crescents: almond eyes in black rims", still, ArtPreview.NO_WINGS, new double[] {-4.0, 3.2, 6.5},
            new double[] {-3.6, 2.2, 0.0}, whole(still, FurutaStage.DRAGON, 800, FurutaMove.NONE, 0.0, null, 1.0, new double[] {-4.0, 3.2, 6.5})));
        shots.add(new ArtPreview.Shot("Kakuja head: one kakugan, the mouth shut", still, ArtPreview.NO_WINGS, new double[] {0.6, 1.75, 1.9},
            new double[] {0.0, 1.6, 0.0}, kakujaBody(still, 1.0, 0.0, 0.0)));
        shots.add(new ArtPreview.Shot("Kakuja head: the jaw thrown open", still, ArtPreview.NO_WINGS, new double[] {0.6, 1.75, 1.9},
            new double[] {0.0, 1.55, 0.0}, kakujaBody(still, 1.0, 1.0, 0.0)));
        shots.add(new ArtPreview.Shot("Colossus head: mandibles, mouth, eyes", still, ArtPreview.NO_WINGS, new double[] {3.5, 6.5, 6.2},
            new double[] {1.2, 5.2, 2.6}, whole(still, FurutaStage.KAKUJA, 1000, FurutaMove.CHECKMATE, 5.0, new double[] {0.0, 1.3, 7.0}, 1.0,
                new double[] {3.5, 6.5, 6.2})));
        ArtPreview.sheet(file, "Furuta DNA: details", shots, 3, 560, 440);
    }

    /** The kakuja: armor front and back, the head, the colossi from round him. */
    static void kakuja(File file) throws Exception {
        // His kakuja stance as the game poses him: hunched, knees and elbows bent (the hide folding with them).
        RigPose still = posed(FurutaStage.KAKUJA, 1000, FurutaMove.NONE, 0.0);
        List<ArtPreview.Shot> shots = new ArrayList<>();
        shots.add(new ArtPreview.Shot("Front: the star dead centre", still, ArtPreview.NO_WINGS, new double[] {0.0, 1.5, 3.4}, new double[] {0.0, 1.1, 0.0},
            kakujaBody(still, 1.0, 0.25, 0.0)));
        shots.add(new ArtPreview.Shot("Three quarters: forearms and shins under the hide", still, ArtPreview.NO_WINGS, new double[] {2.6, 1.3, 2.4},
            new double[] {0.0, 1.0, 0.0}, kakujaBody(still, 1.0, 0.0, 0.0)));
        shots.add(new ArtPreview.Shot("Back: the colossi's sockets", still, ArtPreview.NO_WINGS, new double[] {0.6, 1.4, -3.6},
            new double[] {0.0, 1.1, 0.0}, kakujaBody(still, 1.0, 0.0, 0.0)));
        double[] eye = {7.5, 5.5, 11.0};
        shots.add(new ArtPreview.Shot("The whole kakuja, front three quarters", still, ArtPreview.NO_WINGS, eye, new double[] {0.5, 3.6, 0.0},
            with(kakujaBody(still, 1.0, 0.3, 0.0), whole(still, FurutaStage.KAKUJA, 1000, FurutaMove.NONE, 0.0, null, 1.0, eye))));
        double[] back = {-8.0, 6.0, -10.0};
        shots.add(new ArtPreview.Shot("Behind", still, ArtPreview.NO_WINGS, back, new double[] {0.5, 3.6, 0.0},
            with(kakujaBody(still, 1.0, 0.0, 0.0), whole(still, FurutaStage.KAKUJA, 1000, FurutaMove.NONE, 0.0, null, 1.0, back))));
        double[] side = {13.0, 4.5, 1.0};
        shots.add(new ArtPreview.Shot("Side", still, ArtPreview.NO_WINGS, side, new double[] {0.5, 3.6, 0.5},
            with(kakujaBody(still, 1.0, 0.0, 0.0), whole(still, FurutaStage.KAKUJA, 1000, FurutaMove.NONE, 0.0, null, 1.0, side))));
        ArtPreview.sheet(file, "Furuta DNA: the kakuja", shots, 3, 600, 480);
    }

    /** Key frames of the moves, one per stage's signature. */
    static void moves(File file) throws Exception {
        RigPose still = new RigPose();
        List<ArtPreview.Shot> shots = new ArrayList<>();
        Object[][] cases = {
            {"Rising Lash (M1, first)", FurutaStage.TRANSPLANT, 200, FurutaMove.LASH_1, 5.0, new double[] {0.0, 1.2, 4.0}},
            {"Gutting Upper (M1, fourth): ripped up", FurutaStage.TRANSPLANT, 200, FurutaMove.LASH_4, 6.5, new double[] {0.0, 1.0, 4.0}},
            {"Guillotine (M1, fifth)", FurutaStage.TRANSPLANT, 200, FurutaMove.LASH_5, 6.0, new double[] {0.0, 1.0, 4.0}},
            {"Snapping Lunge (centipede M1, third)", FurutaStage.CENTIPEDE, 350, FurutaMove.RIP_3, 5.5, new double[] {0.0, 1.3, 5.5}},
            {"Centipede Crash (M1, fifth)", FurutaStage.CENTIPEDE, 350, FurutaMove.RIP_5, 9.0, new double[] {0.0, 1.0, 4.0}},
            {"Cross Cleave (thorns M1, third)", FurutaStage.THORNS, 600, FurutaMove.THORN_3, 7.5, new double[] {0.0, 1.2, 5.0}},
            {"Crescent Jaws (many-eyed M1, fourth)", FurutaStage.DRAGON, 800, FurutaMove.CRESCENT_4, 6.5, new double[] {0.0, 1.2, 5.0}},
            {"Mockingbird (1): poised low", FurutaStage.TRANSPLANT, 200, FurutaMove.MOCKINGBIRD, 15.0, new double[] {0.0, 1.3, 4.0}},
            {"Owl's Execution (2): a cut", FurutaStage.TRANSPLANT, 200, FurutaMove.OWLS_EXECUTION, 10.0, new double[] {0.0, 1.2, 2.6}},
            {"Human Puppet (4): held out", FurutaStage.TRANSPLANT, 200, FurutaMove.HUMAN_PUPPET, 14.0, new double[] {0.4, 1.3, 3.4}},
            {"Hungry Maw (centipede 1): out after it", FurutaStage.CENTIPEDE, 400, FurutaMove.HUNGRY_MAW, 8.0, new double[] {0.0, 1.2, 8.0}},
            {"The Ambush (3): up from under", FurutaStage.CENTIPEDE, 400, FurutaMove.AMBUSH, 10.0, new double[] {0.0, 1.2, 4.5}},
            {"Living Carapace (4)", FurutaStage.CENTIPEDE, 400, FurutaMove.CARAPACE, 20.0, new double[] {0.0, 1.2, 4.0}},
            {"Crucifixion (thorns 1): held up", FurutaStage.THORNS, 600, FurutaMove.CRUCIFIXION, 16.0, new double[] {0.0, 2.9, 3.6}},
            {"Unstoppable Breach (2)", FurutaStage.THORNS, 600, FurutaMove.BREACH, 10.0, new double[] {0.0, 1.3, 5.0}},
            {"Dragon's Dragnet (3): swept in", FurutaStage.THORNS, 600, FurutaMove.DRAGNET, 10.0, new double[] {0.0, 1.0, 3.5}},
            {"Eye of the Storm (many-eyed 1)", FurutaStage.DRAGON, 800, FurutaMove.STORM, 30.0, new double[] {0.0, 1.2, 6.0}},
            {"Flesh Bloom (2): a wave of spikes", FurutaStage.DRAGON, 800, FurutaMove.BLOOM, 14.0, new double[] {0.0, 1.2, 6.0}},
            {"Black Curtain (3): the wall", FurutaStage.DRAGON, 800, FurutaMove.CURTAIN, 9.0, new double[] {0.0, 1.2, 6.0}},
            {"Kakuja: King's Checkmate (1), hooked", FurutaStage.KAKUJA, 1000, FurutaMove.CHECKMATE, 12.0, new double[] {0.0, 1.0, 5.0}},
            {"Kakuja: Centipede Hunt (2), biting", FurutaStage.KAKUJA, 1000, FurutaMove.HUNT, 12.0, new double[] {0.0, 1.0, 7.0}},
            {"Kakuja: Predator's Finish (M1, sixth)", FurutaStage.KAKUJA, 1000, FurutaMove.CLAW_6, 7.0, new double[] {0.0, 0.0, 5.0}},
        };
        for (Object[] c : cases) {
            FurutaStage stage = (FurutaStage)c[1];
            int mastery = (Integer)c[2];
            double size = stage.length(mastery);
            double[] eye = {size * 1.3, 2.2 + size * 0.45, size * 0.9};
            RigPose body = posed(stage, mastery, (FurutaMove)c[3], (Double)c[4]);
            List<WingPreview.Tri> tris = whole(body, stage, mastery, (FurutaMove)c[3], (Double)c[4], (double[])c[5], 1.0, eye);
            if (stage == FurutaStage.KAKUJA) {
                tris = with(kakujaBody(body, 1.0, 0.8, 0.0), tris);
            }
            shots.add(new ArtPreview.Shot((String)c[0], body, ArtPreview.NO_WINGS, eye, new double[] {0.0, 1.6, size * 0.25}, tris));
        }
        ArtPreview.sheet(file, "Furuta DNA: moves (key frames from the game's own beats)", shots, 4, 480, 400);
    }

    // ---------------------------------------------------------------- frame by frame

    /** The mastery each stage's frame sheets show it at (every limb it has out). */
    static int frameMastery(FurutaStage stage) {
        return switch (stage) {
            case TRANSPLANT -> 200;
            case CENTIPEDE -> 350;
            case THORNS -> 600;
            case DRAGON -> 800;
            case KAKUJA -> 1000;
        };
    }

    static final int FRAMES = 6;

    /**
     * Every move of {@code stage}, frame by frame, as the game draws it: two sheets, its M1 string (and the dash) and its abilities,
     * a row a move, {@link #FRAMES} frames over its ticks. His body is posed as the game poses it, the limbs follow the rig's poses
     * on the game's own springs from rest, a dummy stands where the move is aimed (or is carried where the server carries it), and
     * on the tick a blow lands a yellow marker shows where the server hits (a strike's point, a slam's ground, a sweep's arc).
     */
    static void frames(File dir, FurutaStage stage) throws Exception {
        int mastery = frameMastery(stage);
        List<FurutaMove> string = new ArrayList<>();
        for (FurutaMove move : FurutaMove.values()) {
            if (move.combo() && move.stage() == stage) {
                string.add(move);
            }
        }
        if (stage == FurutaStage.TRANSPLANT) {
            string.add(FurutaMove.ACCORD);
        }
        string.add(FurutaMove.STEP);
        List<FurutaMove> abilities = new ArrayList<>();
        for (FurutaMove.Slot slot : new FurutaMove.Slot[] {FurutaMove.Slot.Z, FurutaMove.Slot.J, FurutaMove.Slot.V, FurutaMove.Slot.B, FurutaMove.Slot.N,
            FurutaMove.Slot.H}) {
            FurutaMove move = FurutaMove.of(slot, stage);
            if (move != FurutaMove.NONE) {
                abilities.add(move);
            }
        }
        frameSheet(new File(dir, "furuta-frames-" + stage.id() + "-m1.png"), stage, mastery, string, "M1 string and dash");
        frameSheet(new File(dir, "furuta-frames-" + stage.id() + "-abilities.png"), stage, mastery, abilities, "abilities");
    }

    static void frameSheet(File file, FurutaStage stage, int mastery, List<FurutaMove> moves, String what) throws Exception {
        List<ArtPreview.Shot> shots = new ArrayList<>();
        int n = stage.limbs(mastery);
        double size = stage.length(mastery);
        for (FurutaMove move : moves) {
            double[] look;
            double[] eye;
            if (sweeping(move, n)) {
                // Sweeps and spins from high in front (a little to his left), to read where the tips are round him.
                look = new double[] {0.0, 0.9 + 0.12 * size, 1.6};
                eye = new double[] {-(size * 0.45 + 1.0), 3.0 + 1.15 * size, 1.6 + size * 0.75 + 2.2};
            } else {
                // Strikes from his right side, a little in front and above: the line from his back to the dummy, the height of it
                // (drawn back for the long reaches so their dummy stays in view).
                double far = Math.max(0.0, frameAim(move, n)[2] - 4.5);
                look = new double[] {0.0, 1.2 + 0.2 * size, 2.4 + 0.5 * far};
                eye = new double[] {size * 1.25 + 4.0 + 0.9 * far, 2.4 + 0.45 * size, 3.4 + 0.25 * size + 0.5 * far};
            }
            double[] aim = frameAim(move, n);
            double[][] targets = frameTargets(move, n, aim);
            Simulated sim = new Simulated(stage, mastery, move, aim, targets);
            for (double age : frameAges(move, n)) {
                sim.run(age);
                RigPose body = posed(stage, mastery, move, age);
                List<WingPreview.Tri> tris = sim.draw(body, age);
                double[] held = sim.held(age);
                if (held != null) {
                    tris.addAll(EtoPreview.dummyAt(new double[] {held[0], held[1] - 1.0, held[2]}));
                } else if (targets != null) {
                    for (double[] t : targets) {
                        tris.addAll(EtoPreview.dummyAt(new double[] {t[0], 0.0, t[2]}));
                    }
                } else {
                    tris.addAll(EtoPreview.dummyAt(new double[] {aim[0], 0.0, aim[2]}));
                }
                String landing = hits(tris, stage, n, size, move, age, aim, targets);
                if (stage == FurutaStage.KAKUJA) {
                    tris = with(kakujaBody(body, 1.0, sim.ownJaw(age), age), tris);
                }
                String label = move.displayName() + " t" + trim(age) + (landing.isEmpty() ? "" : " " + landing);
                shots.add(new ArtPreview.Shot(label, body, ArtPreview.NO_WINGS, eye, look, tris));
            }
        }
        ArtPreview.sheet(file, "Furuta DNA, " + stage.displayName() + " (" + mastery + "): " + what
            + " frame by frame (the game's springs; yellow: where the server hits that tick)", shots, FRAMES, 300, 250);
    }

    // ---------------------------------------------------------------- the two tails' four (7.0.2, the user's rework)

    /**
     * The world round him through a chain (rig space, his feet at the origin): the move's point, what it holds, the dummy, the ground
     * (its height, its turn round the dummy, how far it has slid back under him), where he looks; 7.0.2's centipedes: a limb's own
     * target (the mouth's prey), what the job holds, and scenery (a wall the mouth bends over).
     */
    static final class Scene {
        java.util.function.DoubleFunction<double[]> aim;
        java.util.function.DoubleFunction<double[]> held;
        java.util.function.DoubleFunction<double[]> dummy;
        java.util.function.DoubleUnaryOperator floor;
        java.util.function.DoubleUnaryOperator pitch;
        java.util.function.DoubleUnaryOperator turn;
        java.util.function.DoubleFunction<double[]> target = t -> null;
        java.util.function.DoubleFunction<double[]> jobHeld = t -> null;
        java.util.function.DoubleUnaryOperator slide = t -> 0.0;
        boolean floorUnderHim;
        List<WingPreview.Tri> props = new ArrayList<>();

        Scene(java.util.function.DoubleFunction<double[]> aim, java.util.function.DoubleFunction<double[]> held, java.util.function.DoubleFunction<double[]> dummy,
              java.util.function.DoubleUnaryOperator floor, java.util.function.DoubleUnaryOperator pitch, java.util.function.DoubleUnaryOperator turn) {
            this.aim = aim;
            this.held = held;
            this.dummy = dummy;
            this.floor = floor;
            this.pitch = pitch;
            this.turn = turn;
        }

        java.util.function.DoubleFunction<double[]> aim() {
            return this.aim;
        }

        java.util.function.DoubleFunction<double[]> held() {
            return this.held;
        }

        java.util.function.DoubleFunction<double[]> dummy() {
            return this.dummy;
        }

        java.util.function.DoubleUnaryOperator floor() {
            return this.floor;
        }

        java.util.function.DoubleUnaryOperator pitch() {
            return this.pitch;
        }

        java.util.function.DoubleUnaryOperator turn() {
            return this.turn;
        }
    }

    /** A move and what follows on from it, one after another, the limbs on the game's springs throughout (as {@link Simulated}). */
    static final class Chain {
        static final int MASTERY = 200;
        final FurutaStage stage;
        final int mastery;
        final int n;
        final FurutaMove[] moves;
        final double[] starts;
        final Scene scene;
        final com.hex.kagune.control.furuta.FurutaMotion.Limb[] legs;
        FurutaMove[] jobs = new FurutaMove[0];
        double[] jobStarts = new double[0];
        double time = -24.0;

        Chain(Scene scene, FurutaMove[] moves, double[] starts) {
            this(FurutaStage.TRANSPLANT, MASTERY, scene, moves, starts);
        }

        Chain(FurutaStage stage, int mastery, Scene scene, FurutaMove[] moves, double[] starts) {
            this.stage = stage;
            this.mastery = mastery;
            this.n = stage.limbs(mastery);
            this.scene = scene;
            this.moves = moves;
            this.starts = starts;
            this.legs = new com.hex.kagune.control.furuta.FurutaMotion.Limb[this.n];
            for (int i = 0; i < this.legs.length; i++) {
                this.legs[i] = new com.hex.kagune.control.furuta.FurutaMotion.Limb();
            }
        }

        /** 7.0.2: the jobs running alongside, from when (each runs its own length). */
        Chain jobs(FurutaMove[] jobs, double[] starts) {
            this.jobs = jobs;
            this.jobStarts = starts;
            return this;
        }

        FurutaMove job(double t) {
            FurutaMove out = FurutaMove.NONE;
            for (int i = 0; i < this.jobs.length; i++) {
                if (t >= this.jobStarts[i] && t <= this.jobStarts[i] + this.jobs[i].ticks()) {
                    out = this.jobs[i];
                }
            }
            return out;
        }

        double jobAge(double t) {
            double age = 0.0;
            for (int i = 0; i < this.jobs.length; i++) {
                if (t >= this.jobStarts[i] && t <= this.jobStarts[i] + this.jobs[i].ticks()) {
                    age = t - this.jobStarts[i];
                }
            }
            return age;
        }

        int segment(double t) {
            int at = -1;
            for (int i = 0; i < this.starts.length; i++) {
                if (t >= this.starts[i]) {
                    at = i;
                }
            }
            return at;
        }

        FurutaMove move(double t) {
            int i = segment(t);
            if (i < 0 || t - this.starts[i] > this.moves[i].ticks() + 2) {
                return FurutaMove.NONE;
            }
            return this.moves[i];
        }

        double age(double t) {
            int i = segment(t);
            return i < 0 ? 0.0 : t - this.starts[i];
        }

        FurutaRig.Input input(int limb, double t) {
            FurutaMove move = move(t);
            boolean moving = move != FurutaMove.NONE;
            double floor = this.scene.floor().applyAsDouble(t);
            double vertical = floor - this.scene.floor().applyAsDouble(t + 1.0);
            double[] dummy = this.scene.dummy().apply(t);
            double[] prey = dummy == null ? null : new double[] {dummy[0], dummy[1] + 1.0, dummy[2]};
            FurutaRig.Body body = new FurutaRig.Body(0.0, false, vertical, floor > -0.05, 0.0, this.scene.pitch().applyAsDouble(t), Simulated.CLOCK + t, prey);
            FurutaMove job = job(t);
            return new FurutaRig.Input(this.stage, this.n, limb, this.stage.length(this.mastery), body, move, age(t), moving ? this.scene.aim().apply(t) : null,
                moving && move.chases() && limb == 0 ? this.scene.target.apply(t) : null, moving ? this.scene.held().apply(t) : null, 0.35, job, jobAge(t),
                job == FurutaMove.NONE ? null : this.scene.jobHeld.apply(t));
        }

        void follow(double t) {
            for (int limb = 0; limb < this.n; limb++) {
                this.legs[limb].follow(FurutaRig.pose(input(limb, t)), Simulated.CLOCK + t);
            }
        }

        void run(double t) {
            if (this.time == -24.0) {
                follow(this.time);
            }
            while (this.time + Simulated.STEP <= t + 1.0E-9) {
                this.time += Simulated.STEP;
                follow(this.time);
            }
        }

        List<WingPreview.Tri> draw(RigPose body, double t) {
            List<WingPreview.Tri> out = new ArrayList<>();
            double width = this.stage.width(this.mastery);
            double[] aim = this.scene.aim().apply(t);
            for (int limb = 0; limb < this.n; limb++) {
                com.hex.kagune.control.furuta.FurutaMotion.Limb leg = this.legs[limb];
                double[][] points = com.hex.kagune.control.client.animation.FurutaPose.ride(body, leg.points());
                KakujaModel.Spine spine = CentipedeTail.through(points, 96, FurutaRig.side(this.stage, this.n, limb), leg.dorsal());
                double[] tip = points[points.length - 1];
                FurutaModel.Look look = new FurutaModel.Look(width, true, 1.0, limb * 7 + 3, leg.jaw(), leg.flare(), leg.stare(), FurutaRig.maw(this.stage, limb),
                    Simulated.CLOCK + t, aim == null ? null : FurutaModel.gazeToward(tip, aim), false);
                FurutaStage drawn = this.stage;
                out.addAll(ArtPreview.placed(sink -> FurutaModel.limb(sink, drawn, spine, look), q -> q));
            }
            return out;
        }

        /** The frame at {@code t}: his body posed by the move, the limbs, the ground, the dummy, the server's hits that tick. */
        ArtPreview.Shot shot(double t, String label, double[] eye, double[] look) {
            run(t);
            FurutaMove move = move(t);
            double age = age(t);
            RigPose body = new RigPose();
            com.hex.kagune.control.client.animation.FurutaPose.body(body, this.stage, this.n, move, age, job(t), jobAge(t), this.stage.kakuja() ? 1.0 : 0.0,
                age + 20.0);
            List<WingPreview.Tri> tris = draw(body, t);
            if (this.stage.kakuja()) {
                tris = with(kakujaBody(body, 1.0, 0.3, t), tris);
            }
            double floor = this.scene.floor().applyAsDouble(t);
            double[] dummy = this.scene.dummy().apply(t);
            double slide = this.scene.slide.applyAsDouble(t) % 2.0;
            tris.addAll(checker(dummy == null || this.scene.floorUnderHim ? new double[] {0.0, floor, 2.0 - slide} : new double[] {dummy[0], floor, dummy[2]},
                this.scene.turn().applyAsDouble(t)));
            if (dummy != null) {
                tris.addAll(EtoPreview.dummyAt(dummy));
            }
            double[] jobHeld = job(t) == FurutaMove.NONE ? null : this.scene.jobHeld.apply(t);
            if (jobHeld != null) {
                tris.addAll(EtoPreview.dummyAt(new double[] {jobHeld[0], jobHeld[1] - 1.0, jobHeld[2]}));
            }
            tris.addAll(this.scene.props);
            String landing = move == FurutaMove.NONE ? "" : hits(tris, this.stage, this.n, this.stage.length(this.mastery), move, age, this.scene.aim().apply(t),
                null);
            return new ArtPreview.Shot(label + (landing.isEmpty() ? "" : " " + landing), body, ArtPreview.NO_WINGS, eye, look, tris);
        }
    }

    /** A checkered patch of ground at {@code centre} (its y the ground's), turned {@code angle} about it: the world moving under him. */
    static List<WingPreview.Tri> checker(double[] centre, double angle) {
        List<WingPreview.Tri> out = new ArrayList<>();
        double[] up = {0.0, 1.0, 0.0};
        double c = Math.cos(angle);
        double sn = Math.sin(angle);
        int half = 6;
        for (int i = -half; i < half; i++) {
            for (int j = -half; j < half; j++) {
                int argb = ((i + j) & 1) == 0 ? 0xFF3A4048 : 0xFF2C3138;
                double[][] corners = new double[4][];
                double[][] local = {{i, j}, {i + 1, j}, {i + 1, j + 1}, {i, j + 1}};
                for (int k = 0; k < 4; k++) {
                    double x = local[k][0];
                    double z = local[k][1];
                    corners[k] = new double[] {centre[0] + x * c + z * sn, centre[1] - 0.01, centre[2] - x * sn + z * c};
                }
                out.add(new WingPreview.Tri(new double[][] {corners[0], corners[1], corners[2]}, new int[] {argb, argb, argb}, up, true));
                out.add(new WingPreview.Tri(new double[][] {corners[0], corners[2], corners[3]}, new int[] {argb, argb, argb}, up, true));
            }
        }
        return out;
    }

    /** The middle of a dummy whose feet are at {@code feet}. */
    static double[] middle(double[] feet) {
        return feet == null ? null : new double[] {feet[0], feet[1] + 1.0, feet[2]};
    }

    /** How far a push of {@code v} blocks a tick carries him over {@code ticks} on the ground (vanilla's friction, 0.546 a tick). */
    static double slid(double v, double ticks) {
        double out = 0.0;
        double speed = v;
        for (int i = 0; i < Math.floor(Math.max(0.0, ticks)); i++) {
            out += speed;
            speed *= 0.546;
        }
        return out + speed * (Math.max(0.0, ticks) - Math.floor(Math.max(0.0, ticks)));
    }

    /**
     * The two tails' four, frame by frame on the game's springs, each into what follows it: Mockingbird's stance and its counter
     * (the attacker walking in, the dodge carrying him aside), Owl's Execution (the rush, then the ground turning under him as he
     * circles), Scorpion Vault (the ground dropping away below the planted tails) and its dive, Human Puppet's catch and its three
     * follow-ups. Yellow: where the server hits that tick.
     */
    /** The counter alone, close and from straight above (where its hook and its flank go round the attacker). */
    static boolean TAILS_COUNTER;

    static void tails(File file) throws Exception {
        List<ArtPreview.Shot> shots = new ArrayList<>();
        double size = FurutaStage.TRANSPLANT.length(Chain.MASTERY);
        double[] side = {size * 1.25 + 4.5, 2.6 + 0.45 * size, 3.6};
        double[] sideLook = {0.0, 1.3, 2.2};
        // Mockingbird: the attacker walks in on the open stance and swings at 15; he slips aside to his right, hooked, run through.
        java.util.function.DoubleFunction<double[]> attacker = t -> {
            double z = t < 12.0 ? 4.0 - 0.15 * t : 2.2;
            double x = t < 15.0 ? 0.0 : -slid(com.hex.kagune.control.furuta.FurutaRules.COUNTER_DODGE, t - 15.0);
            return new double[] {x, 0.0, z};
        };
        Chain mock = new Chain(new Scene(t -> middle(attacker.apply(t)), t -> t > 18.0 && t <= 27.0 ? middle(attacker.apply(t)) : null, attacker, t -> 0.0,
            t -> 0.0, t -> 0.0), new FurutaMove[] {FurutaMove.MOCKINGBIRD, FurutaMove.MOCKINGBIRD_COUNTER}, new double[] {0.0, 15.0});
        String[] mockLabels = {"Mockingbird t3: open, the tails dropping", "t13: poised low, the stings at his hips", "struck at: counter t1, slipped aside",
            "counter t3: hooked", "counter t6: through it from behind", "counter t12: hold let go, the string's 4th blow next"};
        double[] mockAt = {3.0, 13.0, 16.0, 18.0, 21.0, 27.0};
        double[] mockEye = {5.2, 2.6, 4.6};
        double[] mockLook = {0.0, 1.0, 0.8};
        double[] counterEye = {-1.2, 7.4, 6.4};
        double[] counterLook = {-1.2, 0.8, 1.8};
        for (int i = 0; i < mockAt.length; i++) {
            if (TAILS_COUNTER) {
                shots.add(mock.shot(mockAt[i], mockLabels[i], new double[] {-1.0, 9.0, 0.9}, new double[] {-1.0, 0.8, 1.9}));
                continue;
            }
            shots.add(mock.shot(mockAt[i], mockLabels[i], i < 2 ? mockEye : counterEye, i < 2 ? mockLook : counterLook));
        }
        if (TAILS_COUNTER) {
            ArtPreview.sheet(file, "Mockingbird from above (he faces up the page): the hook wraps the attacker; the other tail goes out wide, round behind it, and through it toward him", shots, 3, 480, 420);
            return;
        }
        // Owl's Execution: rushed in from seven blocks to a stride off it, then round it (the ground turning under him), cut after cut.
        double[] rush = new double[8];
        rush[0] = 7.0;
        for (int i = 1; i < rush.length; i++) {
            rush[i] = rush[i - 1] - Math.min(com.hex.kagune.control.furuta.FurutaRules.EXECUTION_SPEED,
                Math.max(0.0, rush[i - 1] - com.hex.kagune.control.furuta.FurutaRules.EXECUTION_RADIUS) * 0.6);
        }
        java.util.function.DoubleFunction<double[]> prey = t -> {
            int i = (int)Math.max(0, Math.min(rush.length - 1, Math.floor(t)));
            double z = i + 1 < rush.length ? rush[i] + (rush[i + 1] - rush[i]) * (t - Math.floor(t)) : rush[rush.length - 1];
            return new double[] {0.0, 0.0, t > 6.0 ? com.hex.kagune.control.furuta.FurutaRules.EXECUTION_RADIUS : z};
        };
        Chain exec = new Chain(new Scene(t -> middle(prey.apply(t)), t -> null, prey, t -> 0.0, t -> 0.0,
            t -> -Math.toRadians(com.hex.kagune.control.furuta.FurutaRules.executionTurn(t))), new FurutaMove[] {FurutaMove.OWLS_EXECUTION}, new double[] {0.0});
        double[] execAt = {3.0, 6.0, 9.0, 12.0, 18.0, 27.0};
        String[] execLabels = {"Owl's Execution t3: rushing in", "t6: the first cut", "t9", "t12", "t18: still circling", "t27: both through it"};
        double[] execEye = {-4.2, 6.4, -2.8};
        double[] execLook = {0.0, 1.0, 2.0};
        for (int i = 0; i < execAt.length; i++) {
            shots.add(exec.shot(execAt[i], execLabels[i], execEye, execLook));
        }
        // Scorpion Vault: planted, thrown up off them (the ground falling away below), then the dive where he looks (55 degrees down).
        double diveAt = 14.0;
        double pitch = Math.toRadians(55.0);
        double[] dir = com.hex.kagune.control.furuta.FurutaRules.diveDirection(0.0, -Math.sin(pitch), Math.cos(pitch));
        double high = com.hex.kagune.control.furuta.FurutaRules.vaultRise(diveAt - com.hex.kagune.control.furuta.FurutaRules.VAULT_LAUNCH);
        double down = -dir[1] * com.hex.kagune.control.furuta.FurutaRules.DIVE_SPEED;
        double ahead = dir[2] * com.hex.kagune.control.furuta.FurutaRules.DIVE_SPEED;
        double landsIn = high / down;
        java.util.function.DoubleUnaryOperator floor = t -> t < diveAt ? -com.hex.kagune.control.furuta.FurutaRules.vaultRise(t - 2.0)
            : -Math.max(0.0, high - down * (t - diveAt));
        java.util.function.DoubleFunction<double[]> below = t -> new double[] {0.3, floor.applyAsDouble(t),
            t < diveAt ? landsIn * ahead + 1.0 : Math.max(0.6, (landsIn - (t - diveAt)) * ahead + 1.0)};
        Chain vault = new Chain(new Scene(t -> t < diveAt ? new double[] {0.0, 1.0, 4.0} : middle(below.apply(t)), t -> null, below, floor,
            t -> t < diveAt ? 0.0 : pitch, t -> 0.0), new FurutaMove[] {FurutaMove.SCORPION_VAULT, FurutaMove.VAULT_DIVE}, new double[] {0.0, diveAt});
        double[] vaultAt = {1.0, 2.0, 5.0, 10.0, diveAt + 2.0, diveAt + Math.ceil(landsIn)};
        String[] vaultLabels = {"Scorpion Vault t1: driven in behind", "t2: thrown", "t5: still in the ground below", "t10: torn free, in the air",
            "the key again: diving t2", "dive t" + (int)Math.ceil(landsIn) + ": the impact"};
        double[] vaultEye = {12.5, 1.2, 3.0};
        double[] vaultLook = {0.0, -0.9, 2.2};
        for (int i = 0; i < vaultAt.length; i++) {
            shots.add(vault.shot(vaultAt[i], vaultLabels[i], vaultEye, vaultLook));
        }
        // Human Puppet: caught at seven and a half blocks, dragged in and held out at the tail's length; then each follow-up.
        double[][] carried = new double[60][];
        double[] at = {0.0, 1.0, 7.5};
        double[] heldAt = com.hex.kagune.control.furuta.FurutaRules.heldPoint(FurutaMove.HUMAN_PUPPET, FurutaMove.Kind.SEIZE, 0.0);
        for (int i = 0; i < carried.length; i++) {
            if (i > 5) {
                double[] gap = {heldAt[0] - at[0], heldAt[1] - at[1], heldAt[2] - at[2]};
                double length = Math.sqrt(gap[0] * gap[0] + gap[1] * gap[1] + gap[2] * gap[2]);
                double k = length * 0.55 > 1.6 ? 1.6 / length : 0.55;
                at = new double[] {at[0] + gap[0] * k, at[1] + gap[1] * k, at[2] + gap[2] * k};
            }
            carried[i] = at.clone();
        }
        java.util.function.DoubleFunction<double[]> caught = t -> carried[(int)Math.max(0, Math.min(carried.length - 1, Math.floor(t)))];
        double from = 16.0;
        // The throw: hoisted high, then hurled along his look.
        java.util.function.DoubleFunction<double[]> thrown = t -> {
            if (t < from) {
                return caught.apply(t);
            }
            double age = t - from;
            if (age < 5.0) {
                return com.hex.kagune.control.furuta.FurutaRules.heldPoint(FurutaMove.PUPPET_THROW, FurutaMove.Kind.WHIP, age);
            }
            double[] p = com.hex.kagune.control.furuta.FurutaRules.heldPoint(FurutaMove.PUPPET_THROW, FurutaMove.Kind.WHIP, 5.0).clone();
            double vy = com.hex.kagune.control.furuta.FurutaRules.THROW_LIFT;
            for (int i = 0; i < Math.floor(age - 5.0); i++) {
                p[2] += com.hex.kagune.control.furuta.FurutaRules.THROW_SPEED;
                p[1] += vy;
                vy = (vy - 0.08) * 0.98;
            }
            return p;
        };
        java.util.function.DoubleFunction<double[]> thrownFeet = t -> {
            double[] m = thrown.apply(t);
            return new double[] {m[0], m[1] - 1.0, m[2]};
        };
        Chain hurl = new Chain(new Scene(t -> t < from ? (t <= 5.0 ? new double[] {0.0, 1.0, 7.5} : caught.apply(t)) : new double[] {0.0, 1.5, 9.0},
            t -> t > 5.0 && t < from + 5.0 ? thrown.apply(t) : null, thrownFeet, t -> 0.0, t -> 0.0, t -> 0.0),
            new FurutaMove[] {FurutaMove.HUMAN_PUPPET, FurutaMove.PUPPET_THROW}, new double[] {0.0, from});
        double[] hurlAt = {3.0, 6.0, 13.0, from + 2.0, from + 5.0, from + 8.0};
        String[] hurlLabels = {"Human Puppet t3: drawn back to shoot", "t6: caught, stunned", "t13: dragged in, held out", "the key again: throw t2, hoisted",
            "throw t5: hurled", "throw t8"};
        double[] puppetEye = {size * 1.25 + 6.0, 3.4 + 0.45 * size, 4.6};
        double[] puppetLook = {0.0, 1.6, 3.6};
        for (int i = 0; i < hurlAt.length; i++) {
            shots.add(hurl.shot(hurlAt[i], hurlLabels[i], puppetEye, puppetLook));
        }
        // M1: the other tail rips up through it and throws it up; right click: he hauls himself in, claws it, runs it through.
        java.util.function.DoubleFunction<double[]> ripped = t -> {
            if (t < from + 4.0) {
                return caught.apply(t);
            }
            double[] p = caught.apply(from).clone();
            double vy = 1.25;
            for (int i = 0; i < Math.floor(t - from - 4.0); i++) {
                p[1] += vy;
                p[2] += 0.15;
                vy = (vy - 0.08) * 0.98;
            }
            return p;
        };
        Chain strike = new Chain(new Scene(ripped, t -> t > 5.0 && t < from + 4.0 ? ripped.apply(t) : null, t -> {
            double[] m = ripped.apply(t);
            return new double[] {m[0], m[1] - 1.0, m[2]};
        }, t -> 0.0, t -> 0.0, t -> 0.0), new FurutaMove[] {FurutaMove.HUMAN_PUPPET, FurutaMove.PUPPET_STRIKE}, new double[] {0.0, from});
        double[] pulled = new double[24];
        double gap = caught.apply(from)[2];
        for (int i = 0; i < pulled.length; i++) {
            pulled[i] = gap;
            if (i <= 6) {
                double close = com.hex.kagune.control.furuta.FurutaRules.PULL_CLOSE + 0.3;
                gap -= Math.min(com.hex.kagune.control.furuta.FurutaRules.PULL_SPEED, Math.max(0.0, (gap - close) * 0.7));
            }
        }
        java.util.function.DoubleFunction<double[]> pinned = t -> {
            if (t < from) {
                return caught.apply(t);
            }
            double[] p = caught.apply(from).clone();
            p[2] = pulled[(int)Math.max(0, Math.min(pulled.length - 1, Math.floor(t - from)))];
            return p;
        };
        Chain pull = new Chain(new Scene(pinned, t -> t > 5.0 ? pinned.apply(t) : null, t -> {
            double[] m = pinned.apply(t);
            return new double[] {m[0], m[1] - 1.0, m[2]};
        }, t -> 0.0, t -> 0.0, t -> 0.0), new FurutaMove[] {FurutaMove.HUMAN_PUPPET, FurutaMove.PUPPET_PULL}, new double[] {0.0, from});
        shots.add(strike.shot(from + 2.0, "M1: strike t2, the other tail low under it", puppetEye, puppetLook));
        shots.add(strike.shot(from + 4.0, "strike t4: ripped up through it", puppetEye, puppetLook));
        shots.add(strike.shot(from + 8.0, "strike t8: thrown up", puppetEye, puppetLook));
        shots.add(pull.shot(from + 3.0, "RMB: pull t3, hauled in to it", puppetEye, puppetLook));
        shots.add(pull.shot(from + 6.0, "pull t6: his claws", puppetEye, puppetLook));
        shots.add(pull.shot(from + 13.0, "pull t13: a tail through it", puppetEye, puppetLook));
        ArtPreview.sheet(file, "Furuta DNA, the two tails' four (" + Chain.MASTERY + "), frame by frame into what follows each (the game's springs; yellow: where"
            + " the server hits that tick)", shots, 6, 300, 250);
    }

    /**
     * 7.0.2, the final kakuja's four, frame by frame on the game's springs: King's Checkmate (at its front, hooked from behind, his
     * charge and combination), Centipede Hunt (after it, biting; caught, dragged, thrown), Monstrous Recovery (the coils round him)
     * with the start of the performance, The Final Performance (the colossi from every side, his claws, the finish).
     */
    static void kakujaFour(File file) throws Exception {
        List<ArtPreview.Shot> shots = new ArrayList<>();
        FurutaStage stage = FurutaStage.KAKUJA;
        int mastery = 1000;
        double size = stage.length(mastery);
        double[] wide = {size + 9.0, 7.0, 12.0};
        double[] wideLook = {0.0, 3.0, 3.5};
        // King's Checkmate: its prey six blocks off; hooked at 10; his charge brings him in to 1.8.
        java.util.function.DoubleFunction<double[]> mate = t -> {
            double z = t < com.hex.kagune.control.furuta.FurutaRules.CHECKMATE_CHARGE_FROM ? 6.0 : Math.max(2.1, 6.0 - 1.2 * (t
                - com.hex.kagune.control.furuta.FurutaRules.CHECKMATE_CHARGE_FROM));
            return new double[] {0.0, 1.0, z};
        };
        Chain checkmate = new Chain(stage, mastery, new Scene(mate, t -> t > 10.0 && t <= 34.0 ? mate.apply(t) : null, t -> {
            double[] m = mate.apply(t);
            return new double[] {m[0], 0.0, m[2]};
        }, t -> 0.0, t -> 0.0, t -> 0.0), new FurutaMove[] {FurutaMove.CHECKMATE}, new double[] {0.0});
        double[] mateAt = {5.0, 10.0, 14.0, 16.0, 25.0, 33.0};
        String[] mateLabels = {"King's Checkmate t5: at its front", "t10: hooked from behind", "t14: his charge in", "t16: his claws", "t25: his jaws",
            "t33: the last blow"};
        for (int i = 0; i < mateAt.length; i++) {
            shots.add(checkmate.shot(mateAt[i], mateLabels[i], wide, wideLook));
        }
        // Centipede Hunt: after it from twelve blocks (the ground sliding back), caught at 2.8, dragged as he runs on, thrown.
        double[] chase = new double[60];
        double gap = 12.0;
        int caught = -1;
        for (int i = 0; i < chase.length; i++) {
            chase[i] = gap;
            if (caught < 0 && i >= 3 && gap <= com.hex.kagune.control.furuta.FurutaRules.HUNT_CATCH + 0.3) {
                caught = i;
            }
            if (caught < 0 && i >= 1) {
                gap -= Math.min(com.hex.kagune.control.furuta.FurutaRules.HUNT_SPEED, 0.4 + Math.max(0.0, gap - 2.0) * 0.7) - 0.15;
            }
        }
        final int at = caught;
        java.util.function.DoubleFunction<double[]> quarry = t -> new double[] {0.5, 1.0, chase[(int)Math.max(0, Math.min(chase.length - 1, Math.floor(t)))]};
        java.util.function.DoubleFunction<double[]> dragged = t -> {
            double d = t - at;
            if (d < com.hex.kagune.control.furuta.FurutaRules.HUNT_THROW) {
                double m = Math.min(1.0, d / 3.0);
                double[] from = quarry.apply(at);
                return new double[] {from[0] + (2.4 - from[0]) * m, from[1] + (0.45 - from[1]) * m, from[2] + (0.8 - from[2]) * m};
            }
            double f = d - com.hex.kagune.control.furuta.FurutaRules.HUNT_THROW;
            return new double[] {2.4, 0.45 + 0.5 * f, 0.8 + 1.3 * f};
        };
        Scene huntScene = new Scene(t -> t < at ? quarry.apply(t) : dragged.apply(t), t -> t >= at ? dragged.apply(t) : null, t -> {
            double[] m = t < at ? quarry.apply(t) : dragged.apply(t);
            return new double[] {m[0], m[1] - 1.0, m[2]};
        }, t -> 0.0, t -> 0.0, t -> 0.0);
        huntScene.floorUnderHim = true;
        huntScene.slide = t -> 1.3 * t;
        Chain hunt = new Chain(stage, mastery, huntScene, new FurutaMove[] {FurutaMove.HUNT, FurutaMove.HUNT_DRAG}, new double[] {0.0, at});
        double[] huntAt = {2.0, Math.max(3.0, at - 1.0), at + 2.0, at + 8.0, at + 16.0, at + 21.0};
        String[] huntLabels = {"Centipede Hunt t2: hurled after it", "t" + (at - 1) + ": closing, biting", "caught: dragged t2", "drag t8: across the ground",
            "drag t16", "thrown"};
        for (int i = 0; i < huntAt.length; i++) {
            shots.add(hunt.shot(huntAt[i], huntLabels[i], wide, wideLook));
        }
        // Monstrous Recovery: the coils round him (he could walk on); the performance's first blows.
        Chain recovery = new Chain(stage, mastery, new Scene(t -> null, t -> null, t -> null, t -> 0.0, t -> 0.0, t -> 0.0), new FurutaMove[0], new double[0])
            .jobs(new FurutaMove[] {FurutaMove.RECOVERY}, new double[] {0.0});
        double[] near = {size * 0.6 + 6.0, 4.0, 7.0};
        double[] nearLook = {0.0, 2.0, 0.5};
        shots.add(recovery.shot(3.0, "Monstrous Recovery t3: curling round him", near, nearLook));
        shots.add(recovery.shot(40.0, "t40: knitting (half of every blow taken)", near, nearLook));
        java.util.function.DoubleFunction<double[]> foe = t -> new double[] {2.0, 1.0, 7.0};
        Scene perfScene = new Scene(foe, t -> null, t -> new double[] {2.0, 0.0, 7.0}, t -> 0.0, t -> 0.0, t -> 0.0);
        perfScene.jobHeld = foe;
        Chain perform = new Chain(stage, mastery, perfScene, new FurutaMove[] {FurutaMove.PERF_CLAW, FurutaMove.PERF_FINISH}, new double[] {27.0, 100.0})
            .jobs(new FurutaMove[] {FurutaMove.PERFORMANCE}, new double[] {0.0});
        shots.add(perform.shot(6.0, "The Final Performance t6: straight in", wide, wideLook));
        shots.add(perform.shot(12.0, "t12: round behind it", wide, wideLook));
        shots.add(perform.shot(18.0, "t18: swept across", wide, wideLook));
        shots.add(perform.shot(24.0, "t24: up from under it", wide, wideLook));
        // The finish: caught, hauled in, the other colossus through it, his last blow.
        java.util.function.DoubleFunction<double[]> hauled = t -> {
            double d = t - 100.0;
            if (d <= 5.0) {
                return new double[] {2.0, 1.0, 7.0};
            }
            double m = Math.min(1.0, (d - 5.0) / 4.0);
            return new double[] {2.0 + (0.6 - 2.0) * m, 1.0 + 0.4 * m, 7.0 + (2.0 - 7.0) * m};
        };
        perfScene.held = t -> t > 105.0 && t <= 119.0 ? hauled.apply(t) : null;
        perfScene.dummy = t -> {
            double[] m = t < 100.0 ? foe.apply(t) : hauled.apply(t);
            return new double[] {m[0], m[1] - 1.0, m[2]};
        };
        perfScene.aim = t -> t < 100.0 ? foe.apply(t) : hauled.apply(t);
        shots.add(perform.shot(30.0, "M1: his claws (t3)", wide, wideLook));
        shots.add(perform.shot(106.0, "the finish t6: caught", wide, wideLook));
        shots.add(perform.shot(114.0, "t14: hauled in, the other colossus through it", wide, wideLook));
        shots.add(perform.shot(119.0, "t19: his last blow", wide, wideLook));
        ArtPreview.sheet(file, "Furuta DNA, the final kakuja's four (" + mastery + "), frame by frame (the game's springs; yellow: where the server hits that tick)",
            shots, 6, 300, 250);
    }
    /**
     * 7.0.2, the mass of eyes' four, frame by frame on the game's springs (the crescents' look untouched): Eye of the Storm (every
     * crescent wheeling on its own path), Flesh Bloom (drawn in, flung out, the waves of spikes, drawn in), Black Curtain (the wall,
     * its fall, its ends curling round), The King's Game (at its prey from every side while he stands off; his sword's rush in).
     */
    static void eyes(File file) throws Exception {
        List<ArtPreview.Shot> shots = new ArrayList<>();
        FurutaStage stage = FurutaStage.DRAGON;
        int mastery = 850;
        double size = stage.length(mastery);
        // Eye of the Storm, from above and behind: dummies round him.
        Scene stormScene = new Scene(t -> new double[] {0.0, 1.3, 6.0}, t -> null, t -> null, t -> 0.0, t -> 0.0, t -> 0.0);
        stormScene.floorUnderHim = true;
        for (double[] at : new double[][] {{4.5, 0.0, 2.5}, {-4.0, 0.0, 3.5}, {-3.0, 0.0, -4.0}, {3.5, 0.0, -3.5}}) {
            stormScene.props.addAll(EtoPreview.dummyAt(at));
        }
        Chain storm = new Chain(stage, mastery, stormScene, new FurutaMove[] {FurutaMove.STORM}, new double[] {0.0});
        double[] stormEye = {0.0, 15.0, -9.0};
        double[] stormLook = {0.0, 1.0, 0.5};
        for (double t : new double[] {3.0, 10.0, 20.0, 30.0, 42.0, 54.0}) {
            shots.add(storm.shot(t, (t == 3.0 ? "Eye of the Storm " : "") + "t" + (int)t + (t == 3.0 ? ": growing out" : t == 10.0 ? ": wheeling" : ""), stormEye,
                stormLook));
        }
        // Flesh Bloom.
        Scene bloomScene = new Scene(t -> new double[] {0.0, 1.3, 6.0}, t -> null, t -> null, t -> 0.0, t -> 0.0, t -> 0.0);
        bloomScene.floorUnderHim = true;
        for (double[] at : new double[][] {{3.5, 0.0, 4.0}, {-5.0, 0.0, 1.0}}) {
            bloomScene.props.addAll(EtoPreview.dummyAt(at));
        }
        Chain bloom = new Chain(stage, mastery, bloomScene, new FurutaMove[] {FurutaMove.BLOOM}, new double[] {0.0});
        double[] bloomEye = {size + 7.0, 6.5, 9.0};
        double[] bloomLook = {0.0, 1.5, 0.5};
        double[] bloomAt = {4.0, 9.0, 14.0, 18.0, 22.0, 41.0};
        String[] bloomLabels = {"Flesh Bloom t4: drawn in", "t9: flung out", "t14: the first wave of spikes", "t18: easing", "t22: the second wave",
            "t41: drawn in again"};
        for (int i = 0; i < bloomAt.length; i++) {
            shots.add(bloom.shot(bloomAt[i], bloomLabels[i], bloomEye, bloomLook));
        }
        // Black Curtain: two dummies in front of him.
        Scene curtainScene = new Scene(t -> new double[] {0.0, 1.3, 6.0}, t -> null, t -> null, t -> 0.0, t -> 0.0, t -> 0.0);
        curtainScene.floorUnderHim = true;
        for (double[] at : new double[][] {{1.5, 0.0, 4.0}, {-2.0, 0.0, 5.5}}) {
            curtainScene.props.addAll(EtoPreview.dummyAt(at));
        }
        Chain curtain = new Chain(stage, mastery, curtainScene, new FurutaMove[] {FurutaMove.CURTAIN}, new double[] {0.0});
        double[] curtainEye = {12.0, 6.0, 9.0};
        double[] curtainLook = {0.0, 2.0, 3.0};
        double[] curtainAt = {5.0, 9.0, 12.0, 14.0, 20.0, 34.0};
        String[] curtainLabels = {"Black Curtain t5: rising", "t9: a wall before him", "t12: falling", "t14: down on them", "t20: its ends curled round",
            "t34: lifting"};
        for (int i = 0; i < curtainAt.length; i++) {
            shots.add(curtain.shot(curtainAt[i], curtainLabels[i], curtainEye, curtainLook));
        }
        // The King's Game: its prey ten blocks off; the crescents at it in turn; his sword's rush in (it lands at 3.5 blocks).
        java.util.function.DoubleFunction<double[]> prey = t -> t < 40.0 ? new double[] {3.0, 1.0, 10.0} : new double[] {1.0, 1.0, 3.2};
        Scene gameScene = new Scene(prey, t -> null, t -> {
            double[] m = prey.apply(t);
            return new double[] {m[0], 0.0, m[2]};
        }, t -> 0.0, t -> 0.0, t -> 0.0);
        gameScene.jobHeld = prey;
        Chain game = new Chain(stage, mastery, gameScene, new FurutaMove[] {FurutaMove.KINGS_STRIKE}, new double[] {40.0})
            .jobs(new FurutaMove[] {FurutaMove.KINGS_GAME}, new double[] {0.0});
        double[] gameEye = {size + 6.0, 7.0, 13.0};
        double[] gameLook = {1.0, 1.5, 5.0};
        double[] gameAt = {6.0, 13.0, 20.0, 27.0, 43.0, 46.0};
        String[] gameLabels = {"The King's Game t6: straight in", "t13: round behind it", "t20: up from under it", "t27: down on it",
            "a sword's M1: rushed in, his blow t3", "t6: the crescent with it"};
        for (int i = 0; i < gameAt.length; i++) {
            shots.add(game.shot(gameAt[i], gameLabels[i], gameEye, gameLook));
        }
        ArtPreview.sheet(file, "Furuta DNA, the mass of eyes' four (" + mastery + "), frame by frame (the game's springs; yellow: where the server hits that tick)",
            shots, 6, 300, 250);
    }
    /**
     * 7.0.2, the four thorns' four, frame by frame on the game's springs: Crucifixion (caught from either side and held up, run
     * through again and again, hurled), Unstoppable Breach (thrown on, the ground sliding back, a wall in the way smashed, a body
     * run through), Dragon's Dragnet (three bodies swept in to the middle, the slam), Living Catapult (planted, flung, the flight,
     * the landing). Yellow: where the server hits that tick.
     */
    static void thorns(File file) throws Exception {
        List<ArtPreview.Shot> shots = new ArrayList<>();
        FurutaStage stage = FurutaStage.THORNS;
        int mastery = 600;
        double size = stage.length(mastery);
        double[] side = {size * 1.25 + 6.0, 3.6 + 0.45 * size, 5.0};
        double[] sideLook = {0.0, 1.8, 3.2};
        // Crucifixion: caught five blocks off, lifted and held up, run through, hurled.
        double[] start = {0.0, 1.0, 5.0};
        java.util.function.DoubleFunction<double[]> lifted = t -> {
            if (t <= 6.0) {
                return start;
            }
            if (t < com.hex.kagune.control.furuta.FurutaRules.CRUCIFY_THROW) {
                double[] h = com.hex.kagune.control.furuta.FurutaRules.heldPoint(FurutaMove.CRUCIFIXION, FurutaMove.Kind.SEIZE, t - 6.0);
                double m = Math.min(1.0, (t - 6.0) / 4.0);
                return new double[] {start[0] + (h[0] - start[0]) * m, start[1] + (h[1] - start[1]) * m, start[2] + (h[2] - start[2]) * m};
            }
            double flown = t - com.hex.kagune.control.furuta.FurutaRules.CRUCIFY_THROW;
            return new double[] {0.0, 3.4 + 0.5 * flown, 3.0 + com.hex.kagune.control.furuta.FurutaRules.CRUCIFY_SPEED * flown};
        };
        Chain cross = new Chain(stage, mastery, new Scene(t -> lifted.apply(Math.min(t, 36.0)), t -> t > 6.0 && t < 37.0 ? lifted.apply(t) : null, t -> {
            double[] m = lifted.apply(t);
            return new double[] {m[0], m[1] - 1.0, m[2]};
        }, t -> 0.0, t -> 0.0, t -> 0.0), new FurutaMove[] {FurutaMove.CRUCIFIXION}, new double[] {0.0});
        double[] crossEye = {size * 0.8 + 5.0, 3.0 + 0.4 * size, 9.5};
        double[] crossLook = {0.0, 2.2, 2.8};
        double[] crossAt = {4.0, 8.0, 13.0, 22.0, 31.0, 39.0};
        String[] crossLabels = {"Crucifixion t4: the upper pair out", "t8: caught from either side, lifting", "t13: held up, run through", "t22",
            "t31: the last thrusts", "t39: hurled"};
        for (int i = 0; i < crossAt.length; i++) {
            shots.add(cross.shot(crossAt[i], crossLabels[i], crossEye, crossLook));
        }
        // Unstoppable Breach: thrown on (the ground sliding back), a body in his path, a wall beyond it smashed.
        java.util.function.DoubleUnaryOperator gone = t -> com.hex.kagune.control.furuta.FurutaRules.BREACH_SPEED * Math.max(0.0, Math.min(t,
            com.hex.kagune.control.furuta.FurutaRules.BREACH_TICKS) - 1.0);
        Scene breachScene = new Scene(t -> new double[] {0.0, 1.3, 5.0}, t -> null, t -> new double[] {0.6, 0.0, 9.0 - gone.applyAsDouble(t)}, t -> 0.0, t -> 0.0,
            t -> 0.0);
        breachScene.slide = gone;
        breachScene.floorUnderHim = true;
        Chain breach = new Chain(stage, mastery, breachScene, new FurutaMove[] {FurutaMove.BREACH}, new double[] {0.0});
        double[] breachEye = {-size * 0.9 - 4.0, 3.4 + 0.3 * size, 7.5};
        double[] breachLook = {0.0, 1.2, 3.0};
        double[] breachAt = {2.0, 4.0, 7.0, 10.0, 13.0, 19.0};
        for (double t : breachAt) {
            shots.add(breach.shot(t, (t == breachAt[0] ? "Unstoppable Breach " : "") + "t" + (int)t + (t == 4.0 ? ": smashing ahead" : t == 7.0
                ? ": the next thorn" : t == 19.0 ? ": the last thrust" : ""), breachEye, breachLook));
        }
        // Dragon's Dragnet: three bodies out to the sides, swept in to the middle, the slam.
        double point = com.hex.kagune.control.furuta.FurutaRules.DRAGNET_POINT;
        double[][] bodies = {{6.5, 0.0, 2.5}, {-6.0, 0.0, 3.5}, {-3.5, 0.0, 6.5}};
        Scene netScene = new Scene(t -> new double[] {0.0, 1.0, point}, t -> null, t -> null, t -> 0.0, t -> 0.0, t -> 0.0);
        java.util.function.DoubleFunction<List<WingPreview.Tri>> crowd = t -> {
            List<WingPreview.Tri> out = new ArrayList<>();
            for (double[] b : bodies) {
                double bearing = Math.toDegrees(Math.atan2(b[0], b[2]));
                // The sweep's angle at t (from its side, eased as the server sweeps it); caught once it has passed, then dragged in.
                double sweep = 115.0 - 110.0 * smooth((t - 7.0) / 6.0);
                boolean passed = t >= 7.0 && Math.abs(bearing) >= sweep - 12.0;
                double pull = passed ? Math.min(1.0, (t - 7.0 - (115.0 - Math.abs(bearing)) / 110.0 * 6.0) / 6.0) : 0.0;
                pull = Math.max(0.0, pull);
                out.addAll(EtoPreview.dummyAt(new double[] {b[0] + (0.0 - b[0]) * pull * 0.85, 0.0, b[2] + (point - b[2]) * pull * 0.85}));
            }
            return out;
        };
        Chain net = new Chain(stage, mastery, netScene, new FurutaMove[] {FurutaMove.DRAGNET}, new double[] {0.0});
        double[] netEye = {0.0, 11.0, -6.0};
        double[] netLook = {0.0, 0.5, 3.0};
        double[] netAt = {4.0, 8.0, 11.0, 15.0, 18.0, 23.0};
        String[] netLabels = {"Dragon's Dragnet t4: out far to either side", "t8: sweeping in, dragging", "t11", "t15: gathered; the upper pair over it",
            "t18: the slam", "t23"};
        for (int i = 0; i < netAt.length; i++) {
            ArtPreview.Shot shot = net.shot(netAt[i], netLabels[i], netEye, netLook);
            shot.extra().addAll(crowd.apply(netAt[i]));
            shots.add(shot);
        }
        // Living Catapult: planted, flung (the ground falling away), the flight, the landing.
        double launch = com.hex.kagune.control.furuta.FurutaRules.CATAPULT_LAUNCH;
        double peakAt = 0.0;
        double peak = 0.0;
        for (double t = 0.0; t < 40.0; t += 0.25) {
            double r = com.hex.kagune.control.furuta.FurutaRules.catapultRise(t);
            if (r > peak) {
                peak = r;
                peakAt = t;
            }
        }
        double down = peakAt;
        while (com.hex.kagune.control.furuta.FurutaRules.catapultRise(down) > 0.05 && down < 80.0) {
            down += 0.25;
        }
        double lands = launch + Math.ceil(down);
        Chain cat = new Chain(stage, mastery, new Scene(t -> new double[] {0.0, 1.0, 4.0}, t -> null, t -> null,
            t -> t >= lands ? 0.0 : -com.hex.kagune.control.furuta.FurutaRules.catapultRise(t - launch), t -> 0.0, t -> 0.0),
            new FurutaMove[] {FurutaMove.CATAPULT, FurutaMove.CATAPULT_LAND}, new double[] {0.0, lands})
            .jobs(new FurutaMove[] {FurutaMove.CATAPULT_FLIGHT}, new double[] {launch});
        double[] catEye = {size * 1.2 + 9.0, 2.0, 5.0};
        double[] catLook = {0.0, -1.2, 1.0};
        double[] catAt = {2.0, launch + 3.0, launch + 7.0, launch + Math.round(peakAt), lands - 3.0, lands + 4.0};
        String[] catLabels = {"Living Catapult t2: all four in", "flung: the four still in the ground", "torn free", "the flight, splayed (" + (int)Math.round(peak)
            + " blocks up)", "coming down, reaching for the ground", "landed: the four into the ground in turn"};
        for (int i = 0; i < catAt.length; i++) {
            shots.add(cat.shot(catAt[i], catLabels[i], catEye, catLook));
        }
        ArtPreview.sheet(file, "Furuta DNA, the four thorns' four (" + mastery + "), frame by frame into what follows each (the game's springs; yellow: where the"
            + " server hits that tick)", shots, 6, 300, 250);
    }
    /**
     * 7.0.2, the centipedes' four, frame by frame on the game's springs: Hungry Maw (the mouth up and over a wall after its prey,
     * snapping, clamping it, then dragging it at his side while a centipede strikes for the string), Centipede Locomotion (the
     * strides, the ground sliding back under him, a strike on the way, the bite at the end), The Ambush (the feint, the mouth from
     * under), Living Carapace (coiled, struck at, sprung, the bite). Yellow: where the server hits that tick.
     */
    static void centipedes(File file) throws Exception {
        List<ArtPreview.Shot> shots = new ArrayList<>();
        FurutaStage stage = FurutaStage.CENTIPEDE;
        int mastery = 400;
        double size = stage.length(mastery);
        double k = size / 5.0;
        double[] side = {size * 1.25 + 5.5, 3.2 + 0.45 * size, 4.4};
        double[] sideLook = {0.0, 1.6, 3.0};
        // Hungry Maw: prey nine blocks off behind a wall; the mouth's route bends up over it (the server's first open bend).
        double[] prey = {1.0, 1.0, 9.0};
        double[] rear = {0.0, 0.95 + 2.4 * k, -0.17 - 0.4 * k};
        double[] bend = {0.5 * (rear[0] + prey[0]), 0.5 * (rear[1] + prey[1]) + 2.5 + 0.2 * 9.5, 0.5 * (rear[2] + prey[2])};
        double route = distance(rear, bend) + distance(bend, prey);
        double back = 2.6 * k;
        int caught = 0;
        while (com.hex.kagune.control.furuta.FurutaRules.mawReach(caught, size) - back < route - 0.8 && caught < 30) {
            caught++;
        }
        final int at = caught;
        double[] drag = com.hex.kagune.control.furuta.FurutaRules.DRAG_POINT;
        double[] carried = new double[] {prey[0], prey[1], prey[2]};
        double[][] path = new double[40][];
        for (int i = 0; i < path.length; i++) {
            if (i > 0) {
                double[] gap = {drag[0] - carried[0], drag[1] - carried[1], drag[2] - carried[2]};
                double length = Math.sqrt(gap[0] * gap[0] + gap[1] * gap[1] + gap[2] * gap[2]);
                double m = length * 0.55 > 1.6 ? 1.6 / length : 0.55;
                carried = new double[] {carried[0] + gap[0] * m, carried[1] + gap[1] * m, carried[2] + gap[2] * m};
            }
            path[i] = carried.clone();
        }
        java.util.function.DoubleFunction<double[]> dragged = t -> path[(int)Math.max(0, Math.min(path.length - 1, Math.floor(t - at)))];
        Scene mawScene = new Scene(t -> bend, t -> null, t -> t < at ? new double[] {prey[0], 0.0, prey[2]} : null, t -> 0.0, t -> 0.0, t -> 0.0);
        mawScene.target = t -> prey;
        mawScene.jobHeld = dragged;
        EtoPreview.box(mawScene.props, new double[] {-1.5, 0.0, 4.6}, new double[] {2.5, 2.6, 5.2}, 0xFF6B5A4A);
        Chain maw = new Chain(stage, mastery, mawScene, new FurutaMove[] {FurutaMove.HUNGRY_MAW, FurutaMove.RIP_1}, new double[] {0.0, at + 12.0})
            .jobs(new FurutaMove[] {FurutaMove.MAW_DRAG}, new double[] {at});
        double[] mawEye = {size * 1.25 + 7.5, 4.5 + 0.45 * size, 5.5};
        double[] mawLook = {0.0, 2.2, 4.2};
        shots.add(maw.shot(3.0, "Hungry Maw t3: reared, going out", mawEye, mawLook));
        shots.add(maw.shot(Math.floor(at * 0.5), "t" + (int)Math.floor(at * 0.5) + ": up over the wall, snapping", mawEye, mawLook));
        shots.add(maw.shot(at - 1.0, "t" + (at - 1) + ": down onto it", mawEye, mawLook));
        shots.add(maw.shot(at + 2.0, "caught: the drag t2, clamped", side, sideLook));
        shots.add(maw.shot(at + 10.0, "drag t10: hauled to his side", side, sideLook));
        shots.add(maw.shot(at + 15.0, "drag t15: a centipede strikes (M1)", side, sideLook));
        // Centipede Locomotion: the strides (the ground sliding back under him), a strike on the way, the bite at its end.
        List<FurutaMove.Beat> strides = FurutaMove.LOCOMOTION.beats(stage.limbs(mastery));
        java.util.function.DoubleUnaryOperator hauled = t -> {
            double out = 0.0;
            for (int i = 0; i < strides.size(); i++) {
                double land = strides.get(i).land();
                double next = i + 1 < strides.size() ? strides.get(i + 1).land() : 60.0;
                if (t > land) {
                    out += com.hex.kagune.control.furuta.FurutaRules.crawled(Math.min(t, next) - land);
                }
            }
            return out;
        };
        double crawlEnd = FurutaMove.LOCOMOTION.ticks();
        java.util.function.DoubleFunction<double[]> ahead = t -> new double[] {0.3, 0.0, 14.5 - hauled.applyAsDouble(Math.min(t, crawlEnd + 3.0))};
        Scene crawlScene = new Scene(t -> {
            double[] d = ahead.apply(t);
            return new double[] {d[0], 1.0, d[2]};
        }, t -> null, ahead, t -> 0.0, t -> 0.0, t -> 0.0);
        crawlScene.slide = hauled;
        crawlScene.floorUnderHim = true;
        Chain crawl = new Chain(stage, mastery, crawlScene, new FurutaMove[] {FurutaMove.RIP_2, FurutaMove.CRAWL_BITE}, new double[] {13.0, crawlEnd})
            .jobs(new FurutaMove[] {FurutaMove.LOCOMOTION}, new double[] {0.0});
        // 7.0.2 side on, so the strides' reach ahead reads (from the front it foreshortens into him).
        double[] crawlEye = {size * 1.2 + 8.0, 2.8 + 0.3 * size, 2.2};
        double[] crawlLook = {0.0, 0.9, 2.0};
        shots.add(crawl.shot(1.0, "Locomotion t1: reaching ahead", crawlEye, crawlLook));
        shots.add(crawl.shot(3.0, "t3: planted, hauling", crawlEye, crawlLook));
        shots.add(crawl.shot(11.0, "t11: the other side's stride", crawlEye, crawlLook));
        shots.add(crawl.shot(16.0, "t16: a strike on the way (M1)", crawlEye, crawlLook));
        shots.add(crawl.shot(crawlEnd + 3.0, "the crawl's end: bite t3, lunging", crawlEye, crawlLook));
        shots.add(crawl.shot(crawlEnd + 5.0, "bite t5: snapped shut", crawlEye, crawlLook));
        // The Ambush.
        double[] front = {0.0, 1.0, 4.2};
        Chain ambush = new Chain(stage, mastery, new Scene(t -> front, t -> null, t -> new double[] {front[0], 0.0, front[2]}, t -> 0.0, t -> 0.0, t -> 0.0),
            new FurutaMove[] {FurutaMove.AMBUSH}, new double[] {0.0});
        double[] ambushEye = {size * 1.25 + 4.5, 2.2 + 0.3 * size, 5.2};
        double[] ambushLook = {0.0, 1.2, 3.0};
        double[] ambushAt = {2.0, 4.0, 6.0, 8.0, 10.0, 13.0};
        String[] ambushLabels = {"The Ambush t2: centipedes at its front", "t4: the feint lands", "t6: the mouth low, round under it", "t8: under it, gaping",
            "t10: snapped up through its guard", "t13"};
        for (int i = 0; i < ambushAt.length; i++) {
            shots.add(ambush.shot(ambushAt[i], ambushLabels[i], ambushEye, ambushLook));
        }
        // Living Carapace: coiled; the attacker steps in and swings at 20; sprung; the mouth bites (M1).
        java.util.function.DoubleFunction<double[]> attacker = t -> new double[] {0.2, 0.0, t < 16.0 ? 4.5 - 0.15 * t : t < 22.0 ? 2.1
            : 2.1 + Math.min(2.4, 0.8 * (t - 22.0))};
        Chain carapace = new Chain(stage, mastery, new Scene(t -> {
            double[] a = attacker.apply(t);
            return new double[] {a[0], 1.0, a[2]};
        }, t -> null, attacker, t -> 0.0, t -> 0.0, t -> 0.0), new FurutaMove[] {FurutaMove.CARAPACE, FurutaMove.CARAPACE_SPRING, FurutaMove.CARAPACE_BITE},
            new double[] {0.0, 20.0, 24.0});
        double[] coilEye = {size * 0.9 + 3.5, 3.0 + 0.3 * size, 4.4};
        double[] coilLook = {0.0, 1.2, 1.2};
        shots.add(carapace.shot(4.0, "Living Carapace t4: coiled round him", coilEye, coilLook));
        shots.add(carapace.shot(18.0, "t18: the mouth free, watching", coilEye, coilLook));
        shots.add(carapace.shot(21.0, "struck at: spring t1", coilEye, coilLook));
        shots.add(carapace.shot(22.0, "spring t2: out at it, stunned", coilEye, coilLook));
        shots.add(carapace.shot(28.0, "M1: bite t4", coilEye, coilLook));
        shots.add(carapace.shot(31.0, "bite t7", coilEye, coilLook));
        ArtPreview.sheet(file, "Furuta DNA, the centipedes' four (" + mastery + "), frame by frame into what follows each (the game's springs; yellow: where"
            + " the server hits that tick)", shots, 6, 300, 250);
    }

    static double distance(double[] a, double[] b) {
        return Math.sqrt((a[0] - b[0]) * (a[0] - b[0]) + (a[1] - b[1]) * (a[1] - b[1]) + (a[2] - b[2]) * (a[2] - b[2]));
    }
    /**
     * The mouths hunting at rest (on the game's springs): the centipede's and the kakuja's colossi, with prey near (a dummy at its
     * place: the head on it, rearing, lunging, snapping or feinting) through one of its strikes, and with none (jerking to new
     * bearings and freezing on each).
     */
    static void hunt(File file) throws Exception {
        List<ArtPreview.Shot> shots = new ArrayList<>();
        Object[][] cases = {{"Centipede mouth", FurutaStage.CENTIPEDE, 350, new double[] {1.8, 1.0, 4.6}, new double[] {8.0, 4.5, 6.0}},
            {"Kakuja colossi", FurutaStage.KAKUJA, 1000, new double[] {2.0, 1.0, 6.5}, new double[] {13.0, 6.5, 9.0}}};
        for (Object[] c : cases) {
            FurutaStage stage = (FurutaStage)c[1];
            int mastery = (Integer)c[2];
            double[] prey = (double[])c[3];
            double[] eye = (double[])c[4];
            double[] look = {prey[0] * 0.4, 2.2 + stage.length(mastery) * 0.15, prey[2] * 0.45};
            RigPose body = posed(stage, mastery, FurutaMove.NONE, 0.0);
            for (int pass = 0; pass < 2; pass++) {
                Simulated sim = new Simulated(stage, mastery, FurutaMove.NONE, null, null);
                sim.prey = pass == 0 ? prey : null;
                // With prey: its next strike (the colossi's first), tracking before it, rearing, lunging, recoiling.
                double strike = FurutaRig.nextStrike(stage, 0, Simulated.CLOCK + 12.0) - Simulated.CLOCK;
                double[] ages = pass == 0 ? new double[] {strike - 6.0, strike + 3.5, strike + 6.8, strike + 8.6, strike + 11.0, strike + 16.0}
                    : new double[] {4.0, 10.0, 16.0, 22.0, 28.0, 34.0};
                for (double age : ages) {
                    sim.run(age);
                    List<WingPreview.Tri> tris = sim.draw(body, age);
                    if (pass == 0) {
                        tris.addAll(EtoPreview.dummyAt(new double[] {prey[0], 0.0, prey[2]}));
                    }
                    if (stage == FurutaStage.KAKUJA) {
                        tris = with(kakujaBody(body, 1.0, 0.3, age), tris);
                    }
                    shots.add(new ArtPreview.Shot(c[0] + (pass == 0 ? ", prey near" : ", hunting") + " t" + trim(age), body, ArtPreview.NO_WINGS, eye, look,
                        tris));
                }
            }
        }
        ArtPreview.sheet(file, "Furuta DNA: the mouths hunting at rest (the game's springs)", shots, 6, 300, 260);
    }

    /** The cocoons up close (as the game draws them, on their springs): Flesh Bloom drawn in, and the kakuja's Monstrous Recovery. */
    static void fortress(File file) throws Exception {
        List<ArtPreview.Shot> shots = new ArrayList<>();
        Object[][] views = {{"front", new double[] {0.6, 2.6, 6.2}}, {"his left", new double[] {-6.2, 2.4, 0.8}}, {"behind", new double[] {-1.0, 3.0, -6.2}},
            {"from above", new double[] {0.4, 7.5, 1.6}}};
        for (FurutaStage stage : new FurutaStage[] {FurutaStage.DRAGON, FurutaStage.KAKUJA}) {
            boolean kakuja = stage == FurutaStage.KAKUJA;
            Scene scene = new Scene(t -> null, t -> null, t -> null, t -> 0.0, t -> 0.0, t -> 0.0);
            Chain chain = kakuja ? new Chain(stage, frameMastery(stage), scene, new FurutaMove[0], new double[0])
                .jobs(new FurutaMove[] {FurutaMove.RECOVERY}, new double[] {0.0})
                : new Chain(stage, frameMastery(stage), scene, new FurutaMove[] {FurutaMove.BLOOM}, new double[] {0.0});
            double age = kakuja ? 24.0 : 5.5;
            double far = kakuja ? 2.1 : 1.15;
            for (Object[] view : views) {
                double[] v = (double[])view[1];
                double[] eye = {v[0] * far, 1.4 + (v[1] - 1.4) * far, v[2] * far};
                shots.add(chain.shot(age, (kakuja ? "Monstrous Recovery, " : "Flesh Bloom drawn in, ") + view[0], eye, new double[] {0.0, 1.4, 0.0}));
            }
        }
        ArtPreview.sheet(file, "Furuta DNA: the cocoons (a wall about 1.8 blocks round him, eyes out)", shots, 4, 450, 400);
    }

    /** True for a move whose blows are mostly sweeps, spins and bursts round him (seen best from above). */
    static boolean sweeping(FurutaMove move, int limbs) {
        int round = 0;
        int other = 0;
        for (FurutaMove.Beat beat : move.beats(limbs)) {
            switch (beat.kind()) {
                case SWEEP, SPIN, BURST, CURL -> round++;
                default -> other++;
            }
        }
        return round > other;
    }

    static String trim(double v) {
        return Math.abs(v - Math.rint(v)) < 1.0E-6 ? Long.toString(Math.round(v)) : String.format(java.util.Locale.ROOT, "%.1f", v);
    }

    /** The move's point: the dummy's middle, in front of him at about the reach its strikes are made for. */
    static double[] frameAim(FurutaMove move, int limbs) {
        double reach = 0.0;
        double shortest = Double.MAX_VALUE;
        for (FurutaMove.Beat beat : move.beats(limbs)) {
            reach = Math.max(reach, beat.reach());
            shortest = Math.min(shortest, beat.reach());
        }
        double z = reach >= 12.0 ? 9.0 : Math.max(2.4, Math.min(4.5, 0.8 * Math.min(reach, shortest + 1.5)));
        return new double[] {0.0, 1.0, z};
    }

    /** One dummy a limb for the moves that send each limb after its own (spread round in front of him), else null. */
    static double[][] frameTargets(FurutaMove move, int limbs, double[] aim) {
        boolean multi = move.beats(limbs).stream().anyMatch(b -> b.has(FurutaMove.MULTI));
        if (!multi) {
            return null;
        }
        double[][] out = new double[limbs][];
        for (int i = 0; i < limbs; i++) {
            double a = Math.toRadians((i - (limbs - 1) * 0.5) * 22.0);
            double d = aim[2] * (0.75 + 0.12 * (i % 3));
            out[i] = new double[] {Math.sin(a) * d, 1.0, Math.cos(a) * d};
        }
        return out;
    }

    /** The ticks to show: the first wind-up, the blows landing (a sweep's middle too), the end; {@link #FRAMES} of them. */
    static double[] frameAges(FurutaMove move, int limbs) {
        java.util.TreeSet<Double> keys = new java.util.TreeSet<>();
        List<FurutaMove.Beat> beats = move.beats(limbs);
        int lastEnd = 0;
        for (FurutaMove.Beat beat : beats) {
            keys.add((double)beat.land());
            if (beat.kind() == FurutaMove.Kind.SWEEP || beat.kind() == FurutaMove.Kind.SPIN || beat.kind() == FurutaMove.Kind.CURL) {
                keys.add(Math.floor((beat.land() + beat.end()) * 0.5));
            }
            lastEnd = Math.max(lastEnd, beat.end());
        }
        FurutaMove.Beat first = beats.isEmpty() ? null : beats.get(0);
        double windup = first == null ? 0.0 : Math.floor(first.start() + (first.land() - first.start()) * 0.6);
        double end = Math.max(lastEnd + 1, move.ticks());
        List<Double> middle = new ArrayList<>(keys);
        middle.removeIf(t -> t <= windup || t >= end);
        List<Double> out = new ArrayList<>();
        out.add(windup);
        int room = FRAMES - 2;
        if (middle.size() > room) {
            for (int i = 0; i < room; i++) {
                out.add(middle.get((int)Math.round(i * (middle.size() - 1) / (double)(room - 1))));
            }
        } else {
            out.addAll(middle);
            // Fill the gaps evenly through the move.
            while (out.size() < FRAMES - 1) {
                double best = -1.0;
                double at = 0.0;
                List<Double> sorted = new ArrayList<>(out);
                sorted.add(end);
                java.util.Collections.sort(sorted);
                for (int i = 0; i + 1 < sorted.size(); i++) {
                    double gap = sorted.get(i + 1) - sorted.get(i);
                    if (gap > best) {
                        best = gap;
                        at = Math.floor(sorted.get(i) + gap * 0.5);
                    }
                }
                if (best < 2.0 || out.contains(at)) {
                    break;
                }
                out.add(at);
            }
        }
        out.add(end);
        java.util.Collections.sort(out);
        double[] ages = new double[out.size()];
        for (int i = 0; i < ages.length; i++) {
            ages[i] = out.get(i);
        }
        return ages;
    }

    /** The limbs following the rig's poses on the game's springs ({@code FurutaMotion}), from rest into the move. */
    static final class Simulated {
        static final double STEP = 0.25;
        static final double CLOCK = 100.0;
        final FurutaStage stage;
        final int mastery;
        final FurutaMove move;
        final double[] aim;
        final double[][] targets;
        final int n;
        final com.hex.kagune.control.furuta.FurutaMotion.Limb[] legs;
        double time = -24.0;
        /** The prey its mouths hunt at rest (rig space), or null. */
        double[] prey;

        Simulated(FurutaStage stage, int mastery, FurutaMove move, double[] aim, double[][] targets) {
            this.stage = stage;
            this.mastery = mastery;
            this.move = move;
            this.aim = aim;
            this.targets = targets;
            this.n = stage.limbs(mastery);
            this.legs = new com.hex.kagune.control.furuta.FurutaMotion.Limb[this.n];
            for (int i = 0; i < this.n; i++) {
                this.legs[i] = new com.hex.kagune.control.furuta.FurutaMotion.Limb();
            }
            follow(this.time);
        }

        FurutaRig.Input input(int limb, double age) {
            boolean moving = age >= 0.0;
            double[] held = moving ? held(age) : null;
            return new FurutaRig.Input(this.stage, this.n, limb, this.stage.length(this.mastery), new FurutaRig.Body(0.0, false, 0.0, true, 0.0, 0.0,
                CLOCK + age, this.prey), moving ? this.move : FurutaMove.NONE, Math.max(0.0, age), moving ? this.aim : null,
                moving && this.targets != null ? this.targets[limb] : null, held, 0.35);
        }

        void follow(double age) {
            for (int limb = 0; limb < this.n; limb++) {
                this.legs[limb].follow(FurutaRig.pose(input(limb, age)), CLOCK + age);
            }
        }

        void run(double age) {
            while (this.time + STEP <= age + 1.0E-9) {
                this.time += STEP;
                follow(this.time);
            }
        }

        /** Where the server carries the body this move holds at {@code age} (rig space), or null. */
        double[] held(double age) {
            for (FurutaMove.Beat beat : this.move.beats(this.n)) {
                boolean grips = beat.kind() == FurutaMove.Kind.SEIZE;
                if (grips && age > beat.land() && age <= beat.end()) {
                    return com.hex.kagune.control.furuta.FurutaRules.heldPoint(this.move, beat.kind(), age - beat.land());
                }
            }
            return null;
        }

        /** The kakuja's own jaw: thrown open through its bites. */
        double ownJaw(double age) {
            for (FurutaMove.Beat beat : this.move.beats(this.n)) {
                if (beat.limb() == FurutaMove.ARM && beat.kind() == FurutaMove.Kind.BITE && beat.holds(age)) {
                    return age < beat.land() ? smooth(beat.windup(age)) : 0.15;
                }
            }
            return 0.25;
        }

        List<WingPreview.Tri> draw(RigPose body, double age) {
            List<WingPreview.Tri> out = new ArrayList<>();
            double width = this.stage.width(this.mastery);
            for (int limb = 0; limb < this.n; limb++) {
                com.hex.kagune.control.furuta.FurutaMotion.Limb leg = this.legs[limb];
                double[][] points = com.hex.kagune.control.client.animation.FurutaPose.ride(body, leg.points());
                KakujaModel.Spine spine = CentipedeTail.through(points, 96, FurutaRig.side(this.stage, this.n, limb), leg.dorsal());
                double[] tip = points[points.length - 1];
                double[] goal = this.targets != null ? this.targets[limb] : this.aim != null ? this.aim : this.prey;
                FurutaModel.Look look = new FurutaModel.Look(width, true, 1.0, limb * 7 + 3, leg.jaw(), leg.flare(), leg.stare(),
                    FurutaRig.maw(this.stage, limb), CLOCK + age,
                    FurutaModel.gazeToward(tip, goal), this.move.guards() && FurutaRig.cocoon(this.stage) && age <= this.move.ticks() + 2);
                FurutaStage drawn = this.stage;
                out.addAll(ArtPreview.placed(sink -> FurutaModel.limb(sink, drawn, spine, look), q -> q));
            }
            return out;
        }
    }

    static double smooth(double v) {
        double t = Math.max(0.0, Math.min(1.0, v));
        return t * t * (3.0 - 2.0 * t);
    }

    /** Yellow markers where the server hits on this tick (none between blows); the kinds of blow landing, for the label. */
    static String hits(List<WingPreview.Tri> tris, FurutaStage stage, int n, double size, FurutaMove move, double age, double[] aim, double[][] targets) {
        StringBuilder landing = new StringBuilder();
        int yellow = 0xFFFFD21E;
        double moveReach = 3.0;
        for (FurutaMove.Beat beat : move.beats(n)) {
            moveReach = Math.max(moveReach, beat.reach());
        }
        moveReach = Math.min(moveReach, 18.0);
        for (FurutaMove.Beat beat : move.beats(n)) {
            boolean lands = age >= beat.land() && age < beat.land() + 1.0;
            boolean sweeping = (beat.kind() == FurutaMove.Kind.SWEEP || beat.kind() == FurutaMove.Kind.SPIN) && age >= beat.land() && age <= beat.end();
            if (!lands && !sweeping) {
                continue;
            }
            if (lands && landing.indexOf(beat.kind().name()) < 0) {
                landing.append(landing.length() == 0 ? "" : "+").append(beat.kind().name());
            }
            if (beat.kind() == FurutaMove.Kind.SWEEP) {
                // The arc the server sweeps this tick, at its reach and height.
                double span = Math.max(1, beat.end() - beat.land());
                double before = age == beat.land() ? beat.from() : beat.from() + (beat.to() - beat.from()) * smooth((age - 1 - beat.land()) / span);
                double after = beat.from() + (beat.to() - beat.from()) * smooth((age - beat.land()) / span);
                for (int k = 0; k <= 6; k++) {
                    double a = Math.toRadians(before + (after - before) * k / 6.0);
                    marker(tris, new double[] {Math.sin(a) * moveReach, beat.height(), Math.cos(a) * moveReach}, 0.12, yellow);
                }
                continue;
            }
            if (beat.kind() == FurutaMove.Kind.SPIN || beat.kind() == FurutaMove.Kind.BURST) {
                double reach = Math.min(beat.reach(), size * 1.4) + 0.6;
                for (int k = 0; k < 16; k++) {
                    double a = Math.PI * 2.0 * k / 16.0;
                    marker(tris, new double[] {Math.sin(a) * reach, 1.0, Math.cos(a) * reach}, 0.1, yellow);
                }
                continue;
            }
            if (beat.kind() == FurutaMove.Kind.CLAW) {
                for (int k = -2; k <= 2; k++) {
                    double a = Math.toRadians(k * 30.0);
                    marker(tris, new double[] {Math.sin(a) * beat.reach(), 1.2, Math.cos(a) * beat.reach()}, 0.1, yellow);
                }
                continue;
            }
            for (int limb = 0; limb < n; limb++) {
                if (!beat.involves(limb) && !(beat.limb() == FurutaMove.ARM && limb == 0)) {
                    continue;
                }
                FurutaRig.Input in = new FurutaRig.Input(stage, n, limb, size, FurutaRig.Body.STILL, move, age, aim, targets == null ? null : targets[limb],
                    null, 0.35);
                double[] at = FurutaRig.hitPoint(in, beat);
                if (at != null) {
                    marker(tris, at, 0.2, yellow);
                }
                if (beat.limb() == FurutaMove.ARM) {
                    break;
                }
            }
        }
        return landing.toString();
    }

    static void marker(List<WingPreview.Tri> tris, double[] at, double r, int argb) {
        EtoPreview.box(tris, new double[] {at[0] - r, at[1] - r, at[2] - r}, new double[] {at[0] + r, at[1] + r, at[2] + r}, argb);
    }

    /** The kagune growing out of his back, and the kakuja's armor spreading. */
    static void deploy(File file) throws Exception {
        RigPose still = new RigPose();
        List<ArtPreview.Shot> shots = new ArrayList<>();
        for (double g : new double[] {0.15, 0.4, 0.7, 1.0}) {
            double[] eye = {-3.0, 3.0, -4.5};
            shots.add(new ArtPreview.Shot("Tails tearing out: " + Math.round(g * 100) + "%", still, ArtPreview.NO_WINGS, eye, new double[] {0.3, 1.9, 0.0},
                whole(still, FurutaStage.TRANSPLANT, 200, FurutaMove.NONE, 0.0, null, g, eye)));
        }
        for (double g : new double[] {0.25, 0.5, 0.75, 1.0}) {
            shots.add(new ArtPreview.Shot("Kakuja armor spreading: " + Math.round(g * 100) + "%", still, ArtPreview.NO_WINGS, new double[] {1.4, 1.5, 3.2},
                new double[] {0.0, 1.2, 0.0}, kakujaBody(still, g, 0.0, 0.0)));
        }
        ArtPreview.sheet(file, "Furuta DNA: growing out", shots, 4, 420, 380);
    }

    /** Every limb of {@code stage} posed for {@code move} at {@code age}, grown {@code grown} of the way out. */
    static List<WingPreview.Tri> whole(RigPose pose, FurutaStage stage, int mastery, FurutaMove move, double age, double[] aim, double grown, double[] camera) {
        List<WingPreview.Tri> out = new ArrayList<>();
        int n = stage.limbs(mastery);
        double length = stage.length(mastery);
        double width = stage.width(mastery);
        for (int limb = 0; limb < n; limb++) {
            double[] held = move.holds() && age > 6.0 && aim != null ? aim : null;
            FurutaRig.Pose p = FurutaRig.pose(new FurutaRig.Input(stage, n, limb, length, new FurutaRig.Body(0.0, false, 0.0, true, 0.0, 0.0, 40.0), move, age,
                aim, null, held, 0.35));
            double[][] points = com.hex.kagune.control.client.animation.FurutaPose.ride(pose, p.points());
            if (grown < 0.999) {
                points = CentipedeMotion.resample(points, CentipedeMotion.curveLength(points) * Math.max(0.05, grown));
            }
            int side = FurutaRig.side(stage, n, limb);
            KakujaModel.Spine spine = CentipedeTail.through(points, 96, side, p.dorsal());
            double[] tip = points[points.length - 1];
            double[] gaze = aim == null ? null : FurutaModel.gazeToward(tip, aim);
            FurutaModel.Look look = new FurutaModel.Look(width * (0.45 + 0.55 * grown), true, 1.0, limb * 7 + 3, p.jaw(), p.flare(), p.stare(),
                FurutaRig.maw(stage, limb), 40.0, gaze);
            out.addAll(ArtPreview.placed(sink -> FurutaModel.limb(sink, stage, spine, look), q -> q));
        }
        return out;
    }

    static final String ROOT = System.getProperty("furuta.root", ".");

    /** His body as the game poses it ({@code FurutaPose}): the kakuja's stance on the kakuja, {@code move} at {@code age}. */
    static RigPose posed(FurutaStage stage, int mastery, FurutaMove move, double age) {
        RigPose p = new RigPose();
        com.hex.kagune.control.client.animation.FurutaPose.body(p, stage, stage.limbs(mastery), move, age, stage == FurutaStage.KAKUJA ? 1.0 : 0.0, age + 20.0);
        return p;
    }

    /** The kakuja's hide on every part of the posed body (the game's texture and its glow), grown {@code grow}. */
    static List<WingPreview.Tri> hide(RigPose pose, double grow) {
        List<WingPreview.Tri> out = new ArrayList<>();
        WingPreview.Texture skin = WingPreview.Texture.load(ROOT + "/src/main/resources/assets/hexkagune/" + com.hex.kagune.control.furuta.FurutaSkin.TEXTURE);
        WingPreview.Texture.load(ROOT + "/src/main/resources/assets/hexkagune/" + com.hex.kagune.control.furuta.FurutaSkin.GLOW);
        WingPreview.Texture glow = WingPreview.Texture.bright(ROOT + "/src/main/resources/assets/hexkagune/" + com.hex.kagune.control.furuta.FurutaSkin.GLOW);
        Object[][] parts = {{com.hex.kagune.control.furuta.FurutaSkin.Box.HEAD, "head"}, {com.hex.kagune.control.furuta.FurutaSkin.Box.TORSO, "torso"},
            {com.hex.kagune.control.furuta.FurutaSkin.Box.RIGHT_ARM, "rightArm"}, {com.hex.kagune.control.furuta.FurutaSkin.Box.LEFT_ARM, "leftArm"},
            {com.hex.kagune.control.furuta.FurutaSkin.Box.RIGHT_LEG, "rightLeg"}, {com.hex.kagune.control.furuta.FurutaSkin.Box.LEFT_LEG, "leftLeg"}};
        for (Object[] part : parts) {
            com.hex.kagune.control.furuta.FurutaSkin.Box box = (com.hex.kagune.control.furuta.FurutaSkin.Box)part[0];
            String name = (String)part[1];
            com.hex.kagune.control.furuta.FurutaSkin.build((corners, uv, n, alpha) -> {
                if ((alpha[0] + alpha[1] + alpha[2] + alpha[3]) * 0.25 < 0.5) {
                    return; // (the preview cuts the spreading edge out; the game fades it)
                }
                double[][] c = new double[4][];
                for (int k = 0; k < 4; k++) {
                    c[k] = ArtPreview.onPart(pose, name, corners[k]);
                }
                double[] tip = ArtPreview.onPart(pose, name, new double[] {corners[0][0] + n[0], corners[0][1] + n[1], corners[0][2] + n[2]});
                double[] wn = PoseRender.norm(PoseRender.sub(tip, c[0]));
                int white = 0xFFFFFFFF;
                out.add(new WingPreview.Tri(new double[][] {c[0], c[1], c[2]}, new int[] {white, white, white}, wn, false, false,
                    new double[][] {uv[0], uv[1], uv[2]}, skin));
                out.add(new WingPreview.Tri(new double[][] {c[0], c[2], c[3]}, new int[] {white, white, white}, wn, false, false,
                    new double[][] {uv[0], uv[2], uv[3]}, skin));
                out.add(new WingPreview.Tri(new double[][] {c[0], c[1], c[2]}, new int[] {white, white, white}, wn, false, true,
                    new double[][] {uv[0], uv[1], uv[2]}, glow));
                out.add(new WingPreview.Tri(new double[][] {c[0], c[2], c[3]}, new int[] {white, white, white}, wn, false, true,
                    new double[][] {uv[0], uv[2], uv[3]}, glow));
            }, box, grow, 1.0);
        }
        return out;
    }

    /** The kakuja's hide and what stands out of it on every part of the posed body, and the head, grown {@code grow}, its jaw {@code jaw}. */
    static List<WingPreview.Tri> kakujaBody(RigPose pose, double grow, double jaw, double time) {
        List<WingPreview.Tri> out = new ArrayList<>(hide(pose, grow));
        Object[][] parts = {{FurutaArmor.Part.TORSO, "torso"}, {FurutaArmor.Part.RIGHT_ARM, "rightArm"}, {FurutaArmor.Part.LEFT_ARM, "leftArm"},
            {FurutaArmor.Part.RIGHT_LEG, "rightLeg"}, {FurutaArmor.Part.LEFT_LEG, "leftLeg"}};
        for (Object[] part : parts) {
            FurutaArmor.Part p = (FurutaArmor.Part)part[0];
            String name = (String)part[1];
            out.addAll(ArtPreview.placed(sink -> FurutaArmor.build(sink, p, false, grow, time, true), q -> ArtPreview.onPart(pose, name, q)));
        }
        out.addAll(ArtPreview.placed(sink -> FurutaArmor.head(sink, grow, jaw, -2.0, -3.6, jaw, time, true), q -> ArtPreview.onPart(pose, "head", q)));
        return out;
    }

    static List<WingPreview.Tri> with(List<WingPreview.Tri> a, List<WingPreview.Tri> b) {
        List<WingPreview.Tri> out = new ArrayList<>(a);
        out.addAll(b);
        return out;
    }
}
