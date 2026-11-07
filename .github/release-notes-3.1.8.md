## 3.1.8 (HexGhoul): A signature move for the greatsword, the hammer and the whip

HexKagune 3.1.8 for Forge 1.20.1. Not tested in game.

The owner: "improve the move sets of the briefcase ... I meant the heavy blade, hammer and whip." The Bone Greatsword, Flesh Hammer and Tendril Whip each had their M1 string and two moves (right click, Sneak + right click). Each now has a third move of its own on **Sneak + M1**, giving it something its other moves don't (mobility, a line attack, defence):

- **Tendril Whip: Tendril Swing.** The whip is cast at the block you aim at (up to 16 blocks), bites into it and hauls you to it, up and over what's in the way. You let go just short of it. No fall damage from it. The tendril is drawn stretched from your hand to the exact spot it bit, for everyone watching (its latch is sent with the swing). 3.5 s cooldown.
- **Flesh Hammer: Fault Line.** Heaved overhead and driven into the ground before you. The ground breaks open in a line ahead of you, about 1.6 blocks further each blow, out to about 10 blocks (the ground's own block bursting up at each step). Everyone standing on the line is struck (1.25x) and thrown up. Stops at a drop. 6.5 s cooldown.
- **Bone Greatsword: Bone Wall.** The blade comes up flat across your chest as a wall for about a second. Blows that land on it deal only a quarter. A blow in its first quarter second is parried outright: nothing gets through, the striker is shoved back and staggered (1.2 s), and your next M1 is the string's last blow, its overhead cleave. 4 s cooldown.
- Each has its own animation in third person and in first person, and the item tooltips list them. Investigators carrying these weapons still use only their two moves; nothing about the NPCs changes. The Crystal Spear has no third move.
- Network: `C2SArmAction.THIRD` (4); swings now carry three new move codes, and the swing's spare field carries the whip's latch point. **Protocol 92**: client and server must both have 3.1.8. No new Minecraft API.
- ControlTests: 37 new checks (which weapon has which move, the spear none, investigators keep two, every move's code and timing, first-person motion still at its ends, the fault's reach, the wall's windows, the parry leading to the cleave, the latch surviving the packet to a quarter block, the tendril's tip on its latch), plus every new move in the existing check that every move is a real pose that ends back in its stance. Preview (stiff investigator limbs, the real meshes): `docs/handoff/weapon-signatures-3.1.8.png`.

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 92. Not tested in game.
