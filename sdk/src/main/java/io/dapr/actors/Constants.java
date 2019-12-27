/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.actors;

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
   * Dapr's default port.
   */
  public static final int DEFAULT_PORT = 3500;

  /**
   * Environment variable used to set Dapr's port.
   */
  public static final String ENV_DAPR_HTTP_PORT = "DAPR_HTTP_PORT";

  /**
   * Header used for request id in Dapr.
   */
  public static final String HEADER_DAPR_REQUEST_ID = "X-DaprRequestId";

  /**
   * Base URL for Dapr Actor APIs.
   */
  private static String ACTORS_BASE_URL = API_VERSION + "/" + "actors";

  /**
   * String format for Actors state management relative url.
   */
  public static String ACTOR_STATE_KEY_RELATIVE_URL_FORMAT = ACTORS_BASE_URL + "/%s/%s/state/%s";

  /**
   * String format for Actors state management relative url.
   */
  public static String ACTOR_STATE_RELATIVE_URL_FORMAT = ACTORS_BASE_URL + "/%s/%s/state";

  /**
   * String format for Actors method invocation relative url.
   */
  public static String ACTOR_METHOD_RELATIVE_URL_FORMAT = ACTORS_BASE_URL + "/%s/%s/method/%s";

  /**
   * String format for Actors reminder registration relative url..
   */
  public static String ACTOR_REMINDER_RELATIVE_URL_FORMAT = ACTORS_BASE_URL + "/%s/%s/reminders/%s";

  /**
   * String format for Actors timer registration relative url..
   */
  public static String ACTOR_TIMER_RELATIVE_URL_FORMAT = ACTORS_BASE_URL + "/%s/%s/reminders/%s";

  /**
   * Invoke Publish Path
   */
  public static String PUBLISH_PATH = DEFAULT_HOSTNAME + ":" + DEFAULT_PORT + API_VERSION + "/publish";

  /**
   * Invoke Binding Path
   */
  public static String BINDING_PATH = DEFAULT_HOSTNAME + ":" + DEFAULT_PORT + API_VERSION + "/binding";

  /**
   * State Path
   */
  public static String STATE_PATH = DEFAULT_HOSTNAME + ":" + DEFAULT_PORT + API_VERSION + "/state";



}
