## 2.5.1 (HexGhoul): The finger crack flows: eased up and down, never a snap; a cleaner crack sound

HexKagune 2.5.1 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

The finger crack is now one flowing movement: the arm eases up over almost a second, the thumb presses into the finger, the finger cracks (a small flick of the hand, the head tipping with it), a moment held, then the arm eases back down to rest. On a player it starts and ends exactly where their body already was, instead of jumping to the pose. The crack sound is replaced with a cleaner, shorter knuckle-style crack.

- `EtoPose.crack` (companions) and `EtoPose.crackOver` (players, blended over their stance): `crackWeight` eases in by tick 18, holds, eases out from 34 to 52; the press from 16 to 22; the crack at tick 22 (`EtoGesture.CRACK_SNAP`, shared with the sound on both sides). Before, the pose was held at full and then cut off, and on a player it overwrote the arm outright. `EtoGesture.CRACK` is 56 ticks now; the kagune bursts out 4 ticks after the crack (`KenPullOut.OUT_AT`).
- `EtoTests`: never more than a sliver of a jump between quarter ticks; starts and ends at rest; on a player, starts and ends where the body was. Previews: `docs/handoff/ken-crack-2.5.1.png`, `docs/handoff/player-crack-2.5.1.png`.
- `sounds/sfx/bone/finger_crack.ogg`: three short pops (a knuckle cracking, 0.17 s) cut from the same CC BY 4.0 pack, softly band-limited (brightness centre about 3.9 kHz, against 6.7 kHz for the old cut, which was more than half hiss and crunch above 6 kHz), and a little quieter. Credited in `CREDITS.txt`.
