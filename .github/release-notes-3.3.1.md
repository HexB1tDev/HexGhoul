## 3.3.1 (HexGhoul): Ken teaches the Rubble Hurl and the Impale, with proofs

HexKagune 3.3.1 for Forge 1.20.1. Not tested in game.

From the owner's list (after the kiss bug): Ken teaching the Impale and the Rubble Hurl, with proofs.

**How it works now.**

- The **Rubble Hurl** (hold N with nothing in your tails, 3.1.6) and the **Impale** (hold H with nothing in your tails, 3.1.5) are no longer yours by mastery alone. Past their mastery (250 and 350), **Ken teaches them**, the Hurl first, then the Impale.
- **Proofs first**, in his words, before each lesson:
  - **Rubble Hurl:** *"Before you throw the ground at anyone, throw something that fights back."* Grab someone with your tails and **throw them, five times**.
  - **Impale:** *"The Impale is a crush that doesn't wait for you to be close."* **Crush five** in your tails.
  - Progress shows on the action bar as you go ("Ken's proof: 3 of 5 bodies thrown from your tails"), and "Go and see him" when it's done. Ask him again and he teaches it.
- **His lessons** are in his own voice: how to grab the ground under the dirt and pull until it lets go, hold it to grow it, let go where you're looking; how to bring one tail over your shoulder, make its end a point and let it go straight.
- Until he has taught you:
  - pressing the key says who teaches it and at what mastery ("Rubble Hurl: Ken teaches it, at mastery 250. Find them and ask.");
  - the guide on the left shows "Rubble Hurl (Ken teaches it)" and "Impale (Ken teaches it)", greyed.
- He still teaches the centipede, the Full Kakuja and the Dragon after these, as before.

**Your existing saves.** These two moves are new this week, so **no save keeps them**: even a maxed character goes to Ken for them (that way you get to see the lessons). The other lessons he taught you are kept: each lesson's saved mark is its place among his, and the two new ones were added after the old.

**A side effect on clashes.** 3.2.4 counts the share of your path's gates you've opened toward your clash power. A fully maxed Rinkaku now has two more gates to open, so it's at about 82% of that share (about 5% off the clash push) until Ken teaches them. After that it's full again.

- No network changes (protocol 92). No new Minecraft API.
- Tests: CompanionTests (Ken's six lessons, the old ones keep their saved bits, taught by mastery: the Hurl, the Impale at 350, then the centipede; the two proofs and their bars; his lesson words), ControlTests (the guide's "Ken teaches it" rows), the old dialogue and lesson checks moved past the two new lessons.

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 92. Not tested in game.
