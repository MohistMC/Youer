---
id: workflow.server-operations
title: Server Operations Workflow
description: Perform controlled diagnostics, saving, tick management, transfers, and shutdown operations.
commands: [debug, jfr, list, perf, save-all, save-off, save-on, seed, setidletimeout, stop, tick, transfer]
tools: [search_commands, execute_console_command]
execution: mixed
risk: SERVER_ACTION
---

# Purpose
Use this workflow for dedicated-server operations with clear recovery and service-impact awareness.

# Preconditions
The requester must be allowed to view this workflow and every declared command and tool. Resolve all targets from live server state.
Before any complex mutation, call `load_skill` for each selected atomic command Skill and follow its command-specific constraints.
A Skill cannot bypass Tool permissions or confirmation; risk classification and execution context remain authoritative.
Stop on ambiguity instead of guessing targets, identifiers, coordinates, values, or intended scope.

# Procedure
1. Classify the request as observation, profiling, persistence, tick control, player transfer, or shutdown.
2. Inspect current status first when possible and state the expected impact.
3. For save disabling, profiling, freezing ticks, transfer, or shutdown, define the matching recovery or completion step before execution.
4. Execute one operation after confirmation and retain its server response.
5. Validate completion; never issue `stop`, leave saving disabled, or leave a profiler running as an implicit follow-up.

# Validation
Validate the actual Tool result against the requested outcome. For multi-step work, take a fresh capability snapshot before subsequent Tool calls and do not treat an earlier permission or confirmation as reusable authorization.

# Failure handling
Stop the workflow on denial, missing commands or tools, invalid syntax, changed server state, confirmation refusal, timeout, or partial failure. Preserve successful earlier state, describe it accurately, and request new direction before any compensating action that was not already authorized.

# Examples
`/save-all flush` before an explicitly requested `/stop`
