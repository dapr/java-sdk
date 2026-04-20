/*
 * Copyright 2026 The Dapr Authors
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

package io.dapr.spring.workflows.config.annotations;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface WorkflowMetadata {
  /**
   * Name of the workflow.
   * Required when version is specified.
   *
   * @return the name of the workflow.
   */
  String name() default "";


  /**
   * Version of the workflow.
   * When specified, name must also be provided.
   *
   * @return the version of the workflow.
   */

  String version() default "";

  /**
   * Specifies if the version is the latest or not.
   * When true, the version and name must be provided.
   *
   * @return true if the version is the latest
   */
  boolean isLatest() default false;
}
