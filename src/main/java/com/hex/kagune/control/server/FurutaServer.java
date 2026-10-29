package com.hex.kagune.control.server;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.furuta.FurutaMove;
import com.hex.kagune.control.furuta.FurutaMutation;
import com.hex.kagune.control.furuta.FurutaRules;
import com.hex.kagune.control.furuta.FurutaStage;
import com.hex.kagune.control.network.C2SFurutaAction;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CFurutaState;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.network.KaguneNetwork;
import com.hex.kagune.sound.HexKaguneSounds;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Server owner of Furuta DNA (7.0.0): the base stage his kagune is in (the highest his mastery has grown, or a lower one he picked),
 * the kakuja, the move his limbs are making ({@link FurutaMove}, one at a time, its beats run by {@link FurutaCombat}), his
 * cooldowns, and what every client tracking him needs to draw it ({@link S2CFurutaState}).
 *
 * <p>His kagune is the original's manifested kagune (Alt + R brings it out, R puts it away, the RC drains as always), drawn as his own
 * limbs ({@code FurutaRenderer}) instead of the original tendrils, which never come out for him. His body keeps itself alive: the
 * kakuhou feeds RC while the kagune is out, his wounds close far faster than any ghoul's (paid in RC, {@code GhoulRegen}), a heavy blow
 * is half healed again at once, and once in a long while a killing blow leaves him standing. In the kakuja he takes far less from
 * every blow.
 *
 * <p>Runs after the original controller each tick (LOW priority), like the other paths.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class FurutaServer {
    /** Profile key: the base stage he picked (its id); none: the highest unlocked. */
    static final String STAGE_KEY = "FurutaStage";
    private static final Map<UUID, FurutaState> STATES = new HashMap<>();
    private static final long NOTICE_TICKS = 40L;
    private static long nextErrorLogAt;

    /** Transient per-player state on the logical server. */
    static final class FurutaState {
        boolean kakuja;
        /** When the kakuja last went on or off (long ago to start with, never MIN_VALUE: the time since would overflow). */
        long kakujaAt = -1_000_000L;
        long kakujaReadyAt = Long.MIN_VALUE;
        FurutaMove move = FurutaMove.NONE;
        long startedAt = Long.MIN_VALUE;
        float yaw;
        Vec3 point = Vec3.f_82478_;
        final int[] targets = new int[S2CFurutaState.TARGETS];
        int heldId;
        /** Each beat's bodies already struck (each at most once a beat; key -1: shared by every beat of the move). */
        final Map<Integer, Set<Integer>> hits = new HashMap<>();
        final EnumMap<FurutaMove, Long> readyAt = new EnumMap<>(FurutaMove.class);
        /** The M1 string: the last strike, when it ends, a click waiting for the current move. */
        FurutaMove lastStrike;
        long strikeEndsAt = Long.MIN_VALUE;
        long queuedAt = Long.MIN_VALUE;
        int queuedHint;
        FurutaMove.Slot queuedSlot;
        long queuedSlotAt = Long.MIN_VALUE;
        /** 7.0.2: the job running alongside the move ({@link FurutaMove#job}), when it began, the body it holds. */
        FurutaMove job = FurutaMove.NONE;
        /** 7.0.2: the King's Game: his sword's blow lands on its prey at this tick (after his rush in), or never. */
        long kingStrikeAt = Long.MIN_VALUE;
        long jobAt = Long.MIN_VALUE;
        int jobHeld;
        /** 7.0.2: Scorpion Vault: the dive can follow until this tick (while he is in the air). */
        long vaultUntil = Long.MIN_VALUE;
        /** 7.0.2: Owl's Execution's circle: where round its prey it began (radians) and which way round it goes. */
        double circleFrom;
        int circleWay = 1;
        /** A dash or a leap: no fall hurts him until he is down. */
        boolean fallSafe;
        /** The kakuhou's refusal to die, and a heavy blow's healing still owed (health) and over how many ticks. */
        long surviveReadyAt = Long.MIN_VALUE;
        float recoverLeft;
        int recoverTicks;
        long nextNoticeAt = Long.MIN_VALUE;
        FurutaStage sentStage;
        boolean dirty = true;

        void calm() {
            this.move = FurutaMove.NONE;
            this.startedAt = Long.MIN_VALUE;
            this.heldId = 0;
            java.util.Arrays.fill(this.targets, 0);
            this.hits.clear();
            this.lastStrike = null;
            this.queuedAt = Long.MIN_VALUE;
            this.queuedSlot = null;
            this.recoverLeft = 0.0F;
            this.recoverTicks = 0;
            this.vaultUntil = Long.MIN_VALUE;
            this.job = FurutaMove.NONE;
            this.jobAt = Long.MIN_VALUE;
            this.jobHeld = 0;
            this.kingStrikeAt = Long.MIN_VALUE;
            this.dirty = true;
        }
    }

    private FurutaServer() {
    }

    public static boolean isFuruta(ServerPlayer player) {
        return KaguneProfile.isUnlocked(player) && PathServer.path(player) == KagunePath.FURUTA;
    }

    // ---------------------------------------------------------------- mutations

    /** Profile key: his unlocked mutations (a bitmask of FurutaMutation). */
    static final String MUTATIONS_KEY = "FurutaMutations";

    /** His unlocked mutations (a bitmask of {@link FurutaMutation}). */
    public static int mutations(ServerPlayer player) {
        return KaguneProfile.root(player).m_128451_(MUTATIONS_KEY) & FurutaMutation.all();
    }

    /** True if he is on Furuta DNA and has unlocked {@code mutation}. */
    public static boolean hasMutation(ServerPlayer player, FurutaMutation mutation) {
        return isFuruta(player) && mutation.in(mutations(player));
    }

    /** His menu asks to unlock a mutation: not had yet, and a milestone point left to spend. */
    static void unlock(ServerPlayer player, int ordinal) {
        FurutaMutation mutation = FurutaMutation.byOrdinal(ordinal);
        int mask = mutations(player);
        // 7.0.1: quests pay mutation points too; some mutations also need their quest.
        if (mutation == null || !FurutaMutation.canUnlock(KaguneProfile.mastery(player), mask, mutation, QuestServer.points(player))
            || !QuestServer.pass(player, com.hex.kagune.control.quest.Gates.furuta(mutation))) {
            return;
        }
        KaguneProfile.root(player).m_128405_(MUTATIONS_KEY, mask | mutation.bit());
        player.m_5661_(Component.m_237110_("message.hexkagune.eto.mutation_unlocked", mutation.displayName()), true);
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_RESHAPE, 0.8F, KaguneServerController.vary(player, 1.1F, 0.05F));
        broadcast(player, state(player));
    }

    /** His body blown apart (You Thought You'd Get Rid of Me That Easily?): whatever the kagune was doing stops, the kakuja falls. */
    public static void apart(ServerPlayer player) {
        FurutaState state = peek(player);
        if (state == null) {
            return;
        }
        FurutaCombat.release(player, state, false);
        state.calm();
        if (state.kakuja) {
            state.kakuja = false;
            state.kakujaAt = player.m_9236_().m_46467_();
        }
        broadcast(player, state);
    }

    /** True while his kagune is out and he is alive: the only time his moves are heard. */
    static boolean ready(ServerPlayer player) {
        return isFuruta(player) && KaguneProfile.isActive(player) && player.m_6084_();
    }

    static FurutaState peek(ServerPlayer player) {
        return STATES.get(player.m_20148_());
    }

    static FurutaState state(ServerPlayer player) {
        return STATES.computeIfAbsent(player.m_20148_(), ignored -> new FurutaState());
    }

    /** True while he is in the kakuja (growing into it, or grown). */
    public static boolean kakuja(ServerPlayer player) {
        FurutaState state = STATES.get(player.m_20148_());
        return state != null && state.kakuja;
    }

    /** The base stage he picked, held to what his mastery allows (7.0.1: and the quests he has done). */
    public static FurutaStage chosen(ServerPlayer player) {
        int mastery = KaguneProfile.mastery(player);
        String id = KaguneProfile.root(player).m_128461_(STAGE_KEY);
        FurutaStage usable = FurutaStage.usable(FurutaStage.fromId(id), mastery);
        return com.hex.kagune.control.quest.Gates.furutaUsable(usable, mastery, gate -> QuestServer.open(player, gate));
    }

    /** The stage his moves come from now: the kakuja while he is in it, his base stage otherwise. */
    public static FurutaStage stage(ServerPlayer player) {
        return kakuja(player) ? FurutaStage.KAKUJA : chosen(player);
    }

    /** "{@code name} unlocks at mastery {@code mastery}" (true, said on the action bar) or false if it is unlocked. */
    static boolean locked(ServerPlayer player, FurutaState state, String name, int mastery) {
        if (KaguneProfile.mastery(player) >= mastery) {
            return false;
        }
        notice(player, state, Component.m_237110_("message.hexkagune.furuta.locked", name, mastery));
        return true;
    }

    static void notice(ServerPlayer player, FurutaState state, Component message) {
        long now = player.m_9236_().m_46467_();
        if (now >= state.nextNoticeAt || now < state.nextNoticeAt - NOTICE_TICKS) {
            state.nextNoticeAt = now + NOTICE_TICKS;
            player.m_5661_(message, true);
        }
    }

    static S2CFurutaState snapshot(ServerPlayer player, FurutaState state) {
        return new S2CFurutaState(player.m_20148_(), chosen(player).ordinal(), state.kakuja, state.kakujaAt, state.move.ordinal(), state.startedAt,
            state.yaw, state.point.f_82479_, state.point.f_82480_, state.point.f_82481_, state.targets.clone(), state.heldId, mutations(player),
            state.job.ordinal(), state.jobAt, state.jobHeld);
    }

    static void broadcast(ServerPlayer player, FurutaState state) {
        state.dirty = false;
        state.sentStage = chosen(player);
        ControlNetwork.broadcast(player, snapshot(player, state));
    }

    /** Every running cooldown of his, over now ({@link Cooldowns}). */
    static void clearCooldowns(ServerPlayer player) {
        FurutaState state = STATES.get(player.m_20148_());
        if (state != null) {
            state.readyAt.clear();
            state.kakujaReadyAt = Long.MIN_VALUE;
            state.surviveReadyAt = Long.MIN_VALUE;
        }
    }

    /** Called through {@code AyatoServer.onPathChanged}: whatever was in flight stops and every client learns his kagune anew. */
    static void onPathChanged(ServerPlayer player) {
        FurutaState state = STATES.remove(player.m_20148_());
        if (state != null) {
            FurutaCombat.release(player, state, false);
            state.calm();
            state.kakuja = false;
            ControlNetwork.broadcast(player, snapshot(player, state));
        }
        if (isFuruta(player)) {
            broadcast(player, state(player));
        }
    }

    // ---------------------------------------------------------------- input

    /** A Furuta input from {@code C2SFurutaAction}. */
    public static void onAction(ServerPlayer player, int action, int arg, int targetId) {
        if (!isFuruta(player) || EtoWitness.frozen(player)) {
            return;
        }
        FurutaState state = state(player);
        long now = player.m_9236_().m_46467_();
        if (action == C2SFurutaAction.MUTATION) {
            unlock(player, arg); // from his menu, whatever he is doing
            return;
        }
        if (EtoRevive.down(player)) {
            return; // in pieces: nothing he does counts
        }
        if (action == C2SFurutaAction.STAGE) {
            pickStage(player, state, arg);
            return;
        }
        if (EtoStun.stunned(player)) {
            return; // 7.0.2: stunned, he can't move or strike, nor reach for his kagune's moves
        }
        if (!ready(player)) {
            return;
        }
        Entity hint = targetId > 0 ? player.m_284548_().m_6815_(targetId) : null;
        switch (action) {
            case C2SFurutaAction.KAKUJA -> kakujaKey(player, false);
            case C2SFurutaAction.STEP -> FurutaCombat.step(player, state, arg, now);
            case C2SFurutaAction.KEY -> {
                FurutaMove.Slot[] slots = FurutaMove.Slot.values();
                if (arg > 0 && arg < slots.length) {
                    key(player, state, slots[arg], hint, now);
                }
            }
            default -> {
            }
        }
    }

    /** A move key: the stage's move on it, now if the limbs are free, or as soon as they are (a few ticks' grace). */
    static void key(ServerPlayer player, FurutaState state, FurutaMove.Slot slot, Entity hint, long now) {
        KakujaPassive.attacked(player);
        if (FurutaFinal.key(player, state, slot, now) || FurutaTails.key(player, state, slot, now) || FurutaCentipedes.key(player, state, slot, now)) {
            return; // 7.0.2: the vault's dive, the puppet's throw, the drag flung, the crawl's bite, the carapace's bite
        }
        if (FurutaCombat.committed(state, now)) {
            state.queuedSlot = slot;
            state.queuedSlotAt = now;
            state.queuedHint = hint == null ? 0 : hint.m_19879_();
            return;
        }
        FurutaMove move = FurutaMove.of(slot, stage(player));
        if (move == FurutaMove.NONE) {
            notice(player, state, Component.m_237115_("message.hexkagune.furuta.no_move"));
            return;
        }
        FurutaCombat.start(player, state, move, hint, now);
    }

    /** Trampoline target of the original M1 (through {@code KaguneCombat.startSwipe}): a strike of the string, or his blade and the kagune. */
    public static void strike(ServerPlayer player, Entity hint) {
        if (!ready(player) || EtoWitness.frozen(player) || EtoRevive.down(player) || EtoStun.stunned(player)) {
            return;
        }
        FurutaState state = state(player);
        long now = player.m_9236_().m_46467_();
        if (FurutaFinal.strike(player, state, now) || FurutaTails.strike(player, state, now) || FurutaCentipedes.strike(player, state, now)) {
            return; // 7.0.2: Human Puppet's strike, the carapace's bite
        }
        if (FurutaCombat.weaponStrike(player, state, hint, now)) {
            return;
        }
        if (FurutaCombat.committed(state, now)) {
            state.queuedAt = now;
            state.queuedHint = hint == null ? 0 : hint.m_19879_();
            return;
        }
        FurutaCombat.strike(player, state, hint, now);
    }

    /** The base stage from the menu or a held K ({@code arg} an ordinal; -1 the next unlocked one). */
    static void pickStage(ServerPlayer player, FurutaState state, int arg) {
        int mastery = KaguneProfile.mastery(player);
        FurutaStage current = chosen(player);
        FurutaStage wanted = arg < 0 ? com.hex.kagune.control.quest.Gates.furutaNext(current, mastery, gate -> QuestServer.open(player, gate))
            : FurutaStage.byOrdinal(arg);
        if (wanted.kakuja()) {
            return;
        }
        if (!wanted.unlocked(mastery)) {
            notice(player, state, Component.m_237110_("message.hexkagune.furuta.locked", wanted.displayName(), wanted.mastery()));
            return;
        }
        if (!QuestServer.pass(player, com.hex.kagune.control.quest.Gates.furuta(wanted))) {
            return; // 7.0.1: the stage also needs its quest
        }
        if (wanted == current) {
            return;
        }
        KaguneProfile.root(player).m_128359_(STAGE_KEY, wanted.id());
        if (!state.kakuja) {
            FurutaCentipedes.endJob(player, state, false, false);
            FurutaCombat.release(player, state, false);
            state.move = FurutaMove.NONE;
            state.startedAt = Long.MIN_VALUE;
        }
        player.m_5661_(Component.m_237110_("message.hexkagune.furuta.stage", wanted.displayName()), true);
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_RESHAPE, 0.9F, KaguneServerController.vary(player, 0.85F, 0.05F));
        broadcast(player, state);
    }

    /** K: into the kakuja (its mastery, its RC, its cooldown) or out of it; held, the next base stage. */
    public static void kakujaKey(ServerPlayer player, boolean hold) {
        if (!isFuruta(player)) {
            return;
        }
        FurutaState state = state(player);
        if (hold) {
            pickStage(player, state, -1);
            return;
        }
        if (!ready(player) || EtoStun.stunned(player)) {
            return;
        }
        long now = player.m_9236_().m_46467_();
        long since = now - state.kakujaAt;
        if (since < (state.kakuja ? FurutaRules.KAKUJA_IN : FurutaRules.KAKUJA_OUT) && since >= 0) {
            return; // mid-transformation
        }
        if (state.kakuja) {
            leaveKakuja(player, state, now);
            return;
        }
        if (locked(player, state, FurutaStage.KAKUJA.displayName(), FurutaRules.KAKUJA_MASTERY)
            || !QuestServer.pass(player, com.hex.kagune.control.quest.Gate.FURUTA_KAKUJA)) {
            return;
        }
        if (now < state.kakujaReadyAt) {
            notice(player, state, Component.m_237110_("message.hexkagune.furuta.cooldown", (state.kakujaReadyAt - now + 19) / 20));
            return;
        }
        if (!KaguneServerController.useEnergy(player, hasMutation(player, FurutaMutation.WASHUU_BLOOD) ? FurutaMutation.BLOOD_KAKUJA_COST
            : FurutaRules.KAKUJA_COST, true)) {
            return;
        }
        FurutaCentipedes.endJob(player, state, false, false);
        FurutaCombat.release(player, state, false);
        state.move = FurutaMove.NONE;
        state.startedAt = Long.MIN_VALUE;
        state.kakuja = true;
        state.kakujaAt = now;
        KaguneServerController.playSelf(player, HexKaguneSounds.MANIFEST_RIP, 1.0F, KaguneServerController.vary(player, 0.6F, 0.05F));
        AyatoFire.sound(player.m_284548_(), player.m_20182_(), "entity.warden.roar", 0.7F, KaguneServerController.vary(player, 1.3F, 0.05F));
        KaguneNetwork.sendTo(player, player);
        broadcast(player, state);
    }

    static void leaveKakuja(ServerPlayer player, FurutaState state, long now) {
        if (!state.kakuja) {
            return;
        }
        FurutaCombat.release(player, state, false);
        state.move = FurutaMove.NONE;
        state.startedAt = Long.MIN_VALUE;
        state.kakuja = false;
        state.kakujaAt = now;
        state.kakujaReadyAt = now + Cooldowns.of(player, (long)FurutaRules.KAKUJA_COOLDOWN);
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_DISMISS, 0.9F, KaguneServerController.vary(player, 0.7F, 0.05F));
        broadcast(player, state);
    }

    /** True once the kakuja has grown far enough for its moves. */
    static boolean kakujaReady(FurutaState state, long now) {
        return state.kakuja && now - state.kakujaAt >= FurutaRules.KAKUJA_READY;
    }

    // ---------------------------------------------------------------- the tick

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        try {
            tick(player);
        } catch (RuntimeException failure) {
            long now = System.currentTimeMillis();
            if (now >= nextErrorLogAt) {
                nextErrorLogAt = now + 30000L;
                KaguneMod.LOGGER.error("HexKagune Furuta DNA tick failed", failure);
            }
        }
    }

    private static void tick(ServerPlayer player) {
        if (!isFuruta(player)) {
            return;
        }
        FurutaState state = state(player);
        long now = player.m_9236_().m_46467_();
        if (now % 20L == 0L) {
            // His profile keeps the neutral seed: whatever Kaneki form or mutation found its way in is parked within a second.
            KanekiStash.stash(player);
        }
        if (!KaguneProfile.isActive(player) || !player.m_6084_()) {
            if (state.move != FurutaMove.NONE || state.heldId != 0 || state.kakuja) {
                FurutaCombat.release(player, state, false);
                state.calm();
                if (state.kakuja) {
                    state.kakuja = false;
                    state.kakujaAt = now;
                }
                broadcast(player, state);
            }
            return;
        }
        int mastery = KaguneProfile.mastery(player);
        if (state.kakuja) {
            float drain = FurutaRules.KAKUJA_DRAIN * (hasMutation(player, FurutaMutation.WASHUU_BLOOD) ? FurutaMutation.BLOOD_DRAIN : 1.0F);
            if (!KaguneProfile.consumeEnergy(player, drain) || mastery < FurutaRules.KAKUJA_MASTERY) {
                KaguneProfile.setEnergy(player, 0.0F);
                leaveKakuja(player, state, now);
                player.m_5661_(Component.m_237115_("message.hexkagune.furuta.kakuja_spent"), true);
            } else if (now % 10L == 0L) {
                KaguneServerController.refreshFormEffect(player, MobEffects.f_19600_, 1);
                KaguneServerController.refreshFormEffect(player, MobEffects.f_19596_, 0);
                KaguneServerController.refreshFormEffect(player, MobEffects.f_19603_, 0);
            }
            if (now % 10L == 0L) {
                KaguneNetwork.sendTo(player, player);
            }
        } else if (KaguneProfile.energy(player) < KaguneProfile.MAX_ENERGY) {
            KaguneProfile.restoreEnergy(player, FurutaRules.formRegen(mastery, false));
            if (now % 10L == 0L) {
                KaguneNetwork.sendTo(player, player);
            }
        }
        if (state.fallSafe) {
            if (player.m_20096_() || player.m_20069_()) {
                state.fallSafe = false;
            } else {
                player.f_19789_ = 0.0F;
            }
        }
        recover(player, state);
        FurutaCombat.tick(player, state, now);
        if (chosen(player) != state.sentStage) {
            state.dirty = true;
        }
        if (state.dirty) {
            broadcast(player, state);
        }
    }

    /** A heavy blow's healing, paid out over a short while (and in RC). */
    private static void recover(ServerPlayer player, FurutaState state) {
        if (state.recoverTicks <= 0 || state.recoverLeft <= 0.0F) {
            return;
        }
        float amount = state.recoverLeft / state.recoverTicks;
        state.recoverTicks--;
        state.recoverLeft -= amount;
        if (player.m_21223_() < player.m_21233_() && KaguneProfile.consumeEnergy(player, amount * FurutaRules.RECOVER_COST)) {
            player.m_5634_(amount);
        } else {
            state.recoverTicks = 0;
            state.recoverLeft = 0.0F;
        }
    }

    // ---------------------------------------------------------------- his body

    /** True while a guard of his catches what comes at him. */
    static boolean guarding(FurutaState state, long now) {
        if (state.move == null || !state.move.guards()) {
            return false;
        }
        long age = now - state.startedAt;
        for (FurutaMove.Beat beat : state.move.beats(4)) {
            if (beat.kind() == FurutaMove.Kind.CURL && age >= beat.start() && age <= beat.end()) {
                return true;
            }
        }
        return false;
    }

    /** His guards catch the blow; anything that would hurt him meanwhile misses. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void attacked(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || STATES.isEmpty()) {
            return;
        }
        FurutaState state = STATES.get(player.m_20148_());
        if (state == null || !ready(player)) {
            return;
        }
        long now = player.m_9236_().m_46467_();
        if (FurutaTails.attacked(player, state, event, now) || FurutaCentipedes.attacked(player, state, event, now)) {
            return; // 7.0.2: Mockingbird's counter, the carapace springing
        }
        // A guard catches blows: something struck him (the void, a fall, hunger, fire go through).
        if (!guarding(state, now) || event.getSource().m_7639_() == null && event.getSource().m_7640_() == null) {
            return;
        }
        if (!KaguneProfile.consumeEnergy(player, 0.6F)) {
            return; // run dry, the wall gives
        }
        event.setCanceled(true);
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_GUARD, 0.8F, KaguneServerController.vary(player, 1.0F, 0.1F));
    }

    /** In the kakuja he takes a fraction of every blow. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void hurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || STATES.isEmpty() || event.getAmount() <= 0.0F || event.getAmount() > 1000.0F) {
            return;
        }
        FurutaState state = STATES.get(player.m_20148_());
        if (state != null && state.kakuja && isFuruta(player)) {
            // 7.0.2: in Monstrous Recovery the coils take half of what is left.
            event.setAmount(event.getAmount() * FurutaRules.KAKUJA_TAKEN * FurutaFinal.taken(state));
        }
    }

    /**
     * After every reduction: a killing blow (once in a long while, for RC) leaves him at one health; a heavy one is healed back in
     * part over the next second and a half.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void hurtLast(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || STATES.isEmpty() || event.getAmount() <= 0.0F) {
            return;
        }
        FurutaState state = STATES.get(player.m_20148_());
        if (state == null || !ready(player)) {
            return;
        }
        long now = player.m_9236_().m_46467_();
        float health = player.m_21223_();
        float amount = event.getAmount();
        if (amount >= health && amount < 1000.0F && now >= state.surviveReadyAt && KaguneProfile.energy(player) >= FurutaRules.SURVIVE_COST) {
            KaguneProfile.setEnergy(player, KaguneProfile.energy(player) - FurutaRules.SURVIVE_COST);
            state.surviveReadyAt = now + Cooldowns.of(player, (long)FurutaRules.SURVIVE_COOLDOWN);
            event.setAmount(Math.max(0.0F, health - 1.0F));
            state.recoverLeft = Math.min(player.m_21233_() * 0.5F, 10.0F);
            state.recoverTicks = FurutaRules.RECOVER_TICKS;
            player.m_5661_(Component.m_237115_("message.hexkagune.furuta.survived"), true);
            KaguneServerController.playSelf(player, HexKaguneSounds.BONE_CRACK, 1.0F, 0.6F);
            KaguneNetwork.sendTo(player, player);
            return;
        }
        if (amount >= FurutaRules.HEAVY_BLOW) {
            float share = (state.kakuja ? FurutaRules.RECOVER_SHARE_KAKUJA : FurutaRules.RECOVER_SHARE)
                + (hasMutation(player, FurutaMutation.KAKUHOU_OVERDRIVE) ? FurutaMutation.OVERDRIVE_RECOVER : 0.0F);
            state.recoverLeft = Math.min(20.0F, state.recoverLeft + amount * share);
            state.recoverTicks = FurutaRules.RECOVER_TICKS;
        }
    }

    // ---------------------------------------------------------------- tracking

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof ServerPlayer subject && isFuruta(subject)) {
            ControlNetwork.sendTo(viewer, snapshot(subject, state(subject)));
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            STATES.remove(player.m_20148_());
            if (isFuruta(player)) {
                broadcast(player, state(player));
            }
        }
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FurutaState state = STATES.get(player.m_20148_());
            if (state != null) {
                FurutaCombat.release(player, state, false);
                state.calm();
                state.kakuja = false;
                if (isFuruta(player)) {
                    broadcast(player, state);
                }
            }
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FurutaState state = STATES.get(player.m_20148_());
            if (state != null) {
                FurutaCombat.release(player, state, false);
                state.calm();
                if (isFuruta(player)) {
                    broadcast(player, state);
                }
            }
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FurutaState state = STATES.remove(player.m_20148_());
            if (state != null) {
                FurutaCombat.release(player, state, false);
            }
        }
    }
}
