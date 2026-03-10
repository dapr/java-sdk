# Quarkus Agentic Dapr Examples

This module demonstrates how to use `quarkus-agentic-dapr` to orchestrate LangChain4j agents with [Dapr Workflows](https://docs.dapr.io/developing-applications/building-blocks/workflow/workflow-overview/) as the underlying execution engine.

## What is Quarkus Agentic Dapr?

`quarkus-agentic-dapr` is a Quarkus extension that bridges LangChain4j's agentic framework with Dapr Durable Workflows. It is a **drop-in replacement** for the `quarkus-langchain4j-agentic` module — your agent definitions, annotations, and application code stay exactly the same. The only thing that changes is the dependency in your `pom.xml`.

### In-Memory vs Dapr Orchestration

LangChain4j ships with `quarkus-langchain4j-agentic`, which provides in-memory orchestration of agents using JVM thread pools. This works well for development and simple use cases, but has limitations in production:

- **No durability** — if the process crashes, all in-flight agent work is lost.
- **No observability** — agent execution state lives only in memory; there is no history of what happened.
- **No fault tolerance** — a failed tool call or LLM call requires restarting the entire flow from scratch.
- **Single JVM only** — orchestration cannot span multiple service instances.

`quarkus-agentic-dapr` solves these problems by routing every orchestration decision, tool call, and LLM call through Dapr Workflow activities. This gives you:

| Capability | `quarkus-langchain4j-agentic` | `quarkus-agentic-dapr` |
|---|---|---|
| Durability | None — lost on crash | Full workflow history persisted by Dapr |
| Observability | Application logs only | Dapr dashboard + per-activity status tracking |
| Fault tolerance | Manual retry | Dapr auto-retries failed activities |
| Scalability | Single JVM thread pool | Distributed across Dapr sidecars |
| Tool call audit trail | Log-based | Every tool call recorded in workflow history with inputs/outputs |
| Code changes required | None | **None** — just swap the dependency |

### How Dapr Workflows Work Behind the Scenes

When you use `quarkus-agentic-dapr`, your agent orchestration is backed by a hierarchy of Dapr Workflows:

1. **Orchestration Workflow** (top level) — one of `SequentialOrchestrationWorkflow`, `ParallelOrchestrationWorkflow`, `LoopOrchestrationWorkflow`, or `ConditionalOrchestrationWorkflow`. This coordinates the order in which agents execute.

2. **AgentRunWorkflow** (per agent) — each agent gets its own child workflow that manages its full ReAct loop lifecycle. It listens for events like `"tool-call"`, `"llm-call"`, and `"done"`, scheduling the appropriate Dapr activities for each.

3. **Activities** (individual operations) — `ToolCallActivity` executes tool methods, `LlmCallActivity` executes LLM calls. Each activity is a durable unit of work that Dapr can retry on failure and that appears in the workflow history.

This architecture means that every decision the LLM makes, every tool it calls, and every result it receives is recorded as a durable workflow event. If the process crashes mid-execution, Dapr replays the workflow from the last checkpoint — no work is lost.

The routing is completely transparent. At build time, the Quarkus extension automatically applies interceptors to all `@Tool`-annotated methods and generates CDI decorators for all `@Agent` interfaces. No changes to your agent code are required.

### Java SPI Discovery

The integration uses Java SPI (Service Provider Interface) to register itself. When LangChain4j encounters `@SequenceAgent`, `@ParallelAgent`, `@LoopAgent`, or `@ConditionalAgent`, it discovers `DaprWorkflowAgentsBuilder` via SPI and uses Dapr-based planners instead of in-memory ones. This is why the swap is transparent — just adding the dependency is enough.

## Prerequisites

- Java 17+
- Maven 3.9+
- An OpenAI API key (or compatible LLM provider)

No separate Dapr installation is needed for development — the extension uses **Quarkus Dapr Dev Services** to automatically start a Dapr sidecar, placement service, scheduler, and state store when you run in dev mode.

## Project Setup

### Dependency

Replace `quarkus-langchain4j-agentic` with `quarkus-agentic-dapr` in your `pom.xml`:

```xml
<!-- Remove this (in-memory orchestration) -->
<dependency>
    <groupId>io.quarkiverse.langchain4j</groupId>
    <artifactId>quarkus-langchain4j-agentic</artifactId>
</dependency>

<!-- Add this (Dapr workflow orchestration) -->
<dependency>
    <groupId>io.dapr.quarkus</groupId>
    <artifactId>quarkus-agentic-dapr</artifactId>
    <version>1.18.0-SNAPSHOT</version>
</dependency>
```

You also need an LLM provider and a REST endpoint:

```xml
<dependency>
    <groupId>io.quarkiverse.langchain4j</groupId>
    <artifactId>quarkus-langchain4j-openai</artifactId>
</dependency>
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-rest</artifactId>
</dependency>
```

Optionally, add the agent registry to auto-register agent metadata in a Dapr state store:

```xml
<dependency>
    <groupId>io.dapr.quarkus</groupId>
    <artifactId>quarkus-agentic-dapr-agents-registry</artifactId>
    <version>1.18.0-SNAPSHOT</version>
</dependency>
```

### Configuration

Add to `application.properties`:

```properties
# Enable Dapr Dev Services (auto-starts sidecar, placement, scheduler, state store)
quarkus.dapr.devservices.enabled=true
quarkus.dapr.workflow.enabled=true

# LLM provider configuration
quarkus.langchain4j.openai.api-key=${OPENAI_API_KEY}
quarkus.langchain4j.openai.chat-model.model-name=gpt-4o-mini
```

## Examples

### 1. Sequential Agent — Story Creator

A composite agent that runs two sub-agents in sequence: first a `CreativeWriter` generates a story draft, then a `StyleEditor` refines it.

**Agent definitions:**

```java
public interface CreativeWriter {

  @UserMessage("""
      You are a creative writer.
      Generate a draft of a story no more than 3 sentences around the given topic.
      Return only the story and nothing else.
      The topic is {{topic}}.
      """)
  @Agent(name = "creative-writer-agent",
      description = "Generate a story based on the given topic", outputKey = "story")
  String generateStory(@V("topic") String topic);
}

public interface StyleEditor {

  @UserMessage("""
      You are a style editor.
      Review the following story and improve its style to match the requested style: {{style}}.
      Return only the improved story and nothing else.
      Story: {{story}}
      """)
  @Agent(name = "style-editor-agent",
      description = "Edit a story to improve its writing style", outputKey = "story")
  String editStory(@V("story") String story, @V("style") String style);
}
```

**Orchestration:**

```java
public interface StoryCreator {

  @SequenceAgent(name = "story-creator-agent",
      outputKey = "story",
      subAgents = { CreativeWriter.class, StyleEditor.class })
  String write(@V("topic") String topic, @V("style") String style);
}
```

**REST endpoint:**

```java
@Path("/story")
public class StoryResource {

  @Inject
  StoryCreator storyCreator;

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public String createStory(
      @QueryParam("topic") @DefaultValue("dragons and wizards") String topic,
      @QueryParam("style") @DefaultValue("fantasy") String style) {
    return storyCreator.write(topic, style);
  }
}
```

Behind the scenes, this starts a `SequentialOrchestrationWorkflow` in Dapr that runs `CreativeWriter` first, passes the `story` output to `StyleEditor`, and returns the final result.

**Try it:**

```bash
curl "http://localhost:8080/story?topic=dragons&style=comedy"
```

### 2. Parallel Agent — Story + Research

A composite agent that runs a `StoryCreator` (itself a sequential agent) and a `ResearchWriter` in parallel. This demonstrates nested composite agents.

```java
public interface ParallelCreator {

  @ParallelAgent(name = "parallel-creator-agent",
      outputKey = "storyAndCountryResearch",
      subAgents = { StoryCreator.class, ResearchWriter.class })
  ParallelStatus create(@V("topic") String topic, @V("country") String country, @V("style") String style);

  @Output
  static ParallelStatus output(String story, String summary) {
    if (story == null || summary == null) {
      return new ParallelStatus("ERROR", story, summary);
    }
    return new ParallelStatus("OK", story, summary);
  }
}
```

Behind the scenes, a `ParallelOrchestrationWorkflow` spawns both sub-agents concurrently using Dapr's `allOf()` task composition and waits for all to complete.

**Try it:**

```bash
curl "http://localhost:8080/parallel?topic=dragons&country=France&style=comedy"
```

### 3. Standalone Agent with Tool Calls — Research Writer

An agent that uses `@Tool`-annotated methods to fetch data. Tool calls are automatically routed through `ToolCallActivity` Dapr activities.

```java
public interface ResearchWriter {

  @ToolBox(ResearchTools.class)
  @UserMessage("""
      You are a research assistant.
      Write a concise 2-sentence summary about the country {{country}}
      using the available tools to fetch accurate data.
      Return only the summary.
      """)
  @Agent(name = "research-location-agent",
      description = "Researches and summarises facts about a country", outputKey = "summary")
  String research(@V("country") String country);
}
```

The tools are plain CDI beans — no Dapr-specific code needed:

```java
@ApplicationScoped
public class ResearchTools {

  @Tool("Looks up real-time population data for a given country")
  public String getPopulation(String country) {
    return switch (country.toLowerCase()) {
      case "france" -> "France has approximately 68 million inhabitants (2024).";
      case "germany" -> "Germany has approximately 84 million inhabitants (2024).";
      default -> country + " population data is not available in this demo.";
    };
  }

  @Tool("Returns the official capital city of a given country")
  public String getCapital(String country) {
    return switch (country.toLowerCase()) {
      case "france" -> "The capital of France is Paris.";
      case "germany" -> "The capital of Germany is Berlin.";
      default -> "Capital city for " + country + " is not available in this demo.";
    };
  }
}
```

When the LLM decides to call `getPopulation` or `getCapital`, the Dapr extension intercepts the call and executes it as a `ToolCallActivity`. This means every tool invocation is recorded in the Dapr workflow history and can be retried automatically on failure.

**Try it:**

```bash
curl "http://localhost:8080/research?country=France"
```

## Running the Examples

1. Set your OpenAI API key:

```bash
export OPENAI_API_KEY=sk-...
```

2. Start in Quarkus dev mode (Dapr Dev Services will start automatically):

```bash
cd quarkus/examples
mvn quarkus:dev
```

3. Call the endpoints:

```bash
# Sequential story creation
curl "http://localhost:8080/story?topic=space+exploration&style=noir"

# Parallel story + research
curl "http://localhost:8080/parallel?topic=robots&country=Japan&style=sci-fi"

# Standalone research with tool calls
curl "http://localhost:8080/research?country=Germany"
```

## Summary

`quarkus-agentic-dapr` lets you keep writing standard LangChain4j agent code while gaining the production-grade durability, observability, and fault tolerance of Dapr Workflows. Swap the dependency, add two lines of configuration, and your agents are now backed by durable workflows — no code changes required.