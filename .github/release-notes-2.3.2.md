## 2.3.2 (HexGhoul): Rinkaku strikes go round the right side again

HexKagune 2.3.2 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

Fix: Rinkaku strikes came round the wrong side. A strike from your right tail swept round through your left side, and the other way round, in every Rinkaku form. 2.1.6 changed which side a tail is "on" to stop tails swinging across on their way back. That side also feeds the code that steers a strike round your body, and that code's "+1" is your left even though it calls it right, so every strike passing you went round the opposite side. Strikes now go round the tail's own side again. The 2.1.6 fix for tails swinging across on their way back is kept.

- `RinkakuRoot.clearance` gives `BodyClearance` its own convention. `StrikePaths` uses it.
- `ControlTests` now checks that every tail, laid on either side or on neither, is carried round its own side when it strikes straight through you.
- Protocol stays 84. Not tested in game.
