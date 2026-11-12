package com.hex.kagune.control.server;

import com.hex.kagune.control.RinkakuForms;
import com.hex.kagune.control.RinkakuHurl;
import com.hex.kagune.control.RinkakuImpale;
import com.hex.kagune.control.RinkakuUnlocks;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CHurl;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.KaguneForm;
import com.hex.kagune.gameplay.KaguneRuntime;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.sound.HexKaguneSounds;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 3.1.6: Rinkaku's Rubble Hurl on the server ({@link RinkakuHurl}). N pressed with no one held (the client's own hold, as the Impale's;
 * the original's throw only runs with someone held, {@code UkakuGates.throwHeld}): up to two free tails tear a chunk out of the block
 * he looks at, or the ground before him ({@link BlockSmash#tear}: the block itself out of the world only where he may break it, and
 * put back exactly later); it rises into their grip and grows while N is held; let go, it is thrown at what he looks at, in pieces
 * in the final kakuja. Each piece flies ({@link RinkakuHurl#step}) until it meets a body or a wall, or has flown its most, and bursts
 * there: whoever is near is hurt, stunned a moment and thrown, and a full one craters the ground. Every client near him is told every
 * tick ({@link S2CHurl}) and draws it ({@code HurlClient}).
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class HurlServer {
    private HurlServer() {
    }

    /** A piece in flight: where, how fast, how big, how long it has flown, and whether it has burst. */
    private static final class Piece {
        double[] pos;
        double[] vel;
        double size;
        int flown;
        boolean done;
    }

    /** One player's hurl: its tails, its times, what it was torn from, how charged, where it hangs, and its pieces once thrown. */
    private static final class Hurl {
        int limbA;
        int limbB = -1;
        long started;
        long heldUntil;
        boolean released;
        double[] from;
        BlockState state;
        String item;
        double charge;
        double[] holdAt;
        boolean kakuja;
        long thrownAt = -1L;
        final List<Piece> pieces = new ArrayList<>();
    }

    private static final Map<UUID, Hurl> HURLS = WorldTimes.kept();
    private static final Map<UUID, Long> READY_AT = WorldTimes.map();
    /** The body a burst is hurting right now (its entity id), so {@code RinkakuWounds} tears it as a crush. */
    private static int bursting;

    /** True while {@code player}'s tails hold a chunk (torn out, not yet thrown). */
    public static boolean holding(ServerPlayer player) {
        Hurl hurl = HURLS.get(player.m_20148_());
        return hurl != null && hurl.thrownAt < 0L;
    }

    /** True while {@code body} is being hurt by a burst (its wound a crush). */
    static boolean crushing(LivingEntity body) {
        return bursting != 0 && bursting == body.m_19879_();
    }

    /** True if N is the original's throw for {@code player} now: a tail holds someone (else it is the Rubble Hurl). */
    static boolean throwsHeld(ServerPlayer player) {
        KaguneRuntime.State state = KaguneRuntime.get(player.m_20148_());
        return state != null && state.hasManualHeldEntities();
    }

    /** N from {@code player}'s client: {@code phase} 2 pressed, 1 still held, 0 let go. */
    static void input(ServerPlayer player, int phase) {
        Hurl hurl = HURLS.get(player.m_20148_());
        long now = player.m_9236_().m_46467_();
        if (phase == 0) {
            if (hurl != null) {
                hurl.released = true;
            }
            return;
        }
        if (hurl != null) {
            if (phase == 1 && !hurl.released) {
                hurl.heldUntil = now + RinkakuHurl.HELD;
            }
            return;
        }
        if (phase == 2) {
            start(player, now);
        }
    }

    private static void start(ServerPlayer player, long now) {
        if (!ImpaleServer.allowed(player) || !(player.m_9236_() instanceof ServerLevel level)) {
            return;
        }
        KaguneRuntime.State state = KaguneRuntime.get(player.m_20148_());
        if (state == null || state.hasManualHeldEntities() || state.selectedHeldEntityId() != 0 || ThyDeathServer.active(player)
            || RinkakuBlockServer.up(player) || ImpaleServer.active(player)) {
            return; // someone held: N is the throw; or the tails are busy
        }
        if (!UkakuGates.unlocked(player, "The rubble hurl", RinkakuUnlocks.HURL) || !QuestServer.pass(player, com.hex.kagune.control.quest.Gate.RINKAKU_HURL)) {
            return;
        }
        Long ready = READY_AT.get(player.m_20148_());
        if (ready != null && now < ready && ready - now <= 400L) {
            player.m_5661_(Component.m_237115_("message.hexkagune.hurl.recovering"), true);
            return;
        }
        KaguneForm form = KaguneProfile.form(player);
        int limbs = Math.min(KaguneProfile.manifestedLimbs(player), form.limbCount());
        boolean[] free = new boolean[Math.max(0, limbs)];
        boolean busy = KaguneServerController.committed(state.mode);
        for (int i = 0; i < free.length; i++) {
            free[i] = !form.limb(i).segmented() && state.heldEntityId(i) == 0 && !(busy && state.actionLimbIndex == i);
        }
        int limbA = RinkakuImpale.pick(free, player.m_217043_().m_188500_());
        if (limbA < 0) {
            player.m_5661_(Component.m_237115_("message.hexkagune.impale.no_tail"), true);
            return;
        }
        free[limbA] = false;
        int limbB = RinkakuHurl.TAILS > 1 ? RinkakuImpale.pick(free, player.m_217043_().m_188500_()) : -1;
        BlockPos source = source(player, level);
        if (source == null) {
            player.m_5661_(Component.m_237115_("message.hexkagune.hurl.nothing"), true);
            return;
        }
        if (!KaguneServerController.useEnergy(player, RinkakuHurl.COST, true)) {
            return;
        }
        BlockState block = level.m_8055_(source);
        Hurl hurl = new Hurl();
        hurl.limbA = limbA;
        hurl.limbB = limbB;
        hurl.started = now;
        hurl.heldUntil = now + RinkakuHurl.HELD;
        hurl.from = new double[] {source.m_123341_() + 0.5, source.m_123342_() + 0.5, source.m_123343_() + 0.5};
        hurl.holdAt = hurl.from.clone();
        hurl.state = block;
        hurl.item = item(block);
        hurl.kakuja = form == RinkakuForms.finalKakuja();
        HURLS.put(player.m_20148_(), hurl);
        // The block itself comes out of the world (put back later), but never one he stands on: he isn't dropped into his own hole.
        double half = player.m_20205_() * 0.5 + 0.5;
        boolean underFoot = source.m_123342_() == (int)Math.floor(player.m_20186_() - 0.01)
            && Math.abs(hurl.from[0] - player.m_20185_()) < half && Math.abs(hurl.from[2] - player.m_20189_()) < half;
        if (underFoot) {
            level.m_46796_(2001, source, Block.m_49956_(block));
        } else {
            BlockSmash.tear(player, source, block, RinkakuHurl.HARDEST);
        }
        Vec3 at = new Vec3(hurl.from[0], hurl.from[1], hurl.from[2]);
        KaguneServerController.playAt(player, at, HexKaguneSounds.KAGUNE_GRAB, 0.9F, KaguneServerController.vary(player, 0.85F, 0.06F));
        UkakuFire.sound(level, at, "block.stone.break", 1.0F, KaguneServerController.vary(player, 0.8F, 0.06F));
        tell(player, hurl, 0, S2CHurl.HOLD, hurl.holdAt, RinkakuHurl.size(0.0, 1));
    }

    /** The block his tails tear a chunk from: the one he looks at within reach, else the ground just before him; null for none. */
    private static BlockPos source(ServerPlayer player, ServerLevel level) {
        BlockHitResult hit = KaguneServerController.traceBlock(player, RinkakuHurl.DIG_REACH);
        if (hit != null && hit.m_6662_() == HitResult.Type.BLOCK && usable(level, hit.m_82425_())) {
            return hit.m_82425_();
        }
        Vec3 look = player.m_20154_();
        double flat = Math.sqrt(look.f_82479_ * look.f_82479_ + look.f_82481_ * look.f_82481_);
        double yaw = Math.toRadians(player.m_146908_());
        double fx = flat > 1.0E-4 ? look.f_82479_ / flat : -Math.sin(yaw);
        double fz = flat > 1.0E-4 ? look.f_82481_ / flat : Math.cos(yaw);
        for (double ahead : new double[] {RinkakuHurl.GROUND_AHEAD, 1.0, 2.6}) {
            for (int dy = 0; dy >= -3; dy--) {
                BlockPos pos = BlockPos.m_274561_(player.m_20185_() + fx * ahead, player.m_20186_() - 0.5 + dy, player.m_20189_() + fz * ahead);
                if (usable(level, pos)) {
                    return pos;
                }
            }
        }
        return null;
    }

    /** A block a chunk can be torn from: something solid there, not unbreakable. */
    static boolean usable(ServerLevel level, BlockPos pos) {
        BlockState state = level.m_8055_(pos);
        return !state.m_60795_() && !state.m_60812_(level, pos).m_83281_() && state.m_60800_(level, pos) >= 0.0F;
    }

    /** The item {@code state}'s chunk is drawn as (its block's own; a block with none, cobblestone). */
    static String item(BlockState state) {
        ItemStack stack = new ItemStack(state.m_60734_(), 1);
        ResourceLocation key = stack.m_41619_() ? null : ForgeRegistries.ITEMS.getKey(stack.m_41720_());
        return key == null ? "minecraft:cobblestone" : key.toString();
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || HURLS.isEmpty() || !(event.player instanceof ServerPlayer player)
            || !(player.m_9236_() instanceof ServerLevel level)) {
            return;
        }
        Hurl hurl = HURLS.get(player.m_20148_());
        if (hurl == null) {
            return;
        }
        long now = level.m_46467_();
        try {
            // 2.5.0's rule: a clock gone back, or the tails taken away, ends it at once.
            if (!ImpaleServer.allowed(player) || now < hurl.started
                || now - hurl.started > RinkakuHurl.MOST_HELD + RinkakuHurl.FLIGHT + 60L) {
                end(player, level, hurl, now);
                return;
            }
            if (hurl.thrownAt < 0L) {
                hold(player, level, hurl, now);
                return;
            }
            boolean flying = false;
            for (int i = 0; i < hurl.pieces.size(); i++) {
                Piece piece = hurl.pieces.get(i);
                if (!piece.done) {
                    fly(player, level, hurl, piece, i, now);
                    flying |= !piece.done;
                }
            }
            if (!flying) {
                end(player, level, hurl, now);
            }
        } catch (RuntimeException failure) {
            com.hex.kagune.control.Safety.caught(failure, "the Rubble Hurl");
            end(player, level, hurl, now);
        }
    }

    /** Held: it rises into the tails' grip and grows while N is held; let go (once it is up), or held its most, it is thrown. */
    private static void hold(ServerPlayer player, ServerLevel level, Hurl hurl, long now) {
        boolean held = !hurl.released && now <= hurl.heldUntil && hurl.heldUntil - now <= RinkakuHurl.HELD;
        if (!held) {
            hurl.released = true;
        }
        long age = now - hurl.started;
        if (held) {
            hurl.charge = RinkakuHurl.charge(age);
        }
        double size = RinkakuHurl.size(hurl.charge, 1);
        Vec3 look = player.m_20154_();
        double flat = Math.sqrt(look.f_82479_ * look.f_82479_ + look.f_82481_ * look.f_82481_);
        double yaw = Math.toRadians(player.m_146908_());
        double fx = flat > 1.0E-4 ? look.f_82479_ / flat : -Math.sin(yaw);
        double fz = flat > 1.0E-4 ? look.f_82481_ / flat : Math.cos(yaw);
        Vec3 eye = player.m_146892_();
        double[] hover = RinkakuHurl.hover(new double[] {eye.f_82479_, eye.f_82480_, eye.f_82481_}, fx, fz, size);
        hurl.holdAt = age < RinkakuHurl.RISE ? RinkakuHurl.rising(hurl.from, hover, age) : hover;
        if (hurl.released && age >= RinkakuHurl.RISE || age >= RinkakuHurl.MOST_HELD) {
            hurl(player, level, hurl, now);
            return;
        }
        if (held && age > RinkakuHurl.RISE && age <= RinkakuHurl.CHARGE && age % 8 == 0) {
            // It grows: more of the ground torn up with it.
            UkakuFire.sound(level, new Vec3(hurl.holdAt[0], hurl.holdAt[1], hurl.holdAt[2]), "block.gravel.break", 0.6F,
                KaguneServerController.vary(player, 0.7F, 0.06F));
        }
        tell(player, hurl, 0, S2CHurl.HOLD, hurl.holdAt, size);
    }

    /** Let go: thrown at what he looks at (a body under his aim, else the block, else as far as it aims), in pieces in the final kakuja. */
    private static void hurl(ServerPlayer player, ServerLevel level, Hurl hurl, long now) {
        Vec3 eye = player.m_146892_();
        Vec3 look = player.m_20154_();
        Vec3 end = eye.m_82549_(look.m_82490_(RinkakuHurl.AIM_RANGE));
        BlockHitResult wall = level.m_45547_(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 aim = wall.m_6662_() == HitResult.Type.BLOCK ? wall.m_82450_() : end;
        LivingEntity target = KaguneServerController.traceLiving(player, RinkakuHurl.AIM_RANGE, false);
        if (target != null) {
            Vec3 centre = KaguneServerController.center(target);
            Vec3 to = centre.m_82546_(eye);
            if (to.m_82556_() > 1.0E-4 && to.m_82556_() < aim.m_82557_(eye) + 1.0 && to.m_82541_().m_82526_(look) > 0.94) {
                aim = centre;
            }
        }
        int count = hurl.kakuja ? RinkakuHurl.KAKUJA_PIECES : 1;
        double[] velocity = RinkakuHurl.aim(hurl.holdAt, new double[] {aim.f_82479_, aim.f_82480_, aim.f_82481_}, RinkakuHurl.speed(hurl.charge));
        for (int k = 0; k < count; k++) {
            Piece piece = new Piece();
            piece.pos = hurl.holdAt.clone();
            piece.vel = count == 1 ? velocity : RinkakuHurl.spread(velocity, (k - (count - 1) / 2.0) * RinkakuHurl.SPREAD);
            piece.size = RinkakuHurl.size(hurl.charge, count);
            hurl.pieces.add(piece);
        }
        hurl.thrownAt = now;
        Vec3 at = new Vec3(hurl.holdAt[0], hurl.holdAt[1], hurl.holdAt[2]);
        KaguneServerController.playAt(player, at, HexKaguneSounds.KAGUNE_WHIP, 1.0F, KaguneServerController.vary(player, 0.8F, 0.06F));
        UkakuFire.sound(level, at, "entity.ender_dragon.flap", 0.6F + 0.4F * (float)hurl.charge, KaguneServerController.vary(player, 0.9F, 0.06F));
        for (int k = 0; k < hurl.pieces.size(); k++) {
            tell(player, hurl, k, S2CHurl.FLY, hurl.pieces.get(k).pos, hurl.pieces.get(k).size);
        }
    }

    /** A tick of a piece's flight: on until it meets a body or a wall (it bursts there), or has flown its most. */
    private static void fly(ServerPlayer player, ServerLevel level, Hurl hurl, Piece piece, int index, long now) {
        double[] a = piece.pos.clone();
        double[] next = piece.pos.clone();
        double[] vel = piece.vel.clone();
        RinkakuHurl.step(next, vel);
        Vec3 from = new Vec3(a[0], a[1], a[2]);
        Vec3 to = new Vec3(next[0], next[1], next[2]);
        BlockHitResult wall = level.m_45547_(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        boolean walled = wall.m_6662_() == HitResult.Type.BLOCK;
        Vec3 stop = walled ? wall.m_82450_() : to;
        double radius = piece.size * 0.45;
        double[] b = {stop.f_82479_, stop.f_82480_, stop.f_82481_};
        AABB swept = new AABB(Math.min(from.f_82479_, stop.f_82479_), Math.min(from.f_82480_, stop.f_82480_), Math.min(from.f_82481_, stop.f_82481_),
            Math.max(from.f_82479_, stop.f_82479_), Math.max(from.f_82480_, stop.f_82480_), Math.max(from.f_82481_, stop.f_82481_)).m_82400_(radius + 1.0);
        LivingEntity first = null;
        double nearest = Double.MAX_VALUE;
        for (LivingEntity body : level.m_6443_(LivingEntity.class, swept, e -> hits(player, e))) {
            if (RinkakuHurl.meets(a, b, body.m_20185_(), body.m_20186_(), body.m_20186_() + body.m_20206_(), body.m_20189_(),
                body.m_20205_() * 0.5, radius)) {
                double d = body.m_20238_(from);
                if (d < nearest) {
                    nearest = d;
                    first = body;
                }
            }
        }
        if (first != null) {
            burst(player, level, hurl, piece, index, KaguneServerController.center(first), now);
            return;
        }
        if (walled) {
            Vec3 back = to.m_82546_(from);
            Vec3 at = back.m_82556_() > 1.0E-6 ? stop.m_82546_(back.m_82541_().m_82490_(0.3)) : stop;
            burst(player, level, hurl, piece, index, at, now);
            return;
        }
        piece.pos = next;
        piece.vel = vel;
        piece.flown++;
        if (piece.flown >= RinkakuHurl.FLIGHT) {
            burst(player, level, hurl, piece, index, to, now);
            return;
        }
        tell(player, hurl, index, S2CHurl.FLY, piece.pos, piece.size);
    }

    /** True if {@code body} is something it hurts: whoever the M1 may hit (never his own allies, pets or partner), not a creative player. */
    private static boolean hits(ServerPlayer player, LivingEntity body) {
        return body != player && body.m_6084_() && !body.m_5833_()
            && !(body instanceof net.minecraft.world.entity.player.Player other && other.m_7500_())
            && KaguneServerController.validSwipeTarget(player, body);
    }

    /** A piece bursts at {@code at}: hurt, stunned and thrown by how near, the block breaking up there, the ground cratered by a full one. */
    private static void burst(ServerPlayer player, ServerLevel level, Hurl hurl, Piece piece, int index, Vec3 at, long now) {
        piece.done = true;
        piece.pos = new double[] {at.f_82479_, at.f_82480_, at.f_82481_};
        double radius = RinkakuHurl.radius(piece.size);
        float base = KaguneProfile.swipeDamage(player) * (float)RinkakuHurl.power(hurl.charge) * (hurl.pieces.size() > 1 ? 0.7F : 1.0F);
        boolean any = false;
        for (LivingEntity body : level.m_6443_(LivingEntity.class, new AABB(at.f_82479_, at.f_82480_, at.f_82481_, at.f_82479_, at.f_82480_, at.f_82481_).m_82400_(radius + 1.0), e -> hits(player, e))) {
            // How far from its middle: to the nearest point of the body's upright middle line, less its half width.
            double y = Math.max(body.m_20186_(), Math.min(body.m_20186_() + body.m_20206_(), at.f_82480_));
            double dx = body.m_20185_() - at.f_82479_;
            double dz = body.m_20189_() - at.f_82481_;
            double d = Math.max(0.0, Math.sqrt(dx * dx + dz * dz) - body.m_20205_() * 0.5);
            d = Math.sqrt(d * d + (y - at.f_82480_) * (y - at.f_82480_));
            double share = RinkakuHurl.falloff(d, radius);
            if (share <= 0.0) {
                continue;
            }
            any = true;
            bursting = body.m_19879_();
            try {
                body.m_6469_(player.m_269291_().m_269333_(player), base * (float)share);
            } finally {
                bursting = 0;
            }
            if (!body.m_6084_()) {
                continue;
            }
            PaleStun.stun(body, now, RinkakuHurl.stun(hurl.charge));
            double away = Math.sqrt(dx * dx + dz * dz);
            double wx = away > 1.0E-3 ? dx / away : piece.vel[0];
            double wz = away > 1.0E-3 ? dz / away : piece.vel[2];
            body.m_147240_(RinkakuHurl.knock(hurl.charge) * share, -wx, -wz);
            body.m_20256_(body.m_20184_().m_82520_(0.0, RinkakuHurl.lift(hurl.charge) * share, 0.0));
            body.f_19864_ = true;
        }
        // The block breaks up where it lands: more of it the bigger it was.
        int id = Block.m_49956_(hurl.state);
        level.m_46796_(2001, BlockPos.m_274561_(at.f_82479_, at.f_82480_, at.f_82481_), id);
        if (piece.size > 1.2) {
            for (double[] off : new double[][] {{0.8, 0.0}, {-0.8, 0.0}, {0.0, 0.8}, {0.0, -0.8}}) {
                level.m_46796_(2001, BlockPos.m_274561_(at.f_82479_ + off[0], at.f_82480_, at.f_82481_ + off[1]), id);
            }
        }
        UkakuFire.sound(level, at, "entity.generic.explode", 0.45F + 0.5F * (float)hurl.charge, KaguneServerController.vary(player, 1.0F, 0.08F));
        UkakuFire.sound(level, at, "block.stone.break", 1.0F, KaguneServerController.vary(player, 0.75F, 0.06F));
        if (hurl.charge >= RinkakuHurl.CRATERS) {
            BlockSmash.crater(player, at.m_82520_(0.0, -piece.size * 0.5, 0.0), 1.0 + 0.5 * piece.size);
        }
        if (any) {
            KaguneProfile.addMastery(player, "Swipe", 2);
        }
        tell(player, hurl, index, S2CHurl.BURST, piece.pos, piece.size);
    }

    /** It is over: every piece burst (or cut short: a chunk still held crumbles where it is). */
    private static void end(ServerPlayer player, ServerLevel level, Hurl hurl, long now) {
        HURLS.remove(player.m_20148_());
        if (hurl.thrownAt < 0L && hurl.state != null) {
            level.m_46796_(2001, BlockPos.m_274561_(hurl.holdAt[0], hurl.holdAt[1], hurl.holdAt[2]), Block.m_49956_(hurl.state));
        }
        READY_AT.put(player.m_20148_(), now + KaguneProfile.cooldown(player, RinkakuHurl.COOLDOWN));
        ControlNetwork.broadcastAround(player, new S2CHurl(player.m_19879_(), 0, S2CHurl.GONE, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1, -1, ""));
    }

    private static void tell(ServerPlayer player, Hurl hurl, int piece, int phase, double[] at, double size) {
        ControlNetwork.broadcastAround(player, new S2CHurl(player.m_19879_(), piece, phase, (float)at[0], (float)at[1], (float)at[2], (float)size,
            (float)hurl.charge, hurl.limbA, hurl.limbB, hurl.item));
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.m_9236_() instanceof ServerLevel level) {
            Hurl hurl = HURLS.get(player.m_20148_());
            if (hurl != null) {
                end(player, level, hurl, level.m_46467_());
            }
        }
        READY_AT.remove(event.getEntity().m_20148_());
    }
}
