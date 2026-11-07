## 3.3.7 (HexGhoul): Talking it out

HexKagune 3.3.7 for Forge 1.20.1. Not tested in game.

From your list after 3.2.1: **reconciliation talks.** Before, making up was one line: "I'm sorry." to someone cross, "It's only you." to someone jealous, or one short scene they came to you with once it had passed. Now you can talk it out properly, and how it goes depends on what you say.

**"Can we talk about it?"** Talk to Eto, Touka or Ken while they're cross with you (after you hit them, or a bad answer). Next to "I'm sorry." there's now a talk:

- **Partners** get one of two, picked at random: *"Can we talk about it?"* (they say what hurt) or *"Are we okay?"* (they ask why you did it).
- **Friends** get *"Are we still friends?"*.

**"Can we talk about earlier?"** While a partner is jealous, it's on their first page, next to "It's only you.": they ask who that was.

Each talk has three or four ways to answer, and most lead on to a second page:

- **Going well:** owning it, being honest, offering to make it up (Eto: read to her tonight, every voice; Touka: a week of dishes; Ken: a walk, no talking about it). This ends the sulk or the jealousy for good, with a hug, a held hand, an arm, a blush or a laugh.
- **Going badly:** "You're overreacting." / "You're being paranoid." / "We're fine. Drop it." makes them sulk longer. "You're reading too much into it." leaves them jealous a while longer.

They have new lines for all of this in their own voices: Eto theatrical and dangerous, Touka blunt and secretly soft, Ken gentle and hurt.

**Coming to make up.** When it passes on its own, they still come to you to make it up. Each now has a second way of doing it for each:

- After a sulk: Eto brings tea ("It's not poisoned. That's the apology."), Touka bumps your shoulder, Ken marks a page in a book for you.
- After jealousy: Eto's writing was all stabbing, Touka admits she's lost a lot of people, Ken made a list.

Which one they use is picked at random.

- Approximated: what the talk says is the same whatever made them cross (it doesn't know whether you hit them or said something). A villager partner has no such talk.
- No network change (protocol 93). No new Minecraft API.
- Tests (SceneTests): every companion has a talk for a cross partner (two), a cross friend and a jealous partner, none when they're happy; each talk can go well or badly; nothing ends it but a kind answer; two ways each to come and make up, never come up with by chance.

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 93. Not tested in game.
