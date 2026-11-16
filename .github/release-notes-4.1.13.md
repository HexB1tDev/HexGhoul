## 4.1.13 (HexGhoul): the wing slams bend like hands and stay off his body, the guard clamps round him, /kagune places checks each place

HexKagune 4.1.13 for Forge 1.20.1. Not tested in game. Previews: `docs/handoff/ukaku-impacts-4.1.13.png`, `docs/handoff/ukaku-guard-4.1.13.png`, `docs/handoff/coat-crimson-4.1.13.png` and `docs/handoff/coat-formal-4.1.13.png`.

### The M1 wing slams

Your words: "that they actually bend like that, like hands. That's what I fucking wanted. That's it. While being perfectly aligned as well, and it doesn't like clip to the body."

Every slam is rebuilt as an arm and a hand.

- **A shoulder:** the wing's base now comes out of his back sideways, behind his own shoulder, and only then swings forward.
  - Before, it went straight forward through his shoulder.
- **An arm and a hand:** past the shoulder, the wing is an arm. The end of it is a hand that curls toward the target.
  - The arm swings a beat ahead and the hand whips round after it, landing exactly on the hit.
- **Each blow lands where it aims:**
  - **Slap and swing:** the hand whips across his front, past his middle.
  - **Rising wing:** low across his front, then up. It ends high in front of him; before, it ended up and behind him.
  - **Overhead slams:** the arm is raised high over his shoulder and the hand comes down onto the ground about 3 to 5 blocks in front, on its own side. Before, it fell out at his side.
  - **Twin slam:** both wings rise high behind him, then both hands come down together in front of him.
- **Nothing goes through him:**
  - Every slam was tuned frame by frame against his body as the game poses it, with the wings riding his torso.
  - That covers fire and crystal wings, mastery 50 to 1000, and Rabbit Guise's one wing.
  - No part of a wing sinks into his head, chest or arms any more. 4.1.2 to 4.1.12's slams went through his shoulder, his raised arms and the back of his head.
  - The other wing tips out of his head's way while one strikes.
  - In the twin slam's wind-up he keeps his eyes on the target. Throwing his head back put it into the rising wings.
- The slams are as long as in 4.1.12, with no particles.

### The guard

Your words: "I asked for a block animation where the wings like clamp together like hands in front of the player."

- The two wings come forward and wrap round his front, chest to head, overlapping in the middle.
- They do that clear of his arms, which the game raises to guard.
- Smaller wings, at lower mastery, curl less tightly so they never hook back into his head.

### Finding the places

Your words: "And I still can't. Mind you, I still fucking can't."

Each one-of-a-kind place (the hideout, the CCG building, Ken's tower, Anteiku, the library) may stand in **one** chunk of the world only. If anything stops it being built in that chunk, the world has none, and `/locate` finds nothing. Structure Essentials, in your pack, can do that: it has a "minimum distance to other structures" setting.

**`/kagune places` now goes and looks.** Run by a player, it makes each place's chunk and says either:
- **built there**, with its x and z, or
- **NOT built there:** its chunk is made but the place isn't in it, so something stopped it.

To use it:
1. Make a **new world**.
2. Run `/locate structure hexkagune:aogiri_hideout` (and the CCG building's). Even when it fails, that makes the world work out the spots.
3. Run `/kagune places` and send me what it says.

If it says **NOT built there**, look in `config/structureessentials.json` for the minimum-distance (overlap) setting and set it to 0, then try a new world. `/place structure hexkagune:<id>` always builds one where you stand.

### The coat

- 4.1.12's coat (each half of the tails on its own leg) is now previewed off-game: standing, a running stride, a tucked jump, a front kick, a high kick and a side kick.
- Each half lifts with its own leg and the other keeps hanging. Nothing flares out flat.

### Other

- Protocol 116 (unchanged).
