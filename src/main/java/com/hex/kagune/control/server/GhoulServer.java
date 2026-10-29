package com.hex.kagune.control.server;

import com.hex.kagune.control.GhoulBuffs;
import com.hex.kagune.control.GhoulKey;
import com.hex.kagune.control.RinkakuForms;
import com.hex.kagune.control.ukaku.UkakuMutation;
import com.hex.kagune.control.ukaku.UkakuRules;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CGhoulState;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.KaguneServerController;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Ghoul mode and the kagune key (3.0.0). R alone opens a ghoul's kakugan without the kagune (ghoul mode, 1.2.0: kakugan mode) and
 * closes it again; with the kagune out, R (1.2.0) puts only the kagune away and leaves the eyes open, back in kakugan mode.
 * Ctrl + R unleashes the kagune (the eyes with it), and with the kagune out puts everything away ({@link GhoulKey}). The
 * client tells which ({@code C2SGhoulKey}); the original's own manifest and dismiss still do the work ({@link PathServer#toggle},
 * which also asks for a path first). Ghoul mode lasts until it is closed, the player dies or logs out.
 *
 * <p>3.0.3: with only the eyes open a ghoul keeps the kagune's movement buffs at half their level ({@link GhoulBuffs}), and
 * the kakugan opening on its own is heard by everyone near ({@code hexkagune:eye_open}, both paths).
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class GhoulServer {
    private static final Set<UUID> GHOULS = new HashSet<>();
    /** 6.1.0: players whose eyes-only form had its buffs on last tick (so they are taken away once, when it ends). */
    private static final Set<UUID> EYES_FORM = new HashSet<>();
    /** The kakugan opening (the user's sound): played only when the eyes open without the kagune. */
    private static final SoundEvent EYE_OPEN = SoundEvent.m_262824_(ResourceLocation.m_214293_("hexkagune", "eye_open"));

    private GhoulServer() {
    }

    /** True while a player's kakugan is open in ghoul mode. */
    public static boolean ghoul(ServerPlayer player) {
        return GHOULS.contains(player.m_20148_());
    }

    /** R ({@code unleash} false) or Ctrl + R ({@code unleash} true). */
    public static void press(ServerPlayer player, boolean unleash) {
        if (!KaguneProfile.isUnlocked(player) || !player.m_6084_()) {
            return;
        }
        boolean out = KaguneProfile.isActive(player);
        GhoulKey result = GhoulKey.press(out, ghoul(player), unleash);
        // Coming out: the eyes first; going in: the kagune first, then the eyes.
        if (result.ghoul()) {
            set(player, true, !result.toggleKagune());
        }
        if (result.toggleKagune()) {
            PathServer.toggle(player);
        }
        if (!result.ghoul()) {
            set(player, false, !out);
        } else if (out && result.toggleKagune()) {
            // 1.2.0: from the kagune back to kakugan mode: the eyes stay open.
            player.m_5661_(Component.m_237115_("message.hexkagune.ghoul.kakugan_mode"), true);
        }
    }

    static void set(ServerPlayer player, boolean ghoul, boolean say) {
        boolean changed = ghoul ? GHOULS.add(player.m_20148_()) : GHOULS.remove(player.m_20148_());
        if (changed) {
            ControlNetwork.broadcast(player, new S2CGhoulState(player.m_20148_(), ghoul));
            if (say) {
                player.m_5661_(Component.m_237115_(ghoul ? "message.hexkagune.ghoul.on" : "message.hexkagune.ghoul.off"), true);
            }
            if (ghoul && say) {
                // Only the eyes are coming out (not the kagune with them): everyone near hears the kakugan open.
                player.m_284548_().m_6263_(null, player.m_20185_(), player.m_146892_().f_82480_, player.m_20189_(), EYE_OPEN,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
    }

    /**
     * With only the eyes open: the kagune's movement buffs, toned down; and (6.1.0) the eyes' own form: Resistance I and Strength I,
     * paid for with a slow RC drain ({@link GhoulBuffs#eyesCost}). Run dry, the eyes close. The eyes' buffs go the moment the
     * form ends (eyes closed, the kagune out, death).
     */
    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (!ghoul(player) || !player.m_6084_() || KaguneProfile.isActive(player)) {
            if (EYES_FORM.remove(player.m_20148_())) {
                dropEyeBuff(player, MobEffects.f_19606_, GhoulBuffs.EYES_RESISTANCE);
                dropEyeBuff(player, MobEffects.f_19600_, GhoulBuffs.EYES_STRENGTH);
            }
            return;
        }
        if (!KaguneProfile.consumeEnergy(player, GhoulBuffs.eyesCost(KaguneProfile.energyDrainEfficiency(player)))) {
            // The reserve is spent: the eyes can't stay open.
            KaguneProfile.setEnergy(player, 0.0F);
            set(player, false, false);
            player.m_5661_(Component.m_237115_("message.hexkagune.energy_depleted"), true);
            if (EYES_FORM.remove(player.m_20148_())) {
                dropEyeBuff(player, MobEffects.f_19606_, GhoulBuffs.EYES_RESISTANCE);
                dropEyeBuff(player, MobEffects.f_19600_, GhoulBuffs.EYES_STRENGTH);
            }
            return;
        }
        EYES_FORM.add(player.m_20148_());
        KaguneServerController.refreshFormEffect(player, MobEffects.f_19606_, GhoulBuffs.EYES_RESISTANCE);
        KaguneServerController.refreshFormEffect(player, MobEffects.f_19600_, GhoulBuffs.EYES_STRENGTH);
        if (UkakuServer.isUkaku(player)) {
            int mastery = KaguneProfile.mastery(player);
            boolean agile = UkakuServer.hasMutation(player, UkakuMutation.AGILITY_BOOST);
            buff(player, MobEffects.f_19596_, UkakuRules.speedAmplifier(mastery) + (agile ? UkakuRules.AGILITY_SPEED : 0));
            buff(player, MobEffects.f_19603_, UkakuRules.jumpAmplifier(mastery) + (agile ? UkakuRules.AGILITY_JUMP : 0));
        } else if (KaguneProfile.form(player).hasCentipedeLegs()) {
            // The Centipede forms' Speed (KaguneServerController.tickCentipedeFormBuffs); not their Resistance or Health Boost.
            buff(player, MobEffects.f_19596_, RinkakuForms.speed(KaguneProfile.form(player)));
        }
    }

    /**
     * Takes away the eyes' own refreshed effect ({@code refreshFormEffect} keeps it at 15 ticks or less at {@code amplifier}); a
     * potion's longer or stronger effect of the same kind is left alone.
     */
    private static void dropEyeBuff(ServerPlayer player, MobEffect effect, int amplifier) {
        MobEffectInstance current = player.m_21124_(effect);
        if (current != null && current.m_19564_() == amplifier && current.m_19557_() <= 15) {
            player.m_21195_(effect);
        }
    }

    private static void buff(ServerPlayer player, MobEffect effect, int kagune) {
        int eyes = GhoulBuffs.eyes(kagune);
        if (eyes >= 0) {
            KaguneServerController.refreshFormEffect(player, effect, eyes);
        }
    }

    @SubscribeEvent
    public static void died(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            set(player, false, false);
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        GHOULS.remove(event.getEntity().m_20148_());
        EYES_FORM.remove(event.getEntity().m_20148_());
    }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof ServerPlayer subject && ghoul(subject)) {
            ControlNetwork.sendTo(viewer, new S2CGhoulState(subject.m_20148_(), true));
        }
    }
}
