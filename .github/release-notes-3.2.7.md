## 3.2.7 (HexGhoul): Fix: Peace's shields looked like shields and floated off the tails

HexKagune 3.2.7 for Forge 1.20.1. Not tested in game.

The owner's bug report (Peace): the Aegis of :Re shields are too shield-like and float above the tails.

**What caused it.**

- Each of the three was drawn as a **knight's heater shield**: a white plate with square shoulders and a point, a silver rim, a crimson cross with a glowing heart where its arms met.
- Its tendril **stopped short of it**: the tendril ended a quarter of a block behind the shield, and the shield sat about 0.4 blocks further out, held in the air.
- The top one **hung flat over his head**, above everything on his back.

**The fix: bulwarks grown on the tendrils.**

- Each tendril now **opens out like a hand** into a cupped fan of its own crimson flesh. Seven white bone ribs hold it open from a bone knot (a red core in it, beating) where the tendril comes in. The flesh **sags in between the ribs** to a ragged, scalloped edge, and **each rib runs on past the edge into a claw** (the top one longest, hooked over).
- No rim, no cross, no heater shape.
- The tendril runs **into a flared stalk on the bulwark's back**, so the plate grows on it and never hangs off its end.
- It **breathes**: the webs swell and settle, the claws flex.
- **The top one rises behind his head** out of the kagune on his back, like a hood leaning over him, instead of hovering above it. **The side ones hang closer** at his sides.
- What they do is unchanged: which one meets a blow, the shove, the grab and the catch.
- The small white scales down every Peace tendril are drawn as before.

**Something to look at in game.** At rest, seen from the front, the side bulwarks now blend into the crimson mass on his back. They stand out once one swings out to meet a blow. If you want them to read more as a defence at rest (brighter webs, or held further out), say so.

- Preview: `docs/handoff/peace-aegis-3.2.7.png` (front, three quarters, side, behind, above, one meeting a blow).
- No network changes (protocol 92). No new Minecraft API.
- Tests (KingTests):
  - every bulwark, at rest and swung out, is one piece with its tendril (checked surface to surface; the old shields come out as two pieces each);
  - a plate held 0.4 blocks off its tendril is caught as floating;
  - none rests over his head, and the side ones are within 1.3 blocks of him;
  - the outline reaches out to each rib and sags at least 15% in between.

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 92. Not tested in game.
