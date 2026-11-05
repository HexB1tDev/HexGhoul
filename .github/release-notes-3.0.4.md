## 3.0.4 (HexGhoul): Eto is in the library you find

HexKagune 3.0.4 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

Eto is in the library you find. A world has three libraries (placed in a ring, like strongholds), but only one Eto, and whichever library loaded first claimed her, often one a player only passed within range of without ever seeing it. The library they then found removed its own Eto on her first tick. Not tested in game.

- **Until someone has met her** (talked to her) and while she has no partner, a library's Eto no longer removes herself. She waits where she was made, and the body a player actually comes upon (within 24 blocks) becomes her. The unseen one elsewhere is sent away the next time its ground loads.
- **Once she has been met,** she stays the one they met, as before. "Met" is now kept with the world (`UniqueNpc.met`, set the first time anyone talks to her). For a world saved before this, it counts as met if any player present has met her, or if she has a partner.
- **The same rule covers every companion slot** (Touka, Ken).
- **Your current world:** if you haven't talked to Eto yet, the library you're at gets her the next time you're there.
