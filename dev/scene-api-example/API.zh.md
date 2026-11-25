# GTL Additions 场景接口（v1）

[English](API.md)

包 `com.gtladd.gtladditions.api.client.scene`，仅客户端。本目录的示例 mod 演示了主要的接入方式：主动提交部件的 Java 提供者，以及被动提交的 Kotlin 提供者；各文件演示了什么、怎样构建与试用，见 [README.zh.md](README.zh.md)。

1. [能得到什么](#能得到什么)
2. [准备](#准备)
3. [第一个特效](#第一个特效)
4. [通道与混合怎么选](#通道与混合怎么选)
5. [进一步](#进一步)：场景深度、手部、被动提交、重载、离屏工作
6. [参考](#参考)
7. [出问题时](#出问题时)

## 能得到什么

GTL Additions 的机器特效每帧都经由同一个调度器绘制。其他 mod 把自己的世界特效交给这个调度器，就得到同样的处理：

- 所有接入的 mod 的半透明特效放在一起，按包围体由远及近排序，而不是按事件的先后；
- 特效会与场景做遮挡判断，渲染器还可以读取场景深度来做淡出或裁剪；
- 光影（Oculus）下，特效画在光影的最终合成之后，这条世界管线的后处理不再改动它们；手部遮罩与模板程序就绪时，半透明部件不会盖住第一人称手部。

其他 mod 不经这套接口画的内容，不参与排序，也不归它管。

每个特效描述为一个“部件”（part）：它在哪里、怎样混合、由哪个渲染器来画。渲染器何时运行由调度器决定，绘制目标与渲染状态也由调度器事先设好。

用原版渲染类型绘制的普通模型不需要这套接口。它面向的是带自己着色器的特效：辉光、光束、外壳、体积。

## 准备

- 把 GTL Additions 的 JAR 作为仅编译依赖。
- 在 `mods.toml` 里声明 `gtladditions`，版本下限写带有这套接口的最早版本。想让自己的 mod 在没有 GTL Additions 时也能运行，就把依赖设为可选，并照 [README.zh.md](README.zh.md) 的“可选依赖”一节去做。
- 只在客户端代码里、只在渲染线程上使用接口。

## 第一个特效

在世界坐标处画一个半透明的球，用 Java。它就是示例里的 `JavaOrbs.java` 去掉场景深度的样子；场景深度在后面的[与世界相交处淡出或裁剪](#与世界相交处淡出或裁剪)里再加上。下面的片段只列出与接口有关的调用：事件的接线在 `SceneBridge.java`，着色器加载和动画用的 uniform 在 `JavaOrbs.java`，球体网格在 `Meshes.java`。`Orb` 是这里用的简称，对应示例里的 `Placements.Placement`。

**1. 注册一次。** 客户端初始化事件运行在工作线程上，所以把调用交给渲染线程。

```java
private static ProviderHandle handle;

static void onClientSetup(FMLClientSetupEvent event) {
    event.enqueueWork(() -> handle = SceneRendering.register(new ResourceLocation("mymod", "orbs")));
}
```

**2. 加载着色器。** 这一步与接口无关，只有一条规则：着色器 JSON 里的混合，就是之后为部件声明的混合。对应 `PartBlend.PREMULTIPLIED` 的写法：

```json
"blend": {"func": "add", "srcrgb": "one", "dstrgb": "1-srcalpha"}
```

示例里的 `orb_direct.json` 是这样一个不读场景深度的程序的完整文件。每种 `PartBlend` 对应的写法见[混合表](#部件)。

**3. 每帧提交部件**，在 `AFTER_ENTITIES` 阶段的 `RenderLevelStageEvent` 里，或在方块实体渲染器的 `render` 方法里。提交之后不要再自己画一遍。

```java
static void onStage(RenderLevelStageEvent event) {
    if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
    SceneFrame frame = SceneRendering.frame();
    if (frame == null) return;                       // 光影的阴影通道
    for (Orb orb : orbs) {
        frame.submit(ScenePart.builder(new PartKey(handle, orb.id(), 0), RENDERER, orb)
                .blend(PartBlend.PREMULTIPLIED)
                .bounds(PartBounds.sphere(orb.x(), orb.y(), orb.z(), orb.radius()))   // 世界坐标
                .batchKey(RENDERER)                  // 相邻的球在一次 draw 调用里送达
                .build());
    }
}
```

在方块实体渲染器里用 `PartKey.block(handle, pos, 0)`。第三个值是同一个实例里各部件的编号。

**4. 绘制。** 轮到这些部件时，调度器调用渲染器。目标、视口、深度测试和混合都已设好；pose stack 以相机为原点。

```java
static final PartRenderer<Orb> RENDERER = (context, parts) -> {
    ShaderInstance shader = orbShader;
    if (shader == null) {                            // 不要抛异常：报告后返回
        for (ScenePart<Orb> part : parts) context.report(part, PartOutcome.RESOURCE_UNAVAILABLE);
        return;
    }
    SceneFrame frame = context.getFrame();
    PoseStack pose = context.getPoseStack();
    VertexBuffer mesh = sphereMesh();
    mesh.bind();
    try {
        for (ScenePart<Orb> part : parts) {
            Orb orb = part.getPayload();
            float radius = (float) orb.radius();
            pose.pushPose();
            try {
                pose.translate(orb.x() - frame.getCameraX(), orb.y() - frame.getCameraY(), orb.z() - frame.getCameraZ());
                pose.scale(radius, radius, radius);  // 网格是单位球；包围体不会替你缩放它
                mesh.drawWithShader(pose.last().pose(), RenderSystem.getProjectionMatrix(), shader);
            } finally {
                pose.popPose();
            }
        }
    } finally {
        VertexBuffer.unbind();                       // 把状态还原成进来时的样子
    }
};
```

**5. 检查结果。** `SceneRendering.lastOutcome(key)` 给出某个部件在最近结束的那一帧里的结果：`DRAWN`、`CULLED`、`RESOURCE_UNAVAILABLE` 等。启动游戏时加上 `-Dgtladditions.scene.debug=true`，每个提供者的每种结果在这次运行里第一次出现时，日志会记一行。部件没有显示出来，就看[出问题时](#出问题时)。

## 通道与混合怎么选

| 特效 | 声明 | 着色器输出 |
| --- | --- | --- |
| 挡住后面东西的实心表面：核心、外壳、镂空的形状 | `pass(PartPass.OPAQUE)` | alpha 为 1；可以 `discard` |
| 只叠加亮度的光：辉光、光束、火花 | `blend(PartBlend.ADDITIVE)` | 要叠加的颜色 |
| 逐层向白色靠近但不会超过的提亮 | `blend(PartBlend.SCREEN)` | [0, 1] 的颜色 |
| 真正的半透明：烟、玻璃、外壳 | `blend(PartBlend.PREMULTIPLIED)` | 已乘过 alpha 的颜色 |
| 同上，但着色器输出的是未预乘的颜色 | `blend(PartBlend.STRAIGHT)` | 颜色与 alpha |

- 既有实心核心又有辉光的特效，拆成同一个实例、不同编号的两个部件；`KotlinBeacons.kt` 就是这样做的。
- 你提交的两个部件同为 `ADDITIVE`，或同为 `SCREEN` 时，它们之间不需要先后。其他半透明部件按包围体排序，排序的好坏取决于包围体：包围体重叠的地方只能是近似的。包围体要尽量贴合，一个盒子或球盖得太松的形状用 `PartBounds.union` 拼。
- 包围体用世界坐标，要盖住部件可能画到的全部范围，包括动画和闪光。

## 进一步

### 与世界相交处淡出或裁剪

声明 `PartFeature.SCENE_DEPTH`，在渲染器里绑定深度，在着色器里读取。这样就把第一个特效的程序变成了示例里的 `orb_scene`：

```java
.features(PartFeature.SCENE_DEPTH)                   // 构建器上
context.getSceneDepth().bind(shader);                // draw 里，绘制之前
```

```glsl
#moj_import <gtladditions:scene_depth.glsl>

vec2 scenePixel = gl_FragCoord.xy - SceneViewport;
float depth = sceneDepthAt(ivec2(scenePixel));       // 传入像素
vec2 uv = scenePixel / SceneSize;                    // 其余函数用归一化坐标
float scene = sceneRayDistance(uv, depth);
fragColor = colour * smoothstep(0.0, 0.4, scene - length(viewPosition));   // 与场景相交处淡出
```

`#moj_import` 一行留在文件作用域，因为它引入的是 uniform 和函数；它后面的几行放在 `main` 里，`colour` 代表你自己的材质。完整的着色器见 `orb_scene.fsh`。

声明要做两处，import 只解决第一处。GLSL 里声明采样器和 uniform；程序的 JSON 里也要把它们列出来，因为 `bind` 是经由 JSON 里的条目设置的，JSON 里没有的会被跳过：采样器 `SceneDepth`，以及 uniform `SceneSize`、`SceneViewport`（各两个 float）和 `SceneInverseProjection`（矩阵）。见 [orb_scene.json](src/main/resources/assets/gtladditions_scene_example/shaders/core/orb_scene.json)。

### 让第一人称手部保持在前面

光影下部件画在手部之后。手部遮罩与模板程序就绪时，半透明部件由调度器负责避开手部（见[手部遮罩](#手部遮罩)）；`OPAQUE` 部件要自己处理（`beacon_core.fsh`）：

```kotlin
.features(PartFeature.OWN_HAND_MASK)                 // 构建器上
val mask = context.handMaskTexture                   // 0：这次绘制没有要丢弃的像素
shader.setSampler("HandMask", mask)
shader.safeGetUniform("HandMaskEnabled").set(if (mask != 0) 1 else 0)
try {
    // 绘制这些部件
} finally {
    shader.safeGetUniform("HandMaskEnabled").set(0)  // 这个程序之后可能在没有遮罩时再被使用
}
```

```glsl
fragColor = material();                              // 先求出材质
if (HandMaskEnabled != 0 && texelFetch(HandMask, ivec2(gl_FragCoord.xy), 0).r > 0.5) discard;
```

这两行放在 `main` 里，`material()` 代表你自己的着色；完整的着色器见 `beacon_core.fsh`。

这里和 `KotlinBeacons.kt` 一样用的是 `ShaderInstance.setSampler`，它只登记贴图，真正的绑定发生在程序应用时。如果改为手动绑定纹理单元，要放在你自己的渲染类型或着色器设置之后，绑完恢复活动单元。用完把自己的开关 uniform 复位。只有画到调度器的目标时，`gl_FragCoord` 才直接对应遮罩；在尺寸或原点不同的自有目标里，要把坐标换算回全帧。半透明部件也可以声明 `OWN_HAND_MASK`，用于它画到自有目标里的内容。

### 被动提交

不想在事件里主动提交，可以实现 `SceneProvider.collect(frame)` 并注册这个提供者：`SceneRendering.register(id, provider)`。调度器每帧在提交关闭前调用它一次。`KotlinBeacons.kt` 用的是这种方式。

### 响应重载

同一个 `SceneProvider` 还可以覆盖 `onResourcesReloaded`、`onPipelineChanged`（光影被打开、关闭或重载）和 `onLevelChanged`（客户端世界被卸载），用来丢弃依赖它们的缓存。

### 离屏工作

覆盖 `PartRenderer.prepare`，在该通道绘制之前画到自己的帧缓冲里。它收到的是该渲染器在这个通道里的全部可见部件。在 `draw` 里用过自己的帧缓冲之后，调用 `context.bindTarget()`。

## 参考

### 一帧的过程

1. 帧从主视图的 `AFTER_SKY` 开始。在提交关闭之前，`SceneRendering.frame()` 返回这一帧；其他时候返回 null，包括光影的阴影通道和这段窗口之外。
2. 提交在 `AFTER_BLOCK_ENTITIES` 结束时关闭。关闭前，每个已注册的提供者会收到一次 `SceneProvider.collect(frame)`。
3. `OPAQUE` 部件按提交顺序画入主目标并写深度：没有光影时紧接在提交关闭之后，所以玻璃和水会叠在它们上面；有光影时在光影的最终合成之后。
4. `TRANSLUCENT` 部件按包围体由远及近绘制：没有光影时在雨雪之前（`CompositionPoint.BEFORE_WEATHER`），有光影时在光影的最终合成之后（`AFTER_SHADERPACK_FINAL`）。
5. 下一个渲染 tick 开始时这一帧结束。此后 `SceneRendering.lastOutcome(key)` 返回每个部件的结果。

一帧里的东西不能留到下一帧：`SceneFrame`、绘制上下文、场景深度和手部遮罩贴图都是如此。

### 部件

| 构建器调用 | 含义 |
| --- | --- |
| `builder(key, renderer, payload)` | `PartKey` 由提供者句柄、实例（`long`）和部件编号组成；`PartKey.block` 用方块坐标作实例。每帧每个键只能有一个部件。载荷在这一帧结束前不能改变。 |
| `pass(...)` | `TRANSLUCENT`（默认）或 `OPAQUE`。 |
| `blend(...)` | 半透明部件必须声明。 |
| `bounds(...)` | 必须声明。保守的世界坐标包围体：`box`、`sphere`，或若干凸块的 `union`。调度器按它剔除和排序。 |
| `features(...)` | 渲染器需要的功能，见下表。 |
| `batchKey(...)` | 只对半透明部件有效：同一渲染器、设置了键且键相等、在顺序中相邻的部件，会在一次 `draw` 调用里一起画；没有设置键的部件各占一次调用。键相等意味着混合、功能和绘制资源都相同。`OPAQUE` 部件总是一次调用画一个。 |

| `PartBlend` | 混合因子 | 着色器 JSON 的 `blend` |
| --- | --- | --- |
| `PREMULTIPLIED` | `ONE`、`ONE_MINUS_SRC_ALPHA` | `"srcrgb": "one", "dstrgb": "1-srcalpha"` |
| `STRAIGHT` | `SRC_ALPHA`、`ONE_MINUS_SRC_ALPHA`；alpha 为 `ONE`、`ONE_MINUS_SRC_ALPHA` | `"srcrgb": "srcalpha", "dstrgb": "1-srcalpha", "srcalpha": "one", "dstalpha": "1-srcalpha"` |
| `ADDITIVE` | `ONE`、`ONE` | `"srcrgb": "one", "dstrgb": "one"` |
| `SCREEN` | `ONE`、`ONE_MINUS_SRC_COLOR` | `"srcrgb": "one", "dstrgb": "1-srccolor"` |

调度器在绘制一批部件前设好所声明的混合。程序的 JSON 里有 `blend` 块时会取代它，必须与声明一致。另外三种混合的 alpha 因子与颜色因子相同，所以它们的 JSON 不需要写 alpha 两项。半透明通道本来也不写 alpha。

半透明部件的先后由包围体推出：包围体能证明一个部件在另一个后面时，它先画。包围体相交，或先后关系成环的部件，按稳定的近似顺序画出，结果记为 `DRAWN_UNORDERED`。v1 不拆分部件，所以不保证重叠部件内部逐像素的先后。经这套接口提交的两个部件同为 `ADDITIVE` 或同为 `SCREEN` 时，彼此不需要先后；这不等于两种顺序舍入到 8 位后的数值完全相同。它们与 GTL Additions 自身特效之间的先后仍由调度器决定。

| `PartFeature` | 含义 |
| --- | --- |
| `SCENE_DEPTH` | 渲染器要采样场景深度。 |
| `OWN_HAND_MASK` | 渲染器自己在第一人称手部上丢弃片元。 |
| `INTERNAL_SORT` | 保留；v1 中没有效果。 |
| `WRITES_DEPTH` | 为写深度的半透明部件保留；v1 对任何声明它的部件返回 `UNSUPPORTED`，`OPAQUE` 部件也一样。`OPAQUE` 部件写深度是由通道决定的。 |

### 绘制

`PartRenderer.draw(context, parts)` 在 `TRANSLUCENT` 通道里收到的是批次键相等的相邻部件，已经排好顺序；在 `OPAQUE` 通道里每次调用只有一个部件。上下文按这些部件声明的功能提供资源。`prepare(context, parts)` 可选：每帧每个通道（先 `OPAQUE`，后 `TRANSLUCENT`）在该通道绘制之前调用一次，传入该渲染器在这个通道里的全部可见部件，上下文按它们功能的并集提供资源，只用于离屏工作。

每次回调之前，以及在 `context.bindTarget()` 里，调度器设好下面的基线：

| | `OPAQUE` 通道 | `TRANSLUCENT` 通道 |
| --- | --- | --- |
| 目标 | 主目标，全帧 | 组合目标，全帧 |
| 深度 | `LEQUAL` 测试，写入 | `LEQUAL` 测试，不写入 |
| 颜色掩码 | RGBA | RGB |
| 混合 | 关闭 | `draw` 时为所声明的混合；`prepare` 时不设置 |
| 模板 | 不设置 | 光影下为本帧的手部测试 |
| 两者相同 | 背面剔除开，正面为逆时针，裁剪测试关，无程序，无顶点数组，纹理单元 0 | |

- 其余被你改动的状态要自己恢复。`bindTarget` 不会还原 pose stack、投影、像素缓冲、纹理绑定和采样器。
- `context.poseStack` 以相机为原点，push 与 pop 要成对。`context.frame` 提供相机、矩阵（`projection` 不带抖动）、视口、`partialTick`，以及 `renderTicks`（暂停时不走的渲染时钟，单位为 tick）。
- 往调度器的目标里写显示参照的颜色，alpha 按所声明的混合给出；调度器不会为光影做任何转换。自有目标的格式与颜色处理由你自己决定。
- 调度器不捕获异常：从回调里抛出的异常会使客户端崩溃，与方块实体渲染器相同。
- `context.report(part, outcome)` 只替换记录下来的结果，不会取消任何调用：在 `prepare` 里报告之后，`draw` 仍然会被调用，要在那里自己跳过绘制。

### 场景深度

只有声明了 `SCENE_DEPTH` 的部件在 `TRANSLUCENT` 通道里才能拿到非 null 的 `context.sceneDepth`；`OPAQUE` 通道运行时深度还不存在。`sceneDepth.bind(shader)` 设置 `scene_depth.glsl` 所声明的内容：

| GLSL | 含义 |
| --- | --- |
| `SceneDepth`、`SceneSize`、`SceneViewport`、`SceneInverseProjection` | 采样器与 uniform。 |
| `sceneDepthAt(ivec2 scenePixel)` | [0, 1] 的窗口深度，1 是天空。场景像素为 `gl_FragCoord.xy - SceneViewport`。 |
| `sceneViewPosition(vec2 uv, float depth)` | 视图空间位置。 |
| `sceneRayDistance(vec2 uv, float depth)` | 沿视线到场景的距离；天空返回 `1e20`。 |
| `sceneRayLimit(tEnter, tExit, tScene)` | 视线区间被场景挡住之后的终点：`max(tEnter, min(tExit, tScene))`。 |

`uv` 是场景像素除以 `SceneSize`。采样器和三个 uniform 在程序的 JSON 里也要声明，只有 import 是不够的。

这张贴图是 R32F 的副本，用 `texelFetch` 读取；它是借用的，不要删除。里面是什么取决于画质模式。流畅、高品质和光影下，区块的半透明地形（例如水和染色玻璃）被排除：某个像素上如果有东西在这些地形之后画在了它们前面，就保留最终深度，否则退回到不透明场景的深度。极佳（Fabulous）下直接使用最终的主深度。v1 中 `translucentTerrain` 在各种模式下都是 false。`jittered` 在每个光影帧里都是 true：它表示深度可能带有抖动，并不表示检测到了 TAA 设置，也拿不到抖动偏移；重建位置时用本帧不带抖动的投影。贴图重新分配时 `generation` 会变。

### 手部遮罩

只有声明了 `OWN_HAND_MASK` 的部件在光影下才能拿到非零的 `context.handMaskTexture`，两个通道都一样；`prepare` 收到的部件里只要有一个声明了它，就能拿到。它是一张借用的、全帧尺寸的贴图，手部所在处红色通道大于 0.5。画到调度器的目标时，用 `ivec2(gl_FragCoord.xy)` 取样；在自有目标里要换算回全帧像素。为 0 表示这次绘制没有遮罩，不算失败。

`SceneCapabilities.handStencil` 表示调度器有能力让半透明绘制避开手部。它是一项能力，不是某一帧的状态：画面里有手的帧，在手部遮罩或模板程序没有就绪时绘制不带模板；没有手的帧两者都不需要。模板只管画到调度器目标里的内容，不管自有目标。

### 结果

| `SubmitResult` | 含义 |
| --- | --- |
| `ACCEPTED` | 部件进入了本帧的队列，之后仍可能被剔除。 |
| `WRONG_VIEW` | 阴影通道。 |
| `LATE` | 提交窗口已关闭。 |
| `DUPLICATE` | 本帧已有同键的部件，保留先提交的那个。 |
| `UNSUPPORTED` | 部件声明了 `WRITES_DEPTH`。 |
| `INVALID_BOUNDS` | 包围体含非有限值、半径为负，或一块也没有。 |

| `PartOutcome` | 含义 |
| --- | --- |
| `DRAWN` | 按顺序画出。 |
| `DRAWN_UNORDERED` | 按稳定的近似顺序画出：它的包围体与另一个部件相交，或它处在成环的先后关系里。 |
| `DRAWN_LAYERED` | 与宇宙之心逐像素分出了先后；只有 GTL Additions 自己的渲染器能得到。 |
| `CULLED` | 在视野之外。 |
| `PENDING` | 它的资源正在发布。 |
| `RESOURCE_UNAVAILABLE` | 由渲染器报告；调度器自己的深度程序缺失时也由调度器记下。 |
| `FAILED` | 由渲染器报告；GTL Additions 自己的宇宙之心内部出错时也会记下它。调度器不会把从外部渲染器或提供者回调里抛出的异常转成它。 |
| `DROPPED` | 被某一帧接受，但那一帧结束时既没有画它也没有剔除它。 |

在下一帧结束之前，`lastOutcome` 一直返回最近结束的那一帧的结果。

### 通知

`onResourcesReloaded(generation)`、`onPipelineChanged(generation)` 和 `onLevelChanged()` 在下一个渲染 tick 开始时送达，不论当时有没有世界。多次变化可能合并成一次调用。已经送达的通知不会为之后注册的提供者补发；还在等下一个 tick 的变化，会送给届时已注册的每个提供者。通知只标记发生了变化，并不表示你的或别的 mod 的资源已经重新加载并发布。`SceneFrame.resourceGeneration` 与 `pipelineGeneration` 是同一组计数，只增不减，可能跳号。

### 版本与限制

- `SceneRendering.API_VERSION` 为 1，在运行时读取。对 GTL Additions 的最低版本要求写在 `mods.toml` 里。
- 每个提供者在客户端生命周期内只注册一次，id 在自己的命名空间内保持唯一。唯一性由调用方保证：`register` 不检查 id 是否重复，也不会返回已有的句柄。不要在提供者回调或渲染器里注册。
- 只支持主视图。调度器只认得 Oculus 的阴影通道，别的都不识别：同一个 tick 里把世界再渲染一遍，结果没有定义。
- 没有提交的内容不归这套接口管。那样的绘制落在哪里，取决于它所属的 mod 选的阶段。部件绘制时已经在画面上的内容是背景颜色；它的深度只有被本帧的深度规则选入时才会遮挡部件（见[场景深度](#场景深度)）。之后才画的内容可以盖住部件。
- v1 中 `SceneCapabilities.layeredVolumes` 为 false：GTL Additions 给宇宙之心用的逐像素分层不对其他 mod 开放。
- `SceneCapabilities.glVersion` 是主版本 × 10 + 次版本；`computeShaders` 表示计算着色器是否可用。

## 出问题时

同一个现象可能有几种原因；下表说的是先检查什么。

| 现象 | 检查 |
| --- | --- |
| 什么都没画，`lastOutcome` 是 null | null 只说明最近结束的那一帧里没有这个键的结果。检查 `submit` 的返回值、调用所在的阶段，以及提交之后是否已经过了一个渲染 tick。`SceneRendering.frame()` 在 `AFTER_SKY` 之前、`AFTER_BLOCK_ENTITIES` 之后、没有世界时和阴影通道里都是 null。再检查查询用的键与提交时的是否相同。 |
| 特效明明在屏幕上，结果却是 `CULLED` | 先检查包围体：是不是世界坐标，够不够盖住部件画出的全部内容。 |
| `RESOURCE_UNAVAILABLE` | 要么是你的渲染器为自己的着色器或贴图报告的，要么是调度器自己的深度程序没有加载成功、由调度器记下的。到日志里找加载时的报错。 |
| `submit` 返回 `DUPLICATE` | 同一帧里有两次提交用了同一个键。先检查同一个部件是不是经两条路径提交了两次；确实是两个部件，再给它们各自的编号。 |
| `build()` 抛出 `IllegalStateException` | 半透明部件没有声明 `blend`，或部件没有声明 `bounds`。包围体里的非法数值不会抛异常，而是由 `submit` 返回 `INVALID_BOUNDS`。 |
| 特效被画了两遍，或只在光影下不对 | 先检查是不是还留着直接绘制的路径。提交过的部件不能再在别处画。 |
| 亮度或透明度不对 | 对照检查着色器 JSON 的 `blend` 块与声明的 `PartBlend`，以及着色器的输出与它是否匹配（是否预乘）。 |
| 两个特效随镜头移动互换前后，结果为 `DRAWN_UNORDERED` | 它们的包围体相交，或先后关系成了环。包围体比实际松会造成假冲突，所以要让它贴合；`PartBounds.union` 在这里有用，但它不会把一个部件拆开，也修不了真实的相交。想让两半分别排序，就把它们作为两个部件提交。不要把包围体缩到盖不住几何。 |
| 光影下实心部件盖住了手 | 检查部件是否声明了 `OWN_HAND_MASK`、着色器是否按遮罩丢弃。再检查那一帧的遮罩是不是 0：没有遮罩就无从丢弃。 |
| 你的特效之后，后面的特效画坏了 | 先检查你的渲染器有没有留下基线管不到的状态改动。改过的要还原，缓冲在 `finally` 里解绑。 |
| 客户端在调度器里崩溃 | 看堆栈确认异常是从哪里抛出的：渲染器、提供者回调，或者都不是。异常一律不会被捕获。只有已知的、可预见的缺资源才应改为报告结果，而且只有渲染器手里有可以用来报告的上下文；其他异常是要修的缺陷。 |
