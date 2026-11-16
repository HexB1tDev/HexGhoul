## 1.3.63 (HexGhoul): the same big move meeting itself cancels both with a big impact (testing)
HexKagune 1.3.63 for Forge 1.20.1. Install on client and server (**protocol 71**, unchanged). Testing build: compiled, tests pass, **not tried in game**.

The owner: "if both use the same move like big punch, speed kick or any of the sort it should cancel both out and cause a big impact".

- **Big Punch meets Big Punch, Speed Kick meets Speed Kick, slam meets slam.** When one of these is let go at someone who is doing the same kind of move back at it (let go too, or still winding up the same punch or club), and they come within reach of each other facing each other before the blow lands, both moves are cancelled: neither lands.
- **A big impact instead:** always at least twice an ordinary clash (bigger still for the hardest blows), so the ground always shakes: your clash sound deep and loud, the wind and sparks, a ring of dust and grit on the ground, everyone near feeling the shudder, both thrown apart hard and stunned for most of a second.
- **Which moves count as the same:** a Big Punch and the Owl's Big Punch are both big punches (charging or let go); the Speed Kick and the Owl's Speed Kick are both speed kicks; the flesh club's axe kick and the Owl's Fissure are both slams. Different moves never cancel each other this way (the M1s keep their own clash).
- **Who:** a player on the Pale path and Eto (who fights with the same kit), against each other or against another Pale player.

Judgment call: War's and Peace's own moves are not part of this yet. Their kits run each move as a long sequence of blows with no way to stop one partway through cleanly, so cancelling them needs that built first; tell me if you want it next. Eto's child's club slam isn't included either.

Untested in game: compiled and unit tested only. Try: two Pale players (or you and Eto) throwing a Big Punch at each other at the same moment, or Speed Kicks.
