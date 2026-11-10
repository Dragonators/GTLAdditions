#version 150

in vec3 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec3 viewPosition;
out vec3 viewNormal;

void main() {
    vec4 view = ModelViewMat * vec4(Position, 1.0);
    viewPosition = view.xyz;
    // The meshes are centred on their origin and scaled uniformly.
    viewNormal = mat3(ModelViewMat) * Position;
    gl_Position = ProjMat * view;
}
