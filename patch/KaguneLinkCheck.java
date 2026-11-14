import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 4.1.103 (the owner: "making sure absolutely nothing in the mod fails"): the build's last word on the bytecode it rewrote. KagunePatch and
 * KaguneGuard rewrite classes with ASM, and ASM's own check ({@code BasicVerifier}) reads the instructions but not the stack map frames the
 * JVM checks as it loads a class: a wrong frame would be a VerifyError in the game, the class never loading (a crash). So every class the guard
 * changed (its report) is loaded and initialized here from the finished JAR, the compile stubs behind it, by HotSpot itself: its type-checking
 * verifier runs on each before initialization. A VerifyError or a ClassFormatError fails the build. A class whose initializer then fails (it
 * reaches into Minecraft, here only stubs) or whose supertypes aren't stubbed (Photon, LogUtils) was verified or can't be: those are counted,
 * and at most {@link #UNLINKED_MOST} may be the latter.
 */
public final class KaguneLinkCheck {
    /** Classes that can't be linked here at all (their supertypes not stubbed): 6 when this was written. */
    static final int UNLINKED_MOST = 12;

    private KaguneLinkCheck() {
    }

    public static void main(String[] args) {
        // An initializer run here may start a thread of its own: the check ends the JVM itself, either way.
        try {
            check(args);
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }

    static void check(String[] args) throws Exception {
        URL[] urls = {Path.of(args[0]).toUri().toURL(), Path.of(args[1]).toUri().toURL()};
        List<String> names = Files.readAllLines(Path.of(args[2]), StandardCharsets.UTF_8);
        int verified = 0;
        int unlinked = 0;
        List<String> broken = new ArrayList<>();
        List<String> unlinkedNames = new ArrayList<>();
        try (URLClassLoader loader = new URLClassLoader(urls, ClassLoader.getPlatformClassLoader())) {
            for (String entry : names) {
                if (entry.isBlank() || !entry.endsWith(".class")) {
                    continue;
                }
                String name = entry.substring(0, entry.length() - ".class".length()).replace('/', '.');
                try {
                    Class.forName(name, true, loader);
                    verified++;
                } catch (VerifyError | ClassFormatError bad) {
                    broken.add(name + ": " + bad);
                } catch (Throwable failure) {
                    Throwable root = failure;
                    while (root.getCause() != null && root.getCause() != root) {
                        root = root.getCause();
                    }
                    if (root instanceof VerifyError || root instanceof ClassFormatError) {
                        broken.add(name + ": " + root);
                    } else if (failure instanceof ExceptionInInitializerError || root instanceof UnsupportedOperationException
                        || failure instanceof NoSuchMethodError || failure instanceof NoSuchFieldError
                        || failure instanceof NoClassDefFoundError && String.valueOf(failure.getMessage()).startsWith("Could not initialize class")) {
                        // Verified, then its initializer reached the stubs (or a class whose own initializer had: "Could not initialize
                        // class" is thrown only once a class is linked, so after its verification).
                        verified++;
                    } else {
                        unlinked++; // a supertype or a class it needs to be checked not stubbed (Photon, LogUtils)
                        unlinkedNames.add(name.substring(name.lastIndexOf('.') + 1) + " (" + failure + ")");
                    }
                }
            }
        }
        if (!broken.isEmpty()) {
            throw new IllegalStateException(broken.size() + " rewritten classes fail the JVM's own verifier: " + broken);
        }
        if (unlinked > UNLINKED_MOST) {
            throw new IllegalStateException(unlinked + " rewritten classes could not be linked to be verified (at most " + UNLINKED_MOST
                + "): stub what they need: " + unlinkedNames);
        }
        System.out.println("Verified by the JVM itself: " + verified + " rewritten classes load and pass its verifier (" + unlinked
            + " need classes not stubbed to be linked).");
    }
}
