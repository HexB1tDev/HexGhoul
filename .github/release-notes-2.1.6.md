## 2.1.6 (HexGhoul): Rinkaku tails no longer swing across on their way back

HexKagune 2.1.6 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

Fix: Rinkaku tails no longer swing across the back after an attack or when the kagune comes out. Each tail's rest spot was on the opposite side from where the original's own motion carries that tail. So every time a tail eased back into rest (after a blow, when transforming), it crossed over to the other side, which looked like the tails rotating or swapping. Once it settled the picture looked right, because the rest shape is symmetric. The kagune's model, shape and rest position are unchanged. Each tail now rests on its own side.

- **The cause**: the original lays a tail out along its owner's left, times the layout's fan side (`TendrilChain.initialize`, read from the original jar). With no fan side, it puts an even tail on the player's right. `RinkakuRoot.side` read the sign the other way round. That rule decides the rest pose (2.0.51), the root sunk in the lower back (2.0.38) and the block's guard pose (2.0.42). The side `StrikePaths` hands the body clearance had the same mix-up.
- **The fix**: one side rule, matching the original, used by all four. A tail whose layout gives no side takes it from its own place among the limbs, as the original does.
- **Tests**: `ControlTests.rinkakuAlive` now checks the rule against the original's and that every tail rests on the side the original lays it out on. Not tested in game. Protocol stays 80.
