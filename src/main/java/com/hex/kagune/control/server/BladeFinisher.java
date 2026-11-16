package com.hex.kagune.control.server;

import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CUkakuFx;
import com.hex.kagune.control.ukaku.Finisher;
import com.hex.kagune.control.ukaku.UkakuSounds;
import com.hex.kagune.data.KaguneProfile;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 4.2.9: the Steel Blade's finisher ({@link Finisher}; the owner: "Has a finisher if the entity / player dies, they are stunned for 2 seconds
 * ..."). A cut of the blade (its four cuts, its flash) that would kill doesn't: the body is left at a sliver of health, as a totem leaves it,
 * and held where it stands for {@link Finisher#STUN} ticks while every client draws the line round its chest and arms; it can do nothing at
 * all (the owner: "during this death fase, the hit player/wild ghoul or whatever, is unable to use any move at all or use the kagune/quinque"):
 * no blow, no shot, no kagune, no quinque, no item, no step; and nothing else may hurt it. When the line meets itself every client cuts it apart
 * ({@code FinisherClient}: the ragdoll) and it dies, the blade's kill. The named NPCs, children and the White Reaper (their own deaths) and
 * bodies too big for it (bosses) die as they always did.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class BladeFinisher {
    private static final class Dying {
        final ServerLevel level;
        final UUID killer;
        final int killerId;
        final long from;

        Dying(ServerLevel level, UUID killer, int killerId, long from) {
            this.level = level;
            this.killer = killer;
            this.killerId = killerId;
            this.from = from;
        }
    }

    /** The bodies under the finisher, by entity id. */
    private static final Map<Integer, Dying> DYING = new HashMap<>();
    /** The player whose blade's cut is landing right now (its death, if it kills, is the finisher), and the finisher's own killing blow. */
    private static ServerPlayer armed;
    private static boolean killing;
    /** The most bodies held at once (a sweep through a crowd still finishes each). */
    static final int MOST = 64;

    private BladeFinisher() {
    }

    /** A cut of {@code player}'s Steel Blade is about to land. */
    static void arm(ServerPlayer player) {
        armed = player;
    }

    /** It has landed. */
    static void disarm() {
        armed = null;
    }

    /** True while {@code entity} is under the finisher: held, unable to act, unhurt by anything else. */
    public static boolean dying(Entity entity) {
        if (entity == null || DYING.isEmpty()) {
            return false;
        }
        Dying d = DYING.get(entity.m_19879_());
        return d != null && d.level == entity.m_9236_();
    }

    /** True if {@code body} dies by the finisher: not a named NPC or a child, nor the White Reaper, nor anything a boss's size. */
    static boolean eligible(LivingEntity body) {
        if (body instanceof com.hex.kagune.control.npc.UniqueBody) {
            return false;
        }
        if (body instanceof com.hex.kagune.control.npc.CcgAgent agent && com.hex.kagune.control.npc.AgentRules.reaperLook(agent.lookIndex())) {
            return false;
        }
        return body.m_20205_() <= 3.0F && body.m_20206_() <= 4.0F && body.m_9236_() instanceof ServerLevel;
    }

    /** A blow of the blade's that would kill: the death is taken back (a sliver of health left, as a totem leaves) and the finisher begins. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void death(LivingDeathEvent event) {
        ServerPlayer by = armed;
        LivingEntity body = event.getEntity();
        if (by == null || killing || body == by || DYING.size() >= MOST || DYING.containsKey(body.m_19879_())
            || event.getSource().m_7639_() != by || !eligible(body)) {
            return;
        }
        event.setCanceled(true);
        body.m_21153_(1.0F);
        begin(body, by);
    }

    static void begin(LivingEntity body, ServerPlayer by) {
        ServerLevel level = (ServerLevel)body.m_9236_();
        DYING.put(body.m_19879_(), new Dying(level, by.m_20148_(), by.m_19879_(), level.m_46467_()));
        hold(body);
        ControlNetwork.broadcastAround(body, new S2CUkakuFx(S2CUkakuFx.FINISH, by.m_19879_(), body.m_19879_(), body.m_20185_(), body.m_20186_(),
            body.m_20189_(), 0.0F, body.f_20883_, 0.0F, 1.0F, Finisher.STUN));
        // 4.1.91's rule: the game's own sounds. The line drawing itself in: a thin ring.
        UkakuFire.sound(level, body.m_20182_(), UkakuSounds.CRYSTAL_RING, 0.7F, 1.9F);
    }

    @SubscribeEvent
    public static void levelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level) || DYING.isEmpty()) {
            return;
        }
        long now = level.m_46467_();
        for (Iterator<Map.Entry<Integer, Dying>> it = DYING.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Dying> entry = it.next();
            Dying d = entry.getValue();
            if (d.level != level) {
                continue;
            }
            Entity entity = level.m_6815_(entry.getKey());
            if (!(entity instanceof LivingEntity body) || !body.m_6084_()) {
                it.remove();
                continue;
            }
            if (now - d.from < Finisher.STUN) {
                hold(body);
                continue;
            }
            it.remove();
            split(level, body, d);
        }
    }

    /** Held where it stands: its own walking slowed to nothing, its motion stopped, a mob's path and target dropped. */
    static void hold(LivingEntity body) {
        body.m_147207_(new MobEffectInstance(MobEffects.f_19597_, 3, 10, false, false, false), null);
        Vec3 motion = body.m_20184_();
        body.m_20256_(new Vec3(0.0, Math.min(motion.f_82480_, 0.0), 0.0));
        if (body instanceof Player) {
            body.f_19864_ = true;
        }
        if (body instanceof Mob mob) {
            mob.m_21573_().m_26573_();
            mob.m_6710_(null);
        }
    }

    /** The line has met itself: every client cuts the body apart, and it dies, the blade's kill. */
    static void split(ServerLevel level, LivingEntity body, Dying d) {
        ControlNetwork.broadcastAround(body, new S2CUkakuFx(S2CUkakuFx.SPLIT, d.killerId, body.m_19879_(), body.m_20185_(), body.m_20186_(),
            body.m_20189_(), 0.0F, body.f_20883_, 0.0F, 1.0F, kagune(body)));
        Vec3 at = body.m_20182_().m_82520_(0.0, body.m_20206_() * 0.55, 0.0);
        UkakuFire.sound(level, at, UkakuSounds.WING_CUT, 1.0F, 0.6F);
        UkakuFire.sound(level, at, UkakuSounds.GORE, 1.0F, 0.8F);
        ServerPlayer killer = level.m_7654_().m_6846_().m_11259_(d.killer);
        DamageSource source = killer != null ? killer.m_269291_().m_269075_(killer) : body.m_269291_().m_269333_(body);
        killing = true;
        try {
            body.m_6469_(source, Float.MAX_VALUE);
        } finally {
            killing = false;
        }
    }

    /** What happens to the body's kagune as it is cut: 1 a Rinkaku's or a Bikaku's shatters, 2 an Ukaku's wings go down with the top half, else 0. */
    static int kagune(LivingEntity body) {
        if (!(body instanceof ServerPlayer player) || !KaguneProfile.isActive(player)) {
            return 0;
        }
        KagunePath path = PathServer.path(player);
        if (path == KagunePath.UKAKU) {
            return 2;
        }
        return Finisher.shatters(path.name()) ? 1 : 0;
    }

    // ------------------------------------------------------------------ nothing hurts it, it hurts nothing

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void attack(LivingAttackEvent event) {
        if (DYING.isEmpty()) {
            return;
        }
        if (!killing && dying(event.getEntity())) {
            event.setCanceled(true);
            return;
        }
        DamageSource source = event.getSource();
        if (source != null && (dying(source.m_7639_()) || dying(source.m_7640_()))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void swing(AttackEntityEvent event) {
        if (dying(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void useItem(PlayerInteractEvent.RightClickItem event) {
        if (dying(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void useBlock(PlayerInteractEvent.RightClickBlock event) {
        if (dying(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void touchEntity(PlayerInteractEvent.EntityInteract event) {
        if (dying(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void startUsing(LivingEntityUseItemEvent.Start event) {
        if (dying(event.getEntity())) {
            event.setCanceled(true);
        }
    }
}
