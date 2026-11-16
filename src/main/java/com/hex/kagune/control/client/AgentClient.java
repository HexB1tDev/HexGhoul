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
        // 2.0.49: the White Reaper's soar: its own flip, done turning by the top, where his aim (or his hold) takes over.
        boolean soaring = agent.flip() >= CcgAgent.SOARING;
        if (soaring && flipAge >= 0.0 && flipAge <= com.hex.kagune.control.npc.ReaperTactics.SPIN_FROM
            + com.hex.kagune.control.npc.ReaperTactics.SPIN_TICKS + 1.0) {
            double landed = agent.m_20096_() && flipAge > 4.0 ? flipAge - 4.0 : -1.0;
            return com.hex.kagune.control.client.animation.AgentPose.soar(PaleRules.Flip.byOrdinal(agent.flip() - CcgAgent.SOARING), flipAge, landed);
        }
        if (!soaring && agent.flip() >= 0 && flipAge >= 0.0 && flipAge <= NpcRules.FLIP_POSED) {
            double landed = agent.m_20096_() && flipAge > 4.0 ? flipAge - 4.0 : -1.0;
            return PaleMotions.flip(PaleRules.Flip.byOrdinal(agent.flip()), flipAge, landed);
        }
        // 4.1.28: the White Reaper taking his jacket off (AgentPose.shed), his hands empty; 4.1.29: then drawing the golden case from behind
        // his back and opening it into his blade (ReaperBladePose.draw).
        if (agent.swing() == CcgAgent.SHED) {
            // 4.1.74 (the owner: "I think same for the transformation animation, it goes by too quick, can barely see it"): its keys played
            // AgentRules.REAPER_CHANGE_SLOW times slower.
            double shedAge = AgentRules.changeAge(CcgAgent.since(now, agent.swingAt()) + partial);
            if (shedAge >= 0.0 && shedAge < AgentRules.REAPER_SHED_TICKS) {
                return AgentPose.shed(shedAge);
            }
            if (shedAge >= AgentRules.REAPER_SHED_TICKS && shedAge < AgentRules.REAPER_CHANGE_TICKS) {
                return com.hex.kagune.control.client.animation.ReaperBladePose.draw(shedAge - AgentRules.REAPER_SHED_TICKS);
            }
        }
        // 4.1.38: putting his jacket back on, the fight long over: the blade back in its case, the case away, the coat on (ReaperDressPose).
        if (agent.swing() == CcgAgent.DRESS && AgentRules.reaperLook(agent.lookIndex())) {
            double dressAge = CcgAgent.since(now, agent.swingAt()) + partial;
            if (dressAge >= 0.0 && dressAge < AgentRules.REAPER_DRESS_TICKS) {
                return com.hex.kagune.control.client.animation.ReaperDressPose.pose(dressAge);
            }
        }
        // 4.1.31: his end: the blade lowered and shattered, on his knees, held as he dies.
        if (agent.lookIndex() == AgentRules.REAPER_BARE_LOOK && agent.swing() == CcgAgent.END) {
            return com.hex.kagune.control.client.animation.ReaperBladePose.end(Math.max(0.0, CcgAgent.since(now, agent.swingAt()) + partial));
        }
        // 4.1.29: his second phase: his blade's string, or his guard (on his run).
        if (agent.lookIndex() == AgentRules.REAPER_BARE_LOOK) {
            return bladed(agent, limbSwing, partial, now);
        }
        // 4.1.25: the White Reaper's hand-to-hand (Kick Master's string or the Speed Blitz): the whole body a player's strike, legs and all,
        // Dawn carried clear (AgentPose.brawl).
        int code = agent.swing() - CcgAgent.STRIKE;
        if (code >= 0 && code < com.hex.kagune.control.pale.PaleStrike.values().length) {
            com.hex.kagune.control.pale.PaleStrike strike = com.hex.kagune.control.pale.PaleStrike.values()[code];
            double strikeAge = CcgAgent.since(now, agent.swingAt()) + partial;
            if (strikeAge >= 0.0 && strikeAge < strike.duration) {
                return AgentPose.brawl(strike, strikeAge);
            }
        }
        // 3.0.0: a kagune-made weapon, out of its case (its hand empty: the weapon is drawn from its entity data, WeaponLayer).
        com.hex.kagune.control.quinque.Arm arm = agent.weapon();
        if (arm != null && agent.m_21205_().m_41619_()) {
            float shown = com.hex.kagune.control.npc.ArmRules.shown(com.hex.kagune.control.npc.ArmRules.out(agent.armPacked()),
                CcgAgent.since(now, agent.armAt()) + partial);
            if (shown > 0.0F) {
                return armed(agent, arm, shown, limbSwing, partial, now);
            }
        }
        ItemStack held = agent.m_21205_();
        Quinque weapon = ControlItems.quinqueWeapon(held);
        Quinque shut = weapon == null ? ControlItems.quinqueCase(held) : null;
        double speed = Math.hypot(agent.m_20185_() - agent.f_19790_, agent.m_20189_() - agent.f_19792_);
        // 2.0.49: drifting back through the air after a soar is no run.
        float run = soaring && !agent.m_20096_() && flipAge <= com.hex.kagune.control.npc.ReaperTactics.LONGEST ? 0.0F : AgentPose.running(speed);
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
        } else if (weapon != null && swing >= 0 && swing < CcgAgent.PARRY && swingAge >= 0.0 && swingAge < weapon.swing(swing)) {
            arms = QuinquePose.swing(weapon, swing, swingAge / weapon.swing(swing));
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
        if (agent.seated()) {
            // 4.1.4: at its desk in the CCG building: sat on its chair, its hands on its knees, looking over its work.
            AgentPose.seated(p, agent.f_19797_ + partial, agent.m_19879_());
            posed = true;
        } else if (still > 0.0F) {
            if (AgentRules.reaperLook(agent.lookIndex())) {
                AgentPose.reaperIdle(p, agent.f_19797_ + partial, agent.m_19879_(), still); // 2.0.43: his own, at ease
            } else {
                AgentPose.idle(p, agent.f_19797_ + partial, agent.m_19879_(), still);
            }
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

    /**
     * 4.1.29: the White Reaper with his blade (ReaperBladePose): a blow of his string whole (legs and all), or his guard, standing or on his
     * run (its legs, his arms in the guard, a little lowered); a dash on top.
     */
    private static RigPose bladed(CcgAgent agent, float limbSwing, float partial, long now) {
        int step = agent.swing() - CcgAgent.BLADE;
        double age = CcgAgent.since(now, agent.swingAt()) + partial;
        if (step >= 0 && step < com.hex.kagune.control.npc.ReaperBladeRules.STEPS && age >= 0.0
            && age < com.hex.kagune.control.npc.ReaperBladeRules.blow(step).total()) {
            return com.hex.kagune.control.client.animation.ReaperBladePose.strike(step, age);
        }
        double speed = Math.hypot(agent.m_20185_() - agent.f_19790_, agent.m_20189_() - agent.f_19792_);
        float run = AgentPose.running(speed);
        RigPose guard = com.hex.kagune.control.client.animation.ReaperBladePose.guard(run);
        RigPose p;
        if (run <= 0.0F && speed < 0.02) {
            p = guard;
        } else {
            p = AgentPose.run(limbSwing * 0.6662, run, AgentPose.Carry.STANCE);
            guard.legHold = 0.0F;
            take(p, guard);
            p.leftArmHold = 1.0F;
            p.rightArmHold = 1.0F;
        }
        double dashAge = CcgAgent.since(now, agent.dashAt()) + partial;
        if (dashAge >= 0.0 && dashAge < AgentPose.DASH_TICKS) {
            AgentPose.dash(p, agent.dash(), dashAge / AgentPose.DASH_TICKS);
        }
        return p;
    }

    /**
     * 3.0.0: posed with its kagune-made weapon ({@link com.hex.kagune.control.client.animation.WeaponPose}): its run under it; a blow of its
     * string, one of its moves, a parry, tearing out of its case or going back in, its stance; a dash on top; both hands on a two-handed one
     * (but the javelin's throw).
     */
    private static RigPose armed(CcgAgent agent, com.hex.kagune.control.quinque.Arm arm, float shown, float limbSwing, float partial, long now) {
        double speed = Math.hypot(agent.m_20185_() - agent.f_19790_, agent.m_20189_() - agent.f_19792_);
        float run = AgentPose.running(speed);
        double phase = limbSwing * 0.6662;
        Quinque base = com.hex.kagune.control.client.animation.WeaponPose.base(arm);
        RigPose p = AgentPose.run(phase, run, arm.twoHanded ? AgentPose.Carry.STANCE : AgentPose.Carry.BLADE);
        int swing = agent.swing();
        double swingAge = CcgAgent.since(now, agent.swingAt()) + partial;
        int step = com.hex.kagune.control.npc.ArmRules.step(swing);
        com.hex.kagune.control.quinque.Arm.Ability ability = com.hex.kagune.control.npc.ArmRules.ability(swing);
        boolean oneHanded = !arm.twoHanded;
        RigPose arms = null;
        if (shown < 1.0F) {
            arms = QuinquePose.unfolding(base, shown);
        } else if (step >= 0 && swingAge >= 0.0 && swingAge < arm.strike(step).total()) {
            arms = com.hex.kagune.control.client.animation.WeaponPose.strike(arm, step, swingAge);
        } else if (ability != null && arm.slot(ability) >= 0 && swingAge >= 0.0 && swingAge < ability.total) {
            arms = com.hex.kagune.control.client.animation.WeaponPose.ability(arm, ability, swingAge);
            oneHanded |= com.hex.kagune.control.client.animation.WeaponPose.oneHanded(ability, swingAge);
        } else if (swing == CcgAgent.PARRY && swingAge >= 0.0 && swingAge < AgentPose.PARRY_TICKS) {
            arms = AgentPose.parry(base, swingAge / AgentPose.PARRY_TICKS);
        } else if (arm.twoHanded && run > 0.0F) {
            AgentPose.stanceRun(p, base, phase, run);
        } else {
            arms = com.hex.kagune.control.client.animation.WeaponPose.carry(arm, speed, phase);
        }
        if (arms != null) {
            take(p, arms);
        }
        p = flow(agent.m_19879_(), agent.swingAt(), swing, p, now + partial);
        double dashAge = CcgAgent.since(now, agent.dashAt()) + partial;
        if (dashAge >= 0.0 && dashAge < AgentPose.DASH_TICKS) {
            AgentPose.dash(p, agent.dash(), dashAge / AgentPose.DASH_TICKS);
        }
        if (!oneHanded && QuinquePose.gripped(shown)) {
            AgentRules.Look look = agent.look();
            QuinqueGrip.reach(p, base, look.slim() || look.female());
        }
        // 4.1.21: its wrists, for the quinque drawn in its hand (QuinqueItemRenderer).
        QuinqueWrists.posed(agent, new float[] {p.weaponX, p.weaponY, p.weaponZ});
        return p;
    }

    /** 3.0.1: how long a new blow or move blends out of whatever the last left it in (ticks). */
    static final double FLOW = 3.0;

    /** 3.0.1: each armed investigator's last pose drawn, and the swing it was drawn in (by entity id). */
    private static final java.util.Map<Integer, Flow> FLOWS = new java.util.HashMap<>();

    private static final class Flow {
        int swingAt = CcgAgent.NEVER;
        int swing = -1;
        RigPose last;
        RigPose from;
        double since;
        double drawnAt;
    }

    /**
     * 3.0.1: one blow into the next without a jump. A string's next blow (or a move) begins before the last has settled back into its
     * stance (the higher the rank, the sooner): its first ticks are blended out of the pose the last one was drawn in (a spin taken the
     * short way round, never unwound).
     */
    static RigPose flow(int id, int swingAt, int swing, RigPose p, double clock) {
        if (FLOWS.size() > 256) {
            FLOWS.clear();
        }
        Flow f = FLOWS.computeIfAbsent(id, ignored -> new Flow());
        if (f.last != null && (swingAt != f.swingAt || swing != f.swing) && clock - f.drawnAt < 10.0) {
            f.from = new RigPose().add(f.last, 1.0F);
            f.since = clock;
        }
        f.swingAt = swingAt;
        f.swing = swing;
        RigPose out = p;
        if (f.from != null) {
            double k = (clock - f.since) / FLOW;
            if (k >= 1.0 || k < 0.0) {
                f.from = null;
            } else {
                float w = (float)(k * k * (3.0 - 2.0 * k));
                f.from.spin = p.spin + (float)Math.IEEEremainder(f.from.spin - p.spin, Math.PI * 2.0);
                out = new RigPose().add(f.from, 1.0F - w).add(p, w);
                if (turned(f.from) || turned(p)) {
                    blendTurns(out, f.from, p, w);
                }
                out.legHold = Math.max(f.from.legHold, p.legHold);
                out.rightArmHold = Math.max(f.from.rightArmHold, p.rightArmHold);
                out.leftArmHold = Math.max(f.from.leftArmHold, p.leftArmHold);
                out.armHold = Math.max(f.from.armHold, p.armHold);
            }
        }
        f.last = new RigPose().add(out, 1.0F);
        f.drawnAt = clock;
        return out;
    }

    /** 3.4.8: true if {@code p} has a weapon turned in its fist (the greatsword's hold, WeaponPose.hold). */
    private static boolean turned(RigPose p) {
        return p.weaponX != 0.0F || p.weaponY != 0.0F || p.weaponZ != 0.0F;
    }

    /**
     * 3.4.8: {@code out}'s arms and the weapon in the fist {@code w} of the way from {@code from} to {@code to}, each turned the shortest way
     * round (their angles blended went the long way, through poses neither had, when the greatsword's next blow began).
     */
    private static void blendTurns(RigPose out, RigPose from, RigPose to, float w) {
        float[] right = com.hex.kagune.control.client.animation.QuinqueGrip.blendTurn(from.rightArmX, from.rightArmY, from.rightArmZ,
            to.rightArmX, to.rightArmY, to.rightArmZ, w);
        float[] left = com.hex.kagune.control.client.animation.QuinqueGrip.blendTurn(from.leftArmX, from.leftArmY, from.leftArmZ,
            to.leftArmX, to.leftArmY, to.leftArmZ, w);
        float[] held = com.hex.kagune.control.client.animation.QuinqueGrip.blendTurn(from.weaponX, from.weaponY, from.weaponZ,
            to.weaponX, to.weaponY, to.weaponZ, w);
        out.rightArmX = right[0];
        out.rightArmY = right[1];
        out.rightArmZ = right[2];
        out.leftArmX = left[0];
        out.leftArmY = left[1];
        out.leftArmZ = left[2];
        out.weaponX = held[0];
        out.weaponY = held[1];
        out.weaponZ = held[2];
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
        AgentPose.withWeapon(p, arms);
    }
}
