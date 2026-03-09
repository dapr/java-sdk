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

package io.dapr.springboot.examples.orchestrator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.http.HttpClient;

@RestController
public class FullVersionRestController {

  public static final String V1_URL = "http://localhost:8081";
  public static final String V2_URL = "http://localhost:8082";
  private final String V1 = "v1";
  private final String V2 = "v2";

  private final Logger logger = LoggerFactory.getLogger(FullVersionRestController.class);

  @Autowired
  private HttpClient httpClient;

  @GetMapping("/")
  public String root() {
    return "OK";
  }


  /**
   * Track customer endpoint.
   *
   * @return confirmation that the workflow instance was created for a given customer
   */
  @PostMapping("/version/{version}/full")
  public String createWorkflowInstance(@PathVariable("version") String version) throws Exception {
    logger.info("Create workflow instance requested");
    var url = getUrlForVersion(version);
    var request = java.net.http.HttpRequest.newBuilder()
        .uri(java.net.URI.create(url + "/version/full"))
        .POST(java.net.http.HttpRequest.BodyPublishers.noBody())
        .header("Content-Type", "application/json")
        .build();

    // Send the request
    var response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

    return response.body();
  }

  /**
   * Request customer follow-up.
   *
   * @return confirmation that the follow-up was requested
   */
  @PostMapping("/version/{version}/full/followup/{instanceId}")
  public String followUp(@PathVariable("version") String version, @PathVariable("instanceId") String instanceId) throws Exception {
    logger.info("Follow-up requested");
    var url = getUrlForVersion(version);
    var request = java.net.http.HttpRequest.newBuilder()
        .uri(java.net.URI.create(url + "/version/full/followup/" + instanceId))
        .POST(java.net.http.HttpRequest.BodyPublishers.noBody())
        .header("Content-Type", "application/json")
        .build();

    // Send the request
    var response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
    return response.body();
  }

  /**
   * Request customer workflow instance status.
   *
   * @return the workflow instance status for a given customer
   */
  @PostMapping("/version/{version}/full/status/{instanceId}")
  public String getStatus(@PathVariable("version") String version, @PathVariable("instanceId") String instanceId) throws Exception {
    logger.info("Status requested");
    var url = getUrlForVersion(version);
    var request = java.net.http.HttpRequest.newBuilder()
        .uri(java.net.URI.create(url + "/version/full/status/" + instanceId))
        .POST(java.net.http.HttpRequest.BodyPublishers.noBody())
        .header("Content-Type", "application/json")
        .build();

    // Send the request
    var response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
    return response.body();
  }

  private String getUrlForVersion(String version) {
    if (V1.equals(version)) {
      return V1_URL;
    }

    if (V2.equals(version)) {
      return V2_URL;
    }

    throw new IllegalArgumentException("Invalid version: " + version);
  }
}

