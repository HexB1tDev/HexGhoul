## 3.1.2 (HexGhoul): Add a rebindable briefcase key for every quinque

HexKagune 3.1.2 for Forge 1.20.1. Not tested in game.

Added Fold quinque into briefcase under the HexKagune controls, default Y (unused by the mod's existing key mappings). The key works on every open quinque in the main hand, old or new, and does nothing to an already closed case or unrelated item. Existing right-click and Shift + right-click abilities retain their bindings. The item tooltip displays the assigned fold key.

The server validates the player and held item. Older quinques use their existing stack-turning operation; newer kagune-made weapons now have a manual closing operation which copies the full stack tag (including custom name, enchantments, durability and masterwork), records the closing animation time and cancels pending strikes/queued moves/bind visuals. A weapon still opening must finish opening first. Folding an older launcher/Dawn also cancels its pending shot. Inventory-based automatic folding remains available.

C2SArmAction appends FOLD=3 after unchanged STRIKE=0, FIRST=1 and SECOND=2. No message ids or fields changed. Protocol is bumped from 88 to 89 for the new action, so client and server must both use the updated mod. No new Minecraft/Forge API calls were needed. Focused QuinqueTests (918), ControlTests (19330) and API verification passed; reviewed tag-copying, server validation, idempotence and animation/queued-hit cancellation paths.

Validation: full python3 build.py completed with EXIT 0 on Java 17, including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 89. Not tested in game.
