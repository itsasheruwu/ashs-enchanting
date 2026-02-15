## Ash's Enchanting v1.1.0

Bedrock/Geyser anvil compatibility stabilization while preserving Java behavior.

### Added

- Runtime Bedrock player detection with automatic Floodgate-first, then Geyser API fallback (reflection-based, no hard dependency required).
- Focused diagnostics for unsupported anvil result actions, including player id, client type, click/action, and slot details.

### Changed

- Bedrock players now bypass true `40+` UI ability spoof mode and use stable `39` fallback display to avoid Geyser-side interaction issues.
- Custom anvil result handling now uses `InventoryAction`-based support (`PICKUP_*` and `MOVE_TO_OTHER_INVENTORY`) for better Bedrock click translation parity.
- Plugin soft dependencies now include Bedrock stack plugins for safer startup ordering.

### Preserved

- Java players keep true `40+` native anvil UI cost display when ProtocolLib is available.
- Server-authoritative cost checks/charges and all custom anvil compat logic remain unchanged.

### Artifact

- `ashs-enchanting-1.1.0.jar`
