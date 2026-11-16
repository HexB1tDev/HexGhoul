# HANDOFF: HexGhoul after 1.3.67. Read this first, then `docs/handoff/next-ai-1.3.60.md` (house rules, family talk), then the top
sections of `docs/handoff/companions-handoff.md`.

**RULE 1 (the owner's, all sessions, no exceptions): at 97% of the 5-hour or the weekly usage, stop at once, save (commit and push,
WIP marked as such), write the handoff, and give the owner a prompt for the next session. How to read the usage: `CLAUDE.md`.**

Branch **`claude/relaxed-carson-4y0frw`** (check it out from origin; push with `git push -u origin claude/relaxed-carson-4y0frw`).
Never push to `main`; no PR unless asked. Network `PROTOCOL` is **"71"**. Nothing since 1.3.28 has been tried in game: never say anything
works in game. House rules as in `next-ai-1.3.63.md` (each fix/feature its own version, full `python3 build.py` EXIT 0, banned names, sounds
only from the hexkagune: pack, allowlist new API, append only to ordinal-sent enums, never alter the owner's files).

## Done in the session that made 1.3.64 to 1.3.67 (each pushed)

- 1.3.64 `Sightings.of`: dread is what is worn now (War/Peace on, the Dragon, Rinkaku's or Ukaku's final kakuja), not `KingServer.awake`.
- 1.3.65 everyone fears War: `FinalSight`, `FearLines`, `Script.dread`; ghouls keep running, humans flee screaming (bubbles: no scream
  sound in the pack), CCG falter, children hide behind the other parent (COWER), companions' own lines and gestures (FLINCH, AWE).
- 1.3.66 (the owner's bug report) War's back tendrils alive like the original tentacles (`WarModel.alive`, follow-through, smooth shape).
- 1.3.67 War and Peace runs (`KingRun` charge and glide), Ken's Peace runs too.
- Not done from the earlier list: War/Peace same-move cancels (optional; their kits need a way to cut a move short first).

## The owner's last update (their words, sent during 1.3.67; "Finally:")

> "Make sure the wild ghouls hold up, as in manifest their Kagunes properly + hunt humans at night (Kagune and Kakugan off during the day).
>
> Make the kagune arm blade for ghouls sharper and less cubic.
>
> Make my litematic into a schematic and make this village spawn in plains biomes only, adapt it's spawn best you can, humans will spawn
> there, they run from anyone with eyes on, or kagune out, CCG agents often patrol the streets in an orderly maner, never going into the
> houses, Humans often trade in the streets, are assigned their own houses and patrol the streets, they never leave the village perimeter
> through!
>
> Make general conversation and make humans dateable with romance and different types of personalities, you can take animations from eto,
> kids, touka and kaneki for base, mix it together and adapt it to NPCS, make the humans as cute looking as possible you can also adapt
> skins I gave you for the kids as long as you make sure they are differently looking from the kids, such as adapting hair color, clothes,
> etc.
>
> CCG agents now have different ranks (random in the wild) the higher the rank the more HP and faster they are, CCG agents dont attack
> anyone unless their eyes are revealed or kagune shown.
>
> make sure this is as bug free as possible, however we gotta start working faster, we're running out of usage in all accounts."

And two more, sent during 1.3.68:

> "if you're a ghoul at some point before starting a relationship you have to admit to your human love interest that you're a ghoul, may or
> may not require convincing.
>
> Humans will run to a nearby ccg agent in the village if any are available, CCG agents top priority if to defend humans, so they will drop
> what they're doing to protect them if they are in trouble."

Their litematic was uploaded to the session that made 1.3.67 as `Brick_Village-from-abfielder.litematic` (kept unchanged in the repo as
`art/village/Brick_Village-from-abfielder.litematic`). "Kaneki" in their words means Ken (the name is banned in the jar).

## Plan (each its own version; see the top of `companions-handoff.md` for what of it is done)

1. Wild ghouls: kagune out properly when they fight (check `GhoulNpcBrain.fight` `mind.out`, `GhoulNpcRenderer`), hunt humans at night
   (`GhoulNpcBrain.wanted`: `HumanNpc` prey at night), kagune and kakugan off by day (`dawn`).
2. Ghoul NPCs' arm blade (`npc/BladeArm`, `BladeArmModel`): sharper, less cubic; preview `tools/BladeArmPreview.java`.
3. CCG ranks (`AgentRules`: rank by chance in the wild, health and speed by rank, the name tag), and they attack only revealed ghouls
   (eyes or kagune: `AgentBrain.wanted` / `reveal`; wild ghoul NPCs only with their kagune out or eyes on).
4. The village: the litematic to a structure (NBT, `data/hexkagune/structures/`), a jigsaw-free single-piece structure set for plains
   only (worldgen JSON: structure, structure_set, biome tag), its humans (house assignment, street patrol, trading in the streets, a
   perimeter they never leave), CCG patrols on the streets, never indoors; a frightened human runs to the nearest free agent (if any near),
   and agents drop anything to defend a human in trouble (their top priority).
5. Humans: general talk, personalities, romance and dating (reuse `Romance`, `RomanceCard`, the scene system, companion gestures),
   cute looks from the children's skins (different hair, clothes); a ghoul player must admit what they are to a human love interest before
   the relationship can start (a confession scene that may need convincing: by personality, regard).
