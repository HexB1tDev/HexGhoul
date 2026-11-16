package com.hex.kagune.control.client.animation;

import com.hex.kagune.control.quinque.Quinque;

/**
 * 4.1.17 (pure): the source of {@code QuinqueKeys}: every swing QuinquePose writes by intent ({@link QuinquePose#aims}), solved
 * ({@link QuinqueAim#keys}) and written out as numbers (tools/QuinqueAimBake.java writes the file; QuinqueTests checks it matches).
 */
public final class QuinqueKeysBaker {
    private QuinqueKeysBaker() {
    }

    /** {@code q}'s {@code step}th swing's aims (null: still written as arm angles). */
    public static QuinqueAim.Aim[] aims(Quinque q, int step) {
        return QuinquePose.aims(q, step);
    }

    /** The baked keys' source. */
    public static String source() {
        StringBuilder out = new StringBuilder();
        out.append("package com.hex.kagune.control.client.animation;\n\n");
        out.append("/**\n * 4.1.17: every quinque swing written by intent (QuinquePose.aims), its arm solved by QuinqueAim (written by\n");
        out.append(" * tools/QuinqueAimBake.java: do not edit by hand; QuinqueTests checks these still match their aims).\n */\n");
        out.append("final class QuinqueKeys {\n    private QuinqueKeys() {\n    }\n\n");
        out.append("    private static QuinquePose.Key k(double t, double pitch, double yaw, double roll, double twist, double lean, double lunge, double drop,\n");
        out.append("                                     double spin, double wristX, double wristZ) {\n");
        out.append("        return new QuinquePose.Key((float)t, (float)pitch, (float)yaw, (float)roll, (float)twist, (float)lean, (float)lunge, (float)drop,\n");
        out.append("            (float)spin, (float)wristX, (float)wristZ);\n    }\n\n");
        out.append("    /** By quinque (its ordinal) and blow; null where a quinque's swings are still written as arm angles. */\n");
        out.append("    static final QuinquePose.Key[][][] KEYS = {\n");
        for (Quinque q : Quinque.values()) {
            if (QuinquePose.aims(q, 0) == null) {
                out.append("        null, // ").append(q).append("\n");
                continue;
            }
            out.append("        { // ").append(q).append("\n");
            for (int step = 0; step < q.steps(); step++) {
                out.append("            {");
                QuinquePose.Key[] keys = QuinqueAim.keys(q, QuinquePose.aims(q, step));
                for (int i = 0; i < keys.length; i++) {
                    QuinquePose.Key k = keys[i];
                    out.append(i == 0 ? "" : ",\n                ").append(String.format(java.util.Locale.ROOT,
                        "k(%.3f, %.4f, %.4f, %.4f, %.3f, %.3f, %.3f, %.3f, %.5f, %.4f, %.4f)", k.t(), k.pitch(), k.yaw(), k.roll(), k.twist(),
                        k.lean(), k.lunge(), k.drop(), k.spin(), k.wristX(), k.wristZ()));
                }
                out.append("},\n");
            }
            out.append("        },\n");
        }
        out.append("    };\n}\n");
        return out.toString();
    }
}
