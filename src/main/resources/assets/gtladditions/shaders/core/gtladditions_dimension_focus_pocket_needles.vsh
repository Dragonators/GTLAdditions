#version 150

in vec3 Position;
in vec4 Color;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec3 layerPos;
out vec3 layerNormal;

void main() {
    layerPos = Position;
    layerNormal = Normal;
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
