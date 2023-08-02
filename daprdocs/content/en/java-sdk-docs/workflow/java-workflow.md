---
type: docs
title: "Getting started with the Dapr Workflow Java SDK"
linkTitle: "Workflow"
weight: 20000
description: How to get up and running with workflows using the Dapr Java SDK
---

{{% alert title="Note" color="primary" %}}
Dapr Workflow is currently in alpha.
{{% /alert %}}

Let’s create a Dapr workflow and invoke it using the console. With the [provided hello world workflow example](todo), you will:

- Run a [Java console application using `todo`](todo)
- Utilize the Java workflow SDK and API calls to start, pause, resume, terminate, and purge workflow instances

This example uses the default configuration from `dapr init` in [self-hosted mode](https://github.com/dapr/cli#install-dapr-on-your-local-machine-self-hosted).

In the Java example project, the `todo` file contains the setup of the app, including:
- The workflow definition 
- The workflow activity definitions
- The registration of the workflow and workflow activities 

## Prerequisites
- [Dapr CLI and initialized environment](https://docs.dapr.io/getting-started).
- Java JDK 11 (or greater):
  - [Oracle JDK](https://www.oracle.com/java/technologies/downloads), or
  - OpenJDK
- [Apache Maven](https://maven.apache.org/install.html), version 3.x.
<!-- IGNORE_LINKS -->
- [Docker Desktop](https://www.docker.com/products/docker-desktop)
<!-- END_IGNORE -->
- Verify you're using the latest proto bindings

## Set up the environment

Run the following command to install the requirements for running this workflow sample with the Dapr Java SDK.

```bash
todo
```

Clone the Java SDK repo.

```bash
git clone https://github.com/dapr/java-sdk.git
```

From the Java SDK root directory, navigate to the Dapr Workflow example.

```bash
todo
```

## Run the application locally

To run the Dapr application, you need to start the Java program and a Dapr sidecar. In the terminal, run:

```bash
todo
```


**Expected output**

```
todo
```

## What happened?

When you ran `dapr run`, the Dapr client:
1. Registered the workflow (`todo`) and its actvity (`todo`)
1. Started the workflow engine

```java
```

Dapr then paused and resumed the workflow:

```java
```

Once the workflow resumed, Dapr raised a workflow event and printed the new counter value:

```java
```

To clear out the workflow state from your state store, Dapr purged the workflow:

```java
```

The sample then demonstrated terminating a workflow by:
- Starting a new workflow using the same `instanceId` as the purged workflow.
- Terminating the workflow and purging before shutting down the workflow.

```java
```

## Next steps
- [Learn more about Dapr workflow]({{< ref workflow >}})
- [Workflow API reference]({{< ref workflow_api.md >}})