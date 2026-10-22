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

## Crash Recovery

If the process crashes mid-execution, completed agents are not re-run. The in-progress agent is automatically re-run from scratch using its original prompt and tools.

### How it works

1. **Normal operation**: LangChain4j's AiServices drives the ReAct loop. Each LLM call and tool call is recorded as a Dapr Workflow activity.
2. **Crash**: The process dies. Dapr workflow history persists.
3. **Restart**: Dapr replays the workflow. Completed activities return cached results instantly.
4. **Recovery detection**: The in-progress activity is re-dispatched but fails because the in-memory `AgentRunContext` is gone. `AgentRunWorkflow` catches the failure.
5. **Agent re-run**: `RecoveryAgentActivity` re-executes the agent's entire ReAct loop from scratch — calling `ChatModel.chat()` directly and invoking tools via `ToolRegistry`.

### Recovery granularity

| Scope | Behavior |
|-------|----------|
| Orchestration (e.g., Agent1 → Agent2 → Agent3) | Completed agents are skipped (Dapr child workflow replay). Only the in-progress agent re-runs. |
| Single agent (LLM calls + tool calls) | The entire agent re-runs from its original prompt. Individual LLM/tool calls within the agent are not skipped. |

### Demo: simulating a crash

```bash
# 1. Start the app and trigger a multi-agent workflow
curl "http://localhost:8080/travel/plan?origin=NYC&destination=Paris"

# 2. Kill the process mid-execution (e.g., during the second agent)
kill -9 <pid>

# 3. Restart the app — the workflow resumes automatically
mvn quarkus:dev
```

In the Dapr dashboard, completed agents show cached results. The crashed agent is detected via `TaskFailedException`, then re-runs and completes.

### Key classes

| Class | Role |
|-------|------|
| `AgentRunWorkflow` | Catches `TaskFailedException` on activity failure, triggers recovery |
| `RecoveryAgentActivity` | Self-contained ReAct loop: ChatModel.chat() + tool dispatch |
| `ToolRegistry` | CDI bean that discovers @Tool methods at startup for recovery |
| `AgentToolClassRegistry` | Maps agent names to their @ToolBox classes (populated at build time) |

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

- **Single replica only (current design)**: live agent execution keeps per-run state in
  JVM memory — the `AgentRunContext` registry, the `CompletableFuture` the agent thread
  blocks on, and the per-thread Dapr context. Dapr Workflow, however,
  [randomly load-balances activities across all replicas](https://docs.dapr.io/developing-applications/building-blocks/workflow/workflow-architecture/)
  of an app-id, with no locality to where a run started. With more than one replica an
  LLM/tool activity can be dispatched to a replica that lacks the in-memory context, fail
  to find it, and surface as a (false) crash-recovery while the originating request blocks
  until the call timeout. **Deploy a single replica** until execution state is moved into
  workflow history (control inversion — see [Crash Recovery](#crash-recovery)); this is the
  same root cause as agent-level (not per-call) recovery.
- **Recovery granularity**: Agent-level only — individual LLM/tool calls within an agent are re-executed (not skipped)
- **Same agent name in concurrent _parallel_ orchestrations**: within a single
  orchestration each run's Dapr context is bound to its own run ID — including every
  iteration of a `@LoopAgent` and all sequential agents, which run on the planner's
  thread and route through the context it sets directly. Only when two *different*
  concurrent requests run the same agent name on parallel executor threads at the same
  instant can the FIFO name binding cross-attribute their runs; run IDs stay unique so
  routing is still correct, but observability may interleave.
- **daprd 1.18.0 workflow race**: a workflow event can be lost by the runtime (the
  workflow completes app-side but stays RUNNING in daprd, its event reminder fires on
  an empty inbox) — fix upstream in
  [dapr/dapr#10054](https://github.com/dapr/dapr/pull/10054). `AgentRunWorkflow` now
  self-heals: each `agent-event` wait has a 60s timeout that re-arms and lets replay
  redeliver the missed event from history, turning a permanent hang into a bounded
  delay. The e2e tests still retry once locally and are skipped on CI (slow runners
  widen the race window) until a fixed runtime ships.
- **Small models**: llama3.2 (3B) sometimes malforms tool call arguments; llama3.1:8b+ recommended
