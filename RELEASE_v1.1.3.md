## Ash's Enchanting v1.1.3

Bedrock/Geyser compatibility fix for Infinity + Mending merge inputs that are translated as plain books.

### Changed

- Compat merge validation now treats both `ENCHANTED_BOOK` and `BOOK` as valid book-like anvil sources.
- Vanilla-like compat cost logic now applies book cost behavior to both `ENCHANTED_BOOK` and `BOOK` inputs.

### Preserved

- Java/Paper behavior remains unchanged.
- Existing Bedrock/Geyser takeover and fallback handling from `v1.1.2` remains active.

### Artifact

- `ashs-enchanting-1.1.3.jar`
