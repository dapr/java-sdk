package io.dapr.spring.workflows;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Operation {
  /**
   * Operation's name.
   * Default empty String
   * @return String with the name
   */
  String name() default "";
}
