package com.hex.kagune.control.network;

import com.hex.kagune.control.client.AbilityHud;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** {@code /kagune hud toggle} (3.1.0): the player's client turns its abilities board on or off (saved in its client config). */
public record S2CAbilityHud() {
    public static void encode(S2CAbilityHud message, FriendlyByteBuf buffer) {
    }

    public static S2CAbilityHud decode(FriendlyByteBuf buffer) {
        return new S2CAbilityHud();
    }

    public static void handle(S2CAbilityHud message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> AbilityHud::toggle));
        context.setPacketHandled(true);
    }
}
