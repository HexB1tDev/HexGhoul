> **Superseded (2026-10-27):** the final kakujas (War and Peace, HexKagunesv2 1.0.13 to 1.0.17) are handed over in `docs/handoff/kings.md`, and the prompt to paste is `docs/handoff/kings-next-prompt.md`.

# Prompt for the next session

Paste everything below the line into a new session on `21M4rco/HexKagunes`.

---

You're continuing HexKagune: a Forge 1.20.1 mod built by bytecode-patching the original jar, plus the expansion in
`src/main/java/com/hex/kagune/control/`. The latest release is **7.0.1**. The newest work is on branch
`claude/furuta-dna-path-replacement-wja31k`; start from it, and push only to the branch your session gives you (that one, if it
is the one you get). No PR unless I ask.

**Read first, in this order:**
1. `docs/handoff/next-session.md`: where the work stopped, the phase plan, and every SRG name already resolved for the next
   phase.
2. `docs/handoff/requests.md`: my words, verbatim and binding, especially "The progression overhaul".
3. `docs/HANDOFF.md`: the deeper reference (build rules, coordinate spaces, conventions).

**The job: finish the progression overhaul.** Phases 1 to 3 are done and released:
- every ghoul fights with the Owl DNA body in eye mode;
- Ghoul Meat;
- quests that gate the transformations, with rival ways, sides (Anteiku or the Pack), hunts and the U journal.

Next is **phase 4, ghoul NPCs**:
- They spawn in any biome, only at night, rarely, never clustered.
- Personalities: hostile ones attack on sight; neutral and friendly ones can be talked to.
- Combat is basic kagune M1 only, reusing what exists: Furuta DNA's transplant tails and string for rinkaku ghouls, Ayato's
  wings and fire shots for ukaku ghouls. No mutations, no kakuja.
- They drop Ghoul Meat.
- Wire `server/GhoulKinds` so their kills count for quests and hunts.

Then **phase 5**: right-click dialogue with choices, favours as quests, friendships, allies and enemies. Both phases have a written
plan in `docs/handoff/next-session.md`.

**Rules:**
- **No agents or subagents, ever.** One thing at a time.
- Before every push:
  - `python3 build.py` must pass; check its exit code. It patches, compiles against `stubs/`, proves every external reference and
    runs every `*Tests`.
  - A new Minecraft or Forge reference needs SRG evidence in `verify/api-allowlist.txt` and a stub. `tools/srg_lookup.py` joins
    MCPConfig `joined.tsrg`, Fabric intermediary and yarn 1.20.1, all fetchable from raw.githubusercontent.com (the mavens and
    Mojang's servers are blocked).
  - Render previews, look at them yourself, and send me the PNGs. Tools: `tools/furuta-preview.sh`, `tools/eto-preview.sh`, and
    `java -cp build/classes:build/base.jar tools/UiPreview.java <dir> [quests|furuta]`.
- Push every time something works. CI publishes a `build-<branch>` jar, and I test it on my phone through Amethyst, so anything
  you add must work there too.
- Show me a model before building animations or combat on it. For phase 4, that means a preview of a ghoul NPC with its tails
  out and one with its wings out, before any AI or combat.
- Never put a model id anywhere in the repo. Commit messages end with the Co-Authored-By and Claude-Session lines your session
  gives you.
- No player-visible text may say Eto, Takatsuki, Aogiri or Yoshimura (`EtoTests.owlDna` fails the build).
- On the Many-Eyed Kagune: never change its look unless I ask, and never use disc eyes.
- Don't overclaim that a render matches a reference: say what differs.
- Releases: push `.github/release-notes-<VERSION>.md` on the branch (bump VERSION in `build.py` and `patch/KagunePatch.java`
  first), but only when I ask.

**How I work:** I judge the visuals, and I'm blunt. If an idea of mine is weak, say so straight away and stress-test it. Keep it
witty. Use they/them for me. Don't stop to ask; just do it.
