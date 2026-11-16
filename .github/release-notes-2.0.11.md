## 2.0.11 (HexGhoul): bigger, louder clashes; the weaker one flies
HexKagune 2.0.11 for Forge 1.20.1. Install on client and server (**protocol 73**, unchanged). Built and tested by CI. **Not yet tried in game**: please report anything that looks or feels wrong.

### From your request

- **Louder clashes.**
  - The clash sound is louder and carries further, more so the stronger the two blows were.
  - A hard clash plays the sound twice at once; a huge one three times. Minecraft never plays a single sound louder than full, so stacking it is what makes it louder.
- **A bigger impact.** The burst of air and sparks is drawn about 40% bigger than before, growing with the power of the blows. The ground starts shaking at smaller clashes than before.
- **The weaker side flies further.** When two kagune clash, each side is pushed back by how much harder the other's blow was:
  - an even clash is unchanged
  - the weaker is pushed back up to 2.2 times as far
  - the stronger is pushed back less
- **Too big a gap overwhelms the weaker side.** From a 2.5× difference in blow power, it gets the Speed Kick's effect without the kick animation:
  - flung off fast and low, smashing through blocks it can break and slamming into anything in its way
  - tumbling through the air
  - lying where it lands, stunned, then getting up

### Known approximations

- Only a player who wins a clash sends the other through blocks. When an NPC wins, the loser still flies, tumbles and is stunned, but breaks no blocks (the kick's block smashing has always been a player's).
- A crash on the way hurts as hard as the stronger blow was. The clash itself still hurts no one.
- The gap is measured by each blow's damage, so mastery, forms and kakuja all count.
