## 4.1.3 (HexGhoul): Fighting styles, and Speedster from Ayato

HexKagune 4.1.3 for Forge 1.20.1. Not tested in game.

Your words: "The default M1 fighting style is only unlocked through a new quest with ayato, until a fighting style is unlocked, the player can only use default punches in kakugan mode, right click is also default, the previous M1 and M2 flips are now all part of a fighting style called Speedster."

### Fighting styles

A fighting style is what M1 and right click do with only your kakugan open (the kagune put away).

- **With no style:** M1 is the game's own punch and right click is the game's own. No Speed Blitz, no flips.
- **Speedster:** the Speed Blitz on M1 and the flips on right click (W A S D picks the flip), exactly as they were before.
- The ability list (with your eyes open) shows your style: "Speedster", or "None: ask Ayato" with a plain "Punch".

### The quest with Ayato

1. Find Ayato and ask him to teach you. He teaches any ghoul, whatever your kagune, at any mastery.
2. He sets his proof: **kill six hostile things with your bare hands**, your eyes open, your kagune away and nothing in your main hand.
   - Mobs that attack you, hostile ghouls and Doves all count.
   - The count shows above your hotbar as you go.
3. Go back and ask again. He teaches you Speedster, and it's yours for good (it survives death).

### Saves from before

- **Nobody starts with Speedster.** A player who had the blitz and the flips yesterday has bare hands today, until Ayato's quest is done.
- An operator can give it at once: `/kagune quests complete all` opens it with everything else.
- If you were half-way through one of Ayato's other proofs (his Doves, for example), that one stays your next lesson and keeps its count. Speedster comes after it.

### What I left alone

- **Z (the Big Punch) and V (the Speed Kick)** with your eyes open. You named M1 and M2, so I only moved those into the style. Say so if the keys should wait for a style too.
- **Eto's lessons.** Two of them ask for blitz strikes and flips, so they now need Speedster first. Their tasks say "(Speedster)".
- **The One-Eyed Owl path.** Her blitz and flips are her kagune's own kit, with her kagune out. Ayato has nothing to teach her.
- Everything with the kagune out, on every path.

### My guesses

- The proof: six kills, and that they must be hostile.
- Ayato teaches it at mastery 0, before anything else of his.
- Ayato's lines.

### Network and tests

- **Network:** protocol 112. Clients and the server must both be on 4.1.3.
- **CompanionTests (`speedster`):**
  - with no style, M1 and right click are the game's own, and no other unlock gives a style;
  - Ayato teaches it first, to Rinkaku, Ukaku, Bikaku and Graft, at mastery 0, never to a human;
  - his proof is six bare-handed kills;
  - his older lessons keep their saved places, and a proof under way is kept;
  - the server refuses a blitz or a flip without the style, and leaves the Big Punch and the Speed Kick alone;
  - the client leaves both clicks to the game without the style;
  - no save is given it for its mastery;
  - the ability list shows the right rows either way.
- **The full suite.**

Validation: the full `python3 build.py` completed with EXIT 0 on these sources before this was pushed. Not tested in game.
