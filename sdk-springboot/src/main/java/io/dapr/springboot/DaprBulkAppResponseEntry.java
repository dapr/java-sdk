/*
 * Copyright 2022 The Dapr Authors
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

package io.dapr.springboot;

public class DaprBulkAppResponseEntry {
    private String entryID;
    private DaprBulkAppResponseStatus status;

    public DaprBulkAppResponseEntry(String entryID, DaprBulkAppResponseStatus status) {
        this.entryID = entryID;
        this.status = status;
    }

    public String getEntryID() {
        return entryID;
    }

    public DaprBulkAppResponseStatus getStatus() {
        return status;
    }
}
