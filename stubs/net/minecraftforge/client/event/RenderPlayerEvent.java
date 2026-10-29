// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraftforge.client.event;

public abstract class RenderPlayerEvent extends net.minecraftforge.event.entity.player.PlayerEvent {
    public net.minecraft.client.renderer.entity.player.PlayerRenderer getRenderer() { throw new UnsupportedOperationException(); }
    public float getPartialTick() { throw new UnsupportedOperationException(); }
    public com.mojang.blaze3d.vertex.PoseStack getPoseStack() { throw new UnsupportedOperationException(); }
    public net.minecraft.client.renderer.MultiBufferSource getMultiBufferSource() { throw new UnsupportedOperationException(); }
    public int getPackedLight() { throw new UnsupportedOperationException(); }

    public static class Pre extends RenderPlayerEvent {
        public void setCanceled(boolean cancel) { throw new UnsupportedOperationException(); }
    }

    public static class Post extends RenderPlayerEvent {
    }
}
