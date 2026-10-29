package com.hex.kagune.control.server;

import com.hex.kagune.control.UrgeType;

/** Transient per-player control data on the logical server. Persistent progress lives in KaguneProfile NBT. */
final class ServerControlState {
    float control;
    float hunger;
    float frenzy;
    long controlRefreshAt = Long.MIN_VALUE;

    long lastManifestStart = Long.MIN_VALUE;
    boolean manifestSettled = true;
    long nextEnduranceAwardAt;

    UrgeType urge = UrgeType.NONE;
    long urgeStarted;
    int urgeDuration;
    int urgeTargetId;
    float urgeStrength;
    float urgeInstinct;
    boolean urgeRestrained;
    boolean urgeRestrainAttempted;
    boolean urgeActed;
    long nextUrgeCheckAt;
    long urgeCooldownUntil;
    long nextRestraintAwardAt;
    int urgeHintsShown;

    int preEatFood;
    float preEatSaturation;
    boolean preEatHadHunger;
    long preEatTick = -1L;
    Object preEatItem;
    long nextFeedAwardAt;
    long nextDietMessageAt;

    boolean dirty = true;
    long nextSelfSyncAt;
    long nextTrackerSyncAt;
    float syncedControl = -1.0F;
    float syncedHunger = -1.0F;
    float syncedFrenzy = -1.0F;

    void clearUrge() {
        this.urge = UrgeType.NONE;
        this.urgeStarted = 0L;
        this.urgeDuration = 0;
        this.urgeTargetId = 0;
        this.urgeStrength = 0.0F;
        this.urgeInstinct = 0.0F;
        this.urgeRestrained = false;
        this.urgeRestrainAttempted = false;
        this.urgeActed = false;
    }
}
