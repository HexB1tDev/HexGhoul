package com.hex.kagune.control.server;

import com.hex.kagune.control.village.BrickVillage;
import com.hex.kagune.control.village.VillageMap;
import com.hex.kagune.control.village.VillageRules;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;

/**
 * 1.3.71: where one of the Brick Village's people belongs ({@link VillageRules}): placed with the village (its template's entities carry
 * {@code HexVillage}: their offset to its middle in the template's frame, and their home's number), the first time it ticks it works out
 * the village's turn from its own yaw and keeps, in its saved data, the village's middle in the world, its turn and (a human) its home:
 * where it was placed, inside its house. Null for anyone else.
 */
final class VillagePlace {
    static final String KEY = "HexVillage";

    /** The village's middle (block x, y, z), its turn (quarter turns clockwise), this one's home (block x, y, z; a human's). */
    record Place(int x, int y, int z, int turns, int homeX, int homeY, int homeZ, boolean brick) {
        /** 2.0.1: its village's streets, patrol and perimeter (the new village's; a Brick Village's, built before 2.0.1, its own). */
        int[][] streets() {
            return this.brick ? BrickVillage.STREETS : VillageMap.STREETS;
        }

        int[][] route() {
            return this.brick ? BrickVillage.ROUTE : VillageMap.ROUTE;
        }

        int perimeter() {
            return this.brick ? BrickVillage.PERIMETER : VillageMap.PERIMETER;
        }

        /** The map's spot {@code at} ({dx, dy, dz} from the middle, the template's frame) in the world: {x + 0.5, y, z + 0.5}. */
        double[] world(int[] at) {
            int[] turned = VillageRules.turn(at[0], at[2], this.turns);
            return new double[] {this.x + turned[0] + 0.5, this.y + at[1], this.z + turned[1] + 0.5};
        }

        /** True if ({@code wx}, {@code wz}) is within the perimeter. */
        boolean inside(double wx, double wz) {
            return VillageRules.inside(wx - this.x - 0.5, wz - this.z - 0.5, perimeter());
        }

        /** ({@code wx}, {@code wz}) brought back within the perimeter, {@code margin} in from it. */
        double[] clamp(double wx, double wz, double margin) {
            double[] c = VillageRules.clamp(wx - this.x - 0.5, wz - this.z - 0.5, perimeter(), margin);
            return new double[] {this.x + 0.5 + c[0], this.z + 0.5 + c[1]};
        }

        /** Where ({@code wx}, {@code wz}) is in the template's frame, from the middle. */
        double[] local(double wx, double wz) {
            int[] back = VillageRules.unturn((int)Math.floor(wx) - this.x, (int)Math.floor(wz) - this.z, this.turns);
            return new double[] {back[0], back[1]};
        }
    }

    private VillagePlace() {
    }

    /** 2.0: {@code mob}'s slot among its village's people (its template number), or -1. */
    static int slot(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        return data.m_128441_(KEY) ? data.m_128469_(KEY).m_128451_("Home") : -1;
    }

    /** {@code mob}'s place in its village, worked out (and kept) the first time; null if it is none of the village's. */
    static Place of(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        if (!data.m_128441_(KEY)) {
            return null;
        }
        CompoundTag v = data.m_128469_(KEY);
        if (!v.m_128441_("Turns")) {
            int turns = VillageRules.turns(mob.m_146908_());
            int[] offset = VillageRules.turn(v.m_128451_("CentreX"), v.m_128451_("CentreZ"), turns);
            int bx = (int)Math.floor(mob.m_20185_());
            int by = (int)Math.floor(mob.m_20186_() + 0.01);
            int bz = (int)Math.floor(mob.m_20189_());
            v.m_128405_("Turns", turns);
            v.m_128405_("MidX", bx + offset[0]);
            v.m_128405_("MidY", by + v.m_128451_("CentreY"));
            v.m_128405_("MidZ", bz + offset[1]);
            v.m_128405_("HomeX", bx);
            v.m_128405_("HomeY", by);
            v.m_128405_("HomeZ", bz);
            data.m_128365_(KEY, v);
        }
        return new Place(v.m_128451_("MidX"), v.m_128451_("MidY"), v.m_128451_("MidZ"), v.m_128451_("Turns"), v.m_128451_("HomeX"), v.m_128451_("HomeY"),
            v.m_128451_("HomeZ"), !v.m_128441_("Map"));
    }
}
