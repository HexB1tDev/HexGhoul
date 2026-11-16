## 4.1.71 (HexGhoul): the CCG, joined through the General

HexKagune 4.1.71 for Forge 1.20.1. Not tested in game. Protocol unchanged (125).

Your words (quoted exactly): "CCG Faction: / Join through the general (quest) and now daily quests as well. / Traits: / CCG no longer
attacks the player. / All ghouls but official NPCS are hostile towards the player. / Damage increased when fighting ghouls. / Looks:  Cyan
Blue with a brief cafe symbol." Your answer on the symbol: "A briefcase (Recommended)".

- **Joining, through the General:** ask him on his first page ("I want to join the CCG."). He asks why, three answers; only the
  bureau's own reason gets you further ("To protect people who can't protect themselves."). A ghoul may join too: he says flatly that
  the bureau has a division for that. Then 5 ghouls killed (any ghoul but the CCG's own division), counted over your hotbar. Then he
  swears you in and you're issued a quinque (a bikaku blade, in its case). A member gets "I want out of the CCG." on his page.
- **Daily orders from the General:** "Orders for today, General?" on his page, one a day: kill 4 to 7 ghouls, kill 2 to 4 of Aogiri, the
  Clowns or the Gas Masks, or bring him 8 to 16 sheets of paper for the reports (in your hand). Paid in the bureau's things (iron,
  emeralds, bread, gold, now and then a diamond or a golden apple) and reputation, as the other gangs' dailies.
- **The CCG no longer attacks you:** no investigator goes for a member, even struck or after seeing your kagune (it's one of theirs);
  hitting one costs reputation instead, killing them counts as killing your own (more than 10 and you're banned: then they come for you
  on sight). The CCG's ghoul division is the CCG too: it never goes for you, and it defends you.
- **Every ghoul but the named ones is hostile:** every ghoul NPC, wild (by day too), any gang's, Aogiri's, goes for a member. The
  official NPCs (Eto, Touka, Ken, Ayato, Noro, Roma, Banjou...) don't, and the division doesn't.
- **More damage to ghouls:** a member's blows land 1.3 times as hard on any ghoul: ghoul NPCs, ghoul players, the named ghouls (not
  the General, a human; not the division).
- **The name:** a quinque case, a space, your name in cyan blue (a colour of its own), in chat and over your head. The reputation meter
  is that blue too.

### Choices of mine you may want changed

- The General is now a gang leader, so 4.1.67's rules are his too: marrying him while in another gang takes you out of it, and a General
  married to someone in another gang asks them to quit it.
- The CCG tower is the CCG's "hideout": at 0 reputation there you're told to leave, then turned on, like the other gangs.
- Killing ghouls earns a member reputation; the issued quinque replaces the mask the gangs hand out.

### Other

- Renders: `docs/handoff/ccg-general-talk-4.1.71.png` (the General's pages), `docs/handoff/ccg-emblem-4.1.71.png` (the case beside
  the other emblems, in a name tag and a chat line).
- Tests: `NpcTests.ccg` (the chain step by step, the name and its colour, the division the CCG's, where the investigators' and the
  ghouls' choices sit, the damage, the hideout, the issued case, the General's dailies and pay); each rule sabotaged fails.
- Next: the hotel replacing the Small City.
