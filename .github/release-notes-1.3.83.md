## 1.3.83 (HexGhoul): animation pass: six broken poses fixed (testing)
HexKagune 1.3.83 for Forge 1.20.1. Install on client and server (**protocol 72**, unchanged). Testing build: compiled, tests pass, **not tried in game**.

You asked: "Check all animations make sure all of them are, PERFECT and I mean it".

What I checked: every move a villager makes (wave, bow, giggle, laugh, cheer, hands on hips, shrug, think, whisper, point, pat, crossed arms, hair, look, shy, pout, huff, tap, yawn, stretch, hum, dance, twirl, peace, hop, chin, awe, flinch, kiss, kissed, peck, hug, hand in hand), in each of the three styles (Eto's, Ken's, Touka's), on real villager skins. I also checked all 46 of Eto's gestures on Eto. I rendered them off-game with the game's own pose transforms on a mob's elbow-less arms, which is how NPCs are drawn (sheet: `docs/handoff/villager-moves-2.0.png`).

What was wrong, and is fixed (these poses are shared, so Eto, Touka and Ken are fixed too):
- **Giggle:** meant as "hands over her mouth", it held both arms straight out ahead like a sleepwalker. The arms were turned the wrong way. Now one hand covers the mouth and the other lies across the tummy, shoulders shaking.
- **Laugh:** a stiff T-pose (arms flung straight out). Now head back, hands on the belly, shaking.
- **Lean** ("chin on her hand"), **yawn** ("a hand to her mouth"), **lick** ("a finger at her lips") and the ring proposal's **"hands over her mouth"**: each held an arm straight out sideways (the same wrong turn). Now the hand reaches the face.

Everything else in those sets reads right on the previews. Honest limits: I can't see the game itself, and "perfect" is something only you can confirm in it. The other animation families (the kagune, War and Peace, the Owl, fights, the children's moments) were checked on their own previews when they were made and weren't redone here.
