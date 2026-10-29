package com.hex.kagune.control;

import com.hex.kagune.control.client.render.StrikeCurve;
import com.hex.kagune.gameplay.KaguneForm;

/** Every form x stage x limb x control: finite, connects at impact, ends at rest, no jumps, within reach. */
public final class StrikeShapeTests {
    public static void main(String[] args) {
        int checks = 0;
        int failures = 0;
        StrikeCurve curve = new StrikeCurve();
        for (KaguneForm form : KaguneForm.values()) {
            ComboStyle style = ComboStyle.forForm(form);
            for (int index = 0; index < style.stageCount(); index++) {
                ComboStage stage = style.stage(index);
                int mask = ComboStyle.limbMask(stage, form.limbCount(), 0);
                for (int limb = 0; limb < form.limbCount(); limb++) {
                    if ((mask & 1 << limb) == 0) {
                        continue;
                    }
                    for (double control : new double[] {0.0, 0.5, 1.0}) {
                        KaguneForm.LimbLayout layout = form.limb(limb);
                        curve.rootX = 0; curve.rootY = 1; curve.rootZ = 0;
                        curve.restX = layout.fanSide() * 0.4; curve.restY = 1.6; curve.restZ = -0.6;
                        curve.targetX = 0.4; curve.targetY = 1.2; curve.targetZ = 6.0;
                        curve.forwardX = 0; curve.forwardZ = 1;
                        curve.sideSign = Math.abs(layout.fanSide()) < 0.01 ? 1.0 : Math.signum(layout.fanSide());
                        curve.lane = StrikeCurve.laneOf(layout.fanHeight());
                        curve.size = StrikeCurve.sizeScale(6.0);
                        curve.stage(stage, stage.impactFraction());
                        curve.control = control;
                        curve.involuntary = control == 0.0;
                        curve.deviation = 0.0;
                        curve.strikeSeed = 1234L + limb;
                        curve.limbIndex = limb;
                        curve.rank = Integer.bitCount(mask & ((1 << limb) - 1));
                        curve.count = Integer.bitCount(mask);
                        String label = form + " " + stage.name() + " limb " + limb + " control " + control;
                        double px = Double.NaN, py = 0, pz = 0;
                        for (int step = 0; step <= 200; step++) {
                            curve.sample(step / 200.0);
                            checks++;
                            boolean finite = Double.isFinite(curve.x) && Double.isFinite(curve.y) && Double.isFinite(curve.z);
                            double reach = Math.sqrt(curve.x * curve.x + (curve.y - 1) * (curve.y - 1) + curve.z * curve.z);
                            double jump = Double.isNaN(px) ? 0 : Math.sqrt(Math.pow(curve.x - px, 2) + Math.pow(curve.y - py, 2) + Math.pow(curve.z - pz, 2));
                            if (!finite || reach > StrikeCurve.MAX_REACH + 1e-6 || jump > 1.5) {
                                failures++;
                                System.err.println("FAILED " + label + " at " + step + ": finite=" + finite + " reach=" + reach + " jump=" + jump);
                                break;
                            }
                            px = curve.x; py = curve.y; pz = curve.z;
                        }
                        curve.sample(stage.impactFraction());
                        double miss = Math.sqrt(Math.pow(curve.x - curve.aimX, 2) + Math.pow(curve.y - curve.aimY, 2) + Math.pow(curve.z - curve.aimZ, 2));
                        curve.sample(1.0);
                        double home = Math.sqrt(Math.pow(curve.x - curve.restX, 2) + Math.pow(curve.y - curve.restY, 2) + Math.pow(curve.z - curve.restZ, 2));
                        checks += 2;
                        if (miss > 0.6 || home > 1e-6) {
                            failures++;
                            System.err.println("FAILED " + label + ": impact miss " + miss + ", rest error " + home);
                        }
                    }
                }
            }
        }
        if (failures > 0) {
            System.err.println(failures + " strike path failures");
            System.exit(1);
        }
        System.out.println("Strike path tests: " + checks + " checks passed");
    }
}
