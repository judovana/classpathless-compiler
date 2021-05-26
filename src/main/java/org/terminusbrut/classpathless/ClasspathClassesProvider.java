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
package org.terminusbrut.classpathless;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.terminusbrut.classpathless.api.ClassIdentifier;
import org.terminusbrut.classpathless.api.ClassesProvider;
import org.terminusbrut.classpathless.api.IdentifiedBytecode;

public class ClasspathClassesProvider implements ClassesProvider {
    static final String CP_SEPARATOR = System.getProperty("path.separator");

    public List<String> classpath = Collections.emptyList();
    public Map<String, Path> classesToClassFilePaths;

    public ClasspathClassesProvider(String classpath) {
        super();

        if (classpath != null) {
            this.classpath = obtainClasspath(classpath);
        }

        this.classesToClassFilePaths = findAllClasses(this.classpath);
    }

    @Override
    public Collection<IdentifiedBytecode> getClass(ClassIdentifier... names) {
        var result = new ArrayList<IdentifiedBytecode>();

        for (var indentifier : names) {
            var pathOfClass = classesToClassFilePaths.get(indentifier.getFullName());

            if (pathOfClass != null) {
                try {
                    var bytes = FileUtils.readFileToByteArray(pathOfClass.toFile());
                    result.add(new IdentifiedBytecode(indentifier, bytes));
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            }
        }

        return result;
    }

    @Override
    public List<String> getClassPathListing() {
        return new ArrayList<>(classesToClassFilePaths.keySet());
    }

    /**
     *
     * @param classpath
     * @return A list of strings representing the roots and /* expanded to jar files
     */
    static private List<String> obtainClasspath(String classpath) {
        /// https://docs.oracle.com/javase/8/docs/technotes/tools/windows/classpath.html

        var unexpanded = Arrays.asList(classpath.split(CP_SEPARATOR));
        var result = new ArrayList<String>();

        /// TODO consider symbolic links
        for (var name : unexpanded) {
            if (name.endsWith("/*")) {
                var root = Paths.get(name.substring(0, name.length() - 1));
                if (Files.isDirectory(root)) {
                    try {
                        Files.list(root).filter(p -> p.toString().endsWith(".jar")
                                && !Files.isDirectory(p)).forEach(p -> result.add(p.toString()));

                    } catch (IOException ex) {
                        throw new UncheckedIOException(ex);
                    }
                }
            } else {
                result.add(Paths.get(name).toString());
            }
        }

        return result;
    }

    static private Map<String, Path> findAllClasses(List<String> classpath) {
        /// TODO handle jars

        var result = new HashMap<String, Path>();

        for (var root : classpath) {
            var root_path = Paths.get(root);
            if (Files.isDirectory(root_path)) {
                try {
                    Files.walk(root_path).filter(p -> !Files.isDirectory(p) &&
                            p.toString().endsWith(".class"))
                    .forEach(p -> {
                        var relativeString = root_path.relativize(p).toString();
                        relativeString = relativeString.substring(0, relativeString.length() - 6)
                                .replace(File.separator, ".");
                        result.put(relativeString, p);
                    });
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            }
        }

        return result;
    }

    /// TODO make this into a test case
    /*
    public static void main(String[] args) {
        System.out.println(obtainClasspath("/usr/lib/java"));
        System.out.println(obtainClasspath("/usr/lib/java/*"));
        System.out.println(obtainClasspath("/usr/lib/java/*:/home/mkoncek/Upstream/classpathless-compiler/target/classes/org/terminusbrut/classpathless/impl/"));
        var ccp = new ClasspathClassesProvider(".:target/classes/");
        ccp.getClass(new ClassIdentifier("InMemoryFileManager$1.class"));
        System.out.println(ccp.classesToClassFilePaths);
        for (var bc : ccp.getClass(new ClassIdentifier("org.terminusbrut.classpathless.impl.Compiler$1"))) {
            System.out.println(bc.getFile());
        }
        System.out.println(ccp.getClassPathListing());
    }
     */
}
