## 3.4.0 (HexGhoul): Where WERE you?

HexKagune 3.4.0 for Forge 1.20.1. Not tested in game.

From your list after 3.2.1: **siblings at home reacting to the one brought back.** When you bring a child home (3.2.1; now with or without a house, 3.3.9), its brothers and sisters who stayed behind have something to say about it, and the one brought back answers.

**Who says what** (up to two of them, one after the other, right after the child's call-out at the door):

- **It wandered off, and an older one is home:**
  - a gentle, bold or mischievous older one **covers for it**: "It wasn't Rin's fault. I should've been watching." / "Okay, so, technically I said Rin could go." / "Leave Rin alone. They were with me. Sort of."
  - the others want to know **"Where WERE you?"**
- **It wandered off, and a younger one is home:**
  - a sunny, bold, mischievous or serious little one **tells on it**: "Rin went out ALL BY THEMSELVES! I saw! I'm telling!" / "For the record, I told Rin not to go."
  - a shy, gentle or curious one was worried, or just wants to know where it went.
- **Back from an outing with you:** an older one minds being left out ("Your bed's full of frogs now"); a younger one wants to come next time.

**The one brought back answers** by its own temper: "Snitch!", "Lies. Slander.", a mouthed "thank you" to the one covering, "I'll write you a report", or hiding behind you. Toddlers talk like toddlers on both sides. Babies say nothing yet, and nobody has a word for a baby carried in from a walk.

Siblings who were out with you, asleep, or already saying something else stay quiet.

- **Approximated:** "older" is by birth. Who reacts how is my own map of temper to reaction. What they say is shown in speech bubbles, as the children's lines already are; nobody walks over.
- No network change (protocol 93). No new Minecraft API.
- Tests (FamilyTests): every temper and age, found or not, older or younger, has words both ways; every way of reacting comes up; the big one covers, the little one tells, the serious one asks; babies say nothing.

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 93. Not tested in game.
