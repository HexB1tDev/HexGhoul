## 2.1.5 (HexGhoul): courtships out of the mod's own world

HexKagune 2.1.5 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

The companions' courtships now come from the mod's own world, not generic chores. The CCG's Doves (some of a given rank), wild ghouls of the Pack, a ghoul preying on a human village, a wild ghoul made your ally, village humans who come to care for you or tell you a secret, a walk through a human village at night, a drink from Touka's cookbook, ghoul meat. Every chapter's scene is rewritten to match.

- **Eto**:
  1. walk her through a human village after dark, her following you (2 minutes among its people);
  2. make a village human care about you (their regard at 60);
  3. kill 3 Doves while she watches, her following you;
  4. get a village human to tell you their secret;
  5. the Library in the dead of night.
- **Touka**:
  1. make her a drink from her own cookbook (the Anteiku Blend, or an RC Tonic or Kakuja Shot made with ghoul meat);
  2. kill 4 Doves asking about the cafe;
  3. somewhere high;
  4. kill the wild ghoul preying on her human friend's village (a ghoul killed with village humans within 32 blocks);
  5. kill the investigator hunting the rabbit: Rank 4 or above. She gives you her brother's rabbit's foot.
- **Ken**:
  1. 3 ghoul meat for the ghouls who won't kill;
  2. make a lonely wild ghoul your ally;
  3. stop a ghoul of the Pack;
  4. with him following you, kill the investigator tracking him: Rank 5 or above;
  5. one sunrise not alone.
- **Scenes**: 10 new or rewritten chapter scenes (Eto's walk past a child's window, her question about being loved by a human; Touka's human friend and why she studies; Ken feeding the ghouls who won't hunt, his first friend at the cafe, the fight beside him...).
- **Saves from before**: whoever is mid-chapter keeps the chapter number and its count starts again (it was counting something else). Every chapter closed still means waiting to be asked out.
- **New chapter kinds** in `npc/Courtship.Task` (appended): DOVES, DOVES_WITH, GHOUL_PACK, VILLAGE_GHOUL, ALLY, HUMAN_FRIEND, HUMAN_SECRET, VILLAGE_NIGHT. A chapter can ask for a Dove of a minimum rank (`Chapter.rank`).
- **Hooks**:
  - Dove and ghoul kills in `EtoServer.died` (`AgentBrain.rankLevel`);
  - an ally made in `GhoulNpcTalk`;
  - a human's regard and secret in `HumanTalk`;
  - the night walk in `CourtshipServer`.
- **Tests**: every chapter asks for something of the mod's own world (no plain monster kills, sits, walks or vanilla items); a Dove below a chapter's rank doesn't count; a chapter from before starts again once. Protocol stays 80.
