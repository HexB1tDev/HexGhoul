## 4.1.72 (HexGhoul): the hotel replaces the Small City

HexKagune 4.1.72 for Forge 1.20.1. Not tested in game. Protocol 126 (the structure types differ: the Small City's gone, the hotel's
added), so clients and servers must both have 4.1.72.

Your words (quoted exactly): "To-do list addition: / from now on the small city is gone, erased. / From now on close to a village will be
an hotel where humans work, live and walk around doing their own thing. / CCG patrols around the hotel at all times." Asked how many, you
chose "Beside some villages". And this session: "Only spawns on plains or biomes o plenty equivallent biomes, we cant have that shit
spawning on mountains".

- **The Small City is no longer built.** Its structure, its placement, its tiles and its plan are gone. Worlds that already have one keep
  what was built, and its people keep their streets, homes and patrol (their maps are kept). Chunks of an old world not yet generated get
  no new city pieces.
- **Hotels beside villages:** your `humanhotel.schem`, built as it is (191 x 196, 194 tall, its basement cut into its own stone slab),
  beside about one village in four. It stands on one side of its village, its start 14 chunks (224 blocks) out, its near edge about 118
  blocks from the village's start: its cleared ground and terrain fit stop short of where any of the village's houses can reach. It is
  turned so its forecourt (the fountain plaza, the porte-cochere, the taxis) faces the village.
- **Only on plains:** every chunk of its footprint, and a little round it, must be plains: vanilla's plains, sunflower plains or snowy
  plains, or Biomes O' Plenty's field, grassland, pasture, prairie or lavender field. Never on a mountain, a hill, a forest's edge or
  anything else. It also needs the same dry, fairly flat land as the Aogiri hideout (no sea, no lake, no rise of more than 14).
- **Never on another village or another hotel:** the side it takes is the first (from one the world's seed gives each village, round
  clockwise) with no other village anywhere near; of two hotels that would overlap, only one is built. The hideout, the CCG building,
  Anteiku, the library and the rest stay clear of every hotel.
- **Its people:** 20 humans live in it, two at each of its 10 homes: 6 homes in the staff rooms under the street (the kitchens, offices
  and laundry of its working floor), 4 on the ground floor. 6 CCG investigators patrol round it on a loop of 20 stops on its roads and
  forecourt, every stop outside under the open sky.
- `/place structure hexkagune:hotel` builds one where you stand, any biome, turned at random.

### Choices of mine you may want changed

- Snowy plains count as plains (Forge's own plains tag counts them too). Say if you want them out.
- Biomes O' Plenty's own "plains" tag also holds its highland, rocky shrubland, tundra, lush savanna, orchard, overgrown greens and
  shrubland; I left those out (the first two are hills). Say if you want any of them in.
- The plan the last session left put the hotel 12 chunks out, on a random side, and checked nothing else. I measured it: on the north
  and west sides its clearing could cut into the village's outer houses, and over eight seeds 166 of 597 hotels (28%) had another
  village's spot within reach or overlapped another hotel (by the spots alone: whether a spot holds a real village depends on its
  biome). 14 chunks out and the side rule fix both: now none does.
- With plains only, fewer villages get a hotel than one in four: at most about one plains village in four (its spot must also be flat and
  dry); a desert, savanna or taiga village gets one only if plains lie on its hotel's side.

### Other

- Renders: `docs/handoff/hotel-se-4.1.72.png` and `hotel-nw-4.1.72.png` (the whole hotel from two sides), `hotel-street-4.1.72.png`
  (street level cut away: the working floor under the street and the ground floor, its humans, its investigators and their patrol),
  `hotel-village-4.1.72.png` (a village with its hotel on each side, turned to face it).
- Tests: `NpcTests.hotel` (the site and its tiles, its 20 humans and 6 investigators, the plains tag and the footprint's biomes, the
  side rule, the distance, hotels never on another village or each other, every chunk near a hotel known to the other places, all
  checked against vanilla's village spread on four seeds); each rule sabotaged fails.
