## 2.3.0 (HexGhoul): grown daughters take their mother's height

HexKagune 2.3.0 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

Grown daughters now take their mother's height, and each one varies a little. A daughter grows up to her mother's size: Eto's, Touka's, or a woman player's (the female body). She then comes out up to 4% taller or shorter than her mother, by her own genes, so sisters aren't all the same height. Before, every grown daughter was drawn at one fixed size.

- As a teen, a girl is never drawn taller than she'll be grown, so growing up never makes her shorter. Before, she lost about 2% when she grew up.
- Little girls and sons are drawn as before.
- Each daughter's height is fixed by her own genes and name, so it's the same every time you see her.
- **Code**: `ChildRules.daughter` and `girl` (pure, tested in `FamilyTests.daughters`); `Children.motherSize`. `S2CChild` carries the mother's size: **protocol "84"**, so install on both client and server. Not tested in game.
