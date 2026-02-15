<!-- markdownlint-disable MD033 MD041 -->
<a id="top"></a>
<div align="center">

# Ascendant Difficulty

Mod for the Hytale Server that manages per-player difficulty tiers and dynamically scales enemies and rewards.

<br/>

![Game](https://img.shields.io/badge/Game-Hytale-2a2a2a)
![Status](https://img.shields.io/badge/Status-Experimental-6b7280)
![Build](https://img.shields.io/badge/Build-2026.02.06--aa1b071c2-444)
![Requires](https://img.shields.io/badge/Requires-HyUI-1f6feb)


![Tier UI](images/tier-selection-ui.png)
</div>

---

## Table of contents

<img src="images/hud-small-badge.png" align="right" alt="" width="86" height="101">

- [Features](#features)
- [Configuration](#configuration)
- [Dependencies](#dependencies)
- [Command and Permission](#command-and-permission)
- [Integrations (Optional)](#integrations-optional)
- [AI Assistance Disclosure](#ai-assistance-disclosure)
- [Not a public end-user release](#not-a-public-end-user-release)

---

## Features

- Per-player difficulty tiers with a configurable list (defaults include Ascendant I-XX).
- Tier selection UI (`/ascendant-difficulty`) with paging; respects `is_hidden` and `is_allowed`.
- HUD badge for the current tier with a per-player toggle (`/ascendant-difficulty-badge-toggle`) when `base.allow.uiBadge` is enabled.
- Enemy HP scales to the nearest player in range.
- Spawn tier is stored on mobs and used for reward scaling (configurable).
- Optional debug nameplate shows the spawn tier on mobs.
- Incoming damage to players scales by tier; enemy armor reduces player damage dealt.
- Loot scaling: drop rate, drop quantity, and drop quality per tier, with optional spawn-tier mismatch scaling.
- EliteMobs spawns are queued and rolled on tick to avoid spawn spikes; optional timing logs via `base.allow.debugLogging`.
- XP and cash multipliers per tier (with cash variance) when integrations are present; optional spawn-tier mismatch scaling.
- Difficulty change cooldown and combat lock (configurable).
- Per-player tier overrides and badge visibility are persisted.

<p align="right">(<a href="#top">back to top</a>)</p>

---

## Configuration

The base config is written to `config/ascendant/difficulty.json`.
Per-tier drop-ins live in `config/ascendant/difficultys/*.json`.

Base file (`difficulty.json`) sections:

- `base`: global switches/limits: `defaultDifficulty`, `cashVarianceFactor`, `playerDistanceRadiusToCheck`, `minDamageFactor`, `minHealthScalingFactor`, `maxHealthScalingFactor`, `healthScalingTolerance`, `roundingDigits`, `difficultyChangeCooldownMs`, `difficultyChangeCombatTimeoutMs`, `spawnTierRewardOverFactor`, `spawnTierRewardUnderFactor`, `eliteSpawnQueue`.
- `base.allow`: feature toggles: `difficultyChange`, `difficultyChangeInCombat`, `uiBadge`, `healthModifier`, `damageModifier`, `damagePhysical`, `damageProjectile`, `damageCommand`, `damageDrowning`, `damageEnvironment`, `damageFall`, `damageOutOfWorld`, `damageSuffocation`, `armorModifier`, `dropModifier`, `xpReward`, `cashReward`, `cashRewardEvenWithPhysical`, `spawnTierReward`, `spawnTierNameplate`, `debugCommands`, `debugLogging`, `eliteSpawn`.
- `base.integrations`: integration toggles: `eliteMobs`, `ecotale`, `levelingCore`, `mmoSkillTree`.
- `base.mmoSkillTree`: MMO SkillTree config: `xpBonusWhitelist`.
- Default `base.mmoSkillTree.xpBonusWhitelist`: `Swords`, `Daggers`, `Polearms`, `Staves`, `Axes`, `Blunt`, `Archery`, `Unarmed`.
- `base.eliteSpawnQueue`: queue settings for EliteMobs rolls: `intervalMs`, `maxPerDrain`, `maxDrainMs`.

Drop-in file format (`config/ascendant/difficultys/*.json`):

- `id` (required): tier identifier (used everywhere else, e.g. `very_easy`, `ascendant_III`).
- `order` (required): numeric sort key for UI order (can be fractional, e.g. `13.5`).
- `meta`: UI metadata per tier: `displayName`, `description`, `imagePath`, `iconPath`, `color`.
- Tier overrides: `is_allowed`, `is_hidden`, `health_multiplier`, `damage_multiplier` (optional overrides: `damage_multiplier_physical`, `damage_multiplier_projectile`, `damage_multiplier_command`, `damage_multiplier_drowning`, `damage_multiplier_environment`, `damage_multiplier_fall`, `damage_multiplier_out_of_world`, `damage_multiplier_suffocation`), `armor_multiplier`, `drop_rate_multiplier`, `drop_quantity_multiplier`, `drop_quality_multiplier`, `xp_multiplier`, `cash_multiplier`, `elite_mobs_chance_multiplier` (percent scale), `elite_mobs_chance_uncommon`, `elite_mobs_chance_rare`, `elite_mobs_chance_legendary`.
- Missing keys fall back to the base template (defaults to `1.0` for multipliers).

Ordering:

- Ordering uses the numeric `order` field only; filenames are ignored.
- If multiple entries share the same `order`, they are ordered by `id`.

Default drop-ins are shipped in `src/main/resources/difficultys` and copied to `config/ascendant/difficultys` on first run.

Player settings are stored in `config/ascendant/players-settings.json` (keys: `difficulty`, `showBadge`).
Legacy overrides from `config/ascendant/difficulty-players.json` are migrated if found.

<p align="right">(<a href="#top">back to top</a>)</p>

---

## Dependencies

- Required: `Ellie:HyUI` (tier selection page + HUD badge).

<p align="right">(<a href="#top">back to top</a>)</p>

---

## Command and Permission

- Command: `/ascendant-difficulty` (open tier selection UI)
- Command: `/ascendant-difficulty-badge-toggle` (toggle badge visibility)
- Permission: `ascendant.difficulty` (required for both commands)

Debug commands (only registered when `base.allow.debugCommands` is `true`):

- Command: `/ce` (clear non-player living entities)
- Permission: `ascendant.debug.clear_entities`
- Command: `/ci` (clear dropped items)
- Permission: `ascendant.debug.clear_items`
- Command: `/test_attack` (toggle debug max attack damage)
- Permission: `ascendant.debug.test_attack`
- Command: `/spawn_wraith [count]` (spawn Wraiths)
- Permission: `ascendant.debug.spawn_wraith`
- Command: `/tier_lowest` (set tier to lowest)
- Permission: `ascendant.debug.tier_lowest`
- Command: `/tier_highest` (set tier to highest)
- Permission: `ascendant.debug.tier_highest`

<p align="right">(<a href="#top">back to top</a>)</p>

---

## Integrations (Optional)

- EliteMobs: rolls elite spawns using `elite_mobs_chance_*` (toggle via `base.integrations.eliteMobs`).
- LevelingCore: applies bonus XP based on `xp_multiplier` and updates XP notifications; also triggers cash bonuses via Ecotale (toggle via `base.integrations.levelingCore`).
- MMOSkillTree: applies bonus XP and appends a percentage line to green `+XP` notifications (toggle via `base.integrations.mmoSkillTree`, whitelist via `base.mmoSkillTree.xpBonusWhitelist`).
- Ecotale: deposits bonus cash based on `cash_multiplier` (respects `base.allow.cashReward` and `base.allow.cashRewardEvenWithPhysical`, toggle via `base.integrations.ecotale`).

If these APIs are missing, XP/cash multipliers are skipped; core difficulty scaling (health/damage/armor/loot) still runs.

#### Notification examples:
<img src="images/integration-notification-xp-disabled-vanilla.png" align="left" alt="" width="340" height="120">
<img src="images/integration-notification-norrmal.png" alt="" width="340" height="120">
<img src="images/integration-notification-ascendant_v.png" alt="" width="340" height="120">

<p align="right">(<a href="#top">back to top</a>)</p>

---

### Integration Setup (Original Mod Configuration Required)

The following changes must be applied to the configuration files of the respective mods

#### EliteMobs
(only required if using custom roll values)

Set the default spawn chances to `0.0` in the EliteMobs Main.json:

- `UncommonChance` -> `0.0`
- `RareChance` -> `0.0`
- `LegendaryChance` -> `0.0`

This ensures that spawn rolls are handled exclusively by this integration layer.

#### LevelingCore
(only required if using custom leveling)

Disable the default XP system in the LevelingCore levelingcore.json:

- `EnableDefaultXPGainSystem` -> `false`

This prevents duplicate XP handling and allows this integration to manage XP rewards.

---

## AI Assistance Disclosure

> [!NOTE]
> This project was developed with the support of AI-based tooling.
>
> Approximately 40% of the code (including scaffolding, refactoring, configuration restructuring, and integration logic) was initially generated or iterated with the help of AI agents. All such contributions were subsequently reviewed, validated, and integrated manually.
>
> AI tooling was used as a productivity accelerator, not as an autonomous contributor. Architectural decisions, final implementations, performance validation, and maintenance responsibility remain entirely with the project maintainer.
>
> This disclosure is provided in the interest of transparency.

<p align="right">(<a href="#top">back to top</a>)</p>


---

## Not a public end-user release
> [!IMPORTANT]
> This repository is not intended to be published as a finished, end-user mod
> (e.g. on CurseForge or similar platforms).
>
> The code may be freely viewed, copied, modified, reused, and incorporated
> into other projects without restriction.
>
> The only intended limitation is scope: this project exists primarily for
> private use and experimentation, rather than as a polished, public-facing release.

 <p align="right">(<a href="#top">back to top</a>)</p>
