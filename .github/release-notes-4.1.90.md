## 4.1.90 (HexGhoul): the structures at home in TerraBlender's biomes (Regions Unexplored, Oh The Biomes We've Gone)

HexKagune 4.1.90 for Forge 1.20.1. Not tested in game. Protocol unchanged (127).

You said (quoted exactly): "I need you to adapt the mod so that the structures also spawn in the mod called Terra Blender. So research
should add a bunch of flat biomes from that mod, and make sure that the structures are correctly placed in the ground whenever they spawn."

**What I found:** TerraBlender adds no biomes itself; it is the library that Regions Unexplored, Biomes O' Plenty and Oh The Biomes We've
Gone add theirs through. Our one-of-a-kind places (Anteiku, the library, the hideouts, the CCG building, the circus, the Reaper's mansion)
already stand in any biome that isn't wet or steep, so they do spawn in TerraBlender's biomes. What didn't know them:

- **The hotel** stands only on open plains (your rule: "Only spawns on plains or biomes o plenty equivallent biomes, we cant have that shit
  spawning on mountains"), and that list was vanilla's and Biomes O' Plenty's alone. It now also takes the open fields of Regions
  Unexplored (barley fields, clover plains, flower fields, grassland, poppy fields, prairie, pumpkin fields, steppe, shrubland) and of Oh
  The Biomes We've Gone (allium shrubland, amaranth grassland, prairie, rose fields, pumpkin valley). Still never a meadow, a highland, a
  hill or a slope.
- **Villages:** a hotel only ever stands beside a village, so vanilla's plains villages may now stand in those same fields too (added to
  the game's own list, never replacing it).
- **Wet ground:** no structure stands in a swamp, a river or on a beach; Regions Unexplored's (marsh, fen, bayou, deltas, its rivers, reefs,
  beaches) and Oh The Biomes We've Gone's (bayou, cypress swamps and wetlands, mangrove marshes, the dead sea, its shores and beaches) are
  now known as wet.
- **Steep ground:** Regions Unexplored's arid mountains, pine slopes, spires and mauve hills and Oh The Biomes We've Gone's peaks, ridges,
  badlands, Canadian shield, glacier and Skyris vale are now known as steep, so no place is set on them.
- Every one of them counts only if that mod is installed (a pack without it loads as before).

**On the ground:** every place is set on the world generator's own terrain heights (never on trees or plants, whatever a biome mod grows)
and only where the ground is flat and dry enough under its whole footprint, then cleared and graded round. That is unchanged and works the
same in TerraBlender's biomes; what changes is that the biome lists no longer let a modded swamp, river or mountain through.

### Choices of mine you may want changed

- Vanilla plains villages now also spawn in those mods' open fields (so hotels can too). If you'd rather villages stay where those mods
  put them, say so and I'll keep only the hotel's list.
- The biome IDs come from each mod's 1.20.1 source (Oh The Biomes We've Gone) and a community list of Regions Unexplored 0.5.5's.

### Other

- Tests: `NpcTests` (the hotel's exact list, the village tag, the wet and steep entries, no field in either); 11 rules sabotaged one at a
  time (`docs/handoff/sabotage-4.1.90.json`), all caught. The jar's checker lets in that one file of the game's data (the village list),
  nothing else of it.
