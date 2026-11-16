package com.hex.kagune.control.server;

import com.hex.kagune.control.KaguneColours;
import com.hex.kagune.data.KaguneAppearance;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.network.KaguneNetwork;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 1.3.34: every player's Rinkaku kagune gets a colour of its own ({@link KaguneColours}) the first time they spawn in, unless they had
 * already chosen one (anything but the original's crimson); {@code /kagune color} says which, {@code /kagune color reroll} rolls
 * another. Kept in the original's appearance (its primary colour and its veins), so the tendrils, the mask and the shards follow, and
 * sent to everyone who sees them (KaguneNetwork.broadcast).
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class KaguneColourServer {
    /** Set in the player's persistent data once their colour has been given. */
    static final String ROLLED = "HexKaguneColourRolled";

    private KaguneColourServer() {
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        try {
            CompoundTag data = player.getPersistentData();
            if (data.m_128471_(ROLLED)) {
                return;
            }
            data.m_128379_(ROLLED, true);
            if (KaguneProfile.color(player) != KaguneColours.DEFAULT) {
                return; // one they chose themselves is kept
            }
            KaguneColours.Pick pick = roll(player);
            player.m_5661_(Component.m_237113_("§7Your kagune will come out " + KaguneColours.name(pick.colour()) + " ("
                + KaguneColours.hex(pick.colour()) + "). §8/kagune color reroll for another."), false);
        } catch (RuntimeException failure) {
            com.hex.kagune.control.Safety.caught(failure, "a kagune's colour");
        }
    }

    /** A new colour for {@code player}'s kagune, kept and sent to everyone who sees them. */
    static KaguneColours.Pick roll(ServerPlayer player) {
        KaguneColours.Pick pick = KaguneColours.roll(() -> player.m_217043_().m_188500_());
        KaguneAppearance was = KaguneProfile.appearance(player);
        KaguneProfile.setAppearance(player, new KaguneAppearance(pick.colour(), pick.vein(), was.lengthScale(), was.thicknessScale(), was.taperStyle(),
            was.surfaceStyle(), was.tipStyle()).validated());
        KaguneNetwork.broadcast(player);
        return pick;
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.m_82127_("kagune").then(Commands.m_82127_("color").executes(context -> {
            ServerPlayer player = context.getSource().m_81375_();
            int colour = KaguneProfile.color(player);
            context.getSource().m_288197_(() -> Component.m_237113_("§7Your kagune is " + KaguneColours.name(colour) + " (" + KaguneColours.hex(colour)
                + "). §8/kagune color reroll for another."), false);
            return 1;
        }).then(Commands.m_82127_("reroll").executes(context -> {
            ServerPlayer player = context.getSource().m_81375_();
            KaguneColours.Pick pick = roll(player);
            player.getPersistentData().m_128379_(ROLLED, true);
            context.getSource().m_288197_(() -> Component.m_237113_("§7Your kagune is " + KaguneColours.name(pick.colour()) + " now ("
                + KaguneColours.hex(pick.colour()) + ")."), false);
            return 1;
        }))));
    }
}
