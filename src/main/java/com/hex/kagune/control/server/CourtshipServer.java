package com.hex.kagune.control.server;

import com.hex.kagune.control.npc.Courtship;
import com.hex.kagune.control.npc.EtoNpc;
import com.hex.kagune.control.npc.EtoRules;
import com.hex.kagune.control.npc.Persona;
import com.hex.kagune.control.quest.QuestLog;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * 2.1.2: a companion's courtship ({@link Courtship}) on the server, past what is handed over on the talk screen ({@code EtoServer}):
 * the chapters counted in time and place (a walk after dark with her along, sitting with him at his place, somewhere high, dawn together,
 * the dead of night at hers), checked every few seconds from her own mind; what the game says as they go; and, a chapter's task done
 * with her right there, her coming up to them with its moment.
 */
public final class CourtshipServer {
    private CourtshipServer() {
    }

    /** How often the time and place are checked (ticks): one step of {@link Courtship#STEP_SECONDS}. */
    static final long EVERY = Courtship.STEP_SECONDS * 20L;
    /** How far from her someone is looked at (blocks); how far a companion walking with them may be; how near "her place" is. */
    static final double AROUND = 24.0;
    static final double ALONG = 16.0;
    static final double OWN_PLACE = 28.0;

    /** Her tick, now and then: each player near her moved on through a chapter of hers counted in time and place. */
    static void tick(EtoNpc npc, ServerLevel level, long now) {
        if (now % EVERY != 11L || npc.mind == null || npc.mind.target != 0) {
            return;
        }
        String key = npc.persona().key();
        long time = Math.floorMod(level.m_46468_(), 24000L);
        for (ServerPlayer player : level.m_6907_()) {
            if (!player.m_6084_() || npc.m_20280_(player) > AROUND * AROUND) {
                continue;
            }
            QuestLog.Bond bond = EtoServer.courtBond(player, key);
            Courtship.Chapter chapter = Courtship.chapter(key, bond);
            if (chapter == null) {
                continue;
            }
            if (EtoRules.count(bond) < chapter.need() && counts(chapter.task(), npc, player, time)) {
                QuestLog.Bond after = Courtship.progress(key, bond, chapter.task(), 1);
                if (after != bond) {
                    QuestServer.bond(player, key, after);
                    tell(player, npc.persona(), after);
                    bond = after;
                }
            }
            if (Courtship.ready(key, bond)) {
                // Done, and she's right here: she comes up to them with its moment.
                SceneServer.approachMoment(npc, player, SceneServer.scene(key + "." + chapter.id()), now);
            }
        }
    }

    /** True if, this step, {@code task} is being done: {@code time} the hour of the day (ticks). */
    static boolean counts(Courtship.Task task, EtoNpc npc, ServerPlayer player, long time) {
        UUID following = npc.following();
        boolean along = player.m_20148_().equals(following) && npc.m_20280_(player) <= ALONG * ALONG;
        double near = Math.sqrt(npc.m_20280_(player));
        return switch (task) {
            case WALK_NIGHT -> along && EtoRules.night(time) && moving(player);
            case SIT -> following == null && near <= Courtship.SITTING && ownPlace(npc);
            case HIGH -> along && player.m_20186_() >= Courtship.HIGH && npc.m_20186_() >= Courtship.HIGH - 8;
            case DAWN -> near <= Courtship.WITH && (time >= 22300L || time < 900L);
            case DEEP -> near <= Courtship.WITH && time >= 16500L && time < 21500L && ownPlace(npc);
            // 2.1.5: through a human village after dark (its people about), her along.
            case VILLAGE_NIGHT -> along && EtoRules.night(time) && moving(player) && villagers(player);
            // 4.1.80: Ken's nights at the Library (her place, Eto's); Ayato up on the Doves' roof; the General's district walked by day.
            case READING -> along && EtoRules.night(time) && near(player, GangServer.home(player.m_284548_(), "eto"), Courtship.LIBRARY_NEAR);
            case ROOF -> along && onRoof(player) && npc.m_20186_() >= player.m_20186_() - 6.0;
            case VILLAGE_DAY -> along && Courtship.daylight(time) && moving(player) && villagers(player);
            default -> false;
        };
    }

    /** 2.1.5: true if a human village's people are about {@code player} (two at least, within {@link #VILLAGE_NEAR}). */
    static boolean villagers(ServerPlayer player) {
        return player.m_284548_().m_6443_(com.hex.kagune.control.npc.HumanNpc.class, player.m_20191_().m_82400_(VILLAGE_NEAR), human -> human.m_6084_())
            .size() >= 2;
    }

    /** 4.1.80: true if {@code player} is within {@code reach} of {@code at} (and on the same floor or so: 20 blocks up or down). */
    static boolean near(ServerPlayer player, BlockPos at, double reach) {
        if (at == null) {
            return false;
        }
        double dx = player.m_20185_() - (at.m_123341_() + 0.5);
        double dz = player.m_20189_() - (at.m_123343_() + 0.5);
        return dx * dx + dz * dz <= reach * reach && Math.abs(player.m_20186_() - at.m_123342_()) <= 20.0;
    }

    /** 4.1.80: true if {@code player} is up on the CCG tower's roof (Courtship.onRoof, from the world's height map round the General's spot). */
    static boolean onRoof(ServerPlayer player) {
        ServerLevel level = player.m_284548_();
        BlockPos home = GangServer.home(level, "general");
        if (home == null) {
            return false;
        }
        double dx = player.m_20185_() - (home.m_123341_() + 0.5);
        double dz = player.m_20189_() - (home.m_123343_() + 0.5);
        if (dx * dx + dz * dz > Courtship.ROOF_REACH * Courtship.ROOF_REACH) {
            return false;
        }
        int here = level.m_6924_(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int)Math.floor(player.m_20185_()),
            (int)Math.floor(player.m_20189_()));
        int tower = level.m_6924_(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, home.m_123341_(), home.m_123343_());
        return Courtship.onRoof(player.m_20186_(), here, tower, home.m_123342_());
    }

    /** 4.1.80: the companion {@code key} near {@code player} (within {@code reach}, alive, in their world), or null. */
    static EtoNpc nearby(ServerPlayer player, String key, double reach) {
        for (EtoNpc npc : EtoBrain.live()) {
            if (npc.persona().key().equals(key) && npc.m_6084_() && npc.m_9236_() == player.m_9236_() && npc.m_20280_(player) <= reach * reach) {
                return npc;
            }
        }
        return null;
    }

    /**
     * 4.1.80: one more done on {@code key}'s chapter on for {@code player} if it is a {@code task} not yet done: the bond saved, the
     * progress told; the new bond (or null if nothing counted).
     */
    static QuestLog.Bond count(ServerPlayer player, String key, Courtship.Task task) {
        QuestLog.Bond bond = EtoServer.courtBond(player, key);
        Courtship.Chapter chapter = Courtship.chapter(key, bond);
        if (chapter == null || chapter.task() != task || EtoRules.count(bond) >= chapter.need()) {
            return null;
        }
        QuestLog.Bond after = Courtship.progress(key, bond, task, 1);
        QuestServer.bond(player, key, after);
        tell(player, Persona.of(key), after);
        return after;
    }

    /** 4.1.80: true if {@code player}'s chapter on with {@code key} is a {@code task} not yet done. */
    static boolean on(ServerPlayer player, String key, Courtship.Task task) {
        QuestLog.Bond bond = EtoServer.courtBond(player, key);
        Courtship.Chapter chapter = Courtship.chapter(key, bond);
        return chapter != null && chapter.task() == task && EtoRules.count(bond) < chapter.need();
    }

    /** Last positions seen ("<player>"), so a walk only counts while they walk. */
    private static final java.util.Map<UUID, double[]> WHERE = new java.util.HashMap<>();

    /** 2.1.5: how near (blocks) humans must be for it to be their village. */
    static final double VILLAGE_NEAR = 32.0;

    /**
     * 2.1.5: something done that a courtship chapter may ask for ({@code task}: a wild ghoul made an ally, a village human brought to care
     * for them, a human's secret told): every companion's chapter of it moved on.
     */
    public static void event(ServerPlayer player, Courtship.Task task, int amount) {
        for (Persona persona : Persona.ALL) {
            QuestLog.Bond bond = EtoServer.courtBond(player, persona.key());
            Courtship.Chapter chapter = Courtship.chapter(persona.key(), bond);
            if (chapter == null || chapter.task() != task || EtoRules.count(bond) >= chapter.need()) {
                continue;
            }
            QuestLog.Bond after = Courtship.progress(persona.key(), bond, task, amount);
            if (after != bond) {
                QuestServer.bond(player, persona.key(), after);
                tell(player, persona, after);
            }
        }
    }

    /** True if {@code player} has gone a few blocks since the last step. */
    private static boolean moving(ServerPlayer player) {
        double[] now = {player.m_20185_(), player.m_20189_()};
        double[] was = WHERE.put(player.m_20148_(), now);
        if (WHERE.size() > 512) {
            WHERE.clear();
        }
        return was == null || Math.hypot(now[0] - was[0], now[1] - was[1]) >= 3.0;
    }

    /** True if {@code npc} is at her own place (her floor of the Library, the cafe, his room). */
    static boolean ownPlace(EtoNpc npc) {
        BlockPos origin = npc.origin();
        return origin != null && npc.m_20238_(new net.minecraft.world.phys.Vec3(origin.m_123341_() + 0.5, origin.m_123342_(),
            origin.m_123343_() + 0.5)) < OWN_PLACE * OWN_PLACE;
    }

    /** True if the companion {@code key} is walking with {@code player} and near them now. */
    static boolean along(ServerPlayer player, String key) {
        for (EtoNpc npc : EtoBrain.live()) {
            if (npc.persona().key().equals(key) && npc.m_6084_() && player.m_20148_().equals(npc.following()) && npc.m_9236_() == player.m_9236_()
                && npc.m_20280_(player) <= ALONG * ALONG) {
                return true;
            }
        }
        return false;
    }

    /** How the chapter on with {@code persona} stands, on {@code player}'s screen (and that it's done: go and see them). */
    static void tell(ServerPlayer player, Persona persona, QuestLog.Bond bond) {
        Courtship.Chapter chapter = Courtship.chapter(persona.key(), bond);
        if (chapter == null) {
            return;
        }
        int done = EtoRules.count(bond);
        String counted = chapter.task().timed() ? Courtship.fill("{m} of {mm} minutes", chapter, done) : done + " of " + chapter.need();
        player.m_5661_(Component.m_237113_(persona.colour() + "❤ " + chapter.title() + ": " + (done >= chapter.need()
            ? "done. Go and see " + persona.her() + "." : counted)), true);
    }
}
