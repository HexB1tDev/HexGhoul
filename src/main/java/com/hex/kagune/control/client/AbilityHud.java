package com.hex.kagune.control.client;

import com.hex.kagune.client.KaguneClientState;
import com.hex.kagune.client.KaguneKeys;
import com.hex.kagune.control.AbilityGuide;
import com.hex.kagune.control.ControlConfig;
import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.ukaku.WingForm;
import com.hex.kagune.control.client.ui.GuiCanvas;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.KeyMapping;
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
 * The abilities panel above the chat (3.1.0): a ghoul's moves with the keys the player bound in Controls ({@link AbilityGuide},
 * drawn by {@link AbilityHudView}). Cheap by design: the keys are read twice a second, the panel is rebuilt and laid out only
 * when something it shows changes (path, form, mode, mastery, a rebound key), and a frame is a few fills and lines of text.
 * Hidden while any screen is open (chat included, so it never jumps when the chat grows), for humans, and when switched off in
 * the client config ({@code abilityHud}).
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class AbilityHud {
    /** The panel's size on screen (GUI pixels per panel pixel). */
    static final float SCALE = 0.75F;
    private static final long KEY_NANOS = 500_000_000L;
    private static Map<AbilityGuide.Key, String> keys = Map.of();
    private static long keysAt = Long.MIN_VALUE;
    private static AbilityGuide.Input shown;
    private static AbilityHudView.Layout layout;

    private AbilityHud() {
    }

    @SubscribeEvent
    public static void render(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().equals(VanillaGuiOverlay.CHAT_PANEL.type())) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        LocalPlayer player = minecraft.f_91074_;
        if (player == null || minecraft.f_91080_ != null || !ControlConfig.abilityHud() || KingHud.showing()) {
            return; // (1.0.6: or War has the body: its own board shows)
        }
        KaguneClientState.View view = KaguneClientState.self();
        if (!view.unlocked()) {
            return;
        }
        long now = System.nanoTime();
        if (now - keysAt > KEY_NANOS || keysAt == Long.MIN_VALUE) {
            keys = keys(minecraft);
            keysAt = now;
        }
        KagunePath path = PathClient.self();
        AbilityGuide.State state = view.active() ? AbilityGuide.State.OUT
            : KakuganClient.ghoul(player) ? AbilityGuide.State.EYES : AbilityGuide.State.CLOSED;
        WingForm wings = path == KagunePath.UKAKU ? UkakuClient.get(player).form().shown(view.mastery()) : WingForm.TWIN;
        boolean crystal = path == KagunePath.UKAKU && UkakuClient.crystalMode(player);
        boolean blade = path == KagunePath.UKAKU && UkakuClient.blade(player, ControlClient.worldTime(0.0F)) > 0.5;
        GraftClient.View graft = GraftClient.get(player);
        java.util.Set<com.hex.kagune.control.quest.Gate> shut = java.util.EnumSet.noneOf(com.hex.kagune.control.quest.Gate.class);
        for (com.hex.kagune.control.quest.Gate gate : com.hex.kagune.control.quest.Gate.values()) {
            if (!QuestClient.open(gate)) {
                shut.add(gate);
            }
        }
        AbilityGuide.Input input = new AbilityGuide.Input(path, state, view.mastery(), view.form(), wings, crystal, keys, blade,
            graft.stage(), path == KagunePath.GRAFT && graft.kakuja(), shut);
        GuiCanvas canvas = new GuiCanvas(event.getGuiGraphics(), minecraft.f_91062_, null);
        if (!input.equals(shown) || layout == null) {
            layout = AbilityHudView.layout(canvas, AbilityGuide.panel(input));
            shown = input;
        }
        // Just above where the chat's lines go (vanilla draws them up from 40 above the bottom of the screen).
        ChatComponent chat = minecraft.f_91065_.m_93076_();
        int chatTop = event.getWindow().m_85446_() - 40 - (int)Math.ceil(chat.m_93814_() * chat.m_93815_());
        AbilityHudView.draw(canvas, layout, 2, chatTop - 4, SCALE);
    }

    /** The player's name for each key the panel names, as bound in Controls (Forge's modifier included). */
    static Map<AbilityGuide.Key, String> keys(Minecraft minecraft) {
        Map<AbilityGuide.Key, String> out = new EnumMap<>(AbilityGuide.Key.class);
        out.put(AbilityGuide.Key.ATTACK, name(minecraft.f_91066_.f_92096_));
        out.put(AbilityGuide.Key.QUESTS, name(ControlKeys.QUESTS));
        out.put(AbilityGuide.Key.USE, name(minecraft.f_91066_.f_92095_));
        out.put(AbilityGuide.Key.TOGGLE, name(KaguneKeys.TOGGLE));
        out.put(AbilityGuide.Key.MENU, name(KaguneKeys.MENU));
        out.put(AbilityGuide.Key.GRAB, name(KaguneKeys.GRAB));
        out.put(AbilityGuide.Key.THROW, name(KaguneKeys.THROW));
        out.put(AbilityGuide.Key.RAPID, name(KaguneKeys.RAPID_STRIKE));
        out.put(AbilityGuide.Key.SLINGSHOT, name(KaguneKeys.SLINGSHOT));
        out.put(AbilityGuide.Key.CYCLE, name(KaguneKeys.CYCLE_TENDRILS));
        out.put(AbilityGuide.Key.CRUSH, name(KaguneKeys.CRUSH));
        out.put(AbilityGuide.Key.LIFT, name(KaguneKeys.LIFT));
        out.put(AbilityGuide.Key.SLAM, name(KaguneKeys.SLAM));
        out.put(AbilityGuide.Key.GUARD, name(KaguneKeys.GUARD));
        out.put(AbilityGuide.Key.UNLEASH, name(ControlKeys.UNLEASH));
        return out;
    }

    static String name(KeyMapping key) {
        if (key == null) {
            return "-";
        }
        Enum<?> modifier = key.getKeyModifier();
        return AbilityGuide.keyName(key.m_90865_(), modifier == null ? "NONE" : modifier.name());
    }

    /** {@code /kagune hud toggle} reached this client. */
    public static void toggle() {
        ControlConfig.toggleAbilityHud();
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        shown = null;
        layout = null;
        keysAt = Long.MIN_VALUE;
    }
}
