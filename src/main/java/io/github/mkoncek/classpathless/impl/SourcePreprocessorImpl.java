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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;

// import com.google.gson.internal.$Gson$Preconditions;
// import com.google.gson.internal..Gson.Preconditions;

public class SourcePreprocessorImpl {
    static String sanitizeImports(String source) {
        String resultContent = "";

        try (var reader = new BufferedReader(new StringReader(source))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("import ") && line.endsWith(";")) {
                    int pos = line.indexOf("..");
                    if (pos > 0) {
                        line = line.substring(0, pos + 1) + line.substring(pos + 1).replace('.', '$');
                    }
                }

                resultContent += line;
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }

        return resultContent;
    }
}
