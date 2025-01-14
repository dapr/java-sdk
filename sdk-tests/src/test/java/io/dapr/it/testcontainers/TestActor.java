package io.dapr.it.testcontainers;
import io.dapr.actors.ActorMethod;
import io.dapr.actors.ActorType;

@ActorType(name = "TestActor")
public interface TestActor {
    @ActorMethod(name = "echo_message")
    String echo(String message);
}
