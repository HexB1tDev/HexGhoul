import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.pale.PaleSteamRules;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Pale's base form off-game (6.1.0): the posed body ({@link PoseRender}, exact PlayerAnimator + bendy-lib) in the art previewer's
 * translucent rasterizer, with her steam simulated by the game's own rules ({@link PaleSteamRules}) and, sheet by sheet, her pale
 * flesh and her moves. Rig space throughout (x to her right, y up from her feet, z forward).
 * Usage: {@code tools/pale-preview.sh <out-dir> [sheet ...]}; sheets: steam, idle, blitz, blitz-gif, strike-gifs, punch, flips, flesh,
 * blitz-fp (and the rest listed in main).
 */
public final class PalePreview {
    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "build/preview");
        out.mkdirs();
        java.util.Set<String> wanted = new java.util.HashSet<>(java.util.Arrays.asList(args).subList(Math.min(1, args.length), args.length));
        if (wanted.isEmpty() || wanted.contains("steam")) {
            steam(new File(out, "pale-steam.png"));
        }
        if (wanted.isEmpty() || wanted.contains("idle")) {
            idle(new File(out, "pale-idle.png"));
        }
        if (wanted.isEmpty() || wanted.contains("blitz")) {
            blitz(new File(out, "pale-blitz.png"));
        }
        if (wanted.isEmpty() || wanted.contains("blitz-gif")) {
            blitzGif(new File(out, "pale-blitz.gif"), new double[] {3.0, 1.6, 2.6}, "Speed blitz, real time");
        }
        if (wanted.contains("strike-gifs")) {
            for (com.hex.kagune.control.pale.PaleStrike strike : com.hex.kagune.control.pale.PaleStrike.values()) {
                double side = strike.right ? 1.0 : -1.0;
                strikeGif(new File(out, "pale-" + strike.name().toLowerCase(java.util.Locale.ROOT) + ".gif"), strike, new double[] {3.2 * side, 1.55, 2.2}, 4.0);
            }
        }
        if (wanted.isEmpty() || wanted.contains("punch")) {
            punch(new File(out, "pale-big-punch.png"));
        }
        if (wanted.isEmpty() || wanted.contains("flips")) {
            flips(new File(out, "pale-flips.png"));
        }
        if (wanted.isEmpty() || wanted.contains("flesh")) {
            flesh(new File(out, "pale-flesh.png"));
        }
        if (wanted.isEmpty() || wanted.contains("kick")) {
            kick(new File(out, "pale-kick.png"));
        }
        if (wanted.isEmpty() || wanted.contains("hammer-huge")) {
            hammerHuge(new File(out, "pale-hammer-huge.png"));
        }
        if (wanted.isEmpty() || wanted.contains("witness")) {
            witness(new File(out, "pale-witness.png"));
        }
        if (wanted.contains("witness-gif")) {
            witnessGif(new File(out, "pale-witness.gif"), 1.0);
            witnessGif(new File(out, "pale-witness-slow.gif"), 0.4);
        }
        if (wanted.isEmpty() || wanted.contains("kick-pose")) {
            kickPoses(new File(out, "pale-kick-poses.png"));
        }
        if (wanted.isEmpty() || wanted.contains("hammer")) {
            hammer(new File(out, "pale-hammer.png"));
        }
        if (wanted.isEmpty() || wanted.contains("kicked")) {
            kicked(new File(out, "pale-kicked.png"));
        }
        if (wanted.isEmpty() || wanted.contains("kick-fp")) {
            kickFirstPerson(new File(out, "pale-kick-first-person.png"));
        }
        if (wanted.isEmpty() || wanted.contains("watch")) {
            watch(new File(out, "pale-watch.png"));
        }
        if (wanted.isEmpty() || wanted.contains("hammer-fp")) {
            hammerFirstPerson(new File(out, "pale-hammer-first-person.png"));
        }
        if (wanted.isEmpty() || wanted.contains("blitz-fp")) {
            blitzFirstPerson(new File(out, "pale-blitz-first-person.png"));
        }
        if (wanted.isEmpty() || wanted.contains("swipe")) {
            swipe(new File(out, "pale-swipe.png"));
        }
        if (wanted.isEmpty() || wanted.contains("arm")) {
            arm(new File(out, "pale-arm.png"));
        }
        if (wanted.isEmpty() || wanted.contains("owl")) {
            owl(new File(out, "pale-owl.png"));
        }
        if (wanted.isEmpty() || wanted.contains("owl-anim")) {
            owlAnim(new File(out, "pale-owl-anim.png"));
        }
        if (wanted.isEmpty() || wanted.contains("owl-transform")) {
            owlTransform(new File(out, "pale-owl-transform.png"));
        }
        if (wanted.isEmpty() || wanted.contains("owl-punch")) {
            owlPunch(new File(out, "pale-owl-punch.png"));
        }
        if (wanted.isEmpty() || wanted.contains("owl-kick")) {
            owlKick(new File(out, "pale-owl-kick.png"));
        }
        if (wanted.isEmpty() || wanted.contains("owl-spikes")) {
            owlSpikes(new File(out, "pale-owl-spikes.png"));
        }
        if (wanted.isEmpty() || wanted.contains("owl-rend")) {
            owlRend(new File(out, "pale-owl-rend.png"));
        }
        if (wanted.isEmpty() || wanted.contains("owl-whirl")) {
            owlWhirl(new File(out, "pale-owl-whirl.png"));
        }
        if (wanted.isEmpty() || wanted.contains("owl-pounce")) {
            owlPounce(new File(out, "pale-owl-pounce.png"));
        }
        if (wanted.isEmpty() || wanted.contains("owl-fissure")) {
            owlFissure(new File(out, "pale-owl-fissure.png"));
        }
        if (wanted.isEmpty() || wanted.contains("owl-watch")) {
            owlWatch(new File(out, "pale-owl-watch.png"));
        }
        if (wanted.isEmpty() || wanted.contains("owl-volley")) {
            owlVolley(new File(out, "pale-owl-volley.png"));
        }
        if (wanted.isEmpty() || wanted.contains("revive")) {
            revive(new File(out, "pale-revive.png"));
        }
        if (wanted.isEmpty() || wanted.contains("revive-end")) {
            reviveEnd(new File(out, "pale-revive-end.png"));
        }
        if (wanted.isEmpty() || wanted.contains("eyes")) {
            eyes(new File(out, "eye-mode.png"));
        }
    }

    // ------------------------------------------------------------------ 7.0.1: the shared eye-mode fight

    /**
     * 7.0.1: any ghoul with only the kakugan open fights with Owl DNA's body (EyeRules): no steam, no flesh, the kakugan on. Every
     * blitz strike on its impact tick (with the mastery it opens at and how hard the eyes hit then), the four flips in the air, the
     * Big Punch charged and landing, the Speed Kick's blow.
     */
    static void eyes(File file) throws Exception {
        List<ArtPreview.Shot> shots = new ArrayList<>();
        double[] target = {0.0, 0.95, 0.75};
        for (com.hex.kagune.control.pale.PaleStrike strike : com.hex.kagune.control.pale.PaleStrike.values()) {
            RigPose p = com.hex.kagune.control.client.animation.PaleMotions.strike(strike, strike.impact, null);
            p.settle(1.0F);
            double side = strike.right ? 1.0 : -1.0;
            List<WingPreview.Tri> scene = dummy(DUMMY_Z);
            scene.addAll(ArtPreview.kakuganTris(p));
            float hit = com.hex.kagune.control.pale.PaleRules.fist(strike.mastery) * strike.damage * com.hex.kagune.control.pale.EyeRules.power(strike.mastery);
            shots.add(new ArtPreview.Shot(String.format(java.util.Locale.ROOT, "M1 %s (mastery %d, %.1f dmg)",
                strike.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' '), strike.mastery, hit), p, ArtPreview.NO_WINGS,
                new double[] {3.0 * side, 2.1, 3.0}, target, scene));
        }
        for (com.hex.kagune.control.pale.PaleRules.Flip flip : com.hex.kagune.control.pale.PaleRules.Flip.values()) {
            boolean sideways = flip == com.hex.kagune.control.pale.PaleRules.Flip.LEFT || flip == com.hex.kagune.control.pale.PaleRules.Flip.RIGHT;
            RigPose p = com.hex.kagune.control.client.animation.PaleMotions.flip(flip, 6.0, -1.0);
            p.settle(1.0F);
            shots.add(new ArtPreview.Shot("right click: " + flip.name().toLowerCase(java.util.Locale.ROOT) + " flip", p, ArtPreview.NO_WINGS,
                sideways ? new double[] {0.4, 1.3, 4.2} : new double[] {4.2, 1.3, 0.4}, new double[] {0.0, 0.9, 0.0}, ArtPreview.kakuganTris(p)));
        }
        RigPose full = com.hex.kagune.control.client.animation.PaleMotions.punchCharge(30.0, null);
        RigPose charging = new RigPose().add(full, 1.0F);
        charging.settle(1.0F);
        shots.add(new ArtPreview.Shot("Z held: Big Punch charging", charging, ArtPreview.NO_WINGS, new double[] {3.0, 2.0, 3.1}, target,
            ArtPreview.kakuganTris(charging)));
        RigPose punch = com.hex.kagune.control.client.animation.PaleMotions.punch(com.hex.kagune.control.pale.PaleRules.PUNCH_IMPACT, full);
        punch.settle(1.0F);
        List<WingPreview.Tri> punched = dummy(DUMMY_Z + 0.2);
        punched.addAll(ArtPreview.kakuganTris(punch));
        shots.add(new ArtPreview.Shot("Z let go: Big Punch lands", punch, ArtPreview.NO_WINGS, new double[] {3.0, 2.0, 3.1}, target, punched));
        int dash = 3;
        double contact = com.hex.kagune.control.pale.PaleRules.kickArrive(dash) + com.hex.kagune.control.pale.PaleRules.KICK_CONTACT;
        RigPose kick = com.hex.kagune.control.client.animation.PaleMotions.kick(contact, dash, null);
        kick.settle(1.0F);
        double up = com.hex.kagune.control.pale.PaleRules.kickHeight(dash, com.hex.kagune.control.pale.PaleRules.kickLift(1.0), contact);
        double[] stand = com.hex.kagune.control.pale.PaleRules.kickStand(0.25);
        List<WingPreview.Tri> kicked = figure(new double[] {-stand[0], 1.0 - up, -stand[1]});
        box(kicked, new double[] {-6.0, -0.2 - up, -4.0}, new double[] {4.0, -up, 14.0}, 0xFF4E5A44);
        kicked.addAll(ArtPreview.kakuganTris(kick));
        shots.add(new ArtPreview.Shot("V: Speed Kick (mastery " + com.hex.kagune.control.pale.PaleRules.KICK_MASTERY + ")", kick, ArtPreview.NO_WINGS,
            new double[] {4.6, 1.3 - up * 0.3, 0.9}, new double[] {0.0, 0.9 - up * 0.4, 0.5}, kicked));
        ArtPreview.sheet(file, String.format(java.util.Locale.ROOT, "Eye mode, any path (kakugan open, kagune away): Owl DNA's body kit; "
            + "the eyes hit x%.2f at mastery 0, x%.2f at 1000", com.hex.kagune.control.pale.EyeRules.power(0), com.hex.kagune.control.pale.EyeRules.power(1000)),
            shots, 5, 300, 330);
    }

    // ------------------------------------------------------------------ You Thought You'd Get Rid of Me That Easily?

    /**
     * Her remains as {@code Remains} has them {@code age} ticks after the blow (as triangles): every piece in a plain skin (pale
     * skin, black hair and jacket, blue trousers, grey shoes), its torn ends raw; her guts; the flesh lines.
     */
    static List<WingPreview.Tri> remains(com.hex.kagune.control.client.animation.Remains.Scatter scatter, double age) {
        List<WingPreview.Tri> tris = new ArrayList<>();
        for (com.hex.kagune.control.client.animation.Remains.Piece piece : com.hex.kagune.control.client.animation.Remains.Piece.values()) {
            com.hex.kagune.control.client.animation.Remains.Placed placed = com.hex.kagune.control.client.animation.Remains.place(scatter, piece, age);
            java.util.function.Function<double[], double[]> at = own -> {
                double[] r = com.hex.kagune.control.client.animation.Remains.rotate(placed.turn(), own);
                return new double[] {placed.at()[0] + r[0], placed.at()[1] + r[1], placed.at()[2] + r[2]};
            };
            com.hex.kagune.control.client.animation.Remains.mesh(piece, false, 7, new com.hex.kagune.control.client.animation.Remains.Sink() {
                @Override
                public void skin(double[] a, double[] b, double[] c, double[] d, double[] normal, float[] uv, boolean layer) {
                    if (layer) {
                        return;
                    }
                    double v = (uv[1] + uv[3] + uv[5] + uv[7]) * 16.0;
                    double u = (uv[0] + uv[2] + uv[4] + uv[6]) * 16.0;
                    int colour = skinColour(piece, u, v);
                    double[] n = com.hex.kagune.control.client.animation.Remains.rotate(placed.turn(), normal);
                    tris.add(new WingPreview.Tri(new double[][] {at.apply(a), at.apply(b), at.apply(c)}, new int[] {colour, colour, colour}, n, true));
                    tris.add(new WingPreview.Tri(new double[][] {at.apply(a), at.apply(c), at.apply(d)}, new int[] {colour, colour, colour}, n, true));
                }

                @Override
                public void flesh(double[][] polygon, double[] normal, int[] argb) {
                    double[] n = com.hex.kagune.control.client.animation.Remains.rotate(placed.turn(), normal);
                    for (int i = 1; i + 1 < polygon.length; i++) {
                        tris.add(new WingPreview.Tri(new double[][] {at.apply(polygon[0]), at.apply(polygon[i]), at.apply(polygon[i + 1])},
                            new int[] {argb[0], argb[i], argb[i + 1]}, n, true));
                    }
                }
            });
        }
        for (int rope = 0; rope < com.hex.kagune.control.client.animation.Remains.ROPES; rope++) {
            double[][] links = com.hex.kagune.control.client.animation.Remains.guts(scatter, rope, age);
            if (links != null) {
                tubeTris(tris, links, com.hex.kagune.control.client.animation.Remains.GUT_RADIUS, com.hex.kagune.control.client.animation.Remains.GUT);
            }
        }
        for (com.hex.kagune.control.client.animation.Remains.Piece piece : com.hex.kagune.control.client.animation.Remains.Piece.values()) {
            com.hex.kagune.control.client.animation.Remains.Line line = com.hex.kagune.control.client.animation.Remains.line(scatter, piece, age);
            if (line != null) {
                int points = 12;
                double[][] path = new double[points][];
                for (int i = 0; i < points; i++) {
                    double t = i / (points - 1.0) * line.reach();
                    path[i] = new double[] {line.from()[0] + (line.to()[0] - line.from()[0]) * t,
                        line.from()[1] + (line.to()[1] - line.from()[1]) * t - line.sag() * 4.0 * t * (1.0 - t),
                        line.from()[2] + (line.to()[2] - line.from()[2]) * t};
                }
                tubeTris(tris, path, 0.035, 0xFFB8323E);
            }
        }
        return tris;
    }

    /** A plain skin's colour for a quad of {@code piece} at ({@code u}, {@code v}) on a 64 x 64 skin. */
    static int skinColour(com.hex.kagune.control.client.animation.Remains.Piece piece, double u, double v) {
        String name = piece.name();
        if (name.equals("HEAD")) {
            return v < 8.0 || v < 12.0 && u > 24.0 ? 0xFF201814 : 0xFFE8C8B0;
        }
        if (name.equals("CHEST") || name.equals("BELLY")) {
            return 0xFF26222C;
        }
        if (name.contains("ARM")) {
            double vTop = name.startsWith("LEFT") ? 48.0 : 16.0;
            return v - vTop > 13.0 ? 0xFFE8C8B0 : 0xFF2E2A36;
        }
        double vTop = name.startsWith("LEFT") ? 48.0 : 16.0;
        return v - vTop > 14.0 ? 0xFF5A5A5A : 0xFF2E3A8C;
    }

    /** A tube along {@code path}, {@code radius} thick. */
    static void tubeTris(List<WingPreview.Tri> tris, double[][] path, double radius, int colour) {
        int sides = 6;
        double[][] previous = null;
        for (int i = 0; i < path.length; i++) {
            double[] a = path[Math.max(0, i - 1)];
            double[] b = path[Math.min(path.length - 1, i + 1)];
            double[] t = {b[0] - a[0], b[1] - a[1], b[2] - a[2]};
            double l = Math.sqrt(t[0] * t[0] + t[1] * t[1] + t[2] * t[2]);
            t = l < 1.0E-9 ? new double[] {0, 1, 0} : new double[] {t[0] / l, t[1] / l, t[2] / l};
            double[] ref = Math.abs(t[1]) > 0.9 ? new double[] {1, 0, 0} : new double[] {0, 1, 0};
            double[] x = PoseRender.norm(PoseRender.cross(t, ref));
            double[] y = PoseRender.cross(t, x);
            double[][] ring = new double[sides + 1][];
            for (int k = 0; k <= sides; k++) {
                double angle = k * Math.PI * 2.0 / sides;
                ring[k] = new double[] {path[i][0] + (x[0] * Math.cos(angle) + y[0] * Math.sin(angle)) * radius,
                    path[i][1] + (x[1] * Math.cos(angle) + y[1] * Math.sin(angle)) * radius, path[i][2] + (x[2] * Math.cos(angle) + y[2] * Math.sin(angle)) * radius};
            }
            if (previous != null) {
                for (int k = 0; k < sides; k++) {
                    double angle = (k + 0.5) * Math.PI * 2.0 / sides;
                    double[] n = {x[0] * Math.cos(angle) + y[0] * Math.sin(angle), x[1] * Math.cos(angle) + y[1] * Math.sin(angle),
                        x[2] * Math.cos(angle) + y[2] * Math.sin(angle)};
                    tris.add(new WingPreview.Tri(new double[][] {previous[k], previous[k + 1], ring[k + 1]}, new int[] {colour, colour, colour}, n, true));
                    tris.add(new WingPreview.Tri(new double[][] {previous[k], ring[k + 1], ring[k]}, new int[] {colour, colour, colour}, n, true));
                }
            }
            previous = ring;
        }
    }

    /**
     * You Thought You'd Get Rid of Me That Easily? (6.2.0): the blow blows her apart, the pieces fly, come down and lie there, torn
     * ends raw, her guts out; five seconds on, the flesh lines run out of her chest to every piece and haul it all back together.
     */
    static void revive(File file) throws Exception {
        List<ArtPreview.Shot> shots = new ArrayList<>();
        com.hex.kagune.control.client.animation.Remains.Scatter scatter = com.hex.kagune.control.client.animation.Remains.scatter(12345L, 0.0, false);
        int s = com.hex.kagune.control.pale.ReviveRules.SCATTER;
        int l = com.hex.kagune.control.pale.ReviveRules.LINES;
        Object[][] frames = {{"blown apart, tick 1", 1.0}, {"flying, tick 4", 4.0}, {"coming down, tick 9", 9.0}, {"lying there, tick 40", 40.0},
            {"the lines run out, tick " + (s + 4), s + 4.0}, {"reaching every piece, tick " + (s + 9), s + 9.0}, {"taut, tick " + (s + l), s + (double)l},
            {"the chest stands, the legs come, tick " + (s + l + 5), s + l + 5.0}, {"the arms, the head last, tick " + (s + l + 9), s + l + 9.0},
            {"together, tick " + (s + l + com.hex.kagune.control.pale.ReviveRules.PULL - 0.1), s + l + com.hex.kagune.control.pale.ReviveRules.PULL - 0.1}};
        double[] eye = {4.6, 3.6, 5.2};
        double[] look = {0.0, 0.4, 0.4};
        for (Object[] f : frames) {
            double age = (Double)f[1];
            List<WingPreview.Tri> scene = remains(scatter, age);
            for (double gx = -6.0; gx < 6.0; gx += 2.0) {
                for (double gz = -6.0; gz < 6.0; gz += 2.0) {
                    box(scene, new double[] {gx, -0.2, gz}, new double[] {gx + 2.0, 0.0, gz + 2.0}, ((int)(gx + gz) & 2) == 0 ? 0xFF4E5A44 : 0xFF48543F);
                }
            }
            shots.add(new ArtPreview.Shot((String)f[0], null, ArtPreview.NO_WINGS, eye, look, scene));
        }
        // Up close: the torn ends.
        List<WingPreview.Tri> close = remains(scatter, 60.0);
        double[] thigh = com.hex.kagune.control.client.animation.Remains.place(scatter, com.hex.kagune.control.client.animation.Remains.Piece.RIGHT_THIGH, 60.0).at();
        shots.add(new ArtPreview.Shot("a thigh, torn at both ends", null, ArtPreview.NO_WINGS, new double[] {thigh[0] + 0.9, thigh[1] + 0.8, thigh[2] + 0.9},
            thigh, close));
        double[] belly = com.hex.kagune.control.client.animation.Remains.place(scatter, com.hex.kagune.control.client.animation.Remains.Piece.BELLY, 60.0).at();
        shots.add(new ArtPreview.Shot("the belly, her guts out", null, ArtPreview.NO_WINGS, new double[] {belly[0] + 1.2, belly[1] + 1.1, belly[2] + 1.2},
            belly, close));
        ArtPreview.sheet(file, "You Thought You'd Get Rid of Me That Easily?: blown apart, five seconds in pieces, the lines, pulled back together",
            shots, 4, 380, 330);
    }

    /**
     * 6.2.0: the last of the revive up close, from the side and a little in front: the pull bringing every piece home and her body
     * standing there whole as the game then draws it (the hand-off from the pieces to her model must not jump).
     */
    static void reviveEnd(File file) throws Exception {
        List<ArtPreview.Shot> shots = new ArrayList<>();
        com.hex.kagune.control.client.animation.Remains.Scatter scatter = com.hex.kagune.control.client.animation.Remains.scatter(12345L, 0.0, false);
        int s = com.hex.kagune.control.pale.ReviveRules.SCATTER;
        int l = com.hex.kagune.control.pale.ReviveRules.LINES;
        int whole = com.hex.kagune.control.pale.ReviveRules.WHOLE;
        double[] eye = {2.6, 1.7, 3.4};
        double[] look = {0.0, 0.95, 0.0};
        double[] ages = {s + l, s + l + 2.0, s + l + 4.0, s + l + 6.0, s + l + 7.5, s + l + 9.0, s + l + 10.5, whole - 0.6, whole - 0.05};
        for (double age : ages) {
            List<WingPreview.Tri> scene = remains(scatter, age);
            box(scene, new double[] {-3.0, -0.2, -3.0}, new double[] {3.0, 0.0, 3.0}, 0xFF4E5A44);
            shots.add(new ArtPreview.Shot(String.format(java.util.Locale.ROOT, "tick %.1f", age), null, ArtPreview.NO_WINGS, eye, look, scene));
        }
        // Her model as triangles in the same camera, at the game's player scale (as the pieces are).
        double scale = ArtPreview.bodyScale;
        ArtPreview.bodyScale = com.hex.kagune.control.client.animation.BodyFrame.SCALE;
        RigPose model = com.hex.kagune.control.client.animation.Remains.wholePose();
        model.settle(1.0F);
        List<WingPreview.Tri> drawn = ArtPreview.body(model);
        box(drawn, new double[] {-3.0, -0.2, -3.0}, new double[] {3.0, 0.0, 3.0}, 0xFF4E5A44);
        shots.add(new ArtPreview.Shot("whole: her model, tick " + whole, null, ArtPreview.NO_WINGS, eye, look, drawn));
        RigPose stance = com.hex.kagune.control.client.animation.PaleIdle.compose(1, 0.0, null, 0.0F, 1);
        stance.settle(1.0F);
        List<WingPreview.Tri> standing = ArtPreview.body(stance);
        box(standing, new double[] {-3.0, -0.2, -3.0}, new double[] {3.0, 0.0, 3.0}, 0xFF4E5A44);
        shots.add(new ArtPreview.Shot("then into her stance", null, ArtPreview.NO_WINGS, eye, look, standing));
        ArtPreview.bodyScale = scale;
        ArtPreview.sheet(file, "The revive's last second up close: every piece hauled home, then her body whole", shots, 4, 340, 380);
    }

    // ------------------------------------------------------------------ 6.2.4: the Owl as a creature

    /** 6.2.4: the Owl's sheets draw the creature in her place (her own body not drawn); false: her body with the kakuja on it. */
    static boolean creature = true;

    /** Her body for a shot of the Owl: none (the creature is drawn in its place) or, not {@link #creature}, her body. */
    static RigPose shown(RigPose body) {
        return creature ? null : body;
    }

    /** The Owl on {@code body} posed so, as triangles: the creature carried over from it with its kagune, or her body's kakuja. */
    static List<WingPreview.Tri> owlScene(RigPose body, com.hex.kagune.control.kakugan.KakuganEyes eyes, com.hex.kagune.control.client.animation.OwlPose.Swing right,
                                          com.hex.kagune.control.client.animation.OwlPose.Swing left, double time, double grown) {
        if (!creature) {
            return owlTris(com.hex.kagune.control.client.animation.OwlPose.look(body, eyes, right, left, time, grown));
        }
        if (grown <= 0.01) {
            // Still herself: her own body (doubled over before it bursts out).
            List<WingPreview.Tri> own = new ArrayList<>(ArtPreview.body(body));
            return own;
        }
        com.hex.kagune.control.pale.OwlCreature.Rig rig = com.hex.kagune.control.pale.OwlCreature.rig(com.hex.kagune.control.client.animation.OwlPose.source(body,
            0.0, 0.0, false, time, grown));
        List<WingPreview.Tri> tris = new ArrayList<>();
        com.hex.kagune.control.ukaku.UkakuModel.Sink sink = (a, b, c, d, n, ca, cb, cc, cd) -> {
            tris.add(new WingPreview.Tri(new double[][] {a, b, c}, new int[] {ca, cb, cc}, n, true));
            tris.add(new WingPreview.Tri(new double[][] {a, c, d}, new int[] {ca, cc, cd}, n, true));
        };
        com.hex.kagune.control.pale.OwlCreature.mesh(sink, skinTris(tris), rig);
        com.hex.kagune.control.pale.OwlCreature.kagune(sink, rig, com.hex.kagune.control.client.animation.OwlPose.look(rig, right, left, time, grown));
        return tris;
    }

    /** Where the creature's painted textures are (as the game loads them from the mod's assets). */
    static final String OWL_TEXTURES = "src/main/resources/assets/hexkagune/textures/entity/";

    /** The creature's painted quads into {@code tris}, textured as the game draws them (its glow added at full brightness). */
    static com.hex.kagune.control.pale.OwlCreature.Skin skinTris(List<WingPreview.Tri> tris) {
        return (t, a, b, c, d, uv, nn, ca, cb, cc, cd) -> {
            WingPreview.Texture tex = t.bright && !t.glows ? WingPreview.Texture.bright(OWL_TEXTURES + t.file + ".png")
                : WingPreview.Texture.load(OWL_TEXTURES + t.file + ".png");
            double[][] uvs = {{uv[0], uv[1]}, {uv[2], uv[3]}, {uv[4], uv[5]}, {uv[6], uv[7]}};
            // (Each corner lit by its own normal, as the game's entity shader lights a vertex: its skin shaded smooth.)
            int[] lit = {ca, cb, cc, cd};
            if (!t.glows && !t.bright) {
                for (int k = 0; k < 4; k++) {
                    lit[k] = WingPreview.lit(lit[k], nn[k]);
                }
            }
            double[] n1 = t.glows || t.bright ? average(nn[0], nn[1], nn[2]) : null;
            double[] n2 = t.glows || t.bright ? average(nn[0], nn[2], nn[3]) : null;
            tris.add(new WingPreview.Tri(new double[][] {a, b, c}, new int[] {lit[0], lit[1], lit[2]}, n1, !t.glows, t.glows, new double[][] {uvs[0], uvs[1], uvs[2]}, tex));
            tris.add(new WingPreview.Tri(new double[][] {a, c, d}, new int[] {lit[0], lit[2], lit[3]}, n2, !t.glows, t.glows, new double[][] {uvs[0], uvs[2], uvs[3]}, tex));
        };
    }

    static double[] average(double[] a, double[] b, double[] c) {
        double[] s = {a[0] + b[0] + c[0], a[1] + b[1] + c[1], a[2] + b[2] + c[2]};
        double l = Math.sqrt(s[0] * s[0] + s[1] * s[1] + s[2] * s[2]);
        return l < 1.0E-9 ? a : new double[] {s[0] / l, s[1] / l, s[2] / l};
    }

    /** The shots of an Owl sheet framed for the creature (half again her size and more: every camera drawn back and up with it). */
    static List<ArtPreview.Shot> framed(List<ArtPreview.Shot> shots) {
        if (!creature) {
            return shots;
        }
        double k = com.hex.kagune.control.pale.OwlCreature.SCALE * 0.85;
        double back = 1.3;
        List<ArtPreview.Shot> out = new ArrayList<>();
        for (ArtPreview.Shot s : shots) {
            double[] t = {s.target()[0] * k, s.target()[1] * k, s.target()[2] * k};
            out.add(new ArtPreview.Shot(s.label(), s.pose(), s.wings(), new double[] {t[0] + (s.eye()[0] - s.target()[0]) * back,
                t[1] + (s.eye()[1] - s.target()[1]) * back, t[2] + (s.eye()[2] - s.target()[2]) * back}, t, s.extra()));
        }
        return out;
    }

    // ------------------------------------------------------------------ the Owl (kakuja V1)

    /** The Owl as the game draws it ({@link com.hex.kagune.control.pale.OwlModel#mesh}), as triangles. */
    static List<WingPreview.Tri> owlTris(com.hex.kagune.control.pale.OwlModel.Look look) {
        List<WingPreview.Tri> tris = new ArrayList<>();
        com.hex.kagune.control.pale.OwlModel.mesh((a, b, c, d, n, ca, cb, cc, cd) -> {
            tris.add(new WingPreview.Tri(new double[][] {a, b, c}, new int[] {ca, cb, cc}, n, true));
            tris.add(new WingPreview.Tri(new double[][] {a, c, d}, new int[] {ca, cc, cd}, n, true));
        }, look);
        return tris;
    }

    /** Pale's first kakuja, the Owl, standing: all round, as the third-person camera sees it, and the mask up close (either eye). */
    static void owl(File file) throws Exception {
        List<ArtPreview.Shot> shots = new ArrayList<>();
        RigPose stand = com.hex.kagune.control.client.animation.OwlPose.stance();
        stand.settle(1.0F);
        com.hex.kagune.control.client.animation.OwlPose.Swing rest = com.hex.kagune.control.client.animation.OwlPose.REST;
        com.hex.kagune.control.kakugan.KakuganEyes right = com.hex.kagune.control.kakugan.KakuganEyes.DEFAULT.withGhoul(com.hex.kagune.control.kakugan.KakuganEyes.RIGHT);
        com.hex.kagune.control.kakugan.KakuganEyes left = com.hex.kagune.control.kakugan.KakuganEyes.DEFAULT.withGhoul(com.hex.kagune.control.kakugan.KakuganEyes.LEFT);
        List<WingPreview.Tri> owl = owlScene(stand, right, rest, rest, 0.0, 1.0);
        double[] middle = {0.0, 1.15, 0.0};
        shots.add(new ArtPreview.Shot("front, three quarters", shown(stand), ArtPreview.NO_WINGS, new double[] {4.0, 2.2, 5.6}, middle, owl));
        shots.add(new ArtPreview.Shot("front", shown(stand), ArtPreview.NO_WINGS, new double[] {0.0, 1.6, 7.0}, middle, owl));
        shots.add(new ArtPreview.Shot("side", shown(stand), ArtPreview.NO_WINGS, new double[] {6.6, 1.6, 0.4}, middle, owl));
        shots.add(new ArtPreview.Shot("back, three quarters", shown(stand), ArtPreview.NO_WINGS, new double[] {-4.2, 2.5, -5.4}, middle, owl));
        shots.add(new ArtPreview.Shot("third person (behind, above)", shown(stand), ArtPreview.NO_WINGS, new double[] {0.6, 3.2, -6.0}, new double[] {0.0, 1.3, 0.6},
            owl));
        double[] face = {0.0, 1.6, 0.0};
        shots.add(new ArtPreview.Shot("the mask: kakugan right", shown(stand), ArtPreview.NO_WINGS, new double[] {0.5, 1.72, 1.9}, face, owl));
        shots.add(new ArtPreview.Shot("the mask: kakugan left", shown(stand), ArtPreview.NO_WINGS, new double[] {-0.5, 1.72, 1.9}, face,
            owlScene(stand, left, rest, rest, 0.0, 1.0)));
        shots.add(new ArtPreview.Shot("the mask: three quarters", shown(stand), ArtPreview.NO_WINGS, new double[] {1.5, 1.8, 1.4}, face, owl));
        // Her body drawn at the game's scale, so the mask sits on her head exactly as in game, and bulked up as HexHaki's haki
        // physique draws it.
        ArtPreview.bodyScale = com.hex.kagune.control.client.animation.BodyFrame.SCALE;
        PoseRender.bulk = 1.0;
        try {
            ArtPreview.sheet(file, "Pale's first kakuja, the Owl (V1)", framed(shots), 4, 380, 400);
        } finally {
            ArtPreview.bodyScale = 1.0;
            PoseRender.bulk = 0.0;
        }
    }

    /**
     * The Owl alive (6.2.0): standing, breathing (the giant arms heaving), swaying, the blades flicking out and back; walking, the
     * arms swinging against her stride and the blades lagging behind, lifted clear (as the game drives OwlPose.swing).
     */
    static void owlAnim(File file) throws Exception {
        List<ArtPreview.Shot> shots = new ArrayList<>();
        com.hex.kagune.control.kakugan.KakuganEyes eyes = com.hex.kagune.control.kakugan.KakuganEyes.DEFAULT
            .withGhoul(com.hex.kagune.control.kakugan.KakuganEyes.RIGHT);
        double grown = com.hex.kagune.control.pale.OwlRules.GROW_TICKS + 40.0;
        double[] look = {0.0, 1.1, 0.4};
        double flickAt = com.hex.kagune.control.client.animation.OwlPose.FLICK_LENGTH * 0.25;
        Object[][] idle = {{"breathing in", 14.0}, {"breathing out", 43.0}, {"swaying", 200.0}, {"the blades flick out", com.hex.kagune.control.client.animation.OwlPose.FLICK_EVERY * 3 + flickAt}};
        for (Object[] f : idle) {
            double time = (Double)f[1];
            RigPose body = new RigPose();
            com.hex.kagune.control.client.animation.OwlPose.body(body, true, grown, time);
            body.settle(1.0F);
            List<WingPreview.Tri> scene = new ArrayList<>(tiles(6.0));
            scene.addAll(owlScene(body, eyes,
                com.hex.kagune.control.client.animation.OwlPose.swing(1, time, 0.0, 0.0), com.hex.kagune.control.client.animation.OwlPose.swing(-1, time, 0.0, 0.0),
                time, 1.0));
            shots.add(new ArtPreview.Shot(f[0] + ", tick " + time, shown(body), ArtPreview.NO_WINGS, new double[] {3.8, 2.1, 5.0}, look, scene));
        }
        for (int i = 0; i < 4; i++) {
            double stride = i * Math.PI / 2.0;
            double time = 60.0 + i * 3.0;
            RigPose body = new RigPose();
            com.hex.kagune.control.client.animation.OwlPose.body(body, true, grown, time);
            // Her own legs and arms in her stride, as vanilla swings them.
            body.rightLegX = (float)(0.7 * Math.cos(stride));
            body.leftLegX = (float)(-0.7 * Math.cos(stride));
            body.rightArmX += (float)(-0.5 * Math.cos(stride));
            body.leftArmX += (float)(0.5 * Math.cos(stride));
            body.settle(1.0F);
            List<WingPreview.Tri> scene = new ArrayList<>(tiles(6.0));
            scene.addAll(owlScene(body, eyes,
                com.hex.kagune.control.client.animation.OwlPose.swing(1, time, 1.0, stride),
                com.hex.kagune.control.client.animation.OwlPose.swing(-1, time, 1.0, stride), time, 1.0));
            shots.add(new ArtPreview.Shot(String.format(java.util.Locale.ROOT, "running, stride %.0f deg", Math.toDegrees(stride)), shown(body), ArtPreview.NO_WINGS,
                new double[] {6.2, 1.8, 1.4}, look, scene));
        }
        ArtPreview.bodyScale = com.hex.kagune.control.client.animation.BodyFrame.SCALE;
        PoseRender.bulk = 1.0;
        try {
            ArtPreview.sheet(file, "The Owl alive: breathing, swaying, the blades flicking; running, the arms swinging, the blades lifted and lagging", framed(shots), 4,
                380, 360);
        } finally {
            ArtPreview.bodyScale = 1.0;
            PoseRender.bulk = 0.0;
        }
    }

    /**
     * 6.2.0: the Owl's moves as the game plays them: from her idle in it, {@code frames} ({move, age}, in order, each move begun when
     * it first appears), the giant arms carried through by {@code OwlArms} exactly as the drawing carries them; the body and both
     * arms at each frame wanted ({@code at}: indices into {@code frames} to keep).
     */
    static Object[][] owlFrames(Object[][] frames, float charge) {
        return owlFrames(frames, charge, 0);
    }

    static Object[][] owlFrames(Object[][] frames, float charge, int detail) {
        com.hex.kagune.control.client.animation.OwlArms arms = new com.hex.kagune.control.client.animation.OwlArms();
        Object[][] out = new Object[frames.length][];
        double time = 200.0;
        com.hex.kagune.control.pale.PaleMove was = com.hex.kagune.control.pale.PaleMove.NONE;
        RigPose fromPose = null;
        RigPose lastPose = null;
        long started = 0L;
        int index = 0;
        double age = 0.0;
        for (Object[] f : frames) {
            com.hex.kagune.control.pale.PaleMove move = (com.hex.kagune.control.pale.PaleMove)f[0];
            double until = (Double)f[1];
            if (move != was) {
                started++;
                fromPose = lastPose;
                age = 0.0;
                was = move;
            }
            RigPose body = null;
            com.hex.kagune.control.client.animation.OwlPose.Swing[] swung = null;
            for (; age <= until + 1.0E-9; age += 0.25) {
                time += 0.25;
                body = com.hex.kagune.control.client.animation.OwlMotions.body(move, age, detail, charge, fromPose);
                body.settle(1.0F);
                swung = arms.swing(move, started, detail, age, charge, time, com.hex.kagune.control.client.animation.OwlPose.swing(1, time, 0.0, 0.0),
                    com.hex.kagune.control.client.animation.OwlPose.swing(-1, time, 0.0, 0.0), body, 0.0F);
            }
            lastPose = body;
            out[index++] = new Object[] {body, swung[0], swung[1], time};
        }
        return out;
    }

    /**
     * 6.2.0, the Owl's Big Punch (Z in it): she sinks into the coil, the right giant arm rearing up behind her and the left one's
     * blade braced on the ground; let go, the last of the wind-up, the drive, the blow (her fist and the giant elbow into it together),
     * driven on through, the blade carried with it, held, eased back.
     */
    static void owlPunch(File file) throws Exception {
        com.hex.kagune.control.pale.PaleMove charge = com.hex.kagune.control.pale.PaleMove.OWL_PUNCH_CHARGE;
        com.hex.kagune.control.pale.PaleMove punch = com.hex.kagune.control.pale.PaleMove.OWL_PUNCH;
        Object[][] frames = {{charge, 4.0}, {charge, 18.0}, {charge, 32.0}, {punch, 1.25}, {punch, 3.25}, {punch, 4.0}, {punch, 5.0}, {punch, 9.0},
            {punch, 15.0}};
        String[] names = {"Z down: sinking, the arm rearing up", "charging: reared, the left blade braced", "full: trembling with it",
            "let go: the last of the wind-up", "the drive, over and down", "the blow: fist and elbow together", "driven on through",
            "held", "easing back"};
        Object[][] posed = owlFrames(frames, 1.0F);
        com.hex.kagune.control.kakugan.KakuganEyes eyes = com.hex.kagune.control.kakugan.KakuganEyes.DEFAULT
            .withGhoul(com.hex.kagune.control.kakugan.KakuganEyes.RIGHT);
        List<ArtPreview.Shot> shots = new ArrayList<>();
        for (int i = 0; i < frames.length; i++) {
            RigPose body = (RigPose)posed[i][0];
            List<WingPreview.Tri> scene = new ArrayList<>(tiles(7.0));
            if (i <= 5) {
                scene.addAll(dummy(2.3));
            }
            scene.addAll(owlScene(body, eyes, (com.hex.kagune.control.client.animation.OwlPose.Swing)posed[i][1],
                (com.hex.kagune.control.client.animation.OwlPose.Swing)posed[i][2], (Double)posed[i][3], 1.0));
            double[] camera = i == 2 || i == 5 ? new double[] {6.8, 1.9, 1.4} : new double[] {4.6, 2.6, 4.4};
            shots.add(new ArtPreview.Shot(names[i], shown(body), ArtPreview.NO_WINGS, camera, new double[] {0.0, 1.4, 0.9}, scene));
        }
        ArtPreview.bodyScale = com.hex.kagune.control.client.animation.BodyFrame.SCALE;
        PoseRender.bulk = 1.0;
        try {
            ArtPreview.sheet(file, "The Owl's Big Punch (Z): reared and braced as it charges; let go, the fist and the giant elbow into it together, the blade carried on it",
                framed(shots), 3, 420, 380);
        } finally {
            ArtPreview.bodyScale = 1.0;
            PoseRender.bulk = 0.0;
        }
    }

    /**
     * 6.2.0, the Owl's Speed Kick (V tapped in it): the dash (all but unseen in the game), the right giant arm's blade driven through
     * it, the left wound back and slashed across its neck, the blade drawn out as she turns away, the spinning heel, down again.
     */
    static void owlKick(File file) throws Exception {
        com.hex.kagune.control.pale.PaleMove kick = com.hex.kagune.control.pale.PaleMove.OWL_KICK;
        int dash = 3;
        double[] ages = {1.0, dash + 0.4, dash + 2.2, dash + 4.0, dash + 5.0, dash + 6.0, dash + 7.6, dash + 8.8, dash + 10.0, dash + 11.6, dash + 13.6,
            dash + 17.0};
        String[] names = {"the dash", "planted, the blade cocked over her head", "through it", "wound up for the slash", "the slash across its neck",
            "on through", "drawn out, turning away", "spinning, her back to it", "the heel", "on round", "down again", "the Owl's stance"};
        Object[][] frames = new Object[ages.length][];
        for (int i = 0; i < ages.length; i++) {
            frames[i] = new Object[] {kick, ages[i]};
        }
        Object[][] posed = owlFrames(frames, 0.0F, dash);
        com.hex.kagune.control.kakugan.KakuganEyes eyes = com.hex.kagune.control.kakugan.KakuganEyes.DEFAULT
            .withGhoul(com.hex.kagune.control.kakugan.KakuganEyes.RIGHT);
        List<ArtPreview.Shot> shots = new ArrayList<>();
        for (int i = 0; i < ages.length; i++) {
            RigPose body = (RigPose)posed[i][0];
            List<WingPreview.Tri> scene = new ArrayList<>(tiles(7.0));
            if (i >= 1 && i <= 8) {
                // (She steps in at it through the spin: here she stays put and it comes to her.)
                scene.addAll(dummy(com.hex.kagune.control.pale.OwlRules.KICK_STAND - com.hex.kagune.control.pale.OwlRules.kickStep(ages[i] - dash)));
            }
            scene.addAll(owlScene(body, eyes, (com.hex.kagune.control.client.animation.OwlPose.Swing)posed[i][1],
                (com.hex.kagune.control.client.animation.OwlPose.Swing)posed[i][2], (Double)posed[i][3], 1.0));
            double[] camera = new double[] {5.6, 2.8, 3.4};
            shots.add(new ArtPreview.Shot(names[i], shown(body), ArtPreview.NO_WINGS, camera, new double[] {0.0, 1.3, 1.0}, scene));
        }
        ArtPreview.bodyScale = com.hex.kagune.control.client.animation.BodyFrame.SCALE;
        PoseRender.bulk = 1.0;
        try {
            ArtPreview.sheet(file, "The Owl's Speed Kick (V tapped): skewered on the right blade, the left across its neck, the blade drawn out, the spinning heel",
                framed(shots), 4, 380, 360);
        } finally {
            ArtPreview.bodyScale = 1.0;
            PoseRender.bulk = 0.0;
        }
    }

    /**
     * 6.2.0, the Owl's Rend (its M1): the four cuts chained, each wound up and thrown with her body behind it: the right blade down
     * across, the left blade back across, both scissoring in front of her, both brought down overhead into the ground.
     */
    static void owlRend(File file) throws Exception {
        com.hex.kagune.control.pale.PaleMove rend = com.hex.kagune.control.pale.PaleMove.OWL_STRIKE;
        int[] stages = {0, 0, 1, 1, 2, 2, 3, 3, 3};
        double[] ages = {2.0, 4.5, 2.0, 4.5, 2.5, 5.5, 4.0, 7.2, 12.0};
        String[] names = {"1: wound up, the right blade raised", "1: down across", "2: the left drawn back", "2: back across",
            "3: opened wide", "3: the scissor", "4: up on her toes, both overhead", "4: brought down into the ground", "4: risen out of it"};
        com.hex.kagune.control.kakugan.KakuganEyes eyes = com.hex.kagune.control.kakugan.KakuganEyes.DEFAULT
            .withGhoul(com.hex.kagune.control.kakugan.KakuganEyes.RIGHT);
        List<ArtPreview.Shot> shots = new ArrayList<>();
        for (int i = 0; i < stages.length; i++) {
            // Each cut out of the last one's end (the chain), the arms carried through.
            List<Object[]> run = new ArrayList<>();
            for (int s = 0; s < stages[i]; s++) {
                run.add(new Object[] {rend, (double)com.hex.kagune.control.pale.OwlRules.REND_CHAIN[s], s});
            }
            run.add(new Object[] {rend, ages[i], stages[i]});
            Object[][] posed = owlChain(run.toArray(new Object[0][]));
            RigPose body = (RigPose)posed[posed.length - 1][0];
            List<WingPreview.Tri> scene = new ArrayList<>(tiles(7.0));
            scene.addAll(dummy(2.9));
            scene.addAll(owlScene(body, eyes,
                (com.hex.kagune.control.client.animation.OwlPose.Swing)posed[posed.length - 1][1],
                (com.hex.kagune.control.client.animation.OwlPose.Swing)posed[posed.length - 1][2], (Double)posed[posed.length - 1][3], 1.0));
            shots.add(new ArtPreview.Shot(names[i], shown(body), ArtPreview.NO_WINGS, new double[] {4.8, 2.8, 4.6}, new double[] {0.0, 1.2, 1.0}, scene));
        }
        ArtPreview.bodyScale = com.hex.kagune.control.client.animation.BodyFrame.SCALE;
        PoseRender.bulk = 1.0;
        try {
            ArtPreview.sheet(file, "The Owl's Rend (M1): four cuts of the giant blades, chained, her whole body behind each", framed(shots), 3, 420, 380);
        } finally {
            ArtPreview.bodyScale = 1.0;
            PoseRender.bulk = 0.0;
        }
    }

    /** 6.2.0, the Owl's Reaping Whirl (J tapped in it): coiled, round a whole turn with both blades out at full reach, on round, settled. */
    static void owlWhirl(File file) throws Exception {
        com.hex.kagune.control.pale.PaleMove whirl = com.hex.kagune.control.pale.PaleMove.OWL_WHIRL;
        double[] ages = {3.0, 6.0, 8.0, 10.0, 12.0, 8.0, 15.0, 19.0};
        String[] names = {"coiled up to her right", "into the turn", "half way round", "on round", "nearly all the way", "from above, mid-turn",
            "on round a hair", "settled"};
        com.hex.kagune.control.kakugan.KakuganEyes eyes = com.hex.kagune.control.kakugan.KakuganEyes.DEFAULT
            .withGhoul(com.hex.kagune.control.kakugan.KakuganEyes.RIGHT);
        List<ArtPreview.Shot> shots = new ArrayList<>();
        for (int i = 0; i < ages.length; i++) {
            Object[][] posed = owlChain(new Object[][] {{whirl, ages[i], 0}});
            RigPose body = (RigPose)posed[0][0];
            List<WingPreview.Tri> scene = new ArrayList<>(tiles(7.0));
            scene.addAll(owlScene(body, eyes, (com.hex.kagune.control.client.animation.OwlPose.Swing)posed[0][1],
                (com.hex.kagune.control.client.animation.OwlPose.Swing)posed[0][2], (Double)posed[0][3], 1.0));
            double[] camera = i == 5 ? new double[] {0.4, 8.5, 1.0} : new double[] {5.2, 3.0, 5.0};
            shots.add(new ArtPreview.Shot(names[i], shown(body), ArtPreview.NO_WINGS, camera, new double[] {0.0, 1.0, 0.0}, scene));
        }
        ArtPreview.bodyScale = com.hex.kagune.control.client.animation.BodyFrame.SCALE;
        PoseRender.bulk = 1.0;
        try {
            ArtPreview.sheet(file, "The Owl's Reaping Whirl (J tapped): coiled, then a whole turn with both giant blades out, reaping all round her", framed(shots), 4,
                380, 360);
        } finally {
            ArtPreview.bodyScale = 1.0;
            PoseRender.bulk = 0.0;
        }
    }

    /**
     * 6.2.0, the Owl's Pounce (M2 in it): crouched with the blades reared up behind her, the bound, tucked in the air with them over
     * her, touching down, both blades driven into the ground ahead, up again. (Drawn where she stands: in the game she is up there.)
     */
    static void owlPounce(File file) throws Exception {
        com.hex.kagune.control.pale.PaleMove pounce = com.hex.kagune.control.pale.PaleMove.OWL_LEAP;
        double landed = 16.0;
        double[] ages = {3.0, 4.5, 10.0, landed, landed + com.hex.kagune.control.pale.OwlRules.LEAP_SLAM_AFTER, landed + 5.0, landed + 9.0,
            landed + 13.0};
        String[] names = {"sunk down, the blades reared", "the bound", "tucked in the air, the blades over her", "touching down",
            "both blades into the ground", "held there", "rising", "the Owl's stance"};
        com.hex.kagune.control.kakugan.KakuganEyes eyes = com.hex.kagune.control.kakugan.KakuganEyes.DEFAULT
            .withGhoul(com.hex.kagune.control.kakugan.KakuganEyes.RIGHT);
        List<ArtPreview.Shot> shots = new ArrayList<>();
        for (int i = 0; i < ages.length; i++) {
            com.hex.kagune.control.client.animation.OwlArms arms = new com.hex.kagune.control.client.animation.OwlArms();
            RigPose body = null;
            com.hex.kagune.control.client.animation.OwlPose.Swing[] swung = null;
            double time = 500.0;
            for (double age = 0.0; age <= ages[i] + 1.0E-9; age += 0.25) {
                time += 0.25;
                body = com.hex.kagune.control.client.animation.OwlMotions.body(pounce, age, 0, (float)landed, null);
                body.settle(1.0F);
                swung = arms.swing(pounce, 1L, 0, age, (float)landed, time, com.hex.kagune.control.client.animation.OwlPose.swing(1, time, 0.0, 0.0),
                    com.hex.kagune.control.client.animation.OwlPose.swing(-1, time, 0.0, 0.0), body, 0.0F);
            }
            List<WingPreview.Tri> scene = new ArrayList<>(tiles(7.0));
            if (i >= 3) {
                scene.addAll(dummy(com.hex.kagune.control.pale.OwlRules.LEAP_SLAM_AHEAD + 0.6));
            }
            scene.addAll(owlScene(body, eyes, swung[0], swung[1], time, 1.0));
            shots.add(new ArtPreview.Shot(names[i], shown(body), ArtPreview.NO_WINGS, new double[] {5.2, 2.8, 4.4}, new double[] {0.0, 1.1, 0.8}, scene));
        }
        ArtPreview.bodyScale = com.hex.kagune.control.client.animation.BodyFrame.SCALE;
        PoseRender.bulk = 1.0;
        try {
            ArtPreview.sheet(file, "The Owl's Pounce (M2): reared, the bound, the blades over her in the air, driven into the ground as she lands", framed(shots), 4,
                380, 360);
        } finally {
            ArtPreview.bodyScale = 1.0;
            PoseRender.bulk = 0.0;
        }
    }

    /**
     * 6.2.0, the Owl's Fissure (V held in it): reared up onto her toes, the giant arms raised high, the blades hooked over her,
     * trembling at full charge; let go, the last of the wind-up, both blades brought down on one place ahead, held there while the
     * ground splits on from it.
     */
    static void owlFissure(File file) throws Exception {
        com.hex.kagune.control.pale.PaleMove charge = com.hex.kagune.control.pale.PaleMove.OWL_FISSURE_CHARGE;
        com.hex.kagune.control.pale.PaleMove slam = com.hex.kagune.control.pale.PaleMove.OWL_FISSURE;
        double held = com.hex.kagune.control.pale.PaleRules.HAMMER_GROW_TICKS + 4.0;
        Object[][] steps = {{charge, 4.0}, {charge, held}, {charge, held}, {slam, 2.0}, {slam, (double)com.hex.kagune.control.pale.OwlRules.FISSURE_IMPACT},
            {slam, 10.0}, {slam, 18.0}};
        String[] names = {"V down: rearing up", "full: reared, the blades high over her", "full, from behind", "let go: the last of it",
            "both blades down on one place", "held there as the ground splits on", "rising"};
        double[][] cameras = {{4.8, 2.6, 4.6}, {4.8, 2.6, 4.6}, {-4.4, 3.0, -5.0}, {6.6, 2.0, 1.6}, {6.6, 2.0, 1.6}, {4.8, 2.6, 4.6}, {4.8, 2.6, 4.6}};
        com.hex.kagune.control.kakugan.KakuganEyes eyes = com.hex.kagune.control.kakugan.KakuganEyes.DEFAULT
            .withGhoul(com.hex.kagune.control.kakugan.KakuganEyes.RIGHT);
        List<ArtPreview.Shot> shots = new ArrayList<>();
        for (int i = 0; i < steps.length; i++) {
            Object[][] run = steps[i][0] == slam ? new Object[][] {{charge, held, 0}, {slam, steps[i][1], 0}} : new Object[][] {{charge, steps[i][1], 0}};
            Object[][] posed = owlChain(run);
            Object[] last = posed[posed.length - 1];
            RigPose body = (RigPose)last[0];
            List<WingPreview.Tri> scene = new ArrayList<>(tiles(9.0));
            scene.addAll(owlScene(body, eyes, (com.hex.kagune.control.client.animation.OwlPose.Swing)last[1],
                (com.hex.kagune.control.client.animation.OwlPose.Swing)last[2], (Double)last[3], 1.0));
            shots.add(new ArtPreview.Shot(names[i], shown(body), ArtPreview.NO_WINGS, cameras[i], new double[] {0.0, 1.3, 0.8}, scene));
        }
        ArtPreview.bodyScale = com.hex.kagune.control.client.animation.BodyFrame.SCALE;
        PoseRender.bulk = 1.0;
        try {
            ArtPreview.sheet(file, "The Owl's Fissure (V held): reared, the blades raised high over her; let go, both brought down on one place, the ground split on",
                framed(shots), 4, 380, 360);
        } finally {
            ArtPreview.bodyScale = 1.0;
            PoseRender.bulk = 0.0;
        }
    }

    /**
     * The Owl's moves one after another as the game chains them ({move, ticks it runs, detail}: each begun out of the last as drawn),
     * the giant arms carried through by {@code OwlArms}; the body and arms at the end of each.
     */
    static Object[][] owlChain(Object[][] moves) {
        com.hex.kagune.control.client.animation.OwlArms arms = new com.hex.kagune.control.client.animation.OwlArms();
        Object[][] out = new Object[moves.length][];
        double time = 300.0;
        RigPose last = null;
        for (int m = 0; m < moves.length; m++) {
            com.hex.kagune.control.pale.PaleMove move = (com.hex.kagune.control.pale.PaleMove)moves[m][0];
            double until = (Double)moves[m][1];
            int detail = (Integer)moves[m][2];
            RigPose from = last;
            RigPose body = null;
            com.hex.kagune.control.client.animation.OwlPose.Swing[] swung = null;
            for (double age = 0.0; age <= until + 1.0E-9; age += 0.25) {
                time += 0.25;
                body = com.hex.kagune.control.client.animation.OwlMotions.body(move, age, detail, 0.0F, from);
                body.settle(1.0F);
                swung = arms.swing(move, m + 1L, detail, age, 0.0F, time, com.hex.kagune.control.client.animation.OwlPose.swing(1, time, 0.0, 0.0),
                    com.hex.kagune.control.client.animation.OwlPose.swing(-1, time, 0.0, 0.0), body, 0.0F);
            }
            last = body;
            out[m] = new Object[] {body, swung[0], swung[1], time};
        }
        return out;
    }

    /** 6.2.0: the Owl's bone spikes out of {@code body}'s back, {@code age} ticks in (let go at {@code letGo}), as triangles. */
    static List<WingPreview.Tri> spikeTris(RigPose body, int seed, double age, double letGo) {
        List<WingPreview.Tri> tris = new ArrayList<>();
        for (com.hex.kagune.control.pale.OwlSpikes.Spike spike : com.hex.kagune.control.pale.OwlSpikes.of(seed)) {
            double out = com.hex.kagune.control.pale.OwlSpikes.out(spike, age, letGo);
            double[][] placed = com.hex.kagune.control.client.animation.OwlPose.spike(body, spike, 0.0);
            com.hex.kagune.control.pale.OwlSpikes.mesh((a, b, c, d, n, ca, cb, cc, cd) -> {
                tris.add(new WingPreview.Tri(new double[][] {a, b, c}, new int[] {ca, cb, cc}, n, true));
                tris.add(new WingPreview.Tri(new double[][] {a, c, d}, new int[] {ca, cc, cd}, n, true));
            }, spike, placed[0], placed[1], new double[] {0.0, 1.0, 0.0}, out);
        }
        return tris;
    }

    /**
     * 6.2.0, the Owl's bone spikes (J held in it): hunched over, the spikes bursting out of her back one after another all round her,
     * all out (from three sides and above), let go and drawn back in.
     */
    static void owlSpikes(File file) throws Exception {
        com.hex.kagune.control.pale.PaleMove move = com.hex.kagune.control.pale.PaleMove.OWL_SPIKES;
        int seed = 20261019;
        com.hex.kagune.control.pale.OwlSpikes.Spike[] spikes = com.hex.kagune.control.pale.OwlSpikes.of(seed);
        double allOut = com.hex.kagune.control.pale.OwlSpikes.allOut(spikes);
        double letGo = allOut + 10.0;
        double first = com.hex.kagune.control.pale.OwlSpikes.FIRST;
        double[] ages = {first - 0.5, first + 1.0, first + 3.0, allOut, allOut + 4.0, allOut + 4.0, allOut + 4.0, letGo + 2.5, letGo + 6.0};
        String[] names = {"hunched over", "the first bursting out of her", "more, one after another", "all out", "held: from behind",
            "held: from the side", "held: from above", "let go: drawn back in", "nearly in"};
        double[][] cameras = {{4.6, 2.6, 4.4}, {4.6, 2.6, 4.4}, {4.6, 2.6, 4.4}, {5.6, 3.6, 5.6}, {-4.8, 3.8, -5.6}, {7.6, 2.4, 0.6}, {0.6, 9.0, 1.2},
            {5.6, 3.6, 5.6}, {4.6, 2.6, 4.4}};
        Object[][] frames = new Object[ages.length][];
        for (int i = 0; i < ages.length; i++) {
            frames[i] = new Object[] {move, ages[i]};
        }
        // (Each frame its own run from the start: the views of the held burst all at the same moment.)
        com.hex.kagune.control.kakugan.KakuganEyes eyes = com.hex.kagune.control.kakugan.KakuganEyes.DEFAULT
            .withGhoul(com.hex.kagune.control.kakugan.KakuganEyes.RIGHT);
        List<ArtPreview.Shot> shots = new ArrayList<>();
        for (int i = 0; i < ages.length; i++) {
            Object[][] posed = owlFrames(new Object[][] {frames[i]}, (float)letGo, seed);
            RigPose body = (RigPose)posed[0][0];
            List<WingPreview.Tri> scene = new ArrayList<>(tiles(7.0));
            scene.addAll(owlScene(body, eyes, (com.hex.kagune.control.client.animation.OwlPose.Swing)posed[0][1],
                (com.hex.kagune.control.client.animation.OwlPose.Swing)posed[0][2], (Double)posed[0][3], 1.0));
            scene.addAll(spikeTris(body, seed, ages[i], letGo));
            shots.add(new ArtPreview.Shot(names[i], shown(body), ArtPreview.NO_WINGS, cameras[i], new double[] {0.0, 1.1, 0.0}, scene));
        }
        ArtPreview.bodyScale = com.hex.kagune.control.client.animation.BodyFrame.SCALE;
        PoseRender.bulk = 1.0;
        try {
            ArtPreview.sheet(file, "The Owl's bone spikes (J held): hunched over, they burst out of her back all round her one after another; let go, drawn back in",
                framed(shots), 3, 420, 380);
        } finally {
            ArtPreview.bodyScale = 1.0;
            PoseRender.bulk = 0.0;
        }
    }

    /**
     * The Owl in the game (K), 6.2.4: her body as it grows on her (doubled over, the burst: the crest ignites at her nape and the four
     * limbs tear up out of her back folded tight and unfold down into her idle, the robe spilling down, the mask closing over her face
     * from the eye, the crown and claws last), the Owl all out, and turning back (folding up and drawn in).
     */
    static void owlTransform(File file) throws Exception {
        List<ArtPreview.Shot> shots = new ArrayList<>();
        com.hex.kagune.control.kakugan.KakuganEyes eyes = com.hex.kagune.control.kakugan.KakuganEyes.DEFAULT
            .withGhoul(com.hex.kagune.control.kakugan.KakuganEyes.RIGHT);
        double[] front = {3.6, 2.0, 4.8};
        double[] look = {0.0, 1.1, 0.0};
        Object[][] frames = {
            {"K: it starts to tear out of her", true, 3.0, front},
            {"doubled over", true, 8.0, front},
            {"the burst: the crest ignites, the limbs tear up out of her", true, (double)com.hex.kagune.control.pale.OwlRules.BURST + 2.0, front},
            {"the burst, from behind", true, (double)com.hex.kagune.control.pale.OwlRules.BURST + 3.0, new double[] {-3.4, 2.4, -4.6}},
            {"the roar: the limbs unfolding, the mask closing", true, 15.0, front},
            {"the blades out, the robe spilling down", true, 20.0, front},
            {"the Owl", true, 60.0, front},
            {"the Owl, from behind", true, 60.0, new double[] {-3.6, 2.4, -4.8}},
            {"the Owl, side", true, 60.0, new double[] {5.8, 1.7, 0.6}},
            {"K again: drawing in", false, 4.0, front},
            {"bent over as it goes", false, 10.0, front},
            {"herself again", false, (double)com.hex.kagune.control.pale.OwlRules.SHED_TICKS, front}};
        ArtPreview.bodyScale = com.hex.kagune.control.client.animation.BodyFrame.SCALE;
        try {
            for (Object[] f : frames) {
                boolean on = (Boolean)f[1];
                double age = (Double)f[2];
                RigPose body = new RigPose();
                com.hex.kagune.control.client.animation.OwlPose.body(body, on, age, age);
                body.settle(1.0F);
                double grown = com.hex.kagune.control.pale.OwlRules.grown(on, age);
                List<WingPreview.Tri> scene = new ArrayList<>(tiles(6.0));
                scene.addAll(owlScene(body, eyes,
                    com.hex.kagune.control.client.animation.OwlPose.unfurl(com.hex.kagune.control.client.animation.OwlPose.swing(1, age, 0.0, 0.0), 1, grown),
                    com.hex.kagune.control.client.animation.OwlPose.unfurl(com.hex.kagune.control.client.animation.OwlPose.swing(-1, age, 0.0, 0.0), -1, grown),
                    age, grown));
                shots.add(new ArtPreview.Shot((String)f[0], shown(body), ArtPreview.NO_WINGS, (double[])f[3], look, scene));
            }
            ArtPreview.sheet(file, "The Owl (K): the crest ignites, four limbs tear up out of her back and unfold, the mask closes over her face; K again, it folds away",
                framed(shots), 4, 360, 360);
        } finally {
            ArtPreview.bodyScale = 1.0;
        }
    }

    // ------------------------------------------------------------------ moves

    /** A stand-in target 1.7 blocks ahead of her: a plain grey figure, to judge reach and where each blow lands. */
    /** 6.2.0: one of the Owl's Watch This lances out of {@code body} at {@code tip}, as triangles. */
    static List<WingPreview.Tri> lanceTris(RigPose body, int i, com.hex.kagune.control.pale.OwlLances.Tip tip, double time) {
        List<WingPreview.Tri> tris = new ArrayList<>();
        com.hex.kagune.control.pale.OwlLances.mesh((a, b, c, d, n, ca, cb, cc, cd) -> {
            tris.add(new WingPreview.Tri(new double[][] {a, b, c}, new int[] {ca, cb, cc}, n, true));
            tris.add(new WingPreview.Tri(new double[][] {a, c, d}, new int[] {ca, cc, cd}, n, true));
        }, com.hex.kagune.control.client.animation.OwlLancePose.path(body, i, tip), tip.dir(), tip.grown(), 41 + i * 7, time);
        return tris;
    }

    /** 6.2.0: one of the Owl's volley spears with its point at {@code point}, going {@code way}, as triangles. */
    static List<WingPreview.Tri> shardTris(double[] point, double[] way, int seed) {
        List<WingPreview.Tri> tris = new ArrayList<>();
        double l = Math.sqrt(way[0] * way[0] + way[1] * way[1] + way[2] * way[2]);
        double[] d = {way[0] / l, way[1] / l, way[2] / l};
        double[] tail = {point[0] - d[0] * com.hex.kagune.control.pale.OwlShards.LENGTH, point[1] - d[1] * com.hex.kagune.control.pale.OwlShards.LENGTH,
            point[2] - d[2] * com.hex.kagune.control.pale.OwlShards.LENGTH};
        com.hex.kagune.control.pale.OwlShards.mesh((a, b, c, e, n, ca, cb, cc, ce) -> {
            tris.add(new WingPreview.Tri(new double[][] {a, b, c}, new int[] {ca, cb, cc}, n, true));
            tris.add(new WingPreview.Tri(new double[][] {a, c, e}, new int[] {ca, cc, ce}, n, true));
        }, tail, d, 1.0, seed);
        return tris;
    }

    /**
     * 6.2.0, the Owl's blade volley (N in it): tapped, the right arm rearing back and whipping through, its fan of spears ripped off
     * the blade; the left arm's throw with the first fan in flight; a spear through a dummy carrying it on; the dummy pinned to a
     * wall; held, the storm.
     */
    static void owlVolley(File file) throws Exception {
        com.hex.kagune.control.kakugan.KakuganEyes eyes = com.hex.kagune.control.kakugan.KakuganEyes.DEFAULT
            .withGhoul(com.hex.kagune.control.kakugan.KakuganEyes.RIGHT);
        com.hex.kagune.control.pale.PaleMove move = com.hex.kagune.control.pale.PaleMove.OWL_VOLLEY;
        double[] target = {0.0, 1.0, 13.0};
        double wall = 16.0;
        String[] names = {"N tapped: the right arm rears back", "whipped through: its spears ripped off the blade", "the left's throw, the first fan in flight",
            "through the dummy, carrying it on", "pinned to the wall", "N held: the storm, turn about", "the storm: the left", "after: stuck, crumbling soon"};
        double[] ages = {3.5, 5.3, 8.2, 9.0, 14.0, 17.0, 18.5, 30.0};
        int[] held = {0, 0, 0, 0, 0, 1, 1, 0};
        double[][] cameras = {{4.2, 3.0, 3.6}, {4.2, 3.0, 3.6}, {5.5, 4.5, -6.5}, {9.5, 5.0, 4.0}, {9.5, 5.0, 8.0}, {5.0, 3.6, -4.5}, {-5.0, 3.6, -4.5},
            {3.5, 2.8, 7.5}};
        double[][] looks = {{0.0, 1.8, 0.5}, {0.0, 1.9, 1.0}, {0.0, 1.8, 4.0}, {0.0, 1.5, 9.0}, {0.0, 1.4, 13.0}, {0.0, 2.0, 4.0}, {0.0, 2.0, 4.0},
            {0.0, 1.6, 15.5}};
        List<ArtPreview.Shot> shots = new ArrayList<>();
        for (int f = 0; f < names.length; f++) {
            boolean storm = held[f] == 1;
            double bodyAge = Math.min(ages[f], com.hex.kagune.control.pale.OwlShards.ends(storm, 0.0F) - 0.25);
            Object[][] posed = owlFrames(new Object[][] {{move, bodyAge}}, 0.0F, held[f]);
            RigPose body = (RigPose)posed[0][0];
            double time = (Double)posed[0][3];
            List<WingPreview.Tri> scene = new ArrayList<>(tiles(18.0));
            scene.addAll(owlScene(body, eyes, (com.hex.kagune.control.client.animation.OwlPose.Swing)posed[0][1],
                (com.hex.kagune.control.client.animation.OwlPose.Swing)posed[0][2], time, 1.0));
            List<WingPreview.Tri> wallTris = new ArrayList<>();
            box(wallTris, new double[] {-3.0, 0.0, wall}, new double[] {3.0, 3.5, wall + 1.0}, 0xFF7A7672);
            scene.addAll(wallTris);
            boolean dummyCarried = false;
            double[] dummyAt = {target[0], 0.0, target[2]};
            int made = com.hex.kagune.control.pale.OwlShards.throwsMade(storm, 0.0F);
            for (int k = 0; k < made; k++) {
                double t = ages[f] - com.hex.kagune.control.pale.OwlShards.throwAt(k);
                if (t < 0.0) {
                    continue;
                }
                double[] from = com.hex.kagune.control.pale.OwlShards.release(k);
                for (int j = 0; j < com.hex.kagune.control.pale.OwlShards.spears(k); j++) {
                    double[] d = com.hex.kagune.control.pale.OwlShards.direction(from, target, k, j);
                    double[] v = {d[0] * com.hex.kagune.control.pale.OwlShards.SPEED, d[1] * com.hex.kagune.control.pale.OwlShards.SPEED,
                        d[2] * com.hex.kagune.control.pale.OwlShards.SPEED};
                    double[] point = com.hex.kagune.control.pale.OwlShards.at(from, v, t);
                    double[] way = com.hex.kagune.control.pale.OwlShards.velocity(v, t);
                    boolean centre = k == 0 && j == 1;
                    if (point[2] >= wall - 0.05) {
                        // Stuck in the wall where its flight crosses it (the first throw's middle one through the dummy, pinning it there).
                        double struck = (wall - 0.05 - from[2]) / Math.max(1.0E-6, v[2]);
                        point = com.hex.kagune.control.pale.OwlShards.at(from, v, struck);
                        way = com.hex.kagune.control.pale.OwlShards.velocity(v, struck);
                        if (centre) {
                            dummyAt = new double[] {point[0], point[1] - 1.0, point[2] - 0.45};
                            dummyCarried = true;
                        }
                    } else if (point[1] < 0.0) {
                        continue;
                    } else if (centre && point[2] > target[2]) {
                        dummyAt = new double[] {point[0] - way[0] * 0.12, point[1] - 1.0, point[2] - 0.35};
                        dummyCarried = true;
                    }
                    if (t > com.hex.kagune.control.pale.OwlShards.LIFE && point[2] < wall - 0.1) {
                        continue;
                    }
                    scene.addAll(shardTris(point, way, k * 7 + j));
                }
            }
            if (f >= 2 || dummyCarried) {
                scene.addAll(dummyAt(dummyAt));
            }
            shots.add(new ArtPreview.Shot(names[f], shown(body), ArtPreview.NO_WINGS, cameras[f], looks[f], scene));
        }
        ArtPreview.bodyScale = com.hex.kagune.control.client.animation.BodyFrame.SCALE;
        PoseRender.bulk = 1.0;
        try {
            ArtPreview.sheet(file, "The Owl's blade volley (N): bone spears ripped off the giant blades and hurled, running through and pinning; held, a storm",
                framed(shots), 4, 360, 330);
        } finally {
            ArtPreview.bodyScale = 1.0;
            PoseRender.bulk = 0.0;
        }
    }

    /** A dummy (two blocks tall) standing with its feet at {@code feet}. */
    static List<WingPreview.Tri> dummyAt(double[] feet) {
        List<WingPreview.Tri> tris = new ArrayList<>();
        double x = feet[0];
        double y = feet[1];
        double z = feet[2];
        box(tris, new double[] {x - 0.25, y + 0.75, z - 0.125}, new double[] {x + 0.25, y + 1.5, z + 0.125}, 0xFF6E7378);
        box(tris, new double[] {x - 0.25, y + 1.5, z - 0.25}, new double[] {x + 0.25, y + 2.0, z + 0.25}, 0xFF7C8186);
        box(tris, new double[] {x - 0.24, y, z - 0.12}, new double[] {x + 0.24, y + 0.75, z + 0.12}, 0xFF5A5F64);
        return tris;
    }

    /**
     * 6.2.0, the Owl's Watch This (B in it): hunched, reared up as six lances tear out of her back into a crown over her (from the
     * front and behind); then one of them on a dummy ahead and to her right: rearing high over it, the dive through it into the
     * ground, pinned, hoisted up over her, slammed down on her far side, torn free.
     */
    static void owlWatch(File file) throws Exception {
        com.hex.kagune.control.pale.PaleMove move = com.hex.kagune.control.pale.PaleMove.OWL_WATCH;
        int at = com.hex.kagune.control.pale.OwlRules.LANCES_ERUPT_AT;
        com.hex.kagune.control.kakugan.KakuganEyes eyes = com.hex.kagune.control.kakugan.KakuganEyes.DEFAULT
            .withGhoul(com.hex.kagune.control.kakugan.KakuganEyes.RIGHT);
        // The dummy: four blocks out ahead and to her right, where the first lance hunts; its chest a little over a block up.
        double bearing = com.hex.kagune.control.pale.OwlLances.bearing(0);
        double chestUp = 2.0 * com.hex.kagune.control.pale.OwlLances.CHEST;
        double[] chest = {Math.sin(bearing) * 4.0, chestUp, Math.cos(bearing) * 4.0};
        double[] slam = com.hex.kagune.control.pale.OwlLances.slamPoint(chest, chestUp, 0.0);
        int dive = com.hex.kagune.control.pale.OwlLances.diveTicks(4.5);
        com.hex.kagune.control.pale.OwlLances.Phase[] phases = {null, null, null, null,
            com.hex.kagune.control.pale.OwlLances.Phase.STRIKE, com.hex.kagune.control.pale.OwlLances.Phase.STRIKE, com.hex.kagune.control.pale.OwlLances.Phase.PIN,
            com.hex.kagune.control.pale.OwlLances.Phase.HOIST, com.hex.kagune.control.pale.OwlLances.Phase.HOIST, com.hex.kagune.control.pale.OwlLances.Phase.RECOVER,
            com.hex.kagune.control.pale.OwlLances.Phase.IN};
        double[] phaseAges = {0.0, 0.0, 0.0, 0.0, com.hex.kagune.control.pale.OwlRules.LANCE_REAR, com.hex.kagune.control.pale.OwlLances.strikeTicks(dive), 3.0,
            6.0, com.hex.kagune.control.pale.OwlRules.LANCE_HOIST, 3.0, 6.0};
        double[] bodyAges = {3.5, at + 2.5, 18.0, 18.0, 30.0, 30.0, 30.0, 30.0, 30.0, 30.0, 30.0};
        String[] names = {"B: hunched, gathering herself", "reared up: the lances tearing out of her", "the crown, hunting", "the crown, from behind",
            "one rears up high over what it hunts", "the dive: down through it into the ground", "pinned", "hoisted up over her on the barbs",
            "slammed down on her far side", "torn free, back to the crown", "ten seconds on: drawn back in"};
        double[][] cameras = {{4.6, 2.6, 4.4}, {4.6, 3.0, 4.6}, {5.4, 4.2, 5.2}, {-4.8, 4.4, -5.6}, {-3.2, 3.6, 7.4}, {-3.2, 3.6, 7.4}, {-3.2, 3.6, 7.4},
            {7.4, 4.2, -6.6}, {6.8, 3.6, -7.6}, {6.8, 3.6, -7.6}, {5.4, 4.2, 5.2}};
        double[][] looks = {{0.0, 1.1, 0.0}, {0.0, 1.6, 0.0}, {0.0, 2.0, 0.0}, {0.0, 2.0, 0.0}, {1.5, 1.6, 1.5}, {1.5, 1.6, 1.5}, {1.5, 1.6, 1.5},
            {0.0, 2.6, 0.0}, {-1.0, 1.4, -1.0}, {-1.0, 1.4, -1.0}, {0.0, 2.0, 0.0}};
        List<ArtPreview.Shot> shots = new ArrayList<>();
        for (int f = 0; f < names.length; f++) {
            double bodyAge = Math.min(bodyAges[f], com.hex.kagune.control.pale.OwlRules.LANCES_POSE_TICKS);
            Object[][] posed = owlFrames(new Object[][] {{move, bodyAge}}, 0.0F);
            RigPose body = (RigPose)posed[0][0];
            double time = (Double)posed[0][3];
            List<WingPreview.Tri> scene = new ArrayList<>(tiles(8.0));
            scene.addAll(owlScene(body, eyes, (com.hex.kagune.control.client.animation.OwlPose.Swing)posed[0][1],
                (com.hex.kagune.control.client.animation.OwlPose.Swing)posed[0][2], time, 1.0));
            double[] where = null;
            for (int i = 0; i < com.hex.kagune.control.pale.OwlLances.COUNT; i++) {
                com.hex.kagune.control.pale.OwlLances.Tip tip;
                if (f == 0) {
                    continue;
                } else if (f == 1) {
                    tip = com.hex.kagune.control.pale.OwlLances.tip(i, com.hex.kagune.control.pale.OwlLances.Phase.OUT, bodyAges[f] - at, 0, null, 1.0, null, time);
                } else if (f == 10) {
                    tip = com.hex.kagune.control.pale.OwlLances.tip(i, com.hex.kagune.control.pale.OwlLances.Phase.IN, phaseAges[f], 0, null, 1.0, null, time);
                } else if (i == 0 && phases[f] != null) {
                    com.hex.kagune.control.pale.OwlLances.Phase phase = phases[f];
                    double[] held = chest;
                    double[] from = null;
                    if (phase == com.hex.kagune.control.pale.OwlLances.Phase.HOIST) {
                        held = com.hex.kagune.control.pale.OwlLances.hoist(com.hex.kagune.control.pale.OwlLances.hoistU(phaseAges[f]), chest, slam);
                    } else if (phase == com.hex.kagune.control.pale.OwlLances.Phase.RECOVER) {
                        held = null;
                        from = new double[] {slam[0], 0.0, slam[2]};
                    }
                    where = held;
                    tip = com.hex.kagune.control.pale.OwlLances.tip(i, phase, phaseAges[f], dive, held, chestUp, from, time);
                } else {
                    tip = com.hex.kagune.control.pale.OwlLances.crown(i, time);
                }
                scene.addAll(lanceTris(body, i, tip, time));
            }
            if (f >= 4 && f <= 9) {
                double[] c = where == null ? (f == 9 ? new double[] {slam[0], chestUp, slam[2]} : chest) : where;
                scene.addAll(dummyAt(new double[] {c[0], c[1] - chestUp, c[2]}));
            }
            shots.add(new ArtPreview.Shot(names[f], shown(body), ArtPreview.NO_WINGS, cameras[f], looks[f], scene));
        }
        ArtPreview.bodyScale = com.hex.kagune.control.client.animation.BodyFrame.SCALE;
        PoseRender.bulk = 1.0;
        try {
            ArtPreview.sheet(file, "The Owl's Watch This (B): six lances tear out of her back and hunt on their own: down through it from above, hoisted over her, slammed",
                framed(shots), 4, 360, 330);
        } finally {
            ArtPreview.bodyScale = 1.0;
            PoseRender.bulk = 0.0;
        }
    }

    static List<WingPreview.Tri> dummy(double z) {
        List<WingPreview.Tri> tris = new ArrayList<>();
        box(tris, new double[] {-0.25, 0.75, z - 0.125}, new double[] {0.25, 1.5, z + 0.125}, 0xFF6E7378);
        box(tris, new double[] {-0.25, 1.5, z - 0.25}, new double[] {0.25, 2.0, z + 0.25}, 0xFF7C8186);
        box(tris, new double[] {-0.24, 0.0, z - 0.12}, new double[] {0.24, 0.75, z + 0.12}, 0xFF5A5F64);
        return tris;
    }

    static void box(List<WingPreview.Tri> tris, double[] min, double[] max, int argb) {
        double[][] c = new double[8][];
        for (int i = 0; i < 8; i++) {
            c[i] = new double[] {(i & 1) == 0 ? min[0] : max[0], (i & 2) == 0 ? min[1] : max[1], (i & 4) == 0 ? min[2] : max[2]};
        }
        int[][] faces = {{0, 2, 6, 4}, {1, 5, 7, 3}, {0, 4, 5, 1}, {2, 3, 7, 6}, {0, 1, 3, 2}, {4, 6, 7, 5}};
        double[][] normals = {{-1, 0, 0}, {1, 0, 0}, {0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}};
        for (int f = 0; f < 6; f++) {
            int[] q = faces[f];
            int[] colours = {argb, argb, argb};
            tris.add(new WingPreview.Tri(new double[][] {c[q[0]], c[q[1]], c[q[2]]}, colours, normals[f], true));
            tris.add(new WingPreview.Tri(new double[][] {c[q[0]], c[q[2]], c[q[3]]}, colours, normals[f], true));
        }
    }

    static final double DUMMY_Z = 1.7;

    /** Every strike of the speed blitz key by key: coiled, halfway through the drive, landing, overshooting, holding. */
    static void blitz(File file) throws Exception {
        List<ArtPreview.Shot> shots = new ArrayList<>();
        double[] target = {0.0, 0.95, 0.75};
        for (com.hex.kagune.control.pale.PaleStrike strike : com.hex.kagune.control.pale.PaleStrike.values()) {
            int i = strike.impact;
            double[] ages = {i - 1.2, i - 0.45, i, i + 0.55, i + 1.6};
            String[] names = {"coiled", "driving", "IMPACT", "overshoot", "hold"};
            // Seen from her open side, three quarters in front, the target in view.
            double side = strike.right ? 1.0 : -1.0;
            double[] eye = {3.0 * side, 2.1, 3.0};
            for (int k = 0; k < ages.length; k++) {
                RigPose p = com.hex.kagune.control.client.animation.PaleMotions.strike(strike, Math.max(0.0, ages[k]), null);
                p.settle(1.0F);
                String name = strike.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
                shots.add(new ArtPreview.Shot(name + ": " + names[k], p, ArtPreview.NO_WINGS, eye, target, dummy(DUMMY_Z)));
            }
        }
        ArtPreview.sheet(file, "Pale's speed blitz: every strike coiled, driving, landing (the server's impact tick), overshooting, holding", shots, 5,
            300, 330);
    }

    /** The whole blitz chained as the server chains it (each strike from the last one's chain tick), as an animated GIF at 30 fps. */
    static void blitzGif(File file, double[] eye, String label) throws Exception {
        List<java.awt.image.BufferedImage> frames = new ArrayList<>();
        double fps = 30.0;
        RigPose shown = null;
        com.hex.kagune.control.pale.PaleStrike[] all = com.hex.kagune.control.pale.PaleStrike.values();
        // A beat in the guard first.
        for (int f = 0; f < 10; f++) {
            frames.add(ArtPreview.render(new ArtPreview.Shot(label, com.hex.kagune.control.client.animation.PaleMotions.guard(), ArtPreview.NO_WINGS, eye,
                new double[] {0.0, 1.0, 0.6}, dummy(DUMMY_Z)), 420, 360));
        }
        for (int s = 0; s < all.length; s++) {
            com.hex.kagune.control.pale.PaleStrike strike = all[s];
            boolean last = s == all.length - 1;
            double length = last ? strike.duration + 6 : strike.chain;
            RigPose from = shown;
            for (double age = 0.0; age < length; age += 20.0 / fps) {
                RigPose p = com.hex.kagune.control.client.animation.PaleMotions.strike(strike, age, from);
                shown = p;
                RigPose drawn = new RigPose().add(p, 1.0F);
                drawn.settle(1.0F);
                frames.add(ArtPreview.render(new ArtPreview.Shot(label, drawn, ArtPreview.NO_WINGS, eye, new double[] {0.0, 1.0, 0.6}, dummy(DUMMY_Z)),
                    420, 360));
            }
        }
        writeGif(file, frames, (int)Math.round(100.0 / fps));
    }

    /** One strike over and over, slowed to a quarter, to see the timing. */
    static void strikeGif(File file, com.hex.kagune.control.pale.PaleStrike strike, double[] eye, double slow) throws Exception {
        List<java.awt.image.BufferedImage> frames = new ArrayList<>();
        double fps = 30.0;
        for (double age = 0.0; age <= strike.duration + 3; age += 20.0 / fps / slow) {
            RigPose p = com.hex.kagune.control.client.animation.PaleMotions.strike(strike, age, null);
            p.settle(1.0F);
            frames.add(ArtPreview.render(new ArtPreview.Shot(strike.name().toLowerCase(java.util.Locale.ROOT) + " at 1/" + (int)slow + " speed, tick "
                + String.format(java.util.Locale.ROOT, "%.1f", age), p, ArtPreview.NO_WINGS, eye, new double[] {0.0, 1.0, 0.6}, dummy(DUMMY_Z)), 420, 360));
        }
        writeGif(file, frames, (int)Math.round(100.0 / fps));
    }

    /**
     * 6.2.0, Witness This as a GIF ({@code speed} 1: real time, 30 frames a second): the full-charge Big Punch let go in front of a
     * dummy, then the combo, from one place in the world off to the side, far enough back to see the whole climb. Her afterimage is
     * left (pale, fading) where she blinks from; a white flash where each blow lands.
     */
    static void witnessGif(File file, double speed) throws Exception {
        List<java.awt.image.BufferedImage> frames = new ArrayList<>();
        double fps = 30.0;
        double step = 20.0 / fps * speed;
        double punchIn = com.hex.kagune.control.pale.PaleRules.PUNCH_IMPACT;
        double stand = com.hex.kagune.control.pale.PaleRules.strikeStand(0.5);
        // The world: x her right, y up, z the way the punch goes; the dummy's feet at the origin when it is hit.
        double[] camera = {16.0, 8.0, -2.5};
        double[] lookAt = {0.0, 5.2, 3.0};
        RigPose ghost = null;
        double[] ghostAt = null;
        double ghostSince = 0.0;
        double[] lastHer = null;
        int lastTick = -1;
        for (double t = -punchIn; t <= com.hex.kagune.control.pale.WitnessRules.TICKS + 6.0; t += step) {
            RigPose pose;
            double[] her;
            double[] it;
            String label;
            if (t < 0.0) {
                pose = com.hex.kagune.control.client.animation.PaleMotions.punch(t + punchIn, null);
                her = new double[] {0.0, 0.0, -stand};
                it = new double[] {0.0, 0.0, 0.0};
                label = "Big Punch, full charge";
            } else {
                pose = com.hex.kagune.control.client.animation.PaleMotions.witness(t, null);
                double[] at = com.hex.kagune.control.pale.WitnessRules.her(t, 0.0);
                her = at != null ? at : t < com.hex.kagune.control.pale.WitnessRules.BEHIND ? new double[] {0.0, 0.0, -stand}
                    : com.hex.kagune.control.pale.WitnessRules.her(com.hex.kagune.control.pale.WitnessRules.FREE - 0.01, 0.0);
                if (at == null && t >= com.hex.kagune.control.pale.WitnessRules.FREE) {
                    her = new double[] {her[0], 0.0, her[2]};
                }
                it = com.hex.kagune.control.pale.WitnessRules.target(Math.min(t, com.hex.kagune.control.pale.WitnessRules.IMPACT), 0.0);
                label = t < com.hex.kagune.control.pale.WitnessRules.BEHIND ? "the punch lands"
                    : t < com.hex.kagune.control.pale.WitnessRules.FIRST_HIT - 1 ? "behind it: the uppercut"
                    : t < com.hex.kagune.control.pale.WitnessRules.ABOVE - 1 ? "the blitz"
                    : t < com.hex.kagune.control.pale.WitnessRules.IMPACT ? "above it: the kick down" : "the crater";
            }
            pose.settle(1.0F);
            int tick = (int)Math.floor(t);
            if (tick != lastTick && tick >= 0 && com.hex.kagune.control.pale.WitnessRules.blinks(tick) && lastHer != null) {
                // She blinks: what she was is left behind, fading.
                ghost = com.hex.kagune.control.client.animation.PaleMotions.witness(tick - 0.01, null);
                ghost.settle(1.0F);
                ghostAt = lastHer;
                ghostSince = t;
            }
            lastTick = tick;
            lastHer = her;
            List<WingPreview.Tri> scene = new ArrayList<>();
            boolean down = t >= com.hex.kagune.control.pale.WitnessRules.IMPACT;
            double[] lies = down ? new double[] {it[0], -0.6, it[2]} : it;
            if (t >= com.hex.kagune.control.pale.WitnessRules.UPPER) {
                // A rag doll from the uppercut on (KickTumble, as every client draws it): tumbling, down flat in the crater, lying there.
                double since = t - com.hex.kagune.control.pale.WitnessRules.UPPER;
                double landed = com.hex.kagune.control.pale.WitnessRules.IMPACT - com.hex.kagune.control.pale.WitnessRules.UPPER;
                com.hex.kagune.control.pale.KickTumble.Frame frame = com.hex.kagune.control.pale.KickTumble.at(since, since >= landed ? landed : Double.NaN);
                scene.addAll(tumbled(new double[] {lies[0] - her[0], lies[1] - her[1], lies[2] - her[2]}, new double[] {0.0, 1.0}, frame));
            } else {
                scene.addAll(figure(new double[] {it[0] - her[0], it[1] + 1.0 - her[1], it[2] - her[2]}));
            }
            for (double gx = -6.0; gx < 8.0; gx += 2.0) {
                for (double gz = -6.0; gz < 10.0; gz += 2.0) {
                    double gy = -her[1] - (down && Math.hypot(gx + 1.0 - it[0], gz + 1.0 - it[2]) < 2.6 ? 0.6 : 0.0);
                    box(scene, new double[] {gx - her[0], gy - 0.2, gz - her[2]}, new double[] {gx + 2.0 - her[0], gy, gz + 2.0 - her[2]},
                        ((int)(gx + gz) & 2) == 0 ? 0xFF4E5A44 : 0xFF48543F);
                }
            }
            if (ghost != null && t - ghostSince < 9.0) {
                double fade = 1.0 - (t - ghostSince) / 9.0;
                int alpha = (int)Math.round(110.0 * fade * fade);
                int colour = alpha << 24 | 0xD8DCE2;
                double[] off = {ghostAt[0] - her[0], ghostAt[1] - her[1], ghostAt[2] - her[2]};
                double turn = 0.0;
                com.hex.kagune.control.client.animation.BodyFrame.surface(ghost, (a, b, c, d, n) -> {
                    double[][] q = {moved(a, off), moved(b, off), moved(c, off), moved(d, off)};
                    scene.add(new WingPreview.Tri(new double[][] {q[0], q[1], q[2]}, new int[] {colour, colour, colour}, n, false, false));
                    scene.add(new WingPreview.Tri(new double[][] {q[0], q[2], q[3]}, new int[] {colour, colour, colour}, n, false, false));
                });
            }
            // A white flash where a blow lands, for a tick and a half.
            for (double blow : witnessBlows()) {
                double since = t - blow;
                if (since >= 0.0 && since < 1.5) {
                    double r = 0.15 + 0.35 * since;
                    double[] at = com.hex.kagune.control.pale.WitnessRules.target(blow, 0.0);
                    double[] c = {at[0] - her[0], at[1] + 1.0 - her[1], at[2] - her[2]};
                    int bright = (int)Math.round(200.0 * (1.0 - since / 1.5));
                    int glow = 0xFF000000 | bright << 16 | bright << 8 | bright;
                    box(scene, new double[] {c[0] - r, c[1] - r, c[2] - r}, new double[] {c[0] + r, c[1] + r, c[2] + r}, glow);
                    for (int k = scene.size() - 12; k < scene.size(); k++) {
                        WingPreview.Tri tri = scene.get(k);
                        scene.set(k, new WingPreview.Tri(tri.p(), tri.argb(), tri.n(), false, true));
                    }
                }
            }
            double[] eye = {camera[0] - her[0], camera[1] - her[1], camera[2] - her[2]};
            double[] look = {lookAt[0] - her[0], lookAt[1] - her[1], lookAt[2] - her[2]};
            frames.add(ArtPreview.render(new ArtPreview.Shot(String.format(java.util.Locale.ROOT, "Witness This%s: %s, tick %.1f",
                speed < 1.0 ? String.format(java.util.Locale.ROOT, " (x%.1f speed)", speed) : "", label, t), pose, ArtPreview.NO_WINGS, eye, look, scene), 480,
                400));
        }
        writeGif(file, frames, (int)Math.round(100.0 / fps));
    }

    /** The ticks Witness This's blows land on: the uppercut, each blitz blow, the kick down, the crater. */
    static double[] witnessBlows() {
        double[] blows = new double[com.hex.kagune.control.pale.WitnessRules.HITS + 3];
        blows[0] = com.hex.kagune.control.pale.WitnessRules.UPPER;
        for (int i = 0; i < com.hex.kagune.control.pale.WitnessRules.HITS; i++) {
            blows[i + 1] = com.hex.kagune.control.pale.WitnessRules.hitAt(i);
        }
        blows[blows.length - 2] = com.hex.kagune.control.pale.WitnessRules.DROP;
        blows[blows.length - 1] = com.hex.kagune.control.pale.WitnessRules.IMPACT;
        return blows;
    }

    /** Frames as a looping GIF ({@code delay} hundredths of a second each). */
    static void writeGif(File file, List<java.awt.image.BufferedImage> frames, int delay) throws Exception {
        javax.imageio.ImageWriter writer = javax.imageio.ImageIO.getImageWritersByFormatName("gif").next();
        try (javax.imageio.stream.ImageOutputStream out = javax.imageio.ImageIO.createImageOutputStream(file)) {
            writer.setOutput(out);
            writer.prepareWriteSequence(null);
            for (int i = 0; i < frames.size(); i++) {
                java.awt.image.BufferedImage rgb = new java.awt.image.BufferedImage(frames.get(i).getWidth(), frames.get(i).getHeight(),
                    java.awt.image.BufferedImage.TYPE_INT_RGB);
                rgb.getGraphics().drawImage(frames.get(i), 0, 0, null);
                javax.imageio.ImageTypeSpecifier type = javax.imageio.ImageTypeSpecifier.createFromRenderedImage(rgb);
                javax.imageio.metadata.IIOMetadata meta = writer.getDefaultImageMetadata(type, null);
                String format = meta.getNativeMetadataFormatName();
                javax.imageio.metadata.IIOMetadataNode root = (javax.imageio.metadata.IIOMetadataNode)meta.getAsTree(format);
                javax.imageio.metadata.IIOMetadataNode control = new javax.imageio.metadata.IIOMetadataNode("GraphicControlExtension");
                control.setAttribute("disposalMethod", "none");
                control.setAttribute("userInputFlag", "FALSE");
                control.setAttribute("transparentColorFlag", "FALSE");
                control.setAttribute("delayTime", Integer.toString(delay));
                control.setAttribute("transparentColorIndex", "0");
                root.appendChild(control);
                if (i == 0) {
                    javax.imageio.metadata.IIOMetadataNode apps = new javax.imageio.metadata.IIOMetadataNode("ApplicationExtensions");
                    javax.imageio.metadata.IIOMetadataNode app = new javax.imageio.metadata.IIOMetadataNode("ApplicationExtension");
                    app.setAttribute("applicationID", "NETSCAPE");
                    app.setAttribute("authenticationCode", "2.0");
                    app.setUserObject(new byte[] {1, 0, 0});
                    apps.appendChild(app);
                    root.appendChild(apps);
                }
                meta.setFromTree(format, root);
                writer.writeToSequence(new javax.imageio.IIOImage(rgb, null, meta), null);
            }
            writer.endWriteSequence();
        }
        System.out.println("wrote " + file + " (" + frames.size() + " frames)");
    }

    /** Big Punch: the charge sinking in, then (she has blinked in front of the target) the blow and its hold. */
    static void punch(File file) throws Exception {
        List<ArtPreview.Shot> shots = new ArrayList<>();
        double[] eye = {3.0, 2.0, 3.1};
        double[] target = {0.0, 0.95, 0.75};
        for (double held : new double[] {0.0, 15.0, 30.0}) {
            RigPose p = com.hex.kagune.control.client.animation.PaleMotions.punchCharge(held, null);
            p.settle(1.0F);
            shots.add(new ArtPreview.Shot("charging, tick " + (int)held + (held >= 30 ? " (full)" : ""), p, ArtPreview.NO_WINGS, eye, target, List.of()));
        }
        RigPose full = com.hex.kagune.control.client.animation.PaleMotions.punchCharge(30.0, null);
        String[] names = {"blinked in, coiled", "driving", "IMPACT", "overshoot", "holding"};
        double[] ages = {1.0, 3.2, com.hex.kagune.control.pale.PaleRules.PUNCH_IMPACT, com.hex.kagune.control.pale.PaleRules.PUNCH_IMPACT + 0.7, 8.0};
        for (int i = 0; i < ages.length; i++) {
            RigPose p = com.hex.kagune.control.client.animation.PaleMotions.punch(ages[i], full);
            p.settle(1.0F);
            shots.add(new ArtPreview.Shot(names[i], p, ArtPreview.NO_WINGS, eye, target, dummy(DUMMY_Z + 0.2)));
        }
        ArtPreview.sheet(file, "Pale's Big Punch (Z held, let go): the charge, then the blow", shots, 4, 320, 340);
    }

    /** A stand-in body (two blocks tall) with its middle at {@code centre}. */
    static List<WingPreview.Tri> figure(double[] centre) {
        List<WingPreview.Tri> tris = new ArrayList<>();
        double x = centre[0];
        double y = centre[1] - 1.0;
        double z = centre[2];
        box(tris, new double[] {x - 0.25, y + 0.75, z - 0.125}, new double[] {x + 0.25, y + 1.5, z + 0.125}, 0xFF6E7378);
        box(tris, new double[] {x - 0.25, y + 1.5, z - 0.25}, new double[] {x + 0.25, y + 2.0, z + 0.25}, 0xFF7C8186);
        box(tris, new double[] {x - 0.24, y, z - 0.12}, new double[] {x + 0.24, y + 0.75, z + 0.12}, 0xFF5A5F64);
        return tris;
    }

    /** Her flesh as the game draws it ({@link com.hex.kagune.control.pale.PaleFleshModel#mesh}), as triangles, with anything else. */
    static List<WingPreview.Tri> flesh(com.hex.kagune.control.pale.PaleFleshModel.Pose pose, List<WingPreview.Tri> with) {
        List<WingPreview.Tri> tris = new ArrayList<>(with);
        if (pose == null) {
            return tris;
        }
        com.hex.kagune.control.pale.PaleFleshModel.mesh((a, b, c, d, n, ca, cb, cc, cd) -> {
            tris.add(new WingPreview.Tri(new double[][] {a, b, c}, new int[] {ca, cb, cc}, n, true));
            tris.add(new WingPreview.Tri(new double[][] {a, c, d}, new int[] {ca, cc, cd}, n, true));
        }, pose);
        return tris;
    }

    /** One frame of a flesh move: her body as it moves, the flesh on it, and what it reaches for. */
    static ArtPreview.Shot fleshShot(String label, com.hex.kagune.control.pale.PaleMove move, double age, double[] tip, double reach, double[] eye,
                                     double[] look, double[] body) {
        RigPose p = com.hex.kagune.control.client.animation.PaleMotions.flesh(move, age, null);
        p.settle(1.0F);
        com.hex.kagune.control.pale.PaleFleshModel.Pose pose = com.hex.kagune.control.client.animation.PaleFleshPose.of(p, move, age, tip, 0.45, reach, age,
            7);
        if (move == com.hex.kagune.control.pale.PaleMove.SWIPE && pose != null) {
            pose = whipped(pose, age);
        }
        return new ArtPreview.Shot(label, p, ArtPreview.NO_WINGS, eye, look, flesh(pose, body == null ? List.of() : figure(body)));
    }

    /** The swipe's flesh at {@code age} as it carries its weight (FleshPhysics.WHIP), followed from the start, as the game does. */
    static com.hex.kagune.control.pale.PaleFleshModel.Pose whipped(com.hex.kagune.control.pale.PaleFleshModel.Pose pose, double age) {
        com.hex.kagune.control.pale.PaleMove swipe = com.hex.kagune.control.pale.PaleMove.SWIPE;
        double[][] path = com.hex.kagune.control.pale.FleshPhysics.simulate(t -> {
            RigPose b = com.hex.kagune.control.client.animation.PaleMotions.flesh(swipe, t, null);
            b.settle(1.0F);
            com.hex.kagune.control.pale.PaleFleshModel.Pose at = com.hex.kagune.control.client.animation.PaleFleshPose.of(b, swipe, t,
                com.hex.kagune.control.client.animation.PaleFleshPose.swipeTip(t), 0.45, 0.0, t, 7);
            return at == null ? null : at.path();
        }, age, com.hex.kagune.control.pale.FleshPhysics.WHIP);
        if (path == null) {
            return pose;
        }
        return new com.hex.kagune.control.pale.PaleFleshModel.Pose(path, pose.up(), pose.axis(), pose.grown(), pose.shape(), pose.grip(), pose.time(),
            pose.seed(), pose.keep());
    }

    /** 6.2.0: the swipe's whip, tick by tick from above and behind her, the blade trailing then catching up; and the blade up close. */
    static void swipe(File file) throws Exception {
        List<ArtPreview.Shot> shots = new ArrayList<>();
        com.hex.kagune.control.pale.PaleMove swipe = com.hex.kagune.control.pale.PaleMove.SWIPE;
        double[] above = {0.0, 17.0, -5.0};
        double[] ahead = {0.0, 0.5, 2.8};
        for (double age : new double[] {4.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 16.0}) {
            shots.add(fleshShot("tick " + age, swipe, age, com.hex.kagune.control.client.animation.PaleFleshPose.swipeTip(age), 0.0, above, ahead, null));
        }
        double[] tip = com.hex.kagune.control.client.animation.PaleFleshPose.swipeTip(11.0);
        double[] x = bladeAt(swipe, 11.0);
        shots.add(fleshShot("the blade, cutting through", swipe, 11.0, tip, 0.0, new double[] {x[0] + 1.6, x[1] + 2.4, x[2] - 2.2}, x, null));
        ArtPreview.sheet(file, "Pale's Flesh Swipe (J tap): the whip, the blade trailing, catching up, swinging through", shots, 4, 340, 320);
        // The blade up close, from over her shoulder (as you see it in game), from above, and level with it.
        List<ArtPreview.Shot> close = new ArrayList<>();
        for (double age : new double[] {9.0, 12.0}) {
            double[] b = bladeAt(swipe, age);
            double[] t = com.hex.kagune.control.client.animation.PaleFleshPose.swipeTip(age);
            close.add(fleshShot("behind her, tick " + age, swipe, age, t, 0.0, new double[] {-2.4, 6.0, -5.5}, b, null));
            close.add(fleshShot("from above, tick " + age, swipe, age, t, 0.0, new double[] {b[0] * 0.8, b[1] + 3.2, b[2] * 0.8 - 0.4}, b, null));
            close.add(fleshShot("level with it, tick " + age, swipe, age, t, 0.0, new double[] {b[0] * 0.55 + 1.2, b[1] + 0.35, b[2] * 0.55 - 1.4}, b, null));
        }
        ArtPreview.sheet(new File(file.getParentFile(), "pale-blade.png"), "The swipe's blade up close", close, 3, 420, 380);
    }

    /** Where the swipe's blade is at {@code age} (rig space): the end of the flesh, as it carries its weight. */
    static double[] bladeAt(com.hex.kagune.control.pale.PaleMove swipe, double age) {
        RigPose p = com.hex.kagune.control.client.animation.PaleMotions.flesh(swipe, age, null);
        p.settle(1.0F);
        com.hex.kagune.control.pale.PaleFleshModel.Pose pose = whipped(com.hex.kagune.control.client.animation.PaleFleshPose.of(p, swipe, age,
            com.hex.kagune.control.client.animation.PaleFleshPose.swipeTip(age), 0.45, 0.0, age, 7), age);
        double[][] path = pose.path();
        double[] end = path[path.length - 1];
        double[] before = path[path.length - 2];
        double[] d = {end[0] - before[0], 0.0, end[2] - before[2]};
        double l = Math.max(1.0E-6, Math.hypot(d[0], d[2]));
        return new double[] {end[0] + d[0] / l * 1.0, end[1], end[2] + d[2] / l * 1.0};
    }

    /** The flesh: the swipe sweeping across her front, the grab reaching, holding, drawing back and flinging, seen from behind too. */
    static void flesh(File file) throws Exception {
        List<ArtPreview.Shot> shots = new ArrayList<>();
        com.hex.kagune.control.pale.PaleMove swipe = com.hex.kagune.control.pale.PaleMove.SWIPE;
        double[] wide = {-5.5, 6.5, -4.0};
        double[] ahead = {0.0, 1.0, 3.0};
        for (double age : new double[] {2.0, 4.0, 6.0, 8.0, 10.0, 12.0, 15.0, 18.0}) {
            double[] tip = com.hex.kagune.control.client.animation.PaleFleshPose.swipeTip(age);
            shots.add(fleshShot("swipe, tick " + age, swipe, age, tip, 0.0, wide, ahead, null));
        }
        double[] close = {-2.6, 2.6, -2.6};
        double[] her = {0.0, 1.4, 0.0};
        for (double age : new double[] {3.0, 7.0}) {
            shots.add(fleshShot("swipe from behind, tick " + age, swipe, age, com.hex.kagune.control.client.animation.PaleFleshPose.swipeTip(age), 0.0,
                close, her, null));
        }
        double[] target = {1.5, 1.2, 7.0};
        int reach = com.hex.kagune.control.pale.PaleRules.grabReachTicks(7.0);
        double[] side = {-10.0, 5.5, 0.0};
        double[] mid = {0.6, 1.4, 3.4};
        double[] near = {-4.6, 3.2, 3.8};
        double[] held = {0.0, 1.7, 1.6};
        for (double u : new double[] {0.35, 0.7, 1.0}) {
            shots.add(fleshShot("grab reaching, " + (int)(u * 100) + "%", com.hex.kagune.control.pale.PaleMove.GRAB_REACH, reach * u, target, reach, side, mid,
                target));
        }
        double[] hold = {0.0, com.hex.kagune.control.pale.PaleRules.GRAB_HOLD_UP, com.hex.kagune.control.pale.PaleRules.GRAB_HOLD_AHEAD};
        shots.add(fleshShot("holding it up", com.hex.kagune.control.pale.PaleMove.GRAB_HOLD, 6.0, hold, 0.0, near, held, hold));
        shots.add(fleshShot("holding it, from behind", com.hex.kagune.control.pale.PaleMove.GRAB_HOLD, 6.0, hold, 0.0, close, her, hold));
        shots.add(fleshShot("holding it, close", com.hex.kagune.control.pale.PaleMove.GRAB_HOLD, 6.0, hold, 0.0, new double[] {-2.4, 3.4, 7.6}, hold, hold));
        shots.add(fleshShot("reaching, close", com.hex.kagune.control.pale.PaleMove.GRAB_REACH, reach, target, reach, new double[] {-1.8, 2.4, 10.5}, target,
            target));
        int release = com.hex.kagune.control.pale.PaleRules.grabRelease();
        int pull = com.hex.kagune.control.pale.PaleRules.GRAB_PULL;
        int hang = pull + com.hex.kagune.control.pale.PaleRules.GRAB_HANG;
        double[] ages = {pull * 0.5, pull, hang, hang + 1.5, hang + 2.4, release};
        String[] names = {"hauling it over", "all the way back", "hanging, straining", "the whip, over her", "the whip, coming down", "let go"};
        double[] wideNear = {-7.0, 4.5, 2.5};
        double[] overHer = {0.0, 2.6, 0.0};
        for (int i = 0; i < ages.length; i++) {
            double[] at = com.hex.kagune.control.pale.PaleRules.throwAt(ages[i], hold);
            shots.add(fleshShot(names[i], com.hex.kagune.control.pale.PaleMove.GRAB_THROW, ages[i], at, 0.0, wideNear, overHer, at));
        }
        shots.add(fleshShot("drawing back in", com.hex.kagune.control.pale.PaleMove.GRAB_THROW, release + 5.0, com.hex.kagune.control.pale.PaleRules.GRAB_FLING,
            0.0, near, held, null));
        ArtPreview.sheet(file, "Pale's flesh (J): the swipe, the grab, the throw", shots, 4, 360, 340);
    }

    static double[] lerp(double[] a, double[] b, double t) {
        return new double[] {a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
    }

    /**
     * The Speed Kick (6.2.0, Kizaru's kick): the lunge into the dash (the user's stick figure), gone (what is left behind as she goes),
     * there in the chamber, the leg driven straight out into a dummy's middle, held out, folded, her stance; side on (as the Kizaru
     * frame), from her right front, and from behind her.
     */
    static void kick(File file) throws Exception {
        List<ArtPreview.Shot> shots = new ArrayList<>();
        int dash = 3;
        int arrive = com.hex.kagune.control.pale.PaleRules.kickArrive(dash);
        double contact = arrive + com.hex.kagune.control.pale.PaleRules.KICK_CONTACT;
        double[] ages = {0.7, 1.5, arrive - 0.01, arrive, contact - 0.5, contact, contact + 0.6, arrive + com.hex.kagune.control.pale.PaleRules.KICK_HOLD,
            arrive + 3.9, arrive + com.hex.kagune.control.pale.PaleRules.KICK_TUCK, arrive + 5.7, arrive + com.hex.kagune.control.pale.PaleRules.KICK_OPEN,
            arrive + com.hex.kagune.control.pale.PaleRules.KICK_LAND, arrive + 10.5, arrive + com.hex.kagune.control.pale.PaleRules.KICK_TICKS};
        String[] names = {"into the lunge", "THE LUNGE (left behind)", "gone (unseen)", "there: the chamber", "driving out", "THE BLOW", "through",
            "held out flat", "the rebound: over", "tucked, upside down", "coming round", "opening out", "down: the crouch", "rising", "her stance"};
        double middle = 1.0;
        double lift = com.hex.kagune.control.pale.PaleRules.kickLift(middle);
        double[] stand = com.hex.kagune.control.pale.PaleRules.kickStand(0.25);
        double[] flight = com.hex.kagune.control.pale.PaleRules.kickFlight();
        double[][] eyes = {{4.6, 1.3, 0.9}, {3.2, 2.0, 3.6}, {0.6, 2.0, -4.2}};
        String[] views = {"side on", "her right front", "behind her"};
        for (int pass = 0; pass < eyes.length; pass++) {
            for (int i = 0; i < ages.length; i++) {
                double age = ages[i];
                RigPose p = com.hex.kagune.control.client.animation.PaleMotions.kick(age, dash, null);
                p.settle(1.0F);
                // She is drawn where she is; the target and the ground as far below her as she is up. Before she is there, the target
                // is ahead of her by however much of the dash is left.
                double up = com.hex.kagune.control.pale.PaleRules.kickHeight(dash, lift, age);
                double s = Math.max(0.0, Math.min(1.0, (age - com.hex.kagune.control.pale.PaleRules.KICK_BURST) / dash));
                double left = age < arrive ? 12.0 * (1.0 - s) * (1.0 - s) : 0.0;
                double flown = age > contact ? Math.min(6.0, (age - contact) * 4.6) : 0.0;
                List<WingPreview.Tri> scene = flown > 5.5 ? new ArrayList<>()
                    : figure(new double[] {-stand[0] + flight[0] * flown, 1.0 - up + flown * 0.1, -stand[1] + left + flight[1] * flown});
                box(scene, new double[] {-6.0, -0.2 - up, -4.0}, new double[] {4.0, -up, 14.0}, 0xFF4E5A44);
                double[] eye = eyes[pass];
                shots.add(new ArtPreview.Shot(names[i] + " (" + views[pass] + String.format(java.util.Locale.ROOT, ", tick %.1f)", age),
                    com.hex.kagune.control.pale.PaleRules.kickGone(dash, age) && i != 1 ? null : p, ArtPreview.NO_WINGS,
                    new double[] {eye[0], eye[1] - up * 0.3, eye[2]}, new double[] {0.0, 0.9 - up * 0.4, 0.5}, scene));
            }
        }
        ArtPreview.sheet(file, "Owl DNA's Speed Kick (V tapped): the lunge, gone, there in the chamber, the leg driven straight into its middle (Kizaru)",
            shots, 4, 330, 330);
    }

    /**
     * 6.2.0: the Speed Kick's two poses up close, side on as the user's references are drawn: the lunge into the dash (the stick
     * figure) and the blow (Kizaru's kick into Luffy's gut), and each from her right front.
     */
    static void kickPoses(File file) throws Exception {
        List<ArtPreview.Shot> shots = new ArrayList<>();
        int dash = 3;
        int arrive = com.hex.kagune.control.pale.PaleRules.kickArrive(dash);
        double contact = arrive + com.hex.kagune.control.pale.PaleRules.KICK_CONTACT;
        double[] stand = com.hex.kagune.control.pale.PaleRules.kickStand(0.25);
        double lift = com.hex.kagune.control.pale.PaleRules.kickLift(1.0);
        Object[][] frames = {{"the lunge into the dash (side on)", 1.5, new double[] {3.4, 0.8, 0.5}}, {"the lunge (her right front)", 1.5, new double[] {2.4, 1.4, 2.6}},
            {"THE BLOW (side on)", contact + 0.3, new double[] {4.0, 0.9, 0.2}}, {"THE BLOW (other side)", contact + 0.3, new double[] {-4.0, 0.9, 0.2}},
            {"THE BLOW (her right front)", contact + 0.3, new double[] {2.6, 1.6, 3.2}}, {"THE BLOW (from above)", contact + 0.3, new double[] {1.2, 4.2, 0.4}},
            {"there: the chamber (side on)", (double)arrive, new double[] {3.8, 0.9, 0.6}}, {"the backflip off it (side on)", arrive + 4.8, new double[] {4.2, 1.2, 0.3}}};
        for (Object[] f : frames) {
            double age = (Double)f[1];
            RigPose p = com.hex.kagune.control.client.animation.PaleMotions.kick(age, dash, null);
            p.settle(1.0F);
            double up = com.hex.kagune.control.pale.PaleRules.kickHeight(dash, lift, age);
            List<WingPreview.Tri> scene = age < arrive || age > contact + 1.0 ? new ArrayList<>() : figure(new double[] {-stand[0], 1.0 - up, -stand[1]});
            box(scene, new double[] {-3.0, -0.2 - up, -3.0}, new double[] {3.0, -up, 4.0}, 0xFF4E5A44);
            shots.add(new ArtPreview.Shot((String)f[0], p, ArtPreview.NO_WINGS, (double[])f[2], new double[] {0.0, 0.75 - up * 0.5, 0.45}, scene));
        }
        ArtPreview.sheet(file, "The Speed Kick's poses: the lunge (the stick figure) and Kizaru's kick", shots, 2, 560, 460);
    }

    /**
     * 6.2.0, True Anonymity: the Flesh Hammer's club grown on past its full size, bristling with bone spikes, next to the plain one at
     * its full size; held up, and the slam.
     */
    static void hammerHuge(File file) throws Exception {
        List<ArtPreview.Shot> shots = new ArrayList<>();
        com.hex.kagune.control.pale.PaleMove charging = com.hex.kagune.control.pale.PaleMove.HAMMER_CHARGE;
        com.hex.kagune.control.pale.PaleMove slamming = com.hex.kagune.control.pale.PaleMove.HAMMER;
        float huge = com.hex.kagune.control.pale.PaleRules.HAMMER_HUGE;
        RigPose held = com.hex.kagune.control.client.animation.PaleMotions.hammerCharge(30.0, null);
        held.settle(1.0F);
        double[] side = {9.0, 3.4, 1.4};
        double[] mid = {0.0, 2.8, 0.8};
        shots.add(new ArtPreview.Shot("plain, full size (held)", held, ArtPreview.NO_WINGS, side, mid,
            flesh(com.hex.kagune.control.client.animation.PaleFleshPose.club(held, charging, 30.0, 1.0F, 30.0, 9, false), ground(4.5))));
        shots.add(new ArtPreview.Shot("TRUE ANONYMITY (held)", held, ArtPreview.NO_WINGS, side, mid,
            flesh(com.hex.kagune.control.client.animation.PaleFleshPose.club(held, charging, 30.0, huge, 30.0, 9, true), ground(6.5))));
        shots.add(new ArtPreview.Shot("TRUE ANONYMITY (close)", held, ArtPreview.NO_WINGS, new double[] {3.2, 4.4, 2.6}, new double[] {0.0, 3.6, 0.6},
            flesh(com.hex.kagune.control.client.animation.PaleFleshPose.club(held, charging, 30.0, huge, 30.0, 9, true), ground(6.5))));
        double impact = com.hex.kagune.control.pale.PaleRules.HAMMER_IMPACT;
        for (double age : new double[] {com.hex.kagune.control.pale.PaleRules.HAMMER_COCK, impact, impact + 0.8}) {
            RigPose p = com.hex.kagune.control.client.animation.PaleMotions.hammer(age, huge, held);
            p.settle(1.0F);
            shots.add(new ArtPreview.Shot((age == impact ? "IMPACT" : "the slam") + String.format(java.util.Locale.ROOT, ", tick %.1f", age), p, ArtPreview.NO_WINGS,
                new double[] {9.0, 3.0, 3.0}, new double[] {0.0, 1.4, 2.6},
                flesh(com.hex.kagune.control.client.animation.PaleFleshPose.club(p, slamming, age, huge, age, 9, true), ground(7.0))));
        }
        ArtPreview.sheet(file, String.format(java.util.Locale.ROOT, "True Anonymity: the Flesh Hammer's club grown on to %.1fx, bristling with bone spikes", huge),
            shots, 3, 380, 360);
    }

    /**
     * 6.2.0, Witness This: the full-charge Big Punch's combo, frame by frame, from one place beside it: driven back, behind it, the
     * uppercut, up after it, the blitz from side to side, above it, the kick down, the crater, landed.
     */
    static void witness(File file) throws Exception {
        List<ArtPreview.Shot> shots = new ArrayList<>();
        Object[][] frames = {{"the punch lands, driven back", 2.0}, {"gone: behind it", 5.0}, {"THE UPPERCUT", 7.0}, {"up after it", 10.5},
            {"blitz 1", (double)com.hex.kagune.control.pale.WitnessRules.hitAt(0)}, {"blitz 2 (the other side)", (double)com.hex.kagune.control.pale.WitnessRules.hitAt(1)},
            {"blitz 3", (double)com.hex.kagune.control.pale.WitnessRules.hitAt(2)}, {"blitz 4", (double)com.hex.kagune.control.pale.WitnessRules.hitAt(3)},
            {"blitz 8 (twin palms)", (double)com.hex.kagune.control.pale.WitnessRules.hitAt(7)}, {"above it", com.hex.kagune.control.pale.WitnessRules.ABOVE + 1.0},
            {"THE KICK DOWN", (double)com.hex.kagune.control.pale.WitnessRules.DROP}, {"down with it", com.hex.kagune.control.pale.WitnessRules.DROP + 1.5},
            {"CRATER", (double)com.hex.kagune.control.pale.WitnessRules.IMPACT}, {"landing beside it", com.hex.kagune.control.pale.WitnessRules.IMPACT + 3.0},
            {"landed", (double)com.hex.kagune.control.pale.WitnessRules.FREE + 2.0}};
        for (Object[] f : frames) {
            double age = (Double)f[1];
            double[] it = com.hex.kagune.control.pale.WitnessRules.target(age, 0.0);
            double[] her = com.hex.kagune.control.pale.WitnessRules.her(age, 0.0);
            if (her == null) {
                // Where she threw the punch (before), or beside the crater (after).
                her = age < com.hex.kagune.control.pale.WitnessRules.BEHIND ? new double[] {0.0, 0.0, -1.3}
                    : com.hex.kagune.control.pale.WitnessRules.her(com.hex.kagune.control.pale.WitnessRules.FREE - 0.01, 0.0);
            }
            RigPose p = com.hex.kagune.control.client.animation.PaleMotions.witness(age, null);
            p.settle(1.0F);
            List<WingPreview.Tri> scene = age >= com.hex.kagune.control.pale.WitnessRules.IMPACT ? new ArrayList<>()
                : figure(new double[] {it[0] - her[0], it[1] + 1.0 - her[1], it[2] - her[2]});
            if (age >= com.hex.kagune.control.pale.WitnessRules.IMPACT) {
                // Lying in the crater.
                box(scene, new double[] {it[0] - her[0] - 0.25, -her[1] - 0.6, it[2] - her[2] - 1.0}, new double[] {it[0] - her[0] + 0.25, -her[1] - 0.15,
                    it[2] - her[2] + 1.0}, 0xFF6E7378);
            }
            for (double gx = -8.0; gx < 8.0; gx += 2.0) {
                for (double gz = -6.0; gz < 10.0; gz += 2.0) {
                    double gy = -her[1] - (age >= com.hex.kagune.control.pale.WitnessRules.IMPACT && Math.hypot(gx + 1.0 - it[0], gz + 1.0 - it[2]) < 2.6 ? 0.6 : 0.0);
                    box(scene, new double[] {gx - her[0], gy - 0.2, gz - her[2]}, new double[] {gx + 2.0 - her[0], gy, gz + 2.0 - her[2]},
                        ((int)(gx + gz) & 2) == 0 ? 0xFF4E5A44 : 0xFF48543F);
                }
            }
            // Following it, from its side and a little above: she is round it wherever she is.
            double[] eye = {it[0] + 7.0 - her[0], it[1] + 2.6 - her[1], it[2] - 3.0 - her[2]};
            double[] look = {it[0] - her[0], it[1] + 0.8 - her[1], it[2] - her[2]};
            shots.add(new ArtPreview.Shot((String)f[0] + String.format(java.util.Locale.ROOT, ", tick %.1f", age), p, ArtPreview.NO_WINGS, eye, look, scene));
        }
        ArtPreview.sheet(file, "Witness This (full-charge Big Punch): behind it, the uppercut, the blitz from side to side, the kick down into a crater",
            shots, 5, 300, 320);
    }

    /** The Flesh Hammer: the leg swung up and the club growing on up out of it, then the wind-up, the axe kick, the club drawn back in. */
    static void hammer(File file) throws Exception {
        List<ArtPreview.Shot> shots = new ArrayList<>();
        double[] side = {8.0, 2.8, 1.0};
        double[] mid = {0.0, 2.3, 0.8};
        com.hex.kagune.control.pale.PaleMove charging = com.hex.kagune.control.pale.PaleMove.HAMMER_CHARGE;
        com.hex.kagune.control.pale.PaleMove slamming = com.hex.kagune.control.pale.PaleMove.HAMMER;
        for (double age : new double[] {3.0, 6.0, 10.0, 30.0}) {
            RigPose p = com.hex.kagune.control.client.animation.PaleMotions.hammerCharge(age, null);
            p.settle(1.0F);
            float charge = com.hex.kagune.control.pale.PaleRules.hammerCharge(age);
            shots.add(new ArtPreview.Shot("held, tick " + age + String.format(java.util.Locale.ROOT, " (charge %.2f)", charge), p, ArtPreview.NO_WINGS,
                side, mid, flesh(com.hex.kagune.control.client.animation.PaleFleshPose.club(p, charging, age, charge, age, 9), ground(4.5))));
        }
        RigPose held = com.hex.kagune.control.client.animation.PaleMotions.hammerCharge(30.0, null);
        RigPose front = com.hex.kagune.control.client.animation.PaleMotions.hammerCharge(30.0, null);
        front.settle(1.0F);
        shots.add(new ArtPreview.Shot("held, from the front", front, ArtPreview.NO_WINGS, new double[] {2.2, 2.6, 6.0}, mid,
            flesh(com.hex.kagune.control.client.animation.PaleFleshPose.club(front, charging, 30.0, 1.0F, 30.0, 9), ground(4.5))));
        double impact = com.hex.kagune.control.pale.PaleRules.HAMMER_IMPACT;
        double retract = com.hex.kagune.control.pale.PaleRules.HAMMER_RETRACT_AT;
        String[] names = {"cocked", "coming down", "coming down", "IMPACT", "over it", "lying there", "drawing in", "back up"};
        double[] ages = {com.hex.kagune.control.pale.PaleRules.HAMMER_COCK, 3.5, 4.8, impact, impact + 0.8, retract - 2.0, retract + 5.0,
            retract + 9.0};
        double[] wide = {8.0, 2.8, 1.8};
        double[] ahead = {0.0, 1.8, 1.7};
        for (int i = 0; i < ages.length; i++) {
            RigPose p = com.hex.kagune.control.client.animation.PaleMotions.hammer(ages[i], 1.0F, held);
            p.settle(1.0F);
            shots.add(new ArtPreview.Shot(names[i] + ", tick " + ages[i], p, ArtPreview.NO_WINGS, wide, ahead,
                flesh(com.hex.kagune.control.client.animation.PaleFleshPose.club(p, slamming, ages[i], 1.0F, ages[i], 9), ground(4.5))));
        }
        for (float charge : new float[] {0.1F, 0.5F}) {
            RigPose small = com.hex.kagune.control.client.animation.PaleMotions.hammerCharge(charge * 30.0, null);
            RigPose p = com.hex.kagune.control.client.animation.PaleMotions.hammer(impact, charge, small);
            p.settle(1.0F);
            shots.add(new ArtPreview.Shot(String.format(java.util.Locale.ROOT, "IMPACT at charge %.1f", charge), p, ArtPreview.NO_WINGS, wide, ahead,
                flesh(com.hex.kagune.control.client.animation.PaleFleshPose.club(p, slamming, impact, charge, impact, 9), ground(4.5))));
        }
        ArtPreview.sheet(file, "Pale's Flesh Hammer (V held, let go): the leg up, the club grown on up out of it, the axe kick", shots, 4, 340, 330);
    }

    /**
     * The Flesh Hammer in first person, as the game draws it there (PaleFleshRenderer): no body but her own right leg (PaleOwnLeg, a
     * plain skin: blue trousers, grey shoes) and the club round it, both seen from where her head has gone; the camera at her eyes
     * as she stands, looking ahead (or up).
     */
    static void hammerFirstPerson(File file) throws Exception {
        List<ArtPreview.Shot> shots = new ArrayList<>();
        com.hex.kagune.control.pale.PaleMove charging = com.hex.kagune.control.pale.PaleMove.HAMMER_CHARGE;
        com.hex.kagune.control.pale.PaleMove slamming = com.hex.kagune.control.pale.PaleMove.HAMMER;
        double[] eye = {0.0, 1.62, 0.0};
        double[] level = {0.0, 1.62, 4.0};
        double[] up = {0.0, 1.62 + 4.0 * Math.tan(Math.toRadians(30.0)), 4.0};
        Object[][] frames = {
            {"held, tick 3", charging, 3.0, 0.0F, level}, {"held, tick 6", charging, 6.0, 0.0F, level}, {"held, tick 10 (leg up)", charging, 10.0, 0.0F, level},
            {"held, tick 30 (full)", charging, 30.0, 0.0F, level}, {"held, tick 30, looking up", charging, 30.0, 0.0F, up},
            {"let go: cocked, tick 2", slamming, 2.0, 1.0F, level}, {"coming down, tick 4.8", slamming, 4.8, 1.0F, level},
            {"IMPACT, tick 6", slamming, 6.0, 1.0F, level}, {"lying there, tick 12", slamming, 12.0, 1.0F, level},
            {"drawing in, tick 20", slamming, 20.0, 1.0F, level}};
        for (Object[] f : frames) {
            com.hex.kagune.control.pale.PaleMove move = (com.hex.kagune.control.pale.PaleMove)f[1];
            double age = (Double)f[2];
            float charge = move == charging ? com.hex.kagune.control.pale.PaleRules.hammerCharge(age) : (Float)f[3];
            RigPose body = com.hex.kagune.control.client.animation.PaleMotions.fleshBody(move, age, charge, 0.0F);
            double[] shift = com.hex.kagune.control.client.animation.PaleOwnLeg.shift(body);
            List<WingPreview.Tri> tris = new ArrayList<>();
            box(tris, new double[] {-6.0, -0.2, 0.6}, new double[] {6.0, 0.0, 12.0}, 0xFF4E5A44);
            com.hex.kagune.control.pale.PaleFleshModel.Pose club = com.hex.kagune.control.client.animation.PaleFleshPose.club(body, move, age, charge, age, 9);
            if (club != null) {
                com.hex.kagune.control.pale.PaleFleshModel.mesh((a, b, c, d, n, ca, cb, cc, cd) -> {
                    double[][] q = {moved(a, shift), moved(b, shift), moved(c, shift), moved(d, shift)};
                    tris.add(new WingPreview.Tri(new double[][] {q[0], q[1], q[2]}, new int[] {ca, cb, cc}, n, true));
                    tris.add(new WingPreview.Tri(new double[][] {q[0], q[2], q[3]}, new int[] {ca, cc, cd}, n, true));
                }, club);
            }
            com.hex.kagune.control.client.animation.PaleOwnLeg.mesh(body, shift, (a, b, c, d, n, uv, trousers) -> {
                if (trousers) {
                    return;
                }
                // A plain skin: the shoe is the leg's last three pixels.
                int colour = uv[5] * 64.0F > 29.01F ? 0xFF5A5A5A : 0xFF2E3A8C;
                tris.add(new WingPreview.Tri(new double[][] {a, b, c}, new int[] {colour, colour, colour}, n, true));
                tris.add(new WingPreview.Tri(new double[][] {a, c, d}, new int[] {colour, colour, colour}, n, true));
            });
            shots.add(new ArtPreview.Shot((String)f[0], null, ArtPreview.NO_WINGS, eye, (double[])f[4], tris));
        }
        ArtPreview.sheet(file, "Pale's Flesh Hammer in first person: her own leg swung up, the club grown round it, the axe kick", shots, 5, 360, 300);
    }

    static double[] moved(double[] p, double[] by) {
        return new double[] {p[0] + by[0], p[1] + by[1], p[2] + by[2]};
    }

    /**
     * The body the Speed Kick lands on (com.hex.kagune.control.pale.KickTumble, as every client draws it): the head snapped on the frame
     * of the blow, the body after it, launched straight away from her (6.2.0: carried as the server carries it, no air drag, slowing a
     * little a tick, gravity), tumbling; down flat, lying there, getting up. Her kick for the first frames.
     */
    static void kicked(File file) throws Exception {
        List<ArtPreview.Shot> shots = new ArrayList<>();
        int dash = 3;
        int arrive = com.hex.kagune.control.pale.PaleRules.kickArrive(dash);
        double middle = 1.0;
        double lift = com.hex.kagune.control.pale.PaleRules.kickLift(middle);
        double[] stand = com.hex.kagune.control.pale.PaleRules.kickStand(0.25);
        double[] start = {-stand[0], 0.0, -stand[1]};
        double[] dir = com.hex.kagune.control.pale.PaleRules.kickFlight();
        // Flown as the server flies it: carried along its line (slowing by KICK_CARRY a tick), gravity; once down, it skids to a stop.
        List<double[]> path = new ArrayList<>();
        double[] at = start.clone();
        double vy = com.hex.kagune.control.pale.PaleRules.KICK_UP;
        double landed = Double.NaN;
        double skid = 0.0;
        for (int tick = 0; tick <= 80; tick++) {
            path.add(at.clone());
            double speed = Double.isNaN(landed) ? com.hex.kagune.control.pale.PaleRules.kickCarried(com.hex.kagune.control.pale.PaleRules.KICK_SPEED, tick, 0) : skid;
            at[0] += dir[0] * speed;
            at[1] += vy;
            at[2] += dir[1] * speed;
            if (at[1] <= 0.0) {
                at[1] = 0.0;
                vy = 0.0;
                if (Double.isNaN(landed) && tick >= 2) {
                    landed = tick + 1;
                    skid = speed * 0.6;
                }
            } else {
                vy = (vy - 0.08) * 0.98;
            }
            skid *= 0.546;
        }
        double[] times = {0.0, 0.5, 1.0, 2.0, 5.0, 9.0, landed, landed + 1.5, landed + 3.0, landed + 20.0,
            landed + com.hex.kagune.control.pale.PaleRules.KICK_DOWN_TICKS + 6.0, landed + com.hex.kagune.control.pale.PaleRules.KICK_DOWN_TICKS + 12.0};
        String[] names = {"CONTACT", "the head snaps", "the body after it", "launched", "flying, tumbling", "flying, tumbling", "comes down",
            "the thump", "flat", "lying there", "getting up", "up"};
        for (int i = 0; i < times.length; i++) {
            double t = times[i];
            com.hex.kagune.control.pale.KickTumble.Frame frame = com.hex.kagune.control.pale.KickTumble.at(t, t >= landed ? landed : Double.NaN);
            double[] where = path.get((int)Math.min(path.size() - 1, Math.floor(t)));
            List<WingPreview.Tri> scene = tumbled(where, dir, frame);
            for (double gx = -6.0; gx < 6.0; gx += 2.0) {
                for (double gz = -2.0; gz < 60.0; gz += 2.0) {
                    box(scene, new double[] {gx, -0.2, gz}, new double[] {gx + 2.0, 0.0, gz + 2.0}, ((int)(gx + gz) & 2) == 0 ? 0xFF4E5A44 : 0xFF48543F);
                }
            }
            RigPose her = null;
            double since = arrive + com.hex.kagune.control.pale.PaleRules.KICK_CONTACT + t;
            if (t <= 2.0) {
                her = com.hex.kagune.control.client.animation.PaleMotions.kick(since, dash, null);
                her.settle(1.0F);
            }
            // (She is drawn at her feet: lifted, the world as far below her.)
            double up = t <= 2.0 ? com.hex.kagune.control.pale.PaleRules.kickHeight(dash, lift, since) : 0.0;
            List<WingPreview.Tri> lowered = new ArrayList<>();
            for (WingPreview.Tri tri : scene) {
                double[][] v = new double[3][];
                for (int k = 0; k < 3; k++) {
                    v[k] = new double[] {tri.p()[k][0], tri.p()[k][1] - up, tri.p()[k][2]};
                }
                lowered.add(new WingPreview.Tri(v, tri.argb(), tri.n(), tri.opaque(), tri.additive()));
            }
            // Side on for the blow; then following it as it flies, from the side and a little behind.
            double[] eye = t <= 2.0 ? new double[] {4.2, 1.4 - up, 1.4}
                : new double[] {where[0] + 5.0, where[1] + 2.2, where[2] - 2.5};
            double[] look = t <= 2.0 ? new double[] {0.0, 0.9 - up, 1.2} : new double[] {where[0], where[1] + 0.6, where[2]};
            shots.add(new ArtPreview.Shot(names[i] + String.format(java.util.Locale.ROOT, ", tick %.1f, %.0f blocks off", t, where[2] - start[2]), her,
                ArtPreview.NO_WINGS, eye, look, lowered));
        }
        ArtPreview.sheet(file, "The Speed Kick's target: the head snaps, the body follows, launched straight away and far, tumbling, down, up", shots, 4, 340,
            300);
    }

    /** A dummy (2 high, its head 0.5) at its feet {@code at}, flying along {@code dir}, as the frame turns it. */
    static List<WingPreview.Tri> tumbled(double[] at, double[] dir, com.hex.kagune.control.pale.KickTumble.Frame frame) {
        List<WingPreview.Tri> parts = new ArrayList<>();
        // Facing her (it faced back along the kick's drive): its front toward -z.
        box(parts, new double[] {-0.25, 0.75, -0.125}, new double[] {0.25, 1.5, 0.125}, 0xFF6E7378);
        box(parts, new double[] {-0.24, 0.0, -0.12}, new double[] {0.24, 0.75, 0.12}, 0xFF5A5F64);
        List<WingPreview.Tri> head = new ArrayList<>();
        box(head, new double[] {-0.25, 1.5, -0.25}, new double[] {0.25, 2.0, 0.25}, 0xFF7C8186);
        box(head, new double[] {-0.12, 1.68, -0.27}, new double[] {0.12, 1.76, -0.25}, 0xFF2A2C30);
        // Which way round: toward where it flies (it faces -z).
        double side = dir[0] >= 0.0 ? -1.0 : 1.0;
        for (WingPreview.Tri tri : head) {
            parts.add(turn(tri, new double[] {0.0, 1.5, 0.0}, new double[] {0.0, 1.0, 0.0}, side * frame.head(), new double[] {1.0, 0.0, 0.0}, frame.pitch()));
        }
        double pivot = 1.0 * (1.0 - frame.flat()) + 0.25 * frame.flat();
        List<WingPreview.Tri> out = new ArrayList<>();
        for (WingPreview.Tri tri : parts) {
            WingPreview.Tri t = turn(tri, new double[] {0.0, pivot, 0.0}, new double[] {0.0, 1.0, 0.0}, side * frame.twist(), new double[] {dir[1], 0.0, -dir[0]},
                frame.tumble());
            double[][] v = new double[3][];
            for (int k = 0; k < 3; k++) {
                v[k] = new double[] {t.p()[k][0] + at[0], t.p()[k][1] + at[1], t.p()[k][2] + at[2]};
            }
            out.add(new WingPreview.Tri(v, t.argb(), t.n(), t.opaque(), t.additive()));
        }
        return out;
    }

    /** {@code tri} turned about {@code pivot}: first {@code a} radians about axis {@code first}, then {@code b} about {@code second}. */
    static WingPreview.Tri turn(WingPreview.Tri tri, double[] pivot, double[] first, double a, double[] second, double b) {
        double[][] v = new double[3][];
        for (int k = 0; k < 3; k++) {
            double[] p = {tri.p()[k][0] - pivot[0], tri.p()[k][1] - pivot[1], tri.p()[k][2] - pivot[2]};
            p = PoseRender.rotate(p, PoseRender.norm(first), a);
            p = PoseRender.rotate(p, PoseRender.norm(second), b);
            v[k] = new double[] {p[0] + pivot[0], p[1] + pivot[1], p[2] + pivot[2]};
        }
        double[] n = PoseRender.rotate(PoseRender.rotate(tri.n(), PoseRender.norm(first), a), PoseRender.norm(second), b);
        return new WingPreview.Tri(v, tri.argb(), n, tri.opaque(), tri.additive());
    }

    /**
     * The Speed Kick in first person, as the game draws it (6.2.0): nothing of her through the dash (she is gone); there, her own right
     * knee up in the chamber, the leg driven straight out into the dummy's middle, held out, folded, down.
     */
    static void kickFirstPerson(File file) throws Exception {
        List<ArtPreview.Shot> shots = new ArrayList<>();
        int dash = 3;
        int arrive = com.hex.kagune.control.pale.PaleRules.kickArrive(dash);
        double middle = 1.0;
        double lift = com.hex.kagune.control.pale.PaleRules.kickLift(middle);
        double[] stand = com.hex.kagune.control.pale.PaleRules.kickStand(0.25);
        double contact = arrive + com.hex.kagune.control.pale.PaleRules.KICK_CONTACT;
        double[] ages = {1.5, arrive, contact - 0.5, contact, contact + 0.6, arrive + com.hex.kagune.control.pale.PaleRules.KICK_HOLD,
            arrive + com.hex.kagune.control.pale.PaleRules.KICK_TUCK, arrive + com.hex.kagune.control.pale.PaleRules.KICK_LAND};
        String[] names = {"the lunge", "there: the chamber", "driving out", "THE BLOW", "through", "held out flat", "the backflip", "down"};
        for (int i = 0; i < ages.length; i++) {
            RigPose body = com.hex.kagune.control.client.animation.PaleMotions.kickBody(ages[i], dash, 0.0F);
            double[] shift = com.hex.kagune.control.client.animation.PaleOwnLeg.shift(body);
            double up = com.hex.kagune.control.pale.PaleRules.kickHeight(dash, lift, ages[i]);
            double s = Math.max(0.0, Math.min(1.0, (ages[i] - com.hex.kagune.control.pale.PaleRules.KICK_BURST) / dash));
            double ahead = ages[i] < arrive ? 12.0 * (1.0 - s) * (1.0 - s) : 0.0;
            double flown = ages[i] > contact ? Math.min(6.0, (ages[i] - contact) * 4.6) : 0.0;
            List<WingPreview.Tri> tris = flown > 5.5 ? new ArrayList<>() : figure(new double[] {-stand[0], 1.0 - up + flown * 0.1, -stand[1] + ahead + flown});
            box(tris, new double[] {-6.0, -0.2 - up, 0.6}, new double[] {6.0, -up, 16.0}, 0xFF4E5A44);
            com.hex.kagune.control.client.animation.PaleOwnLeg.mesh(body, shift, (a, b, c, d, n, uv, trousers) -> {
                if (trousers) {
                    return;
                }
                int colour = uv[5] * 64.0F > 29.01F ? 0xFF5A5A5A : 0xFF2E3A8C;
                tris.add(new WingPreview.Tri(new double[][] {a, b, c}, new int[] {colour, colour, colour}, n, true));
                tris.add(new WingPreview.Tri(new double[][] {a, c, d}, new int[] {colour, colour, colour}, n, true));
            });
            // Her view on its middle (PaleCamera's lock-on).
            shots.add(new ArtPreview.Shot(names[i] + String.format(java.util.Locale.ROOT, ", tick %.2f", ages[i]), null, ArtPreview.NO_WINGS,
                new double[] {0.0, 1.62, 0.0}, new double[] {-stand[0], middle - up, -stand[1] + ahead}, tris));
        }
        ArtPreview.sheet(file, "The Speed Kick in first person: there, the knee up, the leg driven straight out into its middle", shots, 4, 360, 300);
    }

    /**
     * 6.2.0: the Speed Blitz in first person, as the game draws it there (PaleFleshRenderer.arms): no body but her own two arms (PaleOwnArm,
     * a plain skin: pale arms, the jacket's dark sleeves to the elbow) posed exactly as third person poses them, each strike chained out
     * of the last one's hold as in the game, seen from where her head has gone; the camera at her eyes, looking at the dummy.
     */
    static void blitzFirstPerson(File file) throws Exception {
        List<ArtPreview.Shot> shots = new ArrayList<>();
        com.hex.kagune.control.pale.PaleStrike[] chain = {com.hex.kagune.control.pale.PaleStrike.JAB, com.hex.kagune.control.pale.PaleStrike.CROSS,
            com.hex.kagune.control.pale.PaleStrike.HOOK, com.hex.kagune.control.pale.PaleStrike.LIVER, com.hex.kagune.control.pale.PaleStrike.UPPERCUT,
            com.hex.kagune.control.pale.PaleStrike.TWIN_PALM};
        double stand = com.hex.kagune.control.pale.PaleRules.strikeStand(0.6);
        RigPose from = null;
        for (com.hex.kagune.control.pale.PaleStrike strike : chain) {
            double[] ages = {1.0, strike.impact, strike.chain};
            String[] names = {"the coil", "IMPACT", "the hold"};
            for (int i = 0; i < ages.length; i++) {
                if (strike != com.hex.kagune.control.pale.PaleStrike.JAB && i == 0) {
                    continue; // chained: its coil is the last one's hold
                }
                RigPose body = com.hex.kagune.control.client.animation.PaleMotions.strike(strike, ages[i], from);
                body.settle(1.0F);
                shots.add(armsShot(strike.name().toLowerCase(java.util.Locale.ROOT) + ": " + names[i] + String.format(java.util.Locale.ROOT, ", tick %.0f", ages[i]),
                    body, stand));
            }
            from = com.hex.kagune.control.client.animation.PaleMotions.strike(strike, strike.chain, from);
            from.settle(1.0F);
        }
        RigPose guard = com.hex.kagune.control.client.animation.PaleMotions.guard();
        guard.settle(1.0F);
        shots.add(armsShot("the guard after", guard, stand));
        ArtPreview.sheet(file, "The Speed Blitz in first person: both her arms, posed as third person poses them, chained strike to strike", shots, 4, 360, 300);
    }

    /** Her own two arms on {@code body} from her eyes (a plain skin), a dummy {@code stand} blocks ahead. */
    static ArtPreview.Shot armsShot(String label, RigPose body, double stand) {
        double[] shift = com.hex.kagune.control.client.animation.PaleOwnLeg.shift(body);
        List<WingPreview.Tri> tris = figure(new double[] {0.0, 1.0, stand});
        box(tris, new double[] {-6.0, -0.2, 0.6}, new double[] {6.0, 0.0, 12.0}, 0xFF4E5A44);
        com.hex.kagune.control.client.animation.PaleOwnArm.both(body, shift, false, (a, b, c, d, n, uv, sleeve) -> {
            if (sleeve) {
                return;
            }
            // A plain skin: the jacket's dark sleeve down to the elbow (the arm's first six pixels), pale skin below and on the hand.
            double u = (uv[0] + uv[2] + uv[4] + uv[6]) * 16.0;
            double v = (uv[1] + uv[3] + uv[5] + uv[7]) * 16.0;
            boolean leftArm = v >= 48.0;
            double along = v - (leftArm ? 52.0 : 20.0);
            boolean hand = along < 0.0 ? u - (leftArm ? 32.0 : 40.0) > 8.0 : along >= 6.0;
            int colour = hand ? 0xFFE9CDB8 : 0xFF22222A;
            tris.add(new WingPreview.Tri(new double[][] {a, b, c}, new int[] {colour, colour, colour}, n, true));
            tris.add(new WingPreview.Tri(new double[][] {a, c, d}, new int[] {colour, colour, colour}, n, true));
        });
        return new ArtPreview.Shot(label, null, ArtPreview.NO_WINGS, new double[] {0.0, 1.62, 0.0}, new double[] {0.0, 1.35, stand}, tris);
    }

    /** One Watch This tentacle on {@code body} as the game draws it (before its weight swings it), as triangles. */
    static List<WingPreview.Tri> tentacle(RigPose body, int side, com.hex.kagune.control.client.animation.WatchPose.Tip tip, double time) {
        return flesh(com.hex.kagune.control.client.animation.WatchPose.pose(body, side, tip, time, side > 0 ? 3 : 7), List.of());
    }

    /**
     * Watch This (B): the cross as the two tentacles burst out of her back, poised over her shoulders, one rearing and lunging at a
     * body ahead of her, the beak through its chest, the body lifted up over her and thrown out to that side; the beak up close.
     */
    static void watch(File file) throws Exception {
        List<ArtPreview.Shot> shots = new ArrayList<>();
        com.hex.kagune.control.pale.WatchRules.Phase out = com.hex.kagune.control.pale.WatchRules.Phase.OUT;
        com.hex.kagune.control.pale.WatchRules.Phase idle = com.hex.kagune.control.pale.WatchRules.Phase.IDLE;
        com.hex.kagune.control.pale.WatchRules.Phase strike = com.hex.kagune.control.pale.WatchRules.Phase.STRIKE;
        com.hex.kagune.control.pale.WatchRules.Phase carry = com.hex.kagune.control.pale.WatchRules.Phase.CARRY;
        com.hex.kagune.control.pale.WatchRules.Phase recover = com.hex.kagune.control.pale.WatchRules.Phase.RECOVER;
        double chestUp = 2.0 * com.hex.kagune.control.pale.WatchRules.CHEST;
        double[] mark = {1.6, chestUp, 3.2};
        int lunge = com.hex.kagune.control.pale.WatchRules.lungeTicks(Math.hypot(mark[0] - 0.55, Math.hypot(mark[1] - 2.3, mark[2] + 0.3)));
        double[] front = {4.2, 2.8, 5.6};
        double[] wide = {6.5, 4.5, 7.5};
        double[] high = {11.0, 6.5, 11.0};
        double[] look = {0.3, 1.9, 1.0};
        Object[][] frames = {
            {"bursting out (her cross)", 5.0, out, 1.0, idle, 0.0, null, front},
            {"the cross", 8.0, out, 4.5, out, 4.5, null, front},
            {"poised, hunting", 16.0, idle, 0.0, idle, 0.0, null, front},
            {"poised, from behind", 16.0, idle, 0.0, idle, 0.0, null, new double[] {-3.2, 3.6, -4.6}},
            {"rearing back", 30.0, strike, (double)com.hex.kagune.control.pale.WatchRules.REAR, idle, 0.0, mark, wide},
            {"the lunge", 30.0, strike, com.hex.kagune.control.pale.WatchRules.REAR + lunge * 0.55, idle, 0.0, mark, wide},
            {"IMPALED", 30.0, carry, 0.0, idle, 0.0, mark, wide},
            {"lifted", 30.0, carry, 0.35, idle, 0.0, mark, high},
            {"over the top", 30.0, carry, 0.6, idle, 0.0, mark, high},
            {"thrown out to her right", 30.0, recover, 0.0, idle, 0.0, mark, high},
            {"the left one: over to her left", 30.0, idle, 0.0, carry, 0.55, new double[] {-1.6, chestUp, 3.2}, high}};
        for (Object[] f : frames) {
            double bodyAge = (Double)f[1];
            RigPose body = bodyAge < com.hex.kagune.control.pale.WatchRules.POSE_TICKS
                ? com.hex.kagune.control.client.animation.PaleMotions.watch(bodyAge, null) : com.hex.kagune.control.client.animation.PaleMotions.guard();
            body.settle(1.0F);
            List<WingPreview.Tri> scene = new ArrayList<>();
            box(scene, new double[] {-7.0, -0.2, -4.0}, new double[] {7.0, 0.0, 8.0}, 0xFF4E5A44);
            double[] target = (double[])f[6];
            for (int side : new int[] {1, -1}) {
                com.hex.kagune.control.pale.WatchRules.Phase phase = (com.hex.kagune.control.pale.WatchRules.Phase)(side > 0 ? f[2] : f[4]);
                double age = (Double)(side > 0 ? f[3] : f[5]);
                double[] chest = null;
                double[] from = null;
                double tipAge = age;
                if (phase == carry && target != null) {
                    chest = com.hex.kagune.control.pale.WatchRules.carry(age, target, side);
                    tipAge = 4.0 + age * 12.0;
                    scene.addAll(figure(new double[] {chest[0], chest[1] - chestUp + 1.0, chest[2]}));
                } else if (phase == strike && target != null) {
                    chest = target;
                    scene.addAll(figure(new double[] {target[0], 1.0, target[2]}));
                } else if (phase == recover && target != null) {
                    from = com.hex.kagune.control.pale.WatchRules.carry(1.0, target, side);
                    double[] v = com.hex.kagune.control.pale.WatchRules.throwVelocity(target, side);
                    scene.addAll(figure(new double[] {from[0] + v[0] * 1.5, from[1] - chestUp + 1.0 + v[1] * 1.5, from[2] + v[2] * 1.5}));
                }
                scene.addAll(tentacle(body, side, com.hex.kagune.control.client.animation.WatchPose.tip(side, phase, tipAge, lunge, chest, from, 9.0), 9.0));
            }
            shots.add(new ArtPreview.Shot((String)f[0], body, ArtPreview.NO_WINGS, (double[])f[7], f[7] == high ? new double[] {0.0, 3.0, 0.5} : look, scene));
        }
        // The beak up close, poised.
        RigPose stance = com.hex.kagune.control.client.animation.PaleMotions.guard();
        stance.settle(1.0F);
        com.hex.kagune.control.client.animation.WatchPose.Tip poised = com.hex.kagune.control.client.animation.WatchPose.idle(1, 0.0);
        double[] k = poised.knot();
        shots.add(new ArtPreview.Shot("the beak up close", stance, ArtPreview.NO_WINGS, new double[] {k[0] + 1.6, k[1] + 0.5, k[2] + 1.4},
            new double[] {k[0], k[1] - 0.2, k[2] + 0.5}, tentacle(stance, 1, poised, 0.0)));
        ArtPreview.sheet(file, "Watch This (B): two tentacles out of her back, each a long beak of bone, hunting, impaling, lifting, throwing", shots, 4, 360,
            340);
    }

    /**
     * One frame of the Mutated Arm: her body under its weight ({@link com.hex.kagune.control.client.animation.ArmPose#body}), the arm
     * out of her right shoulder as the game draws it (before its weight swings it), what it holds at its hand, the sphere.
     */
    static RigPose armFrame(List<WingPreview.Tri> scene, com.hex.kagune.control.pale.ArmRules.Phase phase, double age, double from, double charge,
                            double held, boolean holding, boolean afterSphere, double[] look, double[] sphere, double radius) {
        com.hex.kagune.control.client.animation.ArmPose.Reach reach = com.hex.kagune.control.client.animation.ArmPose.reach(phase, age, from, charge,
            held, holding, afterSphere);
        // Her own arm pointed where it goes, the flesh out of her hand; its reach measured from her shoulder (as the game does).
        RigPose body = com.hex.kagune.control.client.animation.PaleMotions.guard();
        com.hex.kagune.control.client.animation.ArmPose.body(body, phase, age, reach, look, afterSphere, 9.0 + age);
        body.settle(1.0F);
        double[] aim = com.hex.kagune.control.client.animation.ArmPose.aimNow(look, reach.length(), com.hex.kagune.control.client.animation.ArmPose.way(body));
        double[] shoulder = com.hex.kagune.control.client.animation.ArmPose.shoulder(body);
        double[] root = com.hex.kagune.control.client.animation.ArmPose.root(body);
        double girth;
        double[] end;
        if (sphere != null) {
            girth = com.hex.kagune.control.client.animation.ArmPose.girth(reach.girth(), com.hex.kagune.control.client.animation.ArmPose.distance(shoulder, sphere));
            end = com.hex.kagune.control.client.animation.ArmPose.onSphere(root, sphere, radius);
            double[] state = com.hex.kagune.control.pale.ArmRules.sphere(age);
            if (state[0] < 0.999) {
                scene.addAll(figure(new double[] {sphere[0], sphere[1] - 0.15, sphere[2]}));
            }
            com.hex.kagune.control.pale.PaleFleshModel.cocoon((a, b, c, d, n, ca, cb, cc, cd) -> {
                scene.add(new WingPreview.Tri(new double[][] {a, b, c}, new int[] {ca, cb, cc}, n, true));
                scene.add(new WingPreview.Tri(new double[][] {a, c, d}, new int[] {ca, cc, cd}, n, true));
            }, sphere, com.hex.kagune.control.client.animation.ArmPose.back(root, sphere), radius, state[0], state[1], 9.0 + age, 5);
        } else if (holding) {
            double[] at = {shoulder[0] + aim[0] * reach.length(), shoulder[1] + aim[1] * reach.length(), shoulder[2] + aim[2] * reach.length()};
            scene.addAll(figure(at));
            girth = com.hex.kagune.control.client.animation.ArmPose.girth(reach.girth(), reach.length());
            end = com.hex.kagune.control.client.animation.ArmPose.holding(root, at, girth);
        } else {
            girth = com.hex.kagune.control.client.animation.ArmPose.girth(reach.girth(), reach.length());
            end = com.hex.kagune.control.client.animation.ArmPose.out(shoulder, aim, reach.length(), girth);
        }
        scene.addAll(flesh(com.hex.kagune.control.client.animation.ArmPose.pose(body, end, girth, reach.hand(), 9.0 + age, 8), List.of()));
        return body;
    }

    /** Ground under her out to {@code ahead} blocks, in two-block tiles (one slab across the near plane is dropped whole). */
    static List<WingPreview.Tri> tiles(double ahead) {
        List<WingPreview.Tri> tris = new ArrayList<>();
        for (double x = -6.0; x < 6.0; x += 2.0) {
            for (double z = -6.0; z < ahead; z += 2.0) {
                box(tris, new double[] {x, -0.2, z}, new double[] {x + 2.0, 0.0, z + 2.0}, ((int)(x + z) & 2) == 0 ? 0xFF4E5A44 : 0xFF4A5640);
            }
        }
        return tris;
    }

    /** Where the middle of the Mutated Arm's hand is (rig space), as {@link #armFrame} draws it. */
    static double[] armHand(com.hex.kagune.control.pale.ArmRules.Phase phase, double age, double from, double charge, double held, double[] look) {
        com.hex.kagune.control.client.animation.ArmPose.Reach reach = com.hex.kagune.control.client.animation.ArmPose.reach(phase, age, from, charge,
            held, false, false);
        RigPose body = com.hex.kagune.control.client.animation.PaleMotions.guard();
        com.hex.kagune.control.client.animation.ArmPose.body(body, phase, age, reach, look, false, 9.0 + age);
        body.settle(1.0F);
        double[] aim = com.hex.kagune.control.client.animation.ArmPose.aimNow(look, reach.length(), com.hex.kagune.control.client.animation.ArmPose.way(body));
        double[] root = com.hex.kagune.control.client.animation.ArmPose.shoulder(body);
        return new double[] {root[0] + aim[0] * reach.length(), root[1] + aim[1] * reach.length(), root[2] + aim[2] * reach.length()};
    }

    /**
     * The Mutated Arm (N): her own arm pointed out and the flesh bursting out of her hand; tapped, it shoots out, catches, hauls what it
     * caught back to her and flings it; held, it swells and reaches
     * out to 35 blocks; at the full charge it folds into a sphere round what it holds and crushes it, held out on the stretched arm,
     * then draws slowly back into her, the mutation going down.
     */
    static void arm(File file) throws Exception {
        com.hex.kagune.control.pale.ArmRules.Phase shoot = com.hex.kagune.control.pale.ArmRules.Phase.SHOOT;
        com.hex.kagune.control.pale.ArmRules.Phase hold = com.hex.kagune.control.pale.ArmRules.Phase.HOLD;
        com.hex.kagune.control.pale.ArmRules.Phase drag = com.hex.kagune.control.pale.ArmRules.Phase.DRAG;
        com.hex.kagune.control.pale.ArmRules.Phase sphere = com.hex.kagune.control.pale.ArmRules.Phase.SPHERE;
        com.hex.kagune.control.pale.ArmRules.Phase retract = com.hex.kagune.control.pale.ArmRules.Phase.RETRACT;
        List<ArtPreview.Shot> shots = new ArrayList<>();
        double aheadLength = Math.sqrt(0.05 * 0.05 + 0.04 * 0.04 + 1.0);
        double[] ahead = {0.05 / aheadLength, -0.04 / aheadLength, 1.0 / aheadLength};
        double held = com.hex.kagune.control.pale.PaleRules.HOLD_TICKS + com.hex.kagune.control.pale.ArmRules.SHOOT_TICKS;
        double caught = 8.0;
        // {label, phase, age, from, charge, held before, holding, after the sphere, eye, look}
        double[] side = {11.5, 4.5, 4.0};
        double[] sideLook = {0.0, 1.0, 4.4};
        double[] near = {4.4, 2.6, 4.4};
        double[] nearLook = {0.3, 1.1, 1.8};
        double[] behind = {3.4, 4.4, -7.0};
        double[] behindLook = {0.3, 0.9, 6.0};
        double[] level = {0.03, 0.02, 0.9993};
        Object[][] frames = {
            {"tap: shot out", shoot, 1.0, com.hex.kagune.control.pale.ArmRules.ARM, 0.0, 0.0, false, false, side, sideLook},
            {"caught, 8 blocks out", drag, 0.0, caught, 0.0, 0.0, true, false, side, sideLook},
            {"hauled in", drag, 6.0, caught, 0.0, 0.0, true, false, near, nearLook},
            {"flung out to her right; the arm going back in", retract, 3.0, com.hex.kagune.control.pale.ArmRules.dragTo(0.0), 0.0, 0.0, false, false, near,
                nearLook},
            {"held: swelling, 1 s", hold, 8.0, 0.0, 0.0, held, false, false, behind, behindLook},
            {"held: carrying what it caught, 2 s", hold, 28.0, 0.0, 0.0, held, true, false, behind, behindLook},
            {"the full charge: 35 blocks out", hold, 48.0, 0.0, 0.0, held, false, false, behind, new double[] {0.3, 0.9, 9.0}},
            {"her weight under it", hold, 48.0, 0.0, 0.0, held, false, false, new double[] {4.6, 1.5, 0.2}, new double[] {0.0, 1.0, 0.9}}};
        ArtPreview.bodyScale = com.hex.kagune.control.client.animation.BodyFrame.SCALE;
        try {
            for (Object[] f : frames) {
                List<WingPreview.Tri> scene = new ArrayList<>();
                scene.addAll(tiles(40.0));
                RigPose body = armFrame(scene, (com.hex.kagune.control.pale.ArmRules.Phase)f[1], (Double)f[2], (Double)f[3], (Double)f[4], (Double)f[5],
                    (Boolean)f[6], (Boolean)f[7], f[1] == hold ? level : ahead, null, 0.0);
                shots.add(new ArtPreview.Shot((String)f[0], body, ArtPreview.NO_WINGS, (double[])f[8], (double[])f[9], scene));
            }
            // The sphere (drawn 6 blocks out so it reads here; in game it is wherever the arm got to, up to 35).
            double out = 6.0;
            double radius = com.hex.kagune.control.pale.ArmRules.sphereRadius(2.0);
            double[] sphereEye = {10.5, 3.4, 3.4};
            double[] sphereLook = {0.3, 1.3, 3.5};
            Object[][] crush = {
                {"the sphere: folding in over it", 4.0},
                {"shut", (double)com.hex.kagune.control.pale.ArmRules.SPHERE_FORM},
                {"CRUSHED (first squeeze)", (double)com.hex.kagune.control.pale.ArmRules.squeezeTick(0)},
                {"CRUSHED (last squeeze)", (double)com.hex.kagune.control.pale.ArmRules.squeezeTick(com.hex.kagune.control.pale.ArmRules.SQUEEZES - 1)},
                {"held out on the stretched arm", 28.0},
                {"opening", (double)com.hex.kagune.control.pale.ArmRules.SPHERE_TICKS - 2.0}};
            for (Object[] f : crush) {
                List<WingPreview.Tri> scene = new ArrayList<>();
                scene.addAll(tiles(12.0));
                RigPose probe = com.hex.kagune.control.client.animation.PaleMotions.guard();
                probe.settle(1.0F);
                double[] root = com.hex.kagune.control.client.animation.ArmPose.shoulder(probe);
                double[] centre = {root[0] + ahead[0] * out, root[1] + ahead[1] * out + 0.3, root[2] + ahead[2] * out};
                RigPose body = armFrame(scene, sphere, (Double)f[1], out, 1.0, 0.0, true, true, ahead, centre, radius);
                shots.add(new ArtPreview.Shot((String)f[0], body, ArtPreview.NO_WINGS, sphereEye, sphereLook, scene));
            }
            Object[][] back = {
                {"drawing back in, slowly", 8.0},
                {"the mutation going down", 22.0},
                {"her own arm again", 29.5}};
            for (Object[] f : back) {
                List<WingPreview.Tri> scene = new ArrayList<>();
                scene.addAll(tiles(12.0));
                RigPose body = armFrame(scene, retract, (Double)f[1], out, 1.0, 0.0, false, true, ahead, null, 0.0);
                shots.add(new ArtPreview.Shot((String)f[0], body, ArtPreview.NO_WINGS, f[1].equals(8.0) ? sphereEye : near,
                    f[1].equals(8.0) ? sphereLook : nearLook, scene));
            }
            // Up close: the hand reaching, the hand round a body; and in first person.
            List<WingPreview.Tri> reaching = new ArrayList<>();
            reaching.addAll(tiles(40.0));
            RigPose reachBody = armFrame(reaching, hold, 8.0, 0.0, 0.0, held, false, false, ahead, null, 0.0);
            double[] hand = armHand(hold, 8.0, 0.0, 0.0, held, ahead);
            shots.add(new ArtPreview.Shot("its hand, reaching", reachBody, ArtPreview.NO_WINGS, new double[] {hand[0] + 3.6, hand[1] + 1.6, hand[2] - 3.2}, hand,
                reaching));
            List<WingPreview.Tri> gripping = new ArrayList<>();
            gripping.addAll(tiles(40.0));
            RigPose gripBody = armFrame(gripping, drag, 0.0, caught, 0.0, 0.0, true, false, ahead, null, 0.0);
            double[] grip = armHand(drag, 0.0, caught, 0.0, 0.0, ahead);
            shots.add(new ArtPreview.Shot("its hand round what it caught", gripBody, ArtPreview.NO_WINGS, new double[] {grip[0] + 3.0, grip[1] + 1.4,
                grip[2] - 3.0}, grip, gripping));
            List<WingPreview.Tri> fp = new ArrayList<>();
            box(fp, new double[] {-6.0, -0.2, 0.6}, new double[] {6.0, 0.0, 40.0}, 0xFF4E5A44);
            RigPose fpBody = armFrame(fp, hold, 20.0, 0.0, 0.0, held, false, false, ahead, null, 0.0);
            // Her own arm, as the game draws it in first person (a plain skin: a pale hand, a dark sleeve).
            com.hex.kagune.control.client.animation.PaleOwnArm.mesh(fpBody, com.hex.kagune.control.client.animation.PaleOwnLeg.shift(fpBody), false,
                (a, b, c, d, n, uv, sleeve) -> {
                    if (sleeve) {
                        return;
                    }
                    int colour = uv[5] * 64.0F > 26.01F ? 0xFFE8C8B0 : 0xFF2A2630;
                    fp.add(new WingPreview.Tri(new double[][] {a, b, c}, new int[] {colour, colour, colour}, n, true));
                    fp.add(new WingPreview.Tri(new double[][] {a, c, d}, new int[] {colour, colour, colour}, n, true));
                });
            List<WingPreview.Tri> seen = new ArrayList<>();
            double[] eye = {0.0, 1.62, 0.0};
            for (WingPreview.Tri tri : fp) {
                boolean close = true;
                for (double[] v : tri.p()) {
                    close &= Math.sqrt((v[0] - eye[0]) * (v[0] - eye[0]) + (v[1] - eye[1]) * (v[1] - eye[1]) + (v[2] - eye[2]) * (v[2] - eye[2])) < 0.35;
                }
                if (!close) {
                    seen.add(tri);
                }
            }
            shots.add(new ArtPreview.Shot("first person", null, ArtPreview.NO_WINGS, eye, new double[] {0.0, 1.3, 8.0}, seen));
            ArtPreview.sheet(file, "The Mutated Arm (N): shot out, caught, hauled in; held, swelling out to 35 blocks; the sphere that crushes; drawn back in",
                shots, 4, 380, 330);
        } finally {
            ArtPreview.bodyScale = 1.0;
        }
    }

    /** A slab of ground under her, {@code ahead} blocks out in front, to see where the club comes down. */
    static List<WingPreview.Tri> ground(double ahead) {
        List<WingPreview.Tri> tris = new ArrayList<>();
        box(tris, new double[] {-1.5, -0.2, -1.0}, new double[] {1.5, 0.0, ahead}, 0xFF4E5A44);
        return tris;
    }

    /** Each flip along its turn, seen from the side it turns across, and the landing. */
    static void flips(File file) throws Exception {
        List<ArtPreview.Shot> shots = new ArrayList<>();
        double[] ages = {0.5, 3.0, 6.0, 9.0, 12.0, 15.0};
        for (com.hex.kagune.control.pale.PaleRules.Flip flip : com.hex.kagune.control.pale.PaleRules.Flip.values()) {
            boolean side = flip == com.hex.kagune.control.pale.PaleRules.Flip.LEFT || flip == com.hex.kagune.control.pale.PaleRules.Flip.RIGHT;
            double[] eye = side ? new double[] {0.4, 1.3, 4.2} : new double[] {4.2, 1.3, 0.4};
            for (double age : ages) {
                RigPose p = com.hex.kagune.control.client.animation.PaleMotions.flip(flip, age, -1.0);
                p.settle(1.0F);
                shots.add(new ArtPreview.Shot(flip.name().toLowerCase(java.util.Locale.ROOT) + " flip, tick " + age, p, ArtPreview.NO_WINGS, eye,
                    new double[] {0.0, 0.9, 0.0}, List.of()));
            }
            RigPose land = com.hex.kagune.control.client.animation.PaleMotions.flip(flip, 20.0, 1.5);
            land.settle(1.0F);
            shots.add(new ArtPreview.Shot(flip.name().toLowerCase(java.util.Locale.ROOT) + " flip: landing", land, ArtPreview.NO_WINGS, eye,
                new double[] {0.0, 0.9, 0.0}, List.of()));
        }
        ArtPreview.sheet(file, "Pale's flips (right click + W S A D): the turn and the landing", shots, 7, 260, 300);
    }

    // ------------------------------------------------------------------ idle

    /** Her stance on either leg and each gesture at its height, front three quarters. */
    static void idle(File file) throws Exception {
        List<ArtPreview.Shot> shots = new ArrayList<>();
        double[] eye = {2.3, 1.5, 3.6};
        double[] target = {0.0, 1.0, 0.0};
        for (int side : new int[] {1, -1}) {
            RigPose p = com.hex.kagune.control.client.animation.PaleIdle.compose(side, 0.0, null, 0.0F, 1);
            p.settle(1.0F);
            shots.add(new ArtPreview.Shot("Stance: weight on her " + (side > 0 ? "right" : "left") + " leg", p, ArtPreview.NO_WINGS, eye, target, List.of()));
        }
        float[] peaks = {0.45F, 0.6F, 0.5F, 0.25F, 0.12F, 0.5F, 0.08F, 0.36F, 0.12F, 0.42F};
        com.hex.kagune.control.client.animation.PaleIdle.Gesture[] all = com.hex.kagune.control.client.animation.PaleIdle.Gesture.values();
        for (int i = 0; i < all.length; i++) {
            RigPose p = com.hex.kagune.control.client.animation.PaleIdle.compose(1, 0.0, all[i], peaks[i], 1);
            p.settle(1.0F);
            shots.add(new ArtPreview.Shot(all[i].name().toLowerCase(java.util.Locale.ROOT).replace('_', ' '), p, ArtPreview.NO_WINGS, eye, target,
                List.of()));
        }
        ArtPreview.sheet(file, "Pale's base form at rest: hip-shot stance and her gestures (each at its height)", shots, 4, 340, 400);
    }

    // ------------------------------------------------------------------ steam

    /** One simulated wisp, moved exactly as PaleSteam.Wisp moves (no Minecraft). */
    static final class Wisp {
        double x, y, z, vx, vy, vz;
        float a0, a1, a2, s0, s1, s2, curl, phase, peak;
        int age, life;
    }

    /** Wisps off a body standing still ({@code lean} forward) after {@code ticks} ticks of steam. */
    static List<Wisp> simulate(long seed, int ticks, double lean, double speedZ) {
        Random random = new Random(seed);
        List<Wisp> wisps = new ArrayList<>();
        float owed = 0.0F;
        double bodyZ = 0.0;
        for (int tick = 0; tick < ticks; tick++) {
            bodyZ += speedZ;
            owed += PaleSteamRules.rate(1.0F, Math.abs(speedZ), false);
            while (owed >= 1.0F) {
                owed -= 1.0F;
                PaleSteamRules.Point point = PaleSteamRules.pick(random.nextDouble(), false);
                double[] at = PaleSteamRules.place(point, lean, 0.0, 0.0, 1.0, random.nextGaussian() * 0.05, random.nextGaussian() * 0.05,
                    random.nextGaussian() * 0.05);
                Wisp w = new Wisp();
                w.x = at[0];
                w.y = at[1];
                w.z = at[2] + bodyZ;
                double out = Math.max(1.0E-4, Math.hypot(point.x(), point.z()));
                w.vx = point.x() / out * PaleSteamRules.OUTWARD;
                w.vz = speedZ * PaleSteamRules.INHERIT + point.z() / out * PaleSteamRules.OUTWARD;
                w.vy = PaleSteamRules.RISE_MIN + random.nextDouble() * (PaleSteamRules.RISE_MAX - PaleSteamRules.RISE_MIN);
                w.life = PaleSteamRules.LIFE_MIN + random.nextInt(PaleSteamRules.LIFE_MAX - PaleSteamRules.LIFE_MIN + 1);
                w.a0 = random.nextFloat() * 6.2832F;
                w.a1 = random.nextFloat() * 6.2832F;
                w.a2 = random.nextFloat() * 6.2832F;
                w.s0 = (random.nextFloat() - 0.5F) * PaleSteamRules.TUMBLE;
                w.s1 = (random.nextFloat() - 0.5F) * PaleSteamRules.TUMBLE;
                w.s2 = (random.nextFloat() - 0.5F) * PaleSteamRules.TUMBLE;
                w.curl = (random.nextBoolean() ? 1.0F : -1.0F) * (0.6F + 0.8F * random.nextFloat());
                w.phase = random.nextFloat() * 6.2832F;
                w.peak = PaleSteamRules.PEAK_ALPHA * (0.75F + 0.5F * random.nextFloat());
                wisps.add(w);
            }
            for (Wisp w : wisps) {
                w.age++;
                w.a0 += w.s0;
                w.a1 += w.s1;
                w.a2 += w.s2;
                w.phase += PaleSteamRules.CURL_RATE;
                w.vx = w.vx * PaleSteamRules.DRAG + Math.cos(w.phase) * PaleSteamRules.CURL * w.curl + (random.nextFloat() - 0.5F) * PaleSteamRules.WANDER;
                w.vz = w.vz * PaleSteamRules.DRAG + Math.sin(w.phase) * PaleSteamRules.CURL * w.curl + (random.nextFloat() - 0.5F) * PaleSteamRules.WANDER;
                w.vy = w.vy * PaleSteamRules.DRAG + PaleSteamRules.BUOYANCY;
                w.x += w.vx;
                w.y += w.vy;
                w.z += w.vz;
            }
            wisps.removeIf(w -> w.age >= w.life);
        }
        return wisps;
    }

    /** The wisps as triangles seen from {@code eye}: three crossed planes each, as opaque as each faces the eye (as the game draws them). */
    static List<WingPreview.Tri> steamTris(List<Wisp> wisps, double[] eye, float boost) {
        List<WingPreview.Tri> tris = new ArrayList<>();
        for (Wisp w : wisps) {
            float t = Math.min(1.0F, w.age / (float)w.life);
            float alpha = w.peak * PaleSteamRules.opacity(t, w.age) * boost;
            float size = PaleSteamRules.size(t);
            float[] axes = PaleSteamRules.axes(w.a0, w.a1, w.a2);
            double dx = w.x - eye[0];
            double dy = w.y - eye[1];
            double dz = w.z - eye[2];
            double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            for (int plane = 0; plane < 3; plane++) {
                int n = plane * 3;
                double facing = (axes[n] * dx + axes[n + 1] * dy + axes[n + 2] * dz) / d;
                int a = (int)Math.round(255.0 * alpha * facing * facing);
                if (a <= 0) {
                    continue;
                }
                int u = ((plane + 1) % 3) * 3;
                int v = ((plane + 2) % 3) * 3;
                // A soft disc: a fan of triangles, opaque in the middle, clear at the rim (the texture's falloff).
                int sides = 12;
                double[] centre = {w.x, w.y, w.z};
                int colour = PaleSteamRules.COLOUR;
                for (int k = 0; k < sides; k++) {
                    double t0 = k * Math.PI * 2.0 / sides;
                    double t1 = (k + 1) * Math.PI * 2.0 / sides;
                    double r0 = size * (0.85 + 0.15 * Math.sin(k * 2.3 + w.phase));
                    double r1 = size * (0.85 + 0.15 * Math.sin((k + 1) * 2.3 + w.phase));
                    double[] p0 = {w.x + (axes[u] * Math.cos(t0) + axes[v] * Math.cos(t0 + Math.PI / 2)) * r0,
                        w.y + (axes[u + 1] * Math.cos(t0) + axes[v + 1] * Math.sin(t0)) * r0,
                        w.z + (axes[u + 2] * Math.cos(t0) + axes[v + 2] * Math.sin(t0)) * r0};
                    p0 = new double[] {w.x + (axes[u] * Math.cos(t0) + axes[v] * Math.sin(t0)) * r0,
                        w.y + (axes[u + 1] * Math.cos(t0) + axes[v + 1] * Math.sin(t0)) * r0,
                        w.z + (axes[u + 2] * Math.cos(t0) + axes[v + 2] * Math.sin(t0)) * r0};
                    double[] p1 = {w.x + (axes[u] * Math.cos(t1) + axes[v] * Math.sin(t1)) * r1,
                        w.y + (axes[u + 1] * Math.cos(t1) + axes[v + 1] * Math.sin(t1)) * r1,
                        w.z + (axes[u + 2] * Math.cos(t1) + axes[v + 2] * Math.sin(t1)) * r1};
                    tris.add(new WingPreview.Tri(new double[][] {centre, p0, p1}, new int[] {a << 24 | colour, colour, colour},
                        new double[] {axes[n], axes[n + 1], axes[n + 2]}, false));
                }
            }
        }
        return tris;
    }

    static void steam(File file) throws Exception {
        RigPose still = new RigPose();
        List<Wisp> standing = simulate(61, 90, 0.0, 0.0);
        List<Wisp> walking = simulate(62, 90, 0.0, 0.13);
        List<ArtPreview.Shot> shots = new ArrayList<>();
        Object[][] views = {
            {"Standing: front, three quarters", new double[] {2.4, 1.7, 3.4}},
            {"Standing: back, three quarters", new double[] {-2.4, 1.9, -3.4}},
            {"Standing: side", new double[] {4.0, 1.4, 0.0}},
            {"Standing: from above", new double[] {0.6, 4.2, 1.4}},
        };
        for (Object[] view : views) {
            double[] eye = (double[])view[1];
            shots.add(new ArtPreview.Shot((String)view[0], still, ArtPreview.NO_WINGS, eye, new double[] {0.0, 1.35, 0.0}, steamTris(standing, eye, 1.0F)));
        }
        for (Wisp w : walking) {
            w.z -= 90 * 0.13; // she is drawn where she has walked to
        }
        double[] eye = {2.6, 1.7, 3.2};
        shots.add(new ArtPreview.Shot("Walking toward the camera: no trail", still, ArtPreview.NO_WINGS, eye,
            new double[] {0.0, 1.35, 0.0}, steamTris(walking, eye, 1.0F)));
        double[] closeEye = {1.3, 1.6, 1.6};
        shots.add(new ArtPreview.Shot("Up close, 3x as dense to see the shapes", still, ArtPreview.NO_WINGS, closeEye,
            new double[] {0.0, 1.45, 0.0}, steamTris(standing, closeEye, 3.0F)));
        System.out.println("steam wisps alive while standing: " + standing.size() + ", walking: " + walking.size());
        ArtPreview.sheet(file, "Pale's base form: steam off the skin (Photon, three crossed planes per wisp, very low opacity)", shots, 3, 460, 460);
    }
}
