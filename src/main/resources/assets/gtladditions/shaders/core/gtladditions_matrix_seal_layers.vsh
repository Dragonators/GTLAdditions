#version 330 core
in vec3 Position;
in vec4 Color;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
out vec4 vertexColor;
out float viewDepth;
void main() {
    vertexColor = Color;
    viewDepth = -(ModelViewMat * vec4(Position, 1.0)).z;
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
