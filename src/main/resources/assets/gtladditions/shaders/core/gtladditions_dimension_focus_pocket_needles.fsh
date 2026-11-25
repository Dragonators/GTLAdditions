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
    if (facing < 0.003) discard;
    setRay(CameraLocal, rotY(LayerAngle) * layerPos);
    float a = facing * 0.7 * softEdge();
    fragColor = vec4((pocketColor() * 0.75 + vec3(0.008, 0.02, 0.055)) * a, a);
}
