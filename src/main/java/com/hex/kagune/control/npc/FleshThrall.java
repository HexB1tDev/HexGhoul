package com.hex.kagune.control.npc;

import com.hex.kagune.control.king.PeaceRules;
import com.hex.kagune.control.server.FleshThralls;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.level.Level;

/**
 * 1.0.34: one of the creatures Peace's Rebirth Instinct brings up out of the ground: a body of its living flesh and kagune, a man's
 * size. It lives a short while ({@link PeaceRules#THRALL_LIFE}) at its maker's side and is never kept: whatever it does is decided on
 * the server ({@link FleshThralls}), and a body no one there knows of (a world loaded again) goes at once. Every client draws it
 * ({@code FleshThrallRenderer}, {@code PeaceFx}) from what the server tells it.
 */
public class FleshThrall extends PathfinderMob {
    public FleshThrall(EntityType<? extends FleshThrall> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.m_21552_()
            .m_22268_(Attributes.f_22276_, PeaceRules.THRALL_HEALTH)
            .m_22268_(Attributes.f_22279_, 0.32)
            .m_22268_(Attributes.f_22277_, 32.0)
            .m_22268_(Attributes.f_22284_, 6.0)
            .m_22268_(Attributes.f_22278_, 0.5);
    }

    @Override
    protected void m_8099_() {
        this.f_21345_.m_25352_(0, new FloatGoal(this));
    }

    @Override
    protected void m_8024_() {
        FleshThralls.tick(this);
    }
}
