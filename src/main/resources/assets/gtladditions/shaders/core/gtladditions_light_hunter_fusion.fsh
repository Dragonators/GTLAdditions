#version 150
#moj_import <gtladditions:light_hunter_fusion.glsl>
out vec4 fragColor;
void main() {
    fusionMain();
    fragColor=fusionColor;
}
