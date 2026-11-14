import com.hex.kagune.control.client.PaleClient;
import com.hex.kagune.control.client.animation.PaleMotions;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.pale.PaleMove;
import com.hex.kagune.control.pale.PaleRules;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 4.1.92: Kick Master's charge and dash kick as the game's own animator puts them on her body (the path 4.1.92 opened).
 * Usage (after tools/fast_tests.sh): javac -nowarn -cp build/fast/classes:build/fast/base.jar -d DIR tools/PoseRender.java
 * tools/WingPreview.java tools/ArtPreview.java tools/PalePreview.java tools/DashDrawnPreview.java; java -Djava.awt.headless=true -cp
 * DIR:build/fast/classes:build/fast/base.jar DashDrawnPreview OUT.png
 */
public final class DashDrawnPreview {
    public static void main(String[] args) throws Exception {
        Class<?> type = Class.forName("com.hex.kagune.control.client.animation.PaleAnimator");
        java.lang.reflect.Constructor<?> make = type.getDeclaredConstructor();
        make.setAccessible(true);
        java.lang.reflect.Method apply = type.getDeclaredMethod("apply", RigPose.class, PaleClient.PaleView.class, double.class, float.class, float.class, float.class);
        apply.setAccessible(true);
        int ticks = 4;
        int detail = PaleRules.dashDetail(ticks, 1.0F);
        int arrive = PaleRules.dashArrive(detail);
        double contact = arrive + PaleRules.DASH_CONTACT;
        double lift = PaleRules.dashLift(2.0 * PaleRules.DASH_HEAD);
        double run = ticks * PaleRules.DASH_SPEED;
        double ahead = 0.25 + PaleRules.DASH_STAND + 0.3;
        double[] flight = PaleRules.dashFlight();
        double[][] eyes = {{4.6, 1.3, 0.9}, {-3.4, 1.9, 3.6}};
        String[] views = {"side on", "front-left"};
        List<ArtPreview.Shot> shots = new ArrayList<>();
        for (int pass = 0; pass < eyes.length; pass++) {
            double[] eye = eyes[pass];
            // The charge held (right click down), drawn through the animator out of her stance.
            Object charging = make.newInstance();
            PaleClient.PaleView charge = new PaleClient.PaleView(PaleMove.TWIRL_CHARGE, 0, 50L, 0, 0.0F, 0.0F, 0, 0, 0, 50.0);
            for (double held : new double[] {0.0, 3.0, 12.0, 30.0}) {
                RigPose p = PaleMotions.kickStance();
                apply.invoke(charging, p, charge, 50.0 + held, (float)(50.0 + held), 0.0F, 0.0F);
                p.settle(1.0F);
                List<WingPreview.Tri> scene = PalePreview.figure(new double[] {0.0, 1.0, ahead + run});
                PalePreview.box(scene, new double[] {-6.0, -0.2, -4.0}, new double[] {4.0, 0.0, 14.0}, 0xFF4E5A44);
                shots.add(new ArtPreview.Shot(String.format(java.util.Locale.ROOT, "charge held %.0f t (%s)", held, views[pass]), p, ArtPreview.NO_WINGS, eye,
                    new double[] {0.0, 0.9, 0.5}, scene));
            }
            // Let go: the dash kick, every tick from the off to her guard.
            Object animator = make.newInstance();
            PaleClient.PaleView dash = new PaleClient.PaleView(PaleMove.DASH_KICK, detail, 100L, 0, 1.0F, 0.0F, 0, 0, 0, 100.0);
            int end = PaleRules.dashEnd(detail);
            for (double age = 0.0; age <= end; age += 1.0) {
                RigPose p = PaleMotions.kickStance();
                apply.invoke(animator, p, dash, 100.0 + age, (float)(100.0 + age), 0.0F, 0.0F);
                p.settle(1.0F);
                double[] at = PaleRules.dashPlace(new double[] {0.0, 0.0, run}, detail, lift, age);
                double gone = at == null ? run : at[2];
                double up = at == null ? 0.0 : at[1];
                double flown = age > contact ? Math.min(6.0, (age - contact) * 2.4) : 0.0;
                List<WingPreview.Tri> scene = flown > 5.5 ? new ArrayList<>()
                    : PalePreview.figure(new double[] {flight[0] * flown, 1.0 - up + flown * 0.15, ahead + run - gone + flight[1] * flown});
                PalePreview.box(scene, new double[] {-6.0, -0.2 - up, -4.0}, new double[] {4.0, -up, 14.0}, 0xFF4E5A44);
                String what = age < PaleRules.DASH_BURST ? "off the mark" : age < arrive ? "THE RUN" : age < contact ? "cocked" : age < contact + 1.0 ? "THE KICK" : "after";
                shots.add(new ArtPreview.Shot(String.format(java.util.Locale.ROOT, "tick %.0f %s (%s)", age, what, views[pass]), p, ArtPreview.NO_WINGS,
                    new double[] {eye[0], eye[1] - up * 0.3, eye[2]}, new double[] {0.0, 0.9 - up * 0.4, 0.5}, scene));
            }
        }
        ArtPreview.sheet(new File(args[0]), "4.1.92: Kick Master's charge and dash kick as the game's animator now draws them (before: her stance, unmoved)", shots, 9, 250, 270);
    }
}
