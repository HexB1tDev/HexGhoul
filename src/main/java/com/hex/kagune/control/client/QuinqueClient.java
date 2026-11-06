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

    /**
     * 2.0.19 (the owner's crash on loading: "QuinqueClient tried to access private method ItemProperties.m_174570_"): the register is
     * private in vanilla and only public where Forge's access transformer has been applied to it, which their launcher did not do.
     * It is called through Forge's {@code ObfuscationReflectionHelper} instead (made accessible either way), and if even that fails
     * the quinques are only drawn without their turning frames: the game never stops for it.
     */
    static void register() {
        try {
            java.lang.reflect.Method put = net.minecraftforge.fml.util.ObfuscationReflectionHelper.findMethod(ItemProperties.class, "m_174570_",
                net.minecraft.world.item.Item.class, ResourceLocation.class, net.minecraft.client.renderer.item.ClampedItemPropertyFunction.class);
            ResourceLocation id = ResourceLocation.m_214293_("hexkagune", Quinque.PROPERTY);
            for (Quinque quinque : Quinque.values()) {
                net.minecraft.client.renderer.item.ClampedItemPropertyFunction caseFrame = (stack, level, entity, seed) -> caseFrame(stack, entity);
                net.minecraft.client.renderer.item.ClampedItemPropertyFunction weaponFrame = quinque == Quinque.DAWN
                    ? (stack, level, entity, seed) -> dawnFrame(stack, level) : (stack, level, entity, seed) -> shown(stack, true);
                put.invoke(null, ControlItems.caseOf(quinque), id, caseFrame);
                put.invoke(null, ControlItems.weaponOf(quinque), id, weaponFrame);
            }
            // 3.0.6: a kagune-made weapon's case (shut, or carried by an investigator).
            for (com.hex.kagune.control.quinque.Arm arm : com.hex.kagune.control.quinque.Arm.values()) {
                net.minecraft.client.renderer.item.ClampedItemPropertyFunction caseFrame = (stack, level, entity, seed) -> caseFrame(stack, entity);
                put.invoke(null, ControlItems.armCaseOf(arm), id, caseFrame);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            com.hex.kagune.control.Safety.caught(failure, "registering the quinques' turning frames");
        }
    }

    /**
     * 2.0.32: Dawn's frame: its "fire" frame (its point parted about the gathering ball, {@link Quinque#DAWN_FIRE_FRAME}) while its stack
     * says its point is open ({@link Quinque#tipOpen}), else as any weapon (kept below the fire frame's place: "full" is 0.95).
     */
    static float dawnFrame(ItemStack stack, net.minecraft.client.multiplayer.ClientLevel level) {
        long[] open = dawnOpen(stack, level);
        return open != null ? 1.0F : Math.min(0.96F, shown(stack, true));
    }

    /** 2.0.32: {when its point began to open, the game time now} while Dawn's point is open, else null. */
    static long[] dawnOpen(ItemStack stack, net.minecraft.client.multiplayer.ClientLevel level) {
        net.minecraft.nbt.CompoundTag tag = stack.m_41783_();
        if (tag == null || !tag.m_128441_(Quinque.DAWN_AT) || !tag.m_128441_(Quinque.DAWN_FIRES)) {
            return null;
        }
        net.minecraft.client.multiplayer.ClientLevel world = level != null ? level : net.minecraft.client.Minecraft.m_91087_().f_91073_;
        if (world == null) {
            return null;
        }
        long now = world.m_46467_();
        long at = tag.m_128454_(Quinque.DAWN_AT);
        return Quinque.tipOpen(now, at, tag.m_128454_(Quinque.DAWN_FIRES)) ? new long[] {at, now} : null;
    }

    /** 1.3.25: what a quinque is and how it is used, under its name (on the game bus). */
    @Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
    public static final class Tooltips {
        private Tooltips() {
        }

        @SubscribeEvent
        public static void tooltip(net.minecraftforge.event.entity.player.ItemTooltipEvent event) {
            ItemStack stack = event.getItemStack();
            com.hex.kagune.control.quinque.Arm armCase = ControlItems.armCase(stack);
            com.hex.kagune.control.quinque.Arm armOut = armCase == null ? ControlItems.armWeapon(stack) : null;
            if (armCase != null || armOut != null) {
                // 3.0.6: a kagune-made weapon: what it is, its string, its two moves, how it opens and shuts.
                com.hex.kagune.control.quinque.Arm arm = armCase != null ? armCase : armOut;
                java.util.List<Component> lines = event.getToolTip();
                if (ControlItems.masterwork(stack)) {
                    lines.add(Component.m_237115_("item.hexkagune.arm.masterwork"));
                }
                lines.add(Component.m_237115_("item.hexkagune.arm." + arm.key() + ".lore"));
                lines.add(Component.m_237115_("item.hexkagune.quinque.ghoul"));
                if (armCase != null) {
                    lines.add(Component.m_237115_("item.hexkagune.arm.open"));
                } else {
                    lines.add(Component.m_237115_("item.hexkagune.arm.string"));
                    lines.add(Component.m_237115_("item.hexkagune.arm." + arm.key() + ".moves"));
                    lines.add(Component.m_237115_("item.hexkagune.arm.put_away"));
                    lines.add(foldHint());
                }
                return;
            }
            Quinque shut = ControlItems.quinqueCase(stack);
            Quinque open = shut == null ? ControlItems.quinqueWeapon(stack) : null;
            if (shut == null && open == null) {
                return;
            }
            Quinque quinque = shut != null ? shut : open;
            java.util.List<Component> lines = event.getToolTip();
            if (open != null) {
                lines.add(foldHint());
            }
            lines.add(Component.m_237115_("item.hexkagune.quinque." + quinque.model + ".lore"));
            lines.add(Component.m_237115_("item.hexkagune.quinque.ghoul"));
            if (!quinque.ranged) {
                lines.add(Component.m_237115_("item.hexkagune.quinque.combo"));
            }
            lines.add(Component.m_237115_(shut != null ? "item.hexkagune.quinque.open" : quinque == Quinque.DAWN ? "item.hexkagune.quinque.dawn_fire"
                : quinque.ranged ? "item.hexkagune.quinque.fire" : "item.hexkagune.quinque.close"));
        }
    }

    private static Component foldHint() {
        return Component.m_237110_("item.hexkagune.quinque.fold", Component.m_237115_(ControlKeys.FOLD_QUINQUE.m_90865_()));
    }

    @Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
    public static final class FoldKey {
        private FoldKey() {
        }

        @SubscribeEvent
        public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
            if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) {
                return;
            }
            boolean pressed = false;
            while (ControlKeys.FOLD_QUINQUE.m_90859_()) {
                pressed = true;
            }
            Minecraft minecraft = Minecraft.m_91087_();
            if (!pressed || minecraft.f_91080_ != null || minecraft.f_91074_ == null) {
                return;
            }
            ItemStack held = minecraft.f_91074_.m_21205_();
            if (ControlItems.armWeapon(held) != null || ControlItems.quinqueWeapon(held) != null) {
                com.hex.kagune.control.network.ControlNetwork.CHANNEL.sendToServer(
                    new com.hex.kagune.control.network.C2SArmAction(com.hex.kagune.control.network.C2SArmAction.FOLD, 0));
            }
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
        if (ControlItems.quinqueWeapon(held) != null || ControlItems.armWeapon(held) != null) {
            return true; // 3.0.6: or a kagune-made weapon
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
        com.hex.kagune.control.quinque.Arm arm = ControlItems.armWeapon(held);
        if (arm != null) {
            return WeaponClient.pose(body, held, arm, partial); // 3.0.6: a kagune-made weapon, as an investigator holds and swings it
        }
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
            if (weapon == Quinque.DAWN) {
                // 2.0.31: its dark ribbons along the way its point goes.
                com.hex.kagune.control.client.fx.DawnTrails.swing(body, swing.step, swing.began, t, partial);
            }
        } else {
            float out = shown(held, true);
            long[] open = weapon == Quinque.DAWN ? dawnOpen(held, null) : null;
            p = open != null ? com.hex.kagune.control.client.animation.AgentPose.aimDawn(open[1] - open[0] + partial, body.m_146909_())
                : out < 1.0F ? com.hex.kagune.control.client.animation.QuinquePose.unfolding(weapon, out)
                : com.hex.kagune.control.client.animation.QuinquePose.hold(weapon); // 2.0.32: pointing Dawn as its ball gathers
        }
        if (p != null && weapon.ranged) {
            // 1.3.33: the gun along the look (up or down with the head) unless it is clubbing; a kick for each shard of the pair a
            // pull fires (QuinqueServer.fire: one now, one two ticks on).
            if (!(t >= 0.0 && t < 1.0) && shown(held, true) >= 1.0F) {
                float pitch = (float)Math.toRadians(Math.max(-60.0F, Math.min(60.0F, body.m_146909_())));
                com.hex.kagune.control.client.animation.QuinquePose.pitchBy(p, weapon, pitch);
            }
            double since = clock - swing.shot;
            double kick = Math.max(kick(since), kick(since - Quinque.SHOT_EVERY));
            com.hex.kagune.control.client.animation.QuinquePose.recoil(p, weapon, (float)kick);
        }
        // 1.3.33: both hands on a two-handed quinque, whatever moved the right arm (the left put on its grip, last).
        if (p != null && weapon.twoHanded && shown(held, true) >= 1.0F) {
            boolean slim = body instanceof net.minecraft.client.player.AbstractClientPlayer player && "slim".equals(player.m_108564_());
            com.hex.kagune.control.client.animation.QuinqueGrip.reach(p, weapon, slim);
        }
        return p;
    }

    /** A kick {@code since} ticks after its shot: sharp, gone in QuinquePose.KICK_TICKS. */
    static double kick(double since) {
        return since < 0.0 ? 0.0 : Math.max(0.0, 1.0 - since / com.hex.kagune.control.client.animation.QuinquePose.KICK_TICKS);
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

    /**
     * 1.3.28: the frame a shut case is drawn in ({@code holder}: who holds it, or null): as it opens, its opening frames; shut, in an
     * investigator's hand, the carried frame solved for its arms (AgentPose.caseFrame), in anyone else's the plain shut case.
     */
    static float caseFrame(ItemStack stack, net.minecraft.world.entity.LivingEntity holder) {
        float shown = shown(stack, false);
        try {
            if (holder instanceof com.hex.kagune.control.npc.CcgAgent agent) {
                com.hex.kagune.control.npc.AgentRules.Look look = agent.look();
                return com.hex.kagune.control.client.animation.AgentPose.caseFrame(shown, true, look.slim() || look.female());
            }
        } catch (RuntimeException failure) {
            // (its look unreadable: drawn as anyone's)
        }
        return com.hex.kagune.control.client.animation.AgentPose.caseFrame(shown, false, false);
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
