package com.hex.kagune.control;

import com.hex.kagune.control.client.PathCards;
import com.hex.kagune.control.client.PathChoiceLayout;
import com.hex.kagune.control.client.PathChoiceLayout.Box;
import com.hex.kagune.control.client.PathSilhouette;
import com.hex.kagune.gameplay.KaguneForm;
import java.util.ArrayList;
import java.util.List;

/**
 * Dependency-free tests for the path choice screen's geometry: layout at every GUI size Minecraft can produce,
 * hit testing, and the animated silhouettes staying inside their cards. Also checks the Ayato stage table.
 */
public final class PathScreenTests {
    private static int passed;
    private static final List<String> FAILURES = new ArrayList<>();

    private static void check(boolean condition, String description) {
        if (condition) {
            passed++;
        } else {
            FAILURES.add(description);
        }
    }

    public static void main(String[] args) {
        layouts();
        hitTesting();
        silhouettes();
        stages();
        framework();
        if (!FAILURES.isEmpty()) {
            FAILURES.stream().limit(40).forEach(failure -> System.err.println("FAILED: " + failure));
            System.err.println(FAILURES.size() + " of " + (passed + FAILURES.size()) + " checks failed");
            System.exit(1);
        }
        System.out.println("Path screen tests: " + passed + " checks passed");
    }

    /** Every GUI-scaled size from Minecraft's 320x240 floor up to a 4K window at GUI scale 1. */
    private static List<int[]> sizes() {
        List<int[]> sizes = new ArrayList<>();
        for (int width = 320; width <= 3840; width += width < 700 ? 7 : 61) {
            for (int height = 240; height <= 2160; height += height < 500 ? 5 : 53) {
                sizes.add(new int[] {width, height});
            }
        }
        return sizes;
    }

    private static void layouts() {
        for (int[] size : sizes()) {
            int width = size[0];
            int height = size[1];
            String at = " at " + width + "x" + height;
            PathChoiceLayout layout = PathChoiceLayout.of(width, height);
            Box screen = new Box(0, 0, width, height);
            Box header = new Box(0, layout.titleY, width, layout.subtitleY + layout.subtitleLines * PathChoiceLayout.LINE - layout.titleY);
            int count = KagunePath.choosable().size();
            check(layout.stacked == (width < PathChoiceLayout.stackBelowWidth(count)), "stacking follows the width threshold" + at);
            check(count != 2 || PathChoiceLayout.stackBelowWidth(count) == PathChoiceLayout.STACK_BELOW_WIDTH, "two paths stack below 420");
            List<Box> cards = layout.cards;
            Box first = cards.get(0);
            check(cards.size() == count, "one card per choosable path" + at);
            List<Box> boxes = new ArrayList<>(cards);
            boxes.add(layout.confirm);
            boxes.add(layout.later);
            for (Box box : boxes) {
                check(box.inside(screen), "everything is on screen" + at + ": " + box);
                check(box.x() >= Math.min(PathChoiceLayout.MARGIN, 8) || box == layout.later, "side gutter respected" + at);
                check(!box.overlaps(header), "nothing covers the title" + at + ": " + box);
            }
            // 6.1.0: with a third path the smallest screens stack three shorter cards (silhouette, name, type, a bullet or more).
            int minHeight = count > 2 ? 44 : 60;
            for (int i = 0; i < cards.size(); i++) {
                Box card = cards.get(i);
                for (int j = i + 1; j < cards.size(); j++) {
                    check(!card.overlaps(cards.get(j)), "cards never overlap" + at);
                }
                check(!card.overlaps(layout.confirm) && !card.overlaps(layout.later), "cards never cover the buttons" + at);
                check(card.width() >= 150, "cards stay readable (width " + card.width() + ")" + at);
                check(card.height() >= minHeight, "cards stay readable (height " + card.height() + ")" + at);
                check(card.width() == first.width() && card.height() == first.height(), "every path gets the same space (no favourite)" + at);
            }
            check(!layout.confirm.overlaps(layout.later), "confirm and later are separate" + at);
            int leftGap = cards.stream().mapToInt(Box::x).min().orElse(0);
            int rightGap = width - cards.stream().mapToInt(Box::right).max().orElse(width);
            check(Math.abs(leftGap - rightGap) <= 1, "cards are centred" + at);
            check(Math.abs(layout.confirm.centerX() - width / 2) <= 1, "confirm is centred" + at);
            check(layout.hintY < 0 || layout.hintY >= layout.later.bottom() && layout.hintY + PathChoiceLayout.LINE <= height,
                "the key hint sits under the buttons" + at);
            check(layout.compact == (height < PathChoiceLayout.COMPACT_BELOW_HEIGHT) && (layout.hintY < 0) == layout.compact,
                "short screens drop the hint" + at);

            for (Box card : cards) {
                PathChoiceLayout.Card inner = layout.card(card);
                check(inner.silhouette().inside(card), "silhouette is inside its card" + at);
                check(inner.silhouette().width() >= 20 && inner.silhouette().height() >= 20, "silhouette is visible" + at);
                check(inner.nameY() >= card.y() && inner.typeY() + PathChoiceLayout.LINE <= card.bottom(), "name and type fit" + at);
                check(inner.textX() >= card.x() && inner.textX() + inner.textWidth() <= card.right(), "text column is inside the card" + at);
                check(inner.textWidth() >= (layout.stacked ? 140 : 60), "text column is usable (" + inner.textWidth() + ")" + at);
                int bulletsEnd = inner.bulletsY() + inner.maxBullets() * PathChoiceLayout.LINE;
                check(inner.maxBullets() == 0 || bulletsEnd <= card.bottom(), "bullets fit" + at);
                if (inner.tagsY() >= 0) {
                    check(bulletsEnd <= inner.tagsY() && inner.tagsY() + PathChoiceLayout.LINE <= card.bottom(), "tags fit under bullets" + at);
                }
                if (inner.stripY() >= 0) {
                    check(inner.stripY() >= bulletsEnd && inner.stripY() + 14 <= card.bottom(), "progression strip fits" + at);
                    check(inner.tagsY() < 0 || inner.tagsY() + PathChoiceLayout.LINE <= inner.stripY(), "tags sit above the strip" + at);
                }
                if (layout.stacked) {
                    check(inner.silhouette().right() <= inner.textX(), "stacked: silhouette left of the text" + at);
                } else {
                    check(inner.silhouette().bottom() <= inner.bulletsY(), "side by side: bullets under the silhouette" + at);
                }
            }
        }
        PathChoiceLayout common = PathChoiceLayout.of(480, 270);
        // 6.1.0: three paths at 1080p, GUI scale 4: stacked bands, each with its silhouette, name, type and bullets.
        check(common.stacked && common.card(common.cards.get(0)).maxBullets() >= 2 && common.card(common.cards.get(0)).textWidth() >= 200,
            "1080p at GUI scale 4 shows every path with room for bullets");
        PathChoiceLayout wide = PathChoiceLayout.of(640, 360);
        check(!wide.stacked, "1080p at GUI scale 3 shows the three cards side by side");
        PathChoiceLayout roomy = PathChoiceLayout.of(640, 360);
        check(roomy.card(roomy.cards.get(1)).maxBullets() >= 5 && roomy.card(roomy.cards.get(1)).stripY() >= 0,
            "1080p at GUI scale 3 fits every Ayato bullet and the progression strip");
    }

    private static void hitTesting() {
        for (int[] size : sizes()) {
            PathChoiceLayout layout = PathChoiceLayout.of(size[0], size[1]);
            List<Box> all = new ArrayList<>(layout.cards);
            all.add(layout.confirm);
            all.add(layout.later);
            Box[] targets = all.toArray(new Box[0]);
            for (int i = 0; i < targets.length; i++) {
                Box box = targets[i];
                double cx = box.x() + box.width() / 2.0;
                double cy = box.y() + box.height() / 2.0;
                int claims = 0;
                for (Box other : targets) {
                    if (other.contains(cx, cy)) {
                        claims++;
                    }
                }
                check(claims == 1 && box.contains(cx, cy), "the centre of each target selects only that target");
                check(box.contains(box.x(), box.y()) && !box.contains(box.right(), box.y()) && !box.contains(box.x(), box.bottom()),
                    "hit boxes are half-open");
            }
        }
    }

    private static void silhouettes() {
        int bg = 0x130B0D;
        int[][] boxes = {{30, 30, 174, 72}, {10, 10, 60, 60}, {0, 0, 24, 24}, {5, 7, 214, 72}, {100, 50, 90, 90}, {3, 3, 280, 40}};
        for (int[] spec : boxes) {
            Box box = new Box(spec[0], spec[1], spec[2], spec[3]);
            for (double time = 0.0; time < 20.0; time += 0.173) {
                for (float intensity : new float[] {0.0F, 0.5F, 1.0F}) {
                    String at = " in " + box + " t=" + time;
                    List<PathSilhouette.Mark> kaneki = PathSilhouette.kaneki(box, time, intensity, bg);
                    List<PathSilhouette.Mark> gaseous = PathSilhouette.ayato(box, time, intensity, false, bg);
                    List<PathSilhouette.Mark> crystal = PathSilhouette.ayato(box, time, intensity, true, bg);
                    List<PathSilhouette.Mark> eto = PathSilhouette.eto(box, time, intensity, false, bg);
                    List<PathSilhouette.Mark> etoOut = PathSilhouette.eto(box, time, intensity, true, bg);
                    check(!kaneki.isEmpty() && !gaseous.isEmpty() && !crystal.isEmpty() && !eto.isEmpty() && !etoOut.isEmpty(),
                        "silhouettes draw something" + at);
                    for (List<PathSilhouette.Mark> marks : List.of(kaneki, gaseous, crystal, eto, etoOut)) {
                        for (PathSilhouette.Mark mark : marks) {
                            check(inside(mark, box), "mark stays inside its card: " + mark + at);
                            check(color(mark) >= 0 && color(mark) <= 0xFFFFFF, "colours are opaque RGB" + at);
                        }
                    }
                }
            }
        }
        Box card = new Box(0, 0, 200, 72);
        check(PathSilhouette.ayato(card, 1.0, 1.0F, true, bg).equals(PathSilhouette.ayato(card, 1.0, 1.0F, true, bg)),
            "silhouettes are deterministic (no per-frame randomness)");
        check(PathSilhouette.ayato(card, 1.0, 1.0F, true, bg).equals(PathSilhouette.ayato(card, 9.0, 1.0F, true, bg)),
            "crystallized wings hold still");
        check(!PathSilhouette.ayato(card, 1.0, 1.0F, false, bg).equals(PathSilhouette.ayato(card, 1.4, 1.0F, false, bg)),
            "gaseous wings flicker");
        check(!PathSilhouette.kaneki(card, 1.0, 1.0F, bg).equals(PathSilhouette.kaneki(card, 1.4, 1.0F, bg)), "tendrils writhe");
        float left = 0.0F;
        float right = 0.0F;
        for (PathSilhouette.Mark mark : PathSilhouette.ayato(card, 0.0, 1.0F, true, bg)) {
            if (mark instanceof PathSilhouette.Shape shape) {
                for (float x : shape.x()) {
                    left = Math.min(left, x - 100.0F);
                    right = Math.max(right, x - 100.0F);
                }
            }
        }
        check(right > 15.0F && Math.abs(left + right) < 1.0F, "crystallized wings are mirror images (" + left + ", " + right + ")");
        boolean shapesBehindFigure = false;
        List<PathSilhouette.Mark> front = PathSilhouette.ayato(card, 0.0, 1.0F, false, bg);
        for (int i = 0; i < front.size(); i++) {
            if (front.get(i) instanceof PathSilhouette.Stroke) {
                shapesBehindFigure = front.subList(i, front.size()).stream().noneMatch(m -> m instanceof PathSilhouette.Shape);
                break;
            }
        }
        check(shapesBehindFigure, "the wings are drawn behind the figure, like the anime's front view");
        check(PathSilhouette.blend(0x000000, 0xFFFFFF, 0.5F) == 0x808080 && PathSilhouette.blend(0x123456, 0xABCDEF, 0.0F) == 0x123456
            && PathSilhouette.blend(0x123456, 0xABCDEF, 1.0F) == 0xABCDEF && PathSilhouette.blend(0, 0xFFFFFF, 7.0F) == 0xFFFFFF,
            "colour blending");
    }

    private static boolean inside(PathSilhouette.Mark mark, Box box) {
        if (mark instanceof PathSilhouette.Shape shape) {
            for (int i = 0; i < shape.x().length; i++) {
                if (shape.x()[i] < box.x() || shape.x()[i] > box.right() || shape.y()[i] < box.y() || shape.y()[i] > box.bottom()) {
                    return false;
                }
            }
            return shape.x().length >= 3;
        }
        if (mark instanceof PathSilhouette.Stroke s) {
            float half = Math.max(s.t0(), s.t1()) / 2.0F;
            return Math.min(s.x0(), s.x1()) - half >= box.x() && Math.max(s.x0(), s.x1()) + half <= box.right()
                && Math.min(s.y0(), s.y1()) - half >= box.y() && Math.max(s.y0(), s.y1()) + half <= box.bottom();
        }
        PathSilhouette.Disc d = (PathSilhouette.Disc) mark;
        return d.cx() - d.r() >= box.x() && d.cx() + d.r() <= box.right() && d.cy() - d.r() >= box.y() && d.cy() + d.r() <= box.bottom();
    }

    private static int color(PathSilhouette.Mark mark) {
        if (mark instanceof PathSilhouette.Shape shape) {
            return shape.argb() & 0xFFFFFF; // translucent by design; the colour itself is checked here
        }
        return mark instanceof PathSilhouette.Stroke s ? s.rgb() : ((PathSilhouette.Disc) mark).rgb();
    }

    private static void stages() {
        int previous = -1;
        for (AyatoStage stage : AyatoStage.values()) {
            check(stage.requiredMastery() > previous, "Ayato stages unlock in order: " + stage);
            check(stage.shortName().length() <= 9, "stage short names fit the card strip: " + stage);
            previous = stage.requiredMastery();
            if (!stage.comingSoon()) {
                check(AyatoStage.forMastery(stage.requiredMastery()) == stage, "reaching a threshold unlocks its stage: " + stage);
            }
        }
        check(AyatoStage.values()[0] == AyatoStage.ONE_WING && AyatoStage.ONE_WING.requiredMastery() == 0 && AyatoStage.forMastery(-5) == AyatoStage.ONE_WING,
            "a new Ayato starts with one small wing");
        check(AyatoStage.TWIN_WINGS.requiredMastery() == 100 && AyatoStage.forMastery(99) == AyatoStage.ONE_WING
            && AyatoStage.forMastery(100) == AyatoStage.TWIN_WINGS, "two small wings come next, at mastery 100");
        check(AyatoStage.FULL_WINGS.requiredMastery() == 500 && AyatoStage.FULL_SIZE_MASTERY == 500, "the wings reach full size at mastery 500");
        check(AyatoStage.growth(100) == 0.0F && AyatoStage.growth(500) == 1.0F && AyatoStage.growth(300) > 0.4F && AyatoStage.growth(300) < 0.6F
            && AyatoStage.growth(9000) == 1.0F, "the wings grow steadily from 100 to 500, then stop");
        check(!AyatoStage.KAKUJA_I.comingSoon() && !AyatoStage.KAKUJA_II.comingSoon() && !AyatoStage.FULL_WINGS.comingSoon(),
            "both kakuja are built");
        check(AyatoStage.forMastery(749) == AyatoStage.FULL_WINGS && AyatoStage.forMastery(750) == AyatoStage.KAKUJA_I, "the first kakuja is reached at 750");
        check(AyatoStage.forMastery(999) == AyatoStage.KAKUJA_I && AyatoStage.forMastery(1000) == AyatoStage.KAKUJA_II, "the final kakuja is reached at 1000");
        check(!AyatoStage.forMastery(499).reEra() && AyatoStage.forMastery(500).reEra(), "the full :re shape comes with full size");
    }

    /** Room for future kagunes: the starting screen lays out any number of cards, and every path has a consistent card. */
    private static void framework() {
        for (int count = 1; count <= 4; count++) {
            for (int[] size : sizes()) {
                int width = size[0];
                int height = size[1];
                String at = " (" + count + " cards at " + width + "x" + height + ")";
                PathChoiceLayout layout = PathChoiceLayout.of(width, height, count);
                Box screen = new Box(0, 0, width, height);
                check(layout.cards.size() == count, "one box per card" + at);
                for (int i = 0; i < count; i++) {
                    Box card = layout.cards.get(i);
                    check(card.inside(screen), "every card is on screen" + at);
                    check(!card.overlaps(layout.confirm) && !card.overlaps(layout.later), "cards never cover the buttons" + at);
                    check(card.width() == layout.cards.get(0).width() && card.height() == layout.cards.get(0).height(), "no favourite card" + at);
                    check(layout.cardAt(card.x() + card.width() / 2.0, card.y() + card.height() / 2.0) == i, "clicking a card picks it" + at);
                    for (int j = i + 1; j < count; j++) {
                        check(!card.overlaps(layout.cards.get(j)), "cards never overlap" + at);
                    }
                    check(layout.stacked || card.width() >= PathChoiceLayout.MIN_SIDE_CARD_WIDTH || width < PathChoiceLayout.STACK_BELOW_WIDTH,
                        "side-by-side cards stay wide enough" + at);
                }
                int leftGap = layout.cards.get(0).x();
                int rightGap = width - (layout.stacked ? layout.cards.get(0).right() : layout.cards.get(count - 1).right());
                check(Math.abs(leftGap - rightGap) <= 1, "cards are centred" + at);
            }
        }
        check(PathChoiceLayout.of(800, 400, 3).cards.get(0).width() >= PathChoiceLayout.MIN_SIDE_CARD_WIDTH && !PathChoiceLayout.of(800, 400, 3).stacked,
            "three kagunes still sit side by side on a 1080p screen at GUI scale 2.7");
        for (KagunePath path : KagunePath.choosable()) {
            PathCards.Card card = PathCards.of(path);
            check(card.path() == path, "each path has its own card: " + path);
            check(!card.bullets().isEmpty() && !card.name().isBlank() && !card.awaken().isBlank(), "cards say something: " + path);
            check(card.stageNames().size() == card.stageMastery().size() && card.stageNames().size() >= 2, "stage strip is consistent: " + path);
            check(card.stageMastery().get(0) == 0, "every path starts at mastery 0: " + path);
            for (int i = 1; i < card.stageMastery().size(); i++) {
                check(card.stageMastery().get(i) > card.stageMastery().get(i - 1), "stages unlock in order: " + path);
            }
            check(card.silhouette().marks(new Box(0, 0, 200, 72), 1.0, 1.0F, true, 0x130B0D).size() > 0, "each path draws a silhouette: " + path);
            check(path.awakenPitch() > 0.5F && path.awakenPitch() < 2.0F, "awakening sounds sensible: " + path);
        }
        check(PathCards.choosable().size() == KagunePath.choosable().size() && !KagunePath.choosable().contains(KagunePath.NONE),
            "the starting screen offers every real path and never 'undecided'");
        KaguneForm[] forms = KanekiForms.forms();
        PathCards.Card kaneki = PathCards.of(KagunePath.KANEKI);
        check(kaneki.stageNames().size() == forms.length, "the Kaneki strip lists every form a player can have");
        for (int i = 0; i < Math.min(forms.length, kaneki.stageNames().size()); i++) {
            check(kaneki.stageNames().get(i).equals(forms[i].displayName()) && kaneki.stageMastery().get(i) == forms[i].requiredMastery(),
                "the Kaneki strip matches the original form " + forms[i]);
        }
    }
}
