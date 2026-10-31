// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraftforge.event.entity;

public class EntityEvent extends net.minecraftforge.eventbus.api.Event {
    public net.minecraft.world.entity.Entity getEntity() { throw new UnsupportedOperationException(); }

    /** 1.3.9: the size an entity is about to take (Entity.refreshDimensions), changeable. */
    public static class Size extends EntityEvent {
        public net.minecraft.world.entity.EntityDimensions getNewSize() { throw new UnsupportedOperationException(); }
        public void setNewSize(net.minecraft.world.entity.EntityDimensions size, boolean updateEyeHeight) { throw new UnsupportedOperationException(); }
        public float getNewEyeHeight() { throw new UnsupportedOperationException(); }
        public void setNewEyeHeight(float eyeHeight) { throw new UnsupportedOperationException(); }
    }
}
