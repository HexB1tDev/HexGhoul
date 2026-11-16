## 3.6.8 (HexGhoul): Fix: the Bikaku kakuja's camera fits its size

HexKagune 3.6.8 for Forge 1.20.1. Not tested in game.

Your words: "IN kakuja form you cant see anything, the camera needs to adapt to the new size."

- **Third person:** the camera now turns round the kakuja's crest (about 4.4 blocks up) and sits 9 blocks behind it, instead of Minecraft's 4 blocks behind your own eyes. Before, that spot was inside the kakuja's back and its rear tentacle, so flesh filled the screen. It still stops short of blocks, as Minecraft's own camera does. The front view (F5 twice) works the same way, in front.
- **First person:** your view is lifted to the kakuja's crest, a little forward, so you look out from the top of it.
- **Both:** the view grows with the kakuja as you go into it and comes back down as it falls away. Nothing of your own kakuja is drawn within 2.6 blocks of the lens, so a tentacle swinging past it can't blank the screen.
- **Unchanged:** other players see your kakuja as before; out of the kakuja your camera is Minecraft's own.
- **My guesses:**
  - The numbers: the crest view at 4.4 blocks up and 0.8 forward, 9 blocks back in third person, 2.6 blocks kept clear round the lens.
  - Lifting the view in first person too (rather than only fixing third person).
- **Known limits:**
  - In first person your crosshair is at the lifted view, but Minecraft still aims from your real eyes, a few blocks lower. The kakuja's bites pick from a wide cone, so they should still find what you look at, but a block you point at is picked from your real eyes.
  - Minecraft's camera can't be moved by an open method, so it is moved through Forge's reflection helper; if that ever fails, the view goes back to Minecraft's own and the game carries on.
- Network: protocol 103 (unchanged).
- Tests: BikakuWormTests `kakujaView()`. Out of the kakuja the view is your own; fully grown it is on the crest and 9 blocks back; it never jumps on the way in or out. In third person, Minecraft's old spot had flesh within 2 blocks of the lens over 21% to 66% of the view (measured); the new one has none, and the kakuja fills about 37% to 54% of it. In first person from the crest, at most a quarter of the view ahead is blocked. Also the full suite.

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 103. Not tested in game.
