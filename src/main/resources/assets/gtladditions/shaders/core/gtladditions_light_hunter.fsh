#version 150
#moj_import <gtladditions:light_hunter_material.glsl>
out vec4 fragColor;
void main() {
    materialMain();
    fragColor = materialColor;
}
