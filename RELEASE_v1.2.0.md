## Ash's Enchanting v1.2.0

Minor milestone release that includes built-in automatic updater and Bedrock confirmation flow improvements from recent hotfixes.

### Added

- Built-in automatic updater:
  - Checks GitHub releases on startup.
  - Downloads newer jar to `plugins/update/`.
  - Applies update on next full restart.
- Updater config options:
  - `autoUpdateEnabled`
  - `autoUpdateAllowPrerelease`
  - `autoUpdateRepository`
  - `autoUpdateTimeoutSeconds`

### Improved

- Bedrock compat merge confirmation uses explicit `/aeconfirm` command by default.
- Bedrock/Geyser fallback handling for custom anvil merge paths remains active.

### Preserved

- Java/Paper behavior remains unchanged.

### Artifact

- `ashs-enchanting-1.2.0.jar`
