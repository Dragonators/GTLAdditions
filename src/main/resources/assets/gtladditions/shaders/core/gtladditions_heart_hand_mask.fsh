#version 150

uniform sampler2D SolidBeforeDepthSampler;
uniform sampler2D SolidAfterDepthSampler;
uniform sampler2D TranslucentBeforeDepthSampler;
uniform sampler2D TranslucentAfterDepthSampler;
uniform float SolidHandEnabled;
uniform float TranslucentHandEnabled;
out vec4 fragColor;

void main() {
    ivec2 pixel = ivec2(gl_FragCoord.xy);
    bool solidChanged = SolidHandEnabled > 0.5
        && texelFetch(SolidBeforeDepthSampler, pixel, 0).r != texelFetch(SolidAfterDepthSampler, pixel, 0).r;
    bool translucentChanged = TranslucentHandEnabled > 0.5
        && texelFetch(TranslucentBeforeDepthSampler, pixel, 0).r != texelFetch(TranslucentAfterDepthSampler, pixel, 0).r;
    fragColor = vec4(solidChanged || translucentChanged ? 1.0 : 0.0);
}