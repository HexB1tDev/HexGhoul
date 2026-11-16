> **Standing rules (the owner, 2026-11-16):** (1) **Always work on the VERY LATEST code.** `main` is not it (it stopped at 1.3.0): before touching anything, `git fetch` and start from the remote branch with the newest commit (`git for-each-ref --sort=-committerdate refs/remotes/origin --format='%(committerdate:iso) %(refname:short) %(subject)' | head`), merged into your session branch. (2) **Every preview render uses the owner's own skin** as the reference body: `art/user_skin.png` (64x64, slim arms, eyes on face rows 5-6); `tools/PoseRender.java` puts it on every preview body without a skin of its own (`-Dhex.skin=...`, set by every `tools/*.sh`), and `ArtPreview.eyes()` fits masks and kakugan to its eyes. Never the old stand-in suit. Pass both rules on in every handoff you write.

# Prompt for the next AI (after 4.1.34)

Read CLAUDE.md, then docs/handoff/next-ai-4.1.9.md: "START HERE (after 4.1.34)" first, then "How to work". The house rules are in
next-ai-4.1.2.md and next-ai-4.0.0.md. Fast-forward your branch onto claude/sleepy-knuth-5jlwby. This is taking way too long: hurry, one
short version at a time, and show me every render in chat before you call anything done.

Next is the Small City, the whole 544 x 544 city from art/city/small-city.zip as four 272 x 272 quarters that always spawn together at one
ground height:
1. 4.1.35: apply docs/handoff/small-city-4.1.35-wip.patch, finish the cut (swap every block 1.20.1 doesn't have), render the city.
2. 4.1.36: the four quarters placed together (rarer than a vanilla village), every hexkagune:village_x and everything related deleted,
   hexkagune:small_city in their place, humans in vanilla villages.

Judge every structure, pose and screen on your own renders, not on test numbers. Run the full build to EXIT 0 before every push. Show me
every render in chat. Never claim in-game testing.
