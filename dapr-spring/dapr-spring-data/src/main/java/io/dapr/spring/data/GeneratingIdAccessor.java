/*
 * Copyright 2024 The Dapr Authors
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

package io.dapr.spring.data;

import org.springframework.data.keyvalue.core.IdentifierGenerator;
import org.springframework.data.mapping.IdentifierAccessor;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.util.Assert;

/**
 * {@link IdentifierAccessor} adding a {@link #getOrGenerateIdentifier()} to automatically generate an identifier and
 * set it on the underling bean instance.
 *
 * @see #getOrGenerateIdentifier()
 */
class GeneratingIdAccessor implements IdentifierAccessor {

  private final PersistentPropertyAccessor<?> accessor;
  private final PersistentProperty<?> identifierProperty;
  private final IdentifierGenerator generator;

  /**
   * Creates a new {@link GeneratingIdAccessor} using the given {@link PersistentPropertyAccessor}, identifier property
   * and {@link IdentifierGenerator}.
   *
   * @param accessor           must not be {@literal null}.
   * @param identifierProperty must not be {@literal null}.
   * @param generator          must not be {@literal null}.
   */
  GeneratingIdAccessor(PersistentPropertyAccessor<?> accessor, PersistentProperty<?> identifierProperty,
                       IdentifierGenerator generator) {

    Assert.notNull(accessor, "PersistentPropertyAccessor must not be null");
    Assert.notNull(identifierProperty, "Identifier property must not be null");
    Assert.notNull(generator, "IdentifierGenerator must not be null");

    this.accessor = accessor;
    this.identifierProperty = identifierProperty;
    this.generator = generator;
  }

  @Override
  public Object getIdentifier() {
    return accessor.getProperty(identifierProperty);
  }

  Object getOrGenerateIdentifier() {

    Object existingIdentifier = getIdentifier();

    if (existingIdentifier != null) {
      return existingIdentifier;
    }

    Object generatedIdentifier = generator.generateIdentifierOfType(identifierProperty.getTypeInformation());
    accessor.setProperty(identifierProperty, generatedIdentifier);

    return generatedIdentifier;
  }
}
