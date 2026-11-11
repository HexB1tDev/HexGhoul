## 4.1.42 (HexGhoul): structures once per world, on flat ground, never floating, spread out

HexKagune 4.1.42 for Forge 1.20.1. Not tested in game. **Use a new world to test it:** what an older version already generated stays in
your current world (the copies and the floating ones too); only new chunks follow these rules.

Your words: "How does this generation pass your bug test, bro?"; "They got the fucking Kaneki Tower, the fucking library, and the Intaku all
in the same spot"; "just research the fucking biomes of plenty mods in the basic Minecraft mods and separate them ... I see so many flat
areas they could have a structure and instead just spawns on a fucking mountain"; "The structures are spawning multiple times. There's tons
of them spawning like hundreds of times in the fucking world. And they're all fucking flying"; "the mod is lagging like crazy".

**What was wrong:**
- **Hundreds of copies:** to let `/place` build a place anywhere, the code skipped the "one per world" rule whenever the structure's biome
  test said yes to "no biome at all". In plain Minecraft that test fails, but a mod that rewrites structures' biome tests (Structure
  Essentials can) makes it say yes, so every region of the world built its own Anteiku, library, Ken's tower, mansion, hideout and CCG
  building, with all their people: most likely the lag too.
- **Floating:** those copies, and every place, were lifted to the highest point of their footprint with no flatness check (the hideout and
  the CCG building allowed 24 blocks of rise), and the city judged its land on samples 68 blocks apart. Under them only the game's terrain fit
  filled in (about 12 blocks). Over slopes and valleys: in the air.
- **Clustered:** each place took the first spot the world made, so all of them landed round where you first explored.

**Now:**
- **Once per world, really:** only the `/place` command itself (asking on the live world) skips the rule; the world's own generation never
  does, whatever a mod does to biome tests.
- **Never floating:** under every structure of ours (the city, every place) a foundation of the world's own ground (stone under stone, dirt
  under dirt) is carried up to its floor, column by column, up to 64 blocks.
- **Flat land first:** each place looks over the spots near its start (25 of them) and takes the flattest; none if even that one rises and
  falls more than 6 blocks (10 for the big hideout). The city checks its land every 34 blocks (was 68) and allows 10 blocks of rise over its
  544 x 544 (was 12).
- **No mountains:** mountains, hills, peaks, slopes and plateaus are refused for every structure of ours: vanilla's (Frozen Peaks, meadows
  and groves on mountains, windswept hills, badlands plateaus...), Forge's mountain/peak/slope/plateau tags that biome mods fill, and Biomes
  O' Plenty's and Regions Unexplored's cliffs and highlands by name (each only if that mod is there). Flat biomes of any mod are fine.
- **Spread out:** two one-of-a-kind places are never within 512 blocks of each other.

**Tests now run on made-up land**, not just the math: a valley the old city samples stepped over (now refused), hillsides, the sea, a
steep biome, a floor over air (filled down to the ground), a floor on the ground (left alone), places too near each other (refused).

### Other

- `data/hexkagune/tags/worldgen/biome/steep.json`.
- Protocol 118 (unchanged).
