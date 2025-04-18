package io.dapr.spring.invoke.grpc.client;

import com.google.common.collect.Lists;
import io.dapr.client.DaprClient;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.stub.AbstractAsyncStub;
import io.grpc.stub.AbstractStub;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeansException;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Handlers Dapr gRPC client in spring boot.
 * Searches for fields and methods in beans that are annotated with {@link DaprGrpcClient} and sets them.
 *
 */
public class DaprGrpcBeanPostProcessor implements BeanPostProcessor {

  private final ApplicationContext applicationContext;

  private DaprClient daprClient;

  public DaprGrpcBeanPostProcessor(ApplicationContext applicationContext) {
    this.applicationContext =  requireNonNull(applicationContext, "applicationContext");
  }

  @Override
  public Object postProcessBeforeInitialization(final Object bean, final String beanName) throws BeansException {
    processDaprGprcClients(bean);

    return bean;
  }

  /**
   * Process Dapr gRPC client in bean.
   * @param bean The bean to process.
   */
  private void processDaprGprcClients(final Object bean) {
    Class<?> clazz = bean.getClass();
    do {
      processDaprGprcClientFields(clazz, bean);
      clazz = clazz.getSuperclass();
    } while (clazz != null);
  }

  /**
   * Process DaprGrpcClient annotation in bean.
   * @param clazz The class to process.
   * @param bean The bean to process.
   */
  private void processDaprGprcClientFields(final Class<?> clazz, final Object bean) {
    for (final Field field : clazz.getDeclaredFields()) {
      final DaprGrpcClient annotation = AnnotationUtils.findAnnotation(field, DaprGrpcClient.class);
      if (annotation != null) {
        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, bean, processDaprGrpcClientPoint(field, field.getType(), annotation));
      }
    }
  }

  /**
   * Processes DaprGrpcClient announce and computes the appropriate value for the injection.
   *
   * @param <T> The type to be injected for the given injection point.
   * @param injectionTarget The target member for the injection.
   * @param injectionType The class that should injected type.
   * @param annotation The DaprGrpcClient annotation instance.
   * @return The value to be injected for the given injection point.
   */
  private <T> T processDaprGrpcClientPoint(final Member injectionTarget, final Class<T> injectionType,
                                           final DaprGrpcClient annotation) {
    final String name = annotation.value();

    final List<ClientInterceptor> interceptors = interceptorsFromAnnotation(annotation);
    if (annotation.sortInterceptors()) {
      interceptors.sort(AnnotationAwareOrderComparator.INSTANCE);
    }

    return valueForMember(name, injectionTarget, injectionType, interceptors);
  }

  /**
   * Processes DaprGrpcClient announce with Channel or AbstractStub type.
   *
   * @param <T> The type of the instance to be injected.
   * @param <ST> The type of the AbstractStub implement class.
   * @param name The name of target service name.
   * @param injectionTarget The target member for the injection.
   * @param injectionType The class that should injected type.
   * @param interceptors The interceptors defined on DaprGrpcClient.
   * @return The value that matches the type of the given field.
   * @throws BeansException If the value of the field could not be created or the type of the field is unsupported.
   */
  protected <T, ST extends AbstractStub<ST>> T valueForMember(final String name, final Member injectionTarget,
                                                              final Class<T> injectionType,
                                                              final List<ClientInterceptor> interceptors)
      throws BeansException {
    if (Channel.class.equals(injectionType)) {
      Channel channel = getDaprClient().newGrpcStub(name, DaprGrpcBeanPostProcessor::newStub).getChannel();
      channel = ClientInterceptors.interceptForward(channel, interceptors);
      return injectionType.cast(channel);
    } else if (AbstractStub.class.isAssignableFrom(injectionType)) {
      @SuppressWarnings("unchecked")
      ST stub = getDaprClient().newGrpcStub(name, (daprChannel ->
          createStub((Class<ST>)injectionType.asSubclass(AbstractStub.class), daprChannel)));
      stub = stub.withInterceptors(interceptors.toArray(new ClientInterceptor[0]));
      return injectionType.cast(stub);
    } else {
      if (injectionTarget != null) {
        throw new InvalidPropertyException(injectionTarget.getDeclaringClass(), injectionTarget.getName(),
            "Unsupported type " + injectionType.getName());
      } else {
        throw new BeanInstantiationException(injectionType, "Unsupported grpc stub or channel type");
      }
    }
  }

  /**
   * Create a new stub by stubType and channel.
   *
   * @param stubClass The stub class that needs to be created.
   * @param channel The gRPC channel associated with the created stub, passed as a parameter to the stub factory.
   * @return A newly created gRPC stub.
   */
  private <T extends AbstractStub<T>> T createStub(final Class<T> stubClass, final Channel channel) {
    try {
      // Search for public static *Grpc#new*Stub(Channel)
      final Class<?> declaringClass = stubClass.getDeclaringClass();
      if (declaringClass != null) {
        for (final Method method : declaringClass.getMethods()) {
          final String name = method.getName();
          final int modifiers = method.getModifiers();
          final Parameter[] parameters = method.getParameters();
          if (name.startsWith("new") && name.endsWith("Stub")
              && Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers)
              && method.getReturnType().isAssignableFrom(stubClass)
              && parameters.length == 1
              && Channel.class.equals(parameters[0].getType())) {
            return stubClass.cast(method.invoke(null, channel));
          }
        }
      }

      // Search for a public constructor *Stub(Channel)
      final Constructor<T> constructor = stubClass.getConstructor(Channel.class);
      return constructor.newInstance(channel);
    } catch (final Exception e) {
      throw new BeanInstantiationException(stubClass, "Failed to create gRPC client", e);
    }
  }

  /**
   * Create a new stub by stubType and channel.
   *
   * @param channel The grpc channel.
   * @return The transparent stub instance.
   */
  private static DaprTransparentStub newStub(final Channel channel) {
    AbstractStub.StubFactory<DaprTransparentStub> factory = new AbstractStub.StubFactory<DaprTransparentStub>() {
      public DaprTransparentStub newStub(final Channel channel, final CallOptions callOptions) {
        return new DaprTransparentStub(channel, callOptions);
      }
    };
    return DaprTransparentStub.newStub(factory, channel);
  }

  /**
   * A transparent stub that for DaprClient get channel.
   */
  private static class DaprTransparentStub extends AbstractAsyncStub<DaprTransparentStub> {
    private DaprTransparentStub(final Channel channel, final CallOptions callOptions) {
      super(channel, callOptions);
    }

    protected DaprTransparentStub build(final Channel channel, final CallOptions callOptions) {
      return new DaprTransparentStub(channel, callOptions);
    }
  }

  /**
   * Gets or creates the {@link ClientInterceptor}s that are referenced in the given annotation.
   *
   * @param annotation The annotation to get the interceptors for.
   * @return A list containing the interceptors for the given annotation.
   * @throws BeansException If the referenced interceptors weren't found or could not be created.
   */
  protected List<ClientInterceptor> interceptorsFromAnnotation(final DaprGrpcClient annotation) throws BeansException {
    final List<ClientInterceptor> list = Lists.newArrayList();
    for (final Class<? extends ClientInterceptor> interceptorClass : annotation.interceptors()) {
      final ClientInterceptor clientInterceptor;
      if (this.applicationContext.getBeanNamesForType(interceptorClass).length > 0) {
        clientInterceptor = this.applicationContext.getBean(interceptorClass);
      } else {
        try {
          clientInterceptor = interceptorClass.getConstructor().newInstance();
        } catch (final Exception e) {
          throw new BeanCreationException("Failed to create interceptor instance", e);
        }
      }
      list.add(clientInterceptor);
    }
    for (final String interceptorName : annotation.interceptorNames()) {
      list.add(this.applicationContext.getBean(interceptorName, ClientInterceptor.class));
    }
    return list;
  }

  private DaprClient getDaprClient() {
    if (this.daprClient == null) {
      this.daprClient = this.applicationContext.getBean(DaprClient.class);
    }
    return this.daprClient;
  }
}
