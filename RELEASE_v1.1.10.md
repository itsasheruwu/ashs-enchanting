## Ash's Enchanting v1.1.10

Built-in automatic updater added.

### Added

- Startup auto-update check against GitHub releases.
- Automatic download of newer plugin jar into `plugins/update/` so update applies on next server restart.
- Configurable updater settings:
  - `autoUpdateEnabled`
  - `autoUpdateAllowPrerelease`
  - `autoUpdateRepository`
  - `autoUpdateTimeoutSeconds`

### Preserved

- Existing Bedrock/Geyser compatibility behavior and commands are unchanged.
- Java/Paper anvil behavior is unchanged.

### Artifact

- `ashs-enchanting-1.1.10.jar`
