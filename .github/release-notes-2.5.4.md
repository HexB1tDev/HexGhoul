## 2.5.4 (HexGhoul): Children fight monsters that come close in their sight; ghoul children's kakugan at night

HexKagune 2.5.4 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

Your sons and daughters stand their ground now: any child old enough to be taught (child, teen or grown) goes for a monster that comes close and that it can see, taught or not (lessons still make its blows harder); babies and toddlers still run to a parent. And at night a ghoul child's eyes turn to kakugan (the eye or eyes it was born with), not only while it fights.

- `ChildRules.fights`: any stage that learns, whatever its skill (an untaught teen hits for 3, a child 1.5, more for a ghoul and with each lesson). `ChildRules.spots`: within 8 blocks and in its line of sight (`hasLineOfSight`), never one behind a wall; a monster after the child, a parent or a sibling is still fought wherever it is about.
- `ChildClient.eyesOut`: kakugan while it fights and all night (`NpcRules.night`, read from the client's own world clock: nothing new over the network); used for the painted skin and the glowing eye layer in `ChildRenderer`. A human child has no kakugan; a sleeping one's eyes stay shut.
