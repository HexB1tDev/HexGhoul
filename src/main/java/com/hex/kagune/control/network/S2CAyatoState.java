package com.hex.kagune.control.network;

import com.hex.kagune.control.ayato.WingForm;
import com.hex.kagune.control.client.AyatoClient;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * An Ayato player's kagune state for renderers: wing form, the last Wing Vortex (which wing was thrown, the tick
 * it left and the tick a new one starts to grow back), and the crystal mode (on or off, and the tick it last changed, which
 * times the crystal climbing the wings), the Gatling (firing or not, and the tick that last changed) and his unlocked
 * mutations (a bitmask of {@code AyatoMutation}: they change how some moves look). Sent to the player and everyone tracking
 * them. Purely visual.
 */
public record S2CAyatoState(UUID playerId, WingForm form, int vortexSide, long vortexAt, long vortexRegrowAt,
                            boolean crystalMode, long crystalChangedAt, boolean gatling, long gatlingChangedAt, int mutations, boolean blade,
                            long bladeChangedAt) {
    public static void encode(S2CAyatoState message, FriendlyByteBuf buffer) {
        buffer.m_130077_(message.playerId);
        buffer.writeByte(message.form.ordinal());
        buffer.writeByte(Integer.signum(message.vortexSide));
        buffer.m_130103_(message.vortexAt);
        buffer.m_130103_(message.vortexRegrowAt);
        buffer.writeBoolean(message.crystalMode);
        buffer.m_130103_(message.crystalChangedAt);
        buffer.writeBoolean(message.gatling);
        buffer.m_130103_(message.gatlingChangedAt);
        buffer.writeByte(message.mutations & 0xFF);
        buffer.writeBoolean(message.blade);
        buffer.m_130103_(message.bladeChangedAt);
    }

    public static S2CAyatoState decode(FriendlyByteBuf buffer) {
        return new S2CAyatoState(buffer.m_130259_(), WingForm.byOrdinal(buffer.readByte()), buffer.readByte(),
            buffer.m_130258_(), buffer.m_130258_(), buffer.readBoolean(), buffer.m_130258_(), buffer.readBoolean(), buffer.m_130258_(),
            buffer.readByte() & 0xFF, buffer.readBoolean(), buffer.m_130258_());
    }

    public static void handle(S2CAyatoState message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getSender() == null) { // only ever accepted from the server
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> AyatoClient.accept(message)));
        }
        context.setPacketHandled(true);
    }
}
