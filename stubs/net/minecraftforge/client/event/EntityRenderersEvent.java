// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraftforge.client.event;

public abstract class EntityRenderersEvent extends net.minecraftforge.eventbus.api.Event {
    public static class RegisterRenderers extends EntityRenderersEvent {
        public RegisterRenderers() {
        }

        public <T extends net.minecraft.world.entity.Entity> void registerEntityRenderer(net.minecraft.world.entity.EntityType<? extends T> type,
            net.minecraft.client.renderer.entity.EntityRendererProvider<T> provider) { throw new UnsupportedOperationException(); }
    }

    public static class AddLayers extends EntityRenderersEvent {
        public java.util.Set<String> getSkins() { throw new UnsupportedOperationException(); }

        public <R extends net.minecraft.client.renderer.entity.LivingEntityRenderer<? extends net.minecraft.world.entity.player.Player,
            ? extends net.minecraft.client.model.EntityModel<? extends net.minecraft.world.entity.player.Player>>> R getSkin(String skinName) {
            throw new UnsupportedOperationException();
        }
    }
}
