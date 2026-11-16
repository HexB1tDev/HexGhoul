## 4.1.84 (HexGhoul): romance: presents each companion loves or hates

HexKagune 4.1.84 for Forge 1.20.1. Not tested in game. Protocol unchanged (126).

Your to-do's final step (quoted exactly): "Add to the to-do list that the final step is to better the quests, to include everything in the
mod + minecraft, daily  and romance quest betterment, so it's more fun, less boring, same for the joining gang quests." The plan shown to you
(you said "whatever you think is best") had, for romance, gifts each companion loves or hates. Until now a companion took only their own
few gifts (their errand's item, their flower, Ghoul Meat, a letter).

- **Anything in your main hand can be a present now.** With something in hand that has no gift of its own, their gifts page offers "Here.
  I want you to have this. (the clock in your hand)", and their first page shows "I brought you something..." to get there.
- **Each of them loves some things, hates others, and takes the rest politely**, in their own words, naming what you gave:
  - Every ghoul hates human food (bread, cake, cooked meat, fish, fruit...): "I chewed that for a whole lunch break once, smiling, so
    nobody would notice. Never again. Take it back." (Touka). The General, a human, eats it gladly and hates ghoul food and ghoul masks.
  - Eto loves ink, a book and quill, feathers, candles, an Anteiku Blend, her own mask, the Owl's Omelette; hates quinques and Arima's mask.
  - Touka loves an Anteiku Blend, a Blood Latte, a rabbit hide, her mask, velvet cake, a book and quill (her studies); hates quinques and
    Amon's mask.
  - Ken loves an Anteiku Blend, a book and quill, a lantern, crimson tea, his eyepatch mask; hates spider eyes, centipede soba, shears and
    Yamori's mask ("A thousand minus seven...").
  - Ayato loves black dye, swords, his mask, RC tonic, a kakuja shot; hates every flower but the wither rose, pink dye, and his sister's mask.
  - Noro loves what a ghoul eats (cooked ghoul meat, kakuhou tartare, rotten flesh) and his mask.
  - The General loves a book and quill, a clock, bread, cooked beef, a shield, a quinque.
  - Roma loves firework stars, slime balls, jack o'lanterns, purple, magenta and pink dye, records, the Clowns' masks; hates paper and books
    ("Homework?! Boooring!").
  - Banjou loves anything that grows (every flower and sapling), bone meal, flower pots, the Gas Masks' mask; hates the wither rose ("That
    kills everything round it.") and TNT.
- **Loved:** taken, as much regard as their own gifts (6), their delighted gesture. **Taken politely:** 2. **Hated:** pushed back (you
  keep it), 4 regard lost, and they show it. One present a day, as every gift; a second gets their "tomorrow" in their words.

### Choices of mine you may want changed

- Only the main hand's item is offered (their own gifts still come from either hand).
- A hated present counts as the day's gift (so it can't be tried again and again).
- What each loves and hates is my reading of them; any of it can be changed.

### Other

- Render: `docs/handoff/gifts-4.1.84.png` (each companion's gifts page with a present, then a loved one and a hated one, on the game's own
  talk screen; `tools/UiPreview.java <dir> gifts`).
- Tests: `CompanionTests.gifts484`; 33 rules sabotaged one at a time (`docs/handoff/sabotage-4.1.84.json`).
- Found on the way: the lines first said "A ink sac!" and "A Anteiku Blend"; they take "an" now, and a test holds it.
