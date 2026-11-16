package com.hex.kagune.control.ukaku;

/**
 * 4.1.91 (the owner: "rework the the Ukaku sounds to use only Minecraft sounds that are ideal for every single thing that form does. The
 * only thing you're not allowed to touch is the clash sounds. That's a universal thing. ... change every sound in the Ukaku so that every
 * sound is Minecraft based. Just make sure that you pick the most badass sounds possible. Like for each situation, such as like firing
 * projectiles, crystal projectiles, the slaps with the normal wings and whatsoever."): every sound the Ukaku makes, one of the game's own
 * (pure). Each name carries its namespace, so it plays exactly as named: never drawn from the user's recorded pack ({@code Sfx}, which
 * turns a bare vanilla name into one of its pools). The clash ({@code KaguneClash}) is not the Ukaku's: it keeps its own.
 */
public final class UkakuSounds {
    // The wings.
    /** A beat of the wings: a hop, a hover, a flap before a strike (the dragon's wings: deep, leathery, huge). */
    public static final String WING_FLAP = "minecraft:entity.ender_dragon.flap";
    /** A light beat (a small hop, a shot's wind-up): the phantom's wings. */
    public static final String WING_FLICK = "minecraft:entity.phantom.flap";
    /** A rush through the air (a dash, the Rabbit's charge): the phantom's dive. */
    public static final String WING_RUSH = "minecraft:entity.phantom.swoop";
    /** A wing (or the Steel Blade) cutting the air: a sweeping blade. */
    public static final String WING_CUT = "minecraft:entity.player.attack.sweep";
    /** A wing slap landing. */
    public static final String SLAP = "minecraft:entity.player.attack.strong";
    /** A heavy slap that throws them back. */
    public static final String SLAP_HEAVY = "minecraft:entity.player.attack.knockback";
    /** A wing slam crushing what it lands on: the Warden's blow. */
    public static final String SLAM = "minecraft:entity.warden.attack_impact";
    /** A kick's swish. */
    public static final String KICK = "minecraft:entity.player.attack.nodamage";

    // The fire: RC shards.
    /** A shard thrown (a fiery spit): the Blaze's shot. */
    public static final String SHARD = "minecraft:entity.blaze.shoot";
    /** A heavy volley or a burst of flame: a fire charge. */
    public static final String FLAME_BURST = "minecraft:item.firecharge.use";
    /** A fireball loosed: the Ghast's. */
    public static final String FIREBALL = "minecraft:entity.ghast.shoot";
    /** Burning. */
    public static final String BURN = "minecraft:entity.generic.burn";
    /** A flame put out. */
    public static final String SNUFF = "minecraft:block.fire.extinguish";
    /** A shard or a shot bursting where it lands: a firework's crack. */
    public static final String POP = "minecraft:entity.firework_rocket.blast";
    /** An explosion. */
    public static final String BLAST = "minecraft:entity.generic.explode";

    // The crystal.
    /** Crystal growing (a lance, a spike, a needle forming). */
    public static final String CRYSTAL_GROW = "minecraft:block.amethyst_cluster.place";
    /** A crystal fired: a crossbow's snap. */
    public static final String CRYSTAL_LAUNCH = "minecraft:item.crossbow.shoot";
    /** A crystal striking. */
    public static final String CRYSTAL_HIT = "minecraft:block.amethyst_block.hit";
    /** Crystal cracking. */
    public static final String CRYSTAL_CRACK = "minecraft:block.amethyst_cluster.break";
    /** Crystal shattering. */
    public static final String CRYSTAL_SHATTER = "minecraft:block.amethyst_block.break";
    /** A crystal ringing. */
    public static final String CRYSTAL_RING = "minecraft:block.amethyst_block.chime";
    /** Crystal humming with power. */
    public static final String CRYSTAL_HUM = "minecraft:block.amethyst_block.resonate";
    /** A crystal's light touch (a needle landing). */
    public static final String CRYSTAL_TAP = "minecraft:block.amethyst_block.step";
    /** A crystal driven into flesh: a trident's hit. */
    public static final String STAB = "minecraft:item.trident.hit";
    /** Glass breaking. */
    public static final String GLASS = "minecraft:block.glass.break";
    /** Spikes bursting out (the Thorns): the Evoker's fangs. */
    public static final String SPIKES = "minecraft:entity.evoker_fangs.attack";
    /** A crystal shot's detonation: a lightning strike's crack. */
    public static final String CRACK = "minecraft:entity.lightning_bolt.impact";
    /** A big burst of crystal: a large firework's blast. */
    public static final String BIG_BURST = "minecraft:entity.firework_rocket.large_blast";

    // The blade, the vortex, the kakuja.
    /** The Steel Blade drawn out of the wing. */
    public static final String BLADE_DRAW = "minecraft:item.trident.return";
    /** The Steel Blade taken back: armour clamping. */
    public static final String BLADE_SHEATHE = "minecraft:item.armor.equip_netherite";
    /** The Steel Blade setting hard. */
    public static final String BLADE_SET = "minecraft:block.anvil.place";
    /** A Wing Vortex spinning up. */
    public static final String VORTEX_SPIN = "minecraft:item.trident.riptide_1";
    /** A Wing Vortex thrown. */
    public static final String VORTEX_THROW = "minecraft:item.trident.riptide_3";
    /** A blink out of sight. */
    public static final String SHADOW_STEP = "minecraft:entity.illusioner.mirror_move";
    /** The wings tearing out of the back, ablaze (the form manifesting). */
    public static final String MANIFEST = "minecraft:item.firecharge.use";
    /** The Ukaku's kakuja coming on: a totem's burst. */
    public static final String KAKUJA = "minecraft:item.totem.use";
    /** The final kakuja: the Wither's birth. */
    public static final String FINAL_KAKUJA = "minecraft:entity.wither.spawn";
    /** A mutation learned: an enchantment taking. */
    public static final String MUTATION = "minecraft:block.enchantment_table.use";
    /** Out of the kakuja: an anchor's charge spent. */
    public static final String KAKUJA_OFF = "minecraft:block.respawn_anchor.deplete";

    /** Every sound above (for the tests: each one the game's own). */
    public static final String[] ALL = {WING_FLAP, WING_FLICK, WING_RUSH, WING_CUT, SLAP, SLAP_HEAVY, SLAM, KICK, SHARD, FLAME_BURST, FIREBALL, BURN,
        SNUFF, POP, BLAST, CRYSTAL_GROW, CRYSTAL_LAUNCH, CRYSTAL_HIT, CRYSTAL_CRACK, CRYSTAL_SHATTER, CRYSTAL_RING, CRYSTAL_HUM, CRYSTAL_TAP, GLASS,
        SPIKES, STAB, CRACK, BIG_BURST, BLADE_DRAW, BLADE_SHEATHE, BLADE_SET, VORTEX_SPIN, VORTEX_THROW, SHADOW_STEP, MANIFEST, KAKUJA, FINAL_KAKUJA,
        MUTATION, KAKUJA_OFF};

    private UkakuSounds() {
    }
}
