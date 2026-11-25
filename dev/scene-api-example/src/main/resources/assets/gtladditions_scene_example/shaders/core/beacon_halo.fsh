#version 150

uniform vec4 HaloColor;

in vec3 viewPosition;
in vec3 viewNormal;

out vec4 fragColor;

void main() {
    float facing = abs(dot(normalize(viewNormal), normalize(-viewPosition)));
    float rim = max(1.0 - facing, 0.0);
    // ONE, ONE: the colour carries its own weight.
    fragColor = vec4(HaloColor.rgb * HaloColor.a * (rim * rim * rim), 1.0);
}
