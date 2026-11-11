package com.hex.kagune.control.server;

import com.hex.kagune.control.ControlItems;
import com.hex.kagune.control.network.C2SArmAction;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CArmSwing;
import com.hex.kagune.control.npc.ArmRules;
import com.hex.kagune.control.npc.ChildNpc;
import com.hex.kagune.control.npc.EtoNpc;
import com.hex.kagune.control.npc.FleshThrall;
import com.hex.kagune.control.npc.GhoulNpc;
import com.hex.kagune.control.quinque.Arm;
import com.hex.kagune.control.quinque.Quinque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 3.0.6 (the owner: "the item is not droppable. Like, it's not in the game for the player, it's only for the NPCs"): a player wielding one of
 * the investigators' kagune-made weapons ({@link Arm}), as an investigator wields it ({@link AgentArms}), on the server.
 *
 * <p>A right click on its case tears the weapon out of it ({@link #open}: it grows out of the hand, {@link ArmRules#UNFOLD} ticks); put away
 * (another slot), it folds back into its case ({@link #fold}). Out, M1 is the next blow of its long string ({@link #begin}, landing at its own
 * tick on everyone in its reach and arc, {@link #land}: a click while one is under way is kept and goes when it is done, so a held rhythm runs
 * the whole string); right click is its first move, Shift + right click its second ({@link #start}: the whip's Bind and Flay, the spear's
 * Javelin and Piercing Charge, the hammer's Quake and Whirlwind, the greatsword's Leaping Cut and Cleave), each on its cooldown. Every client
 * sees each blow and move ({@link S2CArmSwing}).
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class PlayerArms {
    /** A player's blows come at this pace (an investigator's rank's: a little quicker than a rank 4's). */
    static final double PACE = 0.9;
    /** How much of a blow's own weight a player's arm deals (an investigator's rank's power: a rank 6's). */
    static final float POWER = 1.0F;
    /** A click this long before the blow under way is done is kept for when it is (ticks); earlier, it is dropped. */
    static final long BUFFER = 14L;
    /** How long after a leap or a slam their own fall is forgiven (ticks). */
    static final long SAFE_AFTER = 30L;

    private PlayerArms() {
    }

    /** One player's weapon as it is being swung. */
    static final class Wield {
        Arm arm;
        boolean master;
        int step = -1;
        long stepAt = Long.MIN_VALUE;
        boolean striking;
        long landsAt;
        long readyAt = Long.MIN_VALUE;
        int target;
        boolean queued;
        int queuedTarget;
        Arm.Ability move;
        long moveAt;
        int moveTarget;
        int moveHit;
        boolean bound;
        Vec3 moveWay = Vec3.f_82478_;
        final Set<Integer> moveStruck = new HashSet<>();
        final long[] moveReady = {Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE};
        /** 3.1.8: the Tendril Swing's latch (where the whip bit), and whether it has hauled him there. */
        Vec3 anchor;
        boolean arrived;
        final List<long[]> bleeds = new ArrayList<>();
        long safeUntil = Long.MIN_VALUE;
    }

    private static final Map<UUID, Wield> WIELDS = WorldTimes.kept();

    // ---------------------------------------------------------------- out of its case and back

    /**
     * A right click on its case ({@code held}, in {@code player}'s main hand): the case becomes its weapon, keeping all it carries (a
     * masterwork's mark, its name), and every client draws it tearing out of the hand. Not while it is still moving.
     */
    static void open(ServerPlayer player, ServerLevel level, ItemStack held, Arm arm) {
        long now = level.m_46467_();
        CompoundTag tag = held.m_41783_();
        if (tag != null && tag.m_128441_(Quinque.AT) && now - tag.m_128454_(Quinque.AT) < ArmRules.UNFOLD && now >= tag.m_128454_(Quinque.AT)) {
            return;
        }
        ItemStack next = new ItemStack(ControlItems.armWeaponOf(arm), 1);
        CompoundTag kept = tag == null ? new CompoundTag() : tag.m_6426_();
        kept.m_128356_(Quinque.AT, now);
        next.m_41751_(kept);
        player.m_21008_(InteractionHand.MAIN_HAND, next);
        Vec3 at = hand(player);
        UkakuFire.sound(level, at, "hexkagune:sfx.case_open", 1.0F, 0.95F + 0.1F * (float)player.m_217043_().m_188500_());
        UkakuFire.sound(level, at, arm == Arm.CRYSTAL_SPEAR ? "hexkagune:sfx.crystal_form" : "hexkagune:manifest_rip", 0.7F, 0.9F);
        UkakuFire.sound(level, at, "hexkagune:kagune_reshape", 0.6F, 1.0F);
    }

    /** True while the weapon in {@code held} is still tearing out of its case (it can't be swung yet). */
    static boolean unfolding(ItemStack held, long now) {
        CompoundTag tag = held.m_41783_();
        if (tag == null || !tag.m_128441_(Quinque.AT)) {
            return false;
        }
        long at = tag.m_128454_(Quinque.AT);
        return now >= at && now - at < ArmRules.UNFOLD;
    }

    /** Every weapon of these in {@code player}'s inventory but the one in their hand folds back into its case (quietly; once a second). */
    static void fold(ServerPlayer player, ServerLevel level) {
        ItemStack inHand = player.m_21205_();
        net.minecraft.world.entity.player.Inventory inventory = player.m_150109_();
        boolean folded = false;
        for (int slot = 0; slot < inventory.m_6643_(); slot++) {
            ItemStack stack = inventory.m_8020_(slot);
            if (stack == inHand || stack.m_41619_()) {
                continue;
            }
            Arm arm = ControlItems.armWeapon(stack);
            if (arm == null) {
                continue;
            }
            ItemStack shut = new ItemStack(ControlItems.armCaseOf(arm), 1);
            CompoundTag tag = stack.m_41783_();
            if (tag != null) {
                CompoundTag kept = tag.m_6426_();
                kept.m_128473_(Quinque.AT);
                shut.m_41751_(kept);
            }
            inventory.m_6836_(slot, shut);
            folded = true;
        }
        if (folded) {
            UkakuFire.sound(level, hand(player), "hexkagune:sfx.case_close", 0.7F, 1.0F);
        }
    }

    // ---------------------------------------------------------------- asked for

    /** 3.1.2: the dedicated fold key: keep the stack's data, cancel pending hits/moves and draw the case closing. */
    static void close(ServerPlayer player, ServerLevel level, ItemStack held, Arm arm) {
        long now = level.m_46467_();
        if (unfolding(held, now)) {
            return;
        }
        ItemStack shut = new ItemStack(ControlItems.armCaseOf(arm), 1);
        CompoundTag tag = held.m_41783_();
        CompoundTag kept = tag == null ? new CompoundTag() : tag.m_6426_();
        kept.m_128356_(Quinque.AT, now);
        shut.m_41751_(kept);
        Wield w = WIELDS.get(player.m_20148_());
        if (w != null) {
            finish(player, w, now);
            w.striking = false;
            w.queued = false;
        }
        player.m_21008_(InteractionHand.MAIN_HAND, shut);
        UkakuFire.sound(level, hand(player), "hexkagune:sfx.case_close", 0.7F, 1.0F);
    }

    /** {@code player} asks for a blow ({@link C2SArmAction#STRIKE}) or one of its moves, at {@code target} (0: none). */
    public static void request(ServerPlayer player, int kind, int target) {
        if (!(player.m_9236_() instanceof ServerLevel level) || !player.m_6084_() || player.m_5833_()) {
            return;
        }
        try {
            if (kind == C2SArmAction.FOLD) {
                QuinqueServer.fold(player, level);
                return;
            }
            long now = level.m_46467_();
            ItemStack held = player.m_21205_();
            Arm arm = ControlItems.armWeapon(held);
            if (arm == null || unfolding(held, now)) {
                return;
            }
            Wield w = wield(player, arm, ControlItems.masterwork(held));
            if (kind == C2SArmAction.STRIKE) {
                if (w.move != null) {
                    return;
                }
                if (w.striking || now < w.readyAt) {
                    // Kept for when the one under way is done, if it is nearly done.
                    if (Math.max(w.readyAt, w.landsAt) - now <= BUFFER) {
                        w.queued = true;
                        w.queuedTarget = target;
                    }
                    return;
                }
                begin(player, level, w, target, now);
            } else if (kind == C2SArmAction.FIRST || kind == C2SArmAction.SECOND) {
                move(player, level, w, kind == C2SArmAction.FIRST ? 0 : 1, target, now);
            } else if (kind == C2SArmAction.THIRD && arm.signature() != null) {
                move(player, level, w, 2, target, now); // 3.1.8: its signature (Sneak + M1)
            }
        } catch (RuntimeException failure) {
            com.hex.kagune.control.Safety.caught(failure, "a kagune weapon swung");
        }
    }

    /** {@code player}'s wield of {@code arm}, fresh if they took up another. */
    private static Wield wield(ServerPlayer player, Arm arm, boolean master) {
        Wield w = WIELDS.get(player.m_20148_());
        if (w == null || w.arm != arm) {
            Wield fresh = new Wield();
            if (w != null) {
                // The other's moves' cooldowns and its bleeds go on.
                fresh.bleeds.addAll(w.bleeds);
                fresh.safeUntil = w.safeUntil;
            }
            w = fresh;
            w.arm = arm;
            WIELDS.put(player.m_20148_(), w);
        }
        w.master = master;
        return w;
    }

    // ---------------------------------------------------------------- its string

    /** The next blow of its string sets out: every client sees it; it lands at its own tick; a step in behind it. */
    static void begin(ServerPlayer player, ServerLevel level, Wield w, int target, long now) {
        Arm arm = w.arm;
        w.step = ArmRules.next(arm, w.step, w.stepAt == Long.MIN_VALUE ? Long.MAX_VALUE : now - w.stepAt);
        w.stepAt = now;
        Arm.Strike strike = arm.strike(w.step);
        w.striking = true;
        w.queued = false;
        w.target = target;
        w.landsAt = now + strike.land();
        w.readyAt = w.landsAt + ArmRules.recover(arm, w.step, PACE);
        ControlNetwork.broadcast(player, new S2CArmSwing(player.m_19879_(), ArmRules.stepCode(w.step), now, 0));
        boolean last = w.step == arm.length() - 1;
        UkakuFire.sound(level, hand(player), AgentArms.swingSound(arm, last), 0.6F, AgentArms.pitch(arm) + 0.12F * (float)player.m_217043_().m_188500_());
        if (player.m_20096_() && strike.step() > 0.0) {
            Vec3 look = UkakuFire.horizontal(player.m_20154_());
            Vec3 velocity = player.m_20184_();
            double step = strike.step() * 0.55;
            player.m_20256_(new Vec3(velocity.f_82479_ + look.f_82479_ * step, velocity.f_82480_, velocity.f_82481_ + look.f_82481_ * step));
            player.f_19864_ = true;
        }
    }

    /** The blow lands on everyone in its reach and arc about where they look. */
    static void land(ServerPlayer player, ServerLevel level, Wield w, long now) {
        Arm arm = w.arm;
        int step = w.step;
        Arm.Strike strike = arm.strike(step);
        w.striking = false;
        Entity found = w.target != 0 ? level.m_6815_(w.target) : null;
        LivingEntity aimed = found instanceof LivingEntity body && body.m_6084_() ? body : null;
        Vec3 look = UkakuFire.horizontal(player.m_20154_());
        double reach = ArmRules.reach(arm, step, w.master);
        float dealt = ArmRules.damage(arm, step, w.master) * POWER;
        boolean any = false;
        for (LivingEntity body : foes(player, level, w.target, reach + 1.5)) {
            if (AgentArms.inArc(player, body, look, reach, strike.arc())) {
                any |= hit(player, w, body, body == aimed ? dealt : dealt * 0.7F, strike.blow(), player.m_20182_(), look, strike.push(), strike.lift(),
                    level, now);
            }
        }
        boolean last = step == arm.length() - 1;
        if (any) {
            UkakuFire.sound(level, aimed != null ? GhoulNpcBrain.centre(aimed) : hand(player), AgentArms.hitSound(arm, strike.blow()), 0.8F,
                0.9F + 0.15F * (float)player.m_217043_().m_188500_());
            if (last) {
                UkakuFire.sound(level, hand(player), "hexkagune:sfx.finisher", 0.7F, arm == Arm.FLESH_HAMMER ? 0.7F : 1.0F);
            }
        }
        if (last && arm == Arm.FLESH_HAMMER) {
            KaguneClash.quake(player, player.m_20182_().m_82549_(look.m_82490_(1.6)), 1.3F); // its smash shakes the ground
        }
    }

    // ---------------------------------------------------------------- its moves

    /** Its {@code slot}th move (0 right click, 1 Shift + right click), if it is ready and it has what it needs (the Bind, someone to catch). */
    static void move(ServerPlayer player, ServerLevel level, Wield w, int slot, int target, long now) {
        if (w.move != null || w.striking) {
            return;
        }
        Arm.Ability ability = slot == 2 ? w.arm.signature() : w.arm.abilities()[slot];
        if (ability == null) {
            return;
        }
        if (now < w.moveReady[slot]) {
            player.m_5661_(Component.m_237113_(String.format("§7%s again in %.1f s.", ability.title, (w.moveReady[slot] - now) / 20.0)), true);
            return;
        }
        double edge = w.master ? 1.12 : 1.0;
        LivingEntity aimed = aim(player, level, target, ability.farthest * edge + 1.0);
        if (ability == Arm.Ability.BIND && aimed == null) {
            player.m_5661_(Component.m_237113_("§7No one in reach to bind."), true);
            return;
        }
        w.anchor = null;
        w.arrived = false;
        if (ability == Arm.Ability.SWING) {
            // 3.1.8: lashed onto the block he aims at (no body: it is a way to move).
            net.minecraft.world.phys.BlockHitResult block = com.hex.kagune.gameplay.KaguneServerController.traceBlock(player, Arm.SWING_REACH);
            if (block == null || block.m_6662_() != net.minecraft.world.phys.HitResult.Type.BLOCK) {
                player.m_5661_(Component.m_237113_("§7Nothing in reach to lash onto."), true);
                return;
            }
            w.anchor = block.m_82450_();
            aimed = null;
        } else if (ability == Arm.Ability.GUARD || ability == Arm.Ability.FAULT) {
            aimed = null; // a guard raised, the ground struck: straight ahead, where he faces
        }
        start(player, level, w, ability, slot, aimed, now);
    }

    /** {@code ability} begins (at {@code aimed}, or along their look): every client sees it; its cooldown starts. */
    static void start(ServerPlayer player, ServerLevel level, Wield w, Arm.Ability ability, int slot, LivingEntity aimed, long now) {
        w.move = ability;
        w.moveAt = now;
        w.moveTarget = aimed == null ? 0 : aimed.m_19879_();
        w.moveHit = 0;
        w.moveStruck.clear();
        w.bound = false;
        w.queued = false;
        w.moveWay = aimed != null ? UkakuFire.horizontal(aimed.m_20182_().m_82546_(player.m_20182_())) : UkakuFire.horizontal(player.m_20154_());
        if (w.moveWay.m_82556_() < 1.0E-6) {
            w.moveWay = UkakuFire.horizontal(player.m_20154_());
        }
        w.moveReady[slot] = now + ArmRules.cooldown(ability, PACE);
        w.safeUntil = now + ability.total + SAFE_AFTER;
        // 3.1.8: a Tendril Swing tells every client where it bit (so its tendril is drawn to it).
        int bound = ability == Arm.Ability.SWING && w.anchor != null ? com.hex.kagune.control.quinque.WhipPath.packAnchor(
            w.anchor.f_82479_ - player.m_20185_(), w.anchor.f_82480_ - player.m_20186_(), w.anchor.f_82481_ - player.m_20189_()) : 0;
        ControlNetwork.broadcast(player, new S2CArmSwing(player.m_19879_(), ArmRules.abilityCode(ability), now, bound));
        Vec3 at = hand(player);
        switch (ability) {
            case BIND -> UkakuFire.sound(level, at, "hexkagune:kagune_whip", 0.9F, 0.8F);
            case FLAY -> UkakuFire.sound(level, at, "hexkagune:sfx.writhe", 0.8F, 1.1F);
            case JAVELIN -> UkakuFire.sound(level, at, "hexkagune:sfx.crystal_form", 0.9F, 0.8F);
            case PIERCE -> UkakuFire.sound(level, at, "hexkagune:sfx.windup_clash", 0.7F, 1.2F);
            case QUAKE -> UkakuFire.sound(level, at, "hexkagune:kagune_strain", 0.9F, 0.7F);
            case WHIRL -> UkakuFire.sound(level, at, "hexkagune:sfx.whoosh", 0.9F, 0.7F);
            case LEAP -> UkakuFire.sound(level, at, "hexkagune:sfx.step_heavy", 0.9F, 0.8F);
            case CLEAVE -> UkakuFire.sound(level, at, "hexkagune:sfx.windup_clash", 0.8F, 0.8F);
            case SWING -> UkakuFire.sound(level, at, "hexkagune:kagune_whip", 1.0F, 1.1F);
            case FAULT -> UkakuFire.sound(level, at, "hexkagune:kagune_strain", 1.0F, 0.6F);
            case GUARD -> UkakuFire.sound(level, at, "hexkagune:sfx.blade_arc", 0.7F, 0.7F);
        }
    }

    /** Its move under way, a tick on: its feet, its blows as they come due, over at its end. */
    static void advance(ServerPlayer player, ServerLevel level, Wield w, long now) {
        Arm.Ability ability = w.move;
        long age = now - w.moveAt;
        Entity found = w.moveTarget != 0 ? level.m_6815_(w.moveTarget) : null;
        LivingEntity target = found instanceof LivingEntity body && body.m_6084_() ? body : null;
        switch (ability) {
            case BIND, LEAP -> {
                if (target != null) {
                    Vec3 to = UkakuFire.horizontal(target.m_20182_().m_82546_(player.m_20182_()));
                    if (to.m_82556_() > 1.0E-6) {
                        w.moveWay = to;
                    }
                }
            }
            case PIERCE, FAULT, SWING -> {
                // A charge goes straight, as it set out; 3.1.8: so does the line the ground breaks along, and the swing to its latch.
            }
            default -> {
                Vec3 look = UkakuFire.horizontal(player.m_20154_());
                if (look.m_82556_() > 1.0E-6) {
                    w.moveWay = look; // turned where they look
                }
            }
        }
        feet(player, w, ability, target, age);
        while (w.move == ability && w.moveHit < ability.hits.length && age >= ability.hits[w.moveHit]) {
            int index = w.moveHit++;
            blow(player, w, ability, index, target, level, now);
        }
        if (w.move == ability && ability == Arm.Ability.PIERCE && age >= ability.hits[0] && age < ability.hits[1]) {
            through(player, w, level, now);
        }
        if (w.move == ability && ability == Arm.Ability.BIND && w.bound && age < ability.hits[1]) {
            drag(player, w, target);
        }
        if (w.move == ability && (age >= ability.total || ability == Arm.Ability.BIND && !w.bound && age >= ability.hits[0] + 6L
            || ability == Arm.Ability.SWING && w.arrived)) {
            finish(player, w, now);
        }
    }

    /** How they move through {@code ability}: a hop, a leap, a charge, a slow advance, or planted (their own keys still steer them). */
    private static void feet(ServerPlayer player, Wield w, Arm.Ability ability, LivingEntity target, long age) {
        Vec3 velocity = player.m_20184_();
        Vec3 way = w.moveWay;
        switch (ability) {
            case PIERCE -> {
                if (age >= ability.hits[0] && age < ability.hits[1]) {
                    double speed = 1.0;
                    player.m_20256_(new Vec3(way.f_82479_ * speed, Math.min(velocity.f_82480_, 0.05), way.f_82481_ * speed));
                    player.f_19864_ = true;
                } else if (age == ability.hits[1]) {
                    player.m_20256_(new Vec3(velocity.f_82479_ * 0.2, velocity.f_82480_, velocity.f_82481_ * 0.2));
                    player.f_19864_ = true;
                }
            }
            case QUAKE -> {
                if (age == 3L && player.m_20096_()) {
                    double gap = target == null ? 0.0 : Math.sqrt(target.m_20280_(player));
                    double forward = Math.min(0.35, Math.max(0.0, gap - 1.8) / 12.0);
                    player.m_20256_(new Vec3(way.f_82479_ * forward, 0.55, way.f_82481_ * forward));
                    player.f_19864_ = true;
                } else if (age == ability.hits[0] - 3L && !player.m_20096_()) {
                    player.m_20256_(new Vec3(velocity.f_82479_ * 0.5, -0.9, velocity.f_82481_ * 0.5)); // brought down hard
                    player.f_19864_ = true;
                }
            }
            case LEAP -> {
                int up = ability.hits[0];
                int down = ability.hits[1];
                if (age >= up && age < down) {
                    // At its foe (where they are now), or six blocks on along their look, falling on it as the cut comes down.
                    Vec3 rel = target == null ? way.m_82490_(Math.max(0.5, 6.0 * (down - age) / (double)(down - up))) : target.m_20182_().m_82546_(player.m_20182_());
                    double gap = Math.sqrt(rel.f_82479_ * rel.f_82479_ + rel.f_82481_ * rel.f_82481_);
                    double left = Math.max(1.0, down - age);
                    double speed = Math.min(1.1, Math.max(0.0, gap - (target == null ? 0.0 : 1.2)) / left);
                    Vec3 flat = gap < 1.0E-3 ? Vec3.f_82478_ : new Vec3(rel.f_82479_ / gap, 0.0, rel.f_82481_ / gap);
                    double vy = age == up ? 0.62 : age >= down - 2 ? Math.min(velocity.f_82480_, -0.8) : velocity.f_82480_;
                    player.m_20256_(new Vec3(flat.f_82479_ * speed, vy, flat.f_82481_ * speed));
                    player.f_19864_ = true;
                }
            }
            case SWING -> {
                // 3.1.8: hauled along the whip to where it bit, up and over what is in the way; stopped short of it.
                if (age >= ability.hits[0] && w.anchor != null) {
                    Vec3 to = w.anchor.m_82546_(player.m_20182_().m_82520_(0.0, 0.9, 0.0));
                    double gap = Math.sqrt(to.m_82556_());
                    if (gap < 1.8) {
                        player.m_20256_(velocity.m_82490_(0.35));
                        player.f_19864_ = true;
                        w.arrived = true;
                    } else {
                        double speed = Math.min(1.35, 0.35 + gap * 0.18);
                        player.m_20256_(new Vec3(to.f_82479_ / gap * speed, to.f_82480_ / gap * speed + 0.12, to.f_82481_ / gap * speed));
                        player.f_19864_ = true;
                    }
                }
            }
            case WHIRL -> {
                if (age >= 6L && age < ability.total - 6L && player.m_20096_()) {
                    double speed = 0.12;
                    player.m_20256_(new Vec3(velocity.f_82479_ * 0.6 + way.f_82479_ * speed, velocity.f_82480_, velocity.f_82481_ * 0.6 + way.f_82481_ * speed));
                    player.f_19864_ = true;
                }
            }
            default -> {
            }
        }
    }

    /** Its {@code index}th blow of {@code ability} comes due. */
    private static void blow(ServerPlayer player, Wield w, Arm.Ability ability, int index, LivingEntity target, ServerLevel level, long now) {
        float dealt = ArmRules.damage(w.arm, ability, w.master) * POWER;
        double edge = w.master ? 1.12 : 1.0;
        Vec3 from = player.m_20182_();
        switch (ability) {
            case BIND -> {
                if (index == 0) {
                    // Thrown round them: caught if they are still within its length and in sight; a sting as it bites in.
                    double length = ability.farthest * edge + 1.5;
                    if (target != null && !AgentBrain.untouchable(target) && target.m_20280_(player) <= length * length
                        && player.m_142582_(target)) {
                        w.bound = true;
                        ControlNetwork.broadcast(player, new S2CArmSwing(player.m_19879_(), ArmRules.abilityCode(ability), w.moveAt, target.m_19879_()));
                        hit(player, w, target, dealt * 0.2F, Arm.Strike.SLASH, from, w.moveWay, 0.0, 0.0, level, now);
                        UkakuFire.sound(level, GhoulNpcBrain.centre(target), "hexkagune:kagune_grab", 1.0F, 0.9F);
                        UkakuFire.sound(level, GhoulNpcBrain.centre(target), "hexkagune:sfx.grab", 0.8F, 1.0F);
                    } else {
                        UkakuFire.sound(level, hand(player), "hexkagune:kagune_whip", 0.6F, 1.3F); // it cracks on empty air
                    }
                } else if (w.bound && target != null) {
                    // Dragged in and slammed down.
                    Vec3 at = GhoulNpcBrain.centre(target);
                    hit(player, w, target, dealt, Arm.Strike.CRUSH, from, w.moveWay, 0.15, 0.0, level, now);
                    Vec3 velocity = target.m_20184_();
                    target.m_20256_(new Vec3(velocity.f_82479_ * 0.2, -1.0, velocity.f_82481_ * 0.2));
                    target.f_19864_ = true;
                    KaguneClash.quake(player, target.m_20182_(), 1.2F);
                    level.m_8767_(ParticleTypes.f_123759_, at.f_82479_, target.m_20186_() + 0.1, at.f_82481_, 10, 0.5, 0.05, 0.5, 0.03);
                    UkakuFire.sound(level, at, "hexkagune:kagune_slam", 1.0F, 0.85F);
                }
            }
            case FLAY -> {
                boolean any = false;
                for (LivingEntity body : foes(player, level, w.moveTarget, ability.farthest * edge + 1.5)) {
                    if (AgentArms.inArc(player, body, w.moveWay, ability.farthest * edge, 120.0)) {
                        any |= hit(player, w, body, body == target ? dealt : dealt * 0.75F, Arm.Strike.SLASH, from, w.moveWay, 0.12, 0.0, level, now);
                    }
                }
                UkakuFire.sound(level, hand(player), "hexkagune:kagune_whip", 0.7F, 1.0F + 0.08F * index);
                if (any) {
                    UkakuFire.sound(level, target != null ? GhoulNpcBrain.centre(target) : hand(player), "hexkagune:sfx.slash_spray", 0.6F, 1.1F);
                }
            }
            case JAVELIN -> {
                // Thrown: a javelin of crystal (three shards in a line, the head the hardest), at where its target will be, else along their look.
                Vec3 look = player.m_20154_();
                Vec3 side = new Vec3(-look.f_82481_, 0.0, look.f_82479_);
                if (side.m_82556_() > 1.0E-6) {
                    side = side.m_82541_();
                }
                Vec3 hand = player.m_146892_().m_82520_(0.0, -0.1, 0.0).m_82549_(side.m_82490_(0.3));
                double speed = 3.2;
                Vec3 way = look;
                if (target != null) {
                    Vec3 aim = GhoulNpcBrain.centre(target);
                    double flight = aim.m_82554_(hand) / speed;
                    Vec3 lead = target.m_20184_();
                    aim = aim.m_82520_(lead.f_82479_ * flight, 0.0, lead.f_82481_ * flight);
                    way = aim.m_82546_(hand);
                }
                if (way.m_82556_() < 1.0E-6) {
                    way = w.moveWay;
                }
                way = way.m_82541_();
                for (int i = 0; i < 3; i++) {
                    CrystalShots.fire(player, level, hand.m_82549_(way.m_82490_(-0.55 * i)), way, speed, 30.0, i == 0 ? dealt : dealt * 0.25F,
                        body -> mayHit(player, body, w.moveTarget));
                }
                UkakuFire.sound(level, hand, "hexkagune:shard_launch", 1.0F, 0.8F);
                UkakuFire.sound(level, hand, "hexkagune:sfx.spike_throw", 0.9F, 0.9F);
            }
            case PIERCE -> {
                if (index == 0) {
                    UkakuFire.sound(level, from, "hexkagune:sfx.dash", 0.9F, 0.9F);
                }
            }
            case QUAKE -> {
                // The hammer brought down on the ground: everyone near thrown up, the nearer the harder.
                Vec3 ground = from.m_82549_(w.moveWay.m_82490_(1.4));
                double radius = ability.farthest * edge;
                for (LivingEntity body : foes(player, level, w.moveTarget, radius + 1.5)) {
                    Vec3 off = body.m_20182_().m_82546_(ground);
                    double gap = Math.sqrt(off.f_82479_ * off.f_82479_ + off.f_82481_ * off.f_82481_);
                    if (gap <= radius + body.m_20205_() * 0.5 && Math.abs(body.m_20186_() - player.m_20186_()) <= 2.5) {
                        double near = 1.0 - 0.4 * Math.min(1.0, gap / radius);
                        hit(player, w, body, (float)(dealt * near), Arm.Strike.CRUSH, ground, w.moveWay, 0.55 * near, 0.6 * near, level, now);
                    }
                }
                KaguneClash.quake(player, ground, 1.6F);
                level.m_8767_(ParticleTypes.f_123759_, ground.f_82479_, ground.f_82480_ + 0.1, ground.f_82481_, 24, radius * 0.4, 0.05, radius * 0.4, 0.06);
                level.m_8767_(ParticleTypes.f_123762_, ground.f_82479_, ground.f_82480_ + 0.2, ground.f_82481_, 12, 0.6, 0.1, 0.6, 0.02);
                UkakuFire.sound(level, ground, "hexkagune:kagune_slam", 1.0F, 0.7F);
                UkakuFire.sound(level, ground, "hexkagune:sfx.shockwave", 0.9F, 0.75F);
            }
            case WHIRL -> {
                double radius = ability.farthest * edge;
                for (LivingEntity body : foes(player, level, w.moveTarget, radius + 1.5)) {
                    if (AgentArms.inArc(player, body, w.moveWay, radius, 360.0)) {
                        hit(player, w, body, dealt, Arm.Strike.CRUSH, from, w.moveWay, 0.45, 0.15, level, now);
                    }
                }
                UkakuFire.sound(level, hand(player), "hexkagune:sfx.punch_swing_heavy", 0.9F, 0.7F + 0.08F * index);
            }
            case LEAP -> {
                if (index == 1) {
                    // Down onto them where it lands: a cut and the ground split round it.
                    Vec3 at = from.m_82549_(w.moveWay.m_82490_(0.8));
                    for (LivingEntity body : foes(player, level, w.moveTarget, 4.0)) {
                        Vec3 off = body.m_20182_().m_82546_(at);
                        double gap = Math.sqrt(off.f_82479_ * off.f_82479_ + off.f_82481_ * off.f_82481_);
                        if (gap <= 2.6 + body.m_20205_() * 0.5 && Math.abs(body.m_20186_() - player.m_20186_()) <= 2.5) {
                            hit(player, w, body, body == target ? dealt : dealt * 0.6F, Arm.Strike.SLASH, at, w.moveWay, 0.4, 0.25, level, now);
                        }
                    }
                    KaguneClash.quake(player, at, 1.3F);
                    level.m_8767_(ParticleTypes.f_123759_, at.f_82479_, at.f_82480_ + 0.1, at.f_82481_, 14, 0.8, 0.05, 0.8, 0.04);
                    UkakuFire.sound(level, at, "hexkagune:sfx.blade_heavy", 1.0F, 0.75F);
                    UkakuFire.sound(level, at, "hexkagune:sfx.ground", 0.9F, 0.9F);
                } else {
                    UkakuFire.sound(level, from, "hexkagune:sfx.dash", 0.8F, 0.75F);
                }
            }
            case SWING -> {
                // 3.1.8: it bites into the block.
                if (w.anchor != null) {
                    UkakuFire.sound(level, w.anchor, "block.stone.hit", 1.0F, 0.8F);
                    UkakuFire.sound(level, w.anchor, "hexkagune:kagune_whip", 0.8F, 1.3F);
                }
            }
            case FAULT -> {
                // 3.1.8: the ground breaks open a stretch further on at each blow; whatever stands on it is struck and thrown up.
                double along = (1.6 + 1.6 * index) * edge;
                Vec3 point = from.m_82549_(w.moveWay.m_82490_(along));
                net.minecraft.core.BlockPos ground = null;
                for (int dy = 1; dy >= -3 && ground == null; dy--) {
                    net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.m_274561_(point.f_82479_, point.f_82480_ - 0.5 + dy, point.f_82481_);
                    if (!level.m_8055_(pos).m_60795_()) {
                        ground = pos;
                    }
                }
                if (ground == null) {
                    return; // over a drop: the break runs out
                }
                level.m_46796_(2001, ground, net.minecraft.world.level.block.Block.m_49956_(level.m_8055_(ground)));
                UkakuFire.sound(level, point, index == 0 ? "entity.generic.explode" : "block.stone.break", index == 0 ? 0.8F : 0.9F,
                    0.7F + 0.05F * index);
                for (LivingEntity body : foes(player, level, 0, along + 2.5)) {
                    double dx = body.m_20185_() - point.f_82479_;
                    double dz = body.m_20189_() - point.f_82481_;
                    if (dx * dx + dz * dz <= 1.6 * 1.6 && Math.abs(body.m_20186_() - (ground.m_123342_() + 1.0)) <= 2.5
                        && w.moveStruck.add(body.m_19879_())) {
                        hit(player, w, body, dealt, Arm.Strike.CRUSH, point.m_82546_(w.moveWay), w.moveWay, 0.15, 0.75, level, now);
                    }
                }
            }
            case GUARD -> {
                // 3.1.8: raised; Bone Wall's blows are what reach him (guarded / guardedHurt).
            }
            case CLEAVE -> {
                double reach = ability.farthest * edge;
                boolean any = false;
                for (LivingEntity body : foes(player, level, w.moveTarget, reach + 1.5)) {
                    if (AgentArms.inArc(player, body, w.moveWay, reach, 200.0)) {
                        float cut = body == target ? dealt : dealt * 0.8F;
                        if (hit(player, w, body, cut, Arm.Strike.SLASH, from, w.moveWay, 0.55, 0.1, level, now)) {
                            any = true;
                            // Left bleeding: a twelfth of the cut a second, four seconds.
                            w.bleeds.add(new long[] {body.m_19879_(), now + 80L, Float.floatToIntBits(Math.max(0.5F, cut / 12.0F))});
                        }
                    }
                }
                UkakuFire.sound(level, hand(player), "hexkagune:sfx.blade_heavy", 1.0F, 0.6F);
                if (any) {
                    UkakuFire.sound(level, target != null ? GhoulNpcBrain.centre(target) : hand(player), "hexkagune:sfx.blood_spray", 0.9F, 0.9F);
                }
            }
        }
    }

    /** The Bind: its catch dragged in to them along the whip, lifted a little off its feet. */
    private static void drag(ServerPlayer player, Wield w, LivingEntity target) {
        if (target == null || target.m_20280_(player) > 14.0 * 14.0) {
            w.bound = false;
            ControlNetwork.broadcast(player, new S2CArmSwing(player.m_19879_(), ArmRules.abilityCode(w.move), w.moveAt, 0));
            return;
        }
        Vec3 to = player.m_20182_().m_82549_(w.moveWay.m_82490_(1.6 + target.m_20205_() * 0.5)).m_82546_(target.m_20182_());
        double length = Math.sqrt(to.m_82556_());
        Vec3 pull = length < 0.2 ? Vec3.f_82478_ : to.m_82490_(Math.min(0.75, length * 0.3) / length);
        double resist = Math.max(0.0, Math.min(1.0, target.m_21133_(net.minecraft.world.entity.ai.attributes.Attributes.f_22278_))) * 0.5;
        target.m_20256_(new Vec3(pull.f_82479_ * (1.0 - resist), Math.max(target.m_20184_().f_82480_, length > 1.0 ? 0.12 : 0.0), pull.f_82481_ * (1.0 - resist)));
        target.f_19864_ = true;
        target.f_19789_ = 0.0F;
    }

    /** The Piercing Charge, a tick on: whoever it runs into (once each) is run through and thrown along its way. */
    private static void through(ServerPlayer player, Wield w, ServerLevel level, long now) {
        float dealt = ArmRules.damage(w.arm, Arm.Ability.PIERCE, w.master) * POWER;
        Vec3 tip = player.m_20182_().m_82549_(w.moveWay.m_82490_(1.2));
        for (LivingEntity body : foes(player, level, w.moveTarget, 3.0)) {
            if (w.moveStruck.contains(body.m_19879_())) {
                continue;
            }
            Vec3 off = body.m_20182_().m_82546_(tip);
            double gap = Math.sqrt(off.f_82479_ * off.f_82479_ + off.f_82481_ * off.f_82481_);
            if (gap <= 1.0 + body.m_20205_() * 0.5 && Math.abs(body.m_20186_() - player.m_20186_()) <= 2.0) {
                w.moveStruck.add(body.m_19879_());
                Vec3 ahead = body.m_20182_().m_82546_(w.moveWay.m_82490_(2.0));
                if (hit(player, w, body, dealt, Arm.Strike.STAB, ahead, w.moveWay, 0.9, 0.3, level, now)) {
                    UkakuFire.sound(level, GhoulNpcBrain.centre(body), "hexkagune:sfx.stab", 1.0F, 0.85F);
                }
            }
        }
    }

    /** Its move done (or broken off): the whip let go, back to its string. */
    static void finish(ServerPlayer player, Wield w, long now) {
        w.move = null;
        w.bound = false;
        w.moveStruck.clear();
        w.readyAt = Math.max(w.readyAt, now + 4L);
        ControlNetwork.broadcast(player, new S2CArmSwing(player.m_19879_(), -1, now, 0));
    }

    // ---------------------------------------------------------------- whom it hits

    /** Everyone within {@code range} of {@code player} its blows may take ({@link #mayHit}). */
    static List<LivingEntity> foes(ServerPlayer player, ServerLevel level, int aimed, double range) {
        return level.m_6443_(LivingEntity.class, player.m_20191_().m_82400_(range), body -> body != player && body.m_6084_() && mayHit(player, body, aimed));
    }

    /**
     * True if {@code player}'s weapon takes {@code body}: whoever they aim at ({@code aimed}, an entity id), and round them monsters, wild
     * ghouls, flesh creatures and anything going for them; never another player, a companion, a child, a villager or a tame animal they did
     * not aim at.
     */
    /** 4.1.37: a Bind that holds {@code id} lets go of it (no drag, no slam; the move ends as one that missed). */
    static void unbind(ServerPlayer player, int id) {
        Wield w = WIELDS.isEmpty() ? null : WIELDS.get(player.m_20148_());
        if (w != null && w.bound && w.moveTarget == id) {
            w.bound = false;
        }
    }

    static boolean mayHit(ServerPlayer player, LivingEntity body, int aimed) {
        if (body == player || !body.m_6084_() || AgentBrain.untouchable(body)) {
            return false;
        }
        if (aimed != 0 && body.m_19879_() == aimed) {
            return !(body instanceof Player other && (other.m_7500_() || other.m_5833_()));
        }
        if (body instanceof Player || body instanceof EtoNpc || body instanceof ChildNpc) {
            return false;
        }
        if (body instanceof net.minecraft.world.entity.monster.Enemy || body instanceof GhoulNpc || body instanceof FleshThrall) {
            return true;
        }
        return body instanceof Mob mob && mob.m_5448_() == player;
    }

    /**
     * Whom a move is aimed at: {@code crosshair} (an entity id the client sends: what is under the crosshair) if it is within {@code range}
     * and in sight; else whoever its blows may take nearest the line of their look (within about twenty degrees), or null.
     */
    static LivingEntity aim(ServerPlayer player, ServerLevel level, int crosshair, double range) {
        if (crosshair != 0 && level.m_6815_(crosshair) instanceof LivingEntity body && body.m_6084_() && body != player
            && !AgentBrain.untouchable(body) && body.m_20280_(player) <= (range + 1.0) * (range + 1.0) && player.m_142582_(body)) {
            return body;
        }
        Vec3 eye = player.m_146892_();
        Vec3 look = player.m_20154_();
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (LivingEntity body : foes(player, level, 0, range)) {
            Vec3 to = GhoulNpcBrain.centre(body).m_82546_(eye);
            double distance = to.m_82553_();
            if (distance < 1.0E-3 || distance > range + body.m_20205_()) {
                continue;
            }
            double cos = to.m_82526_(look) / distance;
            if (cos < 0.94 || !player.m_142582_(body)) {
                continue;
            }
            double score = distance * (2.0 - cos);
            if (score < bestScore) {
                bestScore = score;
                best = body;
            }
        }
        return best;
    }

    /**
     * One blow on {@code body}: {@code dealt} (harder on a ghoul, as the quinque it is held as cuts one, and a ghoul player's mending held
     * back), its wound, thrown away from {@code from}. True if it hurt.
     */
    static boolean hit(ServerPlayer player, Wield w, LivingEntity body, float dealt, int blow, Vec3 from, Vec3 fallback, double push, double lift,
                       ServerLevel level, long now) {
        Vec3 away = UkakuFire.horizontal(body.m_20182_().m_82546_(from));
        if (away.m_82556_() < 1.0E-6) {
            away = fallback.m_82556_() > 1.0E-6 ? fallback : UkakuFire.horizontal(player.m_20154_());
        }
        Quinque kind = w.arm.carried;
        boolean ghoul = QuinqueServer.ghoul(body, level);
        float amount = ghoul ? dealt * kind.ghoul : dealt;
        Vec3 centre = GhoulNpcBrain.centre(body);
        WoundHits.expect(body, AgentArms.wound(blow), centre.m_82546_(away.m_82490_(0.3)), away, 0.0);
        body.f_19802_ = 0;
        if (!body.m_6469_(player.m_269291_().m_269075_(player), amount)) {
            return false;
        }
        if (ghoul && body instanceof ServerPlayer other) {
            GhoulRegen.suppress(other, now + kind.suppress);
        }
        if (push > 0.0 || lift > 0.0) {
            double resist = Math.max(0.0, Math.min(1.0, body.m_21133_(net.minecraft.world.entity.ai.attributes.Attributes.f_22278_)));
            Vec3 velocity = body.m_20184_();
            double along = push * (1.0 - resist);
            body.m_20256_(new Vec3(velocity.f_82479_ * 0.3 + away.f_82479_ * along, Math.max(velocity.f_82480_, lift * (1.0 - resist * 0.5)),
                velocity.f_82481_ * 0.3 + away.f_82481_ * along));
            body.f_19864_ = true;
        }
        return true;
    }

    // ---------------------------------------------------------------- 3.1.8: the Bone Wall

    /** {@code player}'s Bone Wall {@code age} ticks up, or -1 when it isn't. */
    private static long guardAge(ServerPlayer player) {
        Wield w = WIELDS.isEmpty() ? null : WIELDS.get(player.m_20148_());
        if (w == null || w.move != Arm.Ability.GUARD || !(player.m_9236_() instanceof ServerLevel level)) {
            return -1L;
        }
        long age = level.m_46467_() - w.moveAt;
        return age >= 1L && age < Arm.Ability.GUARD.total - 3L ? age : -1L;
    }

    /** A blow in the Bone Wall's first ticks: parried outright, its striker staggered, his next blow the string's overhead cleave. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void guarded(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.m_9236_() instanceof ServerLevel level)) {
            return;
        }
        long age = guardAge(player);
        if (age < 0L || age > Arm.GUARD_PERFECT || !(event.getSource().m_7639_() instanceof LivingEntity striker) || striker == player) {
            return;
        }
        event.setCanceled(true);
        Wield w = WIELDS.get(player.m_20148_());
        long now = level.m_46467_();
        PaleStun.stun(striker, now, 24);
        Vec3 away = UkakuFire.horizontal(striker.m_20182_().m_82546_(player.m_20182_()));
        if (away.m_82556_() > 1.0E-6) {
            striker.m_20256_(striker.m_20184_().m_82549_(away.m_82541_().m_82490_(0.7)).m_82520_(0.0, 0.2, 0.0));
            striker.f_19864_ = true;
        }
        finish(player, w, now);
        // The answer: the string's last blow, next (ArmRules.next goes on from the one before it).
        w.step = w.arm.length() - 2;
        w.stepAt = now;
        w.readyAt = now;
        UkakuFire.sound(level, hand(player), "hexkagune:sfx.blade_heavy", 1.0F, 1.4F);
        player.m_5661_(Component.m_237113_("§cParried!"), true);
    }

    /** A blow on the Bone Wall after its first ticks: most of it stopped by the bone. */
    @SubscribeEvent
    public static void guardedHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getSource().m_7639_() != null && event.getSource().m_7639_() != player
            && guardAge(player) >= 0L) {
            event.setAmount(event.getAmount() * Arm.GUARD_TAKES);
            if (player.m_9236_() instanceof ServerLevel level) {
                UkakuFire.sound(level, hand(player), "hexkagune:sfx.blade_arc", 0.8F, 0.6F);
            }
        }
    }

    // ---------------------------------------------------------------- each tick

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || !(player.m_9236_() instanceof ServerLevel level)) {
            return;
        }
        try {
            long now = level.m_46467_();
            if (Math.floorMod(now + player.m_19879_(), 20L) == 0L) {
                fold(player, level);
            }
            Wield w = WIELDS.isEmpty() ? null : WIELDS.get(player.m_20148_());
            if (w == null) {
                return;
            }
            if (now <= w.safeUntil) {
                player.f_19789_ = 0.0F; // a leap's or a slam's fall forgiven
            }
            bleed(player, level, w, now);
            Arm held = ControlItems.armWeapon(player.m_21205_());
            if (held != w.arm || !player.m_6084_() || player.m_5833_()) {
                // Put away, or down: nothing more of it lands.
                if (w.move != null) {
                    finish(player, w, now);
                }
                w.striking = false;
                w.queued = false;
                if (w.bleeds.isEmpty() && now > w.safeUntil) {
                    WIELDS.remove(player.m_20148_());
                }
                return;
            }
            if (w.move != null) {
                advance(player, level, w, now);
                return;
            }
            if (w.striking && now >= w.landsAt) {
                land(player, level, w, now);
            }
            if (!w.striking && w.queued && now >= w.readyAt) {
                begin(player, level, w, w.queuedTarget, now);
            }
        } catch (RuntimeException failure) {
            com.hex.kagune.control.Safety.caught(failure, "a player's kagune weapon");
        }
    }

    /** Wounds its Cleave left bleed, a second at a time. */
    private static void bleed(ServerPlayer player, ServerLevel level, Wield w, long now) {
        if (w.bleeds.isEmpty() || now % 20L != 0L) {
            return;
        }
        Iterator<long[]> each = w.bleeds.iterator();
        while (each.hasNext()) {
            long[] bleed = each.next();
            Entity found = level.m_6815_((int)bleed[0]);
            if (now > bleed[1] || !(found instanceof LivingEntity body) || !body.m_6084_()) {
                each.remove();
                continue;
            }
            body.f_19802_ = 0;
            body.m_6469_(player.m_269291_().m_269075_(player), Float.intBitsToFloat((int)bleed[2]));
            UkakuFire.sound(level, GhoulNpcBrain.centre(body), "hexkagune:sfx.drip", 0.5F, 0.9F + 0.2F * (float)player.m_217043_().m_188500_());
        }
    }

    /** About where their weapon hand is. */
    private static Vec3 hand(LivingEntity body) {
        return body.m_20182_().m_82520_(0.0, body.m_20206_() * 0.6, 0.0);
    }
}
