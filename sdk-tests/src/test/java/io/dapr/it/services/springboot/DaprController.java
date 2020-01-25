package io.dapr.actors.it.services.springboot;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.actors.runtime.ActorManagerTest;
import io.dapr.actors.runtime.ActorRuntime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;

/**
 * SpringBoot Controller to handle callback APIs for Dapr.
 */
@RestController
public class DaprController {

  @GetMapping("/")
  public String index() {
    return "Greetings from Dapr!";
  }

  @GetMapping("/dapr/config")
  public String daprConfig() throws Exception {
    return "{\"actorIdleTimeout\":\"5s\",\"actorScanInterval\":\"2s\",\"drainOngoingCallTimeout\":\"1s\",\"drainBalancedActors\":true,\"entities\":[\"DemoActorTest\"]}";
  }

  @PostMapping(path = "/actors/{type}/{id}")
  public Mono<Void> activateActor(@PathVariable("type") String type,
                                  @PathVariable("id") String id) throws Exception {
    return ActorRuntime.getInstance().activate(type, id);
  }

  @DeleteMapping(path = "/actors/{type}/{id}")
  public Mono<Void> deactivateActor(@PathVariable("type") String type,
                                    @PathVariable("id") String id) throws Exception {
    return ActorRuntime.getInstance().deactivate(type, id);
  }

  @PutMapping(path = "/actors/{type}/{id}/method/{method}")
  public Mono<byte[]> invokeActorMethod(@PathVariable("type") String type,
                                        @PathVariable("id") String id,
                                        @PathVariable("method") String method,
                                        @RequestBody(required = false) byte[] body) {
    return ActorRuntime.getInstance().invoke(type, id, method, body);
  }

  @PutMapping(path = "/actors/{type}/{id}/method/timer/{timer}")
  public Mono<Void> invokeActorTimer(@PathVariable("type") String type,
                                     @PathVariable("id") String id,
                                     @PathVariable("timer") String timer) {
    return ActorRuntime.getInstance().invokeTimer(type, id, timer);
  }

  @PutMapping(path = "/actors/{type}/{id}/method/remind/{reminder}")
  public Mono<Void> invokeActorReminder(@PathVariable("type") String type,
                                        @PathVariable("id") String id,
                                        @PathVariable("reminder") String reminder,
                                        @RequestBody(required = false) byte[] body) {
    return ActorRuntime.getInstance().invokeReminder(type, id, reminder, body);
  }
}
