# Moonpaw

Werewolf in Minecraft! Play with your friends and find the werewolves.

Moonpaw brings the party game Werewolf to Minecraft as a game mode, without any cards. It is in early development. So far it only adds a Discord Rich Presence, the game itself comes next.

## Development

Moonpaw runs on Fabric and NeoForge for Minecraft 26.3 and needs a JDK 25 or newer. The JDK used by Gradle is set in `gradle.properties` (`org.gradle.java.home`), so change it if yours is somewhere else.

| Folder | Content |
| --- | --- |
| `common` | The whole mod, vanilla Minecraft code only |
| `fabric` | Fabric entrypoints and `fabric.mod.json` |
| `neoforge` | NeoForge entrypoints and `neoforge.mods.toml` |
| `buildSrc` | Gradle setup shared by the projects above |

| Command | What it does |
| --- | --- |
| `./gradlew build` | Runs the tests and builds the jars into `fabric/build/libs` and `neoforge/build/libs` |
| `./gradlew :fabric:runClient` | Starts the game with Fabric (`:fabric:runServer` for a server) |
| `./gradlew :neoforge:runClient` | Starts the game with NeoForge (`:neoforge:runServer` for a server) |

## Discord Rich Presence

The presence uses a Discord application from the [Discord Developer Portal](https://discord.com/developers/applications). Discord shows the name of the application as the game name. Its ID is `APPLICATION_ID` in `common/src/main/java/cc/avie/moonpaw/client/presence/MoonpawPresence.java`, and the presence stays off while that is empty.

The image comes from the application too: upload it under `Rich Presence → Art Assets` with the name `logo`.

## License

This mod is under the [Avie Terms & Conditions 1.0](https://raw.githubusercontent.com/avie293/.github/refs/heads/main/LICENSE).
