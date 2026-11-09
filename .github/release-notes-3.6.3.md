## 3.6.3 (HexGhoul): Bikaku: Grand Devouring

HexKagune 3.6.3 for Forge 1.20.1. Not tested in game.

**Needs GeckoLib now** (4.4 or newer, on both sides), with Photon and PlayerAnimator as before. Your words: "use geckolib it is meant to be a requirement". Nothing draws with GeckoLib yet; the next version will (the view from inside the mouth).

Grand Devouring is the first of the two Bikaku mutations you asked for. Your words are kept whole in `docs/handoff/owner-words-mutations.md`.

- **How it is unlocked:** not by mastery. Your words: "these mutations are NOT unlocked through mastery they're gonna be unlocked through an NPC's quest introduced later today." Until the quest is in, an operator can turn it on or off:
  - `/kagune bikaku mutation grand_devouring on`
  - `/kagune bikaku mutation grand_devouring off`
  - Add a player's name at the end for someone else. The quest will call the same thing on the server.
- **When it starts:** a body within 10 blocks at 1 heart (2 HP) or less, while your kagune is out (not in the kakuja). A free worm is chosen at random and does it on its own.
- **What it does, about 23 seconds in all:**
  - It rises and swells. It keeps growing for 4 seconds, through the rise and while it arches over.
  - It arches over and hangs its mouth open over the body, then comes down over all of it to the ground.
  - The jaws close slowly while the body is drawn up inside (4 s).
  - The gulp: the body dies at the end of it.
  - A bulge the size of the body runs down the whole worm to your back (9 s). It goes into you, and your bar refills.
  - The worm goes back to its own size and place.
- **Its shape, after your three reports on the preview:**
  - "its not meant to be that big at all, never even close": it now leaves your back as thin as a plain worm.
  - "the kagune is literally VERTICAL from the point of origin" and "why is it not arching and progressively increasing size": every pose is now one smooth arch. It leaves your back leaning back and up and curves over into the mouth, as in the 65t frame you picked. It gets bigger steadily along its whole length to the mouth.
  - "the giant, is literally covering the user's body ... it jumps into a massive size  insantly": the giant's worm is thin by you too, grows steadily into its mouth, and grows in over time.
  - It stays swollen until the digesting is over, then goes back to normal (your spec: "Once the digestion animation finishes, the tentacle gradually returns to its normal size").
- **Any size of body:** the mouth, the worm and the bulge scale with what it eats, from a small mob to a 12-tall giant. The body is kept inside the mouth from just after it is caught until it is out of sight.
- **The bulge:** it is squeezed thinner and longer as the worm narrows toward your back, like a snake's prey. It shrinks over its last stretch into you, so your back never swells to the bulge's size.
- **Cooldown:** 1 minute, from when the worm is itself again. Your words: "Forgot to mention but Grand Devour should have a cooldown of 1 minute". `/kagune cooldowns off` and creative skip it.
- **The worm that feeds does nothing else** until it is done:
  - M1's bites go to the other worms. In One Worm, M1 says "Your worm is feeding".
  - Moves use the other worms. In One Worm they wait.
  - The kakuja and form changes are refused while it feeds.
- **The body caught can do nothing, player or NPC:**
  - It is held and drawn up into the mouth. Its movement keys do nothing.
  - Its kagune, kakuja and eyes go off, and an investigator's weapon and a ghoul's blade arm go out of sight at once.
  - Whatever is in its hands is taken out and kept, never deleted. It is given back if it is let go, or just before it dies so it drops as any death would.
  - It can't attack, use items or interact, and nothing else can hurt it.
  - Ghoul NPCs and investigators stand down.
- **Let go** if your kagune goes away, you die, or the body is lost or taken away. Logging out gives back what was taken.
- **Wet:** drool off its mouth as it hangs open, a splash as it comes down, drips while the jaws close, a spray as they shut and at the gulp. These are Photon particles, more of them for a bigger mouth.
- **Sounds:** only the pack's own recordings (roar, bites, grabs, drips, the gulp, the bulge going down).
- **Menu:** a MUTATIONS section. Grand Devouring shows LOCKED, or ON with the seconds left on its cooldown. Potential Unlock shows SOON.

Previews (off-game):
- `docs/handoff/bikaku-devour-3.6.3.png`: the whole of it for a player-sized body, a small mob and a giant.
- `docs/handoff/bikaku-devour-before-after-3.6.3.png`: the frames you circled, before and now.
- `docs/handoff/bikaku-menu-3.6.3.png` and `bikaku-menu-on-3.6.3.png`: the menu.

- **My guesses:**
  - 4 s of growing, about 23 s in all.
  - The cooldown counts from when the worm is itself again, not from when it starts.
  - The bulge squeezed as it nears your back.
  - The bar refilled by 35 plus twice the body's full health.
  - 2 mastery for a feeding.
  - The worm lifts the body a little while it digests.
  - The wet particles.
- **Known limits:**
  - The view from inside the mouth for a player being eaten isn't built yet (next, with GeckoLib).
  - Not in the kakuja yet.
  - Minecraft gives no way to muffle other sounds, so the mouth's own sounds play close and loud.
  - What was taken from a body's hands is kept in memory: a server crash mid-feeding loses it (logging out gives it back).
  - A held player can still use blocks.
- **Found while working (not changed here):** `/kagune cooldowns off` doesn't end the Bikaku cooldowns already running (`BikakuServer.clearCooldowns` is never called). New ones are skipped. Say if you want it fixed.
- Network: protocol 103 (S2CBikakuState carries the mutations, the feeding and its cooldown).
- Tests:
  - BikakuWormTests (2,419,779): Grand Devouring for a player-sized body, a small mob and a giant, by every worm of every form, every half tick:
    - finite, never jumping, never through you;
    - the body inside the mouth until it is out of sight;
    - the worm busy to the end;
    - the bulge only going down, and taken in at your back;
    - thin out of your back, only ever getting bigger toward the mouth with no jump, leaving your back leaning back and up;
    - swelling slowly; a worm not feeding exactly as before.
  - BikakuTests (1,640): the mutations by id, 60 s cooldown, the bites passing the feeding worm.
  - Also run: the full suite of every path.

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 103. Not tested in game.
