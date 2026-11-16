## 4.1.81 (HexGhoul): dates at real places: they follow you there, and the date happens when you arrive together

HexKagune 4.1.81 for Forge 1.20.1. Not tested in game. Protocol unchanged (126).

Your to-do, its final step (quoted exactly): "Add to the to-do list that the final step is to better the quests, to include everything in the
mod + minecraft, daily  and romance quest betterment, so it's more fun, less boring, same for the joining gang quests." The plan shown to you
(you said "whatever you think is best") had, for romance, "real dates at real places (the hotel, the circus, the café after hours)". This
is that. Until now a date was a scene on the talk screen, there and then, wherever you stood.

- **A date is a trip now.** Ask your partner out ("Let's go somewhere."), pick one of their dates, and they say yes and where: "Fine. Go on,
  then. I'm right behind you. (Take her to the water (a river, a lake, the sea) by day, her following you.)". They follow you. When you
  get there together, at the right time of day, they come up to you and the date's scene plays. If you're already there, it plays at once.
- **Where each date is had**, from what the date is:
  - The companions' own places: the Library (Eto's old books, her late-night cooking, Ken's bookstore afternoon and his night reading),
    Anteiku (tea with Eto; Touka's cafe after closing; Ken cooking for you), the circus (Roma's private show, at night), Aogiri's hideout
    (Ayato's spar date; the stars from its roof), the Gas Masks' den (all three of Banjou's: the garden at night, the cottage roof, tea),
    the CCG tower (the General's training hall) and its roof (his "after the shift change").
  - Kinds of place, anywhere in the world: somewhere high with the sky over you (rooftops: Eto, Touka, Ayato, Roma), the water (Touka's
    beach, Ken's river and aquarium), a field under the open sky (stars at night, a park or a picnic by day), out in the rain (Eto's and
    Ken's rain dates), a village (Touka's movie and late-night snack run, Ayato's arcade and ramen, the graveyard walks, the General's dinner).
- **The action bar** says where you're going every few seconds ("A date with Touka: Anteiku at night, 120 blocks north-east"; for a
  companion's own place, which way), and if they've stopped following you.
- **Ten minutes** to get there, or they call it off in their own words ("Show cancelled! Refunds at the door! ...Another night,
  sweetheart."). A date only counts as today's (one a day, as before) once it happens. Logging out calls it off.
- The dates page only offers places the world has (a date at the Library only if the world has Eto's Library).

### Choices of mine you may want changed

- The hotel isn't one of the places yet: the world has many (one beside each village that gets one), and finding the nearest one needs the
  game's structure lookup, which the mod doesn't use yet. The General's dinner is in a village instead.
- Somewhere high is height 90 or more with nothing over you; the water is any water within 3 blocks; a field is grass, moss or podzol under
  your feet with nothing over you; at a companion's place is within 30 blocks of their home.
- They must be following you (or holding your hand) and within 16 blocks when you arrive.
- A date's scenes are as they were: only where they're had changed.

### Other

- Render: `docs/handoff/dates-4.1.81.png` (each companion's dates page and their yes to one; the game's own talk screen).
- Tests: `CompanionTests.dates481` (every date of the scripts has a place, every place is someone's date, the times, the checks, their
  words, how the server holds each); 29 rules sabotaged one at a time (`docs/handoff/sabotage-4.1.81.json`).
- Found on the way, before shipping: the yes line first carried a colour code, which the talk screen draws as it is ("§7"); gone.
