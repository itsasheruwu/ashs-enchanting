## Ash's Enchanting v1.1.9

Bedrock compat confirmation fix: replaced crouch-only confirmation with explicit command confirmation.

### Added

- New Bedrock confirm command: `/aeconfirm` (aliases: `/ashsenchantingconfirm`, `/aec`) for explicit compat merge confirmation.

### Changed

- Added `bedrockCompatAutoApplyRequiresCommandConfirm` config (default `true`).
- `bedrockCompatAutoApplyRequiresSneak` now defaults to `false` and is optional only.
- Bedrock hint messaging now points users to `/aeconfirm` when confirmation is required.

### Preserved

- Existing Bedrock compat merge support remains active.
- Java/Paper behavior remains unchanged.

### Artifact

- `ashs-enchanting-1.1.9.jar`
