#!/usr/bin/env python3
"""The hexkagune sound pack, including the owner's sounds and credited recorded foley.

Run after dropping the pack's OGGs into src/main/resources/assets/hexkagune/sounds/sfx/<group>/<name>.ogg (mono: Minecraft only
places mono sounds in the world; a stereo one is heard at full volume by everyone, wherever they are). Writes the expansion's
sounds.json: the pools every sound of the mod is drawn from (sfx.<pool>: a few of the pack's sounds, one picked at random each time,
each at a volume that evens out how loud the pack's files are), the original's own events pointed at them ({@code replace}: build.py
lets these through), and the expansion's own events (eye_open stays the user's earlier sound).

Usage: python3 tools/sfx_pack.py
Import the licensed recordings listed in tools/sfx_recordings.json:
    python3 tools/sfx_pack.py --import-recordings /path/to/downloaded/sources
Only trims, short edge fades, mono conversion and Vorbis encoding are applied; no synthesis.
"""
import argparse
import functools
import hashlib
import json
import math
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
# 1.3.28: the quinque case's own (tools/case_sfx.py builds them: its latches, its mechanism, its lock).
QUINQUE = "quinque/"
# 1.3.61: the owner's own clash sounds (Clashes.zip: Clash1..4.ogg, mono 48 kHz), every clash's impact.
CLASHES = "clashes/"
# 2.4.8: bone sounds the owner asked to be fetched from the web (CC BY 4.0, credited in sfx/bone/CREDITS.txt).
BONE = "bone/"
RECORDED = "recorded/"

# 3.0.7: all new material is recorded, never synthesized. The source manifest carries the author's
# licence, original page, immutable GitHub download and source checksum for every recording.
RECORDING_MANIFEST = ROOT / "tools/sfx_recordings.json"
SWISHES = [RECORDED + n for n in ("air_swish_1", "air_swish_2", "air_swish_3")]
SPLATS = [RECORDED + n for n in ("flesh_slap_1", "flesh_slap_2", "flesh_slap_3")]
RIPS = [RECORDED + n for n in ("flesh_rip_1", "flesh_rip_2")]
INSECTS = [RECORDED + n for n in ("insect_chirr", "insect_click_1", "insect_click_2")]

# pool: (gain, [files], subtitle). Frequent sounds get short files and a lower gain; the big ones the long, loud collisions.
POOLS = {
    # Kagune cutting the air (every path's M1 swings). 1.2.8: air alone, never a sword's ring (the user: no metal on a swing).
    "blade_swing": (0.6, SWISHES + [RECORDED + "air_slash"], "Kagune cuts the air"),
    "blade_heavy": (0.8, [RECORDED + "air_heavy", RECORDED + "air_swish_3"], "Kagune cleaves"),
    "blade_arc": (0.65, SWISHES, "Kagune cuts the air"),
    # Fists.
    "punch_swing": (0.6, [PSWING + "short_jab_air", PSWING + "straight_cross", PSWING + "left_hook", PSWING + "right_hook",
                          PSWING + "fast_fist_snap"], "A fist swings"),
    "punch_swing_heavy": (0.7, [PSWING + "rising_uppercut", PSWING + "heavy_windup", PSWING + "spinning_fist", PSWING + "charged_fist_swing",
                                PSWING + "double_feint"], "A heavy swing"),
    "punch_hit": (0.7, [PHIT + "face_smack", PHIT + "short_jab", PHIT + "rib_impact", PHIT + "heavy_uppercut", PHIT + "dry_fist_hit"], "A blow lands"),
    "punch_hit_heavy": (0.85, [PHIT + "heavy_body_blow", PHIT + "crushing_cross", PHIT + "knuckle_slam", PHIT + "finishing_punch"], "A heavy blow lands"),
    "finisher": (0.95, [PHIT + "finishing_punch", PHIT + "crushing_cross"], "A finishing blow"),
    "crunch": (0.6, [PHIT + "bone_crack", PHIT + "rib_impact"], "Bone cracks"),
    "crush": (0.85, [PHIT + "crushing_cross", PHIT + "bone_crack"] + RIPS, "Bones crush"),
    # 2.4.8: a finger cracked (Ken's, and a player he taught a kakuja), and the kagune tearing out of a back after it (bone/CREDITS.txt).
    "finger_crack": (1.0, [BONE + "finger_crack"], "A finger cracks"),
    "kagune_burst": (1.0, [BONE + "kagune_burst"], "Kagune tears out"),
    # Steps.
    "footfall": (0.5, [RUN + "concrete_footfall", RUN + "leather_footfall", RUN + "cloth_footfall", RUN + "armor_footfall"], "Footsteps"),
    "step_heavy": (0.55, [RUN + "armor_footfall", RUN + "concrete_footfall"], "Heavy footsteps"),
    "step_light": (0.4, [RUN + "cloth_footfall", RUN + "leather_footfall"], "Light footsteps"),
    "chitin": (0.45, INSECTS, "Centipede legs click"),
    "centipede_manifest": (0.35, [RECORDED + "insect_hiss_1", RECORDED + "insect_hiss_2"], "Centipede legs emerge"),
    # Spikes, shards, needles (Ukaku's fire shots too: shards of RC).
    "spike_throw": (0.6, [SPIKE + "needle_flick", SPIKE + "steel_spike", SPIKE + "fast_dart", SPIKE + "spinning_spike"], "A spike flies"),
    "spike_heavy": (0.8, [SPIKE + "heavy_throw", SPIKE + "heavy_lance", SPIKE + "charged_spike"], "A heavy spike flies"),
    "spike_volley": (0.8, [SPIKE + "double_throw", SPIKE + "triple_fan", SPIKE + "razor_volley"], "A volley flies"),
    "stab": (0.5, [PHIT + "short_jab", PHIT + "rib_impact", SPIKE + "needle_flick"], "A spike sinks in"),
    "anchor": (0.7, [SPIKE + "steel_spike", SPIKE + "heavy_lance"], "Kagune anchors"),
    # 3.2.6 (the owner: "Crystals sound DOES not sound like crystals, it sounds like punches"): the shatter was a bone crack and two punch
    # collisions, a crystal's hit the pack's punch hits. The pack has no glass of its own (and no recording whose CC0 licence can be checked
    # from here): its brightest steel, a needle's flick, a spinning spike, raised into a ring and a tinkle (a file's own pitch: (file, pitch)).
    # 3.3.5 (the owner on 3.3.3: "Get the sounds from the web, don't generate them yourself"): crystal from real glass recordings (CC0,
    # tools/sfx_recordings.json), as recorded. 3.2.6 had raised the pack's steel and needle sounds into a ring: made over, gone.
    # 3.4.4 (the owner: "The ones you added for the crystal form are fucking dog shit, bro. Just download better ones."): 3.3.5's were glasses
    # clinking, a cup and saucer, a glass break's crack. Now a rubbed wine glass singing (three of its swells) and a small bell's ring.
    "crystal_form": (0.75, [RECORDED + "crystal_form_1", RECORDED + "crystal_form_2", RECORDED + "crystal_form_3", RECORDED + "crystal_form_4"],
                     "Crystal forms"),
    "shatter": (0.75, [RECORDED + "crystal_shatter_1", RECORDED + "crystal_shatter_2", RECORDED + "crystal_shatter_3"], "Crystal shatters"),
    "crystal_hit": (0.6, [RECORDED + "crystal_clink_1", RECORDED + "crystal_clink_2", RECORDED + "crystal_crack"], "A crystal strikes"),
    "cue": (0.9, [SPIKE + "needle_flick"], "A glint"),
    # Blood and flesh.
    "blood_splat": (0.7, SPLATS, "Flesh splits"),
    "blood_splat_big": (0.85, [RECORDED + "flesh_slap_2"] + RIPS, "Flesh bursts"),
    "grab": (0.6, SPLATS, "Kagune grabs"),
    "blood_spray": (0.55, [RECORDED + "flesh_rip_short"] + SPLATS, "Blood sprays"),
    "gush": (0.65, RIPS, "Blood gushes"),
    "gush_big": (0.75, RIPS, "Blood bursts out"),
    "gush_long": (0.5, RIPS, "Blood pours"),
    "drip": (0.5, [DRIP + "single_blood_drop", DRIP + "thick_double_drop", DRIP + "last_two_drops"], "Blood drips"),
    "drips": (0.5, [DRIP + "slow_blood_drips", DRIP + "fast_blood_drips", DRIP + "drips_into_a_pool"], "Blood drips"),
    "eat": (0.6, RIPS, "Eating"),
    "writhe": (0.2, [RECORDED + "flesh_rip_short"], "Kagune writhes"),
    "crush_kill": (0.85, RIPS, "Crushed to death"),
    "slash_spray": (0.65, [RECORDED + "flesh_rip_short"], "A cut sprays blood"),
    "blood_bursts": (0.7, SPLATS, "Blood bursts"),
    # Air, clashes, the big ones.
    "whoosh": (0.55, SWISHES, "A rush of air"),
    "dash": (0.6, SWISHES, "A dash"),
    "air_burst": (0.75, [CLASH + "heavy_air_burst"], "A burst of air"),
    # 1.3.61 (the owner: "use the new sounds I sent as the new clash sounds whenever there's an impact"): theirs.
    "clash": (0.95, [CLASHES + "clash_1", CLASHES + "clash_2", CLASHES + "clash_3", CLASHES + "clash_4"], "Kagune clash"),
    "clash_big": (0.95, [CLASH + "titan_fist_clash", CLASH + "double_shock_impact", CLASH + "sonic_fist_crash"], "A huge clash"),
    "burst": (0.7, [CLASH + "close_dry_collision", CLASH + "fast_counter_clash", CLASH + "heavy_air_burst"], "An impact bursts"),
    "shockwave": (0.85, [CLASH + "wide_air_shockwave", CLASH + "twin_fist_shockwave", CLASH + "heavy_air_burst", CLASH + "double_shock_impact"],
                  "A shockwave"),
    "quake": (0.95, [CLASH + "ground_shaking_collision", CLASH + "deep_pressure_collision"], "The ground shakes"),
    "ultimate": (1.0, [CLASH + "ultimate_fist_collision", CLASH + "thunder_punch_collision", CLASH + "sky_splitting_clash",
                       CLASH + "charged_fist_collision"], "A colossal impact"),
    "thunder": (1.0, [CLASH + "thunder_punch_collision"], "Thunder"),
    # 1.3.28: a quinque case opening (two latches, a servo, plates sliding, the lock home) and shutting (the servo back, the halves
    # meeting, the latches snapped shut).
    "case_open": (0.85, [QUINQUE + "case_open_1", QUINQUE + "case_open_2"], "A quinque case unlocks"),
    "case_close": (0.85, [QUINQUE + "case_close_1", QUINQUE + "case_close_2"], "A quinque case locks"),
    "windup_clash": (1.0, [CLASH + "windup_to_massive_clash"], "A massive clash"),
    "ground": (0.55, [CLASH + "close_dry_collision", PHIT + "knuckle_slam"], "Stone breaks"),
    # 3.3.3 (the owner: the Kakuja V2 Dragon is silent; "Get the sounds from the web, don't generate them yourself"): its own voice, real
    # creature and earth recordings (CC0, tools/sfx_recordings.json), each as recorded (no pitch change), at full gain.
    "dragon_roar": (1.0, [RECORDED + n for n in ("dragon_groan", "dragon_demon_growl", "dragon_lion_roar_1", "dragon_lion_roar_2",
                                                 "dragon_monster_growl", "dragon_beast_7")], "The Dragon roars"),
    "dragon_death": (1.0, [RECORDED + "dragon_death"], "The Dragon's death cry"),
    "dragon_crawl": (0.8, [RECORDED + "dragon_chitter", RECORDED + "dragon_stomp"], "The Dragon crawls"),
    "dragon_slam": (1.0, [RECORDED + "dragon_quake", RECORDED + "dragon_rumble", RECORDED + "dragon_stomp"], "The Dragon crashes down"),
    "dragon_bite": (1.0, [RECORDED + "dragon_crunch_1", RECORDED + "dragon_crunch_2", RECORDED + "dragon_beast_3"], "The Dragon bites"),
    "dragon_crack": (1.0, [BONE + "kagune_burst", PHIT + "bone_crack"], "The Dragon's body cracks"),
}
# One transformation each (they are long: one at a time, never pooled).
FORMS = ["pressure_awakening", "organic_armor_shift", "spectral_reveal", "energy_metamorphosis", "body_warp", "dark_aura_release",
         "heavy_morph", "overdrive_awakening", "dimensional_shift", "final_form"]
FORM_GAIN = 0.8
# Both Rinkaku half kakujas have centipede legs. Keep their event IDs, replace the loud
# transformation recordings with short real hisses (not streamed, so they can preload).
FORM_RECORDINGS = {
    "body_warp": RECORDED + "insect_hiss_1",
    "heavy_morph": RECORDED + "insect_hiss_2",
}

# The original's events, pointed at the pools (event: [(pool or "file:<path>", pitch)]).
ORIGINAL = {
    "kagune_whip": "blade_swing",
    "bone_crack": "crunch",
    "kagune_slam": "punch_hit_heavy",
    "flesh_impact": "blood_splat",
    "kagune_strain": "blade_heavy",
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
    "manifest_centipede": "centipede_manifest",
    "manifest_ukaku": "transform.spectral_reveal",
    "finger_crack": "finger_crack",
    "kagune_burst": "kagune_burst",
}


@functools.lru_cache(maxsize=None)
def levels(path):
    out = subprocess.run(["ffmpeg", "-hide_banner", "-nostats", "-i", str(path), "-af", "volumedetect", "-f", "null", "-"],
                         capture_output=True, text=True, check=True).stderr
    found = {}
    for line in out.splitlines():
        for key in ("mean_volume", "max_volume"):
            if key + ":" in line:
                found[key] = float(line.split(key + ":")[1].split()[0])
    if len(found) == 2 and all(math.isfinite(v) for v in found.values()):
        return found["mean_volume"], found["max_volume"]
    raise RuntimeError(f"no loudness for {path}")


def volume(path, gain, pool):
    # 3.0.7: the owner's finger recording is untouched and plays at full gain. Everything
    # else is lower in both mean level and peak, including the old peak-normalized pack.
    # No minimum gain: the former 0.2 floor defeated quiet transformations and eye cues.
    if pool == "finger_crack":
        return 1.0
    # 3.2.5 (the owner: "the impact sounds, like when we clash, the sound has to be way higher ... Can barely hear it. That's meant to
    # be like the loudest sound of the game"): their clash files at full gain, the huge clash too, as the finger crack is; the clash's
    # rumble well up. (They were capped at a 0.035 peak to leave room for three layers: a clash came out at a few percent.)
    if pool in {"clash", "clash_big"}:
        return 1.0
    if pool == "quake":
        return 0.6
    # 3.3.3: the Dragon's voice at full gain (its legs' clatter, heard every few ticks while it moves, at 0.55).
    if pool == "dragon_crawl":
        return 0.55
    if pool.startswith("dragon_"):
        return 1.0
    quiet = pool in {"chitin", "centipede_manifest", "transform.body_warp", "transform.heavy_morph", "writhe"}
    target_mean, peak_ceiling = (-40.0, 0.06) if quiet else (-34.0, 0.16)
    # A huge clash requests three layers at up to 2.95 each, plus a gust and rumble.
    # Reserve headroom for their sum as well as the individual sound's amplitude.
    peak_ceiling = {"clash": 0.035, "air_burst": 0.06, "quake": 0.045}.get(pool, peak_ceiling)
    mean, peak = levels(path)
    level = gain * 10.0 ** ((target_mean - mean) / 20.0)
    ceiling = peak_ceiling / 10.0 ** (peak / 20.0)
    # Round down so a rounded JSON gain never exceeds the ceiling.
    return math.floor(min(1.0, level, ceiling) * 10000.0) / 10000.0


# 3.0.2 (the owner: a "weird stop when you first pull out the Kagune"): the pull-out's sounds are loaded with the pack, not on their
# first play (the game reads a sound's file the first time it is played: the first crack of a session came late).
PRELOAD = {"finger_crack", "kagune_burst", "gush", "chitin", "centipede_manifest"}


def sound(path, gain, pool, stream=False, preload=False):
    pitch = None
    if isinstance(path, tuple):
        path, pitch = path  # 3.2.6: a file played at its own pitch in this pool
    file = SOUNDS / "sfx" / (path + ".ogg")
    if not file.is_file():
        raise RuntimeError(f"missing {file}")
    entry = {"name": "hexkagune:sfx/" + path, "volume": volume(file, gain, pool)}
    if pitch is not None:
        entry["pitch"] = pitch
    if stream:
        entry["stream"] = True
    if preload:
        entry["preload"] = True
    return entry


def event(target):
    return {"name": "hexkagune:sfx." + target, "type": "event"}


def import_recordings(directory, only=None):
    """Import verified real recordings; never generate an audio waveform. 3.3.3: {@code only} (clip names) imports just those, the others
    kept as shipped (their files, checksums and notes from the shipped SOURCES.json), so earlier clips are never re-encoded."""
    manifest = json.loads(RECORDING_MANIFEST.read_text(encoding="utf-8"))
    destination = SOUNDS / "sfx" / RECORDED
    shipped = {}
    if only:
        shipped = json.loads((destination / "SOURCES.json").read_text(encoding="utf-8"))["clips"]
        unknown = set(only) - set(manifest["clips"])
        if unknown:
            raise RuntimeError(f"no such clips: {sorted(unknown)}")
    needed = {clip["source"] for name, clip in manifest["clips"].items() if not only or name in only}
    for name, source in manifest["sources"].items():
        if name not in needed:
            continue
        path = directory / name
        if hashlib.sha256(path.read_bytes()).hexdigest() != source["sha256"]:
            raise RuntimeError(f"recording checksum mismatch: {path}")
    destination.mkdir(parents=True, exist_ok=True)
    credits = ["Recorded sound additions (3.0.7; 3.3.3 the Kakuja V2 Dragon's). No synthesized or AI-generated audio.",
               "All sources: CC0 1.0 Universal, https://creativecommons.org/publicdomain/zero/1.0/", ""]
    for name, clip in manifest["clips"].items():
        source = manifest["sources"][clip["source"]]
        if only and name not in only:
            kept = shipped.get(name)
            if kept is None or "changes" not in kept:
                raise RuntimeError(f"{name} was never imported: import it too")
            clip["output_sha256"] = kept["output_sha256"]
            clip["changes"] = kept["changes"]
            credits.extend([name + ".ogg: " + source["title"] + " by " + source["author"],
                            "Licence: " + source["licence"], "Original: " + source["original_url"],
                            "GitHub recording: " + source["download_url"], clip["changes"], ""])
            continue
        path = directory / clip["source"]
        duration = float(subprocess.check_output(["ffprobe", "-v", "error", "-show_entries", "format=duration",
                                                  "-of", "default=nw=1:nk=1", str(path)], text=True))
        start, end = clip.get("start", 0.0), clip.get("end", duration)
        if not 0.0 <= start < end <= duration:
            raise RuntimeError(f"invalid excerpt: {name}")
        filters = (f"atrim=start={start}:end={end},asetpts=PTS-STARTPTS,"
                   f"afade=t=in:st=0:d=0.003,afade=t=out:st={max(0.003, end-start-0.02)}:d=0.02")
        gain = clip.get("gain_db", 0.0)
        if gain:
            # 3.3.3: a quiet recording's level raised (its loudest sample to -3 dB at most); nothing else about it changed.
            filters += f",volume={gain}dB"
        out = destination / (name + ".ogg")
        subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", str(path), "-map_metadata", "-1", "-vn",
                        "-af", filters, "-ac", "1", "-ar", "44100", "-c:a", "libvorbis", "-q:a", "5", str(out)], check=True)
        clip["output_sha256"] = hashlib.sha256(out.read_bytes()).hexdigest()
        clip["changes"] = (f"Excerpt {start:.6f}-{end:.6f} s; 3 ms fade-in, 20 ms fade-out; " + (f"level raised {gain:g} dB; " if gain else "")
                           + "mono 44.1 kHz OGG Vorbis; tags removed. No pitch change, synthesis or layering.")
        credits.extend([name + ".ogg: " + source["title"] + " by " + source["author"],
                        "Licence: " + source["licence"], "Original: " + source["original_url"],
                        "GitHub recording: " + source["download_url"], clip["changes"], ""])
    licence = directory / "CC0-1.0.txt"
    if hashlib.sha256(licence.read_bytes()).hexdigest() != manifest["licence_sha256"]:
        raise RuntimeError("CC0 licence checksum mismatch")
    (destination / "CC0-1.0.txt").write_bytes(licence.read_bytes())
    (destination / "CREDITS.txt").write_text("\n".join(credits), encoding="utf-8")
    (destination / "SOURCES.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    print(f"Imported {len(manifest['clips'])} excerpts of verified recordings")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--import-recordings", type=pathlib.Path, metavar="SOURCE_DIRECTORY")
    parser.add_argument("--only", metavar="CLIP,CLIP", help="3.3.3: import only these clips (the others kept as shipped)")
    args = parser.parse_args()
    if args.import_recordings:
        import_recordings(args.import_recordings, args.only.split(",") if args.only else None)
    sounds = {}
    for name, (gain, files, subtitle) in POOLS.items():
        sounds["sfx." + name] = {"subtitle": "subtitles.hexkagune.sfx." + name, "sounds": [sound(f, gain, name, preload=name in PRELOAD) for f in files]}
    for form in FORMS:
        sounds["sfx.transform." + form] = {"subtitle": "subtitles.hexkagune.sfx.transform",
                                            "sounds": [sound(FORM_RECORDINGS.get(form, FORM + form), FORM_GAIN,
                                                             "transform." + form, stream=form not in FORM_RECORDINGS,
                                                             preload=form in FORM_RECORDINGS)]}
    for name, target in ORIGINAL.items():
        sounds[name] = {"replace": True, "subtitle": "subtitles.hexkagune." + name, "sounds": [event(target)]}
    for name, target in EXPANSION.items():
        sounds[name] = {"subtitle": "subtitles.hexkagune." + name, "sounds": [event(target)]}
    # The kakugan opening: the user's own earlier sound, kept.
    sounds["eye_open"] = {"subtitle": "subtitles.hexkagune.eye_open", "sounds": [{"name": "hexkagune:eye_open", "stream": False,
                           "volume": volume(SOUNDS / "eye_open.ogg", 0.5, "eye_open")}]}
    OUT.write_text(json.dumps(sounds, indent=2) + "\n", encoding="utf-8")
    used = {e["name"] for v in sounds.values() for e in v["sounds"] if "type" not in e}
    pack = {"hexkagune:sfx/" + p.relative_to(SOUNDS / "sfx").with_suffix("").as_posix() for p in (SOUNDS / "sfx").rglob("*.ogg")}
    print(f"wrote {OUT.relative_to(ROOT)}: {len(sounds)} events, {len(used & pack)} of the pack's {len(pack)} sounds in use")
    for unused in sorted(pack - used):
        print("  not used:", unused)


if __name__ == "__main__":
    main()
