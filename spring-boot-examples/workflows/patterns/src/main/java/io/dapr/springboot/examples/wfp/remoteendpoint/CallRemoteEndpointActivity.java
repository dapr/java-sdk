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

package io.dapr.springboot.examples.wfp.remoteendpoint;

import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class CallRemoteEndpointActivity implements WorkflowActivity {

  private Logger logger = LoggerFactory.getLogger(CallRemoteEndpointActivity.class);

  @Value("${application.process-base-url:}")
  private String processBaseURL;

  @Autowired
  private RestTemplate restTemplate;


  @Override
  public Object run(WorkflowActivityContext ctx) {
    logger.info("Starting Activity: " + ctx.getName());
    var payload = ctx.getInput(Payload.class);

    HttpEntity<Payload> request =
            new HttpEntity<>(payload);
    payload = restTemplate.postForObject(processBaseURL + "/process", request, Payload.class);

    logger.info("Payload from the remote service: " + payload);

    return payload;
  }
}
