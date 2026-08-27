---
id: youer.entityclear
title: Entityclear Command
description: Run entity or item cleanup and manage cleanup exclusion lists.
commands: [entityclear]
tools: [search_commands, execute_console_command]
execution: console
risk: SERVER_ACTION
---

# Purpose
Run entity or item cleanup and manage cleanup exclusion lists. This Skill describes the command shipped and registered by Youer.

# Preconditions
The command must be enabled and registered on the running server. The requesting player must have AI Skill access, access to every declared tool, and the command's own permission. Player-only commands also require a live player context.

# Procedure
1. Clarify the desired subcommand, target, and scope.
2. Call `search_commands` to confirm the command is currently registered and to inspect its live usage when arguments are uncertain.
3. Construct one explicit command without a leading slash. Avoid guesses about player names, plugin names, worlds, files, registry IDs, or configuration values.
4. Execute through `execute_console_command`; permission checks, risk confirmation, and main-thread dispatch remain authoritative in the Tool layer.
5. Read the command result before proposing or running any follow-up.

# Validation
Confirm the response matches the requested operation. For configuration changes, report exactly what changed; for diagnostics, report the observed result without adding an unrelated mutation.

# Failure handling
Do not bypass a disabled command, missing dependency, denied permission, confirmation refusal, player-only restriction, or failed syntax. Use live command discovery to correct arguments, or explain the unmet precondition.

# Examples
`/entityclear all`

