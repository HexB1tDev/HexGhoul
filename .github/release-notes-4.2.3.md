## 4.2.3 (HexGhoul): the General, Roma and Banjou were there all along, hidden from your game; now they're drawn

HexKagune 4.2.3 for Forge 1.20.1. Not tested in game. Protocol unchanged (129): a 4.2.2 server takes 4.2.3 players and the other way round.

You said (quoted exactly): "Bro, I'm at the exact location says Banjo is at. He's not here. The NPCs, for the last fucking time, 7 times I
told you to fix this, and 7 times you delivered nothing but garbage. This is already an existing world that I'm not erasing. And once again,
the CCG, Banjo, and the circus girl, they're not spawning. Fix this once and for all. I'm so tired of your shit."

"I'm sure, and I didn't even check, but I'm sure that the fucking general from the CCG and the circus girl are not spawning either. This is
bullshit, bro."

**What was wrong:** your screenshot showed it. The server had Banjou alive at x 57, y 73, z 750, "in his place", right where you stood, and
your game didn't show him. They were spawning. Your game was hiding them.

Back in 1.x your crash reports showed your setup sometimes ends up without the mod's renderers (the code that draws its NPCs); a game
without one crashes the moment the NPC comes into view. Since then a guard rebuilds every renderer of the mod when that happens, and if one
is still missing it keeps that NPC out of your game instead of crashing. The General (added in 4.1.5), Roma (4.1.49) and Banjou (4.1.54)
were never added to that guard's list. So in your game the rebuild made nothing for them, and the guard hid them every time. Ken, Touka, Eto,
Ayato and Noro were on the list, which is why you see them.

**What's fixed:** all three are on the list, and so are the village humans. A new test reads every kind of NPC the mod registers and fails
the build if any is ever left off again (it names general, roma, banjou and human against the old code).

**Your existing world:** nothing to erase or redo. Banjou is already in his bedroom on the server; with this jar your game draws him. The
General and Roma are made the same way as Banjou: go to the CCG tower and to Roma's circus, and each one is made at their spot within half a
minute (`/kagune places` says when). If one is still not drawn, your `logs/latest.log` will have a line with "came into this client's world
with no renderer": send me that line.
