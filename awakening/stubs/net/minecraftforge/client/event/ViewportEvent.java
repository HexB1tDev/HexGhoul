package net.minecraftforge.client.event;

import net.minecraftforge.eventbus.api.Event;

public class ViewportEvent extends Event {
    public static class ComputeCameraAngles extends ViewportEvent {
        public float getYaw() { throw null; }
        public void setYaw(float yaw) { throw null; }
        public float getPitch() { throw null; }
        public void setPitch(float pitch) { throw null; }
        public float getRoll() { throw null; }
        public void setRoll(float roll) { throw null; }
    }

    public static class ComputeFov extends ViewportEvent {
        public double getFOV() { throw null; }
        public void setFOV(double fov) { throw null; }
    }
}
