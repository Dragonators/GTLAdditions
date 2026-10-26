#moj_import <gtladditions:light_hunter_plasma.glsl>

// The engine's ShaderInstance sampler list binds only TEXTURE_2D. The renderer
// therefore binds this 3D texture explicitly and sets its integer uniform.
uniform sampler3D PlasmaField;
uniform int UsePlasmaField;

// q is nozzle-local; RGB and density are unintegrated.
vec4 samplePlasmaField(vec3 q, float len, bool mainJet) {
    if (UsePlasmaField == 0 || !mainJet) return plasmaSample(q, len, mainJet);
    if (q.x < 0.0 || q.x > PLASMA_MAIN_LENGTH) return vec4(0.0);
    float extent = plasmaFieldExtent(q.x);
    if (dot(q.yz, q.yz) > extent * extent) return vec4(0.0);
    vec3 unit = vec3(q.x / PLASMA_MAIN_LENGTH, q.yz / (2.0 * extent) + 0.5);
    if (any(lessThan(unit, vec3(0.0))) || any(greaterThan(unit, vec3(1.0)))) return vec4(0.0);
    vec3 size = vec3(textureSize(PlasmaField, 0));
    vec3 address = (unit * (size - 1.0) + 0.5) / size;
    vec4 voxel = textureLod(PlasmaField, address, 0.0);
    // Stored RGB is density-weighted so interpolation cannot bleed bright
    // empty cells into dense gas.
    return vec4(voxel.rgb / max(voxel.a, 0.000001), voxel.a);
}
