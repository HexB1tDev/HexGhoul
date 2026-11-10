## 4.1.4 (HexGhoul): The CCG building

HexKagune 4.1.4 for Forge 1.20.1. Not tested in game.

Your words: "CCG building, tons of ccg agents guard and patrol the area, sit in their offices" and "Also I forgot to send you the CCG building, this is it, also like the others only 1 of them spawns."

This version is the building and its agents. The General, his romance and Kick Master come next, each in its own version.

### The building

- It's your file, `art/exploration/ccg_building.schem`, block for block: the L-shaped tower of seven floors on its stone plate, with the yard in the L's arm.
- Your file is 68 blocks tall with nothing in its top 10 layers. Those empty layers are left off, so it's 39 x 58 x 39 in the world.
- Every block in it exists in 1.20.1, so nothing was swapped.
- All 164 signs are kept, the builder's credit among them.

### One per world, on dry land

- **Exactly one per world**, chosen the way the mansion is: the world offers it one spot in every 640 x 640 blocks, and it's built at the nearest to the middle of the world (at least 512 blocks out) that is dry.
- **Dry** means the 4.1.1 rule: no sea, river, beach or swamp biome, and no water on the ground, under the building or within 8 blocks of it. The ground 8 more blocks out is checked too, because the patrol walks there.
- **Never on top of the mansion or a village.** A spot within 192 blocks of anywhere the mansion or a village could be built is never used.
- **Biomes:** plains, sunflower plains, meadow, cherry grove, savanna, savanna plateau, snowy plains, forest, birch forest and taiga (and the matching Biomes O' Plenty ones).
- It prefers fairly flat land. If none of its spots is flat, it takes the first dry one and the ground is fitted to it.
- `/locate structure hexkagune:ccg_building` finds it.

### Its agents: 38 investigators

- **4 guards outside**, always at the same spots: one either side of the door (the gap in the south wall) and two in the yard. Each keeps watch the same way.
- **6 on patrol**, walking a loop round the building a block or two out from its walls, some one way and some the other.
- **21 at desks**, three on every floor. They sit on the building's own chairs: an office chair in the north-west offices, one in the east wing, and one at the tables.
- **7 in the halls**, one on every floor by the stairs.

They are the investigators you know. They go for a ghoul they recognise, defend humans, and fight with their quinques.

- Someone at a desk gets up the moment there's a fight, a fright or a stun, and walks back and sits down again afterwards.
- A guard pulled away from a post walks back to it and faces the same way again.
- If a chair is broken, its agent stands where it was.

### Your worlds

- Chunks already made don't change.
- In a world made before 4.1.4, the building is built only if its chosen spot hasn't been explored yet. For a sure one, make a new world.

### My guesses

- How many agents (38) and where they stand and sit.
- The patrol's loop, and that it goes round the outside.
- The biomes.
- How far it keeps from the mansion and villages (192 blocks).
- How someone sits: hips just over the seat, thighs level, shins down in front of the chair, a hand on a knee, eyes on the desk, looking up now and then.

### Known limits

- The agents don't use the stairs to change floors as part of their day. Each keeps to its own floor unless a fight takes it elsewhere.
- Anteiku, the library and Ken's tower are placed by the game's rings, which I can't work out ahead of time, so the building isn't kept away from those. They're rare enough that I'd expect it never to matter, but I can't prove it.
- A world with no dry land at any of its spots has no CCG building.

### Network and tests

- **Network:** protocol 113 (a new structure type, and agents now tell clients when they sit). Clients and the server must both be on 4.1.4.
- **NpcTests (`ccgBuilding`):**
  - its spots are the game's own for its set, nearest first, and none is within 12 chunks of a mansion or village spot (every seed tried);
  - every world tried has at least 6 spots left;
  - the built template is read back: 38 investigators, 21 on chairs with room for their knees, 11 at posts, 6 on patrol, desks and a hall guard on all seven floors, all 164 signs;
  - the patrol has stops on every side of the building.
- **AgentTests (`headquarters`):**
  - the way an agent faces turns exactly as the building's blocks turn;
  - a seat is half a block up and toward the chair's open side;
  - the sitting pose keeps the hips over the seat, the knees past its edge and the feet above the floor;
  - anything other than sitting gets an agent up.
- **The full suite.**

Validation: the full `python3 build.py` completed with EXIT 0 on these sources before this was pushed. Not tested in game.
