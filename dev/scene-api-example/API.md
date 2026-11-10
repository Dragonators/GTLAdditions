# GTL Additions scene API (v1)

[中文](API.zh.md)

Package `com.gtladd.gtladditions.api.client.scene`, client only. The example mod in this directory shows the main ways
in, a Java provider that pushes its parts and a Kotlin provider that is asked for them; [README.md](README.md) lists what each
file shows and how to build and try it.

1. [What you get](#what-you-get)
2. [Set up](#set-up)
3. [Your first effect](#your-first-effect)
4. [Choosing pass and blend](#choosing-pass-and-blend)
5. [Next steps](#next-steps): scene depth, the hand, being asked for parts, reloads, offscreen work
6. [Reference](#reference)
7. [When something is wrong](#when-something-is-wrong)

## What you get

GTL Additions draws its machine effects through one scheduler per frame. A mod that hands its own world effects to that
scheduler gets the same treatment:

- translucent effects of all participating mods are sorted far to near by their bounds, instead of in event order;
- the effects are tested against the scene, and a renderer can read the scene depth to fade or clip;
- under a shaderpack (Oculus) the effects are drawn after the pack's final pass, so the post-processing of that world
  pipeline no longer alters them, and translucent parts are kept off the first-person hand while the hand mask and the
  stencil program are ready.

What other mods draw without the API is neither sorted nor managed by it.

You describe each effect as a *part*: where it is, how it blends, and a renderer that draws it. The scheduler decides when
the renderer runs and prepares the target and render state for it.

You do not need the API for ordinary models drawn with vanilla render types. It is meant for effects with their own shaders:
glows, beams, shells, volumes.

## Set up

- Compile against the GTL Additions JAR as a compile-only dependency.
- Declare `gtladditions` in `mods.toml`, with the oldest version that has the API. To keep your mod working without GTL
  Additions, make the dependency optional and follow "The optional dependency" in [README.md](README.md).
- Touch the API from client code only, and only on the render thread.

## Your first effect

A translucent sphere at a world position, in Java. It is `JavaOrbs.java` of the example without the scene depth, which
[Fade or clip against the world](#fade-or-clip-against-the-world) adds afterwards. The snippets show only the calls that
matter here. The event wiring is in `SceneBridge.java`, shader loading and the animation uniform in `JavaOrbs.java`, and the
sphere mesh in `Meshes.java`. `Orb` is a short name used here for the example's `Placements.Placement`.

**1. Register once.** Client setup runs on a worker thread, so hand the call to the render thread.

```java
private static ProviderHandle handle;

static void onClientSetup(FMLClientSetupEvent event) {
    event.enqueueWork(() -> handle = SceneRendering.register(new ResourceLocation("mymod", "orbs")));
}
```

**2. Load a shader.** Nothing here is specific to the API except one rule: the blend in the shader JSON is the blend you
will declare for the part. For `PartBlend.PREMULTIPLIED`:

```json
"blend": {"func": "add", "srcrgb": "one", "dstrgb": "1-srcalpha"}
```

`orb_direct.json` in the example is a complete file for a program like this one, which does not read the scene depth.
The [blend table](#a-part) lists the line for every `PartBlend`.

**3. Submit a part every frame**, from a `RenderLevelStageEvent` at `AFTER_ENTITIES` or from a block entity renderer's
`render` method. Do not draw the effect yourself as well.

```java
static void onStage(RenderLevelStageEvent event) {
    if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
    SceneFrame frame = SceneRendering.frame();
    if (frame == null) return;                       // a shaderpack's shadow pass
    for (Orb orb : orbs) {
        frame.submit(ScenePart.builder(new PartKey(handle, orb.id(), 0), RENDERER, orb)
                .blend(PartBlend.PREMULTIPLIED)
                .bounds(PartBounds.sphere(orb.x(), orb.y(), orb.z(), orb.radius()))   // world space
                .batchKey(RENDERER)                  // adjacent orbs arrive in one draw call
                .build());
    }
}
```

In a block entity renderer use `PartKey.block(handle, pos, 0)`. The third value numbers the parts of one instance.

**4. Draw.** The scheduler calls the renderer when it is the parts' turn. The target, viewport, depth test and blend are
already set; the pose stack is camera-relative.

```java
static final PartRenderer<Orb> RENDERER = (context, parts) -> {
    ShaderInstance shader = orbShader;
    if (shader == null) {                            // never throw: report and return
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
                pose.scale(radius, radius, radius);  // the mesh is a unit sphere; the bounds do not scale it
                mesh.drawWithShader(pose.last().pose(), RenderSystem.getProjectionMatrix(), shader);
            } finally {
                pose.popPose();
            }
        }
    } finally {
        VertexBuffer.unbind();                       // leave the state as you found it
    }
};
```

**5. Check the result.** `SceneRendering.lastOutcome(key)` tells what happened to a part in the last finished frame:
`DRAWN`, `CULLED`, `RESOURCE_UNAVAILABLE` and so on. Starting the game with `-Dgtladditions.scene.debug=true` logs each
outcome once per provider, the first time it occurs in that run. If the part does not show up, go to
[When something is wrong](#when-something-is-wrong).

## Choosing pass and blend

| The effect | Declare | Shader output |
| --- | --- | --- |
| A solid surface that hides what is behind it: a core, a hull, a cut-out shape | `pass(PartPass.OPAQUE)` | Alpha 1; `discard` is allowed |
| Light that only adds: glow, beams, sparks | `blend(PartBlend.ADDITIVE)` | Colour to add |
| Light that brightens towards white without exceeding it | `blend(PartBlend.SCREEN)` | Colour in [0, 1] |
| Real transparency: smoke, glass, shells | `blend(PartBlend.PREMULTIPLIED)` | Colour already multiplied by alpha |
| The same from a shader that outputs plain alpha | `blend(PartBlend.STRAIGHT)` | Colour and alpha |

- An effect with a solid core and a glow is two parts with the same instance and different part numbers; `KotlinBeacons.kt`
  does this.
- Two parts of yours that are both `ADDITIVE`, or both `SCREEN`, need no order between them. Other translucent parts are
  ordered by their bounds, and the order is only as good as the bounds: where bounds overlap it can be approximate. Keep the bounds
  tight, and use `PartBounds.union` for a shape that a single box or sphere would cover badly.
- Bounds are in world space and must cover everything the part can draw, including animation and flashes.

## Next steps

### Fade or clip against the world

Declare `PartFeature.SCENE_DEPTH`, bind the depth in the renderer, and read it in the shader. This turns the program of
the first effect into the example's `orb_scene`:

```java
.features(PartFeature.SCENE_DEPTH)                   // on the builder
context.getSceneDepth().bind(shader);                // in draw, before drawing
```

```glsl
#moj_import <gtladditions:scene_depth.glsl>

vec2 scenePixel = gl_FragCoord.xy - SceneViewport;
float depth = sceneDepthAt(ivec2(scenePixel));       // takes a pixel
vec2 uv = scenePixel / SceneSize;                    // the other helpers take normalised coordinates
float scene = sceneRayDistance(uv, depth);
fragColor = colour * smoothstep(0.0, 0.4, scene - length(viewPosition));   // fades where it meets the scene
```

The `#moj_import` line stays at file scope, because it brings in uniforms and functions; the lines after it belong in
`main`, where `colour` stands for your material. `orb_scene.fsh` is the complete shader.

Two declarations are needed, and the import gives only the first. The GLSL declares the sampler and the uniforms; the
program's JSON has to list them too, because `bind` sets them through the JSON's entries and skips what is missing there:
the sampler `SceneDepth`, and the uniforms `SceneSize`, `SceneViewport` (two floats each) and `SceneInverseProjection`
(a matrix). See [orb_scene.json](src/main/resources/assets/gtladditions_scene_example/shaders/core/orb_scene.json).

### Keep the first-person hand in front

Under a shaderpack the parts are drawn after the hand. Translucent parts are kept off the hand for you while the hand mask
and the stencil program are ready (see [Hand mask](#hand-mask)). An `OPAQUE` part has
to do it itself (`beacon_core.fsh`):

```kotlin
.features(PartFeature.OWN_HAND_MASK)                 // on the builder
val mask = context.handMaskTexture                   // 0: nothing to discard in this draw
shader.setSampler("HandMask", mask)
shader.safeGetUniform("HandMaskEnabled").set(if (mask != 0) 1 else 0)
try {
    // draw the parts
} finally {
    shader.safeGetUniform("HandMaskEnabled").set(0)  // the program may be used again without a mask
}
```

```glsl
fragColor = material();                              // first the material
if (HandMaskEnabled != 0 && texelFetch(HandMask, ivec2(gl_FragCoord.xy), 0).r > 0.5) discard;
```

These two lines belong in `main`, with `material()` standing for your shading; `beacon_core.fsh` is the complete shader.

`ShaderInstance.setSampler`, used here as in `KotlinBeacons.kt`, only registers the texture; it is bound when the program
is applied. If you bind a texture unit by hand instead, do it after any render-type or shader setup of your own and restore
the active unit. Reset your switch uniform when done. `gl_FragCoord` addresses the mask only while you draw into the scheduler's
target; in a target of your own with another size or origin, map the coordinates back to the full frame. A translucent
part may declare `OWN_HAND_MASK` as well, for what it draws into its own targets.

### Be asked for the parts

Instead of pushing parts from an event, implement `SceneProvider.collect(frame)` and register the provider:
`SceneRendering.register(id, provider)`. The scheduler calls it once per frame, just before submissions close.
`KotlinBeacons.kt` works this way.

### React to reloads

The same `SceneProvider` can override `onResourcesReloaded`, `onPipelineChanged` (a shaderpack was switched on, off or
reloaded) and `onLevelChanged` (the client level was unloaded), to drop caches that depend on them.

### Offscreen work

Override `PartRenderer.prepare` to render into your own framebuffers before the pass draws. It receives all visible parts
of the renderer in that pass. Inside `draw`, call `context.bindTarget()` after using a framebuffer of your own.

## Reference

### A frame

1. The frame begins at `AFTER_SKY` of the main view. `SceneRendering.frame()` returns it until submissions close, and null
   otherwise: during a shaderpack's shadow pass and outside this window.
2. Submissions close at the end of `AFTER_BLOCK_ENTITIES`. Just before that, `SceneProvider.collect(frame)` is called on every
   registered provider.
3. `OPAQUE` parts are drawn into the main target with depth writes, in submission order: without a shaderpack right after
   submissions close, so glass and water blend over them; with a shaderpack after its final pass.
4. `TRANSLUCENT` parts are drawn far to near by their bounds: without a shaderpack before weather
   (`CompositionPoint.BEFORE_WEATHER`), with a shaderpack after its final pass (`AFTER_SHADERPACK_FINAL`).
5. The frame ends when the next render tick begins. From then on `SceneRendering.lastOutcome(key)` returns what happened to
   each part.

Nothing of a frame may be kept for a later one: not the `SceneFrame`, a draw context, the scene depth or the hand mask
texture.

### A part

| Builder call | Meaning |
| --- | --- |
| `builder(key, renderer, payload)` | `PartKey` is the provider handle, an instance (`long`) and a part number; `PartKey.block` uses a block position. One part per key per frame. The payload must not change until the frame ends. |
| `pass(...)` | `TRANSLUCENT` (default) or `OPAQUE`. |
| `blend(...)` | Required for a translucent part. |
| `bounds(...)` | Required. Conservative world-space bounds: `box`, `sphere`, or a `union` of convex pieces. The scheduler culls and sorts by them. |
| `features(...)` | What the renderer needs; see below. |
| `batchKey(...)` | Translucent parts only: adjacent parts of one renderer whose keys are set and equal are drawn in one `draw` call; without a key each part gets its own call. Equal keys promise the same blend, features and drawing resources. `OPAQUE` parts are always drawn one per call. |

| `PartBlend` | Blend factors | Shader JSON `blend` |
| --- | --- | --- |
| `PREMULTIPLIED` | `ONE`, `ONE_MINUS_SRC_ALPHA` | `"srcrgb": "one", "dstrgb": "1-srcalpha"` |
| `STRAIGHT` | `SRC_ALPHA`, `ONE_MINUS_SRC_ALPHA`; alpha `ONE`, `ONE_MINUS_SRC_ALPHA` | `"srcrgb": "srcalpha", "dstrgb": "1-srcalpha", "srcalpha": "one", "dstalpha": "1-srcalpha"` |
| `ADDITIVE` | `ONE`, `ONE` | `"srcrgb": "one", "dstrgb": "one"` |
| `SCREEN` | `ONE`, `ONE_MINUS_SRC_COLOR` | `"srcrgb": "one", "dstrgb": "1-srccolor"` |

The scheduler sets the declared blend before a batch. A program whose JSON has a `blend` block replaces it and must equal the
declaration. The other three blends use the same factors for alpha as for colour, so their JSON needs no alpha entries.
The translucent pass does not write alpha in any case.

The order of translucent parts is derived from their bounds: a part is drawn before another when the bounds prove that it
lies behind. Parts whose bounds intersect, or whose relations form a cycle, are drawn in a stable approximate order and
reported as `DRAWN_UNORDERED`. v1 never splits a part, so the order pixel by pixel inside overlapping parts is not promised.
Two parts submitted through this API need no order between them when both are `ADDITIVE` or both are `SCREEN`; this does
not promise that the two orders round to identical 8-bit values. The order against GTL Additions' own effects is decided
by the scheduler.

| `PartFeature` | Meaning |
| --- | --- |
| `SCENE_DEPTH` | The renderer samples the scene depth. |
| `OWN_HAND_MASK` | The renderer discards on the first-person hand itself. |
| `INTERNAL_SORT` | Reserved; no effect in v1. |
| `WRITES_DEPTH` | Reserved for translucent parts that write depth; v1 rejects every part declaring it with `UNSUPPORTED`, `OPAQUE` ones included. An `OPAQUE` part writes depth because of its pass. |

### Drawing

`PartRenderer.draw(context, parts)` receives, in the `TRANSLUCENT` pass, adjacent parts with equal batch keys, already in
order, and in the `OPAQUE` pass one part per call. The context serves the features of those parts. `prepare(context, parts)`
is optional: it runs once per frame and pass (`OPAQUE`, then `TRANSLUCENT`) before that pass draws, with all visible parts
of the renderer in that pass and the union of their features, for offscreen work only.

Before each callback, and again in `context.bindTarget()`, the scheduler sets this baseline:

| | `OPAQUE` pass | `TRANSLUCENT` pass |
| --- | --- | --- |
| Target | Main target, full frame | Composition target, full frame |
| Depth | Test `LEQUAL`, writes on | Test `LEQUAL`, writes off |
| Colour mask | RGBA | RGB |
| Blend | Off | The declared blend for `draw`; not set for `prepare` |
| Stencil | Not set | Under a shaderpack, the hand test of this frame |
| Both | Back-face culling on, front face CCW, scissor off, no program, no vertex array, texture unit 0 | |

- Restore everything else you change. `bindTarget` does not put back the pose stack, the projection, pixel buffers,
  texture bindings or samplers.
- `context.poseStack` is camera-relative; balance push and pop. `context.frame` carries the camera, the matrices
  (`projection` is unjittered), the viewport, `partialTick` and `renderTicks`, a pause-aware clock in ticks.
- Write display-referred colour into the scheduler's target, with the alpha your blend expects; the scheduler converts
  nothing for a shaderpack. Targets of your own keep whatever format and colour handling you choose.
- The scheduler catches no exception: one that leaves a callback crashes the client, as in a block entity renderer.
- `context.report(part, outcome)` only replaces the recorded outcome. It cancels nothing: after a report from `prepare`,
  `draw` is still called, so skip the drawing there.

### Scene depth

`context.sceneDepth` is non-null only for a `SCENE_DEPTH` part in the `TRANSLUCENT` pass; the `OPAQUE` pass runs before the
depth exists. `sceneDepth.bind(shader)` sets what `scene_depth.glsl` declares:

| GLSL | Meaning |
| --- | --- |
| `SceneDepth`, `SceneSize`, `SceneViewport`, `SceneInverseProjection` | The sampler and uniforms. |
| `sceneDepthAt(ivec2 scenePixel)` | Window depth in [0, 1]; 1 is the sky. The scene pixel is `gl_FragCoord.xy - SceneViewport`. |
| `sceneViewPosition(vec2 uv, float depth)` | The view-space position. |
| `sceneRayDistance(vec2 uv, float depth)` | The eye distance to the scene along the view ray; `1e20` for the sky. |
| `sceneRayLimit(tEnter, tExit, tScene)` | The end of a ray interval once the scene is in the way: `max(tEnter, min(tExit, tScene))`. |

`uv` is the scene pixel divided by `SceneSize`. The program's JSON has to declare the sampler and the three uniforms as
well; the import alone is not enough.

The texture is an R32F copy, read with `texelFetch`, and borrowed: do not delete it. What it holds depends on the graphics
mode. In Fast and Fancy and under a shaderpack, chunk translucent terrain such as water and stained glass is left out: a
pixel keeps the final depth where something was drawn in front of that terrain after it, and falls back to the depth of the
opaque scene otherwise. In Fabulous the final main depth is used as it is. `translucentTerrain` is false in v1 in every
mode. `jittered` is true for every shaderpack frame: it says that the depth may carry a jitter, not that a TAA setting was
detected, and no offset is available; reconstruct positions with the frame's unjittered projection. `generation` changes
whenever the texture is reallocated.

### Hand mask

`context.handMaskTexture` is non-zero only for an `OWN_HAND_MASK` part under a shaderpack, in either pass; `prepare` gets it
when any of its parts declares the feature. It is a borrowed texture of the full frame's size whose red channel exceeds 0.5
on the first-person hand. `ivec2(gl_FragCoord.xy)` addresses it while you draw into the scheduler's target; from a target
of your own, map back to full-frame pixels. Zero means there is no mask for this draw, which is not a failure.

`SceneCapabilities.handStencil` says that the scheduler can keep translucent draws off the hand. It is a capability, not
the state of a frame: a frame that shows a hand draws without the stencil while the hand mask or the stencil program is
not ready, and a frame without a hand needs neither. The
stencil covers draws into the scheduler's target only, not into targets of your own.

### Results

| `SubmitResult` | Meaning |
| --- | --- |
| `ACCEPTED` | The part is queued for this frame; it may still be culled. |
| `WRONG_VIEW` | The shadow pass. |
| `LATE` | The submission window has closed. |
| `DUPLICATE` | The frame already holds a part with this key; that one stays. |
| `UNSUPPORTED` | The part declares `WRITES_DEPTH`. |
| `INVALID_BOUNDS` | A non-finite value, a negative radius or no piece at all. |

| `PartOutcome` | Meaning |
| --- | --- |
| `DRAWN` | Drawn in order. |
| `DRAWN_UNORDERED` | Drawn in a stable approximate order: its bounds intersect another part's, or it is in a cycle of order relations. |
| `DRAWN_LAYERED` | Ordered pixel by pixel against a Heart of the Universe; only GTL Additions' own renderers get this. |
| `CULLED` | Outside the view. |
| `PENDING` | Its resources are being published. |
| `RESOURCE_UNAVAILABLE` | Reported by the renderer, or by the scheduler when its own depth program is missing. |
| `FAILED` | Reported by the renderer; GTL Additions also records it for failures inside its own Heart. The scheduler does not turn an exception that escapes an external renderer or provider callback into it. |
| `DROPPED` | Accepted by a frame that ended without drawing or culling it. |

`lastOutcome` keeps returning the results of the last frame that ended until another frame ends.

### Notifications

`onResourcesReloaded(generation)`, `onPipelineChanged(generation)` and `onLevelChanged()` arrive when the next render tick
begins, with or without a level. Several changes may arrive as one call. A notification that has been delivered is not
replayed for a provider registered later; a change still waiting for the next tick reaches every provider registered by
then. They mark a change. They do not tell that any resource, yours or another mod's, has been reloaded and published.
`SceneFrame.resourceGeneration` and `pipelineGeneration` carry the same counters; they only grow and may skip values.

### Version and limits

- `SceneRendering.API_VERSION` is 1 and is read at run time. Require a minimum GTL Additions version in `mods.toml`.
- Register each provider once for the client's lifetime, with an id that is unique within your own namespace. Keeping it
  unique is up to you: `register` neither checks the id for duplicates nor returns an existing handle. Do not register from a provider callback or a renderer.
- Only the main view is supported. The scheduler recognises the shadow pass of Oculus and nothing else: rendering the
  level a second time within one tick has undefined results.
- Content that is not submitted is outside the API. Where such a draw lands depends on the stage its mod chose. What is
  already there when the parts are drawn is background colour, and its depth hides parts only where the frame's depth rules
  select it (see [Scene depth](#scene-depth)). What is drawn later can cover the parts.
- `SceneCapabilities.layeredVolumes` is false in v1: the per-pixel layering GTL Additions uses for its Heart of the Universe
  is not available to other mods.
- `SceneCapabilities.glVersion` is major * 10 + minor; `computeShaders` tells whether compute shaders are available.

## When something is wrong

One symptom can have several causes; the table says what to check first.

| What you see | Check |
| --- | --- |
| Nothing is drawn and `lastOutcome` is null | Null only says that the last finished frame had no result for this key. Check what `submit` returned, which stage the call comes from, and that a render tick has passed since. `SceneRendering.frame()` is null before `AFTER_SKY`, after `AFTER_BLOCK_ENTITIES`, without a level and in the shadow pass. Check that the key you query equals the one submitted. |
| `CULLED` while the effect should be on screen | Check the bounds first: world space, and large enough for everything the part draws. |
| `RESOURCE_UNAVAILABLE` | Either your renderer reported it for a shader or texture of its own, or the scheduler recorded it because its depth program failed to load. Look for the load error in the log. |
| `DUPLICATE` from `submit` | Two submissions used one key in a frame. Check first that the same part is not submitted along two paths; if they really are two parts, give each its own number. |
| `IllegalStateException` from `build()` | A translucent part without `blend`, or a part without `bounds`. Bad numbers in the bounds do not throw: `submit` answers them with `INVALID_BOUNDS`. |
| The effect is drawn twice, or looks wrong only under a shaderpack | Check first whether a direct drawing path is still active. A submitted part must not be drawn anywhere else. |
| Wrong brightness or transparency | Check the `blend` block of the shader JSON against the declared `PartBlend`, and whether the shader output matches it (premultiplied or not). |
| Two effects swap order as the camera moves, `DRAWN_UNORDERED` | Their bounds intersect, or their order relations form a cycle. Looser bounds than needed cause false conflicts, so make them fit; `PartBounds.union` helps there, but it does not split a part and cannot repair a real intersection. To have two halves ordered separately, submit them as two parts. Do not shrink bounds below the geometry. |
| An opaque part covers the hand under a shaderpack | Check that the part declares `OWN_HAND_MASK` and that the shader discards on the mask. Check also whether the mask was 0 in that frame: without a mask nothing can be discarded. |
| A later effect looks broken after yours | Check first whether your renderer leaves state changed that the baseline does not cover. Restore what you change, unbind buffers in `finally`. |
| The client crashes inside the scheduler | Read the stack trace to see where the exception was thrown: a renderer, a provider callback, or neither. Nothing is caught. Only a known, expected lack of a resource should become a reported outcome, and only a renderer has a context to report with; any other exception is a bug to fix. |
