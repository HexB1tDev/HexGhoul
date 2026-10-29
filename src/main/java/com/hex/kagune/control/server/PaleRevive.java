package com.hex.kagune.control.server;

import com.hex.kagune.control.pale.PaleMutation;
import com.hex.kagune.control.pale.ReviveRules;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CPaleMutations;
import com.hex.kagune.control.network.S2CPaleRevive;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.sound.HexKaguneSounds;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 6.2.0, Pale's mutations ({@link PaleMutation}: points from mastery milestones, spent in her menu) and the first of them, You Thought
 * You'd Get Rid of Me That Easily? ({@link ReviveRules}): the blow that would kill her, while it is ready, is cancelled; her body is
 * blown apart instead (every client draws the pieces from {@link S2CPaleRevive}) and she is held where she stood, nothing able to
 * hurt her and nothing she does hurting anything, until the lines have pulled her back together: then she stands there whole, at
 * full health, and it is ready again five minutes later. It is not cheated by falling out of the world or /kill.
 *
 * <p>6.2.0: everything acts as if she really died ({@link ReviveForget}); she stands up in her kagune, eyes and all, even if she
 * went down without it; and a gust bursts out of her as she is whole, throwing everything near her back ({@link #GUST_RANGE}).
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class PaleRevive {
    /** Profile keys: her unlocked mutations (a bitmask of PaleMutation), and the game tick it is ready again. */
    static final String MUTATIONS_KEY = "PaleMutations";
    static final String READY_KEY = "PaleReviveReadyAt";
    static final String INSTANT_KEY = "PaleInstantPunch";
    /** 6.2.0: set while we have made her invulnerable (in pieces), so a crash or a quit never leaves her that way. */
    static final String SHIELD_KEY = "PaleReviveShield";
    /** 6.2.0: the gust as she stands up whole: how far round her (blocks, level), how high, how hard (blocks a tick) and how far up. */
    static final double GUST_RANGE = 4.0;
    static final double GUST_HEIGHT = 2.5;
    static final double GUST_PUSH = 1.25;
    static final double GUST_LIFT = 0.45;
    /** 6.2.0: the RC she stands up with at least, so her kagune can come out (the original wants 5 to manifest). */
    static final float AWAKE_ENERGY = 25.0F;
    /** How far down (blocks) the ground under her is looked for, at most. */
    static final double GROUND_DEPTH = 3.0;

    /**
     * A body in pieces: since when, how it came apart, where she stood, the way she faced, the ground under her; whether her kagune
     * was out (6.2.0: if not, it comes out as she stands up) and whether she was invulnerable already (left as it was).
     */
    record Down(long at, long seed, Vec3 where, float yaw, float ground, boolean wasOut, boolean wasInvulnerable) {
    }

    private static final Map<UUID, Down> DOWN = new HashMap<>();

    private PaleRevive() {
    }

    // ------------------------------------------------------------------ mutations

    /** Her unlocked mutations (a bitmask of {@link PaleMutation}). */
    public static int mutations(ServerPlayer player) {
        return KaguneProfile.root(player).m_128451_(MUTATIONS_KEY) & PaleMutation.all();
    }

    /**
     * 7.0.0: whether a death would not take: Owl DNA's mutation (dormant), or Graft DNA's own You Thought You'd Get Rid of Me
     * That Easily? (the same death, the same knitting back together; slower since 7.0.0, {@link ReviveRules}). 1.0.41: and the same
     * on the Ukaku path (his fifth mutation) and the Rinkaku path (its one passive, its quest done).
     */
    static boolean revives(ServerPlayer player) {
        return hasMutation(player, PaleMutation.NOT_THAT_EASILY)
            || GraftServer.hasMutation(player, com.hex.kagune.control.graft.GraftMutation.NOT_THAT_EASILY)
            || UkakuServer.hasMutation(player, com.hex.kagune.control.ukaku.UkakuMutation.NOT_THAT_EASILY)
            || rinkaku(player);
    }

    /** 1.0.41: on the Rinkaku path with its passive's quest done. */
    static boolean rinkaku(ServerPlayer player) {
        return PathServer.path(player) == com.hex.kagune.control.KagunePath.RINKAKU
            && QuestServer.log(player).open(com.hex.kagune.control.quest.Gate.RINKAKU_NOT_THAT_EASILY);
    }

    /** True if she is on the Pale path and has unlocked {@code mutation}. */
    public static boolean hasMutation(ServerPlayer player, PaleMutation mutation) {
        return PaleServer.isPale(player) && mutation.in(mutations(player));
    }

    /** 6.2.2: Instant Big Punch is on (and she has Witness This): Z tapped is the full-charge punch at once. */
    public static boolean instantPunch(ServerPlayer player) {
        return hasMutation(player, PaleMutation.WITNESS_THIS) && KaguneProfile.root(player).m_128471_(INSTANT_KEY);
    }

    /** Her menu's Instant Big Punch switch. */
    static void instantPunch(ServerPlayer player, boolean on) {
        if (!hasMutation(player, PaleMutation.WITNESS_THIS)) {
            return;
        }
        KaguneProfile.root(player).m_128379_(INSTANT_KEY, on);
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_RESHAPE, 0.5F, KaguneServerController.vary(player, on ? 1.4F : 0.9F, 0.05F));
        sync(player);
    }

    /** Her menu asks to unlock a mutation: not had yet, and a milestone point left to spend. */
    static void unlock(ServerPlayer player, int ordinal) {
        PaleMutation mutation = PaleMutation.byOrdinal(ordinal);
        int mastery = KaguneProfile.mastery(player);
        int mask = mutations(player);
        if (mutation == null || !PaleMutation.canUnlock(mastery, mask, mutation)) {
            return;
        }
        KaguneProfile.root(player).m_128405_(MUTATIONS_KEY, mask | mutation.bit());
        player.m_5661_(Component.m_237110_("message.hexkagune.pale.mutation_unlocked", mutation.displayName()), true);
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_RESHAPE, 0.8F, KaguneServerController.vary(player, 1.1F, 0.05F));
        sync(player);
    }

    /** She learns her mutations and when her death that does not take is ready again (her menu shows both). */
    static void sync(ServerPlayer player) {
        ControlNetwork.sendTo(player, new S2CPaleMutations(mutations(player)
            | (KaguneProfile.root(player).m_128471_(INSTANT_KEY) ? PaleMutation.INSTANT_PUNCH : 0), KaguneProfile.root(player).m_128454_(READY_KEY)));
    }

    /** 6.2.0: it is ready again now ({@link Cooldowns}); her menu told. */
    static void clearCooldown(ServerPlayer player) {
        if (KaguneProfile.root(player).m_128441_(READY_KEY)) {
            KaguneProfile.root(player).m_128473_(READY_KEY);
        }
        if (PaleServer.isPale(player)) {
            sync(player);
        }
    }

    // ------------------------------------------------------------------ the death that does not take

    /** Whether she is in pieces now (held, untouchable, nothing she does counts). */
    public static boolean down(ServerPlayer player) {
        return DOWN.containsKey(player.m_20148_());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void dying(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || down(player) || !revives(player)) {
            return;
        }
        String cause = event.getSource().m_19385_();
        if ("outOfWorld".equals(cause) || "genericKill".equals(cause)) {
            return; // out of the world, or /kill: those take
        }
        ServerLevel level = player.m_284548_();
        long now = level.m_46467_();
        if (!ReviveRules.ready(now, KaguneProfile.root(player).m_128454_(READY_KEY))) {
            return;
        }
        event.setCanceled(true);
        player.m_21153_(1.0F);
        player.f_19789_ = 0.0F;
        KaguneProfile.root(player).m_128356_(READY_KEY, now + Cooldowns.of(player, ReviveRules.COOLDOWN));
        Vec3 where = player.m_20182_();
        long seed = now * 0x9E3779B97F4A7C15L ^ player.m_20148_().getLeastSignificantBits();
        Down down = new Down(now, seed, where, player.f_20883_, (float)ground(level, player, where), KaguneProfile.isActive(player),
            player.m_20147_());
        DOWN.put(player.m_20148_(), down);
        PaleServer.apart(player);
        GraftServer.apart(player);
        KingServer.off(player, false); // 1.0.41: (a final kakuja breaks as it would in a death)
        // 6.2.0: as far as anything knows she is dead: nothing remembers her, and nothing can set on her while she is in pieces.
        shield(player, down, true);
        ReviveForget.forget(player, ReviveForget.RANGE);
        ControlNetwork.broadcast(player, message(player, down, false));
        // Burst apart: the wet tearing of it, bones going.
        UkakuFire.sound(level, where.m_82520_(0.0, 1.0, 0.0), "entity.generic.explode", 0.7F, KaguneServerController.vary(player, 1.45F, 0.05F));
        UkakuFire.sound(level, where.m_82520_(0.0, 1.0, 0.0), "entity.slime.squish", 1.2F, KaguneServerController.vary(player, 0.5F, 0.05F));
        KaguneServerController.playAt(player, player, HexKaguneSounds.MANIFEST_RIP, 1.0F, KaguneServerController.vary(player, 0.7F, 0.05F));
        KaguneServerController.playAt(player, player, HexKaguneSounds.BONE_CRACK, 1.0F, KaguneServerController.vary(player, 0.8F, 0.05F));
        if (PaleServer.isPale(player)) {
            sync(player);
        }
    }

    /** How far below her feet (blocks, 0 or less) the ground is, looked for down to {@link #GROUND_DEPTH}. */
    static double ground(ServerLevel level, ServerPlayer player, Vec3 feet) {
        Vec3 from = feet.m_82520_(0.0, 0.25, 0.0);
        BlockHitResult hit = level.m_45547_(new ClipContext(from, feet.m_82520_(0.0, -GROUND_DEPTH, 0.0), ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE, player));
        return hit.m_6662_() == HitResult.Type.MISS ? -GROUND_DEPTH : Math.min(0.0, hit.m_82450_().f_82480_ - feet.f_82480_);
    }

    static S2CPaleRevive message(ServerPlayer player, Down down, boolean over) {
        return new S2CPaleRevive(player.m_20148_(), down.at(), down.seed(), down.where().f_82479_, down.where().f_82480_, down.where().f_82481_,
            down.yaw(), down.ground(), over);
    }

    /** Nothing hurts her while she is in pieces, and nothing she does hurts anything. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && down(player)
            || event.getSource().m_7639_() instanceof ServerPlayer attacker && down(attacker)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        Down down = DOWN.get(player.m_20148_());
        if (down == null) {
            return;
        }
        long age = player.m_9236_().m_46467_() - down.at();
        // Held where she stood.
        player.m_20256_(Vec3.f_82478_);
        player.f_19789_ = 0.0F;
        if (player.m_20182_().m_82557_(down.where()) > 0.01) {
            player.m_6021_(down.where().f_82479_, down.where().f_82480_, down.where().f_82481_);
        }
        if (age == ReviveRules.SCATTER) {
            // The lines run out of her chest.
            KaguneServerController.playAt(player, player, HexKaguneSounds.KAGUNE_STRAIN, 1.0F, KaguneServerController.vary(player, 0.75F, 0.05F));
        }
        if (age == ReviveRules.SCATTER + ReviveRules.LINES) {
            KaguneServerController.playAt(player, player, HexKaguneSounds.KAGUNE_RESHAPE, 1.0F, KaguneServerController.vary(player, 0.6F, 0.05F));
        }
        if (age > 0 && age % 10 == 0 && age < ReviveRules.WHOLE) {
            ReviveForget.forget(player, ReviveForget.NEAR); // still nothing remembers her
        }
        if (age >= ReviveRules.WHOLE || age < 0) {
            DOWN.remove(player.m_20148_());
            shield(player, down, false);
            player.m_21153_(player.m_21233_());
            player.f_19789_ = 0.0F;
            UkakuFire.sound(player.m_284548_(), player.m_20182_(), "entity.slime.squish", 1.0F, KaguneServerController.vary(player, 0.8F, 0.05F));
            KaguneServerController.playAt(player, player, HexKaguneSounds.BONE_CRACK, 1.0F, KaguneServerController.vary(player, 1.1F, 0.05F));
            player.m_5661_(Component.m_237115_("message.hexkagune.pale.not_that_easily"), true);
            // 6.2.0: they have forgotten her; a gust bursts out of her; and she is in her kagune, whatever she went down in.
            ReviveForget.forget(player, ReviveForget.RANGE);
            gust(player);
            if (!down.wasOut()) {
                awaken(player);
            }
        }
    }

    /** It ends at once (she left, went to another world, changed path): every client told. */
    static void end(ServerPlayer player) {
        Down down = DOWN.remove(player.m_20148_());
        if (down != null) {
            shield(player, down, false);
            player.m_21153_(player.m_21233_());
            ControlNetwork.broadcast(player, message(player, down, true));
        }
    }

    /**
     * 6.2.0: while she is in pieces she is invulnerable (no mob's targeting will take her: the Warden's, a goal's, a brain's, a
     * boss's), unless she already was; marked in her profile so it is undone however it ends (a quit, a crash: see loggedIn).
     */
    static void shield(ServerPlayer player, Down down, boolean on) {
        if (down.wasInvulnerable()) {
            return;
        }
        player.m_20331_(on);
        if (on) {
            KaguneProfile.root(player).m_128379_(SHIELD_KEY, true);
        } else {
            KaguneProfile.root(player).m_128473_(SHIELD_KEY);
        }
    }

    /**
     * 6.2.0: the gust as she stands up whole: everything within {@link #GUST_RANGE} of her (mobs, players, whatever lies about) is
     * thrown back and up, the harder the nearer, less what resists knockback.
     */
    static void gust(ServerPlayer player) {
        ServerLevel level = player.m_284548_();
        Vec3 at = player.m_20182_();
        AABB area = new AABB(at.f_82479_ - GUST_RANGE, at.f_82480_ - GUST_HEIGHT, at.f_82481_ - GUST_RANGE, at.f_82479_ + GUST_RANGE,
            at.f_82480_ + GUST_HEIGHT, at.f_82481_ + GUST_RANGE);
        for (Entity entity : level.m_6443_(Entity.class, area, entity -> entity != player && entity.m_6084_() && !entity.m_5833_())) {
            Vec3 away = entity.m_20182_().m_82546_(at);
            double distance = Math.sqrt(away.f_82479_ * away.f_82479_ + away.f_82481_ * away.f_82481_);
            if (distance > GUST_RANGE) {
                continue;
            }
            Vec3 flat = distance < 1.0E-3 ? UkakuFire.horizontal(entity.m_20154_()).m_82490_(-1.0) : new Vec3(away.f_82479_ / distance, 0.0, away.f_82481_ / distance);
            double push = GUST_PUSH * (1.0 - 0.45 * distance / GUST_RANGE);
            if (entity instanceof LivingEntity living) {
                push *= 1.0 - 0.6 * Math.max(0.0, Math.min(1.0, living.m_21133_(Attributes.f_22278_)));
                living.f_19812_ = true;
            }
            entity.m_20256_(entity.m_20184_().m_82490_(0.2).m_82549_(new Vec3(flat.f_82479_ * push, GUST_LIFT, flat.f_82481_ * push)));
            entity.f_19864_ = true;
        }
        UkakuFire.sound(level, at.m_82520_(0.0, 1.0, 0.0), "entity.ender_dragon.flap", 1.2F, KaguneServerController.vary(player, 0.75F, 0.05F));
        UkakuFire.sound(level, at.m_82520_(0.0, 1.0, 0.0), "entity.player.attack.sweep", 1.0F, KaguneServerController.vary(player, 0.6F, 0.05F));
    }

    /**
     * 6.2.0: she went down without her kagune: she stands up in it, eyes open and the kagune out (as Alt + R brings it), whatever had
     * it put away (a spent kagune's wait is over, and she has the RC to bring it out).
     */
    static void awaken(ServerPlayer player) {
        if (KaguneProfile.isActive(player) || !KaguneProfile.isUnlocked(player)) {
            return;
        }
        KaguneLock.clear(player);
        if (KaguneProfile.energy(player) < AWAKE_ENERGY) {
            KaguneProfile.setEnergy(player, AWAKE_ENERGY);
        }
        GhoulServer.press(player, true);
    }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof ServerPlayer subject) {
            Down down = DOWN.get(subject.m_20148_());
            if (down != null) {
                ControlNetwork.sendTo(viewer, message(subject, down, false));
            }
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (KaguneProfile.root(player).m_128471_(SHIELD_KEY) && !down(player)) {
            // She left (or the server stopped) while in pieces: not invulnerable any more.
            player.m_20331_(false);
            KaguneProfile.root(player).m_128473_(SHIELD_KEY);
        }
        if (PaleServer.isPale(player)) {
            sync(player);
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            end(player);
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Down down = DOWN.remove(player.m_20148_());
            if (down != null) {
                // Whole again before she goes (she is not left on one heart, nor invulnerable).
                shield(player, down, false);
                player.m_21153_(player.m_21233_());
            }
        }
    }
}
