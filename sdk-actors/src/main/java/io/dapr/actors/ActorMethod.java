/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.actors;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ActorMethod {

  /**
   * Actor's method return type. This is required when result object is within a Mono response.
   *
   * @return Actor's method return type.
   */
  Class returns() default Undefined.class;

  /**
   * Actor's method name. This is optional and will override the method's default name for actor invocation.
   *
   * @return Actor's method name.
   */
  String name() default "";

}
