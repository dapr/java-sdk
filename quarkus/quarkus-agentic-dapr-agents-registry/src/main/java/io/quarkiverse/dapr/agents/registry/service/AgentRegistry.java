package io.quarkiverse.dapr.agents.registry.service;

import io.dapr.client.DaprClient;
import io.quarkiverse.dapr.agents.registry.model.AgentMetadata;
import io.quarkiverse.dapr.agents.registry.model.AgentMetadataSchema;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class AgentRegistry {

  private static final Logger LOG = Logger.getLogger(AgentRegistry.class);

  /** Fully-qualified name of langchain4j {@code @Agent} annotation. */
  private static final String AGENT_ANNOTATION_NAME = "dev.langchain4j.agentic.Agent";
  /** Fully-qualified name of langchain4j {@code @SystemMessage} annotation. */
  private static final String SYSTEM_MESSAGE_ANNOTATION_NAME = "dev.langchain4j.service.SystemMessage";

  @Inject
  DaprClient client;

  @Inject
  BeanManager beanManager;

  @ConfigProperty(name = "dapr.agents.statestore", defaultValue = "kvstore")
  String statestore;

  @ConfigProperty(name = "dapr.appid", defaultValue = "local-dapr-app")
  String appId;

  @ConfigProperty(name = "dapr.agents.team", defaultValue = "default")
  String team;

  void onStartup(@Observes StartupEvent event) {
    discoverAndRegisterAgents();
  }

  void discoverAndRegisterAgents() {
    LOG.info("Starting agent auto-discovery...");
    Set<Bean<?>> beans = beanManager.getBeans(Object.class, Any.Literal.INSTANCE);
    LOG.debugf("Found %d CDI beans to scan", beans.size());

    int registered = 0;
    int failed = 0;
    Set<String> scannedInterfaces = new HashSet<>();
    // Collect all interface classes from CDI beans first
    List<Class<?>> interfacesToScan = new ArrayList<>();

    for (Bean<?> bean : beans) {
      for (Type type : bean.getTypes()) {
        if (type instanceof Class<?> clazz && clazz.isInterface()) {
          if (scannedInterfaces.add(clazz.getName())) {
            interfacesToScan.add(clazz);
          }
        }
      }
    }

    // Also discover sub-agent classes referenced by composite agent annotations
    // (e.g., @SequenceAgent(subAgents = {CreativeWriter.class, StyleEditor.class})).
    // Sub-agents are often not CDI beans themselves, so they won't appear in BeanManager.
    List<Class<?>> subAgentClasses = new ArrayList<>();
    for (Class<?> iface : interfacesToScan) {
      for (Method method : iface.getDeclaredMethods()) {
        for (Annotation ann : method.getDeclaredAnnotations()) {
          for (Class<?> subAgent : extractSubAgentClasses(ann)) {
            if (subAgent.isInterface() && scannedInterfaces.add(subAgent.getName())) {
              subAgentClasses.add(subAgent);
              LOG.debugf("Discovered sub-agent interface %s from %s on %s",
                  subAgent.getName(), ann.annotationType().getSimpleName(), iface.getName());
            }
          }
        }
      }
    }
    interfacesToScan.addAll(subAgentClasses);

    // Scan all collected interfaces for @Agent methods
    for (Class<?> iface : interfacesToScan) {
      LOG.debugf("Scanning interface: %s", iface.getName());
      List<AgentMetadataSchema> agents = scanForAgents(iface, appId);
      if (!agents.isEmpty()) {
        LOG.debugf("Found %d @Agent method(s) on interface %s", agents.size(), iface.getName());
      }
      for (AgentMetadataSchema schema : agents) {
        try {
          registerAgent(schema);
          registered++;
        } catch (Exception e) {
          failed++;
          LOG.errorf(e, "Failed to register agent '%s' in state store '%s': %s",
              schema.getName(), statestore, e.getMessage());
        }
      }
    }

    LOG.debugf("Scanned %d unique interfaces", scannedInterfaces.size());

    if (registered == 0 && failed == 0) {
      LOG.warn("No @Agent-annotated methods found on any CDI bean interface. "
          + "Ensure your @Agent interfaces are registered as CDI beans.");
    } else if (failed > 0) {
      LOG.warnf("Agent discovery complete: %d registered, %d failed", registered, failed);
    } else {
      LOG.infof("Agent discovery complete: %d agent(s) registered successfully", registered);
    }
  }

  /**
   * Extracts sub-agent classes from a composite agent annotation.
   * <p>
   * Looks for a {@code subAgents()} method returning {@code Class<?>[]} on the annotation.
   * This works for any composite agent annotation (e.g., {@code @SequenceAgent},
   * {@code @ParallelAgent}, etc.) without coupling to specific annotation types.
   */
  static Class<?>[] extractSubAgentClasses(Annotation ann) {
    try {
      Method subAgentsMethod = ann.annotationType().getMethod("subAgents");
      Object result = subAgentsMethod.invoke(ann);
      if (result instanceof Class<?>[] classes) {
        return classes;
      }
    } catch (NoSuchMethodException e) {
      // Not a composite agent annotation — expected for most annotations
    } catch (Exception e) {
      LOG.debugf("Failed to extract subAgents from %s: %s",
          ann.annotationType().getSimpleName(), e.getMessage());
    }
    return new Class<?>[0];
  }

  /**
   * Scans an interface for methods annotated with {@code @Agent} and extracts metadata.
   * <p>
   * Uses name-based annotation matching ({@code annotationType().getName()}) instead of
   * class identity ({@code method.getAnnotation(Agent.class)}) to handle classloader
   * differences between library JARs and the Quarkus application classloader.
   */
  static List<AgentMetadataSchema> scanForAgents(Class<?> type, String appId) {
    List<AgentMetadataSchema> result = new ArrayList<>();
    for (Method method : type.getDeclaredMethods()) {
      Annotation agentAnn = findAnnotationByName(method, AGENT_ANNOTATION_NAME);
      if (agentAnn == null) {
        continue;
      }

      String name = invokeStringMethod(agentAnn, "name");
      if (name == null || name.isBlank()) {
        name = type.getSimpleName() + "." + method.getName();
      }

      String goal = invokeStringMethod(agentAnn, "description");

      String systemPrompt = null;
      Annotation smAnn = findAnnotationByName(method, SYSTEM_MESSAGE_ANNOTATION_NAME);
      if (smAnn != null) {
        String[] values = invokeMethod(smAnn, "value", String[].class);
        String delimiter = invokeStringMethod(smAnn, "delimiter");
        if (values != null && values.length > 0) {
          String joined = String.join(delimiter != null ? delimiter : "\n", values);
          if (!joined.isBlank()) {
            systemPrompt = joined;
          }
        }
      }

      AgentMetadataSchema schema = AgentMetadataSchema.builder()
          .schemaVersion("0.11.1")
          .name(name)
          .registeredAt(Instant.now().toString())
          .agent(AgentMetadata.builder()
              .appId(appId)
              .type("standalone")
              .goal(goal)
              .systemPrompt(systemPrompt)
              .framework("langchain4j")
              .build())
          .build();

      result.add(schema);
    }
    return result;
  }

  /**
   * Finds an annotation on a method by its fully-qualified type name.
   * This is resilient against classloader mismatches where the same annotation class
   * may be loaded by different classloaders.
   */
  private static Annotation findAnnotationByName(Method method, String annotationName) {
    for (Annotation ann : method.getDeclaredAnnotations()) {
      if (ann.annotationType().getName().equals(annotationName)) {
        return ann;
      }
    }
    return null;
  }

  /** Invokes a no-arg method on an annotation proxy and returns the result as a String. */
  private static String invokeStringMethod(Annotation ann, String methodName) {
    return invokeMethod(ann, methodName, String.class);
  }

  /** Invokes a no-arg method on an annotation proxy, casting to the expected type. */
  @SuppressWarnings("unchecked")
  private static <T> T invokeMethod(Annotation ann, String methodName, Class<T> returnType) {
    try {
      Object result = ann.annotationType().getMethod(methodName).invoke(ann);
      return returnType.isInstance(result) ? (T) result : null;
    } catch (Exception e) {
      LOG.debugf("Failed to invoke %s.%s(): %s", ann.annotationType().getSimpleName(), methodName, e.getMessage());
      return null;
    }
  }

  public void registerAgent(AgentMetadataSchema schema) {
    String key = "agents:" + team + ":" + schema.getName();
    LOG.infof("Registering agent: %s", key);
    client.saveState(statestore, key, null, schema,
        Map.of("contentType", "application/json"), null).block();
  }
}
