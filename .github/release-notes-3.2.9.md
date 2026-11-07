## 3.2.9 (HexGhoul): Fix: the Wing Vortex: thinner, round, straight forward, and you can see past it

HexKagune 3.2.9 for Forge 1.20.1. Not tested in game.

The owner's bug report (Ukaku, Z): "Basically the vortex is meant to be less janky, thinner and more circular, can't see anything in first person and glitches a ton instead of going straight forward sometimes it stays in place, its really really strange."

**What caused it.**

- **Janky and fat:** since 2.4.6 it was seven broad, flat ribbons of near-black flame rolled into big upright loops 3 blocks tall around its line, with a solid cone filling them. Up close the flat ribbons read as a heap of crumpled panels.
- **Not straight:** that rolling line was drawn along your flat facing, ignoring where the vortex was really going up or down. So when it dipped, the drawing and the real thing went separate ways.
- **"Stays in place":** the server aimed it from your shoulder at the middle of the body under your crosshair, or at the ground where your crosshair landed. A mob near you and a bit below sent it **down into the ground just past it**. It ended there, a few blocks off you, and unwound where it stopped: it looked stuck.
- **Can't see in first person:** your own vortex was 3 blocks wide a couple of blocks from your eyes. On top of that, the launch set off a burst of flame, a pink flash and a ring 2.6 blocks wide **at your shoulder**, and it shed fire and big smoke puffs along its whole length, **including right at your eyes**.

**The fix.**

- **Thinner and round:** a clean corkscrew straight along its flight line. Three round tubes of the wing's fire are wound around it in circles (a loop every 4.5 blocks), with a thin dark jet of flame down its middle. Its spiral is at most 0.8 blocks out (was 1.5, rolling in 1.5-block loops). It's narrow where it leaves your back, rounded at its point, still spinning, still dark maroon with burning pink edges.
- **Straight forward:** it flies along your look, only a little up or down with it (at most about 13° down, 17° up). A body just off your crosshair (within 18°) turns it **level** toward that body; it is never dived at the body's middle or at the ground. Its catch (2.1 to 2.6 blocks around its line) still hits what's in front of you.
- **First person:**
  - your own vortex is a thin, see-through jet near your eyes, opening and coming in solid only as it goes off ahead;
  - the launch burst goes off two blocks ahead, smaller, with no flash or ring at your shoulder;
  - no fire or smoke is shed within two blocks of your eyes.
- Everyone else sees the same thin, round corkscrew from the outside.

- Preview: `docs/handoff/ukaku-vortex-3.2.9.png` (the bottom row is your own from your eyes).
- No network changes (protocol 92). No new Minecraft API.
- Tests (UkakuTests):
  - it's thin and round (strands on one circle round its straight line, a thin jet down it, nothing in between);
  - it's narrower at your back;
  - in first person nothing of it comes near your eyes and near them it is a thin jet;
  - it flies forward, only a little up or down, whatever you look at;
  - a body just off your look turns it level, and one well off to the side doesn't.

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 92. Not tested in game.
