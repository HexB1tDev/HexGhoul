HexKagune 1.0.36 for Forge 1.20.1 (clients need PlayerAnimator, bendy-lib and Photon). Install on client and server (protocol 48). Testing build.

- **Black Sun trajectory:** the punch now sends the body along a long shallow line (22 degrees, was 75), it slams into the ground far ahead and ploughs forward through trees and walls; it no longer digs into the ground.
- **Final Horizon trajectory:** the great blade's blow sends the body down a long diagonal away from you (24 degrees), not straight down under you; the blades then drive it on along a shallow line through the terrain (up to 18 blocks).
- **Execution sent you across the map:** after the blitz the server kept re-sending your dash speed for 2 s. Fixed; you now drift down slowly instead.
- **Execution / Black Sun / Horizon / Ascent shaking:** the player was steered by chasing a one-tick-old position, which overshot every tick. They now follow their planned path, so no more shaking or snapping back and forth.
- **Your keys during those moves:** walking, strafing and jumping are ignored while Execution, Black Sun, Horizon, the Ascent and its landing carry you (they only fought the move).
- **Held mobs shaking:** mobs held in the air by Execution, Black Sun and Horizon no longer fall between holds; their gravity comes back when they're let go.
- **Stale speed bugs:** air M1s, the spike, King's Defiance and the M1 step-in no longer reuse the server's outdated speed for you (could fling you).
- **Peace blades two layers:** the dark outline copy behind each point is gone; one solid blade with darker keen edges.
- **Crossblade Waltz:** each blade goes into its own side of the body at its own height, slanting across so they cross inside it.
- **Piercing Salvation:** the six blades close into a real cone around the lance tip (flats out, points together); on recall they open and return slowly (1.2 s).
- **First person (Peace and War):** the kagune is drawn even when nothing else poses your body, so Peace's M1 blades show; broken shapes are dropped instead of drawn.
- **Sprint afterimages:** half as many and body-only (less lag), plus a light motion blur on the player, stronger as the sprint takes off.
