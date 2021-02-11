/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

import io.dapr.actors.ActorUtils;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * Contains the information about the class implementing an actor.
 */
final class ActorTypeInformation<T> {

  /**
   * Actor type's name.
   */
  private final String name;

  /**
   * Actor's implementation class.
   */
  private final Class<T> implementationClass;

  /**
   * Actor's immediate interfaces.
   */
  private final Collection<Class<?>> interfaces;

  /**
   * Whether Actor type is abstract.
   */
  private final boolean abstractClass;

  /**
   * Whether Actor type is remindable.
   */
  private final boolean remindable;

  /**
   * Instantiates a new {@link ActorTypeInformation}.
   *
   * @param name                Actor type's name.
   * @param implementationClass Actor's implementation class.
   * @param interfaces          Actor's immediate interfaces.
   * @param abstractClass       Whether Actor type is abstract.
   * @param remindable          Whether Actor type is remindable.
   */
  private ActorTypeInformation(String name,
                               Class<T> implementationClass,
                               Collection<Class<?>> interfaces,
                               boolean abstractClass,
                               boolean remindable) {
    this.name = name;
    this.implementationClass = implementationClass;
    this.interfaces = interfaces;
    this.abstractClass = abstractClass;
    this.remindable = remindable;
  }

  /**
   * Returns the name of this ActorType.
   *
   * @return ActorType's name.
   */
  public String getName() {
    return this.name;
  }

  /**
   * Gets the type of the class implementing the actor.
   *
   * @return The {@link Class} of implementing the actor.
   */
  public Class<T> getImplementationClass() {
    return this.implementationClass;
  }

  /**
   * Gets the actor interfaces that are implemented by actor class.
   *
   * @return Collection of actor interfaces.
   */
  public Collection<Class<?>> getInterfaces() {
    return Collections.unmodifiableCollection(this.interfaces);
  }

  /**
   * Gets a value indicating whether the class implementing actor is abstract.
   *
   * @return true if the class implementing actor is abstract, otherwise false.
   */
  public boolean isAbstractClass() {
    return this.abstractClass;
  }

  /**
   * Gets a value indicating whether the actor class implements
   * {@link Remindable}.
   *
   * @return true if the actor class implements {@link Remindable}.
   */
  public boolean isRemindable() {
    return this.remindable;
  }

  /**
   * Creates the {@link ActorTypeInformation} from given Class.
   *
   * @param actorClass The type of class implementing the actor to create
   *                   ActorTypeInformation for.
   * @return ActorTypeInformation if successfully created for actorType or null.
   */
  public static <T> ActorTypeInformation<T> tryCreate(Class<T> actorClass) {
    try {
      return create(actorClass);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /**
   * Creates an {@link #ActorTypeInformation} from actorType.
   *
   * @param actorClass The class implementing the actor to create
   *                   ActorTypeInformation for.
   * @return {@link #ActorTypeInformation} created from actorType.
   */
  public static <T> ActorTypeInformation<T> create(Class<T> actorClass) {
    if (!ActorTypeUtilities.isActor(actorClass)) {
      throw new IllegalArgumentException(
            String.format(
                  "The type '%s' is not an Actor. An actor class must inherit from '%s'.",
                  actorClass == null ? "" : actorClass.getCanonicalName(),
                  AbstractActor.class.getCanonicalName()));
    }

    // get all actor interfaces
    Class<?>[] actorInterfaces = actorClass.getInterfaces();

    boolean isAbstract = Modifier.isAbstract(actorClass.getModifiers());
    boolean isRemindable = ActorTypeUtilities.isRemindableActor(actorClass);

    String typeName = ActorUtils.findActorTypeName(actorClass);
    return new ActorTypeInformation(typeName, actorClass, Arrays.asList(actorInterfaces), isAbstract, isRemindable);
  }

}
