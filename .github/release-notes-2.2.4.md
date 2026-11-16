## 2.2.4 (HexGhoul): a kiss for every mood

HexKagune 2.2.4 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

A kiss for every mood. It works the same as before: they lean in and wait, a tap of Shift is a quick kiss, holding makes them blush, and holding longer makes them pull you in. Now each mood gives its own kiss: how they wait, how long, what they say, how they move, and what they do after. A kiss is never turned down any more. Ask an angry partner for one and you get an angry one.

- **Angry** (sulking): grabs your collar with one fist, the other clenched, looking away and tapping a foot ("Well? Hurry up."). Waits only half as long as usual. After, they shove you back with arms crossed ("There. Happy? I'm still annoyed. ...Less annoyed."). **The kiss ends the sulk.** Worth fewer hearts.
- **Possessive** (jealous): pulls you down at once with both arms round your neck, gripping tight ("Mine. Got it? Mine."). Clings to your arm after. **The kiss ends the jealousy.**
- **Giddy**: hands clasped at the chest, bouncing on their toes, head bobbing. After, a burst of laughter ("Again! No... again!").
- **Tender** (affectionate or clingy): both hands up to your face, slowly, swaying. After, forehead to forehead ("Stay like this. Just a minute."). Worth more hearts.
- **Desperate** (lonely): throws their arms round you with their whole weight, trembling a little. Waits longer, and won't let go after (a hug).
- **Sleepy**: arms hanging, head lolling, drooping in and catching themselves. Waits the longest. After, their head on your shoulder.
- **Hungry** (bloodthirsty): head tipped as if to bite, a hand at your jaw, the other on the hip. After, a nip at your lip and licking their lips ("You taste like dinner.").
- **Teasing** (bored): hands behind the back, bobbing in and out of reach. **Dodges your first try**, laughing ("Too slow! Try again."), then leans back in for a real one, then hops away.
- **Sweet** (happy and anything else): exactly the kiss as it was.
- Each companion (Eto, Touka, Ken) has their own lines for every kind: 2 for each, and which one you get also depends on whether it was quick, held or pulled.
- Hugs are still turned down when they're sulking or jealous. Their own asks for a kiss still follow their mood (2.2.3: mad or jealous, they don't come asking).
- **Code**:
  - `npc/MoodKiss` (pure): styles, waits, the dodge, after gestures, hearts and lines.
  - `client/animation/MoodKissPose` (pure): the body for the wait, the blush and the pull.
  - `EtoBrain`'s kiss machine picks the style from the mood when the kiss begins.
  - `S2CEto` carries it: **protocol "83"**, so install on both client and server.
  - Tested in `CompanionTests.moodKisses`. Not tested in game.
