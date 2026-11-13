## 4.1.75 (HexGhoul): Arima falls on his face and lies there before he dies

HexKagune 4.1.75 for Forge 1.20.1. Not tested in game. Protocol unchanged (126).

Your words (quoted exactly): "make him actually lay on the floor face down like falling on his face and lay there a bit".

- **He falls on his face.** After the blade has broken and he has gone down on his knees and bowed his head, he holds still a moment,
  then topples forward from his knees: slowly at first, then faster and faster (nothing holds him up: 0.6 s), about his knees, which
  stay where they rest, his legs straightening out behind him. He hits the ground face down, flat, with a thud.
- **He lies there a bit.** Face down, still, his arms limp at his sides, the broken hilt in his slack hand flat on the ground beside
  him, for about 2.8 seconds; then he dies. His whole end is 10 seconds now (8.5 in 4.1.74).
- **He dies as he lies.** Minecraft tips a dying body over onto its side for its last second; not him: he stays face down.
- **The shards stay where they fell.** The pieces of his blade lying round him no longer move with his body (they would have swung up
  off the ground as he fell).
- Untouchable, ungrabbable, everything off him, as in 4.1.74, until the end.

### Choices of mine you may want changed

- He keeps hold of the broken hilt as he falls (his hand turns with it so it lies flat beside him); he could let go of it instead.
- His head lies in line with him, face to the ground (not turned to one side).
- His last second (Minecraft's own death) still flashes red, as every mob's does.

### Other

- Renders: `docs/handoff/arima-end-4.1.75.gif` (his whole end at real speed, a frame a world tick, three-quarter beside his right side),
  `arima-fall-4.1.75.png` (the whole end), `arima-fall-side-4.1.75.png` and `arima-fall-top-4.1.75.png` (the fall, close, from his right
  and from above).
- Tests: `AgentTests.reaperFallen` (the fall's length; faster every tick; forward, his head before him; face down, flat; on the ground,
  not in it or over it, and nothing through it as he falls; about his knees, kept where they rest; still till he dies and after; the
  hilt flat beside him; a point carried between his body's frames keeps its place, 400 random poses; never tipped on his side; the
  shards carried; the thud); 12 rules sabotaged one at a time, each caught.
