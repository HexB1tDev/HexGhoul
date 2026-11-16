## 2.3.8 (HexGhoul): a child can always be sent home

HexKagune 2.3.8 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

Fix: there was no button to send a child home. "Go home." only showed while the child was out away from home, never while it followed you around nearby. Now it's on the page whenever the child can walk.

- A child with a family home walks back there. One without a family home walks back to where it belongs (where it was born or first put down). Either way it stops following you and isn't left carried.
- **Code**: `ChildPage` (the option), `ChildBrain.goHome` (the fallback). Protocol stays 84. Not tested in game.
