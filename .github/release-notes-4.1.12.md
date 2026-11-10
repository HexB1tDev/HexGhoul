## 4.1.12 (HexGhoul): places never nowhere, /kagune places, slower slams, no slam particles, coat halves on their own legs

HexKagune 4.1.12 for Forge 1.20.1. Not tested in game.

### Finding the places

Your words: "And I still can't. Mind you, I still fucking can't."

- **Never nowhere:** the hideout, the CCG building, Ken's tower, Anteiku and the library now **always** get a spot. If none of their candidate spots is on dry land, they're built at the nearest one anyway.
- **New command: `/kagune places`.** It lists where each one stands (x, z) as the world worked it out, so you can `/tp` straight there.
  - Run `/locate structure hexkagune:aogiri_hideout` first; that makes the world work the spot out.
  - If `/kagune places` still says **"not worked out yet"** right after a `/locate`, the game never asked HexKagune for its places in that world. That points at another mod in the pack, and that line is exactly what I need from you.

### The wing slams

Your words: "The hits are way too fast" … "these fuck ass red Minecraft dust particles in crit particles that you've added. No one ever asked for that."

- **No particles:** the slams leave nothing in the air any more. No red dust arc, no crit sparks. (They came in with 4.1.2's slams; they're gone.)
- **Slower, so you can see them:**
  - Each slam is about 1.75× longer: a slap 16 ticks (was 9), the swing and the rising wing 20 (12), the overhead slams 22 (13), the twin slam 28 (16).
  - The wing's swing itself is now about a fifth to a third of a second, not one tick and a bit.
  - The hit lands a little later in each move, so it stays on the wing.
- **Damage per hit is unchanged.** Because the hits come slower, damage per second is lower. Tell me if you want the hits stronger to make up for it.

### The coat

Your words: "I truly don't know how in this world you expected the clothes to open like that and look good."

- Each half of a coat's tails now hangs from **its own leg only**.
  - A kick or a tucked leg lifts that leg's flap, and the other half keeps hanging.
  - The coat parts down the middle instead of the whole skirt flaring out flat like a disc.
  - No leg comes through its own half.
- Not previewed off-game (there's no off-game renderer for outfits): check it in a jump and a kick.

### Other

- Protocol 116 (unchanged).
