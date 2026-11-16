## 2.3.7 (HexGhoul): bubbles steady in bed; children's kagune shown, both parents' halves

HexKagune 2.3.7 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

Fixes: bubbles jumping over NPCs in bed, and children's kagune that couldn't be seen. Children now also have both parents' halves of the kagune.

- **Bubbles over a sleeper**: speech and thought bubbles (and the z's) over someone in bed now sit at a fixed height over the bed. Before, they followed the lying body's tiny box and its settling in the bed, so they bounced up and down.
- **A child's kagune shows whenever it fights**: tails (in the Rinkaku tendrils players have, in its own colour) and wings appear whenever its kakugan is out. Before, they waited on a separate kagune message, and without it the child fought with nothing visible.
- **Both parents' halves** (the owner: "one of each, both parents. If they're both ghouls, you get both halves"):
  - A child of two different kinds always has both: a tail and a wing, Eto's flesh and a tail, and so on. Before, it was 40% the mother's kind, 40% the father's, 20% both.
  - A child of two Rinkaku parents has two tentacles, one over each shoulder, one from each parent.
  - Children already born are brought up to date the next time both parents are known (a companion always, a player while online). Their colour is kept.
  - A half-human child still has its ghoul parent's kagune.
- **Code**: `SpeechBubbles.head`; `KaguneGenes.TWIN` and `tentacles`, `ONE_KIND` set to 0; `Children.both`; the child loops in `GraftRenderer` (a second tentacle) and `UkakuWingRenderer`. Tested in `FamilyTests`. Protocol stays 84. Not tested in game.
