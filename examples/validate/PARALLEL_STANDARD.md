# Parallel-safe example validation — the standard

The example `README.md` files are executed in CI by [mechanical-markdown](https://github.com/dapr/mechanical-markdown)
(`mm.py`) to prove that the commands a user copy-pastes actually work. Historically these
ran **one README at a time** against a single shared Dapr runtime (`dapr init` →
placement + scheduler + Redis + Zipkin).

This document defines the **standard** that lets every README run **concurrently** against
that one shared runtime without interfering with the others. `examples/validate/run-parallel.sh`
executes the READMEs in parallel; `examples/validate/check-parallel-safe.sh` enforces the
rules below in CI.

## Why interference happens

All scenarios share one Redis, one pub/sub broker, and one placement service. Two READMEs
running at the same time collide on anything they name the same way:

| Resource | Auto-isolated by Dapr? | How the standard isolates it |
| --- | --- | --- |
| **State keys** | ✅ Yes — the Redis state store uses `keyPrefix=appid` by default, so every key is stored as `<app-id>\|\|<key>` | Falls out of Rule 1 (unique app-ids) for free |
| **Dapr HTTP/gRPC port** | Sidecar-bound; two sidecars can't share one | Rule 2 (unique ports) |
| **App server port** | App-bound (subscribers/services) | Rule 2 (unique ports) |
| **Pub/sub topics** | ❌ No — topics are global on the broker | Rule 3 (namespaced topics) |
| **Actor types** | ❌ No — placement keys hosts by actor **type** globally | Rule 3 (namespaced actor types) |

## The rules

Each README owns a **slug** = its path under `examples/src/main/java/io/dapr/examples/`
with `/` replaced by `-`. Examples: `pubsub`, `invoke/http` → `invoke-http`,
`pubsub/stream` → `pubsub-stream`.

### Rule 1 — Unique app-id (prefix with the slug)

Every `--app-id` in a README **must start with that README's slug**. No bare/shared ids.

```
# before (collides: 13 READMEs use "subscriber")
dapr run --app-id subscriber      ...
# after
dapr run --app-id pubsub-subscriber ...
```

This is the keystone: unique app-ids give **state-key isolation for free** (see table).

**Also update component `scopes`.** A Dapr component YAML may carry a `scopes:` list of
app-ids; Dapr only exposes that component to the app-ids listed. If you rename an app-id
without updating the scope, the app fails at runtime with e.g. `pubsub <name> is not
found`. Today the only scoped component is
[`examples/components/pubsub/redis_messagebus.yaml`](../components/pubsub/redis_messagebus.yaml)
(shared by the `pubsub` and `pubsub/stream` examples) — its `scopes` must list the
renamed app-ids of both examples.

### Rule 2 — Unique ports from the port registry

- **Dapr sidecar ports** — **omit** `--dapr-http-port` and `--dapr-grpc-port` entirely.
  The Dapr CLI defaults both to `-1` (confirmed in `dapr/cli` `cmd/run.go`), i.e. it
  auto-assigns a free port per sidecar, and the SDK reads the injected `DAPR_HTTP_PORT`/
  `DAPR_GRPC_PORT` env vars automatically. So concurrent `dapr run`s never fight over
  `3500`/`50001`. Any README still passing the literal defaults `--dapr-http-port 3500`
  or `--dapr-grpc-port 50001` is a violation — remove the flag.
- **App server ports** (subscribers/services that bind a port) — these are *not* auto:
  the app binds a specific port and tells Dapr via `--app-port`, so they must be unique
  across concurrently-running READMEs. Assign from the **port base** below: `base`,
  `base+1`, … for each server app in the README.

Client-only examples (publishers, state/secrets clients) run a sidecar but bind no server,
so they need no `--app-port` — a unique app-id + omitted (auto) dapr ports suffice.

| Slug | Port base | | Slug | Port base |
| --- | --- | --- | --- | --- |
| actors | 3100 | | pubsub-stream | 3300 |
| bindings-http | 3120 | | querystate | 3320 |
| configuration | 3140 | | secrets | 3340 |
| conversation | 3160 | | state | 3360 |
| crypto | 3180 | | tracing | 3380 |
| exception | 3200 | | unittesting | 3400 |
| invoke-grpc | 3220 | | workflows | 3420 |
| invoke-http | 3240 | | | |
| jobs | 3260 | | | |

### Rule 3 — Namespace anything the runtime keys globally

Pub/sub **topic names** and user-defined **actor type names** must carry the slug so a
shared broker/placement never crosses two READMEs:

```
# topic
testingtopic            ->  pubsub-testingtopic
# actor type
@ActorType(name = "DemoActor")  ->  @ActorType(name = "ActorsDemoActor")
```

Publisher and subscriber **within the same README** share the namespaced topic; different
READMEs never share one. (Workflow-internal actor types are already app-id-scoped by Dapr,
so Rule 1 covers workflow examples — to be confirmed by the CI probe.)

### Rule 4 — Self-contained lifecycle

Each README must `dapr stop` every app-id it starts, so nothing leaks into a concurrently
running scenario.

## Migration note — shared state across app-ids

Rule 1 relies on state being app-id-scoped. If an example **intentionally** has two
different app-ids read/write the *same* state key, app-id prefixing would break it. No
current example does this; the migration must flag any that would.

## Enforcement

`examples/validate/check-parallel-safe.sh` fails CI on: a `--app-id` not prefixed with its
slug, a default `3500`/`50001` port, or an `--app-port` reused across two READMEs. Topic
and actor-type namespacing are enforced by review + this document.
