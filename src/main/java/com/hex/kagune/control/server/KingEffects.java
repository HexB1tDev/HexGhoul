package com.hex.kagune.control.server;

import com.hex.kagune.effect.KaguneEffects;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * 1.0.6: what War's blows leave on a body, for its kit ({@code control.awakening.WarKit}, which can't name these itself): the stun
 * every path's stun is ({@code PaleStun}: renewed, capped, and a player held too long breaks free), the mod's bleeding, a slowing.
 */
public final class KingEffects {
    private KingEffects() {
    }

    /** Stunned for {@code ticks}; false if it broke free or can't be stunned yet. */
    public static boolean stun(LivingEntity body, int ticks) {
        return PaleStun.stun(body, body.m_9236_().m_46467_(), ticks);
    }

    /** 1.0.20: a guard broken: a raised shield knocked aside and down for a while (as an axe does). */
    public static void breakGuard(LivingEntity body) {
        if (body instanceof net.minecraft.world.entity.player.Player held && held.m_21254_()) {
            held.m_36384_(true);
        }
    }

    /** 1.0.16: true if {@code body} stands on something. */
    public static boolean grounded(Entity body) {
        return body.m_20096_();
    }

    /** 1.0.16: what eats at {@code body} from within taken off it: bleeding, poison, wither. */
    public static void cleanse(LivingEntity body) {
        body.m_21195_(KaguneEffects.BLEEDING.get());
        body.m_21195_(MobEffects.f_19614_);
        body.m_21195_(MobEffects.f_19615_);
    }

    private static final UUID BRACED = UUID.fromString("5e2b8b0c-7d55-4f1e-9a0b-1c0de5a10006");

    /** 1.0.16: {@code player} braced against every throw (knockback resistance to the full), or not. */
    public static void brace(ServerPlayer player, boolean on) {
        AttributeInstance resist = player.m_21051_(Attributes.f_22278_);
        if (resist == null) {
            return;
        }
        boolean has = resist.m_22111_(BRACED) != null;
        if (on && !has) {
            resist.m_22118_(new AttributeModifier(BRACED, "Peace braced", 1.0, AttributeModifier.Operation.ADDITION));
        } else if (!on && has) {
            resist.m_22120_(BRACED);
        }
    }

    /**
     * 1.0.16: {@code amount} made as much again as {@code body}'s armour would take off it (vanilla's armour, no toughness), so that
     * much goes through: Peace's blade straight through armour.
     */
    public static float throughArmour(LivingEntity body, float amount) {
        double armour = body.m_21230_();
        double taken = Math.min(20.0, Math.max(armour / 5.0, armour - amount / 2.0)) / 25.0;
        return (float)(amount / Math.max(0.25, 1.0 - taken));
    }

    /** 1.0.14: how much of a throw {@code body} shrugs off: its knockback resistance, 0..1. */
    public static double steady(LivingEntity body) {
        return Math.max(0.0, Math.min(1.0, body.m_21133_(Attributes.f_22278_)));
    }

    /**
     * 1.0.27: {@code body} thrown off its feet along {@code launch} by {@code by}: on every client it tumbles as the Speed Kick's do
     * (the body twisted, tipped over the way it flies, lying where it comes down, getting up: {@code KickTumble}). Only how it is drawn.
     */
    public static void tumble(ServerPlayer by, LivingEntity body, net.minecraft.world.phys.Vec3 launch) {
        PaleBlitz.fx(by, com.hex.kagune.control.network.S2CPaleFx.KICKED, body.m_19879_(), body.m_20182_(), launch, 1.0F, 0);
    }

    /**
     * 1.0.34: whether {@code body} is hostile to {@code player} (what Peace's flesh and shields go for unbidden): a monster, something
     * out for him or that he is fighting, or what the original counts so.
     */
    public static boolean hostile(ServerPlayer player, LivingEntity body) {
        if (body == player || !body.m_6084_() || body.m_5833_()) {
            return false;
        }
        return body instanceof net.minecraft.world.entity.monster.Enemy || com.hex.kagune.gameplay.KaguneServerController.isHostileTo(player, body)
            || player.m_21188_() == body || body.m_21188_() == player;
    }

    /** Bleeding at {@code amplifier} for {@code ticks}. */
    public static void bleed(LivingEntity body, ServerPlayer by, int ticks, int amplifier) {
        body.m_147207_(new MobEffectInstance(KaguneEffects.BLEEDING.get(), ticks, amplifier, false, false, true), by);
    }

    /** 1.0.7: War drinks from what it cuts: {@code amount} of health back. */
    public static void heal(ServerPlayer player, float amount) {
        if (amount > 0.0F && player.m_6084_()) {
            player.m_5634_(amount);
        }
    }

    /** Slowed at {@code amplifier} for {@code ticks}. */
    /** 1.0.16: Peace's glide on his wings, as his own client falls (no icon); {@code ticks} 0 ends it. */
    public static void glide(ServerPlayer player, int ticks) {
        if (ticks <= 0) {
            player.m_21195_(MobEffects.f_19591_);
            return;
        }
        player.m_147207_(new MobEffectInstance(MobEffects.f_19591_, ticks, 0, false, false, false), player);
    }

    public static void slow(LivingEntity body, ServerPlayer by, int ticks, int amplifier) {
        body.m_147207_(new MobEffectInstance(MobEffects.f_19597_, ticks, amplifier, false, false, true), by);
    }
}
