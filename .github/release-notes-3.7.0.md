## 3.7.0 (HexGhoul): Fix: The Walker's arms never swing

HexKagune 3.7.0 for Forge 1.20.1. Not tested in game.

Your words: "the arms should not swing at any point (they currently do)"

- **In The Walker your arms now hang still at your sides**, a little out, elbows slightly bent. They sway only as slightly as your hanging legs do.
- **No arm swing at all:** none of Minecraft's own swing (walking, attacking, the bob), and no other pose moves them while The Walker carries you.
- **Before:** only your legs were held still; your arms swung as you walked.
- **Unchanged:** everything else about The Walker in this version. Your other words on it (higher, any terrain, tap K) are the next version; its preview was sent.
- **My guesses:** how far out the arms hang (0.1) and the elbow bend (0.15).
- Network: protocol 103 (unchanged).
- Tests: BikakuWormTests `walker()`: whatever was posing your arms before, in The Walker they are all the pose's (no swing). They hang at your sides and barely sway, at every moment over 20 seconds. Also the full suite.

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 103. Not tested in game.
