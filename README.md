# AliasCreator

A Minecraft server plugin for creating **command aliases** — both simple
one-to-one shortcuts and templated aliases with arguments and placeholders.
Built on [MagicUtils](https://github.com/THEROER/MagicUtils), so it runs on
**Bukkit/Paper** and **Fabric** from a shared core, with **Folia** support.

## Features

- **Simple aliases** — map one or more shortcuts to a target command, e.g.
  `hub` / `lobby` → `/server hub`.
- **Template aliases** — aliases that take arguments and expand placeholders
  into a full command, e.g. `agive {item} {count}` →
  `minecraft:give @s minecraft:totem_of_undying[item_model="{item}"] {count}`.
- **Per-alias permissions** — gate any alias behind a permission node.
- **Live reload** — with `realtime: true`, edits to the config apply instantly,
  no restart required.
- **Tab completion** — suggestions for alias names and target commands.

## Requirements

- A Bukkit/Paper (API 1.21+) or Fabric server.
- [MagicUtils](https://github.com/THEROER/MagicUtils) (required, loads first).

## Commands

Base command: `/aliascreator` (aliases: `/alias`, `/ac`).

| Subcommand | Description |
| --- | --- |
| `set <alias> <target…>` | Create or update an alias (alias: `create`). |
| `remove <alias>` | Remove an alias (alias: `delete`). |
| `perm <alias> [permission]` | Set or clear the permission for an alias. |
| `list` | List configured aliases. |
| `reload` | Reload the configuration. |

## Configuration

`plugins/AliasCreator/aliascreator.yml`:

```yaml
# Apply alias changes in real time when the config changes
realtime: true
# Always show namespaced commands in target suggestions
target-namespaced: false

# Simple aliases (target command, aliases list, permission)
aliases:
  - target: "/spawn"
    aliases: ["spawn"]
    permission: "aliascreator.alias.spawn"
  - target: "/server hub"
    aliases: ["hub", "lobby"]
    permission: "aliascreator.alias.hub"

# Template aliases with arguments and placeholder replacements
template-aliases:
  - alias: "agive"
    target: 'minecraft:give @s minecraft:totem_of_undying[item_model="{item}"] {count}'
    permission: "aliascreator.alias.agive"
    args:
      - item
      - count
```

## Building

```bash
./gradlew build
```

Platform jars are produced under each module's `build/libs/`.

## License

Licensed under the [Mozilla Public License 2.0](LICENSE).
