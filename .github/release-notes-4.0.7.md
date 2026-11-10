## 4.0.7 (HexGhoul): A full mask hides your kakugan

HexKagune 4.0.7 for Forge 1.20.1. Not tested in game.

Your words: "Full masks (only full masks not half masks) prevent CCG agents and humans to tell you're a ghoul when your eyes are in Kakugan."

### Behind a full mask

- **Your kakugan is hidden:** with a full mask on your head, CCG investigators and townsfolk can't see your eyes. They don't mark you, flee or shout "ghoul" for your eyes alone.
- **Still seen:** your kagune, a kakuja, and War, Peace or the Dragon are seen as before. A mask hides your eyes, not those.
- **Full masks:**
  - your 16 full ones (every one of yours except the eight half masks);
  - eight of the generic ones: Clown, Gas, Skull, Fox, Oni, Porcelain, Bandage and Kabuki.
- **No help:** half masks (Eyepatch, Ayato's, Red Muzzle, Fanged, White Muzzle, Black Respirator, Dotted, Black Grin) and the Cloth Wrap leave the eyes bare.

### My guesses

- The Cloth Wrap counts as a half mask (it covers the lower face).
- Someone who already knows you're a ghoul still knows it, mask or not.

### Tests

- **OutfitTests:**
  - which masks are full (16 of yours, 8 generic) and which aren't;
  - investigators and townsfolk check the mask before your eyes.
- **The full suite.**

Network: protocol 107 (unchanged).

Validation: the full `python3 build.py` completed with EXIT 0 on these sources before this was pushed. Not tested in game.
