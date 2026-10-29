// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraftforge.client.event;

public abstract class ViewportEvent extends net.minecraftforge.eventbus.api.Event {
    public double getPartialTick() { throw new UnsupportedOperationException(); }

    public static class ComputeCameraAngles extends ViewportEvent {
        public float getYaw() { throw new UnsupportedOperationException(); }
        public void setYaw(float yaw) { throw new UnsupportedOperationException(); }
        public float getPitch() { throw new UnsupportedOperationException(); }
        public void setPitch(float pitch) { throw new UnsupportedOperationException(); }
        public float getRoll() { throw new UnsupportedOperationException(); }
        public void setRoll(float roll) { throw new UnsupportedOperationException(); }
    }
}
