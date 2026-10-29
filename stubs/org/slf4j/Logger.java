// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package org.slf4j;

public interface Logger {
    void info(String message);
    void error(String message, Throwable failure);
    void error(String format, Object first, Object second);
}
