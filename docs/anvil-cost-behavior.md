# Anvil Cost Behavior (Too Expensive + True Cost)

## What Players See

When `disableTooExpensive: true`:
- The vanilla anvil UI may still show `Too Expensive!` for high-cost operations.
- The plugin still allows taking the result.
- The plugin charges the true calculated level cost on take.
- The player receives a private chat message with that true charged cost.

## Why `Too Expensive!` Can Still Appear

The label is client-side UI behavior tied to high anvil costs.
This plugin is server-side only, so it does not replace the vanilla anvil screen.

Instead, it enforces behavior on the server:
- allow take when configured,
- consume inputs,
- charge levels using the server-calculated/compat-calculated cost,
- keep inventories synchronized (including Geyser/Bedrock).

## Cost Source Rules

1. If vanilla already provides a valid anvil cost, that value is used.
2. For custom compatibility paths (vanilla-invalid merges), a vanilla-style cost is computed.
3. That charged value is also announced to the player in private chat.

## Compatibility Overrides

The plugin only overrides specific conflicts:

- Infinity <-> Mending on bows (and crossbows if enabled).
- Optional: all primary protection conflicts on armor if `allowAllProtectionsOnArmor: true`.

All other vanilla incompatibility rules remain unchanged.
