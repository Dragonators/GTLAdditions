#version 150

in vec3 Position;
in vec2 UV0;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 SpriteUv;
out vec4 SpriteColor;

void main() {
    SpriteUv = UV0;
    SpriteColor = Color;
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
