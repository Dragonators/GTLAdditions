#version 150

// Adapted from GTNH GoodGenerator antimatter.vert.glsl (LGPL-3.0).
// Provenance: models/effect/condensation/ASSET-SOURCES.txt.
in vec3 Position;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform float Phase;
uniform vec3 ColorCore;
uniform vec3 ColorSpike;
out vec3 vertexColor;

float lazyHash(vec3 position) {
    vec3 value = fract(position * 1.23456 + 3.1456);
    value *= 7.0;
    return fract(value.y + value.x * (value.z + 1.0));
}

void main() {
    float phase = mod(Phase / 4.0 + lazyHash(Position) / 2.0, 1.0);
    float triangle = 1.0 - abs(2.0 * (phase - 0.5));
    vertexColor = mix(ColorCore, ColorSpike, triangle);
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
