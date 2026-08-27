---
id: youer.bans
title: Bans Command
description: Configure Youer's item, entity, enchantment, recipe, block, world, or structure restrictions.
commands: [bans]
tools: [search_commands, execute_player_command]
execution: player
risk: SERVER_ACTION
---

# Purpose
Configure Youer's item, entity, enchantment, recipe, block, world, or structure restrictions. This Skill describes the command shipped and registered by Youer.

# Preconditions
The command must be enabled and registered on the running server. The requesting player must have AI Skill access, access to every declared tool, and the command's own permission. Player-only commands also require a live player context.

# Procedure
1. Clarify the desired subcommand, target, and scope.
2. Call `search_commands` to confirm the command is currently registered and to inspect its live usage when arguments are uncertain.
3. Construct one explicit command without a leading slash. Avoid guesses about player names, plugin names, worlds, files, registry IDs, or configuration values.
4. Execute through `execute_player_command`; permission checks, risk confirmation, and main-thread dispatch remain authoritative in the Tool layer.
5. Read the command result before proposing or running any follow-up.

# Validation
Confirm the response matches the requested operation. For configuration changes, report exactly what changed; for diagnostics, report the observed result without adding an unrelated mutation.

# Failure handling
Do not bypass a disabled command, missing dependency, denied permission, confirmation refusal, player-only restriction, or failed syntax. Use live command discovery to correct arguments, or explain the unmet precondition.

# Examples
`/bans show item`

