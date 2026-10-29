// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraftforge.client.event;

public abstract class RenderLivingEvent<T extends net.minecraft.world.entity.LivingEntity, M> extends net.minecraftforge.eventbus.api.Event {
    public T getEntity() { throw new UnsupportedOperationException(); }
    public float getPartialTick() { throw new UnsupportedOperationException(); }
    public com.mojang.blaze3d.vertex.PoseStack getPoseStack() { throw new UnsupportedOperationException(); }
    public net.minecraft.client.renderer.entity.LivingEntityRenderer<T, ?> getRenderer() { throw new UnsupportedOperationException(); }
    public net.minecraft.client.renderer.MultiBufferSource getMultiBufferSource() { throw new UnsupportedOperationException(); }
    public int getPackedLight() { throw new UnsupportedOperationException(); }

    public static class Pre<T extends net.minecraft.world.entity.LivingEntity, M> extends RenderLivingEvent<T, M> {
    }

    public static class Post<T extends net.minecraft.world.entity.LivingEntity, M> extends RenderLivingEvent<T, M> {
    }
}
