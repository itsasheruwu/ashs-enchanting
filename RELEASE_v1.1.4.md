## Ash's Enchanting v1.1.4

Bedrock/Geyser creative-book compatibility fix for Infinity + Mending anvil merges.

### Changed

- Infinity level extraction now performs key-based matching fallbacks (including legacy alias keys) when reading anvil source enchant data.
- Added targeted Bedrock prepare diagnostics for bow+mending+book compat misses so item meta/enchant shape is visible in logs when merge patching does not apply.

### Preserved

- Existing `v1.1.2`/`v1.1.3` Bedrock fallback/takeover behavior remains active.
- Java/Paper merge behavior remains unchanged.

### Artifact

- `ashs-enchanting-1.1.4.jar`
