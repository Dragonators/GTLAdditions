#version 150

in vec4 vertexColor;
in vec2 mark;
out vec4 fragColor;

void main() {
    bool disc = mark.y > 16.0;
    float distance = disc ? length(vec2(mark.x, mark.y - 32.0)) : abs(mark.x);
    float aa = max(fwidth(distance), 0.001);
    float core = 1.0 - smoothstep(1.0 - aa, 1.0 + aa, distance);
    float capAA = max(fwidth(mark.y), 0.001);
    float cap = disc ? 1.0 : smoothstep(-capAA, capAA, mark.y);
    float glowDistance = disc ? distance : length(vec2(mark.x, min(mark.y, 0.0)));
    float halo = disc ? 0.20 * (1.0 - smoothstep(1.0, 3.0, glowDistance))
        : 0.14 * (1.0 - smoothstep(1.0, 2.5, glowDistance))
        + 0.06 * (1.0 - smoothstep(1.0, 5.0, glowDistance));
    float emission = vertexColor.a * (core * cap + halo);
    const float emissionStrength = 1.10;
    // RGB is already weighted: ONE/ONE adds light without darkening the casing below it.
    fragColor = vec4(vertexColor.rgb * emission * emissionStrength, min(1.0, emission));
}
