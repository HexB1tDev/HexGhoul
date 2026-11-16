## 2.3.3 (HexGhoul): the male chest stays in every form

HexKagune 2.3.3 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

Fix: a male player's chest vanished whenever he transformed. The build was switched off in every form (Ukaku's kakuja, Graft's hide, War, Peace and the Dragon's turning), so the chest dropped back to the plain torso. It now stays in every form, and each form's torso armour is widened to fit over it instead: about 10% wider and 28% deeper, centred on the torso, so it covers the chest and the jacket layer.

- The chest still hides under a chestplate and when you're invisible. Female players were never affected (their figure stays in forms).
- **Code**: `BuildLayer.built` and `fit`; `ReaperBuild.PLAYER_FIT_X` and `_Z` (tested in `AgentTests`). Used by `KakujaArmorLayer`, `GraftArmorLayer` (the torso) and `KingLayer` (the torso). Protocol stays 84. Not tested in game.
