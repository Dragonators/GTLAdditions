#version 150

in vec3 Position;
in vec2 UV0;
in vec4 Color;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec3 LocalPos;
out vec3 LocalNormal;
out vec2 AtlasUv;
flat out vec4 Data;

void main() {
    LocalPos = Position;
    LocalNormal = Normal;
    AtlasUv = UV0;
    Data = Color;
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
