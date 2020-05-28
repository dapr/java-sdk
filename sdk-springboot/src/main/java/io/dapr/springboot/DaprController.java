/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.springboot;

import io.dapr.actors.runtime.ActorRuntime;
import io.dapr.serializer.DefaultObjectSerializer;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.io.IOException;

/**
 * SpringBoot Controller to handle callback APIs for Dapr.
 */
@RestController
public class DaprController {

  /**
   * Dapr's default serializer/deserializer.
   */
  private static final DefaultObjectSerializer SERIALIZER = new DefaultObjectSerializer();

  /**
   * Callback API for health checks from Dapr's sidecar.
   */
  @GetMapping("/healthz")
  public void healthz() {
  }

  /**
   * Returns Dapr's configuration for Actors.
   * @return Actor's configuration.
   * @throws IOException If cannot generate configuration.
   */
  @GetMapping("/dapr/config")
  public byte[] daprConfig() throws IOException {
    return ActorRuntime.getInstance().serializeConfig();
  }

  /**
   * Returns the list of subscribed topics.
   * @return List of subscribed topics.
   * @throws IOException If cannot generate list of topics.
   */
  @GetMapping("/dapr/subscribe")
  public byte[] daprSubscribe() throws IOException {
    return SERIALIZER.serialize(DaprRuntime.getInstance().listSubscribedTopics());
  }

  /**
   * Handles API to deactivate an actor.
   * @param type Actor type.
   * @param id Actor Id.
   * @return Void.
   */
  @DeleteMapping(path = "/actors/{type}/{id}")
  public Mono<Void> deactivateActor(@PathVariable("type") String type,
                                    @PathVariable("id") String id) {
    return ActorRuntime.getInstance().deactivate(type, id);
  }

  /**
   * Handles API to invoke an actor's method.
   * @param type Actor type.
   * @param id Actor Id.
   * @param method Actor method.
   * @param body Raw request body.
   * @return Raw response body.
   */
  @PutMapping(path = "/actors/{type}/{id}/method/{method}")
  public Mono<byte[]> invokeActorMethod(@PathVariable("type") String type,
                                        @PathVariable("id") String id,
                                        @PathVariable("method") String method,
                                        @RequestBody(required = false) byte[] body) {
    return ActorRuntime.getInstance().invoke(type, id, method, body);
  }

  /**
   * Handles API to trigger an actor's timer.
   * @param type Actor type.
   * @param id Actor Id.
   * @param timer Actor timer's name.
   * @return Void.
   */
  @PutMapping(path = "/actors/{type}/{id}/method/timer/{timer}")
  public Mono<Void> invokeActorTimer(@PathVariable("type") String type,
                                     @PathVariable("id") String id,
                                     @PathVariable("timer") String timer) {
    return ActorRuntime.getInstance().invokeTimer(type, id, timer);
  }

  /**
   * Handles API to trigger an actor's reminder.
   * @param type Actor type.
   * @param id Actor Id.
   * @param reminder Actor reminder's name.
   * @param body Raw request's body.
   * @return Void.
   */
  @PutMapping(path = "/actors/{type}/{id}/method/remind/{reminder}")
  public Mono<Void> invokeActorReminder(@PathVariable("type") String type,
                                        @PathVariable("id") String id,
                                        @PathVariable("reminder") String reminder,
                                        @RequestBody(required = false) byte[] body) {
    return ActorRuntime.getInstance().invokeReminder(type, id, reminder, body);
  }

}
