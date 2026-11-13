## 4.1.78 (HexGhoul): the daily board: three jobs a day to choose from, eight per gang, Ken's dailies for Anteiku, better pay

HexKagune 4.1.78 for Forge 1.20.1. Not tested in game. Protocol unchanged (126).

Your to-do, its final step (quoted exactly): "Add to the to-do list that the final step is to better the quests, to include everything in the
mod + minecraft, daily  and romance quest betterment, so it's more fun, less boring, same for the joining gang quests." Asked what first,
you said: "whatever you think is best". The dailies first, as they are what you play every day; the gangs' joining and the romance next.

- **Pick one of three.** Ask your gang's giver for work and they lay out three jobs for today; you choose one. A different three tomorrow.
  Only jobs you can actually do are offered (a ghoul's eyes-only job is never offered to a human; a watch only once the world has the place).
- **Eight jobs per gang, of different kinds** (40 in all, the old 13 among them, a job already taken kept as it was):
  - **Aogiri (Noro):** investigators at night; investigators with only your eyes, kagune hidden; watch the CCG building at night; bring him
    a quinque case (any); ender pearls; and the old ones.
  - **The Clowns (Roma):** put on a show (kills in a row); kills in mid-air; fireworks; a cake; Gas Masks at night; and the old ones.
  - **The Gas Masks (Banjou):** keep the night off the den (monsters at night); stand guard at the den at night; white wool; bread;
    Clowns at night; and the old ones.
  - **Anteiku (Ken), new: it had no dailies:** brew Anteiku Blend for the regulars; Ghoul Meat for the ones who won't kill; keep the ward
    safe at night; stop a feral ghoul; drive off investigators sniffing round; watch over Anteiku at night; coffee beans; monsters with only
    your eyes.
  - **The CCG (the General):** night patrol (ghouls at night); put down a feral ghoul; stake out Aogiri's hideout at night; Ghoul Meat for
    the quinque lab; keep the ward safe at night; and the old ones.
- **How they count:** a kill counts by what it was and how (at night, eyes only, in mid-air, a feral ghoul...); a show by your best run of
  kills in a row; a watch by the seconds you spend near the place at night (it tells you to come back at night if you're early); things
  brought, in your hand, as before.
- **Better pay:** the gang's things as before (Ken's: the café's blend and lattes, Ghoul Meat, emeralds, a golden apple, coffee beans),
  plus, for a ghoul, 30 to 50 mastery and now and then (one in twenty) a mutation point (a Rinkaku's or Bikaku's as mastery, as quests pay
  it). Days in a row count: every third day a third thing paid, every seventh a mutation point. Reputation as before.
- Each giver says it their own way (Noro terse, Roma theatrical, Banjou shy, the General formal, Ken warm).

### Choices of mine you may want changed

- One job a day (pick one of three); not all three.
- Watches last 60 to 90 seconds, within 40 blocks of the place's leader (Ayato for the hideout, the General, Banjou, Ken).
- Jobs with a named target spawned for them (a marked investigator, an escort) are kept for a later version, done carefully.

### Other

- Render: `docs/handoff/daily-board-4.1.78.png` (each giver's board, the game's own talk screen, and Ken taking one).
- Tests: `NpcTests.board` (eight jobs and three kinds per giver; three different offered a day, the same all day, every job coming up;
  only what the player can do; the old jobs keep their saved places and count what they did; kills by their tags; a show's best run; a
  watch's seconds; "a cake", "a feral ghoul", whole tens of seconds; streaks; the pay; every giver's voice); 14 rules sabotaged one at a
  time, each caught.
