package org.slf4j;

public interface Logger {
    void error(String message, Throwable error);
    void info(String message);
}
