package io.dapr.actors.client;

import io.dapr.actors.ActorId;
import io.dapr.actors.runtime.ActorStateSerializer;
import reactor.core.publisher.Mono;

import java.io.IOException;

/**
 * Implements a proxy client for an Actor's instance.
 */
class ActorProxyImpl implements ActorProxy {

    /**
     * Actor's identifier for this Actor instance.
     */
    private final ActorId actorId;

    /**
     * Actor's type for this Actor instance.
     */
    private final String actorType;

    /**
     * Serializer/deserialzier to exchange message for Actors.
     */
    private final ActorStateSerializer serializer;

    /**
     * Client to talk to the Dapr's API.
     */
    private final ActorProxyAsyncClient daprClient;

    /**
     * Creates a new instance of {@link ActorProxyAsyncClient}.
     *
     * @param actorType  actor implementation type of the actor associated with the proxy object.
     * @param actorId    The actorId associated with the proxy
     * @param serializer Serializer and deserializer for method calls.
     * @param daprClient Dapr client.
     */
    ActorProxyImpl(String actorType, ActorId actorId, ActorStateSerializer serializer, ActorProxyAsyncClient daprClient) {
        this.actorType = actorType;
        this.actorId = actorId;
        this.daprClient = daprClient;
        this.serializer = serializer;
    }

    /**
     * {@inheritDoc}
     */
    public ActorId getActorId() {
        return actorId;
    }

    /**
     * {@inheritDoc}
     */
    public String getActorType() {
        return actorType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Mono<T> invokeActorMethod(String methodName, Object data, Class<T> clazz) {
        String jasonPayload;
        try {
            jasonPayload = this.wrap(data);
        } catch (IOException e) {
            return Mono.error(e);
        }

        Mono<String> result = this.daprClient.invokeActorMethod(
                    actorType,
                    actorId.toString(),
                    methodName,
                    jasonPayload
                    );

            return result
                    .filter(s -> !s.isEmpty())
                    .flatMap(s -> {
                        try {
                            return Mono.just(unwrap(s, clazz));
                        } catch (IOException e) {
                            return Mono.error(e);
                        }
                    });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Mono<T> invokeActorMethod(String methodName, Class<T> clazz) {
        Mono<String> result = this.daprClient.invokeActorMethod(actorType, actorId.toString(), methodName, null);
            return  result
                    .filter(s -> !s.isEmpty())
                    .flatMap(s -> {
                        try {
                            return Mono.just(unwrap(s, clazz));
                        } catch (IOException e) {
                           return Mono.error(e);
                        }
                    });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<Void> invokeActorMethod(String methodName) {
        Mono<String> result = this.daprClient.invokeActorMethod(actorType, actorId.toString(), methodName, null);
        return result.then();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<Void> invokeActorMethod(String methodName, Object data) {
        String jasonPayload;
        try {
            jasonPayload = this.wrap(data);
        } catch (IOException e) {
            return Mono.error(e);
        }
        return this.daprClient.invokeActorMethod(
                    actorType,
                    actorId.toString(),
                    methodName,
                    jasonPayload).then();


    }

    /**
     * Extracts the response object from the Actor's method result.
     *
     * @param response String returned by API.
     * @param clazz    Expected response class.
     * @param <T>      Expected response type.
     * @return Response object, null or RuntimeException.
     */
    private <T> T unwrap(final String response, Class<T> clazz) throws IOException {
        return this.serializer.unwrapMethodResponse(response, clazz);
    }

    /**
     * Builds the request to invoke an API for Actors.
     *
     * @param request Request object for the original Actor's method.
     * @param <T>     Type for the original Actor's method request.
     * @return String to be sent to Dapr's API.
     * @throws IOException In case it cannot generate String.
     */
    private <T> String wrap(final T request) throws IOException {
        return this.serializer.wrapMethodRequest(request);
    }

}
