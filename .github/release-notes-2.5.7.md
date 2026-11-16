## 2.5.7 (HexGhoul): Another baby on the sweet page, five days apart; the night comes at home

HexKagune 2.5.7 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

Another baby: asked for on the sweet page now, in each companion's own words, five Minecraft days after the last was born; and at the home you share, the night you asked for actually comes (she comes to you for it, as she did before you had a home).

- The ask moved out of the Talk topics into "Be sweet to your wife/husband." (`EtoTalk.babyOption`, on Eto's, Touka's and Ken's sweet pages): "Let's have a baby.", "Let's have another baby." once you have a child together, and "About tonight..." once asked. Each answers in their own words, a first child or another (`EtoTalk.babyAnswer`; `EtoTalk.sweetly`, `ToukaScript.sweetly`, `KenScript.sweetly`).
- The wait: another baby can be asked for 5 Minecraft days (120000 ticks) after the last one was born (`FamilyView.BABY_WAIT`, `sinceBorn` from `Children.lastBorn`). Asked sooner, they say how many days are left. Talk topics no longer offer it ("Do you think about a family?" and "Do you want a family someday?" only before your first child).
- The bug: at a shared home she went straight to bed at nightfall and only waited there, and before bedtime her chores held her, so the "she comes to you" step never ran. Most players move into a home after the first child, which is why "tonight it is" was never followed. Now `EtoBrain.comesForBabyNight` runs as she goes to bed: if a baby was asked for and the night has not been had, she comes to you (out of bed if she was in it, from up to 32 blocks), takes your arm (Touka hugs), and the night follows. Already beside her in bed, it follows as before (`babyNightInBed`).
