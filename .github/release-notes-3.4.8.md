## 3.4.8 (HexGhoul): Fix: the Bone Greatsword's moves rebuilt (both hands on the grip)

HexKagune 3.4.8 for Forge 1.20.1. Not tested in game.

Your report: *"You need to fix the animations of the three new [quinques] the heavy sword, the whip, and the hammer. Especially the heavy sword, if you look at the moves, if you look at the sheet, you can tell that the moves are ridiculously stupidly broken ... It is completely botched."* Then: *"You can leave the hammer and the whip as they are, they're good."*

What was wrong with the greatsword:

- **The left hand wasn't on the grip.** An arm here is a straight stick, and the sword was set in the right fist at one fixed angle. So for most blade angles the left hand couldn't reach the handle, and it hung up to half a block off it.
- **The blade went where the arm's angle sent it**, not where the swing should go. That's why the sheet showed it flicking between unrelated directions (sideways at the moment a downward cut lands, straight down along the leg after it).
- **Worse in game than on the sheet:** the body's settle step clamped the arm's turn, so the overhead blows (the overhead cleave, the Leaping Cut) were drawn with the arm up to 90 degrees off.

What's new:

- **The sword turns in the hands, as a real two-handed sword does.** Each moment of a blow now says where the blade points, where the hands are (out in front, up before the brow, low before the belly) and which way the edge faces. Both hands stay on the grip every frame (within 0.1 px), and the edge leads every cut.
- **All six blows of the string, rebuilt:** falling cut (raised over the right shoulder, cut down across to the left hip), rising cut (low left up to high right), level sweep (wound right, flat through them at chest height), thrust (drawn back low, driven out with a long stride), spinning cut (a whole turn to the right, the blade coming round into them three-quarters of the way), and overhead cleave (raised high, hands before the brow, straight down the middle).
- **Its three moves:** Leaping Cut (raised overhead through the leap, cut down on landing), Cleave (wound far behind the right shoulder, one great flat sweep) and Bone Wall (the blade level across the chest, its flat square to them).
- **Unchanged:** the stance, walking and running with it, the first-person view, timings, reach, damage, and the hammer, whip and spear (checked pose by pose: identical).

The new sheet: `docs/handoff/bone_sword-moves-3.4.8.png` (the old one is `bone_sword-moves-3.0.1.png`). It's an off-game render: same model, poses and grip as the game.

- **Approximated:** the exact angles and timings of each blow are my choice. The raised blade can graze the top of the head by under a pixel at the top of the Leaping Cut.
- No network change (protocol 93). No new Minecraft API.
- Tests (new GreatswordTests): every frame of every blow and move keeps the left hand on the grip, draws the grip where that hand holds it, keeps the blade out of the chest, never jumps between frames, and ends in the stance. The stance is unchanged, and the other kagune weapons turn nothing in the fist.

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 93. Not tested in game.
