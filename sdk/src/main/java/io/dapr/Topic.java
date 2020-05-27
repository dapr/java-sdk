/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Topic {

  /**
   * Name of topic to be subscribed to.
   * @return Topic's name.
   */
  String name();

  /**
   * Metadata in the form of a json object.
   * {
   *    "mykey": "myvalue"
   * }
   * @return metadata object
   */
  String metadata() default "{}";
}
