## 2.0.43 (HexGhoul): the White Reaper at ease

HexKagune 2.0.43 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

The White Reaper has his own idle when he stands around with his case shut, in place of the regular investigators' one. He leans well back from the hips with his hip cocked and his weight sunk on the leg that holds the case. His other leg is loose, out in front and turned out. His chest is turned open, his head cocked with his chin a touch up so he looks down at you, and his free hand is in his coat pocket. It's a pose: only a slow breath, and no glancing about.

- `AgentPose.reaperIdle` (tested in `AgentTests`), used by `AgentClient` for the Reaper's look. The pocket arm was solved through the game's own transforms (`tools/ArmSolve`'s method).
- Previewed off-game in his skin with the new `tools/ReaperPreview.java`, next to the regular idle: `docs/handoff/reaper-idle-2.0.43.png`. Protocol stays 78.
