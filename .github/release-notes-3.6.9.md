## 3.6.9 (HexGhoul): Fix: the Bikaku kakuja shatters when you turn back

HexKagune 3.6.9 for Forge 1.20.1. Not tested in game.

Your words: "Also Kakuja detransformation needs a simple shatter animation like the rinkaku has."

- **Turning back, the kakuja now breaks into pieces** the way the Rinkaku's does, using the same machinery. The pieces are about 0.7 blocks across. They fly off its middle, spin, bounce once on the ground and fade over about 2 seconds, with the same glass-break sound.
- **Before:** it shrank back into you over 1.5 seconds.
- **The worm** grows back out of your back as before, while the pieces fall.
- **When:** whenever the kakuja ends: K, running out of energy, or the kagune put away. Every player who can see you (within 64 blocks) sees it. Nothing new is sent over the network; each client sees the kakuja end and breaks it itself.
- **If you leave it before it has fully grown,** what had grown so far is what breaks.
- **Your camera** comes back down from the kakuja's size smoothly, as in 3.6.8.
- **My guesses:** the piece size (0.7 blocks); the kakuja breaking as it stands, its tentacles still and not mid-bite; the same force and fall as the Rinkaku's.
- Preview: `docs/handoff/bikaku-shatter-3.6.9.png` (two views: in the kakuja, then 0, 4, 8, 14, 24 and 36 ticks after turning back).
- Network: protocol 103 (unchanged).
- Tests: BikakuWormTests `kakujaShatter()`:
  - its low-detail mesh fits the pieces' budget whole;
  - every quad goes into a piece;
  - 90% or more of the pieces fly further out (over half a block on average in 12 ticks);
  - none goes into the ground, and all are gone after 44 ticks;
  - the worm grows back;
  - the renderer no longer draws it falling away, and the client breaks it the moment it is told you turned back.
  - Also the full suite.

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 103. Not tested in game.
