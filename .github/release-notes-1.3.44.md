## 1.3.44 (HexGhoul): fix: a Rinkaku child has one real Rinkaku tentacle, not two graft tails (testing)
HexKagune 1.3.44 for Forge 1.20.1. Install on client and server (**protocol 68**, unchanged). Testing build: compiled, tests pass, **not tried in game**.

- **One tentacle, not two.** A child who inherits Rinkaku now has **one** tentacle (with its one Ukaku wing if the other parent is Ukaku). 1.3.43 misread "both tentacles and one wing" and gave two.
- **The real Rinkaku tentacle, not the old graft tails.** 1.3.43 drew the child's tails with the dormant Graft DNA tails the wild ghouls use (toothed, eyed, segmented). The child's tentacle is now drawn by the original Rinkaku renderer, the same tube a Rinkaku player's tentacle uses: its taper, surface, veins and point.
  - **Colour:** the child's inherited colour, with veins in the same hue, much darker.
  - **Thickness:** a Rinkaku player's first tentacle, at the child's size.
- **At rest** it has a Rinkaku player's first-tentacle curl: out of the lower back, up behind the head and over, swaying a little. Its length at rest matches its length when it lashes, so it never stretches.
- **In a fight** it lashes with one tentacle on the same beats the server hits on (a rising sweep, a thrust, an upper, the overhead slam), at the child's size. Damage and reach are as before for one tentacle.

Untested in game: compiled and unit tested only; the look was checked offline (`tools/ChildKagunePreview.java`, which uses the previews' stand-in for the original tube; the game uses the original's own). Try: a child of a Rinkaku player and Touka, taught to fight, against a zombie near the house.
