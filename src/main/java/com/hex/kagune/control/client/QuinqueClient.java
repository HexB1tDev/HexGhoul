package com.hex.kagune.control.client;

import com.hex.kagune.control.ControlItems;
import com.hex.kagune.control.quinque.Quinque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 1.3.25: the quinques drawn turning: each case's and weapon's model chooses its frame by "hexkagune:deploy" ({@link Quinque#shown}):
 * the case shut, splitting open on its red core, the weapon half out, nearly out, out; a case shutting runs it backwards. The stack's
 * {@link Quinque#AT} says when it turned (set by the server); with none it is still (a case shut, a weapon out).
 */
@Mod.EventBusSubscriber(modid = "hexkagune", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class QuinqueClient {
    private QuinqueClient() {
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        // On the game's thread: the property table is a plain map.
        event.enqueueWork(QuinqueClient::register);
    }

    static void register() {
        ResourceLocation id = ResourceLocation.m_214293_("hexkagune", Quinque.PROPERTY);
        for (Quinque quinque : Quinque.values()) {
            ItemProperties.m_174570_(ControlItems.caseOf(quinque), id, (stack, level, entity, seed) -> shown(stack, false));
            ItemProperties.m_174570_(ControlItems.weaponOf(quinque), id, (stack, level, entity, seed) -> shown(stack, true));
        }
    }

    /** 1.3.25: what a quinque is and how it is used, under its name (on the game bus). */
    @Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
    public static final class Tooltips {
        private Tooltips() {
        }

        @SubscribeEvent
        public static void tooltip(net.minecraftforge.event.entity.player.ItemTooltipEvent event) {
            ItemStack stack = event.getItemStack();
            Quinque shut = ControlItems.quinqueCase(stack);
            Quinque open = shut == null ? ControlItems.quinqueWeapon(stack) : null;
            if (shut == null && open == null) {
                return;
            }
            Quinque quinque = shut != null ? shut : open;
            java.util.List<Component> lines = event.getToolTip();
            lines.add(Component.m_237115_("item.hexkagune.quinque." + quinque.model + ".lore"));
            lines.add(Component.m_237115_("item.hexkagune.quinque.ghoul"));
            if (!quinque.ranged) {
                lines.add(Component.m_237115_("item.hexkagune.quinque.combo"));
            }
            lines.add(Component.m_237115_(shut != null ? "item.hexkagune.quinque.open" : quinque.ranged ? "item.hexkagune.quinque.fire"
                : "item.hexkagune.quinque.close"));
        }
    }

    // ---------------------------------------------------------------- 1.3.26: the body holding and swinging it

    /** Each body's quinque swings as this client sees them: its last swing's start (client clock), which of the three, its last shot. */
    static final class Swing {
        boolean wasSwinging;
        int lastSwingTime = Integer.MAX_VALUE;
        double began = -1.0E9;
        int step = -1;
        double shot = -1.0E9;
    }

    private static final java.util.Map<Integer, Swing> SWINGS = new java.util.HashMap<>();

    /** True while {@code player}'s body is a quinque's: a weapon out, or a case just shut (the arms coming down). */
    public static boolean posing(net.minecraft.world.entity.LivingEntity player) {
        ItemStack held = player.m_21205_();
        if (ControlItems.quinqueWeapon(held) != null) {
            return true;
        }
        Quinque shut = ControlItems.quinqueCase(held);
        return shut != null && shown(held, false) > 0.0F;
    }

    /**
     * {@code body}'s pose this frame for the quinque in its hand (null: none of its own): a swing under way (its three M1s in turn), or
     * its stance (rising into it as the case opens, falling out of it as it shuts); the launcher's kick as a shard leaves.
     */
    public static com.hex.kagune.control.client.animation.RigPose pose(net.minecraft.world.entity.LivingEntity body, float partial) {
        ItemStack held = body.m_21205_();
        double clock = PaleClient.clock(partial);
        Swing swing = SWINGS.computeIfAbsent(body.m_19879_(), id -> new Swing());
        if (SWINGS.size() > 512) {
            SWINGS.clear();
        }
        boolean swinging = body.f_20911_;
        if (swinging && (!swing.wasSwinging || body.f_20913_ < swing.lastSwingTime)) {
            swing.step = Quinque.next(swing.step, (long)(clock - swing.began));
            swing.began = clock;
        }
        swing.wasSwinging = swinging;
        swing.lastSwingTime = body.f_20913_;
        Quinque weapon = ControlItems.quinqueWeapon(held);
        if (weapon == null) {
            Quinque shut = ControlItems.quinqueCase(held);
            float out = shut == null ? 0.0F : shown(held, false);
            return out > 0.0F ? com.hex.kagune.control.client.animation.QuinquePose.unfolding(shut, out) : null;
        }
        com.hex.kagune.control.client.animation.RigPose p;
        double t = (clock - swing.began) / weapon.swing;
        if (t >= 0.0 && t < 1.0) {
            p = com.hex.kagune.control.client.animation.QuinquePose.swing(weapon, swing.step, t);
        } else {
            float out = shown(held, true);
            p = out < 1.0F ? com.hex.kagune.control.client.animation.QuinquePose.unfolding(weapon, out)
                : com.hex.kagune.control.client.animation.QuinquePose.hold(weapon);
        }
        if (p != null && weapon.ranged) {
            com.hex.kagune.control.client.animation.QuinquePose.recoil(p, (float)Math.max(0.0, 1.0 - (clock - swing.shot) / 5.0));
        }
        return p;
    }

    /** 1.3.26: the launcher fired in this player's own hand (the server fires it): their arms kick. */
    @Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
    public static final class Recoil {
        private Recoil() {
        }

        @SubscribeEvent
        public static void use(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem event) {
            net.minecraft.world.entity.player.Player player = event.getEntity();
            if (player.m_9236_() == null || !player.m_9236_().f_46443_) {
                return;
            }
            Quinque weapon = ControlItems.quinqueWeapon(player.m_21205_());
            if (weapon != null && weapon.ranged && !player.m_6047_()) {
                SWINGS.computeIfAbsent(player.m_19879_(), id -> new Swing()).shot = PaleClient.clock(0.0F);
            }
        }
    }

    /** How far out {@code stack} is drawn now. */
    static float shown(ItemStack stack, boolean weapon) {
        try {
            CompoundTag tag = stack.m_41783_();
            Minecraft minecraft = Minecraft.m_91087_();
            if (tag == null || !tag.m_128441_(Quinque.AT) || minecraft.f_91073_ == null) {
                return weapon ? 1.0F : 0.0F;
            }
            return Quinque.shown(weapon, minecraft.f_91073_.m_46467_(), tag.m_128454_(Quinque.AT));
        } catch (RuntimeException failure) {
            return weapon ? 1.0F : 0.0F;
        }
    }
}
