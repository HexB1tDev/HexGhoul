## 3.2.1 (HexGhoul): Bringing your child home yourself: who meets you at the door

HexKagune 3.2.1 for Forge 1.20.1. Not tested in game.

The owner: "more conversations, like bringing the child home, like more diverse conversations, depending on how the character is or who it is, the personality they have ... make up your own shit." 3.2.0 did the companion parent bringing the child home; this is **you** bringing it home. Every line here is new and written for this.

**When it happens.** You carry a child of yours into your home (in your arms, on your shoulders, by the hand), or it walks in following you, after it has been out at least 45 seconds. Once per child every 5 minutes at most. The game tells apart two cases:

- **An outing:** it went out with you and you're back together.
- **Found:** it went off on its own (or with someone else) and you went and got it.

**Who meets you at the door.** If its other parent is Eto, Touka or Ken, is home, and can see you (not out with you, not fighting, not asleep, not in another talk), they come up to you with something of their own. They say it to you, and they name the child. At the same moment the child calls out to them as it comes in, in its own temper ("Before you say anything, Mama: I had it handled." / "*peeks at Papa from behind your legs and waves, tiny*"). Twelve conversations each, by what happened and how they are:

- **You found the child.** Eto had "three search parties planned and one revenge". Touka has had her boots on for an hour. Ken looked away "for a minute. That's all it takes."
- **You found it at night.** Eto's hands shake; she knows exactly what lives in the woods at night because "I am exactly what lives there". Touka is on the floor by the door with her kagune half out. Ken was about to "let it out" to find them.
- **Back from an outing.** Eto wants to know whether they fell in the river ("I need it for the book"). Touka made lunch and it's cold. Ken wants to know whether you saw the herons by the bend.
- **An outing at night.** The night is Eto's thing and she's wounded. Touka asks "with the patrols?" very calmly, which is worse. Ken kept a lamp in the window.
- **Carrying the child in, by day or asleep at night.** Eto gets her notebook out. Touka: "That's my spot." Ken: "Mind the creaky board."
- **You came home hurt.** Eto wants to "send it a letter. A very short one."
- **Jealous or sulking that she wasn't invited.** Eto reads a book upside down. Touka is "very happy. Look at my face." Ken just wishes he'd been asked.
- **Sleepy.** "Is everyone in? Count them for me."
- **Clingy or lonely.** Eto talked to the furniture. Ken: "It reminds me of before."
- **You took the child to see your other partner.** Touka can't braid. Ken is "afraid of being the one left over".
- **Just friends now, but co-parents.** "Whatever we are to each other these days, we're still good at this part."

Answers do what they say: hugs, holding hands, a kiss, her forgiveness, a sulk lifted, hearts.

**With nobody waiting, the child talks to you itself.** By its age and its temper (sunny, shy, bold, curious, mischievous, gentle, serious), and by whether it was an outing or you found it:

- a baby that headbutts you nose to nose;
- a toddler with a snail shell called Bob, or one that "wasn't lost, was EXPLORING";
- a child who found cave drawings and forgot the time, or one whose frog is now "Gerald the Second";
- a shy teen who "needed to be somewhere nobody was looking at me";
- a curious teen who found an old watchtower full of letters from someone waiting for somebody who never came home.

There are also night, brothers-and-sisters, other-parent and you-got-hurt variants. 58 scenes. Answers do what they say: picked up, put down, to bed, a hug, a spin, a toss, a high five, play, praise, sometimes a feather for you.

Other changes:

- A companion parent still out searching for that child stops once you bring it home.
- No network changes (protocol 92). No new Minecraft API.

Tests: SceneTests check that every companion meets you for every tier, mood, hour, found-or-not and carrying-or-not, and that every child of every age, temper, mood and hour has at least two scenes, one of its own temper. They also check that none of these scenes comes up except at a homecoming. FamilyTests check the homecoming rules (out long enough, not again too soon, how long the moment lasts, found or an outing) and the child's calls at the door (by temper, age and found or not, all different).

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 92. Not tested in game.
