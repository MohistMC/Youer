---
id: workflow.world-editing
title: World Editing Workflow
description: Plan and apply bounded block, biome, structure, and region edits.
commands: [clone, fill, fillbiome, place, setblock]
tools: [search_commands, execute_console_command]
execution: mixed
risk: SERVER_ACTION
---

# Purpose
Use this workflow for deliberate world edits. Large edits can be destructive, lag-inducing, and difficult to reverse.

# Preconditions
The requester must be allowed to view this workflow and every declared command and tool. Resolve all targets from live server state.
Before any complex mutation, call `load_skill` for each selected atomic command Skill and follow its command-specific constraints.
A Skill cannot bypass Tool permissions or confirmation; risk classification and execution context remain authoritative.
Stop on ambiguity instead of guessing targets, identifiers, coordinates, values, or intended scope.

# Procedure
1. Confirm the dimension, source and destination coordinates, intended block or structure, replacement mode, and maximum affected volume.
2. Calculate or estimate the affected region and reject ambiguous relative coordinates in console context.
3. Prefer a small test or single `setblock` when validating an unfamiliar material or placement.
4. Present the exact command for confirmation, then execute only the approved edit.
5. Inspect the command result and do not retry a partial edit with broader coordinates.

# Validation
Validate the actual Tool result against the requested outcome. For multi-step work, take a fresh capability snapshot before subsequent Tool calls and do not treat an earlier permission or confirmation as reusable authorization.

# Failure handling
Stop the workflow on denial, missing commands or tools, invalid syntax, changed server state, confirmation refusal, timeout, or partial failure. Preserve successful earlier state, describe it accurately, and request new direction before any compensating action that was not already authorized.

# Examples
`/fill 0 64 0 10 64 10 minecraft:stone replace`
