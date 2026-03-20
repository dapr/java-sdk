/*
 * Copyright 2025 The Dapr Authors
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

package io.dapr.examples.workflows.compensation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CompensationHelper {

    private final Map<String, Runnable> compensations = new LinkedHashMap<>();

    public void addCompensation(String name, Runnable compensation) {
        compensations.put(name, compensation);
    }

    public void compensate() {
        List<String> keys = new ArrayList<>(compensations.keySet());
        Collections.reverse(keys);
        for (String key : keys) {
            compensations.get(key).run();
        }
    }
}
