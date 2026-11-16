## 2.0.30 (HexGhoul): Dawn smoothed

HexKagune 2.0.30 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

Dawn is round where it was square: its guard is a 16-sided cone in fine steps (it was an 8-sided one in 12 big steps), and its spike, grip and butt are 8-sided along smooth profiles, with the spike's two ridges turning about it along its thicker middle.

- Same length, grip and holds as before (the hold solver's output is unchanged), same black and gold. 487 boxes (171 before).
- New frame `dawn_fire`: the point split in two and parted around a yellow light, for when a lightning ball leaves it. Not shown yet: the next versions choose it as a shot is fired. 22.5 degrees each way is the smallest turn an item model allows, so only the last 6 pixels of the point part.
- Previewed off-game with the new `tools/model_preview.py` (draws a model JSON from any side, with zoom); before and after: `docs/handoff/dawn-smooth-2.0.30.png`. Protocol stays 76.
