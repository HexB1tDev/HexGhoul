# Next AI: after 4.1.10 (4.1.9: the inventory crash, every special place in any biome, one of each; 4.1.10: the Ukaku's wing slams)

Read this first. The house rules are in `next-ai-4.1.2.md` and `next-ai-4.0.0.md` (they still hold): answer in English, quote the owner
exactly, never claim in-game testing, the full `python3 build.py` must end with EXIT 0 on the exact commit before it is pushed, never
alter the owner's files, one version at a time, no PR.

## Where things stand

| Version | What | State |
|---|---|---|
| 4.1.8 | Kick Master's twirling speed kick | pushed, EXIT 0 |
| 4.1.9 | The inventory crash; the hideout, the CCG building, Anteiku, the library and Ken's tower in any biome, one of each | pushed, EXIT 0 |
| 4.1.10 | The Ukaku's wing slams: chamber, drive, overshoot, recoil, the whip; the guard's flare and settle | pushed, EXIT 0 |

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

## Next after that

- The owner judges the look. Likely asks: more or less whip (`WHIP_PERIOD`, `WHIP_DAMPING`, `WHIP_MOST`), a longer or shorter drive per slam
  (`Blow.drive`), bigger chambers. Then the combos themselves (the rest of their ask: "study complex fighting moves, and combos"): the string
  is 7 fixed stages (`ComboStyle.UKAKU_WINGS`); ideas to pitch first, not build unasked: a held M1 finisher, a launcher into an air slam,
  the chain flowing (the next stage's chamber taken from the last one's recoil instead of from rest).
