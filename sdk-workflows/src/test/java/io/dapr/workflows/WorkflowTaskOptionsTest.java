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
 * limitations under the License.
*/

package io.dapr.workflows;

import io.dapr.durabletask.HistoryPropagationScope;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorkflowTaskOptionsTest {

    @Test
    void testConstructorWithRetryPolicyAndHandler() {
        WorkflowTaskRetryPolicy retryPolicy = WorkflowTaskRetryPolicy.newBuilder().build();
        WorkflowTaskRetryHandler retryHandler = (context) -> true;
        
        WorkflowTaskOptions options = new WorkflowTaskOptions(retryPolicy, retryHandler);
        
        assertEquals(retryPolicy, options.getRetryPolicy());
        assertEquals(retryHandler, options.getRetryHandler());
        assertNull(options.getAppId());
    }

    @Test
    void testConstructorWithRetryPolicyOnly() {
        WorkflowTaskRetryPolicy retryPolicy = WorkflowTaskRetryPolicy.newBuilder().build();
        
        WorkflowTaskOptions options = new WorkflowTaskOptions(retryPolicy);
        
        assertEquals(retryPolicy, options.getRetryPolicy());
        assertNull(options.getRetryHandler());
        assertNull(options.getAppId());
    }

    @Test
    void testConstructorWithRetryHandlerOnly() {
        WorkflowTaskRetryHandler retryHandler = (context) -> true;
        
        WorkflowTaskOptions options = new WorkflowTaskOptions(retryHandler);
        
        assertNull(options.getRetryPolicy());
        assertEquals(retryHandler, options.getRetryHandler());
        assertNull(options.getAppId());
    }

    @Test
    void testConstructorWithAppIdOnly() {
        String appId = "test-app";
        
        WorkflowTaskOptions options = new WorkflowTaskOptions(appId);
        
        assertNull(options.getRetryPolicy());
        assertNull(options.getRetryHandler());
        assertEquals(appId, options.getAppId());
    }

    @Test
    void testConstructorWithAllParameters() {
        WorkflowTaskRetryPolicy retryPolicy = WorkflowTaskRetryPolicy.newBuilder().build();
        WorkflowTaskRetryHandler retryHandler = (context) -> true;
        String appId = "test-app";
        
        WorkflowTaskOptions options = new WorkflowTaskOptions(retryPolicy, retryHandler, appId);
        
        assertEquals(retryPolicy, options.getRetryPolicy());
        assertEquals(retryHandler, options.getRetryHandler());
        assertEquals(appId, options.getAppId());
    }

    @Test
    void testConstructorWithRetryPolicyAndAppId() {
        WorkflowTaskRetryPolicy retryPolicy = WorkflowTaskRetryPolicy.newBuilder().build();
        String appId = "test-app";
        
        WorkflowTaskOptions options = new WorkflowTaskOptions(retryPolicy, appId);
        
        assertEquals(retryPolicy, options.getRetryPolicy());
        assertNull(options.getRetryHandler());
        assertEquals(appId, options.getAppId());
    }

    @Test
    void testConstructorWithRetryHandlerAndAppId() {
        WorkflowTaskRetryHandler retryHandler = (context) -> true;
        String appId = "test-app";

        WorkflowTaskOptions options = new WorkflowTaskOptions(retryHandler, appId);

        assertNull(options.getRetryPolicy());
        assertEquals(retryHandler, options.getRetryHandler());
        assertEquals(appId, options.getAppId());
    }

    @Test
    void testWithHistoryPropagation_lineage() {
        WorkflowTaskOptions options = WorkflowTaskOptions.withHistoryPropagation(HistoryPropagationScope.LINEAGE);

        assertEquals(HistoryPropagationScope.LINEAGE, options.getHistoryPropagationScope());
        assertNull(options.getRetryPolicy());
        assertNull(options.getRetryHandler());
        assertNull(options.getAppId());
    }

    @Test
    void testWithHistoryPropagation_ownHistory() {
        WorkflowTaskOptions options = WorkflowTaskOptions.withHistoryPropagation(HistoryPropagationScope.OWN_HISTORY);

        assertEquals(HistoryPropagationScope.OWN_HISTORY, options.getHistoryPropagationScope());
    }

    @Test
    void testPropagateLineage_factory() {
        WorkflowTaskOptions options = WorkflowTaskOptions.propagateLineage();

        assertEquals(HistoryPropagationScope.LINEAGE, options.getHistoryPropagationScope());
    }

    @Test
    void testPropagateOwnHistory_factory() {
        WorkflowTaskOptions options = WorkflowTaskOptions.propagateOwnHistory();

        assertEquals(HistoryPropagationScope.OWN_HISTORY, options.getHistoryPropagationScope());
    }

    @Test
    void testConstructorWithAllParametersIncludingScope() {
        WorkflowTaskRetryPolicy retryPolicy = WorkflowTaskRetryPolicy.newBuilder().build();
        WorkflowTaskRetryHandler retryHandler = (context) -> true;
        String appId = "test-app";

        WorkflowTaskOptions options = new WorkflowTaskOptions(retryPolicy, retryHandler, appId,
            HistoryPropagationScope.LINEAGE);

        assertEquals(retryPolicy, options.getRetryPolicy());
        assertEquals(retryHandler, options.getRetryHandler());
        assertEquals(appId, options.getAppId());
        assertEquals(HistoryPropagationScope.LINEAGE, options.getHistoryPropagationScope());
    }

    @Test
    void testConstructorWithoutScope_returnsNull() {
        WorkflowTaskOptions options = new WorkflowTaskOptions("test-app");

        assertNull(options.getHistoryPropagationScope());
    }
}
