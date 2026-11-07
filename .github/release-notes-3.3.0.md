## 3.3.0 (HexGhoul): The move guide shows what your keys do right now (Rinkaku)

HexKagune 3.3.0 for Forge 1.20.1. Not tested in game.

From the owner's list (after the kiss bug): the moves in the guide on the left.

**What was wrong.** On the Rinkaku path, G, N, H, V and B do different things depending on whether one of your tails is holding someone, and the guide crammed both into a line or showed moves that do nothing at that moment:

- "Grab · Throw · Rubble Hurl (hold N, nothing held)" on one line, keys G·N.
- "Impale (hold) · Crush what a tail holds" on one line, key H.
- **Lift** (V) and **Slam** (B) shown as always ready. They only work on someone a tail holds; with nobody held they just say you need a target.

**Now the guide follows your tails, live:**

- **Nothing held:**
  - **G** Grab
  - **N hold** Rubble Hurl
  - **H hold** Impale
  - **V · B** Lift · Slam, greyed: "once a tail holds someone"
- **A tail holds someone** (the subtitle says so):
  - **N** Throw them
  - **H** Crush them
  - **V** Lift them
  - **B** Slam them
  - **G** Grab another

Every Rinkaku form whose tails are yours: the seed, twin, four-tail and the final kakuja. The first kakuja's legs still fight on their own, and its board is unchanged. Each move still shows dim with the mastery it needs until you have it. Your own key bindings are shown, as before.

- The previous session wrote a version of this but it was lost with its session (never pushed); this is a fresh build of it.
- No network changes (protocol 92). No new Minecraft API (the client already knew who your tails hold).
- Tests (ControlTests): with nothing held, Grab on G, the Rubble Hurl on N held, the Impale on H held, Lift · Slam greyed; with someone held, Throw, Crush, Lift, Slam on N, H, V, B and Grab another on G, with no Hurl or Impale rows. Every form.

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 92. Not tested in game.
