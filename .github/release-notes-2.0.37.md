## 2.0.37 (HexGhoul): Rinkaku's guard removed

HexKagune 2.0.37 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

Rinkaku's C move, the tendril guard, is gone: C does nothing on the Rinkaku path any more and the guard is no longer listed in the ability guide. Rinkaku's defence becomes its right click (the block, in a coming version).

- The original's `toggleGuardLegacy` was the only thing that ever raised the guard; it is never called now, and a guard somehow still up is let down when C is pressed. C still works for the paths that use it (Ukaku's final kakuja Steel Blade, Graft's own C). Protocol stays 77.
