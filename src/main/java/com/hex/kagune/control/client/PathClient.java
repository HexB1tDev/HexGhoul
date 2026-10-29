package com.hex.kagune.control.client;

import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.network.S2CPathState;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client store of every known player's kagune path, fed only by the server. Unknown players read as {@link KagunePath#NONE}. */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class PathClient {
    private static final Map<UUID, KagunePath> PATHS = new HashMap<>();

    private PathClient() {
    }

    public static void accept(S2CPathState message) {
        PATHS.put(message.playerId(), message.path());
    }

    public static KagunePath get(AbstractClientPlayer player) {
        return player == null ? KagunePath.NONE : PATHS.getOrDefault(player.m_36316_().getId(), KagunePath.NONE);
    }

    public static KagunePath self() {
        Minecraft minecraft = Minecraft.m_91087_();
        return minecraft.f_91074_ == null ? KagunePath.NONE : get(minecraft.f_91074_);
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        PATHS.clear();
    }
}
