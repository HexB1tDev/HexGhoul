## 3.6.0 (HexGhoul): Bikaku: the forms (Twin Worms, Three Worms)

HexKagune 3.6.0 for Forge 1.20.1. Not tested in game.

Your words: "we need more forms. Currently there's only one one of these things, but another form should have like two worms. The third should have three", and of the moves: "to basically become overpowered, like, to do enhanced versions of themselves whenever they're, like, in different transformations".

- **Forms, picked in the menu (FORM, under your mastery):**
  - One Worm (as before).
  - Twin Worms (mastery 250).
  - Three Worms (mastery 500).
  - The Walker (750) is shown as "soon": it comes in its own version and can't be picked yet.
  - Your form is kept in your profile (death keeps it). If your mastery is below the form you picked, you get the strongest it has opened.
- **The worms:**
  - All come out of the one root in your lower back, each in its own lane so they never bunch up behind you: one out to your right, one to your left, the third high behind your head (the lanes you approved for Ravenous Pursuit).
  - Each is full size, alive at rest on its own rhythm, in your kagune colour.
  - Each is drawn round your body (3.5.5) and round blocks (3.5.9).
- **M1's bites take turns among the worms:** Snap by the first, Lunge by the second, Maul by the third (or back to the first with two). Each is thrown from its own worm's shoulder: held further (3.5.8), round blocks (3.5.9).
- **The moves (this version):** in a form of several worms, the first worm makes each move from its lane while the others rest. Ravenous Pursuit makes its three worms as before.
- The path's card on the starting screen now lists the forms (One Worm, Twin Worms, Three Worms, The Walker coming soon, The Kakuja).

Preview (off-game): `docs/handoff/bikaku-forms-3.6.0.png` (Twin and Three at rest from round about, and M1's bites taking turns).

- **My guesses:**
  - 250 and 500 for the forms (like the Rinkaku's, every 250).
  - The worms' lanes.
  - The order the bites take turns.
  - That the first worm makes the moves for now.
- **Next (3.6.1): every move made stronger by all the worms**, as you said:
  - Devouring Maw and Predatory Drag each catching a body per worm.
  - Ravenous Pursuit with all the worms.
  - Carrion Eruption coming up more than once.
  - Then The Walker (3.6.2).
- Network: protocol 101 (the state carries the form; a menu action picks one). No new Minecraft API.
- Tests: BikakuWormTests (108,209: each form has its worms, each at rest keeps out of your body, its worms well apart behind you, the bites taking turns and landing, the others at rest, one worm exactly as before, the forms' masteries and fall-back). BikakuTests, PathTests, PathScreenTests, ControlTests.

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 101. Not tested in game.
