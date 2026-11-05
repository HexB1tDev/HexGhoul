## 2.4.7 (HexGhoul): villagers' favours: rare mini quests, more village talk

HexKagune 2.4.7 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

Village humans now ask for favours: rare mini quests that come up through the alert banner, raise their regard and pay out. More village talk too.

- **How a favour comes**: now and then, by day, a villager near you that can see you comes up and asks. A banner shows who and what ("Hana needs a favour: bring 6 coal. (3 emeralds)"), and they say it in their temper's words. It's rare: each villager thinks of it about once every 8 minutes, you're asked at most once every 10 minutes, you only ever have one favour open, and it lapses after an hour of game time. Villagers you've hurt never ask.
- **Three kinds**:
  - **Fetch**: an everyday thing, a little of it (wheat, eggs, coal, iron, logs, wool, string, apples, paper, flowers, torches, a bucket of milk and so on). Hold it in your hand and hand it over on their talk page.
  - **Hunt**: deal with 3 to 5 monsters. Any monster you kill counts, and the action bar keeps the tally.
  - **Visit**: go and see a neighbour of theirs, by name. Talking to that neighbour is enough, and they have a word for whoever sent you.
- **Handing it in**: their talk page offers "It's done." (or the fetched thing). You get 2 to 8 emeralds, something from their trade, +10 regard, and thanks in their temper's words. You can also give it up ("I can't do that favour after all.") without any harm done.
- **More lines**: every temper has its own words for asking each kind, thanking you, and being let down (about 170 lines). The neighbours you visit have their own lines too, and the village has 12 more bits of news and 10 more of gossip.
- **Code**: `HumanErrand` (pure, tested in `NpcTests.errands247`), `HumanErrandServer`; asked from `HumanBrain.village`, handed in on the `HumanTalk` page (`ACCEPT`, `DECLINE`). The favour is kept with the player (`HexErrand`). Protocol stays 85. Not tested in game.
