## 3.4.9 (HexGhoul): Bikaku, the worm kagune, in the game (its M1 bites)

HexKagune 3.4.9 for Forge 1.20.1. Not tested in game.

Your ask: *"my Bikaku path (the worm kagune with the mouth) ... starting with 3.4.9: the path in the game with the worm drawn, alive, in my kagune colour, with its M1 bites. Keep it long, with its zigzags; I liked that design."*

What's new:

- **A third path on the starting screen: Bikaku.** Its card shows a little figure with the worm coiling up behind him and over his shoulder, its fangs open when the card is selected. Its menu (the menu key) shows the kagune, mastery, RC, what its bites do, the kakugan button, its moves now and the moves still to come.
- **The worm in the game.** Alt + R brings it out of your lower back, mouth first along its own line; R puts it back the same way. It is the worm from the previews: about 8 blocks of zigzag coils behind you, its head over your right shoulder, a round mouth with six great fangs and three rings of teeth, ridges, grey bone spurs, glowing veins. It is alive at rest: it slithers, scans about in quick snaps, and now and then gapes, showing its teeth. Your other players see it too, synced from the server.
- **In your kagune colour.** The flesh, lip and veins take your colour; the teeth, spurs, gums and throat keep their own, so the mouth reads on any colour.
- **M1: a string of three bites.** Snap (thrown low over your shoulder), Lunge (reared up high and brought down on it), Maul (drawn far back, hooked in from the right, clamped and shaken: it stuns and throws them a little aside). Click again as a bite ends to go on with the string. It goes after what you aim at (or the nearest thing in front), reaches about 6.5 blocks from over your shoulder, and shuts on it partway through each bite. Each bite costs a little RC, makes them bleed, and grows your mastery.
- **Predatory Feeding** (from your spec): a bite that lands gives back half a heart; a kill with it gives two hearts more.
- **Kept long when it bites.** Thrown out, only the front of the worm uncoils, as far as that bite needs; the rest stays coiled in zigzags behind you, so it never shrinks into a short tube. Nothing of it goes through your own head or body.
- **First person.** The worm is above you at rest (look up and to the right to see it). A bite comes in past the top right of your view and lands on what you bite without covering it; nothing of the worm comes near the camera. (Other players see its true path.)
- **The original Rinkaku tendrils never come out on this path**, and the original's other kagune keys (grab, throw, crush, lift, slam, grapple, Thy Death, K) say "Your worm only bites for now" instead. The worm's own moves come in the next versions.
- Choosing Bikaku doesn't double you over in pain as the kagune tears out, and your idle body isn't the Rinkaku's feral stance (your spec: he "barely reacts to injuries").

Previews (off-game renders from the same code the game uses): `docs/handoff/bikaku-bites-3.4.9.png` (the three bites), `bikaku-firstperson-3.4.9.png`, `bikaku-rest-3.4.9.png`, `bikaku-colours-3.4.9.png`, `bikaku-grow-3.4.9.png` (coming out), `bikaku-card-3.4.9.png`, `bikaku-menu-3.4.9.png`, `bikaku-guide-3.4.9.png` (the move guide).

- **My guesses:** the three bites' names, shapes and timing (12, 13 and 18 ticks); 6.5 blocks of reach; half a heart a bite and two more on a kill; the Maul's stun (0.6 s); the RC per bite; how far the worm moves aside in your first person. No body animation goes with a bite yet (your arms and body stay as they are).
- **Not built yet** (planned in `docs/BIKAKU_PATH.md`): Devouring Maw and Predatory Drag, Ravenous Pursuit, Carrion Eruption, Monstrous Regeneration, the passives besides feeding, the kakuja. No path quests yet for Bikaku (the shared quests still run; their mutation points come as mastery, as Rinkaku's do).
- Network: protocol 94 (the path's new id and its state message): the server and every player need 3.4.9. No new Minecraft API.
- Tests: new BikakuTests (the path, the string, the bites, feeding, the card, the move guide) and BikakuWormTests (12,245 checks: every bite of the string lands on its target shut, never jumps, ends at rest, stays over 6.5 blocks long with its first coils zigzagging behind you, never passes through your head or body; growing out; first person keeps clear of the eyes).

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 94. Not tested in game.
