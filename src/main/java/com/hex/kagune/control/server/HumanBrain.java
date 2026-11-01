package com.hex.kagune.control.server;

import com.hex.kagune.control.ControlItems;
import com.hex.kagune.control.npc.GhoulNpc;
import com.hex.kagune.control.npc.HumanNpc;
import com.hex.kagune.control.npc.HumanRules;
import com.hex.kagune.data.KaguneProfile;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Home Living (1.3.40): what The Human does ({@link HumanRules}), on the server: given a look the first time it ticks (kept in its saved
 * data), it wanders a few steps now and then; a ghoul that shows itself (a ghoul player with its kagune out, or one come too close; a
 * wild ghoul) or anyone who has hurt it sends it running straight away from them, its arms up, round whatever is in the way, until it
 * is far enough to stop, frightened a while after.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class HumanBrain {
    private HumanBrain() {
    }

    /** The server's state for one. */
    public static final class Mind {
        UUID hurtBy;
        long hurtUntil;
        long shakenUntil;
        long nextWander;
        long nextPlan;
    }

    public static void tick(HumanNpc human) {
        if (!(human.m_9236_() instanceof ServerLevel level)) {
            return;
        }
        long now = level.m_46467_();
        Mind mind = human.mind;
        if (mind == null) {
            mind = human.mind = new Mind();
            CompoundTag saved = human.getPersistentData();
            int look = saved.m_128441_(HumanNpc.KEY) ? saved.m_128451_(HumanNpc.KEY)
                : human.m_217043_().m_188503_(HumanRules.LOOKS.length * 2);
            saved.m_128405_(HumanNpc.KEY, look);
            human.dress(look);
            mind.nextWander = now + 40 + human.m_217043_().m_188503_(80);
        }
        LivingEntity danger = danger(human, level, mind, now);
        if (danger != null) {
            mind.shakenUntil = now + HumanRules.SHAKEN;
            human.scared(true);
            if (now >= mind.nextPlan || human.m_21573_().m_26571_()) {
                mind.nextPlan = now + 10;
                flee(human, danger);
            }
            return;
        }
        human.scared(now < mind.shakenUntil);
        if (now >= mind.nextWander && now >= mind.shakenUntil) {
            mind.nextWander = now + 120 + human.m_217043_().m_188503_(160);
            double angle = human.m_217043_().m_188500_() * Math.PI * 2.0;
            double length = 3.0 + human.m_217043_().m_188500_() * 6.0;
            human.m_21573_().m_26519_(human.m_20185_() + Math.cos(angle) * length, human.m_20186_(), human.m_20189_() + Math.sin(angle) * length,
                HumanRules.WALK);
        }
    }

    /** The nearest thing it is running from, or null. */
    static LivingEntity danger(HumanNpc human, ServerLevel level, Mind mind, long now) {
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        if (mind.hurtBy != null && now < mind.hurtUntil) {
            net.minecraft.world.entity.Entity by = level.m_8791_(mind.hurtBy);
            if (by instanceof LivingEntity living && living.m_6084_() && living.m_20280_(human) < HumanRules.SAFE * HumanRules.SAFE) {
                best = living;
                bestDistance = Math.sqrt(living.m_20280_(human));
            }
        }
        for (LivingEntity living : level.m_6443_(LivingEntity.class, human.m_20191_().m_82400_(HumanRules.NOTICE),
                living -> living != human && living.m_6084_())) {
            double distance = Math.sqrt(living.m_20280_(human));
            boolean ghoul;
            if (living instanceof ServerPlayer player) {
                ghoul = !player.m_5833_() && KaguneProfile.isUnlocked(player) && HumanRules.notices(distance, KaguneProfile.isActive(player));
            } else {
                ghoul = living instanceof GhoulNpc && HumanRules.notices(distance, true);
            }
            if (ghoul && distance < bestDistance) {
                best = living;
                bestDistance = distance;
            }
        }
        return best;
    }

    /** Off, straight away from {@code danger} if it can, else turned a little either way, then further round. */
    static void flee(HumanNpc human, LivingEntity danger) {
        for (double turn : new double[] {0.0, 0.6, -0.6, 1.2, -1.2, 1.8, -1.8}) {
            double[] to = HumanRules.fleeStep(human.m_20185_(), human.m_20189_(), danger.m_20185_(), danger.m_20189_(), turn, 10.0);
            if (human.m_21573_().m_26519_(to[0], human.m_20186_(), to[1], HumanRules.RUN)) {
                return;
            }
        }
    }

    /** Its meat, when it dies. */
    public static void drop(HumanNpc human, int looting) {
        Item meat = ControlItems.dishItem("human_meat");
        if (meat != null) {
            human.m_19983_(new ItemStack(meat, HumanRules.meat(human.m_217043_().m_188500_(), looting)));
        }
    }

    /** Hurt: it remembers who did it and runs from them. */
    @SubscribeEvent
    public static void hurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof HumanNpc human) || !(human.m_9236_() instanceof ServerLevel level)
            || !(event.getSource().m_7639_() instanceof LivingEntity attacker) || attacker == human) {
            return;
        }
        if (human.mind == null) {
            human.mind = new Mind();
        }
        human.mind.hurtBy = attacker.m_20148_();
        human.mind.hurtUntil = level.m_46467_() + HumanRules.HURT_MEMORY;
        human.mind.nextPlan = 0L;
    }
}
