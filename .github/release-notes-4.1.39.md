## 4.1.39 (HexGhoul): Small City fixes: humans stay in their village, the city's land worked out once, protocol bumped

HexKagune 4.1.39 for Forge 1.20.1. Not tested in game.

Your words: "after that, just look for bugs on your own, like search for any possible bugs. I hear there's a bunch. That could be related to
the city."

**What was wrong, and now:**
- **Humans walking off from vanilla villages for good:** the humans a vanilla villager brings (4.1.36) had no home: every 6 to 14 seconds
  they took a random step in any direction, so over an hour they drifted ~100 blocks off and never came back. They never despawn, and the
  village's newborn villagers then brought more (the "4 within 48 blocks" limit no longer saw the ones that had left), so humans slowly
  piled up across the world. Now each one remembers its village (where its villager stood) and heads back whenever it strays more than 24
  blocks. Humans brought before this update have no village saved and still roam; new ones stay.
- **The city's land measured four times over (or more):** whether a city can stand (flat enough, no sea, no deep water) is about 740 height
  samples of the world's terrain. Each of the city's four quarters did it again, and so did every hideout, CCG building or other place
  nearby checking it isn't inside a city. Same answer every time, so now it's worked out once per city and remembered (less stutter while new
  land generates, a faster `/locate`).
- **Protocol 118:** 4.1.36 changed the mod's registries (the city's structure and placement types added, the old villages' types removed)
  without bumping the network protocol. A 4.1.35 client could try to join a 4.1.36+ server. Now a mismatched client and server are refused
  with the version message.

**Checked and fine:** neighbouring cities can never overlap (their starts are 37+ chunks apart, a city is 34 wide); every quarter stays
within its start's 8-chunk reach; the city's biome tag covers every overworld biome; nothing left points at the deleted villages.

### Other

- Protocol 118.
