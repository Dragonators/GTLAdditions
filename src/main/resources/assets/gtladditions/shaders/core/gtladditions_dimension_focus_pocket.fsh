#version 150

#moj_import <gtladditions:dimension_focus_pocket.glsl>

uniform vec3 CameraLocal;
uniform int OpaquePass;

in vec3 localPos;

out vec4 fragColor;

void main() {
    // Drawn on the sphere's back faces only, so every ray enters the pocket once.
    setRay(CameraLocal, localPos);
    float a = softEdge();
    // Only fully covered pixels establish depth; keep the fading rim translucent.
    if ((OpaquePass != 0) != (a >= 1.0)) discard;
    fragColor = vec4(pocketColor() * a, a);
}
