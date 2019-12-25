package io.dapr.actors.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.actors.ActorId;
import io.dapr.actors.Constants;
import reactor.core.publisher.Mono;

import java.io.IOException;

public class ActorProxyImpl implements  ActorProxy {

    /**
     * Shared Json serializer/deserializer as per Jackson's documentation.
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ActorId actorId;
    private String actorType;
    private ActorProxyHttpAsyncClient abstractDaprClient;

    /**
     * Creates a new instance of {@link ActorProxyHttpAsyncClient}.
     *
     * @param actorId The actorId associated with the proxy
     * @param actorType actor implementation type of the actor associated with the proxy object.
     */
    ActorProxyImpl(ActorId actorId, String actorType, ActorProxyHttpAsyncClient abstractDaprClient) {
        this.abstractDaprClient= abstractDaprClient;
        this.setActorId(actorId);
        this.setActorType(actorType);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Mono<T> invokeActorMethod(String methodName, Object data, Class<T> clazz) throws IOException {

        Mono<String> result=this.invokeActorMethod(actorType,actorId.toString(),methodName,OBJECT_MAPPER.writeValueAsString(data));
        return result
                .filter(s -> (s != null) && (!s.isEmpty()))
                .map(s -> {
                    try {
                        return OBJECT_MAPPER.readValue(s, clazz);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Mono<T> invokeActorMethod(String methodName,  Class<T> clazz){

        Mono<String> result=this.invokeActorMethod(actorType,actorId.toString(),methodName,null);
        return result
                .filter(s -> (s != null) && (!s.isEmpty()))
                .map(s -> {
                    try {
                        return OBJECT_MAPPER.readValue(s, clazz);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<String> invokeActorMethod(String methodName) {

        Mono<String> result=this.invokeActorMethod(actorType,actorId.toString(),methodName,null);
        return result
                .filter(s -> (s != null) && (!s.isEmpty()))
                .map(s -> s);
    }

    @Override
    public Mono<String> invokeActorMethod(String methodName, Object data) throws IOException {
        Mono<String> result=this.invokeActorMethod(actorType,actorId.toString(),methodName,OBJECT_MAPPER.writeValueAsString(data));
        return result
                .filter(s -> (s != null) && (!s.isEmpty()))
                .map(s -> s);
    }


    protected Mono<String> invokeActorMethod(String actorType, String actorId, String methodName, String jsonPayload) {
        String url = String.format(Constants.ACTOR_METHOD_RELATIVE_URL_FORMAT, actorType, actorId, methodName);
        return this.abstractDaprClient.invokeAPI("PUT", url, jsonPayload);
    }




    /**
     * {@inheritDoc}
     */
    public ActorId getActorId() {
        return actorId;
    }

    private void setActorId(ActorId actorId) {
        this.actorId = actorId;
    }

    /**
     * {@inheritDoc}
     */
    public String getActorType() {
        return actorType;
    }

    private void setActorType(String actorType) {
        this.actorType = actorType;
    }
}
