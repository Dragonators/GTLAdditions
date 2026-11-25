#version 150

in vec3 Position;
in vec2 UV0;
in vec4 Color;
in vec3 Normal;

in vec4 InstanceModel0;
in vec4 InstanceModel1;
in vec4 InstanceModel2;
in vec4 InstanceModel3;
in vec4 InstanceView0;
in vec4 InstanceView1;
in vec4 InstanceView2;
in vec4 InstanceView3;
uniform int Instanced;
uniform mat4 ModelMat;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

flat out mat3 ObjectToWorld;
out vec3 WorldPos;
out vec3 WorldNormal;
out vec3 ObjectPos;
out vec3 ObjectNormal;
out vec2 TexUv;

void main() {
    mat4 model = Instanced != 0 ? mat4(InstanceModel0, InstanceModel1, InstanceModel2, InstanceModel3) : ModelMat;
    mat4 view = Instanced != 0 ? mat4(InstanceView0, InstanceView1, InstanceView2, InstanceView3) : ModelViewMat;
    ObjectToWorld = mat3(model);
    WorldPos = (model * vec4(Position, 1.0)).xyz;
    WorldNormal = ObjectToWorld * Normal;
    ObjectPos = Position;
    ObjectNormal = Normal;
    TexUv = UV0;
    gl_Position = ProjMat * view * vec4(Position, 1.0);
}
