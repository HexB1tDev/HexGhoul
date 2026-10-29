package net.minecraft.world.phys;

public abstract class HitResult {
    public Type getType() { throw null; }
    public Vec3 getLocation() { throw null; }

    public enum Type { MISS, BLOCK, ENTITY }
}
