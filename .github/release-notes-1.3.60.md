## 1.3.60 (HexGhoul): two fixes to family talks, found reviewing 1.3.53 to 1.3.59 (testing)
HexKagune 1.3.60 for Forge 1.20.1. Install on client and server (**protocol 70**, unchanged). Testing build: compiled, tests pass, **not tried in game**.

Found by reading back this session's own code (nothing seen in game):

- **No more standing alone waiting for nobody.** When a child set off to talk to its brother or sister but couldn't get there (a wall, a fence, no path), its lines were dropped after 12 seconds, yet the one it was coming to kept standing still, turned toward it, for another 12 seconds before the talk gave up. Now a talk ends at once when there's nobody left in it to talk to (and nobody says anything about it).
- **A companion following you is never pulled away to break up a squabble.** Only a parent who is free (not following anyone, not fighting, not fetching a child, not on a hand, a kiss or a talk with you) comes over.

Untested in game: compiled and unit tested only.
