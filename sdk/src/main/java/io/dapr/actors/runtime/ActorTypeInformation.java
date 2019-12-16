/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.actors.runtime;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * Contains the information about the class implementing an actor.
 */
final class ActorTypeInformation {

  /**
   * Actor type's name.
   */
  private final String name;

  /**
   * Actor's implementation class.
   */
  private final Class implementationClass;

  /**
   * Actor's immediate interfaces.
   */
  private final Collection<Class> interfaces;

  /**
   * Whether Actor type is abstract.
   */
  private final boolean abstractClass;

  /**
   * Whether Actor type is remindable.
   */
  private final boolean remindable;

  /**
   * Instantiates a new {@link ActorTypeInformation}
   *
   * @param name Actor type's name.
   * @param implementationClass Actor's implementation class.
   * @param interfaces Actor's immediate interfaces.
   * @param abstractClass Whether Actor type is abstract.
   * @param remindable Whether Actor type is remindable.
   */
  private ActorTypeInformation(String name,
      Class implementationClass,
      Collection<Class> interfaces,
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
  public Class getImplementationClass() {
    return this.implementationClass;
  }

  /**
   * Gets the actor interfaces which derive from {@link Actor} and implemented
   * by actor class.
   *
   * @return Collection of actor interfaces.
   */
  public Collection<Class> getInterfaces() {
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
   * ActorTypeInformation for.
   * @return ActorTypeInformation if successfully created for actorType or null.
   */
  public static ActorTypeInformation tryCreate(Class actorClass) {
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
   * ActorTypeInformation for.
   * @return {@link #ActorTypeInformation} created from actorType.
   */
  public static ActorTypeInformation create(Class actorClass) {
    if (!ActorTypeUtilities.isActor(actorClass)) {
      throw new IllegalArgumentException(
          String.format(
              "The type '%s' is not an Actor. An actor type must derive from '%s'.",
              actorClass == null ? "" : actorClass.getCanonicalName(),
              Actor.class.getCanonicalName()));
    }

    // get all actor interfaces
    Class<?>[] actorInterfaces = actorClass.getInterfaces();

    boolean isAbstract = Modifier.isAbstract(actorClass.getModifiers());
    // ensure that the if the actor type is not abstract it implements at least one actor interface
    if ((actorInterfaces.length == 0) && !isAbstract) {
      throw new IllegalArgumentException(
          String.format(
              "The actor type '%s' does not implement any actor interfaces or one of the "
              + "interfaces implemented is not an actor interface. "
              + "All interfaces(including its parent interface) implemented by actor type must "
              + "be actor interface. An actor interface is the one that ultimately derives "
              + "from '%s' type.",
              actorClass == null ? "" : actorClass.getCanonicalName(),
              Actor.class.getCanonicalName()));
    }

    boolean isRemindable = ActorTypeUtilities.isRemindableActor(actorClass);
    ActorType actorTypeAnnotation = (ActorType) actorClass.getAnnotation(ActorType.class);
    String typeName = actorTypeAnnotation != null ? actorTypeAnnotation.Name() : actorClass.getSimpleName();

    return new ActorTypeInformation(typeName, actorClass, Arrays.asList(actorInterfaces), isAbstract, isRemindable);
  }

}
