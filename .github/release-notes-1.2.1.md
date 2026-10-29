HexKagune 1.2.1 for Forge 1.20.1 (clients need PlayerAnimator, bendy-lib and Photon). Install on client and server (protocol 52). Testing build.

- **Rebirth crash fix:** when Rebirth Instinct's flesh creatures reach a client with no renderer, the client now registers one and rebuilds its entity renderers before drawing them; if that still fails they're kept off that client instead of crashing the game.
- **Second registration:** the creatures' renderer is now also registered alongside the ghoul NPC's, and the log says when it's registered.
- **Creature tick guard:** an error in a flesh creature's server tick now removes just that creature and gets logged, instead of crashing the world.
