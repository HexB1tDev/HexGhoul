## 2.0.28 (HexGhoul): the White Reaper's own skin

HexKagune 2.0.28 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

The White Reaper always wears the owner's own skin, on the slim arms it is painted for; no other investigator can roll it.

- The owner's file is kept unchanged as `art/white_reaper_skin.png` and shipped byte for byte as `textures/entity/ccg/ccg_reaper.png` (the tenth look, appended: `AgentRules.REAPER_LOOK`).
- Ordinary investigators roll from the first nine looks only (`AgentRules.regularLook`); a Reaper saved before this version changes into his skin when he next loads. Protocol stays 76.
