// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / Photon 1.1.17). Never packaged; see verify/api-allowlist.txt.
package com.lowdragmc.photon.client.gameobject.particle;

import org.joml.Vector3f;
import org.joml.Vector4f;

public class TileParticle implements IParticle {
    protected float localX, localY, localZ;
    protected float velocityX, velocityY, velocityZ;
    protected int age;
    protected int lifetime;
    public net.minecraft.util.RandomSource randomSource;

    public TileParticle(com.lowdragmc.photon.client.gameobject.emitter.IParticleEmitter emitter,
                        com.lowdragmc.photon.client.gameobject.emitter.particle.ParticleConfig config, net.minecraft.util.RandomSource randomSource) {
        throw new UnsupportedOperationException();
    }

    public void setLocalPos(Vector3f position, boolean origin) { throw new UnsupportedOperationException(); }
    public void setInternalVelocity(Vector3f velocity) { throw new UnsupportedOperationException(); }
    public void setSize(Vector3f size) { throw new UnsupportedOperationException(); }
    public void setARGBColor(int color) { throw new UnsupportedOperationException(); }
    public void setColor(Vector4f color) { throw new UnsupportedOperationException(); }
    public void setLifetime(int lifetime) { throw new UnsupportedOperationException(); }
    public void setRemoved(boolean removed) { throw new UnsupportedOperationException(); }
    protected void update() { throw new UnsupportedOperationException(); }
    public org.joml.Vector3f getWorldPos(float partialTicks) { throw new UnsupportedOperationException(); }
    public int getRealLight(float partialTicks) { throw new UnsupportedOperationException(); }
    public void renderInternal(com.mojang.blaze3d.vertex.VertexConsumer buffer, net.minecraft.client.Camera camera, float partialTicks) { throw new UnsupportedOperationException(); }
}
