## 2.3.1 (HexGhoul): the mood kisses rendered; possessive, giddy and tender reworked

HexKagune 2.3.1 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

The mood kisses (2.2.4) are rendered for the first time, and three of them are reworked after seeing the renders. Possessive and tender looked almost the same (both arms straight out). Giddy's "hands clasped at the chest" just looked like arms hanging, because a mob's arms don't bend at the elbow.

- **Possessive**: both arms thrown up round your neck at once, high and crossed behind it, on her toes, pulling you down.
- **Giddy**: bouncing on her toes with her arms flapping at her sides, head bobbing.
- **Tender**: both hands brought slowly up to your cheeks, close together, her head tipped, a gentle sway.
- The other kisses are unchanged: angry (a fist in your collar, looking away, foot tapping), desperate (arms round you), sleepy (arms hanging, head lolling), hungry (head tipped to bite, a hand at your jaw), teasing (hands behind her back, bobbing), sweet (as always).
- **Previews**, rendered off-game through the game's own transforms on each companion's skin:
  - `docs/handoff/eto-mood-kisses-2.3.1.png`, `touka-mood-kisses-2.3.1.png` and `ken-mood-kisses-2.3.1.png`: every kiss waiting (three-quarter and side), held (blush) and pulled in (front and side).
  - `docs/handoff/mood-kisses-waiting-2.3.1.gif`: all nine waiting, moving.
  - Made by `tools/CompanionPreview.java <out> kisses`.
- **Code**: `MoodKissPose`. Protocol stays 84. Not tested in game.
