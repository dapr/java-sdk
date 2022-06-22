/*
 * Copyright 2022 The Dapr Authors
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
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Rule {
  /**
   * The Common Expression Language (CEL) expression to use
   * to match the incoming cloud event.
   * <a href="https://docs.dapr.io/developing-applications/building-blocks/pubsub/howto-route-messages/">Examples</a>.
   * @return the CEL expression.
   */
  String match();

  /**
   * Priority of the rule used for ordering. Lowest numbers have higher priority.
   * @return the rule's priority.
   */
  int priority();
}
