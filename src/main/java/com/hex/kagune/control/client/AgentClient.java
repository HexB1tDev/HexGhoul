package com.hex.kagune.control.client;

import com.hex.kagune.control.ControlItems;
import com.hex.kagune.control.client.animation.AgentPose;
import com.hex.kagune.control.client.animation.PaleMotions;
import com.hex.kagune.control.client.animation.QuinquePose;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.npc.CcgAgent;
import com.hex.kagune.control.npc.NpcRules;
import com.hex.kagune.control.pale.PaleRules;
import com.hex.kagune.control.quinque.Quinque;
import net.minecraft.world.item.ItemStack;

/**
 * 1.3.27: a CCG investigator's pose as this client draws it, from what its entity data says ({@link CcgAgent}): a flip over everything
 * (a player's, {@code PaleMotions.flip}); otherwise its run (AgentPose: the same for every one of them) under its quinque (a swing of its
 * string, a parry, the case opening or shutting, its stance, the launcher's kick) and a dash on top. Pure reading: nothing is kept.
 */
public final class AgentClient {
    private AgentClient() {
    }

    /** {@code agent}'s pose this frame ({@code limbSwing}: its walk's own, for the run's stride), or null for none (vanilla's walk). */
    public static RigPose pose(CcgAgent agent, float limbSwing, float partial) {
        if (agent.m_9236_() == null) {
            return null;
        }
        long now = agent.m_9236_().m_46467_();
        double flipAge = CcgAgent.since(now, agent.flipAt()) + partial;
        if (agent.flip() >= 0 && flipAge >= 0.0 && flipAge <= NpcRules.FLIP_POSED) {
            double landed = agent.m_20096_() && flipAge > 4.0 ? flipAge - 4.0 : -1.0;
            return PaleMotions.flip(PaleRules.Flip.byOrdinal(agent.flip()), flipAge, landed);
        }
        ItemStack held = agent.m_21205_();
        Quinque weapon = ControlItems.quinqueWeapon(held);
        Quinque shut = weapon == null ? ControlItems.quinqueCase(held) : null;
        double speed = Math.hypot(agent.m_20185_() - agent.f_19790_, agent.m_20189_() - agent.f_19792_);
        float run = AgentPose.running(speed);
        double phase = limbSwing * 0.6662;
        AgentPose.Carry carry = weapon == null ? AgentPose.Carry.CASE : weapon.twoHanded ? AgentPose.Carry.STANCE : AgentPose.Carry.BLADE;
        RigPose p = AgentPose.run(phase, run, carry);
        boolean posed = run > 0.0F;
        // Its quinque.
        double swingAge = CcgAgent.since(now, agent.swingAt()) + partial;
        int swing = agent.swing();
        RigPose arms = null;
        if (weapon != null && swing >= 0 && swing < CcgAgent.PARRY && swingAge >= 0.0 && swingAge < weapon.swing) {
            arms = QuinquePose.swing(weapon, swing, swingAge / weapon.swing);
        } else if (weapon != null && swing == CcgAgent.PARRY && swingAge >= 0.0 && swingAge < AgentPose.PARRY_TICKS) {
            arms = AgentPose.parry(weapon, swingAge / AgentPose.PARRY_TICKS);
        } else if (weapon != null) {
            float out = QuinqueClient.shown(held, true);
            if (out < 1.0F) {
                arms = QuinquePose.unfolding(weapon, out);
            } else if (weapon.twoHanded && run > 0.0F) {
                AgentPose.stanceRun(p, QuinquePose.hold(weapon), phase, run);
                posed = true;
            } else {
                arms = QuinquePose.hold(weapon);
            }
        } else if (shut != null) {
            float out = QuinqueClient.shown(held, false);
            if (out > 0.0F) {
                arms = QuinquePose.unfolding(shut, out);
            }
        }
        if (arms != null) {
            take(p, arms);
            posed = true;
        }
        if (weapon != null && weapon.ranged) {
            double shot = CcgAgent.since(now, agent.shotAt()) + partial;
            if (shot >= 0.0 && shot < 5.0) {
                QuinquePose.recoil(p, (float)(1.0 - shot / 5.0));
                posed = true;
            }
        }
        double dashAge = CcgAgent.since(now, agent.dashAt()) + partial;
        if (dashAge >= 0.0 && dashAge < AgentPose.DASH_TICKS) {
            AgentPose.dash(p, agent.dash(), dashAge / AgentPose.DASH_TICKS);
            posed = true;
        }
        return posed ? p : null;
    }

    /** The quinque's arms over the run's (its body's turn, lean, lunge and drop added to the run's). */
    static void take(RigPose p, RigPose arms) {
        if (arms.armHold > 0.0F || arms.rightArmHold > 0.0F) {
            p.rightArmX = arms.rightArmX;
            p.rightArmY = arms.rightArmY;
            p.rightArmZ = arms.rightArmZ;
            p.rightArmHold = 1.0F;
        }
        if (arms.armHold > 0.0F || arms.leftArmHold > 0.0F) {
            p.leftArmX = arms.leftArmX;
            p.leftArmY = arms.leftArmY;
            p.leftArmZ = arms.leftArmZ;
            p.leftArmHold = Math.max(arms.armHold, arms.leftArmHold);
        }
        if (p.armHold > 0.0F && arms.leftArmHold <= 0.0F && arms.armHold <= 0.0F) {
            // A one-handed swing: the free arm keeps pumping with the run.
            p.leftArmHold = Math.max(p.leftArmHold, p.armHold);
        }
        p.armHold = 0.0F;
        p.twist += arms.twist;
        p.lean += arms.lean;
        p.lunge += arms.lunge;
        p.drop += arms.drop;
    }
}
