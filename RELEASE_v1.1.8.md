## Ash's Enchanting v1.1.8

Bedrock/Geyser confirmation improvement for compat auto-apply merges.

### Changed

- Added `bedrockCompatAutoApplyRequiresSneak` config (default `true`) to require explicit crouch/sneak confirmation before Bedrock compat auto-apply commits.
- Added one-time Bedrock hint message in anvil sessions when compat merge is available but sneak confirmation is required.
- Session cleanup now clears Bedrock auto-apply hint state.

### Preserved

- Bedrock compat merge support remains active.
- Java/Paper behavior remains unchanged.

### Artifact

- `ashs-enchanting-1.1.8.jar`
