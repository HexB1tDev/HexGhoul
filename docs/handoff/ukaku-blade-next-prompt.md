# Prompt for the next session

Paste everything below the line into a new session on the HexGhoul repository.

---

You're continuing HexKagune (HexGhoul), a Forge 1.20.1 mod built by bytecode-patching the original jar plus the expansion in
`src/main/java/com/hex/kagune/control/`.

**First, always work on the VERY LATEST code**: `git fetch`, find the remote branch with the newest commit
(`git for-each-ref --sort=-committerdate refs/remotes/origin --format='%(committerdate:iso) %(refname:short) %(subject)' | head`) and merge
it into your session branch before touching anything. `main` is stale (1.3.0). When this was written the newest was
`claude/eager-ritchie-xni1ms` (4.2.8 plus the Ukaku blade work, the mask in the armor's colours, the Steel Blade's flash and its finisher).

**Read first:** `docs/handoff/ukaku-blade-handoff.md` (Session 2 at the top: my words, what was done and where, what is left), then the rules in
`docs/handoff/next-ai-4.1.9.md` (build, allowlist, protocol, sounds).

**The job, in order:**
1. An Ukaku victim of the Steel Blade's finisher: its wings go down with the falling top half (its kagune "cut in perfect half"), instead of
   vanishing with the body. Show me a model (a GIF) first.
2. Ukaku mastered transformation as an evil laugh: hand on the face, head tilted down, laughing; the right hand down a little, a little out
   to the right of the body; a finger crack with its sound; the ukaku emerges. Show me the animation as a GIF before wiring it in.
3. Ukaku wings: more violent M1s, spread open like wings; M2s less compacted, wider. GIFs first.
4. Then ask me how the flash and the finisher play on my phone (the ragdoll's cost, the line, the pieces), and fix what I report.

**Rules:**
- No agents or subagents. One thing at a time; push every time something works (CI builds a jar per branch; I test on my phone through
  Amethyst, so it must work there).
- `python3 build.py` must pass (check its exit code) before every push.
- Every preview render uses **my skin** (`art/user_skin.png`, already wired into the preview tools). Judge every render yourself before you
  send it, and say what differs from my reference. Show me a model before building animations or combat on it. I watch the renders as you
  make them and answer mid-turn: act on it at once.
- Every Ukaku sound is Minecraft's own (no sound pack), and none shared with the Rinkaku's or Bikaku's.
- Never put a model id in the repo. No PR, no release unless I ask.
- Pass these rules on in every handoff you write.

**How I work:** blunt and fast. If an idea of mine is weak, say so straight away. Don't stop to ask; just do it.
