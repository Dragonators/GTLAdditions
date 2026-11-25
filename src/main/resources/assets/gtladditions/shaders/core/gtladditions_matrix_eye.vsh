#version 150

in vec3 Position;
uniform mat4 ModelViewMat, ProjMat;
uniform vec3 uScale;
uniform float uSeat;
out vec3 vLocal, vWorld;

void main() {
    vLocal = Position;
    vWorld = vec3(Position.xy * uScale.xy, uSeat + Position.z * uScale.z);
    gl_Position = ProjMat * ModelViewMat * vec4(vWorld, 1.);
}
