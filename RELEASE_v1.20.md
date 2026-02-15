## Ash's Enchanting v1.20

Built-in automatic updater plus Bedrock confirmation flow improvements.

### Added

- Built-in automatic updater:
  - Checks GitHub releases on startup.
  - Downloads newer plugin jar into `plugins/update/`.
  - Applies update on next full server restart.
- Updater config options:
  - `autoUpdateEnabled`
  - `autoUpdateAllowPrerelease`
  - `autoUpdateRepository`
  - `autoUpdateTimeoutSeconds`
- Bedrock explicit merge confirmation command:
  - `/aeconfirm` (aliases: `/ashsenchantingconfirm`, `/aec`)

### Improved

- Bedrock compat merge confirmation now uses explicit command confirmation by default.

### Preserved

- Existing Bedrock/Geyser merge compatibility behavior remains active.
- Java/Paper behavior remains unchanged.

### Artifact

- `ashs-enchanting-1.20.jar`
