## 4.0.5 (HexGhoul): New worlds load again; official NPCs stay at home

HexKagune 4.0.5 for Forge 1.20.1. Not tested in game.

### The crash creating a world

Your words: "also game crash upon creating world" (your crash report: "Failed to load registries").

- **Cause:** since 4.0.2, the placement that makes the Aogiri hideout one per world was missing a field Minecraft requires (its `salt`), so no new world could be made.
- **Fixed:** it has its field. Worlds you already have were never affected (they load their own data).
- **Can't slip through again:** a new test checks every structure placement in the mod for every field Minecraft requires, and that the structures and biome tags it names exist.

### Official NPCs

Your words: "Official NPCS disappear from their residential areas and dont come back they should always stay there unless married to a player and set home in another place."

### What was happening

- **Taken over:** when a character's home lay unloaded for five game days (you away elsewhere), a body of hers made with another copy of her building could take her place. When you came back and her home loaded, her own body was sent away, and she was gone from her home for good.
- **Lost, never replaced:** a body lost without dying was never made again. Only the dead came back.
- **Married, wrong home:** a character married with a home of your own came back to her old residence after dying, not to your home.

### Now

- **Never replaced:** once anyone has talked to an official character (Eto, Touka, Ken, Ayato, Noro), no other body ever takes her place while she lives, however long her home lies unloaded.
- **Made again at home:** if you've been at her home for half a minute and she's nowhere about (and she isn't out with someone or left waiting somewhere), she's made there again. A lost body of hers is sent away if it ever loads.
- **Her home:** her residence, or the home she shares with you once you're married and have set one. She comes back there after dying, a day later, as before.
- **Unchanged:** left waiting somewhere or sent off, she still walks home after a day with no one about.

### My guesses

- Half a minute at her home (within 24 blocks) before she's made again.

### Tests

- **NpcTests:** every structure placement complete, naming structures and biome tags that exist.
- **EtoTests:**
  - once met, no other body takes her place;
  - missing from home only with someone there half a minute and her body nowhere;
  - not missing while she's out or dead.
- **The full suite.**

Network: protocol 107 (unchanged).

Validation: the full `python3 build.py` completed with EXIT 0 on these sources before this was pushed. Not tested in game.
