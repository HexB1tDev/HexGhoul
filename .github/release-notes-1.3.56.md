## 1.3.56 (HexGhoul): Eto's child moves its body when it fights with her flesh (testing)
HexKagune 1.3.56 for Forge 1.20.1. Install on client and server (**protocol 69**, unchanged). Testing build: compiled, tests pass, **not tried in game**.

Found by reading the code (listed in the last handoff): since 1.3.50 Eto's child fights with her flesh (the rake, the grab and throw, the club) and her Mutated Arm, and those were drawn, but the child's own body stood still while it raked or hammered.

- **Its body now moves with each move**, posed by the very same animator Eto's body and a Pale player's use: it winds up and twists into the rake, reaches out and hauls back for the grab and the throw, raises and brings down the club, eases out of each move into a guard and back, and carries the Mutated Arm's weight when that is out. Never the Owl.
- **Her flesh comes out of its body as posed that frame**, so the flesh's root follows its arm and shoulder instead of the pose the move alone would have given.

Judgment call and approximation: the poses are exactly Eto's (already previewed as `tools/pale-preview.sh <dir> flesh`), applied to the child's model (a player's proportions at its size: 0.7 for a child, 0.88 for a teen). A toddler never fights, so its little-one proportions are never posed this way.

Untested in game: compiled and verified only. Try: Eto's child (a child or a teen that has been taught to fight) against a zombie.
