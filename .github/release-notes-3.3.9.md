## 3.3.9 (HexGhoul): Homecomings without a home

HexKagune 3.3.9 for Forge 1.20.1. Not tested in game.

From your list after 3.2.1: **homecomings without a home.** Bringing a child home (carried, or walking at your side, after a while out) only counted if your family had a home set ("This is your home now"). Without one, nothing happened at all. Now a family with no home has homecomings too.

**How it works now:**

- Without a family home, the child's home is **the place it lives**: where it was born, or where you last set it down or told it to wait. It's "out" once it's more than 16 blocks from there, and bringing it back is a homecoming, just like coming home to a house.
- Its other parent (Eto, Touka or Ken), if they're near, meets you there. With nobody there, the child has its own word with you, as before.

**New scenes for it**, because a lot of the old ones talk about the door, the stairs, the house:

- **Eto, Touka and Ken**: four each, for when there's no house. Found: Touka's been pacing the same ten steps of grass; Ken's at the treeline calling. Out together: Eto reading in the grass. Back after dark: Touka by the fire with her kagune half out; Ken holding up a lantern. And one where they bring up building a real home (Eto's never had one; Touka once lived upstairs from a coffee shop; Ken didn't have much of one growing up).
- **The children**: seven. A toddler slapping "our tree"; "Why we sleep outside? Are we birds?"; a big kid who couldn't find the way back because "there's no house to see from far away"; "Are we ever going to have a real house?"
- **The old scenes that speak of a house** (45 of them) now only come up when there is one. 3.3.8's door scenes were reworded so they fit either way.

- **Approximated:** a child out with you that you put down somewhere else settles there (as before), so that spot becomes the place it lives. The 16 blocks is a guess (its leash there is 8).
- No network change (protocol 93). No new Minecraft API.
- Tests: SceneTests (with no home, every companion tier, mood, hour, found or not, carrying or not, still has a scene; every child's age and temper still has two (a baby one); no homecoming scene that mentions a door or a house comes up without a home); FamilyTests (the 16 blocks clear the leash and its play).

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 93. Not tested in game.
