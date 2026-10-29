"""Prove every line of awakening/mappings.srg against public Minecraft 1.20.1 mapping data.

For each mapped member this checks, independently:
  * MCPConfig 1.20.1 (Forge's SRG source): the SRG id exists on the declaring class with exactly the
    expected descriptor (method) or exists at all (field).
  * Fabric intermediary + Yarn 1.20.1: the same obfuscated member carries the recorded Yarn name,
    which is a second, human-readable identification of what the member is.

Needs network access to raw.githubusercontent.com. Downloads are cached in build/mapping-cache.
Usage: python3 tools/check_mappings.py
"""
from pathlib import Path
import re
import struct
import sys
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parent.parent
TABLE = ROOT / "awakening" / "mappings.srg"
CACHE = ROOT / "build" / "mapping-cache"
MCPCONFIG = "https://raw.githubusercontent.com/MinecraftForge/MCPConfig/master/versions/release/1.20.1/joined.tsrg"
INTERMEDIARY = "https://raw.githubusercontent.com/FabricMC/intermediary/master/mappings/1.20.1.tiny"
YARN = "https://raw.githubusercontent.com/FabricMC/yarn/1.20.1/mappings/{}.mapping"


def fetch(url, name):
    CACHE.mkdir(parents=True, exist_ok=True)
    path = CACHE / name
    if not path.exists():
        with urllib.request.urlopen(url, timeout=60) as response:
            path.write_bytes(response.read())
    return path.read_text(encoding="utf-8")


def parse_tsrg(text):
    classes = {}
    current = None
    for line in text.splitlines():
        if line.startswith("tsrg2") or not line.strip():
            continue
        if not line.startswith("\t"):
            current = {"fields": {}, "methods": {}}
            classes[line.split()[0]] = current
        elif not line.startswith("\t\t"):
            parts = line.split()
            if len(parts) == 3:
                current["fields"][parts[1]] = parts[0]
            elif len(parts) == 4:
                current["methods"].setdefault(parts[2], []).append((parts[0], parts[1]))
    return classes


def parse_intermediary(text):
    classes, fields, methods = {}, {}, {}
    for line in text.splitlines():
        parts = line.split("\t")
        if parts[0] == "CLASS":
            classes[parts[2]] = parts[1]
        elif parts[0] == "FIELD":
            fields[(parts[1], parts[3])] = parts[4]
        elif parts[0] == "METHOD":
            methods[(parts[1], parts[3], parts[2])] = parts[4]
    return classes, fields, methods


def parse_yarn(text):
    """intermediary class -> yarn class, and (intermediary class, intermediary member) -> yarn name."""
    classes, members = {}, {}
    stack = []
    for line in text.splitlines():
        depth = len(line) - len(line.lstrip("\t"))
        parts = line.split()
        if not parts:
            continue
        if parts[0] == "CLASS":
            stack = stack[:depth]
            inter = parts[1] if depth == 0 or "/" in parts[1] else stack[-1][0] + "$" + parts[1]
            named = parts[2] if len(parts) > 2 else inter
            if depth > 0 and len(parts) > 2 and "/" not in named:
                named = stack[-1][1] + "$" + named
            stack.append((inter, named))
            classes[named] = inter
        elif parts[0] in ("METHOD", "FIELD") and stack and depth == len(stack):
            named = parts[2] if len(parts) == 4 else parts[1]
            members[(stack[-1][0], parts[1])] = named
    return classes, members


def jar_references(path):
    """(name, descriptor) of every member reference in a JAR, read straight from the constant pools."""
    found = set()
    with zipfile.ZipFile(path) as jar:
        for entry in jar.namelist():
            if not entry.endswith(".class"):
                continue
            data = jar.read(entry)
            count = struct.unpack(">H", data[8:10])[0]
            utf8, name_types = {}, []
            offset, index = 10, 1
            while index < count:
                tag = data[offset]
                if tag == 1:
                    length = struct.unpack(">H", data[offset + 1:offset + 3])[0]
                    utf8[index] = data[offset + 3:offset + 3 + length].decode("utf-8", "replace")
                    offset += 3 + length
                elif tag in (3, 4, 9, 10, 11, 12, 17, 18):
                    if tag == 12:
                        name_types.append(struct.unpack(">HH", data[offset + 1:offset + 5]))
                    offset += 5
                elif tag in (5, 6):
                    offset += 9
                    index += 1
                elif tag in (7, 8, 16, 19, 20):
                    offset += 3
                elif tag == 15:
                    offset += 4
                else:
                    raise ValueError(f"constant pool tag {tag} in {entry}")
                index += 1
            for name, descriptor in name_types:
                found.add((utf8[name], utf8[descriptor]))
    return found


def main():
    original = jar_references(ROOT / "input" / "hexkagune-1.0.0.jar")
    tsrg = parse_tsrg(fetch(MCPCONFIG, "joined.tsrg"))
    inter_classes, inter_fields, inter_methods = parse_intermediary(fetch(INTERMEDIARY, "intermediary-1.20.1.tiny"))
    obf_of_intermediary = inter_classes
    yarn_files = {}
    official_to_obf = {}
    official_to_intermediary = {}
    entries = []
    for number, raw in enumerate(TABLE.read_text().splitlines(), 1):
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        parts = line.split()
        if parts[0] == "C":
            official, yarn_class = parts[1], parts[2]
            top = yarn_class.split("$")[0]
            if top not in yarn_files:
                yarn_files[top] = parse_yarn(fetch(YARN.format(top), top.replace("/", "_") + ".mapping"))
            classes, _ = yarn_files[top]
            inter = classes.get(yarn_class)
            if inter is None or inter not in obf_of_intermediary:
                sys.exit(f"line {number}: Yarn class {yarn_class} not found")
            obf = obf_of_intermediary[inter]
            if obf not in tsrg:
                sys.exit(f"line {number}: obfuscated class {obf} for {official} missing from MCPConfig")
            official_to_obf[official] = obf
            official_to_intermediary[official] = (inter, top)
        else:
            entries.append((number, parts))

    def to_obf(descriptor):
        def replace(match):
            name = match.group(1)
            if name.startswith("net/minecraft/") or name.startswith("com/mojang/blaze3d/") or name.startswith("com/mojang/math/"):
                if name not in official_to_obf:
                    raise KeyError(name)
                return "L" + official_to_obf[name] + ";"
            return match.group(0)
        return re.sub(r"L([^;]+);", replace, descriptor)

    failures = []
    for number, (kind, owner, name, descriptor, srg, declaring, yarn_name) in entries:
        if declaring not in official_to_obf:
            failures.append(f"line {number}: declaring class {declaring} has no C line")
            continue
        obf_class = official_to_obf[declaring]
        inter_class, top = official_to_intermediary[declaring]
        _, yarn_members = yarn_files[top]
        if kind == "M":
            found = tsrg[obf_class]["methods"].get(srg)
            if not found:
                failures.append(f"line {number}: {srg} is not a method of {declaring}")
                continue
            try:
                expected = to_obf(descriptor)
            except KeyError as missing:
                failures.append(f"line {number}: descriptor class {missing} has no C line")
                continue
            match = [obf_name for obf_name, obf_descriptor in found if obf_descriptor == expected]
            if not match:
                failures.append(f"line {number}: {srg} descriptors are {[d for _, d in found]}, table says {descriptor} ({expected})")
                continue
            obf_name = match[0]
            member = inter_methods.get((obf_class, obf_name, expected))
        else:
            obf_name = tsrg[obf_class]["fields"].get(srg)
            if obf_name is None:
                failures.append(f"line {number}: {srg} is not a field of {declaring}")
                continue
            member = inter_fields.get((obf_class, obf_name))
        if yarn_name == "-":
            # Declared by a supertype that Yarn maps elsewhere: require proof that the original,
            # working JAR already calls exactly this SRG member.
            if (srg, descriptor) not in original:
                failures.append(f"line {number}: {srg}{descriptor} is not referenced by the original JAR")
            continue
        actual = yarn_members.get((inter_class, member), member) if member else None
        if actual != yarn_name:
            failures.append(f"line {number}: {srg} ({owner}.{name}) is Yarn '{actual}', table says '{yarn_name}'")
    if failures:
        print("\n".join(failures))
        sys.exit(f"{len(failures)} mapping problems")
    print(f"Verified {len(entries)} SRG mappings against MCPConfig 1.20.1 and Yarn 1.20.1.")


if __name__ == "__main__":
    main()
