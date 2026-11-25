uniform sampler2D SceneDepth;
uniform mat4 InverseProjection;
uniform vec2 SceneSize;
uniform vec2 PassSize;

float heartSceneDepthAt(vec2 uv) {
    ivec2 pixel = clamp(ivec2(uv * SceneSize), ivec2(0), ivec2(SceneSize) - 1);
    return texelFetch(SceneDepth, pixel, 0).r;
}

float heartSceneDistance(vec2 uv, float depth) {
    if (depth >= 1.0) return 1e20;
    vec4 view = InverseProjection * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    return length(view.xyz) / max(abs(view.w), 1e-20);
}

// Bilinear half-resolution filtering at depth discontinuities can bleed occluded emission
// onto foreground blocks or erase visible emission beside them.
bool heartMaterialDepthEdge(vec2 uv, float depth, float sceneDistance, float volumeExit) {
    ivec2 size = textureSize(MaterialSampler, 0);
    ivec2 base = ivec2(floor(uv * vec2(size) - 0.5));
    float current = min(sceneDistance, volumeExit);
    float tolerance = max(0.04, current * 0.001);
    for (int y = 0; y < 2; ++y) {
        for (int x = 0; x < 2; ++x) {
            ivec2 pixel = clamp(base + ivec2(x, y), ivec2(0), size - 1);
            float adjacentDepth = heartSceneDepthAt((vec2(pixel) + 0.5) / vec2(size));
            if (adjacentDepth == depth) continue;
            // Compare both depths on the current ray: radial distance changes across
            // a flat wall even when its view-space depth is completely continuous.
            float adjacent = heartSceneDistance(uv, adjacentDepth);
            if (abs(min(adjacent, volumeExit) - current) > tolerance) return true;
        }
    }
    return false;
}
