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

package io.dapr.quarkus.langchain4j.deployment;

import io.dapr.quarkus.langchain4j.durable.AgentMethodMeta;
import io.dapr.quarkus.langchain4j.durable.ConditionalBranch;
import io.dapr.quarkus.langchain4j.durable.DurableAgentProxyRecorder;
import io.dapr.quarkus.langchain4j.durable.OutputCombiner;
import io.dapr.quarkus.langchain4j.workflow.DaprWorkflowRuntimeRecorder;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.runtime.RuntimeValue;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Quarkus deployment processor for the Dapr Agentic extension (control-inversion engine).
 *
 * <p>Every declarative LangChain4j agent runs <em>as</em> a Dapr Workflow: a leaf {@code @Agent}'s
 * ReAct loop runs as {@code react-agent}; composites ({@code @SequenceAgent} /
 * {@code @ParallelAgent} / {@code @LoopAgent} / {@code @ConditionalAgent}) run as
 * {@code durable-sequence} / {@code durable-parallel} / {@code durable-loop} /
 * {@code durable-conditional}, calling their children directly. The only non-deterministic steps —
 * the model call and each tool call — are the {@code agent-llm} and {@code agent-tool} activities,
 * so all agent state lives in workflow history (per-call crash recovery, horizontal scale, and
 * observability come for free).
 *
 * <p>{@code DaprWorkflowProcessor.searchWorkflows()} uses {@code ApplicationIndexBuildItem} which
 * only indexes application classes — extension runtime JARs are invisible to it. We fix this by:
 * <ol>
 *   <li>Producing an {@link IndexDependencyBuildItem} so our runtime JAR is indexed into the
 *       {@link CombinedIndexBuildItem} (and visible to Arc for CDI bean discovery).</li>
 *   <li>Registering our workflows and activities with the Dapr workflow runtime
 *       ({@link #setupWorkflowRuntime}) and producing {@link AdditionalBeanBuildItem}s so Arc
 *       discovers them as CDI beans.</li>
 *   <li>Replacing each agent interface's AiServices-built synthetic bean with a durable-workflow
 *       proxy ({@link #registerDurableAgentBeans}), so unchanged user {@code @Agent} interfaces
 *       transparently run as workflows.</li>
 * </ol>
 */
public class DaprAgenticProcessor {

  private static final Logger LOG = Logger.getLogger(DaprAgenticProcessor.class);

  private static final String FEATURE = "dapr-agentic";

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
   * LangChain4j {@code @V} annotation — names a method parameter for {@code {{var}}} binding.
   */
  private static final DotName V_ANNOTATION =
      DotName.createSimple("dev.langchain4j.service.V");

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

  // Composite agent annotations.
  private static final DotName SEQUENCE_AGENT_ANNOTATION =
      DotName.createSimple("dev.langchain4j.agentic.declarative.SequenceAgent");
  private static final DotName PARALLEL_AGENT_ANNOTATION =
      DotName.createSimple("dev.langchain4j.agentic.declarative.ParallelAgent");
  private static final DotName LOOP_AGENT_ANNOTATION =
      DotName.createSimple("dev.langchain4j.agentic.declarative.LoopAgent");
  private static final DotName CONDITIONAL_AGENT_ANNOTATION =
      DotName.createSimple("dev.langchain4j.agentic.declarative.ConditionalAgent");
  private static final DotName ACTIVATION_CONDITION_ANNOTATION =
      DotName.createSimple("dev.langchain4j.agentic.declarative.ActivationCondition");
  private static final DotName OUTPUT_ANNOTATION =
      DotName.createSimple("dev.langchain4j.agentic.declarative.Output");

  /**
   * Quarkus-LangChain4j {@code @ToolBox} annotation — references tool classes for an agent.
   */
  private static final DotName TOOLBOX_ANNOTATION =
      DotName.createSimple("io.quarkiverse.langchain4j.ToolBox");

  private static final String[] WORKFLOW_CLASSES = {
      // The agent's ReAct loop run AS a workflow.
      "io.dapr.quarkus.langchain4j.durable.ReActAgentWorkflow",
      // Composites: orchestrations calling react-agent (or nested composite) children directly.
      "io.dapr.quarkus.langchain4j.durable.DurableSequenceWorkflow",
      "io.dapr.quarkus.langchain4j.durable.DurableParallelWorkflow",
      "io.dapr.quarkus.langchain4j.durable.DurableLoopWorkflow",
      "io.dapr.quarkus.langchain4j.durable.DurableConditionalWorkflow",
  };

  private static final String[] ACTIVITY_CLASSES = {
      // The durable ReAct loop's only non-deterministic steps.
      "io.dapr.quarkus.langchain4j.durable.AgentLlmActivity",
      "io.dapr.quarkus.langchain4j.durable.AgentToolActivity",
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
    return new IndexDependencyBuildItem("io.dapr.quarkus", "quarkus-agentic-dapr");
  }

  /**
   * Register all workflows and activities using the Dapr Java SDK directly,
   * bypassing quarkus-dapr's {@code WorkflowItemBuildItem} pipeline.
   */
  @BuildStep
  @Record(ExecutionTime.RUNTIME_INIT)
  void setupWorkflowRuntime(DaprWorkflowRuntimeRecorder recorder,
      CombinedIndexBuildItem combinedIndex) {

    @SuppressWarnings("rawtypes") RuntimeValue builder = recorder.createBuilder();
    IndexView index = combinedIndex.getIndex();

    // Register workflows (under their @WorkflowMetadata name).
    for (String className : WORKFLOW_CLASSES) {
      ClassInfo classInfo = index.getClassByName(DotName.createSimple(className));
      if (classInfo == null) {
        continue;
      }
      String regName = className;
      AnnotationInstance meta = classInfo.annotation(WORKFLOW_METADATA_DOTNAME);
      if (meta != null) {
        String metaName = stringValueOrNull(meta, "name");
        if (metaName != null) {
          regName = metaName;
        }
      }
      recorder.registerWorkflow(builder, regName, className);
    }

    // Register activities (under their @ActivityMetadata name).
    for (String className : ACTIVITY_CLASSES) {
      ClassInfo classInfo = index.getClassByName(DotName.createSimple(className));
      if (classInfo == null) {
        continue;
      }
      String regName = className;
      AnnotationInstance meta = classInfo.annotation(ACTIVITY_METADATA_DOTNAME);
      if (meta != null) {
        String metaName = stringValueOrNull(meta, "name");
        if (metaName != null) {
          regName = metaName;
        }
      }
      recorder.registerActivity(builder, regName, className);
    }

    // Register agent → tool-class mappings. For each @Agent method with a @ToolBox, extract the
    // tool class names so the durable agent-llm/agent-tool activities can resolve the agent's
    // tools (via AgentToolClassRegistry → AgentToolSpecRegistry).
    for (AnnotationInstance ann : index.getAnnotations(AGENT_ANNOTATION)) {
      if (ann.target().kind() != AnnotationTarget.Kind.METHOD) {
        continue;
      }
      AnnotationValue nameValue = ann.value("name");
      if (nameValue == null || nameValue.asString().isEmpty()) {
        continue;
      }
      String agentName = nameValue.asString();
      MethodInfo method = ann.target().asMethod();
      AnnotationInstance toolBoxAnn = method.annotation(TOOLBOX_ANNOTATION);
      if (toolBoxAnn != null && toolBoxAnn.value() != null) {
        Type[] toolBoxTypes = toolBoxAnn.value().asClassArray();
        List<String> toolClassNames = new ArrayList<>();
        for (Type t : toolBoxTypes) {
          toolClassNames.add(t.name().toString());
        }
        recorder.registerAgentToolClasses(agentName, toolClassNames);
      }
    }

    recorder.startRuntime(builder);
  }

  /**
   * Explicitly register our Workflow, WorkflowActivity, and supporting CDI beans.
   */
  @BuildStep
  void registerAdditionalBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
    for (String className : WORKFLOW_CLASSES) {
      additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(className));
    }
    for (String className : ACTIVITY_CLASSES) {
      additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(className));
    }
    // Tool registry — scans @Tool CDI beans at startup so the durable activities can invoke them.
    additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(
        "io.dapr.quarkus.langchain4j.agent.recovery.ToolRegistry"));
    // Tool-spec resolver used by the durable agent-llm activity to advertise an agent's tools.
    additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(
        "io.dapr.quarkus.langchain4j.durable.AgentToolSpecRegistry"));
    // Registers the Dapr-backed AgenticScope store (checkpointing) when enabled.
    additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(
        "io.dapr.quarkus.langchain4j.scope.AgenticScopeStoreInitializer"));
  }

  /**
   * Drop-in entry point: replace each agent interface's AiServices-built synthetic bean (registered
   * by the quarkiverse agentic processor) with an alternative synthetic bean whose instance is a
   * {@code java.lang.reflect.Proxy} that runs the agent as a durable Dapr Workflow
   * ({@code react-agent} for leaves, {@code durable-*} for composites). Marked {@code alternative}
   * with priority so it wins over the AiServices bean, leaving the user's {@code @Agent} interfaces
   * unchanged.
   */
  @BuildStep
  @Record(ExecutionTime.RUNTIME_INIT)
  void registerDurableAgentBeans(DurableAgentProxyRecorder recorder,
      CombinedIndexBuildItem combinedIndex,
      BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
      BuildProducer<NativeImageProxyDefinitionBuildItem> nativeProxies) {

    IndexView index = combinedIndex.getIndex();
    Map<DotName, Map<String, AgentMethodMeta>> byInterface = new HashMap<>();

    collectAgentMethods(index, AGENT_ANNOTATION, "react-agent", byInterface);
    collectAgentMethods(index, SEQUENCE_AGENT_ANNOTATION, "durable-sequence", byInterface);
    collectAgentMethods(index, PARALLEL_AGENT_ANNOTATION, "durable-parallel", byInterface);
    collectAgentMethods(index, LOOP_AGENT_ANNOTATION, "durable-loop", byInterface);
    collectAgentMethods(index, CONDITIONAL_AGENT_ANNOTATION, "durable-conditional", byInterface);

    for (Map.Entry<DotName, Map<String, AgentMethodMeta>> entry : byInterface.entrySet()) {
      String interfaceName = entry.getKey().toString();
      LOG.infof("Registering durable agent bean for %s (%d method(s))",
          interfaceName, entry.getValue().size());
      syntheticBeans.produce(SyntheticBeanBuildItem
          .configure(entry.getKey())
          // Distinct identifier so this coexists with the quarkiverse-built synthetic bean
          // (same types+qualifiers would otherwise collide); the alternative + priority then
          // makes this one win at injection.
          .identifier("durableAgent_" + interfaceName.replace('.', '_'))
          .forceApplicationClass()
          .createWith(recorder.createAgentProxy(interfaceName, entry.getValue()))
          .setRuntimeInit()
          .scope(ApplicationScoped.class)
          .alternative(true)
          .priority(100)
          .done());
      // The durable bean instance is a java.lang.reflect.Proxy of the interface; register it
      // so the proxy works under native image.
      nativeProxies.produce(new NativeImageProxyDefinitionBuildItem(List.of(interfaceName)));
    }
  }

  private void collectAgentMethods(IndexView index, DotName annotation, String workflowName,
      Map<DotName, Map<String, AgentMethodMeta>> byInterface) {
    for (AnnotationInstance ann : index.getAnnotations(annotation)) {
      if (ann.target().kind() != AnnotationTarget.Kind.METHOD) {
        continue;
      }
      MethodInfo method = ann.target().asMethod();
      if (!method.declaringClass().isInterface()) {
        continue;
      }
      AgentMethodMeta meta = buildAgentMethodMeta(index, method, annotation, workflowName);
      byInterface
          .computeIfAbsent(method.declaringClass().name(), k -> new HashMap<>())
          .put(method.name(), meta);
    }
  }

  private AgentMethodMeta buildAgentMethodMeta(IndexView index, MethodInfo method,
      DotName annotation, String workflowName) {
    List<String> varNames = orderedParamNames(method);

    if (annotation.equals(AGENT_ANNOTATION)) {
      // outputKey matters when this leaf is a sub-agent (its result is stored under it in the
      // parent's state); harmless for a top-level leaf (the handler reads the workflow's text).
      return new AgentMethodMeta(workflowName, extractAgentName(method),
          extractAnnotationText(method, USER_MESSAGE_ANNOTATION),
          extractAnnotationText(method, SYSTEM_MESSAGE_ANNOTATION),
          varNames, List.of(),
          stringValueOrNull(method.annotation(AGENT_ANNOTATION), "outputKey"),
          0, List.of(), null);
    }

    AnnotationInstance composite = method.annotation(annotation);
    String name = stringValueOrNull(composite, "name");
    if (name == null || name.isBlank()) {
      name = method.declaringClass().name().withoutPackagePrefix() + "." + method.name();
    }
    String outputKey = stringValueOrNull(composite, "outputKey");
    OutputCombiner combiner = resolveOutputCombiner(method.declaringClass());

    if (annotation.equals(CONDITIONAL_AGENT_ANNOTATION)) {
      return new AgentMethodMeta(workflowName, name, null, null, varNames, List.of(), outputKey, 0,
          resolveConditionalBranches(index, composite, method.declaringClass()), combiner);
    }

    int maxIterations = annotation.equals(LOOP_AGENT_ANNOTATION) ? intValueOrDefault(composite, 2) : 0;
    return new AgentMethodMeta(workflowName, name, null, null, varNames,
        resolveSubAgents(index, composite), outputKey, maxIterations, List.of(), combiner);
  }

  private OutputCombiner resolveOutputCombiner(ClassInfo agentInterface) {
    for (MethodInfo method : agentInterface.methods()) {
      if (method.hasAnnotation(OUTPUT_ANNOTATION)) {
        List<String> paramNames = new ArrayList<>();
        for (int i = 0; i < method.parametersCount(); i++) {
          paramNames.add(method.parameterName(i));
        }
        return new OutputCombiner(agentInterface.name().toString(), method.name(), paramNames);
      }
    }
    return null;
  }

  private List<AgentMethodMeta> resolveSubAgents(IndexView index, AnnotationInstance composite) {
    List<AgentMethodMeta> nodes = new ArrayList<>();
    AnnotationValue subAgentsValue = composite.value("subAgents");
    if (subAgentsValue == null) {
      return nodes;
    }
    for (Type subType : subAgentsValue.asClassArray()) {
      AgentMethodMeta node = resolveSubAgentNode(index, subType);
      if (node != null) {
        nodes.add(node);
      }
    }
    return nodes;
  }

  /**
   * Resolves a sub-agent class to a recursive {@link AgentMethodMeta} node — a leaf {@code @Agent}
   * or a nested composite ({@code @SequenceAgent}/{@code @ParallelAgent}/{@code @LoopAgent}/
   * {@code @ConditionalAgent}), recursing through {@link #buildAgentMethodMeta}.
   */
  private AgentMethodMeta resolveSubAgentNode(IndexView index, Type subType) {
    ClassInfo subInterface = index.getClassByName(subType.name());
    if (subInterface == null) {
      return null;
    }
    for (MethodInfo subMethod : subInterface.methods()) {
      if (subMethod.hasAnnotation(AGENT_ANNOTATION)) {
        return buildAgentMethodMeta(index, subMethod, AGENT_ANNOTATION, "react-agent");
      }
      if (subMethod.hasAnnotation(SEQUENCE_AGENT_ANNOTATION)) {
        return buildAgentMethodMeta(index, subMethod, SEQUENCE_AGENT_ANNOTATION, "durable-sequence");
      }
      if (subMethod.hasAnnotation(PARALLEL_AGENT_ANNOTATION)) {
        return buildAgentMethodMeta(index, subMethod, PARALLEL_AGENT_ANNOTATION, "durable-parallel");
      }
      if (subMethod.hasAnnotation(LOOP_AGENT_ANNOTATION)) {
        return buildAgentMethodMeta(index, subMethod, LOOP_AGENT_ANNOTATION, "durable-loop");
      }
      if (subMethod.hasAnnotation(CONDITIONAL_AGENT_ANNOTATION)) {
        return buildAgentMethodMeta(index, subMethod, CONDITIONAL_AGENT_ANNOTATION,
            "durable-conditional");
      }
    }
    return null;
  }

  private List<ConditionalBranch> resolveConditionalBranches(IndexView index,
      AnnotationInstance composite, ClassInfo router) {
    List<ConditionalBranch> branches = new ArrayList<>();
    AnnotationValue subAgentsValue = composite.value("subAgents");
    if (subAgentsValue == null) {
      return branches;
    }
    for (Type subType : subAgentsValue.asClassArray()) {
      AgentMethodMeta node = resolveSubAgentNode(index, subType);
      if (node == null) {
        continue;
      }
      MethodInfo condition = findActivationCondition(router, subType.name());
      if (condition != null) {
        branches.add(new ConditionalBranch(node, router.name().toString(),
            condition.name(), orderedParamNames(condition)));
      } else {
        branches.add(new ConditionalBranch(node, null, null, List.of()));
      }
    }
    return branches;
  }

  private MethodInfo findActivationCondition(ClassInfo router, DotName subAgentClass) {
    for (MethodInfo method : router.methods()) {
      AnnotationInstance condition = method.annotation(ACTIVATION_CONDITION_ANNOTATION);
      if (condition == null || condition.value() == null) {
        continue;
      }
      for (Type guarded : condition.value().asClassArray()) {
        if (guarded.name().equals(subAgentClass)) {
          return method;
        }
      }
    }
    return null;
  }

  private List<String> orderedParamNames(MethodInfo method) {
    Map<Integer, String> byPosition = new HashMap<>();
    for (AnnotationInstance ann : method.annotations()) {
      if (ann.name().equals(V_ANNOTATION)
          && ann.target() != null
          && ann.target().kind() == AnnotationTarget.Kind.METHOD_PARAMETER) {
        AnnotationValue value = ann.value();
        if (value != null) {
          byPosition.put((int) ann.target().asMethodParameter().position(), value.asString());
        }
      }
    }
    List<String> ordered = new ArrayList<>();
    for (int i = 0; i < method.parametersCount(); i++) {
      ordered.add(byPosition.getOrDefault(i, "arg" + i));
    }
    return ordered;
  }

  private static int intValueOrDefault(AnnotationInstance annotation, int defaultValue) {
    if (annotation == null) {
      return defaultValue;
    }
    AnnotationValue value = annotation.value("maxIterations");
    return value == null ? defaultValue : value.asInt();
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

  private static String stringValueOrNull(AnnotationInstance annotation, String name) {
    AnnotationValue value = annotation.value(name);
    if (value == null) {
      return null;
    }
    String sv = value.asString();
    return (sv == null || sv.isEmpty()) ? null : sv;
  }
}
