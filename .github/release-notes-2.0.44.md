## 2.0.44 (HexGhoul): the Reaper's pocket hand on a straight arm

HexKagune 2.0.44 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

Fix for 2.0.43: the White Reaper's free hand now actually goes into his pocket in game. Investigators are drawn on vanilla's player model, which doesn't bend elbows or knees, but 2.0.43's pocket arm relied on a bent elbow, so in game it would have hung off at an angle. The arm is now solved for a straight arm: angled in to his hip, the hand sunk into the pocket at its front.

- `AgentPose.reaperIdle`: the pocket arm re-solved through the game's transforms with no elbow; the unused knee bend is dropped. `tools/ReaperPreview.java` now renders as an investigator is drawn (no bends), and `docs/handoff/reaper-idle-2.0.43.png` is re-rendered that way. Protocol stays 78.
