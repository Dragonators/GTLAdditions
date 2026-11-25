#version 150

in vec3 Position;
in vec4 Color;

uniform mat4 ModelMat;
uniform vec4 Tint;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec4 LineColor;

void main() {
    LineColor = Color * Tint;
    gl_Position = ProjMat * ModelViewMat * vec4((ModelMat * vec4(Position, 1.0)).xyz, 1.0);
}
