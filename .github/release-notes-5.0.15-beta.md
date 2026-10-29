HexKagune 5.0 Beta (5.0.15) for Forge 1.20.1 (needs PlayerAnimator, bendy-lib and Photon). Install on both the client and the server.

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

**Fixed in 5.0.10**
- In Kaneki's first kakuja (the two centipede legs) you can only walk. Rapid Strike (Z), the grapple leap (J), grab and throw (G, N), crush, lift and slam (H, V, B) and guard (C) now do nothing in it, like M1 since 5.0.9. The legs hunt, block and fight on their own, and a refused key says so on the action bar.
- K still turns you back or takes you to the final kakuja, and R still puts the kagune away.
- Anything you were doing with your tails when the first kakuja came out is let go. Bodies you were holding drop, a grapple lets go, and your guard comes down.
- The abilities board shows only "Centipede legs fight on their own", "You can only walk", and the K and R keys for this form.
- The legs go after what's hostile to you: mobs targeting you, and anyone who hit you in the last 12 seconds. Passive mobs, and players who haven't hit you, are left alone.

**New in 5.0.11**
- The red of the kakugan glows harder. The iris is brighter and its glowing core covers more of it, and the red cracks and veins glow brighter too. The red halo around each eye is stronger and spreads further over the face, so the eyes stand out at night. The black of the eye stays pure black.

**New in 5.0.12**
- The red cracks inside the eye, across the black, glow like the veins outside it. They're thicker and brighter, with a hot red core inside a soft glow, and they glow at any distance (before, only up close). The black of the eye stays black around them.

**New in 5.0.13**
- Ayato's Wing Block is gone. C does nothing on the Ayato path (Kaneki keeps his guard). It's off the abilities board, the Ayato menu and the unlock track, which now goes from the start straight to the twin wings at 100. The Gatling, the Wing Vortex and the strikes work as before.
- The kakugan glows in the kakuja forms. Ayato's kakuja hunch, Kaneki's crouch and running in a kakuja lean the upper body forward, and the eyes were drawn where the head would be if it stood straight, so they ended up inside the leaning head and nothing glowed. They now move with the head, and so do Kaneki's mask, Ayato's half mask and wounds on the head and arms.
- 5.0.13 and 5.0.12 can't be mixed: the server and every player need 5.0.13 (an older client or server is refused when joining).
- Dedicated servers: every build now checks that nothing the server runs touches client-only code. The server needs only Forge; PlayerAnimator, bendy-lib and Photon are for clients.

**New in 5.0.14**
- Kaneki's Auto Projectile is a guard now. Instead of a tentacle slapping at an arrow (and missing), a free tentacle swings across the line the shots come in on and they stop on it. Shots from another side bring another tentacle, up to two at once. It also blocks Ayato's fire shots, crystal spikes and needles, which it couldn't see before. A blocked fire shot doesn't burn him. A blocked spike or needle goes into the tentacle and stays stuck there, moving with it, until it bursts or the tentacle goes back in. When it bursts it hurts the tentacle's guard, not Kaneki, but with Ayato's Bursting Shards its explosion goes off right next to him and hits him in full, and Crystal Bloom sets them all off at once. That's the explosive build's answer to the guard.
- It's a guard, not a wall. It takes a moment to come up, so close-range spikes and needles beat it. Each tentacle's guard breaks after about three shots and stays down for 3 seconds; the Gatling breaks one in about a second. Heavy shots punch through a worn guard. It drops whenever Kaneki attacks or grabs, and every blocked shot costs RC.
- With the Projectile Reflex Organ the guards are cheaper and sturdier, and arrows fly back at whoever shot them.
- Server owners can tune it in hexkagune-server.toml (combat): tendrilGuardStrength (0 turns it off) and tendrilGuardMax.
- 5.0.14 can't be mixed with older versions: the server and every player need 5.0.14.

**New in 5.0.15**
- Crystal thorns: Ayato can't be held in crystal mode. If a Kaneki grabs him while his wings are crystal, or he presses V while being held, dozens of long crystal spikes burst out of his whole body, straight through the tentacles wrapped round him, and one second later the kagune lets go of him. The centipede legs of Kaneki's first kakuja let go too.
- The spikes stay out for a moment after he's free, and grabbing him again in that moment only holds him for a split second. Then they sink back into him, or shatter if his wings thaw.
- The thorns don't do damage. They only break the hold, so a quick crush or slam can still land in that second.
- 5.0.15 can't be mixed with older versions: the server and every player need 5.0.15.
