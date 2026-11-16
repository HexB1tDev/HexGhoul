## 4.1.70 (HexGhoul): Anteiku, joined through Ken

HexKagune 4.1.70 for Forge 1.20.1. Not tested in game. Protocol unchanged (125).

Your words (quoted exactly): "Anteikou - Joinable through Ken (quest) / Traits: / All ghouls and factions are neutral to the player but the
CCG. / Enegy bar increases size and increases maximum capacity. / Looks: Light brown name tag with a coffee symbol."

- **Joining, through Ken:** ask him on his first page ("I want to join Anteiku."). He asks why you want in, three answers; only Anteiku's
  own reason gets you further ("So I don't turn into a monster. And so nobody else has to be alone like I was."). Then he asks for 8
  cocoa beans for the café (the mod's coffee beans, as Touka's cookbook), held in your hand. Then 3 investigators who came asking about
  the café (agents or the CCG's ghoul division), counted over your hotbar. Then he takes you in. A member gets "I want out of Anteiku."
  on his page, as every leader has since 4.1.67.
- **Neutral to all but the CCG:** while you're one of Anteiku, no wild ghoul, gang or Aogiri member (their gate included) goes for you,
  nor hits you standing in the way, unless you start it (hit one, and it fights back), you're banned by its gang, or you go for one of
  its gang's own. The CCG still hunts you, its ghoul division too.
- **A longer bar that holds more:** your one bar (energy and food) holds one bar's worth more (everything it pays for costs half as much
  of it; with Potential Unlock, 4 instead of 3). Your words on the first try, whose bar ran past the hotbar (quoted exactly): "naw man
  just make the energy bar smaller but longer to fit the bar, dont make it past it wtf bruh"; of the two ways I showed, you picked "B:
  shrunk into its spot". So it's drawn on half pixels in the usual bar's own spot: half as tall, its tail about twice as long, the eye
  at its end. Nothing past the hotbar, nothing under your XP level number. It stays red: purple is still Potential Unlock's alone.
- **The name:** a coffee cup, a space, and your name in light brown (a colour of its own: none of the game's colour codes is brown), in
  chat and over your head. The reputation meter, talking to Ken or Touka, is that brown too.

### Choices of mine you may want changed

- Ken is now a gang leader, so 4.1.67's rules are his too: marrying Ken while in another gang takes you out of it, and a Ken married to
  someone in another gang asks them to quit it.
- Touka counts as one of Anteiku (her talk shows your reputation), not a leader.
- No dailies and no mask for Anteiku (you asked dailies for the CCG); killing other gangs' members earns Anteiku no reputation (it is no
  one's rival); Anteiku has no hideout to be thrown out of, so an empty bar never turns it on you.

### Other

- Renders: `docs/handoff/anteiku-ken-talk-4.1.70.png` (Ken's pages), `docs/handoff/anteiku-bar-4.1.70.png` (the HUD and the bar, as it
  is and for Anteiku: finer and longer in the same spot), `docs/handoff/anteiku-emblem-4.1.70.png` (the cup beside the other emblems, in a name tag and a chat line).
- Tests: `NpcTests.anteiku` (the chain step by step, the name and its colour, Ken's words, where neutrality sits in a ghoul's choice, the
  capacity on both sides, the bar measured as drawn: exactly the usual spot, half as tall); each rule sabotaged fails. At GUI scale 2 and
  4 its half pixels land on whole screen pixels; at 3 and 1 they can't, so it is a touch softer there.
- Next: the CCG faction through the General, with dailies; then the hotel.
