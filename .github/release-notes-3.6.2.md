## 3.6.2 (HexGhoul): Bikaku: The Walker

HexKagune 3.6.2 for Forge 1.20.1. Not tested in game.

Your words: "the fourth before the Kakuja should be like the player no longer even like walks with their own legs, like they get like two separate appendages on either, like both sides emerging from the same spot as the worms, that when the player walks his body doesn't move, but like he controls these tentacles that walk around adapt to terrain. Being able to climb anything, like Dr. Octopus pretty much."

- **The Walker (mastery 750)** can now be picked in the menu's FORM list. The menu shows it as "3 worms, 2 legs, one root".
- **Two walking tentacles** come out of the same root as the worms, one on each side. They arch out over your hips and down onto the blocks around you, and each one's mouth bites the block it stands on.
- **They walk for you:**
  - Your own legs hang still and you are lifted a little off the ground.
  - As you go, the feet step one at a time, each put down a little ahead of you.
  - They find the ground around you: up steps, down off ledges, onto the top of whatever is there. A foot with nothing in reach hangs.
- **Climbing:**
  - Walk into any wall and you go up it at a ladder's pace, the tentacles gripping its face.
  - Hold sneak to hang on the wall. Let go to slide down it slowly.
  - Falls do you no harm while The Walker is out.
- **Moves:** The Walker makes every move with its three worms, as Three Worms does (3.6.1).
- **Unchanged:** the kakuja (the legs go when you turn into it) and your walking speed.
- The path's card now reads "Two worms, then three, then legs".

Preview (off-game): `docs/handoff/bikaku-walker-3.6.2.png` shows these rows:
- At rest from round about.
- Walking.
- Up two steps.
- Up a wall.
- Off a ledge.

- **My guesses:**
  - "Two separate appendages on either" read as two in all, one each side. If you meant two on each side (four), say so.
  - Your legs hanging still, lifted about a fifth of a block.
  - Climbing covers walls only, not ceilings.
  - No fall damage in The Walker.
  - Its moves are Three Worms' moves; the legs make none of their own.
  - Mastery 750.
  - The feet biting the blocks they stand on.
- **Next:** your forms ask is done (One, Twin, Three, The Walker, then the kakuja). Tell me what you think of The Walker, and whether its legs should add anything to the moves.
- Network: protocol 102 (unchanged; the form was already sent). No new Minecraft API: blocks are read through `ClientLevel.getBlockState`, which the original mod already uses.
- Tests:
  - BikakuWormTests (143,274). Standing, a foot is planted on the ground out to each side. Walking and running on flat ground, up steps and off them:
    - the feet step one at a time and both step in turn;
    - a foot never jumps;
    - a planted foot is always on top of a block with open air over it, and never further than it reaches.
  - Climbing a wall, the feet hold onto its face. After a teleport, the feet are put down at once.
  - A walking tentacle runs from the root to its foot and is never drawn through you. The Walker opens at 750 and falls back to Three Worms below it.
  - Also run: BikakuTests, ControlTests, PathTests, PathScreenTests.

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 102. Not tested in game.
