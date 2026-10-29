package com.hex.kagune.control.server;

import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CAyatoFx;
import com.hex.kagune.control.wound.WoundBook;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 5.0.17: every hit on a player wounds him where it landed, the way it was dealt. Before, only Kaneki's tails
 * ({@link KanekiWounds}) and Ayato's spikes said where they struck; every other loss of health tore wounds open anywhere. Now
 * each hit is read as the server sees it ({@link #hurt}): a sword's gash where the swing met him, an arrow's hole where it went
 * in, a fist's bruise, a beast's claws, an explosion tearing the side that faced it, a fall breaking the shins, fire burning
 * where it touched, and nothing for poison, hunger, drowning or bleeding. Every client near him is told
 * ({@link S2CAyatoFx#HIT}), and the health it costs opens that wound there ({@link WoundBook#blow}). The mod's own moves say
 * exactly where they struck before they deal their damage ({@link #expect}).
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class WoundHits {
    /** Damage that leaves nothing to see (by its message id). */
    private static final Set<String> INTERNAL = Set.of("magic", "indirectMagic", "wither", "starve", "drown", "freeze", "outOfWorld", "genericKill",
        "generic", "dryout", "inWall", "cramming", "sonic_boom", "dragonBreath", "fellOutOfWorld");
    private static final Set<String> BURNS = Set.of("inFire", "onFire", "lava", "hotFloor", "lightningBolt", "campfire");
    private static final Set<String> BLASTS = Set.of("explosion", "explosion.player", "badRespawnPoint", "fireworks", "witherSkull");
    private static final Set<String> FALLS = Set.of("fall", "stalagmite");
    private static final Set<String> SCRATCHES = Set.of("cactus", "sweetBerryBush", "thorns");
    private static final Set<String> FROM_ABOVE = Set.of("anvil", "fallingBlock", "fallingStalactite");
    /** Mobs that hit this hard with their bare hands crush (an iron golem, a ravager, a warden). */
    private static final float CRUSHING = 9.0F;

    /** A hit the mod is about to deal: where, which way into the body, how. */
    private record Hit(WoundBook.Blow blow, Vec3 at, Vec3 into, double angle) {
    }

    /** The one hit the mod said it is about to deal, on whom and when (it lands, or not, within the same call). */
    private static Hit expected;
    private static int expectedTarget;
    private static long expectedAt = Long.MIN_VALUE;

    private WoundHits() {
    }

    /**
     * One of the mod's own moves is about to hurt {@code target}: it struck at {@code at} going {@code into} the body, a
     * {@code blow} (a slash at {@code angle}). Called right before the damage.
     */
    static void expect(LivingEntity target, WoundBook.Blow blow, Vec3 at, Vec3 into, double angle) {
        expected = new Hit(blow, at, into, angle);
        expectedTarget = target.m_19879_();
        expectedAt = target.m_9236_().m_46467_();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void hurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim) || event.getAmount() <= 0.0F || KanekiWounds.tails(victim, event.getSource())) {
            return;
        }
        long now = victim.m_9236_().m_46467_();
        Hit hit = expected != null && expectedTarget == victim.m_19879_() && expectedAt == now ? expected : read(victim, event.getSource(),
            event.getAmount());
        expected = null;
        if (hit == null) {
            return; // nothing known about it: the clients place it themselves
        }
        Entity attacker = event.getSource().m_7639_();
        float power = Math.max(0.4F, Math.min(2.2F, event.getAmount() / 5.0F));
        int degrees = (int)Math.round(Math.toDegrees(hit.angle())) + 180;
        int extra = hit.blow().ordinal() + 16 * Math.max(0, Math.min(360, degrees));
        Vec3 at = hit.at() == null ? victim.m_20182_() : hit.at();
        Vec3 into = hit.into() == null ? Vec3.f_82478_ : hit.into();
        ControlNetwork.broadcastAround(victim, new S2CAyatoFx(S2CAyatoFx.HIT, attacker == null ? victim.m_19879_() : attacker.m_19879_(),
            victim.m_19879_(), at.f_82479_, at.f_82480_, at.f_82481_, (float)into.f_82479_, (float)into.f_82480_, (float)into.f_82481_, power, extra));
    }

    /** What a hit is, from its source: where it landed and how. Null when nothing tells. */
    static Hit read(ServerPlayer victim, DamageSource source, float amount) {
        String id = source.m_19385_();
        Entity attacker = source.m_7639_();
        Entity direct = source.m_7640_();
        Vec3 centre = victim.m_20182_().m_82520_(0.0, victim.m_20206_() * 0.55, 0.0);
        double swing = (victim.m_217043_().m_188500_() - 0.5) * 1.4;
        if (id == null || INTERNAL.contains(id) || id.contains("bleed") || id.contains("poison") || id.contains("starv")) {
            return new Hit(WoundBook.Blow.INTERNAL, null, null, 0.0);
        }
        if (FALLS.contains(id)) {
            return new Hit(WoundBook.Blow.FALL, victim.m_20182_().m_82520_(0.0, 0.3, 0.0), new Vec3(0.0, -1.0, 0.0), 0.0);
        }
        if (SCRATCHES.contains(id)) {
            Vec3 way = sideways(victim);
            return new Hit(WoundBook.Blow.SCRATCH, victim.m_20182_().m_82520_(0.0, 0.4 + victim.m_217043_().m_188500_() * 1.0, 0.0).m_82546_(
                way.m_82490_(0.3)), way, swing);
        }
        if (BURNS.contains(id)) {
            if ("lava".equals(id) || "hotFloor".equals(id) || "inFire".equals(id) || "campfire".equals(id)) {
                // Standing in it: the legs.
                Vec3 way = sideways(victim);
                return new Hit(WoundBook.Blow.BURN, victim.m_20182_().m_82520_(0.0, 0.35, 0.0).m_82546_(way.m_82490_(0.3)), way, 0.0);
            }
            return new Hit(WoundBook.Blow.BURN, null, null, 0.0); // alight: anywhere
        }
        if (FROM_ABOVE.contains(id)) {
            return new Hit(WoundBook.Blow.BLUNT, victim.m_146892_().m_82520_(0.0, 0.2, 0.0), new Vec3(0.0, -1.0, 0.0), 0.0);
        }
        if (BLASTS.contains(id)) {
            Vec3 from = source.m_7270_();
            Vec3 into = from == null ? sideways(victim) : centre.m_82546_(from);
            into = into.m_82556_() < 1.0E-6 ? sideways(victim) : into.m_82541_();
            return new Hit(WoundBook.Blow.BLAST, centre.m_82546_(into.m_82490_(0.3)), into, 0.0);
        }
        if (direct != null && direct != attacker) {
            // A projectile: where it is now, the way it flew.
            Vec3 at = direct.m_20182_().m_82520_(0.0, direct.m_20206_() * 0.5, 0.0);
            Vec3 into = direct.m_20184_();
            if (into.m_82556_() < 1.0E-6) {
                into = centre.m_82546_(at);
            }
            into = into.m_82556_() < 1.0E-6 ? sideways(victim) : into.m_82541_();
            WoundBook.Blow blow = "fireball".equals(id) ? WoundBook.Blow.BURN : "thrown".equals(id) ? WoundBook.Blow.BLUNT : WoundBook.Blow.STAB;
            return new Hit(blow, onBody(victim, at, into), into, 0.0);
        }
        if (attacker instanceof LivingEntity hitter) {
            // A blow in hand: where the hitter's aim meets the body.
            Vec3 eye = ((Entity)hitter).m_146892_();
            Vec3 look = ((Entity)hitter).m_20154_();
            Vec3 at = ray(victim.m_20191_(), eye, look);
            Vec3 into = at == null ? centre.m_82546_(eye) : look;
            into = into.m_82556_() < 1.0E-6 ? sideways(victim) : into.m_82541_();
            if (at == null) {
                at = onBody(victim, new Vec3(centre.f_82479_, Math.max(victim.m_20186_() + 0.2, Math.min(victim.m_20186_() + victim.m_20206_() - 0.1,
                    eye.f_82480_)), centre.f_82481_).m_82546_(into.m_82490_(2.0)), into);
            }
            ItemStack held = hitter.m_21205_();
            Item item = held == null || held.m_41619_() ? null : held.m_41720_();
            WoundBook.Blow blow;
            if (item instanceof SwordItem || item instanceof AxeItem) {
                blow = WoundBook.Blow.SLASH;
            } else if (item != null || hitter instanceof Player) {
                blow = WoundBook.Blow.BLUNT;
            } else {
                blow = amount >= CRUSHING ? WoundBook.Blow.BLUNT : WoundBook.Blow.MAUL;
            }
            return new Hit(blow, at, into, swing);
        }
        Vec3 from = source.m_7270_();
        if (from != null) {
            Vec3 into = centre.m_82546_(from);
            into = into.m_82556_() < 1.0E-6 ? sideways(victim) : into.m_82541_();
            return new Hit(WoundBook.Blow.BLUNT, centre.m_82546_(into.m_82490_(0.3)), into, swing);
        }
        return null;
    }

    /** A way into the body from one of its sides at random (level). */
    static Vec3 sideways(ServerPlayer victim) {
        double a = victim.m_217043_().m_188500_() * Math.PI * 2.0;
        return new Vec3(Math.sin(a), 0.0, Math.cos(a));
    }

    /** Where a line from {@code from} along {@code way} first meets {@code box}, or null if it misses (a slab test). */
    static Vec3 ray(AABB box, Vec3 from, Vec3 way) {
        double[] lo = {box.f_82288_, box.f_82289_, box.f_82290_};
        double[] hi = {box.f_82291_, box.f_82292_, box.f_82293_};
        double[] o = {from.f_82479_, from.f_82480_, from.f_82481_};
        double[] d = {way.f_82479_, way.f_82480_, way.f_82481_};
        double t0 = 0.0;
        double t1 = 16.0;
        for (int i = 0; i < 3; i++) {
            if (Math.abs(d[i]) < 1.0E-9) {
                if (o[i] < lo[i] || o[i] > hi[i]) {
                    return null;
                }
                continue;
            }
            double a = (lo[i] - o[i]) / d[i];
            double b = (hi[i] - o[i]) / d[i];
            t0 = Math.max(t0, Math.min(a, b));
            t1 = Math.min(t1, Math.max(a, b));
            if (t0 > t1) {
                return null;
            }
        }
        return new Vec3(o[0] + d[0] * t0, o[1] + d[1] * t0, o[2] + d[2] * t0);
    }

    /** Where something at {@code at} going {@code into} meets the body (its point, or the nearest on the box if it's off it). */
    static Vec3 onBody(ServerPlayer victim, Vec3 at, Vec3 into) {
        Vec3 met = ray(victim.m_20191_(), at.m_82546_(into.m_82490_(2.0)), into);
        if (met != null) {
            return met;
        }
        AABB box = victim.m_20191_();
        return new Vec3(Math.max(box.f_82288_, Math.min(box.f_82291_, at.f_82479_)), Math.max(box.f_82289_, Math.min(box.f_82292_, at.f_82480_)),
            Math.max(box.f_82290_, Math.min(box.f_82293_, at.f_82481_)));
    }
}
