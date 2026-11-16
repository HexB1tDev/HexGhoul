#!/usr/bin/env python3
"""Quick loop (about 25 s) between full builds: compile the stubs and src/main/java, assemble a jar with the last full build's Dragon
classes, run the API verifier and the name check, then any test suites named.

Run `python3 build.py` once first (it makes build/base.jar, build/patch-classes, build/awakening-srg). Then:

    python3 tools/quick_check.py                         # compile + verify
    python3 tools/quick_check.py FamilyTests EtoTests    # ... and these suites (com.hex.kagune.control.<name>)
    python3 tools/quick_check.py --noverify QuestTests   # compile + suites only

It never touches build/ itself (it works in build/quick). The full `python3 build.py` is still what must end with EXIT 0.
"""
import os
import shutil
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
QUICK = REPO / "build" / "quick"
os.environ["HEXKAGUNE_BUILD_DIR"] = str(QUICK)
sys.path.insert(0, str(REPO))
os.chdir(REPO)
import build as b  # noqa: E402  (after the build dir is chosen)

QUICK.mkdir(parents=True, exist_ok=True)
for need in ("base.jar", "patch-classes", "awakening-srg"):
    if not (REPO / "build" / need).exists():
        sys.exit(f"build/{need} is missing: run python3 build.py once first")
if not b.BASE.exists():
    shutil.copyfile(REPO / "build/base.jar", b.BASE)
if not b.TOOLS.exists():
    shutil.copytree(REPO / "build/patch-classes", b.TOOLS)
for d in (b.STUBS, b.CLASSES, b.TEST_CLASSES):
    b.clean(d)
b.javac("--release", "17", "-d", b.STUBS, *b.sources("stubs"))
b.javac("--release", "17", "-Xlint:-options", "-cp", f"{b.BASE}{os.pathsep}{b.STUBS}", "-d", b.CLASSES, *b.sources("src/main/java"))
dragon = REPO / "build/awakening-srg"
for p in dragon.rglob("*.class"):
    t = b.CLASSES / p.relative_to(dragon)
    t.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(p, t)
b.assemble()
if "--noverify" not in sys.argv:
    b.run("java", *b.EXPORTS, "-cp", b.TOOLS, "KaguneVerify", b.OUTPUT, b.INPUT, REPO / "verify" / "api-allowlist.txt", b.STUBS)
    b.check_names()
suites = [a for a in sys.argv[1:] if not a.startswith("--")]
if suites:
    b.javac("--release", "17", "-cp", os.pathsep.join(map(str, (b.CLASSES, b.BASE, b.STUBS))), "-d", b.TEST_CLASSES, *b.sources("src/test/java"))
    for s in suites:
        b.run("java", "-ea", "-cp", os.pathsep.join(map(str, (b.TEST_CLASSES, b.CLASSES, b.BASE))), "com.hex.kagune.control." + s)
print("QUICK OK")
