package com.hex.kagune.control.client;

import com.hex.kagune.client.KaguneClientState;
import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.bikaku.BikakuRules;
import com.hex.kagune.control.bikaku.BikakuRules.Bite;
import com.hex.kagune.control.network.S2CBikakuState;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 3.4.9: the client's store of every Bikaku player's worm (fed only by the server, {@code S2CBikakuState}): the bite it is making and when
 * it began, where it is thrown and at whom. The renderer animates from when each bite began on the client's own clock
 * ({@link PaleClient#clock}: never set back, so a lagging server never throws a bite back a few ticks).
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class BikakuClient {
    /** A bite this few ticks old when it gets here is played from its first frame (it was sent as it began). */
    static final long JUST_BEGUN = 4L;

    /** One player's worm as the server last said, with when (on the client clock) its bite began here. */
    public record View(Bite bite, long startedAt, double began, double x, double y, double z, int targetId) {
        public static final View NONE = new View(Bite.NONE, Long.MIN_VALUE, 0.0, 0.0, 0.0, 0.0, 0);

        /** Ticks since the bite began ({@code clock}: {@link PaleClient#clock}); MAX_VALUE for none. */
        public double age(double clock) {
            return this.bite == Bite.NONE || this.startedAt == Long.MIN_VALUE ? Double.MAX_VALUE : Math.max(0.0, clock - this.began);
        }
    }

    private static final Map<UUID, View> VIEWS = new HashMap<>();

    private BikakuClient() {
    }

    public static void accept(S2CBikakuState message) {
        Minecraft minecraft = Minecraft.m_91087_();
        long world = minecraft.f_91073_ == null ? 0L : minecraft.f_91073_.m_46467_();
        double clock = PaleClient.clock(0.0F);
        View before = VIEWS.get(message.playerId());
        long behind = message.startedAt() == Long.MIN_VALUE ? 0L : world - message.startedAt();
        double began = clock - (behind > JUST_BEGUN ? behind : 0L);
        if (before != null && before.startedAt() == message.startedAt() && before.bite().ordinal() == message.bite()) {
            began = before.began(); // the same bite, told again: its clock runs on
        }
        VIEWS.put(message.playerId(), new View(Bite.byOrdinal(message.bite()), message.startedAt(), began, message.x(), message.y(), message.z(),
            message.targetId()));
    }

    public static View get(Player player) {
        return player == null ? View.NONE : VIEWS.getOrDefault(player.m_36316_().getId(), View.NONE);
    }

    public static boolean isBikaku(Player player) {
        return player instanceof AbstractClientPlayer client && PathClient.get(client) == KagunePath.BIKAKU;
    }

    /** 0..1: how far the kagune is out (the original's manifest and dismiss timing; 0 put away). */
    public static float deploy(Player player, float partial) {
        return UkakuClient.deploy(KaguneClientState.get(player), ControlClient.worldTime(partial));
    }

    /** The worm's seed for {@code player}: the same on every client. */
    public static int seed(Player player) {
        UUID id = player.m_36316_().getId();
        return BikakuRules.seed(id.getMostSignificantBits(), id.getLeastSignificantBits());
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        VIEWS.clear();
    }
}
