from __future__ import annotations

"""Parse NPC role JSON data and aggregate combat/base stats into a single output file."""

import json
import re
import zipfile
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Final, Iterable


@dataclass(frozen=True, slots=True)
class Defaults:
    """Fallback values used when a key is not found in a role JSON tree."""

    maxSpeed: Any = 0
    wanderRadius: Any = 0
    viewRange: Any = 0
    hearingRange: Any = 0
    combatRelativeTurnSpeed: Any = 0
    regeneration: Any = 0


@dataclass(frozen=True, slots=True)
class ParserConfig:
    """Configuration for parser behavior and output destination."""

    include_raw_list: bool = False
    include_source: bool = False
    output_filename: str = "npc_roles.json"


JsonObject = dict[str, Any]
JsonSource = tuple[str, bytes]

KEYS: Final[tuple[str, ...]] = (
    "maxSpeed",
    "wanderRadius",
    "viewRange",
    "hearingRange",
    "combatRelativeTurnSpeed",
    "regeneration",
)


class NpcRoleParser:
    """Parser for extracting role values and damage stats from JSON sources."""

    def __init__(
        self,
        base_dir: Path,
        defaults: Defaults | None = None,
        config: ParserConfig | None = None,
    ) -> None:
        self.base_dir = base_dir
        self.defaults = defaults or Defaults()
        self.config = config or ParserConfig()

    def run(self) -> None:
        """Build result payload and write it to the configured output file."""
        result = self.build_result()
        out_file = self.base_dir / self.config.output_filename
        out_file.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")

    def build_result(self) -> JsonObject:
        """Build full output structure from all available role JSON sources."""
        sources, mode, source_meta = self._resolve_sources()
        roles: JsonObject = {}
        result: JsonObject = {"roles": roles}
        if self.config.include_source:
            result["_source"] = {
                "mode": mode,
                "root": "Server/NPC/Roles",
                "zips": source_meta["zips"],
                "scanned_files": len(sources),
            }

        for rel_path, raw in sources:
            try:
                data = self._parse_role_json_bytes(raw)
            except Exception as e:
                roles[rel_path] = {
                    "error": f"parse_error: {e.__class__.__name__}: {e}",
                }
                continue

            role_name = Path(rel_path).stem
            roles[role_name] = self._build_role_object(data)

        return result

    def _build_role_object(self, data: JsonObject) -> JsonObject:
        values: JsonObject = {}
        for key in KEYS:
            value = self._find_first_key_value(data, key)
            if value is None:
                value = getattr(self.defaults, key)
            values[key] = value

        raw_calcs: list[JsonObject] = []
        self._collect_damage_calculators(data, raw_calcs)

        base_damage_by_interaction: dict[str, JsonObject] = {}
        raw_base_damage_list: list[JsonObject] = []

        for calculator in raw_calcs:
            json_path = calculator.get("json_path")
            interaction_var = self._extract_interaction_var(str(json_path)) or "Unknown"
            base_damage = calculator.get("BaseDamage")
            damage = base_damage if isinstance(base_damage, dict) else {}

            entry: JsonObject = {
                "Type": calculator.get("Type"),
                "RandomPercentageModifier": calculator.get("RandomPercentageModifier"),
                "Damage": dict(damage),
            }

            unique_key = self._make_unique_interaction_key(interaction_var, base_damage_by_interaction)
            base_damage_by_interaction[unique_key] = entry

            if self.config.include_raw_list:
                raw_base_damage_list.append(
                    {
                        "Interaction_Var": interaction_var,
                        **entry,
                    }
                )

        role_obj: JsonObject = {
            **values,
            "baseDamageStats": self._compute_stats(base_damage_by_interaction),
            "baseDamageByInteraction": base_damage_by_interaction,
        }

        if self.config.include_raw_list:
            role_obj["BaseDamage"] = raw_base_damage_list

        return role_obj

    def _resolve_sources(self) -> tuple[list[JsonSource], str, JsonObject]:
        folder_sources = list(self._iter_role_json_sources_from_folder())
        zip_candidates = sorted(
            p for p in self.base_dir.iterdir() if p.is_file() and p.suffix.lower() == ".zip"
        )
        zip_names = [zip_path.name for zip_path in zip_candidates]

        merged_sources: dict[str, bytes] = {}
        for rel_path, raw in folder_sources:
            merged_sources[rel_path] = raw
        for zip_path in zip_candidates:
            for rel_path, raw in self._iter_role_json_sources_from_zip(zip_path):
                merged_sources[rel_path] = raw

        sources = sorted(merged_sources.items(), key=lambda item: item[0])
        has_folder = bool(folder_sources)
        has_zip = bool(zip_candidates)
        if has_folder and has_zip:
            mode = "mixed"
        elif has_folder:
            mode = "folder"
        elif has_zip:
            mode = "zip"
        else:
            mode = "none"

        source_meta: JsonObject = {"zips": zip_names}
        return sources, mode, source_meta

    def _iter_role_json_sources_from_folder(self) -> Iterable[JsonSource]:
        """Yield role JSON files from the unpacked server folder layout."""
        roles_dir = self.base_dir / "Server" / "NPC" / "Roles"
        if not roles_dir.exists():
            return

        for json_file in sorted(roles_dir.rglob("*.json")):
            if json_file.is_file():
                rel = json_file.relative_to(self.base_dir).as_posix()
                yield rel, json_file.read_bytes()

    @staticmethod
    def _iter_role_json_sources_from_zip(zip_path: Path) -> Iterable[JsonSource]:
        """Yield role JSON files from a zip archive with server folder layout."""
        with zipfile.ZipFile(zip_path, "r") as zip_file:
            for info in sorted(zip_file.infolist(), key=lambda entry: entry.filename):
                if info.is_dir():
                    continue
                filename = info.filename.replace("\\", "/")
                if not filename.lower().endswith(".json"):
                    continue
                if not filename.startswith("Server/NPC/Roles/"):
                    continue
                yield filename, zip_file.read(info)

    @staticmethod
    def _parse_role_json_bytes(raw_bytes: bytes) -> JsonObject:
        """Parse role JSON bytes while accepting UTF-8 BOM."""
        text = raw_bytes.decode("utf-8-sig")
        return json.loads(text)

    @staticmethod
    def _find_first_key_value(obj: Any, key: str) -> Any | None:
        """Return first matching key value from nested dict/list structures."""
        if isinstance(obj, dict):
            if key in obj:
                return obj.get(key)
            for value in obj.values():
                hit = NpcRoleParser._find_first_key_value(value, key)
                if hit is not None:
                    return hit
            return None

        if isinstance(obj, list):
            for value in obj:
                hit = NpcRoleParser._find_first_key_value(value, key)
                if hit is not None:
                    return hit
            return None

        return None

    @staticmethod
    def _extract_interaction_var(json_path: str) -> str | None:
        """Extract interaction variable name from a JSON-style path string."""
        match = re.search(r"_InteractionVars\.([^.]+)\.", json_path)
        return match.group(1) if match else None

    @staticmethod
    def _collect_damage_calculators(obj: Any, out: list[JsonObject], path: str = "$") -> None:
        """Collect all damage calculators that have a non-empty BaseDamage object."""
        if isinstance(obj, dict):
            damage_calculator = obj.get("DamageCalculator")
            if isinstance(damage_calculator, dict):
                base_damage = damage_calculator.get("BaseDamage")
                if isinstance(base_damage, dict) and base_damage:
                    out.append(
                        {
                            "json_path": f"{path}.DamageCalculator.BaseDamage",
                            "Type": damage_calculator.get("Type"),
                            "RandomPercentageModifier": damage_calculator.get("RandomPercentageModifier"),
                            "BaseDamage": dict(base_damage),
                        }
                    )
            for key, value in obj.items():
                NpcRoleParser._collect_damage_calculators(value, out, f"{path}.{key}")
            return

        if isinstance(obj, list):
            for index, value in enumerate(obj):
                NpcRoleParser._collect_damage_calculators(value, out, f"{path}[{index}]")

    @staticmethod
    def _make_unique_interaction_key(
        interaction_var: str,
        base_damage_by_interaction: dict[str, JsonObject],
    ) -> str:
        if interaction_var not in base_damage_by_interaction:
            return interaction_var

        index = 2
        while f"{interaction_var}#{index}" in base_damage_by_interaction:
            index += 1
        return f"{interaction_var}#{index}"

    @staticmethod
    def _compute_stats(damage_by_interaction: dict[str, JsonObject]) -> dict[str, dict[str, float]]:
        """Compute min/max/mean per damage type across all interactions."""
        collector: dict[str, list[float]] = {}

        for entry in damage_by_interaction.values():
            damage_values = entry.get("Damage")
            if not isinstance(damage_values, dict):
                continue
            for damage_type, value in damage_values.items():
                try:
                    collector.setdefault(str(damage_type), []).append(float(value))
                except (TypeError, ValueError):
                    continue

        stats: dict[str, dict[str, float]] = {}
        for damage_type, values in collector.items():
            if not values:
                continue
            stats[damage_type] = {
                "min": float(min(values)),
                "max": float(max(values)),
                "mean": float(sum(values) / len(values)),
            }

        return stats


def main() -> None:
    """Generate `text.json` from NPC role files in folder or zip mode."""
    base_dir = Path(__file__).resolve().parent
    parser = NpcRoleParser(base_dir=base_dir, defaults=Defaults(), config=ParserConfig())
    parser.run()


if __name__ == "__main__":
    main()
