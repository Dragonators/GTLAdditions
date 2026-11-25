#version 150

in vec3 Position;
in vec2 UV0;
in vec4 Color;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec3 viewPosition;
out vec3 viewNormal;
out vec3 viewTangent;
out vec4 lightColor;

void main() {
    vec4 position = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * position;
    viewPosition = position.xyz;
    viewNormal = mat3(ModelViewMat) * Normal;
    vec3 tangent = UV0.x > 4.0 ? vec3(0.0) : vec3(cos(UV0.x) * cos(UV0.y), sin(UV0.y), sin(UV0.x) * cos(UV0.y));
    viewTangent = mat3(ModelViewMat) * tangent;
    lightColor = Color;
}
