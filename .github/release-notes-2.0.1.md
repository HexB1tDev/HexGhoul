## 2.0.1 (HexGhoul): the crash fixed, the new village, on dry land
HexKagune 2.0.1 for Forge 1.20.1. Install on client and server (**protocol 72**, unchanged). Compiled, every test suite passes, the dedicated-server check passes. **Not yet tried in game**: please report anything that looks or feels wrong, and it gets fixed first.

### Fixes from your 2.0.0 report

- **The crash on speech bubbles is fixed.** 2.0.0 crashed the game whenever anyone spoke (`NoSuchFieldError: f_181350_`). On top of the fix, a bubble that ever fails to draw is now logged and skipped, never a crash.
- **The right face on the talk screen.** Children showed Eto's face, and villagers showed a companion's or a ghoul's. Now every talk screen shows the skin the person is actually wearing.

### The new village

- **Renamed to plain "village":** `/locate structure hexkagune:village` (was `hexkagune:brick_village`).
- **Your new schematic** (`japanesevillagev4`) is the village now: the hillside town, the walled temple, the lake with its island, the cherry trees and the mountain behind. It is 256 x 256 blocks: every house is in. A strip of the mountain's cobblestone on the north and east and the lake's far east end are left out, because a structure can reach only 128 blocks from its middle. The lake's cut edge is made bank, so no water spills out.
- Blocks from other mods are swapped for vanilla ones: BYG's cherry for 1.20's cherry logs, leaves and pink petals, BYG's pink flowers for pink tulips and petals, Pixelmon's temple blocks for stone bricks. Barriers, command blocks, fire and the Pixelmon shop signs are left out. The old server's sign text (shops, warps, gym records) is blanked. The village's own sign text stays: Iroh's Tea House, the Apothecary, the Police Station, the Temple of Exodius, the Hall of Records, the two verses.
- It is placed sunk, so its ground meets the world's surface.
- **13 houses with 26 villagers** (two each; 26 looks, so none look alike in a village).
- **Only on dry land:** a village starts only where there is no water anywhere under its footprint and the ground is fairly flat (checked at 25 points). No more villages in the sea. Plains and sunflower plains only, as before.
- They are bigger now, so they are kept a little further apart, and spots with water or steep ground are skipped: villages may be a bit rarer than before.
- **Brick Villages already in your worlds keep working:** their people keep the Brick Village's own streets and patrol.

### More CCG investigators

- **6 investigators patrol each village** (2 before).
- Pairs of investigators turn up in the wild **about once a day** near you (once every two days before).

### Known approximations

- At the edges of the village, the world's own terrain meets the schematic's. Where the mountain is cut, it ends in a steep face.
- The village template is large (about 900,000 blocks over 25 pieces). The server keeps it in memory once a village has been generated (about 45 MB).
