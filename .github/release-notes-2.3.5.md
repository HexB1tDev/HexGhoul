## 2.3.5 (HexGhoul): mood kisses: each companion's own, the face kept clear

HexKagune 2.3.5 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

The mood kisses, redone: each companion has their own, and no kiss closes the arms in front of the face any more. Before, every kiss was the same pose on all three, and some brought both arms up crossed or together in front of her face. That's exactly where your face and first-person camera are, so it would have blocked your view.

- **The rule (checked by a test on every pose)**: no arm raised to face height may turn in toward the middle. Hands go to your collar (low, under both faces), your shoulders or waist (out to the sides), round your neck from the side, behind the back, on the hips, or stay down.
- **Angry**:
  - **Eto**: a fist in your collar, the other on her hip, nose in the air, head turned away, stamping her foot and shaking.
  - **Touka**: both fists clenched at her sides, head turned hard away, glaring; held, she grabs your collar.
  - **Ken**: fists at his sides, head bowed and turned away; held, his hands come to your shoulders.
- **Possessive**: Eto is on her toes, one fist in your collar and the other hand round the back of your neck. Touka twists your collar and drags you down. Ken grips your shoulders.
- **Giddy**: Eto bounces with her arms flapping. Touka keeps her hands behind her back, bouncing anyway, looking away and back. Ken has a hand up by his head, grinning.
- **Tender**: Eto puts both hands on your shoulders and sways. Touka puts one hand on your shoulder, head tipped. Ken holds your waist.
- **Desperate**: Eto throws her arms round your middle on her toes. Touka holds a fist in your shirt with an arm round you. Ken puts his arms round your back with his head on your shoulder. All three tremble.
- **Sleepy**: Eto's head lolls side to side. Touka droops onto you holding your sleeve. Ken leans his weight on you with his hands in his pockets.
- **Hungry**: Eto has a hand at your jaw from the side and the other on her hip, head tipped to bite. Touka holds herself back with her fists clenched. Ken holds you at arm's length by the shoulders, shaking.
- **Teasing**: Eto keeps her hands behind her back. Touka has hers on her hips. Ken keeps his at his sides. All three lean in and pull away.
- **Previews**, rendered off-game on each skin through the game's transforms:
  - `docs/handoff/{eto,touka,ken}-mood-kisses-2.3.5.png`: every kiss waiting, held and pulled in, from the front and side.
  - `docs/handoff/{eto,touka,ken}-mood-kisses-2.3.5.gif`: every kiss waiting, moving.
  - 2.3.1's previews are removed.
- **Code**: `MoodKissPose` takes the companion (`Persona.style`), with `MoodKissPose.clear` checking each pose. `CompanionTests` checks every pose of every mood on all three, that the three differ from each other, and that angry turns the face away. Protocol stays 84. Not tested in game.
