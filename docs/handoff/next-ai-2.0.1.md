# HANDOFF: HexGhoul after 2.0.1 (2.0.2: more biomes for all four structures, a gentler village land check: see the top of
`companions-handoff.md`; the next is 2.0.3). Read this first, then `docs/handoff/next-ai-2.0.0.md`, then `docs/handoff/next-ai-1.3.60.md` (house
rules), then the top of `docs/handoff/companions-handoff.md`.

Branch **`claude/relaxed-carson-4y0frw`** (push only there; never main; no PR unless asked). Version **2.0.1** in `build.py` and
`patch/KagunePatch.java` (the next is **2.0.2**). Protocol **"72"** (unchanged in 2.0.1). Nothing is tested in game except what the owner
reports. CI (`.github/workflows/release.yml`) builds and publishes the jar when `.github/release-notes-<ver>.md` is pushed.

## The owner's report on 2.0.0 (their words) and what 2.0.1 did

> "the villages should be renamed just village, like Hex Ghouls Village. Next up is that the fact that the villages are spawning in big
> bodies of water when they should look for dry land to attempt to spawn ... CCG investigators, I only see two of them when there should
> be at least a little more. ... the game just crashed ... the speak to menu ... is currently showing like the inaccurate face of the
> person ... I'm going to send you the new schematic file for the new villages."

1. **Crash**: `Font.DisplayMode.NORMAL` (an enum constant keeps its name; 1.3.38 had `f_181350_`). `SpeechBubbles.draw/snore` now catch
   RuntimeException and LinkageError (`Safety.caught`): a bubble never crashes the game.
2. **Talk faces**: `S2CNpcTalk.skin` is a byte. Children sent -1 (Eto's face); villagers sent -100 - look (look >= 1000: cut to a byte,
   a companion's or a ghoul's face). Now `S2CNpcTalk.CHILD_FACE` (-50) and `HUMAN_FACE` (-51); `NpcTalkClient` draws
   `ChildRenderer.textureOf(id)` / `HumanRenderer.textureOf(entity)` (the level's entity by id): the skin each is drawn in.
3. **Rename**: the structure is `hexkagune:village` (structure, structure_set, biome tag `has_structure/village`, pools
   `village/<i>_<j>`, templates `structures/village/t_<i>_<j>.nbt`). The map class is `village/VillageMap` (generated).
4. **The new village** (`tools/village_structure.py`, reads the Sponge schematic): window x 10..265, z 8..263 of the 288 x 272 schematic
   (every house kept; vanilla's jigsaw reaches 128 from the middle, any turn); ground y 8 (the commonest top of ground), sunk by it
   (`start_height` -8, written by the tool); natural rock below ground - 3 left to the world unless it shows; air written 3 over the ground
   and inside buildings; rim water made bank; BYG/Pixelmon swapped (SWAPS), barriers/command blocks/fire/Pixelmon signs dropped; leaves
   persistent; signs' text kept only for the village's own (SIGNS_KEPT); containers empty. 5 x 5 tiles (SPLITS) joined by jigsaw blocks
   (a tree from the middle tile: its row, then each column; `size` 4), start anchor `hexkagune:village_middle`, terrain adaptation none,
   `max_distance_from_center` 128. 13 houses (of 23 found; farthest-point spread), 2 villagers each (26 = the 26 looks), 6 investigators
   on the patrol ring (18..36 out). Preview: `docs/handoff/village-2.0.1.png`. Template entities carry HexVillage `Map` 2.
5. **Dry land**: `ControlStructures` registers `hexkagune:dry_jigsaw` (DeferredRegister on `Registries.STRUCTURE_TYPE`): `DryJigsaw`
   wraps vanilla's `JigsawStructure` (its map codec xmapped: same JSON fields), its settings from Forge's
   `modifiableStructureInfo().getOriginalStructureInfo()`; `findGenerationPoint` samples 25 points (`VillageGround.SAMPLES`, +-112) with
   `ChunkGenerator.getBaseHeight` WORLD_SURFACE_WG vs OCEAN_FLOOR_WG: any water, or ground more than `FLAT` 8 apart: no village. Else
   vanilla's own findGenerationPoint, called through `ObfuscationReflectionHelper` (its access could not be checked here; by name it works
   either way). All new API is allowlisted with evidence (2.0.1 block at the end of `verify/api-allowlist.txt`).
6. **Old worlds**: Brick Villages already built keep `village/BrickVillage` (restored as legacy data): `VillagePlace.Place.brick` (no
   `Map` key) picks its streets/route/perimeter. The old structure id is gone: Minecraft logs unknown structure starts and goes on.
7. **More CCG**: 6 per village; wild pairs `AgentRules.SPAWN_CHANCE` 0.0084 (about a pair a day; was 0.0042).

## Untested in game (watch these first)

- World loading with the new structure type (a wrong name here would fail world creation: everything was checked against the mappings,
  but nothing ran). `/locate structure hexkagune:village`.
- How the village sits: sunk by 8, terrain adaptation none (edges meet the world as they are; the mountain cut on the east is a steep face).
  Trees of the world may grow in its open grass (features come after structures).
- Memory: about 914,000 template blocks (~45 MB once loaded).
- Villagers and investigators in the big village: walking the hills (street spots up to 6 over the ground), homes, the patrol.

## Ideas left from 2.0.0's list

See `next-ai-1.3.60.md` (group scenes, family meal, bedtime story, ...).
