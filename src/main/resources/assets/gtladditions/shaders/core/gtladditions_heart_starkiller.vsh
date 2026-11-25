#version 150

in vec3 Position;
in vec4 Color;
in vec3 Normal;

uniform mat4 ModelMat;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec3 Center;

out vec3 VertexColor;
out vec3 WorldNormal;
out vec3 ObjectNormal;
out vec3 WorldPos;
out vec3 ObjectPos;
out vec3 ObjectLight;
out float ClipDepthFromFar;

void main() {
    VertexColor = Color.rgb;
    ObjectPos = Position;
    ObjectNormal = Normal;
    WorldNormal = normalize(mat3(ModelMat) * Normal);
    WorldPos = (ModelMat * vec4(Position, 1.0)).xyz;
    // ModelMat is a rotation times a uniform scale: its transpose takes a direction back to the model.
    ObjectLight = transpose(mat3(ModelMat)) * (Center - WorldPos);
    vec4 viewPos = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * viewPos;

    // Compute w - z in view space before projection rounds both coordinates to nearly the same value.
    vec4 farPlane = transpose(ProjMat) * vec4(0.0, 0.0, -1.0, 1.0);

    // Beyond one model diameter, bound the view-space separation of coplanar material layers.
    float diameter = 2.0 * length(ModelMat[0].xyz);
    float scale = diameter / max(diameter, gl_Position.w);
    float clipBias = Color.a * 255.0 * 3.0e-7 * gl_Position.w * scale * scale;
    ClipDepthFromFar = dot(farPlane, viewPos) + clipBias;
    gl_Position.z -= clipBias;
}
