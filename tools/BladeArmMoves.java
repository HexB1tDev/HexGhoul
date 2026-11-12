import com.hex.kagune.control.client.animation.BladeArmPose;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.npc.BladeArm;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 4.1.47: the arm blade's moveset off-game (BladeArmPose: the four slashes, frame by frame through each, and its living stance), on a ghoul
 * NPC with the great blade (BladeArmShowcase's drawing of BladeArmModel), a floor at its feet; prints the blade's lowest point through
 * every pose (blocks above the feet: below 0 it would cut into the ground).
 *
 * <pre>
 * javac -nowarn -encoding UTF-8 -cp build/quick/classes:build/quick/base.jar -sourcepath tools -d build/blade-showcase tools/BladeArmMoves.java
 * java -Djava.awt.headless=true -cp build/blade-showcase:build/quick/classes:build/quick/base.jar BladeArmMoves &lt;out.png&gt; [camera: front|side|top]
 * </pre>
 */
public final class BladeArmMoves {
    public static void main(String[] args) throws Exception {
        PoseRender.stiff = true;
        String view = args.length > 1 ? args[1] : "front";
        double[] eye = switch (view) {
            case "side" -> new double[] {-5.6, 1.7, 1.2};
            case "top" -> new double[] {0.3, 7.5, 1.4};
            default -> new double[] {-2.8, 2.3, 5.4};
        };
        double[] at = {0.0, 1.1, 0.9};
        List<ArtPreview.Shot> shots = new ArrayList<>();
        String[] names = {"Chop down", "Rising backhand", "Sweep", "Cleave (finisher)"};
        for (int step = 0; step < BladeArm.STEPS; step++) {
            double land = BladeArm.lands(step) / (double)BladeArm.ticks(step);
            double[] ts = {0.15, 0.3, land - 0.1, land, land + 0.12, 0.85};
            for (double t : ts) {
                RigPose pose = BladeArmPose.swing(step, t);
                String label = names[step] + " t" + String.format("%.2f", t) + (Math.abs(t - land) < 1.0E-6 ? " (lands)" : "");
                shots.add(BladeArmShowcase.shot(label, pose, 1.0, eye, at, 0x2A4FC8, 9));
            }
        }
        for (double time : new double[] {0, 16, 32, 48, 160 + 0 - 0.37 * 0 * 40.0 + 11, 64}) {
            shots.add(BladeArmShowcase.shot("Stance, tick " + (int)time, BladeArmPose.stand(time, 0), 1.0, eye, at, 0x2A4FC8, 9));
        }
        ArtPreview.sheet(new File(args[0]), "The arm blade's moveset (4.1.47): four slashes and its stance (" + view + ")", shots, 6, 300, 300);
        for (int step = 0; step < BladeArm.STEPS; step++) {
            StringBuilder b = new StringBuilder(names[step] + ":");
            for (double t = 0.0; t <= 1.0001; t += 0.05) {
                b.append(String.format(" %.2f", BladeLow.low(BladeArmPose.swing(step, t), 1.0)));
            }
            System.out.println(b);
        }
        double worst = 99;
        for (double time = 0; time < 400; time += 2) {
            worst = Math.min(worst, BladeLow.low(BladeArmPose.stand(time, 7), 1.0));
        }
        System.out.printf("stance lowest %.2f%n", worst);
    }
}
