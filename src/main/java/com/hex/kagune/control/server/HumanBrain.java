package com.hex.kagune.control.server;

import com.hex.kagune.control.ControlItems;
import com.hex.kagune.control.npc.FearLines;
import com.hex.kagune.control.npc.FinalSight;
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
 * is far enough to stop, frightened a while after. 1.3.65: War and the Dragon it sees from much further and runs from at a sprint,
 * screaming (a bubble over its head); a final kakuja from further than a kagune.
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
        /** 1.3.65: a terror it saw (War, the Dragon: a player's entity id) and until when it keeps running from it out of sight. */
        int dreadOf;
        long dreadUntil = Long.MIN_VALUE;
        /** 1.3.65: what it last ran from (an entity id, 0 nothing) and when it last screamed. */
        int ranFrom;
        long screamAt = Long.MIN_VALUE / 2;
    }

    /** 1.3.65: what it runs from, how bad a sight it is, and whether it is a ghoul it screams about. */
    record Danger(LivingEntity body, FinalSight sight, boolean ghoul) {
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
        VillagePlace.Place place = VillagePlace.of(human); // 1.3.71: one of the Brick Village's people (null: anyone else)
        Danger danger = danger(human, level, mind, now);
        if (danger != null) {
            mind.shakenUntil = now + HumanRules.SHAKEN;
            human.scared(true);
            scream(human, level, mind, danger, now);
            mind.ranFrom = danger.body().m_19879_();
            if (now >= mind.nextPlan || human.m_21573_().m_26571_()) {
                mind.nextPlan = now + 10;
                // 1.3.70 (the owner: "Humans will run to a nearby ccg agent ... if any are available"): behind the nearest investigator,
                // if one is near; else away.
                com.hex.kagune.control.npc.CcgAgent guard = guard(human, level, danger.body());
                if (guard != null) {
                    double[] spot = com.hex.kagune.control.npc.FinalSight.behind(guard.m_20185_(), guard.m_20189_(), danger.body().m_20185_(),
                        danger.body().m_20189_(), 1.6);
                    double dx = spot[0] - human.m_20185_();
                    double dz = spot[1] - human.m_20189_();
                    if (dx * dx + dz * dz > 1.2 * 1.2) {
                        human.m_21573_().m_26519_(spot[0], guard.m_20186_(), spot[1], HumanRules.pace(danger.sight()));
                    } else {
                        human.m_21573_().m_26573_();
                    }
                } else {
                    flee(human, danger.body(), HumanRules.pace(danger.sight()), danger.sight().terror() ? 14.0 : 10.0, place);
                }
            }
            return;
        }
        mind.ranFrom = 0;
        human.scared(now < mind.shakenUntil);
        if (place != null) {
            village(human, mind, place, level, now);
            return;
        }
        if (now >= mind.nextWander && now >= mind.shakenUntil) {
            mind.nextWander = now + 120 + human.m_217043_().m_188503_(160);
            double angle = human.m_217043_().m_188500_() * Math.PI * 2.0;
            double length = 3.0 + human.m_217043_().m_188500_() * 6.0;
            human.m_21573_().m_26519_(human.m_20185_() + Math.cos(angle) * length, human.m_20186_(), human.m_20189_() + Math.sin(angle) * length,
                HumanRules.WALK);
        }
    }

    /**
     * The nearest thing it is running from, or null. 1.3.65: a ghoul player as {@link HumanRules#notice} says (War and the Dragon from
     * {@link HumanRules#TERROR_NOTICE} off, another final kakuja from {@link HumanRules#DREAD_NOTICE}: beyond {@link HumanRules#NOTICE} only
     * in its sight), and a terror kept running from out of sight a while ({@link HumanRules#TERROR_MEMORY}, out to {@link HumanRules#TERROR_SAFE}).
     */
    static Danger danger(HumanNpc human, ServerLevel level, Mind mind, long now) {
        Danger best = null;
        double bestDistance = Double.MAX_VALUE;
        if (mind.hurtBy != null && now < mind.hurtUntil) {
            net.minecraft.world.entity.Entity by = level.m_8791_(mind.hurtBy);
            if (by instanceof LivingEntity living && living.m_6084_() && living.m_20280_(human) < HumanRules.SAFE * HumanRules.SAFE) {
                best = new Danger(living, living instanceof ServerPlayer hurter ? Sightings.seen(hurter) : FinalSight.NONE,
                    living instanceof GhoulNpc || living instanceof ServerPlayer hurter2 && KaguneProfile.isUnlocked(hurter2));
                bestDistance = Math.sqrt(living.m_20280_(human));
            }
        }
        for (ServerPlayer player : level.m_6907_()) {
            if (player.m_5833_() || !player.m_6084_() || !KaguneProfile.isUnlocked(player)
                || player.m_20280_(human) > HumanRules.TERROR_SAFE * HumanRules.TERROR_SAFE) {
                continue;
            }
            double distance = Math.sqrt(player.m_20280_(human));
            FinalSight sight = Sightings.seen(player);
            boolean shown = KaguneProfile.isActive(player) || sight.any() || GhoulServer.ghoul(player)
                || Sightings.last(player) != com.hex.kagune.control.npc.Display.NONE; // 1.3.68: their eyes on is enough
            boolean noticed = distance <= HumanRules.notice(sight, shown) && (distance <= HumanRules.NOTICE || human.m_142582_(player));
            if (noticed && sight.terror()) {
                mind.dreadOf = player.m_19879_();
                mind.dreadUntil = now + HumanRules.TERROR_MEMORY;
            }
            boolean remembered = sight.terror() && mind.dreadOf == player.m_19879_() && now < mind.dreadUntil;
            if ((noticed || remembered) && distance < bestDistance) {
                best = new Danger(player, sight, true);
                bestDistance = distance;
            }
        }
        // 1.3.68: a ghoul NPC only with its eyes on or its kagune out (by day a wild one hides both: just a person).
        for (GhoulNpc ghoul : level.m_6443_(GhoulNpc.class, human.m_20191_().m_82400_(HumanRules.NOTICE),
                living -> living.m_6084_() && living.mind != null && (living.mind.out || living.mind.eyes))) {
            double distance = Math.sqrt(ghoul.m_20280_(human));
            if (HumanRules.notices(distance, true) && distance < bestDistance) {
                best = new Danger(ghoul, FinalSight.NONE, true);
                bestDistance = distance;
            }
        }
        return best;
    }

    /**
     * 1.3.65: a scream, as it starts to run from a ghoul (not again for a while), and from a terror every {@link HumanRules#SCREAMS} ticks
     * or so; in a bubble over its head, to every player near who sees it.
     */
    static void scream(HumanNpc human, ServerLevel level, Mind mind, Danger danger, long now) {
        if (!danger.ghoul()) {
            return;
        }
        boolean fresh = danger.body().m_19879_() != mind.ranFrom;
        long since = now - mind.screamAt;
        boolean due = danger.sight().terror() ? since >= HumanRules.SCREAMS + human.m_217043_().m_188503_(30) : fresh && since >= 200L;
        if (!due || since < 30L) {
            return;
        }
        mind.screamAt = now;
        String line = FearLines.scream(danger.sight(), human.m_217043_().m_188503_(100));
        for (ServerPlayer player : level.m_6907_()) {
            if (player.m_20280_(human) <= HumanRules.HEARD * HumanRules.HEARD && human.m_142582_(player)) {
                com.hex.kagune.control.network.ControlNetwork.sendTo(player, new com.hex.kagune.control.network.S2CNpcSay(human.m_19879_(), line));
            }
        }
    }

    /**
     * 1.3.70: the investigator it runs to from {@code danger}: the nearest alive within {@code AgentRules.DEFEND}, never one it would have to
     * run past the danger to reach; null if there is none.
     */
    static com.hex.kagune.control.npc.CcgAgent guard(HumanNpc human, ServerLevel level, LivingEntity danger) {
        com.hex.kagune.control.npc.CcgAgent best = null;
        double bestDistance = Double.MAX_VALUE;
        double range = com.hex.kagune.control.npc.AgentRules.DEFEND;
        for (com.hex.kagune.control.npc.CcgAgent agent : level.m_6443_(com.hex.kagune.control.npc.CcgAgent.class, human.m_20191_().m_82400_(range),
                a -> a.m_6084_() && a != danger)) {
            // Not one it would have to run past the danger to reach.
            boolean past = danger.m_20280_(agent) < danger.m_20280_(human) && agent.m_20280_(human) > danger.m_20280_(human);
            double distance = agent.m_20280_(human);
            if (!past && distance < bestDistance) {
                best = agent;
                bestDistance = distance;
            }
        }
        return best;
    }

    /** Off, straight away from {@code danger} if it can, else turned a little either way, then further round. */
    static void flee(HumanNpc human, LivingEntity danger, double pace, double length, VillagePlace.Place place) {
        for (double turn : new double[] {0.0, 0.6, -0.6, 1.2, -1.2, 1.8, -1.8}) {
            double[] to = HumanRules.fleeStep(human.m_20185_(), human.m_20189_(), danger.m_20185_(), danger.m_20189_(), turn, length);
            if (place != null) {
                to = place.clamp(to[0], to[1], 2.0); // 1.3.71: never past the village's perimeter, even running
            }
            if (human.m_21573_().m_26519_(to[0], human.m_20186_(), to[1], pace)) {
                return;
            }
        }
    }

    /**
     * 1.3.71: a Brick Village human's day ({@code VillageRules}): by night it goes home (where it was placed, inside its house) and stays;
     * by day, now and then, it walks to a spot on the streets near it; never past the perimeter.
     */
    static void village(HumanNpc human, Mind mind, VillagePlace.Place place, ServerLevel level, long now) {
        if (com.hex.kagune.control.npc.NpcRules.night(level.m_46468_())) {
            double dx = place.homeX() + 0.5 - human.m_20185_();
            double dz = place.homeZ() + 0.5 - human.m_20189_();
            if (dx * dx + dz * dz > 1.5 * 1.5) {
                if (now % 20L == 0L || human.m_21573_().m_26571_()) {
                    human.m_21573_().m_26519_(place.homeX() + 0.5, place.homeY(), place.homeZ() + 0.5, com.hex.kagune.control.village.VillageRules.HOMEWARD);
                }
            } else {
                human.m_21573_().m_26573_();
            }
            return;
        }
        if (now < mind.nextWander || now < mind.shakenUntil || !human.m_21573_().m_26571_()) {
            return;
        }
        mind.nextWander = now + com.hex.kagune.control.village.VillageRules.STROLL_EVERY
            + human.m_217043_().m_188503_(com.hex.kagune.control.village.VillageRules.STROLL_MORE);
        int[][] streets = com.hex.kagune.control.village.BrickVillage.STREETS;
        double reach = com.hex.kagune.control.village.VillageRules.STROLL_REACH;
        for (int tries = 0; tries < 12; tries++) {
            double[] to = place.world(streets[human.m_217043_().m_188503_(streets.length)]);
            double dx = to[0] - human.m_20185_();
            double dz = to[2] - human.m_20189_();
            if (dx * dx + dz * dz <= reach * reach && place.inside(to[0], to[2])) {
                human.m_21573_().m_26519_(to[0], to[1], to[2], com.hex.kagune.control.village.VillageRules.STROLL);
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
