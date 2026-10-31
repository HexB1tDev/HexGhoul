"""Patch the original HexKagune JAR, compile the kagune-control expansion and verify the result.

Pipeline (Java 17+ and Python 3, no downloads):
  1. check the SHA-256 of the supplied 1.0.0 JAR,
  2. compile and run the ASM patcher (1.0.2 changes + 1.1.0 hooks) -> build/base.jar,
  3. compile compile-only API stubs (stubs/) -> build/stubs   (never packaged),
  4. compile src/main/java against build/base.jar + stubs with --release 17 -> build/classes,
  5. assemble build/hexkagune-<version>.jar with src/main/resources (lang keys and sound events are merged),
  6. verify bytecode contracts, every external API reference, and the archive scope,
  7. prove the JAR loads and runs on a dedicated server (no client class on any path a server runs, patch/KaguneServerCheck),
  8. guard every way Minecraft calls into the mod (event listeners, the spike's and the bleeding effect's callbacks): an error
     inside is logged and the game carries on, never a kicked player or a crashed server (patch/KaguneGuard); the archive scope
     and the dedicated server are checked again on the guarded JAR,
  9. compile and run the pure-logic tests in src/test/java.
"""
from pathlib import Path
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import zipfile

ROOT = Path(__file__).resolve().parent
VERSION = "1.3.6"
INPUT = ROOT / "input" / "hexkagune-1.0.0.jar"
BUILD = Path(os.environ.get("HEXKAGUNE_BUILD_DIR", ROOT / "build"))
OUTPUT = BUILD / f"hexkagune-{VERSION}.jar"
BASE = BUILD / "base.jar"
TOOLS = BUILD / "patch-classes"
STUBS = BUILD / "stubs"
CLASSES = BUILD / "classes"
TEST_CLASSES = BUILD / "test-classes"
EXPORTS = [
    "--add-exports", "java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED",
    "--add-exports", "java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED",
    "--add-exports", "java.base/jdk.internal.org.objectweb.asm.tree.analysis=ALL-UNNAMED",
    "--add-exports", "java.base/jdk.internal.org.objectweb.asm.commons=ALL-UNNAMED",
]
# 1.0.4: the Dragon (Kakuja V2, com.hex.kagune.control.awakening) is written with Mojang names against its own stubs and
# remapped to SRG through awakening/mappings.srg (proven by tools/check_mappings.py), then packaged like the rest.
AWAKENING_STUBS = BUILD / "awakening-stubs"
AWAKENING_RAW = BUILD / "awakening-mojang"
AWAKENING_SRG = BUILD / "awakening-srg"
AWAKENING_TESTS = BUILD / "awakening-test-classes"
EXPECTED_INPUT_SHA256 = "c5ebfd47b8cdb51c949fbace9ffa9378353c4d2cf939c4ced1e5554907217d34"
LANG = "assets/hexkagune/lang/en_us.json"
SOUNDS = "assets/hexkagune/sounds.json"
# Original entries that are intentionally different in the output. Everything else must be byte-identical.
CHANGED = {
    "com/hex/kagune/gameplay/KaguneServerController.class",
    "com/hex/kagune/gameplay/KaguneRuntime$State.class",
    "com/hex/kagune/client/KaguneAuraEffects.class",
    "com/hex/kagune/client/render/TendrilChain.class",
    "com/hex/kagune/client/KaguneManifestationEffects.class",
    "com/hex/kagune/KaguneMod.class",
    "com/hex/kagune/data/KaguneProfile.class",
    "com/hex/kagune/client/render/KaguneWorldRenderer.class",
    "com/hex/kagune/client/KaguneAmbientSounds.class",
    "com/hex/kagune/client/render/KaguneSpikeRenderer.class",
    "com/hex/kagune/gameplay/KaguneForm.class",
    "com/hex/kagune/gameplay/KaguneForm$1.class",
    "com/hex/kagune/gameplay/KaguneEvents$1.class",
    "com/hex/kagune/gameplay/MutationNode.class",
    "com/hex/kagune/client/screen/KaguneScreen.class",
    "com/hex/kagune/client/KaguneEnergyHud.class",
    "META-INF/mods.toml",
    "META-INF/MANIFEST.MF",
    LANG,
    SOUNDS,
}
# New entries may only live under these prefixes.
NEW_PREFIXES = (
    "com/hex/kagune/control/",
    "assets/hexkagune/textures/",
    "assets/hexkagune/models/one_eyed_owl.bin",
    "assets/hexkagune/models/item/",  # 7.0.1: the expansion's item models (Ghoul Meat)
    "assets/hexkagune/sounds/",
    "data/hexkagune/",
)


def run(*args):
    subprocess.run([str(a) for a in args], cwd=ROOT, check=True)


def sources(directory):
    return sorted(str(p) for p in (ROOT / directory).rglob("*.java"))


def clean(path):
    if path.exists():
        shutil.rmtree(path)
    path.mkdir(parents=True)


def javac(*args):
    run("javac", "-encoding", "UTF-8", "-proc:none", "-nowarn", *args)


# 7.0.0: original entries the user asked to rename (the controls: "Ability 1".."Ability 8" and the category "HexKagune"); 1.2.2: and
# the original's sounds the user asked to replace with their own.
RENAMED = {
    LANG: {"key.categories.hexkagune", "key.hexkagune.menu", "key.hexkagune.rapid_strike", "key.hexkagune.slingshot",
           "key.hexkagune.lift", "key.hexkagune.slam", "key.hexkagune.throw", "key.hexkagune.crush", "key.hexkagune.grab",
           "key.hexkagune.guard", "key.hexkagune.cycle_tendrils"},
    # 1.2.2: every one of the original's sounds the user asked to be replaced by their own pack (tools/sfx_pack.py): each event now
    # draws from the pack's pools ("replace": true), the original's OGGs left in the jar unplayed.
    SOUNDS: {"manifest_rip", "kagune_reshape", "kagune_dismiss", "kagune_whip", "flesh_impact", "bone_crack", "bone_crush", "kagune_grab",
             "kagune_strain", "kagune_slam", "kagune_guard", "kagune_anchor", "centipede_click", "shard_launch", "kagune_idle", "crush_kill"},
}


def merged_json(name, original_bytes):
    """Original keys stay exactly as they are (but the few in RENAMED); the expansion may only add new ones."""
    original = json.loads(original_bytes.decode("utf-8"))
    additions = json.loads((ROOT / "src/main/resources" / name).read_text(encoding="utf-8"))
    renamed = RENAMED.get(name, set())
    missing = sorted(k for k in renamed if k not in original)
    if missing:
        raise RuntimeError(f"RENAMED names entries the original {name} does not have: {missing}")
    clash = sorted(k for k in additions if k in original and original[k] != additions[k] and k not in renamed)
    if clash:
        raise RuntimeError(f"Expansion keys in {name} would change original entries: {clash}")
    merged = dict(original)
    merged.update(additions)
    return (json.dumps(merged, indent=2, ensure_ascii=False) + "\n").encode("utf-8")


def assemble():
    entries = {}
    with zipfile.ZipFile(BASE) as base:
        for name in base.namelist():
            entries[name] = base.read(name)
    for path in sorted(CLASSES.rglob("*.class")):
        name = path.relative_to(CLASSES).as_posix()
        if not name.startswith("com/hex/kagune/control/"):
            raise RuntimeError(f"Expansion class outside com/hex/kagune/control: {name}")
        entries[name] = path.read_bytes()
    resources = ROOT / "src/main/resources"
    for path in sorted(p for p in resources.rglob("*") if p.is_file()):
        name = path.relative_to(resources).as_posix()
        if name in (LANG, SOUNDS):
            entries[name] = merged_json(name, entries[name])
        elif name in entries:
            raise RuntimeError(f"Resource would overwrite an original entry: {name}")
        else:
            entries[name] = path.read_bytes()
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(OUTPUT, "w", zipfile.ZIP_DEFLATED) as out:
        for name, data in entries.items():
            out.writestr(name, data)


# 1.2.5: no character of the show is named anywhere in the mod's jar (the user's ask): not in a class, a text, a resource or a file
# name. These are the only places the names are written (to be looked for); the original jar's own are renamed by KagunePatch.
# 1.3.0: the user allowed "Eto" (the Library's NPC), so her name is no longer looked for.
FORBIDDEN = re.compile(rb"Kaneki|KANEKI|kaneki|Ayato|AYATO|ayato|Furuta|FURUTA|furuta")


def check_names():
    found = []
    with zipfile.ZipFile(OUTPUT) as jar:
        for name in jar.namelist():
            looked = [(name + " (its name)", name.encode("utf-8"))]
            if not name.endswith((".png", ".ogg", ".bin")):
                looked.append((name, jar.read(name)))
            for where, data in looked:
                hit = FORBIDDEN.search(data)
                if hit:
                    found.append(f"{where}: {hit.group(0).decode('utf-8', 'replace')}")
    if found:
        raise RuntimeError(f"A character's name is in the jar ({len(found)}): {found[:20]}")
    return len(found)


def check_archive_scope():
    with zipfile.ZipFile(INPUT) as before, zipfile.ZipFile(OUTPUT) as after:
        old = set(before.namelist())
        new = set(after.namelist())
        missing = old - new
        if missing:
            raise RuntimeError(f"Original entries were removed: {sorted(missing)}")
        changed = {n for n in old if before.read(n) != after.read(n)}
        if changed != CHANGED:
            raise RuntimeError(f"Unexpected changes to original entries: {sorted(changed ^ CHANGED)}")
        added = new - old
        stray = sorted(n for n in added if not n.startswith(NEW_PREFIXES))
        if stray:
            raise RuntimeError(f"Unexpected new entries (stubs must never be packaged): {stray}")
    return len(added)


def check_guarded_scope(report):
    """After the guard: original entries changed are the patched ones and the guarded ones, nothing else, none removed."""
    guarded = {line.strip() + ("" if line.strip().endswith(".class") else "") for line in report.read_text(encoding="utf-8").splitlines()
               if line.strip()}
    with zipfile.ZipFile(INPUT) as before, zipfile.ZipFile(OUTPUT) as after:
        old = set(before.namelist())
        new = set(after.namelist())
        if old - new:
            raise RuntimeError(f"Original entries were removed by the guard: {sorted(old - new)}")
        changed = {n for n in old if before.read(n) != after.read(n)}
        unexpected = changed - CHANGED - guarded
        if unexpected:
            raise RuntimeError(f"The guard changed original entries it did not report: {sorted(unexpected)}")
        if not guarded <= new:
            raise RuntimeError(f"The guard reported entries that are not in the JAR: {sorted(guarded - new)}")


def build_awakening():
    """The Dragon: compile against its Mojang-named stubs, run its tests, remap to SRG and add it to the expansion classes."""
    for directory in (AWAKENING_STUBS, AWAKENING_RAW, AWAKENING_SRG, AWAKENING_TESTS):
        clean(directory)
    javac("--release", "17", "-d", AWAKENING_STUBS, *sources("awakening/stubs"))
    classpath = os.pathsep.join(map(str, (AWAKENING_STUBS, BASE, CLASSES)))
    run("javac", "-encoding", "UTF-8", "-proc:none", "-Xlint:all", "-Xlint:-options", "-Xlint:-classfile", "-Werror", "--release", "17",
        "-d", AWAKENING_RAW, "-cp", classpath, *sources("awakening/src"))
    javac("--release", "17", "-d", AWAKENING_TESTS, "-cp", os.pathsep.join(map(str, (AWAKENING_RAW, AWAKENING_STUBS, BASE, CLASSES))),
          *sources("awakening/test"))
    tests = ROOT / "awakening" / "test"
    for test in sorted(p.relative_to(tests).with_suffix("").as_posix().replace("/", ".") for p in tests.rglob("*Test.java")):
        run("java", "-ea", "-cp", os.pathsep.join(map(str, (AWAKENING_TESTS, AWAKENING_RAW, AWAKENING_STUBS, BASE, CLASSES))), test)
    run("java", *EXPORTS, "-cp", TOOLS, "KaguneRemap", AWAKENING_RAW, AWAKENING_STUBS, ROOT / "awakening" / "mappings.srg", INPUT,
        AWAKENING_SRG)
    for path in sorted(AWAKENING_SRG.rglob("*.class")):
        target = CLASSES / path.relative_to(AWAKENING_SRG)
        if target.exists():
            raise RuntimeError(f"The Dragon would overwrite an expansion class: {target}")
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(path, target)


def main():
    actual = hashlib.sha256(INPUT.read_bytes()).hexdigest()
    if actual != EXPECTED_INPUT_SHA256:
        raise RuntimeError(f"Baseline JAR differs from the inspected version: {actual}")
    for directory in (TOOLS, STUBS, CLASSES, TEST_CLASSES):
        clean(directory)

    run("java", "-m", "jdk.compiler/com.sun.tools.javac.Main", *EXPORTS, "-nowarn",
        "-d", TOOLS, "patch/KagunePatch.java", "patch/KaguneVerify.java", "patch/KaguneServerCheck.java", "patch/KaguneGuard.java",
        "patch/KaguneRemap.java")
    run("java", *EXPORTS, "-cp", TOOLS, "KagunePatch", INPUT, BASE)

    javac("--release", "17", "-d", STUBS, *sources("stubs"))
    javac("--release", "17", "-Xlint:-options", "-cp", f"{BASE}{os.pathsep}{STUBS}", "-d", CLASSES, *sources("src/main/java"))
    build_awakening()

    assemble()
    run("java", *EXPORTS, "-cp", TOOLS, "KaguneVerify", OUTPUT, INPUT, ROOT / "verify" / "api-allowlist.txt", STUBS)
    added = check_archive_scope()
    run("java", *EXPORTS, "-cp", TOOLS, "KaguneServerCheck", OUTPUT, STUBS)
    # 6.0.10: nothing in the mod may kick a player or crash the server: every entry point wrapped, then checked again.
    report = BUILD / "guarded-classes.txt"
    run("java", *EXPORTS, "-cp", TOOLS, "KaguneGuard", OUTPUT, report)
    check_guarded_scope(report)
    run("java", *EXPORTS, "-cp", TOOLS, "KaguneServerCheck", OUTPUT, STUBS)
    check_names()

    javac("--release", "17", "-cp", os.pathsep.join(map(str, (CLASSES, BASE, STUBS))), "-d", TEST_CLASSES, *sources("src/test/java"))
    suites = sorted(p.relative_to(ROOT / "src/test/java").with_suffix("").as_posix().replace("/", ".")
                    for p in (ROOT / "src/test/java").rglob("*Tests.java"))
    for suite in suites:
        run("java", "-ea", "-cp", os.pathsep.join(map(str, (TEST_CLASSES, CLASSES, BASE))), suite)

    print(f"Verified {OUTPUT.name}: only the intended original entries changed, {added} expansion entries added, "
          "all external references are on the verified API list, and the logic tests pass.")


if __name__ == "__main__":
    try:
        main()
    except subprocess.CalledProcessError as failure:
        sys.exit(failure.returncode)
