## 2.0.2 (HexGhoul): villages, Anteiku, the Cyber Tower and the library in many more biomes
HexKagune 2.0.2 for Forge 1.20.1. Install on client and server (**protocol 72**, unchanged). Built and tested by CI. **Not yet tried in game**: please report anything that looks or feels wrong.

### Fixes from your 2.0.1 report ("currently cant even find a village with locate structure")

- **More biomes, not just plains.** All four structures can now spawn in plains, sunflower plains, meadows, cherry groves, savannas and snowy plains. Anteiku, the Cyber Tower and the library can also spawn in forests, birch and dark forests and taigas.
- **Biomes O' Plenty biomes too** (used only when BOP is installed). All four can spawn in fields, grassland, pastures, prairie, lavender fields, orchards, shrublands, highlands, lush savanna, drylands, tundra, fir clearings, aspen glades, cherry and snowblossom groves and forested fields. Anteiku, the Cyber Tower and the library can also spawn in woodlands, seasonal, coniferous, maple, Mediterranean and redwood forests, mystic groves and origin valleys.
- **The village's land check is gentler:** the ground may differ by up to 12 blocks (was 8), and a river or pond may touch 2 of its 16 edge points (was no water anywhere). The middle must still be dry, so there are still no villages in the sea.
- **Villages are a little closer together** (spacing 40 chunks, was 48).
- **A safer start:** the village now calls vanilla's own placing directly, with a fallback, so it can't silently never generate.
