# LitematList MOD

A material list assistant built specifically for players
(The code is in other branches)

## Features

- **Material List Management**: Manage material lists for multiple schematics in a single window
- **Multi-format Import**: Supports `.txt`, `.json`, and `.csv` files exported from Litematica
- **Sync from Projection**: Integrates with Litematica to sync material lists of loaded schematics
- **Tree View**: Display the raw materials required for producing a finished item in a tree structure
- **Sync Projection Render Layers**: Count blocks according to Litematica's current render-layer rules when syncing from a projection
- **Armor Stand Equipment**: Count items worn on armor stands (including armor trim templates and inlay materials)
- **Material Statistics**: Show item icon + name, total count, on-hand count, and missing count
- **Edit List**: Players can ignore or replace items in the list
- **Quantity Conversion**: Show quantity conversions on hover (e.g., 328 = 5x64 + 8, 0.19 shulker boxes)
- **Ignore/Replace**: Ignore unneeded materials or replace them with other items
- **Mark Materials**: Support pinning marked materials to display them first
- **Hide Non-Missing Materials**: Hide items with a missing count of 0 in the processing list
- **Schematic Preview**: Preview the full 3D view of a schematic directly in the main interface after installing the SchematicPreview mod
- **Shared Schematic Reading**: Sync claimed materials from "Shared Schematic Enhancements" (Syncmatica Revolution)
- **Config System**: Configure through the multi-mod configuration menu provided by MaLib

## Dependencies

- Fabric Loader
- Fabric API
- [MaLiLib](https://github.com/sakura-ryoko/malilib) (Sakura-Ryoko fork)
- [Litematica](https://github.com/sakura-ryoko/litematica)
- (Optional) [SchematicPreview](https://github.com/sakura-ryoko/schematicpreview) — 3D schematic preview
- (Optional) [Mod Menu](https://github.com/TerraformersMC/ModMenu) — mod menu integration

## Key Bindings

| Key Binding | Function |
|-------------|----------|
| L + C | Open the main material list interface |
| V + C | Open the mod configuration interface |

## Building

```bash
./gradlew build
```

The build output is located in `build/libs/`.

## License

MIT License - See [LICENSE](LICENSE)