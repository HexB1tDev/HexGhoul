HexKagune 5.0 Beta (5.0.3) for Forge 1.20.1 (needs PlayerAnimator, bendy-lib and Photon). Install on both the client and the server.

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
- Alt + R unleashes the kagune again with just Alt held (5.0.1 only took it with Ctrl + Alt). Either Alt works while the keybind is on Alt.

**New in 5.0.3**
- `/kagune reset <player>` (operators) wipes everything kagune about a player as if they had never picked one. It clears their path, mastery, form, the mutations of both paths, the kakugan's eye placement, the flesh reserve and ghoul mode. The kagune goes back in (a kakuja shatters) and the "choose your kagune" screen opens again. They stay a ghoul.
