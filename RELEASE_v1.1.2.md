## Ash's Enchanting v1.1.2

Additional Bedrock/Geyser hotfix for Infinity + Mending anvil combines that still failed on some setups.

### Changed

- Custom compat operations now always use manual result takeover when a compat result exists, removing reliance on vanilla result/cost signals.
- Compat prepare flow now mirrors the prepared result directly into anvil output slot 2 to improve Bedrock/Geyser UI synchronization.
- Result-take fallback handling now accepts safe click-based fallback paths even when `InventoryAction` is not one of the expected Java values.

### Preserved

- Java/Paper behavior remains unchanged.
- Server-authoritative charge/consume flow and anti-dupe ordering remain intact.

### Artifact

- `ashs-enchanting-1.1.2.jar`
