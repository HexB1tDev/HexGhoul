"""4.1.78: prove the tests: break each rule in turn and check a test catches it (the house rule "each rule sabotaged fails").

    python3 tools/sabotage.py <list.json> [name ...]

<list.json>: a list of {"name": what is broken, "file": the source (from the repo root), "old": text found exactly once in it, "new": what
it becomes, "suite": the test suite to run (AgentTests, NpcTests...)}. For each (or only those named): the file changed, tools/fast_tests.sh
<suite> run, the file put back (always, even on an error). Prints for each: OK (caught, with the first FAILED line), MISS (no test failed:
the tests do not hold that rule), BAD (it did not compile: a sabotage that proves nothing), SKIP (its text not found exactly once); then
ALL CAUGHT or SOME MISSED. A MISS is a hole in the tests: fix the test, then run that sabotage again.

Holes it found before (4.1.74..4.1.78): a check of text.indexOf(x, from) - from < n passes when x is gone (-1); a size checked against the
very constant sabotaged (OFFERED); a test that crashed printed no FAILED (BAD/MISS: read the run's output).
"""
import json
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
FAST = ROOT / "tools" / "fast_tests.sh"


def main():
    rules = json.loads(Path(sys.argv[1]).read_text())
    only = set(sys.argv[2:])
    caught = True
    for rule in rules:
        if only and rule["name"] not in only:
            continue
        path = ROOT / rule["file"]
        source = path.read_text()
        if source.count(rule["old"]) != 1:
            print(f"SKIP {rule['name']}: its text found {source.count(rule['old'])} times")
            caught = False
            continue
        try:
            path.write_text(source.replace(rule["old"], rule["new"]))
            out = subprocess.run([str(FAST), rule["suite"]], capture_output=True, text=True, cwd=ROOT).stdout
            failed = [line for line in out.splitlines() if line.startswith("FAILED:")]
            if "MAIN COMPILE FAILED" in out:
                print(f"BAD  {rule['name']}: did not compile")
                caught = False
            elif failed:
                print(f"OK   {rule['name']}: caught ({len(failed)}): {failed[0][8:120]}")
            elif "checks passed" not in out:
                print(f"BAD  {rule['name']}: the suite did not finish (crashed?)")
                caught = False
            else:
                print(f"MISS {rule['name']}: no test failed")
                caught = False
        finally:
            path.write_text(source)
        sys.stdout.flush()
    print("ALL CAUGHT" if caught else "SOME MISSED")
    return 0 if caught else 1


if __name__ == "__main__":
    sys.exit(main())
