package com.hex.kagune.control;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 4.1.103 (the owner: "making sure absolutely nothing in the mod fails and nothing ever lags"): the ways the game calls into the mod that a
 * throw used to crash, guarded. The build's guard (patch/KaguneGuard) proves at build time that every one it names is wrapped and type-checks;
 * these hold the names it is given: the NPCs' brains and drops (a "Ticking entity" crash of the server), every screen's drawing and every
 * NPC renderer (a crash of the game).
 */
public final class NoFailTests {
    private static int passed;
    private static final List<String> FAILURES = new ArrayList<>();

    private static void check(boolean condition, String description) {
        if (condition) {
            passed++;
        } else {
            FAILURES.add(description);
        }
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }

    public static void main(String[] args) throws Exception {
        String guard = read("patch/KaguneGuard.java");
        check(guard.contains("\"com/hex/kagune/control/npc/CcgAgent\", Set.of(\"m_8024_\", \"m_7472_\"),"), "an investigator's brain and drops guarded");
        check(guard.contains("\"com/hex/kagune/control/npc/HumanNpc\", Set.of(\"m_8024_\", \"m_7472_\"),"), "a townsperson's brain and drops guarded");
        check(guard.contains("\"com/hex/kagune/control/npc/EtoNpc\", Set.of(\"m_8024_\"),"), "a companion's or an official's brain guarded");
        check(guard.contains("\"com/hex/kagune/control/npc/ChildNpc\", Set.of(\"m_8024_\", \"m_6138_\"));"), "a child's brain and push guarded");
        check(guard.contains("\"com/hex/kagune/control/npc/FleshThrall\", Set.of(\"m_8024_\"));"), "a flesh creature's brain guarded (removed on a throw)");
        check(guard.contains("GHOUL, Set.of(\"m_8024_\")") && guard.contains("GHOUL, Set.of(\"m_8099_\", \"m_7472_\")"), "a wild ghoul's, as before");
        int callbacks = guard.indexOf("static final Map<String, Set<String>> CALLBACKS");
        int discard = guard.indexOf("static final Map<String, Set<String>> DISCARD");
        check(guard.indexOf("\"com/hex/kagune/control/npc/CcgAgent\"", callbacks) > callbacks
            && guard.indexOf("\"com/hex/kagune/control/npc/EtoNpc\"", callbacks) > callbacks
            && guard.indexOf("\"com/hex/kagune/control/npc/FleshThrall\"", discard) < callbacks,
            "an official, the Reaper or a companion never removed for a bug (logged and carried on); a flesh creature removed");
        check(guard.contains("GUARDED_NAMES = Set.of(\"m_88315_\");") && guard.contains("SCREEN_FLOOR = 10;")
            && guard.contains("if ((subscribes(method) || guardedByName(node, method)) && !guarded(method)) {"),
            "every screen's drawing guarded, and proved so for every one in the jar");
        // Every NPC renderer: the draw in a catch that puts the pose stack back and tells it once.
        String renderGuard = read("src/main/java/com/hex/kagune/control/client/render/RenderGuard.java");
        check(renderGuard.contains("for (int i = 0; i < MOST_POPS && pose.m_85850_() != mark; i++) {\n                pose.m_85849_();")
            && renderGuard.contains("PlayerLayers.failed(what + \" (its renderer)\", failure);"), "a failing draw: the stack put back, told once");
        for (String renderer : new String[] {"GhoulNpcRenderer", "ChildRenderer", "HumanRenderer", "AgentRenderer", "EtoRenderer"}) {
            String source = read("src/main/java/com/hex/kagune/control/client/render/" + renderer + ".java");
            int render = source.indexOf("    public void m_7392_(");
            int mark = source.indexOf("PoseStack.Pose mark = pose.m_85850_();", render);
            int end = source.indexOf("\n    }\n", render);
            int caught = source.lastIndexOf("RenderGuard.failed(pose, mark, ", end);
            check(render > 0 && mark > render && mark - render < 200 && caught > mark && end - caught < 120, renderer + " draws inside RenderGuard");
        }
        // The JVM's own verifier over every rewritten class, in the build (ASM's check reads no stack map frames: a frame the guard got wrong
        // passed it; proved by breaking the guard's handler frame, which only this caught: 34 classes "Stack map does not match").
        String build = read("build.py");
        int guarded = build.indexOf("run(\"java\", *EXPORTS, \"-cp\", TOOLS, \"KaguneGuard\", OUTPUT, report)");
        int linked = build.indexOf("run(\"java\", \"-cp\", TOOLS, \"KaguneLinkCheck\", OUTPUT, STUBS, report)");
        check(guarded > 0 && linked > guarded && build.indexOf("check_names()", linked) > linked && build.contains("\"patch/KaguneLinkCheck.java\")"),
            "the build has the JVM verify every class the guard rewrote, after the guard");
        String link = read("patch/KaguneLinkCheck.java");
        check(link.contains("} catch (VerifyError | ClassFormatError bad) {\n                    broken.add(")
            && link.contains("if (root instanceof VerifyError || root instanceof ClassFormatError) {\n                        broken.add(")
            && link.contains("if (!broken.isEmpty()) {\n            throw new IllegalStateException(") && link.contains("UNLINKED_MOST = 12;")
            && link.contains("Class.forName(name, true, loader);"), "a class the JVM won't verify fails the build; few may go unlinked");
        String safety = read("src/main/java/com/hex/kagune/control/Safety.java");
        check(safety.contains("if (failure instanceof VirtualMachineError && !(failure instanceof StackOverflowError)) {"),
            "only what nothing can carry on from (out of memory) is let through");
        if (!FAILURES.isEmpty()) {
            FAILURES.forEach(failure -> System.out.println("FAILED: " + failure));
            System.exit(1);
        }
        System.out.println("No-fail tests: " + passed + " checks passed");
    }
}
