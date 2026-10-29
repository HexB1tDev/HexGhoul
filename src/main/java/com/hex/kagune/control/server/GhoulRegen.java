package com.hex.kagune.control.server;

import com.hex.kagune.control.GhoulReserve;
import com.hex.kagune.control.wound.Regeneration;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.MutationNode;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * A ghoul's regeneration on the server ({@link Regeneration}): the wait after the last hit, then the missing health back over a
 * short stretch (each player's wounds close on every client as it comes back, see {@code WoundClient}). Food's natural
 * regeneration no longer heals a ghoul; potions of healing and regeneration effects still do. Healing is paid from the flesh
 * reserve ({@link GhoulReserve}, with the ghoul diet on), and a ghoul with none left does not regenerate.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class GhoulRegen {
    private static final Map<UUID, long[]> HURT = new HashMap<>(); // {tick of the last hit}
    private static final Map<UUID, float[]> MISSING = new HashMap<>(); // {health missing when regeneration began}
    private static boolean healing;

    private GhoulRegen() {
    }

    @SubscribeEvent
    public static void hurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getAmount() > 0.0F) {
            HURT.put(player.m_20148_(), new long[] {player.m_9236_().m_46467_()});
            MISSING.remove(player.m_20148_());
        }
    }

    @SubscribeEvent
    public static void heal(LivingHealEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && KaguneProfile.isUnlocked(player)
            && Regeneration.natural(event.getAmount(), player.m_21023_(MobEffects.f_19605_), healing)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || !KaguneProfile.isUnlocked(player)
            || !player.m_6084_()) {
            return;
        }
        float health = player.m_21223_();
        float max = player.m_21233_();
        UUID id = player.m_20148_();
        if (health >= max) {
            MISSING.remove(id);
            return;
        }
        long now = player.m_9236_().m_46467_();
        long[] hurt = HURT.computeIfAbsent(id, ignored -> new long[] {now});
        boolean diet = GhoulReserveServer.applies(player);
        float reserve = diet ? GhoulReserveServer.get(player) : 0.0F;
        if (diet && reserve <= 0.0F) {
            return;
        }
        int mastery = KaguneProfile.mastery(player);
        boolean reservoir = KaguneProfile.hasMutation(player, MutationNode.REGEN_RESERVOIR);
        long since = now - hurt[0];
        // 7.0.0: Graft DNA's body heals far faster with the kagune out (far faster still in the kakuja), the speed paid in RC; run
        // dry, it heals as any ghoul's.
        boolean kakuja = GraftServer.kakuja(player);
        boolean graft = GraftServer.ready(player) && KaguneProfile.energy(player) > 1.0F;
        int wait = Regeneration.waitTicks(mastery, reservoir);
        int heal = Regeneration.healTicks(mastery);
        if (graft) {
            double overdrive = GraftServer.hasMutation(player, com.hex.kagune.control.graft.GraftMutation.KAKUHOU_OVERDRIVE)
                ? com.hex.kagune.control.graft.GraftMutation.OVERDRIVE_SHARE : 1.0;
            wait = (int)Math.round(wait * com.hex.kagune.control.graft.GraftRules.waitShare(mastery, kakuja) * overdrive);
            heal = Math.max(1, (int)Math.round(heal * com.hex.kagune.control.graft.GraftRules.healShare(mastery, kakuja) * overdrive));
        }
        if (since < wait) {
            return;
        }
        float[] missing = MISSING.computeIfAbsent(id, ignored -> new float[] {max - health});
        float amount = Math.min(max - health, missing[0] / heal);
        if (graft && amount > 0.0F) {
            KaguneProfile.consumeEnergy(player, amount * com.hex.kagune.control.graft.GraftRules.REGEN_COST);
            if (diet) {
                reserve = GhoulReserveServer.get(player); // 7.0.0: that RC came off the same bar
            }
        }
        if (diet) {
            amount = GhoulReserve.healable(reserve, amount);
        }
        if (amount <= 0.0F) {
            return;
        }
        healing = true;
        try {
            player.m_5634_(amount);
        } finally {
            healing = false;
        }
        if (diet) {
            GhoulReserveServer.set(player, GhoulReserve.afterHeal(reserve, amount));
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        HURT.remove(event.getEntity().m_20148_());
        MISSING.remove(event.getEntity().m_20148_());
    }
}
