---
id: workflow.player-state
title: Player State Workflow
description: Safely inspect or change player inventory, effects, progress, game mode, position, and respawn state.
commands: [advancement, attribute, clear, effect, enchant, experience, gamemode, give, item, spawnpoint, teleport]
tools: [search_commands, execute_player_command]
execution: mixed
risk: PLAYER_ACTION
---

# Purpose
Use this workflow for coordinated changes to one or more aspects of a player's state while preserving the requesting player's identity and permissions.

# Preconditions
The requester must be allowed to view this workflow and every declared command and tool. Resolve all targets from live server state.
Before any complex mutation, call `load_skill` for each selected atomic command Skill and follow its command-specific constraints.
A Skill cannot bypass Tool permissions or confirmation; risk classification and execution context remain authoritative.
Stop on ambiguity instead of guessing targets, identifiers, coordinates, values, or intended scope.

# Procedure
1. Identify the exact online target and list each requested state change.
2. Query or inspect current state when the command supports it, and distinguish additive operations from replacements.
3. Resolve item, effect, enchantment, advancement, attribute, and coordinate identifiers before execution.
4. Execute the smallest necessary command through player context, then validate its result.
5. For multiple changes, re-check the remaining plan after each command and stop on the first failure.

# Validation
Validate the actual Tool result against the requested outcome. For multi-step work, take a fresh capability snapshot before subsequent Tool calls and do not treat an earlier permission or confirmation as reusable authorization.

# Failure handling
Stop the workflow on denial, missing commands or tools, invalid syntax, changed server state, confirmation refusal, timeout, or partial failure. Preserve successful earlier state, describe it accurately, and request new direction before any compensating action that was not already authorized.

# Examples
`/effect give @s minecraft:speed 30 1 true` then `/spawnpoint @s ~ ~ ~`
