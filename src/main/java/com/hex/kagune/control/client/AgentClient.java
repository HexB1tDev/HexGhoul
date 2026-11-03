package com.hex.kagune.control.client;

import com.hex.kagune.control.ControlItems;
import com.hex.kagune.control.client.animation.AgentPose;
import com.hex.kagune.control.client.animation.PaleMotions;
import com.hex.kagune.control.client.animation.QuinqueGrip;
import com.hex.kagune.control.client.animation.QuinquePose;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.npc.AgentRules;
import com.hex.kagune.control.npc.CcgAgent;
import com.hex.kagune.control.npc.NpcRules;
import com.hex.kagune.control.pale.PaleRules;
import com.hex.kagune.control.quinque.Quinque;
import net.minecraft.world.item.ItemStack;

/**
 * 1.3.27: a CCG investigator's pose as this client draws it, from what its entity data says ({@link CcgAgent}): a flip over everything
 * (a player's, {@code PaleMotions.flip}); otherwise its run (AgentPose: the same for every one of them) under its quinque (a swing of its
 * string, a parry, the case opening or shutting, its stance, the launcher's kick) and a dash on top. Pure reading: nothing is kept.
 *
 * <p>1.3.28: standing about out of a fight it idles (AgentPose.idle: leaning back, its weight on one leg); its shut case is carried by
 * the handle (AgentPose.carry: the arm hanging plumb whatever the body does, swinging a little with its stride), the case drawn in its
 * carried frame (QuinqueClient.caseFrame) so it hangs straight down from the fist.
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
        if (weapon == Quinque.DAWN && swing == CcgAgent.KICK && swingAge >= 0.0 && swingAge < AgentRules.REAPER_KICK_DURATION) {
            arms = AgentPose.kick(swingAge);
        } else if (weapon == Quinque.DAWN && swing == CcgAgent.GROUND && swingAge >= 0.0 && swingAge < com.hex.kagune.control.npc.ReaperTendril.DURATION) {
            arms = AgentPose.ground(swingAge); // 2.0.33: Dawn driven into the ground for his tendril
        } else if (weapon == Quinque.DAWN && swing == CcgAgent.AIM_DAWN && swingAge >= 0.0 && swingAge <= AgentRules.DAWN_AIM_TICKS + 2) {
            arms = AgentPose.aimDawn(swingAge, agent.m_146909_());
        } else if (weapon != null && swing >= 0 && swing < CcgAgent.PARRY && swingAge >= 0.0 && swingAge < weapon.swing) {
            arms = QuinquePose.swing(weapon, swing, swingAge / weapon.swing);
            if (weapon == Quinque.DAWN) {
                // 2.0.31: its dark ribbons along the way its point goes.
                com.hex.kagune.control.client.fx.DawnTrails.swing(agent, swing, agent.swingAt(), swingAge / weapon.swing, partial);
            }
        } else if (weapon != null && swing == CcgAgent.PARRY && swingAge >= 0.0 && swingAge < AgentPose.PARRY_TICKS) {
            arms = AgentPose.parry(weapon, swingAge / AgentPose.PARRY_TICKS);
        } else if (weapon != null) {
            float out = QuinqueClient.shown(held, true);
            if (out < 1.0F) {
                arms = QuinquePose.unfolding(weapon, out);
            } else if (weapon.twoHanded && run > 0.0F) {
                AgentPose.stanceRun(p, weapon, phase, run);
                posed = true;
            } else {
                arms = weapon == Quinque.DAWN ? AgentPose.aimDawn(0.0, 0.0F) : QuinquePose.hold(weapon);
                double shot = CcgAgent.since(now, agent.shotAt()) + partial;
                if (weapon == Quinque.DAWN && shot >= 0.0 && shot < QuinquePose.KICK_TICKS) {
                    arms = AgentPose.aimDawn(AgentRules.DAWN_AIM_TICKS, agent.m_146909_());
                    QuinquePose.recoil(arms, weapon, (float)(1.0 - shot / QuinquePose.KICK_TICKS));
                }
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
        // 1.3.28: standing about with its case shut: its idle.
        boolean carried = weapon == null && (shut == null || QuinqueClient.shown(held, false) <= 0.0F);
        float still = carried ? AgentPose.still(speed) : 0.0F;
        if (still > 0.0F) {
            AgentPose.idle(p, agent.f_19797_ + partial, agent.m_19879_(), still);
            posed = true;
        }
        if (weapon != null && weapon.ranged) {
            // 1.3.33: the gun along its look, up or down with its head, while it is not clubbing with it.
            boolean clubbing = swing >= 0 && swing < CcgAgent.PARRY && swingAge >= 0.0 && swingAge < weapon.swing;
            if (!clubbing && QuinqueClient.shown(held, true) >= 1.0F) {
                float pitch = (float)Math.toRadians(Math.max(-60.0F, Math.min(60.0F, agent.m_146909_())));
                QuinquePose.pitchBy(p, weapon, pitch * (run > 0.0F ? 0.5F : 1.0F));
                posed = true;
            }
            double shot = CcgAgent.since(now, agent.shotAt()) + partial;
            if (shot >= 0.0 && shot < QuinquePose.KICK_TICKS) {
                QuinquePose.recoil(p, weapon, (float)(1.0 - shot / QuinquePose.KICK_TICKS));
                posed = true;
            }
        }
        double dashAge = CcgAgent.since(now, agent.dashAt()) + partial;
        if (dashAge >= 0.0 && dashAge < AgentPose.DASH_TICKS) {
            AgentPose.dash(p, agent.dash(), dashAge / AgentPose.DASH_TICKS);
            posed = true;
        }
        // 1.3.28: its case shut, carried by the handle (last: the arm hangs plumb from wherever the body has gone).
        if (carried && shut != null) {
            AgentPose.carry(p, AgentPose.sway(phase, (float)Math.min(1.0, speed / 0.1), run));
            posed = true;
        }
        // 1.3.33: both hands on a two-handed quinque, whatever moved the body and the right arm (last of all).
        if (weapon != null && weapon.twoHanded && QuinquePose.gripped(QuinqueClient.shown(held, true))) {
            AgentRules.Look look = agent.look();
            QuinqueGrip.reach(p, weapon, look.slim() || look.female());
        }
        return posed ? p : null;
    }

    /** 1.3.33: a dash's afterimages: how long each lasts (ticks), how see-through it starts, its colour (a cold white), how often one. */
    static final double GHOST_LIFE = 6.0;
    static final float GHOST_ALPHA = 0.34F;
    static final int GHOST_COLOUR = 0xC8D6E6;
    static final double GHOST_EVERY = 1.0;
    /** Each investigator's last afterimage (PaleClient's clock), by entity id. */
    private static final java.util.Map<Integer, Double> GHOSTED = new java.util.HashMap<>();

    /**
     * 1.3.33: while {@code agent} dashes, a brief afterimage of it left behind every tick of the push (frozen in the pose it was in, a
     * cold white, gone in a third of a second). The owner: "dash left, right, with some afterimages maybe, just very briefly".
     */
    public static void afterimages(CcgAgent agent, RigPose posed, float partial) {
        if (agent.m_9236_() == null || posed == null) {
            return;
        }
        double age = CcgAgent.since(agent.m_9236_().m_46467_(), agent.dashAt()) + partial;
        if (!(age >= 0.5 && age <= AgentRules.DASH_TICKS + 0.5)) {
            return;
        }
        double clock = PaleClient.clock(partial);
        Double last = GHOSTED.get(agent.m_19879_());
        if (last != null && clock - last < GHOST_EVERY && clock >= last) {
            return;
        }
        if (GHOSTED.size() > 256) {
            GHOSTED.clear();
        }
        GHOSTED.put(agent.m_19879_(), clock);
        double x = agent.f_19790_ + (agent.m_20185_() - agent.f_19790_) * partial;
        double y = agent.f_19791_ + (agent.m_20186_() - agent.f_19791_) * partial;
        double z = agent.f_19792_ + (agent.m_20189_() - agent.f_19792_) * partial;
        float yaw = agent.f_20884_ + (agent.f_20883_ - agent.f_20884_) * partial;
        AgentRules.Look look = agent.look();
        float size = AgentRules.size(look);
        com.hex.kagune.control.client.render.PaleAfterimages.add(new RigPose().add(posed, 1.0F), new net.minecraft.world.phys.Vec3(x, y, z), yaw, clock,
            GHOST_COLOUR, GHOST_LIFE, GHOST_ALPHA, size * AgentRules.build(look), size);
    }

    /**
     * The quinque's arms over the run's (its body's turn, lean, lunge and drop added to the run's; 1.3.33: its spin and its head's turn
     * against it, and its step's legs over the run's as far as it holds them).
     */
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
        p.spin += arms.spin;
        p.headX += arms.headX;
        p.headY += arms.headY;
        p.headZ += arms.headZ;
        if (arms.legHold > 0.0F) {
            float k = Math.min(1.0F, arms.legHold);
            p.rightLegX += (arms.rightLegX - p.rightLegX) * k;
            p.leftLegX += (arms.leftLegX - p.leftLegX) * k;
            p.rightLegZ += (arms.rightLegZ - p.rightLegZ) * k;
            p.leftLegZ += (arms.leftLegZ - p.leftLegZ) * k;
            p.legHold = Math.max(p.legHold, k);
        }
    }
}
