#version 150

#moj_import <gtladditions:dimension_focus_pocket.glsl>

uniform vec3 CameraLocal;
uniform int OpaquePass;

in vec3 localPos;

out vec4 fragColor;

void main() {
    // Inside the solid core radius every forward ray has rc <= |CameraLocal|, so softEdge() is uniformly 1.0.
    if (OpaquePass == 0 && dot(CameraLocal, CameraLocal) < POCKET_RADIUS * POCKET_RADIUS * (1.0 - 0.43 * 0.43)) discard;
    // Drawn on the sphere's back faces only, so every ray enters the pocket once.
    setRay(CameraLocal, localPos);
    float a = softEdge();
    // Evaluate cubemap sampling in uniform control flow before per-fragment discard so derivatives stay valid on the seam.
    vec3 color = pocketColor();
    // Only fully covered pixels establish depth; keep the fading rim translucent.
    if ((OpaquePass != 0) != (a >= 1.0)) discard;
    fragColor = vec4(color * a, a);
}
