## 2.4.3 (HexGhoul): NPCs swim like players, never stuck in water

HexKagune 2.4.3 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

NPCs swim like players now and don't get stuck in water. This covers every NPC in the mod: companions, children, villagers, wild ghouls, investigators and thralls. Before, they only bobbed at the surface, crawled at the game's slow mob pace, and nosed at the bank without getting out.

- **Speed**: heading somewhere, they swim at about a player's pace (around 3.5 blocks a second) the way they face.
- **Head up**: they rise whenever their head is under water.
- **Getting out**: at a bank (a block in front, with room above it) they pull themselves up and out, as a jump would.
- **No path**: when they have nowhere to go (no path through the water), they make for the nearest dry ground within 8 blocks instead of floating in place.
- **Code**: `NpcSwim` (one tick handler for all of them). Protocol stays 84. Not tested in game.
