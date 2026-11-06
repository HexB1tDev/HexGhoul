## 3.0.9 (HexGhoul): Family followers walk instead of teleporting

HexKagune 3.0.9 for Forge 1.20.1. Not tested in game.

Removed the unconditional 24-block distance teleports in EtoBrain's follow routine and ChildBrain.follow, and the five-block/height snap in stayBeside. Those paths use navigation at any distance, so a fight, sprint or change of floor cannot trigger a distance-based jump to the player.

Homeward used a general collision query that includes other entities. Crowds and melee could count as being trapped in a wall. It now uses block collisions only, and HomeRecovery clears its observations while the NPC follows, fights, is carried, holds hands or is in a held interaction. This prevents recovery to a moving follower home or a stale wall counter firing after combat. Genuine persistent wall trapping and the existing unseen two-minute no-headway recovery remain. Intentional carrying/hand-holding, births/respawns and combat-move positioning are preserved.

HomeRecovery is pure and checked in FamilyTests: persistent vs transient wall contact, no wall recovery from crowds/distance alone, busy follower/fighter/carrier suppression, fresh observations after combat, the two-minute timeout, progress resetting it and clock rollback. Added exactly one Minecraft API entry/stub: CollisionGetter.getBlockCollisions, SRG m_186434_, verified by tools/find_srg.py against MCPConfig joined.tsrg, intermediary 1.20.1 and Yarn CollisionView.method_20812 (obfuscated clw.d, Entity/AABB -> Iterable). No packet or enum ordinals changed.

Validation: full python3 build.py completed with EXIT 0 on Java 17, including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 88 unchanged. Not tested in game.
