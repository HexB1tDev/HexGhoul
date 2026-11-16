## 4.1.33 (HexGhoul): quests finished once, for good

HexKagune 4.1.33 for Forge 1.20.1. Not tested in game. No pose or model changed (nothing to render).

Your words: "Quests should only be done once. You can spam /kagune quest complete all over and like, I mean, it's not that you can spam
it. It's just once you did all of them /quest complete, all of them should be finished. Even if you pick another kagune, this should still
be finished because you finished all of them. You don't need to be typing it again."

**What it was:** `/kagune quest complete all` only finished the quests open right then, on your current kagune at your current mastery.
Pick another kagune or gain mastery and a new batch opened, so you had to type it again.

**Now:** `complete all` finishes the ones open now (paid as before), opens every unlock (as before), and then marks **every quest of every
kagune** done for good, saved with you: switching kagune, gaining mastery, or quests added in later versions never open anything again.
Running it a second time just says everything is already finished.

- The nightly hunts and the favours ghouls ask are one-night side jobs, not your progression, so they still come. (Say the word if you want
  them gone after `complete all` too.)
- `/kagune quests reset` (operators) starts everything over, the "all done" mark included.

### Other

- Protocol 117 (unchanged). The save gains `all=1` in the quest log (old saves load as before).
