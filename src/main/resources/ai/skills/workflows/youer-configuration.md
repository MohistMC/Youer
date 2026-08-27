---
id: workflow.youer-configuration
title: Youer Configuration Workflow
description: Inspect and apply Youer administration, plugin, filter, restriction, and entity-policy changes.
commands: [youer, plugin, permission, logfilter, enchantlimit, entitydamage, entitylimits, entityclear]
tools: [search_commands, execute_player_command, execute_console_command]
execution: mixed
risk: SERVER_ACTION
---

# Purpose
Use this workflow for Youer-owned administrative settings while respecting each command's live registration and permission.

# Preconditions
The requester must be allowed to view this workflow and every declared command and tool. Resolve all targets from live server state.
Before any complex mutation, call `load_skill` for each selected atomic command Skill and follow its command-specific constraints.
A Skill cannot bypass Tool permissions or confirmation; risk classification and execution context remain authoritative.
Stop on ambiguity instead of guessing targets, identifiers, coordinates, values, or intended scope.

# Procedure
1. Identify the exact subsystem and inspect its current list or status where supported.
2. Confirm target identifiers and values; percentages must be within the command's accepted range and plugin names must match live plugins.
3. Prefer the subsystem's own reload or list operation over a broad Youer reload.
4. Execute one confirmed change and validate its response.
5. Do not load arbitrary plugin files, unload dependencies, or run broad cleanup without explicit user scope.

# Validation
Validate the actual Tool result against the requested outcome. For multi-step work, take a fresh capability snapshot before subsequent Tool calls and do not treat an earlier permission or confirmation as reusable authorization.

# Failure handling
Stop the workflow on denial, missing commands or tools, invalid syntax, changed server state, confirmation refusal, timeout, or partial failure. Preserve successful earlier state, describe it accurately, and request new direction before any compensating action that was not already authorized.

# Examples
`/enchantlimit list` or `/logfilter list`
