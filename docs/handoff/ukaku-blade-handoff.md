> **Standing rules (the owner, 2026-11-16):** (1) **Always work on the VERY LATEST code.** `main` is not it (it stopped at 1.3.0):
> before touching anything, `git fetch` and start from the remote branch with the newest commit (`git for-each-ref --sort=-committerdate
> refs/remotes/origin --format='%(committerdate:iso) %(refname:short) %(subject)' | head`), merged into your session branch.
> (2) **Every preview render uses the owner's own skin** as the reference body: `art/user_skin.png` (64x64, slim arms, eyes on face rows
> 5-6); `tools/PoseRender.java` puts it on every preview body without a skin of its own (`-Dhex.skin=...`, set by every `tools/*.sh`), and
> `ArtPreview.eyes()` / `userEyes()` fit masks and kakugan to its eyes. Never the old stand-in suit. Pass both rules on in every handoff.

> **The owner's rules for every session (2026-11-16, pass them on in every handoff):** no agents or subagents; one thing at a time and push
> every time something works (CI builds a jar per branch; they test on their phone through Amethyst, so it must work there); `python3 build.py`
> must pass (check its exit code) before every push; every preview render on their skin (`art/user_skin.png`), judged before it is sent, saying
> what differs from their reference; show them a model before building animations or combat on it; every Ukaku sound Minecraft's own (no sound
> pack, and none shared with the Rinkaku's or Bikaku's: `SfxTests`); never a model id in the repo; no PR and no release unless they ask (a
> `.github/release-notes-*.md` pushed IS a release: don't add one).

# Session 2 (branch `claude/eager-ritchie-xni1ms`, after `claude/great-galileo-cdhewb`): the mask's colour, the flash, the finisher

They watch the renders as they are made (the images you Read reach them): they answer mid-turn, often with a screenshot marked up. Act on it at
once. VERSION is still 4.2.8 and nothing is released; the work is labelled 4.2.9 in comments and tests. Not tested in game.

## Their words (verbatim, in order)

1. "the mask is meant to be the same color as the armor" — **done**, pushed (`cff3076a`).
2. (the full-form render, sent) then the flash: "Blade Arm Control + right click (does a high speed dash, only leaving behind after images)" —
   **done**, pushed (`21e143b3`).
3. The finisher, refined live: "A bit up, its too low"; "make sure the wound shows the buts, bone etc, for mobs just a red uneven surface like
   meat."; (a picture: a line across the chest) "Up more around here, the hands/arms get chopped off too and land on the floor (the entire thing
   is meant to be an adaptive ragdoll that adapts to any terrain, can roll, etc. The vanilla player death animation never plays in this (only
   for the death screen for the hit player), the body slowly vanishes after around 40 seconds."; "make sure also that during this death fase,
   the hit player/wild ghoul or whatever, is unable to use any move at all or use the kagune/quinque."; "bro the top half is clearly falling in
   the legs, it should fall forward a bit more"; "more forward, still too close"; "for the line, just make a really thin red (bright and glowing)
   line around the whole area of the cut."; "no stripes, an actual line, and even thinner"; "slowly appearing, then give me the gif"; "the line
   should honestly now start in the very center, and the line flows from both sides at once, left and right"; "no matter the mob, the height of
   the cut is always this of the player, you dont have to adapt it to literally every mob / the cut height is a fixed thing"; "why does the line
   stop on the overlayer?"; "when thats done push an update and show me the gif of a zombie dying".

## Done (where it is)

- **Mask colour:** `ukaku/KakujaMask.colours` (the plates' palette by facing and curvature), drawn on white through `KaguneTint` like the chest
  (`KakujaArmorLayer.mask`). Test `UkakuTests.maskColour`.
- **Flash:** `ukaku/SteelFlash` (pure: 2.6 blocks a tick for 4 ticks, the line it cuts, afterimages a body apart), `server/BladeFlash` (the
  push, the vanish's invisibility for its ticks, untouchable, one cut per body), `C2SUkakuAction.FLASH` 12, `S2CUkakuFx.FLASH` 50,
  `client/fx/BladeFlashClient` (ghosts each a quarter stride on in `BladeArmPose.run`), `KingAfterimages` carries the blade, the board row.
  Test `UkakuTests.flash`. Render `docs/handoff/ukaku-flash-4.2.9.png`.
- **Finisher:**
  - Server `server/BladeFinisher`: a Steel Blade cut or flash cut (`arm`/`disarm` round the hurt in `KaguneCombat` and `BladeFlash`) whose death
    would land: `LivingDeathEvent` cancelled, health 1 (as a totem), held 40 ticks (slowness, motion, a mob's path and target), nothing hurts it
    and it hurts nothing (`LivingAttackEvent`), no swing, item, block or use; `DragonLock.locked`/`held`, `PaleStun.stunned` (the NPC brains),
    `C2SArmAction` (quinques) and `C2SRegen` all refuse a dying body. Then `S2CUkakuFx.SPLIT` 52 (with what its kagune does) and it is killed,
    the killer's kill. Named NPCs (`UniqueBody`), the White Reaper and boss-sized bodies die as always. Protocol 131.
  - Pure: `ukaku/Finisher` (the stun, `round`, `line` for non-humanoids, `section`: what a cut shows, person trunk/limb or meat),
    `ukaku/Ragdoll` (rigid bodies of boxes; contacts swept from each piece's middle to its corners against `Terrain` (the game: `level.clip`
    COLLIDER), the hit face found on the sixteenth grid (`face`), contacts solved in passes (`solve`), piece on piece, sleep),
    `client/animation/Carcass` (the body cut in four at the player's chest, fixed: top, belly and legs, both forearms; vanilla's cube UVs
    cut with the faces; `wound`: the thin glowing line from the chest's middle both ways, past the second layer (flat 0.25 px, 3D Skin Layers
    1.15); `launch`: the top thrown forward clear of the legs).
  - Client `client/fx/FinisherClient`: the line on the live body (a player's pose from `KaguneBodyAnimation.body`), the body hidden once cut
    (`RenderLivingEvent.Pre`/`RenderPlayerEvent.Pre`: no death animation), the pieces simulated and drawn in the body's own texture with their
    cuts, blood (`UkakuBlood.spurt`, `RcFx`, `BloodStains.pool` where each piece comes to rest), 40 s then a 5 s fade; a Rinkaku's or Bikaku's
    kagune shatters (`KakujaFx.breakApart`). Non-humanoid mobs: the line round their box, blood, and the vanilla death.
  - A mob with a humanoid model (zombies, skeletons, the mod's ghouls, CCG, townsfolk): its pose is read off its model each frame of the stun
    (`FinisherClient.posed`: arms, legs, head held whole in a `RigPose`), drawn at its model's own size (1/0.9375 of a player's), its texture
    (`EntityRenderer.m_5478_`); vanilla's plain humanoid wears its right limbs' texture mirrored on its left (`Layout.HUMANOID`). The cut is one
    level plane at the player's chest ("the cut height is a fixed thing"): an arm crossing it is cut where it crosses (`Carcass.armCut`); an arm
    held up out of it (a zombie reaching) goes whole with the top, with no line round it. The mod's people are BODY inside, other mobs MEAT.
    Zombie GIF: `HEX_PREVIEW_OPTS=-Dhex.zombie=<zombie.png> tools/art-preview.sh <out> finisher-zombie`.
  - Tests `UkakuTests.finisher`; sabotage `docs/handoff/sabotage-4.2.9-finisher.json`. Previews `tools/art-preview.sh <out> finisher`,
    `finisher-gif`, `finisher-arm`, `finisher-zombie` (with `HEX_PREVIEW_OPTS=-Dhex.zombie=<zombie.png>`; the texture is Mojang's: fetch it into
    the scratchpad, never commit it). The finisher previews draw the body at the game's 0.9375 (`bodyScale`): unscaled, the drawn sleeve buried
    the line (their "why does the line stop on the overlayer?").

## Not done yet (next, in order)

1. An Ukaku victim's wings should go down with the top half (the owner: the kagune "cut in perfect half"); now they vanish with the dead body.
   `KakujaFx` keeps each Ukaku player's last drawn wing quads (`ukakuDrawn`): carry them on the top piece (`FinisherClient`, kind 2 of SPLIT).
2. The Ukaku mastered transformation as an evil laugh (their words below, item 5).
3. Ukaku wings: wider, more violent M1s and M2s (item 6).
4. Risks to watch on their phone: the ragdoll costs about 70 block clips a tick per corpse while it moves (it sleeps at rest); at most 12 corpses.

# Handoff: the Ukaku final kakuja's mask and Steel Blade (after 4.2.8, unreleased), for the next agent

Branch `claude/great-galileo-cdhewb` = **4.2.8** (`claude/youthful-gauss-0u9gis`, the newest branch when this was written) merged, plus
this session's work on top. `VERSION` is still 4.2.8; nothing released (release only when the owner asks). The deeper reference for the
whole mod is `docs/handoff/next-ai-4.1.9.md` (4.2.8's live handoff): read its rules (build, allowlist, protocol, sounds) before coding.

**Nothing below has been tried in game.** Compiled, tests pass (see "Build" at the end), previews rendered and sent.

## What the owner asked this session (verbatim, in order)

1. "I want the Ukaku full kakuja mask redo to be more like the one in the show. it's too flat, lacks detail." Then, after many rejected
   attempts of mine: "here's the mask model, adapt it." (their own model) **Done** (below).
2. "When thats done I want the blade arm reworked, to be smaller, more efficient and have custom combat animations, be more intimidating,
   learner and smoother, with an equally intimidating relaxed stance, custom run animation, etc." Then: "cant you just use the arm blade
   of the wild ghouls for a base? looks much better than that garbage" ("look at the CCG human-ghoul division"), "make it the armor's
   texture like the last blade, a bit slimmer too (the blade)", "now sharpen it, make the edge more visible, and make it thiner".
   **Done** (below).
3. "Blade Arm Control + right click (does a high speed dash, only leaving behind after images)". **Not started** (plan below).
4. "Has a finisher if the entity / player dies, they are stunned for 2 seconds and then their top half will fall face forward on the
   ground, a blood splashes everywhere when this happens, the enemy player's kagune is also cut in perfect half at the waist, if it's a
   bikaku or rinkaku, then it shatters. while the entity is stunned a perfect wound like (can be from the cut wounds we already have in
   the game) really shallow will slowly appear around the entity's waist, when it reaches it's start thats when the top half slides
   off." **Not started** (plan below).
5. "Ukaku Mastered animation should be a evil laugh sequence: Put hand on face (head tilted down), start laughing, hand back down a
   little on the side (right hand) the hand should be a little to the right off the body, crack finger with sound, ukaku emerges."
   **Not started.**
6. "Ukaku wings have more violent m1s (spead open more like wings) (same for M2, less compacted, more free as in wider)." **Not started.**
7. Last thing before the handoff: "show me the absolute full form, Kakugan eyes, full kakuja and all". **Sheet written, not rendered**:
   `tools/art-preview.sh <out> full-form` (`ArtPreview.fullForm`): render it, look at it, send it. Do this first.

How they work: blunt; they judge the visuals and will say so. **Judge every render yourself before sending it, honestly** (I sent several
bad mask renders without judging them and they rightly called it out). Say what differs from their reference.

## Done this session (where it is)

### Previews on the owner's skin
`art/user_skin.png`; `tools/PoseRender.java` (`USER_SKIN`, `USER_SLIM`, `model()` wears it when a body has no skin of its own);
every `tools/*.sh` passes `-Dhex.skin`; `tools/ArtPreview.java`: `eyes()`, `shell()`, `userEyes()`, `userKakugan()`, sheets `half-mask`,
`blade-moves`, `full-form`; `art-preview.sh` compiles `npc/BladeArm*.java` too.

### The final kakuja's mask: the owner's model
- Their files: `art/kakuja_mask/` (`black_half_plague_doctor.obj/.mtl/.png/.bbmodel`; OBJ: neck pivot at the origin, Y up, front -Z, blocks).
- `tools/kakuja_mask.py <obj> <png>` bakes it into `src/main/resources/assets/hexkagune/models/kakuja_mask.bin` (head pixels, welded,
  12,754 vertices, 21,384 triangles, 294 KB) and copies the texture to `textures/entity/kakuja_mask.png`. `build.py` `NEW_PREFIXES` lists the bin.
- `ukaku/KakujaMask.java` (pure): `mesh()` (read once from the jar), `read`, `place` (grows in from the face; pushed out over a skin's
  second layer by `shell - FACE`), `alpha`.
- `client/render/KakujaArmorLayer.java`: the final kakuja draws it textured (`MASK`, `RenderType.m_110473_` with its texture; each triangle
  a quad with its last corner twice), the old `KakujaModel.halfMask` only if the mesh could not be read.
- **Risk:** 21k triangles per masked player per frame. If the owner reports lag (they play on a phone through Amethyst), decimate the beak
  in `kakuja_mask.py` (it is 17,200 of the triangles) or cache it in a VBO.

### The Steel Blade: the ghouls' arm blade, in the old blade's steel
- `ukaku/SteelBlade.java` rewritten, same public API (`FORM_TICKS`, `LENGTH`, `REACH`, `extend`, `fold`, `build`, `extent`, `edgePoint`, ...):
  builds `npc/BladeArmModel` (the CCG ghoul division's arm blade, 4.1.47) in plain grey and retints every corner to the old Steel Blade's
  mottled steel (`retint`, `steel`; veins become inked grooves; blood after it cuts), `WIDTH` 0.66, `THICKNESS` 0.5, `GLEAM` 0.3 (the bright
  edge band), `POINT` 2.6 (a keener point). `glows()`: the edge band is drawn full bright (`SteelBladeLayer.sink`).
- `npc/BladeArmModel.java`: overloads with the blade's reach, width, thickness, gleam band and point, `edge()`, `root()`; the ghouls' own
  call is unchanged (defaults).
- **Moves** (`client/animation/KaguneBodyAnimation.java`, `carry` and `steelStrike`): the player's body with the blade out is the ghouls'
  own moveset, `BladeArmPose`: `wrap` while it forms, `stand` (the living stance), `run` while sprinting, and the four cuts (`swing(step)`),
  crossfading `BLADE_BLEND` (4 ticks) between them; `bladeFit` 1 (drawn in rather than into the ground). `ComboStyle.UKAKU_STEEL_BLADE` is
  four stages timed as the ghouls' cuts: Steel Chop (`STEEL_SWEEP`), Steel Backhand (`STEEL_RETURN`), Steel Sweep (`STEEL_SWEEP`), Steel
  Execution (`STEEL_EXECUTION`); `steelStep(stage)` is the stage's index (no new StrikePattern: its ordinals are compared with
  `compareTo`). `SteelSwing` is no longer used by the player (kept, still tested).
- Tests: `SteelBladeTests` rewritten for it, `UkakuTests` (four cuts, reach). The old hold-based carry checks were replaced by a hanging-arm check.

## Next, in order

1. **Render `full-form` and send it** (the owner asked). Judge it first.
2. **The flash dash (Ctrl + right click, blade out).** Plan:
   - Client (`client/UkakuInput.useInput`, the `dashes` branch): if `ControlKeys.unleashHeld()` (Ctrl, the owner's binding) send
     `C2SUkakuAction` **FLASH** (new id 12, appended), else the old `DASH`.
   - Server: a `BladeFlash` like `ReaperDashServer.push`: velocity along the look (about 3.2 blocks a tick for 3 ticks, vertical damped),
     so collisions hold; a cooldown (`Cooldowns.of`, creative free with `Cooldowns.off`); every living thing the swept box touches cut once
     (`KaguneBalance.swipeDamage`-based, the player's damage source `m_269291_().m_269075_(player)`), counted as a blade hit (for the finisher).
   - Clients: a new `S2CUkakuFx` kind **FLASH** (50, appended): the player not drawn for the burst (cancel `RenderPlayerEvent.Pre`, as
     `PlayerLayers`/`VanishClient` hide a vanished player) and `KingAfterimages.add` along the way every frame of it (pale tint), so only
     afterimages are left behind.
   - Sounds: **Minecraft's own only** for anything Ukaku (`ukaku/UkakuSounds`, the owner's 4.1.91 rule). Bump `PROTOCOL` in
     `network/ControlNetwork.java` (129 now).
3. **The finisher.** Plan:
   - Mark blade hits: in `server/KaguneCombat` where a Steel Blade cut lands (`SteelSwing.swings(stage.pattern())`, about line 544) and in
     the flash's cuts; a `LivingDamageEvent` handler: a marked hit that would kill → health left at 1, `BladeFinisher` begins.
   - 40 ticks (2 s): held where it stands (as `PaleStun` holds a body; mobs' AI off), spared every other hit; clients told (a new S2C):
     a shallow wound line grows round its waist, from one side all the way round (reuse the look of the cut wounds, `client/WoundClient`,
     `render/WoundLayer`, `wound/*`); when it meets its start: it dies (credited to the player).
   - The halves: `client/KingHalves.java` already draws a humanoid mob cut at the waist (legs where they stand, the rest sliding off along
     the cut). Extend it: players too (PlayerRenderer resets part visibility before `RenderLivingEvent.Pre`, so hide parts in Pre and draw
     the top half again in Post), the top half falls **face forward** onto the ground, the body kept a few seconds after death (keep the
     entity object and draw it after `RemoveEntities`), a blood burst everywhere (`RcFx` CRIMSON motes, `render/BloodStains`).
   - The victim's kagune: cut at the waist with the body (wings ride the top half); a **bikaku or rinkaku shatters** instead (find the
     existing shatter: the kakuja shatter, `KakujaBurst`, `UkakuWingRenderer`, the revive shatter in 1.2.8).
4. **The Ukaku mastered transformation as an evil laugh** (owner's words above). The pull-outs already taught by NPCs are the pattern:
   `client/KenPullOutClient` (a finger crack, `S2CUkakuFx.CRACK`), `NoroPullOutClient`, `AyatoPullOutClient`. Ask the owner which coming-out
   it replaces if it is unclear ("Ukaku Mastered": the mastered wings coming out). Laughing: Minecraft's own sounds only.
5. **Ukaku wings, M1 and M2 wider and more violent**: the wing strikes (`ukaku/UkakuBody`, `WingRig`, the 4.1.2+ wing slaps and slams)
   spread open like wings; the M2s less compacted.

## Build

`python3 build.py` (about 10 minutes on 4.2.8). See the commit that carries this note for whether it passed.
