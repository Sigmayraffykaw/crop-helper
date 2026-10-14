# Crop Helper 🌾

A Fabric mod for Minecraft Java Edition **26.3**.

## Planned features

- **Crop helper:** find mature wheat, carrots, potatoes and beetroot nearby.
- **Harvest + replant:** safely harvest supported crops and replant from available seeds/items.
- **Chest supplies:** check a nearby supply chest and show what materials are missing.
- **Schematic farm builder:** preview a supported schematic, list required blocks, then build only after the player confirms.

## Current status

This repository is the project starter. The automation features are **not implemented yet**, and no installable JAR has been built or tested yet. We'll add them in small steps and test each build before calling it ready.

## Development setup

- Minecraft Java Edition 26.3
- Fabric Loader 0.19.5
- Java 25
- Fabric Loom 1.18-SNAPSHOT
- Fabric API 0.162.0+26.3

Open this folder as a Gradle project in IntelliJ IDEA or VS Code with Java extensions. Once Gradle is installed, run:

```powershell
gradle build
```

The JAR will be created in `build/libs/` after a successful build.

## Safety and compatibility

Use automation only in worlds/servers where it is allowed. Server rules and anti-cheat systems may block automated interactions. Back up a world before testing building features.

## Planned commands

These are design ideas, **not available commands yet**:

- `/farm status`
- `/farm scan`
- `/farm start`
- `/farm stop`
- `/farm build <schematic>`
