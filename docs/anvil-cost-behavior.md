# Anvil Cost Display Behavior (True 40+ UI vs 39 Fallback)

## Summary

This plugin separates:
- **Displayed UI cost** in the anvil window
- **Actual charged cost** when the result is taken

With `disableTooExpensive: true`, the plugin always charges the true calculated cost server-side.

## Why This Is Necessary

Minecraft clients hardcode special handling for anvil operations at `40+` levels:
- If the anvil repair cost shown to the client is `>= 40`, the UI shows `"Too Expensive!"`.
- Standard server-side Spigot APIs alone cannot override this text behavior.

## How Ash's Enchanting Handles It

1. The plugin computes or reads the true operation cost.
2. If `showTrueCostAbove40InAnvilUi: true` and ProtocolLib is installed, the plugin temporarily spoofs client `instabuild` ability while the anvil state is active so the native UI can display numeric `40+` cost.
3. If ProtocolLib is unavailable (or true-cost UI mode is disabled), it falls back to displaying `39` when true cost is `>= 40`.
4. On successful result take, it charges the true cost from player levels.
5. Private true-cost chat messaging is controlled by `showTrueCostChatMessage` (default `fallback-only`).

## Infinity + Mending Compatibility

For Infinity+Mending override paths where vanilla would normally block, the plugin uses a vanilla-style cost model:
- prior work penalty (`RepairCost`)
- enchant multipliers (including enchanted-book adjustments)
- rename surcharge
- compatibility checks (only Infinity <-> Mending conflict is relaxed)

This keeps compatibility pricing as close to vanilla logic as possible when bypassing that one incompatibility rule.
