## 1.3.43 (HexGhoul): children inherit their parents' kagune, part 1: Rinkaku tails and Ukaku wings, colour from one parent (testing)
HexKagune 1.3.43 for Forge 1.20.1. Install on client and server (**protocol 68**: a child's data changed; client and server must both be 1.3.43). Testing build: compiled, tests pass, **not tried in game**.

- **Kids inherit their parents' kagune.** It's decided the night the child is conceived, along with its looks. Each parent passes on what they have: a player their path (Rinkaku tails or the Ukaku pair; a ghoul who hasn't chosen yet passes on Rinkaku), Touka her Ukaku wings, Ken his Rinkaku tails, Eto her Owl kagune. A human parent passes on nothing.
  - **Same kind from both parents:** the child has it whole (two Ukaku parents give the pair of wings).
  - **Two different kinds:** the child has both, with the wings down to one. **A Rinkaku dad and an Ukaku mom (Touka) give two tails and one wing**, with the moves of both. The wing is always on the same side for a given child.
  - **One parent with none** (a human player): the child has the other parent's kagune whole.
- **Colour from one parent.** The child's kagune takes either the player's own kagune colour or the companion's (Touka's and Ken's crimson, Eto's pale flesh), 50/50.
- **How they fight.** A taught ghoul child or teen fights with its kagune instead of its fists, like a wild ghoul's but at its size:
  - **Tails:** the transplant's lash string (rising lash, backhand, twin skewer, gutting upper, guillotine), two tails from the start ("both tentacles"), a teen's longer.
  - **Wing(s):** fire shots from the tip of the wing it aims, in short volleys. With one wing every shot comes from that side; with the pair they take turns.
  - **Both:** the tails up close, the wing from further off, and a step in between.
  - Its blows land only on what it fights and on monsters around it, never on a player, a companion or another child. A lash is lighter than a grown ghoul's. The kagune goes back in when the fight ends.
- **Old saves.** Children born before this version have their kagune worked out from their parents the first time a player parent is online, then kept. `/kagune children` now shows what each child inherited and its colour.
- **Not in this part:** Eto's child doesn't use her Owl kagune or transform at low health yet. That's the next update (1.3.44). Until then, Eto's child with a Rinkaku or Ukaku father uses the father's kagune, and with a human father it fights with its fists as before.

Untested in game: compiled and unit tested only (the drawing was checked offline with `tools/GraftPreview.java <dir> child`). Try: with a Rinkaku player, have a child with Touka, age it to a child or teen (`/kagune family grow`, as an operator), teach it to fight, and let a zombie come at the house.
