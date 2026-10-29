HexKagune 1.0.4 for Forge 1.20.1 (needs PlayerAnimator, bendy-lib and Photon on clients; the server needs only Forge). Install on both the client and the server; everyone on a server needs this jar.

This release carries everything built since 7.0.1 (the last release of the original repository): the ghoul NPCs, the Furuta ability rework with its PvP pass, and Kaneki's Kakuja V2, the Dragon.

**Kakuja V2: the Dragon (Kaneki DNA)**
- **The quest.** A new Kaneki quest, *Kakuja V2: The Dragon*, opens at mastery 1000. It asks for 100 kills in the Full Kakuja and 7 brinks lived through in a kakuja. Track it in the journal (U).
- **The trigger.** Once the quest is done, your next death is cancelled and the Dragon erupts instead. It is a colossal, uncontrollable centipede, about 200 blocks long. The Dragon happens once per character, and never on a fall into the void.
- **The rampage.** For exactly 60 seconds it crawls along the ground, head first, and ploughs through hills, trees and houses. The terrain repairs itself afterwards.
- **How it attacks.**
  - It hunts every mob, NPC and player within reach, biting with fanged heads and grabbing, slamming and lashing with its tentacles.
  - Whatever lies under its body is crushed.
  - Where its body crosses itself, it climbs over.
- **The end.** A cinematic camera follows the whole rampage. Then the Dragon cracks, collapses and shatters with a shockwave, and Kaneki stands back up in his own body. The final evolution is now unlocked; its Peace and War forms come in a later update.
- **No control while it rampages.** No kagune, eye-mode or path ability fires. The server refuses all of them, and the client swallows the keys.
- **Commands (operators):** `/kagune awakening status|complete|trigger|reset <player>`. `complete` finishes the quest.

**Also in this release (since 7.0.1)**
- **Ghoul NPCs** roam at night. They have tempers, kagune, dialogue, favours, friends and allies, and betrayal.
- **The Furuta ability rework** gives every stage four new abilities: two tails, centipedes, four thorns, the mass of eyes and the final kakuja. The fifteen old abilities are gone.
- **The Furuta PvP pass:** Furuta blows on players do 45 % damage, and chained stuns on players break and give immunity.

Not yet tested in game: the Dragon on 7.x. It is covered by its own tests: a full simulated rampage on several terrains, the mesh budget, packet precision, terrain repair and the quest rules. The quest gate is checked by the quest-book tests.
