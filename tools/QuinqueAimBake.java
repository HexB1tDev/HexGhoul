import com.hex.kagune.control.client.animation.QuinqueKeysBaker;

/**
 * 4.1.17: writes src/main/java/com/hex/kagune/control/client/animation/QuinqueKeys.java: every quinque swing written by intent
 * (QuinquePose's aims, QuinqueAim) solved into the arm's keys once, here, so the game never solves them as it swings (a third of a second
 * each). QuinqueTests checks the written keys still match their aims. Run after changing an aim:
 * <pre>
 * python3 tools/quick_check.py --noverify
 * javac -cp build/quick/classes:build/quick/base.jar -d build/aim-bake tools/QuinqueAimBake.java
 * java -cp build/aim-bake:build/quick/classes:build/quick/base.jar QuinqueAimBake
 * </pre>
 */
public final class QuinqueAimBake {
    public static void main(String[] args) throws Exception {
        java.nio.file.Files.writeString(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/client/animation/QuinqueKeys.java"),
            QuinqueKeysBaker.source());
        System.out.println("wrote QuinqueKeys.java");
    }
}
