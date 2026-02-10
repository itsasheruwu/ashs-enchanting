# v1.0.9 — True 40+ Anvil Cost Display via ProtocolLib (Vanilla Clients)

## Summary

Vanilla clients can now see true anvil costs above 39 in the native anvil UI when ProtocolLib is present. Previously, cost was clamped to 39 and the true value was only sent in chat.

**New behavior:** ProtocolLib is used to spoof the client *instabuild* ability while the anvil is open so the native UI can render numeric 40+ cost. Server-side take/charge checks remain authoritative; no client mods or resource packs required.

## Changes

### Config & API

- **config.yml**
  - `showTrueCostAbove40InAnvilUi: true` (default: enabled) — use packet spoof so the anvil UI shows the real cost when ≥ 40.
  - `showTrueCostChatMessage: fallback-only` — send the private true-cost chat message only when we must fall back to clamp-to-39 (e.g. ProtocolLib missing).

- **PluginSettings** — New fields for the above options.

- **AnvilSessionState** — New flags: `trueCostDisplayedInUi`, `abilitySpoofActive` for tracking and cleanup.

### Optional dependency

- **ProtocolLib** — Added as optional (soft) dependency:
  - `pom.xml`: ProtocolLib as `provided` + optional.
  - `plugin.yml`: `softdepend: [ProtocolLib]`.

### Implementation

- **ClientAbilitySpoofer** — Packet abstraction with:
  - **ProtocolLibAbilitySpoofer** — Sends ABILITIES packet and toggles only *instabuild*.
  - **NoopAbilitySpoofer** — No-op when ProtocolLib is missing.
- Spoofer initialized in **AshsEnchanting** and exposed via getters.

- **AnvilPrepareListener**
  - Keeps `disableTooExpensive` / max-cost bypass (`setMaximumRepairCost(Integer.MAX_VALUE)`).
  - When true-cost UI mode is on, spoofer is available, and effective cost ≥ 40: set displayed repair cost to true cost and activate ability spoof for that player.
  - Otherwise: keep clamp-to-39; session state records whether true cost is shown in UI and whether spoof is active.

- **AnvilClickListener**
  - Server-authoritative checks unchanged (`player.getLevel() < cost` still blocks take).
  - Private “True anvil cost” chat message only in fallback/clamp mode (not when true cost is already visible in UI).

- **AnvilSessionListener & shutdown**
  - On anvil close, quit, and plugin disable: always revert spoofed ability for tracked players.

### Fallback

- If ProtocolLib is missing: log once and fall back to existing behavior (clamp to 39 + private chat). No feature regression; crafting/takeover logic unchanged.

## Behavior summary

| Scenario | UI cost | Take allowed? | Chat message? |
|----------|--------|----------------|---------------|
| ProtocolLib present, cost 55, player can afford | 55 | Yes, 55 levels charged | No |
| ProtocolLib present, cost 55, player cannot afford | 55 | Blocked server-side | No |
| ProtocolLib missing, cost 55 | 39 (clamped) | Manual take still charges 55 | Yes (fallback) |

- Closing anvil or quitting while spoof is active: ability spoof reverted.
- Plugin disable/reload: all tracked spoof state reverted.
- Infinity+Mending and protection compatibility: cost computation and charging unchanged.
- Standard &lt;40 anvil operations unchanged.

## Assumptions

- Spigot 1.21.1 API.
- No client mod or resource pack required.
- ProtocolLib optional; manual take logic remains authoritative for anti-duplication and level checks.
