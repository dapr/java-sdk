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

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.domain.Sort;
import org.springframework.data.keyvalue.core.IdentifierGenerator;
import org.springframework.data.keyvalue.core.KeyValueAdapter;
import org.springframework.data.keyvalue.core.KeyValueCallback;
import org.springframework.data.keyvalue.core.KeyValueOperations;
import org.springframework.data.keyvalue.core.KeyValuePersistenceExceptionTranslator;
import org.springframework.data.keyvalue.core.event.KeyValueEvent;
import org.springframework.data.keyvalue.core.mapping.KeyValuePersistentEntity;
import org.springframework.data.keyvalue.core.mapping.KeyValuePersistentProperty;
import org.springframework.data.keyvalue.core.mapping.context.KeyValueMappingContext;
import org.springframework.data.keyvalue.core.query.KeyValueQuery;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class DaprKeyValueTemplate implements KeyValueOperations, ApplicationEventPublisherAware {

  private static final PersistenceExceptionTranslator DEFAULT_PERSISTENCE_EXCEPTION_TRANSLATOR =
      new KeyValuePersistenceExceptionTranslator();

  private final KeyValueAdapter adapter;
  private final MappingContext<? extends KeyValuePersistentEntity<?, ?>, ? extends KeyValuePersistentProperty<?>>
      mappingContext;
  private final IdentifierGenerator identifierGenerator;

  private PersistenceExceptionTranslator exceptionTranslator = DEFAULT_PERSISTENCE_EXCEPTION_TRANSLATOR;
  private @Nullable ApplicationEventPublisher eventPublisher;
  private boolean publishEvents = false;
  private @SuppressWarnings("rawtypes") Set<Class<? extends KeyValueEvent>> eventTypesToPublish = Collections
      .emptySet();

  /**
   * Create new {@link DaprKeyValueTemplate} using the given {@link KeyValueAdapterResolver} with a default
   * {@link KeyValueMappingContext}.
   *
   * @param resolver must not be {@literal null}.
   */
  public DaprKeyValueTemplate(KeyValueAdapterResolver resolver) {
    this(resolver, new KeyValueMappingContext<>());
  }

  /**
   * Create new {@link DaprKeyValueTemplate} using the given {@link KeyValueAdapterResolver} and {@link MappingContext}.
   *
   * @param resolver       must not be {@literal null}.
   * @param mappingContext must not be {@literal null}.
   */
  @SuppressWarnings("LineLength")
  public DaprKeyValueTemplate(KeyValueAdapterResolver resolver,
                              MappingContext<? extends KeyValuePersistentEntity<?, ?>, ? extends KeyValuePersistentProperty<?>> mappingContext) {
    this(resolver, mappingContext, DefaultIdentifierGenerator.INSTANCE);
  }

  /**
   * Create new {@link DaprKeyValueTemplate} using the given {@link KeyValueAdapterResolver} and {@link MappingContext}.
   *
   * @param resolver            must not be {@literal null}.
   * @param mappingContext      must not be {@literal null}.
   * @param identifierGenerator must not be {@literal null}.
   */
  @SuppressWarnings("LineLength")
  public DaprKeyValueTemplate(KeyValueAdapterResolver resolver,
                              MappingContext<? extends KeyValuePersistentEntity<?, ?>, ? extends KeyValuePersistentProperty<?>> mappingContext,
                              IdentifierGenerator identifierGenerator) {
    Assert.notNull(resolver, "Resolver must not be null");
    Assert.notNull(mappingContext, "MappingContext must not be null");
    Assert.notNull(identifierGenerator, "IdentifierGenerator must not be null");

    this.adapter = resolver.resolve();
    this.mappingContext = mappingContext;
    this.identifierGenerator = identifierGenerator;
  }

  private static boolean typeCheck(Class<?> requiredType, @Nullable Object candidate) {
    return candidate == null || ClassUtils.isAssignable(requiredType, candidate.getClass());
  }

  public void setExceptionTranslator(PersistenceExceptionTranslator exceptionTranslator) {
    Assert.notNull(exceptionTranslator, "ExceptionTranslator must not be null");
    this.exceptionTranslator = exceptionTranslator;
  }

  /**
   * Set the {@link ApplicationEventPublisher} to be used to publish {@link KeyValueEvent}s.
   *
   * @param eventTypesToPublish must not be {@literal null}.
   */
  @SuppressWarnings("rawtypes")
  public void setEventTypesToPublish(Set<Class<? extends KeyValueEvent>> eventTypesToPublish) {
    if (CollectionUtils.isEmpty(eventTypesToPublish)) {
      this.publishEvents = false;
    } else {
      this.publishEvents = true;
      this.eventTypesToPublish = Collections.unmodifiableSet(eventTypesToPublish);
    }
  }

  @Override
  public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
    this.eventPublisher = applicationEventPublisher;
  }

  @Override
  public <T> T insert(T objectToInsert) {
    KeyValuePersistentEntity<?, ?> entity = getKeyValuePersistentEntity(objectToInsert);
    GeneratingIdAccessor generatingIdAccessor = new GeneratingIdAccessor(
        entity.getPropertyAccessor(objectToInsert),
        entity.getIdProperty(),
        identifierGenerator
    );
    Object id = generatingIdAccessor.getOrGenerateIdentifier();

    return insert(id, objectToInsert);
  }

  @Override
  public <T> T insert(Object id, T objectToInsert) {
    Assert.notNull(id, "Id for object to be inserted must not be null");
    Assert.notNull(objectToInsert, "Object to be inserted must not be null");

    String keyspace = resolveKeySpace(objectToInsert.getClass());

    potentiallyPublishEvent(KeyValueEvent.beforeInsert(id, keyspace, objectToInsert.getClass(), objectToInsert));

    execute((KeyValueCallback<Void>) adapter -> {

      if (adapter.contains(id, keyspace)) {
        throw new DuplicateKeyException(
            String.format("Cannot insert existing object with id %s; Please use update", id));
      }

      adapter.put(id, objectToInsert, keyspace);
      return null;
    });

    potentiallyPublishEvent(KeyValueEvent.afterInsert(id, keyspace, objectToInsert.getClass(), objectToInsert));

    return objectToInsert;
  }

  @Override
  public <T> T update(T objectToUpdate) {
    KeyValuePersistentEntity<?, ?> entity = getKeyValuePersistentEntity(objectToUpdate);

    if (!entity.hasIdProperty()) {
      throw new InvalidDataAccessApiUsageException(
          String.format("Cannot determine id for type %s", ClassUtils.getUserClass(objectToUpdate)));
    }

    return update(entity.getIdentifierAccessor(objectToUpdate).getRequiredIdentifier(), objectToUpdate);
  }

  @Override
  public <T> T update(Object id, T objectToUpdate) {
    Assert.notNull(id, "Id for object to be inserted must not be null");
    Assert.notNull(objectToUpdate, "Object to be updated must not be null");

    String keyspace = resolveKeySpace(objectToUpdate.getClass());

    potentiallyPublishEvent(KeyValueEvent.beforeUpdate(id, keyspace, objectToUpdate.getClass(), objectToUpdate));

    Object existing = execute(adapter -> adapter.put(id, objectToUpdate, keyspace));

    potentiallyPublishEvent(
        KeyValueEvent.afterUpdate(id, keyspace, objectToUpdate.getClass(), objectToUpdate, existing));

    return objectToUpdate;
  }

  @Override
  public <T> Optional<T> findById(Object id, Class<T> type) {
    Assert.notNull(id, "Id for object to be found must not be null");
    Assert.notNull(type, "Type to fetch must not be null");

    String keyspace = resolveKeySpace(type);

    potentiallyPublishEvent(KeyValueEvent.beforeGet(id, keyspace, type));

    T result = execute(adapter -> {
      Object value = adapter.get(id, keyspace, type);

      if (value == null || typeCheck(type, value)) {
        return type.cast(value);
      }

      return null;
    });

    potentiallyPublishEvent(KeyValueEvent.afterGet(id, keyspace, type, result));

    return Optional.ofNullable(result);
  }

  @Override
  public void delete(Class<?> type) {
    Assert.notNull(type, "Type to delete must not be null");

    String keyspace = resolveKeySpace(type);

    potentiallyPublishEvent(KeyValueEvent.beforeDropKeySpace(keyspace, type));

    execute((KeyValueCallback<Void>) adapter -> {

      adapter.deleteAllOf(keyspace);
      return null;
    });

    potentiallyPublishEvent(KeyValueEvent.afterDropKeySpace(keyspace, type));
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T delete(T objectToDelete) {
    Class<T> type = (Class<T>) ClassUtils.getUserClass(objectToDelete);
    KeyValuePersistentEntity<?, ?> entity = getKeyValuePersistentEntity(objectToDelete);
    Object id = entity.getIdentifierAccessor(objectToDelete).getIdentifier();

    if (id == null) {
      String error = String.format("Cannot determine id for type %s", ClassUtils.getUserClass(objectToDelete));

      throw new InvalidDataAccessApiUsageException(error);
    }

    return delete(id, type);
  }

  @Override
  public <T> T delete(Object id, Class<T> type) {
    Assert.notNull(id, "Id for object to be deleted must not be null");
    Assert.notNull(type, "Type to delete must not be null");

    String keyspace = resolveKeySpace(type);

    potentiallyPublishEvent(KeyValueEvent.beforeDelete(id, keyspace, type));

    T result = execute(adapter -> adapter.delete(id, keyspace, type));

    potentiallyPublishEvent(KeyValueEvent.afterDelete(id, keyspace, type, result));

    return result;
  }

  @Nullable
  @Override
  public <T> T execute(KeyValueCallback<T> action) {
    Assert.notNull(action, "KeyValueCallback must not be null");

    try {
      return action.doInKeyValue(this.adapter);
    } catch (RuntimeException e) {
      throw resolveExceptionIfPossible(e);
    }
  }

  protected <T> T executeRequired(KeyValueCallback<T> action) {
    T result = execute(action);

    if (result != null) {
      return result;
    }

    throw new IllegalStateException(String.format("KeyValueCallback %s returned null value", action));
  }

  @Override
  public <T> Iterable<T> find(KeyValueQuery<?> query, Class<T> type) {
    return executeRequired((KeyValueCallback<Iterable<T>>) adapter -> {
      Iterable<?> result = adapter.find(query, resolveKeySpace(type), type);

      List<T> filtered = new ArrayList<>();

      for (Object candidate : result) {
        if (typeCheck(type, candidate)) {
          filtered.add(type.cast(candidate));
        }
      }

      return filtered;
    });
  }

  @Override
  public <T> Iterable<T> findAll(Class<T> type) {
    Assert.notNull(type, "Type to fetch must not be null");

    return executeRequired(adapter -> {
      Iterable<?> values = adapter.getAllOf(resolveKeySpace(type), type);

      ArrayList<T> filtered = new ArrayList<>();
      for (Object candidate : values) {
        if (typeCheck(type, candidate)) {
          filtered.add(type.cast(candidate));
        }
      }

      return filtered;
    });
  }

  @SuppressWarnings("rawtypes")
  @Override
  public <T> Iterable<T> findAll(Sort sort, Class<T> type) {
    return find(new KeyValueQuery(sort), type);
  }

  @SuppressWarnings("rawtypes")
  @Override
  public <T> Iterable<T> findInRange(long offset, int rows, Class<T> type) {
    return find(new KeyValueQuery().skip(offset).limit(rows), type);
  }

  @SuppressWarnings("rawtypes")
  @Override
  public <T> Iterable<T> findInRange(long offset, int rows, Sort sort, Class<T> type) {
    return find(new KeyValueQuery(sort).skip(offset).limit(rows), type);
  }

  @Override
  public long count(Class<?> type) {
    Assert.notNull(type, "Type for count must not be null");
    return adapter.count(resolveKeySpace(type));
  }

  @Override
  public long count(KeyValueQuery<?> query, Class<?> type) {
    return executeRequired(adapter -> adapter.count(query, resolveKeySpace(type)));
  }

  @Override
  public boolean exists(KeyValueQuery<?> query, Class<?> type) {
    return executeRequired(adapter -> adapter.exists(query, resolveKeySpace(type)));
  }

  @Override
  public MappingContext<?, ?> getMappingContext() {
    return this.mappingContext;
  }

  @Override
  public KeyValueAdapter getKeyValueAdapter() {
    return adapter;
  }

  @Override
  public void destroy() throws Exception {
    this.adapter.destroy();
  }

  private KeyValuePersistentEntity<?, ?> getKeyValuePersistentEntity(Object objectToInsert) {
    return this.mappingContext.getRequiredPersistentEntity(ClassUtils.getUserClass(objectToInsert));
  }

  private String resolveKeySpace(Class<?> type) {
    return this.mappingContext.getRequiredPersistentEntity(type).getKeySpace();
  }

  private RuntimeException resolveExceptionIfPossible(RuntimeException e) {
    DataAccessException translatedException = exceptionTranslator.translateExceptionIfPossible(e);

    return translatedException != null ? translatedException : e;
  }

  @SuppressWarnings("rawtypes")
  private void potentiallyPublishEvent(KeyValueEvent event) {
    if (eventPublisher == null) {
      return;
    }

    if (publishEvents && (eventTypesToPublish.isEmpty() || eventTypesToPublish.contains(event.getClass()))) {
      eventPublisher.publishEvent(event);
    }
  }
}
