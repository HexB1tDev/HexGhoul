## 4.1.36 (HexGhoul): the Small City in every world, the villages gone, humans in vanilla villages

HexKagune 4.1.36 for Forge 1.20.1. Not tested in game. Previews: `docs/handoff/small-city-placed-4.1.36.png` (the city as the game lays
it, its four quarters in their slots, at all four turns, on a stretch of grass), `docs/handoff/small-city-on-land-4.1.36.png` (on rolling
land with a river running under it: the ground fitted as the game fits it, roughly; drawn by our tool, not taken from the game).

Your words: "ALl villages get replaced with the city in my minecraft world that I sent named "Small-City". Delete all ids hexkagune:village_x
( and anything related) add Hexkagune:small_city. This big structure is relatively rare (a bit rarer than a normal village), Humans can now
however spawn in normal minecraft villages." You chose: "Whole city". Then: "Also, that city should be able to go over rivers. It doesn't really matter. Just make sure it blends well with
the terrain."

**What it was:** two towns of the mod (`hexkagune:village_1`, the human town, and `hexkagune:village_2`, the CCG settlement) in their own
set, and the old jigsaw village (`hexkagune:village`) still in the files.

**Now:**
- **`hexkagune:small_city`:** your whole city, 544 x 544, in new chunks. It's built as four quarters that always come together: each one
  works out the city's turn and its ground on its own and gets the same answer, so the four line up side by side as one city, turned any of
  the four ways, all at one height, or none of them is built. Checked on the render: at every turn the quarters
  land exactly where the whole city turned as one would put them.
- **Rarer than a vanilla village:** at most one city in each 48 x 48 chunk region (vanilla villages: 34), and only where the land is fairly
  flat (it rises and falls 12 blocks at most over the 544 x 544). Two cities never touch.
- **Over rivers, blended with the land:** a river, a beach or a swamp may run under it; a sea or a lake deeper than 8 blocks may not. Its
  streets go on the middle height of the land (a river's surface counted), never under sea level (so no street floods). The game's terrain
  fit cuts the land off above its streets inside it and fills the land up to them at its edges, easing it in over about 12 blocks all round.
  A river running into it stops at that slope (the city's ground covers it; there are no bridges for it to pass under).
- **`/locate structure hexkagune:small_city`** finds it (it points at the city's north-west quarter).
- **Kept apart:** the hideout, the CCG building, Anteiku, the library, Ken's tower and Arima's mansion never land in a city (each checks,
  before taking a spot, whether a city is really built there), and a city is never built over one of them in an older world.
- **Deleted:** `hexkagune:village_1`, `hexkagune:village_2`, `hexkagune:village`, their structure set, their templates and pieces, their
  maps and the tool that made the old village. (In an old world, the towns already built stay as blocks; their people keep living there.)
- **Humans in vanilla villages:** every vanilla villager near a player is looked at once: one in two brings a human with it, unless four
  humans are already within 48 blocks. So a vanilla village (new or in an old world) gets a few humans; a villager farm never fills up with
  them; a wandering trader brings none. They wander and talk like any human outside a town.

**Watch out:**
- **Old worlds:** a city only goes in chunks that haven't been made yet. Right at the edge of land you'd already explored before this
  update, a city can come with a quarter missing.
- **`/place structure hexkagune:small_city`** won't build one (a city needs its four start chunks); use `/locate`.
- If another mod (Structure Essentials) replaces the city's placement, no city is built at all, rather than a broken one.

### Other

- `tools/city_render.py --placed T` draws the city as the game lays it (and checks the quarters against the whole city).
- Protocol 117 (unchanged).
