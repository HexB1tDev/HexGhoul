## 2.1.0 (HexGhoul): kakuja legs and mask in the kagune's colours

HexKagune 2.1.0 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

A Rinkaku kakuja's centipede legs and mask now take on the kagune's colour, blending with it while staying distinct. Before, both were a fixed plum. The centipede's segments are always greys, black and white: charcoal plates with pale grey and white rims, or bone-white plates for a kagune too dark to stand apart from them. They carry only a breath of the kagune's hue. The glowing flesh between the plates is the kagune's own colour. The mask has the same grey shell leaning a little more toward the kagune's colour, with the kagune's dark in its edges. The red kakugan lens stays. From this version on, the last number never reaches 10 (after x.y.9 comes x.(y+1).0), the owner's rule.

- New `KakujaPalette` (pure, tested in `ControlTests.kakujaPalette`): the plates (always greys, measured by chroma), their lit rims, the claws, and the flesh (`KaguneTint.recolour` of the original red flesh; a black kagune's hottest flesh glows blood red, as its veins do).
- `CentipedeTail.colours` and `CentipedeMask.colours` take a palette. They're set from each player's kagune colour where the legs are drawn (`CentipedeRenderer`), where the mask is drawn (`RinkakuMaskLayer`), and when either breaks apart (`KakujaFx.shatter`).
- Previews in six kagune colours (crimson, blue, purple, green, black, white): `docs/handoff/kakuja-legs-colours-2.1.0.png` and `docs/handoff/kakuja-mask-colours-2.1.0.png` (`tools/ArtPreview.java` takes `-Dkagune=RRGGBB`). Protocol stays 80.
