## 4.0.4 (HexGhoul): Your 24 new masks

HexKagune 4.0.4 for Forge 1.20.1. Not tested in game.

Your words, with `Tokyo_Ghoul_24_Masks_1.20.1.zip`: "New masks, replace ALL the masks with these models."

### The masks

- **All 24 of yours are replaced** by the new pack's models and textures, every cube as you made it (3,838 in all).
  - They're read straight from your Blockbench projects.
  - The old fitting (eyes moved, half masks pulled down, masks widened) is gone. Your new masks are already fitted: half masks leave the eyes clear, and full masks have their eye holes lowered as your pack describes.
  - The pack's README says every shell clears the head and its hat layer. Tests check that every mask stays clear of both.
- **Half masks:** Eyepatch (Kaneki's), Ayato's, Red Muzzle (Tatara's), Fanged (Shachi's), White Muzzle (Rize's), Black Respirator (Yomo's), Dotted (Yoshimura's), Black Grin (Nishiki's).
- **Full masks:** the other sixteen. Touka's rabbit ears stand up above the head, and the Gourmet's (Tsukiyama's) beak points forward.
- **Sides checked:** the asymmetric details sit where your renders put them, on the wearer's left (Roma's diamond, Uta's eye circle, Itori's swirl, the strap buckles). Your pack's Java export and its Blockbench projects agree cube for cube.
- **One name changed:** "Gourmet's Half Mask" is now "Gourmet's Mask", since it covers the whole face. Every other mask keeps its item and its name.
- **Unchanged:** the nine generic masks the wild ghouls wear (they're not in your pack).
- **Your files:** the pack is kept in `art/outfits/masks/` (Blockbench projects, textures, Java models, manifest, README and your preview sheets). The OBJ and GeckoLib copies are left out because they hold the same geometry.

### Network and tests

- **Network:** protocol 107 (unchanged since 4.0.3).
- **OutfitTests:**
  - every cube of your 24 masks, as many as your manifest counts;
  - each half mask leaves your pack's eye zones clear;
  - no mask comes into the head or its hat layer.
- **The full suite.**

Preview: `docs/handoff/masks-4.0.4.png` (each mask on a head, front and three-quarter), drawn from the baked models outside the game.

Validation: the full `python3 build.py` completed with EXIT 0 on these sources before this was pushed. Not tested in game.
