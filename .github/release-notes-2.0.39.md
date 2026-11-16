## 2.0.39 (HexGhoul): Rinkaku tails as in the reference

HexKagune 2.0.39 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

Rinkaku tails are drawn as the owner's reference picture has them: broad, flat ribbons that swell a little past the back and taper to a point, bright at the middle of each face and deep at the edges with darker smudges along them, a light stripe running along each face near one edge, and thin chevrons all over them, black and light, pointing back toward the body. Every colour follows the player's own kagune colour.

- Colours: the picture's crimson palette is turned to the tail's kagune colour (`KaguneTint`, as the wings and shards are), so a blue kagune gets a blue ribbon with pale blue stripe and marks, and the original crimson matches the picture exactly. A black kagune is black with smoke-grey stripe and marks. The chevrons' black stays black.
- How: the ribbon is drawn into the original's own buffer (per-vertex colours), and the markings (a generated tile, `tools/rinkaku_scales.py` -> `textures/misc/rinkaku_scales.png`) a hair outside it after every tail is drawn. The original's tube is only used if this ever fails. Ghoul children's Rinkaku tentacles get the same look in their own colour. Centipede legs are unchanged.
- Pure shape and colours in `RinkakuRibbon` (tested: the crimson palette exact, blue stays blue, black is black with visible marks, edges deeper than the middle, the taper to a point, no NaN). Previewed off-game with a software renderer over the real mesh and texture in six colours (`tools/RibbonPreview.java`, `docs/handoff/rinkaku-ribbon-2.0.39.png`); the test tails there are short, synthetic paths. Protocol stays 77.
