#!/usr/bin/env python3
"""Yarn name -> intermediary -> official (obf) -> SRG: the [srg] evidence verify/api-allowlist.txt asks for (7.0.1).

Needs, next to this script (or in the directory given by HEX_MAPS), three files from raw.githubusercontent.com (the Forge and
Fabric mavens and Mojang's servers are blocked in the cloud sessions; GitHub raw is not):
  joined.tsrg        MinecraftForge/MCPConfig/master/versions/release/1.20.1/joined.tsrg
  intermediary.tiny  FabricMC/intermediary/master/mappings/1.20.1.tiny
  <Name>.mapping     FabricMC/yarn/1.20.1/mappings/net/minecraft/<yarn path>.mapping  (e.g. entity/mob/MobEntity.mapping)
Usage: python3 tools/srg_lookup.py <yarn .mapping file> [yarn member name ...]
Prints the class chain and each member's: yarn name, intermediary, official owner and name, and the SRG name (m_..._ / f_..._).
Yarn leaves some fields unnamed (EntityAttributes, ModelLayers): read their order in joined.tsrg ([order] evidence)."""
import re
import sys
import os

HERE = os.environ.get('HEX_MAPS') or os.path.dirname(os.path.abspath(__file__))

# intermediary.tiny v1: official -> intermediary
cls_off = {}      # intermediary class -> official
meth = {}         # (official owner, intermediary method) -> (official name, official desc)
fld = {}          # (official owner, intermediary field) -> official name
for line in open(os.path.join(HERE, 'intermediary.tiny')):
    p = line.rstrip('\n').split('\t')
    if p[0] == 'CLASS':
        cls_off[p[2]] = p[1]
    elif p[0] == 'METHOD':
        meth[(p[1], p[4])] = (p[3], p[2])
    elif p[0] == 'FIELD':
        fld[(p[1], p[4])] = p[3]

# joined.tsrg (tsrg2 obf srg id)
srg_cls = {}
srg_m = {}   # (obf owner, obf name, obf desc) -> srg
srg_f = {}   # (obf owner, obf name) -> srg
owner = None
for line in open(os.path.join(HERE, 'joined.tsrg')):
    if line.startswith('tsrg2'):
        continue
    if not line.startswith('\t'):
        p = line.split()
        owner = p[0]
        srg_cls[owner] = p[1]
        continue
    if line.startswith('\t\t'):
        continue
    p = line.strip().split()
    if len(p) == 4:
        srg_m[(owner, p[0], p[1])] = p[2]
    elif len(p) == 3:
        srg_f[(owner, p[0])] = p[1]


def yarn(path):
    """Parses a yarn .mapping file: classes (nested by indent) with their fields and methods."""
    out = []
    stack = []
    for raw in open(path):
        depth = len(raw) - len(raw.lstrip('\t'))
        p = raw.strip().split()
        if not p:
            continue
        if p[0] == 'CLASS':
            stack = stack[:depth]
            inter = p[1] if depth == 0 else stack[-1][0] + '$' + p[1]
            name = p[2] if len(p) > 2 else p[1]
            named = name if depth == 0 else stack[-1][1] + '$' + name
            stack.append((inter, named))
            out.append(('CLASS', inter, named, None))
        elif p[0] in ('METHOD', 'FIELD'):
            stack = stack[:depth]
            cur = stack[-1]
            if p[0] == 'METHOD':
                inter, name, desc = p[1], (p[2] if len(p) > 3 else p[1]), p[-1]
            else:
                inter, name, desc = p[1], (p[2] if len(p) > 3 else p[1]), p[-1]
            out.append((p[0], cur, (inter, name, desc), None))
    return out


def main():
    path = sys.argv[1]
    wanted = set(sys.argv[2:])
    for kind, a, b, _ in yarn(path):
        if kind == 'CLASS':
            off = cls_off.get(a)
            print(f'CLASS {b}  intermediary {a}  official {off}  srg {srg_cls.get(off)}')
            continue
        inter_owner, named_owner = a
        inter, name, desc = b
        if wanted and name not in wanted:
            continue
        off_owner = cls_off.get(inter_owner)
        if kind == 'METHOD':
            o = meth.get((off_owner, inter))
            s = srg_m.get((off_owner, o[0], o[1])) if o else None
            print(f'  M {named_owner}.{name} {desc}  {inter}  official {off_owner}.{o}  srg {s}')
        else:
            o = fld.get((off_owner, inter))
            s = srg_f.get((off_owner, o)) if o else None
            print(f'  F {named_owner}.{name} {desc}  {inter}  official {off_owner}.{o}  srg {s}')


main()
