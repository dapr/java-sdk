/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.it;


import java.io.IOException;

public interface Stoppable {

  void stop() throws InterruptedException, IOException;

}
