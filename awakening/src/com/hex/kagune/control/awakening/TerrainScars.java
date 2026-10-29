package com.hex.kagune.control.awakening;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.ScaffoldingBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Temporary, self-repairing terrain destruction.
 *
 * <p>Blocks are removed and restored without neighbour updates, so sand does not fall, water does
 * not flow in and attached blocks do not pop off while a hole exists. Nothing is ever dropped:
 * anything with a block entity (chests, furnaces, beds, spawners...), fluids, unbreakable blocks,
 * the walls behind paintings and item frames, and redstone or multi-block parts that react to their
 * own placement are left untouched. Restores happen
 * 15–25 seconds after destruction, are deferred while a creature stands in the hole, skip positions a
 * player has since built on, and are all forced on server shutdown or level unload.
 */
public final class TerrainScars {
    private static final int SILENT = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final int DESTROY_BUDGET = 700;
    private static final int CHECK_BUDGET = 30_000;
    private static final int RESTORE_BUDGET = 900;
    private static final long RESTORE_MIN = 300L;
    private static final long RESTORE_JITTER = 200L;
    private static final int LEVEL_EVENT_BLOCK_BREAK = 2001;
    /** A 4x4 painting's farthest support block is about 2.1 blocks from the painting's centre. */
    private static final double HANGING_CLEARANCE = 2.6;
    private static final Map<ServerLevel, TerrainScars> LEVELS = new IdentityHashMap<>();

    private final ServerLevel level;
    private final ScarLedger<BlockState> ledger = new ScarLedger<>();
    private final List<V3> supports = new ArrayList<>();
    private long budgetTick = Long.MIN_VALUE;
    private int destroyed;
    private int checked;
    private int effects;

    private TerrainScars(ServerLevel level) {
        this.level = level;
    }

    public static TerrainScars of(ServerLevel level) {
        return LEVELS.computeIfAbsent(level, TerrainScars::new);
    }

    static void tickAll() {
        LEVELS.values().removeIf(scars -> {
            scars.restoreDue();
            return scars.ledger.size() == 0;
        });
    }

    /** Restores everything immediately; used on shutdown and level unload. */
    static void restoreAll() {
        for (TerrainScars scars : LEVELS.values()) scars.restoreEverything();
        LEVELS.clear();
    }

    static void restoreAll(ServerLevel level) {
        TerrainScars scars = LEVELS.remove(level);
        if (scars != null) scars.restoreEverything();
    }

    /** Schedules every pending repair in this level to finish within the given number of ticks. */
    void hastenAll(long withinTicks) {
        long now = level.getGameTime();
        ledger.hasten(now, now + withinTicks);
    }

    int pending() {
        return ledger.size();
    }

    private void refreshBudget() {
        long now = level.getGameTime();
        if (now != budgetTick) {
            budgetTick = now;
            destroyed = 0;
            checked = 0;
            effects = 0;
        }
    }

    /** Carves a sphere; returns how many blocks were destroyed. */
    int carveSphere(ServerPlayer owner, V3 center, double radius, double floorY) {
        refreshBudget();
        if (radius < 0.6) return 0;
        int count = 0;
        int r = (int) Math.ceil(radius);
        // Paintings, item frames and leash knots drop for good when their wall vanishes: keep their supports.
        double reach = radius + HANGING_CLEARANCE;
        List<HangingEntity> hanging = level.getEntitiesOfClass(HangingEntity.class,
                new AABB(center.x() - reach, center.y() - reach, center.z() - reach, center.x() + reach, center.y() + reach, center.z() + reach), HangingEntity::isAlive);
        supports.clear();
        for (HangingEntity entity : hanging) supports.add(new V3(entity.getX(), entity.getY(), entity.getZ()));
        int cx = (int) Math.floor(center.x());
        int cy = (int) Math.floor(center.y());
        int cz = (int) Math.floor(center.z());
        double limit = radius * radius;
        int minY = Math.max(level.getMinBuildHeight() + 1, (int) Math.floor(floorY));
        int maxY = level.getMaxBuildHeight() - 1;
        for (int dy = -r; dy <= r; dy++) {
            int y = cy + dy;
            if (y < minY || y > maxY) continue;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dy * dy + dz * dz > limit) continue;
                    if (destroyed >= DESTROY_BUDGET || checked >= CHECK_BUDGET || ledger.full()) return count;
                    if (carve(owner, new BlockPos(cx + dx, y, cz + dz))) count++;
                }
            }
        }
        return count;
    }

    private boolean carve(ServerPlayer owner, BlockPos pos) {
        long key = pos.asLong();
        if (ledger.contains(key)) return false;
        checked++;
        for (V3 support : supports) {
            double dx = pos.getX() + 0.5 - support.x();
            double dy = pos.getY() + 0.5 - support.y();
            double dz = pos.getZ() + 0.5 - support.z();
            if (dx * dx + dy * dy + dz * dz < HANGING_CLEARANCE * HANGING_CLEARANCE) return false;
        }
        if (!level.isLoaded(pos) || !level.mayInteract(owner, pos)) return false;
        BlockState state = level.getBlockState(pos);
        if (!carvable(state, pos)) return false;
        long now = level.getGameTime();
        long restoreAt = now + RESTORE_MIN + Math.floorMod(key * 0x9E3779B97F4A7C15L, RESTORE_JITTER);
        if (!ledger.record(key, state, restoreAt)) return false;
        if (effects < 24 && Math.floorMod(key, 7L) == 0L) {
            effects++;
            level.levelEvent(null, LEVEL_EVENT_BLOCK_BREAK, pos, Block.getId(state));
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), SILENT);
        destroyed++;
        return true;
    }

    private boolean carvable(BlockState state, BlockPos pos) {
        if (state.isAir() || !state.getFluidState().isEmpty() || state.hasBlockEntity()) return false;
        if (state.getDestroySpeed(level, pos) < 0.0F) return false;
        Block block = state.getBlock();
        return !(block instanceof TntBlock
                || block instanceof BaseRailBlock
                || block instanceof RedStoneWireBlock
                || block instanceof DiodeBlock
                || block instanceof PistonBaseBlock
                || block instanceof PistonHeadBlock
                || block instanceof ObserverBlock
                || block instanceof LeverBlock
                || block instanceof ButtonBlock
                || block instanceof BasePressurePlateBlock
                || block instanceof RedstoneTorchBlock
                || block instanceof DoorBlock
                || block instanceof NetherPortalBlock
                || block instanceof ScaffoldingBlock);
    }

    private void restoreDue() {
        long now = level.getGameTime();
        int restored = 0;
        ScarLedger.Scar<BlockState> scar;
        while (restored < RESTORE_BUDGET && (scar = ledger.poll(now)) != null) {
            BlockPos pos = BlockPos.of(scar.position);
            if (!level.isLoaded(pos)) {
                ledger.defer(scar, now + 100L, false);
                continue;
            }
            BlockState current = level.getBlockState(pos);
            if (!replaceable(current)) {
                // Someone built here since: respect their block.
                ledger.finish(scar);
                continue;
            }
            if (scar.original.getBlock() instanceof FallingBlock && ledger.contains(BlockPos.of(scar.position).below().asLong())) {
                // Restore supports first so sand and gravel never fall into a hole that is about to close.
                ledger.defer(scar, now + 2L, false);
                continue;
            }
            List<LivingEntity> inside = level.getEntitiesOfClass(LivingEntity.class, new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 1.0), LivingEntity::isAlive);
            // Never seal a ceiling directly over a player's head either.
            boolean playerInside = !level.getEntitiesOfClass(Player.class, new AABB(pos.getX() - 0.3, pos.getY() - 1.5, pos.getZ() - 0.3, pos.getX() + 1.3, pos.getY() + 1.0, pos.getZ() + 1.3), Player::isAlive).isEmpty();
            if (playerInside || (!inside.isEmpty() && scar.retries < 15)) {
                ledger.defer(scar, now + 20L, true);
                continue;
            }
            level.setBlock(pos, scar.original, SILENT);
            ledger.finish(scar);
            restored++;
        }
    }

    /** Immediately closes every hole within the radius, lowest blocks first, so Rinkaku lands on intact ground. */
    void restoreNear(V3 center, double radius) {
        List<ScarLedger.Scar<BlockState>> close = new ArrayList<>();
        double limit = radius * radius;
        for (ScarLedger.Scar<BlockState> scar : ledger.all()) {
            BlockPos pos = BlockPos.of(scar.position);
            double dx = pos.getX() + 0.5 - center.x();
            double dy = pos.getY() + 0.5 - center.y();
            double dz = pos.getZ() + 0.5 - center.z();
            if (dx * dx + dy * dy + dz * dz <= limit) close.add(scar);
        }
        close.sort((a, b) -> Integer.compare(BlockPos.of(a.position).getY(), BlockPos.of(b.position).getY()));
        for (ScarLedger.Scar<BlockState> scar : close) {
            BlockPos pos = BlockPos.of(scar.position);
            if (replaceable(level.getBlockState(pos))) level.setBlock(pos, scar.original, SILENT);
            ledger.finish(scar);
        }
    }

    private void restoreEverything() {
        for (ScarLedger.Scar<BlockState> scar : ledger.all()) {
            BlockPos pos = BlockPos.of(scar.position);
            if (replaceable(level.getBlockState(pos))) level.setBlock(pos, scar.original, SILENT);
        }
        ledger.clear();
    }

    private static boolean replaceable(BlockState current) {
        return current.isAir() || !current.getFluidState().isEmpty() || current.getBlock() instanceof BaseFireBlock;
    }
}
