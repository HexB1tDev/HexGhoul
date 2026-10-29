package com.hex.kagune.control;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/**
 * {@code serverconfig/hexkagune-server.toml} (per world, synced to clients) and
 * {@code config/hexkagune-client.toml}. Every getter falls back to its default if the config is
 * not loaded yet, so gameplay code never throws because of configuration timing.
 */
public final class ControlConfig {
    public static final ForgeConfigSpec SERVER_SPEC;
    public static final ForgeConfigSpec CLIENT_SPEC;

    private static final ForgeConfigSpec.BooleanValue GHOUL_DIET;
    private static final ForgeConfigSpec.DoubleValue MANIFEST_HUNGER_DRAIN;
    private static final ForgeConfigSpec.BooleanValue INVOLUNTARY_ACTIONS;
    private static final ForgeConfigSpec.BooleanValue INVOLUNTARY_ATTACKS;
    private static final ForgeConfigSpec.DoubleValue INSTINCT_STRENGTH;
    private static final ForgeConfigSpec.BooleanValue KAGUNE_COMBOS;
    private static final ForgeConfigSpec.BooleanValue BLOCK_SMASH;
    private static final ForgeConfigSpec.DoubleValue BLOCK_SMASH_RESTORE_SECONDS;
    private static final ForgeConfigSpec.DoubleValue TENDRIL_GUARD_STRENGTH;
    private static final ForgeConfigSpec.DoubleValue TENDRIL_GUARD_MAX;

    private static final ForgeConfigSpec.BooleanValue BODY_ANIMATIONS;
    private static final ForgeConfigSpec.BooleanValue BLOODLUST_OVERLAY;
    private static final ForgeConfigSpec.DoubleValue OVERLAY_OPACITY;
    private static final ForgeConfigSpec.BooleanValue CAMERA_EFFECTS;
    private static final ForgeConfigSpec.DoubleValue CAMERA_EFFECT_STRENGTH;
    private static final ForgeConfigSpec.BooleanValue URGE_VIEW_PULL;
    private static final ForgeConfigSpec.BooleanValue CONTROL_INDICATOR;
    private static final ForgeConfigSpec.BooleanValue ABILITY_HUD;

    static {
        ForgeConfigSpec.Builder server = new ForgeConfigSpec.Builder();
        server.push("ghoul");
        GHOUL_DIET = server
            .comment("Ghouls (players with HexKagune unlocked) are only nourished by raw flesh.",
                "Ordinary food is rejected: it gives no nourishment and causes a particle-free Hunger effect.",
                "Item tags hexkagune:ghoul_food, hexkagune:ghoul_tolerated and hexkagune:not_ghoul_food override detection.")
            .define("ghoulDiet", true);
        MANIFEST_HUNGER_DRAIN = server
            .comment("Multiplier for the food exhaustion caused by keeping a kagune manifested. 0 disables it.")
            .defineInRange("manifestHungerDrain", 1.0, 0.0, 4.0);
        server.pop();
        server.push("instinct");
        INVOLUNTARY_ACTIONS = server
            .comment("Untrained, starving or frenzied ghouls occasionally act on kagune instinct (short glances, steps, lunges, strikes).",
                "Crouching restrains an urge. Urges fade with mastery and vanish near full control.")
            .define("involuntaryActions", true);
        INVOLUNTARY_ATTACKS = server
            .comment("Allow instinct to force a kagune strike at hostile mobs or hunted animals. Never targets players or villagers.")
            .define("involuntaryAttacks", true);
        INSTINCT_STRENGTH = server
            .comment("Multiplier on how often instinct breaks through.")
            .defineInRange("instinctStrength", 1.0, 0.0, 2.0);
        server.pop();
        server.push("combat");
        KAGUNE_COMBOS = server
            .comment("Form-specific M1 combo chains with authoritative multi-target hit shapes. false restores the original single swipe.")
            .define("kaguneCombos", true);
        TENDRIL_GUARD_STRENGTH = server
            .comment("Auto Projectile (Kaneki): how much damage each tentacle's guard holds before it breaks, as a multiplier",
                "(1 = 6 at mastery 0 to 12 at 1000, half as much again with the Projectile Reflex Organ). 0 turns the guard off.")
            .defineInRange("tendrilGuardStrength", 1.0, 0.0, 4.0);
        TENDRIL_GUARD_MAX = server
            .comment("Auto Projectile (Kaneki): how many tentacles may guard at once, each against shots from its own direction.")
            .defineInRange("tendrilGuardMax", 2.0, 1.0, 8.0);
        BLOCK_SMASH = server
            .comment("Owl DNA: bodies her blows launch smash through weak blocks (dirt, sand, wood, glass, leaves: nothing that needs a pickaxe),",
                "which grow back exactly as they were. Claims and spawn protection are respected. false: they only slam into them.")
            .define("owlBlockSmash", true);
        BLOCK_SMASH_RESTORE_SECONDS = server
            .comment("Owl DNA: seconds before a smashed block grows back (never over anything built in its place since).")
            .defineInRange("owlBlockSmashRestoreSeconds", 30.0, 5.0, 600.0);
        server.pop();
        SERVER_SPEC = server.build();

        ForgeConfigSpec.Builder client = new ForgeConfigSpec.Builder();
        client.push("visuals");
        BODY_ANIMATIONS = client
            .comment("Mastery-driven body animation while a kagune is manifested (posture, struggle, combat poses).")
            .define("bodyAnimations", true);
        BLOODLUST_OVERLAY = client
            .comment("Pulsing red veins at the edge of the screen while bloodlust is high.")
            .define("bloodlustOverlay", true);
        OVERLAY_OPACITY = client
            .comment("Opacity multiplier for the bloodlust overlay.")
            .defineInRange("overlayOpacity", 1.0, 0.0, 1.0);
        CAMERA_EFFECTS = client
            .comment("First-person camera shake, roll and strike kick. Also scaled by Minecraft's Distortion Effects option.")
            .define("cameraEffects", true);
        CAMERA_EFFECT_STRENGTH = client
            .comment("Multiplier for first-person camera effects.")
            .defineInRange("cameraEffectStrength", 1.0, 0.0, 1.5);
        URGE_VIEW_PULL = client
            .comment("Let instinct urges pull your view toward prey. Disable for motion comfort; server-side urges are unchanged.")
            .define("urgeViewPull", true);
        CONTROL_INDICATOR = client
            .comment("Thin kagune-control bar under the RC bar: crimson when the kagune controls you, bone-white when you control it.")
            .define("controlIndicator", true);
        ABILITY_HUD = client
            .comment("A small panel above the chat listing your kagune's moves with the keys you bound to them, and how to open the eyes",
                "(R) and unleash the kagune (Alt + R). Its moves and colours follow your path and form. Hidden while a screen is open.")
            .define("abilityHud", true);
        client.pop();
        CLIENT_SPEC = client.build();
    }

    private ControlConfig() {
    }

    public static void register() {
        ModLoadingContext context = ModLoadingContext.get();
        context.registerConfig(ModConfig.Type.SERVER, SERVER_SPEC, "hexkagune-server.toml");
        context.registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC, "hexkagune-client.toml");
    }

    private static boolean bool(ForgeConfigSpec.BooleanValue value, boolean fallback) {
        try {
            return value.get();
        } catch (Throwable notLoaded) {
            return fallback;
        }
    }

    private static double number(ForgeConfigSpec.DoubleValue value, double fallback) {
        try {
            return value.get();
        } catch (Throwable notLoaded) {
            return fallback;
        }
    }

    /** 6.1.0: launched bodies smash through weak blocks ({@code BlockSmash}). */
    public static boolean blockSmash() {
        return bool(BLOCK_SMASH, true);
    }

    /** 6.1.0: seconds before a smashed block grows back. */
    public static long blockSmashRestoreSeconds() {
        return Math.round(number(BLOCK_SMASH_RESTORE_SECONDS, 30.0));
    }

    public static boolean ghoulDiet() {
        return bool(GHOUL_DIET, true);
    }

    public static float manifestHungerDrain() {
        return (float)number(MANIFEST_HUNGER_DRAIN, 1.0);
    }

    public static boolean involuntaryActions() {
        return bool(INVOLUNTARY_ACTIONS, true);
    }

    public static boolean involuntaryAttacks() {
        return bool(INVOLUNTARY_ATTACKS, true);
    }

    public static float instinctStrength() {
        return (float)number(INSTINCT_STRENGTH, 1.0);
    }

    /** 5.0.14: Auto Projectile's guard strength (multiplier on {@code TendrilShield.guardHealth}; 0: no guard). */
    public static double tendrilGuardStrength() {
        return number(TENDRIL_GUARD_STRENGTH, 1.0);
    }

    /** 5.0.14: how many tentacles may guard at once. */
    public static int tendrilGuardMax() {
        return (int)Math.round(number(TENDRIL_GUARD_MAX, TendrilShield.MAX_GUARDS));
    }

    public static boolean kaguneCombos() {
        return bool(KAGUNE_COMBOS, true);
    }

    public static boolean bodyAnimations() {
        return bool(BODY_ANIMATIONS, true);
    }

    /** The abilities board shown, unless switched off (in the config or by {@code /kagune hud toggle} this session). */
    public static boolean abilityHud() {
        return hudOverride != null ? hudOverride : bool(ABILITY_HUD, true);
    }

    private static Boolean hudOverride;

    /** {@code /kagune hud toggle}: the board on or off, saved to the client config (kept for this session if that can't be). */
    public static boolean toggleAbilityHud() {
        boolean on = !abilityHud();
        hudOverride = on;
        try {
            ABILITY_HUD.set(on);
            ABILITY_HUD.save();
            hudOverride = null;
        } catch (Throwable notLoaded) {
            // The client config isn't loaded (yet): the override holds until it is.
        }
        return on;
    }

    public static boolean bloodlustOverlay() {
        return bool(BLOODLUST_OVERLAY, true);
    }

    public static float overlayOpacity() {
        return (float)number(OVERLAY_OPACITY, 1.0);
    }

    public static boolean cameraEffects() {
        return bool(CAMERA_EFFECTS, true);
    }

    public static float cameraEffectStrength() {
        return (float)number(CAMERA_EFFECT_STRENGTH, 1.0);
    }

    public static boolean urgeViewPull() {
        return bool(URGE_VIEW_PULL, true);
    }

    public static boolean controlIndicator() {
        return bool(CONTROL_INDICATOR, true);
    }
}
