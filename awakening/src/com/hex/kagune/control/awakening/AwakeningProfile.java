package com.hex.kagune.control.awakening;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/**
 * Awakening progress stored beside the kagune profile in the player's persisted data, so it survives
 * death, dimension changes and the kagune profile reset command.
 */
public final class AwakeningProfile {
    private static final String PLAYER_PERSISTED = "PlayerPersisted";
    private static final String ROOT = "HexKaguneAwakening";
    private static final String STAGE = "Stage";
    private static final String FINAL_KAKUJA = "FinalKakuja";
    private static final String EMERGED_AT = "EmergedAt";
    private static final String DRAGON_X = "DragonX";
    private static final String DRAGON_Y = "DragonY";
    private static final String DRAGON_Z = "DragonZ";
    private static final int TAG_COMPOUND = 10;

    private AwakeningProfile() {
    }

    private static CompoundTag persisted(ServerPlayer player) {
        CompoundTag outer = player.getPersistentData();
        if (!outer.contains(PLAYER_PERSISTED, TAG_COMPOUND)) outer.put(PLAYER_PERSISTED, new CompoundTag());
        return outer.getCompound(PLAYER_PERSISTED);
    }

    private static CompoundTag root(ServerPlayer player) {
        CompoundTag persisted = persisted(player);
        if (!persisted.contains(ROOT, TAG_COMPOUND)) persisted.put(ROOT, new CompoundTag());
        return persisted.getCompound(ROOT);
    }

    public static AwakeningStage stage(ServerPlayer player) {
        return AwakeningStage.of(root(player).getInt(STAGE));
    }

    public static void setStage(ServerPlayer player, AwakeningStage stage) {
        root(player).putInt(STAGE, stage.ordinal());
    }

    public static boolean finalKakuja(ServerPlayer player) {
        return root(player).getBoolean(FINAL_KAKUJA);
    }

    static void setFinalKakuja(ServerPlayer player, long now) {
        CompoundTag tag = root(player);
        tag.putBoolean(FINAL_KAKUJA, true);
        tag.putLong(EMERGED_AT, now);
    }

    /** Remembers an in-progress awakening so a crash or disconnect can still complete it. */
    static void beginDragon(ServerPlayer player, V3 anchor) {
        CompoundTag tag = root(player);
        tag.putInt(STAGE, AwakeningStage.DRAGON.ordinal());
        tag.putDouble(DRAGON_X, anchor.x());
        tag.putDouble(DRAGON_Y, anchor.y());
        tag.putDouble(DRAGON_Z, anchor.z());
    }

    static V3 dragonAnchor(ServerPlayer player) {
        CompoundTag tag = root(player);
        return tag.contains(DRAGON_Y) ? new V3(tag.getDouble(DRAGON_X), tag.getDouble(DRAGON_Y), tag.getDouble(DRAGON_Z)) : null;
    }

    static void clearDragon(ServerPlayer player) {
        CompoundTag tag = root(player);
        tag.remove(DRAGON_X);
        tag.remove(DRAGON_Y);
        tag.remove(DRAGON_Z);
    }

    public static void copy(ServerPlayer original, ServerPlayer clone) {
        persisted(clone).put(ROOT, root(original).copy());
    }

    public static void reset(ServerPlayer player) {
        persisted(player).put(ROOT, new CompoundTag());
    }
}
