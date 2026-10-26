uniform sampler2D SceneHandMask;
uniform int SceneHandMaskEnabled;
bool sceneHandOccluded(vec2 pixel) {
    return SceneHandMaskEnabled != 0 && texelFetch(SceneHandMask, ivec2(pixel), 0).r > 0.5;
}
