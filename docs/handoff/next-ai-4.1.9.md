# Next AI: after 4.1.14 (4.1.14: places built at any good spot, first one kept; coats on the legs; no Ukaku M1 spam; 4.1.9: the inventory crash, the special places; 4.1.10-4.1.13: the wing slams; 4.1.12: places never nowhere, the coat; 4.1.13: slams as arms and hands clear of his body, the guard, /kagune places checks each place)

Read this first. The house rules are in `next-ai-4.1.2.md` and `next-ai-4.0.0.md` (they still hold): answer in English, quote the owner
exactly, never claim in-game testing, the full `python3 build.py` must end with EXIT 0 on the exact commit before it is pushed, never
alter the owner's files, one version at a time, no PR.

## Where things stand

| Version | What | State |
|---|---|---|
| 4.1.8 | Kick Master's twirling speed kick | pushed, EXIT 0 |
| 4.1.9 | The inventory crash; the hideout, the CCG building, Anteiku, the library and Ken's tower in any biome, one of each | pushed, EXIT 0 |
| 4.1.10 | The Ukaku's wing slams: chamber, drive, overshoot, recoil, the whip; the guard's flare and settle | pushed, EXIT 0 |
| 4.1.11 | The slamming wing stretches out as far as the slam hits, a lash of fire; the whip keeps pace with each blow | pushed, EXIT 0 |
| 4.1.12 | Places never nowhere, `/kagune places`; slams without particles and 1.75x as long; each coat half on its own leg | pushed, EXIT 0 |
| 4.1.13 | Every slam an arm (behind a new shoulder) and a hand, aligned on its target and clear of his posed body; the guard clamped round him clear of his raised arms; `/kagune places` makes each place's chunk and says built or stopped; coat previews | pushed, EXIT 0 |
| 4.1.14 | One-of-a-kind places at any dry spot their set offers, the first the world builds kept with the world (`PlaceClaims`); coat tails drawn on the legs; each Ukaku wing slam plays out in full (1.3x damage) | pushed, EXIT 0 |

Branch: `claude/hopeful-euler-dnx1ub` (fast-forwarded from `claude/next-ai-4-1-6-handoff-sqv9e4`, which stops at 4.1.8). A new session's own
branch usually starts at `main` (still 1.3.0): fast-forward it first (`git merge --ff-only origin/claude/hopeful-euler-dnx1ub`).

## 4.1.9, what and where

The owner (quoted exactly): "Important top priority bugs: Cant find CCG building, Aogiri Hideout or Kaneki's building AT ALL not even with
locate. Opening the inventory crashes the game instantly." Then: "Make sure to include a bunch of, like, I don't know which biomes you're
setting, but it should be able to spawn literally in any biome as long as there's only one of each structure aside from the villages, of
course. It should also be fully compatible with Biomes of Plenty, so that should be something already in place."

- **The crash** (their report: `AbstractMethodError: ControlItems$$Lambda does not define ... m_257865_ ... CreativeModeTab$DisplayItemsGenerator`):
  the stub named `DisplayItemsGenerator.accept` by its Mojang name and the allowlist called it "unobfuscated"; it is `m_257865_`
  (joined.tsrg `cef$b accept (Lcef$d;Lcef$e;)V m_257865_`). Every lambda of the four creative tabs (4.0.2) implemented a method the game
  never calls. Stub and allowlist fixed; `OutfitTests` reads the compiled `ControlItems.class` for `m_257865_`. Lesson: a lambda or a class
  that implements a Minecraft interface takes its method name from the stub, and nothing checks that at run time but the game.
- **Why no place was found:** every structure's `biomes` was its own short list (`has_structure/*`, `prefers/*`), and `ControlStructures.biome`
  tested that list at every offer. The owner's pack runs Regions Unexplored (TerraBlender): its biomes were on no list, so near the world's
  middle no offer passed, `DryLand.pick` gave up and the log said "all sea". Now:
  - every structure's `biomes` is `#hexkagune:overworld` (= `#minecraft:is_overworld`): it only keeps a set to the overworld (a set runs only in
    a dimension whose biome source makes one of its biomes: `ChunkGeneratorStructureState.hasBiomesForStructureSet`);
  - every place type overrides `Structure.findValidGenerationPoint` (`m_262864_`, public, not final in 1.20.1; world generation and
    `StructureCheck.canCreateStructure`, /locate's, both call it) to leave the game's biome-list test at the start out;
  - `ControlStructures.biome` is now "any biome but a wet one" (`hexkagune:wet`, which also takes Forge's and the common swamp/water tags,
    each optional). The dry-land checks (`DryLand.dry`) are unchanged.
- **One of each:** Anteiku, the library and Ken's tower were rings of three (`concentric_rings`, count 3). They are random spreads now
  (40/20, their old salts; `DryLand.PLACES`, `placeSalt`), each the world's one (`DryPlace.chosen`): the first offer, nearest the middle,
  where `DryLand.firstPlaced` finds it stands once laid out exactly as the world lays it (`DryPlace.at`: the game's own 9-argument
  `GenerationContext`, its random seeded from the seed and the chunk, then `here`, the 4.1.1 dry-land shift loop), at least
  `PLACE_APART` (12) chunks from every village offer and from the hideout, the CCG building and the places chosen before it. At most
  `PLACE_TRIES` (24) offers that look like land are laid out in full. A place is found by its key in the structure registry
  (`DryPlace.id`, `byId`); a name that can't be read builds nothing that time (never many).
- **The spreads are the world's own:** `ControlStructures.spread` reads each set's placement from the structure set registry and asks it
  (`RandomSpreadStructurePlacement.getPotentialStructureChunk`, `spacing`), so a mod that changes every set's spacing (Structure Essentials,
  in the owner's pack, has a global multiplier) moves the offers with it. With the files' numbers the offers are exactly 4.1.8's
  (`NpcTests.oldOffers` holds the old algorithm to prove it).
- **Tests:** `NpcTests.onePerWorld` (new), `NpcTests.dryLand`/`ccgBuilding`/`mansion` and `KitchenTests` updated to the new rule.
- **Not tested in game.** In an old world a place whose chosen spot was generated before 4.1.9 can't appear there; `/place structure
  hexkagune:<id>` builds one. The log names each place's spot the first time a world works it out ("HexKagune: this world's ...").

## Next, in this order

1. **Ask the owner how 4.1.9 played** (the inventory, `/locate` of all five in a new world) and fix what they report.
2. **4.1.10 did the first part of the owner's ask from before the bugs**, quoted exactly: "after that please put all your focus into the combat
   rework, and kagune rework for the ukaku, this model is very expensive and I need you to focus on what's hard, study complex fighting moves,
   and combos and make it look smooth and beautiful." Ask how the slams play (`docs/handoff/ukaku-slams-4.1.10.gif`, `ukaku-slams-4.1.10.png`
   against `ukaku-slams-before-4.1.10.png`: the same camera on 4.1.9's code; `ukaku-slams-zoom-4.1.10.png`: the overhead and twin slams,
   before over after).

## 4.1.10, what and where

- **The flaw it fixed:** both the wings (`WingRig.phases` then) and the body (`UkakuBody.Keys`) slowed to a stop as the blow landed and held.
- **`WingRig`:** each slam is a `Blow` (keys CHAMBER, IMPACT, OVERSHOOT, RECOIL as {sweep, roll}; the drive's, overshoot's and recoil's
  shares; the move's ticks), timed by `beats` (chamber, load end, impact = the server's, overshoot end, recoil end). `slam`: rest to chamber
  (smooth), a `LOAD` further back, the drive on an ease-in (`DRIVE_SNAP` 2.4: fastest landing), the overshoot on an ease-out, the recoil,
  back to rest. `tip`: a damped spring (`WHIP_PERIOD` 3.2 ticks, `WHIP_DAMPING` 0.42) on the root angles, integrated from progress 0 every
  frame (pure); `slamFrame` bends the wing toward it with `UkakuModel.Bend` (at most `WHIP_MOST`), its fan folded by `slamFold` (tucked
  `SLAM_TUCK` on the drive, flared `SLAM_FLARE` on the hit). `slamThrust` (the fire streaming back), `slamHard` (the RC hardened),
  `counterPull` (the other wing). `guard(amount)`: flare (`GUARD_OPEN`), shut past (`GUARD_PAST`, `GUARD_OVER`), settle.
- **`UkakuBody`:** the slam patterns (`slams`) use `Keys.blow`: their impact `BODY_LEAD` before the wing's, a drive of at most `BODY_DRIVE`
  (0.55 of the time to it) on an ease-in (`BODY_SNAP`), a `BODY_LOAD` past the wind-up, carried `BODY_OVER` past the follow-through key.
- **Tests:** `UkakuTests.slamBeats` (keys on their beats, fastest at the impact, still speeding up into it, carried past it, overshoot and
  rebound, the tip trailing then passing, the feathers, the counter-pull, the guard, never below -0.15 at any 1% of any slam in TWIN or
  RABBIT at three masteries, both sides) and `slamBody` (rest, no snap at 1/400, limits, mirrored, speeding up into the hit and on past it).
  `wingSlams` measures each wing alone now (a slap follows through across the middle).
- **Previews:** `tools/art-preview.sh <out> slams slams-gif` (`ArtPreview.slams`: each slam key by key and its tip's path from above, dots
  every 1/60, blue to white to the impact, yellow to red after: their spacing is the speed; `slamsGif`: the whole string at 30 fps).

## 4.1.11, what and where

The owner (quoted exactly): "I thought it was clear, but I guess not. You can make the wing stretch further forward, because it currently just
feels like a little chicken wings, bro. Whenever they stretch to slap something, they can stretch to be larger in size and length as well. You
don't have to be limited by such small range. I mean, it is what it is. You do you."

- **The mismatch:** the slams hit `UkakuRules.slamReach` (the original's `KaguneProfile.attackRange`, 7.5 + 0.0075 a mastery point, at most 15,
  × `SLAM_REACH` × the stage's `rangeMul`: about 8.3 blocks at 300, 12.75 at 1000) with a wing 1.6 to 4.3 blocks long.
- **`WingRig`:** `lash` (0..1: out through the drive, held through the overshoot, back in by `LASH_BACK` past the recoil); `stretchMost`
  (the wing's scale at the impact: the reach over `TIP_SHARE` (1.4, measured: the lashed wing's furthest fire) its length, at most
  `STRETCH_MOST` 3.2); `slamFrame` now turns the wing `LASH_TURN` of the way at the root toward the lash and curls the rest (at most
  `LASH_MOST`), the lash level (`lashElevation`: 5°, the rising wing 22°) along where the blow sweeps (the root's sweep, `LASH_TRAIL` of
  the whip's trail), its feathers folded `LASH_FOLD` but for the hit's flare. Uniform scale on purpose (UkakuFlame's normals come from the
  frame's axes; the owner asked for "larger in size and length").
- **The whip fixed:** its spring's period is `WHIP_PER_DRIVE` (1.4) of each blow's drive in ticks (4.1.10's fixed 3.2 ticks left a slap's tip a
  quarter turn behind at its impact).
- **Tests:** `UkakuTests.slamBeats` (the tip reaches 0.8 to 1 of the reach (+1.5) at 300, 500 and 1000; its own size at rest; `TIP_SHARE`
  measured), the rig's bound is the reach for slams, `rig` and the fine ground check unchanged otherwise.
- **First person** draws the own wings at 0.45 alpha: a lash crosses the view. Ask the owner if it bothers them; if so, lower `alpha` for a
  lashed wing in `UkakuClient.rigInput` (first person only).
- Previews: `docs/handoff/ukaku-slams-4.1.11.png` (from in front: a lash at the camera foreshortens; the top-view tip paths show it), `.gif`
  (from his side). `tools/art-preview.sh <out> slams slams-gif`.

## 4.1.12, what and where (the owner was furious; read their words)

The owner (quoted exactly): "Listen, asshole, I'm seriously running out of patience. You took 40 minutes to fucking fix an issue that I couldn't
fucking locate a structure. And I still can't." (their screenshot: "Could not find a structure of type "hexkagune:aogiri_hideout" nearby", the same
for ccg_building); "The hits are way too fast, and I don't understand why the fuck did you add default Minecraft particles in the area that it
swipes from? ... these fuck ass red Minecraft dust particles in crit particles that you've added. No one ever asked for that"; "insanely fast wing
slap animation that you can barely fucking see"; of the Crimson Outfit in a jump and a kick: "I truly don't know how in this world you expected the
clothes to open like that and look good"; "How do you look at the block animation, for example, and you call that good?"

- **This machine cannot run Minecraft** (Mojang's, Forge's and Modrinth's hosts are blocked): nothing here was seen in game. The structures still
  failed for the owner after 4.1.9 and the cause is NOT known. 4.1.12 makes every one-of-a-kind place always have a spot (`Site.FORCED`: the
  nearest offer when none is dry; `DryPlace` lays it with `DryJigsaw.run`) and adds `/kagune places` (`ControlStructures.report`, filled by
  `Site.told` when a chooser runs). **First thing next: ask the owner for `/kagune places` after a `/locate`, in a new world, and the jar version
  from the mods list.** "not worked out yet" after a /locate means the game never asked our structure (the set is filtered out in that world or
  another mod intercepts: suspects in their pack: Structure Essentials, Regions Unexplored/TerraBlender, Noisium, Canary).
- **Slams:** `UkakuCombat.impact` spawns nothing for the five slam patterns (4.1.2 drew a red dust arc and crits). `ComboStyle.UKAKU_WINGS` and
  `UKAKU_LEFT_WING` stages ~1.75x as long (16, 20, 20, 22/23, 28 ticks), impacts 0.5 to 0.6; `WingRig.Blow` drive 0.28 of the move,
  `DRIVE_SNAP` 1.6, the whip fades out after the recoil; `UkakuBody.BODY_DRIVE` 0.28. `UkakuTests.slamBeats` reads each slam's timing from
  `ComboStyle` and wants a swing of at least 3.5 ticks; no particles (source check). Previews `ukaku-slams-4.1.12.*`; `ArtPreview.SLAMS` reads
  `ComboStyle` now. Damage per hit unchanged (so less per second): offered to the owner.
- **The coat:** `OutfitLayer.drapes`: each half of the tails from its own leg (`CoatDrape.over(own front, own back, {thigh, thigh}, {knee, knee})`,
  `HUNG` per half). 4.0.3 hung both fronts from the leg furthest forward (never parted): any kick or tuck threw the skirt out flat. `OutfitTests`
  to the new rule. Not previewed (no off-game renderer for the outfit layer).
- **The block (the wing guard):** NOT changed. It closes in `RinkakuBlock.RAISE_TICKS` (3 ticks) with Rinkaku's tail-block body pose
  (`RinkakuBlockPose`, in `KaguneBodyAnimation`): ask the owner what they want before building (a slower close with its own body pose was
  proposed to them).

## 4.1.13, what and where

The owner (quoted exactly): "You see the shapes you're making? That was the right idea that I wanted for the M1 attacks in the first place,
that they actually bend like that, like hands. That's what I fucking wanted. That's it. While being perfectly aligned as well, and it
doesn't like clip to the body." Then: "Continue and do not stop at any point till all tasks are deemed perfect by you".

- **The wing is an arm and a hand** (`WingRig.slamFrame`, `UkakuModel.Bend`'s new shoulder fields `base`, `shoulderStart`, `shoulderEnd`):
  - **Shoulder:** the first `SHOULDER_START` blocks run along `base` (out of his back at `shoulderSweep`: drawn back by
    `SHOULDER_DRAW` of the arm's swing past rest, to `SHOULDER_SWEEP` 40 at most; never higher than `SHOULDER_ROLL` 0). The wing then
    turns onto the arm's axis on an arc by `SHOULDER_END`.
  - **Arm and hand:** past the turn it is the old frame, moved. The hand curls past `handStraight` (overhead `SLAM_FOREARM` 0.5, twin
    `TWIN_FOREARM` 0.4 of the wing; at least past the shoulder) toward `cupToward`:
    - slap and swing: level across;
    - overhead: world `{-side*SLAM_IN, -1, SLAM_ON}`;
    - twin: `{-side*TWIN_IN, -1, TWIN_ON}`;
    - rise: up.
  - **Timing:** the arm leads by `ARM_LEAD` of the drive; the hand lands on the server's tick.
  - **No pop:** at rest the shoulder has no turn, so nothing jumps as a blow starts.
  - The guard (`guardFrame`) has the same shoulder; its wrap is never tighter than `GUARD_RADIUS` (small wings hooked into the head).
- **The keys (`WingRig.blow`) were found by search, not by hand**, against his body as the game draws it:
  - The body is `UkakuBody` mapped as `KaguneBodyAnimation.addUkaku` does. The wings are carried by `BodyFrame.carry(pose, "torso")` as
    `UkakuWingRenderer` does, and sunk-in points are counted with `BodyFrame.Solid.depth`.
  - The idle wing rolls out (`COUNTER_ROLL` -12) and `UkakuBody`'s twin wind-up keeps the head on the target (`headX` 0.15). Both were head
    clips.
  - Targets are the hand's centre line (`WingRig.slamTip`), carried:
    - overhead hand at about (0.6, 0.8, 3.3) at mastery 300;
    - twin hands at about (±0.4, 0.7, 3.2);
    - rise at about (0.8 to its side, 3.2, 2.0).
  - The probes and the optimizer are scratch code, not in the repo. Re-derive them from the tests: `UkakuTests.sunkIn` / `posed` / `carried`.
- **Tests** (`UkakuTests`):
  - `slamsClearOfHim`: every slam of the string, Rabbit Guise's left string and the guard (with the arms raised as `guarding` raises them),
    fire and crystal, mastery 50/150/300/1000, every 0.02. At most `GRAZE` (30) more sunk points than at rest; today's worst is about 26.
  - `slamsAligned`: each hand lands where it aims.
  - `slamBeats`: rewritten for arm and hand (the hand through its keys on its beats, the arm a beat ahead, the hand fastest as it lands and
    never braking, the hand trailing the arm through the drive, growth up to `GROW_MOST` / `GROW_MOST_DOWN`).
- **The preview now carries the wings with the torso** (`ArtPreview.render`: `BodyFrame.carry`), as the game does. Before 4.1.13 every
  leaning pose's preview put the wings in the wrong place.
  - Sheets: `impacts` (each slam at its hit, front/side/above), `guard`, `coats` (both coats in six leg poses through `CoatDrape` as
    `OutfitLayer` hangs them), `slams-gif`.
  - Run: `bash tools/art-preview.sh <out> impacts guard coats` after a `quick_check` build.
- **`/kagune places`** (`ControlStructures.report(LevelReader)`, `built`, `STARTS`):
  - From a player, it makes each one-of-a-kind place's start chunk in full and looks for its start: "built there", or "NOT built there"
    (something stopped it).
  - Why: each such place may stand in one chunk a world only, so a veto there (Structure Essentials' "minimum distance to other
    structures", in the owner's pack) leaves the world without it and `/locate` finds nothing.
  - New API (allowlisted with joined.tsrg evidence): `LevelReader.m_9598_`, `m_6325_`, `ChunkAccess.m_213652_`, `StructureStart.m_73603_`.
- Previews in the repo: `docs/handoff/ukaku-impacts-4.1.13.png`, `ukaku-guard-4.1.13.png`, `coat-crimson-4.1.13.png`, `coat-formal-4.1.13.png`.

## 4.1.14, what and where

The owner (quoted exactly): "Things to fix and improve: Prevent m1 spam with ukaku. Change the armors so they dont open like skirts, simply
attach to the leg properly. Fix structures still not spawning at all or able to be located (still not spawning)"

- **The places** (`ControlStructures`: `claimed`, `making`, `offered`, `dryGround`; `PlaceClaims`; `village/TakenPlaces`):
  - **Before:** the hideout, the CCG building, Anteiku, the library and Ken's tower were each allowed in ONE start chunk per world, worked
    out in advance (4.1.1 to 4.1.13). Anything that stopped them there left the world without them, and `/locate` found nothing.
  - **Now:** any spot their set's live spread offers can hold one, if it stands there:
    - hideout and CCG: `dryGround` over the footprint plus `CCG_ROUND` (dry, steepness at most `STEEPEST`, biome);
    - the jigsaw places: `here`;
    - all clear of the other sets' spots (`DryLand.near`).
  - **The claim:** the first spot the world MAKES takes the place (`making`: the context's heights are a `ChunkAccess`; `/locate`'s
    structure check only asks). `PlaceClaims.claim` takes it under one lock and saves it on the overworld (`hexkagune_places`, loaded on
    `LevelEvent.Load`, or from `ServerLifecycleHooks.getCurrentServer()` if ground is made first). With nowhere to keep a claim it refuses,
    never risking a second one. Every other spot is refused from then on.
  - **Releasing:** a taken spot whose chunk loads in full without the start is given up (`ChunkEvent.Load`), and the next one made takes it.
  - `/kagune places` lists the taken spots, looks in each, and gives up any that was stopped.
  - The 4.1.6 to 4.1.13 world-wide choice (`hideout()`, `ccg()`, `chosen()`, FORCED) is gone; `DryLand.pick`, `choose` and `offers` remain
    (tested, unused by the game).
  - **Risk:** if some mod made `making` always false, every spot would be built (none would ever be taken). If the owner reports several of
    one place, look there first.
- **Coats** (`OutfitLayer.draw`):
  - The tails (`RIGHT_LEG_COAT`, `LEFT_LEG_COAT`) are emitted on the leg part with its bend, exactly as the trousers.
  - `CoatDrape` and the per-wearer drape are deleted.
  - `ArtPreview`'s `coats` sheet draws them the same way.
- **No M1 spam** (`ComboStyle.chainOpen`, used by `KaguneCombat.tryBegin`):
  - An Ukaku wing slam's next stage opens at `max(duration, chainOpenOffset)`, never mid-move. Before, it opened about 60% in, right after
    the hit, and snapped the wing.
  - Wing slam `damageMul` is 1.3x 4.1.13's, so `ControlTests`' pace window (0.7 to 1.5 of the original swipe) holds.
  - `UkakuTests` checks every stage at every duration.

## Still open

- **The places:** 4.1.14 builds them at any good spot and keeps the first. If the owner still finds none in a NEW world after a `/locate`,
  ask for the log lines `HexKagune: <id> is built at ...` and `... was stopped at chunk ...`, and for what `/kagune places` says.
- **The look:** the owner judges the new slams in game. Likely asks:
  - how far the hand curls (`blow` curl keys);
  - the shoulder's size (`SHOULDER_*`);
  - the drive's speed (`Blow.drive`, `DRIVE_SNAP`).

  Any change must pass `slamsClearOfHim` and `slamsAligned`; re-tune against them, not by eye alone.

## Next after that

- Then the combos themselves (the rest of their ask: "study complex fighting moves, and combos"): the string
  is 7 fixed stages (`ComboStyle.UKAKU_WINGS`); ideas to pitch first, not build unasked: a held M1 finisher, a launcher into an air slam,
  the chain flowing (the next stage's chamber taken from the last one's recoil instead of from rest).
