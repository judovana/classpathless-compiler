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
package io.github.mkoncek.classpathless.helpers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import io.github.mkoncek.classpathless.api.ClassIdentifier;
import io.github.mkoncek.classpathless.api.ClassesProvider;
import io.github.mkoncek.classpathless.api.IdentifiedBytecode;

public class SimpleClassesProvider implements ClassesProvider {
    private Map<ClassIdentifier, IdentifiedBytecode> classes;

    public SimpleClassesProvider(Collection<IdentifiedBytecode> bytecodes) {
        this();
        for (var bytecode : bytecodes) {
            classes.put(bytecode.getClassIdentifier(), bytecode);
        }
    }

    public SimpleClassesProvider() {
        classes = new TreeMap<>();
    }

    @Override
    public Collection<IdentifiedBytecode> getClass(ClassIdentifier... names) {
        var result = new ArrayList<IdentifiedBytecode>();

        for (var name : names) {
            var bytecode = classes.get(name);
            if (bytecode != null) {
                result.add(bytecode);
            }
        }

        return result;
    }

    @Override
    public List<String> getClassPathListing() {
        return classes.keySet().stream().map(ci -> ci.getFullName()).collect(Collectors.toList());
    }
}
