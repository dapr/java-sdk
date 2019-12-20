package io.dapr.actors.runtime;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Actor method dispatcher map. Holds method_name -> Method for methods defined in Actor interfaces.
 */
class ActorMethodInfoMap {
  /**
   * Map for methods based on name.
   */
  private final Map<String, Method> methods;

  ActorMethodInfoMap(Iterable<Class<?>> interfaceTypes) {
    Map<String, Method> methods = new HashMap<>();

    // Find methods which are defined in Actor interface.
    for (Class<?> actorInterface : interfaceTypes) {
      for (Method methodInfo : actorInterface.getMethods()) {
        // Only support methods with 1 or 0 argument.
        if (methodInfo.getParameterCount() <= 1) {
          // If Actor class uses overloading, then one will win.
          // Document this behavior, so users know how to write their code.
          methods.put(methodInfo.getName(), methodInfo);
        }
      }
    }

    this.methods = Collections.unmodifiableMap(methods);
  }

  Method get(String methodName) throws NoSuchMethodException {
    Method method = this.methods.get(methodName);
    if (method == null) {
      throw new NoSuchMethodException(String.format("Could not find method %s.", methodName));
    }

    return method;
  }
}
