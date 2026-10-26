#version 150
#moj_import <gtladditions:light_hunter_paradox.glsl>
out vec4 fragColor;
void main() {
    paradoxMain();
    fragColor=vec4(paradoxColor.rgb*paradoxColor.a,paradoxColor.a);
}
