## 4.1.98 (HexGhoul): the gang leaders are made in their places even when the world remembers a lost body of theirs

HexKagune 4.1.98 for Forge 1.20.1. Not tested in game. Protocol unchanged (127).

You said (quoted exactly): "same issue PRESISTS , 5 times ive asks for this to be patched, CCG general, Gas mask leader, Circus Leader ARE
NOT SPAWNING" (with a screenshot of Banjou's empty bedroom).

**What was wrong:** the world keeps one record per official (who her body is, where her home is, whether she is away). Once you had met
Banjou (or Roma, or the General) and that body was later lost (left in ground nobody loads, an older version's, gone with its chunk), the
record still said "alive, at home". Two rules then kept him away for good:
- His place's own body, made with the building, was taken for a stray when it first woke up (the record's home was in the same place, and
  you had met him), and sent away at once.
- The check that makes a missing official at their spot (every 10 seconds while you're in their place) skipped him if that lost body had
  last been "away" (following you, left waiting): "away" was never cleared, because that body never ticked again.

**Now:** a remembered body only holds his place if it is really there (loaded and alive). If it isn't, the body in his building is him,
and if there is none, he is made at his spot within about 30 seconds of you standing in his place (Banjou's bedroom, the middle of Roma's
ring, the General's office at the top of the CCG tower).

**If one is still missing,** the game's log (logs/latest.log) now says why: every time an official is made at their spot, every body of
theirs sent away and the reason, and every death with where and of what. A dead official comes back to their spot a day after (that rule is
unchanged), and `/kagune places` lists each official's state.
