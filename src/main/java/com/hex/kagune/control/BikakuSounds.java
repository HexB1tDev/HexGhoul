package com.hex.kagune.control;

/**
 * 4.1.93 (the owner: "do for both of the other cognates completely. Replace all sounds. Just make sure not to use the same ones as ukaku"):
 * every sound the Bikaku makes (the worm with the mouth, its kakuja the devouring dragon), one of the game's own (pure), never one of the
 * Ukaku's ({@code ukaku.UkakuSounds}) nor the Rinkaku's ({@link RinkakuSounds}): each kagune its own voice. Named with its namespace, so it
 * plays exactly as named.
 */
public final class BikakuSounds {
    // The worm.
    /** The worm thrown out at what she looks at: a trident thrown. */
    public static final String LUNGE = "minecraft:item.trident.throw";
    /** Its jaws shutting on something: a Ravager's bite. */
    public static final String BITE = "minecraft:entity.ravager.attack";
    /** A lighter snap of the jaws (a grab, a nip): a fox's bite. */
    public static final String SNAP = "minecraft:entity.fox.bite";
    /** A bite landing in flesh: a Hoglin's goring. */
    public static final String IMPACT = "minecraft:entity.hoglin.attack";
    /** Bones crushed in its jaws. */
    public static final String BONE_CRUSH = "minecraft:block.bone_block.break";
    /** The worm anchoring in the ground. */
    public static final String ANCHOR = "minecraft:block.mud.break";
    /** The worm slamming down: an iron golem's blow. */
    public static final String SLAM = "minecraft:entity.iron_golem.attack";
    /** The worm writhing, hauling. */
    public static final String WRITHE = "minecraft:block.sculk.charge";
    /** The worm reshaped, coming out or going back: flesh tearing like honey. */
    public static final String RESHAPE = "minecraft:block.honey_block.break";
    /** Its armour of flesh shifting over her. */
    public static final String ARMOUR = "minecraft:item.armor.equip_chain";

    // Feeding.
    /** Eating. */
    public static final String EAT = "minecraft:entity.generic.eat";
    /** A gulp. */
    public static final String GULP = "minecraft:entity.generic.drink";
    /** Blood dripping. */
    public static final String DRIP = "minecraft:block.pointed_dripstone.drip_lava";
    /** Blood gushing. */
    public static final String GUSH = "minecraft:entity.generic.splash";
    /** A great gush. */
    public static final String GUSH_BIG = "minecraft:entity.player.splash.high_speed";

    // The dragon (the kakuja) and her potential.
    /** The dragon's roar: the Ender Dragon's growl. */
    public static final String DRAGON_ROAR = "minecraft:entity.ender_dragon.growl";
    /** The kakuja bursting out of her: the Wither's birth (the one sound the owner let the kagunes share). */
    public static final String KAKUJA = "minecraft:entity.wither.spawn";
    /** Her body warping into something else: an elder guardian's curse. */
    public static final String WARP = "minecraft:entity.elder_guardian.curse";
    /** The dragon burrowing, crawling. */
    public static final String DIG = "minecraft:entity.warden.dig";
    /** Something torn and dragged: a slime squished. */
    public static final String SQUELCH = "minecraft:entity.slime.squish";
    /** The dragon's cataclysm bursting out of her, everything near hurled: the Warden's sonic boom. */
    public static final String CATACLYSM = "minecraft:entity.warden.sonic_boom";
    /** The kakuja breaking apart on her. */
    public static final String KAKUJA_BREAK = "minecraft:entity.ender_dragon.hurt";

    /** Every sound above (the tests: each the game's own, none the Ukaku's or the Rinkaku's but the Wither's birth). */
    public static final String[] ALL = {LUNGE, BITE, SNAP, IMPACT, BONE_CRUSH, ANCHOR, SLAM, WRITHE, RESHAPE, ARMOUR, EAT, GULP, DRIP, GUSH, GUSH_BIG,
        DRAGON_ROAR, KAKUJA, WARP, DIG, SQUELCH, CATACLYSM, KAKUJA_BREAK};

    private BikakuSounds() {
    }
}
