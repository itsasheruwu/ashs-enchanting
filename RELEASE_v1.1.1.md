## Ash's Enchanting v1.1.1

Bedrock/Geyser hotfix for Infinity + Mending anvil merges and result-take reliability.

### Added

- Legacy Geyser connector Bedrock detection fallback for older Geyser API variants.
- Startup warning when Geyser/Floodgate is installed but Bedrock API detection cannot bind.
- Bedrock fallback action diagnostics for anvil result-take handling.

### Changed

- Bedrock players now force compat manual-takeover when custom compat is applied, preventing UI-side result suppression for merges like Mending bow + Infinity book.
- Bedrock result handling now allows a safe fallback path for translated click/action combinations that are not represented as standard Java `InventoryAction` values.

### Preserved

- Java/Paper behavior remains unchanged.
- Server-authoritative level charging and anti-dupe safeguards remain intact.

### Artifact

- `ashs-enchanting-1.1.1.jar`
