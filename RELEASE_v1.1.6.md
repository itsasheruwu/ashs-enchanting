## Ash's Enchanting v1.1.6

Bedrock/Geyser fallback for custom anvil merges when the client suppresses output-slot interaction.

### Changed

- Added Bedrock-only auto-apply fallback for compat anvil merges after input placement, so merges can complete even when Bedrock does not allow output-slot take for custom combinations.
- Added Bedrock auto-apply log diagnostics with result and cost details.

### Preserved

- Java/Paper output-slot behavior is unchanged.
- Existing server-authoritative level checks, consume flow, and anti-dupe ordering remain intact.

### Artifact

- `ashs-enchanting-1.1.6.jar`
