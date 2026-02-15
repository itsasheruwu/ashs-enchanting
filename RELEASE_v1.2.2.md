## Ash's Enchanting v1.2.2

Added timed Bedrock auto-apply command flow.

### Added

- New command: `/autoconfirmae` (alias: `/aeautoconfirm`).
- Running the command enables Bedrock compat auto-apply for a temporary window.
- Warning chat message is sent when enabled, noting instant combine behavior during the window.

### Changed

- Bedrock compat auto-apply now requires an active `/autoconfirmae` window.
- Added `bedrockAutoApplyWindowSeconds` config (default `20`).

### Preserved

- Existing Bedrock/Geyser compatibility path remains active.
- Java/Paper behavior remains unchanged.
- Built-in auto-updater remains unchanged.

### Artifact

- `ashs-enchanting-1.2.2.jar`
