## 3.7.3 (HexGhoul): Bikaku: Potential Unlock, torn apart and pulled back together

HexKagune 3.7.3 for Forge 1.20.1. Not tested in game.

Your words (the whole request is in `docs/handoff/owner-words-mutations.md`), among them: "While the player still has energy remaining, they gain the ability to survive devastating injuries that would ordinarily cause death", "dismemberment must NOT happen frequently", "Every detached limb takes approximately 5 seconds to completely reattach", "All regenerative strands and biological tissue must inherit the player's currently selected kagune color". On its preview: "Continue Im still at 96".

- **A blow that would kill you doesn't**, while your bar has energy. You're left at one heart, it costs 20 energy (a third of that with Potential Unlock's triple energy), and it tears a part off you. Without energy, the death takes. Never while Grand Devouring holds you, nor out of the world or by /kill.
- **A single blow of half your full health or more** that doesn't kill you also tears a part off. Ordinary blows never do.
- **What comes off:** an arm at the shoulder, a leg at the hip, or now and then the head at the neck. The head is taken by about one in five blows that would have killed you and one in twenty heavy ones. Arms are twice as likely as legs, and the side the blow came from more often. A part already off is never torn again, and every part has its own timeline, so several can come back at once.
- **About 5 seconds a part:**
  - It's thrown the way the blow went, tumbling, bounces once and lies where it fell, in your own skin with its torn end raw. If you move on, it stays where it fell.
  - Strands of your kagune, in your kagune colour, come out of the raw wound on you, stretch and twist to it each on its own, and wrap its torn end.
  - They tighten and haul it up off the ground, back through the air and upright.
  - Tissue grows between the two. It's in place at 4.4 s; the flesh closes and the strands sink into you.
  - Whole at 5 s, and only then a small heal (one heart).
- **Energy:** the strands draw energy while they reach and haul. With none, the part waits where it is until there is.
- **Off your model:** a part that's off isn't drawn on you until it's back, nor its hand in first person. You keep fighting with what you have left; your worms are unaffected.
- **With the head off,** your camera stays where it is.
- **Everyone sees it:** every player who can see you (within 96 blocks) sees the part fly, the strands and the haul.
- **Sounds:** the pack's own recordings (`sfx.gush_big` and `sfx.crunch` as it tears, `sfx.writhe` as they take hold, `sfx.crunch` as it goes back on).
- **My guesses:**
  - The numbers: one heart left; 20 energy to survive; 0.15 a tick to pull; half your health for a heavy blow; how likely the head is; the timeline (strands out at 0.7 s, hold at 2 s, haul from 2.4 s, in place at 4.4 s, whole at 5 s); a one-heart heal.
  - The throw: how far, the spin, one bounce.
- **Known limits:**
  - No torso pieces yet (your words: "pieces of the torso can be torn apart"); only head, arms and legs come off.
  - Armour on a part that's off (a helmet), and an item held in a hand that's off, may still be drawn where it would be in third person.
  - The wound is placed on your standing body, not following a crouch or a pose.
  - The animations still move the hidden part (unseen).
- Preview: `docs/handoff/potential-reattach-preview.png` (sent before building; the same motion).
- Network: protocol 104 (new: S2CPotential, message 56).
- Tests: BikakuWormTests `potential()`:
  - a part already off is never torn again, and none is taken when all are off;
  - the head now and then, mostly from blows that would have killed;
  - the side the blow came from is taken more often;
  - energy is drawn only while the strands reach and haul;
  - every part, both skins: never in the ground; thrown off; lying still before the haul; hauled ever nearer; back exactly in place; off you until then;
  - the strands come out of the wound and take hold of its torn end;
  - a part lies where it fell while you move on, and comes back to where you are;
  - the server, network and drawing wiring (read from the sources).
  - Also the full suite.

Validation: pushed at the owner's request ("please push it directly I dont have time or usage to wait for your local build") before the full python3 build.py finished; it then completed with EXIT 0 on the same sources (javac --release 17, the regression suites, API/bytecode and archive checks, dedicated-server loading checks). Protocol 104. Not tested in game.
