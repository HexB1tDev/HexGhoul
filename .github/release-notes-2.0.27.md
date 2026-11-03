## 2.0.27 (HexGhoul): wild ghouls' kagune always drawn

HexKagune 2.0.27 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

Wild ghouls' kagune no longer depends on their body's draw call being noticed: Rinkaku tails and Ukaku wings are drawn for every living wild ghoul with its kagune out within 48 blocks, and one ghoul failing to draw never hides the others.

- The owner's report: wild ghouls (likely Rinkaku) used their moves but their kagune never showed.
- Found by reading and an out-of-game harness: the server sends the kagune out correctly, and the tails' geometry builds cleanly for every tier and every lash (600 frames with flips and the grow-out, no throws, no NaN, full alpha). The one fragile link was that a ghoul's kagune was only drawn if its body's render layer had noted it that frame, and one failure aborted every ghoul's tails, the children's tentacles and every Graft player after it for the whole frame.
- Now: a ghoul is noted at the start of its body's draw; the tails' and wings' passes also take every living ghoul within 48 blocks whose kagune is out; each ghoul and each Graft player is drawn on its own, a failure logged ("HexKagune's a wild ghoul's tails layer failed ...") and skipped.
- If the tails are still invisible on your device, the log line above names the cause: please send it. Protocol stays 76.
