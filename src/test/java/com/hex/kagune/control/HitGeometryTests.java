package com.hex.kagune.control;

import com.hex.kagune.gameplay.KaguneForm;
import java.util.ArrayList;
import java.util.List;

/** Server combo hit volumes for every form and stage, checked with plain geometry (strike direction = +Z). */
public final class HitGeometryTests {
    private static int passed;
    private static final List<String> FAILURES = new ArrayList<>();
    private static final double RANGE = 9.0;
    private static final double HALF_WIDTH = 0.3;
    private static final double HALF_HEIGHT = 0.9;

    private static void check(boolean condition, String description) {
        if (condition) {
            passed++;
        } else {
            FAILURES.add(description);
        }
    }

    /** Candidate at horizontal angle (degrees from the strike direction) and distance, at eye height + dy. */
    private static boolean hits(ComboStage stage, double angleDegrees, double distance, double dy, double slamDistance) {
        double radians = Math.toRadians(angleDegrees);
        double ox = Math.sin(radians) * distance;
        double oz = Math.cos(radians) * distance;
        return HitGeometry.inside(stage.shape(), stage.shapeParam(), ox, dy, oz, 0.0, 0.0, 1.0, RANGE, HALF_WIDTH, HALF_HEIGHT,
            0.0, 0.0, slamDistance);
    }

    public static void main(String[] args) {
        for (KaguneForm form : KaguneForm.values()) {
            ComboStyle style = ComboStyle.forForm(form);
            for (int index = 0; index < style.stageCount(); index++) {
                ComboStage stage = style.stage(index);
                String label = form + " " + stage.name() + " (" + stage.shape() + ")";
                double impact = RANGE * 0.6;
                check(hits(stage, 0.0, impact, 0.0, impact), label + " hits the aimed target ahead");
                check(!hits(stage, 180.0, 4.0, 0.0, impact) || stage.shape() == HitShape.SPIN, label + " does not hit behind (except Whirlwind)");
                check(!hits(stage, 0.0, RANGE + 3.0, 0.0, impact), label + " respects reach");
                check(!hits(stage, 0.0, 4.0, 9.0, impact), label + " does not reach far above");
                switch (stage.shape()) {
                    case SWEEP -> {
                        double half = stage.shapeParam() / 2.0;
                        check(hits(stage, half - 5.0, 6.0, 0.0, impact), label + " covers its arc edge");
                        check(!hits(stage, half + 25.0, 6.0, 0.0, impact), label + " stops past its arc");
                        check(hits(stage, 170.0, 0.8, 0.0, impact), label + " clips point-blank attackers");
                    }
                    case PIERCE -> {
                        check(hits(stage, Math.toDegrees(Math.atan2(stage.shapeParam() * 0.8, 6.0)), 6.0, 0.0, impact), label + " pierces near the line");
                        check(!hits(stage, 35.0, 6.0, 0.0, impact), label + " misses well off the line");
                    }
                    case SLAM -> {
                        check(hits(stage, 10.0, impact, 0.0, impact), label + " crushes around the impact");
                        check(!hits(stage, 0.0, 1.5, 0.0, impact), label + " does not crush the user's own feet");
                        check(!hits(stage, 90.0, 6.0, 0.0, impact), label + " stays local");
                    }
                    case SPIN -> {
                        check(hits(stage, 180.0, impact * 0.9, 0.0, impact), label + " whirls all the way round at the aimed distance");
                        check(!hits(stage, 90.0, impact + 2.0, 0.0, impact), label + " has a bounded radius");
                        check(hits(stage, 120.0, RANGE * stage.shapeParam() * 0.9, 0.0, 1.0), label + " keeps a minimum radius when aimed close");
                        check(!hits(stage, 0.0, RANGE + 1.0, 0.0, RANGE + 5.0), label + " never spins beyond reach");
                        check(RANGE * stage.shapeParam() <= 7.5, label + " base radius stays modest at 15-block max reach");
                    }
                    case SINGLE -> {
                    }
                }
            }
        }
        check(!HitGeometry.inside(HitShape.SWEEP, 90.0F, 1.0, 0.0, 3.0, 0.0, 0.0, 0.0, RANGE, 0.3, 0.9, 0.0, 0.0, 0.0),
            "a degenerate vertical strike direction never sweeps");
        check(!HitGeometry.inside(HitShape.SINGLE, 0.0F, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, RANGE, 0.3, 0.9, 0.0, 0.0, 0.0),
            "SINGLE never adds extra targets");
        if (!FAILURES.isEmpty()) {
            FAILURES.forEach(failure -> System.err.println("FAILED: " + failure));
            System.err.println(FAILURES.size() + " of " + (passed + FAILURES.size()) + " checks failed");
            System.exit(1);
        }
        System.out.println("Hit geometry tests: " + passed + " checks passed");
    }
}
