## 3.6.7 (HexGhoul): Bikaku: Potential Unlock, first part (three times the energy, the bar purple)

HexKagune 3.6.7 for Forge 1.20.1. Not tested in game.

Your words: "Upon unlocking Potential Unlock, the player's energy bar permanently changes color, visually representing the mutation's activation.", "The maximum energy capacity is also permanently multiplied by 3. This means the player can store three times as much energy as before unlocking the mutation.", "The increased maximum energy capacity must persist after death, relogging, and server restarts.", then "Should be Purple, the bar, my bad".

- **Potential Unlock, the second Bikaku mutation, is in the game (its first part).** It is unlocked by the NPC's quest when that comes. Until then, operators: `/kagune bikaku mutation potential_unlock on|off`.
- **Three times the energy.** Everything you do spends a third as much of the bar. Eating fills the bar as fast as before, so a meal gives three times as much energy. The menu reads it three times over (for example "192 / 300").
- **The one bar (energy and food) turns purple**, and so does the menu's bar. There is no RC bar.
- **Kept for good:** the mutation is saved in your profile, so it stays through death, relogging and restarts. `/kagune reset` clears it.
- **Bikaku path only.**
- **The menu's bar is now labelled "ENERGY"** (it said "RC"), its line reads "One bar: energy, healing and food. Eat to fill it", and the kakuja's price reads "25 energy". Your words: "RC bar IS NOT A THING".
- **Coming next (its second and third parts):** surviving lethal blows while energy lasts, rare tearing-off of body parts, and the strands in your kagune colour pulling each part back. A preview comes first.
- **My guesses:**
  - The shade of purple (0x8A2BE2).
  - Three times the energy done as a third of every price, rather than a longer bar.
  - Bikaku only.
  - Healing off the bar takes the same share as before; only what moves and the kagune cost is a third.
  - Only the Bikaku menu's labels changed; the Rinkaku and Graft menus still say "RC" over their bar.
- Previews: `docs/handoff/potential-bar-3.6.7.png` (the bar as it is and with Potential Unlock: full, hungry, low), `docs/handoff/bikaku-menu-potential-3.6.7.png` (the menu with both mutations on).
- Network: protocol 103 (unchanged).
- Tests: BikakuTests `mutations()` (three times the energy with it, once without; the colour is purple; each red of the bar turns purple, its alpha kept). The full suite.

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 103. Not tested in game.
