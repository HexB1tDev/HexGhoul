## 4.0.6 (HexGhoul): A torn-off limb takes its clothes, armour and mask with it

HexKagune 4.0.6 for Forge 1.20.1. Not tested in game.

Your words: "Make sure that when limbs are removed, over layer of the skin also goes with the head and etc, coats and armors go with the limbs as well. (same with masks)"

### What goes with a part when it's torn off (Potential Unlock)

- **The skin's outer layer:** it already went with the part (the hat with the head, each sleeve and trouser layer with its limb), and it still does.
- **Clothes:** the outfits now go with the part. An arm takes its coat sleeve, a leg its trouser leg and boot, the head its cap.
  - They're drawn on the part where it flies, lies and is pulled back.
  - Your body no longer draws them where the part was.
  - A coat's tails stay with the body: they hang from the hip.
- **Masks:** any mask goes with the head, whether worn or the face-mask an NPC carries. So do the kakuja's half mask and the Rinkaku kakuja's mask.
- **Vanilla armour:** iron, diamond and the rest, and other mods' armour that uses the same armour layer, is no longer drawn on a part that's off you.
- **Bodies cut in two:** each half now keeps only the clothes on its own parts.

### My guesses

- Coat tails stay with the body rather than going with a torn leg.
- Vanilla armour isn't drawn on the flying part itself; only the mod's outfits and masks are.

### Tests

- **BikakuWormTests:** what a part wears sits on it, about its middle and the right way up; source checks for the body, the torn part, vanilla armour and the kakuja masks.
- **The full suite.**

Network: protocol 107 (unchanged).

Validation: the full `python3 build.py` completed with EXIT 0 on these sources before this was pushed. Not tested in game.
