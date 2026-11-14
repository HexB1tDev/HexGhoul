## 4.1.101 (HexGhoul): world generation works each answer out once (the lag patch, part 3)

HexKagune 4.1.101 for Forge 1.20.1. Not tested in game. Protocol unchanged (128).

You said (quoted exactly): "make handoff mentioning a massive long, lag patch, making sure absolutely nothing in the mod fails and nothing
ever lags." And earlier, of /locate: "The server usually freezes trying to look for it."

**The hotels' plan, once:** every one of the mod's places (the hideout, the CCG building, the mansion, the library, the circus, Anteiku...)
checks it isn't on a hotel's ground before it takes a spot, and every chunk asks whether a hotel starts there. Each of those asks worked
out the villages round it, their four sides and the hotels of the villages before them, some thousands of village positions each time, the
same ones every time. Now each world keeps its hotel plan as it is worked out: in the tests, the same 6,724 chunks asked twice cost 26,648
village positions instead of 4,006,725 (the answers exactly the same).

**The ground under a place's spots, once per search:** a place tries up to 25 spots near its offer, and under each looks at how rough the
ground is (the terrain's height, worked out from the world's noise, column by column) and whether there is water (hundreds of biomes).
The spots overlap, so the same columns and biomes were worked out over and over. Now a search keeps what it has seen: in the tests about
55% fewer height and biome lookups and about a quarter less time for the whole search (the heights are sampled every 6 blocks from each
spot's own corner, so shifted spots share fewer columns; that sampling is unchanged, so places stand exactly where they did).

Nothing about where or whether a place or a hotel is built changes: only how often the same thing is worked out.

Also checked now by the tests, so they can't quietly come back: quest news stays on the action bar (the chat keeps only what NPCs
say and warnings); a mission tried again counts from nothing; `/kagune places` lists where each official is; and the log keeps telling
each official made at her spot, each body of hers sent away (and why) and each death.

Please keep sending `/kagune lag` lines (its last line now also lists the world generation threads' costliest parts).
