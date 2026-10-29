package net.minecraftforge.client.event;

import net.minecraftforge.eventbus.api.Event;

public class InputEvent extends Event {
    public static class InteractionKeyMappingTriggered extends InputEvent {
        public void setSwingHand(boolean value) { throw null; }
        public boolean isAttack() { throw null; }
    }
}
