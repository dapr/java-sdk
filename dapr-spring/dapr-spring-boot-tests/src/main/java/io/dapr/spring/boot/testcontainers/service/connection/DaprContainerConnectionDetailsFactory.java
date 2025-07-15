/*
 * Copyright 2021 The Dapr Authors
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

package io.dapr.spring.boot.testcontainers.service.connection;

import io.dapr.spring.boot.autoconfigure.client.DaprConnectionDetails;
import io.dapr.testcontainers.DaprContainer;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;

public class DaprContainerConnectionDetailsFactory
        extends ContainerConnectionDetailsFactory<DaprContainer, DaprConnectionDetails> {

  DaprContainerConnectionDetailsFactory() {
  }

  protected DaprConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<DaprContainer> source) {
    return new DaprContainerConnectionDetails(source);
  }

  private static final class DaprContainerConnectionDetails
          extends ContainerConnectionDetailsFactory.ContainerConnectionDetails<DaprContainer>
          implements DaprConnectionDetails {
    private DaprContainerConnectionDetails(ContainerConnectionSource<DaprContainer> source) {
      super(source);
    }

    @Override
    public String getHttpEndpoint() {
      return getContainer().getHttpEndpoint();
    }

    @Override
    public String getGrpcEndpoint() {
      return getContainer().getGrpcEndpoint();
    }

    @Override
    public Integer getHttpPort() {
      return getContainer().getHttpPort();
    }

    @Override
    public Integer getGrpcPort() {
      return getContainer().getGrpcPort();
    }

    /*
     * No API Token for local container
     */
    @Override
    public String getApiToken() {
      return "";
    }
  }
}
