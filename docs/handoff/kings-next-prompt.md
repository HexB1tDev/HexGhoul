# Prompt for the next session (paste this as the first message)

You're continuing **HexKagune**, a Forge 1.20.1 mod: a bytecode-patched original jar plus an expansion in
`src/main/java/com/hex/kagune/control/` and the `awakening/` package. The repo is **`21M4rco/HexKagunesv2`**, branch **`main`**.
Every push to `main` builds and publishes release `v<VERSION>`; I test that jar in game. Don't open PRs unless I ask.

**First read `docs/handoff/kings.md` in full.** It covers how I work, the build and test commands, the coordinate spaces, where
the War and Peace code lives, what 1.0.17 contains, and my latest complaints, quoted with the two screenshots next to it.

How I want you to work:
- Be flirty, witty, casual and blunt. Tell me straight when an idea is weak, and stress-test everything.
- Be fast. Release notes are one line per item. When I say push a testing build, push one right away.
- Never tell me something works in game when you've only compiled or previewed it.
- Every update bumps the version in `build.py` and `patch/KagunePatch.java`, bumps `PROTOCOL` when the network meaning
  changes, and adds `.github/release-notes-<ver>.md` plus a README line.
- Render previews with `tools/art-preview.sh`, look at them yourself and send them to me.

**Your first job: Peace.** I hated the 1.0.16/1.0.17 Peace (`docs/handoff/peace-ingame-1.0.17-rejected.png`).
- Bring back the **original model** (`docs/handoff/peace-original-wanted.png`, the pre-1.0.16 `PeaceModel` at commit `1ea5982`,
  after `tools/kakuja/reference/peace.webp`): the huge crimson wings and the giant cross-shaped blades with glowing red cores.
- The main blades: **six** (check with me in one line if you think I mean the original eight). They point **forward at all
  times unless attacking**.
- **The kakugan (red eyes) must be on** in Peace form.
- It must feel **alive**: wings breathing and writhing, blades drifting and turning, secondary motion when I move. Rebuild
  every frame, not once a tick.
- Re-choreograph Peace's M1 and its six abilities onto those blades. Keep the server's cuts (`PeaceKit`, which cuts along
  `PeaceMotion`'s paths) matching what's drawn, and keep the transformation (`EMERGE`, `KingGrowth`) working.
- The kagune must **never clip through my body**.

Show me front, side and back previews, then push a testing build. After that, ask me how War's new cloth and the two
transformations from 1.0.17 played, and fix what I report.
