import com.hex.kagune.control.ayato.WingForm;
import com.hex.kagune.control.ayato.WingRig;

/**
 * Prints how many quads the twin wings build per player at each era and state (full detail and far detail), to keep
 * an eye on the budget AyatoTests enforces (under 3200 at rest, under 6400 mid-crystallization, far under half).
 * Usage after tools/preview.sh (or a build): {@code java -cp build/preview-classes:build/classes tools/QuadCount.java}.
 */
public class QuadCount {
    public static void main(String[] args) {
        double[] eye = {0.6, 2.4, -5.0};
        for (int mastery : new int[] {0, 300, 450, 800, 1000}) {
            for (float crystal : new float[] {0F, 0.5F, 1F}) {
                for (float volley : new float[] {0F, 1F}) {
                    int n = WingRig.build(new WingRig.Input(WingForm.TWIN, mastery, 1F, 1.3, crystal, crystal, WingRig.Motion.NONE, 0, 0.4F, volley,
                        volley, false, 1F, 1F, false), eye).size();
                    int far = WingRig.build(new WingRig.Input(WingForm.TWIN, mastery, 1F, 1.3, crystal, crystal, WingRig.Motion.NONE, 0, 0.4F,
                        volley, volley, false, 0.3F, 1F, false), eye).size();
                    System.out.println("mastery " + mastery + " crystal " + crystal + " volley " + volley + ": " + n + " quads (far " + far + ")");
                }
            }
        }
    }
}
