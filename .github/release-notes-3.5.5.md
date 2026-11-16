## 3.5.5 (HexGhoul): Fix: the Bikaku worm goes round your body, not through it

HexKagune 3.5.5 for Forge 1.20.1. Not tested in game.

Your report: "make sure that the tentacles don't go through a player's body. I noticed that a few are doing that. Just, like, a few moves. Like the predatory drag looks a little strange when you send it and it goes through the player's body instead of going around it."

What was wrong (measured off-game, on the worm as the game draws it): the old code kept the worm's few guide points out of your body, but the curve drawn between them still cut through you. Predatory Drag went into you in about half its frames, by the worm's whole thickness: down through the front of your body and out between your legs. The M1 bites and Devouring Maw did it in some frames (mostly the mouth passing through your head on the way to something close on your left).

- **Every Bikaku worm is now drawn round you.** Where its curve would pass through your head, body, arms or legs, that stretch is swept round your side (the shorter way) and smoothed so it bends round you rather than folding. At rest, and anywhere it doesn't come near you, it is exactly the line it was (your zigzag design is untouched). Measured over every move at 18 places round you: none of it goes into you now (Ravenous Pursuit and Carrion Eruption never did).
- **Predatory Drag takes a new route:** down your right side past your knee, to in front of your right foot, then out along the ground. While it's out, the coils it leaves behind you draw in to your right, so it never cuts across your back. It dips toward the side its target is on (it always dipped in front of you first). It holds its catch up from in front of your right shoulder (it was from behind it, which put it through your head for a catch on your left).
- **Predatory Drag drags its catch in to the side it came from**, and lifts and slams it there. It used to drag everything to the same spot in front of you, so a catch from your side came across you. A catch straight ahead is dragged, lifted and slammed exactly as before.
- Two smaller causes fixed with it: a coil passing your middle no longer jumps from your back to your front (or your left to your right), and when the worm's head is close in front of you, its neck is shortened rather than pushed out of your back.

Preview (off-game, the 3.5.4 build against this one, the same frames): `docs/handoff/bikaku-drag-round-3.5.5.png` (Predatory Drag at something ahead and something front-left, from above and from the front).

- **My guesses:** the size of the body it keeps out of (Minecraft's player shape; arms and legs a little deeper for their swing; the head as a ball, so it holds when you turn your head); 4 cm of room past the worm's skin; taking the shorter way round; the Drag's new spots; dragging a catch to its own side.
- **Known limit:** for something behind you or straight to your left (these moves go after what you're looking at, so only if you turn right round mid-move), the worm can switch which side it goes round you from one frame to the next: a visible snap, never through you.
- **Next:** the kakuja's four moves and its passives; then the range you asked for (held, up to about 30 blocks, round blocks); then the new forms.
- Network: unchanged (protocol 97). No new Minecraft API.
- Tests: BikakuWormTests (105,666: every move drawn as the game draws it stays out of the player's body; drawn round him it moves no more than 0.3 further between quarter ticks than its points do, for bodies in front; the root never moves, the mouth stays on what it bites; at rest the line is unchanged; the Drag's catch never comes through him and goes to its own side, straight ahead as before), BikakuTests.

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 97. Not tested in game.
