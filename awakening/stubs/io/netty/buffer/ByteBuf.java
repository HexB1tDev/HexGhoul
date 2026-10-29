package io.netty.buffer;

public abstract class ByteBuf {
    public abstract ByteBuf writeFloat(float value);
    public abstract ByteBuf writeDouble(double value);
    public abstract ByteBuf writeByte(int value);
    public abstract ByteBuf writeBoolean(boolean value);
    public abstract ByteBuf writeInt(int value);
    public abstract float readFloat();
    public abstract double readDouble();
    public abstract byte readByte();
    public abstract boolean readBoolean();
    public abstract int readInt();
}
