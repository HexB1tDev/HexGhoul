package com.hex.kagune.control.server;

import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.RinkakuForms;
import com.hex.kagune.control.RinkakuImpale;
import com.hex.kagune.control.RinkakuRoot;
import com.hex.kagune.control.RinkakuUnlocks;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CImpale;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.KaguneForm;
import com.hex.kagune.gameplay.KaguneRuntime;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.sound.HexKaguneSounds;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 3.1.5: Rinkaku's Impale on the server ({@link RinkakuImpale}). H pressed with no one held (the client's own hold: pressed, a ping every
 * few ticks, let go; the original's crush only runs with someone held, {@code UkakuGates.crush}) sends one random free tail, never a
 * centipede leg, out from over its shoulder along his look; held, it keeps going. Whatever its point runs through is hurt, bled and
 * carried on it ({@link #carry}); a wall stops it and pins them there. Let go, at its reach for long enough, or {@link RinkakuImpale#MOST_OUT}
 * ticks out, it tears back out of them (they drop where they are) and comes home. Every client near him is told every tick
 * ({@link S2CImpale}) and draws it ({@code ImpaleClient}).
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class ImpaleServer {
    private ImpaleServer() {
    }

    /** One tail out impaling. */
    private static final class Spear {
        int limb;
        long started;
        long heldUntil;
        boolean released;
        double length;
        double reach;
        Vec3 way;
        boolean stopped;
        boolean pinned;
        long spentAt = -1L;
        long backAt = -1L;
        /** Ids of the bodies it carries, the first caught first. */
        final List<Integer> carried = new ArrayList<>();
        /** Every body it has run through (once each). */
        final Set<Integer> struck = new HashSet<>();
    }

    private static final Map<UUID, Spear> SPEARS = WorldTimes.kept();
    private static final Map<UUID, Long> READY_AT = WorldTimes.map();

    /** True while one of {@code player}'s tails is out impaling. */
    public static boolean active(ServerPlayer player) {
        return SPEARS.containsKey(player.m_20148_());
    }

    /** True if {@code body} has been run through by {@code player}'s Impale under way (its hits are stabs, {@code RinkakuWounds}). */
    static boolean ran(ServerPlayer player, LivingEntity body) {
        Spear spear = SPEARS.get(player.m_20148_());
        return spear != null && spear.struck.contains(body.m_19879_());
    }

    /** True if H is the original's crush for {@code player} now: one of his tails holds someone (else it is the Impale). */
    static boolean crushes(ServerPlayer player) {
        KaguneRuntime.State state = KaguneRuntime.get(player.m_20148_());
        return state != null && state.selectedHeldEntityId() != 0;
    }

    /** H from {@code player}'s client: {@code phase} 2 pressed, 1 still held, 0 let go. */
    static void input(ServerPlayer player, int phase) {
        Spear spear = SPEARS.get(player.m_20148_());
        long now = player.m_9236_().m_46467_();
        if (phase == 0) {
            if (spear != null) {
                spear.released = true;
            }
            return;
        }
        if (spear != null) {
            if (phase == 1 && !spear.released) {
                spear.heldUntil = now + RinkakuImpale.HELD;
            }
            return;
        }
        if (phase == 2) {
            start(player, now);
        }
    }

    /** True if {@code player} can have a tail out impaling: a Rinkaku ghoul with the kagune out, alive, not in the first kakuja. */
    static boolean allowed(ServerPlayer player) {
        if (!player.m_6084_() || player.m_5833_() || !KaguneProfile.isActive(player) || PathServer.path(player) != KagunePath.RINKAKU
            || UkakuServer.isUkaku(player) || PaleServer.isPale(player) || GraftServer.isGraft(player)) {
            return false;
        }
        KaguneForm form = KaguneProfile.form(player);
        return form != null && !RinkakuForms.autonomous(form) && !PaleStun.stunned(player) && !PaleWitness.frozen(player)
            && !com.hex.kagune.control.DragonLock.locked(player);
    }

    private static void start(ServerPlayer player, long now) {
        if (!allowed(player) || !(player.m_9236_() instanceof ServerLevel level)) {
            return;
        }
        KaguneRuntime.State state = KaguneRuntime.get(player.m_20148_());
        if (state == null || state.selectedHeldEntityId() != 0 || state.hasManualHeldEntities() || ThyDeathServer.active(player)
            || RinkakuBlockServer.up(player)) {
            return; // someone held: H is the crush; or the tails are all busy hunting, or up in the block
        }
        if (!UkakuGates.unlocked(player, "The impale", RinkakuUnlocks.IMPALE)) {
            return;
        }
        Long ready = READY_AT.get(player.m_20148_());
        if (ready != null && now < ready && ready - now <= 400L) {
            player.m_5661_(Component.m_237115_("message.hexkagune.impale.recovering"), true);
            return;
        }
        KaguneForm form = KaguneProfile.form(player);
        int limbs = Math.min(KaguneProfile.manifestedLimbs(player), form.limbCount());
        boolean[] free = new boolean[Math.max(0, limbs)];
        boolean busy = KaguneServerController.committed(state.mode);
        for (int i = 0; i < free.length; i++) {
            free[i] = !form.limb(i).segmented() && state.heldEntityId(i) == 0 && !(busy && state.actionLimbIndex == i);
        }
        int limb = RinkakuImpale.pick(free, player.m_217043_().m_188500_());
        if (limb < 0) {
            player.m_5661_(Component.m_237115_("message.hexkagune.impale.no_tail"), true);
            return;
        }
        if (!KaguneServerController.useEnergy(player, RinkakuImpale.COST, true)) {
            return;
        }
        Spear spear = new Spear();
        spear.limb = limb;
        spear.started = now;
        spear.heldUntil = now + RinkakuImpale.HELD;
        spear.reach = RinkakuImpale.reach(KaguneProfile.mastery(player));
        Vec3 launch = launch(player, form, limb);
        spear.way = aim(player, launch, spear.reach);
        SPEARS.put(player.m_20148_(), spear);
        UkakuFire.sound(level, launch, "entity.illusioner.mirror_move", 0.75F, KaguneServerController.vary(player, 1.2F, 0.08F));
        KaguneServerController.playAt(player, launch, HexKaguneSounds.KAGUNE_WHIP, 0.95F, KaguneServerController.vary(player, 1.3F, 0.08F));
        tell(player, spear, S2CImpale.OUT);
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || SPEARS.isEmpty() || !(event.player instanceof ServerPlayer player)
            || !(player.m_9236_() instanceof ServerLevel level)) {
            return;
        }
        Spear spear = SPEARS.get(player.m_20148_());
        if (spear == null) {
            return;
        }
        long now = level.m_46467_();
        try {
            // 2.5.0's rule: a clock gone back, or the tails taken away, ends it at once.
            if (!allowed(player) || now < spear.started || now - spear.started > RinkakuImpale.MOST_OUT + RinkakuImpale.RETRACT + 40L) {
                end(player, level, spear, now, false);
                return;
            }
            if (spear.backAt >= 0L) {
                if (now - spear.backAt >= RinkakuImpale.RETRACT) {
                    end(player, level, spear, now, true);
                }
                return;
            }
            boolean held = !spear.released && now <= spear.heldUntil && spear.heldUntil - now <= RinkakuImpale.HELD;
            if (!held) {
                spear.released = true;
            }
            out(player, level, spear, held, now);
        } catch (RuntimeException failure) {
            com.hex.kagune.control.Safety.caught(failure, "the Impale");
            end(player, level, spear, now, false);
        }
    }

    /** A tick out: it turns after his look, runs on, runs through what is in its way, carries what it holds, stops at a wall. */
    private static void out(ServerPlayer player, ServerLevel level, Spear spear, boolean held, long now) {
        KaguneForm form = KaguneProfile.form(player);
        if (spear.limb >= form.limbCount() || form.limb(spear.limb).segmented()) {
            end(player, level, spear, now, false);
            return;
        }
        Vec3 launch = launch(player, form, spear.limb);
        Vec3 tipBefore = launch.m_82549_(spear.way.m_82490_(spear.length));
        double[] turned = RinkakuImpale.turn(new double[] {spear.way.f_82479_, spear.way.f_82480_, spear.way.f_82481_},
            vec(aim(player, launch, spear.reach)), RinkakuImpale.TURN);
        spear.way = new Vec3(turned[0], turned[1], turned[2]);
        double length = spear.stopped ? spear.length : RinkakuImpale.step(spear.length, held, spear.reach);
        Vec3 tip = launch.m_82549_(spear.way.m_82490_(length));
        // A wall in its way: it stops there (the whole of it, so it never swings through one either).
        if (length > 0.05) {
            BlockHitResult wall = level.m_45547_(new ClipContext(launch, tip, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (wall.m_6662_() == HitResult.Type.BLOCK) {
                double to = Math.max(0.4, Math.sqrt(wall.m_82450_().m_82557_(launch)) - 0.12);
                if (!spear.stopped) {
                    UkakuFire.sound(level, wall.m_82450_(), "block.stone.hit", 0.9F, KaguneServerController.vary(player, 0.85F, 0.08F));
                }
                spear.stopped = true;
                length = Math.min(length, to);
                tip = launch.m_82549_(spear.way.m_82490_(length));
            }
        }
        // Whatever its point runs through this tick.
        if (length > 0.05) {
            Vec3 from = spear.length > 0.05 ? tipBefore : launch;
            AABB swept = new AABB(Math.min(from.f_82479_, tip.f_82479_), Math.min(from.f_82480_, tip.f_82480_), Math.min(from.f_82481_, tip.f_82481_),
                Math.max(from.f_82479_, tip.f_82479_), Math.max(from.f_82480_, tip.f_82480_), Math.max(from.f_82481_, tip.f_82481_)).m_82400_(1.5);
            double[] a = vec(from);
            double[] b = vec(tip);
            // Whoever and whatever the M1 may hit (never his own allies, pets or partner); a creative player is never carried off.
            for (LivingEntity body : level.m_6443_(LivingEntity.class, swept, e -> e != player && e.m_6084_() && !e.m_5833_()
                && !(e instanceof net.minecraft.world.entity.player.Player other && other.m_7500_())
                && !spear.struck.contains(e.m_19879_()) && KaguneServerController.validSwipeTarget(player, e))) {
                if (RinkakuImpale.through(a, b, body.m_20185_(), body.m_20186_(), body.m_20186_() + body.m_20206_(), body.m_20189_(),
                    body.m_20205_() * 0.5)) {
                    runThrough(player, level, spear, body, now);
                }
            }
        }
        spear.length = length;
        carry(player, level, spear, launch, now);
        // Pinned: it met a wall with someone on it.
        if (spear.stopped && !spear.pinned && !spear.carried.isEmpty()) {
            spear.pinned = true;
            float pin = KaguneProfile.swipeDamage(player) * RinkakuImpale.PIN;
            for (int id : spear.carried) {
                if (level.m_6815_(id) instanceof LivingEntity body && body.m_6084_()) {
                    body.m_6469_(player.m_269291_().m_269333_(player), pin);
                    KaguneServerController.playAt(player, body, HexKaguneSounds.BONE_CRACK, 0.9F, KaguneServerController.vary(player, 0.9F, 0.08F));
                }
            }
        }
        // Gone as far as it goes: a beat there, then back. Held at its reach (or at a wall) it waits for the key, up to its most.
        boolean done = spear.stopped || RinkakuImpale.spent(length, held, spear.reach);
        if (done && !held) {
            if (spear.spentAt < 0L) {
                spear.spentAt = now;
            }
        } else {
            spear.spentAt = -1L;
        }
        if (spear.spentAt >= 0L && now - spear.spentAt >= RinkakuImpale.LINGER || now - spear.started >= RinkakuImpale.MOST_OUT) {
            back(player, level, spear, now);
            return;
        }
        tell(player, spear, S2CImpale.OUT);
    }

    /** {@code body} run through: hurt, bled, and carried on it (while it carries fewer than its most). */
    private static void runThrough(ServerPlayer player, ServerLevel level, Spear spear, LivingEntity body, long now) {
        spear.struck.add(body.m_19879_());
        Vec3 at = KaguneServerController.center(body);
        body.m_6469_(player.m_269291_().m_269333_(player), KaguneProfile.swipeDamage(player) * RinkakuImpale.THROUGH);
        UkakuCrystalCombat.rinkakuBleed(body, player, now);
        UkakuFire.sound(level, at, "hexkagune:sfx.stab", 1.0F, KaguneServerController.vary(player, 0.9F, 0.08F));
        KaguneServerController.playAt(player, body, HexKaguneSounds.FLESH_IMPACT, 0.9F, KaguneServerController.vary(player, 0.85F, 0.06F));
        KaguneProfile.addMastery(player, "Swipe", 2);
        if (body.m_6084_() && spear.carried.size() < RinkakuImpale.MOST_CARRIED) {
            spear.carried.add(body.m_19879_());
        }
    }

    /** What it carries, each moved to its place on the shaft (through the world: a body never goes into a wall); one held back too far tears free. */
    private static void carry(ServerPlayer player, ServerLevel level, Spear spear, Vec3 launch, long now) {
        int count = spear.carried.size();
        List<Integer> keep = new ArrayList<>();
        for (int k = 0; k < count; k++) {
            int id = spear.carried.get(k);
            if (!(level.m_6815_(id) instanceof LivingEntity body) || !body.m_6084_()) {
                continue;
            }
            Vec3 point = launch.m_82549_(spear.way.m_82490_(RinkakuImpale.slot(k, count, spear.length)));
            Vec3 feet = point.m_82520_(0.0, -body.m_20206_() * 0.5, 0.0);
            Vec3 at = body.m_20182_();
            // As CentipedeHunt holds a caught body: only where it fits, so it is never put in a wall: all the way; else not into the
            // ground (a spear run low drags it along the ground); else half way.
            Vec3 full = feet.m_82546_(at);
            Vec3[] tries = {full, new Vec3(full.f_82479_, Math.max(0.0, full.f_82480_), full.f_82481_), full.m_82490_(0.5)};
            for (Vec3 offset : tries) {
                Vec3 to = at.m_82549_(offset);
                if (level.m_45756_(body, body.m_20191_().m_82386_(offset.f_82479_, offset.f_82480_, offset.f_82481_))) {
                    if (offset.m_82556_() > 1.0E-6) {
                        body.m_6021_(to.f_82479_, to.f_82480_, to.f_82481_);
                    }
                    break;
                }
            }
            body.m_20256_(Vec3.f_82478_);
            body.f_19864_ = true;
            body.f_19789_ = 0.0F;
            KaguneServerController.syncKaguneHeldPlayer(body);
            if (body.m_20182_().m_82557_(feet) > RinkakuImpale.TEARS_FREE * RinkakuImpale.TEARS_FREE) {
                tearOut(player, level, spear, body);
                continue;
            }
            keep.add(id);
        }
        spear.carried.clear();
        spear.carried.addAll(keep);
    }

    /** It starts back: tearing out of everything it carries (they drop where they are). */
    private static void back(ServerPlayer player, ServerLevel level, Spear spear, long now) {
        spear.backAt = now;
        for (int id : spear.carried) {
            if (level.m_6815_(id) instanceof LivingEntity body && body.m_6084_()) {
                tearOut(player, level, spear, body);
            }
        }
        spear.carried.clear();
        KaguneServerController.playAt(player, launch(player, KaguneProfile.form(player), spear.limb), HexKaguneSounds.KAGUNE_WHIP, 0.6F,
            KaguneServerController.vary(player, 0.95F, 0.06F));
        tell(player, spear, S2CImpale.BACK);
    }

    /** It tears out of {@code body}: a last wound, blood, and it slides off the point. */
    private static void tearOut(ServerPlayer player, ServerLevel level, Spear spear, LivingEntity body) {
        Vec3 at = KaguneServerController.center(body);
        body.m_6469_(player.m_269291_().m_269333_(player), KaguneProfile.swipeDamage(player) * RinkakuImpale.TEAR);
        UkakuCrystalCombat.rinkakuBleed(body, player, level.m_46467_());
        UkakuFire.sound(level, at, "hexkagune:sfx.blood_spray", 0.9F, KaguneServerController.vary(player, 0.95F, 0.08F));
        body.m_20256_(new Vec3(spear.way.f_82479_ * 0.12, 0.05, spear.way.f_82481_ * 0.12));
        body.f_19864_ = true;
        body.f_19789_ = 0.0F;
    }

    /** It is home (or cut short: {@code home} false, and whatever it still carried let go as it is). */
    private static void end(ServerPlayer player, ServerLevel level, Spear spear, long now, boolean home) {
        SPEARS.remove(player.m_20148_());
        if (!home) {
            for (int id : spear.carried) {
                if (level.m_6815_(id) instanceof LivingEntity body) {
                    body.f_19789_ = 0.0F;
                }
            }
        }
        spear.carried.clear();
        READY_AT.put(player.m_20148_(), now + KaguneProfile.cooldown(player, RinkakuImpale.COOLDOWN));
        tell(player, spear, S2CImpale.GONE);
    }

    private static void tell(ServerPlayer player, Spear spear, int phase) {
        ControlNetwork.broadcastAround(player, new S2CImpale(player.m_19879_(), spear.limb, (float)spear.length, (float)spear.way.f_82479_,
            (float)spear.way.f_82480_, (float)spear.way.f_82481_, phase));
    }

    /** Where tail {@code limb} leaves over his shoulder (as the client draws it, {@code ImpaleClient}). */
    static Vec3 launch(ServerPlayer player, KaguneForm form, int limb) {
        double yaw = Math.toRadians(player.f_20883_);
        double side = limb < form.limbCount() ? RinkakuRoot.side(limb, form.limb(limb).fanSide()) : 1.0;
        double[] at = RinkakuImpale.launch(new double[] {player.m_20185_(), player.m_20186_(), player.m_20189_()}, player.m_20206_(), side,
            -Math.cos(yaw), -Math.sin(yaw), -Math.sin(yaw), Math.cos(yaw), false);
        return new Vec3(at[0], at[1], at[2]);
    }

    /** The way from {@code launch} to what he looks at, {@code reach} out (a unit vector). */
    private static Vec3 aim(ServerPlayer player, Vec3 launch, double reach) {
        Vec3 look = player.m_20154_();
        Vec3 to = player.m_146892_().m_82549_(look.m_82490_(reach)).m_82546_(launch);
        return to.m_82556_() < 1.0E-6 ? look : to.m_82541_();
    }

    private static double[] vec(Vec3 v) {
        return new double[] {v.f_82479_, v.f_82480_, v.f_82481_};
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.m_9236_() instanceof ServerLevel level) {
            Spear spear = SPEARS.get(player.m_20148_());
            if (spear != null) {
                end(player, level, spear, level.m_46467_(), false);
            }
        }
        READY_AT.remove(event.getEntity().m_20148_());
    }
}
