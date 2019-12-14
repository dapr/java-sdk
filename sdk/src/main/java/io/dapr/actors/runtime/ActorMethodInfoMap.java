package io.dapr.actors.runtime;

import java.lang.reflect.Method;
import java.util.HashMap;

/**
 *  Actor method dispatcher map. Holds method_name -> Method for methods defined in Actor interfaces.
 */
class ActorMethodInfoMap {
    private final HashMap<String, Method> methods;

    public <T> ActorMethodInfoMap(Iterable<Class<T>> interfaceTypes)
    {
        this.methods = new HashMap<String, Method>();

        // Find methods which are defined in Actor interface.
        for (Class<T> actorInterface : interfaceTypes)
        {
            for (Method methodInfo : actorInterface.getMethods())
            {
                this.methods.put(methodInfo.getName(), methodInfo);
            }
        }
    }

    public Method LookupActorMethodInfo(String methodName) throws NoSuchMethodException
    {
        Method methodInfo = this.methods.get(methodName);
        if (methodInfo == null) {
            throw new NoSuchMethodException("Actor type doesn't contain method " + methodName);
        }

        return methodInfo;
    }
}
