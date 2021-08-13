/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Rule {
  /**
   * The Common Expression Language (CEL) expression to use
   * to match the incoming cloud event.
   * @return the CEL expression.
   */
  String match();

  /**
   * Priority of the rule used for ordering.
   * @return the rule priority.
   */
  int priority();
}
