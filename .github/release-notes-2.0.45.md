## 2.0.45 (HexGhoul): the White Reaper's build

HexKagune 2.0.45 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

The White Reaper has a heavier build. His upper torso is a little bigger than the part that meets his legs: broader across the shoulders and chest, with the chest standing about a pixel out in front of his belly and a touch fuller at the back, tapering into his normal waist (a V-shape). No spheres or sculpted muscle: it's his own torso drawn again a little larger, in his own skin and coat layer. His arms hang slightly further out to clear it.

- `ReaperBuild` (pure, tested in `AgentTests`): the chest's outline down the torso, each face painted from the torso's own texels (and the jacket layer's), every face turned outward. Drawn by a new layer on the investigator renderer, for the Reaper only, carried by his torso as it is posed (leans, flips, his idle). His arms are set 0.75 px out while he is drawn, then put back, since the model is shared with other investigators. Dawn is one-handed, so its grip is unaffected.
- Previewed off-game in his skin through the game's transforms (`tools/ReaperPreview.java ... build`): before and after, front, sides (with the near arm hidden to show the profile), three-quarters, above: `docs/handoff/reaper-build-2.0.45.png`. Protocol stays 78.
