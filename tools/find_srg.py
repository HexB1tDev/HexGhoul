"""Look up the SRG id of a Minecraft 1.20.1 member by its Yarn name, to write new lines of awakening/mappings.srg.

Usage: python3 tools/find_srg.py <yarn class> [member substring]
  python3 tools/find_srg.py net/minecraft/registry/tag/BlockTags LOGS
  python3 tools/find_srg.py net/minecraft/block/AbstractBlock\\$AbstractBlockState isIn

Prints every matching member as: kind, Yarn name, SRG id, obfuscated descriptor, Yarn descriptor.
Uses the same downloads (and cache) as tools/check_mappings.py. The result is only a lead: the line
still has to pass check_mappings.py, and the Mojang name in the table is still yours to get right.
"""
import re
import sys

from check_mappings import INTERMEDIARY, MCPCONFIG, YARN, fetch, parse_intermediary, parse_tsrg


def main():
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    yarn_class = sys.argv[1]
    needle = sys.argv[2] if len(sys.argv) > 2 else ""
    top = yarn_class.split("$")[0]
    tsrg = parse_tsrg(fetch(MCPCONFIG, "joined.tsrg"))
    inter_classes, inter_fields, inter_methods = parse_intermediary(fetch(INTERMEDIARY, "intermediary-1.20.1.tiny"))
    text = fetch(YARN.format(top), top.replace("/", "_") + ".mapping")
    # Yarn class -> intermediary class, and per class its members with Yarn names and Yarn descriptors.
    stack, inter_of, members = [], {}, []
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
            inter_of[named] = inter
        elif parts[0] in ("METHOD", "FIELD") and stack and depth == len(stack):
            named = parts[2] if len(parts) == 4 else parts[1]
            descriptor = parts[3] if len(parts) == 4 else parts[2]
            members.append((stack[-1][1], parts[0], parts[1], named, descriptor))
    inter = inter_of.get(yarn_class)
    if inter is None:
        sys.exit(f"Yarn class {yarn_class} not found in {top}")
    obf = inter_classes.get(inter)
    print(f"{yarn_class}: intermediary {inter}, obfuscated {obf}")
    srg_methods = {(name, desc): srg for srg, entries in tsrg[obf]["methods"].items() for name, desc in entries}
    srg_fields = {name: srg for srg, name in tsrg[obf]["fields"].items()}
    # Intermediary member -> obfuscated name (and descriptor) on this class.
    obf_method = {member: (name, desc) for (owner, name, desc), member in inter_methods.items() if owner == obf}
    obf_field = {member: name for (owner, name), member in inter_fields.items() if owner == obf}
    for owner, kind, inter_member, named, descriptor in members:
        if owner != yarn_class or needle.lower() not in named.lower():
            continue
        if kind == "METHOD":
            found = obf_method.get(inter_member)
            srg = srg_methods.get(found) if found else None
            print(f"M {named} {srg} {found[1] if found else '?'} {descriptor}")
        else:
            name = obf_field.get(inter_member)
            print(f"F {named} {srg_fields.get(name)} {descriptor}")


if __name__ == "__main__":
    sys.path.insert(0, str(__import__("pathlib").Path(__file__).resolve().parent))
    main()
