## 3.1.9 (HexGhoul): Jealousy you can see; co-wives and half-siblings who know about each other

HexKagune 3.1.9 for Forge 1.20.1. Not tested in game.

The owner: "make the relationships even more complex ... jealousy is actually ... a really prominent thing ... if you have two wives ... the kids and all, they react to each other, like meeting each other, if they got like siblings from another mother, they interact and talk about it ... as immersive as possible ... make up your own shit." Every line here is new and written for this.

- **Jealousy you can see.** Being sweet on one partner (a kiss, a hug, a date, a ring, a night) while another of yours is within 24 blocks and can see you: they say so to your face, in their own voice, with a gesture to match (Eto sly and dangerous, Touka furious and hurt, Ken quietly devastated), and they stay jealous for two days instead of one. Jealousy still never costs affection or ends a relationship (3.0.8's rule).
- **Partners who meet.** Two of your partners within 9 blocks of each other while you're around (both free, not fighting) stop and talk, at most once every half day per pair, standing and taking turns like the family talks. What they say depends on how things stand:
  - the first time they meet as two people who love you;
  - one of them jealous right now;
  - both with children by you (they compare notes on the kids, by name);
  - settled into it.
  Each pair (Eto and Touka, Eto and Ken, Touka and Ken) has its own conversations in their own voices, 18 in all.
- **Half-siblings who meet.** Two of your children by different mothers or fathers within 9 blocks of each other (both old enough to talk) talk about it, at most once every half day per pair: about whose mother is whose, what you are to both of them, cake, fights at the market, and whether you love their mothers the same. Little ones, big ones, and a big one with a little one each have their own talks (8 in all). They use each child's other parent's real name.
- **New things to talk about.** Your partners' talk menus can bring up the other one by name (`cowife`): asking about them, jealous of them (only while jealous), and (with children) the kids becoming one gang. Three new conversations each for Eto, Touka and Ken. Your children can bring up a half-brother or half-sister and their other mother or father (`halfsib`, `{halfsib}`, `{halfmom}`): "why does {halfsib} have {halfmom}?", a fight over "you're only HALF", a teen admitting the other mother is "kind of nice, in a terrifying way", and a teen asking whether you love both the same. Four new conversations.
- No network changes (protocol 92). No new Minecraft API.
- Tests: DialogueTests 288 new checks (every partner has words for seeing it, each different; every pair has a talk for every mood, both speaking in turn, each saying their own lines whichever comes first; children by different parents talk at every age; every line filled, bubble-sized and free of banned names). SceneTests validate the 13 new scenes. FamilyTests unchanged and passing.

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 92. Not tested in game.
