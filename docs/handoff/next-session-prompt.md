# The prompt for the next session (paste it as it is)

```
Continue HexGhoul (Forge 1.20.1 mod). Repo hexb1tdev/hexghoul, branch claude/relaxed-carson-4y0frw: check it out from origin and push only there (git push -u origin claude/relaxed-carson-4y0frw). Never push to main, no PR unless I ask. Answer me in English only.

FIRST read CLAUDE.md, then docs/handoff/next-ai-2.0.5.md (the newest: what to do now and how I want you to work), then docs/handoff/next-ai-2.0.1.md, then docs/handoff/next-ai-1.3.60.md (house rules), then the top sections of docs/handoff/companions-handoff.md. Version 2.0.4, the next is 2.0.5. Protocol "73".

Rules: nothing is tested in game, never claim it is. Look at your own previews honestly, zoom into every face or texture before saying it passes, and match my marks exactly. Banned names in the jar: Kaneki, Ayato, Furuta. Banned in visible text: Takatsuki, Aogiri, Yoshimura. Sounds only from the hexkagune: pack. Never alter my skins or files (only generated copies). Any new Minecraft API reference needs an entry in verify/api-allowlist.txt with [srg] or [order] evidence plus a stub; enum constants keep their names; stale entries also fail the build. Append only to ordinal-sent enums. Each fix/feature is its own version: bump build.py and patch/KagunePatch.java, release notes, README entry, new top section in companions-handoff.md, commit "2.0.x: ...", then push.

This session:
1. Other players interacting with another player's sons and daughters: a real talk page for strangers (by age and temper, gentle choices only), parents-only things stay parents-only. NOT DATEABLE: no romance with any child at any age, grown ones included, for anyone; add a test that guards it.
2. Double check the kids can inherit both kagunes: so far I've only seen Ukaku on kids, never a kid with both. The handoff says what the code does now and where a drawing bug may hide. Ask me before changing the odds.

I'm short on usage: be fast, don't run the full build locally, push as soon as each piece is done; CI builds and publishes the jar from .github/release-notes-<version>.md.
```
