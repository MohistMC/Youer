---
id: workflow.player-moderation
title: Player Moderation Workflow
description: Investigate and perform proportionate player moderation with reversible steps where possible.
commands: [ban, ban-ip, banlist, kick, pardon, pardon-ip, whitelist, op, deop]
tools: [search_commands, execute_console_command]
execution: mixed
risk: SERVER_ACTION
---

# Purpose
Use this workflow for allowlist changes, kicks, bans, pardons, and operator membership. Treat operator grants and IP-wide actions as especially sensitive.

# Preconditions
The requester must be allowed to view this workflow and every declared command and tool. Resolve all targets from live server state.
Before any complex mutation, call `load_skill` for each selected atomic command Skill and follow its command-specific constraints.
A Skill cannot bypass Tool permissions or confirmation; risk classification and execution context remain authoritative.
Stop on ambiguity instead of guessing targets, identifiers, coordinates, values, or intended scope.

# Procedure
1. Collect the exact player profile or IP, the reason, duration expectations, and desired outcome. Never infer an IP address from a player name.
2. Inspect current state with `banlist` or `whitelist list` when relevant.
3. Choose the least-powerful command that meets the request; prefer `kick` over a ban when the request is temporary.
4. Present the exact mutating command and wait for Tool-layer confirmation.
5. Execute one change, validate the response, and stop unless the user explicitly requested additional targets.

# Validation
Validate the actual Tool result against the requested outcome. For multi-step work, take a fresh capability snapshot before subsequent Tool calls and do not treat an earlier permission or confirmation as reusable authorization.

# Failure handling
Stop the workflow on denial, missing commands or tools, invalid syntax, changed server state, confirmation refusal, timeout, or partial failure. Preserve successful earlier state, describe it accurately, and request new direction before any compensating action that was not already authorized.

# Examples
`/ban PlayerName Repeated griefing` followed, only when requested, by `/pardon PlayerName`
