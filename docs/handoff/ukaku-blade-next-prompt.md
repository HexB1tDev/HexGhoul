# Prompt for the next session

Paste everything below the line into a new session on the HexGhoul repository.

---

You're continuing HexKagune (HexGhoul), a Forge 1.20.1 mod built by bytecode-patching the original jar plus the expansion in
`src/main/java/com/hex/kagune/control/`.

**First, always work on the VERY LATEST code**: `git fetch`, find the remote branch with the newest commit
(`git for-each-ref --sort=-committerdate refs/remotes/origin --format='%(committerdate:iso) %(refname:short) %(subject)' | head`) and merge
it into your session branch before touching anything. `main` is stale (1.3.0). When this was written the newest was
`claude/eager-ritchie-xni1ms` (4.3.0: the Steel Blade's flash on its right click, its finisher, the laugh, the blade dragging at rest, the small half mask back in the armor's colours: never the beaked model again).

**Read first:** `docs/handoff/ukaku-blade-handoff.md` (Session 2 at the top: my words, what was done and where, what is left), then the rules in
`docs/handoff/next-ai-4.1.9.md` (build, allowlist, protocol, sounds).

**The job, in order (each a version of its own: +1, after x.y.9 comes x.(y+1).0):**
1. Every mob cut at a random angle through its own body: "any mob, in general, the line should be randomly placed on their mesh itself and be
   cut randomly in any direction ... for mobs only and for players keeping this fixed a height." Big modded mobs too (a Mowzie's Frostmaw).
   The plan is in the handoff (capture the mob's own mesh as it's drawn, freeze it for the stun, the line where a random plane through its
   middle crosses it, then the two halves as ragdolls). Show me a GIF first.
2. An Ukaku victim's wings go down with the falling top half instead of vanishing. GIF first.
3. Ukaku wings: more violent M1s, spread open like wings; M2s less compacted, wider. GIFs first.
4. Then ask me how 4.2.9 and 4.3.0 play on my phone (the flash on right click, the finisher, the laugh, the blade dragging, any lag).

**Rules:**
- No agents or subagents. One thing at a time; push every time something works (CI builds a jar per branch; I test on my phone through
  Amethyst, so it must work there).
- `python3 build.py` must pass (check its exit code) before every push.
- Every preview render uses **my skin** (`art/user_skin.png`, already wired into the preview tools). Judge every render yourself before you
  send it, and say what differs from my reference. Show me a model before building animations or combat on it. I watch the renders as you
  make them and answer mid-turn: act on it at once.
- Every Ukaku sound is Minecraft's own (no sound pack), and none shared with the Rinkaku's or Bikaku's.
- **Versions: every change you push is a new version, +1. After x.y.9 comes x.(y+1).0. Never push a change under the old number.** Its
  release notes (`.github/release-notes-<version>.md`, my words first), README entry and handoff row go with it.
- Never put a model id in the repo. No PR.
- Pass these rules on in every handoff you write.

**How I work:** blunt and fast. If an idea of mine is weak, say so straight away. Don't stop to ask; just do it.
