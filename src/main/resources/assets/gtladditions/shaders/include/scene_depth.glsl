uniform sampler2D SceneDepth;
uniform vec2 SceneSize;
uniform vec2 SceneViewport;
uniform mat4 SceneInverseProjection;

// Window depth in [0, 1] at a scene pixel (gl_FragCoord.xy - SceneViewport); 1 is the sky.
float sceneDepthAt(ivec2 scenePixel) {
    return texelFetch(SceneDepth, clamp(scenePixel, ivec2(0), ivec2(SceneSize) - 1), 0).r;
}

vec3 sceneViewPosition(vec2 uv, float depth) {
    vec4 view = SceneInverseProjection * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    return view.xyz / view.w;
}

// Eye distance to the scene along the view ray through uv; the sky returns 1e20.
float sceneRayDistance(vec2 uv, float depth) {
    return depth >= 1.0 ? 1e20 : length(sceneViewPosition(uv, depth));
}

// The exit of the ray interval [tEnter, tExit] once the scene is in the way; never before tEnter.
float sceneRayLimit(float tEnter, float tExit, float tScene) {
    return max(tEnter, min(tExit, tScene));
}
