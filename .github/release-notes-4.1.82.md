## 4.1.82 (HexGhoul): your bug list: the control bar gone, Roma kept in her tent, the ghoul division at its tower, a trial's bodies its player's alone, Anteiku's regulars at the counter

HexKagune 4.1.82 for Forge 1.20.1. Not tested in game. Protocol unchanged (126).

Your reports after 4.1.81, quoted exactly, and what changed for each:

- **"wtf is the bar above energy"** (asked "removed for good, or kept but made readable?", you answered "Yes"; read as removed): it was 1.0.43's
  kagune-control line (crimson to bone-white by how much you control your kagune). It is gone, and its client setting
  (`controlIndicator`) with it. If you meant "readable", say so and it comes back.
- **"Yes, and the clown girl is not in the circus tent, either that or multiple of the tents are spawning one of the 2, fix it, now"**: the
  world still builds one tent (its one-of-a-kind claim holds). What went wrong: once anyone has met Roma, only one body of hers lives, and
  the first one met wins. A Roma met anywhere else (made by an egg or `/kagune roma here`, or one that wandered off and got lost) made the
  tent's own Roma vanish the moment the tent loaded. Now:
  - A body of hers in her own tent is her, whatever body the world had elsewhere (unless she is married to someone: then she lives where
    they made their home). The other one is sent away.
  - If you're in the tent for half a minute and she isn't there (and she isn't out with someone, or married), she is made in the middle of
    the ring, where the tent's template puts her, and any stray body of hers elsewhere leaves.
  - If she dies, she comes back a day later in her tent, not where she fell.
- **"also where tf is the human ghoul division from the ccg?"**: they only ever turned up by chance (a third of the CCG's squads that come
  for a ghoul, about one every three days of play near someone), dressed like any investigator. Now a squad of three of the division is
  always posted at the CCG tower: when you come within 96 blocks of it and none of its squad is there, three are made on the ground round it
  (32 to 44 blocks from its middle), and they keep to their post (one that strays 20 blocks walks back). At most one new squad every five
  minutes, so one wiped out isn't back at once. The chance squads still come as before. Their name tags say "CCG Ghoul Division".
- **"BUG: Quests are PER player,  currently i'm 90% sure Im taking another player's quest."**: a joining trial's bodies (the patrol, the
  ambush, the ghoul you hunt and your partner, the café's raid and its regulars) are now its player's alone:
  - The ones you're to find, and the regular waiting for a cup, are marked by a column of green sparks over their heads that only that
    player sees (they glowed before, and every player saw the glow).
  - No other player's blow lands on them, and they go for no other player.
- **"ANother BUG: In the give coffee mod at anteikou clients are spawning in areas that are not infront of the reception. Clients should be
  PER player, only highlight the ones that are OF THE PLAYER DOING THE QUEST."**: the regulars now stand in front of the counter, in the
  row nearest it, the way Touka faces from behind it (picked from 2 to 6 blocks in front of her, 3 to either side, nearest rows first, on
  her floor, under the roof). Before, they came anywhere 3 to 7 blocks round her, behind the counter and in the side rooms, and, failing
  that, even outdoors. Only if Touka isn't there to say which way she faces do they come anywhere on the café's floor round her spot; never
  outside any more. The highlight is yours alone (above).
- **"giving items to NPCS like coffee should be right clicking them, not dropping it at them, bugs are raining out of the sky bro, giving
  coffee to people at anteikou is buggy sometimes they dont even accept it"**: the cup is served by a right click: on the regular, as
  before, or anywhere within 4 blocks of the one waiting, aimed at them or not (you don't drink it instead). The cup can be in either
  hand (they refused one held in the off hand).

### Still open (next)

- **"Gas mask guy is ALSO not spawning in his room, or anywhere"** and **"and I'm 99% sure the general of the CCg isnt spawning either"**:
  this version brings back only Roma (her spot is her tent's middle, so it can be found in any world). Banjou and the General need their
  spots kept as their places are built; that's the next version. The likeliest cause is the same as Roma's: `/kagune <name> here` makes a
  body where you stand and makes it theirs; their home is there from then on, and the body in their own building is sent away when it
  loads.

### Other

- Render: `docs/handoff/cafe-4.1.82.png` (Anteiku's floor from above, from its template: Touka, the counter, the six seats in front of it,
  and the old ring for comparison; `tools/cafe_seats.py`).
- Tests: `CompanionTests.bugs482`; 71 rules sabotaged one at a time (`docs/handoff/sabotage-4.1.82.json`), all caught.
