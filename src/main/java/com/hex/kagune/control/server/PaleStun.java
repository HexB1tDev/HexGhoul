package com.hex.kagune.control.server;

import com.hex.kagune.control.pale.StunRules;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 6.2.0: bodies Pale's fists have stunned ({@link StunRules}). While a stun holds, anything it would hurt is spared (its own blows,
 * what it shot or set off: the attack is cancelled before it lands, so no knockback, no flash) and it is held where it is (its own
 * walking slowed to nothing; a blow still sends it flying).
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class PaleStun {
    /**
     * A stunned body: its world, the game tick its stun runs to, when it began without a break, and (a player who broke free) until
     * when it can't be stunned again.
     */
    private static final class Stun {
        final ServerLevel level;
        long until;
        long since;
        long immuneUntil = Long.MIN_VALUE;

        Stun(ServerLevel level, long until) {
            this.level = level;
            this.until = until;
            this.since = until;
        }
    }

    /** The stunned bodies, by entity id. */
    private static final Map<Integer, Stun> STUNNED = new HashMap<>();

    private PaleStun() {
    }

    /**
     * {@code body} was hit now: stunned for {@code ticks} more (renewed, capped). 7.0.2: a player kept stunned too long breaks free
     * ({@link StunRules#breaksFree}) and can't be stunned for a moment after. True if it is stunned now (false: it broke free, or
     * can't be stunned yet).
     */
    static boolean stun(LivingEntity body, long now, int ticks) {
        if (!(body.m_9236_() instanceof ServerLevel level)) {
            return false;
        }
        Stun stun = STUNNED.get(body.m_19879_());
        if (stun == null || stun.level != level) {
            stun = new Stun(level, now);
            STUNNED.put(body.m_19879_(), stun);
        }
        boolean player = body instanceof net.minecraft.world.entity.player.Player;
        if (player && now < stun.immuneUntil) {
            return false;
        }
        if (!StunRules.holds(stun.until, now)) {
            stun.since = now; // a fresh stun, after a break
        }
        if (player && StunRules.breaksFree(stun.since, now)) {
            stun.until = now;
            stun.immuneUntil = now + StunRules.PLAYER_IMMUNE;
            return false;
        }
        stun.until = StunRules.renewed(stun.until, now, ticks);
        hold(body);
        return true;
    }

    /** True while {@code entity} is stunned. */
    static boolean stunned(Entity entity) {
        if (entity == null) {
            return false;
        }
        Stun stun = STUNNED.get(entity.m_19879_());
        return stun != null && StunRules.holds(stun.until, stun.level.m_46467_()) && stun.level.m_6815_(entity.m_19879_()) == entity;
    }

    /** A stunned body can't hurt anything: not with its own blows, nor with what it shot or set off. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void attack(LivingAttackEvent event) {
        if (STUNNED.isEmpty()) {
            return;
        }
        DamageSource source = event.getSource();
        if (source == null) {
            return;
        }
        Entity attacker = source.m_7639_();
        Entity direct = source.m_7640_();
        if ((attacker != event.getEntity() && stunned(attacker)) || (direct != event.getEntity() && stunned(direct))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void levelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level) || STUNNED.isEmpty()) {
            return;
        }
        long now = level.m_46467_();
        for (Iterator<Map.Entry<Integer, Stun>> it = STUNNED.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Stun> entry = it.next();
            Stun stun = entry.getValue();
            if (stun.level != level) {
                continue;
            }
            Entity entity = level.m_6815_(entry.getKey());
            if (!(entity instanceof LivingEntity body) || !body.m_6084_() || !StunRules.holds(stun.until, now) && now >= stun.immuneUntil) {
                it.remove();
                continue;
            }
            if (!StunRules.holds(stun.until, now)) {
                continue; // broke free: kept only to remember it can't be stunned yet
            }
            hold(body);
        }
    }

    /** Its own walking slowed to nothing for the next few ticks (renewed each tick while the stun holds). */
    private static void hold(LivingEntity body) {
        body.m_147207_(new MobEffectInstance(MobEffects.f_19597_, 3, StunRules.SLOW_AMPLIFIER, false, false, false), null);
    }
}
