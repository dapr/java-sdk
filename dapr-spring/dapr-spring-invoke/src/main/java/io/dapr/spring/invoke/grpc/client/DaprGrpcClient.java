package io.dapr.spring.invoke.grpc.client;

import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.stub.AbstractStub;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation for fields of type {@link Channel} or subclasses of {@link AbstractStub}/gRPC client services.
 *
 * <p>
 * <b>Note:</b> Fields that are annotated with this annotation should NOT be annotated with
 * {@link Autowired} (conflict).
 * </p>
 *
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface DaprGrpcClient {

  /**
   * The name of the target service. The channel use DaprClient's grpc channel.
   *
   * <p>
   * <b>Example:</b> <code>@DaprGrpcClient("exampleService")</code> &lt;-&gt;
   * {@code headers.put(Metadata.Key.of("dapr-app-id", Metadata.ASCII_STRING_MARSHALLER), name); }
   * </p>
   *
   * @return The name of the target service.
   */
  String value();

  /**
   * A list of {@link ClientInterceptor} classes that should be used with this client in addition to the globally
   * defined ones. If a bean of the given type exists, it will be used; otherwise a new instance of that class will be
   * created via no-args constructor.
   *
   * @return A list of ClientInterceptor classes that should be used.
   */
  Class<? extends ClientInterceptor>[] interceptors() default {};

  /**
   * A list of {@link ClientInterceptor} beans that should be used with this client in addition to the globally
   * defined ones.
   *
   * @return A list of ClientInterceptor beans that should be used.
   */
  String[] interceptorNames() default {};

  /**
   * Whether the custom ClientInterceptor defined by interceptors and interceptorNames should be sorted.
   *
   * <p>
   * Sorted by
   * {@code interceptors.sort(AnnotationAwareOrderComparator.INSTANCE)}
   * </p>
   *
   *
   * @return True, if the custom interceptors will be sorted. False otherwise.
   */
  boolean sortInterceptors() default false;
}
