package com.hex.kagune.control.server;

import com.hex.kagune.control.ClashRules;
import com.hex.kagune.control.Safety;
import com.hex.kagune.control.npc.EtoNpc;
import com.hex.kagune.control.pale.MoveClashRules;
import com.hex.kagune.control.pale.PaleMove;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 1.3.63: the same big move meeting itself ({@link MoveClashRules}; the owner: "if both use the same move like big punch, speed kick or
 * any of the sort it should cancel both out and cause a big impact"). Pale's kit's big moves (a player's on the Pale path, Eto's, who
 * fights with the same kit) say here when they are let go ({@link #begun}); each tick, one still on its way that finds what it goes for,
 * within reach and facing it, doing the same kind of move back (let go, or still winding up), cancels both: a big clash between them
 * ({@link KaguneClash#collide}: the owner's clash sound, the wind, the ground shaking), both thrown apart and stunned a moment.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
final class MoveClash {
    /** A big move on its way: whose, which, its family, when it was let go, the last tick before it lands, what it goes for, how hard. */
    private record Big(LivingEntity who, PaleMove move, MoveClashRules.Family family, long began, long until, int target, double hit,
                       BooleanSupplier live, Runnable stop) {
    }

    private static final Map<UUID, Big> BIG = new HashMap<>();
    /** How hard a big move is when nothing better is known (a hard blow). */
    static final double HIT = 20.0;

    private MoveClash() {
    }

    /** {@code who}'s {@code move} (with {@code detail}) set out {@code now} at {@code target} (0: none), {@code hit} hard; nothing for another move. */
    static void begun(LivingEntity who, PaleMove move, int detail, int target, long now, double hit, BooleanSupplier live, Runnable stop) {
        if (!MoveClashRules.released(move)) {
            BIG.remove(who.m_20148_());
            return;
        }
        BIG.put(who.m_20148_(), new Big(who, move, MoveClashRules.family(move), now, now + MoveClashRules.window(move, detail), target, hit, live, stop));
    }

    /** Each tick: every big move still on its way that meets the same coming back. */
    @SubscribeEvent
    public static void levelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level) || BIG.isEmpty()) {
            return;
        }
        long now = level.m_46467_();
        for (Big big : new ArrayList<>(BIG.values())) {
            LivingEntity who = big.who();
            if (who.m_9236_() != level) {
                continue;
            }
            if (now > big.until() || who.m_213877_() || !who.m_6084_() || !alive(big)) {
                BIG.remove(who.m_20148_(), big);
                continue;
            }
            try {
                meet(big, level, now);
            } catch (RuntimeException failure) {
                Safety.caught(failure, "a big move meeting another");
            }
        }
    }

    /** {@code big} meets what it goes for, if that does the same kind of move back at it. */
    private static void meet(Big big, ServerLevel level, long now) {
        LivingEntity who = big.who();
        double reach = MoveClashRules.reach(big.family());
        for (LivingEntity other : level.m_6443_(LivingEntity.class, who.m_20191_().m_82400_(reach), body -> body != who && body.m_6084_())) {
            PaleMove theirs = moveOf(other);
            if (MoveClashRules.family(theirs) != big.family() || other.m_20280_(who) > reach * reach) {
                continue;
            }
            if (!at(who, big.target(), other) || !at(other, targetOf(other), who)) {
                continue; // not at each other
            }
            Big their = BIG.get(other.m_20148_());
            BIG.remove(who.m_20148_());
            BIG.remove(other.m_20148_());
            stop(big.stop());
            if (their != null && their.who() == other) {
                stop(their.stop());
            } else {
                cut(other);
            }
            float power = MoveClashRules.power(big.hit(), their != null ? their.hit() : big.hit());
            KaguneClash.collide(who, other, power);
            PaleStun.stun(who, now, MoveClashRules.STAGGER);
            PaleStun.stun(other, now, MoveClashRules.STAGGER);
            return;
        }
    }

    /** True if {@code body} goes for {@code other}: at it by name, or (at nothing in particular) facing it. */
    private static boolean at(LivingEntity body, int target, LivingEntity other) {
        if (target != 0) {
            return target == other.m_19879_();
        }
        double yaw = Math.toRadians(body.f_20883_);
        Vec3 look = body instanceof ServerPlayer ? body.m_20154_() : new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        return ClashRules.faces(look.f_82479_, look.f_82481_, other.m_20185_() - body.m_20185_(), other.m_20189_() - body.m_20189_());
    }

    /** The Pale move {@code body} is doing now (a player on the Pale path, Eto's kit), or NONE. */
    private static PaleMove moveOf(LivingEntity body) {
        if (body instanceof ServerPlayer player) {
            PaleServer.PaleState state = PaleServer.state(player);
            return state == null ? PaleMove.NONE : state.move;
        }
        if (body instanceof EtoNpc npc && npc.mind != null) {
            return npc.mind.owl.move;
        }
        return PaleMove.NONE;
    }

    /** What {@code body}'s move goes for (an entity id; 0: none). */
    private static int targetOf(LivingEntity body) {
        if (body instanceof ServerPlayer player) {
            PaleServer.PaleState state = PaleServer.state(player);
            return state == null ? 0 : state.targetId;
        }
        return body instanceof EtoNpc npc && npc.mind != null ? npc.mind.owl.target : 0;
    }

    /** {@code body}'s move cut off (one winding up, not registered here). */
    private static void cut(LivingEntity body) {
        if (body instanceof ServerPlayer player) {
            PaleServer.PaleState state = PaleServer.state(player);
            if (state != null) {
                PaleServer.end(player, state);
            }
        } else if (body instanceof EtoNpc npc && npc.mind != null) {
            EtoOwl.end(npc.mind.owl);
        }
    }

    private static boolean alive(Big big) {
        try {
            return big.live().getAsBoolean();
        } catch (RuntimeException failure) {
            Safety.caught(failure, "a big move asking after itself");
            return false;
        }
    }

    private static void stop(Runnable stop) {
        try {
            stop.run();
        } catch (RuntimeException failure) {
            Safety.caught(failure, "a big move cut off");
        }
    }
}
