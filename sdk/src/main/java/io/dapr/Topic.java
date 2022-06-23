/*
 * Copyright 2021 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
limitations under the License.
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
   * Name of the pubsub bus to be subscribed to.
   * @return pubsub bus's name.
   */
  String pubsubName();

  /**
   * The rules used to match the incoming cloud event.
   * @return the CEL expression.
   */
  Rule rule() default @Rule(match = "", priority = 0);

  /**
   * Metadata in the form of a json object.
   * {
   *    "mykey": "myvalue"
   * }
   * @return metadata object
   */
  String metadata() default "{}";
}
