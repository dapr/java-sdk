/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.utils;

/**
 * Useful constants for the Dapr's Actor SDK.
 */
public final class Constants {

    /**
     * Dapr API used in this client.
     */
    public static final String API_VERSION = "v1.0";

    /**
     * Dapr's default hostname.
     */
    public static final String DEFAULT_HOSTNAME = "localhost";

    /**
     * Header used for request id in Dapr.
     */
    public static final String HEADER_DAPR_REQUEST_ID = "X-DaprRequestId";

    /**
     * Header for the conditional operation.
     */
    public static final String HEADER_HTTP_ETAG_ID = "If-Match";

    /**
     * Base URL for Dapr Actor APIs.
     */
    private static final String ACTORS_BASE_URL = API_VERSION + "/" + "actors";

    /**
     * String format for Actors state management relative url.
     */
    public static final String ACTOR_STATE_KEY_RELATIVE_URL_FORMAT = ACTORS_BASE_URL + "/%s/%s/state/%s";

    /**
     * String format for Actors state management relative url.
     */
    public static final String ACTOR_STATE_RELATIVE_URL_FORMAT = ACTORS_BASE_URL + "/%s/%s/state";

    /**
     * String format for Actors method invocation relative url.
     */
    public static final String ACTOR_METHOD_RELATIVE_URL_FORMAT = ACTORS_BASE_URL + "/%s/%s/method/%s";

    /**
     * String format for Actors reminder registration relative url..
     */
    public static final String ACTOR_REMINDER_RELATIVE_URL_FORMAT = ACTORS_BASE_URL + "/%s/%s/reminders/%s";

    /**
     * String format for Actors timer registration relative url..
     */
    public static final String ACTOR_TIMER_RELATIVE_URL_FORMAT = ACTORS_BASE_URL + "/%s/%s/timers/%s";

    /**
     * Base path to invoke methods.
     */
    public static final String INVOKE_PATH = API_VERSION + "/invoke";

    /**
     * Invoke Publish Path
     */
    public static final String PUBLISH_PATH = API_VERSION + "/publish";

    /**
     * Invoke Binding Path
     */
    public static final String BINDING_PATH = API_VERSION + "/bindings";

    /**
     * State Path
     */
    public static final String STATE_PATH = API_VERSION + "/state";
}
