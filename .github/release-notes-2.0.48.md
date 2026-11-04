## 2.0.48 (HexGhoul): the White Reaper's five-ball volley

HexKagune 2.0.48 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

The White Reaper can fire a five-ball volley from Dawn. All five lightning balls leave its point together, open out into a ring around the line he aimed along (1.6 blocks across, 1.0 up and down, one ball at the top and the rest evenly round), then close in on the spot he aimed at from every side at once: a pincer. Standing still gets you caught, while a good step aside slips it. Past that spot they cross and spread again. Each ball carries 40% of a single shot, so all five on one target is twice a single ball. From 7 blocks or more, half his shots are the volley.

- `quinque/DawnVolley` (pure, tested in `QuinqueTests`): the path every ball follows. The server flies and hits along it (`CrystalShots.volley`), and every client draws it from the same numbers, so nothing more is sent per tick. The ring is flattened up and down so the lower balls don't hit the ground from chest height.
- New FX kind `S2CUkakuFx.DAWN_RING` (46): each ball's start, its way, the aimed-at distance, and its place in the ring. Protocol 79.
- Preview of the flight paths (from behind him, and from the side): `docs/handoff/dawn-volley-2.0.48.png` (new `tools/VolleyPreview.java`). Players' Dawn still fires a single ball.
