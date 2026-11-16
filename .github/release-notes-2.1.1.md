## 2.1.1 (HexGhoul): final forms rest five minutes: Exhaustion

HexKagune 2.1.1 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

The final forms (War and Peace) now have a 5-minute cooldown, shown as a new custom effect, Exhaustion, with its own icon: a kakugan half shut under a heavy, cracked lid, its red dimmed, a drop of RC running from it. Once a final form is let go (taken off, run dry, or the player killed in it), Exhaustion runs for 5 minutes, and calling War or Peace meanwhile is refused with the time left ("Exhausted: War can't be called again for 4:12").

- The rest is kept on the player's profile (`KingRules.RESTS`, the game tick it ends), not in the effect itself, so drinking milk, dying or relogging can't end it early. The server puts the effect back while the rest lasts and takes it away when it's over (`KingServer.restTick`, every half second).
- None while cooldowns are off (`/kagune cooldowns off`, or creative), for testing. The admin preview of a form doesn't start it.
- New `ExhaustionEffect` (harmful, registered with the kitchen's effects), its icon `textures/mob_effect/exhaustion.png` (drawn by the new `tools/exhaustion_icon.py`, 18x18 in the style of the kitchen's four), and lang entries for the effect, its description and the refusal.
- Tests in `KingTests.exhaustion`. Icon beside the kitchen's: `docs/handoff/exhaustion-icon-2.1.1.png`. Protocol stays 80.
