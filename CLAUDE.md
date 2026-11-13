# HexGhoul: rules for every session

Read the newest `docs/handoff/next-ai-*.md` first: it holds the house rules (versions, builds, banned names, sounds, the API allowlist,
never altering the owner's files) and what to do next. Answer the owner in English.

(The old RULE 1, stopping at 97% usage, was removed by the owner after 1.3.78: "Yes take that shit out.")

RULE: judge your own renders. The owner (after 4.1.16, quoted exactly): "we should make this a rule for every AI. So on the AI, on the next
handoff, always specify that the AI is to evaluate the renders that it generates, and it does not give up until it's a perfect match and
it's a perfect combat system. Like, the animations always have to be top tier, no excuses whatsoever." So: every pose, animation, model
or texture you change, you render (the preview tools in `tools/`), look at it yourself, from the front, the side and in first person
where it applies, and fix it, render again, until it is right. Passing tests is not enough: "you keep passing them, but they're just the
most basic garbage you can have, which I don't approve of."

RULE: a push is done only when GitHub says so. After every push, check the branch's "Build HexKagune" run (and "Release HexKagune" when
release notes changed) ends green and the version is on the Releases page. A local `python3 build.py` EXIT 0 is not enough: 4.1.73 passed
locally and failed on the runner (no numpy or Pillow there), so it went unreleased while its handoff said pushed. Python packages any check
needs go in `tools/requirements.txt`, which both workflows install.

RULE: prove the tests. Every rule you add, break it on purpose and see a test fail: `tools/sabotage.py <list.json>` does it (a list like
`docs/handoff/sabotage-4.1.78.json`); `tools/fast_tests.sh <Suite>` is the quick loop between full builds. A MISS is a hole in the tests,
fixed before the push. Holes found so far: `indexOf(x, from) - from < n` passes when x is gone (-1); a size checked against the very
constant broken; a crashed suite prints no "FAILED".
