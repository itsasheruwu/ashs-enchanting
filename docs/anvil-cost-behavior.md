# Anvil Cost Display Behavior (Why You See 39 + True Cost In Chat)

## Summary

This plugin intentionally separates:
- **Displayed UI cost** in the anvil window
- **Actual charged cost** when the result is taken

With `disableTooExpensive: true`, the UI may show `39`, while the plugin still charges the true calculated cost.

## Why This Is Necessary

Minecraft clients hardcode special handling for anvil operations at `40+` levels:
- If the anvil repair cost shown to the client is `>= 40`, the UI shows `"Too Expensive!"`.
- This is client-side presentation logic and cannot be fully overridden with standard server-side Spigot APIs.

Because this plugin is server-side only (no client mods), the reliable way to avoid the `"Too Expensive!"` banner is:
- keep displayed cost below 40 (typically 39),
- but charge the true operation cost server-side.

## How Ash's Enchanting Handles It

1. The plugin computes or reads the true operation cost.
2. If the true cost is high enough to trigger `"Too Expensive!"`, it displays `39` in the native anvil UI.
3. On successful result take, it charges the true cost from player levels.
4. It sends the player a private message with the true charged cost.

## Infinity + Mending Compatibility

For Infinity+Mending override paths where vanilla would normally block, the plugin uses a vanilla-style cost model:
- prior work penalty (`RepairCost`)
- enchant multipliers (including enchanted-book adjustments)
- rename surcharge
- compatibility checks (only Infinity <-> Mending conflict is relaxed)

This keeps compatibility pricing as close to vanilla logic as possible when bypassing that one incompatibility rule.
