## 2.0.7 (HexGhoul): no more teleporting home; out by day, home by night
HexKagune 2.0.7 for Forge 1.20.1. Install on client and server (**protocol 73**, unchanged). Built and tested by CI. **Not yet tried in game**: please report anything that looks or feels wrong.

### From your request

- **Your spouse and your children are never teleported home for being far away.** They walk back.
  - Before, your spouse was put home from 64 blocks out (a companion without a shared home from 16), and a child from 64.
  - When nobody had been around them for a day while away, they now set off home on foot.
- **By day they can go past the house's edge:**
  - your spouse, teens and grown children up to 24 blocks past it
  - children up to 12
  - toddlers and babies stay as before
- **By night they have to be home.** At dusk their reach shrinks back to the house, they walk home, and bedtime goes on as before. Children's bedtime comes earlier than your spouse's.
- **Still put home only when something is wrong:**
  - Their body is stuck inside blocks for 3 seconds.
  - They're out when they should be home and haven't got any nearer for 2 minutes, with no player within 32 blocks to see it.
  - Death works as before.

### Unchanged

- **Following you:** they're still brought to you if they fall far behind (24 blocks; children by their own following rule).
- **Hand in hand:** they're still kept at your side.

### Known approximations

- The way home is vanilla's: they head toward home in steps of up to 16 blocks. Across water or cliffs they can stall, which is what the 2-minute rule is for.
