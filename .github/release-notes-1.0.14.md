HexKagune 1.0.14 for Forge 1.20.1 (needs PlayerAnimator, bendy-lib and Photon on clients; the server needs only Forge). Install on both the client and the server; everyone on a server needs this jar (1.0.13 and 1.0.14 refuse each other at login).

**War's M1: a six-blow chain**
In War, left click is no longer a vanilla punch. It's a chain of six blows, one per click, thrown with fists, feet and the bladed arm:
1. **Jab.** The free hand snaps straight out.
2. **Reap.** The bladed arm sweeps across in front of you, right to left, catching a wide arc and throwing them to your left. It leaves a burning crescent and makes them bleed.
3. **Side kick.** You turn side-on and drive your foot out flat, sending them well back with a shockwave.
4. **Rising slash.** The blade comes up from low on your left to high on your right, lifting them off their feet.
5. **Spinning heel.** A full turn into your left heel, catching everything around you.
6. **King's Verdict.** The blade rises over your head and comes down with all your weight, while four rinkaku burst out of the ground through the target and everything near it, and a black ring races across the ground. It's the big hit: heavy bleed, a long stun, and the camera kicks.

How it plays:
- Click during a blow and the next one fires the moment the current one allows.
- Click within about half a second after a blow and the chain carries on; wait longer and it restarts from the jab.
- After the Verdict there's a short recovery.
- You lock onto whatever you clicked, or the nearest body near your crosshair within 6 blocks, and step in so the blow lands. Every blow stuns, so a full chain keeps them from hitting back.
- On a block, left click still mines, unless something is right by your crosshair.
- Using an ability cuts a blow off, and you can't throw blows while an ability has your body.

Each blow has its own pose (wind-up, impact, follow-through), and chained blows flow into each other instead of snapping back to the stance. The blade arcs swing with you and pass through the target on the impact tick. Fists and kicks leave shock rings, and blades spray blood along the cut. Missed blows still show their arcs and shocks.

**Fix for 1.0.13's effects**
The red glow on War's effects (the cleave's edge, the Black Sun, the crescents, the spikes' veins) used a render type that hides back faces, so it could vanish depending on which side you saw it from. It's now drawn from both sides.

**What's checked:** the build passes the full verifier and the dedicated-server check, and 233 logic checks cover the chain's order and timing, what each blow catches, every pose staying bounded and settling by its end, the handoff between blows, and the effects' shapes. The poses and effects were checked in renders made with the game's own code. **This hasn't been played in game yet**, so the feel (step-in speed, knockback, timings) is untested. Tell me what feels off and I'll tune it.
