# LitematList

在游戏内集中查看多个 litematic 投影的材料清单，支持材料树溯源、原材料替换、HUD 显示与全屏原理图预览。

本工程基于 [Fallen-Breath/fabric-mod-template](https://github.com/Fallen-Breath/fabric-mod-template) 的多版本结构，用一个 Gradle 工程同时构建多个 Minecraft 版本。

## 支持版本

| 子项目 | 编译 MC 版本 | 运行范围 | Java |
| --- | --- | --- | --- |
| 1.17.1 | 1.17.1 | 1.17.x | 16 |
| 1.19.4 | 1.19.4 | 1.19.x | 17 |
| 1.20.1 | 1.20.1 | 1.20.1 | 17 |
| 1.21.1 | 1.21 | >=1.21 <=1.21.1 | 21 |
| 1.21.4 | 1.21.4 | ~1.21.4 | 21 |
| 1.21.8 | 1.21.8 | ~1.21.8 | 21 |
| 1.21.10 | 1.21.10 | >=1.21.9 <=1.21.10 | 21 |
| 1.21.11 | 1.21.11 | ~1.21.11 | 21 |
| 26.1.2 | 26.1.2 | ~26.1.2 | 25 |
| 26.2 | 26.2 | ~26.2 | 25 |
| 26.3 | 26.3 | ~26.3 | 25 |

依赖：Fabric API、MaLib、Litematica；Mod Menu 仅编译期使用；pinyin4j 打包进模组 jar。

## 工程结构

```
src/main/java                         统一核心源码（core = 1.21.10）
versions/<版本>/src/main/java         版本级源码覆盖（仅 26.x 使用）
versions/<版本>/src/main/resources    版本级资源覆盖（lang、accesswidener、fabric.mod.json）
versions/mainProject                  核心版本名（1.21.10）
settings.json                         参与构建的版本列表
build.gradle                          预处理器版本节点链 + 产物汇总任务
common.gradle                         各版本共用的构建逻辑
```

- **1.17.1 ~ 1.21.11**：共用 `src/main/java`。版本差异用预处理器指令表达：
  - `//#if MC == 12110` / `//#elseif` / `//#endif` 声明条件；
  - 非核心版本的备选行加 `//$$` 前缀，核心版本的代码保持正常缩进。
  - 核心版本（1.21.10）的源码本身必须能直接编译，因此非核心分支必须全部带 `//$$`。
- **26.x**：Minecraft 26 起为未混淆版本，类名与 Yarn 完全不同（`Minecraft` / `Level` / `ResourceLocation`），因此使用 `versions/26.x/src/main/java` 独立覆盖。26.2、26.3 只保留与上游不同的文件，其余文件自动继承；26.2 中被移除的类用空 `.java` 文件占位。
- 每个版本的 `gradle.properties` 定义该版本使用的 Minecraft/Yarn/loader/依赖版本。

## 构建

需要 JDK 25（26.x 使用 `--release 25`，低版本在同一 JDK 上以 `--release 8/16/17/21` 编译）：

```bat
set JAVA_HOME=D:\Java\java25
gradlew.bat buildAndGather
```

单版本构建：

```bat
gradlew.bat :1.21.10:build
```

产物：`build/libs/litematlist-v1.7.7-mc<MC版本>.jar`，以及各子项目的 `versions/<版本>/build/libs/`。

## 修改指南

- **公共逻辑**：直接改 `src/main/java`，所有 Yarn 版本同步生效。
- **仅某一版本**：在共用代码中用 `//#if MC == <版本号>` 包裹；需要整文件替换时，把文件放进 `versions/<版本>/src/main/java/...`（核心版本 1.21.10 除外）。
- **26.x**：改对应 `versions/26.x/src/main/java`；若 26.2/26.3 与上游行为相同，删除该文件即可自动继承。
- **新增 MC 版本**：在 `settings.json` 与 `build.gradle` 中增加版本节点，创建 `versions/<版本>/gradle.properties`；若与相邻版本差异大，按 26.x 的方式放覆盖源码。

## 备注

- `masa.dy.fi` / `sakura-ryoko` 仓库可能偶发不可达，Gradle 会优先使用本地缓存。
- 1.19.4 / 1.20.1 的少量中文注释在独立版本中原本就是乱码，合并时原样保留，不影响功能。
- 多版本结构参考自 fabric-mod-template（模板脚手架部分遵循其原许可证），本次合并后的模组代码遵循 MIT。
