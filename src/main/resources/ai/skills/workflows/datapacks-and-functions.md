---
id: workflow.datapacks-and-functions
title: Data Packs And Functions Workflow
description: Inspect data packs and run, schedule, return from, or reload data-pack functions.
commands: [datapack, function, reload, return, schedule]
tools: [search_commands, execute_console_command]
execution: mixed
risk: SERVER_ACTION
---

# Purpose
Use this workflow for data-pack lifecycle and function execution. Reloading or scheduling can affect the entire server.

# Preconditions
The requester must be allowed to view this workflow and every declared command and tool. Resolve all targets from live server state.
Before any complex mutation, call `load_skill` for each selected atomic command Skill and follow its command-specific constraints.
A Skill cannot bypass Tool permissions or confirmation; risk classification and execution context remain authoritative.
Stop on ambiguity instead of guessing targets, identifiers, coordinates, values, or intended scope.

# Procedure
1. Inspect enabled and available data packs before changing pack state.
2. Confirm the exact namespaced function ID and whether execution should be immediate or scheduled.
3. For schedules, confirm delay, append-or-replace behavior, and whether an existing schedule should be cleared.
4. Run or reload only after confirmation, then inspect server output for data-pack or function errors.
5. Do not repeatedly reload after parse failures; report the first actionable error.

# Validation
Validate the actual Tool result against the requested outcome. For multi-step work, take a fresh capability snapshot before subsequent Tool calls and do not treat an earlier permission or confirmation as reusable authorization.

# Failure handling
Stop the workflow on denial, missing commands or tools, invalid syntax, changed server state, confirmation refusal, timeout, or partial failure. Preserve successful earlier state, describe it accurately, and request new direction before any compensating action that was not already authorized.

# Examples
`/schedule function minecraft:example 10s replace`
