package com.hex.kagune.control.server;

import com.hex.kagune.control.ReaperDash;
import com.hex.kagune.control.family.HalfBlood;
import com.hex.kagune.control.npc.ChildNpc;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 2.2.1: a ghoul's and a human's child dashes as the White Reaper does ({@link ReaperDash}: three charges, one back every five seconds): in a
 * fight it closes a gap in one burst ({@link HalfBlood#DASH_FROM} to {@link HalfBlood#DASH_TO} blocks off), and, hurt, it is out of the way
 * of the next blow before it lands.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class HalfBloodServer {
    private HalfBloodServer() {
    }

    private static final class State {
        final ReaperDash.Charges charges = new ReaperDash.Charges();
        long until = Long.MIN_VALUE;
        Vec3 way = Vec3.f_82478_;
        long nextLook = Long.MIN_VALUE;
    }

    /** By the child's entity id (a dash lasts a few ticks; the charges are its own while it is loaded). */
    private static final Map<Integer, State> STATES = new HashMap<>();

    /** Its fight, now and then: a gap closed in a dash, when one is ready. */
    static void fight(ChildNpc npc, LivingEntity threat, ServerLevel level, long now) {
        State state = state(npc);
        if (now < state.until || now < state.nextLook) {
            return;
        }
        state.nextLook = now + 10L;
        double distance = Math.sqrt(npc.m_20280_(threat));
        if (distance < HalfBlood.DASH_FROM || distance > HalfBlood.DASH_TO || !npc.m_20096_() || npc.m_217043_().m_188500_() >= 0.5
            || !state.charges.spend(now)) {
            return;
        }
        Vec3 toward = threat.m_20182_().m_82546_(npc.m_20182_());
        double length = Math.hypot(toward.f_82479_, toward.f_82481_);
        if (length < 1.0E-3) {
            return;
        }
        // Into reach, not through it: the burst shortened to the gap.
        double share = Math.min(1.0, (distance - 1.6) / (ReaperDash.SPEED * ReaperDash.TICKS));
        start(npc, state, new Vec3(toward.f_82479_ / length * share, 0.0, toward.f_82481_ / length * share), level, now);
    }

    /** Hurt: out of the way of the next blow, to one side, if a dash is ready. */
    @SubscribeEvent
    public static void hurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ChildNpc npc) || !(npc.m_9236_() instanceof ServerLevel level) || !half(npc, level)) {
            return;
        }
        long now = level.m_46467_();
        State state = state(npc);
        Entity from = event.getSource().m_7639_();
        if (from == null || now < state.until || !npc.m_20096_() || !state.charges.spend(now)) {
            return;
        }
        Vec3 away = npc.m_20182_().m_82546_(from.m_20182_());
        double length = Math.hypot(away.f_82479_, away.f_82481_);
        if (length < 1.0E-3) {
            return;
        }
        double side = npc.m_217043_().m_188499_() ? 1.0 : -1.0;
        Vec3 dodge = new Vec3((away.f_82479_ / length) * 0.5 - (away.f_82481_ / length) * side, 0.0, (away.f_82481_ / length) * 0.5
            + (away.f_82479_ / length) * side);
        double norm = Math.hypot(dodge.f_82479_, dodge.f_82481_);
        start(npc, state, new Vec3(dodge.f_82479_ / norm * 0.8, 0.0, dodge.f_82481_ / norm * 0.8), level, now);
    }

    /** Its tick: a dash under way kept up. */
    static void tick(ChildNpc npc, long now) {
        State state = STATES.get(npc.m_19879_());
        if (state == null) {
            return;
        }
        if (now < state.until) {
            push(npc, state);
        } else if (state.charges.ready(now) >= ReaperDash.CHARGES && now - state.until > 200L) {
            STATES.remove(npc.m_19879_());
        }
    }

    private static void start(ChildNpc npc, State state, Vec3 way, ServerLevel level, long now) {
        state.way = way;
        state.until = now + ReaperDash.TICKS;
        npc.m_21573_().m_26573_();
        push(npc, state);
        UkakuFire.sound(level, npc.m_20182_(), "hexkagune:sfx.dash", 0.7F, 1.25F);
    }

    private static void push(ChildNpc npc, State state) {
        Vec3 motion = npc.m_20184_();
        npc.m_20256_(new Vec3(state.way.f_82479_ * ReaperDash.SPEED, Math.min(motion.f_82480_, 0.08), state.way.f_82481_ * ReaperDash.SPEED));
        npc.f_19864_ = true;
        npc.f_19789_ = 0.0F;
    }

    private static State state(ChildNpc npc) {
        if (STATES.size() > 256) {
            STATES.clear();
        }
        return STATES.computeIfAbsent(npc.m_19879_(), id -> new State());
    }

    /** True if {@code npc} is a ghoul's and a human's child. */
    static boolean half(ChildNpc npc, ServerLevel level) {
        Children.Child child = Children.get(level, npc.childId());
        com.hex.kagune.control.family.Genome genome = child == null ? null : Children.genome(level, child);
        return genome != null && genome.half();
    }
}
