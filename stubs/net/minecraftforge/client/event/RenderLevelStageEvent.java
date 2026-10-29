// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraftforge.client.event;

public class RenderLevelStageEvent extends net.minecraftforge.eventbus.api.Event {
    public Stage getStage() { throw new UnsupportedOperationException(); }
    public com.mojang.blaze3d.vertex.PoseStack getPoseStack() { throw new UnsupportedOperationException(); }
    public float getPartialTick() { throw new UnsupportedOperationException(); }

    public static class Stage {
        public static final Stage AFTER_ENTITIES = null;
    }
}
