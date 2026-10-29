package com.hex.kagune.control.server;

import com.hex.kagune.control.pale.PaleRules;
import com.hex.kagune.control.pale.KickTumble;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 6.2.0: a body the Speed Kick launched is down for a moment where it comes down (every client draws it lying there and getting
 * up, {@link KickTumble}, finding its landing the same way): from when it touches the ground it is held there, slowed to nothing,
 * for {@link PaleRules#KICK_DOWN_TICKS} and while it gets up ({@link PaleRules#KICK_GETUP_TICKS}).
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class PaleKnockdown {
    /** A launched body: its world, when it was kicked and when it came down (MIN_VALUE: still in the air). */
    private static final class Down {
        final ServerLevel level;
        final long at;
        /** 6.2.3: how long it lies there, and whether it is stunned all the while (Witness This's crater). */
        final int lie;
        final boolean stunned;
        long landed = Long.MIN_VALUE;

        Down(ServerLevel level, long at, int lie, boolean stunned) {
            this.level = level;
            this.at = at;
            this.lie = lie;
            this.stunned = stunned;
        }
    }

    /** The launched bodies, by entity id. */
    private static final Map<Integer, Down> DOWN = new HashMap<>();

    private PaleKnockdown() {
    }

    /** {@code body} was kicked off its feet now. */
    static void launched(LivingEntity body, long now) {
        launched(body, now, PaleRules.KICK_DOWN_TICKS, false);
    }

    /** 6.2.3: ...to lie there {@code lie} ticks once down, {@code stunned} (it can do nothing) until it is up again. */
    static void launched(LivingEntity body, long now, int lie, boolean stunned) {
        if (body.m_9236_() instanceof ServerLevel level) {
            DOWN.put(body.m_19879_(), new Down(level, now, lie, stunned));
        }
    }

    @SubscribeEvent
    public static void levelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level) || DOWN.isEmpty()) {
            return;
        }
        long now = level.m_46467_();
        for (Iterator<Map.Entry<Integer, Down>> it = DOWN.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Down> entry = it.next();
            Down down = entry.getValue();
            if (down.level != level) {
                continue;
            }
            Entity entity = level.m_6815_(entry.getKey());
            if (!(entity instanceof LivingEntity body) || !body.m_6084_() || now < down.at) {
                it.remove();
                continue;
            }
            long since = now - down.at;
            if (down.landed == Long.MIN_VALUE) {
                if ((since >= 3 && body.m_20096_()) || since >= KickTumble.AIRBORNE_MAX) {
                    down.landed = now;
                } else {
                    continue;
                }
            }
            if (now - down.landed >= down.lie + PaleRules.KICK_GETUP_TICKS) {
                it.remove();
                continue;
            }
            // Down: it skids to a stop and stays there until it is up.
            body.m_147207_(new MobEffectInstance(MobEffects.f_19597_, 3, 6, false, false, false), null);
            if (down.stunned) {
                PaleStun.stun(body, now, 3);
            }
        }
    }
}
