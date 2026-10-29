// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraftforge.client.event;

public class InputEvent extends net.minecraftforge.eventbus.api.Event {
    public static class Key extends InputEvent {
        public int getKey() { throw new UnsupportedOperationException(); }
        public int getScanCode() { throw new UnsupportedOperationException(); }
        public int getAction() { throw new UnsupportedOperationException(); }
    }
    public static class InteractionKeyMappingTriggered extends InputEvent {
        public boolean isAttack() { throw new UnsupportedOperationException(); }
        public boolean isUseItem() { throw new UnsupportedOperationException(); }
        public net.minecraft.world.InteractionHand getHand() { throw new UnsupportedOperationException(); }
        public void setSwingHand(boolean swing) { throw new UnsupportedOperationException(); }
        public void setCanceled(boolean cancel) { throw new UnsupportedOperationException(); }
    }
}
