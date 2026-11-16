package com.hex.kagune.control.server;

import com.hex.kagune.control.bikaku.BikakuMutation;
import com.hex.kagune.control.bikaku.GrandDevour;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.sound.HexKaguneSounds;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 3.6.3: the Bikaku mutations ({@link BikakuMutation}: unlocked by a quest, {@link #grant}, kept in the profile) and the
 * first of them, Grand Devouring ({@link GrandDevour}; the owner's words in {@code docs/handoff/owner-words-mutations.md}).
 *
 * <p>A body within {@link #RANGE} blocks at one heart or less ({@link #THRESHOLD}) is fed on by one of his free worms, chosen at random,
 * on its own. From that moment the body is held where it was caught (drawn up into the mouth as it closes), completely stunned and
 * stripped: its kagune put away and kept away (its kakuja, its eyes), whatever is in its hands taken out of them (kept, never deleted:
 * given back if it lives, back in its hands as it dies so it drops as it would), its blows and its use of anything stopped, its own mind
 * (a ghoul's, an investigator's) standing down ({@code GhoulNpcBrain}, {@code AgentBrain}); nothing else can hurt it. It dies at the end
 * of the swallow ({@link GrandDevour#KILL}); the bulge runs down to his back and his bar is refilled when it is in ({@link GrandDevour#FED});
 * the worm does nothing else until it is itself again ({@link GrandDevour#END}). Cut short (his kagune put away, his death, the body
 * gone or taken away), the body is let go with everything given back.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class BikakuDevour {
    /** Profile key: his unlocked mutations (a bitmask of BikakuMutation). */
    static final String MUTATIONS_KEY = "BikakuMutations";
    /** The owner: "it can catch targets within 10 blocks"; "reaches 1 heart (2 HP) or less". */
    static final double RANGE = 10.0;
    static final float THRESHOLD = 2.0F;
    /** How far a held body may be taken from where it was caught before the feeding is given up. */
    static final double LOST = 6.0;

    /** Bodies being devoured (entity id) and who is devouring them. */
    private static final Map<Integer, UUID> HELD = new HashMap<>();
    /** What was taken out of a held body's hands, to give back. */
    private static final Map<Integer, List<Taken>> TAKEN = new HashMap<>();

    /** One thing taken out of a hand. */
    private record Taken(InteractionHand hand, ItemStack stack) {
    }

    private BikakuDevour() {
    }

    // ---------------------------------------------------------------- the mutations

    /** His unlocked mutations (a bitmask of {@link BikakuMutation}). */
    public static int mutations(ServerPlayer player) {
        return KaguneProfile.root(player).m_128451_(MUTATIONS_KEY) & BikakuMutation.all();
    }

    static boolean has(ServerPlayer player, BikakuMutation mutation) {
        return mutation.in(mutations(player));
    }

    /**
     * A mutation unlocked ({@code on}) or taken away, kept in his profile: what the quest that unlocks them calls (the owner: "they're
     * gonna be unlocked through an NPC's quest introduced later today"), and the operators' {@code /kagune bikaku mutation}. False if it
     * was already so, or is still to come.
     */
    public static boolean grant(ServerPlayer player, BikakuMutation mutation, boolean on) {
        int mask = mutations(player);
        if (mutation == null || !mutation.built() || mutation.in(mask) == on) {
            return false;
        }
        KaguneProfile.root(player).m_128405_(MUTATIONS_KEY, on ? mask | mutation.bit() : mask & ~mutation.bit());
        if (on) {
            KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_RESHAPE, 1.0F, KaguneServerController.vary(player, 0.6F, 0.05F));
            player.m_5661_(Component.m_237110_("message.hexkagune.bikaku.mutation", mutation.displayName()), true);
        }
        if (BikakuServer.isBikaku(player)) {
            BikakuServer.State state = BikakuServer.state(player);
            if (!on && mutation == BikakuMutation.GRAND_DEVOURING && state.devourVictim != 0 && !state.devourKilled) {
                cancel(player, state, null);
            }
            BikakuServer.broadcast(player, state);
        }
        return true;
    }

    // ---------------------------------------------------------------- Grand Devouring

    /** The worm feeding (-1 none): it does nothing else. */
    static int busyWorm(BikakuServer.State state) {
        return state.devourVictim != 0 ? state.devourWorm : -1;
    }

    /** True while {@code entity} is being devoured (held, stunned, stripped), until it dies or is let go. */
    public static boolean held(Entity entity) {
        return entity != null && HELD.containsKey(entity.m_19879_());
    }

    static void tick(ServerPlayer player, BikakuServer.State state, long now) {
        if (state.devourVictim != 0) {
            feed(player, state, now);
            return;
        }
        if (now % 4L != 0L || now < state.devourReadyAt || state.kakuja || !player.m_6084_() || !KaguneProfile.isActive(player)
            || !has(player, BikakuMutation.GRAND_DEVOURING)) {
            return;
        }
        int worm = freeWorm(player, state, now);
        if (worm < 0) {
            return;
        }
        LivingEntity victim = victim(player);
        if (victim != null) {
            start(player, state, victim, worm, now);
        }
    }

    /** A worm free to feed, chosen at random among the free ones (-1 none: all of them busy with a bite, a move or a wind-up). */
    static int freeWorm(ServerPlayer player, BikakuServer.State state, long now) {
        if (state.move != com.hex.kagune.control.bikaku.BikakuMove.NONE) {
            return -1; // every worm of his is in the move
        }
        int worms = BikakuServer.worms(player, state);
        List<Integer> free = new ArrayList<>();
        for (int i = 0; i < worms; i++) {
            boolean biting = BikakuServer.out(state, now) && state.biteWorm == i;
            boolean winding = state.charge >= 0 && i == 0;
            if (!biting && !winding) {
                free.add(i);
            }
        }
        return free.isEmpty() ? -1 : free.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(free.size()));
    }

    /** The nearest body in reach at one heart or less that he may hurt and nobody is devouring already. */
    static LivingEntity victim(ServerPlayer player) {
        LivingEntity best = null;
        double nearest = Double.MAX_VALUE;
        Vec3 from = player.m_20182_();
        for (LivingEntity body : player.m_284548_().m_6443_(LivingEntity.class, player.m_20191_().m_82400_(RANGE + 2.0), body -> body != player)) {
            if (!body.m_6084_() || body.m_21223_() > THRESHOLD || held(body) || !KaguneServerController.validSwipeTarget(player, body)) {
                continue;
            }
            double far = KaguneServerController.center(body).m_82554_(from);
            if (far <= RANGE + body.m_20205_() * 0.5 && far < nearest) {
                nearest = far;
                best = body;
            }
        }
        return best;
    }

    static void start(ServerPlayer player, BikakuServer.State state, LivingEntity victim, int worm, long now) {
        state.devourVictim = victim.m_19879_();
        state.devourWorm = worm;
        state.devourAt = now;
        state.devourSpot = victim.m_20182_();
        state.devourWidth = victim.m_20205_();
        state.devourHeight = victim.m_20206_();
        state.devourWorth = victim.m_21233_();
        state.devourKilled = false;
        state.devourFed = false;
        HELD.put(victim.m_19879_(), player.m_20148_());
        strip(victim);
        ServerLevel level = player.m_284548_();
        UkakuFire.sound(level, player.m_20182_().m_82520_(0.0, 3.0, 0.0), com.hex.kagune.control.BikakuSounds.DRAGON_ROAR, 1.3F, KaguneServerController.vary(player, 0.55F, 0.04F));
        UkakuFire.sound(level, player.m_20182_().m_82520_(0.0, 2.0, 0.0), com.hex.kagune.control.BikakuSounds.WRITHE, 1.0F, 0.7F);
        BikakuServer.broadcast(player, state);
    }

    /** Each tick of a feeding, from its first to the worm being itself again. */
    static void feed(ServerPlayer player, BikakuServer.State state, long now) {
        long age = now - state.devourAt;
        ServerLevel level = player.m_284548_();
        Entity entity = level.m_6815_(state.devourVictim);
        LivingEntity victim = entity instanceof LivingEntity living ? living : null;
        boolean his = KaguneProfile.isActive(player) && player.m_6084_() && !state.kakuja;
        if (!state.devourKilled) {
            boolean lost = victim == null || !victim.m_6084_() || victim.m_213877_() || state.devourSpot == null
                || victim.m_20182_().m_82554_(state.devourSpot) > LOST + state.devourHeight;
            if (lost || !his || age < 0) {
                cancel(player, state, victim);
                return;
            }
            hold(victim, state, age);
            strip(victim);
            sounds(level, player, state, age);
            if (age >= GrandDevour.KILL) {
                kill(player, state, victim);
            }
            return;
        }
        if (!his) {
            end(player, state); // the worm went in with his kagune: what it swallowed is lost
            return;
        }
        sounds(level, player, state, age);
        if (!state.devourFed && age >= GrandDevour.FED) {
            state.devourFed = true;
            fed(player, state);
        }
        if (age >= GrandDevour.END) {
            end(player, state);
        }
    }

    /** Held where it was caught, drawn up into the cavity as the jaws close under it; nothing of its own going moves it. */
    private static void hold(LivingEntity victim, BikakuServer.State state, long age) {
        Vec3 at = state.devourSpot.m_82520_(0.0, GrandDevour.lift(age, state.devourHeight), 0.0);
        Vec3 gap = at.m_82546_(victim.m_20182_());
        double length = gap.m_82553_();
        victim.m_20256_(length > 2.0 ? gap.m_82490_(2.0 / length) : gap);
        victim.f_19864_ = true;
        victim.f_19812_ = true;
        victim.f_19789_ = 0.0F;
        if (victim instanceof Mob mob) {
            mob.m_21573_().m_26573_();
        }
    }

    /**
     * Everything it could fight with taken from it, at once and every tick after (the owner: "their kagune must be COMPLETELY OFF, and
     * their weapons must be gone from view"): its hands emptied (what was in them kept to give back), a player's kagune put away (its
     * kakuja with it) and its eyes closed, a player's own feeding given up.
     */
    private static void strip(LivingEntity victim) {
        for (InteractionHand hand : new InteractionHand[] {InteractionHand.MAIN_HAND, InteractionHand.OFF_HAND}) {
            ItemStack stack = hand == InteractionHand.MAIN_HAND ? victim.m_21205_() : victim.m_21206_();
            if (stack != null && !stack.m_41619_()) {
                TAKEN.computeIfAbsent(victim.m_19879_(), ignored -> new ArrayList<>()).add(new Taken(hand, stack));
                victim.m_21008_(hand, ItemStack.f_41583_);
            }
        }
        if (victim instanceof ServerPlayer player) {
            if (KaguneProfile.isActive(player)) {
                KaguneServerController.release(player, false);
                com.hex.kagune.gameplay.KaguneRuntime.remove(player.m_20148_());
                KaguneProfile.setActive(player, false);
                com.hex.kagune.network.KaguneNetwork.broadcast(player);
            }
            KingServer.off(player, false);
            GhoulServer.set(player, false, false);
            if (BikakuServer.isBikaku(player)) {
                BikakuServer.State own = BikakuServer.state(player);
                if (own.devourVictim != 0) {
                    cancel(player, own, null);
                }
            }
        }
    }

    /** Everything taken out of its hands given back (a hand that has something in it again: into its pack, or dropped at its feet). */
    private static void giveBack(LivingEntity victim) {
        List<Taken> taken = TAKEN.remove(victim.m_19879_());
        if (taken == null) {
            return;
        }
        for (Taken one : taken) {
            ItemStack now = one.hand() == InteractionHand.MAIN_HAND ? victim.m_21205_() : victim.m_21206_();
            if (now == null || now.m_41619_()) {
                victim.m_21008_(one.hand(), one.stack());
            } else if (victim instanceof Player player && !stow(player, one.stack())) {
                player.m_36176_(one.stack(), false);
            } else if (!(victim instanceof Player)) {
                victim.m_19983_(one.stack());
            }
        }
    }

    /** Into the first empty slot of a player's pack; false if it is full. */
    private static boolean stow(Player player, ItemStack stack) {
        net.minecraft.world.entity.player.Inventory pack = player.m_150109_();
        for (int i = 0; i < pack.m_6643_(); i++) {
            if (pack.m_8020_(i).m_41619_()) {
                pack.m_6836_(i, stack);
                return true;
            }
        }
        return false;
    }

    /** Swallowed: it dies (by his hand; what it carried given back first, so it drops as any death's would). */
    private static void kill(ServerPlayer player, BikakuServer.State state, LivingEntity victim) {
        giveBack(victim);
        HELD.remove(victim.m_19879_());
        state.devourKilled = true;
        victim.f_19802_ = 0;
        victim.m_6469_(player.m_269291_().m_269075_(player), victim.m_21233_() * 50.0F + 1000.0F);
        ServerLevel level = player.m_284548_();
        UkakuFire.sound(level, state.devourSpot.m_82520_(0.0, state.devourHeight, 0.0), com.hex.kagune.control.BikakuSounds.GUSH, 1.0F, 0.7F);
        if (victim.m_6084_()) {
            // Something kept it alive (a totem, a god's own guard): it is spat out, the worm goes back to itself.
            cancel(player, state, victim);
            return;
        }
        KaguneProfile.addMastery(player, "Bikaku", 2);
        BikakuServer.broadcast(player, state);
    }

    /** Fed: the bar refilled by what it swallowed (the more it was, the more: a player's worth most of the bar). */
    private static void fed(ServerPlayer player, BikakuServer.State state) {
        float worth = Math.min(KaguneProfile.MAX_ENERGY, 35.0F + 2.0F * Math.max(2.0F, state.devourWorth));
        SharedBar.setEnergy(player, Math.min(KaguneProfile.MAX_ENERGY, SharedBar.energy(player) + worth));
        UkakuFire.sound(player.m_284548_(), player.m_20182_().m_82520_(0.0, 1.0, 0.0), com.hex.kagune.control.BikakuSounds.DRAGON_ROAR, 0.6F, 0.9F);
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_RESHAPE, 0.8F, KaguneServerController.vary(player, 0.55F, 0.05F));
        absorbed(player);
    }

    /**
     * 3.7.2 (the owner: "Whenever an entity is absorbed, the player gains a speed buff for 15 seconds and some of our custom effects from
     * foods, any that give special strength and buffs also for 15 seconds"; they chose "Grand Devouring only (Recommended)" and "Strength,
     * Resistance, Refreshed, Replenished (Recommended)"): the body in him, Speed, Strength, Resistance, Refreshed and Replenished, level I,
     * for 15 seconds (as the dishes give them; a longer or stronger one he already has is kept).
     */
    static void absorbed(ServerPlayer player) {
        for (String key : com.hex.kagune.control.bikaku.BikakuRules.ABSORBED) {
            net.minecraft.world.effect.MobEffect effect = com.hex.kagune.control.ControlItems.effect(key);
            if (effect != null) {
                player.m_147207_(new net.minecraft.world.effect.MobEffectInstance(effect, com.hex.kagune.control.bikaku.BikakuRules.ABSORBED_TICKS, 0,
                    false, true, true), player);
            }
        }
    }

    /** Let go before it was swallowed: everything given back; the worm is itself again. */
    static void cancel(ServerPlayer player, BikakuServer.State state, LivingEntity victim) {
        LivingEntity body = victim;
        if (body == null && state.devourVictim != 0 && player.m_284548_().m_6815_(state.devourVictim) instanceof LivingEntity living) {
            body = living;
        }
        if (body != null) {
            giveBack(body);
        }
        HELD.remove(state.devourVictim);
        TAKEN.remove(state.devourVictim);
        end(player, state);
    }

    private static void end(ServerPlayer player, BikakuServer.State state) {
        HELD.remove(state.devourVictim);
        // 3.6.3: its cooldown, from now (the owner: "Grand Devour should have a cooldown of 1 minute").
        state.devourReadyAt = player.m_9236_().m_46467_() + Cooldowns.of(player, (long)com.hex.kagune.control.bikaku.BikakuRules.DEVOUR_COOLDOWN);
        state.devourVictim = 0;
        state.devourWorm = -1;
        state.devourAt = Long.MIN_VALUE;
        state.devourSpot = null;
        state.devourKilled = false;
        state.devourFed = false;
        BikakuServer.broadcast(player, state);
    }

    /** Its sounds (all from the pack's recordings): the roar as it rears, its jaws, wet working, the gulp, the bulge going down. */
    private static void sounds(ServerLevel level, ServerPlayer player, BikakuServer.State state, long age) {
        Vec3 spot = state.devourSpot == null ? player.m_20182_() : state.devourSpot;
        Vec3 mouth = spot.m_82520_(0.0, state.devourHeight * 0.6, 0.0);
        Vec3 back = player.m_20182_().m_82520_(0.0, 1.2, 0.0);
        if (age == GrandDevour.RISE + GrandDevour.REACH) {
            UkakuFire.sound(level, mouth, com.hex.kagune.control.BikakuSounds.EAT, 1.0F, 0.6F);
        } else if (age == GrandDevour.CAUGHT) {
            UkakuFire.sound(level, mouth, com.hex.kagune.control.BikakuSounds.BITE, 1.2F, 0.6F);
            UkakuFire.sound(level, mouth, com.hex.kagune.control.BikakuSounds.SNAP, 1.0F, 0.7F);
        } else if (age > GrandDevour.CAUGHT && age < GrandDevour.SHUT && (age - GrandDevour.CAUGHT) % 16 == 0) {
            UkakuFire.sound(level, mouth, (age / 16) % 2 == 0 ? com.hex.kagune.control.BikakuSounds.SNAP : com.hex.kagune.control.BikakuSounds.DRIP, 0.8F, 0.6F + 0.1F * (float)((age / 16) % 3));
        } else if (age == GrandDevour.SHUT) {
            UkakuFire.sound(level, mouth, com.hex.kagune.control.BikakuSounds.BITE, 1.4F, 0.5F);
        } else if (age == GrandDevour.SHUT + GrandDevour.SWALLOW / 2) {
            UkakuFire.sound(level, mouth, com.hex.kagune.control.BikakuSounds.GULP, 0.9F, 0.55F); // the gulp
        } else if (age > GrandDevour.KILL && age < GrandDevour.FED && (age - GrandDevour.KILL) % 22 == 0) {
            UkakuFire.sound(level, back, (age / 22) % 3 == 0 ? com.hex.kagune.control.BikakuSounds.DIG : com.hex.kagune.control.BikakuSounds.SNAP, 0.6F, 0.5F);
        }
    }

    // ---------------------------------------------------------------- a held body can do nothing; nothing else can hurt it

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void attack(LivingAttackEvent event) {
        if (HELD.isEmpty()) {
            return;
        }
        DamageSource source = event.getSource();
        Entity attacker = source == null ? null : source.m_7639_();
        Entity direct = source == null ? null : source.m_7640_();
        if (held(attacker) && attacker != event.getEntity() || held(direct) && direct != event.getEntity()) {
            event.setCanceled(true); // it can't hurt anything
            return;
        }
        if (held(event.getEntity())) {
            event.setCanceled(true); // nothing hurts it until it is swallowed (its death is the swallow's, never another's)
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void use(PlayerInteractEvent.RightClickItem event) {
        if (held(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void touch(PlayerInteractEvent.EntityInteract event) {
        if (held(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    /** A held player leaving gets everything back (they are let go); a devouring one lets go of what it holds. */
    @SubscribeEvent
    public static void leave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (held(player)) {
            giveBack(player);
            HELD.remove(player.m_19879_());
        }
        if (BikakuServer.isBikaku(player)) {
            BikakuServer.State state = BikakuServer.state(player);
            if (state.devourVictim != 0 && !state.devourKilled) {
                cancel(player, state, null);
            }
        }
    }
}
