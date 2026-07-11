# CommandFlow

Custom commands for Minecraft networks: build commands from actions and
conditions, gate them with cooldowns and costs, and route them across servers
from your proxy. Powered by [MagicUtils](https://maven.theroer.dev).

Supports Paper/Spigot/Folia, Fabric, Velocity and BungeeCord/Waterfall across
Minecraft 1.21.6 to 26.2.

## What it does

- **Aliases** map a short name to a target command (`spawn` runs `/spawn`), with
  arguments, permissions and template placeholders.
- **Action sequences** turn one command into a series of steps: run commands (as
  the player, the console or op), send messages, broadcast, play sounds, show
  titles and action bars, and wait between steps.
- **Conditions** gate a command: permission, world, argument value, placeholder
  comparison, and economy cost (Vault on Bukkit).
- **Cooldowns** per player, per command, with a bypass permission.
- **Placeholders** anywhere in text and commands, resolved through MagicUtils
  (including PlaceholderAPI where installed).
- **Cross-server** execution from the proxy: forward a command to a player's
  backend, or query a placeholder from another server and continue the sequence
  with the result.
- **Dry-run debugging**: `/commandflow debug <alias>` shows a step-by-step trace
  of what would happen, without any side effects.

## Configuration

Aliases and template commands live in `commandflow.yml`.

### Simple alias

```yaml
aliases:
  - target: "/spawn"
    aliases: ["spawn"]
    permission: "commandflow.alias.spawn"
```

### Action sequence with conditions and cooldown

```yaml
template-aliases:
  - alias: kit
    permission: commandflow.kit
    cooldown-seconds: 60
    cooldown-message: "<red>Wait {cooldown-remaining}s before using /kit again."
    conditions:
      - type: WORLD
        value: survival
      - type: PLACEHOLDER_COMPARE
        left: "%player_level%"
        operator: ">="        # or GREATER_EQUAL
        right: "10"
      - type: COST
        amount: 100.0          # charged through Vault; skipped on Fabric
    actions:
      - type: RUN
        as: console
        command: "give {sender} diamond 5"
      - type: MESSAGE
        text: "<green>Kit received!"
      - type: SOUND
        sound: entity.player.levelup
```

Action types: `RUN`, `MESSAGE`, `BROADCAST`, `DELAY`, `SOUND`, `TITLE`,
`ACTIONBAR`, `QUERY`. Condition types: `PERMISSION`, `WORLD`, `ARG_EQUALS`,
`PLACEHOLDER_COMPARE`, `COST`.

## Cross-server

Cross-server routing uses the MagicUtils message bus. On Bukkit it works over the
proxy's plugin-messaging channel out of the box; on Fabric it requires Redis
(mods cannot use the vanilla plugin-messaging channel). Enable Redis in
`messaging.yml` for delivery that reaches other servers.

An action's `server` field decides where it runs: empty means this server, a
backend name targets that server, `*` targets every backend, and `@player`
targets the backend hosting the invoking player.

### Forward a command to another server

```yaml
template-aliases:
  - alias: lobby-broadcast
    permission: commandflow.admin
    actions:
      - type: RUN
        as: console
        server: lobby
        command: "say Server restart in 5 minutes"
```

### Query a value from another server and branch on it (pipeline)

```yaml
template-aliases:
  - alias: buyvip
    permission: commandflow.vip
    actions:
      - type: QUERY
        server: economy
        placeholder: "%vault_eco_balance%"
        into: balance          # result stored as {balance}
        timeout-ticks: 40      # stop the pipeline if no reply in 2s
      - type: RUN
        as: console
        server: economy
        command: "eco take {sender} 1000"
      - type: MESSAGE
        text: "<green>Balance was {balance}, VIP purchased!"
```

Install the proxy module (`commandflow-velocity` or `commandflow-bungee`) on your
proxy so aliases typed there are forwarded to the player's backend, which runs
the sequence.

## Commands

`/commandflow` (aliases: `/cf`, `/alias`, `/ac`)

- `list` - list configured aliases
- `set <alias> <target>` - create or update an alias
- `perm <alias> <permission>` - set an alias permission
- `remove <alias>` - remove an alias
- `reload` - reload the configuration
- `debug <alias> [args]` - dry-run an alias and print a step-by-step trace

## Modules

- `commandflow-bukkit` - Paper/Spigot/Folia plugin (backend)
- `commandflow-fabric` - Fabric mod (backend)
- `commandflow-velocity` - Velocity proxy module (forwarder)
- `commandflow-bungee` - BungeeCord/Waterfall proxy module (forwarder)

MagicUtils must be available at runtime (as a separate plugin/mod, or shaded per
the build configuration).

## Building

```
./gradlew build -Pscenario=workspace
```

Per-platform: `-Pscenario=bukkit|fabric|velocity|bungee`.

## License

Licensed under the [Mozilla Public License 2.0](LICENSE).
