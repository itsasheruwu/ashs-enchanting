## Ash's Enchanting v1.1.7

Bedrock/Geyser hard fallback for custom anvil merges when client-side UI blocks output interaction.

### Changed

- Added prepare-phase Bedrock auto-apply for compat merges: when a valid compat result is prepared and inputs remain unchanged, the server completes the merge directly on next tick.
- Bedrock auto-apply now runs independently of output-slot click handling, covering cases where Bedrock never sends result-take interaction packets.
- Added `Bedrock prepare auto-apply: ...` diagnostics for confirmation during testing.

### Preserved

- Java/Paper anvil behavior remains unchanged.
- Existing server-authoritative level checks, item consumption, and anvil damage behavior remain intact.

### Artifact

- `ashs-enchanting-1.1.7.jar`
