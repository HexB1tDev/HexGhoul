## 2.2.5 (HexGhoul): a male player's chest a little smaller

HexKagune 2.2.5 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

A male player's chest is a little smaller. The upper chest no longer stands out as far: it's broader than the waist, as before, but only 60% as far out past the torso as the White Reaper's, across the shoulders, in front and behind. The arms hang a little closer in to match. The White Reaper keeps his build exactly as it was.

- Widest across the chest: 4.41 px a side, down from 4.68 (the torso is 4). Out in front of the belly: 0.63 px, down from 1.05. The back: 0.21 px past the torso, down from 0.35. The waist is unchanged, so the taper (the V) stays.
- Arms 0.45 px further out than a bare torso, down from 0.75.
- The Rinkaku mark on a man's back follows the slimmer back.
- **Code**:
  - `ReaperBuild.PLAYER` (the Reaper's outline at `PLAYER_SHARE` 0.6 past the torso) and `PLAYER_ARM_OUT`.
  - `ReaperBuild.chest` and `back` take an outline.
  - Used by `BuildLayer`, `RinkakuMarkLayer` and `KaguneBodyAnimation`. The Reaper's `AgentRenderer`/`AgentModel` keep `PROFILE` and `ARM_OUT`.
  - Tested in `AgentTests.reaperBuild`.
- Preview, rendered off-game: `docs/handoff/player-build-2.2.5.png` (player before, player now, the Reaper; front, side, three-quarter). Made with `tools/ReaperPreview.java <skin> <out> player`. Protocol stays 83. Not tested in game.
