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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

import io.github.mkoncek.classpathless.api.ClassIdentifier;
import io.github.mkoncek.classpathless.api.ClassesProvider;
import io.github.mkoncek.classpathless.api.ClasspathlessCompiler;
import io.github.mkoncek.classpathless.api.CompilationError;
import io.github.mkoncek.classpathless.api.IdentifiedBytecode;
import io.github.mkoncek.classpathless.api.IdentifiedSource;
import io.github.mkoncek.classpathless.api.MessagesListener;
import io.github.mkoncek.classpathless.api.SourcePostprocessor;

/**
 * An implementation using javax.tools compiler API
 */
public class CompilerJavac implements ClasspathlessCompiler {
    private JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    private InMemoryFileManager fileManager;
    private Arguments arguments;
    private SourcePostprocessor postprocessor = new SourcePostprocessor.Null();

    private static ClassIdentifier getIdentifier(JavaFileObject object) {
        /// Remove the leading "/"
        return new ClassIdentifier(object.getName().substring(1));
    }

    static private class DiagnosticToMessagesListener implements DiagnosticListener<JavaFileObject> {
        MessagesListener listener;
        Map<ClassIdentifier, Collection<CompilationError>> compilationErrors;

        DiagnosticToMessagesListener(MessagesListener listener) {
            this.listener = listener;
            this.compilationErrors = new TreeMap<>();
        }

        @Override
        public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
            var msg = diagnostic.getMessage(Locale.ENGLISH);

            var source = diagnostic.getSource();
            if (source != null) {
                compilationErrors.computeIfAbsent(getIdentifier(source), k -> new ArrayList<>())
                .add(new CompilationError(diagnostic.getLineNumber(), diagnostic.getColumnNumber(), diagnostic.getCode()));
            }

            if (listener != null) {
                listener.addMessage(Level.SEVERE, "Compiler diagnostic at {5}[{0}, {1}]: {2}{3}(code: {4})",
                        diagnostic.getLineNumber(), diagnostic.getColumnNumber(), msg,
                        System.lineSeparator(), diagnostic.getCode(),
                        (source != null ? "(" + source.getName() + ") " : " "));
            }
        }
    }

    public CompilerJavac(Arguments arguments) {
        this.fileManager = new InMemoryFileManager(
                compiler.getStandardFileManager(null, null, null));
        this.arguments = arguments;
    }

    public CompilerJavac() {
        this(new Arguments().useHostJavaClasses(true));
    }

    public void setPostProcessor(SourcePostprocessor postprocessor) {
        if (postprocessor == null) {
            postprocessor = new SourcePostprocessor.Null();
        }

        this.postprocessor = postprocessor;
    }

    @Override
    public Collection<IdentifiedBytecode> compileClass(
            ClassesProvider classprovider,
            Optional<MessagesListener> messagesConsumer,
            IdentifiedSource... javaSourceFiles) {
        Collection<InMemoryJavaSourceFileObject> compilationUnits = new ArrayList<>();

        for (var source : Arrays.asList(javaSourceFiles)) {
            compilationUnits.add(new InMemoryJavaSourceFileObject(source));
        }

        var messagesListener = messagesConsumer.orElse(new NullMessagesListener());

        fileManager.setClassProvider(classprovider);

        var availableClasses = new TreeSet<>(classprovider.getClassPathListing());

        fileManager.setAvailableClasses(availableClasses);
        var loggingSwitch = new LoggingSwitch();
        fileManager.setLoggingSwitch(loggingSwitch);
        fileManager.setArguments(arguments);

        for (boolean sourcesChanged = true; sourcesChanged;) {
            sourcesChanged = false;
            var diagnosticListener = new DiagnosticToMessagesListener(messagesListener);

            if (compiler.getTask(
                    null, fileManager, diagnosticListener, null, null, compilationUnits).call()) {
                break;
            }

            messagesListener.addMessage(Level.SEVERE, "Compilation failed");
            for (var entry : diagnosticListener.compilationErrors.entrySet()) {
                for (var cu : compilationUnits) {
                    if (entry.getKey().equals(cu.getClassIdentifier())) {
                        var result = postprocessor.postprocess(cu.getIdentifiedSource(), entry.getValue());
                        if (result.changed) {
                            sourcesChanged = true;
                            messagesListener.addMessage(Level.INFO, "SourcePostprocessor: reloading the source code of {0}", cu.getClassIdentifier().getFullName());
                            cu.setSource(result.source.getSourceCode());
                        }
                    }
                }
            }

            if (!sourcesChanged) {
                throw new RuntimeException("Could not compile file");
            }
        }

        var result = new ArrayList<IdentifiedBytecode>();
        var classOutputs = new ArrayList<JavaFileObject>();

        fileManager.clearAndGetOutput(classOutputs);

        for (final var classOutput : classOutputs) {
            byte[] content;
            try {
                content = classOutput.openInputStream().readAllBytes();
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
            result.add(new IdentifiedBytecode(getIdentifier(classOutput), content));
        }

        fileManager.setClassProvider(null);
        fileManager.setLoggingSwitch(null);

        return result;
    }
}
