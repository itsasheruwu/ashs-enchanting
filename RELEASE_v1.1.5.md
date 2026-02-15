## Ash's Enchanting v1.1.5

Bedrock/Geyser creative-book compatibility hardening for Infinity + Mending merges.

### Changed

- Added serialized-meta enchant parsing fallback when Bukkit’s direct/stored enchant getters return empty, improving detection of Geyser-translated creative books.
- Added broader enchant key normalization/alias resolution for fallback parsing.
- Expanded Bedrock compat-miss log output to include serialized item meta snapshot.

### Preserved

- Existing anvil takeover/fallback behavior from earlier Bedrock hotfixes remains active.
- Java/Paper behavior remains unchanged.

### Artifact

- `ashs-enchanting-1.1.5.jar`
