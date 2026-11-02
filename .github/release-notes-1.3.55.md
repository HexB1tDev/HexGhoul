## 1.3.55 (HexGhoul): one escort for a toddler that wandered off (testing)
HexKagune 1.3.55 for Forge 1.20.1. Install on client and server (**protocol 69**, unchanged). Testing build: compiled, tests pass, **not tried in game**.

Found by reading the code (listed in the last handoff): a parent going after a toddler that wandered off and an older brother or sister going after the same toddler could both try to walk it home at once, each tugging it toward themselves every tick.

- **A parent already going comes first.** An older child who was heading for its little brother or sister and sees that Touka, Ken or Eto is already on the way stops and says so, in its own temper: "Mama's got you. ...You're SO in trouble, Hana." (bold), "Be good for Papa, okay?" (gentle), "Ooh, Mama's got you. Hehe. Good luck, Hana~" (mischievous), "...Mama's got it." (a shy teen). 38 lines, by temper and age.
- **A big sibling already bringing it home is left to it.** A parent doesn't start after a toddler that someone else is already walking home.
- An older child doesn't set off after a toddler at all if a parent is already going.

Untested in game: compiled and unit tested only. Try: a toddler and an older child at home with a companion parent, and lead the toddler a little way out of the yard.
