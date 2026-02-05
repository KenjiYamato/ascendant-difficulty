# Ascendant Difficulty

Mod for the Hytale Server that manages per-player difficulty tiers and dynamically scales enemies and rewards.

## Features
- Per-player difficulty tiers with fully configurable tiers (including Ascendant I-XX).
- Player UI selection page (`/ascendant-difficulty`) with icons, descriptions, and paging.
- HUD badge that shows the current difficulty (optional toggle).
- Enemy HP scales based on the nearest player in range.
- Incoming damage to players is multiplied per tier.
- Enemy "armor" reduces player damage dealt (configurable per tier).
- Loot scaling: drop rate, drop quantity, and drop quality per tier.
- XP and cash multipliers per tier (with random cash variance).
- Tier metadata (name, description, color, image/icon) is configurable.
- Per-player tier overrides are persisted.

## Configuration
The default config is written to `config/ascendant/difficulty.json`.

Key sections:
- `base`: global switches and limits (e.g. allowDifficultyChange, allowUIBadge, min/max health scaling).
- `tiers`: numeric multipliers per tier (health, damage, armor, loot, XP, cash).
- `meta`: display names, descriptions, and icon paths per tier.

Player overrides are stored in `config/ascendant/difficulty-players.json`.

## Command and Permission
- Command: `/ascendant-difficulty`
- Permission: `ascendant.difficulty`

## Integrations
- XP: LevelingCore (XP gain listener).
- Cash: Ecotale (deposits per kill/drop).

If the APIs are missing, only the core difficulty features run.