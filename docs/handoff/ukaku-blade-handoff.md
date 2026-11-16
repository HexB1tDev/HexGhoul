> **Standing rules (the owner, 2026-11-16):** (1) **Always work on the VERY LATEST code.** `main` is not it (it stopped at 1.3.0):
> before touching anything, `git fetch` and start from the remote branch with the newest commit (`git for-each-ref --sort=-committerdate
> refs/remotes/origin --format='%(committerdate:iso) %(refname:short) %(subject)' | head`), merged into your session branch.
> (2) **Every preview render uses the owner's own skin** as the reference body: `art/user_skin.png` (64x64, slim arms, eyes on face rows
> 5-6); `tools/PoseRender.java` puts it on every preview body without a skin of its own (`-Dhex.skin=...`, set by every `tools/*.sh`), and
> `ArtPreview.eyes()` / `userEyes()` fit masks and kakugan to its eyes. Never the old stand-in suit. Pass both rules on in every handoff.

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
