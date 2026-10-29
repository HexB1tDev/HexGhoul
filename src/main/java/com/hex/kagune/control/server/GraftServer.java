package com.hex.kagune.control.server;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.graft.GraftMove;
import com.hex.kagune.control.graft.GraftMutation;
import com.hex.kagune.control.graft.GraftRules;
import com.hex.kagune.control.graft.GraftStage;
import com.hex.kagune.control.network.C2SGraftAction;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CGraftState;
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
 * Server owner of Graft DNA (7.0.0): the base stage his kagune is in (the highest his mastery has grown, or a lower one he picked),
 * the kakuja, the move his limbs are making ({@link GraftMove}, one at a time, its beats run by {@link GraftCombat}), his
 * cooldowns, and what every client tracking him needs to draw it ({@link S2CGraftState}).
 *
 * <p>His kagune is the original's manifested kagune (Alt + R brings it out, R puts it away, the RC drains as always), drawn as his own
 * limbs ({@code GraftRenderer}) instead of the original tendrils, which never come out for him. His body keeps itself alive: the
 * kakuhou feeds RC while the kagune is out, his wounds close far faster than any ghoul's (paid in RC, {@code GhoulRegen}), a heavy blow
 * is half healed again at once, and once in a long while a killing blow leaves him standing. In the kakuja he takes far less from
 * every blow.
 *
 * <p>Runs after the original controller each tick (LOW priority), like the other paths.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class GraftServer {
    /** Profile key: the base stage he picked (its id); none: the highest unlocked. */
    static final String STAGE_KEY = "GraftStage";
    private static final Map<UUID, GraftState> STATES = new HashMap<>();
    private static final long NOTICE_TICKS = 40L;
    private static long nextErrorLogAt;

    /** Transient per-player state on the logical server. */
    static final class GraftState {
        boolean kakuja;
        /** When the kakuja last went on or off (long ago to start with, never MIN_VALUE: the time since would overflow). */
        long kakujaAt = -1_000_000L;
        long kakujaReadyAt = Long.MIN_VALUE;
        GraftMove move = GraftMove.NONE;
        long startedAt = Long.MIN_VALUE;
        float yaw;
        Vec3 point = Vec3.f_82478_;
        final int[] targets = new int[S2CGraftState.TARGETS];
        int heldId;
        /** Each beat's bodies already struck (each at most once a beat; key -1: shared by every beat of the move). */
        final Map<Integer, Set<Integer>> hits = new HashMap<>();
        final EnumMap<GraftMove, Long> readyAt = new EnumMap<>(GraftMove.class);
        /** The M1 string: the last strike, when it ends, a click waiting for the current move. */
        GraftMove lastStrike;
        long strikeEndsAt = Long.MIN_VALUE;
        long queuedAt = Long.MIN_VALUE;
        int queuedHint;
        GraftMove.Slot queuedSlot;
        long queuedSlotAt = Long.MIN_VALUE;
        /** 7.0.2: the job running alongside the move ({@link GraftMove#job}), when it began, the body it holds. */
        GraftMove job = GraftMove.NONE;
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
        GraftStage sentStage;
        boolean dirty = true;

        void calm() {
            this.move = GraftMove.NONE;
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
            this.job = GraftMove.NONE;
            this.jobAt = Long.MIN_VALUE;
            this.jobHeld = 0;
            this.kingStrikeAt = Long.MIN_VALUE;
            this.dirty = true;
        }
    }

    private GraftServer() {
    }

    public static boolean isGraft(ServerPlayer player) {
        return KaguneProfile.isUnlocked(player) && PathServer.path(player) == KagunePath.GRAFT;
    }

    // ---------------------------------------------------------------- mutations

    /** Profile key: his unlocked mutations (a bitmask of GraftMutation). */
    static final String MUTATIONS_KEY = "GraftMutations";

    /** His unlocked mutations (a bitmask of {@link GraftMutation}). */
    public static int mutations(ServerPlayer player) {
        return KaguneProfile.root(player).m_128451_(MUTATIONS_KEY) & GraftMutation.all();
    }

    /** True if he is on Graft DNA and has unlocked {@code mutation}. */
    public static boolean hasMutation(ServerPlayer player, GraftMutation mutation) {
        return isGraft(player) && mutation.in(mutations(player));
    }

    /** His menu asks to unlock a mutation: not had yet, and a milestone point left to spend. */
    static void unlock(ServerPlayer player, int ordinal) {
        GraftMutation mutation = GraftMutation.byOrdinal(ordinal);
        int mask = mutations(player);
        // 7.0.1: quests pay mutation points too; some mutations also need their quest.
        if (mutation == null || !GraftMutation.canUnlock(KaguneProfile.mastery(player), mask, mutation, QuestServer.points(player))
            || !QuestServer.pass(player, com.hex.kagune.control.quest.Gates.graft(mutation))) {
            return;
        }
        KaguneProfile.root(player).m_128405_(MUTATIONS_KEY, mask | mutation.bit());
        player.m_5661_(Component.m_237110_("message.hexkagune.pale.mutation_unlocked", mutation.displayName()), true);
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_RESHAPE, 0.8F, KaguneServerController.vary(player, 1.1F, 0.05F));
        broadcast(player, state(player));
    }

    /** His body blown apart (You Thought You'd Get Rid of Me That Easily?): whatever the kagune was doing stops, the kakuja falls. */
    public static void apart(ServerPlayer player) {
        GraftState state = peek(player);
        if (state == null) {
            return;
        }
        GraftCombat.release(player, state, false);
        state.calm();
        if (state.kakuja) {
            state.kakuja = false;
            state.kakujaAt = player.m_9236_().m_46467_();
        }
        broadcast(player, state);
    }

    /** True while his kagune is out and he is alive: the only time his moves are heard. */
    static boolean ready(ServerPlayer player) {
        return isGraft(player) && KaguneProfile.isActive(player) && player.m_6084_();
    }

    static GraftState peek(ServerPlayer player) {
        return STATES.get(player.m_20148_());
    }

    static GraftState state(ServerPlayer player) {
        return STATES.computeIfAbsent(player.m_20148_(), ignored -> new GraftState());
    }

    /** True while he is in the kakuja (growing into it, or grown). */
    public static boolean kakuja(ServerPlayer player) {
        GraftState state = STATES.get(player.m_20148_());
        return state != null && state.kakuja;
    }

    /** The base stage he picked, held to what his mastery allows (7.0.1: and the quests he has done). */
    public static GraftStage chosen(ServerPlayer player) {
        int mastery = KaguneProfile.mastery(player);
        String id = KaguneProfile.root(player).m_128461_(STAGE_KEY);
        GraftStage usable = GraftStage.usable(GraftStage.fromId(id), mastery);
        return com.hex.kagune.control.quest.Gates.graftUsable(usable, mastery, gate -> QuestServer.open(player, gate));
    }

    /** The stage his moves come from now: the kakuja while he is in it, his base stage otherwise. */
    public static GraftStage stage(ServerPlayer player) {
        return kakuja(player) ? GraftStage.KAKUJA : chosen(player);
    }

    /** "{@code name} unlocks at mastery {@code mastery}" (true, said on the action bar) or false if it is unlocked. */
    static boolean locked(ServerPlayer player, GraftState state, String name, int mastery) {
        if (KaguneProfile.mastery(player) >= mastery) {
            return false;
        }
        notice(player, state, Component.m_237110_("message.hexkagune.graft.locked", name, mastery));
        return true;
    }

    static void notice(ServerPlayer player, GraftState state, Component message) {
        long now = player.m_9236_().m_46467_();
        if (now >= state.nextNoticeAt || now < state.nextNoticeAt - NOTICE_TICKS) {
            state.nextNoticeAt = now + NOTICE_TICKS;
            player.m_5661_(message, true);
        }
    }

    static S2CGraftState snapshot(ServerPlayer player, GraftState state) {
        return new S2CGraftState(player.m_20148_(), chosen(player).ordinal(), state.kakuja, state.kakujaAt, state.move.ordinal(), state.startedAt,
            state.yaw, state.point.f_82479_, state.point.f_82480_, state.point.f_82481_, state.targets.clone(), state.heldId, mutations(player),
            state.job.ordinal(), state.jobAt, state.jobHeld);
    }

    static void broadcast(ServerPlayer player, GraftState state) {
        state.dirty = false;
        state.sentStage = chosen(player);
        ControlNetwork.broadcast(player, snapshot(player, state));
    }

    /** Every running cooldown of his, over now ({@link Cooldowns}). */
    static void clearCooldowns(ServerPlayer player) {
        GraftState state = STATES.get(player.m_20148_());
        if (state != null) {
            state.readyAt.clear();
            state.kakujaReadyAt = Long.MIN_VALUE;
            state.surviveReadyAt = Long.MIN_VALUE;
        }
    }

    /** Called through {@code UkakuServer.onPathChanged}: whatever was in flight stops and every client learns his kagune anew. */
    static void onPathChanged(ServerPlayer player) {
        GraftState state = STATES.remove(player.m_20148_());
        if (state != null) {
            GraftCombat.release(player, state, false);
            state.calm();
            state.kakuja = false;
            ControlNetwork.broadcast(player, snapshot(player, state));
        }
        if (isGraft(player)) {
            broadcast(player, state(player));
        }
    }

    // ---------------------------------------------------------------- input

    /** A Graft input from {@code C2SGraftAction}. */
    public static void onAction(ServerPlayer player, int action, int arg, int targetId) {
        if (!isGraft(player) || PaleWitness.frozen(player)) {
            return;
        }
        GraftState state = state(player);
        long now = player.m_9236_().m_46467_();
        if (action == C2SGraftAction.MUTATION) {
            unlock(player, arg); // from his menu, whatever he is doing
            return;
        }
        if (PaleRevive.down(player)) {
            return; // in pieces: nothing he does counts
        }
        if (action == C2SGraftAction.STAGE) {
            pickStage(player, state, arg);
            return;
        }
        if (PaleStun.stunned(player)) {
            return; // 7.0.2: stunned, he can't move or strike, nor reach for his kagune's moves
        }
        if (!ready(player)) {
            return;
        }
        Entity hint = targetId > 0 ? player.m_284548_().m_6815_(targetId) : null;
        switch (action) {
            case C2SGraftAction.KAKUJA -> kakujaKey(player, false);
            case C2SGraftAction.STEP -> GraftCombat.step(player, state, arg, now);
            case C2SGraftAction.KEY -> {
                GraftMove.Slot[] slots = GraftMove.Slot.values();
                if (arg > 0 && arg < slots.length) {
                    key(player, state, slots[arg], hint, now);
                }
            }
            default -> {
            }
        }
    }

    /** A move key: the stage's move on it, now if the limbs are free, or as soon as they are (a few ticks' grace). */
    static void key(ServerPlayer player, GraftState state, GraftMove.Slot slot, Entity hint, long now) {
        KakujaPassive.attacked(player);
        if (GraftFinal.key(player, state, slot, now) || GraftTails.key(player, state, slot, now) || GraftCentipedes.key(player, state, slot, now)) {
            return; // 7.0.2: the vault's dive, the puppet's throw, the drag flung, the crawl's bite, the carapace's bite
        }
        if (GraftCombat.committed(state, now)) {
            state.queuedSlot = slot;
            state.queuedSlotAt = now;
            state.queuedHint = hint == null ? 0 : hint.m_19879_();
            return;
        }
        GraftMove move = GraftMove.of(slot, stage(player));
        if (move == GraftMove.NONE) {
            notice(player, state, Component.m_237115_("message.hexkagune.graft.no_move"));
            return;
        }
        GraftCombat.start(player, state, move, hint, now);
    }

    /** Trampoline target of the original M1 (through {@code KaguneCombat.startSwipe}): a strike of the string, or his blade and the kagune. */
    public static void strike(ServerPlayer player, Entity hint) {
        if (!ready(player) || PaleWitness.frozen(player) || PaleRevive.down(player) || PaleStun.stunned(player)) {
            return;
        }
        GraftState state = state(player);
        long now = player.m_9236_().m_46467_();
        if (GraftFinal.strike(player, state, now) || GraftTails.strike(player, state, now) || GraftCentipedes.strike(player, state, now)) {
            return; // 7.0.2: Human Puppet's strike, the carapace's bite
        }
        if (GraftCombat.weaponStrike(player, state, hint, now)) {
            return;
        }
        if (GraftCombat.committed(state, now)) {
            state.queuedAt = now;
            state.queuedHint = hint == null ? 0 : hint.m_19879_();
            return;
        }
        GraftCombat.strike(player, state, hint, now);
    }

    /** The base stage from the menu or a held K ({@code arg} an ordinal; -1 the next unlocked one). */
    static void pickStage(ServerPlayer player, GraftState state, int arg) {
        int mastery = KaguneProfile.mastery(player);
        GraftStage current = chosen(player);
        GraftStage wanted = arg < 0 ? com.hex.kagune.control.quest.Gates.graftNext(current, mastery, gate -> QuestServer.open(player, gate))
            : GraftStage.byOrdinal(arg);
        if (wanted.kakuja()) {
            return;
        }
        if (!wanted.unlocked(mastery)) {
            notice(player, state, Component.m_237110_("message.hexkagune.graft.locked", wanted.displayName(), wanted.mastery()));
            return;
        }
        if (!QuestServer.pass(player, com.hex.kagune.control.quest.Gates.graft(wanted))) {
            return; // 7.0.1: the stage also needs its quest
        }
        if (wanted == current) {
            return;
        }
        KaguneProfile.root(player).m_128359_(STAGE_KEY, wanted.id());
        if (!state.kakuja) {
            GraftCentipedes.endJob(player, state, false, false);
            GraftCombat.release(player, state, false);
            state.move = GraftMove.NONE;
            state.startedAt = Long.MIN_VALUE;
        }
        player.m_5661_(Component.m_237110_("message.hexkagune.graft.stage", wanted.displayName()), true);
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_RESHAPE, 0.9F, KaguneServerController.vary(player, 0.85F, 0.05F));
        broadcast(player, state);
    }

    /** K: into the kakuja (its mastery, its RC, its cooldown) or out of it; held, the next base stage. */
    public static void kakujaKey(ServerPlayer player, boolean hold) {
        if (!isGraft(player)) {
            return;
        }
        GraftState state = state(player);
        if (hold) {
            pickStage(player, state, -1);
            return;
        }
        if (!ready(player) || PaleStun.stunned(player)) {
            return;
        }
        long now = player.m_9236_().m_46467_();
        long since = now - state.kakujaAt;
        if (since < (state.kakuja ? GraftRules.KAKUJA_IN : GraftRules.KAKUJA_OUT) && since >= 0) {
            return; // mid-transformation
        }
        if (state.kakuja) {
            leaveKakuja(player, state, now);
            return;
        }
        if (locked(player, state, GraftStage.KAKUJA.displayName(), GraftRules.KAKUJA_MASTERY)
            || !QuestServer.pass(player, com.hex.kagune.control.quest.Gate.GRAFT_KAKUJA)) {
            return;
        }
        if (now < state.kakujaReadyAt) {
            notice(player, state, Component.m_237110_("message.hexkagune.graft.cooldown", (state.kakujaReadyAt - now + 19) / 20));
            return;
        }
        if (!KaguneServerController.useEnergy(player, hasMutation(player, GraftMutation.WASHUU_BLOOD) ? GraftMutation.BLOOD_KAKUJA_COST
            : GraftRules.KAKUJA_COST, true)) {
            return;
        }
        GraftCentipedes.endJob(player, state, false, false);
        GraftCombat.release(player, state, false);
        state.move = GraftMove.NONE;
        state.startedAt = Long.MIN_VALUE;
        state.kakuja = true;
        state.kakujaAt = now;
        KaguneServerController.playSelf(player, HexKaguneSounds.MANIFEST_RIP, 1.0F, KaguneServerController.vary(player, 0.6F, 0.05F));
        UkakuFire.sound(player.m_284548_(), player.m_20182_(), "entity.warden.roar", 0.7F, KaguneServerController.vary(player, 1.3F, 0.05F));
        KaguneNetwork.sendTo(player, player);
        broadcast(player, state);
    }

    static void leaveKakuja(ServerPlayer player, GraftState state, long now) {
        if (!state.kakuja) {
            return;
        }
        GraftCombat.release(player, state, false);
        state.move = GraftMove.NONE;
        state.startedAt = Long.MIN_VALUE;
        state.kakuja = false;
        state.kakujaAt = now;
        state.kakujaReadyAt = now + Cooldowns.of(player, (long)GraftRules.KAKUJA_COOLDOWN);
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_DISMISS, 0.9F, KaguneServerController.vary(player, 0.7F, 0.05F));
        broadcast(player, state);
    }

    /** True once the kakuja has grown far enough for its moves. */
    static boolean kakujaReady(GraftState state, long now) {
        return state.kakuja && now - state.kakujaAt >= GraftRules.KAKUJA_READY;
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
                KaguneMod.LOGGER.error("HexKagune Graft DNA tick failed", failure);
            }
        }
    }

    private static void tick(ServerPlayer player) {
        if (!isGraft(player)) {
            return;
        }
        GraftState state = state(player);
        long now = player.m_9236_().m_46467_();
        if (now % 20L == 0L) {
            // His profile keeps the neutral seed: whatever Rinkaku form or mutation found its way in is parked within a second.
            RinkakuStash.stash(player);
        }
        if (!KaguneProfile.isActive(player) || !player.m_6084_()) {
            if (state.move != GraftMove.NONE || state.heldId != 0 || state.kakuja) {
                GraftCombat.release(player, state, false);
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
            float drain = GraftRules.KAKUJA_DRAIN * (hasMutation(player, GraftMutation.WASHUU_BLOOD) ? GraftMutation.BLOOD_DRAIN : 1.0F);
            if (!KaguneProfile.consumeEnergy(player, drain) || mastery < GraftRules.KAKUJA_MASTERY) {
                KaguneProfile.setEnergy(player, 0.0F);
                leaveKakuja(player, state, now);
                player.m_5661_(Component.m_237115_("message.hexkagune.graft.kakuja_spent"), true);
            } else if (now % 10L == 0L) {
                KaguneServerController.refreshFormEffect(player, MobEffects.f_19600_, 1);
                KaguneServerController.refreshFormEffect(player, MobEffects.f_19596_, 0);
                KaguneServerController.refreshFormEffect(player, MobEffects.f_19603_, 0);
            }
            if (now % 10L == 0L) {
                KaguneNetwork.sendTo(player, player);
            }
        } else if (KaguneProfile.energy(player) < KaguneProfile.MAX_ENERGY) {
            KaguneProfile.restoreEnergy(player, GraftRules.formRegen(mastery, false));
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
        GraftCombat.tick(player, state, now);
        if (chosen(player) != state.sentStage) {
            state.dirty = true;
        }
        if (state.dirty) {
            broadcast(player, state);
        }
    }

    /** A heavy blow's healing, paid out over a short while (and in RC). */
    private static void recover(ServerPlayer player, GraftState state) {
        if (state.recoverTicks <= 0 || state.recoverLeft <= 0.0F) {
            return;
        }
        float amount = state.recoverLeft / state.recoverTicks;
        state.recoverTicks--;
        state.recoverLeft -= amount;
        if (player.m_21223_() < player.m_21233_() && KaguneProfile.consumeEnergy(player, amount * GraftRules.RECOVER_COST)) {
            player.m_5634_(amount);
        } else {
            state.recoverTicks = 0;
            state.recoverLeft = 0.0F;
        }
    }

    // ---------------------------------------------------------------- his body

    /** True while a guard of his catches what comes at him. */
    static boolean guarding(GraftState state, long now) {
        if (state.move == null || !state.move.guards()) {
            return false;
        }
        long age = now - state.startedAt;
        for (GraftMove.Beat beat : state.move.beats(4)) {
            if (beat.kind() == GraftMove.Kind.CURL && age >= beat.start() && age <= beat.end()) {
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
        GraftState state = STATES.get(player.m_20148_());
        if (state == null || !ready(player)) {
            return;
        }
        long now = player.m_9236_().m_46467_();
        if (GraftTails.attacked(player, state, event, now) || GraftCentipedes.attacked(player, state, event, now)) {
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
        GraftState state = STATES.get(player.m_20148_());
        if (state != null && state.kakuja && isGraft(player)) {
            // 7.0.2: in Monstrous Recovery the coils take half of what is left.
            event.setAmount(event.getAmount() * GraftRules.KAKUJA_TAKEN * GraftFinal.taken(state));
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
        GraftState state = STATES.get(player.m_20148_());
        if (state == null || !ready(player)) {
            return;
        }
        long now = player.m_9236_().m_46467_();
        float health = player.m_21223_();
        float amount = event.getAmount();
        if (amount >= health && amount < 1000.0F && now >= state.surviveReadyAt && KaguneProfile.energy(player) >= GraftRules.SURVIVE_COST) {
            KaguneProfile.setEnergy(player, KaguneProfile.energy(player) - GraftRules.SURVIVE_COST);
            state.surviveReadyAt = now + Cooldowns.of(player, (long)GraftRules.SURVIVE_COOLDOWN);
            event.setAmount(Math.max(0.0F, health - 1.0F));
            state.recoverLeft = Math.min(player.m_21233_() * 0.5F, 10.0F);
            state.recoverTicks = GraftRules.RECOVER_TICKS;
            player.m_5661_(Component.m_237115_("message.hexkagune.graft.survived"), true);
            KaguneServerController.playSelf(player, HexKaguneSounds.BONE_CRACK, 1.0F, 0.6F);
            KaguneNetwork.sendTo(player, player);
            return;
        }
        if (amount >= GraftRules.HEAVY_BLOW) {
            float share = (state.kakuja ? GraftRules.RECOVER_SHARE_KAKUJA : GraftRules.RECOVER_SHARE)
                + (hasMutation(player, GraftMutation.KAKUHOU_OVERDRIVE) ? GraftMutation.OVERDRIVE_RECOVER : 0.0F);
            state.recoverLeft = Math.min(20.0F, state.recoverLeft + amount * share);
            state.recoverTicks = GraftRules.RECOVER_TICKS;
        }
    }

    // ---------------------------------------------------------------- tracking

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof ServerPlayer subject && isGraft(subject)) {
            ControlNetwork.sendTo(viewer, snapshot(subject, state(subject)));
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            STATES.remove(player.m_20148_());
            if (isGraft(player)) {
                broadcast(player, state(player));
            }
        }
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GraftState state = STATES.get(player.m_20148_());
            if (state != null) {
                GraftCombat.release(player, state, false);
                state.calm();
                state.kakuja = false;
                if (isGraft(player)) {
                    broadcast(player, state);
                }
            }
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GraftState state = STATES.get(player.m_20148_());
            if (state != null) {
                GraftCombat.release(player, state, false);
                state.calm();
                if (isGraft(player)) {
                    broadcast(player, state);
                }
            }
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GraftState state = STATES.remove(player.m_20148_());
            if (state != null) {
                GraftCombat.release(player, state, false);
            }
        }
    }
}
