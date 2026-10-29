package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import java.util.UUID;

public class FriendlyByteBuf extends ByteBuf {
    public FriendlyByteBuf writeUUID(UUID value) { throw null; }
    public UUID readUUID() { throw null; }
    public FriendlyByteBuf writeVarInt(int value) { throw null; }
    public int readVarInt() { throw null; }
    public FriendlyByteBuf writeEnum(Enum<?> value) { throw null; }
    public <T extends Enum<T>> T readEnum(Class<T> type) { throw null; }
    @Override public ByteBuf writeFloat(float value) { throw null; }
    @Override public ByteBuf writeDouble(double value) { throw null; }
    @Override public ByteBuf writeByte(int value) { throw null; }
    @Override public ByteBuf writeBoolean(boolean value) { throw null; }
    @Override public ByteBuf writeInt(int value) { throw null; }
    @Override public float readFloat() { throw null; }
    @Override public double readDouble() { throw null; }
    @Override public byte readByte() { throw null; }
    @Override public boolean readBoolean() { throw null; }
    @Override public int readInt() { throw null; }
}
