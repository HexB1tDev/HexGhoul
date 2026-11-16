## 3.3.2 (HexGhoul): Dawn has an idle

HexKagune 3.3.2 for Forge 1.20.1. Not tested in game.

From the owner's list (after the kiss bug): Dawn's hold, ambient and M1s. Asked this session, the owner picked: **add an idle/ambient** (the hold and the three M1s stay as they are).

**What it does.** Stand still with Dawn out for a second and a half (not walking, swinging or charging its lightning ball), and a slow loop starts. It's relaxed: he knows he doesn't need to be ready.

- **Breathing:** the lance rises and falls a little with him.
- **A turn in the fingers:** Dawn rolled to and fro in his hand, its point circling and lifting a little, then still again.
- **Carried aside at ease:** a while later he brings it up and out to his right, the point raised about 40 degrees. His left hand goes to his hip, he leans back a touch with his head tilted, and the point taps lazily up and down for a few seconds. Then it comes back down into the stance.
- The loop is 13 seconds. Moving, swinging or aiming stops it at once.
- Everyone sees it on you, as on any player holding Dawn.

**Why not "resting on the shoulder".** Arms are stiff in game (no elbows), and Dawn's angle in the hand is fixed by its model. Resting its three blocks on your shoulder would put your hand over your head (it looked like holding up a torch), and a full twirl swept it down through the ground. Both were tried in the preview and dropped.

- Preview: `docs/handoff/dawn-idle-3.3.2.png` (front, side, back; the loop from left to right).
- No network changes (protocol 92). No new Minecraft API.
- Tests (QuinqueTests):
  - it starts in the stance;
  - its whole length stays above the ground and never goes through your head, all the way round the loop;
  - the turn in the fingers stays a small circle (5 to 45 degrees);
  - it goes up aside and is back in the stance before the loop ends;
  - the other quinques keep their stance.

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 92. Not tested in game.
