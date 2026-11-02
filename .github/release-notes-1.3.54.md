## 1.3.54 (HexGhoul): a game of tag stays in the yard (testing)
HexKagune 1.3.54 for Forge 1.20.1. Install on client and server (**protocol 69**, unchanged). Testing build: compiled, tests pass, **not tried in game**.

Found by reading the code (listed in the last handoff): in tag, the one running away picked a spot five blocks off with no thought for home, so a toddler at play could run right out of the yard, and its parent would come after it as if it had wandered off.

- **The runner stays inside.** It still runs away from whoever is it, a little to one side, but always toward a spot inside its home, at least 2.5 blocks in from the edge. Chased to the edge, it turns and runs along it (whichever way keeps it farthest from the chaser) instead of out; cornered, it ducks back toward the middle.
- **Tag is only played at home.** Two siblings who aren't both inside their home's yard chat instead.

Untested in game: compiled and unit tested only (4000 random chases, none leave the yard). Try: two children at home by day, and watch for a game of tag.
