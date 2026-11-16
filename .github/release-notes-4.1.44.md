## 4.1.44 (HexGhoul): the Aogiri hideout and the CCG building spawn again

HexKagune 4.1.44 for Forge 1.20.1. Not tested in game. **Test in a new world.** `/kagune places` lists where each one-of-a-kind place
was built (or "not built yet"); `/locate structure hexkagune:aogiri_hideout` (or `ccg_building`, `anteiku`, `library`, `cyber_tower`,
`reaper_mansion`) finds it.

Your words: "Im 99% sure the Aogiri place IS not spawning at all, same with the CCG building".

**What was wrong:** before 4.1.42 they only ever appeared through the duplicate bug's shortcut. Their real path almost never passed:
- the hideout (272 x 263 blocks) had one spot per region, and needed no water anywhere under it (one pond and no hideout) on a perfect
  grid of heights, and since 4.1.42 at most 10 blocks of rise over all of it;
- the CCG building had one spot per region too, with no looking round it;
- every place (and Anteiku, the library, Ken's tower and the mansion too) was only built if the game's spot was exactly the one our own
  numbers expected: a mod in your pack that moves structure spots makes that never true.

**Now:**
- **The hideout is judged like the Small City** (it's about as big): land rising and falling no more than 14 blocks over it, a stream or a
  pond under it allowed (its foundation fills them), never a sea, a lake or a mountain.
- **The CCG building looks at 25 spots** within two chunks and takes the flattest dry one.
- **Every place is built wherever the game asks** (no more matching our own numbers); the claim from 4.1.43 keeps each to one per world.

### Other

- Protocol 118 (unchanged).
