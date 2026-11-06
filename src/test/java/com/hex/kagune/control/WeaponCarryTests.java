package com.hex.kagune.control;

import com.hex.kagune.control.client.animation.AgentPose;
import com.hex.kagune.control.client.animation.QuinqueGrip;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.client.animation.WeaponPose;
import com.hex.kagune.control.quinque.Arm;
import com.hex.kagune.control.quinque.ArmMesh;
import com.hex.kagune.control.quinque.WhipPath;

/** 3.1.0: carrying a kagune-made quinque must not replace an investigator's walk with planted combat feet. Pure, off-game checks. */
public final class WeaponCarryTests {
    private static final Arm[] REPORTED = {Arm.WHIP, Arm.FLESH_HAMMER, Arm.BONE_SWORD};
    private static int passed;

    private static void check(boolean good, String message) {
        if (!good) {
            throw new AssertionError(message);
        }
        passed++;
    }

    public static void main(String[] args) {
        walking();
        unaffected();
        combat();
        whip();
        System.out.println("Weapon carry tests: " + passed + " checks passed");
    }

    private static void walking() {
        for (Arm arm : REPORTED) {
            RigPose stance = WeaponPose.stance(arm);
            RigPose idle = WeaponPose.carry(arm, 0.0, 1.7);
            check(idle.legHold == 1.0F && idle.rightArmX == stance.rightArmX && idle.twist == stance.twist,
                arm + ": stopped keeps its stance");
            float half = WeaponPose.carry(arm, 0.02, 0.0).legHold;
            check(half > 0.0F && half < 1.0F && WeaponPose.carry(arm, 0.039, 0.0).legHold < half,
                arm + ": smoothly releases its stance as it starts moving");
            for (double speed : new double[] {0.04, 0.1, 0.2, 0.4}) {
                for (double phase : new double[] {0.0, 0.7, 1.6, 3.2, 4.8}) {
                    float run = AgentPose.running(speed);
                    RigPose gait = AgentPose.run(phase, run, arm.twoHanded ? AgentPose.Carry.STANCE : AgentPose.Carry.BLADE);
                    RigPose p = new RigPose().add(gait, 1.0F);
                    RigPose held = WeaponPose.carry(arm, speed, phase);
                    check(held.legHold == 0.0F, arm + ": carry never plants moving feet");
                    if (arm.twoHanded && run > 0.0F) {
                        AgentPose.stanceRun(p, WeaponPose.base(arm), phase, run);
                    } else {
                        AgentPose.withWeapon(p, held);
                    }
                    // AgentModel passes in vanilla's actual walk, with opposite rotations on the two legs.
                    float stride = (float)(Math.cos(phase) * 0.77);
                    for (String leg : new String[] {"rightLeg", "leftLeg"}) {
                        float vanilla = leg.equals("rightLeg") ? stride : -stride;
                        float[] expected = gait.transform(leg, RigPose.ROTATION, vanilla, 0, 0);
                        float[] actual = p.transform(leg, RigPose.ROTATION, vanilla, 0, 0);
                        check(Math.abs(actual[0] - expected[0]) < 1.0E-6F && Math.abs(actual[2] - expected[2]) < 1.0E-6F,
                            arm + ": preserves the actual " + leg + " gait at speed " + speed);
                    }
                    if (arm.twoHanded) {
                        for (boolean slim : new boolean[] {false, true}) {
                            RigPose gripped = new RigPose().add(p, 1.0F);
                            float miss = QuinqueGrip.reach(gripped, WeaponPose.base(arm), slim);
                            check(Float.isFinite(miss) && miss < 2.5F, arm + ": both hands stay on the moving weapon");
                            for (boolean master : new boolean[] {false, true}) {
                                check(clearance(gripped, arm, slim, master) > 0.1, arm + ": full-size carried mesh clears the floor");
                            }
                        }
                    } else {
                        check(p.leftArmX == gait.leftArmX && p.leftArmHold == run,
                            "Whip: free arm keeps the walk/run instead of its static combat pose");
                        if (run == 0.0F) {
                            float[] forward = p.transform("leftArm", RigPose.ROTATION, 0.5F, 0, 0);
                            float[] back = p.transform("leftArm", RigPose.ROTATION, -0.5F, 0, 0);
                            check(Math.abs(forward[0] - back[0]) > 0.5F, "Whip: vanilla free-arm movement survives posing");
                        }
                    }
                }
            }
        }
    }

    /** 3.1.1: the owner clarified that the other quinques are fine. The spear's carry must match its original stance exactly. */
    private static void unaffected() {
        RigPose original = WeaponPose.stance(Arm.CRYSTAL_SPEAR);
        for (double speed : new double[] {0, 0.02, 0.1, 0.2, 0.4}) {
            for (double phase : new double[] {0, 0.7, 1.6, 3.2}) {
                RigPose carried = WeaponPose.carry(Arm.CRYSTAL_SPEAR, speed, phase);
                for (String part : new String[] {"body", "head", "torso", "rightArm", "leftArm", "rightLeg", "leftLeg"}) {
                    for (int type : new int[] {RigPose.POSITION, RigPose.ROTATION, RigPose.BEND}) {
                        check(java.util.Arrays.equals(original.transform(part, type, 0.3F, 0.2F, 0.1F),
                            carried.transform(part, type, 0.3F, 0.2F, 0.1F)), "Crystal spear: original carry channel " + part + "/" + type);
                    }
                }
            }
        }
    }

    /** The real mesh in the arm's final ModelPart transform, as WeaponLayer draws it (carry has no flip/roll). */
    private static double clearance(RigPose p, Arm arm, boolean slim, boolean master) {
        float[] pivot = p.transform("rightArm", RigPose.POSITION, -5, 2, 0);
        float[] rot = p.transform("rightArm", RigPose.ROTATION, 0, 0, 0.05F);
        ArmMesh.Mesh mesh = ArmMesh.of(arm, master);
        double low = Double.POSITIVE_INFINITY;
        for (int i = 0; i < mesh.pos.length; i += 3) {
            double[] onArm = WeaponPose.onArm(WeaponPose.meshToArm(arm), new double[] {mesh.pos[i], mesh.pos[i + 1], mesh.pos[i + 2]});
            double[] at = WeaponPose.onBody(pivot[0], pivot[1], pivot[2], rot[0], rot[1], rot[2], slim, onArm);
            low = Math.min(low, 1.501 - at[1] - p.drop / 16.0);
        }
        return low;
    }

    private static void combat() {
        for (Arm arm : Arm.values()) {
            for (int step = 0; step < arm.length(); step++) {
                Arm.Strike strike = arm.strike(step);
                RigPose hit = WeaponPose.strike(arm, step, strike.land());
                RigPose p = AgentPose.run(0.8, 1.0F, AgentPose.Carry.BLADE);
                AgentPose.withWeapon(p, hit);
                check(p.legHold == 1.0F && Math.abs(p.rightLegX - hit.rightLegX) < 1.0E-6F && Math.abs(p.leftLegX - hit.leftLegX) < 1.0E-6F,
                    arm + ": a landing still uses its intended combat footwork");
            }
        }
    }

    private static void whip() {
        for (boolean master : new boolean[] {false, true}) {
            for (double drop : new double[] {0.2, 0.45, 0.94, 1.4}) {
                for (double time : new double[] {0, 17, 80, 211}) {
                    double[][] path = WhipPath.rest(WhipPath.length(master), drop, time);
                    check(path[0][0] == 0.0 && path[0][1] == 0.0 && path[0][2] == 0.0, "Whip: root stays at the handle's knot");
                    double low = Double.POSITIVE_INFINITY;
                    double high = Double.NEGATIVE_INFINITY;
                    double arc = 0.0;
                    for (int i = 0; i < path.length; i++) {
                        double[] p = path[i];
                        check(Double.isFinite(p[0]) && Double.isFinite(p[1]) && Double.isFinite(p[2]), "Whip: finite resting curve");
                        double height = drop + p[2];
                        check(height > 0.15, "Whip: the whole rest curve clears the floor");
                        low = Math.min(low, p[2]);
                        high = Math.max(high, p[2]);
                        if (i > 0) {
                            double[] before = path[i - 1];
                            arc += Math.sqrt(Math.pow(p[0] - before[0], 2) + Math.pow(p[1] - before[1], 2) + Math.pow(p[2] - before[2], 2));
                        }
                    }
                    double[] tip = path[path.length - 1];
                    check(low < -0.03 && high > 0.03 && tip[2] > 0.0, "Whip: dips then curls upward, never a flat floor trail");
                    check(arc < WhipPath.length(master) && arc > 2.5, "Whip: carried curl fits within its full attacking length");
                }
            }
        }
    }
}
