## 1.3.37 (HexGhoul): a sleeping spouse or child only half wakes when you touch them, drowsy and in their own way; a parent gets up for a CCG investigator by the house too (testing)
HexKagune 1.3.37 for Forge 1.20.1. Install on client and server (**protocol 67**, unchanged from 1.3.36). Testing build: compiled, tests pass, **not tried in game**.

- **Bedtime, as it works (since 1.3.21).** At night your spouse and children go to bed in the home you set ("This is our home now."). They find the beds within the home: a double bed (two beds side by side, facing the same way) is the spouse's, single beds are the children's (eldest nearest home first). Each walks to their own and lies down. They get up in the morning, if hurt, if picked up, if their bed is gone, or if you wake them properly.
- **Waking your spouse (new).** Right click them while they sleep: they half wake, drowsy, and stay lying down. Eto teases even half asleep, Touka is cross about the hour, Ken asks if something's wrong. You get three answers:
  - **"Sorry. Go back to sleep."** They mumble and settle back.
  - **"\*kiss them goodnight\*"** (their partner only). A sleepy smile, a little closer.
  - **"Wake up. I need you."** They're up and listening, with the usual talk page.
  If you walk away, they just keep sleeping.
- **Waking your children (new).** A little one asks if it's morning, a child mumbles about a dream, a teen wants to know what you could possibly want at 2 a.m. A baby just sleeps on. Answers: **tuck them in** and **stroke their hair** (parents), or **"Go back to sleep."**
- **A parent guards the house at night.** A sleeping spouse already woke for a hostile mob near the house. Now a **CCG investigator** near the house wakes them too. They get up, deal with it, and go back to bed.

Untested in game: compiled and unit tested only. Try: marry, set the home with a double bed and single beds, wait for night, right click them in bed; `/summon hexkagune:ccg_investigator` near the house at night.
