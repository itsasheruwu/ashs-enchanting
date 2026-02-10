# Ash's Enchanting

`Ash's Enchanting` is a Spigot plugin for Minecraft 1.21.11 that:
- Removes anvil "Too Expensive!" completion blocking while keeping vanilla-calculated level cost.
- Allows Infinity and Mending to coexist on bows.
- Optionally allows the same coexistence rule on crossbows, including Infinity application to crossbows.

## Build

Requirements:
- Java 21
- Maven 3.9+

Build command:

```bash
mvn clean package
```

Output jar:
- `target/ashs-enchanting-1.0.4.jar`

## Install

1. Stop the server.
2. Copy `target/ashs-enchanting-1.0.4.jar` to the server `plugins/` folder.
3. Start the server once to generate config.
4. Edit `plugins/AshsEnchanting/config.yml` as needed.
5. Restart server.

## Default Config

```yaml
disableTooExpensive: true
allowInfinityMendingOnBows: true
alsoAllowOnCrossbows: false
chargeCreative: false
useLogger: true
```

## Config Behavior

- `disableTooExpensive`
  - `true`: anvil result take is allowed even when vanilla would block with "Too Expensive!".
  - Level cost charged is the anvil's server-computed cost.

- `allowInfinityMendingOnBows`
  - `true`: Infinity and Mending can coexist on bows through anvil merges/book application.

- `alsoAllowOnCrossbows`
  - `true`: extends Infinity+Mending coexistence to crossbows and also permits Infinity application to crossbows through anvil/book paths.

- `chargeCreative`
  - `false`: creative players are not charged levels.
  - `true`: creative players are charged the same as non-creative.

- `useLogger`
  - Enables plugin info logging.

## Notes: Dupe Prevention

- Result-slot manual takeovers are processed with a per-player reentrancy guard.
- Unsupported result click types are cancelled and inventory is resynced.
- Inputs are consumed only after result delivery succeeds (cursor/inventory fit checks first).
- Session state and processing locks are cleared on anvil close and player quit.

## Notes: Bedrock/Geyser Safety

- Manual takeovers force immediate `player.updateInventory()`.
- A 1-tick delayed second `updateInventory()` is scheduled to reduce Java/Bedrock UI divergence.
- All logic is server-side only; no client mods or custom packets are required.

## Anvil Break Behavior

- Vanilla anvil break/damage behavior is preserved automatically when the plugin does not need to take over result handling.
- During forced manual result handling paths, plugin applies best-effort vanilla-like 12% break chance and progression:
  - `ANVIL -> CHIPPED_ANVIL -> DAMAGED_ANVIL -> AIR`
