## 2.0.50 (HexGhoul): Rinkaku scales, leaf shape and more glow

HexKagune 2.0.50 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

The Rinkaku tails are reworked as asked: realistic scales, thin where they leave the body, and more glowing veins.

- **Scales**: the thin black-and-white chevrons are replaced by overlapping keeled scales, as on a reptile. They're staggered row to row with each scale's rounded free edge toward the tip, lying over the base of the next. Each scale is darker toward its base where it tucks under, with a raised keel down its middle, a lit rim along its free edge, a thin dark outline and the shadow its edge casts on the scale beyond. The scales are four across a face, large enough to read a few blocks off. They're in greys over the tail's own colour, so every kagune colour keeps its own scales (a faint light stripe still runs along each face). The new `tools/rinkaku_scales.py` regenerates `rinkaku_scales.png`; it tiles seamlessly.
- **Shape** (the owner's drawing): a quarter of its width where it leaves the body, widening steadily to its broadest 78% of the way along, then a short rounded end (`RinkakuRibbon.profile`, `BROADEST`, `PEAK`, `ROOT_WIDTH`). Before, it swelled just past the back and tapered over its second half.
- **Veins and glow**: 6 main veins (was 4), each forked and half of them forked twice, plus a glowing spine down the middle of each broad face: 17 glowing lines a tail, about double. Wider halos, brighter cores, a wide faint aura round the main veins and spines, a stronger RC pulse, and a hotter root glowing further out.
- Tests updated in `ControlTests` (the leaf profile: thin root, widening to its broadest near the far end, rounded end; more veins; brighter cores).
- Previews: `docs/handoff/kagune-showcase-2.0.50.png` (on a body, every colour, a close-up of the scales and veins, at night) and `docs/handoff/rinkaku-scales-2.0.50.png` (the scale tile in crimson, stretched as it lies on the tail). Protocol stays 80.
