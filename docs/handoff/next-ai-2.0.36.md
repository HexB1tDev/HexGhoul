# HANDOFF: HexGhoul after 2.0.36. Read this first, then `docs/handoff/next-ai-2.0.21.md` (the White Reaper as planned),
`docs/handoff/next-ai-2.0.9.md`, `docs/handoff/next-ai-2.0.5.md` (how the owner works), `docs/handoff/next-ai-1.3.60.md` (house rules),
then the top sections of `docs/handoff/companions-handoff.md` (one per version, newest first).

Branch **`claude/loving-albattani-xld4zv`** (it was fast-forwarded from `claude/relaxed-carson-4y0frw` at 2.0.26; push only to the branch
the session is given). Never push to `main`; no PR unless asked. Version **2.0.36** in `build.py` and `patch/KagunePatch.java`; the next
is **2.0.37**. Protocol **"77"** (`ControlNetwork.PROTOCOL`; 2.0.33 added the Reaper's GROUND swing and two `S2CUkakuFx` kinds). Nothing
is tested in game.

## The owner's requests this session (their words, trimmed)

> "give proper M1s to the new investigator, rank 10 ... give the humans that spawn in the village ... the females proper female bodies that
> we already got in the game, and ... romance with both males and female characters, different preferences on every male and female, more
> variety in the traits ... Make sure that the rank 10 investigator can be spawned via command for testing purposes, but overall he's just a
> rare guy that spawns once in a while."
>
> Known bug: "wild ghouls, I assume it's Rinkaku users, ... use the moves, but ... they're not manifesting the kagune ... It's invisible."
>
> Mid-session: "smooth out their [Dawn] model, because it's really squared ... proper M1 combat on that ... weapon ... Right click with it
> should fire a lightning ball, like a yellow lightning ball that explodes with VFX and all. This should use photon ... the tip of the weapon
> should open slightly to release the ball of energy ... another move only for the NPC ... he shoves the stick in the ground and then like a
> black tendril comes out of the ground where the user was ... still possible to dodge, but it's a pretty big tendril ... use this skin ...
> for the White Reaper specifically ... always."

## What shipped (each its own version; details at the top of `companions-handoff.md`)

- **2.0.27** wild ghouls' kagune: by reading and an out-of-game harness the server state, network, deploy timing and the tails' geometry
  were all correct (every tier, every lash, 600 frames, no NaN, full alpha). **No single cause was proven.** Hardened instead: a ghoul is
  noted at the start of its draw, the tails' and wings' passes also take every living ghoul within 48 blocks with its kagune out, and each
  ghoul and each Graft player is drawn on its own (a failure logged as "HexKagune's a wild ghoul's tails layer failed ..."). **Ask the owner
  whether the tails now show; if not, ask for that log line.**
- **2.0.28** the White Reaper wears the owner's skin (`art/white_reaper_skin.png`, unchanged; shipped byte for byte as
  `textures/entity/ccg/ccg_reaper.png`, `AgentRules.REAPER_LOOK`, slim arms). Ordinary agents roll only `REGULAR_LOOKS`.
- **2.0.29** `/kagune reaper` (operators): sends any loaded Reaper away, takes over the slot, spawns him five blocks ahead. Wild: each
  investigator appearance has `AgentRules.REAPER_CHANCE` 1% to be him alone (about once in 100 in-game days; was about once in 660).
- **2.0.30** Dawn smoothed (`tools/quinque_models.py`: 16-sided guard rings in fine steps, 8-sided spike/grip/butt on smooth profiles,
  turning ridges; 487 boxes) and its `dawn_fire` frame (the point parted 22.5 degrees each way about a yellow light: the smallest turn a
  model allows). New `tools/model_preview.py` draws any model JSON off-game.
- **2.0.31** Dawn's own M1s (`QuinquePose` DAWN keys: level sweep, rising backhand, lunging thrust; the sweep's arc 3 blocks at 60%, the
  thrust's line 4.5 at 80% + knockback), for the Reaper and players; dark ribbons along each swing (`quinque/DawnArc`, `client/fx/DawnTrails`).
- **2.0.32** players fire Dawn's lightning on right click (5-tick gather with the aim pose, 16 damage, 2 s cooldown kept on the server, no
  new API); Shift + right click folds it; the open point shows (stack tags `DawnAt`/`DawnFires`, `QuinqueClient.dawnFrame`, override 0.99
  on the weapon item only). The Reaper leaves his case to a player who kills him; Dawn is in the creative Combat tab.
- **2.0.33** the Reaper's ground tendril (`npc/ReaperTendril`, `AgentBrain.ground/plant/rooted/erupt`, `AgentPose.ground`,
  `client/render/ReaperTendrils` drawing the original's Rinkaku tube black, `UkakuFx` REAPER_MARK/REAPER_TENDRIL). Protocol 77.
- **2.0.34** village women's bodies (`HumanRules.female/WOMAN_*` = the CCG women's figure; `HumanRenderer` resets the shared model per
  villager). Preview: `tools/VillageWomenPreview.java`.
- **2.0.35** who villagers fall for (`HumanPersona.Attraction`, `Taste`, `type`, `notMyType`, bond flag `NOT_TYPE` 64); "What's your
  type?" (`NpcTalk.Choice.WHO` on the villager page). A player who never chose a gender may date anyone; couples already together stay.
- **2.0.36** four new tempers appended (PLAYFUL, STOIC, SARCASTIC, ROMANTIC) with every line set and gestures. Old villagers keep theirs
  (`HumanPersona.temper(seed)` over the first `LEGACY` 8); villagers placed since roll any of the 12 (`rolled`), saved as `HexHumanTemper`.

## How it was checked

One full `python3 build.py` of 2.0.26 at the start (EXIT 0), then `tools/quick_check.py` (compile + API verify + the touched suites) for
every version; CI built and released each. Previews (all off-game, in `docs/handoff/`): `dawn-smooth-2.0.30.png`, `dawn-m1s-2.0.31.png`,
`dawn-aim-2.0.32.png`, `reaper-ground-2.0.33.png`, `reaper-tendril-2.0.33.png` (the tendril's line only: the tube itself is the original's
renderer and can only be seen in game), `village-women-2.0.34.png`.

## Open, to ask the owner

- 2.0.27: do wild Rinkaku ghouls' tails show now? If not, the log line.
- Dawn's numbers are first guesses: players' ball 16 damage / 2 s; the Reaper's tendril 24 raw, every ~8.5 s at most; the Reaper's case
  always dropping to a player's kill (2.0.32) was a judgment call, as was the 1% Reaper chance (2.0.29).
- The open point can only part 22.5 degrees (item model limit): if "slightly" means less, a thin glow at the tip instead is the option.
- Village women's height (0.92 of a man's, the CCG women's) and the attraction shares (62/22/16) are judgment calls.

## House rules (unchanged; full text in `next-ai-1.3.60.md`)

Nothing is tested in game, never claim it is. Look at your own previews honestly, zoom in, match the owner's pictures. Banned names in the
jar: Kaneki, Ayato, Furuta. Banned in visible text: Takatsuki, Aogiri, Yoshimura. Sounds only from the hexkagune: pack. Never alter the
owner's skins or files (only generated copies). New Minecraft API: an allowlist entry with [srg] or [order] evidence plus a stub; stale
entries fail the build; never call a vanilla-private member directly (`ObfuscationReflectionHelper`). Append only to ordinal-sent enums.
Each fix/feature is its own version (both version strings, release notes, README entry, a new top section in `companions-handoff.md`,
commit `2.0.x: ...` with `Co-Authored-By: Claude <noreply@anthropic.com>` and the session line, no model names), then push.
