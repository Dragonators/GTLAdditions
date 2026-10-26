#version 150
in vec3 Position;
in vec2 UV0;
in vec4 Color;
in vec3 Normal;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat4 ModelMat;
out vec2 uv;
out vec4 tint;
out vec3 worldPosition;
out vec3 worldNormal;
void main() {
    uv=UV0;
    tint=Color;
    worldPosition=(ModelMat*vec4(Position,1.0)).xyz;
    worldNormal=normalize(mat3(ModelMat)*Normal);
    gl_Position=ProjMat*ModelViewMat*vec4(worldPosition,1.0);
}
