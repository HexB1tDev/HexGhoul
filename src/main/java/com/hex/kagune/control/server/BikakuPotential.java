package com.hex.kagune.control.server;

import com.hex.kagune.control.bikaku.BikakuMutation;
import com.hex.kagune.control.bikaku.PotentialRules;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CPotential;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 3.7.3, Potential Unlock, torn apart and pulled back together (the owner's words in {@code docs/handoff/owner-words-mutations.md}; the
 * preview they went ahead with, "Continue", in {@code docs/handoff/potential-reattach-preview.png}). On the Bikaku path with the mutation:
 *
 * <p>A blow that would kill him, while his bar has energy, doesn't: he is left at one heart, it costs {@link PotentialRules#SURVIVE_COST}
 * energy, and it tears a part off him. A single blow of half his full health or more that doesn't kill him tears one off too (rare: never an
 * ordinary blow). Without energy nothing protects him and nothing is torn. A part already off is never torn again; every part off has its
 * own timeline ({@link PotentialRules}): thrown, lying, its strands reaching it and hauling it back; while they pull it draws energy, and
 * with none it waits where it is until there is; back on and whole, he gets a small heal. Every client is told and draws it ({@link
 * S2CPotential}). Not while Grand Devouring holds him (that death takes), nor out of the world or by /kill.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class BikakuPotential {
    /** One part off: which, how it was thrown, the way the blow went (his body's frame then), where he stood and faced, the ground. */
    static final class Torn {
        final PotentialRules.Part part;
        final long seed;
        final float blowX;
        final float blowZ;
        final Vec3 anchor;
        final float yaw;
        final float ground;
        double age;
        boolean paused;

        Torn(PotentialRules.Part part, long seed, float blowX, float blowZ, Vec3 anchor, float yaw, float ground) {
            this.part = part;
            this.seed = seed;
            this.blowX = blowX;
            this.blowZ = blowZ;
            this.anchor = anchor;
            this.yaw = yaw;
            this.ground = ground;
        }
    }

    private static final Map<UUID, List<Torn>> TORN = new HashMap<>();

    private BikakuPotential() {
    }

    /** True if {@code player} has Potential Unlock on the Bikaku path. */
    static boolean protects(ServerPlayer player) {
        return BikakuServer.isBikaku(player) && BikakuMutation.POTENTIAL_UNLOCK.in(BikakuDevour.mutations(player));
    }

    /** The parts off {@code player} now ({@link PotentialRules.Part#bit} bits), being pulled back or not yet whole. */
    public static int off(ServerPlayer player) {
        int bits = 0;
        for (Torn torn : TORN.getOrDefault(player.m_20148_(), List.of())) {
            bits |= torn.part.bit();
        }
        return bits;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void dying(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.m_7500_() || !protects(player) || BikakuDevour.held(player)) {
            return;
        }
        String cause = event.getSource().m_19385_();
        if ("outOfWorld".equals(cause) || "genericKill".equals(cause)) {
            return; // out of the world, or /kill: those take
        }
        if (SharedBar.energy(player) <= 0.5F) {
            return; // the owner: "Without energy, the mutation can no longer protect the player from otherwise lethal damage."
        }
        event.setCanceled(true);
        player.m_21153_(PotentialRules.SURVIVE_HEALTH);
        player.f_19789_ = 0.0F;
        if (!SharedBar.consumeEnergy(player, PotentialRules.SURVIVE_COST)) {
            SharedBar.setEnergy(player, 0.0F);
        }
        tear(player, event.getSource(), true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void damaged(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.m_7500_() || !protects(player) || BikakuDevour.held(player)) {
            return;
        }
        float amount = event.getAmount();
        // The blow that kills him is the death's ({@link #dying}); this is one that doesn't, but takes half of him or more at once.
        if (amount < PotentialRules.HEAVY_BLOW * player.m_21233_() || amount >= player.m_21223_() || SharedBar.energy(player) <= 0.5F) {
            return;
        }
        tear(player, event.getSource(), false);
    }

    /** A part torn off by the blow from {@code source}: which, on the side it came from; thrown the way it went. */
    static void tear(ServerPlayer player, DamageSource source, boolean lethal) {
        List<Torn> torn = TORN.computeIfAbsent(player.m_20148_(), id -> new ArrayList<>());
        double turn = Math.toRadians(player.f_20883_);
        double rightX = -Math.cos(turn);
        double rightZ = -Math.sin(turn);
        double aheadX = -Math.sin(turn);
        double aheadZ = Math.cos(turn);
        Vec3 at = player.m_20182_();
        Entity by = source.m_7639_();
        Vec3 from = by != null ? by.m_20182_() : source.m_7270_();
        double dx = from == null ? -aheadX : at.f_82479_ - from.f_82479_;
        double dz = from == null ? -aheadZ : at.f_82481_ - from.f_82481_;
        double bx = dx * rightX + dz * rightZ;
        double bz = dx * aheadX + dz * aheadZ;
        double length = Math.sqrt(bx * bx + bz * bz);
        if (length < 1.0E-4) {
            bx = 0.0;
            bz = -1.0;
        } else {
            bx /= length;
            bz /= length;
        }
        long now = player.m_9236_().m_46467_();
        SplittableRandom random = new SplittableRandom(now * 0x9E3779B97F4A7C15L ^ player.m_20148_().getLeastSignificantBits() ^ torn.size());
        PotentialRules.Part part = PotentialRules.pick(off(player), lethal, -bx, random.nextDouble(), random.nextDouble());
        if (part == null) {
            return; // every part is already off
        }
        ServerLevel level = player.m_284548_();
        torn.add(new Torn(part, random.nextLong(), (float)bx, (float)bz, at, player.f_20883_, (float)PaleRevive.ground(level, player, at)));
        UkakuFire.sound(level, at.m_82520_(0.0, 1.2, 0.0), "hexkagune:sfx.gush_big", 1.0F, 0.9F);
        UkakuFire.sound(level, at.m_82520_(0.0, 1.2, 0.0), "hexkagune:sfx.crunch", 0.9F, 0.8F);
        sync(player);
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        List<Torn> torn = TORN.get(player.m_20148_());
        if (torn == null || torn.isEmpty()) {
            return;
        }
        if (!player.m_6084_()) {
            TORN.remove(player.m_20148_());
            sync(player);
            return;
        }
        boolean changed = false;
        ServerLevel level = player.m_284548_();
        for (Iterator<Torn> it = torn.iterator(); it.hasNext(); ) {
            Torn t = it.next();
            double was = t.age;
            if (PotentialRules.pulling(t.age)) {
                // The strands draw energy while they reach and haul; with none they hold where they are until there is.
                boolean paid = SharedBar.consumeEnergy(player, PotentialRules.PULL_DRAIN);
                if (paid == t.paused) {
                    t.paused = !paid;
                    changed = true;
                }
                if (paid) {
                    t.age += 1.0;
                }
            } else {
                t.age += 1.0;
            }
            if (was < PotentialRules.HAUL_AT && t.age >= PotentialRules.HAUL_AT) {
                UkakuFire.sound(level, player.m_20182_().m_82520_(0.0, 1.0, 0.0), "hexkagune:sfx.writhe", 0.8F, 1.0F); // they take hold and haul
            }
            if (was < PotentialRules.HOME_AT && t.age >= PotentialRules.HOME_AT) {
                UkakuFire.sound(level, player.m_20182_().m_82520_(0.0, 1.0, 0.0), "hexkagune:sfx.crunch", 0.7F, 1.1F); // back on
            }
            if (t.age >= PotentialRules.WHOLE) {
                it.remove();
                GhoulRegen.healOwn(player, PotentialRules.HEAL); // the owner: "only after the body part has completely reconnected"
                changed = true;
            }
        }
        if (torn.isEmpty()) {
            TORN.remove(player.m_20148_());
        }
        if (changed) {
            sync(player);
        }
    }

    static S2CPotential message(ServerPlayer player) {
        List<Torn> torn = TORN.getOrDefault(player.m_20148_(), List.of());
        S2CPotential.Torn[] out = new S2CPotential.Torn[Math.min(S2CPotential.MAX, torn.size())];
        for (int i = 0; i < out.length; i++) {
            Torn t = torn.get(i);
            out[i] = new S2CPotential.Torn(t.part.ordinal(), t.seed, t.blowX, t.blowZ, t.anchor.f_82479_, t.anchor.f_82480_, t.anchor.f_82481_, t.yaw,
                t.ground, (float)t.age, t.paused);
        }
        return new S2CPotential(player.m_20148_(), out);
    }

    static void sync(ServerPlayer player) {
        ControlNetwork.broadcast(player, message(player));
    }

    /** Whole again at once (a new world, a new life, gone): every client told. */
    static void clear(ServerPlayer player) {
        if (TORN.remove(player.m_20148_()) != null) {
            sync(player);
        }
    }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof ServerPlayer subject && TORN.containsKey(subject.m_20148_())) {
            ControlNetwork.sendTo(viewer, message(subject));
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clear(player);
        }
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clear(player);
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TORN.remove(player.m_20148_());
        }
    }
}
