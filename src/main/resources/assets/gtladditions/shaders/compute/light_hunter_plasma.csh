#version 430

layout(local_size_x = 8, local_size_y = 4, local_size_z = 4) in;
layout(rgba16f, binding = 0) uniform writeonly image3D FieldOutput;
uniform sampler2D PlasmaNoise;
uniform ivec3 FieldSize;

#moj_import <gtladditions:light_hunter_plasma.glsl>

void main() {
    ivec3 cell = ivec3(gl_GlobalInvocationID);
    if (any(greaterThanEqual(cell, FieldSize))) return;
    vec3 unit = vec3(cell) / vec3(FieldSize - 1);
    float x = unit.x * PLASMA_MAIN_LENGTH;
    vec2 yz = (unit.yz * 2.0 - 1.0) * plasmaFieldExtent(x);
    vec4 value = plasmaSample(vec3(x, yz), PLASMA_MAIN_LENGTH, true);
    imageStore(FieldOutput, cell, vec4(value.rgb * value.a, value.a));
}
