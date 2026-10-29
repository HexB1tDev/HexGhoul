package com.hex.kagune.control.client;

import com.hex.kagune.control.network.S2CNpcTalk;
import com.hex.kagune.control.npc.NpcLook;
import com.hex.kagune.control.npc.NpcTalk;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/** 7.0.2: a page of a conversation with a ghoul NPC, from the server: the talk screen opened on it, or turned to it. */
public final class NpcTalkClient {
    private NpcTalkClient() {
    }

    public static void accept(S2CNpcTalk message) {
        Minecraft minecraft = Minecraft.m_91087_();
        List<NpcTalk.Choice> choices = new ArrayList<>();
        for (int ordinal : message.choices()) {
            choices.add(NpcTalk.Choice.byOrdinal(ordinal));
        }
        NpcTalkView.Model model = new NpcTalkView.Model(message.name(), message.temper(), message.regard(), message.ally(), message.line(), choices);
        if (minecraft.f_91080_ instanceof NpcTalkScreen screen && screen.npc == message.npc()) {
            screen.page(model);
            return;
        }
        ResourceLocation skin = Objects.requireNonNull(ResourceLocation.m_214293_("hexkagune", NpcLook.texture(message.skin())));
        minecraft.m_91152_(new NpcTalkScreen(message.npc(), skin, model));
    }
}
