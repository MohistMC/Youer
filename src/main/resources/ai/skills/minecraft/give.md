---
id: minecraft.give
title: Give Command
description: Give an item stack to selected players.
edition: java
minecraft_version: 1.21.1
commands: [give]
tools: [search_commands, execute_player_command]
execution: player
risk: PLAYER_ACTION
source: https://minecraft.wiki/w/Commands/give
---

# Purpose
Give an item stack to selected players. Use this Skill for Minecraft Java Edition 1.21.1 dedicated-server syntax.

# Preconditions
The requesting player must have AI Skill access, access to every declared tool, and permission to use the command. Confirm target selectors, coordinates, dimensions, identifiers, and quantities before any state-changing execution.

# Procedure
1. Restate the requested outcome and identify the exact target scope.
2. Call `search_commands` when syntax, aliases, plugin interception, or argument availability is uncertain.
3. Build one explicit command without a leading slash and avoid broad selectors unless the user requested them.
4. Execute through `execute_player_command`; the Tool layer remains responsible for permission checks, risk confirmation, and main-thread dispatch.
5. Inspect the returned command result before deciding whether another command is necessary.

# Validation
Verify the server response and confirm that the intended target changed. For read-only queries, report the returned value without issuing a mutating follow-up.

# Failure handling
Do not bypass a denied permission, hidden command, confirmation refusal, invalid selector, or failed syntax. Narrow the command, correct its arguments from live command discovery, or explain the failure to the user.

# Examples
`/give @s minecraft:stone 1`

