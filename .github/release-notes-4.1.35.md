## 4.1.35 (HexGhoul): the Small City, cut for 1.20.1

HexKagune 4.1.35 for Forge 1.20.1. Not tested in game. Previews: `docs/handoff/small-city-4.1.35.png` (the whole city, rebuilt from the
game's tiles), `docs/handoff/small-city-middle-4.1.35.png` (where its four quarters meet, up close).

Your words: "ALl villages get replaced with the city in my minecraft world that I sent named "Small-City". Delete all ids hexkagune:village_x
( and anything related) add Hexkagune:small_city. This big structure is relatively rare (a bit rarer than a normal village), Humans can now
however spawn in normal minecraft villages." You chose: "Whole city".

**What it was:** your world save (`art/city/small-city.zip`), saved by a newer game, unread by the mod.

**Now (part 1 of 2: the city itself, not yet placed in worlds):**
- **The whole city, 544 x 544,** read out of your save and cut into **four 272 x 272 quarters** (one structure can only reach 272 blocks), all
  on the city's one ground (y 64 in your world), 138 blocks tall (to the top of the highest tower).
- **Every block is one 1.20.1 has:** a pale oak sign is a birch sign, tuff bricks are stone bricks, wildflowers are pink petals (laid the same
  way). The patterned banners keep their patterns (the grey-bordered ones and the orange diamond ones). The shelves become their wood's
  trapdoor (as in the mansion). Your file is not changed.
- **No silverfish:** the city had 1,732 infested stone blocks (they look like plain stone and let silverfish out when broken); they're plain
  stone bricks and stone now. (The old town had one; it's plain too.)
- **Its people:** in every quarter, 24 humans in 12 homes and two investigators on patrol.

**Left (4.1.36):** the four quarters placed together in worlds (rarer than a vanilla village, at one ground height), every
`hexkagune:village_x` and everything related deleted, `hexkagune:small_city` in their place, humans in vanilla villages.

### Other

- `tools/city_structure.py` (reads the save), `tools/city_render.py` (the isometric picture of the city from its tiles).
- Protocol 117 (unchanged).
