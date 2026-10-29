package com.hex.kagune.control.awakening;

import com.hex.kagune.control.king.KingMarkFx;
import com.hex.kagune.control.king.KingMove;
import com.hex.kagune.control.king.PeaceFxModel;
import com.hex.kagune.control.king.PeaceMotion;
import com.hex.kagune.control.king.PeaceRules;
import com.hex.kagune.control.king.PeaceStrike;
import com.hex.kagune.control.king.WarRules;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CKingFx;
import com.hex.kagune.control.server.FleshThralls;
import com.hex.kagune.control.server.KingEffects;
import com.hex.kagune.control.server.KingServer;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.sound.HexKaguneSounds;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;

/**
 * 1.0.16: Peace's kit on the server ({@link PeaceRules}, {@link PeaceStrike}), as {@link KingServer}'s kit while Peace is worn: its M1
 * and its six abilities. Its M1's blades cut along their own paths ({@link PeaceMotion}, the same every client draws): each tick each
 * blade that is cutting sweeps from where its point was to where it is, and what that sweep passes through is struck, once a blow.
 * 1.0.34: the six rebuilt (the user's design, {@link PeaceRules}): what each does to bodies is done here, tick by tick, and every
 * client is told what to draw of it ({@link S2CKingFx}'s PEACE_* kinds: the blades into bodies and out, the shields, the lifting
 * tendrils, the flesh torn up, the lance, the creatures).
 */
final class PeaceKit implements KingServer.Kit {
    static final PeaceKit INSTANCE = new PeaceKit();

    /** Blocks round a blade's line that it cuts. */
    private static final double CUT = 0.9;

    /** Crossblade Waltz: at whom, since when, where it is held, what its blades owe it (paid as they tear out). */
    private static final class Waltz {
        LivingEntity prey;
        long at;
        V3 held;
        float owed;
    }

    /** White Wing Ascent: since when, from where, the two tendrils' feet, how high, what now (0 up, 1 wrenched down, 2 let down). */
    private static final class Lift {
        long at;
        V3 from;
        V3 left;
        V3 right;
        double height;
        int phase;
        long phaseAt;
        double phaseFrom;
    }

    /** The flesh torn up out of the ground: its seed, since when, where, its tendrils (as every client lays them out), their feet. */
    private static final class Erupt {
        int seed;
        long at;
        V3 centre;
        double[][] layout;
        V3[] bases;
        boolean[] up;
        final Set<Integer> struck = new HashSet<>();
        final List<Object[]> lashes = new ArrayList<>();
    }

    /** Piercing Salvation: charging since, driven out when, from where, which way, how long and how charged; how far its point has gone;
     *  whom it carries, how far; pinned, called back when. */
    private static final class Lance {
        long chargeAt;
        long firedAt = -1L;
        V3 from;
        V3 dir;
        double length;
        double k;
        double gone;
        LivingEntity prey;
        double carried;
        V3 pinned;
        long pinnedAt = -1L;
        long backAt = -1L;
    }

    /** The Final Horizon: at whom, since when; where it found the body, the way to it from him and its right; where the body is held;
     *  falling, driven, how far; what its blows owe it; let go. */
    private static final class Horizon {
        LivingEntity prey;
        long at;
        V3 origin;
        V3 away;
        V3 right;
        V3 from;
        V3 body;
        double floor;
        boolean falling;
        long fellAt = -1L;
        long landedAt = -1L;
        double driven;
        boolean stopped;
        float owed;
        long releasedAt = -1L;
        V3 hover;
        /** 1.0.36: ticks it has been driven on into solid blocks in the fall; whether it fell of itself before (its gravity, given back). */
        int drilled;
        boolean fell = true;
    }

    private static final class State {
        // The M1 (1.0.20: and the fighting grammar's blows, KingCombo: blows chained on the ground and in the air, the flags the kept
        // click came with, whether this blow is thrown in the air, the launch's chase, the heavy charging, what the ender is worth)
        int chained;
        int airHits;
        int queuedFlags;
        boolean strikeAir;
        long chaseUntil = Long.MIN_VALUE;
        long chargeAt = -1L;
        double worth = 1.0;
        PeaceStrike strike;
        long strikeAt;
        PeaceStrike last;
        long strikeEnded = Long.MIN_VALUE / 2;
        long queuedAt = Long.MIN_VALUE;
        long recoverUntil = Long.MIN_VALUE;
        // The move the blades are in (as every client has it), and what each blade has cut in its current cut
        KingMove move = KingMove.NONE;
        int level;
        long moveAt;
        final boolean[] wasCutting = new boolean[PeaceMotion.BLADES];
        final V3[] lastPoint = new V3[PeaceMotion.BLADES];
        final V3[] lastHeart = new V3[PeaceMotion.BLADES];
        final List<Set<Integer>> cut = new ArrayList<>();
        final Set<Integer> struck = new HashSet<>();
        // 1.0.34: the six
        Waltz waltz;
        long aegisAt = -1L;
        final long[] shieldAt = {Long.MIN_VALUE / 2, Long.MIN_VALUE / 2, Long.MIN_VALUE / 2};
        double lastY = Double.NaN;
        Lift lift;
        Erupt erupt;
        Lance lance;
        long rebirthAt = -1L;
        long rebornAt = -1L;
        Horizon horizon;
        long hangUntil = Long.MIN_VALUE;

        State() {
            for (int i = 0; i < PeaceMotion.BLADES; i++) {
                cut.add(new HashSet<>());
            }
        }
    }

    private final Map<UUID, State> states = new HashMap<>();

    private PeaceKit() {
    }

    private State state(ServerPlayer player) {
        return states.computeIfAbsent(player.getUUID(), id -> new State());
    }

    // ------------------------------------------------------------------ the body's move

    private void move(ServerPlayer player, State state, KingMove move, int level) {
        long now = player.serverLevel().getGameTime();
        state.move = move;
        state.level = level;
        state.moveAt = now;
        state.struck.clear();
        for (int i = 0; i < PeaceMotion.BLADES; i++) {
            state.cut.get(i).clear();
            state.wasCutting[i] = false;
            state.lastPoint[i] = null;
            state.lastHeart[i] = null;
        }
        KingServer.move(player, move, level, 0);
    }

    private void still(ServerPlayer player, State state) {
        state.move = KingMove.NONE;
        KingServer.move(player, KingMove.NONE, 0, 0);
    }

    // ------------------------------------------------------------------ keys

    @Override
    public void key(ServerPlayer player, int slot, int action, int target) {
        PeaceRules.Ability ability = PeaceRules.Ability.bySlot(slot);
        if (ability == null) return;
        State state = state(player);
        long now = player.serverLevel().getGameTime();
        if (action == WarRules.RELEASE) {
            // The held two go when their key is let go.
            if (ability == PeaceRules.Ability.PIERCE && state.lance != null && state.lance.firedAt < 0L) fire(player, state, now);
            if (ability == PeaceRules.Ability.REBIRTH && state.rebirthAt >= 0L) reborn(player, state, now);
            return;
        }
        // Pressed again while it runs: the Ascent wrenches him down; the lance is called back.
        if (ability == PeaceRules.Ability.ASCENT && state.lift != null) {
            if (state.lift.phase != 1 && player.getY() - state.lift.from.y() > 2.5) slam(player, state, now);
            return;
        }
        if (ability == PeaceRules.Ability.PIERCE && state.lance != null) {
            if (state.lance.backAt < 0L) recall(player, state, now);
            return;
        }
        if (ability == PeaceRules.Ability.AEGIS && state.aegisAt >= 0L) return;
        if (busy(state)) return;
        if (!ready(player, ability, now)) return;
        LivingEntity prey = null;
        if (ability == PeaceRules.Ability.WALTZ || ability == PeaceRules.Ability.HORIZON) {
            double range = ability == PeaceRules.Ability.WALTZ ? PeaceRules.WALTZ_RANGE : PeaceRules.HORIZON_RANGE;
            prey = WarPath.aimed(player, target, range);
            if (prey == null || !valid(player, prey)) {
                player.displayClientMessage(Component.literal("§f" + ability.displayName() + "§7: no one in reach"), true);
                return;
            }
        }
        state.strike = null;
        state.chargeAt = -1L; // an ability cuts a heavy's charge off
        state.queuedAt = Long.MIN_VALUE;
        KingServer.cooldown(player, slot, now + ability.cooldown());
        switch (ability) {
            case WALTZ -> waltz(player, state, prey, now);
            case AEGIS -> aegis(player, state, now);
            case ASCENT -> ascend(player, state, now);
            case PIERCE -> gather(player, state, now);
            case REBIRTH -> bury(player, state, now);
            case HORIZON -> horizon(player, state, prey, now);
        }
    }

    /** True while an ability has the blades (no other, and no M1). The Aegis's shields are their own: it leaves them free. */
    private static boolean busy(State state) {
        return state.waltz != null || state.lift != null || state.lance != null || state.rebirthAt >= 0L || state.rebornAt >= 0L || state.horizon != null
                || state.move == KingMove.LANDING;
    }

    private static boolean ready(ServerPlayer player, PeaceRules.Ability ability, long now) {
        long at = KingServer.ready(player, ability.ordinal());
        if (at <= now) return true;
        player.displayClientMessage(Component.literal("§f" + ability.displayName() + "§7: " + String.format(java.util.Locale.ROOT, "%.1f", (at - now) / 20.0) + " s"), true);
        return false;
    }

    // ------------------------------------------------------------------ the M1

    @Override
    public void strike(ServerPlayer player, int target) {
        this.strike(player, target, 0);
    }

    /** 1.0.20: an M1 with its flags (KingCombo): the chain's next blow, the launcher, an air blow or the spike. */
    @Override
    public void strike(ServerPlayer player, int target, int flags) {
        State state = state(player);
        long now = player.serverLevel().getGameTime();
        if (busy(state) || state.chargeAt >= 0L) return;
        if (state.strike != null) {
            state.queuedAt = now;
            state.queuedFlags = flags;
            return;
        }
        if (now < state.recoverUntil) return;
        begin(player, state, now, flags);
    }

    /**
     * 1.0.20: the heavy (RMB). Pressed in the air: Judgment; right after a blow: Halo Rend (worth more for every blow before it); else
     * the blades are drawn back to charge, and let go it is Cross Cleave, or held long enough the Seraph Thrust.
     */
    @Override
    public void heavy(ServerPlayer player, int flags, int target) {
        State state = state(player);
        long now = player.serverLevel().getGameTime();
        if (busy(state)) return;
        boolean press = (flags & com.hex.kagune.control.king.KingCombo.PRESS) != 0;
        if (!press) {
            if (state.chargeAt >= 0L) release(player, state, now);
            return;
        }
        if (state.chargeAt >= 0L || now < state.recoverUntil) return;
        boolean air = !KingEffects.grounded(player) || now < state.chaseUntil;
        long since = state.strike != null ? 0L : now - state.strikeEnded;
        com.hex.kagune.control.king.KingCombo.Heavy heavy = com.hex.kagune.control.king.KingCombo.heavy(true, air, state.chained, since, 0L);
        if (state.strike != null && heavy != com.hex.kagune.control.king.KingCombo.Heavy.ENDER && heavy != com.hex.kagune.control.king.KingCombo.Heavy.SPIKE) {
            return;
        }
        switch (heavy) {
            case SPIKE -> {
                state.strike = null;
                start(player, state, PeaceStrike.JUDGMENT, now, true);
            }
            case ENDER -> {
                state.worth = com.hex.kagune.control.king.KingCombo.ender(state.chained);
                state.strike = null;
                start(player, state, PeaceStrike.HALO, now, false);
            }
            default -> {
                state.chargeAt = now;
                move(player, state, KingMove.CHARGE, 0);
                vanilla(player, center(player), "block.amethyst_block.resonate", 1.0F, 0.8F);
            }
        }
    }

    /** The heavy let go (or held to the end): Cross Cleave, or the Seraph Thrust if it was held long enough. */
    private void release(ServerPlayer player, State state, long now) {
        long held = now - state.chargeAt;
        state.chargeAt = -1L;
        com.hex.kagune.control.king.KingCombo.Heavy heavy = com.hex.kagune.control.king.KingCombo.heavy(false, false, 0, -1L, held);
        start(player, state, heavy == com.hex.kagune.control.king.KingCombo.Heavy.CHARGED ? PeaceStrike.SERAPH : PeaceStrike.CROSS_CLEAVE, now, false);
    }

    /** The next blow by the fighting grammar (KingCombo): the chain's (or the Lance, gone cold), the launcher, an air blow, the spike. */
    private void begin(ServerPlayer player, State state, long now, int flags) {
        boolean air = !KingEffects.grounded(player) || now < state.chaseUntil;
        if (now - state.strikeEnded > com.hex.kagune.control.king.WarStrike.CHAIN_WINDOW) state.chained = 0;
        if (!air) state.airHits = 0;
        com.hex.kagune.control.king.KingCombo.Blow kind = com.hex.kagune.control.king.KingCombo.blow(state.chained, state.airHits, flags, air);
        PeaceStrike blow = switch (kind) {
            case LAUNCH -> PeaceStrike.ASCEND;
            case AIR, SPIKE -> PeaceStrike.air(state.airHits);
            default -> PeaceStrike.after(state.last, now - state.strikeEnded);
        };
        switch (kind) {
            case LAUNCH -> state.chained = 0;
            case AIR, SPIKE -> state.airHits++;
            default -> state.chained = blow == PeaceStrike.LANCE ? 1 : state.chained + 1;
        }
        start(player, state, blow, now, kind == com.hex.kagune.control.king.KingCombo.Blow.AIR || kind == com.hex.kagune.control.king.KingCombo.Blow.SPIKE);
    }

    /** Blow {@code blow} begun now ({@code air}: thrown in the air: he hangs there for it). */
    private void start(ServerPlayer player, State state, PeaceStrike blow, long now, boolean air) {
        state.strike = blow;
        state.strikeAt = now;
        state.strikeAir = air;
        state.last = blow;
        state.queuedAt = Long.MIN_VALUE;
        move(player, state, KingMove.BLADES, blow.ordinal());
        // 1.0.40: on its way: another's M1 landing on him before his blades cut meets it (KaguneClash), and both stop.
        com.hex.kagune.control.server.KaguneClash.swing(player, now, blow.impact + CLASH_LATE, CLASH_REACH, () -> state.strike == blow && state.strikeAt == now,
                () -> clashed(player, state));
        play(player, center(player), HexKaguneSounds.KAGUNE_WHIP, 0.7F, blow == PeaceStrike.CRUCIFIX || blow.special() ? 0.8F : 1.3F);
        if (air) {
            player.fallDistance = 0.0F;
            KingCombos.motion(player, 0.0, 0.02, 0.0); // 1.0.36: (the server's own speed for him is stale)
        }
        if (blow == PeaceStrike.SERAPH) vanilla(player, center(player), "item.trident.riptide_3", 1.2F, 1.2F);
    }

    /** 1.0.40: how far his M1's blades reach out to meet another's, and how long past its impact tick they may still be cutting. */
    private static final double CLASH_REACH = 4.5;
    private static final int CLASH_LATE = 4;

    /** 1.0.40: his blow met another's coming back (KaguneClash): cut off, nothing queued after it, a moment before he can strike again. */
    private void clashed(ServerPlayer player, State state) {
        long now = player.serverLevel().getGameTime();
        state.strike = null;
        state.queuedAt = Long.MIN_VALUE;
        state.strikeEnded = now;
        state.recoverUntil = now + com.hex.kagune.control.ClashRules.STAGGER;
        state.chained = 0;
        state.airHits = 0;
        move(player, state, KingMove.NONE, 0);
    }

    private void striking(ServerPlayer player, State state, long now) {
        PeaceStrike blow = state.strike;
        if (blow == null) return;
        long age = now - state.strikeAt;
        if (age == blow.impact - 1) vanilla(player, center(player), "entity.player.attack.sweep", 0.8F, 1.4F);
        if (age >= blow.chain && state.queuedAt >= state.strikeAt && !blow.finisher()) {
            state.strikeEnded = now;
            state.strike = null;
            begin(player, state, now, state.queuedFlags);
            return;
        }
        if (age >= blow.duration) {
            state.strike = null;
            state.strikeEnded = now;
            if (blow.finisher()) {
                state.recoverUntil = now + (blow.special() ? 6 : com.hex.kagune.control.king.WarStrike.RECOVERY);
                state.chained = 0;
                state.airHits = 0;
            }
            state.move = KingMove.NONE;
            KingServer.move(player, KingMove.NONE, 0, 0);
        }
    }

    /**
     * 1.0.20: the heavy charging (held to the end, it goes off by itself; pale light gathering on the blades); whatever he spiked that
     * lands breaks the ground there, and what is round it.
     */
    private void combos(ServerPlayer player, State state, long now) {
        if (state.chargeAt >= 0L && now - state.chargeAt >= com.hex.kagune.control.king.KingCombo.CHARGE_MAX) release(player, state, now);
        if (state.chargeAt >= 0L && (now - state.chargeAt) % 6 == 0) {
            mark(player, com.hex.kagune.control.king.KingMarkFx.GLINT, center(player).add(0.0, 0.6, 0.0).sub(facing(player).scale(0.8)), V3.ZERO,
                    0.4 + 0.6 * com.hex.kagune.control.king.KingCombo.charge(now - state.chargeAt), null);
        }
        for (KingCombos.Landing landing : KingCombos.landed(player, now)) {
            V3 at = landing.at();
            mark(player, com.hex.kagune.control.king.KingMarkFx.QUAKE, at, V3.ZERO, 1.6 * landing.power(), null);
            mark(player, com.hex.kagune.control.king.KingMarkFx.GLINT, at.add(0.0, 0.6, 0.0), new V3(0.0, 1.0, 0.0), 1.4 * landing.power(), null);
            play(player, at, HexKaguneSounds.KAGUNE_SLAM, 1.6F, 0.8F);
            vanilla(player, at, "block.amethyst_cluster.break", 1.4F, 0.7F);
            for (LivingEntity body : near(player, at, com.hex.kagune.control.king.KingCombo.CRATER + 1.0)) {
                if (!valid(player, body)) continue;
                if (new V3(body.getX() - at.x(), 0.0, body.getZ() - at.z()).length() > com.hex.kagune.control.king.KingCombo.CRATER) continue;
                if (hurt(player, body, com.hex.kagune.control.king.KingCombo.CRATER_DAMAGE * (float) landing.power())) KingEffects.stun(body, 20);
            }
        }
    }

    // ------------------------------------------------------------------ the M1's cuts, along the blades' own paths

    /** Each blade cutting now sweeps from where it was to where it is: what that sweep passes through is struck, once a cut. */
    private void cutting(ServerPlayer player, State state, long now) {
        if (state.move == KingMove.NONE) return;
        double age = now - state.moveAt;
        double pitch = -Math.toRadians(player.getXRot());
        V3 feet = new V3(player.getX(), player.getY(), player.getZ());
        double yaw = Math.toRadians(player.getYRot());
        List<V3> contacts = new ArrayList<>();
        List<Integer> hitIds = new ArrayList<>();
        for (int blade = 0; blade < PeaceMotion.BLADES; blade++) {
            boolean cutting = PeaceMotion.cutting(blade, state.move, state.level, age);
            if (!cutting) {
                state.wasCutting[blade] = false;
                state.lastPoint[blade] = null;
                state.lastHeart[blade] = null;
                continue;
            }
            if (!state.wasCutting[blade]) {
                state.cut.get(blade).clear(); // a new cut
                state.wasCutting[blade] = true;
            }
            PeaceMotion.Tip tip = PeaceMotion.clear(blade, PeaceMotion.pose(blade, now, state.move, state.level, age, pitch, 0.0, 0.0));
            V3 heart = world(tip.at(), feet, yaw);
            V3 point = world(PeaceMotion.point(tip, blade), feet, yaw);
            V3 lastPoint = state.lastPoint[blade] == null ? point : state.lastPoint[blade];
            V3 lastHeart = state.lastHeart[blade] == null ? heart : state.lastHeart[blade];
            state.lastPoint[blade] = point;
            state.lastHeart[blade] = heart;
            V3 low = new V3(Math.min(Math.min(heart.x(), point.x()), Math.min(lastHeart.x(), lastPoint.x())),
                    Math.min(Math.min(heart.y(), point.y()), Math.min(lastHeart.y(), lastPoint.y())),
                    Math.min(Math.min(heart.z(), point.z()), Math.min(lastHeart.z(), lastPoint.z())));
            V3 high = new V3(Math.max(Math.max(heart.x(), point.x()), Math.max(lastHeart.x(), lastPoint.x())),
                    Math.max(Math.max(heart.y(), point.y()), Math.max(lastHeart.y(), lastPoint.y())),
                    Math.max(Math.max(heart.z(), point.z()), Math.max(lastHeart.z(), lastPoint.z())));
            for (LivingEntity body : player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                    new AABB(low.x() - 2.0, low.y() - 2.5, low.z() - 2.0, high.x() + 2.0, high.y() + 1.0, high.z() + 2.0), b -> valid(player, b))) {
                if (state.cut.get(blade).contains(body.getId())) continue;
                // A blow strikes each body once between all its blades.
                if (state.struck.contains(body.getId())) continue;
                boolean blades = state.move == KingMove.BLADES;
                V3 c = center(body);
                double reach = CUT + body.getBbWidth() * 0.5;
                double d = Math.min(Math.min(c.distanceToSegment(lastPoint, point), c.distanceToSegment(lastHeart, heart)), c.distanceToSegment(heart, point));
                if (d > reach && Math.abs(c.y() - point.y()) > body.getBbHeight() * 0.5 + CUT) continue;
                if (d > reach + body.getBbHeight() * 0.35) continue;
                // 1.0.40: a player whose own M1 is on its way back at him: the two meet, and neither cuts.
                if (blades && com.hex.kagune.control.server.KaguneClash.meets(player, body, now)) return;
                state.cut.get(blade).add(body.getId());
                state.struck.add(body.getId());
                if (strike(player, state, body, blade, age, point.sub(lastPoint))) {
                    if (blades) com.hex.kagune.control.server.KaguneClash.landed(player);
                    contacts.add(c);
                    hitIds.add(body.getId());
                }
            }
        }
        if (!contacts.isEmpty()) {
            int[] ids = new int[hitIds.size()];
            for (int i = 0; i < ids.length; i++) ids[i] = hitIds.get(i);
            fx(player, S2CKingFx.PEACE_HIT, ids, contacts.toArray(new V3[0]));
        }
    }

    /** One body cut by blade {@code blade} in a blow of the M1, {@code age} ticks in, the blade going {@code along}. */
    private boolean strike(ServerPlayer player, State state, LivingEntity body, int blade, double age, V3 along) {
        if (state.move != KingMove.BLADES) return false;
        V3 push = along.withY(0.0).length() > 1.0E-4 ? along.withY(0.0).normalize().scale(0.45) : V3.ZERO;
        double lift = 0.12;
        PeaceStrike blow = PeaceStrike.byOrdinal(state.level);
        float damage = blow.hit() * (blow == PeaceStrike.HALO ? (float) state.worth : 1.0F);
        int bleed = blow.bleeds ? PeaceStrike.BLEED : 0;
        int stun = blow.finisher() ? PeaceStrike.FINISH_STUN : PeaceStrike.STUN;
        if (blow.finisher()) push = push.scale(2.0);
        if (blow == PeaceStrike.HALO) {
            // Flung away from him, all round.
            V3 away = center(body).sub(center(player)).withY(0.0);
            push = (away.length() < 1.0E-3 ? facing(player) : away.normalize()).scale(1.5);
            lift = 0.4;
        }
        if (blow == PeaceStrike.SERAPH || blow == PeaceStrike.CROSS_CLEAVE) {
            push = facing(player).scale(blow == PeaceStrike.SERAPH ? 1.5 : 1.0);
            lift = 0.3;
        }
        if (!hurt(player, body, damage)) return false;
        if (blow.breaksGuard()) KingEffects.breakGuard(body);
        if (body.isAlive() && (blow == PeaceStrike.ASCEND || blow == PeaceStrike.JUDGMENT || state.strikeAir)) {
            // 1.0.20: the launcher throws it up (and him after it), the spike drives it down, an air blow keeps it floating before him.
            V3 facing = KingCombos.toward(player, body, facing(player));
            long now = player.serverLevel().getGameTime();
            if (blow == PeaceStrike.ASCEND) {
                KingCombos.launch(player, body, facing);
                state.chaseUntil = now + com.hex.kagune.control.king.KingCombo.CHASE_TICKS;
                mark(player, KingMarkFx.GLINT, center(body), new V3(0.0, 1.0, 0.0), 1.3, null);
            } else if (blow == PeaceStrike.JUDGMENT) {
                KingCombos.spike(player, body, facing, 1.0, now);
            } else {
                KingCombos.air(player, body, facing);
            }
            if (bleed > 0) KingEffects.bleed(body, player, bleed, 0);
        } else if (body.isAlive()) {
            Vec3 was = body.getDeltaMovement();
            double steady = 1.0 - 0.75 * KingEffects.steady(body);
            body.setDeltaMovement(was.x * 0.2 + push.x() * steady, was.y * 0.2 + lift, was.z * 0.2 + push.z() * steady);
            body.hurtMarked = true;
            if (body instanceof ServerPlayer hit && hit.connection != null) hit.connection.send(new ClientboundSetEntityMotionPacket(hit));
            KingEffects.stun(body, stun);
            if (bleed > 0) KingEffects.bleed(body, player, bleed, 0);
        }
        play(player, center(body), HexKaguneSounds.FLESH_IMPACT, 0.9F, 1.1F);
        return true;
    }

    // ------------------------------------------------------------------ Z: Crossblade Waltz

    private void waltz(ServerPlayer player, State state, LivingEntity prey, long now) {
        Waltz w = new Waltz();
        w.prey = prey;
        w.at = now;
        state.waltz = w;
        move(player, state, KingMove.WALTZ, 0);
        play(player, center(player), HexKaguneSounds.KAGUNE_RESHAPE, 1.4F, 1.3F);
    }

    /**
     * The blades sent at it one at a time, each through it and left there (it held where the first caught it); all in, a beat, and they
     * tear out of it together, blood thrown every way; then home. Its wounds never kill it on the way: what would have is paid as they
     * tear out.
     */
    private void waltzing(ServerPlayer player, State state, long now) {
        Waltz w = state.waltz;
        if (w == null) return;
        long age = now - w.at;
        LivingEntity prey = w.prey;
        boolean gone = !prey.isAlive() || prey.isRemoved() || prey.level() != player.level() || center(prey).distance(center(player)) > PeaceRules.WALTZ_RANGE + 8.0;
        if (gone && age < PeaceRules.WALTZ_RIP) {
            fx(player, S2CKingFx.PEACE_TEAR, new int[] {prey.getId(), 1});
            state.waltz = null;
            still(player, state);
            return;
        }
        for (int i = 0; i < PeaceRules.WALTZ_ORDER.length; i++) {
            int blade = PeaceRules.WALTZ_ORDER[i];
            long sent = PeaceRules.waltzSent(blade);
            if (age == sent) {
                fx(player, S2CKingFx.PEACE_IMPALE, new int[] {blade, prey.getId(), 0});
                play(player, center(player), HexKaguneSounds.KAGUNE_WHIP, 1.0F, 1.2F + 0.08F * i);
            } else if (age == sent + PeaceRules.WALTZ_REACH) {
                V3 c = center(prey);
                w.owed += bruise(player, prey, PeaceRules.WALTZ_STAB);
                mark(player, KingMarkFx.BLOOD, c, c.sub(center(player)).normalize(), 0.9, prey);
                play(player, c, HexKaguneSounds.FLESH_IMPACT, 1.1F, 0.9F + 0.06F * i);
                KingEffects.stun(prey, 30);
                if (w.held == null) w.held = feet(prey);
            }
        }
        if (w.held != null && age < PeaceRules.WALTZ_RIP) hold(prey, w.held); // run through, it goes nowhere
        if (age == PeaceRules.WALTZ_RIP) {
            V3 c = center(prey);
            fx(player, S2CKingFx.PEACE_TEAR, new int[] {prey.getId(), 0});
            float owed = w.owed;
            w.owed = 0.0F;
            if (hurt(player, prey, PeaceRules.WALTZ_TEAR + owed) && prey.isAlive()) {
                KingEffects.bleed(prey, player, PeaceRules.WALTZ_BLEED, 1);
                KingEffects.stun(prey, 30);
                V3 away = c.sub(center(player)).withY(0.0);
                if (away.length() > 1.0E-3) KingCombos.motion(prey, away.normalize().x() * 0.5, 0.25, away.normalize().z() * 0.5);
            }
            mark(player, KingMarkFx.GORE, c, new V3(0.0, 1.0, 0.0), 1.5, prey);
            play(player, c, HexKaguneSounds.FLESH_IMPACT, 2.0F, 0.6F);
            play(player, c, HexKaguneSounds.BONE_CRACK, 1.6F, 0.8F);
            vanilla(player, c, "entity.player.attack.sweep", 1.6F, 0.6F);
        }
        if (age >= PeaceRules.WALTZ_TICKS) {
            state.waltz = null;
            still(player, state);
        }
    }

    // ------------------------------------------------------------------ J: Aegis of :Re

    private void aegis(ServerPlayer player, State state, long now) {
        state.aegisAt = now;
        state.lastY = Double.NaN;
        fx(player, S2CKingFx.PEACE_AEGIS, new int[] {1});
        play(player, center(player), HexKaguneSounds.KAGUNE_RESHAPE, 1.4F, 0.8F);
        play(player, center(player), HexKaguneSounds.KAGUNE_GUARD, 1.4F, 0.9F);
    }

    /**
     * The three shields' fifteen seconds: what crowds him shoved off (unhurt), lava pushed off from, and if he is in it, a ledge near
     * reached for and he hauled out onto it; a fall that would hurt caught on them near the ground. (Every harm is taken on them:
     * {@link #hurt}; every shot sent back: {@link #deflect}.)
     */
    private void shielding(ServerPlayer player, State state, long now) {
        if (state.aegisAt < 0L) return;
        long age = now - state.aegisAt;
        if (age >= PeaceRules.AEGIS_TICKS) {
            state.aegisAt = -1L;
            fx(player, S2CKingFx.PEACE_AEGIS, new int[] {0});
            play(player, center(player), HexKaguneSounds.KAGUNE_RESHAPE, 1.2F, 1.1F);
            return;
        }
        ServerLevel level = player.serverLevel();
        V3 c = center(player);
        V3 feet = feet(player);
        if (age % PeaceRules.AEGIS_SHOVE_EVERY == 0) {
            for (LivingEntity body : near(player, PeaceRules.AEGIS_SHOVE_RADIUS + 1.5)) {
                if (!valid(player, body)) continue;
                V3 away = center(body).sub(c).withY(0.0);
                if (away.length() > PeaceRules.AEGIS_SHOVE_RADIUS + body.getBbWidth() * 0.5 || Math.abs(body.getY() - player.getY()) > 2.5) continue;
                V3 way = away.length() < 1.0E-3 ? facing(player) : away.normalize();
                double steady = 1.0 - 0.75 * KingEffects.steady(body);
                KingCombos.motion(body, way.x() * PeaceRules.AEGIS_SHOVE * steady, 0.2, way.z() * PeaceRules.AEGIS_SHOVE * steady);
                shield(player, state, shieldToward(player, center(body)), PeaceRules.SHIELD_BLOCK, center(body), now, false);
            }
        }
        if (player.isInLava()) {
            // In it: a shield reaches for the nearest ledge and hauls him out onto it.
            V3 shore = shore(level, feet);
            V3 pull = shore == null ? new V3(0.0, 0.7, 0.0) : shore.add(0.0, 0.4, 0.0).sub(feet);
            double flat = Math.min(0.7, pull.withY(0.0).length());
            V3 h = pull.withY(0.0).length() < 1.0E-3 ? V3.ZERO : pull.withY(0.0).normalize().scale(flat);
            KingCombos.motion(player, h.x(), Math.max(0.45, Math.min(0.8, pull.y() * 0.4 + 0.45)), h.z());
            if (shore != null) shield(player, state, shieldToward(player, shore.add(0.0, 0.5, 0.0)), PeaceRules.SHIELD_GRAB, shore.add(0.0, 0.3, 0.0), now, true);
            player.clearFire();
        } else if (age % 2 == 0) {
            // Near it: pushed off from it.
            V3 lava = lavaNear(level, feet);
            if (lava != null) {
                V3 away = feet.sub(lava).withY(0.0);
                V3 way = away.length() < 1.0E-3 ? facing(player).scale(-1.0) : away.normalize();
                KingCombos.motion(player, way.x() * 0.45, 0.12, way.z() * 0.45);
                shield(player, state, shieldToward(player, lava.add(0.0, 0.8, 0.0)), PeaceRules.SHIELD_BLOCK, lava.add(0.0, 0.6, 0.0), now, false);
            }
        }
        if (age % 10 == 0) player.clearFire();
        // A fall that would hurt: caught on a shield thrown down under him as the ground comes.
        double y = player.getY();
        double fall = Double.isNaN(state.lastY) ? 0.0 : state.lastY - y;
        state.lastY = y;
        if (fall > PeaceRules.AEGIS_FALL_SPEED && !KingEffects.grounded(player)) {
            double ground = WarPath.ground(level, player.getX(), player.getZ(), y);
            if (!Double.isNaN(ground) && y - ground <= PeaceRules.AEGIS_CATCH) {
                KingCombos.motion(player, 0.0, -0.12, 0.0);
                player.fallDistance = 0.0F;
                V3 under = new V3(player.getX(), ground, player.getZ());
                if (shield(player, state, PeaceRules.SHIELD_TOP, PeaceRules.SHIELD_CATCH, under.add(0.0, 0.15, 0.0), now, false)) {
                    play(player, under, HexKaguneSounds.KAGUNE_GUARD, 1.2F, 0.7F);
                }
            }
        }
    }

    /** Shield {@code i} sent to do {@code action} at {@code at} (not again before it has held: {@code force} for what cannot wait). */
    private static boolean shield(ServerPlayer player, State state, int i, int action, V3 at, long now, boolean force) {
        if (!force && now - state.shieldAt[i] < PeaceRules.SHIELD_HOLD) return false;
        state.shieldAt[i] = now;
        fx(player, S2CKingFx.PEACE_SHIELD, new int[] {i, action}, at);
        return true;
    }

    /** The shield best placed to meet what comes from {@code at}. */
    private static int shieldToward(ServerPlayer player, V3 at) {
        V3 d = at.sub(center(player));
        V3 ahead = facing(player);
        V3 right = new V3(-ahead.z(), 0.0, ahead.x());
        return PeaceRules.shieldFor(d.dot(right), d.y(), d.dot(ahead));
    }

    /** The nearest ground out of the lava near {@code feet} with room to stand on it (its top), or null. */
    private static V3 shore(ServerLevel level, V3 feet) {
        V3 best = null;
        double bestD = Double.MAX_VALUE;
        int fx = (int) Math.floor(feet.x());
        int fy = (int) Math.floor(feet.y());
        int fz = (int) Math.floor(feet.z());
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                for (int dy = -1; dy <= 3; dy++) {
                    int x = fx + dx;
                    int y = fy + dy;
                    int z = fz + dz;
                    if (!WarPath.solid(level, x + 0.5, y + 0.5, z + 0.5) || lava(level, x, y, z)) continue;
                    if (WarPath.solid(level, x + 0.5, y + 1.5, z + 0.5) || WarPath.solid(level, x + 0.5, y + 2.5, z + 0.5) || lava(level, x, y + 1, z)) continue;
                    V3 top = new V3(x + 0.5, y + 1.0, z + 0.5);
                    double d = top.distance(feet);
                    if (d < bestD) {
                        bestD = d;
                        best = top;
                    }
                }
            }
        }
        return best;
    }

    /** Where the lava near {@code feet} is (the middle of what is within reach), or null. */
    private static V3 lavaNear(ServerLevel level, V3 feet) {
        double sx = 0.0;
        double sy = 0.0;
        double sz = 0.0;
        int n = 0;
        int fx = (int) Math.floor(feet.x());
        int fy = (int) Math.floor(feet.y());
        int fz = (int) Math.floor(feet.z());
        double reach = PeaceRules.AEGIS_LAVA_NEAR + 0.5;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -1; dy <= 0; dy++) {
                    if (!lava(level, fx + dx, fy + dy, fz + dz)) continue;
                    double cx = fx + dx + 0.5;
                    double cz = fz + dz + 0.5;
                    if (Math.hypot(cx - feet.x(), cz - feet.z()) > reach) continue;
                    sx += cx;
                    sy += fy + dy + 0.5;
                    sz += cz;
                    n++;
                }
            }
        }
        return n == 0 ? null : new V3(sx / n, sy / n, sz / n);
    }

    private static boolean lava(ServerLevel level, int x, int y, int z) {
        return level.getBlockState(new BlockPos(x, y, z)).getBlock() == Blocks.LAVA;
    }

    // ------------------------------------------------------------------ G: White Wing Ascent

    private void ascend(ServerPlayer player, State state, long now) {
        ServerLevel level = player.serverLevel();
        V3 from = feet(player);
        V3 ahead = facing(player);
        V3 right = new V3(-ahead.z(), 0.0, ahead.x());
        Lift lift = new Lift();
        lift.at = now;
        lift.from = from;
        lift.left = foot(level, from.add(right.scale(-PeaceRules.ASCENT_SPREAD)).add(ahead.scale(-0.4)), from.y());
        lift.right = foot(level, from.add(right.scale(PeaceRules.ASCENT_SPREAD)).add(ahead.scale(-0.4)), from.y());
        lift.height = Math.max(3.0, Math.min(PeaceRules.ASCENT_HEIGHT, WarPath.headroom(level, from.add(0.0, 2.0, 0.0), PeaceRules.ASCENT_HEIGHT + 2.0) - 0.2));
        lift.phaseAt = now;
        state.lift = lift;
        player.setNoGravity(true);
        fx(player, S2CKingFx.PEACE_LIFT, new int[] {0}, lift.left, lift.right);
        move(player, state, KingMove.ASCENT, 0);
        play(player, center(player), HexKaguneSounds.KAGUNE_WHIP, 1.6F, 0.7F);
        play(player, from, HexKaguneSounds.KAGUNE_SLAM, 1.2F, 1.2F);
    }

    /** Where one of the lift's tendrils plants itself: the ground at {@code at}, or level with him if there is none near. */
    private static V3 foot(ServerLevel level, V3 at, double y) {
        double ground = WarPath.ground(level, at.x(), at.z(), y + 1.0);
        return at.withY(Double.isNaN(ground) ? y : ground);
    }

    /** Up on the two tendrils, held there, then let down gently; or wrenched down (G again) into the landing. */
    private void lifting(ServerPlayer player, State state, long now) {
        Lift lift = state.lift;
        if (lift == null) return;
        player.fallDistance = 0.0F;
        // 1.0.36: held straight up over where he stood, carried along the lift's own path (glide): never chasing where the server last
        // saw him (that bounced him up and down the whole time).
        V3 over = lift.from;
        switch (lift.phase) {
            case 0 -> {
                long t = now - lift.at;
                glide(player, over.withY(lift.from.y() + PeaceRules.lifted(t, lift.height)), over.withY(lift.from.y() + PeaceRules.lifted(t + 1.0, lift.height)));
                if (t >= PeaceRules.ASCENT_RISE + PeaceRules.ASCENT_HOLD) {
                    lift.phase = 2;
                    lift.phaseAt = now;
                    lift.phaseFrom = Math.max(0.0, player.getY() - lift.from.y());
                    fx(player, S2CKingFx.PEACE_LIFT, new int[] {2});
                }
            }
            case 2 -> {
                long t = now - lift.phaseAt;
                double y = lift.from.y() + PeaceRules.lowered(t, lift.phaseFrom);
                double ground = WarPath.ground(player.serverLevel(), player.getX(), player.getZ(), player.getY());
                if (t >= PeaceRules.ASCENT_LOWER || t > 4 && (KingEffects.grounded(player) || !Double.isNaN(ground) && player.getY() - ground < 0.2)) {
                    state.lift = null;
                    player.setNoGravity(false);
                    fx(player, S2CKingFx.PEACE_LIFT, new int[] {3});
                    still(player, state);
                    return;
                }
                double next = lift.from.y() + PeaceRules.lowered(t + 1.0, lift.phaseFrom);
                double floor = Double.isNaN(ground) ? Double.NEGATIVE_INFINITY : ground;
                glide(player, over.withY(Math.max(y, floor)), over.withY(Math.max(next, floor)));
            }
            case 1 -> {
                long t = now - lift.phaseAt;
                double ground = WarPath.ground(player.serverLevel(), player.getX(), player.getZ(), player.getY());
                boolean down = t > 1 && (KingEffects.grounded(player) || !Double.isNaN(ground) && player.getY() - ground <= PeaceRules.SLAM_SPEED * 0.5);
                if (down || t > 40) {
                    land(player, state, now, Double.isNaN(ground) ? player.getY() : ground);
                    return;
                }
                double speed = Math.min(PeaceRules.SLAM_SPEED, 0.6 + 0.5 * t);
                if (!Double.isNaN(ground)) speed = Math.min(speed, Math.max(0.3, player.getY() - ground));
                KingCombos.motion(player, 0.0, -speed, 0.0);
            }
            default -> {
            }
        }
    }

    /** G again up there: the tendrils wrench him down. */
    private void slam(ServerPlayer player, State state, long now) {
        Lift lift = state.lift;
        lift.phase = 1;
        lift.phaseAt = now;
        player.setNoGravity(false);
        fx(player, S2CKingFx.PEACE_LIFT, new int[] {1});
        move(player, state, KingMove.DIVE, 0);
        play(player, center(player), HexKaguneSounds.KAGUNE_STRAIN, 1.6F, 0.8F);
        vanilla(player, center(player), "item.trident.riptide_3", 1.2F, 0.6F);
    }

    /** Down: the landing, the ground torn open all round and the flesh up out of it ({@link #erupting}). */
    private void land(ServerPlayer player, State state, long now, double ground) {
        state.lift = null;
        player.setNoGravity(false);
        player.fallDistance = 0.0F;
        KingCombos.motion(player, 0.0, -0.1, 0.0);
        fx(player, S2CKingFx.PEACE_LIFT, new int[] {3});
        move(player, state, KingMove.LANDING, 0);
        ServerLevel level = player.serverLevel();
        V3 at = new V3(player.getX(), ground, player.getZ());
        Erupt e = new Erupt();
        e.seed = (int) (WarPath.noise(now * 31L + player.getId(), 7) * Integer.MAX_VALUE);
        e.at = now;
        e.centre = at;
        e.layout = PeaceFxModel.eruption(e.seed, PeaceRules.ERUPT_COUNT, PeaceRules.ERUPT_RADIUS);
        e.bases = new V3[e.layout.length];
        e.up = new boolean[e.layout.length];
        float[] data = new float[5 + e.layout.length];
        data[0] = (float) at.x();
        data[1] = (float) at.y();
        data[2] = (float) at.z();
        data[3] = (float) PeaceRules.ERUPT_RADIUS;
        data[4] = e.layout.length;
        for (int i = 0; i < e.layout.length; i++) {
            double x = at.x() + e.layout[i][0];
            double z = at.z() + e.layout[i][1];
            double y = WarPath.ground(level, x, z, at.y() + 5.0);
            if (Double.isNaN(y)) y = WarPath.ground(level, x, z, at.y() - 8.0);
            e.bases[i] = Double.isNaN(y) ? null : new V3(x, y, z);
            data[5 + i] = Double.isNaN(y) ? Float.NaN : (float) y;
        }
        state.erupt = e;
        ControlNetwork.broadcast(player, new S2CKingFx(player.getUUID(), S2CKingFx.PEACE_ERUPT, new int[] {e.seed}, data));
        mark(player, KingMarkFx.QUAKE, at, V3.ZERO, 2.6, null);
        play(player, at, HexKaguneSounds.KAGUNE_SLAM, 3.0F, 0.5F);
        play(player, at, HexKaguneSounds.MANIFEST_RIP, 2.4F, 0.6F);
        vanilla(player, at, "entity.generic.explode", 1.6F, 0.6F);
        // What stands right where he comes down is thrown off it.
        for (LivingEntity body : near(player, at, 4.5)) {
            if (!valid(player, body) || feet(body).distance(at) > 3.5 + body.getBbWidth() * 0.5) continue;
            if (hurt(player, body, 8.0F) && body.isAlive()) {
                V3 away = center(body).sub(at).withY(0.0);
                V3 way = away.length() < 1.0E-3 ? facing(player) : away.normalize();
                double steady = 1.0 - 0.75 * KingEffects.steady(body);
                KingCombos.motion(body, way.x() * 0.9 * steady, 0.45, way.z() * 0.9 * steady);
                KingEffects.stun(body, 20);
            }
        }
    }

    /**
     * The flesh up out of the ground: each tendril as it comes up (spreading out from where he landed) strikes what it comes up through
     * or beside; while they stand, they lash out at what is hostile within their reach; then they sink back.
     */
    private void erupting(ServerPlayer player, State state, long now) {
        Erupt e = state.erupt;
        if (e == null) return;
        long age = now - e.at;
        if (age > PeaceRules.ERUPT_TICKS) {
            state.erupt = null;
            return;
        }
        for (int i = 0; i < e.layout.length; i++) {
            if (e.up[i] || e.bases[i] == null) continue;
            double local = age - e.layout[i][7] / PeaceRules.ERUPT_SPREAD;
            if (local < 0.0) continue;
            e.up[i] = true;
            double[][] path = PeaceFxModel.eruptionPath(new double[] {e.bases[i].x(), e.bases[i].y(), e.bases[i].z()}, e.layout[i], 8);
            double thick = e.layout[i][3];
            V3 base = e.bases[i];
            for (LivingEntity body : near(player, base, e.layout[i][2] + 3.0)) {
                if (!valid(player, body) || e.struck.contains(body.getId())) continue;
                V3 c = center(body);
                double d = Double.MAX_VALUE;
                for (int k = 0; k + 1 < path.length; k++) {
                    d = Math.min(d, c.distanceToSegment(new V3(path[k][0], path[k][1], path[k][2]), new V3(path[k + 1][0], path[k + 1][1], path[k + 1][2])));
                }
                if (d > thick + body.getBbWidth() * 0.5 + 0.6) continue;
                e.struck.add(body.getId());
                if (hurt(player, body, PeaceRules.ERUPT_DAMAGE) && body.isAlive()) {
                    KingEffects.stun(body, 20);
                    KingEffects.bleed(body, player, 80, 0);
                    V3 away = c.sub(base).withY(0.0);
                    V3 way = away.length() < 1.0E-3 ? V3.ZERO : away.normalize();
                    KingCombos.motion(body, way.x() * 0.4, 0.75, way.z() * 0.4);
                }
                mark(player, KingMarkFx.BLOOD, c, new V3(0.0, 1.0, 0.0), 1.0, body);
            }
            if (i % 4 == 0) play(player, base, HexKaguneSounds.MANIFEST_RIP, 1.0F, 0.7F + 0.4F * (float) WarPath.noise(e.seed, i));
        }
        // The lashes, each landing a few ticks after it is thrown.
        e.lashes.removeIf(lash -> {
            if (now < (long) lash[1]) return false;
            LivingEntity body = (LivingEntity) lash[0];
            V3 from = (V3) lash[2];
            if (body.isAlive() && valid(player, body) && center(body).distance(from) <= PeaceRules.LASH_REACH + 4.0) {
                if (hurt(player, body, PeaceRules.LASH_DAMAGE) && body.isAlive()) {
                    KingEffects.stun(body, 10);
                    V3 away = center(body).sub(from).withY(0.0);
                    if (away.length() > 1.0E-3) KingCombos.motion(body, away.normalize().x() * 0.5, 0.3, away.normalize().z() * 0.5);
                }
                mark(player, KingMarkFx.BLOOD, center(body), center(body).sub(from).normalize(), 0.9, body);
                play(player, center(body), HexKaguneSounds.KAGUNE_WHIP, 1.0F, 1.1F);
            }
            return true;
        });
        if (age < PeaceRules.ERUPT_GROW + 6 || age > PeaceRules.ERUPT_TICKS - PeaceRules.ERUPT_SINK || age % PeaceRules.LASH_EVERY != 0) return;
        Set<Integer> busy = new HashSet<>();
        for (LivingEntity body : near(player, e.centre, PeaceRules.ERUPT_RADIUS + PeaceRules.LASH_REACH)) {
            if (!valid(player, body) || !KingEffects.hostile(player, body)) continue;
            V3 c = center(body);
            int best = -1;
            double bestD = Double.MAX_VALUE;
            for (int i = 0; i < e.layout.length; i++) {
                if (!e.up[i] || e.bases[i] == null || busy.contains(i)) continue;
                double d = new V3(c.x() - e.bases[i].x(), 0.0, c.z() - e.bases[i].z()).length();
                if (d < bestD && d <= PeaceRules.LASH_REACH && c.y() - e.bases[i].y() < e.layout[i][2] + 2.0 && c.y() > e.bases[i].y() - 2.0) {
                    bestD = d;
                    best = i;
                }
            }
            if (best < 0) continue;
            busy.add(best);
            fx(player, S2CKingFx.PEACE_LASH, new int[] {e.seed, best, body.getId()});
            e.lashes.add(new Object[] {body, now + PeaceRules.LASH_TICKS / 2, e.bases[best].add(0.0, e.layout[best][2] * 0.6, 0.0)});
        }
    }

    // ------------------------------------------------------------------ B: Piercing Salvation

    private void gather(ServerPlayer player, State state, long now) {
        Lance lance = new Lance();
        lance.chargeAt = now;
        state.lance = lance;
        move(player, state, KingMove.PIERCE, 0);
        fx(player, S2CKingFx.PEACE_LANCE, new int[] {0});
        play(player, center(player), HexKaguneSounds.KAGUNE_STRAIN, 1.3F, 1.1F);
        play(player, center(player), HexKaguneSounds.KAGUNE_RESHAPE, 1.2F, 0.9F);
    }

    /** Let go: the lance driven out from where it gathered, along his look, as long and as hard as it was held for. */
    private void fire(ServerPlayer player, State state, long now) {
        Lance lance = state.lance;
        lance.k = PeaceRules.charge(now - lance.chargeAt);
        lance.length = PeaceRules.lance(lance.k);
        V3 look = look(player);
        lance.dir = look;
        lance.from = eye(player).add(0.0, -0.3, 0.0).add(look.scale(1.1 + lance.length));
        lance.firedAt = now;
        lance.gone = 0.0;
        move(player, state, KingMove.PIERCE, 1);
        fx(player, S2CKingFx.PEACE_LANCE, new int[] {1}, lance.from, look, new V3(lance.length, PeaceRules.PIERCE_SPEED, 0.0));
        vanilla(player, center(player), "item.trident.riptide_3", 1.4F, 1.1F - 0.3F * (float) lance.k);
        play(player, center(player), HexKaguneSounds.KAGUNE_WHIP, 1.8F, 0.7F);
    }

    /**
     * The lance: gathering (held; let go of itself at last); out, through the first body in its way; carrying it on, up to its reach,
     * or pinning it to the wall it meets; back. Its point is {@code from} on along {@code dir} by {@code gone}, as every client has it.
     */
    private void piercing(ServerPlayer player, State state, long now) {
        Lance lance = state.lance;
        if (lance == null) return;
        if (lance.backAt >= 0L) {
            if (now - lance.backAt >= PeaceRules.PIERCE_BACK) {
                state.lance = null;
                still(player, state);
            }
            return;
        }
        if (lance.firedAt < 0L) {
            long held = now - lance.chargeAt;
            if (held >= PeaceRules.PIERCE_CHARGE + 100) fire(player, state, now);
            else if (held % 10 == 0) play(player, center(player), HexKaguneSounds.KAGUNE_STRAIN, 0.8F, 0.8F + 0.6F * (float) PeaceRules.charge(held));
            return;
        }
        ServerLevel level = player.serverLevel();
        LivingEntity prey = lance.prey;
        if (prey != null && (!prey.isAlive() || prey.isRemoved() || prey.level() != level)) {
            recall(player, state, now);
            return;
        }
        if (lance.pinnedAt >= 0L) {
            hold(prey, lance.pinned);
            KingEffects.stun(prey, 6);
            if (now - lance.pinnedAt >= PeaceRules.PIERCE_PIN) recall(player, state, now);
            return;
        }
        if (prey != null) {
            // Carrying it on, until a wall stops them or it has gone as far as it will.
            V3 c = center(prey);
            double half = prey.getBbWidth() * 0.5;
            BlockHitResult wall = clip(player, c, c.add(lance.dir.scale(PeaceRules.PIERCE_CARRY + half + 0.1)));
            if (wall.getType() == HitResult.Type.BLOCK) {
                V3 hit = new V3(wall.getLocation().x, wall.getLocation().y, wall.getLocation().z);
                V3 against = hit.sub(lance.dir.scale(half + 0.15));
                lance.pinned = against.sub(new V3(0.0, prey.getBbHeight() * 0.5, 0.0));
                lance.pinnedAt = now;
                hold(prey, lance.pinned);
                fx(player, S2CKingFx.PEACE_LANCE, new int[] {3, prey.getId()});
                mark(player, KingMarkFx.QUAKE, hit, lance.dir.scale(-1.0), 1.2, null);
                mark(player, KingMarkFx.BLOOD, c, lance.dir.scale(-1.0), 1.2, prey);
                hurt(player, prey, 4.0F);
                play(player, hit, HexKaguneSounds.KAGUNE_SLAM, 2.0F, 0.7F);
                play(player, c, HexKaguneSounds.BONE_CRACK, 1.6F, 0.7F);
                return;
            }
            lance.carried += PeaceRules.PIERCE_CARRY;
            lance.gone += PeaceRules.PIERCE_CARRY;
            V3 at = c.add(lance.dir.scale(PeaceRules.PIERCE_CARRY));
            hold(prey, at.sub(new V3(0.0, prey.getBbHeight() * 0.5, 0.0)));
            KingEffects.stun(prey, 6);
            if (now % 3 == 0) mark(player, KingMarkFx.BLOOD, at, lance.dir.scale(-1.0), 0.6, prey);
            if (lance.carried >= PeaceRules.PIERCE_DRAG) {
                recall(player, state, now);
                KingCombos.motion(prey, lance.dir.x() * 0.7, lance.dir.y() * 0.7 + 0.2, lance.dir.z() * 0.7);
            }
            return;
        }
        // Out, nothing on it yet: the first body along its way this tick, else the first wall, else on.
        V3 was = lance.from.add(lance.dir.scale(lance.gone));
        V3 next = lance.from.add(lance.dir.scale(lance.gone + PeaceRules.PIERCE_SPEED));
        BlockHitResult wall = clip(player, was, next);
        V3 end = wall.getType() == HitResult.Type.BLOCK ? new V3(wall.getLocation().x, wall.getLocation().y, wall.getLocation().z) : next;
        LivingEntity first = null;
        double firstAlong = Double.MAX_VALUE;
        for (LivingEntity body : near(player, was.lerp(end, 0.5), PeaceRules.PIERCE_SPEED + 3.0)) {
            if (!valid(player, body)) continue;
            V3 c = center(body);
            if (c.distanceToSegment(was, end) > body.getBbWidth() * 0.5 + 0.6 + (Math.abs(c.y() - was.y()) < body.getBbHeight() * 0.5 ? 0.2 : 0.0)) continue;
            double along = c.sub(was).dot(lance.dir);
            if (along < firstAlong) {
                firstAlong = along;
                first = body;
            }
        }
        if (first != null) {
            lance.prey = first;
            lance.gone += Math.max(0.0, firstAlong);
            V3 c = center(first);
            fx(player, S2CKingFx.PEACE_LANCE, new int[] {2, first.getId()});
            mark(player, KingMarkFx.GORE, c, lance.dir, 1.3, first);
            play(player, c, HexKaguneSounds.FLESH_IMPACT, 2.0F, 0.6F);
            play(player, c, HexKaguneSounds.BONE_CRUSH, 1.6F, 0.8F);
            if (!hurt(player, first, PeaceRules.pierceDamage(lance.k)) || !first.isAlive()) {
                recall(player, state, now);
                return;
            }
            KingEffects.stun(first, 40);
            KingEffects.bleed(first, player, 100, 1);
            return;
        }
        if (wall.getType() == HitResult.Type.BLOCK) {
            mark(player, KingMarkFx.QUAKE, end, lance.dir.scale(-1.0), 0.8, null);
            play(player, end, HexKaguneSounds.KAGUNE_SLAM, 1.4F, 1.2F);
            recall(player, state, now);
            return;
        }
        lance.gone += PeaceRules.PIERCE_SPEED;
        if (lance.gone >= PeaceRules.PIERCE_RANGE) recall(player, state, now);
    }

    /** The lance called back (its course run, or B again): whatever it held let go. */
    private void recall(ServerPlayer player, State state, long now) {
        Lance lance = state.lance;
        if (lance == null || lance.backAt >= 0L) return;
        lance.backAt = now;
        fx(player, S2CKingFx.PEACE_LANCE, new int[] {4, lance.prey == null ? 0 : lance.prey.getId()});
        if (lance.prey != null && lance.prey.isAlive()) KingEffects.stun(lance.prey, 20);
        lance.prey = null;
        if (lance.firedAt < 0L) {
            // Never let go: nothing to call back.
            state.lance = null;
            still(player, state);
        }
        play(player, center(player), HexKaguneSounds.KAGUNE_RESHAPE, 1.0F, 1.3F);
    }

    // ------------------------------------------------------------------ C: Rebirth Instinct

    private void bury(ServerPlayer player, State state, long now) {
        state.rebirthAt = now;
        move(player, state, KingMove.REBIRTH, 0);
        play(player, center(player), HexKaguneSounds.KAGUNE_RESHAPE, 1.6F, 0.7F);
        play(player, feet(player), HexKaguneSounds.KAGUNE_SLAM, 1.2F, 0.6F);
    }

    /** Held: the blades in the ground, the ground stirring before him more the longer; at the full hold they come of themselves. */
    private void burying(ServerPlayer player, State state, long now) {
        if (state.rebornAt >= 0L && now - state.rebornAt >= PeaceRules.REBIRTH_BACK) {
            state.rebornAt = -1L;
            if (state.move == KingMove.REBIRTH) still(player, state);
        }
        if (state.rebirthAt < 0L) return;
        long held = now - state.rebirthAt;
        if (held % 5 == 0) KingEffects.slow(player, player, 8, 3); // rooted to it, all but
        if (held % 12 == 0) {
            V3 at = feet(player).add(facing(player).scale(PeaceRules.THRALL_NEAR + 0.6));
            mark(player, KingMarkFx.QUAKE, at, V3.ZERO, 0.5 + 0.8 * Math.min(1.0, held / (double) PeaceRules.REBIRTH_CHARGE), null);
            vanilla(player, at, "entity.warden.heartbeat", 1.2F, 0.6F + 0.4F * (float) Math.min(1.0, held / (double) PeaceRules.REBIRTH_CHARGE));
        }
        if (held >= PeaceRules.REBIRTH_CHARGE) reborn(player, state, now);
    }

    /** Let go: two to four of them up out of the ground before him, each in its own place, apart, on open ground. */
    private void reborn(ServerPlayer player, State state, long now) {
        long held = now - state.rebirthAt;
        state.rebirthAt = -1L;
        state.rebornAt = now;
        move(player, state, KingMove.REBIRTH, 1);
        FleshThralls.dismiss(player);
        int count = PeaceRules.thralls(held);
        ServerLevel level = player.serverLevel();
        V3 feet = feet(player);
        double yaw = Math.toRadians(player.getYRot());
        List<V3> spots = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            V3 spot = null;
            for (int attempt = 0; attempt < 10 && spot == null; attempt++) {
                double a = yaw + (i - (count - 1) * 0.5) * 0.62 + (attempt == 0 ? 0.0 : (WarPath.noise(now + i, attempt) - 0.5) * 1.2);
                double r = PeaceRules.THRALL_NEAR + (PeaceRules.THRALL_FAR - PeaceRules.THRALL_NEAR) * ((i % 2) * 0.6 + 0.4 * WarPath.noise(now * 3 + i, attempt));
                double x = feet.x() - Math.sin(a) * r;
                double z = feet.z() + Math.cos(a) * r;
                double y = WarPath.ground(level, x, z, feet.y() + 1.0);
                if (Double.isNaN(y)) continue;
                V3 at = new V3(x, y, z);
                if (!WarPath.open(level, at)) continue;
                boolean apart = true;
                for (V3 other : spots) apart &= other.distance(at) >= PeaceRules.THRALL_APART * 1.25;
                if (apart) spot = at;
            }
            if (spot == null) continue;
            spots.add(spot);
            FleshThralls.summon(player, spot.x(), spot.y(), spot.z(), player.getYRot(), i, count);
            mark(player, KingMarkFx.QUAKE, spot, V3.ZERO, 1.0, null);
            mark(player, KingMarkFx.BLOOD, spot.add(0.0, 0.3, 0.0), new V3(0.0, 1.0, 0.0), 1.0, null);
        }
        play(player, feet, HexKaguneSounds.MANIFEST_RIP, 2.0F, 0.5F);
        vanilla(player, feet, "entity.warden.emerge", 1.0F, 1.2F);
    }

    // ------------------------------------------------------------------ V: The Final Horizon

    private void horizon(ServerPlayer player, State state, LivingEntity prey, long now) {
        Horizon h = new Horizon();
        h.prey = prey;
        h.at = now;
        h.origin = feet(prey);
        h.from = feet(player);
        V3 to = h.origin.sub(h.from).withY(0.0);
        h.away = to.length() < 1.0E-3 ? facing(player) : to.normalize();
        h.right = new V3(-h.away.z(), 0.0, h.away.x());
        h.body = h.origin;
        double floor = WarPath.ground(player.serverLevel(), h.origin.x(), h.origin.z(), h.origin.y() + 1.0);
        h.floor = Double.isNaN(floor) ? h.origin.y() : floor;
        // 1.0.36: held in the air by the blades, not falling between their holds (what made it shake).
        h.fell = !prey.isNoGravity();
        prey.setNoGravity(true);
        state.horizon = h;
        move(player, state, KingMove.HORIZON, 0);
        play(player, center(player), HexKaguneSounds.MANIFEST_RIP, 2.0F, 1.2F);
        vanilla(player, center(player), "block.beacon.activate", 1.4F, 0.7F);
    }

    /**
     * The Final Horizon, tick by tick: on it in a blink, the great blade's uppercut throwing it up through whatever is over it, him up
     * after it and under it; the other five into it one at a time from all round, left in it; the great one's blow from over it sending
     * it down; on the ground, the blades in it drive it on down through the ground, layer after layer, before they draw out of it. Its
     * wounds never kill it on the way: what would have is paid as they let it go.
     */
    private void horizoning(ServerPlayer player, State state, long now) {
        Horizon h = state.horizon;
        if (h == null) return;
        long age = now - h.at;
        LivingEntity prey = h.prey;
        ServerLevel level = player.serverLevel();
        TerrainScars scars = TerrainScars.of(level);
        if ((!prey.isAlive() || prey.isRemoved() || prey.level() != level) && h.releasedAt < 0L) {
            release(player, state, h, now);
        }
        double w = prey.getBbWidth() * 0.5;
        V3 front = h.origin.sub(h.away.scale(1.2 + w));
        // The body.
        if (h.releasedAt < 0L) {
            if (age < PeaceRules.HORIZON_UPPER) {
                hold(prey, h.origin);
                KingEffects.stun(prey, 10);
            } else if (!h.falling && age < PeaceRules.HORIZON_STRIKE) {
                double up = PeaceRules.horizonRise(age);
                h.body = h.origin.add(0.0, up, 0.0).add(h.away.scale(PeaceRules.horizonDrift(age)));
                if (age <= PeaceRules.HORIZON_APEX + 1) {
                    scars.carveSphere(player, h.body.add(0.0, prey.getBbHeight() + 0.9, 0.0), 1.6, h.origin.y() + 0.6);
                    scars.carveSphere(player, h.body.add(0.0, prey.getBbHeight() * 0.5, 0.0), 1.2, h.origin.y() + 0.6);
                }
                hold(prey, h.body);
                KingEffects.stun(prey, 10);
            } else if (h.landedAt < 0L) {
                // 1.0.36: sent down along a long line away from him (as War's Black Sun), through whatever is in the air on its way (the
                // ground under it left alone), till it slams into the ground.
                if (!h.falling) {
                    h.falling = true;
                    h.fellAt = now;
                }
                V3 fall = horizonFall(h);
                V3 next = h.body.add(fall.scale(PeaceRules.HORIZON_DOWN));
                double floor = WarPath.ground(level, next.x(), next.z(), Math.max(next.y(), h.body.y()) + 1.0);
                if (Double.isNaN(floor) && WarPath.solid(level, next.x(), next.y() + 0.5, next.z())) {
                    h.drilled++; // into the side of something: through it if it is thin, else it has hit the ground
                } else {
                    h.drilled = 0;
                }
                boolean down = !Double.isNaN(floor) && next.y() <= floor + 0.05;
                if (down || h.drilled > WarPath.DRILL || now - h.fellAt > PeaceRules.HORIZON_FALL_MOST) {
                    h.body = down ? next.withY(floor) : h.body;
                    h.landedAt = now;
                    hold(prey, h.body);
                    h.owed += bruise(player, prey, PeaceRules.HORIZON_GROUND);
                    fx(player, S2CKingFx.CRATER, new int[] {prey.getId()}, h.body, new V3(2.6, WarRules.SHAKE_RADIUS, 0.0));
                    scars.carveSphere(player, h.body.add(0.0, 1.0, 0.0), 1.6, h.body.y());
                    mark(player, KingMarkFx.QUAKE, h.body, V3.ZERO, 2.2, null);
                    play(player, h.body, HexKaguneSounds.KAGUNE_SLAM, 3.0F, 0.4F);
                    vanilla(player, h.body, "entity.generic.explode", 2.0F, 0.6F);
                } else {
                    double under = Double.isNaN(floor) ? next.y() - 2.0 : floor;
                    scars.carveSphere(player, next.add(0.0, 0.8, 0.0), 1.35, Math.max(under, next.y() - 0.2));
                    h.body = next;
                    hold(prey, h.body);
                    if (now % 2 == 0) mark(player, KingMarkFx.SOOT, center(prey), fall.scale(-1.0), 0.7, null);
                }
            } else if (!h.stopped) {
                // On the ground: the blades in it stretch on, driving it on away from him along a line down through the ground,
                // layer after layer, everything in its way broken (to grow back).
                long driving = now - h.landedAt;
                V3 tunnel = h.away.scale(Math.cos(PeaceRules.HORIZON_TUNNEL)).add(0.0, -Math.sin(PeaceRules.HORIZON_TUNNEL), 0.0);
                V3 next = h.body.add(tunnel.scale(PeaceRules.HORIZON_DRIVE_SPEED));
                scars.carveSphere(player, next.add(0.0, prey.getBbHeight() * 0.5, 0.0), Math.max(1.05, w + 0.55), next.y() - 0.3);
                boolean blocked = WarPath.solid(level, next.x(), next.y() + 0.1, next.z()) || WarPath.solid(level, next.x(), next.y() + prey.getBbHeight() * 0.6, next.z());
                if (blocked || h.driven >= PeaceRules.HORIZON_DEPTH || driving >= PeaceRules.HORIZON_DRIVE) {
                    h.stopped = true; // as far as it goes (or what is in its way would not break)
                } else {
                    h.body = next;
                    h.driven += PeaceRules.HORIZON_DRIVE_SPEED;
                }
                hold(prey, h.body);
                KingEffects.stun(prey, 10);
                if (driving % 4 == 0) {
                    h.owed += bruise(player, prey, PeaceRules.HORIZON_GRIND);
                    mark(player, KingMarkFx.QUAKE, h.body.add(0.0, 1.0, 0.0), V3.ZERO, 0.8, null);
                    play(player, h.body, HexKaguneSounds.BONE_CRACK, 1.4F, 0.5F + 0.1F * (driving % 3));
                }
                if (driving % 3 == 0) mark(player, KingMarkFx.SOOT, h.body.add(0.0, prey.getBbHeight() + 0.4, 0.0), new V3(0.0, 1.0, 0.0), 0.9, null);
            } else {
                release(player, state, h, now);
            }
        }
        // Him: there in a blink, then back a little and up off the ground as it goes up before him, held there. 1.0.36: carried along
        // his own planned path (where it has him next tick, and a little of how far off it he is), never chasing where the server last
        // saw him (what threw him back and forth). 1.2.2: and there he stays through the blow down and the drive, held in the air: the
        // blades stretch out from him to drive it down and on into the ground; he is never carried after it.
        if (age < PeaceRules.HORIZON_STRIKE) {
            if (age >= PeaceRules.HORIZON_UPPER + 2) player.setNoGravity(true);
            glide(player, horizonPlan(h, age, w), horizonPlan(h, age + 1.0, w));
        } else if (h.releasedAt < 0L) {
            player.setNoGravity(true);
            if (h.hover == null) h.hover = horizonPlan(h, PeaceRules.HORIZON_STRIKE - 1.0, w);
            glide(player, h.hover, h.hover);
        }
        // The blades.
        V3 c = center(prey);
        if (age == PeaceRules.HORIZON_UPPER) {
            fx(player, S2CKingFx.PEACE_IMPALE, new int[] {PeaceMotion.GREAT, prey.getId(), 2});
            h.owed += bruise(player, prey, PeaceRules.HORIZON_UPPER_DAMAGE);
            mark(player, KingMarkFx.FLARE, c, new V3(0.0, 1.0, 0.0), 1.4, prey);
            mark(player, KingMarkFx.BLOOD, c, new V3(0.0, 1.0, 0.0), 1.2, prey);
            play(player, c, HexKaguneSounds.FLESH_IMPACT, 1.8F, 0.7F);
            vanilla(player, c, "entity.player.attack.strong", 1.6F, 0.6F);
        } else if (age == PeaceRules.HORIZON_APEX) {
            fx(player, S2CKingFx.PEACE_TEAR, new int[] {prey.getId(), 1}); // the uppercut's blade drawn back out of it
        }
        for (int blade = 0; blade < PeaceMotion.BLADES; blade++) {
            long sent = PeaceRules.horizonSent(blade);
            if (sent < 0L) continue;
            boolean great = blade == PeaceMotion.GREAT;
            if (age == sent) {
                fx(player, S2CKingFx.PEACE_IMPALE, new int[] {blade, prey.getId(), great ? 3 : 1});
                play(player, center(player), HexKaguneSounds.KAGUNE_WHIP, great ? 1.8F : 1.1F, great ? 0.7F : 1.2F + 0.05F * blade);
            } else if (age == sent + PeaceRules.HORIZON_REACH) {
                h.owed += bruise(player, prey, great ? PeaceRules.HORIZON_STRIKE_DAMAGE : PeaceRules.HORIZON_STAB);
                mark(player, great ? KingMarkFx.FLARE : KingMarkFx.BLOOD, c, c.sub(center(player)).normalize(), great ? 1.8 : 1.0, prey);
                play(player, c, HexKaguneSounds.FLESH_IMPACT, great ? 2.2F : 1.2F, great ? 0.5F : 0.9F);
                if (great) vanilla(player, c, "entity.generic.explode", 1.4F, 1.2F);
            }
        }
        if (h.releasedAt >= 0L && now - h.releasedAt >= PeaceRules.HORIZON_LET_GO || age >= PeaceRules.HORIZON_TICKS + 40) {
            if (h.releasedAt < 0L) release(player, state, h, now);
            state.horizon = null;
            state.hangUntil = now + PeaceRules.HORIZON_HANG / 2;
            still(player, state);
        }
    }

    /** 1.0.36: the line the great blade's blow sends it down along: away from him and down. */
    private static V3 horizonFall(Horizon h) {
        return h.away.scale(Math.cos(PeaceRules.HORIZON_DIVE)).add(0.0, -Math.sin(PeaceRules.HORIZON_DIVE), 0.0);
    }

    /** 1.0.36: where the Horizon has him {@code t} ticks in, up to the great blade's blow: in at it, then up under it as it rises. */
    private static V3 horizonPlan(Horizon h, double t, double w) {
        V3 front = h.origin.sub(h.away.scale(1.2 + w));
        if (t <= PeaceRules.HORIZON_ARRIVE) {
            return h.from.lerp(front, Math.min(1.0, t / PeaceRules.HORIZON_ARRIVE));
        }
        if (t <= PeaceRules.HORIZON_UPPER + 2) {
            return front;
        }
        // 1.2.2: back from where he struck it and a little up off the ground, as far below and before it as it rises: never up under it
        // (it was lifted right over his head, as high again as it is now).
        double up = PeaceRules.horizonRise(Math.min(t, PeaceRules.HORIZON_STRIKE));
        V3 under = front.sub(h.away.scale(PeaceRules.HORIZON_BACK)).add(0.0, PeaceRules.HORIZON_LIFT * up / PeaceRules.HORIZON_UP, 0.0);
        if (t >= PeaceRules.HORIZON_UNDER) {
            return under;
        }
        double k = (t - PeaceRules.HORIZON_UPPER - 2.0) / (PeaceRules.HORIZON_UNDER - PeaceRules.HORIZON_UPPER - 2.0);
        return front.lerp(under, k * k * (3.0 - 2.0 * k));
    }

    /** 1.0.36: him carried from {@code was} to {@code to} over this tick: that step, and a third of how far off {@code was} he is. */
    private static void glide(ServerPlayer player, V3 was, V3 to) {
        V3 step = to.sub(was).add(was.sub(feet(player)).scale(0.33));
        KingCombos.motion(player, step.x(), step.y() + (player.isNoGravity() ? 0.0 : 0.08), step.z());
        player.fallDistance = 0.0F;
    }

    /** The Horizon's end: the blades drawn out of it, what its blows owed it paid; what it broke grows back sooner. */
    private void release(ServerPlayer player, State state, Horizon h, long now) {
        h.releasedAt = now;
        LivingEntity prey = h.prey;
        if (h.fell) prey.setNoGravity(false);
        fx(player, S2CKingFx.PEACE_TEAR, new int[] {prey.getId(), 1});
        if (prey.isAlive()) {
            V3 c = center(prey);
            hurt(player, prey, PeaceRules.HORIZON_TEAR + h.owed);
            mark(player, KingMarkFx.GORE, c, new V3(0.0, 1.0, 0.0), 1.6, prey);
            play(player, c, HexKaguneSounds.FLESH_IMPACT, 2.0F, 0.5F);
            if (prey.isAlive()) {
                KingEffects.stun(prey, 60);
                KingEffects.bleed(prey, player, 120, 1);
            }
        }
        h.owed = 0.0F;
        TerrainScars.of(player.serverLevel()).hastenAll(160L);
    }

    /** After the Horizon: hanging in the air a moment, then let go to fall on his own (nothing of it hurts him). */
    private static void hang(ServerPlayer player, State state, long now) {
        if (state.hangUntil == Long.MIN_VALUE) return;
        player.fallDistance = 0.0F;
        if (now < state.hangUntil) {
            player.setNoGravity(true);
            KingCombos.motion(player, 0.0, 0.0, 0.0);
            return;
        }
        player.setNoGravity(false);
        state.hangUntil = Long.MIN_VALUE;
    }

    // ------------------------------------------------------------------ the clock

    @Override
    public void tick(ServerPlayer player, long now) {
        State state = states.get(player.getUUID());
        if (state == null) return;
        striking(player, state, now);
        combos(player, state, now);
        waltzing(player, state, now);
        shielding(player, state, now);
        lifting(player, state, now);
        erupting(player, state, now);
        piercing(player, state, now);
        burying(player, state, now);
        horizoning(player, state, now);
        hang(player, state, now);
        if (state.move == KingMove.LANDING && now - state.moveAt >= state.move.ticks(state.level)) still(player, state);
        cutting(player, state, now);
    }

    @Override
    public float hurt(ServerPlayer player, Entity attacker, float amount) {
        return hurt(player, attacker, null, amount);
    }

    /** 1.0.34: under the Aegis, nothing reaches him: every harm is met on a shield (the one nearest where it came from). */
    @Override
    public float hurt(ServerPlayer player, Entity attacker, Entity direct, float amount) {
        State state = states.get(player.getUUID());
        if (state == null || state.aegisAt < 0L) return amount;
        long now = player.serverLevel().getGameTime();
        Entity source = direct != null ? direct : attacker;
        if (source != null) {
            V3 from = center(source);
            V3 c = center(player);
            V3 at = c.add(from.sub(c).length() < 1.0E-3 ? facing(player) : from.sub(c).normalize().scale(1.35));
            if (shield(player, state, shieldToward(player, from), PeaceRules.SHIELD_BLOCK, at, now, true)) {
                play(player, at, HexKaguneSounds.KAGUNE_GUARD, 1.2F, 1.2F);
            }
        }
        return 0.0F;
    }

    /** 1.0.34: under the Aegis, a shot is met on a shield and sent back at who shot it. */
    @Override
    public double[] deflect(ServerPlayer player, Projectile projectile) {
        State state = states.get(player.getUUID());
        if (state == null || state.aegisAt < 0L) return null;
        long now = player.serverLevel().getGameTime();
        V3 c = center(player);
        V3 from = new V3(projectile.getX(), projectile.getY(), projectile.getZ());
        V3 at = c.add(from.sub(c).length() < 1.0E-3 ? facing(player) : from.sub(c).normalize().scale(1.4));
        shield(player, state, shieldToward(player, from), PeaceRules.SHIELD_BLOCK, at, now, true);
        return new double[] {at.x(), at.y(), at.z()};
    }

    @Override
    public void off(ServerPlayer player) {
        State state = states.remove(player.getUUID());
        KingCombos.forget(player);
        FleshThralls.dismiss(player);
        if (state == null) return;
        if (state.aegisAt >= 0L) fx(player, S2CKingFx.PEACE_AEGIS, new int[] {0});
        if (state.lift != null) fx(player, S2CKingFx.PEACE_LIFT, new int[] {3});
        if (state.waltz != null) fx(player, S2CKingFx.PEACE_TEAR, new int[] {state.waltz.prey.getId(), 1});
        if (state.horizon != null) {
            fx(player, S2CKingFx.PEACE_TEAR, new int[] {state.horizon.prey.getId(), 1});
            if (state.horizon.fell) state.horizon.prey.setNoGravity(false);
        }
        if (state.lance != null && state.lance.backAt < 0L) fx(player, S2CKingFx.PEACE_LANCE, new int[] {4, 0});
        if (state.lift != null || state.horizon != null || state.hangUntil != Long.MIN_VALUE) player.setNoGravity(false);
    }

    // ------------------------------------------------------------------ helpers

    /** A point in his body pixels out in the world: his feet at {@code feet}, facing {@code yaw} (radians, as the game turns). */
    static V3 world(double[] px, V3 feet, double yaw) {
        double k = 1.0 / PeaceMotion.BLOCK;
        V3 left = new V3(Math.cos(yaw), 0.0, Math.sin(yaw));
        V3 forward = new V3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        return feet.add(left.scale(px[0] * k)).add(0.0, px[1] * k, 0.0).add(forward.scale(-px[2] * k));
    }

    private static boolean valid(ServerPlayer player, LivingEntity body) {
        return body != player && body.isAlive() && !body.isSpectator() && !(body instanceof ArmorStand) && !FleshThralls.ownedBy(body, player)
                && KaguneServerController.validSwipeTarget(player, body);
    }

    private static boolean hurt(ServerPlayer player, LivingEntity target, float amount) {
        if (!target.isAlive()) return false;
        target.invulnerableTime = 0;
        if (!target.hurt(player.damageSources().playerAttack(player), amount)) return false;
        if (!target.isAlive()) {
            V3 c = center(target);
            mark(player, KingMarkFx.GORE, c, c.sub(center(player)).withY(0.0).normalize().add(0.0, 0.5, 0.0), 1.2, null);
        }
        return true;
    }

    /** As much of {@code amount} as leaves it alive (one health at least); the rest owed, paid when the move lets it go. */
    private static float bruise(ServerPlayer player, LivingEntity prey, float amount) {
        float now = Math.min(amount, Math.max(0.0F, prey.getHealth() - 1.0F));
        if (now > 0.0F) hurt(player, prey, now);
        return amount - now;
    }

    /** The body held where the move has it (its own motion and its fall forgotten). */
    private static void hold(LivingEntity body, V3 at) {
        body.teleportTo(at.x(), at.y(), at.z());
        body.setDeltaMovement(new Vec3(0.0, 0.0, 0.0));
        body.fallDistance = 0.0F;
        body.hurtMarked = true;
    }


    private static BlockHitResult clip(ServerPlayer player, V3 from, V3 to) {
        return player.serverLevel().clip(new ClipContext(new Vec3(from.x(), from.y(), from.z()), new Vec3(to.x(), to.y(), to.z()), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
    }

    /** 1.0.19: a mark a blow leaves (KingMarkFx: blood, gore, quake, glint), {@code power} strong, on {@code body} if any. */
    private static void mark(ServerPlayer player, int style, V3 at, V3 way, double power, Entity body) {
        fx(player, S2CKingFx.MARK, new int[] {style, body == null ? 0 : body.getId()}, at, way, new V3(power, 0.0, 0.0));
    }

    private static void fx(ServerPlayer player, int kind, int[] ids, V3... points) {
        float[] data = new float[points.length * 3];
        for (int i = 0; i < points.length; i++) {
            data[i * 3] = (float) points[i].x();
            data[i * 3 + 1] = (float) points[i].y();
            data[i * 3 + 2] = (float) points[i].z();
        }
        ControlNetwork.broadcast(player, new S2CKingFx(player.getUUID(), kind, ids, data));
    }

    private static List<LivingEntity> near(ServerPlayer player, double radius) {
        return near(player, center(player), radius);
    }

    private static List<LivingEntity> near(ServerPlayer player, V3 at, double radius) {
        return player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                new AABB(at.x() - radius, at.y() - radius, at.z() - radius, at.x() + radius, at.y() + radius, at.z() + radius),
                body -> body != player && body.isAlive() && !body.isSpectator() && !(body instanceof ArmorStand));
    }

    private static V3 center(Entity entity) {
        return new V3(entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ());
    }

    private static V3 feet(Entity entity) {
        return new V3(entity.getX(), entity.getY(), entity.getZ());
    }

    private static V3 eye(ServerPlayer player) {
        Entity body = player;
        Vec3 eye = body.getEyePosition(1.0F);
        return new V3(eye.x, eye.y, eye.z);
    }

    private static V3 look(ServerPlayer player) {
        Entity body = player;
        Vec3 view = body.getViewVector(1.0F);
        return new V3(view.x, view.y, view.z).normalize();
    }

    private static V3 facing(ServerPlayer player) {
        double yaw = Math.toRadians(player.getYRot());
        return new V3(-Math.sin(yaw), 0.0, Math.cos(yaw));
    }

    private static void vanilla(ServerPlayer player, V3 at, String name, float volume, float pitch) {
        // 1.2.2: drawn from the user's own pack (Sfx): the name's pool, its pitch drawn back toward the sound's own.
        com.hex.kagune.control.Sfx.Sound picked = com.hex.kagune.control.Sfx.play(name, volume, pitch);
        SoundEvent sound = SoundEvent.createVariableRangeEvent(new ResourceLocation(picked.namespace(), picked.path()));
        player.serverLevel().playSound(null, at.x(), at.y(), at.z(), sound, SoundSource.PLAYERS, picked.volume(), picked.pitch());
    }

    private static void play(ServerPlayer player, V3 at, RegistryObject<?> sound, float volume, float pitch) {
        player.serverLevel().playSound(null, at.x(), at.y(), at.z(), (SoundEvent) sound.get(), SoundSource.PLAYERS, volume, pitch);
    }
}
