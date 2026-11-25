#version 330 core
in vec3 Position;
in vec2 UV0;
in vec4 Color;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform float AngularTime;
uniform int Effect;
out vec2 uv;
out vec4 tint;
out vec3 localPosition;
flat out vec2 ringShape;
out float effectViewDepth;
flat out vec3 localEye;
flat out vec3 localForward;
flat out float orthoCamera;
void main() {
    mat4 inverseView = inverse(ModelViewMat);
    localEye = inverseView[3].xyz;
    localForward = mat3(inverseView) * vec3(0, 0, -1);
    orthoCamera = ProjMat[3][3] > 0.5 ? 1.0 : 0.0;
    uv = UV0;
    tint = Color;
    vec3 p = Position;
    ringShape = UV0;
    if (Effect == 1) {
        // UV stores centre X and major radius. Animate the field, never its raster bounds.
        ringShape.y *= 1.0 + 0.008 * sin(AngularTime * 3.2 + ringShape.x * 0.11);
    }
    vec4 view = ModelViewMat * vec4(p, 1.0);
    localPosition = p;
    effectViewDepth = -view.z;
    gl_Position = ProjMat * view;
}
