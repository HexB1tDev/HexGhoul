package com.hex.kagune.control.server;

import com.hex.kagune.control.ControlItems;
import com.hex.kagune.control.npc.ChildNpc;
import com.hex.kagune.control.npc.EtoNpc;
import com.hex.kagune.control.npc.FleshThrall;
import com.hex.kagune.control.npc.GhoulNpc;
import com.hex.kagune.control.quinque.Quinque;
import com.hex.kagune.data.KaguneProfile;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 1.3.25: the CCG's quinques in the hand ({@link Quinque}): a right click on a case releases it (the case becomes its weapon, opening
 * out over {@link Quinque#UNFOLD} ticks as its model is drawn); Shift + right click on the weapon shuts it back into its case. The stack
 * keeps everything it carries (its wear, its enchantments, its name) as it turns. Its cut goes deeper into a ghoul (a player with a
 * kagune, a wild ghoul, Eto, Touka, Ken, a flesh creature, a ghoul child) and holds a ghoul player's regeneration back a while; the
 * Koukaku spear reaches further while it is out.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class QuinqueServer {
    /** The spear's reach, while it is out in the main hand. */
    static final UUID REACH = UUID.fromString("8a3c1f0e-5b7d-4c2a-9e61-2f4d8b0c7a15");

    private QuinqueServer() {
    }

    @SubscribeEvent
    public static void use(PlayerInteractEvent.RightClickItem event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Player player = event.getEntity();
        ItemStack held = player.m_21205_();
        Quinque shut = ControlItems.quinqueCase(held);
        Quinque open = shut == null ? ControlItems.quinqueWeapon(held) : null;
        if (shut == null && (open == null || !player.m_6047_())) {
            return; // not a quinque; or the weapon used without Shift (it is a blade, nothing more)
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!(player instanceof ServerPlayer server) || !(server.m_9236_() instanceof ServerLevel level)) {
            return; // the server turns it; the client only swings
        }
        try {
            turn(server, level, held, shut != null ? shut : open, shut != null);
        } catch (RuntimeException failure) {
            com.hex.kagune.control.Safety.caught(failure, "a quinque turning");
        }
    }

    /** {@code held} opens into its weapon ({@code opening}) or shuts into its case; not while it is still moving. */
    static void turn(ServerPlayer player, ServerLevel level, ItemStack held, Quinque quinque, boolean opening) {
        long now = level.m_46467_();
        CompoundTag tag = held.m_41783_();
        if (tag != null && tag.m_128441_(Quinque.AT) && Quinque.moving(now, tag.m_128454_(Quinque.AT))) {
            return;
        }
        ItemStack next = new ItemStack(opening ? ControlItems.weaponOf(quinque) : ControlItems.caseOf(quinque), 1);
        CompoundTag kept = tag == null ? new CompoundTag() : tag.m_6426_();
        kept.m_128356_(Quinque.AT, now);
        next.m_41751_(kept);
        player.m_21008_(InteractionHand.MAIN_HAND, next);
        Vec3 at = player.m_20182_().m_82520_(0.0, player.m_20206_() * 0.6, 0.0);
        if (opening) {
            // The latch, the case unfolding, the weapon locking out (the owner's own pack, as every sound in the mod).
            UkakuFire.sound(level, at, "hexkagune:centipede_click", 0.8F, 1.5F);
            UkakuFire.sound(level, at, "hexkagune:sfx.transform.organic_armor_shift", 0.6F, 1.3F);
            UkakuFire.sound(level, at, quinque == Quinque.SPEAR ? "hexkagune:sfx.chitin" : "hexkagune:sfx.blade_arc", 0.7F, 1.1F);
        } else {
            UkakuFire.sound(level, at, "hexkagune:kagune_dismiss", 0.6F, 1.3F);
            UkakuFire.sound(level, at, "hexkagune:centipede_click", 0.8F, 1.3F);
        }
    }

    /** A quinque's cut: deeper into a ghoul, and a ghoul player's regeneration held back. Its wielder's own swing only (not a throw). */
    @SubscribeEvent
    public static void hurt(LivingHurtEvent event) {
        if (event.getSource() == null || !(event.getEntity().m_9236_() instanceof ServerLevel level)) {
            return;
        }
        Entity by = event.getSource().m_7639_();
        if (!(by instanceof LivingEntity attacker) || event.getSource().m_7640_() != attacker) {
            return;
        }
        Quinque quinque = ControlItems.quinqueWeapon(attacker.m_21205_());
        if (quinque == null || !ghoul(event.getEntity(), level)) {
            return;
        }
        event.setAmount(event.getAmount() * quinque.ghoul);
        if (event.getEntity() instanceof ServerPlayer player) {
            GhoulRegen.suppress(player, level.m_46467_() + quinque.suppress);
        }
    }

    /** True for a ghoul a quinque was made to cut. */
    static boolean ghoul(LivingEntity body, ServerLevel level) {
        if (body instanceof ServerPlayer player) {
            return KaguneProfile.isUnlocked(player);
        }
        if (body instanceof GhoulNpc || body instanceof EtoNpc || body instanceof FleshThrall) {
            return true;
        }
        if (body instanceof ChildNpc child) {
            Children.Child known = Children.get(level, child.childId());
            return known != null && known.genome().ghoul();
        }
        return false;
    }

    /** The spear's reach, kept on its wielder while it is out in their main hand (and taken off when it isn't). */
    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        Quinque out = ControlItems.quinqueWeapon(player.m_21205_());
        double reach = out == null ? 0.0 : out.reach;
        AttributeInstance instance = player.m_21051_(net.minecraftforge.common.ForgeMod.ENTITY_REACH.get());
        if (instance == null) {
            return;
        }
        AttributeModifier has = instance.m_22111_(REACH);
        if (has != null && reach <= 0.0) {
            instance.m_22120_(REACH);
        } else if (has == null && reach > 0.0) {
            instance.m_22118_(new AttributeModifier(REACH, "Quinque reach", reach, AttributeModifier.Operation.ADDITION));
        }
    }
}
