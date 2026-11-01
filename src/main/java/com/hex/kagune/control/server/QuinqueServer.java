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
import net.minecraft.world.phys.AABB;
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
    /** 1.3.26: the spear's quickness on its feet (agile), while it is out. */
    static final UUID QUICK = UUID.fromString("3d1f6b2a-9c4e-4f71-8a25-6e0b4c9d1f37");

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
        boolean fires = open != null && open.ranged && !player.m_6047_();
        if (shut == null && !fires && (open == null || !player.m_6047_())) {
            return; // not a quinque; or a blade used without Shift (it is a blade, nothing more)
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!(player instanceof ServerPlayer server) || !(server.m_9236_() instanceof ServerLevel level)) {
            return; // the server turns it (and fires it); the client only swings
        }
        try {
            if (fires) {
                fire(server, level, held, open);
            } else {
                turn(server, level, held, shut != null ? shut : open, shut != null);
            }
        } catch (RuntimeException failure) {
            com.hex.kagune.control.Safety.caught(failure, "a quinque turning");
        }
    }

    // ---------------------------------------------------------------- 1.3.26: the launcher

    /** Each player's launcher: its heat, its last shot, when it is cool enough again after overheating. */
    private static final java.util.Map<UUID, long[]> HEAT = new java.util.HashMap<>();

    /**
     * A shard from the launcher in {@code player}'s hand (use held repeats every four ticks: five a second): from its muzzle along their
     * aim, a little spread; it heats a shard at a time and cools a shard a tick, and at its most it overheats and won't fire a while.
     */
    static void fire(ServerPlayer player, ServerLevel level, ItemStack held, Quinque quinque) {
        long now = level.m_46467_();
        CompoundTag tag = held.m_41783_();
        if (tag != null && tag.m_128441_(Quinque.AT) && Quinque.moving(now, tag.m_128454_(Quinque.AT))) {
            return; // still opening
        }
        long[] heat = HEAT.computeIfAbsent(player.m_20148_(), id -> new long[] {0L, Long.MIN_VALUE, Long.MIN_VALUE});
        if (heat[2] != Long.MIN_VALUE && now < heat[2]) {
            return; // overheated
        }
        int hot = Quinque.heated((int)heat[0], heat[1] == Long.MIN_VALUE ? Long.MAX_VALUE : now - heat[1]);
        heat[0] = hot;
        heat[1] = now;
        Vec3 look = player.m_20154_();
        Vec3 right = new Vec3(-look.f_82481_, 0.0, look.f_82479_).m_82541_();
        Vec3 from = player.m_146892_().m_82549_(look.m_82490_(0.9)).m_82549_(right.m_82490_(0.22)).m_82520_(0.0, -0.32, 0.0);
        double spread = Math.toRadians(1.6);
        Vec3 way = look.m_82520_((player.m_217043_().m_188500_() - 0.5) * spread, (player.m_217043_().m_188500_() - 0.5) * spread,
            (player.m_217043_().m_188500_() - 0.5) * spread).m_82541_();
        CrystalShots.fire(player, level, from, way, Quinque.SHARD_SPEED, Quinque.SHARD_RANGE, Quinque.SHARD_DAMAGE,
            body -> !(body instanceof ServerPlayer other && (other.m_7500_() || other.m_5833_())));
        UkakuFire.sound(level, from, "block.amethyst_block.hit", 0.55F, 1.55F + 0.15F * (float)player.m_217043_().m_188500_());
        if (hot >= Quinque.HEAT_MOST) {
            heat[2] = now + Quinque.COOL;
            heat[0] = 0L;
            UkakuFire.sound(level, from, "hexkagune:kagune_dismiss", 0.7F, 0.7F);
            player.m_5661_(net.minecraft.network.chat.Component.m_237113_("§7The launcher overheats."), true);
        }
    }

    /** {@code held} opens into its weapon ({@code opening}) or shuts into its case; not while it is still moving. */
    static void turn(ServerPlayer player, ServerLevel level, ItemStack held, Quinque quinque, boolean opening) {
        hand(player, level, held, quinque, opening);
    }

    /**
     * 1.3.27: the quinque in {@code who}'s hand ({@code held}) opens into its weapon or shuts into its case (a player's, an
     * investigator's): the stack keeps everything it carries and when it turned; the latch, the unfolding, the lock heard. False if it
     * is still moving from the last time (nothing happens).
     */
    static boolean hand(LivingEntity who, ServerLevel level, ItemStack held, Quinque quinque, boolean opening) {
        long now = level.m_46467_();
        CompoundTag tag = held.m_41783_();
        if (tag != null && tag.m_128441_(Quinque.AT) && Quinque.moving(now, tag.m_128454_(Quinque.AT))) {
            return false;
        }
        ItemStack next = new ItemStack(opening ? ControlItems.weaponOf(quinque) : ControlItems.caseOf(quinque), 1);
        CompoundTag kept = tag == null ? new CompoundTag() : tag.m_6426_();
        kept.m_128356_(Quinque.AT, now);
        next.m_41751_(kept);
        who.m_21008_(InteractionHand.MAIN_HAND, next);
        Vec3 at = who.m_20182_().m_82520_(0.0, who.m_20206_() * 0.6, 0.0);
        if (opening) {
            // The latch, the case unfolding, the weapon locking out (the owner's own pack, as every sound in the mod).
            UkakuFire.sound(level, at, "hexkagune:centipede_click", 0.8F, 1.5F);
            UkakuFire.sound(level, at, "hexkagune:sfx.transform.organic_armor_shift", 0.6F, 1.3F);
            UkakuFire.sound(level, at, quinque == Quinque.SPEAR ? "hexkagune:sfx.chitin" : "hexkagune:sfx.blade_arc", 0.7F, 1.1F);
        } else {
            UkakuFire.sound(level, at, "hexkagune:kagune_dismiss", 0.6F, 1.3F);
            UkakuFire.sound(level, at, "hexkagune:centipede_click", 0.8F, 1.3F);
        }
        return true;
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
        if (quinque == null) {
            return;
        }
        long now = level.m_46467_();
        if (attacker instanceof ServerPlayer player && !CrystalShots.striking && !sweeping) {
            // 1.3.26: its string of M1s: the third blow the finisher (harder, and the katana's, the spear's and the mace's reaching others).
            long[] combo = COMBOS.computeIfAbsent(player.m_20148_(), id -> new long[] {-1L, Long.MIN_VALUE});
            int step = Quinque.next((int)combo[0], combo[1] == Long.MIN_VALUE ? Long.MAX_VALUE : now - combo[1]);
            combo[0] = step;
            combo[1] = now;
            event.setAmount(event.getAmount() * quinque.weight(step));
            if (step == Quinque.FINISHER) {
                finisher(player, event.getEntity(), quinque, event.getAmount(), level);
            }
        }
        if (!ghoul(event.getEntity(), level)) {
            return;
        }
        event.setAmount(event.getAmount() * quinque.ghoul);
        if (event.getEntity() instanceof ServerPlayer player) {
            GhoulRegen.suppress(player, now + quinque.suppress);
        }
    }

    /** Each player's string of M1s: its last blow's place in the three, and when it landed. */
    private static final java.util.Map<UUID, long[]> COMBOS = new java.util.HashMap<>();
    /** True while a finisher's reach is being dealt to the others round it (not blows of their own). */
    private static boolean sweeping;

    /**
     * A string's third blow on {@code target}: the blade knocks it back; the katana's wide cut takes the others in front too, the spear's
     * thrust the others in its line, the mace's slam the others round where it lands (thrown up); each a share of the blow ({@code dealt}).
     * 1.3.27: a player's or an investigator's.
     */
    static void finisher(LivingEntity player, LivingEntity target, Quinque quinque, float dealt, ServerLevel level) {
        Vec3 look = player.m_20154_();
        Vec3 at = GhoulNpcBrain.centre(target);
        UkakuFire.sound(level, at, quinque == Quinque.MACE ? "hexkagune:kagune_slam" : "hexkagune:sfx.blade_heavy", 0.9F,
            quinque == Quinque.MACE ? 0.7F : 1.1F);
        if (quinque == Quinque.BLADE) {
            target.m_147240_(0.6, -look.f_82479_, -look.f_82481_);
            return;
        }
        double reach = quinque.sweep(Quinque.FINISHER);
        if (reach <= 0.0) {
            return;
        }
        Vec3 centre = quinque == Quinque.MACE ? at : player.m_20182_();
        AABB area = new AABB(centre.f_82479_, centre.f_82480_, centre.f_82481_, centre.f_82479_, centre.f_82480_, centre.f_82481_).m_82400_(reach + 1.0);
        sweeping = true;
        try {
            for (LivingEntity other : level.m_6443_(LivingEntity.class, area, b -> b != player && b != target && b.m_6084_() && sweepable(b))) {
                Vec3 to = GhoulNpcBrain.centre(other).m_82546_(player.m_146892_());
                boolean hits;
                if (quinque == Quinque.MACE) {
                    hits = other.m_20238_(at) <= reach * reach;
                } else if (quinque == Quinque.SPEAR) {
                    double along = to.m_82526_(look);
                    hits = along > 0.0 && along <= reach && to.m_82546_(look.m_82490_(along)).m_82553_() <= 0.9;
                } else {
                    hits = to.m_82553_() <= reach && to.m_82541_().m_82526_(look) > 0.35;
                }
                if (!hits) {
                    continue;
                }
                other.f_19802_ = 0;
                net.minecraft.world.damagesource.DamageSource source = player instanceof ServerPlayer server ? player.m_269291_().m_269075_(server)
                    : player.m_269291_().m_269333_(player);
                if (other.m_6469_(source, dealt * (quinque == Quinque.MACE ? 0.6F : quinque == Quinque.SPEAR ? 0.8F : 0.7F))
                    && quinque == Quinque.MACE) {
                    other.m_20256_(other.m_20184_().m_82520_(0.0, 0.55, 0.0));
                    other.f_19864_ = true;
                }
            }
        } finally {
            sweeping = false;
        }
        if (quinque == Quinque.MACE) {
            KaguneClash.burst(player, at.m_82520_(0.0, -0.6, 0.0), look, 1.2F, 0.9F);
        }
    }

    /** What a finisher's reach takes besides its target: monsters and ghouls, never another player nor anyone's own. */
    static boolean sweepable(LivingEntity body) {
        if (body instanceof Player) {
            return false;
        }
        return body instanceof net.minecraft.world.entity.monster.Enemy || body instanceof GhoulNpc || body instanceof FleshThrall;
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
        AttributeInstance speed = player.m_21051_(net.minecraft.world.entity.ai.attributes.Attributes.f_22279_);
        if (speed != null) {
            boolean quick = out == Quinque.SPEAR;
            AttributeModifier fast = speed.m_22111_(QUICK);
            if (fast != null && !quick) {
                speed.m_22120_(QUICK);
            } else if (fast == null && quick) {
                speed.m_22118_(new AttributeModifier(QUICK, "Quinque spear", 0.08, AttributeModifier.Operation.MULTIPLY_BASE));
            }
        }
    }
}
