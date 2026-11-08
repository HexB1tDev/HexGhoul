## 3.4.5 (HexGhoul): Fix: the Spire Wave stays on your hand

HexKagune 3.4.5 for Forge 1.20.1. Not tested in game.

Your report: *"In the Warpath, Spirewave now is kind of bugged. It detaches from your hand if you move. Like, before it was fine, you could, like, move and it would still be attached to your body. But now ... whenever you move around, that detaches."*

**What was wrong.** My 3.2.8 change drew War's left arm in your own first-person view, but it started the centipede mass at the spot where the move set out (the point the server sends), not at that arm. Stand still and they lined up. Move, and your arm went with you while the mass stayed hanging where it started. Third person (and everyone else watching you) was fine: there the mass already followed the body's hand.

**Now,** in your own first-person view, the mass starts at the fist you see. That position is worked out again every frame from where your eyes are and where you're looking, the same way the arm itself is placed. It goes where you go and turns as you turn, while the far end of the mass keeps running along its line toward what it's going for, as it always has.

- **Approximated:** the screen-shake bob and the arm's slight lag when you turn quickly aren't followed exactly, so the mass may sit a hair off your fist while sprinting.
- No network change (protocol 93). No new Minecraft API.
- Tests (KingTests): the fist is ahead of your eyes, to the left, whichever way you look; walking and climbing move it with you exactly; turning turns it with you; every centipede in the mass starts at your fist, however far you've walked from where it set out.

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 93. Not tested in game.
