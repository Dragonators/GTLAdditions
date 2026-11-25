#version 150

#moj_import <gtladditions_scene_example:orb_material.glsl>

in vec3 viewPosition;
in vec3 viewNormal;

out vec4 fragColor;

void main() {
    fragColor = orbShell(viewPosition, viewNormal);
}
