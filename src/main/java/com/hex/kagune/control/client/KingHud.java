package com.hex.kagune.control.client;

import com.hex.kagune.control.AbilityGuide;
import com.hex.kagune.control.ControlConfig;
import com.hex.kagune.control.client.ui.GuiCanvas;
import com.hex.kagune.control.king.KingForm;
import com.hex.kagune.control.king.PeaceRules;
import com.hex.kagune.control.king.WarRules;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 1.0.6: War's abilities board while War is worn (1.0.16: or Peace), where the kagune's board goes (which steps aside). 1.0.22: it
 * is that board's own card ({@link AbilityGuide#king}, {@link AbilityHudView}), with the cooldowns on it.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class KingHud {
    private static final long KEY_NANOS = 500_000_000L;
    private static Map<AbilityGuide.Key, String> keys = Map.of();
    private static long keysAt = Long.MIN_VALUE;
    private static Map<AbilityGuide.Key, String> shownKeys;
    private static boolean shownPeace;
    private static AbilityHudView.Layout layout;

    private KingHud() {
    }

    /** True while the abilities board is War's: the kagune's own board stays hidden. */
    public static boolean showing() {
        return KingClient.wearing(KingForm.WAR) || KingClient.wearing(KingForm.PEACE); // 1.0.16: and Peace
    }

    @SubscribeEvent
    public static void render(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().equals(VanillaGuiOverlay.CHAT_PANEL.type())) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        LocalPlayer player = minecraft.f_91074_;
        // 1.0.13: under someone's Black Sun, swallowed in darkness: black closing in from the edges.
        double dark = KingFx.darkness();
        if (player != null && dark > 0.01) {
            int w = event.getWindow().m_85445_();
            int h = event.getWindow().m_85446_();
            GuiCanvas shade = new GuiCanvas(event.getGuiGraphics(), minecraft.f_91062_, null);
            shade.fill(0, 0, w, h, (int)Math.round(130 * dark) << 24);
            for (int k = 1; k <= 6; k++) {
                int inset = (int)Math.round(Math.min(w, h) * 0.04 * k);
                int a = (int)Math.round(28 * dark) << 24;
                shade.fill(0, 0, w, inset, a);
                shade.fill(0, h - inset, w, h, a);
                shade.fill(0, inset, inset, h - inset, a);
                shade.fill(w - inset, inset, w, h - inset, a);
            }
        }
        if (player == null || minecraft.f_91080_ != null || !ControlConfig.abilityHud() || !showing()) {
            return;
        }
        // 1.0.22: the same card as every other form's, in the same place (AbilityHud), the cooldowns on it.
        long nanos = System.nanoTime();
        if (nanos - keysAt > KEY_NANOS || keysAt == Long.MIN_VALUE) {
            keys = AbilityHud.keys(minecraft);
            keysAt = nanos;
        }
        boolean peace = KingClient.wearing(KingForm.PEACE);
        GuiCanvas canvas = new GuiCanvas(event.getGuiGraphics(), minecraft.f_91062_, null);
        if (layout == null || peace != shownPeace || !keys.equals(shownKeys)) {
            layout = AbilityHudView.layout(canvas, AbilityGuide.king(keys, peace), true);
            shownPeace = peace;
            shownKeys = keys;
        }
        int rows = layout.panel().rows().size();
        double[] ready = new double[rows];
        long[] wait = new long[rows];
        java.util.Arrays.fill(ready, 1.0);
        double now = ControlClient.worldTime(0.0F);
        for (int i = 0; i < WarRules.Ability.values().length && AbilityGuide.KING_FIRST + i < rows; i++) {
            long left = Math.max(0L, KingClient.ready(i) - (long)now);
            int cooldown = peace ? PeaceRules.Ability.values()[i].cooldown() : WarRules.Ability.values()[i].cooldown();
            wait[AbilityGuide.KING_FIRST + i] = left;
            ready[AbilityGuide.KING_FIRST + i] = left == 0L ? 1.0 : 1.0 - Math.min(1.0, left / (double)cooldown);
        }
        ChatComponent chat = minecraft.f_91065_.m_93076_();
        int chatTop = event.getWindow().m_85446_() - 40 - (int)Math.ceil(chat.m_93814_() * chat.m_93815_());
        AbilityHudView.draw(canvas, layout, 2, chatTop - 4, AbilityHud.SCALE, ready, wait);
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        layout = null;
        keysAt = Long.MIN_VALUE;
    }
}
