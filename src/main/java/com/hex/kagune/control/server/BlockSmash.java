package com.hex.kagune.control.server;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.control.ControlConfig;
import com.hex.kagune.control.pale.PaleRules;
import com.hex.kagune.control.pale.OwlRules;
import com.hex.kagune.control.network.S2CPaleFx;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Bodies Pale launches smash through weak blocks (6.1.0): shared by every move that launches (Big Punch, the flesh's throw; 6.2.0:
 * the speed kick carries its body through far more, {@link #kick}). While a launched body flies fast, the weak blocks in its way this tick (dirt, sand, gravel, wood, glass, leaves,
 * wool: nothing a pickaxe is needed for, nothing unbreakable, nothing tougher than wood) are broken out of its path before it
 * moves, each slowing it a little; anything stronger stops it, and slamming into it hurts.
 *
 * <p>A smashed block is not destroyed: it is taken out as vanilla's /setblock takes one out (its block entity cleared first, so a
 * chest spills nothing; no neighbour updates, so the torch on a smashed wall stays put) and remembered, state and block entity
 * data, and after {@link ControlConfig#blockSmashRestoreSeconds} it is put back exactly as it was. If something else is there by
 * then (a player built in the hole), it is never overwritten: it waits until the place is free again. What waits is saved with
 * the world, and put back when the server stops. Protected blocks are never touched: every block is first offered to Forge's
 * {@link BlockEvent.BreakEvent} as hers to break (claims, spawn protection, adventure mode), as if she broke it by hand.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class BlockSmash {
    /** Faster than this (blocks a tick) a launched body breaks what is in its way; slower, it is just falling. */
    static final double SMASH_SPEED = 0.7;
    /** Into something it can't break this fast, it is hurt (per block a tick over). */
    static final double CRASH_SPEED = 0.9;
    static final float CRASH_DAMAGE = 2.5F;
    /** Each block broken takes this much of its speed. */
    static final double PER_BLOCK = 0.9;
    /** Coming down faster than this (blocks a tick), it breaks into the ground; slower, the ground holds it. */
    static final double SLAM_SPEED = 1.0;
    /** At most this many blocks a tick, and a launch, so nothing tunnels forever. */
    static final int MOST_A_TICK = 24;
    static final int MOST_A_LAUNCH = 160;
    /** Nothing tougher than wood breaks (destroy time). */
    static final float HARDEST = 2.5F;
    /** At most this many blocks wait at once in a level; past it, nothing more breaks until some are back. */
    static final int MOST_WAITING = 4096;
    /** A block whose place is taken is tried again this often (ticks). */
    static final long RETRY_TICKS = 100L;

    /** A body in flight: its entity id, whose blow sent it, ticks left to watch it, blocks broken, whether it has crashed. */
    static final class Launch {
        final int id;
        final UUID owner;
        int ticks;
        int broken;
        boolean crashed;
        /**
         * 6.2.0, the Owl's Big Punch ({@link #drive}): the level line it is driven along (unit; null for an ordinary launch), the
         * height it flies at, its speed now and how it slows, how far it has gone of how far it goes, the blow's damage, and what it
         * has already slammed into on the way.
         */
        Vec3 line;
        double height;
        double speed;
        double decel;
        double travelled;
        double distance;
        float damage;
        final java.util.Set<Integer> bowled = new java.util.HashSet<>();
        /**
         * 6.2.0, the Speed Kick ({@link #kick}): the level line the kick carries it along (unit; null for anything else), how fast it
         * was sent off, and how many ticks it has flown.
         */
        Vec3 carry;
        double sent;
        int flown;

        Launch(int id, UUID owner, int ticks) {
            this.id = id;
            this.owner = owner;
            this.ticks = ticks;
        }
    }

    private static final Map<ServerLevel, List<Launch>> FLYING = new IdentityHashMap<>();

    private BlockSmash() {
    }

    /** Throws {@code target} along {@code velocity} (blocks a tick) for {@code ticks}: it smashes through what it can on the way. */
    public static void launch(ServerPlayer owner, LivingEntity target, Vec3 velocity, int ticks) {
        target.m_20256_(velocity);
        target.f_19864_ = true;
        target.f_19812_ = true;
        watch(owner.m_284548_(), new Launch(target.m_19879_(), owner.m_20148_(), ticks));
    }

    /** True while the flights are being stepped (a body thrown meanwhile is watched from the next tick: the list is not touched). */
    private static boolean stepping;
    private static final List<Runnable> LATER = new ArrayList<>();

    /** {@code launch} watched from now on (in place of any earlier flight of the same body). */
    private static void watch(ServerLevel level, Launch launch) {
        if (stepping) {
            LATER.add(() -> watch(level, launch));
            return;
        }
        List<Launch> flying = FLYING.computeIfAbsent(level, ignored -> new ArrayList<>());
        flying.removeIf(other -> other.id == launch.id);
        flying.add(launch);
    }

    /**
     * 6.2.0, the Owl's Big Punch: {@code target} driven dead level along {@code line} (horizontal; the way the blow went, nothing
     * else), at the height it was struck at, off at {@code speed} (blocks a tick) and slowing evenly to a stop {@code distance} on
     * ({@link OwlRules#punchDecel}): never up, never down, whatever it was standing on or bumps into. Whatever is in its way within
     * {@link OwlRules#PUNCH_SMASH_REACH} of where it was struck is broken out of it before it gets there (anything up to
     * {@link OwlRules#PUNCH_HARDEST}, a pickaxe's blocks too; each put back exactly later, as every smashed block is); past that,
     * or anything harder, stops it dead and hurts it. What it slams into on the way is hurt ({@code damage} the blow's) and thrown
     * aside. It can do nothing while it flies (stunned).
     */
    public static void drive(ServerPlayer owner, LivingEntity target, Vec3 line, double distance, double speed, float damage) {
        Vec3 level = new Vec3(line.f_82479_, 0.0, line.f_82481_);
        if (level.m_82556_() < 1.0E-8) {
            return;
        }
        level = level.m_82541_();
        Launch launch = new Launch(target.m_19879_(), owner.m_20148_(), OwlRules.PUNCH_FLIGHT_TICKS);
        launch.line = level;
        launch.height = target.m_20186_();
        launch.speed = speed;
        launch.distance = distance;
        launch.decel = OwlRules.punchDecel(speed, distance);
        launch.damage = damage;
        launch.bowled.add(owner.m_19879_());
        launch.bowled.add(target.m_19879_());
        target.m_20256_(level.m_82490_(OwlRules.punchStep(speed, 0.0, distance)));
        target.f_19864_ = true;
        target.f_19812_ = true;
        watch(owner.m_284548_(), launch);
    }

    /**
     * 6.2.0, the Speed Kick: {@code target} sent off along {@code line} (level: straight away from her) at {@code speed} (blocks a
     * tick) and {@code up}. The kick carries it: no air drag, only {@link PaleRules#KICK_CARRY} of its speed lost a tick and
     * {@link PaleRules#KICK_PER_BLOCK} a block it breaks, gravity bringing it down, until it is down on the ground (vanilla's friction
     * skids it to a stop), spent, or stopped dead by something it can't break. On the way it smashes through anything up to
     * {@link PaleRules#KICK_HARDEST} (each block put back exactly later) and bowls over what it flies into ({@code damage} x
     * {@link PaleRules#KICK_BOWLED}). It can do nothing while it flies (stunned).
     */
    public static void kick(ServerPlayer owner, LivingEntity target, Vec3 line, double speed, double up, float damage) {
        Vec3 level = new Vec3(line.f_82479_, 0.0, line.f_82481_);
        if (level.m_82556_() < 1.0E-8) {
            return;
        }
        level = level.m_82541_();
        Launch launch = new Launch(target.m_19879_(), owner.m_20148_(), PaleRules.KICK_FLIGHT_TICKS);
        launch.carry = level;
        launch.sent = speed;
        launch.speed = speed;
        launch.damage = damage;
        launch.bowled.add(owner.m_19879_());
        launch.bowled.add(target.m_19879_());
        target.m_20256_(level.m_82490_(speed).m_82520_(0.0, up, 0.0));
        target.f_19864_ = true;
        target.f_19812_ = true;
        watch(owner.m_284548_(), launch);
    }

    @SubscribeEvent
    public static void levelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !(event.level instanceof ServerLevel level)) {
            return;
        }
        List<Launch> flying = FLYING.get(level);
        if (flying != null) {
            stepping = true;
            try {
                for (Iterator<Launch> it = flying.iterator(); it.hasNext(); ) {
                    Launch launch = it.next();
                    if (!fly(level, launch)) {
                        it.remove();
                    }
                }
            } finally {
                stepping = false;
            }
            if (flying.isEmpty()) {
                FLYING.remove(level);
            }
        }
        if (!LATER.isEmpty()) {
            List<Runnable> later = new ArrayList<>(LATER);
            LATER.clear();
            later.forEach(Runnable::run);
        }
        long now = level.m_46467_();
        if (now % 20L == 0L) {
            Waiting.of(level).restore(level, now, false);
        }
    }

    /** One tick of one launched body, before it moves: false once it is done. */
    private static boolean fly(ServerLevel level, Launch launch) {
        Entity entity = level.m_6815_(launch.id);
        if (!(entity instanceof LivingEntity body) || !body.m_6084_() || --launch.ticks < 0) {
            return false;
        }
        if (launch.line != null) {
            return driven(level, launch, body);
        }
        if (launch.carry != null) {
            return carried(level, launch, body);
        }
        Vec3 velocity = body.m_20184_();
        double speed = velocity.m_82553_();
        if (speed < SMASH_SPEED) {
            return launch.ticks > 0 && speed > 0.05; // slowed (in the air still): keep watching a moment in case it is thrown again
        }
        ServerPlayer owner = level.m_7654_().m_6846_().m_11259_(launch.owner);
        AABB path = body.m_20191_().m_82369_(velocity).m_82400_(0.05);
        int broke = 0;
        boolean blocked = false;
        double kept = 1.0;
        int minX = (int)Math.floor(path.f_82288_);
        int minY = (int)Math.floor(path.f_82289_);
        int minZ = (int)Math.floor(path.f_82290_);
        int maxX = (int)Math.floor(path.f_82291_);
        int maxY = (int)Math.floor(path.f_82292_);
        int maxZ = (int)Math.floor(path.f_82293_);
        for (int x = minX; x <= maxX && broke < MOST_A_TICK; x++) {
            for (int y = minY; y <= maxY && broke < MOST_A_TICK; y++) {
                for (int z = minZ; z <= maxZ && broke < MOST_A_TICK; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.m_8055_(pos);
                    if (state.m_60795_() || !inPath(level, pos, state, path) || floor(body, y, velocity)) {
                        continue;
                    }
                    if (owner != null && launch.broken < MOST_A_LAUNCH && ControlConfig.blockSmash() && weak(level, pos, state)
                        && smash(level, owner, pos, state, velocity)) {
                        broke++;
                        launch.broken++;
                        kept *= PER_BLOCK;
                    } else {
                        blocked = true;
                    }
                }
            }
        }
        if (broke > 0) {
            body.m_20256_(velocity.m_82490_(kept));
            body.f_19864_ = true;
        }
        if (blocked && !launch.crashed && speed > CRASH_SPEED) {
            // Slammed into something that held.
            launch.crashed = true;
            float damage = (float)((speed - CRASH_SPEED) * CRASH_DAMAGE);
            body.f_19802_ = 0;
            if (owner != null) {
                body.m_6469_(owner.m_269291_().m_269075_(owner), damage);
            }
            Vec3 at = body.m_20182_().m_82549_(velocity.m_82541_().m_82490_(body.m_20205_() * 0.5)).m_82520_(0.0, body.m_20206_() * 0.5, 0.0);
            PaleBlitz.fxAround(body, owner == null ? 0 : owner.m_19879_(), S2CPaleFx.CRASH, at, velocity, (float)speed, 0);
            UkakuFire.sound(level, at, "entity.generic.big_fall", 1.0F, 0.8F);
        }
        return true;
    }

    /** At most this many blocks a tick out of the way of a body the Owl's punch drives. */
    static final int MOST_DRIVEN_A_TICK = 48;

    /** One tick of a body the Owl's Big Punch drives ({@link #drive}), before it moves: false once it is done. */
    private static boolean driven(ServerLevel level, Launch launch, LivingEntity body) {
        ServerPlayer owner = level.m_7654_().m_6846_().m_11259_(launch.owner);
        long now = level.m_46467_();
        double step = OwlRules.punchStep(launch.speed, launch.travelled, launch.distance);
        if (step < 0.05) {
            // Its force spent: it drops where it is.
            body.m_20256_(launch.line.m_82490_(0.1));
            body.f_19864_ = true;
            return false;
        }
        // Dead level: back onto the height it was struck at, whatever has pushed it up or down.
        double rise = Math.max(-0.6, Math.min(0.6, launch.height - body.m_20186_()));
        Vec3 velocity = new Vec3(launch.line.f_82479_ * step, rise, launch.line.f_82481_ * step);
        // Its way this tick, a hair wider all round but never down into what it skims along (the ground it was struck on).
        AABB swept = body.m_20191_().m_82369_(velocity);
        AABB path = new AABB(swept.f_82288_ - 0.05, swept.f_82289_ + 0.02, swept.f_82290_ - 0.05, swept.f_82291_ + 0.05, swept.f_82292_ + 0.05,
            swept.f_82293_ + 0.05);
        Vec3 from = body.m_20182_();
        int broke = 0;
        int firstState = 0;
        boolean blocked = false;
        double kept = 1.0;
        Vec3 hitAt = null;
        int minX = (int)Math.floor(path.f_82288_);
        int minY = (int)Math.floor(path.f_82289_);
        int minZ = (int)Math.floor(path.f_82290_);
        int maxX = (int)Math.floor(path.f_82291_);
        int maxY = (int)Math.floor(path.f_82292_);
        int maxZ = (int)Math.floor(path.f_82293_);
        for (int x = minX; x <= maxX && broke < MOST_DRIVEN_A_TICK; x++) {
            for (int y = minY; y <= maxY && broke < MOST_DRIVEN_A_TICK; y++) {
                for (int z = minZ; z <= maxZ && broke < MOST_DRIVEN_A_TICK; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.m_8055_(pos);
                    if (state.m_60795_() || !inPath(level, pos, state, path)) {
                        continue;
                    }
                    // How far along its flight this block is (from where it was struck).
                    double along = launch.travelled + Math.max(0.0, (x + 0.5 - from.f_82479_) * launch.line.f_82479_ + (z + 0.5 - from.f_82481_) * launch.line.f_82481_);
                    if (owner != null && along <= OwlRules.PUNCH_SMASH_REACH && launch.broken < OwlRules.PUNCH_MOST_BLOCKS && ControlConfig.blockSmash()
                        && breakable(level, pos, state, OwlRules.PUNCH_HARDEST) && smash(level, owner, pos, state, velocity)) {
                        if (broke == 0) {
                            firstState = Block.m_49956_(state);
                        }
                        broke++;
                        launch.broken++;
                        kept *= OwlRules.PUNCH_PER_BLOCK;
                    } else {
                        blocked = true;
                        if (hitAt == null) {
                            hitAt = new Vec3(x + 0.5, y + 0.5, z + 0.5);
                        }
                    }
                }
            }
        }
        if (broke > 0) {
            // The debris thrown on along its line, the dust and the crack of it, for everyone who sees it.
            Vec3 front = body.m_20182_().m_82549_(launch.line.m_82490_(body.m_20205_() * 0.5 + step * 0.5)).m_82520_(0.0, body.m_20206_() * 0.5, 0.0);
            PaleBlitz.fxAround(body, owner == null ? 0 : owner.m_19879_(), S2CPaleFx.SMASH, front, velocity, broke, firstState);
        }
        if (blocked) {
            // Into something that holds (too hard, or past its reach): stopped dead, and it hurts.
            double speed = launch.speed;
            body.m_20256_(new Vec3(0.0, Math.min(0.0, body.m_20184_().f_82480_), 0.0));
            body.f_19864_ = true;
            if (speed > CRASH_SPEED) {
                body.f_19802_ = 0;
                if (owner != null) {
                    body.m_6469_(owner.m_269291_().m_269075_(owner), (float)((speed - CRASH_SPEED) * CRASH_DAMAGE));
                }
            }
            Vec3 at = hitAt == null ? body.m_20182_().m_82520_(0.0, body.m_20206_() * 0.5, 0.0) : hitAt.m_82546_(launch.line.m_82490_(0.5));
            PaleBlitz.fxAround(body, owner == null ? 0 : owner.m_19879_(), S2CPaleFx.CRASH, at, velocity, (float)speed, 0);
            UkakuFire.sound(level, at, "entity.generic.big_fall", 1.2F, 0.7F);
            UkakuFire.sound(level, at, "entity.generic.explode", 0.6F, 1.3F);
            return false;
        }
        body.m_20256_(velocity);
        body.f_19864_ = true;
        body.f_19812_ = true;
        body.f_19789_ = 0.0F;
        PaleStun.stun(body, now, 3);
        launch.travelled += step;
        launch.speed = OwlRules.slowed(launch.speed * kept, launch.decel);
        if (owner != null) {
            bowl(level, owner, launch, body, path);
        }
        return true;
    }

    /** At most this many blocks a tick out of the way of a body the Speed Kick carries. */
    static final int MOST_CARRIED_A_TICK = 32;

    /** One tick of a body the Speed Kick carries ({@link #kick}), before it moves: false once it is done. */
    private static boolean carried(ServerLevel level, Launch launch, LivingEntity body) {
        ServerPlayer owner = level.m_7654_().m_6846_().m_11259_(launch.owner);
        long now = level.m_46467_();
        launch.flown++;
        double speed = PaleRules.kickCarried(launch.sent, launch.flown, launch.broken);
        if ((launch.flown > 2 && body.m_20096_()) || speed < SMASH_SPEED) {
            // Down (it skids to a stop on the ground, vanilla's friction), or its force spent: an ordinary body again.
            return false;
        }
        Vec3 was = body.m_20184_();
        Vec3 velocity = new Vec3(launch.carry.f_82479_ * speed, was.f_82480_, launch.carry.f_82481_ * speed);
        AABB path = body.m_20191_().m_82369_(velocity).m_82400_(0.05);
        int broke = 0;
        int firstState = 0;
        boolean blocked = false;
        Vec3 hitAt = null;
        int minX = (int)Math.floor(path.f_82288_);
        int minY = (int)Math.floor(path.f_82289_);
        int minZ = (int)Math.floor(path.f_82290_);
        int maxX = (int)Math.floor(path.f_82291_);
        int maxY = (int)Math.floor(path.f_82292_);
        int maxZ = (int)Math.floor(path.f_82293_);
        for (int x = minX; x <= maxX && broke < MOST_CARRIED_A_TICK; x++) {
            for (int y = minY; y <= maxY && broke < MOST_CARRIED_A_TICK; y++) {
                for (int z = minZ; z <= maxZ && broke < MOST_CARRIED_A_TICK; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.m_8055_(pos);
                    if (state.m_60795_() || !inPath(level, pos, state, path) || floor(body, y, velocity)) {
                        continue;
                    }
                    if (owner != null && launch.broken < PaleRules.KICK_MOST_BLOCKS && ControlConfig.blockSmash()
                        && breakable(level, pos, state, PaleRules.KICK_HARDEST) && smash(level, owner, pos, state, velocity)) {
                        if (broke == 0) {
                            firstState = Block.m_49956_(state);
                        }
                        broke++;
                        launch.broken++;
                    } else {
                        blocked = true;
                        if (hitAt == null) {
                            hitAt = new Vec3(x + 0.5, y + 0.5, z + 0.5);
                        }
                    }
                }
            }
        }
        if (broke > 0) {
            // The debris thrown on along its line, the dust and the crack of it, for everyone who sees it.
            Vec3 front = body.m_20182_().m_82549_(launch.carry.m_82490_(body.m_20205_() * 0.5 + speed * 0.5)).m_82520_(0.0, body.m_20206_() * 0.5, 0.0);
            PaleBlitz.fxAround(body, owner == null ? 0 : owner.m_19879_(), S2CPaleFx.SMASH, front, velocity, broke, firstState);
        }
        if (blocked) {
            // Into something that holds: stopped dead, and it hurts.
            body.m_20256_(new Vec3(0.0, Math.min(0.0, was.f_82480_), 0.0));
            body.f_19864_ = true;
            if (speed > CRASH_SPEED) {
                body.f_19802_ = 0;
                if (owner != null) {
                    body.m_6469_(owner.m_269291_().m_269075_(owner), (float)((speed - CRASH_SPEED) * CRASH_DAMAGE));
                }
            }
            Vec3 at = hitAt == null ? body.m_20182_().m_82520_(0.0, body.m_20206_() * 0.5, 0.0) : hitAt.m_82546_(launch.carry.m_82490_(0.5));
            PaleBlitz.fxAround(body, owner == null ? 0 : owner.m_19879_(), S2CPaleFx.CRASH, at, velocity, (float)speed, 0);
            UkakuFire.sound(level, at, "entity.generic.big_fall", 1.2F, 0.7F);
            UkakuFire.sound(level, at, "entity.generic.explode", 0.6F, 1.3F);
            return false;
        }
        launch.speed = speed;
        body.m_20256_(velocity);
        body.f_19864_ = true;
        body.f_19812_ = true;
        body.f_19789_ = 0.0F;
        PaleStun.stun(body, now, 3);
        if (owner != null) {
            bowl(level, owner, launch, body, path, launch.carry, PaleRules.KICK_BOWLED);
        }
        return true;
    }

    /** What the driven body slams into on its way: hurt (part of the blow) and thrown aside, off its line and on along it. */
    private static void bowl(ServerLevel level, ServerPlayer owner, Launch launch, LivingEntity body, AABB path) {
        bowl(level, owner, launch, body, path, launch.line, OwlRules.PUNCH_BOWLED);
    }

    /** ...along {@code line}, each hurt by {@code share} of the blow. */
    private static void bowl(ServerLevel level, ServerPlayer owner, Launch launch, LivingEntity body, AABB path, Vec3 line, float share) {
        for (LivingEntity other : level.m_6443_(LivingEntity.class, path.m_82400_(0.3), entity -> entity.m_6084_() && !launch.bowled.contains(entity.m_19879_())
            && PaleBlitz.valid(owner, entity))) {
            launch.bowled.add(other.m_19879_());
            Vec3 off = other.m_20182_().m_82546_(body.m_20182_());
            Vec3 side = new Vec3(-line.f_82481_, 0.0, line.f_82479_);
            double which = off.f_82479_ * side.f_82479_ + off.f_82481_ * side.f_82481_ >= 0.0 ? 1.0 : -1.0;
            other.f_19802_ = 0;
            if (other.m_6469_(owner.m_269291_().m_269075_(owner), launch.damage * share)) {
                Vec3 thrown = side.m_82490_(which * 1.1).m_82549_(line.m_82490_(Math.min(1.6, launch.speed * 0.45))).m_82520_(0.0, 0.35, 0.0);
                launch(owner, other, thrown, 20);
                PaleBlitz.fxAround(other, owner.m_19879_(), S2CPaleFx.IMPACT, other.m_20182_().m_82520_(0.0, other.m_20206_() * 0.5, 0.0), thrown, 1.4F,
                    S2CPaleFx.BIG_PUNCH);
            }
        }
    }

    /**
     * A blow into the ground at {@code at} (on its surface): the weak blocks of the ground's top within {@code radius}
     * knocked out, two deep in the middle, one round it, each as a launched body breaks one (only if it is hers to break, put back
     * exactly later). Nothing below the ground's top two layers, nothing tough. How many went.
     */
    public static int crater(ServerPlayer owner, Vec3 at, double radius) {
        if (!ControlConfig.blockSmash()) {
            return 0;
        }
        ServerLevel level = owner.m_284548_();
        int reach = (int)Math.ceil(radius);
        int broke = 0;
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                double off = Math.sqrt(dx * dx + dz * dz);
                if (off > radius) {
                    continue;
                }
                int deep = off < radius * 0.45 ? 2 : 1;
                int dug = 0;
                // The column's top: the first solid block down from just above the blow.
                for (int dy = 1; dy >= -2 && dug < deep && broke < MOST_A_LAUNCH; dy--) {
                    BlockPos pos = BlockPos.m_274561_(at.f_82479_ + dx, at.f_82480_ - 0.01 + dy, at.f_82481_ + dz);
                    BlockState state = level.m_8055_(pos);
                    if (state.m_60795_()) {
                        if (dug > 0) {
                            break;
                        }
                        continue;
                    }
                    if (!weak(level, pos, state) || !smash(level, owner, pos, state, Vec3.f_82478_)) {
                        break;
                    }
                    dug++;
                    broke++;
                }
            }
        }
        return broke;
    }

    /**
     * The ground it slides or skims along: the block row under its feet, unless it is slamming down into it (then it craters).
     */
    private static boolean floor(LivingEntity body, int y, Vec3 velocity) {
        return y < Math.floor(body.m_20186_() + 0.01) && velocity.f_82480_ > -SLAM_SPEED;
    }

    /** True when {@code state}'s collision box at {@code pos} lies in {@code path}. */
    private static boolean inPath(ServerLevel level, BlockPos pos, BlockState state, AABB path) {
        VoxelShape shape = state.m_60812_(level, pos);
        if (shape.m_83281_()) {
            return false;
        }
        for (AABB box : shape.m_83299_()) {
            if (box.m_82338_(pos).m_82381_(path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Weak enough to smash: breakable, no tougher than wood, needs no pickaxe (stone, ores, bricks, metal and the like all do) nor
     * the right tool for its drops, and if it has a block entity, a vanilla one (its contents can be put back exactly).
     */
    static boolean weak(ServerLevel level, BlockPos pos, BlockState state) {
        float hardness = state.m_60800_(level, pos);
        if (hardness < 0.0F || hardness > HARDEST || state.m_204336_(BlockTags.f_144282_) || state.m_60834_()) {
            return false;
        }
        BlockEntity entity = level.m_7702_(pos);
        if (entity == null) {
            return true;
        }
        ResourceLocation type = BlockEntityType.m_58954_(entity.m_58903_());
        return type != null && "minecraft".equals(type.m_135827_());
    }

    /**
     * 6.2.0: breakable by the Owl's punch: anything not unbreakable up to {@code hardest} (a pickaxe's blocks too), and if it has a
     * block entity, a vanilla one (its contents can be put back exactly).
     */
    static boolean breakable(ServerLevel level, BlockPos pos, BlockState state, float hardest) {
        float hardness = state.m_60800_(level, pos);
        if (hardness < 0.0F || hardness > hardest) {
            return false;
        }
        BlockEntity entity = level.m_7702_(pos);
        if (entity == null) {
            return true;
        }
        ResourceLocation type = BlockEntityType.m_58954_(entity.m_58903_());
        return type != null && "minecraft".equals(type.m_135827_());
    }

    /** Takes the block out (if it is hers to break) and remembers it to put back; true when it is out. */
    private static boolean smash(ServerLevel level, ServerPlayer owner, BlockPos pos, BlockState state, Vec3 velocity) {
        Waiting waiting = Waiting.of(level);
        if (waiting.blocks.size() >= MOST_WAITING || !owner.m_36326_() || !level.m_7966_(owner, pos)) {
            return false;
        }
        if (MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, owner))) {
            return false; // claimed, protected, or otherwise not hers to break
        }
        BlockEntity entity = level.m_7702_(pos);
        CompoundTag data = entity == null ? null : entity.m_187480_();
        Clearable.m_18908_(entity);
        // Out as /setblock takes a block out: the clients told, no neighbour updates (what hangs on it stays), nothing dropped.
        level.m_7731_(pos, Blocks.f_50016_.m_49966_(), 2 | 16);
        level.m_46796_(2001, pos, Block.m_49956_(state));
        waiting.add(new Smashed(pos, state, data, level.m_46467_() + ControlConfig.blockSmashRestoreSeconds() * 20L));
        return true;
    }

    /** One block out, waiting to be put back. */
    record Smashed(BlockPos pos, BlockState state, CompoundTag data, long at) {
    }

    /** The blocks waiting to be put back in one level, saved with it. */
    static final class Waiting extends SavedData {
        static final String NAME = "hexkagune_smashed_blocks";
        final List<Smashed> blocks = new ArrayList<>();

        static Waiting of(ServerLevel level) {
            return level.m_8895_().m_164861_(tag -> load(level, tag), Waiting::new, NAME);
        }

        void add(Smashed smashed) {
            this.blocks.add(smashed);
            this.m_77762_();
        }

        /** Puts back what is due (everything, {@code all}) where the place is free, lowest first (sand never falls into a hole); the rest waits. */
        void restore(ServerLevel level, long now, boolean all) {
            boolean changed = false;
            this.blocks.sort(java.util.Comparator.comparingInt(smashed -> smashed.pos().m_123342_()));
            for (int i = 0; i < this.blocks.size(); i++) {
                Smashed smashed = this.blocks.get(i);
                if (!all && now < smashed.at() || !level.m_46805_(smashed.pos())) {
                    continue;
                }
                if (!free(level.m_8055_(smashed.pos()))) {
                    // Someone built there: never overwrite it; try again in a while.
                    this.blocks.set(i, new Smashed(smashed.pos(), smashed.state(), smashed.data(), now + RETRY_TICKS));
                    changed = true;
                    continue;
                }
                put(level, smashed);
                this.blocks.remove(i--);
                changed = true;
            }
            if (changed) {
                this.m_77762_();
            }
        }

        /** Air, or water or lava that has flowed in (not a source someone poured): the hole is still the hole. */
        private static boolean free(BlockState state) {
            return state.m_60795_() || state.m_60734_() instanceof net.minecraft.world.level.block.LiquidBlock && !state.m_60819_().m_76170_();
        }

        private static void put(ServerLevel level, Smashed smashed) {
            level.m_7731_(smashed.pos(), smashed.state(), 3);
            if (smashed.data() != null) {
                BlockEntity entity = level.m_7702_(smashed.pos());
                if (entity != null) {
                    entity.m_142466_(smashed.data());
                    entity.m_6596_();
                }
            }
            Vec3 at = Vec3.m_82512_(smashed.pos());
            PaleBlitz.fxNear(level, S2CPaleFx.REGROW, at, 1.0F, Block.m_49956_(smashed.state()));
        }

        @Override
        public CompoundTag m_7176_(CompoundTag tag) {
            ListTag list = new ListTag();
            for (Smashed smashed : this.blocks) {
                CompoundTag entry = new CompoundTag();
                entry.m_128365_("pos", NbtUtils.m_129224_(smashed.pos()));
                entry.m_128365_("state", NbtUtils.m_129202_(smashed.state()));
                if (smashed.data() != null) {
                    entry.m_128365_("data", smashed.data());
                }
                entry.m_128356_("at", smashed.at());
                list.add(entry);
            }
            tag.m_128365_("blocks", list);
            return tag;
        }

        static Waiting load(ServerLevel level, CompoundTag tag) {
            Waiting waiting = new Waiting();
            ListTag list = tag.m_128437_("blocks", 10);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.m_128728_(i);
                BlockState state = NbtUtils.m_247651_(level.m_246945_(Registries.f_256747_), entry.m_128469_("state"));
                if (state.m_60795_()) {
                    continue; // a block from a mod no longer there: nothing to put back
                }
                CompoundTag data = entry.m_128441_("data") ? entry.m_128469_("data") : null;
                waiting.blocks.add(new Smashed(NbtUtils.m_129239_(entry.m_128469_("pos")), state, data, entry.m_128454_("at")));
            }
            return waiting;
        }
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) {
        // Everything back before the world is saved for the last time.
        for (ServerLevel level : event.getServer().m_129785_()) {
            try {
                Waiting.of(level).restore(level, level.m_46467_(), true);
            } catch (RuntimeException failure) {
                KaguneMod.LOGGER.error("HexKagune could not put smashed blocks back in " + level, failure);
            }
        }
        FLYING.clear();
    }
}
