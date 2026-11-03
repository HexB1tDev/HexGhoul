## 2.0.8 (HexGhoul): cooking at the stove takes its time, and a task is finished first
HexKagune 2.0.8 for Forge 1.20.1. Install on client and server (**protocol 73**, unchanged). Built and tested by CI. **Not yet tried in game**: please report anything that looks or feels wrong.

### From your request

- **Cooking is a real process.** Your spouse goes to the chest first (the lid opens while she takes what she needs), walks to the stove, and cooks there. The stirring now goes round three times, about 24 seconds instead of 8.
- **The stove crackles while she cooks**, as you allowed (vanilla's sound, the one exception to the hexkagune-only rule):
  - a furnace or blast furnace: the furnace's crackle
  - a smoker: its smoke
  - a campfire: its crackle
  - At a table, where nothing is on the fire, there's no sound.
- **She doesn't turn away while cooking.** Her body and head stay on the stove every tick. While any chore has her, she no longer glances at players or looks about.
- **A task isn't dropped for another she thinks of.**
  - A chore under way now comes before her own errands: going after a straying child, a moment with you, a word with a friend, asking for a kiss. They wait until it's done.
  - Her own gestures and words no longer cut a step short: her hands go back to it.
  - A child on a rampage still can't wait.
  - What you or danger start still stops her where she is: a fight, you talking to her, a kiss, taking her hand, nightfall.

### Known approximations

- What she takes from the chest isn't drawn in her hands on the way to the stove. The ingredients still leave the chest and the food comes out in one go when the cooking is done, so nothing is lost or made twice.
- The crackle plays on the PLAYERS sound slider (the one already proven to work).
