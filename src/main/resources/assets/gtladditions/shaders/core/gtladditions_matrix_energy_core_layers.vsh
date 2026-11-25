#version 330 core

in vec3 Position;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
out vec3 LocalPosition;

void main() {
    LocalPosition = Position;
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
