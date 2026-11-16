## 3.2.8 (HexGhoul): Fix: War's Spire Wave was a mess in first person

HexKagune 3.2.8 for Forge 1.20.1. Not tested in game.

The owner's bug report (War): the move that "charges centipedes from the left arm and shoots them forwards in a beam of sorts of centipedes" (the Spire Wave, B) is janky in first person.

**What caused it.**

- In first person, the game never draws your left arm (vanilla draws an empty off hand as nothing).
- So the centipedes winding round your arm while B is held were drawn round the **third-person body's** arm: just under and in front of your eyes, swinging around with the body's lagging turn, not with your view.
- On letting go, the mass **bent back to that hand** for its first 3 blocks, and its eight centipedes (up to 0.44 blocks thick each) started bunched up less than a block from your eyes: a wall of centipede heads across the view for the first moments.

**The fix (your own first-person view only; everyone else sees it as before):**

- **Your left arm is drawn** where vanilla puts a left hand: your real arm in your skin, War's sleeve and shoulder spikes on it. It reaches forward a little as the charge builds, then thrusts out with the beam, holds through the crush, and draws back as the centipedes come home. Whatever your off hand holds is hidden for the move.
- **The charge's centipedes coil round that arm**, half as thick as third person draws them, hugging closer, their heads gathering just past your fist. They never reach the middle of the view.
- **Let go, the beam comes straight out of your fist**: a cone that widens as it goes away from you, its centipedes thinner close up. Nothing of it comes within 0.6 blocks of your eyes. It still goes where it went (the server's line is unchanged), and does what it did.

- Preview: `docs/handoff/war-spire-first-person-3.2.8.png` (top row 3.2.7, bottom row now; the arm in a stand-in skin colour).
- No network changes (protocol 92). One new Minecraft API: `PlayerRenderer.renderLeftHand` (to draw your arm), allowlisted with its SRG evidence.
- Tests (KingTests):
  - the arm is in view, lower left, in front of you at every charge and thrust;
  - the coils stay off your eyes and out of the middle of the view;
  - the beam leaves your fist as a cone and never comes within 0.6 blocks of your eyes;
  - third person is drawn exactly as before;
  - the arm's thrust follows the beam out and back.

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 92. Not tested in game.
