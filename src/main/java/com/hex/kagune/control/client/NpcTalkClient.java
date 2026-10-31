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
        String texture = message.skin() == -1 ? "textures/entity/eto/eto.png" : NpcLook.texture(message.skin());
        ResourceLocation skin = Objects.requireNonNull(ResourceLocation.m_214293_("hexkagune", texture));
        minecraft.m_91152_(new NpcTalkScreen(message.npc(), skin, model));
    }
}
