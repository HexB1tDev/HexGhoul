## 2.4.4 (HexGhoul): better reactions to War and Peace

HexKagune 2.4.4 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

Better reactions to the final forms, War and Peace.

- **Spouses don't like War.** Eto, Touka and Ken each have new lines saying they hate seeing you like that and want you to take it off. Even Eto's fascination stops when it's you. They flinch, start nothing sweet while you wear it (or while the Dragon has you), and keep a step back. Eto still studies a stranger's War from afar.
- **Spouses are taken aback by Peace.** All three are startled ("Wait... that's YOU?") with the awe gesture.
- **Everyone else admits they can't win**: wild ghouls, strangers and screaming villagers now say so ("We can't beat that. Nobody can.", "I couldn't scratch that if I tried.", "The CCG can't stop that!"). Low-tier ghouls still run and humans still flee.
- **Your children**:
  - The little ones (toddler to teen) shake where they stand: crouched in on themselves, eyes on you, trembling. They don't run off any more.
  - Grown children are scared and back away behind your partner, or keep their distance.
  - This now happens for Peace as well as War, and stays up while you wear either one.
- **Code**: `EtoLines`, `KenScript` and `ToukaScript` (`dread`), `FearLines`, `ChildBrain.cower`, `Sightings.dread`, `EtoBrain.together`. Protocol stays 84. Not tested in game.
