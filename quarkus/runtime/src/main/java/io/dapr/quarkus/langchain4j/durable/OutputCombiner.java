/*
 * Copyright 2026 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
limitations under the License.
*/

package io.dapr.quarkus.langchain4j.durable;

import java.util.List;

/**
 * Reference to a composite's static {@code @Output} method, which combines sub-agent outputs
 * (matched from scope by parameter name) into the composite's result.
 *
 * @param declaringClass FQCN declaring the {@code @Output} method
 * @param methodName     the static method name
 * @param paramNames     the method's parameter names, in order (each a scope key to read)
 */
public record OutputCombiner(String declaringClass, String methodName, List<String> paramNames) {
}
