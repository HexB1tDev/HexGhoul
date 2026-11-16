## 2.0.25 (HexGhoul): Dawn lightning

HexKagune 2.0.25 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

The White Reaper now points Dawn at distant targets and fires an explosive yellow lightning ball.

- The shot has a 12-tick aim, travels 1.5 blocks per tick, and can fly up to 32 blocks. It detonates on a body or solid surface.
- Direct hits deal 26 raw damage. The blast falls off to 10.4 at 2.5 blocks, pushes exposed targets away, and leaves terrain intact. No fire.
- Protocol 76 carries the aim and lightning effects. Dawn remains encounter-only pending a player acquisition decision.
- Targeted Java compilation and the Agent and Quinque suites passed. CI builds and publishes the release jar.
