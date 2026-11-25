#version 150

#moj_import <gtladditions:dimension_focus_pocket.glsl>

uniform vec3 CameraLocal;
uniform float LayerAngle;

in vec3 layerPos;
in vec3 layerNormal;

out vec4 fragColor;

void main() {
    // Far needles use the space behind their centre-facing faces to avoid bright blocks inside the pocket.
    float facing = smoothstep(0.05, 0.6, dot(normalize(layerNormal), -normalize(layerPos)));
    setRay(CameraLocal, rotY(LayerAngle) * layerPos);
    float a = facing * 0.7 * softEdge();
    // Evaluate cubemap sampling in uniform control flow before per-fragment discard so derivatives stay valid on the boundary.
    vec3 color = pocketColor();
    if (facing < 0.003) discard;
    fragColor = vec4((color * 0.75 + vec3(0.008, 0.02, 0.055)) * a, a);
}
