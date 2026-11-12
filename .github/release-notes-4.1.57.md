## 4.1.57 (HexGhoul): every place cleared and set on the ground; the glass joined

HexKagune 4.1.57 for Forge 1.20.1. Not tested in game. Protocol unchanged (125).

Your words: "AFTER this, please for the love of god, FIX THE FUCKING STRUCTURE SPAWNING PLACEMENT, i asked multiple times to fix it,
CLEAR THE FUCKING AREA OF SPAWN, CLEAR ALL BLOCKS OR TREES, AND THEN PLACE IT DIRECTLY ON THE FLOOR, NOT FLYING, NOT HOVERING OVER THE
FLOOR, I SAID ON IT." And: "Be careful with the glass placement on the structures, the CCG building has all glass pannels unconnected and
not attached to eachother!"

**Why they floated, found this time:**
- Since 4.1.17 every place (Anteiku, the library, the circus, the mansion, the Gas Masks' cottage, the tower) was **lifted onto the highest
  ground of its footprint**. On any slope that left it standing on a raised mound of dirt over its low side: on the render's
  hillside the circus stands 10 blocks up.
- Most of our buildings are saved **without their air**, so when one was placed, whatever the world already had there (the hill, a
  tree's trunk and leaves) **stayed inside it and through it**. Nothing cleared the site first.
- Places were built **before the trees** in each stretch of land, so trees could still grow up against them and into their open parts.

**What it does now, for every place (the CCG building and the Aogiri hideout too):**
1. **Cleared first.** Before the building is placed, its whole site (its footprint and 8 blocks round it) is cleared of everything over
   the floor: hills, trees, leaves, plants, up to 32 blocks over the highest ground (the tallest trees). This is done with plain air
   pieces laid ahead of the building, so it's the game's own way of placing blocks, saved and reloaded like any structure.
2. **On the floor.** Each place stands at the middle height of its own footprint's ground, never on its highest point. The higher side is
   cut down to that floor and the lower side filled up to it with the world's own ground (grass on grass, stone on stone), so the floor
   stands on solid ground: under the building its foundation and the game's own terrain shaping, round it the grading. Nothing is left
   under it: not flying, not hovering. (A gap in the building's own floor, its pool or a stairwell, is never filled.)
3. **Graded round it.** Flat for 2 blocks round the building, then a bank a block up or down per block out to the land: cut into a hill
   (topped with the hill's own grass) or built up over a dip. No cliff, no ledge, no tree left on it. The game's own shaping of the land
   round a structure is told the whole site's ground is the floor, so it carves the site down to it and slopes the land away too (never a
   rim of land raised round it).
4. **Built after the trees.** Places now go in the game's last generation step, after the trees, so a tree generated in the same chunks
   can't grow through them later.

**The glass:** every glass pane and iron bar in every building is now joined to what it touches, the way the game joins them (to other
panes and bars, walls and any full block face). The CCG building had **all 3,134 of its panes standing alone as posts**; they now make
real windows. The Aogiri hideout had 2,490 loose panes, fixed too, plus a handful at the tile edges of the other places.

### Other

- Renders: `docs/handoff/ground-circus-4.1.57.png` and `ground-gas_hideout-4.1.57.png` (the circus tent and the Gas Masks' cottage on
  a wooded hillside, the old way above and the new way below, from the mod's own templates and placement code, with a cut through each
  seen from the side). Before: the tent on a 10-block mound with 1,943 tree blocks in its site. After: on its own floor, 0 tree blocks,
  0 world blocks left over its floor, nothing hanging over air. `docs/handoff/ccg-glass-4.1.57.png`: the CCG building's front and an
  office floor, before and after.
- Places already started in an existing world before this version keep their old pieces where they haven't generated yet. New places
  (a new world, or land not reached yet) get all of this.
- The Small City wasn't touched (it's being replaced by your hotel).
- Next: the menus go on (Rinkaku next), then the to-do, your new list and the hotel.
