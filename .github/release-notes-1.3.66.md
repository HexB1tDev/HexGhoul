## 1.3.66 (HexGhoul): War's back tendrils come alive, like your other kagunes' tentacles (testing)
HexKagune 1.3.66 for Forge 1.20.1. Install on client and server (**protocol 71**, unchanged; client-side drawing only). Testing build: compiled, tests pass, **not tried in game**.

Your report: "the back tendrils in WAR path are too unrealistic and solid, they are like unmoving branches of a tree, they have 0 similarity to the original tendrils of the other kagunes."

You were right on both counts: the shape was built as rigid armour, and they barely moved.

- **They move like the original tentacles now.** Each of the six sways on its own, on several slow waves at once: side to side, up and down, reaching out and drawing back, curling in and opening. The bend travels out along the tendril from the root, so it undulates like a tentacle instead of swinging stiffly. Before, the tips moved about 1.3 pixels either way. Now each tip travels 14 to 18 pixels (about one block) in a couple of seconds. They still jerk now and then, as if they smelled something, and thrash faster and wider in his moves.
- **They follow you.** They stream back behind you as you run, swing out the way you came from as you turn, hang below you as you jump, and lift as you fall, so they feel heavy and real. This uses the same per-tick turn, speed and rise tracking that Peace's blades already react to.
- **Smooth, soft shape instead of armour.** Gone: the armoured segments that flared at every joint, the hooked barbs and thorns all along them, the long hooked claw at the tip, and the blade-like ridge. Now each is a round, smooth tube that tapers from a thick root to a fine sharp point, with a slow swell running along it like breathing. They keep War's colours: black on top, glowing crimson underneath, the glowing vein along the underside, and a soft sheen down the back.
- **Never through the ground:** the lowest pair already reached your feet at rest, so their sway is kept above the ground (the originals lift off the ground too).
- Everything else about them is unchanged: they still reach out to seize and impale in your moves, fan out, wind into the cocoon and King's Defiance, and strike at what they hunt. First person shows the same living tendrils.

Judgment call: the reference sheet drew them as jointed, spiky arcs, which is what was built. I kept War's palette and arrangement (two over the head, two at the hips, two to the knees) but made them move and look like your original tentacles, as you asked. Tell me if you want them thinner, longer, more of them, or the barbs back in a softer form.

Untested in game: compiled and unit tested (a test checks every tip really moves, follows you, and never goes below your feet), and checked on the off-game preview. Try: stand in War and watch them from behind; then run, turn, jump.

(The new running animations for War and Peace you asked for are next, as 1.3.67.)
