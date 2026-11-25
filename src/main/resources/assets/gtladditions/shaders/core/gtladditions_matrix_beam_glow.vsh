#version 150

in vec3 Position;
in vec2 UV0;
in ivec2 UV1;

uniform mat4 BeamViews[6];
uniform float BeamCameraX[6];
uniform float BeamCameraY[6];
uniform mat4 ProjMat;

out vec2 texCoord;
out vec2 localPosition;
out float transparency;
flat out vec2 beamCameraPosition;

void main() {
    int layer = UV1.y;
    texCoord = UV0;
    localPosition = Position.xy;
    transparency = float(UV1.x) / 10000.0;
    beamCameraPosition = vec2(BeamCameraX[layer], BeamCameraY[layer]);
    gl_Position = ProjMat * BeamViews[layer] * vec4(Position, 1.0);
}
