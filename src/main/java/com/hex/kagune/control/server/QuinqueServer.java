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
        // 3.0.6: a kagune-made weapon's case tears open (its weapon's own right clicks are its moves: WeaponClient, PlayerArms).
        com.hex.kagune.control.quinque.Arm armCase = ControlItems.armCase(held);
        if (armCase != null) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            if (player instanceof ServerPlayer server && server.m_9236_() instanceof ServerLevel level && tapped(server, level.m_46467_())) {
                try {
                    PlayerArms.open(server, level, held, armCase);
                } catch (RuntimeException failure) {
                    com.hex.kagune.control.Safety.caught(failure, "a kagune weapon's case opening");
                }
            }
            return;
        }
        if (ControlItems.armWeapon(held) != null) {
            event.setCanceled(true); // (its moves come from WeaponClient; nothing else happens on a use that reached here)
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        Quinque shut = ControlItems.quinqueCase(held);
        Quinque open = shut == null ? ControlItems.quinqueWeapon(held) : null;
        boolean fires = open != null && open.firesOnUse() && !player.m_6047_();
        // 1.3.33: a melee quinque folds back into its case at a tap of right click (the owner's ask); the launcher fires on right
        // click, so it still shuts on Shift + right click. 2.0.32: Dawn too (its lightning).
        boolean folds = open != null && (!open.firesOnUse() || player.m_6047_());
        if (shut == null && !fires && !folds) {
            return; // not a quinque
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!(player instanceof ServerPlayer server) || !(server.m_9236_() instanceof ServerLevel level)) {
            return; // the server turns it (and fires it); the client only swings
        }
        try {
            if (fires && open == Quinque.DAWN) {
                gather(server, level, held);
            } else if (fires) {
                fire(server, level, held, open);
                SECOND.put(server.m_20148_(), level.m_46467_() + Quinque.SHOT_EVERY);
            } else if (tapped(server, level.m_46467_())) {
                turn(server, level, held, shut != null ? shut : open, shut != null);
            }
        } catch (RuntimeException failure) {
            com.hex.kagune.control.Safety.caught(failure, "a quinque turning");
        }
    }

    /** 1.3.33: each player's last right click on a quinque that turns it (game time), to tell a tap from the button held down. */
    private static final java.util.Map<UUID, Long> USED = WorldTimes.map();
    /** Vanilla repeats a held right click every four ticks: one this soon after the last is the button still held, not a new tap. */
    static final long HELD_REPEAT = 5L;

    /**
     * True for a fresh tap of right click, false for vanilla's repeat of one held down (so holding the button opens a case once and
     * never folds the weapon straight back).
     */
    static boolean tapped(ServerPlayer player, long now) {
        Long last = USED.put(player.m_20148_(), now);
        return last == null || now - last > HELD_REPEAT || now < last;
    }

    // ---------------------------------------------------------------- 1.3.26: the launcher

    /** 1.3.33: the second shard of each pull's pair, due at this game time (fired from the player's tick). */
    private static final java.util.Map<UUID, Long> SECOND = WorldTimes.map();

    /** Each player's launcher: its heat, its last shot, when it is cool enough again after overheating. */
    private static final java.util.Map<UUID, long[]> HEAT = new java.util.HashMap<>();

    /**
     * A shard from the launcher in {@code player}'s hand (use held repeats every four ticks; 1.3.33: each pull fires a second shard two
     * ticks on, ten a second): from its muzzle along their aim, a little spread; it heats a shard at a time and cools as it rests, and at
     * its most it overheats and won't fire a while.
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

    // ---------------------------------------------------------------- 2.0.32: Dawn's lightning in a player's hand

    /** Each player's Dawn: when its ball leaves (game time; the player's tick releases it), and when its last one left. */
    private static final java.util.Map<UUID, Long> DAWN_DUE = WorldTimes.map();
    private static final java.util.Map<UUID, Long> DAWN_LAST = WorldTimes.map();

    /**
     * 2.0.32: a right click with Dawn out: its point opens (its stack's {@link Quinque#DAWN_AT} / {@link Quinque#DAWN_FIRES}, drawn as its
     * "fire" frame by every client) and a ball gathers there for {@link Quinque#DAWN_CHARGE} ticks, then flies ({@link #release}); not
     * while it is still opening out of its case, nor within {@link Quinque#DAWN_COOLDOWN} of the last.
     */
    static void gather(ServerPlayer player, ServerLevel level, ItemStack held) {
        long now = level.m_46467_();
        CompoundTag tag = held.m_41783_();
        if (tag != null && tag.m_128441_(Quinque.AT) && Quinque.moving(now, tag.m_128454_(Quinque.AT))) {
            return; // still opening
        }
        if (DAWN_DUE.containsKey(player.m_20148_())) {
            return; // a ball already gathering
        }
        Long last = DAWN_LAST.get(player.m_20148_());
        if (last != null && !Quinque.dawnReady(now - last)) {
            player.m_5661_(net.minecraft.network.chat.Component.m_237113_(String.format("\u00a77Dawn gathers again in %.1f s.",
                (Quinque.DAWN_COOLDOWN - (now - last)) / 20.0)), true);
            return;
        }
        opens(held, now, now + Quinque.DAWN_CHARGE);
        DAWN_DUE.put(player.m_20148_(), now + Quinque.DAWN_CHARGE);
        UkakuFire.sound(level, player.m_146892_(), "hexkagune:sfx.dash", 0.35F, 1.7F);
    }

    /** 2.0.32: Dawn's point (the stack {@code held}) opens at {@code at} for a ball leaving at {@code fires}: every client draws it open. */
    public static void opens(ItemStack held, long at, long fires) {
        CompoundTag own = held.m_41783_();
        if (own == null) {
            own = new CompoundTag();
            held.m_41751_(own);
        }
        own.m_128356_(Quinque.DAWN_AT, at);
        own.m_128356_(Quinque.DAWN_FIRES, fires);
    }

    /** 2.0.32: the gathered ball leaves the open point of the Dawn in {@code player}'s hand, along their look (if it is still there). */
    static void release(ServerPlayer player, ServerLevel level) {
        Quinque out = ControlItems.quinqueWeapon(player.m_21205_());
        if (out != Quinque.DAWN || !player.m_6084_()) {
            return;
        }
        long now = level.m_46467_();
        DAWN_LAST.put(player.m_20148_(), now);
        Vec3 look = player.m_20154_();
        Vec3 right = new Vec3(-look.f_82481_, 0.0, look.f_82479_).m_82541_();
        Vec3 eye = player.m_146892_();
        Vec3 hand = eye.m_82549_(right.m_82490_(0.3)).m_82520_(0.0, -0.4, 0.0);
        Vec3 tip = hand.m_82549_(look.m_82490_(Quinque.DAWN_TIP));
        // Never through a wall right in front: from the eye's side of anything in the way.
        net.minecraft.world.phys.BlockHitResult in = level.m_45547_(new net.minecraft.world.level.ClipContext(eye, tip,
            net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, player));
        if (in.m_6662_() != net.minecraft.world.phys.HitResult.Type.MISS) {
            tip = in.m_82450_().m_82546_(look.m_82490_(0.3));
        }
        CrystalShots.lightning(player, level, tip, look, body -> !(body instanceof ServerPlayer other && (other.m_7500_() || other.m_5833_())),
            Quinque.DAWN_PLAYER_SHOT);
        UkakuFire.sound(level, tip, "hexkagune:sfx.thunder", 0.6F, 1.5F);
    }

    /** {@code held} opens into its weapon ({@code opening}) or shuts into its case; not while it is still moving. */
    static void turn(ServerPlayer player, ServerLevel level, ItemStack held, Quinque quinque, boolean opening) {
        hand(player, level, held, quinque, opening);
    }

    /** 3.1.2: one dedicated key for every quinque, including the newer kagune-made items. Already shut is a no-op. */
    static void fold(ServerPlayer player, ServerLevel level) {
        ItemStack held = player.m_21205_();
        com.hex.kagune.control.quinque.Arm arm = ControlItems.armWeapon(held);
        if (arm != null) {
            PlayerArms.close(player, level, held, arm);
            return;
        }
        Quinque open = ControlItems.quinqueWeapon(held);
        if (open != null && hand(player, level, held, open, false)) {
            DAWN_DUE.remove(player.m_20148_());
            SECOND.remove(player.m_20148_());
        }
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
        // 1.3.28: the case's own mechanism (tools/case_sfx.py): opening, its two latches thumbed back, a servo spinning up, plates
        // sliding out and the lock home, with the weapon's own ring under it; shutting, the servo back, the halves meeting and the
        // latches snapped shut. Both last the 12 ticks it takes.
        float pitch = 0.96F + 0.08F * (float)who.m_217043_().m_188500_();
        if (opening) {
            UkakuFire.sound(level, at, "hexkagune:sfx.case_open", 1.0F, pitch);
            UkakuFire.sound(level, at, quinque == Quinque.SPEAR ? "hexkagune:sfx.chitin" : "hexkagune:sfx.blade_arc", 0.35F, 1.1F);
        } else {
            UkakuFire.sound(level, at, "hexkagune:sfx.case_close", 1.0F, pitch);
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
            } else if (quinque.arc(step) > 0.0) {
                arc(player, event.getEntity(), quinque, step, event.getAmount(), level, QuinqueServer::sweepable);
            }
        }
        if (quinque == Quinque.DAWN && attacker instanceof com.hex.kagune.control.npc.CcgAgent
            || !ghoul(event.getEntity(), level)) {
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
        if (quinque == Quinque.DAWN) {
            target.m_147240_(0.8, -look.f_82479_, -look.f_82481_); // 2.0.31: the thrust drives it back along its line
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
                } else if (quinque == Quinque.SPEAR || quinque == Quinque.DAWN) {
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
                if (other.m_6469_(source, dealt * (quinque == Quinque.MACE ? 0.6F : quinque == Quinque.SPEAR || quinque == Quinque.DAWN ? 0.8F : 0.7F))
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

    /**
     * 2.0.31: an ordinary blow's arc ({@link Quinque#arc}: Dawn's wide sweep): everyone {@code may} allows in front of {@code attacker}
     * within its reach besides {@code target} takes {@link Quinque#ARC_SHARE} of {@code dealt}. A player's or an investigator's.
     */
    static void arc(LivingEntity attacker, LivingEntity target, Quinque quinque, int step, float dealt, ServerLevel level,
                    java.util.function.Predicate<LivingEntity> may) {
        double reach = quinque.arc(step);
        if (reach <= 0.0 || sweeping) {
            return;
        }
        Vec3 look = UkakuFire.horizontal(attacker.m_20154_());
        Vec3 from = attacker.m_20182_();
        AABB area = new AABB(from.f_82479_, from.f_82480_, from.f_82481_, from.f_82479_, from.f_82480_, from.f_82481_).m_82400_(reach + 1.0);
        sweeping = true;
        try {
            for (LivingEntity other : level.m_6443_(LivingEntity.class, area, b -> b != attacker && b != target && b.m_6084_() && may.test(b))) {
                Vec3 to = GhoulNpcBrain.centre(other).m_82546_(attacker.m_146892_());
                Vec3 flat = UkakuFire.horizontal(to);
                if (to.m_82553_() > reach + other.m_20205_() * 0.5 || Math.abs(to.f_82480_) > 2.0 || flat.m_82526_(look) < 0.2) {
                    continue;
                }
                other.f_19802_ = 0;
                net.minecraft.world.damagesource.DamageSource source = attacker instanceof ServerPlayer server ? attacker.m_269291_().m_269075_(server)
                    : attacker.m_269291_().m_269333_(attacker);
                other.m_6469_(source, dealt * Quinque.ARC_SHARE);
            }
        } finally {
            sweeping = false;
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
        // 2.0.32: Dawn's gathered ball, due now.
        Long ball = DAWN_DUE.isEmpty() ? null : DAWN_DUE.get(player.m_20148_());
        if (ball != null && player.m_9236_() instanceof ServerLevel here && here.m_46467_() >= ball) {
            DAWN_DUE.remove(player.m_20148_());
            try {
                release(player, here);
            } catch (RuntimeException failure) {
                com.hex.kagune.control.Safety.caught(failure, "Dawn's lightning");
            }
        }
        // 1.3.33: the second shard of the pull, if the launcher is still out in the hand and not being shut.
        Long due = SECOND.isEmpty() ? null : SECOND.get(player.m_20148_());
        if (due != null && player.m_9236_() instanceof ServerLevel level && level.m_46467_() >= due) {
            SECOND.remove(player.m_20148_());
            if (out != null && out.ranged && !player.m_6047_() && player.m_6084_() && level.m_46467_() - due < 4L) {
                try {
                    fire(player, level, player.m_21205_(), out);
                } catch (RuntimeException failure) {
                    com.hex.kagune.control.Safety.caught(failure, "a quinque firing");
                }
            }
        }
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
