## 1.3.82 (HexGhoul): bug and lag pass (testing)
HexKagune 1.3.82 for Forge 1.20.1. Install on client and server (**protocol 72**, unchanged). Testing build: compiled, tests pass, **not tried in game**.

You asked: "Check for bugs, patch them, make sure theres no lag or unecessary lagging shit."

What I checked (by reading the code; I can't run the game or a profiler here):
- **Every per-tick hook** (67 of them, server and client). Each one returns early on the wrong tick phase, and most also return early when there is nothing to do or run only every few ticks.
- **The widest searches for nearby mobs.** The 128-block ally count only runs when you talk to a ghoul. The CCG's and wild ghouls' searches for a target run every 10 ticks, and they check who they want before the costly line-of-sight test.
- **The villagers' whole tick**, end to end (it grew the most lately).

What I fixed:
- **Lag:** a calm villager now looks for danger every 4 ticks instead of every tick (in 1.3.81, since there are twice as many). A frightened or hurt one still looks every tick.
- **Lag:** the children's renderer rebuilt its texture names several times a frame for every child drawn. They're made once now.
- **Bug:** a villager walking up to you for a kiss or a hug could follow you out of the village, and villagers never leave it. Now it does it from the edge.

Nothing else turned up by reading. If you notice lag in game, tell me where (near a village, a fight, a final kakuja...) and I'll go after it.
