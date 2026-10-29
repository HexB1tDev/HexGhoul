HexKagune 1.0.33 for Forge 1.20.1 (clients need PlayerAnimator, bendy-lib and Photon). Install on client and server (protocol 47). Testing build.

- **Mastery and RC:** every drain on the bar (moves, kakuja upkeep, healing, the kakugan) now scales with mastery: a full bar goes 2.25x as far at 500 and 5x at 1000 (was 2x, moves only).
- **Creative:** no RC is spent and no cooldowns run (War's and Peace's abilities included); `/kagune cooldowns off` now covers War's and Peace's too.
- **Sprint afterimages:** they now show for your own player in first person too (left behind you; turn round to see them), last longer and are more solid.
- **Faster sprint:** War's sprint boost 0.6 -> 1.3, Peace's 0.75 -> 1.8.
- **Black Sun:** the punch is drawn back for 36 ticks (was 20), coiling tighter and shaking, with sparks and smoke pulled into the fist.
- **Black Sun:** thrown with War's bare left fist, from over and behind the target, driving it down at 75 degrees; it hangs on the fist 0.7 s, feeling it, before it flies.
- **Black Sun:** the ring of smoke stands up across the sky, square to the punch, clipped at the ground; white streaks burst out of the back of the blow.
- **Black Sun:** the body tumbles limp from the launch, hits the ground and ploughs on through everything in its way, further than before.
- **Black Sun:** its blows can no longer kill mid-flight (the killing damage lands where it stops), so it always flies.
- **Black Sun:** blocks over the uppercut are smashed out of the way (no more low throw under a ceiling) and grow back with the rest after.
- **Black Sun:** after the punch War hangs in the air for 2.5 s with his view free, then falls on his own with no fall damage; the view lock ends at the punch.
- **Centipede Dominion was barely drivable:** the camera code swallowed W/A/S/D, M1 and RMB before the Dominion saw them. Fixed: all of them reach it now.
- **Centipede Dominion steering:** your own body no longer walks and snaps back under you, which was what made the steering fight your mouse.
- **Centipede Dominion drives as you'd walk:** W goes where you look, A/D go to your left/right (with W: diagonally), S brakes, sprint rushes.
- **Centipede Dominion transformation:** a short one first: War doubles over shaking, it tears out of his back, rears up over him roaring and crashes down ahead (2 s).
- **First-person War arms:** they were jammed against your eyes; now they're carried ahead like vanilla's hand, each punch drives into the middle of the view, and the scythe arm moves with every blow.
