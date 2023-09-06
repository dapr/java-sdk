/*
 * Copyright 2023 The Dapr Authors
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

package io.dapr.it;

import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.ToxicDirection;

import java.io.IOException;
import java.time.Duration;


public class ToxiProxyRun implements Stoppable {

  private final DaprRun daprRun;

  private final Duration latency;

  private final Duration jitter;

  private Command toxiProxyServer;

  private ToxiproxyClient toxiproxyClient;

  private Proxy grpcProxy;

  private Proxy httpProxy;

  private DaprPorts toxiProxyPorts;

  public ToxiProxyRun(DaprRun run, Duration latency, Duration jitter) {
    this.daprRun = run;
    this.latency = latency;
    this.jitter = jitter;
    this.toxiProxyPorts = DaprPorts.build(true, true, true);
    // artursouza: we use the "appPort" for the ToxiProxy server.
    this.toxiProxyServer = new Command(
            "Starting HTTP server on endpoint",
            "toxiproxy-server --port "
                    + this.toxiProxyPorts.getAppPort());
  }

  public void start() throws IOException, InterruptedException {
    this.toxiProxyServer.run();
    this.toxiproxyClient = new ToxiproxyClient("127.0.0.1", this.toxiProxyPorts.getAppPort());

    if (this.daprRun.getGrpcPort() != null) {
      this.grpcProxy = toxiproxyClient.createProxy(
              "daprd_grpc",
              "127.0.0.1:" + this.toxiProxyPorts.getGrpcPort(),
              "127.0.0.1:" + this.daprRun.getGrpcPort());
      this.grpcProxy.toxics()
              .latency("latency", ToxicDirection.DOWNSTREAM, this.latency.toMillis())
              .setJitter(this.jitter.toMillis());
    }

    if (this.daprRun.getHttpPort() != null) {
      this.httpProxy = toxiproxyClient.createProxy(
              "daprd_http",
              "127.0.0.1:" + this.toxiProxyPorts.getHttpPort(),
              "127.0.0.1:" + this.daprRun.getHttpPort());
      this.httpProxy.toxics()
              .latency("latency", ToxicDirection.DOWNSTREAM, this.latency.toMillis())
              .setJitter(this.jitter.toMillis());
    }
  }

  public void use() {
    this.toxiProxyPorts.use();
  }

  @Override
  public void stop() throws InterruptedException, IOException {
    this.toxiProxyServer.stop();
    this.toxiproxyClient = null;
    this.grpcProxy = null;
    this.httpProxy = null;
  }
}
