import com.hex.kagune.control.KaguneColours;
import com.hex.kagune.control.KaguneTint;
import com.hex.kagune.control.client.animation.BladeArmPose;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 4.1.47: the arm blade in the wild ghouls' colours (GhoulNpcRenderer: KaguneTint.wild of each one's seed, a random kagune colour each): the
 * original's crimson, the rare near-black and bone, and wild ghouls' own rolls, each in its stance.
 *
 * <pre>
 * javac -nowarn -encoding UTF-8 -cp build/quick/classes:build/quick/base.jar -sourcepath tools -d build/blade-showcase tools/BladeArmColours.java
 * java -Djava.awt.headless=true -cp build/blade-showcase:build/quick/classes:build/quick/base.jar BladeArmColours &lt;out.png&gt;
 * </pre>
 */
public final class BladeArmColours {
    public static void main(String[] args) throws Exception {
        PoseRender.stiff = true;
        List<ArtPreview.Shot> shots = new ArrayList<>();
        List<Object[]> colours = new ArrayList<>();
        colours.add(new Object[] {"the original's crimson", KaguneColours.DEFAULT});
        colours.add(new Object[] {"rare: near black", KaguneColours.BLACK});
        colours.add(new Object[] {"rare: bone", KaguneColours.BONE});
        int found = 0;
        for (int seed = 1; found < 5; seed++) {
            int c = KaguneTint.wild(seed * 977);
            if (c != KaguneColours.DEFAULT && c != KaguneColours.BLACK && c != KaguneColours.BONE) {
                colours.add(new Object[] {String.format("a wild ghoul's roll #%06X", c), c});
                found++;
            }
        }
        double[] eye = {-3.0, 1.9, 3.6};
        double[] at = {0.0, 1.0, 0.7};
        for (Object[] c : colours) {
            shots.add(BladeArmShowcase.shot((String)c[0], BladeArmPose.stand(40, 0), 1.0, eye, at, (Integer)c[1], 9));
        }
        ArtPreview.sheet(new File(args[0]), "The arm blade in the wild ghouls' random kagune colours", shots, 4, 340, 320);
    }
}
