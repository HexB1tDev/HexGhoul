## 4.1.40 (HexGhoul): older worlds' townspeople find their streets again; "complete all" keeps later abilities open

HexKagune 4.1.40 for Forge 1.20.1. Not tested in game.

Your words: "just look for bugs on your own, like search for any possible bugs".

**What was wrong, and now:**
- **The old towns' people lost:** 4.1.36 deleted the human town and the CCG settlement along with their maps. In a world made before
  4.1.36 those towns still stand with their people, and each of them carries its town's map number; with the maps gone they walked the old
  jigsaw village's streets and patrol instead (the wrong town's spots: into walls, out of their town, agents patrolling nothing). The two
  maps are back, for their people only: the towns are still never built again.
- **"Complete all" and quests from later updates:** after `/kagune quest complete all`, every quest is finished for good (4.1.33),
  including any quest a later update adds. But the abilities those new quests unlock weren't opened (the command had opened every gate
  there was *then*), so such an ability could never be had: its quest shows done, nothing left to do. Now every gate is opened again at
  login for a log finished for good.

**Checked and fine:** the quinque M1 first (4.1.32) still digs blocks and only takes over clicks on entities and air; "complete all" does
open every path's abilities right away (the gates of other kagunes too).

### Other

- `village/RetiredSites.java` holds the two maps (kept by hand; `tools/exploration_structures.py` writes `SiteMaps.byMap` to fall back to it).
- Protocol 118 (unchanged).
