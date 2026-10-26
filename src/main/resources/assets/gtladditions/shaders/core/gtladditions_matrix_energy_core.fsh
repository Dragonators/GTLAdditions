#version 150
#moj_import <gtladditions:matrix_energy_core.glsl>
#moj_import <gtladditions:heart_scene_hand.glsl>
out vec4 fragColor;
void main() {
    if (sceneHandOccluded(gl_FragCoord.xy)) discard;
    fragColor = coreMaterial();
}
