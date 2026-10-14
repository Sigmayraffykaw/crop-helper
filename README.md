# Crop Helper 🌾

A client-side Fabric helper for Minecraft Java Edition **26.3**.

## Included in this first implementation

- `/farm scan` — count mature and growing supported crops within 4 blocks of the player.
- `/farm start` — enable nearby crop harvesting/replanting attempts.
- `/farm stop` — pause automation.
- `/farm status` — show whether automation is running.
- The crop loop handles mature wheat, carrots, potatoes and beetroot, and only attempts to harvest when matching replant items are in the hotbar.

## Still in progress

- Reading chest inventories and calculating a schematic's exact material requirements.
- Importing schematic files, previewing the build, and placing blocks from a confirmed plan.
- End-to-end build and in-game testing. The current automation is a first pass and may need fixes for Minecraft 26.3 mappings or particular servers.

## Development setup

- Minecraft Java Edition 26.3
- Fabric Loader 0.19.5
- Java 25
- Fabric Loom 1.18-SNAPSHOT
- Fabric API 0.162.0+26.3

Open this repository as a Gradle project in IntelliJ IDEA or VS Code with Java extensions. Run `gradle build` after installing Java 25 and Gradle. A successful build creates the mod JAR in `build/libs/`.

## Commands

These are client commands; use them in chat in a world with the mod installed:

- `/farm status`
- `/farm scan`
- `/farm start`
- `/farm stop`

The `/farm build <schematic>` command is currently a placeholder and does not place blocks yet.

## Safety and compatibility

Automation interacts with the world like a player and can be blocked by server rules or anti-cheat. Test in a backup copy of a single-player world first.
