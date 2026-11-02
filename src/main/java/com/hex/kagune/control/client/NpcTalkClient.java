package com.hex.kagune.control.client;

import com.hex.kagune.control.network.S2CNpcTalk;
import com.hex.kagune.control.npc.NpcLook;
import com.hex.kagune.control.npc.NpcTalk;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/** 7.0.2: a page of a conversation with a ghoul NPC (1.3.0: or Eto), from the server: the talk screen opened on it, or turned to it. */
public final class NpcTalkClient {
    private NpcTalkClient() {
    }

    public static void accept(S2CNpcTalk message) {
        Minecraft minecraft = Minecraft.m_91087_();
        // 1.3.77: a page with nothing to answer ends the talk (a villager walking up for a kiss or a hug, to be seen in the world).
        if (message.choices().length == 0) {
            if (minecraft.f_91080_ instanceof NpcTalkScreen screen && screen.npc == message.npc()) {
                minecraft.m_91152_(null);
            }
            return;
        }
        List<NpcTalk.Choice> choices = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < message.choices().length; i++) {
            NpcTalk.Choice choice = NpcTalk.Choice.byOrdinal(message.choices()[i]);
            choices.add(choice);
            String label = message.label(i);
            labels.add(label.isEmpty() ? choice.text() : label);
        }
        NpcTalkView.Model model = new NpcTalkView.Model(message.name(), message.temper(), message.regard(), message.ally(), message.line(), choices,
            labels);
        if (minecraft.f_91080_ instanceof NpcTalkScreen screen && screen.npc == message.npc()) {
            screen.page(model);
            return;
        }
        // 1.3.0: skin -1 is Eto's own (128 pixels: the face is drawn by its share of the texture, so it fits).
        // 1.3.14: -1, -2, -3...: each companion's own, in Persona's order (Eto, Touka, Ken).
        // 2.0.1: CHILD_FACE is a child's: the skin it is drawn in (ChildRenderer), not Eto's (-1 had shown hers on every child).
        if (message.skin() == S2CNpcTalk.CHILD_FACE) {
            minecraft.m_91152_(new NpcTalkScreen(message.npc(), com.hex.kagune.control.client.render.ChildRenderer.textureOf(message.npc()), model));
            return;
        }
        if (message.skin() == S2CNpcTalk.HUMAN_FACE) {
            ResourceLocation face = minecraft.f_91073_ != null && minecraft.f_91073_.m_6815_(message.npc()) instanceof com.hex.kagune.control.npc.HumanNpc human
                ? com.hex.kagune.control.client.render.HumanRenderer.textureOf(human) : STEVE;
            minecraft.m_91152_(new NpcTalkScreen(message.npc(), face, model));
            return;
        }
        String texture = message.skin() < 0 ? companion(-message.skin() - 1) : NpcLook.texture(message.skin());
        ResourceLocation skin = Objects.requireNonNull(ResourceLocation.m_214293_("hexkagune", texture));
        minecraft.m_91152_(new NpcTalkScreen(message.npc(), skin, model));
    }

    private static final ResourceLocation STEVE = Objects.requireNonNull(ResourceLocation.m_214293_("minecraft", "textures/entity/player/wide/steve.png"));

    /** 1.3.14: the companion {@code index}'s skin (Persona's order). */
    static String companion(int index) {
        java.util.List<com.hex.kagune.control.npc.Persona> all = com.hex.kagune.control.npc.Persona.ALL;
        String key = all.get(Math.max(0, Math.min(all.size() - 1, index))).key();
        return "textures/entity/" + key + "/" + key + ".png";
    }
}
