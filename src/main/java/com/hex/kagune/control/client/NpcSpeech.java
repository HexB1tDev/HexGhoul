package com.hex.kagune.control.client;

import com.hex.kagune.control.npc.Speech;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 1.3.30: what each NPC near this player last said ({@code S2CNpcSay}), up in a bubble over its head for as long as it takes to read
 * ({@link Speech#ticks}): wrapped to a few lines, an action grey and slanted; the newest words take the bubble's place.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class NpcSpeech {
    /** One bubble: its lines (styled), the game time it comes down, and (1.3.38) the game time it went up. */
    record Bubble(List<String> lines, long until, long from) {
    }

    private static final Map<Integer, Bubble> BUBBLES = new HashMap<>();

    private NpcSpeech() {
    }

    private static long now() {
        Minecraft minecraft = Minecraft.m_91087_();
        return minecraft.f_91073_ == null ? 0L : minecraft.f_91073_.m_46467_();
    }

    /** {@code npc} (an entity id) says {@code text}: up over its head from now. */
    public static void accept(int npc, String text) {
        accept(npc, text, 0);
    }

    /**
     * {@code npc} says {@code text}: up over its head from now, for {@code ticks} (1.3.52: a line in a talk, until the answer comes; 0, or
     * longer than it takes to read: as long as it takes to read).
     */
    public static void accept(int npc, String text, int ticks) {
        if (text == null || text.isBlank()) {
            return;
        }
        boolean action = Speech.action(text);
        List<String> lines = new ArrayList<>();
        for (String line : Speech.wrap(text)) {
            lines.add(Speech.styled(line, action));
        }
        BUBBLES.put(npc, new Bubble(List.copyOf(lines), now() + Speech.life(text, ticks), now()));
    }

    /**
     * 1.3.31: the bubble over {@code body} as this player sees it: none through a wall (only what it says in their sight is shown, as only
     * that is sent).
     */
    public static List<String> lines(net.minecraft.world.entity.LivingEntity body) {
        Minecraft minecraft = Minecraft.m_91087_();
        List<String> lines = lines(body.m_19879_());
        if (lines.isEmpty() || minecraft.f_91074_ == null || !body.m_142582_(minecraft.f_91074_)) {
            return List.of();
        }
        return lines;
    }

    /** The bubble's lines over {@code npc} now, top first (empty: nothing said lately). */
    public static List<String> lines(int npc) {
        Bubble bubble = BUBBLES.get(npc);
        if (bubble == null) {
            return List.of();
        }
        if (now() >= bubble.until() || now() < bubble.until() - Speech.MOST - 20L) {
            BUBBLES.remove(npc);
            return List.of();
        }
        return bubble.lines();
    }

    /** 1.3.38: how long ago (ticks, {@code partial} in) the bubble over {@code npc} went up (huge if there is none). */
    public static double age(int npc, float partial) {
        Bubble bubble = BUBBLES.get(npc);
        return bubble == null ? 1.0E9 : now() - bubble.from() + partial;
    }

    /** 1.3.52: how long until the bubble over {@code npc} comes down (ticks, {@code partial} in; huge if there is none: it is not going). */
    public static double left(int npc, float partial) {
        Bubble bubble = BUBBLES.get(npc);
        return bubble == null ? 1.0E9 : bubble.until() - now() - partial;
    }

    /** 1.3.52: everyone with a bubble up now (entity ids). */
    public static java.util.Set<Integer> speaking() {
        return java.util.Set.copyOf(BUBBLES.keySet());
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        BUBBLES.clear();
    }
}
