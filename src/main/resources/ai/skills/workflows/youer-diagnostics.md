---
id: workflow.youer-diagnostics
title: Youer Diagnostics Workflow
description: Collect Youer registry, object, lighting, and performance diagnostics with minimal mutation.
commands: [dump, infos, shows, pulsegrasp, lightfix]
tools: [search_commands, execute_player_command, execute_console_command]
execution: mixed
risk: SERVER_ACTION
---

# Purpose
Use this workflow to diagnose Youer or hybrid-server behavior before changing configuration or game state.

# Preconditions
The requester must be allowed to view this workflow and every declared command and tool. Resolve all targets from live server state.
Before any complex mutation, call `load_skill` for each selected atomic command Skill and follow its command-specific constraints.
A Skill cannot bypass Tool permissions or confirmation; risk classification and execution context remain authoritative.
Stop on ambiguity instead of guessing targets, identifiers, coordinates, values, or intended scope.

# Procedure
1. Define the symptom, affected world or player, and the evidence needed.
2. Prefer read-only `infos`, `shows`, `dump`, or `pulsegrasp status` before starting profilers or repairs.
3. If profiling is required, choose bounded sampling options and plan the stop step.
4. Use `lightfix` only when lighting corruption is the diagnosed scope and after confirming the affected world.
5. Report generated output locations or command results and separate observations from inferences.

# Validation
Validate the actual Tool result against the requested outcome. For multi-step work, take a fresh capability snapshot before subsequent Tool calls and do not treat an earlier permission or confirmation as reusable authorization.

# Failure handling
Stop the workflow on denial, missing commands or tools, invalid syntax, changed server state, confirmation refusal, timeout, or partial failure. Preserve successful earlier state, describe it accurately, and request new direction before any compensating action that was not already authorized.

# Examples
`/dump entitytypes` or `/pulsegrasp status`
