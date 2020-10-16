/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it;


import java.io.IOException;

public interface Stoppable {

  void stop() throws InterruptedException, IOException;

}
