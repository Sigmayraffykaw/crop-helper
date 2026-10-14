# Crop Helper 🌾

A client-side Fabric helper for Minecraft Java Edition **26.3**.

## Features in this starter implementation

- `/farm scan` — count mature and growing supported crops within 4 blocks of the player.
- `/farm start` and `/farm stop` — start or pause first-pass nearby harvest/replant automation for wheat, carrots, potatoes and beetroot.
- `/farm status` — show whether crop automation is running.
- `/farm materials` — summarise nearby loaded containers.
- `/farm build small_wheat` — preview a schematic and compare its block requirements against your inventory and nearby chests.
- `/farm build confirm` — begin placing the previewed schematic, one block at a time. Use `/farm stop` to cancel.

## Schematic support

This first builder version uses Crop Helper's **JSON block-plan format**, not Sponge `.schem` or Litematica `.litematic` files yet.

A sample `small_wheat` plot is included. It places a 5x5 dirt/fence foundation; you still need to add water, hoe the dirt, and plant crops yourself.

To add a custom plan, create:

` .minecraft/config/crop-helper/schematics/my_plot.json `

Example format:

```json
{
  "name": "My plot",
  "blocks": [
    { "x": 0, "y": 0, "z": 0, "block": "minecraft:dirt" },
    { "x": 1, "y": 0, "z": 0, "block": "minecraft:oak_fence" }
  ]
}
```

Coordinates are relative to a point three blocks in front of the player. Keep each offset within 32 blocks and each plan at 512 blocks or fewer. Use ordinary placeable blocks with item forms. The builder only places into air or replaceable positions; it skips occupied positions rather than replacing existing builds. The player must have all required materials in their inventory and the needed block types in the hotbar. Take materials out of the chest first; automatic chest extraction is not implemented yet.

## Development setup

- Minecraft Java Edition 26.3
- Fabric Loader 0.19.5
- Java 25
- Fabric Loom 1.18-SNAPSHOT
- Fabric API 0.162.0+26.3

Open this repository as a Gradle project in IntelliJ IDEA or VS Code with Java extensions. Run `gradle build` after installing Java 25 and Gradle. A successful build creates the mod JAR in `build/libs/`.

## Important limits

- The project is still being built and tested. Do not assume a working installable JAR until GitHub Actions reports a successful build.
- Standard Sponge and Litematica schematic formats are not supported yet.
- The builder does not automatically pull items out of chests, place water with buckets, hoe soil, or plant crops.
- Automation can be blocked by server rules or anti-cheat. Test in a backup single-player world first.
