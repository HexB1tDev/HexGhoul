# Next AI: after 4.1.9 (the inventory crash; every special place in any biome, one of each)

Read this first. The house rules are in `next-ai-4.1.2.md` and `next-ai-4.0.0.md` (they still hold): answer in English, quote the owner
exactly, never claim in-game testing, the full `python3 build.py` must end with EXIT 0 on the exact commit before it is pushed, never
alter the owner's files, one version at a time, no PR.

## Where things stand

| Version | What | State |
|---|---|---|
| 4.1.8 | Kick Master's twirling speed kick | pushed, EXIT 0 |
| 4.1.9 | The inventory crash; the hideout, the CCG building, Anteiku, the library and Ken's tower in any biome, one of each | pushed, EXIT 0 |

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
2. **Then the owner's ask from before the bugs, quoted exactly:** "after that please put all your focus into the combat rework, and kagune
   rework for the ukaku, this model is very expensive and I need you to focus on what's hard, study complex fighting moves, and combos and
   make it look smooth and beautiful." Not started in code. What this session found and planned:
   - **The flaw:** both the wings (`WingRig.phases`: the swing is a smoothstep ending just past the impact, then `FOLLOW_HOLD`) and the body
     (`UkakuBody.Keys.at`: smoothstep from wind-up to the impact key) slow to a stop as the blow lands and then hold still. A blow should
     land at its fastest and slow down after contact. And the wing is rigid: `slam()` only moves two root angles (sweep, roll), so the tip is
     locked to the root, a paddle, not a whip.
   - **The plan per slam (SLAP, SWING, RISE, SLAM, TWIN_SLAM, GUARD):** a chamber (drawn back past the wind-up, a short loaded hold), a
     drive that accelerates into the impact (Hermite, velocity carried through it), an overshoot past the impact pose that decelerates, a
     recoil (a small rebound off the hit, then a damped settle). The tip whips: a spring follower on the root angles (integrated from
     progress 0, pure, so the same frame comes out every time), bent onto the wing with the existing `UkakuModel.Bend` (as `aimFrame` does),
     the fan folded on the drive and flared at the impact. The body leads the wing (hips, then chest, then arm, then wing: `UkakuBody` keys
     a little earlier than the wing's), the other wing counter-pulls on the drive. GUARD's progress is the guard's amount
     (`RinkakuBlockClient.amount`), not time: shape its curve over that.
   - **Keep:** `UkakuTests.rig` (no wing below -0.15 at any progress: sample finer than its five points for the slams), the mirrored-pairs
     and twin-slam checks in `UkakuTests.wingSlams`, the server's impact ticks (`ComboStage.impactFraction`, visual only otherwise).
   - **Previews:** `tools/ArtPreview.java` has no sheet for the slams yet. Add one (`slams`: each motion key by key, the body through
     `posed(UkakuBody.pose(...))` and `wings(...)`, plus the wing tip's path over the whole move as dots, their spacing showing the speed)
     and a GIF of the 7-stage string (`PalePreview.writeGif` shows how).
