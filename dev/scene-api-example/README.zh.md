# 场景接口示例

[English](README.md)

一个很小的客户端 mod：装了 GTL Additions 时，它通过 GTL Additions 的场景接口（`com.gtladd.gtladditions.api.client.scene`，版本 1）绘制；没装时自己绘制。它不属于 GTL Additions 的 JAR。

接口本身的说明见 [API.zh.md](API.zh.md)。

## 构建

```
./gradlew remapSceneApiExampleJar
```

成品写到 `build/libs/gtladditions-scene-api-example-<version>.jar`。`./gradlew build` 只编译它，所以破坏接口调用方的改动会让构建失败。

## 它演示了什么

| 文件 | 演示 |
| --- | --- |
| `JavaOrbs.java` | Java 提供者，每帧主动提交部件；用 lambda 写的 `PartRenderer`；带 `SCENE_DEPTH` 的 `PREMULTIPLIED` 半透明部件；生命周期通知 |
| `KotlinBeacons.kt` | Kotlin 提供者，经 `SceneProvider.collect` 被动提交；带 `OWN_HAND_MASK` 的 `OPAQUE` 部件；`ADDITIVE` 部件；`lastOutcome` |
| `orb_scene.fsh` | `#moj_import <gtladditions:scene_depth.glsl>` 与场景像素的约定 |
| `beacon_core.fsh` | 手部遮罩：先求出材质，再丢弃 |
| `ExampleClient.java`、`SceneBridge.java`、`DirectOrbs.java` | 可选依赖：哪些代码不能出现接口的类型 |

## 游戏内

```
/scene_example add orb <x> <y> <z>
/scene_example add beacon <x> <y> <z>
/scene_example mode scene|direct
/scene_example status
/scene_example clear
```

`mode` 只切换球的绘制方式。`direct` 模式下它们在调度器之外绘制，就像一个不使用这套接口的 mod 的内容。信标需要 GTL Additions。放置的内容属于当前世界，世界卸载时一并清除。

## 可选依赖

没有 GTL Additions 时要满足三件事，哪一件不满足都会以各自的方式出错：

1. 被加载的类都不能出现接口的类型。`ExampleClient` 先查 `ModList`，确认之后才调用 `SceneBridge`，它是第一个导入接口的类。Kotlin 提供者也是这样进入的，所以同样不需要 Kotlin 运行时。
2. 被加载的着色器程序都不能导入 GTL Additions 的着色器文件。`orb_scene` 导入了 `scene_depth.glsl`，由 `JavaOrbs` 注册；直接绘制的路径用自己的程序 `orb_direct`。
3. 不注册任何会被自动加载的东西：出现接口类型的类上不加 `@Mod.EventBusSubscriber`。

## 示例遵守的规则

- 提供者只注册一次，并且在渲染线程上：`FMLClientSetupEvent.enqueueWork`。
- 每帧提交一次部件，提交了就不要再直接绘制。`ACCEPTED` 表示部件进入了这一帧；结果要等这一帧结束后经 `lastOutcome` 取得。
- 不保留任何属于某一帧的东西：`SceneFrame`、绘制上下文、场景深度和手部遮罩贴图都不能活过那一帧。
- 缺着色器时报告 `RESOURCE_UNAVAILABLE`。调度器不捕获异常，从渲染器里抛出的异常会使客户端崩溃。
- 着色器程序的混合状态与声明的 `PartBlend` 一致。
- 基线没有覆盖的状态要自己恢复：pose stack 成对，顶点缓冲在 `finally` 里解绑。

## 版本

`mods.toml` 里的 `versionRange` 写的是示例接受的最早的 GTL Additions 版本。接口版本 1 是在 `3.3.1Custom` 之后加入的，当时还没有新的 mod 版本号，所以这个范围目前仍会接受该版本的旧构建；发布时应改成第一个带有这套接口的版本。
