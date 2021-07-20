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
package io.github.mkoncek.classpathless.api;

import java.text.MessageFormat;

public interface MessagesListener {
    /**
     * Allows InMemoryCompiler to send runtime updates to caller.
     * @param level Severity of information.
     * @param message The message.
     */
    void addMessage(java.util.logging.Level level, String message);

    /**
     * Allows InMemoryCompiler to send runtime updates to caller.
     * @param level Severity of information.
     * @param format Format string as given to MessageFormat.
     * @param args Arguments to format.
     */
    default void addMessage(java.util.logging.Level level, String format, Object... args) {
        addMessage(level, MessageFormat.format(format, args));
    }
}
