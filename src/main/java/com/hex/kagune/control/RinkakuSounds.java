package com.hex.kagune.control;

/**
 * 4.1.93 (the owner: "now you're gonna do the same sound thing that you just did for ukaku. You're gonna do for both of the other
 * cognates completely. Replace all sounds. Just make sure not to use the same ones as ukaku. Like that's a no go. The only one you're
 * allowed to use is the transformation in kakuja, the wither sound, if you want."): every sound the Rinkaku makes, one of the game's own
 * (pure), never one of the Ukaku's ({@code ukaku.UkakuSounds}) but the Wither's birth for its full kakuja. Each named with its namespace, so
 * it plays exactly as named (never drawn from the recorded pack). The clash is not the Rinkaku's: it keeps its own.
 */
public final class RinkakuSounds {
    // The tendrils.
    /** A tendril lashing out: a whip's crack (a fishing line cast). */
    public static final String WHIP = "minecraft:entity.fishing_bobber.throw";
    /** A heavy lash or a throw: a wither skull loosed. */
    public static final String HURL = "minecraft:entity.wither.shoot";
    /** A tendril striking flesh: a critical hit. */
    public static final String IMPACT = "minecraft:entity.player.attack.crit";
    /** A bone cracking. */
    public static final String BONE_CRACK = "minecraft:block.bone_block.hit";
    /** Bones crushed: an iron golem's damage, a grinding crunch. */
    public static final String BONE_CRUSH = "minecraft:entity.iron_golem.damage";
    /** A crush that kills: a slime bursting. */
    public static final String CRUSH_KILL = "minecraft:entity.slime.death";
    /** Tendrils wrapping round what they grab. */
    public static final String GRAB = "minecraft:block.slime_block.place";
    /** Tendrils straining (lifting, holding): the Warden's tendrils clicking. */
    public static final String STRAIN = "minecraft:entity.warden.tendril_clicks";
    /** Tendrils slamming down: a door broken in. */
    public static final String SLAM = "minecraft:entity.zombie.break_wooden_door";
    /** Blocks smashed: the Wither breaking through. */
    public static final String SMASH = "minecraft:entity.wither.break_block";
    /** A blow taken on the tendrils' guard: a shield blocking. */
    public static final String GUARD = "minecraft:item.shield.block";
    /** The guard broken. */
    public static final String GUARD_BREAK = "minecraft:item.shield.break";
    /** Tendrils driven into the ground or through a body: a trident striking home. */
    public static final String ANCHOR = "minecraft:item.trident.hit_ground";
    /** Rock torn out of the ground (the Rubble Hurl's chunk). */
    public static final String RUBBLE = "minecraft:block.stone.break";
    /** Loose ground scattering. */
    public static final String GRAVEL = "minecraft:block.gravel.break";
    /** Blood spraying where a tendril went through. */
    public static final String BLOOD = "minecraft:entity.slime.squish_small";
    /** A cue to be heard: the grab's window (a bell). */
    public static final String CUE = "minecraft:block.note_block.bell";
    /** The tendrils writhing while out (the loop the client plays). */
    public static final String WRITHE = "minecraft:block.sculk_sensor.clicking";

    // Coming out, reshaping, going back.
    /** The tendrils tearing out: a sculk catalyst blooming. */
    public static final String MANIFEST = "minecraft:block.sculk_catalyst.bloom";
    /** The kagune reshaped (more tails, another form): sculk spreading. */
    public static final String RESHAPE = "minecraft:block.sculk.spread";
    /** The tendrils drawn back in. */
    public static final String DISMISS = "minecraft:block.sculk_vein.break";

    // The centipede and the kakuja.
    /** The centipede's chitter. */
    public static final String CHITTER = "minecraft:entity.silverfish.ambient";
    /** The centipede (the first kakuja) coming on: a Ravager's roar. */
    public static final String CENTIPEDE = "minecraft:entity.ravager.roar";
    /** The full kakuja coming on: the Wither's birth (the one sound the owner let it share). */
    public static final String FULL_KAKUJA = "minecraft:entity.wither.spawn";
    /** Out of the centipede: a silverfish's death. */
    public static final String CHITIN_OFF = "minecraft:entity.silverfish.death";
    /** Out of the full kakuja: a beacon going dark. */
    public static final String KAKUJA_OFF = "minecraft:block.beacon.deactivate";
    /** The kakuja breaking apart on her. */
    public static final String KAKUJA_BREAK = "minecraft:entity.wither.break_block";

    /** Every sound above (the tests: each the game's own, none the Ukaku's but the Wither's birth). */
    public static final String[] ALL = {WHIP, HURL, IMPACT, BONE_CRACK, BONE_CRUSH, CRUSH_KILL, GRAB, STRAIN, SLAM, SMASH, GUARD, GUARD_BREAK, ANCHOR,
        WRITHE, RUBBLE, GRAVEL, BLOOD, CUE, MANIFEST, RESHAPE, DISMISS, CHITTER, CENTIPEDE, FULL_KAKUJA, CHITIN_OFF, KAKUJA_OFF, KAKUJA_BREAK};

    private RinkakuSounds() {
    }
}
