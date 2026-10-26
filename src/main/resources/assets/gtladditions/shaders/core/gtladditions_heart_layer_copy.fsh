#version 150
uniform sampler2DArray HeartImages;
#moj_import <gtladditions:heart_image_sampling.glsl>

uniform int ImageIndex;
uniform int AlphaOnly;
uniform vec4 SkipRect;
out vec4 fragColor;
void main() {
    if (all(greaterThanEqual(gl_FragCoord.xy,SkipRect.xy)) &&
        all(lessThan(gl_FragCoord.xy,SkipRect.xy+SkipRect.zw))) discard;
    vec4 color=sampleHeartImage(gl_FragCoord.xy,ImageIndex);
    fragColor=AlphaOnly==1 ? vec4(color.a) : color;
}
