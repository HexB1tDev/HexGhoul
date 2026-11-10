## 4.1.9 (HexGhoul): the inventory crash, and every special place findable in any biome

HexKagune 4.1.9 for Forge 1.20.1. Not tested in game.

Your words: "Important top priority bugs: Cant find CCG building, Aogiri Hideout or Kaneki's building AT ALL not even with locate. Opening the inventory crashes the game instantly." Then: "it should be able to spawn literally in any biome as long as there's only one of each structure aside from the villages, of course. It should also be fully compatible with Biomes of Plenty".

### The inventory crash

- **Fixed:** opening the inventory (the creative tabs) crashed with `AbstractMethodError ... ControlItems ... m_257865_`.
- The mod's four creative tabs (HexKagune, Quinques, Outfits, Masks) listed their items under the wrong method name for 1.20.1, so the game found no list at all. They now use the game's own name.
- A test now reads the built jar and fails the build if that ever comes back.

### Finding the places

- **Why nothing was found:** every place had its own short list of biomes it could stand in (mostly vanilla plains, forests and savannas, some Biomes O' Plenty ones). Every biome a mod added that wasn't on the list (your pack's Regions Unexplored biomes, for one) counted as no place at all. When the land around the world's middle was those biomes, the hideout, the CCG building and Ken's tower had nowhere to go, and `/locate` had nothing to find.
- **Now any biome:** every place (the hideout, the CCG building, Anteiku, the library, Ken's tower and the villages) stands in any biome of the overworld, a mod's too: Biomes O' Plenty, Regions Unexplored, anything. Only water keeps them out, as before: no sea, river, beach or swamp under them or beside them, a mod's swamps and waters included.
- They still never appear in the Nether, the End or the Aether.

### One of each

- The hideout, the CCG building, **Anteiku, the library and Ken's tower are one per world** each. Anteiku, the library and the tower used to be three each.
- Each is built at the first spot on dry land nearest the world's middle (at least 512 blocks out), kept well apart from the others and from the villages.
- Villages are still many, as rare as Minecraft's.
- The spots are read from the world's own structure settings, so a mod that changes every structure's spacing (your pack has Structure Essentials, which can) moves them with it instead of losing them.

### To find them

- In a **new world**, `/locate structure hexkagune:aogiri_hideout` (or `ccg_building`, `cyber_tower`, `anteiku`, `library`).
- The game's log names where each one stands the first time the world works it out (search it for "HexKagune: this world's").
- In an **old world**, a place whose spot was already generated before 4.1.9 can't appear there any more. `/place structure hexkagune:<id>` builds one where you stand.

### Other

- Protocol unchanged (116). The places are decided by the world (the server), so on a server it's the server's jar that matters for them.
