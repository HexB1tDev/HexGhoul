## 1.3.68 (HexGhoul): wild ghouls prowl and hunt humans at night, and hide what they are by day (testing)
HexKagune 1.3.68 for Forge 1.20.1. Install on client and server (**protocol 72**: the ghoul NPC packet now says whether its eyes are on). Testing build: compiled, tests pass, **not tried in game**.

You asked: "Make sure the wild ghouls hold up, as in manifest their Kagunes properly + hunt humans at night (Kagune and Kakugan off during the day)."

- **At night** a wild ghoul (one not following a player) prowls with its **kakugan glowing**, even before it fights. It **hunts humans**: any human it sees within 20 blocks is prey. It brings its **kagune out** (the tearing-out animation and sound) as it goes for them, and puts it away a while after the fight, as before.
- **By day** a wild ghoul **hides what it is**: kagune in, kakugan off. It starts nothing: no hunting humans, no going for players it dislikes, no picking fights with the CCG. If someone attacks it, it **runs** instead of manifesting.
- **Humans now run only from what shows**, as you described for the village: anyone with their **eyes on** or their **kagune out** (players and ghouls alike; War, Peace and the final kakujas from much further, as in 1.3.65). A ghoul hiding everything is just a person to them now (before, they "felt" a hidden ghoul player within 6 blocks). So by day a wild ghoul can walk among them; at night, with its eyes on, they flee.
- Ghouls that follow you are unchanged, day and night.

Judgment call: "Kagune and Kakugan off during the day" is read as wild ghouls not fighting at all by day (they run if attacked), since fighting without a kagune isn't possible for them. Tell me if you want them to fight back by day instead.

Untested in game: compiled and unit tested. Try: a wild ghoul at night near a human (`/summon hexkagune:human`), then the same by day; attack a wild ghoul by day.
