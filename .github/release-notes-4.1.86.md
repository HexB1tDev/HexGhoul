## 4.1.86 (HexGhoul): quests per player: another player's quest target says whose it is

HexKagune 4.1.86 for Forge 1.20.1. Not tested in game. Protocol unchanged (126).

You said (quoted exactly): "quests per player. thats the main thing".

**What was checked, every quest system, for anything shared between players or credited to the wrong one:**
- The joining trials (Aogiri's patrol and ambush, the Gas Masks' gate, Roma's ring, the CCG hunt, Anteiku's raid and shift): each player's
  trial is their own; every body in it is made fresh for that player (never one already in the world, so two players never share or take
  each other's), and is known as theirs. This is the one place that was shared before 4.1.82: their bodies glowed for every player and
  anyone could kill them, which counted for (or ended) the other player's trial. Since 4.1.82 only their player sees their mark, no other
  player's blow lands on them, and they go for no other player.
- The daily board's jobs, the gangs' kill counts and reputation, the villagers' favours, the courtship chapters (Banjou's suppers are
  counted per player too), dates, presents and the daily wish, the quest log's kill counts: all kept on each player, and a kill counts only
  for the player who made it.

**New:** hitting another player's quest target (or right-clicking one of its ghouls, which would open their talk) now tells you so on your
action bar: "That one is Steve's quest, not yours." (their name if they're about, else "another player's"), at most every two seconds, and
no talk opens. Before, the blow simply did nothing, with no reason given.

### Other

- Tests: `CompanionTests.notYours486`; 9 rules sabotaged one at a time (`docs/handoff/sabotage-4.1.86.json`).
