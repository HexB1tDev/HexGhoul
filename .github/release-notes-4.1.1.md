## 4.1.1 (HexGhoul): No structure in water

HexKagune 4.1.1 for Forge 1.20.1. Not tested in game.

Your words: "bug, make sure NONE of the structures spawns in water or collides with water bodies, the mansion is currently happily resting at the very bottom of the ocean..."

### Why the mansion was in the sea

- The hideout had one fixed spot in each world. In 4.0.2 I let it be built there whatever the ground, so that every world would have one. When that spot was the sea, it went on the sea floor.
- The villages looked at the ground only 25 times over 272 blocks, and allowed a little water at their edge.
- Anteiku, the library and Ken's tower never looked at the ground at all.

### Now

- **Every structure checks the land under it and 8 blocks out round it.** Where there's any water, it isn't built there:
  - **Biomes, every 4 blocks** (the biomes' own grain): no sea, river, beach, stony shore or swamp (mangrove swamps included, and Biomes O' Plenty's marsh, bog, wetland and floodplain).
  - **The ground, every 16 blocks:** no water lying on it (a lake or a pond).
- **The Aogiri hideout (the mansion):**
  - Each world now offers it a spread of possible spots, one in every 640 × 640 blocks. They're the game's own spots for the hideout's structure set.
  - It's built at the nearest one to the middle of the world (at least 512 blocks out) that is dry land in one of its biomes.
  - There's still exactly one per world, and every part of the world agrees which spot it is.
  - It prefers fairly flat land. If no spot is flat, it takes the first dry one and fits the ground to it.
  - `/locate structure hexkagune:aogiri_hideout` finds it.
- **The two villages:** if the spot they start at is wet, they try up to 8 others nearby (32 blocks apart) and take the first that's dry, fairly flat and in one of their biomes.
- **Anteiku, the library and Ken's tower:**
  - Each is moved a chunk at a time, up to 5 chunks away and nearest first, to the first dry spot in one of its biomes.
  - Each stays within reach of its start, so it's built whole.

### Your worlds

- Chunks already made don't change: the mansion on your sea floor stays there.
- In a world made before 4.1.1, a second hideout can be built on dry land far off, in land not yet explored.
- For a dry hideout, make a new world.

### My guesses

- How closely the ground is looked at (every 4 blocks for the biomes, every 16 for water on the ground). A pond under 16 blocks across that isn't in a wet biome could still slip between two looks.
- Which biomes count as wet: `data/hexkagune/tags/worldgen/biome/wet.json`.
- How far the places may move (8 tries for the villages, 5 chunks for the others), and the hideout's spacing (640 blocks).

### Known limits

- A world with no dry land in any of its hideout's 64 offered spots (for example a world of all ocean) has no hideout.
- A village whose spot and nearby spots are all wet isn't built there. Villages may be a little rarer by the coast and rivers.

### Network and tests

- **Network:** protocol 110. There's a new structure type (the registries differ), so clients and the server must both be on 4.1.1.
- **NpcTests:**
  - the columns looked at cover every structure and the ground 8 blocks round it, never further apart than their step;
  - a lake 16 blocks across, a wet biome's patch, or a river, anywhere under a structure or at its edge (tried at every block), keeps it from being built there;
  - the sea is seen at the first look;
  - the hideout's spots are the game's own (its random written out step by step), nearest first;
  - the hideout takes the first dry, flat spot, then the first dry steep one, and none if all are wet;
  - every structure uses a dry-land type, and none of their biome lists has a wet biome.
- **The full suite.**

Validation: the full `python3 build.py` completed with EXIT 0 on these sources before this was pushed. Not tested in game.
