# HANDOFF: HexGhoul after 2.0.9. Read this first, then `docs/handoff/next-ai-2.0.5.md` (how the owner works now), then
`docs/handoff/next-ai-1.3.60.md` (house rules), then the top sections of `docs/handoff/companions-handoff.md` (one per version).

Branch **`claude/relaxed-carson-4y0frw`** (push only there; never main; no PR unless asked). Version **2.0.9** in `build.py` and
`patch/KagunePatch.java`; the next is **2.0.10**. Protocol **"73"** (unchanged this session). Nothing is tested in game.

## What this session shipped (each its own version; details at the top of `companions-handoff.md`)

- **2.0.5** someone else's children: a stranger's talk page by age and temper (`family/ChildPage`, `family/StrangerLines`), parents-only
  choices refused for others, children frightened by a stranger's kagune; NOT DATEABLE guarded by `FamilyTests.strangers`. The owner, mid
  session: "dont make a toddler/baby be able to say anything coherent": the new lines follow it (tested: two spoken words at most).
- **2.0.6** kagune inheritance: the owner was Ukaku with Touka (wings x wings = wings: no bug). They chose 40/40/20 for two kinds.
- **2.0.7** no teleporting home (only stuck in a wall, or no headway for 2 minutes unseen); out by day, home by night.
- **2.0.8** cooking: three rounds of stirring, vanilla's crackle (the owner's exception to the sound rule), no turning away, a chore
  under way before her own errands.
- **2.0.9** chest sorting by kind, chests labelled by her; the player's own named chests left alone.

## How it was checked

No local full build (the owner: short on usage; `build/` doesn't exist here, so `tools/quick_check.py` can't run either). Each version's
new pure tests were run in a small harness compiled against `src/main/java` + `stubs` (the pure classes only); the server code was only
read. CI is the real check: 2.0.5 built and released green; look at the runs for 2.0.6 to 2.0.9 first and fix any red one as its own
version.

## Open (ask the owner when it fits)

- **Old toddler and baby lines** (with their parents, in `ChildLines`, `talk/child.txt`, siblings, hide and seek...) are full sentences.
  The owner's "nothing coherent" was said while the stranger lines were being written; offer a pass over the old ones as its own version.
- Grown daughters' height (from 2.0.4's handoff): still unanswered.
- What a cook takes from the chest isn't shown in her hands on the way to the stove (EtoRenderer draws only gesture props).
- Mob Drops, Ores and the rest are decided by item id words; modded items mostly land in Whatever. Tune `ChestSort` word lists if the
  owner reports mis-sorted items.
