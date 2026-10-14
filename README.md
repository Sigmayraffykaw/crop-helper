# Crop Helper 🌾

A Fabric helper for Minecraft Java Edition **26.3**. Most features run client-side; craft and carry a Crop Helper Core to unlock them. Because the Core is a custom item and recipe, multiplayer servers must also install Crop Helper.

## Unlocking Crop Helper

Craft a **Crop Helper Core** in a crafting table using this recipe:

```text
Iron Ingot | Wheat       | Iron Ingot
Wheat      | Iron Hoe    | Wheat
(empty)    | Redstone    | (empty)
```

Keep the Core anywhere in your inventory to use the mod's features. If you remove it, farming pauses and an active schematic build stops. `/farm stop` remains available without the Core.

## Features

- `/farm scan` — count mature and growing supported crops near the player.
- `/farm start` and `/farm stop` — start or pause first-pass nearby harvest/replant automation for wheat, carrots, potatoes and beetroot.
- `/farm status` — show crop automation state.
- `/farm materials` — summarise nearby loaded containers.
- `/farm build small_wheat` — preview a schematic and compare required blocks with inventory and nearby chests.
- `/farm build confirm` — start placing the previewed plan.
- `/farm pull` — while a supply chest/container GUI is open, quick-move matching material stacks needed for the last previewed plan into your inventory.

## Schematic formats

Crop Helper supports its own JSON block-plan format and a basic subset of **Sponge `.schem`** files (palette-based v2/v3-style files).

- Put custom files in `.minecraft/config/crop-helper/schematics/`.
- Use `/farm build my_plot` for `my_plot.json` or `my_plot.schem`.
- A JSON plan contains a `blocks` array with `x`, `y`, `z`, and `block` fields.
- Sponge schematics are centered on X/Z and placed with their bottom at the anchor height.
- Current safety limits: at most 512 non-air blocks and offsets within 32 blocks.
- Sponge palette state properties (for example stair direction) are not applied yet; the block is placed using its default state. Litematica `.litematic` files are not supported yet.

The included `small_wheat` plan is a 5×5 dirt-and-fence foundation. Add water, hoe the dirt and plant crops yourself.

## Typical use

1. Copy a `.schem` or `.json` file into `.minecraft/config/crop-helper/schematics/`.
2. Stand in a clear area and run `/farm build my_plot` to preview the plan and materials.
3. Open the supply chest and run `/farm pull` to quick-move matching stacks into your inventory.
4. Make sure all required blocks are in your inventory and the block types you need are in your hotbar.
5. Run `/farm build confirm`. Use `/farm stop` to cancel.

**Important:** `/farm pull` only works while a container screen is open and transfers matching stacks using normal quick-move inventory actions. It does not reach into closed chests. The builder skips occupied positions instead of replacing solid blocks, but always test in a backup world first. Client automation may be restricted by multiplayer server rules or anti-cheat.

## Development setup

- Minecraft Java Edition 26.3
- Fabric Loader 0.19.5
- Java 25
- Fabric Loom 1.18-SNAPSHOT
- Fabric API 0.162.0+26.3

Open the repository as a Gradle project in IntelliJ IDEA or VS Code with Java extensions. Run `gradle build` with Java 25. A successful build creates the mod JAR in `build/libs/`.

The project is still under development. A successful CI build confirms compilation, not that every schematic or in-game interaction has been tested.
