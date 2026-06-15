# Durable agents (control-inversion approach)

This branch (`control-inversion`) runs an agent's ReAct loop **as a Dapr Workflow**
(`ReActAgentWorkflow`) instead of recording an in-memory LangChain4j AiServices loop. The
model call (`agent-llm`) and each tool call (`agent-tool`) are activities — pure functions
of their input, with no in-memory run context. That single change is what makes per-call
crash recovery and horizontal scale possible. See the architecture notes in `../README.md`.

## What the automated tests prove

Run with Docker available (dev services start Dapr + placement + scheduler + state store):

```bash
mvn test -Dtest=DurableAgentResourceTest,DurableToolAgentResourceTest
```

- **`DurableAgentResourceTest`** (`GET /durable`) — a single agent's loop runs as the
  `react-agent` workflow; the model call executes as the `agent-llm` activity and the run
  completes from workflow history.
- **`DurableToolAgentResourceTest`** (`GET /durable/research`) — a tool round-trip:
  `agent-llm` requests a tool → `agent-tool` executes the real `ResearchTools.getCapital`
  → result flows back → `agent-llm` produces the final answer. Asserting the response
  contains `Paris` proves the tool ran via the activity.

The two demos below need **multiple processes**, so they're run by hand rather than in the
single-JVM test harness.

## Demo 1 — per-call crash recovery

Shows that a crash mid-loop resumes from workflow history: completed LLM/tool calls are
**not** re-executed (contrast with the passive-recorder approach, which re-runs the whole
agent from scratch and loses template variables).

```bash
# Terminal 1 — standalone Dapr so workflow state persists across an app restart
dapr run --app-id durable-agents --resources-path ./components -- \
    mvn quarkus:dev

# Terminal 2 — start a multi-step (tool-using) run
curl "http://localhost:8080/durable/research?country=France"

# Terminal 2 — while it is between the agent-llm and agent-tool steps, kill the app
#   (find the quarkus:dev / java PID and: kill -9 <pid>)

# Terminal 1 — restart; Dapr replays the workflow from history
mvn quarkus:dev
```

On restart the workflow rehydrates: already-completed `agent-llm`/`agent-tool` activities
return from history (visible in the Dapr dashboard as cached), and the loop continues at
the next un-run step. Nothing is re-executed and no prompt/state is lost.

## Demo 2 — horizontal scale (activities across replicas)

Shows the run executing across **two** replicas. Dapr Workflow
[randomly distributes activity work across all replicas](https://docs.dapr.io/developing-applications/building-blocks/workflow/workflow-architecture/)
of an app-id — which is correct here because the activities carry no in-memory state (the
passive-recorder approach is single-replica-only for exactly this reason).

```bash
# Shared placement + scheduler + state store, then two app instances on the same app-id.
# (Point both at the same Dapr control plane; e.g. two `dapr run --app-id durable-agents`
#  on different app ports against a shared placement/scheduler, or two replicas on k8s.)

# Fire several requests
for i in 1 2 3 4 5; do curl -s "http://localhost:8080/durable/research?country=France" & done; wait
```

Watch each instance's logs: `Processing activity request: agent-llm` / `agent-tool` lines
appear on **both** replicas for the same workflow instance — work is load-balanced, and any
replica can run any activity because none of them depend on a node-local `AgentRunContext`.
