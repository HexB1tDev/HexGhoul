## 2.0.41 (HexGhoul): Rinkaku tails' living material

HexKagune 2.0.41 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

Rinkaku tails now have a living material at the level of Ukaku's wings: glowing veins running through the flesh with a pulse of RC flowing from the body to the point, a glossy rim toward the silhouette, a wet glint where the light catches them, a slightly clear skin seen edge-on (the veins on the far side show faintly through), a hotter, glowing root where they leave the back and darker points. All in the player's kagune colour.

- Shading is worked out per vertex from the camera, as Ukaku's fire is. The veins (four per tail, each with a fork) wander along the surface and are drawn at full brightness, so they glow in the dark. A bright band travels along them from root to point.
- The chevrons and stripe fade where the tail is seen edge-on instead of shimmering at its silhouette.
- Colours: every part follows the kagune colour (blue tails glow blue). A black kagune's veins glow blood red, as its wings' glow does.
- Pure rules in `RinkakuRibbon` (`surface`, `markAlpha`, `veins`; tested). Previewed off-game with the upgraded `tools/RibbonPreview.java` (now shading by an eye, blending the clear skin, drawing the veins unlit): `docs/handoff/rinkaku-material-2.0.41.png` (six colours) and `rinkaku-material-zoom-2.0.41.png`. Protocol stays 77.
