# Ash's Enchanting

`Ash's Enchanting` is a Spigot plugin for Minecraft 1.21.11 that:
- Removes anvil "Too Expensive!" completion blocking while keeping vanilla-calculated level cost.
- Allows Infinity and Mending to coexist on bows.
- Optionally allows the same coexistence rule on crossbows, including Infinity application to crossbows.
- Optionally allows all primary protection enchants on the same armor piece.

## Build

Requirements:
- Java 21
- Maven 3.9+

Build command:

```bash
mvn clean package
```

Output jar:
- `target/ashs-enchanting-1.1.8.jar`

## Install

1. Stop the server.
2. Copy `target/ashs-enchanting-1.1.8.jar` to the server `plugins/` folder.
3. Start the server once to generate config.
4. Edit `plugins/AshsEnchanting/config.yml` as needed.
5. Restart server.

## Default Config

```yaml
disableTooExpensive: true
allowInfinityMendingOnBows: true
alsoAllowOnCrossbows: false
allowAllProtectionsOnArmor: false
chargeCreative: false
useLogger: true
bedrockCompatAutoApplyRequiresSneak: true
showTrueCostAbove40InAnvilUi: true
showTrueCostChatMessage: fallback-only
```

## Config Behavior

- `disableTooExpensive`
  - `true`: anvil result take is allowed even when vanilla would block with "Too Expensive!".
  - Level cost charged is the anvil's server-computed cost.

- `allowInfinityMendingOnBows`
  - `true`: Infinity and Mending can coexist on bows through anvil merges/book application.

- `alsoAllowOnCrossbows`
  - `true`: extends Infinity+Mending coexistence to crossbows and also permits Infinity application to crossbows through anvil/book paths.

- `allowAllProtectionsOnArmor`
  - `true`: allows `Protection`, `Fire Protection`, `Blast Protection`, and `Projectile Protection` to coexist on armor through anvil combine/book application paths.
  - `false` (default): keeps vanilla mutual exclusivity for protection families.

- `chargeCreative`
  - `false`: creative players are not charged levels.
  - `true`: creative players are charged the same as non-creative.

- `useLogger`
  - Enables plugin info logging.

- `bedrockCompatAutoApplyRequiresSneak`
  - `true` (default): Bedrock compat auto-apply requires sneaking/crouching for explicit confirmation.
  - `false`: Bedrock compat auto-apply commits immediately when a compat result is prepared.

- `showTrueCostAbove40InAnvilUi`
  - `true`: if ProtocolLib is installed, client ability packets are spoofed while relevant anvil states are open so the native UI can display numeric `40+` costs.
  - If ProtocolLib is missing, plugin safely falls back to `39` display clamp behavior.

- `showTrueCostChatMessage`
  - `fallback-only` (default): private true-cost chat message appears only when ProtocolLib fallback (`39` clamp mode) is in effect.
  - `always`: private true-cost chat message appears for all 40+ bypassed operations.
  - `never`: disables the private true-cost chat message.

## Notes: Dupe Prevention

- Result-slot manual takeovers are processed with a per-player reentrancy guard.
- Unsupported result click types are cancelled and inventory is resynced.
- Inputs are consumed only after result delivery succeeds (cursor/inventory fit checks first).
- Session state and processing locks are cleared on anvil close and player quit.

## Notes: Bedrock/Geyser Safety

- Manual takeovers force immediate `player.updateInventory()`.
- A 1-tick delayed second `updateInventory()` is scheduled to reduce Java/Bedrock UI divergence.
- All logic is server-side only; no client mods are required.
- Bedrock players are detected through Floodgate/Geyser when available.
- For Bedrock stability, true `40+` UI ability spoof mode is intentionally disabled (falls back to `39` display), while true server-side cost charging and custom anvil features remain active.
- Bedrock compat auto-apply can be set to require sneak confirmation to prevent accidental commits while previewing.

## Anvil Break Behavior

- Vanilla anvil break/damage behavior is preserved automatically when the plugin does not need to take over result handling.
- During forced manual result handling paths, plugin applies best-effort vanilla-like 12% break chance and progression:
  - `ANVIL -> CHIPPED_ANVIL -> DAMAGED_ANVIL -> AIR`

## Cost Display And Technical Note

When `disableTooExpensive: true`:
- With ProtocolLib and `showTrueCostAbove40InAnvilUi: true`, the native anvil UI displays the true numeric `40+` cost.
- Without ProtocolLib (or when true-cost UI mode is disabled), the UI displays at most `39` to avoid client-side `"Too Expensive!"` block text.
- The plugin still computes and charges the true operation cost server-side.
- Private true-cost chat behavior is controlled by `showTrueCostChatMessage`.

For the deeper technical explanation, [click here](docs/anvil-cost-behavior.md).
