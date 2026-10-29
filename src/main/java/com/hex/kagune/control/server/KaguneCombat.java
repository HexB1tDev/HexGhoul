package com.hex.kagune.control.server;

import com.hex.kagune.control.ClashRules;
import com.hex.kagune.control.ComboStage;
import com.hex.kagune.control.ComboStyle;
import com.hex.kagune.control.ControlConfig;
import com.hex.kagune.control.ControlMath;
import com.hex.kagune.control.HitGeometry;
import com.hex.kagune.control.HitShape;
import com.hex.kagune.control.StrikePattern;
import com.hex.kagune.control.ayato.AyatoMutation;
import com.hex.kagune.control.ayato.AyatoRules;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.KaguneForm;
import com.hex.kagune.gameplay.KaguneRuntime;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.gameplay.MutationNode;
import com.hex.kagune.gameplay.TendrilMode;
import com.hex.kagune.network.KaguneNetwork;
import com.hex.kagune.sound.HexKaguneSounds;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Server-authoritative M1 combos. Clicks only ever ask the server to strike; stage choice, timing, the
 * hit volume and damage are all resolved here at the impact tick against server positions, with each
 * entity damaged at most once per strike. Clients receive the stage through {@code S2CControlState}
 * purely to animate it. Ayato-path players fight with kicks and wing strikes instead of tendrils
 * ({@link AyatoRules#style}); everything else about the combo (timing, buffering, hit volumes) is shared.
 */
public final class KaguneCombat {
    private static final Map<UUID, Combo> COMBOS = new HashMap<>();
    /** A click this many ticks before the next strike may begin is remembered instead of dropped. */
    private static final int INPUT_BUFFER_TICKS = 6;

    private KaguneCombat() {
    }

    static final class Combo {
        ComboStyle style = ComboStyle.WHIP;
        int stage = -1;
        long startedAt = Long.MIN_VALUE;
        int duration;
        int impactTick;
        int limbMask;
        float deviation;
        boolean involuntary;
        boolean impactDone = true;
        long chainOpenAt;
        long lastEndAt = Long.MIN_VALUE;
        Vec3 direction = Vec3.f_82478_;
        double range;
        long queuedAt = Long.MIN_VALUE;
        int queuedHintId;
        /** A right click waiting for the shot in progress to open the chain (Ayato's enders), and whether W was held. */
        long queuedEnderAt = Long.MIN_VALUE;
        int queuedEnderHintId;
        boolean queuedEnderForward;
        /** Game time each ender may next be used (AyatoRules.enderCooldown). */
        final long[] enderReadyAt = new long[ComboStyle.AYATO_ENDERS.stageCount()];

        boolean owns(KaguneRuntime.State state) {
            return state.mode == TendrilMode.SWIPE && state.modeStarted == this.startedAt;
        }
    }

    static Combo peek(ServerPlayer player) {
        return COMBOS.get(player.m_20148_());
    }

    static void forget(ServerPlayer player) {
        COMBOS.remove(player.m_20148_());
    }

    /** 6.2.0: every ender ready again now ({@link Cooldowns}). */
    static void clearCooldowns(ServerPlayer player) {
        Combo combo = COMBOS.get(player.m_20148_());
        if (combo != null) {
            java.util.Arrays.fill(combo.enderReadyAt, Long.MIN_VALUE);
        }
    }

    /** The pattern of the strike this player's tails are making right now, or null (3.0.3: how its wounds look). */
    static StrikePattern livePattern(ServerPlayer player, KaguneRuntime.State state) {
        Combo combo = COMBOS.get(player.m_20148_());
        return combo != null && state != null && combo.owns(state) && combo.stage >= 0 ? combo.style.stage(combo.stage).pattern() : null;
    }

    private static Combo combo(ServerPlayer player) {
        return COMBOS.computeIfAbsent(player.m_20148_(), ignored -> new Combo());
    }

    /** The combo system runs for this player: always on the Ayato path (its kit is the combo), else per config. */
    private static boolean combos(ServerPlayer player) {
        return ControlConfig.kaguneCombos() || AyatoServer.isAyato(player);
    }

    /** Replaces {@code KaguneServerController.startSwipe} (bytecode trampoline): every M1 and vanilla-attack swipe lands here. */
    public static void startSwipe(ServerPlayer player, Entity hint) {
        KakujaPassive.attacked(player);
        if (EtoServer.isEto(player)) {
            EtoBlitz.click(player, hint); // 6.1.0: Eto fights with her fists (the Speed Blitz)
            return;
        }
        if (FurutaServer.isFuruta(player)) {
            FurutaServer.strike(player, hint); // 7.0.0: Furuta's limbs' string, or his blade and the kagune
            return;
        }
        if (KanekiAutonomy.refuse(player)) {
            return; // the first kakuja's legs fight on their own; he has no M1 in it
        }
        if (!combos(player)) {
            KaguneServerController.startSwipeLegacy(player, hint);
            return;
        }
        if (AyatoGatling.firing(player)) {
            return; // Ayato's wings are busy firing the Gatling
        }
        KaguneRuntime.State state = KaguneRuntime.get(player.m_20148_());
        long now = player.m_9236_().m_46467_();
        Combo combo = combo(player);
        if (!tryBegin(player, state, combo, hint, now, false, null, -1)) {
            boolean busyAttacking = state.mode == TendrilMode.SWIPE || state.mode == TendrilMode.RETRACT || now < state.inputCooldownUntil;
            if (busyAttacking && !state.hasManualHeldEntities()) {
                combo.queuedAt = now;
                combo.queuedHintId = hint == null ? 0 : hint.m_19879_();
            }
        }
    }

    /** Instinct-forced strike at {@code prey}. Returns true if the kagune actually lashed out. */
    static boolean startInvoluntary(ServerPlayer player, LivingEntity prey) {
        if (EtoServer.isEto(player) || FurutaServer.isFuruta(player)) {
            return false; // 6.1.0: Eto's flesh never lashes out on its own (7.0.0: nor Furuta's limbs: he is in control)
        }
        KaguneRuntime.State state = KaguneRuntime.get(player.m_20148_());
        long now = player.m_9236_().m_46467_();
        if (!combos(player)) {
            if (KaguneServerController.committed(state.mode) || now < state.inputCooldownUntil) {
                return false;
            }
            KaguneServerController.startSwipeLegacy(player, prey);
            return state.mode == TendrilMode.SWIPE && state.modeStarted == now;
        }
        return tryBegin(player, state, combo(player), prey, now, true, null, -1);
    }

    /**
     * Ayato's right click (C2SAyatoAction): the ender for the shots fired so far in this chain ({@link AyatoRules#ender}),
     * from the fire's set or, in the crystal mode, the crystal's. Pressed while a shot is still playing, it waits for that
     * shot to open the chain.
     */
    public static void startEnder(ServerPlayer player, Entity hint) {
        startEnder(player, hint, false);
    }

    /** {@link #startEnder(ServerPlayer, Entity)}; {@code forward} when W was held (Fire Dash dashes forward instead of the hop). */
    public static void startEnder(ServerPlayer player, Entity hint, boolean forward) {
        KakujaPassive.attacked(player);
        if (!AyatoServer.isAyato(player) || !AyatoServer.comboStyle(player, player.m_9236_().m_46467_()).ayatoShots()) {
            return;
        }
        KaguneRuntime.State state = KaguneRuntime.get(player.m_20148_());
        long now = player.m_9236_().m_46467_();
        Combo combo = combo(player);
        if (!tryEnder(player, state, combo, hint, now, forward)) {
            boolean shooting = combo.style.ayatoShots() && combo.owns(state);
            if (shooting && !state.hasManualHeldEntities()) {
                combo.queuedEnderAt = now;
                combo.queuedEnderHintId = hint == null ? 0 : hint.m_19879_();
                combo.queuedEnderForward = forward;
                combo.queuedAt = Long.MIN_VALUE;
            }
        }
    }

    /** Starts a move of its own ({@code style}'s first stage, like the kakuja's Rushing Death); true if it started. */
    static boolean startArt(ServerPlayer player, ComboStyle style) {
        KaguneRuntime.State state = KaguneRuntime.get(player.m_20148_());
        return tryBegin(player, state, combo(player), null, player.m_9236_().m_46467_(), false, style, 0);
    }

    /** Starts the ender this chain earned, if it may start now (from the set of the mode he is in now). */
    private static boolean tryEnder(ServerPlayer player, KaguneRuntime.State state, Combo combo, Entity hint, long now, boolean forward) {
        ComboStyle shooting = AyatoServer.comboStyle(player, now);
        if (!shooting.ayatoShots()) {
            return false;
        }
        boolean live = combo.style == shooting && combo.stage >= 0
            && (combo.owns(state) || now - combo.lastEndAt <= AyatoRules.ENDER_WINDOW_TICKS);
        int shots = live ? combo.stage + 1 : 0;
        int ender = AyatoRules.ender(shots, KaguneProfile.mastery(player), shooting.kakujaShots());
        if (ender == 0 && forward && !shooting.kakujaShots() && AyatoServer.hasMutation(player, AyatoMutation.FIRE_DASH)) {
            // Fire Dash: W and right click, where the hop would throw him back, dashes him forward through everything instead.
            if (now < combo.enderReadyAt[0]) {
                return false;
            }
            if (!tryBegin(player, state, combo, hint, now, false, ComboStyle.AYATO_ENDERS, AyatoRules.RABBIT_DASH_ENDER)) {
                return false;
            }
            combo.enderReadyAt[0] = now + Cooldowns.of(player, AyatoRules.enderCooldown(AyatoRules.RABBIT_DASH_ENDER));
            return true;
        }
        if (now < combo.enderReadyAt[ender]) {
            return false;
        }
        if (!tryBegin(player, state, combo, hint, now, false, shooting.ayatoEnders(), ender)) {
            return false;
        }
        combo.enderReadyAt[ender] = now + Cooldowns.of(player, AyatoRules.enderCooldown(ender));
        return true;
    }

    /** Runs after the original controller each server tick: executes a buffered click once the chain window opens. */
    static void serverTick(ServerPlayer player, KaguneRuntime.State state, long now) {
        Combo combo = COMBOS.get(player.m_20148_());
        if (combo == null) {
            return;
        }
        if (combo.queuedEnderAt != Long.MIN_VALUE) {
            if (now - combo.queuedEnderAt > AyatoRules.ENDER_WINDOW_TICKS || !AyatoServer.isAyato(player)) {
                combo.queuedEnderAt = Long.MIN_VALUE;
            } else {
                Entity hint = combo.queuedEnderHintId > 0 ? player.m_284548_().m_6815_(combo.queuedEnderHintId) : null;
                if (tryEnder(player, state, combo, hint, now, combo.queuedEnderForward)) {
                    combo.queuedEnderAt = Long.MIN_VALUE;
                }
                return;
            }
        }
        if (combo.queuedAt == Long.MIN_VALUE) {
            return;
        }
        if (now - combo.queuedAt > INPUT_BUFFER_TICKS || !combos(player) || KanekiAutonomy.active(player)) {
            combo.queuedAt = Long.MIN_VALUE;
            return;
        }
        Entity hint = combo.queuedHintId > 0 ? player.m_284548_().m_6815_(combo.queuedHintId) : null;
        if (tryBegin(player, state, combo, hint, now, false, null, -1)) {
            combo.queuedAt = Long.MIN_VALUE;
        }
    }

    /**
     * Starts the next stage of the chain, or with {@code forced} the stage {@code forcedStage} of that style (an ender),
     * which ends the chain: the next M1 starts a new one.
     */
    private static boolean tryBegin(ServerPlayer player, KaguneRuntime.State state, Combo combo, Entity hint, long now, boolean involuntary,
                                    ComboStyle forced, int forcedStage) {
        if (!KaguneProfile.isActive(player) || !player.m_6084_() || state.hasManualHeldEntities()) {
            return false;
        }
        boolean chaining = combo.owns(state) && combo.impactDone && now >= combo.chainOpenAt;
        if ((!chaining && KaguneServerController.committed(state.mode)) || now < state.inputCooldownUntil) {
            return false;
        }
        boolean ayato = AyatoServer.isAyato(player);
        ComboStyle style = forced != null ? forced : ayato ? AyatoServer.comboStyle(player, now) : ComboStyle.forForm(KaguneProfile.form(player));
        float control = ControlServer.control(player);
        int next = 0;
        if (forced != null) {
            next = Math.max(0, Math.min(style.stageCount() - 1, forcedStage));
        } else if (combo.stage >= 0 && combo.style == style && (chaining || now - combo.lastEndAt <= Math.max(ControlMath.comboWindowTicks(control),
            style.ayatoShots() ? AyatoRules.ENDER_WINDOW_TICKS : 0))) {
            next = (combo.stage + 1) % style.stageCount();
        }
        ComboStage stage = style.stage(next);
        double range = range(player, style, stage);
        LivingEntity target = KaguneServerController.validHintedLiving(player, hint, range, false);
        if (target == null && !involuntary) {
            target = KaguneServerController.traceLiving(player, range, false);
        }
        if (target == null && involuntary) {
            return false;
        }
        Vec3 eye = player.m_146892_();
        Vec3 targetPosition;
        int targetId = 0;
        if (target != null) {
            targetPosition = KaguneServerController.center(target);
            targetId = target.m_19879_();
        } else {
            BlockHitResult block = KaguneServerController.traceBlock(player, range);
            targetPosition = block.m_6662_() == HitResult.Type.BLOCK ? block.m_82450_() : eye.m_82549_(player.m_20154_().m_82490_(range));
        }
        int duration = scaledDuration(player, stage, control);
        // Ayato's low RC supply (canon) makes every strike dearer until :re mastery.
        float cost = 0.85F * stage.energyMul() * (ayato ? AyatoRules.rcCostMultiplier(KaguneProfile.mastery(player)) : 1.0F);
        if (!KaguneServerController.useEnergy(player, cost, !involuntary)) {
            return false;
        }

        int mask;
        if (ayato) {
            // Wings (0 right, 1 left) and legs (8, 9) are always there: no tail cycling, no Centipede legs to share.
            mask = ComboStyle.limbMask(stage, 31, 0);
        } else {
            int manifested = Math.max(1, KaguneProfile.manifestedLimbs(player));
            mask = ComboStyle.limbMask(stage, manifested, -1);
            if (mask == 0) {
                // Stage limbs are cycled away (or the stage rotates limbs): use the original alternating limb choice.
                mask = 1 << Math.max(0, Math.min(30, KaguneServerController.takeNextAttackLimb(player, state)));
            }
            KaguneForm form = KaguneProfile.form(player);
            boolean segmented = false;
            for (int limb = 0; limb < 31; limb++) {
                if ((mask & 1 << limb) != 0) {
                    KaguneServerController.interruptCentipedeIfUsingLimb(state, limb, now);
                    segmented |= form.isCentipedeLimb(limb);
                }
            }
            if (segmented) {
                // Keep the autonomous Centipede legs from hijacking a leg that is mid-strike.
                state.nextCentipedeAttackAt = Math.max(state.nextCentipedeAttackAt, now + duration);
            }
        }

        float maxDeviation = ControlMath.strikeDeviationDegrees(control, involuntary);
        float deviation = maxDeviation <= 0.01F ? 0.0F : (player.m_217043_().m_188501_() * 2.0F - 1.0F) * maxDeviation;
        Vec3 aim = target != null ? KaguneServerController.center(target).m_82546_(eye) : player.m_20154_();
        if (aim.m_82556_() < 1.0E-6) {
            aim = player.m_20154_();
        }
        if (chaining) {
            combo.lastEndAt = now;
        }

        // The original reads this as a tendril index: an Ayato strike reports its wing (kicks report the right wing).
        state.actionLimbIndex = ayato ? ComboStyle.firstLimb(mask & 3) : ComboStyle.firstLimb(mask);
        state.begin(TendrilMode.SWIPE, now, duration, targetId, targetPosition);
        int impact = stage.impactTick(duration);
        ComboStage following = style.stage(next + 1);
        int followingImpact = following.impactTick(scaledDuration(player, following, control));
        // Never let chained impacts land inside the target's 10-tick hurt immunity: no wasted (or exploitable) hits.
        // Shots are bolts and spikes, not blows: their own fast cadence (each ignores hurt immunity); an ender plays out first.
        combo.chainOpenAt = now + (stage.pattern().ender() ? Math.max(impact + 1, duration - 2)
            : stage.pattern().flame() ? Math.max(impact + 1, AyatoRules.shotCadence(stage.pattern(), KaguneProfile.mastery(player)))
            : ComboStyle.chainOpenOffset(impact, followingImpact));
        state.inputCooldownUntil = combo.chainOpenAt;
        state.guarding = false;

        combo.style = style;
        combo.stage = next;
        combo.startedAt = now;
        combo.duration = duration;
        combo.impactTick = impact;
        combo.limbMask = mask;
        combo.deviation = deviation;
        combo.involuntary = involuntary;
        combo.impactDone = false;
        combo.queuedAt = Long.MIN_VALUE;
        combo.queuedEnderAt = Long.MIN_VALUE;
        combo.direction = rotateYaw(aim.m_82541_(), deviation);
        combo.range = range;
        if (!(ayato && (stage.pattern().flame() || stage.pattern().crystal() || stage.pattern().finalKakuja()))) {
            // 1.0.40: a blow (not a shot) on its way: another's M1 landing on him before it lands meets it (KaguneClash), and both stop.
            long began = now;
            KaguneClash.swing(player, now, impact, range, () -> combo.startedAt == began && !combo.impactDone && combo.owns(state),
                () -> clashed(player, state, combo));
        }

        if (ayato) {
            AyatoCombat.swing(player, stage);
        } else {
            float pitch = KaguneServerController.formPitch(player) * (1.12F - 0.3F * stage.weight());
            KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_WHIP, 0.78F + 0.3F * stage.weight(),
                KaguneServerController.vary(player, pitch, 0.05F));
        }
        ControlServer.broadcastNow(player);
        KaguneNetwork.broadcast(player);
        return true;
    }

    /** 1.0.40: the blow met another's coming back (KaguneClash): cut off and drawn back, a moment before the next. */
    private static void clashed(ServerPlayer player, KaguneRuntime.State state, Combo combo) {
        long now = player.m_9236_().m_46467_();
        combo.impactDone = true;
        combo.queuedAt = Long.MIN_VALUE;
        combo.queuedEnderAt = Long.MIN_VALUE;
        combo.lastEndAt = now;
        state.damageApplied = true;
        state.inputCooldownUntil = now + ClashRules.STAGGER;
        if (combo.owns(state)) {
            KaguneServerController.beginRetract(player, state, now);
        }
    }

    /** Reach of one stage: the tendril attack range, or Ayato's leg and wing reach. */
    private static double range(ServerPlayer player, ComboStyle style, ComboStage stage) {
        if (style.ayato()) {
            // 5.0.18: the Steel Blade reaches as far as it is long.
            return AyatoServer.blade(player) ? com.hex.kagune.control.ayato.SteelBlade.REACH * stage.rangeMul()
                : AyatoRules.strikeRange(KaguneProfile.mastery(player), AyatoServer.form(player), stage);
        }
        return Math.min(15.0, KaguneProfile.attackRange(player) * stage.rangeMul());
    }

    private static int scaledDuration(ServerPlayer player, ComboStage stage, float control) {
        int base = Math.round(stage.baseDuration() * ControlMath.strikeDurationScale(control));
        return Math.max(8, KaguneProfile.cooldown(player, base));
    }

    /** Replaces the SWIPE case of {@code KaguneServerController.tick} (bytecode redirect). */
    public static void tickSwipe(ServerPlayer player, KaguneRuntime.State state, long now, long elapsed) {
        Combo combo = COMBOS.get(player.m_20148_());
        if (combo == null || !combo.owns(state)) {
            // Swipes started by other systems (Threat Auto Attack) keep their original behaviour.
            KaguneServerController.tickSwipe(player, state, now, elapsed);
            return;
        }
        if (!combo.impactDone && elapsed >= combo.impactTick) {
            combo.impactDone = true;
            state.damageApplied = true;
            resolveStrike(player, state, combo);
            if (!combo.owns(state)) {
                return; // 1.0.40: it met another's (KaguneClash): already drawn back
            }
        }
        if (elapsed >= state.modeDuration) {
            combo.lastEndAt = now;
            KaguneServerController.beginRetract(player, state, now);
        }
    }

    private static void resolveStrike(ServerPlayer player, KaguneRuntime.State state, Combo combo) {
        ComboStage stage = combo.style.stage(combo.stage);
        boolean ayato = combo.style.ayato();
        if (ayato && stage.pattern().finalKakuja()) {
            AyatoPyre.resolve(player, state, stage, combo.limbMask, combo.direction, combo.startedAt, combo.duration);
            return;
        }
        if (ayato && stage.pattern().crystal()) {
            AyatoCrystal.resolve(player, state, stage, combo.limbMask, combo.direction, combo.startedAt, combo.duration);
            return;
        }
        if (ayato && stage.pattern().flame()) {
            AyatoFire.resolve(player, state, stage, combo.limbMask, combo.direction, combo.startedAt, combo.duration);
            return;
        }
        double range = combo.range;
        Vec3 eye = player.m_146892_();
        LivingEntity primary = null;
        if (player.m_284548_().m_6815_(state.actionTargetId) instanceof LivingEntity living
            && KaguneServerController.validSwipeTarget(player, living)
            && living.m_20280_(player) <= (range + 1.0) * (range + 1.0)
            && KaguneServerController.clearLineTo(player, living)) {
            primary = living;
        }
        Vec3 impactPoint = primary != null ? KaguneServerController.center(primary) : impactPointWithoutTarget(player, combo, range);

        List<LivingEntity> hits = new ArrayList<>();
        if (primary != null) {
            hits.add(primary);
        }
        if (stage.shape() != HitShape.SINGLE && hits.size() < stage.maxTargets()) {
            LivingEntity exclude = primary;
            AABB area = player.m_20191_().m_82400_(range + 2.0);
            List<LivingEntity> candidates = player.m_284548_().m_6443_(LivingEntity.class, area,
                entity -> entity != player && entity != exclude && KaguneServerController.validSwipeTarget(player, entity));
            candidates.sort(Comparator.comparingDouble(entity -> entity.m_20280_(player)));
            for (LivingEntity candidate : candidates) {
                if (hits.size() >= stage.maxTargets()) {
                    break;
                }
                if (inShape(stage, combo.direction, eye, impactPoint, range, candidate) && KaguneServerController.clearLineTo(player, candidate)) {
                    hits.add(candidate);
                }
            }
        }

        // 1.0.40: a player whose own M1 is on its way back at him: the two meet, and neither lands.
        long now = player.m_9236_().m_46467_();
        for (LivingEntity target : hits) {
            if (KaguneClash.meets(player, target, now)) {
                return;
            }
        }
        KaguneClash.landed(player);

        KaguneForm form = KaguneProfile.form(player);
        boolean bleeds = false;
        for (int limb = 0; limb < 31 && !ayato; limb++) {
            if ((combo.limbMask & 1 << limb) != 0 && form.isCentipedeLimb(limb)) {
                bleeds = true;
            }
        }
        // Ayato's damage follows his stages, not Kaneki's forms and mutations.
        float baseDamage = (ayato ? AyatoRules.meleeDamage(KaguneProfile.mastery(player)) : KaguneProfile.swipeDamage(player)) * stage.damageMul();
        float knockback = (!ayato && KaguneProfile.hasMutation(player, MutationNode.MASS) ? 0.82F : 0.58F) * stage.knockbackMul();
        int landed = 0;
        LivingEntity firstLanded = null;
        float firstDamage = 0.0F;
        for (int index = 0; index < hits.size(); index++) {
            LivingEntity target = hits.get(index);
            float damage = baseDamage * (index == 0 ? 1.0F : 0.7F);
            if (!ayato && KaguneProfile.hasMutation(player, MutationNode.ARM_WRAP) && target.m_20280_(player) <= 16.0) {
                damage *= 1.28F;
            }
            if (ayato) {
                Vec3 centre = KaguneServerController.center(target);
                Vec3 toward = centre.m_82546_(player.m_146892_());
                Vec3 into = toward.m_82556_() < 1.0E-6 ? player.m_20154_() : toward.m_82541_();
                WoundHits.expect(target, stage.pattern() == com.hex.kagune.control.StrikePattern.KICK ? com.hex.kagune.control.wound.WoundBook.Blow.BLUNT
                    : com.hex.kagune.control.wound.WoundBook.Blow.SLASH, centre.m_82546_(into.m_82490_(0.3)), into,
                    (player.m_217043_().m_188500_() - 0.5) * 1.4);
            }
            if (!target.m_6469_(player.m_269291_().m_269075_(player), damage)) {
                continue;
            }
            landed++;
            if (ayato && com.hex.kagune.control.ayato.SteelSwing.swings(stage.pattern())) {
                BladeBlood.cut(player, target, stage.pattern(), combo.direction, damage);
            }
            if (firstLanded == null) {
                firstLanded = target;
                firstDamage = damage;
            }
            if (bleeds) {
                KaguneServerController.applyCentipedeBleed(player, target);
            }
            if (landed <= 3) {
                KaguneServerController.playAt(player, target, HexKaguneSounds.FLESH_IMPACT, 0.9F,
                    KaguneServerController.vary(player, bleeds ? 0.9F : 1.0F, 0.06F));
            }
            Vec3 away = target.m_20182_().m_82546_(player.m_20182_());
            if (away.m_165925_() > 1.0E-4) {
                target.m_147240_(knockback, -away.f_82479_, -away.f_82481_);
            }
        }

        if (stage.shape() == HitShape.SLAM) {
            player.m_284548_().m_8767_(ParticleTypes.f_123797_, impactPoint.f_82479_, impactPoint.f_82480_, impactPoint.f_82481_,
                14, 0.45, 0.15, 0.45, 0.1);
            KaguneServerController.playAt(player, impactPoint, HexKaguneSounds.KAGUNE_SLAM, 0.75F,
                KaguneServerController.vary(player, KaguneServerController.formPitch(player), 0.05F));
        }
        if (ayato) {
            AyatoCombat.impact(player, stage, combo.direction, impactPoint, range);
        }
        if (landed > 0) {
            KaguneProfile.addMastery(player, "Swipe", 1);
            if (!ayato) {
                KaguneServerController.multiTendrilFollowUp(player, firstLanded, firstDamage);
            }
            ControlServer.onKaguneHit(player, landed);
            KaguneNetwork.broadcast(player);
        }
    }

    private static Vec3 impactPointWithoutTarget(ServerPlayer player, Combo combo, double range) {
        Vec3 eye = player.m_146892_();
        BlockHitResult block = KaguneServerController.traceBlock(player, range);
        double reach = range * 0.6;
        if (block.m_6662_() == HitResult.Type.BLOCK) {
            reach = Math.min(reach, Math.sqrt(eye.m_82557_(block.m_82450_())));
        }
        return eye.m_82549_(combo.direction.m_82490_(reach));
    }

    /** True if {@code entity} lies inside the stage's hit volume (pure geometry in {@link HitGeometry}, on server positions). */
    static boolean inShape(ComboStage stage, Vec3 direction, Vec3 eye, Vec3 impactPoint, double range, LivingEntity entity) {
        Vec3 offset = KaguneServerController.center(entity).m_82546_(eye);
        Vec3 slam = impactPoint.m_82546_(eye);
        return HitGeometry.inside(stage.shape(), stage.shapeParam(), offset.f_82479_, offset.f_82480_, offset.f_82481_,
            direction.f_82479_, direction.f_82480_, direction.f_82481_, range, entity.m_20205_() * 0.5, entity.m_20206_() * 0.5,
            slam.f_82479_, slam.f_82480_, slam.f_82481_);
    }

    static Vec3 rotateYaw(Vec3 vector, float degrees) {
        if (Math.abs(degrees) < 1.0E-4F) {
            return vector;
        }
        double[] rotated = ControlMath.rotateYaw(vector.f_82479_, vector.f_82481_, degrees);
        return new Vec3(rotated[0], vector.f_82480_, rotated[1]);
    }
}
