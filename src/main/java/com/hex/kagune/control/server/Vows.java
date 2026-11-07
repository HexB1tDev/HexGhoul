package com.hex.kagune.control.server;

import com.hex.kagune.control.npc.Romance;
import com.hex.kagune.control.quest.QuestLog;
import com.hex.kagune.control.village.HumanPersona;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 3.3.4 (the owner, replacing break-ups and divorce: "From now on, you can only have one wife, no matter what ... There's no divorce
 * button. No anything. When you're married to them, that's it. The only way you can remarry is if you kill them. But if it's an
 * official NPC like Toka, Ken, or Ito, there's no way to unmarry or to anything. You guys are stuck forever."): one partner at a
 * time, across the companions (Eto, Touka, Ken) and the villagers alike. Nobody new is asked out while a player has a partner,
 * dating or married; nobody is married while they are married. A villager partner's death frees them ({@link #died}); a companion's
 * never does ({@link Romance#endsWithDeath}): she comes back, still theirs. Saves that already have more than one partner keep them.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class Vows {
    private Vows() {
    }

    /** A villager's bond key: its UUID (the companions' are their names). */
    static boolean villager(String key) {
        return Romance.villagerKey(key);
    }

    /** One of {@code player}'s partners other than {@code except} (a companion's key or a villager's id), dating or married; null if none. */
    public static String partnerElsewhere(ServerLevel level, ServerPlayer player, String except) {
        Map<String, Romance.Stage> companions = UniqueNpcs.partnersOf(level, player, except == null ? "" : except);
        if (!companions.isEmpty()) {
            return companions.keySet().iterator().next();
        }
        for (Map.Entry<String, QuestLog.Bond> bond : QuestServer.log(player).bonds().entrySet()) {
            if (villager(bond.getKey()) && !bond.getKey().equals(except) && bond.getValue().has(HumanPersona.PARTNER)) {
                return bond.getKey();
            }
        }
        return null;
    }

    /** Whether {@code player} is married to anyone other than {@code except}, a companion or a villager. */
    public static boolean marriedElsewhere(ServerLevel level, ServerPlayer player, String except) {
        if (UniqueNpcs.spouseOf(level, player, except) != null) {
            return true;
        }
        for (Map.Entry<String, QuestLog.Bond> bond : QuestServer.log(player).bonds().entrySet()) {
            if (villager(bond.getKey()) && !bond.getKey().equals(except) && bond.getValue().has(HumanPersona.MARRIED)) {
                return true;
            }
        }
        return false;
    }

    /**
     * A villager died: whoever it was with (dating or married) is free again, the one way a vow ends ({@link Romance#endsWithDeath}).
     * Only partners online in its world to hear it (a bond is kept with the player).
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void died(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof com.hex.kagune.control.npc.HumanNpc human) || !(human.m_9236_() instanceof ServerLevel level)
            || !Romance.endsWithDeath(false)) {
            return;
        }
        String key = human.m_20148_().toString();
        for (ServerPlayer player : level.m_6907_()) {
            QuestLog.Bond bond = QuestServer.bond(player, key);
            if (!bond.has(HumanPersona.PARTNER) && !bond.has(HumanPersona.MARRIED)) {
                continue;
            }
            boolean wed = bond.has(HumanPersona.MARRIED);
            QuestServer.bond(player, key, bond.with(HumanPersona.PARTNER, false).with(HumanPersona.MARRIED, false));
            boolean theirs = event.getSource().m_7639_() == player;
            player.m_5661_(Component.m_237113_("§4" + HumanBrain.name(human) + " is dead" + (theirs ? ", by your hand" : "") + ". "
                + (wed ? "You're free to marry again." : "Whatever you had is over.")), false);
        }
    }
}
