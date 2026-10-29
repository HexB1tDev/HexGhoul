package com.hex.kagune.control.server;

import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.king.KingForm;
import com.hex.kagune.control.king.KingMove;
import com.hex.kagune.control.king.KingRules;
import com.hex.kagune.control.king.KingTransform;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CKingState;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.network.KaguneNetwork;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 1.0.5: the final kakujas on the server. The Dragon spent ({@link #awaken}, from {@code control.awakening}), a Rinkaku ghoul may
 * choose one, once, from his menu ({@link #request}); its key then puts it on and takes it off. Worn, it is his body: the kagune
 * stays in and its keys do nothing ({@code DragonLock}), and it gives what the spec gives ({@link KingRules}): melee, speed,
 * attack speed and health as transient modifiers (never saved, so a crash can't leave them on), damage taken off, a slow mend.
 * Never kept through death, a dimension change or a logout. The admin preview ({@link #preview}) puts on the look alone.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class KingServer {
    private static final String KEY = "HexKing";
    private static final UUID MELEE = UUID.fromString("5e2b8b0c-7d55-4f1e-9a0b-1c0de5a10001");
    private static final UUID SPEED = UUID.fromString("5e2b8b0c-7d55-4f1e-9a0b-1c0de5a10002");
    private static final UUID ATTACK_SPEED = UUID.fromString("5e2b8b0c-7d55-4f1e-9a0b-1c0de5a10003");
    private static final UUID HEALTH = UUID.fromString("5e2b8b0c-7d55-4f1e-9a0b-1c0de5a10004");
    /** 1.0.7: War can't be moved: knockback resistance to the full. */
    private static final UUID ROOTED = UUID.fromString("5e2b8b0c-7d55-4f1e-9a0b-1c0de5a10005");
    /** 1.0.26: the sprint's own speed, on while he sprints in a final kakuja. */
    private static final UUID SPRINT = UUID.fromString("5e2b8b0c-7d55-4f1e-9a0b-1c0de5a10006");
    private static final SoundEvent EYE_OPEN = SoundEvent.m_262824_(ResourceLocation.m_214293_("hexkagune", "eye_open"));
    /** Ticks between War's mends, and what each mends. */
    private static final long MEND_TICKS = 20L;
    private static final float MEND = 1.0F;

    /** A final kakuja on a ghoul (or going): what, on or going, since when, and whether it is the admin's preview (the look alone). */
    public record Worn(KingForm form, boolean on, long since, boolean preview) {
    }

    /**
     * 1.0.6: a final kakuja's abilities on the server. War's ({@code control.awakening.WarKit}) is installed by the awakening at common
     * setup, so this class never names it: every request, tick, blow taken and ending reaches it through here while War is worn.
     */
    public interface Kit {
        /** Its key {@code slot} ({@code WarRules.Ability} ordinal) did {@code action} (PRESS, RELEASE, CROUCH_PRESS), aimed at {@code target}. */
        void key(ServerPlayer player, int slot, int action, int target);

        void tick(ServerPlayer player, long now);

        /** 1.0.14: an M1, at {@code target} (the body under the crosshair, 0: none). */
        void strike(ServerPlayer player, int target);

        /** 1.0.20: an M1 clicked with {@code flags} ({@code KingCombo}: the jump key held, off the ground). */
        default void strike(ServerPlayer player, int target, int flags) {
            this.strike(player, target);
        }

        /** 1.0.20: the heavy (RMB) pressed or let go ({@code flags}, {@code KingCombo}), at {@code target}. */
        default void heavy(ServerPlayer player, int flags, int target) {
        }

        /** 1.0.29: the keys held this tick while it drives something (Centipede Dominion), {@code WarRules.WYRM_*} bits; 1.0.39: and his aim ({@code WarRules.wyrmAim}). */
        default void drive(ServerPlayer player, int keys, int aim) {
        }

        /** A blow of {@code amount} from {@code attacker} (null: none) on the wearer: what is left of it (0 turns it aside). */
        float hurt(ServerPlayer player, net.minecraft.world.entity.Entity attacker, float amount);

        /** 1.0.15: as {@link #hurt}, knowing what struck ({@code direct}: the arrow, the fist; null: nothing). */
        default float hurt(ServerPlayer player, net.minecraft.world.entity.Entity attacker, net.minecraft.world.entity.Entity direct, float amount) {
            return this.hurt(player, attacker, amount);
        }

        /** 1.0.15: a blow of {@code amount} on another player ({@code victim}), which a wearer near may shelter: what is left of it. */
        default float cover(ServerPlayer victim, net.minecraft.world.entity.Entity attacker, net.minecraft.world.entity.Entity direct, float amount) {
            return amount;
        }

        /**
         * 1.0.34: a projectile about to hit the wearer: where something of its kit meets it (world x, y, z), to be sent back at whoever
         * shot it; null lets it through.
         */
        default double[] deflect(ServerPlayer player, net.minecraft.world.entity.projectile.Projectile projectile) {
            return null;
        }

        /** It came off: whatever it was doing ends. */
        void off(ServerPlayer player);
    }

    /** What a final kakuja's body is doing now: the move, since when, at what level, at whom. */
    private record Motion(KingMove move, long at, int level, int target) {
        static final Motion IDLE = new Motion(KingMove.NONE, 0L, 0, 0);
    }

    private static final Map<UUID, Worn> WORN = new HashMap<>();
    private static final Map<UUID, Motion> MOTIONS = new HashMap<>();
    /**
     * When each of a wearer's abilities is ready again (game ticks). Kept through taking it off and on, not saved. 1.0.35: each final
     * kakuja its own (War's six and Peace's six share their keys, not their waits: one path's cooldowns never block the other's).
     */
    private static final Map<UUID, Map<KingForm, long[]>> READY = new HashMap<>();
    /**
     * 1.0.22: K held with the kagune out: the kagune goes back in first (its own animation, ticks long), and the final kakuja comes
     * over him the moment it is in, if it is in by this tick.
     */
    private static final Map<UUID, Long> PENDING = new HashMap<>();
    /** How long a pending final kakuja waits for the kagune to go in (ticks). */
    static final long PENDING_TICKS = 80L;
    /** 1.0.15: each final kakuja's kit, by form. */
    private static final Map<KingForm, Kit> KITS = new java.util.concurrent.ConcurrentHashMap<>();

    /** Called once by the awakening at common setup: War's kit. */
    public static void installKit(Kit war) {
        installKit(KingForm.WAR, war);
    }

    /** 1.0.15: {@code form}'s kit. */
    public static void installKit(KingForm form, Kit kit) {
        KITS.put(form, kit);
    }

    private static Kit kit(KingForm form) {
        return form == null || form == KingForm.NONE ? null : KITS.get(form);
    }

    /** 1.0.34: where the kit {@code player} wears meets {@code projectile} to send it back ({@link Kit#deflect}), or null. */
    public static double[] deflect(ServerPlayer player, net.minecraft.world.entity.projectile.Projectile projectile) {
        if (!WORN.containsKey(player.m_20148_())) {
            return null;
        }
        Kit kit = kit(wearing(player));
        return kit == null ? null : kit.deflect(player, projectile);
    }

    private KingServer() {
    }

    // ------------------------------------------------------------------ the crown

    static String saved(ServerPlayer player) {
        return KaguneProfile.root(player).m_128461_(KEY);
    }

    private static void save(ServerPlayer player, String saved) {
        KaguneProfile.root(player).m_128359_(KEY, saved);
    }

    /** True once the Dragon has been through {@code player}. */
    public static boolean awake(ServerPlayer player) {
        return KingRules.awake(saved(player));
    }

    /** The final kakuja {@code player} chose, or NONE. */
    public static KingForm chosen(ServerPlayer player) {
        return KingRules.chosen(saved(player));
    }

    /** The Dragon is spent in {@code player}: a crown waits (called by the Dragon; harmless again). 1.0.11: Rinkaku's alone. */
    public static void awaken(ServerPlayer player) {
        if (awake(player) || PathServer.path(player) != KagunePath.RINKAKU) {
            return;
        }
        save(player, KingRules.AWAKE);
        player.m_5661_(Component.m_237115_("message.hexkagune.king.awake"), false);
        broadcast(player);
    }

    /** Forgets the crown (a command). */
    public static void reset(ServerPlayer player) {
        off(player, false);
        save(player, "");
        broadcast(player);
    }

    /** 1.0.17: true while {@code player}'s final kakuja is still coming over him (KingTransform): no hurt, no abilities yet. */
    public static boolean coming(ServerPlayer player) {
        Worn worn = worn(player);
        return worn != null && !worn.preview() && KingTransform.coming(worn.form(), worn.on(), player.m_9236_().m_46467_() - worn.since());
    }

    /**
     * 1.0.17: its coming, {@code age} ticks in, as the world hears it: War's heart pounding as the darkness gathers, then the burst
     * (the blast that throws back and blinds all near); Peace's light chiming, its wings beating, the soft light at the end. 1.2.2: in the
     * user's own pack, one sound to a beat (four or five of vanilla's were layered on some): War's awakening swelling under the heart,
     * the colossal impact and the blood of it at the burst; Peace revealed, its wings' air, its light.
     */
    static void coming(ServerPlayer player, KingForm form, long age) {
        net.minecraft.server.level.ServerLevel level = player.m_284548_();
        net.minecraft.world.phys.Vec3 at = new net.minecraft.world.phys.Vec3(player.m_20185_(), player.m_20186_() + 1.0, player.m_20189_());
        if (form == KingForm.WAR) {
            if (age == 0L) {
                UkakuFire.sound(level, at, "hexkagune:sfx.transform.overdrive_awakening", 2.0F, 0.9F);
            }
            if (age == 0L || age == 5L || age == 9L || age == 12L || age == 14L) {
                UkakuFire.sound(level, at, "entity.warden.heartbeat", 2.0F, 0.6F + age * 0.02F);
            }
            if (KingTransform.burstNow(form, age)) {
                UkakuFire.sound(level, at, "hexkagune:sfx.ultimate", 2.4F, 0.9F);
                UkakuFire.sound(level, at, "hexkagune:sfx.gush_big", 1.6F, 0.9F);
                blast(player, level, at);
            }
        } else if (form == KingForm.PEACE) {
            if (age == 0L) {
                UkakuFire.sound(level, at, "hexkagune:sfx.transform.spectral_reveal", 1.6F, 1.0F);
            }
            if (age == 12L || age == 22L) {
                UkakuFire.sound(level, at, "hexkagune:sfx.whoosh", 0.9F, 1.1F);
            }
            if (age == KingTransform.PEACE_LIGHT) {
                UkakuFire.sound(level, at, "hexkagune:sfx.transform.pressure_awakening", 1.4F, 1.1F);
            }
        }
    }

    /**
     * 1.0.19: its leaving, {@code age} ticks after it was taken off, as the world hears it: War dying on him (a last heartbeat, then
     * crumbling like rock, the last of it dripping away); Peace drawn back in (its wings folding, breaking as they wither, the light
     * going out of it). 1.2.2: the user's own pack.
     */
    static void going(ServerPlayer player, KingForm form, long age) {
        net.minecraft.server.level.ServerLevel level = player.m_284548_();
        net.minecraft.world.phys.Vec3 at = new net.minecraft.world.phys.Vec3(player.m_20185_(), player.m_20186_() + 1.0, player.m_20189_());
        if (form == KingForm.WAR) {
            if (age == 0L) {
                UkakuFire.sound(level, at, "entity.warden.heartbeat", 1.8F, 0.55F);
            }
            if (age == 3L) {
                UkakuFire.sound(level, at, "hexkagune:sfx.quake", 1.2F, 0.9F);
            }
            if (age == 8L || age == 16L) {
                UkakuFire.sound(level, at, "hexkagune:sfx.ground", 1.0F, 0.9F + age * 0.005F);
            }
            if (age == 22L) {
                UkakuFire.sound(level, at, "hexkagune:sfx.drips", 0.8F, 0.9F);
            }
        } else if (form == KingForm.PEACE) {
            if (age == 0L) {
                UkakuFire.sound(level, at, "hexkagune:sfx.whoosh", 0.8F, 0.95F);
            }
            if (age == 12L || age == 18L) {
                UkakuFire.sound(level, at, "hexkagune:sfx.shatter", 0.9F, age == 12L ? 1.0F : 1.1F);
            }
            if (age == 32L) {
                UkakuFire.sound(level, at, "hexkagune:sfx.transform.organic_armor_shift", 1.0F, 1.1F);
            }
        }
    }

    /** War's burst: everything near thrown back from him, harder the nearer, and blinded a moment. No hurt: it is his coming. */
    private static void blast(ServerPlayer player, net.minecraft.server.level.ServerLevel level, net.minecraft.world.phys.Vec3 at) {
        double r = KingTransform.WAR_BLAST;
        net.minecraft.world.phys.AABB area = new net.minecraft.world.phys.AABB(at.f_82479_ - r, at.f_82480_ - r, at.f_82481_ - r, at.f_82479_ + r,
            at.f_82480_ + r, at.f_82481_ + r);
        for (net.minecraft.world.entity.LivingEntity body : level.m_6443_(net.minecraft.world.entity.LivingEntity.class, area,
            e -> e != player && e.m_6084_() && com.hex.kagune.gameplay.KaguneServerController.validSwipeTarget(player, e))) {
            double dx = body.m_20185_() - player.m_20185_();
            double dz = body.m_20189_() - player.m_20189_();
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d > r) {
                continue;
            }
            double k = 1.6 * (1.0 - d / r) + 0.4;
            double nx = d < 1.0E-3 ? 0.0 : dx / d;
            double nz = d < 1.0E-3 ? 1.0 : dz / d;
            double steady = 1.0 - 0.75 * KingEffects.steady(body);
            body.m_20256_(new net.minecraft.world.phys.Vec3(nx * k * steady, 0.45 + 0.3 * (1.0 - d / r), nz * k * steady));
            body.f_19864_ = true;
            body.m_147207_(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.f_19610_, 50, 0, false, false, true), player);
        }
    }

    /** A request from the client ({@code C2SKingAction}). */
    public static void request(ServerPlayer player, int kind, int arg, int target) {
        if (kind == com.hex.kagune.control.network.C2SKingAction.CHOOSE) {
            // 1.0.11: chosen on the cards, it takes him there and then: his first transformation.
            if (choose(player, KingForm.byOrdinal(arg))) {
                toggle(player);
            }
        } else if (kind == com.hex.kagune.control.network.C2SKingAction.TOGGLE) {
            toggle(player);
        } else if (kind == com.hex.kagune.control.network.C2SKingAction.ABILITY) {
            Kit worn = kit(wearing(player));
            if (worn != null && player.m_6084_() && !coming(player)) {
                worn.key(player, arg >> 2, arg & 3, target);
            }
        } else if (kind == com.hex.kagune.control.network.C2SKingAction.STRIKE) {
            Kit worn = kit(wearing(player));
            if (worn != null && player.m_6084_() && !coming(player)) {
                worn.strike(player, target, arg);
            }
        } else if (kind == com.hex.kagune.control.network.C2SKingAction.HEAVY) {
            Kit worn = kit(wearing(player));
            if (worn != null && player.m_6084_() && !coming(player)) {
                worn.heavy(player, arg, target);
            }
        } else if (kind == com.hex.kagune.control.network.C2SKingAction.WYRM) {
            Kit worn = kit(wearing(player));
            if (worn != null && player.m_6084_()) {
                worn.drive(player, arg, target);
            }
        }
    }

    // ------------------------------------------------------------------ the kit's side

    /** The kit's body moves: {@code move} from now, at {@code level}, at {@code target} (0: none); every client draws it. */
    public static void move(ServerPlayer player, KingMove move, int level, int target) {
        MOTIONS.put(player.m_20148_(), move == KingMove.NONE ? Motion.IDLE : new Motion(move, player.m_9236_().m_46467_(), level, target));
        broadcast(player);
    }

    /** 1.0.7: rinkaku {@code tendril} of {@code player}'s War struck {@code target} on its own (a lash, or a spike fired). */
    public static void lash(ServerPlayer player, int tendril, int target, boolean spike) {
        ControlNetwork.broadcast(player, new com.hex.kagune.control.network.S2CKingLash(player.m_20148_(), tendril, target, spike));
    }

    /** The move {@code player}'s final kakuja is in, or NONE. */
    public static KingMove moving(ServerPlayer player) {
        return MOTIONS.getOrDefault(player.m_20148_(), Motion.IDLE).move();
    }

    /** 1.0.35: {@code player}'s waits for the final kakuja he wears (or chose), made if {@code make}; else null if there are none. */
    private static long[] waits(ServerPlayer player, boolean make) {
        KingForm form = wearing(player);
        if (form == KingForm.NONE) {
            form = chosen(player);
        }
        Map<KingForm, long[]> all = make ? READY.computeIfAbsent(player.m_20148_(), id -> new java.util.EnumMap<>(KingForm.class))
            : READY.get(player.m_20148_());
        if (all == null) {
            return null;
        }
        return make ? all.computeIfAbsent(form, f -> new long[S2CKingState.ABILITIES]) : all.get(form);
    }

    /** When ability {@code slot} of the final kakuja {@code player} wears is ready again (game ticks). */
    public static long ready(ServerPlayer player, int slot) {
        long[] ready = waits(player, false);
        return ready == null || slot < 0 || slot >= ready.length ? 0L : ready[slot];
    }

    /**
     * Ability {@code slot} is ready again at {@code at} (game ticks); the HUD shows it. (1.0.33: at once while cooldowns are off,
     * {@link Cooldowns#off}: switched off, or in creative; a move still cannot start over itself, its kit's busy.)
     */
    public static void cooldown(ServerPlayer player, int slot, long at) {
        long[] ready = waits(player, true);
        if (slot >= 0 && slot < ready.length) {
            ready[slot] = Cooldowns.off(player) ? Math.min(at, player.m_9236_().m_46467_()) : at;
            broadcast(player);
        }
    }

    /** 1.0.33: every ability of {@code player}'s final kakuja ready now. */
    static void clearCooldowns(ServerPlayer player) {
        Map<KingForm, long[]> all = READY.get(player.m_20148_());
        if (all != null) {
            for (long[] ready : all.values()) {
                java.util.Arrays.fill(ready, 0L);
            }
            broadcast(player);
        }
    }

    /** Chooses {@code form} for good: a Rinkaku ghoul the Dragon has been through, who has not chosen yet, and a form that is built. */
    public static boolean choose(ServerPlayer player, KingForm form) {
        if (PathServer.path(player) != KagunePath.RINKAKU || !awake(player)) {
            player.m_5661_(Component.m_237115_("message.hexkagune.king.not_awake"), true);
            return false;
        }
        if (chosen(player) != KingForm.NONE) {
            player.m_5661_(Component.m_237110_("message.hexkagune.king.already", chosen(player).displayName()), true);
            return false;
        }
        if (form == null || !form.built()) {
            player.m_5661_(Component.m_237115_("message.hexkagune.king.unbuilt"), true);
            return false;
        }
        save(player, KingRules.choose(saved(player), form));
        player.m_5661_(Component.m_237110_("message.hexkagune.king.chosen", form.displayName()), false);
        broadcast(player);
        return true;
    }

    // ------------------------------------------------------------------ wearing it

    /** What {@code player} wears now, or null. */
    public static Worn worn(ServerPlayer player) {
        return WORN.get(player.m_20148_());
    }

    /** The final kakuja {@code player} truly wears (on, not the preview), or NONE: what abilities and stats go by. */
    public static KingForm wearing(ServerPlayer player) {
        Worn worn = worn(player);
        return worn != null && worn.on() && !worn.preview() ? worn.form() : KingForm.NONE;
    }

    /** {@code DragonLock}: a final kakuja has the body of the player with this id. */
    public static boolean locks(UUID id) {
        Worn worn = WORN.get(id);
        return worn != null && worn.on() && !worn.preview();
    }

    /** The key: on if off, off if on. */
    public static void toggle(ServerPlayer player) {
        Worn worn = worn(player);
        if (worn != null && worn.on()) {
            off(player, true);
            return;
        }
        KingForm form = chosen(player);
        if (form == KingForm.NONE || !form.built()) {
            player.m_5661_(Component.m_237115_(awake(player) ? "message.hexkagune.king.choose_first" : "message.hexkagune.king.not_awake"), true);
            return;
        }
        if (PathServer.path(player) != KagunePath.RINKAKU || !player.m_6084_()) {
            return;
        }
        // 1.0.23: it runs on RC, as every form does: not while spent, not on an empty bar.
        int spent = KaguneLock.locked(player);
        if (spent > 0) {
            player.m_5661_(Component.m_237110_("message.hexkagune.kagune_spent_wait", spent), true);
            return;
        }
        if (KaguneProfile.energy(player) < KingRules.CALL_ENERGY) {
            player.m_5661_(Component.m_237115_("message.hexkagune.low_energy"), true);
            return;
        }
        long now = player.m_9236_().m_46467_();
        if (KaguneProfile.isActive(player)) {
            PathServer.toggle(player); // the kagune goes in first: the final kakuja has the body to itself
            if (KaguneProfile.isActive(player)) {
                // 1.0.22: it is still going in (its own animation): it comes over him once it is in (playerTick), not never.
                PENDING.put(player.m_20148_(), now + PENDING_TICKS);
                return;
            }
        }
        PENDING.remove(player.m_20148_());
        WORN.put(player.m_20148_(), new Worn(form, true, now, false));
        boost(player, form);
        player.m_284548_().m_6263_(null, player.m_20185_(), player.m_146892_().f_82480_, player.m_20189_(), EYE_OPEN, SoundSource.PLAYERS, 1.4F,
            0.55F);
        broadcast(player);
    }

    /** Takes it off ({@code shown}: it draws back over him; else it is simply gone). */
    public static void off(ServerPlayer player, boolean shown) {
        PENDING.remove(player.m_20148_());
        Worn worn = WORN.get(player.m_20148_());
        unboost(player);
        Kit kit = worn == null ? null : kit(worn.form());
        if (kit != null && worn.on() && !worn.preview()) {
            kit.off(player);
        }
        MOTIONS.remove(player.m_20148_());
        if (worn == null) {
            return;
        }
        if (shown && worn.on()) {
            WORN.put(player.m_20148_(), new Worn(worn.form(), false, player.m_9236_().m_46467_(), worn.preview()));
        } else {
            WORN.remove(player.m_20148_());
        }
        broadcast(player);
    }

    /** The admin preview: the look of {@code form} on {@code player} (NONE takes it off). No stats, no lock. */
    public static void preview(ServerPlayer player, KingForm form) {
        if (form == null || form == KingForm.NONE) {
            off(player, true);
            return;
        }
        Worn worn = worn(player);
        if (worn != null && worn.on() && !worn.preview()) {
            off(player, false);
        }
        WORN.put(player.m_20148_(), new Worn(form, true, player.m_9236_().m_46467_(), true));
        broadcast(player);
    }

    private static void boost(ServerPlayer player, KingForm form) {
        double[] boosts = KingRules.boosts(form);
        modifier(player, Attributes.f_22281_, MELEE, "Final kakuja melee", boosts[0]);
        modifier(player, Attributes.f_22279_, SPEED, "Final kakuja speed", boosts[1]);
        modifier(player, Attributes.f_22283_, ATTACK_SPEED, "Final kakuja attack speed", boosts[2]);
        modifier(player, Attributes.f_22276_, HEALTH, "Final kakuja health", boosts[3]);
        if (form == KingForm.WAR) {
            AttributeInstance rooted = player.m_21051_(Attributes.f_22278_);
            if (rooted != null && rooted.m_22111_(ROOTED) == null) {
                rooted.m_22118_(new AttributeModifier(ROOTED, "War is not moved", 1.0, AttributeModifier.Operation.ADDITION));
            }
        }
    }

    /** 1.0.26: sprinting in a final kakuja, he is far faster: its sprint's speed on while he sprints, off when he stops. */
    private static void sprint(ServerPlayer player, KingForm form, boolean on) {
        AttributeInstance speed = player.m_21051_(Attributes.f_22279_);
        if (speed == null) {
            return;
        }
        boolean has = speed.m_22111_(SPRINT) != null;
        if (on && !has) {
            speed.m_22118_(new AttributeModifier(SPRINT, "Final kakuja sprint", KingRules.sprint(form), AttributeModifier.Operation.MULTIPLY_BASE));
        } else if (!on && has) {
            speed.m_22120_(SPRINT);
        }
    }

    private static void modifier(ServerPlayer player, Attribute attribute, UUID id, String name, double amount) {
        AttributeInstance instance = player.m_21051_(attribute);
        if (instance == null || amount == 0.0 || instance.m_22111_(id) != null) {
            return;
        }
        instance.m_22118_(new AttributeModifier(id, name, amount, AttributeModifier.Operation.MULTIPLY_BASE));
    }

    private static void unboost(ServerPlayer player) {
        Attribute[] attributes = {Attributes.f_22281_, Attributes.f_22279_, Attributes.f_22283_, Attributes.f_22276_, Attributes.f_22278_, Attributes.f_22279_};
        UUID[] ids = {MELEE, SPEED, ATTACK_SPEED, HEALTH, ROOTED, SPRINT};
        for (int i = 0; i < ids.length; i++) {
            AttributeInstance instance = player.m_21051_(attributes[i]);
            if (instance != null && instance.m_22111_(ids[i]) != null) {
                instance.m_22120_(ids[i]);
            }
        }
        if (player.m_21223_() > player.m_21233_()) {
            player.m_21153_(player.m_21233_());
        }
    }

    // ------------------------------------------------------------------ sync

    static S2CKingState snapshot(ServerPlayer player) {
        Worn worn = worn(player);
        int crown = awake(player) ? chosen(player).ordinal() : -1;
        Motion motion = MOTIONS.getOrDefault(player.m_20148_(), Motion.IDLE);
        long[] waits = waits(player, false);
        long[] ready = waits == null ? new long[S2CKingState.ABILITIES] : waits.clone();
        return worn == null ? new S2CKingState(player.m_20148_(), crown, KingForm.NONE.ordinal(), false, 0L, false, 0, 0L, 0, 0, ready)
            : new S2CKingState(player.m_20148_(), crown, worn.form().ordinal(), worn.on(), worn.since(), worn.preview(), motion.move().ordinal(),
                motion.at(), motion.level(), motion.target(), ready);
    }

    static void broadcast(ServerPlayer player) {
        ControlNetwork.broadcast(player, snapshot(player));
    }

    // ------------------------------------------------------------------ events

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        Worn worn = WORN.get(player.m_20148_());
        long now = player.m_9236_().m_46467_();
        Long pending = PENDING.get(player.m_20148_());
        if (pending != null && (worn == null || !worn.on())) {
            if (now > pending || !player.m_6084_()) {
                PENDING.remove(player.m_20148_());
            } else if (!KaguneProfile.isActive(player)) {
                PENDING.remove(player.m_20148_());
                toggle(player); // 1.0.22: the kagune is in: now it comes over him
                return;
            }
        }
        if (worn == null) {
            return;
        }
        if (!worn.on()) {
            long gone = now - worn.since();
            if (gone >= 0L && gone < KingTransform.leave(worn.form())) {
                going(player, worn.form(), gone);
            }
            if (gone > Math.max(KingRules.GROW_TICKS * 2L, KingTransform.leave(worn.form()) + 12L)) {
                WORN.remove(player.m_20148_()); // gone back into him: nothing left to draw
            }
            return;
        }
        if (worn.preview()) {
            return;
        }
        if (PathServer.path(player) != KagunePath.RINKAKU || chosen(player) != worn.form()) {
            off(player, true);
            return;
        }
        if (!KaguneProfile.consumeEnergy(player, KingRules.UPKEEP)) {
            // 1.0.23: run dry, it falls away from him (shown), and nothing comes out again for a while, as with a spent kagune.
            KaguneProfile.setEnergy(player, 0.0F);
            off(player, true);
            KaguneLock.spend(player);
            player.m_5661_(Component.m_237110_("message.hexkagune.king.spent", worn.form().displayName(), KaguneLock.TICKS / 20), true);
            KaguneNetwork.sendTo(player, player);
            return;
        }
        if (now % 10L == 0L) {
            KaguneNetwork.sendTo(player, player); // the RC bar going down
        }
        if (now % 20L == 0L) {
            boost(player, worn.form());
        }
        sprint(player, worn.form(), player.m_20142_() && !coming(player)); // 1.0.26
        long age = now - worn.since();
        if (age >= 0L && age < KingTransform.ticks(worn.form())) {
            coming(player, worn.form(), age);
        }
        Kit kit = kit(worn.form());
        if (kit != null) {
            kit.tick(player, now);
            player.f_19789_ = 0.0F; // 1.0.7: no fall hurts War (1.0.15: nor Peace, on its wings)
        }
        if (now % MEND_TICKS == 0L && player.m_21223_() < player.m_21233_() && player.m_6084_()) {
            player.m_5634_(MEND);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void hurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getAmount() > 0.0F) {
            if (coming(player)) {
                event.setCanceled(true); // 1.0.17: nothing touches him while it comes over him
                return;
            }
            KingForm form = wearing(player);
            float amount = event.getAmount();
            // 1.0.15: whoever is hurt, a Peace near may be sheltering them behind its wall.
            Kit peace = KITS.get(KingForm.PEACE);
            if (peace != null && form != KingForm.PEACE) {
                amount = peace.cover(player, event.getSource().m_7639_(), event.getSource().m_7640_(), amount);
            }
            if (form != KingForm.NONE) {
                Kit kit = kit(form);
                if (kit != null) {
                    amount = kit.hurt(player, event.getSource().m_7639_(), event.getSource().m_7640_(), amount);
                }
                amount = (float)(amount * (1.0 - KingRules.reduction(form)));
            }
            event.setAmount(amount);
        }
    }

    @SubscribeEvent
    public static void died(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && WORN.containsKey(player.m_20148_())) {
            off(player, false);
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && WORN.containsKey(player.m_20148_())) {
            off(player, false);
        }
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            unboost(player);
            WORN.remove(player.m_20148_());
            broadcast(player);
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            WORN.remove(player.m_20148_());
            unboost(player);
            broadcast(player);
        }
    }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof ServerPlayer subject && WORN.containsKey(subject.m_20148_())) {
            ControlNetwork.sendTo(viewer, snapshot(subject));
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            off(player, false);
            READY.remove(player.m_20148_());
            PENDING.remove(player.m_20148_());
            unboost(player);
            WORN.remove(player.m_20148_());
        }
    }
}
