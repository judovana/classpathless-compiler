/*-
 * Copyright (c) 2021 Marián Konček
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.mkoncek.classpathless.impl;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LoggingSwitch {
    PrintStream printer;
    private boolean tracing = false;
    private java.util.logging.Level logLevel = Level.OFF;

    public static class Null extends LoggingSwitch {
        public Null() {
            super(new PrintStream(PrintStream.nullOutputStream(), false, StandardCharsets.UTF_8));
        }
    }

    public LoggingSwitch(PrintStream printer) {
        this.printer = printer;
    }

    public LoggingSwitch() {
        var logging = System.getProperty("io.github.mkoncek.cplc.logging");
        if (logging == null) {
            printer = new PrintStream(PrintStream.nullOutputStream(), false, StandardCharsets.UTF_8);
        } else {
            if (logging.isEmpty()) {
                printer = System.err;
            } else {
                FileOutputStream os;
                try {
                    /// TODO not closed if any exception happens
                    os = new FileOutputStream(Paths.get(logging).toFile(), true);
                    printer = new PrintStream(os, true, StandardCharsets.UTF_8);
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            }
        }

        var level = Level.OFF;
        var loglevel = System.getProperty("io.github.mkoncek.cplc.loglevel");
        if (loglevel != null) {
            switch (loglevel) {
                case "all":
                    level = Level.ALL;
                    break;

                case "finest":
                    level = Level.FINEST;
                    break;

                case "finer":
                    level = Level.FINER;
                    break;

                case "fine":
                    level = Level.FINE;
                    break;

                case "config":
                    level = Level.CONFIG;
                    break;

                case "info":
                    level = Level.INFO;
                    break;

                case "warning":
                    level = Level.WARNING;
                    break;

                case "severe":
                    level = Level.SEVERE;
                    break;

                case "off":
                    level = Level.OFF;
                    break;

                default :
                    throw new IllegalArgumentException("Unrecognized logging level: \"" + loglevel + "\"");
            }
        }

        setLogLevel(level);

        if (System.getProperty("io.github.mkoncek.cplc.tracing") != null) {
            setTracing(true);
        }
    }

    public void setTracing(boolean value) {
        this.tracing = value;
    }

    public void setLogLevel(java.util.logging.Level value) {
        this.logLevel = value;
    }

    public void trace(Object struct, String name, Object... args) {
        if (tracing) {
            logln(Level.FINEST, "[TRACE] invoking {0}::{1}({2})", struct.getClass().getName(), name,
                    Stream.of(args).map(arg -> arg == null ? "<null>" : arg.toString())
                    .collect(Collectors.joining(", ")));
        }
    }

    public void trace(Object result) {
        if (tracing) {
            logln(Level.FINEST, "[TRACE] returning {0}", result == null ? "<null>" : result.toString());
        }
    }

    public void log(java.util.logging.Level level, String format, Object... args) {
        if (logLevel != Level.OFF && level.intValue() >= logLevel.intValue()) {
            var message = "[LOG] " + MessageFormat.format(format, args);
            printer.print(message);
        }
    }

    public void logln(java.util.logging.Level level, String format, Object... args) {
        log(level, format + System.lineSeparator(), args);
    }
}
