#version 150

uniform vec4 CoreColor;
uniform sampler2D HandMask;
uniform int HandMaskEnabled;

in vec3 viewPosition;

out vec4 fragColor;

void main() {
    vec3 normal = normalize(cross(dFdx(viewPosition), dFdy(viewPosition)));
    fragColor = vec4(CoreColor.rgb * (0.35 + 0.65 * abs(normal.z)), 1.0);
    // After the material: derivatives are undefined once a neighbouring fragment has been discarded.
    if (HandMaskEnabled != 0 && texelFetch(HandMask, ivec2(gl_FragCoord.xy), 0).r > 0.5) discard;
}
