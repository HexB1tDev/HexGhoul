## 2.5.5 (HexGhoul): A child's Rinkaku is drawn whenever it is out

HexKagune 2.5.5 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

A child's Rinkaku (and its wings) is now drawn whenever it is out, whether or not the child's body was drawn the usual way that frame. Before, the tentacle was only drawn for a child its body's renderer had noted during the same frame, so anything that skips or replaces that call (an entity-culling or performance mod, a body drawn some other way) left the kagune invisible even while the child fought with it. This is the fix the wild ghouls' tails got in 2.0.27, which children never had.

- `ChildRenderer.drawn()`: the children noted this frame, plus every living child within 48 blocks with a kagune whose kagune is out (it fights: its kakugan out, or its kagune's own state from the server). Every child kagune pass reads it (tentacles, wings, Eto's child's flesh, the centipede).
- `GraftRenderer` and `UkakuWingRenderer`: each child drawn on its own; one failing is logged ("HexKagune's a child's tentacle layer failed ..." / "a child's wings") and skipped, never hiding the others or the rest of the frame.
- Checked out of game: the server brings the kagune out and tells every client; the tentacle's geometry builds cleanly for a child, a teen and a grown one (2.4 to 3.6 blocks, no NaN) and its mesh is opaque.
- If it is still invisible on your side, the log line above names the cause.
