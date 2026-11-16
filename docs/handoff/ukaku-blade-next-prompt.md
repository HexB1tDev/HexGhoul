# Prompt for the next session

Paste everything below the line into a new session on the HexGhoul repository.

---

You're continuing HexKagune (HexGhoul), a Forge 1.20.1 mod built by bytecode-patching the original jar plus the expansion in
`src/main/java/com/hex/kagune/control/`.

**First, always work on the VERY LATEST code**: `git fetch`, find the remote branch with the newest commit
(`git for-each-ref --sort=-committerdate refs/remotes/origin --format='%(committerdate:iso) %(refname:short) %(subject)' | head`) and merge
it into your session branch before touching anything. `main` is stale (1.3.0). When this was written the newest was
`claude/great-galileo-cdhewb` (4.2.8 plus the Ukaku blade work).

**Read first:** `docs/handoff/ukaku-blade-handoff.md` (where the work stopped, my words, the plans), then the rules in
`docs/handoff/next-ai-4.1.9.md` (build, allowlist, protocol, sounds).

**The job, in order:**
1. Render `tools/art-preview.sh <out> full-form` (the final kakuja in full: kakugan, my mask, chest, tentacles, fire, the Steel Blade), look
   at it honestly, send it to me.
2. The Steel Blade's flash dash: Ctrl + right click, a high speed dash that leaves only afterimages behind.
3. The blade's finisher: a kill stuns them 2 seconds while a shallow wound grows round their waist; when it meets its start the top half
   slides off and falls face forward, blood everywhere; their kagune is cut at the waist too, a bikaku or rinkaku shatters.
4. Ukaku mastered transformation as an evil laugh: hand on the face, head tilted down, laughing; the right hand down a little, a little out
   to the right of the body; a finger crack with its sound; the ukaku emerges.
5. Ukaku wings: more violent M1s, spread open like wings; M2s less compacted, wider.

**Rules:**
- No agents or subagents. One thing at a time; push every time something works (CI builds a jar per branch; I test on my phone through
  Amethyst, so it must work there).
- `python3 build.py` must pass (check its exit code) before every push.
- Every preview render uses **my skin** (`art/user_skin.png`, already wired into the preview tools). Judge every render yourself before you
  send it, and say what differs from my reference. Show me a model before building animations or combat on it.
- Every Ukaku sound is Minecraft's own (no sound pack).
- Never put a model id in the repo. No PR, no release unless I ask.
- Pass these rules on in every handoff you write.

**How I work:** blunt and fast. If an idea of mine is weak, say so straight away. Don't stop to ask; just do it.
