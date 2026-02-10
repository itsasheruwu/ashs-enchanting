# Ash's Enchanting

`Ash's Enchanting` is a Spigot plugin for Minecraft 1.21.11.

## Features

- Lets players complete anvil operations even when vanilla shows `Too Expensive!`.
- Charges the true server-side operation cost when the result is taken.
- Allows Infinity + Mending coexistence on bows.
- Optionally allows the same Infinity + Mending behavior on crossbows.
- Optionally allows all primary protection enchants on the same armor piece:
  - `Protection`
  - `Fire Protection`
  - `Blast Protection`
  - `Projectile Protection`

## Build

Requirements:
- Java 21
- Maven 3.9+

```bash
mvn clean package
```

Output jar:
- `target/ashs-enchanting-1.0.8.jar`

## Install

1. Stop server.
2. Put `target/ashs-enchanting-1.0.8.jar` in `plugins/`.
3. Start server once to generate config.
4. Edit `plugins/AshsEnchanting/config.yml`.
5. Restart server.

## Default Config

```yaml
disableTooExpensive: true
allowInfinityMendingOnBows: true
alsoAllowOnCrossbows: false
allowAllProtectionsOnArmor: false
chargeCreative: false
useLogger: true
```

## Config Notes

- `disableTooExpensive`
  - Enables result-take bypass for operations that vanilla blocks with `Too Expensive!`.
  - The anvil UI can still show `Too Expensive!` (client behavior), but the plugin permits completion and charges true cost.

- `allowInfinityMendingOnBows`
  - Enables Infinity + Mending coexistence on bows through anvil combine/book flows.

- `alsoAllowOnCrossbows`
  - Extends Infinity + Mending behavior to crossbows.

- `allowAllProtectionsOnArmor`
  - Allows the four main protection enchants to coexist on armor pieces via anvil combine/book flows.
  - Default is `false`.

- `chargeCreative`
  - `false`: creative players are not charged.
  - `true`: creative players are charged like survival players.

- `useLogger`
  - Enables plugin info logging.

## True Cost Chat Message

On successful anvil result take, the plugin sends the player a private message with the true charged level cost.
That message includes a clickable **Click here** link to the technical note:

- [Technical Cost Note](docs/anvil-cost-behavior.md)

## Dupe Prevention

- Per-player result-take reentrancy guard.
- Input snapshot validation before custom compat fallback result use.
- Inputs consumed only after result delivery succeeds.
- Session and lock cleanup on inventory close and player quit.

## Bedrock/Geyser Safety

- Immediate `player.updateInventory()` after manual takeover.
- One-tick delayed second `updateInventory()` refresh.
- Server-side only behavior; no client mods required.

## Anvil Break Behavior

- Vanilla break/damage behavior is preserved when vanilla handles the result take.
- In manual takeover paths, best-effort vanilla-like 12% progression is applied:
  - `ANVIL -> CHIPPED_ANVIL -> DAMAGED_ANVIL -> AIR`
