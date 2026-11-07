## 3.1.7 (HexGhoul): Rubble Hurl tails go round the body, never through it

HexKagune 3.1.7 for Forge 1.20.1. Not tested in game.

- **The bug (the owner, with the 3.1.6 preview: "the tails are going through the body, bro"):** the two tails gripping the Rubble Hurl's chunk were bent straight from the lower back to the chunk, a line that runs through the torso and the head. The game's body clearance pushed them back out afterwards, but only as a correction, and the 3.1.6 preview didn't even include that pass.
- **Now each gripping tail is routed round him (`RinkakuHurl.grip`):** out of his back, swung well behind it and round its own side, up behind its own shoulder beside his head, then on to the chunk. For a chunk lower down (torn from the ground before him) it leaves lower, at his waist, wide of his arm. For a grip across his body (the chunk on his other side) the reach bows out ahead of him, past his face instead of across it. In his own first person the tails come in low and wide from the sides, out of his eyes.
- The same routing carries the tails as they whip after a thrown chunk. Body clearance still runs after it, as for every tail.
- **Checked, not just looked at:** a new regression check (ControlTests `hurlGrip`) builds every grip the hurl draws: rising from the ground before him, to either side, further, lower and higher; held at five charges; whipping after the throw; all four tails; seen by others and in first person (4,400 paths). It requires the ribbon, at its real breadth along its length with 30% extra for its swell, never to meet his torso, arms or head once it has left his back. 4,401 new checks.
- Preview, now drawn with the body clearance the game applies: `docs/handoff/rinkaku-hurl-3.1.7.png` (straight from behind, from the side and in first person). Off-game geometry only.
- No network or API changes. Protocol 91.

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 91. Not tested in game.
