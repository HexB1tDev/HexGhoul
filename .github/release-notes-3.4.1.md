## 3.4.1 (HexGhoul): A word at the door

HexKagune 3.4.1 for Forge 1.20.1. Not tested in game.

From your list after 3.2.1: **the companion and the child talking to each other at the door after your scene.** When you bring a child home and Eto, Touka or Ken meets you at the door (3.2.1, 3.3.8), once you've answered their card (or let it go), the two of them now have a word of their own. The child walks over, and they talk in four turns:

1. **The parent starts**, in their voice: found, or back from an outing; to a toddler, or to a bigger child. Eto: "Tell me the plot. Don't skip the part where you got lost." Touka: "Okay. You and me. Talk. What were you thinking?" Ken: "I'm not angry. I just want to understand."
2. **The child answers** by its temper: "I followed a beetle, and then a bird, and then there was a cave." / "Was I lost? Or was everyone ELSE lost?" / "I made a mistake. I've already thought about why."
3. **The parent answers that**, by who they are and what kind of child it is (shy or gentle, bold or mischievous, sunny or curious, serious). Touka to a bold one: "Next time you want to run off, you run off with ME. Deal?" Ken to a shy one: "You just have to come back to people who love you. And you did." Eto to a curious one: "Just... read the map first."
4. **The child has the last word**: "*crosses fingers behind back* ...Okay, real deal."

**To a baby** the parent talks alone (a hum, "You smell like wind").

It waits until your card is done and the two of them are free and close, with the child not in your arms. If that doesn't happen within a minute, it's let go.

- **Approximated:** the parent's answer follows four groups of tempers, not all seven; the lines are the same with or without a house.
- No network change (protocol 93). No new Minecraft API.
- Tests (FamilyTests): every companion, found or not, every temper, toddler, child or teen: four lines in turn, all filled in; a baby gets the parent alone.

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 93. Not tested in game.
