#!/usr/bin/env python3
"""1.2.2: the user's own sound pack (Bizarre SFX, 120 labeled OGGs) as the mod's sounds.

Run after dropping the pack's OGGs into src/main/resources/assets/hexkagune/sounds/sfx/<group>/<name>.ogg (mono: Minecraft only
places mono sounds in the world; a stereo one is heard at full volume by everyone, wherever they are). Writes the expansion's
sounds.json: the pools every sound of the mod is drawn from (sfx.<pool>: a few of the pack's sounds, one picked at random each time,
each at a volume that evens out how loud the pack's files are), the original's own events pointed at them ({@code replace}: build.py
lets these through), and the expansion's own events (eye_open stays the user's earlier sound).

Usage: python3 tools/sfx_pack.py
"""
import json
import pathlib
import subprocess

ROOT = pathlib.Path(__file__).resolve().parent.parent
SOUNDS = ROOT / "src/main/resources/assets/hexkagune/sounds"
OUT = ROOT / "src/main/resources/assets/hexkagune/sounds.json"

SWORD = "combat/sword_swings/"
PSWING = "combat/punch_swings/"
PHIT = "combat/punch_hits/"
RUN = "combat/running/"
SPIKE = "combat/spike_throws/"
FORM = "combat/transformations/"
BLOOD = "blood_and_gush/blood_impacts/"
SPRAY = "blood_and_gush/blood_sprays/"
GUSH = "blood_and_gush/gushing_bursts/"
FLOW = "blood_and_gush/sustained_gushing/"
DRIP = "blood_and_gush/drips_and_pools/"
COMBO = "blood_and_gush/dramatic_combos/"
CLASH = "super_punch_collisions/"

# pool: (gain, [files], subtitle). Frequent sounds get short files and a lower gain; the big ones the long, loud collisions.
POOLS = {
    # Kagune and blades cutting the air (every path's M1 swings).
    "blade_swing": (0.6, [SWORD + "fast_steel_cut", SWORD + "reverse_slash", SWORD + "air_tear", SWORD + "rising_blade"], "Kagune cuts the air"),
    "blade_heavy": (0.8, [SWORD + "heavy_cleave", SWORD + "wide_saber_sweep", SWORD + "spinning_blade", SWORD + "twin_cuts"], "Kagune cleaves"),
    "blade_arc": (0.75, [SWORD + "spectral_sword_arc", SWORD + "needle_edge"], "A blade rings"),
    # Fists.
    "punch_swing": (0.6, [PSWING + "short_jab_air", PSWING + "straight_cross", PSWING + "left_hook", PSWING + "right_hook",
                          PSWING + "fast_fist_snap"], "A fist swings"),
    "punch_swing_heavy": (0.7, [PSWING + "rising_uppercut", PSWING + "heavy_windup", PSWING + "spinning_fist", PSWING + "charged_fist_swing",
                                PSWING + "double_feint"], "A heavy swing"),
    "punch_hit": (0.7, [PHIT + "face_smack", PHIT + "short_jab", PHIT + "rib_impact", PHIT + "heavy_uppercut", PHIT + "dry_fist_hit"], "A blow lands"),
    "punch_hit_heavy": (0.85, [PHIT + "heavy_body_blow", PHIT + "crushing_cross", PHIT + "knuckle_slam", PHIT + "finishing_punch"], "A heavy blow lands"),
    "finisher": (0.95, [PHIT + "finishing_punch", PHIT + "crushing_cross"], "A finishing blow"),
    "crunch": (0.6, [PHIT + "bone_crack", PHIT + "rib_impact"], "Bone cracks"),
    "crush": (0.85, [PHIT + "crushing_cross", PHIT + "bone_crack", BLOOD + "dense_squelch"], "Bones crush"),
    # Steps.
    "footfall": (0.5, [RUN + "concrete_footfall", RUN + "leather_footfall", RUN + "cloth_footfall", RUN + "armor_footfall"], "Footsteps"),
    "step_heavy": (0.55, [RUN + "armor_footfall", RUN + "concrete_footfall"], "Heavy footsteps"),
    "step_light": (0.4, [RUN + "cloth_footfall", RUN + "leather_footfall"], "Light footsteps"),
    "chitin": (0.45, [RUN + "armor_footfall", RUN + "concrete_footfall"], "Centipede legs click"),
    # Spikes, shards, needles (Ukaku's fire shots too: shards of RC).
    "spike_throw": (0.6, [SPIKE + "needle_flick", SPIKE + "steel_spike", SPIKE + "fast_dart", SPIKE + "spinning_spike"], "A spike flies"),
    "spike_heavy": (0.8, [SPIKE + "heavy_throw", SPIKE + "heavy_lance", SPIKE + "charged_spike"], "A heavy spike flies"),
    "spike_volley": (0.8, [SPIKE + "double_throw", SPIKE + "triple_fan", SPIKE + "razor_volley"], "A volley flies"),
    "stab": (0.5, [PHIT + "short_jab", PHIT + "rib_impact", SPIKE + "needle_flick"], "A spike sinks in"),
    "anchor": (0.7, [SPIKE + "steel_spike", SPIKE + "heavy_lance"], "Kagune anchors"),
    "crystal_form": (0.7, [SPIKE + "charged_spike", SPIKE + "steel_spike"], "Crystal forms"),
    "shatter": (0.65, [PHIT + "bone_crack", CLASH + "close_dry_collision", CLASH + "fast_counter_clash"], "Crystal shatters"),
    "cue": (0.9, [SPIKE + "needle_flick"], "A glint"),
    # Blood and flesh.
    "blood_splat": (0.7, [BLOOD + "small_wet_splat", BLOOD + "thick_blood_smack", BLOOD + "heavy_wet_impact", BLOOD + "sharp_blood_slap",
                          BLOOD + "wide_splatter_hit", BLOOD + "sudden_blood_burst"], "Flesh splits"),
    "blood_splat_big": (0.85, [BLOOD + "dense_squelch", BLOOD + "big_anime_splat", BLOOD + "heavy_wet_impact"], "Flesh bursts"),
    "grab": (0.7, [BLOOD + "thick_blood_smack", BLOOD + "sharp_blood_slap"], "Kagune grabs"),
    "blood_spray": (0.75, [SPRAY + n for n in ("light_blood_spray", "sharp_blood_squirt", "sideways_spray", "heavy_blood_spray", "double_spray",
                                              "triple_spray", "pressure_spray", "wide_blood_fan")], "Blood sprays"),
    "gush": (0.75, [GUSH + "short_thick_gush", GUSH + "deep_blood_gush", GUSH + "pressurized_gush", GUSH + "double_gush"], "Blood gushes"),
    "gush_big": (0.85, [GUSH + "pulsing_gush", GUSH + "staggered_gush", GUSH + "rushing_blood_burst", GUSH + "dramatic_blood_gush"], "Blood bursts out"),
    "gush_long": (0.6, [FLOW + n for n in ("continuous_blood_gush", "heavy_continuous_gush", "thick_blood_flow", "pulsing_blood_stream",
                                           "long_tapering_gush", "gush_fading_to_drips")], "Blood pours"),
    "drip": (0.5, [DRIP + "single_blood_drop", DRIP + "thick_double_drop", DRIP + "last_two_drops"], "Blood drips"),
    "drips": (0.5, [DRIP + "slow_blood_drips", DRIP + "fast_blood_drips", DRIP + "drips_into_a_pool"], "Blood drips"),
    "pulse": (0.85, [DRIP + "thick_double_drop"], "A heartbeat"),
    "hiss": (0.45, [SPRAY + "pressure_spray", SPRAY + "sideways_spray"], "A hiss"),
    "eat": (0.75, [BLOOD + "dense_squelch", BLOOD + "thick_blood_smack"], "Eating"),
    "writhe": (0.3, [FLOW + "thick_blood_flow"], "Kagune writhes"),
    "crush_kill": (0.95, [COMBO + "heavy_hit_and_gush", COMBO + "final_blood_burst"], "Crushed to death"),
    "slash_spray": (0.85, [COMBO + "slash_then_blood_spray"], "A cut sprays blood"),
    "blood_bursts": (0.85, [COMBO + "rapid_blood_bursts"], "Blood bursts"),
    # Air, clashes, the big ones.
    "whoosh": (0.55, [SWORD + "air_tear", SWORD + "wide_saber_sweep", PSWING + "double_feint"], "A rush of air"),
    "dash": (0.65, [SWORD + "air_tear", SWORD + "fast_steel_cut"], "A dash"),
    "air_burst": (0.75, [CLASH + "heavy_air_burst"], "A burst of air"),
    "clash": (0.85, [CLASH + "fast_counter_clash", CLASH + "close_dry_collision", CLASH + "hit_stop_clash"], "Kagune clash"),
    "clash_big": (0.95, [CLASH + "titan_fist_clash", CLASH + "double_shock_impact", CLASH + "sonic_fist_crash"], "A huge clash"),
    "burst": (0.7, [CLASH + "close_dry_collision", CLASH + "fast_counter_clash", CLASH + "heavy_air_burst"], "An impact bursts"),
    "shockwave": (0.85, [CLASH + "wide_air_shockwave", CLASH + "twin_fist_shockwave", CLASH + "heavy_air_burst", CLASH + "double_shock_impact"],
                  "A shockwave"),
    "quake": (0.95, [CLASH + "ground_shaking_collision", CLASH + "deep_pressure_collision"], "The ground shakes"),
    "ultimate": (1.0, [CLASH + "ultimate_fist_collision", CLASH + "thunder_punch_collision", CLASH + "sky_splitting_clash",
                       CLASH + "charged_fist_collision"], "A colossal impact"),
    "thunder": (1.0, [CLASH + "thunder_punch_collision"], "Thunder"),
    "windup_clash": (1.0, [CLASH + "windup_to_massive_clash"], "A massive clash"),
    "ground": (0.55, [CLASH + "close_dry_collision", PHIT + "knuckle_slam"], "Stone breaks"),
}
# One transformation each (they are long: one at a time, never pooled).
FORMS = ["pressure_awakening", "organic_armor_shift", "spectral_reveal", "energy_metamorphosis", "body_warp", "dark_aura_release",
         "heavy_morph", "overdrive_awakening", "dimensional_shift", "final_form"]
FORM_GAIN = 0.8

# The original's events, pointed at the pools (event: [(pool or "file:<path>", pitch)]).
ORIGINAL = {
    "kagune_whip": "blade_swing",
    "bone_crack": "crunch",
    "kagune_slam": "punch_hit_heavy",
    "flesh_impact": "blood_splat",
    "kagune_strain": "punch_swing_heavy",
    "manifest_rip": "gush",
    "kagune_reshape": "transform.organic_armor_shift",
    "bone_crush": "crush",
    "kagune_guard": "clash",
    "centipede_click": "chitin",
    "kagune_grab": "grab",
    "kagune_dismiss": "transform.organic_armor_shift",
    "shard_launch": "spike_volley",
    "crush_kill": "crush_kill",
    "kagune_anchor": "anchor",
    "kagune_idle": "writhe",
}
# The expansion's own: Rinkaku's tendrils tearing out one by one (short gushes, each cut off by the next), Ukaku's wings igniting.
EXPANSION = {
    "manifest_rinkaku": "gush",
    "manifest_ukaku": "transform.spectral_reveal",
}


def loudness(path):
    out = subprocess.run(["ffmpeg", "-hide_banner", "-nostats", "-i", str(path), "-af", "volumedetect", "-f", "null", "-"],
                         capture_output=True, text=True).stderr
    for line in out.splitlines():
        if "mean_volume:" in line:
            return float(line.split("mean_volume:")[1].split()[0])
    raise RuntimeError(f"no loudness for {path}")


def volume(path, gain):
    # Even out the pack's loudness (its files are all peak-normalized, but some are far denser than others): -22 dB mean is 1.0.
    level = 10.0 ** ((-22.0 - loudness(path)) / 20.0)
    return round(max(0.2, min(1.0, gain * min(1.6, level))), 2)


def sound(path, gain, stream=False):
    file = SOUNDS / "sfx" / (path + ".ogg")
    if not file.is_file():
        raise RuntimeError(f"missing {file}")
    entry = {"name": "hexkagune:sfx/" + path, "volume": volume(file, gain)}
    if stream:
        entry["stream"] = True
    return entry


def event(target):
    return {"name": "hexkagune:sfx." + target, "type": "event"}


def main():
    sounds = {}
    for name, (gain, files, subtitle) in POOLS.items():
        sounds["sfx." + name] = {"subtitle": "subtitles.hexkagune.sfx." + name, "sounds": [sound(f, gain) for f in files]}
    for form in FORMS:
        sounds["sfx.transform." + form] = {"subtitle": "subtitles.hexkagune.sfx.transform",
                                            "sounds": [sound(FORM + form, FORM_GAIN, stream=True)]}
    for name, target in ORIGINAL.items():
        sounds[name] = {"replace": True, "subtitle": "subtitles.hexkagune." + name, "sounds": [event(target)]}
    for name, target in EXPANSION.items():
        sounds[name] = {"subtitle": "subtitles.hexkagune." + name, "sounds": [event(target)]}
    # The kakugan opening: the user's own earlier sound, kept.
    sounds["eye_open"] = {"subtitle": "subtitles.hexkagune.eye_open", "sounds": [{"name": "hexkagune:eye_open", "stream": False}]}
    OUT.write_text(json.dumps(sounds, indent=2) + "\n", encoding="utf-8")
    used = {e["name"] for v in sounds.values() for e in v["sounds"] if "type" not in e}
    pack = {"hexkagune:sfx/" + p.relative_to(SOUNDS / "sfx").with_suffix("").as_posix() for p in (SOUNDS / "sfx").rglob("*.ogg")}
    print(f"wrote {OUT.relative_to(ROOT)}: {len(sounds)} events, {len(used & pack)} of the pack's {len(pack)} sounds in use")
    for unused in sorted(pack - used):
        print("  not used:", unused)


if __name__ == "__main__":
    main()
