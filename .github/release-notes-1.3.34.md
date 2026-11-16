## 1.3.34 (HexGhoul): every Rinkaku kagune gets a random colour of its own; /kagune color reroll for another (testing)
HexKagune 1.3.34 for Forge 1.20.1. Install on client and server (**protocol 66**, unchanged). Testing build: compiled, tests pass, **not tried in game**.

- **A random kagune colour.** The first time you spawn in after this update, your kagune (the Rinkaku tendrils of the Ken path, its mask and its shards) gets a random colour instead of the default crimson. Any hue, kept vivid and visible, with veins in a much darker shade of the same hue. About 1 in 25 rolls is a rare one: near-black with blood-red veins, or bone white with ash veins. You get a chat line saying which ("Your kagune will come out dark teal (#1F6B66)").
- **`/kagune color`** tells you your colour. **`/kagune color reroll`** rolls a new one, as often as you like. Everyone near you sees it change right away.
- If you'd already picked your own colour in the kagune screen, it's kept. Only the default crimson gets replaced, and only once. Rerolling always works.
- The colour lives where the original mod already keeps it, so everything drawn in it (tendrils, Centipede, mask, shards) follows without any new rendering code.

Untested in game: compiled and unit tested only. Try: log in (watch for the chat line), `/kagune color`, `/kagune color reroll` a few times with the kagune out.
