## 3.2.2 (HexGhoul): One at a time: nobody piles on

HexKagune 3.2.2 for Forge 1.20.1. Not tested in game.

The owner: "we also need to minimize the interactions. In terms of like not reduce the amount. It's just like if I'm being already interacted by someone giving me a hug, for example, like another one shouldn't be coming to do the same thing."

Before, each kind of NPC only kept out of its own kind's way: villagers didn't crowd each other, and only one person at a time could come up with a talk card. Nothing stopped Touka walking over for a hug while Eto was already on your arm, your toddler running in for a hug in the middle of a kiss, or a village spouse coming over while your kid was showing you a flower.

Now there's one rule for everyone: **while someone has you, nobody else starts something with you by themselves.** Someone "has you" while they are:

- walking up to you for something;
- hugging you, kissing you, on your arm, or holding your hand;
- in a moment with you (a child's hug, wave, flower or clap), or walking up for one;
- waiting on your answer to a card;
- in a talk screen with you.

Once they let go, the next one waits **4 seconds** before coming over. Nothing is taken away: the hug, the kiss, the "come here, I want to tell you something" all still happen, just one at a time.

- **Who it covers:** Eto, Touka and Ken (their hugs, clinging, a hand, kisses and pecks, the welcome-back hug, the kiss after a meal, coming for the baby night, coming over with a word), your children (their own hugs, waves, flowers and claps, and the run-to-hug when you're back), village partners and neighbours coming over, and anyone's talk card.
- **Already on their way:** someone who set off toward you by themselves and finds someone else got there first lets it go. They'll come another time.
- **Never held back:** what *you* ask for (a hug, a kiss, their hand, a talk). Someone already with you can still say what they came to say.
- **At the door:** a homecoming (3.2.1) only waits for someone who has you right now, not the 4 seconds after.
- No network changes (protocol 92). No new Minecraft API.
- Tests: CompanionTests check the rule: someone else has them now, so wait; just let go, so a moment's breath and then free; your own turn never waits; a homecoming waits only for now.

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 92. Not tested in game.
