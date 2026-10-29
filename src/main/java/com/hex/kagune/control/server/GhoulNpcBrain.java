package com.hex.kagune.control.server;

import com.hex.kagune.control.ControlItems;
import com.hex.kagune.control.graft.GraftMove;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CGhoulNpc;
import com.hex.kagune.control.npc.GhoulNpc;
import com.hex.kagune.control.npc.NpcBonds;
import com.hex.kagune.control.npc.NpcIdentity;
import com.hex.kagune.control.npc.NpcRules;
import com.hex.kagune.control.npc.NpcTemper;
import com.hex.kagune.control.quest.QuestBook;
import com.hex.kagune.control.quest.QuestLog;
import com.hex.kagune.control.quest.Signal;
import com.hex.kagune.control.quest.Tags;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.sound.HexKaguneSounds;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 7.0.2: what a ghoul NPC does, decided on the server every tick ({@link GhoulNpc}'s own AI step): who it goes for (its temper's
 * {@link NpcTemper#toward attitude} to each player, the NPCs its temper hunts, anyone who hurt it while its grudge lasts), when it
 * runs (a coward losing, or a ghoul too close), and the rest of the time it wanders and watches. Its kagune comes out when it fights
 * (the kakugan with it) and goes back a while after; the fighting itself is {@link GhoulNpcCombat}'s. Ghouls heal fast: unhurt for
 * a moment, it knits back up. At dawn, unwatched, it slips away. Every client tracking it is told what it needs to draw it.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class GhoulNpcBrain {
    /** One NPC's state on the server. */
    public static final class Mind {
        /** What it goes for now (an entity id, 0 none) and the game tick it last looked for something. */
        int prey;
        long preySought = Long.MIN_VALUE;
        /** Who hurt it, and until when it holds that against them. */
        final Map<UUID, Long> grudges = new HashMap<>();
        long hurtAt = Long.MIN_VALUE;
        /** The kagune: out or not, since when, and when it last fought. */
        boolean out;
        long outAt = Long.MIN_VALUE;
        long foughtAt = Long.MIN_VALUE;
        /** Rinkaku: the lash it is making, when it began, where it was aimed; the last one and when it ended; lashes so far. */
        GraftMove move = GraftMove.NONE;
        long moveAt = Long.MIN_VALUE;
        Vec3 point = Vec3.f_82478_;
        /** The way the lash faces (a Minecraft yaw, degrees): at its prey as it began. */
        double yaw;
        GraftMove last;
        long lastEnds = Long.MIN_VALUE;
        int strung;
        final Map<Integer, Set<Integer>> hits = new HashMap<>();
        /** Not before this tick does it start another string or volley. */
        long readyAt = Long.MIN_VALUE;
        /** Ukaku: the volley (when it began aiming, 0 shots so far), the next shot, the wing it comes from, the last shot. */
        long volleyAt = Long.MIN_VALUE;
        int shots;
        long nextShot = Long.MIN_VALUE;
        int side = 1;
        long shotAt = Long.MIN_VALUE;
        int shotSide = 1;
        final List<GhoulNpcCombat.Bolt> bolts = new ArrayList<>();
        /** Walking: when it next picks somewhere to wander to; where it circles its prey from (and until when). */
        long wanderAt = Long.MIN_VALUE;
        double circle = 1.0;
        long circleAt = Long.MIN_VALUE;
        boolean fleeing;
        /** Something every client should be told. */
        boolean dirty = true;
        /** 7.0.2: the player it is talking to (an entity id, 0 none) and until when it waits on them. */
        int talking;
        long talkUntil = Long.MIN_VALUE;
        /** 7.0.2: the player it walks with (null: none), whether it follows them or waits, where it waits. */
        UUID owner;
        boolean follow = true;
        Vec3 waitAt;
        /** 7.0.2: sent off (it walks away and is gone), or scared off (it runs a while). */
        long leavingAt = Long.MIN_VALUE;
        long scaredUntil = Long.MIN_VALUE;
        /** 1.0.41: its flip (an PaleRules.Flip, -1 none), when it took off, its push (renewed the tick after), when it may flip again. */
        int flip = -1;
        long flipAt = Long.MIN_VALUE;
        Vec3 flipPush;
        long flipReadyAt = Long.MIN_VALUE;
        /** 1.0.41: when its heavy is ready again, and the blow coming at it it last answered (when that set out). */
        long heavyReadyAt = Long.MIN_VALUE;
        long answered = Long.MIN_VALUE;
    }

    /** Where an ally's player and whether it waits are kept in its saved data. */
    static final String ALLY = "HexGhoulAlly";
    static final String WAITS = "HexGhoulWaits";

    private GhoulNpcBrain() {
    }

    // ---------------------------------------------------------------- the tick

    public static void tick(GhoulNpc npc) {
        if (!(npc.m_9236_() instanceof ServerLevel level)) {
            return;
        }
        NpcIdentity who = npc.identity();
        if (who == null) {
            // Summoned or hatched, not spawned at night: it becomes someone now.
            who = NpcIdentity.roll(0, () -> npc.m_217043_().m_188500_());
            npc.identify(who);
            dress(npc);
        }
        Mind mind = npc.mind;
        if (mind == null) {
            mind = new Mind();
            CompoundTag saved = npc.getPersistentData();
            if (saved.m_128403_(ALLY)) {
                mind.owner = saved.m_128342_(ALLY);
                mind.follow = !saved.m_128471_(WAITS);
                mind.waitAt = npc.m_20182_();
            }
            npc.mind = mind;
        }
        long now = level.m_46467_();
        if (mind.leavingAt != Long.MIN_VALUE) {
            leave(npc, mind, level, now);
            return;
        }
        regen(npc, mind, now);
        if (dawn(npc, level, now)) {
            return;
        }
        GhoulNpcCombat.fly(npc, who, mind, level, now);
        if (PaleStun.stunned(npc)) {
            sync(npc, mind, who);
            return; // reeling: it does nothing until it shakes it off
        }
        LivingEntity prey = prey(npc, who, mind, level, now);
        if (now < mind.scaredUntil || who.temper().coward() && afraid(npc, level, prey)) {
            flee(npc, mind, prey, level, now);
        } else if (prey == null && talking(npc, mind, level, now)) {
            mind.fleeing = false;
            calm(npc, mind, now, false); // it stands and talks
        } else if (prey != null) {
            mind.fleeing = false;
            fight(npc, who, mind, prey, level, now);
        } else {
            mind.fleeing = false;
            idle(npc, who, mind, level, now);
        }
        sync(npc, mind, who);
    }

    /** Puts its tier on it (health, armor, speed), heals it whole and names it. */
    public static void dress(GhoulNpc npc) {
        NpcIdentity who = npc.identity();
        if (who == null) {
            return;
        }
        npc.m_21051_(Attributes.f_22276_).m_22100_(NpcRules.health(who.tier()));
        npc.m_21051_(Attributes.f_22284_).m_22100_(NpcRules.armor(who.tier()));
        npc.m_21051_(Attributes.f_22279_).m_22100_(NpcRules.speed(who.tier()));
        npc.m_21153_(npc.m_21233_());
        npc.m_6593_(Component.m_237113_(who.displayName()));
    }

    /** What it leaves when it dies: Ghoul Meat. */
    public static void drop(GhoulNpc npc) {
        NpcIdentity who = npc.identity();
        int pieces = NpcRules.meat(who == null ? 0 : who.tier(), npc.m_217043_().m_188500_());
        npc.m_19983_(new ItemStack(ControlItems.GHOUL_MEAT.get(), pieces));
        if (npc.mind != null) {
            GhoulNpcCombat.fizzleAll(npc, npc.mind);
            ServerPlayer owner = owner(npc, npc.mind);
            if (owner != null) {
                String key = npc.m_20148_().toString();
                QuestServer.bond(owner, key, QuestServer.bond(owner, key).with(NpcBonds.ALLY, false));
                owner.m_5661_(Component.m_237113_((who == null ? "Your ally" : who.displayName()) + " is dead."), false);
            }
        }
    }

    // ---------------------------------------------------------------- what it goes for

    /** What it goes for: the one it is after while that still holds, else the nearest it sees that it wants (looked for every half second). */
    private static LivingEntity prey(GhoulNpc npc, NpcIdentity who, Mind mind, ServerLevel level, long now) {
        LivingEntity current = mind.prey != 0 && level.m_6815_(mind.prey) instanceof LivingEntity living ? living : null;
        if (current != null && (!current.m_6084_() || current.m_20280_(npc) > NpcRules.LEASH * NpcRules.LEASH || !wanted(npc, who, mind, current, now))) {
            current = null;
        }
        // 1.0.15: "never looked" is Long.MIN_VALUE, and now minus that overflows negative: it never looked round at all (wild ghouls
        // never fought back). Never-looked looks at once.
        if (current == null && (mind.preySought == Long.MIN_VALUE || now - mind.preySought >= 10L)) {
            mind.preySought = now;
            double best = Double.MAX_VALUE;
            ServerPlayer owner = owner(npc, mind);
            if (owner != null && owner.m_21188_() != null && owner.m_21188_() != npc && owner.m_21188_().m_6084_()
                && owner.m_21188_().m_20280_(npc) <= NpcRules.LEASH * NpcRules.LEASH && !(owner.m_21188_() instanceof GhoulNpc kin && owner.equals(owner(kin, kin.mind)))) {
                current = owner.m_21188_(); // whatever just hurt its player
                best = 0.0;
            }
            for (LivingEntity body : level.m_6443_(LivingEntity.class, npc.m_20191_().m_82400_(NpcRules.SIGHT),
                living -> living != npc && living.m_6084_())) {
                double distance = body.m_20280_(npc);
                if (distance < best && distance <= NpcRules.SIGHT * NpcRules.SIGHT && wanted(npc, who, mind, body, now) && npc.m_142582_(body)) {
                    best = distance;
                    current = body;
                }
            }
        }
        int id = current == null ? 0 : current.m_19879_();
        if (id != mind.prey) {
            mind.prey = id;
            npc.m_6710_(current);
            mind.dirty = true;
        }
        return current;
    }

    /** True if it goes for {@code body}: a grudge, its temper's attitude to a player, or an NPC its temper hunts. */
    static boolean wanted(GhoulNpc npc, NpcIdentity who, Mind mind, LivingEntity body, long now) {
        if (body == npc || !body.m_6084_()) {
            return false;
        }
        if (body instanceof Player player && (player.m_7500_() || player.m_5833_())) {
            return false;
        }
        Long until = mind.grudges.get(body.m_20148_());
        if (until != null && until > now) {
            return true;
        }
        ServerPlayer owner = owner(npc, mind);
        if (owner != null) {
            // An ally follows its player's lead: what hurts them, what hunts them, the monsters round them; never them or theirs.
            if (body == owner || body instanceof GhoulNpc kin && owner.equals(owner(kin, kin.mind))) {
                return false;
            }
            // (Not a creeper: it would blow up beside them.)
            return body == owner.m_21188_() || body instanceof Mob mob && mob.m_5448_() == owner && !(body instanceof Creeper)
                || body instanceof Enemy && !(body instanceof Creeper) && body.m_20280_(owner) <= 100.0;
        }
        if (body instanceof ServerPlayer player) {
            return attitude(npc, who, player) == NpcTemper.Attitude.HOSTILE;
        }
        return body instanceof GhoulNpc other && other.identity() != null && who.temper().hunts(other.identity().temper());
    }

    /** How it takes to {@code player}: its temper's way with what they are and the side they took, over how it regards them. */
    static NpcTemper.Attitude attitude(GhoulNpc npc, NpcIdentity who, ServerPlayer player) {
        NpcTemper.Attitude base = who.temper().toward(KaguneProfile.isUnlocked(player), side(player));
        return NpcBonds.attitude(base, QuestServer.bond(player, npc.m_20148_().toString()).regard());
    }

    /** The side {@code player} took in the quests ("anteiku", "pack"), or null. */
    static String side(ServerPlayer player) {
        QuestLog log = QuestServer.log(player);
        int anteiku = log.side(QuestBook.ANTEIKU);
        int pack = log.side(QuestBook.PACK);
        return anteiku > pack ? QuestBook.ANTEIKU : pack > anteiku ? QuestBook.PACK : null;
    }

    /** True for what its blows and shots may land on: whatever it goes for, and anyone it is not friendly with that gets in the way. */
    static boolean fair(GhoulNpc npc, NpcIdentity who, Mind mind, LivingEntity body, long now) {
        if (body == npc || !body.m_6084_() || body.m_5833_()) {
            return false;
        }
        if (wanted(npc, who, mind, body, now)) {
            return true;
        }
        if (body instanceof ServerPlayer player) {
            return !player.m_7500_() && !player.equals(owner(npc, mind)) && attitude(npc, who, player) != NpcTemper.Attitude.FRIENDLY;
        }
        return !(body instanceof GhoulNpc);
    }

    /** A coward is afraid when it is losing, or when a ghoul is close (and it has not been cornered into fighting). */
    private static boolean afraid(GhoulNpc npc, ServerLevel level, LivingEntity prey) {
        if (npc.m_21223_() < npc.m_21233_() * NpcRules.FLEE_BELOW) {
            return true;
        }
        if (prey != null) {
            return false; // cornered: it fights back
        }
        for (ServerPlayer player : level.m_6907_()) {
            if (!player.m_5833_() && player.m_20280_(npc) <= NpcRules.SHY * NpcRules.SHY && KaguneProfile.isUnlocked(player)) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- moving

    private static void fight(GhoulNpc npc, NpcIdentity who, Mind mind, LivingEntity prey, ServerLevel level, long now) {
        mind.foughtAt = now;
        if (!mind.out) {
            mind.out = true;
            mind.outAt = now;
            mind.dirty = true;
            level.m_6263_(null, npc.m_20185_(), npc.m_20186_() + 1.0, npc.m_20189_(), HexKaguneSounds.KAGUNE_WHIP.get(), SoundSource.HOSTILE, 0.9F,
                0.8F + 0.2F * (float)npc.m_217043_().m_188500_());
        }
        npc.m_21563_().m_24960_(prey, 30.0F, 30.0F);
        if (now - mind.outAt < NpcRules.DEPLOY_TICKS) {
            npc.m_21573_().m_26573_(); // the kagune is still tearing out
            if (mind.heavyReadyAt == Long.MIN_VALUE) {
                mind.heavyReadyAt = now + 60L; // (1.0.41: not its heavy the moment it is out)
            }
            return;
        }
        // 1.0.41: its feet first: a blow coming at it answered, a flip in or out.
        if (GhoulNpcCombat.maneuver(npc, who, mind, prey, level, now)) {
            return;
        }
        if (who.kind() == NpcIdentity.Kind.RINKAKU) {
            GhoulNpcCombat.lash(npc, who, mind, prey, level, now);
        } else {
            GhoulNpcCombat.shoot(npc, who, mind, prey, level, now);
        }
    }

    private static void flee(GhoulNpc npc, Mind mind, LivingEntity from, ServerLevel level, long now) {
        Entity threat = from;
        if (threat == null) {
            double best = Double.MAX_VALUE;
            for (ServerPlayer player : level.m_6907_()) {
                double distance = player.m_20280_(npc);
                if (distance < best) {
                    best = distance;
                    threat = player;
                }
            }
        }
        calm(npc, mind, now, true);
        mind.fleeing = true;
        if (threat == null || now % 10L != 0L && !npc.m_21573_().m_26571_()) {
            return;
        }
        Vec3 away = GhoulNpcCombat.flat(npc.m_20182_().m_82546_(threat.m_20182_()));
        Vec3 to = npc.m_20182_().m_82549_(away.m_82490_(14.0));
        int y = level.m_6924_(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int)Math.floor(to.f_82479_), (int)Math.floor(to.f_82481_));
        npc.m_21573_().m_26519_(to.f_82479_, y, to.f_82481_, 1.3);
    }

    private static void idle(GhoulNpc npc, NpcIdentity who, Mind mind, ServerLevel level, long now) {
        calm(npc, mind, now, false);
        ServerPlayer owner = owner(npc, mind);
        if (owner != null) {
            accompany(npc, mind, owner, level, now);
            return;
        }
        if (mind.owner != null) {
            // Its player is away (gone, another world): it waits where it is.
            npc.m_21573_().m_26573_();
            return;
        }
        if (who.temper() == NpcTemper.ANTEIKU || who.temper() == NpcTemper.DRIFTER) {
            // A quiet one stops for someone who comes near.
            for (ServerPlayer player : level.m_6907_()) {
                if (!player.m_5833_() && player.m_20280_(npc) <= 36.0) {
                    npc.m_21573_().m_26573_();
                    return;
                }
            }
        }
        if (now < mind.wanderAt || !npc.m_21573_().m_26571_()) {
            return;
        }
        mind.wanderAt = now + 80L + (long)(npc.m_217043_().m_188500_() * 160.0);
        double angle = npc.m_217043_().m_188500_() * Math.PI * 2.0;
        double distance = 5.0 + npc.m_217043_().m_188500_() * 9.0;
        double x = npc.m_20185_() + Math.cos(angle) * distance;
        double z = npc.m_20189_() + Math.sin(angle) * distance;
        int y = level.m_6924_(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int)Math.floor(x), (int)Math.floor(z));
        if (Math.abs(y - npc.m_20186_()) <= 6.0) {
            npc.m_21573_().m_26519_(x, y, z, 0.7);
        }
    }

    /** No fight: whatever it was making ends; a while after its last fight (or at once when it runs) the kagune goes back in. */
    private static void calm(GhoulNpc npc, Mind mind, long now, boolean hide) {
        if (mind.move != GraftMove.NONE) {
            mind.move = GraftMove.NONE;
            mind.dirty = true;
        }
        mind.strung = 0;
        mind.shots = 0;
        if (mind.volleyAt != Long.MIN_VALUE) {
            mind.volleyAt = Long.MIN_VALUE;
            mind.dirty = true;
        }
        if (mind.out && (hide || now - mind.foughtAt > NpcRules.CALM_TICKS)) {
            mind.out = false;
            mind.outAt = now;
            mind.dirty = true;
        }
    }

    // ---------------------------------------------------------------- talking and walking with a player (7.0.2)

    /** True if it talks to {@code player} now: its temper would, it regards them well enough, and it is not after them. */
    static boolean talks(GhoulNpc npc, NpcIdentity who, Mind mind, ServerPlayer player, long now) {
        if (mind.prey == player.m_19879_() || mind.leavingAt != Long.MIN_VALUE || PaleStun.stunned(npc)) {
            return false;
        }
        Long until = mind.grudges.get(player.m_20148_());
        if (until != null && until > now) {
            return false;
        }
        return player.equals(owner(npc, mind)) || com.hex.kagune.control.npc.NpcTalk.talks(who.temper(), attitude(npc, who, player));
    }

    /** It turns to {@code player} and waits on them a while (null: the talk is over). */
    static void listen(GhoulNpc npc, Mind mind, ServerPlayer player, long now) {
        mind.talking = player == null ? 0 : player.m_19879_();
        mind.talkUntil = player == null ? Long.MIN_VALUE : now + 600L;
    }

    /** True while it stands talking: it faces whoever it talks to (until they walk off or the talk runs long). */
    private static boolean talking(GhoulNpc npc, Mind mind, ServerLevel level, long now) {
        if (mind.talking == 0) {
            return false;
        }
        Entity talker = level.m_6815_(mind.talking);
        if (talker == null || now > mind.talkUntil || talker.m_20280_(npc) > 100.0) {
            mind.talking = 0;
            return false;
        }
        npc.m_21573_().m_26573_();
        npc.m_21563_().m_24960_(talker, 30.0F, 30.0F);
        return true;
    }

    /** The player it walks with, if they are here (online, in its world). */
    static ServerPlayer owner(GhoulNpc npc, Mind mind) {
        if (mind == null || mind.owner == null || !(npc.m_9236_() instanceof ServerLevel level)) {
            return null;
        }
        for (ServerPlayer player : level.m_6907_()) {
            if (player.m_20148_().equals(mind.owner)) {
                return player.m_6084_() ? player : null;
            }
        }
        return null;
    }

    /** True if it walks with {@code player}. */
    static boolean allyOf(GhoulNpc npc, ServerPlayer player) {
        return npc.mind != null && player.m_20148_().equals(npc.mind.owner);
    }

    /** True if it follows its player (false: it waits where it was told). */
    static boolean following(GhoulNpc npc) {
        return npc.mind == null || npc.mind.follow;
    }

    /** It walks with {@code player} from now: kept (it never slips away), following them. */
    static void bind(GhoulNpc npc, ServerPlayer player) {
        Mind mind = npc.mind;
        if (mind == null) {
            return;
        }
        mind.owner = player.m_20148_();
        mind.follow = true;
        mind.grudges.remove(player.m_20148_());
        npc.getPersistentData().m_128362_(ALLY, player.m_20148_());
        npc.getPersistentData().m_128379_(WAITS, false);
        npc.m_21530_();
    }

    /** Sent off: it no longer walks with anyone, and goes its own way (out of sight in a few seconds). */
    static void unbind(GhoulNpc npc, long now) {
        Mind mind = npc.mind;
        if (mind == null) {
            return;
        }
        mind.owner = null;
        mind.leavingAt = now;
        npc.getPersistentData().m_128473_(ALLY);
    }

    /** Follow its player, or wait here. */
    static void follow(GhoulNpc npc, boolean follow) {
        Mind mind = npc.mind;
        if (mind == null) {
            return;
        }
        mind.follow = follow;
        mind.waitAt = npc.m_20182_();
        npc.getPersistentData().m_128379_(WAITS, !follow);
    }

    /** Provoked by {@code player}: it goes for them. */
    static void provoke(GhoulNpc npc, Mind mind, ServerPlayer player, NpcIdentity who, long now) {
        int grudge = who.temper().grudge();
        mind.grudges.put(player.m_20148_(), grudge == Integer.MAX_VALUE ? Long.MAX_VALUE : now + grudge);
        mind.preySought = Long.MIN_VALUE;
        listen(npc, mind, null, now);
    }

    /** Scared off: it runs a while. */
    static void scare(GhoulNpc npc, Mind mind, long now) {
        mind.scaredUntil = now + 120L;
        listen(npc, mind, null, now);
    }

    /** True if a ghoul NPC walking with {@code player} is near them (the quests' ally tag). */
    public static boolean allyNear(ServerPlayer player) {
        for (GhoulNpc npc : player.m_284548_().m_6443_(GhoulNpc.class, player.m_20191_().m_82400_(16.0), GhoulNpc::m_6084_)) {
            if (allyOf(npc, player)) {
                return true;
            }
        }
        return false;
    }

    /** How many ghoul NPCs walk with {@code player} round them now. */
    static int allies(ServerPlayer player) {
        int count = 0;
        for (GhoulNpc npc : player.m_284548_().m_6443_(GhoulNpc.class, player.m_20191_().m_82400_(128.0), GhoulNpc::m_6084_)) {
            if (allyOf(npc, player)) {
                count++;
            }
        }
        return count;
    }

    /** An ally at rest: it keeps near its player (and catches up at once when left far behind), or waits where it was told. */
    private static void accompany(GhoulNpc npc, Mind mind, ServerPlayer owner, ServerLevel level, long now) {
        if (!mind.follow) {
            Vec3 at = mind.waitAt == null ? npc.m_20182_() : mind.waitAt;
            if (npc.m_20238_(at) > 16.0 && npc.m_21573_().m_26571_()) {
                npc.m_21573_().m_26519_(at.f_82479_, at.f_82480_, at.f_82481_, 0.9);
            }
            return;
        }
        double distance = npc.m_20280_(owner);
        if (distance > 32.0 * 32.0 && owner.m_20096_()) {
            Vec3 behind = owner.m_20182_().m_82546_(GhoulNpcCombat.flat(owner.m_20154_()).m_82490_(2.0));
            npc.m_7678_(behind.f_82479_, owner.m_20186_(), behind.f_82481_, owner.m_146908_(), 0.0F);
            npc.m_21573_().m_26573_();
        } else if (distance > 25.0) {
            if (now % 10L == 0L || npc.m_21573_().m_26571_()) {
                npc.m_21573_().m_5624_(owner, distance > 100.0 ? 1.3 : 1.05);
            }
        } else if (distance < 6.0) {
            npc.m_21573_().m_26573_();
        }
    }

    /** Sent off: it walks away from whoever it was with, and is gone a few seconds on. */
    private static void leave(GhoulNpc npc, Mind mind, ServerLevel level, long now) {
        if (now - mind.leavingAt > 100L) {
            npc.m_146870_();
            return;
        }
        if (now - mind.leavingAt == 1L || npc.m_21573_().m_26571_()) {
            Vec3 away = GhoulNpcCombat.flat(npc.m_20154_()).m_82490_(-1.0);
            Vec3 to = npc.m_20182_().m_82549_(away.m_82490_(16.0));
            npc.m_21573_().m_26519_(to.f_82479_, npc.m_20186_(), to.f_82481_, 0.9);
        }
    }

    // ---------------------------------------------------------------- living

    private static void regen(GhoulNpc npc, Mind mind, long now) {
        boolean rested = mind.hurtAt == Long.MIN_VALUE || now - mind.hurtAt >= NpcRules.REGEN_AFTER; // 1.0.15: the same overflow
        if (rested && now % NpcRules.REGEN_EVERY == 0L && npc.m_21223_() < npc.m_21233_()) {
            npc.m_5634_(1.0F);
        }
    }

    /** At dawn, with no one near enough to see it go, it slips away (one kept as an ally stays). True if it went. */
    private static boolean dawn(GhoulNpc npc, ServerLevel level, long now) {
        if (now % 100L != 0L || NpcRules.night(level.m_46468_()) || npc.m_21532_()) {
            return false;
        }
        for (ServerPlayer player : level.m_6907_()) {
            if (player.m_20280_(npc) <= NpcRules.DAWN_WATCHED * NpcRules.DAWN_WATCHED) {
                return false;
            }
        }
        if (npc.mind != null) {
            GhoulNpcCombat.fizzleAll(npc, npc.mind);
        }
        npc.m_146870_();
        return true;
    }

    /** It was hurt: whoever did it is remembered for as long as its temper holds a grudge. */
    @SubscribeEvent
    public static void hurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof GhoulNpc npc) || !(npc.m_9236_() instanceof ServerLevel)) {
            return;
        }
        Mind mind = npc.mind;
        NpcIdentity who = npc.identity();
        if (mind == null || who == null) {
            return;
        }
        long now = npc.m_9236_().m_46467_();
        mind.hurtAt = now;
        Entity source = event.getSource().m_7639_();
        if (source instanceof LivingEntity attacker && attacker != npc) {
            int grudge = who.temper().grudge();
            boolean holds = true;
            if (attacker instanceof ServerPlayer player) {
                // A blow from a player cools its regard; a friend forgives one stray blow, but the blow that turns it is a betrayal.
                String key = npc.m_20148_().toString();
                QuestLog.Bond bond = QuestServer.bond(player, key);
                int after = NpcBonds.struck(bond.regard());
                if (NpcBonds.betrayal(bond.regard())) {
                    QuestServer.signal(player, Signal.BETRAY, 1, Tags.temper(who.temper().name().toLowerCase(java.util.Locale.ROOT)));
                    player.m_5661_(Component.m_237113_(who.displayName() + " trusted you."), true);
                }
                QuestServer.bond(player, key, bond.regard(after));
                holds = after < 0;
                listen(npc, mind, null, now);
            }
            if (holds) {
                mind.grudges.put(attacker.m_20148_(), grudge == Integer.MAX_VALUE ? Long.MAX_VALUE : now + grudge);
            }
            mind.preySought = Long.MIN_VALUE; // it looks round again at once
        }
    }

    /** An ally can't be hurt by its own player (a sweep that catches it, a stray shot): friendly fire is nothing between them. */
    @SubscribeEvent
    public static void attacked(LivingAttackEvent event) {
        if (event.getEntity() instanceof GhoulNpc npc && event.getSource().m_7639_() instanceof ServerPlayer player && allyOf(npc, player)) {
            event.setCanceled(true);
        }
    }

    // ---------------------------------------------------------------- what the clients see

    private static void sync(GhoulNpc npc, Mind mind, NpcIdentity who) {
        if (!mind.dirty) {
            return;
        }
        mind.dirty = false;
        ControlNetwork.broadcastAround(npc, message(npc, mind, who));
    }

    static S2CGhoulNpc message(GhoulNpc npc, Mind mind, NpcIdentity who) {
        if (mind == null) {
            return new S2CGhoulNpc(npc.m_19879_(), who.pack(), false, Long.MIN_VALUE, GraftMove.NONE.ordinal(), Long.MIN_VALUE, 0.0, 0.0, 0.0, 0,
                Long.MIN_VALUE, Long.MIN_VALUE, 1, -1, Long.MIN_VALUE);
        }
        return new S2CGhoulNpc(npc.m_19879_(), who.pack(), mind.out, mind.outAt, mind.move.ordinal(), mind.moveAt, mind.point.f_82479_,
            mind.point.f_82480_, mind.point.f_82481_, mind.prey, mind.volleyAt, mind.shotAt, mind.shotSide, mind.flip, mind.flipAt);
    }

    /** A player starts seeing it: they are told who it is and what it is doing. */
    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof GhoulNpc npc && event.getEntity() instanceof ServerPlayer player && npc.identity() != null) {
            ControlNetwork.sendTo(player, message(npc, npc.mind, npc.identity()));
        }
    }

    /** Where {@code body}'s middle is (for aiming at it). */
    static Vec3 centre(LivingEntity body) {
        return KaguneServerController.center(body);
    }

    /** The bodies a beat has struck (each at most once). */
    static Set<Integer> hitSet(Mind mind, int key) {
        return mind.hits.computeIfAbsent(key, ignored -> new HashSet<>());
    }
}
