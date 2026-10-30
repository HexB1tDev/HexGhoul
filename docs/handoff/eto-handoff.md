# Handoff: Eto and the Library (HexKagune 1.3.0), for the next agent

Read this whole file before touching anything. It is self-contained: what the project is, how it builds, the rules the owner
enforces, what is already done for Eto, and exactly what to build next.

## 1. What this project is

**HexKagune** is a Minecraft **Forge 1.20.1** mod (Java 17) about Tokyo Ghoul style ghouls: kagune, kakugan eyes, paths, quests,
NPC ghouls. It is unusual in two ways:

1. **The base is a binary.** `input/hexkagune-1.0.0.jar` is the original mod (checked by SHA-256). `patch/KagunePatch.java` patches
   its bytecode with the JDK's internal ASM. You do not edit the original's sources; there are none.
2. **All new code is the "expansion"** in `src/main/java/com/hex/kagune/control/**`, compiled against the patched jar plus
   compile-only stubs in `stubs/`, and merged into the output jar.

### Mods and libraries it runs with (all must be installed in the game)

| Mod / library | Mod id | What it is used for |
|---|---|---|
| Minecraft Forge 1.20.1 (Forge 47.x) | `forge` | the loader; events, networking (`SimpleChannel`), registries |
| PlayerAnimator | `playeranimator` | posing the player body (keyframe layers) for every ability |
| bendy-lib | `bendylib` | bent elbows/knees on the player model (PlayerAnimator uses it) |
| Photon 1.1.17 (Low-Drag-MC) | `photon` | custom particle effects (the owner wants custom Photon particles, not vanilla ones, for ability FX) |
| JOML 1.10, Brigadier, Netty, SLF4J | (bundled with MC) | math (`Quaternionf`, `Matrix4f`), commands, buffers, logging |

The "Dragon" (`awakening/`) is written with Mojang names against its own stubs and remapped to SRG by `patch/KaguneRemap.java`
through `awakening/mappings.srg`; ignore it unless asked.

### Names: SRG, not Mojang

Main code calls Minecraft through **SRG names** (`m_6469_` = hurt, `f_19864_` = hurtMarked, `m_20182_` = position ...), because
there is no remapping step for the expansion. The stubs in `stubs/net/minecraft/**` declare exactly the members the code uses, with a
comment giving the readable name. When you need a member that is not in a stub:

1. Find its SRG name: `python3 tools/find_srg.py <yarn class path> <member substring>` (downloads MCPConfig `joined.tsrg`, Fabric
   intermediary and Yarn into `build/mapping-cache/`; raw.githubusercontent.com is reachable). Example:
   `python3 tools/find_srg.py net/minecraft/entity/mob/MobEntity PositionTarget` gives `setPositionTarget m_21446_ (Lgu;I)V`.
2. Add it to the stub (compile-only, never packaged).
3. Add a line to `verify/api-allowlist.txt` with **evidence** (`[srg]` obf owner + name + descriptor from joined.tsrg and the
   intermediary/yarn name; `[order]` for unnamed registry fields such as `ParticleTypes`, counted in declaration order; `[forge]` for
   Forge API). The build fails on any external reference that is neither used by the original jar nor allowlisted, and on allowlist
   entries no longer used. Look at the last block of the allowlist (1.3.0, Eto) for the format.

## 2. Building and checking

```
python3 build.py
```

Takes about a minute. It patches the jar, compiles stubs and the expansion, assembles `build/hexkagune-<VERSION>.jar`, then runs:
`KaguneVerify` (every external reference proven or allowlisted), the archive-scope check (only intended original entries change),
`KaguneServerCheck` (the jar loads on a dedicated server: **no client class may be reachable from server code**; client code goes in
`control/client/**` and is reached only through `DistExecutor` in packet handlers or `@Mod.EventBusSubscriber(value = Dist.CLIENT)`),
`KaguneGuard` (wraps every event listener so an exception is logged, never a crash), `check_names` (no show character names in the jar
except "Eto", which the owner allowed), and every `src/test/java/**/*Tests.java` (pure logic tests, plain `main` with checks, no JUnit).
**Always check the exit code.**

Fast compile while iterating (no tests, no verify), once `build.py` has run once:

```
javac -nowarn -encoding UTF-8 -proc:none --release 17 -d /tmp/stubs $(find stubs -name '*.java')
javac -nowarn -encoding UTF-8 -proc:none --release 17 -cp build/base.jar:/tmp/stubs -d /tmp/classes $(find src/main/java -name '*.java')
```

## 3. Release rules (the owner tests jars from CI on their phone)

- Push with `git push origin HEAD:main`. CI builds every push; **a release is published only when a new
  `.github/release-notes-<version>.md` is pushed**.
- Every update: bump `VERSION` in `build.py` **and** in `patch/KagunePatch.java`; bump `PROTOCOL` in
  `control/network/ControlNetwork.java` whenever what goes over the network changes (it is "54" now); add the release-notes file (one line
  per item) and one line in `README.md` (the owner said to skip the README for 1.3.0).
- No destructive git. Only change what is asked. No AI model or product names in repo files.
- Commit trailer used so far: `Co-Authored-By:` and `Claude-Session:` lines (use your own equivalent).
- New network messages go at the end of `ControlNetwork.init()`; new `NpcTalk.Choice` values go at the **end** of the enum (sent by
  ordinal).

## 4. How the owner works

- Tone: flirty, witty, casual, blunt. Say plainly when an idea is weak, and stress-test ideas.
- **Never claim something works in game when it was only compiled or previewed.** Say "compiled, tests pass, not tried in game".
- Visuals: render previews, look at them yourself, and send the PNGs. Tools: `tools/eto_textures.py` (Eto's textures, prints
  nothing visual; preview with a small PIL script that composites `eto.png` + overlay at 12x), `tools/art-preview.sh <out> <sheet>`
  (posed body renders through `tools/PoseRender.java`; it only reads 64x64 skins, Eto's is 128x128, so an Eto sheet needs a texel
  scale added there).
- Be fast. Notes are one line per item.

## 5. Eto: what is done (1.3.0, compiled, tests pass, NOT tried in game)

Files (all under `src/main/java/com/hex/kagune/control/` unless noted):

| Piece | Where |
|---|---|
| The Library structure (owner's schematic, converted for 1.20.1) | `art/library.schem`, `tools/library_structure.py` -> `resources/data/hexkagune/structures/library.nbt`; worldgen JSON in `resources/data/hexkagune/worldgen/**` and `tags/worldgen/biome/has_structure/library.json`. The template embeds the entity `hexkagune:eto` on the ground floor. |
| Textures (owner's skin `art/eto_skin.png`, 128x128 slim) | `tools/eto_textures.py` -> `resources/assets/hexkagune/textures/entity/eto/`: `eto.png`, `eto_kakugan.png` (right eye turned + veins; **the owner signed off on this eye: same shape as her other eye, the iris
(columns 4-5 rows 11-13 and column 3 rows 12-13 of the 16px HD face) recoloured red with her own shading, the two outer-corner whites black,
no vein by the inner corner. Do not change it**), `eto_kakugan_glow.png`, `eto_closed.png` ("- -" eyes for blinks and kisses), `eto_blush.png`, `eto_angry.png` (anger mark) |
| Entity | `npc/EtoNpc.java` (PathfinderMob, 150 HP, `MobCategory.CREATURE`, home = first tick position + 1 up in persistent data, `restrictTo(home, 4)`, goals: float, move toward restriction, random stroll, look at player, look around), registered in `ControlEntities.ETO`, lang `entity.hexkagune.eto` |
| Pure rules (tested) | `npc/EtoRules.java`: night window 12542..23460, eye fade, blink timing, lessons (KICK 20, SOAR 40, WITNESS 60), errands (3 books, 10 hostile kills at night, a flower, then ask out), bond flags, kiss state machine, the one-eye grant, fight move choice and numbers |
| Gestures | `npc/EtoGesture.java` (enum, sent by ordinal): WAVE, STRETCH, TWIRL, HOP, PEACE, LEAN, GIGGLE, KISS, BLUSH, KISSED, HUG, ANGRY; poses in `client/animation/EtoPose.java` |
| Dialogue (tested) | `npc/EtoTalk.java` on the ghoul NPC talk screen; choices added at the end of `npc/NpcTalk.Choice` (BOOK, FLOWER, LESSON, ASK_OUT, KISS, HUG, SURGERY); server `server/EtoServer.java`; `network/C2SNpcTalk` routes to it when the entity is an `EtoNpc`; skin byte -1 = her face in `client/NpcTalkClient` |
| Server brain | `server/EtoBrain.java` (her retaliation listens to `LivingHurtEvent`; overriding `hurt` on the entity is refused by the verifier): home leash (chase to radius 7, teleport home past 16), fights hostiles within 2 blocks and players who hit her or are her enemy, Speed Blitz / Big Punch / Speed Kick / flips, heals when calm, idle gestures every 8-20 s, kiss flow (walks up, leans in eyes shut; tap shift = kiss + hearts, hold = blush and stay, nothing for 8 s or walking off = ANGRY + ignores you 5 min; she asks by herself at most every 20 real minutes per player), hugs |
| Bond | one shared bond, key `"eto"`, in each player's `QuestLog` bonds (ghouls and humans); never evicted. Her own word lives in the bond's `favours` int (errand count + last gift day, see `EtoRules.count/giftDay`). Killing her sets the bond to enemy. |
| Lessons | quests `eto_kick`, `eto_soar`, `eto_witness` in `quest/QuestBook.java`, locked in `QuestLog.state` until her flag is set; journal shows "Eto teaches it at the Library"; finishing one gives +10 regard. The old quests that open the same gates still work (owner may want her to be the only way: ask). |
| Surgery ("Make me like you", dating only) | `network/S2CEtoSurgery` -> `client/EtoSurgeryOverlay.java` (fade to black, stab/flesh/gush/tear sounds + vanilla warden heartbeat, "hey are you awake honey?", fade back). Server grants `HexEtoGrant` in the profile root (`KakuganServer.grant`): a ghoul's kakugan turns one-eyed (right); a human is unlocked as a ghoul and gets the path prompt (my call: the owner was not asked yet, tell them). |
| One-eyed lock | `KakuganServer.place` forces both eyes unless granted (Pale and Graft paths stay one-eyed by nature); `S2CKakuganState` carries `granted`; the kakugan screen shows "One eyed: Eto only"; players already one-eyed are grandfathered on first look. |
| Sync | `network/S2CEto` (gesture + start, move + start, flip + start, turned) via `ControlNetwork.broadcastAround` and on `StartTracking`; client store `client/EtoClient.java` |
| Rendering | `client/render/EtoRenderer.java` + `EtoModel.java`: slim player model, legs drawn 13% wider and 6% deeper (hips), a bust wedge on the torso textured from the torso front, kakugan overlay (entityTranslucent at the eye's fade) + glow (`RenderType.eyes`), closed eyes on blinks and eyes-closed gestures, blush, the anger mark as a camera-facing quad over her head. Whole-body lean/hop/twirl/flip applied in the renderer about 0.7 blocks up. |
| Tests | `src/test/java/com/hex/kagune/control/EtoTests.java` |

Known gaps and risks in what is done (verify first, in this order):
1. Nothing has run in a real game. Most likely trouble spots: the bust wedge's orientation and UVs (`EtoRenderer.bust`), the sign of the
   kiss lean (legs should stay planted while only her top half tips forward; if her feet swing out, flip the sign of the legs' X in
   `EtoPose.kiss`), the anger mark's facing, the structure placing her (she must spawn once per Library on the ground floor).
2. No preview sheet exists for Eto's poses yet (PoseRender needs a 2x texel scale for her 128px skin).
3. Surgery for a human (unlock as ghoul) was decided without asking the owner. Ask.

## 6. What to build next (the owner's words, then the plan)

The owner wants:

> A complex romance system. Give her a variety of stuff to do in her library like: read books, sweep the floor with a broom, write in a
> little pocket book, etc. After the player dates her, she will unlock an option to follow me and come live with me (makes where she's
> standing her new home), greet the player when he gets home, protect him if he's being attacked (she has to see it happen).
> Kiss: she walks up, hands behind her back, leans her top half forward, eyes close "- -"; tap shift = hearts, hold shift = she blushes
> and stays; leave her hanging = she gets mad, anger mark over her head, ignores you for a while. She can ask for a kiss whenever she
> wants (random, with a hidden cooldown of about 20 real minutes) or you can ask from the menu. Make sure she blinks and the eyes are cute.
> A triangled chest area and better curves (legs a little larger than the torso).

The kiss, blinking, chest and curves are done (section 5). Still to build, in this order:

1. **Her activities in the Library** (when calm, not talking, not fighting): a `EtoActivity` enum and a scheduler in `EtoBrain`
   that walks her to a spot and plays a long looping pose: READ (sit or stand with an open book: a small book quad in her hands, page
   flips), SWEEP (a broom held in both hands, sweeping strokes, dust particles at her feet), WRITE (a little pocket book in one hand, pen
   in the other, small writing motion), plus SHELVE (reaching up to a bookshelf), STRETCH, NAP (sitting, eyes closed). Pick spots by
   scanning blocks near home: bookshelves / chiseled bookshelves for READ and SHELVE, open floor for SWEEP, a lectern or table for WRITE.
   Props (book, broom, pocket book) are drawn by the renderer as small textured quads attached to her arm part (the same way the bust is
   attached to the torso: `ModelPart.m_104299_` then emit quads); add their textures to `tools/eto_textures.py`. Sync the activity like the
   gestures (add fields to `S2CEto`, bump PROTOCOL).
2. **Romance depth**: affection that decays slowly if the player stays away; moods (happy, sulky, jealous after the anger mark); dates
   (a choice that makes her walk with the player for a few minutes and come back); small gifts she gives back (a written book, a flower);
   pet names in dialogue once dating; anniversary lines (count in-game days dating). Keep all the rules pure and tested in `EtoRules` /
   `EtoTalk`, like the rest.
3. **"Come live with me"** (dating only): a talk choice (new `NpcTalk.Choice` at the end) that makes her follow the player (navigation
   to the owner like a ghoul ally: see `GhoulNpcBrain` follow code), then a second choice "This is home" that calls `EtoNpc.settle()`
   at her current position (her home moves; `restrictTo` follows). Store the player she lives with in her persistent data (UUID).
   While following she must not be bound by the 16-block "lost" teleport (skip it while following).
4. **Greeting** the player she lives with when they come home: when that player enters her home radius + 8 after being away more than
   a few minutes, she walks up, WAVEs or HUGs, and says a line on the action bar.
5. **Protecting** the player she lives with: when that player is hurt by a living attacker and she has line of sight to the attacker
   (`m_142582_`) within ~16 blocks, she targets it (leash widened while protecting) and fights it with her kit, then returns home. Use
   `LivingHurtEvent` (see `GhoulNpcBrain.hurt` for the pattern) and keep it server-only.
6. Then the owner's earlier drop-2 combat list for her: flesh, "Watch This", Mutated Arm, her Owl kakuja (see `control/pale/**` and
   `server/PaleOwl*.java`: the player's Pale path already has these; reuse them for her).

Every step: pure rules + tests, compile, `python3 build.py` green, preview images sent, commit, push, and say plainly what was not tried
in game.

## 7. How to test in game (tell the owner this)

- `/locate structure hexkagune:library`, then `/tp` there; or `/summon hexkagune:eto` anywhere (her home becomes where she first stands).
- Right click her to talk. `/time set 18000` to see her kakugan open.
