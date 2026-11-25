#version 150

uniform vec4 CoreColor;
uniform sampler2D HandMask;
uniform int HandMaskEnabled;

in vec3 viewPosition;

out vec4 fragColor;

void main() {
    vec3 dx = dFdx(viewPosition);
    vec3 dy = dFdy(viewPosition);
    float dx2 = dot(dx, dx);
    float dy2 = dot(dy, dy);
    // Normalize tangents first so cross() stays scale-invariant at close range and only trips fallback on degenerate faces.
    vec3 gradient = (dx2 > 1e-30 && dy2 > 1e-30)
        ? cross(dx * inversesqrt(max(dx2, 1e-30)), dy * inversesqrt(max(dy2, 1e-30)))
        : vec3(0.0);
    float len2 = dot(gradient, gradient);
    vec3 normal = len2 > 1e-12 ? gradient * inversesqrt(max(len2, 1e-12)) : vec3(0.0, 0.0, 1.0);
    fragColor = vec4(CoreColor.rgb * (0.35 + 0.65 * abs(normal.z)), 1.0);
    // After the material: derivatives are undefined once a neighbouring fragment has been discarded.
    if (HandMaskEnabled != 0 && texelFetch(HandMask, ivec2(gl_FragCoord.xy), 0).r > 0.5) discard;
}
