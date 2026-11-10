## 4.1.6 (HexGhoul): The hideout and the CCG building, always somewhere

HexKagune 4.1.6 for Forge 1.20.1. Not tested in game.

Your words: "also I cant get the aogiri hideout to spawn or the ccg building at all."

### What was wrong

- Each world gets exactly one hideout and one CCG building. The game offers about 60 spots for each near the middle of the world.
- Until now each one was built only at the first spot that was **completely** dry: no sea, river, beach, swamp or pond anywhere under it or 8 blocks round it.
- The mansion is 272 x 263 blocks. In many worlds, none of its 60 spots is that dry, so **those worlds got no hideout at all**, and `/locate` found nothing.
- The spot is fixed for each world. In a world made before the building existed, it can fall in ground you'd already explored, and then it never appears there.
- `/place structure` failed for both everywhere except that one fixed spot.

### What changed

- **A world now always gets both** if any of its spots is on land:
  1. First, the nearest spot that is completely dry, as before. A world that already had a hideout or a CCG building keeps the same spot.
  2. If none is, the next ring of spots further out (out to about 5,000 blocks), still completely dry.
  3. If none of those is either, the **driest** of the near spots. Its middle must be on dry land in a biome it may stand in, and it has the least water under it.
- **`/place structure hexkagune:aogiri_hideout`** and **`/place structure hexkagune:ccg_building`** now build them where you stand (operators). Use this in an old world, or to test.
  - The mansion is big: the chunks for about 8 chunks round you must be loaded, so set your render distance to 10 or more first.
  - `/place` doesn't reshape the ground under it the way world generation does.
- **The log names the spot.** The first time a world works out where they go, the game log gets a line:
  `HexKagune: this world's Aogiri Tree's hideout stands at x ..., z ...` (and the same for the CCG building).
- To find them in a new world: `/locate structure hexkagune:aogiri_hideout` and `/locate structure hexkagune:ccg_building`. Both are at least 512 blocks from the middle of the world.
- Nothing sent over the network changed: protocol 114.

### Next

- Kick Master (the General's fighting style) is now 4.1.7.
