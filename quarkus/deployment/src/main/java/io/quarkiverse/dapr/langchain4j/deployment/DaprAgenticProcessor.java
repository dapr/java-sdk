/*
 * Copyright 2025 The Dapr Authors
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

package io.quarkiverse.dapr.langchain4j.deployment;

import io.quarkiverse.dapr.deployment.items.WorkflowItemBuildItem;
import io.quarkiverse.dapr.langchain4j.agent.AgentRunLifecycleManager;
import io.quarkiverse.dapr.langchain4j.agent.DaprAgentMetadataHolder;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

/**
 * Quarkus deployment processor for the Dapr Agentic extension.
 *
 * <p>{@code DaprWorkflowProcessor.searchWorkflows()} uses {@code ApplicationIndexBuildItem}
 * which only indexes application classes -- extension runtime JARs are invisible to it.
 * We fix this in two steps:
 * <ol>
 *   <li>Produce an {@link IndexDependencyBuildItem} so our runtime JAR is indexed into
 *       the {@link CombinedIndexBuildItem} (and visible to Arc for CDI bean discovery).</li>
 *   <li>Consume the {@link CombinedIndexBuildItem}, look up our Workflow and WorkflowActivity
 *       classes, and produce {@link WorkflowItemBuildItem} instances that the existing
 *       {@code DaprWorkflowProcessor} build steps consume to register with the Dapr
 *       workflow runtime.</li>
 *   <li>Produce {@link AdditionalBeanBuildItem} instances so Arc explicitly discovers
 *       our Workflow and WorkflowActivity classes as CDI beans.</li>
 *   <li>Apply {@code @DaprAgentToolInterceptorBinding} to all {@code @Tool}-annotated
 *       methods automatically so that {@code DaprToolCallInterceptor} routes those calls
 *       through Dapr Workflow Activities without requiring user code changes.</li>
 *   <li>Generate a CDI {@code @Decorator} for every {@code @Agent}-annotated interface
 *       so the {@link AgentRunLifecycleManager} workflow is started at the very beginning
 *       of the agent method call -- before LangChain4j assembles the prompt -- giving Dapr
 *       full observability of the agent's lifecycle from its first instruction.</li>
 * </ol>
 */
public class DaprAgenticProcessor {

  private static final Logger LOG = Logger.getLogger(DaprAgenticProcessor.class);

  private static final String FEATURE = "dapr-agentic";

  /**
   * Generated decorator classes live in this package to avoid polluting user packages.
   */
  private static final String DECORATOR_PACKAGE = "io.quarkiverse.dapr.langchain4j.generated";

  /**
   * LangChain4j {@code @Tool} annotation (on CDI bean methods).
   */
  private static final DotName TOOL_ANNOTATION =
      DotName.createSimple("dev.langchain4j.agent.tool.Tool");

  /**
   * LangChain4j {@code @Agent} annotation (on AiService interface methods).
   */
  private static final DotName AGENT_ANNOTATION =
      DotName.createSimple("dev.langchain4j.agentic.Agent");

  /**
   * LangChain4j {@code @UserMessage} annotation.
   */
  private static final DotName USER_MESSAGE_ANNOTATION =
      DotName.createSimple("dev.langchain4j.service.UserMessage");

  /**
   * LangChain4j {@code @SystemMessage} annotation.
   */
  private static final DotName SYSTEM_MESSAGE_ANNOTATION =
      DotName.createSimple("dev.langchain4j.service.SystemMessage");

  /**
   * Our interceptor binding that triggers {@code DaprToolCallInterceptor}.
   */
  private static final DotName DAPR_TOOL_INTERCEPTOR_BINDING = DotName.createSimple(
      "io.quarkiverse.dapr.langchain4j.agent.DaprAgentToolInterceptorBinding");

  /**
   * Our interceptor binding that triggers {@code DaprAgentMethodInterceptor}.
   */
  private static final DotName DAPR_AGENT_INTERCEPTOR_BINDING = DotName.createSimple(
      "io.quarkiverse.dapr.langchain4j.agent.DaprAgentInterceptorBinding");

  /**
   * {@code @WorkflowMetadata} annotation for custom workflow registration names.
   */
  private static final DotName WORKFLOW_METADATA_DOTNAME = DotName.createSimple(
      "io.quarkiverse.dapr.workflows.WorkflowMetadata");

  /**
   * {@code @ActivityMetadata} annotation for custom activity registration names.
   */
  private static final DotName ACTIVITY_METADATA_DOTNAME = DotName.createSimple(
      "io.quarkiverse.dapr.workflows.ActivityMetadata");

  private static final String[] WORKFLOW_CLASSES = {
      "io.quarkiverse.dapr.langchain4j.workflow.orchestration.SequentialOrchestrationWorkflow",
      "io.quarkiverse.dapr.langchain4j.workflow.orchestration.ParallelOrchestrationWorkflow",
      "io.quarkiverse.dapr.langchain4j.workflow.orchestration.LoopOrchestrationWorkflow",
      "io.quarkiverse.dapr.langchain4j.workflow.orchestration.ConditionalOrchestrationWorkflow",
      // Per-agent workflow (one per @Agent invocation)
      "io.quarkiverse.dapr.langchain4j.agent.workflow.AgentRunWorkflow",
  };

  private static final String[] ACTIVITY_CLASSES = {
      "io.quarkiverse.dapr.langchain4j.workflow.orchestration.activities.AgentExecutionActivity",
      "io.quarkiverse.dapr.langchain4j.workflow.orchestration.activities.ExitConditionCheckActivity",
      "io.quarkiverse.dapr.langchain4j.workflow.orchestration.activities.ConditionCheckActivity",
      // Per-tool-call activity
      "io.quarkiverse.dapr.langchain4j.agent.activities.ToolCallActivity",
      // Per-LLM-call activity
      "io.quarkiverse.dapr.langchain4j.agent.activities.LlmCallActivity",
  };

  @BuildStep
  FeatureBuildItem feature() {
    return new FeatureBuildItem(FEATURE);
  }

  /**
   * Index our runtime JAR so its classes appear in {@link CombinedIndexBuildItem}
   * and are discoverable by Arc for CDI bean creation.
   */
  @BuildStep
  IndexDependencyBuildItem indexRuntimeModule() {
    return new IndexDependencyBuildItem("io.quarkiverse.dapr", "quarkus-agentic-dapr");
  }

  /**
   * Produce {@link WorkflowItemBuildItem} for each of our Workflow and WorkflowActivity
   * classes.
   */
  @BuildStep
  void registerWorkflowsAndActivities(CombinedIndexBuildItem combinedIndex,
      BuildProducer<WorkflowItemBuildItem> workflowItems) {
    IndexView index = combinedIndex.getIndex();

    for (String className : WORKFLOW_CLASSES) {
      ClassInfo classInfo = index.getClassByName(DotName.createSimple(className));
      if (classInfo != null) {
        String regName = null;
        String version = null;
        Boolean isLatest = null;
        AnnotationInstance meta = classInfo.annotation(WORKFLOW_METADATA_DOTNAME);
        if (meta != null) {
          regName = stringValueOrNull(meta, "name");
          version = stringValueOrNull(meta, "version");
          AnnotationValue isLatestVal = meta.value("isLatest");
          if (isLatestVal != null) {
            isLatest = isLatestVal.asBoolean();
          }
        }
        workflowItems.produce(new WorkflowItemBuildItem(
            classInfo, WorkflowItemBuildItem.Type.WORKFLOW, regName, version, isLatest));
      }
    }

    for (String className : ACTIVITY_CLASSES) {
      ClassInfo classInfo = index.getClassByName(DotName.createSimple(className));
      if (classInfo != null) {
        String regName = null;
        AnnotationInstance meta = classInfo.annotation(ACTIVITY_METADATA_DOTNAME);
        if (meta != null) {
          regName = stringValueOrNull(meta, "name");
        }
        workflowItems.produce(new WorkflowItemBuildItem(
            classInfo, WorkflowItemBuildItem.Type.WORKFLOW_ACTIVITY, regName, null, null));
      }
    }
  }

  /**
   * Explicitly register our Workflow, WorkflowActivity, and CDI interceptor classes as beans.
   */
  @BuildStep
  void registerAdditionalBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
    for (String className : WORKFLOW_CLASSES) {
      additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(className));
    }
    for (String className : ACTIVITY_CLASSES) {
      additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(className));
    }
    // AgentRunLifecycleManager is injected by generated decorators and must be discoverable.
    additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(
        AgentRunLifecycleManager.class.getName()));
    // CDI interceptors must be registered as unremovable beans.
    additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(
        "io.quarkiverse.dapr.langchain4j.agent.DaprToolCallInterceptor"));
    additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(
        "io.quarkiverse.dapr.langchain4j.agent.DaprAgentMethodInterceptor"));
    // CDI decorator that wraps ChatModel to route LLM calls through Dapr activities.
    // A decorator is used instead of an interceptor because quarkus-langchain4j registers
    // ChatModel as a synthetic bean, and Arc does not apply CDI interceptors to synthetic
    // beans via AnnotationsTransformer -- but it DOES apply decorators at the type level.
    additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(
        "io.quarkiverse.dapr.langchain4j.agent.DaprChatModelDecorator"));
  }

  /**
   * Generates a CDI {@code @Decorator} for every interface that declares at least one
   * {@code @Agent}-annotated method.
   *
   * <p><h3>Why a generated decorator?</h3>
   * quarkus-langchain4j registers {@code @Agent} AiService beans as <em>synthetic beans</em>
   * (via {@code SyntheticBeanBuildItem}) -- CDI interceptors applied via
   * {@code AnnotationsTransformer} are silently ignored on synthetic beans.  CDI
   * <em>decorators</em>, however, are matched at the <em>bean type</em> level and are applied
   * by Arc to all beans (including synthetic beans) whose type includes the delegate type.
   * This is the same mechanism used by
   * {@link io.quarkiverse.dapr.langchain4j.agent.DaprChatModelDecorator}
   * to wrap the synthetic {@code ChatModel} bean.
   *
   * <p><h3>What the generated decorator does</h3>
   * For each interface {@code I} with at least one {@code @Agent} method, Gizmo emits a class
   * equivalent to:
   * <pre>{@code
   * @Decorator @Priority(APPLICATION) @Dependent
   * class DaprDecorator_I implements I {
   *   @Inject @Delegate @Any I delegate;
   *   @Inject AgentRunLifecycleManager lifecycleManager;
   *
   *   @Override
   *   ReturnType agentMethod(Params...) {
   *     lifecycleManager.getOrActivate(agentName, userMessage, systemMessage);
   *     try {
   *       ReturnType result = delegate.agentMethod(params);
   *       lifecycleManager.triggerDone();
   *       return result;
   *     } catch (Throwable t) {
   *       lifecycleManager.triggerDone();
   *       throw t;
   *     }
   *   }
   *   // non-@Agent abstract methods: pure delegation to delegate
   * }
   * }</pre>
   *
   * <p>Non-{@code @Agent} abstract methods are delegated transparently. Static and default
   * (non-abstract) interface methods are not overridden.
   */
  @BuildStep
  void generateAgentDecorators(
      CombinedIndexBuildItem combinedIndex,
      BuildProducer<GeneratedBeanBuildItem> generatedBeans) {

    IndexView index = combinedIndex.getIndex();
    ClassOutput classOutput = new GeneratedBeanGizmoAdaptor(generatedBeans);

    Set<DotName> processedInterfaces = new HashSet<>();

    for (AnnotationInstance agentAnnotation : index.getAnnotations(AGENT_ANNOTATION)) {
      if (agentAnnotation.target().kind() != AnnotationTarget.Kind.METHOD) {
        continue;
      }

      ClassInfo declaringClass = agentAnnotation.target().asMethod().declaringClass();

      // Only generate decorators for interfaces.
      // CDI bean classes with @Agent methods are handled by DaprAgentMethodInterceptor.
      if (!declaringClass.isInterface()) {
        continue;
      }

      if (!processedInterfaces.add(declaringClass.name())) {
        continue; // one decorator per interface
      }

      generateDecorator(classOutput, index, declaringClass);
    }
  }

  // -------------------------------------------------------------------------
  // Decorator generation helpers
  // -------------------------------------------------------------------------

  private void generateDecorator(ClassOutput classOutput, IndexView index,
      ClassInfo agentInterface) {
    String interfaceName = agentInterface.name().toString();

    // Use the fully-qualified interface name (dots replaced by underscores) so two
    // interfaces with the same simple name in different packages never collide.
    String decoratorClassName = DECORATOR_PACKAGE + ".DaprDecorator_"
        + interfaceName.replace('.', '_');

    LOG.debugf("Generating CDI decorator %s for @Agent interface %s",
        decoratorClassName, interfaceName);

    try (ClassCreator cc = ClassCreator.builder()
        .classOutput(classOutput)
        .className(decoratorClassName)
        .interfaces(interfaceName)
        .build()) {

      // --- class-level CDI annotations ---
      cc.addAnnotation(Decorator.class);
      cc.addAnnotation(Dependent.class);
      cc.addAnnotation(Priority.class)
          .addValue("value", Interceptor.Priority.APPLICATION);

      // --- @Inject @Delegate @Any InterfaceType delegate ---
      FieldCreator delegateField = cc.getFieldCreator("delegate", interfaceName);
      delegateField.setModifiers(Modifier.PROTECTED);
      delegateField.addAnnotation(Inject.class);
      delegateField.addAnnotation(Delegate.class);
      delegateField.addAnnotation(Any.class);

      // --- @Inject AgentRunLifecycleManager lifecycleManager ---
      FieldCreator lcmField = cc.getFieldCreator("lifecycleManager",
          AgentRunLifecycleManager.class.getName());
      lcmField.setModifiers(Modifier.PRIVATE);
      lcmField.addAnnotation(Inject.class);

      FieldDescriptor delegateDesc = delegateField.getFieldDescriptor();
      FieldDescriptor lcmDesc = lcmField.getFieldDescriptor();

      // --- method overrides ---
      // Collect all abstract methods declared directly on this interface.
      // Inherited abstract methods from parent interfaces are intentionally skipped:
      // CDI decorators are allowed to be "partial" (not implement every inherited
      // method); Arc will delegate un-overridden abstract methods to the next
      // decorator/bean in the chain automatically.
      for (MethodInfo method : agentInterface.methods()) {
        // Skip static and default (non-abstract) interface methods.
        if (Modifier.isStatic(method.flags())
            || !Modifier.isAbstract(method.flags())) {
          continue;
        }

        if (method.hasAnnotation(AGENT_ANNOTATION)) {
          generateDecoratedAgentMethod(cc, method, delegateDesc, lcmDesc);
        } else {
          generateDelegateMethod(cc, method, delegateDesc);
        }
      }
    }
  }

  /**
   * Generates the body for an {@code @Agent}-annotated method.
   * <pre>
   *   lifecycleManager.getOrActivate(agentName, userMsg, sysMsg);
   *   try {
   *     [result =] delegate.method(params);
   *     lifecycleManager.triggerDone();
   *     return [result];           // or returnVoid()
   *   } catch (Throwable t) {
   *     lifecycleManager.triggerDone();
   *     throw t;
   *   }
   * </pre>
   */
  private void generateDecoratedAgentMethod(ClassCreator cc, MethodInfo method,
      FieldDescriptor delegateDesc, FieldDescriptor lcmDesc) {

    String agentName = extractAgentName(method);
    String userMessage = extractAnnotationText(method, USER_MESSAGE_ANNOTATION);
    String systemMessage = extractAnnotationText(method, SYSTEM_MESSAGE_ANNOTATION);
    final boolean isVoid = method.returnType().kind() == Type.Kind.VOID;

    MethodCreator mc = cc.getMethodCreator(MethodDescriptor.of(method));
    mc.setModifiers(Modifier.PUBLIC);
    for (Type exType : method.exceptions()) {
      mc.addException(exType.name().toString());
    }

    // Store @Agent metadata on the current thread so that DaprChatModelDecorator can
    // retrieve the real agent name and messages if the activation below fails and the
    // decorator falls through to direct delegation (lazy-activation path).
    mc.invokeStaticMethod(
        MethodDescriptor.ofMethod(DaprAgentMetadataHolder.class, "set",
            void.class, String.class, String.class, String.class),
        mc.load(agentName),
        userMessage != null ? mc.load(userMessage) : mc.loadNull(),
        systemMessage != null ? mc.load(systemMessage) : mc.loadNull());

    // Try to activate the Dapr agent lifecycle. This may fail when running on
    // threads without a CDI request scope (e.g., LangChain4j's parallel executor).
    // In that case, fall through to a direct delegate call without Dapr routing.
    TryBlock activateTry = mc.tryBlock();
    ResultHandle lcm = activateTry.readInstanceField(lcmDesc, activateTry.getThis());
    activateTry.invokeVirtualMethod(
        MethodDescriptor.ofMethod(AgentRunLifecycleManager.class, "getOrActivate",
            String.class, String.class, String.class, String.class),
        lcm,
        activateTry.load(agentName),
        userMessage != null
            ? activateTry.load(userMessage) : activateTry.loadNull(),
        systemMessage != null
            ? activateTry.load(systemMessage) : activateTry.loadNull());

    // If activation fails (no request scope), delegate directly without Dapr routing.
    CatchBlockCreator activateCatch = activateTry.addCatch(Throwable.class);
    {
      ResultHandle delFallback = activateCatch.readInstanceField(
          delegateDesc, activateCatch.getThis());
      ResultHandle[] fallbackParams = new ResultHandle[method.parametersCount()];
      for (int i = 0; i < fallbackParams.length; i++) {
        fallbackParams[i] = activateCatch.getMethodParam(i);
      }
      if (isVoid) {
        activateCatch.invokeInterfaceMethod(
            MethodDescriptor.of(method), delFallback, fallbackParams);
        activateCatch.returnVoid();
      } else {
        ResultHandle fallbackResult = activateCatch.invokeInterfaceMethod(
            MethodDescriptor.of(method), delFallback, fallbackParams);
        activateCatch.returnValue(fallbackResult);
      }
    }

    // Activation succeeded -- wrap the delegate call with triggerDone() on both paths.
    // try { ... } catch (Throwable t) { ... }
    TryBlock tryBlock = mc.tryBlock();

    ResultHandle del = tryBlock.readInstanceField(delegateDesc, tryBlock.getThis());
    ResultHandle[] params = new ResultHandle[method.parametersCount()];
    for (int i = 0; i < params.length; i++) {
      params[i] = tryBlock.getMethodParam(i);
    }

    // Normal path: delegate call + triggerDone + return
    ResultHandle result = null;
    if (!isVoid) {
      result = tryBlock.invokeInterfaceMethod(
          MethodDescriptor.of(method), del, params);
    } else {
      tryBlock.invokeInterfaceMethod(MethodDescriptor.of(method), del, params);
    }

    ResultHandle lcmInTry = tryBlock.readInstanceField(lcmDesc, tryBlock.getThis());
    tryBlock.invokeVirtualMethod(
        MethodDescriptor.ofMethod(
            AgentRunLifecycleManager.class, "triggerDone", void.class),
        lcmInTry);
    tryBlock.invokeStaticMethod(
        MethodDescriptor.ofMethod(DaprAgentMetadataHolder.class, "clear", void.class));

    if (isVoid) {
      tryBlock.returnVoid();
    } else {
      tryBlock.returnValue(result);
    }

    // Exception path: triggerDone + rethrow
    CatchBlockCreator catchBlock = tryBlock.addCatch(Throwable.class);
    ResultHandle lcmInCatch =
        catchBlock.readInstanceField(lcmDesc, catchBlock.getThis());
    catchBlock.invokeVirtualMethod(
        MethodDescriptor.ofMethod(
            AgentRunLifecycleManager.class, "triggerDone", void.class),
        lcmInCatch);
    catchBlock.invokeStaticMethod(
        MethodDescriptor.ofMethod(DaprAgentMetadataHolder.class, "clear", void.class));
    catchBlock.throwException(catchBlock.getCaughtException());
  }

  /**
   * Generates a trivial delegation body for non-{@code @Agent} abstract interface methods.
   * <pre>
   *   return delegate.method(params);   // or just delegate.method(params); for void
   * </pre>
   */
  private void generateDelegateMethod(ClassCreator cc, MethodInfo method,
      FieldDescriptor delegateDesc) {

    final boolean isVoid = method.returnType().kind() == Type.Kind.VOID;

    MethodCreator mc = cc.getMethodCreator(MethodDescriptor.of(method));
    mc.setModifiers(Modifier.PUBLIC);
    for (Type exType : method.exceptions()) {
      mc.addException(exType.name().toString());
    }

    ResultHandle del = mc.readInstanceField(delegateDesc, mc.getThis());
    ResultHandle[] params = new ResultHandle[method.parametersCount()];
    for (int i = 0; i < params.length; i++) {
      params[i] = mc.getMethodParam(i);
    }

    if (isVoid) {
      mc.invokeInterfaceMethod(MethodDescriptor.of(method), del, params);
      mc.returnVoid();
    } else {
      ResultHandle result = mc.invokeInterfaceMethod(
          MethodDescriptor.of(method), del, params);
      mc.returnValue(result);
    }
  }

  // -------------------------------------------------------------------------
  // Annotation metadata extraction (Jandex)
  // -------------------------------------------------------------------------

  /**
   * Returns the {@code @Agent(name)} value if non-blank, otherwise falls back to
   * {@code InterfaceName.methodName}.
   */
  private String extractAgentName(MethodInfo method) {
    AnnotationInstance agent = method.annotation(AGENT_ANNOTATION);
    if (agent != null) {
      AnnotationValue nameVal = agent.value("name");
      if (nameVal != null && !nameVal.asString().isBlank()) {
        return nameVal.asString();
      }
    }
    return method.declaringClass().name().withoutPackagePrefix()
        + "." + method.name();
  }

  /**
   * Returns the joined text of a {@code String[] value()} annotation attribute, or
   * {@code null} when the annotation is absent or its value is empty.
   *
   * <p>Handles both the single-string form ({@code @UserMessage("text")}) and the
   * array form ({@code @UserMessage({"line1", "line2"})}).
   */
  private String extractAnnotationText(MethodInfo method, DotName annotationName) {
    AnnotationInstance annotation = method.annotation(annotationName);
    if (annotation == null) {
      return null;
    }
    AnnotationValue value = annotation.value(); // "value" is the default attribute
    if (value == null) {
      return null;
    }
    if (value.kind() == AnnotationValue.Kind.ARRAY) {
      String[] parts = value.asStringArray();
      return parts.length == 0 ? null : String.join("\n", parts);
    }
    // single String stored directly (rare but defensively handled)
    return value.asString();
  }

  // -------------------------------------------------------------------------
  // Annotation metadata extraction helpers
  // -------------------------------------------------------------------------

  private static String stringValueOrNull(AnnotationInstance annotation, String name) {
    AnnotationValue value = annotation.value(name);
    if (value == null) {
      return null;
    }
    String sv = value.asString();
    return (sv == null || sv.isEmpty()) ? null : sv;
  }

  // -------------------------------------------------------------------------
  // Interceptor / annotation-transformer build steps (unchanged)
  // -------------------------------------------------------------------------

  /**
   * Automatically apply {@code @DaprAgentToolInterceptorBinding} to every
   * {@code @Tool}-annotated method in the application index.
   */
  @BuildStep
  @SuppressWarnings("deprecation")
  AnnotationsTransformerBuildItem addDaprInterceptorToToolMethods() {
    return new AnnotationsTransformerBuildItem(
        AnnotationsTransformer.appliedToMethod()
            .whenMethod(m -> m.hasAnnotation(TOOL_ANNOTATION))
            .thenTransform(t -> t.add(DAPR_TOOL_INTERCEPTOR_BINDING)));
  }

  /**
   * Automatically apply {@code @DaprAgentInterceptorBinding} to every
   * {@code @Agent}-annotated method in the application index.
   *
   * <p>This causes {@link io.quarkiverse.dapr.langchain4j.agent.DaprAgentMethodInterceptor}
   * to fire when an {@code @Agent} method is called on a regular CDI bean (not a synthetic
   * AiService bean).  For synthetic AiService beans the generated CDI decorator (produced by
   * {@link #generateAgentDecorators}) is the authoritative hook point.
   */
  @BuildStep
  @SuppressWarnings("deprecation")
  AnnotationsTransformerBuildItem addDaprInterceptorToAgentMethods() {
    return new AnnotationsTransformerBuildItem(
        AnnotationsTransformer.appliedToMethod()
            .whenMethod(m -> m.hasAnnotation(AGENT_ANNOTATION))
            .thenTransform(t -> t.add(DAPR_AGENT_INTERCEPTOR_BINDING)));
  }

  /**
   * Also apply the interceptor binding at the class level for any CDI bean whose
   * declared class itself has {@code @Tool} (less common but supported by LangChain4j).
   */
  @BuildStep
  @SuppressWarnings("deprecation")
  AnnotationsTransformerBuildItem addDaprInterceptorToToolClasses() {
    return new AnnotationsTransformerBuildItem(
        AnnotationsTransformer.appliedToClass()
            .whenClass(c -> {
              for (MethodInfo method : c.methods()) {
                if (method.hasAnnotation(TOOL_ANNOTATION)) {
                  return true;
                }
              }
              return false;
            })
            .thenTransform(t -> t.add(DAPR_TOOL_INTERCEPTOR_BINDING)));
  }
}
