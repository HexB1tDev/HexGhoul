package com.hex.kagune.control.client;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.control.kakugan.KakuganEyes;
import com.hex.kagune.control.kakugan.KakuganSkin;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Reads players' skins back from the graphics card, once per skin, for what the kakugan needs ({@link KakuganSkin}): which
 * face pixels the skin's second layer covers, and where its eyes are. Done on the client tick (the render thread), a couple of
 * skins a tick; a skin that is not a 64-pixel multiple (or not there), or that comes back unreadable (graphics drivers that cannot
 * read textures back, as OpenGL ES translation layers on phones), reads as nothing covered and no eyes found.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class SkinReader {
    /** What a skin says: the face cells its second layer covers (and which of those are glass), and the eyes found on it (or null). */
    public record Skin(long covered, long glass, KakuganEyes eyes) {
    }

    static final Skin NOTHING = new Skin(0L, 0L, null);
    private static final int TEXTURE_2D = 3553;
    private static final int TEXTURE_WIDTH = 4096;
    private static final int TEXTURE_HEIGHT = 4097;
    private static final int READS_PER_TICK = 2;
    private static final Map<ResourceLocation, Skin> SKINS = new HashMap<>();
    private static boolean unreadableReported;

    private SkinReader() {
    }

    /** The player's skin as read, or null until it has been. */
    public static Skin of(AbstractClientPlayer player) {
        return player == null ? null : SKINS.get(player.m_108560_());
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        ClientLevel level = Minecraft.m_91087_().f_91073_;
        if (level == null) {
            return;
        }
        int reads = 0;
        for (AbstractClientPlayer player : level.m_6907_()) {
            ResourceLocation skin = player.m_108560_();
            if (skin == null || SKINS.containsKey(skin)) {
                continue;
            }
            SKINS.put(skin, read(skin));
            if (++reads >= READS_PER_TICK) {
                return;
            }
        }
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        SKINS.clear();
    }

    /** One skin from the graphics card, scaled to the 64-pixel layout. */
    static Skin read(ResourceLocation location) {
        try {
            AbstractTexture texture = Minecraft.m_91087_().m_91097_().m_118506_(location);
            if (texture == null) {
                return NOTHING;
            }
            RenderSystem.bindTexture(texture.m_117963_());
            int width = GlStateManager._getTexLevelParameter(TEXTURE_2D, 0, TEXTURE_WIDTH);
            int height = GlStateManager._getTexLevelParameter(TEXTURE_2D, 0, TEXTURE_HEIGHT);
            // Only a square skin of 64-pixel multiples is read: the buffer must match the texture exactly.
            if (width < 64 || width != height || width % 64 != 0 || width > 2048) {
                return NOTHING;
            }
            int scale = width / 64;
            int[] pixels = new int[64 * 64];
            // Zeroed (calloc), so a driver that cannot read textures back (OpenGL ES translation on phones) leaves it blank, not garbage.
            try (NativeImage image = new NativeImage(width, height, true)) {
                image.m_85045_(0, false);
                for (int v = 0; v < 64; v++) {
                    for (int u = 0; u < 64; u++) {
                        int abgr = image.m_84985_(u * scale, v * scale);
                        pixels[v * 64 + u] = abgr & 0xFF00FF00 | (abgr & 0xFF) << 16 | (abgr >> 16) & 0xFF;
                    }
                }
            }
            if (!KakuganSkin.readable(pixels, 64)) {
                if (!unreadableReported) {
                    unreadableReported = true;
                    KaguneMod.LOGGER.info("HexKagune cannot read skins back on this renderer (" + location + " came back blank or scrambled); "
                        + "the kakugan uses the eyes placed on the eye screen, or the default ones");
                }
                return NOTHING;
            }
            return new Skin(KakuganSkin.covered(pixels, 64), KakuganSkin.glass(pixels, 64), KakuganSkin.detect(pixels, 64));
        } catch (RuntimeException failure) {
            KaguneMod.LOGGER.info("HexKagune could not read skin " + location + " for the kakugan: " + failure);
            return NOTHING;
        }
    }
}
