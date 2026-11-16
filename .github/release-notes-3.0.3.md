## 3.0.3 (HexGhoul): No pause at the start of the finger-crack pull-out

HexKagune 3.0.3 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

The pause at the start of the finger-crack pull-out is gone: the arm moves the moment you press the key, and the kagune comes out about a quarter of a second sooner. Not tested in game.

- **The cause:** two slow starts stacked. The player's body-animation layer fades in from nothing when the crack begins (20% a tick), and the crack was drawn through that fade. On top of that, the arm's rise began with an ease-in that barely moved for its first few ticks. Together, nothing visible happened for about a quarter of a second.
- **The fix:** the crack is now laid on after the layer's fade-in (`KaguneBodyAnimation.finish`), so it is at full strength from the first frame and blends in from wherever the body is. The arm's rise eases out instead: it starts moving at once and slows into its reach (`EtoPose.crackWeight`).
- **Timing:** the rise is shorter (10 ticks, was 14) and the crack lands at tick 17 (was 22), so the kagune bursts out at tick 21 (about one second). Ken's own finger crack uses the same timing.
- **Sounds:** the crack, the burst and the rip now load with the resource pack instead of on first play. The game reads a sound's file the first time it is played, which could make the very first crack of a session late.
