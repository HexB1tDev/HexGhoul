package com.hex.kagune.control.awakening;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Dragon's sense of the ground: the top of the original terrain per column, cached on first look
 * so the holes it tears do not drag it deeper. Water and lava count as a surface. {@link #terrain}
 * also looks through leaves and logs, so the body crawls on the land under a forest and ploughs
 * through the trees.
 */
final class TerrainProbe implements DragonRig.Ground {
    private static final int SCAN_UP = 56;
    private static final int SCAN_DOWN = 72;
    private final ServerLevel level;
    private final int referenceY;
    private final Map<Long, Integer> columns = new HashMap<>();
    private final Map<Long, Integer> land = new HashMap<>();
    private final Map<Long, Boolean> liquidTops = new HashMap<>();

    TerrainProbe(ServerLevel level, double referenceY) {
        this.level = level;
        this.referenceY = (int) Math.floor(referenceY);
    }

    @Override
    public double surface(double x, double z) {
        return column((int) Math.floor(x), (int) Math.floor(z)) + 1.0;
    }

    @Override
    public double terrain(double x, double z) {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        long key = key(bx, bz);
        Integer cached = land.get(key);
        if (cached == null) {
            cached = scanLand(bx, bz);
            land.put(key, cached);
        }
        return cached + 1.0;
    }

    /** Logs, wood and bamboo blocks burn; deepslate, basalt and quartz pillars do not and stay ground. */
    private static boolean tree(BlockState state) {
        return state.getBlock() instanceof LeavesBlock || (state.getBlock() instanceof RotatedPillarBlock && state.ignitedByLava());
    }

    private int scanLand(int x, int z) {
        int top = Math.min(level.getMaxBuildHeight() - 1, referenceY + SCAN_UP);
        int bottom = Math.max(level.getMinBuildHeight(), referenceY - SCAN_DOWN);
        if (!level.isLoaded(new BlockPos(x, top, z))) return referenceY - 1;
        for (int y = top; y >= bottom; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (!state.getFluidState().isEmpty()) return y;
            if (!state.getCollisionShape(level, pos).isEmpty() && !tree(state)) return y;
        }
        return bottom;
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    private int column(int x, int z) {
        long key = key(x, z);
        Integer cached = columns.get(key);
        if (cached == null) {
            cached = scan(x, z, key);
            columns.put(key, cached);
        }
        return cached;
    }

    private int scan(int x, int z, long key) {
        int top = Math.min(level.getMaxBuildHeight() - 1, referenceY + SCAN_UP);
        int bottom = Math.max(level.getMinBuildHeight(), referenceY - SCAN_DOWN);
        BlockPos probe = new BlockPos(x, top, z);
        if (!level.isLoaded(probe)) {
            liquidTops.put(key, true);
            return referenceY - 1;
        }
        for (int y = top; y >= bottom; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);
            boolean liquid = !state.getFluidState().isEmpty();
            if (liquid || !state.getCollisionShape(level, pos).isEmpty()) {
                liquidTops.put(key, liquid);
                return y;
            }
        }
        liquidTops.put(key, true);
        return bottom;
    }

    /** Current top of a column, ignoring the cache; used once the ground under Kaneki has been repaired. */
    double rescan(double x, double z) {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        long key = key(bx, bz);
        int y = scan(bx, bz, key);
        columns.put(key, y);
        return y + 1.0;
    }

    /** Nearest column (searching outwards) whose original top is solid ground rather than liquid. */
    V3 solidNear(V3 near, int radius) {
        int cx = (int) Math.floor(near.x());
        int cz = (int) Math.floor(near.z());
        for (int ring = 0; ring <= radius; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                    int y = column(cx + dx, cz + dz);
                    if (!liquidTops.getOrDefault(key(cx + dx, cz + dz), true)) return new V3(cx + dx + 0.5, y + 1.0, cz + dz + 0.5);
                }
            }
        }
        return null;
    }

    @Override
    public double minY() {
        return level.getMinBuildHeight();
    }

    @Override
    public double maxY() {
        return level.getMaxBuildHeight();
    }
}
