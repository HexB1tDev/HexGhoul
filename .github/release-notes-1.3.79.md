## 1.3.79 (HexGhoul): 136 new looks for the children: other hair and eye colours (testing)
HexKagune 1.3.79 for Forge 1.20.1. Install on client and server (**protocol 72**, unchanged). Testing build: compiled, tests pass, **not tried in game**.

You asked: "grab the current children, and make dozens of other versions with different hair colors and eye colors? (careful with the hair cause skins usually have hair on the back especially for women, they have hair drawn on their back of the skin"

- **136 new versions** of the children's skins: 4 per skin for 34 of the 39 skins. Each has other hair (black, browns, auburn, ginger, blonde, platinum, silver, pink, blue, lavender, mint) and other eyes (blue, green, amber, violet, teal, brown, grey; never red, which is the kakugan's). The sheet `docs/handoff/child-looks-1.3.79.png` shows every one, front and back.
- **Hair on the back, as you warned:** on the girls' skins, long hair drawn down the back, over the shoulders and at the sides of the chest changes with the head's hair, all the way down. Hair is only counted where it runs down unbroken from the shoulders, so dresses, belts and shirts lower down keep their colours. Boys change only on the head (their hoodies and shirts are too often their hair's colour). Shading is kept: each hair pixel takes the new colour by its own light and dark.
- **Eyes:** on the 28 skins whose eyes are boxed by hand (from the blinks), the irises change colour. Whites, pupils, lashes and glints stay.
- **Who wears what:** a child's genes already carry a hair colour (a blend of both parents') and an eye colour (one parent's). A child now wears its skin in the version nearest those colours, so children take after their parents. A child keeps the design its skin already has, but **children you already have may change hair or eye colour** to the version nearest their genes.
- **Kept as they are:** daughter 2 and 4 (pale hair came out striped or blotchy), daughter 7 (black hair and a black hoodie are one colour), daughter 10 (its white bows were read as hair) and son 14 (pale hair, hidden eyes). Daughters 5, 8, 9, 14, 17 and 23 have hair too close to their skin colour to recolour cleanly, so only their eyes change.
- Kakugan, blinks and blushes still fit every version: the eyes are where they were.

Honest look (flat front and back previews, every skin zoomed in, not in game): a few keep a two-tone look on purpose where the original was two-tone (daughter 6 and 19), or dyed tips (daughter 3's red ends, daughter 16's purple ends). daughter 16's tail takes the hair colour too.

Untested in game: compiled and unit tested (every version's texture is there, each picked by its own colours).
