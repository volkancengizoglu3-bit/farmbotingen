# FarmBotInGen behavior

## Farm spot
- Open the in-game chat and tap the user-configured farm coordinate to travel to the saved farm spot.
- Do not use a map-list teleport flow.

## Buff mode
- Buff mode is optional and controlled by the user.
- Read the current ATK and DEF values and compare them with the user-configured thresholds.
- When buff mode is enabled and the configured threshold condition is met, travel to the user-configured buff location.
- The buff selection/action is assumed to be ready in-game; the bot only needs to reach the configured buff location. Do not add an NPC interaction or extra menu selection unless requested later.
- After reaching the buff location, return to the saved farm spot using the chat coordinate flow.

## Death recovery
- Detect death/respawn using confirmed on-screen signals.
- After respawn, return to the saved farm spot using the chat coordinate flow.
- If buffing is also due, handle the recovery and buff state without repeatedly triggering conflicting travel actions.

## Configuration still needed
- Farm coordinate tap location.
- Buff coordinate tap location.
- ATK threshold and DEF threshold.
- Exact visual cues for death/respawn and ATK/DEF readings.
- Whether buff should trigger when either stat is below threshold or only when both are below threshold.
