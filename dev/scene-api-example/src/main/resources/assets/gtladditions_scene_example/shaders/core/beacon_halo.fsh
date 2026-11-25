#version 150

uniform vec4 HaloColor;

in vec3 viewPosition;
in vec3 viewNormal;

out vec4 fragColor;

void main() {
    float facing = abs(dot(normalize(viewNormal), normalize(-viewPosition)));
    // ONE, ONE: the colour carries its own weight.
    fragColor = vec4(HaloColor.rgb * HaloColor.a * pow(1.0 - facing, 3.0), 1.0);
}
