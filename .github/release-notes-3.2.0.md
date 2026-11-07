## 3.2.0 (HexGhoul): The word at the door: bringing the child home, by who is talking

HexKagune 3.2.0 for Forge 1.20.1. Not tested in game.

The owner: "more conversations, like bringing the child home, like more diverse conversations, depending on how the character is or who it is, the personality they have ... make up your own shit." Every line here is new and written for this.

When Eto, Touka or Ken goes after a child of yours (it wandered too far, it snuck out at night, or it went on a rampage with its kagune) and brings it home, the word at the door used to be one line each. Now it is a real exchange, and it knows why they went out:

- **The parent at the door knows what happened.** Touka checking knees for scrapes after a wander, kicking the door shut on the dark after a night out, throwing a towel at a teen still red from a fight. Ken kneeling to unbutton a muddy coat, drawing the curtains at night, wiping a child's chin after a rampage and telling a teen about the first time *he* couldn't stop. Eto cataloguing the runaway like stolen treasure, calling a midnight walk "how gothic", stitching a teen up and warning them about "that place" in their head. 72 new lines, by parent, reason and age (toddler, child, teen). The old homecoming lines still come up now and then.
- **The child answers in its own temper, about what it did.** A sunny toddler saw a BIG rabbit; a shy child thought you'd be mad; a bold one "was guarding the house. From outside. It's a strategy"; a curious one counted forty-two bats; a mischievous one was "running TOWARD something"; a gentle one picked you flowers, which is why it went so far; a serious teen admits "the calculation was bad". 112 new lines, by temper (sunny, shy, bold, curious, mischievous, gentle, serious), reason and age.
- **The parent answers that kind of child.** Each parent has its own answer for each temper at each age, so Touka with a bold kid ("Brave and stupid is how people get eaten. Learn the difference.") sounds nothing like Ken with the same kid ("You remind me of me. That's what worries me.") or Eto ("Fearless heroes die in chapter three. Clever ones make it to the sequel."). 126 new lines.
- **Sometimes the child has the last word** (about half the time): a shy one asks you to stay till it falls asleep, a mischievous one tells you not to look under its bed, "no reason". 42 new lines.
- They gesture along: Touka folds her arms and sighs, Ken puts a hand to his chin, Eto laughs; the child waves, shrugs, giggles, thinks or hangs its head by its temper.
- No network changes (protocol 92). No new Minecraft API.
- Tests: FamilyTests' new door-talk checks: every parent, reason, age and temper has lines of its own; each parent answers each temper in its own words (never the same answer for two tempers); the shape (parent, child, parent, sometimes the child); every line filled in, bubble-sized and free of banned names; hundreds of different lines heard.

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 92. Not tested in game.
