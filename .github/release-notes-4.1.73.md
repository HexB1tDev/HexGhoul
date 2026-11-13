## 4.1.73 (HexGhoul): NPCs never in the ground; lag, structures and official NPCs checked

HexKagune 4.1.73 for Forge 1.20.1. Not tested in game. Protocol unchanged (126).

Your words (quoted exactly): "Run a lag prevention check / Make sure NPCS dont spawn in the ground at any point, especially official NPCS. /
Make sure Structures are unique and spawn correctly. / Make sure every OFFICIAL npc is spawning correctly and are indeed working."

### NPCs never in the ground

- **Where they were put down wrong:** every home of an NPC is kept as a whole block, so whenever one was put there (an official NPC coming
  back after dying or after going missing, one sent home, one put home stuck in a wall or stalled, a child sent home, an ally ghoul
  brought up behind you) it was set at that whole height: half into a slab or a stair under it, into a carpet, or into whatever was built
  there since, and nothing checked there was room. Now each is stood on its floor there: the top of what's really under it (a slab's half,
  a carpet's sixteenth), up to three blocks over or two under, wherever its whole body has room.
- **A guard over every NPC of the mod's** (official ones, children, humans, ghouls, investigators, thralls): every half second, one found
  in the blocks round it, twice running, is stood on the nearest floor with room. Never one being held or carried, riding, asleep, or an
  investigator sat at its desk.
- **Every NPC written into a structure checked** (a new tool, `tools/npc_spots.py`, run by the tests): all 117, their whole body against
  every block's shape. Fixed: the General and the CCG building's lobby staff started a sixteenth into the carpet; a Gas Mask in the garden
  house started almost a block over its carpet. Now none is in a block or over nothing (the CCG's desk investigators start in their
  chairs and sit on the step at once, as before).

### Lag

Checked: every wide search for nearby bodies (each NPC's runs every few ticks, staggered, not every tick), what's sent to players (only
to those near, only when something changed), chunk loading (the ground NPCs look up to wander, flee or spawn on stays inside the
chunks the game keeps loaded round them; their homes' chores look first whether a block is loaded; `/kagune places check` loads on
purpose), memory that could pile up, the client's per-frame work, and the hotel's worldgen. Fixed: an investigator whose partner died kept
searching 72 blocks round it every second for as long as it lived; now every 30 seconds once its partner is lost.

### Structures and official NPCs

- **Structures:** each of the seven one-of-a-kind places (the Aogiri hideout, the CCG building, Anteiku, the library, the Reaper's
  mansion, the circus, the Gas Masks' hideout) is claimed once per world as it is built (`/place` too), and every other spot builds
  nothing; their worldgen files, sets and templates all match. Nothing needed fixing.
- **Official NPCs:** Eto (library), Touka and Ken (Anteiku), Ayato and Noro (the hideout), the General (the CCG building), Roma (the
  circus), Banjou (the Gas Masks' hideout): each written into exactly its own place, once; each entity has its attributes and renderer,
  each is registered to come back at its home after dying; Arima keeps his one-per-world slot. Their coming back is the fix above.

### Choices of mine you may want changed

- The CCG building was rebuilt from your file for the NPC heights, and its 153 light blue banners now carry the gray middle stripe and
  border your file gives them (they were plain light blue: the converter learnt to keep banner patterns in 4.1.72). Say if you want them
  plain again.
- An NPC buried deeper than three blocks is left for the official NPCs' own recovery (put home); a wild one that deep stays (rare: only
  if the ground was built up over it).

### Other

- Renders: `docs/handoff/npc-spot-4.1.73.png` (the Gas Mask in the garden house, its body's box before and after),
  `docs/handoff/ccg-banner-4.1.73.png` (a sketch of the banner before and after).
- Tests: `NpcTests.footing` (the floor rule on slabs, carpets, fences, raised and lowered ground, a low ceiling; every place that puts an
  NPC down; the guard's limits; every template's NPCs), `NpcTests.lag173`; each rule sabotaged fails.
- Published later than it was pushed: its first GitHub builds failed, the runner having no numpy or Pillow for `tools/npc_spots.py` (the
  tests run it). The workflows now install them (`tools/requirements.txt`). The mod is the same 4.1.73.
