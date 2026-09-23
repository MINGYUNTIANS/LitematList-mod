# LitematList (Minecraft 1.21 / 1.21.1)

Litematica 的专用材料清单助手模组，支持读取、汇总和展示多个原理图的材料信息。本工程对应游戏版本 **1.21 / 1.21.1**。

## 功能

- **材料清单管理**：在一个窗口中同时查看多个原理图的材料列表
- **多格式导入**：支持 Litematica 导出的 `.txt`（ASCII 表格）和 `.json`（投影导出）材料列表
- **从投影同步**：与 Litematica 联动，一键同步已加载原理图的材料清单
- **原料分解**：将成品材料回溯解析为可直接获取的原料
- **同步投影渲染层**：从投影同步时按 Litematica 当前渲染层规则统计方块
- **装甲架装备**：统计盔甲架穿戴/手持的物品（含纹饰模板与镶嵌材质）
- **材料统计**：显示物品图标+名称、总计、已有（背包中）、缺失数量
- **量级换算**：鼠标悬停时显示数量换算（如 328 = 5x64 + 8，0.19 潜影盒）
- **忽略/替换**：可忽略不需要的材料，或替换为其他物品
- **标记材料**：支持置顶标记材料，优先显示
- **隐藏不缺材料**：一键隐藏缺失数量为 0 的物品
- **原理图预览**：安装 SchematicPreview 模组后，可预览原理图 3D 全貌
- **共享原理图集成**：可同步「共享原理图增强」(Syncmatica Revolution) 的已认领材料
- **配置系统**：通过 MaLib 的多模组配置菜单进行设置

## 依赖

- Minecraft 1.21 / 1.21.1
- Fabric Loader
- Fabric API
- [MaLiLib](https://github.com/sakura-ryoko/malilib) (Sakura-Ryoko fork)
- [Litematica](https://github.com/sakura-ryoko/litematica)
- (可选) [SchematicPreview](https://github.com/sakura-ryoko/schematicpreview) — 原理图 3D 预览
- (可选) [Mod Menu](https://github.com/TerraformersMC/ModMenu) — 模组菜单集成

## 快捷键

| 快捷键 | 功能 |
|--------|------|
| L + C | 打开材料列表主界面 |

## 构建

```bash
./gradlew build
```

构建产物位于 `build/libs/`。

## 协议

MIT License - 详见 [LICENSE](LICENSE)