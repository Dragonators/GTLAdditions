#version 150
#moj_import <gtladditions:light_hunter_interior_materials.glsl>
in vec3 Position;
in vec2 UV0;
in vec4 Color;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec2 FanRotation;
uniform vec2 FanCenters;
out vec2 uv;
out vec3 localPosition;
flat out int material;
flat out float surfaceLight;
void main() {
    material = int(floor(Color.a * 255.0 + 0.5));
    surfaceLight = Color.r;
    uv = UV0;
    localPosition = Position;
    vec3 position = Position;
    if (material == INTERIOR_FAN_BLADE || material == INTERIOR_FAN_HUB) {
        float center = abs(position.x - FanCenters.x) < abs(position.x - FanCenters.y) ? FanCenters.x : FanCenters.y;
        vec2 radial = position.xz - vec2(center, 0.0);
        position.xz = mat2(FanRotation.x, FanRotation.y, -FanRotation.y, FanRotation.x) * radial + vec2(center, 0.0);
    }
    gl_Position = ProjMat * ModelViewMat * vec4(position, 1.0);
}
