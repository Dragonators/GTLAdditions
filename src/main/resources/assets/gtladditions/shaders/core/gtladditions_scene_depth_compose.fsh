#version 150

uniform sampler2D SceneDepthIn;
uniform sampler2D OpaqueDepth;
uniform sampler2D TranslucentDepth;
uniform int ExcludeTranslucent;

out vec4 fragColor;

// Policy B: a surface written after chunk translucency and in front of it still occludes; otherwise look through it.
void main() {
    ivec2 pixel = ivec2(gl_FragCoord.xy);
    float scene = texelFetch(SceneDepthIn, pixel, 0).r;
    float depth = ExcludeTranslucent == 0 || scene < texelFetch(TranslucentDepth, pixel, 0).r ? scene : texelFetch(OpaqueDepth, pixel, 0).r;
    gl_FragDepth = depth;
    fragColor = vec4(depth, 0.0, 0.0, 1.0);
}
