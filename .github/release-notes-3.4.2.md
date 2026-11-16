## 3.4.2 (HexGhoul): Welcome home

HexKagune 3.4.2 for Forge 1.20.1. Not tested in game.

From your list after 3.2.1, the last of it: **kids greeting you after days away.** Before, a child greeted you with a short line by age only, after a minute or more apart. And it forgot how long it had been whenever it was unloaded, which is exactly what happens while you're far away for days. So coming home after days away usually got nothing.

**Now each child remembers when it last saw you.** The date is kept with your save, so it survives the child being unloaded. Two or more in-game days away, each of your children greets you by its temper and age when you come near, then runs in for its hug as before. **A week or more**, and they have more to say:

- **Sunny:** "I drew you like six pictures while you were gone!" / after a week: "I counted! Then I lost count! Then I started again!"
- **Shy:** holds on without a word / after a week: "...I thought maybe you forgot us."
- **Bold:** "I kept everything running while you were gone." / after a week: "That was nine days. That's a LOT of days. ...Don't do that. Okay?"
- **Curious:** "Where did you go? Did you bring anything back?" / after a week: forty-one questions in a notebook.
- **Mischievous:** "Nothing happened while you were gone. Don't check the chest."
- **Gentle:** "Are you hungry? Sit down, I'll get you something." / after a week: "I kept a candle lit every night."
- **Serious:** "You were away four days. Everything here is in order." (They count the days.)
- **Toddlers:** "I growed while you gone!" **Babies:** a long careful look, then the biggest smile.

They also count as having missed you (their "missed" mood and its scenes, as before).

- **Approximated:** "days" are in-game days (24,000 ticks of play, about 20 minutes each). Two days, and a week for the longer greeting, were my picks. A save from before this version has no record yet, so the first time you come back after updating there's no greeting.
- No network change (protocol 93). No new Minecraft API.
- Tests (FamilyTests): two days or more is a greeting; the record is kept under a name your save accepts; every temper and age has more than one greeting, and more after a week; a serious child names the number of days.

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 93. Not tested in game.
