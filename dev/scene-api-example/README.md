# Scene API example

[中文](README.zh.md)

A small client mod that draws through the GTL Additions scene API (`com.gtladd.gtladditions.api.client.scene`, version 1)
when GTL Additions is installed, and draws by itself when it is not. It is not part of the GTL Additions JAR.

The API itself is described in [API.md](API.md).

## Build

```
./gradlew remapSceneApiExampleJar
```

The mod is written to `build/libs/gtladditions-scene-api-example-<version>.jar`. `./gradlew build` only compiles it, so a
change that breaks the API for its callers fails the build.

## What it shows

| File | Shows |
| --- | --- |
| `JavaOrbs.java` | A Java provider that pushes parts each frame, a `PartRenderer` lambda, a translucent `PREMULTIPLIED` part with `SCENE_DEPTH`, the lifecycle notifications |
| `KotlinBeacons.kt` | A Kotlin provider pulled through `SceneProvider.collect`, an `OPAQUE` part with `OWN_HAND_MASK`, an `ADDITIVE` part, `lastOutcome` |
| `orb_scene.fsh` | `#moj_import <gtladditions:scene_depth.glsl>` and the scene-pixel convention |
| `beacon_core.fsh` | The hand mask: evaluate the material, then discard |
| `ExampleClient.java`, `SceneBridge.java`, `DirectOrbs.java` | The optional dependency: what must stay free of API types |

## In game

```
/scene_example add orb <x> <y> <z>
/scene_example add beacon <x> <y> <z>
/scene_example mode scene|direct
/scene_example status
/scene_example clear
```

`mode` switches the orbs only. In `direct` mode they are drawn outside the scheduler, as content of a mod that does not use
the API would be. Beacons need GTL Additions. Placements belong to the current level and are dropped when it unloads.

## The optional dependency

Without GTL Additions three things must hold, and each fails in its own way if it does not:

1. No class that loads names an API type. `ExampleClient` checks `ModList` and only then calls `SceneBridge`, the first class
   that imports the API. The Kotlin provider is reached the same way, so no Kotlin runtime is needed either.
2. No program that loads imports a GTL Additions shader file. `orb_scene` imports `scene_depth.glsl` and is registered by
   `JavaOrbs`; the direct path has its own program, `orb_direct`.
3. Nothing is registered for automatic loading: no `@Mod.EventBusSubscriber` on a class that names the API.

## Rules the example follows

- Register a provider once, on the render thread: `FMLClientSetupEvent.enqueueWork`.
- Submit a part once per frame, and never also draw it directly. `ACCEPTED` means the part entered the frame; the outcome
  arrives through `lastOutcome` after the frame has ended.
- Keep nothing of a frame: no `SceneFrame`, draw context, scene depth or hand mask texture outlives its frame.
- Report a missing shader as `RESOURCE_UNAVAILABLE`. The scheduler catches no exception; one that leaves a renderer crashes
  the client.
- A program's blend state equals the declared `PartBlend`.
- Restore what the baseline does not cover: balance the pose stack and unbind the vertex buffer in `finally`.

## Version

The `versionRange` in `mods.toml` names the oldest GTL Additions release the example accepts. API version 1 is included
starting in `3.6Custom`, so the example requires `[3.6Custom,)`.
