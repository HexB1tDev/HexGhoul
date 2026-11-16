## 2.0.47 (HexGhoul): Dawn bigger and smoother

HexKagune 2.0.47 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

Dawn, the White Reaper's lance, is bigger and smoother. It's drawn about 20% bigger in the hand (scale 0.74 to 0.9). Its spike is half as thick again all along and meets the cone guard without a step; before, it read as a needle beside the guard. The spike and grip are 16-sided (were 8) in finer steps. Same silhouette as before: a black lance with a flared, gold-banded guard, a gun-sword style grip with its hooked knuckle guard, and a spiked pommel.

- `tools/quinque_models.py`: `DAWN_SPIKE` and `DAWN_TAPER` thickened, a new `rod16`, finer spike steps, Dawn's hand scale 0.9 and first-person scale 0.66. The generator re-solved the hold, so `QuinqueHolds` DAWN is regenerated. Only Dawn's frames changed (595 elements, was 487). The open firing frame still parts about its light.
- The lightning now leaves from the bigger lance's point: a new `Quinque.DAWN_TIP` (2.1 blocks from the hand), used by both the player's shot and the Reaper's (they had 1.9 and 1.72).
- Previews: `docs/handoff/dawn-bigger-2.0.47.png` (model, before and after) and `docs/handoff/dawn-aim-2.0.47.png` (in hand while aiming, three views). Protocol stays 78.
