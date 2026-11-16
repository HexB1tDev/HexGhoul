## 4.1.17 (HexGhoul): no gap in the coats, places on top of the ground and never twice, Ukaku spikes in their colour everywhere

HexKagune 4.1.17 for Forge 1.20.1. Not tested in game. Previews: `docs/handoff/coat-walk-formal-4.1.17.png`, `docs/handoff/coat-walk-crimson-4.1.17.png`.

### The coat's gap while walking

Your words: "whenever you walk, like, it opens like a fucking, like a gap in every armor that we got. Like the two, they open like a gap.
And that gap is what I don't like about the armor."

**What it was:** each half of a coat's tails rides its own leg, as you asked in 4.1.14. Each front hangs about 3 pixels in front of its leg's hip hinge, and leans out 3 degrees.
- **A slot down the middle:** when the legs swung apart, one half went forward and the other back. The slit between them (half a pixel at the hip, about one and a half at the hem) opened onto the black trouser.
- **A gap under the jacket:** the half going back also dropped its top below the jacket's hem. That's the dark patch you circled.

**Now:**
- **The halves overlap** at the middle like a buttoned coat, front and back. Each reaches 1.3 pixels past the middle (still half a pixel past it at the flared hem), and the right one sits a hair in front so they never flicker. Whichever leg is forward, its half covers its own leg all the way across.
- **A strip of the coat's own cloth** sits across the hip under the jacket, behind the fronts. When a half drops back, cloth shows there instead of the trouser.
- Standing, the coat now looks closed instead of showing a dark slit.
- Both coats are checked through a walk, a run, a jump and three kicks, from the front, the side and three quarters.

### Places under the ground

Your words: "they are spawning under the ground, which cannot happen, bro. This is causing major fucking chunks to be missing. It should be
always spawning above ground, always at the top max area"

- **Ken's tower:** it was fixed at y=63, whatever the ground. In mountains it went in dozens of blocks deep, and its terrain adaptation carved out everything inside its huge box: the pit and the missing land. It now starts on the ground's height, like the library and Anteiku.
- **Every one-of-a-kind jigsaw place** (Anteiku, the library, Ken's tower) is lifted onto the highest ground under its whole footprint, sampled every 4 blocks and at the edges. It's never cut into a hillside; the ground is built up under its low side instead.

### More than one of a place

Your words: "Yep, I just confirmed it. ... there's multiple structures of each, which cannot happen, bro."

**Why worlds have duplicates:**
- 4.1.9 to 4.1.13 each had their own rule for where a world's one goes.
- 4.1.14 started counting from scratch with its claims.
- So a world played across those versions can hold one from each set of rules.
- Another way to get one: a mod that makes chunks through its own wrapper could get every spot built.

**Now:**
- **A place already standing counts:** when its chunk loads and no spot holds the claim yet, it becomes the world's one, and nothing else gets built. That stops worlds from before the claims making more.
- **Another mod's chunk wrapper** can no longer get every spot built: only `/locate`'s own question counts as "just asking".
- **Duplicates get logged:** any second one found is written to the log with both chunks (`HexKagune: WARNING: a second anteiku stands at chunk ...`).
- Duplicates that are already built stay in your world. A new world gets exactly one of each.
- If you ever see a second one in a world made fresh on this version, send me the log lines starting with `HexKagune:`.

### Ukaku spikes in blocks

Your words: "when it hits like a tree or a block or whatever, instead of being green, it turns back to the default color, which is red. It
should remain the same color as the Kagune or when it hits the mob ... This also applies for the kakuja"

- **The cause:** a spike stuck in a body was drawn in its shooter's colour, but one planted in a block was drawn without any. That gave the default red, and its chips came out red too when it crumbled.
- **Now:** it keeps who shot it, and the spike, the kakuja's needles, the lance and their crumbling chips all stay in the kagune's colour. The same goes for crystal thorns crumbling.

### Other

- **A new rule in `CLAUDE.md` and the handoff, in your words:** every AI renders and judges its own work until it's perfect, animations top tier.
- **Your full list is in the handoff word for word, in your order.** Next: every quinque's M1, drawing and sheathing, first person, more quinques. Then Arima, the quests and the Small City. Your files are kept in `art/reaper/` and `art/city/`.
- Protocol 117 (unchanged).
