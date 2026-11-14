package com.hex.kagune.control.server;

import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.RinkakuBlock;
import com.hex.kagune.control.RinkakuForms;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CRinkakuBlock;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.KaguneForm;
import com.hex.kagune.gameplay.KaguneRuntime;
import com.hex.kagune.gameplay.TendrilMode;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 2.0.42: Rinkaku's block on the right click ({@link RinkakuBlock} has the rules). The client says when the button goes down (a fresh
 * press, which may open the perfect window), every {@link RinkakuBlock#RESEND_TICKS} while it is held, and when it is let go; the block
 * is up while the tails are out and free (not mid-move, not holding anyone, not in the first kakuja, which fights on its own, nor the
 * final one, whose right click is its dash). Every client is told when it goes up or down, takes a blow, a perfect block or breaks
 * ({@link S2CRinkakuBlock}), and draws the tails and the body from that.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class RinkakuBlockServer {
    /** One player's block: up or not, when it went up and whether that press opened the perfect window, the weight on it, its cooldown. */
    private static final class Block {
        boolean up;
        long raisedAt;
        boolean fresh;
        long lastWindow = Long.MIN_VALUE / 2;
        long heldAt;
        float weight;
        long lastHit = Long.MIN_VALUE / 2;
        long lastCounted = Long.MIN_VALUE / 2;
        long lastFx = Long.MIN_VALUE / 2;
        long coolUntil;
    }

    private static final Map<UUID, Block> BLOCKS = new HashMap<>();
    /** What the client sends: let go, still held, pressed. */
    public static final int RELEASED = 0;
    public static final int HELD = 1;
    public static final int PRESSED = 2;

    private RinkakuBlockServer() {
    }

    /** The right click from {@code player}'s client: {@link #RELEASED}, {@link #HELD} or {@link #PRESSED}. */
    public static void input(ServerPlayer player, int state) {
        long now = player.m_9236_().m_46467_();
        Block block = BLOCKS.get(player.m_20148_());
        if (state == RELEASED) {
            if (block != null && block.up) {
                lower(player, block, now, S2CRinkakuBlock.DOWN);
            }
            return;
        }
        if (!can(player)) {
            if (block != null && block.up) {
                lower(player, block, now, S2CRinkakuBlock.DOWN);
            } else if (state == PRESSED && (PathServer.path(player) == KagunePath.RINKAKU || ukakuGuard(player))) {
                ControlNetwork.sendTo(player, new S2CRinkakuBlock(player.m_20148_(), S2CRinkakuBlock.REFUSED, 0));
            }
            return;
        }
        if (block == null) {
            block = new Block();
            BLOCKS.put(player.m_20148_(), block);
        }
        block.heldAt = now;
        if (block.up) {
            return;
        }
        if (now < block.coolUntil) {
            if (state == PRESSED) {
                ControlNetwork.sendTo(player, new S2CRinkakuBlock(player.m_20148_(), S2CRinkakuBlock.REFUSED, (int)(block.coolUntil - now)));
                player.m_5661_(Component.m_237115_("message.hexkagune.rinkaku_block.recovering"), true);
            }
            return;
        }
        block.up = true;
        block.raisedAt = now;
        block.fresh = state == PRESSED && RinkakuBlock.opensWindow(block.lastWindow, now);
        if (block.fresh) {
            block.lastWindow = now;
        }
        ControlNetwork.broadcast(player, new S2CRinkakuBlock(player.m_20148_(), S2CRinkakuBlock.UP, 0));
        if (player.m_9236_() instanceof ServerLevel level) {
            UkakuFire.sound(level, player.m_20182_().m_82520_(0.0, 1.2, 0.0), com.hex.kagune.control.RinkakuSounds.GRAB, 0.35F, 1.35F);
        }
    }

    /**
     * 4.1.2 (the owner: "Right click (M2) is now a protection like Rinkaku has, wings grow and protect the player ... just like rinkaku they
     * break after a bunch of hits, perfectly timed right clicks can do the same as Rinkaku and stagger"): an Ukaku with the wings out
     * in the fire (twin or Rabbit Guise's one) guards with them, by Rinkaku's rules.
     */
    static boolean ukakuGuard(ServerPlayer player) {
        return PathServer.path(player) == KagunePath.UKAKU && com.hex.kagune.control.ukaku.UkakuRules.wingGuard(KaguneProfile.mastery(player),
            UkakuServer.form(player), UkakuServer.crystalMode(player));
    }

    /** True while {@code player} can hold the block up: a Rinkaku with the tails out and free (4.1.2: or an Ukaku's wings in the fire). */
    static boolean can(ServerPlayer player) {
        if (!player.m_6084_() || player.m_5833_() || !KaguneProfile.isActive(player)) {
            return false;
        }
        if (PathServer.path(player) == KagunePath.UKAKU) {
            if (!ukakuGuard(player) || PaleStun.stunned(player) || PaleWitness.frozen(player) || com.hex.kagune.control.DragonLock.locked(player)) {
                return false;
            }
            KaguneRuntime.State wings = KaguneRuntime.get(player.m_20148_());
            return wings != null && wings.mode == TendrilMode.IDLE;
        }
        if (PathServer.path(player) != KagunePath.RINKAKU) {
            return false;
        }
        KaguneForm form = KaguneProfile.form(player);
        if (form == null || form == KaguneForm.FULL_KAKUJA || RinkakuForms.autonomous(form)) {
            return false;
        }
        if (PaleStun.stunned(player) || PaleWitness.frozen(player) || com.hex.kagune.control.DragonLock.locked(player)) {
            return false;
        }
        KaguneRuntime.State state = KaguneRuntime.get(player.m_20148_());
        // 3.1.5: nor while a tail is out impaling (ImpaleServer); 3.1.6: nor while the tails hold a chunk (HurlServer).
        return state != null && state.mode == TendrilMode.IDLE && !state.hasManualHeldEntities() && !ImpaleServer.active(player)
            && !HurlServer.holding(player);
    }

    /** True while {@code player}'s block is up. */
    public static boolean up(ServerPlayer player) {
        Block block = BLOCKS.get(player.m_20148_());
        return block != null && block.up;
    }

    private static void lower(ServerPlayer player, Block block, long now, int event) {
        block.up = false;
        block.fresh = false;
        ControlNetwork.broadcast(player, new S2CRinkakuBlock(player.m_20148_(), event,
            event == S2CRinkakuBlock.BREAK ? (int)Math.max(0L, block.coolUntil - now) : 0));
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || BLOCKS.isEmpty()) {
            return;
        }
        Block block = BLOCKS.get(player.m_20148_());
        if (block == null) {
            return;
        }
        long now = player.m_9236_().m_46467_();
        block.weight = RinkakuBlock.settle(block.weight, now - block.lastHit);
        if (!block.up) {
            if (block.weight <= 0.0F && now >= block.coolUntil) {
                BLOCKS.remove(player.m_20148_());
            }
            return;
        }
        if (!can(player) || now - block.heldAt > RinkakuBlock.HELD_TIMEOUT || now < block.heldAt) {
            lower(player, block, now, S2CRinkakuBlock.DOWN);
            return;
        }
        // Guard up: a steady, careful walk.
        player.m_147207_(new MobEffectInstance(MobEffects.f_19597_, 3, RinkakuBlock.SLOW_AMPLIFIER, false, false, false), null);
    }

    /**
     * Blows at him while the block is up: stopped whole when they come from in front and have something behind them (a body, a shot, a
     * blast); a perfect block staggers the one who struck. Ahead of the rest (a stunned striker is already cancelled by
     * {@link PaleStun}).
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void attacked(LivingAttackEvent event) {
        if (BLOCKS.isEmpty() || !(event.getEntity() instanceof ServerPlayer player) || !(event.getAmount() > 0.0F)) {
            return;
        }
        Block block = BLOCKS.get(player.m_20148_());
        if (block == null || !block.up || !can(player)) {
            return;
        }
        DamageSource source = event.getSource();
        Entity attacker = source.m_7639_();
        Entity direct = source.m_7640_();
        if (attacker == player || direct == player) {
            return;
        }
        Vec3 from = source.m_7270_();
        if (from == null && direct != null) {
            from = direct.m_20182_();
        }
        if (from == null && attacker != null) {
            from = attacker.m_20182_();
        }
        if (from == null) {
            return; // nothing behind it (a fall, fire, hunger, the void): it lands
        }
        Vec3 chest = player.m_20182_().m_82520_(0.0, player.m_20206_() * 0.6, 0.0);
        Vec3 look = player.m_20154_();
        Vec3 toward = from.m_82546_(chest);
        if (!RinkakuBlock.covers(new double[] {look.f_82479_, look.f_82480_, look.f_82481_},
            new double[] {toward.f_82479_, toward.f_82480_, toward.f_82481_})) {
            return; // from behind: the tails are in front
        }
        event.setCanceled(true);
        long now = player.m_9236_().m_46467_();
        Vec3 flat = new Vec3(toward.f_82479_, 0.0, toward.f_82481_);
        Vec3 way = flat.m_82556_() < 1.0E-6 ? new Vec3(look.f_82479_, 0.0, look.f_82481_) : flat;
        way = way.m_82556_() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : way.m_82541_();
        Vec3 at = chest.m_82549_(way.m_82490_(0.7));
        if (RinkakuBlock.perfect(block.raisedAt, block.fresh, now)) {
            perfect(player, block, attacker instanceof LivingEntity striker ? striker : null, at, way, now);
            return;
        }
        if (!RinkakuBlock.counts(block.lastCounted, now)) {
            return; // the same flurry: stopped, already weighed
        }
        block.lastCounted = now;
        block.lastHit = now;
        block.weight += RinkakuBlock.weight(event.getAmount());
        if (RinkakuBlock.breaks(block.weight)) {
            broken(player, block, at, way, now);
            return;
        }
        push(player, way, RinkakuBlock.BLOCK_PUSH, 0.04);
        ControlNetwork.broadcast(player, new S2CRinkakuBlock(player.m_20148_(), S2CRinkakuBlock.HIT, 0));
        if (now - block.lastFx >= 2 && player.m_9236_() instanceof ServerLevel level) {
            block.lastFx = now;
            KaguneClash.burst(player, at, way, 0.55F, 0.6F);
            UkakuFire.sound(level, at, com.hex.kagune.control.RinkakuSounds.GUARD, 0.9F, 0.9F);
        }
    }

    /** A perfect block: an impact, no push, and the striker (when close) staggered and pushed off. */
    private static void perfect(ServerPlayer player, Block block, LivingEntity striker, Vec3 at, Vec3 way, long now) {
        block.weight = Math.max(0.0F, block.weight - 1.0F);
        ControlNetwork.broadcast(player, new S2CRinkakuBlock(player.m_20148_(), S2CRinkakuBlock.PERFECT, 0));
        if (now - block.lastFx >= 1) {
            block.lastFx = now;
            KaguneClash.burst(player, at, way, 1.6F, 1.0F);
            if (player.m_9236_() instanceof ServerLevel level) {
                UkakuFire.sound(level, at, "hexkagune:sfx.clash_big", 1.0F, 1.1F);
            }
        }
        if (striker != null && striker != player && striker.m_6084_()
            && striker.m_20280_(player) <= RinkakuBlock.STAGGER_RANGE * RinkakuBlock.STAGGER_RANGE) {
            if (PaleStun.stun(striker, now, RinkakuBlock.STAGGER_TICKS)) {
                Vec3 off = striker.m_20182_().m_82546_(player.m_20182_());
                Vec3 flat = new Vec3(off.f_82479_, 0.0, off.f_82481_);
                push(striker, flat.m_82556_() < 1.0E-6 ? way : flat.m_82541_().m_82490_(-1.0), RinkakuBlock.STAGGER_PUSH, 0.18);
            }
        }
    }

    /** Too much: the guard breaks, the tails are thrown back, he is pushed back hard and the block cools down. */
    private static void broken(ServerPlayer player, Block block, Vec3 at, Vec3 way, long now) {
        block.weight = 0.0F;
        block.coolUntil = now + Cooldowns.of(player, (long)RinkakuBlock.BREAK_COOLDOWN);
        lower(player, block, now, S2CRinkakuBlock.BREAK);
        push(player, way, RinkakuBlock.BREAK_PUSH, 0.22);
        KaguneClash.burst(player, at, way, 1.1F, 0.9F);
        if (player.m_9236_() instanceof ServerLevel level) {
            UkakuFire.sound(level, at, com.hex.kagune.control.RinkakuSounds.GUARD_BREAK, 1.0F, 0.8F);
        }
        player.m_5661_(Component.m_237115_("message.hexkagune.rinkaku_block.broken"), true);
    }

    /**
     * {@code body} pushed away from where the blow came from ({@code toward}: from it toward the blow, unit, level) by {@code strength},
     * lifted a little.
     */
    private static void push(LivingEntity body, Vec3 toward, double strength, double lift) {
        Vec3 motion = body.m_20184_();
        body.m_20256_(new Vec3(motion.f_82479_ * 0.4 - toward.f_82479_ * strength, Math.max(motion.f_82480_, lift),
            motion.f_82481_ * 0.4 - toward.f_82481_ * strength));
        body.f_19864_ = true;
    }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof ServerPlayer subject && up(subject)) {
            ControlNetwork.sendTo(viewer, new S2CRinkakuBlock(subject.m_20148_(), S2CRinkakuBlock.UP, 0));
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        BLOCKS.remove(event.getEntity().m_20148_());
    }
}
