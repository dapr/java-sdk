/*
 * Copyright 2023 The Dapr Authors
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

package io.dapr.springboot.annotations;

import io.dapr.Topic;
import io.dapr.client.domain.BulkSubscribeAppResponse;
import io.dapr.client.domain.BulkSubscribeMessage;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * BulkSubscribe annotation should be applied with {@link Topic @Topic} when
 * the topic should be subscribed to using the Bulk Subscribe API.
 * This will require handling multiple messages using {@link BulkSubscribeMessage
 * DaprBulkMessage}
 * and returning a {@link BulkSubscribeAppResponse DaprBulkAppResponse}.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BulkSubscribe {
  /**
   * Maximum number of messages in a bulk message from the message bus.
   * 
   * @return number of messages.
   */
  int maxMessagesCount() default -1;

  /**
   * Maximum duration to wait for maxBulkSubCount messages by the message bus
   * before sending the messages to Dapr.
   * 
   * @return time to await in milliseconds.
   */
  int maxAwaitDurationMs() default -1;
}
