---
id: workflow.scoreboards-and-teams
title: Scoreboards And Teams Workflow
description: Coordinate objectives, scores, teams, trigger access, and player-facing messages.
commands: [scoreboard, team, teammsg, tellraw, title, trigger]
tools: [search_commands, execute_player_command, execute_console_command]
execution: mixed
risk: SERVER_ACTION
---

# Purpose
Use this workflow to build scoreboard and team behavior without overwriting unrelated objectives, membership, or display state.

# Preconditions
The requester must be allowed to view this workflow and every declared command and tool. Resolve all targets from live server state.
Before any complex mutation, call `load_skill` for each selected atomic command Skill and follow its command-specific constraints.
A Skill cannot bypass Tool permissions or confirmation; risk classification and execution context remain authoritative.
Stop on ambiguity instead of guessing targets, identifiers, coordinates, values, or intended scope.

# Procedure
1. Inspect or confirm the objective and team names, criteria, display slot, and intended members.
2. Choose namespaced or clearly scoped identifiers to avoid collisions.
3. Create prerequisites before assigning scores, members, triggers, or displays.
4. Validate JSON text components before using `tellraw` or `title`.
5. Execute in dependency order and stop if any prerequisite command fails.

# Validation
Validate the actual Tool result against the requested outcome. For multi-step work, take a fresh capability snapshot before subsequent Tool calls and do not treat an earlier permission or confirmation as reusable authorization.

# Failure handling
Stop the workflow on denial, missing commands or tools, invalid syntax, changed server state, confirmation refusal, timeout, or partial failure. Preserve successful earlier state, describe it accurately, and request new direction before any compensating action that was not already authorized.

# Examples
`/scoreboard objectives add event_points dummy` then `/scoreboard players set @s event_points 1`
