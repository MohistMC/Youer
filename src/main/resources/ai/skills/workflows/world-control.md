---
id: workflow.world-control
title: World Control Workflow
description: Inspect and configure difficulty, chunks, game rules, spawn, time, weather, and world borders.
commands: [difficulty, forceload, gamerule, setworldspawn, time, weather, worldborder]
tools: [search_commands, execute_player_command, execute_console_command]
execution: mixed
risk: SERVER_ACTION
---

# Purpose
Use this workflow for persistent or global world behavior changes and for temporary environmental adjustments.

# Preconditions
The requester must be allowed to view this workflow and every declared command and tool. Resolve all targets from live server state.
Before any complex mutation, call `load_skill` for each selected atomic command Skill and follow its command-specific constraints.
A Skill cannot bypass Tool permissions or confirmation; risk classification and execution context remain authoritative.
Stop on ambiguity instead of guessing targets, identifiers, coordinates, values, or intended scope.

# Procedure
1. Identify the affected dimension or world and whether the change should be persistent.
2. Read current state first where a query form exists.
3. Confirm units and bounds, especially world-border distances, transition times, chunk coordinates, and weather duration.
4. Execute one explicit change with the context required by the command.
5. Query the resulting value when possible and report whether restart or later cleanup is needed.

# Validation
Validate the actual Tool result against the requested outcome. For multi-step work, take a fresh capability snapshot before subsequent Tool calls and do not treat an earlier permission or confirmation as reusable authorization.

# Failure handling
Stop the workflow on denial, missing commands or tools, invalid syntax, changed server state, confirmation refusal, timeout, or partial failure. Preserve successful earlier state, describe it accurately, and request new direction before any compensating action that was not already authorized.

# Examples
`/gamerule doDaylightCycle false` and `/time set day`
