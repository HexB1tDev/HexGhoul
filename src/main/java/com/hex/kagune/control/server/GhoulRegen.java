package com.hex.kagune.control.server;

import com.hex.kagune.control.GhoulReserve;
import com.hex.kagune.control.wound.Regeneration;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.MutationNode;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * A ghoul's regeneration on the server ({@link Regeneration}). 4.2.2: only while the regeneration key is held, slower and at a fifth of the
 * price ({@link #playerTick}); before, it was by itself: the wait after the last hit, then the missing health back over a short stretch.
 * Each player's wounds close on every client as health comes back ({@code WoundClient}); not held, they stay. Food's natural regeneration
 * never heals a ghoul ({@link #heal}: nor any other heal of 1.5 or less the mod didn't make through here, such as the original's Regen
 * Reservoir heal), potions of healing and regeneration effects still do. Healing is paid from the flesh reserve ({@link GhoulReserve}, with
 * the ghoul diet on), and a ghoul with none left does not regenerate. A Bikaku ghoul's regeneration costs nothing; once Monstrous
 * Regeneration is his ({@link #healOwn}, from BikakuServer: its cooldown its price), it is his only one. 4.0.0 to 4.2.1 a Bikaku's wounds
 * also closed by themselves after the wait.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class GhoulRegen {
    /** 1.3.25: until when a quinque's cut holds a ghoul's regeneration back (RC suppression), by player. */
    private static final Map<UUID, Long> SUPPRESSED = WorldTimes.map();
    private static boolean healing;

    private GhoulRegen() {
    }

    /**
     * 3.5.2: a heal of the mod's own on a ghoul (Bikaku's Predatory Feeding): never taken for food's natural regeneration, which
     * {@link #heal} cancels (any heal of 1.5 or less it didn't make itself).
     */
    public static void healOwn(ServerPlayer player, float amount) {
        if (amount <= 0.0F) {
            return;
        }
        healing = true;
        try {
            player.m_5634_(amount);
        } finally {
            healing = false;
        }
    }

    /** 1.3.25: a quinque cut {@code player}: their regeneration holds back until {@code until} (game time); the later of two cuts. */
    public static void suppress(ServerPlayer player, long until) {
        SUPPRESSED.merge(player.m_20148_(), until, Math::max);
    }

    @SubscribeEvent
    public static void heal(LivingHealEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && KaguneProfile.isUnlocked(player)
            && Regeneration.natural(event.getAmount(), player.m_21023_(MobEffects.f_19605_), healing)) {
            event.setCanceled(true);
        }
    }

    /**
     * 4.2.2 (the owner: "To regenerate, you need to press that key, but it's optional. You don't automatically regenerate anymore. You need to
     * hold the key down"): each player's last word on the regeneration key (RegenClient sends it as it goes down, every half second while it is
     * held, and as it comes up), by game time. Held is a word within {@link #HELD_FOR} ticks: a client that went quiet stops regenerating.
     */
    private static final Map<UUID, Long> HELD = WorldTimes.map(); // forgotten with its world, like every map of game times
    /** 4.2.2: how long a "held" counts with no word since (ticks; the client says it every 10). */
    static final long HELD_FOR = 30L;

    /** 4.2.2: {@code player}'s regeneration key down or up (C2SRegen). */
    public static void held(ServerPlayer player, boolean down) {
        if (down) {
            HELD.put(player.m_20148_(), player.m_9236_().m_46467_());
        } else {
            HELD.remove(player.m_20148_());
        }
    }

    /** 4.2.2: true while {@code player} holds the regeneration key ({@link #HELD_FOR}). */
    public static boolean holding(ServerPlayer player) {
        Long at = HELD.get(player.m_20148_());
        long now = player.m_9236_().m_46467_();
        return at != null && now >= at && now - at <= HELD_FOR;
    }

    /**
     * 4.2.2: a ghoul regenerates only while holding the regeneration key (any state: eyes shut, the kakugan, the kagune out, any kakuja, War,
     * Peace), at {@link Regeneration#heldPerTick} (slower than the old burst), paid at a fifth of the old price ({@link GhoulReserve#REGEN_PER_HEALTH};
     * Graft DNA's faster healing, its kagune out, its RC at the same share). Not held, the wounds stay. A Bikaku ghoul's regeneration, once
     * Monstrous Regeneration is his, is that alone (BikakuServer.ownRegeneration): the owner, "except for the Bikaku. That guy already has a
     * regeneration ability. That from now on is that."; until then he holds the key too, for nothing. A quinque's cut still holds it back.
     */
    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || !KaguneProfile.isUnlocked(player)
            || !player.m_6084_()) {
            return;
        }
        float health = player.m_21223_();
        float max = player.m_21233_();
        UUID id = player.m_20148_();
        if (health >= max || !holding(player) || BikakuServer.ownRegeneration(player)) {
            return;
        }
        long now = player.m_9236_().m_46467_();
        Long held = SUPPRESSED.get(id);
        if (held != null) {
            if (now < held) {
                return; // 1.3.25: a quinque's cut: nothing grows back yet
            }
            SUPPRESSED.remove(id);
        }
        // 4.0.0: a Bikaku's healing costs him nothing (the owner: "regen for them does not cost energy at all").
        boolean diet = !BikakuServer.isBikaku(player) && GhoulReserveServer.applies(player);
        float reserve = diet ? GhoulReserveServer.get(player) : 0.0F;
        if (diet && reserve <= 0.0F) {
            return;
        }
        int mastery = KaguneProfile.mastery(player);
        boolean reservoir = KaguneProfile.hasMutation(player, MutationNode.REGEN_RESERVOIR);
        // 7.0.0: Graft DNA's body heals faster with the kagune out (faster still in the kakuja), the speed paid in RC; run dry, as any ghoul's.
        boolean kakuja = GraftServer.kakuja(player);
        boolean graft = GraftServer.ready(player) && KaguneProfile.energy(player) > 1.0F;
        double speed = 1.0;
        if (graft) {
            double overdrive = GraftServer.hasMutation(player, com.hex.kagune.control.graft.GraftMutation.KAKUHOU_OVERDRIVE)
                ? com.hex.kagune.control.graft.GraftMutation.OVERDRIVE_SHARE : 1.0;
            speed = 1.0 / Math.max(0.05, com.hex.kagune.control.graft.GraftRules.healShare(mastery, kakuja) * overdrive);
        }
        float amount = Math.min(max - health, Regeneration.heldPerTick(mastery, reservoir, speed));
        if (graft && amount > 0.0F) {
            KaguneProfile.consumeEnergy(player, amount * com.hex.kagune.control.graft.GraftRules.REGEN_COST * Regeneration.PRICE_SHARE);
            if (diet) {
                reserve = GhoulReserveServer.get(player); // 7.0.0: that RC came off the same bar
            }
        }
        if (diet) {
            amount = GhoulReserve.healable(reserve, amount, GhoulReserve.REGEN_PER_HEALTH);
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
            GhoulReserveServer.set(player, GhoulReserve.afterHeal(reserve, amount, GhoulReserve.REGEN_PER_HEALTH));
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        HELD.remove(event.getEntity().m_20148_());
    }

    /**
     * 2.4.8: the original's regeneration's particles (hearts at the player on every heal: the owner didn't want them): none drawn. Called
     * in place of {@code ServerLevel.sendParticles} by the patched {@code tickRegeneration}.
     */
    public static int particles(net.minecraft.server.level.ServerLevel level, net.minecraft.core.particles.ParticleOptions type, double x, double y, double z, int count, double dx,
                                double dy, double dz, double speed) {
        return 0;
    }
}
