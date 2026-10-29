package net.minecraftforge.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraftforge.eventbus.api.Event;

public class RenderLevelStageEvent extends Event {
    public Stage getStage() { throw null; }
    public float getPartialTick() { throw null; }
    public PoseStack getPoseStack() { throw null; }

    public static class Stage {
        public static final Stage AFTER_ENTITIES = null;
    }
}
