HexKagune 1.0.28 for Forge 1.20.1 (clients need PlayerAnimator, bendy-lib and Photon). Install on client and server (protocol 44). Testing build: playability fixes.

- **War's rinkaku no longer move in 20 fps steps:** his whole model was rebuilt once a tick and drawn frozen in between. It's now built at two ticks and blended every frame, like Peace's kagune.
- **Smooth clock, safer:** anything that fires on one exact tick (FX bursts, dust, sounds) is back on whole game ticks, so it can't skip or double-fire; only what's drawn between ticks uses the smooth clock.
- **No M1 during the blitz or Black Sun:** clicking mid-combo used to start a normal hit and break the move.
- **Indoors and in caves:** the blitz and Black Sun only throw the target as high as there's room, never up into the ceiling, and you no longer blink into walls.
- **Black Sun into hills:** the body smashes through thin things (walls, roofs, trees) but stops at real ground instead of tunnelling into a mountain; skidding into a cliff stops it with an impact, and skidding off an edge sends it falling on instead of floating.
- **Invulnerability fixed:** after the blitz or the spike rise you were immune to every hit for about 2.5 s; now only to the fall.
- **Ability keys report when they're let go** (groundwork for the held moves coming next).
