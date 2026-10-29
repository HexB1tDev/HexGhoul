HexKagune 6.2.3 for Forge 1.20.1 (needs PlayerAnimator, bendy-lib and Photon on clients; the server needs only Forge). Install on both the client and the server; everyone on a server needs this jar.

**Witness This: further, varied, heavier**
- The target is knocked much further. Quick jabs send it about 7.5 blocks and the slow, heavy kicks up to 23 blocks, around a space about 16 × 22 blocks wide. The uppercut throws it 17 blocks up, and it always stays at least 7 blocks off the ground.
- Every blow has its own pace, never the same twice in a row. There are flurries of quick punches (a blow every 2–3 ticks) and slow, heavy kicks (up to 9 ticks). She winds up longer before the slow ones and recovers as fast as the next blow allows.
- Hit-stop: every blow freezes on contact for a moment (longest on the heavy ones) before the target flies.
- New kicks in the mix: a high roundhouse, a spinning heel kick (a full turn into it), an axe kick from above and a flying knee, alongside the side kick and the stomp. Half the blitz is now kicks, and the slow, heavy blows are always the big kicks.
- More impact: heavy blows and kicks get the big-punch flash and shockwave, a boom and a whoosh. The target is squashed along the line of each blow on contact, then stretched out as it flies. Her body throws itself into the blows: the chest drives through the punches, and her head snaps on the heavy ones.

**Fixes**
- From about the fourth blow on, she was drawn facing the wrong way. The turn built up past a full circle and got clamped. That was a big part of the messy head and body.
- The juggled target's head is now locked to its body. It no longer jitters as the mob's AI (or a player) looks around, and it never pitches past 70°.

**Longer stun after the kick-down**
- The target lies in the crater stunned for 3.5 seconds (was 1.4) before it gets up, and it can do nothing until it is up.
