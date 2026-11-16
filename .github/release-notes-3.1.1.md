## 3.1.1 (HexGhoul): Limit weapon carry fixes to heavy sword, hammer and whip

HexKagune 3.1.1 for Forge 1.20.1. Not tested in game.

The animation fix is limited to the three reported weapons: Arm.BONE_SWORD (Bone Greatsword), Arm.FLESH_HAMMER (Flesh Hammer) and Arm.WHIP (Tendril Whip). Their moving carries release the planted stance's legs and preserve the underlying walk/run, with restrained stride motion; the whip's free arm moves and its tendril rests in a raised anchored curl. Their combat poses are retained.

The crystal spear uses its original stance and running carry again. Removed the extra spear running-angle adjustment from 3.1.0. Verified by source comparison with 3.0.9 that the legacy quinque pose path and every existing AgentPose method are unchanged, and that the extracted AgentPose.withWeapon composition is identical to the original AgentClient.take body. No dependency versions, packet fields or enum ordinals changed. Protocol 88 remains.

Focused WeaponCarryTests (2785), AgentTests (48281) and ControlTests (19330) plus API verification passed. Added a regression comparing every crystal-spear pose channel with its original stance across movement speeds/phases. The reported weapons retain gait-transform, classic/slim grip, regular/masterwork floor-clearance and attack-footwork coverage. Reviewed the updated three-weapon offline sheet (stiff NPC limbs, real meshes): docs/handoff/weapon-carry-3.1.1.png, reproducible with tools/WeaponCarryPreview.java. This is an offline geometry check, not a game test.

This build also contains 3.0.8's jealousy/marriage preservation and recovery and 3.0.9's family catch-up navigation/block-only recovery fixes.

Validation: full python3 build.py completed with EXIT 0 on Java 17, including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 88 unchanged. Not tested in game.
