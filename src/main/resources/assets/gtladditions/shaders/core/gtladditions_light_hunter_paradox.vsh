#version 150
in vec3 Position;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform float WindowPlane;
uniform float WindowBias;
out vec2 windowCoord;
out float effectViewDepth;
void main() {
    windowCoord=vec2(-(Position.z-12.0),Position.y-6.0)/12.2;
    vec3 p=vec3(WindowPlane+WindowBias,Position.yz);
    vec4 view=ModelViewMat*vec4(p,1.0);
    effectViewDepth=-view.z;
    gl_Position=ProjMat*view;
}
