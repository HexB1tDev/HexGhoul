HexKagune 5.0 Beta (5.0.9) for Forge 1.20.1 (needs PlayerAnimator, bendy-lib and Photon). Install on both the client and the server.

**Kaneki's kakuja**
- The kakuja bursts out of his body. On either kakuja, each centipede leg tears out of his back in turn. It shoots out past its full length, swollen, jaw open and claws splayed, then settles. On the final kakuja the red tentacles erupt right after the legs. Each limb comes out with blood and the tearing sound. This works with K and when the kagune comes out already in a kakuja.
- The mask grows in slowly. It spreads over his face from the lens outward over about 2.5 seconds, raw red at the edge and darkening to plum as it sets.
- The kakuja shatters when it ends. Pressing K to leave it, putting the kagune away, running out of energy or dying breaks every leg, tentacle and the mask into pieces. The pieces fly off him, spin, bounce once and fade, with a deep glass break sound. If he goes back to a form with tentacles, they grow back out of his back.
- The legs don't hunt until they are fully out.

**Server and PvP**
- Every client plays the burst and the shatter from the state the server already syncs, so everyone nearby sees and hears the same thing. Nothing new goes over the network.
- The pieces are capped at a fixed number for all players together.

**Controls**
- The key you hold with R to unleash the kagune is now its own keybind, **Unleash Kagune**, under HexKagune in Options → Controls. It's Left Alt by default and you can rebind it. The abilities board shows whatever you bind.

**Fixed in 5.0.2**
- Alt + R unleashes the kagune again with just Alt held (5.0.1 only took it with Ctrl + Alt).

**New in 5.0.3**
- `/kagune reset <player>` (operators) wipes everything kagune about a player as if they had never picked one. It clears their path, mastery, form, the mutations of both paths, the kakugan's eye placement, the flesh reserve and ghoul mode. The kagune goes back in (a kakuja shatters) and the "choose your kagune" screen opens again. They stay a ghoul.

**Fixed in 5.0.4**
- Alt + R really works with just Alt now. The mod reads the R press straight from the keyboard. Before, Forge handed an R press made with Alt held to anything else bound to Alt + R (another mod or a binding in Controls), so the kagune key never got it. That's why only Ctrl + Alt + R worked.
- Ayato's spikes and needles sink nearly half their length into the bodies and blocks they hit, instead of hanging off the surface.

**New in 5.0.5: Ayato's kakuja**
- Both of Ayato's kakuja burst out of his back. The steel spike tentacles forge outward from his spine, white-hot at their growing edge. On the final kakuja, the fire on his left ignites a beat later and flares yellow-white as it catches. Going from the first kakuja to the final one lights the fire side.
- They shatter the way Kaneki's do when they end (K, putting the kagune away, energy out, death): every tentacle, steel and fire, breaks into flying pieces with the deep glass break.

**Fixed in 5.0.6**
- The pull-out animations play slow enough to actually see. Each of Kaneki's legs tears out over 0.8 s, one after the other (they were 0.3 s), his tentacles over 0.6 s, and his body doubles over in pain as the kakuja rips out on K. Ayato's steel forges out over about a second, and the fire catches half a second later and spreads over another second.

**Fixed in 5.0.7**
- The kakuja are pulled out of the body for real. Bringing out a kakuja with K now runs the kagune's own manifestation, as when you unleash it. On Kaneki, every leg and tentacle grows out of his back in turn, with the pain pose, blood and tearing. On Ayato, both kakuja grow out tier by tier with the ignition. The server syncs it to every player.

**New in 5.0.8**
- The kakuja visibly erupt out of the body instead of swelling up from nothing.
  - Kaneki: blood gushes and scraps of flesh fly out of the wound in his back as each leg and tentacle tears out.
  - Ayato: steel sparks spray and a flash rings from his shoulder blades as the armored tentacles force out. On the final kakuja, the fire bursts into flame at his left side and licks outward along it as it catches.
- Ayato's kakuja grows out over 2 seconds instead of 1.3.

**Fixed in 5.0.9**
- Kaneki's first kakuja (the two centipede legs) can't be controlled any more: M1 does nothing in it, the punch included. The legs hunt and fight on their own, and a click says so on the action bar. The abilities board drops the M1 row for this form.
- Kaneki's first kakuja runs faster: Speed V instead of Speed III, in that form only. The final kakuja keeps Speed III.
- Ayato's Wing Vortex (Z) is a fire move only. In crystal mode Z won't throw it (thaw your wings with V first), and crystallizing during the wind-up cancels the throw. The final kakuja's fire cyclone is unchanged. The abilities board no longer lists the Wing Vortex in crystal mode.
- Ayato's kakuja is pulled out of his back properly. Each tentacle pushes straight out of his shoulder blade, then curls into place as the rest comes free. Before, a tentacle that had just started growing got shoved about a block away, down his back to his feet, and jumped around before it came out. That's fixed on both kakuja, steel and fire. The particles are unchanged.
