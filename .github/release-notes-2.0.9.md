## 2.0.9 (HexGhoul): your spouse sorts the house's chests by kind
HexKagune 2.0.9 for Forge 1.20.1. Install on client and server (**protocol 73**, unchanged). Built and tested by CI. **Not yet tried in game**: please report anything that looks or feels wrong.

### From your request

- **Your spouse sorts every chest and barrel in the house by kind.** Each time she tidies a chest, everything in it that belongs elsewhere goes to the right chest.
- **The kinds:**
  - **Food:** anything that can be eaten, and ghoul meat
  - **Blocks**
  - **Tools & Weapons:** tools, swords, bows, tridents, shields, arrows and the like
  - **Armor**
  - **Ores & Minerals:** ores, raw metal, ingots, nuggets, gems, coal, redstone and the like
  - **Plants & Seeds:** seeds, saplings, flowers, crops
  - **Mob Drops:** bones, string, leather, gunpowder, pearls and the like
  - **Whatever:** anything she doesn't know what to do with
- **She labels the chests herself,** as you chose. A chest she gives a kind is named for it, so you see "Food", "Blocks", "Whatever" when you open it.
  - The kinds with the most things get chests first.
  - There is always one "Whatever" chest.
  - If there are more kinds than chests, the extra kinds go to "Whatever".
- **Your own names count.**
  - A chest you name with an anvil after a kind ("Food", "armour", "Tools", "Whatever"...) is used for that kind.
  - A chest you name anything else is yours. She never takes anything out of it or puts anything in it.
- **A double chest is one chest:** both halves get the same name.
- **Nothing is lost.** Each stack goes whole into its chest where it fits. If it doesn't fit, it stays where it was until there's room.

### Known approximations

- **How she decides:** by the item's id and what the game says about it (can it be eaten, does it place a block), so modded items mostly sort by their names. Anything unclear goes to "Whatever".
- **Spare chests:** a chest that isn't needed for a kind stays unnamed. She empties it into the others over time.
