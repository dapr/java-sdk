# Quarkus Agentic Dapr

A Quarkus extension that bridges [LangChain4j's agentic framework](https://docs.langchain4j.dev/) with [Dapr Workflows](https://docs.dapr.io/developing-applications/building-blocks/workflow/workflow-overview/) for durable, observable AI agent orchestration.

## What it does

The agent's **ReAct loop runs _as_ a Dapr Workflow** (control inversion): the only
non-deterministic steps — each model call and each tool call — are workflow activities, so all
agent state lives in the workflow history. Crash recovery, horizontal scale, and observability
fall out for free. Composites (sequential, parallel, loop, conditional) are themselves workflows
that call their children directly, forming a replayable parent-child tree.

| Capability | Without Dapr | With `quarkus-langchain4j-dapr-agentic` |
|---|---|---|
| Durability | Lost on crash | Full workflow history persisted |
| Crash recovery | Restart from scratch | Resume at the next un-run model/tool call |
| Horizontal scale | Single process | Activities placed across replicas (no in-memory state) |
| Observability | Logs only | Dapr dashboard + per-activity tracking |
| Tool / LLM call audit trail | None | Every request/response recorded in history |
| Code changes | — | **None** — just add `quarkus-langchain4j-dapr-agentic` |

## Modules

```
quarkus/
├── quarkus-langchain4j-dapr-agentic/   # The extension: agents as durable Dapr Workflows
│   ├── runtime/                        #   durable agent workflows + activities
│   └── deployment/                     #   annotation scanning, durable agent proxies
├── quarkus-langchain4j-dapr-llm/       # Optional: Dapr Conversation API as ChatModel provider
├── quarkus-langchain4j-dapr-registry/  # Optional: registers agents in Dapr state store
└── examples/                           # Built-in examples
```

## Supported Agent Types

All 5 LangChain4j orchestration types are supported:

| Annotation | Dapr Workflow |
|------------|---------------|
| `@Agent` | `react-agent` (the agent's ReAct loop run as a workflow) |
| `@SequenceAgent` | `durable-sequence` |
| `@ParallelAgent` | `durable-parallel` |
| `@LoopAgent` | `durable-loop` |
| `@ConditionalAgent` | `durable-conditional` |

Composites can be nested arbitrarily (e.g. a `@SequenceAgent` whose sub-agent is a
`@ParallelAgent`): each composite completes with its full shared-state map, which its parent
merges, so state propagates across the whole tree. Structured (`@Output`) combiners and
record return types are supported.

## Quick Start

### 1. Add the dependency

```xml
<dependency>
    <groupId>io.dapr.quarkus</groupId>
    <artifactId>quarkus-langchain4j-dapr-agentic</artifactId>
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

## Crash Recovery

Because the ReAct loop _is_ a workflow, recovery is the same deterministic replay Dapr gives any
workflow — there is no special recovery path. If the process crashes mid-execution, every model
call and tool call that already completed returns from the workflow history on replay, and the
loop resumes at the next un-run call. Recovery is **per call**, not per agent.

### How it works

1. **Normal operation**: `ReActAgentWorkflow` drives the loop. Each model call (`agent-llm`) and
   tool call (`agent-tool`) is a workflow activity whose result is recorded in history.
2. **Crash**: The process dies. Dapr workflow history persists.
3. **Restart**: Dapr replays the workflow. Completed `agent-llm`/`agent-tool` activities return
   their recorded results instantly; the loop is deterministic given those results.
4. **Resume**: The first activity with no recorded result is the only one that actually re-runs —
   the loop continues from exactly where it stopped. The same applies to composite children
   (a completed child workflow is not re-executed).

### Recovery granularity

| Scope | Behavior |
|-------|----------|
| Composite (e.g., Agent1 → Agent2 → Agent3) | Completed child workflows are skipped on replay; only the in-progress child resumes. |
| Single agent (LLM calls + tool calls) | Completed model/tool calls return from history; only the next un-run call executes. |

### Demo: simulating a crash

```bash
# 1. Start the app and trigger a multi-agent workflow
curl "http://localhost:8080/travel/plan?origin=NYC&destination=Paris"

# 2. Kill the process mid-execution (e.g., during the second agent)
kill -9 <pid>

# 3. Restart the app — the workflow resumes automatically
mvn quarkus:dev
```

In the Dapr dashboard, completed activities show recorded results; the workflow resumes at the
next un-run call and completes.

### Key classes

| Class | Role |
|-------|------|
| `ReActAgentWorkflow` | The agent's ReAct loop run as a workflow (`react-agent`) |
| `AgentLlmActivity` | One model call (`agent-llm`) — a pure function of the conversation + agent name |
| `AgentToolActivity` | One `@Tool` invocation (`agent-tool`) |
| `ToolRegistry` | CDI bean that discovers `@Tool` methods at startup |
| `AgentToolClassRegistry` | Maps agent names to their `@ToolBox` classes (populated at build time) |

## AgenticScope Checkpointing

LangChain4j's agentic scope (the shared state and conversation context of a multi-agent
workflow) can be checkpointed to a Dapr state store — the LangChain4j equivalent of a
LangGraph checkpointer. Every scope update is persisted, so agentic state survives
restarts and is shareable across replicas.

```properties
dapr.agentic.scope-store.enabled=true
dapr.agentic.scope-store.name=kvstore
```

| Class | Role |
|-------|------|
| `DaprAgenticScopeStore` | `AgenticScopeStore` implementation over the Dapr state API |
| `AgenticScopeStoreInitializer` | Registers the store with LangChain4j's `AgenticScopePersister` at startup |

## Known Limitations

- **At-least-once activities**: `agent-tool` activities can be redelivered on retry/replay, so
  side-effecting tools must be idempotent or externally guarded.
- **`ResultWithAgenticScope<T>` return type**: not yet supported — the durable engine has no
  in-memory `AgenticScope` to surface. Use a plain return type or an `@Output` combiner.
- **Small models**: llama3.2 (3B) sometimes malforms tool call arguments; llama3.1:8b+ recommended
