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
