---
id: workflow.entity-control
title: Entity Control Workflow
description: Inspect, select, move, tag, damage, remove, or summon entities with tightly bounded selectors.
commands: [damage, data, kill, ride, spreadplayers, summon, tag]
tools: [search_commands, execute_player_command, execute_console_command]
execution: mixed
risk: SERVER_ACTION
---

# Purpose
Use this workflow for entity operations where selector scope and NBT correctness are safety-critical.

# Preconditions
The requester must be allowed to view this workflow and every declared command and tool. Resolve all targets from live server state.
Before any complex mutation, call `load_skill` for each selected atomic command Skill and follow its command-specific constraints.
A Skill cannot bypass Tool permissions or confirmation; risk classification and execution context remain authoritative.
Stop on ambiguity instead of guessing targets, identifiers, coordinates, values, or intended scope.

# Procedure
1. Define entity type, dimension, distance, count limit, and sort order. Never default to an unrestricted `@e` selector for a mutation.
2. Use a read-only query such as `data get` when identity or state must be confirmed.
3. Validate resource identifiers and any NBT structure before execution.
4. Execute a single bounded action and inspect the affected entity count.
5. If zero or too many entities matched, stop and revise the selector instead of broadening it automatically.

# Validation
Validate the actual Tool result against the requested outcome. For multi-step work, take a fresh capability snapshot before subsequent Tool calls and do not treat an earlier permission or confirmation as reusable authorization.

# Failure handling
Stop the workflow on denial, missing commands or tools, invalid syntax, changed server state, confirmation refusal, timeout, or partial failure. Preserve successful earlier state, describe it accurately, and request new direction before any compensating action that was not already authorized.

# Examples
`/kill @e[type=minecraft:zombie,distance=..16,limit=10,sort=nearest]`
