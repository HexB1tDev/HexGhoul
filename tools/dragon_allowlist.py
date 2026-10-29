"""Allowlist lines, with their evidence, for the Dragon's (control/awakening) external references.

The Dragon is remapped from Mojang names through awakening/mappings.srg, whose every line tools/check_mappings.py proves
against MCPConfig and Yarn 1.20.1. KaguneVerify then wants each reference the original JAR doesn't make listed in
verify/api-allowlist.txt. Feed this the verifier's complaints and it prints the lines to add:

    python3 build.py 2> errors.txt; python3 tools/dragon_allowlist.py errors.txt

Minecraft classes and Forge members it doesn't know are refused, never guessed: add them to the tables below after checking
the Forge 1.20.1 source (raw.githubusercontent.com/MinecraftForge/MinecraftForge/1.20.1/...) or Yarn 1.20.1.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

# Minecraft classes the Dragon names (instanceof, constructors, descriptors) -> Yarn 1.20.1. Every one ran in game in the
# Dragon of HexKagunesv2 1.0.0-1.0.3 (built from the same source against the same 1.0.0 JAR).
YARN_CLASSES = {
    "net/minecraft/world/level/block/piston/PistonHeadBlock": "net/minecraft/block/PistonHeadBlock",
    "net/minecraft/world/level/block/piston/PistonBaseBlock": "net/minecraft/block/PistonBlock",
    "net/minecraft/world/level/block/TntBlock": "net/minecraft/block/TntBlock",
    "net/minecraft/world/level/block/ScaffoldingBlock": "net/minecraft/block/ScaffoldingBlock",
    "net/minecraft/world/level/block/RotatedPillarBlock": "net/minecraft/block/PillarBlock",
    "net/minecraft/world/level/block/RedstoneTorchBlock": "net/minecraft/block/RedstoneTorchBlock",
    "net/minecraft/world/level/block/RedStoneWireBlock": "net/minecraft/block/RedstoneWireBlock",
    "net/minecraft/world/level/block/ObserverBlock": "net/minecraft/block/ObserverBlock",
    "net/minecraft/world/level/block/NetherPortalBlock": "net/minecraft/block/NetherPortalBlock",
    "net/minecraft/world/level/block/LeverBlock": "net/minecraft/block/LeverBlock",
    "net/minecraft/world/level/block/LeavesBlock": "net/minecraft/block/LeavesBlock",
    "net/minecraft/world/level/block/FallingBlock": "net/minecraft/block/FallingBlock",
    "net/minecraft/world/level/block/DoorBlock": "net/minecraft/block/DoorBlock",
    "net/minecraft/world/level/block/DiodeBlock": "net/minecraft/block/AbstractRedstoneGateBlock",
    "net/minecraft/world/level/block/ButtonBlock": "net/minecraft/block/ButtonBlock",
    "net/minecraft/world/level/block/BaseRailBlock": "net/minecraft/block/AbstractRailBlock",
    "net/minecraft/world/level/block/BasePressurePlateBlock": "net/minecraft/block/AbstractPressurePlateBlock",
    "net/minecraft/world/level/block/BaseFireBlock": "net/minecraft/block/AbstractFireBlock",
    "net/minecraft/world/entity/decoration/HangingEntity": "net/minecraft/entity/decoration/AbstractDecorationEntity",
    "net/minecraft/world/entity/decoration/ArmorStand": "net/minecraft/entity/decoration/ArmorStandEntity",
    "net/minecraft/network/protocol/game/ClientboundSetTitlesAnimationPacket": "net/minecraft/network/packet/s2c/play/TitleFadeS2CPacket",
    "net/minecraft/network/protocol/game/ClientboundSetTitleTextPacket": "net/minecraft/network/packet/s2c/play/TitleS2CPacket",
    "net/minecraft/network/protocol/game/ClientboundSetSubtitleTextPacket": "net/minecraft/network/packet/s2c/play/SubtitleS2CPacket",
    "net/minecraft/client/gui/screens/PauseScreen": "net/minecraft/client/gui/screen/GameMenuScreen",
    "net/minecraft/client/gui/screens/ChatScreen": "net/minecraft/client/gui/screen/ChatScreen",
    "net/minecraft/world/level/LevelAccessor": "net/minecraft/world/WorldAccess",
    "net/minecraft/resources/ResourceLocation": "net/minecraft/util/Identifier",
    "net/minecraft/client/CameraType": "net/minecraft/client/option/Perspective",
}

# Forge classes (checked in the MinecraftForge 1.20.1 sources) -> the file that declares them.
FORGE_FILES = {
    "net/minecraftforge/fml/event/lifecycle/FMLCommonSetupEvent": "src/fmlcommon/java/.../fml/event/lifecycle/FMLCommonSetupEvent.java",
    "net/minecraftforge/event/level/LevelEvent$Unload": "event/level/LevelEvent.java (Unload extends LevelEvent)",
    "net/minecraftforge/event/level/BlockEvent$BreakEvent": "event/level/BlockEvent.java (BreakEvent extends BlockEvent)",
    "net/minecraftforge/event/entity/player/PlayerInteractEvent$RightClickItem": "event/entity/player/PlayerInteractEvent.java (RightClickItem)",
    "net/minecraftforge/event/entity/player/PlayerInteractEvent$RightClickBlock": "event/entity/player/PlayerInteractEvent.java (RightClickBlock)",
    "net/minecraftforge/event/entity/player/PlayerInteractEvent$LeftClickBlock": "event/entity/player/PlayerInteractEvent.java (LeftClickBlock)",
    "net/minecraftforge/event/entity/player/PlayerInteractEvent$EntityInteractSpecific": "event/entity/player/PlayerInteractEvent.java (EntityInteractSpecific)",
    "net/minecraftforge/event/entity/EntityTravelToDimensionEvent": "event/entity/EntityTravelToDimensionEvent.java (extends EntityEvent, @Cancelable)",
    "net/minecraftforge/event/TickEvent$ServerTickEvent": "event/TickEvent.java (ServerTickEvent extends TickEvent)",
    "net/minecraftforge/common/util/FakePlayer": "common/util/FakePlayer.java (extends ServerPlayer)",
    "net/minecraftforge/client/event/ViewportEvent$ComputeFov": "client/event/ViewportEvent.java (ComputeFov)",
    "net/minecraftforge/client/event/ViewportEvent$ComputeCameraAngles": "client/event/ViewportEvent.java (ComputeCameraAngles)",
    "net/minecraftforge/client/event/RenderGuiEvent$Post": "client/event/RenderGuiEvent.java (Post extends RenderGuiEvent)",
}

# Forge members, as declared in those sources (inherited ones name their declaring class).
FORGE_MEMBERS = {
    "getEntity": "PlayerEvent.getEntity / EntityEvent.getEntity",
    "setCanceled": "eventbus Event.setCanceled (the event is @Cancelable)",
    "getPlayer": "BlockEvent.BreakEvent.getPlayer",
    "getLevel": "LevelEvent.getLevel",
    "getGuiGraphics": "RenderGuiEvent.getGuiGraphics",
    "getWindow": "RenderGuiEvent.getWindow",
    "getYaw": "ViewportEvent.ComputeCameraAngles.getYaw",
    "setYaw": "ViewportEvent.ComputeCameraAngles.setYaw",
    "getFOV": "ViewportEvent.ComputeFov.getFOV",
    "setFOV": "ViewportEvent.ComputeFov.setFOV",
    "phase": "TickEvent.phase (public final Phase)",
}


def srg_table():
    table = {}
    for line in (ROOT / "awakening" / "mappings.srg").read_text().splitlines():
        parts = line.split()
        if parts and parts[0] in ("M", "F") and len(parts) >= 7:
            table[parts[4]] = (parts[2], parts[6])
    return table


def main():
    table = srg_table()
    lines, refused = set(), []
    for raw in Path(sys.argv[1]).read_text().splitlines():
        m = re.match(r"\s+unverified (class|M|F) (\S+)(?: (\S+) (\S+))?", raw)
        if not m:
            continue
        kind, owner, name, desc = m.groups()
        if kind == "class":
            if owner in YARN_CLASSES:
                lines.add(f"C {owner}  # [srg] yarn {YARN_CLASSES[owner]}; used in game by the 1.0.0-1.0.3 Dragon")
            elif owner in FORGE_FILES:
                lines.add(f"C {owner}  # [forge] {FORGE_FILES[owner]}")
            else:
                refused.append(raw.strip())
            continue
        entry = f"{kind} {owner} {name} {desc}"
        if owner.startswith("net/minecraftforge/") and name in FORGE_MEMBERS:
            lines.add(f"{entry}  # [forge] {FORGE_MEMBERS[name]}")
        elif owner.startswith("com/mojang/brigadier/") and name == "requires":
            lines.add(f"{entry}  # [lib] Brigadier 1.1.8 ArgumentBuilder.requires (inherited by RequiredArgumentBuilder)")
        elif re.fullmatch(r"[mf]_\d+_", name) and name in table:
            mojang, yarn = table[name]
            proof = "proven by the original JAR's own reference" if yarn == "-" else f"yarn {yarn}"
            lines.add(f"{entry}  # [srg] {mojang} ({proof}; awakening/mappings.srg, tools/check_mappings.py)")
        elif kind == "F" and re.fullmatch(r"[A-Z][A-Z0-9_]*", name) and owner in YARN_CLASSES:
            lines.add(f"{entry}  # [srg] enum constants keep their names (as the original's HitResult$Type.BLOCK); yarn {YARN_CLASSES[owner]}.{name}")
        elif name == "<init>" and owner in YARN_CLASSES:
            lines.add(f"{entry}  # [srg] constructor of yarn {YARN_CLASSES[owner]}; used in game by the 1.0.0-1.0.3 Dragon")
        else:
            refused.append(raw.strip())
    for line in sorted(lines):
        print(line)
    if refused:
        print("\n".join("REFUSED " + r for r in refused), file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
