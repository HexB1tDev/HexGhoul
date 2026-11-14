package com.hex.kagune.control;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** 1.2.2: every sound of the mod is the user's own pack ({@link Sfx}), placed in the world (mono), and never piled into noise ({@link SfxLimit}). */
public final class SfxTests {
    private static int passed;
    private static final List<String> FAILURES = new ArrayList<>();
    /**
     * The only vanilla sounds still played: the quest journal's own cues (the game's interface, not a fight), and 1.2.8, what the
     * pack has nothing for: a heartbeat, fire.
     */
    private static final Set<String> KEPT_VANILLA = Set.of("ui.toast.challenge_complete", "entity.player.levelup", "entity.warden.heartbeat",
        "entity.generic.burn", "block.fire.extinguish",
        // 2.0.8: the stove's crackle while the spouse cooks (the owner chose vanilla's crackle: "Use vanilla's crackle").
        "block.furnace.fire_crackle", "block.smoker.smoke", "block.campfire.crackle",
        // 1.3.2: a partner's private chimes (interface cues, as the journal's) and the night's cutscene (a candle, the sheets, a rooster).
        "entity.experience_orb.pickup", "block.note_block.chime", "block.note_block.harp", "block.bell.use", "block.candle.extinguish",
        "block.wool.step", "entity.chicken.ambient",
        // 1.3.3: the talk screen's voice, a soft blip as the words come.
        "block.note_block.bit",
        // 1.3.10: the birth's cutscene: a newborn's small cry in the dark (a kitten's mew, pitched up).
        "entity.cat.ambient");
    private static final Path SOUNDS = Path.of("src/main/resources/assets/hexkagune/sounds");

    private static void check(boolean condition, String description) {
        if (condition) {
            passed++;
        } else {
            FAILURES.add(description);
        }
    }

    public static void main(String[] args) throws IOException {
        String json = Files.readString(Path.of("src/main/resources/assets/hexkagune/sounds.json"), StandardCharsets.UTF_8);
        Set<String> events = new HashSet<>();
        Matcher key = Pattern.compile("^  \"([a-z0-9_.]+)\": \\{", Pattern.MULTILINE).matcher(json);
        while (key.find()) {
            events.add(key.group(1));
        }
        check(events.size() > 60, "the pools and events are all there: " + events.size());
        // Every pool a vanilla name is drawn from is an event of the pack.
        for (String pool : Sfx.pools()) {
            check(events.contains("sfx." + pool), "the pool " + pool + " is in sounds.json");
        }
        // Every event a sound points to is there, every file exists and is mono (a stereo sound is heard everywhere at full volume).
        Matcher name = Pattern.compile("\"name\": \"hexkagune:([a-z0-9_./]+)\"(,\\s*\"type\": \"event\")?").matcher(json);
        int files = 0;
        while (name.find()) {
            String path = name.group(1);
            if (name.group(2) != null) {
                check(events.contains(path), "the event " + path + " a sound points to is there");
                continue;
            }
            Path file = SOUNDS.resolve(path + ".ogg");
            check(Files.isRegularFile(file), "the file of " + path + " is there");
            if (Files.isRegularFile(file)) {
                check(channels(Files.readAllBytes(file)) == 1, path + " is mono");
                files++;
            }
        }
        check(files >= 100, "most of the user's pack is in use: " + files + " files");
        // 3.2.5 (the owner: the clash "is meant to be like the loudest sound of the game"): every clash file at full gain, and no sound in
        // the pack set louder than the quietest of them.
        Matcher pool = Pattern.compile("^  \"([a-z0-9_.]+)\": \\{(.*?)^  }", Pattern.MULTILINE | Pattern.DOTALL).matcher(json);
        double clashLeast = Double.MAX_VALUE;
        double othersMost = 0.0;
        while (pool.find()) {
            Matcher volume = Pattern.compile("\"volume\": ([0-9.]+)").matcher(pool.group(2));
            while (volume.find()) {
                double v = Double.parseDouble(volume.group(1));
                if (pool.group(1).equals("sfx.clash") || pool.group(1).equals("sfx.clash_big")) {
                    clashLeast = Math.min(clashLeast, v);
                } else {
                    othersMost = Math.max(othersMost, v);
                }
            }
        }
        check(clashLeast == 1.0, "every clash sound plays at full gain: " + clashLeast);
        check(othersMost <= clashLeast, "nothing in the pack is set louder than a clash: " + othersMost);
        // 3.2.6 (the owner: "Crystals sound DOES not sound like crystals, it sounds like punches"): no punch, punch collision or bone in any
        // crystal's sound. 3.3.5 (the owner: "Get the sounds from the web, don't generate them yourself"): real glass recordings, as recorded
        // (no pitch: 3.2.6's raised steel is gone), each in the pack and credited; a crystal's hit is a crystal's, not the stab's.
        String glassCredits = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of(
            "src/main/resources/assets/hexkagune/sounds/sfx/recorded/CREDITS.txt")), java.nio.charset.StandardCharsets.UTF_8);
        Matcher crystal = Pattern.compile("^  \"sfx\\.(crystal_form|crystal_hit|shatter)\": \\{(.*?)^  }", Pattern.MULTILINE | Pattern.DOTALL).matcher(json);
        int crystals = 0;
        while (crystal.find()) {
            crystals++;
            String body = crystal.group(2);
            check(!body.contains("punch_hits") && !body.contains("super_punch_collisions") && !body.contains("bone_crack") && !body.contains("punch_swings"),
                "sfx." + crystal.group(1) + " has no punch or bone in it");
            check(!body.contains("\"pitch\"") && !body.contains("spike_throws"), "sfx." + crystal.group(1) + " is not made over from other sounds (no pitch, no steel)");
            Matcher glass = Pattern.compile("sfx/recorded/(crystal_[a-z0-9_]+)\"").matcher(body);
            int recordings = 0;
            while (glass.find()) {
                recordings++;
                check(glassCredits.contains(glass.group(1) + ".ogg: "), glass.group(1) + " is credited");
            }
            check(recordings >= 3, "sfx." + crystal.group(1) + " is real glass recordings: " + recordings);
        }
        check(crystals == 3, "the crystal's three pools are there: " + crystals);
        // 3.4.4 (the owner: "The ones you added for the crystal form are fucking dog shit, bro. Just download better ones."): a crystal forming
        // is a rubbed wine glass singing and a small bell ringing, never 3.3.5's glasses clinking, cup and saucer or glass break's crack.
        Matcher form = Pattern.compile("^  \"sfx\\.crystal_form\": \\{(.*?)^  }", Pattern.MULTILINE | Pattern.DOTALL).matcher(json);
        check(form.find() && !form.group(1).contains("crystal_clink") && !form.group(1).contains("crystal_crack")
            && form.group(1).split("crystal_form_", -1).length - 1 >= 4, "a crystal forms with its own recordings, not kitchen clinks");
        check(glassCredits.contains("crystal_form_1.ogg: Glass rub") && glassCredits.contains("by ani_music")
            && glassCredits.contains("crystal_form_4.ogg: Bell") && glassCredits.contains("by AlaskaRobotics")
            && glassCredits.contains("sonic-pi-net/sonic-pi"), "the glass and the bell are credited, with where they were downloaded");
        // 3.3.3 (the owner: the Kakuja V2 Dragon is silent; "Get the sounds from the web, don't generate them yourself"): its own six pools,
        // loud, every sound as recorded (no pitch: real recordings, not made over), each subtitled; its voice credited CC0 recordings.
        Matcher dragon = Pattern.compile("^  \"sfx\\.(dragon_[a-z]+)\": \\{(.*?)^  }", Pattern.MULTILINE | Pattern.DOTALL).matcher(json);
        int dragons = 0;
        String lang = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("src/main/resources/assets/hexkagune/lang/en_us.json")),
            java.nio.charset.StandardCharsets.UTF_8);
        String credits = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("src/main/resources/assets/hexkagune/sounds/sfx/recorded/CREDITS.txt")),
            java.nio.charset.StandardCharsets.UTF_8);
        while (dragon.find()) {
            dragons++;
            String dragonPool = dragon.group(1);
            String body = dragon.group(2);
            Matcher volume = Pattern.compile("\"volume\": ([0-9.]+)").matcher(body);
            double least = Double.MAX_VALUE;
            while (volume.find()) {
                least = Math.min(least, Double.parseDouble(volume.group(1)));
            }
            check(least >= (dragonPool.equals("dragon_crawl") ? 0.5 : 1.0), "sfx." + dragonPool + " is loud: " + least);
            check(!body.contains("\"pitch\""), "sfx." + dragonPool + " plays its recordings as recorded (no pitch change)");
            check(lang.contains("\"subtitles.hexkagune.sfx." + dragonPool + "\""), "sfx." + dragonPool + " has a subtitle");
            if (!dragonPool.equals("dragon_crack")) {
                Matcher file = Pattern.compile("sfx/recorded/(dragon_[a-z0-9_]+)\"").matcher(body);
                int dragonFiles = 0;
                while (file.find()) {
                    dragonFiles++;
                    check(credits.contains(file.group(1) + ".ogg: ") && java.nio.file.Files.isRegularFile(java.nio.file.Path.of(
                        "src/main/resources/assets/hexkagune/sounds/sfx/recorded/" + file.group(1) + ".ogg")), file.group(1) + " is there and credited");
                }
                check(dragonFiles >= 1, "sfx." + dragonPool + " is the web's recordings: " + dragonFiles);
            }
        }
        check(dragons == 6, "the Dragon's six pools are there: " + dragons);
        check(credits.contains("Licence: CC0-1.0") && !credits.contains("pitch changed"), "the Dragon's recordings are credited, CC0");
        check(Sfx.play("block.amethyst_block.hit", 1.0F, 1.0F).path().equals("sfx.crystal_hit")
            && Sfx.play("block.amethyst_block.break", 1.0F, 1.0F).path().equals("sfx.shatter"), "a crystal's hit and break play the crystal's pools");
        // Every vanilla name the mod's code plays is drawn from the pack (but the journal's cues).
        Pattern literal = Pattern.compile("\"((?:entity|block|item|ui)\\.[a-z_.]+)\"");
        Set<String> named = new HashSet<>();
        for (String root : new String[] {"src/main/java", "awakening/src"}) {
            try (Stream<Path> walk = Files.walk(Path.of(root))) {
                for (Path source : walk.filter(p -> p.toString().endsWith(".java")).toList()) {
                    Matcher m = literal.matcher(Files.readString(source, StandardCharsets.UTF_8));
                    while (m.find()) {
                        String id = m.group(1);
                        if (!id.startsWith("item.hexkagune") && !id.startsWith("entity.hexkagune") && !id.startsWith("block.hexkagune")) {
                            named.add(id);
                        }
                    }
                }
            }
        }
        check(named.size() > 40, "the mod's vanilla names were all read: " + named.size());
        for (String id : named) {
            check(Sfx.remapped(id) || KEPT_VANILLA.contains(id), "\"" + id + "\" is drawn from the user's pack");
        }
        // The remap: pools by volume, pitches drawn back, names with a namespace as they are.
        Sfx.Sound crack = Sfx.play("entity.generic.explode", 0.5F, 1.4F);
        Sfx.Sound wave = Sfx.play("entity.generic.explode", 1.2F, 1.0F);
        Sfx.Sound blast = Sfx.play("entity.generic.explode", 2.4F, 0.6F);
        check(crack.path().equals("sfx.burst") && wave.path().equals("sfx.shockwave") && blast.path().equals("sfx.ultimate"),
            "a little crack, a shockwave and the ground splitting: " + crack.path() + " " + wave.path() + " " + blast.path());
        check(crack.namespace().equals("hexkagune") && Math.abs(blast.pitch() - (1.0F + (0.6F - 1.0F) * Sfx.KEEP)) < 1.0E-6F,
            "the pitch kept only in part: " + blast.pitch());
        check(Sfx.pitch(0.1F) == Sfx.LOWEST && Sfx.pitch(5.0F) == Sfx.HIGHEST && Sfx.pitch(Float.NaN) == 1.0F, "pitches kept in bounds");
        Sfx.Sound own = Sfx.play("hexkagune:sfx.clash", 2.0F, 0.6F);
        check(own.namespace().equals("hexkagune") && own.path().equals("sfx.clash") && own.pitch() == 0.6F, "a name of the pack's own plays as asked");
        // 1.2.8: never metal on a swing, never blood for a heartbeat or fire.
        check(!Sfx.remapped("entity.warden.heartbeat") && !Sfx.remapped("entity.generic.burn") && !Sfx.remapped("block.fire.extinguish"),
            "a heartbeat and fire stay vanilla's (the pack has none)");
        Sfx.Sound journal = Sfx.play("ui.toast.challenge_complete", 0.6F, 1.0F);
        check(journal.namespace().equals("minecraft") && journal.pitch() == 1.0F, "the journal's cue stays the game's");
        limit();
        ukakuVanilla491();
        kaguneVanilla493();
        if (!FAILURES.isEmpty()) {
            FAILURES.stream().limit(40).forEach(failure -> System.err.println("FAILED: " + failure));
            System.err.println(FAILURES.size() + " of " + (passed + FAILURES.size()) + " checks failed");
            System.exit(1);
        }
        System.out.println("Sound tests: " + passed + " checks passed");
    }

    /** The Vorbis identification header's channel count (the first page: "\1vorbis", version, channels). */
    static int channels(byte[] ogg) {
        byte[] tag = {1, 'v', 'o', 'r', 'b', 'i', 's'};
        outer:
        for (int i = 0; i + 12 < ogg.length && i < 4096; i++) {
            for (int k = 0; k < tag.length; k++) {
                if (ogg[i + k] != tag[k]) {
                    continue outer;
                }
            }
            return ogg[i + 11] & 0xFF;
        }
        return -1;
    }

    /**
     * 4.1.91 (the owner: "rework the the Ukaku sounds to use only Minecraft sounds that are ideal for every single thing that form does.
     * The only thing you're not allowed to touch is the clash sounds. ... change every sound in the Ukaku so that every sound is Minecraft
     * based. Just make sure that you pick the most badass sounds possible."): every sound the Ukaku makes is one of the game's own (in
     * vanilla 1.20.1's sounds.json, tools/vanilla_sounds_1.20.1.txt), named with its namespace so the pack never stands in for it; the clash
     * keeps its own.
     */
    static void ukakuVanilla491() throws IOException {
        Set<String> vanilla = new HashSet<>(Files.readAllLines(Path.of("tools/vanilla_sounds_1.20.1.txt"), StandardCharsets.UTF_8));
        check(vanilla.size() > 1400 && vanilla.contains("entity.warden.attack_impact") && vanilla.contains("block.amethyst_block.chime"),
            "vanilla 1.20.1's sound events are all there: " + vanilla.size());
        // ALL holds every one of them (none escapes the check below).
        Set<String> declared = new HashSet<>();
        for (java.lang.reflect.Field field : com.hex.kagune.control.ukaku.UkakuSounds.class.getFields()) {
            if (field.getType() == String.class && java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                try {
                    declared.add((String)field.get(null));
                } catch (IllegalAccessException failure) {
                    check(false, "read " + field.getName());
                }
            }
        }
        check(declared.size() > 30 && declared.equals(new HashSet<>(java.util.Arrays.asList(com.hex.kagune.control.ukaku.UkakuSounds.ALL))),
            "UkakuSounds.ALL lists every Ukaku sound: " + declared.size());
        for (String id : com.hex.kagune.control.ukaku.UkakuSounds.ALL) {
            check(id.startsWith("minecraft:") && vanilla.contains(id.substring("minecraft:".length())), id + " is one of the game's own sounds");
            Sfx.Sound played = Sfx.play(id, 0.7F, 0.6F);
            check(played.namespace().equals("minecraft") && ("minecraft:" + played.path()).equals(id) && played.pitch() == 0.6F && played.volume() == 0.7F,
                id + " plays as named, at the volume and pitch asked (never the pack's)");
        }
        // Every sound call in the Ukaku's own files names one of them; nothing of the pack's, no bare name the pack would stand in for.
        String server = "src/main/java/com/hex/kagune/control/server/";
        for (String file : new String[] {"UkakuFire", "UkakuServer", "UkakuRush", "UkakuCombat", "UkakuGatling", "CrystalShots", "UkakuVortex",
            "UkakuThorns", "UkakuCrystalCombat", "UkakuPyre", "NpcWings"}) {
            String source = Files.readString(Path.of(server + file + ".java"), StandardCharsets.UTF_8);
            int calls = 0;
            boolean named = true;
            for (String line : source.split("\n")) {
                if (line.matches(".*\\bsound\\(.*") && !line.contains("static void sound(")) {
                    calls++;
                    named &= line.contains("UkakuSounds.");
                }
            }
            check(calls > 0 && named, file + ": every one of its " + calls + " sounds is the game's own (UkakuSounds)");
            check(!source.contains("\"hexkagune:") && !source.contains("HexKaguneSounds") && !source.matches("(?s).*\"(entity|block|item)\\.[a-z_.]+\".*"),
                file + ": nothing of the pack's, and no bare vanilla name the pack would draw from");
        }
        String combat = Files.readString(Path.of(server + "KaguneCombat.java"), StandardCharsets.UTF_8);
        String ukakuCombat = Files.readString(Path.of(server + "UkakuCombat.java"), StandardCharsets.UTF_8);
        check(combat.contains("UkakuCombat.hit(player, target, stage.weight() >= 0.5F);") && combat.contains("UkakuCombat.slam(player, impactPoint);")
            && ukakuCombat.contains("heavy ? UkakuSounds.SLAP_HEAVY") && ukakuCombat.contains("UkakuSounds.SLAP, 1.0F")
            && ukakuCombat.contains("UkakuFire.sound(player.m_284548_(), at, UkakuSounds.SLAM, 1.0F"),
            "the Ukaku's blows land with a slap (a heavy one throwing them), its slams with the Warden's blow");
        check(ukakuCombat.contains("case KICK -> UkakuFire.sound(level, at, UkakuSounds.KICK,")
            && ukakuCombat.contains("default -> UkakuFire.sound(level, at, UkakuSounds.WING_FLAP, 0.75F + 0.35F * weight,")
            && ukakuCombat.contains("case WING_SLASH, WING_CROSS, ONE_WING_SLASH, ONE_WING_RETURN, BEHEADING -> {"),
            "its swings: a kick's swish, the wings' heavy beat for a slap, a blade's sweep with it for a cut");
        String forms = Files.readString(Path.of(server + "UkakuForms.java"), StandardCharsets.UTF_8);
        check(forms.contains("static final String UKAKU_KAKUJA_SOUND = com.hex.kagune.control.ukaku.UkakuSounds.KAKUJA;")
            && forms.contains("static final String FINAL_KAKUJA_SOUND = com.hex.kagune.control.ukaku.UkakuSounds.FINAL_KAKUJA;")
            && forms.contains("ukaku ? com.hex.kagune.control.ukaku.UkakuSounds.KAKUJA_OFF : com.hex.kagune.control.RinkakuSounds.KAKUJA_OFF"),
            "the Ukaku's kakuja, final kakuja and coming out of them: the game's own");
        check(Files.readString(Path.of("src/main/java/com/hex/kagune/control/client/ManifestSounds.java"), StandardCharsets.UTF_8)
                .contains("UKAKU = SoundEvent.m_262824_(ResourceLocation.m_214293_(\"minecraft\", com.hex.kagune.control.ukaku.UkakuSounds.MANIFEST.substring(")
            && Files.readString(Path.of(server + "AyatoPullOut.java"), StandardCharsets.UTF_8).contains("UkakuSounds.MANIFEST, 1.4F")
            && Files.readString(Path.of(server + "KakujaPassive.java"), StandardCharsets.UTF_8).contains("ukaku ? com.hex.kagune.control.ukaku.UkakuSounds.SHADOW_STEP"),
            "the wings tearing out (a player's, Ayato's) and the Steel Blade's vanish: the game's own");
        // The clash is not the Ukaku's: it keeps the pack's own.
        String clash = Files.readString(Path.of(server + "KaguneClash.java"), StandardCharsets.UTF_8);
        check(clash.contains("UkakuFire.sound(level, at, \"hexkagune:sfx.clash\", volume,") && !clash.contains("UkakuSounds"),
            "the clash keeps its own sound, untouched");
    }

    /** Every public String constant of {@code type} (a sound table). */
    static Set<String> declared(Class<?> type) {
        Set<String> out = new HashSet<>();
        for (java.lang.reflect.Field field : type.getFields()) {
            if (field.getType() == String.class && java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                try {
                    out.add((String)field.get(null));
                } catch (IllegalAccessException failure) {
                    check(false, "read " + field.getName());
                }
            }
        }
        return out;
    }

    /**
     * 4.1.93 (the owner: "now you're gonna do the same sound thing that you just did for ukaku. You're gonna do for both of the other
     * cognates completely. Replace all sounds. Just make sure not to use the same ones as ukaku. Like that's a no go. The only one you're
     * allowed to use is the transformation in kakuja, the wither sound, if you want."): the Rinkaku's and the Bikaku's every sound the
     * game's own, none the Ukaku's (but the Wither's birth), each kagune its own; the original's sounds by the player's kagune.
     */
    static void kaguneVanilla493() throws IOException {
        Set<String> vanilla = new HashSet<>(Files.readAllLines(Path.of("tools/vanilla_sounds_1.20.1.txt"), StandardCharsets.UTF_8));
        String wither = "minecraft:entity.wither.spawn";
        Set<String> ukaku = new HashSet<>(java.util.Arrays.asList(com.hex.kagune.control.ukaku.UkakuSounds.ALL));
        Set<String> rinkaku = new HashSet<>(java.util.Arrays.asList(RinkakuSounds.ALL));
        Set<String> bikaku = new HashSet<>(java.util.Arrays.asList(BikakuSounds.ALL));
        check(declared(RinkakuSounds.class).equals(rinkaku) && rinkaku.size() >= 20, "RinkakuSounds.ALL lists every one: " + rinkaku.size());
        check(declared(BikakuSounds.class).equals(bikaku) && bikaku.size() >= 18, "BikakuSounds.ALL lists every one: " + bikaku.size());
        for (Set<String> table : java.util.List.of(rinkaku, bikaku)) {
            for (String id : table) {
                check(id.startsWith("minecraft:") && vanilla.contains(id.substring("minecraft:".length())), id + " is one of the game's own sounds");
                Sfx.Sound played = Sfx.play(id, 0.7F, 0.6F);
                check(("minecraft:" + played.path()).equals(id) && played.namespace().equals("minecraft") && played.pitch() == 0.6F,
                    id + " plays as named (never the pack's)");
                check(id.equals(wither) || !ukaku.contains(id), id + " is not one of the Ukaku's");
            }
        }
        for (String id : rinkaku) {
            check(id.equals(wither) || !bikaku.contains(id), id + " is the Rinkaku's alone, not the Bikaku's too");
        }
        // The original's sixteen events, by kagune: every one the kagune's own, the other paths as they were.
        check(KaguneVoices.EVENTS.length == 16, "the original's sixteen kagune events");
        for (String event : KaguneVoices.EVENTS) {
            check(rinkaku.contains(KaguneVoices.of(KagunePath.RINKAKU, event)), "a Rinkaku's " + event + " is the Rinkaku's own: " + KaguneVoices.of(KagunePath.RINKAKU, event));
            check(bikaku.contains(KaguneVoices.of(KagunePath.BIKAKU, event)), "a Bikaku's " + event + " is the Bikaku's own: " + KaguneVoices.of(KagunePath.BIKAKU, event));
            check(ukaku.contains(KaguneVoices.of(KagunePath.UKAKU, event)), "an Ukaku's " + event + " is the Ukaku's own: " + KaguneVoices.of(KagunePath.UKAKU, event));
            check(KaguneVoices.of(KagunePath.PALE, event) == null && KaguneVoices.of(KagunePath.GRAFT, event) == null && KaguneVoices.of(KagunePath.NONE, event) == null,
                "Owl DNA, Graft DNA and no path keep the original's " + event);
        }
        check(KaguneVoices.of(KagunePath.RINKAKU, "some_other_sound") == null && KaguneVoices.of(null, "kagune_whip") == null, "any other sound as it was");
        check(RinkakuSounds.WHIP.equals(KaguneVoices.of(KagunePath.RINKAKU, "kagune_whip")) && BikakuSounds.SNAP.equals(KaguneVoices.of(KagunePath.BIKAKU, "kagune_grab"))
            && RinkakuSounds.WRITHE.equals(KaguneVoices.of(KagunePath.RINKAKU, "kagune_idle"))
            && com.hex.kagune.control.ukaku.UkakuSounds.SNUFF.equals(KaguneVoices.of(KagunePath.UKAKU, "kagune_dismiss")), "the lash a whip's crack, the worm's grab a fox's bite, the writhe the sensor's clicking, the wings put away a fire put out");
        String server = "src/main/java/com/hex/kagune/control/server/";
        String voice = Files.readString(Path.of(server + "KaguneVoice.java"), StandardCharsets.UTF_8);
        boolean named = true;
        for (String event : KaguneVoices.EVENTS) {
            named &= voice.contains("map.put(HexKaguneSounds." + event.toUpperCase(java.util.Locale.ROOT) + ", \"" + event + "\");");
        }
        check(named && voice.contains("KaguneVoices.of(PathServer.path(player), eventName(sound))") && voice.contains("KaguneServerController.playAtLegacy(player, at, sound, volume, pitch);"),
            "each of the original's events known by its field; the other paths and sounds played as before");
        // The jar: the original's playAt goes through KaguneVoice, its writhing loop through KaguneVoiceClient.
        byte[] controller = SfxTests.class.getClassLoader().getResourceAsStream("com/hex/kagune/gameplay/KaguneServerController.class").readAllBytes();
        byte[] writhe = SfxTests.class.getClassLoader().getResourceAsStream("com/hex/kagune/client/KaguneAmbientSounds$WritheSound.class").readAllBytes();
        String controllerText = new String(controller, StandardCharsets.ISO_8859_1);
        String writheText = new String(writhe, StandardCharsets.ISO_8859_1);
        check(controllerText.contains("com/hex/kagune/control/server/KaguneVoice") && controllerText.contains("playAtLegacy"),
            "the patched controller's playAt goes through KaguneVoice (its own kept as playAtLegacy)");
        check(writheText.contains("com/hex/kagune/control/client/KaguneVoiceClient") && !writheText.contains("KAGUNE_IDLE"),
            "the writhing loop takes its sound from the player's kagune");
        check(Files.readString(Path.of("build.py"), StandardCharsets.UTF_8).contains("\"com/hex/kagune/client/KaguneAmbientSounds$WritheSound.class\","),
            "the jar's checker expects the writhing loop changed");
        String patch = Files.readString(Path.of("patch/KagunePatch.java"), StandardCharsets.UTF_8);
        check(patch.contains("trampolineExact(node, \"playAt\", PLAY_AT_DESC, \"com/hex/kagune/control/server/KaguneVoice\", \"playAt\");")
            && patch.contains("patchWritheSound(writhe);") && patch.contains("expect(changed, 1, \"writhe sound by kagune\");"),
            "the patcher sends the controller's playAt and the writhing loop's sound to the kagune's");
        // The Rinkaku's and the Bikaku's own files: every sound of theirs named from their table; nothing of the pack's but the clash.
        String[][] owners = {{"ImpaleServer", "RinkakuSounds."}, {"HurlServer", "RinkakuSounds."}, {"RinkakuBlockServer", "RinkakuSounds."},
            {"RinkakuGrab", "RinkakuSounds."}, {"NpcTendrils", "RinkakuSounds."}, {"BikakuMoves", "BikakuSounds."}, {"BikakuDevour", "BikakuSounds."},
            {"BikakuServer", "BikakuSounds."}, {"BikakuPotential", "BikakuSounds."}};
        for (String[] owner : owners) {
            String source = Files.readString(Path.of(server + owner[0] + ".java"), StandardCharsets.UTF_8);
            int calls = 0;
            boolean theirs = true;
            for (String line : source.split("\n")) {
                if ((line.contains("UkakuFire.sound(") || line.contains("KaguneVoice.event(")) && !line.contains("sfx.clash")) {
                    calls++;
                    theirs &= line.contains(owner[1]);
                }
            }
            check(calls > 0 && theirs, owner[0] + ": every one of its " + calls + " sounds is its kagune's own (" + owner[1] + ")");
            String rest = source.replace("\"hexkagune:sfx.clash_big\"", "");
            check(!rest.contains("\"hexkagune:") && !rest.contains("HexKaguneSounds.KAGUNE_IDLE") && !rest.matches("(?s).*UkakuFire\\.sound\\([^;]*\"(entity|block|item)\\.[a-z_.]+\".*"),
                owner[0] + ": nothing of the pack's (but the clash), no bare name the pack would draw from");
        }
        String npc = Files.readString(Path.of(server + "GhoulNpcCombat.java"), StandardCharsets.UTF_8);
        check(npc.contains("KaguneVoice.event(com.hex.kagune.control.RinkakuSounds.STRAIN)") && npc.contains("KaguneVoice.event(com.hex.kagune.control.RinkakuSounds.WHIP)")
            && npc.contains("KaguneVoice.event(com.hex.kagune.control.RinkakuSounds.SLAM)") && npc.contains("KaguneVoice.event(com.hex.kagune.control.RinkakuSounds.IMPACT)")
            && npc.contains("com.hex.kagune.control.ukaku.UkakuSounds.WING_FLAP, 0.9F, 1.6F") && npc.contains("com.hex.kagune.control.ukaku.UkakuSounds.SHARD, 0.5F,")
            && npc.contains("com.hex.kagune.control.ukaku.UkakuSounds.BURN, 0.5F, 1.2F") && npc.contains("\"hexkagune:sfx.blade_heavy\""),
            "a wild ghoul's Rinkaku and Ukaku sound as the players' do (its Koukaku's blade keeps its own)");
        String forms = Files.readString(Path.of(server + "UkakuForms.java"), StandardCharsets.UTF_8);
        check(forms.contains("static final String CENTIPEDE_SOUND = com.hex.kagune.control.RinkakuSounds.CENTIPEDE;")
            && forms.contains("static final String RINKAKU_KAKUJA_SOUND = com.hex.kagune.control.RinkakuSounds.FULL_KAKUJA;")
            && forms.contains("centipede ? com.hex.kagune.control.RinkakuSounds.CHITIN_OFF : ukaku ? com.hex.kagune.control.ukaku.UkakuSounds.KAKUJA_OFF : com.hex.kagune.control.RinkakuSounds.KAKUJA_OFF;"),
            "the Rinkaku's centipede, full kakuja and coming out of them: the game's own");
        String client = "src/main/java/com/hex/kagune/control/client/";
        check(Files.readString(Path.of(client + "ManifestSounds.java"), StandardCharsets.UTF_8).contains("SoundEvent own = ukaku ? null : KaguneVoiceClient.tear(player, centipede);")
            && Files.readString(Path.of(client + "fx/KakujaFx.java"), StandardCharsets.UTF_8).contains("KaguneVoiceClient.kakujaBreak(player)")
            && Files.readString(Path.of(client + "KaguneVoiceClient.java"), StandardCharsets.UTF_8).contains("return event(centipede ? RinkakuSounds.CHITTER : RinkakuSounds.MANIFEST);"),
            "the tendrils tearing out and a kakuja breaking: each kagune's own");
        check(Files.readString(Path.of(server + "KaguneClash.java"), StandardCharsets.UTF_8).contains("UkakuFire.sound(level, at, \"hexkagune:sfx.clash\", volume,")
            && Files.readString(Path.of(server + "RinkakuBlockServer.java"), StandardCharsets.UTF_8).contains("\"hexkagune:sfx.clash_big\""),
            "the clash untouched, the Rinkaku's guard's too");
    }

    static void limit() {
        SfxLimit limit = new SfxLimit();
        long ms = 1_000_000L;
        check(limit.allow("kagune_whip", 0, 64, 0, 0) && !limit.allow("kagune_whip", 0.5, 64, 0, 20 * ms), "the same sound on itself is let go");
        check(limit.allow("kagune_whip", 30, 64, 0, 20 * ms), "the same sound somewhere else plays");
        check(limit.allow("kagune_slam", 0, 64, 0, 20 * ms), "another sound in the same place plays");
        check(limit.allow("kagune_whip", 0, 64, 0, 60 * ms) && limit.allow("kagune_whip", 0, 64, 0, 120 * ms)
            && !limit.allow("kagune_whip", 0, 64, 0, 180 * ms), "no more than three of one sound near each other at once");
        check(limit.allow("kagune_whip", 0, 64, 0, 500 * ms), "and again once they are over");
        SfxLimit crowd = new SfxLimit();
        int played = 0;
        for (int i = 0; i < 40; i++) {
            played += crowd.allow("sound" + i, i * 10.0, 64, 0, i * ms) ? 1 : 0;
        }
        check(played == SfxLimit.ALL, "no more than " + SfxLimit.ALL + " of the mod's sounds begin at once: " + played);
        check(crowd.allow("late", 0, 64, 0, 400 * ms), "the crowd over, sounds play again");
    }
}
