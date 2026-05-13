# Quarkus Agentic Dapr

A Quarkus extension that bridges [LangChain4j's agentic framework](https://docs.langchain4j.dev/) with [Dapr Workflows](https://docs.dapr.io/developing-applications/building-blocks/workflow/workflow-overview/) for durable, observable AI agent orchestration.

## What it does

Every LLM call and tool call becomes a **durable Dapr Workflow activity**. If the process crashes, completed activities are recovered from the workflow history. Agent orchestration (sequential, parallel, loop, conditional) is backed by Dapr Workflows with parent-child hierarchy.

| Capability | Without Dapr | With `quarkus-agentic-dapr` |
|---|---|---|
| Durability | Lost on crash | Full workflow history persisted |
| Observability | Logs only | Dapr dashboard + per-activity tracking |
| Tool call audit trail | None | Every call recorded with inputs/outputs |
| LLM call audit trail | None | Every LLM request/response recorded |
| Code changes | — | **None** — just swap the dependency |

## Modules

```
quarkus/
├── runtime/                               # Core extension (interceptors, workflows, activities)
├── deployment/                            # Build-time processing (annotation scanning, CDI decorators)
├── quarkus-agentic-dapr-agents-registry/  # Optional: registers agents in Dapr state store
├── quarkus-langchain4j-dapr/              # Optional: Dapr Conversation API as ChatModel provider
└── examples/                              # Built-in examples
```

## Supported Agent Types

All 5 LangChain4j orchestration types are supported:

| Annotation | Dapr Workflow |
|------------|---------------|
| `@Agent` | `AgentRunWorkflow` (per-agent ReAct loop) |
| `@SequenceAgent` | `SequentialOrchestrationWorkflow` |
| `@ParallelAgent` | `ParallelOrchestrationWorkflow` |
| `@LoopAgent` | `LoopOrchestrationWorkflow` |
| `@ConditionalAgent` | `ConditionalOrchestrationWorkflow` |

## Quick Start

### 1. Add the dependency

```xml
<dependency>
    <groupId>io.dapr.quarkus</groupId>
    <artifactId>quarkus-agentic-dapr</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

### 2. Configure

```properties
quarkus.dapr.devservices.enabled=false
quarkus.dapr.workflow.enabled=false

# Dapr sidecar endpoints
dapr.grpc.endpoint=${DAPR_GRPC_ENDPOINT:http://localhost:40001}
dapr.http.endpoint=${DAPR_HTTP_ENDPOINT:http://localhost:3500}
```

### 3. Write agents (standard LangChain4j — no Dapr-specific code)

```java
public interface WeatherAssistant {

    @ToolBox(WeatherTools.class)
    @UserMessage("Check the weather in {{city}}")
    @Agent(name = "weather-assistant", outputKey = "weather")
    String checkWeather(@V("city") String city);
}
```

### 4. Run with Dapr

```bash
# Terminal 1: Dapr sidecar
dapr run --app-id my-agent --dapr-grpc-port 40001 --dapr-http-port 3500 \
    --resources-path ./components

# Terminal 2: Quarkus app
DAPR_GRPC_ENDPOINT=http://localhost:40001 \
DAPR_HTTP_ENDPOINT=http://localhost:3500 \
mvn quarkus:dev -Ddebug=false
```

## LLM Provider Options

### Ollama (direct)
```properties
quarkus.langchain4j.ollama.chat-model.model-id=llama3.1:8b
```

### Dapr Conversation API (provider-agnostic)
```properties
quarkus.langchain4j.chat-model.provider=dapr-conversation
quarkus.langchain4j.dapr.component-name=llm
```

Swap LLM providers by changing the Dapr component YAML — no Java code changes.

## Example Project

See [travel-planner-agents](https://github.com/javier-aliaga/travel-planner-agents) for a complete example with all agent types, Makefile targets, and Dapr components.

## Known Limitations

- **Nested composites**: `@ParallelAgent` inside `@SequenceAgent` is unstable (input type mismatch)
- **Crash recovery**: Workflow history survives but mid-agent resumption requires future work
- **Small models**: llama3.2 (3B) sometimes malforms tool call arguments; llama3.1:8b+ recommended
