package com.hex.kagune.control;

import java.util.HashMap;
import java.util.Map;

/**
 * 1.2.2: every sound of the mod is the user's own pack (Bizarre SFX, {@code tools/sfx_pack.py}), pure, tested. The moves were written
 * calling vanilla's sounds by name ("entity.generic.explode", "entity.player.attack.sweep", ...); each name the mod uses is drawn here
 * from the pack's pool that does that job ({@code hexkagune:sfx.<pool>}): the louder calls of a name from its heavier pool where it
 * has one (a little crack and a ground-splitting blast were both "explode"). The pitch a call asks for is drawn most of the way back to
 * the sound's own ({@link #KEEP}): vanilla's were pitched far down or up to make them fit, and the pack's are made to fit as they are.
 * A name with no pool here is played as vanilla's (only the quest journal's own cues, which are the game's interface, not a fight).
 */
public final class Sfx {
    /** How much of a call's own pitch shift is kept (0: none, the pack's own pitch; 1: all of it). */
    public static final float KEEP = 0.3F;
    public static final float LOWEST = 0.8F;
    public static final float HIGHEST = 1.25F;

    /** What a call plays: the sound's namespace and path, at the volume and pitch to play it at. */
    public record Sound(String namespace, String path, float volume, float pitch) {
    }

    /** A vanilla name's pool, and from what volume on its heavier one (and from what its heaviest). */
    private record Remap(String light, float heavyFrom, String heavy, float heaviestFrom, String heaviest) {
        String pool(float volume) {
            if (volume >= this.heaviestFrom) {
                return this.heaviest;
            }
            return volume >= this.heavyFrom ? this.heavy : this.light;
        }
    }

    private static final Map<String, Remap> MAP = new HashMap<>();

    private static void one(String vanilla, String pool) {
        MAP.put(vanilla, new Remap(pool, Float.MAX_VALUE, pool, Float.MAX_VALUE, pool));
    }

    private static void two(String vanilla, String light, float heavyFrom, String heavy) {
        MAP.put(vanilla, new Remap(light, heavyFrom, heavy, Float.MAX_VALUE, heavy));
    }

    static {
        // Blows.
        two("entity.player.attack.sweep", "blade_swing", 1.1F, "blade_heavy");
        two("entity.player.attack.strong", "punch_hit", 0.95F, "punch_hit_heavy");
        two("entity.player.attack.knockback", "punch_hit", 1.15F, "punch_hit_heavy");
        one("entity.player.attack.crit", "finisher");
        one("entity.player.attack.nodamage", "punch_swing");
        one("entity.player.hurt", "punch_hit");
        one("entity.iron_golem.damage", "punch_hit_heavy");
        one("block.anvil.land", "punch_hit_heavy");
        one("block.anvil.place", "punch_hit");
        // Explosions: a crack, a shockwave, the ground splitting.
        MAP.put("entity.generic.explode", new Remap("burst", 0.8F, "shockwave", 1.6F, "ultimate"));
        one("entity.warden.sonic_boom", "shockwave");
        one("entity.lightning_bolt.thunder", "thunder");
        two("entity.generic.big_fall", "step_heavy", 1.0F, "punch_hit_heavy");
        one("entity.player.small_fall", "footfall");
        one("block.gravel.break", "ground");
        one("block.stone.break", "ground");
        one("block.stone.hit", "ground");
        one("block.deepslate.break", "ground");
        one("block.basalt.break", "ground");
        // Air: wings beating, dashes, vanishing.
        two("entity.ender_dragon.flap", "whoosh", 0.75F, "air_burst");
        one("entity.illusioner.mirror_move", "dash");
        // Ayato's fire: shards of RC, thrown.
        two("entity.blaze.shoot", "spike_throw", 0.75F, "spike_heavy");
        one("item.firecharge.use", "spike_heavy");
        one("entity.generic.burn", "hiss");
        one("block.fire.extinguish", "hiss");
        one("item.trident.throw", "spike_heavy");
        one("item.trident.return", "spike_throw");
        one("entity.evoker_fangs.attack", "spike_heavy");
        // Ayato's crystal.
        one("block.amethyst_cluster.place", "crystal_form");
        one("block.amethyst_block.chime", "crystal_form");
        one("block.amethyst_cluster.break", "shatter");
        one("block.amethyst_block.break", "shatter");
        one("block.glass.break", "shatter");
        one("block.amethyst_block.hit", "stab");
        one("block.amethyst_block.step", "stab");
        one("block.amethyst_block.resonate", "transform.spectral_reveal");
        // Flesh and blood.
        two("entity.slime.squish", "blood_splat", 1.15F, "blood_splat_big");
        one("block.honey_block.break", "blood_splat_big");
        one("block.soul_sand.break", "blood_splat");
        one("entity.generic.eat", "eat");
        one("entity.warden.heartbeat", "pulse");
        one("entity.warden.emerge", "gush_big");
        // Voices and powers rising: the pack's transformations.
        one("entity.warden.roar", "transform.dark_aura_release");
        one("entity.ravager.roar", "transform.dark_aura_release");
        one("entity.ghast.scream", "transform.dark_aura_release");
        one("entity.wolf.howl", "transform.dark_aura_release");
        one("block.sculk_catalyst.bloom", "transform.pressure_awakening");
        one("block.beacon.activate", "transform.energy_metamorphosis");
        one("block.beacon.power_select", "transform.spectral_reveal");
        one("block.beacon.deactivate", "transform.organic_armor_shift");
        one("item.armor.equip_iron", "crystal_form");
        // A cue to be heard (Kaneki's grab's window).
        one("block.note_block.bell", "cue");
    }

    private Sfx() {
    }

    /**
     * What a call naming {@code id} at {@code volume} and {@code pitch} plays: a name with a namespace ("hexkagune:sfx.ultimate") as it
     * is; a vanilla name from its pool, its pitch drawn back toward the sound's own; any other vanilla name as vanilla's.
     */
    public static Sound play(String id, float volume, float pitch) {
        int colon = id.indexOf(':');
        if (colon > 0) {
            return new Sound(id.substring(0, colon), id.substring(colon + 1), volume, pitch);
        }
        Remap remap = MAP.get(id);
        if (remap == null) {
            return new Sound("minecraft", id, volume, pitch);
        }
        return new Sound("hexkagune", "sfx." + remap.pool(volume), volume, pitch(pitch));
    }

    /** A call's pitch with only {@link #KEEP} of its shift kept, between {@link #LOWEST} and {@link #HIGHEST}. */
    public static float pitch(float asked) {
        if (!Float.isFinite(asked)) {
            return 1.0F;
        }
        return Math.max(LOWEST, Math.min(HIGHEST, 1.0F + (asked - 1.0F) * KEEP));
    }

    /** True if {@code id} (a vanilla name) is drawn from the pack. */
    public static boolean remapped(String id) {
        return MAP.containsKey(id);
    }

    /** Every pool a remap can draw from (for the tests: each must be in sounds.json). */
    public static java.util.Set<String> pools() {
        java.util.Set<String> pools = new java.util.TreeSet<>();
        for (Remap remap : MAP.values()) {
            pools.add(remap.light());
            pools.add(remap.heavy());
            pools.add(remap.heaviest());
        }
        return pools;
    }
}
